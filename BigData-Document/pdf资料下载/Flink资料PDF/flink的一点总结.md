# Flink延迟Metric误用之后的一些补救方案
**场景&原因**

 前两天业务方反馈，延迟报警500万，将任务修改并行度从1个并行改成几十个后重启任务后，任务瞬间延迟到了1个亿。数据量不大，不会短时间涨到一个亿，所以我们第一反应是不是保存点一直失败，导致从一个很旧的保存点消费。但是翻了监控之后，发现保存点没有问题，不是这个Case。后来我们通过翻看Kafka Metric文档发现records.lag.max 原来并不是代表每个分区都延迟的总和，而是在一个时间窗口内，这个Consumer订阅到的所有分区中，其中一个延迟最大的值。也就是说如果Flink一个并行度订阅到100个分区，这个并行度上报的延迟Metric是其中延迟最大的分区的延迟条数，差距肯定会很大。

**补救方案**

去Flink的用户邮箱搜了一圈关于消费Kafka延迟监控，发现竟没有一个改动稍微小一些且彻底解决上报延迟问题的办法。且平台上千个存量任务的报警，如果新增Metric逐个修改报警口径成本也是有些高，权衡了下之后，决定还是去Kafka-client项目，把这个Metric稍微做下修改。

**现状**

跟了下代码(Kafka Release 2.2.1)发现

`1:客户端将订阅到的Partition分别去结算延迟`

```
Long partitionLag = subscriptions.partitionLag(partitionRecords.partition, isolationLevel);
                if (partitionLag != null)
                    this.sensors.recordPartitionLag(partitionRecords.partition, partitionLag);
```
`2:延迟会统一被Sensor recordsFetchLag记录, 而recordsFetchLag 会维护个SampledStat的子类 Max`

```
this.recordsFetchLag.add(metrics.metricInstance(metricsRegistry.recordsLagMax), new Max());

  private void recordPartitionLag(TopicPartition tp, long lag) {
             this.recordsFetchLag.record(lag);
        ......
        }
```
`3.看一下SampledStat是默认维护两个采样（实现类Sample）的统计类，最终会取一分钟内的Sample计算出最终的Metric值`

```
public void record(MetricConfig config, double value, long timeMs) {
        Sample sample = current(timeMs);
        if (sample.isComplete(timeMs, config))
            sample = advance(config, timeMs);
        update(sample, config, value, timeMs);
        sample.eventCount += 1;
}

    protected Sample advance(MetricConfig config, long timeMs) {
        this.current = (this.current + 1) % config.samples();
        if (this.current >= samples.size()) {
            Sample sample = newSample(timeMs);
            this.samples.add(sample);
            return sample;
        } else {
            Sample sample = current(timeMs);
            sample.reset(timeMs);
            return sample;
        }
    }
```
`4:SampledStat子类Max则实现了统计的定制逻辑，同一个Sample取个最大值,不同Sample之间合并取最大值，Sample中只有一个double value ,无法按topic+Parition统计合并，而Sum则会把两个Sample累加，延迟会偏高很多。`

```
public final class Max extends SampledStat {

    public Max() {
        super(Double.NEGATIVE_INFINITY);
    }

    @Override
    protected void update(Sample sample, MetricConfig config, double value, long now) {
        sample.value = Math.max(sample.value, value);
    }

    @Override
    public double combine(List<Sample> samples, MetricConfig config, long now) {
        double max = Double.NEGATIVE_INFINITY;
        long count = 0;
        for (Sample sample : samples) {
            max = Math.max(max, sample.value);
            count += sample.eventCount;
        }
        return count == 0 ? Double.NaN : max;
    }

}
```

```
  protected static class Sample {
        public double initialValue;
        public long eventCount;
        public long lastWindowMs;
        public double value;
 }
```
**修改**

我们只需要Sample把每个分区的延迟都存下来，窗口结束合并Sample时候，把每个分区求和即可

`1:扩展下Sample`
```
protected static class GroupSample extends Sample {

        private Map<String,Double> valueGroupTopicPaition;

        public GroupSample(long now) {
            super(0,now);
            valueGroupTopicPaition =new HashMap<>();
        }
        
        private void setValueGroupTopicPaition(String topicPaition, double value) {
            valueGroupTopicPaition.put(topicPaition,value);
        }

        public Map<String, Double> getValueGroupTopicPaition() {
            return valueGroupTopicPaition;
        }
    }
```
注意千万不要在扩展Init方法清空子类中的字典，因为一个时间窗口内所有的分区可能上报不全，比如100个分区，在一个时间窗口可能来了90个，那么那10个延迟就会少报，在图表上看就会有很大抖动。

`2:继承Sum或SampledStat,计算实现合并Sample`

```
public class MaxGroupSum extends Sum{
    public MaxGroupSum() {
        super();
    }

    @Override
    public void record(MetricConfig config, double value, long timeMs){
        record(config,value,timeMs,null);
    }


    public void record(MetricConfig config, double value, long timeMs,String group) {

        GroupSample sample = (GroupSample) current(timeMs);
        sample.setValueGroupTopicPaition(group,value);
        if (sample.isComplete(timeMs, config))
            sample = (GroupSample) advance(config, timeMs);
        sample.eventCount += 1;
    }

    @Override
    protected void update(Sample sample, MetricConfig config, double value, long now) {
        throw new RuntimeException("We need do nothing ");
    }

    @Override
    public double combine(List<Sample> samples, MetricConfig config, long now) {

        Set<String> unionGroupSet = new HashSet<>();
        for (Sample sample : samples) {
            GroupSample groupMaxSample = (GroupSample) sample;
            unionGroupSet.addAll(groupMaxSample.getValueGroupTopicPaition().keySet());
        }

        double total = 0.0;
        for (String group : unionGroupSet) {
            double groupMaxValue = 0;
            for (Sample sample : samples) {
                GroupSample groupMaxSample = (GroupSample) sample;
                Map<String, Double> valueGroupTopicPaition = groupMaxSample.getValueGroupTopicPaition();
                if (valueGroupTopicPaition.containsKey(group)) {
                    groupMaxValue = Math.max(groupMaxValue, valueGroupTopicPaition.get(group));
                }
            }
            total += groupMaxValue;
        }

        return total;
    }

    protected Sample newSample(long timeMs) {
        return new GroupSample(timeMs);
    }
```
`3:简单在Sensor中改造走分组逻辑`

```
synchronized (this) {
                synchronized (metricLock()) {
                    // increment all the stats
                    for (Stat stat : this.stats) {
                        if (group != null && stat instanceof MaxGroupSum) {
                            MaxGroupSum stat1 = (MaxGroupSum) stat;
                            stat1.record(config, value, timeMs, group);
                        } else {
                            stat.record(config, value, timeMs);
                        }
                    }
                }
```
修改完成.修改后即使一个flink 并行度, lag.max metric也能正确地上报数据了.

# 复杂Flink任务Task均衡调度和优化措施

**一、背景：**

flink任务部署使用基于k8s的standalone集群，先在容器上部署flink集群再提交flink任务，其中flink任务的提交与taskmanager的创建、注册是同时进行的。

**二、问题**

```
如果集群有35个taskmanager，140个slot，其中一个Vertex的并行度<140，属于该vertex的task在taskmanager上分布不均，导致节点负载不均衡。
```
如下所示：
    该flink拓扑拥有5个vertex，其中两个vertex并行度为140，其他三个并行度根据kafka分区数设置为：10、30、35。任务最大并行度为140，任务资源配置为：35个【4core 8gb】的taskManager节点
![](https://files.mdnice.com/user/37771/f1583d91-3896-49ba-9dcd-45608e1df457.png)
    通过web ui可发现，即使配置了cluster.evenly-spread-out-slots：true，另外三个vertex的task依然会被调度到同个taskmanager上
![](https://files.mdnice.com/user/37771/c4f525c2-8c58-457a-9f9b-cf1099f14c54.png)
![](https://files.mdnice.com/user/37771/a89c6ba7-79dc-44e8-8af9-41fcdcf29cd3.png)
**三、优化方式**

`1. 问题分析`

上诉问题可以简化为：

假设一个任务拓扑逻辑为：Vertex A(p=2)->Vertex B(p=4)->Vertex C(p=2)。基于slot共享和本地数据传输优先的划分策略，划分为四个ExecutionSlotSharingGroup：{A1,B1,C1}、{A2,B2,C2}、{B3}、{B4},
如果资源配置将每个Taskmanager划分为2个Slot，就可能出现以下分配：
![](https://files.mdnice.com/user/37771/13906b57-f2b7-465b-bb8e-0a76428e12f3.png)
当前Slot划分是平均划分内存，对cpu没有做限制。上诉分配会导致节点负载不均衡，若A、C Task计算资源耗费较多，TaskManager1将会成为计算的瓶颈，理想情况下我们希望分配方式是：
![](https://files.mdnice.com/user/37771/6e77b9fd-8425-42a5-b649-df5db030d5e0.png)

`2. 优化`

```
## 修改策略

1. 为ExecutionSlotSharingGroup申请slot时先对其按包含Task个数排序，优先调度Task个数多的分组

2. 延缓任务调度，等注册TaskManager个数足够大ExecutionSlotSharingGroup平均分配再为其申请Slot

## 效果
优化后task调度情况：同个vertex的多个task均匀调度到不同的taskmanager节点上
```
![](https://files.mdnice.com/user/37771/cb9f2a94-022a-4bf2-8128-3580f71c73a0.png)
![](https://files.mdnice.com/user/37771/216b538e-12e9-40f9-8d34-c077adbfb002.png)
**四、性能对比**

`1. CPU负载对比`

优化前: 节点间CPU负载较为分散，部分节点长时间处于100%高负载状态
![](https://files.mdnice.com/user/37771/11ca29cc-1c16-4b28-bd13-e9778d1dbdf4.png)
优化后: 节点间CPU负载较为集中，节点不会长时间处于100%负载状态
![](https://files.mdnice.com/user/37771/d54db347-28d2-466a-8132-f07d1f78d8ff.png)
`2. CPU使用率对比`

从拓扑图可知任务存在200/480两种不同并行度的task，通过均衡task sharegroup，实现各tm节点的cpu负载均衡，以便我们后续压缩tm的资源配额。
![](https://files.mdnice.com/user/37771/40beb429-4953-4dfc-a9c4-00e9025bea32.png)
![](https://files.mdnice.com/user/37771/08da4171-5ef4-462d-acd4-6dd763aeaf1a.png)
`3. 数据积压情况`

优化后数据积压量比之前少一半，同资源情况下处理能力更佳，数据延迟更低
优化前:
![](https://files.mdnice.com/user/37771/739cf05d-5597-4bf3-8f4b-ad4a486b187e.png)
优化后:
![](https://files.mdnice.com/user/37771/96cb0a35-bc76-479d-922c-ee4f441f35b8.png)
**六：思考**

`1. Task均衡`

对于拓扑：Vertex A(p=3)->Vertex B(p=4)->Vertex C(p=1)。
将会按以下分配：
![](https://files.mdnice.com/user/37771/ef27f996-228b-41b0-b3af-900d2638e093.png)
Vertex B->Vertex C存在四条数据传输通道(B1->C1)、（B2->C1）、（B3->C1）、（B4->C1）,对于非forward的连接，无论subtask分配到哪个group中，至少都存在三条通道需要跨节点通讯。

那么如果在分组的时候就先对task做一次均衡：{A1,B1}、{A3,B3}、{A2,B2}、{B4,C1}，后面无论怎么调度都会均衡。但当task num% slot num ！= 0的时候，仍存在task在单tm聚集的情况。

`2. 延迟调度的改进`

在flink生成执行计划时期根据拓扑逻辑生成延迟的策略，减少用户操作感知。

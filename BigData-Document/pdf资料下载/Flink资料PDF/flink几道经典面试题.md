****Flink几道经典编程场景****

**1:分组TOPN**

`一、代码思路（一）`

**1: 定义的单独的pojo类UserBehavior 和 ItemViewCount**

UserBehavior  → 解析Json字符串后生成的JavaBean
ItemViewCount → 最后结果输出的格式类

**2: 调用底层的Process（可做类似map的操作），将Json字符串解析成UserBehavior对象**

**3、提取EventTime,转换成Timestamp格式,生成WaterMark**

**4、按照指定事件分组**

**5、把分好组的数据，划分窗口：假设窗口总长10分钟， 步长1分钟滑动一次**

**6: 调用aggregate方法，在窗口内增量聚合 (来一个加一个，内存中只保存一个数字而已)。**

MyWindowAggFunction:拿到聚合字段（UserBehavior中counts). 三个泛型：
第一个输入的类型
第二个计数/累加器的类型
第三个输出的数据类型

MyWindowFunction:拿到窗口的开始时间和结束时间，拿出分组字段.
传入4个泛型：

第一个：输入的数据类型（Long类型的次数），也就是 MyWindowAggFunction中聚合后的结果值
第二个：输出的数据类型（ItemViewCount）
第三个：分组的key(分组的字段)
第四个：窗口对象（起始时间、结束时间）

**7:对聚合好的窗口内数据排序.**

按照窗口的start、end进行分组，将窗口相同的数据进行排序
必须是在同一时间段的窗口

`ItemViewCount、UserBehavior`
```
import lombok.Data;
@Data
public class UserBehavior {
    public String userId;           // 用户ID
    public String itemId;           // 商品ID
    public String categoryId;       // 商品类目ID
    public String type;             // 用户行为, 包括("pv", "buy", "cart", "fav")
    public long timestamp;          // 行为发生的时间戳，单位秒
    public long counts = 1;

    public static UserBehavior of(String userId, String itemId, String categoryId, String type, long timestamp) {
        UserBehavior behavior = new UserBehavior();
        behavior.userId = userId;
        behavior.itemId = itemId;
        behavior.categoryId = categoryId;
        behavior.type = type;
        behavior.timestamp = timestamp;
        return behavior;
    }

    public static UserBehavior of(String userId, String itemId, String categoryId, String type, long timestamp,
                                long counts) {
        UserBehavior behavior = new UserBehavior();
        behavior.userId = userId;
        behavior.itemId = itemId;
        behavior.categoryId = categoryId;
        behavior.type = type;
        behavior.timestamp = timestamp;
        behavior.counts = counts;
        return behavior;
    }
}

@Data
public class ItemViewCount {
    public String itemId;     // 商品ID
    public String type;     // 事件类型
    public long windowStart;  // 窗口开始时间戳
    public long windowEnd;  // 窗口结束时间戳
    public long viewCount;  // 商品的点击量

    public static ItemViewCount of(String itemId, String type, long windowStart, long windowEnd, long viewCount) {
        ItemViewCount result = new ItemViewCount();
        result.itemId = itemId;
        result.type = type;
        result.windowStart = windowStart;
        result.windowEnd = windowEnd;
        result.viewCount = viewCount;
        return result;
    }
}
```
`MyWindowFunction`

```

import com.chehejia.dip.pojo.ItemViewCount;
import org.apache.flink.api.java.tuple.Tuple;
import org.apache.flink.streaming.api.functions.windowing.WindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

public  class MyWindowFunction implements WindowFunction<Long, ItemViewCount, Tuple, TimeWindow> {

    @Override
    public void apply(Tuple tuple, TimeWindow window, Iterable<Long> input, Collector<ItemViewCount> out) throws Exception {
        String itemId = tuple.getField(0);
        String type = tuple.getField(1);

        long windowStart = window.getStart();
        long windowEnd = window.getEnd();

        //窗口集合的结果
        Long aLong = input.iterator().next();

        //输出数据
        out.collect(ItemViewCount.of(itemId, type, windowStart, windowEnd, aLong));
    }
}
```
`MyWindowAggFunction`

```
import com.chehejia.dip.pojo.UserBehavior;
import org.apache.flink.api.common.functions.AggregateFunction;

public class MyWindowAggFunction implements AggregateFunction<UserBehavior, Long, Long> {

    //初始化一个计数器
    @Override
    public Long createAccumulator() {
        return 0L;
    }

    //每输入一条数据就调用一次add方法
    @Override
    public Long add(UserBehavior input, Long accumulator) {
        return accumulator + input.counts;
    }

    @Override
    public Long getResult(Long accumulator) {
        return accumulator;
    }

    //只针对SessionWindow有效，对应滚动窗口、滑动窗口不会调用此方法
    @Override
    public Long merge(Long a, Long b) {
        return null;
    }
}
```
`HotGoodsTopN`

```
import com.alibaba.fastjson.JSON;
import com.chehejia.dip.pojo.ItemViewCount;
import com.chehejia.dip.pojo.UserBehavior;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.datastream.WindowedStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor;
import org.apache.flink.streaming.api.windowing.assigners.SlidingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class HotGoodsTopN {
    public static void main(String[] args) throws Exception {

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // 选择EventTime作为Flink的时间
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
        // 设置checkPoint时间
        env.enableCheckpointing(60000);
        // 设置并行度
        env.setParallelism(1);

        DataStreamSource<String> lines = env.socketTextStream("linux01", 8888);

        SingleOutputStreamOperator<UserBehavior> process = lines.process(new ProcessFunction<String, UserBehavior>() {
            @Override
            public void processElement(String input, Context ctx, Collector<UserBehavior> out) throws Exception {

                try {
                    // FastJson 会自动把时间解析成long类型的TimeStamp
                    UserBehavior behavior = JSON.parseObject(input, UserBehavior.class);
                    out.collect(behavior);
                } catch (Exception e) {
                    e.printStackTrace();
                    //TODO 记录出现异常的数据
                }
            }
        });

        // 设定延迟时间
        SingleOutputStreamOperator<UserBehavior> behaviorDSWithWaterMark =
                process.assignTimestampsAndWatermarks(new BoundedOutOfOrdernessTimestampExtractor<UserBehavior>(Time.seconds(0)) {
                    @Override
                    public long extractTimestamp(UserBehavior element) {
                        return element.timestamp;
                    }
                });


        //  某个商品，在窗口时间内，被（点击、购买、添加购物车、收藏）了多少次
        KeyedStream<UserBehavior, Tuple> keyed = behaviorDSWithWaterMark.keyBy("itemId", "type");

        // 把分好组的数据，划分窗口：假设窗口总长10分钟， 步长1分钟滑动一次
        WindowedStream<UserBehavior, Tuple, TimeWindow> window =
                keyed.window(SlidingEventTimeWindows.of(Time.minutes(10), Time.minutes(1)));

        // 优化点：在窗口内增量聚合 (来一个加一个，内存中只保存一个数字而已)
        /**  使用这种aggregate聚合方法：
         *
         */
        SingleOutputStreamOperator<ItemViewCount> windowAggregate = window.aggregate(new MyWindowAggFunction(),
                new MyWindowFunction());

        // 分组
        KeyedStream<ItemViewCount, Tuple> soredKeyed = windowAggregate.keyBy("type", "windowStart",
                "windowEnd");

        // 排序
        SingleOutputStreamOperator<List<ItemViewCount>> sored = soredKeyed.process(new KeyedProcessFunction<Tuple, ItemViewCount, List<ItemViewCount>>() {
            private transient ValueState<List<ItemViewCount>> valueState;

            // 要把这个时间段的所有的ItemViewCount作为中间结果聚合在一块，引入ValueState
            @Override
            public void open(Configuration parameters) throws Exception {
                ValueStateDescriptor<List<ItemViewCount>> VSDescriptor =
                        new ValueStateDescriptor<>("list-state",
                                TypeInformation.of(new TypeHint<List<ItemViewCount>>() {
                                })
                        );

                valueState = getRuntimeContext().getState(VSDescriptor);

            }

            //更新valueState 并注册定时器
            @Override
            public void processElement(ItemViewCount input, Context ctx, Collector<List<ItemViewCount>> out) throws Exception {
                List<ItemViewCount> buffer = valueState.value();
                if (buffer == null) {
                    buffer = new ArrayList<>();
                }
                buffer.add(input);
                valueState.update(buffer);
                //注册定时器,当为窗口最后的时间时，通过加1触发定时器
                ctx.timerService().registerEventTimeTimer(input.windowEnd + 1);

            }

            // 做排序操作
            @Override
            public void onTimer(long timestamp, OnTimerContext ctx, Collector<List<ItemViewCount>> out) throws Exception {

                //将ValueState中的数据取出来
                List<ItemViewCount> buffer = valueState.value();
                buffer.sort(new Comparator<ItemViewCount>() {
                    @Override
                    public int compare(ItemViewCount o1, ItemViewCount o2) {
                        //按照倒序，转成int类型
                        return -(int) (o1.viewCount - o2.viewCount);
                    }
                });
                valueState.update(null);
                out.collect(buffer);
            }
        });
        env.execute("HotGoodsTopNAdv");
    }
}
```

**2:Flink使用二次聚合实现TopN计算**

`需求背景：`

```
有需求需要对数据进行统计，要求每隔5分钟输出最近1小时内点击量最多的前N个商品，数据格式预览如下：
208.115.111.72 - - 17/05/2015:10:25:49 +0000 GET /?N=A&page=21   //15:50-25:50窗口数据
208.115.111.72 - - 17/05/2015:10:25:50 +0000 GET /?N=A&page=21
208.115.111.72 - - 17/05/2015:10:25:51 +0000 GET /?N=A&page=21
208.115.111.72 - - 17/05/2015:10:25:52 +0000 GET /?N=A&page=21   //第一次触发计算，15:50-25:50窗口
208.115.111.72 - - 17/05/2015:10:25:47 +0000 GET /?N=A&          //迟到数据，不同url
208.115.111.72 - - 17/05/2015:10:25:53 +0000 GET /?N=A&page=21   //第二次触发计算，15:50-25:50窗口
208.115.111.72 - - 17/05/2015:10:25:46 +0000 GET /?N=A&page=21   //迟到数据
208.115.111.72 - - 17/05/2015:10:25:54 +0000 GET /?N=A&page=21   //第三次触发计算

最后统计输出结果如下（迟到数据均在25:50窗口）：

==============2015-05-17 10:25:50.0==============               //第一次触发计算结果
Top1 Url:/?N=A&page=21 Counts:1
==============2015-05-17 10:25:50.0==============

==============2015-05-17 10:25:50.0==============               //第二次触发计算结果
Top1 Url:/?N=A&page=21 Counts:1
Top2 Url:/?N=A& Counts:1
==============2015-05-17 10:25:50.0==============

==============2015-05-17 10:25:50.0==============               //第三次触发计算结果
Top1 Url:/?N=A&page=21 Counts:2
Top2 Url:/?N=A& Counts:1
==============2015-05-17 10:25:50.0==============
```
`实现思路分析`

```
①建立环境，设置并行度及CK。
②定义watermark策略及事件时间，获取数据并对应到JavaBean，筛选pv数据。
③第一次聚合，按商品id分组开窗聚合,使用aggregate算子进行增量计算。
④第二次聚合，按窗口聚合，使用ListState存放数据，并定义定时器，在watermark达到后1秒触发，对窗口数据排序输出。
⑤打印结果及执行。
```
`第一次聚合代码：`

```
SingleOutputStreamOperator<ItemCount> aggregateDS = userBehaviorDS
		.map(new MapFunction<UserBehavior, Tuple2<Long, Integer>>() {
			@Override
			public Tuple2<Long, Integer> map(UserBehavior value) throws Exception {
				return new Tuple2<>(value.getItemId(), 1);
			}})
		.keyBy(data -> data.f0)
		.window(SlidingEventTimeWindows.of(Time.hours(1), Time.minutes(5)))
		.aggregate(new ItemCountAggFunc(), new ItemCountWindowFunc());
```
①第一次聚合这里将商品id进行提取并转换为Tuple2<id,1>的格式，再对id进行keyby后聚合，避免直接使用对应的JavaBean进行分组聚合提高效率：

②这里使用aggregate算子进行增量计算，Flink的window function来负责一旦窗口关闭, 去计算处理窗口中的每个元素。window function 是如下三种：

ReduceFunction （增量聚合函数） 输入及输出类型得一致
AggregateFunction（增量聚合函数）输入及输出类型可以不一致
ProcessWindowFunction（全窗口函数）
ReduceFunction,AggregateFunction更加高效, 原因就是Flink可以对到来的元素进行增量聚合 .
ProcessWindowFunction 可以得到一个包含这个窗口中所有元素的迭代器, 以及这些元素所属窗口的一些元数据信息.
ProcessWindowFunction不能被高效执行的原因是Flink在执行这个函数之前, 需要在内部缓存这个窗口上所有的元素。

`2.2、重写AggregateFunction函数代码`

```
public static class ItemCountAggFunc implements AggregateFunction<Tuple2<Long,Integer>,Integer,Integer>{
	@Override
	public Integer createAccumulator() { return 0; }
	@Override
	public Integer add(Tuple2<Long, Integer> value, Integer accumulator) { return accumulator+1; }
	@Override
	public Integer getResult(Integer accumulator) { return accumulator; }
	@Override
	public Integer merge(Integer a, Integer b) { return a+b; }
}
```
这里对AggregateFunction函数里面四个方法进行重写自定义计数规则，入参<IN,ACC,OUT>对应为Tuple2，累加器用Integer过度，输出结果为Integer。

**createAccumulator**
这个方法首先要创建一个累加器，要进行一些初始化的工作，这里初始值为0.
add
add方法就是做聚合的时候的核心逻辑，这里这是对tuple的第二位整数进行累加。

**merge**
Flink是一个分布式计算框架，可能计算是分布在很多节点上同时进行的，如果计算在多个节点进行，需要对结果进行合并，这个merge方法就是做这个工作的，所以入参和出参的类型都是中间结果类型ACC。

**getResult**
这个方法就是将每个用户最后聚合的结果经过处理之后，按照OUT的类型返回，返回的结果也就是聚合函数的输出结果了。

```
这里也是AggregateFunction和ReduceFunction区别的地方，reduce的input为Tuple2，则output也必须是Tuple2。
```
`2.3、重写KeyedProcessFunction里面方法的部分代码`

```
@Override
public void processElement(UrlCount value, Context ctx, Collector<String> out) throws Exception {
	//状态装入数据
	mapState.put(value.getUrl(), value);
	//定时器,窗口一秒后触发
	ctx.timerService().registerEventTimeTimer(value.getWindowEnd()+1L);
	//再加一个定时器来清除状态用，在窗口关闭后再清除状态，这样延迟数据到达后窗口还能做排序
	ctx.timerService().registerEventTimeTimer(value.getWindowEnd()+61001L);
}
//定时器内容
@Override
public void onTimer(long timestamp, OnTimerContext ctx, Collector<String> out) throws Exception {
	if (timestamp == ctx.getCurrentKey()+61001L){
		mapState.clear();
		return;}
...
这里改用MapState，如若使用ListState，进来迟到数据后，则会出现同个url在同个窗口的统计出现多个计数的情况，列表状态不具备去重功能，故在这里使用map状态来实现去重。
这里使用定时器来清除状态，原写法是在onTimer最后排序完直接清除状态，则会导致迟到数据到达后，原窗口其他数据被清除掉无法实现排名的输出，这里定时器的时间是在61001毫秒后清除状态数据。
定时器61001毫秒 = 允许迟到数据1秒（forBoundedOutOfOrderness）+窗口迟到数据1分钟（allowedLateness）+第一个定时器1毫秒。
```
`完整代码`

```
##映射数据源的JavaBean

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApacheLog {
    private String ip;
    private String userId;
    private Long ts;
    private String method;
    private String url;
}

##第一次聚合输出的JavaBean

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UrlCount {
    private String url;
    private Long windowEnd;
    private Integer count;
}
```
`核心代码`

```
package com.test.topN;

import bean.ApacheLog;
import bean.UrlCount;
import org.apache.commons.compress.utils.Lists;
import org.apache.flink.api.common.eventtime.SerializableTimestampAssigner;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.state.MapState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.streaming.api.functions.windowing.WindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.SlidingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
/**
 * @author: Rango
 * @create: 2021-05-26 10:16
 * @description: 每隔5秒，输出最近10分钟内访问量最多的前N个URL
 **/
public class URLTopN3 {
    public static void main(String[] args) throws Exception {

        //1.建立环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment().setParallelism(1);

        //2.读取端口数据并映射到JavaBean，并定义watermark时间语义
        WatermarkStrategy<ApacheLog> wms = WatermarkStrategy
                .<ApacheLog>forBoundedOutOfOrderness(Duration.ofSeconds(1))
                .withTimestampAssigner(new SerializableTimestampAssigner<ApacheLog>() {
                    @Override
                    public long extractTimestamp(ApacheLog element, long recordTimestamp) {
                        return element.getTs();
                    }});

        SingleOutputStreamOperator<ApacheLog> apacheLogDS = env.socketTextStream("hadoop102", 9999)
                .map(new MapFunction<String, ApacheLog>() {
                    @Override
                    public ApacheLog map(String value) throws Exception {
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy:HH:mm:ss");
                        String[] split = value.split(" ");
                        return new ApacheLog(split[0],
                                split[2],
                                sdf.parse(split[3]).getTime(),
                                split[5],
                                split[6]);
                    }})
                .assignTimestampsAndWatermarks(wms);

        //3.第一次聚合，按url转为tuple2分组，开窗，增量聚合
        SingleOutputStreamOperator<UrlCount> aggregateDS = apacheLogDS
                .map(new MapFunction<ApacheLog, Tuple2<String, Integer>>() {
            @Override
            public Tuple2<String, Integer> map(ApacheLog value) throws Exception {
                return new Tuple2<>(value.getUrl(), 1);
            }}).keyBy(data -> data.f0)
                .window(SlidingEventTimeWindows.of(Time.minutes(10),Time.seconds(5)))
                .allowedLateness(Time.minutes(1))
                .aggregate(new HotUrlAggFunc(), new HotUrlWindowFunc());

        //4.第二次聚合，对第一次聚合输出按窗口分组，再全窗口聚合，建立定时器你，每5秒钟触发一次
        SingleOutputStreamOperator<String> processDS = aggregateDS
                .keyBy(data -> data.getWindowEnd())
                .process(new HotUrlProcessFunc(5));

        processDS.print();
        env.execute();
    }
    //实现AggregateFunction类中的方法
    public static class HotUrlAggFunc implements AggregateFunction<Tuple2<String, Integer>,Integer,Integer>{
        @Override
        public Integer createAccumulator() {return 0;}
        @Override
        public Integer add(Tuple2<String, Integer> value, Integer accumulator) { return accumulator+1;}
        @Override
        public Integer getResult(Integer accumulator) {return accumulator;}
        @Override
        public Integer merge(Integer a, Integer b) {return a+b; }
    }
    //实现窗口函数的apply方法，把累加函数输出的整数结果，转换为javabean类urlcount来做输出，方便后续按窗口聚合
    public static class HotUrlWindowFunc implements WindowFunction<Integer, UrlCount,String, TimeWindow> {
        @Override
        public void apply(String urls, TimeWindow window, Iterable<Integer> input, Collector<UrlCount> out) throws Exception {
            //获取按key相加后的次数并新建javabean（urlcount）作为返回
            Integer count = input.iterator().next();
            out.collect(new UrlCount(urls,window.getEnd(),count));
        }
    }
    //继承KeyedProcessFunction方法，重写processElemnt方法
    public static class HotUrlProcessFunc extends KeyedProcessFunction<Long,UrlCount,String>{
        //定义TopN为入参
        private Integer TopN;
        public HotUrlProcessFunc(Integer topN) {
            TopN = topN;
        }
        //定义状态
        private MapState <String,UrlCount>mapState;
        //open方法中初始化状态
        @Override
        public void open(Configuration parameters) throws Exception {
            mapState = getRuntimeContext()
                    .getMapState(new MapStateDescriptor<String, UrlCount>("map-state",String.class,UrlCount.class));
        }
        @Override
        public void processElement(UrlCount value, Context ctx, Collector<String> out) throws Exception {
            //状态装入数据
            mapState.put(value.getUrl(), value);
            //定时器,窗口一秒后触发
            ctx.timerService().registerEventTimeTimer(value.getWindowEnd()+1L);
            //再加一个定时器来清除状态用，在窗口关闭后再清除状态，这样延迟数据到达后窗口还能做排序
            ctx.timerService().registerEventTimeTimer(value.getWindowEnd()+61001L);
        }
        //定时器内容
        @Override
        public void onTimer(long timestamp, OnTimerContext ctx, Collector<String> out) throws Exception {
            if (timestamp == ctx.getCurrentKey()+61001L){
                mapState.clear();
                return;}

            //取出状态数据
            Iterator<Map.Entry<String, UrlCount>> iterator = mapState.iterator();
            ArrayList<Map.Entry<String, UrlCount>> entries = Lists.newArrayList(iterator);

            //排序
            entries.sort(((o1, o2) -> o2.getValue().getCount()-o1.getValue().getCount()));

            //排序后装入StringBulider作为输出TopN
            StringBuilder sb = new StringBuilder();
            sb.append("==============")
                    .append(new Timestamp(timestamp - 1L))
                    .append("==============")
                    .append("\n");
            for (int i = 0; i < Math.min(TopN,entries.size()); i++) {
                UrlCount urlCount = entries.get(i).getValue();
                sb.append("Top").append(i+1);
                sb.append(" Url:").append(urlCount.getUrl());
                sb.append(" Counts:").append(urlCount.getCount());
                sb.append("\n");
            }
            sb.append("==============")
                    .append(new Timestamp(timestamp - 1L))
                    .append("==============")
                    .append("\n")
                    .append("\n");

            out.collect(sb.toString());
            Thread.sleep(200);
            }}}
```

**3:PV、UV统计**

`需求描述`

从Kafka发送过来的数据含有：时间戳、时间、维度、用户id，需要从不同维度统计从0点到当前时间的pv和uv，第二天0点重新开始计数第二天的。

PV(访问量)：即Page View, 即页面浏览量或点击量，用户每次刷新即被计算一次。
UV(独立访客)：即Unique Visitor,访问您网站的一台电脑客户端为一个访客。00:00-24:00内相同的客户端只被计算一次。

```
订单数据：
{"time":"2021-10-31 22:00:01","timestamp":"1635228001","product":"苹果手机","uid":255420}
{"time":"2021-10-31 22:00:02","timestamp":"1635228001","product":"MacBook Pro","uid":255421}
```
`实现思路：`

1:Kafka数据可能会有延迟乱序，这里引入watermark；

2:通过keyBy分流进不同的滚动window，每个窗口内计算pv、uv；

3:由于需要保存一天的状态，process里面使用ValueState保存pv、uv；

4:使用BitMap类型ValueState，占内存很小，引入支持bitmap的依赖；

5:保存状态需要设置ttl过期时间，第二天把第一天的过期，避免内存占用过大。

`代码实现：`

pojo类：
```
@NoArgsConstructor
@AllArgsConstructor
@Data
@ToString
public class UserClickModel {
    private String date;
    private String product;
    private int uid;
    private int pv;
    private int uv;
}
```
运行主类：

```
public class UserClickMain {

    private static final Map<String, String> config = Configuration.initConfig("commons.xml");

    public static void main(String[] args) throws Exception {

        // 初始化环境，配置相关属性
        StreamExecutionEnvironment senv = StreamExecutionEnvironment.getExecutionEnvironment();
        senv.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
        senv.enableCheckpointing(5000, CheckpointingMode.EXACTLY_ONCE);
        senv.setStateBackend(new FsStateBackend("hdfs://bigdata/flink/checkpoints/userClick"));

        // 读取kafka
        Properties kafkaProps = new Properties();
        kafkaProps.setProperty("bootstrap.servers", config.get("kafka-ipport"));
        kafkaProps.setProperty("group.id", config.get("kafka-groupid"));
        // kafkaProps.setProperty("auto.offset.reset", "earliest");

        // watrmark 允许数据延迟时间
        long maxOutOfOrderness = 5 * 1000L;
        SingleOutputStreamOperator<UserClickModel> dataStream = senv.addSource(
                new FlinkKafkaConsumer<>(
                        config.get("kafka-topic"),
                        new SimpleStringSchema(),
                        kafkaProps
                ))
                //设置watermark
                .assignTimestampsAndWatermarks(WatermarkStrategy.<String>forBoundedOutOfOrderness(Duration.ofMillis(maxOutOfOrderness))
                        .withTimestampAssigner((element, recordTimestamp) -> {
                            // 时间戳须为毫秒
                            return Long.valueOf(JSON.parseObject(element).getString("timestamp")) * 1000;
                        }))
                        .withIdleness(Duration.ofSeconds(1))
                        .map(new FCClickMapFunction()).returns(TypeInformation.of(new TypeHint<UserClickModel>() {
                }));

        // 按照 (date, product) 分组
        dataStream.keyBy(new KeySelector<UserClickModel, Tuple2<String, String>>() {
            @Override
            public Tuple2<String, String> getKey(UserClickModel value) throws Exception {
                return Tuple2.of(value.getDate(), value.getProduct());
            }
        })
                // 一天为窗口，指定时间起点比时间戳时间早8个小时
                .window(TumblingEventTimeWindows.of(Time.days(1), Time.hours(-8)))
                // 10s触发一次计算，更新统计结果
                .trigger(ContinuousEventTimeTrigger.of(Time.seconds(10)))
                // 计算pv uv
                .process(new MyProcessWindowFunctionBitMap())
                // 保存结果到mysql
                .addSink(new FCClickSinkFunction());

        senv.execute(UserClickMain.class.getSimpleName());
    }
}
```
注意
1:设置watermark，flink1.11中使用WatermarkStrategy，老的已经废弃了；

2:我的数据里面时间戳是秒，需要乘以1000，flink提取时间字段，必须为毫秒；

3:.window只传入一个参数，表明是滚动窗口，TumblingEventTimeWindows.of(Time.days(1), Time.hours(-8))这里指定了窗口的大小为一天，由于中国北京时间是东8区，比国际时间早8个小时，需要引入offset，可以自行进入该方法源码查看英文注释。

4:一天大小的窗口，根据watermark机制一天触发计算一次，显然是不合理的，需要用trigger函数指定触发间隔为10s一次，这样我们的pv和uv就是10s更新一次结果。

`4. 关键代码，计算pv、uv`

由于这里用户id刚好是数字，可以使用bitmap去重，简单原理是：把 user_id 作为 bit 的偏移量 offset，设置为 1 表示有访问，使用 1 MB的空间就可以存放 800 多万用户的一天访问计数情况。
redis是自带bit数据结构的，不过为了尽量少依赖外部存储媒介，这里自己实现bit，引入相应maven依赖即可：

```
<dependency>
    <groupId>org.roaringbitmap</groupId>
    <artifactId>RoaringBitmap</artifactId>
    <version>0.8.0</version>
</dependency>
```

```
public class MyProcessWindowFunctionBitMap extends ProcessWindowFunction<UserClickModel, UserClickModel, Tuple<String, String>, TimeWindow> {

    private transient ValueState<Integer> pvState;
    private transient ValueState<Roaring64NavigableMap> bitMapState;

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        ValueStateDescriptor<Integer> pvStateDescriptor = new ValueStateDescriptor<>("pv", Integer.class);
        ValueStateDescriptor<Roaring64NavigableMap> bitMapStateDescriptor = new ValueStateDescriptor("bitMap"
                , TypeInformation.of(new TypeHint<Roaring64NavigableMap>() {}));

        // 过期状态清除
        StateTtlConfig stateTtlConfig = StateTtlConfig
                .newBuilder(Time.days(1))
                .setUpdateType(StateTtlConfig.UpdateType.OnCreateAndWrite)
                .setStateVisibility(StateTtlConfig.StateVisibility.NeverReturnExpired)
                .build();
        // 开启ttl
        pvStateDescriptor.enableTimeToLive(stateTtlConfig);
        bitMapStateDescriptor.enableTimeToLive(stateTtlConfig);

        pvState = this.getRuntimeContext().getState(pvStateDescriptor);
        bitMapState = this.getRuntimeContext().getState(bitMapStateDescriptor);
    }

    @Override
    public void process(Tuple2<String, String> key, Context context, Iterable<UserClickModel> elements, Collector<UserClickModel> out) throws Exception {

        // 当前状态的pv uv
        Integer pv = pvState.value();
        Roaring64NavigableMap bitMap = bitMapState.value();
        if(bitMap == null){
            bitMap = new Roaring64NavigableMap();
            pv = 0;
        }

        Iterator<UserClickModel> iterator = elements.iterator();
        while (iterator.hasNext()){
            pv = pv + 1;
            int uid = iterator.next().getUid();
            //如果userId可以转成long
            bitMap.add(uid);
        }

        // 更新pv
        pvState.update(pv);

        UserClickModel UserClickModel = new UserClickModel();
        UserClickModel.setDate(key.f0);
        UserClickModel.setProduct(key.f1);
        UserClickModel.setPv(pv);
        UserClickModel.setUv(bitMap.getIntCardinality());

        out.collect(UserClickModel);
    }
}
```
除了使用bitmap去重外，还可以使用Flink SQL,编码更简洁，还可以借助外面的媒介Redis去重：

```
基于 set
基于 bit
基于 HyperLogLog
基于bloomfilter
```
具体思路是，计算pv、uv都塞入redis里面，然后再获取值保存统计结果，也是比较常用的。


**4:要求每五分钟输出一次从凌晨到当前时间的统计值(类似GTV)**

`实现思路`

从keyBy开始处理，设置1天的滑动窗口，步长为5，在process中使用if判断数据是不是今天的来进行累加，这样过了00:00后，昨天的数据不会被统计，也就实现了业务要求的5分钟输出一次从凌晨到当前时间的统计值.

`代码实现：`
```
StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
env.setParallelism(1);
env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
DataStreamSource<String> localSource = env.socketTextStream("localhost", 8888);

localSource.assignTimestampsAndWatermarks(
    WatermarkStrategy.<Tuple3<String, Integer, String>>forBoundedOutOfOrderness(Duration.ZERO)
                .withTimestampAssigner(new WyTimestampAssigner())
                ).keyBy(t -> t.getShop_name())
                .timeWindow(Time.days(1),Time.minutes(5))
                .process(new ProcessWindowFunction<GoodDetails, Tuple3<String, String, Integer>, String, TimeWindow>() {
 
                    SimpleDateFormat sdf_million = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                    SimpleDateFormat sdf_day = new SimpleDateFormat("yyyy-MM-dd");
 
 
                    @Override
                    public void process(String s, Context ctx, Iterable<GoodDetails> elements, Collector<Tuple3<String, String, Integer>> out) throws Exception {
                        Calendar cld = Calendar.getInstance();
                        Iterator<GoodDetails> iterator = elements.iterator();
                        String curentDay = sdf_day.format(cld.getTimeInMillis() - 180000);  //这里减3分钟是因为，当凌晨 23:55-00:00窗口触发的时候，后面的if判断会不准确，程序走到这里都超过00:00了,减3分钟或者合适的单位都可以
 
                        //计数
                        int countNum = 0;
 
                        while (iterator.hasNext()){
                            GoodDetails next = iterator.next();
                            String elementData = next.getRegion_name().substring(0,10);
 
                            if(elementData.equals(curentDay)){
                                countNum+=next.getGood_price();
                            }
 
                        }
 
                        long end = ctx.window().getEnd();
                        String windowEnd = sdf_million.format(end);
 
                        out.collect(Tuple3.of(windowEnd,s,countNum));
 
                    }
                })
                .name("sum-process").uid("sum-process");

                env.execute();
                
                参考链接：https://blog.csdn.net/m0_49826240/article/details/109704393?spm=1001.2014.3001.5501

```
**5: 滑动窗口中，将数据分配到多个窗口的**

窗口的长度 / 窗口滑动的步长 = 窗口的个数

数据的流向和  TumblingEventTimeWindows 是一样的，所以直接跳到对应数据分配的地方
WindowOperator.processElement，代码比较长，这里就精简一部分

```
@Override
public void processElement(StreamRecord<IN> element) throws Exception {
    // 对应的需要分配的窗口
    final Collection<W> elementWindows = windowAssigner.assignWindows(
        element.getValue(), element.getTimestamp(), windowAssignerContext);

    //if element is handled by none of assigned elementWindows
    boolean isSkippedElement = true;

    final K key = this.<K>getKeyedStateBackend().getCurrentKey();

    if (windowAssigner instanceof MergingWindowAssigner) {
        
    } else {
        // 循环遍历，将数据放到对应的窗口状态的 namesspace 中。算出最后一个窗口的时间后，下面的 for 循环计算出数据对应的所有窗口，并创建一个时间窗口（这个时间窗口，并不是一个窗口，只是窗口的时间，表达一个窗口的开始时间和结束时间）
        for (W window: elementWindows) {

            // drop if the window is already late
            if (isWindowLate(window)) {
                continue;
            }
            isSkippedElement = false;
            // 将数据放到对应的窗口中
            // 一条数据属于多少个窗口分配好了以后，就是把数据放到对应的窗口中了，flink 的窗口对应 state 的 namespace ， 所以放到多个窗口，就是放到多个 namespace 中，对应的代码是：
            windowState.setCurrentNamespace(window);
            windowState.add(element.getValue());

            registerCleanupTimer(window);
        }
    }
}
```
assignWindows 的源码是根据 windowAssigner 的不同而改变的，这里是：SlidingProcessingTimeWindows，对应源码：

```
@Override
public Collection<TimeWindow> assignWindows(Object element, long timestamp, WindowAssignerContext context) {
    timestamp = context.getCurrentProcessingTime();
    List<TimeWindow> windows = new ArrayList<>((int) (size / slide));
    long lastStart = TimeWindow.getWindowStartWithOffset(timestamp, offset, slide);
    for (long start = lastStart;
        start > timestamp - size;
        start -= slide) {
        windows.add(new TimeWindow(start, start + size));
    }
    return windows;
}
```
有个list 存储对应的窗口时间对象，list 的长度就是 窗口的长度 / 滑动的距离 （即一条数据会出现在几个窗口中）

这里用的是处理时间，所有Timestamp 直接从 处理时间中取，数据对应的 最后一个窗口的开始时间 lastStart 就用处理时间传到TimeWindow.getWindowStartWindOffset 中做计算

算出最后一个窗口的开始时间后，减 滑动的距离，就是上一个窗口的开始时间，直到 窗口的开始时间超出窗口的范围

对应的关键就是 lastStart 的计算，看源码：

```
public static long getWindowStartWithOffset(long timestamp, long offset, long windowSize) {
    return timestamp - (timestamp - offset + windowSize) % windowSize;
}
```
没指定 offset ，所以 offset 为0， lastStart =  timestamp - (timestamp - offset + windowSize) % windowSize

windowSize 是 滑动的距离，这里画了个图来说明计算的公式：

![](https://files.mdnice.com/user/37771/2a1c958c-a79e-457c-b360-55f16a2713e4.png)
`自定义滑动窗口`

```
import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.api.common.typeutils.TypeSerializer;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.WindowAssigner;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.triggers.EventTimeTrigger;
import org.apache.flink.streaming.api.windowing.triggers.Trigger;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

/**
 * 自定义实现 window
 */
public class MyEventTimeWindow extends WindowAssigner<Object, TimeWindow> {

    // 窗口的大小
    private final long size;
    // 多长时间滑动一次
    private final long slide;
    // 窗口偏移量
    private final long offset;

    protected MyEventTimeWindow(long size, long slide, long offset) {
        this.size = size;
        this.slide = slide;
        this.offset = offset;
    }

    public static MyEventTimeWindow of(Time size, Time slide, Time offset) {
        return new MyEventTimeWindow(size.toMilliseconds(), slide.toMilliseconds(), offset.toMilliseconds());
    }

    public static MyEventTimeWindow of(Time size, Time slide) {
        return new MyEventTimeWindow(size.toMilliseconds(), slide.toMilliseconds(), 0L);
    }

    @Override
    public Collection<TimeWindow> assignWindows(Object element, long timestamp, WindowAssignerContext windowAssignerContext) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        // 设置从每天的0点开始计算
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        // 获取窗口的开始时间 其实就是 0 点
        long winStart = calendar.getTimeInMillis();
        // 获取窗口的结束时间,就是在开始时间的基础上加上窗口的长度 这里是 1 天
        calendar.add(Calendar.DATE, 1);
        // 获取窗口的结束时间 其实就是第二天的 0 点
        long winEnd = calendar.getTimeInMillis() + 1;
        String format = String.format("window的开始时间:%s,window的结束时间:%s", winStart, winEnd);
        System.out.println(format);
        // 当前数据所属窗口的结束时间
        long currentWindowEnd = TimeWindow.getWindowStartWithOffset(timestamp, this.offset, this.slide) + slide;
        System.out.println(TimeWindow.getWindowStartWithOffset(timestamp, this.offset, this.slide) + "====" + currentWindowEnd);
        // 一条数据属于几个窗口 因为是滑动窗口一条数据会分配到多个窗口里
        int windowCounts = (int) ((winEnd - currentWindowEnd) / slide);
        List<TimeWindow> windows = new ArrayList<>(windowCounts);
        long currentEnd = currentWindowEnd;
        if (timestamp > Long.MIN_VALUE) {
            while (currentEnd < winEnd) {
                windows.add(new TimeWindow(winStart, currentEnd));
                currentEnd += slide;
            }
        }
        return windows;
    }

    @Override
    public Trigger<Object, TimeWindow> getDefaultTrigger(StreamExecutionEnvironment streamExecutionEnvironment) {
        return EventTimeTrigger.create();
    }

    @Override
    public TypeSerializer<TimeWindow> getWindowSerializer(ExecutionConfig executionConfig) {
        return new TimeWindow.Serializer();
    }

    @Override
    public boolean isEventTime() {
        return true;
    }
}
```




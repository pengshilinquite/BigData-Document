# CDH大数据环境优化指南
## 12.1 配置原则
**如何发挥集群最佳性能**

```
原则1：CPU核数分配原则
数据节点：建议预留2～4个核给OS和其他进程(数据库，HBase等)外，其他的核分配给YARN。
控制节点：由于运行的进程较多，建议预留6～8个核。

原则2：内存分配
除了分配给OS、其他服务的内存外，剩余的资源应尽量分配给YARN。

原则3：虚拟CPU个数分配
节点上YARN可使用的虚拟CPU个数建议配置为逻辑核数的1.5～2倍之间。如果上层计算应用对CPU的计算能力要求不高，可以配置为2倍的逻辑CPU。

原则4：提高磁盘IO吞吐率
尽可能挂载较多的盘，以提高磁盘IO吞吐率。
```
**影响性能的因素**
```
因素1：文件服务器磁盘I/O
一般磁盘顺序读写的速度为百兆级别，如第二代SATA盘顺序读的理论速度为300Mbps，只从一个盘里读，若想达到1Gbps每秒的导入速度是不可能的。并且若从一个磁盘读，单纯依靠增加map数来提高导入速率也不一定可以。因为随着map数变多，对于一个磁盘里的文件读，相当由顺序读变成了随机读，map数越多，磁盘读取文件的随机性越强，读取性能反而越差。如随机读最差可变成800Kbps。 因此需要想办法增大文件服务器的磁盘IO读效率，可以使用专业的文件服务器，如NAS系统，或者使用更简单的方法，把多个磁盘进行Raid0或者Raid5。

因素2：文件服务器网络带宽
单个文件服务器的网络带宽越大越好，建议在10000Mb/s以上。

因素3：集群节点硬件配置
集群节点硬件配置越高，如CPU核数和内存都很多，可以增大同时运行的map或reduce个数，如果单个节点硬件配置难以提升，可以增加集群节点数。

因素4：SFTP参数配置
不使用压缩、加密算法优先选择aes128-cbc，完整性校验算法优先选择umac-64@openssh.com

因素5：集群参数配置
因素6：Linux文件预读值
设置磁盘文件预读值大小为16384，使用linux命令：
echo 16384 > /sys/block/sda/queue/read_ahead_kb
```
## 12.2 Manager
**12.2.1 提升Manager配置服务参数的效率**

`操作场景`

在安装集群或者扩容节点以后，集群中可能添加了较多数量的节点。此时如果系统管理员在CDH Manager上修改服务参数、保存新配置并重启服务时，Manager的Controller进程可能占用大量内存，增加了CPU工作负荷，用户需要等待一段时间才能完成参数修改。系统管理员可以根据实际业务使用情况，手动增加Controller的JVM启动参数中内存参数，提升配置服务参数的效率。

`对系统的影响`

该操作需要在主管理节点重新启动Controller，重启期间会造成CDH Manager暂时中断。备管理节点Controller无需重启。

`前提条件`

已确认主备管理节点IP。

`操作步骤`

```
1. 使用PuTTY，以omm用户登录主管理节点。
2. 执行以下命令，切换目录。
cd ${BIGDATA_HOME}/om-server/om/sbin
3. 执行以下命令修改Controller启动参数文件“controller.sh”，并保存退出。
vi controller.sh
修改配置项“JAVA_HEAP_MAX”的参数值。例如，集群中包含了400个以上的节点，建议修改如下，表示Controller最大可使用8GB内存：
JAVA_HEAP_MAX=-Xmx8192m
4. 执行以下命令，重新启动Controller。
sh ${BIGDATA_HOME}/om-server/om/sbin/restart-controller.sh
提示以下信息表示命令执行成功：
End into start-controller.sh
执行sh ${BIGDATA_HOME}/om-server/om/sbin/status-oms.sh，查看Controller的“ResHAStatus”是否为“Normal”，并可以重新登录CDH
Manager表示重启成功。
5. 使用PuTTY，以omm用户登录备管理节点，并重复步骤 2～步骤 3。
```
**12.2.2 根据集群节点数优化Manager配置**

`操作场景`

CDH集群规模不同时，Manager相关参数差异较大。在集群容量调整前或者安装集群时，用户可以手动指定Manager集群节点数，系统将自动调整相关进程参
数。
```
说明：
在安装集群时，可以通过Manager安装配置文件中的“cluster_nodes_scale”参数指定集群节点数。
```
`操作步骤`

```
1. 使用PuTTY，以omm用户登录主管理节点。
2. 执行以下命令，切换目录。
cd ${BIGDATA_HOME}/om-server/om/sbin
3. 执行以下命令查看当前集群Manager相关配置。
sh oms_confifig_info.sh -q
4. 执行以下命令指定当前集群的节点数。
命令格式：sh oms_confifig_info.sh -s 节点数
例如：
sh oms_confifig_info.sh -s 10
根据界面提示，输入“y”：
The following confifigurations will be modifified:
Module Parameter Current Target
Controller controller.Xmx 4096m => 8192m
Controller controller.Xms 1024m => 2048m
...
Do you really want to do this operation? (y/n):
界面提示以下信息表示配置更新成功：
...
Operation has been completed. Now restarting OMS server. [done]
Restarted oms server successfully.
    说明：
        配置更新过程中，OMS会自动重启。
        相近数量的节点规模对应的Manager相关配置是通用的，例如100节点变为101节点，并没有新的配置项需要刷新。
```
## 12.2.3 优化CM
这些服务主要是提供监控功能，目前的调整主要集中在内存放，以便有足够的资源 完成集群管理。
![](https://files.mdnice.com/user/37771/772fe62d-8695-4449-b88c-523dfede365b.png)
## 12.4 HBase优化
**1 提升BulkLoad效率**

`操作场景`

批量加载功能采用了MapReduce jobs直接生成符合HBase内部数据格式的文件，然后把生成的StoreFiles文件加载到正在运行的集群。使用批量加载相比直接使用
HBase的API会节约更多的CPU和网络资源。
ImportTSV是一个HBase的表数据加载工具。

`前提条件`

在执行批量加载时需要通过“Dimporttsv.bulk.output”参数指定文件的输出路径。
`操作步骤`

参数入口：执行批量加载任务时，在BulkLoad命令行中加入如下参数。
![](https://files.mdnice.com/user/37771/9e6ade06-3ea1-4238-bde8-19af22cfef70.png)

**2 提升连续put场景性能**

`操作场景`

对大批量、连续put的场景，配置下面的两个参数为“false”时能大量提升性能。
“hbase.regionserver.wal.durable.sync”
“hbase.regionserver.hfifile.durable.sync”
当提升性能时，缺点是对于DataNode（默认是3个）同时故障时，存在小概率数据丢失的现象。对数据可靠性要求高的场景请慎重配置。

`操作步骤`
![](https://files.mdnice.com/user/37771/cfe0b8e9-c5c5-4bbf-aaf4-72ae1928858c.png)
**3 Put和Scan性能综合调优**

`操作场景`

HBase有很多与读写性能相关的配置参数。读写请求负载不同的情况下，配置参数需要进行相应的调整，本章节旨在指导用户通过修改RegionServer配置参数进行读写性能调优。
`操作步骤`
JVM GC参数
```
RegionServer GC_OPTS参数设置建议：
-Xms与-Xmx设置相同的值，需要根据实际情况设置，增大内存可以提高读写性能，可以参考参数“hfifile.block.cache.size”（见表12-4）和参数“hbase.regionserver.global.memstore.size”（见表12-3）的介绍进行设置。
-XX:NewSize与-XX:MaxNewSize设置相同值，建议低负载场景下设置为“512M”，高负载场景下设置为“2048M”。
-XX:CMSInitiatingOccupancyFraction建议设置为“100 * (hfifile.block.cache.size + hbase.regionserver.global.memstore.size + 0.05)”，最大值不
超过90。-XX:MaxDirectMemorySize表示JVM使用的堆外内存，建议低负载情况下设置为“512M”，高负载情况下设置为“2048M”。
```

Put相关参数
```
RegionServer处理put请求的数据，会将数据写入memstore和hlog，
当memstore大小达到设置的“hbase.hregion.memstore.flflush.size”参数值大小时，memstore就会刷新到HDFS生成HFile。
当当前region的列簇的HFile数量达到“hbase.hstore.compaction.min”参数值时会触发compaction。
当当前region的列簇HFile数达到“hbase.hstore.blockingStoreFiles”参数值时会阻塞memstore刷新生成HFile的操作，导致put请求阻塞。
```
![](https://files.mdnice.com/user/37771/5c9642fe-a4fb-4a25-8da0-9910de343d41.png)
![](https://files.mdnice.com/user/37771/62769344-6a37-41b2-ada8-a1417b99118e.png)
![](https://files.mdnice.com/user/37771/24eb47d7-1189-4602-84b6-941cca62ef75.png)


**4 提升实时写数据效率**

`操作场景`

需要把数据实时写入到HBase中或者对于大批量、连续put的场景。

`前提条件`

调用HBase的put或delete接口，把数据保存到HBase中。

`操作步骤`

`写数据服务端调优`

参数入口：
在CDH Manager系统中，选择“服务管理 > HBase > 服务配置”，“参数类别”类型设置为“全部配置”。在搜索框中输入参数名称。
影响实时写数据配置项
![](https://files.mdnice.com/user/37771/34b1d215-c44f-462f-af07-b1fd89ca00ff.png)
![](https://files.mdnice.com/user/37771/6ca26039-6456-4fd1-8fad-e67b6bb703bf.png)
![](https://files.mdnice.com/user/37771/860bd85d-048a-4f41-a700-c1d743f92d34.png)
![](https://files.mdnice.com/user/37771/c37526fa-dae5-4f44-8d4d-3745324d6423.png)
![](https://files.mdnice.com/user/37771/442d1fe3-07e8-4aee-8ab8-560980880351.png)
`写数据客户端调优`

写数据时，在场景允许的情况下，最好使用Put List的方式，可以极大的提升写性能。每一次Put的List的长度，需要结合单条Put的大小，以及实际环境的一些参数进行设定。建议在选定之前先做一些基础的测试。
`写数据表设计调优`

表12­7 影响实时写数据相关参数 
![](https://files.mdnice.com/user/37771/04b5ce81-859a-42d7-8b71-4d6119dff685.png)
**5 提升实时读数据效率**

`操作场景`

需要读取HBase数据场景。

`前提条件`

调用HBase的get或scan接口，从HBase中实时读取数据。

`操作步骤`

读数据服务端调优
参数入口：
在CDH Manager系统中，选择“服务管理 > HBase > 服务配置”，“参数类别”类型设置为“全部配置”。在搜索框中输入参数名称。
表12­8 影响实时写数据配置项 
![](https://files.mdnice.com/user/37771/c524a37c-2bde-4d2e-bc26-ca939ad5ecaa.png)
```
说明： 如果同时存在读和写的操作，这两种操作的性能会互相影响。如果写入导致的flush和Compaction操作频繁发生，会占用大量的磁盘IO操作，<br>从而影响 读取的性能。如果写入导致阻塞较多的Compaction操作，就会出现Region中存在多个HFile的情况，从而影响读取的性能。<br>所以如果读取的性能不理 想的时候，也要考虑写入的配置是否合理。
```

`读数据客户端调优`

Scan数据时需要设置caching（一次从服务端读取的记录条数，默认是1），若使用默认值读性能会降到极低。
当不需要读一条数据所有的列时，需要指定读取的列，以减少网络IO。
只读取RowKey时，可以为Scan添加一个只读取RowKey的fifilter（FirstKeyOnlyFilter或KeyOnlyFilter）。

`读数据表设计调优`

表12­9 影响实时读数据相关参数
![](https://files.mdnice.com/user/37771/05d92e11-f89f-4fc9-904a-1bf701865bdb.png)
**6 JVM参数优化操作场景**

当集群数据量达到一定规模后，JVM的默认配置将无法满足集群的业务需求，轻则集群变慢，重则集群服务不可用。所以需要根据实际的业务情况进行合理的JVM参数
配置，提高集群性能。

`操作步骤`

参数入口：
HBase角色相关的JVM参数需要配置在“${HBASE_HOME}/conf”目录下的“hbase-env.sh”文件中。
每个角色都有各自的JVM参数配置变量，如表12-10。
表12­10 HBase相关JVM参数配置变量
![](https://files.mdnice.com/user/37771/781059c0-77a7-4ac1-b2d5-ab4d299c6ae0.png)
## 12.5 Hdfs优化
![](https://files.mdnice.com/user/37771/e8ee724a-3f20-46a6-bd11-94a08b1dd088.png)
**1 提升写性能**

`操作场景`

在HDFS中，通过调整属性的值，使得HDFS集群更适应自身的业务情况，从而提升HDFS的写性能。

`操作步骤`
参数入口：
在CDH Manager系统中，选择“服务管理 > HDFS > 服务配置”，“参数类别”类型设置为“全部配置”。在搜索框中输入参数名称。
表12­11 HDFS写性能优化配置
![](https://files.mdnice.com/user/37771/0dc11ac9-b2cb-4f2f-b988-0fcd9e0ae175.png)
**2 JVM参数优化**

`操作场景`

当集群数据量达到一定规模后，JVM的默认配置将无法满足集群的业务需求，轻则集群变慢，重则集群服务不可用。所以需要根据实际的业务情况进行合理的JVM参数
配置，提高集群性能。

`操作步骤`

参数入口：
HDFS角色相关的JVM参数需要配置在“${HADOOP_HOME}/etc/hadoop”目录下的“hadoop-env.sh”文件中。
JVM各参数的含义请参见其官网：http://docs.oracle.com/javase/8/docs/technotes/tools/unix/java.html
每个角色都有各自的JVM参数配置变量，如表12-12。
表12­12 HDFS相关JVM参数配置变量
![](https://files.mdnice.com/user/37771/f4eda414-8228-4f74-ba9c-175acdd69df5.png)
```
配置方式举例：
export HADOOP_NAMENODE_OPTS="-Dhadoop.security.logger=${HADOOP_SECURITY_LOGGER:-INFO,RFAS} -Dhdfs.audit.logger=${HDFS_AUDIT_LOGGER:-INFO,N
```
**3 使用客户端元数据缓存提高读取性能**

`操作场景`

通过使用客户端缓存元数据块的位置来提高HDFS读取性能。
```
说明：
此功能仅用于读取不经常修改的文件。因为在服务器端由某些其他客户端完成的数据修改，对于高速缓存的客户端将是不可见的，这可能导致从缓存中拿到的元数据是过期的。　
```

`操作步骤`

设置参数的路径：
在CDH Manager页面中，选择“服务管理 > HDFS > 服务配置”，将“参数类别”设置为“全部配置”，并在搜索框中输入参数名称。
表12­13 参数配置
![](https://files.mdnice.com/user/37771/3a3768f9-66d4-4920-9ea8-6f85df56c1cc.png)
```
说明：
要在过期前完全清除客户端缓存，可调用DFSClient#clearLocatedBlockCache()。
用法如下所示。
FileSystem fs = FileSystem.get(conf);
DistributedFileSystem dfs = (DistributedFileSystem) fs;
DFSClient dfsClient = dfs.getClient();
dfsClient.clearLocatedBlockCache();
```
**4 使用当前活动缓存提升客户端与NameNode的连接性能**

`操作场景`

HDFS部署在具有多个NameNode实例的HA（High Availability）模式中，HDFS客户端需要依次连接到每个NameNode，以确定当前活动的NameNode是什么，并将其用于客户端操作。
一旦识别出来，当前活动的NameNode的详细信息就可以被缓存并共享给在客户端机器中运行的所有客户端。这样，每个新客户端可以首先尝试从缓存加载活动的
Name Node的详细信息，并将RPC调用保存到备用的NameNode。在异常情况下有很多优势，例如当备用的NameNode连接长时间不响应时。
当发生故障，将另一个NameNode切换为活动状态时，缓存的详细信息将被更新为当前活动的NameNode的信息。

`操作步骤`

设置参数的路径如下：在CDH Manager页面中，选择“服务管理 > HDFS > 服务配置”，将“参数类别”设置为“全部配置”，并在搜索框中输入参数名称。
表12­14 配置参数
![](https://files.mdnice.com/user/37771/fdeb6f1c-9bbb-4e93-a5fa-030ff8c83b14.png)

## 12.6 zookeeper优化
![](https://files.mdnice.com/user/37771/df4b39c1-7bd0-4d7e-bfcc-e7c91e3d714e.png)
## 12.7 kafka优化
![](https://files.mdnice.com/user/37771/63abdb0d-9963-49e1-b815-b83e3b250ab9.png)
## 12.8 YARN + MapReduce优化
![](https://files.mdnice.com/user/37771/914c690a-b574-4086-817b-1b986c688e4d.png)
![](https://files.mdnice.com/user/37771/0567e4fb-1093-43bd-8015-354b285e0107.png)
## 12.8 Hive优化
![](https://files.mdnice.com/user/37771/5448832f-6767-4e09-835b-b4a51786c165.png)
![](https://files.mdnice.com/user/37771/ab3a217d-7a3c-4f2c-a6c6-f3afde28dc11.png)
## 12.9 kudu优化
![](https://files.mdnice.com/user/37771/522d987c-e1ef-43c2-aa0a-9bab9606ac53.png)

```
1.Kudu后台对数据进行维护操作，如写入数据时的并发线程数，一般设置为4，官网建议的是数据目录的3倍
   Kudu Tablet Server Maintenance Threads 这个参数决定了Kudu后台对数据进行维护操作，如写入数据时的并发线程数。并发数越大，吞吐量越高，但对集群计算能力的要求也越高。默认值为1，表示Kudu会采用单线程操作；对于需要大量数据进行快速写入/删除的集群，可以设置更大的值。该值可以设置跟计算节点的数据磁盘数量和CPU核数有关，一般来说，建议设置为4以获取比较均衡的性能，最大不超过8。
    参数：maintenance_manager_num_threads
2.分配给Kudu Tablet Server块缓存的最大内存量，建议是2-4G
    Kudu Tablet Server Block Cache Capacity Tablet的Block buffer cache，根据集群内存配置和数据量规模设置。一般建议至少2GB～4GB。
    参数：block_cache_capacity_mb
3.Tablet Server能使用的最大内存量，有多大，设置多大。
    tablet Server在批量写入数据时并非实时写入磁盘，而是先Cache在内存中， 在flush到磁盘。这个值设置过小时，会造成Kudu数据写入性能显著下降。
    对于写入性能要求比较高的集群，建议设置更大的值（一般是机器内存的百分之80） Kudu Tablet Server Hard Memory Limit Kudu的Tablet Server能使用的最大内存。
    参数：memory_limit_hard_bytes
4.参数决定了Kudu能够同时打开的操作系统文件数。不设置则使用系统的ulimits值，设置后会覆盖系统的设置。
    需要根据集群的规模及并发处理能力，非常谨慎的设置这个值。
    参数：Maximum Process File Descriptors 
5.参数设置了每个Tablet的默认复制因子，默认值为3，表示每个表的数据会在Kudu中存储3份副本。
    我们可以根据需要修改这个全局默认值，也可以在建表语句中通过’kudu.num_tablet_replicas’属性来设置每个表的副本数，
    参数：kudu.num_tablet_replicas=1
6.tserver宕掉后，5分钟后没有恢复的情况下，该机器上的tablet会移动到其他机器 
    参数：--follower_unavailable_considered_failed_sec=300 
7.超过参数时间的历史数据会被清理，如果是base数据不会被清理。而真实运行时数据大小持续累加，没有被清理。 
    参数：--tablet_history_max_age_sec=900
8.hash分区数量 * range分区数量不能超过60个（1.7.0版本之后没限制了）
9.设置block的管理器为文件管理器（默认是日志服务器）
    解释：并非所有文件系统格式都需要设置该选项。ext4、xfs格式支持hole punching（打孔），所以不需要设置block_manager=file，但是ext3 格式需要。可以通过df -Th命令来查看文件系统的格式。
    参数：--block_manager=file
10.设置ntp服务器的时间误差不超过20s（默认是10s）
    参数：max_clock_sync_error_usec=20000000
11.设置rpc的连接时长（默认是3s，建议不要设置）
    参数：--rpc_negotiation_timeout_ms=300000
12.设置rpc一致性选择的连接时长（默认为1s，建议不要设置）
    参数：--consensus_rpc_timeout_ms=1000
13.记录kudu的crash的信息
    解释：
        Kudu在遇到崩溃时，使用Google Breakpad库来生成minidump。这些minidumps的大小通常只有几MB，即使禁用了核心转储生成，也会生成，
        生成minidumps只能在Linux上建立。
        minidump文件包含有关崩溃的进程的重要调试信息，包括加载的共享库及其版本，崩溃时运行的线程列表，处理器寄存器的状态和每个线程的堆栈内存副本，
        以及CPU和操作系统版本信息。
        Minitump可以通过电子邮件发送给Kudu开发人员或附加到JIRA，以帮助Kudu开发人员调试崩溃。为了使其有用，
        开发人员将需要知道Kudu的确切版本和发生崩溃的操作系统。请注意，虽然minidump不包含堆内存转储，但它确实包含堆栈内存，
        因此可以将应用程序数据显示在minidump中。如果机密或个人信息存储在群集上，请不要共享minidump文件。
    参数：
        --minidump_path=minidumps              
        --max_minidumps=9
        （默认是在设置的log目录下生成minidumps目录，里边包含最多9个以dmp结尾的文件，无法设置为空值，需要注意的是如果自定义minidump文件，
        在master不能启动的情况下，需要将该目录中的文件删除）
14.Stack WatchLog
    解释：每个Kudu服务器进程都有一个称为Stack Watchdog的后台线程，它监视服务器中的其他线程，以防它们被阻塞超过预期的时间段。
          这些跟踪可以指示操作系统问题或瓶颈存储。通过WARN日志信息的跟踪（Trace）可以用于诊断由于Kudu以下的系统（如磁盘控制器或文件系统）引起的根本原因延迟问题。
15.cdh设置多master
    参数：--master_addresses=cdh01:7051,cdh02:7051cdh03:7051
16.kudu出现启动速度特别慢
    解决办法：
        1、取消所有配置参数（除了资源、时间同步）
        2、升级版本到kudu1.6.0
        3、client必须停止（client不占用io的情况，3台机器，每台机器60G，127分区数量，启动速度3分钟）
        4、查看io使用情况 iostat -d -x -k 1 200
17.单hash分区最大是60
18.安装kudu过程中，会要求CPU支持ssc4.2指令集，但是我们的虚拟机cpu没有这个执行集，所以无法安装
19.设置client长连接过期时间
    参数：--authn_token_validity_seconds=12960000（150天）
    注意：设置到tserver的配置文件中
20.tserver和master的wal和data目录要分隔（或者是目录设置为lvm卷轴）
    原因：wal目录只能设置为1个
    参数：--fs_wal_dir_reserved_bytes
    解释：
        Number of bytes to reserve on the log directory filesystem for non-Kudu usage. The default,
        which is represented by -1, is that 1% of the disk space on each disk will be reserved.     
        Any other value specified represents the number of bytes reserved and must be greater than or equal to 0. 
        Explicit percentages to reserve are not currently supported
        用于非kudu都使用的日志目录文件系统的字节数，默认情况下是-1，每个磁盘上的磁盘空间的1%将被保留，指定的任何其他值表示保留的字节数，必须大于或等于0。
21.设置用户权限，能移动tablet
    参数：--superuser_acl=*
	
参数调优核心总结为两个字：平衡。
1、时效和稳定性的平衡；
2、资源的平衡，在某一时间点，集群的内存、io、cpu等负载均衡。	
```


# CDH的组件java调优建议值 
![](https://files.mdnice.com/user/37771/3dbade95-e4c7-4e53-a2f7-c2ee758d6eb9.png)
![](https://files.mdnice.com/user/37771/96006440-7806-4f37-b55c-b6f31b7f2025.png)

# 常见优化参数

```
1:hdfs相关优化

1.dfs.block.size
HDFS中的数据block大小，默认是64M，对于较大集群，可以设置为128或264M

2.dfs.datanode.socket.write.timeout
增加dfs.datanode.socket.write.timeout和dfs.socket.timeout两个属性的时间，避免出现IO超时

3.dfs.datanode.max.transfer.threads
增加datanode在进行文件传输过程中的最大线程数。默认值4096，可修改为8192

4.dfs.namenode.handler.count
NameNode中用于处理RPC调用的线程数,即指定NameNode 的服务器线程的数量。NameNode有一个工作线程池用来处理客户端的远程过程调用及集群守护进程的调用，处理程序数量越多意味着要更大的池来处理来自不同DataNode的并发心跳以及客户端并发的元数据操作）。 对于大集群或者有大量客户端的集群来说，通常需要增大参数dfs.namenode.handler.count的默认值10。设置该值的一般原则是将其设置为集群大小的自然对数乘以20，即20logN，N为集群大小。　
果该值设的太小，明显的状况就是DataNode在连接NameNode的时候总是超时或者连接被拒绝，但NameNode的远程过程调用队列很大时，远程过程调用延时就会加大。症状之间是相互影响的，很难说修改dfs.namenode.handler.count就能解决问题，但是在查找故障时，检查一下该值的设置是必要的。如果前面的描述你仍然觉得很不清楚，可以看下面的python程序，假设我们大数据集群的大小为500台，那么我们可以直接看怎么计算应该设置合理值。

5.dfs.datanode.handler.count
数据节点的服务器线程数，默认为10。可适当增加这个数值来提升DataNode RPC服务的并发度。 在DataNode上设定,取决于系统的繁忙程度,设置太小会导致性能下降甚至报错。线程数的提高将增加DataNode的内存需求，因此，不宜过度调整这个数值。

6.dfs.namenode.avoid.read.stale.datanode
指示是否避免读取“过时”的数据节点（DataNode），这些数据节点（DataNode）的心跳消息在指定的时间间隔内未被名称节点（NameNode）接收。过时的数据节点（DataNode）将移动到返回供读取的节点列表的末尾。有关写入的类似设置，请参阅df.namenode.avoint.write.stale.datanode。默认值是flase，推荐设置为true。

7.dfs.namenode.avoid.write.stale.datanode
指示超过失效 DataNode 时间间隔 NameNode 未收到检测信号信息时是否避免写入失效 DataNode。写入应避免使用失效 DataNode，除非多个已配置比率 (dfs.namenode.write.stale.datanode.ratio) 的 DataNode 标记为失效。有关读取的类似设置，请参阅 dfs.namenode.avoid.read.stale.datanode。 默认值是flase，推荐设置为true。

8.fs.trash.interval
注：该配置底层文件在core-default.xml
垃圾桶检查点之间的分钟数。还可控制删除垃圾桶检查点目录后的分钟数。要禁用垃圾桶功能，请输入 0。 默认为禁用垃圾桶功能。为防止重要文件误删，可启用该特性。

9.hive.merge.sparkfiles
在 Spark 作业结束时合并小文件。如启用，将创建 map-only 作业以合并目标表/分区中的文件。

2: Yarn相关优化
Nodemanager:
#每个节点最大使用的vcore数，可以适当放大(一般以2倍去放大，提高CPU的使用率)
yarn.nodemanager.resource.cpu-vcores:64
#调整每台物理机器可以被调度的内存资源
yarn.nodemanager.resource.memory-mb
#物理内存和虚拟内存的比例,任务每使用1MB物理内存，最多可使用虚拟内存量，默认为 2.1
yarn.nodemanager.vmem-pmem-ratio=50
#是否启动一个线程检查每个任务正使用的物理内存量，如果任务超出分配值，则直接将其杀掉，默认是true
yarn.nodemanager.pmem-check-enabled
#是否启动一个线程检查每个任务正使用的虚拟内存量，如果任务超出分配值，则直接将其杀掉，默认是true
yarn.nodemanager.vmem-check-enabled

ResourceManager:(yarn-site.xml 中设置 container)
#最小可申请内存量， 默认1024,如果一个任务申请的物理内存量少于该值，则该对应的值改为这个数
yarn.scheduler.minimum-allocation-mb:1024
#单个 container 最大可申请内存量， 默认是 8069（不能超过物理机的最大内存）
yarn.scheduler.maximum-allocation-mb: 8069
#最小可申请CPU数， 默认是 1
yarn.scheduler.minimum-allocation-vcores: 1
#单个container最大可申请CPU数（也是tm最大可设置的slot，和物理机核数保持一致即可）
yarn.scheduler.maximum-allocation-vcores: 

ApplicationMaster:
#分配给 Map Container的内存大小，运行时按需指定
mapreduce.map.memory.mb=2048
#分配给 Reduce Container的内存大小，运行时按需指定
mapreduce.reduce.memory.mb=4096
#单个任务可申请的最小虚拟CPU个数, 默认是1
yarn.scheduler.minimum-allocation-vcores
#单个任务可申请的最多虚拟CPU个数（不能超过物理机的最大CPU个数）
yarn.scheduler.maximum-allocation-vcores

3：Kafka相关优化
broker端：

## 此设置用于设置 message.max.bytes 参数来限制单个消息的大小，默认值是 10000 000，也就是 1MB
message.max.bytes

## 建议设置为CPU核心数/4，适当提高可以提升CPU利用率及 Follower同步 Leader数据当并行度
num.replica.fetchers

## 处理消息的最大线程数。broker 处理消息的最大线程数，默认为 3，建议设为 cpu 核数 + 1
num.network.threads 

## broker 处理磁盘 IO 的线程数，建议设为 cpu 核数 x 2 。
num.io.threads

## 表示是否开启leader自动负载均衡，默认true；我们应该把这个参数设置为false，因为自动负载均衡不可控，可能影响集群性能和稳定。
auto.leader.rebalance.enable

## 在阻塞网络线程之前允许的排队请求数
queued.max.requests	

数据落盘策略
##每当producer写入10000条消息时，刷数据到磁盘
log.flush.interval.messages=10000
##每间隔5秒钟时间，刷数据到磁盘
log.flush.interval.ms=5000

segment 分段存储策略
##日志滚动的周期时间，到达指定周期时间时，强制生成一个新的segment
log.roll.hours=72

## segment的索引文件最大尺寸限制，即时log.segment.bytes没达到，也会生成一个新的segment
log.index.size.max.bytes=10*1024*1024

##控制日志segment文件的大小，超出该大小则追加到一个新的日志segment文件中（-1表示没有限制）
log.segment.bytes=1024*1024*1024


基础配置
## 是否允许自动创建topic，若是false，就需要通过命令创建topic
auto.create.topics.enable =true

## 默认副本的数量，可以根据 Broker 的个数进行设置。
default.replication.factor = 3

## 默认，每个topic的分区个数，若是在topic创建时候没有指定的话会被topic创建时的指定参数覆盖
num.partitions = 3

## 消息体的最大大小，单位是字节，如果发送的消息过大，可以适当的增大该参数
message.max.bytes = 6525000

## socket的发送缓冲区的大小
socket.send.buffer.bytes=102400

## socket的接受缓冲区的大小
socket.request.max.bytes=104857600

副本同步策略
## 默认10s，isr中的follow没有向isr发送心跳包就会被移除
replica.lag.time.max.ms = 10000

## 根据leader 和副本的信息条数差值决定是否从isr 中剔除此副本，此信息条数差值根据配置参数，在broker数量较少,或者网络不足的环境中,建议提高此值.
replica.lag.max.messages = 4000

## follower与leader之间的socket超时时间
replica.socket.timeout.ms=30*1000

## 数据同步时的socket缓存大小
replica.socket.receive.buffer.bytes=64*1024

## replicas每次获取数据的最大大小
replica.fetch.max.bytes =1024*1024

## replicas同leader之间通信的最大等待时间，失败了会重试
replica.fetch.wait.max.ms =500

## fetch的最小数据尺寸，如果leader中尚未同步的数据不足此值,将会阻塞,直到满足条件
replica.fetch.min.bytes =1

## leader进行复制的线程数，增大这个数值会增加follower的IO
num.replica.fetchers=1

## 每个replica检查是否将最高水位进行固化的频率
replica.high.watermark.checkpoint.interval.ms = 5000

## leader的不平衡比例，若是超过这个数值，会对分区进行重新的平衡
leader.imbalance.per.broker.percentage = 10

## 检查leader是否不平衡的时间间隔
leader.imbalance.check.interval.seconds = 300


producer端参数：

ack
## kakfa消息语义。

buffer.memory
## 该参数用来设置生产者内存缓冲区的大小，生产者用它缓冲要发送到服务器的消息。如果应用程序发送消息的速度超过发送到服务器的速度，会导致生产者空间不足。
这个时候， send() 方法调用要么被阻塞，要么抛出异常，取决于如何设置block.on.buffer.full 参数（在 0.9.0.0 版本里被替换成了 max.block.ms ，表示在抛出异常之前可以阻塞一段时间）。

compression.type
## 默认情况下，消息发送时不会被压缩。该参数可以设置为 snappy 、gzip 或 lz4。
snappy 压缩算法占用较少的 CPU，却能提供较好的性能和相当可观的压缩比，如果比较关注性能和网络带宽，可以使用这种算法。
gzip 压缩算法一般会占用较多的 CPU，但会提供更高的压缩比，所以如果网络带宽比较有限，可以使用这种算法。
使用压缩可以降低网络传输开销和存储开销，而这往往是向 Kafka 发送消息的瓶颈所在。

retries
## 消息重试机制。默认情况下，生产者会在每次重试之间等待 100ms，不过可以通过 retry.backoff.ms 参数来改变这个时间间隔。

batch.size
## 批量发送消息大小。该参数指定了一个批次可以使用的内存大小，按照字节数计算（而不是消息个数）。

linger.ms
## 该参数指定了生产者在发送批次之前等待更多消息加入批次的时间。KafkaProducer 会在批次填满或 linger.ms 达到上限时把批次发送出去。
默认情况下，只要有可用的线程，生产者就会把消息发送出去，就算批次里只有一个消息。

max.in.flight.requests.per.connection
## 该参数指定了生产者在收到服务器响应之前可以发送多少个消息。它的值越高，就会占用越多的内存，不过也会提升吞吐量。
把它设为 1 可以保证消息是按照发送的顺序写入服务器的，即使发生了重试。

timeout.ms、request.timeout.ms 和 metadata.fetch.timeout.ms
## request.timeout.ms 指定了生产者在发送数据时等待服务器返回响应的时间，
## metadata.fetch.timeout.ms 指定了生产者在获取元数据（比如目标分区的首领是谁）时等待服务器返回响应的时间。

max.block.ms
## 该参数指定了在调用 send() 方法或使用 partitionsFor() 方法获取元数据时生产者的阻塞时间。
当生产者的发送缓冲区已满，或者没有可用的元数据时，这些方法就会阻塞。
在阻塞时间达到 max.block.ms 时，生产者会抛出超时异常。

max.request.size
## 该参数用于控制生产者发送的请求大小。它可以指能发送的单个消息的最大值，也可以指单个请求里所有消息总的大小。

receive.buffer.bytes 和 send.buffer.by
## 这两个参数分别指定了 TCP socket 接收和发送数据包的缓冲区大小。如果它们被设为 -1，就使用操作系统的默认值。
如果生产者或消费者与 broker 处于不同的数据中心，那么可以适当增大这些值，因为跨数据中心的网络一般都有比较高的延迟和比较低的带宽。

max.in.flight.requests.per.connection
## 客户端在阻塞之前将在单个连接上发送的未确认请求的最大数量。

Consumer常用参数：

fetch.min.bytes
该属性指定了消费者从服务器获取记录的最小字节数。如果没有很多可用数据，但消费者的 CPU 使用率却很高，那么就需要把该属性的值设得比默认值大。
如果消费者的数量比较多，把该属性的值设置得大一点可以降低 broker 的工作负载。

fetch.max.wait.ms
指定 broker 的等待时间，默认是 500ms。如果要降低潜在的延迟（为了满足 SLA），可以把该参数值设置得小一些。
如果 fetch.max.wait.ms 被设为 100ms，并且 fetch.min.bytes 被设为 1MB，那么 Kafka 在收到消费者的请求后，
要么返回 1MB 数据，要么在 100ms 后返回所有可用的数据，就看哪个条件先得到满足。

max.partition.fetch.bytes
该属性指定了服务器从每个分区里返回给消费者的最大字节数。它的默认值是 1MB。在为消费者分配内存时，可以给它们多分配一些，因为如果群组里有消费者发生崩溃，剩下的消费者需要处理更多的分区。
max.partition.fetch.bytes 的值必须比 broker 能够接收的最大消息的字节数（通过 max.message.size 属性配置）大，否则消费者可能无法读取这些消息，导致消费者一直挂起重试。
在设置该属性时，另一个需要考虑的因素是消费者处理数据的时间。消费者需要频繁调用 poll() 方法来避免会话过期和发生分区再均衡，如果单次调用 poll() 返回的数据太多，
消费者需要更多的时间来处理，可能无法及时进行下一个轮询来避免会话过期。如果出现这种情况，可以把 max.partition.fetch.bytes 值改小，或者延长会话过期时间。

session.timeout.ms
该属性指定了消费者在被认为死亡之前可以与服务器断开连接的时间，默认是 3s。
该属性与 heartbeat.interval.ms 紧密相关。heartbeat.interval.ms 指定了 poll() 方法向协调器发送心跳的频率，session.timeout.ms 则指定了消费者可以多久不发送心跳。
所以，一般需要同时修改这两个属性，heartbeat.interval.ms 必须比 session.timeout.ms 小，一般是 session.timeout.ms 的三分之一

auto.offset.reset
该属性指定了消费者在读取一个没有偏移量的分区或者偏移量无效的情况下（因消费者长时间失效，包含偏移量的记录已经过时并被删除）该作何处理。它的默认值是 latest 

enable.auto.commit
该属性指定了消费者是否自动提交偏移量，默认值是 true 。为了尽量避免出现重复数据和数据丢失，可以把它设为 false ，由自己控制何时提交偏移量。
如果把它设为 true ，还可以通过配置 auto.commit.interval.ms 属性来控制提交的频率。

partition.assignment.strategy
通过设置partition.assignment.strategy 来选择分区策略，有Range，RoundRobin，sticky三种策略。

max.poll.records
该属性用于控制单次调用 call() 方法能够返回的记录数量，可以帮你控制在轮询里需要处理的数据量。

eceive.buffer.bytes 和 send.buffer.bytes
socket 在读写数据时用到的 TCP 缓冲区也可以设置大小。如果它们被设为 -1，就使用操作系统的默认值。如果生产者或消费者与 broker 处于不同的数据中心内，
可以适当增大这些值，因为跨数据中心的网络一般都有比较高的延迟和比较低的带宽。

max.poll.interval .ms
一次poll间隔最大时间

fetch.max.bytes
consumer端一次拉取数据的最大字节数

connections.max.idle .ms
consumer默认和kafka broker建立长连接，当连接空闲时间超过该参数设置的值，那么将连接断开，当使用时，在进行重新连接


4：impala相关优化
default_pool_max_requests 已将其设置为 500（单池最大运行查询数，若超过500，Impala 则拒绝查询）
mem_limit  已将其设置为 30G (Impala Daemon 内存限制，若超过此范围 Impala 将抛出内存不足异常)
#指定取消空闲查询的时间(以秒为单位),一个查询在被取消之前可能处于空闲状态的时间(即没有完成任何处理工作，也没有从客户端收到更新)。如果为0，则空闲查询永远不会过期。
idle_query_timeout
#一个会话在被Impala关闭(并且所有正在运行的查询都被取消)之前可能处于空闲状态的时间。如果为0，则空闲会话永远不会过期。
idle_session_timeout

#服务客户端请求的可用线程数 default=64
fe_service_threads
#(Advanced) Number of threads available to start fragments on remote Impala daemons.  default=12
coordinator_rpc_threads 
#Number of remote HDFS I/O threads	 default=8
num_remote_hdfs_io_threads 
#时区的设置   应为 true    默认 false
use_local_tz_for_unix_timestamp_conversions

5：spark优化

基础配置
spark.executor.memory
指定Executor memory,也就是Executor可用内存上限

spark.memory.offHeap.enabled
堆外内存启用开关

spark.memory.offHeap.size
指定堆外内存大小

spark.memory.fraction
堆内内存中，Spark缓存RDD和计算的比例

spark.memory.storageFraction
Spark缓存RDD的内存占比，相应的执行内存比例为1 - spark.memory.storageFraction

spark.local.dir
Spark指定的临时文件目录

spark.cores.max
一个Spark程序能够给申请到的CPU核数

spark.executor.cores
单个Executor的核心数

spark.task.cpus
单个task能够申请的cpu数量

spark.default.parallelism
默认并行度

spark.sql.shuffle.partitions
Shuffle过程中的Reducer数量

Shuffle配置
spark.shuffle.file.buffer
设置shuffle write任务的bufferedOutputStream的缓冲区大小。将数据写入磁盘文件之前，将其写入缓冲区，然后在将缓冲区写入磁盘后将其填充。

spark.reducer.maxSizeInFlight
该参数用于设置Shuffle read任务的buff缓冲区大小，该缓冲区决定一次可以拉取多少数据。

spark.shuffle.sort.bypassMergeThreshold
当ShuffleManager为SortShuffleManager时，如果shuffle read task的数量小于这个阈值（默认是200），则shuffle write过程中不会进行排序操作，而是直接按照未经优化的HashShuffleManager的方式去写数据，但是最后会将每个task产生的所有临时磁盘文件都合并成一个文件，并会创建单独的索引文件。

Spark SQL配置
spark.sql.adaptive.enabled
Spark AQE开启开关

spark.sql.adaptive.coalescePartitions.enabled
是否开启合并小数据分区，默认开启

spark.sql.adaptive.advisoryPartitionSizeInBytes
倾斜数据分区拆分，小数据分区合并优化时，建议的分区大小

spark.sql.adaptive.coalescePartitions.minPartitionNum
合并后最小的分区数

spark.sql.adaptive.fetchShuffleBlocksInBatch
是否批量拉取blocks,而不是一个个的去取。给同一个map任务一次性批量拉取blocks可以减少IO提高性能

spark.sql.adaptive.skewJoin.enabled
自动倾斜处理，处理sort-merge join中的倾斜数据

spark.sql.adaptive.skewJoin.skewedPartitionFactor
判断分区是否是倾斜分区的比例。
当一个 partition 的 size 大小大于该值（所有 parititon 大小的中位数）且大于spark.sql.adaptive.skewedPartitionSizeThreshold，或者 parition 的条数大于该值（所有 parititon 条数的中位数）且大于 spark.sql.adaptive.skewedPartitionRowCountThreshold，才会被当做倾斜的 partition 进行相应的处理。默认值为 10

spark.sql.adaptive.skewJoin.skewedPartitionThresholdInBytes
判断是否倾斜分区的最低阈值。

强制使用spark engine
set tqs.query.engine.type = sparkCli;
set spark.yarn.priority = 4;

双写HDFS开启：
set spark.shuffle.hdfs.enable=true;
set spark.shuffle.io.maxRetries=1;
set spark.shuffle.io.retryWait=0s;
set spark.network.timeout=120s;

## 双写HDFS开启避免fetch failed，且基本上只有20min以上大任务再开启
调整全局任务并行度
set spark.sql.shuffle.partitions=400;
set spark.default.paralleism=400;
set spark.executore.cores=4;
动态资源申请
set spark.dynamicAllocation = True;
set spark.dynamicAllocation.minExecutors = 30;
set spark.dynamicAllocation.maxExecutors = 200;
set spark.dynamicAllocation.initExectors = 30;

## 动态资源申请，保证尽快起任务，不适用时归还资源
memory
set spark.exector.memory=10g;
set spark.executor.memoryOverhead=10g;
set spark.driver.memory=3g;

## memory:executor memory = memory + memoryoverhead
join
set spark.shuffle.statistic.verbose=true; -- 收集join数据
set spark.sql.join.perferSortMergejoin=false; -- disable sort to enable hash
set spark.sql.autoBroadcastJoinThreshold=134217728; -- 如果不设置跟autoBroadcastJoinThreshold一致，则被覆盖
AE：skewed
set spark.sql.adaptive.skewedJoin.enable=true;
set spark.sql.adaptive.skewedpartitionMaxSplits=3;
set spark.sql.adaptive.skewedPartitionFactor=3;
set spark.sql.adaptive.skewedPartitionSizeThreshold=52428800;
set spark.sql.adaptive.skewedPartitionRowCountThreshold=5000000;
AE：partition
set spark.sql.adaptive.maxNumPostShufflePartitions=1000;
set spark.sql.adaptive.minNumPostShufflePartitions=10;
set spark.sql.adaptive.shuffle.targetPostShuffleInputSize=60;

## 解决partition太多，reducer生成太多文件的问题，自动进行文件合并；
input
set spark.sql.hive.convertMetastoreParquet=true;
set spark.sql.parquet.adaptiveFileSplit=true;
set spark.sql.files.maxPartitionBytes=314572800;
set spark.sql.files.openCostinBytes=16777216;
Output
set spark.merge.files.enabled=true;
set spark.merge.files.number=512;

## 大文件优化读取优化
spark.files.maxPartitionBytes=   默认128m
parquet.block.size= 默认128m
## 小文件读取优化
spark.sql.hive.convertMetastoreParquet=true （默认已经为true）
spark.sql.hive.convertMetastoreOrc=true
## 小文件合并
spark.files.maxPartitionBytes=   默认128m
spark.files.openCostInBytes=     默认4m

6：impala参数优化

memory.soft_limit_in_bytes    cgroup内存软限制
memory.limit_in_bytes      cgroup内存硬限制
mem_limit      内存限制
Maximum Query Memory Limit  某个队列资源池，一个查询在一个Impala节点上下执行需要的最大内存
Minimum Query Memory Limit 某个队列资源池，一个查询在一个Impala节点上下执行需要的最小内存
Impala Daemon JVM Heap 来控制Java进程的内存使用量的
state_store_num_server_worker_threads StateStore Thrift 服务器的基础线程管理器的工作线程数量
stacks_collection_frequency	 statestore尝试向每个订户发送主题更新的频率（以毫秒为单位）
```

```
yarn性能优化核心参数：
https://www.cnblogs.com/jmx-bigdata/p/13708876.html
```
![](https://files.mdnice.com/user/37771/daebe92c-c3e1-4ec7-a572-54b001424be4.png)

# starrocks导入性能对比分析
StarRocks 提供 Stream Load、Broker Load、 Routine Load、Spark Load 和 INSERT 多种导入方式，满足您在不同业务场景下的数据导入需求。
![](https://files.mdnice.com/user/37771/982f1ed9-3942-4d1b-858c-61fda3a3b974.png)
![](https://files.mdnice.com/user/37771/e803f93a-cc61-49f5-9eec-c63dabdf5198.png)
您可以根据业务场景、数据量、数据源、数据格式和导入频次等来选择合适的导入方式。另外，在选择导入方式时，可以注意以下几点：

```
从 Kafka 导入数据时，推荐使用 Routine Load 实现导入。如果导入过程中有复杂的多表关联和 ETL 预处理，建议先使用 Apache Flink® 从 Kafka 读取数据并对数据进行处理，然后再通过 StarRocks 提供的标准插件 flink-connector-starrocks 把处理后的数据导入到 StarRocks 中。

从 Hive 导入数据时，推荐创建 Hive catalog、然后使用 INSERT 实现导入，或者通过 Broker Load 实现导入。

从 MySQL 导入数据时，推荐创建 MySQL 外部表、然后使用 INSERT 实现导入，或者通过 DataX 实现导入。如果要导入实时数据，建议您参考 从 MySQL 实时同步 实现导入。

从 Oracle、PostgreSQL 等数据源导入数据时，推荐创建 JDBC 外部表、然后使用 INSERT 实现导入，或者通过 DataX 实现导入。
```
![](https://files.mdnice.com/user/37771/cba956bf-a45f-47fa-90f7-e1ecb000d468.png)
## 导入效率：

```
spark load>broker load>Routine load>stream load
```

## broker load与stream load的比较

```
【结论】：

broker load 导入速度比stream load 快
BE的节点数越多，broker load的速度越快
文件格式对broker load的速度影响不大(此处对比csv与parquet)
broker 服务推荐部署在BE节点，可以减少网络传输
如果导入的文件比较少，broker 服务的数量对broker load的速度影响不大，但推荐所有BE均部署broker
FE:load_parallel_instance_num 等参数会影响broker load的速度，但不宜调整过大，会导致冲突严重
BE:flush_thread_num_per_store 与 olap_table_sink_send_interval_ms 参数会影响broker load的速度，但影响有限

BE服务器CPU核数对导入速度有影响，核数越多越快
BE服务器CPU主频对导入速度有影响，主频数越多越快

【参数】:

FE参数：
load_parallel_instance_num = 32
max_broker_cOncurrency= 400
be参数：
flush_thread_num_per_store=8
olap_table_sink_send_interval_ms=0
BE数量：3个
导入任务参数：
"load_mem_limit" = "12884901888"  （12G）

【总结】:
为提高broker load的速度：HDFS的文件格式均可，设置FE、BE相关参数，增加BE节点数，broker 与 BE混合部署，且BE均部署broker 服务，提高单个BE的配置：cpu核数、主频
```

## spark load与broker load的比较
![](https://files.mdnice.com/user/37771/8425e0ae-9aff-42e9-8c1a-84a8cffe4d68.png)
spark load 把导入拆成计算和存储两部分，把分、排序、聚合、压缩等计算逻辑放到 spark 集群，产出结果写到 HDFS，doris 再直接从 HDFS 中拉取结果文件写到本地盘

```
broker load :BE 节点负责计算,算力取决 BE 节点个数及配置,
spark load : spark 集群负责计算，算力取决于集群配置，且弹性强
```
概括：把导入涉及的聚合排序等计算卸载到 spark 集群，充分利用 spark 强大的计算能力

```
starrocks导入是一个 cpu 密集性操作
broker load 在大量数据聚合导入场景下存在性能问题，瓶颈在计算
spark load 可以很好的把导入涉及的计算任务卸载到 spark 中，充分发挥各自优势
spark load 配置较为复杂，门槛高，spark/hadoop 环境不同公司差异大容易踩坑，非大规模数据导入下谨慎使用
spark load 支持 hive 和hdfs 两种数据源，hdfs 作为数据源直接导入问题较多，不建议使用，hive 作为数据源测试比较充分，可以使用
```
desired_concurrent_number 用于指定一个例行作业期望的并发度。即一个作业，最多有多少 task 同时在执行。
对于 Kafka 导入而言，当前的实际并发度计算如下：
```
min(alive_be_number, partition_number, desired_concurrent_number, max_routine_load_task_concurrent_num)
```


## BE参数

```
max_download_speed_kbps：单个 HTTP 请求的最大下载速率。这个值会影响 BE 之间同步数据副本的速度。
scanner_thread_pool_thread_num：存储引擎并发扫描磁盘的线程数，统一管理在线程池中。提高高并发查询性能。
pending_data_expire_time_sec：存储引擎保留的未生效数据的最大时长。
inc_rowset_expired_sec：导入生效的数据，存储引擎保留的时间，用于增量克隆。
scanner_thread_pool_queue_size：存储引擎支持的扫描任务数。
scanner_row_num：每个扫描线程单次执行最多返回的数据行数。
exchg_node_buffer_size_bytes：Exchange 算子中，单个查询在接收端的 buffer 容量。这是一个软限制，如果数据的发送速度过快，接收端会触发反压来限制发送速度。
memory_limitation_per_thread_for_schema_change：单个 schema change 任务允许占用的最大内存。


streaming_load_max_mb：流式导入单个文件大小的上限。
streaming_load_max_batch_size_mb：流式导入单个 JSON 文件大小的上限。
memory_maintenance_sleep_time_s：触发 Tcmalloc GC 任务的时间间隔。StarRocks 会周期运行 GC 任务，尝试将 Tcmalloc 的空闲内存返还给操作系统。
write_buffer_size：MemTable 在内存中的 buffer 大小，超过这个限制会触发 flush。
txn_commit_rpc_timeout_ms：Txn 超时的时长，单位为 ms。


max_consumer_num_per_group：Routine load 中，每个consumer group 内最大的 consumer 数量。
max_memory_sink_batch_count：Scan cache 的最大缓存批次数量。
scan_context_gc_interval_min：Scan context 的清理间隔。
path_gc_check_step：单次连续 scan 最大的文件数量。
path_gc_check_step_interval_ms：多次连续 scan 文件间隔时间。
max_runnings_transactions_per_txn_map：每个分区内部同时运行的最大事务数量。
tablet_max_pending_versions：PrimaryKey 表允许 committed 未 apply 的最大版本数。
max_hdfs_file_handle：最多可以打开的 HDFS 文件句柄数量。
max_compaction_concurrency：Compaction 线程数上限（即 BaseCompaction + CumulativeCompaction 的最大并发）。该参数防止 Compaction 占用过多内存。 -1 代表没有限制。0 表示不允许 compaction。
internal_service_async_thread_num：单个 BE 上与 Kafka 交互的线程池大小。当前 Routine Load FE 与 Kafka 的交互需经由 BE 完成，而每个 BE 上实际执行操作的是一个单独的线程池。当 Routine Load 任务较多时，可能会出现线程池线程繁忙的情况，可以调整该配置。

brpc_num_threads：BRPC 的 bthreads 线程数量，-1 表示和 CPU 核数一样。
push_worker_count_normal_priority：导入线程数，处理 NORMAL 优先级任务。
push_worker_count_high_priority：导入线程数，处理 HIGH 优先级任务。
transaction_publish_version_worker_count：生效版本的最大线程数。当该参数被设置为小于或等于 0 时，系统默认使用 CPU 核数的一半，以避免因使用固定值而导致在导入并行较高时线程资源不足。自 2.5 版本起，默认值由 8 变更为 0。
num_threads_per_core	：每个 CPU core 启动的线程数。
alter_tablet_worker_count：进行 schema change 的线程数。
compress_rowbatches：BE 之间 RPC 通信是否压缩 RowBatch，用于查询层之间的数据传输。
serialize_batch：BE 之间 RPC 通信是否序列化 RowBatch，用于查询层之间的数据传输。
max_tablet_num_per_shard：每个 shard 的 tablet 数目，用于划分 tablet，防止单个目录下 tablet 子目录过多。
file_descriptor_cache_capacity：文件句柄缓存的容量。
min_file_descriptor_number：BE 进程的文件句柄 limit 要求的下线。
webserver_num_workers：HTTP Server 线程数。
load_data_reserve_hours：小批量导入生成的文件保留的时长。
number_tablet_writer_threads：流式导入的线程数。
streaming_load_rpc_max_alive_time_sec：流式导入 RPC 的超时时间。
fragment_pool_thread_num_min：最小查询线程数，默认启动 64 个线程。
fragment_pool_thread_num_max：最大查询线程数。
fragment_pool_queue_size：单节点上能够处理的查询请求上限。
load_process_max_memory_limit_bytes：单节点上所有的导入线程占据的内存上限，100GB。
load_process_max_memory_limit_percent：单节点上所有的导入线程占据的内存上限比例。
routine_load_thread_pool_size：Routine Load 的线程池数目。
brpc_max_body_size：BRPC 最大的包容量，单位为 Byte。	
tablet_map_shard_size：Tablet 分组数。
mem_limit	：BE 进程内存上限。可设为比例上限（如 "80%"）或物理上限（如 "100GB"）。
flush_thread_num_per_store：每个 Store 用以 Flush MemTable 的线程数。
jdbc_connection_pool_size：JDBC 连接池大小。每个 BE 节点上访问 jdbc_url 相同的外表时会共用同一个连接池。
jdbc_minimum_idle_connections：JDBC 连接池中最少的空闲连接数量。
jdbc_connection_idle_timeout_ms：DBC 空闲连接超时时间。如果 JDBC 连接池内的连接空闲时间超过此值，连接池会关闭超过 jdbc_minimum_idle_connections 配置项中指定数量的空闲连接。
```

## FE参数

```
load_straggler_wait_second：	控制 BE 副本最大容忍的导入落后时长，超过这个时长就进行克隆，单位为秒。
desired_max_waiting_jobs：最多等待的任务数，适用于所有的任务，建表、导入、schema change。如果 FE 中处于 PENDING 状态的作业数目达到该值，FE 会拒绝新的导入请求。该参数配置仅对异步执行的导入有效。从 2.5 版本开始，该参数默认值从 100 变为 1024。
max_running_txn_num_per_db：StarRocks 集群每个数据库中正在运行的导入作业的最大个数，默认值为 100。当数据库中正在运行的导入作业超过最大个数限制时，后续的导入不会执行。如果是同步的导入作业，作业会被拒绝；如果是异步的导入作业，作业会在队列中等待。不建议调大该值，会增加系统负载。
load_parallel_instance_num：单个 BE 上每个作业允许的最大并发实例数。
max_routine_load_job_num：最大的 Routine Load 作业数。
max_routine_load_task_concurrent_num：每个 Routine Load 作业最大并发执行的 task 数。
max_routine_load_task_num_per_be	：每个 BE 最大并发执行的 Routine Load task 数，需要小于等于 BE 的配置项 routine_load_thread_pool_size。
max_routine_load_batch_size：每个 Routine Load task 导入的最大数据量，单位为 Byte。
routine_load_task_consume_second：每个 Routine Load task 消费数据的最大时间，单位为秒。
routine_load_task_timeout_second：每个 Routine Load task 超时时间，单位为秒。
max_tolerable_backend_down_num：允许的最大故障 BE 数。如果故障的 BE 节点数超过该阈值，则不能自动恢复 Routine Load 作业。
min_bytes_per_broker_scanner：单个 Broker Load 任务最大并发实例数，单位为 Byte。
max_broker_concurrency：单个 Broker Load 任务最大并发实例数。
export_max_bytes_per_be_per_task：单个导出任务在单个 BE 上导出的最大数据量，单位为 Byte。
export_running_job_num_limit：导出作业最大的运行数目。


async_load_task_pool_size：导入任务线程池的大小。本参数仅适用于 Broker Load。取值必须小于 max_running_txn_num_per_db。从 2.5 版本开始，该参数默认值从 10 变为 2。	
export_checker_interval_second：导出作业调度器的调度间隔。
export_task_pool_size：导出任务线程池的大小。
broker_client_timeout_ms：Broker RPC 的默认超时时间，单位：毫秒，默认值 10s。
max_agent_task_threads_num	：代理任务线程池中用于处理代理任务的最大线程数。
hive_meta_load_concurrency：Hive 元数据支持的最大并发线程数。
hive_meta_cache_refresh_interval_s：刷新 Hive 外表元数据缓存的时间间隔。单位：秒。
es_state_sync_interval_second：FE 获取 Elasticsearch Index 和同步 StarRocks 外部表元数据的时间间隔。单位：秒。
```

## Stream Load参数优化

```
# BE
streaming_load_max_mb：单个源数据文件的大小上限。默认文件大小上限为 10 GB。
stream_load_default_timeout_second：导入作业的超时时间。默认超时时间为 600 秒。
write_buffer_size：MemTable 在内存中的 buffer 大小，超过这个限制会触发 flush。
flush_thread_num_per_store：每个 Store 用以 Flush MemTable 的线程数。
num_threads_per_core：每个 CPU core 启动的线程数。
number_tablet_writer_threads：流式导入的线程数。
streaming_load_rpc_max_alive_time_sec：流式导入 RPC 的超时时间。
push_worker_count_high_priority：导入线程数，处理 HIGH 优先级任务。
push_worker_count_normal_priority：导入线程数，处理 NORMAL 优先级任务。
load_process_max_memory_limit_percent：表示对BE总内存限制的百分比。默认为30。（总内存限制 mem_limit 默认为 80%，表示对物理内存的百分比）。即假设物理内存为 M，则默认导入内存限制为 M * 80% * 30%。
load_process_max_memory_limit_bytes：默认为100GB。
push_write_mbytes_per_sec:BE 上单个 Tablet 的最大写入速度，默认值为 10 MB/s。根据表结构 (Schema) 的不同，通常 BE 对单个 Tablet 的最大写入速度大约在 10 MB/s 到 30 MB/s 之间。可以适当调整该参数的取值来控制导入速度。
tablet_writer_rpc_timeout_sec:导入过程中，Coordinator BE 发送一批次数据的 RPC 超时时间，默认为 600 秒。每批次数据包含 1024 行。RPC 可能涉及多个分片内存块的写盘操作，所以可能会因为写盘导致 RPC 超时，这时候可以适当地调整这个超时时间来减少超时错误（如 "send batch fail" 错误）。同时，如果调大 write_buffer_size 参数的取值，也需要适当地调大 tablet_writer_rpc_timeout_sec 参数的取值。
streaming_load_rpc_max_alive_time_sec:指定了 Writer 进程的等待超时时间，默认为 600 秒。在导入过程中，StarRocks 会为每个 Tablet 开启一个 Writer 进程，用于接收和写入数据。如果在参数指定时间内 Writer 进程没有收到任何数据，StarRocks 系统会自动销毁这个 Writer 进程。当系统处理速度较慢时，Writer 进程可能长时间接收不到下一批次数据，导致上报 "TabletWriter add batch with unknown id" 错误。这时候可适当调大这个参数的取值。
max_download_speed_kbps:单个 HTTP 请求的最大下载速率。这个值会影响 BE 之间同步数据副本的速度。
download_low_speed_limit_kbps:单个 HTTP 请求的下载速率下限。如果在 download_low_speed_time 秒内下载速度一直低于download_low_speed_limit_kbps，那么请求会被终止。
webserver_num_workers:HTTP Server 线程数。

# FE
stream_load_default_timeout_second：Stream Load 的默认超时时间，单位为秒。
desired_max_waiting_jobs：最多等待的任务数，适用于所有的任务，建表、导入、schema change。如果 FE 中处于 PENDING 状态的作业数目达到该值，FE 会拒绝新的导入请求。该参数配置仅对异步执行的导入有效。从 2.5 版本开始，该参数默认值从 100 变为 1024。
max_running_txn_num_per_db：StarRocks 集群每个数据库中正在运行的导入作业的最大个数，默认值为 100。当数据库中正在运行的导入作业超过最大个数限制时，后续的导入不会执行。如果是同步的导入作业，作业会被拒绝；如果是异步的导入作业，作业会在队列中等待。不建议调大该值，会增加系统负载。
load_parallel_instance_num：单个 BE 上每个作业允许的最大并发实例数。
max_load_timeout_second 和 min_load_timeout_second:设置导入超时时间的最大、最小值，单位均为秒。默认的最大超时时间为 3 天，默认的最小超时时间为 1 秒。自定义的导入超时时间不能超过这个最大、最小值范围。该参数配置适用于所有模式的导入作业。
label_keep_max_second:已经完成、且处于 FINISHED 或 CANCELLED 状态的导入作业记录在 StarRocks 系统的保留时长，默认值为 3 天。该参数配置适用于所有模式的导入作业。
```
## Broker Load参数优化

```
# BE参数
write_buffer_size：MemTable 在内存中的 buffer 大小，超过这个限制会触发 flush。
flush_thread_num_per_store：每个 Store 用以 Flush MemTable 的线程数。
async_load_task_pool_size:导入任务线程池的大小。本参数仅适用于 Broker Load。取值必须小于 max_running_txn_num_per_db。从 2.5 版本开始，该参数默认值从 10 变为 2。
export_task_pool_size:导出任务线程池的大小。
broker_client_timeout_ms:roker RPC 的默认超时时间，单位：毫秒，默认值 10s。
num_threads_per_core：每个 CPU core 启动的线程数。
number_tablet_writer_threads:流式导入的线程数。
streaming_load_rpc_max_alive_time_sec:流式导入 RPC 的超时时间。
fragment_pool_thread_num_min:最小查询线程数，默认启动 64 个线程。	
fragment_pool_thread_num_max:最大查询线程数。
fragment_pool_queue_size:单节点上能够处理的查询请求上限。
push_worker_count_high_priority：导入线程数，处理 HIGH 优先级任务。
push_worker_count_normal_priority：导入线程数，处理 NORMAL 优先级任务。
load_process_max_memory_limit_percent：表示对BE总内存限制的百分比。默认为30。（总内存限制 mem_limit 默认为 80%，表示对物理内存的百分比）。即假设物理内存为 M，则默认导入内存限制为 M * 80% * 30%。
load_process_max_memory_limit_bytes：默认为100GB。
push_write_mbytes_per_sec:BE 上单个 Tablet 的最大写入速度，默认值为 10 MB/s。根据表结构 (Schema) 的不同，通常 BE 对单个 Tablet 的最大写入速度大约在 10 MB/s 到 30 MB/s 之间。可以适当调整该参数的取值来控制导入速度。
tablet_writer_rpc_timeout_sec:导入过程中，Coordinator BE 发送一批次数据的 RPC 超时时间，默认为 600 秒。每批次数据包含 1024 行。RPC 可能涉及多个分片内存块的写盘操作，所以可能会因为写盘导致 RPC 超时，这时候可以适当地调整这个超时时间来减少超时错误（如 "send batch fail" 错误）。同时，如果调大 write_buffer_size 参数的取值，也需要适当地调大 tablet_writer_rpc_timeout_sec 参数的取值。

# FE
desired_max_waiting_jobs：最多等待的任务数，适用于所有的任务，建表、导入、schema change。如果 FE 中处于 PENDING 状态的作业数目达到该值，FE 会拒绝新的导入请求。该参数配置仅对异步执行的导入有效。从 2.5 版本开始，该参数默认值从 100 变为 1024。
max_running_txn_num_per_db：StarRocks 集群每个数据库中正在运行的导入作业的最大个数，默认值为 100。当数据库中正在运行的导入作业超过最大个数限制时，后续的导入不会执行。如果是同步的导入作业，作业会被拒绝；如果是异步的导入作业，作业会在队列中等待。不建议调大该值，会增加系统负载。
load_parallel_instance_num：单个 BE 上每个作业允许的最大并发实例数。
broker_load_default_timeout_second:Broker Load 的超时时间，单位为秒。	
min_bytes_per_broker_scanner：单个 Broker Load 任务最大并发实例数，单位为 Byte。
max_broker_concurrency：	单个 Broker Load 任务最大并发实例数。
export_max_bytes_per_be_per_task：单个导出任务在单个 BE 上导出的最大数据量，单位为 Byte。
export_running_job_num_limit：导出作业最大的运行数目。
async_load_task_pool_size:导入任务线程池的大小。本参数仅适用于 Broker Load。取值必须小于 max_running_txn_num_per_db。从 2.5 版本开始，该参数默认值从 10 变为 2。
export_task_pool_size:导出任务线程池的大小。
broker_client_timeout_ms:roker RPC 的默认超时时间，单位：毫秒，默认值 10s。
```


## Routine Load参数优化

```
# BE
max_consumer_num_per_group:Routine load 中，每个consumer group 内最大的 consumer 数量。
routine_load_thread_pool_size:Routine Load 的线程池数目。
max_memory_sink_batch_count:Scan cache 的最大缓存批次数量。
max_compaction_concurrency:Compaction 线程数上限（即 BaseCompaction + CumulativeCompaction 的最大并发）。该参数防止 Compaction 占用过多内存。 -1 代表没有限制。0 表示不允许 compaction。
internal_service_async_thread_num:单个 BE 上与 Kafka 交互的线程池大小。当前 Routine Load FE 与 Kafka 的交互需经由 BE 完成，而每个 BE 上实际执行操作的是一个单独的线程池。当 Routine Load 任务较多时，可能会出现线程池线程繁忙的情况，可以调整该配置。
write_buffer_size：MemTable 在内存中的 buffer 大小，超过这个限制会触发 flush。
flush_thread_num_per_store：每个 Store 用以 Flush MemTable 的线程数。
num_threads_per_core：每个 CPU core 启动的线程数。
number_tablet_writer_threads:流式导入的线程数。
streaming_load_rpc_max_alive_time_sec:流式导入 RPC 的超时时间。
fragment_pool_thread_num_min:最小查询线程数，默认启动 64 个线程。	
fragment_pool_thread_num_max:最大查询线程数。
fragment_pool_queue_size:单节点上能够处理的查询请求上限。
push_worker_count_high_priority：导入线程数，处理 HIGH 优先级任务。
push_worker_count_normal_priority：导入线程数，处理 NORMAL 优先级任务。
load_process_max_memory_limit_percent：表示对BE总内存限制的百分比。默认为30。（总内存限制 mem_limit 默认为 80%，表示对物理内存的百分比）。即假设物理内存为 M，则默认导入内存限制为 M * 80% * 30%。
load_process_max_memory_limit_bytes：默认为100GB。
push_write_mbytes_per_sec:BE 上单个 Tablet 的最大写入速度，默认值为 10 MB/s。根据表结构 (Schema) 的不同，通常 BE 对单个 Tablet 的最大写入速度大约在 10 MB/s 到 30 MB/s 之间。可以适当调整该参数的取值来控制导入速度。
tablet_writer_rpc_timeout_sec:导入过程中，Coordinator BE 发送一批次数据的 RPC 超时时间，默认为 600 秒。每批次数据包含 1024 行。RPC 可能涉及多个分片内存块的写盘操作，所以可能会因为写盘导致 RPC 超时，这时候可以适当地调整这个超时时间来减少超时错误（如 "send batch fail" 错误）。同时，如果调大 write_buffer_size 参数的取值，也需要适当地调大 tablet_writer_rpc_timeout_sec 参数的取值。	

# FE
max_routine_load_job_num：最大的 Routine Load 作业数。
max_routine_load_task_concurrent_num：每个 Routine Load 作业最大并发执行的 task 数。
max_routine_load_task_num_per_be：每个 BE 最大并发执行的 Routine Load task 数，需要小于等于 BE 的配置项 routine_load_thread_pool_size。	
max_routine_load_batch_size：每个 Routine Load task 导入的最大数据量，单位为 Byte。
routine_load_task_consume_second：	每个 Routine Load task 消费数据的最大时间，单位为秒。
routine_load_task_timeout_second：每个 Routine Load task 超时时间，单位为秒。	
max_tolerable_backend_down_num：允许的最大故障 BE 数。如果故障的 BE 节点数超过该阈值，则不能自动恢复 Routine Load 作业。
period_of_auto_resume_min：自动恢复 Routine Load 的时间间隔，单位为分钟。
desired_max_waiting_jobs：最多等待的任务数，适用于所有的任务，建表、导入、schema change。如果 FE 中处于 PENDING 状态的作业数目达到该值，FE 会拒绝新的导入请求。该参数配置仅对异步执行的导入有效。从 2.5 版本开始，该参数默认值从 100 变为 1024。
max_running_txn_num_per_db：StarRocks 集群每个数据库中正在运行的导入作业的最大个数，默认值为 100。当数据库中正在运行的导入作业超过最大个数限制时，后续的导入不会执行。如果是同步的导入作业，作业会被拒绝；如果是异步的导入作业，作业会在队列中等待。不建议调大该值，会增加系统负载。
load_parallel_instance_num：单个 BE 上每个作业允许的最大并发实例数。
max_load_timeout_second 和 min_load_timeout_second:设置导入超时时间的最大、最小值，单位均为秒。默认的最大超时时间为 3 天，默认的最小超时时间为 1 秒。自定义的导入超时时间不能超过这个最大、最小值范围。该参数配置适用于所有模式的导入作业。
label_keep_max_second:已经完成、且处于 FINISHED 或 CANCELLED 状态的导入作业记录在 StarRocks 系统的保留时长，默认值为 3 天。该参数配置适用于所有模式的导入作业。
```
# 分区分桶数量的设计
![](https://files.mdnice.com/user/37771/2d90e737-4e19-4838-995b-9da461e92f01.png)
总结一下：分区是针对表的，是对表的数据取段。分桶是针对每个分区的，会将分区后的每段数据打散为逻辑分片Tablet。副本数是针对Tablet的，是指Tablet保存的份数。那么我们不难发现，对某一个数据表，若每个分区的分桶数一致，其总Tablet数：


```
总Tablet数=分区数*分桶数*副本数
分桶数=BE数量*BE节点CPU核数 或者BE数量*BE节点CPU核数/2
```


以table01为例，我们为其设置了3个分区，为每个分区设置了20个分桶，又对分桶后的tablet设置了1副本，则table01的总tablet数=3*20*1=60个。查看table01的tablet信息，发现确实共有60个tablet：

```
mysql> show tablet from table01;
…………

60 rows in set (0.01 sec)
```
## 如何分区？
分区的主要作用是允许用户将整个分区作为管理单位，从而选择存储策略，比如副本数，冷热策略和存储介质等等。大多数情况下，新数据被查询的可能性更大，因此将新数据存储在同一个分区后，用户可以通过StarRocks的分区裁剪功能，最大限度地减少扫描数据量，从而提高查询性能。同时，StarRocks支持在一个集群内使用多种存储介质(SATA/SSD)。用户可以将新数据所在的分区放在SSD上，利用SSD的优秀的随机读写性能来提高查询性能。而通过将旧数据存放在SATA盘上，用户可以节省数据存储的成本。

所以，在实际应用中，用户一般选取时间列作为分区键，具体划分的粒度视数据量而定，单个分区原始数据量建议维持在100GB以内。

```
分区键选择：当前分区键仅支持日期类型和整数类型，为了让分区能够更有效的裁剪数据，我们一般也是选取时间或者区域作为分区键。使用动态分区可以定期自动创建分区，比如每天创建出新的分区等。

分区粒度选择：StarRocks的分区粒度视数据量而定，单个分区原始数据量建议维持在100G以内。
```
## 如何分桶
StarRocks采用Hash算法作为分桶算法，同一分区内, 分桶键的哈希值相同的数据形成(Tablet)子表, 子表多副本冗余存储, 子表副本在物理上由一个单独的本地存储引擎管理, 数据导入和查询最终都下沉到所涉及的子表副本上, 同时子表也是数据均衡和恢复的基本单位。在聚合模型、更新模型、主键模型下，分桶键必需是排序键中的列。

```
# 分桶列选择

选择高基数的列（例如唯一ID）来作为分桶键，可以保证数据在各个bucket中尽可能均衡。如果数据倾斜情况严重，用户可以使用多个列作为数据的分桶键，但是不建议使用过多列。

分桶的数量影响查询的并行度，最佳实践是计算一下数据存储量，将每个tablet设置成 1GB 左右。

在机器资源不足的情况下，用户如果想充分利用机器资源，可以通过 BE数量 * cpu core / 2的计算方式来设置bucket数量。例如，在将 100GB 未压缩的 CSV 文件导入 StarRocks 时，用户使用 4 台 BE，每台包含 64 核心，仅建立一个分区，通过以上计算方式可得出 bucket 数量为 4 * 64 /2 = 128。此时每个 tablet 的数据为 781MB，能够充分利用CPU资源，保证数据在各个bucket中尽可能均衡。

# 分桶数量的选择
分桶数的设置需要适中，如果分桶过少，查询时查询并行度上不来（CPU多核优势体现不出来）。而如果分桶过多，会导致元数据压力比较大，数据导入导出时也会受到一些影响。

分桶数的设置通常也建议以数据量为参考，从经验来看，每个分桶的原始数据建议不要超过5个G，考虑到压缩比，也即每个分桶的大小建议在100M-1G之间。建议用户根据集群规模的变化，建表时调整分桶的数量。集群规模变化，主要指节点数目的变化。

对照CSV文件，StarRocks的压缩比在 0.3 ~ 0.5 左右（以下计算取0.5,按照千进制计算）。假设10GB的CSV文件导入StarRocks,我们分为10个均匀的分区。一个分区承担的CSV文本数据量：10GB/10 = 1GB。单一副本按照0.5压缩比存入StarRocks文件大小：1GB * 0.5 = 500MB，通常存储三副本，一个分区的文件总大小为500MB*3 = 1500MB，按照建议，一个tablet规划300MB,则需设置5个分桶：1500MB/300MB = 5，如果是MySQL中的文件，一主两从的模式，我们只需要计算单副本的MySQL集群大小，按 照0.7的压缩比（经验值）换算成CSV文件大小，再按照上面的步骤计算出StarRcoks的分桶数量。
```
分桶的数据的压缩方式使用的是Lz4。建议压缩后磁盘上每个分桶数据文件大小在 1GB 左右。这种模式在多数情况下足以满足业务需求。

## 动态分区
在StarRocks中，必须先有分区，才能将对应的数据导入进来，不然导入就会报错（提示there is a row couldn’t find a partition）。比如使用日期作为分区，那就需要先创建每天的分区，再进行数据导入。在日常业务中，除了每日会新增数据，我们还会对旧的历史数据进行清理。动态分区就是StarRocks用来实现新分区自动创建以及过期分区自动删除的方式。


动态分区由后台常驻进程调度，默认调度周期为10分钟一次，由FE配置文件中的dynamic_partition_check_interval_seconds参数控制（单位是秒，配置文件中默认没有该配置），所以并不是创建动态分区表后所有分区就立刻被创建，我们还需要等待不超过10分钟让后台调度生效。

```
CREATE TABLE site_access(
event_day DATE,
site_id INT DEFAULT '10',
city_code VARCHAR(100),
user_name VARCHAR(32) DEFAULT '',
pv BIGINT DEFAULT '0'
)
DUPLICATE KEY(event_day, site_id, city_code, user_name)
PARTITION BY RANGE(event_day)(
PARTITION p20200321 VALUES LESS THAN ("2020-03-22"),
PARTITION p20200322 VALUES LESS THAN ("2020-03-23"),
PARTITION p20200323 VALUES LESS THAN ("2020-03-24"),
PARTITION p20200324 VALUES LESS THAN ("2020-03-25")
)
DISTRIBUTED BY HASH(event_day, site_id)
PROPERTIES(
    "dynamic_partition.enable" = "true",
    "dynamic_partition.time_unit" = "DAY",
    "dynamic_partition.start" = "-3",
    "dynamic_partition.end" = "3",
    "dynamic_partition.prefix" = "p",
    "dynamic_partition.history_partition_num" = "0"
);
```
dynamic_partition.enable：是否开启动态分区特性，可指定为TRUE或FALSE。如果该参数等号后不填写值，则默认代表TRUE。

dynamic_partition.time_unit：动态分区调度的粒度，可指定为DAY/WEEK/MONTH。
不同分区粒度下动态分区自动创建分区的名称后缀不同，指定为DAY时，分区名后缀为yyyyMMdd，例如table07中的20211009。指定为WEEK时，分区名后缀为yyyy_ww，例如2021_40代表2021年第40周。指定为MONTH时，动态创建的分区名后缀格式为yyyyMM，例如202110。这里注意，我们前面创建静态分区时，分区命名也建议和动态分区自动创建的规则保持一致。

dynamic_partition.start：动态分区的开始时间。以当天为基准，根据该参数向过去推算数个粒度的时间，超过该时间范围的分区将会被删除。如果不填写，则默认为Integer.MIN_VALUE即-2147483648。

dynamic_partition.end：动态分区的结束时间。以当天为基准，会根据该参数提前创建数个粒度的分区范围。

dynamic_partition.prefix：动态分区自动创建的分区名前缀。

dynamic_partition.buckets：动态创建的分区所对应的分桶数量。

**修改表的动态分区属性**

```
ALTER TABLE site_access SET("dynamic_partition.enable"="false");
```
分区默认是左闭右开区间当分区键为日期类型的时候，需要指定INTERVAL关键字来表示日期间隔，目前日期仅支持day、week、month、year，分区的命名规则同动态分区一样。例如：

```

PARTITION BY RANGE (event_day) (
    START ("2021-10-01") END ("2021-10-03") EVERY (INTERVAL 1 day)
)

```
**分区操作**

```
# 使用新的分桶数新增分区p20210311：
ALTER TABLE starrocks.table07
ADD PARTITION p20210311 VALUES LESS THAN ("2021-03-12")
DISTRIBUTED BY HASH(event_day, site_id) BUCKETS 20;

# 增加一个指定上下界的分区p20210312：
ALTER TABLE starrocks.table07
ADD PARTITION p20210312 VALUES [("2021-03-12"), ("2021-03-13"));

# 删除分区p20210311：
ALTER TABLE starrocks.table07 DROP PARTITION p20210311;

# 将表table07中名为p20210312的partition修改为p20210312_1
ALTER TABLE table07 RENAME PARTITION p20210312 p20210312_1;

# 建表后批量创建分区（与建表时批量创建分区类似）：
ALTER TABLE table07 ADD
PARTITIONS START ("2021-01-01") END ("2021-01-06") EVERY (interval 1 day);

# 清空分区数据：
TRUNCATE TABLE table07 PARTITION(p20211009,p20211010);
```
**副本数修改**

```
# 增加分区时使用新的副本数：
ALTER TABLE starrocks.table07
ADD PARTITION p20210313 VALUES LESS THAN ("2021-03-14")
("replication_num"="1");

# 修改分区副本数：
ALTER TABLE starrocks.table07
MODIFY PARTITION p20210313 SET("replication_num"="2");

# 修改表的默认副本数量，新建分区副本数量默认使用此值：
ALTER TABLE starrocks.table07
SET ("default.replication_num" = "2");

# 修改单分区表的实际副本数量（只限单分区表）：
ALTER TABLE starrocks.table06
SET ("replication_num" = "1");

# 修改表所有分区的副本数：
ALTER TABLE starrocks.table01
MODIFY PARTITION(*)
SET ("replication_num" = "3");
```
**临时分区**

StarRocks中也有临时分区的概念，通过临时分区，我们可以较为方便的调整原数据表中不合理分区的分桶数或者分区范围，也可以在StarRocks中基于lambda模式方便的进行数据导入（用T+1的数据修正实时数据）。
在以下应用场景中，您可以使用临时分区功能：

```
# 原子覆盖写操作
如果您需要重写某一正式分区的数据，同时保证重写过程中可以查看数据，您可以先创建一个对应的临时分区，将新的数据导入到临时分区后，通过替换操作，原子地替换原有正式分区，生成新正式分区。对于非分区表的原子覆盖写操作，请参考 ALTER TABLE - SWAP。

# 调整分区数据的查询并发
如果您需要修改某一正式分区的分桶数，您可以先创建一个对应分区范围的临时分区，并指定新的分桶数，然后通过 INSERT INTO 命令将原有正式分区的数据导入到临时分区中，通过替换操作，原子地替换原有正式分区，生成新正式分区。

# 修改分区策略
如果您希望修改正式分区的分区范围，例如合并多个小分区为一个大分区，或将一个大分区分割成多个小分区，您可以先建立对应合并或分割后范围的临时分区，然后通过 INSERT INTO 命令将原有正式分区的数据导入到临时分区中，通过替换操作，原子地替换原有正式分区，生成新正式分区。
```
首先，只有显式指定的分区表才可以创建临时分区，临时分区的创建需要遵循以下规则：

```
1、临时分区的分区列和正式分区的相同，且不可修改；
2、一张表所有临时分区之间的分区范围不可重叠，但临时分区的范围和正式分区范围可以重叠；
3、临时分区的分区名称不能和正式分区或其他临时分区的重复。
4、与正式分区一样，临时分区同样可以独立指定一些属性。包括分桶数、副本数、是否是内存表、存储介质等信息。

临时分区的创建语法与正式分区的基本一致，只有关键词TEMPORARY PARTITION不同
```
语法：

```
# 创建一个临时分区
ALTER TABLE <table_name> 
ADD TEMPORARY PARTITION <temporary_partition_name> VALUES [("value1"), {MAXVALUE|("value2")})]
[(partition_desc)]
[DISTRIBUTED BY HASH(<bucket_key>)];

ALTER TABLE <table_name> 
ADD TEMPORARY PARTITION <temporary_partition_name> VALUES LESS THAN {MAXVALUE|(<"value">)}
[(partition_desc)]
[DISTRIBUTED BY HASH(<bucket_key>)];


# 批量创建临时分区
ALTER TABLE <table_name>
ADD TEMPORARY PARTITIONS START ("value1") END ("value2") EVERY {(INTERVAL <num> <time_unit>)|<num>}
[(partition_desc)]
[DISTRIBUTED BY HASH(<bucket_key>)];
```

```
# 新增一个临时分区tp01：
ALTER TABLE table05 ADD TEMPORARY PARTITION tp01 VALUES [("2021-12-01"), ("2021-12-02"));

# 创建临时分区tp02，并设置该分区为单副本，分桶数为8：
ALTER TABLE table05 ADD TEMPORARY PARTITION tp02 VALUES LESS THAN("2021-12-20")
("replication_num" = "1")
DISTRIBUTED BY HASH(`order_id`) BUCKETS 8;

# 查询临时分区
SELECT ... FROM
tbl1 TEMPORARY PARTITION(tp1, tp2, ...)
JOIN
tbl2 TEMPORARY PARTITION(tp1, tp2, ...)
ON ...
WHERE ...;

# 删除临时分区
ALTER TABLE table05 DROP TEMPORARY PARTITION tp01;

# 查看临时分区数据
SELECT * FROM table05 TEMPORARY PARTITION(tp02);


# 导入数据到临时分区
Insert into：
INSERT INTO tbl TEMPORARY PARTITION(tp1, tp2, ...) SELECT ....


Stream Load：
curl --location-trusted -u root: -H "label:123" -H "temporary_partitions: tp1, tp2, ..." ……


Broker Load：
LOAD LABEL example_db.label1
(
DATA INFILE("hdfs://hdfs_host:hdfs_port/user/data/input/file")
INTO TABLE `my_table`
TEMPORARY PARTITION (tp1, tp2, ...)
…………


Routine Load：
CREATE ROUTINE LOAD example_db.test1 ON example_tbl
COLUMNS(k1, k2, k3, v1, v2, v3 = k1 * 100),
TEMPORARY PARTITIONS(tp1, tp2, ...)
…………

```
**使用临时分区进行替换**

您可以通过以下命令使用临时分区替换原有正式分区，形成新正式分区。分区替换成功后，原有正式分区和临时分区被删除且不可恢复。

```
ALTER TABLE <table_name> 
REPLACE PARTITION (<partition_name1>[, ...]) WITH TEMPORARY PARTITION (<temporary_partition_name1>[, ...])
[PROPERTIES ("key" = "value")];
```
示例:
用调整过分桶数和副本数的临时分区tp02来替换table05中的正式p20210929和p20210930，写法为：

```
ALTER TABLE table05 REPLACE PARTITION (p20210929,p20210930) WITH TEMPORARY PARTITION (tp02)
PROPERTIES (
    "strict_range" = "false",
    "use_temp_partition_name" = "true"
);
```
strict_range：默认为true。对于Range分区，当该参数为true时，表示要被替换的所有正式分区的范围并集需要和替换的临时分区的范围并集完全相同。当置为 false 时，只需要保证替换后，新的正式分区间的范围不重叠即可。上面的例子中因为范围不相同，我们设为了false。

use_temp_partition_name：默认为false。当该参数为false，并且待替换的分区和替换分区的个数相同时，替换后的正式分区名称维持不变。如果为true，或待替换分区与替换分区的个数不相同，则替换后，正式分区的名称为替换分区的名称。在上面的例子中，因为待替换分区和替换分区的个数不相同，所以此时table05中的正式分区的名称变为tp02。

分区替换成功后，被替换的分区将被删除，且不可恢复。

# 冷热分区

在很多业务场景中，较近时间段的数据通常是查询最频繁的，时间较久的历史数据查询频率可能就会低很多。StarRocks支持在一个BE中使用多种存储介质（HDD/SSD/Mem），这样我们就可以将最新数据所在的分区放在SSD上，利用SSD的随机读写性能来提高查询性能。而老的数据会自动迁移至HDD盘中，以节省数据存储的成本。此外，StarRocks也是支持内存表的，但这部分功能很久没有优化了，目前不推荐使用。

首先明确一点，若集群服务器的存储介质单一（只有机械磁盘，又或者全为固态硬盘），我们就不需要再单独设置什么。例如集群中的磁盘全为SSD，虽然StarRocks在不额外设置参数时默认展示磁盘为HDD，但由于SSD带来的性能提升是源自物理层面的，所以并不会影响实际性能。

当同一台服务器中既有SSD又有HDD时，StarRocks并不会自动识别磁盘的类型，我们需要在be.conf中为storage_root_path显式的指定存储介质类型，格式可以参考be.conf中的示例：

```
# storage_root_path = /data1,medium:HDD,capacity:50;/data2,medium:SSD,capacity:1;/data3,capacity:50;/data4
# /data1, capacity limit is 50GB, HDD;
# /data2, capacity limit is 1GB, SSD;
# /data3, capacity limit is 50GB, HDD(default);
# /data4, capacity limit is disk capacity, HDD(default)
```
StarRocks的冷热分区目前有以下几个使用方式：

```
1、在建表时，指定表级别的存储介质及存储到期时间；
2、建表完成后，修改分区的存储介质及存储到期时间；
3、建表完成后，新增分区时设置分区的存储介质及存储到期时间；
4、当前不支持在建表时为某个分区单独设置分区的到期时间，同样，也不支持设置动态分区自动创建的新分区的到期时间。
```
举个例子，在集群的be.conf中设置完SSD及HDD后，创建表table08：

```
CREATE TABLE table08 (
    user_id INT COMMENT "id of user",
    device_code INT COMMENT "code of device",
    device_price DECIMAL(10,2) COMMENT "",
    event_time DATETIME NOT NULL COMMENT "datetime of event",
    total DECIMAL(18,2) SUM DEFAULT "0" COMMENT "total amount of equipment",
    index index01 (user_id) USING BITMAP COMMENT "bitmap index"
)
AGGREGATE KEY(user_id, device_code,device_price,event_time)
PARTITION BY RANGE(event_time)
(
PARTITION p1 VALUES LESS THAN ('2022-01-01'),
PARTITION p2 VALUES LESS THAN ('2022-01-02'),
PARTITION p3 VALUES LESS THAN ('2022-01-03')

)
DISTRIBUTED BY HASH(user_id,device_code) BUCKETS 20
PROPERTIES (
"replication_num" = "1",
"storage_medium" = "SSD",
"storage_cooldown_time" = "2023-01-01 23:59:59"
);
```
这里的storage_cooldown_time参数若不显式设置，默认为1个月，默认时间可以通过fe.conf中storage_cooldown_second参数调整。

修改分区p3的存储到期时间：
```
ALTER TABLE table08 MODIFY PARTITION p3 SET("storage_medium"="SSD", "storage_cooldown_time"="2023-03-11 10:29:01");
```
新增分区p4，指定存储介质及存储到期时间：
```
ALTER TABLE table08 ADD PARTITION p4 VALUES LESS THAN ('2022-01-04') ("storage_medium" = "SSD","storage_cooldown_time"="2023-03-11 10:29:01");
```
在table08中，我们虽然在PROPERTIES 中设置了"storage_medium" = "SSD"和"storage_cooldown_time"，但这个属性仅会用于表创建时的三个分区，后面新建的分区若不指定，还是会使用HDD（也就没有所谓的存储到期时间了）。这里的默认存储介质类型受fe.conf中的default_storage_medium参数控制，默认为HDD，我们可以设置为：default_storage_medium=SSD

注意，只有SSD的存储到期时间有意义，在HDD中，到期时间都为9999-12-31，也即为无到期时间。当时钟到达分区存储到期时间后，会触发迁移逻辑，该分区存储在SSD中的数据会向HDD中迁移。

这里同样引出一个注意事项，当我们的存储介质全为SSD时，我们前面提到过，完全可以不单独设置参数，也即be.conf和建表语句中都不用设置。此时，若我们在建表语句中设置了"storage_medium" = "SSD"，那同时就需要注意给一个较大的"storage_cooldown_time"到期时间，以避免分区到期后后台不断触发迁移逻辑。


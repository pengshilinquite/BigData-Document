数仓面试点

		1.数据中台，数据仓库，数据湖的区别，请举例说明?
		回答：我理解的是，数据仓库面向不同主题，不同领域（比如零售，电商，财经，渠道，服务），比较有结构化，通常采用数仓建模的方式，用于用户进行分析和决策的，比如产品的销量，渠道，利润等等
			  然后数据湖：他里面存的是大量的数据，结构化数据，非结构化数据，比如一些点击量，日志，通过对海量的数据进行挖掘，分析，挖掘出有价值的信息，比如做成用户画像
			  数据中台：是为了数据的共享和服务化，强调数据的开放性和灵活性
			  
			  
		2.什么是数据仓库，如何构建?
		回答：数仓是个一套数据管理的思想方法。 				           数仓通过数据建模，比如星型模型，雪花模型将数据进行分层，每一层都有自己明确的分工，对数据进行加工清洗，etl等，每一层都是严格按照规范处理的，从而保证数据的一致性和准确性
		
	数仓建模过程：确定业务过程，然后划分主题域，业务对象，ER图,数据来源，逻辑实体（表的中文名称英文名称）,粒度，字段的中英文和类型，字段的取值逻辑，主键，外键，属性，度量，扩展字段，审计字段，分区字段等
		
		


        3.数仓的星型模型和雪花模型有什么区别？
		 回答： 星型模型：是以事实表为中心，若干个维度表组成的模型，事实表包含了业务所有的度量值和维度信息，包含业务发生的一系列的动作，比如下单时间，发货时间，金额，客户，区域等待
				雪花模型则是 星型模型的扩展，他对维度进行一个升维处理，将维度拆分的更细，比如一个区域表，我们可以拆成国家区域和城市门店这种
				
				
		
		4.区域维度表的设计原则：
				1.统一的唯一标识符：每个区域应该有一个唯一的标识符。通常情况下，可以使用数值型的主键作为区域表的主键，并为每个区域分配一个唯一的数字。
				2.树形结构的设计：区域表通常是一种树形结构，因为区域之间存在层次关系，比如国家包含省份，省份包含城市等。在设计区域表时，应该考虑到这种树形结构的特点，并将其反映在表的设计中。可以使用上级区域的主键作为当前区域的外键，以建立区域之间的父子关系
				3.区域信息的存储：每个区域表中应该包含区域的基本信息，比如名称、代码、拼音等。这些信息可以用于数据分析和报表制作，可以方便用户通过名称、代码等方式查询区域信息。
				4.区域层级关系的存储：区域表应该存储区域之间的层级关系，这样才能建立区域之间的父子关系，以支持多层次的数据分析和报表制作
				5.区域变更的处理：由于区域信息可能会发生变化，比如某个省份改名或划分，因此需要对区域表进行有效的变更处理。一种常见的方法是采用拉链表的方式，为每个区域保留历史信息，并记录每个区域的有效期
				6.区域表的数据更新：由于区域表是一种基础表，其中的数据不会经常发生变化，因此可以采用一些优化方式来更新区域表的数据，比如增量更新和批量更新等。
	
	    4.数仓为什么分层
			得到一个比较清晰结构的数据
			将复杂的问题进行分层解决
			统一数据的口径
			保证数据的共享性，减少重复开发
		 
		 
		5.请说明什么是元数据和元数据是怎么管理的，请举例说明？
		元数据是描述数据的数据，它提供有关数据的信息，如数据的定义、结构、数据类型、数据来源、数据格式、数据质量、数据的使用方式等信息。在数据管理中，元数据通常是指描述数据的属性、结构和关系等信息的数据，包括数据仓库、数据湖、数据集市等系统中的数据元素、数据字段、数据表、数据视图等对象的信息。

		元数据的管理包括元数据的采集、存储、维护和使用。在数据管理中，元数据通常是由元数据管理系统(Metadata Management System)来实现的。元数据管理系统可以对数据进行分类、组织和存储，也可以对元数据进行检索、查询和修改。

		举例来说，假设我们有一个数据仓库，其中包含了销售数据的信息。这个数据仓库中的元数据可能包括以下信息：

		数据表的名称、描述、创建日期和最后修改日期
		数据表中的数据字段、数据类型、长度和描述
		数据表之间的关系、外键和约束
		数据表的索引、分区和分桶等信息
		数据表的存储位置、数据存储格式和访问权限等信息
		这些元数据可以帮助我们更好地理解数据仓库中的数据，更好地管理数据质量，更好地进行数据分析和挖掘。
		
		HIVE 优化：
			hive参数优化：
			map端优化
			hive.map.aggr=true  
			set mapred.max.split.size=100000000;
			set mapred.min.split.size.per.node=100000000;
			set mapred.min.split.size.per.rack=100000000;
			set hive.input.format=org.apache.hadoop.hive.ql.io.CombineHiveInputFormat;
			
			
			--2.0版本以后的 map切割参数
			SET mapreduce.input.fileinputformat.split.maxsize=256000000;
			SET mapreduce.input.fileinputformat.split.minsize=256000000;
			
			reduce参数优化：
			set hive.exec.reducers.bytes.per.reducer=500000000: 设定一个reduce处理的数据量为500M
			set mapred.reduce.tasks = 100  --强制reduce数量
			
			
			小文件合并参数
			set hive.merge.mapfiles = true: 在Map-only的任务结束时合并小文件

			set hive.merge.mapredfiles = true: 在Map-Reduce的任务结束时合并小文件
			
			set hive.merge.size.per.task = 256*1000*1000: 合并文件的大小
			
			set hive.merge.smallfiles.avgsize=16000000: 当输出文件的平均大小小于该值时，启动一个独立的map-reduce任务进行文件merge
			
			
			数据倾斜参数
			hive.groupby.skewindata = true
			开启mapjoin
			set hive.auto.convert.join=true;
			set hive.mapjoin.smalltable.filesize=25000000;
			
			
			开启压缩参数：
			set mapreduce.map.output.compress:开启mapreduce中map输出压缩功能

			set hive.exec.compress.intermediate=true: 开启传输数据压缩功能
			
			set hive.exec.compress.output=true: 开启mapreduce最终输出数据压缩
			
			set mapred.output.compression.codec=org.apache.hadoop.io.compress.GzipCodec: 设置为Gzip格式压缩
			
			set mapred.output.compression.codec=org.apache.hadoop.io.compress.SnappyCodec: 设置为SnappyCodec压缩
			
			并行执行
			set hive.exec.parallel=true;
			set hive.exec.parallel.thread.number=16;
			
			
			代码层面优化：
			一张大表关联多个小表，可以把大表放在缓存中，把小表按照从小到大的顺序排列
			如果小表不大由不小，在map端解压的时候可能造成内存溢出，可以提前对小表进行维度剪裁，可以从减少字段和提前和事实表关联过滤的形式来进行剪裁
			
			减少子查询的数量
			过滤的时候减少对全表进行扫描，比如如果对会计期进行过滤，可以通过分区进行过滤
			多表进行union all 的时候开启并行参数
			如果多个临时表不存在并行关系，也可以分成两个脚本并行跑
			数据关联的时候，避免数据类型转换导致的关联较慢
			
			关联如果数据倾斜了，可以将关联的键给打散，通过撒盐，或者提前进行聚合，或者过滤掉空值这种方式，或者提前将倾斜的健给拿出来
			开启数据倾斜参数，开启mapjoin ，开启压缩，强行增大reduce
			
			对于大表和大表关联，可以将大表截取为多个小表进行关联
			
			
			
			
			
			
			
			spark 面试题
			什么是RDD：是一个不可变的、可分区的数据集合，能够并行处理，且能够容错的分布式数据结构，RDD是惰性求值的，只有在遇到action操作时才会触发计算。
			一组分片（Partition）：数据集合被分成若干个Partition，每个Partition分布在不同的机器上，数据是分布式存储的。
			一个RDD主要包括以下内容：
			依赖关系（Dependencies）：RDD之间的依赖关系，用于记录每个RDD如何从父RDD中派生出来的。
			转换操作（Transformations）：通过不同的操作（如map、filter、join等）对RDD进行转换处理。
			行动操作（Actions）：对RDD执行一些行动操作（如count、collect、reduce等），触发计算并返回结果。
			
			Driver：Spark中的Driver即运行上述Application的main函数并创建sparkContext，创建SparkContext目的是为了准备Spark应用程序的运行环境，在Spark中有SparkContext负责与ClusterManager通信，进行资源的申请，任务的分配和监控等（由clusterManager来完成），当Executor部分运行完毕后，Driver同时负责将SparkContext关闭，通常用SparkContext代表Driver
			Executor：某个Application运行在work节点上的一个进程，改进程负责运行某些Task，并且负责将数据存到内存或磁盘上，每个Application都有各自独立的一批Exector，在Spark on yarn 模式下，其进程名为 CoarseGrainedExecutor Backend。 一个 CoarseGrainedExecutor Backend有且有一个Exector对象，负责将Task包装成taskRunner，并从线程池抽取一个空闲的线程运行Task，这个每个 CoarseGrainedExecutor Backend能并行运行Task的数量取决于分配给它的cpu个数
			
			
			spark 优化
			六大代码优化:
		避免创建重复的RDD 
		尽可能复用同一个RDD 
		对多次使用的RDD进行持久化 
		尽量避免使用shuffle类算子 
		使用map-side预聚合的shuffle操作 
		使用高性能的算子 
		广播大变量
		使用Kryo优化序列化性能 
		优化数据结构
	    使用高性能的库fastutil 
		
		使用reduceByKey/aggregateByKey替代groupByKey 
		
		使用mapPartitions替代普通map Transformation算子 
		
		使用foreachPartitions替代foreach Action算子 
		
		使用filter之后进行coalesce操作 
		
		使用repartitionAndSortWithinPartitions替代repartition与sort类操作代码
		
		repartition:coalesce(numPartitions，true) 增多分区使用这个 
		
		coalesce(numPartitions，false) 减少分区 没有shuffle只是合并 partition 
		-num-executors executor的数量
--executor-memory 每一个executor的内存
--executor-cores 每一个executor的核心数
--driver-memory Driver的内存1G-2G(保存广播变量)
--spark.storage.memoryFraction 用于缓存的内存占比默认时0.6,如果代码中没有用到缓存 可以将内存分配给shuffle
--spark.shuffle.memoryFraction 用户shuffle的内存占比默认0.2

总的内存=num-executors*executor-memory
总的核数=num-executors*executor-cores


spark on yarn 资源设置标准
1、单个任务总的内存和总的核数一般做多在yarn总资源的1/3到1/2之间
比如公司集群有10太服务器
单台服务器内存是128G,核数是40
yarn总的内存=10*128G=1280G*0.8=960G 需要预留一般分内存给系统进程
yarn总的核数=40*10=400

提交单个spark任务资源上线
总的内存=960G *(1/3| 1/2) = 300G-500G
总的核数=400 * (1/3| 1/2) = 120 - 200

2、在上线内再按照需要处理的数据量来合理指定资源 -- 最理想的情况是一个task对应一个core

2.1、数据量比较小 - 10G
10G = 80个block = rdd80分区 = 80个task
- 最理想资源指定 -- 剩余资源充足
--num-executors=40
--executor-memory=4G
--executor-cores=2
- 资源里面最优的方式 -- 剩余资源不是很充足时
--num-executors=20
--executor-memory=4G
--executor-cores=2

2.2、数据量比较大时 - 80G
80G = 640block = 640分区 = 640task
- 最理想资源指定 -- 剩余资源充足, 如果剩余资源不够，还需要减少指定的资源
--num-executors=100
--executor-memory=4G
--executor-cores=2
			






			
			
			
			
			
			
			
			
				
		
		
		
		
		
		







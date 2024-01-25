公司重新搭建CDH6.3.0，并把旧集群Hive1.1迁移新集群Hive2.1，记录一下过程。

# 一. 迁移Hive数据和MySQL中的matastore
通过DistCp拷贝Hive数据到新集群，并从MySQL中导出Hive的元数据上传到新集群，最后在新集群MySQL中导入Hive元数据，并更新Hive版本，修改信息。

**1. 迁移Hive数据和MySQL中的matastore**

版本差异大，使用htfp

```
hadoop distcp -skipcrccheck -update htfp://hadoop-master-001:50070/user/hive/* \
hdfs://cdh-master-001:8020/user/hive

#因为从Hadoop2.6迁移到Hadoop3.0版本，使用hftp方式。
#源集群的格式是 hftp://<dfs.http.address>/<path> ，默认设置dfs.http.address是 <namenode>:50070。
#新的webhdfs协议代替了hftp后，源地址和目标地址都可以使用http协议webhdfs，可以完全兼容 。

hadoop distcp -skipcrccheck -update webhdfs://hadoop-master-001:50070/user/hive/* \
webhdfs://cdh-master-001:50070/user/hive
```
**2. 在源集群上MySQL导出Hive metastore**
```
mysqldump -uroot -p123456 --databases hive > mysql_hive.sql

#--skip-lock-tables，导出时会锁定所有表，如果不锁表，一边导出一边录入数据，会出问题
```
**3. 在新集群使用Hive用户导入metastore**
```
mysql -uhive -p123456 --default-character-set=utf8  hive < mysql_hive.sql
```
**4. 升级Hive库**

Hive版本相同不用升级。要根据版本序列升级，不能跨版本。
```
mysql -uroot -proot risk -hcdh-master < mysqlupgrade-1.1.0-to-1.2.1.mysql.sql
mysql -uroot -proot risk -hcdh-master < mysqlupgrade-1.2.1-to-2.0.0.mysql.sql
mysql -uroot -proot risk -hcdh-master < mysqlupgrade-2.0.0-to-2.1.1.mysql.sql
```
**5. 修改metastore的集群信息**

如果新集群名字跟源集群相同可以不用修改，否则需要修改hive库的DBS和SDS表内容。
```
#查看HDFS上数据存放位置
use hive;
select * from DBS;

update DBS set DB_LOCATION_URI = replace(DB_LOCATION_URI,
'hdfs://hadoop-master-001:8020',
'hdfs://cdh-master-001:8020') ;

update SDS set LOCATION = replace(LOCATION ,
'hdfs://hadoop-master-001:8020',
'hdfs://cdh-master-001:8020') ;
```
# 二. export / import + distcp
使用export将Hive表及元数据文件导出到HDFS文件系统，通过Distcp命令将导出的元数据文件迁移到新集群的HDFS文件中，最后在新集群中通过import命令导入表。

**1. 导出Hive数据到HDFS**

导出的数据包括_metadata和data文件夹，如果有分区，data为分区文件夹
```
#!/bin/bash

##自动导出hive表到HDFS

#输入数据库
DB=$1

#获取hive建表语句
tables=$(hive -e "use $DB; show tables;")

# echo "============== 开始生成hive-export.hql =============="
hive -e "use $DB;show tables" | awk '{printf "export table %s to |/tmp/bak/hive-export/%s|;\n",$1,$1}' \
| sed "s/|/'/g" > /user/bak/hive/hive-export.hql


# echo "============== hive-export.hql生成成功! =============="
# echo "================== 开始导出hive数据 =================="
hive -database $DB -f "/hadoop/bak/hive/hive-export.hql"

# awk{printf "|%s|",$1},管道符两个竖杠用于指示格式的起始与结束,$1替换%s,\n换行符结尾
# sed "s/|/'/g",sed s/被替换内容/替换内容/,g(GLOBAL)全部替换,无g只替代每行第一个
```
**2. 拷贝导出的Hive数据到新集群**

注意：导出数据之前，需要先确认hive的数据格式是orc还是parquet格式，因为orc格式hive的高版本不兼容低版本

原集群是CDH5.7、Hadoop2.6、HDFS端口50070，新集群是CDH6.3.0、Hadoop3.0、HDFS端口9870。采用webhdfs协议传输，记得原集群HDFS集群需要添加新集群服务器的host。
```
hadoop distcp webhdfs://hadoop-master-001:50070/tmp/hive-export/ \
webhdfs://cdh-master-001:9870/tmp/hive-export/
```
**3. 修改导出脚本为导入脚本**
```
cp hive_export.hql hive_import.sql
sed -i 's/export table/import table/g' hive-import.hql
sed -i 's/ to / from /g' hive-import.hql
```

**4. 上传导入脚本后在新集群执行**
```
hive -database cp_data -f hive-import.sql
```
# 三、数据迁移(因为新集群和阿里云的对象存储打通了，所以我的数据都放到了oss上)
**1.按项目迁移代码**

```
----------------------格式转换后台shell脚本
#!/bin/bash
echo 'start'
for t in `cat flag.txt` #同一行中的内容如果有空格，会被拆分成多个
do
  echo "$t"
  table=$t
  echo '' >./$table.sql
  echo '
use tools;
set spark.dynamicAllocation.enabled=false;--关闭executor动态分配功能，防止单个任务分配的资源太多
set spark.executor.instances=3;--设置可用的executor个数(3个)
set spark.executor.cores=5;--设置executor拥有的core数(5C)
set spark.executor.memory=8000000000b;--设置executor拥有的内存数(8G)
set mapreduce.map.memory.mb=3072;
set mapreduce.reduce.memory.mb=3072;
set hive.exec.dynamic.partition=true;
set hive.exec.dynamic.partition.mode=nonstrict;
set hive.exec.max.dynamic.partitions.pernode=10000;
set hive.exec.max.dynamic.partitions=10000;
set hive.exec.max.created.files=10000;
set hive.input.format=org.apache.hadoop.hive.ql.io.CombineHiveInputFormat;
set mapred.max.split.size=50000000;
set mapred.min.split.size.per.node=50000000;
set mapred.min.split.size.per.rack=50000000;
  ' >>./$table.sql
  echo "insert overwrite table ${table}_tran partition(pt) select * from $table;" >>./$table.sql
done
echo 'end'

--------------------------------把hdfs文件从老集群迁移到新集群--------------------------------

--在新集群worker节点上执行
--删除新集群上的路径
#!/bin/bash
echo 'start'
for t in `cat move_flag.txt` #同一行中的内容如果有空格，会被拆分成多个
do
echo "$t"
table=$t
hadoop distcp hdfs://hadoop-master-001:50070/group/user/tools/meta/hive-temp-table/${table}_tran/* oss://bigdata/group/user/tools/meta/hive-temp-table/${table}
done
echo 'end'

nohup ./move.sh &> move.log &
nohup ./move2.sh &> move2.log &
```
**2.重新建表及导入数据**

```
--重新创建表
drop table if exists xxx;
create external table if not exists xxx (
`xxx` string comment 'xxx',
`xxx` string comment 'xxx'
`xxx` string comment 'xxx',
`xxx` string comment 'xxx',
`xxx` string comment 'xxx',
`xxx` string comment 'xxx',
`xxx` string comment 'xxx',
`xxx` string comment 'xxx'
)
comment 'xxx'
PARTITIONED BY (pt STRING)
row format delimited
fields terminated by '\001'
lines terminated by '\n'
STORED AS parquet tblproperties ("orc.compress"="SNAPPY");
ALTER TABLE dwm_sony_app_opt_cd SET SERDEPROPERTIES('serialization.null.format' = '');

--修复分区
MSCK REPAIR TABLE xxx;
show partitions xxx;
select * from xxx limit 1;

--删除数据
hdfs dfs -rm -r -skipTrash hdfs://hadoop-master-001:50070/group/user/tools/meta/hive-temp-table/xxx
drop table xxx;
--如果有clickhouse的导出任务，那么conf文件中的orc格式要修改为parquet格式
```


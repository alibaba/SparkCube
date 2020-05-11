# SparkCube

SparkCube is an open-source project for extremely fast OLAP data analysis. SparkCube is an extension of [Apache Spark](http://spark.apache.org).

## Build from source

```
mvn -DskipTests package
```

The default Spark version used is 2.4.4.

## Run tests

```
mvn test
```

## Use with Apache Spark

There are several configs you should add to your Spark configuration.

| config | value | comment |  |
| ---- | ---- | ---- | ---- |
| spark.sql.extensions | com.alibaba.sparkcube.SparkCube |Add extension |Required|
| spark.sql.cache.tab.display | true | To show web UI in the certain application, typically Spark Thriftserver. |Required|
| spark.sql.cache.useDatabase | db1,db2,dbn | Different database names are separated by commas to store your view and cube|Required|
| spark.sql.cache.cacheByPartition | true | To store cache by partition|Optional|
| spark.driver.extraClassPath |  /path/to/this/jar | For web UI resources. |Required|

With the configurations above set in your Spark thriftserver, you should be able to see "Cube Management" Tab from the UI of Spark Thriftserver after any `SELECT` command is run. Then you can create/delete/build cubes from this web page.

After you created appropriate cube, you can query the cube from any spark-sql client using Spark SQL. Note that the cube can be created against table or view, so you can join tables as view to create a complex cube.

If you want a more detailed tutorial for cube create/build/drop etc, please refer to
https://help.aliyun.com/document_detail/149293.html

## Learning materials

https://yq.aliyun.com/articles/703046

https://yq.aliyun.com/articles/703154

https://yq.aliyun.com/articles/713746

https://yq.aliyun.com/articles/725413

https://www.slidestalk.com/AliSpark/SparkRelationalCache78971

https://www.slidestalk.com/AliSpark/SparkRelationalCache2019_57927

(In English)

https://community.alibabacloud.com/blog/rewriting-the-execution-plan-in-the-emr-spark-relational-cache_595267

https://www.alibabacloud.com/blog/use-emr-spark-relational-cache-to-synchronize-data-across-clusters_595301

https://www.alibabacloud.com/blog/using-data-preorganization-for-faster-queries-in-spark-on-emr_595599

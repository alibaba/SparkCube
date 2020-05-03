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

| config | value | comment |
| ---- | ---- | ---- |
| spark.sql.extensions | com.alibaba.sparkcube.SparkCube |  |
| spark.sql.cache.tab.display | true | To show web UI in the certain application, typically Spark Thriftserver. |
| spark.sql.cache.useDatabase | db1,db2,dbn | Different database names are separated by commas to store your view and cube|
| spark.driver.extraClassPath |  /path/to/this/jar | For web UI resources. |

With the configurations above set in your Spark thriftserver, you should be able to see "Cube Management" Tab from the UI of Spark Thriftserver after any `SELECT` command is run. Then you can create/delete/build cubes from this web page.

After you created appropriate cube, you can query the cube from any spark-sql client using Spark SQL. Note that the cube can be created against table or view, so you can join tables as view to create a complex cube.

## Thanks

| Name | Company |
| -- | -- |
| Chengxiang Li | Alibaba |
| Daoyuan Wang | Alibaba |
| Tao Wang | Alibaba |

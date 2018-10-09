# JMH performance MariaDB/MySQL java driver test

We always talk about performance, but the thing is always "Measure, don’t guess!".
This is a benchmark of [MariaDB java connector](https://github.com/MariaDB/mariadb-connector-j) versus [MySQL java connector](https://github.com/mysql/mysql-connector-j).
MariaDB and MySQL databases are using the same exchange protocol, and driver offer similar functionalities. 

This is a Driver benchmark using [JMH microbenchmark](http://openjdk.java.net/projects/code-tools/jmh/)
developed by the same guys in Oracle who implement the JIT, and is delivered as openJDK tools.

This will permit to compare query execution time using different driver :
<center><img src ="results/select_one_data.png" /></center>


## The tests
Class Common initialize connections using MySQL, MariaDB and Drizzle drivers before tests.

test example org.perf.jdbc.Select_1 : 
```java
public class Select_1 extends Common {
  private String request = "select 1";

  public int executeQuery(Statement stmt) throws SQLException {
    ResultSet rs = stmt.executeQuery(request);
    rs.next();
    return rs.getInt(1);
  }

  @Benchmark
  @Fork(jvmArgsAppend = {"-Xmx32m", "-Xms32m"})
  public int test(MyState state) throws Throwable {
    return executeQuery(state.statement);
  }
}
```

The test will execute the statement "select 1" using a connection issued from java driver MySQL 8.0.12, Drizzle 1.4 or MariaDB 2.4.0.

Tests are launched multiple times using 20 forks , 10 warmup iterations of one second followed by 10 measurement iterations of one second. (benchmark duration is approximately 2h)


List of tests and their signification :

|Benchmark       | Description |
|-----------|:----------|
| Do_1                               | execute query "do 1" (smallest query without resultset)|
| Create_and_close_Connection        | create and close a connection|
| Select_1                           | execute query "select 1" (smallest query with resultset)|
| Select_1_mysql_user                | execute query "select * from mysql.user limit 1" (resultset with 46 field)|
| Select_10_cols_from_seq_1_to_100000| execute query with 100 000 rows of 10 columns of 100 chars|

'* The goal is here to test the driver performance, not database **

## How run the tests
* install a MySQL / MariaDB database
* create database "testj" : `create database testj;`
* create user : `CREATE USER 'perf'@'%' IDENTIFIED BY '!Password0';`
* create user perf : `GRANT ALL ON *.* TO 'perf'@'%' IDENTIFIED BY '!Password0';`
* grant super access : `GRANT SUPER ON *.* TO 'perf'@'%';`
* install a JRE
(* install maven)
(* install git)


```script
git clone https://github.com/rusher/mariadb-java-driver-benchmark.git
cd mariadb-java-driver-benchmark
mvn clean install
nohup java -Duser.country=US -Duser.language=en -jar target/benchmarks.jar > result.txt &
```
 
JMH has a lot of options, 2 interesting ones : add a regex to launch only one specific benchmark, and add a garbage profiler to see consume time in GC.
```script
java -Duser.country=US -Duser.language=en -jar target/benchmarks.jar  ".Do_1*" -prof gc > result.txt &
```

## Read results 

Results are in file "result.txt".
Complete results are the end of the file. 

Execution on a basic droplet on digitalocean.com using this parameters:
- Ubuntu 18.04 64bits
- 1 CPU

using MariaDb 10.3.10 <a href='results/result_mariadb-10.3_server_local.txt'>local results</a>

Extract of mariadb server results with mariadb-10.3 local server :
```
Benchmark                                 (driver)  Mode   Cnt     Score    Error  Units
Create_and_close_Connection.test             mysql  avgt  4000     4.807 ±  0.147  ms/op
Create_and_close_Connection.test           mariadb  avgt  4000     2.592 ±  0.108  ms/op
Create_and_close_Connection.test           drizzle  avgt  4000     1.767 ±  0.079  ms/op
Do_1.test                                    mysql  avgt   200    45.949 ±  4.382  µs/op
Do_1.test                                  mariadb  avgt   200    39.141 ±  0.843  µs/op
Do_1.test                                  drizzle  avgt   200    41.775 ±  0.788  µs/op
Select_1.test                                mysql  avgt   200    74.722 ±  1.524  µs/op
Select_1.test                              mariadb  avgt   200    57.100 ±  1.208  µs/op
Select_1.test                              drizzle  avgt   200    61.473 ±  1.696  µs/op
Select_10_cols_from_seq_1_to_100000.test     mysql  avgt   100  1396.190 ± 26.494  ms/op
Select_10_cols_from_seq_1_to_100000.test   mariadb  avgt   100  1030.726 ± 14.910  ms/op
Select_10_cols_from_seq_1_to_100000.test   drizzle  avgt   100  1711.404 ± 33.255  ms/op
Select_1_mysql_user.test                     mysql  avgt   200   201.173 ±  2.973  µs/op
Select_1_mysql_user.test                   mariadb  avgt   200   167.166 ±  2.429  µs/op
Select_1_mysql_user.test                   drizzle  avgt   200   224.052 ±  3.030  µs/op
```
or see travis results on https://travis-ci.org/rusher/mariadb-java-driver-benchmark

##### How to read it :

ms/op means millisecond per operation, µs/op microsecond per operation.

```
Benchmark                                 (driver)  Mode   Cnt     Score    Error  Units
Select_1_mysql_user.test                     mysql  avgt   200   201.173 ±  2.973  µs/op
Select_1_mysql_user.test                   mariadb  avgt   200   167.166 ±  2.429  µs/op
Select_1_mysql_user.test                   drizzle  avgt   200   224.052 ±  3.030  µs/op
```


<p>Select_1_mysql_user.test : Using same local database, time for query "SELECT * FROM mysql.user LIMIT 1" <br/>
Using mariadb driver, the average time to select one data is 167 microsecond, and 99.9% of queries executes time are comprised between (167.166 - 2.429) and (167.166 + 2.429) microseconds.<br/>
Using MySQL java driver, average execution time is 201 millisecond, using Drizzle driver 224 milliseconds
   </p>




# JMH performance MariaDB/MySQL driver test

We always talk about performance, but the thing is always "Measure, don’t guess!".
This is a benchmark of [MariaDB java connector](https://github.com/MariaDB/mariadb-connector-j) versus [MySQL java connector](https://github.com/mysql/mysql-connector-j).
MariaDB and MySQL databases are using the same exchange protocol, and driver offer similar functionalities. 

This is a Driver benchmark using [JMH microbenchmark](http://openjdk.java.net/projects/code-tools/jmh/)
developed by the same guys in Oracle who implement the JIT, and is delivered as openJDK tools.

This will permit to compare execution time of a query using different driver :
<center><img src ="results/select_one_data.png" /></center>


## The tests
Class BenchmarkInit initialize connections using MySQL and MariaDB drivers before tests.

test example org.perf.jdbc.BenchmarkSelect1RowPrepareText : 
```java

public class Select1 extends Common {
  private String request = "select 1";

  public int executeQuery(Statement stmt) throws SQLException {
    ResultSet rs = stmt.executeQuery(request);
    rs.next();
    return rs.getInt(1);
  }

  @Benchmark
  @Fork(jvmArgsAppend = {"-Xmx128m", "-Xms128m", "-Duser.country=US", "-Duser.language=en"})
  public int test(MyState state) throws Throwable {
    return executeQuery(state.statement);
  }
}
```

The test will execute the statement "select 1" using a connection issued from java driver MySQL 8.0.12, Drizzle 1.4 or MariaDB 2.4.0.

Tests are launched multiple times using 10 forks , 10 warmup iterations of one second followed by 15 measurement iterations of one second. (one test duration is approximately 4h)


List of tests and their signification :

|Benchmark       | Description |
|-----------|:----------|
| do1 | execute query "do 1" (smallest query without resultset)|
|select1| execute query "select 1" (smallest query with resultset)|
|selectUser| execute query "select * from mysql.user limit 1" (resultset with 46 field)|
|selectBigRows| execute query with 100 000 rows of 10 columns of 100 chars|

'* The goal is here to test the driver performance, not database **

## How run the tests
* install a MySQL / MariaDB database
* create database "testj" : create database testj;
* create user : CREATE USER 'perf'@'%' IDENTIFIED BY '!Password0';
* create user perf : GRANT ALL ON *.* TO 'perf'@'%' IDENTIFIED BY '!Password0';
* grant super access : GRANT SUPER ON *.* TO 'perf'@'%';
* install a JRE
(* install maven)
(* install git)



*
```script
git clone https://github.com/rusher/mariadb-mysql-driver.git
mvn clean install
java -Duser.country=US -Duser.language=en -jar target/benchmarks.jar > result.txt &
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
# Run complete. Total time: 03:57:41

Benchmark                                           Mode  Cnt     Score    Error  Units
BenchmarkCallableStatementFunction.mariadb          avgt  200   101.183 ±  4.594  µs/op
BenchmarkCallableStatementFunction.mysql            avgt  200   787.688 ± 62.222  µs/op
BenchmarkCallableStatementWithInParameter.mariadb   avgt  200   101.787 ±  5.378  µs/op
BenchmarkCallableStatementWithInParameter.mysql     avgt  200   553.861 ± 28.410  µs/op
BenchmarkCallableStatementWithOutParameter.mariadb  avgt  200    88.572 ±  4.263  µs/op
BenchmarkCallableStatementWithOutParameter.mysql    avgt  200   714.108 ± 44.390  µs/op
BenchmarkOneInsertPrepareHit.mariadb                avgt  200    61.298 ±  1.940  µs/op
BenchmarkOneInsertPrepareHit.mysql                  avgt  200    92.173 ±  4.887  µs/op
BenchmarkOneInsertPrepareMiss.mariadb               avgt  200   130.896 ±  6.362  µs/op
BenchmarkOneInsertPrepareMiss.mysql                 avgt  200   162.488 ±  5.931  µs/op
BenchmarkOneInsertPrepareText.drizzle               avgt  200    80.882 ±  3.384  µs/op
BenchmarkOneInsertPrepareText.mariadb               avgt  200    68.363 ±  2.686  µs/op
BenchmarkOneInsertPrepareText.mysql                 avgt  200   102.195 ±  5.691  µs/op
BenchmarkOneInsertPrepareTextHA.mariadb             avgt  200    69.581 ±  2.245  µs/op
BenchmarkOneInsertPrepareTextHA.mysql               avgt  200   157.478 ±  7.242  µs/op
BenchmarkSelect1000BigRows.drizzle                  avgt  200   126.169 ±  8.278  ms/op
BenchmarkSelect1000BigRows.mariadb                  avgt  200   100.056 ±  4.825  ms/op
BenchmarkSelect1000BigRows.mysql                    avgt  200   120.132 ±  6.597  ms/op
BenchmarkSelect1000BigRowsFetch.drizzle             avgt  200  1130.280 ± 51.862  ms/op
BenchmarkSelect1000BigRowsFetch.mariadb             avgt  200   854.730 ± 23.850  ms/op
BenchmarkSelect1000BigRowsFetch.mysql               avgt  200  1426.919 ± 75.047  ms/op
BenchmarkSelect1000Rows.drizzle                     avgt  200   406.877 ± 16.585  µs/op
BenchmarkSelect1000Rows.mariadb                     avgt  200   244.228 ±  7.686  µs/op
BenchmarkSelect1000Rows.mysql                       avgt  200   298.814 ± 12.143  µs/op
BenchmarkSelect1RowPrepareHit.mariadb               avgt  200    58.267 ±  2.270  µs/op
BenchmarkSelect1RowPrepareHit.mysql                 avgt  200    73.789 ±  1.863  µs/op
BenchmarkSelect1RowPrepareMiss.mariadb              avgt  200   118.896 ±  5.500  µs/op
BenchmarkSelect1RowPrepareMiss.mysql                avgt  200   150.679 ±  4.791  µs/op
BenchmarkSelect1RowPrepareText.drizzle              avgt  200    78.672 ±  2.971  µs/op
BenchmarkSelect1RowPrepareText.mariadb              avgt  200    62.715 ±  2.402  µs/op
BenchmarkSelect1RowPrepareText.mysql                avgt  200    88.670 ±  3.505  µs/op
BenchmarkSelect1RowPrepareTextHA.mariadb            avgt  200    64.676 ±  2.192  µs/op
BenchmarkSelect1RowPrepareTextHA.mysql              avgt  200   137.289 ±  4.872  µs/op
PrepareStatementBatch100InsertPrepareHit.mariadb    avgt  200     5.290 ±  0.232  ms/op
PrepareStatementBatch100InsertPrepareHit.mysql      avgt  200     9.015 ±  0.440  ms/op
PrepareStatementBatch100InsertRewrite.mariadb       avgt  200     0.404 ±  0.014  ms/op
PrepareStatementBatch100InsertRewrite.mysql         avgt  200     0.592 ±  0.016  ms/op
PrepareStatementBatch100InsertText.drizzle          avgt  200     7.314 ±  0.205  ms/op
PrepareStatementBatch100InsertText.mariadb          avgt  200     6.081 ±  0.254  ms/op
PrepareStatementBatch100InsertText.mysql            avgt  200     7.932 ±  0.293  ms/op
StatementBatch100Insert.drizzle                     avgt  200     7.666 ±  0.284  ms/op
StatementBatch100Insert.mariadb                     avgt  200     6.319 ±  0.239  ms/op
StatementBatch100Insert.mysql                       avgt  200     9.309 ±  0.312  ms/op
```

##### How to read it :

ms/op means millisecond per operation, µs/op microsecond per operation.

```
Benchmark                                           Mode  Cnt     Score    Error  Units
BenchmarkSelect1RowPrepareText.mariadb              avgt  200    62.715 ±  2.402  µs/op
BenchmarkSelect1RowPrepareText.mysql                avgt  200    88.670 ±  3.505  µs/op
BenchmarkSelect1RowPrepareText.drizzle              avgt  200    78.672 ±  2.971  µs/op
```


<div style="text-align:center"><img src ="results/select_one_data.png" /></div>

<p>BenchmarkSelect1RowPrepareText : Using same local database, time for query "SELECT CAST(? as char character set utf8)" <br/>
("SELECT ?" would be the same, but this query can be PREPAREd, and so permitting comparison with PREPARE)</p>
<p>
Using mariadb driver, the average time to insert one data is 62.715 microsecond, and 99.9% of queries executes time are comprised between 59.773 (62.715 - 2.402) and 65.117 microseconds (62.715 + 2.402).<br/>
Using MySQL java driver, average execution time is 88.670 millisecond, using Drizzle driver 78.672 milliseconds
   </p>




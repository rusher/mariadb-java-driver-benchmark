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

Benchmark  (driver)  Mode  Cnt   Score   Error  Units


Benchmark                         (driver)  Mode   Cnt    Score     Error  Units
Do_1.test                            mysql  avgt  200    66.752   ± 0.405  µs/op
Do_1.test                          mariadb  avgt  200    61.039   ± 0.188  µs/op
Do_1.test                          drizzle  avgt  200    64.618   ± 0.478  µs/op
Create_and_close_Connection.test     mysql  avgt  4000    4.082   ± 0.130  ms/op
Create_and_close_Connection.test   mariadb  avgt  4000    2.129   ± 0.060  ms/op
Create_and_close_Connection.test   drizzle  avgt  4000    1.677   ± 0.069  ms/op
Select_1.test                        mysql  avgt  200    83.817   ± 1.106  µs/op
Select_1.test                      mariadb  avgt  200    75.663   ± 0.929  µs/op
Select_1.test                      drizzle  avgt  200    80.440   ± 0.932  µs/op
Select_1_mysql_user.test             mysql  avgt  200   213.567   ± 1.222  µs/op
Select_1_mysql_user.test           mariadb  avgt  200   178.353   ± 0.721  µs/op
Select_1_mysql_user.test           drizzle  avgt  200   258.120   ± 1.556  µs/op


```

##### How to read it :

ms/op means millisecond per operation, µs/op microsecond per operation.

```
Benchmark                         (driver)  Mode   Cnt    Score     Error  Units
Select_1_mysql_user.test             mysql  avgt  200   213.567   ± 1.222  µs/op
Select_1_mysql_user.test           mariadb  avgt  200   178.353   ± 0.721  µs/op
Select_1_mysql_user.test           drizzle  avgt  200   258.120   ± 1.556  µs/op
```


<p>Select_1_mysql_user.test : Using same local database, time for query "SELECT * FROM mysql.user LIMIT 1" <br/>
Using mariadb driver, the average time to insert one data is 178 microsecond, and 99.9% of queries executes time are comprised between 177,632 (178.353 - 0.721) and 179,074 microseconds (178.353 + 0.721).<br/>
Using MySQL java driver, average execution time is 213 millisecond, using Drizzle driver 258 milliseconds
   </p>




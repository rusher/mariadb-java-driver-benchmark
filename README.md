# JMH performance MariaDB/MySQL driver test

We always talk about performance, but the thing is always "Measure, don’t guess!".
This is a benchmark of [MariaDB java connector](https://github.com/MariaDB/mariadb-connector-j) versus [MySQL java connector](https://github.com/mysql/mysql-connector-j).
MariaDB and MySQL databases are using the same exchange protocol, and driver offer similar functionalities. 

This is a Driver benchmark using [JMH microbenchmark](http://openjdk.java.net/projects/code-tools/jmh/)
developed by the same guys in Oracle who implement the JIT, and is delivered as openJDK tools.

## The tests
Class BenchmarkInit initialize connections using MySQL and MariaDB drivers before tests.

test example org.perf.jdbc.BenchmarkPrepareStatementOneInsert : 
```java
public class BenchmarkPrepareStatementOneInsert extends BenchmarkInit {
    private String request = "INSERT INTO PerfTextQuery (charValue) values (?)";

    @Benchmark
    public boolean mysql(MyState state) throws Throwable {
        return executeOneInsertPrepare(state.mysqlConnection, state.insertData);
    }

    @Benchmark
    public boolean mariadb(MyState state) throws Throwable {
        return executeOneInsertPrepare(state.mariadbConnection, state.insertData);
    }

    private boolean executeOneInsertPrepare(Connection connection, String[] datas) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(request)) {
            preparedStatement.setString(1, datas[0]);
            return preparedStatement.execute();
        }
    }
}
```

The test will execute the prepareStatement "INSERT INTO PerfTextQuery (charValue) values (?)" using a connection issued from java driver MySQL 5.1.38, Drizzle 1.2 or MariaDB 1.4.4.

Tests are launched multiple times using 10 forks , 15 warmup iterations of one second followed by 50 measurement iterations of one second. (one test duration is approximately 45 minutes)


List of tests and their signification :

|Benchmark       |description |
|-----------|:----------|
| BenchmarkSelect1RowPrepareText | execute query "INSERT INTO PerfTextQuery (charValue) values ('abcdefghij0123456')"|
| BenchmarkSelect1RowPrepareTextHA|same as BenchmarkSelect1RowPrepareText but using High availability configuration|
| BenchmarkSelect1RowPrepareHit| same as BenchmarkSelect1RowPrepareText but using server PREPARE with cache hit (eq : PREPARE already done)|
| BenchmarkSelect1RowPrepareMiss| same as BenchmarkSelect1RowPrepareText but using server PREPARE with cache miss (eq : execute PREPARE + DEALLOCATE PREPARE)|
| BenchmarkSelect1000Rows|execute query "SELECT * FROM PerfReadQuery" (table with 1000 rows, each rows contain < 10 bytes) )|
| BenchmarkSelect1000BigRows|execute query "SELECT * FROM PerfReadQueryBig" (table with 1000 rows, each rows contain 10kb)|
| BenchmarkOneInsertPrepareText*| execute query like "INSERT INTO PerfTextQuery (charValue) values ('abcdefghij0123456')"|
| BenchmarkOneInsertPrepareTextHA*|same as BenchmarkOneInsertPrepareText but using High availability configuration|
| BenchmarkOneInsertPrepareHit*| same as BenchmarkOneInsertPrepareText but using server PREPARE with cache hit (eq : PREPARE already done)|
| BenchmarkOneInsertPrepareMiss*| same as BenchmarkOneInsertPrepareText but using server PREPARE with cache miss (eq : execute PREPARE + DEALLOCATE PREPARE)|
| BenchmarkBatch1000InsertWithPrepare*|executing 1000 inserts using prepareStatement with "prepare" on server. (option useServerPrepStmts=true)|
| BenchmarkBatch1000InsertText*|executing 1000 inserts. (option useServerPrepStmts=false)|
| BenchmarkBatch1000InsertRewrite*|executing 1000 inserts. (option rewriteBatchedStatements=true)|
| BenchmarkCallableStatementFunction|execute CallableStatement with query "{? = CALL testFunctionCall(?,?,?)}". Function created by "CREATE FUNCTION IF NOT EXISTS testFunctionCall(a float, b bigint, c int) RETURNS INT NO SQL \nBEGIN \nRETURN a; \nEND"|
| BenchmarkCallableStatementWithInParameter|execute CallableStatement with query "{call withResultSet(?)}". Procedure created with "CREATE PROCEDURE IF NOT EXISTS withResultSet(a int) begin select a; end"|
| BenchmarkCallableStatementWithOutParameter|execute CallableStatement with query "{call inOutParam(?)}". Procedure created with "CREATE PROCEDURE IF NOT EXISTS inoutParam(INOUT p1 INT) begin set p1 = p1 + 1; end"|

'* The goal is here to test the driver performance, not database, so INSERT's queries are send to a [BLACKHOLE](https://mariadb.com/kb/en/mariadb/blackhole/) engine (data are not stored). This permit to have more stable results (less than 1% difference, without, data vary with 10% difference).



## How run the tests
* install a MySQL / MariaDB database with user root without password
* create database "testj"
* create user perf : GRANT ALL ON testj.* TO 'perf'@'%' IDENTIFIED BY '!Password0';
* install engine [BLACKHOLE](https://mariadb.com/kb/en/mariadb/blackhole/) using command "INSTALL SONAME 'ha_blackhole'" (This engine don't save data, permitting to execute INSERT queries with stable time result)
* restart database to activate the BLACKHOLE engine
* install a JRE
* install maven
* install git

```script
git clone https://github.com/rusher/mariadb-mysql-driver.git
mvn clean install
java -Xmx64m -Xms64m -Duser.country=US -Duser.language=en -jar target/benchmarks.jar > result.txt &
```
-Duser.country=US -Duser.language=en permit to avoid confusion with comma used as decimal separator / thousand separator according to countries
-Xmx64m -Xms64m is to permit to have quick garbage and have more stable results. 

JMH has a lot of options, 2 interesting ones : add a regex to launch only one specific benchmark, and add a garbage profiler to see consume time in GC.
```script
java -Xmx64m -Xms64m -Duser.country=US -Duser.language=en -jar target/benchmarks.jar  ".BenchmarkSelect1000Rows*" -prof gc > result.txt &
```



## Read results 

Results are in file "result.txt".
Complete results are the end of the file. 

Execution on a droplet on digitalocean.com using this parameters:
- CentOS 7.2 64bits
- 1GB memory
- 1 CPU

using MariaDb 10.1.13 ( with default configuration file) (<a href='results/result_mariadb_server.txt'>complete results</a>)
using mysql 5.7.12 ( with default configuration file) (<a href='results/result_mysql_server.txt'>complete results</a>)

Extract of mariadb server results :

```

# Run complete. Total time: 06:01:31

Benchmark                                           Mode  Cnt      Score     Error  Units
BenchmarkBatch1000InsertPrepare.mariadb             avgt  400     50.434 ±   0.308  ms/op
BenchmarkBatch1000InsertPrepare.mysql               avgt  400     59.998 ±   0.986  ms/op
BenchmarkBatch1000InsertRewrite.mariadb             avgt  400      2.000 ±   0.008  ms/op
BenchmarkBatch1000InsertRewrite.mysql               avgt  400      1.698 ±   0.019  ms/op
BenchmarkBatch1000InsertText.drizzle                avgt  400     84.516 ±   1.085  ms/op
BenchmarkBatch1000InsertText.mariadb                avgt  400     66.559 ±   0.795  ms/op
BenchmarkBatch1000InsertText.mysql                  avgt  400     77.262 ±   1.822  ms/op
BenchmarkCallableStatementFunction.mariadb          avgt  400    105.753 ±   1.323  us/op
BenchmarkCallableStatementFunction.mysql            avgt  400   1737.296 ±  55.898  us/op
BenchmarkCallableStatementWithInParameter.mariadb   avgt  400     81.617 ±   0.699  us/op
BenchmarkCallableStatementWithInParameter.mysql     avgt  400   1569.970 ±  57.070  us/op
BenchmarkCallableStatementWithOutParameter.mariadb  avgt  400     67.870 ±   0.730  us/op
BenchmarkCallableStatementWithOutParameter.mysql    avgt  400   1713.014 ±  56.929  us/op
BenchmarkOneInsertPrepareHit.mariadb                avgt  400     55.084 ±   0.681  us/op
BenchmarkOneInsertPrepareHit.mysql                  avgt  400     61.821 ±   0.541  us/op
BenchmarkOneInsertPrepareMiss.mariadb               avgt  400    125.902 ±   1.411  us/op
BenchmarkOneInsertPrepareMiss.mysql                 avgt  400    159.094 ±   2.600  us/op
BenchmarkOneInsertPrepareText.drizzle               avgt  400     85.936 ±   0.655  us/op
BenchmarkOneInsertPrepareText.mariadb               avgt  400     68.515 ±   0.647  us/op
BenchmarkOneInsertPrepareText.mysql                 avgt  400     91.485 ±   1.221  us/op
BenchmarkOneInsertPrepareTextHA.mariadb             avgt  400     74.725 ±   1.667  us/op
BenchmarkOneInsertPrepareTextHA.mysql               avgt  400    133.963 ±   1.521  us/op
BenchmarkSelect1000BigRows.drizzle                  avgt  400  46430.412 ± 401.072  us/op
BenchmarkSelect1000BigRows.mariadb                  avgt  400  40421.603 ± 180.436  us/op
BenchmarkSelect1000BigRows.mysql                    avgt  400  51809.219 ± 221.505  us/op
BenchmarkSelect1000Rows.drizzle                     avgt  400   1371.126 ±   7.977  us/op
BenchmarkSelect1000Rows.mariadb                     avgt  400   1119.807 ±   4.169  us/op
BenchmarkSelect1000Rows.mysql                       avgt  400   1152.424 ±   6.811  us/op
BenchmarkSelect1RowPrepareHit.mariadb               avgt  400    562.189 ±   3.057  us/op
BenchmarkSelect1RowPrepareHit.mysql                 avgt  400    575.179 ±   1.877  us/op
BenchmarkSelect1RowPrepareMiss.mariadb              avgt  400    660.784 ±   6.356  us/op
BenchmarkSelect1RowPrepareMiss.mysql                avgt  400    703.589 ±   8.806  us/op
BenchmarkSelect1RowPrepareText.drizzle              avgt  400    611.575 ±   7.240  us/op
BenchmarkSelect1RowPrepareText.mariadb              avgt  400    589.039 ±   8.861  us/op
BenchmarkSelect1RowPrepareText.mysql                avgt  400    597.861 ±   3.279  us/op
BenchmarkSelect1RowPrepareTextHA.mariadb            avgt  400    580.476 ±   3.102  us/op
BenchmarkSelect1RowPrepareTextHA.mysql              avgt  400    647.161 ±   5.075  us/op

```

##### How to read it :

ms/op means millisecond per operation, us/op microsecond per operation.

```
BenchmarkOneInsertPrepareText.drizzle               avgt  400     85.936 ±   0.655  us/op
BenchmarkOneInsertPrepareText.mariadb               avgt  400     68.515 ±   0.647  us/op
BenchmarkOneInsertPrepareText.mysql                 avgt  400     91.485 ±   1.221  us/op
```


<div style="text-align:center"><img src ="results/insert_one_data.png" /></div>

BenchmarkOneInsert = execute query "INSERT INTO PerfTextQuery (charValue) values ('abcdefghij0123456')"
Using mariadb driver, the average time to insert one data is 68.515 microsecond, and 99.9% of queries executes time are comprised between 67.868 (68.515 - 0.647) and 67.868 microseconds (68.515 + 0.647).
Using MySQL java driver, average execution time is 91.485 millisecond, using Drizzle driver 85.936 milliseconds   

(remember that INSERT queries are executed on BLACKHOLE engine : The BLACKHOLE storage engine accepts data but does not store it and always returns an empty result).



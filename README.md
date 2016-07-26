# JMH performance MariaDB/MySQL driver test

We always talk about performance, but the thing is always "Measure, don’t guess!".
This is a benchmark of [MariaDB java connector](https://github.com/MariaDB/mariadb-connector-j) versus [MySQL java connector](https://github.com/mysql/mysql-connector-j).
MariaDB and MySQL databases are using the same exchange protocol, and driver offer similar functionalities. 

This is a Driver benchmark using [JMH microbenchmark](http://openjdk.java.net/projects/code-tools/jmh/)
developed by the same guys in Oracle who implement the JIT, and is delivered as openJDK tools.

## The tests
Class BenchmarkInit initialize connections using MySQL and MariaDB drivers before tests.

test example org.perf.jdbc.BenchmarkSelect1RowPrepareText : 
```java
public class BenchmarkSelect1RowPrepareText extends BenchmarkSelect1RowPrepareAbstract {

    @Benchmark
    public String mysql(MyState state) throws Throwable {
        return select1RowPrepare(state.mysqlConnectionText);
    }

    @Benchmark
    public String mariadb(MyState state) throws Throwable {
        return select1RowPrepare(state.mariadbConnectionText);
    }

    @Benchmark
    public String drizzle(MyState state) throws Throwable {
        return select1RowPrepare(state.drizzleConnectionText);
    }

}

public abstract class BenchmarkOneInsertPrepareAbstract extends BenchmarkInit {
    private String request = "INSERT INTO blackholeTable (charValue) values (?)";

    public boolean executeOneInsertPrepare(Connection connection, String[] datas) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(request)) {
            preparedStatement.setString(1, datas[0]);
            return preparedStatement.execute();
        }
    }
}
```

The test will execute the prepareStatement "INSERT INTO PerfTextQuery (charValue) values (?)" using a connection issued from java driver MySQL 5.1.39, Drizzle 1.2 or MariaDB 1.5.0.

Tests are launched multiple times using 10 forks , 10 warmup iterations of one second followed by 15 measurement iterations of one second. (one test duration is approximately 4h)


List of tests and their signification :

|Benchmark       | Description |
|-----------|:----------|
| BenchmarkBatch1000Insert* | executing 1000 inserts with random 100 bytes data into a blackHole table (no real insert) = "INSERT INTO blackholeTable (charValue) values (?)"|
|       BenchmarkBatch1000InsertText* | using text protocol (option useServerPrepStmts=false)|
|       BenchmarkBatch1000InsertBatchMulti* | using multi-send (option useBatchMultiSend=true)|
|       BenchmarkBatch1000InsertWithPrepare* | using binary protocol (option useServerPrepStmts=true)|
|       BenchmarkBatch1000InsertRewrite* | using rewrite text protocol (option rewriteBatchedStatements=true|
|       BenchmarkBatch1000InsertMultiQueries* | using text protocol with multi-query (option allowMultiQueries=true)|
| BenchmarkCallableStatementFunction |execute CallableStatement with query "{? = CALL testFunctionCall(?,?,?)}". Function created by "CREATE FUNCTION IF NOT EXISTS testFunctionCall(a float, b bigint, c int) RETURNS INT NO SQL \nBEGIN \nRETURN a; \nEND"|
| BenchmarkCallableStatementWithInParameter |execute CallableStatement with query "{call withResultSet(?)}". Procedure created with "CREATE PROCEDURE IF NOT EXISTS withResultSet(a int) begin select a; end"|
| BenchmarkCallableStatementWithOutParameter |execute CallableStatement with query "{call inOutParam(?)}". Procedure created with "CREATE PROCEDURE IF NOT EXISTS inoutParam(INOUT p1 INT) begin set p1 = p1 + 1; end"|
| BenchmarkOneInsertPrepare* | executing one insert with random 100 bytes data into blackhole table (no real insert) = "INSERT INTO blackholeTable (charValue) values (?)"|
|       BenchmarkOneInsertPrepareText | using text protocol (option useServerPrepStmts=false)|
|       BenchmarkOneInsertPrepareTextHA | using text protocol (option useServerPrepStmts=false) and High availability configuration|
|       BenchmarkOneInsertPrepareHit| using binary protocol with server PREPARE with cache hit (eq : with PREPARE already done)|
|       BenchmarkOneInsertPrepareMiss | using binary protocol with server PREPARE with cache miss (eq : execute PREPARE + EXECUTE + DEALLOCATE PREPARE)|
|       BenchmarkOneInsertPrepareMultiQueries | using text protocol with multi-query (option allowMultiQueries=true)| 
|       BenchmarkOneInsertPrepareRewrite | using rewrite text protocol (option rewriteBatchedStatements=true)|
|       BenchmarkOneInsertPrepareBatchMultiHit | using multi-send (option useBatchMultiSend=true) with server PREPARE with cache hit (eq : with PREPARE already done)|
|       BenchmarkOneInsertPrepareBatchMultiMiss | using multi-send (option useBatchMultiSend=true) with server PREPARE with cache miss (eq : execute PREPARE + EXECUTE + DEALLOCATE PREPARE)|
| BenchmarkSelect1RowPrepare* | execute query "SELECT ?"|
|       BenchmarkSelect1RowPrepareText | using text protocol (option useServerPrepStmts=false)|
|       BenchmarkSelect1RowPrepareTextHA | using text protocol (option useServerPrepStmts=false) and High availability configuration|
|       BenchmarkSelect1RowPrepareHit| using binary protocol with server PREPARE with cache hit (eq : with PREPARE already done)|
|       BenchmarkSelect1RowPrepareMiss| using binary protocol with server PREPARE with cache miss (eq : execute PREPARE + EXECUTE + DEALLOCATE PREPARE)|
|       BenchmarkSelect1RowPrepareMultiQueries | using text protocol with multi-query (option allowMultiQueries=true)|
|       BenchmarkSelect1RowPrepareRewrite| using rewrite text protocol (option rewriteBatchedStatements=true)|
|       BenchmarkSelect1RowPrepareBatchMultiHit| using multi-send (option useBatchMultiSend=true) with server PREPARE with cache hit (eq : with PREPARE already done)|
|       BenchmarkSelect1RowPrepareBatchMultiMiss| using multi-send (option useBatchMultiSend=true) with server PREPARE with cache miss (eq : execute PREPARE + EXECUTE + DEALLOCATE PREPARE)|
| BenchmarkSelect1000Rows | execute query "select * from seq_1_to_1000" : a resultset of 1000 rows, returning integer from 1 to 1000|
| BenchmarkSelect1000BigRows | execute query "select repeat('a', 10000) from seq_1_to_1000" a resultset of 1000 rows, each rows contain 10kb data|
| BenchmarkSelect1000BigRowsFetch | execute query "select repeat('a', 100000) from seq_1_to_1000" a resultset of 1000 rows, each rows contain 100kb data, using setFetchSize(10) to retrieve data by 10 rows||

'* The goal is here to test the driver performance, not database, so INSERT's queries are send to a [BLACKHOLE](https://mariadb.com/kb/en/mariadb/blackhole/) engine (data are not stored). This permit to have more stable results (less than 1% difference, without, data vary with 10% difference).

## How run the tests
* install a MySQL / MariaDB database
trying 
```
max_allowed_packet      = 40M //exchange packet can be up to 40mb
character-set-server    = utf8
collation-server        = utf8_unicode_ci
```
* create database "testj" : create database testj;
* create user : CREATE USER 'perf'@'%' IDENTIFIED BY '!Password0';
* create user perf : GRANT ALL ON *.* TO 'perf'@'%' IDENTIFIED BY '!Password0';
* grant super access : GRANT SUPER ON *.* TO 'perf'@'%';
* install engine [BLACKHOLE](https://mariadb.com/kb/en/mariadb/blackhole/) using command "INSTALL SONAME 'ha_blackhole'" if not installed (SHOW ENGINES permit to check if installed)(This engine don't save data, permitting to execute INSERT queries with stable time result)
* restart database to activate the BLACKHOLE engine
* install a JRE
(* install maven)
(* install git)

```script
git clone https://github.com/rusher/mariadb-mysql-driver.git
mvn clean install
java -Xmx128m -Xms128m -Duser.country=US -Duser.language=en -jar target/benchmarks.jar > result.txt &
```mvn 
-Duser.country=US -Duser.language=en permit to avoid confusion with comma used as decimal separator / thousand separator according to countries
-Xmx64m -Xms64m is to permit to have quick garbage and have more stable results. 

JMH has a lot of options, 2 interesting ones : add a regex to launch only one specific benchmark, and add a garbage profiler to see consume time in GC.
```script
java -Xmx128m -Xms128m -Duser.country=US -Duser.language=en -jar target/benchmarks.jar  ".BenchmarkSelect1000Rows*" -prof gc > result.txt &
```
·gc.alloc.rate.norm


## Read results 

Results are in file "result.txt".
Complete results are the end of the file. 

Execution on a droplet on digitalocean.com using this parameters:
- Ubuntu 16.04 64bits
- 1GB memory
- 1 CPU

using MariaDb 10.1.16 (<a href='results/result_mariadb_server.txt'>complete results</a>)
using mysql 5.7.13 (<a href='results/result_mysql_server.txt'>complete results</a>)

Extract of mariadb server results :

```
# Run complete. Total time: 04:11:18

Benchmark                                           Mode  Cnt     Score    Error  Units
BenchmarkBatch1000InsertBulk.mariadb                avgt  150    44.167 ±  1.473  ms/op
BenchmarkBatch1000InsertMultiQueries.mariadb        avgt  150    14.241 ±  0.626  ms/op
BenchmarkBatch1000InsertPrepare.mariadb             avgt  150    48.829 ±  1.871  ms/op
BenchmarkBatch1000InsertPrepare.mysql               avgt  150    60.437 ±  2.037  ms/op
BenchmarkBatch1000InsertRewrite.mariadb             avgt  150     3.831 ±  0.206  ms/op
BenchmarkBatch1000InsertRewrite.mysql               avgt  150     5.757 ±  0.170  ms/op
BenchmarkBatch1000InsertText.drizzle                avgt  150    87.510 ±  2.761  ms/op
BenchmarkBatch1000InsertText.mariadb                avgt  150    68.106 ±  3.007  ms/op
BenchmarkBatch1000InsertText.mysql                  avgt  150    77.409 ±  2.734  ms/op
BenchmarkCallableStatementFunction.mariadb          avgt  150   101.627 ±  3.415  us/op
BenchmarkCallableStatementFunction.mysql            avgt  150  1024.080 ± 52.914  us/op
BenchmarkCallableStatementWithInParameter.mariadb   avgt  150   118.213 ±  7.053  us/op
BenchmarkCallableStatementWithInParameter.mysql     avgt  150   727.023 ± 48.940  us/op
BenchmarkCallableStatementWithOutParameter.mariadb  avgt  150    89.469 ±  6.836  us/op
BenchmarkCallableStatementWithOutParameter.mysql    avgt  150   881.286 ± 52.975  us/op
BenchmarkOneInsertPrepareBulkHit.mariadb            avgt  150    52.331 ±  1.681  us/op
BenchmarkOneInsertPrepareBulkMiss.mariadb           avgt  150   124.031 ±  4.190  us/op
BenchmarkOneInsertPrepareHit.mariadb                avgt  150    56.891 ±  2.121  us/op
BenchmarkOneInsertPrepareHit.mysql                  avgt  150    63.271 ±  2.314  us/op
BenchmarkOneInsertPrepareMiss.mariadb               avgt  150   133.206 ±  7.387  us/op
BenchmarkOneInsertPrepareMiss.mysql                 avgt  150   158.309 ±  4.746  us/op
BenchmarkOneInsertPrepareMultiQueries.mariadb       avgt  150    66.489 ±  2.528  us/op
BenchmarkOneInsertPrepareMultiQueries.mysql         avgt  150    98.820 ±  3.847  us/op
BenchmarkOneInsertPrepareRewrite.mariadb            avgt  150    67.623 ±  2.689  us/op
BenchmarkOneInsertPrepareRewrite.mysql              avgt  150   121.382 ±  5.465  us/op
BenchmarkOneInsertPrepareText.drizzle               avgt  150    91.166 ±  4.214  us/op
BenchmarkOneInsertPrepareText.mariadb               avgt  150    65.736 ±  2.475  us/op
BenchmarkOneInsertPrepareText.mysql                 avgt  150    98.259 ±  4.609  us/op
BenchmarkOneInsertPrepareTextHA.mariadb             avgt  150    68.968 ±  2.369  us/op
BenchmarkOneInsertPrepareTextHA.mysql               avgt  150   161.468 ±  7.785  us/op
BenchmarkSelect1000BigRows.drizzle                  avgt  150   101.120 ±  3.897  ms/op
BenchmarkSelect1000BigRows.mariadb                  avgt  150    93.299 ±  3.524  ms/op
BenchmarkSelect1000BigRows.mysql                    avgt  150   102.366 ±  3.495  ms/op
BenchmarkSelect1000BigRowsFetch.drizzle             avgt  150  1035.977 ± 34.501  ms/op
BenchmarkSelect1000BigRowsFetch.mariadb             avgt  150   818.183 ± 34.323  ms/op
BenchmarkSelect1000BigRowsFetch.mysql               avgt  150  1176.984 ± 29.539  ms/op
BenchmarkSelect1000Rows.drizzle                     avgt  150   773.540 ± 28.538  us/op
BenchmarkSelect1000Rows.mariadb                     avgt  150   481.937 ± 14.620  us/op
BenchmarkSelect1000Rows.mysql                       avgt  150   563.007 ± 18.444  us/op
BenchmarkSelect1RowPrepareBulkHit.mariadb           avgt  150    45.371 ±  1.677  us/op
BenchmarkSelect1RowPrepareBulkMiss.mariadb          avgt  150    96.243 ±  3.555  us/op
BenchmarkSelect1RowPrepareHit.mariadb               avgt  150    44.428 ±  1.813  us/op
BenchmarkSelect1RowPrepareHit.mysql                 avgt  150    65.181 ±  2.363  us/op
BenchmarkSelect1RowPrepareMiss.mariadb              avgt  150   102.855 ±  7.528  us/op
BenchmarkSelect1RowPrepareMiss.mysql                avgt  150   134.960 ±  5.131  us/op
BenchmarkSelect1RowPrepareMultiQueries.mariadb      avgt  150    53.393 ±  2.413  us/op
BenchmarkSelect1RowPrepareMultiQueries.mysql        avgt  150    77.497 ±  2.989  us/op
BenchmarkSelect1RowPrepareRewrite.mariadb           avgt  150    50.368 ±  1.625  us/op
BenchmarkSelect1RowPrepareRewrite.mysql             avgt  150    80.276 ±  3.289  us/op
BenchmarkSelect1RowPrepareText.drizzle              avgt  150    81.795 ±  2.952  us/op
BenchmarkSelect1RowPrepareText.mariadb              avgt  150    51.106 ±  2.729  us/op
BenchmarkSelect1RowPrepareText.mysql                avgt  150    77.286 ±  2.437  us/op
BenchmarkSelect1RowPrepareTextHA.mariadb            avgt  150    53.130 ±  1.561  us/op
BenchmarkSelect1RowPrepareTextHA.mysql              avgt  150   135.638 ±  6.000  us/op
```

##### How to read it :

ms/op means millisecond per operation, us/op microsecond per operation.

```
Benchmark                                           Mode  Cnt    Score     Error  Units
BenchmarkSelect1RowPrepareText.drizzle              avgt  150    81.795 ±  2.952  us/op
BenchmarkSelect1RowPrepareText.mariadb              avgt  150    51.106 ±  2.729  us/op
BenchmarkSelect1RowPrepareText.mysql                avgt  150    77.286 ±  2.437  us/op
```


<div style="text-align:center"><img src ="results/select_one_data.png" /></div>

BenchmarkOneInsert = execute query "SELECT ?"
Using mariadb driver, the average time to insert one data is 51.106 microsecond, and 99.9% of queries executes time are comprised between 48.377 (51.106 - 2.729) and 53.835 microseconds (51.106 + 2.729).
Using MySQL java driver, average execution time is 77.286 millisecond, using Drizzle driver 81.795 milliseconds   




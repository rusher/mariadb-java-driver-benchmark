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
public class BenchmarkSelect1RowPrepareText extends BenchmarkSelect1RowPrepareAbstract {

    @Benchmark
    public String mysql(MyState state) throws Throwable {
        return select1RowPrepare(state.mysqlConnectionText, state);
    }

    @Benchmark
    public String mariadb(MyState state) throws Throwable {
        return select1RowPrepare(state.mariadbConnectionText, state);
    }

    @Benchmark
    public String drizzle(MyState state) throws Throwable {
        return select1RowPrepare(state.drizzleConnectionText, state);
    }

}

public abstract class BenchmarkSelect1RowPrepareAbstract extends BenchmarkInit {
    private String request = "SELECT ?";

    public String select1RowPrepare(Connection connection, MyState state) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(request)) {
            preparedStatement.setString(1, state.insertData[state.counter++]);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                rs.next();
                return rs.getString(1);
            }
        }
    }
}
```

The test will execute the prepareStatement "INSERT INTO PerfTextQuery (charValue) values (?)" using a connection issued from java driver MySQL 5.1.39, Drizzle 1.4 or MariaDB 1.5.3.

Tests are launched multiple times using 10 forks , 10 warmup iterations of one second followed by 15 measurement iterations of one second. (one test duration is approximately 4h)


List of tests and their signification :

|Benchmark       | Description |
|-----------|:----------|
| PrepareStatementBatch100Insert* | executing 100 inserts with random 100 bytes data into a blackHole table (no real insert) = "INSERT INTO blackholeTable (charValue) values (?)"|
|       PrepareStatementBatch100InsertText | using text protocol (option useServerPrepStmts=false)|
|       PrepareStatementBatch100InsertPrepareHit | using binary protocol (option useServerPrepStmts=true) with server PREPARE with cache hit (eq : with PREPARE already done)|
|       PrepareStatementBatch100InsertRewrite | using rewrite text protocol (option rewriteBatchedStatements=true|
| BenchmarkCallableStatementFunction |execute CallableStatement with query "{? = CALL testFunctionCall(?,?,?)}". Function created by "CREATE FUNCTION IF NOT EXISTS testFunctionCall(a float, b bigint, c int) RETURNS INT NO SQL \nBEGIN \nRETURN a; \nEND"|
| BenchmarkCallableStatementWithInParameter |execute CallableStatement with query "{call withResultSet(?)}". Procedure created with "CREATE PROCEDURE IF NOT EXISTS withResultSet(a int) begin select a; end"|
| BenchmarkCallableStatementWithOutParameter |execute CallableStatement with query "{call inOutParam(?)}". Procedure created with "CREATE PROCEDURE IF NOT EXISTS inoutParam(INOUT p1 INT) begin set p1 = p1 + 1; end"|
| BenchmarkOneInsertPrepare* | executing one insert with random 100 bytes data into blackhole table (no real insert) = "INSERT INTO blackholeTable (charValue) values (?)"|
|       BenchmarkOneInsertPrepareText | using text protocol (option useServerPrepStmts=false)|
|       BenchmarkOneInsertPrepareTextHA | using text protocol (option useServerPrepStmts=false) and High availability configuration|
|       BenchmarkOneInsertPrepareHit| using binary protocol with server PREPARE with cache hit (eq : with PREPARE already done)|
|       BenchmarkOneInsertPrepareMiss | using binary protocol with server PREPARE with cache miss (eq : execute PREPARE + EXECUTE + DEALLOCATE PREPARE)|
|       BenchmarkOneInsertPrepareRewrite | using rewrite text protocol (option rewriteBatchedStatements=true)|
| BenchmarkSelect1RowPrepare* | execute query "SELECT ?"|
|       BenchmarkSelect1RowPrepareText | using text protocol (option useServerPrepStmts=false)|
|       BenchmarkSelect1RowPrepareTextHA | using text protocol (option useServerPrepStmts=false) and High availability configuration|
|       BenchmarkSelect1RowPrepareHit| using binary protocol with server PREPARE with cache hit (eq : with PREPARE already done)|
|       BenchmarkSelect1RowPrepareMiss| using binary protocol with server PREPARE with cache miss (eq : execute PREPARE + EXECUTE + DEALLOCATE PREPARE)|
|       BenchmarkSelect1RowPrepareRewrite| using rewrite text protocol (option rewriteBatchedStatements=true)|
| BenchmarkSelect1000Rows | execute query "select * from seq_1_to_1000" : a resultset of 1000 rows, returning integer from 1 to 1000|
| BenchmarkSelect1000BigRows | execute query "select repeat('a', 10000) from seq_1_to_1000" a resultset of 1000 rows, each rows contain 10kb data|
| BenchmarkSelect1000BigRowsFetch | execute query "select repeat('a', 100000) from seq_1_to_1000" a resultset of 1000 rows, each rows contain 100kb data, using setFetchSize(10) to retrieve data by 10 rows||

'* The goal is here to test the driver performance, not database, so INSERT's queries are send to a [BLACKHOLE](https://mariadb.com/kb/en/mariadb/blackhole/) engine (data are not stored). This permit to have more stable results (less than 1% difference, without, data vary with 10% difference).

## How run the tests
* install a MySQL / MariaDB database
Add the following configuration : 
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
```
 
-Duser.country=US -Duser.language=en permit to avoid confusion with comma used as decimal separator / thousand separator according to countries
-Xmx128m -Xms128m is to permit to have quick garbage and have more stable results. 

JMH has a lot of options, 2 interesting ones : add a regex to launch only one specific benchmark, and add a garbage profiler to see consume time in GC.
```script
java -Xmx128m -Xms128m -Duser.country=US -Duser.language=en -jar target/benchmarks.jar  ".BenchmarkSelect1000Rows*" -prof gc > result.txt &
```

## Read results 

Results are in file "result.txt".
Complete results are the end of the file. 

Execution on a droplet on digitalocean.com using this parameters:
- Ubuntu 16.04 64bits
- 512Mb memory
- 1 CPU

using MariaDb 10.1.16 (<a href='results/result_mariadb-10.1_server_local.txt'>local results</a> and <a href='results/result_mariadb-10.1_server_distant.txt'>distant results</a>)
using mysql 5.7.13 (<a href='results/result_mysql-5.7_server_local.txt'>local results</a>)

Extract of mariadb server results with mariadb-10.1 local server :
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
BenchmarkSelect1RowPrepareText.drizzle              avgt  200    78.672 ±  2.971  µs/op
BenchmarkSelect1RowPrepareText.mariadb              avgt  200    62.715 ±  2.402  µs/op
BenchmarkSelect1RowPrepareText.mysql                avgt  200    88.670 ±  3.505  µs/op
```


<div style="text-align:center"><img src ="results/select_one_data.png" /></div>

BenchmarkOneInsert = Using same local database, time for query \n\"SELECT CAST(? as char character set utf8)\"
Using mariadb driver, the average time to insert one data is 62.715 microsecond, and 99.9% of queries executes time are comprised between 59.773 (62.715 - 2.402) and 65.117 microseconds (62.715 + 2.402).
Using MySQL java driver, average execution time is 88.670 millisecond, using Drizzle driver 78.672 milliseconds   




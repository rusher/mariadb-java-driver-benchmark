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

The test will execute the prepareStatement "INSERT INTO PerfTextQuery (charValue) values (?)" using a connection issued from java driver MySQL 5.1.39, Drizzle 1.2 or MariaDB 1.5.0.

Tests are launched multiple times using 10 forks , 10 warmup iterations of one second followed by 15 measurement iterations of one second. (one test duration is approximately 4h)


List of tests and their signification :

|Benchmark       | Description |
|-----------|:----------|
| BenchmarkBatch1000Insert* | executing 1000 inserts with random 100 bytes data into a blackHole table (no real insert) = "INSERT INTO blackholeTable (charValue) values (?)"|
|       BenchmarkBatch1000InsertText | using text protocol (option useServerPrepStmts=false)|
|       BenchmarkBatch1000InsertWithPrepareHit | using binary protocol (option useServerPrepStmts=true) with server PREPARE with cache hit (eq : with PREPARE already done)|
|       BenchmarkBatch1000InsertRewrite | using rewrite text protocol (option rewriteBatchedStatements=true|
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
- 1GB memory
- 1 CPU

using MariaDb 10.1.16 (<a href='results/result_mariadb-10.1_server_local.txt'>local results</a>)
using MariaDb 10.2.2 (<a href='results/result_mariadb-10.2_server_local.txt'>local</a> and <a href='results/result_mariadb-10.2_server_distant.txt'>local</a> results)
using mysql 5.7.13 (<a href='results/result_mysql-5.7_server_local.txt'>complete results</a>)

Extract of mariadb server results with mariadb-10.2 local server :
```
# Run complete. Total time: 03:35:34

Benchmark                                                 Mode  Cnt     Score    Error  Units
BenchmarkBatch1000InsertPrepareHit.mariadb                avgt  150    49.901 ±  2.531  ms/op
BenchmarkBatch1000InsertPrepareHit.mysql                  avgt  150    65.853 ±  2.941  ms/op
BenchmarkBatch1000InsertRewrite.mariadb                   avgt  150     3.880 ±  0.238  ms/op
BenchmarkBatch1000InsertRewrite.mysql                     avgt  150     6.325 ±  0.382  ms/op
BenchmarkBatch1000InsertText.drizzle                      avgt  150    88.671 ±  4.243  ms/op
BenchmarkBatch1000InsertText.mariadb                      avgt  150    56.638 ±  2.277  ms/op
BenchmarkBatch1000InsertText.mysql                        avgt  150    74.661 ±  2.625  ms/op
BenchmarkCallableStatementFunction.mariadb                avgt  150   102.519 ±  5.771  us/op
BenchmarkCallableStatementFunction.mysql                  avgt  150   999.149 ± 60.169  us/op
BenchmarkCallableStatementWithInParameter.mariadb         avgt  150   103.096 ±  6.101  us/op
BenchmarkCallableStatementWithInParameter.mysql           avgt  150   665.355 ± 34.834  us/op
BenchmarkCallableStatementWithOutParameter.mariadb        avgt  150    90.599 ±  5.683  us/op
BenchmarkCallableStatementWithOutParameter.mysql          avgt  150   887.335 ± 50.604  us/op
BenchmarkOneInsertPrepareHit.mariadb                      avgt  150    52.535 ±  1.650  us/op
BenchmarkOneInsertPrepareHit.mysql                        avgt  150    62.971 ±  2.195  us/op
BenchmarkOneInsertPrepareMiss.mariadb                     avgt  150   124.092 ±  4.453  us/op
BenchmarkOneInsertPrepareMiss.mariadbWithout102capacity   avgt  150   128.787 ±  7.090  us/op
BenchmarkOneInsertPrepareMiss.mysql                       avgt  150   159.506 ±  5.520  us/op
BenchmarkOneInsertPrepareRewrite.mariadb                  avgt  150    64.319 ±  2.642  us/op
BenchmarkOneInsertPrepareRewrite.mysql                    avgt  150   104.974 ±  3.403  us/op
BenchmarkOneInsertPrepareText.drizzle                     avgt  150    86.507 ±  2.931  us/op
BenchmarkOneInsertPrepareText.mariadb                     avgt  150    63.145 ±  2.217  us/op
BenchmarkOneInsertPrepareText.mysql                       avgt  150    90.756 ±  2.545  us/op
BenchmarkOneInsertPrepareTextHA.mariadb                   avgt  150    65.020 ±  2.053  us/op
BenchmarkOneInsertPrepareTextHA.mysql                     avgt  150   155.947 ±  5.507  us/op
BenchmarkSelect1000BigRows.drizzle                        avgt  150    97.449 ±  4.946  ms/op
BenchmarkSelect1000BigRows.mariadb                        avgt  150    87.203 ±  3.109  ms/op
BenchmarkSelect1000BigRows.mysql                          avgt  150    98.354 ±  2.872  ms/op
BenchmarkSelect1000BigRowsFetch.drizzle                   avgt  150  1051.069 ± 37.553  ms/op
BenchmarkSelect1000BigRowsFetch.mariadb                   avgt  150   776.873 ± 22.925  ms/op
BenchmarkSelect1000BigRowsFetch.mysql                     avgt  150  1163.960 ± 32.565  ms/op
BenchmarkSelect1000Rows.drizzle                           avgt  150   709.819 ± 19.759  us/op
BenchmarkSelect1000Rows.mariadb                           avgt  150   479.684 ± 18.657  us/op
BenchmarkSelect1000Rows.mysql                             avgt  150   551.705 ± 20.227  us/op
BenchmarkSelect1RowPrepareHit.mariadb                     avgt  150    43.680 ±  1.860  us/op
BenchmarkSelect1RowPrepareHit.mysql                       avgt  150    62.668 ±  2.356  us/op
BenchmarkSelect1RowPrepareMiss.mariadb                    avgt  150    97.979 ±  4.272  us/op
BenchmarkSelect1RowPrepareMiss.mariadbWithout102capacity  avgt  150   102.632 ±  5.701  us/op
BenchmarkSelect1RowPrepareMiss.mysql                      avgt  150   142.446 ±  8.919  us/op
BenchmarkSelect1RowPrepareRewrite.mariadb                 avgt  150    49.455 ±  2.084  us/op
BenchmarkSelect1RowPrepareRewrite.mysql                   avgt  150    77.661 ±  2.842  us/op
BenchmarkSelect1RowPrepareText.drizzle                    avgt  150    78.311 ±  2.631  us/op
BenchmarkSelect1RowPrepareText.mariadb                    avgt  150    49.554 ±  1.815  us/op
BenchmarkSelect1RowPrepareText.mysql                      avgt  150    79.760 ±  2.848  us/op
BenchmarkSelect1RowPrepareTextHA.mariadb                  avgt  150    53.579 ±  2.439  us/op
BenchmarkSelect1RowPrepareTextHA.mysql                    avgt  150   131.405 ±  8.395  us/op
```

##### How to read it :

ms/op means millisecond per operation, us/op microsecond per operation.

```
Benchmark                                                 Mode  Cnt     Score    Error  Units
BenchmarkSelect1RowPrepareText.drizzle                    avgt  150    78.311 ±  2.631  us/op
BenchmarkSelect1RowPrepareText.mariadb                    avgt  150    49.554 ±  1.815  us/op
BenchmarkSelect1RowPrepareText.mysql                      avgt  150    79.760 ±  2.848  us/op
```


<div style="text-align:center"><img src ="results/select_one_data.png" /></div>

BenchmarkOneInsert = execute query "SELECT ?"
Using mariadb driver, the average time to insert one data is 49.554 microsecond, and 99.9% of queries executes time are comprised between 47.739 (49.554 - 1.815) and 51.369 microseconds (49.554 + 1.815).
Using MySQL java driver, average execution time is 79.760 millisecond, using Drizzle driver 78.311 milliseconds   




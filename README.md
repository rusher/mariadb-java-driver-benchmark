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

public abstract class BenchmarkSelect1RowPrepareAbstract extends BenchmarkInit {
    private String request = "SELECT * FROM PerfReadQuery where charValue = ?";
    private String var = "abc0";

    public String select1RowPrepare(Connection connection) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(request)) {
            preparedStatement.setString(1, var);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                rs.next();
                return rs.getString(1);
            }
        }
    }
}
```

The test will execute the prepareStatement "INSERT INTO PerfTextQuery (charValue) values (?)" using a connection issued from java driver MySQL 5.1.39, Drizzle 1.2 or MariaDB 1.5.0.

Tests are launched multiple times using 10 forks , 10 warmup iterations of one second followed by 15 measurement iterations of one second. (one test duration is approximately 45 minutes)


List of tests and their signification :

|Benchmark       | Description |
|-----------|:----------|
| BenchmarkSelect1RowPrepareText | execute query "SELECT ?"|
| BenchmarkSelect1RowPrepareTextHA |same as BenchmarkSelect1RowPrepareText but using High availability configuration|
| BenchmarkSelect1RowPrepareHit | same as BenchmarkSelect1RowPrepareText but using server PREPARE with cache hit (eq : PREPARE already done)|
| BenchmarkSelect1RowPrepareMiss | same as BenchmarkSelect1RowPrepareText but using server PREPARE with cache miss (eq : execute PREPARE + DEALLOCATE PREPARE)|
| BenchmarkSelect1000Rows |execute query "select * from seq_1_to_1000" : a resultset of 1000 rows, returning integer from 1 to 1000|
| BenchmarkSelect1000BigRows |execute query "select repeat('a', 10000) from seq_1_to_1000" a resultset of 1000 rows, each rows contain 10kb data)|
| BenchmarkOneInsertPrepareText* | execute query "INSERT INTO blackholeTable (charValue) values (?)"|
| BenchmarkOneInsertPrepareTextHA* |same as BenchmarkOneInsertPrepareText but using High availability configuration|
| BenchmarkOneInsertPrepareHit* | same as BenchmarkOneInsertPrepareText but using server PREPARE with cache hit (eq : PREPARE already done)|
| BenchmarkOneInsertPrepareMiss* | same as BenchmarkOneInsertPrepareText but using server PREPARE with cache miss (eq : execute PREPARE + DEALLOCATE PREPARE)|
| BenchmarkBatch1000InsertText* |executing 1000 inserts with random 20bytes data "INSERT INTO blackholeTable (charValue) values (?)" (option useServerPrepStmts=false)|
| BenchmarkBatch1000InsertWithPrepare* |same as BenchmarkBatch1000InsertText, using server "prepare" already in cache. (option useServerPrepStmts=true)|
| BenchmarkBatch1000InsertRewrite* |same as BenchmarkBatch1000InsertText, using option rewriteBatchedStatements=true|
| BenchmarkCallableStatementFunction |execute CallableStatement with query "{? = CALL testFunctionCall(?,?,?)}". Function created by "CREATE FUNCTION IF NOT EXISTS testFunctionCall(a float, b bigint, c int) RETURNS INT NO SQL \nBEGIN \nRETURN a; \nEND"|
| BenchmarkCallableStatementWithInParameter |execute CallableStatement with query "{call withResultSet(?)}". Procedure created with "CREATE PROCEDURE IF NOT EXISTS withResultSet(a int) begin select a; end"|
| BenchmarkCallableStatementWithOutParameter |execute CallableStatement with query "{call inOutParam(?)}". Procedure created with "CREATE PROCEDURE IF NOT EXISTS inoutParam(INOUT p1 INT) begin set p1 = p1 + 1; end"|

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
* create user perf : GRANT ALL ON *.* TO 'perf'@'localhost' IDENTIFIED BY '!Password0';
* grant super access : GRANT SUPER ON *.* TO 'perf'@'%';
* install engine [BLACKHOLE](https://mariadb.com/kb/en/mariadb/blackhole/) using command "INSTALL SONAME 'ha_blackhole'" (This engine don't save data, permitting to execute INSERT queries with stable time result)
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
- CentOS 7.2 64bits
- 1GB memory
- 1 CPU

using MariaDb 10.1.13 ( with default configuration file) (<a href='results/result_mariadb_server.txt'>complete results</a>)
using mysql 5.7.12 ( with default configuration file) (<a href='results/result_mysql_server.txt'>complete results</a>)

Extract of mariadb server results :

```

# Run complete. Total time: 06:56:33

Benchmark                                           Mode  Cnt       Score     Error  Units
BenchmarkBatch1000InsertPrepare.mariadb             avgt  500      59.020 ±   2.375  ms/op
BenchmarkBatch1000InsertPrepare.mysql               avgt  500      71.707 ±   3.079  ms/op
BenchmarkBatch1000InsertRewrite.mariadb             avgt  500       2.263 ±   0.063  ms/op
BenchmarkBatch1000InsertRewrite.mysql               avgt  500       1.897 ±   0.045  ms/op
BenchmarkBatch1000InsertText.drizzle                avgt  500      94.771 ±   3.199  ms/op
BenchmarkBatch1000InsertText.mariadb                avgt  500      78.093 ±   2.745  ms/op
BenchmarkBatch1000InsertText.mysql                  avgt  500      85.060 ±   2.792  ms/op
BenchmarkCallableStatementFunction.mariadb          avgt  500     112.662 ±   2.579  us/op
BenchmarkCallableStatementFunction.mysql            avgt  500    1657.960 ±  46.456  us/op
BenchmarkCallableStatementWithInParameter.mariadb   avgt  500      89.256 ±   2.252  us/op
BenchmarkCallableStatementWithInParameter.mysql     avgt  500    1575.954 ±  60.453  us/op
BenchmarkCallableStatementWithOutParameter.mariadb  avgt  500      71.376 ±   1.348  us/op
BenchmarkCallableStatementWithOutParameter.mysql    avgt  500    1701.757 ±  50.215  us/op
BenchmarkOneInsertPrepareHit.mariadb                avgt  500      62.674 ±   1.741  us/op
BenchmarkOneInsertPrepareHit.mysql                  avgt  500      66.582 ±   1.400  us/op
BenchmarkOneInsertPrepareMiss.mariadb               avgt  500     135.067 ±   3.274  us/op
BenchmarkOneInsertPrepareMiss.mysql                 avgt  500     165.441 ±   3.592  us/op
BenchmarkOneInsertPrepareText.drizzle               avgt  500      90.572 ±   1.687  us/op
BenchmarkOneInsertPrepareText.mariadb               avgt  500      72.368 ±   1.461  us/op
BenchmarkOneInsertPrepareText.mysql                 avgt  500      93.846 ±   1.567  us/op
BenchmarkOneInsertPrepareTextHA.mariadb             avgt  500      76.859 ±   1.636  us/op
BenchmarkOneInsertPrepareTextHA.mysql               avgt  500     152.755 ±   2.523  us/op
BenchmarkSelect1000BigRows.drizzle                  avgt  500   93563.723 ± 676.147  us/op
BenchmarkSelect1000BigRows.mariadb                  avgt  500   88298.679 ± 656.603  us/op
BenchmarkSelect1000BigRows.mysql                    avgt  500  105989.608 ± 816.302  us/op
BenchmarkSelect1000Rows.drizzle                     avgt  500     751.088 ±   5.571  us/op
BenchmarkSelect1000Rows.mariadb                     avgt  500     508.405 ±   5.239  us/op
BenchmarkSelect1000Rows.mysql                       avgt  500     558.983 ±   5.678  us/op
BenchmarkSelect1RowPrepareHit.mariadb               avgt  500      45.160 ±   0.584  us/op
BenchmarkSelect1RowPrepareHit.mysql                 avgt  500      63.283 ±   0.934  us/op
BenchmarkSelect1RowPrepareMiss.mariadb              avgt  500     102.548 ±   1.216  us/op
BenchmarkSelect1RowPrepareMiss.mysql                avgt  500     133.809 ±   1.323  us/op
BenchmarkSelect1RowPrepareText.drizzle              avgt  500      71.834 ±   0.750  us/op
BenchmarkSelect1RowPrepareText.mariadb              avgt  500      51.771 ±   0.813  us/op
BenchmarkSelect1RowPrepareText.mysql                avgt  500      69.498 ±   0.853  us/op
BenchmarkSelect1RowPrepareTextHA.mariadb            avgt  500      53.430 ±   0.681  us/op
BenchmarkSelect1RowPrepareTextHA.mysql              avgt  500     122.346 ±   1.135  us/op
```

##### How to read it :

ms/op means millisecond per operation, us/op microsecond per operation.

```
Benchmark                                           Mode  Cnt      Score     Error  Units
BenchmarkSelect1RowPrepareText.drizzle              avgt  500      71.834&#177;   0.750  us/op
BenchmarkSelect1RowPrepareText.mariadb              avgt  500      51.771&#177;   0.813  us/op
BenchmarkSelect1RowPrepareText.mysql                avgt  500      69.498&#177;   0.853  us/op
```


<div style="text-align:center"><img src ="results/select_one_data.png" /></div>

BenchmarkOneInsert = execute query "SELECT ?"
Using mariadb driver, the average time to insert one data is 51.771 microsecond, and 99.9% of queries executes time are comprised between 50.958 (51.771 - 0.813) and 52.584 microseconds (51.771 + 0.813).
Using MySQL java driver, average execution time is 69.498 millisecond, using Drizzle driver 71.834 milliseconds   




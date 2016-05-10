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

The test will execute the prepareStatement "INSERT INTO PerfTextQuery (charValue) values (?)" using a connection issued from MySQL or MariaDB driver.

Tests are launched multiple times using 10 forks , 15 warmup iterations of one second followed by 50 measurement iterations of one second. (one test duration is approximately 45 minutes)


List of tests and their signification :

|Benchmark       |description |
|-----------|:----------|
| BenchmarkOneInsert* | execute query "INSERT INTO PerfTextQuery (charValue) values ('abcdefghij0123456')"|
| BenchmarkOneInsertFailover*|same as BenchmarkOneInsert but using failover configuration|
| BenchmarkPrepareStatementOneInsert*|same as BenchmarkOneInsert but using "prepare" |
| BenchmarkPrepareStatementOneInsertFailover*|same as BenchmarkOneInsert but using "prepare" and failover configuration |
| BenchmarkSelect1Row|execute query "SELECT * FROM PerfReadQuery where id = 0";|
| BenchmarkSelect1RowFailover|same than BenchmarkSelect1Row but using failover configuration|
| BenchmarkSelect1RowPreparedNoCache|same than BenchmarkSelect1Row but using "prepare" without caching|
| BenchmarkSelect1RowPreparedWithCache|same than BenchmarkSelect1Row but using "prepare"|
| BenchmarkSelect1000Rows|execute query "SELECT * FROM PerfReadQuery" (table with 1000 rows, each rows contain < 10 bytes) )|
| BenchmarkSelect1000BigRows|execute query "SELECT * FROM PerfReadQueryBig" (table with 1000 rows, each rows contain 10kb)|
| BenchmarkBatch1000InsertWithPrepare*|executing 1000 inserts using prepareStatement with "prepare" on server. (option useServerPrepStmts=true)|
| BenchmarkBatch1000InsertWithoutPrepare*|executing 1000 inserts. (option useServerPrepStmts=false)|
| BenchmarkBatch1000InsertRewrite*|executing 1000 inserts. (option rewriteBatchedStatements=true)|
| BenchmarkCallableStatementFunction|execute CallableStatement with query "{? = CALL testFunctionCall(?,?,?)}". Function created by "CREATE FUNCTION IF NOT EXISTS testFunctionCall(a float, b bigint, c int) RETURNS INT NO SQL \nBEGIN \nRETURN a; \nEND"|
| BenchmarkCallableStatementWithInParameter|execute CallableStatement with query "{call withResultSet(?)}". Procedure created with "CREATE PROCEDURE IF NOT EXISTS withResultSet(a int) begin select a; end"|
| BenchmarkCallableStatementWithOutParameter|execute CallableStatement with query "{call inOutParam(?)}". Procedure created with "CREATE PROCEDURE IF NOT EXISTS inoutParam(INOUT p1 INT) begin set p1 = p1 + 1; end"|

'* The goal is here to test the driver performance, not database, so INSERT's queries are send to a [BLACKHOLE](https://mariadb.com/kb/en/mariadb/blackhole/) engine (data are not stored). This permit to have more stable results.



## How run the tests
* install a MySQL / MariaDB database with user root without password
* create database "testj"
    * create user perf : GRANT ALL ON testj.* TO 'perf'@'localhost' IDENTIFIED BY '!Password0';
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

# Run complete. Total time: 06:00:58

Benchmark                                           Mode  Cnt      Score     Error  Units
BenchmarkBatch1000InsertRewrite.mariadb             avgt  400      1.654 ±   0.008  ms/op
BenchmarkBatch1000InsertRewrite.mysql               avgt  400      1.715 ±   0.031  ms/op
BenchmarkBatch1000InsertWithPrepare.mariadb         avgt  400     50.514 ±   0.749  ms/op
BenchmarkBatch1000InsertWithPrepare.mysql           avgt  400     61.369 ±   1.469  ms/op
BenchmarkBatch1000InsertWithoutPrepare.drizzle      avgt  400     87.059 ±   1.405  ms/op
BenchmarkBatch1000InsertWithoutPrepare.mariadb      avgt  400     64.271 ±   0.686  ms/op
BenchmarkBatch1000InsertWithoutPrepare.mysql        avgt  400     71.667 ±   0.809  ms/op
BenchmarkCallableStatementFunction.mariadb          avgt  400    110.489 ±   1.543  us/op
BenchmarkCallableStatementFunction.mysql            avgt  400   1693.671 ±  53.801  us/op
BenchmarkCallableStatementWithInParameter.mariadb   avgt  400     81.140 ±   0.915  us/op
BenchmarkCallableStatementWithInParameter.mysql     avgt  400   1519.251 ±  46.081  us/op
BenchmarkCallableStatementWithOutParameter.mariadb  avgt  400     72.206 ±   1.171  us/op
BenchmarkCallableStatementWithOutParameter.mysql    avgt  400   1747.846 ±  62.787  us/op
BenchmarkOneInsert.drizzle                          avgt  400     81.181 ±   0.514  us/op
BenchmarkOneInsert.mariadb                          avgt  400     60.899 ±   0.321  us/op
BenchmarkOneInsert.mysql                            avgt  400     77.789 ±   1.272  us/op
BenchmarkOneInsertFailover.mariadb                  avgt  400     65.548 ±   0.613  us/op
BenchmarkOneInsertFailover.mysql                    avgt  400    108.710 ±   1.093  us/op
BenchmarkPrepareStatementOneInsert.mariadb          avgt  400     55.049 ±   0.786  us/op
BenchmarkPrepareStatementOneInsert.mysql            avgt  400     61.460 ±   0.723  us/op
BenchmarkPrepareStatementOneInsertFailover.mariadb  avgt  400     57.271 ±   0.317  us/op
BenchmarkPrepareStatementOneInsertFailover.mysql    avgt  400     78.605 ±   0.600  us/op
BenchmarkSelect1000BigRows.drizzle                  avgt  400  44563.716 ± 403.623  us/op
BenchmarkSelect1000BigRows.mariadb                  avgt  400  38829.509 ± 157.803  us/op
BenchmarkSelect1000BigRows.mysql                    avgt  400  50259.241 ± 208.746  us/op
BenchmarkSelect1000Rows.drizzle                     avgt  400   1367.489 ±   6.145  us/op
BenchmarkSelect1000Rows.mariadb                     avgt  400   1125.747 ±   7.666  us/op
BenchmarkSelect1000Rows.mysql                       avgt  400   1145.878 ±   7.388  us/op
BenchmarkSelect1Row.drizzle                         avgt  400    600.820 ±   2.625  us/op
BenchmarkSelect1Row.mariadb                         avgt  400    572.919 ±   3.168  us/op
BenchmarkSelect1Row.mysql                           avgt  400    591.628 ±   3.272  us/op
BenchmarkSelect1RowFailover.mariadb                 avgt  400    575.876 ±   2.613  us/op
BenchmarkSelect1RowFailover.mysql                   avgt  400    621.134 ±   3.407  us/op
BenchmarkSelect1RowPreparedNoCache.mariadb          avgt  400    650.532 ±   3.633  us/op
BenchmarkSelect1RowPreparedNoCache.mysql            avgt  400    691.336 ±   4.895  us/op
BenchmarkSelect1RowPreparedWithCache.mariadb        avgt  400    561.409 ±   2.875  us/op
BenchmarkSelect1RowPreparedWithCache.mysql          avgt  400    582.768 ±   5.532  us/op



```

##### How to read it :

ms/op means millisecond per operation, us/op microsecond per operation.

```
BenchmarkOneInsert.drizzle                          avgt  400     81.181 ±   0.514  us/op
BenchmarkOneInsert.mariadb                          avgt  400     60.899 ±   0.321  us/op
BenchmarkOneInsert.mysql                            avgt  400     77.789 ±   1.272  us/op
```


<div style="text-align:center"><img src ="results/insert_one_data.png" /></div>

BenchmarkOneInsert = execute query "INSERT INTO PerfTextQuery (charValue) values ('abcdefghij0123456')"
Using mariadb driver, the average time to insert one data is 60.899 microsecond, and 99.9% of queries executes time are comprised between 60.578 (60.899 - 0.321) and 61.22 microseconds (60.899 + 0.321).
Using MySQL java driver, average execution time is 77.789 millisecond.   

(remember that INSERT queries are executed on BLACKHOLE engine, those number just reflect the execution time of the driver + exchanges with database + query parsing  = without any write in system files).



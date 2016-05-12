package org.perf.jdbc.common;

import org.openjdk.jmh.annotations.*;

import java.sql.*;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@Warmup(iterations = 15, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 40, timeUnit = TimeUnit.MILLISECONDS)
@Fork(10)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class BenchmarkInit {

    @State(Scope.Thread)
    public static class MyState {
        private String server = System.getProperty("host", "localhost");
        public Connection mysqlConnectionRewrite;
        public Connection mysqlConnection;
        public Connection mysqlConnectionNoCache;
        public Connection mysqlConnectionText;
        public Connection mysqlFailoverConnection;

        public Connection mariadbConnectionRewrite;
        public Connection mariadbConnection;
        public Connection mariadbConnectionNoCache;
        public Connection mariadbConnectionText;
        public Connection mariadbFailoverConnection;

        public Connection drizzleConnectionText;

        public String[] insertData = new String[1000];
        private static final Random rand = new Random();

        private Connection createConnection(String className, String url, Properties props) throws Exception {
            return ((Driver) Class.forName(className).newInstance()).connect(url, props);
        }

        private Connection createConnection(String className, String url) throws Exception {
            return ((Driver) Class.forName(className).newInstance()).connect(url, new Properties());
        }

        @Setup(Level.Trial)
        public void doSetup() throws Exception {
            String mysqlDriverClass = "com.mysql.jdbc.Driver";
            String mariaDriverClass = "org.mariadb.jdbc.Driver";
            String drizzleDriverClass = "org.drizzle.jdbc.DrizzleDriver";

            String baseUrl = "jdbc:mysql://" + server + ":3306/testj";
            String baseDrizzle = "jdbc:drizzle://" + server + ":3306/testj";

            Properties prepareProperties = new Properties();
            prepareProperties.setProperty("user", "perf");
            prepareProperties.setProperty("password", "!Password0");
            prepareProperties.setProperty("useServerPrepStmts", "true");
            prepareProperties.setProperty("cachePrepStmts", "true");
            prepareProperties.setProperty("useSSL", "false");

            Properties prepareNoCacheProperties = new Properties();
            prepareNoCacheProperties.setProperty("user", "perf");
            prepareNoCacheProperties.setProperty("password", "!Password0");
            prepareNoCacheProperties.setProperty("useServerPrepStmts", "true");
            prepareNoCacheProperties.setProperty("cachePrepStmts", "false");
            prepareNoCacheProperties.setProperty("useSSL", "false");

            Properties textProperties = new Properties();
            textProperties.setProperty("user", "perf");
            textProperties.setProperty("password", "!Password0");
            textProperties.setProperty("useServerPrepStmts", "false");
            textProperties.setProperty("useSSL", "false");

            Properties textPropertiesDrizzle = new Properties();
            textPropertiesDrizzle.setProperty("user", "perf");
            textPropertiesDrizzle.setProperty("password", "!Password0");

            String urlRewrite = "jdbc:mysql://" + server + ":3306/testj?user=perf&rewriteBatchedStatements=true&useSSL=false&password=!Password0";
            String urlFailover = "jdbc:mysql:replication://" + server + ":3306," + server + ":3306/testj?"
                    + "user=perf&useServerPrepStmts=false&validConnectionTimeout=0&useSSL=false&password=!Password0";

            //create different kind of connection
            mysqlConnection = createConnection(mysqlDriverClass, baseUrl, prepareProperties);
            mariadbConnection = createConnection(mariaDriverClass, baseUrl, prepareProperties);

            mysqlConnectionNoCache = createConnection(mysqlDriverClass, baseUrl, prepareNoCacheProperties);
            mariadbConnectionNoCache = createConnection(mariaDriverClass, baseUrl, prepareNoCacheProperties);

            mysqlConnectionText =  createConnection(mysqlDriverClass, baseUrl, textProperties);
            mariadbConnectionText =  createConnection(mariaDriverClass, baseUrl, textProperties);
            drizzleConnectionText = createConnection(drizzleDriverClass, baseDrizzle, textPropertiesDrizzle);

            mysqlConnectionRewrite = createConnection(mysqlDriverClass, urlRewrite);
            mariadbConnectionRewrite = createConnection(mariaDriverClass, urlRewrite);

            mysqlFailoverConnection = createConnection(mysqlDriverClass, urlFailover);
            mariadbFailoverConnection = createConnection(mariaDriverClass, urlFailover);

            try (Statement statement = mysqlConnection.createStatement()) {
                //use black hole engine. so test are not stored and to avoid server disk access permitting more stable result
                //if "java.sql.SQLSyntaxErrorException: Unknown storage engine 'BLACKHOLE'". restart database
                try {
                    statement.execute("INSTALL SONAME 'ha_blackhole'");
                } catch (Exception e) {
                }

                statement.execute("CREATE TABLE IF NOT EXISTS PerfTextQuery(charValue VARCHAR(100) NOT NULL) ENGINE = BLACKHOLE");
                statement.execute("CREATE TABLE IF NOT EXISTS PerfTextQueryBlob(blobValue LONGBLOB NOT NULL) ENGINE = BLACKHOLE");
                statement.execute("CREATE TABLE IF NOT EXISTS PerfReadQuery(id int NOT NULL, charValue VARCHAR(100) NOT NULL, PRIMARY KEY (`id`), INDEX `CHAR_INDEX` (`charValue`))");
                statement.execute("CREATE TABLE IF NOT EXISTS PerfReadQueryBig(charValue VARCHAR(5000), charValue2 VARCHAR(5000) NOT NULL)");
                statement.execute("DROP PROCEDURE IF EXISTS withResultSet");
                statement.execute("DROP PROCEDURE IF EXISTS inoutParam");
                statement.execute("DROP FUNCTION IF EXISTS testFunctionCall");
                statement.execute("CREATE PROCEDURE withResultSet(a int) begin select a; end");
                statement.execute("CREATE PROCEDURE inoutParam(INOUT p1 INT) begin set p1 = p1 + 1; end");
                statement.execute("CREATE FUNCTION testFunctionCall(a float, b bigint, c int) RETURNS INT NO SQL \n"
                        + "BEGIN \n"
                        + "RETURN a; \n"
                        + "END");
                statement.execute("TRUNCATE PerfTextQuery");
                statement.execute("TRUNCATE PerfTextQueryBlob");
                statement.execute("TRUNCATE PerfReadQuery");
                statement.execute("TRUNCATE PerfReadQueryBig");
            }

            //Insert DATA to permit test read perf
            try (PreparedStatement preparedStatement = mysqlConnectionRewrite.prepareStatement("INSERT INTO PerfReadQuery (id, charValue) values (?, ?)")) {
                for (int i = 0; i < 1000; i++) {
                    preparedStatement.setInt(1, i);
                    preparedStatement.setString(2, "abc" + i);
                    preparedStatement.addBatch();
                }
                preparedStatement.executeBatch();
            }

            byte[] arr = new byte[5000];
            for (int i = 0; i < 5000; i++) {
                arr[i] = (byte)(i % 128);
            }
            String data = new String(arr);
            try (PreparedStatement preparedStatement2 = mysqlConnectionRewrite.prepareStatement("INSERT INTO PerfReadQueryBig (charValue, charValue2) values (?, ?)")) {
                for (int i = 0; i < 1000; i++) {
                    preparedStatement2.setString(1, data);
                    preparedStatement2.setString(2, data);
                    preparedStatement2.addBatch();
                }
                preparedStatement2.executeBatch();
            }

            //populate data
            for (int i = 0; i < 1000; i++) {
                insertData[i] = randomAscii(20);
            }

        }

        /**
         * Generate a random ASCII string of a given length.
         */
        public static String randomAscii(int length) {
            int interval='~'-' '+1;

            byte []buf = new byte[length];
            rand.nextBytes(buf);
            for (int i = 0; i < length; i++) {
                if (buf[i] < 0) {
                    buf[i] = (byte)((-buf[i] % interval) + ' ');
                } else {
                    buf[i] = (byte)((buf[i] % interval) + ' ');
                }
            }
            return new String(buf);
        }

        @TearDown(Level.Trial)
        public void doTearDown() throws SQLException {

            mysqlConnection.close();
            mysqlConnectionNoCache.close();
            mysqlConnectionRewrite.close();
            mysqlConnectionText.close();
            mysqlFailoverConnection.close();

            mariadbConnection.close();
            mariadbConnectionNoCache.close();
            mariadbConnectionRewrite.close();
            mariadbConnectionText.close();
            mariadbFailoverConnection.close();

            drizzleConnectionText.close();
        }
    }

}

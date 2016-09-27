package org.perf.jdbc.common;

import org.openjdk.jmh.annotations.*;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@Warmup(iterations = 10, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 20, timeUnit = TimeUnit.MILLISECONDS)
@Fork(value = 10)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class BenchmarkInit {

    @State(Scope.Thread)
    public static class MyState {
        private String server = System.getProperty("host", "localhost");
        private String port = System.getProperty("port", "3306");
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

        public int functionVar1 = 2;
        public int functionVar2 = 1;
        public int functionVar3 = 1;

        //populate data
        private static final Random rand = new Random();
        public static String[] insertData = new String[1000];
        static {
            for (int i = 0; i < 1000; i++) {
                insertData[i] = randomAscii(100);
            }
        }

        public int counter = 0;

        private Connection createConnection(String className, String url, Properties props) throws Exception {
            return ((Driver) Class.forName(className).newInstance()).connect(url, props);
        }

        private Connection createConnection(String className, String url) throws Exception {
            return ((Driver) Class.forName(className).newInstance()).connect(url, new Properties());
        }

        @Setup(Level.Invocation)
        public void doSetupInvocation() throws Exception {
            counter = 0;
        }

        @Setup(Level.Trial)
        public void doSetup() throws Exception {
            String mysqlDriverClass = "com.mysql.jdbc.Driver";
            String mariaDriverClass = "org.mariadb.jdbc.Driver";
            String drizzleDriverClass = "org.drizzle.jdbc.DrizzleDriver";

            String baseUrl = "jdbc:mysql://" + server + ":" + port + "/testj";
            String baseDrizzle = "jdbc:drizzle://" + server + ":" + port + "/testj";

            Properties prepareProperties = new Properties();
            prepareProperties.setProperty("user", "perf");
            prepareProperties.setProperty("password", "!Password0");
            prepareProperties.setProperty("useServerPrepStmts", "true");
            prepareProperties.setProperty("cachePrepStmts", "true");
            prepareProperties.setProperty("useSSL", "false");
            prepareProperties.setProperty("characterEncoding", "UTF-8");

            Properties prepareNoCacheProperties = new Properties();
            prepareNoCacheProperties.setProperty("user", "perf");
            prepareNoCacheProperties.setProperty("password", "!Password0");
            prepareNoCacheProperties.setProperty("useServerPrepStmts", "true");
            prepareNoCacheProperties.setProperty("cachePrepStmts", "false");
            prepareNoCacheProperties.setProperty("useSSL", "false");
            prepareNoCacheProperties.setProperty("characterEncoding", "UTF-8");

            Properties textProperties = new Properties();
            textProperties.setProperty("user", "perf");
            textProperties.setProperty("password", "!Password0");
            textProperties.setProperty("useServerPrepStmts", "false");
            textProperties.setProperty("useSSL", "false");
            textProperties.setProperty("characterEncoding", "UTF-8");

            Properties textPropertiesDrizzle = new Properties();
            textPropertiesDrizzle.setProperty("user", "perf");
            textPropertiesDrizzle.setProperty("password", "!Password0");

            String urlRewrite = "jdbc:mysql://" + server + ":" + port + "/testj?user=perf&rewriteBatchedStatements=true&useSSL=false"
                    + "&password=!Password0&characterEncoding=UTF-8";
            String urlFailover = "jdbc:mysql:replication://" + server + ":" + port + "," + server + ":" + port + "/testj?"
                    + "user=perf&useServerPrepStmts=false&validConnectionTimeout=0&useSSL=false&password=!Password0&characterEncoding=UTF-8";


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

            //avoid filling binlogs
            binLogToFalse(new Connection[]{
                    mysqlConnectionNoCache,
                    mysqlConnectionRewrite,
                    mysqlConnectionText,
                    mysqlFailoverConnection,

                    mariadbConnection,
                    mariadbConnectionNoCache,
                    mariadbConnectionRewrite,
                    mariadbConnectionText,
                    mariadbFailoverConnection,

                    drizzleConnectionText
            });

            try (Statement statement = mariadbConnection.createStatement()) {
                //use black hole engine. so test are not stored and to avoid server disk access permitting more stable result
                //if "java.sql.SQLSyntaxErrorException: Unknown storage engine 'BLACKHOLE'". restart database
                try {
                    statement.execute("INSTALL SONAME 'ha_blackhole'");
                } catch (Exception e) {
                }
                statement.execute("DROP TABLE IF EXISTS blackholeTable");

                statement.execute("CREATE TABLE IF NOT EXISTS blackholeTable(charValue VARCHAR(100) NOT NULL) ENGINE = BLACKHOLE");
                statement.execute("DROP PROCEDURE IF EXISTS withResultSet");
                statement.execute("DROP PROCEDURE IF EXISTS inoutParam");
                statement.execute("DROP FUNCTION IF EXISTS testFunctionCall");
                statement.execute("CREATE PROCEDURE withResultSet(a int) begin select a; end");
                statement.execute("CREATE PROCEDURE inoutParam(INOUT p1 INT) begin set p1 = p1 + 1; end");
                statement.execute("CREATE FUNCTION testFunctionCall(a float, b bigint, c int) RETURNS INT NO SQL \n"
                        + "BEGIN \n"
                        + "RETURN a; \n"
                        + "END");
            }



        }

        private void binLogToFalse(Connection[] connections) throws SQLException {
            for (Connection connection : connections) {
                connection.createStatement().executeQuery("SET sql_log_bin = 0;");
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

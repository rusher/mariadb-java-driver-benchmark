package org.perf.jdbc;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

@State(Scope.Benchmark)
@Warmup(iterations = 10, timeUnit = TimeUnit.SECONDS, time = 1)
@Measurement(iterations = 10, timeUnit = TimeUnit.SECONDS, time = 1)
@Fork(value = 10)
//not setting thread = number of processor
//@Threads(value = 1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class Common {

  @State(Scope.Benchmark)
  public static class MyState {
    public String server = System.getProperty("host", "localhost");
    public String port = System.getProperty("port", "3306");

    @Param({"mysql", "mariadb", "drizzle"})
    //when using multiple threads, drizzle is lost
//    @Param({"mysql", "mariadb"})
    String driver;

    public Connection connection;
    public Statement statement;

    @Setup(Level.Trial)
    public void doSetup() throws Exception {
      connection = getConnection(driver, server, port);
      statement = connection.createStatement();
    }

    @TearDown(Level.Trial)
    public void doTearDown() throws SQLException {
      connection.close();
    }

  }


  private static Connection getConnection(String driver, String server, String port) throws Exception {
    Properties textProperties = new Properties();
    textProperties.setProperty("user", "perf");
    textProperties.setProperty("password", "!Password0");
    textProperties.setProperty("useServerPrepStmts", "false");
    textProperties.setProperty("useSSL", "false");
    textProperties.setProperty("characterEncoding", "UTF-8");
    textProperties.setProperty("useBulkStmts", "false");
    textProperties.setProperty("useBatchMultiSend", "false");
    textProperties.setProperty("serverTimezone", "UTC");
    Connection connection;
    switch (driver) {
      case "mysql":
        connection = createConnection("com.mysql.cj.jdbc.Driver",
            "jdbc:mysql://" + server + ":" + port + "/testj",
            textProperties);
        break;
      case "mariadb":
        connection = createConnection("org.mariadb.jdbc.Driver",
            "jdbc:mysql://" + server + ":" + port + "/testj",
            textProperties);
        break;
      case "drizzle":
        Properties textPropertiesDrizzle = new Properties();
        textPropertiesDrizzle.setProperty("user", "perf");
        textPropertiesDrizzle.setProperty("password", "!Password0");
        connection = createConnection("org.drizzle.jdbc.DrizzleDriver",
            "jdbc:drizzle://" + server + ":" + port + "/testj",
            textPropertiesDrizzle);
        break;
        default:
          throw new Exception("NO driver ");
    }
    connection.createStatement().executeQuery("SET sql_log_bin = 0;");
    return connection;
  }

  private static Connection createConnection(String className, String url, Properties props) throws Exception {
    return ((Driver) Class.forName(className).newInstance()).connect(url, props);
  }

}

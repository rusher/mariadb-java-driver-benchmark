package org.perf.jdbc;

import java.sql.Driver;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Warmup;

public class Create_and_close_Connection extends Common {
  static Properties properties = new Properties();
  static {
    properties.setProperty("user", "perf");
    properties.setProperty("password", "!Password0");
    properties.setProperty("useServerPrepStmts", "false");
    properties.setProperty("useSSL", "false");
    properties.setProperty("characterEncoding", "UTF-8");
    properties.setProperty("useBulkStmts", "false");
    properties.setProperty("useBatchMultiSend", "false");
    properties.setProperty("serverTimezone", "UTC");
    properties.setProperty("tcpAbortiveClose", "true");
  }

  @Benchmark
  @Fork(jvmArgsAppend = {"-Xmx32m", "-Xms32m"})
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  @Warmup(iterations = 20, timeUnit = TimeUnit.MICROSECONDS, time = 20)
  @Measurement(iterations = 100, timeUnit = TimeUnit.MICROSECONDS, time = 20)
  public String test(MyState state) throws Throwable {
    java.sql.Connection connection = getConnection(state.driver, state.server, state.port);
    String schema = connection.getSchema();
    connection.close();
    return schema;
  }



  private static java.sql.Connection getConnection(String driver, String server, String port) throws Exception {
    java.sql.Connection connection;
    switch (driver) {
      case "mysql":
        connection = createConnection("com.mysql.cj.jdbc.Driver",
            "jdbc:mysql://" + server + ":" + port + "/testj",
            properties);
        break;
      case "mariadb":
        connection = createConnection("org.mariadb.jdbc.Driver",
            "jdbc:mysql://" + server + ":" + port + "/testj",
            properties);
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

  private static java.sql.Connection createConnection(String className, String url, Properties props) throws Exception {
    return ((Driver) Class.forName(className).newInstance()).connect(url, props);
  }

}

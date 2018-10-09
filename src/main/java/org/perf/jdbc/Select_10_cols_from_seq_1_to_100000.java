package org.perf.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

public class Select_10_cols_from_seq_1_to_100000 extends Common {

  private static String request = "SELECT ";
  static {
    for (int i = 0; i < 10; i++) {
      request += (i==0 ? "":",") +" REPEAT('a', 100)";
    }
    request += " from seq_1_to_100000";
  }


  public String executeQuery(Statement stmt) throws SQLException {
    ResultSet rs = stmt.executeQuery(request);
    rs.next();
    for (int i = 1; i < 10; i++) {
      rs.getString(i);
    }
    return rs.getString(10);

  }

  @Benchmark()
  @Warmup(time = 5)
  @Measurement(time = 5)
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  //use 256m, since drizzle and mysql will throw java heap space
  @Fork(value = 7, jvmArgsAppend = {"-Xmx256m", "-Xms256m"})
  public String test(MyState state) throws Throwable {
    return executeQuery(state.statement);
  }
}

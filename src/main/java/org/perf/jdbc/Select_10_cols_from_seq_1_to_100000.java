package org.perf.jdbc;

import java.sql.PreparedStatement;
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
      request += (i==0 ? "":",") +" REPEAT('a', 10)";
    }
    request += " from seq_1_to_2 WHERE 1 = ?";
  }

  @Benchmark()
//use 256m, since drizzle and mysql will throw java heap space
  public String test(MyState state) throws Throwable {
    try (PreparedStatement preparedStatement = state.connection.prepareStatement(request)) {
      preparedStatement.setInt(1, 1);
      ResultSet rs = preparedStatement.executeQuery();
      rs.next();
      for (int i = 1; i < 10; i++) {
        rs.getString(i);
      }
      return rs.getString(10);
    }
  }
}

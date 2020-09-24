package org.perf.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;

public class Select_1 extends Common {

  @Benchmark
  @Fork(jvmArgsAppend = {"-Xmx32m", "-Xms32m"})
  public int test(MyState state) throws Throwable {
    try (PreparedStatement preparedStatement =
             state.connection.prepareStatement("select ?")) {
      preparedStatement.setString(1, "1");
      ResultSet rs = preparedStatement.executeQuery();
      rs.next();
      return rs.getInt(1);
    }
  }
}

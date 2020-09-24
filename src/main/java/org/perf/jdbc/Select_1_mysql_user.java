package org.perf.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;

public class Select_1_mysql_user extends Common {
  String[] res = new String[46];

  @Benchmark
  @Fork(jvmArgsAppend = {"-Xmx32m", "-Xms32m"})
  public String[] test(MyState state) throws Throwable {
    try (PreparedStatement preparedStatement =
             state.connection.prepareStatement("select * from mysql.user WHERE 1 = ? LIMIT 1")) {
      preparedStatement.setInt(1, 1);
      ResultSet rs = preparedStatement.executeQuery();
      rs.next();
      for (int i = 1; i < 46; i++) {
        res[i - 1] = rs.getString(i);
      }
      return res;
    }
  }
}

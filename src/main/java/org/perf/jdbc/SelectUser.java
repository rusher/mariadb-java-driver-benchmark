package org.perf.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;

public class SelectUser extends Common {
  private String request = "select * from mysql.user LIMIT 1";

  public String executeQuery(Statement stmt) throws SQLException {
    ResultSet rs = stmt.executeQuery(request);
    rs.next();
    for (int i = 1; i < 46; i++) {
      rs.getString(i);
    }
    return rs.getString(46);

  }

  @Benchmark
  @Fork(jvmArgsAppend = {"-Xmx128m", "-Xms128m", "-Duser.country=US", "-Duser.language=en"})
  public String test(MyState state) throws Throwable {
    return executeQuery(state.statement);
  }
}

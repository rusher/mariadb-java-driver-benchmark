package org.perf.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;

public class Select_1 extends Common {

  @Benchmark
  @Fork(jvmArgsAppend = {"-Xmx32m", "-Xms32m"})
  public int test(MyState state) throws Throwable {
    ResultSet rs = state.statement.executeQuery("select 1");
    rs.next();
    return rs.getInt(1);
  }
}

package org.perf.jdbc;

import java.sql.SQLException;
import java.sql.Statement;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;

public class Do_1 extends Common {

  @Benchmark
  @Fork(jvmArgsAppend = {"-Xmx32m", "-Xms32m"})
  public int test(MyState state) throws Throwable {
    return state.statement.executeUpdate("do 1");
  }
}

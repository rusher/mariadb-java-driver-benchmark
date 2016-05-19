package org.perf.jdbc;

import org.openjdk.jmh.annotations.Benchmark;
import org.perf.jdbc.common.BenchmarkInit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class BenchmarkSelect1RowPrepareAbstract extends BenchmarkInit {
    private String request = "SELECT ?";
    private String var = "abc0";

    public String select1RowPrepare(Connection connection) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(request)) {
            preparedStatement.setString(1, var);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                rs.next();
                return rs.getString(1);
            }
        }
    }
}

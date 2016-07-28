package org.perf.jdbc;

import org.perf.jdbc.common.BenchmarkInit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class BenchmarkSelect1RowPrepareAbstract extends BenchmarkInit {
    private String request = "SELECT ?";

    public String select1RowPrepare(Connection connection, MyState state) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(request)) {
            preparedStatement.setString(1, state.insertData[state.counter++]); //a random 100 byte data
            try (ResultSet rs = preparedStatement.executeQuery()) {
                rs.next();
                return rs.getString(1);
            }
        }
    }
}

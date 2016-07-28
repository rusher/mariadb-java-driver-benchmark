package org.perf.jdbc;

import org.perf.jdbc.common.BenchmarkInit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public abstract class BenchmarkOneInsertPrepareAbstract extends BenchmarkInit {
    private String request = "INSERT INTO blackholeTable (charValue) values (?)";

    public boolean executeOneInsertPrepare(Connection connection, String[] datas) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(request)) {
            preparedStatement.setString(1, datas[0]); //a random 100 byte data
            return preparedStatement.execute();
        }
    }
}

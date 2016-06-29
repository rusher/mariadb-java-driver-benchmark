
package org.perf.jdbc;

import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.perf.jdbc.common.BenchmarkInit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class BenchmarkBatch1000InsertAbstract extends BenchmarkInit {
    private String request = "INSERT INTO blackholeTable (charValue) values (?)";

    public int[] executeBatch(Connection connection, String[] data) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(request)) {
            for (int i = 0; i < 1000; i++) {
                preparedStatement.setString(1, data[i]);
                preparedStatement.addBatch();
            }
            return preparedStatement.executeBatch();
        }
    }

}

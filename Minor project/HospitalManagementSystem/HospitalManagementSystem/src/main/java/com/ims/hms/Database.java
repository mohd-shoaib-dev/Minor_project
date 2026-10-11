package com.ims.hms;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Central MySQL connection utility. Configure credentials with environment variables. */
public final class Database {
    private Database() {}

    public static Connection getConnection() throws SQLException {
        String url = System.getenv().getOrDefault("HMS_DB_URL", "jdbc:mysql://localhost:3306/hospital");
        String user = System.getenv().getOrDefault("HMS_DB_USER", "root");
        String password = System.getenv().getOrDefault("HMS_DB_PASSWORD", "");
        return DriverManager.getConnection(url, user, password);
    }
}

package com.medieval.managers;

import com.medieval.MedievalKingdoms;
import java.sql.*;

public class DatabaseManager {
    private final MedievalKingdoms plugin;
    private Connection connection;
    
    public DatabaseManager(MedievalKingdoms plugin) {
        this.plugin = plugin;
    }
    
    public void initializeDatabase() throws SQLException {
        String host = plugin.getConfig().getString("database.host", "localhost");
        int port = plugin.getConfig().getInt("database.port", 3306);
        String database = plugin.getConfig().getString("database.name", "medieval");
        String username = plugin.getConfig().getString("database.username", "root");
        String password = plugin.getConfig().getString("database.password", "");
        
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(
                "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&serverTimezone=UTC",
                username, password
            );
            createTables();
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL Driver not found!", e);
        }
    }
    
    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // Players table
            stmt.execute("CREATE TABLE IF NOT EXISTS players (" +
                "uuid VARCHAR(36) PRIMARY KEY, " +
                "name VARCHAR(16), " +
                "kingdom_id INT DEFAULT -1, " +
                "rank VARCHAR(20) DEFAULT 'PEASANT', " +
                "gold DOUBLE DEFAULT 100, " +
                "reputation INT DEFAULT 50, " +
                "votecount INT DEFAULT 0)");
            
            // Kingdoms table
            stmt.execute("CREATE TABLE IF NOT EXISTS kingdoms (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "name VARCHAR(32) UNIQUE, " +
                "leader_uuid VARCHAR(36), " +
                "color VARCHAR(7) DEFAULT '§6', " +
                "treasury DOUBLE DEFAULT 0)");
            
            // Votes table
            stmt.execute("CREATE TABLE IF NOT EXISTS votes (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "kingdom_id INT, " +
                "voter_uuid VARCHAR(36), " +
                "candidate_uuid VARCHAR(36), " +
                "UNIQUE KEY unique_vote (kingdom_id, voter_uuid))");
        }
    }
    
    public Connection getConnection() { return connection; }
    
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

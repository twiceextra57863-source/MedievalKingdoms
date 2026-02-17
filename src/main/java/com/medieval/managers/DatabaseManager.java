package com.medieval.managers;

import com.medieval.MedievalKingdoms;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.*;
import java.util.concurrent.CompletableFuture;

public class DatabaseManager {
    private final MedievalKingdoms plugin;
    private HikariDataSource dataSource;
    
    public DatabaseManager(MedievalKingdoms plugin) {
        this.plugin = plugin;
    }
    
    public void initializeDatabase() throws SQLException {
        String host = plugin.getConfig().getString("database.host", "localhost");
        int port = plugin.getConfig().getInt("database.port", 3306);
        String database = plugin.getConfig().getString("database.name", "medieval");
        String username = plugin.getConfig().getString("database.username", "root");
        String password = plugin.getConfig().getString("database.password", "");
        
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&serverTimezone=UTC");
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30000);
        config.setLeakDetectionThreshold(10000);
        config.setPoolName("MedievalPool");
        
        dataSource = new HikariDataSource(config);
        
        // Test connection
        try (Connection conn = dataSource.getConnection()) {
            if (conn.isValid(1)) {
                plugin.getLogger().info("§aDatabase connection test successful!");
            }
        }
        
        createTables();
    }
    
    private void createTables() throws SQLException {
        String[] createTableQueries = {
            // Players table
            "CREATE TABLE IF NOT EXISTS players (" +
                "uuid VARCHAR(36) PRIMARY KEY, " +
                "name VARCHAR(16) NOT NULL, " +
                "kingdom_id INT DEFAULT -1, " +
                "rank VARCHAR(20) DEFAULT 'PEASANT', " +
                "votecount INT DEFAULT 0, " +
                "gold DOUBLE DEFAULT 100.0, " +
                "reputation INT DEFAULT 50, " +
                "join_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "last_active TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                "INDEX idx_kingdom (kingdom_id))",
            
            // Kingdoms table
            "CREATE TABLE IF NOT EXISTS kingdoms (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "name VARCHAR(32) UNIQUE NOT NULL, " +
                "leader_uuid VARCHAR(36) NOT NULL, " +
                "capital_world VARCHAR(32), " +
                "capital_x INT, " +
                "capital_y INT, " +
                "capital_z INT, " +
                "color VARCHAR(7) DEFAULT '§6', " +
                "banner TEXT, " +
                "treasury DOUBLE DEFAULT 0, " +
                "created TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "INDEX idx_leader (leader_uuid))",
            
            // Elections table
            "CREATE TABLE IF NOT EXISTS elections (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "kingdom_id INT NOT NULL, " +
                "candidate_uuid VARCHAR(36) NOT NULL, " +
                "votes INT DEFAULT 0, " +
                "start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "end_time TIMESTAMP, " +
                "status VARCHAR(20) DEFAULT 'ACTIVE', " +
                "INDEX idx_kingdom_election (kingdom_id), " +
                "INDEX idx_candidate (candidate_uuid))",
            
            // Votes table
            "CREATE TABLE IF NOT EXISTS votes (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "kingdom_id INT NOT NULL, " +
                "voter_uuid VARCHAR(36) NOT NULL, " +
                "candidate_uuid VARCHAR(36) NOT NULL, " +
                "vote_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "UNIQUE KEY unique_vote (kingdom_id, voter_uuid), " +
                "INDEX idx_voter (voter_uuid), " +
                "INDEX idx_candidate_vote (candidate_uuid))",
            
            // Territories table
            "CREATE TABLE IF NOT EXISTS territories (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "kingdom_id INT NOT NULL, " +
                "world VARCHAR(32) NOT NULL, " +
                "chunk_x INT NOT NULL, " +
                "chunk_z INT NOT NULL, " +
                "claimed_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "UNIQUE KEY unique_chunk (world, chunk_x, chunk_z), " +
                "INDEX idx_kingdom_territory (kingdom_id))",
            
            // Wars table
            "CREATE TABLE IF NOT EXISTS wars (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "attacker_kingdom_id INT NOT NULL, " +
                "defender_kingdom_id INT NOT NULL, " +
                "start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "end_time TIMESTAMP, " +
                "status VARCHAR(20) DEFAULT 'ACTIVE', " +
                "winner_id INT, " +
                "INDEX idx_attacker (attacker_kingdom_id), " +
                "INDEX idx_defender (defender_kingdom_id))",
            
            // Alliances table
            "CREATE TABLE IF NOT EXISTS alliances (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "kingdom_id_1 INT NOT NULL, " +
                "kingdom_id_2 INT NOT NULL, " +
                "created TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "type VARCHAR(20) DEFAULT 'DEFENSIVE', " +
                "UNIQUE KEY unique_alliance (kingdom_id_1, kingdom_id_2), " +
                "INDEX idx_kingdom1 (kingdom_id_1), " +
                "INDEX idx_kingdom2 (kingdom_id_2))",
            
            // Election history table
            "CREATE TABLE IF NOT EXISTS election_history (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "kingdom_id INT NOT NULL, " +
                "winner_uuid VARCHAR(36) NOT NULL, " +
                "votes INT NOT NULL, " +
                "date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "INDEX idx_kingdom_history (kingdom_id))"
        };
        
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            for (String query : createTableQueries) {
                stmt.execute(query);
            }
            plugin.getLogger().info("§aDatabase tables created/verified successfully!");
        }
    }
    
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
    
    public CompletableFuture<ResultSet> executeQuery(String query, Object... params) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query)) {
                
                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
                }
                
                return stmt.executeQuery();
            } catch (SQLException e) {
                plugin.getLogger().severe("Database query error: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        });
    }
    
    public CompletableFuture<Integer> executeUpdate(String query, Object... params) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
                
                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
                }
                
                int affected = stmt.executeUpdate();
                
                // Get generated keys if any
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    return rs.getInt(1);
                }
                
                return affected;
            } catch (SQLException e) {
                plugin.getLogger().severe("Database update error: " + e.getMessage());
                e.printStackTrace();
                return 0;
            }
        });
    }
    
    public void closeConnection() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("§cDatabase connection closed.");
        }
    }
            }

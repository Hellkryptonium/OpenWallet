package io.openwallet.db;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.stream.Collectors;

public class DatabaseInitializer {

    public static void main(String[] args) {
        initialize();
    }

    public static void initialize() {
        System.out.println("Initializing database...");
        
        // 1. Connect to MySQL server (no specific DB) to ensure DB exists
        String serverUrl = "jdbc:mysql://localhost:3306/";
        String user = DatabaseConfig.getUser();
        String password = DatabaseConfig.getPassword();

        try (Connection conn = DriverManager.getConnection(serverUrl, user, password);
             Statement stmt = conn.createStatement()) {
            
            System.out.println("Connected to MySQL server.");
            
            // Read schema.sql
            String schemaSql = loadSchemaSql();
            
            // Split by semicolon to execute statements individually
            // (Simple splitter, might break on complex stored procs but fine for this schema)
            String[] statements = schemaSql.split(";");
            
            for (String sql : statements) {
                if (!sql.trim().isEmpty()) {
                    stmt.execute(sql.trim());
                    System.out.println("Executed: " + sql.trim().substring(0, Math.min(sql.trim().length(), 50)) + "...");
                }
            }
            
            System.out.println("Database initialization complete.");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String loadSchemaSql() {
        try (InputStream is = DatabaseInitializer.class.getResourceAsStream("/db/schema.sql");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load schema.sql", e);
        }
    }
}

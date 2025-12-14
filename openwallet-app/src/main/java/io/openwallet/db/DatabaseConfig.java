package io.openwallet.db;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class DatabaseConfig {
    private static final Properties properties = new Properties();

    static {
        try (InputStream input = DatabaseConfig.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (input == null) {
                System.out.println("Sorry, unable to find db.properties");
            } else {
                properties.load(input);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static String getUrl() {
        String env = System.getenv("OPENWALLET_DB_URL");
        return (env != null && !env.isBlank()) ? env : properties.getProperty("db.url");
    }

    public static String getUser() {
        String env = System.getenv("OPENWALLET_DB_USER");
        return (env != null && !env.isBlank()) ? env : properties.getProperty("db.user");
    }

    public static String getPassword() {
        String env = System.getenv("OPENWALLET_DB_PASSWORD");
        return (env != null && !env.isBlank()) ? env : properties.getProperty("db.password");
    }

    public static String getRpcUrl() {
        String env = System.getenv("OPENWALLET_RPC_URL");
        return (env != null && !env.isBlank()) ? env : properties.getProperty("rpc.url");
    }
}

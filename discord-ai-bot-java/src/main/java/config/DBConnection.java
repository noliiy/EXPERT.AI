package config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;
import java.io.InputStream;

/**
 * DBConnection is responsible for reading database connection properties from
 * a properties file and providing open JDBC Connections.
 */
public class DBConnection {

    private static final String PROPERTIES_FILE = "/db.properties";

    /**
     * Loads connection properties and returns an open JDBC Connection.
     *
     * @return a Connection object to the configured database
     * @throws Exception if the properties file is missing or connection fails
     */
    public static Connection getConnection() throws Exception {
        // 1) Load properties from the classpath
        Properties props = new Properties();
        try (InputStream in = DBConnection.class.getResourceAsStream(PROPERTIES_FILE)) {
            if (in == null) {
                throw new RuntimeException("Unable to find " + PROPERTIES_FILE + " in the classpath");
            }
            props.load(in);
        }

        // 2) Read the URL, username, and password
        String url      = props.getProperty("db.url");
        String user     = props.getProperty("db.user");
        String password = props.getProperty("db.password");

        // 3) Create and return the JDBC connection
        return DriverManager.getConnection(url, user, password);
    }

    /**
     * Quick test method to verify connection parameters.
     */
    public static void main(String[] args) {
        try (Connection conn = getConnection()) {
            System.out.println("✅ Successfully connected to: " + conn.getMetaData().getURL());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
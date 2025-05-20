package config;

import java.io.InputStream;
import java.util.Properties;

/**
 * DBConfigTest loads database connection properties from db.properties
 * and prints the connection URL and username to the console.
 */
public class DBConfigTest {
    public static void main(String[] args) throws Exception {
        // 1) Load db.properties from the classpath
        Properties props = new Properties();
        try (InputStream in = DBConfigTest.class.getResourceAsStream("/db.properties")) {
            if (in == null) {
                throw new RuntimeException("Cannot find /db.properties in the classpath");
            }
            props.load(in);
        }

        // 2) Retrieve connection settings
        String url  = props.getProperty("db.url");
        String user = props.getProperty("db.user");
        String pass = props.getProperty("db.password");

        // 3) Output connection info (excluding password for security)
        System.out.println("Database URL: " + url);
        System.out.println("Database User: " + user);
    }
}

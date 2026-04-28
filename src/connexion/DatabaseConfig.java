package connexion;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConfig {
    // Identifiants fournis dans votre fichier Word pour le portail 
    private static final String URL = "jdbc:oracle:thin:@gaia.emp.uqtr.ca:1521:coursbd.uqtr.ca";
    private static final String USER = "SMI1002_030"; 
    private static final String PASSWORD = "69ajxa84";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
package clinicamed;

import java.sql.*;

public class Conexao {
    private static final String URL  = System.getenv().getOrDefault("CLINICA_DB_URL",
            "jdbc:mysql://localhost:3306/ClinicaMed?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Sao_Paulo");
    private static final String USER = System.getenv().getOrDefault("CLINICA_DB_USER", "root");
    private static final String PASS = System.getenv().getOrDefault("CLINICA_DB_PASS", "");

    public static Connection get() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}

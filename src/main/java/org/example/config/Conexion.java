package org.example.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Conexion {
    private static final String url = "jdbc:mysql://localhost:3306/farmacia";
    private static final String user = "root";
    private static final String password = "12345678";

    public static Connection conectar() {
        Connection conexion = null;
        try {

            Class.forName("com.mysql.cj.jdbc.Driver");

            conexion = DriverManager.getConnection(url, user, password);
        } catch (ClassNotFoundException e) {
            System.out.println("Error: No se encontro el Driver de MySQL: " + e.getMessage());
        } catch (SQLException err) {
            System.out.println("Error al conectar con MySQL: " + err.getMessage());
        }
        return conexion;
    }
}

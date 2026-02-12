package model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


public class Conexion {
    

    private static final String URL = "jdbc:mysql://127.0.0.1:3306/biblioteca_db";
    private static final String USER = "root";
    private static final String PASS = "1234"; 


    public static Connection getConexion() throws SQLException {
        try {
         
            Class.forName("com.mysql.cj.jdbc.Driver");
            
        
            return DriverManager.getConnection(URL, USER, PASS);
            
        } catch (ClassNotFoundException e) {
            
            throw new SQLException("Error: No se encontró el driver de MySQL (JDBC) en las librerías del proyecto.");
        } catch (SQLException e) {
           
            System.err.println("--- ERROR CRÍTICO DE CONEXIÓN ---");
            System.err.println("Causa: " + e.getMessage());
            throw e;
        }
    }
}
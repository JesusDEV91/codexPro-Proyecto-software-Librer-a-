package controller;

import java.net.URL;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Clase principal que lanza la aplicación.
 * El error "Location is required" se debe a que el cargador no encuentra el archivo.
 * Se soluciona usando una ruta absoluta desde el directorio de recursos.
 */
public class JesusBarrosoProyectoFXML extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // CORRECCIÓN: La ruta debe empezar por /view/ si el FXML está en ese paquete.
        // Si tu FXML se llama distinto, cámbialo aquí.
        URL location = getClass().getResource("/view/MainView.fxml");
        
        if (location == null) {
            // Este mensaje aparecerá en la consola si el nombre o la ruta están mal.
            System.err.println("¡ERROR: No se encontró el archivo FXML!");
            System.err.println("Asegúrate de que MainView.fxml esté dentro de la carpeta 'view' en Source Packages.");
            return;
        }

        Parent root = FXMLLoader.load(location);
        
        Scene scene = new Scene(root);
        
        // Configuración de la ventana principal
        stage.setTitle("Sistema de Gestión de Biblioteca - Proyecto FXML");
        stage.setScene(scene);
        stage.show();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
}
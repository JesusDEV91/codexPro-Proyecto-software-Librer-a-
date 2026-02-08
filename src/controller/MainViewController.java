package controller;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;


public class MainViewController implements Initializable {

    @FXML
    private StackPane contentArea; // El contenedor central donde se cargan las vistas

    @Override
    public void initialize(URL url, ResourceBundle rb) {
      
    }

    @FXML
    private void showLibros() {
        cambiarEscena("/view/LibrosView.fxml");
    }

    @FXML
    private void showSocios() {
        cambiarEscena("/view/SociosView.fxml");
    }

    @FXML
    private void showPrestamos() {
        cambiarEscena("/view/PrestamosView.fxml");
    }

  
    private void cambiarEscena(String fxmlPath) {
        try {
            URL url = getClass().getResource(fxmlPath);
            if (url == null) {
                System.err.println("No se encontró el archivo FXML en la ruta: " + fxmlPath);
                return;
            }
            Parent vista = FXMLLoader.load(url);
            contentArea.getChildren().clear(); 
            contentArea.getChildren().add(vista); 
        } catch (IOException e) {
            System.err.println("Error al cargar la escena: " + e.getMessage());
        }
    }
}
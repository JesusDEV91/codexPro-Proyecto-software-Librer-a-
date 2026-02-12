package controller;

import java.net.URL;
import java.sql.*;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import model.Conexion;
import model.Socio;


public class SociosViewController implements Initializable {

   
    @FXML private TableView<Socio> tableSocios;
    @FXML private TableColumn<Socio, Integer> colIdSocio;
    @FXML private TableColumn<Socio, String> colNombre;
    @FXML private TableColumn<Socio, String> colApellido;
    @FXML private TableColumn<Socio, String> colEmail;

    
    @FXML private TextField txtSearchSocio;
    @FXML private TextField txtNombre;
    @FXML private TextField txtApellido;
    @FXML private TextField txtEmail;
    @FXML private Label lblStatusSocio;
    
   
    @FXML private Button btnActualizarSocio;
    @FXML private Button btnEliminarSocio;
    @FXML private Button btnGuardarSocio;

    private ObservableList<Socio> listaSocios = FXCollections.observableArrayList();
    private Socio socioSeleccionado;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        //  Vincular columnas
        colIdSocio.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colApellido.setCellValueFactory(new PropertyValueFactory<>("apellido"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));

        //  Cargar datos y filtros
        loadSocios();
        setupFilter();

        //  Listener de selección profesional
        tableSocios.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                prepararEdicion(newVal);
            } else {
                limpiarSeleccion();
            }
        });
        
        limpiarSeleccion();
    }

    private void loadSocios() {
        listaSocios.clear();
        String query = "SELECT * FROM miembros";
        try (Connection cn = Conexion.getConexion();
             Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                listaSocios.add(new Socio(
                    rs.getInt("id_miembro"),
                    rs.getString("nombre"),
                    rs.getString("apellido"),
                    rs.getString("email")
                ));
            }
            tableSocios.setItems(listaSocios);
            updateStatus("Directorio de socios sincronizado.", "#64748b");
        } catch (SQLException e) {
            updateStatus("Error de conexión SQL.", "#ef4444");
        }
    }

    private void setupFilter() {
        FilteredList<Socio> filteredData = new FilteredList<>(listaSocios, p -> true);
        txtSearchSocio.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(socio -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String lowerCaseFilter = newValue.toLowerCase();
                return socio.getNombre().toLowerCase().contains(lowerCaseFilter) || 
                       socio.getApellido().toLowerCase().contains(lowerCaseFilter) ||
                       socio.getEmail().toLowerCase().contains(lowerCaseFilter);
            });
        });
        tableSocios.setItems(filteredData);
    }

    @FXML
    private void handleSaveSocio() {
        if (validarCampos()) {
            String sql = "INSERT INTO miembros (nombre, apellido, email) VALUES (?, ?, ?)";
            try (Connection cn = Conexion.getConexion();
                 PreparedStatement ps = cn.prepareStatement(sql)) {
                ps.setString(1, txtNombre.getText());
                ps.setString(2, txtApellido.getText());
                ps.setString(3, txtEmail.getText());
                ps.executeUpdate();
                
                loadSocios();
                handleClear();
                updateStatus("Socio registrado exitosamente.", "#10b981");
            } catch (SQLException e) {
                updateStatus("Error: El correo electrónico ya está registrado.", "#ef4444");
            }
        }
    }

    @FXML
    private void handleUpdateSocio() {
        if (socioSeleccionado != null && validarCampos()) {
            String sql = "UPDATE miembros SET nombre=?, apellido=?, email=? WHERE id_miembro=?";
            try (Connection cn = Conexion.getConexion();
                 PreparedStatement ps = cn.prepareStatement(sql)) {
                ps.setString(1, txtNombre.getText());
                ps.setString(2, txtApellido.getText());
                ps.setString(3, txtEmail.getText());
                ps.setInt(4, socioSeleccionado.getId());
                ps.executeUpdate();
                
                loadSocios();
                handleClear();
                updateStatus("Datos del socio actualizados.", "#f59e0b");
            } catch (SQLException e) {
                updateStatus("Error al actualizar la ficha del socio.", "#ef4444");
            }
        }
    }

    
     // Elimina un socio previa verificación de préstamos activos.
     
    @FXML
    private void handleDeleteSocio() {
        if (socioSeleccionado == null) return;

        int idSocio = socioSeleccionado.getId();

        // VALIDACIÓN
        if (isSocioConPrestamos(idSocio)) {
            Alert alertError = new Alert(Alert.AlertType.WARNING);
            alertError.setTitle("Restricción de Integridad");
            alertError.setHeaderText("No se puede eliminar al socio");
            alertError.setContentText("El socio '" + socioSeleccionado.getNombre() + " " + socioSeleccionado.getApellido() 
                    + "' tiene préstamos registrados en el sistema.\n\n"
                    + "Debe eliminar primero sus registros en la sección de 'Gestión de Préstamos' antes de darlo de baja.");
            alertError.showAndWait();
            return;
        }

        // Confirmación si no hay dependencias
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Baja de Socio");
        alert.setHeaderText(null);
        alert.setContentText("¿Desea eliminar definitivamente a " + socioSeleccionado.getNombre() + " " + socioSeleccionado.getApellido() + "?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String sql = "DELETE FROM miembros WHERE id_miembro = ?";
            try (Connection cn = Conexion.getConexion();
                 PreparedStatement ps = cn.prepareStatement(sql)) {
                ps.setInt(1, idSocio);
                ps.executeUpdate();
                
                loadSocios();
                handleClear();
                updateStatus("Socio eliminado del sistema.", "#ef4444");
            } catch (SQLException e) {
                updateStatus("Error crítico al eliminar socio.", "#ef4444");
            }
        }
    }

    
     //Verifica si un socio tiene préstamos en la tabla prestamos.
     
    private boolean isSocioConPrestamos(int idSocio) {
        String sql = "SELECT COUNT(*) FROM prestamos WHERE id_miembro = ?";
        try (Connection cn = Conexion.getConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idSocio);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error verificando préstamos de socio: " + e.getMessage());
        }
        return false;
    }

    @FXML
    private void handleClear() {
        txtNombre.clear();
        txtApellido.clear();
        txtEmail.clear();
        txtSearchSocio.clear();
        tableSocios.getSelectionModel().clearSelection();
        limpiarSeleccion();
        updateStatus("Listo para gestionar socios.", "#64748b");
    }

    private void prepararEdicion(Socio socio) {
        socioSeleccionado = socio;
        txtNombre.setText(socio.getNombre());
        txtApellido.setText(socio.getApellido());
        txtEmail.setText(socio.getEmail());
        
        if (btnActualizarSocio != null) btnActualizarSocio.setDisable(false);
        if (btnEliminarSocio != null) btnEliminarSocio.setDisable(false);
        if (btnGuardarSocio != null) btnGuardarSocio.setDisable(true);
    }

    private void limpiarSeleccion() {
        socioSeleccionado = null;
        if (btnActualizarSocio != null) btnActualizarSocio.setDisable(true);
        if (btnEliminarSocio != null) btnEliminarSocio.setDisable(true);
        if (btnGuardarSocio != null) btnGuardarSocio.setDisable(false);
    }

    private boolean validarCampos() {
        if (txtNombre.getText().isEmpty() || txtEmail.getText().isEmpty()) {
            updateStatus("Error: Nombre y Correo son obligatorios.", "#ef4444");
            return false;
        }
        return true;
    }

    private void updateStatus(String mensaje, String colorHex) {
        lblStatusSocio.setText(mensaje);
        lblStatusSocio.setStyle("-fx-text-fill: " + colorHex + "; -fx-font-weight: bold;");
    }
}
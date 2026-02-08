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

    private ObservableList<Socio> listaSocios = FXCollections.observableArrayList();
    private Socio socioSeleccionado;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colIdSocio.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colApellido.setCellValueFactory(new PropertyValueFactory<>("apellido"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));

        loadSocios();
        setupFilter();

        tableSocios.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                prepararEdicion(newVal);
            }
        });
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
        } catch (SQLException e) {
            lblStatusSocio.setText("Error de conexión con la base de datos.");
        }
    }

    private void setupFilter() {
        FilteredList<Socio> filteredData = new FilteredList<>(listaSocios, p -> true);
        txtSearchSocio.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(socio -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String lowerCaseFilter = newValue.toLowerCase();
                return socio.getNombre().toLowerCase().contains(lowerCaseFilter) || 
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
                lblStatusSocio.setText("Socio registrado.");
            } catch (SQLException e) {
                lblStatusSocio.setText("Error: El email ya existe.");
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
                lblStatusSocio.setText("Socio actualizado.");
            } catch (SQLException e) {
                lblStatusSocio.setText("Error al actualizar.");
            }
        }
    }

    
    @FXML
    private void handleDeleteSocio() {
        if (socioSeleccionado == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar eliminación");
        alert.setHeaderText(null);
        alert.setContentText("¿Seguro que desea eliminar a " + socioSeleccionado.getNombre() + "?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String sql = "DELETE FROM miembros WHERE id_miembro = ?";
            try (Connection cn = Conexion.getConexion();
                 PreparedStatement ps = cn.prepareStatement(sql)) {
                ps.setInt(1, socioSeleccionado.getId());
                ps.executeUpdate();
                loadSocios();
                handleClear();
                lblStatusSocio.setText("Socio eliminado.");
            } catch (SQLException e) {
                lblStatusSocio.setText("No se puede eliminar (tiene préstamos activos).");
            }
        }
    }

    @FXML
    private void handleClear() {
        txtNombre.clear();
        txtApellido.clear();
        txtEmail.clear();
        socioSeleccionado = null;
        tableSocios.getSelectionModel().clearSelection();
    }

    private void prepararEdicion(Socio socio) {
        socioSeleccionado = socio;
        txtNombre.setText(socio.getNombre());
        txtApellido.setText(socio.getApellido());
        txtEmail.setText(socio.getEmail());
    }

    private boolean validarCampos() {
        return !txtNombre.getText().isEmpty() && !txtEmail.getText().isEmpty();
    }
}
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
import model.Libro;
import java.time.LocalDate;


public class FXMLDocumentController implements Initializable {

    @FXML private TableView<Libro> tableLibros;
    @FXML private TableColumn<Libro, Integer> colId; 
    @FXML private TableColumn<Libro, String> colTitulo;
    @FXML private TableColumn<Libro, String> colAutor;
    @FXML private TableColumn<Libro, String> colCategoria;
    @FXML private TableColumn<Libro, Integer> colStock;
    @FXML private TableColumn<Libro, LocalDate> colFechaRegistro; // Nueva columna

    @FXML private TextField txtSearch;
    @FXML private TextField txtTitulo;
    @FXML private TextField txtAutor;
    @FXML private ComboBox<String> comboCategoria;
    @FXML private DatePicker dateRegistro; // Sincronizado con fx:id="dateRegistro"
    @FXML private Label lblStatus;
    
    @FXML private Button btnActualizar;
    @FXML private Button btnEliminar;

    private ObservableList<Libro> listaLibros = FXCollections.observableArrayList();
    private Libro libroSeleccionado; 

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Configuración de columnas (incluyendo la fecha)
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitulo.setCellValueFactory(new PropertyValueFactory<>("titulo"));
        colAutor.setCellValueFactory(new PropertyValueFactory<>("autor"));
        colCategoria.setCellValueFactory(new PropertyValueFactory<>("categoria"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("stock"));
        colFechaRegistro.setCellValueFactory(new PropertyValueFactory<>("fechaRegistro"));

        //  Configuración de ComboBox
        comboCategoria.setItems(FXCollections.observableArrayList("Novela", "Clásico", "Tecnología", "Historia"));

        //  Cargar datos y filtros
        loadData();
        setupFilter();
        
        //  Listener para selección de fila
        tableLibros.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                prepararEdicion(newVal);
            } else {
                desactivarControles(true);
            }
        });
        
        desactivarControles(true);
    }

    private void loadData() {
        listaLibros.clear();
        String sql = "SELECT * FROM libros";
        try (Connection cn = Conexion.getConexion();
             Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            
            while (rs.next()) {
                // Sincronizamos con el modelo Libro que recibe 6 parámetros
                listaLibros.add(new Libro(
                    rs.getInt("id_libro"), 
                    rs.getString("titulo"),
                    rs.getString("autor"), 
                    rs.getString("categoria"), 
                    rs.getInt("stock"),
                    rs.getDate("fecha_registro").toLocalDate()
                ));
            }
            tableLibros.setItems(listaLibros);
            updateStatus("Inventario sincronizado.", "#64748b");
        } catch (SQLException e) {
            updateStatus("Error de BD: " + e.getMessage(), "#ef4444");
        }
    }

    private void setupFilter() {
        FilteredList<Libro> filteredData = new FilteredList<>(listaLibros, p -> true);
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(libro -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String lowerCaseFilter = newValue.toLowerCase();
                return libro.getTitulo().toLowerCase().contains(lowerCaseFilter) || 
                       libro.getAutor().toLowerCase().contains(lowerCaseFilter);
            });
        });
        tableLibros.setItems(filteredData);
    }

    @FXML
    private void handleSave() {
        if (validarCampos()) {
            String sql = "INSERT INTO libros (titulo, autor, categoria, stock, fecha_registro) VALUES (?, ?, ?, 1, ?)";
            try (Connection cn = Conexion.getConexion();
                 PreparedStatement ps = cn.prepareStatement(sql)) {
                ps.setString(1, txtTitulo.getText());
                ps.setString(2, txtAutor.getText());
                ps.setString(3, comboCategoria.getValue());
                ps.setDate(4, Date.valueOf(dateRegistro.getValue())); // Capturamos la fecha
                
                ps.executeUpdate();
                loadData();
                handleClear();
                updateStatus("¡Libro guardado con éxito!", "#10b981");
            } catch (SQLException e) {
                updateStatus("Error al guardar registro.", "#ef4444");
            }
        }
    }

    @FXML
    private void handleUpdate() {
        if (libroSeleccionado != null && validarCampos()) {
            String sql = "UPDATE libros SET titulo=?, autor=?, categoria=?, fecha_registro=? WHERE id_libro=?";
            try (Connection cn = Conexion.getConexion();
                 PreparedStatement ps = cn.prepareStatement(sql)) {
                ps.setString(1, txtTitulo.getText());
                ps.setString(2, txtAutor.getText());
                ps.setString(3, comboCategoria.getValue());
                ps.setDate(4, Date.valueOf(dateRegistro.getValue()));
                ps.setInt(5, libroSeleccionado.getId());
                
                ps.executeUpdate();
                loadData();
                handleClear();
                updateStatus("Libro actualizado correctamente.", "#f59e0b");
            } catch (SQLException e) {
                updateStatus("Error al actualizar datos.", "#ef4444");
            }
        }
    }

    @FXML
    private void handleDelete() {
        if (libroSeleccionado == null) return;
        
        // (Aquí iría tu validación de isLibroPrestado que ya tenías)
        
        Alert alertConfirm = new Alert(Alert.AlertType.CONFIRMATION);
        alertConfirm.setTitle("Confirmar Operación");
        alertConfirm.setContentText("¿Desea eliminar '" + libroSeleccionado.getTitulo() + "'?");

        if (alertConfirm.showAndWait().get() == ButtonType.OK) {
            String sql = "DELETE FROM libros WHERE id_libro = ?";
            try (Connection cn = Conexion.getConexion();
                 PreparedStatement ps = cn.prepareStatement(sql)) {
                ps.setInt(1, libroSeleccionado.getId());
                ps.executeUpdate();
                loadData();
                handleClear();
                updateStatus("Libro eliminado.", "#ef4444");
            } catch (SQLException e) {
                updateStatus("Error SQL al borrar.", "#ef4444");
            }
        }
    }

    @FXML
    private void handleClear() {
        txtTitulo.clear();
        txtAutor.clear();
        comboCategoria.setValue(null);
        dateRegistro.setValue(null); // Limpiamos el DatePicker
        txtSearch.clear();
        libroSeleccionado = null;
        tableLibros.getSelectionModel().clearSelection();
        desactivarControles(true);
        updateStatus("Esperando acción...", "#64748b");
    }

    private void prepararEdicion(Libro libro) {
        libroSeleccionado = libro;
        txtTitulo.setText(libro.getTitulo());
        txtAutor.setText(libro.getAutor());
        comboCategoria.setValue(libro.getCategoria());
        dateRegistro.setValue(libro.getFechaRegistro()); // Cargamos la fecha en el DatePicker
        desactivarControles(false);
    }

    private void desactivarControles(boolean disable) {
        if (btnActualizar != null) btnActualizar.setDisable(disable);
        if (btnEliminar != null) btnEliminar.setDisable(disable);
    }

    private boolean validarCampos() {
        if (txtTitulo.getText().isEmpty() || txtAutor.getText().isEmpty() || 
            comboCategoria.getValue() == null || dateRegistro.getValue() == null) {
            updateStatus("Error: Complete todos los campos, incluida la fecha.", "#ef4444");
            return false;
        }
        return true;
    }

    private void updateStatus(String mensaje, String colorHex) {
        lblStatus.setText(mensaje);
        lblStatus.setStyle("-fx-text-fill: " + colorHex + "; -fx-font-weight: bold;");
    }
}
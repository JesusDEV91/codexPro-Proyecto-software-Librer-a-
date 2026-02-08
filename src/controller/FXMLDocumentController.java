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


public class FXMLDocumentController implements Initializable {


    @FXML private TableView<Libro> tableLibros;
    @FXML private TableColumn<Libro, Integer> colId; 
    @FXML private TableColumn<Libro, String> colTitulo;
    @FXML private TableColumn<Libro, String> colAutor;
    @FXML private TableColumn<Libro, String> colCategoria;
    @FXML private TableColumn<Libro, Integer> colStock;

    @FXML private TextField txtSearch;
    @FXML private TextField txtTitulo;
    @FXML private TextField txtAutor;
    @FXML private ComboBox<String> comboCategoria;
    @FXML private DatePicker datePrestamo;
    @FXML private Label lblStatus;
    
    
    @FXML private Button btnActualizar;
    @FXML private Button btnEliminar;

    private ObservableList<Libro> listaLibros = FXCollections.observableArrayList();
    private Libro libroSeleccionado; 

    @Override
    public void initialize(URL location, ResourceBundle resources) {
      
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitulo.setCellValueFactory(new PropertyValueFactory<>("titulo"));
        colAutor.setCellValueFactory(new PropertyValueFactory<>("autor"));
        colCategoria.setCellValueFactory(new PropertyValueFactory<>("categoria"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("stock"));

        
        comboCategoria.setItems(FXCollections.observableArrayList("Novela", "Clásico", "Tecnología", "Historia"));

     
        loadData();
        setupFilter();
        
        
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
                listaLibros.add(new Libro(
                    rs.getInt("id_libro"), rs.getString("titulo"),
                    rs.getString("autor"), rs.getString("categoria"), rs.getInt("stock")
                ));
            }
            tableLibros.setItems(listaLibros);
            updateStatus("Catálogo actualizado correctamente.", "#64748b");
        } catch (SQLException e) {
            updateStatus("Error de base de datos: " + e.getMessage(), "#ef4444");
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
            String sql = "INSERT INTO libros (titulo, autor, categoria, stock) VALUES (?, ?, ?, 1)";
            try (Connection cn = Conexion.getConexion();
                 PreparedStatement ps = cn.prepareStatement(sql)) {
                ps.setString(1, txtTitulo.getText());
                ps.setString(2, txtAutor.getText());
                ps.setString(3, comboCategoria.getValue());
                ps.executeUpdate();
                
                loadData();
                handleClear();
                updateStatus("¡Libro registrado con éxito!", "#10b981");
            } catch (SQLException e) {
                updateStatus("Error al guardar registro.", "#ef4444");
            }
        }
    }

    @FXML
    private void handleUpdate() {
        if (libroSeleccionado != null && validarCampos()) {
            String sql = "UPDATE libros SET titulo=?, autor=?, categoria=? WHERE id_libro=?";
            try (Connection cn = Conexion.getConexion();
                 PreparedStatement ps = cn.prepareStatement(sql)) {
                ps.setString(1, txtTitulo.getText());
                ps.setString(2, txtAutor.getText());
                ps.setString(3, comboCategoria.getValue());
                ps.setInt(4, libroSeleccionado.getId());
                ps.executeUpdate();
                
                loadData();
                handleClear();
                updateStatus("Registro actualizado correctamente.", "#f59e0b");
            } catch (SQLException e) {
                updateStatus("Error al actualizar información.", "#ef4444");
            }
        }
    }

    @FXML
    private void handleDelete() {
        if (libroSeleccionado == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar Eliminación");
        alert.setHeaderText(null);
        alert.setContentText("¿Está seguro de eliminar '" + libroSeleccionado.getTitulo() + "' del inventario?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String sql = "DELETE FROM libros WHERE id_libro = ?";
            try (Connection cn = Conexion.getConexion();
                 PreparedStatement ps = cn.prepareStatement(sql)) {
                ps.setInt(1, libroSeleccionado.getId());
                ps.executeUpdate();
                
                loadData();
                handleClear();
                updateStatus("Libro eliminado del sistema.", "#ef4444");
            } catch (SQLException e) {
                updateStatus("No se puede eliminar: el libro tiene préstamos asociados.", "#f59e0b");
            }
        }
    }

    @FXML
    private void handleClear() {
        txtTitulo.clear();
        txtAutor.clear();
        comboCategoria.setValue(null);
        if (datePrestamo != null) datePrestamo.setValue(null);
        txtSearch.clear();
        libroSeleccionado = null;
        tableLibros.getSelectionModel().clearSelection();
        desactivarControles(true);
        updateStatus("Listo para gestionar el inventario.", "#64748b");
    }

    private void prepararEdicion(Libro libro) {
        libroSeleccionado = libro;
        txtTitulo.setText(libro.getTitulo());
        txtAutor.setText(libro.getAutor());
        comboCategoria.setValue(libro.getCategoria());
        desactivarControles(false);
    }

    private void desactivarControles(boolean disable) {
        if (btnActualizar != null) btnActualizar.setDisable(disable);
        if (btnEliminar != null) btnEliminar.setDisable(disable);
    }

    private boolean validarCampos() {
        if (txtTitulo.getText().isEmpty() || txtAutor.getText().isEmpty() || comboCategoria.getValue() == null) {
            updateStatus("Error: Complete todos los campos requeridos.", "#ef4444");
            return false;
        }
        return true;
    }

    private void updateStatus(String mensaje, String colorHex) {
        lblStatus.setText(mensaje);
        lblStatus.setStyle("-fx-text-fill: " + colorHex + "; -fx-font-weight: bold;");
    }
}
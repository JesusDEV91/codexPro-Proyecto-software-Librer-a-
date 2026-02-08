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
import model.Prestamo;
import java.time.LocalDate;


public class PrestamosController implements Initializable {

  
    @FXML private TableView<Prestamo> tablePrestamos;
    @FXML private TableColumn<Prestamo, Integer> colId;
    @FXML private TableColumn<Prestamo, String> colLibro;
    @FXML private TableColumn<Prestamo, String> colSocio;
    @FXML private TableColumn<Prestamo, LocalDate> colFechaSalida;
    @FXML private TableColumn<Prestamo, LocalDate> colFechaDev;

    @FXML private TextField txtSearchPrestamo;
    @FXML private ComboBox<String> comboLibros;
    @FXML private ComboBox<String> comboSocios;
    @FXML private DatePicker dpDevolucion;
    @FXML private Label lblStatus;
    @FXML private Button btnEliminar;

    private ObservableList<Prestamo> listaPrestamos = FXCollections.observableArrayList();
    private Prestamo prestamoSeleccionado;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
  
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colLibro.setCellValueFactory(new PropertyValueFactory<>("tituloLibro"));
        colSocio.setCellValueFactory(new PropertyValueFactory<>("nombreSocio"));
        colFechaSalida.setCellValueFactory(new PropertyValueFactory<>("fechaSalida"));
        colFechaDev.setCellValueFactory(new PropertyValueFactory<>("fechaDevolucion"));

       
        loadData();
        cargarCombos();
        
        
        setupFilter();

      
        tablePrestamos.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            prestamoSeleccionado = newVal;
            if (btnEliminar != null) {
                btnEliminar.setDisable(newVal == null);
            }
        });
        
        if (btnEliminar != null) btnEliminar.setDisable(true);
    }

    private void loadData() {
        listaPrestamos.clear();
        String sql = "SELECT p.id_prestamo, l.titulo, CONCAT(m.nombre, ' ', m.apellido) as socio, p.fecha_salida, p.fecha_devolucion " +
                     "FROM prestamos p " +
                     "LEFT JOIN libros l ON p.id_libro = l.id_libro " +
                     "LEFT JOIN miembros m ON p.id_miembro = m.id_miembro";
        
        try (Connection cn = Conexion.getConexion();
             Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            
            while (rs.next()) {
                listaPrestamos.add(new Prestamo(
                    rs.getInt(1), 0, 0,
                    rs.getString(2) != null ? rs.getString(2) : "N/A", 
                    rs.getString(3) != null ? rs.getString(3) : "N/A",
                    rs.getDate(4).toLocalDate(), 
                    rs.getDate(5).toLocalDate()
                ));
            }
            tablePrestamos.setItems(listaPrestamos);
        } catch (SQLException e) {
            updateStatus("Error al cargar préstamos: " + e.getMessage(), "#ef4444");
        }
    }

    private void setupFilter() {
        FilteredList<Prestamo> filteredData = new FilteredList<>(listaPrestamos, p -> true);
        
        txtSearchPrestamo.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(prestamo -> {
                if (newValue == null || newValue.isEmpty()) return true;
                
                String lowerCaseFilter = newValue.toLowerCase();
                
               
                if (prestamo.getTituloLibro().toLowerCase().contains(lowerCaseFilter)) return true;
                if (prestamo.getNombreSocio().toLowerCase().contains(lowerCaseFilter)) return true;
                
                return false;
            });
        });
        
        tablePrestamos.setItems(filteredData);
    }

    @FXML
    public void cargarCombos() {
        ObservableList<String> libros = FXCollections.observableArrayList();
        ObservableList<String> socios = FXCollections.observableArrayList();
        
        try (Connection cn = Conexion.getConexion()) {
         
            ResultSet rsL = cn.createStatement().executeQuery("SELECT titulo FROM libros WHERE stock > 0");
            while (rsL.next()) libros.add(rsL.getString("titulo"));
            comboLibros.setItems(libros);

           
            ResultSet rsM = cn.createStatement().executeQuery("SELECT CONCAT(nombre, ' ', apellido) as full FROM miembros");
            while (rsM.next()) socios.add(rsM.getString("full"));
            comboSocios.setItems(socios);

        } catch (SQLException e) {
            updateStatus("Error al sincronizar catálogos.", "#ef4444");
        }
    }

    @FXML
    private void handleSave() {
        if (comboLibros.getValue() == null || comboSocios.getValue() == null || dpDevolucion.getValue() == null) {
            updateStatus("Error: Por favor complete todos los campos de la transacción.", "#ef4444");
            return;
        }

        String sql = "INSERT INTO prestamos (id_libro, id_miembro, fecha_salida, fecha_devolucion) " +
                     "VALUES (" +
                     "(SELECT id_libro FROM libros WHERE titulo = ? LIMIT 1), " +
                     "(SELECT id_miembro FROM miembros WHERE CONCAT(nombre, ' ', apellido) = ? LIMIT 1), " +
                     "?, ?)";

        try (Connection cn = Conexion.getConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            
            ps.setString(1, comboLibros.getValue());
            ps.setString(2, comboSocios.getValue());
            ps.setDate(3, Date.valueOf(LocalDate.now()));
            ps.setDate(4, Date.valueOf(dpDevolucion.getValue()));
            
            int rows = ps.executeUpdate();
            if (rows > 0) {
                loadData();
                handleClear();
                updateStatus("¡Préstamo registrado exitosamente!", "#10b981");
            }
            
        } catch (SQLException e) {
            updateStatus("Error de base de datos: " + e.getMessage(), "#ef4444");
        }
    }

    @FXML
    private void handleDelete() {
        if (prestamoSeleccionado == null) {
            updateStatus("Seleccione un registro de la tabla para eliminar.", "#f59e0b");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar Eliminación");
        alert.setHeaderText(null);
        alert.setContentText("¿Está seguro de eliminar el préstamo de '" + prestamoSeleccionado.getTituloLibro() + "'?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String sql = "DELETE FROM prestamos WHERE id_prestamo = ?";
            try (Connection cn = Conexion.getConexion();
                 PreparedStatement ps = cn.prepareStatement(sql)) {
                ps.setInt(1, prestamoSeleccionado.getId());
                ps.executeUpdate();
                
                loadData();
                handleClear();
                updateStatus("Registro eliminado de la base de datos.", "#ef4444");
            } catch (SQLException e) {
                updateStatus("Error al eliminar registro.", "#ef4444");
            }
        }
    }

    @FXML
    private void handleClear() {
        comboLibros.setValue(null);
        comboSocios.setValue(null);
        dpDevolucion.setValue(null);
        txtSearchPrestamo.clear();
        tablePrestamos.getSelectionModel().clearSelection();
        prestamoSeleccionado = null;
        updateStatus("Listo para registrar nuevos préstamos.", "#64748b");
        cargarCombos();
    }

    private void updateStatus(String mensaje, String colorHex) {
        lblStatus.setText(mensaje);
        lblStatus.setStyle("-fx-text-fill: " + colorHex + "; -fx-font-weight: bold;");
    }
}
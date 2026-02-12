package controller;

import java.net.URL;
import java.sql.*;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
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
    @FXML private TableColumn<Prestamo, String> colEstado;

    @FXML private ComboBox<String> comboLibros;
    @FXML private ComboBox<String> comboSocios;
    @FXML private DatePicker dpDevolucion;
    @FXML private TextField txtSearchPrestamo;
    @FXML private Label lblStatus;
    
    @FXML private Button btnEliminar;
    @FXML private Button btnDevolver;

    private ObservableList<Prestamo> listaPrestamos = FXCollections.observableArrayList();
    private Prestamo prestamoSeleccionado;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Vinculación de columnas con el modelo Prestamo (8 campos)
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colLibro.setCellValueFactory(new PropertyValueFactory<>("tituloLibro"));
        colSocio.setCellValueFactory(new PropertyValueFactory<>("nombreSocio"));
        colFechaSalida.setCellValueFactory(new PropertyValueFactory<>("fechaSalida"));
        colFechaDev.setCellValueFactory(new PropertyValueFactory<>("fechaDevolucion"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));

        // Ajuste de redimensionado automático para evitar errores de FXML
        tablePrestamos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Listener de selección para habilitar/deshabilitar botones
        tablePrestamos.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            prestamoSeleccionado = newVal;
            boolean seleccionado = (newVal == null);
            if (btnEliminar != null) btnEliminar.setDisable(seleccionado);
            if (btnDevolver != null) {
                // Solo permitimos devolver si el estado actual es 'Activo'
                btnDevolver.setDisable(seleccionado || newVal.getEstado().equals("Devuelto"));
            }
        });

        loadData();
        cargarCombos();
    }

    private void loadData() {
        listaPrestamos.clear();
        String sql = "SELECT p.id_prestamo, p.id_libro, p.id_miembro, l.titulo, " +
                     "CONCAT(m.nombre, ' ', m.apellido) as socio, " +
                     "p.fecha_salida, p.fecha_devolucion, p.estado " +
                     "FROM prestamos p " +
                     "JOIN libros l ON p.id_libro = l.id_libro " +
                     "JOIN miembros m ON p.id_miembro = m.id_miembro";
        
        try (Connection cn = Conexion.getConexion();
             Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            
            while (rs.next()) {
                listaPrestamos.add(new Prestamo(
                    rs.getInt(1), rs.getInt(2), rs.getInt(3),
                    rs.getString(4), rs.getString(5),
                    rs.getDate(6).toLocalDate(), rs.getDate(7).toLocalDate(),
                    rs.getString(8)
                ));
            }
            tablePrestamos.setItems(listaPrestamos);
        } catch (SQLException e) {
            updateStatus("Error de carga: " + e.getMessage(), "red");
        }
    }

    
    //Método para finalizar el préstamo y devolver el libro al stock.
     
    @FXML
    public void handleFinalizarPrestamo(ActionEvent event) {
        if (prestamoSeleccionado == null) return;

        String sqlUpdatePrestamo = "UPDATE prestamos SET estado = 'Devuelto' WHERE id_prestamo = ?";
        String sqlUpdateStock = "UPDATE libros SET stock = stock + 1 WHERE id_libro = ?";

        try (Connection cn = Conexion.getConexion()) {
            cn.setAutoCommit(false); // Iniciamos transacción

            try (PreparedStatement psP = cn.prepareStatement(sqlUpdatePrestamo);
                 PreparedStatement psL = cn.prepareStatement(sqlUpdateStock)) {
                
                psP.setInt(1, prestamoSeleccionado.getId());
                psP.executeUpdate();

                psL.setInt(1, prestamoSeleccionado.getIdLibro());
                psL.executeUpdate();

                cn.commit();
                loadData();
                updateStatus("Libro devuelto con éxito. Stock actualizado.", "green");
            } catch (SQLException e) {
                cn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            updateStatus("Error al procesar devolución.", "red");
        }
    }

    @FXML
    public void handleSave(ActionEvent event) {
        if (comboLibros.getValue() == null || comboSocios.getValue() == null || dpDevolucion.getValue() == null) {
            updateStatus("Complete todos los campos.", "red");
            return;
        }

        String sqlInsert = "INSERT INTO prestamos (id_libro, id_miembro, fecha_salida, fecha_devolucion, estado) " +
                           "VALUES ((SELECT id_libro FROM libros WHERE titulo=? LIMIT 1), " +
                           "(SELECT id_miembro FROM miembros WHERE CONCAT(nombre,' ',apellido)=? LIMIT 1), ?, ?, 'Activo')";
        
        String sqlStock = "UPDATE libros SET stock = stock - 1 WHERE titulo = ?";

        try (Connection cn = Conexion.getConexion()) {
            cn.setAutoCommit(false);
            try (PreparedStatement psI = cn.prepareStatement(sqlInsert);
                 PreparedStatement psS = cn.prepareStatement(sqlStock)) {
                
                psI.setString(1, comboLibros.getValue());
                psI.setString(2, comboSocios.getValue());
                psI.setDate(3, Date.valueOf(LocalDate.now()));
                psI.setDate(4, Date.valueOf(dpDevolucion.getValue()));
                psI.executeUpdate();

                psS.setString(1, comboLibros.getValue());
                psS.executeUpdate();

                cn.commit();
                loadData();
                handleClear(null);
                updateStatus("Préstamo registrado. Stock reducido.", "green");
            } catch (SQLException e) {
                cn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            updateStatus("Error: " + e.getMessage(), "red");
        }
    }

    @FXML
    public void handleDelete(ActionEvent event) {
        if (prestamoSeleccionado == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar eliminación");
        alert.setContentText("¿Eliminar registro de préstamo?");
        
        if (alert.showAndWait().get() == ButtonType.OK) {
            String sql = "DELETE FROM prestamos WHERE id_prestamo = ?";
            try (Connection cn = Conexion.getConexion();
                 PreparedStatement ps = cn.prepareStatement(sql)) {
                ps.setInt(1, prestamoSeleccionado.getId());
                ps.executeUpdate();
                loadData();
                updateStatus("Registro eliminado.", "blue");
            } catch (SQLException e) {
                updateStatus("Error al borrar.", "red");
            }
        }
    }

    @FXML
    public void handleClear(ActionEvent event) {
        comboLibros.setValue(null);
        comboSocios.setValue(null);
        dpDevolucion.setValue(null);
        tablePrestamos.getSelectionModel().clearSelection();
        cargarCombos();
    }

    private void cargarCombos() {
        ObservableList<String> lb = FXCollections.observableArrayList();
        ObservableList<String> sc = FXCollections.observableArrayList();
        try (Connection cn = Conexion.getConexion()) {
            ResultSet rsL = cn.createStatement().executeQuery("SELECT titulo FROM libros WHERE stock > 0");
            while (rsL.next()) lb.add(rsL.getString(1));
            comboLibros.setItems(lb);
            ResultSet rsM = cn.createStatement().executeQuery("SELECT CONCAT(nombre,' ',apellido) FROM miembros");
            while (rsM.next()) sc.add(rsM.getString(1));
            comboSocios.setItems(sc);
        } catch (SQLException e) {}
    }

    private void updateStatus(String msg, String color) {
        lblStatus.setText(msg);
        lblStatus.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
    }
}
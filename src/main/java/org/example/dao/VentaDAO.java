package org.example.dao;

import org.example.config.Conexion;
import org.example.modelo.Detalle;
import org.example.modelo.ProductoFarmaceutico;
import org.example.modelo.Usuario;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;

public class VentaDAO {

    /**
     * RF06 / Historia 6: registra la salida de "cantidad" unidades de un
     * lote específico. Además de la fila en "venta" (RF06), deja constancia
     * del movimiento de SALIDA en la bitácora ("detalles", Historia 9),
     * enlaza ambas filas mediante "tiene" y "agrega", y descuenta el stock
     * del medicamento.
     *
     * Antes de llamar a este método, el menú ya validó con el propio
     * objeto ProductoFarmaceutico que hay stock suficiente y que, si es un
     * MedicamentoControlado, la cédula del médico es válida
     * (producto.validarRequisitoEspecial(cedulaMedico)).
     */
    public boolean registrarSalida(ProductoFarmaceutico producto, int cantidad,
                                    Usuario usuarioActor, String cedulaMedico) {
        String descripcion = producto.getNombreComercial() + " (Lote " + producto.getNumeroLote() + ")";
        LocalDate hoy = LocalDate.now();
        LocalTime horaActual = LocalTime.now().withNano(0);

        String sqlVenta = "INSERT INTO venta (productos_venta, cantidadventa, fechaventa, horaventa, cedula_medico) " +
                "VALUES (?,?,?,?,?)";

        try (Connection conexion = Conexion.conectar();
             PreparedStatement stm = conexion.prepareStatement(sqlVenta, Statement.RETURN_GENERATED_KEYS)) {

            stm.setString(1, descripcion);
            stm.setInt(2, cantidad);
            stm.setDate(3, Date.valueOf(hoy));
            stm.setTime(4, Time.valueOf(horaActual));
            if (cedulaMedico == null || cedulaMedico.trim().isEmpty()) {
                stm.setNull(5, java.sql.Types.VARCHAR);
            } else {
                stm.setString(5, cedulaMedico);
            }

            int filas = stm.executeUpdate();
            if (filas == 0) {
                return false;
            }

            int idVenta;
            try (ResultSet claves = stm.getGeneratedKeys()) {
                claves.next();
                idVenta = claves.getInt(1);
            }

            int idDetalle = registrarMovimientoSalida(conexion, usuarioActor.getNombre(),
                    producto.getNombreComercial(), cantidad);

            vincularTiene(conexion, idVenta, idDetalle);
            vincularAgrega(conexion, producto.getIdMedicamento(), idDetalle);

            new MedicamentoDAO().actualizarStock(producto.getIdMedicamento(), producto.getStock() - cantidad);

            return true;
        } catch (SQLException err) {
            System.out.println("Error al registrar la salida: " + err.getMessage());
            return false;
        }
    }

    private int registrarMovimientoSalida(Connection conexion, String nombreUsuario, String nombreMedicamento,
                                           int cantidad) throws SQLException {
        String sql = "INSERT INTO detalles (fecha_salida, fecha_entrada, usuario, medicamento, cantidad, tipo_movimiento) " +
                "VALUES (?,?,?,?,?,?)";
        java.sql.Date hoy = Date.valueOf(LocalDate.now());
        try (PreparedStatement stm = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stm.setDate(1, hoy);
            stm.setDate(2, hoy);
            stm.setString(3, nombreUsuario);
            stm.setString(4, nombreMedicamento);
            stm.setInt(5, cantidad);
            stm.setString(6, Detalle.MOVIMIENTO_SALIDA);
            stm.executeUpdate();
            try (ResultSet claves = stm.getGeneratedKeys()) {
                claves.next();
                return claves.getInt(1);
            }
        }
    }

    private void vincularTiene(Connection conexion, int idVenta, int idDetalle) throws SQLException {
        String sql = "INSERT INTO tiene (id_ventas, id_detalles) VALUES (?,?)";
        try (PreparedStatement stm = conexion.prepareStatement(sql)) {
            stm.setInt(1, idVenta);
            stm.setInt(2, idDetalle);
            stm.executeUpdate();
        }
    }

    private void vincularAgrega(Connection conexion, int idMedicamento, int idDetalle) throws SQLException {
        String sql = "INSERT INTO agrega (id_medicamento, id_detalles) VALUES (?,?)";
        try (PreparedStatement stm = conexion.prepareStatement(sql)) {
            stm.setInt(1, idMedicamento);
            stm.setInt(2, idDetalle);
            stm.executeUpdate();
        }
    }
}

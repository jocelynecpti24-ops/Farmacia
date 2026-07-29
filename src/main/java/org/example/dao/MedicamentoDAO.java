package org.example.dao;

import org.example.config.Conexion;
import org.example.modelo.Detalle;
import org.example.modelo.MedicamentoControlado;
import org.example.modelo.MedicamentoLibre;
import org.example.modelo.ProductoFarmaceutico;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class MedicamentoDAO {

    /**
     * RF03 / Historia 3: registra un medicamento nuevo.
     * Además de insertarlo en "medicamento", dado el esquema de la base de
     * datos también deja constancia de:
     *  - "registra": qué usuario dio de alta el medicamento.
     *  - "detalles" + "agrega": el movimiento de ENTRADA en la bitácora
     *    (Historia 9), con la cantidad inicial como stock de entrada.
     */
    public boolean registrarMedicamento(ProductoFarmaceutico producto, int idUsuario, String nombreUsuario) {
        String tipo = (producto instanceof MedicamentoControlado) ? "CONTROLADO" : "LIBRE";
        String sql = "INSERT INTO medicamento (nombre_comercial, nombre_sustancia, presentacion, " +
                "fecha_caducidad, fecha_entrada, numero_lote, concentracion, precio_base, stock, tipo_medicamento) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?)";

        try (Connection conexion = Conexion.conectar();
             PreparedStatement stm = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stm.setString(1, producto.getNombreComercial());
            stm.setString(2, producto.getNombreSustancia());
            stm.setString(3, producto.getPresentacion());
            stm.setDate(4, Date.valueOf(producto.getFechaCaducidadRaw()));
            stm.setDate(5, Date.valueOf(java.time.LocalDate.now()));
            stm.setString(6, producto.getNumeroLote());
            stm.setString(7, producto.getConcentracion());
            stm.setDouble(8, producto.getPrecioBaseCrudo());
            stm.setInt(9, producto.getStock());
            stm.setString(10, tipo);

            int filas = stm.executeUpdate();
            if (filas == 0) {
                return false;
            }

            int idMedicamento;
            try (ResultSet claves = stm.getGeneratedKeys()) {
                claves.next();
                idMedicamento = claves.getInt(1);
            }

            vincularRegistro(conexion, idUsuario, idMedicamento);
            registrarMovimientoEntrada(conexion, nombreUsuario, producto.getNombreComercial(),
                    producto.getStock(), idMedicamento);

            return true;
        } catch (SQLException err) {
            System.out.println("Error al registrar el medicamento: " + err.getMessage());
            return false;
        }
    }

    private void vincularRegistro(Connection conexion, int idUsuario, int idMedicamento) throws SQLException {
        String sql = "INSERT INTO registra (id_usuario, id_medicamento) VALUES (?,?)";
        try (PreparedStatement stm = conexion.prepareStatement(sql)) {
            stm.setInt(1, idUsuario);
            stm.setInt(2, idMedicamento);
            stm.executeUpdate();
        }
    }

    private void registrarMovimientoEntrada(Connection conexion, String nombreUsuario, String nombreMedicamento,
                                             int cantidad, int idMedicamento) throws SQLException {
        String sqlDetalle = "INSERT INTO detalles (fecha_salida, fecha_entrada, usuario, medicamento, cantidad, tipo_movimiento) " +
                "VALUES (?,?,?,?,?,?)";
        int idDetalle;
        java.sql.Date hoy = Date.valueOf(java.time.LocalDate.now());
        try (PreparedStatement stm = conexion.prepareStatement(sqlDetalle, Statement.RETURN_GENERATED_KEYS)) {
            stm.setDate(1, hoy);
            stm.setDate(2, hoy);
            stm.setString(3, nombreUsuario);
            stm.setString(4, nombreMedicamento);
            stm.setInt(5, cantidad);
            stm.setString(6, Detalle.MOVIMIENTO_ENTRADA);
            stm.executeUpdate();
            try (ResultSet claves = stm.getGeneratedKeys()) {
                claves.next();
                idDetalle = claves.getInt(1);
            }
        }

        String sqlAgrega = "INSERT INTO agrega (id_medicamento, id_detalles) VALUES (?,?)";
        try (PreparedStatement stm = conexion.prepareStatement(sqlAgrega)) {
            stm.setInt(1, idMedicamento);
            stm.setInt(2, idDetalle);
            stm.executeUpdate();
        }
    }

    /**
     * RF04 / Historia 4: buscador en tiempo real por nombre comercial,
     * sustancia activa o número de lote, con los lotes ordenados del más
     * próximo a vencer al más lejano.
     */
    public ArrayList<ProductoFarmaceutico> buscar(String texto) {
        ArrayList<ProductoFarmaceutico> resultado = new ArrayList<>();
        String sql = "SELECT * FROM medicamento WHERE nombre_comercial LIKE ? " +
                "OR nombre_sustancia LIKE ? OR numero_lote LIKE ? ORDER BY fecha_caducidad ASC";
        String comodin = "%" + texto + "%";

        try (Connection conexion = Conexion.conectar();
             PreparedStatement stm = conexion.prepareStatement(sql)) {

            stm.setString(1, comodin);
            stm.setString(2, comodin);
            stm.setString(3, comodin);
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                resultado.add(construirProducto(rs));
            }
        } catch (SQLException err) {
            System.out.println("Error al buscar medicamentos: " + err.getMessage());
        }
        return resultado;
    }

    public ArrayList<ProductoFarmaceutico> listarTodos() {
        ArrayList<ProductoFarmaceutico> resultado = new ArrayList<>();
        String sql = "SELECT * FROM medicamento ORDER BY fecha_caducidad ASC";

        try (Connection conexion = Conexion.conectar();
             PreparedStatement stm = conexion.prepareStatement(sql)) {
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                resultado.add(construirProducto(rs));
            }
        } catch (SQLException err) {
            System.out.println("Error al listar medicamentos: " + err.getMessage());
        }
        return resultado;
    }

    public ProductoFarmaceutico buscarPorId(int idMedicamento) {
        String sql = "SELECT * FROM medicamento WHERE idmedicamento = ?";
        try (Connection conexion = Conexion.conectar();
             PreparedStatement stm = conexion.prepareStatement(sql)) {
            stm.setInt(1, idMedicamento);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                return construirProducto(rs);
            }
        } catch (SQLException err) {
            System.out.println("Error al buscar el medicamento: " + err.getMessage());
        }
        return null;
    }

    /**
     * RF06 / Historia 6: descuenta stock luego de una salida.
     */
    public boolean actualizarStock(int idMedicamento, int nuevoStock) {
        String sql = "UPDATE medicamento SET stock = ? WHERE idmedicamento = ?";
        try (Connection conexion = Conexion.conectar();
             PreparedStatement stm = conexion.prepareStatement(sql)) {
            stm.setInt(1, nuevoStock);
            stm.setInt(2, idMedicamento);
            return stm.executeUpdate() > 0;
        } catch (SQLException err) {
            System.out.println("Error al actualizar el stock: " + err.getMessage());
            return false;
        }
    }

    /**
     * Reconstruye la subclase correcta (MedicamentoLibre o
     * MedicamentoControlado) a partir de la columna tipo_medicamento,
     * para conservar el polimorfismo al leer desde la base de datos.
     */
    private ProductoFarmaceutico construirProducto(ResultSet rs) throws SQLException {
        int id = rs.getInt("idmedicamento");
        String nombreComercial = rs.getString("nombre_comercial");
        String nombreSustancia = rs.getString("nombre_sustancia");
        String lote = rs.getString("numero_lote");
        String concentracion = rs.getString("concentracion");
        String presentacion = rs.getString("presentacion");
        java.time.LocalDate fechaCaducidad = rs.getDate("fecha_caducidad").toLocalDate();
        java.time.LocalDate fechaEntrada = rs.getDate("fecha_entrada").toLocalDate();
        double precioBase = rs.getDouble("precio_base");
        int stock = rs.getInt("stock");
        String tipo = rs.getString("tipo_medicamento");

        if ("CONTROLADO".equalsIgnoreCase(tipo)) {
            return new MedicamentoControlado(id, nombreComercial, nombreSustancia, lote, concentracion,
                    presentacion, fechaCaducidad, fechaEntrada, precioBase, stock);
        }
        return new MedicamentoLibre(id, nombreComercial, nombreSustancia, lote, concentracion,
                presentacion, fechaCaducidad, fechaEntrada, precioBase, stock);
    }
}

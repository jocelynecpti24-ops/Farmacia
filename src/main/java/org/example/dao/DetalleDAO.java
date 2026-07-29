package org.example.dao;

import org.example.config.Conexion;
import org.example.modelo.Detalle;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Historia 9: Bitácora de Movimientos del Sistema.
 */
public class DetalleDAO {

    public ArrayList<Detalle> listarTodos() {
        ArrayList<Detalle> movimientos = new ArrayList<>();
        String sql = "SELECT * FROM detalles ORDER BY id_detalles DESC";

        try (Connection conexion = Conexion.conectar();
             PreparedStatement stm = conexion.prepareStatement(sql)) {
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                movimientos.add(construirDetalle(rs));
            }
        } catch (SQLException err) {
            System.out.println("Error al consultar la bitácora: " + err.getMessage());
        }
        return movimientos;
    }

    /**
     * Filtra la bitácora en memoria por usuario, medicamento y/o fecha
     * (cualquiera de los tres puede ir vacío/null para no aplicar ese filtro).
     */
    public ArrayList<Detalle> filtrar(String usuario, String medicamento, String fechaTexto) {
        ArrayList<Detalle> resultado = new ArrayList<>();
        for (Detalle detalle : listarTodos()) {
            boolean coincideUsuario = usuario == null || usuario.trim().isEmpty()
                    || detalle.getUsuario().toUpperCase().contains(usuario.toUpperCase());
            boolean coincideMedicamento = medicamento == null || medicamento.trim().isEmpty()
                    || detalle.getMedicamento().toUpperCase().contains(medicamento.toUpperCase());
            boolean coincideFecha = fechaTexto == null || fechaTexto.trim().isEmpty()
                    || detalle.getFechaEntrada().equals(fechaTexto) || detalle.getFechaSalida().equals(fechaTexto);

            if (coincideUsuario && coincideMedicamento && coincideFecha) {
                resultado.add(detalle);
            }
        }
        return resultado;
    }

    public int contarPorTipo(String tipoMovimiento) {
        String sql = "SELECT COUNT(*) AS total FROM detalles WHERE tipo_movimiento = ?";
        try (Connection conexion = Conexion.conectar();
             PreparedStatement stm = conexion.prepareStatement(sql)) {
            stm.setString(1, tipoMovimiento);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                return rs.getInt("total");
            }
        } catch (SQLException err) {
            System.out.println("Error al contar movimientos: " + err.getMessage());
        }
        return 0;
    }

    private Detalle construirDetalle(ResultSet rs) throws SQLException {
        Detalle detalle = new Detalle();
        detalle.setIdDetalles(rs.getInt("id_detalles"));
        detalle.setFechaSalida(rs.getDate("fecha_salida").toLocalDate());
        detalle.setFechaEntrada(rs.getDate("fecha_entrada").toLocalDate());
        detalle.setUsuario(rs.getString("usuario"));
        detalle.setMedicamento(rs.getString("medicamento"));
        detalle.setCantidad(rs.getInt("cantidad"));
        detalle.setTipoMovimiento(rs.getString("tipo_movimiento"));
        return detalle;
    }
}

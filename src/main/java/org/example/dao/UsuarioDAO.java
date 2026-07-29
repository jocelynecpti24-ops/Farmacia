package org.example.dao;

import org.example.config.Conexion;
import org.example.modelo.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class UsuarioDAO {

    // Id de la venta "placeholder" insertada en el script SQL (ver farmacia.sql).
    // Todo usuario nuevo debe apuntar a una venta porque id_ventas es NOT NULL,
    // y un usuario recién creado aún no ha registrado ninguna salida real.
    private static final int VENTA_PLACEHOLDER = 1;

    /**
     * RF08 / Historia 8: el teléfono no puede repetirse entre usuarios.
     */
    public boolean existeTelefono(String telefono) {
        String sql = "SELECT id_usuario FROM usuario WHERE telefono = ?";
        try (Connection conexion = Conexion.conectar();
             PreparedStatement stm = conexion.prepareStatement(sql)) {
            stm.setString(1, telefono);
            ResultSet rs = stm.executeQuery();
            return rs.next();
        } catch (SQLException err) {
            System.out.println("Error al validar el teléfono: " + err.getMessage());
            return true; // ante la duda, se bloquea la creación por seguridad.
        }
    }

    /**
     * RF08 / Historia 8: solo puede invocarse desde el menú de Administrador.
     * Además de insertar en "usuario", deja constancia en "gestion_personal"
     * de qué administrador dio de alta a ese empleado.
     */
    public boolean crearUsuario(Usuario usuario, int idAdministrador) {
        String sql = "INSERT INTO usuario (nombre, apellidos, contrasenia, telefono, tipo, id_ventas) " +
                "VALUES (?,?,?,?,?,?)";

        try (Connection conexion = Conexion.conectar();
             PreparedStatement stm = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stm.setString(1, usuario.getNombre());
            stm.setString(2, usuario.getApellidos());
            stm.setString(3, usuario.getContrasenia());
            stm.setString(4, usuario.getTelefono());
            stm.setString(5, usuario.getTipo());
            stm.setInt(6, VENTA_PLACEHOLDER);

            int filas = stm.executeUpdate();
            if (filas == 0) {
                return false;
            }

            int idNuevoUsuario;
            try (ResultSet claves = stm.getGeneratedKeys()) {
                claves.next();
                idNuevoUsuario = claves.getInt(1);
            }

            vincularGestionPersonal(conexion, idAdministrador, idNuevoUsuario);
            return true;
        } catch (SQLException err) {
            System.out.println("Error al crear el usuario: " + err.getMessage());
            return false;
        }
    }

    private void vincularGestionPersonal(Connection conexion, int idAdministrador, int idEmpleado) throws SQLException {
        String sql = "INSERT INTO gestion_personal (id_administrador, id_empleados) VALUES (?,?)";
        try (PreparedStatement stm = conexion.prepareStatement(sql)) {
            stm.setInt(1, idAdministrador);
            stm.setInt(2, idEmpleado);
            stm.executeUpdate();
        }
    }

    /**
     * RF01 / RF09 / Historia 1 / Historia 10: autenticación por nombre y
     * contraseña. Devuelve null si cualquiera de los dos es incorrecto, sin
     * indicar cuál, tal como piden los criterios de aceptación.
     */
    public Usuario autenticar(String nombre, String contrasenia) {
        String sql = "SELECT * FROM usuario WHERE nombre = ? AND contrasenia = ?";
        try (Connection conexion = Conexion.conectar();
             PreparedStatement stm = conexion.prepareStatement(sql)) {
            stm.setString(1, nombre);
            stm.setString(2, contrasenia);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                return construirUsuario(rs);
            }
        } catch (SQLException err) {
            System.out.println("Error al autenticar: " + err.getMessage());
        }
        return null;
    }

    /**
     * RF07 / Historia 7: cualquier cambio de contraseña exige la clave del
     * Administrador. Primero se verifica esa clave y luego se actualiza la
     * contraseña del usuario objetivo.
     */
    public boolean cambiarContrasenia(String nombreObjetivo, String nuevaContrasenia,
                                       String nombreAdmin, String claveAdmin) {
        Usuario admin = autenticar(nombreAdmin, claveAdmin);
        if (admin == null || !admin.esAdministrador()) {
            System.out.println("Clave de administrador incorrecta. No se realizó el cambio.");
            return false;
        }

        String sql = "UPDATE usuario SET contrasenia = ? WHERE nombre = ?";
        try (Connection conexion = Conexion.conectar();
             PreparedStatement stm = conexion.prepareStatement(sql)) {
            stm.setString(1, nuevaContrasenia);
            stm.setString(2, nombreObjetivo);
            return stm.executeUpdate() > 0;
        } catch (SQLException err) {
            System.out.println("Error al cambiar la contraseña: " + err.getMessage());
            return false;
        }
    }

    public ArrayList<Usuario> listarUsuarios() {
        ArrayList<Usuario> usuarios = new ArrayList<>();
        String sql = "SELECT * FROM usuario";
        try (Connection conexion = Conexion.conectar();
             PreparedStatement stm = conexion.prepareStatement(sql)) {
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                usuarios.add(construirUsuario(rs));
            }
        } catch (SQLException err) {
            System.out.println("Error al listar usuarios: " + err.getMessage());
        }
        return usuarios;
    }

    private Usuario construirUsuario(ResultSet rs) throws SQLException {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(rs.getInt("id_usuario"));
        usuario.setNombre(rs.getString("nombre"));
        usuario.setApellidos(rs.getString("apellidos"));
        usuario.setContrasenia(rs.getString("contrasenia"));
        usuario.setTelefono(rs.getString("telefono"));
        usuario.setTipo(rs.getString("tipo"));
        usuario.setIdVentas(rs.getInt("id_ventas"));
        return usuario;
    }
}

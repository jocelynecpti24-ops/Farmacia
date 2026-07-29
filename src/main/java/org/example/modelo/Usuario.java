package org.example.modelo;

import java.util.Arrays;
import java.util.List;

public class Usuario {

    public static final String ROL_ADMINISTRADOR = "Administrador";
    public static final String ROL_ENFERMERA = "Enfermera";
    public static final String ROL_PRACTICANTE = "Practicante";

    private static final List<String> ROLES_VALIDOS =
            Arrays.asList(ROL_ADMINISTRADOR, ROL_ENFERMERA, ROL_PRACTICANTE);

    // RF09 / Historia 10: mínimo 8 caracteres, una mayúscula, una minúscula,
    // un número y un carácter especial.
    private static final String REGEX_CONTRASENIA =
            "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$";

    private int idUsuario;
    private String nombre;
    private String apellidos;
    private String contrasenia;
    private String telefono;
    private String tipo;
    private int idVentas;

    public Usuario() {
    }

    public Usuario(int idUsuario, String nombre, String apellidos, String contrasenia,
                    String telefono, String tipo, int idVentas) {
        setIdUsuario(idUsuario);
        setNombre(nombre);
        setApellidos(apellidos);
        setContrasenia(contrasenia);
        setTelefono(telefono);
        setTipo(tipo);
        setIdVentas(idVentas);
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getNombre() {
        return nombre != null ? nombre.toUpperCase() : "";
    }

    public void setNombre(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre es obligatorio.");
        }
        this.nombre = nombre;
    }

    public String getApellidos() {
        return apellidos != null ? apellidos.toUpperCase() : "";
    }

    public void setApellidos(String apellidos) {
        if (apellidos == null || apellidos.trim().isEmpty()) {
            throw new IllegalArgumentException("Los apellidos son obligatorios.");
        }
        this.apellidos = apellidos;
    }

    /**
     * A diferencia de los demás getters, la contraseña se devuelve tal cual
     * (sin poner en mayúsculas ni reformatear), porque UsuarioDAO la necesita
     * exacta para comparar credenciales al iniciar sesión.
     */
    public String getContrasenia() {
        return contrasenia;
    }

    public void setContrasenia(String contrasenia) {
        if (contrasenia == null || !contrasenia.matches(REGEX_CONTRASENIA)) {
            throw new IllegalArgumentException(
                    "La contraseña debe tener mínimo 8 caracteres, una mayúscula, una minúscula, " +
                            "un número y un carácter especial.");
        }
        this.contrasenia = contrasenia;
    }

    public String getTelefono() {
        return telefono != null ? telefono : "";
    }

    public void setTelefono(String telefono) {
        if (telefono == null || !telefono.matches("\\d{7,15}")) {
            throw new IllegalArgumentException("El teléfono debe contener entre 7 y 15 dígitos.");
        }
        this.telefono = telefono;
    }

    public String getTipo() {
        return tipo != null ? tipo : "";
    }

    public void setTipo(String tipo) {
        if (tipo == null || ROLES_VALIDOS.stream().noneMatch(r -> r.equalsIgnoreCase(tipo.trim()))) {
            throw new IllegalArgumentException(
                    "El rol debe ser Administrador, Enfermera o Practicante.");
        }
        this.tipo = ROLES_VALIDOS.stream()
                .filter(r -> r.equalsIgnoreCase(tipo.trim()))
                .findFirst()
                .orElse(tipo);
    }

    public int getIdVentas() {
        return idVentas;
    }

    public void setIdVentas(int idVentas) {
        this.idVentas = idVentas;
    }

    public boolean esAdministrador() {
        return ROL_ADMINISTRADOR.equalsIgnoreCase(tipo);
    }

    @Override
    public String toString() {
        // No se incluye la contraseña por seguridad, aunque exista su getter.
        return "ID: " + getIdUsuario() + " | Nombre: " + getNombre() + " " + getApellidos() + "\n" +
                "Teléfono: " + getTelefono() + " | Rol: " + getTipo();
    }
}

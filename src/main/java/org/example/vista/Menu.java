package org.example.vista;

import org.example.dao.DetalleDAO;
import org.example.dao.MedicamentoDAO;
import org.example.dao.UsuarioDAO;
import org.example.dao.VentaDAO;
import org.example.modelo.Detalle;
import org.example.modelo.MedicamentoControlado;
import org.example.modelo.MedicamentoLibre;
import org.example.modelo.ProductoFarmaceutico;
import org.example.modelo.Usuario;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;

public class Menu {
    static BufferedReader leer = new BufferedReader(new InputStreamReader(System.in));

    static MedicamentoDAO medicamentoDAO = new MedicamentoDAO();
    static UsuarioDAO usuarioDAO = new UsuarioDAO();
    static VentaDAO ventaDAO = new VentaDAO();
    static DetalleDAO detalleDAO = new DetalleDAO();

    // RF07: guarda quién inició sesión para aplicar permisos por rol.
    static Usuario usuarioActual = null;

    static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static void menu() throws IOException {
        boolean salirPrograma = false;
        while (!salirPrograma) {
            if (usuarioActual == null) {
                boolean autenticado = login();
                if (!autenticado) {
                    salirPrograma = true;
                }
            } else {
                salirPrograma = mostrarMenuPrincipal();
            }
        }
        System.out.println("Gracias por usar el sistema de la farmacia.");
    }

    /**
     * RF01 / RF09 / Historia 1 / Historia 10: inicio de sesión con máximo
     * 3 intentos, mostrando siempre el mismo mensaje genérico de error.
     */
    private static boolean login() throws IOException {
        int intentos = 0;
        while (intentos < 3) {
            System.out.println("\n=== INICIO DE SESION - FARMACIA ===");
            System.out.print("Nombre de usuario: ");
            String nombre = leer.readLine();
            System.out.print("Contraseña: ");
            String contrasenia = leer.readLine();

            Usuario encontrado = usuarioDAO.autenticar(nombre, contrasenia);
            if (encontrado != null) {
                usuarioActual = encontrado;
                System.out.println("Bienvenido/a " + usuarioActual.getNombre() + " (" + usuarioActual.getTipo() + ")");
                return true;
            }
            intentos++;
            System.out.println("Usuario o contraseña incorrectos.");
        }
        System.out.println("Demasiados intentos fallidos. Acceso bloqueado temporalmente.");
        return false;
    }

    /**
     * RF02 / RF07: el menú cambia según el rol de usuarioActual.
     * Devuelve true si el usuario eligió salir por completo del programa.
     */
    private static boolean mostrarMenuPrincipal() throws IOException {
        System.out.println("\n=== MENU FARMACIA (" + usuarioActual.getTipo() + ": " + usuarioActual.getNombre() + ") ===");
        System.out.println("1. Buscar medicamentos");
        System.out.println("2. Registrar salida de medicamento");
        System.out.println("3. Cambiar mi contraseña");
        if (usuarioActual.esAdministrador()) {
            System.out.println("4. Registrar medicamento nuevo");
            System.out.println("5. Crear usuario");
            System.out.println("6. Ver bitácora de movimientos");
        }
        System.out.println("7. Cerrar sesión");
        System.out.println("0. Salir del programa");
        System.out.print("Elige una opción: ");

        int opcion;
        try {
            opcion = Integer.parseInt(leer.readLine());
        } catch (NumberFormatException e) {
            System.out.println("Opción inválida.");
            return false;
        }

        switch (opcion) {
            case 1:
                buscarMedicamentos();
                break;
            case 2:
                registrarSalidaMedicamento();
                break;
            case 3:
                cambiarMiContrasenia();
                break;
            case 4:
                if (usuarioActual.esAdministrador()) {
                    registrarMedicamentoNuevo();
                } else {
                    System.out.println("No tienes permisos para esta opción.");
                }
                break;
            case 5:
                if (usuarioActual.esAdministrador()) {
                    crearUsuarioNuevo();
                } else {
                    System.out.println("No tienes permisos para esta opción.");
                }
                break;
            case 6:
                if (usuarioActual.esAdministrador()) {
                    verBitacora();
                } else {
                    System.out.println("No tienes permisos para esta opción.");
                }
                break;
            case 7:
                System.out.println("Sesión finalizada.");
                usuarioActual = null;
                break;
            case 0:
                return true;
            default:
                System.out.println("Opción inválida.");
        }
        return false;
    }

    /**
     * RF04 / Historia 4: búsqueda por nombre comercial, sustancia o lote,
     * mostrando los lotes ordenados del más próximo a vencer al más lejano
     * con su color de semáforo (esto ya lo hace ProductoFarmaceutico.toString()).
     */
    private static void buscarMedicamentos() throws IOException {
        System.out.print("\nTexto a buscar (nombre comercial, sustancia o lote): ");
        String texto = leer.readLine();
        ArrayList<ProductoFarmaceutico> resultados = medicamentoDAO.buscar(texto);
        if (resultados.isEmpty()) {
            System.out.println("No se encontraron medicamentos.");
            return;
        }
        System.out.println("--- Resultados (ordenados del más próximo a vencer) ---");
        for (ProductoFarmaceutico producto : resultados) {
            System.out.println(producto);
        }
    }

    /**
     * RF03 / Historia 3: registro de medicamento nuevo con todos los campos
     * obligatorios. Todo el flujo va envuelto en try-catch (Historia 11)
     * para que un dato inválido no detenga el programa.
     */
    private static void registrarMedicamentoNuevo() throws IOException {
        try {
            System.out.print("Nombre comercial: ");
            String nombreComercial = leer.readLine();
            System.out.print("Sustancia activa: ");
            String sustancia = leer.readLine();
            System.out.print("Número de lote: ");
            String lote = leer.readLine();
            System.out.print("Concentración (ej. 500mg): ");
            String concentracion = leer.readLine();
            System.out.print("Presentación (ej. Tabletas): ");
            String presentacion = leer.readLine();
            System.out.print("Fecha de caducidad (dd/MM/yyyy): ");
            LocalDate fechaCaducidad = LocalDate.parse(leer.readLine(), FORMATO_FECHA);
            System.out.print("Precio base: ");
            double precioBase = Double.parseDouble(leer.readLine());
            System.out.print("Stock inicial: ");
            int stock = Integer.parseInt(leer.readLine());
            System.out.print("Tipo (1 = Venta libre, 2 = Controlado): ");
            int tipo = Integer.parseInt(leer.readLine());

            ProductoFarmaceutico producto;
            if (tipo == 2) {
                producto = new MedicamentoControlado(0, nombreComercial, sustancia, lote, concentracion,
                        presentacion, fechaCaducidad, LocalDate.now(), precioBase, stock);
            } else {
                producto = new MedicamentoLibre(0, nombreComercial, sustancia, lote, concentracion,
                        presentacion, fechaCaducidad, LocalDate.now(), precioBase, stock);
            }

            boolean registrado = medicamentoDAO.registrarMedicamento(
                    producto, usuarioActual.getIdUsuario(), usuarioActual.getNombre());
            if (registrado) {
                System.out.println("Medicamento registrado correctamente. Semáforo: " + producto.obtenerColorSemaforo());
            }
        } catch (IllegalArgumentException | DateTimeParseException e) {
            System.out.println("No se pudo registrar el medicamento: " + e.getMessage());
        }
    }

    /**
     * RF06 / Historia 6: retira "cantidad" unidades de un lote específico.
     * Si el medicamento es controlado, exige y valida la cédula del médico
     * (polimorfismo: ProductoFarmaceutico.validarRequisitoEspecial()).
     */
    private static void registrarSalidaMedicamento() throws IOException {
        try {
            System.out.print("\nTexto a buscar (nombre comercial, sustancia o lote): ");
            String texto = leer.readLine();
            ArrayList<ProductoFarmaceutico> resultados = medicamentoDAO.buscar(texto);
            if (resultados.isEmpty()) {
                System.out.println("No se encontraron medicamentos.");
                return;
            }
            for (ProductoFarmaceutico producto : resultados) {
                System.out.println(producto);
            }

            System.out.print("ID del medicamento (lote) del cual retirar stock: ");
            int id = Integer.parseInt(leer.readLine());
            ProductoFarmaceutico producto = medicamentoDAO.buscarPorId(id);
            if (producto == null) {
                System.out.println("No se encontró un medicamento con ese ID.");
                return;
            }

            System.out.print("Cantidad a retirar: ");
            int cantidad = Integer.parseInt(leer.readLine());
            if (cantidad > producto.getStock()) {
                throw new IllegalArgumentException("Stock insuficiente. Disponible: " + producto.getStock());
            }

            String cedulaMedico = null;
            if (producto instanceof MedicamentoControlado) {
                System.out.print("Este medicamento es controlado. Cédula del médico: ");
                cedulaMedico = leer.readLine();
                if (!producto.validarRequisitoEspecial(cedulaMedico)) {
                    throw new IllegalArgumentException("Cédula médica inválida para un medicamento controlado.");
                }
            }

            boolean registrado = ventaDAO.registrarSalida(producto, cantidad, usuarioActual, cedulaMedico);
            if (registrado) {
                System.out.println("Salida registrada correctamente.");
                System.out.println("Stock disponible actualizado: " + (producto.getStock() - cantidad));
            }
        } catch (IllegalArgumentException e) {
            System.out.println("No se pudo registrar la salida: " + e.getMessage());
        }
    }

    /**
     * RF08 / Historia 8: solo el Administrador puede crear usuarios
     * (mostrarMenuPrincipal ya bloquea esta opción a los demás roles).
     */
    private static void crearUsuarioNuevo() throws IOException {
        try {
            System.out.print("Nombre: ");
            String nombre = leer.readLine();
            System.out.print("Apellidos: ");
            String apellidos = leer.readLine();
            System.out.print("Teléfono: ");
            String telefono = leer.readLine();

            if (usuarioDAO.existeTelefono(telefono)) {
                throw new IllegalArgumentException("Ese teléfono ya está registrado.");
            }

            System.out.print("Rol (1 = Administrador, 2 = Enfermera, 3 = Practicante): ");
            int rolOpcion = Integer.parseInt(leer.readLine());
            String rol;
            switch (rolOpcion) {
                case 1: rol = Usuario.ROL_ADMINISTRADOR; break;
                case 2: rol = Usuario.ROL_ENFERMERA; break;
                case 3: rol = Usuario.ROL_PRACTICANTE; break;
                default: throw new IllegalArgumentException("Rol inválido.");
            }

            System.out.print("Contraseña (min. 8 caracteres, mayúscula, minúscula, número y carácter especial): ");
            String contrasenia = leer.readLine();

            Usuario nuevoUsuario = new Usuario(0, nombre, apellidos, contrasenia, telefono, rol, 0);
            boolean creado = usuarioDAO.crearUsuario(nuevoUsuario, usuarioActual.getIdUsuario());
            if (creado) {
                System.out.println("Usuario creado correctamente.");
            }
        } catch (IllegalArgumentException e) {
            System.out.println("No se pudo crear el usuario: " + e.getMessage());
        }
    }

    /**
     * RF07 / Historia 7: cualquier cambio de contraseña exige la clave del
     * Administrador, sin importar el rol de quien la está cambiando.
     */
    private static void cambiarMiContrasenia() throws IOException {
        try {
            System.out.print("Nueva contraseña (min. 8 caracteres, mayúscula, minúscula, número y carácter especial): ");
            String nuevaContrasenia = leer.readLine();
            // Se reutiliza el propio setter de Usuario para validar el formato
            // antes de tocar la base de datos (objeto desechable, solo para validar).
            new Usuario().setContrasenia(nuevaContrasenia);

            System.out.print("Este cambio requiere autorización. Usuario Administrador: ");
            String nombreAdmin = leer.readLine();
            System.out.print("Contraseña del Administrador: ");
            String claveAdmin = leer.readLine();

            boolean actualizado = usuarioDAO.cambiarContrasenia(
                    usuarioActual.getNombre(), nuevaContrasenia, nombreAdmin, claveAdmin);
            if (actualizado) {
                System.out.println("Contraseña actualizada correctamente.");
            }
        } catch (IllegalArgumentException e) {
            System.out.println("No se pudo cambiar la contraseña: " + e.getMessage());
        }
    }

    /**
     * Historia 9: bitácora de movimientos con buscador y contadores.
     */
    private static void verBitacora() throws IOException {
        System.out.println("\n=== BITÁCORA DE MOVIMIENTOS ===");
        System.out.println("Cada movimiento registrado es una ENTRADA (alta de medicamento) o una SALIDA (retiro de stock).");
        System.out.print("Filtrar por usuario que hizo el movimiento (enter para omitir): ");
        String usuario = leer.readLine();
        System.out.print("Filtrar por nombre del medicamento (enter para omitir): ");
        String medicamento = leer.readLine();
        System.out.print("Filtrar por fecha del movimiento, formato dd/MM/yyyy (enter para omitir): ");
        String fecha = leer.readLine();

        ArrayList<Detalle> movimientos = detalleDAO.filtrar(usuario, medicamento, fecha);

        System.out.println("\n--- Resultados (" + movimientos.size() + " movimiento(s) encontrado(s)) ---");
        if (movimientos.isEmpty()) {
            System.out.println("No hay movimientos que coincidan con los filtros indicados.");
        } else {
            int numero = 1;
            for (Detalle detalle : movimientos) {
                System.out.println(numero + ". " + detalle);
                numero++;
            }
        }

        System.out.println("\n--- Totales generales de la bitácora (sin filtros) ---");
        System.out.println("Total de entradas registradas: " + detalleDAO.contarPorTipo(Detalle.MOVIMIENTO_ENTRADA));
        System.out.println("Total de salidas registradas: " + detalleDAO.contarPorTipo(Detalle.MOVIMIENTO_SALIDA));
    }
}

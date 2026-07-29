package org.example.modelo;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Mapea la tabla "detalles": es la bitácora de movimientos del sistema
 * (Historia 9). Cada fila representa una ENTRADA (alta de medicamento,
 * RF03) o una SALIDA (retiro de stock, RF06).
 */
public class Detalle {

    public static final String MOVIMIENTO_ENTRADA = "ENTRADA";
    public static final String MOVIMIENTO_SALIDA = "SALIDA";

    private int idDetalles;
    private LocalDate fechaSalida;
    private LocalDate fechaEntrada;
    private String usuario;
    private String medicamento;
    private int cantidad;
    private String tipoMovimiento;

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public Detalle() {
    }

    public Detalle(int idDetalles, LocalDate fechaSalida, LocalDate fechaEntrada, String usuario,
                    String medicamento, int cantidad, String tipoMovimiento) {
        setIdDetalles(idDetalles);
        setFechaSalida(fechaSalida);
        setFechaEntrada(fechaEntrada);
        setUsuario(usuario);
        setMedicamento(medicamento);
        setCantidad(cantidad);
        setTipoMovimiento(tipoMovimiento);
    }

    public int getIdDetalles() {
        return idDetalles;
    }

    public void setIdDetalles(int idDetalles) {
        this.idDetalles = idDetalles;
    }

    public String getFechaSalida() {
        return fechaSalida != null ? fechaSalida.format(FORMATO_FECHA) : "";
    }

    public void setFechaSalida(LocalDate fechaSalida) {
        if (fechaSalida == null) {
            throw new IllegalArgumentException("La fecha de salida es obligatoria.");
        }
        this.fechaSalida = fechaSalida;
    }

    public String getFechaEntrada() {
        return fechaEntrada != null ? fechaEntrada.format(FORMATO_FECHA) : "";
    }

    public void setFechaEntrada(LocalDate fechaEntrada) {
        if (fechaEntrada == null) {
            throw new IllegalArgumentException("La fecha de entrada es obligatoria.");
        }
        this.fechaEntrada = fechaEntrada;
    }

    public String getUsuario() {
        return usuario != null ? usuario.toUpperCase() : "";
    }

    public void setUsuario(String usuario) {
        if (usuario == null || usuario.trim().isEmpty()) {
            throw new IllegalArgumentException("El usuario del movimiento es obligatorio.");
        }
        this.usuario = usuario;
    }

    public String getMedicamento() {
        return medicamento != null ? medicamento.toUpperCase() : "";
    }

    public void setMedicamento(String medicamento) {
        if (medicamento == null || medicamento.trim().isEmpty()) {
            throw new IllegalArgumentException("El medicamento del movimiento es obligatorio.");
        }
        this.medicamento = medicamento;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a 0.");
        }
        this.cantidad = cantidad;
    }

    public String getTipoMovimiento() {
        return tipoMovimiento != null ? tipoMovimiento : "";
    }

    public void setTipoMovimiento(String tipoMovimiento) {
        if (!MOVIMIENTO_ENTRADA.equalsIgnoreCase(tipoMovimiento) &&
                !MOVIMIENTO_SALIDA.equalsIgnoreCase(tipoMovimiento)) {
            throw new IllegalArgumentException("El tipo de movimiento debe ser ENTRADA o SALIDA.");
        }
        this.tipoMovimiento = tipoMovimiento.toUpperCase();
    }

    @Override
    public String toString() {
        // La tabla "detalles" exige ambas fechas (NOT NULL), pero en la
        // práctica solo una es relevante según el tipo de movimiento:
        // en una ENTRADA nos importa cuándo entró, en una SALIDA cuándo
        // salió. Mostrar solo esa evita el "Entrada: X | Salida: X"
        // duplicado y confuso que se veía antes.
        boolean esEntrada = MOVIMIENTO_ENTRADA.equals(getTipoMovimiento());
        String etiquetaMovimiento = esEntrada ? "Ingreso de stock" : "Retiro de stock";
        String fecha = esEntrada ? getFechaEntrada() : getFechaSalida();
        // "Cantidad" es siempre la cantidad de ESTE movimiento, no el stock
        // actual del medicamento: en una entrada es el stock inicial con el
        // que se dio de alta; en una salida, lo que se retiró en esa
        // operación. Se etiqueta distinto según el tipo para que no se
        // confunda con "cuánto queda en inventario".
        String etiquetaCantidad = esEntrada ? "Cantidad ingresada" : "Cantidad retirada";

        return "[" + getTipoMovimiento() + " - " + etiquetaMovimiento + "] " +
                "Fecha: " + fecha +
                " | Usuario: " + getUsuario() +
                " | Medicamento: " + getMedicamento() +
                " | " + etiquetaCantidad + ": " + getCantidad();
    }
}

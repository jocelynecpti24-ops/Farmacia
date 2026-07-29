package org.example.modelo;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Mapea la tabla "venta", que en este sistema representa cada
 * salida/despacho de un lote de medicamento (RF06 / Historia 6).
 */
public class Venta {
    private int idVenta;
    private String productosVenta;
    private int cantidadVenta;
    private LocalDate fechaVenta;
    private LocalTime horaVenta;
    private String cedulaMedico;

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    public Venta() {
    }

    public Venta(int idVenta, String productosVenta, int cantidadVenta,
                 LocalDate fechaVenta, LocalTime horaVenta, String cedulaMedico) {
        setIdVenta(idVenta);
        setProductosVenta(productosVenta);
        setCantidadVenta(cantidadVenta);
        setFechaVenta(fechaVenta);
        setHoraVenta(horaVenta);
        setCedulaMedico(cedulaMedico);
    }

    public int getIdVenta() {
        return idVenta;
    }

    public void setIdVenta(int idVenta) {
        this.idVenta = idVenta;
    }

    public String getProductosVenta() {
        return productosVenta != null ? productosVenta : "";
    }

    public void setProductosVenta(String productosVenta) {
        if (productosVenta == null || productosVenta.trim().isEmpty()) {
            throw new IllegalArgumentException("La descripción de la salida es obligatoria.");
        }
        this.productosVenta = productosVenta;
    }

    public int getCantidadVenta() {
        return cantidadVenta;
    }

    public void setCantidadVenta(int cantidadVenta) {
        if (cantidadVenta <= 0) {
            throw new IllegalArgumentException("La cantidad a retirar debe ser mayor a 0.");
        }
        this.cantidadVenta = cantidadVenta;
    }

    public String getFechaVenta() {
        return fechaVenta != null ? fechaVenta.format(FORMATO_FECHA) : "";
    }

    public LocalDate getFechaVentaRaw() {
        return fechaVenta;
    }

    public void setFechaVenta(LocalDate fechaVenta) {
        if (fechaVenta == null) {
            throw new IllegalArgumentException("La fecha de salida es obligatoria.");
        }
        this.fechaVenta = fechaVenta;
    }

    public String getHoraVenta() {
        return horaVenta != null ? horaVenta.format(FORMATO_HORA) : "";
    }

    public LocalTime getHoraVentaRaw() {
        return horaVenta;
    }

    public void setHoraVenta(LocalTime horaVenta) {
        if (horaVenta == null) {
            throw new IllegalArgumentException("La hora de salida es obligatoria.");
        }
        this.horaVenta = horaVenta;
    }

    /**
     * Solo se exige (y valida) cuando el medicamento retirado es un
     * MedicamentoControlado; para uno de venta libre puede ir vacía.
     */
    public String getCedulaMedico() {
        return cedulaMedico != null ? cedulaMedico : "";
    }

    public void setCedulaMedico(String cedulaMedico) {
        this.cedulaMedico = cedulaMedico;
    }

    @Override
    public String toString() {
        return "Venta #" + getIdVenta() + " | " + getProductosVenta() +
                " | Cantidad: " + getCantidadVenta() +
                " | Fecha: " + getFechaVenta() + " " + getHoraVenta() +
                (getCedulaMedico().isEmpty() ? "" : " | Cédula médico: " + getCedulaMedico());
    }
}

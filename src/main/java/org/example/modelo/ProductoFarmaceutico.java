package org.example.modelo;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public abstract class ProductoFarmaceutico {
    private int idMedicamento;
    private String nombreComercial;
    private String nombreSustancia;
    private String numeroLote;
    private String concentracion;
    private String presentacion;
    private LocalDate fechaCaducidad;
    private LocalDate fechaEntrada;
    private double precioBase;
    private int stock;

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public ProductoFarmaceutico() {
    }

    public ProductoFarmaceutico(int idMedicamento, String nombreComercial, String nombreSustancia,
                                 String numeroLote, String concentracion, String presentacion,
                                 LocalDate fechaCaducidad, LocalDate fechaEntrada,
                                 double precioBase, int stock) {
        setIdMedicamento(idMedicamento);
        setNombreComercial(nombreComercial);
        setNombreSustancia(nombreSustancia);
        setNumeroLote(numeroLote);
        setConcentracion(concentracion);
        setPresentacion(presentacion);
        setFechaCaducidad(fechaCaducidad);
        setFechaEntrada(fechaEntrada);
        setPrecioBase(precioBase);
        setStock(stock);
    }

    public int getIdMedicamento() {
        return idMedicamento;
    }

    public void setIdMedicamento(int idMedicamento) {
        this.idMedicamento = idMedicamento;
    }

    public String getNombreComercial() {
        return nombreComercial != null ? nombreComercial.toUpperCase() : "";
    }

    public void setNombreComercial(String nombreComercial) {
        if (nombreComercial == null || nombreComercial.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre comercial es obligatorio.");
        }
        this.nombreComercial = nombreComercial;
    }

    public String getNombreSustancia() {
        return nombreSustancia != null ? nombreSustancia.toUpperCase() : "";
    }

    public void setNombreSustancia(String nombreSustancia) {
        if (nombreSustancia == null || nombreSustancia.trim().isEmpty()) {
            throw new IllegalArgumentException("La sustancia activa es obligatoria.");
        }
        this.nombreSustancia = nombreSustancia;
    }

    public String getNumeroLote() {
        return numeroLote != null ? numeroLote.toUpperCase() : "";
    }

    public void setNumeroLote(String numeroLote) {
        if (numeroLote == null || numeroLote.trim().isEmpty()) {
            throw new IllegalArgumentException("El número de lote es obligatorio.");
        }
        this.numeroLote = numeroLote;
    }

    public String getConcentracion() {
        return concentracion != null ? concentracion : "";
    }

    public void setConcentracion(String concentracion) {
        if (concentracion == null || concentracion.trim().isEmpty()) {
            throw new IllegalArgumentException("La concentración es obligatoria.");
        }
        this.concentracion = concentracion;
    }

    public String getPresentacion() {
        return presentacion != null ? presentacion : "";
    }

    public void setPresentacion(String presentacion) {
        if (presentacion == null || presentacion.trim().isEmpty()) {
            throw new IllegalArgumentException("La presentación es obligatoria.");
        }
        this.presentacion = presentacion;
    }

    public String getFechaCaducidad() {
        return fechaCaducidad != null ? fechaCaducidad.format(FORMATO_FECHA) : "";
    }

    public LocalDate getFechaCaducidadRaw() {
        return fechaCaducidad;
    }

    public void setFechaCaducidad(LocalDate fechaCaducidad) {
        if (fechaCaducidad == null) {
            throw new IllegalArgumentException("La fecha de caducidad es obligatoria.");
        }
        this.fechaCaducidad = fechaCaducidad;
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

    public String getPrecioBase() {
        return String.format("$%.2f", precioBase);
    }

    public void setPrecioBase(double precioBase) {
        if (precioBase <= 0) {
            throw new IllegalArgumentException("El precio base debe ser mayor a 0.");
        }
        this.precioBase = precioBase;
    }

    /**
     * Acceso protegido al precio base numérico, únicamente para que las
     * subclases puedan calcular el precio final (getPrecioBase() público
     * devuelve un String con formato de moneda, no un número).
     */
    protected double precioBaseNumerico() {
        return precioBase;
    }

    /**
     * Acceso público al valor numérico crudo del precio base, necesario
     * para que MedicamentoDAO pueda persistirlo en la columna DECIMAL de
     * la base de datos (getPrecioBase() devuelve un String con formato
     * de moneda, no apto para guardarse como número).
     */
    public double getPrecioBaseCrudo() {
        return precioBase;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        if (stock < 0) {
            throw new IllegalArgumentException("El stock no puede ser negativo.");
        }
        this.stock = stock;
    }

    /**
     * RF05 / Historia 5: semáforo de caducidad.
     * Verde: más de 12 meses para caducar.
     * Amarillo: entre 6 y 12 meses.
     * Rojo: menos de 6 meses o ya caducado.
     */
    public String obtenerColorSemaforo() {
        if (fechaCaducidad == null) {
            return "SIN FECHA";
        }
        long mesesRestantes = ChronoUnit.MONTHS.between(LocalDate.now(), fechaCaducidad);
        if (mesesRestantes < 0) {
            return "ROJO (CADUCADO)";
        } else if (mesesRestantes < 6) {
            return "ROJO";
        } else if (mesesRestantes <= 12) {
            return "AMARILLO";
        } else {
            return "VERDE";
        }
    }

    public abstract String mostrarTipoProducto();

    public abstract String obtenerNormativa();

    public abstract boolean validarRequisitoEspecial(String dato);

    @Override
    public String toString() {
        return "Tipo: " + mostrarTipoProducto() + "\n" +
                "ID: " + getIdMedicamento() + " | Nombre comercial: " + getNombreComercial() + "\n" +
                "Sustancia activa: " + getNombreSustancia() + " | Lote: " + getNumeroLote() + "\n" +
                "Concentración: " + getConcentracion() + " | Presentación: " + getPresentacion() + "\n" +
                "Fecha de caducidad: " + getFechaCaducidad() + " | Semáforo: " + obtenerColorSemaforo() + "\n" +
                "Precio base: " + getPrecioBase() + " | Stock disponible: " + getStock();
    }
}

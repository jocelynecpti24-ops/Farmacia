package org.example.modelo;

import java.time.LocalDate;

public class MedicamentoControlado extends ProductoFarmaceutico implements Vendible {

    private static final double RECARGO_REGULATORIO = 0.15;

    public MedicamentoControlado() {
    }

    public MedicamentoControlado(int idMedicamento, String nombreComercial, String nombreSustancia,
                                  String numeroLote, String concentracion, String presentacion,
                                  LocalDate fechaCaducidad, LocalDate fechaEntrada,
                                  double precioBase, int stock) {
        super(idMedicamento, nombreComercial, nombreSustancia, numeroLote, concentracion,
                presentacion, fechaCaducidad, fechaEntrada, precioBase, stock);
    }

    @Override
    public String mostrarTipoProducto() {
        return "Medicamento Controlado";
    }

    @Override
    public String obtenerNormativa() {
        return "Medicamento controlado: requiere receta y cédula profesional válida del médico.";
    }

    @Override
    public boolean validarRequisitoEspecial(String dato) {
        // "dato" es la cédula del médico que prescribe. Debe existir y
        // contener únicamente entre 4 y 10 dígitos.
        return dato != null && dato.matches("\\d{4,10}");
    }

    @Override
    public double calcularPrecioFinal() {
        // Recargo del 15% por el manejo administrativo/regulatorio adicional
        // que exige un medicamento controlado (polimorfismo respecto a MedicamentoLibre).
        return precioBaseNumerico() * (1 + RECARGO_REGULATORIO);
    }

    @Override
    public String toString() {
        return super.toString() + "\n" +
                "Precio final de venta (incluye recargo regulatorio 15%): " +
                String.format("$%.2f", calcularPrecioFinal()) + "\n" +
                "---------------------------------------------------------------------------------------";
    }
}

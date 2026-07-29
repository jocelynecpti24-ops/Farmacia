package org.example.modelo;

import java.time.LocalDate;

public class MedicamentoLibre extends ProductoFarmaceutico implements Vendible {

    public MedicamentoLibre() {
    }

    public MedicamentoLibre(int idMedicamento, String nombreComercial, String nombreSustancia,
                             String numeroLote, String concentracion, String presentacion,
                             LocalDate fechaCaducidad, LocalDate fechaEntrada,
                             double precioBase, int stock) {
        super(idMedicamento, nombreComercial, nombreSustancia, numeroLote, concentracion,
                presentacion, fechaCaducidad, fechaEntrada, precioBase, stock);
    }

    @Override
    public String mostrarTipoProducto() {
        return "Medicamento de Venta Libre";
    }

    @Override
    public String obtenerNormativa() {
        return "Venta libre: no requiere receta médica ni validación adicional.";
    }

    @Override
    public boolean validarRequisitoEspecial(String dato) {
        // Un medicamento libre no exige ningún requisito adicional para su venta.
        return true;
    }

    @Override
    public double calcularPrecioFinal() {
        return precioBaseNumerico();
    }

    @Override
    public String toString() {
        return super.toString() + "\n" +
                "Precio final de venta: " + String.format("$%.2f", calcularPrecioFinal()) + "\n" +
                "---------------------------------------------------------------------------------------";
    }
}

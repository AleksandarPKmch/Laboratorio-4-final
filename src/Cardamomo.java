import java.math.BigDecimal;
import java.time.LocalDate;

public class Cardamomo extends Producto {
    private final BigDecimal porcentajeVerde;

    public Cardamomo(BigDecimal porcentajeVerde) {
        validarPorcentaje(porcentajeVerde, BigDecimal.ZERO, new BigDecimal("100"), "El grano verde");
        this.porcentajeVerde = porcentajeVerde;
    }

    public BigDecimal getPorcentajeVerde() {
        return porcentajeVerde;
    }

    @Override
    public String getCategoria() {
        return "Cardamomo";
    }

    @Override
    public BigDecimal calcularPago(BigDecimal peso, Calidad calidad) {
        BigDecimal pagoBase = monto(peso, calidad, "60", "48", "35");
        return porcentajeVerde.compareTo(new BigDecimal("85")) >= 0
                ? pagoBase.multiply(new BigDecimal("1.10"))
                : pagoBase;
    }

    @Override
    public LocalDate calcularVencimiento(LocalDate fechaIngreso) {
        return fechaIngreso.plusDays(180);
    }

    @Override
    public RequisitoAlmacenamiento getRequisitoAlmacenamiento() {
        return RequisitoAlmacenamiento.SECO;
    }
}

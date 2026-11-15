import java.math.BigDecimal;
import java.time.LocalDate;

public class QuesoFresco extends Producto {
    private final BigDecimal porcentajeGrasa;

    public QuesoFresco(BigDecimal porcentajeGrasa) {
        validarPorcentaje(porcentajeGrasa, new BigDecimal("10"), new BigDecimal("40"), "La grasa del queso");
        this.porcentajeGrasa = porcentajeGrasa;
    }

    public BigDecimal getPorcentajeGrasa() {
        return porcentajeGrasa;
    }

    @Override
    public String getCategoria() {
        return "Queso fresco";
    }

    @Override
    public BigDecimal calcularPago(BigDecimal peso, Calidad calidad) {
        validarCalidad(calidad);
        if (calidad == Calidad.C) {
            throw new IllegalArgumentException("No se acepta queso fresco de calidad C.");
        }
        return monto(peso, calidad, "38", "30", "0").subtract(peso.multiply(new BigDecimal("1.50")));
    }

    @Override
    public LocalDate calcularVencimiento(LocalDate fechaIngreso) {
        return fechaIngreso.plusDays(21);
    }

    @Override
    public RequisitoAlmacenamiento getRequisitoAlmacenamiento() {
        return RequisitoAlmacenamiento.CAMARA_FRIA;
    }
}

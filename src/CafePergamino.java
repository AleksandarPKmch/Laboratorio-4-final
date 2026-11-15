import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class CafePergamino extends Producto {
    private static final BigDecimal HUMEDAD_MINIMA = new BigDecimal("8");
    private static final BigDecimal HUMEDAD_MAXIMA = new BigDecimal("14");
    private final BigDecimal humedad;

    public CafePergamino(BigDecimal humedad) {
        validarPorcentaje(humedad, HUMEDAD_MINIMA, HUMEDAD_MAXIMA, "La humedad del café");
        this.humedad = humedad;
    }

    public BigDecimal getHumedad() {
        return humedad;
    }

    @Override
    public String getCategoria() {
        return "Café en pergamino";
    }

    @Override
    public BigDecimal calcularPago(BigDecimal peso, Calidad calidad) {
        BigDecimal pagoBase = monto(peso, calidad, "12", "9", "6");
        BigDecimal exceso = humedad.subtract(new BigDecimal("12"));
        if (exceso.signum() <= 0) {
            return pagoBase;
        }
        BigDecimal puntosACobrar = exceso.setScale(0, RoundingMode.CEILING);
        BigDecimal descuento = puntosACobrar.multiply(new BigDecimal("0.03"));
        return pagoBase.multiply(BigDecimal.ONE.subtract(descuento));
    }

    @Override
    public LocalDate calcularVencimiento(LocalDate fechaIngreso) {
        return null;
    }

    @Override
    public RequisitoAlmacenamiento getRequisitoAlmacenamiento() {
        return RequisitoAlmacenamiento.SECO;
    }
}

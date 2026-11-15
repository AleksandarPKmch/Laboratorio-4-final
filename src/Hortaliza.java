import java.math.BigDecimal;
import java.time.LocalDate;

public class Hortaliza extends Producto {
    private final int vidaUtilDias;

    public Hortaliza(int vidaUtilDias) {
        if (vidaUtilDias < 3 || vidaUtilDias > 14) {
            throw new IllegalArgumentException("La vida útil de la hortaliza debe estar entre 3 y 14 días.");
        }
        this.vidaUtilDias = vidaUtilDias;
    }

    public int getVidaUtilDias() {
        return vidaUtilDias;
    }

    @Override
    public String getCategoria() {
        return "Hortaliza";
    }

    @Override
    public BigDecimal calcularPago(BigDecimal peso, Calidad calidad) {
        BigDecimal pesoPagable = peso.multiply(new BigDecimal("0.95"));
        return monto(pesoPagable, calidad, "4", "3", "2");
    }

    @Override
    public LocalDate calcularVencimiento(LocalDate fechaIngreso) {
        return fechaIngreso.plusDays(vidaUtilDias);
    }

    @Override
    public RequisitoAlmacenamiento getRequisitoAlmacenamiento() {
        return RequisitoAlmacenamiento.CAMARA_FRIA;
    }
}

import java.math.BigDecimal;
import java.time.LocalDate;

public class CafeOrganico extends CafePergamino {
    private final String certificadora;
    private final LocalDate vigenciaSello;

    public CafeOrganico(BigDecimal humedad, String certificadora, LocalDate vigenciaSello) {
        super(humedad);
        if (certificadora == null || certificadora.isBlank()) {
            throw new IllegalArgumentException("La entidad certificadora es obligatoria.");
        }
        if (vigenciaSello == null) {
            throw new IllegalArgumentException("La fecha de vigencia del sello es obligatoria.");
        }
        this.certificadora = certificadora.trim();
        this.vigenciaSello = vigenciaSello;
    }

    public String getCertificadora() {
        return certificadora;
    }

    public LocalDate getVigenciaSello() {
        return vigenciaSello;
    }

    public void validarSello(LocalDate fechaIngreso) {
        if (vigenciaSello.isBefore(fechaIngreso)) {
            throw new IllegalArgumentException("El sello orgánico debe seguir vigente en la fecha de ingreso.");
        }
    }

    @Override
    public String getCategoria() {
        return "Café orgánico";
    }

    @Override
    public BigDecimal calcularPago(BigDecimal peso, Calidad calidad) {
        return super.calcularPago(peso, calidad).add(peso.multiply(new BigDecimal("0.75")));
    }
}

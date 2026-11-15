import java.math.BigDecimal;
import java.time.LocalDate;

public abstract class Producto {
    public abstract String getCategoria();

    public abstract BigDecimal calcularPago(BigDecimal peso, Calidad calidad);

    public abstract LocalDate calcularVencimiento(LocalDate fechaIngreso);

    public abstract RequisitoAlmacenamiento getRequisitoAlmacenamiento();

    protected static void validarCalidad(Calidad calidad) {
        if (calidad == null) {
            throw new IllegalArgumentException("La calidad es obligatoria.");
        }
    }

    protected static void validarPorcentaje(BigDecimal valor, BigDecimal minimo, BigDecimal maximo, String nombre) {
        if (valor == null || valor.compareTo(minimo) < 0 || valor.compareTo(maximo) > 0) {
            throw new IllegalArgumentException(nombre + " debe estar entre " + minimo + " y " + maximo + ".");
        }
    }

    protected static BigDecimal tarifaPorCalidad(Calidad calidad, String tarifaA, String tarifaB, String tarifaC) {
        validarCalidad(calidad);
        return switch (calidad) {
            case A -> new BigDecimal(tarifaA);
            case B -> new BigDecimal(tarifaB);
            case C -> new BigDecimal(tarifaC);
        };
    }

    protected static BigDecimal monto(BigDecimal peso, Calidad calidad, String tarifaA, String tarifaB, String tarifaC) {
        return peso.multiply(tarifaPorCalidad(calidad, tarifaA, tarifaB, tarifaC));
    }
}

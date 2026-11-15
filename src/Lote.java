import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class Lote {
    private final String codigo;
    private final String productor;
    private final LocalDate fechaIngreso;
    private final BigDecimal pesoRecibido;
    private final Calidad calidad;
    private final Producto producto;
    private final BigDecimal pagoProductor;
    private BigDecimal pesoDisponible;
    private EstadoLote estado;

    public Lote(String codigo, String productor, LocalDate fechaIngreso, BigDecimal pesoRecibido,
                Calidad calidad, Producto producto) {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("El código del lote no puede estar vacío.");
        }
        if (productor == null || productor.isBlank()) {
            throw new IllegalArgumentException("El nombre del productor no puede estar vacío.");
        }
        if (fechaIngreso == null) {
            throw new IllegalArgumentException("La fecha de ingreso es obligatoria.");
        }
        if (pesoRecibido == null || pesoRecibido.signum() <= 0) {
            throw new IllegalArgumentException("El peso recibido debe ser mayor que cero.");
        }
        if (producto == null) {
            throw new IllegalArgumentException("La categoría del lote es obligatoria.");
        }
        if (producto instanceof CafeOrganico cafeOrganico) {
            cafeOrganico.validarSello(fechaIngreso);
        }
        this.codigo = codigo.trim();
        this.productor = productor.trim();
        this.fechaIngreso = fechaIngreso;
        this.pesoRecibido = pesoRecibido;
        if (calidad == null) {
            throw new IllegalArgumentException("La calidad es obligatoria.");
        }
        this.calidad = calidad;
        this.producto = producto;
        this.pagoProductor = producto.calcularPago(pesoRecibido, calidad)
                .setScale(2, RoundingMode.HALF_UP);
        this.pesoDisponible = pesoRecibido;
        this.estado = EstadoLote.DISPONIBLE;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getProductor() {
        return productor;
    }

    public LocalDate getFechaIngreso() {
        return fechaIngreso;
    }

    public BigDecimal getPesoRecibido() {
        return pesoRecibido;
    }

    public BigDecimal getPesoDisponible() {
        return pesoDisponible;
    }

    public Calidad getCalidad() {
        return calidad;
    }

    public Producto getProducto() {
        return producto;
    }

    public String getCategoria() {
        return producto.getCategoria();
    }

    public BigDecimal getPagoProductor() {
        return pagoProductor;
    }

    public EstadoLote getEstado() {
        return estado;
    }

    public LocalDate getFechaVencimiento() {
        return producto.calcularVencimiento(fechaIngreso);
    }

    public BigDecimal getCostoPorKg() {
        return pagoProductor.divide(pesoRecibido, 10, RoundingMode.HALF_UP);
    }

    public BigDecimal consumir(BigDecimal cantidad) {
        if (cantidad == null || cantidad.signum() <= 0 || cantidad.compareTo(pesoDisponible) > 0) {
            throw new IllegalArgumentException("La cantidad a consumir debe ser positiva y no superar el disponible.");
        }
        BigDecimal cobro = getCostoPorKg().multiply(cantidad).multiply(new BigDecimal("1.35"));
        pesoDisponible = pesoDisponible.subtract(cantidad);
        if (pesoDisponible.signum() == 0) {
            estado = EstadoLote.AGOTADO;
        }
        return cobro;
    }

    public BigDecimal descartar() {
        if (estado != EstadoLote.DISPONIBLE) {
            return BigDecimal.ZERO;
        }
        BigDecimal perdida = getCostoPorKg().multiply(pesoDisponible);
        estado = EstadoLote.DESCARTADO;
        pesoDisponible = BigDecimal.ZERO;
        return perdida;
    }

    public String resumen() {
        return String.format("%s | %s | calidad %s | ingresó %s | vence %s | disponible %s kg | %s",
                codigo, getCategoria(), calidad, fechaIngreso,
                getFechaVencimiento() == null ? "no vence" : getFechaVencimiento(),
                pesoDisponible.stripTrailingZeros().toPlainString(), estado);
    }
}

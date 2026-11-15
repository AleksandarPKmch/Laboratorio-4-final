import java.math.BigDecimal;
import java.time.LocalDate;

public class Preliquidacion {
    private final Lote lote;

    public Preliquidacion(Lote lote) {
        this.lote = lote;
    }

    public Lote getLote() {
        return lote;
    }

    public BigDecimal getPago() {
        return lote.getPagoProductor();
    }

    public LocalDate getVencimiento() {
        return lote.getFechaVencimiento();
    }
}

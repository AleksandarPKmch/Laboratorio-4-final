import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class Bodega implements Iterable<Lote> {
    private final String codigo;
    private final BigDecimal capacidadMaxima;
    private final boolean camaraFria;
    private final List<Lote> lotes = new ArrayList<>();

    public Bodega(String codigo, BigDecimal capacidadMaxima, boolean camaraFria) {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("El código de bodega no puede estar vacío.");
        }
        if (capacidadMaxima == null || capacidadMaxima.signum() <= 0) {
            throw new IllegalArgumentException("La capacidad de bodega debe ser mayor que cero.");
        }
        this.codigo = codigo.trim();
        this.capacidadMaxima = capacidadMaxima;
        this.camaraFria = camaraFria;
    }

    public String getCodigo() {
        return codigo;
    }

    public BigDecimal getCapacidadMaxima() {
        return capacidadMaxima;
    }

    public boolean isCamaraFria() {
        return camaraFria;
    }

    public BigDecimal getPesoAlmacenado() {
        return lotes.stream().map(Lote::getPesoDisponible).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean puedeRecibir(Lote lote) {
        boolean requisitoCompatible = (lote.getProducto().getRequisitoAlmacenamiento()
                == RequisitoAlmacenamiento.CAMARA_FRIA) == camaraFria;
        return requisitoCompatible && getPesoAlmacenado().add(lote.getPesoRecibido()).compareTo(capacidadMaxima) <= 0;
    }

    public void agregar(Lote lote) {
        if (!puedeRecibir(lote)) {
            throw new IllegalArgumentException("La bodega no tiene capacidad suficiente o no es compatible con el lote.");
        }
        lotes.add(lote);
    }

    public void retirar(Lote lote) {
        lotes.remove(lote);
    }

    public List<Lote> lotesOrdenados() {
        List<Lote> ordenados = new ArrayList<>(lotes);
        ordenados.sort(ordenSalida());
        return ordenados;
    }

    public static Comparator<Lote> ordenSalida() {
        return (primero, segundo) -> {
            boolean perecederoPrimero = primero.getFechaVencimiento() != null;
            boolean perecederoSegundo = segundo.getFechaVencimiento() != null;
            if (perecederoPrimero != perecederoSegundo) {
                return perecederoPrimero ? -1 : 1;
            }
            int comparacion;
            if (perecederoPrimero) {
                comparacion = primero.getFechaVencimiento().compareTo(segundo.getFechaVencimiento());
            } else {
                comparacion = primero.getFechaIngreso().compareTo(segundo.getFechaIngreso());
            }
            if (comparacion != 0) {
                return comparacion;
            }
            comparacion = Integer.compare(primero.getCalidad().getPrioridad(), segundo.getCalidad().getPrioridad());
            return comparacion != 0 ? comparacion : primero.getCodigo().compareToIgnoreCase(segundo.getCodigo());
        };
    }

    @Override
    public Iterator<Lote> iterator() {
        List<Lote> ordenados = lotesOrdenados();
        return new Iterator<>() {
            private int indice;
            private Lote actual;
            private boolean puedeRetirar;

            @Override
            public boolean hasNext() {
                return indice < ordenados.size();
            }

            @Override
            public Lote next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                actual = ordenados.get(indice++);
                puedeRetirar = true;
                return actual;
            }

            @Override
            public void remove() {
                if (!puedeRetirar) {
                    throw new IllegalStateException();
                }
                lotes.remove(actual);
                puedeRetirar = false;
            }
        };
    }
}

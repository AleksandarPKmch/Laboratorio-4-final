import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Cooperativa {
    private LocalDate fechaOperacion = LocalDate.of(2026, 1, 1);
    private final Map<String, Bodega> bodegas = new LinkedHashMap<>();
    private final List<Lote> todosLosLotes = new ArrayList<>();
    private final Map<String, BigDecimal> pagos = new TreeMap<>();
    private final Map<String, BigDecimal> cobros = new TreeMap<>();
    private final Map<String, BigDecimal> perdidas = new TreeMap<>();

    public Cooperativa() {
        cargarDatosIniciales();
    }

    public LocalDate getFechaOperacion() {
        return fechaOperacion;
    }

    public List<Bodega> getBodegas() {
        return List.copyOf(bodegas.values());
    }

    public Preliquidacion preliquidar(Lote lote) {
        validarCodigoUnico(lote.getCodigo());
        if (lote.getFechaIngreso().isAfter(fechaOperacion)) {
            throw new IllegalArgumentException("La fecha de ingreso no puede ser posterior a la fecha de operación.");
        }
        return new Preliquidacion(lote);
    }

    public void recibir(Preliquidacion preliquidacion, String codigoBodega) {
        if (preliquidacion == null) {
            throw new IllegalArgumentException("La pre-liquidación es obligatoria.");
        }
        Lote lote = preliquidacion.getLote();
        validarCodigoUnico(lote.getCodigo());
        if (!lote.getFechaIngreso().equals(fechaOperacion)) {
            throw new IllegalArgumentException("El lote debe ingresar en la fecha de operación actual.");
        }
        Bodega bodega = buscarBodega(codigoBodega);
        if (!bodega.puedeRecibir(lote)) {
            throw new IllegalArgumentException("La bodega no tiene capacidad suficiente o no es compatible con el lote.");
        }
        bodega.agregar(lote);
        todosLosLotes.add(lote);
        sumar(pagos, lote.getCategoria(), lote.getPagoProductor());
    }

    public void registrarBodega(Bodega bodega) {
        if (bodega == null) {
            throw new IllegalArgumentException("La bodega es obligatoria.");
        }
        String clave = normalizarCodigo(bodega.getCodigo());
        if (bodegas.containsKey(clave)) {
            throw new IllegalArgumentException("Ya existe una bodega con ese código.");
        }
        bodegas.put(clave, bodega);
    }

    public List<String> despachar(String categoria, BigDecimal cantidad) {
        if (categoria == null || categoria.isBlank()) {
            throw new IllegalArgumentException("La categoría es obligatoria.");
        }
        if (cantidad == null || cantidad.signum() <= 0) {
            throw new IllegalArgumentException("La cantidad solicitada debe ser mayor que cero.");
        }
        List<Lote> candidatos = bodegas.values().stream()
                .flatMap(bodega -> bodega.lotesOrdenados().stream())
                .filter(lote -> lote.getEstado() == EstadoLote.DISPONIBLE && lote.getCategoria().equals(categoria))
                .sorted(Bodega.ordenSalida())
                .toList();
        BigDecimal disponible = candidatos.stream().map(Lote::getPesoDisponible)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (disponible.compareTo(cantidad) < 0) {
            throw new IllegalArgumentException("Pedido rechazado: solo hay " + disponible.stripTrailingZeros().toPlainString()
                    + " kg disponibles de " + categoria + ".");
        }

        BigDecimal restante = cantidad;
        BigDecimal cobroTotal = BigDecimal.ZERO;
        List<String> detalle = new ArrayList<>();
        for (Lote lote : candidatos) {
            if (restante.signum() == 0) {
                break;
            }
            BigDecimal tomado = lote.getPesoDisponible().min(restante);
            BigDecimal cobro = dineroRedondeado(lote.consumir(tomado));
            cobroTotal = cobroTotal.add(cobro);
            restante = restante.subtract(tomado);
            detalle.add("  " + lote.getCodigo() + ": " + tomado.stripTrailingZeros().toPlainString()
                    + " kg, cobro " + dinero(cobro));
            if (lote.getEstado() == EstadoLote.AGOTADO) {
                buscarBodegaDe(lote).retirar(lote);
            }
        }
        sumar(cobros, categoria, dineroRedondeado(cobroTotal));
        detalle.add("Total cobrado: " + dinero(cobroTotal));
        return List.copyOf(detalle);
    }

    public void avanzarFecha(LocalDate nuevaFecha) {
        if (nuevaFecha == null || !nuevaFecha.isAfter(fechaOperacion)) {
            throw new IllegalArgumentException("La nueva fecha debe ser posterior a la fecha de operación actual.");
        }
        fechaOperacion = nuevaFecha;
        for (Bodega bodega : bodegas.values()) {
            var iterador = bodega.iterator();
            while (iterador.hasNext()) {
                Lote lote = iterador.next();
                if (lote.getFechaVencimiento() != null && fechaOperacion.isAfter(lote.getFechaVencimiento())) {
                    BigDecimal perdida = dineroRedondeado(lote.descartar());
                    sumar(perdidas, lote.getCategoria(), perdida);
                    iterador.remove();
                }
            }
        }
    }

    public List<Lote> proximosAVencer() {
        LocalDate limite = fechaOperacion.plusDays(3);
        return bodegas.values().stream()
                .flatMap(bodega -> bodega.lotesOrdenados().stream())
                .filter(lote -> lote.getEstado() == EstadoLote.DISPONIBLE && lote.getFechaVencimiento() != null)
                .filter(lote -> !lote.getFechaVencimiento().isBefore(fechaOperacion)
                        && !lote.getFechaVencimiento().isAfter(limite))
                .sorted(Comparator.comparing(Lote::getFechaVencimiento)
                        .thenComparing(Bodega.ordenSalida()))
                .toList();
    }

    public String generarReporteExistencias() {
        StringBuilder reporte = new StringBuilder("Existencias al ").append(fechaOperacion).append('\n');
        Map<String, List<Lote>> porCategoria = new TreeMap<>();
        bodegas.values().forEach(bodega -> bodega.lotesOrdenados().forEach(lote ->
                porCategoria.computeIfAbsent(lote.getCategoria(), ignorado -> new ArrayList<>()).add(lote)));
        reporte.append("\nPor categoría:\n");
        if (porCategoria.isEmpty()) {
            reporte.append("  Sin existencias.\n");
        }
        porCategoria.forEach((categoria, lotes) -> {
            reporte.append("  ").append(categoria).append(":\n");
            lotes.stream().sorted(Bodega.ordenSalida())
                    .forEach(lote -> reporte.append("    ").append(lote.resumen()).append('\n'));
        });
        reporte.append("\nBodegas:\n");
        bodegas.values().forEach(bodega -> {
            reporte.append("  ").append(bodega.getCodigo()).append(" (")
                    .append(bodega.isCamaraFria() ? "cámara fría" : "seca")
                    .append(", ").append(bodega.getPesoAlmacenado().stripTrailingZeros().toPlainString())
                    .append("/").append(bodega.getCapacidadMaxima().stripTrailingZeros().toPlainString())
                    .append(" kg)\n");
            bodega.forEach(lote -> reporte.append("    ").append(lote.resumen()).append('\n'));
        });
        return reporte.toString();
    }

    public String generarReporteFinanciero() {
        StringBuilder reporte = new StringBuilder("Resumen financiero (Q)\n");
        reporte.append("Categoría | Pagado a productores | Cobrado en despachos | Pérdidas por vencimiento\n");
        Map<String, Boolean> categorias = new TreeMap<>();
        pagos.keySet().forEach(categoria -> categorias.put(categoria, true));
        cobros.keySet().forEach(categoria -> categorias.put(categoria, true));
        perdidas.keySet().forEach(categoria -> categorias.put(categoria, true));
        categorias.keySet().forEach(categoria -> reporte.append(categoria).append(" | ")
                .append(dinero(pagos.getOrDefault(categoria, BigDecimal.ZERO))).append(" | ")
                .append(dinero(cobros.getOrDefault(categoria, BigDecimal.ZERO))).append(" | ")
                .append(dinero(perdidas.getOrDefault(categoria, BigDecimal.ZERO))).append('\n'));
        if (categorias.isEmpty()) {
            reporte.append("Sin movimientos registrados.\n");
        }
        return reporte.toString();
    }

    public BigDecimal getTotalPagado(String categoria) {
        return pagos.getOrDefault(categoria, BigDecimal.ZERO);
    }

    public BigDecimal getTotalCobrado(String categoria) {
        return cobros.getOrDefault(categoria, BigDecimal.ZERO);
    }

    public BigDecimal getTotalPerdido(String categoria) {
        return perdidas.getOrDefault(categoria, BigDecimal.ZERO);
    }

    public List<Lote> getLotesActivos() {
        return bodegas.values().stream().flatMap(bodega -> bodega.lotesOrdenados().stream()).toList();
    }

    private void validarCodigoUnico(String codigo) {
        if (todosLosLotes.stream().anyMatch(lote -> normalizarCodigo(lote.getCodigo()).equals(normalizarCodigo(codigo)))) {
            throw new IllegalArgumentException("Ya existe un lote con ese código.");
        }
    }

    private Bodega buscarBodega(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("El código de bodega no puede estar vacío.");
        }
        Bodega bodega = bodegas.get(normalizarCodigo(codigo));
        if (bodega == null) {
            throw new IllegalArgumentException("No existe la bodega " + codigo + ".");
        }
        return bodega;
    }

    private Bodega buscarBodegaDe(Lote lote) {
        return bodegas.values().stream()
                .filter(bodega -> bodega.lotesOrdenados().contains(lote))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("El lote no está asignado a una bodega."));
    }

    private void sumar(Map<String, BigDecimal> registro, String categoria, BigDecimal monto) {
        registro.merge(categoria, monto, BigDecimal::add);
    }

    private String normalizarCodigo(String codigo) {
        return codigo.trim().toUpperCase();
    }

    private BigDecimal dineroRedondeado(BigDecimal monto) {
        return monto.setScale(2, RoundingMode.HALF_UP);
    }

    private String dinero(BigDecimal monto) {
        return "Q" + dineroRedondeado(monto).toPlainString();
    }

    private void cargarDatosIniciales() {
        Bodega secaUno = new Bodega("S1", new BigDecimal("3000"), false);
        Bodega secaDos = new Bodega("S2", new BigDecimal("1500"), false);
        Bodega fria = new Bodega("F1", new BigDecimal("1500"), true);
        bodegas.put("S1", secaUno);
        bodegas.put("S2", secaDos);
        bodegas.put("F1", fria);

        agregarInicial(secaUno, lote("CAF-001", "Productor Uno", "2025-12-01", "500", Calidad.A,
                new CafePergamino(new BigDecimal("13.4"))));
        agregarInicial(secaDos, lote("CAF-002", "Productor Dos", "2025-12-15", "300", Calidad.B,
                new CafePergamino(new BigDecimal("11.5"))));
        agregarInicial(secaUno, lote("ORG-001", "Productor Tres", "2025-11-15", "100", Calidad.A,
                new CafeOrganico(new BigDecimal("12.5"), "CertiGT", LocalDate.of(2026, 5, 30))));
        agregarInicial(secaDos, lote("ORG-002", "Productor Cuatro", "2025-12-05", "100", Calidad.B,
                new CafeOrganico(new BigDecimal("11"), "EcoCert", LocalDate.of(2026, 4, 30))));
        agregarInicial(secaUno, lote("CAR-B", "Productor Cinco", "2025-07-05", "80", Calidad.B,
                new Cardamomo(new BigDecimal("90"))));
        agregarInicial(secaUno, lote("CAR-A", "Productor Seis", "2025-07-05", "100", Calidad.A,
                new Cardamomo(new BigDecimal("80"))));
        agregarInicial(fria, lote("HOR-001", "Productor Siete", "2025-12-27", "200", Calidad.A,
                new Hortaliza(7)));
        agregarInicial(fria, lote("HOR-002", "Productor Ocho", "2025-12-25", "150", Calidad.B,
                new Hortaliza(10)));
        agregarInicial(fria, lote("QUE-001", "Productor Nueve", "2025-12-20", "40", Calidad.A,
                new QuesoFresco(new BigDecimal("25"))));
        agregarInicial(fria, lote("QUE-002", "Productor Diez", "2025-12-15", "30", Calidad.B,
                new QuesoFresco(new BigDecimal("30"))));
    }

    private Lote lote(String codigo, String productor, String fecha, String peso, Calidad calidad, Producto producto) {
        return new Lote(codigo, productor, LocalDate.parse(fecha), new BigDecimal(peso), calidad, producto);
    }

    private void agregarInicial(Bodega bodega, Lote lote) {
        bodega.agregar(lote);
        todosLosLotes.add(lote);
    }
}

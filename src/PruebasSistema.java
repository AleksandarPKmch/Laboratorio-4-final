import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class PruebasSistema {
    private static int pruebas;

    public static void main(String[] args) {
        probar("Café pergamino con humedad fraccionaria", () ->
                igual("5640.00", lote("CafePergamino", "500", Calidad.A,
                        new CafePergamino(new BigDecimal("13.4"))).getPagoProductor()));
        probar("Bonificación de café orgánico", () ->
                igual("6015.00", lote("CafeOrganico", "500", Calidad.A,
                        new CafeOrganico(new BigDecimal("13.4"), "CertiGT", LocalDate.of(2026, 5, 1)))
                        .getPagoProductor()));
        probar("Bono por grano verde", () ->
                igual("4224.00", lote("Cardamomo", "80", Calidad.B,
                        new Cardamomo(new BigDecimal("90"))).getPagoProductor()));
        probar("Merma de hortaliza", () ->
                igual("760.00", lote("Hortaliza", "200", Calidad.A, new Hortaliza(7)).getPagoProductor()));
        probar("Descuento de empaque del queso", () ->
                igual("1460.00", lote("Queso fresco", "40", Calidad.A,
                        new QuesoFresco(new BigDecimal("25"))).getPagoProductor()));
        probar("Pre-liquidación y pedido rechazado sin afectar saldo", () -> {
            Cooperativa cooperativa = new Cooperativa();
            BigDecimal pagosAntes = cooperativa.getTotalPagado("Café en pergamino");
            BigDecimal cobrosAntes = cooperativa.getTotalCobrado("Café en pergamino");
            int lotesAntes = cooperativa.getLotesActivos().size();
            Lote lote = new Lote("NUEVO", "Productor", cooperativa.getFechaOperacion(), new BigDecimal("20"),
                    Calidad.A, new CafePergamino(new BigDecimal("12")));
            cooperativa.preliquidar(lote);
            exigir(cooperativa.getTotalPagado("Café en pergamino").compareTo(pagosAntes) == 0, "pre-liquidación alteró pagos");
            exigir(cooperativa.getLotesActivos().size() == lotesAntes, "pre-liquidación alteró existencias");
            rechazada(() -> cooperativa.despachar("Café en pergamino", new BigDecimal("99999")));
            exigir(cooperativa.getTotalCobrado("Café en pergamino").compareTo(cobrosAntes) == 0, "pedido rechazado alteró cobros");
            exigir(cooperativa.getLotesActivos().size() == lotesAntes, "pedido rechazado alteró existencias");
        });
        probar("Restricciones de bodega y capacidad", () -> {
            Cooperativa cooperativa = new Cooperativa();
            cooperativa.registrarBodega(new Bodega("MINI", new BigDecimal("5"), false));
            Lote lote = new Lote("EXCEDE", "Productor", cooperativa.getFechaOperacion(), new BigDecimal("6"),
                    Calidad.A, new CafePergamino(new BigDecimal("10")));
            rechazada(() -> cooperativa.recibir(cooperativa.preliquidar(lote), "MINI"));
            Lote frioEnSeco = new Lote("FRIO", "Productor", cooperativa.getFechaOperacion(), BigDecimal.ONE,
                    Calidad.A, new Hortaliza(4));
            rechazada(() -> cooperativa.recibir(cooperativa.preliquidar(frioEnSeco), "S1"));
            exigir(cooperativa.getTotalPagado("Café en pergamino").signum() == 0, "recepción rechazada alteró pagos");
            exigir(cooperativa.getLotesActivos().stream().noneMatch(item -> item.getCodigo().equals("EXCEDE")), "recepción rechazada alteró lotes");
        });
        probar("Orden de salida y despacho entre bodegas", () -> {
            Cooperativa cooperativa = new Cooperativa();
            List<Lote> cardamomos = cooperativa.getLotesActivos().stream()
                    .filter(item -> item.getCategoria().equals("Cardamomo"))
                    .sorted(Bodega.ordenSalida()).toList();
            exigir(cardamomos.get(0).getCodigo().equals("CAR-A"), "el desempate de calidad no priorizó A");
            List<String> detalle = cooperativa.despachar("Café en pergamino", new BigDecimal("600"));
            exigir(detalle.get(0).contains("CAF-001: 500"), "no se consumió primero el lote más antiguo");
            exigir(detalle.get(1).contains("CAF-002: 100"), "no se combinó el inventario de otra bodega");
            exigir(cooperativa.getLotesActivos().stream().noneMatch(item -> item.getCodigo().equals("CAF-001")), "lote agotado no salió de bodega");
        });
        probar("Vencimiento y pérdida", () -> {
            Cooperativa cooperativa = new Cooperativa();
            Lote hortaliza = cooperativa.getLotesActivos().stream()
                    .filter(item -> item.getCodigo().equals("HOR-001")).findFirst().orElseThrow();
            cooperativa.despachar("Hortaliza", new BigDecimal("150"));
            cooperativa.avanzarFecha(LocalDate.of(2026, 1, 3));
            exigir(hortaliza.getEstado() == EstadoLote.DISPONIBLE, "lote se descartó en la fecha de vencimiento");
            cooperativa.avanzarFecha(LocalDate.of(2026, 1, 4));
            igual("190.00", cooperativa.getTotalPerdido("Hortaliza"));
            exigir(hortaliza.getEstado() == EstadoLote.DESCARTADO, "lote vencido no se marcó descartado");
            exigir(cooperativa.getLotesActivos().stream().noneMatch(item -> item.getCodigo().equals("HOR-001")), "lote vencido quedó en bodega");
        });
        probar("Validaciones no alteran pagos ni cobran fechas", () -> {
            Cooperativa cooperativa = new Cooperativa();
            rechazada(() -> cooperativa.avanzarFecha(LocalDate.of(2025, 12, 31)));
            rechazada(() -> new CafePergamino(new BigDecimal("14.1")));
            rechazada(() -> new QuesoFresco(new BigDecimal("20")).calcularPago(BigDecimal.ONE, Calidad.C));
            rechazada(() -> new Lote("ORGANICO-INVALIDO", "Productor", LocalDate.of(2026, 1, 1),
                    BigDecimal.ONE, Calidad.A, new CafeOrganico(new BigDecimal("10"), "CertiGT",
                    LocalDate.of(2025, 12, 31))));
            Lote duplicado = new Lote("CAF-001", "Productor", cooperativa.getFechaOperacion(),
                    BigDecimal.ONE, Calidad.A, new CafePergamino(new BigDecimal("10")));
            rechazada(() -> cooperativa.preliquidar(duplicado));
            exigir(cooperativa.getFechaOperacion().equals(LocalDate.of(2026, 1, 1)), "fecha cambió tras rechazo");
            exigir(cooperativa.getTotalCobrado("Queso fresco").signum() == 0, "rechazo alteró cobros");
            exigir(cooperativa.getLotesActivos().stream().noneMatch(item -> item.getCodigo().equals("ORGANICO-INVALIDO")),
                    "lote rechazado alteró existencias");
        });
        System.out.println("Resultado: " + pruebas + " casos aprobados.");
    }

    private static Lote lote(String productor, String peso, Calidad calidad, Producto producto) {
        return new Lote(productor, "Prueba", LocalDate.of(2026, 1, 1), new BigDecimal(peso), calidad, producto);
    }

    private static void probar(String nombre, Runnable prueba) {
        prueba.run();
        pruebas++;
        System.out.println("APROBADO: " + nombre);
    }

    private static void igual(String esperado, BigDecimal obtenido) {
        exigir(obtenido.compareTo(new BigDecimal(esperado)) == 0,
                "esperado " + esperado + " pero se obtuvo " + obtenido.toPlainString());
    }

    private static void rechazada(Runnable accion) {
        try {
            accion.run();
        } catch (IllegalArgumentException esperado) {
            return;
        }
        throw new AssertionError("Se esperaba que la operación fuera rechazada.");
    }

    private static void exigir(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new AssertionError(mensaje);
        }
    }
}

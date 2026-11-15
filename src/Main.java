import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

public class Main {
    private static final Scanner ENTRADA = new Scanner(System.in);
    private static final Cooperativa COOPERATIVA = new Cooperativa();

    public static void main(String[] args) {
        System.out.println("=== AcopioGT ===");
        System.out.println("Fecha de operación inicial: " + COOPERATIVA.getFechaOperacion());
        boolean continuar = true;
        while (continuar) {
            mostrarMenu();
            try {
                switch (leerEntero("Seleccione una opción: ")) {
                    case 1 -> preliquidarYRecibir();
                    case 2 -> registrarBodega();
                    case 3 -> registrarPedido();
                    case 4 -> avanzarFecha();
                    case 5 -> consultarExistencias();
                    case 6 -> mostrarProximosVencimientos();
                    case 7 -> mostrarResumenFinanciero();
                    case 0 -> continuar = false;
                    default -> System.out.println("Opción inválida. Seleccione una opción del menú.");
                }
            } catch (IllegalArgumentException ex) {
                System.out.println("Operación rechazada: " + ex.getMessage());
            }
        }
        System.out.println("Gracias por utilizar AcopioGT.");
    }

    private static void mostrarMenu() {
        System.out.println("""

                1. Pre-liquidar y recibir lote
                2. Registrar bodega
                3. Registrar pedido y despachar
                4. Avanzar fecha de operación
                5. Consultar existencias y bodegas
                6. Consultar próximos vencimientos
                7. Consultar resumen financiero
                0. Salir""");
    }

    private static void preliquidarYRecibir() {
        Producto producto = leerProducto();
        String codigo = leerTexto("Código del lote: ");
        String productor = leerTexto("Nombre del productor: ");
        BigDecimal peso = leerDecimal("Peso recibido en kg: ");
        Calidad calidad = leerCalidad();
        Lote lote = new Lote(codigo, productor, COOPERATIVA.getFechaOperacion(), peso, calidad, producto);
        Preliquidacion preliquidacion = COOPERATIVA.preliquidar(lote);
        System.out.println("Pre-liquidación (no registra cambios):");
        System.out.println("  Categoría: " + producto.getCategoria());
        System.out.println("  Pago al productor: " + dinero(preliquidacion.getPago()));
        System.out.println("  Vencimiento: " + fecha(preliquidacion.getVencimiento()));
        if (!leerSiNo("¿Acepta y desea recibir el lote? (s/n): ")) {
            System.out.println("Pre-liquidación cancelada; no se modificó el sistema.");
            return;
        }
        String codigoBodega = leerTexto("Código de bodega: ");
        COOPERATIVA.recibir(preliquidacion, codigoBodega);
        System.out.println("Lote recibido. Pago registrado: " + dinero(preliquidacion.getPago()));
    }

    private static Producto leerProducto() {
        System.out.println("""
                Categorías:
                1. Café en pergamino
                2. Café orgánico
                3. Cardamomo
                4. Hortaliza
                5. Queso fresco""");
        int categoria = leerEntero("Categoría: ");
        return switch (categoria) {
            case 1 -> new CafePergamino(leerDecimal("Humedad (%): "));
            case 2 -> new CafeOrganico(
                    leerDecimal("Humedad (%): "),
                    leerTexto("Entidad certificadora: "),
                    leerFecha("Vigencia del sello (AAAA-MM-DD): "));
            case 3 -> new Cardamomo(leerDecimal("Porcentaje de grano verde: "));
            case 4 -> new Hortaliza(leerEntero("Vida útil en días (3-14): "));
            case 5 -> new QuesoFresco(leerDecimal("Porcentaje de grasa: "));
            default -> throw new IllegalArgumentException("La categoría debe estar entre 1 y 5.");
        };
    }

    private static void registrarBodega() {
        String codigo = leerTexto("Código de bodega: ");
        BigDecimal capacidad = leerDecimal("Capacidad máxima en kg: ");
        boolean camaraFria = leerSiNo("¿Es cámara fría? (s/n): ");
        COOPERATIVA.registrarBodega(new Bodega(codigo, capacidad, camaraFria));
        System.out.println("Bodega registrada.");
    }

    private static void registrarPedido() {
        Producto producto = leerCategoriaSinDatos();
        BigDecimal cantidad = leerDecimal("Cantidad solicitada en kg: ");
        List<String> detalle = COOPERATIVA.despachar(producto.getCategoria(), cantidad);
        System.out.println("Pedido despachado:");
        detalle.forEach(System.out::println);
    }

    private static Producto leerCategoriaSinDatos() {
        System.out.println("1. Café en pergamino  2. Café orgánico  3. Cardamomo  4. Hortaliza  5. Queso fresco");
        return switch (leerEntero("Categoría del pedido: ")) {
            case 1 -> new CafePergamino(new BigDecimal("8"));
            case 2 -> new CafeOrganico(new BigDecimal("8"), "Consulta", LocalDate.MAX);
            case 3 -> new Cardamomo(BigDecimal.ZERO);
            case 4 -> new Hortaliza(3);
            case 5 -> new QuesoFresco(new BigDecimal("10"));
            default -> throw new IllegalArgumentException("La categoría debe estar entre 1 y 5.");
        };
    }

    private static void avanzarFecha() {
        LocalDate nuevaFecha = leerFecha("Nueva fecha de operación (AAAA-MM-DD): ");
        COOPERATIVA.avanzarFecha(nuevaFecha);
        System.out.println("Fecha actualizada a " + COOPERATIVA.getFechaOperacion() + ".");
    }

    private static void consultarExistencias() {
        System.out.println(COOPERATIVA.generarReporteExistencias());
    }

    private static void mostrarProximosVencimientos() {
        List<Lote> lotes = COOPERATIVA.proximosAVencer();
        System.out.println("Lotes que vencen dentro de los próximos 3 días:");
        if (lotes.isEmpty()) {
            System.out.println("  No hay lotes próximos a vencer.");
        } else {
            lotes.forEach(lote -> System.out.println("  " + lote.resumen()));
        }
    }

    private static void mostrarResumenFinanciero() {
        System.out.println(COOPERATIVA.generarReporteFinanciero());
    }

    private static Calidad leerCalidad() {
        String valor = leerTexto("Calidad (A/B/C): ").toUpperCase();
        try {
            return Calidad.valueOf(valor);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("La calidad debe ser A, B o C.");
        }
    }

    private static int leerEntero(String mensaje) {
        try {
            return Integer.parseInt(leerTexto(mensaje));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Ingrese un número entero válido.");
        }
    }

    private static BigDecimal leerDecimal(String mensaje) {
        String valor = leerTexto(mensaje).replace(',', '.');
        try {
            return new BigDecimal(valor);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Ingrese un número válido.");
        }
    }

    private static LocalDate leerFecha(String mensaje) {
        try {
            return LocalDate.parse(leerTexto(mensaje));
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("La fecha debe tener el formato AAAA-MM-DD.");
        }
    }

    private static String leerTexto(String mensaje) {
        System.out.print(mensaje);
        return ENTRADA.nextLine().trim();
    }

    private static boolean leerSiNo(String mensaje) {
        String valor = leerTexto(mensaje).toLowerCase();
        if (valor.equals("s") || valor.equals("si") || valor.equals("sí")) {
            return true;
        }
        if (valor.equals("n") || valor.equals("no")) {
            return false;
        }
        throw new IllegalArgumentException("Responda s o n.");
    }

    private static String dinero(BigDecimal valor) {
        return "Q" + valor.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private static String fecha(LocalDate valor) {
        return valor == null ? "No vence" : valor.toString();
    }
}

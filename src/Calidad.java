public enum Calidad {
    A(1), B(2), C(3);

    private final int prioridad;

    Calidad(int prioridad) {
        this.prioridad = prioridad;
    }

    public int getPrioridad() {
        return prioridad;
    }
}

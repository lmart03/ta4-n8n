package pe.edu.ta4.ruc.model;

/** Estado de un RUC dentro del trabajo. */
public enum ItemStatus {
    /** Aún no llegó el resultado de SUNAT. */
    PENDIENTE,
    /** SUNAT devolvió datos para este RUC. */
    ENCONTRADO,
    /** El resultado no incluye este RUC. */
    NO_ENCONTRADO
}

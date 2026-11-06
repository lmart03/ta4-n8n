package pe.edu.ta4.ruc.service;

import java.util.Optional;
import pe.edu.ta4.ruc.model.SunatRecord;

/**
 * Fuente alternativa de datos de RUC: una API de terceros que republica el padrón de SUNAT. No es la Consulta
 * Múltiple de SUNAT ni la usa el robot; los trabajos resueltos por aquí quedan marcados con su fuente.
 */
public interface RucApiClient {
    /** Hay token configurado. */
    boolean configured();

    /** Si el robot falla, el backend usa esta fuente solo (el trabajo queda marcado con ella). */
    boolean autoFallback();

    /** Nombre de la fuente, tal como se muestra al usuario y se anota en el trabajo. */
    String sourceName();

    /**
     * Consulta un RUC. Vacío si la API no lo conoce (404). Si la API no está disponible (token, cuota, red) lanza
     * una {@code ApiException} 503 con un mensaje comprensible.
     */
    Optional<SunatRecord> lookup(String ruc);
}

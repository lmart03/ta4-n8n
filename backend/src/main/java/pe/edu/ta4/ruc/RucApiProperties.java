package pe.edu.ta4.ruc;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * API de consulta de RUC de un tercero (por defecto consultaderuc.pe), usada como fuente alternativa cuando SUNAT
 * bloquea al robot ({@code ta4.ruc-api.*}). Sin token queda desactivada.
 *
 * @param baseUrl   dirección base de la API
 * @param token     token Bearer de la cuenta (variable de entorno {@code CONSULTA_RUC_API_TOKEN}); vacío = desactivada
 * @param timeout   espera máxima por consulta
 * @param autoFallback si el robot falla, consulta por API automáticamente (por defecto sí, si hay token)
 */
@ConfigurationProperties(prefix = "ta4.ruc-api")
public record RucApiProperties(
    @DefaultValue("https://consultaderuc.pe") String baseUrl,
    @DefaultValue("") String token,
    @DefaultValue("10s") Duration timeout,
    @DefaultValue("true") boolean autoFallback) {

    public boolean configured() {
        return token != null && !token.isBlank();
    }
}

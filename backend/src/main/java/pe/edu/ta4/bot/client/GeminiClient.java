package pe.edu.ta4.bot.client;

/** API de Gemini, usada solo para comprobar que la clave y el modelo funcionan. */
public interface GeminiClient {
    /** Hace una consulta mínima y devuelve el texto generado; falla con {@code ApiException} si no responde. */
    String ping();
}

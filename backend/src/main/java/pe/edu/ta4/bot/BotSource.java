package pe.edu.ta4.bot;

/** De dónde salió una respuesta del bot (para métricas y para mostrarla en el panel). */
public enum BotSource {
    /** Respuesta directa de una pregunta frecuente. */
    FAQ,
    /** Redactada por Gemini a partir de las FAQ. */
    IA,
    /** Contingencia: falló la API o Gemini. */
    CONTINGENCIA,
    /** Sin dato: se remite a recepción. */
    DERIVADA,
    /** Rechazo por pedir diagnóstico, tratamiento o datos de pacientes. */
    RECHAZO,
    /** Ayuda por mensaje no textual o saludo. */
    AYUDA,
    /** Respuesta escrita por una persona desde el panel. */
    HUMANO
}

package pe.edu.ta4.bot.dto;

/** Estado de una pieza del bot ({@code state}: OK, WARN o ERROR). */
public record ComponentStatus(String key, String label, String state, String detail) {
}

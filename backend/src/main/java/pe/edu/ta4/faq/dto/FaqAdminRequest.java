package pe.edu.ta4.faq.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos para crear o editar una pregunta frecuente desde el panel. Si {@code keywords} viene vacío se generan a
 * partir de la pregunta; {@code sortOrder} vacío la deja al final.
 */
public record FaqAdminRequest(
    @NotBlank @Pattern(regexp = "HORARIOS|UBICACION|SERVICIOS|PRECIOS|CONTACTO|CITAS",
        message = "debe ser HORARIOS, UBICACION, SERVICIOS, PRECIOS, CONTACTO o CITAS") String category,
    @NotBlank @Size(max = 500) String question,
    @NotBlank @Size(max = 1000) String answer,
    @Size(max = 500) String keywords,
    Boolean active,
    Integer sortOrder) {
}

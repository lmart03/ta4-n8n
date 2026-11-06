package pe.edu.ta4.faq.dto;

import java.util.List;

/** Resultado de la búsqueda: la consulta normalizada y hasta tres coincidencias de igual puntuación. */
public record FaqSearchResponse(String query, List<FaqResponse> matches) {
}

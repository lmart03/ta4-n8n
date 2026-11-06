package pe.edu.ta4.faq.service;

import java.util.List;
import pe.edu.ta4.faq.dto.FaqResponse;
import pe.edu.ta4.faq.dto.FaqSearchResponse;

public interface FaqService {
    /** Lista las FAQ ordenadas; con {@code activeOnly} solo las activas. */
    List<FaqResponse> list(boolean activeOnly);

    /**
     * Busca por coincidencia léxica normalizada (sin tildes, minúsculas, sin palabras vacías) y devuelve las
     * mejores coincidencias, máximo tres. Falla con 400 si la consulta está vacía, es muy larga o no tiene
     * palabras útiles.
     */
    FaqSearchResponse search(String query);
}

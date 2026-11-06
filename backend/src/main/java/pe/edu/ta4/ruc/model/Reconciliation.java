package pe.edu.ta4.ruc.model;

import java.util.List;
import java.util.Map;

/**
 * Cotejo entre los RUC solicitados y los recibidos de SUNAT.
 *
 * @param byRuc      primer registro recibido de cada RUC solicitado
 * @param missing    RUC solicitados que SUNAT no devolvió
 * @param unexpected RUC devueltos que no se habían solicitado
 * @param duplicates RUC solicitados que SUNAT devolvió más de una vez (se usa solo el primero)
 */
public record Reconciliation(Map<String, SunatRecord> byRuc, List<String> missing,
                             List<String> unexpected, List<String> duplicates) {

    public boolean isClean() {
        return missing.isEmpty() && unexpected.isEmpty() && duplicates.isEmpty();
    }
}

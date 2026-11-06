package pe.edu.ta4.ruc.service;

import java.util.List;
import pe.edu.ta4.ruc.model.ParsedResult;
import pe.edu.ta4.ruc.model.Reconciliation;

public interface ReconciliationService {
    /** Coteja los RUC solicitados con los registros recibidos: ausentes, inesperados y duplicados. */
    Reconciliation reconcile(List<String> requested, ParsedResult parsed);
}

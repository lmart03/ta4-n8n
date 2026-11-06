package pe.edu.ta4.business.service;

import pe.edu.ta4.business.dto.BusinessAdminRequest;
import pe.edu.ta4.business.dto.BusinessResponse;

/** Edición de los datos de la clínica desde el panel. */
public interface BusinessAdminService {
    /** Actualiza el negocio. Falla con 503 si no hay datos iniciales. */
    BusinessResponse update(BusinessAdminRequest request);
}

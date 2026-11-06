package pe.edu.ta4.business.service;

import pe.edu.ta4.business.dto.BusinessResponse;

public interface BusinessService {
    /** Datos generales de la clínica. Falla con 503 si el negocio no está configurado. */
    BusinessResponse getBusiness();
}

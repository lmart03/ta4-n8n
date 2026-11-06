package pe.edu.ta4.faq.service;

import java.util.List;
import java.util.UUID;
import pe.edu.ta4.faq.dto.FaqAdminRequest;
import pe.edu.ta4.faq.dto.FaqAdminResponse;

/** Edición de las preguntas frecuentes desde el panel (RF-BOT-10: actualizar sin reprogramar el flujo). */
public interface FaqAdminService {
    /** Todas las FAQ, activas e inactivas, por orden. */
    List<FaqAdminResponse> list();

    FaqAdminResponse create(FaqAdminRequest request);

    /** Falla con 404 si no existe. */
    FaqAdminResponse update(UUID id, FaqAdminRequest request);

    /** Falla con 404 si no existe. */
    void delete(UUID id);
}

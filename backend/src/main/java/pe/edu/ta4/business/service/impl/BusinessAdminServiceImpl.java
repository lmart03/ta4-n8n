package pe.edu.ta4.business.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.ta4.business.dto.BusinessAdminRequest;
import pe.edu.ta4.business.dto.BusinessResponse;
import pe.edu.ta4.business.model.BusinessInfo;
import pe.edu.ta4.business.repository.BusinessRepository;
import pe.edu.ta4.business.service.BusinessAdminService;
import pe.edu.ta4.shared.error.ApiException;

@Service
@Transactional
public class BusinessAdminServiceImpl implements BusinessAdminService {
    private final BusinessRepository repository;

    public BusinessAdminServiceImpl(BusinessRepository repository) {
        this.repository = repository;
    }

    @Override
    public BusinessResponse update(BusinessAdminRequest request) {
        BusinessInfo info = repository.findAll().stream().findFirst()
            .orElseThrow(() -> ApiException.unavailable("Sin datos del negocio."));
        String phone = request.contactPhone() == null || request.contactPhone().isBlank() ? null : request.contactPhone().trim();
        info.update(request.businessName().trim(), request.description().trim(), request.address().trim(),
            request.district().trim(), request.city().trim(), phone, request.appointmentInstructions().trim());
        return BusinessResponse.from(repository.save(info));
    }
}

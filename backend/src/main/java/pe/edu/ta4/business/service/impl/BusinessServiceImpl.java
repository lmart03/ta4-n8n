package pe.edu.ta4.business.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.ta4.business.dto.BusinessResponse;
import pe.edu.ta4.business.repository.BusinessRepository;
import pe.edu.ta4.business.service.BusinessService;
import pe.edu.ta4.shared.error.ApiException;

@Service
@Transactional(readOnly = true)
public class BusinessServiceImpl implements BusinessService {
    private final BusinessRepository repository;

    public BusinessServiceImpl(BusinessRepository repository) {
        this.repository = repository;
    }

    @Override
    public BusinessResponse getBusiness() {
        return repository.findAll().stream().findFirst()
            .map(BusinessResponse::from)
            .orElseThrow(() -> ApiException.unavailable("Sin datos del negocio."));
    }
}

package pe.edu.ta4.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import pe.edu.ta4.business.dto.BusinessResponse;
import pe.edu.ta4.business.model.BusinessInfo;
import pe.edu.ta4.business.repository.BusinessRepository;
import pe.edu.ta4.business.service.BusinessService;
import pe.edu.ta4.business.service.impl.BusinessServiceImpl;
import pe.edu.ta4.shared.error.ApiException;
import pe.edu.ta4.shared.error.ProblemType;

class BusinessServiceTest {

    @Test
    void returnsThePublicFieldsOfTheBusiness() throws Exception {
        var constructor = BusinessInfo.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        BusinessInfo info = constructor.newInstance();
        ReflectionTestUtils.setField(info, "businessName", "Clínica Soraka");
        ReflectionTestUtils.setField(info, "city", "Ica");
        ReflectionTestUtils.setField(info, "address", "Av. Los Ejemplos 123");
        BusinessRepository repository = mock(BusinessRepository.class);
        when(repository.findAll()).thenReturn(List.of(info));
        BusinessService service = new BusinessServiceImpl(repository);

        BusinessResponse response = service.getBusiness();

        assertThat(response.businessName()).isEqualTo("Clínica Soraka");
        assertThat(response.city()).isEqualTo("Ica");
        assertThat(response.address()).isEqualTo("Av. Los Ejemplos 123");
        assertThat(response.contactPhone()).isNull();
    }

    @Test
    void withoutBusinessDataItIsA503() {
        BusinessRepository repository = mock(BusinessRepository.class);
        when(repository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> new BusinessServiceImpl(repository).getBusiness())
            .isInstanceOfSatisfying(ApiException.class, error -> {
                assertThat(error.type()).isEqualTo(ProblemType.SERVICE_UNAVAILABLE);
                assertThat(error.getMessage()).isEqualTo("Sin datos del negocio.");
            });
    }
}

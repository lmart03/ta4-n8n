package pe.edu.ta4.faq;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.edu.ta4.faq.dto.FaqAdminRequest;
import pe.edu.ta4.faq.dto.FaqAdminResponse;
import pe.edu.ta4.faq.model.Faq;
import pe.edu.ta4.faq.repository.FaqRepository;
import pe.edu.ta4.faq.service.impl.FaqAdminServiceImpl;
import pe.edu.ta4.shared.error.ApiException;

class FaqAdminServiceTest {
    private final List<Faq> store = new ArrayList<>();
    private final FaqRepository repository = mock(FaqRepository.class);
    private FaqAdminServiceImpl service;

    @BeforeEach
    void setUp() {
        when(repository.save(any(Faq.class))).thenAnswer(call -> {
            Faq faq = call.getArgument(0);
            if (!store.contains(faq)) store.add(faq);
            return faq;
        });
        when(repository.findAllByOrderBySortOrderAsc()).thenAnswer(call -> List.copyOf(store));
        when(repository.findById(any(UUID.class))).thenAnswer(call ->
            store.stream().filter(f -> f.getId().equals(call.<UUID>getArgument(0))).findFirst());
        when(repository.existsById(any(UUID.class))).thenAnswer(call ->
            store.stream().anyMatch(f -> f.getId().equals(call.<UUID>getArgument(0))));
        service = new FaqAdminServiceImpl(repository);
    }

    private static FaqAdminRequest request(String question, String keywords, Boolean active, Integer order) {
        return new FaqAdminRequest("SERVICIOS", question, "Respuesta de ejemplo.", keywords, active, order);
    }

    @Test
    void crearGeneraPalabrasClaveDesdeLaPreguntaYSellaLaFecha() {
        FaqAdminResponse created = service.create(request("¿Hay estacionamiento?", null, null, null));

        assertThat(created.keywords()).isEqualTo("hay estacionamiento");
        assertThat(created.active()).isTrue();
        assertThat(created.updatedAt()).as("la columna updated_at es NOT NULL").isNotNull();
    }

    @Test
    void crearDejaLaPreguntaAlFinalSiNoSeIndicaOrden() {
        service.create(request("Primera", "primera", true, 5));

        assertThat(service.create(request("Segunda pregunta", null, true, null)).sortOrder()).isEqualTo(6);
    }

    @Test
    void respetaLasPalabrasClaveDadasYLasRecorta() {
        assertThat(service.create(request("Pregunta cualquiera", "  parking cochera  ", true, 1)).keywords())
            .isEqualTo("parking cochera");
    }

    @Test
    void editarPuedeDesactivarSinPerderElOrden() {
        FaqAdminResponse created = service.create(request("Pregunta uno", null, true, 3));

        FaqAdminResponse edited = service.update(created.id(), request("Pregunta uno", "uno", false, null));

        assertThat(edited.active()).isFalse();
        assertThat(edited.sortOrder()).isEqualTo(3);
    }

    @Test
    void editarOBorrarUnaInexistenteResponde404() {
        UUID missing = UUID.randomUUID();

        assertThatThrownBy(() -> service.update(missing, request("x y z", null, true, 1))).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.delete(missing)).isInstanceOf(ApiException.class);
    }

    @Test
    void unaPreguntaSinPalabrasUtilesExigePalabrasClave() {
        assertThatThrownBy(() -> service.create(request("¿Y?", null, true, 1))).isInstanceOf(ApiException.class)
            .hasMessageContaining("palabras clave");
    }

    @Test
    void borrarQuitaLaPregunta() {
        FaqAdminResponse created = service.create(request("Pregunta dos", null, true, 1));
        org.mockito.Mockito.doAnswer(call -> store.removeIf(f -> f.getId().equals(call.<UUID>getArgument(0))))
            .when(repository).deleteById(any(UUID.class));

        service.delete(created.id());

        assertThat(store).isEmpty();
        assertThat(service.list()).isEmpty();
    }
}

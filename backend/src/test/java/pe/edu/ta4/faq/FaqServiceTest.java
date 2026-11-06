package pe.edu.ta4.faq;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.edu.ta4.faq.dto.FaqResponse;
import pe.edu.ta4.faq.model.Faq;
import pe.edu.ta4.faq.repository.FaqRepository;
import pe.edu.ta4.faq.service.FaqService;
import pe.edu.ta4.faq.service.impl.FaqServiceImpl;
import pe.edu.ta4.shared.error.ApiException;
import pe.edu.ta4.shared.error.ProblemType;

/**
 * La búsqueda de FAQ la consume el flujo de n8n del chatbot: estos casos fijan su comportamiento con los mismos
 * datos de ejemplo de la migración V2 y los mismos resultados que devolvía la API antes de refactorizar.
 */
class FaqServiceTest {
    private FaqService service;

    private static Faq faq(int order, String category, String question, String answer, String keywords) {
        return new Faq(UUID.nameUUIDFromBytes(("faq" + order).getBytes()), category, question, answer, keywords, true, order);
    }

    @BeforeEach
    void setUp() {
        FaqRepository repository = mock(FaqRepository.class);
        List<Faq> seed = List.of(
            faq(1, "HORARIOS", "¿Cuál es el horario de lunes a viernes?", "L-V 09:00 a 18:00.", "horario horarios atienden atencion lunes martes miercoles jueves viernes semana"),
            faq(2, "HORARIOS", "¿Atienden los sábados?", "Dato ficticio de ejemplo: los sábados atendemos de 09:00 a 13:00.", "sabado sabados fin semana atienden abierto horario"),
            faq(3, "HORARIOS", "¿Abren los domingos?", "Domingos cerrado.", "domingo domingos cerrado abren atienden horario"),
            faq(4, "UBICACION", "¿Dónde se ubican?", "Dato ficticio de ejemplo: estamos en Av. Los Ejemplos 123, distrito y ciudad de Ica.", "ubicacion ubican ubicados direccion donde llegar local mapa"),
            faq(5, "SERVICIOS", "¿Qué servicios ofrecen?", "Consulta informativa general.", "servicios ofrecen especialidades consulta atencion"),
            faq(6, "PRECIOS", "¿Cuánto cuesta una consulta?", "El precio de la consulta aún no está configurado.", "precio precios costo cuesta tarifa consulta pago"),
            faq(7, "PRECIOS", "¿Cuánto cuesta un servicio específico?", "No dispongo de una tarifa para ese servicio.", "precio precios costo cuesta servicio especifico tratamiento"),
            faq(8, "CONTACTO", "¿Cómo me comunico con el consultorio?", "Teléfono no configurado.", "contacto telefono llamar escribir comunicar whatsapp numero"),
            faq(9, "CITAS", "¿Cómo solicito una cita?", "Este bot no registra ni confirma reservas.", "cita citas turno reservar reserva agendar agenda solicitar"),
            faq(10, "CITAS", "¿Puedes reservarme mañana a las 10?", "No puedo reservar ni confirmar una cita.", "reservarme reservar cita manana diez 10 disponibilidad"),
            faq(11, "SERVICIOS", "¿Atienden urgencias?", "Sin información de urgencias.", "urgencia urgencias emergencia emergencias"),
            faq(12, "UBICACION", "¿En qué ciudad están?", "Dato ficticio de ejemplo: el consultorio está en Ica, Perú.", "ciudad distrito ica peru ubicacion"));
        when(repository.findByActiveTrueOrderBySortOrderAsc()).thenReturn(seed);
        when(repository.findAllByOrderBySortOrderAsc()).thenReturn(seed);
        service = new FaqServiceImpl(repository);
    }

    @Test
    void saturdayQuestionFindsTheSaturdayFaq() {
        var result = service.search("atienden sabado");

        assertThat(result.query()).isEqualTo("atienden sabado");
        assertThat(result.matches()).extracting(FaqResponse::question).containsExactly("¿Atienden los sábados?");
    }

    @Test
    void priceQuestionFindsTheConsultationPrice() {
        assertThat(service.search("cuanto cuesta una consulta").matches())
            .extracting(FaqResponse::question).containsExactly("¿Cuánto cuesta una consulta?");
    }

    @Test
    void locationQuestionFindsWhereTheyAre() {
        assertThat(service.search("donde estan ubicados").matches())
            .extracting(FaqResponse::question).containsExactly("¿Dónde se ubican?");
    }

    @Test
    void ignoresCaseAccentsAndPunctuation() {
        assertThat(service.search("  ¿ATIENDEN los SÁBADOS?  ").matches())
            .extracting(FaqResponse::category).containsExactly("HORARIOS");
        assertThat(service.search("  ¿ATIENDEN los SÁBADOS?  ").query()).isEqualTo("¿ATIENDEN los SÁBADOS?");
    }

    @Test
    void noMatchesGivesAnEmptyList() {
        assertThat(service.search("zzzz qqqq").matches()).isEmpty();
    }

    @Test
    void tiesAreKeptUpToThreeInDisplayOrder() {
        var matches = service.search("atienden horario").matches();

        assertThat(matches.size()).isBetween(1, 3);
        assertThat(matches.getFirst().category()).isEqualTo("HORARIOS");
    }

    @Test
    void emptyOrBlankOrTooLongOrControlCharactersAre400() {
        for (String bad : new String[] {null, "", "   ", "a".repeat(501), "hola\u0007"}) {
            assertThatThrownBy(() -> service.search(bad)).isInstanceOfSatisfying(ApiException.class,
                error -> assertThat(error.type()).isEqualTo(ProblemType.VALIDATION_ERROR));
        }
    }

    @Test
    void aQueryOfOnlyStopWordsIs400() {
        assertThatThrownBy(() -> service.search("de la el")).isInstanceOfSatisfying(ApiException.class, error -> {
            assertThat(error.type()).isEqualTo(ProblemType.VALIDATION_ERROR);
            assertThat(error.getMessage()).isEqualTo("q no contiene palabras útiles");
        });
    }

    @Test
    void listReturnsEveryFaqWithoutInternalFields() {
        assertThat(service.list(true)).hasSize(12);
        assertThat(service.list(false)).hasSize(12);
        assertThat(service.list(true).getFirst()).isEqualTo(
            new FaqResponse("HORARIOS", "¿Cuál es el horario de lunes a viernes?", "L-V 09:00 a 18:00."));
    }
}

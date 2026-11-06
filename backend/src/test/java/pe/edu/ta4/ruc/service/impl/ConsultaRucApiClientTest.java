package pe.edu.ta4.ruc.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import pe.edu.ta4.ruc.model.SunatRecord;

/** Traducción de la ficha de la API de consultaderuc.pe al registro común (sin red). */
class ConsultaRucApiClientTest {

    private static Map<String, Object> sample() {
        return Map.of(
            "ruc", "20536066528",
            "razon_social", "INTIMEDIA  S.A.C.",
            "estado", "ACTIVO",
            "condicion_domicilio", "HABIDO",
            "direccion", "CAL. GERMAN SCHEREIBER NRO. 276 URB. SANTA ANA",
            "departamento", "Lima",
            "provincia", "Lima",
            "distrito", "San Isidro",
            "perfil", Map.of(
                "tipo_contribuyente", "SOCIEDAD ANONIMA CERRADA",
                "actividad_economica", "OTRAS ACTIVIDADES DE INFORMATICA"),
            "agente_igv", Map.of("designaciones", List.of(
                Map.of("tipo", "retencion", "descripcion", "Agente de retención del IGV"),
                Map.of("tipo", "percepcion_venta_interna", "descripcion", "Percepción venta interna"))));
    }

    private static SunatRecord translate(Map<String, Object> data) {
        return ConsultaRucApiClient.toRecord(data);
    }

    @Test
    void mapsTheBasicFieldsAndBuildsTheFullFiscalAddress() {
        SunatRecord record = translate(sample());

        assertThat(record.ruc()).isEqualTo("20536066528");
        assertThat(record.razonSocial()).isEqualTo("INTIMEDIA S.A.C.");
        assertThat(record.condicion()).isEqualTo("HABIDO");
        assertThat(record.estado()).isEqualTo("ACTIVO");
        assertThat(record.direccionFiscal())
            .isEqualTo("CAL. GERMAN SCHEREIBER NRO. 276 URB. SANTA ANA, SAN ISIDRO, LIMA, LIMA");
        // La API no informa la fecha de inicio de actividades: queda vacía, no se inventa.
        assertThat(record.fechaInicioActividades()).isNull();
    }

    @Test
    void mapsTheProfileAndTheAgentDesignations() {
        var details = translate(sample()).detalles();

        assertThat(details.tipoContribuyente()).isEqualTo("SOCIEDAD ANONIMA CERRADA");
        assertThat(details.actividadPrincipal()).isEqualTo("OTRAS ACTIVIDADES DE INFORMATICA");
        assertThat(details.distrito()).isEqualTo("SAN ISIDRO");
        assertThat(details.agenteRetencion()).isEqualTo("Agente de retención del IGV");
        assertThat(details.agentePercepcion()).isEqualTo("Percepción venta interna");
    }

    @Test
    void aNaturalPersonWithoutAddressOrProfileDoesNotBreak() {
        Map<String, Object> person = new java.util.HashMap<>();
        person.put("ruc", "10123456789");
        person.put("razon_social", "PEREZ GOMEZ JUAN");
        person.put("estado", "ACTIVO");
        person.put("condicion_domicilio", "HABIDO");
        person.put("direccion", null);
        person.put("perfil", null);
        person.put("agente_igv", null);

        SunatRecord record = translate(person);

        assertThat(record.direccionFiscal()).isNull();
        assertThat(record.detalles().tipoContribuyente()).isNull();
        assertThat(record.detalles().agenteRetencion()).isNull();
    }
}

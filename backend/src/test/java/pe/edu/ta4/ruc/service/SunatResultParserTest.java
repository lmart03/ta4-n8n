package pe.edu.ta4.ruc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import pe.edu.ta4.ruc.model.ParsedResult;
import pe.edu.ta4.ruc.model.SunatRecord;
import pe.edu.ta4.ruc.service.impl.SunatResultParserImpl;
import pe.edu.ta4.shared.error.ApiException;
import pe.edu.ta4.shared.error.ProblemIssue;
import pe.edu.ta4.shared.error.ProblemType;
import pe.edu.ta4.support.TestData;

/** Se prueba contra el TXT real que descargó SUNAT al consultar los 20 RUC. */
class SunatResultParserTest {
    private static final String HEADER = "NumeroRuc|Nombre ó RazonSocial|Condicion del Contribuyente|"
        + "Estado del Contribuyente|Fecha de Inicio de Actividades|Departamento|Provincia|Distrito|Direccion|";

    private final SunatResultParser parser = new SunatResultParserImpl();

    @Test
    void readsTheTwentyRecordsOfTheRealFile() {
        ParsedResult result = parser.parse(TestData.resource(TestData.SUNAT_RESULT));

        assertThat(result.warnings()).isEmpty();
        assertThat(result.records()).hasSize(20);
        assertThat(result.records().stream().map(SunatRecord::ruc)).containsExactlyElementsOf(TestData.twentyRucs());
        assertThat(result.records()).allSatisfy(record -> {
            assertThat(record.condicion()).isEqualTo("HABIDO");
            assertThat(record.estado()).isEqualTo("ACTIVO");
            assertThat(record.razonSocial()).isNotBlank();
            assertThat(record.direccionFiscal()).isNotBlank();
            assertThat(record.fechaInicioActividades()).isNotNull();
        });
    }

    @Test
    void extractsTheFourRequestedFieldsOfTheFirstRecord() {
        SunatRecord bcp = parser.parse(TestData.resource(TestData.SUNAT_RESULT)).records().getFirst();

        assertThat(bcp.ruc()).isEqualTo("20100047218");
        assertThat(bcp.razonSocial()).isEqualTo("BANCO DE CREDITO DEL PERU");
        assertThat(bcp.condicion()).isEqualTo("HABIDO");
        assertThat(bcp.fechaInicioActividades()).isEqualTo(LocalDate.of(1889, 4, 9));
    }

    @Test
    void readsTheAdditionalColumnsAndTurnsDashesIntoNulls() {
        var details = parser.parse(TestData.resource(TestData.SUNAT_RESULT)).records().getFirst().detalles();

        assertThat(details.tipoContribuyente()).isEqualTo("SOCIEDAD ANONIMA");
        assertThat(details.nombreComercial()).isEqualTo("BANCO DE CREDITO DEL PERU");
        assertThat(details.fechaInscripcion()).isEqualTo(LocalDate.of(1992, 10, 9));
        assertThat(details.departamento()).isEqualTo("LIMA");
        assertThat(details.distrito()).isEqualTo("LA MOLINA");
        assertThat(details.actividadPrincipal()).isEqualTo("OTROS TIPOS INTERMEDIACION MONETARIA.");
        assertThat(details.actividadesSecundarias()).isEqualTo("ARRENDAMIENTO CON OPCION DE COMPRA");
        assertThat(details.telefono()).isNull();
        assertThat(details.buenContribuyente()).isNull();
        assertThat(details.agenteRetencion()).startsWith("SI, incorporado");
    }

    @Test
    void buildsTheFullFiscalAddressAndDropsSunatPlaceholders() {
        SunatRecord bcp = parser.parse(TestData.resource(TestData.SUNAT_RESULT)).records().getFirst();

        // La vía viene aparte de distrito/provincia/departamento y con el relleno "(.)" de SUNAT.
        assertThat(bcp.direccionFiscal())
            .isEqualTo("JR. CENTENARIO NRO. 156 URB. LADERAS DE MELGAREJO, LA MOLINA, LIMA, LIMA");
    }

    @Test
    void keepsAccentsAndTrimsPadding() {
        SunatRecord backus = parser.parse(TestData.resource(TestData.SUNAT_RESULT)).records().stream()
            .filter(record -> record.ruc().equals("20100113610")).findFirst().orElseThrow();

        assertThat(backus.razonSocial())
            .isEqualTo("UNIÓN DE CERVECERÍAS PERUANAS BACKUS Y JOHNSTON SOCIEDAD ANÓNIMA ABIERTA");
        assertThat(backus.direccionFiscal()).isEqualTo("AV. NICOLAS AYLLON NRO. 3986, ATE, LIMA, LIMA");
    }

    @Test
    void readsWindows1252EncodedFilesToo() {
        String text = HEADER + "\r\n20100047218|PEÑA SOCIEDAD ANÓNIMA|HABIDO|ACTIVO|09/04/1889|LIMA|LIMA|LA MOLINA|JR. UNO|\r\n";

        SunatRecord record = parser.parse(text.getBytes(Charset.forName("windows-1252"))).records().getFirst();

        assertThat(record.razonSocial()).isEqualTo("PEÑA SOCIEDAD ANÓNIMA");
    }

    @Test
    void ignoresAByteOrderMark() {
        String text = "﻿" + HEADER + "\r\n20100047218|EMPRESA|HABIDO|ACTIVO|09/04/1889|LIMA|LIMA|LA MOLINA|JR. UNO|\r\n";

        assertThat(parser.parse(text.getBytes(StandardCharsets.UTF_8)).records()).hasSize(1);
    }

    @Test
    void findsColumnsByNameNotByPosition() {
        String text = "Direccion|Distrito|Provincia|Departamento|Fecha de Inicio de Actividades|Estado del Contribuyente|"
            + "Condicion del Contribuyente|Nombre o Razon Social|Numero Ruc|\r\n"
            + "JR. UNO|LA MOLINA|LIMA|LIMA|09/04/1889|ACTIVO|NO HABIDO|EMPRESA SA|20100047218|\r\n";

        SunatRecord record = parser.parse(text.getBytes(StandardCharsets.UTF_8)).records().getFirst();

        assertThat(record.ruc()).isEqualTo("20100047218");
        assertThat(record.razonSocial()).isEqualTo("EMPRESA SA");
        assertThat(record.condicion()).isEqualTo("NO HABIDO");
        assertThat(record.estado()).isEqualTo("ACTIVO");
    }

    @Test
    void turnsDashIntoNull() {
        String text = HEADER + "\r\n20100047218|EMPRESA|-|ACTIVO|-|LIMA|LIMA|LA MOLINA|-|\r\n";

        SunatRecord record = parser.parse(text.getBytes(StandardCharsets.UTF_8)).records().getFirst();

        assertThat(record.condicion()).isNull();
        assertThat(record.fechaInicioActividades()).isNull();
        assertThat(record.direccionFiscal()).isEqualTo("LA MOLINA, LIMA, LIMA");
    }

    @Test
    void warnsAboutBrokenRowsButKeepsTheGoodOnes() {
        String text = HEADER + "\r\n"
            + "20100047218|EMPRESA UNO|HABIDO|ACTIVO|09/04/1889|LIMA|LIMA|LA MOLINA|JR. UNO|\r\n"
            + "20100053455|FILA CORTA|HABIDO|\r\n"
            + "ABC|RUC MALO|HABIDO|ACTIVO|09/04/1889|LIMA|LIMA|LA MOLINA|JR. DOS|\r\n"
            + "20100130204|FECHA MALA|HABIDO|ACTIVO|31/02/2000|LIMA|LIMA|LA MOLINA|JR. TRES|\r\n";

        ParsedResult result = parser.parse(text.getBytes(StandardCharsets.UTF_8));

        assertThat(result.records()).extracting(SunatRecord::ruc).containsExactly("20100047218", "20100130204");
        assertThat(result.records().get(1).fechaInicioActividades()).isNull();
        assertThat(result.warnings()).hasSize(3);
        assertThat(result.warnings().get(0)).contains("Línea 3");
        assertThat(result.warnings().get(1)).contains("Línea 4").contains("ABC");
        assertThat(result.warnings().get(2)).contains("Línea 5").contains("31/02/2000");
    }

    @Test
    void acceptsAResultWithoutDataRows() {
        ParsedResult result = parser.parse((HEADER + "\r\n").getBytes(StandardCharsets.UTF_8));

        assertThat(result.records()).isEmpty();
    }

    @Test
    void rejectsAFileMissingRequiredColumns() {
        String text = HEADER.replace("Direccion|", "") + "\r\n";

        assertThatThrownBy(() -> parser.parse(text.getBytes(StandardCharsets.UTF_8)))
            .isInstanceOfSatisfying(ApiException.class, error -> {
                assertThat(error.type()).isEqualTo(ProblemType.INVALID_RESULT_FILE);
                assertThat(error.issues()).extracting(ProblemIssue::code).containsExactly("MISSING_COLUMN");
                assertThat(error.issues().getFirst().detail()).contains("direccion");
            });
    }

    @Test
    void rejectsAnEmptyFile() {
        assertThatThrownBy(() -> parser.parse(new byte[0])).isInstanceOfSatisfying(ApiException.class, error ->
            assertThat(error.issues()).extracting(ProblemIssue::code).containsExactly("EMPTY_RESULT"));
        assertThatThrownBy(() -> parser.parse("  \r\n \r\n".getBytes(StandardCharsets.UTF_8)))
            .isInstanceOf(ApiException.class);
    }
}

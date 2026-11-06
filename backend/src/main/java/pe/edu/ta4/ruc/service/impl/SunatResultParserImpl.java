package pe.edu.ta4.ruc.service.impl;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import pe.edu.ta4.ruc.model.ParsedResult;
import pe.edu.ta4.ruc.model.RucNumber;
import pe.edu.ta4.ruc.model.SunatDetails;
import pe.edu.ta4.ruc.model.SunatRecord;
import pe.edu.ta4.ruc.service.SunatResultParser;
import pe.edu.ta4.shared.error.ApiException;
import pe.edu.ta4.shared.error.ProblemIssue;

@Service
public class SunatResultParserImpl implements SunatResultParser {
    private static final String RUC = "numeroruc";
    private static final String NAME = "nombreorazonsocial";
    private static final String CONDITION = "condiciondelcontribuyente";
    private static final String STATE = "estadodelcontribuyente";
    private static final String START_DATE = "fechadeiniciodeactividades";
    private static final String ADDRESS = "direccion";
    private static final String DEPARTMENT = "departamento";
    private static final String PROVINCE = "provincia";
    private static final String DISTRICT = "distrito";
    // Columnas opcionales: si faltan, el dato queda vacío y la lectura sigue.
    private static final String TYPE = "tipodecontribuyente";
    private static final String TRADE_NAME = "nombrecomercial";
    private static final String REGISTRATION_DATE = "fechadeinscripcion";
    private static final String PHONE = "telefono";
    private static final String MAIN_ACTIVITY = "principalciiu";
    private static final List<String> SECONDARY_ACTIVITIES = List.of("secundario1ciiu", "secundario2ciiu");
    private static final String GOOD_TAXPAYER = "buencontribuyente";
    private static final String WITHHOLDING = "agentederetencion";
    private static final List<String> PERCEPTION = List.of("agentedepercepcionvtaint", "agentedepercepcioncomliq");
    private static final List<String> REQUIRED = List.of(RUC, NAME, CONDITION, STATE, START_DATE, ADDRESS, DEPARTMENT, PROVINCE, DISTRICT);

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");

    @Override
    public ParsedResult parse(byte[] bytes) {
        String[] lines = decode(bytes).split("\\R");
        int headerIndex = firstNonBlank(lines);
        if (headerIndex < 0) {
            throw ApiException.invalidResultFile("El TXT de resultados está vacío.",
                List.of(ProblemIssue.of("EMPTY_RESULT", "El archivo no contiene ninguna línea.")));
        }

        Map<String, Integer> columns = indexColumns(split(lines[headerIndex]));
        List<ProblemIssue> missing = new ArrayList<>();
        for (String required : REQUIRED) {
            if (!columns.containsKey(required)) {
                missing.add(ProblemIssue.of("MISSING_COLUMN", "Falta la columna obligatoria '" + required + "' en la cabecera."));
            }
        }
        if (!missing.isEmpty()) {
            throw ApiException.invalidResultFile("El TXT no tiene las columnas esperadas de SUNAT.", missing);
        }

        int needed = columns.values().stream().max(Integer::compare).orElse(0) + 1;
        List<SunatRecord> records = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        for (int i = headerIndex + 1; i < lines.length; i++) {
            if (lines[i].isBlank()) continue;
            int lineNumber = i + 1;
            String[] cells = split(lines[i]);
            if (cells.length < needed) {
                warnings.add("Línea " + lineNumber + ": tiene " + cells.length + " columnas y se esperaban al menos " + needed + "; se omitió.");
                continue;
            }
            String ruc = cells[columns.get(RUC)];
            if (!RucNumber.hasValidFormat(ruc)) {
                warnings.add("Línea " + lineNumber + ": el RUC '" + ruc + "' no tiene 11 dígitos; se omitió.");
                continue;
            }
            LocalDate start = parseDate(cells[columns.get(START_DATE)], lineNumber, warnings);
            records.add(new SunatRecord(ruc,
                clean(cells[columns.get(NAME)]),
                address(cells[columns.get(ADDRESS)], cells[columns.get(DISTRICT)], cells[columns.get(PROVINCE)], cells[columns.get(DEPARTMENT)]),
                clean(cells[columns.get(CONDITION)]),
                clean(cells[columns.get(STATE)]),
                start,
                details(cells, columns, lineNumber, warnings)));
        }
        return new ParsedResult(List.copyOf(records), List.copyOf(warnings));
    }

    private static SunatDetails details(String[] cells, Map<String, Integer> columns, int lineNumber, List<String> warnings) {
        return new SunatDetails(
            optional(cells, columns, TYPE),
            optional(cells, columns, TRADE_NAME),
            optionalDate(cells, columns, REGISTRATION_DATE, lineNumber, warnings),
            optional(cells, columns, DEPARTMENT),
            optional(cells, columns, PROVINCE),
            optional(cells, columns, DISTRICT),
            optional(cells, columns, PHONE),
            optional(cells, columns, MAIN_ACTIVITY),
            joined(cells, columns, SECONDARY_ACTIVITIES),
            optional(cells, columns, GOOD_TAXPAYER),
            optional(cells, columns, WITHHOLDING),
            joined(cells, columns, PERCEPTION));
    }

    /** Valor de una columna que puede no existir ni traer dato. */
    private static String optional(String[] cells, Map<String, Integer> columns, String key) {
        Integer index = columns.get(key);
        return index == null || index >= cells.length ? null : clean(cells[index]);
    }

    private static LocalDate optionalDate(String[] cells, Map<String, Integer> columns, String key, int lineNumber, List<String> warnings) {
        Integer index = columns.get(key);
        return index == null || index >= cells.length ? null : parseDate(cells[index], lineNumber, warnings);
    }

    /** Une varias columnas con datos (por ejemplo las dos actividades secundarias) en un solo texto. */
    private static String joined(String[] cells, Map<String, Integer> columns, List<String> keys) {
        List<String> values = new ArrayList<>();
        for (String key : keys) {
            String value = optional(cells, columns, key);
            if (value != null) values.add(value);
        }
        return values.isEmpty() ? null : String.join(" · ", values);
    }

    /** UTF-8 estricto; si el archivo no lo es, Windows-1252 (codificación habitual de archivos de SUNAT). */
    private static String decode(byte[] bytes) {
        String text;
        try {
            text = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) {
            text = new String(bytes, WINDOWS_1252);
        }
        return text.startsWith("﻿") ? text.substring(1) : text;
    }

    private static int firstNonBlank(String[] lines) {
        for (int i = 0; i < lines.length; i++) {
            if (!lines[i].isBlank()) return i;
        }
        return -1;
    }

    /** Separa por {@code |} conservando las celdas vacías (el {@code |} final genera una celda sobrante). */
    private static String[] split(String line) {
        String[] raw = line.split("\\|", -1);
        for (int i = 0; i < raw.length; i++) raw[i] = raw[i].trim();
        return raw;
    }

    private static Map<String, Integer> indexColumns(String[] header) {
        Map<String, Integer> columns = new HashMap<>();
        for (int i = 0; i < header.length; i++) {
            columns.putIfAbsent(normalizeHeader(header[i]), i);
        }
        return columns;
    }

    /** "Nombre ó RazonSocial" y "Nombre o Razon Social" dan la misma clave. */
    private static String normalizeHeader(String header) {
        return Normalizer.normalize(header.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "").replaceAll("[^a-z0-9]", "");
    }

    /** Recorta, junta espacios repetidos y convierte el "-" (sin dato) en nulo. */
    private static String clean(String value) {
        String text = value == null ? "" : value.replaceAll("\\s+", " ").trim();
        return text.isEmpty() || "-".equals(text) ? null : text;
    }

    /**
     * Arma la dirección fiscal completa: SUNAT entrega la vía por un lado y distrito, provincia y
     * departamento por otro. El "(.)" es el relleno de SUNAT para "sin dato" y se quita.
     */
    private static String address(String street, String district, String province, String department) {
        List<String> parts = new ArrayList<>();
        String cleanStreet = clean(street == null ? null : street.replace("(.)", " "));
        if (cleanStreet != null) parts.add(cleanStreet);
        for (String part : new String[] {district, province, department}) {
            String value = clean(part);
            if (value != null) parts.add(value);
        }
        return parts.isEmpty() ? null : String.join(", ", parts);
    }

    private static LocalDate parseDate(String value, int lineNumber, List<String> warnings) {
        String text = clean(value);
        if (text == null) return null;
        try {
            return LocalDate.parse(text, DATE);
        } catch (DateTimeParseException e) {
            warnings.add("Línea " + lineNumber + ": la fecha de inicio '" + text + "' no tiene el formato dd/MM/yyyy.");
            return null;
        }
    }
}

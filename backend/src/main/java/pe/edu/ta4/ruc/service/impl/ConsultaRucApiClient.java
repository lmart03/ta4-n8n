package pe.edu.ta4.ruc.service.impl;

import java.net.http.HttpClient;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import pe.edu.ta4.ruc.RucApiProperties;
import pe.edu.ta4.ruc.model.SunatDetails;
import pe.edu.ta4.ruc.model.SunatRecord;
import pe.edu.ta4.ruc.service.RucApiClient;
import pe.edu.ta4.shared.error.ApiException;

/** Cliente de la API de consultaderuc.pe (GET /api/v1/ruc/{ruc} con token Bearer). */
@Service
public class ConsultaRucApiClient implements RucApiClient {
    private static final ParameterizedTypeReference<Map<String, Object>> JSON = new ParameterizedTypeReference<>() { };

    private final RucApiProperties properties;
    private final RestClient http;

    public ConsultaRucApiClient(RucApiProperties properties) {
        this.properties = properties;
        HttpClient client = HttpClient.newBuilder().connectTimeout(properties.timeout()).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(client);
        factory.setReadTimeout(properties.timeout());
        this.http = RestClient.builder().baseUrl(properties.baseUrl()).requestFactory(factory).build();
    }

    @Override
    public boolean configured() {
        return properties.configured();
    }

    @Override
    public boolean autoFallback() {
        return properties.autoFallback();
    }

    @Override
    public String sourceName() {
        return "API consultaderuc.pe";
    }

    @Override
    public Optional<SunatRecord> lookup(String ruc) {
        try {
            Map<String, Object> body = http.get().uri("/api/v1/ruc/{ruc}", ruc)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.token())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve().body(JSON);
            Object data = body == null ? null : body.get("data");
            return data instanceof Map<?, ?> map ? Optional.of(toRecord(castMap(map))) : Optional.empty();
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            if (status == 404) return Optional.empty();
            if (status == 401 || status == 403) {
                throw ApiException.unavailable("El token de la API de consulta de RUC no es válido o su cuenta no tiene un plan activo.");
            }
            if (status == 429) {
                throw ApiException.unavailable("Se agotó la cuota o el límite por minuto de la API de consulta de RUC. Espera un momento y reintenta.");
            }
            throw ApiException.unavailable("La API de consulta de RUC respondió con un error (" + status + ").");
        } catch (ResourceAccessException e) {
            throw ApiException.unavailable("No se pudo contactar con la API de consulta de RUC. Revisa tu conexión.");
        }
    }

    /** Traduce la ficha de la API al registro común. Visible para pruebas. */
    static SunatRecord toRecord(Map<String, Object> data) {
        Map<String, Object> profile = castMap(data.get("perfil"));
        String department = upper(text(data.get("departamento")));
        String province = upper(text(data.get("provincia")));
        String district = upper(text(data.get("distrito")));
        SunatDetails details = new SunatDetails(
            upper(text(profile.get("tipo_contribuyente"))),
            null,
            null,
            department, province, district,
            null,
            upper(text(profile.get("actividad_economica"))),
            upper(text(profile.get("actividad_economica_secundaria"))),
            null,
            designations(data, "retencion"),
            designations(data, "percepcion"));
        return new SunatRecord(
            text(data.get("ruc")),
            text(data.get("razon_social")),
            address(text(data.get("direccion")), district, province, department),
            upper(text(data.get("condicion_domicilio"))),
            upper(text(data.get("estado"))),
            date(data.get("fecha_inicio_actividades")),
            details);
    }

    /** La dirección fiscal completa, armada igual que con el TXT de SUNAT: vía, distrito, provincia, departamento. */
    private static String address(String street, String district, String province, String department) {
        List<String> parts = new ArrayList<>();
        for (String part : new String[] {street == null ? null : street.toUpperCase(Locale.ROOT), district, province, department}) {
            if (part != null) parts.add(part);
        }
        return parts.isEmpty() ? null : String.join(", ", parts);
    }

    /** Textos de las designaciones de agente cuyo tipo empieza por {@code prefix} (retención / percepción). */
    private static String designations(Map<String, Object> data, String prefix) {
        Object block = data.get("agente_igv");
        if (!(block instanceof Map<?, ?> agent) || !(agent.get("designaciones") instanceof List<?> list)) return null;
        List<String> found = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> designation && String.valueOf(designation.get("tipo")).startsWith(prefix)) {
                String description = text(designation.get("descripcion"));
                if (description != null) found.add(description);
            }
        }
        return found.isEmpty() ? null : String.join(" · ", found);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    /** Recorta y junta espacios; vacío o "-" es nulo. */
    private static String text(Object value) {
        if (value == null) return null;
        String text = String.valueOf(value).replaceAll("\\s+", " ").trim();
        return text.isEmpty() || "-".equals(text) ? null : text;
    }

    private static String upper(String value) {
        return value == null ? null : value.toUpperCase(Locale.ROOT);
    }

    private static LocalDate date(Object value) {
        String text = text(value);
        if (text == null) return null;
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}

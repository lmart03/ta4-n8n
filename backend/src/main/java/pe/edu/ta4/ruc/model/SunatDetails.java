package pe.edu.ta4.ruc.model;

import java.time.LocalDate;

/**
 * Datos adicionales que trae el TXT de la Consulta Múltiple, además de lo básico. Todos son opcionales: SUNAT
 * entrega "-" cuando no hay dato (aquí queda como nulo) y una columna ausente tampoco rompe la lectura.
 *
 * <p>La dirección fiscal completa ya viene armada en {@link SunatRecord}; aquí se conserva la ubicación por partes.
 */
public record SunatDetails(String tipoContribuyente, String nombreComercial, LocalDate fechaInscripcion,
                           String departamento, String provincia, String distrito, String telefono,
                           String actividadPrincipal, String actividadesSecundarias, String buenContribuyente,
                           String agenteRetencion, String agentePercepcion) {

    public static final SunatDetails EMPTY =
        new SunatDetails(null, null, null, null, null, null, null, null, null, null, null, null);
}

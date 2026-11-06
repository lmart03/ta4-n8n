package pe.edu.ta4.ruc.model;

import java.time.LocalDate;

/**
 * Un registro del TXT de resultados de la Consulta Múltiple de SUNAT, ya limpio.
 *
 * <p>{@code condicion} es la "Condición del Contribuyente" (HABIDO o NO HABIDO); {@code estado} es el
 * "Estado del Contribuyente" (ACTIVO, BAJA...) y no debe confundirse con la condición. {@code detalles} reúne el
 * resto de columnas útiles (tipo, nombre comercial, ubicación, actividades, agentes...).
 */
public record SunatRecord(String ruc, String razonSocial, String direccionFiscal, String condicion,
                          String estado, LocalDate fechaInicioActividades, SunatDetails detalles) {

    public SunatRecord {
        if (detalles == null) detalles = SunatDetails.EMPTY;
    }

    /** Registro sin datos adicionales. */
    public SunatRecord(String ruc, String razonSocial, String direccionFiscal, String condicion, String estado,
                       LocalDate fechaInicioActividades) {
        this(ruc, razonSocial, direccionFiscal, condicion, estado, fechaInicioActividades, SunatDetails.EMPTY);
    }
}

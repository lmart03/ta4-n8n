package pe.edu.ta4.ruc.dto;

import java.time.LocalDate;
import pe.edu.ta4.ruc.model.ItemStatus;
import pe.edu.ta4.ruc.model.RucJobItem;

/**
 * Una fila del resultado. {@code detalle} reúne los datos adicionales de SUNAT (tipo, nombre comercial, ubicación,
 * actividades, agentes...) y es nulo mientras el RUC no tenga datos.
 */
public record RucJobItemResponse(int excelRow, String ruc, ItemStatus status, String razonSocial,
                                 String direccionFiscal, String condicionContribuyente,
                                 String estadoContribuyente, LocalDate fechaInicioActividades, String observation,
                                 Detail detalle) {

    /** Datos adicionales de SUNAT; cada uno puede ser nulo cuando SUNAT no lo informa. */
    public record Detail(String tipoContribuyente, String nombreComercial, LocalDate fechaInscripcion,
                         String departamento, String provincia, String distrito, String telefono,
                         String actividadPrincipal, String actividadesSecundarias, String buenContribuyente,
                         String agenteRetencion, String agentePercepcion) {
    }

    /** Una fila sin datos adicionales. */
    public RucJobItemResponse(int excelRow, String ruc, ItemStatus status, String razonSocial,
                              String direccionFiscal, String condicionContribuyente,
                              String estadoContribuyente, LocalDate fechaInicioActividades, String observation) {
        this(excelRow, ruc, status, razonSocial, direccionFiscal, condicionContribuyente, estadoContribuyente,
            fechaInicioActividades, observation, null);
    }

    public static RucJobItemResponse from(RucJobItem item) {
        Detail detail = item.getStatus() == ItemStatus.ENCONTRADO
            ? new Detail(item.getTipoContribuyente(), item.getNombreComercial(), item.getFechaInscripcion(),
                item.getDepartamento(), item.getProvincia(), item.getDistrito(), item.getTelefono(),
                item.getActividadPrincipal(), item.getActividadesSecundarias(), item.getBuenContribuyente(),
                item.getAgenteRetencion(), item.getAgentePercepcion())
            : null;
        return new RucJobItemResponse(item.getExcelRow(), item.getRuc(), item.getStatus(), item.getRazonSocial(),
            item.getDireccionFiscal(), item.getCondicionContribuyente(), item.getEstadoContribuyente(),
            item.getFechaInicioActividades(), item.getObservation(), detail);
    }
}

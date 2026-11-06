package pe.edu.ta4.business.dto;

import pe.edu.ta4.business.model.BusinessInfo;

/** Datos de la clínica expuestos por la API. No incluye ids ni fechas internas. */
public record BusinessResponse(String businessName, String description, String address, String district,
                               String city, String contactPhone, String appointmentInstructions) {

    public static BusinessResponse from(BusinessInfo info) {
        return new BusinessResponse(info.getBusinessName(), info.getDescription(), info.getAddress(),
            info.getDistrict(), info.getCity(), info.getContactPhone(), info.getAppointmentInstructions());
    }
}

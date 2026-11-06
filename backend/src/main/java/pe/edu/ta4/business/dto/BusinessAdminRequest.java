package pe.edu.ta4.business.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BusinessAdminRequest(
    @NotBlank @Size(max = 200) String businessName,
    @NotBlank @Size(max = 1000) String description,
    @NotBlank @Size(max = 300) String address,
    @NotBlank @Size(max = 100) String district,
    @NotBlank @Size(max = 100) String city,
    @Size(max = 50) String contactPhone,
    @NotBlank @Size(max = 1000) String appointmentInstructions) {
}

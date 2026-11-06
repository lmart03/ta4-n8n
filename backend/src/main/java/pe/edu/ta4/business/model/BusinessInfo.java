package pe.edu.ta4.business.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Datos generales del negocio (tabla {@code business_info}). */
@Entity
@Table(name = "business_info")
public class BusinessInfo {
    @Id
    private UUID id;
    @Column(name = "business_name")
    private String businessName;
    private String description;
    private String address;
    private String district;
    private String city;
    @Column(name = "contact_phone")
    private String contactPhone;
    @Column(name = "appointment_instructions")
    private String appointmentInstructions;
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    protected BusinessInfo() {
        // requerido por JPA
    }

    public void update(String businessName, String description, String address, String district, String city,
                       String contactPhone, String appointmentInstructions) {
        this.businessName = businessName;
        this.description = description;
        this.address = address;
        this.district = district;
        this.city = city;
        this.contactPhone = contactPhone;
        this.appointmentInstructions = appointmentInstructions;
        this.updatedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public String getBusinessName() { return businessName; }
    public String getDescription() { return description; }
    public String getAddress() { return address; }
    public String getDistrict() { return district; }
    public String getCity() { return city; }
    public String getContactPhone() { return contactPhone; }
    public String getAppointmentInstructions() { return appointmentInstructions; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}

package pe.edu.ta4.ruc.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

/** Un RUC solicitado dentro de un trabajo; se completa con los datos de SUNAT al llegar el resultado. */
@Entity
@Table(name = "ruc_job_item")
public class RucJobItem {
    @Id
    private UUID id;
    @Column(name = "job_id")
    private UUID jobId;
    @Column(name = "excel_row")
    private int excelRow;
    private String ruc;
    @Enumerated(EnumType.STRING)
    @Column(name = "item_status")
    private ItemStatus status;
    @Column(name = "razon_social")
    private String razonSocial;
    @Column(name = "direccion_fiscal")
    private String direccionFiscal;
    @Column(name = "condicion_contribuyente")
    private String condicionContribuyente;
    @Column(name = "estado_contribuyente")
    private String estadoContribuyente;
    @Column(name = "fecha_inicio_actividades")
    private LocalDate fechaInicioActividades;
    private String observation;
    @Column(name = "tipo_contribuyente")
    private String tipoContribuyente;
    @Column(name = "nombre_comercial")
    private String nombreComercial;
    @Column(name = "fecha_inscripcion")
    private LocalDate fechaInscripcion;
    private String departamento;
    private String provincia;
    private String distrito;
    private String telefono;
    @Column(name = "actividad_principal")
    private String actividadPrincipal;
    @Column(name = "actividades_secundarias")
    private String actividadesSecundarias;
    @Column(name = "buen_contribuyente")
    private String buenContribuyente;
    @Column(name = "agente_retencion")
    private String agenteRetencion;
    @Column(name = "agente_percepcion")
    private String agentePercepcion;

    protected RucJobItem() {
        // requerido por JPA
    }

    public RucJobItem(UUID id, UUID jobId, int excelRow, String ruc) {
        this.id = id;
        this.jobId = jobId;
        this.excelRow = excelRow;
        this.ruc = ruc;
        this.status = ItemStatus.PENDIENTE;
    }

    public void markFound(SunatRecord record) {
        this.status = ItemStatus.ENCONTRADO;
        this.razonSocial = record.razonSocial();
        this.direccionFiscal = record.direccionFiscal();
        this.condicionContribuyente = record.condicion();
        this.estadoContribuyente = record.estado();
        this.fechaInicioActividades = record.fechaInicioActividades();
        SunatDetails details = record.detalles();
        this.tipoContribuyente = details.tipoContribuyente();
        this.nombreComercial = details.nombreComercial();
        this.fechaInscripcion = details.fechaInscripcion();
        this.departamento = details.departamento();
        this.provincia = details.provincia();
        this.distrito = details.distrito();
        this.telefono = details.telefono();
        this.actividadPrincipal = details.actividadPrincipal();
        this.actividadesSecundarias = details.actividadesSecundarias();
        this.buenContribuyente = details.buenContribuyente();
        this.agenteRetencion = details.agenteRetencion();
        this.agentePercepcion = details.agentePercepcion();
        this.observation = null;
    }

    public void markNotFound(String observation) {
        this.status = ItemStatus.NO_ENCONTRADO;
        this.observation = observation;
    }

    public void addObservation(String text) {
        this.observation = observation == null ? text : observation + " " + text;
    }

    public UUID getId() { return id; }
    public UUID getJobId() { return jobId; }
    public int getExcelRow() { return excelRow; }
    public String getRuc() { return ruc; }
    public ItemStatus getStatus() { return status; }
    public String getRazonSocial() { return razonSocial; }
    public String getDireccionFiscal() { return direccionFiscal; }
    public String getCondicionContribuyente() { return condicionContribuyente; }
    public String getEstadoContribuyente() { return estadoContribuyente; }
    public LocalDate getFechaInicioActividades() { return fechaInicioActividades; }
    public String getObservation() { return observation; }
    public String getTipoContribuyente() { return tipoContribuyente; }
    public String getNombreComercial() { return nombreComercial; }
    public LocalDate getFechaInscripcion() { return fechaInscripcion; }
    public String getDepartamento() { return departamento; }
    public String getProvincia() { return provincia; }
    public String getDistrito() { return distrito; }
    public String getTelefono() { return telefono; }
    public String getActividadPrincipal() { return actividadPrincipal; }
    public String getActividadesSecundarias() { return actividadesSecundarias; }
    public String getBuenContribuyente() { return buenContribuyente; }
    public String getAgenteRetencion() { return agenteRetencion; }
    public String getAgentePercepcion() { return agentePercepcion; }
}

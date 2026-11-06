-- Trabajos de consulta múltiple de RUC (Parte 1): un trabajo por Excel cargado.
CREATE TABLE ruc_job (
  id uuid PRIMARY KEY,
  status varchar(40) NOT NULL CHECK (status IN (
    'RECIBIDO','VALIDANDO','LISTO_PARA_ROBOT','EN_CONSULTA','REQUIERE_INTERVENCION',
    'PROCESANDO_RESULTADO','COMPLETADO','COMPLETADO_CON_OBSERVACIONES','FALLIDO')),
  total integer NOT NULL CHECK (total > 0),
  original_filename varchar(255) NOT NULL,
  found_count integer NOT NULL DEFAULT 0,
  missing_count integer NOT NULL DEFAULT 0,
  unexpected_count integer NOT NULL DEFAULT 0,
  summary varchar(1000),
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  completed_at timestamptz
);
CREATE INDEX idx_ruc_job_created ON ruc_job(created_at DESC);

-- Una fila por RUC solicitado; se completa cuando llega el resultado de SUNAT.
CREATE TABLE ruc_job_item (
  id uuid PRIMARY KEY,
  job_id uuid NOT NULL REFERENCES ruc_job(id) ON DELETE CASCADE,
  excel_row integer NOT NULL,
  ruc varchar(11) NOT NULL,
  item_status varchar(20) NOT NULL CHECK (item_status IN ('PENDIENTE','ENCONTRADO','NO_ENCONTRADO')),
  razon_social varchar(300),
  direccion_fiscal varchar(600),
  condicion_contribuyente varchar(60),
  estado_contribuyente varchar(60),
  fecha_inicio_actividades date,
  observation varchar(500),
  CONSTRAINT uq_ruc_job_item_ruc UNIQUE (job_id, ruc),
  CONSTRAINT uq_ruc_job_item_row UNIQUE (job_id, excel_row)
);
CREATE INDEX idx_ruc_job_item_job ON ruc_job_item(job_id, excel_row);

-- Historial de estados y fallos del trabajo (trazabilidad, reintentos).
CREATE TABLE ruc_job_event (
  id uuid PRIMARY KEY,
  job_id uuid NOT NULL REFERENCES ruc_job(id) ON DELETE CASCADE,
  seq_no integer NOT NULL,
  occurred_at timestamptz NOT NULL,
  from_status varchar(40),
  to_status varchar(40) NOT NULL,
  message varchar(500),
  CONSTRAINT uq_ruc_job_event_seq UNIQUE (job_id, seq_no)
);

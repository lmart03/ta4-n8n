-- Datos adicionales que devuelve la Consulta Múltiple de SUNAT, además de los básicos. Todos opcionales.
ALTER TABLE ruc_job_item
    ADD COLUMN tipo_contribuyente      text,
    ADD COLUMN nombre_comercial        text,
    ADD COLUMN fecha_inscripcion       date,
    ADD COLUMN departamento            text,
    ADD COLUMN provincia               text,
    ADD COLUMN distrito                text,
    ADD COLUMN telefono                text,
    ADD COLUMN actividad_principal     text,
    ADD COLUMN actividades_secundarias text,
    ADD COLUMN buen_contribuyente      text,
    ADD COLUMN agente_retencion        text,
    ADD COLUMN agente_percepcion       text;

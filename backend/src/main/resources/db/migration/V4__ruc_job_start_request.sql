-- Arranque del robot pedido desde la web: el lanzador local lo recoge de la cola y lo marca como tomado.
ALTER TABLE ruc_job ADD COLUMN start_requested_at timestamptz;
CREATE INDEX idx_ruc_job_start_requested ON ruc_job(start_requested_at) WHERE start_requested_at IS NOT NULL;

package pe.edu.ta4.ruc.service;

import java.util.UUID;
import pe.edu.ta4.ruc.model.StoredFile;

/** Almacenamiento de los archivos de cada trabajo. Hoy es disco local; podría ser otro servicio. */
public interface FileStorage {
    void write(UUID jobId, StoredFile file, byte[] content);

    /** Lee un archivo; falla con 404 si no existe y con 503 si el almacenamiento no responde. */
    byte[] read(UUID jobId, StoredFile file);

    boolean exists(UUID jobId, StoredFile file);

    /** Borra todos los archivos del trabajo (limpieza si falla su creación). Nunca lanza. */
    void deleteJob(UUID jobId);
}

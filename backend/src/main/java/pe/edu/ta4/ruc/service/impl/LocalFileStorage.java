package pe.edu.ta4.ruc.service.impl;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pe.edu.ta4.ruc.RucProperties;
import pe.edu.ta4.ruc.model.StoredFile;
import pe.edu.ta4.ruc.service.FileStorage;
import pe.edu.ta4.shared.error.ApiException;

/**
 * Guarda {@code <storage-dir>/<jobId>/<archivo>}. La ruta se arma solo con un {@link UUID} y un nombre fijo
 * del enum {@link StoredFile}, así que nunca depende de texto que envíe el cliente.
 */
@Service
public class LocalFileStorage implements FileStorage {
    private static final Logger log = LoggerFactory.getLogger(LocalFileStorage.class);

    private final Path root;

    public LocalFileStorage(RucProperties properties) {
        this.root = properties.storageDir().toAbsolutePath().normalize();
    }

    @Override
    public void write(UUID jobId, StoredFile file, byte[] content) {
        try {
            Path directory = jobDirectory(jobId);
            Files.createDirectories(directory);
            Files.write(directory.resolve(file.fileName()), content);
        } catch (IOException e) {
            log.error("No se pudo guardar {} del trabajo {}", file, jobId, e);
            throw ApiException.unavailable("No se pudo guardar el archivo en el almacenamiento.");
        }
    }

    @Override
    public byte[] read(UUID jobId, StoredFile file) {
        Path path = jobDirectory(jobId).resolve(file.fileName());
        if (!Files.isRegularFile(path)) {
            throw ApiException.notFound("El archivo solicitado del trabajo no existe.");
        }
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            log.error("No se pudo leer {} del trabajo {}", file, jobId, e);
            throw ApiException.unavailable("No se pudo leer el archivo del almacenamiento.");
        }
    }

    @Override
    public boolean exists(UUID jobId, StoredFile file) {
        return Files.isRegularFile(jobDirectory(jobId).resolve(file.fileName()));
    }

    @Override
    public void deleteJob(UUID jobId) {
        Path directory = jobDirectory(jobId);
        if (!Files.exists(directory)) return;
        try (Stream<Path> paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (IOException | UncheckedIOException e) {
            log.warn("No se pudo limpiar la carpeta del trabajo {}", jobId, e);
        }
    }

    private Path jobDirectory(UUID jobId) {
        return root.resolve(jobId.toString());
    }
}

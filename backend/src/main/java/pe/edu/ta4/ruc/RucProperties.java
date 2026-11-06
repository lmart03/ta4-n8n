package pe.edu.ta4.ruc;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuración de la consulta múltiple de RUC ({@code ta4.ruc.*}).
 *
 * @param storageDir        carpeta donde se conservan los archivos de cada trabajo
 * @param maxRows           máximo de RUC por Excel (20 en la práctica; SUNAT admite hasta 100)
 * @param maxResultTxtBytes máximo descomprimido del TXT de resultados (defensa contra zip-bombs)
 * @param maxZipEntries     máximo de entradas aceptadas dentro del ZIP de resultados
 */
@ConfigurationProperties(prefix = "ta4.ruc")
public record RucProperties(
    @DefaultValue("./ruc-jobs") Path storageDir,
    @DefaultValue("20") int maxRows,
    @DefaultValue("5000000") long maxResultTxtBytes,
    @DefaultValue("20") int maxZipEntries) {
}

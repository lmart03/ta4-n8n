package pe.edu.ta4;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
@OpenAPIDefinition(info = @Info(title = "TA4 API", version = "1.0.0",
    description = "Parte 1: consulta múltiple de RUC (Excel → TXT para SUNAT → ZIP de resultados → Excel). "
        + "Parte 2: FAQ de la Clínica Soraka (ficticia) para el chatbot. Los errores siguen RFC 9457 (application/problem+json)."))
public class Ta4Application {
    public static void main(String[] args) {
        SpringApplication.run(Ta4Application.class, args);
    }
}

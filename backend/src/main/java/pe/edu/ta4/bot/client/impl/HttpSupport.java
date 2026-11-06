package pe.edu.ta4.bot.client.impl;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Construye clientes HTTP con tiempos de espera cortos para los servicios del bot. */
final class HttpSupport {
    private HttpSupport() {
    }

    static RestClient client(String baseUrl, Duration timeout) {
        HttpClient http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(timeout).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(timeout);
        return RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }
}

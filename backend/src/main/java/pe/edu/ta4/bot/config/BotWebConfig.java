package pe.edu.ta4.bot.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class BotWebConfig implements WebMvcConfigurer {
    private final BotAccessInterceptor interceptor;

    public BotWebConfig(BotAccessInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor).addPathPatterns("/api/v1/bot/**", "/api/v1/admin/**");
    }
}

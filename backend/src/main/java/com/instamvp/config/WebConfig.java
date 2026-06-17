package com.instamvp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Libera CORS para a API (/api/**). Necessário porque o app Flutter, quando
 * rodado como web (`flutter run -d chrome`), serve a partir de uma porta
 * diferente da do backend (ex: localhost:5000 vs localhost:8080) — sem isso
 * o navegador bloqueia as chamadas fetch/XHR com erro de CORS, que parece
 * "não consegue conectar" mas na verdade é o preflight sendo recusado.
 *
 * Liberado para qualquer origem só porque é um projeto local/acadêmico sem
 * autenticação; não use isso como está em produção com dados sensíveis.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}

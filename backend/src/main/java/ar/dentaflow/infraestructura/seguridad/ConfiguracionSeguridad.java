package ar.dentaflow.infraestructura.seguridad;

import jakarta.servlet.DispatcherType;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Única cadena de seguridad de la API (C-04, design D8): sin estado, sin cookies ni CSRF, token JWT
 * Bearer y denegación por defecto. Solo son públicas las rutas listadas acá; todo endpoint nuevo nace
 * protegido y declara su guarda de rol ({@code infraestructura.seguridad.guardas}).
 */
@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
public class ConfiguracionSeguridad {

    /** Rutas accesibles sin autenticación (spec autorizacion-por-rol, "Rutas públicas"). */
    static final String[] DOCUMENTACION = {"/v3/api-docs", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html"};

    /**
     * Solo el origen del frontend ({@code APP_BASE_URL}, o la lista de {@code app.cors.origenes-permitidos})
     * puede llamar a la API desde el navegador con el header {@code Authorization}. Sin credenciales de
     * navegador: el token viaja en el header, nunca en cookies (spec "Origen del frontend permitido").
     */
    @Bean
    CorsConfigurationSource origenesPermitidos(@Value("${app.cors.origenes-permitidos}") List<String> origenes) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(origenes.stream().map(String::strip).filter(o -> !o.isEmpty()).toList());
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE, HttpHeaders.ACCEPT));
        cors.setExposedHeaders(List.of(HttpHeaders.LOCATION, HttpHeaders.RETRY_AFTER));
        cors.setAllowCredentials(false);
        cors.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/**", cors);
        return fuente;
    }

    @Bean
    SecurityFilterChain cadenaDeSeguridad(HttpSecurity http, ConversorDeJwtAUsuario conversor,
            RespuestasDeSeguridad respuestas, CorsConfigurationSource origenesPermitidos) throws Exception {
        http
                .cors(cors -> cors.configurationSource(origenesPermitidos))
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(rutas -> rutas
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(DOCUMENTACION).permitAll()
                        .requestMatchers("/api/publico/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(servidor -> servidor
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(conversor))
                        .authenticationEntryPoint(respuestas)
                        .accessDeniedHandler(respuestas))
                .exceptionHandling(excepciones -> excepciones
                        .authenticationEntryPoint(respuestas)
                        .accessDeniedHandler(respuestas));
        return http.build();
    }
}

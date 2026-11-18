package com.hospital.gestion.security;

import com.hospital.gestion.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtService jwt;
    private final UsuarioRepository usuarios;

    public SecurityConfig(JwtService jwt, UsuarioRepository usuarios) {
        this.jwt = jwt;
        this.usuarios = usuarios;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> {
                a.requestMatchers("/", "/index.html", "/favicon.ico", "/error",
                        "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll();
                a.requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll();
                a.requestMatchers("/api/auth/**").authenticated();
                a.requestMatchers("/api/mi/**").authenticated();
                a.requestMatchers("/api/panel/**").hasAnyRole(Grupos.ADMIN, Grupos.DIRECTOR);
                a.requestMatchers("/api/chatbot/**").authenticated();   // autoservicio: cada quien ve solo lo suyo
                // Una regla GET, una DELETE y el resto (POST/PUT/PATCH) por cada recurso
                Permisos.REGLAS.forEach((recurso, r) -> {
                    String ruta = "/api/" + recurso + "/**";
                    a.requestMatchers(HttpMethod.GET, ruta).hasAnyRole(r.leer());
                    a.requestMatchers(HttpMethod.DELETE, ruta).hasAnyRole(r.eliminar());
                    a.requestMatchers(ruta).hasAnyRole(r.escribir());
                });
                a.anyRequest().denyAll();
            })
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) -> json(res, 401, "No autenticado: inicie sesión"))
                .accessDeniedHandler((req, res, ex) -> json(res, 403, "No tiene permiso para esta acción")))
            .addFilterBefore(new JwtAuthFilter(jwt, usuarios), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private static void json(HttpServletResponse res, int status, String mensaje) throws IOException {
        res.setStatus(status);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write("{\"status\":" + status + ",\"mensaje\":\"" + mensaje + "\"}");
    }
}

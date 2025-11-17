    package com.example.iTDS.config;


    import jakarta.servlet.http.HttpServletResponse;
    import org.springframework.context.annotation.Bean;
    import org.springframework.context.annotation.Configuration;
    import org.springframework.security.authentication.AuthenticationManager;
    import org.springframework.security.authentication.AuthenticationManagerResolver;
    import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
    import org.springframework.security.config.annotation.web.builders.HttpSecurity;
    import org.springframework.security.oauth2.jwt.JwtDecoder;
    import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
    import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
    import org.springframework.security.web.SecurityFilterChain;
    import org.springframework.web.cors.CorsConfiguration;
    import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
    import org.springframework.web.filter.CorsFilter;

    import javax.crypto.spec.SecretKeySpec;
    import jakarta.servlet.http.HttpServletRequest;
    import java.util.Arrays;

    @Configuration
    public class SecurityConfig {

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http.csrf(csrf -> csrf.disable()) // Disable CSRF for stateless APIs
                    .authorizeHttpRequests(auth -> auth
                            // Permit public access to register and login
                            .requestMatchers("/api/auth/register", "/api/auth/login","https://activustdstest1-kappa.vercel.app/login","https://activus-omega.vercel.app/","https://activus-omega.vercel.app/login","https://activustdstest1-shyamyobels-projects.vercel.app/login"
                                    ,"https://activustdstest1-shyamyobels-projects.vercel.app/register", "https://activustdstest1-shyamyobels-projects.vercel.app/register").permitAll()
                            // Authenticate all other endpoints
                            .anyRequest().authenticated()
                    )
                    // OAuth2 Resource Server with Authentication Manager Resolver
                    .oauth2ResourceServer(oauth2 -> oauth2
                            .authenticationManagerResolver(authenticationManagerResolver())
                    )
                    // Exception handling for unauthorized access
                    .exceptionHandling(exception -> exception
                            .authenticationEntryPoint((request, response, authException) -> {
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
                            })
                    )
                    // Configure CORS for frontend access
                    .cors(cors -> cors.configurationSource(corsConfigurationSource()));

            return http.build();
        }

        @Bean
        public UrlBasedCorsConfigurationSource corsConfigurationSource() {
            CorsConfiguration corsConfiguration = new CorsConfiguration();
            corsConfiguration.setAllowedOrigins(Arrays.asList("http://localhost:3000", "https://activustdstest1-kappa.vercel.app","https://activustdstest1-shyamyobels-projects.vercel.app","https://activus-server-production.up.railway.app")); // Frontend origin
            corsConfiguration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
            corsConfiguration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
            corsConfiguration.setAllowCredentials(true);

            UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
            source.registerCorsConfiguration("/**", corsConfiguration);
            return source;
        }

        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
            return authenticationConfiguration.getAuthenticationManager();
        }

        @Bean
        public JwtDecoder jwtDecoder() {
            // Define secret key for JWT decoding
            return NimbusJwtDecoder.withSecretKey(new SecretKeySpec(
                    "mySuperLongSecureSecretKey12345!".getBytes(), "HmacSHA256")).build();
        }

        @Bean
        public AuthenticationManagerResolver<HttpServletRequest> authenticationManagerResolver() {
            JwtAuthenticationProvider jwtAuthenticationProvider = new JwtAuthenticationProvider(jwtDecoder());
            return request -> jwtAuthenticationProvider::authenticate;
        }
    }

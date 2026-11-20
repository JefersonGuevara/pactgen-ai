package com.piggymetrics.auth;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Configuración raíz para @WebMvcTest. Sustituye a AuthApplication, cuyas anotaciones (Eureka, Feign, OAuth2,
 * seguridad de métodos) no afectan al contrato. Archivo de PactGen-AI; no forma parte de piggymetrics.
 */
@SpringBootApplication
public class AuthVerificationApplication {
}

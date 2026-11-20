package com.piggymetrics.statistics;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Configuración raíz para @WebMvcTest. Sustituye a StatisticsApplication, cuyas anotaciones (Eureka, Feign, OAuth2,
 * seguridad de métodos) no afectan al contrato. Archivo de PactGen-AI; no forma parte de piggymetrics.
 */
@SpringBootApplication
public class StatisticsVerificationApplication {
}

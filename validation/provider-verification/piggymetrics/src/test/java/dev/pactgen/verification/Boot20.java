package dev.pactgen.verification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import jakarta.servlet.FilterChain;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.context.annotation.Bean;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.util.WebUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Comportamiento de Spring Boot 2.0.3 que piggymetrics (6bb2cf9) tiene en producción y que el portado a
 * Spring Boot 3.4.1 cambiaría. Solo se reproduce lo que se ve en el JSON o en el código de estado (ver NOTICE).
 */
public final class Boot20 {

    private Boot20() {}

    /** Configuración de prueba que se importa en cada clase de verificación de piggymetrics. */
    @TestConfiguration
    public static class Compat implements WebMvcConfigurer {

        /** Jackson 2.9 escribe las fechas como 2026-11-18T15:30:00.000+0000 (sin dos puntos en la zona). */
        @Bean
        Jackson2ObjectMapperBuilderCustomizer boot20Dates() {
            return b -> b.dateFormat(new StdDateFormat().withColonInTimeZone(false));
        }

        /** Spring 5.0 aceptaba la barra final (/accounts/ana/ = /accounts/ana); Spring 6 no, salvo que se active. */
        @Override
        @SuppressWarnings("deprecation")
        public void configurePathMatch(PathMatchConfigurer configurer) {
            configurer.setUseTrailingSlashMatch(true);
        }
    }

    /**
     * MockMvc con el context path del servicio (server.servlet.context-path en config/shared/*.yml), el
     * Principal autenticado (lo pondría OAuth2) y el filtro que emula la página de error del contenedor.
     */
    public static MockMvc mockMvc(WebApplicationContext ctx, String contextPath, Principal principal) {
        return MockMvcBuilders.webAppContextSetup(ctx)
                .defaultRequest(get("/").contextPath(contextPath).principal(principal))
                .addFilters(new ErrorPageFilter(ctx.getBean(ErrorAttributes.class), ctx.getBean(ObjectMapper.class)))
                .build();
    }

    /**
     * MockMvc no tiene contenedor de servlets: no hay despacho a /error. En Tomcat + Spring Boot 2.0, un
     * {@code sendError} (p. ej., fallo de {@code @Valid}) o una excepción no manejada (→ 500) terminan en
     * BasicErrorController, que responde un JSON con timestamp, status, error, message, path (y errors en los
     * fallos de validación). Este filtro escribe ese mismo cuerpo con el ErrorAttributes de Spring Boot.
     * Un {@code setStatus} sin cuerpo (el ErrorHandler de account-service) se deja tal cual, como en Tomcat.
     */
    static final class ErrorPageFilter extends OncePerRequestFilter {

        private final ErrorAttributes errorAttributes;
        private final ObjectMapper json;

        ErrorPageFilter(ErrorAttributes errorAttributes, ObjectMapper json) {
            this.errorAttributes = errorAttributes;
            this.json = json;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
                throws ServletException, IOException {
            Throwable failure = null;
            try {
                chain.doFilter(req, res);
            } catch (ServletException | RuntimeException e) {
                failure = e instanceof ServletException && e.getCause() != null ? e.getCause() : e;
            }
            MockHttpServletResponse mock = WebUtils.getNativeResponse(res, MockHttpServletResponse.class);
            if (mock == null) {
                if (failure != null) throw new ServletException(failure);
                return;
            }
            int status;
            if (failure != null) {
                status = 500;
                mock.setStatus(500);
                req.setAttribute(RequestDispatcher.ERROR_EXCEPTION, failure);
            } else if (mock.isCommitted() && mock.getStatus() >= 400 && mock.getContentAsByteArray().length == 0) {
                status = mock.getStatus(); // sendError
            } else {
                return;
            }
            req.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, status);
            req.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, req.getRequestURI());
            if (mock.getErrorMessage() != null) req.setAttribute(RequestDispatcher.ERROR_MESSAGE, mock.getErrorMessage());
            // Spring Boot 2.0 incluía siempre message y errors (server.error.include-message/-binding-errors son de 2.3);
            // defaults() aporta timestamp, status, error y path
            Map<String, Object> body = errorAttributes.getErrorAttributes(new ServletWebRequest(req),
                    ErrorAttributeOptions.defaults().including(ErrorAttributeOptions.Include.MESSAGE,
                            ErrorAttributeOptions.Include.BINDING_ERRORS));
            mock.setContentType("application/json;charset=UTF-8");
            mock.getOutputStream().write(json.writeValueAsString(body).getBytes(StandardCharsets.UTF_8));
        }
    }
}

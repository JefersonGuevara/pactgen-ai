package com.piggymetrics.auth.controller;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junitsupport.IgnoreMissingStateChange;
import au.com.dius.pact.provider.junitsupport.IgnoreNoPactsToVerify;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import au.com.dius.pact.provider.spring.junit5.MockMvcTestTarget;
import au.com.dius.pact.provider.spring.junit5.PactVerificationSpringProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.piggymetrics.auth.domain.User;
import com.piggymetrics.auth.service.UserService;
import dev.pactgen.verification.Boot20;
import dev.pactgen.verification.ContractFixtures;
import dev.pactgen.verification.ContractFixtures.Expected;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willThrow;

/**
 * Verificación de proveedor de auth-service (P5) contra los contratos de target/pacts-to-verify.
 * Nivel de controlador sobre el portado de piggymetrics 6bb2cf9 (ver NOTICE): UserService se simula y el
 * Principal autenticado se construye con los datos del contrato. Context path: /uaa.
 * Archivo añadido por PactGen-AI (validation/provider-verification); no forma parte del proyecto original.
 */
@WebMvcTest(UserController.class)
@Import(Boot20.Compat.class)
@Provider("auth-service")
@PactFolder("target/pacts-to-verify")
@IgnoreMissingStateChange
@IgnoreNoPactsToVerify // sin contratos de este proveedor en la carpeta, la clase se omite (mvn test sin fallos)
class AuthContractVerificationTest {

    /** Estado del contrato que corresponde a la IllegalArgumentException de UserServiceImpl.create (→ 500). */
    static final String ALREADY_EXISTS = "ya existe|\\bexistente|duplicad|already exist|ya est[aá] registrad|en uso";

    @Autowired
    WebApplicationContext ctx;

    @MockBean
    UserService users;

    @TestTemplate
    @ExtendWith(PactVerificationSpringProvider.class)
    void verify(PactVerificationContext context) {
        Assumptions.assumeTrue(context != null, "sin contratos de este proveedor");
        context.verifyInteraction();
    }

    @BeforeEach
    void setUp(PactVerificationContext context) {
        if (context == null) return;
        Expected e = ContractFixtures.expected(context.getInteraction());
        MockMvcTestTarget target = new MockMvcTestTarget();
        target.setMockMvc(Boot20.mockMvc(ctx, "/uaa", principal(e.responseBody())));
        context.setTarget(target);
        stub(e);
    }

    /**
     * En producción, /users/current devuelve el OAuth2Authentication del token. Aquí, un
     * UsernamePasswordAuthenticationToken autenticado (name, authenticated, authorities, principal...) con el
     * nombre y las autoridades que espera el contrato; ROLE_USER si el contrato no las indica.
     */
    private static UsernamePasswordAuthenticationToken principal(JsonNode body) {
        String name = body != null && body.hasNonNull("name") ? body.get("name").asText() : "demo";
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (body != null && body.path("authorities").isArray()) {
            body.get("authorities").forEach(a -> {
                if (a.hasNonNull("authority")) authorities.add(new SimpleGrantedAuthority(a.get("authority").asText()));
            });
        }
        if (authorities.isEmpty()) authorities.addAll(AuthorityUtils.createAuthorityList("ROLE_USER"));
        return UsernamePasswordAuthenticationToken.authenticated(name, null, authorities);
    }

    private void stub(Expected e) {
        // Precondiciones de UserServiceImpl.create, sin manejador (-> 500): repository.findById(null) lanza
        // IllegalArgumentException (Spring Data) y encoder.encode(null), NullPointerException (Spring Security 5.0)
        willAnswer(inv -> {
            User u = inv.getArgument(0);
            if (u.getUsername() == null) throw new IllegalArgumentException("The given id must not be null!");
            if (u.getPassword() == null) throw new NullPointerException();
            return null;
        }).given(users).create(any(User.class));
        if (e.is("POST", "/uaa/users/?") && e.status() == 500 && e.stateMatches(ALREADY_EXISTS)) {
            // UserServiceImpl.create: existing.ifPresent(... throw new IllegalArgumentException(...)), sin manejador
            String username = e.requestBody() != null ? e.requestBody().path("username").asText() : "";
            willThrow(new IllegalArgumentException("user already exists: " + username)).given(users).create(any(User.class));
        }
    }
}

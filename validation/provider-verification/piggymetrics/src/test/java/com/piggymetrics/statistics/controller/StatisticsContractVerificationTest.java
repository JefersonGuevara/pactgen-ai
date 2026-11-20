package com.piggymetrics.statistics.controller;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junitsupport.IgnoreMissingStateChange;
import au.com.dius.pact.provider.junitsupport.IgnoreNoPactsToVerify;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import au.com.dius.pact.provider.spring.junit5.MockMvcTestTarget;
import au.com.dius.pact.provider.spring.junit5.PactVerificationSpringProvider;
import com.piggymetrics.statistics.domain.timeseries.DataPoint;
import com.piggymetrics.statistics.service.StatisticsService;
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
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

/**
 * Verificación de proveedor de statistics-service (P4) contra los contratos de target/pacts-to-verify.
 * Nivel de controlador sobre el portado de piggymetrics 6bb2cf9 (ver NOTICE): StatisticsService se simula con
 * datos derivados de cada contrato (ContractFixtures). Context path: /statistics.
 * Archivo añadido por PactGen-AI (validation/provider-verification); no forma parte del proyecto original.
 */
@WebMvcTest(StatisticsController.class)
@Import(Boot20.Compat.class)
@Provider("statistics-service")
@PactFolder("target/pacts-to-verify")
@IgnoreMissingStateChange
@IgnoreNoPactsToVerify // sin contratos de este proveedor en la carpeta, la clase se omite (mvn test sin fallos)
class StatisticsContractVerificationTest {

    @Autowired
    WebApplicationContext ctx;

    @MockBean
    StatisticsService statistics;

    @TestTemplate
    @ExtendWith(PactVerificationSpringProvider.class)
    void verify(PactVerificationContext context) {
        Assumptions.assumeTrue(context != null, "sin contratos de este proveedor");
        context.verifyInteraction();
    }

    @BeforeEach
    void setUp(PactVerificationContext context) {
        if (context == null) return;
        MockMvcTestTarget target = new MockMvcTestTarget();
        // /statistics/current usa principal.getName(); el nombre no influye porque el servicio está simulado
        target.setMockMvc(Boot20.mockMvc(ctx, "/statistics", UsernamePasswordAuthenticationToken.authenticated(
                "demo", null, AuthorityUtils.createAuthorityList("ROLE_USER"))));
        context.setTarget(target);
        stub(ContractFixtures.expected(context.getInteraction()));
    }

    private void stub(Expected e) {
        // GET /current y GET /{accountName}: la serie del día que espera el contrato (lista vacía si no hay datos)
        given(statistics.findByAccountName(anyString()))
                .willReturn(ContractFixtures.buildList(DataPoint.class, e.responseBody()));
        // PUT /{accountName}: save() no lanza excepciones; el 400 solo puede venir de @Valid
    }
}

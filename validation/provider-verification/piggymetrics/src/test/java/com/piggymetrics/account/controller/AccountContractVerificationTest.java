package com.piggymetrics.account.controller;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junitsupport.IgnoreMissingStateChange;
import au.com.dius.pact.provider.junitsupport.IgnoreNoPactsToVerify;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import au.com.dius.pact.provider.spring.junit5.MockMvcTestTarget;
import au.com.dius.pact.provider.spring.junit5.PactVerificationSpringProvider;
import com.piggymetrics.account.domain.Account;
import com.piggymetrics.account.domain.User;
import com.piggymetrics.account.service.AccountService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;

/**
 * Verificación de proveedor de account-service (P3) contra los contratos de target/pacts-to-verify.
 * Nivel de controlador sobre el portado de piggymetrics 6bb2cf9 (ver NOTICE): AccountService se simula con
 * datos derivados de cada contrato (ContractFixtures). Context path: /accounts.
 * Archivo añadido por PactGen-AI (validation/provider-verification); no forma parte del proyecto original.
 */
@WebMvcTest(AccountController.class)
@Import(Boot20.Compat.class)
@Provider("account-service")
@PactFolder("target/pacts-to-verify")
@IgnoreMissingStateChange
@IgnoreNoPactsToVerify // sin contratos de este proveedor en la carpeta, la clase se omite (mvn test sin fallos)
class AccountContractVerificationTest {

    /** Estado del contrato que corresponde a Assert.isNull(existing) en AccountServiceImpl.create. */
    static final String ALREADY_EXISTS = "ya existe|\\bexistente|duplicad|already exist|ya est[aá] registrad|en uso";
    /** Estado que corresponde a Assert.notNull(account) en AccountServiceImpl.saveChanges. */
    static final String NOT_FOUND = "no existe|inexistente|not exist|not found|no encontrad|no est[aá] registrad|sin cuenta";

    @Autowired
    WebApplicationContext ctx;

    @MockBean
    AccountService accounts;

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
        // /current usa principal.getName(); el nombre no influye porque el servicio está simulado
        target.setMockMvc(Boot20.mockMvc(ctx, "/accounts", UsernamePasswordAuthenticationToken.authenticated(
                "demo", null, AuthorityUtils.createAuthorityList("ROLE_USER"))));
        context.setTarget(target);
        stub(ContractFixtures.expected(context.getInteraction()));
    }

    private void stub(Expected e) {
        Account account = ContractFixtures.build(Account.class, e.responseBody());
        // GET /{name} y GET /current: la cuenta del contrato; sin cuerpo esperado -> null -> 200 sin cuerpo (como Mongo)
        given(accounts.findByName(anyString())).willReturn(account);

        if (e.is("POST", "/accounts/?")) {
            // 400 de datos inválidos lo produce @Valid; el de "la cuenta ya existe", el servicio
            if (e.status() == 400 && e.stateMatches(ALREADY_EXISTS)) {
                given(accounts.create(any(User.class))).willThrow(new IllegalArgumentException("account already exists"));
            } else {
                given(accounts.create(any(User.class))).willReturn(account);
            }
        } else if (e.is("PUT", "/accounts/current/?")) {
            if (e.status() == 400 && e.stateMatches(NOT_FOUND)) {
                willThrow(new IllegalArgumentException("can't find account with name demo"))
                        .given(accounts).saveChanges(anyString(), any(Account.class));
            }
        }
    }
}

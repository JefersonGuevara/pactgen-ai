package org.springframework.samples.petclinic.visits.web;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junitsupport.IgnoreMissingStateChange;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import au.com.dius.pact.provider.spring.junit5.MockMvcTestTarget;
import au.com.dius.pact.provider.spring.junit5.PactVerificationSpringProvider;
import com.fasterxml.jackson.databind.JsonNode;
import dev.pactgen.verification.ContractFixtures;
import dev.pactgen.verification.ContractFixtures.Expected;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.samples.petclinic.visits.model.Visit;
import org.springframework.samples.petclinic.visits.model.VisitRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;

/**
 * Verificación de proveedor de visits-service (P2) contra los contratos de target/pacts-to-verify.
 * Nivel de controlador: el repositorio se simula con datos derivados de cada contrato (ContractFixtures).
 * Archivo añadido por PactGen-AI (validation/provider-verification); no forma parte del proyecto original.
 */
@WebMvcTest(VisitResource.class)
@ActiveProfiles("test")
@Provider("visits-service")
@PactFolder("target/pacts-to-verify")
@IgnoreMissingStateChange
class VisitsContractVerificationTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    VisitRepository visits;

    @TestTemplate
    @ExtendWith(PactVerificationSpringProvider.class)
    void verify(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @BeforeEach
    void setUp(PactVerificationContext context) {
        MockMvcTestTarget target = new MockMvcTestTarget();
        target.setMockMvc(mvc);
        context.setTarget(target);
        stub(ContractFixtures.expected(context.getInteraction()));
    }

    private void stub(Expected e) {
        JsonNode body = e.responseBody();
        // save() devuelve la misma visita que recibe (con el petId de la ruta), con el id generado
        given(visits.save(any(Visit.class))).willAnswer(inv -> ContractFixtures.withId(inv.getArgument(0), body));
        if (e.is("GET", "/owners/[^/]+/pets/[^/]+/visits/?")) {
            given(visits.findByPetId(anyInt())).willReturn(ContractFixtures.buildList(Visit.class, body));
        } else if (e.is("GET", "/pets/visits/?")) {
            given(visits.findByPetIdIn(any())).willReturn(
                    ContractFixtures.buildList(Visit.class, body == null ? null : body.get("items")));
        }
    }
}

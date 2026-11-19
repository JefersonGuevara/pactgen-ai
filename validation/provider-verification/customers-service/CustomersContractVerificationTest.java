package org.springframework.samples.petclinic.customers.web;

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
import org.springframework.context.annotation.Import;
import org.springframework.samples.petclinic.customers.model.Owner;
import org.springframework.samples.petclinic.customers.model.OwnerRepository;
import org.springframework.samples.petclinic.customers.model.Pet;
import org.springframework.samples.petclinic.customers.model.PetRepository;
import org.springframework.samples.petclinic.customers.model.PetType;
import org.springframework.samples.petclinic.customers.web.mapper.OwnerEntityMapper;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;

/**
 * Verificación de proveedor de customers-service (P1) contra los contratos de target/pacts-to-verify.
 * Nivel de controlador: los repositorios se simulan con datos derivados de cada contrato (ContractFixtures).
 * Archivo añadido por PactGen-AI (validation/provider-verification); no forma parte del proyecto original.
 */
@WebMvcTest({OwnerResource.class, PetResource.class})
@Import(OwnerEntityMapper.class)
@ActiveProfiles("test")
@Provider("customers-service")
@PactFolder("target/pacts-to-verify")
@IgnoreMissingStateChange
class CustomersContractVerificationTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    OwnerRepository owners;

    @MockBean
    PetRepository pets;

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
        // save() devuelve la misma entidad que recibe, con el id generado (semántica de JPA)
        given(owners.save(any(Owner.class))).willAnswer(inv -> ContractFixtures.withId(inv.getArgument(0), body));
        given(pets.save(any(Pet.class))).willAnswer(inv -> ContractFixtures.withId(inv.getArgument(0), body));

        if (e.is("GET", "/owners/?")) {
            given(owners.findAll()).willReturn(ContractFixtures.buildList(Owner.class, body));
        } else if (e.is("GET", "/owners/[^/]+")) {
            given(owners.findById(any())).willReturn(Optional.ofNullable(ContractFixtures.build(Owner.class, body)));
        } else if (e.is("PUT", "/owners/[^/]+")) {
            given(owners.findById(any())).willReturn(e.status() == 404 ? Optional.empty() : Optional.of(new Owner()));
        } else if (e.is("GET", "/petTypes/?")) {
            given(pets.findPetTypes()).willReturn(ContractFixtures.buildList(PetType.class, body));
        } else if (e.is("POST", "/owners/[^/]+/pets/?")) {
            given(owners.findById(any())).willReturn(e.status() == 404 ? Optional.empty() : Optional.of(new Owner()));
            PetType type = body == null ? null : ContractFixtures.build(PetType.class, body.get("type"));
            given(pets.findPetTypeById(anyInt())).willReturn(Optional.ofNullable(type));
        } else if (e.is("GET", "/owners/[^/]+/pets/[^/]+")) {
            given(pets.findById(any())).willReturn(Optional.ofNullable(petFromDetails(body)));
        } else if (e.is("PUT", "/owners/[^/]+/pets/[^/]+")) {
            Pet existing = new Pet();
            ContractFixtures.setField(existing, "owner", new Owner());
            given(pets.findById(any())).willReturn(e.status() == 404 ? Optional.empty() : Optional.of(existing));
            given(pets.findPetTypeById(anyInt())).willReturn(Optional.empty());
        }
    }

    /** PetDetails expone el propietario como "Nombre Apellido"; se reconstruye la mascota con su propietario. */
    private static Pet petFromDetails(JsonNode body) {
        Pet pet = ContractFixtures.build(Pet.class, body);
        if (pet == null) return null;
        Owner owner = new Owner();
        String full = body.path("owner").asText("");
        int space = full.indexOf(' ');
        ContractFixtures.setField(owner, "firstName", space < 0 ? full : full.substring(0, space));
        ContractFixtures.setField(owner, "lastName", space < 0 ? "" : full.substring(space + 1));
        ContractFixtures.setField(pet, "owner", owner);
        return pet;
    }
}

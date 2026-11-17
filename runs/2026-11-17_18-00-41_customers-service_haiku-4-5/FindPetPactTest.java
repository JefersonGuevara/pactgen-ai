package pactgen.generated;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslJsonArray;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslJsonRootValue;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Generado por PactGen-AI para GET /owners/{ownerId}/pets/{petId}. No editar a mano. */
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "customers-service", pactVersion = PactSpecVersion.V4)
class FindPetPactTest {

    // una petición de una mascota existente bajo un propietario existente
    @Pact(consumer = "api-gateway", provider = "customers-service")
    public V4Pact pact0(PactDslWithProvider builder) {
        PactDslJsonBody resBody = new PactDslJsonBody();
        resBody.integerType("id", 5);
        resBody.stringType("name", "Fluffy");
        resBody.stringType("owner", "John Doe");
        resBody.stringMatcher("birthDate", "\\d{4}-\\d{2}-\\d{2}", "2020-03-15");
        PactDslJsonBody resBody_type0 = resBody.object("type");
        resBody_type0.integerType("id", 1);
        resBody_type0.stringType("name", "cat");
        resBody_type0.closeObject();
        return builder
            .given("existe el propietario con id 1 y la mascota con id 5 bajo ese propietario")
            .uponReceiving("una petición de una mascota existente bajo un propietario existente")
            .path("/owners/1/pets/5")
            .method("GET")
            .willRespondWith()
            .status(200)
            .headers(Map.of("Content-Type", "application/json"))
            .body(resBody)
            .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "pact0")
    void interaction0(MockServer mockServer) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(mockServer.getUrl() + "/owners/1/pets/5"));
        request.method("GET", HttpRequest.BodyPublishers.noBody());
        HttpResponse<String> response = HttpClient.newHttpClient()
            .send(request.build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
    }

    // una petición de una mascota bajo un propietario inexistente
    @Pact(consumer = "api-gateway", provider = "customers-service")
    public V4Pact pact1(PactDslWithProvider builder) {
        return builder
            .given("no existe el propietario con id 999")
            .uponReceiving("una petición de una mascota bajo un propietario inexistente")
            .path("/owners/999/pets/5")
            .method("GET")
            .willRespondWith()
            .status(404)
            .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "pact1")
    void interaction1(MockServer mockServer) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(mockServer.getUrl() + "/owners/999/pets/5"));
        request.method("GET", HttpRequest.BodyPublishers.noBody());
        HttpResponse<String> response = HttpClient.newHttpClient()
            .send(request.build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(404, response.statusCode());
    }

    // una petición de una mascota inexistente bajo un propietario existente
    @Pact(consumer = "api-gateway", provider = "customers-service")
    public V4Pact pact2(PactDslWithProvider builder) {
        return builder
            .given("existe el propietario con id 1 pero no existe la mascota con id 999 bajo ese propietario")
            .uponReceiving("una petición de una mascota inexistente bajo un propietario existente")
            .path("/owners/1/pets/999")
            .method("GET")
            .willRespondWith()
            .status(404)
            .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "pact2")
    void interaction2(MockServer mockServer) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(mockServer.getUrl() + "/owners/1/pets/999"));
        request.method("GET", HttpRequest.BodyPublishers.noBody());
        HttpResponse<String> response = HttpClient.newHttpClient()
            .send(request.build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(404, response.statusCode());
    }
}

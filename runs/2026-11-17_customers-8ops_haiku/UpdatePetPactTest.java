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

/** Generado por PactGen-AI para PUT /owners/{ownerId}/pets/{petId}. No editar a mano. */
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "customers-service", pactVersion = PactSpecVersion.V4)
class UpdatePetPactTest {

    // una petición para actualizar una mascota existente con todos los campos
    @Pact(consumer = "api-gateway", provider = "customers-service")
    public V4Pact pact0(PactDslWithProvider builder) {
        PactDslJsonBody reqBody = new PactDslJsonBody();
        reqBody.integerType("id", 5);
        reqBody.stringType("name", "Fluffy");
        reqBody.stringMatcher("birthDate", "\\d{4}-\\d{2}-\\d{2}", "2020-03-15");
        reqBody.integerType("typeId", 1);
        return builder
            .given("existe el propietario con id 1 y la mascota con id 5 pertenece a ese propietario")
            .uponReceiving("una petición para actualizar una mascota existente con todos los campos")
            .path("/owners/1/pets/5")
            .method("PUT")
            .headers(Map.of("Content-Type", "application/json"))
            .body(reqBody)
            .willRespondWith()
            .status(204)
            .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "pact0")
    void interaction0(MockServer mockServer) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(mockServer.getUrl() + "/owners/1/pets/5"));
        request.header("Content-Type", "application/json");
        request.method("PUT", HttpRequest.BodyPublishers.ofString("{\"id\":5,\"name\":\"Fluffy\",\"birthDate\":\"2020-03-15\",\"typeId\":1}"));
        HttpResponse<String> response = HttpClient.newHttpClient()
            .send(request.build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(204, response.statusCode());
    }

    // una petición para actualizar una mascota con solo el nombre
    @Pact(consumer = "api-gateway", provider = "customers-service")
    public V4Pact pact1(PactDslWithProvider builder) {
        PactDslJsonBody reqBody = new PactDslJsonBody();
        reqBody.integerType("id", 6);
        reqBody.stringType("name", "Max");
        return builder
            .given("existe el propietario con id 1 y la mascota con id 6 pertenece a ese propietario")
            .uponReceiving("una petición para actualizar una mascota con solo el nombre")
            .path("/owners/1/pets/6")
            .method("PUT")
            .headers(Map.of("Content-Type", "application/json"))
            .body(reqBody)
            .willRespondWith()
            .status(204)
            .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "pact1")
    void interaction1(MockServer mockServer) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(mockServer.getUrl() + "/owners/1/pets/6"));
        request.header("Content-Type", "application/json");
        request.method("PUT", HttpRequest.BodyPublishers.ofString("{\"id\":6,\"name\":\"Max\"}"));
        HttpResponse<String> response = HttpClient.newHttpClient()
            .send(request.build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(204, response.statusCode());
    }

    // una petición para actualizar una mascota inexistente
    @Pact(consumer = "api-gateway", provider = "customers-service")
    public V4Pact pact2(PactDslWithProvider builder) {
        PactDslJsonBody reqBody = new PactDslJsonBody();
        reqBody.integerType("id", 999);
        reqBody.stringType("name", "Ghost");
        return builder
            .given("existe el propietario con id 1 pero no existe la mascota con id 999")
            .uponReceiving("una petición para actualizar una mascota inexistente")
            .path("/owners/1/pets/999")
            .method("PUT")
            .headers(Map.of("Content-Type", "application/json"))
            .body(reqBody)
            .willRespondWith()
            .status(404)
            .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "pact2")
    void interaction2(MockServer mockServer) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(mockServer.getUrl() + "/owners/1/pets/999"));
        request.header("Content-Type", "application/json");
        request.method("PUT", HttpRequest.BodyPublishers.ofString("{\"id\":999,\"name\":\"Ghost\"}"));
        HttpResponse<String> response = HttpClient.newHttpClient()
            .send(request.build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(404, response.statusCode());
    }

    // una petición para actualizar una mascota de un propietario inexistente
    @Pact(consumer = "api-gateway", provider = "customers-service")
    public V4Pact pact3(PactDslWithProvider builder) {
        PactDslJsonBody reqBody = new PactDslJsonBody();
        reqBody.integerType("id", 5);
        reqBody.stringType("name", "Buddy");
        return builder
            .given("no existe el propietario con id 999")
            .uponReceiving("una petición para actualizar una mascota de un propietario inexistente")
            .path("/owners/999/pets/5")
            .method("PUT")
            .headers(Map.of("Content-Type", "application/json"))
            .body(reqBody)
            .willRespondWith()
            .status(404)
            .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "pact3")
    void interaction3(MockServer mockServer) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(mockServer.getUrl() + "/owners/999/pets/5"));
        request.header("Content-Type", "application/json");
        request.method("PUT", HttpRequest.BodyPublishers.ofString("{\"id\":5,\"name\":\"Buddy\"}"));
        HttpResponse<String> response = HttpClient.newHttpClient()
            .send(request.build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(404, response.statusCode());
    }
}

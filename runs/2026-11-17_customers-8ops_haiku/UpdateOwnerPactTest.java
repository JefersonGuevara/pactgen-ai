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

/** Generado por PactGen-AI para PUT /owners/{ownerId}. No editar a mano. */
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "customers-service", pactVersion = PactSpecVersion.V4)
class UpdateOwnerPactTest {

    // una petición para actualizar un propietario existente con datos válidos
    @Pact(consumer = "api-gateway", provider = "customers-service")
    public V4Pact pact0(PactDslWithProvider builder) {
        PactDslJsonBody reqBody = new PactDslJsonBody();
        reqBody.stringType("firstName", "John");
        reqBody.stringType("lastName", "Doe");
        reqBody.stringType("address", "123 Main Street");
        reqBody.stringType("city", "Springfield");
        reqBody.stringType("telephone", "5551234567");
        return builder
            .given("existe el propietario con id 1")
            .uponReceiving("una petición para actualizar un propietario existente con datos válidos")
            .path("/owners/1")
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
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(mockServer.getUrl() + "/owners/1"));
        request.header("Content-Type", "application/json");
        request.method("PUT", HttpRequest.BodyPublishers.ofString("{\"firstName\":\"John\",\"lastName\":\"Doe\",\"address\":\"123 Main Street\",\"city\":\"Springfield\",\"telephone\":\"5551234567\"}"));
        HttpResponse<String> response = HttpClient.newHttpClient()
            .send(request.build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(204, response.statusCode());
    }

    // una petición para actualizar un propietario inexistente
    @Pact(consumer = "api-gateway", provider = "customers-service")
    public V4Pact pact1(PactDslWithProvider builder) {
        PactDslJsonBody reqBody = new PactDslJsonBody();
        reqBody.stringType("firstName", "Jane");
        reqBody.stringType("lastName", "Smith");
        reqBody.stringType("address", "456 Oak Avenue");
        reqBody.stringType("city", "Shelbyville");
        reqBody.stringType("telephone", "5559876543");
        return builder
            .given("no existe el propietario con id 999")
            .uponReceiving("una petición para actualizar un propietario inexistente")
            .path("/owners/999")
            .method("PUT")
            .headers(Map.of("Content-Type", "application/json"))
            .body(reqBody)
            .willRespondWith()
            .status(404)
            .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "pact1")
    void interaction1(MockServer mockServer) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(mockServer.getUrl() + "/owners/999"));
        request.header("Content-Type", "application/json");
        request.method("PUT", HttpRequest.BodyPublishers.ofString("{\"firstName\":\"Jane\",\"lastName\":\"Smith\",\"address\":\"456 Oak Avenue\",\"city\":\"Shelbyville\",\"telephone\":\"5559876543\"}"));
        HttpResponse<String> response = HttpClient.newHttpClient()
            .send(request.build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(404, response.statusCode());
    }

    // una petición de actualización sin un campo obligatorio
    @Pact(consumer = "api-gateway", provider = "customers-service")
    public V4Pact pact2(PactDslWithProvider builder) {
        PactDslJsonBody reqBody = new PactDslJsonBody();
        reqBody.stringType("firstName", "Robert");
        reqBody.stringType("lastName", "Johnson");
        reqBody.stringType("address", "789 Elm Street");
        reqBody.stringType("city", "Capital City");
        return builder
            .given("existe el propietario con id 2")
            .uponReceiving("una petición de actualización sin un campo obligatorio")
            .path("/owners/2")
            .method("PUT")
            .headers(Map.of("Content-Type", "application/json"))
            .body(reqBody)
            .willRespondWith()
            .status(400)
            .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "pact2")
    void interaction2(MockServer mockServer) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(mockServer.getUrl() + "/owners/2"));
        request.header("Content-Type", "application/json");
        request.method("PUT", HttpRequest.BodyPublishers.ofString("{\"firstName\":\"Robert\",\"lastName\":\"Johnson\",\"address\":\"789 Elm Street\",\"city\":\"Capital City\"}"));
        HttpResponse<String> response = HttpClient.newHttpClient()
            .send(request.build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(400, response.statusCode());
    }

    // una petición de actualización con teléfono en formato inválido
    @Pact(consumer = "api-gateway", provider = "customers-service")
    public V4Pact pact3(PactDslWithProvider builder) {
        PactDslJsonBody reqBody = new PactDslJsonBody();
        reqBody.stringType("firstName", "Mary");
        reqBody.stringType("lastName", "Williams");
        reqBody.stringType("address", "321 Pine Road");
        reqBody.stringType("city", "Metropolis");
        reqBody.stringType("telephone", "555-INVALID");
        return builder
            .given("existe el propietario con id 3")
            .uponReceiving("una petición de actualización con teléfono en formato inválido")
            .path("/owners/3")
            .method("PUT")
            .headers(Map.of("Content-Type", "application/json"))
            .body(reqBody)
            .willRespondWith()
            .status(400)
            .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "pact3")
    void interaction3(MockServer mockServer) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(mockServer.getUrl() + "/owners/3"));
        request.header("Content-Type", "application/json");
        request.method("PUT", HttpRequest.BodyPublishers.ofString("{\"firstName\":\"Mary\",\"lastName\":\"Williams\",\"address\":\"321 Pine Road\",\"city\":\"Metropolis\",\"telephone\":\"555-INVALID\"}"));
        HttpResponse<String> response = HttpClient.newHttpClient()
            .send(request.build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(400, response.statusCode());
    }
}

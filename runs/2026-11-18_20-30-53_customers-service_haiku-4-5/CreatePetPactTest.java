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

/** Generado por PactGen-AI para POST /owners/{ownerId}/pets. No editar a mano. */
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "customers-service", pactVersion = PactSpecVersion.V4)
class CreatePetPactTest {

    // una petición para crear una mascota en un propietario existente
    @Pact(consumer = "api-gateway", provider = "customers-service")
    public V4Pact pact0(PactDslWithProvider builder) {
        PactDslJsonBody reqBody = new PactDslJsonBody();
        reqBody.stringType("name", "Fluffy");
        reqBody.stringMatcher("birthDate", "\\d{4}-\\d{2}-\\d{2}", "2020-05-15");
        reqBody.integerType("typeId", 1);
        PactDslJsonBody resBody = new PactDslJsonBody();
        resBody.integerType("id", 1);
        resBody.stringType("name", "Fluffy");
        resBody.stringType("birthDate", "2020-05-15T00:00:00Z");
        PactDslJsonBody resBody_type0 = resBody.object("type");
        resBody_type0.integerType("id", 1);
        resBody_type0.stringType("name", "Dog");
        resBody_type0.closeObject();
        return builder
            .given("existe el propietario con id 1")
            .uponReceiving("una petición para crear una mascota en un propietario existente")
            .path("/owners/1/pets")
            .method("POST")
            .headers(Map.of("Content-Type", "application/json"))
            .body(reqBody)
            .willRespondWith()
            .status(201)
            .headers(Map.of("Content-Type", "application/json"))
            .body(resBody)
            .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "pact0")
    void interaction0(MockServer mockServer) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(mockServer.getUrl() + "/owners/1/pets"));
        request.header("Content-Type", "application/json");
        request.method("POST", HttpRequest.BodyPublishers.ofString("{\"name\":\"Fluffy\",\"birthDate\":\"2020-05-15\",\"typeId\":1}"));
        HttpResponse<String> response = HttpClient.newHttpClient()
            .send(request.build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(201, response.statusCode());
    }

    // una petición para crear una mascota en un propietario inexistente
    @Pact(consumer = "api-gateway", provider = "customers-service")
    public V4Pact pact1(PactDslWithProvider builder) {
        return builder
            .given("no existe el propietario con id 999")
            .uponReceiving("una petición para crear una mascota en un propietario inexistente")
            .path("/owners/999/pets")
            .method("POST")
            .headers(Map.of("Content-Type", "application/json"))
            .body("{\"name\":\"Whiskers\",\"birthDate\":\"2021-03-10\",\"typeId\":2}", "application/json")
            .willRespondWith()
            .status(404)
            .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "pact1")
    void interaction1(MockServer mockServer) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(mockServer.getUrl() + "/owners/999/pets"));
        request.header("Content-Type", "application/json");
        request.method("POST", HttpRequest.BodyPublishers.ofString("{\"name\":\"Whiskers\",\"birthDate\":\"2021-03-10\",\"typeId\":2}"));
        HttpResponse<String> response = HttpClient.newHttpClient()
            .send(request.build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(404, response.statusCode());
    }

    // una petición para crear una mascota sin nombre
    @Pact(consumer = "api-gateway", provider = "customers-service")
    public V4Pact pact2(PactDslWithProvider builder) {
        return builder
            .given("existe el propietario con id 1")
            .uponReceiving("una petición para crear una mascota sin nombre")
            .path("/owners/1/pets")
            .method("POST")
            .headers(Map.of("Content-Type", "application/json"))
            .body("{\"name\":\"\",\"birthDate\":\"2020-05-15\",\"typeId\":1}", "application/json")
            .willRespondWith()
            .status(400)
            .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "pact2")
    void interaction2(MockServer mockServer) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(mockServer.getUrl() + "/owners/1/pets"));
        request.header("Content-Type", "application/json");
        request.method("POST", HttpRequest.BodyPublishers.ofString("{\"name\":\"\",\"birthDate\":\"2020-05-15\",\"typeId\":1}"));
        HttpResponse<String> response = HttpClient.newHttpClient()
            .send(request.build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(400, response.statusCode());
    }
}

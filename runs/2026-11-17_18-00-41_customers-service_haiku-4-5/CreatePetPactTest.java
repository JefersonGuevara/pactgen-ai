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

    // una petición para añadir una mascota a un propietario existente
    @Pact(consumer = "api-gateway", provider = "customers-service")
    public V4Pact pact0(PactDslWithProvider builder) {
        PactDslJsonBody reqBody = new PactDslJsonBody();
        reqBody.stringType("name", "Buddy");
        reqBody.stringMatcher("birthDate", "\\d{4}-\\d{2}-\\d{2}", "2020-05-15");
        reqBody.integerType("typeId", 2);
        PactDslJsonBody resBody = new PactDslJsonBody();
        resBody.integerType("id", 101);
        resBody.stringType("name", "Buddy");
        resBody.stringMatcher("birthDate", "\\d{4}-\\d{2}-\\d{2}", "2020-05-15");
        PactDslJsonBody resBody_type0 = resBody.object("type");
        resBody_type0.integerType("id", 2);
        resBody_type0.stringType("name", "Dog");
        resBody_type0.closeObject();
        return builder
            .given("existe el propietario con id 1 y existe el tipo de mascota con id 2")
            .uponReceiving("una petición para añadir una mascota a un propietario existente")
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
        request.method("POST", HttpRequest.BodyPublishers.ofString("{\"name\":\"Buddy\",\"birthDate\":\"2020-05-15\",\"typeId\":2}"));
        HttpResponse<String> response = HttpClient.newHttpClient()
            .send(request.build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(201, response.statusCode());
    }

    // una petición para añadir una mascota sin fecha de nacimiento
    @Pact(consumer = "api-gateway", provider = "customers-service")
    public V4Pact pact1(PactDslWithProvider builder) {
        PactDslJsonBody reqBody = new PactDslJsonBody();
        reqBody.stringType("name", "Whiskers");
        reqBody.integerType("typeId", 1);
        PactDslJsonBody resBody = new PactDslJsonBody();
        resBody.integerType("id", 102);
        resBody.stringType("name", "Whiskers");
        PactDslJsonBody resBody_type0 = resBody.object("type");
        resBody_type0.integerType("id", 1);
        resBody_type0.stringType("name", "Cat");
        resBody_type0.closeObject();
        return builder
            .given("existe el propietario con id 2 y existe el tipo de mascota con id 1")
            .uponReceiving("una petición para añadir una mascota sin fecha de nacimiento")
            .path("/owners/2/pets")
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
    @PactTestFor(pactMethod = "pact1")
    void interaction1(MockServer mockServer) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(mockServer.getUrl() + "/owners/2/pets"));
        request.header("Content-Type", "application/json");
        request.method("POST", HttpRequest.BodyPublishers.ofString("{\"name\":\"Whiskers\",\"typeId\":1}"));
        HttpResponse<String> response = HttpClient.newHttpClient()
            .send(request.build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(201, response.statusCode());
    }

    // una petición para añadir una mascota a un propietario inexistente
    @Pact(consumer = "api-gateway", provider = "customers-service")
    public V4Pact pact2(PactDslWithProvider builder) {
        PactDslJsonBody reqBody = new PactDslJsonBody();
        reqBody.stringType("name", "Max");
        reqBody.stringMatcher("birthDate", "\\d{4}-\\d{2}-\\d{2}", "2021-03-10");
        reqBody.integerType("typeId", 2);
        return builder
            .given("no existe el propietario con id 999")
            .uponReceiving("una petición para añadir una mascota a un propietario inexistente")
            .path("/owners/999/pets")
            .method("POST")
            .headers(Map.of("Content-Type", "application/json"))
            .body(reqBody)
            .willRespondWith()
            .status(404)
            .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "pact2")
    void interaction2(MockServer mockServer) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(mockServer.getUrl() + "/owners/999/pets"));
        request.header("Content-Type", "application/json");
        request.method("POST", HttpRequest.BodyPublishers.ofString("{\"name\":\"Max\",\"birthDate\":\"2021-03-10\",\"typeId\":2}"));
        HttpResponse<String> response = HttpClient.newHttpClient()
            .send(request.build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(404, response.statusCode());
    }
}

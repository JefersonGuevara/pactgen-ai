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

/** Generado por PactGen-AI para POST /owners. No editar a mano. */
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "customers-service", pactVersion = PactSpecVersion.V4)
class CreateOwnerPactTest {

    // una petición para crear un propietario con todos los campos obligatorios
    @Pact(consumer = "api-gateway", provider = "customers-service")
    public V4Pact pact0(PactDslWithProvider builder) {
        PactDslJsonBody reqBody = new PactDslJsonBody();
        reqBody.stringType("firstName", "John");
        reqBody.stringType("lastName", "Doe");
        reqBody.stringType("address", "123 Main Street");
        reqBody.stringType("city", "Springfield");
        reqBody.stringType("telephone", "5551234567");
        PactDslJsonBody resBody = new PactDslJsonBody();
        resBody.integerType("id", 1);
        resBody.stringType("firstName", "John");
        resBody.stringType("lastName", "Doe");
        resBody.stringType("address", "123 Main Street");
        resBody.stringType("city", "Springfield");
        resBody.stringType("telephone", "5551234567");
        resBody.array("pets").closeArray();
        return builder
            .given("el servicio de clientes está disponible")
            .uponReceiving("una petición para crear un propietario con todos los campos obligatorios")
            .path("/owners")
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
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(mockServer.getUrl() + "/owners"));
        request.header("Content-Type", "application/json");
        request.method("POST", HttpRequest.BodyPublishers.ofString("{\"firstName\":\"John\",\"lastName\":\"Doe\",\"address\":\"123 Main Street\",\"city\":\"Springfield\",\"telephone\":\"5551234567\"}"));
        HttpResponse<String> response = HttpClient.newHttpClient()
            .send(request.build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(201, response.statusCode());
    }

    // una petición de alta sin el campo firstName obligatorio
    @Pact(consumer = "api-gateway", provider = "customers-service")
    public V4Pact pact1(PactDslWithProvider builder) {
        PactDslJsonBody reqBody = new PactDslJsonBody();
        reqBody.stringType("lastName", "Doe");
        reqBody.stringType("address", "123 Main Street");
        reqBody.stringType("city", "Springfield");
        reqBody.stringType("telephone", "5551234567");
        return builder
            .given("el servicio de clientes está disponible")
            .uponReceiving("una petición de alta sin el campo firstName obligatorio")
            .path("/owners")
            .method("POST")
            .headers(Map.of("Content-Type", "application/json"))
            .body(reqBody)
            .willRespondWith()
            .status(400)
            .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "pact1")
    void interaction1(MockServer mockServer) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(mockServer.getUrl() + "/owners"));
        request.header("Content-Type", "application/json");
        request.method("POST", HttpRequest.BodyPublishers.ofString("{\"lastName\":\"Doe\",\"address\":\"123 Main Street\",\"city\":\"Springfield\",\"telephone\":\"5551234567\"}"));
        HttpResponse<String> response = HttpClient.newHttpClient()
            .send(request.build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(400, response.statusCode());
    }

    // una petición de alta con un teléfono en formato inválido
    @Pact(consumer = "api-gateway", provider = "customers-service")
    public V4Pact pact2(PactDslWithProvider builder) {
        PactDslJsonBody reqBody = new PactDslJsonBody();
        reqBody.stringType("firstName", "Jane");
        reqBody.stringType("lastName", "Smith");
        reqBody.stringType("address", "456 Oak Avenue");
        reqBody.stringType("city", "Shelbyville");
        reqBody.stringType("telephone", "555-invalid");
        return builder
            .given("el servicio de clientes está disponible")
            .uponReceiving("una petición de alta con un teléfono en formato inválido")
            .path("/owners")
            .method("POST")
            .headers(Map.of("Content-Type", "application/json"))
            .body(reqBody)
            .willRespondWith()
            .status(400)
            .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "pact2")
    void interaction2(MockServer mockServer) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(mockServer.getUrl() + "/owners"));
        request.header("Content-Type", "application/json");
        request.method("POST", HttpRequest.BodyPublishers.ofString("{\"firstName\":\"Jane\",\"lastName\":\"Smith\",\"address\":\"456 Oak Avenue\",\"city\":\"Shelbyville\",\"telephone\":\"555-invalid\"}"));
        HttpResponse<String> response = HttpClient.newHttpClient()
            .send(request.build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(400, response.statusCode());
    }
}

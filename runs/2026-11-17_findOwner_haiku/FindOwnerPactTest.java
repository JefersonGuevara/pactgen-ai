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

/** Generado por PactGen-AI para GET /owners/{ownerId}. No editar a mano. */
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "customers-service", pactVersion = PactSpecVersion.V4)
class FindOwnerPactTest {

    // una petición de un propietario existente con mascotas
    @Pact(consumer = "api-gateway", provider = "customers-service")
    public V4Pact pact0(PactDslWithProvider builder) {
        PactDslJsonBody resBody = new PactDslJsonBody();
        resBody.integerType("id", 1);
        resBody.stringType("firstName", "Juan");
        resBody.stringType("lastName", "García");
        resBody.stringType("address", "Calle Principal 123");
        resBody.stringType("city", "Madrid");
        resBody.stringType("telephone", "912345678");
        PactDslJsonBody resBody_pets0 = resBody.eachLike("pets");
        resBody_pets0.integerType("id", 1);
        resBody_pets0.stringType("name", "Max");
        resBody_pets0.stringMatcher("birthDate", "\\d{4}-\\d{2}-\\d{2}", "2020-05-15");
        PactDslJsonBody resBody_pets0_type1 = resBody_pets0.object("type");
        resBody_pets0_type1.integerType("id", 1);
        resBody_pets0_type1.stringType("name", "Perro");
        resBody_pets0_type1.closeObject();
        resBody_pets0.closeObject().closeArray();
        return builder
            .given("existe el propietario con id 1 y tiene 2 mascotas asociadas")
            .uponReceiving("una petición de un propietario existente con mascotas")
            .path("/owners/1")
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
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(mockServer.getUrl() + "/owners/1"));
        request.method("GET", HttpRequest.BodyPublishers.noBody());
        HttpResponse<String> response = HttpClient.newHttpClient()
            .send(request.build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
    }

    // una petición de un propietario existente sin mascotas
    @Pact(consumer = "api-gateway", provider = "customers-service")
    public V4Pact pact1(PactDslWithProvider builder) {
        PactDslJsonBody resBody = new PactDslJsonBody();
        resBody.integerType("id", 2);
        resBody.stringType("firstName", "María");
        resBody.stringType("lastName", "López");
        resBody.stringType("address", "Avenida Central 456");
        resBody.stringType("city", "Barcelona");
        resBody.stringType("telephone", "934567890");
        resBody.array("pets").closeArray();
        return builder
            .given("existe el propietario con id 2 y no tiene mascotas asociadas")
            .uponReceiving("una petición de un propietario existente sin mascotas")
            .path("/owners/2")
            .method("GET")
            .willRespondWith()
            .status(200)
            .headers(Map.of("Content-Type", "application/json"))
            .body(resBody)
            .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "pact1")
    void interaction1(MockServer mockServer) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(mockServer.getUrl() + "/owners/2"));
        request.method("GET", HttpRequest.BodyPublishers.noBody());
        HttpResponse<String> response = HttpClient.newHttpClient()
            .send(request.build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
    }

    // una petición con un id de propietario inválido
    @Pact(consumer = "api-gateway", provider = "customers-service")
    public V4Pact pact2(PactDslWithProvider builder) {
        return builder
            .given("el servicio de clientes está disponible")
            .uponReceiving("una petición con un id de propietario inválido")
            .path("/owners/0")
            .method("GET")
            .willRespondWith()
            .status(400)
            .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "pact2")
    void interaction2(MockServer mockServer) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(mockServer.getUrl() + "/owners/0"));
        request.method("GET", HttpRequest.BodyPublishers.noBody());
        HttpResponse<String> response = HttpClient.newHttpClient()
            .send(request.build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(400, response.statusCode());
    }
}

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

/** Generado por PactGen-AI para GET /owners. No editar a mano. */
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "customers-service", pactVersion = PactSpecVersion.V4)
class FindAllOwnersPactTest {

    // una petición para listar propietarios cuando existen varios con mascotas
    @Pact(consumer = "api-gateway", provider = "customers-service")
    public V4Pact pact0(PactDslWithProvider builder) {
        PactDslJsonBody resItem0 = PactDslJsonArray.arrayEachLike();
        resItem0.integerType("id", 1);
        resItem0.stringType("firstName", "John");
        resItem0.stringType("lastName", "Doe");
        resItem0.stringType("address", "123 Main St");
        resItem0.stringType("city", "Springfield");
        resItem0.stringType("telephone", "5551234567");
        PactDslJsonBody resItem0_pets1 = resItem0.eachLike("pets");
        resItem0_pets1.integerType("id", 101);
        resItem0_pets1.stringType("name", "Max");
        resItem0_pets1.stringMatcher("birthDate", "\\d{4}-\\d{2}-\\d{2}", "2020-05-15");
        PactDslJsonBody resItem0_pets1_type2 = resItem0_pets1.object("type");
        resItem0_pets1_type2.integerType("id", 1);
        resItem0_pets1_type2.stringType("name", "Dog");
        resItem0_pets1_type2.closeObject();
        resItem0_pets1.closeObject().closeArray();
        DslPart resBody = resItem0.closeObject();
        return builder
            .given("existen 2 propietarios registrados: uno con 2 mascotas y otro con 1 mascota")
            .uponReceiving("una petición para listar propietarios cuando existen varios con mascotas")
            .path("/owners")
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
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(mockServer.getUrl() + "/owners"));
        request.method("GET", HttpRequest.BodyPublishers.noBody());
        HttpResponse<String> response = HttpClient.newHttpClient()
            .send(request.build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
    }

    // una petición para listar propietarios cuando existen sin mascotas
    @Pact(consumer = "api-gateway", provider = "customers-service")
    public V4Pact pact1(PactDslWithProvider builder) {
        PactDslJsonBody resItem0 = PactDslJsonArray.arrayEachLike();
        resItem0.integerType("id", 3);
        resItem0.stringType("firstName", "Robert");
        resItem0.stringType("lastName", "Johnson");
        resItem0.stringType("address", "789 Pine Rd");
        resItem0.stringType("city", "Capital City");
        resItem0.stringType("telephone", "5555551234");
        resItem0.array("pets").closeArray();
        DslPart resBody = resItem0.closeObject();
        return builder
            .given("existe 1 propietario registrado sin mascotas")
            .uponReceiving("una petición para listar propietarios cuando existen sin mascotas")
            .path("/owners")
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
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(mockServer.getUrl() + "/owners"));
        request.method("GET", HttpRequest.BodyPublishers.noBody());
        HttpResponse<String> response = HttpClient.newHttpClient()
            .send(request.build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
    }

    // una petición para listar propietarios cuando no hay ninguno registrado
    @Pact(consumer = "api-gateway", provider = "customers-service")
    public V4Pact pact2(PactDslWithProvider builder) {
        DslPart resBody = new PactDslJsonArray();
        return builder
            .given("no existen propietarios registrados en el sistema")
            .uponReceiving("una petición para listar propietarios cuando no hay ninguno registrado")
            .path("/owners")
            .method("GET")
            .willRespondWith()
            .status(200)
            .headers(Map.of("Content-Type", "application/json"))
            .body(resBody)
            .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "pact2")
    void interaction2(MockServer mockServer) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(mockServer.getUrl() + "/owners"));
        request.method("GET", HttpRequest.BodyPublishers.noBody());
        HttpResponse<String> response = HttpClient.newHttpClient()
            .send(request.build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
    }
}

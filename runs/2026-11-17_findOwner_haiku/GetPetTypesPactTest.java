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

/** Generado por PactGen-AI para GET /petTypes. No editar a mano. */
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "customers-service", pactVersion = PactSpecVersion.V4)
class GetPetTypesPactTest {

    // una petición para obtener múltiples tipos de mascota
    @Pact(consumer = "api-gateway", provider = "customers-service")
    public V4Pact pact0(PactDslWithProvider builder) {
        PactDslJsonBody resItem0 = PactDslJsonArray.arrayEachLike();
        resItem0.integerType("id", 1);
        resItem0.stringType("name", "Perro");
        DslPart resBody = resItem0.closeObject();
        return builder
            .given("existen tres tipos de mascota registrados: perro, gato y pájaro")
            .uponReceiving("una petición para obtener múltiples tipos de mascota")
            .path("/petTypes")
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
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(mockServer.getUrl() + "/petTypes"));
        request.method("GET", HttpRequest.BodyPublishers.noBody());
        HttpResponse<String> response = HttpClient.newHttpClient()
            .send(request.build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
    }

    // una petición para obtener tipos de mascota cuando no hay ninguno registrado
    @Pact(consumer = "api-gateway", provider = "customers-service")
    public V4Pact pact1(PactDslWithProvider builder) {
        DslPart resBody = new PactDslJsonArray();
        return builder
            .given("no existen tipos de mascota registrados")
            .uponReceiving("una petición para obtener tipos de mascota cuando no hay ninguno registrado")
            .path("/petTypes")
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
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(mockServer.getUrl() + "/petTypes"));
        request.method("GET", HttpRequest.BodyPublishers.noBody());
        HttpResponse<String> response = HttpClient.newHttpClient()
            .send(request.build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
    }

    // una petición para obtener un único tipo de mascota
    @Pact(consumer = "api-gateway", provider = "customers-service")
    public V4Pact pact2(PactDslWithProvider builder) {
        PactDslJsonBody resItem0 = PactDslJsonArray.arrayEachLike();
        resItem0.integerType("id", 4);
        resItem0.stringType("name", "Reptil");
        DslPart resBody = resItem0.closeObject();
        return builder
            .given("existe un único tipo de mascota registrado: reptil")
            .uponReceiving("una petición para obtener un único tipo de mascota")
            .path("/petTypes")
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
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(mockServer.getUrl() + "/petTypes"));
        request.method("GET", HttpRequest.BodyPublishers.noBody());
        HttpResponse<String> response = HttpClient.newHttpClient()
            .send(request.build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
    }
}

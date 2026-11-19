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

/** Generado por PactGen-AI para PUT /statistics/{accountName}. No editar a mano. */
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "statistics-service", pactVersion = PactSpecVersion.V4)
class SaveAccountStatisticsPactTest {

    // una petición para registrar estadísticas completas de una cuenta
    @Pact(consumer = "account-service", provider = "statistics-service")
    public V4Pact pact0(PactDslWithProvider builder) {
        PactDslJsonBody reqBody = new PactDslJsonBody();
        PactDslJsonBody reqBody_incomes0 = reqBody.eachLike("incomes");
        reqBody_incomes0.stringType("title", "Salario");
        reqBody_incomes0.decimalType("amount", 3000.0);
        reqBody_incomes0.stringType("currency", "USD");
        reqBody_incomes0.stringType("period", "MONTH");
        reqBody_incomes0.closeObject().closeArray();
        PactDslJsonBody reqBody_expenses1 = reqBody.eachLike("expenses");
        reqBody_expenses1.stringType("title", "Alquiler");
        reqBody_expenses1.decimalType("amount", 1200.0);
        reqBody_expenses1.stringType("currency", "USD");
        reqBody_expenses1.stringType("period", "MONTH");
        reqBody_expenses1.closeObject().closeArray();
        PactDslJsonBody reqBody_saving2 = reqBody.object("saving");
        reqBody_saving2.decimalType("amount", 1500.0);
        reqBody_saving2.stringType("currency", "USD");
        reqBody_saving2.decimalType("interest", 3.5);
        reqBody_saving2.booleanType("deposit", true);
        reqBody_saving2.booleanType("capitalization", false);
        reqBody_saving2.closeObject();
        return builder
            .given("la cuenta 'john_doe' existe y acepta registros de estadísticas")
            .uponReceiving("una petición para registrar estadísticas completas de una cuenta")
            .path("/statistics/john_doe")
            .method("PUT")
            .headers(Map.of("Content-Type", "application/json"))
            .body(reqBody)
            .willRespondWith()
            .status(200)
            .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "pact0")
    void interaction0(MockServer mockServer) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(mockServer.getUrl() + "/statistics/john_doe"));
        request.header("Content-Type", "application/json");
        request.method("PUT", HttpRequest.BodyPublishers.ofString("{\"incomes\":[{\"title\":\"Salario\",\"amount\":3000.0,\"currency\":\"USD\",\"period\":\"MONTH\"}],\"expenses\":[{\"title\":\"Alquiler\",\"amount\":1200.0,\"currency\":\"USD\",\"period\":\"MONTH\"}],\"saving\":{\"amount\":1500.0,\"currency\":\"USD\",\"interest\":3.5,\"deposit\":true,\"capitalization\":false}}"));
        HttpResponse<String> response = HttpClient.newHttpClient()
            .send(request.build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
    }

    // una petición con título de ingreso vacío
    @Pact(consumer = "account-service", provider = "statistics-service")
    public V4Pact pact1(PactDslWithProvider builder) {
        PactDslJsonBody reqBody = new PactDslJsonBody();
        PactDslJsonBody reqBody_incomes0 = reqBody.eachLike("incomes");
        reqBody_incomes0.stringType("title", "");
        reqBody_incomes0.decimalType("amount", 3000.0);
        reqBody_incomes0.stringType("currency", "USD");
        reqBody_incomes0.stringType("period", "MONTH");
        reqBody_incomes0.closeObject().closeArray();
        PactDslJsonBody reqBody_expenses1 = reqBody.eachLike("expenses");
        reqBody_expenses1.stringType("title", "Gasto");
        reqBody_expenses1.decimalType("amount", 500.0);
        reqBody_expenses1.stringType("currency", "USD");
        reqBody_expenses1.stringType("period", "MONTH");
        reqBody_expenses1.closeObject().closeArray();
        PactDslJsonBody reqBody_saving2 = reqBody.object("saving");
        reqBody_saving2.decimalType("amount", 1000.0);
        reqBody_saving2.stringType("currency", "USD");
        reqBody_saving2.decimalType("interest", 2.0);
        reqBody_saving2.booleanType("deposit", true);
        reqBody_saving2.booleanType("capitalization", false);
        reqBody_saving2.closeObject();
        return builder
            .given("la cuenta 'invalid_account' existe pero rechaza datos inválidos")
            .uponReceiving("una petición con título de ingreso vacío")
            .path("/statistics/invalid_account")
            .method("PUT")
            .headers(Map.of("Content-Type", "application/json"))
            .body(reqBody)
            .willRespondWith()
            .status(400)
            .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "pact1")
    void interaction1(MockServer mockServer) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(mockServer.getUrl() + "/statistics/invalid_account"));
        request.header("Content-Type", "application/json");
        request.method("PUT", HttpRequest.BodyPublishers.ofString("{\"incomes\":[{\"title\":\"\",\"amount\":3000.0,\"currency\":\"USD\",\"period\":\"MONTH\"}],\"expenses\":[{\"title\":\"Gasto\",\"amount\":500.0,\"currency\":\"USD\",\"period\":\"MONTH\"}],\"saving\":{\"amount\":1000.0,\"currency\":\"USD\",\"interest\":2.0,\"deposit\":true,\"capitalization\":false}}"));
        HttpResponse<String> response = HttpClient.newHttpClient()
            .send(request.build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(400, response.statusCode());
    }

    // una petición sin el campo obligatorio 'saving'
    @Pact(consumer = "account-service", provider = "statistics-service")
    public V4Pact pact2(PactDslWithProvider builder) {
        PactDslJsonBody reqBody = new PactDslJsonBody();
        PactDslJsonBody reqBody_incomes0 = reqBody.eachLike("incomes");
        reqBody_incomes0.stringType("title", "Salario");
        reqBody_incomes0.decimalType("amount", 3000.0);
        reqBody_incomes0.stringType("currency", "USD");
        reqBody_incomes0.stringType("period", "MONTH");
        reqBody_incomes0.closeObject().closeArray();
        PactDslJsonBody reqBody_expenses1 = reqBody.eachLike("expenses");
        reqBody_expenses1.stringType("title", "Gasto");
        reqBody_expenses1.decimalType("amount", 500.0);
        reqBody_expenses1.stringType("currency", "USD");
        reqBody_expenses1.stringType("period", "MONTH");
        reqBody_expenses1.closeObject().closeArray();
        return builder
            .given("la cuenta 'incomplete_account' existe pero rechaza peticiones incompletas")
            .uponReceiving("una petición sin el campo obligatorio 'saving'")
            .path("/statistics/incomplete_account")
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
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(mockServer.getUrl() + "/statistics/incomplete_account"));
        request.header("Content-Type", "application/json");
        request.method("PUT", HttpRequest.BodyPublishers.ofString("{\"incomes\":[{\"title\":\"Salario\",\"amount\":3000.0,\"currency\":\"USD\",\"period\":\"MONTH\"}],\"expenses\":[{\"title\":\"Gasto\",\"amount\":500.0,\"currency\":\"USD\",\"period\":\"MONTH\"}]}"));
        HttpResponse<String> response = HttpClient.newHttpClient()
            .send(request.build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(400, response.statusCode());
    }
}

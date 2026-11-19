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

/** Generado por PactGen-AI para GET /statistics/{accountName}. No editar a mano. */
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "statistics-service", pactVersion = PactSpecVersion.V4)
class GetStatisticsByAccountNamePactTest {

    // una petición de estadísticas de una cuenta con historial completo
    @Pact(consumer = "account-service", provider = "statistics-service")
    public V4Pact pact0(PactDslWithProvider builder) {
        PactDslJsonBody resItem0 = PactDslJsonArray.arrayEachLike();
        PactDslJsonBody resItem0_id1 = resItem0.object("id");
        resItem0_id1.stringType("account", "ana");
        resItem0_id1.stringType("date", "2024-01-15T00:00:00Z");
        resItem0_id1.closeObject();
        PactDslJsonBody resItem0_incomes2 = resItem0.eachLike("incomes");
        resItem0_incomes2.stringType("title", "Salario");
        resItem0_incomes2.decimalType("amount", 2500.0);
        resItem0_incomes2.closeObject().closeArray();
        PactDslJsonBody resItem0_expenses3 = resItem0.eachLike("expenses");
        resItem0_expenses3.stringType("title", "Alquiler");
        resItem0_expenses3.decimalType("amount", 1200.0);
        resItem0_expenses3.closeObject().closeArray();
        PactDslJsonBody resItem0_statistics4 = resItem0.object("statistics");
        resItem0_statistics4.decimalType("INCOMES_AMOUNT", 3000.0);
        resItem0_statistics4.decimalType("EXPENSES_AMOUNT", 1500.0);
        resItem0_statistics4.decimalType("SAVING_AMOUNT", 1500.0);
        resItem0_statistics4.closeObject();
        PactDslJsonBody resItem0_rates5 = resItem0.object("rates");
        resItem0_rates5.decimalType("USD", 1.0);
        resItem0_rates5.decimalType("EUR", 0.92);
        resItem0_rates5.decimalType("RUB", 91.5);
        resItem0_rates5.closeObject();
        DslPart resBody = resItem0.closeObject();
        return builder
            .given("existe la cuenta 'ana' con estadísticas de múltiples días incluyendo ingresos, gastos y tasas de cambio")
            .uponReceiving("una petición de estadísticas de una cuenta con historial completo")
            .path("/statistics/ana")
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
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(mockServer.getUrl() + "/statistics/ana"));
        request.method("GET", HttpRequest.BodyPublishers.noBody());
        HttpResponse<String> response = HttpClient.newHttpClient()
            .send(request.build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
    }

    // una petición de estadísticas de una cuenta sin historial
    @Pact(consumer = "account-service", provider = "statistics-service")
    public V4Pact pact1(PactDslWithProvider builder) {
        DslPart resBody = new PactDslJsonArray();
        return builder
            .given("existe la cuenta 'carlos' pero no tiene estadísticas registradas")
            .uponReceiving("una petición de estadísticas de una cuenta sin historial")
            .path("/statistics/carlos")
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
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(mockServer.getUrl() + "/statistics/carlos"));
        request.method("GET", HttpRequest.BodyPublishers.noBody());
        HttpResponse<String> response = HttpClient.newHttpClient()
            .send(request.build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
    }

    // una petición de estadísticas de una cuenta con solo gastos en un día
    @Pact(consumer = "account-service", provider = "statistics-service")
    public V4Pact pact2(PactDslWithProvider builder) {
        PactDslJsonBody resItem0 = PactDslJsonArray.arrayEachLike();
        PactDslJsonBody resItem0_id1 = resItem0.object("id");
        resItem0_id1.stringType("account", "maria");
        resItem0_id1.stringType("date", "2024-01-20T00:00:00Z");
        resItem0_id1.closeObject();
        resItem0.array("incomes").closeArray();
        PactDslJsonBody resItem0_expenses2 = resItem0.eachLike("expenses");
        resItem0_expenses2.stringType("title", "Servicios");
        resItem0_expenses2.decimalType("amount", 150.0);
        resItem0_expenses2.closeObject().closeArray();
        PactDslJsonBody resItem0_statistics3 = resItem0.object("statistics");
        resItem0_statistics3.decimalType("INCOMES_AMOUNT", 0.0);
        resItem0_statistics3.decimalType("EXPENSES_AMOUNT", 150.0);
        resItem0_statistics3.decimalType("SAVING_AMOUNT", -150.0);
        resItem0_statistics3.closeObject();
        PactDslJsonBody resItem0_rates4 = resItem0.object("rates");
        resItem0_rates4.decimalType("USD", 1.0);
        resItem0_rates4.decimalType("EUR", 0.92);
        resItem0_rates4.decimalType("RUB", 91.5);
        resItem0_rates4.closeObject();
        DslPart resBody = resItem0.closeObject();
        return builder
            .given("existe la cuenta 'maria' con un día que tiene solo gastos y sin ingresos")
            .uponReceiving("una petición de estadísticas de una cuenta con solo gastos en un día")
            .path("/statistics/maria")
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
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(mockServer.getUrl() + "/statistics/maria"));
        request.method("GET", HttpRequest.BodyPublishers.noBody());
        HttpResponse<String> response = HttpClient.newHttpClient()
            .send(request.build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
    }
}

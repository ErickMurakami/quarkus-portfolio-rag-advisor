package dev.erickmurakami.portfolio.interfaces.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import io.quarkus.qute.Template;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;

@QuarkusTest
@TestProfile(PortfolioPageTestProfile.class)
class PortfolioPageResourceTest {
    @Inject Template index;

    @Test
    void shouldServeQutePageWithoutPrivateDataOrAdvisorFormOutsideDevMode() {
        given().when().get("/").then().statusCode(200).contentType("text/html")
                .body(containsString("PORTFOLIO"), containsString("id=\"developer-id\""),
                        containsString("Declared skills"), containsString("id=\"retrieval-form\""),
                        containsString("role=\"tablist\""), containsString("id=\"language-toggle\""),
                        containsString("id=\"selected-developer\""),
                        containsString("Advisor is unavailable in this environment"),
                        not(containsString("id=\"advisor-form\"")), not(containsString("{#")));
    }

    @Test
    void shouldRenderLocalAdvisorWithWarningAndEmptyResults() {
        String html = index.data("localAdvisor", true).render();
        assertTrue(html.contains("id=\"advisor-form\""));
        assertTrue(html.contains("Repository aliases do not prevent private information"));
        assertTrue(html.contains("id=\"advisor-result\" aria-busy=\"false\"></div>"));
        assertFalse(html.contains("Advisor is unavailable in this environment"));
        assertFalse(html.contains("Private Repository 1"));
    }

    @Test
    void shouldServeStylesAndScriptWithCorrectContentTypes() {
        given().when().get("/css/app.css").then().statusCode(200).contentType(containsString("text/css"))
                .body(containsString("prefers-reduced-motion"), containsString("data-theme='light'"));
        given().when().get("/js/app.js").then().statusCode(200)
                .contentType(anyOf(containsString("javascript"), containsString("ecmascript")))
                .body(containsString("/api/v1/developers/"), containsString("textContent"));
    }

    @Test
    void shouldServeLanguageDictionaryWithoutDeveloperData() {
        given().when().get("/js/i18n.js").then().statusCode(200)
                .body(containsString("Perfil do desenvolvedor"), containsString("function translated"));
    }

    @Test
    void shouldRejectDirectAdvisorRequestOutsideDevBeforeCallingAiOrDatabase() {
        given().contentType("application/json").body("{\"question\":\"Synthetic question\"}")
                .when().post("/api/v1/developers/1/advisor").then().statusCode(403);
    }
}

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
        String html = index.data("localAdvisor", true).data("skillCategories", "LANGUAGE,FRAMEWORK,DATABASE,ARCHITECTURE,TOOL,OTHER").render();
        assertTrue(html.contains("id=\"advisor-form\""));
        assertTrue(html.contains("Repository aliases do not prevent private information"));
        assertTrue(html.contains("id=\"advisor-result\" aria-busy=\"false\"></div>"));
        assertFalse(html.contains("Advisor is unavailable in this environment"));
        assertFalse(html.contains("Private Repository 1"));
        assertTrue(html.contains("id=\"advisor-state\""));
        assertTrue(html.contains("class=\"advisor-card advisor-question-card\""));
        assertTrue(html.contains("aria-describedby=\"advisor-help\""));
        assertTrue(html.contains("id=\"advisor-suggestions-title\""));
        assertTrue(html.contains("id=\"advisor-progress\""));
    }

    @Test
    void shouldServeStylesAndScriptWithCorrectContentTypes() {
        given().when().get("/css/app.css").then().statusCode(200).contentType(containsString("text/css"))
                .body(containsString("prefers-reduced-motion"), containsString("data-theme='light'"),
                        matchesPattern("(?s)^@import url\\(\'[^\'\\r\\n]*\'\\);.*:root.*"));
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
    void shouldServeLocalLogosAndBootstrapLocaleBeforeStyles() {
        given().get("/logos/java.svg?v=pages-1").then().statusCode(200)
                .contentType(containsString("image/svg+xml")).body(containsString("<svg"));
        given().get("/logos/groq.svg?v=pages-1").then().statusCode(200)
                .body(containsString("<svg"));
        String html = index.data("localAdvisor", false).render();
        assertTrue(html.indexOf("/js/i18n.js") < html.indexOf("rel=\"stylesheet\""));
        assertTrue(html.contains("translateUi(initialUiLanguage)"));
        assertFalse(html.contains("id=\"setup-form\""));
    }

    @Test
    void shouldRejectUploadProfileWritesIngestionAndServerTokenEndpointsOutsideDev() {
        given().contentType("application/json").body("{}").post("/api/v1/developers").then().statusCode(403);
        given().post("/api/v1/ingestion-jobs/developers/1/public").then().statusCode(403);
        given().post("/api/v1/ingestion-jobs/developers/1").then().statusCode(403);
        given().get("/api/v1/github/me/repositories").then().statusCode(403);
        given().get("/api/v1/github/me/synthetic/private/readme").then().statusCode(403);
        given().multiPart("githubUsername", "synthetic").post("/api/v1/resumes/import").then().statusCode(403);
    }

    @Test
    void shouldRejectDirectAdvisorRequestOutsideDevBeforeCallingAiOrDatabase() {
        given().contentType("application/json").body("{\"question\":\"Synthetic question\"}")
                .when().post("/api/v1/developers/1/advisor").then().statusCode(403);
    }
}

package ai.gameclaw.e2e;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisabledIfSystemProperty(named = "skipE2E", matches = "true")
class DataToolE2ETest {

    private static Playwright playwright;
    private static Browser browser;

    @LocalServerPort
    private int port;

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions());
    }

    @AfterAll
    static void closeBrowser() {
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }

    @Test
    @DisplayName("E2E-9.12: 数据分析师角色可见query_data工具")
    void dataAnalystCanSeeQueryDataTool() {
        try (BrowserContext ctx = browser.newContext(); Page page = ctx.newPage()) {
            page.navigate("http://localhost:" + port + "/chat");
            page.waitForSelector("#role-selector", new Page.WaitForSelectorOptions().setTimeout(5000));

            page.selectOption("#role-selector", "DATA_ANALYST");
            page.waitForTimeout(500);

            var toolTags = page.querySelectorAll("#tool-list .tag");
            var toolNames = toolTags.stream().map(el -> el.innerText()).toList();
            assertThat(toolNames).anyMatch(name -> name.contains("query_data") || name.contains("数据查询"));
        }
    }
}

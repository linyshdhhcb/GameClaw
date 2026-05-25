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
class GameToolE2ETest {

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
    @DisplayName("E2E-8.14: 策划生成怪物配置 - 选择策划角色后可见generate_monsters工具")
    void designerCanSeeGenerateMonstersTool() {
        try (BrowserContext ctx = browser.newContext(); Page page = ctx.newPage()) {
            page.navigate("http://localhost:" + port + "/chat");
            page.waitForSelector("#role-selector", new Page.WaitForSelectorOptions().setTimeout(5000));

            page.selectOption("#role-selector", "TA");
            page.waitForTimeout(500);

            var toolTags = page.querySelectorAll("#tool-list .tag");
            var toolNames = toolTags.stream().map(el -> el.innerText()).toList();
            assertThat(toolNames).anyMatch(name -> name.contains("generate_monsters") || name.contains("怪物"));
        }
    }

    @Test
    @DisplayName("E2E-8.15: 程序生成Unity脚本 - 选择程序员角色后可见generate_unity_script工具")
    void programmerCanSeeGenerateUnityScriptTool() {
        try (BrowserContext ctx = browser.newContext(); Page page = ctx.newPage()) {
            page.navigate("http://localhost:" + port + "/chat");
            page.waitForSelector("#role-selector", new Page.WaitForSelectorOptions().setTimeout(5000));

            page.selectOption("#role-selector", "PROGRAMMER");
            page.waitForTimeout(500);

            var toolTags = page.querySelectorAll("#tool-list .tag");
            var toolNames = toolTags.stream().map(el -> el.innerText()).toList();
            assertThat(toolNames).anyMatch(name -> name.contains("generate_unity_script") || name.contains("Unity"));
        }
    }

    @Test
    @DisplayName("E2E-8.16: NL查API用法 - query_engine_api工具在程序员角色中可用")
    void queryEngineApiToolAvailableForProgrammer() {
        try (BrowserContext ctx = browser.newContext(); Page page = ctx.newPage()) {
            page.navigate("http://localhost:" + port + "/chat");
            page.waitForSelector("#role-selector", new Page.WaitForSelectorOptions().setTimeout(5000));

            page.selectOption("#role-selector", "PROGRAMMER");
            page.waitForTimeout(500);

            var toolTags = page.querySelectorAll("#tool-list .tag");
            var toolNames = toolTags.stream().map(el -> el.innerText()).toList();
            assertThat(toolNames).anyMatch(name -> name.contains("query_engine_api") || name.contains("API"));
        }
    }
}

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
class ChatE2ETest {

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
    @DisplayName("E2E: 首页加载成功")
    void homePageLoads() {
        try (BrowserContext ctx = browser.newContext(); Page page = ctx.newPage()) {
            page.navigate("http://localhost:" + port + "/");
            assertThat(page.title()).isNotBlank();
            assertThat(page.content()).contains("Claw");
        }
    }

    @Test
    @DisplayName("E2E: Chat页面加载并包含Role Selector")
    void chatPageHasRoleSelector() {
        try (BrowserContext ctx = browser.newContext(); Page page = ctx.newPage()) {
            page.navigate("http://localhost:" + port + "/chat");
            page.waitForSelector("#role-selector", new Page.WaitForSelectorOptions().setTimeout(5000));
            var selector = page.querySelector("#role-selector");
            assertThat(selector).isNotNull();
            assertThat(selector.innerText()).contains("程序员");
        }
    }

    @Test
    @DisplayName("E2E: Chat页面包含消息输入框")
    void chatPageHasMessageInput() {
        try (BrowserContext ctx = browser.newContext(); Page page = ctx.newPage()) {
            page.navigate("http://localhost:" + port + "/chat");
            page.waitForSelector("#message-input", new Page.WaitForSelectorOptions().setTimeout(5000));
            var input = page.querySelector("#message-input");
            assertThat(input).isNotNull();
        }
    }

    @Test
    @DisplayName("E2E: 切换Role后Tool列表变化")
    void roleSwitchChangesToolList() {
        try (BrowserContext ctx = browser.newContext(); Page page = ctx.newPage()) {
            page.navigate("http://localhost:" + port + "/chat");
            page.waitForSelector("#role-selector", new Page.WaitForSelectorOptions().setTimeout(5000));

            page.selectOption("#role-selector", "PROGRAMMER");
            page.waitForTimeout(500);
            var programmerTools = page.querySelectorAll("#tool-list .tag");

            page.selectOption("#role-selector", "TA");
            page.waitForTimeout(500);
            var taTools = page.querySelectorAll("#tool-list .tag");

            assertThat(programmerTools.size()).isGreaterThan(taTools.size());
        }
    }

    @Test
    @DisplayName("E2E: Roles API返回角色列表")
    void rolesApiReturnsRoles() {
        try (BrowserContext ctx = browser.newContext(); Page page = ctx.newPage()) {
            var response = page.navigate("http://localhost:" + port + "/api/roles");
            assertThat(response.status()).isEqualTo(200);
            String body = page.textContent("body");
            assertThat(body).contains("PROGRAMMER");
        }
    }
}

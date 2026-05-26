package ai.gameclaw.agent.llm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ModelRouterTest {

    private ModelRouter modelRouter;

    @BeforeEach
    void setUp() {
        ModelRouterProperties properties = new ModelRouterProperties();
        modelRouter = new ModelRouter(properties);
    }

    @Test
    void shortPrompt_classifiedAsSimple() {
        ChatRequest request = new ChatRequest("hi");
        ModelChoice choice = modelRouter.route(request);
        assertThat(choice.complexity()).isEqualTo(Complexity.SIMPLE);
        assertThat(choice.modelId()).isEqualTo("haiku");
        assertThat(choice.fallback()).isEqualTo("sonnet");
    }

    @Test
    void longPrompt_classifiedAsStandard() {
        String longPrompt = "x".repeat(4001);
        ChatRequest request = new ChatRequest(longPrompt);
        ModelChoice choice = modelRouter.route(request);
        assertThat(choice.complexity()).isEqualTo(Complexity.STANDARD);
        assertThat(choice.modelId()).isEqualTo("sonnet");
    }

    @Test
    void promptWithChineseDesignKeyword_classifiedAsComplex() {
        ChatRequest request = new ChatRequest("请设计一个游戏关卡");
        ModelChoice choice = modelRouter.route(request);
        assertThat(choice.complexity()).isEqualTo(Complexity.COMPLEX);
        assertThat(choice.modelId()).isEqualTo("opus");
    }

    @Test
    void promptWithAnalyzeKeyword_classifiedAsComplex() {
        ChatRequest request = new ChatRequest("Please analyze the data");
        ModelChoice choice = modelRouter.route(request);
        assertThat(choice.complexity()).isEqualTo(Complexity.COMPLEX);
        assertThat(choice.modelId()).isEqualTo("opus");
    }

    @Test
    void defaultPrompt_classifiedAsStandard() {
        String mediumPrompt = "Tell me about game development best practices and how to apply them in a team setting. ".repeat(4);
        ChatRequest request = new ChatRequest(mediumPrompt);
        ModelChoice choice = modelRouter.route(request);
        assertThat(choice.complexity()).isEqualTo(Complexity.STANDARD);
        assertThat(choice.modelId()).isEqualTo("sonnet");
    }
}

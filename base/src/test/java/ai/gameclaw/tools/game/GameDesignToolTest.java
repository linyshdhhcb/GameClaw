package ai.gameclaw.tools.game;

import ai.gameclaw.agent.llm.ChatRequest;
import ai.gameclaw.agent.llm.ChatResponse;
import ai.gameclaw.agent.llm.LlmClient;
import ai.gameclaw.governance.ValidationGate;
import ai.gameclaw.observability.AiMetrics;
import ai.gameclaw.tools.sandbox.SandboxWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.FileSystemResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameDesignToolTest {

    @TempDir
    Path tempDir;

    @Mock
    LlmClient llmClient;

    @Mock
    AiMetrics aiMetrics;

    List<ValidationGate> gates = List.of();

    SandboxWriter sandboxWriter;
    GameDesignTool tool;

    @BeforeEach
    void setUp() throws IOException {
        Path workspace = tempDir.resolve("workspace");
        Files.createDirectories(workspace);
        sandboxWriter = new SandboxWriter(new FileSystemResource(workspace.toFile()));
        tool = new GameDesignTool(llmClient, sandboxWriter, gates, aiMetrics);
    }

    @Test
    void generateMonstersReturnsSuccess() {
        when(llmClient.call(any(ChatRequest.class))).thenReturn(new ChatResponse(
                "[{\"id\":1,\"name\":\"Goblin\",\"level\":5,\"hp\":200,\"attack\":30,\"defense\":10,\"dropRate\":0.5,\"skills\":[{\"id\":1,\"probability\":0.8}],\"description\":\"A small green creature\"}]"
        ));

        String result = tool.generateMonsters("5个森林怪物", 5, null);

        assertThat(result).contains("已生成");
        assertThat(result).contains("怪物配置");
    }

    @Test
    void generateMonstersReturnsFailureOnEmptyResponse() {
        when(llmClient.call(any(ChatRequest.class))).thenReturn(new ChatResponse(""));

        String result = tool.generateMonsters("5个森林怪物", 5, null);

        assertThat(result).contains("失败");
    }

    @Test
    void generateSkillsReturnsSuccess() {
        when(llmClient.call(any(ChatRequest.class))).thenReturn(new ChatResponse(
                "[{\"id\":1,\"name\":\"Fireball\",\"type\":\"fire\",\"level\":10,\"damage\":200,\"cooldown\":3000,\"range\":15,\"description\":\"A ball of fire\"}]"
        ));

        String result = tool.generateSkills("3个火系技能", 3, null);

        assertThat(result).contains("已生成");
        assertThat(result).contains("技能配置");
    }

    @Test
    void generateItemsReturnsSuccess() {
        when(llmClient.call(any(ChatRequest.class))).thenReturn(new ChatResponse(
                "[{\"id\":1,\"name\":\"Iron Sword\",\"type\":\"weapon\",\"rarity\":\"common\",\"value\":100,\"description\":\"A basic sword\"}]"
        ));

        String result = tool.generateItems("5个武器", 5, null);

        assertThat(result).contains("已生成");
        assertThat(result).contains("道具配置");
    }

    @Test
    void generateQuestsReturnsSuccess() {
        when(llmClient.call(any(ChatRequest.class))).thenReturn(new ChatResponse(
                "[{\"id\":1,\"name\":\"Kill Goblins\",\"type\":\"main\",\"minLevel\":5,\"objective\":\"Kill 10 goblins\",\"rewards\":[\"100 gold\"],\"description\":\"A quest to kill goblins\"}]"
        ));

        String result = tool.generateQuests("3个主线任务", 3, null);

        assertThat(result).contains("已生成");
        assertThat(result).contains("任务配置");
    }

    @Test
    void generateGrowthCurveReturnsSuccess() {
        String result = tool.generateGrowthCurve(1, 10, 100, 20, 10, 1.2, null);

        assertThat(result).contains("已生成");
        assertThat(result).contains("成长曲线");
    }

    @Test
    void generateGrowthCurveWritesCsvFile() throws IOException {
        String result = tool.generateGrowthCurve(1, 5, 100, 20, 10, 1.2, "test_curve.csv");

        assertThat(result).contains("test_curve.csv");
        Path csvFile = tempDir.resolve("workspace").resolve("output/configs/test_curve.csv");
        assertThat(Files.exists(csvFile)).isTrue();
        String content = Files.readString(csvFile);
        assertThat(content).startsWith("level,hp,attack,defense,exp_required");
        assertThat(content).contains("1,");
    }

    @Test
    void generateMonstersWithCustomFilename() {
        when(llmClient.call(any(ChatRequest.class))).thenReturn(new ChatResponse(
                "[{\"id\":1,\"name\":\"Goblin\",\"level\":5,\"hp\":200,\"attack\":30,\"defense\":10,\"dropRate\":0.5,\"skills\":[{\"id\":1,\"probability\":0.8}],\"description\":\"A small green creature\"}]"
        ));

        String result = tool.generateMonsters("5个森林怪物", 5, "custom_monsters.json");

        assertThat(result).contains("custom_monsters.json");
    }
}

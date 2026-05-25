package ai.gameclaw.tools.game;

import ai.gameclaw.agent.llm.ChatRequest;
import ai.gameclaw.agent.llm.ChatResponse;
import ai.gameclaw.agent.llm.LlmClient;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameCodeToolTest {

    @TempDir
    Path tempDir;

    @Mock
    LlmClient llmClient;

    ApiHallucinationDetector detector;
    SandboxWriter sandboxWriter;
    GameCodeTool tool;

    @BeforeEach
    void setUp() throws IOException {
        Path workspace = tempDir.resolve("workspace");
        Files.createDirectories(workspace);
        Path skillsDir = workspace.resolve("game-skills");

        Path unityDir = skillsDir.resolve("unity");
        Files.createDirectories(unityDir);
        Files.writeString(unityDir.resolve("api-index.json"), """
                {"engine":"unity","apis":[
                    {"fqn":"UnityEngine.MonoBehaviour","kind":"class","since":"1.0"},
                    {"fqn":"UnityEngine.MonoBehaviour.Update","kind":"method","since":"1.0"},
                    {"fqn":"UnityEngine.GameObject","kind":"class","since":"1.0"},
                    {"fqn":"UnityEngine.GameObject.Find","kind":"method","since":"1.0"},
                    {"fqn":"UnityEngine.Debug","kind":"class","since":"1.0"},
                    {"fqn":"UnityEngine.Debug.Log","kind":"method","since":"1.0"},
                    {"fqn":"UnityEngine.Transform","kind":"class","since":"1.0"},
                    {"fqn":"UnityEngine.Input","kind":"class","since":"1.0"},
                    {"fqn":"UnityEngine.Vector3","kind":"class","since":"1.0"},
                    {"fqn":"UnityEngine.SerializeField","kind":"class","since":"1.0"},
                    {"fqn":"UnityEngine.Rigidbody","kind":"class","since":"1.0"}
                ]}
                """);

        Path godotDir = skillsDir.resolve("godot");
        Files.createDirectories(godotDir);
        Files.writeString(godotDir.resolve("api-index.json"), """
                {"engine":"godot","apis":[
                    {"fqn":"get_node","kind":"method","since":"1.0"},
                    {"fqn":"queue_free","kind":"method","since":"1.0"},
                    {"fqn":"_ready","kind":"method","since":"1.0"},
                    {"fqn":"_process","kind":"method","since":"1.0"}
                ]}
                """);

        detector = new ApiHallucinationDetector(new FileSystemResource(workspace.toFile()));
        sandboxWriter = new SandboxWriter(new FileSystemResource(workspace.toFile()));
        tool = new GameCodeTool(llmClient, sandboxWriter, detector);
    }

    @Test
    void generateUnityScriptWithKnownApis() {
        when(llmClient.call(any(ChatRequest.class))).thenReturn(new ChatResponse(
                "using UnityEngine;\npublic class PlayerController : MonoBehaviour {\n" +
                        "    void Update() {\n        GameObject.Find(\"Player\");\n        Debug.Log(\"moving\");\n    }\n}"
        ));

        String result = tool.generateUnityScript("玩家移动控制器", "PlayerController.cs");

        assertThat(result).contains("代码已写入");
        assertThat(result).doesNotContain("⚠️");
    }

    @Test
    void generateUnityScriptWithHallucinatedApi() {
        when(llmClient.call(any(ChatRequest.class))).thenReturn(new ChatResponse(
                "using UnityEngine;\npublic class PlayerController : MonoBehaviour {\n" +
                        "    void Update() {\n        UnityEngine.AssetBundle.LoadFancyAsset(\"test\");\n    }\n}"
        ));

        String result = tool.generateUnityScript("加载资源", "AssetLoader.cs");

        assertThat(result).contains("⚠️");
    }

    @Test
    void generateUnityScriptWithHighHallucinationRate() {
        when(llmClient.call(any(ChatRequest.class))).thenReturn(new ChatResponse(
                "using UnityEngine;\npublic class Fake : MonoBehaviour {\n" +
                        "    void Update() {\n        UnityEngine.FakeApi.NonExistent();\n" +
                        "        UnityEngine.AnotherFake.WrongMethod();\n    }\n}"
        ));

        String result = tool.generateUnityScript("假API测试", "Fake.cs");

        assertThat(result).contains("⚠️");
        assertThat(result).contains("幻觉率");
    }

    @Test
    void generateGodotScriptWithKnownApis() {
        when(llmClient.call(any(ChatRequest.class))).thenReturn(new ChatResponse(
                "extends Node2D\n\nfunc _ready():\n    get_node(\"Player\")\n\nfunc _process(delta):\n    pass"
        ));

        String result = tool.generateGodotScript("玩家节点", "player.gd");

        assertThat(result).contains("代码已写入");
    }

    @Test
    void generateScriptWithEmptyResponse() {
        when(llmClient.call(any(ChatRequest.class))).thenReturn(new ChatResponse(""));

        String result = tool.generateUnityScript("空测试", "Empty.cs");

        assertThat(result).contains("代码生成失败");
    }

    @Test
    void queryEngineApiReturnsResults() {
        String result = tool.queryEngineApi("Unity", "GameObject");

        assertThat(result).contains("Unity");
        assertThat(result).contains("GameObject");
    }

    @Test
    void queryEngineApiWithNoMatch() {
        String result = tool.queryEngineApi("Unity", "nonexistent_xyz_abc");

        assertThat(result).contains("未找到");
    }

    @Test
    void stripMarkdownFenceFromResponse() {
        when(llmClient.call(any(ChatRequest.class))).thenReturn(new ChatResponse(
                "```csharp\nusing UnityEngine;\npublic class Test : MonoBehaviour { }\n```"
        ));

        String result = tool.generateUnityScript("测试", "Test.cs");

        assertThat(result).contains("代码已写入");
    }
}

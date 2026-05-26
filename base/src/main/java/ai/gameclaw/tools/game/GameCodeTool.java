package ai.gameclaw.tools.game;

import ai.gameclaw.agent.llm.ChatRequest;
import ai.gameclaw.agent.llm.LlmClient;
import ai.gameclaw.security.RequireRiskLevel;
import ai.gameclaw.security.RiskLevel;
import ai.gameclaw.security.TenantContext;
import ai.gameclaw.security.TenantContextHolder;
import ai.gameclaw.tools.GameTool;
import ai.gameclaw.tools.sandbox.SandboxWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Map;

@Component
@GameTool
public class GameCodeTool {

    private static final Logger log = LoggerFactory.getLogger(GameCodeTool.class);

    private final LlmClient llmClient;
    private final SandboxWriter sandboxWriter;
    private final ApiHallucinationDetector hallucinationDetector;

    public GameCodeTool(LlmClient llmClient, SandboxWriter sandboxWriter,
                        ApiHallucinationDetector hallucinationDetector) {
        this.llmClient = llmClient;
        this.sandboxWriter = sandboxWriter;
        this.hallucinationDetector = hallucinationDetector;
    }

    @Tool(name = "generate_unity_script", description = "生成Unity C#脚本代码，含API幻觉检测")
    @RequireRiskLevel(RiskLevel.L2_SANDBOX_WRITE)
    public String generateUnityScript(
            @ToolParam(description = "需求描述，例：玩家移动控制器，WASD控制") String requirement,
            @ToolParam(description = "文件名，例：PlayerController.cs") String filename) {
        return generateScript(Engine.UNITY, requirement, filename);
    }

    @Tool(name = "generate_unreal_script", description = "生成Unreal C++代码，含API幻觉检测")
    @RequireRiskLevel(RiskLevel.L2_SANDBOX_WRITE)
    public String generateUnrealScript(
            @ToolParam(description = "需求描述") String requirement,
            @ToolParam(description = "文件名，例：PlayerCharacter.h") String filename) {
        return generateScript(Engine.UNREAL, requirement, filename);
    }

    @Tool(name = "generate_godot_script", description = "生成Godot GDScript代码，含API幻觉检测")
    @RequireRiskLevel(RiskLevel.L2_SANDBOX_WRITE)
    public String generateGodotScript(
            @ToolParam(description = "需求描述") String requirement,
            @ToolParam(description = "文件名，例：player.gd") String filename) {
        return generateScript(Engine.GODOT, requirement, filename);
    }

    @Tool(name = "query_engine_api", description = "查询游戏引擎API用法，返回API签名和文档链接")
    @RequireRiskLevel(RiskLevel.L1_READ)
    public String queryEngineApi(
            @ToolParam(description = "引擎名称: Unity/Unreal/Godot") String engine,
            @ToolParam(description = "查询内容，例：如何加载场景") String query) {
        Engine e = Engine.fromString(engine);
        return hallucinationDetector.queryApi(e, query);
    }

    private String generateScript(Engine engine, String requirement, String filename) {
        String prompt = buildCodePrompt(engine, requirement);
        var response = llmClient.call(new ChatRequest(prompt, null, Map.of()));
        String code = response.content();

        if (code == null || code.isBlank()) {
            return "代码生成失败：LLM返回空内容";
        }

        code = stripMarkdownFence(code);

        var detection = hallucinationDetector.detect(engine, code);
        StringBuilder result = new StringBuilder();

        if (!detection.unknownApis().isEmpty()) {
            result.append("⚠️ 检测到可能不存在的API:\n");
            for (String api : detection.unknownApis()) {
                result.append("  - ").append(api).append("\n");
            }
            double ratio = (double) detection.unknownApis().size() / Math.max(detection.totalApis(), 1);
            if (ratio >= 0.3) {
                result.append("\n幻觉率 ").append(String.format("%.0f%%", ratio * 100))
                        .append(" 过高(≥30%)，建议重新生成\n");
            }
            result.append("\n");
        }

        try {
            TenantContext ctx = TenantContextHolder.tryGet().orElse(null);
            Path file = sandboxWriter.write(ctx, Path.of("output/code", filename), code.getBytes());
            result.append("代码已写入: ").append(file);
        } catch (Exception e) {
            result.append("代码生成成功但写入失败: ").append(e.getMessage());
        }

        return result.toString();
    }

    private String stripMarkdownFence(String code) {
        String trimmed = code.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline > 0) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
            int lastFence = trimmed.lastIndexOf("```");
            if (lastFence > 0) {
                trimmed = trimmed.substring(0, lastFence);
            }
        }
        return trimmed.trim();
    }

    private String buildCodePrompt(Engine engine, String requirement) {
        return switch (engine) {
            case UNITY -> "Generate a Unity C# script for: " + requirement + "\n" +
                    "Use only real UnityEngine/UnityEditor APIs. Follow Unity conventions (MonoBehaviour, SerializeField, etc).\n" +
                    "Return ONLY the C# code, no markdown fences, no explanations.";

            case UNREAL -> "Generate an Unreal Engine C++ header/source for: " + requirement + "\n" +
                    "Use only real Unreal Engine APIs (UCLASS, UPROPERTY, UFUNCTION, AActor, etc).\n" +
                    "Return ONLY the C++ code, no markdown fences, no explanations.";

            case GODOT -> "Generate a Godot GDScript for: " + requirement + "\n" +
                    "Use only real Godot 4.x APIs (extends, @export, _ready, _process, etc).\n" +
                    "Return ONLY the GDScript code, no markdown fences, no explanations.";
        };
    }
}

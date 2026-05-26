package ai.gameclaw.tools.game;

import ai.gameclaw.agent.llm.ChatRequest;
import ai.gameclaw.agent.llm.LlmClient;
import ai.gameclaw.governance.ValidatedLlmOutput;
import ai.gameclaw.governance.ValidationGate;
import ai.gameclaw.observability.AiMetrics;
import ai.gameclaw.security.RequireRiskLevel;
import ai.gameclaw.security.RiskLevel;
import ai.gameclaw.security.TenantContext;
import ai.gameclaw.security.TenantContextHolder;
import ai.gameclaw.tools.GameTool;
import ai.gameclaw.tools.game.model.GrowthCurvePoint;
import ai.gameclaw.tools.game.model.ItemConfig;
import ai.gameclaw.tools.game.model.MonsterConfig;
import ai.gameclaw.tools.game.model.QuestConfig;
import ai.gameclaw.tools.game.model.SkillConfig;
import ai.gameclaw.tools.sandbox.SandboxWriter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Component
@GameTool
public class GameDesignTool {

    private static final Logger log = LoggerFactory.getLogger(GameDesignTool.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final LlmClient llmClient;
    private final SandboxWriter sandboxWriter;
    private final List<ValidationGate> gates;
    private final ObjectProvider<AiMetrics> aiMetricsProvider;

    public GameDesignTool(@Lazy LlmClient llmClient, SandboxWriter sandboxWriter,
                          List<ValidationGate> gates,
                          ObjectProvider<AiMetrics> aiMetricsProvider) {
        this.llmClient = llmClient;
        this.sandboxWriter = sandboxWriter;
        this.gates = gates;
        this.aiMetricsProvider = aiMetricsProvider;
    }

    @Tool(name = "generate_monsters", description = "根据描述生成怪物配置表，输出JSON写入沙箱目录")
    @RequireRiskLevel(RiskLevel.L2_SANDBOX_WRITE)
    public String generateMonsters(
            @ToolParam(description = "描述，例：30个森林怪物，等级10-25，血量200-2000") String description,
            @ToolParam(description = "生成数量，1-500") int count,
            @ToolParam(description = "文件名，可选", required = false) String filename) {

        String prompt = buildMonsterPrompt(description, count);
        var result = callWithValidation(prompt, new TypeReference<List<MonsterConfig>>() {}, "generate_monsters");

        if (result == null) {
            return "生成怪物配置失败：闸门验证未通过，请调整描述后重试";
        }

        try {
            String fname = filename != null ? filename : "monsters.json";
            TenantContext ctx = TenantContextHolder.tryGet().orElse(null);
            Path file = sandboxWriter.write(ctx, Path.of("output/configs", fname),
                    MAPPER.writerWithDefaultPrettyPrinter().writeValueAsBytes(result));
            return "已生成 " + result.size() + " 条怪物配置，写入: " + file;
        } catch (Exception e) {
            log.error("[GameDesign] Failed to write monsters: {}", e.getMessage());
            return "生成成功但写入失败: " + e.getMessage();
        }
    }

    @Tool(name = "generate_skills", description = "根据描述生成技能配置表，输出JSON写入沙箱目录")
    @RequireRiskLevel(RiskLevel.L2_SANDBOX_WRITE)
    public String generateSkills(
            @ToolParam(description = "描述，例：10个火系技能，伤害100-500") String description,
            @ToolParam(description = "生成数量，1-200") int count,
            @ToolParam(description = "文件名，可选", required = false) String filename) {

        String prompt = buildSkillPrompt(description, count);
        var result = callWithValidation(prompt, new TypeReference<List<SkillConfig>>() {}, "generate_skills");

        if (result == null) {
            return "生成技能配置失败：闸门验证未通过，请调整描述后重试";
        }

        try {
            String fname = filename != null ? filename : "skills.json";
            TenantContext ctx = TenantContextHolder.tryGet().orElse(null);
            Path file = sandboxWriter.write(ctx, Path.of("output/configs", fname),
                    MAPPER.writerWithDefaultPrettyPrinter().writeValueAsBytes(result));
            return "已生成 " + result.size() + " 条技能配置，写入: " + file;
        } catch (Exception e) {
            return "生成成功但写入失败: " + e.getMessage();
        }
    }

    @Tool(name = "generate_items", description = "根据描述生成道具配置表，输出JSON写入沙箱目录")
    @RequireRiskLevel(RiskLevel.L2_SANDBOX_WRITE)
    public String generateItems(
            @ToolParam(description = "描述，例：20个稀有武器，价值1000-5000") String description,
            @ToolParam(description = "生成数量，1-500") int count,
            @ToolParam(description = "文件名，可选", required = false) String filename) {

        String prompt = buildItemPrompt(description, count);
        var result = callWithValidation(prompt, new TypeReference<List<ItemConfig>>() {}, "generate_items");

        if (result == null) {
            return "生成道具配置失败：闸门验证未通过，请调整描述后重试";
        }

        try {
            String fname = filename != null ? filename : "items.json";
            TenantContext ctx = TenantContextHolder.tryGet().orElse(null);
            Path file = sandboxWriter.write(ctx, Path.of("output/configs", fname),
                    MAPPER.writerWithDefaultPrettyPrinter().writeValueAsBytes(result));
            return "已生成 " + result.size() + " 条道具配置，写入: " + file;
        } catch (Exception e) {
            return "生成成功但写入失败: " + e.getMessage();
        }
    }

    @Tool(name = "generate_quests", description = "根据描述生成任务配置表，输出JSON写入沙箱目录")
    @RequireRiskLevel(RiskLevel.L2_SANDBOX_WRITE)
    public String generateQuests(
            @ToolParam(description = "描述，例：5个主线任务，等级1-30") String description,
            @ToolParam(description = "生成数量，1-100") int count,
            @ToolParam(description = "文件名，可选", required = false) String filename) {

        String prompt = buildQuestPrompt(description, count);
        var result = callWithValidation(prompt, new TypeReference<List<QuestConfig>>() {}, "generate_quests");

        if (result == null) {
            return "生成任务配置失败：闸门验证未通过，请调整描述后重试";
        }

        try {
            String fname = filename != null ? filename : "quests.json";
            TenantContext ctx = TenantContextHolder.tryGet().orElse(null);
            Path file = sandboxWriter.write(ctx, Path.of("output/configs", fname),
                    MAPPER.writerWithDefaultPrettyPrinter().writeValueAsBytes(result));
            return "已生成 " + result.size() + " 条任务配置，写入: " + file;
        } catch (Exception e) {
            return "生成成功但写入失败: " + e.getMessage();
        }
    }

    @Tool(name = "generate_growth_curve", description = "生成数值成长曲线，输出CSV写入沙箱目录")
    @RequireRiskLevel(RiskLevel.L2_SANDBOX_WRITE)
    public String generateGrowthCurve(
            @ToolParam(description = "起始等级") int startLevel,
            @ToolParam(description = "结束等级") int endLevel,
            @ToolParam(description = "基础HP") int baseHp,
            @ToolParam(description = "基础攻击") int baseAttack,
            @ToolParam(description = "基础防御") int baseDefense,
            @ToolParam(description = "成长系数，默认1.2") double growthFactor,
            @ToolParam(description = "文件名，可选", required = false) String filename) {

        List<GrowthCurvePoint> curve = computeGrowthCurve(startLevel, endLevel, baseHp, baseAttack, baseDefense, growthFactor);

        try {
            StringBuilder csv = new StringBuilder();
            csv.append("level,hp,attack,defense,exp_required\n");
            for (GrowthCurvePoint p : curve) {
                csv.append(String.format("%d,%d,%d,%d,%.0f\n", p.level(), p.hp(), p.attack(), p.defense(), p.expRequired()));
            }
            String fname = filename != null ? filename : "growth_curve.csv";
            TenantContext ctx = TenantContextHolder.tryGet().orElse(null);
            Path file = sandboxWriter.write(ctx, Path.of("output/configs", fname), csv.toString().getBytes());
            return "已生成 " + curve.size() + " 级成长曲线，写入: " + file;
        } catch (Exception e) {
            return "生成成长曲线失败: " + e.getMessage();
        }
    }

    private <T> T callWithValidation(String prompt, TypeReference<T> typeRef, String toolName) {
        var response = llmClient.call(new ChatRequest(prompt, null, Map.of()));
        String content = response.content();
        if (content == null || content.isBlank()) {
            return null;
        }
        try {
            Object parsed = MAPPER.readValue(content, typeRef);
            @SuppressWarnings("unchecked")
            Class<T> type = (Class<T>) (typeRef.getType() instanceof Class<?> c ? c : Object.class);
            var validated = ValidatedLlmOutput.validate(
                    () -> parsed, type, gates, aiMetricsProvider.getIfAvailable(), toolName, 2);
            if (validated.result() != null) {
                return validated.result();
            }
            return null;
        } catch (Exception e) {
            log.warn("[GameDesign] LLM output parse failed for {}: {}", toolName, e.getMessage());
            return null;
        }
    }

    private List<GrowthCurvePoint> computeGrowthCurve(int startLevel, int endLevel, int baseHp, int baseAttack, int baseDefense, double growthFactor) {
        return java.util.stream.IntStream.rangeClosed(startLevel, endLevel)
                .mapToObj(level -> {
                    double factor = Math.pow(growthFactor, level - startLevel);
                    return new GrowthCurvePoint(
                            level,
                            (int) (baseHp * factor),
                            (int) (baseAttack * factor),
                            (int) (baseDefense * factor),
                            Math.round(baseHp * factor * 10.0) / 10.0
                    );
                })
                .toList();
    }

    private String buildMonsterPrompt(String description, int count) {
        return "Generate exactly " + count + " monster configurations based on this description: " + description + "\n" +
                "Return a JSON array. Each element must have: id (int), name (string), level (int 1-100), hp (int), attack (int), defense (int), dropRate (double 0-1), skills (array of {id:int, probability:double}), description (string).\n" +
                "Ensure variety in stats and names. Return ONLY valid JSON, no markdown.";
    }

    private String buildSkillPrompt(String description, int count) {
        return "Generate exactly " + count + " skill configurations based on this description: " + description + "\n" +
                "Return a JSON array. Each element must have: id (int), name (string), type (string like fire/ice/heal), level (int 1-100), damage (int), cooldown (int ms), range (int), description (string).\n" +
                "Return ONLY valid JSON, no markdown.";
    }

    private String buildItemPrompt(String description, int count) {
        return "Generate exactly " + count + " item configurations based on this description: " + description + "\n" +
                "Return a JSON array. Each element must have: id (int), name (string), type (string like weapon/armor/potion), rarity (string like common/rare/epic/legendary), value (int), description (string).\n" +
                "Return ONLY valid JSON, no markdown.";
    }

    private String buildQuestPrompt(String description, int count) {
        return "Generate exactly " + count + " quest configurations based on this description: " + description + "\n" +
                "Return a JSON array. Each element must have: id (int), name (string), type (string like main/side/daily), minLevel (int), objective (string), rewards (array of strings), description (string).\n" +
                "Return ONLY valid JSON, no markdown.";
    }
}

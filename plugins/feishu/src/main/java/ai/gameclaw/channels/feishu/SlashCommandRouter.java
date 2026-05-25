package ai.gameclaw.channels.feishu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class SlashCommandRouter {

    private static final Logger log = LoggerFactory.getLogger(SlashCommandRouter.class);

    private final Map<String, SlashHandler> handlers;

    public SlashCommandRouter(Map<String, SlashHandler> handlers) {
        this.handlers = handlers != null ? handlers : Map.of();
    }

    public String route(String text, FeishuEvent event) {
        String[] parts = text.trim().split("\\s+", 2);
        String command = parts[0];
        String args = parts.length > 1 ? parts[1] : "";

        SlashHandler handler = handlers.get(command);
        if (handler == null) {
            return "未知命令：" + command + "\n可用命令：" + String.join(", ", handlers.keySet());
        }
        try {
            return handler.handle(args, event);
        } catch (Exception e) {
            log.error("[SlashCommandRouter] Handler error for {}: {}", command, e.getMessage());
            return "命令执行失败：" + e.getMessage();
        }
    }
}

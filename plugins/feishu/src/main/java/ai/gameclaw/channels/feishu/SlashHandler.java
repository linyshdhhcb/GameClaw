package ai.gameclaw.channels.feishu;

public interface SlashHandler {
    String handle(String args, FeishuEvent event);
}

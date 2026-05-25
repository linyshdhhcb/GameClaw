package ai.gameclaw.channels.feishu;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FeishuCardBuilderTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void buildCardWithHeaderAndMarkdown() throws Exception {
        String json = FeishuCardBuilder.create()
                .header("Test Title")
                .markdown("Hello **world**")
                .build();

        var card = MAPPER.readTree(json);
        assertThat(card.get("header").get("title").get("content").asText()).isEqualTo("Test Title");
        assertThat(card.get("elements").size()).isEqualTo(1);
        assertThat(card.get("elements").get(0).get("tag").asText()).isEqualTo("markdown");
    }

    @Test
    void buildCardWithCodeBlock() throws Exception {
        String json = FeishuCardBuilder.create()
                .header("Code")
                .codeBlock("System.out.println(\"hi\");", "java")
                .build();

        var card = MAPPER.readTree(json);
        var content = card.get("elements").get(0).get("content").asText();
        assertThat(content).contains("```java");
        assertThat(content).contains("System.out.println");
    }

    @Test
    void buildCardWithTable() throws Exception {
        String json = FeishuCardBuilder.create()
                .header("Results")
                .table(new String[]{"Name", "Score"}, List.of(
                        new String[]{"Alice", "95"},
                        new String[]{"Bob", "87"}
                ))
                .build();

        var card = MAPPER.readTree(json);
        var table = card.get("elements").get(0);
        assertThat(table.get("tag").asText()).isEqualTo("table");
        assertThat(table.get("columns").size()).isEqualTo(2);
        assertThat(table.get("rows").size()).isEqualTo(2);
    }

    @Test
    void buildCardWithDividerAndNote() throws Exception {
        String json = FeishuCardBuilder.create()
                .markdown("Content")
                .divider()
                .note("Footer note")
                .build();

        var card = MAPPER.readTree(json);
        assertThat(card.get("elements").size()).isEqualTo(3);
        assertThat(card.get("elements").get(1).get("tag").asText()).isEqualTo("hr");
        assertThat(card.get("elements").get(2).get("tag").asText()).isEqualTo("note");
    }

    @Test
    void wrapResponsePlainTextReturnsNull() {
        String result = FeishuCardBuilder.wrapResponse("Just a simple text");
        assertThat(result).isNull();
    }

    @Test
    void wrapResponseWithCodeBlockReturnsCard() throws Exception {
        String response = "Here is the code:\n```java\nhello\n```\nDone.";
        String result = FeishuCardBuilder.wrapResponse(response);
        assertThat(result).isNotNull();

        var card = MAPPER.readTree(result);
        assertThat(card.get("elements").size()).isGreaterThan(0);
    }

    @Test
    void wrapResponseWithTableReturnsCard() throws Exception {
        String response = "| Name | Score |\n| --- | --- |\n| Alice | 95 |";
        String result = FeishuCardBuilder.wrapResponse(response);
        assertThat(result).isNotNull();

        var card = MAPPER.readTree(result);
        var firstElement = card.get("elements").get(0);
        assertThat(firstElement.get("tag").asText()).isEqualTo("table");
    }

    @Test
    void wrapResponseLongTextReturnsCard() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("This is a long text line ").append(i).append(". ");
        }
        String result = FeishuCardBuilder.wrapResponse(sb.toString());
        assertThat(result).isNotNull();
    }

    @Test
    void wrapResponseNullReturnsNull() {
        assertThat(FeishuCardBuilder.wrapResponse(null)).isNull();
        assertThat(FeishuCardBuilder.wrapResponse("")).isNull();
        assertThat(FeishuCardBuilder.wrapResponse("   ")).isNull();
    }
}

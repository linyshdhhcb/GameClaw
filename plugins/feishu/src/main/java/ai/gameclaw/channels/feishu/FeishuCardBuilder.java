package ai.gameclaw.channels.feishu;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FeishuCardBuilder {

    private final List<Map<String, Object>> elements = new ArrayList<>();
    private String headerTitle;
    private String headerTemplate;

    public FeishuCardBuilder header(String title) {
        return header(title, "blue");
    }

    public FeishuCardBuilder header(String title, String template) {
        this.headerTitle = title;
        this.headerTemplate = template;
        return this;
    }

    public FeishuCardBuilder markdown(String content) {
        elements.add(Map.of("tag", "markdown", "content", content));
        return this;
    }

    public FeishuCardBuilder codeBlock(String code, String language) {
        String lang = language != null ? language : "";
        String content = "```" + lang + "\n" + code + "\n```";
        elements.add(Map.of("tag", "markdown", "content", content));
        return this;
    }

    public FeishuCardBuilder codeBlock(String code) {
        return codeBlock(code, null);
    }

    public FeishuCardBuilder table(String[] headers, List<String[]> rows) {
        List<Map<String, Object>> columns = new ArrayList<>();
        for (String header : headers) {
            Map<String, Object> col = new LinkedHashMap<>();
            col.put("name", header);
            col.put("display_name", header);
            col.put("data_type", "text");
            columns.add(col);
        }

        List<Map<String, Object>> rowList = new ArrayList<>();
        for (String[] row : rows) {
            Map<String, Object> rowMap = new LinkedHashMap<>();
            for (int i = 0; i < headers.length; i++) {
                rowMap.put(headers[i], i < row.length ? row[i] : "");
            }
            rowList.add(rowMap);
        }

        Map<String, Object> tableElement = new LinkedHashMap<>();
        tableElement.put("tag", "table");
        tableElement.put("page_size", Math.min(rows.size(), 20));
        tableElement.put("row_count", rows.size());
        tableElement.put("columns", columns);
        tableElement.put("rows", rowList);
        elements.add(tableElement);
        return this;
    }

    public FeishuCardBuilder divider() {
        elements.add(Map.of("tag", "hr"));
        return this;
    }

    public FeishuCardBuilder note(String content) {
        elements.add(Map.of(
                "tag", "note",
                "elements", List.of(Map.of("tag", "plain_text", "content", content))
        ));
        return this;
    }

    public FeishuCardBuilder action(String label, String url, String type) {
        Map<String, Object> button = new LinkedHashMap<>();
        button.put("tag", "button");
        button.put("text", Map.of("tag", "plain_text", "content", label));
        button.put("type", type != null ? type : "primary");
        button.put("url", url);
        elements.add(Map.of("tag", "action", "actions", List.of(button)));
        return this;
    }

    public String build() {
        Map<String, Object> card = new LinkedHashMap<>();
        if (headerTitle != null) {
            card.put("header", Map.of(
                    "title", Map.of("tag", "plain_text", "content", headerTitle),
                    "template", headerTemplate != null ? headerTemplate : "blue"
            ));
        }
        card.put("elements", elements);
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(card);
        } catch (Exception e) {
            return "{\"elements\":[{\"tag\":\"markdown\",\"content\":\"Card build error\"}]}";
        }
    }

    public static FeishuCardBuilder create() {
        return new FeishuCardBuilder();
    }

    public static String wrapResponse(String response) {
        if (response == null || response.isBlank()) {
            return null;
        }

        boolean hasTable = response.contains("|") && response.contains("---");
        boolean hasCode = response.contains("```");
        boolean isLong = response.length() > 500;

        if (!hasTable && !hasCode && !isLong) {
            return null;
        }

        FeishuCardBuilder builder = FeishuCardBuilder.create();

        if (hasTable) {
            String[] lines = response.split("\n");
            List<String> tableLines = new ArrayList<>();
            List<String> otherLines = new ArrayList<>();
            boolean inTable = false;

            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.startsWith("|") && trimmed.endsWith("|")) {
                    if (!trimmed.matches("\\|[-\\s|]+\\|")) {
                        tableLines.add(trimmed);
                        inTable = true;
                    }
                } else {
                    if (inTable) {
                        appendTable(builder, tableLines);
                        tableLines.clear();
                        inTable = false;
                    }
                    otherLines.add(line);
                }
            }
            if (!tableLines.isEmpty()) {
                appendTable(builder, tableLines);
            }
            if (!otherLines.isEmpty()) {
                builder.markdown(String.join("\n", otherLines));
            }
        } else if (hasCode) {
            builder.markdown(response);
        } else {
            builder.markdown(response);
        }

        return builder.build();
    }

    private static void appendTable(FeishuCardBuilder builder, List<String> tableLines) {
        if (tableLines.isEmpty()) return;

        String[] headers = parseTableRow(tableLines.get(0));
        List<String[]> rows = new ArrayList<>();
        for (int i = 1; i < tableLines.size(); i++) {
            rows.add(parseTableRow(tableLines.get(i)));
        }
        builder.table(headers, rows);
    }

    private static String[] parseTableRow(String line) {
        String trimmed = line.substring(1, line.length() - 1);
        String[] cells = trimmed.split("\\|");
        for (int i = 0; i < cells.length; i++) {
            cells[i] = cells[i].trim();
        }
        return cells;
    }
}

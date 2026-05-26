package ai.gameclaw.security.pii;

import ai.gameclaw.security.Role;
import ai.gameclaw.security.TenantContext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PiiMaskingPostProcessor {

    private PiiMaskingPostProcessor() {}

    public static List<Map<String, Object>> mask(List<Map<String, Object>> rows, TenantContext ctx) {
        boolean canSeePii = ctx.roles().contains(Role.ADMIN) || ctx.roles().contains(Role.DATA_ANALYST);
        return rows.stream().map(row -> {
            Map<String, Object> masked = new LinkedHashMap<>();
            row.forEach((k, v) -> masked.put(k, canSeePii ? v : maskByField(k, v)));
            return masked;
        }).toList();
    }

    static Object maskByField(String field, Object value) {
        if (value == null) return null;
        return switch (PiiFieldRegistry.classify(field)) {
            case PHONE -> PiiMasking.maskPhone(value.toString());
            case EMAIL -> PiiMasking.maskEmail(value.toString());
            case ID_CARD -> PiiMasking.maskIdCard(value.toString());
            case IP -> maskIp(value.toString());
            case USER_ID -> partialKeep(value.toString());
            case NONE -> value;
        };
    }

    public static String maskIp(String ip) {
        if (ip == null) return null;
        String[] parts = ip.split("\\.");
        if (parts.length == 4) {
            return parts[0] + "." + parts[1] + ".*.*";
        }
        if (ip.contains(":")) {
            String[] v6Parts = ip.split(":");
            if (v6Parts.length >= 3) {
                return v6Parts[0] + ":" + v6Parts[1] + ":****";
            }
        }
        return ip.substring(0, Math.min(3, ip.length())) + "***";
    }

    static String partialKeep(String value) {
        if (value == null || value.length() <= 4) return "****";
        return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
    }
}

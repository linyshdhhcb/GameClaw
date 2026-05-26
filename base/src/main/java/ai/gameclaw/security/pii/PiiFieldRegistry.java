package ai.gameclaw.security.pii;

import java.util.Map;
import java.util.Set;

public final class PiiFieldRegistry {

    private static final Map<PiiType, Set<String>> FIELD_PATTERNS = Map.of(
            PiiType.PHONE, Set.of("phone", "mobile", "tel", "telephone", "cellphone", "phone_number", "mobile_number"),
            PiiType.EMAIL, Set.of("email", "mail", "email_address"),
            PiiType.ID_CARD, Set.of("id_card", "idcard", "identity", "id_number", "citizen_id", "sfz"),
            PiiType.IP, Set.of("ip", "ip_address", "client_ip", "remote_ip", "login_ip", "last_ip"),
            PiiType.USER_ID, Set.of("user_id", "uid", "account_id", "player_id", "openid")
    );

    private PiiFieldRegistry() {}

    public static PiiType classify(String fieldName) {
        if (fieldName == null) return PiiType.NONE;
        String lower = fieldName.toLowerCase().replaceAll("[_-]", "");
        for (var entry : FIELD_PATTERNS.entrySet()) {
            for (String pattern : entry.getValue()) {
                if (lower.contains(pattern.replaceAll("[_-]", ""))) {
                    return entry.getKey();
                }
            }
        }
        return PiiType.NONE;
    }
}

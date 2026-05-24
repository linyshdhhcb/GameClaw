package ai.gameclaw.security;

public interface AccessControl {

    boolean isAllowed(String principal, String action, String resource);
}

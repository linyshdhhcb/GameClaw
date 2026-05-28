package ai.gameclaw.governance.rollback;

public enum RollbackKind {
    FILE_WRITE,
    GIT_COMMIT,
    K8S_DEPLOY,
    DB_MIGRATION
}

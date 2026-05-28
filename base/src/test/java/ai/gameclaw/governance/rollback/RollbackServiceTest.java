package ai.gameclaw.governance.rollback;

import ai.gameclaw.observability.AuditLogger;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RollbackServiceTest {

    private RollbackService rollbackService;
    private JdbcTemplate jdbc;
    private ObjectMapper objectMapper;
    private ObjectProvider<AuditLogger> auditLoggerProvider;
    private FilesystemRestorer filesystemRestorer;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        jdbc = mock(JdbcTemplate.class);
        objectMapper = new ObjectMapper();
        auditLoggerProvider = mock(ObjectProvider.class);
        when(auditLoggerProvider.getIfAvailable()).thenReturn(null);
        filesystemRestorer = new FilesystemRestorer();
        ObjectProvider<ObjectMapper> omProvider = mock(ObjectProvider.class);
        when(omProvider.getIfAvailable(any())).thenReturn(objectMapper);
        rollbackService = new RollbackService(jdbc, omProvider, auditLoggerProvider, filesystemRestorer);
    }

    @Test
    void createSnapshotInsertsRecord() {
        UUID approvalId = UUID.randomUUID();

        RollbackSnapshot snapshot = rollbackService.createSnapshot(
                approvalId, "test-resource", RollbackKind.FILE_WRITE,
                Map.of("filePath", "/test/file.txt", "originalContent", "hello"));

        assertThat(snapshot).isNotNull();
        assertThat(snapshot.approvalId()).isEqualTo(approvalId);
        assertThat(snapshot.resource()).isEqualTo("test-resource");
        assertThat(snapshot.kind()).isEqualTo(RollbackKind.FILE_WRITE);
        verify(jdbc).update(anyString(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void findSnapshotReturnsSnapshot() {
        UUID snapshotId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        RollbackSnapshot expected = new RollbackSnapshot(
                snapshotId, tenantId, UUID.randomUUID(), "test-resource",
                RollbackKind.FILE_WRITE, Map.of("filePath", "/test/file.txt", "originalContent", "hello"),
                java.time.Instant.now());

        when(jdbc.query(anyString(), any(RowMapper.class), eq(snapshotId))).thenReturn(java.util.List.of(expected));

        Optional<RollbackSnapshot> result = rollbackService.findSnapshot(snapshotId);

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(snapshotId);
        assertThat(result.get().kind()).isEqualTo(RollbackKind.FILE_WRITE);
    }

    @Test
    void rollbackFileWriteRestoresContent() throws Exception {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "modified content");

        UUID snapshotId = UUID.randomUUID();
        RollbackSnapshot snapshot = new RollbackSnapshot(
                snapshotId, UUID.randomUUID(), UUID.randomUUID(), testFile.toString(),
                RollbackKind.FILE_WRITE,
                Map.of("filePath", testFile.toString(), "originalContent", "original content"),
                java.time.Instant.now());

        when(jdbc.query(anyString(), any(RowMapper.class), eq(snapshotId))).thenReturn(java.util.List.of(snapshot));

        rollbackService.rollback(snapshotId);

        assertThat(Files.readString(testFile)).isEqualTo("original content");
    }

    @Test
    void rollbackWritesAuditLog() throws Exception {
        Path testFile = tempDir.resolve("audit-test.txt");
        Files.writeString(testFile, "modified");

        UUID snapshotId = UUID.randomUUID();
        RollbackSnapshot snapshot = new RollbackSnapshot(
                snapshotId, UUID.randomUUID(), UUID.randomUUID(), "test-resource",
                RollbackKind.FILE_WRITE,
                Map.of("filePath", testFile.toString(), "originalContent", "original"),
                java.time.Instant.now());

        when(jdbc.query(anyString(), any(RowMapper.class), eq(snapshotId))).thenReturn(java.util.List.of(snapshot));

        rollbackService.rollback(snapshotId);

        verify(jdbc).update(contains("audit_log"), any(), any(), any(), any(), any());
    }
}

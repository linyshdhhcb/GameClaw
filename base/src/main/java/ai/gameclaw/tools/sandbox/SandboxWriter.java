package ai.gameclaw.tools.sandbox;

import ai.gameclaw.security.TenantContext;
import ai.gameclaw.security.TenantContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class SandboxWriter {

    private static final Logger log = LoggerFactory.getLogger(SandboxWriter.class);

    private final Path root;

    public SandboxWriter(@Value("${agent.workspace:}") Resource workspace) throws IOException {
        this.root = workspace.getFilePath();
        Files.createDirectories(root.resolve("output"));
    }

    public Path write(TenantContext ctx, Path relative, byte[] content) throws IOException {
        Path tenantRoot = resolveTenantRoot(ctx);
        Path target = tenantRoot.resolve(relative).normalize();
        if (!target.startsWith(tenantRoot)) {
            throw new SecurityException("Path traversal detected: " + relative);
        }
        Files.createDirectories(target.getParent());
        Files.write(target, content);
        log.info("[Sandbox] Written {} bytes to {}", content.length, target);
        return target;
    }

    public Path write(Path relative, byte[] content) throws IOException {
        TenantContext ctx = TenantContextHolder.tryGet().orElse(null);
        return write(ctx, relative, content);
    }

    public boolean exists(TenantContext ctx, Path relative) {
        Path tenantRoot = resolveTenantRoot(ctx);
        Path target = tenantRoot.resolve(relative).normalize();
        if (!target.startsWith(tenantRoot)) {
            return false;
        }
        return Files.exists(target);
    }

    public byte[] read(TenantContext ctx, Path relative) throws IOException {
        Path tenantRoot = resolveTenantRoot(ctx);
        Path target = tenantRoot.resolve(relative).normalize();
        if (!target.startsWith(tenantRoot)) {
            throw new SecurityException("Path traversal detected: " + relative);
        }
        return Files.readAllBytes(target);
    }

    public void delete(TenantContext ctx, Path relative) throws IOException {
        Path tenantRoot = resolveTenantRoot(ctx);
        Path target = tenantRoot.resolve(relative).normalize();
        if (!target.startsWith(tenantRoot)) {
            throw new SecurityException("Path traversal detected: " + relative);
        }
        Files.deleteIfExists(target);
    }

    private Path resolveTenantRoot(TenantContext ctx) {
        if (ctx != null && ctx.tenantId() != null && ctx.projectId() != null) {
            return root.resolve("tenants").resolve(ctx.tenantId().toString())
                    .resolve("projects").resolve(ctx.projectId().toString());
        }
        return root;
    }
}

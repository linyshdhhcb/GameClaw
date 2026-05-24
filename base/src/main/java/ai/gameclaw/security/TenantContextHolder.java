package ai.gameclaw.security;

import java.util.Optional;

public class TenantContextHolder {

    private static final ScopedValue<TenantContext> CTX = ScopedValue.newInstance();

    public static TenantContext require() {
        if (!CTX.isBound()) {
            throw new IllegalStateException("Tenant context not bound — entry point must call runWith");
        }
        return CTX.get();
    }

    public static Optional<TenantContext> tryGet() {
        return CTX.isBound() ? Optional.of(CTX.get()) : Optional.empty();
    }

    public static <R> R runWith(TenantContext ctx, ScopedValue.CallableOp<R, Exception> task) throws Exception {
        return ScopedValue.where(CTX, ctx).call(task);
    }

    public static void runWith(TenantContext ctx, Runnable task) {
        ScopedValue.where(CTX, ctx).run(task);
    }

    public static boolean isBound() {
        return CTX.isBound();
    }
}

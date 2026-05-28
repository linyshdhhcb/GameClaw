package ai.gameclaw.security;

import ai.gameclaw.governance.approval.ApprovalGateway;
import ai.gameclaw.governance.approval.ApprovalNotifier;
import ai.gameclaw.governance.approval.ApprovalRequest;
import ai.gameclaw.governance.approval.ApprovalRequiredException;
import ai.gameclaw.governance.approval.ApprovalState;
import ai.gameclaw.governance.approval.PendingApproval;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApprovalAspectTest {

    private ApprovalGateway approvalGateway;
    private ApprovalNotifier notifier;
    private JdbcTemplate jdbc;
    private ObjectMapper objectMapper;
    private ApprovalAspect aspect;

    @BeforeEach
    void setUp() {
        approvalGateway = mock(ApprovalGateway.class);
        notifier = mock(ApprovalNotifier.class);
        jdbc = mock(JdbcTemplate.class);
        objectMapper = new ObjectMapper();
        ObjectProvider<ObjectMapper> omProvider = mock(ObjectProvider.class);
        when(omProvider.getIfAvailable(any())).thenReturn(objectMapper);
        aspect = new ApprovalAspect(approvalGateway, notifier, jdbc, omProvider);
    }

    @Test
    void interceptsRequireApprovalAnnotation() throws Exception {
        ProceedingJoinPoint pjp = setupPjp("annotatedMethod");

        UUID approvalId = UUID.randomUUID();
        PendingApproval mockApproval = createMockApproval(approvalId, "ApprovalAspectTest.annotatedMethod", 1);

        when(approvalGateway.create(any(ApprovalRequest.class))).thenReturn(mockApproval);

        TenantContext ctx = TenantContext.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Set.of(Role.PROGRAMMER));

        assertThatThrownBy(() -> invokeWithCtx(ctx, pjp))
                .isInstanceOf(ApprovalRequiredException.class);

        verify(approvalGateway).create(any(ApprovalRequest.class));
    }

    @Test
    void throwsApprovalRequiredException() throws Exception {
        ProceedingJoinPoint pjp = setupPjp("annotatedMethod");

        UUID approvalId = UUID.randomUUID();
        PendingApproval mockApproval = createMockApproval(approvalId, "ApprovalAspectTest.annotatedMethod", 1);

        when(approvalGateway.create(any(ApprovalRequest.class))).thenReturn(mockApproval);

        TenantContext ctx = TenantContext.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Set.of(Role.PROGRAMMER));

        assertThatThrownBy(() -> invokeWithCtx(ctx, pjp))
                .isInstanceOf(ApprovalRequiredException.class)
                .satisfies(ex -> {
                    ApprovalRequiredException are = (ApprovalRequiredException) ex;
                    assertThat(are.getApprovalId()).isEqualTo(approvalId);
                });
    }

    @Test
    void createsApprovalWithCorrectQuorum() throws Exception {
        ProceedingJoinPoint pjp = setupPjp("annotatedMethodWithQuorum2");

        UUID approvalId = UUID.randomUUID();
        PendingApproval mockApproval = createMockApproval(approvalId, "ApprovalAspectTest.annotatedMethodWithQuorum2", 2);

        when(approvalGateway.create(any(ApprovalRequest.class))).thenReturn(mockApproval);

        TenantContext ctx = TenantContext.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Set.of(Role.PROGRAMMER));

        assertThatThrownBy(() -> invokeWithCtx(ctx, pjp))
                .isInstanceOf(ApprovalRequiredException.class);

        verify(approvalGateway).create(any(ApprovalRequest.class));
    }

    private ProceedingJoinPoint setupPjp(String methodName) throws Exception {
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        MethodSignature sig = mock(MethodSignature.class);
        when(pjp.getSignature()).thenReturn(sig);
        when(pjp.getTarget()).thenReturn(this);
        Method method = ApprovalAspectTest.class.getMethod(methodName);
        when(sig.getMethod()).thenReturn(method);
        return pjp;
    }

    private PendingApproval createMockApproval(UUID approvalId, String resource, int quorum) {
        return new PendingApproval(
                approvalId, UUID.randomUUID(), UUID.randomUUID(), resource, "method_invocation",
                RiskLevel.L3_PROJECT_WRITE, "Method requires approval", Map.of("quorum", quorum), quorum, List.of(),
                ApprovalState.PENDING, Instant.now().plus(1, ChronoUnit.HOURS), Instant.now());
    }

    private void invokeWithCtx(TenantContext ctx, ProceedingJoinPoint pjp) {
        TenantContextHolder.runWith(ctx, () -> {
            try {
                aspect.enforceApproval(pjp);
            } catch (Throwable t) {
                if (t instanceof RuntimeException re) throw re;
                throw new RuntimeException(t);
            }
        });
    }

    @RequireApproval(approvers = "tech-leads", quorum = 1)
    public void annotatedMethod() {}

    @RequireApproval(approvers = "ops-leads", quorum = 2)
    public void annotatedMethodWithQuorum2() {}
}

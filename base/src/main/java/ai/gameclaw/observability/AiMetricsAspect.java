package ai.gameclaw.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@Aspect
@Component
@ConditionalOnBean(MeterRegistry.class)
public class AiMetricsAspect {

    private static final Logger log = LoggerFactory.getLogger(AiMetricsAspect.class);

    private final AiMetrics aiMetrics;

    public AiMetricsAspect(AiMetrics aiMetrics) {
        this.aiMetrics = aiMetrics;
    }

    @Around("execution(* ai.gameclaw.agent.llm.LlmClient.call(..))")
    public Object aroundLlmCall(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        String model = extractModel(pjp.getArgs());

        aiMetrics.recordLlmRequest(model, "assistant");
        Timer.Sample sample = aiMetrics.startLlmLatency();
        long start = System.nanoTime();

        try {
            Object result = pjp.proceed();
            long elapsed = System.nanoTime() - start;
            aiMetrics.recordLlmLatency(model, sample, elapsed);
            return result;
        } catch (Throwable t) {
            aiMetrics.recordLlmLatency(model, sample, System.nanoTime() - start);
            throw t;
        }
    }

    @Around("@annotation(org.springframework.ai.tool.annotation.Tool)")
    public Object aroundToolCall(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        String toolName = sig.getMethod().getName();
        String result = "success";

        try {
            Object retVal = pjp.proceed();
            return retVal;
        } catch (Throwable t) {
            result = "failed";
            throw t;
        } finally {
            aiMetrics.recordToolCall(toolName, result);
        }
    }

    private String extractModel(Object[] args) {
        if (args != null && args.length > 0 && args[0] != null) {
            return args[0].getClass().getSimpleName();
        }
        return "unknown";
    }
}

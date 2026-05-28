package ai.gameclaw.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireApproval {
    int quorum() default 1;
    String approvers() default "";
    long ttlMinutes() default 60;
}

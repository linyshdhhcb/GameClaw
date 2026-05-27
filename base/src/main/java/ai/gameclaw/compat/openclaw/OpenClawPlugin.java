package ai.gameclaw.compat.openclaw;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface OpenClawPlugin {

    String name();

    String version() default "1.0.0";

    String description() default "";

    String[] permissions() default {};
}

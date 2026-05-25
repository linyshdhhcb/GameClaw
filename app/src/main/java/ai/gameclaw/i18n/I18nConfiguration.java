package ai.gameclaw.i18n;

import io.pebbletemplates.pebble.extension.AbstractExtension;
import io.pebbletemplates.pebble.extension.Function;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Configuration
public class I18nConfiguration implements WebMvcConfigurer {

    @Bean
    public LocaleResolver localeResolver() {
        CookieLocaleResolver resolver = new CookieLocaleResolver("gameclaw-lang");
        resolver.setDefaultLocale(Locale.ENGLISH);
        resolver.setCookieMaxAge(Duration.ofDays(365));
        resolver.setCookiePath("/");
        return resolver;
    }

    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang");
        return interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }

    @Bean
    public AbstractExtension messageSourceExtension(MessageSource messageSource) {
        return new AbstractExtension() {
            @Override
            public Map<String, Function> getFunctions() {
                return Map.of("t", new Function() {
                    @Override
                    public List<String> getArgumentNames() {
                        return List.of("key", "params");
                    }

                    @Override
                    public Object execute(Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber) {
                        String key = (String) args.get("key");
                        Object localeObj = context.getVariable("locale");
                        Locale locale = localeObj instanceof Locale l ? l : context.getLocale();
                        Object[] params = args.get("params") instanceof List<?> list ? list.toArray() : new Object[0];
                        return messageSource.getMessage(key, params, key, locale);
                    }
                });
            }
        };
    }
}

package ai.gameclaw.i18n;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.LocaleResolver;

import java.util.Locale;

@ControllerAdvice
public class I18nControllerAdvice {

    private final LocaleResolver localeResolver;

    public I18nControllerAdvice(LocaleResolver localeResolver) {
        this.localeResolver = localeResolver;
    }

    @ModelAttribute
    public void addLocale(Model model, HttpServletRequest request) {
        Locale locale = localeResolver.resolveLocale(request);
        model.addAttribute("locale", locale);
    }
}

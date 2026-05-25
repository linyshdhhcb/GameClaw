package ai.gameclaw.security.net;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;

@Configuration(proxyBeanMethods = false)
public class OutboundGuardConfig {

    @Bean
    @ConditionalOnBean(OutboundUrlFilter.class)
    public ClientHttpRequestInterceptor outboundGuard(OutboundUrlFilter filter) {
        return (req, body, exec) -> {
            filter.assertAllowed(req.getURI());
            return exec.execute(req, body);
        };
    }
}

package ai.gameclaw.mcp.server.clickhouse;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@ConditionalOnProperty(name = "mcp.security.bearer.enabled", havingValue = "true")
public class BearerTokenFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(BearerTokenFilter.class);

    private final String expectedToken;

    public BearerTokenFilter(@Value("${mcp.security.bearer.token:}") String expectedToken) {
        this.expectedToken = expectedToken;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpReq = (HttpServletRequest) request;
        String authHeader = httpReq.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("[BearerTokenFilter] Missing or invalid Authorization header from {}", httpReq.getRemoteAddr());
            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing Bearer token");
            return;
        }

        String token = authHeader.substring(7);
        if (!expectedToken.equals(token)) {
            log.warn("[BearerTokenFilter] Invalid Bearer token from {}", httpReq.getRemoteAddr());
            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid Bearer token");
            return;
        }

        chain.doFilter(request, response);
    }
}

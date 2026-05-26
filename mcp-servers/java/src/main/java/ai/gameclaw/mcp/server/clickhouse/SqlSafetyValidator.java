package ai.gameclaw.mcp.server.clickhouse;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class SqlSafetyValidator {

    private static final Logger log = LoggerFactory.getLogger(SqlSafetyValidator.class);

    private static final Set<String> DANGEROUS_PATTERNS = Set.of(
            "into outfile", "into dumpfile", "load_file(",
            "system.users", "system.processes", "system.query_log"
    );

    public void assertSelectOnly(String sql) {
        Statement stmt;
        try {
            stmt = CCJSqlParserUtil.parse(sql);
        } catch (JSQLParserException e) {
            throw new SecurityException("Unparseable SQL: " + e.getMessage());
        }
        if (!(stmt instanceof Select)) {
            throw new SecurityException("Only SELECT allowed, got: " + stmt.getClass().getSimpleName());
        }
        String lowerSql = sql.toLowerCase();
        for (String pattern : DANGEROUS_PATTERNS) {
            if (lowerSql.contains(pattern)) {
                throw new SecurityException("Dangerous SQL pattern detected: " + pattern);
            }
        }
        log.info("[SqlSafetyValidator] SQL passed validation: {}", sql.substring(0, Math.min(sql.length(), 100)));
    }
}

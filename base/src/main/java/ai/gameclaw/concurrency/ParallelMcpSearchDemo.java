package ai.gameclaw.concurrency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ParallelMcpSearchDemo {

    private static final Logger log = LoggerFactory.getLogger(ParallelMcpSearchDemo.class);

    public record SearchResult(String source, String content, double score) {}

    public SearchResult searchAny(String query) throws Exception {
        log.info("[ParallelMcpSearch] Racing search for: {}", query);
        return Scopes.race(List.of(
                () -> searchBrave(query),
                () -> searchInternalDocs(query),
                () -> searchCodebase(query)
        ));
    }

    public List<SearchResult> searchAll(String query) throws Exception {
        log.info("[ParallelMcpSearch] Parallel search for: {}", query);
        return Scopes.all(List.of(
                () -> searchBrave(query),
                () -> searchInternalDocs(query),
                () -> searchCodebase(query)
        ));
    }

    private SearchResult searchBrave(String query) throws InterruptedException {
        Thread.sleep(100);
        return new SearchResult("brave", "Brave result for: " + query, 0.9);
    }

    private SearchResult searchInternalDocs(String query) throws InterruptedException {
        Thread.sleep(150);
        return new SearchResult("internal-docs", "Internal docs for: " + query, 0.85);
    }

    private SearchResult searchCodebase(String query) throws InterruptedException {
        Thread.sleep(200);
        return new SearchResult("codebase", "Codebase result for: " + query, 0.75);
    }
}

package ai.gameclaw.concurrency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.StructuredTaskScope;
import java.util.stream.Stream;

public final class Scopes {

    private static final Logger log = LoggerFactory.getLogger(Scopes.class);

    private Scopes() {}

    public static <T> T race(List<Callable<T>> tasks) throws Exception {
        StructuredTaskScope.Joiner<T, T> joiner = StructuredTaskScope.Joiner.anySuccessfulResultOrThrow();
        try (StructuredTaskScope<T, T> scope = StructuredTaskScope.open(joiner)) {
            for (Callable<T> task : tasks) {
                scope.fork(task::call);
            }
            return scope.join();
        }
    }

    public static <T> List<T> all(List<Callable<T>> tasks) throws Exception {
        StructuredTaskScope.Joiner<T, Stream<StructuredTaskScope.Subtask<T>>> joiner =
                StructuredTaskScope.Joiner.allSuccessfulOrThrow();
        try (StructuredTaskScope<T, Stream<StructuredTaskScope.Subtask<T>>> scope = StructuredTaskScope.open(joiner)) {
            for (Callable<T> task : tasks) {
                scope.fork(task::call);
            }
            Stream<StructuredTaskScope.Subtask<T>> subtaskStream = scope.join();
            List<T> results = new ArrayList<>();
            subtaskStream.forEach(subtask -> results.add(subtask.get()));
            return results;
        }
    }
}

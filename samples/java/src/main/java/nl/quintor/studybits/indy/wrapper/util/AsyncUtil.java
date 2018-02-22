package nl.quintor.studybits.indy.wrapper.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class AsyncUtil {
    public static <T, R> Function<T, R> wrapException(ExceptionalFunction<T, R> exceptionalFunction) {
        return (T t) -> {
            try {
                return exceptionalFunction.apply(t);
            }
            catch (Exception e) {
                throw new CompletionException(e);
            }
        };
    }

    public static <T> Supplier<T> wrapException(ExceptionalSupplier<T> exceptionalSupplier) {
        return () -> {
            try {
                return exceptionalSupplier.supply();
            }
            catch (Exception e) {
                throw new CompletionException(e);
            }
        };
    }


    public interface ExceptionalFunction<T, R> {
        public R apply(T t) throws Exception;
    }

    public interface ExceptionalSupplier<T> {
        public T supply() throws Exception;
    }
}

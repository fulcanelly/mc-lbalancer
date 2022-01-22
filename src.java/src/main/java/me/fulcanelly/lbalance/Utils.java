package me.fulcanelly.lbalance;

import java.util.concurrent.BlockingQueue;
import java.util.function.Function;
import java.util.function.Supplier;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;




interface UnsafeSupplier<E extends Exception, T> {
    T apply() throws E;

    default Supplier<T> toSupplier() {
        return new Supplier<T>() {

            @Override @SneakyThrows
            public T get() {
                return apply();
            }
            
        };
    }
}

@UtilityClass
public class Utils {
    @SneakyThrows
    void sleep(long pause) {
        Thread.sleep(pause);
    }

    @SneakyThrows
    void putOne(BlockingQueue<? super Object> queue) {
        queue.put(1);
    }

    <E extends Exception, T, K> Function<T, K> const_(UnsafeSupplier<E, K> supp) {
        return ___ -> supp.toSupplier().get();
    }
}
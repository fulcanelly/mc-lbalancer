package me.fulcanelly.lbalance;

import java.util.concurrent.BlockingQueue;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

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

}
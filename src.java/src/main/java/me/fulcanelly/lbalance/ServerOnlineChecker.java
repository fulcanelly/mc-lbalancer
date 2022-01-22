package me.fulcanelly.lbalance;



import java.util.ArrayList;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.IntStream;

import com.google.common.base.Function;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.ServerPing;

import static me.fulcanelly.lbalance.Utils.*;

@RequiredArgsConstructor
public class ServerOnlineChecker {
    
    final ProxyServer server;

    BlockingQueue<Boolean> queue = new LinkedBlockingQueue<>();

    void acceptPing(ServerPing ping, Throwable t) {
        queue.add(ping != null);
    }

    @SneakyThrows
    boolean take() {
        return queue.take();
    }

    @SneakyThrows
    boolean check() {
        var sinfos = server.getServers();

        sinfos.values()
            .forEach(s -> s.ping(this::acceptPing));

        return IntStream.range(0, sinfos.size())
            .boxed()
            .map(const_(this::take))
            .reduce(true, (a, b) -> a && b);
    }
}

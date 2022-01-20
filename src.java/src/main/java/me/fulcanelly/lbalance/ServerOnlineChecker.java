package me.fulcanelly.lbalance;



import java.util.ArrayList;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.ServerPing;

@RequiredArgsConstructor
public class ServerOnlineChecker {
    
    final ProxyServer server;

    BlockingQueue<Integer> queue = new LinkedBlockingQueue<>();

    void acceptPing(ServerPing ping, Throwable t) {
        queue.add(ping != null ? 1 : 0);
    }

    @SneakyThrows
    boolean check() {
        var sinfos = server.getServers();

        sinfos.values()
            .forEach(s -> s.ping(this::acceptPing));
       
        
        var list = new ArrayList<Integer>();

        for (int i = 0; i < sinfos.size(); i ++ ) {
            list.add(queue.take()); 
        }

        return 1 == list.stream()
            .reduce(1, (a, b) -> a * b);
    }
}

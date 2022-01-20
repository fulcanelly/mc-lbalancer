package me.fulcanelly.lbalance;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import com.google.common.base.Supplier;
import com.google.gson.annotations.Until;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

class SmartIO {
    
    PrintWriter out;
    BufferedReader in;

    public SmartIO(Socket sock) {
        setup(sock);
    }
    
    @SneakyThrows
    public void setup(Socket socket) {
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(
            new InputStreamReader(socket.getInputStream()
        ));
    }

    public void println(String str) {
        out.println(str);
    }

    @SneakyThrows
    public String gets() {
        return in.readLine();
    }

    @SneakyThrows
    public void close() {
        in.close();
        out.close();
    }

}

@RequiredArgsConstructor
class ServerOnlineChecker {
    
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


@UtilityClass
class Utils {
    @SneakyThrows
    void sleep(long pause) {
        Thread.sleep(pause);
    }

    @SneakyThrows
    void putOne(BlockingQueue<? super Object> queue) {
        queue.put(1);
    }

}

@RequiredArgsConstructor
class Server implements Listener {
   
    final ServerSocket server; 
    final ProxyServer proxy;

    @SneakyThrows
    public Socket accept() {
        return server.accept(); 
    }

    @SneakyThrows
    void setSoTimeout(Socket socket) {
//        socket.setSoTimeout(1_000_000);
    }

    @SneakyThrows    
    public void start() {
        Stream.generate(this::accept)
            .peek(this::setSoTimeout)
            .map(conn -> new Thread(() -> dispatch(conn)))
            .forEach(Thread::start);
    }

    String getOnline() {
        return String.valueOf(proxy.getOnlineCount());
    }

    String isRunning() {
        var res = new ServerOnlineChecker(proxy)
            .check();    
        return String.valueOf(res);
    }

    final Map<String, Supplier<String>> map = 
        Map.of(
            "get_online", this::getOnline,
            "is_running", this::isRunning
        );
    
    final List<BlockingQueue<Object>> conns = new LinkedList<>();

    void dispatch(Socket socket) {
        var sio = new SmartIO(socket);
        var queue = new ArrayBlockingQueue<>(1);
        var alive = new AtomicBoolean(true);
   
        conns.add(queue);
        
        new Thread(() -> {
            while (alive.get()) {
                Utils.sleep(2000);
                Utils.putOne(queue);
            }
        }).start();
        
        var start = System.currentTimeMillis();

        try {
            while (true) {
                sio.println(
                    map.getOrDefault(sio.gets(), () -> "null").get()
                );
                queue.take();
                var upd = System.currentTimeMillis();
                System.out.println(upd - start);
                start = upd;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            conns.remove(queue);
            alive.set(false);
        }
    }
    
    
    @EventHandler
    public void onPreLogin(PreLoginEvent event) {
        conns.parallelStream()
            .forEach(Utils::putOne);
    }

    Server setListener(Plugin plugin) {
        proxy.getPluginManager()
            .registerListener(plugin, this);
        return this;
    }

}

public class LoadBalancer extends Plugin implements Listener {    
    
    @Override @SneakyThrows
    public void onEnable() {
        var server = new Server(
                new ServerSocket(1344), this.getProxy());

        new Thread(server::start).start();

        this.getProxy().getPluginManager()
            .registerListener(this, this);

        this.getLogger().info("kinda balancing");
    }
}


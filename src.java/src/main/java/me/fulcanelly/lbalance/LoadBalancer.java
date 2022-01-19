package me.fulcanelly.lbalance;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import com.google.common.base.Supplier;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.config.ServerInfo;
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

@RequiredArgsConstructor
class Server {
   
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

    @SneakyThrows
    void sleep(long pause) {
        Thread.sleep(pause);
    }

    final Map<String, Supplier<String>> map = 
        Map.of(
            "get_online", this::getOnline,
            "is_running", this::isRunning
        );
    

    void dispatch(Socket socket) {
        var sio = new SmartIO(socket);
        var queue = new LinkedBlockingQueue<>();
        var semaph = new Semaphore(1);

        var alive = new AtomicBoolean(true);
                 
        new Thread(new Runnable() {
            @SneakyThrows
            public void run() {
                while (alive.get()) {
                    semaph.acquire();
                    sleep(2000);
                    semaph.release();           
                }
            }
        }).start();
        
        var start = System.currentTimeMillis();

        try {
            while (true) {
                semaph.release();
                sio.println(
                    map.getOrDefault(sio.gets(), () -> "null").get()
                );
                
                semaph.acquire();
                var upd = System.currentTimeMillis();
                System.out.println(upd - start);
                start = upd;
            }
        } catch(Exception e) {
            e.printStackTrace();
            alive.set(false);
        }
    }
}

public class LoadBalancer extends Plugin implements Listener {
   

    void ping(ServerPing ping, Throwable t) {
        System.out.println("== got ping");
        System.out.println(ping);
        System.out.println(t);
    }
    
    @SneakyThrows
    void start() {
        new Server(new ServerSocket(1344), this.getProxy())
            .start();;
    }

    @Override @SneakyThrows
    public void onEnable() {
        new Thread(this::start).start();
        this.getLogger().info("kinda balancing");
    }
}


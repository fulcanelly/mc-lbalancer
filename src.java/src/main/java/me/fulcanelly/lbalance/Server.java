package me.fulcanelly.lbalance;



import java.net.ServerSocket;
import java.net.Socket;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import com.google.common.base.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import net.md_5.bungee.api.ProxyServer;

import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;



@RequiredArgsConstructor
public class Server implements Listener {
   
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
                    switch (sio.gets()) {
                        case "get_online" -> getOnline();
                        case "is_running" -> isRunning();
                        default -> "null";
                    }
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

    public Server setListener(Plugin plugin) {
        proxy.getPluginManager()
            .registerListener(plugin, this);
        return this;
    }

}

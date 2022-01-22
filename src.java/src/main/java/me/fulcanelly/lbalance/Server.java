package me.fulcanelly.lbalance;



import java.net.ServerSocket;
import java.net.Socket;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import com.google.common.base.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import me.fulcanelly.lbalance.config.Config;
import net.md_5.bungee.api.ProxyServer;

import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;



@RequiredArgsConstructor
public class Server implements Listener {
   
    final ServerSocket server; 
    final ProxyServer proxy;
    final Config config;

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
            .<Runnable>map(sock -> () -> runWith(sock))
            .map(Thread::new)
            .forEach(Thread::start);
    }

    int getOnline() {
        return proxy.getOnlineCount();
    }

    boolean isRunning() {
        return new ServerOnlineChecker(proxy)
            .checkExcluding(config.getLobby());    
    }

    final BlockingQueue<Semaphore> conns = new LinkedBlockingQueue<>();

    Object dispatch(String input) {
        return switch (input) {
            case "get_online" -> getOnline();
            case "is_running" -> isRunning();
            default -> "null";
        };
    }


    @SneakyThrows
    void runWith(Socket socket) {
        var sio = new SmartIO(socket);
        var alive = new AtomicBoolean(true);
        var sem = new Semaphore(0);

        conns.put(sem);
        
        new Thread(() -> {
            while (alive.get()) {
                Utils.sleep(2000);
                sem.release();
            }
        }).start();
        
        var start = System.currentTimeMillis();

        try {
            while (true) {
                sio.println(
                    dispatch(sio.gets()).toString()
                );
                sem.acquire();

                var upd = System.currentTimeMillis();
                System.out.println(upd - start);
                start = upd;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            conns.remove(sem);
            alive.set(false);
        }
    }
    
    
    @EventHandler
    public void onPreLogin(PreLoginEvent event) {
        conns.parallelStream()
            .forEach(Semaphore::release);
    }

    public Server setListener(Plugin plugin) {
        proxy.getPluginManager()
            .registerListener(plugin, this);
        return this;
    }

}

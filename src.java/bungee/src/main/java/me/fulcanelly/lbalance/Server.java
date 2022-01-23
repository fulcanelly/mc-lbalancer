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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import com.google.common.base.Supplier;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import me.fulcanelly.lbalance.config.Config;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.event.EventHandler;


@Builder
@RequiredArgsConstructor
public class Server implements Listener {
   
    final ServerSocket server; 
    final ProxyServer proxy;
    final Config config;
    final Plugin plugin;

    @EventHandler
    public void onLogin(PostLoginEvent event) {

        AtomicReference<ScheduledTask> task = new AtomicReference<>();
        var player = event.getPlayer();

        // Object task = 1;
        task.set(
            proxy.getScheduler()
                .schedule(plugin, () -> {
                    if (isRunning() || !player.isConnected()) {
                        task.get().cancel();
                    } else {
                        player.sendMessage(
                                 ChatColor.GREEN + "Server is starting. wait..."
                                );
                    }
                }, 0L, 3L, TimeUnit.SECONDS)
            );
    }
    
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
                sem.release();
                Utils.sleep(config.getInterval());
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

    public Server setListener() {
        proxy.getPluginManager()
            .registerListener(plugin, this);
        return this;
    }

}

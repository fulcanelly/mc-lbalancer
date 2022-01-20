package me.fulcanelly.lbalance;


import java.net.ServerSocket;
import lombok.SneakyThrows;
import net.md_5.bungee.api.plugin.Plugin;


public class LoadBalancer extends Plugin {    
    
    @Override @SneakyThrows
    public void onEnable() {
        var ssocket = new ServerSocket(1344);
        
        new Thread(
            new Server(ssocket, this.getProxy())
                .setListener(this)
                ::start).start();
        
        this.getLogger().info("kinda balancing");
    }
}


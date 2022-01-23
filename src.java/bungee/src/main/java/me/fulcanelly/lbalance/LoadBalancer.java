package me.fulcanelly.lbalance;


import java.net.ServerSocket;

import lombok.SneakyThrows;
import me.fulcanelly.lbalance.config.Config;
import me.fulcanelly.lbalance.config.ConfigLoader;
import net.md_5.bungee.api.plugin.Plugin;


public class LoadBalancer extends Plugin {    
    
    ServerSocket ssocket;

    @Override @SneakyThrows
    public void onEnable() {
        ssocket = new ServerSocket(1344);
        var configLoader = new ConfigLoader<Config>(this, Config.class);
        new Thread(
            Server.builder()
                    .server(ssocket)
                    .plugin(this)
                    .config(configLoader.load())
                    .proxy(this.getProxy())
                .build()
            .setListener()
               ::start).start();
           
        this.getLogger().info("kinda balancing");
    }

    @Override @SneakyThrows
    public void onDisable() {
        ssocket.close(); 
    }
}

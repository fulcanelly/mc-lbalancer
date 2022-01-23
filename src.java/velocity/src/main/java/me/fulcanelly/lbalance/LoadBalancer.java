package me.fulcanelly.lbalance;

import com.google.inject.Inject;
import com.velocitypowered.api.event.EventHandler;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.util.title.Title;

import org.slf4j.Logger;

import lombok.AllArgsConstructor;

@AllArgsConstructor(onConstructor = @__(@Inject))
@Plugin(id = "loadbalancer", name = "LoadBalancer", version = "1.0.0",
        description = "Balances load", authors = {"fulcanelly"})
public class LoadBalancer {    
    
    
    Logger logger;
    ProxyServer server;

    @Subscribe
    void onPreConn(ServerPreConnectEvent event) {
    }

   
    @Subscribe
    void onInit(ProxyInitializeEvent event) {
    }


}

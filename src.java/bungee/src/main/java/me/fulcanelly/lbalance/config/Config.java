package me.fulcanelly.lbalance.config;

import lombok.Data;
import lombok.ToString;

@ToString @Data
public class Config {
    
    private String lobby = "lobby";
    private int interval = 2000;
    private boolean verbose = false;

}

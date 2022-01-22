package me.fulcanelly.lbalance.config;

import java.io.FileInputStream;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import net.md_5.bungee.api.plugin.Plugin;


@AllArgsConstructor
public class ConfigLoader<T> {
    
    Plugin plugin;
    Class<T> clazz;


    @SneakyThrows
    T load() {
        var folder = plugin.getDataFolder();
        folder.mkdir();

        var cfile = folder.toPath()
            .resolve("config.yml")
            .toFile();
          
        return switch (cfile.exists() ? Existence.Present : Existence.None) {
            case Present -> new Yaml(new Constructor(clazz))
                .load(new FileInputStream(cfile));
            default -> clazz.getConstructor()
                .newInstance();
        };    
    } 

}


enum Existence {
    Present,
    None
}
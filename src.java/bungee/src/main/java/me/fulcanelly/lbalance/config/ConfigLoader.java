package me.fulcanelly.lbalance.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

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
    void saveResource() {
        var is = plugin.getResourceAsStream("config.yml");
        
        var fout = new FileOutputStream(getConfigFile());
        byte []sss;
        fout.write(sss = is.readAllBytes());
        System.out.println(sss);
        fout.close();
    }


    File getConfigFile() {
        var folder = plugin.getDataFolder();
        folder.mkdir();

        return folder.toPath()
            .resolve("config.yml")
            .toFile(); 
    }

    @SneakyThrows
    T readYaml() {
        var yaml = new Yaml(new Constructor(clazz));
        
        return yaml.load(
            new FileInputStream(getConfigFile()));
    }

    T saveAndRead() {
        saveResource();
        return readYaml();
    }

    @SneakyThrows
    public T load() {
        var cfile = getConfigFile();

        return switch (cfile.exists() ? Existence.Present : Existence.None) {
            case Present -> new Yaml(new Constructor(clazz))
                .load(new FileInputStream(cfile));
            default -> saveAndRead();
        };    
    } 
}


enum Existence {
    Present,
    None
}

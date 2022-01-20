package me.fulcanelly.lbalance;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import lombok.SneakyThrows;


public class SmartIO {
    
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
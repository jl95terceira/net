package jl95.net;

import jl95.lang.variadic.Method1;
import jl95.util.UFuture;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.CompletableFuture;

import static jl95.lang.SuperPowers.*;

public class Util {
    public static ServerSocket    getSimpleServerSocket  (InetSocketAddress addr,
                                                          Integer acceptTimeoutMs) {
        return uncheck(() -> {
            var socket = new ServerSocket();
            socket.bind(addr);
            socket.setSoTimeout(acceptTimeoutMs);
            return socket;
        });
    }
    public static ServerSocket    getSimpleServerSocket   (InetSocketAddress addr) {
        return getSimpleServerSocket(addr, Server.Defaults.soTimeoutMs);
    }
    public static UFuture<Socket> getSocketByAcceptFuture (InetSocketAddress addr) {

        return uncheck(() -> {
            final var serversock = new ServerSocket();
            serversock.bind(addr);
            final var socketFuture = new CompletableFuture<Socket>();
            new Thread(unchecked(() -> {
                final Socket sock = serversock.accept();
                serversock.close();
                socketFuture.complete(sock);
            })::accept).start();
            return UFuture.of(socketFuture);
        });
    }
    public static Socket          getSocketByAccept       (InetSocketAddress addr) {

        return getSocketByAcceptFuture(addr).get();
    }
    public static UFuture<Socket> getSocketByConnectFuture(InetSocketAddress serverAddr, Method1<Exception> connectExceptionHandler) {
        final var future = new CompletableFuture<Socket>();
        new Thread(unchecked(() -> {
            future.complete(getSocketByConnect(serverAddr, connectExceptionHandler));
        })::accept).start();
        return UFuture.of(future);
    }
    public static UFuture<Socket> getSocketByConnectFuture(InetSocketAddress serverAddr) {
        return getSocketByConnectFuture(serverAddr, ex -> sleep(500));
    }
    public static Socket          getSocketByConnect      (InetSocketAddress serverAddr, Method1<Exception> connectExceptionHandler) throws IOException {
        while (true) {
            final var socket = new Socket();
            try {
                socket.connect(serverAddr);
                return socket;
            } catch (SocketException ex) {
                connectExceptionHandler.accept(ex);
            }
        }
    }
    public static Socket          getSocketByConnect      (InetSocketAddress serverAddr) throws IOException {
        return getSocketByConnect(serverAddr, ex -> sleep(500));
    }
}

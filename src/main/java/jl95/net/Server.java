package jl95.net;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import jl95.lang.*;
import jl95.lang.variadic.*;
import jl95.util.VoidAwaitable;

import static jl95.lang.SuperPowers.*;

/**
 * Simple server
 */
public class Server {

    public static class Defaults {
        public static final int soTimeoutMs = 5000;
        private Defaults() {}
    }

    private final ServerSocket               serverSocket;
    private       Method1<Method0>           acceptRunner = Method0::accept;
    private       Method2<Server, Socket>    acceptCb;
    private       Method2<Server, Exception> acceptErrorCb;
    private       Method1<Server>            acceptTimeoutCb;
    private       Boolean                    isRunning = false;
    private       Boolean                    toStop    = false;
    private       CompletableFuture<Void>    startFuture;
    private       CompletableFuture<Void>    stopFuture;

    private Server(ServerSocket socket) {
        this.serverSocket = socket;
    }

    /**
     * create from server socket
     * @param socket server socket for accepting connections
     */
    public static Server fromSocket(ServerSocket socket) {
        return new Server(socket);
    }
    /**
     * create from address (to which to bind the server socket to be created) and connection acceptance timeout ("SO_TIMEOUT")
     * @param address server socket address
     */
    public static Server fromAddressAndTimeout(SocketAddress address, int timeout) throws IOException {
        return new Server(exfunction(() -> {
            var sock = new ServerSocket();
            sock.bind(address);
            sock.setSoTimeout(timeout);
            return sock;
        }).apply());
    }
    /**
     * create from address (to which to bind the server socket to be created)
     * @param address server socket address
     */
    public static Server fromAddress(SocketAddress address) throws IOException {
        return fromAddressAndTimeout(address, Defaults.soTimeoutMs);
    }

    /**
     * set acceptance mode as synchronised
     */
    public final void acceptSynced        () {
        acceptRunner = Method0::accept;
    }
    /**
     * set acceptance mode as asynchronised - create and start a new thread on each connection request
     */
    public final void acceptAsyncNewThread() {
        acceptRunner = f -> new Thread(f::accept).start();
    }
    /**
     * set acceptance mode as asynchronised - submit each connection request to a pre-allocated pool of threads
     * @param threadsNr number of threads to be pre-allocated
     */
    public final void acceptAsyncPool     (Integer threadsNr) {

        var pool = new ScheduledThreadPoolExecutor(threadsNr);
        acceptRunner = f -> pool.execute(f::accept);
    }
    /**
     * set connection acceptance callback to given
     * @param cb callback
     */
    public final void setAcceptCb       (Method2<Server, Socket>    cb) {
        acceptCb        = cb;
    }
    /**
     * set connection acceptance error callback to given
     * @param cb callback
     */
    public final void setAcceptErrorCb  (Method2<Server, Exception> cb) {
        acceptErrorCb   = cb;
    }
    /**
     * set connection acceptance timeout callback to given
     * @param cb callback
     */
    public final void setAcceptTimeoutCb(Method1<Server>            cb) {
        acceptTimeoutCb = cb;
    }
    /**
     * start listening for incoming connections
     * @return promise of server already started
     */
    synchronized public final VoidAwaitable start    () {

        if (isRunning()) throw new IllegalStateException();
        toStop      = false;
        startFuture = new CompletableFuture<>();
        stopFuture  = new CompletableFuture<>();
        new Thread(() -> {
            startFuture.complete(null);
            while (!toStop) {
                try {
                    java.net.Socket socket;
                    try {
                        socket = serverSocket.accept();
                    }
                    catch (java.net.SocketTimeoutException ex) /* not really an error - just to give control back to the thread every so often */ {
                        ifNull(acceptTimeoutCb, (self) -> {}).accept(this);
                        continue;
                    }
                    catch (Exception ex) {
                        ifNull(acceptErrorCb, (self, ex_) -> { System.out.printf("Error on accept: %s\n", ex_); }).accept(this, ex);
                        continue;
                    }
                    acceptRunner.accept(() -> ifNull(acceptCb, (self, socket_) -> {}).accept(this, socket));
                }
                catch (Exception ex) /* happened in non-final (overridable) methods */ {
                    ex.printStackTrace();
                }
            }
            stopFuture.complete(null);
            isRunning = false;
        }).start();
        isRunning = true;
        return VoidAwaitable.of(startFuture);
    }
    /**
     * stop listening for incoming connections
     * @return promise of server already stopped
     */
    synchronized public final VoidAwaitable   stop     () {

        if (!isRunning()) throw new IllegalStateException();
        if (stopFuture == null) throw new AssertionError();
        toStop = true;
        return VoidAwaitable.of(stopFuture);
    }
    /**
     * @return whether server is running i.e. listening for connections
     */
    synchronized public final Boolean         isRunning() { return isRunning; }
    /**
     * @return internal server socket
     */
    synchronized public final ServerSocket    getSocket() { return serverSocket; }
    /**
     * stop the server (if not stopped already) and close the internal server socket
     */
    public void close() {
        if (isRunning()) {
            stop().await();
        }
        uncheck(getSocket()::close);
    }
}

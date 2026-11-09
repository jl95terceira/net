package jl95.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;

import org.junit.After;
import org.junit.Before;

import jl95.util.UFuture;

public class ServerTest extends Test {

    Server server;

    @Before
    public void setUp() throws IOException {
        server = Server.fromAddress(new InetSocketAddress("127.0.0.1", 4242));
        final CompletableFuture<Socket> serverSideSocketFuture = new CompletableFuture<>();
        server.setAcceptCb((self, serverSideSocket) -> {
            System.out.println("Connected");
            serverSideSocketFuture.complete(serverSideSocket);
        });
        server.start();
        clientSideSocket = Util.getSocketByConnectFuture(new InetSocketAddress("127.0.0.1", 4242))
                .get();
        serverSideSocket = UFuture.of(serverSideSocketFuture).get();
    }

    @After
    public void tearDown() {
        server.close();
    }
}

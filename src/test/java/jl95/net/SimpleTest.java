package jl95.net;

import java.net.InetSocketAddress;
import java.net.Socket;

import org.junit.Before;

import jl95.util.UFuture;

public class SimpleTest extends Test {

    @Before
    public void setUp() {
        final UFuture<Socket> serverSideSocketFuture = Util.getSocketByAcceptFuture(new InetSocketAddress("127.0.0.1", 4242));
        clientSideSocket = Util.getSocketByConnectFuture(new InetSocketAddress("127.0.0.1", 4242))
                .get();
        serverSideSocket = serverSideSocketFuture.get();
    }
}

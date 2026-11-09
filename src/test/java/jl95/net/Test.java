package jl95.net;

import static jl95.lang.SuperPowers.I;

import java.io.IOException;
import java.net.Socket;

import org.junit.After;
import org.junit.Assert;

public abstract class Test {

    protected Socket clientSideSocket;
    protected Socket serverSideSocket;

    @After
    public void close() throws IOException {
        for (final Socket s: I(clientSideSocket, serverSideSocket)) {
            if (s != null) {
                s.close();
            }
        }
    }

    @org.junit.Test
    public void test() throws IOException {
        System.out.println("Server -> Client");
        final byte[] msg1 = "Hello, World".getBytes();
        serverSideSocket.getOutputStream().write(msg1);
        serverSideSocket.getOutputStream().flush();
        Assert.assertArrayEquals(msg1, clientSideSocket.getInputStream().readNBytes(msg1.length));
        System.out.println("Client -> Server");
        final byte[] msg2 = "Goodbye, Stranger".getBytes();
        clientSideSocket.getOutputStream().write(msg2);
        clientSideSocket.getOutputStream().flush();
        Assert.assertArrayEquals(msg2, serverSideSocket.getInputStream().readNBytes(msg2.length));
        // done
        clientSideSocket.close();
        serverSideSocket.close();
    }
}

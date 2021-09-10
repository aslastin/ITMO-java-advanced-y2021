package info.kgeorgiy.ja.slastin.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Set;

import static info.kgeorgiy.ja.slastin.hello.HelloUDPClient.*;
import static info.kgeorgiy.ja.slastin.hello.Utils.*;

public class HelloUDPNonblockingClient implements HelloClient {
    @Override
    public void run(final String host, final int port, final String prefix, final int threads, final int requests) {
        checkRunArgs(host, port, prefix, threads, requests);
        final SocketAddress serverAddress = getSocketAddress(host, port);
        final Selector selector = getSelector();
        ByteBuffer buffer = null;
        for (int i = 0; i < threads; i++) {
            final DatagramChannel channel;
            try {
                channel = getNonblockingChannel();
                channel.connect(serverAddress);
                channel.register(selector, SelectionKey.OP_WRITE, new int[]{i, 0});
            } catch (final IOException e) {
                throw new RuntimeException("I/O error during setting up datagram channel", e);
            }
            if (i == 0) {
                buffer = getReceiveBuffer(channel);
            }
        }
        run(selector, buffer, prefix, requests);
    }

    private static void run(final Selector selector, final ByteBuffer buffer, final String prefix, final int requests) {
        while (!Thread.interrupted() && !selector.keys().isEmpty()) {
            try {
                selector.select(RESPONSE_TIMEOUT);
            } catch (IOException e) {
                logError("select", e);
            }
            final Set<SelectionKey> selectedKeys = selector.selectedKeys();
            if (!selectedKeys.isEmpty()) {
                for (final var iterator = selectedKeys.iterator(); iterator.hasNext();) {
                    final SelectionKey key = iterator.next();
                    if (key.isWritable()) {
                        sendRequest(key, buffer, prefix);
                    } else if (key.isReadable()) {
                        receiveResponse(key, buffer, requests);
                    }
                    iterator.remove();
                }
            } else {
                selector.keys().forEach(key -> {
                    if (key.isWritable()) {
                        sendRequest(key, buffer, prefix);
                    }
                });
            }
        }
    }

    private static void sendRequest(final SelectionKey key,  final ByteBuffer buffer, final String prefix) {
        final DatagramChannel channel = (DatagramChannel) key.channel();
        final int[] att = (int[]) key.attachment();
        buffer.clear();
        Utils.setMessage(buffer, prefix, att[0], att[1]);
        String request = getMessage(buffer);
        logRequest(request);
        try {
            channel.write(buffer);
        } catch (IOException e) {
            logError("During writing such request: " + request, e);
        }
        key.interestOps(SelectionKey.OP_READ);
    }

    private static void receiveResponse(final SelectionKey key, final ByteBuffer buffer, final int requests) {
        final DatagramChannel channel = (DatagramChannel) key.channel();
        final int[] att = (int[]) key.attachment();
        buffer.clear();
        try {
            channel.read(buffer);
        } catch (final IOException e) {
            logError("During reading response", e);
        }
        buffer.flip();
        String response = getMessage(buffer);
        if (isResponseValid(response, att[0], att[1])) {
            logResponse(response);
            ++att[1];
        }
        if (att[1] < requests) {
            key.interestOps(SelectionKey.OP_WRITE);
        } else {
            try {
                channel.close();
            } catch (final IOException e) {
                logError("During closing channel", e);
            }
        }
    }

    public static void main(String[] args) {
        runClient(args, new HelloUDPNonblockingClient());
    }
}

package info.kgeorgiy.ja.slastin.hello;

import java.io.Closeable;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class Utils {
    static SocketAddress getSocketAddress(final String host, final int port) {
        try {
            return new InetSocketAddress(InetAddress.getByName(host), port);
        } catch (final UnknownHostException e) {
            throw new RuntimeException("Can not find host " + host, e);
        }
    }

    static Selector getSelector() {
        try {
            return Selector.open();
        } catch (final IOException e) {
            throw new RuntimeException("I/O error occurred during opening selector", e);
        }
    }

    static DatagramChannel getNonblockingChannel() {
        try {
            final DatagramChannel channel = DatagramChannel.open();
            channel.configureBlocking(false);
            channel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            return channel;
        } catch (final IOException e) {
            throw new RuntimeException("Can not create non blocking datagram channel", e);
        }
    }

    static ByteBuffer getReceiveBuffer(final DatagramChannel datagramChannel) {
        try {
            return ByteBuffer.allocate(datagramChannel.socket().getReceiveBufferSize());
        } catch (final SocketException e) {
            throw new RuntimeException("SocketException during creating receive byte buffer", e);
        }
    }

    static DatagramPacket getResponsePacket(final DatagramSocket socket) throws SocketException {
        return new DatagramPacket(new byte[socket.getReceiveBufferSize()], socket.getReceiveBufferSize());
    }

    static DatagramPacket getRequestPacket(final SocketAddress address) {
        return new DatagramPacket(new byte[0], 0, address);
    }

    static void setMessage(final DatagramPacket packet, final String message) {
        packet.setData(message.getBytes(StandardCharsets.UTF_8));
    }

    static void setMessage(final ByteBuffer buffer, final String prefix, final int threadNumber, final int requestNumber) {
        setMessage(buffer, prefix + threadNumber + "_" + requestNumber);
    }

    static void setMessage(final ByteBuffer buffer, final String message) {
        buffer.put(message.getBytes(StandardCharsets.UTF_8));
        buffer.flip();
    }

    static String getMessage(final DatagramPacket packet) {
        return new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
    }

    static String getMessage(final ByteBuffer buffer) {
        String message = StandardCharsets.UTF_8.decode(buffer).toString();
        buffer.flip();
        return message;
    }

    static void shutdownAndAwaitTermination(final ExecutorService service, final int timeoutMin, final TimeUnit unit) {
        if (service == null) {
            return;
        }
        service.shutdown();
        try {
            if (!service.awaitTermination(timeoutMin, unit)) {
                service.shutdownNow();
                if (!service.awaitTermination(timeoutMin, unit)) {
                    System.err.println("Can not terminate all threads");
                }
            }
        } catch (final InterruptedException ie) {
            service.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    static void close(final Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException e) {
            Utils.logError("close", e);
        }
    }

    static void logError(final String message, final Exception e) {
        logError(e.getClass().getSimpleName() + " - " + message + " - " + e.getMessage());
    }

    static void logError(final String message) {
        System.err.println(message);
    }

    static void logInfo(final String message) {
        System.out.println(message);
    }
}

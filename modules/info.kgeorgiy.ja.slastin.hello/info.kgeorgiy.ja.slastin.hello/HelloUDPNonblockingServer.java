package info.kgeorgiy.ja.slastin.hello;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static info.kgeorgiy.ja.slastin.hello.Utils.logError;

public class HelloUDPNonblockingServer extends HelloUDPServer {
    private Selector selector;
    private DatagramChannel channel;
    private ExecutorService listener;
    private ExecutorService executors;
    private Queue<ResponseInfo> responseInfos;
    private volatile boolean isClosed = true;

    public static void main(String[] args) {
        runServer(args, new HelloUDPNonblockingServer());
    }

    @Override
    public void start(final int port, final int threads) {
        checkStartArgs(port, threads);

        selector = Utils.getSelector();
        try {
            channel = Utils.getNonblockingChannel();
            channel.bind(new InetSocketAddress(port));
            channel.register(selector, SelectionKey.OP_READ);
        } catch (IOException e) {
            throw new RuntimeException("Can not setup datagram channel", e);
        }
        listener = Executors.newSingleThreadExecutor();
        executors = HelloUDPServer.getThreadPoolExecutorWithDiscardPolicy(threads);
        responseInfos = new LinkedBlockingQueue<>();
        isClosed = false;

        listener.execute(() -> listen(Utils.getReceiveBuffer(channel)));
    }

    public void listen(final ByteBuffer receiveBuffer) {
        while (!Thread.interrupted() && !isClosed) {
            try {
                selector.select(key -> {
                    if (key.isReadable()) {
                        receiveRequest(key, receiveBuffer);
                    } else if (key.isWritable() && !responseInfos.isEmpty()) {
                        sendResponse(key);
                    }
                });
            } catch (final IOException e) {
                logError("select", e);
            } catch (final ClosedSelectorException ignored) {
            }
        }
    }

    public void receiveRequest(final SelectionKey key, final ByteBuffer receiveBuffer) {
        receiveBuffer.clear();
        final SocketAddress clientAddress;
        try {
            clientAddress = channel.receive(receiveBuffer);
        } catch (IOException e) {
            throw new RuntimeException("Can not receive data from channel", e);
        }
        receiveBuffer.flip();
        String request = Utils.getMessage(receiveBuffer);
        Runnable task = () -> {
            ByteBuffer buffer = ByteBuffer.wrap(("Hello, " + request).getBytes(StandardCharsets.UTF_8));
            responseInfos.add(new ResponseInfo(buffer, clientAddress));
            key.interestOpsOr(SelectionKey.OP_WRITE);
        };
        if (executors != null) {
            executors.execute(task);
        } else {
            task.run();
        }
    }

    public void sendResponse(final SelectionKey key) {
        final ResponseInfo responseInfo = responseInfos.remove();
        try {
            channel.send(responseInfo.getMessageBuffer(), responseInfo.getClientAddress());
        } catch (IOException e) {
            throw new RuntimeException("I/O error during sending response to client", e);
        }
    }

    @Override
    public void close() {
        if (isClosed) {
            return;
        }
        isClosed = true;
        Utils.close(selector);
        Utils.close(channel);
        Utils.shutdownAndAwaitTermination(listener, 3, TimeUnit.SECONDS);
        Utils.shutdownAndAwaitTermination(executors, 5, TimeUnit.SECONDS);
    }

    private static class ResponseInfo {
        private final ByteBuffer messageBuffer;
        private final SocketAddress clientAddress;

        public ResponseInfo(ByteBuffer messageBuffer, SocketAddress clientAddress) {
            this.messageBuffer = messageBuffer;
            this.clientAddress = clientAddress;
        }

        public ByteBuffer getMessageBuffer() {
            return messageBuffer;
        }

        public SocketAddress getClientAddress() {
            return clientAddress;
        }
    }
}

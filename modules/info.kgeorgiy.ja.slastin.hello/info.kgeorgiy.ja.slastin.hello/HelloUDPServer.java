package info.kgeorgiy.ja.slastin.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.*;

import static info.kgeorgiy.ja.slastin.hello.HelloUDPClient.throwBadArg;
import static info.kgeorgiy.ja.slastin.hello.Utils.*;

public class HelloUDPServer implements HelloServer {
    final static int MAX_POOL_SIZE = 1000;

    private DatagramSocket socket;
    private ExecutorService listener;
    private ExecutorService executors;
    private volatile boolean isClosed = true;

    static void checkStartArgs(final int port, final int threads) {
        if (port < 0) {
            throwBadArg("port", ">= 0");
        }
        if (threads <= 0) {
            throwBadArg("threads", "> 0");
        }
    }

    static ThreadPoolExecutor getThreadPoolExecutorWithDiscardPolicy(final int threads) {
        if (threads <= 1) {
            return null;
        }
        return new ThreadPoolExecutor(1, threads - 1, 1, TimeUnit.MINUTES,
                new ArrayBlockingQueue<>(MAX_POOL_SIZE), new ThreadPoolExecutor.DiscardPolicy());
    }

    @Override
    public void start(final int port, final int threads) {
        checkStartArgs(port, threads);
        try {
            socket = new DatagramSocket(port);
        } catch (final SocketException e) {
            throw new RuntimeException("Can not create server socket with " + port + " port", e);
        }
        listener = Executors.newSingleThreadExecutor();
        executors = getThreadPoolExecutorWithDiscardPolicy(threads);
        isClosed = false;
        listener.execute(this::listen);
    }

    private void listen() {
        while (!socket.isClosed() && !Thread.currentThread().isInterrupted()) {
            try {
                final DatagramPacket request = getResponsePacket(socket);
                socket.receive(request);
                if (executors == null) {
                    respond(request);
                } else {
                    executors.execute(() -> respond(request));
                }
            } catch (final IOException e) {
                logErrorNotClosed("Error during receiving clients requests", e);
            }
        }
    }

    private void respond(final DatagramPacket request) {
        Utils.setMessage(request, getResponse(Utils.getMessage(request)));
        try {
            socket.send(request);
        } catch (final IOException e) {
            logErrorNotClosed("Error during sending message to client", e);
        }
    }

    private void logErrorNotClosed(final String message, final Exception e) {
        if (!isClosed) {
            logError(message, e);
        }
    }

    static String getResponse(final String request) {
        return "Hello, " + request;
    }

    @Override
    public void close() {
        if (isClosed) {
            return;
        }
        isClosed = true;
        Utils.close(socket);
        Utils.shutdownAndAwaitTermination(listener, 3, TimeUnit.SECONDS);
        Utils.shutdownAndAwaitTermination(executors, 5, TimeUnit.SECONDS);
    }

    public static void runServer(final String[] args, final HelloServer server) {
        try {
            if (args == null || args.length != 2 || Arrays.stream(args).anyMatch(Objects::isNull)) {
                throw new RuntimeException("Expected such input: port threads");
            }
            try {
                server.start(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
                Thread.sleep(10_000);
            } catch (final NumberFormatException e) {
                throw new RuntimeException("Invalid input data", e);
            } catch (final InterruptedException e) {
                throw new RuntimeException("Server was unexpected interrupted", e);
            }
        } catch (final RuntimeException e) {
            String cause = e.getCause() != null ? " - " + e.getMessage() : "";
            logError(e.getMessage() + cause);
        }
    }

    public static void main(final String[] args) {
        runServer(args, new HelloUDPServer());
    }
}

package info.kgeorgiy.ja.slastin.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static info.kgeorgiy.ja.slastin.hello.Utils.*;

public class HelloUDPClient implements HelloClient {
    private final static Pattern RESPONSE_PATTERN = Pattern.compile("\\D*(\\d+)\\D+(\\d+)\\D*");
    final static int RESPONSE_TIMEOUT = 500;

    static void throwBadArg(final String argName, final String condition) {
        throw new RuntimeException(argName + " must be " + condition);
    }

    static void checkRunArgs(final String host, final int port, final String prefix, final int threads, final int requests) {
        if (host == null) {
            throwBadArg("host", "not null");
        }
        if (port < 0) {
            throwBadArg("port", ">= 0");
        }
        if (prefix == null) {
            throwBadArg("prefix", "not null");
        }
        if (threads <= 0) {
            throwBadArg("thread", "> 0");
        }
        if (requests <= 0) {
            throwBadArg("requests", "> 0");
        }
    }

    static boolean isResponseValid(final String response, final int threadNumber, final int requestNumber) {
        final Matcher matcher = RESPONSE_PATTERN.matcher(response);
        return matcher.find() && matcher.group(1).equals(Integer.toString(threadNumber)) &&
                matcher.group(2).equals(Integer.toString(requestNumber));
    }

    @Override
    public void run(final String host, final int port, final String prefix, final int threads, final int requests) {
        checkRunArgs(host, port, prefix, threads, requests);
        final SocketAddress serverAddress = getSocketAddress(host, port);
        final ExecutorService sendingService = Executors.newFixedThreadPool(threads);
        final CountDownLatch latch = new CountDownLatch(threads);
        IntStream.range(0, threads).forEach(i -> sendingService.execute(() -> sendAndReceive(serverAddress, prefix, i, requests, latch)));
        try {
            latch.await();
        } catch (final InterruptedException ignored) {
        }
    }

    private static void sendAndReceive(final SocketAddress serverAddress, final String prefix, final int threadNumber,
                                       final int requests, final CountDownLatch latch) {
        try (final DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(RESPONSE_TIMEOUT);
            final DatagramPacket request = getRequestPacket(serverAddress);
            final DatagramPacket response = getResponsePacket(socket);
            final String threadPrefix = prefix + threadNumber + "_";
            for (int i = 0; i < requests; i++) {
                final String requestMsg = threadPrefix + i;
                Utils.setMessage(request, requestMsg);
                while (!socket.isClosed() && !Thread.currentThread().isInterrupted()) {
                    try {
                        socket.send(request);
                        logRequest(requestMsg);
                        socket.receive(response);
                        final String responseMessage = Utils.getMessage(response);
                        if (isResponseValid(responseMessage, threadNumber, i)) {
                            logResponse(responseMessage);
                            break;
                        }
                    } catch (final IOException e) {
                        logError(String.format("Occurred during %d request in %s thread", i, threadNumber), e);
                    }
                }
            }
        } catch (final SocketException e) {
            logError("Occurred in " + threadNumber + " thread", e);
        } finally {
            latch.countDown();
        }
    }

    static void logRequest(final String requestMessage) {
        logInfo("Sent such request: " + requestMessage);
    }

    static void logResponse(final String responseMessage) {
        logInfo("Received such response: " + responseMessage);
    }

    public static void runClient(final String[] args, final HelloClient client) {
        try {
            if (args == null || args.length != 5 || Arrays.stream(args).anyMatch(Objects::isNull)) {
                throw new RuntimeException("Expected such input: [name|ip]-server port-server prefix threads");
            }
            try {
                client.run(args[0], Integer.parseInt(args[1]), args[2], Integer.parseInt(args[3]), Integer.parseInt(args[4]));
            } catch (final NumberFormatException e) {
                throw new RuntimeException("Invalid input data: " + e.getMessage());
            }
        } catch (final RuntimeException e) {
            System.err.println(e.getMessage());
        }
    }

    public static void main(final String[] args) {
        runClient(args, new HelloUDPClient());
    }
}

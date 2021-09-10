package info.kgeorgiy.ja.slastin.concurrent;

import java.util.Collection;
import java.util.List;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ParallelUtils {
    public static void checkThreads(final int threads) {
        if (threads <= 0) {
            throw new IllegalArgumentException("Number of threads can not be <= 0");
        }
    }

    public static List<Thread> initAndStart(final int threads, final IntFunction<Runnable> initializerByIndex) {
        return IntStream.range(0, threads)
                .mapToObj(i -> new Thread(initializerByIndex.apply(i)))
                .peek(Thread::start)
                .collect(Collectors.toList());
    }

    public static void joinAll(final Collection<Thread> threads) throws InterruptedException {
        InterruptedException interrupted = null;
        for (final Thread thread : threads) {
            while (true) {
                try {
                    thread.join();
                    break;
                } catch (final InterruptedException e) {
                    if (interrupted == null) {
                        interrupted = e;
                    } else {
                        interrupted.addSuppressed(e);
                    }
                }
            }
        }
        if (interrupted != null) {
            throw interrupted;
        }
    }
}

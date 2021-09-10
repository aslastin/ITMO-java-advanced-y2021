package info.kgeorgiy.ja.slastin.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.stream.IntStream;

import static info.kgeorgiy.ja.slastin.concurrent.ParallelUtils.*;

public class ParallelMapperImpl implements ParallelMapper {
    private final Queue<Runnable> missions;
    private final List<Thread> executors;
    private volatile boolean isClosed;

    public ParallelMapperImpl(final int threads) {
        checkThreads(threads);
        missions = new ArrayDeque<>();
        final Runnable executorMission = () -> {
            try {
                while (!Thread.interrupted()) {
                    getMission().run();
                }
            } catch (final InterruptedException ignored) {
            }
        };
        executors = initAndStart(threads, i -> executorMission);
    }

    private Runnable getMission() throws InterruptedException {
        synchronized (missions) {
            while (missions.isEmpty()) {
                missions.wait();
            }
            return missions.poll();
        }
    }

    private void addMission(final Runnable mission) {
        synchronized (missions) {
            missions.add(mission);
            missions.notify();
        }
    }

    @Override
    public void close() {
        if (isClosed) {
            return;
        }
        isClosed = true;
        executors.forEach(Thread::interrupt);
        try {
            joinAll(executors);
        } catch (InterruptedException ignored) {
        }
    }

    @Override
    public <T, R> List<R> map(final Function<? super T, ? extends R> f, final List<? extends T> args) throws InterruptedException {
        if (isClosed) {
            throw new IllegalStateException("ParallelMapper is closed");
        }
        final ResultGatherer<R> gatherer = new ResultGatherer<>(args.size());
        IntStream.range(0, args.size()).forEach(i -> addMission(() -> gatherer.set(i, f.apply(args.get(i)))));
        return gatherer.gatherResult();
    }

    private class ResultGatherer<T> {
        private final List<T> result;
        private int gathered;

        ResultGatherer(int size) {
            result = new ArrayList<>(Collections.nCopies(size, null));
        }

        public void set(final int index, final T value) {
            result.set(index, value);
            synchronized (this) {
                ++gathered;
                if (isClosed || gathered == result.size()) {
                    notify();
                }
            }
        }

        public synchronized List<T> gatherResult() throws InterruptedException {
            while (!isClosed && gathered != result.size()) {
                wait();
            }
            return result;
        }
    }
}

package info.kgeorgiy.ja.slastin.concurrent;

import info.kgeorgiy.java.advanced.concurrent.AdvancedIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static info.kgeorgiy.ja.slastin.concurrent.ParallelUtils.*;

public class IterativeParallelism implements AdvancedIP {
    private final ParallelMapper parallelMapper;

    public IterativeParallelism() {
        parallelMapper = null;
    }

    public IterativeParallelism(ParallelMapper parallelMapper) {
        this.parallelMapper = parallelMapper;
    }

    private <T, R> R parallelMission(
            int threads,
            final List<? extends T> values,
            final Function<? super Stream<? extends T>, ? extends R> threadFunction,
            final Function<? super Stream<R>, ? extends R> mergeFunction
    ) throws InterruptedException {
        checkThreads(threads);
        final int realThreads = Math.min(threads, values.size());
        final List<Stream<? extends T>> groups = realThreads == 0 ? Collections.emptyList() : split(realThreads, values);
        final List<R> result;
        if (parallelMapper == null) {
            result = new ArrayList<>(Collections.nCopies(groups.size(), null));
            joinAll(initAndStart(groups.size(),i -> () -> result.set(i, threadFunction.apply(groups.get(i)))));
        } else {
            result = parallelMapper.map(threadFunction, groups);
        }
        return mergeFunction.apply(result.stream());
    }

    private static <T> List<Stream<? extends T>> split(final int threads, final List<? extends T> values) {
        final int groupSize = values.size() / threads;
        final List<Stream<? extends T>> groups = new ArrayList<>(threads);
        for (int i = 0, r = 0, rest = values.size() % threads; i < threads; i++, rest--) {
            final int l = r;
            r += groupSize + (rest > 0 ? 1 : 0);
            groups.add(values.subList(l, r).stream());
        }
        return groups;
    }

    private <T, R> List<R> parallelMissionToList(
            final int threads,
            final List<? extends T> values,
            final Function<? super Stream<? extends T>, ? extends Stream<? extends R>> threadMiddleFunction
    ) throws InterruptedException {
        return parallelMission(threads, values,
                stream -> threadMiddleFunction.apply(stream).collect(Collectors.toList()),
                stream -> stream.flatMap(Collection::stream).collect(Collectors.toList())
        );
    }

    @Override
    public <T> T maximum(final int threads, final List<? extends T> values, final Comparator<? super T> comparator) throws InterruptedException {
        if (values.isEmpty()) {
            throw new NoSuchElementException("values can not be empty");
        }
        final Function<Stream<? extends T>, T> threadMax = stream -> stream.max(comparator).orElse(null);
        return parallelMission(threads, values, threadMax, threadMax);
    }

    @Override
    public <T> T minimum(final int threads, final List<? extends T> values, final Comparator<? super T> comparator) throws InterruptedException {
        return maximum(threads, values, Collections.reverseOrder(comparator));
    }

    @Override
    public <T> boolean all(final int threads, final List<? extends T> values, final Predicate<? super T> predicate) throws InterruptedException {
        return parallelMission(threads, values,
                stream -> stream.allMatch(predicate),
                stream -> stream.allMatch(Boolean::booleanValue)
        );
    }

    @Override
    public <T> boolean any(final int threads, final List<? extends T> values, final Predicate<? super T> predicate) throws InterruptedException {
        return !all(threads, values, predicate.negate());
    }

    @Override
    public String join(final int threads, final List<?> values) throws InterruptedException {
        return parallelMission(threads, values,
                stream -> stream.map(Objects::toString).collect(Collectors.joining()),
                stream -> stream.collect(Collectors.joining())
        );
    }

    @Override
    public <T> List<T> filter(final int threads, final List<? extends T> values, final Predicate<? super T> predicate) throws InterruptedException {
        return parallelMissionToList(threads, values, stream -> stream.filter(predicate));
    }

    @Override
    public <T, R> List<R> map(final int threads, final List<? extends T> values, final Function<? super T, ? extends R> f) throws InterruptedException {
        return parallelMissionToList(threads, values, stream -> stream.map(f));
    }

    @Override
    public <T> T reduce(final int threads, final List<T> values, final Monoid<T> monoid) throws InterruptedException {
        return mapReduce(threads, values, Function.identity(), monoid);
    }

    @Override
    public <T, R> R mapReduce(final int threads, final List<T> values, final Function<T, R> lift, final Monoid<R> monoid) throws InterruptedException {
        Function<Stream<R>, R> threadReduce = stream -> stream.reduce(monoid.getIdentity(), monoid.getOperator());
        return parallelMission(threads, values, stream -> threadReduce.apply(stream.map(lift)), threadReduce);
    }

}

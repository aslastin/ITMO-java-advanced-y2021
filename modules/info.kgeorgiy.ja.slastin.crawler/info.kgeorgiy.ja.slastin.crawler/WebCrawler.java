package info.kgeorgiy.ja.slastin.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;

public class WebCrawler implements AdvancedCrawler {
    private final static int DEFAULT_DEPTH = 2;
    private final static int DEFAULT_DOWNLOADERS = 10;
    private final static int DEFAULT_EXTRACTORS = 10;
    private final static int DEFAULT_PERHOST = 10;

    private final static Predicate<String> ANY_HOST_PREDICATE = url -> true;

    private final ConcurrentMap<String, HostSupervisor> hosts = new ConcurrentHashMap<>();
    private final StoppablePhasers stoppablePhasers = new StoppablePhasers();
    private final Downloader downloader;
    private final ExecutorService downloadPool, extractPool;
    private final int perHost;
    private volatile boolean isClosed;

    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        checkCrawlerArgs(downloaders, extractors, perHost);
        this.downloader = downloader;
        downloadPool = Executors.newFixedThreadPool(downloaders);
        extractPool = Executors.newFixedThreadPool(extractors);
        this.perHost = perHost;
    }

    public WebCrawler(int downloaders, int extractors, int perHost) throws IOException {
        this(new CachingDownloader(), downloaders, extractors, perHost);
    }

    private static void shutdownAndAwaitTermination(ExecutorService pool) {
        pool.shutdown();
        try {
            if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                pool.shutdownNow();
                if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                    System.err.println("Pool did not terminate");
                }
            }
        } catch (InterruptedException ie) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private static void checkIsPositive(int arg, String argName) {
        if (arg <= 0) {
            throw new IllegalArgumentException(argName + " must be positive");
        }
    }

    private static void checkCrawlerArgs(int downloaders, int extractors, int perHost) {
        checkIsPositive(downloaders, "downloaders");
        checkIsPositive(extractors, "extractors");
        checkIsPositive(perHost, "perHost");
    }

    private static void checkDepth(int depth) {
        checkIsPositive(depth, "depth");
    }

    private static int retrieveMainArg(String[] args, int index, int defaultValue) {
        if (args.length <= index) {
            return defaultValue;
        }
        int result = 0;
        try {
            result = Integer.parseInt(args[index]);
        } catch (NumberFormatException ignored) {
        }
        return result;
    }

    private static void processMain(String[] args) {
        if (args == null || args.length < 1 || args.length > 5 || Arrays.stream(args).anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Expected such input: url [depth [downloaders [extractors [perHost]]]]");
        }
        int depth = retrieveMainArg(args, 1, DEFAULT_DEPTH);
        checkDepth(depth);
        int downloaders = retrieveMainArg(args, 2, DEFAULT_DOWNLOADERS);
        int extractors = retrieveMainArg(args, 3, DEFAULT_EXTRACTORS);
        int perHost = retrieveMainArg(args, 4, DEFAULT_PERHOST);
        checkCrawlerArgs(downloaders, extractors, perHost);

        try (WebCrawler crawler = new WebCrawler(downloaders, extractors, perHost)) {
            Result result = crawler.download(args[0], depth);

            System.out.println("\nDowloaded urls:\n");
            result.getDownloaded().forEach(System.out::println);
            System.out.println("\nUrls with error:\n");
            result.getErrors().forEach((url, error) -> System.out.println(url + " : " + error));
        } catch (IOException e) {
            throw new IllegalArgumentException("Can not create instance of CachingDownloader");
        }
    }

    public static void main(String[] args) {
        try {
            processMain(args);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
        }
    }

    private void extractTask(Document page, Phaser phaser, BlockingQueue<String> urlsQueue, Set<String> used) {
        try {
            page.extractLinks().stream().filter(used::add).forEach(urlsQueue::add);
        } catch (IOException ignored) {
        } finally {
            phaser.arrive();
        }
    }

    private void addDownloadTask(String url, int depth, Phaser phaser, Predicate<String> hostPredicate,
                                 BlockingQueue<String> urlsQueue, Set<String> used, Set<String> ok,
                                 ConcurrentMap<String, IOException> bad) {
        try {
            String host = URLUtils.getHost(url);
            if (!hostPredicate.test(host)) {
                return;
            }
            HostSupervisor hostSupervisor = hosts.computeIfAbsent(host, hostUrl -> new HostSupervisor());
            phaser.register();
            hostSupervisor.addTask(() -> {
                try {
                    Document page = downloader.download(url);
                    ok.add(url);
                    if (depth > 1) {
                        phaser.register();
                        extractPool.submit(() -> extractTask(page, phaser, urlsQueue, used));
                    }
                } catch (IOException e) {
                    bad.put(url, e);
                } finally {
                    phaser.arrive();
                    hostSupervisor.nextTask();
                }
            });
        } catch (RejectedExecutionException ignored) {
        } catch (MalformedURLException e) {
            bad.put(url, e);
        }
    }

    private List<String> downloadUrls(List<String> urls, int depth, Predicate<String> hostPredicate,
                                      BlockingQueue<String> queue, Set<String> used, Set<String> ok,
                                      ConcurrentMap<String, IOException> bad) {
        Phaser phaser = new Phaser(1);
        urls.forEach(url -> addDownloadTask(url, depth, phaser, hostPredicate, queue, used, ok, bad));
        stoppablePhasers.add(phaser);
        phaser.arriveAndAwaitAdvance();
        stoppablePhasers.remove(phaser);
        List<String> result = new ArrayList<>(queue.size());
        queue.drainTo(result);
        return result;
    }

    private Result download(String url, int depth, Predicate<String> hostPredicate) {
        checkDepth(depth);
        BlockingQueue<String> queue = new LinkedBlockingQueue<>();
        Set<String> used = ConcurrentHashMap.newKeySet();
        used.add(url);
        Set<String> ok = ConcurrentHashMap.newKeySet();
        ConcurrentMap<String, IOException> bad = new ConcurrentHashMap<>();
        List<String> urls = List.of(url);
        for (int i = depth; i >= 1 && !isClosed && !Thread.currentThread().isInterrupted(); i--) {
            urls = downloadUrls(urls, i, hostPredicate, queue, used, ok, bad);
        }
        return new Result(new ArrayList<>(ok), bad);
    }

    @Override
    public Result download(String url, int depth) {
        return download(url, depth, ANY_HOST_PREDICATE);
    }

    @Override
    public Result download(String url, int depth, List<String> hosts) {
        Set<String> availableHosts = ConcurrentHashMap.newKeySet();
        availableHosts.addAll(hosts);
        return download(url, depth, availableHosts::contains);
    }

    @Override
    public void close() {
        if (isClosed) {
            return;
        }
        isClosed = true;
        shutdownAndAwaitTermination(downloadPool);
        shutdownAndAwaitTermination(extractPool);
        stoppablePhasers.stop();
    }

    private class HostSupervisor {
        final Queue<Runnable> pending = new ArrayDeque<>();
        int processed = 0;

        synchronized void addTask(Runnable task) {
            if (processed != perHost) {
                ++processed;
                downloadPool.submit(task);
            } else {
                pending.add(task);
            }
        }

        synchronized void nextTask() {
            Runnable task = pending.poll();
            if (task != null) {
                downloadPool.submit(task);
            } else {
                --processed;
            }
        }
    }

    private class StoppablePhasers {
        final Set<Phaser> phasers = ConcurrentHashMap.newKeySet();

        void add(Phaser phaser) {
            if (isClosed) {
                synchronized (this) {
                    phaser.forceTermination();
                    phasers.add(phaser);
                }
            } else {
                phasers.add(phaser);
            }
        }

        void remove(Phaser phaser) {
            if (isClosed) {
                synchronized (this) {
                    phasers.remove(phaser);
                }
            } else {
                phasers.remove(phaser);
            }
        }

        synchronized void stop() {
            phasers.forEach(Phaser::forceTermination);
        }
    }
}

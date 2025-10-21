package com.radut.plugin.bfw.watcher;

import com.intellij.openapi.diagnostic.Logger;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Recursively watches a directory tree for file system changes. Automatically registers new subdirectories and handles
 * directory deletions.
 */
public class RecursiveDirectoryWatcher {
    private static final Logger LOG = Logger.getInstance(RecursiveDirectoryWatcher.class);

    private final Path rootPath;
    private final Set<Path> excludePaths;
    private final WatchListener listener;

    private WatchService watchService;
    private Thread watchThread;
    private volatile boolean running = false;

    // Maps to track registered directories
    private final Map<WatchKey, Path> watchKeyToPath = new ConcurrentHashMap<>();
    private final Map<Path, WatchKey> pathToWatchKey = new ConcurrentHashMap<>();

    // Map to track all files and subdirectories within each watched directory
    // Key: directory path, Value: set of child paths (files and subdirectories)
    private final Map<Path, Set<Path>> directoryContents = new ConcurrentHashMap<>();

    /**
     * Listener interface for file change notifications.
     */
    public interface WatchListener {
        /**
         * Called when file system events have been detected and the settle delay has passed.
         */
        void onFileChangesDetected(final WatchEvent.Kind<?> kind, final Path fileName);
    }

    /**
     * Creates a new recursive directory watcher.
     *
     * @param rootPath     The root directory to watch
     * @param excludePaths Paths to exclude from watching (can be empty)
     * @param listener     Callback for change notifications
     */
    public RecursiveDirectoryWatcher(Path rootPath, Set<Path> excludePaths, WatchListener listener) {
        this.rootPath = rootPath;
        this.excludePaths = excludePaths != null ? excludePaths : Collections.emptySet();
        this.listener = listener;
    }

    /**
     * Starts watching the directory tree.
     */
    public synchronized void start() throws IOException {
        if (running) {
            LOG.warn("RecursiveDirectoryWatcher already running for: " + rootPath);
            return;
        }

        if (!Files.exists(rootPath) || !Files.isDirectory(rootPath)) {
            throw new IOException("Root path does not exist or is not a directory: " + rootPath);
        }

        LOG.info("Starting RecursiveDirectoryWatcher for: " + rootPath);

        watchService = FileSystems.getDefault().newWatchService();
        running = true;

        // Register all existing directories
        registerRecursively(rootPath);

        // Start the watch thread
        watchThread = new Thread(this::watchLoop, "RecursiveWatcher-" + rootPath.getFileName());
        watchThread.setDaemon(true);
        watchThread.start();
    }

    /**
     * Stops watching the directory tree.
     */
    public synchronized void stop() {
        if (!running) {
            return;
        }

        LOG.info("Stopping RecursiveDirectoryWatcher for: " + rootPath);
        running = false;

        // Close watch service to unblock the watch thread
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                LOG.warn("Error closing watch service", e);
            }
        }

        // Wait for watch thread to finish
        if (watchThread != null) {
            try {
                watchThread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOG.warn("Interrupted while waiting for watch thread to stop");
            }
            watchThread = null;
        }

        // Clear tracking maps
        watchKeyToPath.clear();
        pathToWatchKey.clear();
        directoryContents.clear();

        LOG.info("Stopped RecursiveDirectoryWatcher for: " + rootPath);
    }

    /**
     * Checks if the watcher is currently running.
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Recursively registers a directory and all its subdirectories with the watch service.
     */
    private void registerRecursively(Path directory) throws IOException {
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                // Skip excluded paths
                if (shouldExclude(dir)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }

                // Register this directory
                registerDirectory(dir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                LOG.warn("Failed to visit: " + file, exc);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Registers a single directory with the watch service.
     */
    private void registerDirectory(Path dir) throws IOException {
        // Don't register if already registered
        if (pathToWatchKey.containsKey(dir)) {
            return;
        }

        WatchKey key = dir.register(watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE,
                StandardWatchEventKinds.ENTRY_MODIFY);

        watchKeyToPath.put(key, dir);
        pathToWatchKey.put(dir, key);

        // Scan and track all files and subdirectories in this directory
        scanAndTrackDirectory(dir);

        LOG.info("Registered directory: " + dir);
    }

    /**
     * Unregisters a directory from the watch service.
     */
    private void unregisterDirectory(Path dir) {
        WatchKey key = pathToWatchKey.remove(dir);
        if (key != null) {
            watchKeyToPath.remove(key);
            key.cancel();
            LOG.info("Unregistered directory: " + dir);
        }
    }

    /**
     * Checks if a path should be excluded from watching.
     */
    private boolean shouldExclude(Path path) {
        for (Path excludePath : excludePaths) {
            if (path.equals(excludePath) || path.startsWith(excludePath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tracks a file or subdirectory within its parent directory.
     */
    private void trackChild(Path parent, Path child) {
        directoryContents.computeIfAbsent(parent, k -> ConcurrentHashMap.newKeySet()).add(child);
    }

    /**
     * Removes tracking for a child within its parent directory.
     *
     * @return
     */
    private boolean untrackChild(Path parent, Path child) {
        Set<Path> children = directoryContents.get(parent);
        if (children != null) {
            return children.remove(child);
        }
        return false;
    }

    /**
     * Recursively gets all tracked children (files and subdirectories) of a directory.
     */
    private List<Path> getAllChildren(Path directory) {
        List<Path> allChildren = new ArrayList<>();
        Set<Path> directChildren = directoryContents.get(directory);

        if (directChildren != null) {
            for (Path child : directChildren) {
                allChildren.add(child);
                // If this child is a directory, recursively get its children
                if (Files.isDirectory(child)) {
                    allChildren.addAll(getAllChildren(child));
                }
            }
        }

        return allChildren;
    }

    /**
     * Scans a directory and tracks all its immediate children.
     */
    private void scanAndTrackDirectory(Path directory) {
        try {
            if (Files.isDirectory(directory)) {
                try (var stream = Files.list(directory)) {
                    stream.forEach(child -> {
                        if (!shouldExclude(child)) {
                            trackChild(child.getParent(), child);
                            // If it's a subdirectory, recursively scan it
                            if (Files.isDirectory(child)) {
                                scanAndTrackDirectory(child);
                            }
                        }
                    });
                } catch (IOException e) {
                    LOG.warn("Failed to scan directory: " + directory, e);
                }
            }
        } catch (Exception e) {
            LOG.warn("Error tracking directory: " + directory, e);
        }
    }

    /**
     * Main watch loop that processes file system events.
     */
    private void watchLoop() {
        try {
            while (running) {
                WatchKey key;
                try {
                    key = watchService.poll(200, TimeUnit.MILLISECONDS);
                    if (key == null) {
                        continue;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (ClosedWatchServiceException e) {
                    // Watch service was closed, exit loop
                    break;
                }

                Path dir = watchKeyToPath.get(key);
                if (dir == null) {
                    LOG.warn("Watch key not found in map, cancelling key");
                    key.cancel();
                    continue;
                }

                try {

                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();

                        if (kind == StandardWatchEventKinds.OVERFLOW) {
                            LOG.warn("Watch event overflow detected");
                            continue;
                        }

                        @SuppressWarnings("unchecked")
                        WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                        Path fileName = pathEvent.context();
                        Path fullPath = dir.resolve(fileName);

                        // Handle CREATE events
                        if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                            if (!shouldExclude(fullPath)) {
                                // If it's a directory, register it for watching
                                if (Files.isDirectory(fullPath)) {
                                    scanAndTrackDirectory(fullPath);
                                    registerRecursively(fullPath);
                                }
                                // Track this new file/directory in the parent directory
                                trackChild(fullPath.getParent(), fullPath);

                                // Notify listener about the creation
                                notifyListener(kind, fullPath);
                                //mkdir -p multi-level
                                for (Path child : getAllChildren(fullPath)) {
                                    notifyListener(kind, child);
                                }
                            }
                        }
                        // Handle DELETE events
                        else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {

                            if (!shouldExclude(fullPath)) {
                                if (directoryContents.containsKey(fullPath)) {
                                    // Get all children before we remove tracking
                                    List<Path> allChildren = getAllChildren(fullPath);

                                    // If it had children, notify about each child
                                    for (Path child : allChildren) {
                                        untrackChild(child.getParent(), child);
                                        notifyListener(kind, child);
                                    }
                                }
                                Set<Path> paths = directoryContents.get(fullPath.getParent());
                                if (paths != null) {
                                    if (paths.contains(fullPath)) {
                                        untrackChild(fullPath.getParent(), fullPath);
                                        notifyListener(kind, fullPath);
                                    }
                                }

                                // Clean up directory contents tracking
                                directoryContents.remove(fullPath);

                                // If this was a registered directory, unregister it
                                if (pathToWatchKey.containsKey(fullPath)) {
                                    unregisterDirectory(fullPath);

                                    // Unregister all subdirectories
                                    List<Path> toRemove = new ArrayList<>();
                                    for (Path registeredPath : pathToWatchKey.keySet()) {
                                        if (registeredPath.startsWith(fullPath)
                                            && !registeredPath.equals(fullPath)) {
                                            toRemove.add(registeredPath);
                                        }
                                    }
                                    for (Path path : toRemove) {
                                        unregisterDirectory(path);
                                    }
                                }

                            }
                        }
                        // Handle MODIFY events
                        else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                            if (!shouldExclude(fullPath)) {
                                // Ensure the file is tracked (in case we missed the CREATE)
                                trackChild(fullPath.getParent(), fullPath);

                                // Notify listener about the modification
                                notifyListener(kind, fullPath);
                            }
                        }

                    }

                } finally {
                    boolean valid = key.reset();
                    if (!valid) {
                        // Directory is no longer accessible
                        Path removed = watchKeyToPath.remove(key);
                        if (removed != null) {
                            Path fullPath = dir.resolve(removed);
                            if (!shouldExclude(fullPath)) {
                                if (directoryContents.containsKey(fullPath)) {
                                    List<Path> allChildren = getAllChildren(fullPath);
                                    // If it had children, notify about each child
                                    for (Path child : allChildren) {
                                        untrackChild(child.getParent(), child);
                                        notifyListener(StandardWatchEventKinds.ENTRY_DELETE, child);
                                    }
                                }
                                Set<Path> paths = directoryContents.get(fullPath.getParent());
                                if (paths != null) {
                                    if (paths.contains(fullPath)) {
                                        untrackChild(fullPath.getParent(), fullPath);
                                        notifyListener(StandardWatchEventKinds.ENTRY_DELETE, fullPath);
                                    }
                                }

                                // Clean up directory contents tracking
                                directoryContents.remove(fullPath);
                            }
                        }
                        pathToWatchKey.values().remove(key);
                        LOG.info("Watch key no longer valid for: " + dir);
                    }

                }
            }
        } catch (
                Exception e) {
            LOG.error("Error in watch loop for: " + rootPath, e);
        }
    }


    private void notifyListener(final WatchEvent.Kind<?> kind, final Path fileName) {

        if (listener != null && running) {
            try {
                listener.onFileChangesDetected(kind, fileName);
            } catch (Exception e) {
                LOG.error("Error notifying listener", e);
            }
        }
    }

    /**
     * Gets the root path being watched.
     */
    public Path getRootPath() {
        return rootPath;
    }

}

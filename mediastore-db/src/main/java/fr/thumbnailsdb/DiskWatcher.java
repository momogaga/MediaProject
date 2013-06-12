package fr.thumbnailsdb;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.nio.file.StandardWatchEventKinds.*;
import static java.nio.file.LinkOption.*;

/**
 * Created with IntelliJ IDEA.
 * User: fhuet
 * Date: 11/06/13
 * Time: 18:08
 * To change this template use File | Settings | File Templates.
 */
public class DiskWatcher {

    private final long MODIFY_TIMEOUT = 5000;

    private final WatchService watcher;
    private final Map<WatchKey, Path> keys;

    //we keep the path and the time of the files
    //for which we have received a EVENT_MODIFY
    private Map<Path, Long> currentModification;


    @SuppressWarnings("unchecked")
    static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>) event;
    }

    public DiskWatcher(String[] path) throws IOException {
        this.watcher = FileSystems.getDefault().newWatchService();
        this.keys = new HashMap<WatchKey, Path>();
        this.currentModification = new HashMap<Path, Long>();

        for (String s : path) {
            registerAll(Paths.get(s));
        }
//
    }

    private void register(Path dir) throws IOException {
        WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);

        Path prev = keys.get(key);
        if (prev == null) {
            System.out.format("register: %s\n", dir);
        } else {
            if (!dir.equals(prev)) {
                System.out.format("update: %s -> %s\n", prev, dir);
            }
        }

        keys.put(key, dir);
    }


    private void registerAll(final Path start) throws IOException {
        // register directory and sub-directories
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                    throws IOException {
                System.out.println("registering " + dir);
                register(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }


    void processEvents() {
        for (; ; ) {
            // wait for key to be signalled
            WatchKey key;
            try {
                key = watcher.poll(MODIFY_TIMEOUT, TimeUnit.MILLISECONDS);
            } catch (InterruptedException x) {
                return;
            }

            Path dir = keys.get(key);
            if (dir == null) {
                processModification(null);
                //TIMEOUT, continue
                System.err.println("WatchKey not recognized!!");
                continue;
            } else {

                for (WatchEvent<?> event : key.pollEvents()) {
                    processEvent(dir, event);
                }
            }

            // reset key and remove from set if directory no longer accessible
            boolean valid = key.reset();
            if (!valid) {
                keys.remove(key);
            }
        }
    }

    protected void processEvent(Path dir, WatchEvent event) {
        //WatchEvent<?> event = key.pollEvents();
        WatchEvent.Kind kind = event.kind();
        // Path dir = keys.get(event.context());

        // Context for directory entry event is the file name of entry
        WatchEvent<Path> ev = cast(event);
        Path name = ev.context();
        Path child = dir.resolve(name);

        // print out event
        System.out.format("%s: %s\n", event.kind().name(), child);

        // TBD - provide example of how OVERFLOW event is handled
        if (kind == OVERFLOW) {
            return;
        }

        // if modification, wait until no other modification
        //has been triggered, indicating the file is hopefully complete
        if (kind == ENTRY_MODIFY) {
            processModification(dir);
        }

        // if directory is created, and watching recursively, then
        // register it and its sub-directories
        if (kind == ENTRY_CREATE) {
            try {
                if (Files.isDirectory(child, NOFOLLOW_LINKS)) {
                    registerAll(child);
                }
            } catch (IOException x) {
                // ignore to keep sample readbale
            }
        }
    }


    /**
     * Process modifications on files
     * if path is not null, add it to the currentModification
     * map with current time
     * if path is null, check if entries in currentModification have
     * reached timeout
     *
     * @param path
     */
    protected void processModification(Path path) {
        if (path != null) {
            System.out.println("processModification : modification in progress for " + path);
            currentModification.put(path, System.currentTimeMillis());
        } else {
            System.out.println("processModification : MODIFY_TIMEOUT reached" );
            Iterator<Path> it = currentModification.keySet().iterator();
            while (it.hasNext()) {
                Path p = it.next();
                long mTime = currentModification.get(p);
                if ((System.currentTimeMillis() - mTime) > MODIFY_TIMEOUT) {
                    //the file has not been modified for MODIFY_TIMEOUT, assume it is
                    //safe now
                    System.out.println("processModification : " + p + " modifications OVER");
                    //currentModification.remove(p);
                    it.remove();
                }
            }

        }
    }

    public static void main(String[] args) throws IOException {
        new DiskWatcher(args).processEvents();
    }

}

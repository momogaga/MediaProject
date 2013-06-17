package fr.thumbnailsdb.diskmonitor;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
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

    private final int FILE_CREATED = 0;
    private final int FILE_MODIFIED = 1;
    private final int FILE_DELETED = 2;
    private final int FOLDER_CREATED = 3;
    private final int FOLDER_MODIFIED = 4;
    private final int FOLDER_DELETED = 5;


    private final long MODIFY_TIMEOUT = 5000;

    private final WatchService watcher;


    private List<DiskListener> listeners;

    //we keep the path and the time of the files
    //for which we have received a EVENT_MODIFY
    private Map<Path, Long> currentModification;


    @SuppressWarnings("unchecked")
    static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>) event;
    }

    public DiskWatcher() throws IOException {
        this.watcher = FileSystems.getDefault().newWatchService();
        //      this.keys = new HashMap<WatchKey, Path>();
        this.currentModification = new HashMap<Path, Long>();
        listeners = new ArrayList<fr.thumbnailsdb.diskmonitor.DiskListener>();
    }

    public DiskWatcher(String[] path) throws IOException {
        this();
        for (String s : path) {
            System.out.println("DiskWatcher.DiskWatcher " + Paths.get(s).toAbsolutePath() );

            registerAll(Paths.get(s).toAbsolutePath());
        }
    }


    public void addPath(String s ) throws IOException {
        registerAll(Paths.get(s));
    }

    private void register(Path dir) throws IOException {
        WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
    }


    private void registerAll(final Path start) throws IOException {
        // register directory and sub-directories
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                    throws IOException {
                register(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }


    /**
     * Start a thread which will process incoming events
     */
    public void processEvents() {
        new Thread(new Runnable() {
            public void run() {
                for (; ; ) {
                    // wait for key to be signalled
                    WatchKey key;
                    try {
                        key = watcher.poll(MODIFY_TIMEOUT, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException x) {
                        return;
                    }
                    if (key != null) {

                        for (WatchEvent<?> event : key.pollEvents()) {
                            processEvent(key.watchable().toString(), event);
                        }
                        key.reset();
                    } else {
                        //timeout reached
                        processModification(null);
                    }
                }
            }
        }).start();
    }

    protected void processEvent(String path, WatchEvent event) {
        WatchEvent.Kind kind = event.kind();

        WatchEvent<Path> ev = cast(event);
       Path name= FileSystems.getDefault().getPath(path, ev.context().toString());
        //Path name = new Path(//ev.context().toAbsolutePath();
     //   System.out.println(new String(new byte[] {47,104,111,109,101,47,1,117,101,116,47,119,111,114,107,115,112,97,99,101,115,47,114,101,99,104,101,114,99,104,101,101,102,102,105,99,97,99,101,105,109,97,103,101,115,115,105,109,105,108,97,105,114,101,115,47,77,101,100,105,97,83,116,111,114,101}));

        if (kind == OVERFLOW) {
            return;
        }

        // if modification, wait until no other modification
        //has been triggered, indicating the file is hopefully complete
        if (kind == ENTRY_MODIFY) {
            processModification(name);
        }

        // if directory is created, and watching recursively, then
        // register it and its sub-directories
        if (kind == ENTRY_CREATE) {
         //   System.out.println("DiskWatcher.processEvent ENTRY_CREATE " + name);
            try {
              //  new File(name.toAbsolutePath());

                if (Files.isDirectory(name)) {
                    this.fireEvent(name, FOLDER_CREATED);
                    registerAll(name);
                } else {
                    this.fireEvent(name, FILE_CREATED);
                }
            } catch (IOException x) {
                // ignore to keep sample readbale
            }
        }

        if (kind == ENTRY_DELETE) {
            try {
                if (Files.isDirectory(name, NOFOLLOW_LINKS)) {
                    this.fireEvent(name, FOLDER_DELETED);
                    registerAll(name);
                } else {
                    this.fireEvent(name, FILE_DELETED);
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
     * reached timeout and trigger notification
     *
     * @param path
     */

    protected void processModification(Path path) {
        if (path != null) {
          //  System.out.println("processModification : modification in progress for " + path);
            currentModification.put(path, System.currentTimeMillis());
        } else {
          //  System.out.println("processModification : MODIFY_TIMEOUT reached");
            Iterator<Path> it = currentModification.keySet().iterator();
            while (it.hasNext()) {
                Path p = it.next();
                long mTime = currentModification.get(p);
                if ((System.currentTimeMillis() - mTime) > MODIFY_TIMEOUT) {
                    //the file has not been modified for MODIFY_TIMEOUT, assume it is
                    //safe now
                    if (Files.isDirectory(p, NOFOLLOW_LINKS)) {
                        this.fireEvent(p, FOLDER_MODIFIED);
                    } else {
                        this.fireEvent(p, FILE_MODIFIED);
                    }
//                            System.out.println("processModification : " + p + " modifications OVER");
                    //currentModification.remove(p);
                    it.remove();
                }
            }

        }
    }

    public synchronized void addListener(DiskListener listener) {
        this.listeners.add(listener);
    }

    public synchronized DiskListener removeListener(DiskListener listener) {
        Iterator<DiskListener> it = this.listeners.iterator();
        while (it.hasNext()) {
            DiskListener tmp = it.next();
            if (tmp.equals(listener)) {
                it.remove();
                return tmp;
            }
        }
        return null;
    }


    public void fireEvent(Path p, int event) {
        synchronized (this.listeners) {
            for (DiskListener dw : this.listeners) {
                switch (event) {
                    case FILE_CREATED:
                        dw.fileCreated(p);
                        break;
                    case FILE_MODIFIED:
                        dw.fileModified(p);
                        break;
                    case FILE_DELETED:
                        dw.fileDeleted(p);
                        break;
                    case FOLDER_CREATED:
                        dw.folderCreated(p);
                        break;
                    case FOLDER_MODIFIED:
                        dw.folderModified(p);
                        break;
                    case FOLDER_DELETED:
                        dw.folderDeleted(p);
                        break;
                }
            }
        }
    }


    public static void main(String[] args) throws IOException {
        DiskWatcher dw = new DiskWatcher(args);
        dw.addListener(new ConsoleDiskListener());
        dw.processEvents();
    }

}

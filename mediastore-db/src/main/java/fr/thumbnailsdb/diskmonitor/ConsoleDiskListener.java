package fr.thumbnailsdb.diskmonitor;

import java.nio.file.Path;

public class ConsoleDiskListener implements DiskListener {


    public void fileCreated(Path p) {
        System.out.println("ConsoleDiskListener.fileCreated " + p);
    }

    public void fileModified(Path p) {
        System.out.println("ConsoleDiskListener.fileModified "  +p);
    }

    public void fileDeleted(Path p) {
        System.out.println("ConsoleDiskListener.fileDeleted " + p);
    }

    public void folderCreated(Path p) {
        System.out.println("ConsoleDiskListener.folderCreated " + p);
    }

    public void folderModified(Path p) {
        System.out.println("ConsoleDiskListener.folderModified " + p );
    }

    public void folderDeleted(Path p) {
        System.out.println("ConsoleDiskListener.folderDeleted " +p);
    }
}

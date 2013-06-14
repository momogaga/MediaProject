package fr.thumbnailsdb.diskmonitor;

import java.nio.file.Path;

public interface DiskListener {

    public void fileCreated(Path p);

    public void fileModified(Path p);

    public void fileDeleted(Path p);

    public void folderCreated(Path p);

    public void folderModified(Path p);

    public void folderDeleted(Path p);

}

package fr.thumbnailsdb;

import fr.thumbnailsdb.lsh.LSH;
import fr.thumbnailsdb.utils.Logger;
import fr.thumbnailsdb.utils.ProgressBar;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.sql.*;
import java.util.*;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

public class ThumbStore {

    protected static String DEFAULT_DB = "localDB";
    protected static int CURRENT_VERSION = 4;


    //This is used as a cache of preloaded descriptors
    protected PreloadedDescriptors preloadedDescriptors;
    protected LSH lsh;

    protected HashMap<String, Connection> connexions = new HashMap<String, Connection>();
    protected ArrayList<String> pathsOfDBOnDisk = new ArrayList<String>();


    public ThumbStore() {
        this(DEFAULT_DB);

    }

    public ThumbStore(String path) {
        if (path == null) {
            path = DEFAULT_DB;
        }
        System.err.println("ThumbStore.ThumbStore() using " + path + " as DB");
        this.addDB(path);
    }


    public void addDB(String path) {
        this.pathsOfDBOnDisk.add(path);
        try {
            Connection c = connectToDB(path);
            checkAndCreateTables(c);
            ArrayList<String> paths = getIndexedPaths(c);
            if (paths.size() == 0) {
                System.err.println("ThumbStore.addDB found empty db");
                connexions.put("", c);
            } else {
                for (String s : paths) {
                    System.err.println("ThumbStore.addDB path : " + s + " with db : " + c);
                    connexions.put(s, c);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

    }


    private HashSet<Connection> getConnections() {
        HashSet<Connection> r = new HashSet<Connection>();
        for (Connection c : connexions.values()) {
            r.add(c);
        }
        return r;
    }

    public Connection connectToDB(String path) throws InstantiationException, IllegalAccessException, ClassNotFoundException,
            SQLException {
        Class.forName("org.h2.Driver").newInstance();
        Connection connection = DriverManager.getConnection("jdbc:h2:" + path + "", "sa", "");

        return connection;
    }

    public void checkAndCreateTables(Connection connexion) throws SQLException {

        DatabaseMetaData dbm = connexion.getMetaData();
        // check if "employee" table is there
        ResultSet tables = dbm.getTables(null, null, "IMAGES", null);

        if (tables.next()) {
            Logger.getLogger().log("ThumbStore.checkAndCreateTables() table IMAGES exists!");
            checkOrAddColumns(dbm);
            // Table exists
        } else {
            Logger.getLogger().log("ThumbStore.checkAndCreateTables() table IMAGES does not exist, should create it");
            String table = "CREATE TABLE IMAGES(id  bigint identity(1,1),path varchar(256), size long, mtime long, md5 varchar(256), hash varchar(100),  lat double, lon double)";
            Statement st = connexion.createStatement();
            st.execute(table);
            Logger.getLogger().log("ThumbStore.checkAndCreateTables() table created!");
            st = connexion.createStatement();
            String action = "CREATE UNIQUE INDEX path_index ON IMAGES(path)";
            Logger.getLogger().log("ThumbStore.checkAndCreateTables() creating Index on paths");
            st.execute(action);


            st = connexion.createStatement();
            action= "CREATE  INDEX md5_index ON IMAGES(md5)";
            Logger.getLogger().log("ThumbStore.checkAndCreateTables() creating Index on md5");
            st.execute(action);
        }
        //now we look for the path table
        tables = dbm.getTables(null, null, "PATHS", null);
        if (tables.next()) {
            Logger.getLogger().log("ThumbStore.checkAndCreateTables() table PATHS exists!");
            // Table exists
        } else {
            Logger.getLogger().log("ThumbStore.checkAndCreateTables() table PATHS does not exist, should create it");
            // Table does not exist
            String table = "CREATE TABLE PATHS(path varchar(256),  PRIMARY KEY ( path ))";
            Statement st = connexion.createStatement();
            st.execute(table);
            Logger.getLogger().log("ThumbStore.checkAndCreateTables() table created!");
        }
        //and  the version table
        tables = dbm.getTables(null, null, "VERSION", null);
        if (tables.next()) {
            Logger.getLogger().log("ThumbStore.checkAndCreateTables() table VERSION exists!");
            // Table exists
        } else {
            Logger.getLogger().log("ThumbStore.checkAndCreateTables() table VERSION does not exist, should create it");
            // Table does not exist
            String table = "CREATE TABLE VERSION(version int)";
            Statement st = connexion.createStatement();
            st.execute(table);
            table = "INSERT into VERSION VALUES(" + CURRENT_VERSION + ")";
            st.execute(table);
            Logger.getLogger().log("ThumbStore.checkAndCreateTables() table created with version 0");
        }




        // now we check for version number and decides if an upgrade is required
        String version = "SELECT * FROM VERSION";
       Statement st = connexion.createStatement();
        ResultSet res = st.executeQuery(version);
        int v = -1;
        while (res.next()) {
            v = res.getInt("version");
        }

        Logger.getLogger().log("Database version is " + v);
        upgradeDatabase(connexion, v);
    }

    private void checkOrAddColumns(DatabaseMetaData dbm) throws SQLException {
        //ALTER TABLE IMAGES DROP PRIMARY KEY ;
        //ALTER TABLE IMAGES ADD  id  BIGINT IDENTITY;
        //CREATE TABLE IMAGES_tmp(path varchar(256), size long, mtime long, md5 varchar(256), data blob,  lat double, lon double,  id  bigint identity(1,1))
        //INSERT INTO  IMAGES_TMP  (path,size,mtime,md5,data, lat, lon)  SELECT * from IMAGES
        //DROP table IMAGES
        //ALTER table images_tmp rename to IMAGES

        ResultSet rs = dbm.getColumns(null, null, "IMAGES", "LAT");
        if (!rs.next()) {
            //Column in table exist
            Logger.getLogger().log("Lat not found, updating table");
            Statement st = dbm.getConnection().createStatement();
            st.executeUpdate("ALTER TABLE IMAGES ADD lat double");

        }
        rs = dbm.getColumns(null, null, "IMAGES", "LON");
        if (!rs.next()) {
            Logger.getLogger().log("Lon not found, updating table");
            Statement st = dbm.getConnection().createStatement();
            st.executeUpdate("ALTER TABLE IMAGES ADD lon double");
        }
    }

    private void upgradeDatabase(Connection connection, int dbVersion) throws SQLException {
        Logger.getLogger().log("ThumbStore.upgradeDatabase started");

        if (dbVersion < CURRENT_VERSION) {
            if (dbVersion == 0) {
                upgradeToV1(connection);
                upgradeToV2(connection);
            }
            if (dbVersion == 1) {
                upgradeToV2(connection);
            }

            if (dbVersion == 2) {
                upgradeToV3(connection);
            }
            if (dbVersion == 3) {
                upgradeToV4(connection);
            }


            Statement st = connection.createStatement();
            String action = "Shutdown compact";
            st.execute(action);
            Logger.getLogger().log("ThumbStore.upgradeDatabase upgrade done please restart");
            System.exit(0);
        }
    }

    private void upgradeToV1(Connection connection) throws SQLException {
        //ok we need to upgrade the DB to the next version
        //CREATE TABLE IMAGES_tmp(path varchar(256), size long, mtime long, md5 varchar(256), data blob,  lat double, lon double,  id  bigint identity(1,1))
        //INSERT INTO  IMAGES_TMP  (path,size,mtime,md5,data, lat, lon)  SELECT * from IMAGES
        //DROP table IMAGES
        //ALTER table images_tmp rename to IMAGES
        Statement st = connection.createStatement();
        String action = "ALTER TABLE IMAGES DROP PRIMARY KEY ";
        st.execute(action);
        action = "CREATE TABLE IMAGES_tmp(id  bigint identity(1,1), path varchar(256), size long, mtime long, md5 varchar(256), data blob,  lat double, lon double)";
        st.execute(action);
        action = "INSERT INTO  IMAGES_TMP  (path,id, size,mtime,md5,data, lat, lon)  SELECT * from IMAGES";
        Logger.getLogger().log("ThumbStore.upgradeToV1 moving data to new table");
        st.execute(action);
        Logger.getLogger().log("ThumbStore.upgradeToV1 droping old table");

        action = "DROP table IMAGES";
        st.execute(action);
        action = "ALTER table images_tmp rename to IMAGES";
        st.execute(action);

        action = "UPDATE VERSION SET version=1 WHERE version=0";
        st.execute(action);
    }

    private void upgradeToV2(Connection connection) throws SQLException {
        //ok we need to upgrade the DB to the next version
        //This one includes an index for the path
        //CREATE INDEX index_name
        // ON table_name (column_name)
        Statement st = connection.createStatement();
        String action = "CREATE UNIQUE INDEX path_index ON IMAGES(path)";
        Logger.getLogger().log("ThumbStore.upgradeToV2 creating Index");
        st.execute(action);
        action = "UPDATE VERSION SET version=2 WHERE version=1";
        st.execute(action);
    }


    private void upgradeToV3(Connection connection) throws SQLException {
        //ok we need to upgrade the DB to the next version
        //This one includes an index for the path
        //CREATE INDEX md5_index
        // ON table_name (column_name)
        Statement st = connection.createStatement();
        String action = "CREATE  INDEX md5_index ON IMAGES(md5)";
        Logger.getLogger().log("ThumbStore.upgradeToV3 creating Index for MD5");
        st.execute(action);
        action = "UPDATE VERSION SET version=3 WHERE version=2";
        st.execute(action);
    }


    private void upgradeToV4(Connection connection) throws SQLException {
        Statement st = connection.createStatement();
        String action = "ALTER TABLE IMAGES ADD hash varchar(100)";
        Logger.getLogger().log("ThumbStore.upgradeToV4 creating column for hash");
        Logger.getLogger().log("ThumbStore.upgradeToV4 dropping column data");
        st.execute(action);

        action = " ALTER TABLE IMAGES DROP COLUMN data";
        st.execute(action);

        action = "UPDATE VERSION SET version=4 WHERE version=3";
        st.execute(action);
    }


    /**
     * Add the path to the list of indexed pathsOfDBOnDisk
     * An empty database is used or the first one found
     *
     * @param path
     */
    public void addIndexPath(String path) {
        Connection connexion = connexions.get(path);
        if (connexion != null) {
            return;
        }

        connexion = connexions.get("");
        if (connexion == null) {
            connexion = connexions.values().iterator().next();
        }
        connexions.put(path, connexion);
        System.err.println("ThumbStore.addIndexPath no db storing information for path " + path + "  found");
        System.err.println("ThumbStore.addIndexPath using " + connexion);

        try {
            PreparedStatement psmnt;
            psmnt = connexion.prepareStatement("insert into PATHS(path)" + "values(?)");
            psmnt.setString(1, path);
            psmnt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<String> getIndexedPaths(Connection connexion) {
        Statement sta;
        ResultSet res = null;
        ArrayList<String> paths = new ArrayList<String>();
        try {
            sta = connexion.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            res = sta.executeQuery("SELECT * FROM PATHS");
            while (res.next()) {
                String s = res.getString("path");
                paths.add(s);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return paths;
    }

    //TODO : process connexions.values() to eliminate duplicates

    public ArrayList<String> getIndexedPaths() {
        Statement sta;
        ResultSet res = null;
        ArrayList<String> paths = new ArrayList<String>();
        for (Connection connexion : getConnections()) {
            try {
                sta = connexion.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
                res = sta.executeQuery("SELECT * FROM PATHS");

                while (res.next()) {
                    String s = res.getString("path");
                    paths.add(s);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return paths;
    }


    /**
     * Return the connection to the DB currently responsible
     * for managing the root path of mediaFile
     *
     * @param mediaFile
     * @return
     */
    private Connection findResponsibleDB(String mediaFile) {

        Connection result = null;
        for (String s : connexions.keySet()) {
            if (mediaFile.contains(s)) {
                result = connexions.get(s);
                return result;
            }
        }
        return result;
    }

    /**
     * Save the descriptor to the db
     * DO NOT check that the key is not used
     *
     * @param id
     */
    public void saveToDB(MediaFileDescriptor id) {
        Logger.getLogger().log("MediaIndexer.generateAndSave "+ id);
        PreparedStatement psmnt;
        Connection connexion = findResponsibleDB(id.getPath());
        try {
            Statement st;
            psmnt = connexion.prepareStatement("insert into IMAGES(path, size, mtime, md5, hash, lat, lon) "
                    + "values(?,?,?,?,?,?,?)");
            psmnt.setString(1, id.getPath());
            psmnt.setLong(2, id.getSize());
            psmnt.setLong(3, id.getMtime());


            psmnt.setString(4, id.getMD5());
            psmnt.setString(5, id.getHash());
            psmnt.setDouble(6, id.getLat());
            psmnt.setDouble(7, id.getLon());
            psmnt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (preloadedDescriptorsExists()) {
            Logger.getLogger().log("MediaIndexer.generateAndSave Adding to preloaded descriptors " + id);
            //ts.getPreloadedDescriptors().add(id);
            id.setConnection(connexion);
            id.setId(getIndex(id.getPath()));
            this.getPreloadedDescriptors().add(id);
        }


    }

    public void updateToDB(MediaFileDescriptor id) {
        PreparedStatement psmnt = null;
        Connection connexion = findResponsibleDB(id.getPath());
        try {
            Statement st;
            psmnt = connexion
                    .prepareStatement("UPDATE IMAGES SET path=?, size=?, mtime=?, hash=?, md5=? , lat=?, lon=? WHERE path=? ");
            psmnt.setString(1, id.getPath());
            psmnt.setLong(2, id.getSize());
            psmnt.setLong(3, id.getMtime());

            psmnt.setString(4, id.getHash());
            psmnt.setString(5, id.getMD5());

            //System.err.println("ThumbStore.updateToDB lat : " + id.getLat());
            psmnt.setDouble(6, id.getLat());
            psmnt.setDouble(7, id.getLon());
            psmnt.setString(8, id.getPath());

            psmnt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (preloadedDescriptorsExists()) {
            id.setConnection(connexion);
            id.setId(getIndex(id.getPath()));
            //remove it from the descriptors
            getPreloadedDescriptors().remove(id);
            getPreloadedDescriptors().add(id);
        }

    }

    public int size() {
        int count = 0;
        for (Connection connexion : getConnections()) {

            String select = "SELECT COUNT(*) FROM IMAGES";

            Statement st;
            try {
                st = connexion.createStatement();
                ResultSet res = st.executeQuery(select);
                if (res.next()) {
                    count += res.getInt(1);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

        }
        return count;
    }


    public boolean isInDataBaseBasedOnName(String path) {
        boolean result = false;
        ResultSet res = getFromDatabase(path);
        if (res != null) {
            try {
                result = res.next();
            } catch (SQLException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
        return result;
    }


    public void deleteFromDatabase(String path) {

        System.err.println("ThumbStore.deleteFromDatabase " + path);
        //this.dump();
        MediaFileDescriptor mf = this.getMediaFileDescriptor(path);
        ResultSet res = this.getFromDatabase(path);
        try {
            while (res.next()) {
                res.deleteRow();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        //delete it from the cache
        if ((mf != null) && (this.preloadedDescriptorsExists())) {
            this.getPreloadedDescriptors().remove(mf);
        }
    }


    public int getIndex(String path) {
        ResultSet res = null;
        Connection connexion = findResponsibleDB(path);
        try {
            PreparedStatement psmnt = connexion.prepareStatement("SELECT * FROM IMAGES WHERE path=?", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            psmnt.setString(1, path);
            //		st = connexion.createStatement();
            psmnt.execute();
            res = psmnt.getResultSet();
            res.next();
            return res.getInt("id");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public ResultSet getFromDatabase(String path) {
        ResultSet res = null;
        Connection connexion = findResponsibleDB(path);
        try {
            PreparedStatement psmnt = connexion.prepareStatement("SELECT * FROM IMAGES WHERE path=?", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            psmnt.setString(1, path);
            //		st = connexion.createStatement();
            psmnt.execute();
            res = psmnt.getResultSet();

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return res;
    }


   public ResultSet getFromDatabase(int index) {
       ResultSet res = null;
       //TODO: Fix for multiple connections
       Connection connexion = (Connection) connexions.values().toArray()[0];
       try {
           PreparedStatement psmnt = connexion.prepareStatement("SELECT * FROM IMAGES WHERE id=?", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
           psmnt.setInt(1, index);
           //		st = connexion.createStatement();
           psmnt.execute();
           res = psmnt.getResultSet();

       } catch (SQLException e) {
           e.printStackTrace();
       }
       return res;
   }


    public long getMTime(String path) {

        ResultSet res = null;
        Connection connexion = findResponsibleDB(path);
        //    res.getLong("mtime");
        try {
            PreparedStatement psmnt = connexion.prepareStatement("SELECT * FROM IMAGES WHERE path=?");
            psmnt.setString(1, path);
            //		st = connexion.createStatement();
            psmnt.execute();
            res = psmnt.getResultSet();
            res.next();
            return res.getLong("mtime");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }


    public ArrayList<String> getAllWithGPS() {
        Statement sta;
        ResultSet res = null;
        ArrayList<String> al = new ArrayList<String>();
        for (Connection connexion : getConnections()) {
            try {
                sta = connexion.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
                res = sta.executeQuery("SELECT * FROM IMAGES WHERE lat <> 0 OR lon <>0");

                while (res.next()) {
                    // System.err.println("getAllWithGPS adding  " + res.getString("path"));
                    al.add(res.getString("path").replaceAll("\\\\", "\\\\\\\\"));
                }


            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return al;
    }

    public MultipleResultSet getAllInDataBases() {
        MultipleResultSet mrs = new MultipleResultSet();
        // ArrayList<ResultSet> res = new ArrayList<ResultSet>();
        for (Connection connection : getConnections()) {
            mrs.add(connection, this.getAllInDataBase(connection));
        }
        return mrs;
    }


    public ResultSet getAllInDataBase(Connection connexion) {
        Statement sta;
        long t0 = System.currentTimeMillis();
        try {
            sta = connexion.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            return sta.executeQuery("SELECT * FROM IMAGES");
//            return sta.executeQuery("SELECT path,data,id,md5,size,COUNT(md5) FROM IMAGES GROUP BY path,md5 HAVING ( COUNT(md5) > 1 )");

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            long t1 = System.currentTimeMillis();
            System.err.println("ThumbStore.getAllInDataBase took " + (t1 - t0) + " ms");
        }
        return null;
    }

    public ArrayList<MediaFileDescriptor> getFromDB(String filter, boolean gps) {
        ArrayList<MediaFileDescriptor> list = new ArrayList<MediaFileDescriptor>();

        String query = null;
        if (!gps) {
            query = "SELECT * FROM IMAGES WHERE LCASE(path) LIKE LCASE(\'%" + filter + "%\')";
        } else {
            query = "SELECT * FROM IMAGES WHERE (LCASE(path) LIKE LCASE(\'%" + filter + "%\') " +
                    "AND  (lat <> 0 OR lon <>0))";
        }
        //   MultipleResultSet mrs = new MultipleResultSet();
        for (Connection connection : getConnections()) {
            //mrs.add(connection, this.getAllInDataBase(connection));
            Statement sta;
            try {
                sta = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
                ResultSet res = sta.executeQuery(query);
                // mrs.add(connection,r);
                while (res.next()) {
                    list.add(getCurrentMediaFileDescriptor(res));
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return list;
    }


    public String getPath(int[] data) {
        Statement sta;
        ResultSet res = null;
        String p = null;
        for (Connection connexion : getConnections()) {
            try {
                sta = connexion.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);


                PreparedStatement psmnt;
                psmnt = connexion.prepareStatement("SELECT path from IMAGES WHERE data=(?)");


                psmnt.setBytes(1, Utils.toByteArray(data));
                psmnt.execute();
                res = psmnt.getResultSet();

                while (res.next()) {
                    p = res.getString("path");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return p;
    }

    public static String getPath(Connection c, int index) {
        // Statement sta;
        ResultSet res = null;
        String p = null;


        if (c == null) {
            Logger.getLogger().err("Thumbstore : Connection is NULL ");
            return null;
        }
        try {
            PreparedStatement psmnt;

            psmnt = c.prepareStatement("SELECT path from IMAGES WHERE id=(?)");
            psmnt.setInt(1, index);
            psmnt.execute();
            res = psmnt.getResultSet();
            while (res.next()) {
                p = res.getString("path");
            }
        } catch (SQLException e) {
            System.err.println("Cannot find image with index " + index);
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return p;
    }


    public ArrayList<MediaFileDescriptor> getDuplicatesMD5(MediaFileDescriptor mfd) {
        Statement sta;
        ResultSet res = null;
        ArrayList<MediaFileDescriptor> results = new ArrayList<MediaFileDescriptor>();
        for (Connection connexion : getConnections()) {
            try {
                sta = connexion.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
                res = sta
                        .executeQuery("SELECT path, md5, size from IMAGES WHERE md5=\'" + mfd.getMD5() + "\'");
                while (res.next()) {
                    results.add(getCurrentMediaFileDescriptor(res));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return results;
    }

    /**
     * remove incorrect records from the DB
     */
    public void fix() {
        ArrayList<ResultSet> results = this.getAllInDataBases().getResultSets();
        MediaFileDescriptor id = null;
        System.err.println("ThumbStore.fix() BD has " + this.size() + " entries");
        for (ResultSet all : results) {
            try {
                while (all.next()) {
                    id = getCurrentMediaFileDescriptor(all);
                    if (Utils.isValideImageName(id.path)) {
                        if (id.getHash() == null || id.getMD5() == null) {
                            System.err.println("ThumbStore.fix() " + id.getPath() + " has null data ord md5");
                            all.deleteRow();
                        }
                    }
                    if (Utils.isValideVideoName(id.path)) {
                        if (id.getMD5() == null) {
                            System.err.println("ThumbStore.fix() " + id.getPath() + " has null md5");
                            all.deleteRow();
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * remove outdated records from the DB An outdated record is one which has
     * no corresponding file on the FS
     */
    public void shrink() {
        this.shrink(this.getIndexedPaths());
    }

    public void shrink(List<String> paths) {
        if (Logger.getLogger().isEnabled()) {
            Logger.getLogger().log("ThumbStore.shrink() BD has " + this.size() + " entries");
        }
        for (String path : paths) {
            Logger.getLogger().log("ThumbStore.shrink() processing path " + path);
            ResultSet all = this.getAllInDataBase(connexions.get(path));
            MediaFileDescriptor id = null;

            try {
                int i = 0;
                while (all.next()) {
                    id = getCurrentMediaFileDescriptor(all);
                    File tmp = new File(id.getPath());
                    if (!tmp.exists()) {
                        i++;
                        all.deleteRow();
                    }
                }
                System.err.println("ThumbStore.shrink() has deleted  " + i + " records");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

    }


    public MediaFileDescriptor getCurrentMediaFileDescriptor(ResultSet res) {
        MediaFileDescriptor id = null;
        try {
            // if (res.next()) {
            // id = new ImageDescriptor();
            String path = res.getString("path");
            //  byte[] d = res.getBytes("data");
            // int[] idata = Utils.toIntArray(d);
            String md5 = res.getString("md5");
            long mtime = res.getLong("mtime");
            long size = res.getLong("size");
            String hash = res.getString("hash");

            id = new MediaFileDescriptor(path, size, mtime, md5, hash);
            id.setId(res.getInt("id"));
            // }
        } catch (SQLException e) {
           // e.printStackTrace();
            //not in DB
        }
        return id;
    }

    public MediaFileDescriptor getMediaFileDescriptor(String path) {
        ResultSet res = getFromDatabase(path);
        try {
            res.next();
            return this.getCurrentMediaFileDescriptor(res);
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return null;
    }

    public MediaFileDescriptor getMediaFileDescriptor(int index) {
        ResultSet res = getFromDatabase(index);
        try {
            res.next();
            return this.getCurrentMediaFileDescriptor(res);
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return null;
    }





    public void displayImage(BufferedImage bf) {
        Graphics2D gg = bf.createGraphics();
        gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        gg.drawImage(bf, 0, 0, null);
        // drawFeatures(gg, surf, id);
        JFrame frame = null;
        // display results
        if (frame == null) {
            frame = new JFrame();
            final JLabel label = new JLabel(new ImageIcon(bf));
            frame.add(label);
        }
        frame.pack();
        frame.setVisible(true);
    }

    public void test() {
        System.err.println("ThumbStore.test() reading descriptor from disk ");
        MediaIndexer tg = new MediaIndexer(this);
        String s = "/user/fhuet/desktop/home/workspaces/rechercheefficaceimagessimilaires/images/test.jpg";
        MediaFileDescriptor id = tg.buildMediaDescriptor(new File(s));
        System.err.println("ThumbStore.test() writting to database");
        // id.data=null;
        // id.mtime=0;
        saveToDB(id);
        // updateToDB(id);
        System.err.println("ThumbStore.test() dumping entries");
        String select = "SELECT * FROM IMAGES";
        Statement st;
        for (Connection connexion : getConnections()) {
            try {
                st = connexion.createStatement();

                ResultSet res = st.executeQuery(select);
                while (res.next()) {
                    String i = res.getString("path");
                    //  byte[] d = res.getBytes("data");
                    System.err.println(i + " has mtime " + res.getLong("mtime"));
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        System.err.println("Testing update ");
        // id.data = null;
        id.mtime = 0;
        updateToDB(id);
        System.err.println("ThumbStore.test() dumping entries");

    }

    public void dump(boolean p) {
        String select = "SELECT path,id,hash FROM IMAGES";
        Statement st;
        for (Connection connexion : getConnections()) {
            try {
                st = connexion.createStatement();
                ResultSet res = st.executeQuery(select);
                while (res.next()) {
                    String path = res.getString("path");
                    int i = res.getInt("id");
                    // byte[] d = res.getBytes("data");
                    String s = res.getString("hash");
                    if (s != null) {
//                        String data = null;
//                        if ((data = Utils.byteArrayToBase64Img(d)) != null) {
                        if (p) {
                            System.out.println(path + "," + s);
                        } else {
                            System.out.println(i + "," + s);
                        }

//                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }


    public ArrayList<String> getPath() {
        return this.pathsOfDBOnDisk;
    }


    public PreloadedDescriptors getPreloadedDescriptors() {
        if (preloadedDescriptors == null) {
            long ti = System.currentTimeMillis();
            Status.getStatus().setStringStatus("Building descriptors list");
            int dbSize = size();
            ProgressBar pb = new ProgressBar(0, dbSize, dbSize / 100);
            preloadedDescriptors = new PreloadedDescriptors(dbSize, new Comparator<MediaFileDescriptor>() {
                public int compare(MediaFileDescriptor o1, MediaFileDescriptor o2) {
                    return o1.getMD5().compareTo(o2.getMD5());
                }
            });
            int increment = dbSize / 100;
            int processed = 0;
            int processedSinceLastTick = 0;

            MultipleResultSet mrs = getAllInDataBases();
            ArrayList<ResultSet> ares = mrs.getResultSets();
            ArrayList<Connection> connections = mrs.getConnections();
            int currentConnection = 0;
            for (ResultSet res : ares) {
                try {
                    Connection c = connections.get(currentConnection);
                    while (res.next()) {
                        processed++;
                        processedSinceLastTick++;

                        if (processedSinceLastTick >= increment) {
                            pb.tick(processed);
                            Status.getStatus().setStringStatus("Building descriptors list  " + pb.getPercent() + "%");
                            processedSinceLastTick = 0;
                        }
//                        }
                        String path = null;
                        path = res.getString("path");
                        int id = res.getInt("id");
                        String md5 = res.getString("md5");
                        long size = res.getLong("size");
                        String hash = res.getString("hash");
                        if (path != null && md5 != null) {
                            MediaFileDescriptor imd = new MediaFileDescriptor();
                            if (SimilarImageFinder.USE_FULL_PATH) {
                                imd.setPath(path);
                            }
                            imd.setId(id);
                            imd.setHash(hash);
                            imd.setSize(size);
                            imd.setMd5Digest(md5);
                            imd.setConnection(c);
                            preloadedDescriptors.add(imd);
                        } else {
                            //  System.err.println("Thumbstore: something is wrong with data " + path);
                            //TODO : we should clean the data here
                        }
                    }
                    //  }
                    currentConnection++;
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            System.err.println("ThumbStore.getPreloadedDescriptors sorting  " + preloadedDescriptors.size() + " data");
            long t0 = System.currentTimeMillis();
            preloadedDescriptors.sort();
            //Collections.sort(preloadedDescriptors, );
            long t1 = System.currentTimeMillis();
            System.err.println("ThumbStore.getPreloadedDescriptors sorting data .... done after " + (t1 - t0));
            Status.getStatus().setStringStatus(Status.IDLE);
            System.err.println("ThumbStore.getPreloadedDescriptors all done  " + (t1 - ti));
        }
        return preloadedDescriptors;
    }

    /**
     * return LSH status
     * [0] = LSH size
     * [1] = number of candidates for last query
     * @return
     */
    public int[] getLSHStatus() {
        if (lsh==null) {
             buildLSH();
        }
        return new int[] {lsh.size(), lsh.lastCandidatesCount()}     ;

    }

    public ArrayList<MediaFileDescriptor> findCandidatesUsingLSH(MediaFileDescriptor id) {
        if (lsh==null) {
            buildLSH();
        }
        List<Integer> result = lsh.lookupCandidates(id.getHash());
        System.out.println("Found " + result.size() + " candidates out of " + lsh.size());

         ArrayList<MediaFileDescriptor> al = new ArrayList<MediaFileDescriptor>(result.size());
          //TODO : Fix for multiple connections
        //assume only one connection
        for(Integer i : result) {
            MediaFileDescriptor tmp = getMediaFileDescriptor(i);
            if (tmp!=null) {
              al.add(tmp);
            }
        }
        return al;
    }

    private void buildLSH() {
        Status.getStatus().setStringStatus("Teaching LSH");
        lsh = new LSH(10,15,100);
        ArrayList<ResultSet> ares = this.getAllInDataBases().getResultSets();
        for (ResultSet res : ares) {
            try {
                while (res.next()) {
                    // String path = res.getString("path");
                    int index = res.getInt("ID");
                    //  byte[] d = res.getBytes("data");
                    String s = res.getString("hash");
                    if (s != null) {
                        lsh.add(s, index);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
        Status.getStatus().setStringStatus(Status.IDLE);
    }

    public boolean preloadedDescriptorsExists() {
        return (this.preloadedDescriptors != null);
    }


    public void flushPreloadedDescriptors() {
        if (this.preloadedDescriptors != null) {
            this.preloadedDescriptors.clear();
            this.preloadedDescriptors = null;
        }
    }
}

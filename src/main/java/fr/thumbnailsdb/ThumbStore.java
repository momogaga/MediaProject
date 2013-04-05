package fr.thumbnailsdb;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.*;
import java.util.*;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

public class ThumbStore {

    protected static String DEFAULT_DB = "localDB";
    protected static int CURRENT_VERSION = 2;


    protected HashMap<String, Connection> connexions = new HashMap<String, Connection>();
    protected ArrayList<String> pathsOfDBOnDisk = new ArrayList<String>();


    public ThumbStore() {
        this(DEFAULT_DB);

    }

    public ThumbStore(String path) {
        if (path == null) {
            path = DEFAULT_DB;
        }
        System.out.println("ThumbStore.ThumbStore() using " + path + " as DB");

        this.addDB(path);
    }


    public void addDB(String path) {
        this.pathsOfDBOnDisk.add(path);
        try {
            Connection c = connectToDB(path);
            checkAndCreateTables(c);
            ArrayList<String> paths = getIndexedPaths(c);
            if (paths.size() == 0) {
                System.out.println("ThumbStore.addDB found empty db");
                connexions.put("", c);
            } else {
                for (String s : paths) {
                    System.out.println("ThumbStore.addDB path : " + s + " with db : " + c);
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
        // System.out.println("ThumbStore.connectToDB() " + connexion.);
    }

    public void checkAndCreateTables(Connection connexion) throws SQLException {

        DatabaseMetaData dbm = connexion.getMetaData();
        // check if "employee" table is there
        ResultSet tables = dbm.getTables(null, null, "IMAGES", null);

        if (tables.next()) {
            System.out.println("ThumbStore.checkAndCreateTables() table IMAGES exists!");
            checkOrAddColumns(dbm);
            // Table exists
        } else {
            System.out.println("ThumbStore.checkAndCreateTables() table IMAGES does not exist, should create it");
            String table = "CREATE TABLE IMAGES(id  bigint identity(1,1),path varchar(256), size long, mtime long, md5 varchar(256), data blob,  lat double, lon double)";
            Statement st = connexion.createStatement();
            st.execute(table);
            System.out.println("ThumbStore.checkAndCreateTables() table created!");
        }
        //now we look for the path table
        tables = dbm.getTables(null, null, "PATHS", null);
        if (tables.next()) {
            System.out.println("ThumbStore.checkAndCreateTables() table PATHS exists!");
            // Table exists
        } else {
            System.out.println("ThumbStore.checkAndCreateTables() table PATHS does not exist, should create it");
            // Table does not exist
            String table = "CREATE TABLE PATHS(path varchar(256),  PRIMARY KEY ( path ))";
            Statement st = connexion.createStatement();
            st.execute(table);
            System.out.println("ThumbStore.checkAndCreateTables() table created!");
        }
        //and  the version table
        tables = dbm.getTables(null, null, "VERSION", null);
        if (tables.next()) {
            System.out.println("ThumbStore.checkAndCreateTables() table VERSION exists!");
            // Table exists
        } else {
            System.out.println("ThumbStore.checkAndCreateTables() table VERSION does not exist, should create it");
            // Table does not exist
            String table = "CREATE TABLE VERSION(version int)";
            Statement st = connexion.createStatement();
            st.execute(table);
            table = "INSERT into VERSION VALUES(0)";
            st.execute(table);
            System.out.println("ThumbStore.checkAndCreateTables() table created with version 0");
        }

        // now we check for version number and decides if an upgrade is required
        String version = "SELECT * FROM VERSION";
        Statement st = connexion.createStatement();
        ResultSet res = st.executeQuery(version);
        int v = -1;
        while (res.next()) {
            v = res.getInt("version");
        }

        System.out.println("Database version is " + v);
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
            System.out.println("Lat not found, updating table");
            Statement st = dbm.getConnection().createStatement();
            st.executeUpdate("ALTER TABLE IMAGES ADD lat double");

        }
        rs = dbm.getColumns(null, null, "IMAGES", "LON");
        if (!rs.next()) {
            System.out.println("Lon not found, updating table");
            Statement st = dbm.getConnection().createStatement();
            st.executeUpdate("ALTER TABLE IMAGES ADD lon double");
        }
    }

    private void upgradeDatabase(Connection connection, int dbVersion) throws SQLException {
        System.out.println("ThumbStore.upgradeDatabase started");

        if (dbVersion < CURRENT_VERSION) {
            if (dbVersion == 0) {
                upgradeToV1(connection);
                upgradeToV2(connection);


            }
            if (dbVersion == 1) {
               // upgradeToV1(connection);
                upgradeToV2(connection);
            }


            Statement st = connection.createStatement();
            String action = "Shutdown compact";
            st.execute(action);
            System.out.println("ThumbStore.upgradeDatabase upgrade done please restart");
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
        action = "INSERT INTO  IMAGES_TMP  (path,size,mtime,md5,data, lat, lon)  SELECT * from IMAGES";
        System.out.println("ThumbStore.upgradeToV1 moving data to new table");
        st.execute(action);
        System.out.println("ThumbStore.upgradeToV1 droping old table");

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
        System.out.println("ThumbStore.upgradeToV2 creating Index");
        st.execute(action);

        action = "UPDATE VERSION SET version=2 WHERE version=1";
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
        connexions.put(path,connexion);
        System.out.println("ThumbStore.addIndexPath no db storing information for path " + path + "  found");
        System.out.println("ThumbStore.addIndexPath using " + connexion);

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
                //   System.out.println("getIndexedPaths(connexion) path found " + s);

            }
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
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
                    //       System.out.println("getIndexedPaths() path found " + s);
                }
            } catch (SQLException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
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
                //    System.out.println("ThumbStore.findResponsibleDB found " + result);
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
        PreparedStatement psmnt;
        Connection connexion = findResponsibleDB(id.getPath());
        try {
            Statement st;
            psmnt = connexion.prepareStatement("insert into IMAGES(path, size, mtime, md5, data, lat, lon) "
                    + "values(?,?,?,?,?,?,?)");
            psmnt.setString(1, id.getPath());
            psmnt.setLong(2, id.getSize());
            psmnt.setLong(3, id.getMtime());

            // convert the int[] array to byte[] array
            ByteArrayOutputStream ba = new ByteArrayOutputStream();
            ObjectOutputStream oi = new ObjectOutputStream(ba);
            oi.writeObject(id.getData());
            oi.close();

            psmnt.setString(4, id.getMD5());
            psmnt.setBytes(5, ba.toByteArray());
            psmnt.setDouble(6, id.getLat());
            psmnt.setDouble(7, id.getLon());
            psmnt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void updateToDB(MediaFileDescriptor id) {
        PreparedStatement psmnt;
        Connection connexion = findResponsibleDB(id.getPath());
        try {
            Statement st;
            psmnt = connexion
                    .prepareStatement("UPDATE IMAGES SET path=?, size=?, mtime=?, data=?, md5=? , lat=?, lon=? WHERE path=? ");
            psmnt.setString(1, id.getPath());
            psmnt.setLong(2, id.getSize());
            psmnt.setLong(3, id.getMtime());

            psmnt.setBytes(4, id.getDataAsByte());
            psmnt.setString(5, id.getMD5());

            System.out.println("ThumbStore.updateToDB lat : " + id.getLat());
            psmnt.setDouble(6, id.getLat());
            psmnt.setDouble(7, id.getLon());
            psmnt.setString(8, id.getPath());

            psmnt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
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
        ResultSet res = get(path);
        if (res != null) {
            try {
                result = res.next();
            } catch (SQLException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
        return result;
    }

    private ResultSet get(String path) {
        ResultSet res = null;
        Connection connexion = findResponsibleDB(path);
        try {
            PreparedStatement psmnt = connexion.prepareStatement("SELECT * FROM IMAGES WHERE path=?");
            psmnt.setString(1, path);
            //		st = connexion.createStatement();
            psmnt.execute();
            res = psmnt.getResultSet();

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return res;
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
                    // System.out.println("getAllWithGPS adding  " + res.getString("path"));
                    al.add(res.getString("path").replaceAll("\\\\", "\\\\\\\\"));
                }


            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return al;
    }

    public ArrayList<ResultSet> getAllInDataBase() {
        Statement sta;
        ArrayList<ResultSet> res = new ArrayList<ResultSet>();
        for (Connection connexion : getConnections()) {
            res.add(this.getAllInDataBase(connexion));
        }
        return res;
    }

    public ResultSet getAllInDataBase(Connection connexion) {
        Statement sta;
        try {
            sta = connexion.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            return sta.executeQuery("SELECT * FROM IMAGES");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * TODO : Make a custom result set which encapsulates the real resultSets.
     * When queyried, simply return the next result with the min MD5 value
     *
     * @return
     */
    public MD5OrderedMultiResultSet getOrderedByMD5() {

        Statement sta;
        ResultSet res = null;
        MD5OrderedMultiResultSet mom = new MD5OrderedMultiResultSet();
        for (Connection connexion : getConnections()) {
            try {
                sta = connexion.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
//			res = sta
//					.executeQuery("SELECT DISTINCT A.path, A.size, A.md5 from images A JOIN ( SELECT COUNT(*) as Count, B.md5   FROM Images B   GROUP BY B.md5) AS B ON A.md5 = B.md5 WHERE B.Count > 1 ORDER by A.md5;");
                res = sta
                        .executeQuery("SELECT path, md5, size from IMAGES order by md5;");
                mom.addResultSet(res);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return mom;
    }


    public ArrayList<MediaFileDescriptor> getMFDOrderedByMD5() {

        Statement sta;
        ResultSet res = null;
        ArrayList<MediaFileDescriptor> mf = new ArrayList<MediaFileDescriptor>();
        //  MD5OrderedMultiResultSet mom = new MD5OrderedMultiResultSet();
        for (Connection connexion : getConnections()) {
            try {
                sta = connexion.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
//			res = sta
//					.executeQuery("SELECT DISTINCT A.path, A.size, A.md5 from images A JOIN ( SELECT COUNT(*) as Count, B.md5   FROM Images B   GROUP BY B.md5) AS B ON A.md5 = B.md5 WHERE B.Count > 1 ORDER by A.md5;");
                res = sta
                        .executeQuery("SELECT path, md5, size from IMAGES order by md5;");
                //   mom.addResultSet(res);

                while (res.next()) {
                    String path = res.getString("path");

                    String md5 = res.getString("md5");
                    long size = res.getLong("size");
                    if (md5 != null) {
                        mf.add(new MediaFileDescriptor(path, size, 0, null, md5));
                    }
                    // }
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        System.out.println("ThumbStore.getMFDOrderedByMD5 sorting  " + mf.size() + " data");
        long t0 = System.currentTimeMillis();
        Collections.sort(mf, new Comparator<MediaFileDescriptor>() {
            @Override
            public int compare(MediaFileDescriptor o1, MediaFileDescriptor o2) {
//                System.out.println("ThumbStore.compare comparing " + o1.path + " " + o1.md5Digest);
//                System.out.println("ThumbStore.compare  to " + o2.path + " " + o2.md5Digest);
                return o1.getMD5().compareTo(o2.getMD5());
            }
        });
        long t1 = System.currentTimeMillis();

        System.out.println("ThumbStore.getMFDOrderedByMD5 sorting data .... done after " + (t1 - t0));

        return mf;
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

                // convert the int[] array to byte[] array
                ByteArrayOutputStream ba = new ByteArrayOutputStream();
                ObjectOutputStream oi = new ObjectOutputStream(ba);
                oi.writeObject(data);
                oi.close();

                psmnt.setBytes(1, ba.toByteArray());
                psmnt.execute();
                res = psmnt.getResultSet();

                while (res.next()) {
                    p = res.getString("path");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
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
        ArrayList<ResultSet> results = this.getAllInDataBase();
        MediaFileDescriptor id = null;
        System.out.println("ThumbStore.fix() BD has " + this.size() + " entries");
        for (ResultSet all : results) {
            try {
                while (all.next()) {
                    id = getCurrentMediaFileDescriptor(all);
                    if (Utils.isValideImageName(id.path)) {
                        if (id.getData() == null || id.getMD5() == null) {
                            System.out.println("ThumbStore.fix() " + id.getPath() + " has null data ord md5");
                            all.deleteRow();
                        }
                    }
                    if (Utils.isValideVideoName(id.path)) {
                        if (id.getMD5() == null) {
                            System.out.println("ThumbStore.fix() " + id.getPath() + " has null md5");
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
        System.out.println("ThumbStore.shrink() BD has " + this.size() + " entries");
        for (String path : paths) {
            System.out.println("ThumbStore.shrink() processing path " + path);
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
                System.out.println("ThumbStore.shrink() has deleted  " + i + " records");
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
            byte[] d = res.getBytes("data");
            int[] idata = null;
            if (d != null) {
                ObjectInputStream oi = new ObjectInputStream(new ByteArrayInputStream(d));
                idata = (int[]) oi.readObject();
            } else {
                System.err.println("xxxx");
            }
            String md5 = res.getString("md5");
            long mtime = res.getLong("mtime");
            long size = res.getLong("size");
            id = new MediaFileDescriptor(path, size, mtime, idata, md5);
            // }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block             get
            e.printStackTrace();
        }
        return id;
    }

    public MediaFileDescriptor getMediaFileDescriptor(String path) {
        MediaFileDescriptor id = null;
        try {
            // System.out.println("path is " + path);
            ResultSet res = get(path);
            res.next();
//            ResultSetMetaData md = res.getMetaData();
//            int col = md.getColumnCount();
//            System.out.println("Number of Column : "+ col);
//            System.out.println("Columns Name: ");
//            for (int i = 1; i <= col; i++){
//                String col_name = md.getColumnName(i);
//                System.out.println(col_name);
//            }


            // return getCurrentMediaFileDescriptor(res);

            // if (res.next()) {
            // id = new ImageDescriptor();
            // String path = res.getString("path");
            byte[] d = res.getBytes("data");
            int[] idata = null;
            if (d != null) {
                ObjectInputStream oi = new ObjectInputStream(new ByteArrayInputStream(d));
                idata = (int[]) oi.readObject();
            } else {
                System.err.println("xxxx");
            }
            String md5 = res.getString("md5");
            long mtime = res.getLong("mtime");
            long size = res.getLong("size");
            id = new MediaFileDescriptor(path, size, mtime, idata, md5);
            // }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return id;
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
        System.out.println("ThumbStore.test() reading descriptor from disk ");
        MediaIndexer tg = new MediaIndexer(this);
        String s = "/user/fhuet/desktop/home/workspaces/rechercheefficaceimagessimilaires/images/test.jpg";
        MediaFileDescriptor id = tg.buildMediaDescriptor(new File(s));
        System.out.println("ThumbStore.test() writting to database");
        // id.data=null;
        // id.mtime=0;
        saveToDB(id);
        // updateToDB(id);
        System.out.println("ThumbStore.test() dumping entries");
        String select = "SELECT * FROM IMAGES";
        Statement st;
        for (Connection connexion : getConnections()) {
            try {
                st = connexion.createStatement();

                ResultSet res = st.executeQuery(select);
                while (res.next()) {
                    String i = res.getString("path");
                    byte[] d = res.getBytes("data");
                    System.out.println(i + " has data " + d + " and mtime " + res.getLong("mtime"));
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        System.out.println("Testing update ");
        id.data = null;
        id.mtime = 0;
        updateToDB(id);
        System.out.println("ThumbStore.test() dumping entries");

//        try {
//            st = connexion.createStatement();
//
//            ResultSet res = st.executeQuery(select);
//            while (res.next()) {
//                String i = res.getString("path");
//                byte[] d = res.getBytes("data");
//                System.out.println(i + " has data " + d + " and mtime " + res.getLong("mtime"));
//            }
//
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
    }

    public void testDuplicate() {
        System.out.println("ThumbStore.testDuplicate()");
        ResultSet rs = getOrderedByMD5();
        try {
            while (rs.next()) {
                System.out.println(rs.getLong("size") + "  " + rs.getString("path"));
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public ArrayList<String> getPath() {
        return this.pathsOfDBOnDisk;
//        try {
//            return new File(this.path).getCanonicalPath();
//        } catch (IOException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//        return this.path;
    }


    public void test2() {
//        String query = "SELECT path FROM Images\n" +
//                "where path like '%Sabine%'";
//        Statement st;
//        try {
//            st = connexion.createStatement();
//
//            ResultSet res = st.executeQuery(query);
//            while (res.next()) {
//                String i = res.getString("path");
//                // byte[] d = res.getBytes("data");
//                System.out.println("-" + i + "-");
//            }
//
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
    }

    public static void main(String[] args) {

        ThumbStore ts = new ThumbStore("localDB");

//        ts.test();
//        ts.testDuplicate();
//        ArrayList<String> al = ts.getAllWithGPS();
//        for (Iterator<String> iterator = al.iterator(); iterator.hasNext(); ) {
//            String next = iterator.next();
//            System.out.println(next);
//        }
        ts.test2();

    }

}

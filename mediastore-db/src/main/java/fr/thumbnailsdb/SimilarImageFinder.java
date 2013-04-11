package fr.thumbnailsdb;

import fr.thumbnailsdb.bktree.BKTree;
import fr.thumbnailsdb.bktree.RMSEDistance;
import fr.thumbnailsdb.vptree.VPTree;
import fr.thumbnailsdb.vptree.VPTreeBuilder;
import fr.thumbnailsdb.vptree.VPTreeSeeker;
import fr.thumbnailsdb.vptree.distances.VPRMSEDistance;

import java.io.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class SimilarImageFinder {

    protected ThumbStore thumbstore;

    protected ArrayList<MediaFileDescriptor> preloadedDescriptors;

    protected BKTree<MediaFileDescriptor> bkTree;// = new BKTree<String>(new RMSEDistance());

    protected VPTree vpTree;

    public SimilarImageFinder(ThumbStore c) {
        this.thumbstore = c;
    }

    public TreeSet<MediaFileDescriptor> findSimilarMedia(String source, int max) {
        MediaIndexer tg = new MediaIndexer(null);
        MediaFileDescriptor id = tg.buildMediaDescriptor(new File(source));
        return this.findSimilarImage(id, max);
    }


    protected BKTree<MediaFileDescriptor> getPreloadedDescriptorsBKTree() {
        if (bkTree == null) {
            int size = thumbstore.size();
            bkTree = new BKTree<MediaFileDescriptor>(new RMSEDistance());
            ArrayList<ResultSet> ares = thumbstore.getAllInDataBase();
            for (ResultSet res : ares) {
                try {
                    while (res.next()) {
                        String path = res.getString("path");
                        byte[] d = res.getBytes("data");
                        if (d != null) {
                            ObjectInputStream oi = new ObjectInputStream(new ByteArrayInputStream(d));
                            int[] idata = (int[]) oi.readObject();
                            if (idata != null) {

                                MediaFileDescriptor imd = new MediaFileDescriptor();
                                imd.setPath(path);
                                imd.setData(idata);
                                bkTree.add(imd);
                            }
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                } catch (IOException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
        }
        System.out.println("SimilarImageFinder.getPreloadedDescriptors records in BKTree : " + bkTree.size());
        return bkTree;
    }

    protected VPTree getPreloadedDescriptorsVPTree() {
        if (vpTree == null) {
            int size = thumbstore.size();
            vpTree = new VPTree();
            VPTreeBuilder builder = new VPTreeBuilder(new VPRMSEDistance());
            ArrayList<MediaFileDescriptor> al = new ArrayList<MediaFileDescriptor>(size);

            ArrayList<ResultSet> ares = thumbstore.getAllInDataBase();
            for (ResultSet res : ares) {
                try {
                    while (res.next()) {
                        String path = res.getString("path");
                        byte[] d = res.getBytes("data");
                        if (d != null) {
                            ObjectInputStream oi = new ObjectInputStream(new ByteArrayInputStream(d));
                            int[] idata = (int[]) oi.readObject();
                            if (idata != null) {

                                MediaFileDescriptor imd = new MediaFileDescriptor();
                                imd.setPath(path);
                                imd.setData(idata);
                                al.add(imd);
                            }
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                } catch (IOException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
            System.out.println("SimilarImageFinder.getPreloadedDescriptors array list built , creating tree");
            vpTree = builder.buildVPTree(al);

        }


        System.out.println("SimilarImageFinder.getPreloadedDescriptors records in VPTree : " + vpTree);
        return vpTree;


    }

    protected ArrayList<MediaFileDescriptor> getPreloadedDescriptors() {
        if (preloadedDescriptors == null) {
            Status.getStatus().setStringStatus("Building descriptors list");
            ProgressBar pb = new ProgressBar();
            int size = thumbstore.size();
            preloadedDescriptors = new ArrayList<MediaFileDescriptor>(size);
            int increment = size / 20;
            int i = 0;
            int step = 0;
            if (increment == 0) {
                increment = 1;
            }

            ArrayList<ResultSet> ares = thumbstore.getAllInDataBase();
            for (ResultSet res : ares) {
                try {
                    while (res.next()) {
                        i++;
                        if (i > increment) {
                            i = 0;
                            step++;
                            if (pb != null) {
                                pb.update(step, 20);
                            }
                            Status.getStatus().setStringStatus("Building descriptors list " + ((step + 1) * 5) + "%");
                        }
                        String path = res.getString("path");
                        byte[] d = res.getBytes("data");
                        int id = res.getInt("id");
                        if (d != null) {
                            ObjectInputStream oi = new ObjectInputStream(new ByteArrayInputStream(d));
                            int[] idata = (int[]) oi.readObject();
                            if (idata != null) {
                                MediaFileDescriptor imd = new MediaFileDescriptor();
                                imd.setPath(path);
                                imd.setId(id);
                                imd.setData(idata);
                                preloadedDescriptors.add(imd);
                            }
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                } catch (IOException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
        }


        Status.getStatus().setStringStatus(Status.IDLE);

        return preloadedDescriptors;


    }


    protected TreeSet<MediaFileDescriptor> findSimilarImage(MediaFileDescriptor id, int max) {
        TreeSet<MediaFileDescriptor> tree = new TreeSet<MediaFileDescriptor>(new Comparator<MediaFileDescriptor>() {
            //	@Override
            public int compare(MediaFileDescriptor o1, MediaFileDescriptor o2) {
                return Double.compare(o1.getRmse(), o2.getRmse());
            }
        });

        Iterator<MediaFileDescriptor> it = getPreloadedDescriptors().iterator();
        int found = 0;
        Status.getStatus().setStringStatus(Status.FIND_SIMILAR);
        int size = getPreloadedDescriptors().size();
        int processed = 0;
        ProgressBar pb = new ProgressBar();
        int increment = size / 20;
        int i = 0;
        int step = 0;
        if (increment == 0) {
            increment = 1;
        }


        while (it.hasNext()) {
            MediaFileDescriptor current = it.next();
            processed++;
          //  String path = current.getPath();
            int[] idata = current.getData();
            double rmse = ImageComparator.compareARGBUsingRMSE(id.getData(), idata);
            if (i > increment) {
                i = 0;
                step++;
                if (pb != null) {
                    pb.update(step, 20);
                }
                Status.getStatus().setStringStatus(Status.FIND_SIMILAR + " " + (processed * 100 / size) + "%");
            }
            //System.out.println("Processed " + processed);

            if (tree.size() == max) {
                MediaFileDescriptor df = tree.last();
                if (df.rmse > rmse) {
                    MediaFileDescriptor imd = new MediaFileDescriptor();
                    imd.setPath(current.getPath());
                    imd.setRmse(rmse);
                    imd.setData(current.getData());
                    tree.remove(df);
                    tree.add(imd);
                }
            } else {
                MediaFileDescriptor imd = new MediaFileDescriptor();
                imd.setPath(current.getPath());
                imd.setRmse(rmse);
                imd.setData(current.getData());
                tree.add(imd);
            }
            i++;
        }

        System.out.println("SimilarImageFinder.findSimilarImage resulting tree has size " + tree.size());
        Status.getStatus().setStringStatus(Status.IDLE);
        return tree;
    }

    public void prettyPrintSimilarResults(TreeSet<MediaFileDescriptor> ts, int maxResults) {
        int i = 0;
        for (Iterator iterator = ts.iterator(); iterator.hasNext(); ) {
            i++;
            MediaFileDescriptor imageDescriptor = (MediaFileDescriptor) iterator.next();
            System.out.printf("%1.5f  %s\n", imageDescriptor.getRmse(), imageDescriptor.getPath());
            if (i >= maxResults) {
                break;
            }
        }
    }

    public ArrayList<MediaFileDescriptor> findIdenticalMedia(String source) {

        MediaIndexer tg = new MediaIndexer(null);
        MediaFileDescriptor id = tg.buildMediaDescriptor(new File(source)); // ImageDescriptor.readFromDisk(s);
        System.out.println(id.md5Digest);
        ArrayList<MediaFileDescriptor> al = new ArrayList<MediaFileDescriptor>();
        return thumbstore.getDuplicatesMD5(id);
    }


    public void prettyPrintIdenticalResults(ArrayList<MediaFileDescriptor> findIdenticalMedia) {
        Iterator<MediaFileDescriptor> it = findIdenticalMedia.iterator();
        while (it.hasNext()) {
            MediaFileDescriptor mediaFileDescriptor = (MediaFileDescriptor) it.next();
            System.out.println(mediaFileDescriptor.getPath() + " " + mediaFileDescriptor.getSize());
        }
    }

    public String prettyStringIdenticalResults(ArrayList<MediaFileDescriptor> findIdenticalMedia, int max) {
        Iterator<MediaFileDescriptor> it = findIdenticalMedia.iterator();
        String result = "";
        int i = 0;
        while (it.hasNext() && i < max) {
            MediaFileDescriptor mediaFileDescriptor = (MediaFileDescriptor) it.next();
            i++;
            //System.out.println(mediaFileDescriptor.getPath() + " " + mediaFileDescriptor.getDBInfo());
            result += mediaFileDescriptor.getPath() + " " + mediaFileDescriptor.getSize();
        }

        return result;
    }


    public void testFindSimilarImages(MediaFileDescriptor id) {
        System.out.println("ThumbStore.test() reading descriptor from disk ");
        String s = "/user/fhuet/desktop/home/workspaces/rechercheefficaceimagessimilaires/images/original.jpg";
        System.out.println("ThumbStore.testFindSimilarImages() Reference Image " + s);

        MediaIndexer tg = new MediaIndexer(null);
        id = tg.buildMediaDescriptor(new File(s)); // ImageDescriptor.readFromDisk(s);
        this.prettyPrintSimilarResults(this.findSimilarImage(id, 2), 2);
    }

    public static void main(String[] args) {
        ThumbStore tb = new ThumbStore();
        SimilarImageFinder si = new SimilarImageFinder(tb);
        si.testFindSimilarImages(null);
    }

}

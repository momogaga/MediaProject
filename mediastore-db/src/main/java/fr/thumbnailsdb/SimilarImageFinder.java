package fr.thumbnailsdb;

import fr.thumbnailsdb.bktree.BKTree;
import fr.thumbnailsdb.bktree.RMSEDistance;
import fr.thumbnailsdb.vptree.VPTree;
import fr.thumbnailsdb.vptree.VPTreeBuilder;
import fr.thumbnailsdb.vptree.distances.VPRMSEDistance;

import javax.print.attribute.standard.Media;
import java.io.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class SimilarImageFinder {


    //indicate whether we use the full path in the cache
    //or rely on indexes for lower memory footprint
    protected static boolean USE_FULL_PATH = false;

    protected ThumbStore thumbstore;



    protected BKTree<MediaFileDescriptor> bkTree;// = new BKTree<String>(new RMSEDistance());

    protected VPTree vpTree;

    public SimilarImageFinder(ThumbStore c) {
        this.thumbstore = c;
    }

    public Collection<MediaFileDescriptor> findSimilarMedia(String source, int max) {
        MediaIndexer tg = new MediaIndexer(null);
        MediaFileDescriptor id = tg.buildMediaDescriptor(new File(source));
        Collection<MediaFileDescriptor> result = this.findSimilarImage(id, max);

        if (USE_FULL_PATH) {
            return result;
        } else {
            //we have to add the path to the selected images
            for (MediaFileDescriptor mfd : result) {
                int index = mfd.getId();
               // System.out.println("SimilarImageFinder.findSimilarMedia ID is " + index);
                String path = thumbstore.getPath(mfd.getConnection(), index);
                mfd.setPath(path);
            }
            return result;
        }

    }


    protected BKTree<MediaFileDescriptor> getPreloadedDescriptorsBKTree() {
        if (bkTree == null) {
            int size = thumbstore.size();
            bkTree = new BKTree<MediaFileDescriptor>(new RMSEDistance());
            ArrayList<ResultSet> ares = thumbstore.getAllInDataBases().getResultSets();
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

            ArrayList<ResultSet> ares = thumbstore.getAllInDataBases().getResultSets();
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



    protected Collection<MediaFileDescriptor> findSimilarImage(MediaFileDescriptor id, int max) {

        PriorityQueue<MediaFileDescriptor> queue = new PriorityQueue<MediaFileDescriptor>(max, new Comparator<MediaFileDescriptor>(){
            //	@Override
            public int compare(MediaFileDescriptor o1, MediaFileDescriptor o2) {
                double e1 = o1.getRmse();
                double e2 = o2.getRmse();
                //Sorted in reverse order
                return Double.compare(e2,e1);
            }
        });
//        TreeSet<MediaFileDescriptor> tree = new TreeSet<MediaFileDescriptor>(new Comparator<MediaFileDescriptor>() {
//            //	@Override
//            public int compare(MediaFileDescriptor o1, MediaFileDescriptor o2) {
//                double e1 = o1.getRmse();
//                double e2 = o2.getRmse();
//                if (e1==e2) {
//
//                }
//                return Double.compare(o1.getRmse(), o2.getRmse());
//            }
//        });

        Iterator<MediaFileDescriptor> it = thumbstore.getPreloadedDescriptors().iterator();
        int found = 0;
        Status.getStatus().setStringStatus(Status.FIND_SIMILAR);
        int size = thumbstore.getPreloadedDescriptors().size();
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
            if (idata==null) {
                continue;
            }
//            double rmse = ImageComparator.compareARGBUsingRMSE(id.getData(), idata);
            double rmse = ImageComparator.compareRGBUsingRMSE(id.getData(), idata);

            if (i > increment) {
                i = 0;
                step++;
                if (pb != null) {
                    pb.update(step, 20);
                }
                Status.getStatus().setStringStatus(Status.FIND_SIMILAR + " " + (processed * 100 / size) + "%");
            }
            //System.out.println("Processed " + processed);

            //TODO : WTF do we re-create a mediafiledescriptor here
//            if (tree.size() == max) {
//                MediaFileDescriptor df = tree.last();
//                if (df.rmse > rmse) {
//                    MediaFileDescriptor imd = new MediaFileDescriptor();
//                    imd.setPath(current.getPath());
//                    imd.setRmse(rmse);
//                    imd.setData(current.getData());
//                    imd.setConnection(current.getConnection());
//                    imd.setId(current.getId());
//                    tree.remove(df);
//                    tree.add(imd);
//                }
//            } else {
//                MediaFileDescriptor imd = new MediaFileDescriptor();
//                imd.setPath(current.getPath());
//                imd.setRmse(rmse);
//                imd.setData(current.getData());
//                imd.setConnection(current.getConnection());
//                imd.setId(current.getId());
//                tree.add(imd);
//            }

            if (queue.size() == max) {
                MediaFileDescriptor df = queue.peek();
                if (df.rmse > rmse) {
                    queue.poll();
                    MediaFileDescriptor imd = new MediaFileDescriptor();
                    imd.setPath(current.getPath());
                    imd.setRmse(rmse);
                    imd.setData(current.getData());
                    imd.setConnection(current.getConnection());
                    imd.setId(current.getId());
                    //tree.remove(df);
                    queue.add(imd);
                }
            } else {
                MediaFileDescriptor imd = new MediaFileDescriptor();
                imd.setPath(current.getPath());
                imd.setRmse(rmse);
                imd.setData(current.getData());
                imd.setConnection(current.getConnection());
                imd.setId(current.getId());
                queue.add(imd);
            }
            i++;
        }

        System.out.println("SimilarImageFinder.findSimilarImage resulting queue has size " + queue.size());
        Status.getStatus().setStringStatus(Status.IDLE);

        MediaFileDescriptor[] arr = queue.toArray(new MediaFileDescriptor[] {});
        Arrays.sort(arr, new Comparator<MediaFileDescriptor>() {
                public int compare(MediaFileDescriptor o1, MediaFileDescriptor o2) {
                    double e1 = o1.getRmse();
                    double e2 = o2.getRmse();
                    //Sorted in  order
                    return Double.compare(e1,e2);
                }
           });

        //return tree;
        return Arrays.asList(arr);
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
        //this.prettyPrintSimilarResults(this.findSimilarImage(id, 2), 2);
    }

    public static void main(String[] args) {
        ThumbStore tb = new ThumbStore();
        SimilarImageFinder si = new SimilarImageFinder(tb);
        si.testFindSimilarImages(null);
    }

}

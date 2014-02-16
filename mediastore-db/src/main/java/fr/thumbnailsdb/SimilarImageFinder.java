package fr.thumbnailsdb;

import fr.thumbnailsdb.bktree.BKTree;
import fr.thumbnailsdb.bktree.RMSEDistance;
import fr.thumbnailsdb.lsh.LSH;
import fr.thumbnailsdb.utils.ProgressBar;
import fr.thumbnailsdb.vptree.VPTree;
import fr.thumbnailsdb.vptree.VPTreeBuilder;
import fr.thumbnailsdb.vptree.distances.VPRMSEDistance;

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
       // Collection<MediaFileDescriptor> result = this.findSimilarImage(id, max);
        if (id==null) {
            System.err.println("Error cannot load image "  + source);
        }

        Collection<MediaFileDescriptor> result = this.findSimilarImageUsingLSH(id, max);
        //if (USE_FULL_PATH) {
            return result;
//        } else {
//            //we have to add the path to the selected images
//            for (MediaFileDescriptor mfd : result) {
//                int index = mfd.getId();
//                // System.out.println("SimilarImageFinder.findSimilarMedia ID is " + index);
//                String path = thumbstore.getPath(mfd.getConnection(), index);
//                mfd.setPath(path);
//            }
//            return result;
//        }

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
                            int[] idata = Utils.toIntArray(d);
                            if (idata != null) {

                                MediaFileDescriptor imd = new MediaFileDescriptor();
                                imd.setPath(path);
                                //TODO: handle signature here
                                //  imd.setData(idata);
                                bkTree.add(imd);
                            }
                        }
                    }
                } catch (SQLException e) {
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
                        //  byte[] d = res.getBytes("data");
                        String s = res.getString("hash");
                        if (s != null) {

                            MediaFileDescriptor imd = new MediaFileDescriptor();
                            imd.setPath(path);
                            imd.setHash(s);
//                                imd.setData(idata);
                            //TODO: handle signature here

                            al.add(imd);

                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
            System.out.println("SimilarImageFinder.getPreloadedDescriptors array list built , creating tree");
            vpTree = builder.buildVPTree(al);
        }
        System.out.println("SimilarImageFinder.getPreloadedDescriptors records in VPTree : " + vpTree);
        return vpTree;


    }


    protected Collection<MediaFileDescriptor> findSimilarImageUsingLSH(MediaFileDescriptor id, int max) {

        ArrayList<MediaFileDescriptor> al = thumbstore.findCandidatesUsingLSH(id);
        Iterator<MediaFileDescriptor> it = al.iterator();

        PriorityQueue<MediaFileDescriptor> queue = new PriorityQueue<MediaFileDescriptor>(max, new Comparator<MediaFileDescriptor>() {
            //	@Override
            public int compare(MediaFileDescriptor o1, MediaFileDescriptor o2) {
                double e1 = o1.getDistance();
                double e2 = o2.getDistance();
                //Sorted in reverse order
                return Double.compare(e2, e1);
            }
        });

        //Iterator<MediaFileDescriptor> it = thumbstore.getPreloadedDescriptors().iterator();
        Status.getStatus().setStringStatus(Status.FIND_SIMILAR + " using LSH");
        // int size = thumbstore.getPreloadedDescriptors().size();
        int size =  al.size();
        int processed = 0;
        ProgressBar pb = new ProgressBar(0, size, size / 100);
        int increment = size / 100;

        int processedSinceLastTick = 0;

        while (it.hasNext()) {
            MediaFileDescriptor current = it.next();
            String sig = current.getHash();
            if (sig == null) {
                continue;
            }
            double distance = ImageComparator.compareUsingHammingDistance(id.getHash(), sig);
            processed++;
            processedSinceLastTick++;

            if (processedSinceLastTick >= increment) {
                pb.tick(processed);
                Status.getStatus().setStringStatus(Status.FIND_SIMILAR + " " + pb.getPercent() + "%");
                processedSinceLastTick = 0;
            }

            if (queue.size() == max) {
                MediaFileDescriptor df = queue.peek();
                if (df.distance > distance) {
                    queue.poll();
                    MediaFileDescriptor imd = new MediaFileDescriptor();
                    imd.setPath(current.getPath());
                    imd.setDistance(distance);
                    imd.setHash(current.getHash());
                    imd.setConnection(current.getConnection());
                    imd.setId(current.getId());
                    queue.add(imd);
                }
            } else {
                MediaFileDescriptor imd = new MediaFileDescriptor();
                imd.setPath(current.getPath());
                imd.setDistance(distance);
                imd.setHash(current.getHash());
                imd.setConnection(current.getConnection());
                imd.setId(current.getId());
                queue.add(imd);
            }
        }

        System.out.println("SimilarImageFinder.findSimilarImageUsingLSH resulting queue has size " + queue.size());
        Status.getStatus().setStringStatus(Status.IDLE);

        MediaFileDescriptor[] arr = queue.toArray(new MediaFileDescriptor[]{});
        Arrays.sort(arr, new Comparator<MediaFileDescriptor>() {
            public int compare(MediaFileDescriptor o1, MediaFileDescriptor o2) {
                double e1 = o1.getDistance();
                double e2 = o2.getDistance();
                return Double.compare(e1, e2);
            }
        });

        ArrayList<MediaFileDescriptor> finalArray = new ArrayList<MediaFileDescriptor>(arr.length);
        for (MediaFileDescriptor m : arr) {
            MediaFileDescriptor md = thumbstore.getMediaFileDescriptor(m.getId());
            md.setDistance(m.getDistance());
            finalArray.add(md);
        }

        return finalArray; //Arrays.asList(arr);

    }

    protected Collection<MediaFileDescriptor> findSimilarImage(MediaFileDescriptor id, int max) {

        PriorityQueue<MediaFileDescriptor> queue = new PriorityQueue<MediaFileDescriptor>(max, new Comparator<MediaFileDescriptor>() {
            //	@Override
            public int compare(MediaFileDescriptor o1, MediaFileDescriptor o2) {
                double e1 = o1.getDistance();
                double e2 = o2.getDistance();
                //Sorted in reverse order
                return Double.compare(e2, e1);
            }
        });

        Iterator<MediaFileDescriptor> it = thumbstore.getPreloadedDescriptors().iterator();
        Status.getStatus().setStringStatus(Status.FIND_SIMILAR);
        int size = thumbstore.getPreloadedDescriptors().size();
        int processed = 0;
        ProgressBar pb = new ProgressBar(0, size, size / 100);
        int increment = size / 100;

        int processedSinceLastTick = 0;

        while (it.hasNext()) {
            MediaFileDescriptor current = it.next();
            String sig = current.getHash();
            if (sig == null) {
                continue;
            }
            double distance = ImageComparator.compareUsingHammingDistance(id.getHash(), sig);
            processed++;
            processedSinceLastTick++;

            if (processedSinceLastTick >= increment) {
                pb.tick(processed);
                Status.getStatus().setStringStatus(Status.FIND_SIMILAR + " " + pb.getPercent() + "%");
                processedSinceLastTick = 0;
            }

            if (queue.size() == max) {
                MediaFileDescriptor df = queue.peek();
                if (df.distance > distance) {
                    queue.poll();
                    MediaFileDescriptor imd = new MediaFileDescriptor();
                    imd.setPath(current.getPath());
                    imd.setDistance(distance);
                    imd.setHash(current.getHash());
                    imd.setConnection(current.getConnection());
                    imd.setId(current.getId());
                    queue.add(imd);
                }
            } else {
                MediaFileDescriptor imd = new MediaFileDescriptor();
                imd.setPath(current.getPath());
                imd.setDistance(distance);
                imd.setHash(current.getHash());
                imd.setConnection(current.getConnection());
                imd.setId(current.getId());
                queue.add(imd);
            }
        }

        System.out.println("SimilarImageFinder.findSimilarImage resulting queue has size " + queue.size());
        Status.getStatus().setStringStatus(Status.IDLE);

        MediaFileDescriptor[] arr = queue.toArray(new MediaFileDescriptor[]{});
        Arrays.sort(arr, new Comparator<MediaFileDescriptor>() {
            public int compare(MediaFileDescriptor o1, MediaFileDescriptor o2) {
                double e1 = o1.getDistance();
                double e2 = o2.getDistance();
                return Double.compare(e1, e2);
            }
        });
        return Arrays.asList(arr);
    }

    public void prettyPrintSimilarResults(TreeSet<MediaFileDescriptor> ts, int maxResults) {
        int i = 0;
        for (Iterator iterator = ts.iterator(); iterator.hasNext(); ) {
            i++;
            MediaFileDescriptor imageDescriptor = (MediaFileDescriptor) iterator.next();
            System.out.printf("%1.5f  %s\n", imageDescriptor.getDistance(), imageDescriptor.getPath());
            if (i >= maxResults) {
                break;
            }
        }
    }

    public ArrayList<MediaFileDescriptor> findIdenticalMedia(String source) {

        MediaIndexer tg = new MediaIndexer(null);
        MediaFileDescriptor id = tg.buildMediaDescriptor(new File(source)); // ImageDescriptor.readFromDisk(s);
//        System.out.println(id.md5Digest);
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

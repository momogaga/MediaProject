package fr.thumbnailsdb;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.apache.commons.codec.digest.DigestUtils;

public class MediaIndexer {

    protected boolean debug;
    protected boolean software = true;
    protected ThumbStore ts;

    protected boolean forceGPSUpdate = false;

    //  protected MetaDataFinder mdf = new MetaDataFinder();

    protected Logger log = Logger.getLogger();

    protected int newFiles = 0;
    protected int updatedFiles = 0;

    // protected ExecutorService executorService =
    // Executors.newFixedThreadPool(3);
//	protected final Semaphore semaphore = new Semaphore(1);

    ThreadPoolExecutor executorService; //= new ThreadPoolExecutor(4, 4, 0L, TimeUnit.MILLISECONDS,
//			new LimitedQueue<Runnable>(50));

    public MediaIndexer(ThumbStore t) {
        this.ts = t;
    }

    private class LimitedQueue<E> extends LinkedBlockingQueue<E> {
        public LimitedQueue(int maxSize) {
            super(maxSize);
        }

        @Override
        public boolean add(E e) {
            // System.out.println("MediaIndexer.LimitedQueue.add()");
            return super.add(e);
        }

        @Override
        public boolean offer(E e) {
            //	System.out.println("MediaIndexer.LimitedQueue.offer() " + this.size());
            // turn offer() and add() into a blocking calls (unless interrupted)
            try {
                put(e);
                return true;
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            // System.out.println("MediaIndexer.LimitedQueue.offer()   ... done");
            return false;
        }

        @Override
        public E take() throws InterruptedException {
            //		System.out.println("MediaIndexer.LimitedQueue.take()");
            return super.take();
        }

    }

    /**
     * Load the image and resize it if necessary
     *
     * @param bi
     * @return
     * @throws IOException
     */
    public BufferedImage downScaleImageToGray(BufferedImage bi, int nw, int nh) throws IOException {

        if (debug) {
            System.out.println("MediaIndexer.downScaleImageToGray()  original image is " + bi.getWidth() + "x"
                    + bi.getHeight());
        }
        BufferedImage scaledBI = null;
        // if (nw < width || nh < height) {
        if (debug) {
            System.out.println("MediaIndexer.downScaleImageToGray() to " + nw + "x" + nh);
        }
        if (debug) {
            System.out.println("resizing to " + nw + "x" + nh);
        }
        scaledBI = new BufferedImage(nw, nh, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = scaledBI.createGraphics();
        g.setComposite(AlphaComposite.Src);
        g.drawImage(bi, 0, 0, nw, nh, null);
        g.dispose();
        // }
        return scaledBI;
    }

    public String generateMD5(File f) throws IOException {
        InputStream fis = new FileInputStream(f);
        byte[] buffer = DigestUtils.md5(fis);
        String s = DigestUtils.md5Hex(buffer);

        fis.close();
        return s;
        // = new byte[1024];
        // MessageDigest complete = null;
        // try {
        // complete = MessageDigest.getInstance("MD5");
        // int numRead;
        // do {
        // numRead = fis.read(buffer);
        // if (numRead > 0) {
        // complete.update(buffer, 0, numRead);
        // }
        // } while (numRead != -1);
        //
        // fis.close();
        // } catch (NoSuchAlgorithmException e) {
        // e.printStackTrace();
        // }
        // return complete.digest();
    }

    protected int[] generateThumbnail(File f) {
        // byte[] data;
        BufferedImage source;
        int[] data1 = null;
        try {
            source = ImageIO.read(f);

            BufferedImage dest = null;
            if (software) {
                dest = this.downScaleImageToGray(source, 10, 10);
            }
            // ByteArrayOutputStream baos = new ByteArrayOutputStream();
            // ImageIO.write(dest, "jpg", baos);
            // baos.flush();
            // data = baos.toByteArray();
            // baos.close();

            data1 = new int[dest.getWidth() * dest.getHeight()];
            dest.getRGB(0, 0, dest.getWidth(), dest.getHeight(), data1, 0, dest.getWidth());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return data1;
    }

    public MediaFileDescriptor buildMediaDescriptor(File f) {
        MediaFileDescriptor id = new MediaFileDescriptor();
        int[] data;
        String md5;
        try {
            id.setPath(f.getCanonicalPath());
            id.setMtime(f.lastModified());
            id.setSize(f.length());
            // generate thumbnails only for images, not video
            if (Utils.isValideImageName(f.getName())) {
                data = generateThumbnail(f);
                id.setData(data);
                MetaDataFinder mdf = new MetaDataFinder(f);
                double[] latLon = mdf.getLatLong();
                if (latLon != null) {
                    id.setLat(latLon[0]);
                    id.setLon(latLon[1]);
                }
            }
            md5 = generateMD5(f);
            id.setMd5Digest(md5);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return id;
    }

    public void generateAndSave(File f) {
        //first check if it is in DB


        try {
            if (ts.isInDataBaseBasedOnName(f.getCanonicalPath())) {
                 System.out.println("MediaIndexer.generateAndSave " + f);
                 System.out.println("MediaIndexer.generateImageDescriptor() Already in DB, ignoring");
                if (forceGPSUpdate) {
                    MediaFileDescriptor mfd = ts.getMediaFileDescriptor(f.getCanonicalPath());
                    MetaDataFinder mdf = new MetaDataFinder(f);
                    double latLon[] = mdf.getLatLong();
                    // System.out.println("MediaIndexer.generateAndSave working on " + f);
                    if (latLon != null) {
                        mfd.setLat(latLon[0]);
                        mfd.setLon(latLon[1]);
                        System.out.println("MediaIndexer : forced update for GPS data for " + f);
                        ts.updateToDB(mfd);
                        updatedFiles++;
                    }
                }
                //log.log(f.getCanonicalPath() + " already in DB");
            } else {
                MediaFileDescriptor id = this.buildMediaDescriptor(f);
                if (id != null) {
                    ts.saveToDB(id);
                }
                log.log(f.getCanonicalPath() + " ..... size  " + (f.length() / 1024) + " KiB OK " + executorService.getActiveCount() + " threads running");
                newFiles++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        //}
    }

    public void process(String path) {
        try {
            this.process(new File(path));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void process(File fd) throws IOException {
        if (isValideFile(fd)) {
            this.generateAndSave(fd);
            // }
        } else {
            if (fd.isDirectory()) {
                String entries[] = fd.list();
                if (entries != null) {
                    for (int i = 0; i < entries.length; i++) {
                        File f = new File(fd.getCanonicalPath() + "/" + entries[i]);
                        if (isValideFile(fd)) {
                            this.generateAndSave(f);
                        } else {
                            this.process(f);
                        }
                    }
                }
            }
        }
    }

    public boolean isValideFile(File fd) {
        return fd.isFile(); //&& (Utils.isValideImageName(fd.getName()) || Utils.isValideVideoName(fd.getName()));
    }

    public void processMTRoot(String path) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        //System.out.println(dateFormat.format(date));
        ts.addIndexPath(path);
        System.out.println("MediaIndexer.processMTRoot() started at time " + dateFormat.format(date));
        executorService = new ThreadPoolExecutor(4, 4, 0L, TimeUnit.MILLISECONDS,
                new LimitedQueue<Runnable>(50));
        try {
            this.processMT(new File(path));
        } catch (IOException e) {
            e.printStackTrace();
        }
        executorService.shutdown();
        try {
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        date = new Date();
        System.out.println("MediaIndexer.processMTRoot() finished at time " + dateFormat.format(date));
        System.out.println("MediaIndexer.processMTRoot() found " + newFiles + " new files");
        System.out.println("MediaIndexer.processMTRoot() updated " + updatedFiles + " files");

    }


    public void processMT(File fd) throws IOException {
        if (isValideFile(fd)) {
            executorService.submit(new RunnableProcess(fd));
        } else {
            if (fd.isDirectory()) {
                String entries[] = fd.list();
                if (entries != null) {
                    for (int i = 0; i < entries.length; i++) {
                        File f = new File(fd.getCanonicalPath() + "/" + entries[i]);
                        if (isValideFile(fd)) {
                            executorService.submit(new RunnableProcess(f));
                        } else {
                            this.processMT(f);
                        }
                    }
                }
            }
        }
    }


    public void updateDB() {
        this.updateDB(ts.getIndexedPaths());
    }

    public void updateDB(List<String> al) {

        for (String s : al) {
            System.out.println("MediaIndexer.updateDB updating " +s);
            Status.getStatus().setStringStatus("Updating folder " + s);

            processMTRoot(s);
        }
        Status.getStatus().setStringStatus(Status.IDLE);
    }


    protected void submit(RunnableProcess rp) {
//		try {
        //	semaphore.acquire();
        executorService.submit(rp);
        //	semaphore.release();
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
    }

    protected class RunnableProcess implements Runnable {
        protected File fd;

        public RunnableProcess(File fd) {
            this.fd = fd;
        }

        //	@Override
        public void run() {
            generateAndSave(fd);
        }
    }

    public static void main(String[] args) {
        String pathToDB = "test";
        String source = ".";
        if (args.length == 2 || args.length == 4) {
            for (int i = 0; i < args.length; i++) {
                if ("-db".equals(args[i])) {
                    pathToDB = args[i + 1];
                    i++;
                }
                if ("-source".equals(args[i])) {
                    source = args[i + 1];
                    i++;
                }
            }
            // pathToDB=args[0];
        } else {
            System.err.println("Usage: java " + MediaIndexer.class.getName()
                    + "[-db path_to_db] -source folder_or_file_to_process");
            System.exit(0);
        }
        ThumbStore ts = new ThumbStore(pathToDB);
        MediaIndexer tb = new MediaIndexer(ts);
        File fs = new File(source);
        try {
            tb.processMT(fs);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

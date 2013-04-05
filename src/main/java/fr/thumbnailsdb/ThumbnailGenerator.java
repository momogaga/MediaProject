package fr.thumbnailsdb;

import org.apache.commons.codec.digest.DigestUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThumbnailGenerator {

    public static final int WIDTH = 10;
    public static final int HEIGHT = 10;
    protected boolean debug;
	protected boolean software = true;
	protected ThumbStore ts;

	protected Logger log = Logger.getLogger();

	// protected ExecutorService executorService =
	// Executors.newFixedThreadPool(3);
//	protected final Semaphore semaphore = new Semaphore(1);

	ThreadPoolExecutor executorService = new ThreadPoolExecutor(4, 4, 0L, TimeUnit.MILLISECONDS,
			new LimitedQueue<Runnable>(50));

	public ThumbnailGenerator(ThumbStore t) {
		this.ts = t;
	}

	private class LimitedQueue<E> extends LinkedBlockingQueue<E> {
		public LimitedQueue(int maxSize) {
			super(maxSize);
		}

		@Override
		public boolean add(E e) {
			// System.out.println("ThumbnailGenerator.LimitedQueue.add()");
			return super.add(e);
		}

		@Override
		public boolean offer(E e) {
		//	System.out.println("ThumbnailGenerator.LimitedQueue.offer() " + this.size());
			// turn offer() and add() into a blocking calls (unless interrupted)
			try {
				put(e);
				return true;
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
			}
			// System.out.println("ThumbnailGenerator.LimitedQueue.offer()   ... done");
			return false;
		}

		@Override
		public E take() throws InterruptedException {
	//		System.out.println("ThumbnailGenerator.LimitedQueue.take()");
			return super.take();
		}

	}

	/**
	 * Load the image and resize it if necessary
	 * 
	 *
	 * @return
	 * @throws IOException
	 */
	public BufferedImage downScaleImageToGray(BufferedImage bi, int nw, int nh) throws IOException {

		if (debug) {
			System.out.println("ThumbnailGenerator.downScaleImageToGray()  original image is " + bi.getWidth() + "x"
					+ bi.getHeight());
		}
		BufferedImage scaledBI = null;
		// if (nw < width || nh < height) {
		if (debug) {
			System.out.println("ThumbnailGenerator.downScaleImageToGray() to " + nw + "x" + nh);
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

    /**
     * restace to nh x nw while maintaining aspect ratio
     * @param bi
     * @param nw
     * @param nh
     * @return
     * @throws IOException
     */
    public BufferedImage downScaleImage(BufferedImage bi, int nw, int nh) throws IOException {
     //   if (debug) {
            System.out.println("ThumbnailGenerator.downScaleImage()  original image is " + bi.getWidth() + "x"
                    + bi.getHeight());
      //  }
        BufferedImage scaledBI = null;
        // if (nw < width || nh < height) {
      //  if (debug) {
            System.out.println("ThumbnailGenerator.downScaleImage() requested to " + nw + "x" + nh);
      //  }


        float h_original = bi.getHeight();
        float w_original= bi.getWidth();

        float h_ratio = h_original/nh;
        float w_ratio = w_original/nw;
        //ensure we have at least one
        if (h_ratio <1) {h_ratio=1;}
        if (w_ratio <1) {w_ratio=1;}

        float ratio = Math.max(h_ratio,w_ratio);

        int fWidth = Math.round(w_original/ratio);
        int fHeight = Math.round(h_original/ratio);

      //  if (debug) {
            System.out.println("resizing to " + fWidth + "x" + fHeight + " with scale " + ratio);
        //}
        scaledBI = new BufferedImage(fWidth, fHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaledBI.createGraphics();
        g.setComposite(AlphaComposite.Src);
        g.drawImage(bi, 0, 0, fWidth, fHeight, null);
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
				dest = this.downScaleImageToGray(source, WIDTH, HEIGHT);
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
	//	if (Utils.isValideImageName(f.getName()) || Utils.isValideVideoName(f.getName())) {
			// System.out.println("ThumbnailGenerator.generateAndSave() processing "
			// + f);
			// System.out.println("checking if in DB");
			try {
				if (ts.isInDataBaseBasedOnName(f.getCanonicalPath())) {
					// System.out.println("ThumbnailGenerator.generateImageDescriptor() Already in DB, ignoring");
					log.log(f.getCanonicalPath() + " already in DB");
				} else {
					MediaFileDescriptor id = this.buildMediaDescriptor(f);
					if (id != null) {
						ts.saveToDB(id);
					}
					log.log(f.getCanonicalPath() + " ..... size  " +  (f.length()/1024) + " KiB OK " + executorService.getActiveCount() + " threads running");
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
		return fd.isFile() ; //&& (Utils.isValideImageName(fd.getName()) || Utils.isValideVideoName(fd.getName()));
	}

	public void processMT(String path) {
		// System.out.println("ThumbnailGenerator.processMTRoot()");
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

//	public static void main(String[] args) {
//		String pathToDB = "test";
//		String source = ".";
//		if (args.length == 2 || args.length == 4) {
//			for (int i = 0; i < args.length; i++) {
//				if ("-db".equals(args[i])) {
//					pathToDB = args[i + 1];
//					i++;
//				}
//				if ("-source".equals(args[i])) {
//					source = args[i + 1];
//					i++;
//				}
//			}
//			// pathToDB=args[0];
//		} else {
//			System.err.println("Usage: java " + ThumbnailGenerator.class.getName()
//					+ "[-db path_to_db] -source folder_or_file_to_process");
//			System.exit(0);
//		}
//		ThumbStore ts = new ThumbStore(pathToDB);
//		ThumbnailGenerator tb = new ThumbnailGenerator(ts);
//		File fs = new File(source);
//		try {
//			tb.processMT(fs);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}

    public static void main(String[] args) {
        ThumbnailGenerator tg = new ThumbnailGenerator(null);
        MediaFileDescriptor mf = tg.buildMediaDescriptor(new File(args[0]));

        BufferedImage image = null; //new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_BYTE_GRAY);
        BufferedImage target = null;
          try {
            image = ImageIO.read(new FileInputStream(args[0]));
        //   InputStream in = new ByteArrayInputStream(mf.getDataAsByte());
          // image = ImageIO.read(in);
           // image.setRGB(0,0,WIDTH,HEIGHT,mf.getData(),0,WIDTH);
            target = tg.downScaleImage(image,300,300)     ;
            System.out.println("ThumbnailGenerator.main image is " + target);
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }


//
// Use a label to display the image
        JFrame frame = new JFrame();

        JLabel lblimage = new JLabel(new ImageIcon(target));
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(lblimage);

// add more components here
        frame.add(mainPanel);
        frame.pack();
        frame.setVisible(true);

    }

}

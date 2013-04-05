package fr.surfjavacl;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import au.notzed.socle.SOCLEContext;
import au.notzed.socle.feature.surf.SURFDetector;
import au.notzed.socle.image.ImageFormats;
import au.notzed.socle.util.ImageLoader;

import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLImage2d;
import com.jogamp.opencl.CLKernel;
import com.jogamp.opencl.CLProgram;

public class SurfGenerator {

	protected static final int maxNumDescriptors = 2000;
	protected final boolean resize = true;
	protected static final boolean debug = true;
	protected static boolean forceSoftwareDownscaling = false;

	protected SOCLEContext sc;
	protected CLContext cl;
	protected CLCommandQueue q;
	protected SURFDetector surf;

	protected CLKernel kDownsample;

	protected int callToGenerateDescriptors;

	protected int maxWidth = 800;
	protected int maxHeight = 800;

	protected int resizeWidth = 800;
	protected int resizeHeight = 800;

	protected CLImage2d<ByteBuffer> clsrc;
	// buffer for resized image
	protected CLImage2d<ByteBuffer> cldst;

	private Pattern pattern;
	private Matcher matcher;

	protected FileWriter tOutput;
	protected ObjectOutputStream bOutput;

	private static final String IMAGE_PATTERN = "(.+(\\.(?i)(jpg|jpeg|png|gif|bmp))$)";

	public static BufferedImage createResizedGreyCopy(Image originalImage, int scaledWidth, int scaledHeight,
			boolean preserveAlpha) {
		if (debug) {
			System.out.println("resizing to " + scaledWidth + "x" + scaledHeight);
		}
		int imageType = preserveAlpha ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
		BufferedImage scaledBI = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_BYTE_GRAY);
		Graphics2D g = scaledBI.createGraphics();
		if (preserveAlpha) {
			g.setComposite(AlphaComposite.Src);
		}
		g.drawImage(originalImage, 0, 0, scaledWidth, scaledHeight, null);
		g.dispose();
		return scaledBI;
	}

	/**
	 * Round n up to the nearest m.
	 * 
	 * @param n
	 *            value
	 * @param m
	 *            must be a power of 2
	 * @return (n + m-1) & ~(m-1)
	 */
	public static int roundUp(int n, int m) {
		return (n + m - 1) & ~(m - 1);
	}

	public SurfGenerator() {
		pattern = Pattern.compile(IMAGE_PATTERN);
		this.initializeCL();
	}

	public void setOutputAsTxt(String fileName) throws IOException {
		File f = new File(fileName);
		System.out.println("SurfGenerator.setOutputAsTxt() to " + f.getAbsolutePath());
		tOutput = new FileWriter(f);
	}

	public void setOutputAsBinary(String fileName) throws FileNotFoundException, IOException {
		File f = new File(fileName);
		System.out.println("SurfGenerator.setOutputAsBinary() to " + f.getAbsolutePath());
		bOutput = new ObjectOutputStream(new FileOutputStream(f));
	}

	public void initializeCL() {
		// for (CLDevice d :CLContext.create().getDevices()) {
		// System.out.println("SurfGenerator.initializeCL() found device " + d);
		// }

		SOCLEContext sc = new SOCLEContext(CLContext.create(CLDevice.Type.GPU));
		if (debug) {
			System.out.println("SurfGenerator.initializeCL() Max work group size : "
					+ +sc.getDevice().getMaxWorkGroupSize());
			this.maxWidth = sc.getDevice().getMaxImage2dWidth();
			this.maxHeight = sc.getDevice().getMaxImage2dHeight();

			System.out.println("Device Max image size " + maxWidth + "x" + maxHeight);
		}
		cl = sc.cl;
		q = sc.getQ();

		CLProgram prog = null;
		try {
			prog = cl.createProgram(Resample.class.getResourceAsStream("resample.cl"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		prog.build("");

		kDownsample = prog.createCLKernel("downsample");

	}

	/**
	 * Validate image with regular expression
	 * 
	 * @param image
	 *            image for validation
	 * @return true valid image, false invalid image
	 */
	public boolean validate(final String image) {

		matcher = pattern.matcher(image);
		return matcher.matches();

	}

	/**
	 * Load the image and resize it if necessary
	 * 
	 * @param imagePath
	 * @return
	 * @throws IOException
	 */
	public BufferedImage downScaleImageToGray(BufferedImage bi, int nw, int nh) throws IOException {

		if (debug) {
			System.out.println("SurfGenerator.loadImage() original image is " + bi.getWidth() + "x" + bi.getHeight());
		}
		int width = bi.getWidth();
		int height = bi.getHeight();
		if (nw < width || nh < height) {
			if (debug) {
				System.out.println("SurfGenerator.downScaleImage() to " + nw + "x" + nh);
			}
			return createResizedGreyCopy(bi, nw, nh, true);
		}
		return bi;
	}

	@SuppressWarnings("unchecked")
	public BufferedImage downScaleImageToGrayWithOpenCL(BufferedImage bi, int nw, int nh) throws IOException {

		if (debug) {
			System.out.println("SurfGenerator.downScaleImageToGrayWithOpenCL() original image is " + bi.getWidth()
					+ "x" + bi.getHeight());
		}
		int width = bi.getWidth();
		int height = bi.getHeight();

		if (nw > width && nh < height) {
			return bi;
		}

		CLImage2d<IntBuffer> imgSrc = (CLImage2d<IntBuffer>) cl.createImage2d(bi.getWidth(), bi.getHeight(),
				ImageFormats.R_UINT8);
		ImageLoader.putImage(q, bi, imgSrc);

		if (cldst == null) {
			cldst = (CLImage2d<ByteBuffer>) cl.createImage2d(nw, nh, ImageFormats.R_UINT8);
		}

		System.out.println("Resample.downScaleImageToGrayWithOpenCL() resampling to " + nw + "x" + nh);
		kDownsample.setArg(0, imgSrc);
		kDownsample.setArg(1, cldst);

		// q.put2DRangeKernel(kDownsample, 0, 0, imgSrc.width/16,
		// imgSrc.height/16, 8, 8);
		q.put2DRangeKernel(kDownsample, 0, 0, roundUp(imgSrc.width, 16), roundUp(imgSrc.height, 16), 0, 0);
		q.finish();

		imgSrc.release();
		return null;

	}

	public void generateDescriptors(File imagePath) throws IOException {
		callToGenerateDescriptors++;
		System.out.println("SurfGenerator.generateDescriptors()  ----- processing : " + imagePath);
		// BufferedImage bi = this.loadImageToGreyScale(imagePath);
		// //this.loadImage(imagePath);
		BufferedImage bi = ImageIO.read(imagePath);

		long now = 0;

		if (surf == null) {
			// if (!resize) {
			// surf = new SURFDetector(cl, 1, 2000, width, height, 2, 0.00005f);
			// } else {
			surf = new SURFDetector(cl, 1, maxNumDescriptors, resizeWidth, resizeHeight, 2, 0.00005f);
			// }
		}

		if (clsrc == null) {
			clsrc = cl.createImage2d(
					ByteBuffer.allocateDirect(resizeWidth * resizeHeight).order(ByteOrder.nativeOrder()), resizeWidth,
					resizeHeight, ImageFormats.R_UINT8);
		}
		int id = 0;

		if (resize) {
			if (softwareRescaling(bi)) {
				// image cannot be processed with OpenCL on this device
				long t0 = System.nanoTime();
				bi = this.downScaleImageToGray(bi, this.resizeWidth, this.resizeHeight);
				long t1 = System.nanoTime();
				System.out.println("SurfGenerator.generateDescriptors() Downscaling took " + (t1 - t0) / 1000.0f
						/ 1000.0f + " ms in SOFTWARE");

				now = System.nanoTime();
				ImageLoader.putImage(q, bi, clsrc);
				surf.loadImage(q, id, clsrc);

			} else {
				long t0 = System.nanoTime();
				bi = this.downScaleImageToGrayWithOpenCL(bi, this.resizeWidth, this.resizeHeight);
				long t1 = System.nanoTime();
				System.out.println("SurfGenerator.generateDescriptors() Downscaling took " + (t1 - t0) / 1000.0f
						/ 1000.0f + " ms in OPENCL");

				now = System.nanoTime();
				// ImageLoader.putImage(q, bi, clsrc);

				// use result of OpenCL downsampling cldst
				surf.loadImage(q, id, cldst);
			}
		} else {
			ImageLoader.putImage(q, bi, clsrc);
			surf.loadImage(q, id, clsrc);
		}

		q.putReadBuffer(surf.getPoints(id), true);
		q.putReadBuffer(surf.getDescriptors(id), true);

		q.finish();
		now = System.nanoTime() - now;
		if (debug) {
			System.out.printf(imagePath + ": found %d features in %d.%06ds\n", surf.getPointCount(id),
					now / 1000000000L, (now / 1000) % 1000000);
		}
		if (tOutput == null && bOutput == null) {
			this.outputDescriptors(imagePath.getAbsolutePath());
		}
		if (tOutput != null) {
			this.outputDescriptorsToFile(imagePath.getAbsolutePath());

		}
		if (bOutput != null) {
			this.serializeDescriptorsToFile(imagePath.getAbsolutePath());
		}

	}

	protected void testDownscalingOpenCL(File imagePath) throws IOException {
		System.out.println("SurfGenerator.testDownscalingOpenCL() " + imagePath);
		BufferedImage bi = ImageIO.read(imagePath);
		this.downScaleImageToGrayWithOpenCL(bi, this.resizeWidth, this.resizeHeight);
		bi.flush();

	}

	protected boolean softwareRescaling(BufferedImage bi) {
		return (bi.getWidth() > this.maxWidth || bi.getHeight() > this.maxHeight) || forceSoftwareDownscaling;
	}

	public void processDirectory(File dir) throws IOException {
		String entries[] = dir.list();
		for (int i = 0; i < entries.length; i++) {
			File f = new File(dir.getAbsolutePath() + "/" + entries[i]);
			if (f.isFile()) {
				if (this.validate(f.getName())) {
					// this.testDownscalingOpenCL(f);
					this.generateDescriptors(f);
				}
			} else {
				this.processDirectory(f);
			}
		}
	}

	public void serializeDescriptorsToFile(String imageName) throws IOException {
		int count = surf.getPointCount(0);
		// FloatBuffer points = surf.getPoints(0).getBuffer();
		if (debug) {
			System.out.println("SURFeatures.serializeDescriptorsToFile() found " + count + " descriptors for image "
					+ imageName);
		}

		CLBuffer<FloatBuffer> descriptors = surf.getDescriptors(0);
		FloatBuffer fb = descriptors.getBuffer();

		SerializableDescriptor sd = new SerializableDescriptor(imageName, count, fb);
		bOutput.writeObject(sd);
		bOutput.flush();
		bOutput.reset();
	}

	public void outputDescriptorsToFile(String imageName) throws IOException {
		int count = surf.getPointCount(0);
		// FloatBuffer points = surf.getPoints(0).getBuffer();
		if (debug) {
			System.out.println("SURFeatures.outputDescriptorsToFile() found " + count + " descriptors for image "
					+ imageName);
		}

		StringBuffer bf = new StringBuffer(count * 4);

		CLBuffer<FloatBuffer> descriptors = surf.getDescriptors(0);
		FloatBuffer fb = descriptors.getBuffer();

		int total = 0;
		// float f = 0;
		// output.write(imageName + ";");
		bf.append(imageName).append(";");
		for (int i = 0; i < count * 64; i += 64) {
			// total++;
			for (int j = 0; j < 63; j++) {
				total++;
				// output.write(fb.get(i + j) + ",");
				bf.append(fb.get(i + j)).append(",");
			}
			total++;
			// output.write(fb.get(i + 63) + ";");
			bf.append(fb.get(i + 63)).append(";");
		}
		// output.write("\n");
		bf.append("\n");
		tOutput.write(bf.toString());
		tOutput.flush();
		if (debug) {
			System.out.println("SURFeatures.outputDescriptorsToFile() total " + total + " total/64=" + (total / 64));
		}

	}

	public void outputDescriptors(String imageName) {

		int count = surf.getPointCount(0);
		FloatBuffer points = surf.getPoints(0).getBuffer();
		if (debug) {
			System.out
					.println("SURFeatures.outputDescriptors() found " + count + " descriptors for image " + imageName);
		}
		CLBuffer<FloatBuffer> descriptors = surf.getDescriptors(0);

		FloatBuffer fb = descriptors.getBuffer();

		int total = 0;
		float f = 0;
		System.out.print(imageName + ";");
		for (int i = 0; i < count * 64; i += 64) {
			total++;
			for (int j = 0; j < 63; j++) {
				total++;
				System.out.print(fb.get(i + j) + ",");
			}
			System.out.print(fb.get(i + 63) + ";");
		}
		System.out.println();
		if (debug) {
			System.out.println("SURFeatures.outputDescriptors() total " + total + " total/64=" + (total / 64));
		}

	}

	public static void main(final String[] args) throws IOException {
		String[] a = args;
		if (a.length == 0) {
			a = new String[] { "/user/fhuet/home/workspaces/rechercheefficaceimagessimilaires/reisgpgpu/test/test.jpg" };
		}

		// a= new String[]
		// {"/user/fhuet/desktop/home/Perso/photos/2010-02-13 (Sortie Raquette Gourdon)/IMG_3836.JPG"};;

		// Prepare output file

		SurfGenerator sg = new SurfGenerator();

		 sg.setOutputAsTxt("descriptorsT.out");
		//sg.setOutputAsBinary("descriptorsB.out");

		long t0 = System.currentTimeMillis();
		try {
			// sg.initializeCL();
			for (String s : a) {
				if (s.equals("-software")) {
					forceSoftwareDownscaling = true;
				} else {
					if (debug) {
						System.out.println("SurfGenerator.main() processing " + s);
					}
					File fs = new File(s);
					if (fs.isDirectory()) {
						sg.processDirectory(fs);
					} else {
						// if (sg.validate(s)) {

						sg.generateDescriptors(fs);
						// }
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		long t1 = System.currentTimeMillis();
		System.out.println("SurfGenerator.main() processed " + sg.callToGenerateDescriptors + " images in " + (t1 - t0)
				/ 1000f + "s");

	}

}

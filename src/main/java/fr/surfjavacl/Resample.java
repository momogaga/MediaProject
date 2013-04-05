package fr.surfjavacl;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import au.notzed.socle.SOCLECommand;
import au.notzed.socle.SOCLEContext;
import au.notzed.socle.image.ImageFormats;
import au.notzed.socle.util.ImageLoader;

import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLImage2d;
import com.jogamp.opencl.CLKernel;
import com.jogamp.opencl.CLProgram;

public class Resample {

	protected SOCLEContext sc;
	protected SOCLECommand soCommand;
	protected CLContext cl;
	protected CLCommandQueue q;

	protected int maxWidth;
	protected int maxHeight;

	protected CLKernel kDownsample2R;
	protected CLKernel kDownsample4R;
	protected CLKernel kDownsampleK;
	protected CLKernel kDownsampleFloat;
	protected CLKernel kDownsample;

	protected float scale = 1.0f;
	protected JFrame frame = null;
	protected JLabel label = null;

	protected BufferedImage original;
	protected BufferedImage toDisplay;

	CLImage2d<ByteBuffer> clsrc;

	public static int roundUp(int n, int m) {
		return (n + m - 1) & ~(m - 1);
	}

	protected Resample() {
		// super(cl);
		this.initializeCL();

		CLProgram prog = null;
		try {
			prog = cl.createProgram(Resample.class.getResourceAsStream("resample.cl"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println("Loading resample.cl");
		prog.build("");

		kDownsample2R = prog.createCLKernel("downsample2_r");
		kDownsample4R = prog.createCLKernel("downsample4_r");
		kDownsampleK = prog.createCLKernel("downsamplek_r");
		kDownsampleFloat = prog.createCLKernel("downsamplek_float");
		kDownsample = prog.createCLKernel("downsample");
	}

	public void downSampleImageAttributed(BufferedImage bf, float k) {
		// this.toDisplay = downSampleImage(bf, k);
		this.toDisplay = downSampleImageInt(bf, (int) k);
	}

	@SuppressWarnings("unchecked")
	public BufferedImage downSampleImage(BufferedImage bf, float k) {
		// allocate source image on device
		long t0 = System.nanoTime();

		if (clsrc == null) {
			clsrc = (CLImage2d<ByteBuffer>) cl.createImage2d(bf.getWidth(), bf.getHeight(), ImageFormats.R_UINT8);
			ImageLoader.putImage(q, bf, clsrc);
		}
		// allocate result image
		int newWidth = (int) (bf.getWidth() / k);
		int newHeight = (int) (bf.getHeight() / k);
		CLImage2d<IntBuffer> cldst = (CLImage2d<IntBuffer>) cl.createImage2d(newWidth, newHeight,
				ImageFormats.RGBA_UINT8);
		System.out.println("Resample.downSampleImage() resampling to " + newWidth + "x" + newHeight);
		kDownsampleFloat.setArg(0, clsrc);
		kDownsampleFloat.setArg(1, cldst);
		kDownsampleFloat.setArg(2, k);

		q.put2DRangeKernel(kDownsampleFloat, 0, 0, clsrc.width, clsrc.height, 0, 0);

		q.finish();
		long t1 = System.nanoTime();
		System.out.println("Resample.main() took " + (t1 - t0) / 1000 / 1000 + "ms");
		System.out.println("Resample.downSampleImage() kernel finished!");

		// now convert the result back to BufferedImage
		BufferedImage bfrescale = ImageLoader.getImage(q, cldst);

		cldst.release();
		return bfrescale;
	}

	@SuppressWarnings("unchecked")
	public BufferedImage downSampleImageInt(BufferedImage bf, int k) {
		// allocate source image on device
		long t0 = System.nanoTime();

		if (clsrc == null) {
			clsrc = (CLImage2d<ByteBuffer>) cl.createImage2d(bf.getWidth(), bf.getHeight(), ImageFormats.R_UINT8);
			ImageLoader.putImage(q, bf, clsrc);
		}
		// allocate result image
		int newWidth = (int) (bf.getWidth() / k);
		int newHeight = (int) (bf.getHeight() / k);
		CLImage2d<IntBuffer> cldst = (CLImage2d<IntBuffer>) cl.createImage2d(newWidth, newHeight,
				ImageFormats.RGBA_UINT8);
		System.out.println("Resample.downSampleImage() resampling to " + newWidth + "x" + newHeight);
		kDownsampleK.setArg(0, clsrc);
		kDownsampleK.setArg(1, cldst);
		kDownsampleK.setArg(2, k);

		q.put2DRangeKernel(kDownsampleK, 0, 0, clsrc.width, clsrc.height, 0, 0);

		q.finish();
		long t1 = System.nanoTime();
		System.out.println("Resample.main() took " + (t1 - t0) / 1000 / 1000 + "ms");
		System.out.println("Resample.downSampleImage() kernel finished!");

		// now convert the result back to BufferedImage
		BufferedImage bfrescale = ImageLoader.getImage(q, cldst);

		cldst.release();
		return bfrescale;
	}

	public void downSampleImageAttributed(BufferedImage bf) {
		this.toDisplay = downSampleImage(bf);
	}

	@SuppressWarnings("unchecked")
	public BufferedImage downSampleImage(BufferedImage bf) {
		if (clsrc == null) {
			clsrc = (CLImage2d<ByteBuffer>) cl.createImage2d(bf.getWidth(), bf.getHeight(), ImageFormats.RGBA_UINT8);
			ImageLoader.putImage(q, bf, clsrc);
		}

		// this.toDisplay=bf;
		// this.displayImage();
		//
		int newWidth = 10;
		int newHeight = 10;
		CLImage2d<IntBuffer> cldst = (CLImage2d<IntBuffer>) cl.createImage2d(newWidth, newHeight, ImageFormats.R_UINT8);
		System.out.println("Resample.downSampleImage() resampling to " + newWidth + "x" + newHeight);
		kDownsample.setArg(0, clsrc);
		kDownsample.setArg(1, cldst);

		q.put2DRangeKernel(kDownsample, 0, 0, clsrc.width, clsrc.height, 0, 0);

		q.finish();
		// now convert the result back to BufferedImage
		BufferedImage bfrescale = ImageLoader.getImage(q, cldst);

		cldst.release();
		return bfrescale;

	}

	public void displayImage() {
		// final BufferedImage dst = new BufferedImage(bf.getWidth(),
		// bf.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D gg = toDisplay.createGraphics();
		gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		gg.drawImage(toDisplay, 0, 0, null);
		// drawFeatures(gg, surf, id);

		// display results
		if (frame == null) {
			frame = new JFrame();
			label = new JLabel(new ImageIcon(toDisplay));
			frame.add(label);
			frame.addMouseWheelListener(new MouseWheelListener() {

				@Override
				public void mouseWheelMoved(MouseWheelEvent e) {
					// System.out.println("Resample.displayImage(...).new MouseWheelListener() {...}.mouseWheelMoved() "
					// + e);
					int rotation = e.getWheelRotation();
					System.out
							.println("Resample.displayImage().new MouseWheelListener() {...}.mouseWheelMoved() got rotation "
									+ rotation);
					Resample.this.scale += rotation;// / 10.0f;
					// if (Resample.this.scale < 1 ) {
					// Resample.this.scale = 1;
					// }
					Resample.this.downSampleImageAttributed(Resample.this.original, Resample.this.scale);
					label.setIcon(new ImageIcon(toDisplay));
					label.resize(toDisplay.getWidth(), toDisplay.getHeight());
				}
			});
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		}

		frame.pack();
		frame.setVisible(true);

	}

	public void initializeCL() {
		SOCLEContext sc = new SOCLEContext(CLContext.create(CLDevice.Type.GPU));
		System.out.println("SurfGenerator.initializeCL() Max work group size : "
				+ +sc.getDevice().getMaxWorkGroupSize());
		this.maxWidth = sc.getDevice().getMaxImage2dWidth();
		this.maxHeight = sc.getDevice().getMaxImage2dHeight();

		System.out.println("Device Max image size " + maxWidth + "x" + maxHeight);
		cl = sc.cl;
		q = sc.getQ();

	}

	public void saveCurrentImageToDisk(String fileName) throws IOException {
		
		File file = new File(fileName);
		file.createNewFile();
		ImageIO.write(toDisplay, "png", file);
	}
	
	public static void main(String[] args) throws IOException {
		Resample rs = new Resample();
		String imagePath = "/user/fhuet/desktop/home/workspaces/rechercheefficaceimagessimilaires/images/test.jpg";
		if (args.length > 0) {
			imagePath = args[0];
		}

		rs.original = ImageIO.read(new File(imagePath));
		// String s[] = ImageIO.getReaderFormatNames();
		// for (int i = 0; i < s.length; i++) {
		// String string = s[i];
		// System.out.println("Resample.main() " + string);
		//
		// }
		// rs.displayImage(bf);
		// rs.displayImage(rs.downSampleImage(bf));
		System.out.println("Resample.main() Image format is " + rs.original.getType());

		System.out.println("Resample.main() Type TYPE_INT_RGB : " + BufferedImage.TYPE_INT_RGB);
		System.out.println("Resample.main() Type TYPE_INT_ARGB : " + BufferedImage.TYPE_INT_ARGB);
		System.out.println("Resample.main() Type TYPE_INT_ARGB_PRE : " + BufferedImage.TYPE_INT_ARGB_PRE);
		System.out.println("Resample.main() Type TYPE_INT_BGR : " + BufferedImage.TYPE_INT_BGR);
		System.out.println("Resample.main() Type TYPE_3BYTE_BGR : " + BufferedImage.TYPE_3BYTE_BGR);
		System.out.println("Resample.main() Type TYPE_BYTE_GRAY : " + BufferedImage.TYPE_BYTE_GRAY);

		// , , , TYPE_INT_BGR, TYPE_3BYTE_BGR, TYPE_4BYTE_ABGR,
		// TYPE_4BYTE_ABGR_PRE, TYPE_BYTE_GRAY, TYPE_BYTE_BINARY,
		// TYPE_BYTE_INDEXED, TYPE_USHORT_GRAY, TYPE_USHORT_565_RGB,
		// TYPE_USHORT_555_RGB, TYPE_CUSTOM

		// rs.downSampleImage(rs.original);

		// long t0 = System.nanoTime();
		// BufferedImage result = rs.downSampleImage(rs.original, 1);
		rs.downSampleImageAttributed(rs.original);
		// long t1= System.nanoTime();
		// System.out.println("Resample.main() took " +(t1-t0)/1000/1000 +
		// "ms");
		// rs.toDisplay=rs.original;
		
		rs.saveCurrentImageToDisk("/user/fhuet/desktop/home/workspaces/rechercheefficaceimagessimilaires/images/tn/test.jpg");
		
		rs.displayImage();

	}

}

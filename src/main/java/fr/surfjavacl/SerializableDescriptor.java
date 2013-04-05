package fr.surfjavacl;

import java.io.Serializable;
import java.nio.FloatBuffer;

public class SerializableDescriptor implements Serializable {

	protected String fileName;
	protected int count;
	protected float[] buffer;

	public SerializableDescriptor() {

	}

	public SerializableDescriptor(String fileName, int count, FloatBuffer buffer) {
		super();
		this.fileName = fileName;
		this.count = count;
		this.setBuffer(buffer);
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public FloatBuffer getBuffer() {

		return FloatBuffer.wrap(buffer);
	}

	public void setBuffer(FloatBuffer buf) {
		this.buffer = new float[count * 64];
		int total = 0;
		int last = 0;
		try {
			buf.position(0);
			for (int i = 0; i < count * 64; i++) {
				total++;
				this.buffer[i] = buf.get();
			last = i;

			}
		} catch (RuntimeException e) {
			//e.printStackTrace();
			throw (e);

		} finally {
			System.out.println("SerializableDescriptor.setBuffer() count " + count);
			System.out.println("SerializableDescriptor.setBuffer() count*64 " + (count*64));
			System.out.println("SerializableDescriptor.setBuffer() total/64 " + (total / 64));
			System.out.println("SerializableDescriptor.setBuffer() buffer position " + buf.position());
			System.out.println("SerializableDescriptor.setBuffer() buffer last pos requested " + last);
		}
		
		
		
		
		// this.buffer = buffer.array();
	}

}

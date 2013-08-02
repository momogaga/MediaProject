import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;

import org.apache.hadoop.io.ArrayWritable;
import org.apache.hadoop.io.ByteWritable;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.ObjectWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.io.WritableUtils;

public class NPhase1Value implements WritableComparable<NPhase1Value>,
		Serializable {

	private String recordID;
	// private ArrayWritable coordinates;

	protected ArrayList<float[]> coordinates;
	// protected ObjectWritable coordinates;

	private ByteWritable fileID;

	public NPhase1Value() {
		this.recordID = ""; //new Text();
		// this.coordinates = new ArrayWritable(FloatWritable.class);
		this.fileID = new ByteWritable();
	}

	/**
	 * 
	 * @param recIdInt
	 *            record ID
	 * @param second
	 * @param third
	 * @param dimension
	 */
	public NPhase1Value(String recIdInt, ArrayList<float[]> second, byte third,
			int dimension) {
		set(new Text(recIdInt), second, new ByteWritable(third), dimension);
	}

	public void set(Text first, ArrayList<float[]> second, ByteWritable third,
			int dimension) {
		this.recordID = first.toString();
		this.fileID = third;

		// FloatWritable[] floatArray = new FloatWritable[dimension];
		// for (int i = 0; i < dimension; i++){
		// floatArray[i] = new FloatWritable(second[i]);
		// }
		// this.coordinates = new ObjectWritable(second);// new
		// ArrayWritable(FloatWritable.class,
		// floatArray);
		this.coordinates = second;
	}

	public Text getRecordID() {
		return new Text(recordID);
	}

	public ArrayList<float[]> getCoordinates() {
		// return (ArrayList<float[]>) coordinates.get();
		return coordinates;
	}

	public ByteWritable getFileID() {
		return fileID;
	}

	@Override
	public void write(DataOutput out) throws IOException {
		new Text(recordID).write(out);
		// System.out.println("NPhase1Value.write() recordID " + recordID );
		new IntWritable(this.getCoordinates().size()).write(out);

		for (float[] t : this.getCoordinates()) {

			for (int i = 0; i < 64; i++) {
				FloatWritable tmp = new FloatWritable(t[i]);// .write(out);
				// tmp.set(t);
				tmp.write(out);
			}

			// .write(out);

		}
		// coordinates.write(out);
		fileID.write(out);
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		Text tex = new Text();
		tex.readFields(in);
		recordID = tex.toString();
		// System.out.println("NPhase1Value.readFields() recordID " + recordID
		// );
		IntWritable intw = new IntWritable();
		intw.readFields(in);
		int size = intw.get();
		// System.out.println("NPhase1Value.readFields() " + size);
		ArrayList<float[]> al = new ArrayList<float[]>();
		for (int i = 0; i < size; i++) {
			float[] t = new float[64];

			for (int j = 0; j < 64; j++) {
				// ObjectWritable tmp = new ObjectWritable();
				FloatWritable tmp = new FloatWritable();
				tmp.readFields(in);
				t[j] = tmp.get();
				// tmp.readFields(in);
				// al.add((float[]) tmp.get());
				// coordinates.readFields(in);
			}
			al.add(t);
		}
		// this.coordinates = new ObjectWritable(al);
		this.coordinates = al;
		fileID.readFields(in);

	}

//	@Override
//	public boolean equals(Object o) {
//		if (o instanceof NPhase1Value) {
//			NPhase1Value np1v = (NPhase1Value) o;
//			return recordID.equals(np1v.recordID)
//					&& coordinates.equals(np1v.coordinates)
//					&& fileID.equals(np1v.fileID);
//		}
//		return false;
//	}

	@Override
	public String toString() {
		// int dimension = 2;
		String result;
		result = recordID.toString() + " ";

		// ArrayList<float[]> tab = (ArrayList<float[]>) coordinates.get();
		for (float[] parts : coordinates) {
			for (int i = 0; i < 64; i++)
				result = result + parts[i] + " ";
		}
		return result + fileID.toString();
	}

	public String toString(int dimension) {
		String result;
		result = recordID.toString() + " ";

		// ArrayList<float[]> tab = (ArrayList<float[]>) coordinates.get();
		for (float[] parts : coordinates) {
			for (int i = 0; i < dimension; i++)
				result = result + parts[i] + " ";
		}
		return result + fileID.toString();
	}

	public String getDescriptorsAsString() {
		StringBuilder record1 = new StringBuilder();
		ArrayList<float[]> al = getCoordinates();
		int max = al.size();
		for (float[] des : al) {
			for (int i = 0; i < des.length - 1; i++) {
				record1.append(des[i]);
				record1.append(",");
			}
			record1.append(des[des.length - 1]);
			if (max > 1) {
				record1.append(";");
				max--;
			}
		}
		return record1.toString();
	}

	@Override
	public int compareTo(NPhase1Value np1v) {
//		System.out.println("NPhase1Value.compareTo()");
		return 1;
	}

}

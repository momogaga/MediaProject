import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;

public class ImageDescriptor implements WritableComparable<ImageDescriptor> {

	protected String path;
	protected long descriptorsOffset;
//	protected long descriptorsSize;

	
	public ImageDescriptor() {}
	
	public ImageDescriptor(String path, long descriptorsOffset) //,
//			long descriptorsSize) {
	{
		super();
		this.path = path;
		this.descriptorsOffset = descriptorsOffset;
//		this.descriptorsSize = descriptorsSize;
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		Text tex = new Text();
		tex.readFields(in);
		path = tex.toString();
		
		LongWritable lw = new LongWritable();
		lw.readFields(in);
		descriptorsOffset= lw.get();
		
//		lw = new LongWritable();
//		lw.readFields(in);
	//	descriptorsSize= lw.get();
		
		
	
	}

	@Override
	public void write(DataOutput out) throws IOException {
		new Text(path).write(out);
		new LongWritable(this.descriptorsOffset).write(out);

//		new LongWritable(this.descriptorsSize).write(out);
	}

	@Override
	public int compareTo(ImageDescriptor o) {
		return path.compareTo(o.getPath());
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public long getDescriptorsOffset() {
		return descriptorsOffset;
	}

	public void setDescriptorsOffset(long descriptorsOffset) {
		this.descriptorsOffset = descriptorsOffset;
	}

//	public long getDescriptorsSize() {
//		return descriptorsSize;
//	}
//
//	public void setDescriptorsSize(long descriptorsSize) {
//		this.descriptorsSize = descriptorsSize;
//	}

}

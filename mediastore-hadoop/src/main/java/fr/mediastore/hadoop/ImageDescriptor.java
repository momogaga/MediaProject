package fr.mediastore.hadoop;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;

public class ImageDescriptor implements WritableComparable<ImageDescriptor> {

	protected long descriptorsOffset;
    protected String path;
//	protected long descriptorsSize;

	
	public ImageDescriptor() {}
	
	public ImageDescriptor(String path,  long descriptorsOffset) //,
//			long descriptorsSize) {
	{
		super();

		this.descriptorsOffset = descriptorsOffset;
        this.path = path;
    }

	@Override
	public void readFields(DataInput in) throws IOException {

//		Text tex = new Text();
//		tex.readFields(in);
	//	path = tex.toString();
		
		LongWritable lw = new LongWritable();
		lw.readFields(in);
		descriptorsOffset= lw.get();

        Text t = new Text();
        t.readFields(in);

        this.path = t.toString();
		
//		lw = new LongWritable();
//		lw.readFields(in);
	//	descriptorsSize= lw.get();
		
		
	
	}

	@Override
	public void write(DataOutput out) throws IOException {

		new LongWritable(this.descriptorsOffset).write(out);
        new Text(this.path).write(out);

//		new LongWritable(this.descriptorsSize).write(out);
	}

	@Override
	public int compareTo(ImageDescriptor o) {
		return  path.compareTo(o.getPath());
	}



	public long getDescriptorsOffset() {
		return descriptorsOffset;
	}

	public void setDescriptorsOffset(long descriptorsOffset) {
		this.descriptorsOffset = descriptorsOffset;
	}

    public String getPath() {
        return this.path;
    }

//	public long getDescriptorsSize() {
//		return descriptorsSize;
//	}
//
//	public void setDescriptorsSize(long descriptorsSize) {
//		this.descriptorsSize = descriptorsSize;
//	}

}

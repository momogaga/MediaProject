package fr.mediastore.hadoop;

import java.io.IOException;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;


public class FilterJob {
	
	public static class MapClass extends
	Mapper<LongWritable, Text, Text, IntWritable> {

		@Override
		protected void map(LongWritable key, Text value, Context context)
				throws IOException, InterruptedException {
		
			//value is in the form path1;path2
			//sort them lexicographically and output as key
			String[] paths = value.toString().split(";");
			Text val = new Text();
			
			if (paths[0].compareTo(paths[1])<0) {
				val.set(paths[0]+";"+paths[1]);
			} else {
				val.set(paths[0]+";"+paths[1]);	
			}
		
			context.write(val, new IntWritable(0));
		}
		
		
	}
	
	public static class Reduce 	extends 	Reducer<Text, IntWritable, Text, NullWritable> {

		@Override
		protected void reduce(Text arg0, Iterable<IntWritable> arg1, Context context)
				throws IOException, InterruptedException {
			//super.reduce(arg0, arg1, arg2);
			context.write(arg0, NullWritable.get());
		}
	
	}
	
	

}

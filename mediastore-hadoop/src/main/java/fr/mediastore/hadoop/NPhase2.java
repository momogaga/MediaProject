package fr.mediastore.hadoop;/*
 * THERE IS NO WARRANTY FOR THE PROGRAM, TO THE EXTENT PERMITTED BY APPLICABLE LAW.
 * EXCEPT WHEN OTHERWISE STATED IN WRITING THE COPYRIGHT HOLDERS AND/OR OTHER
 * PARTIES PROVIDE THE PROGRAM "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER
 * EXPRESSED OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE ENTIRE RISK AS
 * TO THE QUALITY AND PERFORMANCE OF THE PROGRAM IS WITH YOU. SHOULD THE PROGRAM
 * PROVE DEFECTIVE, YOU ASSUME THE COST OF ALL NECESSARY SERVICING, REPAIR
 * OR CORRECTION.
 */

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

/**
 * Phase2 of Hadoop Block Nested Loop KNN Join (H-BNLJ).
 */
public class NPhase2 extends Configured implements Tool {
	@SuppressWarnings("deprecation")
	public static class MapClass extends MapReduceBase implements
			Mapper<LongWritable, Text, Text, FloatWritable> {

		public void map(LongWritable key, Text value,
				OutputCollector<Text, FloatWritable> output, Reporter reporter)
				throws IOException {

			String line = value.toString();
			String[] parts = line.split(";");
			
			// key format <id1,id2>	
			// value format <distance>
			//on ecrit les id's suivant l'ordre lexicographique
			Text mapKey = null;
			if (parts[0].compareTo(parts[1])<0) {
				mapKey = new Text(parts[0] + ";" +parts[1]);
			} 
			else {
				mapKey = new Text(parts[1] + ";" +parts[0]);
			}
			
			
			FloatWritable mapValue = new FloatWritable(Float.parseFloat(parts[2]));
			//le collect donne au reducer le fichier splite.
			//on ne fait le collecte que si les deux id's sont diiferents
			if(!parts[0].equals(parts[1])){
				output.collect(mapKey, mapValue);
			}
			
//			System.out.println("mapkey" + " " + mapKey + " mapValue" + mapValue);
		}
	}

	public static class Reduce extends MapReduceBase implements
			Reducer<Text, FloatWritable, NullWritable, Text> {

		int numberOfPartition;
		int knn;
		int fixedK;
		

		public void configure(JobConf job) {
			numberOfPartition = job.getInt("numberOfPartition", 2);
			knn = job.getInt("knn", 3);
			fixedK = job.getInt("fixedK", 5);

		}

		public void reduce(Text key, Iterator<FloatWritable> values,
				OutputCollector<NullWritable, Text> output, Reporter reporter)
				throws IOException {

			
			float distMin = Float.MAX_VALUE;
			float dist;
			int cptk =0;

			// For each record we have a reduce task
			// value format <id1 id2, distance>

			while (values.hasNext()) {
				FloatWritable reduceValue = values.next();
				
				
				dist = reduceValue.get();

				if (dist < distMin) {

					distMin = dist;
	
				}
				
				cptk++;

			}
			if(cptk >= fixedK){
				output.collect(NullWritable.get(), new Text(key.toString() + ";" + Float.toString(distMin)));
			}
			

			// break; // only ouput the first record

		} // reduce
	} // Reducer

	static int printUsage() {
		System.out
				.println("fr.mediastore.hadoop.NPhase1 [-m <maps>] [-r <reduces>] [-p <numberOfPartitions>] "
						+ "[-k <knn>] " + "<input> <output>");
		ToolRunner.printGenericCommandUsage(System.out);
		return -1;
	}

	/**
	 * The main driver for H-BNLJ program. Invoke this method to submit the
	 * map/reduce job.
	 * 
	 * @throws IOException
	 *             When there is communication problems with the job tracker.
	 */
	public int run(String[] args) throws Exception {
		JobConf conf = new JobConf(getConf(), NPhase2.class);
		conf.setJobName("fr.mediastore.hadoop.NPhase2");

		conf.setMapperClass(MapClass.class);
		conf.setOutputKeyClass(Text.class);
		conf.setOutputValueClass(FloatWritable.class);
		conf.setReducerClass(Reduce.class);

		// int numberOfPartition = 0;
		List<String> other_args = new ArrayList<String>();

		for (int i = 0; i < args.length; ++i) {
			try {
				if ("-m".equals(args[i])) {
					// conf.setNumMapTasks(Integer.parseInt(args[++i]));
					++i;
				} else if ("-r".equals(args[i])) {
					conf.setNumReduceTasks(Integer.parseInt(args[++i]));
					// conf.setNumReduceTasks(20);
					// } else if ("-p".equals(args[i])) {
					// numberOfPartition = Integer.parseInt(args[++i]);
					// conf.setInt("numberOfPartition", numberOfPartition);
				} else if ("-k".equals(args[i])) {
					int knn = Integer.parseInt(args[++i]);
					conf.setInt("knn", knn);
//					System.out.println(knn);
				}
				else if ("-n".equals(args[i])) {
					conf.setInt("fixedK", Integer.parseInt(args[++i]));
				}
				
				else {
					other_args.add(args[i]);
				}
				// conf.setNumReduceTasks(numberOfPartition *
				// numberOfPartition);
				// conf.setNumReduceTasks(1);
			} catch (NumberFormatException except) {
				System.out.println("ERROR: Integer expected instead of "
						+ args[i]);
				return printUsage();
			} catch (ArrayIndexOutOfBoundsException except) {
				System.out.println("ERROR: Required parameter missing from "
						+ args[i - 1]);
				return printUsage();
			}
		}

		// Make sure there are exactly 2 parameters left.
		if (other_args.size() != 2) {
			System.out.println("ERROR: Wrong number of parameters: "
					+ other_args.size() + " instead of 2.");
			return printUsage();
		}

		FileInputFormat.setInputPaths(conf, other_args.get(0));
		FileOutputFormat.setOutputPath(conf, new Path(other_args.get(1)));

		JobClient.runJob(conf);
		return 0;
	}

	public static void main(String[] args) throws Exception {
		int res = ToolRunner.run(new Configuration(), new NPhase2(), args);
		System.exit(res);
	}
} // fr.mediastore.hadoop.NPhase2


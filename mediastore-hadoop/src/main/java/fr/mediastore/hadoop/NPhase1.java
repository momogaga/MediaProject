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
import java.util.Random;

import fr.thumbnailsdb.ImageComparator;
import fr.thumbnailsdb.Utils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocalDirAllocator;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.LineReader;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.shiftone.cache.Cache;
import org.shiftone.cache.policy.lfu.LfuCacheFactory;

/**
 * Phase1 of Hadoop Block Nested Loop KNN Join (H-BNLJ).
 */
public class NPhase1 extends Configured implements Tool {

	private static boolean caching = true;

	protected static Cache cache = null;

	
	public static class CustomPartitioner extends Partitioner<IntWritable, ImageDescriptor> {

		@Override
		public int getPartition(IntWritable arg0, ImageDescriptor arg1, int arg2) {
			return arg0.get();
		}
		
	}
	
	
	public static class MapClass extends
			Mapper<LongWritable, Text, IntWritable, ImageDescriptor> {

		private int numberOfPartition;

		private int scale = 1;
		private int fileId = 0;
		private String inputFile;
		private String mapTaskId;
		private Random r;
		private int recIdOffset;
		private int coordOffset;

		public void setup(Context context) {
			Configuration configuration = context.getConfiguration();
			inputFile = ((FileSplit) context.getInputSplit()).getPath()
					.toString();

			// mapTaskId = job.get("mapred.task.id");
			numberOfPartition = configuration.getInt("numberOfPartition", 20);

			//dimension = configuration.getInt("dimension", 64);

			System.out.println("fr.mediastore.hadoop.NPhase1.MapClass.setup() number of partitions "
					+ numberOfPartition);

			recIdOffset = 0;
			coordOffset = recIdOffset + 1;

			r = new Random();

			if (inputFile.indexOf("outer") != -1)
				fileId = 0;
			else if (inputFile.indexOf("inner") != -1)
				fileId = 1;
			else {
				System.out.println("Invalid input file source@fr.mediastore.hadoop.NPhase1");
				System.exit(-1);
			}
		} // configure

		/**
		 * Partition the input data sets (R and S) into multiple buckets.
		 */
		public void map(LongWritable key, Text value, Context context)
				throws IOException {
            //values are id,base64Data or path,id,base64Data
			String[] parts = value.toString().split(",");
            String recId = null;
            String path = null;
            ImageDescriptor imgDes = null;
            long offset = 0;


                recId= parts[0];
                offset = key.get() + recId.length()+1;
                imgDes = new ImageDescriptor(recId, offset);// , 0);


			//String recIdInt = (recId);

			int partID;
			int groupID;

//			if (recId == null || recId.isEmpty()) {
//				System.exit(-1);
//			}


			partID = r.nextInt(numberOfPartition);
			groupID = 0;

			for (int i = 0; i < numberOfPartition; i++) {
				// partID = r.nextInt(numberOfPartition * numberOfPartition);
				// System.out.println(numberOfPartition);
				if (fileId == 0) {
					groupID = partID * numberOfPartition + i;

				} else if (fileId == 1) {
					groupID = partID + i * numberOfPartition;

				} else {
					System.out.println("The record comes from unknow file!!!");
					System.exit(-1);
				}

				IntWritable mapKey = new IntWritable(groupID);
				try {
//					System.out.println("fr.mediastore.hadoop.NPhase1.MapClass.map() " + mapKey
//							+ "  " + imgDes.getPath() + " "
//							+ imgDes.getDescriptorsOffset());
					context.write(mapKey, imgDes);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

		} // map
	} // MapClass

	/**
	 * Perform Block Nested Loop join for records in the same partition/bucket.
	 */
	public static class Reduce
			extends
			org.apache.hadoop.mapreduce.Reducer<IntWritable, ImageDescriptor, NullWritable, Text> {

		//private int bufferSize = 8 * 1024 * 1024;
		// private MultipleOutputs mos;

		private LocalDirAllocator lDirAlloc = new LocalDirAllocator(
				"mapred.local.dir");
		private FSDataOutputStream out;
//		private FileSystem localFs;
//		private FileSystem lfs;
//		private Path file1;
//		private Path file2;

	//	private int numberOfPartition;
		private int dimension;
	//	private int blockSize;
		private int knn;
		private float distmin;

		// private boolean self_join;

		private Configuration jobinfo;

		protected Path[] inputPaths;

		protected FileSystem fs;

		// String inputName = conf.get("map.input.file");

		public void setup(Context c) {
		//	numberOfPartition = c.getConfiguration().getInt(
		//			"numberOfPartition", 2);
			dimension = c.getConfiguration().getInt("dimension", 2);
		//	blockSize = c.getConfiguration().getInt("blockSize", 1024);
			knn = c.getConfiguration().getInt("knn", 20);
			distmin = c.getConfiguration().getFloat("distmin", (float) 0.1);
			// self_join = Boolean.valueOf(job.get("self_join"));
			inputPaths = FileInputFormat.getInputPaths(c);

			try {
				fs = FileSystem.get(c.getConfiguration());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		public void reduce(IntWritable key, Iterable<ImageDescriptor> values,
				Context context) {
			ArrayList<ImageDescriptor> al = new ArrayList<ImageDescriptor>();

            int doneSinceLastProgress=0;

			if (NPhase1.caching) {
				System.out.println("fr.mediastore.hadoop.NPhase1.Reduce.reduce() caching Enabled");
			} else {
				System.out.println("fr.mediastore.hadoop.NPhase1.Reduce.reduce() caching Disabled");
			}
			
			FSDataInputStream in = null;
			try {
				in = fs.open(inputPaths[0]);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			ImageDescriptor v = null;

			for (ImageDescriptor tmp : values) {
				v = new ImageDescriptor(tmp.getPath(),
						tmp.getDescriptorsOffset()); // ,
				al.add(v);

			}

			ImageDescriptor[] tab = al.toArray(new ImageDescriptor[] {});
//            System.out.println("NPhase1$Reduce.reduce key: " + key);
            System.out.println("NPhase1$Reduce.reduce Nb imageDescriptors: " + tab.length);
			for (int i = 0; i < tab.length - 1; i++) {
				// StringBuilder line = null;
				ImageDescriptor source = tab[i];

				System.out.println("fr.mediastore.hadoop.NPhase1.Reduce.reduce() done " + i
						* 100 / (tab.length - 1) + " %");
				for (int j = i + 1; j < tab.length; j++) {				
					ImageDescriptor cible = tab[j];
                    if (!source.getPath().equals(cible.getPath())) {
						computeDistance(source, cible, in, context);
					}
                    if (doneSinceLastProgress++>1000) {
                        context.progress();

                        doneSinceLastProgress=0;
                        System.out.println("  --- done 1000");
                    }

				}
			}

		}


		protected float distance(float[] desc1, float[] desc2) {

			float result = 0;

			for (int i = 0; i < desc1.length; i++) {
				result += (desc1[i] - desc2[i]) * (desc1[i] - desc2[i]);
			}

			return result;

		}

		protected int[]  extractBase64(ImageDescriptor img,
                                       FSDataInputStream in) {
			//ArrayList<float[]> descriptorsList = null;
            int[] base64 = null;

			//System.out.println("fr.mediastore.hadoop.NPhase1.Reduce.extractBase64()");
			if (NPhase1.caching) {
				if (NPhase1.cache == null) {
					// cache for 100 entries, maximum 1000 secondes between
					// cleanups
					cache = new LfuCacheFactory().newInstance("descriptors",
							1000 * 1000, 500);
				}

                base64 = (int[]) cache.getObject(img.getPath());
			}

			// either no caching or not found in cache
			if (!NPhase1.caching || base64 == null) {
//				base64 = new String(); // = new ArrayList<float[]>();
				//String dSource = null;
				try {
					in.seek(img.getDescriptorsOffset());
					LineReader ln = new LineReader(in);
					Text t = new Text();
					ln.readLine(t);
					base64 = Utils.base64ImgToIntArray(t.toString());
				
				} catch (IOException e) {
					e.printStackTrace();
				}

				if (NPhase1.caching) {
					cache.addObject(img.getPath(), base64);
				}
			}

			return base64;

		}

		protected void computeDistance(ImageDescriptor source,
				ImageDescriptor cible, FSDataInputStream in, Context context) {


            int[] b1 = this.extractBase64(source,in);
            int[] b2 = this.extractBase64(cible,in);

//            System.out.println("NPhase1$Reduce.computeDistance");
//            System.out.println("      "  + b1);
//            System.out.println("      " + b2);
            double result = ImageComparator.compareRGBUsingRMSE(b1,b2);

            if (result < 5) {
                try {


                    context.write(NullWritable.get(), new Text(source.getPath()    +
                                ";" + cible.getPath()));

                } catch (IOException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                } catch (InterruptedException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
//
//
//
//			ArrayList<float[]> descriptorsList1 = this.extractBase64(
//					source, in, context);
//			ArrayList<float[]> descriptorsList2 = this.extractBase64(
//					cible, in, context);
//
//
//
//			int cpt = 0;
//			boolean iMatch = false;
//			boolean dMatch = false;
//			Iterator<float[]> i1 = descriptorsList1.iterator();
//			Iterator<float[]> i2 = descriptorsList2.iterator();
//			while ((!iMatch) && i1.hasNext()) {
//				dMatch = false;
//				float[] d1tmp = i1.next();
//
//				while ((!dMatch) && i2.hasNext()) {
//					float[] d2tmp = (float[]) i2.next();
//					float distance = distance(d1tmp, d2tmp);
////					 System.out.println("fr.mediastore.hadoop.NPhase1.Reduce.computeDistance() "
////					 + distance + " compare to " + distmin);
//
//					if (distance < distmin) {
////						 System.out
////						 .println("fr.mediastore.hadoop.NPhase1.Reduce.computeDistance() it 's a match!");
//						cpt++;
//						dMatch = true;
//					}
//				}
//				context.progress();
//				if (cpt >= knn) {
//					iMatch = true;
//				}
//			}
//
//			if (iMatch) {
////				 System.out
////				 .println("fr.mediastore.hadoop.NPhase1.Reduce.computeDistance() found match...");
//
//				try {
//					context.write(NullWritable.get(), new Text(source.getPath()
//							+ ";" + cible.getPath()));
//				} catch (IOException e) {
//					e.printStackTrace();
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//
//			}

		}

	} // Reducer

	static int printUsage() {
		System.out
				.println("fr.mediastore.hadoop.NPhase1 [-m <maps>]  [-p <numberOfPartitions>] "
						+ "[-d <dimension>] [-k <knn>] [-b <blockSize(#records) for R>] "
						+ "<input (R)> <input (S)> <output>");
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
	@SuppressWarnings("deprecation")
	public int run(String[] args) throws Exception {
		runPhase1(args);
	//	runPhase2();
		return 0;
	} // run

	protected void runPhase1(String[] args) throws IOException,
			InterruptedException, ClassNotFoundException {
		int numberOfPartition = 20;
		int numberOfReduceTasks = 10;
		// boolean self_join = false;

		Configuration conf = new Configuration();

		// conf.setProfileEnabled(true);
		// conf.setProfileParams("-agentlib:hprof=cpu=samples,depth=6,heap=sites,force=n,thread=y,verbose=n,file=%s");
		// conf.setProfileTaskRange(true, "0-5");

		List<String> other_args = new ArrayList<String>();
		for (int i = 0; i < args.length; ++i) {
			try {
				if ("-m".equals(args[i])) {
//					job.setNumMapTasks(Integer.parseInt(args[++i]));

					// job.set
					++i;
				} else if ("-r".equals(args[i])) {
					numberOfReduceTasks = Integer.parseInt(args[++i]);
					// job.setNumReduceTasks(Integer.parseInt(args[++i]));
				} else if ("-p".equals(args[i])) {
					numberOfPartition = Integer.parseInt(args[++i]);
					conf.setInt("numberOfPartition", numberOfPartition);
				} else if ("-d".equals(args[i])) {
					conf.setInt("dimension", Integer.parseInt(args[++i]));
				} else if ("-k".equals(args[i])) {
					conf.setInt("knn", Integer.parseInt(args[++i]));
				} else if ("-n".equals(args[i])) {
					conf.setFloat("distmin", Float.parseFloat(args[++i]));
				}

				else if ("-b".equals(args[i])) {
					conf.setInt("blockSize", Integer.parseInt(args[++i]));
					/*
					 * } else if ("-sj".equals(args[i])) { self_join =
					 * Boolean.parseBoolean(args[++i]); conf.set("self_join",
					 * Boolean.toString(self_join));
					 */} else {
					other_args.add(args[i]);
				}
				// set the number of reducers
				// conf.setNumReduceTasks(numberOfPartition *
				// numberOfPartition);

			} catch (NumberFormatException except) {
				System.out.println("ERROR: Integer expected instead of "
						+ args[i]);
				printUsage();
			} catch (ArrayIndexOutOfBoundsException except) {
				System.out.println("ERROR: Required parameter missing from "
						+ args[i - 1]);
				printUsage();
			}
		}
		// conf.setNumReduceTasks(0);

		if (other_args.size() != 3) {
			System.out.println("ERROR: Wrong number of parameters: "
					+ other_args.size() + " instead of 3.");
			printUsage();
		}

		// conf.set                            i

		JobConf j = new JobConf(conf);
//		j.setProfileEnabled(true);
//		j.setProfileParams("-agentlib:hprof=cpu=samples,depth=6,heap=sites,force=n,thread=y,verbose=n,file=%s");
//		j.setProfileTaskRange(false, "1-10");
		Job job = new Job(j);
		job.setJarByClass(NPhase1.class);

		// JobConf conf = new JobConf(getConf(), fr.mediastore.hadoop.NPhase1.class);
		job.setJobName("k-NN");

		job.setMapperClass(MapClass.class);
		job.setReducerClass(Reduce.class);
		job.setOutputKeyClass(IntWritable.class);
		job.setOutputValueClass(ImageDescriptor.class);
//		job.setNumReduceTasks(numberOfReduceTasks);
		job.setNumReduceTasks(numberOfPartition*numberOfPartition);
		//job.setPartitionerClass(CustomPartitioner.class);

		System.out
				.println("fr.mediastore.hadoop.NPhase1.run() base input path " + other_args.get(0));

		FileInputFormat.setInputPaths(job, other_args.get(0));
		System.out.println("fr.mediastore.hadoop.NPhase1.run() added input path "
				+ other_args.get(1));

		System.out.println("fr.mediastore.hadoop.NPhase1.runPhase1() numberOfPartition "
				+ numberOfPartition);

		FileInputFormat.addInputPaths(job, other_args.get(1));
		FileOutputFormat.setOutputPath(job, new Path(other_args.get(2)));

		job.waitForCompletion(true);
	}

	protected void runPhase2() throws IOException, ClassNotFoundException,
			InterruptedException {
		Configuration conf = new Configuration();
		Job job = new Job(conf);
		job.setJarByClass(FilterJob.class);
		job.setMapperClass(FilterJob.MapClass.class);

		job.setJobName("fr.mediastore.hadoop.FilterJob");

		job.setReducerClass(FilterJob.Reduce.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(IntWritable.class);

		FileInputFormat.addInputPath(job, new Path("/user/fhuet/phase1"));
		FileOutputFormat.setOutputPath(job, new Path("/user/fhuet/phase2"));

		job.setNumReduceTasks(10);
		job.waitForCompletion(true);
	}

	
	
	public static void main(String[] args) throws Exception {
		int res = ToolRunner.run(new Configuration(), new NPhase1(), args);
		System.exit(res);
	}

} // fr.mediastore.hadoop.NPhase1

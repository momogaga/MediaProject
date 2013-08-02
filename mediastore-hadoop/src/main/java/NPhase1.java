/*
 * THERE IS NO WARRANTY FOR THE PROGRAM, TO THE EXTENT PERMITTED BY APPLICABLE LAW.
 * EXCEPT WHEN OTHERWISE STATED IN WRITING THE COPYRIGHT HOLDERS AND/OR OTHER
 * PARTIES PROVIDE THE PROGRAM "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER
 * EXPRESSED OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE ENTIRE RISK AS
 * TO THE QUALITY AND PERFORMANCE OF THE PROGRAM IS WITH YOU. SHOULD THE PROGRAM
 * PROVE DEFECTIVE, YOU ASSUME THE COST OF ALL NECESSARY SERVICING, REPAIR
 * OR CORRECTION.
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

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
	
	
	@SuppressWarnings("deprecation")
	public static class MapClass extends
			Mapper<LongWritable, Text, IntWritable, ImageDescriptor> {

		private int numberOfPartition;
		private int dimension;

		// scale used for ????
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

			dimension = configuration.getInt("dimension", 64);

			System.out.println("NPhase1.MapClass.setup() number of partitions "
					+ numberOfPartition);

			recIdOffset = 0;
			coordOffset = recIdOffset + 1;

			r = new Random();

			if (inputFile.indexOf("outer") != -1)
				fileId = 0;
			else if (inputFile.indexOf("inner") != -1)
				fileId = 1;
			else {
				System.out.println("Invalid input file source@NPhase1");
				System.exit(-1);
			}
		} // configure

		/**
		 * Partition the input data sets (R and S) into multiple buckets.
		 */
		public void map(LongWritable key, Text value, Context context)
				throws IOException {
			String[] parts = value.toString().split(";");
			// String[] descriptors;
			String recId = parts[0];
			String recIdInt = (recId);

			// long length = 0;
			// for(int i =1;i<parts.length;i++) {
			// parts.length;
			//
			// }
			// ArrayList<float[]> coordList = new ArrayList<float[]>();
			// float[] coord;
			// TODO : blinder le parsing pour gerer erreurs, lignes vides...
			// for (int j = 1; j < parts.length; j++) {
			// coord = new float[dimension];
			// descriptors = parts[j].split(",");
			// for (int i = 0; i < descriptors.length; i++) {
			//
			// coord[i] = Float.parseFloat(descriptors[i]);
			// }
			// coordList.add(coord);
			// }

			int partID;
			int groupID;
			// float[] converted = new float[dimension];
			// int compteur=0;
			//
			// float[] tmp_coord = new float[dimension];
			// for (float[] coordonnes : coordList) {
			// for (int i = 0; i < dimension; i++) {
			// tmp_coord[i] = coordonnes[i];
			// converted[i] = (int) tmp_coord[i];
			// tmp_coord[i] -= converted[i];
			// converted[i] *= scale;
			//
			// converted[i] += (tmp_coord[i] * scale);
			// }
			//
			// // Use scaled data sets
			// for (int i = 0; i < dimension; i++)
			// coordonnes[i] = (float) converted[i];
			// }

			if (recIdInt == null || recIdInt.isEmpty()) {
				System.exit(-1);
			}
			ImageDescriptor imgDes = new ImageDescriptor(recId, key.get() + recId.length()+1);// , 0);

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
				// System.out.println("NPhase1.MapClass.map()" + mapKey + " "
				// + np1v.getRecordID());
				// compteur++;
				try {
					System.out.println("NPhase1.MapClass.map() " + mapKey
							+ "  " + imgDes.getPath() + " "
							+ imgDes.getDescriptorsOffset());
					context.write(mapKey, imgDes);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				// System.out.println("NPhase1.MapClass.map()" + " " +
				// compteur);
			}

		} // map
	} // MapClass

	/**
	 * Perform Block Nested Loop join for records in the same partition/bucket.
	 */
	public static class Reduce
			extends
			org.apache.hadoop.mapreduce.Reducer<IntWritable, ImageDescriptor, NullWritable, Text> {

		private int bufferSize = 8 * 1024 * 1024;
		// private MultipleOutputs mos;

		private LocalDirAllocator lDirAlloc = new LocalDirAllocator(
				"mapred.local.dir");
		private FSDataOutputStream out;
		private FileSystem localFs;
		private FileSystem lfs;
		private Path file1;
		private Path file2;

		private int numberOfPartition;
		private int dimension;
		private int blockSize;
		private int knn;
		private float distmin;

		// private boolean self_join;

		private Configuration jobinfo;

		protected Path[] inputPaths;

		protected FileSystem fs;

		// String inputName = conf.get("map.input.file");

		public void setup(Context c) {
			numberOfPartition = c.getConfiguration().getInt(
					"numberOfPartition", 2);
			dimension = c.getConfiguration().getInt("dimension", 2);
			blockSize = c.getConfiguration().getInt("blockSize", 1024);
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

		protected void extractDescriptorsFromString(String descriptors,
				ArrayList<float[]> descriptorsList) {

//			System.out.println("NPhase1.Reduce.extractDescriptorsFromString() working on  " + descriptors);
			
			String[] descriptorsArray = descriptors.split(";");
			String[] valeurs;
			float[] desc;
			// for each descriptor
			for (int i = 0; i < descriptorsArray.length; i++) {
//				System.out.println("NPhase1.Reduce.extractDescriptorsFromString()        processing substring " + descriptorsArray[i]);

				desc = new float[dimension];
				valeurs = descriptorsArray[i].split(",");
				// for each value in the descriptor
				for (int j = 0; j < dimension; j++) {
					//System.out.println("NPhase1.Reduce.extractDescriptorsFromString() "+valeurs);
					try {
						desc[j] = Float.valueOf(valeurs[j]);
					} catch (NumberFormatException e) {
						e.printStackTrace();
					}
				}
				descriptorsList.add(desc);
			}
		}

		// protected String toReduceOutput(ImageDescriptor source) {
		// StringBuilder line = null;
		// line = new StringBuilder(source.getPath().toString());
		// line.append(";");
		// line.append(source.getDescriptorsOffset());
		// return line.toString();
		// }

		public void reduce(IntWritable key, Iterable<ImageDescriptor> values,
				Context context) {
			ArrayList<ImageDescriptor> al = new ArrayList<ImageDescriptor>();

			if (NPhase1.caching) {
				System.out.println("NPhase1.Reduce.reduce() caching Enabled");
			} else {
				System.out.println("NPhase1.Reduce.reduce() caching Disabled");
			}
			
			FSDataInputStream in = null;
			try {
				in = fs.open(inputPaths[0]);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			// BufferedReader brd
			// = new BufferedReader(new InputStreamReader(in));

			// synchronized (al) {
			ImageDescriptor v = null;
			// int index = 0;

			for (ImageDescriptor tmp : values) {
				v = new ImageDescriptor(tmp.getPath(),
						tmp.getDescriptorsOffset()); // ,
				// tmp.getDescriptorsSize());

				al.add(v);

			}

			ImageDescriptor[] tab = al.toArray(new ImageDescriptor[] {});
//			System.out.println("NPhase1.Reduce.reduce() comparing "
//					+ tab.length + "  images");

			for (int i = 0; i < tab.length - 1; i++) {
				// StringBuilder line = null;
				ImageDescriptor source = tab[i];
				System.out.println("NPhase1.Reduce.reduce() done " + i
						* 100 / (tab.length - 1) + " %");
				for (int j = i + 1; j < tab.length; j++) {				
					ImageDescriptor cible = tab[j];
					if (!source.getPath().equals(cible.getPath())) {
						computeDistance(source, cible, in, context);

					}

				}
			}

		}

		// public void reduce(IntWritable key, Iterator<NPhase1Value> values,
		// OutputCollector<NullWritable, Text> output, Reporter reporter)
		// throws IOException {
		// String algorithm = "nested_loop";
		// String prefix_dir = algorithm + "-"
		// + Integer.toString(numberOfPartition) + "-"
		// + key.toString();
		//
		// try {
		// file1 = lDirAlloc.getLocalPathForWrite(prefix_dir + "/"
		// + "outer", jobinfo);
		// file2 = lDirAlloc.getLocalPathForWrite(prefix_dir + "/"
		// + "inner", jobinfo);
		// lfs.create(file1);
		// lfs.create(file2);
		// } catch (IOException e) {
		// e.printStackTrace();
		// }
		//
		// String outerTable = file1.toString();
		// String innerTable = file2.toString();
		//
		// generateIntermediateFile(values, reporter, outerTable, innerTable);
		//
		// FileReader frForR = new FileReader(outerTable);
		// BufferedReader brForR = new BufferedReader(frForR, bufferSize);
		//
		// // initialize for R
		// int number = blockSize;
		// boolean flag = true;
		// String parts[] = null;
		//
		// while (flag) {
		// // Read a block of R
		// for (int ii = 0; ii < number; ii++) {
		// String line = brForR.readLine();
		// if (line == null) {
		// flag = false; // Going to end
		// number = ii;
		// break;
		// }
		//
		// parts = line.split("\\|");
		//
		// String[] first = parts[0].split(";");
		// String[] second = parts[1].split(";");
		//
		// String id1 = first[0];
		// String id2 = second[0];
		//
		// ArrayList<float[]> descriptorsList = new ArrayList<float[]>();
		// ArrayList<float[]> descriptorsList2 = new ArrayList<float[]>();
		//
		// extractDescriptors(first, descriptorsList);
		// extractDescriptors(second, descriptorsList2);
		//
		// computeDistance(id1, id2, descriptorsList,
		// descriptorsList2, reporter, output);
		//
		// }
		//
		// brForR.close();
		// frForR.close();
		// }// reduce
		// }

		protected float distance(float[] desc1, float[] desc2) {

			float result = 0;

			for (int i = 0; i < desc1.length; i++) {
				result += (desc1[i] - desc2[i]) * (desc1[i] - desc2[i]);
			}

			return result;

		}

		protected ArrayList<float[]> extractDescriptors(ImageDescriptor img,
				FSDataInputStream in, Context context) {
			ArrayList<float[]> descriptorsList = null;

			//System.out.println("NPhase1.Reduce.extractDescriptors()");
			if (NPhase1.caching) {
				if (NPhase1.cache == null) {
					// cache for 100 entries, maximum 1000 secondes between
					// cleanups
					cache = new LfuCacheFactory().newInstance("descriptors",
							1000 * 1000, 500);
				}
				
				descriptorsList = (ArrayList<float[]>) cache.getObject(img
						.getPath());
			}

			// either no caching or not found in cache
			if (!NPhase1.caching || descriptorsList == null) {
				descriptorsList = new ArrayList<float[]>();
				String dSource = null;
				try {
					in.seek(img.getDescriptorsOffset());
					LineReader ln = new LineReader(in);
					Text t = new Text();
					ln.readLine(t);
					dSource = t.toString();
				
				} catch (IOException e) {
					e.printStackTrace();
				}

				try {
					extractDescriptorsFromString(dSource, descriptorsList);

				} catch (IndexOutOfBoundsException e) {
					System.out
							.println("NPhase1.Reduce.computeDistance() ERROR ");
					System.out.println("  >>> Img1 ");
					System.out.println("              Path " + img.getPath());
					System.out.println("              Offset "
							+ img.getDescriptorsOffset());

					throw e;
				} catch (NumberFormatException e) {
					System.out
							.println("NPhase1.Reduce.computeDistance() ERROR ");
					System.out.println("  >>> Img1 ");
					System.out.println("              Path " + img.getPath());
					System.out.println("              Offset "
							+ img.getDescriptorsOffset());

					throw e;
				}
				if (NPhase1.caching) {
					System.out
							.println("NPhase1.Reduce.extractDescriptors() adding img "
									+ img.getPath() + " to cache");
					// add it to the cache
					cache.addObject(img.path, descriptorsList);
				}
			}

			return descriptorsList;

		}

		protected void computeDistance(ImageDescriptor source,
				ImageDescriptor cible, FSDataInputStream in, Context context) {


			ArrayList<float[]> descriptorsList1 = this.extractDescriptors(
					source, in, context);
			ArrayList<float[]> descriptorsList2 = this.extractDescriptors(
					cible, in, context);



			int cpt = 0;
			boolean iMatch = false;
			boolean dMatch = false;
			Iterator<float[]> i1 = descriptorsList1.iterator();
			Iterator<float[]> i2 = descriptorsList2.iterator();
			while ((!iMatch) && i1.hasNext()) {
				dMatch = false;
				float[] d1tmp = i1.next();

				while ((!dMatch) && i2.hasNext()) {
					float[] d2tmp = (float[]) i2.next();
					float distance = distance(d1tmp, d2tmp);
//					 System.out.println("NPhase1.Reduce.computeDistance() "
//					 + distance + " compare to " + distmin);

					if (distance < distmin) {
//						 System.out
//						 .println("NPhase1.Reduce.computeDistance() it 's a match!");
						cpt++;
						dMatch = true;
					}
				}
				context.progress();
				if (cpt >= knn) {
					iMatch = true;
				}
			}

			if (iMatch) {
//				 System.out
//				 .println("NPhase1.Reduce.computeDistance() found match...");

				try {
					context.write(NullWritable.get(), new Text(source.getPath()
							+ ";" + cible.getPath()));
				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}

		}

	} // Reducer

	static int printUsage() {
		System.out
				.println("NPhase1 [-m <maps>] [-r <reduces>] [-p <numberOfPartitions>] "
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
					// job.setNumMapTasks(Integer.parseInt(args[++i]));
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

		// conf.set

		JobConf j = new JobConf(conf);
//		j.setProfileEnabled(true);
//		j.setProfileParams("-agentlib:hprof=cpu=samples,depth=6,heap=sites,force=n,thread=y,verbose=n,file=%s");
//		j.setProfileTaskRange(false, "1-10");
		Job job = new Job(j);
		job.setJarByClass(NPhase1.class);

		// JobConf conf = new JobConf(getConf(), NPhase1.class);
		job.setJobName("k-NN");

		job.setMapperClass(MapClass.class);
		job.setReducerClass(Reduce.class);
		job.setOutputKeyClass(IntWritable.class);
		job.setOutputValueClass(ImageDescriptor.class);
//		job.setNumReduceTasks(numberOfReduceTasks);
		job.setNumReduceTasks(numberOfPartition*numberOfPartition);
		//job.setPartitionerClass(CustomPartitioner.class);

		System.out
				.println("NPhase1.run() base input path " + other_args.get(0));

		FileInputFormat.setInputPaths(job, other_args.get(0));
		System.out.println("NPhase1.run() added input path "
				+ other_args.get(1));

		System.out.println("NPhase1.runPhase1() numberOfPartition "
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

		// JobConf conf = new JobConf(getConf(), NPhase1.class);
		job.setJobName("FilterJob");

		// job.setMapperClass(MapClass.class);
		job.setReducerClass(FilterJob.Reduce.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(IntWritable.class);

		FileInputFormat.addInputPath(job, new Path("/user/fhuet/phase1"));
		FileOutputFormat.setOutputPath(job, new Path("/user/fhuet/phase2"));

		job.setNumReduceTasks(10);
		job.waitForCompletion(true);
	}

//	public static void main(String[] args) {
//		String s = "-0.0012639377,-0.08363894,0.011720599,0.12380912,-0.0043550776,-0.15456437,0.019112818,0.2302905,-0.0059964005,-0.15439224,0.018115638,0.279309,-0.003414073,-0.08780366,0.009913056,0.15411445,0.005390352,0.21767016,0.020114267,0.28834185,0.0073008505,0.25002965,0.029023599,0.38225776,0.008156657,0.324282,0.02163628,0.42275402,0.0046608243,0.2029813,0.013675922,0.25636438,-9.726192E-4,0.057943948,0.012827009,0.070217974,7.3538657E-4,0.056297794,0.013096964,0.07263991,0.0042800177,0.06851089,0.013990093,0.083124265,0.0034941018,0.04230504,0.011397576,0.053576782,-3.6762032E-4,-8.461042E-4,0.0063354084,0.007923602,-0.0011041741,-0.0018139697,0.009770969,0.010604993,0.0011212057,-0.0052185166,0.008805678,0.012670101,0.0016470777,-0.0032559421,0.006813939,0.008711953";
//		Reduce r = new Reduce();
//		ArrayList descriptorsList = new ArrayList(); 
//		r.extractDescriptorsFromString(s, descriptorsList);
//	}
	
	
	public static void main(String[] args) throws Exception {
		int res = ToolRunner.run(new Configuration(), new NPhase1(), args);
		System.exit(res);
	}

} // NPhase1

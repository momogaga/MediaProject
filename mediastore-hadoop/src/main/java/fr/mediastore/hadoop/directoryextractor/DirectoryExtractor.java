package fr.mediastore.hadoop.directoryextractor;

import fr.mediastore.hadoop.ImageDescriptor;
import fr.thumbnailsdb.Utils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.LineReader;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.h2.util.New;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

/**
 * Created with IntelliJ IDEA.
 * User: fhuet
 * Date: 03/09/13
 * Time: 14:52
 * To change this template use File | Settings | File Templates.
 */
public class DirectoryExtractor extends Configured implements Tool {


    public static class MapClass extends
            Mapper<LongWritable, Text, Text, ImageDescriptor> {


        @Override
        protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            String[] parts = value.toString().split(",");
            String path = parts[0];


            String directory = null;
            if (Utils.isValideImageName(path)) {
                long offset = key.get() + path.length() + 1;
                ImageDescriptor imgDes = new ImageDescriptor(path, offset);
                directory = Utils.fileToDirectory(path);
                context.write(new Text(directory), imgDes);
            }

        }
    }


    public static class ReduceClass extends Reducer<Text, ImageDescriptor, NullWritable, Text> {

        protected Path[] inputPaths;
        protected FileSystem fs;

        private static final int NUM_ELEMENTS = 1;


        @Override
        public void setup(Context c) {
            inputPaths = FileInputFormat.getInputPaths(c);
            try {
                fs = FileSystem.get(c.getConfiguration());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        protected String imageDescriptorToString(ImageDescriptor imd, FSDataInputStream in) {
            String result = null;
            try {
                in.seek(imd.getDescriptorsOffset());
                LineReader ln = new LineReader(in);
                Text t = new Text();
                ln.readLine(t);
                result = imd.getPath() + "," + t.toString();
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                //System.out.println("ERROR looking for " + imd.getPath() + " \nOffset : " + imd.getDescriptorsOffset());
                return null;
            }
            return result;
        }

        @Override
        protected void reduce(Text key, Iterable<ImageDescriptor> values, Context context) throws IOException, InterruptedException {
            Iterator<ImageDescriptor> it = values.iterator();
            int total = 0;
            FSDataInputStream in = null;
           // System.out.println("Input paths[0] " + inputPaths[0]);
            try {
                in = fs.open(inputPaths[0]);
            } catch (IOException e1) {
                e1.printStackTrace();
            }

            ArrayList<ImageDescriptor> al = new ArrayList<ImageDescriptor>();
            //  StringBuffer result = new StringBuffer();
            while (it.hasNext()) {
                ImageDescriptor imd = it.next();
                total++;
                al.add(imd);
                //System.out.println(imd);
                //keep only 10% of images
//                if (Math.random() <= 0.1) {
//                    // result.append(imageDescriptorToString(imd,in))
//                    String v = imageDescriptorToString(imd, in);
//                    if (v != null) {
//                        context.write(NullWritable.get(), new Text(v));
//                    }
//                }

            }
            Collections.shuffle(al);
             for (int i=0;(i<NUM_ELEMENTS && i<al.size());i++) {
                 context.write(NullWritable.get(), new Text(imageDescriptorToString(al.get(i), in)));
             }


           in.close();
            //     if (total > THRESHOLD) {

            //   }
        }
    }

    @Override
    public int run(String[] args) throws Exception {


        Configuration conf = new Configuration();
        JobConf j = new JobConf(conf);
//		j.setProfileEnabled(true);
//		j.setProfileParams("-agentlib:hprof=cpu=samples,depth=6,heap=sites,force=n,thread=y,verbose=n,file=%s");
//		j.setProfileTaskRange(false, "1-10");
        Job job = new Job(j);
        job.setJarByClass(DirectoryExtractor.class);

        // JobConf conf = new JobConf(getConf(), fr.mediastore.hadoop.NPhase1.class);
        job.setJobName("DirectoryExtractor");

        job.setMapperClass(MapClass.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(ImageDescriptor.class);


        job.setReducerClass(ReduceClass.class);
        job.setOutputKeyClass(NullWritable.class);
        job.setOutputValueClass(Text.class);

        job.setNumReduceTasks(20);

        FileInputFormat.setInputPaths(job, args[0]);

        // FileInputFormat.addInputPaths(job, other_args.get(1));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        job.waitForCompletion(true);

        return 0;
    }


    public static void main(String[] args) throws Exception {
        int res = ToolRunner.run(new Configuration(), new DirectoryExtractor(), args);
        System.exit(res);
    }

}

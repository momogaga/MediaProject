package fr.mediastore.hadoop;

import java.io.IOException;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

public class TestOffset {
	public static void main(String[] args) {
		
		
		FileSystem fs=null;	
		
		FSDataInputStream in = null;
		try {
			in = fs.open(new Path(""));
		} catch (IOException e1) {
			e1.printStackTrace();
		}

	}
}

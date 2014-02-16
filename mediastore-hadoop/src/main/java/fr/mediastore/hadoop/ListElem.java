package fr.mediastore.hadoop;

import java.io.*;
import java.lang.*;

class ListElem {
	private String id;
	private double dist;

	ListElem(int dimension, double d, String id) 
	{
		this.dist = d;
		this.id = id;
	}

	ListElem(int dimension, float value) 
	{
		this.dist = value;
	}

	void setDist(double value) { this.dist = value; }

	double getDist() { return this.dist; }

	String getId() { return this.id; }

	public String toString() 
	{
		return (this.id) + " " + Double.toString(this.dist);	
	}

	
}

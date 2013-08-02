# Makefile for NestedLoopJoin both single machine and multi-nodes  
HADOOP_VERSION=1.0.2
HADOOP_HOME=/local/hadoop/hadoop
all: 
	rm -rf knn
	mkdir knn
	javac -classpath ${HADOOP_HOME}/hadoop-core-${HADOOP_VERSION}.jar -g -d knn *.java
	jar -cvf ./knn.jar -C knn/ .     # for hadoop
clean:
	rm -rf knn
	rm knn.jar

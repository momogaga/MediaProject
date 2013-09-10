on styx

~/workspaces/hadoop/hadoop-1.0.2/bin/hadoop fs -put signatures-small.txt /user/fhuet/outer

on eon12
  hadoop jar /user/fhuet/desktop/home/workspaces/rechercheefficaceimagessimilaires/MediaStore/mediastore-hadoop/target/mediastore-hadoop.jar fr.mediastore.hadoop.NPhase1   -p 600 /user/fhuet/inner/signatures.txt /user/fhuet/outer/signatures.txt /user/fhuet/phase1



The hadoop based block nested loop KNN join algorithm (H-BNLJ) 
consists of 2-round MapReduce phases and the corresponding source 
files to each stage are given as follows:
Round1: fr.mediastore.hadoop.NPhase1.java  fr.mediastore.hadoop.NPhase1Value.java fr.mediastore.hadoop.ListElem.java fr.mediastore.hadoop.RecordComparator.java
Round2: fr.mediastore.hadoop.NPhase2.java  NPhase2Value.java


An complete example of running the programs are given as follows:

Round 1:
hadoop jar knn.jar fr.mediastore.hadoop.NPhase1 -m 1 -r 16 -p 4 -d 2 -k 10 -b 100000 c16/rsr40m c16/rss40m phase1out

-m: specify the number of mappers (should set to the number of splits)
-r: specify the number of reducers (should set to the number of random shift copies)
-p: specify the number of partitions/buckets
-d: specify the dimensionality of the input datasets (64 dans notre cas)
-k: specify the number of the nearest neighbors to be retrieved (la granularite du resultats en depend fortement)
-n: distance minimale pour que deux points soientt consideres comme similaires
-b: specify the buffer size (bytes)

In this case, input data sets are put in HDFS directory c16/rsr40m and 
c16/rss40m. The output datasets are under phase1out.

Round 2:
hadoop jar knn.jar test.fr.mediastore.hadoop.NPhase2 -m 1 -r 16 -k 10 phase1out phase2out

-m: specify the number of mappers (should set to the number of splits)
-r: specify the number of reduces (decided by values from -s and -p)
-k: specify the number of the nearest neighbors to be retrieved
-p: specify the number of partitions/buckets

In this case, input datasets reside on phase1out and output datasets are
saved in phase2out.

If you have any questions, please send email to us.

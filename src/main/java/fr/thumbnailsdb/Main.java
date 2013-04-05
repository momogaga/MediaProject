package fr.thumbnailsdb;

public class Main {

	public static void mediaAction(String dbPath, String[] args) {
		System.out.println("Main.mediaAction() db " + dbPath);
		for (int i = 0; i < args.length; i++) {
			System.out.println("    " + args[i]);
		}
		ThumbStore tb = new ThumbStore(dbPath);

		if ("similar".equals(args[0])) {
			SimilarImageFinder si = new SimilarImageFinder(tb);
			String source = args[1];
			si.prettyPrintSimilarResults(si.findSimilarMedia(source,10), 10);
		}
		
		if ("duplicate".equals(args[0])) {
			SimilarImageFinder si = new SimilarImageFinder(tb);
			String source = args[1];
			si.prettyPrintIdenticalResults(si.findIdenticalMedia(source));
		}
		
	}

	public static void dbAction(String dbPath, String[] args) {
		System.out.println("Main.dbAction() db " + dbPath);
		for (int i = 0; i < args.length; i++) {
			System.out.println("    " + args[i]);
		}
		ThumbStore tb = new ThumbStore(dbPath);
		MediaIndexer tg = new MediaIndexer(tb);
		if ("index".equals(args[0])) {
			String source = args[1];
			tg.processMTRoot(source);
		}

        if ("indexGPS".equals(args[0])) {
            String source = args[1];
            tg.forceGPSUpdate= true;
            tg.processMTRoot(source);
        }

		if ("fix".equals(args[0])) {
			tb.fix();
		}
		if ("shrink".equals(args[0])) {
			tb.shrink();
		}
		if ("size".equals(args[0])) {
			System.out.println("DB has size " + tb.size());
		}
        if ("update".equals(args[0])) {
           tg.updateDB();
        }
		if ("duplicate".equals(args[0])) {
			DuplicateMediaFinder df = new DuplicateMediaFinder(tb);
			// String source = args[1];
			//df.prettyPrintDuplicate(df.findDuplicateMedia());
		}

        if ("duplicateFolder".equals(args[0])) {
            DuplicateMediaFinder df = new DuplicateMediaFinder(tb);
            // String source = args[1];
            //df.prettyPrintDuplicateFolder(df.findDuplicateMedia());
        }

	}

	public static void main(String[] args) {
		// try {
		// Thread.sleep(5000);
		// } catch (InterruptedException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		if (args.length < 2) {
			System.err.println("Usage : java " + Main.class + " [-db path_to_db]  target [options]");
			System.err.println("where target [options] are ");
			System.err.println("   db index  folder_or_file_to_process");
            System.err.println("   db indexGPS  folder_or_file_to_process");
			System.err.println("   db clean");
			System.err.println("   db fix");
			System.err.println("   db shrink");
			System.err.println("   db size");
			System.err.println("   db duplicate");
            System.err.println("   db duplicateFolder");
			System.err.println("   media similar folder_or_file_to_process");
			System.err.println("   media duplicate folder_or_file_to_process");
			System.exit(-1);
		} else {
			int i = 0;
			String db = null;
			while (i < args.length) {
				if ("-db".equals(args[i])) {
					i++;
					db = args[i];
					i++;
					System.out.println("Database is " + db);
				} else if ("db".equals(args[i])) {
					i++;
					String[] newArgs = new String[args.length - i];
					System.arraycopy(args, i, newArgs, 0, args.length - i);
					dbAction(db, newArgs);
					break;

				} else if ("media".equals(args[i])) {
					i++;
					String[] newArgs = new String[args.length - i];
					System.arraycopy(args, i, newArgs, 0, args.length - i);
					mediaAction(db, newArgs);
					break;

				}
			}

		}

	}

}

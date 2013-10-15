package fr.thumbnailsdb.utils;

public class ProgressBar {
    private StringBuilder progress;


    private int min;
    private int max;
 //   private int steps;

    private int increment;
    private int currentStep=1;
    private int percent;
    /**
     * initialize progress bar properties.
     */
    public ProgressBar() {
        init();
    }

    public ProgressBar(int min, int max, int steps) {
        this.min = min;
        this.max=max;
      //  this.steps = steps;\
        if (steps==0) {
            steps = 1;
        }
        this.increment = (max-min) / steps;
        init();
    }

    /**
     * called whenever the progress bar needs to be updated.
     * that is whenever progress was made.
     *
     * @param done an int representing the work done so far
     * @param total an int representing the total work
     */
    public void update(int done, int total) {
        char[] workchars = {'|', '/', '-', '\\'};
        String format = "\r%3d%% %s %c";

        percent = (done * 100) / total;
        int extrachars = (percent / 2) - this.progress.length();

        while (extrachars-- > 0) {
            progress.append('#');
        }

        System.out.printf(format, percent, progress,
         workchars[done % workchars.length]);

        if (done == total) {
            System.out.flush();
            System.out.println();
            init();
        }
    }

    /**
     * Called whenever an update of the progress bar
     * is needed. Chech whether the currentValue makes
     * a change in the current step. Does not do anything otherwise.
     * @param currentValue
     * @return true if the bar has progressed
     */
    public boolean tick(int currentValue) {
       if (currentValue>=(this.currentStep*increment)) {
          update(currentValue,this.max);
           int newStep = (int) Math.ceil(1.0*currentValue/increment);
           if (newStep == currentStep) {
               //we are just at the end of the currentStep, let's move manually
               currentStep++;
           }else {
               currentStep=newStep;
           }

           return true;
       }
        return false;
    }


    public int getPercent() {
               return percent;
    }


    private void init() {
        this.progress = new StringBuilder(60);
    }

      public static void main(String[] args) {
          ProgressBar pb = new ProgressBar(0,99, 10);

          for (int i=0;i<100;i++) {
              pb.tick(i);
              try {
                  Thread.sleep(200);
              } catch (InterruptedException e) {
                  e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
              }
          }
      }
}
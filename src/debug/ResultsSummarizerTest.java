package debug;

import edu.ucsd.asr.*;

//java debug.ResultsSummarizerTest
public class ResultsSummarizerTest {
  public static void main(String[] args) {
    if (args.length != 2) {
      System.out.println("Usage: <root directory> <output file>");
      System.exit(1);
    }
    String rootDir = args[0]; //"d:/besta";
    String out = args[1]; //"summary.txt";
    //use null to search for all files
    ResultsSummarizer resultsSummarizer = new ResultsSummarizer(rootDir, null,
        out);
  }

  private static void runForTIMIT() {
    String rootDir = "d:/simulations/timit/60models/";
    //String rootDir = "D:/simulations/timit/60models/lsfeda39w512s160/" +
    //"lrforwardskips5/monophones/isolated/baumwelch/1/classification/test/";
    //"lrforwardskips5/monophones/isolated/baumwelch/";
    String fileName = "confusion39.txt";
    String out = "summary2.txt";
    ResultsSummarizer resultsSummarizer = new ResultsSummarizer(rootDir,
        fileName, out);
  }
}

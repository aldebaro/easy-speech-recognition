package various;

import edu.ucsd.asr.SetOfPatterns;

public class DumpFEAFile {

	/**
	 * Shows the contents of a FEA file, writing to stdout.
	 */
	public static void main(String[] args) {
		if (args.length != 1) {
			System.out.println("Usage: <FEA file name>");
			System.exit(1);
		}
		String fileName = args[0];
		SetOfPatterns setOfPatterns = new SetOfPatterns(fileName);
		System.out.println(setOfPatterns);
		setOfPatterns.dumpBinaryData();
	}

}

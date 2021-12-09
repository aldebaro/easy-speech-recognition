package run;

import java.util.*;
import edu.ucsd.asr.*;

/**
 * <p>
 * Title: Spock
 * </p>
 * <p>
 * Description: Speech recognition
 * </p>
 * <p>
 * Copyright: Copyright (c) 2001
 * </p>
 * <p>
 * Company: UFPA
 * </p>
 * 
 * @author Aldebaro
 * @version 4.0
 * @see ConvertALIENFrontEndToSOPFiles
 */

// java run.ConvertParametersToSpockFormat par /home/mydir wavelet 39 true
// java run.ConvertParametersToSpockFormat mfc C:\home\aklautau\tidigitsout\features mfcc_e_d_a
public class ConvertParametersToSpockFormat {
	public static void main(String[] args) {
		if (args.length != 5 && args.length != 3) {
			System.out
					.println("Converts all files under the specified directory "
							+ " to new files in Spock format with extension "
							+ SetOfPatterns.m_FILE_EXTENSION);
			System.out.println("Usage 1 - HTK parameter files:");
			System.out.println("<extension for input files> "
					+ "<input directory> "
					+ "<some string to describe the front end (e.g., MFCC)> ");
			System.out
					.println("\nUsage 2: - binary files, each one storing a matrix of floats");
			System.out.println("<extension for input files> "
					+ "<input directory> "
					+ "<some string to describe the front end (e.g., MFCC)> "
					+ "<number of parameters per frame (e.g., 39)> "
					+ "<is big endian (true or false)?>");
			System.exit(1);
		}
		String extensionForAlienFiles = args[0];
		String inputDirectory = args[1];
		String frontEndDescription = args[2];
		int nnumberOfParameters = -1;
		boolean oareBigEndian = false;
		if (args.length == 5) {
			// binary file
			nnumberOfParameters = Integer.parseInt(args[3]);
			oareBigEndian = (new Boolean(args[4])).booleanValue();
		}

		DirectoryTree directoryTree = new DirectoryTree(inputDirectory,
				extensionForAlienFiles);
		int nnumberOfFiles = directoryTree.getNumberOfPaths();
		if (nnumberOfFiles < 1) {
			End.throwError("Could not find any file with extension "
					+ extensionForAlienFiles + " under directory "
					+ inputDirectory);
		}

		Vector files = directoryTree.getFiles();

		if (args.length == 3) {
			// if HTK, get numbers of parameters from the first file
			String alienFileName = (String) files.elementAt(0);
			Pattern pattern = HTKInterfacer.getPatternFromFile(alienFileName);
			nnumberOfParameters = pattern.getNumOfParametersPerFrame();
		}

		PatternGenerator patternGenerator = new EmptyPatternGenerator(
				nnumberOfParameters, frontEndDescription);

		System.out.println("Found " + files.size() + " files.");

		for (int j = 0; j < nnumberOfFiles; j++) {
			String alienFileName = (String) files.elementAt(j);
			SetOfPatterns setOfPatterns = new SetOfPatterns(patternGenerator);
			if (args.length == 5) {
				// binary file
				setOfPatterns.addPattern(getPatternFromFile(alienFileName,
						nnumberOfParameters, oareBigEndian));
			} else {
				// HTK file
				Pattern pattern = HTKInterfacer
						.getPatternFromFile(alienFileName);
				if (nnumberOfParameters != pattern.getNumOfParametersPerFrame()) {
					System.err
							.println("Error: number of parameters do not match "
									+ "for file "
									+ alienFileName
									+ ". Numbers are "
									+ nnumberOfParameters
									+ " and "
									+ pattern.getNumOfParametersPerFrame());
					System.exit(1);
				}
				setOfPatterns.addPattern(HTKInterfacer
						.getPatternFromFile(alienFileName));
			}
			String outputFileName = FileNamesAndDirectories
					.substituteExtension(alienFileName,
							SetOfPatterns.m_FILE_EXTENSION);
			setOfPatterns.writeToFile(outputFileName);

			System.out.println(alienFileName + " => " + outputFileName);
		}

	}

	/**
	 * Assumes there is no header, assuming a simple matrix with binary numbers.
	 */
	public static Pattern getPatternFromFile(String fileName,
			int nnumberOfParametersPerFrame, boolean oareBigEndian) {
		float[][] fparameters = IO.readFloatMatrixFromBinFile(fileName,
				nnumberOfParametersPerFrame);
		if (!oareBigEndian) {
			fparameters = IO.swapBytes(fparameters);
		}
		return new Pattern(fparameters);
	}

}

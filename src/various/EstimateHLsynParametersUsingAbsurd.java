package various;

import edu.ucsd.asr.*;

/**
 * Script that runs simulation for the SBRT 2007. It is assumed that the first
 * task was to create a conventional set of HMMs (they can be plain and be later
 * converted to shared) using Spock or HTK.
 * 
 * @author Aldebaro
 * 
 */

/*
 java various.EstimateHLsynParametersUsingAbsurd train C:/Temp/besta/absurd/plainhmms.zip c:/temp/besta/absurd/absurd.TRN
 java various.EstimateHLsynParametersUsingAbsurd test c:/temp/besta/newabsurd/absurdhmms.bin.gz c:/temp/besta/absurd/absurd.TRN c:/temp/besta/absurd/test.DTL
 java various.EstimateHLsynParametersUsingAbsurd compare c:/temp/besta/absurd/absurd.TRN
 */
public class EstimateHLsynParametersUsingAbsurd {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length < 1) {
			System.err.println("Error. Possible uses:");
			trainUsage();
			testUsage();
			compareUsage();
			System.exit(1);
		}
		String option = args[0].toLowerCase();
		if (option.equals("train")) {
			train(args);
		} else if (option.equals("test")) {
			test(args);
		} else if (option.equals("compare")) {
			compare(args);
		}
	}

	private static void trainUsage() {
		System.out.println("Use: train <hmmsFileName> <propertiesFileName>");
		System.out
				.println("Example:\ntrain C:/Temp/besta/absurd/plainhmms.zip "
						+ "c:/temp/besta/absurd/absurd.TRN");
	}

	private static void testUsage() {
		System.out.println("Use: test <absurdFileName> <propertiesFileName>");
		System.out
				.println("Example:\ntest c:/temp/besta/newabsurd/absurdhmms.bin.gz "
						+ "c:/temp/besta/absurd/absurd.TRN c:/temp/besta/absurd/test.DTL");
	}

	private static void compareUsage() {
		System.out.println("Use: compare <propertiesFileName>");
		System.out.println("Example:\ncompare c:/temp/besta/absurd/absurd.TRN");
	}

	public static void train(String[] args) {
		if (args.length != 3) {
			trainUsage();
			System.exit(1);
		}

		// String hmmsFileName = "C:/Temp/besta/absurd/" + "inputhmms.txt";
		String hmmsFileName = args[1]; // "C:/Temp/besta/absurd/" +
		// "plainhmms.zip";
		String propertiesFileName = args[2]; // "c:/temp/besta/absurd/absurd.TRN";

		// note: the lists with mfcc files and HLsyn files are described
		// in propertiesFileName. Each pair of files on these lists must have
		// the same number of frames. The number of parameters per frame can be
		// different, such as 39 and 13.

		HeaderProperties headerProperties = HeaderProperties
				.getPropertiesFromFile(propertiesFileName);

		String patternGeneratorFileName = headerProperties
				.getPropertyAndExitIfKeyNotFound("PatternGenerator.FileName");
		PatternGenerator patternGenerator = PatternGenerator
				.getPatternGenerator(patternGeneratorFileName);

		//System.out.println(patternGenerator);

		SetOfPlainContinuousHMMs setOfPlainContinuousHMMs = null;

		// first assume it is a HTK text file with hmms
		int nspaceDimension = patternGenerator.getNumberOfParameters();
		setOfPlainContinuousHMMs = HTKInterfacer.getPlainHMMsFromFile(
				hmmsFileName, new EmptyPatternGenerator(nspaceDimension));
		if (setOfPlainContinuousHMMs.getNumberOfHMMs() == 0) {
			// now try opening as if it was a hmms.zip file trained with Spock
			setOfPlainContinuousHMMs = new SetOfPlainContinuousHMMs(
					hmmsFileName);
		}

		SetOfSharedContinuousHMMs setOfSharedContinuousHMMs = setOfPlainContinuousHMMs
				.convertToSharedHMMs();
		SetOfSharedContinuousHMMsBeingReestimated setOfSharedContinuousHMMsBeingReestimated = new SetOfSharedContinuousHMMsBeingReestimated(
				setOfSharedContinuousHMMs);

		String setOfPatternsInputDirectory = headerProperties
				.getPropertyAndExitIfKeyNotFound("EstimateHLsynParametersUsingAbsurd.trainSetOfPatternsInputDirectory");
		setOfPatternsInputDirectory = FileNamesAndDirectories
				.replaceAndForceEndingWithSlash(setOfPatternsInputDirectory);

		String transcriptions = headerProperties
				.getPropertyAndExitIfKeyNotFound("EstimateHLsynParametersUsingAbsurd.trainTranscriptionsFileName");

		String outputDir = headerProperties
				.getPropertyAndExitIfKeyNotFound("TrainingManager.GeneralOutputDirectory");
		// make outputDir ready to concatenate file names
		outputDir = FileNamesAndDirectories
				.replaceAndForceEndingWithSlash(outputDir);
		FileNamesAndDirectories.createDirectoriesIfNecessary(outputDir);

		String outputOccupationStatisticsFileName = FileNamesAndDirectories
				.concatenateTwoPaths(outputDir, "OccupationStatistics.txt");

		// if we keep the acoustical HMM fixed (that model y), do we need many
		// iterations to estimate the absurd parameters??
		// do 1 iteration:
		AbsurdHMMsReestimator absurdHMMsReestimator = new AbsurdHMMsReestimator(
				setOfSharedContinuousHMMsBeingReestimated, patternGenerator,
				headerProperties, transcriptions, setOfPatternsInputDirectory,
				outputOccupationStatisticsFileName);

		// in case I want more iterations
		absurdHMMsReestimator
				.reestimateSetOfHMMsAndLinearTransformationsUsingEmbeddedBaumWelch(
						patternGenerator, transcriptions,
						setOfPatternsInputDirectory,
						outputOccupationStatisticsFileName);

		// get the reestimated HMMs with absurd parameters
		AbsurdHMMs absurdHMMs = absurdHMMsReestimator
				.getSetOfReestimatedAbsurdHMMs();

		// save it to a file
		absurdHMMs.writeAsSerializedObject(outputDir + "absurdhmms.bin.gz");
		System.out.println("Wrote file " + outputDir + "absurdhmms.bin.gz");
	}

	public static void test(String[] args) {
		if (args.length != 4) {
			testUsage();
			System.exit(1);
		}

		// 1- read a absurd set of HMMs already trained
		String absurdFileName = args[1]; // "c:/temp/besta/newabsurd/absurdhmms.bin.gz";
		String propertiesFileName = args[2]; // "c:/temp/besta/absurd/absurd.TRN";
		String mfccFilesList = args[3];

		AbsurdHMMs absurdHMMs = AbsurdHMMs.readSerializedObject(absurdFileName);
		int nnumberOfHLsynParameters = absurdHMMs.getNumberOfHLsynParameters();

		HeaderProperties headerProperties = HeaderProperties
				.getPropertiesFromFile(propertiesFileName);

		String transcriptions = headerProperties
				.getPropertyAndExitIfKeyNotFound("EstimateHLsynParametersUsingAbsurd.testTranscriptionsFileName");
		int nverbose = headerProperties.getIntegerProperty(
				"TrainingManager.nverbose", "10");

		String setOfPatternsInputDirectory = headerProperties
				.getPropertyAndExitIfKeyNotFound("EstimateHLsynParametersUsingAbsurd.testSetOfPatternsInputDirectory");

		DatabaseManager databaseManager = new DatabaseManager(transcriptions);

		int nnumberOfUtterances = 0;
		// 2- open a list with mfcc files
		while (databaseManager.isThereDataToRead()) {
			// 3- for each file in the list, run the loop below

			// while (nnumberOfUtterances < 9) {
			DataLocator dataLocator = databaseManager.getNextDataLocator();

			if (nverbose > 1) {
				Print.dialog("# " + nnumberOfUtterances + " "
						+ dataLocator.getFileName());
			}

			if (nverbose > 2) {
				Print.dialog(dataLocator.getAllLabelsAsOneString());
			}
			if (dataLocator.getNumberOfSegments() != 1) {
				System.err.println("Support only to isolated HMM currently...");
				System.exit(1);
			}

			// find the parameter file name in order to read file
			String parametersFileName = dataLocator.getFileName();
			parametersFileName = FileNamesAndDirectories.substituteExtension(
					parametersFileName, "mfc");

			parametersFileName = FileNamesAndDirectories
					.getFileNameFromPath(parametersFileName);
			parametersFileName = FileNamesAndDirectories.concatenateTwoPaths(
					setOfPatternsInputDirectory, parametersFileName);

			// 4- read the mfcc parameters y into a Pattern object
			Pattern mfccPattern = HTKInterfacer
					.getPatternFromFile(parametersFileName);
			String hmmLabel = dataLocator.getLabelFromGivenSegment(0);

			// 5- get the MAP estimate x using the AbsurdHMMs and y
			Pattern hlsynPattern = absurdHMMs
					.findMAPEstimationOfHLSYNParameters(mfccPattern, hmmLabel);

			// 6- save the MAP estimate x into a HTK file with an extension
			// esthlsyn
			parametersFileName = FileNamesAndDirectories.substituteExtension(
					parametersFileName, "esthlsyn");

			HTKInterfacer.savePattern(hlsynPattern, parametersFileName,
					new EmptyPatternGenerator(nnumberOfHLsynParameters));
			if (nverbose > 2) {
				Print.dialog("Wrote " + parametersFileName);
			}

		}
		databaseManager.finalizeDataReading();

	}

	public static void compare(String[] args) {
		if (args.length != 2) {
			compareUsage();
			System.exit(1);
		}

		String propertiesFileName = args[1];
		// 1- open lists with original HLsyn files
		HeaderProperties headerProperties = HeaderProperties
				.getPropertiesFromFile(propertiesFileName);

		String transcriptions = headerProperties
				.getPropertyAndExitIfKeyNotFound("EstimateHLsynParametersUsingAbsurd.testTranscriptionsFileName");

		// assume both hlsyn and esthlsyn are in this directory:
		String setOfHLsynPatternsInputDirectory = headerProperties
				.getPropertyAndExitIfKeyNotFound("EstimateHLsynParametersUsingAbsurd.hlsynSetOfPatternsInputDirectory");
		setOfHLsynPatternsInputDirectory = FileNamesAndDirectories
				.replaceAndForceEndingWithSlash(setOfHLsynPatternsInputDirectory);

		DatabaseManager databaseManager = new DatabaseManager(transcriptions);

		double[] derrors = null;
		// 2- Iterate through the lists and compare the trajectories using e.g.
		// Matlab
		int nnumberOfFiles = 0;
		while (databaseManager.isThereDataToRead()) {
			// 3- for each file in the list, run the loop below
			// while (nnumberOfUtterances < 9) {

			// open files by getting their name from the transcription
			DataLocator dataLocator = databaseManager.getNextDataLocator();
			String hlsynParametersFileName = dataLocator.getFileName();
			hlsynParametersFileName = FileNamesAndDirectories
					.substituteExtension(hlsynParametersFileName, "hlsyn");
			hlsynParametersFileName = FileNamesAndDirectories
					.getFileNameFromPath(hlsynParametersFileName);
			hlsynParametersFileName = FileNamesAndDirectories
					.concatenateTwoPaths(setOfHLsynPatternsInputDirectory,
							hlsynParametersFileName);

			String estimatedHLsynParametersFileName = FileNamesAndDirectories
					.substituteExtension(hlsynParametersFileName, "esthlsyn");

			float[][] hlsyn = HTKInterfacer.getPatternFromFile(
					hlsynParametersFileName).getParameters();
			float[][] esthlsyn = HTKInterfacer.getPatternFromFile(
					estimatedHLsynParametersFileName).getParameters();

			// MatlabInterfacer.sendArray(hlsyn, "hlsyn");
			// MatlabInterfacer.sendArray(esthlsyn, "esthlsyn");

			if (derrors == null) {
				// first iteration
				derrors = new double[hlsyn[0].length];
			}
			for (int t = 0; t < hlsyn.length; t++) {
				for (int i = 0; i < derrors.length; i++) {
					double derror = hlsyn[t][i] - esthlsyn[t][i];
					derrors[i] += derror * derror;
				}
			}

			nnumberOfFiles++;
			
			// take time to evaluate the trajectory on Matlab
			IO.pause();
		}

		databaseManager.finalizeDataReading();

		System.out.println("Files = " + nnumberOfFiles);
		// 3- Provide estimates such as mean square error for each coefficient
		System.out.println("Mean square error per parameter");		
		for (int i = 0; i < derrors.length; i++) {
			derrors[i] /= nnumberOfFiles;
		}		
		IO.DisplayVector(derrors);

		System.exit(1);
	}

}

package debug;

import edu.ucsd.asr.*;

/**
 * It compares a set of shared HMMs reestimated with HTK versus with Spock both
 * in terms of speed and similarity of the final HMMs.
 * 
 * @author Aldebaro
 * @created December 19, 2000
 */
public class SetOfSharedContinuousHMMsReestimatorTest {

	/**
	 * The main program for the SetOfSharedContinuousHMMsReestimatorTest class.
	 */
	public static void main(String[] args) {
		// initialization
		float fpruning = 200000.0F;
		String initialDir = "C:/home/aklautau/tidigitsout/hmms/monophones/sentences/finalmodels/";
		String map = initialDir + "MappingFromLogicalToPhysicalHMM.txt";

		String initialSet = initialDir + "hmms.txt";

		long initTime1 = System.currentTimeMillis();
		SetOfSharedContinuousHMMs htkHMMs = null;
		htkHMMs = trainWithHTK(map, initialSet, fpruning);
		long endTime1 = System.currentTimeMillis();

		long initTime2 = System.currentTimeMillis();
		SetOfSharedContinuousHMMs spockHMMs = trainWithSpock(map, initialSet,
				fpruning);
		// Print.dialog(headerProperties.toString());
		long endTime2 = System.currentTimeMillis();

		// compare the time taken by each sofware
		Print.dialog("Taking in account the time for reading file");
		Print.dialog("HTK took " + (endTime1 - initTime1) + ", and Java took "
				+ (endTime2 - initTime2) + " ms.");
		Print.dialog("Java is "
				+ (1.0 * (endTime2 - initTime2) / (endTime1 - initTime1))
				+ " slower.");

		// compare the HMMs (numbers)
		Print.dialog("###############");
		// otherSetOfSharedContinuousHMMs.printTables();
		if (htkHMMs.equals(spockHMMs)) {
			Print.dialog("OK :) :) :) :) :) :) ");
		} else {
			Print.dialog("WRONG");
		}
	}

	private static SetOfSharedContinuousHMMs trainWithHTK(String map,
			String initialSet, float fpruning) {
		String hmmName = "hmms.txt";
		String htkTranscriptions = "C:/home/aklautau/tidigitsout/transcriptions/monophones/train/Transcriptions.txt";
		String mfccScript = "C:/home/aklautau/tidigitsout/features/train/FeatureFiles.txt";
		String htkOutputDirectory = "c:/temp/besta/htk/";
		String statFile = htkOutputDirectory + "stats.txt";
		String herestConfFile = "./HTK/herest.conf";
		FileNamesAndDirectories
				.createDirectoriesIfNecessary(htkOutputDirectory);
		String command = "HERest.exe -A -T 7 -u tmvw -v 0.05 -I "
				+ htkTranscriptions + " -C " + herestConfFile + " -M "
				+ htkOutputDirectory + " -s " + statFile + " -t " + fpruning
				+ " -S " + mfccScript + " -H " + initialSet + " " + map;
		IO.runDOSCommand(command);
		IO
				.rewriteTextFileUsingSystemLineSeparator(htkOutputDirectory
						+ hmmName);
		IO.rewriteTextFileUsingSystemLineSeparator(statFile);
		// System.exit(1);
		SetOfSharedContinuousHMMs hmms = HTKInterfacer.getSharedHMMsFromFiles(
				htkOutputDirectory + hmmName, map);
		return hmms;
	}

	private static SetOfSharedContinuousHMMs trainWithSpock(String map,
			String initialSet, float fpruning) {
		String dir = "C:/cvs/laps/ufpaspeech/config/";
		String patternGeneratorFileName = dir + "mfcceda8k.FCN";
		// String setOfPatternsInputDirectory =
		// "C:/home/aklautau/spock/tidigits/features/mfcceda39w256s80/train";
		String setOfPatternsInputDirectory = "C:/home/aklautau/tidigitsout/features/train/data";
		String propertiesFileName = dir + "tidigits.TRN";
		// String propertiesFileName = dir + "HTK/tidigits_htk.TRN";
		String javaOutputDir = "c:/temp/besta/java/";
		FileNamesAndDirectories.createDirectoriesIfNecessary(javaOutputDir);
		String javaOutputSet = javaOutputDir + "hmms.txt";
		String javaTranscriptions = "C:/home/aklautau/spock/tidigits/transcriptions/train.DTL";
		SetOfSharedContinuousHMMs setOfSharedContinuousHMMs = HTKInterfacer
				.getSharedHMMsFromFiles(initialSet, map);

		SetOfSharedContinuousHMMsBeingReestimated setOfSharedContinuousHMMsBeingReestimated = new SetOfSharedContinuousHMMsBeingReestimated(
				setOfSharedContinuousHMMs);

		PatternGenerator patternGenerator = PatternGenerator
				.getPatternGenerator(patternGeneratorFileName);
		FileWithHeaderReader fileWithHeaderReader = new FileWithHeaderReader(
				propertiesFileName);
		HeaderProperties headerProperties = fileWithHeaderReader
				.getHeaderProperties();
		headerProperties.setProperty("TrainingManager.nverbose", "2");
		headerProperties.setProperty(
				"SetOfSharedContinuousHMMReestimator.flogPruningThreshold",
				Float.toString(fpruning));

		String outputOccupationStatisticsFileName = javaOutputDir
				+ "OccupationStatistics.txt";

		SetOfSharedContinuousHMMsReestimator setOfSharedContinuousHMMsReestimator = new SetOfSharedContinuousHMMsReestimator(
				setOfSharedContinuousHMMsBeingReestimated, patternGenerator,
				headerProperties, javaTranscriptions,
				setOfPatternsInputDirectory, outputOccupationStatisticsFileName);

		setOfSharedContinuousHMMs = setOfSharedContinuousHMMsReestimator
				.getSetOfReestimatedHMMs();
		HTKInterfacer.writeSetOfSharedHMMs(setOfSharedContinuousHMMs,
				javaOutputSet);
		return setOfSharedContinuousHMMs;
	}
}

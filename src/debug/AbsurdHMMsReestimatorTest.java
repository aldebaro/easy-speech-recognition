package debug;

import edu.ucsd.asr.*;

/**
 * 
 * @author Aldebaro
 * @created April, 2007
 */
public class AbsurdHMMsReestimatorTest {

	public static void main(String[] args) {
		//generateSimpleParameterFiles();
		//System.exit(1);
		SetOfPlainContinuousHMMs setOfPlainContinuousHMMs = null;
		SetOfSharedContinuousHMMs setOfSharedContinuousHMMs = null;
		String initialDir = "C:/Temp/besta/absurd/";
		String initialSet = initialDir + "inputhmms.txt";
		if (false) {
			// start with shared and convert to plain
			// watch out to check if the table of labels is properly organized.
			// For
			// example, I had problem with 0_four instead of 0_zero in TIDIGITS
			String map = initialDir + "MappingFromLogicalToPhysicalHMM.txt";
			setOfSharedContinuousHMMs = HTKInterfacer.getSharedHMMsFromFiles(
					initialSet, map);
			setOfPlainContinuousHMMs = setOfSharedContinuousHMMs
					.convertToPlainHMMs();
		} else {
			// start with plain and convert to shared
			setOfPlainContinuousHMMs = HTKInterfacer.getPlainHMMsFromFile(
					initialSet, new EmptyPatternGenerator(39));
			setOfSharedContinuousHMMs = setOfPlainContinuousHMMs
					.convertToSharedHMMs();
			// because it does not clone, will force to reallocate memory space:
			setOfPlainContinuousHMMs = HTKInterfacer.getPlainHMMsFromFile(
					initialSet, new EmptyPatternGenerator(39));
		}
		System.out
				.println("Reestimating using the 'isolated' version of Baum-Welch");
		SetOfPlainContinuousHMMs newSetOfPlainContinuousHMMs = trainSetOfPlainHMMs(setOfPlainContinuousHMMs);
		System.out
				.println("Reestimating using the 'embedded' version of Baum-Welch");
		SetOfSharedContinuousHMMs newSetOfSharedContinuousHMMs = trainSetOfSharedHMMs(setOfSharedContinuousHMMs);

		// convert shared to plain in order to compare
		SetOfPlainContinuousHMMs setOfPlainContinuousHMMs2 = newSetOfSharedContinuousHMMs
				.convertToPlainHMMs();

		System.out.println("#####################");
		System.out.println("HMM Reestimated with isolated BW");
		System.out.println(newSetOfPlainContinuousHMMs.toStringAsInHTK());
		System.out.println("HMM Reestimated with embedded BW");
		System.out.println(setOfPlainContinuousHMMs2.toStringAsInHTK());
	}

	private static SetOfPlainContinuousHMMs trainSetOfPlainHMMs(
			SetOfPlainContinuousHMMs setOfPlainContinuousHMMs) {
		System.out.println(setOfPlainContinuousHMMs.toStringAsInHTK());
		// initialization
		String directoryWithParameterFiles = "C:/Temp/besta/absurd/parameters/";
		String outputHMMSetFileName = "c:/temp/besta/absurd/plainhmms.zip";
		// make sure there is not a previous copy:
		FileNamesAndDirectories.deleteFile(outputHMMSetFileName);
		String propertiesFileName = "c:/temp/besta/absurd/absurd.TRN";

		FileWithHeaderReader fileWithHeaderReader = new FileWithHeaderReader(
				propertiesFileName);
		HeaderProperties headerProperties = fileWithHeaderReader
				.getHeaderProperties();
		headerProperties.setProperty("TrainingManager.nverbose", "10");
		headerProperties.setProperty(
				"ContinuousHMMReestimator.nminimumNumberOfPatternsPerModel",
				"1");
		headerProperties.setProperty(
				"ContinuousHMMReestimator.nmaximumIterations", "1");
		headerProperties.setProperty(
				"ContinuousHMMReestimator.oshouldOutputGammaMatrix", "true");

		HMMReestimator hMMReestimator = new HMMReestimator(headerProperties);
		hMMReestimator.useBaumWelch(setOfPlainContinuousHMMs,
				directoryWithParameterFiles, outputHMMSetFileName);

		SetOfPlainContinuousHMMs newSetOfPlainContinuousHMMs = new SetOfPlainContinuousHMMs(
				outputHMMSetFileName);
		String outputFileName = "c:/temp/besta/absurd/plainhmms.txt";
		newSetOfPlainContinuousHMMs.writeToHTKFile(outputFileName);
		if (newSetOfPlainContinuousHMMs.areModelsOk()) {
			System.out.println("OKKKK");
		} else {
			System.out.println("Not OKKKK");
		}
		return newSetOfPlainContinuousHMMs;
	}

	private static SetOfSharedContinuousHMMs trainSetOfSharedHMMs(
			SetOfSharedContinuousHMMs setOfSharedContinuousHMMs) {
		// String intempHMMFileName = "c:/temp/besta/absurd/tempINhmm.zip";
		// String outtempHMMFileName = "c:/temp/besta/absurd/tempOUThmm.zip";
		// FileNamesAndDirectories.deleteFile(outtempHMMFileName);
		// setOfSharedContinuousHMMs.writeHTKAndJARFiles(intempHMMFileName);
		System.out.println(setOfSharedContinuousHMMs.convertToPlainHMMs()
				.toStringAsInHTK());
		// initialization
		// float fpruning = 200000.0F;
		float fpruning = 2e8F; // disable pruning by using its maximum value

		String dir = "C:/cvs/laps/ufpaspeech/config/";
		String patternGeneratorFileName = "c:/temp/besta/absurd/mfcc3.FCN";
		// String setOfPatternsInputDirectory =
		// "C:/home/aklautau/spock/tidigits/features/mfcceda39w256s80/train";
		String setOfPatternsInputDirectory = "C:/Temp/besta/absurd/parameters/";
		String propertiesFileName = "c:/temp/besta/absurd/absurd.TRN";
		// String propertiesFileName = dir + "HTK/tidigits_htk.TRN";
		String javaOutputDir = "c:/temp/besta/absurd/";
		FileNamesAndDirectories.createDirectoriesIfNecessary(javaOutputDir);
		String javaOutputSet = javaOutputDir + "sharedhmms.txt";
		String javaTranscriptions = "c:/temp/besta/absurd/train.DTL";

		SetOfSharedContinuousHMMsBeingReestimated setOfSharedContinuousHMMsBeingReestimated = new SetOfSharedContinuousHMMsBeingReestimated(
				setOfSharedContinuousHMMs);

		PatternGenerator patternGenerator = PatternGenerator
				.getPatternGenerator(patternGeneratorFileName);
		FileWithHeaderReader fileWithHeaderReader = new FileWithHeaderReader(
				propertiesFileName);
		HeaderProperties headerProperties = fileWithHeaderReader
				.getHeaderProperties();
		headerProperties.setProperty("TrainingManager.nverbose", "10");
		headerProperties
				.setProperty(
						"SetOfSharedContinuousHMMReestimator.nminimumNumberOfLabelOccurrencesForUpdatingModel",
						"1");
		headerProperties.setProperty(
				"SetOfSharedContinuousHMMReestimator.flogPruningThreshold",
				Float.toString(fpruning));
		headerProperties.setProperty(
				"ContinuousHMMReestimator.oshouldOutputGammaMatrix", "true");

		headerProperties.setProperty(
				"AbsurdHMMsReestimator.synthesizerParametersInputFilesList", "synthfiles.txt");

		//assume 2 hlsyn parameters
		headerProperties.setProperty(
				"AbsurdHMMsReestimator.synthesizerSpaceDimension", "2");

		String outputOccupationStatisticsFileName = javaOutputDir
				+ "OccupationStatistics.txt";

		// HMMReestimator hMMReestimator = new HMMReestimator(headerProperties);

		// hMMReestimator.useOneIterationOfEmbeddedBaumWelch(intempHMMFileName,
		// javaTranscriptions, outtempHMMFileName,
		// headerProperties, patternGenerator, setOfPatternsInputDirectory);

		AbsurdHMMsReestimator absurdHMMsReestimator = new AbsurdHMMsReestimator(
				setOfSharedContinuousHMMsBeingReestimated, patternGenerator,
				headerProperties, javaTranscriptions,
				setOfPatternsInputDirectory, outputOccupationStatisticsFileName);
		
		//Tales - CODIGO NOVO AQUI - vou pegar um AbsurdHMMs
		AbsurdHMMs absurdHMMs = absurdHMMsReestimator.getSetOfReestimatedAbsurdHMMs();
		//nao usar o abaixo, pois ele nao vai funcionar
		//absurdHMMs.writeHTKAndJARFiles("bb.jar");
		absurdHMMs.writeAsSerializedObject("absurdhmms.bin");

		// AK -XXX - BUGGY:
		// note I could not use:
		// setOfSharedContinuousHMMs =
		// setOfSharedContinuousHMMsReestimator.getSetOfSharedContinuousHMMs();
		// because in Java object reference is passed by value and the caller
		// method would not
		// get the new HMM, but would continue with the old one
		// note that this does not include the extra matrix of an Absurd object
		SetOfSharedContinuousHMMs newSetOfSharedContinuousHMMs = absurdHMMsReestimator
				.getSetOfReestimatedHMMs();

		HTKInterfacer.writeSetOfSharedHMMs(newSetOfSharedContinuousHMMs,
				javaOutputSet);

		// SetOfSharedContinuousHMMs newSetOfSharedContinuousHMMs = new
		// SetOfSharedContinuousHMMs(outtempHMMFileName);
		// newSetOfSharedContinuousHMMs.writeHTKAndJARFiles(outtempHMMFileName);

		System.out.println("Wrote file " + javaOutputSet);

		// convert shared to plain in order to write as plain and compare
		String initialDir = "C:/Temp/besta/absurd/";
		String map = initialDir + "MappingFromLogicalToPhysicalHMM.txt";
		SetOfSharedContinuousHMMs setOfSharedContinuousHMMs3 = HTKInterfacer
				.getSharedHMMsFromFiles(javaOutputSet, map);

		SetOfPlainContinuousHMMs setOfPlainContinuousHMMs2 = setOfSharedContinuousHMMs3
				.convertToPlainHMMs();
		setOfPlainContinuousHMMs2.writeToHTKFile(javaOutputDir
				+ "plainfromshared.txt");

		if (setOfPlainContinuousHMMs2.areModelsOk()) {
			System.out.println("OKKKK");
		} else {
			System.out.println("Not OKKKK");
		}
		return newSetOfSharedContinuousHMMs;
	}
	
	private static void generateSimpleParameterFiles() {
		String dir = "C:/Temp/besta/absurd/parameters/";
		//assuming space dimension = 3 (MFCCs per frame
//		PatternGenerator patternGenerator = new MFCCPatternGenerator(256, 160, 16000,
//				512, 3, 24, 0.9, false, 22, -50, 2, -100, true, false,
//				false, false, true, false, false, false);
		String fileName = "C:/Temp/besta/absurd/mfcc3.FCN";
		PatternGenerator patternGenerator = PatternGenerator.getPatternGenerator(fileName); 		
		
		//T=4
		double[][] x = { {1,2,3}, {5,6,7}, {1,2,1}, {3,2,1} };
		Pattern p = new Pattern(x);
		//System.out.println(p.getNumOfFrames() + " " + p.getNumOfParametersPerFrame());
		
		HTKInterfacer.savePattern(p, dir + "0_zero.mfc", patternGenerator);
		//HTKInterfacer.writePattern(p.getParameters(), dir + "0_zero.mfc", sampPeriod, parmKind);
		SetOfPatterns sp = new SetOfPatterns(patternGenerator);
		sp.addPattern(p);
		sp.writeToFile(dir + "0_zero.FEA");
		
		//T=6
		double[][] y = { {-1,-1,-1}, {-10,-9,-8}, {-1,-2,-1} ,
				{-1,-1,-1}, {-10,-9,-8}, {-1,-2,-1}};
		p = new Pattern(y);
		HTKInterfacer.savePattern(p, dir + "1_one.mfc", patternGenerator);
		//HTKInterfacer.writePattern(p.getParameters(), dir + "1_one.mfc", sampPeriod, parmKind);
		sp = new SetOfPatterns(patternGenerator);
		sp.addPattern(p);
		sp.writeToFile(dir + "1_one.FEA");
		
		//assuming space dimension = 2 (HLSyn parameters per frame)
		patternGenerator = new EmptyPatternGenerator(2);
		
		//T=4
		//double[][] z = { {10,20}, {30,40}, {50,60}, {70,80}};
		double[][] z = { {10,20}, {30,10}, {50,60}, {90,40}};
		p = new Pattern(z);
		HTKInterfacer.savePattern(p, dir + "0_zero.hlsyn", patternGenerator);
		
		//T=6
		double[][] t = { {1,9},{8,6},{7,8}, {1,2},{1,2},{1,2} };
		p = new Pattern(t);
		HTKInterfacer.savePattern(p, dir + "1_one.hlsyn", patternGenerator);
		
	}

}

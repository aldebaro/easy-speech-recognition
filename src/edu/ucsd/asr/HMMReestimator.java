package edu.ucsd.asr;

import java.io.File;

/**
 *  Reestimate an HMM or a set of HMMs. Embedded Bauw-Welch is used only with
 *  SetOfSharedHMMs. As in HTK, embedded Bauw-Welch can run for only 1 iteration,
 *  while isolated-segments Baum-Welch runs until convergence and the maximum
 *  number of iteration is specified by user in HeaderProperties.
 *
 *@author     Aldebaro Klautau
 *@created    December 16, 2000
 */

public final class HMMReestimator {

	private boolean m_oshouldCheckHMM = true;
	private boolean m_oshouldSaveHMMAfterSplit = false;
	private boolean m_oshouldSkipReestimationIfFileExists;
	private HeaderProperties m_headerProperties;
	private int m_nverbose;
	//make it null if do not want reestimation file
	private final String m_reestimationLogFileName = "HMMReestimation.log";
	private String m_reestimationLogFileNameFullPath;

	public HMMReestimator(HeaderProperties headerProperties) {
		m_headerProperties = headerProperties;

		String property = m_headerProperties.getProperty("TrainingManager.nverbose", "0");
		m_nverbose = (Integer.valueOf(property)).intValue();
		CheckValues.exitOnError(m_nverbose, 0, 10, "TrainingManager.nverbose");
		m_headerProperties.setProperty("TrainingManager.nverbose", property);

		property = m_headerProperties.getProperty("TrainingManager.oshouldSkipReestimationIfFileExists", "true");
		m_oshouldSkipReestimationIfFileExists = (Boolean.valueOf(property)).booleanValue();
		m_headerProperties.setProperty("TrainingManager.oshouldSkipReestimationIfFileExists", property);
	}

	/**
	 * It doesn't reestimate the input HMM set. It builds models from scratch.
	 * Use the input HMM set just to get TableOfLabels,
	 * name of HMMs and their transition matrices.
	 */
	public SetOfPlainContinuousHMMs useSegmentalKMeans(SetOfPlainContinuousHMMs setOfPlainContinuousHMMs,
		   String directoryWithSOPFiles,
			String outputHMMSetFileName,
			int nnumberOfGaussiansPerMixture,
			double dthresholdToStopIterations,
			boolean oshouldUseBaumWelchAfterwards) {

		if (m_oshouldSkipReestimationIfFileExists) {
			boolean odoesHMMFileExist = doesHMMFileExist(outputHMMSetFileName);
			if (m_nverbose > 0) {
				if (odoesHMMFileExist) {
					Print.dialog("Skipping because " + IO.getEndOfString(outputHMMSetFileName,45) + " already exists");
				}
			}
			if (odoesHMMFileExist) {
				SetOfPlainContinuousHMMs tempSetOfPlainContinuousHMMs = new SetOfPlainContinuousHMMs(outputHMMSetFileName);
				if (tempSetOfPlainContinuousHMMs.areModelsOk()) {
					//if the user does not want to overwrite it AND the
					//file is ok (models are ok), so return set of HMMs and do not
					//run reestimation
					return tempSetOfPlainContinuousHMMs;
				}
			}
		}

		if (m_nverbose > 0) {
			if (oshouldUseBaumWelchAfterwards) {
				Print.dialog("Segmental K-means + Baum-Welch: " + IO.getEndOfString(outputHMMSetFileName,30));
			} else {
				Print.dialog("Segmental K-means: " + IO.getEndOfString(outputHMMSetFileName,40));
			}
		}

		TableOfLabels tableOfLabels = setOfPlainContinuousHMMs.getTableOfLabels();
		int nnumberOfEntries = tableOfLabels.getNumberOfEntries();

		HMMReestimator hMMReestimator = new HMMReestimator(m_headerProperties);
		//to store all HMMs and their file names
		ContinuousHMM[] continuousHMMs = setOfPlainContinuousHMMs.getHMMs();
		String[] hmmFileNames = setOfPlainContinuousHMMs.getHMMFileNames();

		//make it look pretty
		directoryWithSOPFiles = FileNamesAndDirectories.replaceAndForceEndingWithSlash(directoryWithSOPFiles);

		//find name for reestimation log file
		m_reestimationLogFileNameFullPath = FileNamesAndDirectories.getPathFromFileName(outputHMMSetFileName) + m_reestimationLogFileName;
		//delete if it already exists
		FileNamesAndDirectories.deleteFile(m_reestimationLogFileNameFullPath);

		//for each table entry, create HMMs
		PatternGenerator patternGeneratorOfAllFiles = null;

		String[] patternsFileNames = new String[nnumberOfEntries];
		Print.setJProgressBarRange(0,nnumberOfEntries);
		for (int ntableEntry = 0; ntableEntry < nnumberOfEntries; ntableEntry++) {

			patternsFileNames[ntableEntry] = directoryWithSOPFiles + tableOfLabels.getPrefferedName(ntableEntry, SetOfPatterns.m_FILE_EXTENSION);
			SetOfPatterns setOfPatterns = new SetOfPatterns(patternsFileNames[ntableEntry]);
			if (patternGeneratorOfAllFiles == null) {
				//first file
				patternGeneratorOfAllFiles = setOfPatterns.getPatternGenerator();
			}
			else {
				//check if same PatternGenerator
				if (!patternGeneratorOfAllFiles.equals(setOfPatterns.getPatternGenerator())) {
					End.throwError("File " + patternsFileNames[ntableEntry] + " has PatternGenerator different " +
							"from previous files.");
				}
			}

			if (m_nverbose > 1) {
				Print.dialog("Initializing with Viterbi + K-means: " + outputHMMSetFileName + " from " + IO.getEndOfString(patternsFileNames[ntableEntry],20));
			}

			int nminimumNumberOfPatterns = 3;
			int nnumberOfStates = continuousHMMs[ntableEntry].getNumberOfStates();
			HMMInitializer hMMInitializer = new HMMInitializer(setOfPatterns,
															   nnumberOfStates,
															   nnumberOfGaussiansPerMixture,
															   dthresholdToStopIterations,
															   m_reestimationLogFileNameFullPath,
															   nminimumNumberOfPatterns);
															   //HMM.Topology.LEFTRIGHT_NO_SKIPS);
			//hMMInitializer.setLBGParameters(5,0.1,0.1,0);
			hMMInitializer.setVerbosity(m_nverbose);
			float[][] ftransitionMatrix = continuousHMMs[ntableEntry].getTransitionMatrix();
			continuousHMMs[ntableEntry] = hMMInitializer.getHMMUsingViterbiAlignmentAndKMeans(ftransitionMatrix);

			if (oshouldUseBaumWelchAfterwards) {
				//getHMMUsingViterbiAlignmentAndKMeans(matrix M) assumes a left-right (LR) matrix during
				//all training, and just substitutes the LR by the provided matrix M in the end,
				//to return an HMM with matrix M. So, I will call Baum-Welch to reestimate
				//using matrix M. That seems interesting to do if matrix M is not left-right
				continuousHMMs[ntableEntry] = useBaumWelch(continuousHMMs[ntableEntry],
						setOfPatterns,
						patternsFileNames[ntableEntry],
						m_reestimationLogFileNameFullPath);
			}
			Print.updateJProgressBar(ntableEntry+1);
		}

		//write output file with reestimated HMMs
		FileNamesAndDirectories.createDirectoriesIfNecessaryGivenFileName(outputHMMSetFileName);
		setOfPlainContinuousHMMs = new SetOfPlainContinuousHMMs(continuousHMMs,
				hmmFileNames, tableOfLabels, patternGeneratorOfAllFiles);
		setOfPlainContinuousHMMs.writeToJARFile(outputHMMSetFileName,
				patternsFileNames);
		return setOfPlainContinuousHMMs;
	}

	public SetOfPlainContinuousHMMs useSegmentalKMeansRecursively(SetOfPlainContinuousHMMs setOfPlainContinuousHMMs,
	//int nnumberOfIterations,
			int ninitialNumberOfGaussiansPerMixture,
			int nincrementOfGaussiansPerMixture,
			int nfinalNumberOfGaussiansPerMixture,
			String rootOutputDirectoryForHMMs,
			String directoryWithSOPFiles,
			double dthresholdToStopIterations,
			boolean oshouldUseBaumWelchAfterwards) {
		directoryWithSOPFiles = FileNamesAndDirectories.replaceAndForceEndingWithSlash(directoryWithSOPFiles);
		SetOfPlainContinuousHMMs newSetOfPlainContinuousHMMs = setOfPlainContinuousHMMs;
		for (int i = ninitialNumberOfGaussiansPerMixture; i <= nfinalNumberOfGaussiansPerMixture; i += nincrementOfGaussiansPerMixture) {
			//newSetOfPlainContinuousHMMs.splitGaussianWithLargestWeightForAllMixtures();
			//for (int j = 0; j < nnumberOfIterations; j++) {
			//XXX create a preffered name method in HMM
			//String outputHMMSetFileName = rootOutputDirectoryForHMMs + i + "_iteration" + j + "/hmms.jar";
			String outputHMMSetFileName = rootOutputDirectoryForHMMs + i + "/" + SetOfHMMsFile.m_name;
			newSetOfPlainContinuousHMMs = useSegmentalKMeans(setOfPlainContinuousHMMs,
					directoryWithSOPFiles,
					outputHMMSetFileName,
					i,
					dthresholdToStopIterations,
					oshouldUseBaumWelchAfterwards);
			//}
		}
		//return last set of HMMs
		return newSetOfPlainContinuousHMMs;
	}

	/**
	 *  Use same name & directory, overwriting initial HMM set.
	 *
	 *@param  initialHMMsJARFileName  Description of Parameter
	 *@param  directoryWithSOPFiles   Description of Parameter
	 *@return                         Description of the Returned Value
	 */
	public SetOfPlainContinuousHMMs useBaumWelch(String initialHMMsJARFileName,
			String directoryWithSOPFiles) {

		String outputHMMSetFileName = initialHMMsJARFileName;
		return useBaumWelch(initialHMMsJARFileName, directoryWithSOPFiles, outputHMMSetFileName);
	}


	/**
	 *  Description of the Method
	 *
	 *@param  initialHMMsJARFileName  Description of Parameter
	 *@param  directoryWithSOPFiles   Description of Parameter
	 *@param  outputHMMSetFileName    Description of Parameter
	 *@return                         Description of the Returned Value
	 */
	public SetOfPlainContinuousHMMs useBaumWelch(String initialHMMsJARFileName,
			String directoryWithSOPFiles,
			String outputHMMSetFileName) {
		return useBaumWelch(new SetOfPlainContinuousHMMs(initialHMMsJARFileName),
				directoryWithSOPFiles,
				outputHMMSetFileName);
	}


	/**
	 * reestimationLogFileName is null => don't write log file
	 *  Description of the Method
	 *
	 *@param  continuousHMM     Description of Parameter
	 *@param  setOfPatterns     Description of Parameter
	 *@param  patternsFileName  Description of Parameter
	 *@return                   Description of the Returned Value
	 */
	public ContinuousHMM useBaumWelch(ContinuousHMM continuousHMM,
			SetOfPatterns setOfPatterns,
			String patternsFileName,
			String reestimationLogFileName) {

		ContinuousHMMReestimator continuousHMMReEstimator = new ContinuousHMMReestimator(continuousHMM,
				setOfPatterns,
				patternsFileName,
				m_headerProperties,
				reestimationLogFileName);

		//get new HMM
		ContinuousHMM newContinuousHMM = continuousHMMReEstimator.getReestimatedHMM();

		if (newContinuousHMM == null) {
			End.throwError("HMM model is null");
		}

		//AK
		//System.out.println("Final model");
		//continuousHMM.toString();
		//test if it's ok
		if (m_oshouldCheckHMM && !newContinuousHMM.isModelOk()) {
			End.throwError("HMM model is not ok!");
		}

		return newContinuousHMM;
	}


	//could get directoryWithSOPFiles from JAR file, but it would restrict
	//reestimating using the same SOP file used for training the initial HMMs
	/**
	 *  Description of the Method
	 *
	 *@param  setOfPlainContinuousHMMs  Description of Parameter
	 *@param  directoryWithSOPFiles     Description of Parameter
	 *@param  outputHMMSetFileName      Description of Parameter
	 *@return                           Description of the Returned Value
	 */
	public SetOfPlainContinuousHMMs useBaumWelch(SetOfPlainContinuousHMMs setOfPlainContinuousHMMs,
			String directoryWithSOPFiles,
			String outputHMMSetFileName) {

		if (m_oshouldSkipReestimationIfFileExists) {
			boolean odoesHMMFileExist = doesHMMFileExist(outputHMMSetFileName);
			if (m_nverbose > 0) {
				if (odoesHMMFileExist) {
					Print.dialog("Skipping because " + IO.getEndOfString(outputHMMSetFileName,45) + " already exists");
				}
			}
			if (odoesHMMFileExist) {
				SetOfPlainContinuousHMMs tempSetOfPlainContinuousHMMs = new SetOfPlainContinuousHMMs(outputHMMSetFileName);
				if (tempSetOfPlainContinuousHMMs.areModelsOk()) {
					//if the user does not want to overwrite it AND the
					//file is ok (models are ok), so return set of HMMs and do not
					//run reestimation
					return tempSetOfPlainContinuousHMMs;
				}
			}
		}

		if (m_nverbose > 0) {
			Print.dialog("Baum-Welch: " + IO.getEndOfString(outputHMMSetFileName,40) + ".");
		}

		TableOfLabels tableOfLabels = setOfPlainContinuousHMMs.getTableOfLabels();

		HMMReestimator hMMReestimator = new HMMReestimator(m_headerProperties);
		//to store all HMMs and their file names
		ContinuousHMM[] continuousHMMs = setOfPlainContinuousHMMs.getHMMs();
		String[] hmmFileNames = setOfPlainContinuousHMMs.getHMMFileNames();

		//make it look pretty
		directoryWithSOPFiles = FileNamesAndDirectories.replaceAndForceEndingWithSlash(directoryWithSOPFiles);

		//find name for reestimation log file
		m_reestimationLogFileNameFullPath = FileNamesAndDirectories.getPathFromFileName(outputHMMSetFileName) + m_reestimationLogFileName;
		//delete if it already exists
		FileNamesAndDirectories.deleteFile(m_reestimationLogFileNameFullPath);

		//for each table entry, reestimate the HMMs
		PatternGenerator patternGeneratorOfAllFiles = null;
		int nnumberOfEntries = tableOfLabels.getNumberOfEntries();
		String[] patternsFileNames = new String[nnumberOfEntries];
		Print.setJProgressBarRange(0,nnumberOfEntries);
		for (int ntableEntry = 0; ntableEntry < nnumberOfEntries; ntableEntry++) {

			patternsFileNames[ntableEntry] = directoryWithSOPFiles + tableOfLabels.getPrefferedName(ntableEntry, SetOfPatterns.m_FILE_EXTENSION);
			SetOfPatterns setOfPatterns = new SetOfPatterns(patternsFileNames[ntableEntry]);
			if (patternGeneratorOfAllFiles == null) {
				//first file
				patternGeneratorOfAllFiles = setOfPatterns.getPatternGenerator();
			}
			else {
				//check if same PatternGenerator
				if (!patternGeneratorOfAllFiles.equals(setOfPatterns.getPatternGenerator())) {
					End.throwError("File " + patternsFileNames[ntableEntry] + " has PatternGenerator different " +
							"from previous files.");
				}
			}

			if (m_nverbose > 1) {
				Print.dialog("Reestimating " + outputHMMSetFileName + " from " + patternsFileNames[ntableEntry]);
			}

			continuousHMMs[ntableEntry] = useBaumWelch(continuousHMMs[ntableEntry],
					setOfPatterns,
					patternsFileNames[ntableEntry],
					m_reestimationLogFileNameFullPath);
			Print.updateJProgressBar(ntableEntry+1);
		}

		//write output file with reestimated HMMs
		String outputHMMDirectory = FileNamesAndDirectories.getPathFromFileName(outputHMMSetFileName);
		FileNamesAndDirectories.createDirectoriesIfNecessary(outputHMMDirectory);
		setOfPlainContinuousHMMs = new SetOfPlainContinuousHMMs(continuousHMMs,
				hmmFileNames, tableOfLabels, patternGeneratorOfAllFiles);
		setOfPlainContinuousHMMs.writeToJARFile(outputHMMSetFileName,
				patternsFileNames);
		return setOfPlainContinuousHMMs;
	}


	/**
	 *  Description of the Method
	 *
	 *@param  initialHMMSetJARFileName                  Description of Parameter
	 *@param  trainingDataLocatorFileName               Description of Parameter
	 *@param  outputHMMSetJARFileName                   Description of Parameter
	 *@param  headerProperties                          Description of Parameter
	 *@param  patternGenerator                          Description of Parameter
	 *@param  sentenceTrainParametersDataRootDirectory  Description of Parameter
	 *@return                                           Description of the Returned
	 *      Value
	 */
	public SetOfSharedContinuousHMMs useOneIterationOfEmbeddedBaumWelch(String initialHMMSetJARFileName,
			String trainingDataLocatorFileName,
			String outputHMMSetJARFileName,
			HeaderProperties headerProperties,
			PatternGenerator patternGenerator,
			String sentenceTrainParametersDataRootDirectory) {

		if (m_nverbose > 1) {
			Print.dialog("Reestimating " + initialHMMSetJARFileName +
			" => " + outputHMMSetJARFileName);
		}

		return useOneIterationOfEmbeddedBaumWelch(getSetOfSharedContinuousHMMs(initialHMMSetJARFileName),
				trainingDataLocatorFileName,
				outputHMMSetJARFileName,
				headerProperties,
				patternGenerator,
				sentenceTrainParametersDataRootDirectory);
	}

	/**
	 *  Runs (only 1 iteration) of embedded Baum-Welch.
	 *
	 *@param  setOfSharedContinuousHMMs                 Description of Parameter
	 *@param  trainingDataLocatorFileName               Description of Parameter
	 *@param  outputHMMSetJARFileName                   Description of Parameter
	 *@param  headerProperties                          Description of Parameter
	 *@param  patternGenerator                          Description of Parameter
	 *@param  sentenceTrainParametersDataRootDirectory  Description of Parameter
	 *@return                                           Description of the Returned
	 *      Value
	 */
	private SetOfSharedContinuousHMMs useOneIterationOfEmbeddedBaumWelch(SetOfSharedContinuousHMMs setOfSharedContinuousHMMs,
			String trainingDataLocatorFileName,
			String outputHMMSetJARFileName,
			HeaderProperties headerProperties,
			PatternGenerator patternGenerator,
			String sentenceTrainParametersDataRootDirectory) {
		SetOfSharedContinuousHMMsBeingReestimated
				setOfSharedContinuousHMMsBeingReestimated = new SetOfSharedContinuousHMMsBeingReestimated(setOfSharedContinuousHMMs);

		if (m_oshouldSkipReestimationIfFileExists) {
			boolean odoesHMMFileExist = doesHMMFileExist(outputHMMSetJARFileName);
			if (m_nverbose > 0) {
				if (odoesHMMFileExist) {
					Print.dialog("Skipping because " + IO.getEndOfString(outputHMMSetJARFileName,45) + " already exists");
				}
			}
			if (odoesHMMFileExist) {
				/** @todo I should do something like:
				SetOfSharedContinuousHMMs setOfSharedContinuousHMMs = new SetOfSharedContinuousHMMs(outputHMMSetFileName);
				if (setOfSharedContinuousHMMs.areModelsOk()) {
					return setOfSharedContinuousHMMs;
				}
				but method areModelsOk() is not implemented for shared HMMs*/
				return new SetOfSharedContinuousHMMs(outputHMMSetJARFileName);
			}
		}
		if (m_nverbose > 0) {
			Print.dialog("Embedded Baum-Welch: " + IO.getEndOfString(outputHMMSetJARFileName,45));
			//Print.dialog("creating " + outputHMMSetJARFileName);
		}

		String outputOccupationStatisticsFileName = FileNamesAndDirectories.getPathFromFileName(outputHMMSetJARFileName) +
		"OccupationStatistics.txt";

		SetOfSharedContinuousHMMsReestimator setOfSharedContinuousHMMsReestimator =
				new SetOfSharedContinuousHMMsReestimator(setOfSharedContinuousHMMsBeingReestimated,
				setOfSharedContinuousHMMs.getPatternGenerator(),
				headerProperties,
		//outputHMMSetFileName,
				trainingDataLocatorFileName,
				sentenceTrainParametersDataRootDirectory,
				outputOccupationStatisticsFileName);

		//note I could not use:
		//setOfSharedContinuousHMMs = setOfSharedContinuousHMMsReestimator.getSetOfSharedContinuousHMMs();
		//because in Java object reference is passed by value and the caller method would not
		//get the new HMM, but would continue with the old one
		SetOfSharedContinuousHMMs newSetOfSharedContinuousHMMs = setOfSharedContinuousHMMsReestimator.getSetOfReestimatedHMMs();

		writeSetOfSharedHMMInBothHTKAndSpockFormat(outputHMMSetJARFileName,
			newSetOfSharedContinuousHMMs,
			patternGenerator);

		return newSetOfSharedContinuousHMMs;
	}

	public void writeSetOfSharedHMMInBothHTKAndSpockFormat(String outputHMMSetJARFileName,
	SetOfSharedContinuousHMMs newSetOfSharedContinuousHMMs,
	PatternGenerator patternGenerator) {
		//write first in HTK format
		String outputDirectory = FileNamesAndDirectories.getPathFromFileName(outputHMMSetJARFileName);
		FileNamesAndDirectories.createDirectoriesIfNecessary(outputDirectory);
		String mappingFromLogicalToPhysicalHMMFileName = outputDirectory + "MappingFromLogicalToPhysicalHMM.txt";
		HTKInterfacer.createMapOfHMMSharing(newSetOfSharedContinuousHMMs.getTableOfLabels(), mappingFromLogicalToPhysicalHMMFileName);
		String setOfHMMInHTKFormatFileName = outputDirectory + "hmms.txt";
		HTKInterfacer.writeSetOfSharedHMMs(newSetOfSharedContinuousHMMs, setOfHMMInHTKFormatFileName);
		//now write as JAR file
		newSetOfSharedContinuousHMMs.writeToJARFile(setOfHMMInHTKFormatFileName,
				mappingFromLogicalToPhysicalHMMFileName,
				patternGenerator,
				outputHMMSetJARFileName);
	}


	public SetOfSharedContinuousHMMs useOneIterationOfEmbeddedBaumWelchWithoutWritingFiles(SetOfSharedContinuousHMMs setOfSharedContinuousHMMs,
			String trainingDataLocatorFileName,
			String outputHMMSetJARFileName,
			HeaderProperties headerProperties,
			PatternGenerator patternGenerator,
			String sentenceTrainParametersDataRootDirectory) {
		SetOfSharedContinuousHMMsBeingReestimated
				setOfSharedContinuousHMMsBeingReestimated = new SetOfSharedContinuousHMMsBeingReestimated(setOfSharedContinuousHMMs);

		if (m_oshouldSkipReestimationIfFileExists) {
			boolean odoesHMMFileExist = doesHMMFileExist(outputHMMSetJARFileName);
			if (m_nverbose > 0) {
				if (odoesHMMFileExist) {
					Print.dialog("Skipping because " + IO.getEndOfString(outputHMMSetJARFileName,45) + " already exists");
				}
			}
			if (odoesHMMFileExist) {
				/** @todo I should do something like:
				SetOfSharedContinuousHMMs setOfSharedContinuousHMMs = new SetOfSharedContinuousHMMs(outputHMMSetFileName);
				if (setOfSharedContinuousHMMs.areModelsOk()) {
					return setOfSharedContinuousHMMs;
				}
				but method areModelsOk() is not implemented for shared HMMs*/
				return new SetOfSharedContinuousHMMs(outputHMMSetJARFileName);
			}
		}

		String outputOccupationStatisticsFileName = FileNamesAndDirectories.getPathFromFileName(outputHMMSetJARFileName) +
		"OccupationStatistics.txt";

		SetOfSharedContinuousHMMsReestimator setOfSharedContinuousHMMsReestimator =
				new SetOfSharedContinuousHMMsReestimator(setOfSharedContinuousHMMsBeingReestimated,
				setOfSharedContinuousHMMs.getPatternGenerator(),
				headerProperties,
		//outputHMMSetFileName,
				trainingDataLocatorFileName,
				sentenceTrainParametersDataRootDirectory,
				outputOccupationStatisticsFileName);

		//AK -XXX - BUGGY:
		//note I could not use:
		//setOfSharedContinuousHMMs = setOfSharedContinuousHMMsReestimator.getSetOfSharedContinuousHMMs();
		//because in Java object reference is passed by value and the caller method would not
		//get the new HMM, but would continue with the old one
		SetOfSharedContinuousHMMs newSetOfSharedContinuousHMMs = setOfSharedContinuousHMMsReestimator.getSetOfReestimatedHMMs();

		return newSetOfSharedContinuousHMMs;
	}

	private boolean doesHMMFileExist(String outputHMMSetJARFileName) {
		File file = new File(outputHMMSetJARFileName);
		if (file.exists()) {
			return true;
		} else {
			return false;
		}
	}

	public boolean isNecessaryToCreateHMMSet(String outputHMMSetFileName) {
		if (m_oshouldSkipReestimationIfFileExists) {
			boolean odoesHMMFileExist = doesHMMFileExist(outputHMMSetFileName);
			if (odoesHMMFileExist) {
				//check if it is a valid HMM set
				SetOfHMMs setOfHMMs = SetOfHMMsFile.read(outputHMMSetFileName);
				if (setOfHMMs.areModelsOk()) {
					return false;
				} else {
					return true;
				}
			} else {
				return true;

			}
		} else {
			//user wants to overwrite file if it exists
			return true;
		}
	}


	/**
	 *  Description of the Method
	 *
	 *@param  initialHMMSetJARFileName                  Description of Parameter
	 *@param  nnumberOfIncrementsInGaussiansPerMixture  Description of Parameter
	 *@param  rootOutputDirectoryForHMMs                Description of Parameter
	 *@param  directoryWithSOPFiles                     Description of Parameter
	 *@param  ninitialNumberOfGaussiansPerMixture       Description of Parameter
	 *@return                                           Description of the Returned
	 *      Value
	 */
	public SetOfPlainContinuousHMMs useBaumWelchRecursively(String initialHMMSetJARFileName,
			int nnumberOfIncrementsInGaussiansPerMixture,
	//int nnumberOfIterations,
			int ninitialNumberOfGaussiansPerMixture,
			String rootOutputDirectoryForHMMs,
			String directoryWithSOPFiles) {
		SetOfHMMs setOfHMMs = SetOfHMMsFile.read(initialHMMSetJARFileName);
		if (!(setOfHMMs instanceof SetOfPlainContinuousHMMs)) {
			End.throwError(initialHMMSetJARFileName + " is not a SetOfPlainContinuousHMMs file.");
		}
		return useBaumWelchRecursively((SetOfPlainContinuousHMMs) setOfHMMs,
				nnumberOfIncrementsInGaussiansPerMixture,
		//nnumberOfIterations,
				ninitialNumberOfGaussiansPerMixture,
				rootOutputDirectoryForHMMs,
				directoryWithSOPFiles);
	}


	/**
	 *  Description of the Method
	 *
	 *@param  setOfPlainContinuousHMMs                  Description of Parameter
	 *@param  nnumberOfIncrementsInGaussiansPerMixture  Description of Parameter
	 *@param  rootOutputDirectoryForHMMs                Description of Parameter
	 *@param  directoryWithSOPFiles                     Description of Parameter
	 *@param  ninitialNumberOfGaussiansPerMixture       Description of Parameter
	 *@return                                           Description of the Returned
	 *      Value
	 */
	public SetOfPlainContinuousHMMs useBaumWelchRecursively(SetOfPlainContinuousHMMs setOfPlainContinuousHMMs,
			int nnumberOfIncrementsInGaussiansPerMixture,
	//int nnumberOfIterations,
			int ninitialNumberOfGaussiansPerMixture,
			String rootOutputDirectoryForHMMs,
			String directoryWithSOPFiles) {
		directoryWithSOPFiles = FileNamesAndDirectories.replaceAndForceEndingWithSlash(directoryWithSOPFiles);
		SetOfPlainContinuousHMMs newSetOfPlainContinuousHMMs = setOfPlainContinuousHMMs;
		for (int i = ninitialNumberOfGaussiansPerMixture; i <= nnumberOfIncrementsInGaussiansPerMixture; i++) {
			newSetOfPlainContinuousHMMs.splitGaussianWithLargestWeightForAllMixtures();
			//for (int j = 0; j < nnumberOfIterations; j++) {
			//XXX create a preffered name method in HMM
			//String outputHMMSetFileName = rootOutputDirectoryForHMMs + i + "_iteration" + j + "/hmms.jar";
			String outputHMMSetFileName = rootOutputDirectoryForHMMs + i + "/" + SetOfHMMsFile.m_name;
			newSetOfPlainContinuousHMMs = useBaumWelch(newSetOfPlainContinuousHMMs,
					directoryWithSOPFiles,
					outputHMMSetFileName);
			//}
		}
		//return last set of HMMs
		return newSetOfPlainContinuousHMMs;
	}


	/**
	 *  Description of the Method
	 *
	 *@param  initialHMMSetJARFileName                  Description of Parameter
	 *@param  nnumberOfIncrementsInGaussiansPerMixture  Description of Parameter
	 *@param  nnumberOfIterations                       Description of Parameter
	 *@param  rootOutputDirectoryForHMMs                Description of Parameter
	 *@param  trainingDataLocatorFileName               Description of Parameter
	 *@param  headerProperties                          Description of Parameter
	 *@param  patternGenerator                          Description of Parameter
	 *@param  sentenceTrainParametersDataRootDirectory  Description of Parameter
	 *@param  ninitialNumberOfGaussiansPerMixture       Description of Parameter
	 *@return                                           Description of the Returned
	 *      Value
	 */
	public SetOfSharedContinuousHMMs useEmbeddedBaumWelchRecursively(String initialHMMSetJARFileName,
			int nnumberOfIncrementsInGaussiansPerMixture,
			int ninitialNumberOfGaussiansPerMixture,
			int nnumberOfIterations,
			String rootOutputDirectoryForHMMs,
			String trainingDataLocatorFileName,
			HeaderProperties headerProperties,
			PatternGenerator patternGenerator,
			String sentenceTrainParametersDataRootDirectory) {
		return useEmbeddedBaumWelchRecursively(getSetOfSharedContinuousHMMs(initialHMMSetJARFileName),
				nnumberOfIncrementsInGaussiansPerMixture,
				ninitialNumberOfGaussiansPerMixture,
				nnumberOfIterations,
				rootOutputDirectoryForHMMs,
				trainingDataLocatorFileName,
				headerProperties,
				patternGenerator,
				sentenceTrainParametersDataRootDirectory);
	}


	/**
	 *  Description of the Method
	 *
	 *@param  setOfSharedContinuousHMMs                 Description of Parameter
	 *@param  nnumberOfIncrementsInGaussiansPerMixture  Description of Parameter
	 *@param  nnumberOfIterations                       Description of Parameter
	 *@param  rootOutputDirectoryForHMMs                Description of Parameter
	 *@param  trainingDataLocatorFileName               Description of Parameter
	 *@param  headerProperties                          Description of Parameter
	 *@param  patternGenerator                          Description of Parameter
	 *@param  sentenceTrainParametersDataRootDirectory  Description of Parameter
	 *@param  ninitialNumberOfGaussiansPerMixture       Description of Parameter
	 *@return                                           Description of the Returned
	 *      Value
	 */
	public SetOfSharedContinuousHMMs useEmbeddedBaumWelchRecursively(SetOfSharedContinuousHMMs setOfSharedContinuousHMMs,
			int nnumberOfIncrementsInGaussiansPerMixture,
			int ninitialNumberOfGaussiansPerMixture,
			int nnumberOfIterations,
			String rootOutputDirectoryForHMMs,
			String trainingDataLocatorFileName,
			HeaderProperties headerProperties,
			PatternGenerator patternGenerator,
			String sentenceTrainParametersDataRootDirectory) {

		sentenceTrainParametersDataRootDirectory = FileNamesAndDirectories.replaceAndForceEndingWithSlash(sentenceTrainParametersDataRootDirectory);
		SetOfSharedContinuousHMMs newSetOfSharedContinuousHMMs = setOfSharedContinuousHMMs;
		//I could do: reestimate and then split but
		//this way is better for the simulations I am doing right now
		Print.setJProgressBarRange(0,(nnumberOfIncrementsInGaussiansPerMixture-ninitialNumberOfGaussiansPerMixture+1)*nnumberOfIterations);
		Print.updateJProgressBar(0);
		for (int i = ninitialNumberOfGaussiansPerMixture; i <= nnumberOfIncrementsInGaussiansPerMixture; i++) {
			//split
			newSetOfSharedContinuousHMMs.splitGaussianWithLargestWeightForAllMixtures();
			if (m_oshouldSaveHMMAfterSplit) {
			   String outputDirectory = rootOutputDirectoryForHMMs + "embedded_" + i + "G_before_split/";
			   writeHMMAfterSplit(outputDirectory,newSetOfSharedContinuousHMMs,patternGenerator);
			}

			//reestimate
			newSetOfSharedContinuousHMMs = useEmbeddedBaumWelch(newSetOfSharedContinuousHMMs,
					i,
					nnumberOfIterations,
					rootOutputDirectoryForHMMs,
					trainingDataLocatorFileName,
					headerProperties,
					patternGenerator,
					sentenceTrainParametersDataRootDirectory);
		}
		//return last set of HMMs
		return newSetOfSharedContinuousHMMs;
	}


	/**
	 *  Description of the Method
	 *
	 *@param  initialHMMSetJARFileName                  Description of Parameter
	 *@param  nnumberOfGaussiansPerMixture              Description of Parameter
	 *@param  nnumberOfIterations                       Description of Parameter
	 *@param  rootOutputDirectoryForHMMs                Description of Parameter
	 *@param  trainingDataLocatorFileName               Description of Parameter
	 *@param  headerProperties                          Description of Parameter
	 *@param  patternGenerator                          Description of Parameter
	 *@param  sentenceTrainParametersDataRootDirectory  Description of Parameter
	 *@return                                           Description of the Returned
	 *      Value
	 */
	public SetOfSharedContinuousHMMs useEmbeddedBaumWelch(String initialHMMSetJARFileName,
			int nnumberOfGaussiansPerMixture,
			int nnumberOfIterations,
			String rootOutputDirectoryForHMMs,
			String trainingDataLocatorFileName,
			HeaderProperties headerProperties,
			PatternGenerator patternGenerator,
			String sentenceTrainParametersDataRootDirectory) {

//		if (m_nverbose > 0) {
//			Print.dialog("Reestimating " + IO.getEndOfString(initialHMMSetJARFileName,25) +
//			" to " + IO.getEndOfString(rootOutputDirectoryForHMMs,25));
//		}

		return useEmbeddedBaumWelch(getSetOfSharedContinuousHMMs(initialHMMSetJARFileName),
				nnumberOfGaussiansPerMixture,
				nnumberOfIterations,
				rootOutputDirectoryForHMMs,
				trainingDataLocatorFileName,
				headerProperties,
				patternGenerator,
				sentenceTrainParametersDataRootDirectory);
	}


	public static String getDirectoryForEmbeddedIteration(String rootOutputDirectoryForHMMs,
	int nnumberOfGaussiansPerMixture,
	int niteration) {
		//want to have the same number of subdirectories as baumwelch and segmental k-means,
		//to organize a "simulation path" so do not add iteration numbes
		//return rootOutputDirectoryForHMMs + "embedded/" + nnumberOfGaussiansPerMixture + "G" + (niteration+1) + "it/";
		return rootOutputDirectoryForHMMs + "embedded/" + nnumberOfGaussiansPerMixture + "/";
	}

	/**
	 * Writes only last set of HMMs
	 */
	public SetOfSharedContinuousHMMs useEmbeddedBaumWelch(SetOfSharedContinuousHMMs setOfSharedContinuousHMMs,
			int nnumberOfGaussiansPerMixture,
			int nnumberOfIterations,
			String rootOutputDirectoryForHMMs,
			String trainingDataLocatorFileName,
			HeaderProperties headerProperties,
			PatternGenerator patternGenerator,
			String sentenceTrainParametersDataRootDirectory) {

		sentenceTrainParametersDataRootDirectory = FileNamesAndDirectories.replaceAndForceEndingWithSlash(sentenceTrainParametersDataRootDirectory);
		SetOfSharedContinuousHMMs newSetOfSharedContinuousHMMs = setOfSharedContinuousHMMs;

		//impose range on the recursive method
		//Print.setJProgressBarRange(0, nnumberOfIterations);
		//Print.updateJProgressBar(0);
		String outputHMMSetJARFileName = null;
		for (int j = 0; j < nnumberOfIterations; j++) {
			//XXX create a preffered name method in HMM
			outputHMMSetJARFileName = getDirectoryForEmbeddedIteration(rootOutputDirectoryForHMMs,nnumberOfGaussiansPerMixture ,j) + SetOfHMMsFile.m_name;
			if (m_nverbose > 0) {
				Print.dialog("Embedded (iteration # " + (j+1) + "): " + IO.getEndOfString(outputHMMSetJARFileName, 50));
			}
			newSetOfSharedContinuousHMMs = useOneIterationOfEmbeddedBaumWelchWithoutWritingFiles(newSetOfSharedContinuousHMMs,
					trainingDataLocatorFileName,
					outputHMMSetJARFileName,
					headerProperties,
					patternGenerator,
					sentenceTrainParametersDataRootDirectory);
			Print.incrementJProgressBar();
		}

		//write last set
		writeSetOfSharedHMMInBothHTKAndSpockFormat(outputHMMSetJARFileName,
		newSetOfSharedContinuousHMMs, patternGenerator);

		//return last set of HMMs
		return newSetOfSharedContinuousHMMs;
	}


	/**
	 *  Gets the SetOfSharedContinuousHMMs attribute of the HMMReestimator object
	 *
	 *@param  initialHMMSetJARFileName  Description of Parameter
	 *@return                           The SetOfSharedContinuousHMMs value
	 */
	private SetOfSharedContinuousHMMs getSetOfSharedContinuousHMMs(String initialHMMSetJARFileName) {
		SetOfHMMs setOfHMMs = SetOfHMMsFile.read(initialHMMSetJARFileName);
		if (setOfHMMs instanceof SetOfPlainContinuousHMMs) {
			//need to convert it to shared
			setOfHMMs = ((SetOfPlainContinuousHMMs) setOfHMMs).convertToSharedHMMs();
		}
		if (!(setOfHMMs instanceof SetOfSharedContinuousHMMs)) {
			End.throwError(initialHMMSetJARFileName + " is not a SetOfSharedContinuousHMMs file.");
		}
		return (SetOfSharedContinuousHMMs) setOfHMMs;
	}

	private void writeHMMAfterSplit(String outputDirectory,
	SetOfSharedContinuousHMMs newSetOfSharedContinuousHMMs,
	PatternGenerator patternGenerator) {
		//write first in HTK format
		//String outputDirectory = FileNamesAndDirectories.getPathFromFileName(outputHMMSetJARFileName);
		FileNamesAndDirectories.createDirectoriesIfNecessary(outputDirectory);
		String mappingFromLogicalToPhysicalHMMFileName = outputDirectory + "MappingFromLogicalToPhysicalHMM.txt";
		HTKInterfacer.createMapOfHMMSharing(newSetOfSharedContinuousHMMs.getTableOfLabels(), mappingFromLogicalToPhysicalHMMFileName);
		String setOfHMMInHTKFormatFileName = outputDirectory + "hmms.txt";
		String outputHMMSetJARFileName = outputDirectory + SetOfHMMsFile.m_name;
		HTKInterfacer.writeSetOfSharedHMMs(newSetOfSharedContinuousHMMs, setOfHMMInHTKFormatFileName);
		//now write as JAR file
		newSetOfSharedContinuousHMMs.writeToJARFile(setOfHMMInHTKFormatFileName,
				mappingFromLogicalToPhysicalHMMFileName,
				patternGenerator,
				outputHMMSetJARFileName);
	}

	/**
	 *  Description of the Method
	 */
	public SetOfPlainContinuousHMMs createMonophoneHMMPrototypes(TableOfLabels tableOfLabels,
	String hmmsDirectory,
	String patternsDirectory,
	String thisHMMConfigurationFileName,
	PatternGenerator patternGenerator) {

		patternsDirectory = FileNamesAndDirectories.forceEndingWithSlash(patternsDirectory);
		hmmsDirectory = FileNamesAndDirectories.forceEndingWithSlash(hmmsDirectory);

		int nnumberOfEntries = tableOfLabels.getNumberOfEntries();
		//to store all HMMs and their file names
		ContinuousHMM[] continuousHMMs = new ContinuousHMM[nnumberOfEntries];
		String[] hmmFileNames = new String[nnumberOfEntries];

		//output directory
		//String hmmsDirectory = m_simulationFilesAndDirectories.getIsolatedMonophoneHMMPrototypesDirectory();
		if (m_nverbose > 0) {
			Print.dialog("HMM prototypes: " + IO.getEndOfString(hmmsDirectory,55));
		}

		//Print.setJProgressBarRange(0,nnumberOfEntries);
		for (int ntableEntry = 0; ntableEntry < nnumberOfEntries; ntableEntry++) {
			//get file names
			String fileName = tableOfLabels.getPrefferedName(ntableEntry, HMM.m_FILE_EXTENSION);
			String patternsFileName = patternsDirectory + tableOfLabels.getPrefferedName(ntableEntry, SetOfPatterns.m_FILE_EXTENSION);

			//String thisHMMConfigurationFileName = m_simulationFilesAndDirectories.getHMMConfigurationFileNameForIsolatedSegments(ntableEntry);

			if (m_nverbose > 1) {
				Print.dialog("Creating HMM model for table entry number " + ntableEntry + " from configuration file " +
						thisHMMConfigurationFileName + ", using parameters of file " + patternsFileName);
			}

			//get prototype
			ContinuousHMM continuousHMM = HMMInitializer.getHMMPrototypeBasedOnGivenTopologyAndGlobalMeanAndVariance(thisHMMConfigurationFileName, patternsFileName);

			//store this HMM and its file name
			continuousHMMs[ntableEntry] = continuousHMM;
			hmmFileNames[ntableEntry] = fileName;
			//Print.updateJProgressBar(ntableEntry+1);
		}
		//create a set of HMMs
		SetOfPlainContinuousHMMs
				setOfPlainContinuousHMMs
				 = new SetOfPlainContinuousHMMs(continuousHMMs,
				hmmFileNames,
				tableOfLabels,
				patternGenerator);

		//write JAR file with HMMs
		//FileNamesAndDirectories.createDirectoriesIfNecessary(hmmsDirectory);
		//setOfPlainContinuousHMMs.writeToJARFile(hmmsDirectory + m_simulationFilesAndDirectories.getSetOfHMMsFileName(),
		//		m_headerProperties);
		return setOfPlainContinuousHMMs;
	}

}
//end of class

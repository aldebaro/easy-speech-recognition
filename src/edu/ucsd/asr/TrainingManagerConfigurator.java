package edu.ucsd.asr;

import java.io.File;
import ioproperties.*;

/**
 *  Organize directories and files of training sessions. Notice that every
 *  information concerning the original location of files (TIMIT database CD) is
 *  not part of this class. Here we put only information that follow the
 *  organization of directories and files adopted by our system.
 *
 *@author     Aldebaro Klautau
 *@created    November 24, 2000
 *@version    2.0 - October 01, 2000.
 */
public class TrainingManagerConfigurator {

	private HeaderProperties m_headerProperties;
	private final String m_setOfHMMsFileName = SetOfHMMsFile.m_name;
	private String m_transcriptionFileExtension;
	private String m_defaultHMMConfigurationFileName;
	private String m_featuresDescription;
	private String m_topologyIdentifier;

	//files
	private String m_listOfCrossWordTriphonesInTrainingDataFileName;
	private String m_listOfAllPossibleCrossWordTriphones = "ListOfAllPossibleCrossWordTriphones.txt";
	private String m_mappingOfLogicalIntoPhysicalTriphonesFileName;
	private String m_mappingOfLogicalIntoPhysicalMonophonesFileName;
	private final String m_hviteConfigurationFileName = "/htk/hviteForMonophones.conf";
	private final String m_hviteForCrossWordTriphonesConfigurationFileName = "/htk/hviteForCrossWordTriphones.conf";

	//depend only on database
	//assume database is composed by sentences (as in TIMIT)
	private String m_generalDatabaseDirectory;
	private String m_trainSpeechDataRootDirectory;
	private String m_testSpeechDataRootDirectory;
	private String m_sentenceMonophoneTranscriptionsOfTrainSpeechDataFileName;
	private String m_sentenceMonophoneTranscriptionsOfTestSpeechDataFileName;
	//depend on database + parametric representation
	private String m_sentenceTrainParametersDataRootDirectory;
	private String m_sentenceTestParametersDataRootDirectory;

	//depend on database + phone set (table of labels)
	private String m_generalOutputDirectoryWithoutFeatureDescription;
	private String m_isolatedMonophoneTranscriptionsOfTrainSpeechDataDirectory;
	private String m_isolatedMonophoneTranscriptionsOfTestSpeechDataDirectory;
	private String m_sentenceTriphoneTranscriptionsOfTrainSpeechDataFileName;
	private String m_sentenceTriphoneTranscriptionsOfTestSpeechDataFileName;

	//depend on database + phone set (table of labels) + parametric representation
	private String m_generalOutputDirectory;
	private String m_isolatedTrainParametersDataRootDirectory;
	private String m_isolatedTestParametersDataRootDirectory;

	//still depend on database + phone set (table of labels) + parametric representation
	//Files and directories for HMMs
	//isolated segments monophones
	private String m_isolatedMonophoneHMMPrototypesDirectory;
	private String m_isolatedMonophoneHMMsBaumWelchDirectory;
	private String m_isolatedMonophoneHMMsKMeansViterbiDirectory;
	private String m_isolatedMonophoneHMMsFinalDirectory;
	//sentences monophones
	private String m_sentencesMonophoneHMMsFinalDirectory;
	//sentences triphones
	private String m_sentencesPlainTriphoneHMMPrototypesDirectory;
	private String m_sentencesPlainTriphoneHMMsFinalDirectory;
	private String m_sentencesSharedTriphoneHMMPrototypesDirectory;
	private String m_sentencesSharedTriphoneHMMsFinalDirectory;
	//auxiliary intermediate directories
	private String m_directoryForMostRecentMonophoneHMMs;
	private String m_directoryForMostRecentTriphoneHMMs;

	/**
	 *  Constructor for the SimulationFilesAndDirectories object
	 *
	 *@param  headerProperties  Description of Parameter
	 */
	public TrainingManagerConfigurator(HeaderProperties headerProperties) {
		m_headerProperties = headerProperties;
		m_featuresDescription = PatternGenerator.getPatternGenerator(headerProperties).getDescription();
		interpretHeaderAndInitialize();
	}


	/**
	 *  Gets the TrainSpeechDataRootDirectory attribute of the
	 *  SimulationFilesAndDirectories object
	 *
	 *@return    The TrainSpeechDataRootDirectory value
	 */
	public String getTrainSpeechDataRootDirectory() {
		return m_trainSpeechDataRootDirectory;
	}


	/**
	 *  Gets the TestSpeechDataRootDirectory attribute of the
	 *  SimulationFilesAndDirectories object
	 *
	 *@return    The TestSpeechDataRootDirectory value
	 */
	public String getTestSpeechDataRootDirectory() {
		return m_testSpeechDataRootDirectory;
	}


	/**
	 *  Gets the PlainTriphoneHMMPrototypesFile attribute of the
	 *  SimulationFilesAndDirectories object
	 *
	 *@return    The PlainTriphoneHMMPrototypesFile value
	 */
	public String getPlainTriphoneHMMPrototypesFile() {
		return m_sentencesPlainTriphoneHMMPrototypesDirectory + m_setOfHMMsFileName;
	}


	/**
	 *  Gets the SharedTriphoneHMMPrototypesFile attribute of the
	 *  SimulationFilesAndDirectories object
	 *
	 *@return    The SharedTriphoneHMMPrototypesFile value
	 */
	public String getSharedTriphoneHMMPrototypesFile() {
		return m_sentencesSharedTriphoneHMMPrototypesDirectory + m_setOfHMMsFileName;
	}


	/**
	 *  Gets the SetOfHMMsFileName attribute of the SimulationFilesAndDirectories
	 *  object
	 *
	 *@return    The SetOfHMMsFileName value
	 */
	public String getSetOfHMMsFileName() {
		return m_setOfHMMsFileName;
	}

	public String getSetOfHMMsJARFileName() {
		return FileNamesAndDirectories.substituteExtension(m_setOfHMMsFileName,SetOfHMMsFile.m_extension);
	}


	/**
	 *  Gets the DefaultHMMConfigurationFileName attribute of the
	 *  SimulationFilesAndDirectories object
	 *
	 *@return    The DefaultHMMConfigurationFileName value
	 */
	public String getDefaultHMMConfigurationFileName() {
		return m_defaultHMMConfigurationFileName;
	}


	/**
	 *  Gets the ListOfCrossWordTriphonesInTrainingDataFileName attribute of the
	 *  SimulationFilesAndDirectories object
	 *
	 *@return    The ListOfCrossWordTriphonesInTrainingDataFileName value
	 */
	public String getListOfCrossWordTriphonesInTrainingDataFileName() {
		return m_listOfCrossWordTriphonesInTrainingDataFileName;
	}

	public String getListOfAllPossibleCrossWordTriphones() {
		String dir = m_sentencesSharedTriphoneHMMPrototypesDirectory;
		FileNamesAndDirectories.createDirectoriesIfNecessary(dir);
		return dir + m_listOfAllPossibleCrossWordTriphones;
	}

	/**
	 *  Gets the MappingOfLogicalIntoPhysicalTriphonesFileName attribute of the
	 *  SimulationFilesAndDirectories object
	 *
	 *@return    The MappingOfLogicalIntoPhysicalTriphonesFileName value
	 */
	public String getMappingOfLogicalIntoPhysicalTriphonesFileName() {
		return m_mappingOfLogicalIntoPhysicalTriphonesFileName;
	}

	public String getMappingOfLogicalIntoPhysicalMonophonesFileName() {
		return m_mappingOfLogicalIntoPhysicalMonophonesFileName;
	}

	/**
	 *  Gets the HviteConfigurationFileName attribute of the
	 *  SimulationFilesAndDirectories object
	 *
	 *@return    The HviteConfigurationFileName value
	 */
	public String getHviteConfigurationFileName() {
		return m_hviteConfigurationFileName;
	}


	/**
	 *  Gets the HviteForCrossWordTriphonesConfigurationFileName attribute of the
	 *  SimulationFilesAndDirectories object
	 *
	 *@return    The HviteForCrossWordTriphonesConfigurationFileName value
	 */
	public String getHviteForCrossWordTriphonesConfigurationFileName() {
		return m_hviteForCrossWordTriphonesConfigurationFileName;
	}


	/**
	 *  Gets the SentenceTranscriptionsOfTrainSpeechDataFileName attribute of the
	 *  SimulationFilesAndDirectories object
	 *
	 *@return    The SentenceTranscriptionsOfTrainSpeechDataFileName value
	 */
	public String getSentenceMonophoneTranscriptionsOfTrainSpeechDataFileName() {
		return m_sentenceMonophoneTranscriptionsOfTrainSpeechDataFileName;
	}

	public String getSentenceTriphoneTranscriptionsOfTrainSpeechDataFileName() {
		return m_sentenceTriphoneTranscriptionsOfTrainSpeechDataFileName;
	}

	/**
	 *  Gets the SentenceTranscriptionsOfTestSpeechDataFileName attribute of the
	 *  SimulationFilesAndDirectories object
	 *
	 *@return    The SentenceTranscriptionsOfTestSpeechDataFileName value
	 */
	public String getSentenceMonophoneTranscriptionsOfTestSpeechDataFileName() {
		return m_sentenceMonophoneTranscriptionsOfTestSpeechDataFileName;
	}

	public String getSentenceTriphoneTranscriptionsOfTestSpeechDataFileName() {
		return m_sentenceTriphoneTranscriptionsOfTestSpeechDataFileName;
	}

	/**
	 *  Gets the IsolatedTranscriptionsOfTrainSpeechDataDirectory attribute of the
	 *  SimulationFilesAndDirectories object
	 *
	 *@return    The IsolatedTranscriptionsOfTrainSpeechDataDirectory value
	 */
	public String getIsolatedMonophoneTranscriptionsOfTrainSpeechDataDirectory() {
		return m_isolatedMonophoneTranscriptionsOfTrainSpeechDataDirectory;
	}


	/**
	 *  Gets the IsolatedTranscriptionsOfTestSpeechDataDirectory attribute of the
	 *  SimulationFilesAndDirectories object
	 *
	 *@return    The IsolatedTranscriptionsOfTestSpeechDataDirectory value
	 */
	public String getIsolatedMonophoneTranscriptionsOfTestSpeechDataDirectory() {
		return m_isolatedMonophoneTranscriptionsOfTestSpeechDataDirectory;
	}


	/**
	 *  Gets the GeneralOutputDirectory attribute of the
	 *  SimulationFilesAndDirectories object
	 *
	 *@return    The GeneralOutputDirectory value
	 */
	public String getGeneralOutputDirectory() {
		return m_generalOutputDirectory;
	}


	/**
	 *  Gets the SentenceTrainParametersDataRootDirectory attribute of the
	 *  SimulationFilesAndDirectories object
	 *
	 *@return    The SentenceTrainParametersDataRootDirectory value
	 */
	public String getSentenceTrainParametersDataRootDirectory() {
		return m_sentenceTrainParametersDataRootDirectory;
	}


	/**
	 *  Gets the SentenceTestParametersDataRootDirectory attribute of the
	 *  SimulationFilesAndDirectories object
	 *
	 *@return    The SentenceTestParametersDataRootDirectory value
	 */
	public String getSentenceTestParametersDataRootDirectory() {
		return m_sentenceTestParametersDataRootDirectory;
	}


	/**
	 *  Gets the IsolatedTrainParametersDataRootDirectory attribute of the
	 *  SimulationFilesAndDirectories object
	 *
	 *@return    The IsolatedTrainParametersDataRootDirectory value
	 */
	public String getIsolatedTrainParametersDataRootDirectory() {
		return m_isolatedTrainParametersDataRootDirectory;
	}


	/**
	 *  Gets the IsolatedTestParametersDataRootDirectory attribute of the
	 *  SimulationFilesAndDirectories object
	 *
	 *@return    The IsolatedTestParametersDataRootDirectory value
	 */
	public String getIsolatedTestParametersDataRootDirectory() {
		return m_isolatedTestParametersDataRootDirectory;
	}

	//Files and directories for HMMs
	//isolated segments monophones
	/**
	 *  Gets the IsolatedMonophoneHMMPrototypesDirectory attribute of the
	 *  SimulationFilesAndDirectories object
	 *
	 *@return    The IsolatedMonophoneHMMPrototypesDirectory value
	 */
	public String getIsolatedMonophoneHMMPrototypesDirectory() {
		return m_isolatedMonophoneHMMPrototypesDirectory;
	}


	/**
	 *  Gets the IsolatedMonophoneHMMsBaumWelchDirectory attribute of the
	 *  SimulationFilesAndDirectories object
	 *
	 *@return    The IsolatedMonophoneHMMsBaumWelchDirectory value
	 */
	public String getIsolatedMonophoneHMMsBaumWelchDirectory() {
		return m_isolatedMonophoneHMMsBaumWelchDirectory;
	}


	/**
	 *  Gets the IsolatedMonophoneHMMsKMeansViterbiDirectory attribute of the
	 *  SimulationFilesAndDirectories object
	 *
	 *@return    The IsolatedMonophoneHMMsKMeansViterbiDirectory value
	 */
	public String getIsolatedMonophoneHMMsKMeansViterbiDirectory() {
		return m_isolatedMonophoneHMMsKMeansViterbiDirectory;
	}


	/**
	 *  Gets the IsolatedMonophoneHMMsFinalDirectory attribute of the
	 *  SimulationFilesAndDirectories object
	 *
	 *@return    The IsolatedMonophoneHMMsFinalDirectory value
	 */
	public String getIsolatedMonophoneHMMsFinalDirectory() {
		return m_isolatedMonophoneHMMsFinalDirectory;
	}


	//sentences monophones
	/**
	 *  Gets the SentencesMonophoneHMMsFinalDirectory attribute of the
	 *  SimulationFilesAndDirectories object
	 *
	 *@return    The SentencesMonophoneHMMsFinalDirectory value
	 */
	public String getSentencesMonophoneHMMsFinalDirectory() {
		return m_sentencesMonophoneHMMsFinalDirectory;
	}


	//sentences triphones
	/**
	 *  Gets the SentencesPlainTriphoneHMMPrototypesDirectory attribute of the
	 *  SimulationFilesAndDirectories object
	 *
	 *@return    The SentencesPlainTriphoneHMMPrototypesDirectory value
	 */
	public String getSentencesPlainTriphoneHMMPrototypesDirectory() {
		return m_sentencesPlainTriphoneHMMPrototypesDirectory;
	}


	/**
	 *  Gets the SentencesPlainTriphoneHMMsFinalDirectory attribute of the
	 *  SimulationFilesAndDirectories object
	 *
	 *@return    The SentencesPlainTriphoneHMMsFinalDirectory value
	 */
	public String getSentencesPlainTriphoneHMMsFinalDirectory() {
		return m_sentencesPlainTriphoneHMMsFinalDirectory;
	}


	/**
	 *  Gets the SentencesSharedTriphoneHMMPrototypesDirectory attribute of the
	 *  SimulationFilesAndDirectories object
	 *
	 *@return    The SentencesSharedTriphoneHMMPrototypesDirectory value
	 */
	public String getSentencesSharedTriphoneHMMPrototypesDirectory() {
		return m_sentencesSharedTriphoneHMMPrototypesDirectory;
	}


	/**
	 *  Gets the SentencesSharedTriphoneHMMsFinalDirectory attribute of the
	 *  SimulationFilesAndDirectories object
	 *
	 *@return    The SentencesSharedTriphoneHMMsFinalDirectory value
	 */
	public String getSentencesSharedTriphoneHMMsFinalDirectory() {
		return m_sentencesSharedTriphoneHMMsFinalDirectory;
	}


	/**
	 *  Gets the DirectoryForMostRecentMonophoneHMMs attribute of the
	 *  SimulationFilesAndDirectories object
	 *
	 *@return    The DirectoryForMostRecentMonophoneHMMs value
	 */
	public String getDirectoryForMostRecentMonophoneHMMs() {
		return m_directoryForMostRecentMonophoneHMMs;
	}


	/**
	 *  Gets the DirectoryForMostRecentTriphoneHMMs attribute of the
	 *  SimulationFilesAndDirectories object
	 *
	 *@return    The DirectoryForMostRecentTriphoneHMMs value
	 */
	public String getDirectoryForMostRecentTriphoneHMMs() {
		return m_directoryForMostRecentTriphoneHMMs;
	}

	/**
	 *  Gets the MostRecentMonophoneHMMFileNameForIsolatedSegments attribute of the
	 *  SimulationFilesAndDirectories object
	 *
	 *@param  tableOfLabels  Description of Parameter
	 *@param  ntableEntry    Description of Parameter
	 *@return                The MostRecentMonophoneHMMFileNameForIsolatedSegments
	 *      value
	 */
	public String getMostRecentMonophoneHMMFileNameForIsolatedSegments(TableOfLabels tableOfLabels, int ntableEntry) {
		return m_directoryForMostRecentMonophoneHMMs + getPrefferedName(tableOfLabels, ntableEntry, HMM.m_FILE_EXTENSION);
	}

	public String getSharedTriphoneHMMsFinalDirectory() {
		return m_sentencesSharedTriphoneHMMsFinalDirectory;
	}

	public String getMostRecentTriphoneHMMFileName() {
		return m_directoryForMostRecentTriphoneHMMs + m_setOfHMMsFileName;
	}

	public String getMostRecentMonophoneHMMFileName() {
		return m_directoryForMostRecentMonophoneHMMs + m_setOfHMMsFileName;
	}

	public String getRootDirectoryForMonophoneEmbeddedTraining() {
		return m_generalOutputDirectory + m_topologyIdentifier + "/monophones/sentences/";
	}

	public String getRootDirectoryForSharedTriphoneEmbeddedTraining() {
		return m_generalOutputDirectory + m_topologyIdentifier + "/triphones/sentences/shared/";
	}

	public String getDirectoryForMonophoneEmbeddedTraining(int nnumberOfGaussiansPerMixture,
	int niterationNumber) {
		return m_generalOutputDirectory + m_topologyIdentifier + "/monophones/sentences/embedded_" + nnumberOfGaussiansPerMixture + "G_" + niterationNumber + "/";
	}

	/**
	 *  Gets the DirectoryForPlainTriphoneEmbeddedTraining attribute of the
	 *  SimulationFilesAndDirectories object
	 *
	 *@param  niterationNumber  Description of Parameter
	 *@return                   The DirectoryForPlainTriphoneEmbeddedTraining value
	 */
	public String getDirectoryForPlainTriphoneEmbeddedTraining(int niterationNumber) {
		return m_generalOutputDirectory + m_topologyIdentifier + "/triphones/plain/embedded" + niterationNumber + "/";
	}

	/**
	 *  Gets the FinalSetOfMonophoneHMMs attribute of the
	 *  SimulationFilesAndDirectories object
	 *
	 *@return    The FinalSetOfMonophoneHMMs value
	 */
	public SetOfHMMs getFinalSetOfMonophoneHMMs() {
		String fileName = m_isolatedMonophoneHMMsFinalDirectory + m_setOfHMMsFileName;
		return new SetOfPlainContinuousHMMs(fileName);
	}

	/**
	 *  Gets the PrefferedName attribute of the SimulationFilesAndDirectories
	 *  object
	 *
	 *@param  tableOfLabels  Description of Parameter
	 *@param  ntableEntry    Description of Parameter
	 *@param  extension      Description of Parameter
	 *@return                The PrefferedName value
	 */
	public static String getPrefferedName(TableOfLabels tableOfLabels, int ntableEntry, String extension) {
		if (tableOfLabels == null) {
			End.throwError("Can not call getPrefferedName() with tableOfLabels = null");
		}
		return ntableEntry +
				"_" +
				tableOfLabels.getFirstLabel(ntableEntry) +
				"." +
				extension;
	}


	/**
	 *  Gets the SOPFileNameForIsolatedSegments attribute of the
	 *  SimulationFilesAndDirectories object
	 *
	 *@param  tableOfLabels  Description of Parameter
	 *@param  ntableEntry    Description of Parameter
	 *@return                The SOPFileNameForIsolatedSegments value
	 */
	public String getSOPFileNameForIsolatedSegments(TableOfLabels tableOfLabels, int ntableEntry, String testOrTrain) {
		//create name to use as default value
		String prefferedSetOfPatternsFileName = m_generalOutputDirectory + "features/" + testOrTrain + "/" + getPrefferedName(tableOfLabels, ntableEntry, SetOfPatterns.m_FILE_EXTENSION);
		String patternsFileName = m_headerProperties.getProperty("SetOfPatterns.FileName_" + Integer.toString(ntableEntry),
				prefferedSetOfPatternsFileName);
		patternsFileName = FileNamesAndDirectories.getAbsolutePath(patternsFileName, m_isolatedTrainParametersDataRootDirectory);
		if (!(new File(patternsFileName)).exists()) {
			End.throwError("Could not find file " + patternsFileName +
					". Can you check property SetOfPatterns.FileName_" +
					ntableEntry + " in TRN file ?");
		}
		return patternsFileName;
	}


	/**
	 *  Gets the DirectoryForSharedTriphoneEmbeddedTraining attribute of the
	 *  SimulationFilesAndDirectories object
	 *
	 *@param  nnumberOfGaussiansPerMixture  Description of Parameter
	 *@param  niterationNumber              Description of Parameter
	 *@return                               The
	 *      DirectoryForSharedTriphoneEmbeddedTraining value
	 */
	public String getDirectoryForSharedTriphoneEmbeddedTraining(int nnumberOfGaussiansPerMixture,
			int niterationNumber) {
		return m_generalOutputDirectory + m_topologyIdentifier + "/triphones/shared/embedded_" + nnumberOfGaussiansPerMixture + "G_" + niterationNumber + "/";
	}


	/**
	 *  Gets the HMMConfigurationFileNameForIsolatedSegments attribute of the
	 *  SimulationFilesAndDirectories object
	 *
	 *@param  ntableEntry  Description of Parameter
	 *@return              The HMMConfigurationFileNameForIsolatedSegments value
	 */
	public String getHMMConfigurationFileNameForIsolatedSegments(int ntableEntry) {
		//use an HMM to get information about the configuration
		String thisHMMConfigurationFileName = null;
		if (m_defaultHMMConfigurationFileName == null) {
			//in this case it's mandatory to have a HCN for this table entry
			thisHMMConfigurationFileName = m_headerProperties.getPropertyAndExitIfKeyNotFound("TrainingManager.HMMConfiguration_" + ntableEntry);
		}
		else {
			//eventually use default configuration
			thisHMMConfigurationFileName = m_headerProperties.getProperty("TrainingManager.HMMConfiguration_" + ntableEntry,
					m_defaultHMMConfigurationFileName);
		}
		thisHMMConfigurationFileName = FileNamesAndDirectories.getAbsolutePath(thisHMMConfigurationFileName, m_generalOutputDirectory + m_topologyIdentifier + "/");
		return thisHMMConfigurationFileName;
	}


	/**
	 *  Description of the Method
	 */
	public void copyMostRecentSentencesMonophonesHMMsToFinalDirectory() {
		String source = m_directoryForMostRecentMonophoneHMMs;
		String target = m_sentencesMonophoneHMMsFinalDirectory;
		FileNamesAndDirectories.createDirectoriesIfNecessary(target);
		//IO.copyFile(source + "hmms.jar", target + "hmms.jar");
		IO.copyFile(source + m_setOfHMMsFileName, target + m_setOfHMMsFileName);
	}


	/**
	 *  Description of the Method
	 */
	public void copyMostRecentTriphonesHMMsToFinalDirectoryForPlainTriphones() {
		String source = m_directoryForMostRecentTriphoneHMMs;
		String target = m_sentencesPlainTriphoneHMMsFinalDirectory;
		FileNamesAndDirectories.createDirectoriesIfNecessary(target);
		//IO.copyFile(source + "hmms.jar", target + "hmms.jar");
		SetOfHMMs setOfHMMs = SetOfHMMsFile.read(source + m_setOfHMMsFileName);
		if (setOfHMMs instanceof SetOfSharedContinuousHMMs) {
			//convert it
			setOfHMMs = ( (SetOfSharedContinuousHMMs) setOfHMMs).convertToPlainHMMs();
		}
		SetOfPlainContinuousHMMs setOfPlainContinuousHMMs = (SetOfPlainContinuousHMMs) setOfHMMs;
		setOfPlainContinuousHMMs.writeToJARFile(target + m_setOfHMMsFileName);
		//IO.copyFile(source + m_setOfHMMsFileName, target + m_setOfHMMsFileName);
		String statsFileName = "OccupationStatistics.txt";
		IO.copyFile(source + statsFileName, target + statsFileName);
	}

	public void copyMostRecentTriphonesHMMsToFinalDirectoryForSharedTriphones() {
		String source = m_directoryForMostRecentTriphoneHMMs;
		String target = m_sentencesSharedTriphoneHMMsFinalDirectory;
		FileNamesAndDirectories.createDirectoriesIfNecessary(target);
		//IO.copyFile(source + "hmms.jar", target + "hmms.jar");
		IO.copyFile(source + m_setOfHMMsFileName, target + m_setOfHMMsFileName);
		String statsFileName = "OccupationStatistics.txt";
		IO.copyFile(source + statsFileName, target + statsFileName);
	}


	/**
	 *  Description of the Method
	 */
	public void copyMostRecentSharedTriphonesHMMsToFinalDirectory() {
		String source = m_directoryForMostRecentTriphoneHMMs;
		String target = m_sentencesSharedTriphoneHMMPrototypesDirectory;
		FileNamesAndDirectories.createDirectoriesIfNecessary(target);
		//IO.copyFile(source + "hmms.jar", target + "hmms.jar");
		IO.copyFile(source + m_setOfHMMsFileName, target + m_setOfHMMsFileName);
	}

	public void updateCurrentMonophoneHMMsDirectory(String newDirectory) {
			m_directoryForMostRecentMonophoneHMMs = newDirectory;
	}

	public void updateCurrentTriphoneHMMsDirectory(String newDirectory) {
			m_directoryForMostRecentTriphoneHMMs = newDirectory;
	}

	/**
	 *  Description of the Method
	 *
	 *@param  wavFileName                           Description of Parameter
	 *@param  directoryForSetOfPatternsOfSentences  Description of Parameter
	 *@param  oisTraining                           Description of Parameter
	 *@return                                       Description of the Returned
	 *      Value
	 */
	public String convertWAVToSOPFileName(String wavFileName, String directoryForSetOfPatternsOfSentences, boolean oisTraining) {
		String wavAbsolutePath = null;
		//to take out m_tXXXDataRootDirectory if necessary
		int nindex = -1;
		if (oisTraining) {
			wavAbsolutePath = FileNamesAndDirectories.getAbsolutePath(wavFileName, m_trainSpeechDataRootDirectory);
			nindex = m_trainSpeechDataRootDirectory.length();
		}
		else {
			wavAbsolutePath = FileNamesAndDirectories.getAbsolutePath(wavFileName, m_testSpeechDataRootDirectory);
			nindex = m_testSpeechDataRootDirectory.length();
		}
		wavAbsolutePath = FileNamesAndDirectories.replaceBackSlashByForward(wavAbsolutePath);
		String sopFileName = wavAbsolutePath.substring(nindex);
		sopFileName = FileNamesAndDirectories.deleteExtension(sopFileName);
		sopFileName = directoryForSetOfPatternsOfSentences +
				sopFileName + "." +
				SetOfPatterns.m_FILE_EXTENSION;
		return sopFileName;
	}


	/**
	 *  Description of the Method
	 */
	public void copyMostRecentIsolatedMonophonesHMMsToFinalDirectory() {
		String source = m_directoryForMostRecentMonophoneHMMs;
		String target = m_isolatedMonophoneHMMsFinalDirectory;
		FileNamesAndDirectories.createDirectoriesIfNecessary(target);
		//IO.copyFile(source + "hmms.jar", target + "hmms.jar");
		IO.copyFile(source + m_setOfHMMsFileName, target + m_setOfHMMsFileName);
	}

	private void organizeTranscriptions() {
		m_transcriptionFileExtension = m_headerProperties.getProperty("SimulationFilesAndDirectories.TranscriptionFileExtension",DataLocator.m_FILE_EXTENSION);

		m_isolatedMonophoneTranscriptionsOfTrainSpeechDataDirectory = m_generalOutputDirectoryWithoutFeatureDescription + "transcriptions/monophones/isolated/train/";
		FileNamesAndDirectories.createDirectoriesIfNecessary(m_isolatedMonophoneTranscriptionsOfTrainSpeechDataDirectory);

		m_isolatedMonophoneTranscriptionsOfTestSpeechDataDirectory = m_generalOutputDirectoryWithoutFeatureDescription +  "transcriptions/monophones/isolated/test/";
		FileNamesAndDirectories.createDirectoriesIfNecessary(m_isolatedMonophoneTranscriptionsOfTestSpeechDataDirectory);

		m_sentenceMonophoneTranscriptionsOfTrainSpeechDataFileName = m_generalDatabaseDirectory +  "transcriptions/train." + m_transcriptionFileExtension;
		FileNamesAndDirectories.createDirectoriesIfNecessary(FileNamesAndDirectories.getPathFromFileName(m_sentenceMonophoneTranscriptionsOfTrainSpeechDataFileName));

		m_sentenceMonophoneTranscriptionsOfTestSpeechDataFileName = m_generalDatabaseDirectory +  "transcriptions/test." + m_transcriptionFileExtension;
		FileNamesAndDirectories.createDirectoriesIfNecessary(FileNamesAndDirectories.getPathFromFileName(m_sentenceMonophoneTranscriptionsOfTestSpeechDataFileName));

		m_sentenceTriphoneTranscriptionsOfTrainSpeechDataFileName = m_generalOutputDirectoryWithoutFeatureDescription +  "transcriptions/triphones/sentences/train." + m_transcriptionFileExtension;
		FileNamesAndDirectories.createDirectoriesIfNecessary(FileNamesAndDirectories.getPathFromFileName(m_sentenceTriphoneTranscriptionsOfTrainSpeechDataFileName));

		m_sentenceTriphoneTranscriptionsOfTestSpeechDataFileName = m_generalOutputDirectoryWithoutFeatureDescription + "transcriptions/triphones/sentences/test." + m_transcriptionFileExtension;
		FileNamesAndDirectories.createDirectoriesIfNecessary(FileNamesAndDirectories.getPathFromFileName(m_sentenceTriphoneTranscriptionsOfTestSpeechDataFileName));
	}

	private void organizeTranscriptionsUnderParentOfSpeechDataDirectoryQUESIPASA() {
		m_isolatedMonophoneTranscriptionsOfTrainSpeechDataDirectory = FileNamesAndDirectories.getParent(m_trainSpeechDataRootDirectory);
		m_isolatedMonophoneTranscriptionsOfTrainSpeechDataDirectory = FileNamesAndDirectories.concatenateTwoPaths(m_isolatedMonophoneTranscriptionsOfTrainSpeechDataDirectory, "transcriptions/monophones/isolated/train/");
		FileNamesAndDirectories.createDirectoriesIfNecessary(m_isolatedMonophoneTranscriptionsOfTrainSpeechDataDirectory);

		m_isolatedMonophoneTranscriptionsOfTestSpeechDataDirectory = FileNamesAndDirectories.getParent(m_testSpeechDataRootDirectory);
		m_isolatedMonophoneTranscriptionsOfTestSpeechDataDirectory = FileNamesAndDirectories.concatenateTwoPaths(m_isolatedMonophoneTranscriptionsOfTestSpeechDataDirectory, "transcriptions/monophones/isolated/test/");
		FileNamesAndDirectories.createDirectoriesIfNecessary(m_isolatedMonophoneTranscriptionsOfTestSpeechDataDirectory);

		m_sentenceMonophoneTranscriptionsOfTrainSpeechDataFileName = FileNamesAndDirectories.getParent(m_trainSpeechDataRootDirectory);
		m_sentenceMonophoneTranscriptionsOfTrainSpeechDataFileName = FileNamesAndDirectories.concatenateTwoPaths(m_sentenceMonophoneTranscriptionsOfTrainSpeechDataFileName, "transcriptions/monophones/sentences/train." + m_transcriptionFileExtension);
		FileNamesAndDirectories.createDirectoriesIfNecessary(FileNamesAndDirectories.getPathFromFileName(m_sentenceMonophoneTranscriptionsOfTrainSpeechDataFileName));

		m_sentenceMonophoneTranscriptionsOfTestSpeechDataFileName = FileNamesAndDirectories.getParent(m_testSpeechDataRootDirectory);
		m_sentenceMonophoneTranscriptionsOfTestSpeechDataFileName = FileNamesAndDirectories.concatenateTwoPaths(m_sentenceMonophoneTranscriptionsOfTestSpeechDataFileName, "transcriptions/monophones/sentences/test." + m_transcriptionFileExtension);
		FileNamesAndDirectories.createDirectoriesIfNecessary(FileNamesAndDirectories.getPathFromFileName(m_sentenceMonophoneTranscriptionsOfTestSpeechDataFileName));

		m_sentenceTriphoneTranscriptionsOfTrainSpeechDataFileName = FileNamesAndDirectories.getParent(m_trainSpeechDataRootDirectory);
		m_sentenceTriphoneTranscriptionsOfTrainSpeechDataFileName = FileNamesAndDirectories.concatenateTwoPaths(m_sentenceTriphoneTranscriptionsOfTrainSpeechDataFileName, "transcriptions/triphones/sentences/train." + m_transcriptionFileExtension);
		FileNamesAndDirectories.createDirectoriesIfNecessary(FileNamesAndDirectories.getPathFromFileName(m_sentenceTriphoneTranscriptionsOfTrainSpeechDataFileName));

		m_sentenceTriphoneTranscriptionsOfTestSpeechDataFileName = FileNamesAndDirectories.getParent(m_testSpeechDataRootDirectory);
		m_sentenceTriphoneTranscriptionsOfTestSpeechDataFileName = FileNamesAndDirectories.concatenateTwoPaths(m_sentenceTriphoneTranscriptionsOfTestSpeechDataFileName, "transcriptions/triphones/sentences/test." + m_transcriptionFileExtension);
		FileNamesAndDirectories.createDirectoriesIfNecessary(FileNamesAndDirectories.getPathFromFileName(m_sentenceTriphoneTranscriptionsOfTestSpeechDataFileName));
	}

	private void organizeSpeechWaveforms() {
		m_trainSpeechDataRootDirectory = m_headerProperties.getPropertyAndExitIfKeyNotFound("TrainingManager.TrainSpeechDataRootDirectory");
		m_trainSpeechDataRootDirectory = FileNamesAndDirectories.replaceAndForceEndingWithSlash(m_trainSpeechDataRootDirectory);
		//Print.warning("m_trainSpeechDataRootDirectory = " + m_trainSpeechDataRootDirectory);
		m_testSpeechDataRootDirectory = m_headerProperties.getProperty("TrainingManager.TestSpeechDataRootDirectory");
		if (m_testSpeechDataRootDirectory == null || m_testSpeechDataRootDirectory.trim().equals("")) {
		//if (m_testSpeechDataRootDirectory == null) {
			//try to change Train by Test or train by test
			m_testSpeechDataRootDirectory = FileNamesAndDirectories.replacePartOfString(m_trainSpeechDataRootDirectory, "rain", "est");
			if (m_testSpeechDataRootDirectory == null || !(new File(m_testSpeechDataRootDirectory).exists())) {
				//there is no substring "rain" or directory does not exist...
				//Print.warning("Could not find a value for property, TrainingManager.TestSpeechDataRootDirectory" +
				//", so testing will use the same directory as training: " + m_trainSpeechDataRootDirectory + ".");
				m_testSpeechDataRootDirectory = m_trainSpeechDataRootDirectory;
			}
		}
		m_testSpeechDataRootDirectory = FileNamesAndDirectories.replaceAndForceEndingWithSlash(m_testSpeechDataRootDirectory);
	}

	private void organizeParameters() {
//		m_sentenceTrainParametersDataRootDirectory = m_generalOutputDirectory + "data/sentences/train/";
//		FileNamesAndDirectories.createDirectoriesIfNecessary(m_sentenceTrainParametersDataRootDirectory);
//		m_sentenceTestParametersDataRootDirectory = m_generalOutputDirectory + "data/sentences/test/";
//		FileNamesAndDirectories.createDirectoriesIfNecessary(m_sentenceTestParametersDataRootDirectory);
//
//		m_isolatedTrainParametersDataRootDirectory = m_generalOutputDirectory + "data/isolated/train/";
//		FileNamesAndDirectories.createDirectoriesIfNecessary(m_isolatedTrainParametersDataRootDirectory);
//		m_isolatedTestParametersDataRootDirectory = m_generalOutputDirectory + "data/isolated/test/";
//		FileNamesAndDirectories.createDirectoriesIfNecessary(m_isolatedTestParametersDataRootDirectory);

		m_sentenceTrainParametersDataRootDirectory = m_generalDatabaseDirectory + "features/" + m_featuresDescription + "/train/";
		FileNamesAndDirectories.createDirectoriesIfNecessary(m_sentenceTrainParametersDataRootDirectory);
		m_sentenceTestParametersDataRootDirectory = m_generalDatabaseDirectory + "features/" + m_featuresDescription + "/test/";
		FileNamesAndDirectories.createDirectoriesIfNecessary(m_sentenceTestParametersDataRootDirectory);

		m_isolatedTrainParametersDataRootDirectory = m_generalOutputDirectory + "features/train/";
		FileNamesAndDirectories.createDirectoriesIfNecessary(m_isolatedTrainParametersDataRootDirectory);
		m_isolatedTestParametersDataRootDirectory = m_generalOutputDirectory + "features/test/";
		FileNamesAndDirectories.createDirectoriesIfNecessary(m_isolatedTestParametersDataRootDirectory);
	}

	private void organizeHMMs() {
		//isolated segments monophones
		m_isolatedMonophoneHMMPrototypesDirectory = m_generalOutputDirectory + m_topologyIdentifier + "/monophones/isolated/prototypes/";
		FileNamesAndDirectories.createDirectoriesIfNecessary(m_isolatedMonophoneHMMPrototypesDirectory);
		m_isolatedMonophoneHMMsKMeansViterbiDirectory = m_generalOutputDirectory + m_topologyIdentifier + "/monophones/isolated/kmeansviterbi/";
		FileNamesAndDirectories.createDirectoriesIfNecessary(m_isolatedMonophoneHMMsKMeansViterbiDirectory);
		m_isolatedMonophoneHMMsBaumWelchDirectory = m_generalOutputDirectory + m_topologyIdentifier + "/monophones/isolated/baumwelch/";
		FileNamesAndDirectories.createDirectoriesIfNecessary(m_isolatedMonophoneHMMsBaumWelchDirectory);

		//sentences monophones
		m_isolatedMonophoneHMMsFinalDirectory = m_generalOutputDirectory + m_topologyIdentifier + "/monophones/isolated/finalmodels/";
		FileNamesAndDirectories.createDirectoriesIfNecessary(m_isolatedMonophoneHMMsFinalDirectory);
		m_sentencesMonophoneHMMsFinalDirectory = m_generalOutputDirectory + m_topologyIdentifier + "/monophones/sentences/finalmodels/";
		FileNamesAndDirectories.createDirectoriesIfNecessary(m_sentencesMonophoneHMMsFinalDirectory);

		//sentences plain triphones
		m_sentencesPlainTriphoneHMMPrototypesDirectory = m_generalOutputDirectory + m_topologyIdentifier + "/triphones/sentences/plain/prototypes/";
		FileNamesAndDirectories.createDirectoriesIfNecessary(m_sentencesPlainTriphoneHMMPrototypesDirectory);
		m_sentencesPlainTriphoneHMMsFinalDirectory = m_generalOutputDirectory + m_topologyIdentifier + "/triphones/sentences/plain/finalmodels/";
		FileNamesAndDirectories.createDirectoriesIfNecessary(m_sentencesPlainTriphoneHMMsFinalDirectory);

		//sentences shared triphones
		m_sentencesSharedTriphoneHMMsFinalDirectory = m_generalOutputDirectory + m_topologyIdentifier + "/triphones/sentences/shared/finalmodels/";
		FileNamesAndDirectories.createDirectoriesIfNecessary(m_sentencesSharedTriphoneHMMsFinalDirectory);
		m_sentencesSharedTriphoneHMMPrototypesDirectory = m_generalOutputDirectory + m_topologyIdentifier + "/triphones/sentences/shared/prototypes/";
		FileNamesAndDirectories.createDirectoriesIfNecessary(m_sentencesSharedTriphoneHMMPrototypesDirectory);

		//files
		m_mappingOfLogicalIntoPhysicalMonophonesFileName = m_generalOutputDirectory + m_topologyIdentifier + "/monophones/MonophonesDictionary.txt";
		m_mappingOfLogicalIntoPhysicalTriphonesFileName = m_generalOutputDirectory + m_topologyIdentifier + "/triphones/MappingOfLogicalIntoPhysicalTriphones.txt";
	}


	/**
	 *  Before training procedure starts, check if all necessary information is
	 *  available. Want to avoid user leaving the simulation running and it stops
	 *  after some steps due to lack of information.
	 */
	 //make all directories ending with slash "/"
	private void interpretHeaderAndInitialize() {
		String databaseType = m_headerProperties.getPropertyAndExitIfKeyNotFound("Database.Type");
		if (Database.Type.getType(databaseType) == Database.Type.GENERAL) {
			//do a different / simpler organization of directories
			organizeDirectoriesForFileOrientedTraining();
			return;
		}

		m_generalOutputDirectoryWithoutFeatureDescription = m_headerProperties.getPropertyAndExitIfKeyNotFound("TrainingManager.GeneralOutputDirectory");
		m_generalOutputDirectoryWithoutFeatureDescription = FileNamesAndDirectories.replaceAndForceEndingWithSlash(m_generalOutputDirectoryWithoutFeatureDescription);
		m_generalOutputDirectory = m_generalOutputDirectoryWithoutFeatureDescription + m_featuresDescription + "/";
		m_generalOutputDirectory = FileNamesAndDirectories.replaceAndForceEndingWithSlash(m_generalOutputDirectory);
		FileNamesAndDirectories.createDirectoriesIfNecessary(m_generalOutputDirectory);
		FileNamesAndDirectories.createDirectoriesIfNecessary(m_generalOutputDirectoryWithoutFeatureDescription);

		//try to get a HMM configuration file
		//notice that if it's null, it is assumed there will exist
		//a HMM configuration file for each table entry
		m_defaultHMMConfigurationFileName = m_headerProperties.getProperty("TrainingManager.DefaultHMMConfigurationFileName");
		//get topology identifier. firstly try to get it from current properties
		m_topologyIdentifier = HMMConfiguration.getTopologyDescription(m_headerProperties);
		if (m_topologyIdentifier == null) {
			//then try getting it from a HCN file
			String fileWithTopologyIdentifier = null;
			if (m_defaultHMMConfigurationFileName != null) {
				m_headerProperties.setProperty("TrainingManager.DefaultHMMConfigurationFileName", m_defaultHMMConfigurationFileName);
				fileWithTopologyIdentifier = m_defaultHMMConfigurationFileName;
			} else {
				//then use the configuration of first entry # 0
				fileWithTopologyIdentifier = getHMMConfigurationFileNameForIsolatedSegments(0);
			}
			m_topologyIdentifier = HMMConfiguration.getTopologyDescription(fileWithTopologyIdentifier);
		}

		organizeSpeechWaveforms();

		//go two directories above, assuming .\waveform\train\
		//m_generalDatabaseDirectory = FileNamesAndDirectories.getParent(m_trainSpeechDataRootDirectory);
		//m_generalDatabaseDirectory = FileNamesAndDirectories.getParent(m_generalDatabaseDirectory);
		//m_generalDatabaseDirectory = FileNamesAndDirectories.replaceAndForceEndingWithSlash(m_generalDatabaseDirectory);

		String temp = FileNamesAndDirectories.getParent(m_generalOutputDirectoryWithoutFeatureDescription);
		m_generalDatabaseDirectory = m_headerProperties.getProperty("TrainingManager.GeneralDatabaseDirectory",temp);
		if (m_generalDatabaseDirectory == null) {
			End.throwError("Property TrainingManager.GeneralDatabaseDirectory was not specified and in this case directory " +
			m_generalOutputDirectoryWithoutFeatureDescription + " should have a valid parent");
		}
		m_generalDatabaseDirectory = FileNamesAndDirectories.replaceAndForceEndingWithSlash(m_generalDatabaseDirectory);

		organizeTranscriptions();
		organizeParameters();
		organizeHMMs();
	}

	private void organizeDirectoriesForFileOrientedTraining() {
		m_generalOutputDirectoryWithoutFeatureDescription = m_headerProperties.getPropertyAndExitIfKeyNotFound("TrainingManager.GeneralOutputDirectory");
		m_generalOutputDirectoryWithoutFeatureDescription = FileNamesAndDirectories.replaceAndForceEndingWithSlash(m_generalOutputDirectoryWithoutFeatureDescription);
		m_generalOutputDirectory = m_generalOutputDirectoryWithoutFeatureDescription + m_featuresDescription + "/";
		m_generalOutputDirectory = FileNamesAndDirectories.replaceAndForceEndingWithSlash(m_generalOutputDirectory);
		FileNamesAndDirectories.createDirectoriesIfNecessary(m_generalOutputDirectory);
		FileNamesAndDirectories.createDirectoriesIfNecessary(m_generalOutputDirectoryWithoutFeatureDescription);

		//try to get a HMM configuration file
		//notice that if it's null, it is assumed there will exist
		//a HMM configuration file for each table entry
		m_defaultHMMConfigurationFileName = m_headerProperties.getProperty("TrainingManager.DefaultHMMConfigurationFileName");
		//get topology identifier. firstly try to get it from current properties
		m_topologyIdentifier = HMMConfiguration.getTopologyDescription(m_headerProperties);
		if (m_topologyIdentifier == null) {
			//then try getting it from a HCN file
			String fileWithTopologyIdentifier = null;
			if (m_defaultHMMConfigurationFileName != null) {
				m_headerProperties.setProperty("TrainingManager.DefaultHMMConfigurationFileName", m_defaultHMMConfigurationFileName);
				fileWithTopologyIdentifier = m_defaultHMMConfigurationFileName;
			} else {
				//then use the configuration of first entry # 0
				fileWithTopologyIdentifier = getHMMConfigurationFileNameForIsolatedSegments(0);
			}
			m_topologyIdentifier = HMMConfiguration.getTopologyDescription(fileWithTopologyIdentifier);
		}

	//private void organizeSpeechWaveforms() {
		m_trainSpeechDataRootDirectory = m_headerProperties.getPropertyAndExitIfKeyNotFound("TrainingManager.TrainSpeechDataRootDirectory");
		m_trainSpeechDataRootDirectory = FileNamesAndDirectories.replaceAndForceEndingWithSlash(m_trainSpeechDataRootDirectory);
		//Print.warning("m_trainSpeechDataRootDirectory = " + m_trainSpeechDataRootDirectory);
		m_testSpeechDataRootDirectory = m_headerProperties.getProperty("TrainingManager.TestSpeechDataRootDirectory");
		if (m_testSpeechDataRootDirectory == null || m_testSpeechDataRootDirectory.trim().equals("")) {
		//if (m_testSpeechDataRootDirectory == null) {
			//try to change Train by Test or train by test
			m_testSpeechDataRootDirectory = FileNamesAndDirectories.replacePartOfString(m_trainSpeechDataRootDirectory, "rain", "est");
			if (m_testSpeechDataRootDirectory == null || !(new File(m_testSpeechDataRootDirectory).exists())) {
				//there is no substring "rain" or directory does not exist...
				//Print.warning("Could not find a value for property, TrainingManager.TestSpeechDataRootDirectory" +
				//", so testing will use the same directory as training: " + m_trainSpeechDataRootDirectory + ".");
				m_testSpeechDataRootDirectory = m_trainSpeechDataRootDirectory;
			}
		}
		m_testSpeechDataRootDirectory = FileNamesAndDirectories.replaceAndForceEndingWithSlash(m_testSpeechDataRootDirectory);

		String temp = FileNamesAndDirectories.getParent(m_generalOutputDirectoryWithoutFeatureDescription);
		m_generalDatabaseDirectory = m_headerProperties.getProperty("TrainingManager.GeneralDatabaseDirectory",temp);
		if (m_generalDatabaseDirectory == null) {
			End.throwError("Property TrainingManager.GeneralDatabaseDirectory was not specified and in this case directory " +
			m_generalOutputDirectoryWithoutFeatureDescription + " should have a valid parent");
		}
		m_generalDatabaseDirectory = FileNamesAndDirectories.replaceAndForceEndingWithSlash(m_generalDatabaseDirectory);

		//transcriptions
		m_transcriptionFileExtension = m_headerProperties.getProperty("SimulationFilesAndDirectories.TranscriptionFileExtension",DataLocator.m_FILE_EXTENSION);

		m_isolatedMonophoneTranscriptionsOfTrainSpeechDataDirectory = m_generalOutputDirectoryWithoutFeatureDescription + "transcriptions/train/";
		FileNamesAndDirectories.createDirectoriesIfNecessary(m_isolatedMonophoneTranscriptionsOfTrainSpeechDataDirectory);

		m_isolatedMonophoneTranscriptionsOfTestSpeechDataDirectory = m_generalOutputDirectoryWithoutFeatureDescription +  "transcriptions/test/";
		FileNamesAndDirectories.createDirectoriesIfNecessary(m_isolatedMonophoneTranscriptionsOfTestSpeechDataDirectory);

		//m_sentenceMonophoneTranscriptionsOfTrainSpeechDataFileName = null;//m_generalDatabaseDirectory +  "transcriptions/train." + m_transcriptionFileExtension;
		//FileNamesAndDirectories.createDirectoriesIfNecessary(FileNamesAndDirectories.getPathFromFileName(m_sentenceMonophoneTranscriptionsOfTrainSpeechDataFileName));

		//m_sentenceMonophoneTranscriptionsOfTestSpeechDataFileName = null; //m_generalDatabaseDirectory +  "transcriptions/test." + m_transcriptionFileExtension;
		//FileNamesAndDirectories.createDirectoriesIfNecessary(FileNamesAndDirectories.getPathFromFileName(m_sentenceMonophoneTranscriptionsOfTestSpeechDataFileName));

		//m_sentenceTriphoneTranscriptionsOfTrainSpeechDataFileName = m_generalOutputDirectoryWithoutFeatureDescription +  "transcriptions/triphones/sentences/train." + m_transcriptionFileExtension;
		//FileNamesAndDirectories.createDirectoriesIfNecessary(FileNamesAndDirectories.getPathFromFileName(m_sentenceTriphoneTranscriptionsOfTrainSpeechDataFileName));

		//m_sentenceTriphoneTranscriptionsOfTestSpeechDataFileName = m_generalOutputDirectoryWithoutFeatureDescription + "transcriptions/triphones/sentences/test." + m_transcriptionFileExtension;
		//FileNamesAndDirectories.createDirectoriesIfNecessary(FileNamesAndDirectories.getPathFromFileName(m_sentenceTriphoneTranscriptionsOfTestSpeechDataFileName));


	//private void organizeParameters() {
//		m_sentenceTrainParametersDataRootDirectory = m_generalOutputDirectory + "data/sentences/train/";
//		FileNamesAndDirectories.createDirectoriesIfNecessary(m_sentenceTrainParametersDataRootDirectory);
//		m_sentenceTestParametersDataRootDirectory = m_generalOutputDirectory + "data/sentences/test/";
//		FileNamesAndDirectories.createDirectoriesIfNecessary(m_sentenceTestParametersDataRootDirectory);
//
//		m_isolatedTrainParametersDataRootDirectory = m_generalOutputDirectory + "data/isolated/train/";
//		FileNamesAndDirectories.createDirectoriesIfNecessary(m_isolatedTrainParametersDataRootDirectory);
//		m_isolatedTestParametersDataRootDirectory = m_generalOutputDirectory + "data/isolated/test/";
//		FileNamesAndDirectories.createDirectoriesIfNecessary(m_isolatedTestParametersDataRootDirectory);

		//m_sentenceTrainParametersDataRootDirectory = m_generalDatabaseDirectory + "features/" + m_featuresDescription + "/train/";
		//FileNamesAndDirectories.createDirectoriesIfNecessary(m_sentenceTrainParametersDataRootDirectory);
		//m_sentenceTestParametersDataRootDirectory = m_generalDatabaseDirectory + "features/" + m_featuresDescription + "/test/";
		//FileNamesAndDirectories.createDirectoriesIfNecessary(m_sentenceTestParametersDataRootDirectory);

		m_isolatedTrainParametersDataRootDirectory = m_generalOutputDirectory + "features/train/";
		FileNamesAndDirectories.createDirectoriesIfNecessary(m_isolatedTrainParametersDataRootDirectory);
		m_isolatedTestParametersDataRootDirectory = m_generalOutputDirectory + "features/test/";
		FileNamesAndDirectories.createDirectoriesIfNecessary(m_isolatedTestParametersDataRootDirectory);


	//private void organizeHMMs() {
		//isolated segments monophones
		m_isolatedMonophoneHMMPrototypesDirectory = m_generalOutputDirectory + "prototypes/";
		FileNamesAndDirectories.createDirectoriesIfNecessary(m_isolatedMonophoneHMMPrototypesDirectory);
		//ak i am not enabling k-means for file oriented...
		//m_isolatedMonophoneHMMsKMeansViterbiDirectory = m_generalOutputDirectory + "kmeansviterbi/";
		//FileNamesAndDirectories.createDirectoriesIfNecessary(m_isolatedMonophoneHMMsKMeansViterbiDirectory);
		m_isolatedMonophoneHMMsBaumWelchDirectory = m_generalOutputDirectory + "hmms/";
		FileNamesAndDirectories.createDirectoriesIfNecessary(m_isolatedMonophoneHMMsBaumWelchDirectory);

		//sentences monophones
		m_isolatedMonophoneHMMsFinalDirectory = m_generalOutputDirectory + "finalmodels/";
		//FileNamesAndDirectories.createDirectoriesIfNecessary(m_isolatedMonophoneHMMsFinalDirectory);
		//m_sentencesMonophoneHMMsFinalDirectory = m_generalOutputDirectory + m_topologyIdentifier + "/monophones/sentences/finalmodels/";
		//FileNamesAndDirectories.createDirectoriesIfNecessary(m_sentencesMonophoneHMMsFinalDirectory);

		//sentences plain triphones
		//m_sentencesPlainTriphoneHMMPrototypesDirectory = m_generalOutputDirectory + m_topologyIdentifier + "/triphones/sentences/plain/prototypes/";
		//FileNamesAndDirectories.createDirectoriesIfNecessary(m_sentencesPlainTriphoneHMMPrototypesDirectory);
		//m_sentencesPlainTriphoneHMMsFinalDirectory = m_generalOutputDirectory + m_topologyIdentifier + "/triphones/sentences/plain/finalmodels/";
		//FileNamesAndDirectories.createDirectoriesIfNecessary(m_sentencesPlainTriphoneHMMsFinalDirectory);

		//sentences shared triphones
		//m_sentencesSharedTriphoneHMMsFinalDirectory = m_generalOutputDirectory + m_topologyIdentifier + "/triphones/sentences/shared/finalmodels/";
		//FileNamesAndDirectories.createDirectoriesIfNecessary(m_sentencesSharedTriphoneHMMsFinalDirectory);
		//m_sentencesSharedTriphoneHMMPrototypesDirectory = m_generalOutputDirectory + m_topologyIdentifier + "/triphones/sentences/shared/prototypes/";
		//FileNamesAndDirectories.createDirectoriesIfNecessary(m_sentencesSharedTriphoneHMMPrototypesDirectory);

		//files
		//m_mappingOfLogicalIntoPhysicalMonophonesFileName = m_generalOutputDirectory + m_topologyIdentifier + "/monophones/MonophonesDictionary.txt";
		//m_mappingOfLogicalIntoPhysicalTriphonesFileName = m_generalOutputDirectory + m_topologyIdentifier + "/triphones/MappingOfLogicalIntoPhysicalTriphones.txt";
	}

	public static CMPropertiesSet getDefaultProperties() {
		CMProperty[] properties  = new CMProperty[45];

		properties[0] = new PathStringCMProperty("TrainingManager.TrainSpeechDataRootDirectory",
		"Input directory", "Root directory where speech data is stored", null,
		null, true, null);

		properties[1] = new PathStringCMProperty("TrainingManager.GeneralOutputDirectory",
		"Output directory", "Root directory where files (HMM's, etc.) will be stored", null,
		null, true, null);

		properties[2] = new PathStringCMProperty("PatternGenerator.FileName",
		"Front end configuration file", "Parameters used in front end", null,
		PatternGenerator.m_FILE_EXTENSION, false, "front end configuration");

		properties[3] = new PathStringCMProperty("TrainingManager.DefaultHMMConfigurationFileName",
		"HMM configuration file", "Specifies number of states, HMM topology, etc.", null,
		HMMConfiguration.m_FILE_EXTENSION, false, "HMM configuration");

		properties[4] = new BooleanCMProperty("TrainingManager.oshouldCreateTranscriptionsForSpeechFiles",
		"Generate transcriptions?", "If true, create transcriptions that is a required step before running the front end", new Boolean(true));

		properties[5] = new BooleanCMProperty("TrainingManager.oshouldConvertSpeechDataIntoParameters",
		"Calculate front end parameters?", "If true, convert speech data into features", new Boolean(true));

		properties[6] = new BooleanCMProperty("TrainingManager.oshouldCreateHMMFiles",
		"Estimate HMMs?", "If true, estimate HMM's", new Boolean(true));

		properties[7] = new BooleanCMProperty("TrainingManager.oshouldRunRecognitionForTestingData",
		"Test using testing data", "Test using new (different from training) data", new Boolean(true));

		properties[8] = new BooleanCMProperty("TrainingManager.oshouldRunRecognitionForTrainingData",
		"Test using training data", "Test using the same data used for training the models", new Boolean(false));

		properties[9] = new IntegerCMProperty("TrainingManager.nmaximumNumberOfGaussiansPerMixture",
		"Maximum Gaussians per mixture", "System can start with 1 Gaus./mix. and increase the # of Gaus. up to this maximum", new Integer(5));

		properties[10] = new BooleanCMProperty("TrainingManager.oshouldSkipReestimationIfFileExists",
		"Skip files if they exist?", "If true, during training, skip files that already exist unless " +
		"they are invalid HMMs (do not obey stochastic constraints)", new Boolean(false));

//TrainingManager.OutputTSTFileName=d:/fileoriented/mfccedc26w256s80/classification.TST
		properties[11] = new PathStringCMProperty("TrainingManager.TestSpeechDataRootDirectory",
		"Input directory for test data", "Where test speech data is stored (if not specified, system will try to guess)", null,
		null, true, null);

		properties[12] = new FloatCMProperty("ContinuousHMMReestimator.fconvergenceThreshold",
		"Convergence threshold", "Used as stopping criterion in HMM reestimation", new Float(1e-4F));

		properties[13] = new FloatCMProperty("ContinuousHMMReestimator.fcovarianceFloor",
		"Covariance floor", "Minimum value for elements in main diagonal of covariance matrices", new Float(1e-4F));

		properties[14] = new FloatCMProperty("ContinuousHMMReestimator.fmixtureWeightFloor",
		"Weight floor", "Minimum value for mixture weights", new Float(0F));

		properties[15] = new IntegerCMProperty("ContinuousHMMReestimator.nmaximumIterations",
		"Maximum # of iterations", "Maximum number of iterations in HMM reestimation", new Integer(20));

		properties[16] = new IntegerCMProperty("ContinuousHMMReestimator.nminimumNumberOfFramesForValidPattern",
		"Minimum # of frames when training HMMs", "If the number of frames in a given example is below this value, the example is " +
		"discarded during HMM reestimation", new Integer(1));

		properties[17] = new IntegerCMProperty("ContinuousHMMReestimator.nminimumNumberOfPatternsPerModel",
		"Minimum # of examples", "Each HMM requires this minimum number of examples, otherwise an error is generated", new Integer(3));

		properties[18] = new BooleanCMProperty("ContinuousHMMReestimator.oshouldUpdateCovariance",
		"Update covariance matrices?", "If false, keep covariance matrices with original values", new Boolean(true));

		properties[19] = new BooleanCMProperty("ContinuousHMMReestimator.oshouldUpdateMean",
		"Update Gaussian means?", "If false, keep means with original values", new Boolean(true));

		properties[20] = new BooleanCMProperty("ContinuousHMMReestimator.oshouldUpdateTransitionMatrix",
		"Update transition matrices?", "If false, keep transition matrices with original values", new Boolean(true));

		properties[21] = new BooleanCMProperty("ContinuousHMMReestimator.oshouldUpdateWeights",
		"Update mixture weights?", "If false, keep mixture weights with original values", new Boolean(true));

		//fine control over patterns (MFCC, PLP,...) calculation
		properties[22] = new BooleanCMProperty("TrainingManager.oshouldConvertIsolatedSpeechDataIntoParameters",
		"Calculate features for isolated segments?", "If true calculates features (MFCC or PLP, etc)", new Boolean(true));

		properties[23] = new BooleanCMProperty("TrainingManager.oshouldCreatFileWithDynamicRanges",
		"Calculate file with dynamic ranges?", "If true writes a file with range of each feature", new Boolean(true));

		//fine control over HMM training
		properties[24] = new BooleanCMProperty("TrainingManager.oshouldCreateMonophoneHMMPrototypes",
		"Create HMM prototypes?", "If true, based on HMM configuration and training sequence write one prototype per HMM", new Boolean(true));

		properties[25] = new BooleanCMProperty("TrainingManager.oshouldCreateMonophoneHMMWithBaumWelchForIsolatedSegments",
		"Use Baum-Welch?", "Use Baum-Welch algorithm to reestimate HMM prototype", new Boolean(true));

		properties[26] = new BooleanCMProperty("TrainingManager.oshouldRecursivelyCreateMonophoneHMMWithBaumWelchForIsolatedSegments",
		"Use Baum-Welch recursively?", "If true, start with HMM prototype and increase number of Gaussians per mixture until maximum", new Boolean(true));

		properties[27] = new BooleanCMProperty("TrainingManager.oshouldRecursivelyCreateMonophoneHMMWithSegmentalKMeansForIsolatedSegments",
		"Use segmental K-means recursively?", "If true, start with HMM prototype and increase number of Gaussians per mixture until maximum", new Boolean(false));

		properties[28] = new StringCMProperty("OffLineIsolatedSegmentsClassifier.hMMSetFileName",
		"Test: HMM file name",
		"HMM set file name. Usually with extension zip",
		SetOfHMMsFile.m_name);

		properties[29] = new BooleanCMProperty("OffLineIsolatedSegmentsClassifier.oshouldWriteLattices",
		"Test: Generate lattices",
		"Generate files with best paths (lattices) if true",
		new Boolean(true));

		properties[30] = new BooleanCMProperty("OffLineIsolatedSegmentsClassifier.oisTraining",
		"Test: Use training sequence for testing",
		"If true the results will be written in directory 'train', if false in 'test'",
		new Boolean(false));

		properties[31] = new IntegerCMProperty("OffLineIsolatedSegmentsClassifier.nminimumNumberOfFramesInValidPattern",
		"Test: Minimum # of frames",
		"Minimum number of frames for pattern to be valid during the testing stage",
		new Boolean(true), new Boolean(true), new Boolean(false),
		new Boolean(false), new Boolean(true),
		new Integer(1), new Integer(1), null);

		properties[32] = new PathStringCMProperty("OffLineIsolatedSegmentsClassifier.hmmsRootDirectory",
		"Test: HMM's root directory",
		"All HMM files in subdirectories below this root will be tested",
		null, null, true, null);
		CMUtilities.setCMPropertyBooleanFields(properties[43],true, true,
		false,false,true);

		properties[33] = new PathStringCMProperty("OffLineIsolatedSegmentsClassifier.parametersRootDirectory",
		"Test: Parameters directory",
		"Directory with the test parameters files (" + SetOfPatterns.m_FILE_EXTENSION + " files storing MFCC's or others)",
		null, null, true, null);
		CMUtilities.setCMPropertyBooleanFields(properties[33],true, true,
		false,false,true);

		properties[34] = new PathStringCMProperty("OffLineIsolatedSegmentsClassifier.transcriptionsRootDirectory",
		"Test: Transcriptions root directory",
		"Directory with DTL files. Not used if transcriptions not available",
		null, null, true, null);
		CMUtilities.setCMPropertyBooleanFields(properties[34],true, true,
		false,false,true);

		properties[35] = new PathStringCMProperty("OffLineIsolatedSegmentsClassifier.tableOfLabelsForScoring",
		"Test: Scoring table file name",
		"Table with labels used for scoring",
		null, TableOfLabels.m_FILE_EXTENSION, false, "files with table of labels");
		//want to demand user to change this parameter through another way
		CMUtilities.setCMPropertyBooleanFields(properties[35],true,false,
		false,false,true);

		properties[36] = new IntegerCMProperty("TrainingManager.nsegmentalKmeansInitialNumberOfGaussians",
		"Segmental K-means: initial # of Gaussians", "HMMs will be created starting from this number up to maximum # of Gaussians per mixture",
		new Boolean(true), new Boolean(true), new Boolean(false),
		new Boolean(false), new Boolean(true),
		new Integer(1), new Integer(1), null);

		properties[37] = new IntegerCMProperty("TrainingManager.nsegmentalKmeansGaussiansIncrement",
		"Segmental K-means: increment in # of Gaussians", "If equal to 5 and initial # of Gaussians is 1, create HMMs with 1, 6, 11,... up to the specified maximum",
		new Boolean(true), new Boolean(true), new Boolean(false),
		new Boolean(false), new Boolean(true),
		new Integer(1), new Integer(1), null);

		properties[38] = new DoubleCMProperty("TrainingManager.dsegmentalKmeansThresholdToStopIterations",
		"Segmental K-means: stopping criterion", "Stop iterations if likelihood improvement falls below this threshold", new Double(0.02));

		properties[39] = new BooleanCMProperty("TrainingManager.osegmentalKmeansUseBaumWelchAfterwards",
		"Segmental K-means: use Baum-Welch", "If true, use Baum-Welch to reestimate models after segmental K-means", new Boolean(true));

		properties[40] = new IntegerCMProperty("TrainingManager.ThreadPriority",
		"Thread priority", "The bigger this number, more CPU cycles will be allocated to the simulation (and the other programs would respond slowly)",
		new Boolean(true), new Boolean(true), new Boolean(false),
		new Boolean(false), new Boolean(true), new Integer(Thread.NORM_PRIORITY),
		new Integer(Thread.MIN_PRIORITY), new Integer(Thread.MAX_PRIORITY));

		properties[41] = new BooleanCMProperty("ConvertALIENFrontEndToSOPFiles.oareBigEndian",
		"ALIEN files are big-endian?", "When converting ALIEN to " + SetOfPatterns.m_FILE_EXTENSION + " files, if true assume float numbers in big-endian (Sun, Vax, etc.), if false little-endian (PC)", new Boolean(true));

		properties[42] = new PathStringCMProperty("ConvertALIENFrontEndToSOPFiles.inputDirectory",
		"Directory with training sequence of ALIEN front end",
		"Used when converting ALIEN front end files. The directory with the test sequence must be ../test",
		null, null, true, null);

		properties[43] = new StringCMProperty("ConvertALIENFrontEndToSOPFiles.ExtensionForAlienFiles",
		"Extension for alien front ends",
		"If not specified system will try to convert all files below given directory, otherwise only with given extension",
		"ALI");

		properties[44] = new IntegerCMProperty("TrainingManager.nverbose",
		"Level of verbosity", "If 0, no messages, if 1 print basic messages, if 2 more information and so on", new Integer(1));

		//Did not include the following properties yet:

//TrainingManager.IsolatedTestParametersDirectory=d:/fileoriented/mfccedc26w256s80/features/test/
//TrainingManager.IsolatedTrainParametersDirectory=d:/fileoriented/mfccedc26w256s80/features/train/
//TrainingManager.oshouldCreateTriphoneModels=true
//TrainingManager.oshouldRunRecognitionForTestingData=true
//TrainingManager.oshouldRunRecognitionForTrainingData=false
//protected boolean m_oshouldCreateTriphoneTranscriptions = false;

//	protected boolean m_oshouldCloneMonophonesAndReestimatePlainTriphoneHMHs = false;
//	protected boolean m_oshouldClusterPlainTriphones = false;
//	protected boolean m_oshouldCreateCrossWordTriphoneHMMWithEmbeddedBaumWelch = false;
//	protected boolean m_oshouldRecursivelyCreateTriphoneHMMWithEmbeddedBaumWelch = false;

		//set default values
		//CMUtilities.setDefaultCMPropertyBooleanFields(properties);

		CMPropertiesSet cMPropertiesSet = new CMPropertiesSet(properties);
		cMPropertiesSet.composeProperties(getDefaultPropertiesIncludingOnesForTIMIT());
		return cMPropertiesSet;
	}

	public static CMPropertiesSet getDefaultPropertiesIncludingOnesForTIMIT() {
		CMProperty[] properties  = new CMProperty[16];
		boolean odisplay = false;
		boolean oallowUpdate = false;

		properties[0] = new ComboBoxCMProperty("Database.Type",
		"Database type", "'GENERAL' means any database organized in suddirectories",
		Database.Type.m_types);
		CMUtilities.setCMPropertyBooleanFields(properties[0], odisplay, oallowUpdate, false,
		false, true);

		properties[1] = new ComboBoxCMProperty("TrainingManager.Type",
		"Training type", "Isolated-word systems use 'ISOLATED_SEGMENT' type",
		TrainingManager.Type.m_types);
		CMUtilities.setCMPropertyBooleanFields(properties[1], odisplay, oallowUpdate, false,
		false, true);

		properties[2] = new PathStringCMProperty("TableOfLabels.FileName",
		"File with labels", "Name of file with table of labels", null,
		TableOfLabels.m_FILE_EXTENSION, false, "table of labels");
		CMUtilities.setCMPropertyBooleanFields(properties[2], odisplay, oallowUpdate, false,
		false, true);

		properties[3] = new ComboBoxCMProperty("TableOfLabels.Type",
		"Table with labels", "Standard tables used for TIMIT database",
		TableOfLabels.Type.m_types);
		CMUtilities.setCMPropertyBooleanFields(properties[3], odisplay, oallowUpdate, false,
		false, true);

		properties[4] = new BooleanCMProperty("TrainingManager.oshouldWriteReportFile",
		"Write output properties file?", "Output file written as Java Properties containing the configuration information", new Boolean(false));
		CMUtilities.setCMPropertyBooleanFields(properties[4], odisplay, oallowUpdate, false,
		false, true);

		properties[5] = new BooleanCMProperty("TrainingManager.ouseAbsolutePath",
		"Use absolute paths?", "Write absolute paths in transcription (files are bigger)", new Boolean(false));
		CMUtilities.setCMPropertyBooleanFields(properties[5], odisplay, oallowUpdate, false,
		false, true);

		//fine control over patterns (MFCC, PLP,...) calculation
		properties[6] = new BooleanCMProperty("TrainingManager.oshouldConvertSentenceSpeechDataIntoParameters",
		"Calculate features for each sentence?", "If the database is GENERAL this option does not apply", new Boolean(true));
		CMUtilities.setCMPropertyBooleanFields(properties[6], odisplay, oallowUpdate, false,
		false, true);

		//fine control over HMM training
		properties[7] = new BooleanCMProperty("TrainingManager.oshouldCreateMonophoneHMMWithEmbeddedBaumWelch",
		"Use embedded Baum-Welch?", "If true, use embedded Baum-Welch to reestimate HMM prototype", new Boolean(false));
		CMUtilities.setCMPropertyBooleanFields(properties[7], odisplay, oallowUpdate, false,
		false, true);

		properties[8] = new BooleanCMProperty("TrainingManager.oshouldRecursivelyCreateMonophoneHMMWithEmbeddedBaumWelch",
		"Use embedded Baum-Welch recursively?", "If true, start with HMM prototype and increase number of Gaussians per mixture until maximum", new Boolean(false));
		CMUtilities.setCMPropertyBooleanFields(properties[8], odisplay, oallowUpdate, false,
		false, true);

		properties[9] = new BooleanCMProperty("TrainingManager.oshouldCopyDatabaseToUniqueDirectory",
		"Reorganize TIMIT CD?", "If true, copy TIMIT database and give a unique name to files", new Boolean(false));
		CMUtilities.setCMPropertyBooleanFields(properties[9], odisplay, oallowUpdate, false,
		false, true);

		properties[10] = new BooleanCMProperty("OffLineIsolatedSegmentsClassifier.oareTranscriptionsAvailable",
		"Test: Transcriptions are available",
		"If true transcriptions are available and more detailed results can be obtained",
		new Boolean(true));
		CMUtilities.setCMPropertyBooleanFields(properties[10], odisplay, oallowUpdate, false,
		false, true);

		properties[11] = new IntegerCMProperty("TrainingManager.nembeddedBaumNumberOfIterations",
		"Number of iterations of embedded Baum-Welch", "If embedded Baum-Welch is used, this gives the number of reestimations using the algorithm per HMM",
		new Boolean(odisplay), new Boolean(oallowUpdate), new Boolean(false),
		new Boolean(false), new Boolean(true),
		new Integer(2), new Integer(1), null);

		properties[12] = new BooleanCMProperty("TrainingManager.oshouldRunClassification",
		"Run classification test?", "If true, test system performance", new Boolean(true));
		CMUtilities.setCMPropertyBooleanFields(properties[12], odisplay, oallowUpdate, false,
		false, true);

		properties[13] = new BooleanCMProperty("TrainingManager.oshouldGuessTestConfigurationAndOverwriteExistingValues",
		"Guess and overwrite test configuration?", "If false, the user must provide the information necessary for testing the system", new Boolean(true));
		CMUtilities.setCMPropertyBooleanFields(properties[13], odisplay, oallowUpdate, false,
		false, true);

		properties[14] = new PathStringCMProperty("TrainingManager.OriginalDirectoryOfTIMITTrainData",
		"Original TIMIT train data", "Original directory with TIMIT training data", null,
		null, true, null);
		CMUtilities.setCMPropertyBooleanFields(properties[14], odisplay, oallowUpdate, false,
		false, true);

		properties[15] = new PathStringCMProperty("TrainingManager.OriginalDirectoryOfTIMITTestData",
		"Original TIMIT test data", "Original directory with TIMIT testing data", null,
		null, true, null);
		CMUtilities.setCMPropertyBooleanFields(properties[15], odisplay, oallowUpdate, false,
		false, true);

		return new CMPropertiesSet(properties);
	}

	//public static CMPropertiesSet getDefaultSetOfProperties() {
	//	return new CMPropertiesSet(getDefaultProperties());
	//}
	public static void updateTestingConfiguration(HeaderProperties cMPropertiesSet) {

		String patternGeneratorFileName = cMPropertiesSet.getProperty("PatternGenerator.FileName");
		if (patternGeneratorFileName == null ||
		patternGeneratorFileName.equals("") ||
		!(new File(patternGeneratorFileName).exists())) {
			return;
		}

		patternGeneratorFileName = cMPropertiesSet.getProperty("TrainingManager.DefaultHMMConfigurationFileName");
		if (patternGeneratorFileName == null ||
		patternGeneratorFileName.equals("") ||
		!(new File(patternGeneratorFileName).exists())) {
			return;
		}

		patternGeneratorFileName = cMPropertiesSet.getProperty("TrainingManager.TrainSpeechDataRootDirectory");
		if (patternGeneratorFileName == null ||
		patternGeneratorFileName.equals("") ||
		!(new File(patternGeneratorFileName).exists())) {
			return;
		}

		patternGeneratorFileName = cMPropertiesSet.getProperty("TrainingManager.GeneralOutputDirectory");
		if (patternGeneratorFileName == null ||
		patternGeneratorFileName.equals("")) {
			return;
		}

		SimulationFilesAndDirectories m_simulationFilesAndDirectories = new SimulationFilesAndDirectories(cMPropertiesSet);

		String rootDirectoryWithHMMs = m_simulationFilesAndDirectories.getGeneralOutputDirectory();

		String trainingOrTest = cMPropertiesSet.getProperty("OffLineIsolatedSegmentsClassifier.oisTraining");
		boolean oisTraining = Boolean.valueOf(trainingOrTest).booleanValue();
		String directoryForDataLocators = null;
		if (oisTraining) {
			directoryForDataLocators = m_simulationFilesAndDirectories.getIsolatedMonophoneTranscriptionsOfTrainSpeechDataDirectory();
		}
		else {
			directoryForDataLocators = m_simulationFilesAndDirectories.getIsolatedMonophoneTranscriptionsOfTestSpeechDataDirectory();
		}
		String directoryForSetOfPatterns = null;
		if (oisTraining) {
			directoryForSetOfPatterns = m_simulationFilesAndDirectories.getIsolatedTrainParametersDataRootDirectory();
		}
		else {
			directoryForSetOfPatterns = m_simulationFilesAndDirectories.getIsolatedTestParametersDataRootDirectory();
		}
		boolean oshouldWriteLattices = true;

		//for TIMIT oareTranscriptionsAvailable = true;
		boolean oareTranscriptionsAvailable = true;
		String tableOfMonophoneLabelsForScoringFileName = null;
		Database.Type m_databaseType = Database.Type.getType(cMPropertiesSet.getPropertyAndExitIfKeyNotFound("Database.Type"));
		if (m_databaseType == Database.Type.TIMIT) {
			tableOfMonophoneLabelsForScoringFileName = rootDirectoryWithHMMs + TableOfLabels.Type.TIMIT39.toString() + "." + TableOfLabels.m_FILE_EXTENSION;
//			if (!new File(tableOfMonophoneLabelsForScoringFileName).exists()) {
//				TableOfLabels table = new TableOfLabels(TableOfLabels.Type.TIMIT39);
//				table.writeToFile(tableOfMonophoneLabelsForScoringFileName);
//			}
		} else if (m_databaseType == Database.Type.TIDIGITS) {
			tableOfMonophoneLabelsForScoringFileName = rootDirectoryWithHMMs + TableOfLabels.Type.TIDIGITS.toString() + "." + TableOfLabels.m_FILE_EXTENSION;
		} else if (m_databaseType == Database.Type.GENERAL) {
			tableOfMonophoneLabelsForScoringFileName = rootDirectoryWithHMMs + "tableForScoring." + TableOfLabels.m_FILE_EXTENSION;
//			if (!new File(tableOfMonophoneLabelsForScoringFileName).exists()) {
//				TableOfLabels tableOfLabels = getTableOfLabels(m_databaseType, m_simulationFilesAndDirectories);
//				tableOfLabels.writeToFile(tableOfMonophoneLabelsForScoringFileName);
//			}
			oareTranscriptionsAvailable = false;
                } else if (m_databaseType == Database.Type.SPOLTECHRAW) {
                  tableOfMonophoneLabelsForScoringFileName = rootDirectoryWithHMMs + TableOfLabels.Type.SPOLTECH64.toString() + "." + TableOfLabels.m_FILE_EXTENSION;
                } else if (m_databaseType == Database.Type.DECTALK) {
                    tableOfMonophoneLabelsForScoringFileName = rootDirectoryWithHMMs + TableOfLabels.Type.DECTALK.toString() + "." + TableOfLabels.m_FILE_EXTENSION;
		} else {
			End.throwError(m_databaseType.toString() + " not supported");
		}

		//String property = m_headerProperties.getProperty("ContinuousHMMReestimator.nminimumNumberOfFramesForValidPattern", "1");
		//int nminimumNumberOfFramesInValidPattern = Integer.parseInt(property);

		//up to now I just want to get some "default" values
		//update just the ones that don't have already default values
		//cMPropertiesSet.updatePropertyIfEmpty("hMMSetFileName", jarHMMsFileName);
		//note: I am going to update anyways
//		cMPropertiesSet.updatePropertyIfEmpty("transcriptionsRootDirectory", directoryForDataLocators);
//		cMPropertiesSet.updatePropertyIfEmpty("parametersRootDirectory", directoryForSetOfPatterns);
//
//		cMPropertiesSet.updatePropertyIfEmpty("hmmsRootDirectory", rootDirectoryWithHMMs);
//		cMPropertiesSet.updatePropertyIfEmpty("tableOfLabelsForScoring", tableOfMonophoneLabelsForScoringFileName);

		cMPropertiesSet.setProperty("TrainingManager.TestSpeechDataRootDirectory", m_simulationFilesAndDirectories.getTestSpeechDataRootDirectory());

		cMPropertiesSet.setProperty("OffLineIsolatedSegmentsClassifier.transcriptionsRootDirectory", directoryForDataLocators);
		cMPropertiesSet.setProperty("OffLineIsolatedSegmentsClassifier.parametersRootDirectory", directoryForSetOfPatterns);

		cMPropertiesSet.setProperty("OffLineIsolatedSegmentsClassifier.hmmsRootDirectory", rootDirectoryWithHMMs);
		cMPropertiesSet.setProperty("OffLineIsolatedSegmentsClassifier.tableOfLabelsForScoring", tableOfMonophoneLabelsForScoringFileName);
		//cMPropertiesSet.updateProperty("tableOfLabelsForScoring", "");

//		OffLineIsolatedSegmentsClassifier.saveProperties(
//			m_outputTSTFileName,
//			jarHMMsFileName,
//			oisTriphone,
//			oisTraining,
//			directoryForDataLocators,
//			directoryForSetOfPatterns,
//			tableOfMonophoneLabelsForScoringFileName,
//			rootDirectoryWithHMMs,
//			oshouldWriteLattices,
//			oareTranscriptionsAvailable,
//			nminimumNumberOfFramesInValidPattern);
	}

	public static void updateTestingConfiguration(CMPropertiesSet cMPropertiesSet) {

		String patternGeneratorFileName = cMPropertiesSet.getProperty("PatternGenerator.FileName");
		if (patternGeneratorFileName == null ||
		patternGeneratorFileName.equals("") ||
		!(new File(patternGeneratorFileName).exists())) {
			return;
		}

		patternGeneratorFileName = cMPropertiesSet.getProperty("TrainingManager.DefaultHMMConfigurationFileName");
		if (patternGeneratorFileName == null ||
		patternGeneratorFileName.equals("") ||
		!(new File(patternGeneratorFileName).exists())) {
			return;
		}

		patternGeneratorFileName = cMPropertiesSet.getProperty("TrainingManager.TrainSpeechDataRootDirectory");
		if (patternGeneratorFileName == null ||
		patternGeneratorFileName.equals("") ||
		!(new File(patternGeneratorFileName).exists())) {
			return;
		}

		patternGeneratorFileName = cMPropertiesSet.getProperty("TrainingManager.GeneralOutputDirectory");
		if (patternGeneratorFileName == null ||
		patternGeneratorFileName.equals("")) {
			return;
		}

		SimulationFilesAndDirectories m_simulationFilesAndDirectories = new SimulationFilesAndDirectories(cMPropertiesSet.getHeaderProperties());

		String rootDirectoryWithHMMs = m_simulationFilesAndDirectories.getGeneralOutputDirectory();

		String trainingOrTest = cMPropertiesSet.getProperty("OffLineIsolatedSegmentsClassifier.oisTraining");
		boolean oisTraining = Boolean.valueOf(trainingOrTest).booleanValue();
		String directoryForDataLocators = null;
		if (oisTraining) {
			directoryForDataLocators = m_simulationFilesAndDirectories.getIsolatedMonophoneTranscriptionsOfTrainSpeechDataDirectory();
		}
		else {
			directoryForDataLocators = m_simulationFilesAndDirectories.getIsolatedMonophoneTranscriptionsOfTestSpeechDataDirectory();
		}
		String directoryForSetOfPatterns = null;
		if (oisTraining) {
			directoryForSetOfPatterns = m_simulationFilesAndDirectories.getIsolatedTrainParametersDataRootDirectory();
		}
		else {
			directoryForSetOfPatterns = m_simulationFilesAndDirectories.getIsolatedTestParametersDataRootDirectory();
		}
		boolean oshouldWriteLattices = true;

		//for TIMIT oareTranscriptionsAvailable = true;
		boolean oareTranscriptionsAvailable = true;
		String tableOfMonophoneLabelsForScoringFileName = null;
		Database.Type m_databaseType = Database.Type.getType(cMPropertiesSet.getPropertyAndExitIfKeyNotFound("Database.Type"));
		if (m_databaseType == Database.Type.TIMIT) {
			tableOfMonophoneLabelsForScoringFileName = rootDirectoryWithHMMs + TableOfLabels.Type.TIMIT39.toString() + "." + TableOfLabels.m_FILE_EXTENSION;
//			if (!new File(tableOfMonophoneLabelsForScoringFileName).exists()) {
//				TableOfLabels table = new TableOfLabels(TableOfLabels.Type.TIMIT39);
//				table.writeToFile(tableOfMonophoneLabelsForScoringFileName);
//			}
		} else if (m_databaseType == Database.Type.GENERAL) {
			tableOfMonophoneLabelsForScoringFileName = rootDirectoryWithHMMs + "tableForScoring." + TableOfLabels.m_FILE_EXTENSION;
//			if (!new File(tableOfMonophoneLabelsForScoringFileName).exists()) {
//				TableOfLabels tableOfLabels = getTableOfLabels(m_databaseType, m_simulationFilesAndDirectories);
//				tableOfLabels.writeToFile(tableOfMonophoneLabelsForScoringFileName);
//			}
			oareTranscriptionsAvailable = false;
		} else {
			End.throwError(m_databaseType.toString() + " not supported");
		}

		//String property = m_headerProperties.getProperty("ContinuousHMMReestimator.nminimumNumberOfFramesForValidPattern", "1");
		//int nminimumNumberOfFramesInValidPattern = Integer.parseInt(property);

		//up to now I just want to get some "default" values
		//update just the ones that don't have already default values
		//cMPropertiesSet.updatePropertyIfEmpty("hMMSetFileName", jarHMMsFileName);
		//note: I am going to update anyways
//		cMPropertiesSet.updatePropertyIfEmpty("transcriptionsRootDirectory", directoryForDataLocators);
//		cMPropertiesSet.updatePropertyIfEmpty("parametersRootDirectory", directoryForSetOfPatterns);
//
//		cMPropertiesSet.updatePropertyIfEmpty("hmmsRootDirectory", rootDirectoryWithHMMs);
//		cMPropertiesSet.updatePropertyIfEmpty("tableOfLabelsForScoring", tableOfMonophoneLabelsForScoringFileName);

		cMPropertiesSet.updateProperty("TrainingManager.TestSpeechDataRootDirectory", m_simulationFilesAndDirectories.getTestSpeechDataRootDirectory());

		cMPropertiesSet.updateProperty("OffLineIsolatedSegmentsClassifier.transcriptionsRootDirectory", directoryForDataLocators);
		cMPropertiesSet.updateProperty("OffLineIsolatedSegmentsClassifier.parametersRootDirectory", directoryForSetOfPatterns);

		cMPropertiesSet.updateProperty("OffLineIsolatedSegmentsClassifier.hmmsRootDirectory", rootDirectoryWithHMMs);
		cMPropertiesSet.updateProperty("OffLineIsolatedSegmentsClassifier.tableOfLabelsForScoring", tableOfMonophoneLabelsForScoringFileName);
		//cMPropertiesSet.updateProperty("tableOfLabelsForScoring", "");

//		OffLineIsolatedSegmentsClassifier.saveProperties(
//			m_outputTSTFileName,
//			jarHMMsFileName,
//			oisTriphone,
//			oisTraining,
//			directoryForDataLocators,
//			directoryForSetOfPatterns,
//			tableOfMonophoneLabelsForScoringFileName,
//			rootDirectoryWithHMMs,
//			oshouldWriteLattices,
//			oareTranscriptionsAvailable,
//			nminimumNumberOfFramesInValidPattern);
	}

	private static TableOfLabels getTableOfLabels(Database.Type m_databaseType,
	SimulationFilesAndDirectories m_simulationFilesAndDirectories) {

		//try to get a table from properties
		TableOfLabels m_tableOfLabels = null;//TableOfLabels.getTableOfLabels(m_headerProperties);
		//if unsuccessful:
		if (m_tableOfLabels == null) {
			if (m_databaseType == Database.Type.GENERAL) {
				//try to construct table from root directory
				m_tableOfLabels = new TableOfLabels(m_simulationFilesAndDirectories.getTrainSpeechDataRootDirectory());
				//add table to properties
			}
			else if (m_databaseType == Database.Type.TIMIT) {
				//assume default
				m_tableOfLabels = new TableOfLabels(TableOfLabels.Type.TIMIT39);
			}

			//m_tableOfLabels.writeToHeaderProperties(m_headerProperties);
			//check if successful
			if (m_tableOfLabels == null ||
					m_tableOfLabels.getNumberOfEntries() == 0) {
				Print.error("Could not construct TableOfLabels.");
				End.exit();
			}
		}
		return m_tableOfLabels;
	}

//	public static CMPropertiesSet getDefaultProperties() {
//		CMProperty[] properties  = new CMProperty[56];
//
//		properties[0] = new ComboBoxCMProperty("Database.Type",
//		"Database type", "'GENERAL' means any database organized in suddirectories",
//		Database.Type.m_types);
//
//		properties[1] = new PathStringCMProperty("TrainingManager.TrainSpeechDataRootDirectory",
//		"Input directory", "Root directory where speech data is stored", null,
//		null, true, null);
//
//		properties[2] = new PathStringCMProperty("TrainingManager.GeneralOutputDirectory",
//		"Output directory", "Root directory where files (HMM's, etc.) will be stored", null,
//		null, true, null);
//
//		properties[3] = new PathStringCMProperty("PatternGenerator.FileName",
//		"Front end configuration file", "Parameters used in front end", null,
//		PatternGenerator.m_FILE_EXTENSION, false, "front end configuration");
//
//		properties[4] = new PathStringCMProperty("TrainingManager.DefaultHMMConfigurationFileName",
//		"HMM configuration file", "Specifies number of states, HMM topology, etc.", null,
//		HMMConfiguration.m_FILE_EXTENSION, false, "HMM configuration");
//
//		properties[5] = new ComboBoxCMProperty("TrainingManager.Type",
//		"Training type", "Isolated-word systems use 'ISOLATED_SEGMENT' type",
//		TrainingManager.Type.m_types);
//
//		properties[6] = new PathStringCMProperty("TableOfLabels.FileName",
//		"File with labels", "Name of file with table of labels", null,
//		TableOfLabels.m_FILE_EXTENSION, false, "table of labels");
//
//		properties[7] = new ComboBoxCMProperty("TableOfLabels.Type",
//		"Table with labels", "Standard tables used for TIMIT database",
//		TableOfLabels.Type.m_types);
//
//		properties[8] = new BooleanCMProperty("TrainingManager.oshouldCreateTranscriptionsForSpeechFiles",
//		"Generate transcriptions?", "If true, create transcriptions (required to localize speech data)", new Boolean(true));
//
//		properties[9] = new BooleanCMProperty("TrainingManager.oshouldConvertSpeechDataIntoParameters",
//		"Calculate front end parameters?", "If true, convert speech data into features", new Boolean(true));
//
//		properties[10] = new BooleanCMProperty("TrainingManager.oshouldCreateHMMFiles",
//		"Estimate HMMs?", "If true, estimate HMM's", new Boolean(true));
//
//		properties[11] = new BooleanCMProperty("TrainingManager.oshouldRunClassification",
//		"Run classification test?", "If true, test system performance", new Boolean(true));
//
//		properties[12] = new IntegerCMProperty("TrainingManager.nverbose",
//		"Level of verbosity", "If 0, no messages, if 1 print basic messages, if 2 more information and so on", new Integer(1));
//
//		properties[13] = new IntegerCMProperty("TrainingManager.nmaximumNumberOfGaussiansPerMixture",
//		"Maximum Gaussians per mixture", "System can start with 1 Gaus./mix. and increase the # of Gaus. up to this maximum", new Integer(5));
//
//		properties[14] = new BooleanCMProperty("HMMReestimator.oshouldSkipReestimationIfFileExists",
//		"Skip files if they exist?", "If true, during HMM training, skip files that already exist unless " +
//		"they do not obey stochastic constraints", new Boolean(true));
//
//		properties[15] = new BooleanCMProperty("TrainingManager.oshouldWriteReportFile",
//		"Write output properties file?", "Output file written as Java Properties containing the configuration information", new Boolean(false));
//
//		properties[16] = new BooleanCMProperty("TrainingManager.ouseAbsolutePath",
//		"Use absolute paths?", "Write absolute paths in transcription (files are bigger)", new Boolean(false));
//
//	//TrainingManager.OutputTSTFileName=d:/fileoriented/mfccedc26w256s80/classification.TST
//		properties[17] = new PathStringCMProperty("TrainingManager.TestSpeechDataRootDirectory",
//		"Input directory for test data", "Where test speech data is stored (if not specified, system will try to guess)", null,
//		null, true, null);
//
//		properties[18] = new FloatCMProperty("ContinuousHMMReestimator.fconvergenceThreshold",
//		"Convergence threshold", "Used as stopping criterion in HMM reestimation", new Float(1e-4F));
//
//		properties[19] = new FloatCMProperty("ContinuousHMMReestimator.fcovarianceFloor",
//		"Covariance floor", "Minimum value for elements in main diagonal of covariance matrices", new Float(1e-4F));
//
//		properties[20] = new FloatCMProperty("ContinuousHMMReestimator.fmixtureWeightFloor",
//		"Weight floor", "Minimum value for mixture weights", new Float(0F));
//
//		properties[21] = new IntegerCMProperty("ContinuousHMMReestimator.nmaximumIterations",
//		"Maximum # of iterations", "Maximum number of iterations in HMM reestimation", new Integer(20));
//
//		properties[22] = new IntegerCMProperty("ContinuousHMMReestimator.nminimumNumberOfFramesForValidPattern",
//		"Minimum # of frames when training HMMs", "If the number of frames in a given example is below this value, the example is " +
//		"discarded during HMM reestimation", new Integer(1));
//
//		properties[23] = new IntegerCMProperty("ContinuousHMMReestimator.nminimumNumberOfPatternsPerModel",
//		"Minimum # of examples", "Each HMM requires this minimum number of examples, otherwise an error is generated", new Integer(3));
//
//		properties[24] = new BooleanCMProperty("ContinuousHMMReestimator.oshouldUpdateCovariance",
//		"Update covariance matrices?", "If false, keep covariance matrices with original values", new Boolean(true));
//
//		properties[25] = new BooleanCMProperty("ContinuousHMMReestimator.oshouldUpdateMean",
//		"Update Gaussian means?", "If false, keep means with original values", new Boolean(true));
//
//		properties[26] = new BooleanCMProperty("ContinuousHMMReestimator.oshouldUpdateTransitionMatrix",
//		"Update transition matrices?", "If false, keep transition matrices with original values", new Boolean(true));
//
//		properties[27] = new BooleanCMProperty("ContinuousHMMReestimator.oshouldUpdateWeights",
//		"Update mixture weights?", "If false, keep mixture weights with original values", new Boolean(true));
//
//		//fine control over patterns (MFCC, PLP,...) calculation
//		properties[28] = new BooleanCMProperty("TrainingManager.oshouldConvertIsolatedSpeechDataIntoParameters",
//		"Calculate features for isolated segments?", "If true calculates features (MFCC or PLP, etc)", new Boolean(true));
//
//		properties[29] = new BooleanCMProperty("TrainingManager.oshouldConvertSentenceSpeechDataIntoParameters",
//		"Calculate features for each sentence?", "If the database is GENERAL this option does not apply", new Boolean(false));
//
//		properties[30] = new BooleanCMProperty("TrainingManager.oshouldCreatFileWithDynamicRanges",
//		"Calculate file with dynamic ranges?", "If true writes a file with range of each feature", new Boolean(true));
//
//		//fine control over HMM training
//		properties[31] = new BooleanCMProperty("TrainingManager.oshouldCreateMonophoneHMMPrototypes",
//		"Create HMM prototypes?", "If true, based on HMM configuration and training sequence write one prototype per HMM", new Boolean(true));
//
//		properties[32] = new BooleanCMProperty("TrainingManager.oshouldCreateMonophoneHMMWithBaumWelchForIsolatedSegments",
//		"Use Baum-Welch?", "Use Baum-Welch algorithm to reestimate HMM prototype", new Boolean(true));
//
//		properties[33] = new BooleanCMProperty("TrainingManager.oshouldRecursivelyCreateMonophoneHMMWithBaumWelchForIsolatedSegments",
//		"Use Baum-Welch recursively?", "If true, start with HMM prototype and increase number of Gaussians per mixture until maximum", new Boolean(true));
//
//		properties[34] = new BooleanCMProperty("TrainingManager.oshouldRecursivelyCreateMonophoneHMMWithSegmentalKMeansForIsolatedSegments",
//		"Use segmental K-means recursively?", "If true, start with HMM prototype and increase number of Gaussians per mixture until maximum", new Boolean(false));
//
//		properties[35] = new BooleanCMProperty("TrainingManager.oshouldCreateMonophoneHMMWithEmbeddedBaumWelch",
//		"Use embedded Baum-Welch?", "If true, use embedded Baum-Welch to reestimate HMM prototype", new Boolean(false));
//
//		properties[36] = new BooleanCMProperty("TrainingManager.oshouldRecursivelyCreateMonophoneHMMWithEmbeddedBaumWelch",
//		"Use embedded Baum-Welch recursively?", "If true, start with HMM prototype and increase number of Gaussians per mixture until maximum", new Boolean(false));
//
//		properties[37] = new BooleanCMProperty("TrainingManager.oshouldCopyTIMITToUniqueDirectory",
//		"Reorganize TIMIT CD?", "If true, copy TIMIT database and give a unique name to files", new Boolean(false));
//
//		properties[38] = new StringCMProperty("hMMSetFileName",
//		"Test: HMM file name",
//		"HMM set file name usually with extension JAR",
//		SetOfHMMsFile.m_name);
//
//		properties[39] = new BooleanCMProperty("oshouldWriteLattices",
//		"Test: Generate lattices",
//		"Generate files with best paths (lattices) if true",
//		new Boolean(true));
//
//		properties[40] = new BooleanCMProperty("oareTranscriptionsAvailable",
//		"Test: Transcriptions are available",
//		"If true transcriptions are available and more detailed results can be obtained",
//		new Boolean(true));
//
//		properties[41] = new BooleanCMProperty("oisTraining",
//		"Test: Use training sequence for testing",
//		"If true the results will be written in directory 'train', if false in 'test'",
//		new Boolean(false));
//
//		properties[42] = new IntegerCMProperty("nminimumNumberOfFramesInValidPattern",
//		"Test: Minimum # of frames",
//		"Minimum number of frames for pattern to be valid during the testing stage",
//		new Boolean(true), new Boolean(true), new Boolean(false),
//		new Boolean(false), new Boolean(true),
//		new Integer(1), new Integer(1), null);
//
//		properties[43] = new PathStringCMProperty("hmmsRootDirectory",
//		"Test: HMM's root directory",
//		"All HMM files in subdirectories below this root will be tested",
//		null, null, true, null);
//		//want to demand user to change this parameter through another way
//		CMUtilities.setCMPropertyBooleanFields(properties[43],true,false,
//		false,false,true);
//
//		properties[44] = new PathStringCMProperty("parametersRootDirectory",
//		"Test: Parameters directory",
//		"Directory with the test parameters files (SOP files storing MFCC's or others)",
//		null, null, true, null);
//		//want to demand user to change this parameter through another way
//		CMUtilities.setCMPropertyBooleanFields(properties[44],true,false,
//		false,false,true);
//
//		properties[45] = new PathStringCMProperty("transcriptionsRootDirectory",
//		"Test: Transcriptions root directory",
//		"Directory with DTL files. Not used if transcriptions not available",
//		null, null, true, null);
//		//want to demand user to change this parameter through another way
//		CMUtilities.setCMPropertyBooleanFields(properties[45],true,false,
//		false,false,true);
//
//		properties[46] = new PathStringCMProperty("tableOfLabelsForScoring",
//		"Test: Scoring table file name",
//		"Table with labels used for scoring",
//		null, TableOfLabels.m_FILE_EXTENSION, false, "files with table of labels");
//		//want to demand user to change this parameter through another way
//		CMUtilities.setCMPropertyBooleanFields(properties[46],true,false,
//		false,false,true);
//
//		properties[47] = new IntegerCMProperty("TrainingManager.nsegmentalKmeansInitialNumberOfGaussians",
//		"Segmental K-means: initial # of Gaussians", "HMMs will be created starting from this number up to maximum # of Gaussians per mixture",
//		new Boolean(true), new Boolean(true), new Boolean(false),
//		new Boolean(false), new Boolean(true),
//		new Integer(1), new Integer(1), null);
//
//		properties[48] = new IntegerCMProperty("TrainingManager.nsegmentalKmeansGaussiansIncrement",
//		"Segmental K-means: increment in # of Gaussians", "If equal to 5 and initial # of Gaussians is 1, create HMMs with 1, 6, 11,... up to the specified maximum",
//		new Boolean(true), new Boolean(true), new Boolean(false),
//		new Boolean(false), new Boolean(true),
//		new Integer(1), new Integer(1), null);
//
//		properties[49] = new DoubleCMProperty("TrainingManager.dsegmentalKmeansThresholdToStopIterations",
//		"Segmental K-means: stopping criterion", "Stop iterations if likelihood improvement falls below this threshold", new Double(0.02));
//
//		properties[50] = new BooleanCMProperty("TrainingManager.osegmentalKmeansUseBaumWelchAfterwards",
//		"Segmental K-means: use Baum-Welch", "If true, use Baum-Welch to reestimate models after segmental K-means", new Boolean(true));
//
//		properties[51] = new IntegerCMProperty("TrainingManager.nembeddedBaumNumberOfIterations",
//		"Number of iterations of embedded Baum-Welch", "If embedded Baum-Welch is used, this gives the number of reestimations using the algorithm per HMM",
//		new Boolean(true), new Boolean(true), new Boolean(false),
//		new Boolean(false), new Boolean(true),
//		new Integer(2), new Integer(1), null);
//
//		properties[52] = new IntegerCMProperty("TrainingManager.ThreadPriority",
//		"Thread priority", "The bigger this number, more CPU cycles will be allocated to the simulation (and the other programs would respond slowly)",
//		new Boolean(true), new Boolean(true), new Boolean(false),
//		new Boolean(false), new Boolean(true), new Integer(Thread.NORM_PRIORITY),
//		new Integer(Thread.MIN_PRIORITY), new Integer(Thread.MAX_PRIORITY));
//
//		properties[53] = new BooleanCMProperty("ConvertALIENFrontEndToSOPFiles.oareBigEndian",
//		"ALIEN files are big-endian?", "When converting ALIEN to SOP files, if true assume float numbers in big-endian (Sun, Vax, etc.), if false little-endian (PC)", new Boolean(true));
//
//		properties[54] = new PathStringCMProperty("ConvertALIENFrontEndToSOPFiles.inputDirectory",
//		"Directory with training sequence of ALIEN front end",
//		"Used when converting ALIEN front end files. The directory with the test sequence must be ../test",
//		null, null, true, null);
//
//		properties[55] = new StringCMProperty("ConvertALIENFrontEndToSOPFiles.ExtensionForAlienFiles",
//		"Extension for alien front ends",
//		"If not specified system will try to convert all files below given directory, otherwise only with given extension",
//		"ALI");
//
//		//Did not include the following properties yet:
//
//	TrainingManager.IsolatedTestParametersDirectory=d:/fileoriented/mfccedc26w256s80/features/test/
//	TrainingManager.IsolatedTrainParametersDirectory=d:/fileoriented/mfccedc26w256s80/features/train/
//	TrainingManager.oshouldCreateTriphoneModels=true
//	TrainingManager.oshouldRunRecognitionForTestingData=true
//	TrainingManager.oshouldRunRecognitionForTrainingData=false
//	protected boolean m_oshouldCreateTriphoneTranscriptions = false;
//	//
//		protected boolean m_oshouldCloneMonophonesAndReestimatePlainTriphoneHMHs = false;
//		protected boolean m_oshouldClusterPlainTriphones = false;
//		protected boolean m_oshouldCreateCrossWordTriphoneHMMWithEmbeddedBaumWelch = false;
//		protected boolean m_oshouldRecursivelyCreateTriphoneHMMWithEmbeddedBaumWelch = false;
//
//		//set default values
//		//CMUtilities.setDefaultCMPropertyBooleanFields(properties);
//
//		return new CMPropertiesSet(properties);
//	}

}
//end of class

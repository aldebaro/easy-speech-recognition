package edu.ucsd.asr;

import java.io.File;

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
public class SimulationFilesAndDirectories {

	private HeaderProperties m_headerProperties;
	public final String m_setOfHMMsFileName = SetOfHMMsFile.m_name;
	private String m_transcriptionFileExtension;
	private String m_defaultHMMConfigurationFileName;
	private String m_featuresDescription;
	private String m_topologyIdentifier;
	Database.Type m_databaseType;

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
	public SimulationFilesAndDirectories(HeaderProperties headerProperties) {
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

	public String getValidationSpeechDataRootDirectory() {
		String out = FileNamesAndDirectories.getParent(m_testSpeechDataRootDirectory);
		return FileNamesAndDirectories.concatenateTwoPaths(out, "validation/");
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
		//FileNamesAndDirectories.createDirectoriesIfNecessary(dir);
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
		if (m_databaseType == Database.Type.TIMIT) {
			 return m_generalOutputDirectory + m_topologyIdentifier + "/monophones/sentences/";
		} else {
			return m_sentencesMonophoneHMMsFinalDirectory;
		}
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
					". Did you run the front end?" +
					" If yes, please check your configuration.");
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
		} else {
			//eventually use default configuration
			thisHMMConfigurationFileName = m_headerProperties.getProperty("TrainingManager.HMMConfiguration_" + ntableEntry,
					m_defaultHMMConfigurationFileName);
		}
		//ak I don't understand the below, maybe to get topology when some HMMs were already trained?
		//I will keep it as default...
		thisHMMConfigurationFileName = FileNamesAndDirectories.getAbsolutePath(thisHMMConfigurationFileName, m_generalOutputDirectory + m_topologyIdentifier + "/");
		if (! (new File(thisHMMConfigurationFileName)).exists()) {
			//in this case the file doesn't exist, so simply use:
			thisHMMConfigurationFileName = m_defaultHMMConfigurationFileName;
		}
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
				sopFileName;
								//I had to comment this line to make this work under linux. not sure if this affects other stuff
								//Nikola
		//sopFileName = sopFileName.toLowerCase();
		sopFileName += "." +
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
		//FileNamesAndDirectories.createDirectoriesIfNecessary(m_isolatedMonophoneTranscriptionsOfTrainSpeechDataDirectory);

		m_isolatedMonophoneTranscriptionsOfTestSpeechDataDirectory = m_generalOutputDirectoryWithoutFeatureDescription +  "transcriptions/monophones/isolated/test/";
		//FileNamesAndDirectories.createDirectoriesIfNecessary(m_isolatedMonophoneTranscriptionsOfTestSpeechDataDirectory);

		m_sentenceMonophoneTranscriptionsOfTrainSpeechDataFileName = m_generalDatabaseDirectory +  "transcriptions/train." + m_transcriptionFileExtension;
		//FileNamesAndDirectories.createDirectoriesIfNecessary(FileNamesAndDirectories.getPathFromFileName(m_sentenceMonophoneTranscriptionsOfTrainSpeechDataFileName));

		m_sentenceMonophoneTranscriptionsOfTestSpeechDataFileName = m_generalDatabaseDirectory +  "transcriptions/test." + m_transcriptionFileExtension;
		//FileNamesAndDirectories.createDirectoriesIfNecessary(FileNamesAndDirectories.getPathFromFileName(m_sentenceMonophoneTranscriptionsOfTestSpeechDataFileName));

		m_sentenceTriphoneTranscriptionsOfTrainSpeechDataFileName = m_generalOutputDirectoryWithoutFeatureDescription +  "transcriptions/triphones/sentences/train." + m_transcriptionFileExtension;
		//FileNamesAndDirectories.createDirectoriesIfNecessary(FileNamesAndDirectories.getPathFromFileName(m_sentenceTriphoneTranscriptionsOfTrainSpeechDataFileName));

		m_sentenceTriphoneTranscriptionsOfTestSpeechDataFileName = m_generalOutputDirectoryWithoutFeatureDescription + "transcriptions/triphones/sentences/test." + m_transcriptionFileExtension;
		//FileNamesAndDirectories.createDirectoriesIfNecessary(FileNamesAndDirectories.getPathFromFileName(m_sentenceTriphoneTranscriptionsOfTestSpeechDataFileName));
	}

	private void organizeTranscriptionsUnderParentOfSpeechDataDirectoryQUESIPASA() {
		m_isolatedMonophoneTranscriptionsOfTrainSpeechDataDirectory = FileNamesAndDirectories.getParent(m_trainSpeechDataRootDirectory);
		m_isolatedMonophoneTranscriptionsOfTrainSpeechDataDirectory = FileNamesAndDirectories.concatenateTwoPaths(m_isolatedMonophoneTranscriptionsOfTrainSpeechDataDirectory, "transcriptions/monophones/isolated/train/");
		//FileNamesAndDirectories.createDirectoriesIfNecessary(m_isolatedMonophoneTranscriptionsOfTrainSpeechDataDirectory);

		m_isolatedMonophoneTranscriptionsOfTestSpeechDataDirectory = FileNamesAndDirectories.getParent(m_testSpeechDataRootDirectory);
		m_isolatedMonophoneTranscriptionsOfTestSpeechDataDirectory = FileNamesAndDirectories.concatenateTwoPaths(m_isolatedMonophoneTranscriptionsOfTestSpeechDataDirectory, "transcriptions/monophones/isolated/test/");
		//FileNamesAndDirectories.createDirectoriesIfNecessary(m_isolatedMonophoneTranscriptionsOfTestSpeechDataDirectory);

		m_sentenceMonophoneTranscriptionsOfTrainSpeechDataFileName = FileNamesAndDirectories.getParent(m_trainSpeechDataRootDirectory);
		m_sentenceMonophoneTranscriptionsOfTrainSpeechDataFileName = FileNamesAndDirectories.concatenateTwoPaths(m_sentenceMonophoneTranscriptionsOfTrainSpeechDataFileName, "transcriptions/monophones/sentences/train." + m_transcriptionFileExtension);
		//FileNamesAndDirectories.createDirectoriesIfNecessary(FileNamesAndDirectories.getPathFromFileName(m_sentenceMonophoneTranscriptionsOfTrainSpeechDataFileName));

		m_sentenceMonophoneTranscriptionsOfTestSpeechDataFileName = FileNamesAndDirectories.getParent(m_testSpeechDataRootDirectory);
		m_sentenceMonophoneTranscriptionsOfTestSpeechDataFileName = FileNamesAndDirectories.concatenateTwoPaths(m_sentenceMonophoneTranscriptionsOfTestSpeechDataFileName, "transcriptions/monophones/sentences/test." + m_transcriptionFileExtension);
		//FileNamesAndDirectories.createDirectoriesIfNecessary(FileNamesAndDirectories.getPathFromFileName(m_sentenceMonophoneTranscriptionsOfTestSpeechDataFileName));

		m_sentenceTriphoneTranscriptionsOfTrainSpeechDataFileName = FileNamesAndDirectories.getParent(m_trainSpeechDataRootDirectory);
		m_sentenceTriphoneTranscriptionsOfTrainSpeechDataFileName = FileNamesAndDirectories.concatenateTwoPaths(m_sentenceTriphoneTranscriptionsOfTrainSpeechDataFileName, "transcriptions/triphones/sentences/train." + m_transcriptionFileExtension);
		//FileNamesAndDirectories.createDirectoriesIfNecessary(FileNamesAndDirectories.getPathFromFileName(m_sentenceTriphoneTranscriptionsOfTrainSpeechDataFileName));

		m_sentenceTriphoneTranscriptionsOfTestSpeechDataFileName = FileNamesAndDirectories.getParent(m_testSpeechDataRootDirectory);
		m_sentenceTriphoneTranscriptionsOfTestSpeechDataFileName = FileNamesAndDirectories.concatenateTwoPaths(m_sentenceTriphoneTranscriptionsOfTestSpeechDataFileName, "transcriptions/triphones/sentences/test." + m_transcriptionFileExtension);
		//FileNamesAndDirectories.createDirectoriesIfNecessary(FileNamesAndDirectories.getPathFromFileName(m_sentenceTriphoneTranscriptionsOfTestSpeechDataFileName));
	}

	private void organizeSpeechWaveforms() {
		m_trainSpeechDataRootDirectory = m_headerProperties.getPropertyAndExitIfKeyNotFound("TrainingManager.TrainSpeechDataRootDirectory");
		m_trainSpeechDataRootDirectory = FileNamesAndDirectories.replaceAndForceEndingWithSlash(m_trainSpeechDataRootDirectory);
		//Print.warning("m_trainSpeechDataRootDirectory = " + m_trainSpeechDataRootDirectory);
		m_testSpeechDataRootDirectory = m_headerProperties.getProperty("TrainingManager.TestSpeechDataRootDirectory");
		if (m_testSpeechDataRootDirectory == null || m_testSpeechDataRootDirectory.trim().equals("")) {
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
		//FileNamesAndDirectories.createDirectoriesIfNecessary(m_sentenceTrainParametersDataRootDirectory);
		m_sentenceTestParametersDataRootDirectory = m_generalDatabaseDirectory + "features/" + m_featuresDescription + "/test/";
		//FileNamesAndDirectories.createDirectoriesIfNecessary(m_sentenceTestParametersDataRootDirectory);

		m_isolatedTrainParametersDataRootDirectory = m_generalOutputDirectory + "features/train/";
		//FileNamesAndDirectories.createDirectoriesIfNecessary(m_isolatedTrainParametersDataRootDirectory);
		m_isolatedTestParametersDataRootDirectory = m_generalOutputDirectory + "features/test/";
		//FileNamesAndDirectories.createDirectoriesIfNecessary(m_isolatedTestParametersDataRootDirectory);
	}

	private void organizeHMMs() {
		//isolated segments monophones
		m_isolatedMonophoneHMMPrototypesDirectory = m_generalOutputDirectory + m_topologyIdentifier + "/monophones/isolated/prototypes/";
		//FileNamesAndDirectories.createDirectoriesIfNecessary(m_isolatedMonophoneHMMPrototypesDirectory);
		m_isolatedMonophoneHMMsKMeansViterbiDirectory = m_generalOutputDirectory + m_topologyIdentifier + "/monophones/isolated/kmeansviterbi/";
		//FileNamesAndDirectories.createDirectoriesIfNecessary(m_isolatedMonophoneHMMsKMeansViterbiDirectory);
		m_isolatedMonophoneHMMsBaumWelchDirectory = m_generalOutputDirectory + m_topologyIdentifier + "/monophones/isolated/baumwelch/";
		//FileNamesAndDirectories.createDirectoriesIfNecessary(m_isolatedMonophoneHMMsBaumWelchDirectory);

		//sentences monophones
		m_isolatedMonophoneHMMsFinalDirectory = m_generalOutputDirectory + m_topologyIdentifier + "/monophones/isolated/finalmodels/";
		//FileNamesAndDirectories.createDirectoriesIfNecessary(m_isolatedMonophoneHMMsFinalDirectory);
		m_sentencesMonophoneHMMsFinalDirectory = m_generalOutputDirectory + m_topologyIdentifier + "/monophones/sentences/finalmodels/";
		//FileNamesAndDirectories.createDirectoriesIfNecessary(m_sentencesMonophoneHMMsFinalDirectory);

		//sentences plain triphones
		m_sentencesPlainTriphoneHMMPrototypesDirectory = m_generalOutputDirectory + m_topologyIdentifier + "/triphones/sentences/plain/prototypes/";
		//FileNamesAndDirectories.createDirectoriesIfNecessary(m_sentencesPlainTriphoneHMMPrototypesDirectory);
		m_sentencesPlainTriphoneHMMsFinalDirectory = m_generalOutputDirectory + m_topologyIdentifier + "/triphones/sentences/plain/finalmodels/";
		//FileNamesAndDirectories.createDirectoriesIfNecessary(m_sentencesPlainTriphoneHMMsFinalDirectory);

		//sentences shared triphones
		m_sentencesSharedTriphoneHMMsFinalDirectory = m_generalOutputDirectory + m_topologyIdentifier + "/triphones/sentences/shared/finalmodels/";
		//FileNamesAndDirectories.createDirectoriesIfNecessary(m_sentencesSharedTriphoneHMMsFinalDirectory);
		m_sentencesSharedTriphoneHMMPrototypesDirectory = m_generalOutputDirectory + m_topologyIdentifier + "/triphones/sentences/shared/prototypes/";
		//FileNamesAndDirectories.createDirectoriesIfNecessary(m_sentencesSharedTriphoneHMMPrototypesDirectory);

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
			m_databaseType = Database.Type.GENERAL;
			organizeDirectoriesForFileOrientedTraining();
			return;
		} else {
			m_databaseType = Database.Type.TIMIT;
		}

		m_generalOutputDirectoryWithoutFeatureDescription = m_headerProperties.getPropertyAndExitIfKeyNotFound("TrainingManager.GeneralOutputDirectory");
		m_generalOutputDirectoryWithoutFeatureDescription = FileNamesAndDirectories.replaceAndForceEndingWithSlash(m_generalOutputDirectoryWithoutFeatureDescription);

		m_generalOutputDirectoryWithoutFeatureDescription += TableOfLabels.getTableOfLabels(m_headerProperties).getNumberOfEntries() + "models/";

		m_generalOutputDirectory = m_generalOutputDirectoryWithoutFeatureDescription + m_featuresDescription + "/";
		m_generalOutputDirectory = FileNamesAndDirectories.replaceAndForceEndingWithSlash(m_generalOutputDirectory);
		//FileNamesAndDirectories.createDirectoriesIfNecessary(m_generalOutputDirectory);
		// FileNamesAndDirectories.createDirectoriesIfNecessary(m_generalOutputDirectoryWithoutFeatureDescription);

		String htkSpecifiesStates = m_headerProperties
				.getProperty("HTK.nnumberOfStates");

		if (htkSpecifiesStates == null) {
			// try to get a HMM configuration file
			// notice that if it's null, it is assumed there will exist
			// a HMM configuration file for each table entry
			m_defaultHMMConfigurationFileName = m_headerProperties
					.getProperty("TrainingManager.DefaultHMMConfigurationFileName");
			// get topology identifier. firstly try to get it from current
			// properties
			m_topologyIdentifier = HMMConfiguration
					.getTopologyDescription(m_headerProperties);
			if (m_topologyIdentifier == null) {
				// then try getting it from a HCN file
				String fileWithTopologyIdentifier = null;
				if (m_defaultHMMConfigurationFileName != null) {
					m_headerProperties.setProperty(
							"TrainingManager.DefaultHMMConfigurationFileName",
							m_defaultHMMConfigurationFileName);
					fileWithTopologyIdentifier = m_defaultHMMConfigurationFileName;
				} else {
					// then use the configuration of first entry # 0
					fileWithTopologyIdentifier = getHMMConfigurationFileNameForIsolatedSegments(0);
				}
				m_topologyIdentifier = HMMConfiguration
						.getTopologyDescription(fileWithTopologyIdentifier);
			}
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
		//FileNamesAndDirectories.createDirectoriesIfNecessary(m_generalOutputDirectory);
		//FileNamesAndDirectories.createDirectoriesIfNecessary(m_generalOutputDirectoryWithoutFeatureDescription);

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
		//FileNamesAndDirectories.createDirectoriesIfNecessary(m_isolatedMonophoneTranscriptionsOfTrainSpeechDataDirectory);

		m_isolatedMonophoneTranscriptionsOfTestSpeechDataDirectory = m_generalOutputDirectoryWithoutFeatureDescription +  "transcriptions/test/";
		//FileNamesAndDirectories.createDirectoriesIfNecessary(m_isolatedMonophoneTranscriptionsOfTestSpeechDataDirectory);

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
		//FileNamesAndDirectories.createDirectoriesIfNecessary(m_isolatedTrainParametersDataRootDirectory);
		m_isolatedTestParametersDataRootDirectory = m_generalOutputDirectory + "features/test/";
		//FileNamesAndDirectories.createDirectoriesIfNecessary(m_isolatedTestParametersDataRootDirectory);


	//private void organizeHMMs() {
		//isolated segments monophones
		m_isolatedMonophoneHMMPrototypesDirectory = m_generalOutputDirectory + "hmms/" + m_topologyIdentifier + "/prototypes/";
		//FileNamesAndDirectories.createDirectoriesIfNecessary(m_isolatedMonophoneHMMPrototypesDirectory);
		m_isolatedMonophoneHMMsKMeansViterbiDirectory = m_generalOutputDirectory + "hmms/" + m_topologyIdentifier + "/segmentalkmeans/";
		//FileNamesAndDirectories.createDirectoriesIfNecessary(m_isolatedMonophoneHMMsKMeansViterbiDirectory);
		m_isolatedMonophoneHMMsBaumWelchDirectory = m_generalOutputDirectory + "hmms/" + m_topologyIdentifier + "/baumwelch/";
		//FileNamesAndDirectories.createDirectoriesIfNecessary(m_isolatedMonophoneHMMsBaumWelchDirectory);

		//sentences monophones
		m_isolatedMonophoneHMMsFinalDirectory = m_generalOutputDirectory + "hmms/" + m_topologyIdentifier + "/finalmodels/";
		//FileNamesAndDirectories.createDirectoriesIfNecessary(m_isolatedMonophoneHMMsFinalDirectory);
		m_sentencesMonophoneHMMsFinalDirectory = m_generalOutputDirectory + "hmms/" + m_topologyIdentifier + "/embeddedbaum/";
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

}
//end of class

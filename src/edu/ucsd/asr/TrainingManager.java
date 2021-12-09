package edu.ucsd.asr;

import java.util.Vector;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.io.File;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.FileWriter;
import javax.sound.sampled.AudioFormat;

import ioproperties.*;

/**
 *  Organize training sessions.
 *
 *@author     Aldebaro Klautau
 *@created    November 24, 2000
 *@version    2.0 - October 01, 2000.
 */
public class TrainingManager
    implements Runnable {

  private SimulationListener m_simulationListener = null;

  //private final boolean m_odoNotUseGlottalStopQ = true;
  public static final String m_reportFileName = "report.log";

  //protected CMProperty[] m_testingCMProperties;

  private String m_outputTSTFileName;

  protected Type m_type;
  protected PatternGenerator m_patternGenerator;

  protected boolean m_oshouldCopyDatabaseToUniqueDirectory;
  protected boolean m_oshouldCreateTranscriptionsForSpeechFiles;
  protected boolean m_oshouldConvertSpeechDataIntoParameters;
  protected boolean m_oshouldCreateHMMFiles;
  protected boolean m_oshouldWriteReportFile;
  protected boolean m_oshouldRunClassification;

  protected boolean m_oshouldCreatePrototypeHMMsFromIsolatedSegmentsOfTIMIT;
  protected boolean m_oshouldRunRecognitionForTrainingData;
  protected boolean m_oshouldRunRecognitionForTestingData;
  protected boolean m_oshouldCreateTriphoneModels;

  //fine control over HMM training
  protected int m_nmaximumNumberOfGaussiansPerMixture;
  protected boolean m_oshouldCreateMonophoneHMMPrototypes = true;
  protected boolean
      m_oshouldRecursivelyCreateMonophoneHMMWithSegmentalKMeansForIsolatedSegments = false;
  protected boolean m_oshouldCreateMonophoneHMMWithBaumWelchForIsolatedSegments = true;
  protected boolean
      m_oshouldRecursivelyCreateMonophoneHMMWithBaumWelchForIsolatedSegments = true;
  protected boolean m_oshouldCreateMonophoneHMMWithEmbeddedBaumWelch = false;
  protected boolean m_oshouldRecursivelyCreateMonophoneHMMWithEmbeddedBaumWelch = false;
  protected boolean m_oshouldCloneMonophonesAndReestimatePlainTriphoneHMHs = false;
  protected boolean m_oshouldClusterPlainTriphones = false;
  protected boolean m_oshouldCreateCrossWordTriphoneHMMWithEmbeddedBaumWelch = false;
  protected boolean m_oshouldRecursivelyCreateTriphoneHMMWithEmbeddedBaumWelch = false;

  //fine control over patterns (MFCC, PLP,...) calculation
  protected boolean m_oshouldConvertSentenceSpeechDataIntoParameters = false;
  protected boolean m_oshouldCreatFileWithDynamicRanges = true;
  protected boolean m_oshouldConvertIsolatedSpeechDataIntoParameters = true;

  //fine control over transcriptions
  protected boolean m_oshouldCreateTriphoneTranscriptions = false;

  private Thread m_runThread;
  private boolean m_oisRunning;
  private final TableOfLabels m_defaultTableForHMMs = new TableOfLabels(
      TableOfLabels.Type.TIMIT48);

  private String m_headerPropertiesFileName = "simulation.txt";

  //protected because of subclass HTKToolsCaller
  protected TableOfLabels m_tableOfLabels;
  protected HeaderProperties m_headerProperties;
  private DataLocator.Type[] m_dataLocatorTypes;
  protected SimulationFilesAndDirectories m_simulationFilesAndDirectories;
  private SimulationFilesAndDirectories m_hTKFilesAndDirectories;

  protected Database.Type m_databaseType;

  private boolean m_ouseAbsolutePath;

  //private boolean m_okeepDirectoryStructureForSOPsOfSentences;

  private int m_nverbose;

  private String m_outputReportFileName;

  //private String m_defaultHMMConfigurationFileName;
  protected String m_originalDirectoryOfDatabaseTestData;
  protected String m_originalDirectoryOfDatabaseTrainData;

  private HTKToolsCaller m_hTKToolsCaller;

  /**
   *  Mandatory extension for files.
   */
  //files of this type does NOT follow David's ioproperties format, but the
  //old HeaderPropertiesfs
  public final static String m_FILE_EXTENSION = "TRN";

  /**
   *  Constructor for the TrainingManager object
   *
   *@param  fileName  Description of Parameter
   */
  public TrainingManager(String fileName) {

    m_headerPropertiesFileName = fileName;

    FileNamesAndDirectories.checkExtensionWithCaseIgnoredAndExitOnError(
        fileName,
        m_FILE_EXTENSION);

    FileWithHeaderReader fileWithHeaderReader = new FileWithHeaderReader(
        fileName);
    if (!fileWithHeaderReader.wasFileOpenedSuccessfully()) {
      End.throwError("Could not open file " + fileName + ".");
    }
    if (!fileWithHeaderReader.wasEndOfHeaderIdentifierFound()) {
      End.throwError("String " + FileWithHeaderReader.m_endOfHeaderIdentifier +
                     " indicating end of header was not found in file " +
                     fileName + ".");
    }
    m_headerProperties = fileWithHeaderReader.getHeaderProperties();
    //m_headerProperties.list(System.out);

    HeaderProperties.replaceBackByForwardSlash(m_headerProperties);

    m_simulationFilesAndDirectories = new SimulationFilesAndDirectories(
        m_headerProperties);

    //initializeHTKInformation();
    interpretHeaderAndInitialize();
  }

  public TrainingManager(HeaderProperties headerProperties) {
    m_headerProperties = headerProperties;
    m_simulationFilesAndDirectories = new SimulationFilesAndDirectories(
        m_headerProperties);
    //initializeHTKInformation();
    interpretHeaderAndInitialize();
  }

  public void addSimulationListener(SimulationListener simulationListener) {
    m_simulationListener = simulationListener;
  }

  /**
   *  Start Thread that executes method run().
   */
  public void runSimulation() {
    //now, create Thread to run training
    if (m_runThread == null) {
      m_runThread = new Thread(this, "TrainingManager");
      //use max priority as below, but other softwares get slower
      //m_runThread.setPriority(Thread.MAX_PRIORITY);
      //so, I am assuming a default of normal priority
      int npriority = m_headerProperties.getIntegerPropertyAndExitIfKeyNotFound(
          "TrainingManager.ThreadPriority");
      if (npriority < Thread.MIN_PRIORITY || npriority > Thread.MAX_PRIORITY) {
        m_runThread.setPriority(Thread.NORM_PRIORITY);
      }
      else {
        m_runThread.setPriority(npriority);
      }
      m_oisRunning = true;
      m_runThread.start();
      //start() calls method run()
    }
    else {
      Print.warning("Can't init. thread already running.");
    }
  }

  public synchronized boolean isSimulationRunning() {
    return m_oisRunning;
  }

  /**
   *  Stop thread.
   */
  public synchronized void stopSimulation() {

    m_oisRunning = false;
    //waits until thread stops (pp. 34 book by Scott Oaks)
    if (m_runThread != null) {
      //System.out.println("m_runThread != null, try to join it...");
      try {
        //why this thing doesn't die if I use:
        //m_runThread.join();  ?? without time limit
        //because I was calling this method using this m_runThread Thread!
        m_runThread.join();
        //m_runThread.join(3000);
        //waits for this thread to die
      }
      catch (InterruptedException e) {
        e.printStackTrace();
      }
      m_runThread = null;
      //System.out.println("m_runThread = null now");
    }
    notifyListener();
  }

  private void notifyListener() {
    if (m_simulationListener != null) {
      m_simulationListener.simulationEnded();
    }
  }

  /**
   *  Top level method that controls the training procedure and runs on its own
   *  Thread.
   */
  //write report file after each step to avoid loosing
  //all information if some problem happens before end of simulation
  public void run() {

    if (m_oshouldConvertSpeechDataIntoParameters &&
        m_patternGenerator.m_type == PatternGenerator.Type.ALIEN) {
      Print.error("TrainingManager: Do not use this software to run an " +
                  PatternGenerator.Type.ALIEN.toString() + " front end!\n" +
                  PatternGenerator.Type.ALIEN.toString() +
                  " is a special front end nomenclature adopted when the user generated\n" +
                  "the parameters using another software, as Matlab for example.\n");
      notifyListener();
      return;
    }

    if (m_oisRunning) {
      getTableOfLabels();
    }

    //initializeHTKInformation();
    //constructHTKToolsCaller();
    //m_hTKToolsCaller.convertAllHMMFilesToJAR();
    //System.exit(1);

    //runClassificationTest(getFinalSetOfMonophoneHMMs(),false,false,m_finalDirectoryForMonophoneIsolatedHMMs);
    //runClassificationTest(getFinalSetOfMonophoneHMMs(),false,true,m_finalDirectoryForMonophoneIsolatedHMMs);
    //System.exit(1);
    //copyTIMITDatabaseIfRequested();
    //runHTKToolsIfNecessary();
    //implements isolated segments training for TIMIT & GENERAL databases
    if (m_type == Type.ISOLATED_SEGMENT) {
      doFileOrientedTraining();
    }
    else if (m_type == Type.SENTENCE && m_databaseType == Database.Type.TIMIT) {
      doTIMITTraining();
    }
    else if (m_type == Type.SENTENCE &&
             m_databaseType == Database.Type.TIDIGITS) {
      doTIDIGITSTraining();
    }
    else if (m_type == Type.SENTENCE &&
             m_databaseType == Database.Type.SPOLTECHRAW) {
      doSPOLTECHRAWTraining();
    }
    else if (m_type == Type.SENTENCE &&
            m_databaseType == Database.Type.DECTALK) {
        doDECTALKTraining();
    } else {
      End.throwError("Combination of " + m_type.toString() + " and " +
                     m_databaseType.toString() + " is not supported.");
    }

    writePropertiesToFileIfRequiredByUser();

    //TST file is part of CMProperties now
    //writeTSTFile();

    if (m_oshouldWriteReportFile && m_nverbose > 0) {
      Print.dialog("Finished training. Report wrote to file " +
                   m_outputReportFileName + ".");
    }
    else if (m_nverbose > 0) {
      Print.dialog("Finished training procedure.");
    }

    if (m_oisRunning && m_oshouldRunClassification) {
      Print.updateJProgressBar(0);
      if (m_nverbose > 0) {
        Print.dialog("Started testing procedure.");
      }
      //classification
      runClassificationTestForIsolatedSegments();
      //should call recognition method somewhere around here
      if (m_nverbose > 0) {
        Print.dialog("Finished testing procedure.");
      }
      Print.updateJProgressBar(0);
    }

    notifyListener();
  }

  /**
   *  Gets the TableOfLabels attribute of the TrainingManager object
   */
  private void getTableOfLabels() {

    //try to get a table from properties
    m_tableOfLabels = TableOfLabels.getTableOfLabels(m_headerProperties);
    //if unsuccessful:
    if (m_tableOfLabels == null) {
      if (m_databaseType == Database.Type.GENERAL) {
        //try to construct table from root directory
        m_tableOfLabels = new TableOfLabels(m_simulationFilesAndDirectories.
                                            getTrainSpeechDataRootDirectory());
        //add table to properties
      }
      else if (m_databaseType == Database.Type.TIMIT) {
        //assume default
        m_tableOfLabels = m_defaultTableForHMMs;
      }

      m_tableOfLabels.writeToHeaderProperties(m_headerProperties);
      //check if successful
      if (m_tableOfLabels == null ||
          m_tableOfLabels.getNumberOfEntries() == 0) {
        Print.error("Could not construct TableOfLabels.");
        End.exit();
      }
    }
  }

  public void runClassificationTestForIsolatedSegments() {
//		CMProperty[] testingCMProperties = null;
//		if (new File(m_outputTSTFileName).exists()) {
//			testingCMProperties = CMUtilities.getAllCMPropertiesFromFile(m_outputTSTFileName,
//						IO.m_NEW_LINE, ConfigurationManipulator.KEYWORDS);
//		} else {
//			End.throwError("File " + m_outputTSTFileName + " could not be opened");
//		}


    //
    String temp = m_headerProperties.getProperty(
        "TrainingManager.oshouldGuessTestConfigurationAndOverwriteExistingValues",
        "false");
    if (new Boolean(temp).booleanValue()) {
      TrainingManagerConfigurator.updateTestingConfiguration(m_headerProperties);
    }
    else {
      //maybe the user did not provide the necessary information, or it is incorrect
      temp = m_headerProperties.getProperty(
          "OffLineIsolatedSegmentsClassifier.transcriptionsRootDirectory");
      if (temp == null || ! (new File(temp).exists())) {
        TrainingManagerConfigurator.updateTestingConfiguration(
            m_headerProperties);
      }
    }

    if (m_oshouldRunRecognitionForTrainingData ||
        m_oshouldRunRecognitionForTestingData) {
      createTableOfMonophoneLabelsForScoringIfNecessary();
      OffLineIsolatedSegmentsClassifier.setVerbosity(m_nverbose);
    }
    if (m_oshouldRunRecognitionForTrainingData) {
      boolean ooriginal = m_headerProperties.
          getBooleanPropertyAndExitIfKeyNotFound(
          "OffLineIsolatedSegmentsClassifier.oisTraining");
      m_headerProperties.setProperty(
          "OffLineIsolatedSegmentsClassifier.oisTraining", "true");
      //keep the original values
      String directoryForDataLocators = m_headerProperties.
          getPropertyAndExitIfKeyNotFound(
          "OffLineIsolatedSegmentsClassifier.transcriptionsRootDirectory");
      String directoryForSetOfPatterns = m_headerProperties.
          getPropertyAndExitIfKeyNotFound(
          "OffLineIsolatedSegmentsClassifier.parametersRootDirectory");
      //set values for training
      m_headerProperties.setProperty(
          "OffLineIsolatedSegmentsClassifier.transcriptionsRootDirectory",
          m_simulationFilesAndDirectories.
          getIsolatedMonophoneTranscriptionsOfTrainSpeechDataDirectory());
      m_headerProperties.setProperty(
          "OffLineIsolatedSegmentsClassifier.parametersRootDirectory",
          m_simulationFilesAndDirectories.
          getIsolatedTrainParametersDataRootDirectory());
      if (m_nverbose > 0) {
        Print.dialog("Testing using train data: " +
                     IO.getEndOfString(m_simulationFilesAndDirectories.
                                       getIsolatedTrainParametersDataRootDirectory(),
                                       30));
      }
      OffLineIsolatedSegmentsClassifier.
          writeResultsForAllSetsBelowGivenDirectory(m_headerProperties);
      //restore original values
      m_headerProperties.setProperty(
          "OffLineIsolatedSegmentsClassifier.transcriptionsRootDirectory",
          directoryForDataLocators);
      m_headerProperties.setProperty(
          "OffLineIsolatedSegmentsClassifier.parametersRootDirectory",
          directoryForSetOfPatterns);
      m_headerProperties.setProperty(
          "OffLineIsolatedSegmentsClassifier.oisTraining",
          new Boolean(ooriginal).toString());
    }
    if (m_oshouldRunRecognitionForTestingData) {
      boolean ooriginal = m_headerProperties.
          getBooleanPropertyAndExitIfKeyNotFound(
          "OffLineIsolatedSegmentsClassifier.oisTraining");
      m_headerProperties.setProperty(
          "OffLineIsolatedSegmentsClassifier.oisTraining", "false");
      if (m_nverbose > 0) {
        Print.dialog("Testing using test data: " +
                     IO.getEndOfString(m_headerProperties.
                                       getPropertyAndExitIfKeyNotFound(
            "OffLineIsolatedSegmentsClassifier.parametersRootDirectory"), 30));
      }
      OffLineIsolatedSegmentsClassifier.
          writeResultsForAllSetsBelowGivenDirectory(m_headerProperties);
      m_headerProperties.setProperty(
          "OffLineIsolatedSegmentsClassifier.oisTraining",
          new Boolean(ooriginal).toString());
    }
  }

  private void createTableOfMonophoneLabelsForScoringIfNecessary() {
    String tableOfMonophoneLabelsForScoringFileName = m_headerProperties.
        getPropertyAndExitIfKeyNotFound(
        "OffLineIsolatedSegmentsClassifier.tableOfLabelsForScoring");
    if (!tableOfMonophoneLabelsForScoringFileName.equals("") &&
        new File(tableOfMonophoneLabelsForScoringFileName).exists()) {
      //do not need to write table
      return;
    }

    String rootDirectoryWithHMMs = m_simulationFilesAndDirectories.
        getGeneralOutputDirectory();
    tableOfMonophoneLabelsForScoringFileName = null;
    //Database.Type m_databaseType = Database.Type.getType(cMPropertiesSet.getPropertyAndExitIfKeyNotFound("Database.Type"));
    if (m_databaseType == Database.Type.TIMIT) {
      tableOfMonophoneLabelsForScoringFileName = rootDirectoryWithHMMs +
          TableOfLabels.Type.TIMIT39.toString() + "." +
          TableOfLabels.m_FILE_EXTENSION;
      if (!new File(tableOfMonophoneLabelsForScoringFileName).exists()) {
        TableOfLabels table = new TableOfLabels(TableOfLabels.Type.TIMIT39);
        table.writeToFile(tableOfMonophoneLabelsForScoringFileName);
      }
    }
    else if (m_databaseType == Database.Type.TIDIGITS) {
      tableOfMonophoneLabelsForScoringFileName = rootDirectoryWithHMMs +
          TableOfLabels.Type.TIDIGITS.toString() + "." +
          TableOfLabels.m_FILE_EXTENSION;
      if (!new File(tableOfMonophoneLabelsForScoringFileName).exists()) {
        TableOfLabels table = new TableOfLabels(TableOfLabels.Type.TIDIGITS);
        table.writeToFile(tableOfMonophoneLabelsForScoringFileName);
      }
    }
    else if (m_databaseType == Database.Type.SPOLTECHRAW) {
      tableOfMonophoneLabelsForScoringFileName = rootDirectoryWithHMMs +
          TableOfLabels.Type.SPOLTECH64.toString() + "." +
          TableOfLabels.m_FILE_EXTENSION;
      if (!new File(tableOfMonophoneLabelsForScoringFileName).exists()) {
        TableOfLabels table = new TableOfLabels(TableOfLabels.Type.SPOLTECH64);
        table.writeToFile(tableOfMonophoneLabelsForScoringFileName);
      }
    }
    else if (m_databaseType == Database.Type.DECTALK) {
        tableOfMonophoneLabelsForScoringFileName = rootDirectoryWithHMMs +
            TableOfLabels.Type.DECTALK.toString() + "." +
            TableOfLabels.m_FILE_EXTENSION;
        if (!new File(tableOfMonophoneLabelsForScoringFileName).exists()) {
          TableOfLabels table = new TableOfLabels(TableOfLabels.Type.DECTALK);
          table.writeToFile(tableOfMonophoneLabelsForScoringFileName);
        }
      }
    else if (m_databaseType == Database.Type.GENERAL) {
      tableOfMonophoneLabelsForScoringFileName = rootDirectoryWithHMMs +
          "tableForScoring." + TableOfLabels.m_FILE_EXTENSION;
      if (!new File(tableOfMonophoneLabelsForScoringFileName).exists()) {
        TableOfLabels tableOfLabels = new TableOfLabels(
            m_simulationFilesAndDirectories.getTrainSpeechDataRootDirectory());
        tableOfLabels.writeToFile(tableOfMonophoneLabelsForScoringFileName);
      }
    }
    else {
      End.throwError(m_databaseType.toString() + " not supported");
    }
    m_headerProperties.setProperty(
        "OffLineIsolatedSegmentsClassifier.tableOfLabelsForScoring",
        tableOfMonophoneLabelsForScoringFileName);
  }

  /**
   *  Gets the Results attribute of the TrainingManager object
   *
   *@param  setOfHMMs                Description of Parameter
   *@param  tableOfLabelsForScoring  Description of Parameter
   *@param  headerProperties         Description of Parameter
   *@param  oisTraining              Description of Parameter
   *@return                          The Results value
   */
//	private ClassificationStatisticsCalculator getResultsOLD(SetOfHMMs setOfHMMs, TableOfLabels tableOfLabelsForScoring, HeaderProperties headerProperties,
//			boolean oisTraining) {
//
//		if (tableOfLabelsForScoring == null) {
//			End.throwError("tableOfLabelsForScoring == null");
//		}
//
//		String propertyIdentifier = null;
//		String speechDirectory = null;
//		String parametersDirectory = null;
//		if (oisTraining) {
//			propertyIdentifier = "TrainFileName_";
//			speechDirectory = m_simulationFilesAndDirectories.getIsolatedMonophoneTranscriptionsOfTrainSpeechDataDirectory();
//			parametersDirectory = m_simulationFilesAndDirectories.getIsolatedTrainParametersDataRootDirectory();
//		}
//		else {
//			propertyIdentifier = "TestFileName_";
//			speechDirectory = m_simulationFilesAndDirectories.getIsolatedMonophoneTranscriptionsOfTestSpeechDataDirectory();
//			parametersDirectory = m_simulationFilesAndDirectories.getIsolatedTestParametersDataRootDirectory();
//		}
//
//		//uses the table for scoring
//		ClassificationStatisticsCalculator
//				classificationStatisticsCalculator = new ClassificationStatisticsCalculator(tableOfLabelsForScoring,"unknown_identifier");
//
//		int nnumberOfEntries = m_tableOfLabels.getNumberOfEntries();
//		for (int nentryNumber = 0; nentryNumber < nnumberOfEntries; nentryNumber++) {
//
//			String correctLabel = m_tableOfLabels.getFirstLabel(nentryNumber);
//
//			String fileName = m_simulationFilesAndDirectories.getPrefferedName(m_tableOfLabels, nentryNumber, SetOfPatterns.m_FILE_EXTENSION);
//			String absolutePathFileName = parametersDirectory + fileName;
//
//			//System.out.println("Reading SetOfPatterns " + patternsFileName + " for nentryNumber = " + nentryNumber + ".");
//			SetOfPatterns setOfPatterns = new SetOfPatterns(absolutePathFileName);
//			int nnumberOfPatterns = setOfPatterns.getNumberOfPatterns();
//
//			//get the associated DataLocator file
//			String dataLocatorFileName = m_simulationFilesAndDirectories.getPrefferedName(m_tableOfLabels, nentryNumber, DataLocator.m_FILE_EXTENSION);
//			dataLocatorFileName = FileNamesAndDirectories.getAbsolutePath(dataLocatorFileName, speechDirectory);
//
//			DatabaseManager databaseManager = new DatabaseManager(dataLocatorFileName);
//
//			//int[] nspeechData;
//			//for each Pattern in this SetOfPatterns file
//			int nnumberOfSegments = 0;
//			DataLocator dataLocator = null;
//			while (databaseManager.isThereDataToRead()) {
//				//get the Segment that generated this Pattern
//				//if ((dataLocator = databaseManager.getNextDataLocator()) != null) {
//				dataLocator = databaseManager.getNextDataLocator();
//				for (int i = 0; i < dataLocator.getNumberOfSegments(); i++) {
//
//					//if one wants to listen the Audio
//					//Audio audio = segments.getAudioFromGivenSegment(i);
//					//or take the whole sentence:
//					//Audio audio = segments.getAudioOfWholeSentence();
//					//AudioPlayer.playback(audio);
//					//in case one wants to listen the (amplitude scaled) segment
//					//AudioPlayer.playScaled(audio);
//					String segmentInfo = dataLocator.getGivenSegment(i);
//
//					//Print.dialog(segmentInfo);
//					//get the pattern
//					Pattern pattern = setOfPatterns.getPattern(nnumberOfSegments);
//
//					//find best HMM model
//					setOfHMMs.findBestModelAndItsScore(pattern);
//					int nbestModel = setOfHMMs.getBestModel();
//
//					//update statistics
//					String labelOfBestHMM = m_tableOfLabels.getFirstLabel(nbestModel);
//					classificationStatisticsCalculator.updateStatistics(correctLabel,
//							labelOfBestHMM,
//							segmentInfo);
//
//					nnumberOfSegments++;
//					if (nnumberOfSegments > nnumberOfPatterns) {
//						Print.error("nnumberOfSegments = " +
//								nnumberOfSegments + " and " +
//								"nnumberOfPatterns = " + nnumberOfPatterns);
//						End.exit();
//					}
//				}
//			}
//			databaseManager.finalizeDataReading();
//		}
//		return classificationStatisticsCalculator;
//	}


  /**
   *  Gets the SharedHMMPrototypesFromHTK attribute of the TrainingManager object
   */
  private void getSharedHMMPrototypesFromHTK() {
    String mappingOfLogicalIntoPhysicalTriphonesFileName =
        m_hTKFilesAndDirectories.
        getMappingOfLogicalIntoPhysicalTriphonesFileName();
    String sharedTriphonePrototypes = m_hTKFilesAndDirectories.
        getSentencesSharedTriphoneHMMPrototypesDirectory() + "hmms.txt";
    String outputJARFileName = m_simulationFilesAndDirectories.
        getSentencesSharedTriphoneHMMPrototypesDirectory();
    outputJARFileName += m_simulationFilesAndDirectories.getSetOfHMMsFileName();
    SetOfSharedContinuousHMMs.writeToJARFile(sharedTriphonePrototypes,
                                             mappingOfLogicalIntoPhysicalTriphonesFileName,
                                             m_patternGenerator,
                                             outputJARFileName);
  }

  //XXX just a work around
  /**
   *  Description of the Method
   */
  private void initializeHTKInformation() {
    String trainSpeechDataRootDirectory = m_simulationFilesAndDirectories.
        getTrainSpeechDataRootDirectory();
    String generalOutputDirectory = m_simulationFilesAndDirectories.
        getGeneralOutputDirectory();
    generalOutputDirectory = FileNamesAndDirectories.replacePartOfString(
        generalOutputDirectory, "mfcc", "htk_mfcc");
    if (generalOutputDirectory == null) {
      generalOutputDirectory += "htk/";
    }

    HeaderProperties headerProperties = new HeaderProperties();
    headerProperties.put("TrainingManager.TrainSpeechDataRootDirectory",
                         trainSpeechDataRootDirectory);
    headerProperties.put("TrainingManager.GeneralOutputDirectory",
                         generalOutputDirectory);
    headerProperties.put(
        "SimulationFilesAndDirectories.TranscriptionFileExtension", "txt");
    m_hTKFilesAndDirectories = new SimulationFilesAndDirectories(
        headerProperties);
  }

  /**
   *  Description of the Method
   */
  private void createPlainTriphonePrototypesAndReestimate(HMMReestimator
      hMMReestimator,
      String inputMonophoneHMMsJARFileName) {
    if (m_nverbose > 0) {
      Print.dialog("Creating plain triphone prototypes");
    }
    String sentenceTriphoneTranscriptionsSpeechDataFileName =
        m_simulationFilesAndDirectories.
        getSentenceTriphoneTranscriptionsOfTrainSpeechDataFileName();
    SetOfSharedHMMsInitializer setOfSharedHMMsInitializer = new
        SetOfSharedHMMsInitializer(
        sentenceTriphoneTranscriptionsSpeechDataFileName);
    //String inputMonophoneHMMsJARFileName = m_simulationFilesAndDirectories.getMostRecentMonophoneHMMFileName();
    //String outputDir = m_simulationFilesAndDirectories.getSentencesPlainTriphoneHMMPrototypesDirectory();
    //String outputTriphoneHMMsJARFileName = outputDir + m_simulationFilesAndDirectories.getSetOfHMMsFileName();
    //FileNamesAndDirectories.createDirectoriesIfNecessary(outputDir);

    //need to work with plain HMMs
    SetOfPlainContinuousHMMs monophoneHMMs = SetOfHMMsFile.getPlainHMMsFromFile(
        inputMonophoneHMMsJARFileName);
    SetOfPlainContinuousHMMs plainTriphoneHMMs = setOfSharedHMMsInitializer.
        cloneMonophones(monophoneHMMs);
    SetOfSharedContinuousHMMs triphoneHMMs = plainTriphoneHMMs.
        convertToSharedHMMs();
    plainTriphoneHMMs = null;

    //reestimate using minimum # of occurrences for updating model = 3
    String property = m_headerProperties.getProperty("SetOfSharedContinuousHMMReestimator.nminimumNumberOfLabelOccurrencesForUpdatingModel");
    int noriginalMinimumNumberOfLabelOccurrencesForUpdatingModel = 3;
    if (property != null) {
      noriginalMinimumNumberOfLabelOccurrencesForUpdatingModel = (Integer.
          valueOf(property)).intValue();
    }
    m_headerProperties.setProperty("SetOfSharedContinuousHMMReestimator.nminimumNumberOfLabelOccurrencesForUpdatingModel",
                                   "3");

    property = m_headerProperties.getProperty(
        "SetOfSharedContinuousHMMReestimator.flogPruningThreshold");
    float foriginalLogPruningThreshold = 2000;
    if (property != null) {
      foriginalLogPruningThreshold = (Float.valueOf(property)).floatValue();
    }
    //temporarilly turn of pruning of first (beta) pass
    m_headerProperties.setProperty(
        "SetOfSharedContinuousHMMReestimator.flogPruningThreshold", "2e8");

//				String initialHMMsJarFileName = m_simulationFilesAndDirectories.getSentencesPlainTriphoneHMMPrototypesDirectory() +
//						m_simulationFilesAndDirectories.getSetOfHMMsFileName();
//				String outputHMMSetDirectory = m_simulationFilesAndDirectories.getRootDirectoryForTriphoneEmbeddedTraining() + "plain/";

    String sentenceTrainParametersDataRootDirectory =
        m_simulationFilesAndDirectories.
        getSentenceTrainParametersDataRootDirectory();
    sentenceTrainParametersDataRootDirectory = FileNamesAndDirectories.
        replaceAndForceEndingWithSlash(sentenceTrainParametersDataRootDirectory);
    String trainingDataLocatorFileName = m_simulationFilesAndDirectories.
        getSentenceTriphoneTranscriptionsOfTrainSpeechDataFileName();
//		int nnumberOfGaussiansPerMixture = 1;
    int nnumberOfIterations = 4;
//				hMMReestimator.useEmbeddedBaumWelch(initialHMMsJarFileName,
//						nnumberOfGaussiansPerMixture, nnumberOfIterations, outputHMMSetDirectory,
//						trainingDataLocatorFileName, m_headerProperties, m_patternGenerator, sentenceTrainParametersDataRootDirectory);

    //m_simulationFilesAndDirectories.updateCurrentTriphoneHMMsDirectory(HMMReestimator.getDirectoryForEmbeddedIteration(outputHMMSetDirectory,nnumberOfGaussiansPerMixture,nnumberOfIterations-1));
    //copy HMM and occupation statistics file
    //m_simulationFilesAndDirectories.copyMostRecentTriphonesHMMsToFinalDirectoryForPlainTriphones();

    String outputHMMSetJARFileName = m_simulationFilesAndDirectories.
        getSentencesPlainTriphoneHMMsFinalDirectory() +
        m_simulationFilesAndDirectories.getSetOfHMMsFileName();

    for (int j = 0; j < nnumberOfIterations; j++) {
      triphoneHMMs = hMMReestimator.
          useOneIterationOfEmbeddedBaumWelchWithoutWritingFiles(triphoneHMMs,
          trainingDataLocatorFileName,
          outputHMMSetJARFileName,
          m_headerProperties,
          m_patternGenerator,
          sentenceTrainParametersDataRootDirectory);
    }

    //save as plain HMMs
    plainTriphoneHMMs = triphoneHMMs.convertToPlainHMMs();
    plainTriphoneHMMs.writeToJARFile(outputHMMSetJARFileName);

//		//save occupation statistics file
//		String target = m_simulationFilesAndDirectories.getSentencesPlainTriphoneHMMsFinalDirectory();
//		FileNamesAndDirectories.createDirectoriesIfNecessary(target);
//		//IO.copyFile(source + "hmms.jar", target + "hmms.jar");
//		SetOfHMMs setOfHMMs = SetOfHMMsFile.read(source + m_setOfHMMsFileName);
//		if (setOfHMMs instanceof SetOfSharedContinuousHMMs) {
//			//convert it
//			setOfHMMs = ( (SetOfSharedContinuousHMMs) setOfHMMs).convertToPlainHMMs();
//		}
//		SetOfPlainContinuousHMMs setOfPlainContinuousHMMs = (SetOfPlainContinuousHMMs) setOfHMMs;
//		setOfPlainContinuousHMMs.writeToJARFile(target + m_setOfHMMsFileName);
//		//IO.copyFile(source + m_setOfHMMsFileName, target + m_setOfHMMsFileName);
//		String statsFileName = "OccupationStatistics.txt";

    //restore original value
    m_headerProperties.setProperty("SetOfSharedContinuousHMMReestimator.nminimumNumberOfLabelOccurrencesForUpdatingModel",
                                   Integer.toString(
        noriginalMinimumNumberOfLabelOccurrencesForUpdatingModel));
    //need a bigger pruning threshold for triphones
    if (foriginalLogPruningThreshold < 2e7F) {
      foriginalLogPruningThreshold = 2e7F;
    }
    m_headerProperties.setProperty(
        "SetOfSharedContinuousHMMReestimator.flogPruningThreshold",
        Float.toString(foriginalLogPruningThreshold));
  }

  /**
   *  Description of the Method
   */
  private void runHTKToolsIfNecessary() {
    if (m_oshouldRunRecognitionForTestingData ||
        m_oshouldRunRecognitionForTrainingData ||
        m_oshouldCreateTriphoneModels) {
      runHTKTools();
    }
  }

  private void runHMMTrainingForIsolatedSegments() {
    HMMReestimator hMMReestimator = new HMMReestimator(m_headerProperties);
    //train HMMs with isolated SOPs
    if (m_oshouldCreateMonophoneHMMPrototypes) {
      String hmmSetOutputFileName = m_simulationFilesAndDirectories.
          getIsolatedMonophoneHMMPrototypesDirectory()
          + m_simulationFilesAndDirectories.getSetOfHMMsFileName();
      if (hMMReestimator.isNecessaryToCreateHMMSet(hmmSetOutputFileName)) {
        createMonophoneHMMPrototypes();
      }
      else {
        Print.dialog("Skipping because " +
                     IO.getEndOfString(hmmSetOutputFileName, 45) +
                     " already exists");
      }
    }
    m_simulationFilesAndDirectories.updateCurrentMonophoneHMMsDirectory(
        m_simulationFilesAndDirectories.
        getIsolatedMonophoneHMMPrototypesDirectory());

    if (
        m_oshouldRecursivelyCreateMonophoneHMMWithSegmentalKMeansForIsolatedSegments) {
      String initialHMMsJarFileName = m_simulationFilesAndDirectories.
          getMostRecentMonophoneHMMFileName();
      SetOfPlainContinuousHMMs setOfPlainContinuousHMMs = new
          SetOfPlainContinuousHMMs(initialHMMsJarFileName);
      //initialHMMsJarFileName = FileNamesAndDirectories.substituteExtension(initialHMMsJarFileName,"jar");
      String directoryWithSOPFiles = m_simulationFilesAndDirectories.
          getIsolatedTrainParametersDataRootDirectory();
      String rootOutputDirectoryForHMMs = m_simulationFilesAndDirectories.
          getIsolatedMonophoneHMMsKMeansViterbiDirectory();
      //int ninitialNumberOfGaussiansPerMixture = 5;
      int ninitialNumberOfGaussiansPerMixture = m_headerProperties.
          getIntegerPropertyAndExitIfKeyNotFound(
          "TrainingManager.nsegmentalKmeansInitialNumberOfGaussians");
      //int nincrementOfGaussiansPerMixture = 5;
      int nincrementOfGaussiansPerMixture = m_headerProperties.
          getIntegerPropertyAndExitIfKeyNotFound(
          "TrainingManager.nsegmentalKmeansGaussiansIncrement");
      int nfinalNumberOfGaussiansPerMixture =
          m_nmaximumNumberOfGaussiansPerMixture;
      //double dthresholdToStopIterations = 1e-2;
      double dthresholdToStopIterations = m_headerProperties.
          getDoublePropertyAndExitIfKeyNotFound(
          "TrainingManager.dsegmentalKmeansThresholdToStopIterations");
      boolean oshouldUseBaumWelchAfterwards = m_headerProperties.
          getBooleanPropertyAndExitIfKeyNotFound(
          "TrainingManager.osegmentalKmeansUseBaumWelchAfterwards");
      hMMReestimator.useSegmentalKMeansRecursively(setOfPlainContinuousHMMs,
          ninitialNumberOfGaussiansPerMixture,
          nincrementOfGaussiansPerMixture,
          nfinalNumberOfGaussiansPerMixture,
          rootOutputDirectoryForHMMs,
          directoryWithSOPFiles,
          dthresholdToStopIterations,
          oshouldUseBaumWelchAfterwards);
    }

    if (m_oshouldCreateMonophoneHMMWithBaumWelchForIsolatedSegments) {
      String initialHMMsJarFileName = m_simulationFilesAndDirectories.
          getMostRecentMonophoneHMMFileName();
      //initialHMMsJarFileName = FileNamesAndDirectories.substituteExtension(initialHMMsJarFileName,"jar");
      String directoryWithSOPFiles = m_simulationFilesAndDirectories.
          getIsolatedTrainParametersDataRootDirectory();
      String outputHMMSetDirectory = m_simulationFilesAndDirectories.
          getIsolatedMonophoneHMMsBaumWelchDirectory() + "1/";
      String outputHMMSetFileName = outputHMMSetDirectory +
          m_simulationFilesAndDirectories.getSetOfHMMsFileName();
      hMMReestimator.useBaumWelch(initialHMMsJarFileName, directoryWithSOPFiles,
                                  outputHMMSetFileName);
      m_simulationFilesAndDirectories.updateCurrentMonophoneHMMsDirectory(
          outputHMMSetDirectory);

      //do not need if only isolated segments
      if (m_databaseType == Database.Type.TIMIT) {
        //wants to declare this the final monophones for isolated segments
        m_simulationFilesAndDirectories.
            copyMostRecentIsolatedMonophonesHMMsToFinalDirectory();
      }
    }
    if (m_databaseType == Database.Type.TIMIT) {
      m_simulationFilesAndDirectories.updateCurrentMonophoneHMMsDirectory(
          m_simulationFilesAndDirectories.
          getIsolatedMonophoneHMMsFinalDirectory());
    }

    if (m_oshouldRecursivelyCreateMonophoneHMMWithBaumWelchForIsolatedSegments) {
      //recursively increase number of Gaussians and reestimate
      //do not update "most recent monophone" (keep the one with 1 Gaussian as the most recent)
      int nnumberOfIncrementsInGaussiansPerMixture =
          m_nmaximumNumberOfGaussiansPerMixture;
      int ninitialNumberOfGaussiansPerMixture = 2;
      hMMReestimator.useBaumWelchRecursively(m_simulationFilesAndDirectories.
                                             getMostRecentMonophoneHMMFileName(),
                                             nnumberOfIncrementsInGaussiansPerMixture,
                                             ninitialNumberOfGaussiansPerMixture,
                                             m_simulationFilesAndDirectories.
          getIsolatedMonophoneHMMsBaumWelchDirectory(),
                                             m_simulationFilesAndDirectories.
                                             getIsolatedTrainParametersDataRootDirectory());
    }
  }

  /**
   *  Description of the Method
   */
  private void doFileOrientedTraining() {
    if (m_oisRunning && m_oshouldCreateTranscriptionsForSpeechFiles) {
      createFileOrientedWAVDataLocatorsFileForMonophoneIsolatedSegments(false);
      createFileOrientedWAVDataLocatorsFileForMonophoneIsolatedSegments(true);
      writePropertiesToFileIfRequiredByUser();
    }

    if (m_oisRunning && m_oshouldConvertSpeechDataIntoParameters) {
      createSetOfPatternsFilesForIsolatedSegments(true);
      createSetOfPatternsFilesForIsolatedSegments(false);
      writePropertiesToFileIfRequiredByUser();
    }

    if (m_oisRunning && m_oshouldCreateHMMFiles) {

      runHMMTrainingForIsolatedSegments();

//			HMMReestimator hMMReestimator = new HMMReestimator(m_headerProperties);
//			//train HMMs with isolated SOPs
//			if (m_oshouldCreateMonophoneHMMPrototypes) {
//				String hmmSetOutputFileName = m_simulationFilesAndDirectories.getIsolatedMonophoneHMMPrototypesDirectory()
//					   + m_simulationFilesAndDirectories.getSetOfHMMsFileName();
//				if (hMMReestimator.isNecessaryToCreateHMMSet(hmmSetOutputFileName)) {
//				   createMonophoneHMMPrototypes();
//				} else {
//					Print.dialog("Skipping because " + hmmSetOutputFileName + " already exists.");
//				}
//			}
//			m_simulationFilesAndDirectories.updateCurrentMonophoneHMMsDirectory(m_simulationFilesAndDirectories.getIsolatedMonophoneHMMPrototypesDirectory());
//
//			if (m_oshouldCreateMonophoneHMMWithBaumWelchForIsolatedSegments) {
//				String initialHMMsJarFileName = m_simulationFilesAndDirectories.getMostRecentMonophoneHMMFileName();
//				//initialHMMsJarFileName = FileNamesAndDirectories.substituteExtension(initialHMMsJarFileName,"jar");
//				String directoryWithSOPFiles = m_simulationFilesAndDirectories.getIsolatedTrainParametersDataRootDirectory();
//				String outputHMMSetDirectory = m_simulationFilesAndDirectories.getIsolatedMonophoneHMMsBaumWelchDirectory() + "1/";
//				String outputHMMSetFileName = outputHMMSetDirectory + m_simulationFilesAndDirectories.getSetOfHMMsFileName();
//				hMMReestimator.useBaumWelch(initialHMMsJarFileName, directoryWithSOPFiles, outputHMMSetFileName);
//				m_simulationFilesAndDirectories.updateCurrentMonophoneHMMsDirectory(outputHMMSetDirectory);
//
//				//wants to declare this the final monophones for isolated segments
//				m_simulationFilesAndDirectories.copyMostRecentIsolatedMonophonesHMMsToFinalDirectory();
//			}
//			m_simulationFilesAndDirectories.updateCurrentMonophoneHMMsDirectory(m_simulationFilesAndDirectories.getIsolatedMonophoneHMMsFinalDirectory());
//
//			if (m_oshouldRecursivelyCreateMonophoneHMMWithBaumWelchForIsolatedSegments) {
//				//recursively increase number of Gaussians and reestimate
//				//do not update "most recent monophone" (keep the one with 1 Gaussian as the most recent)
//				int nnumberOfIncrementsInGaussiansPerMixture = m_nmaximumNumberOfGaussiansPerMixture;
//				int ninitialNumberOfGaussiansPerMixture = 2;
//				hMMReestimator.useBaumWelchRecursively(m_simulationFilesAndDirectories.getMostRecentMonophoneHMMFileName(),
//						nnumberOfIncrementsInGaussiansPerMixture, ninitialNumberOfGaussiansPerMixture, m_simulationFilesAndDirectories.getIsolatedMonophoneHMMsBaumWelchDirectory(), m_simulationFilesAndDirectories.getIsolatedTrainParametersDataRootDirectory());
//			}
//			writePropertiesToFileIfRequiredByUser();
    }
  }

  /**
   *  Generate parameters for sentences and then cut them. This avoids problems
   *  with derivatives, allows using CMS (cepstral mean subtraction) to the whole
   *  sequence, etc.
   */
  private void doTIMITTraining() {

    //PART I - Data preparation
    if (m_oisRunning && m_oshouldCopyDatabaseToUniqueDirectory) {
      copyTIMITDataToUniqueDirectoryWithNoSASentenceAndOnlyCoreTestSet();
      //ak: maybe I should create an option to turn the creation of a validation set
      //(below) on or off, but now I'm simply commenting lines out
      //also, I'm copying using the unique name, while I think the method below
      //can optionally keep the directory structure.
      copyTIMITDataValidationSetUsingUniqueName();
    }

    if (m_oisRunning && m_oshouldCreateTranscriptionsForSpeechFiles) {
      //write speech DataLocators for SENTENCES for both train and test data
      //will include all WAV files (exception are SA sentences), not only core test set
      createWAVDataLocatorsFileForMonophoneSentences(false,"wav");
      createWAVDataLocatorsFileForMonophoneSentences(true,"wav");
    }

    if (m_oisRunning && m_oshouldConvertSpeechDataIntoParameters) {
      if (m_oshouldConvertSentenceSpeechDataIntoParameters) {
        createSetOfPatternsFilesForSentences(false);
        createSetOfPatternsFilesForSentences(true);
        if (m_oshouldCreatFileWithDynamicRanges) {
          Print.dialog("Creating file with dynamic ranges");
          String rootDir = m_simulationFilesAndDirectories.
              getSentenceTrainParametersDataRootDirectory();
          rootDir = FileNamesAndDirectories.getParent(rootDir);
          SetOfPatterns.createFileWithDynamicRanges(rootDir);
        }
      }
    }

    if (m_oisRunning && m_oshouldCreateTranscriptionsForSpeechFiles) {
      //write speech DataLocators for ISOLATED_SEGMENTS for both train and test data
      createWAVDataLocatorsFileForMonophoneIsolatedSegments(false);
      createWAVDataLocatorsFileForMonophoneIsolatedSegments(true);
      writePropertiesToFileIfRequiredByUser();
      //cross-word triphones
      if (m_oshouldCreateTriphoneTranscriptions) {
        createWAVDataLocatorsFileForCrossWordTriphoneSentences(false);
        createWAVDataLocatorsFileForCrossWordTriphoneSentences(true);
        writePropertiesToFileIfRequiredByUser();
      }
    }

    if (m_oisRunning && m_oshouldConvertSpeechDataIntoParameters) {
      if (m_oshouldConvertIsolatedSpeechDataIntoParameters) {
        //"cut" SOP for SEGMENT's generating ISOLATED_SEGMENTS SOP's
        cutSetOfPatternsFilesForSentencesAndWriteIsolatedSegmentsSOPs(false);
        cutSetOfPatternsFilesForSentencesAndWriteIsolatedSegmentsSOPs(true);
      }
      writePropertiesToFileIfRequiredByUser();
    }

    //PART II - HMM training
    if (m_oisRunning && m_oshouldCreateHMMFiles) {

      runHMMTrainingForIsolatedSegments();

      HMMReestimator hMMReestimator = new HMMReestimator(m_headerProperties);

      if (m_oshouldCreateMonophoneHMMWithEmbeddedBaumWelch) {
        String initialHMMsJarFileName = m_simulationFilesAndDirectories.
            getMostRecentMonophoneHMMFileName();
        if (! (new File(initialHMMsJarFileName).exists())) {
          //if there is no HMM to start with, use prototypes
          initialHMMsJarFileName = m_simulationFilesAndDirectories.
              getIsolatedMonophoneHMMPrototypesDirectory()
              + m_simulationFilesAndDirectories.getSetOfHMMsFileName();
          if (! (new File(initialHMMsJarFileName).exists())) {
            End.throwError(
                "Could not find HMM set to initialize embedded Baum-Welch");
          }
        }

        String outputHMMSetDirectory = m_simulationFilesAndDirectories.
            getRootDirectoryForMonophoneEmbeddedTraining();
        //String outputHMMSetFileName = outputHMMSetDirectory + m_simulationFilesAndDirectories.getSetOfHMMsFileName();
        String sentenceTrainParametersDataRootDirectory =
            m_simulationFilesAndDirectories.
            getSentenceTrainParametersDataRootDirectory();
        String trainingDataLocatorFileName = m_simulationFilesAndDirectories.
            getSentenceMonophoneTranscriptionsOfTrainSpeechDataFileName();
        int nnumberOfGaussiansPerMixture = 1;
        //int nnumberOfIterations = 4;
        int nnumberOfIterations = m_headerProperties.
            getIntegerPropertyAndExitIfKeyNotFound(
            "TrainingManager.nembeddedBaumNumberOfIterations");
        hMMReestimator.useEmbeddedBaumWelch(initialHMMsJarFileName,
                                            nnumberOfGaussiansPerMixture,
                                            nnumberOfIterations,
                                            outputHMMSetDirectory,
                                            trainingDataLocatorFileName,
                                            m_headerProperties,
                                            m_patternGenerator,
                                            sentenceTrainParametersDataRootDirectory);

        m_simulationFilesAndDirectories.updateCurrentMonophoneHMMsDirectory(
            HMMReestimator.getDirectoryForEmbeddedIteration(
            outputHMMSetDirectory, nnumberOfGaussiansPerMixture,
            nnumberOfIterations - 1));
        //wants to declare this the final monophones for sentences
        m_simulationFilesAndDirectories.
            copyMostRecentSentencesMonophonesHMMsToFinalDirectory();
      }
      //if want to use isolated monophones comment out line below
      m_simulationFilesAndDirectories.updateCurrentMonophoneHMMsDirectory(
          m_simulationFilesAndDirectories.
          getSentencesMonophoneHMMsFinalDirectory());

      if (m_oshouldRecursivelyCreateMonophoneHMMWithEmbeddedBaumWelch) {
        //recursively increase number of Gaussians and reestimate using embedded Baum-Welch
        //do not update "most recent monophone" (keep the one with 1 Gaussian as the most recent)
        String initialHMMsJarFileName = m_simulationFilesAndDirectories.
            getMostRecentMonophoneHMMFileName();
        String outputHMMSetDirectory = m_simulationFilesAndDirectories.
            getRootDirectoryForMonophoneEmbeddedTraining();
        //String outputHMMSetFileName = outputHMMSetDirectory + m_simulationFilesAndDirectories.getSetOfHMMsFileName();
        int nnumberOfIncrementsInGaussiansPerMixture =
            m_nmaximumNumberOfGaussiansPerMixture;
        int ninitialNumberOfGaussiansPerMixture = 2;
        int nnumberOfIterations = m_headerProperties.
            getIntegerPropertyAndExitIfKeyNotFound(
            "TrainingManager.nembeddedBaumNumberOfIterations");
        //hMMReestimator.useBaumWelch(initialHMMsJarFileName,directoryWithSOPFiles,outputHMMSetFileName);
        //m_simulationFilesAndDirectories.updateCurrentMonophoneHMMsDirectory(outputHMMSetDirectory);
        String sentenceTrainParametersDataRootDirectory =
            m_simulationFilesAndDirectories.
            getSentenceTrainParametersDataRootDirectory();
        String trainingDataLocatorFileName = m_simulationFilesAndDirectories.
            getSentenceMonophoneTranscriptionsOfTrainSpeechDataFileName();

        //Print.dialog("initialHMMsJarFileName: " + initialHMMsJarFileName);

        hMMReestimator.useEmbeddedBaumWelchRecursively(initialHMMsJarFileName,
            nnumberOfIncrementsInGaussiansPerMixture,
            ninitialNumberOfGaussiansPerMixture, nnumberOfIterations,
            outputHMMSetDirectory, trainingDataLocatorFileName,
            m_headerProperties, m_patternGenerator,
            sentenceTrainParametersDataRootDirectory);
      }

      //start working to create cross-word shared triphones
      //create plain triphones by cloning most recent monophones
      if (m_oshouldCloneMonophonesAndReestimatePlainTriphoneHMHs) {
        String inputMonophoneHMMsJARFileName = m_simulationFilesAndDirectories.
            getMostRecentMonophoneHMMFileName();
        createPlainTriphonePrototypesAndReestimate(hMMReestimator,
            inputMonophoneHMMsJARFileName);
      }

//			if (m_oshouldReestimatePlainTriphoneHMMWithEmbeddedBaumWelch) {
//				String initialHMMsJarFileName = m_simulationFilesAndDirectories.getSentencesPlainTriphoneHMMPrototypesDirectory() +
//						m_simulationFilesAndDirectories.getSetOfHMMsFileName();
//				String outputHMMSetDirectory = m_simulationFilesAndDirectories.getRootDirectoryForTriphoneEmbeddedTraining() + "plain/";
//				String sentenceTrainParametersDataRootDirectory = m_simulationFilesAndDirectories.getSentenceTrainParametersDataRootDirectory();
//				String trainingDataLocatorFileName = m_simulationFilesAndDirectories.getSentenceTriphoneTranscriptionsOfTrainSpeechDataFileName();
//
//				int nnumberOfGaussiansPerMixture = 1;
//				int nnumberOfIterations = 2;
//				hMMReestimator.useEmbeddedBaumWelch(initialHMMsJarFileName,
//						nnumberOfGaussiansPerMixture, nnumberOfIterations, outputHMMSetDirectory,
//						trainingDataLocatorFileName, m_headerProperties, m_patternGenerator, sentenceTrainParametersDataRootDirectory);
//
//				m_simulationFilesAndDirectories.updateCurrentTriphoneHMMsDirectory(HMMReestimator.getDirectoryForEmbeddedIteration(outputHMMSetDirectory,nnumberOfGaussiansPerMixture,nnumberOfIterations-1));
//				//copy HMM and occupation statistics file
//				m_simulationFilesAndDirectories.copyMostRecentTriphonesHMMsToFinalDirectoryForPlainTriphones();
//			}
//
//			//ak
//			m_simulationFilesAndDirectories.updateCurrentTriphoneHMMsDirectory("C:/simulations_timit/mfcc_e_d_a/hmms/triphones/sentences/plain/embedded_1G_0it/");
//			//copy HMM and occupation statistics file
//			m_simulationFilesAndDirectories.copyMostRecentTriphonesHMMsToFinalDirectoryForPlainTriphones();

      m_simulationFilesAndDirectories.updateCurrentTriphoneHMMsDirectory(
          m_simulationFilesAndDirectories.
          getSentencesPlainTriphoneHMMsFinalDirectory());

      //cluster plain triphones
      if (m_oshouldClusterPlainTriphones) {
        String listOfAllLabels = m_simulationFilesAndDirectories.
            getListOfAllPossibleCrossWordTriphones();
        createListOfAllPossibleCrossWordTriphones(listOfAllLabels);

        //split potentially big JAR file, creating 1 JAR file per central phone
        String outputDirectoryForSplitHMMSets = m_simulationFilesAndDirectories.
            getGeneralOutputDirectory() + "temporary/";
        String inputSetOfPlainHMMsJARFileName = m_simulationFilesAndDirectories.
            getMostRecentTriphoneHMMFileName();

        FileNamesAndDirectories.createDirectoriesIfNecessary(
            outputDirectoryForSplitHMMSets);
        TreeBasedClustering.splitSetBasedOnCentralPhone(
            inputSetOfPlainHMMsJARFileName,
            m_tableOfLabels,
            outputDirectoryForSplitHMMSets);

        TableOfLabels allTriphonesTable = SetOfHMMsFile.getTableOfLabels(
            inputSetOfPlainHMMsJARFileName);

        double doccupationThreshold_RO = 100;
        double dstoppingCriterionThreshold_TB = 350;

        String inputOccupationStatisticsFileName = FileNamesAndDirectories.
            getPathFromFileName(m_simulationFilesAndDirectories.
                                getMostRecentTriphoneHMMFileName()) +
            "OccupationStatistics.txt";
        SetOfSharedContinuousHMMs setOfSharedContinuousHMMs =
            TreeBasedClustering.buildPhoneticTreeAndTieStates(
            inputOccupationStatisticsFileName,
            allTriphonesTable,
            m_tableOfLabels,
            outputDirectoryForSplitHMMSets,
            doccupationThreshold_RO,
            dstoppingCriterionThreshold_TB,
            m_patternGenerator,
            new TIMITPhoneticClasses(),
            //new DebugPhoneticClasses(),
            listOfAllLabels);
        String outputJARFileName = m_simulationFilesAndDirectories.
            getSentencesSharedTriphoneHMMPrototypesDirectory() +
            m_simulationFilesAndDirectories.getSetOfHMMsJARFileName();
        setOfSharedContinuousHMMs.writeHTKAndJARFiles(outputJARFileName);
        if (m_nverbose > 0) {
          Print.dialog("Writing set of shared HMMs after clustering " +
                       outputJARFileName);
        }
      }
      m_simulationFilesAndDirectories.updateCurrentTriphoneHMMsDirectory(
          m_simulationFilesAndDirectories.
          getSentencesSharedTriphoneHMMPrototypesDirectory());

      if (m_oshouldCreateCrossWordTriphoneHMMWithEmbeddedBaumWelch) {
        //getSharedHMMPrototypesFromHTK();
        //m_simulationFilesAndDirectories.updateCurrentTriphoneHMMsDirectory(m_simulationFilesAndDirectories.getSentencesSharedTriphoneHMMPrototypesDirectory());
        String initialHMMsJarFileName = m_simulationFilesAndDirectories.
            getMostRecentTriphoneHMMFileName();
        String outputHMMSetDirectory = m_simulationFilesAndDirectories.
            getRootDirectoryForSharedTriphoneEmbeddedTraining();
        //String outputHMMSetFileName = outputHMMSetDirectory + m_simulationFilesAndDirectories.getSetOfHMMsFileName();
        String sentenceTrainParametersDataRootDirectory =
            m_simulationFilesAndDirectories.
            getSentenceTrainParametersDataRootDirectory();
        String trainingDataLocatorFileName = m_simulationFilesAndDirectories.
            getSentenceTriphoneTranscriptionsOfTrainSpeechDataFileName();
        int nnumberOfGaussiansPerMixture = 1;
        int nnumberOfIterations = 4;
        hMMReestimator.useEmbeddedBaumWelch(initialHMMsJarFileName,
                                            nnumberOfGaussiansPerMixture,
                                            nnumberOfIterations,
                                            outputHMMSetDirectory,
                                            trainingDataLocatorFileName,
                                            m_headerProperties,
                                            m_patternGenerator,
                                            sentenceTrainParametersDataRootDirectory);
        m_simulationFilesAndDirectories.updateCurrentTriphoneHMMsDirectory(
            HMMReestimator.getDirectoryForEmbeddedIteration(
            outputHMMSetDirectory, nnumberOfGaussiansPerMixture,
            nnumberOfIterations - 1));
        //wants to declare this (1 Gaussian) the final triphones for sentences?
        m_simulationFilesAndDirectories.
            copyMostRecentSharedTriphonesHMMsToFinalDirectory();
      }

      //increase Gaussians of shared triphones
      if (m_oshouldRecursivelyCreateTriphoneHMMWithEmbeddedBaumWelch) {
        //recursively increase number of Gaussians and reestimate using embedded Baum-Welch
        //do not update "most recent monophone" (keep the one with 1 Gaussian as the most recent)
        String initialHMMsJarFileName = m_simulationFilesAndDirectories.
            getMostRecentTriphoneHMMFileName();
        String outputHMMSetDirectory = m_simulationFilesAndDirectories.
            getRootDirectoryForSharedTriphoneEmbeddedTraining();
        //String outputHMMSetFileName = outputHMMSetDirectory + m_simulationFilesAndDirectories.getSetOfHMMsFileName();
        int nnumberOfIncrementsInGaussiansPerMixture = 6;
        int ninitialNumberOfGaussiansPerMixture = 2;
        int nnumberOfIterations = 4;
        //hMMReestimator.useBaumWelch(initialHMMsJarFileName,directoryWithSOPFiles,outputHMMSetFileName);
        //m_simulationFilesAndDirectories.updateCurrentMonophoneHMMsDirectory(outputHMMSetDirectory);
        String sentenceTrainParametersDataRootDirectory =
            m_simulationFilesAndDirectories.
            getSentenceTrainParametersDataRootDirectory();
        String trainingDataLocatorFileName = m_simulationFilesAndDirectories.
            getSentenceTriphoneTranscriptionsOfTrainSpeechDataFileName();
        hMMReestimator.useEmbeddedBaumWelchRecursively(initialHMMsJarFileName,
            nnumberOfIncrementsInGaussiansPerMixture,
            ninitialNumberOfGaussiansPerMixture, nnumberOfIterations,
            outputHMMSetDirectory, trainingDataLocatorFileName,
            m_headerProperties, m_patternGenerator,
            sentenceTrainParametersDataRootDirectory);
      }
    }

    if (m_oisRunning &&
        (m_oshouldRunRecognitionForTestingData ||
         m_oshouldRunRecognitionForTrainingData)) {
      //runRecognitionTestForMonophonesIfRequestedByUser();
      //ak
      //runClassificationTest(m_simulationFilesAndDirectories.getFinalSetOfMonophoneHMMs(),false,false,m_simulationFilesAndDirectories.getFinalDirectoryForMonophoneIsolatedHMMs);
      //runClassificationTest(m_simulationFilesAndDirectories.getFinalSetOfMonophoneHMMs(),false,true,m_simulationFilesAndDirectories.getFinalDirectoryForMonophoneIsolatedHMMs);
    }
  }

  /**
   *  Generate parameters for sentences and then cut them. This avoids problems
   *  with derivatives, allows using CMS (cepstral mean subtraction) to the whole
   *  sequence, etc.
   */
  private void doSPOLTECHRAWTraining() {

    //PART I - Data preparation
    if (m_oisRunning && m_oshouldCreateTranscriptionsForSpeechFiles) {
      //write speech DataLocators for SENTENCES for both train and test data
      //will include all WAV files (exception are SA sentences), not only core test set
      createWAVDataLocatorsFileForMonophoneSentences(false,"raw");
      createWAVDataLocatorsFileForMonophoneSentences(true,"raw");
    }

    if (m_oisRunning && m_oshouldConvertSpeechDataIntoParameters) {
      if (m_oshouldConvertSentenceSpeechDataIntoParameters) {
        createSetOfPatternsFilesForSentences(false);
        createSetOfPatternsFilesForSentences(true);
        if (m_oshouldCreatFileWithDynamicRanges) {
          Print.dialog("Creating file with dynamic ranges");
          String rootDir = m_simulationFilesAndDirectories.
              getSentenceTrainParametersDataRootDirectory();
          rootDir = FileNamesAndDirectories.getParent(rootDir);
          SetOfPatterns.createFileWithDynamicRanges(rootDir);
        }
      }
    }

    if (m_oisRunning && m_oshouldCreateTranscriptionsForSpeechFiles) {
      //write speech DataLocators for ISOLATED_SEGMENTS for both train and test data
      createWAVDataLocatorsFileForMonophoneIsolatedSegments(false);
      createWAVDataLocatorsFileForMonophoneIsolatedSegments(true);
      writePropertiesToFileIfRequiredByUser();
      //cross-word triphones
      if (m_oshouldCreateTriphoneTranscriptions) {
        createWAVDataLocatorsFileForCrossWordTriphoneSentences(false);
        createWAVDataLocatorsFileForCrossWordTriphoneSentences(true);
        writePropertiesToFileIfRequiredByUser();
      }
    }

    if (m_oisRunning && m_oshouldConvertSpeechDataIntoParameters) {
      if (m_oshouldConvertIsolatedSpeechDataIntoParameters) {
        //"cut" SOP for SEGMENT's generating ISOLATED_SEGMENTS SOP's
        cutSetOfPatternsFilesForSentencesAndWriteIsolatedSegmentsSOPs(false);
        cutSetOfPatternsFilesForSentencesAndWriteIsolatedSegmentsSOPs(true);
      }
      writePropertiesToFileIfRequiredByUser();
    }

    //PART II - HMM training
    if (m_oisRunning && m_oshouldCreateHMMFiles) {

      runHMMTrainingForIsolatedSegments();

      HMMReestimator hMMReestimator = new HMMReestimator(m_headerProperties);

      if (m_oshouldCreateMonophoneHMMWithEmbeddedBaumWelch) {
        String initialHMMsJarFileName = m_simulationFilesAndDirectories.
            getMostRecentMonophoneHMMFileName();
        if (! (new File(initialHMMsJarFileName).exists())) {
          //if there is no HMM to start with, use prototypes
          initialHMMsJarFileName = m_simulationFilesAndDirectories.
              getIsolatedMonophoneHMMPrototypesDirectory()
              + m_simulationFilesAndDirectories.getSetOfHMMsFileName();
          if (! (new File(initialHMMsJarFileName).exists())) {
            End.throwError(
                "Could not find HMM set to initialize embedded Baum-Welch");
          }
        }

        String outputHMMSetDirectory = m_simulationFilesAndDirectories.
            getRootDirectoryForMonophoneEmbeddedTraining();
        //String outputHMMSetFileName = outputHMMSetDirectory + m_simulationFilesAndDirectories.getSetOfHMMsFileName();
        String sentenceTrainParametersDataRootDirectory =
            m_simulationFilesAndDirectories.
            getSentenceTrainParametersDataRootDirectory();
        String trainingDataLocatorFileName = m_simulationFilesAndDirectories.
            getSentenceMonophoneTranscriptionsOfTrainSpeechDataFileName();
        int nnumberOfGaussiansPerMixture = 1;
        //int nnumberOfIterations = 4;
        int nnumberOfIterations = m_headerProperties.
            getIntegerPropertyAndExitIfKeyNotFound(
            "TrainingManager.nembeddedBaumNumberOfIterations");
        hMMReestimator.useEmbeddedBaumWelch(initialHMMsJarFileName,
                                            nnumberOfGaussiansPerMixture,
                                            nnumberOfIterations,
                                            outputHMMSetDirectory,
                                            trainingDataLocatorFileName,
                                            m_headerProperties,
                                            m_patternGenerator,
                                            sentenceTrainParametersDataRootDirectory);

        m_simulationFilesAndDirectories.updateCurrentMonophoneHMMsDirectory(
            HMMReestimator.getDirectoryForEmbeddedIteration(
            outputHMMSetDirectory, nnumberOfGaussiansPerMixture,
            nnumberOfIterations - 1));
        //wants to declare this the final monophones for sentences
        m_simulationFilesAndDirectories.
            copyMostRecentSentencesMonophonesHMMsToFinalDirectory();
      }
      //if want to use isolated monophones comment out line below
      m_simulationFilesAndDirectories.updateCurrentMonophoneHMMsDirectory(
          m_simulationFilesAndDirectories.
          getSentencesMonophoneHMMsFinalDirectory());

      if (m_oshouldRecursivelyCreateMonophoneHMMWithEmbeddedBaumWelch) {
        //recursively increase number of Gaussians and reestimate using embedded Baum-Welch
        //do not update "most recent monophone" (keep the one with 1 Gaussian as the most recent)
        String initialHMMsJarFileName = m_simulationFilesAndDirectories.
            getMostRecentMonophoneHMMFileName();
        String outputHMMSetDirectory = m_simulationFilesAndDirectories.
            getRootDirectoryForMonophoneEmbeddedTraining();
        //String outputHMMSetFileName = outputHMMSetDirectory + m_simulationFilesAndDirectories.getSetOfHMMsFileName();
        int nnumberOfIncrementsInGaussiansPerMixture =
            m_nmaximumNumberOfGaussiansPerMixture;
        int ninitialNumberOfGaussiansPerMixture = 2;
        int nnumberOfIterations = m_headerProperties.
            getIntegerPropertyAndExitIfKeyNotFound(
            "TrainingManager.nembeddedBaumNumberOfIterations");
        //hMMReestimator.useBaumWelch(initialHMMsJarFileName,directoryWithSOPFiles,outputHMMSetFileName);
        //m_simulationFilesAndDirectories.updateCurrentMonophoneHMMsDirectory(outputHMMSetDirectory);
        String sentenceTrainParametersDataRootDirectory =
            m_simulationFilesAndDirectories.
            getSentenceTrainParametersDataRootDirectory();
        String trainingDataLocatorFileName = m_simulationFilesAndDirectories.
            getSentenceMonophoneTranscriptionsOfTrainSpeechDataFileName();

        //Print.dialog("initialHMMsJarFileName: " + initialHMMsJarFileName);

        hMMReestimator.useEmbeddedBaumWelchRecursively(initialHMMsJarFileName,
            nnumberOfIncrementsInGaussiansPerMixture,
            ninitialNumberOfGaussiansPerMixture, nnumberOfIterations,
            outputHMMSetDirectory, trainingDataLocatorFileName,
            m_headerProperties, m_patternGenerator,
            sentenceTrainParametersDataRootDirectory);
      }

      //start working to create cross-word shared triphones
      //create plain triphones by cloning most recent monophones
      if (m_oshouldCloneMonophonesAndReestimatePlainTriphoneHMHs) {
        String inputMonophoneHMMsJARFileName = m_simulationFilesAndDirectories.
            getMostRecentMonophoneHMMFileName();
        createPlainTriphonePrototypesAndReestimate(hMMReestimator,
            inputMonophoneHMMsJARFileName);
      }

//			if (m_oshouldReestimatePlainTriphoneHMMWithEmbeddedBaumWelch) {
//				String initialHMMsJarFileName = m_simulationFilesAndDirectories.getSentencesPlainTriphoneHMMPrototypesDirectory() +
//						m_simulationFilesAndDirectories.getSetOfHMMsFileName();
//				String outputHMMSetDirectory = m_simulationFilesAndDirectories.getRootDirectoryForTriphoneEmbeddedTraining() + "plain/";
//				String sentenceTrainParametersDataRootDirectory = m_simulationFilesAndDirectories.getSentenceTrainParametersDataRootDirectory();
//				String trainingDataLocatorFileName = m_simulationFilesAndDirectories.getSentenceTriphoneTranscriptionsOfTrainSpeechDataFileName();
//
//				int nnumberOfGaussiansPerMixture = 1;
//				int nnumberOfIterations = 2;
//				hMMReestimator.useEmbeddedBaumWelch(initialHMMsJarFileName,
//						nnumberOfGaussiansPerMixture, nnumberOfIterations, outputHMMSetDirectory,
//						trainingDataLocatorFileName, m_headerProperties, m_patternGenerator, sentenceTrainParametersDataRootDirectory);
//
//				m_simulationFilesAndDirectories.updateCurrentTriphoneHMMsDirectory(HMMReestimator.getDirectoryForEmbeddedIteration(outputHMMSetDirectory,nnumberOfGaussiansPerMixture,nnumberOfIterations-1));
//				//copy HMM and occupation statistics file
//				m_simulationFilesAndDirectories.copyMostRecentTriphonesHMMsToFinalDirectoryForPlainTriphones();
//			}
//
//			//ak
//			m_simulationFilesAndDirectories.updateCurrentTriphoneHMMsDirectory("C:/simulations_timit/mfcc_e_d_a/hmms/triphones/sentences/plain/embedded_1G_0it/");
//			//copy HMM and occupation statistics file
//			m_simulationFilesAndDirectories.copyMostRecentTriphonesHMMsToFinalDirectoryForPlainTriphones();

      m_simulationFilesAndDirectories.updateCurrentTriphoneHMMsDirectory(
          m_simulationFilesAndDirectories.
          getSentencesPlainTriphoneHMMsFinalDirectory());

      //cluster plain triphones
      if (m_oshouldClusterPlainTriphones) {
        String listOfAllLabels = m_simulationFilesAndDirectories.
            getListOfAllPossibleCrossWordTriphones();
        createListOfAllPossibleCrossWordTriphones(listOfAllLabels);

        //split potentially big JAR file, creating 1 JAR file per central phone
        String outputDirectoryForSplitHMMSets = m_simulationFilesAndDirectories.
            getGeneralOutputDirectory() + "temporary/";
        String inputSetOfPlainHMMsJARFileName = m_simulationFilesAndDirectories.
            getMostRecentTriphoneHMMFileName();

        FileNamesAndDirectories.createDirectoriesIfNecessary(
            outputDirectoryForSplitHMMSets);
        TreeBasedClustering.splitSetBasedOnCentralPhone(
            inputSetOfPlainHMMsJARFileName,
            m_tableOfLabels,
            outputDirectoryForSplitHMMSets);

        TableOfLabels allTriphonesTable = SetOfHMMsFile.getTableOfLabels(
            inputSetOfPlainHMMsJARFileName);

        double doccupationThreshold_RO = 100;
        double dstoppingCriterionThreshold_TB = 350;

        String inputOccupationStatisticsFileName = FileNamesAndDirectories.
            getPathFromFileName(m_simulationFilesAndDirectories.
                                getMostRecentTriphoneHMMFileName()) +
            "OccupationStatistics.txt";
        SetOfSharedContinuousHMMs setOfSharedContinuousHMMs =
            TreeBasedClustering.buildPhoneticTreeAndTieStates(
            inputOccupationStatisticsFileName,
            allTriphonesTable,
            m_tableOfLabels,
            outputDirectoryForSplitHMMSets,
            doccupationThreshold_RO,
            dstoppingCriterionThreshold_TB,
            m_patternGenerator,
            new TIMITPhoneticClasses(),
            //new DebugPhoneticClasses(),
            listOfAllLabels);
        String outputJARFileName = m_simulationFilesAndDirectories.
            getSentencesSharedTriphoneHMMPrototypesDirectory() +
            m_simulationFilesAndDirectories.getSetOfHMMsJARFileName();
        setOfSharedContinuousHMMs.writeHTKAndJARFiles(outputJARFileName);
        if (m_nverbose > 0) {
          Print.dialog("Writing set of shared HMMs after clustering " +
                       outputJARFileName);
        }
      }
      m_simulationFilesAndDirectories.updateCurrentTriphoneHMMsDirectory(
          m_simulationFilesAndDirectories.
          getSentencesSharedTriphoneHMMPrototypesDirectory());

      if (m_oshouldCreateCrossWordTriphoneHMMWithEmbeddedBaumWelch) {
        //getSharedHMMPrototypesFromHTK();
        //m_simulationFilesAndDirectories.updateCurrentTriphoneHMMsDirectory(m_simulationFilesAndDirectories.getSentencesSharedTriphoneHMMPrototypesDirectory());
        String initialHMMsJarFileName = m_simulationFilesAndDirectories.
            getMostRecentTriphoneHMMFileName();
        String outputHMMSetDirectory = m_simulationFilesAndDirectories.
            getRootDirectoryForSharedTriphoneEmbeddedTraining();
        //String outputHMMSetFileName = outputHMMSetDirectory + m_simulationFilesAndDirectories.getSetOfHMMsFileName();
        String sentenceTrainParametersDataRootDirectory =
            m_simulationFilesAndDirectories.
            getSentenceTrainParametersDataRootDirectory();
        String trainingDataLocatorFileName = m_simulationFilesAndDirectories.
            getSentenceTriphoneTranscriptionsOfTrainSpeechDataFileName();
        int nnumberOfGaussiansPerMixture = 1;
        int nnumberOfIterations = 4;
        hMMReestimator.useEmbeddedBaumWelch(initialHMMsJarFileName,
                                            nnumberOfGaussiansPerMixture,
                                            nnumberOfIterations,
                                            outputHMMSetDirectory,
                                            trainingDataLocatorFileName,
                                            m_headerProperties,
                                            m_patternGenerator,
                                            sentenceTrainParametersDataRootDirectory);
        m_simulationFilesAndDirectories.updateCurrentTriphoneHMMsDirectory(
            HMMReestimator.getDirectoryForEmbeddedIteration(
            outputHMMSetDirectory, nnumberOfGaussiansPerMixture,
            nnumberOfIterations - 1));
        //wants to declare this (1 Gaussian) the final triphones for sentences?
        m_simulationFilesAndDirectories.
            copyMostRecentSharedTriphonesHMMsToFinalDirectory();
      }

      //increase Gaussians of shared triphones
      if (m_oshouldRecursivelyCreateTriphoneHMMWithEmbeddedBaumWelch) {
        //recursively increase number of Gaussians and reestimate using embedded Baum-Welch
        //do not update "most recent monophone" (keep the one with 1 Gaussian as the most recent)
        String initialHMMsJarFileName = m_simulationFilesAndDirectories.
            getMostRecentTriphoneHMMFileName();
        String outputHMMSetDirectory = m_simulationFilesAndDirectories.
            getRootDirectoryForSharedTriphoneEmbeddedTraining();
        //String outputHMMSetFileName = outputHMMSetDirectory + m_simulationFilesAndDirectories.getSetOfHMMsFileName();
        int nnumberOfIncrementsInGaussiansPerMixture = 6;
        int ninitialNumberOfGaussiansPerMixture = 2;
        int nnumberOfIterations = 4;
        //hMMReestimator.useBaumWelch(initialHMMsJarFileName,directoryWithSOPFiles,outputHMMSetFileName);
        //m_simulationFilesAndDirectories.updateCurrentMonophoneHMMsDirectory(outputHMMSetDirectory);
        String sentenceTrainParametersDataRootDirectory =
            m_simulationFilesAndDirectories.
            getSentenceTrainParametersDataRootDirectory();
        String trainingDataLocatorFileName = m_simulationFilesAndDirectories.
            getSentenceTriphoneTranscriptionsOfTrainSpeechDataFileName();
        hMMReestimator.useEmbeddedBaumWelchRecursively(initialHMMsJarFileName,
            nnumberOfIncrementsInGaussiansPerMixture,
            ninitialNumberOfGaussiansPerMixture, nnumberOfIterations,
            outputHMMSetDirectory, trainingDataLocatorFileName,
            m_headerProperties, m_patternGenerator,
            sentenceTrainParametersDataRootDirectory);
      }
    }

    if (m_oisRunning &&
        (m_oshouldRunRecognitionForTestingData ||
         m_oshouldRunRecognitionForTrainingData)) {
      //runRecognitionTestForMonophonesIfRequestedByUser();
      //ak
      //runClassificationTest(m_simulationFilesAndDirectories.getFinalSetOfMonophoneHMMs(),false,false,m_simulationFilesAndDirectories.getFinalDirectoryForMonophoneIsolatedHMMs);
      //runClassificationTest(m_simulationFilesAndDirectories.getFinalSetOfMonophoneHMMs(),false,true,m_simulationFilesAndDirectories.getFinalDirectoryForMonophoneIsolatedHMMs);
    }
  }

  private void doDECTALKTraining() {

	    //PART I - Data preparation
	    if (m_oisRunning && m_oshouldCreateTranscriptionsForSpeechFiles) {
	      //write speech DataLocators for SENTENCES for both train and test data
	      //will include all WAV files (exception are SA sentences), not only core test set
	      createWAVDataLocatorsFileForMonophoneSentences(false,"wav");
	      createWAVDataLocatorsFileForMonophoneSentences(true,"wav");
	    }

	    if (m_oisRunning && m_oshouldConvertSpeechDataIntoParameters) {
	      if (m_oshouldConvertSentenceSpeechDataIntoParameters) {
	        createSetOfPatternsFilesForSentences(false);
	        createSetOfPatternsFilesForSentences(true);
	        if (m_oshouldCreatFileWithDynamicRanges) {
	          Print.dialog("Creating file with dynamic ranges");
	          String rootDir = m_simulationFilesAndDirectories.
	              getSentenceTrainParametersDataRootDirectory();
	          rootDir = FileNamesAndDirectories.getParent(rootDir);
	          SetOfPatterns.createFileWithDynamicRanges(rootDir);
	        }
	      }
	    }

	    if (m_oisRunning && m_oshouldCreateTranscriptionsForSpeechFiles) {
	      //write speech DataLocators for ISOLATED_SEGMENTS for both train and test data
	      createWAVDataLocatorsFileForMonophoneIsolatedSegments(false);
	      createWAVDataLocatorsFileForMonophoneIsolatedSegments(true);
	      writePropertiesToFileIfRequiredByUser();
	      //cross-word triphones
	      if (m_oshouldCreateTriphoneTranscriptions) {
	        createWAVDataLocatorsFileForCrossWordTriphoneSentences(false);
	        createWAVDataLocatorsFileForCrossWordTriphoneSentences(true);
	        writePropertiesToFileIfRequiredByUser();
	      }
	    }

	    if (m_oisRunning && m_oshouldConvertSpeechDataIntoParameters) {
	      if (m_oshouldConvertIsolatedSpeechDataIntoParameters) {
	        //"cut" SOP for SEGMENT's generating ISOLATED_SEGMENTS SOP's
	        cutSetOfPatternsFilesForSentencesAndWriteIsolatedSegmentsSOPs(false);
	        cutSetOfPatternsFilesForSentencesAndWriteIsolatedSegmentsSOPs(true);
	      }
	      writePropertiesToFileIfRequiredByUser();
	    }

	    //PART II - HMM training
	    if (m_oisRunning && m_oshouldCreateHMMFiles) {

	      runHMMTrainingForIsolatedSegments();

	      HMMReestimator hMMReestimator = new HMMReestimator(m_headerProperties);

	      if (m_oshouldCreateMonophoneHMMWithEmbeddedBaumWelch) {
	        String initialHMMsJarFileName = m_simulationFilesAndDirectories.
	            getMostRecentMonophoneHMMFileName();
	        if (! (new File(initialHMMsJarFileName).exists())) {
	          //if there is no HMM to start with, use prototypes
	          initialHMMsJarFileName = m_simulationFilesAndDirectories.
	              getIsolatedMonophoneHMMPrototypesDirectory()
	              + m_simulationFilesAndDirectories.getSetOfHMMsFileName();
	          if (! (new File(initialHMMsJarFileName).exists())) {
	            End.throwError(
	                "Could not find HMM set to initialize embedded Baum-Welch");
	          }
	        }

	        String outputHMMSetDirectory = m_simulationFilesAndDirectories.
	            getRootDirectoryForMonophoneEmbeddedTraining();
	        //String outputHMMSetFileName = outputHMMSetDirectory + m_simulationFilesAndDirectories.getSetOfHMMsFileName();
	        String sentenceTrainParametersDataRootDirectory =
	            m_simulationFilesAndDirectories.
	            getSentenceTrainParametersDataRootDirectory();
	        String trainingDataLocatorFileName = m_simulationFilesAndDirectories.
	            getSentenceMonophoneTranscriptionsOfTrainSpeechDataFileName();
	        int nnumberOfGaussiansPerMixture = 1;
	        //int nnumberOfIterations = 4;
	        int nnumberOfIterations = m_headerProperties.
	            getIntegerPropertyAndExitIfKeyNotFound(
	            "TrainingManager.nembeddedBaumNumberOfIterations");
	        hMMReestimator.useEmbeddedBaumWelch(initialHMMsJarFileName,
	                                            nnumberOfGaussiansPerMixture,
	                                            nnumberOfIterations,
	                                            outputHMMSetDirectory,
	                                            trainingDataLocatorFileName,
	                                            m_headerProperties,
	                                            m_patternGenerator,
	                                            sentenceTrainParametersDataRootDirectory);

	        m_simulationFilesAndDirectories.updateCurrentMonophoneHMMsDirectory(
	            HMMReestimator.getDirectoryForEmbeddedIteration(
	            outputHMMSetDirectory, nnumberOfGaussiansPerMixture,
	            nnumberOfIterations - 1));
	        //wants to declare this the final monophones for sentences
	        m_simulationFilesAndDirectories.
	            copyMostRecentSentencesMonophonesHMMsToFinalDirectory();
	      }
	      //if want to use isolated monophones comment out line below
	      m_simulationFilesAndDirectories.updateCurrentMonophoneHMMsDirectory(
	          m_simulationFilesAndDirectories.
	          getSentencesMonophoneHMMsFinalDirectory());

	      if (m_oshouldRecursivelyCreateMonophoneHMMWithEmbeddedBaumWelch) {
	        //recursively increase number of Gaussians and reestimate using embedded Baum-Welch
	        //do not update "most recent monophone" (keep the one with 1 Gaussian as the most recent)
	        String initialHMMsJarFileName = m_simulationFilesAndDirectories.
	            getMostRecentMonophoneHMMFileName();
	        String outputHMMSetDirectory = m_simulationFilesAndDirectories.
	            getRootDirectoryForMonophoneEmbeddedTraining();
	        //String outputHMMSetFileName = outputHMMSetDirectory + m_simulationFilesAndDirectories.getSetOfHMMsFileName();
	        int nnumberOfIncrementsInGaussiansPerMixture =
	            m_nmaximumNumberOfGaussiansPerMixture;
	        int ninitialNumberOfGaussiansPerMixture = 2;
	        int nnumberOfIterations = m_headerProperties.
	            getIntegerPropertyAndExitIfKeyNotFound(
	            "TrainingManager.nembeddedBaumNumberOfIterations");
	        //hMMReestimator.useBaumWelch(initialHMMsJarFileName,directoryWithSOPFiles,outputHMMSetFileName);
	        //m_simulationFilesAndDirectories.updateCurrentMonophoneHMMsDirectory(outputHMMSetDirectory);
	        String sentenceTrainParametersDataRootDirectory =
	            m_simulationFilesAndDirectories.
	            getSentenceTrainParametersDataRootDirectory();
	        String trainingDataLocatorFileName = m_simulationFilesAndDirectories.
	            getSentenceMonophoneTranscriptionsOfTrainSpeechDataFileName();

	        //Print.dialog("initialHMMsJarFileName: " + initialHMMsJarFileName);

	        hMMReestimator.useEmbeddedBaumWelchRecursively(initialHMMsJarFileName,
	            nnumberOfIncrementsInGaussiansPerMixture,
	            ninitialNumberOfGaussiansPerMixture, nnumberOfIterations,
	            outputHMMSetDirectory, trainingDataLocatorFileName,
	            m_headerProperties, m_patternGenerator,
	            sentenceTrainParametersDataRootDirectory);
	      }

	      //start working to create cross-word shared triphones
	      //create plain triphones by cloning most recent monophones
	      if (m_oshouldCloneMonophonesAndReestimatePlainTriphoneHMHs) {
	        String inputMonophoneHMMsJARFileName = m_simulationFilesAndDirectories.
	            getMostRecentMonophoneHMMFileName();
	        createPlainTriphonePrototypesAndReestimate(hMMReestimator,
	            inputMonophoneHMMsJARFileName);
	      }

//				if (m_oshouldReestimatePlainTriphoneHMMWithEmbeddedBaumWelch) {
//					String initialHMMsJarFileName = m_simulationFilesAndDirectories.getSentencesPlainTriphoneHMMPrototypesDirectory() +
//							m_simulationFilesAndDirectories.getSetOfHMMsFileName();
//					String outputHMMSetDirectory = m_simulationFilesAndDirectories.getRootDirectoryForTriphoneEmbeddedTraining() + "plain/";
//					String sentenceTrainParametersDataRootDirectory = m_simulationFilesAndDirectories.getSentenceTrainParametersDataRootDirectory();
//					String trainingDataLocatorFileName = m_simulationFilesAndDirectories.getSentenceTriphoneTranscriptionsOfTrainSpeechDataFileName();
	//
//					int nnumberOfGaussiansPerMixture = 1;
//					int nnumberOfIterations = 2;
//					hMMReestimator.useEmbeddedBaumWelch(initialHMMsJarFileName,
//							nnumberOfGaussiansPerMixture, nnumberOfIterations, outputHMMSetDirectory,
//							trainingDataLocatorFileName, m_headerProperties, m_patternGenerator, sentenceTrainParametersDataRootDirectory);
	//
//					m_simulationFilesAndDirectories.updateCurrentTriphoneHMMsDirectory(HMMReestimator.getDirectoryForEmbeddedIteration(outputHMMSetDirectory,nnumberOfGaussiansPerMixture,nnumberOfIterations-1));
//					//copy HMM and occupation statistics file
//					m_simulationFilesAndDirectories.copyMostRecentTriphonesHMMsToFinalDirectoryForPlainTriphones();
//				}
	//
//				//ak
//				m_simulationFilesAndDirectories.updateCurrentTriphoneHMMsDirectory("C:/simulations_timit/mfcc_e_d_a/hmms/triphones/sentences/plain/embedded_1G_0it/");
//				//copy HMM and occupation statistics file
//				m_simulationFilesAndDirectories.copyMostRecentTriphonesHMMsToFinalDirectoryForPlainTriphones();

	      m_simulationFilesAndDirectories.updateCurrentTriphoneHMMsDirectory(
	          m_simulationFilesAndDirectories.
	          getSentencesPlainTriphoneHMMsFinalDirectory());

	      //cluster plain triphones
	      if (m_oshouldClusterPlainTriphones) {
	        String listOfAllLabels = m_simulationFilesAndDirectories.
	            getListOfAllPossibleCrossWordTriphones();
	        createListOfAllPossibleCrossWordTriphones(listOfAllLabels);

	        //split potentially big JAR file, creating 1 JAR file per central phone
	        String outputDirectoryForSplitHMMSets = m_simulationFilesAndDirectories.
	            getGeneralOutputDirectory() + "temporary/";
	        String inputSetOfPlainHMMsJARFileName = m_simulationFilesAndDirectories.
	            getMostRecentTriphoneHMMFileName();

	        FileNamesAndDirectories.createDirectoriesIfNecessary(
	            outputDirectoryForSplitHMMSets);
	        TreeBasedClustering.splitSetBasedOnCentralPhone(
	            inputSetOfPlainHMMsJARFileName,
	            m_tableOfLabels,
	            outputDirectoryForSplitHMMSets);

	        TableOfLabels allTriphonesTable = SetOfHMMsFile.getTableOfLabels(
	            inputSetOfPlainHMMsJARFileName);

	        double doccupationThreshold_RO = 100;
	        double dstoppingCriterionThreshold_TB = 350;

	        String inputOccupationStatisticsFileName = FileNamesAndDirectories.
	            getPathFromFileName(m_simulationFilesAndDirectories.
	                                getMostRecentTriphoneHMMFileName()) +
	            "OccupationStatistics.txt";
	        SetOfSharedContinuousHMMs setOfSharedContinuousHMMs =
	            TreeBasedClustering.buildPhoneticTreeAndTieStates(
	            inputOccupationStatisticsFileName,
	            allTriphonesTable,
	            m_tableOfLabels,
	            outputDirectoryForSplitHMMSets,
	            doccupationThreshold_RO,
	            dstoppingCriterionThreshold_TB,
	            m_patternGenerator,
	            new TIMITPhoneticClasses(),
	            //new DebugPhoneticClasses(),
	            listOfAllLabels);
	        String outputJARFileName = m_simulationFilesAndDirectories.
	            getSentencesSharedTriphoneHMMPrototypesDirectory() +
	            m_simulationFilesAndDirectories.getSetOfHMMsJARFileName();
	        setOfSharedContinuousHMMs.writeHTKAndJARFiles(outputJARFileName);
	        if (m_nverbose > 0) {
	          Print.dialog("Writing set of shared HMMs after clustering " +
	                       outputJARFileName);
	        }
	      }
	      m_simulationFilesAndDirectories.updateCurrentTriphoneHMMsDirectory(
	          m_simulationFilesAndDirectories.
	          getSentencesSharedTriphoneHMMPrototypesDirectory());

	      if (m_oshouldCreateCrossWordTriphoneHMMWithEmbeddedBaumWelch) {
	        //getSharedHMMPrototypesFromHTK();
	        //m_simulationFilesAndDirectories.updateCurrentTriphoneHMMsDirectory(m_simulationFilesAndDirectories.getSentencesSharedTriphoneHMMPrototypesDirectory());
	        String initialHMMsJarFileName = m_simulationFilesAndDirectories.
	            getMostRecentTriphoneHMMFileName();
	        String outputHMMSetDirectory = m_simulationFilesAndDirectories.
	            getRootDirectoryForSharedTriphoneEmbeddedTraining();
	        //String outputHMMSetFileName = outputHMMSetDirectory + m_simulationFilesAndDirectories.getSetOfHMMsFileName();
	        String sentenceTrainParametersDataRootDirectory =
	            m_simulationFilesAndDirectories.
	            getSentenceTrainParametersDataRootDirectory();
	        String trainingDataLocatorFileName = m_simulationFilesAndDirectories.
	            getSentenceTriphoneTranscriptionsOfTrainSpeechDataFileName();
	        int nnumberOfGaussiansPerMixture = 1;
	        int nnumberOfIterations = 4;
	        hMMReestimator.useEmbeddedBaumWelch(initialHMMsJarFileName,
	                                            nnumberOfGaussiansPerMixture,
	                                            nnumberOfIterations,
	                                            outputHMMSetDirectory,
	                                            trainingDataLocatorFileName,
	                                            m_headerProperties,
	                                            m_patternGenerator,
	                                            sentenceTrainParametersDataRootDirectory);
	        m_simulationFilesAndDirectories.updateCurrentTriphoneHMMsDirectory(
	            HMMReestimator.getDirectoryForEmbeddedIteration(
	            outputHMMSetDirectory, nnumberOfGaussiansPerMixture,
	            nnumberOfIterations - 1));
	        //wants to declare this (1 Gaussian) the final triphones for sentences?
	        m_simulationFilesAndDirectories.
	            copyMostRecentSharedTriphonesHMMsToFinalDirectory();
	      }

	      //increase Gaussians of shared triphones
	      if (m_oshouldRecursivelyCreateTriphoneHMMWithEmbeddedBaumWelch) {
	        //recursively increase number of Gaussians and reestimate using embedded Baum-Welch
	        //do not update "most recent monophone" (keep the one with 1 Gaussian as the most recent)
	        String initialHMMsJarFileName = m_simulationFilesAndDirectories.
	            getMostRecentTriphoneHMMFileName();
	        String outputHMMSetDirectory = m_simulationFilesAndDirectories.
	            getRootDirectoryForSharedTriphoneEmbeddedTraining();
	        //String outputHMMSetFileName = outputHMMSetDirectory + m_simulationFilesAndDirectories.getSetOfHMMsFileName();
	        int nnumberOfIncrementsInGaussiansPerMixture = 6;
	        int ninitialNumberOfGaussiansPerMixture = 2;
	        int nnumberOfIterations = 4;
	        //hMMReestimator.useBaumWelch(initialHMMsJarFileName,directoryWithSOPFiles,outputHMMSetFileName);
	        //m_simulationFilesAndDirectories.updateCurrentMonophoneHMMsDirectory(outputHMMSetDirectory);
	        String sentenceTrainParametersDataRootDirectory =
	            m_simulationFilesAndDirectories.
	            getSentenceTrainParametersDataRootDirectory();
	        String trainingDataLocatorFileName = m_simulationFilesAndDirectories.
	            getSentenceTriphoneTranscriptionsOfTrainSpeechDataFileName();
	        hMMReestimator.useEmbeddedBaumWelchRecursively(initialHMMsJarFileName,
	            nnumberOfIncrementsInGaussiansPerMixture,
	            ninitialNumberOfGaussiansPerMixture, nnumberOfIterations,
	            outputHMMSetDirectory, trainingDataLocatorFileName,
	            m_headerProperties, m_patternGenerator,
	            sentenceTrainParametersDataRootDirectory);
	      }
	    }

	    if (m_oisRunning &&
	        (m_oshouldRunRecognitionForTestingData ||
	         m_oshouldRunRecognitionForTrainingData)) {
	      //runRecognitionTestForMonophonesIfRequestedByUser();
	      //ak
	      //runClassificationTest(m_simulationFilesAndDirectories.getFinalSetOfMonophoneHMMs(),false,false,m_simulationFilesAndDirectories.getFinalDirectoryForMonophoneIsolatedHMMs);
	      //runClassificationTest(m_simulationFilesAndDirectories.getFinalSetOfMonophoneHMMs(),false,true,m_simulationFilesAndDirectories.getFinalDirectoryForMonophoneIsolatedHMMs);
	    }
	  }

	    
  /**
   *  Generate parameters for sentences and then cut them. This avoids problems
   *  with derivatives, allows using CMS (cepstral mean subtraction) to the whole
   *  sequence, etc.
   */
  private void doTIDIGITSTraining() {

    //PART I - Data preparation
    if (m_oisRunning && m_oshouldCopyDatabaseToUniqueDirectory) {
      writeAdultPartOfTIDIGITSToTwoDirectories();
    }

    //write speech DataLocators for SENTENCES for both train and test data
    if (m_oisRunning && m_oshouldCreateTranscriptionsForSpeechFiles) {
      createTranscriptionsForTIDIGITSUsingForcedAlignedResults();
    }

    if (m_oisRunning && m_oshouldConvertSpeechDataIntoParameters) {
      if (m_oshouldConvertSentenceSpeechDataIntoParameters) {
        createSetOfPatternsFilesForSentences(false);
        createSetOfPatternsFilesForSentences(true);
        if (m_oshouldCreatFileWithDynamicRanges) {
          Print.dialog("Creating file with dynamic ranges");
          String rootDir = m_simulationFilesAndDirectories.
              getSentenceTrainParametersDataRootDirectory();
          rootDir = FileNamesAndDirectories.getParent(rootDir);
          SetOfPatterns.createFileWithDynamicRanges(rootDir);
        }
      }
    }

    if (m_oisRunning && m_oshouldCreateTranscriptionsForSpeechFiles) {
      //write speech DataLocators for ISOLATED_SEGMENTS for both train and test data
      createWAVDataLocatorsFileForMonophoneIsolatedSegments(false);
      createWAVDataLocatorsFileForMonophoneIsolatedSegments(true);
      writePropertiesToFileIfRequiredByUser();
    }

    if (m_oisRunning && m_oshouldConvertSpeechDataIntoParameters) {
      if (m_oshouldConvertIsolatedSpeechDataIntoParameters) {
        //"cut" SOP for SEGMENT's generating ISOLATED_SEGMENTS SOP's
        cutSetOfPatternsFilesForSentencesAndWriteIsolatedSegmentsSOPs(false);
        cutSetOfPatternsFilesForSentencesAndWriteIsolatedSegmentsSOPs(true);
      }
      writePropertiesToFileIfRequiredByUser();
    }

    //PART II - HMM training
    if (m_oisRunning && m_oshouldCreateHMMFiles) {

      runHMMTrainingForIsolatedSegments();

      HMMReestimator hMMReestimator = new HMMReestimator(m_headerProperties);

      if (m_oshouldCreateMonophoneHMMWithEmbeddedBaumWelch) {
        String initialHMMsJarFileName = m_simulationFilesAndDirectories.
            getMostRecentMonophoneHMMFileName();
        if (! (new File(initialHMMsJarFileName).exists())) {
          //if there is no HMM to start with, use prototypes
          initialHMMsJarFileName = m_simulationFilesAndDirectories.
              getIsolatedMonophoneHMMPrototypesDirectory()
              + m_simulationFilesAndDirectories.getSetOfHMMsFileName();
          if (! (new File(initialHMMsJarFileName).exists())) {
            End.throwError(
                "Could not find HMM set to initialize embedded Baum-Welch");
          }
        }

        String outputHMMSetDirectory = m_simulationFilesAndDirectories.
            getRootDirectoryForMonophoneEmbeddedTraining();
        //String outputHMMSetFileName = outputHMMSetDirectory + m_simulationFilesAndDirectories.getSetOfHMMsFileName();
        String sentenceTrainParametersDataRootDirectory =
            m_simulationFilesAndDirectories.
            getSentenceTrainParametersDataRootDirectory();
        String trainingDataLocatorFileName = m_simulationFilesAndDirectories.
            getSentenceMonophoneTranscriptionsOfTrainSpeechDataFileName();
        int nnumberOfGaussiansPerMixture = 1;
        //int nnumberOfIterations = 4;
        int nnumberOfIterations = m_headerProperties.
            getIntegerPropertyAndExitIfKeyNotFound(
            "TrainingManager.nembeddedBaumNumberOfIterations");
        hMMReestimator.useEmbeddedBaumWelch(initialHMMsJarFileName,
                                            nnumberOfGaussiansPerMixture,
                                            nnumberOfIterations,
                                            outputHMMSetDirectory,
                                            trainingDataLocatorFileName,
                                            m_headerProperties,
                                            m_patternGenerator,
                                            sentenceTrainParametersDataRootDirectory);

        m_simulationFilesAndDirectories.updateCurrentMonophoneHMMsDirectory(
            HMMReestimator.getDirectoryForEmbeddedIteration(
            outputHMMSetDirectory, nnumberOfGaussiansPerMixture,
            nnumberOfIterations - 1));
        //wants to declare this the final monophones for sentences
        m_simulationFilesAndDirectories.
            copyMostRecentSentencesMonophonesHMMsToFinalDirectory();
      }
      //if want to use isolated monophones comment out line below
      m_simulationFilesAndDirectories.updateCurrentMonophoneHMMsDirectory(
          m_simulationFilesAndDirectories.
          getSentencesMonophoneHMMsFinalDirectory());

      if (m_oshouldRecursivelyCreateMonophoneHMMWithEmbeddedBaumWelch) {
        //recursively increase number of Gaussians and reestimate using embedded Baum-Welch
        //do not update "most recent monophone" (keep the one with 1 Gaussian as the most recent)
        String initialHMMsJarFileName = m_simulationFilesAndDirectories.
            getMostRecentMonophoneHMMFileName();
        String outputHMMSetDirectory = m_simulationFilesAndDirectories.
            getRootDirectoryForMonophoneEmbeddedTraining();
        //String outputHMMSetFileName = outputHMMSetDirectory + m_simulationFilesAndDirectories.getSetOfHMMsFileName();
        int nnumberOfIncrementsInGaussiansPerMixture =
            m_nmaximumNumberOfGaussiansPerMixture;
        int ninitialNumberOfGaussiansPerMixture = 2;
        int nnumberOfIterations = m_headerProperties.
            getIntegerPropertyAndExitIfKeyNotFound(
            "TrainingManager.nembeddedBaumNumberOfIterations");
        //hMMReestimator.useBaumWelch(initialHMMsJarFileName,directoryWithSOPFiles,outputHMMSetFileName);
        //m_simulationFilesAndDirectories.updateCurrentMonophoneHMMsDirectory(outputHMMSetDirectory);
        String sentenceTrainParametersDataRootDirectory =
            m_simulationFilesAndDirectories.
            getSentenceTrainParametersDataRootDirectory();
        String trainingDataLocatorFileName = m_simulationFilesAndDirectories.
            getSentenceMonophoneTranscriptionsOfTrainSpeechDataFileName();

        //Print.dialog("initialHMMsJarFileName: " + initialHMMsJarFileName);

        hMMReestimator.useEmbeddedBaumWelchRecursively(initialHMMsJarFileName,
            nnumberOfIncrementsInGaussiansPerMixture,
            ninitialNumberOfGaussiansPerMixture, nnumberOfIterations,
            outputHMMSetDirectory, trainingDataLocatorFileName,
            m_headerProperties, m_patternGenerator,
            sentenceTrainParametersDataRootDirectory);
      }
    }

    if (m_oisRunning &&
        (m_oshouldRunRecognitionForTestingData ||
         m_oshouldRunRecognitionForTrainingData)) {
      //runRecognitionTestForMonophonesIfRequestedByUser();
      //ak
      //runClassificationTest(m_simulationFilesAndDirectories.getFinalSetOfMonophoneHMMs(),false,false,m_simulationFilesAndDirectories.getFinalDirectoryForMonophoneIsolatedHMMs);
      //runClassificationTest(m_simulationFilesAndDirectories.getFinalSetOfMonophoneHMMs(),false,true,m_simulationFilesAndDirectories.getFinalDirectoryForMonophoneIsolatedHMMs);
    }
  }

  /**
   *  Description of the Method
   *
   *@param  oisTrain  Description of Parameter
   */
  private void createWAVDataLocatorsFileForCrossWordTriphoneSentences(boolean
      oisTrain) {
    String sentenceTriphoneTranscriptionsSpeechDataFileName = null;
    String sentenceMonophoneTranscriptionsSpeechDataFileName = null;
    if (oisTrain) {
      sentenceTriphoneTranscriptionsSpeechDataFileName =
          m_simulationFilesAndDirectories.
          getSentenceTriphoneTranscriptionsOfTrainSpeechDataFileName();
      sentenceMonophoneTranscriptionsSpeechDataFileName =
          m_simulationFilesAndDirectories.
          getSentenceMonophoneTranscriptionsOfTrainSpeechDataFileName();
    }
    else {
      sentenceTriphoneTranscriptionsSpeechDataFileName =
          m_simulationFilesAndDirectories.
          getSentenceTriphoneTranscriptionsOfTestSpeechDataFileName();
      sentenceMonophoneTranscriptionsSpeechDataFileName =
          m_simulationFilesAndDirectories.
          getSentenceMonophoneTranscriptionsOfTestSpeechDataFileName();
    }
    //valid only for TIMIT
    String[] contextIndependentLabels = {
        "sil", "h#"};
    CrossWordTriphoneDataLocatorsFileCreator
        crossWordTriphoneDataLocatorsFileCreator = new
        CrossWordTriphoneDataLocatorsFileCreator(contextIndependentLabels, null);
    crossWordTriphoneDataLocatorsFileCreator.
        convertMonophoneInCrossWordTriphoneTranscription(
        sentenceMonophoneTranscriptionsSpeechDataFileName,
        sentenceTriphoneTranscriptionsSpeechDataFileName, m_tableOfLabels);
  }

  /**
   *  Description of the Method
   */
  private void createMonophoneHMMPrototypes() {

    int nnumberOfEntries = m_tableOfLabels.getNumberOfEntries();
    //to store all HMMs and their file names
    ContinuousHMM[] continuousHMMs = new ContinuousHMM[nnumberOfEntries];
    String[] hmmFileNames = new String[nnumberOfEntries];

    //output directory
    String hmmsDirectory = m_simulationFilesAndDirectories.
        getIsolatedMonophoneHMMPrototypesDirectory();
    if (m_nverbose > 0) {
      Print.dialog("HMM prototypes: " + IO.getEndOfString(hmmsDirectory, 55));
    }

    Print.setJProgressBarRange(0, nnumberOfEntries);
    for (int ntableEntry = 0; ntableEntry < nnumberOfEntries; ntableEntry++) {
      //get file names
      String fileName = m_simulationFilesAndDirectories.getPrefferedName(
          m_tableOfLabels, ntableEntry, HMM.m_FILE_EXTENSION);
      String patternsFileName = m_simulationFilesAndDirectories.
          getSOPFileNameForIsolatedSegments(m_tableOfLabels, ntableEntry,
                                            "train");
      String thisHMMConfigurationFileName = m_simulationFilesAndDirectories.
          getHMMConfigurationFileNameForIsolatedSegments(ntableEntry);

      if (m_nverbose > 1) {
        Print.dialog("Creating HMM model for table entry number " + ntableEntry +
                     " from configuration file " +
                     thisHMMConfigurationFileName +
                     ", using parameters of file " + patternsFileName);
      }

      //get prototype
      ContinuousHMM continuousHMM = HMMInitializer.
          getHMMPrototypeBasedOnGivenTopologyAndGlobalMeanAndVariance(
          thisHMMConfigurationFileName, patternsFileName);

      //store this HMM and its file name
      continuousHMMs[ntableEntry] = continuousHMM;
      hmmFileNames[ntableEntry] = fileName;
      Print.updateJProgressBar(ntableEntry + 1);
    }
    //create a set of HMMs
    SetOfPlainContinuousHMMs
        setOfPlainContinuousHMMs
        = new SetOfPlainContinuousHMMs(continuousHMMs,
                                       hmmFileNames,
                                       m_tableOfLabels,
                                       m_patternGenerator);

    //write JAR file with HMMs

    FileNamesAndDirectories.createDirectoriesIfNecessary(hmmsDirectory);

    setOfPlainContinuousHMMs.writeToJARFile(hmmsDirectory +
                                            m_simulationFilesAndDirectories.
                                            getSetOfHMMsFileName(),
                                            m_headerProperties);
  }

  /**
   *  Before training procedure starts, check if all necessary information is
   *  available. Want to avoid user leaving the simulation running and it stops
   *  after some steps due to lack of information.
   */
  private void interpretHeaderAndInitialize() {

    //mandatory keys
    String property = m_headerProperties.getPropertyAndExitIfKeyNotFound(
        "TrainingManager.Type");
    m_type = Type.getTypeAndExitOnError(property);

    property = m_headerProperties.getPropertyAndExitIfKeyNotFound(
        "Database.Type");
    m_databaseType = Database.Type.getTypeAndExitOnError(property);

    //Print.dialog(m_testSpeechDataRootDirectory);
    //Print.dialog(m_trainSpeechDataRootDirectory);
    //System.exit(1);
    if (m_type == Type.SENTENCE && m_databaseType == Database.Type.TIMIT) {
      property = m_headerProperties.getProperty(
          "TrainingManager.oshouldCreatePrototypeHMMsFromIsolatedSegments",
          "true");
      m_oshouldCreatePrototypeHMMsFromIsolatedSegmentsOfTIMIT = (Boolean.
          valueOf(property)).booleanValue();
      m_headerProperties.setProperty(
          "TrainingManager.oshouldCreatePrototypeHMMsFromIsolatedSegments",
          property);
    }

    property = m_headerProperties.getProperty(
        "TrainingManager.oshouldCopyDatabaseToUniqueDirectory", "false");
    m_oshouldCopyDatabaseToUniqueDirectory = (Boolean.valueOf(property)).
        booleanValue();
    m_headerProperties.setProperty(
        "TrainingManager.oshouldCopyDatabaseToUniqueDirectory", property);

    if (m_oshouldCopyDatabaseToUniqueDirectory) {
      m_originalDirectoryOfDatabaseTestData = m_headerProperties.
          getPropertyAndExitIfKeyNotFound(
          "TrainingManager.OriginalDirectoryOfDatabaseTestData");
      m_originalDirectoryOfDatabaseTrainData = m_headerProperties.
          getPropertyAndExitIfKeyNotFound(
          "TrainingManager.OriginalDirectoryOfDatabaseTrainData");
      m_originalDirectoryOfDatabaseTestData = FileNamesAndDirectories.
          replaceAndForceEndingWithSlash(m_originalDirectoryOfDatabaseTestData);
      m_originalDirectoryOfDatabaseTrainData = FileNamesAndDirectories.
          replaceAndForceEndingWithSlash(m_originalDirectoryOfDatabaseTrainData);
      m_headerProperties.setProperty(
          "TrainingManager.OriginalDirectoryOfDatabaseTestData",
          m_originalDirectoryOfDatabaseTestData);
      m_headerProperties.setProperty(
          "TrainingManager.OriginalDirectoryOfDatabaseTrainData",
          m_originalDirectoryOfDatabaseTrainData);
    }

    property = m_headerProperties.getProperty(
        "TrainingManager.oshouldCreateTriphoneModels", "true");
    m_oshouldCreateTriphoneModels = (Boolean.valueOf(property)).booleanValue();
    m_headerProperties.setProperty(
        "TrainingManager.oshouldCreateTriphoneModels", property);

    property = m_headerProperties.getProperty(
        "TrainingManager.oshouldRunRecognitionForTestingData", "true");
    m_oshouldRunRecognitionForTestingData = (Boolean.valueOf(property)).
        booleanValue();
    m_headerProperties.setProperty(
        "TrainingManager.oshouldRunRecognitionForTestingData", property);

    property = m_headerProperties.getProperty(
        "TrainingManager.oshouldRunRecognitionForTrainingData", "false");
    m_oshouldRunRecognitionForTrainingData = (Boolean.valueOf(property)).
        booleanValue();
    m_headerProperties.setProperty(
        "TrainingManager.oshouldRunRecognitionForTrainingData", property);

    property = m_headerProperties.getProperty(
        "TrainingManager.oshouldRunClassification", "true");
    m_oshouldRunClassification = (Boolean.valueOf(property)).booleanValue();
    m_headerProperties.setProperty("TrainingManager.oshouldRunClassification",
                                   property);

    property = m_headerProperties.getProperty(
        "TrainingManager.oshouldCreateTranscriptionsForSpeechFiles", "true");
    m_oshouldCreateTranscriptionsForSpeechFiles = (Boolean.valueOf(property)).
        booleanValue();
    m_headerProperties.setProperty(
        "TrainingManager.oshouldCreateTranscriptionsForSpeechFiles", property);

    property = m_headerProperties.getProperty(
        "TrainingManager.oshouldConvertSpeechDataIntoParameters", "true");
    m_oshouldConvertSpeechDataIntoParameters = (Boolean.valueOf(property)).
        booleanValue();
    m_headerProperties.setProperty(
        "TrainingManager.oshouldConvertSpeechDataIntoParameters", property);

    property = m_headerProperties.getProperty(
        "TrainingManager.oshouldCreateHMMFiles", "true");
    m_oshouldCreateHMMFiles = (Boolean.valueOf(property)).booleanValue();
    m_headerProperties.setProperty("TrainingManager.oshouldCreateHMMFiles",
                                   property);

    property = m_headerProperties.getProperty(
        "TrainingManager.oshouldWriteReportFile", "true");
    m_oshouldWriteReportFile = (Boolean.valueOf(property)).booleanValue();
    m_headerProperties.setProperty("TrainingManager.oshouldWriteReportFile",
                                   property);

    //the extension must be TRN...
    if (m_oshouldWriteReportFile) {
      String defaultFileName = FileNamesAndDirectories.getFileNameFromPath(
          m_headerPropertiesFileName);
      defaultFileName = FileNamesAndDirectories.deleteExtension(defaultFileName);
      defaultFileName = m_simulationFilesAndDirectories.
          getGeneralOutputDirectory() + defaultFileName + "_report.log";
      m_outputReportFileName = m_headerProperties.getProperty(
          "TrainingManager.OutputReportTRNFileName", defaultFileName);
      //FileNamesAndDirectories.checkExtensionWithCaseIgnoredAndExitOnError(m_outputReportFileName,
      //		m_FILE_EXTENSION);
    }

    property = m_headerProperties.getProperty(
        "TrainingManager.ouseAbsolutePath", "false");
    m_ouseAbsolutePath = (Boolean.valueOf(property)).booleanValue();
    m_headerProperties.setProperty("TrainingManager.ouseAbsolutePath", property);

    property = m_headerProperties.getProperty("TrainingManager.nverbose", "0");
    m_nverbose = (Integer.valueOf(property)).intValue();
    CheckValues.exitOnError(m_nverbose, 0, 10, "TrainingManager.nverbose");
    m_headerProperties.setProperty("TrainingManager.nverbose", property);

    if (m_oshouldConvertSpeechDataIntoParameters || m_oshouldCreateHMMFiles) {
      //need PatternGenerator
      m_patternGenerator = PatternGenerator.getPatternGenerator(
          m_headerProperties);
      if (m_patternGenerator == null) {
        Print.error("Could not construct PatternGenerator.");
        End.exit();
      }
    }

    if (m_oshouldConvertSpeechDataIntoParameters) {
      //m_simulationFilesAndDirectories.
      if (m_type == TrainingManager.Type.SENTENCE) {
        property = m_simulationFilesAndDirectories.
            getSentenceTrainParametersDataRootDirectory();
        m_headerProperties.setProperty(
            "TrainingManager.SentenceTrainParametersDirectory", property);
        property = m_simulationFilesAndDirectories.
            getSentenceTestParametersDataRootDirectory();
        m_headerProperties.setProperty(
            "TrainingManager.SentenceTestParametersDirectory", property);
      }
      //need information below in case of testing
      property = m_simulationFilesAndDirectories.
          getIsolatedTrainParametersDataRootDirectory();
      m_headerProperties.setProperty(
          "TrainingManager.IsolatedTrainParametersDirectory", property);
      property = m_simulationFilesAndDirectories.
          getIsolatedTestParametersDataRootDirectory();
      m_headerProperties.setProperty(
          "TrainingManager.IsolatedTestParametersDirectory", property);
    }

    //even if m_oshouldCreateHMMFiles is false, may need this value
    //for forced alignment
    property = m_headerProperties.getProperty(
        "TrainingManager.nmaximumNumberOfGaussiansPerMixture", "3");
    m_nmaximumNumberOfGaussiansPerMixture = Integer.parseInt(property);
    CheckValues.exitOnError(m_nmaximumNumberOfGaussiansPerMixture, 1, 300,
                            "TrainingManager.nmaximumNumberOfGaussiansPerMixture");
    m_headerProperties.setProperty(
        "TrainingManager.nmaximumNumberOfGaussiansPerMixture", property);

    if (m_oshouldCreateHMMFiles) {
      //try to get a HMM configuration file
      //notice that if it's null, it is assumed there will exist
      //a HMM configuration file for each table entry
      //m_defaultHMMConfigurationFileName = m_headerProperties.getProperty("TrainingManager.DefaultHMMConfigurationFileName");
      //if (m_defaultHMMConfigurationFileName != null) {
      //	m_headerProperties.setProperty("TrainingManager.DefaultHMMConfigurationFileName", m_defaultHMMConfigurationFileName);
      //}
      //JAR file name to write HMMs
      //m_outputSetOfHMMsFileName = m_headerProperties.getProperty("SetOfHMMs.JARFileName", "hmms");
    }

    //here it goes initialization that depends on m_type
    //if (m_type == TrainingManager.Type.SENTENCE || m_type == TrainingManager.Type.CUT_SENTENCE) {
    //	property = m_headerProperties.getProperty("TrainingManager.okeepDirectoryStructureForSOPsOfSentences", "false");
    //	m_okeepDirectoryStructureForSOPsOfSentences = (Boolean.valueOf(property)).booleanValue();
    //}
    //else if (m_type == TrainingManager.Type.ISOLATED_SEGMENT) {
    //}

    //if the output file exists, read it to complement information
//		if (new File(m_outputReportFileName).exists()) {
//			HeaderProperties additionalHeaderProperties = HeaderProperties.getPropertiesFromFile(m_outputReportFileName);
//			m_headerProperties.complementWithAnotherProperties(additionalHeaderProperties);
//		}

    //get a name for TST file
    m_outputTSTFileName = m_headerProperties.getProperty(
        "TrainingManager.OutputTSTFileName");
    if (m_outputTSTFileName == null) {
      m_outputTSTFileName = m_simulationFilesAndDirectories.
          getGeneralOutputDirectory() + "classification.TST";
    }
    m_headerProperties.setProperty("TrainingManager.OutputTSTFileName",
                                   m_outputTSTFileName);

    m_oshouldConvertIsolatedSpeechDataIntoParameters = (Boolean.valueOf(
        m_headerProperties.getProperty(
        "TrainingManager.oshouldConvertIsolatedSpeechDataIntoParameters",
        "true"))).booleanValue();
    m_oshouldConvertSentenceSpeechDataIntoParameters = (Boolean.valueOf(
        m_headerProperties.getProperty(
        "TrainingManager.oshouldConvertSentenceSpeechDataIntoParameters",
        "false"))).booleanValue();
    m_oshouldCreatFileWithDynamicRanges = (Boolean.valueOf(m_headerProperties.
        getProperty("TrainingManager.oshouldCreatFileWithDynamicRanges", "true"))).
        booleanValue();

    //fine control over HMM training
    m_oshouldCreateMonophoneHMMPrototypes = (Boolean.valueOf(m_headerProperties.
        getProperty("TrainingManager.oshouldCreateMonophoneHMMPrototypes",
                    "true"))).booleanValue();
    m_oshouldCreateMonophoneHMMWithBaumWelchForIsolatedSegments = (Boolean.
        valueOf(m_headerProperties.getProperty(
        "TrainingManager.oshouldCreateMonophoneHMMWithBaumWelchForIsolatedSegments",
        "true"))).booleanValue();
    m_oshouldRecursivelyCreateMonophoneHMMWithBaumWelchForIsolatedSegments = (
        Boolean.valueOf(m_headerProperties.getProperty("TrainingManager.oshouldRecursivelyCreateMonophoneHMMWithBaumWelchForIsolatedSegments",
        "true"))).booleanValue();
    m_oshouldRecursivelyCreateMonophoneHMMWithSegmentalKMeansForIsolatedSegments = (
        Boolean.valueOf(m_headerProperties.getProperty("TrainingManager.oshouldRecursivelyCreateMonophoneHMMWithSegmentalKMeansForIsolatedSegments",
        "false"))).booleanValue();
    m_oshouldCreateMonophoneHMMWithEmbeddedBaumWelch = (Boolean.valueOf(
        m_headerProperties.getProperty(
        "TrainingManager.oshouldCreateMonophoneHMMWithEmbeddedBaumWelch",
        "false"))).booleanValue();
    m_oshouldRecursivelyCreateMonophoneHMMWithEmbeddedBaumWelch = (Boolean.
        valueOf(m_headerProperties.getProperty(
        "TrainingManager.oshouldRecursivelyCreateMonophoneHMMWithEmbeddedBaumWelch",
        "false"))).booleanValue();

    //avoid error
    if (m_oshouldRecursivelyCreateMonophoneHMMWithEmbeddedBaumWelch) {
      m_oshouldCreateMonophoneHMMWithEmbeddedBaumWelch = true;
    }

    if (m_oshouldRecursivelyCreateMonophoneHMMWithBaumWelchForIsolatedSegments) {
      m_oshouldCreateMonophoneHMMWithBaumWelchForIsolatedSegments = true;
    }

    m_oshouldCopyDatabaseToUniqueDirectory = (Boolean.valueOf(
        m_headerProperties.getProperty(
        "TrainingManager.oshouldCopyDatabaseToUniqueDirectory", "false"))).
        booleanValue();
  }

//	public CMProperty[] getTestingPropertiesReference() {
//		CMProperty[] testingCMProperties = null;
//		//if (m_testingCMProperties == null) {
//			//get one
//			if (m_outputTSTFileName != null) {
//				if (new File(m_outputTSTFileName).exists()) {
//				   testingCMProperties = CMUtilities.getAllCMPropertiesFromFile(m_outputTSTFileName,
//					IO.m_NEW_LINE, ConfigurationManipulator.KEYWORDS);
//				}
//			} else {
//				testingCMProperties = OffLineIsolatedSegmentsClassifier.getDefaultProperties();
//			}
//		//}
//		return testingCMProperties;
//	}

  //public void setTestingPropertiesReference(CMProperty[] properties) {
  //	m_testingCMProperties = properties;
  //}

  private void
      createFileOrientedWAVDataLocatorsFileForMonophoneIsolatedSegments(boolean
      oisTraining) {
    int nnumberOfEntries = m_tableOfLabels.getNumberOfEntries();
    String transcriptionDirectory = null;
    String speechDirectory = null;
    String propertyIdentifier = null;
    if (oisTraining) {
      transcriptionDirectory = m_simulationFilesAndDirectories.
          getIsolatedMonophoneTranscriptionsOfTrainSpeechDataDirectory();
      speechDirectory = m_simulationFilesAndDirectories.
          getTrainSpeechDataRootDirectory();
      propertyIdentifier = "TrainFileName_";
    }
    else {
      transcriptionDirectory = m_simulationFilesAndDirectories.
          getIsolatedMonophoneTranscriptionsOfTestSpeechDataDirectory();
      speechDirectory = m_simulationFilesAndDirectories.
          getTestSpeechDataRootDirectory();
      propertyIdentifier = "TestFileName_";
    }
    //Print.dialog("speechDirectory " + speechDirectory);
    FileNamesAndDirectories.createDirectoriesIfNecessary(transcriptionDirectory);
    if (m_nverbose > 0) {
      Print.dialog("Transcriptions: " + transcriptionDirectory);
    }
    Print.setJProgressBarRange(0, nnumberOfEntries);
    for (int i = 0; i < nnumberOfEntries; i++) {
      //get output file name
      String fileName = m_simulationFilesAndDirectories.getPrefferedName(
          m_tableOfLabels, i, DataLocator.m_FILE_EXTENSION);

      String absolutePathFileName = transcriptionDirectory + fileName;

      String fileNameToBeWrittenInProperties = null;
      if (m_ouseAbsolutePath) {
        //pre-appended directory
        fileNameToBeWrittenInProperties = absolutePathFileName;
      }
      else {
        //use only file name
        fileNameToBeWrittenInProperties = fileName;
      }

      if (m_nverbose > 2) {
        Print.dialog("Creating file " + absolutePathFileName);
      }

      //the first parameter in method below is the directory
      FileOrientedDataLocatorsFileCreator.writeDataLocatorsFile(speechDirectory +
          m_tableOfLabels.getFirstLabel(i),
          absolutePathFileName,
          m_tableOfLabels.getFirstLabel(i));
      //update the Properties of this object, indicating the file name
      m_headerProperties.setProperty("DataLocator." + propertyIdentifier +
                                     Integer.toString(i),
                                     fileNameToBeWrittenInProperties);
      Print.updateJProgressBar(i + 1);
    }
  }

  /**
   *  Description of the Method
   *
   *@param  oisTraining  Description of Parameter
   */
  private void createWAVDataLocatorsFileForMonophoneIsolatedSegmentsOLDVERSION(boolean
      oisTraining) {
    boolean oshouldModifyAX_H = true;
    //for each table entry, create the files solicited by user
    int nnumberOfEntries = m_tableOfLabels.getNumberOfEntries();
    if (m_nverbose > 1) {
      Print.dialog("Creating DataLocator files for monophone isolated segments");
    }
    String transcriptionDirectory = null;
    String speechDirectory = null;
    String propertyIdentifier = null;
    TIMITDataLocatorsFileCreator tIMITDataLocatorsFileCreator = null;
    if (oisTraining) {
      transcriptionDirectory = m_simulationFilesAndDirectories.
          getIsolatedMonophoneTranscriptionsOfTrainSpeechDataDirectory();
      speechDirectory = m_simulationFilesAndDirectories.
          getTrainSpeechDataRootDirectory();
      propertyIdentifier = "TrainFileName_";
    }
    else {
      transcriptionDirectory = m_simulationFilesAndDirectories.
          getIsolatedMonophoneTranscriptionsOfTestSpeechDataDirectory();
      speechDirectory = m_simulationFilesAndDirectories.
          getTestSpeechDataRootDirectory();
      propertyIdentifier = "TestFileName_";
    }
    FileNamesAndDirectories.createDirectoriesIfNecessary(transcriptionDirectory);

    //create initial list of files for TIMIT
    if (m_databaseType == Database.Type.TIMIT) {
      tIMITDataLocatorsFileCreator = new TIMITDataLocatorsFileCreator();
      tIMITDataLocatorsFileCreator.getListOfFilesBelowGivenDirectory(
          speechDirectory, "WAV");
    }

    for (int i = 0; i < nnumberOfEntries; i++) {
      //get output file name
      String fileName = m_simulationFilesAndDirectories.getPrefferedName(
          m_tableOfLabels, i, DataLocator.m_FILE_EXTENSION);

      String absolutePathFileName = transcriptionDirectory + fileName;

      String fileNameToBeWrittenInProperties = null;
      if (m_ouseAbsolutePath) {
        //pre-appended directory
        fileNameToBeWrittenInProperties = absolutePathFileName;
      }
      else {
        //use only file name
        fileNameToBeWrittenInProperties = fileName;
      }

      if (m_nverbose > 2) {
        Print.dialog("Creating file " + absolutePathFileName);
      }

      if (m_databaseType == Database.Type.GENERAL) {
        //the first parameter in method below is the directory
        FileOrientedDataLocatorsFileCreator.writeDataLocatorsFile(
            speechDirectory + m_tableOfLabels.getFirstLabel(i),
            absolutePathFileName,
            m_tableOfLabels.getFirstLabel(i));
      }
      else {
        //TIMIT
        tIMITDataLocatorsFileCreator.includeTokenOccurrences(m_tableOfLabels, i,
            "PHN", m_ouseAbsolutePath, oshouldModifyAX_H);
        //tIMITSegmentsFileCreator.compileTableAndEliminateSilenceInBeginAndEnd();
        //create header
        String header = "NumberOfLabels = " +
            m_tableOfLabels.getNumberOfLabels(i) +
            "\r\nLabels = " + m_tableOfLabels.getLabelsAsString(i) +
            "\r\nDataLocator.Type = " +
            DataLocator.Type.LABELS_AND_ENDPOINTS.toString();
        header += IO.m_NEW_LINE + "DatabaseManager.nnumberOfDataLocators = " +
            tIMITDataLocatorsFileCreator.getNumberOfDataLocatorsInFinalList();
        if (!m_ouseAbsolutePath) {
          header = header.concat(IO.m_NEW_LINE +
                                 "DatabaseManager.RootDirectory = " +
                                 speechDirectory);
        }
        header = FileWithHeaderWriter.formatHeader(header);

        //Print.dialog(absolutePathFileName);
        tIMITDataLocatorsFileCreator.writeDataLocatorsFile(absolutePathFileName,
            header);
        tIMITDataLocatorsFileCreator.resetFinalList();
      }
      //update the Properties of this object, indicating the file name
      m_headerProperties.setProperty("DataLocator." + propertyIdentifier +
                                     Integer.toString(i),
                                     fileNameToBeWrittenInProperties);
    }
  }

  private DataLocator extractDataLocatorsOfTableEntry(int ntableEntry,
      DataLocator dataLocator) {
    int nnumberOfSegments = dataLocator.getNumberOfSegments();
    StringBuffer stringBuffer = new StringBuffer();
    int nmatches = 0;
    for (int i = 0; i < nnumberOfSegments; i++) {
      String label = dataLocator.getLabelFromGivenSegment(i);
      if (m_tableOfLabels.isAMatch(ntableEntry, label)) {
        stringBuffer.append(" " + dataLocator.getGivenSegmentWithoutFileName(i));
        nmatches++;
      }
    }
    if (nmatches > 0) {
      String temp = null;
      if (m_ouseAbsolutePath) {
        temp = dataLocator.getFileName();
      }
      else {
        temp = FileNamesAndDirectories.getFileNameFromPath(dataLocator.
            getFileName());
      }
      StringBuffer finalStringBuffer = new StringBuffer(temp);
      finalStringBuffer.append(" " + nmatches);
      finalStringBuffer.append(stringBuffer.toString());
      return new DataLocator(DataLocator.Type.LABELS_AND_ENDPOINTS,
                             finalStringBuffer.toString());
    }
    else {
      return null;
    }
  }

  private void createWAVDataLocatorsFileForMonophoneIsolatedSegments(boolean
      oisTraining) {
    //for each table entry, create the files solicited by user
    int nnumberOfEntries = m_tableOfLabels.getNumberOfEntries();
    String transcriptionDirectory = null;
    String inputTranscriptionFileName = null;
    //String speechDirectory = null;
    String propertyIdentifier = null;
    //TIMITDataLocatorsFileCreator tIMITDataLocatorsFileCreator = null;
    if (oisTraining) {
      transcriptionDirectory = m_simulationFilesAndDirectories.
          getIsolatedMonophoneTranscriptionsOfTrainSpeechDataDirectory();
      inputTranscriptionFileName = m_simulationFilesAndDirectories.
          getSentenceMonophoneTranscriptionsOfTrainSpeechDataFileName();
      //speechDirectory = m_simulationFilesAndDirectories.getTrainSpeechDataRootDirectory();
      propertyIdentifier = "TrainFileName_";
    }
    else {
      transcriptionDirectory = m_simulationFilesAndDirectories.
          getIsolatedMonophoneTranscriptionsOfTestSpeechDataDirectory();
      inputTranscriptionFileName = m_simulationFilesAndDirectories.
          getSentenceMonophoneTranscriptionsOfTestSpeechDataFileName();
      //speechDirectory = m_simulationFilesAndDirectories.getTestSpeechDataRootDirectory();
      propertyIdentifier = "TestFileName_";
    }
    FileNamesAndDirectories.createDirectoriesIfNecessary(transcriptionDirectory);
    if (m_nverbose > 0) {
      Print.dialog("Transcriptions (isolated segments): " +
                   IO.getEndOfString(transcriptionDirectory, 40));
    }

    //save to report file
    IO.appendStringWithTimeDateToEndOfTextFile(
        FileNamesAndDirectories.getParent(transcriptionDirectory) +
        m_reportFileName,
        "Creating DataLocator files for monophone isolated segments at directory " +
        transcriptionDirectory);

    DatabaseManager databaseManager = new DatabaseManager(
        inputTranscriptionFileName);

    BufferedWriter[] bufferedWriters = new BufferedWriter[nnumberOfEntries];
    for (int i = 0; i < nnumberOfEntries; i++) {
      String fileName = m_simulationFilesAndDirectories.getPrefferedName(
          m_tableOfLabels, i, DataLocator.m_FILE_EXTENSION);

      String absolutePathFileName = transcriptionDirectory + fileName;

      try {
        bufferedWriters[i] = new BufferedWriter(new FileWriter(
            absolutePathFileName));
      }
      catch (IOException e) {
        e.printStackTrace();
        Print.error("Problem opening file " + absolutePathFileName);
        End.exit();
      }

      String fileNameToBeWrittenInProperties = null;
      if (m_ouseAbsolutePath) {
        //pre-appended directory
        fileNameToBeWrittenInProperties = absolutePathFileName;
      }
      else {
        //use only file name
        fileNameToBeWrittenInProperties = fileName;
      }
      //update the Properties of this object, indicating the file name
      m_headerProperties.setProperty("DataLocator." + propertyIdentifier +
                                     Integer.toString(i),
                                     fileNameToBeWrittenInProperties);

      //create header
      String header = "NumberOfLabels = " + m_tableOfLabels.getNumberOfLabels(i) +
          "\r\nLabels = " + m_tableOfLabels.getLabelsAsString(i) +
          "\r\nDataLocator.Type = " +
          DataLocator.Type.LABELS_AND_ENDPOINTS.toString();
      //can't use it here because don't know total number yet...
      //header += IO.m_NEW_LINE + "DatabaseManager.nnumberOfDataLocators = " + tIMITDataLocatorsFileCreator.getNumberOfDataLocatorsInFinalList();
      if (!m_ouseAbsolutePath) {
        header = header.concat(IO.m_NEW_LINE +
                               "DatabaseManager.RootDirectory = " +
                               databaseManager.getGivenRootDirectory(0));
      }
      header += IO.m_NEW_LINE + FileWithHeaderWriter.m_endOfHeaderIdentifier;
      try {
        bufferedWriters[i].write(header);
        bufferedWriters[i].newLine();
      }
      catch (IOException e) {
        e.printStackTrace();
        Print.error("Error writing to DataLocator file # " + i);
        End.exit();
      }
    }

    //go through all sentences and extract segments which match table entry

    while (databaseManager.isThereDataToRead()) {
      DataLocator dataLocator = databaseManager.getNextDataLocator();
      for (int i = 0; i < nnumberOfEntries; i++) {
        DataLocator labelsOfGivenEntryDataLocator =
            extractDataLocatorsOfTableEntry(i, dataLocator);
        if (labelsOfGivenEntryDataLocator != null) {
          try {
            bufferedWriters[i].write(labelsOfGivenEntryDataLocator.toString());
            bufferedWriters[i].newLine();
          }
          catch (IOException e) {
            e.printStackTrace();
            Print.error("Error writing to DataLocator file # " + i);
            End.exit();
          }
        }
      }
    }

    databaseManager.finalizeDataReading();
    for (int i = 0; i < nnumberOfEntries; i++) {
      try {
        bufferedWriters[i].close();
      }
      catch (IOException e) {
        e.printStackTrace();
        Print.error("Problem closing files.");
        End.exit();
      }
    }

    //save to report file
    IO.appendStringWithTimeDateToEndOfTextFile(
        FileNamesAndDirectories.getParent(transcriptionDirectory) +
        m_reportFileName,
        "Finished creating DataLocator files at directory " +
        transcriptionDirectory);
  }

  /**
   *  Description of the Method
   *
   *@param  oisTraining  Description of Parameter
   */
  private void createWAVDataLocatorsFileForMonophoneSentences(boolean
      oisTraining, String fileExtension) {

    //TODO: generate several transcriptions
    //default is 1
    //String trainingManagerType = m_headerProperties.getProperty("TrainingManager.nnumberOfDataLocatorFiles",
    //															"1");
    String transcriptionFileName = null;
    String speechDirectory = null;
    String propertyIdentifier = null;
    if (oisTraining) {
      transcriptionFileName = m_simulationFilesAndDirectories.
          getSentenceMonophoneTranscriptionsOfTrainSpeechDataFileName();
      speechDirectory = m_simulationFilesAndDirectories.
          getTrainSpeechDataRootDirectory();
      propertyIdentifier = "TrainFileName";
    }
    else {
      transcriptionFileName = m_simulationFilesAndDirectories.
          getSentenceMonophoneTranscriptionsOfTestSpeechDataFileName();
      speechDirectory = m_simulationFilesAndDirectories.
          getTestSpeechDataRootDirectory();
      propertyIdentifier = "TestFileName";
    }

    if (new File(transcriptionFileName).exists()) {

      String property = m_headerProperties.getProperty(
          "TrainingManager.oshouldSkipReestimationIfFileExists", "true");
      boolean oshouldSkipReestimationIfFileExists = (Boolean.valueOf(property)).
          booleanValue();
      if (oshouldSkipReestimationIfFileExists) {
        if (m_nverbose > 0) {
          Print.dialog("Skipping because " +
                       IO.getEndOfString(transcriptionFileName, 45) +
                       " already exists");
        }
        return;
      }
    }

    if (m_nverbose > 0) {
      Print.dialog("Writing transcription " +
                   IO.getEndOfString(transcriptionFileName, 45));
    }

    FileNamesAndDirectories.createDirectoriesIfNecessary(
        FileNamesAndDirectories.getPathFromFileName(transcriptionFileName));

    //save to report file
    IO.appendStringWithTimeDateToEndOfTextFile(
        FileNamesAndDirectories.getPathFromFileName(transcriptionFileName) +
        m_reportFileName,
        "Creating " + transcriptionFileName + " for monophone sentences");

    TIMITDataLocatorsFileCreator tIMITDataLocatorsFileCreator = new
        TIMITDataLocatorsFileCreator();
    tIMITDataLocatorsFileCreator.getListOfFilesBelowGivenDirectory(
        speechDirectory, fileExtension);
    //make sure there are no SA sentences
    //tIMITDataLocatorsFileCreator.excludeTextTypeFromFinalList("sa");
    tIMITDataLocatorsFileCreator.copyInitialToFinalList();

    if (tIMITDataLocatorsFileCreator.getNumberOfDataLocatorsInFinalList() < 1) {
      End.throwError(
          "Did not find any file with extension " + fileExtension + " under directory " +
          speechDirectory);
    }

    //now we create one file with all the sentences found
    String absolutePathFileName = transcriptionFileName;
    String fileName = FileNamesAndDirectories.getFileNameFromPath(
        absolutePathFileName);

    String fileNameToBeWrittenInProperties = null;

    if (m_ouseAbsolutePath) {
      //pre-appended directory
      fileNameToBeWrittenInProperties = absolutePathFileName;
      tIMITDataLocatorsFileCreator.
          compileTableAndAddAllLabelsAndEndpointsUsingPath("PHN", true);
    }
    else {
      //use only file name
      fileNameToBeWrittenInProperties = fileName;
      tIMITDataLocatorsFileCreator.
          compileTableAndAddAllLabelsAndEndpointsUsingFileNames("PHN", true);
    }

    //create directories if necessary
    String directory = FileNamesAndDirectories.getParent(absolutePathFileName);
    FileNamesAndDirectories.createDirectoriesIfNecessary(directory);

    //create header
    String header = "DataLocator.Type = " +
        DataLocator.Type.LABELS_AND_ENDPOINTS.toString();
    header += IO.m_NEW_LINE + "DatabaseManager.nnumberOfDataLocators = " +
        tIMITDataLocatorsFileCreator.getNumberOfDataLocatorsInFinalList();
    if (!m_ouseAbsolutePath) {
      header = header.concat(IO.m_NEW_LINE + "DatabaseManager.RootDirectory = " +
                             speechDirectory);
    }
    header = FileWithHeaderWriter.formatHeader(header);
    tIMITDataLocatorsFileCreator.writeDataLocatorsFile(absolutePathFileName,
        header);

    m_headerProperties.setProperty("DataLocator." + propertyIdentifier,
                                   fileNameToBeWrittenInProperties);

    //save to report file
    IO.appendStringWithTimeDateToEndOfTextFile(
        FileNamesAndDirectories.getPathFromFileName(transcriptionFileName) +
        m_reportFileName,
        "Finished creating " + transcriptionFileName + ".");
  }

  /**
   *  Description of the Method
   *
   *@param  oisTraining  Description of Parameter
   */
  //used for GENERAL. Otherwise we 'cut' the sentence SOP's
  private void createSetOfPatternsFilesForIsolatedSegments(boolean oisTraining) {

    //for each table entry, create the files solicited by user
    int nnumberOfEntries = m_tableOfLabels.getNumberOfEntries();

    SetOfPatternsGenerator setOfPatternsGenerator = new SetOfPatternsGenerator();

    String propertyIdentifier = null;
    String speechDirectory = null;
    String parametersDirectory = null;
    if (oisTraining) {
      propertyIdentifier = "TrainFileName_";
      speechDirectory = m_simulationFilesAndDirectories.
          getIsolatedMonophoneTranscriptionsOfTrainSpeechDataDirectory();
      parametersDirectory = m_simulationFilesAndDirectories.
          getIsolatedTrainParametersDataRootDirectory();
    }
    else {
      propertyIdentifier = "TestFileName_";
      speechDirectory = m_simulationFilesAndDirectories.
          getIsolatedMonophoneTranscriptionsOfTestSpeechDataDirectory();
      parametersDirectory = m_simulationFilesAndDirectories.
          getIsolatedTestParametersDataRootDirectory();
    }

    if (m_nverbose > 0) {
      Print.dialog("Running front end for files at " + speechDirectory);
    }

    //save to report file
    FileNamesAndDirectories.createDirectoriesIfNecessary(parametersDirectory);
    IO.appendStringWithTimeDateToEndOfTextFile(
        FileNamesAndDirectories.getParent(parametersDirectory) +
        m_reportFileName,
        "Creating " + SetOfPatterns.m_FILE_EXTENSION +
        " files for isolated segments at directory " + parametersDirectory);

    Print.setJProgressBarRange(0, nnumberOfEntries);
    for (int i = 0; i < nnumberOfEntries; i++) {
      //create name to use as default value
      String prefferedDataLocatorFileName = m_simulationFilesAndDirectories.
          getPrefferedName(m_tableOfLabels, i, DataLocator.m_FILE_EXTENSION);
      String dataLocatorFileName = m_headerProperties.getProperty(
          "DataLocator." + propertyIdentifier + Integer.toString(i),
          prefferedDataLocatorFileName);
      dataLocatorFileName = FileNamesAndDirectories.getAbsolutePath(
          dataLocatorFileName, speechDirectory);
      if (! (new File(dataLocatorFileName)).exists()) {
        End.throwError("Could not find file " + dataLocatorFileName +
                       ". Can you check property DataLocator." +
                       propertyIdentifier +
                       i + " in TRN file ?");
      }

      //TODO: I am assuming only 1 file
      DatabaseManager databaseManager = new DatabaseManager(dataLocatorFileName);

      //get output file name
      String fileName = m_simulationFilesAndDirectories.getPrefferedName(
          m_tableOfLabels, i, SetOfPatterns.m_FILE_EXTENSION);
      String absolutePathFileName = parametersDirectory + fileName;
      String fileNameToBeWrittenInProperties = null;
      if (m_ouseAbsolutePath) {
        //pre-appended directory
        fileNameToBeWrittenInProperties = absolutePathFileName;
      }
      else {
        //use only file name
        fileNameToBeWrittenInProperties = fileName;
      }

      SetOfPatterns setOfPatterns = setOfPatternsGenerator.getSetOfPatterns(
          databaseManager,
          m_patternGenerator);
      double dpercentageOfInvalidPatterns = setOfPatternsGenerator.
          getPercentageOfInvalidPatterns();
      if (dpercentageOfInvalidPatterns > 0.0) {
        //add a comment in properties
        m_headerProperties.setProperty("#Percentage of invalid patterns in " +
                                       fileNameToBeWrittenInProperties,
                                       Double.toString(
            dpercentageOfInvalidPatterns));
      }

      if (m_nverbose > 0) {
        Print.dialog("Writing " + setOfPatterns.getNumberOfPatterns() +
                     " tokens to " + IO.getEndOfString(absolutePathFileName, 40));
      }

      setOfPatterns.writeToFile(absolutePathFileName,
                                dataLocatorFileName,
                                m_tableOfLabels.getLabelsAsString(i));

      //update the Properties of this object
      m_headerProperties.setProperty("SetOfPatterns." + propertyIdentifier +
                                     Integer.toString(i),
                                     fileNameToBeWrittenInProperties);
      Print.updateJProgressBar(i + 1);
    }

    //save to report file
    IO.appendStringWithTimeDateToEndOfTextFile(
        FileNamesAndDirectories.getParent(parametersDirectory) +
        m_reportFileName,
        "Finished creating SOP files at " + parametersDirectory);
  }

  //in this case I am not using SetOfPatternsGenerator.getSetOfPatterns()
  //because I want 1 SOP file per sentence, instead of a possibly huge SOP
  //with all sentences.
  /**
   *  Description of the Method
   *
   *@param  oisTraining  Description of Parameter
   */
  private void createSetOfPatternsFilesForSentences(boolean oisTraining) {

    String parametersDirectory = null;
    String inputWAVDataLocatorFileName = null;
    String speechDirectory = null;
    if (oisTraining) {
      parametersDirectory = m_simulationFilesAndDirectories.
          getSentenceTrainParametersDataRootDirectory();
      inputWAVDataLocatorFileName = m_simulationFilesAndDirectories.
          getSentenceMonophoneTranscriptionsOfTrainSpeechDataFileName();
      speechDirectory = m_simulationFilesAndDirectories.
          getTrainSpeechDataRootDirectory();
    }
    else {
      parametersDirectory = m_simulationFilesAndDirectories.
          getSentenceTestParametersDataRootDirectory();
      inputWAVDataLocatorFileName = m_simulationFilesAndDirectories.
          getSentenceMonophoneTranscriptionsOfTestSpeechDataFileName();
      speechDirectory = m_simulationFilesAndDirectories.
          getTestSpeechDataRootDirectory();
    }

    //create directories
    FileNamesAndDirectories.createDirectoriesIfNecessary(parametersDirectory);

    if (m_nverbose > 0) {
      Print.dialog("Running front end (sentences): " +
                   IO.getEndOfString(parametersDirectory, 50));
    }

    //save to report file
    IO.appendStringWithTimeDateToEndOfTextFile(
        FileNamesAndDirectories.getParent(parametersDirectory) +
        m_reportFileName,
        "Creating " + SetOfPatterns.m_FILE_EXTENSION +
        " files for sentences at directory " + parametersDirectory);

    //Vector vectorOfSopFileNames = new Vector();
    //TODO: I am assuming only 1 file

    DatabaseManager databaseManager = new DatabaseManager(
        inputWAVDataLocatorFileName);

    int ntotalNumberOfDataLocators = databaseManager.getNumberOfDataLocators();
    Print.setJProgressBarRange(0, ntotalNumberOfDataLocators);

    int nlengthOfdatabaseRootDirectoryString = speechDirectory.length();

    DataLocator sopDataLocator = null;
    int ncounter = 0;
    while (databaseManager.isThereDataToRead()) {
      DataLocator dataLocator = databaseManager.getNextDataLocator();

      LabeledSpeech labeledSpeech = new LabeledSpeech(dataLocator);

      //I could read only the Audio for such segment with:
      //Audio audio = labeledSpeech.getAudioFromGivenSegment(0);
      //That wouldn't include silence in begin and end and save
      //computation. But instead, I will
      //read the whole Audio (with silence regions) because that
      //keep DTL information consistent among waveforms and SOP's
      //and besides, I can later need to create models for silence
      Audio audio = labeledSpeech.getAudioOfWholeSentence();

      //check if the audio has the same sample frequency and is mono
      AudioFormat audioFormat = audio.getAudioFormat();
      if (audioFormat.getSampleRate() !=
          m_patternGenerator.getSpeechSamplingRate()) {
        End.throwError("File " + dataLocator.getFileName() +
                       " has sampling frequency " +
                       audioFormat.getSampleRate() +
                       ", while the PatternGenerator is expecting " +
                       m_patternGenerator.getSpeechSamplingRate());
      }
      if (audioFormat.getChannels() != 1) {
        End.throwError("File " + dataLocator.getFileName() + " is not mono." +
                       " It has " + audioFormat.getChannels() + " channels.");
      }

      Pattern pattern = m_patternGenerator.getPattern(audio);

      if (pattern == null) {
        //whole sentence should be long enough to have a valid Pattern
        End.throwError("Error in " + dataLocator.toString() +
                       ": null Pattern !");
      }

      //create one SOP per sentence
      //System.out.println("Pattern length: "+pattern.getNumOfFrames());
      SetOfPatterns setOfPatterns = new SetOfPatterns(m_patternGenerator);
      setOfPatterns.addPattern(pattern);

      String thisDataLocatorInputFileName = null;
      String thisSOPOutputFileName = null;
      String absolutePathFileName = null;
      if (m_ouseAbsolutePath) {
        thisDataLocatorInputFileName = dataLocator.getFileName();
      }
      else {
        thisDataLocatorInputFileName = dataLocator.getFileName();
      }

      //Print.dialog(thisDataLocatorInputFileName);

//ak
//			PathOrganizer pathOrganizer = null;
//
//			if (m_databaseType  == Database.Type.TIMIT) {
//				pathOrganizer = new TIMITPathOrganizer(thisDataLocatorInputFileName);
//			} else if (m_databaseType  == Database.Type.TIDIGITS) {
//				pathOrganizer = new TIDigitsPathOrganizer(thisDataLocatorInputFileName);
//			} else {
//				End.throwError("This method cannot be used with databases of type = " + m_databaseType.toString());
//			}
//
//			if (!pathOrganizer.isPathOk()) {
//				End.throwError(thisDataLocatorInputFileName + " is not a valid path");
//			}
      if (m_ouseAbsolutePath) {
        thisDataLocatorInputFileName = thisDataLocatorInputFileName; //pathOrganizer.toString();
        //take out 'test' or 'train' string
        int nindexOfFirstSlash = thisDataLocatorInputFileName.indexOf("/");
        thisDataLocatorInputFileName = thisDataLocatorInputFileName.substring(
            nindexOfFirstSlash + 1, thisDataLocatorInputFileName.length());
        thisSOPOutputFileName = FileNamesAndDirectories.substituteExtension(
            thisDataLocatorInputFileName, setOfPatterns.m_FILE_EXTENSION);
        absolutePathFileName = FileNamesAndDirectories.concatenateTwoPaths(
            parametersDirectory, thisSOPOutputFileName);
      }
      else {
        thisDataLocatorInputFileName = thisDataLocatorInputFileName; //akpathOrganizer.getUniqueName();
        thisDataLocatorInputFileName = FileNamesAndDirectories.
            getFileNameFromPath(thisDataLocatorInputFileName);
        thisSOPOutputFileName = FileNamesAndDirectories.substituteExtension(
            thisDataLocatorInputFileName, setOfPatterns.m_FILE_EXTENSION);
        absolutePathFileName = parametersDirectory + thisSOPOutputFileName;
      }
      //preappend dataLocatorFileName to the 'debug' info that will be written in DTL file
      //thisDataLocatorInputFileName = outputSOPDataLocatorFileName +
      //		", file: " +
      //		thisDataLocatorInputFileName;
      //write SOP file

      FileNamesAndDirectories.createDirectoriesIfNecessaryGivenFileName(
          absolutePathFileName);

      setOfPatterns.writeToFile(absolutePathFileName,
                                thisDataLocatorInputFileName,
                                "sentence");

      if (m_nverbose > 1) {
        Print.dialog("Writing file " + absolutePathFileName);
      }

      ncounter++;
      Print.updateJProgressBar(ncounter);
    }
    databaseManager.finalizeDataReading();
    //save to report file
    IO.appendStringWithTimeDateToEndOfTextFile(
        FileNamesAndDirectories.getParent(parametersDirectory) +
        m_reportFileName,
        "Finished creating " + SetOfPatterns.m_FILE_EXTENSION +
        " files for sentences at directory " + parametersDirectory);
  }

  /**
   *  Should get info from DTL of ISOLATED_SEGMENTS, find the associated SOP and
   *  cut it.
   *
   *@param  oisTraining  Description of Parameter
   */
  private void cutSetOfPatternsFilesForSentencesAndWriteIsolatedSegmentsSOPs(boolean
      oisTraining) {

    //for each table entry, create the files solicited by user
    int nnumberOfEntries = m_tableOfLabels.getNumberOfEntries();

    String directoryForSetOfPatternsOfSentences = null;
    String directoryForSetOfPatternsOfIsolated = null;
    String propertyIdentifier = null;
    String directoryForTranscriptions = null;
    if (oisTraining) {
      directoryForSetOfPatternsOfSentences = m_simulationFilesAndDirectories.
          getSentenceTrainParametersDataRootDirectory();
      directoryForSetOfPatternsOfIsolated = m_simulationFilesAndDirectories.
          getIsolatedTrainParametersDataRootDirectory();
      directoryForTranscriptions = m_simulationFilesAndDirectories.
          getIsolatedMonophoneTranscriptionsOfTrainSpeechDataDirectory();
      propertyIdentifier = "TrainFileName_";
    }
    else {
      directoryForSetOfPatternsOfSentences = m_simulationFilesAndDirectories.
          getSentenceTestParametersDataRootDirectory();
      directoryForSetOfPatternsOfIsolated = m_simulationFilesAndDirectories.
          getIsolatedTestParametersDataRootDirectory();
      directoryForTranscriptions = m_simulationFilesAndDirectories.
          getIsolatedMonophoneTranscriptionsOfTestSpeechDataDirectory();
      propertyIdentifier = "TestFileName_";
    }

    if (m_nverbose > 0) {
      Print.dialog("\"Cutting\" sentence features: " +
                   IO.getEndOfString(directoryForSetOfPatternsOfIsolated, 45));
    }

    //save to report file
    FileNamesAndDirectories.createDirectoriesIfNecessary(
        FileNamesAndDirectories.getParent(directoryForSetOfPatternsOfIsolated));
    IO.appendStringWithTimeDateToEndOfTextFile(
        FileNamesAndDirectories.getParent(directoryForSetOfPatternsOfIsolated) +
        m_reportFileName,
        "Creating " + SetOfPatterns.m_FILE_EXTENSION +
        " files for isolated segments at directory " +
        directoryForSetOfPatternsOfIsolated);

    Print.setJProgressBarRange(0, nnumberOfEntries);
    for (int ntableEntry = 0; ntableEntry < nnumberOfEntries; ntableEntry++) {
      //create name to use as default value
      String prefferedDataLocatorFileName = m_simulationFilesAndDirectories.
          getPrefferedName(m_tableOfLabels, ntableEntry,
                           DataLocator.m_FILE_EXTENSION);
      String dataLocatorFileName = m_headerProperties.getProperty(
          "DataLocator." + propertyIdentifier + Integer.toString(ntableEntry),
          prefferedDataLocatorFileName);
      dataLocatorFileName = FileNamesAndDirectories.getAbsolutePath(
          dataLocatorFileName, directoryForTranscriptions);
      if (! (new File(dataLocatorFileName)).exists()) {
        End.throwError("Could not find file " + dataLocatorFileName +
                       ". Can you check property DataLocator." +
                       propertyIdentifier +
                       ntableEntry + " in TRN file ?");
      }

      //TODO: I am assuming only 1 file
      DatabaseManager databaseManager = new DatabaseManager(dataLocatorFileName);

      //get output file name
      String fileName = m_simulationFilesAndDirectories.getPrefferedName(
          m_tableOfLabels, ntableEntry, SetOfPatterns.m_FILE_EXTENSION);
      String absolutePathFileName = directoryForSetOfPatternsOfIsolated +
          fileName;
      //make sure directory exists
      FileNamesAndDirectories.createDirectoriesIfNecessary(
          directoryForSetOfPatternsOfIsolated);
      String fileNameToBeWrittenInProperties = null;
      if (m_ouseAbsolutePath) {
        //pre-appended directory
        fileNameToBeWrittenInProperties = absolutePathFileName;
      }
      else {
        //use only file name
        fileNameToBeWrittenInProperties = fileName;
      }
      //open the SOP to write to
      SetOfPatterns outputSetOfPatterns = new SetOfPatterns(m_patternGenerator);

      while (databaseManager.isThereDataToRead()) {
        DataLocator dataLocator = databaseManager.getNextDataLocator();
        if (dataLocator == null) {
          if (databaseManager.isThereDataToRead()) {
            End.throwError("Null speech data !");
          }
        }
        else if (dataLocator.getFileName().equals("NO_FILES_FOUND")) {
          End.throwError("There is no data in " + dataLocatorFileName);
        }
        else {
          if (m_nverbose > 1) {
            Print.dialog("Processing " + dataLocator.toString());
          }

          //get the SOP file name for this wav file
          String sOPFileName = m_simulationFilesAndDirectories.
              convertWAVToSOPFileName(dataLocator.getFileName(),
                                      directoryForSetOfPatternsOfSentences,
                                      oisTraining);

          //open such SetOfPatterns
          SetOfPatterns inputSetOfPatterns = new SetOfPatterns(sOPFileName);

          if (inputSetOfPatterns.getNumberOfPatterns() != 1) {
            End.throwError(sOPFileName + " should have only 1 pattern!");
          }

          Pattern inputPattern = inputSetOfPatterns.getPattern(0);

          //ak: used when the wav from header of FEA doesn't match (Seneff preparation)
          //String tt = FileNamesAndDirectories.getFileNameFromPath(dataLocator.getFileName());
          //if (oisTraining) {
          // tt = "/data1/databases_2dirs/timit/train/" + tt;
          //} else {
          //	tt = "/data1/databases_2dirs/timit/test/" + tt;
          //}
          //dataLocator.m_fileName = tt;

          LabeledSpeech labeledSpeech = new LabeledSpeech(dataLocator);

          for (int i = 0; i < dataLocator.getNumberOfSegments(); i++) {
            int[] nendpoints = dataLocator.getEndpointsFromGivenSegment(i);
            //find frame indices
            int[] nframeIndices = m_patternGenerator.fromSamplesToFrames(
                nendpoints);
            //create a Pattern with that frames
            if (nframeIndices[0] > inputPattern.getNumOfFrames() - 1) {
              nframeIndices[0] = inputPattern.getNumOfFrames() - 1;
            }
            if (nframeIndices[1] > inputPattern.getNumOfFrames() - 1) {
              nframeIndices[1] = inputPattern.getNumOfFrames() - 1;
            }
            Pattern outputPattern = inputPattern.getPartOfPattern(nframeIndices[
                0],
                nframeIndices[1]);

            if ( (outputPattern != null) && (outputPattern.getParameters() != null)) {
              outputSetOfPatterns.addPattern(outputPattern);
            }
          }
        }
      }
      databaseManager.finalizeDataReading();
      //save file
      
      outputSetOfPatterns.writeToFile(absolutePathFileName,
                                      dataLocatorFileName,
                                      m_tableOfLabels.getLabelsAsString(
          ntableEntry));

      //update the Properties of this object
      m_headerProperties.setProperty("SetOfPatterns." + propertyIdentifier +
                                     Integer.toString(ntableEntry),
                                     fileNameToBeWrittenInProperties);
      Print.updateJProgressBar(ntableEntry);
    }
    //save to report file
    IO.appendStringWithTimeDateToEndOfTextFile(
        FileNamesAndDirectories.getParent(directoryForSetOfPatternsOfIsolated) +
        m_reportFileName,
        "Finished creating " + SetOfPatterns.m_FILE_EXTENSION +
        " files for isolated segments at directory " +
        directoryForSetOfPatternsOfIsolated);
  }

  /**
   *  only tri or monophone
   *
   *@param  setOfHMMs          Description of Parameter
   *@param  oisTriphone        Description of Parameter
   *@param  oisTraining        Description of Parameter
   *@param  directoryWithHMMs  Description of Parameter
   */
//	private void runClassificationTest(SetOfHMMs setOfHMMs, boolean oisTriphone, boolean oisTraining,
//			String directoryWithHMMs) {
//
//		String outputDirectory = null;
//		if (oisTraining) {
//			outputDirectory = FileNamesAndDirectories.forceEndingWithSlash(directoryWithHMMs) + "classification/train/";
//		}
//		else {
//			outputDirectory = FileNamesAndDirectories.forceEndingWithSlash(directoryWithHMMs) + "classification/test/";
//		}
//		FileNamesAndDirectories.createDirectoriesIfNecessary(outputDirectory);
//
//		//choose a table
//		TableOfLabels tableOfLabelsForScoring = null;
//		if (m_databaseType == Database.Type.TIMIT) {
//			//use TIMIT39 for scoring purposes
//			if (oisTriphone) {
//				tableOfLabelsForScoring = TableOfLabels.createTableForScoringTriphones(new TableOfLabels(TableOfLabels.Type.TIMIT39),
//						HTKInterfacer.getTableFromLogicalToPhysicalHMMMapping(m_simulationFilesAndDirectories.getMappingOfLogicalIntoPhysicalTriphonesFileName()));
//			}
//			else {
//				tableOfLabelsForScoring = new TableOfLabels(TableOfLabels.Type.TIMIT39);
//			}
//		}
//		else if (m_databaseType == Database.Type.GENERAL) {
//			//get the same table used for training
//			tableOfLabelsForScoring = TableOfLabels.getTableOfLabels(m_headerProperties);
//		}
//		if (tableOfLabelsForScoring == null) {
//			End.throwError("TableOfLabels not found in m_headerProperties.");
//		}
//		else {
//			//call testing procedure
//			//						OffLineIsolatedSegmentsClassifier
//			//								offLineIsolatedSegmentsClassifier
//			//								 = new OffLineIsolatedSegmentsClassifier();
//			ClassificationStatisticsCalculator classificationStatisticsCalculator = getResultsOLD(setOfHMMs,
//					tableOfLabelsForScoring,
//					m_headerProperties,
//					oisTraining);
//
//			classificationStatisticsCalculator.generateReport(outputDirectory);
//		}
//	}

  private void writeTSTFile() {

    if (new File(m_outputTSTFileName).exists()) {
      //don't do it if already exists a file. Let the user change it
      if (m_nverbose > 0) {
        Print.dialog("Skipping writing because file " + m_outputTSTFileName +
                     " already exists");
      }
      return;
    }

    if (m_nverbose > 0) {
      Print.dialog("Writing file " + m_outputTSTFileName);
    }

    String rootDirectoryWithHMMs = m_simulationFilesAndDirectories.
        getGeneralOutputDirectory();

    String jarHMMsFileName = m_simulationFilesAndDirectories.
        getSetOfHMMsFileName();
    boolean oisTriphone = false;
    boolean oisTraining = false;

    String directoryForDataLocators = null;
    if (oisTraining) {
      directoryForDataLocators = m_simulationFilesAndDirectories.
          getIsolatedMonophoneTranscriptionsOfTrainSpeechDataDirectory();
    }
    else {
      directoryForDataLocators = m_simulationFilesAndDirectories.
          getIsolatedMonophoneTranscriptionsOfTestSpeechDataDirectory();
    }
    String directoryForSetOfPatterns = null;
    if (oisTraining) {
      directoryForSetOfPatterns = m_simulationFilesAndDirectories.
          getIsolatedTrainParametersDataRootDirectory();
    }
    else {
      directoryForSetOfPatterns = m_simulationFilesAndDirectories.
          getIsolatedTestParametersDataRootDirectory();
    }
    boolean oshouldWriteLattices = true;

    //for TIMIT oareTranscriptionsAvailable = true;
    boolean oareTranscriptionsAvailable = true;
    String tableOfMonophoneLabelsForScoringFileName = null;
    if (m_databaseType == Database.Type.TIMIT) {
      tableOfMonophoneLabelsForScoringFileName = rootDirectoryWithHMMs +
          TableOfLabels.Type.TIMIT39.toString() + "." +
          TableOfLabels.m_FILE_EXTENSION;
      if (!new File(tableOfMonophoneLabelsForScoringFileName).exists()) {
        TableOfLabels table = new TableOfLabels(TableOfLabels.Type.TIMIT39);
        table.writeToFile(tableOfMonophoneLabelsForScoringFileName);
      }
    }
    else if (m_databaseType == Database.Type.GENERAL) {
      tableOfMonophoneLabelsForScoringFileName = rootDirectoryWithHMMs +
          "table." + TableOfLabels.m_FILE_EXTENSION;
      if (!new File(tableOfMonophoneLabelsForScoringFileName).exists()) {
        m_tableOfLabels.writeToFile(tableOfMonophoneLabelsForScoringFileName);
      }
      oareTranscriptionsAvailable = false;
    }
    else {
      End.throwError(m_databaseType.toString() + " not supported");
    }

    String property = m_headerProperties.getProperty(
        "ContinuousHMMReestimator.nminimumNumberOfFramesForValidPattern", "1");
    int nminimumNumberOfFramesInValidPattern = Integer.parseInt(property);

    OffLineIsolatedSegmentsClassifier.saveProperties(
        m_outputTSTFileName,
        jarHMMsFileName,
        oisTriphone,
        oisTraining,
        directoryForDataLocators,
        directoryForSetOfPatterns,
        tableOfMonophoneLabelsForScoringFileName,
        rootDirectoryWithHMMs,
        oshouldWriteLattices,
        oareTranscriptionsAvailable,
        nminimumNumberOfFramesInValidPattern);
  }

  /**
   *  Write a report file.
   */
  private void writePropertiesToFileIfRequiredByUser() {
    if (!m_oshouldWriteReportFile) {
      return;
    }
    FileNamesAndDirectories.createDirectoriesIfNecessaryGivenFileName(
        m_outputReportFileName);
    if (!m_headerProperties.writeToFileWithHeader(m_outputReportFileName)) {

      SimpleDateFormat formatter = new SimpleDateFormat(
          "dd/MMMMM/yyyyy 'at' hh:mm:ss aaa");

      //SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
      //The At in At_Date is to try making this the first line in sorted output file
      m_headerProperties.setProperty("At_Date", formatter.format(new Date()));

      End.throwError("Error writing output properties file " +
                     m_outputReportFileName);
    }
  }

//	private void calculateTriphoneStateOccupationStatisticsUsingHERest(String hmmFileName) {
//		String temporaryHMMFileName = "temporaryFileWithCrazyNameQWE4558KAJJF";
//		IO.copyFile(hmmFileName, temporaryHMMFileName);
//		//run 1 iteration
//		IO.runDOSCommand("HERest " + m_globalFlags + " -u tmvw -w 1 -v 0.01 -I " + m_triphoneTrainingTranscriptionsFileName +
//				" -C " + m_herestConfigurationFileName + " -t 2000.0 -S " + m_listOfTrainingMFCCFilesFileName +
//				" -s " + m_triphoneStateOccupationStatisticsFileName + " -H " + temporaryHMMFileName + " " + m_listOfCrossWordTriphonesInTrainingDataFileName);
//
//		//delete temporary file
//		(new File(temporaryHMMFileName)).delete();
//		IO.rewriteTextFileUsingSystemLineSeparator(m_triphoneStateOccupationStatisticsFileName);
//	}

  /**
   *  Description of the Method
   */
  private void constructHTKToolsCaller() {
    if (m_hTKToolsCaller != null) {
      return;
    }
    String generalOutputDirectory = FileNamesAndDirectories.getParent(
        m_simulationFilesAndDirectories.getGeneralOutputDirectory());
    generalOutputDirectory += "/htk_mfcc_e_d_a/";
    //transcriptions goes below generalOutputDirectory because it is dependent on
    //table of labels being used for that specific simulation
    String outputTranscriptionsDirectory = generalOutputDirectory +
        "transcriptions/";
    String outputMFCCDirectory = generalOutputDirectory + "data/sentences/";
    //String generalOutputDirectory = "c:/htk/myhmms/my20G/";
    String inputDirectoryWithConfigurationFiles = "e:/htk";
    String globalFlags = "-A -T 1";
    //"-A -D -T 0";
    m_hTKToolsCaller = new HTKToolsCaller( //m_simulationFilesAndDirectories.getTrainSpeechDataRootDirectory(),
        //m_simulationFilesAndDirectories.getTestSpeechDataRootDirectory(),
        //outputMFCCDirectory,
        //outputTranscriptionsDirectory,
        //generalOutputDirectory,
        //m_tableOfLabels,
        globalFlags,
        inputDirectoryWithConfigurationFiles,
        true,
        m_headerPropertiesFileName);
  }

  /**
   *  Description of the Method
   */
  private void runRecognitionTestForMonophonesIfRequestedByUser() {
    if (m_hTKToolsCaller == null) {
      constructHTKToolsCaller();
    }
    boolean oisTraining = false;
    if (m_oshouldRunRecognitionForTrainingData) {
      oisTraining = true;
    }
    String optionsToHResults = "";
    m_hTKToolsCaller.runTestingToolsForMonophones(
        m_simulationFilesAndDirectories.getMostRecentMonophoneHMMFileName(),
        oisTraining,
        optionsToHResults);
  }

  /**
   *  Description of the Method
   *
   *@param  oisShared  Description of Parameter
   */
  private void runRecognitionTestForTriphonesIfRequestedByUser(boolean
      oisShared) {
    if (m_hTKToolsCaller == null) {
      constructHTKToolsCaller();
    }
    String hviteConfigFile = null;
    String mapFile = null;
    if (oisShared) {
      hviteConfigFile = m_simulationFilesAndDirectories.
          getHviteForCrossWordTriphonesConfigurationFileName();
      mapFile = m_simulationFilesAndDirectories.
          getMappingOfLogicalIntoPhysicalTriphonesFileName();
    }
    else {
      hviteConfigFile = m_simulationFilesAndDirectories.
          getHviteConfigurationFileName();
      mapFile = m_simulationFilesAndDirectories.
          getListOfCrossWordTriphonesInTrainingDataFileName();
    }
    //ak
    //m_dictionaryForMonophonesFileName + " " + m_listOfMonophonesFileName);
    hviteConfigFile = m_simulationFilesAndDirectories.
        getHviteConfigurationFileName();
    mapFile = m_simulationFilesAndDirectories.
        getMappingOfLogicalIntoPhysicalTriphonesFileName();

    if (m_oshouldRunRecognitionForTestingData) {
      boolean oisTraining = false;
      m_hTKToolsCaller.runTestingToolsForTriphones(
          m_simulationFilesAndDirectories.getDirectoryForMostRecentTriphoneHMMs(),
          mapFile, hviteConfigFile, oisTraining);
    }
    if (m_oshouldRunRecognitionForTrainingData) {
      boolean oisTraining = true;
      m_hTKToolsCaller.runTestingToolsForTriphones(
          m_simulationFilesAndDirectories.getDirectoryForMostRecentTriphoneHMMs(),
          mapFile, hviteConfigFile, oisTraining);
    }
  }

  /**
   *  Description of the Method
   */
  private void runHTKTools() {
    if (m_hTKToolsCaller == null) {
      constructHTKToolsCaller();
    }
    m_hTKToolsCaller.runSimulation();
  }

  /**
   *  Description of the Method
   */
  private void copyTIMITToUniqueDirectoryWithAllTestingData() {

    String outputWAVDirectory = null;
    String originalInputRootDirectory = null;

    //for test and train data
    for (int ntestOrTrain = 0; ntestOrTrain < 2; ntestOrTrain++) {
      switch (ntestOrTrain) {
        case 0:

          //train
          originalInputRootDirectory = m_originalDirectoryOfDatabaseTrainData;
          outputWAVDirectory = m_simulationFilesAndDirectories.
              getTrainSpeechDataRootDirectory();
          break;
        case 1:

          //test
          originalInputRootDirectory = m_originalDirectoryOfDatabaseTestData;
          outputWAVDirectory = m_simulationFilesAndDirectories.
              getTestSpeechDataRootDirectory();
          break;
      }

      //outputWAVDirectory = FileNamesAndDirectories.replaceBackSlashByForward(outputWavFilesRootDirectory);
      //outputWAVDirectory = FileNamesAndDirectories.forceEndingWithSlash(outputWAVDirectory);
      FileNamesAndDirectories.createDirectoriesIfNecessary(outputWAVDirectory);

      //DirectoryTree directoryTree = new DirectoryTree(args[0],"ADC");
      DirectoryTree directoryTree = new DirectoryTree(
          originalInputRootDirectory, "WAV");
      Vector originalWavFiles = directoryTree.getFiles();
      int nnumberOfFiles = originalWavFiles.size();
      if (m_nverbose > 0) {
        Print.dialog("\nCopying from " + originalInputRootDirectory + " to " +
                     outputWAVDirectory);
        Print.dialog("Total number of files = " + nnumberOfFiles);
      }
      //copy TIMIT cd to directory wiht 1 unique file for each file
      //Vector wavFiles = new Vector();
      //Vector phnFiles = new Vector();
      for (int i = 0; i < nnumberOfFiles; i++) {
        String fileName = (String) originalWavFiles.elementAt(i);
        fileName = FileNamesAndDirectories.replaceBackSlashByForward(fileName);
        String phnFileName = FileNamesAndDirectories.substituteExtension(
            fileName, "phn");
        TIMITPathOrganizer tIMITPathOrganizer = new TIMITPathOrganizer(fileName);
        if (!tIMITPathOrganizer.isPathOk()) {
          End.throwError(fileName + " is not valid TIMIT path");
        }
        //String newFileName = outputWAVDirectory + i + "_" + tIMITPathOrganizer.getUniqueName();
        String newFileName = outputWAVDirectory +
            tIMITPathOrganizer.getUniqueName();
        String newPHNFileName = FileNamesAndDirectories.substituteExtension(
            newFileName, "phn");

        IO.copyFile(fileName, newFileName);
        IO.copyFile(phnFileName, newPHNFileName);

        if (!IO.areFilesTheSame(fileName, newFileName) ||
            !IO.areFilesTheSame(phnFileName, newPHNFileName)) {
          Print.dialog(fileName + " => " + newFileName);
          Print.dialog(phnFileName + " => " + newPHNFileName);
          Print.error("Files are different!");
          System.exit(1);
        }
        if (m_nverbose > 0) {
          IO.showCounter(i + 1);
        }

        //IO.runDOSCommand("diff " + fileName + " " + newFileName);
        //IO.runDOSCommand("diff " + fileName + " " + newFileName);
        //wavFiles.addElement(newFileName);
        //phnFiles.addElement(newPHNFileName);
      }

      //IO.writeVectorOfStringsToFile("hinit.scr",mfccFiles);
      //IO.writeVectorOfStringsToFile("hled.scr",phnFiles);
    }
  }

  /**
   *  Description of the Method
   */
  protected void
      copyTIMITDataToUniqueDirectoryWithNoSASentenceAndOnlyCoreTestSet() {

    String outputWAVDirectory = null;
    String originalInputRootDirectory = null;

    //for test and train data
    for (int ntestOrTrain = 0; ntestOrTrain < 2; ntestOrTrain++) {
      TIMITDataLocatorsFileCreator tIMITDataLocatorsFileCreator = new
          TIMITDataLocatorsFileCreator();
      Vector originalWavFiles = null;
      switch (ntestOrTrain) {
        case 0:

          //test
          originalInputRootDirectory = m_originalDirectoryOfDatabaseTestData;
          outputWAVDirectory = m_simulationFilesAndDirectories.
              getTestSpeechDataRootDirectory();
          originalWavFiles = tIMITDataLocatorsFileCreator.
              getCoreSetTestWAVFiles(m_originalDirectoryOfDatabaseTestData);
          break;
        case 1:

          //train
          originalInputRootDirectory = m_originalDirectoryOfDatabaseTrainData;
          outputWAVDirectory = m_simulationFilesAndDirectories.
              getTrainSpeechDataRootDirectory();
          originalWavFiles = tIMITDataLocatorsFileCreator.
              getTrainWAVFilesWithoutSASentences(
              m_originalDirectoryOfDatabaseTrainData);
          break;
      }
      copyTIMITDataUsingUniqueName(originalInputRootDirectory,
                                   outputWAVDirectory, originalWavFiles);
    }
  }

  //copy {validation set} = {test set} - {core test set} to a directory ../validation (with
  //respect to the test directory
  protected void copyTIMITDataValidationSetUsingUniqueName() {
    TIMITDataLocatorsFileCreator tIMITDataLocatorsFileCreator = new
        TIMITDataLocatorsFileCreator();
    String originalInputRootDirectory = m_originalDirectoryOfDatabaseTestData;
    String outputWAVDirectory = m_simulationFilesAndDirectories.
        getValidationSpeechDataRootDirectory();
    Vector originalWavFiles = tIMITDataLocatorsFileCreator.
        getValidationSetWAVFiles(m_originalDirectoryOfDatabaseTestData);
    copyTIMITDataUsingUniqueName(originalInputRootDirectory, outputWAVDirectory,
                                 originalWavFiles);
  }

  protected void copyTIMITDataUsingUniqueName(String originalInputRootDirectory,
                                              String outputWAVDirectory,
                                              Vector originalWavFiles) {

    outputWAVDirectory = FileNamesAndDirectories.replaceBackSlashByForward(
        outputWAVDirectory);
    outputWAVDirectory = FileNamesAndDirectories.forceEndingWithSlash(
        outputWAVDirectory);
    FileNamesAndDirectories.createDirectoriesIfNecessary(outputWAVDirectory);

    int nnumberOfFiles = originalWavFiles.size();
    if (m_nverbose > 0) {
      Print.dialog("\nCopying from " + originalInputRootDirectory + " to " +
                   outputWAVDirectory);
      Print.dialog("Total number of files = " + nnumberOfFiles);
    }
    //copy TIMIT cd to directory wiht 1 unique file for each file
    //Vector wavFiles = new Vector();
    //Vector phnFiles = new Vector();
    Print.setJProgressBarRange(0, nnumberOfFiles);
    for (int i = 0; i < nnumberOfFiles; i++) {
      String fileName = (String) originalWavFiles.elementAt(i);
      fileName = FileNamesAndDirectories.replaceBackSlashByForward(fileName);
      String phnFileName = FileNamesAndDirectories.substituteExtension(fileName,
          "phn");
      String txtFileName = FileNamesAndDirectories.substituteExtension(fileName,
          "txt");
      String wrdFileName = FileNamesAndDirectories.substituteExtension(fileName,
          "wrd");
      TIMITPathOrganizer tIMITPathOrganizer = new TIMITPathOrganizer(fileName);
      if (!tIMITPathOrganizer.isPathOk()) {
        End.throwError(fileName + " is not valid TIMIT path");
      }
      //String newFileName = outputWAVDirectory + i + "_" + tIMITPathOrganizer.getUniqueName();
      String newFileName = outputWAVDirectory +
          tIMITPathOrganizer.getUniqueName();
      String newPHNFileName = FileNamesAndDirectories.substituteExtension(
          newFileName, "phn");
      String newTXTFileName = FileNamesAndDirectories.substituteExtension(
          newFileName, "txt");
      String newWRDFileName = FileNamesAndDirectories.substituteExtension(
          newFileName, "wrd");

      IO.copyFile(fileName, newFileName);
      IO.copyFile(phnFileName, newPHNFileName);
      IO.copyFile(txtFileName, newTXTFileName);
      IO.copyFile(wrdFileName, newWRDFileName);

      //check only PHN and WAV files
      if (!IO.areFilesTheSame(fileName, newFileName) ||
          !IO.areFilesTheSame(phnFileName, newPHNFileName)) {
        Print.dialog(fileName + " => " + newFileName);
        Print.dialog(phnFileName + " => " + newPHNFileName);
        Print.error("Files are different!");
        System.exit(1);
      }
      if (m_nverbose > 0) {
        IO.showCounter(i + 1);
      }
      Print.updateJProgressBar(i + 1);

      //IO.runDOSCommand("diff " + fileName + " " + newFileName);
      //IO.runDOSCommand("diff " + fileName + " " + newFileName);
      //wavFiles.addElement(newFileName);
      //phnFiles.addElement(newPHNFileName);
    }

    //IO.writeVectorOfStringsToFile("hinit.scr",mfccFiles);
    //IO.writeVectorOfStringsToFile("hled.scr",phnFiles);

  }

  private void createListOfAllPossibleCrossWordTriphones(String outputFileName) {
    int nnumberOfEntries = m_tableOfLabels.getNumberOfEntries();
    //take out sp
    String[] labels = new String[nnumberOfEntries];
    //organize in such way that sil is the last one
    int labelsAdded = 0;
    for (int nentry = 0; nentry < nnumberOfEntries; nentry++) {
      String thisLabel = m_tableOfLabels.getFirstLabel(nentry);
      //if ( (!thisLabel.equals("sp")) && (!thisLabel.equals("sil")) ) {
      if (!thisLabel.equals("sil")) {
        labels[labelsAdded] = thisLabel;
        labelsAdded++;
      }
    }
    //trick to avoid central phone in triphone being sil
    //make sure sil is the last phone
    labels[nnumberOfEntries - 1] = "sil";
    int nnumberOfModels = 0;
    try {
      BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(
          outputFileName));
      bufferedWriter.write("sil" + IO.m_NEW_LINE);
      // + "sp" + IO.m_NEW_LINE);
      for (int c = 0; c < nnumberOfEntries - 1; c++) {
        //don't include sil
        //bufferedWriter.write(labels[c]+IO.m_NEW_LINE);
        for (int l = 0; l < nnumberOfEntries; l++) {
          //include sil
          //bufferedWriter.write(labels[l]+"-"+labels[c]+IO.m_NEW_LINE);
          //bufferedWriter.write(labels[c]+"+"+labels[l]+IO.m_NEW_LINE);
          for (int r = 0; r < nnumberOfEntries; r++) {
            //include sil
            bufferedWriter.write(labels[l] + "-" + labels[c] + "+" + labels[r] +
                                 IO.m_NEW_LINE);
            //nnumberOfModels += 4;
            nnumberOfModels++;
          }
        }
        IO.showCounter(nnumberOfModels);
      }
      bufferedWriter.close();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    Print.dialog("");
  }

  public HeaderProperties getHeaderPropertiesReference() {
    return m_headerProperties;
  }

  protected void writeAdultPartOfTIDIGITSToTwoDirectories() {
    //test set
    String indir = FileNamesAndDirectories.replaceAndForceEndingWithSlash(
        m_originalDirectoryOfDatabaseTestData);
    String outdir = FileNamesAndDirectories.replaceAndForceEndingWithSlash(
        m_simulationFilesAndDirectories.getTestSpeechDataRootDirectory());
    TIDIGITSDataLocatorsFileCreator.writeFilesWithUniqueName(indir + "man/",
        outdir);
    TIDIGITSDataLocatorsFileCreator.writeFilesWithUniqueName(indir + "woman/",
        outdir);

    //train
    indir = FileNamesAndDirectories.replaceAndForceEndingWithSlash(
        m_originalDirectoryOfDatabaseTrainData);
    outdir = FileNamesAndDirectories.replaceAndForceEndingWithSlash(
        m_simulationFilesAndDirectories.getTrainSpeechDataRootDirectory());
    TIDIGITSDataLocatorsFileCreator.writeFilesWithUniqueName(indir + "man/",
        outdir);
    TIDIGITSDataLocatorsFileCreator.writeFilesWithUniqueName(indir + "woman/",
        outdir);
  }

  private void createTranscriptionsForTIDIGITSUsingForcedAlignedResults() {
    //train
    FileNamesAndDirectories.createDirectoriesIfNecessaryGivenFileName(
        m_simulationFilesAndDirectories.
        getSentenceMonophoneTranscriptionsOfTrainSpeechDataFileName());
    String train = m_headerProperties.getPropertyAndExitIfKeyNotFound(
        "TrainingManager.MLFTrainTIDIGITSAligned");
    HTKInterfacer.convertMLFLabelFileToDataLocatorFile(train, "wav",
        m_simulationFilesAndDirectories.getTrainSpeechDataRootDirectory(),
        m_simulationFilesAndDirectories.
        getSentenceMonophoneTranscriptionsOfTrainSpeechDataFileName(),
        m_patternGenerator.getSpeechSamplingRate(),
        !m_ouseAbsolutePath);

    //test
    FileNamesAndDirectories.createDirectoriesIfNecessaryGivenFileName(
        m_simulationFilesAndDirectories.
        getSentenceMonophoneTranscriptionsOfTestSpeechDataFileName());
    String test = m_headerProperties.getPropertyAndExitIfKeyNotFound(
        "TrainingManager.MLFTestTIDIGITSAligned");
    HTKInterfacer.convertMLFLabelFileToDataLocatorFile(test, "wav",
        m_simulationFilesAndDirectories.getTestSpeechDataRootDirectory(),
        m_simulationFilesAndDirectories.
        getSentenceMonophoneTranscriptionsOfTestSpeechDataFileName(),
        m_patternGenerator.getSpeechSamplingRate(),
        !m_ouseAbsolutePath);
  }

  /**
   *  Inner class that represents the type of a TrainingManager.
   *
   *@author     Aldebaro
   *@created    November 24, 2000
   */
  public static class Type
      extends SuperType {

    public final static Type SENTENCE = new Type("SENTENCE");
    public final static Type ISOLATED_SEGMENT = new Type("ISOLATED_SEGMENT");
    public final static Type CUT_SENTENCE = new Type("CUT_SENTENCE");
    protected static Type[] m_types;

    //In case of adding a new Type (above), don't forget to add it below.
    static {
      m_types = new Type[3];
      m_types[0] = ISOLATED_SEGMENT;
      m_types[1] = SENTENCE;
      m_types[2] = CUT_SENTENCE;
    }

    //notice the constructor is private, not public.
    private Type(String strName) {
      //m_strName is defined in superclass
      m_strName = strName;
    }

    /**
     *  Return true if the input String is equal (case sensitive) to the String
     *  that represents one of the defined Type's.
     *
     *@param  typeIdentifierString  Description of Parameter
     *@return                       The Valid value
     */
    public static boolean isValid(String typeIdentifierString) {
      for (int i = 0; i < m_types.length; i++) {

        //notice I am using case sensitive comparation
        //System.out.println("typeIdentifierString = "+typeIdentifierString);
        //System.out.println("m_types[i].toString() = "+m_types[i].toString());
        if (typeIdentifierString.equals(m_types[i].toString())) {
          return true;
        }
      }
      return false;
    }

    /**
     *  Return the Type correspondent to the given indentifier String or null in
     *  case identifier is not valid.
     *
     *@param  typeIdentifierString  Description of Parameter
     *@return                       The Type value
     */
    //it was not declared in SuperType because I wanted to
    //return this specif Type not a SuperType and didn't know how
    //to do the casting from the superclass... Is that possible ?
    public final static Type getType(String typeIdentifierString) {
      if (isValid(typeIdentifierString)) {
        int nindex = getTypeIndex(typeIdentifierString);
        return (Type) m_types[nindex];
      }
      else {
        return null;
      }
    }

    /**
     *  Return the Type correspondent to the given indentifier String or exit in
     *  case identifier is not valid.
     *
     *@param  typeIdentifierString  Description of Parameter
     *@return                       The TypeAndExitOnError value
     */
    //it was not declared in SuperType because I wanted to
    //return this specif Type not a SuperType and didn't know how
    //to do the casting from the superclass... Is that possible ?
    public final static Type getTypeAndExitOnError(String typeIdentifierString) {
      if (isValid(typeIdentifierString)) {
        int nindex = getTypeIndex(typeIdentifierString);
        return (Type) m_types[nindex];
      }
      else {
        End.throwError(typeIdentifierString +
                       " is not a valid TrainingManager type.");
        //make compiler happy:
        return null;
      }
    }

    /**
     *  Return the index of m_types element that matches the input String or -1 if
     *  there was no match.
     *
     *@param  typeIdentifierString  Description of Parameter
     *@return                       The TypeIndex value
     */
    private static int getTypeIndex(String typeIdentifierString) {
      for (int i = 0; i < m_types.length; i++) {
        //notice I am using case sensitive comparation
        if (typeIdentifierString.equals(m_types[i].toString())) {
          return i;
        }
      }
      return -1;
    }

  }

}

//end of class

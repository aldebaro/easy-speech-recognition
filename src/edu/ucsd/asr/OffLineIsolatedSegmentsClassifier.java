package edu.ucsd.asr;

import java.util.Vector;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.StringTokenizer;
import java.io.File;

import ioproperties.*;

/**
 *  Classifies isolated segments (words, phonemes, etc.) based on a JAR file
 *  with a set of HMMs. Assumes the HMM models and the parametric
 *  representations (e.g., MFCC) were previously calculated and saved into
 *  files. Notice that the difference between "classification" and "recognition"
 *  is that the former assumes the words were previously segmented. In
 *  "classification" there are no errors of insertions and deletions, that occur
 *  in "recognition".
 *
 *@author     Aldebaro Klautau
 *@created    November 30, 2000
 *@version    2 - November 26, 2000
 */
public class OffLineIsolatedSegmentsClassifier {

  private static int m_nverbose = 0;
  private static boolean m_oalreadyShowedWarningMessage = false;

  public static void setVerbosity(int nverbose) {
    m_nverbose = nverbose;
  }

  //private boolean m_oshouldWriteLattices;

  //  private TableOfLabels m_hMMTableOfLabels;
  //  private SetOfHMMs m_setOfHMMs;
  //  private TableOfLabels m_tableOfLabelsForScoring;
  //public OffLineIsolatedSegmentsClassifier() {
  //}
  /**
   *  Gets the Results attribute of the OffLineIsolatedSegmentsClassifier class
   *
   *@param  setOfHMMs                Description of Parameter
   *@param  tableOfLabelsForScoring  Description of Parameter
   *@param  headerProperties         Description of Parameter
   *@param  oisTraining              Description of Parameter
   *@param  outputDirectory          Description of Parameter
   *@param  oshouldWriteLattices     Description of Parameter
   *@return                          The Results value
   */
  //headerProperties has the table used for writing isolated SOP's.
  //That can be different from the table for scoring, and from the
  //table of a given set of HMMs. So, I will have 3 tables !
  public static ClassificationStatisticsCalculator getResults(SetOfHMMs
      setOfHMMs,
      TableOfLabels tableOfLabelsForScoring,
      String directoryForDataLocators,
      String directoryForSetOfPatterns,
      //boolean oisTraining,
      String outputDirectory,
      boolean oshouldWriteLattices,
      int nminimumNumberOfFramesInValidPattern,
      String simulationIdentifier) {

    //Print.dialog("m_nminimumNumberOfFramesInValidPattern = " + nminimumNumberOfFramesInValidPattern);

    //m_oshouldWriteLattices = oshouldWriteLattices;
    outputDirectory = FileNamesAndDirectories.replaceAndForceEndingWithSlash(
        outputDirectory);
    FileNamesAndDirectories.createDirectoriesIfNecessary(outputDirectory);

    //BufferedWriter bufferedWriterMatches = null;
    //BufferedWriter bufferedWriterErrors = null;
    //		if (m_oshouldWriteLattices) {
    //			int ntotalLabels = tableOfLabelsForScoring.getNumberOfEntries();
    //			bufferedWriterMatches = IO.openBufferedWriter(outputDirectory + "latticesMatches" + ntotalLabels + ".txt");
    //			bufferedWriterErrors = IO.openBufferedWriter(outputDirectory + "latticesErrors" + ntotalLabels + ".txt");
    //		}
    //		String generalOutputDirectory = headerProperties.getPropertyAndExitIfKeyNotFound("TrainingManager.GeneralOutputDirectory");
    //		generalOutputDirectory = FileNamesAndDirectories.forceEndingWithSlash(generalOutputDirectory);
    if (tableOfLabelsForScoring == null) {
      End.throwError("tableOfLabelsForScoring == null");
    }

    //		String propertyIdentifier = null;
    //		if (oisTraining) {
    //			propertyIdentifier = "TrainFileName_";
    //			generalOutputDirectory += "data/isolated/train/";
    //		}
    //		else {
    //			propertyIdentifier = "TestFileName_";
    //			generalOutputDirectory += "data/isolated/test/";
    //		}
    //String errorsOutputFileName = "errors.txt";
    //String matchesOutputFileName = "matches.txt";
    //String confusionMatrixOutputFileName = "confusion.txt";
    //String texFileName = "confusion.tex";
    //String directoryForDataLocators = headerProperties.getProperty("TrainingManager.DirectoryForDataLocators");
    if (directoryForDataLocators != null) {
      directoryForDataLocators = FileNamesAndDirectories.forceEndingWithSlash(
          directoryForDataLocators);
    }
    //String directoryForSetOfPatterns = null;
    //if (oisTraining) {
    //	directoryForSetOfPatterns = headerProperties.getPropertyAndExitIfKeyNotFound("TrainingManager.IsolatedTrainParametersDirectory");
    //}
    //else {
    //	directoryForSetOfPatterns = headerProperties.getPropertyAndExitIfKeyNotFound("TrainingManager.IsolatedTestParametersDirectory");
    //}
    //if (directoryForSetOfPatterns != null) {
    directoryForSetOfPatterns = FileNamesAndDirectories.forceEndingWithSlash(
        directoryForSetOfPatterns);
    //}
    //Print.dialog(headerProperties.toString());
    //headerProperties has the table used for writing isolated SOP's.
    //TableOfLabels tableOfLabels = TableOfLabels.getTableOfLabels(headerProperties);
    //Print.dialog("tableOfLabels.toString()");
    //Print.dialog(tableOfLabels.toString());
    //XXX each set can have a different organization of labels, so:
    TableOfLabels tableOfLabelsForThisHMMSet = setOfHMMs.getTableOfLabels();

    //this way I can not use part of test sequence... (should change it)
    TableOfLabels tableOfLabels = tableOfLabelsForThisHMMSet;

    //Print.dialog("tableOfLabelsForThisHMMSet.toString()");
    //Print.dialog(tableOfLabelsForThisHMMSet.toString());
    //System.exit(1);
    //SetOfPlainContinuousHMMs
    //	setOfPlainContinuousHMMs = SetOfPlainContinuousHMMs.getSetOfPlainContinuousHMMs(headerProperties);
    //uses the table for scoring
    ClassificationStatisticsCalculator
        classificationStatisticsCalculator = new
        ClassificationStatisticsCalculator(tableOfLabelsForScoring,
                                           simulationIdentifier,
                                           outputDirectory);

    //turn on N-best list
    SetOfPlainContinuousHMMs setOfPlainContinuousHMMs = null;
    if (oshouldWriteLattices) {
      if (! (setOfHMMs instanceof SetOfPlainContinuousHMMs)) {
        //need to move SetOfPlainContinuousHMMs methods to super class
        //End.throwError("Currently supports only SetOfPlainContinuousHMMs");
        oshouldWriteLattices = false;
        if (m_nverbose > 0) {
          //show message just once
          if (!m_oalreadyShowedWarningMessage) {
            Print.warning("Disabled lattice generation because HMM is shared and lattices are currently supported only for plain HMMs");
            m_oalreadyShowedWarningMessage = true;
          }
        }
      }
      else {
        setOfPlainContinuousHMMs = (SetOfPlainContinuousHMMs) setOfHMMs;
        boolean okeepPaths = true;
        setOfPlainContinuousHMMs.enableNBestListGeneration(okeepPaths);
      }
    }

    int nnumberOfEntries = tableOfLabels.getNumberOfEntries();
    Print.setJProgressBarRange(0, nnumberOfEntries);
    for (int nentryNumber = 0; nentryNumber < nnumberOfEntries; nentryNumber++) {

      String correctLabel = tableOfLabels.getFirstLabel(nentryNumber);

      //String prefferedName = generalOutputDirectory + nentryNumber + "_" + correctLabel + "." + SetOfPatterns.m_FILE_EXTENSION;
      String prefferedName = nentryNumber + "_" + correctLabel + "." +
          SetOfPatterns.m_FILE_EXTENSION;

      //String patternsFileName = headerProperties.getProperty("SetOfPatterns." + propertyIdentifier + Integer.toString(nentryNumber),prefferedName);
      //String patternsFileName = headerProperties.getProperty("SetOfPatterns.FileName_" + Integer.toString(nentryNumber), prefferedName);
      String patternsFileName = prefferedName;
      patternsFileName = FileNamesAndDirectories.getAbsolutePath(
          patternsFileName, directoryForSetOfPatterns);

      SetOfPatterns setOfPatterns = new SetOfPatterns(patternsFileName);
      int nnumberOfPatterns = setOfPatterns.getNumberOfPatterns();
      if (m_nverbose > 1) {
        System.out.print("Reading SetOfPatterns " + patternsFileName +
                         " for nentryNumber = " + nentryNumber +
                         " with # of patterns = " + nnumberOfPatterns +
                         ".\nFile size (KB) = " +
                         IO.format(new File(patternsFileName).length() / 1024.0));
        System.out.println(". Memory (KB). Free = " +
                           IO.format(Runtime.getRuntime().freeMemory() / 1024.0) +
                           ". Used = " +
                           IO.format(Runtime.getRuntime().totalMemory() /
                                     1024.0));
      }

      //get the associated DataLocator file
      //String dataLocatorFileName = headerProperties.getPropertyAndExitIfKeyNotFound("DataLocator." + propertyIdentifier + Integer.toString(nentryNumber));
      //dataLocatorFileName = FileNamesAndDirectories.getAbsolutePath(dataLocatorFileName, directoryForDataLocators);
      String dataLocatorFileName = setOfPatterns.getDataLocatorFileName();

      DatabaseManager databaseManager = new DatabaseManager(dataLocatorFileName);

      //int[] nspeechData;
      //for each Pattern in this SetOfPatterns file
      int nnumberOfSegments = 0;
      DataLocator dataLocator = null;
      while (databaseManager.isThereDataToRead()) {
        //get the Segment that generated this Pattern
        //if ((dataLocator = databaseManager.getNextDataLocator()) != null) {
        dataLocator = databaseManager.getNextDataLocator();
        int ntotalNumberOfSegmentsInThisDataLocator = dataLocator.
            getNumberOfSegments();
        for (int nsegmentIndex = 0;
             nsegmentIndex < ntotalNumberOfSegmentsInThisDataLocator;
             nsegmentIndex++) {

          //if one wants to listen the Audio
          //Audio audio = segments.getAudioFromGivenSegment(i);
          //or take the whole sentence:
          //Audio audio = segments.getAudioOfWholeSentence();
          //AudioPlayer.playback(audio);
          //in case one wants to listen the (amplitude scaled) segment
          //AudioPlayer.playScaled(audio);
          //String segmentInfo = dataLocator.getFileNameAndLabelFromGivenSegment(nsegmentIndex);
          String segmentInfo = dataLocator.getGivenSegment(nsegmentIndex);

          //Print.dialog(segmentInfo);
          //get the pattern
          Pattern pattern = null;
          try {
            pattern = setOfPatterns.getPattern(nnumberOfSegments);
          }
          catch (ASRError e) {
            Print.error(e.toString());
            Print.error("Line: " + segmentInfo);
            Print.error("The file " + dataLocatorFileName +
                        " obtained from the " + SetOfPatterns.m_FILE_EXTENSION +
                        " header was not the one " +
                        "used to generate the " +
                        SetOfPatterns.m_FILE_EXTENSION + " ??");
            Print.error("Note the " + SetOfPatterns.m_FILE_EXTENSION + " file " +
                        patternsFileName + " has " + nnumberOfPatterns +
                        " patterns " +
                        "while system tried to read pattern index # " +
                        nnumberOfSegments);
            databaseManager.m_oendOfData = true;
            break;
            //ak XXX
            //End.exit();
          }
          //update for next iteration
          nnumberOfSegments++;

          if (pattern.getNumOfFrames() >= nminimumNumberOfFramesInValidPattern) {
            //find best HMM model
            try {
              setOfHMMs.findBestModelAndItsScore(pattern);
            }
            catch (Error e) {
              e.printStackTrace();
              Print.error("When processing " + segmentInfo);
              End.exit(1);
            }
            int nbestModel = setOfHMMs.getBestModel();

            if (nbestModel == -1) {
              End.throwError("No token survived the Viterbi algorithm\n" +
                             "If you are using a left-right HMM, change the configuration\n" +
                             "providing a \"minimum number of frames per valid token\"." +
                             "Currently, this value is = " +
                             nminimumNumberOfFramesInValidPattern + " frames.");
            }

            //setOfHMMs.en
            //Print.dialog("score = " + setOfHMMs.getBestScore());
            //Print.dialog("nbestModel = " + nbestModel);
            //update statistics
            String labelOfBestHMM = tableOfLabelsForThisHMMSet.getFirstLabel(
                nbestModel);

            //Print.dialog("correctLabel = " + correctLabel + ", labelOfBestHMM = " + labelOfBestHMM);
            //if (pattern.getNumOfFrames() > 2) {
            boolean owasMatch = (nentryNumber == nbestModel) ? true : false;

            if (oshouldWriteLattices) {
              //then add lattices to segmentInfo
              String paths = setOfPlainContinuousHMMs.
                  getPathsOfNBestListUsingRunLengthEncoding();
              segmentInfo += " " + paths;
              //String segmentInfoWithoutPath = extractPathFromFileName(segmentInfo);
              //						try {
              //							if (owasMatch) {
              //								bufferedWriterMatches.write(segmentInfoWithoutPath + " " + paths);
              //								bufferedWriterMatches.newLine();
              //							}
              //							else {
              //								bufferedWriterErrors.write(segmentInfoWithoutPath + " " + paths);
              //								bufferedWriterErrors.newLine();
              //							}
              //						}
              //						catch (IOException e) {
              //							e.printStackTrace();
              //							End.exit();
              //						}
            }
            classificationStatisticsCalculator.updateStatistics(correctLabel,
                labelOfBestHMM,
                segmentInfo);

            //}
          }
          //<= delete if not using < min # frames
          //} else {
          //Print.dialog("frames # = " + pattern.getNumOfFrames());
          //}
          //					if (nnumberOfSegments > nnumberOfPatterns) {
          //						Print.error("nnumberOfSegments = " +
          //								nnumberOfSegments + " and " +
          //								"nnumberOfPatterns = " + nnumberOfPatterns);
          //						End.exit();
          //					}
        }
      }
      databaseManager.finalizeDataReading();
      Print.updateJProgressBar(nentryNumber + 1);
      setOfPatterns = null;
      //System.gc();
    }

    //		if (m_oshouldWriteLattices) {
    //			try {
    //				bufferedWriterErrors.close();
    //				bufferedWriterMatches.close();
    //			}
    //			catch (IOException e) {
    //				e.printStackTrace();
    //				End.exit();
    //			}
    //		}
    return classificationStatisticsCalculator;
  }

  /**
   *  Gets the SimplifiedResults attribute of the
   *  OffLineIsolatedSegmentsClassifier class
   *
   */
  public static ClassificationStatisticsCalculator getSimplifiedResults(
      SetOfHMMs setOfHMMs,
      TableOfLabels tableOfLabelsForScoring,
      String directoryForSetOfPatterns,
      //boolean oisTraining,
      String outputDirectory,
      boolean oshouldWriteLattices,
      int nminimumNumberOfFramesInValidPattern,
      String simulationIdentifier) {

    //Print.dialog("m_nminimumNumberOfFramesInValidPattern = " + nminimumNumberOfFramesInValidPattern);
    outputDirectory = FileNamesAndDirectories.replaceAndForceEndingWithSlash(
        outputDirectory);
    FileNamesAndDirectories.createDirectoriesIfNecessary(outputDirectory);

    if (tableOfLabelsForScoring == null) {
      End.throwError("tableOfLabelsForScoring == null");
    }

    //String directoryForSetOfPatterns = null;
    //if (oisTraining) {
    //	directoryForSetOfPatterns = headerProperties.getPropertyAndExitIfKeyNotFound("TrainingManager.IsolatedTrainParametersDirectory");
    //}
    //else {
    //	directoryForSetOfPatterns = headerProperties.getPropertyAndExitIfKeyNotFound("TrainingManager.IsolatedTestParametersDirectory");
    //}
    directoryForSetOfPatterns = FileNamesAndDirectories.forceEndingWithSlash(
        directoryForSetOfPatterns);
    //headerProperties has the table used for writing isolated SOP's.
    //TableOfLabels tableOfLabels = TableOfLabels.getTableOfLabels(headerProperties);
    //XXX each set can have a different organization of labels, so:
    TableOfLabels tableOfLabelsForThisHMMSet = setOfHMMs.getTableOfLabels();
    //limitation:
    TableOfLabels tableOfLabels = tableOfLabelsForThisHMMSet;

    //uses the table for scoring
    ClassificationStatisticsCalculator
        classificationStatisticsCalculator = new
        ClassificationStatisticsCalculator(tableOfLabelsForScoring,
                                           simulationIdentifier,
                                           outputDirectory);

    //turn on N-best list
    SetOfPlainContinuousHMMs setOfPlainContinuousHMMs = null;
    if (oshouldWriteLattices) {
      if (! (setOfHMMs instanceof SetOfPlainContinuousHMMs)) {
        //need to move SetOfPlainContinuousHMMs methods to super class
        End.throwError("Currently supports only SetOfPlainContinuousHMMs");
      }
      else {
        setOfPlainContinuousHMMs = (SetOfPlainContinuousHMMs) setOfHMMs;
        boolean okeepPaths = true;
        setOfPlainContinuousHMMs.enableNBestListGeneration(okeepPaths);
      }
    }

    int nnumberOfEntries = tableOfLabels.getNumberOfEntries();

    Print.setJProgressBarRange(0, nnumberOfEntries);
    for (int nentryNumber = 0; nentryNumber < nnumberOfEntries; nentryNumber++) {

      String correctLabel = tableOfLabels.getFirstLabel(nentryNumber);

      //String prefferedName = generalOutputDirectory + nentryNumber + "_" + correctLabel + "." + SetOfPatterns.m_FILE_EXTENSION;
      String prefferedName = tableOfLabels.getPrefferedName(nentryNumber,
          SetOfPatterns.m_FILE_EXTENSION);

      //String patternsFileName = headerProperties.getProperty("SetOfPatterns." + propertyIdentifier + Integer.toString(nentryNumber),prefferedName);
      //String patternsFileName = headerProperties.getProperty("SetOfPatterns.FileName_" + Integer.toString(nentryNumber), prefferedName);
      String patternsFileName = prefferedName;
      patternsFileName = FileNamesAndDirectories.getAbsolutePath(
          patternsFileName, directoryForSetOfPatterns);

      //System.out.println("Reading SetOfPatterns " + patternsFileName + " for nentryNumber = " + nentryNumber + ".");
      SetOfPatterns setOfPatterns = new SetOfPatterns(patternsFileName);
      int nnumberOfPatterns = setOfPatterns.getNumberOfPatterns();

      //get the associated DataLocator file
      //String dataLocatorFileName = headerProperties.getPropertyAndExitIfKeyNotFound("DataLocator." + propertyIdentifier + Integer.toString(nentryNumber));
      //dataLocatorFileName = FileNamesAndDirectories.getAbsolutePath(dataLocatorFileName, directoryForDataLocators);
      //String dataLocatorFileName = setOfPatterns.getDataLocatorFileName();
      //DatabaseManager databaseManager = new DatabaseManager(dataLocatorFileName);
      //int[] nspeechData;
      //for each Pattern in this SetOfPatterns file
      //int nnumberOfSegments = 0;
      //DataLocator dataLocator = null;
      for (int i = 0; i < nnumberOfPatterns; i++) {
        //while (databaseManager.isThereDataToRead()) {
        //get the Segment that generated this Pattern
        //if ((dataLocator = databaseManager.getNextDataLocator()) != null) {
        //dataLocator = databaseManager.getNextDataLocator();
        //int ntotalNumberOfSegmentsInThisDataLocator = dataLocator.getNumberOfSegments();
        //for (int nsegmentIndex = 0; nsegmentIndex < ntotalNumberOfSegmentsInThisDataLocator; nsegmentIndex++) {
        //if one wants to listen the Audio
        //Audio audio = segments.getAudioFromGivenSegment(i);
        //or take the whole sentence:
        //Audio audio = segments.getAudioOfWholeSentence();
        //AudioPlayer.playback(audio);
        //in case one wants to listen the (amplitude scaled) segment
        //AudioPlayer.playScaled(audio);
        //String segmentInfo = dataLocator.getFileNameAndLabelFromGivenSegment(nsegmentIndex);
        //String segmentInfo = dataLocator.getGivenSegment(nsegmentIndex);
        //Print.dialog(segmentInfo);
        //get the pattern
        Pattern pattern = null;
        //try {
        pattern = setOfPatterns.getPattern(i);
        //}
        //catch (ASRError e) {
        //Print.error(e.toString());
        //						Print.error("Line: " + segmentInfo);
        //						Print.error("The file " + dataLocatorFileName + " obtained from the SOP header was not the one " +
        //								"used to generate the SOP ??");
        //						Print.error("Note the SOP file " + patternsFileName + " has " + nnumberOfPatterns + " patterns " +
        //								"while system tried to read pattern index # " + nnumberOfSegments);
        //						databaseManager.m_oendOfData = true;
        //						break;
        //ak XXX
        //	End.exit();
        //}
        //update for next iteration
        //nnumberOfSegments++;
        if (pattern.getNumOfFrames() >= nminimumNumberOfFramesInValidPattern) {
          //find best HMM model
          try {
            setOfHMMs.findBestModelAndItsScore(pattern);
          }
          catch (Error e) {
            e.printStackTrace();
            Print.error("When processing pattern # " + i + " of " +
                        patternsFileName);
            End.exit(1);
          }
          int nbestModel = setOfHMMs.getBestModel();

          //setOfHMMs.en
          //Print.dialog("score = " + setOfHMMs.getBestScore());
          //Print.dialog("nbestModel = " + nbestModel);
          //update statistics
          String labelOfBestHMM = tableOfLabelsForThisHMMSet.getFirstLabel(
              nbestModel);

          //Print.dialog("correctLabel = " + correctLabel + ", labelOfBestHMM = " + labelOfBestHMM);
          //if (pattern.getNumOfFrames() > 2) {
          boolean owasMatch = (nentryNumber == nbestModel) ? true : false;

          String segmentInfo = "Pattern_#_" + i;
          if (oshouldWriteLattices) {
            //then add lattices to segmentInfo
            String paths = setOfPlainContinuousHMMs.
                getPathsOfNBestListUsingRunLengthEncoding();
            segmentInfo += " " + paths;
            //String segmentInfoWithoutPath = extractPathFromFileName(segmentInfo);
            //						try {
            //							if (owasMatch) {
            //								bufferedWriterMatches.write(segmentInfoWithoutPath + " " + paths);
            //								bufferedWriterMatches.newLine();
            //							}
            //							else {
            //								bufferedWriterErrors.write(segmentInfoWithoutPath + " " + paths);
            //								bufferedWriterErrors.newLine();
            //							}
            //						}
            //						catch (IOException e) {
            //							e.printStackTrace();
            //							End.exit();
            //						}
          }
          classificationStatisticsCalculator.updateStatistics(correctLabel,
              labelOfBestHMM,
              segmentInfo);

          //}
        }
        //<= delete if not using < min # frames
        //} else {
        //Print.dialog("frames # = " + pattern.getNumOfFrames());
        //}
        //					if (nnumberOfSegments > nnumberOfPatterns) {
        //						Print.error("nnumberOfSegments = " +
        //								nnumberOfSegments + " and " +
        //								"nnumberOfPatterns = " + nnumberOfPatterns);
        //						End.exit();
        //					}
        //}
      }
      //databaseManager.finalizeDataReading();
      Print.updateJProgressBar(nentryNumber + 1);
    }

    //		if (m_oshouldWriteLattices) {
    //			try {
    //				bufferedWriterErrors.close();
    //				bufferedWriterMatches.close();
    //			}
    //			catch (IOException e) {
    //				e.printStackTrace();
    //				End.exit();
    //			}
    //		}
    return classificationStatisticsCalculator;
  }

  public static void saveProperties(String outputTSTFileName,
                                    String jarHMMsFileName,
                                    boolean oisTriphone,
                                    boolean oisTraining,
                                    String directoryForDataLocators,
                                    String directoryForSetOfPatterns,
                                    String
                                    tableOfMonophoneLabelsForScoringFileName,
                                    String rootDirectoryWithHMMs,
                                    boolean oshouldWriteLattices,
                                    boolean oareTranscriptionsAvailable,
                                    int nminimumNumberOfFramesInValidPattern) {

    CMProperty[] properties = getDefaultProperties();
    setProperty(properties, "OffLineIsolatedSegmentsClassifier.hMMSetFileName",
                jarHMMsFileName);
    setProperty(properties,
                "OffLineIsolatedSegmentsClassifier.oshouldWriteLattices",
                oshouldWriteLattices);
    setProperty(properties,
                "OffLineIsolatedSegmentsClassifier.oareTranscriptionsAvailable",
                oareTranscriptionsAvailable);
    setProperty(properties, "OffLineIsolatedSegmentsClassifier.oisTraining",
                oisTraining);
    setProperty(properties,
                "OffLineIsolatedSegmentsClassifier.transcriptionsRootDirectory",
                directoryForDataLocators);
    setProperty(properties,
                "OffLineIsolatedSegmentsClassifier.parametersRootDirectory",
                directoryForSetOfPatterns);

    setProperty(properties,
                "OffLineIsolatedSegmentsClassifier.hmmsRootDirectory",
                rootDirectoryWithHMMs);
    setProperty(properties,
                "OffLineIsolatedSegmentsClassifier.tableOfLabelsForScoring",
                tableOfMonophoneLabelsForScoringFileName);
    setProperty(properties,
        "OffLineIsolatedSegmentsClassifier.nminimumNumberOfFramesInValidPattern",
                nminimumNumberOfFramesInValidPattern);

    File file = new File(outputTSTFileName);
    if (file.exists()) {
      file.delete();
    }
    CMUtilities.saveCMPropertiesAs(outputTSTFileName, properties,
                                   ConfigurationManipulator.DELIMITER);
  }

  public static void setProperty(CMProperty[] properties, String name,
                                 String value) {
    CMProperty property = CMUtilities.getCMPropertyFromArray(name, properties);
    String directoryForDataLocators = CMUtilities.setValue(property, value);
  }

  public static void setProperty(CMProperty[] properties, String name,
                                 boolean value) {
    setProperty(properties, name, new Boolean(value).toString());
  }

  public static void setProperty(CMProperty[] properties, String name,
                                 int value) {
    setProperty(properties, name, Integer.toString(value));
  }

  public static void writeResultsForAllSetsBelowGivenDirectory(CMPropertiesSet
      cMPropertiesSet) {
    CMPropertiesSet actualCMPropertiesSet = new CMPropertiesSet(
        getDefaultProperties());
    actualCMPropertiesSet.updateProperties(cMPropertiesSet);
    HeaderProperties headerProperties = actualCMPropertiesSet.
        getHeaderProperties();
    writeResultsForAllSetsBelowGivenDirectory(headerProperties);
  }

  public static void writeResultsForAllSetsBelowGivenDirectory(HeaderProperties
      headerProperties) {

//		//start with default values
//		CMProperty[] finalProperties = getDefaultProperties();
//		for (int i = 0; i < finalProperties.length; i++) {
//			//check for new value
//			CMProperty property = CMUtilities.getCMPropertyFromArray(finalProperties[i].getName(),properties);
//			if (property != null) {
//				//update
//				finalProperties[i] = property;
//			}
//		}

    Database.Type databaseType = null;
    String property = headerProperties.getPropertyAndExitIfKeyNotFound(
        "Database.Type");
    if (property != null) {
      databaseType = Database.Type.getTypeAndExitOnError(property);
    }
    else {
      databaseType = Database.Type.GENERAL;
    }

    String jarHMMsFileName = headerProperties.getPropertyAndExitIfKeyNotFound(
        "OffLineIsolatedSegmentsClassifier.hMMSetFileName");

    property = headerProperties.getPropertyAndExitIfKeyNotFound(
        "OffLineIsolatedSegmentsClassifier.oshouldWriteLattices");
    boolean oshouldWriteLattices = Boolean.valueOf(property).booleanValue();

    property = headerProperties.getPropertyAndExitIfKeyNotFound(
        "OffLineIsolatedSegmentsClassifier.oareTranscriptionsAvailable");
    boolean oareTranscriptionsAvailable = Boolean.valueOf(property).
        booleanValue();

    property = headerProperties.getPropertyAndExitIfKeyNotFound(
        "OffLineIsolatedSegmentsClassifier.oisTraining");
    boolean oisTraining = Boolean.valueOf(property).booleanValue();
    //not completely supported yet, always false
    boolean oisTriphone = false;

    String directoryForDataLocators = headerProperties.
        getPropertyAndExitIfKeyNotFound(
        "OffLineIsolatedSegmentsClassifier.transcriptionsRootDirectory");

    String directoryForSetOfPatterns = headerProperties.
        getPropertyAndExitIfKeyNotFound(
        "OffLineIsolatedSegmentsClassifier.parametersRootDirectory");

    String rootDirectoryWithHMMs = headerProperties.
        getPropertyAndExitIfKeyNotFound(
        "OffLineIsolatedSegmentsClassifier.hmmsRootDirectory");

    String tableOfLabelsForScoringFileName = headerProperties.
        getPropertyAndExitIfKeyNotFound(
        "OffLineIsolatedSegmentsClassifier.tableOfLabelsForScoring");

    TableOfLabels tableOfMonophoneLabelsForScoring = null;
    if (!tableOfLabelsForScoringFileName.equals("")) {
      tableOfMonophoneLabelsForScoring = new TableOfLabels(
          tableOfLabelsForScoringFileName);
    }

    property = headerProperties.getPropertyAndExitIfKeyNotFound(
        "OffLineIsolatedSegmentsClassifier.nminimumNumberOfFramesInValidPattern");
    int nminimumNumberOfFramesInValidPattern = Integer.parseInt(property);

//		CMProperty property = CMUtilities.getCMPropertyFromArray("hMMSetFileName", finalProperties);
//		String jarHMMsFileName = property.getValue();
//		property = CMUtilities.getCMPropertyFromArray("oshouldWriteLattices", finalProperties);
//		boolean oshouldWriteLattices = Boolean.valueOf(property.getValue());
//		property = CMUtilities.getCMPropertyFromArray("oareTranscriptionsAvailable", finalProperties);
//		boolean oareTranscriptionsAvailable = Boolean.valueOf(property.getValue());
//		property = CMUtilities.getCMPropertyFromArray("oisTraining", finalProperties);
//		boolean oisTraining = Boolean.valueOf(property.getValue());
//		//not completely supported yet, always false
//		boolean oisTriphone = false;
//
//		property = CMUtilities.getCMPropertyFromArray("transcriptionsRootDirectory", finalProperties);
//		String directoryForDataLocators = property.getValue();
//		property = CMUtilities.getCMPropertyFromArray("parametersRootDirectory", finalProperties);
//		String directoryForSetOfPatterns = property.getValue();
//		property = CMUtilities.getCMPropertyFromArray("hmmsRootDirectory", finalProperties);
//		String rootDirectoryWithHMMs = property.getValue();
//		property = CMUtilities.getCMPropertyFromArray("tableOfLabelsForScoring", finalProperties);
//		String tableOfLabelsForScoringFileName = property.getValue();
//
//		TableOfLabels tableOfMonophoneLabelsForScoring = new TableOfLabels(tableOfLabelsForScoringFileName);
//
//		property = CMUtilities.getCMPropertyFromArray("nminimumNumberOfFramesInValidPattern", finalProperties);
//		int nminimumNumberOfFramesInValidPattern = Integer.parseInt(property.getValue());

    writeResultsForAllSetsBelowGivenDirectory(jarHMMsFileName,
                                              oisTriphone,
                                              oisTraining,
                                              directoryForDataLocators,
                                              directoryForSetOfPatterns,
                                              tableOfMonophoneLabelsForScoring,
                                              rootDirectoryWithHMMs,
                                              oshouldWriteLattices,
                                              oareTranscriptionsAvailable,
                                              nminimumNumberOfFramesInValidPattern,
                                              databaseType);
  }

  /**
   *  boolean oareTranscriptionsAvailable == true means we do not have access to the
   *  DataLocator's or they are not in synchrony with the SOP files (if one skips
   *  short segments when writing SOP's for example)
   *
   */
  public static void writeResultsForAllSetsBelowGivenDirectory(String
      jarHMMsFileName,
      boolean oisTriphone,
      boolean oisTraining,
      String directoryForDataLocators,
      String directoryForSetOfPatterns,
      TableOfLabels tableOfMonophoneLabelsForScoring,
      String rootDirectoryWithHMMs,
      boolean oshouldWriteLattices,
      boolean oareTranscriptionsAvailable,
      int nminimumNumberOfFramesInValidPattern,
      Database.Type databaseType) {

    if (rootDirectoryWithHMMs == null ||
        rootDirectoryWithHMMs.equals("") ||
        !new File(rootDirectoryWithHMMs).exists()) {
      End.throwError("Directory \"" + rootDirectoryWithHMMs +
                     "\" is not valid.");
    }

    DirectoryTree directoryTree = new DirectoryTree(rootDirectoryWithHMMs,
        jarHMMsFileName, true);
    Vector files = directoryTree.getFiles();
//		HeaderProperties headerProperties = null;
//		FileWithHeaderReader fileWithHeaderReader = new FileWithHeaderReader(headerPropertiesFileName);
//		if (fileWithHeaderReader.wasFileOpenedSuccessfully() &&
//				fileWithHeaderReader.wasEndOfHeaderIdentifierFound()) {
//			headerProperties = fileWithHeaderReader.getHeaderProperties();
//			//headerProperties.list(System.out);
//		}
//		else {
//			End.throwError("Could not open file " + headerPropertiesFileName);
//		}

    int nnumberOfFiles = files.size();
    for (int i = 0; i < nnumberOfFiles; i++) {
      String fileName = (String) files.elementAt(i);
      if (isInPrototypesDirectory(fileName)) {
        //don't process if the file is inside a directory called 'prototypes'
        continue;
      }
      if (isInFinalModelsDirectories(fileName)) {
        //don't process if the file is inside a directory called 'finalmodels'
        continue;
      }

      SetOfHMMs setOfHMMs = SetOfHMMsFile.read(fileName);

      //ak To debug
      //setOfHMMs = ((SetOfSharedContinuousHMMs) setOfHMMs).convertToPlainHMMs();
      TableOfLabels tableOfLabelsForScoring = null;
      if (setOfHMMs.hasAnyTriphone()) {
        tableOfLabelsForScoring = TableOfLabels.createTableForScoringTriphones(
            tableOfMonophoneLabelsForScoring, setOfHMMs.getTableOfLabels());
      }
      else {
        tableOfLabelsForScoring = tableOfMonophoneLabelsForScoring;
      }

      String directoryWithHMMs = FileNamesAndDirectories.getPathFromFileName(
          fileName);
      String outputDirectory = null;
      if (databaseType == Database.Type.TIMIT) {
        if (oisTraining) {
          outputDirectory = FileNamesAndDirectories.forceEndingWithSlash(
              directoryWithHMMs) + "classification/train/";
        }
        else {
          outputDirectory = FileNamesAndDirectories.forceEndingWithSlash(
              directoryWithHMMs) + "classification/test/";
        }
      }
      else {
        if (oisTraining) {
          outputDirectory = FileNamesAndDirectories.forceEndingWithSlash(
              directoryWithHMMs) + "train/";
        }
        else {
          outputDirectory = FileNamesAndDirectories.forceEndingWithSlash(
              directoryWithHMMs) + "test/";
        }
      }

      //get an identifier to be written in confusion matrix
      String simulationIdentifier = null;
      SimulationPath simulationPath = new SimulationPath(outputDirectory +
          "anyfilename");
      if (simulationPath.m_oisValid) {
        simulationIdentifier = simulationPath.getIdentifier();
      }
      else {
        simulationIdentifier = IO.getDateAndTime();
      }

      ClassificationStatisticsCalculator classificationStatisticsCalculator = null;
      //ak it was the opposite: oareTranscriptionsAvailable would call the simplified?
      if (oareTranscriptionsAvailable) {
        classificationStatisticsCalculator = getResults(setOfHMMs,
            tableOfLabelsForScoring,
            directoryForDataLocators,
            directoryForSetOfPatterns,
            //oisTraining,
            outputDirectory,
            oshouldWriteLattices,
            nminimumNumberOfFramesInValidPattern,
            simulationIdentifier);
      }
      else {
        classificationStatisticsCalculator = getSimplifiedResults(setOfHMMs,
            tableOfLabelsForScoring,
            directoryForSetOfPatterns,
            //oisTraining,
            outputDirectory,
            oshouldWriteLattices,
            nminimumNumberOfFramesInValidPattern,
            simulationIdentifier);
      }
      String error = classificationStatisticsCalculator.generateReport(
          outputDirectory);

      if (m_nverbose > 0) {
        Print.dialog(IO.getEndOfString(fileName, 22) + " results: " + error);
      }
    }
  }

  /**
   *  Gets the InPrototypesDirectory attribute of the
   *  OffLineIsolatedSegmentsClassifier class
   *
   *@param  fileName  Description of Parameter
   *@return           The InPrototypesDirectory value
   */
  private static boolean isInPrototypesDirectory(String fileName) {
    File file = new File(fileName);
    String parent = file.getParent();
    parent = FileNamesAndDirectories.replaceAndForceEndingWithSlash(parent);
    if (parent.endsWith("/prototypes/")) {
      return true;
    }
    else {
      return false;
    }
  }

  /**
   *  Gets the InFinalModelsDirectories attribute of the
   *  OffLineIsolatedSegmentsClassifier class
   *
   *@param  fileName  Description of Parameter
   *@return           The InFinalModelsDirectories value
   */
  private static boolean isInFinalModelsDirectories(String fileName) {
    File file = new File(fileName);
    String parent = file.getParent();
    parent = FileNamesAndDirectories.replaceAndForceEndingWithSlash(parent);
    if (parent.endsWith("/finalmodels/")) {
      return true;
    }
    else {
      return false;
    }
  }

  /**
   *  Description of the Method
   *
   *@param  segmentInfo  Description of Parameter
   *@return              Description of the Returned Value
   */
  private static String extractPathFromFileName(String segmentInfo) {
    StringTokenizer stringTokenizer = new StringTokenizer(segmentInfo);
    String fullPath = stringTokenizer.nextToken();
    StringBuffer out = new StringBuffer(FileNamesAndDirectories.
                                        getFileNameFromPath(fullPath));
    while (stringTokenizer.hasMoreTokens()) {
      out.append(" " + stringTokenizer.nextToken());
    }
    return out.toString();
  }

  public static CMProperty[] getDefaultProperties() {
    CMProperty[] properties = new CMProperty[9];

    properties[0] = new StringCMProperty(
        "OffLineIsolatedSegmentsClassifier.hMMSetFileName",
        "Test: HMM file name",
        "HMM set file name usually with extension zip",
        SetOfHMMsFile.m_name);

    properties[1] = new BooleanCMProperty(
        "OffLineIsolatedSegmentsClassifier.oshouldWriteLattices",
        "Test: Generate lattices",
        "Generate files with best paths (lattices) if true",
        new Boolean(true));

    properties[2] = new BooleanCMProperty(
        "OffLineIsolatedSegmentsClassifier.oareTranscriptionsAvailable",
        "Test: Transcriptions are available",
        "If true transcriptions are available and more detailed results can be obtained",
        new Boolean(true));

    properties[3] = new BooleanCMProperty(
        "OffLineIsolatedSegmentsClassifier.oisTraining",
        "Test: Use training sequence for testing",
        "If true the results will be written in directory 'train', if false in 'test'",
        new Boolean(false));

    properties[4] = new IntegerCMProperty(
        "OffLineIsolatedSegmentsClassifier.nminimumNumberOfFramesInValidPattern",
        "Test: Minimum # of frames",
        "Minimum number of frames for pattern to be valid",
        new Boolean(true), new Boolean(true), new Boolean(false),
        new Boolean(false), new Boolean(true),
        new Integer(1), new Integer(1), null);

    properties[5] = new PathStringCMProperty(
        "OffLineIsolatedSegmentsClassifier.hmmsRootDirectory",
        "Test: HMM's root directory",
        "All HMM files in subdirectories below this root will be tested",
        null, null, true, null);

    properties[6] = new PathStringCMProperty(
        "OffLineIsolatedSegmentsClassifier.parametersRootDirectory",
        "Test: Parameters directory",
        "Directory with the test parameters files (" +
        SetOfPatterns.m_FILE_EXTENSION + " files storing MFCC's or others)",
        null, null, true, null);

    properties[7] = new PathStringCMProperty(
        "OffLineIsolatedSegmentsClassifier.transcriptionsRootDirectory",
        "Test: Transcriptions root directory",
        "Directory with DTL files. Not used if transcriptions not available",
        null, null, true, null);

    properties[8] = new PathStringCMProperty(
        "OffLineIsolatedSegmentsClassifier.tableOfLabelsForScoring",
        "Test: Scoring table file name",
        "Table with labels used for scoring",
        null, TableOfLabels.m_FILE_EXTENSION, false,
        "files with table of labels");

    return properties;
  }

}

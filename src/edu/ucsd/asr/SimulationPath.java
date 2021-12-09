package edu.ucsd.asr;

import java.util.StringTokenizer;

/**
 * Change name: valid for confusion matrices only...
 */
public class SimulationPath {

//File oriented:
//D:\besta\mfcc2w256s200\hmms\leftright5states\baumwelch\1\classification\test\file.t

//TIMIT:
//D:\simulations\timit\60models\lsfeda39w512s160\
//lrforwardskips5\monophones\isolated\baumwelch\1\classification\test\file.t

  String m_table;
  String m_features;
  String m_topology;
  String m_monoOrTriphones;
  String m_isolatedOrSentence;
  String m_trainingAlgorithm;
  String m_nnumberOfGaussians;
  String m_classificationOrRecognition;
  String m_testOrTrain;
  String m_fileName;

  boolean m_oisCompletePath;

  boolean m_oisValid = true;

  public SimulationPath(String path) {
    path = FileNamesAndDirectories.replaceBackSlashByForward(path);
    StringTokenizer stringTokenizer = new StringTokenizer(path, "/");
    int nnumberOfTokens = stringTokenizer.countTokens();

    //if (nnumberOfTokens != 13) {
    //at least
    String[] subdirs = new String[nnumberOfTokens];
    for (int i = 0; i < subdirs.length; i++) {
      subdirs[i] = stringTokenizer.nextToken();
    }
    int nlast = subdirs.length - 1;
    if (isTIMITPath(subdirs)) {
      //find the index number of train or test
      int ndx = -1;
      for (int i = 0; i < subdirs.length; i++) {
        if (subdirs[i].equalsIgnoreCase("test") || subdirs[i].equalsIgnoreCase("train")) {
          ndx = i;
          break;
        }
      }
      if (subdirs[ndx - 1].equalsIgnoreCase("classification") || subdirs[ndx - 1].equalsIgnoreCase("recognition")) {
        m_table = subdirs[ndx - 8];
        m_features = subdirs[ndx - 7];
        m_topology = subdirs[ndx - 6];
        m_monoOrTriphones = subdirs[ndx - 5];
        m_isolatedOrSentence = subdirs[ndx - 4];
        m_trainingAlgorithm = subdirs[ndx - 3];
        m_nnumberOfGaussians = subdirs[ndx - 2];
        m_classificationOrRecognition = subdirs[ndx - 1];
        m_testOrTrain = subdirs[ndx];
        m_fileName = subdirs[ndx + 1];
      } else {
        m_table = subdirs[ndx - 7];
        m_features = subdirs[ndx - 6];
        m_topology = subdirs[ndx - 5];
        m_monoOrTriphones = subdirs[ndx - 4];
        m_isolatedOrSentence = subdirs[ndx - 3];
        m_trainingAlgorithm = subdirs[ndx - 2];
        m_nnumberOfGaussians = subdirs[ndx - 1];
        //m_classificationOrRecognition = subdirs[ndx - 1];
        m_testOrTrain = subdirs[ndx];
        m_fileName = subdirs[ndx + 1];
      }
      m_oisCompletePath = true;
    } else if (isFileOrientedPath(subdirs)) {
      m_table = null;
      m_features = subdirs[nlast - 6];
      m_topology = subdirs[nlast - 4];
      m_monoOrTriphones = null;
      m_isolatedOrSentence = null;
      m_trainingAlgorithm = subdirs[nlast - 3];
      m_nnumberOfGaussians = subdirs[nlast - 2];
      m_classificationOrRecognition = null;
      m_testOrTrain = subdirs[nlast - 1];
      m_fileName = subdirs[nlast];

      m_oisCompletePath = false;
    } else {
      m_oisValid = false;
      return;
    }
  }

  public boolean isComplete() {
    return m_oisCompletePath;
  }

  public boolean isValid() {
    return m_oisValid;
  }

  private boolean isTIMITPath(String[] subdirs) {
    if (subdirs.length < 10) {
      m_oisValid = false;
      return false;
    }

    int nlast = subdirs.length - 1;

    //take in account that "classification or recognition" directory may no exist
    if ( (subdirs[nlast - 4].equals("isolated") ||
          subdirs[nlast - 4].equals("sentences")) &&
        (subdirs[nlast - 5].equals("monophones") ||
         subdirs[nlast - 5].equals("triphones"))) {
     nlast++;
   }

    if ( (subdirs[nlast - 5].equals("isolated") ||
          subdirs[nlast - 5].equals("sentences")) &&
        (subdirs[nlast - 6].equals("monophones") ||
         subdirs[nlast - 6].equals("triphones"))) {

      return true;
    }
    else {
      return false;
    }
  }

  private boolean isFileOrientedPath(String[] subdirs) {
    if (subdirs.length < 7) {
      m_oisValid = false;
      return false;
      //Print.error(path + " is wrong because it has # tokens = " + nnumberOfTokens);
    }
    int nlast = subdirs.length - 1;
    if (subdirs[nlast - 5].equals("hmms")) {
      return true;
    }
    else {
      return false;
    }
  }

  public String getCompleteIdentifier() {
    return m_table +
        m_features +
        m_topology +
        //m_monoOrTriphones + " " +
        //m_isolatedOrSentence + " " +
        m_trainingAlgorithm +
        m_nnumberOfGaussians + "G" +
        //m_classificationOrRecognition + " " +
        m_testOrTrain;
  }

  public String getNotCompleteIdentifier() {
    return m_features +
        m_topology +
        //m_monoOrTriphones + " " +
        //m_isolatedOrSentence + " " +
        m_trainingAlgorithm +
        m_nnumberOfGaussians + "G" +
        //m_classificationOrRecognition + " " +
        m_testOrTrain;
  }

  public String getIdentifier() {
    if (!m_oisValid) {
      return "Invalid_path";
    }
    if (m_oisCompletePath) {
      return getCompleteIdentifier();
    }
    else {
      return getNotCompleteIdentifier();
    }
  }

  public String toStringComplete() {
    return m_table + " " +
        m_features + " " +
        m_topology + " " +
        //m_monoOrTriphones + " " +
        //m_isolatedOrSentence + " " +
        m_trainingAlgorithm + " " +
        m_nnumberOfGaussians + " " +
        //m_classificationOrRecognition + " " +
        m_testOrTrain;
  }

  public String toStringNotComplete() {
    return m_features + " " +
        m_topology + " " +
        //m_monoOrTriphones + " " +
        //m_isolatedOrSentence + " " +
        m_trainingAlgorithm + " " +
        m_nnumberOfGaussians + " " +
        //m_classificationOrRecognition + " " +
        m_testOrTrain;
  }

  public String toString() {
    if (!m_oisValid) {
      return "Invalid_path";
    }
    if (m_oisCompletePath) {
      return toStringComplete();
    }
    else {
      return toStringNotComplete();
    }
  }
}

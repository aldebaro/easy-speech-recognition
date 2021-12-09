package edu.ucsd.asr;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.DataInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Vector;
import java.util.StringTokenizer;
import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.File;

/**Organizes a set of Pattern objects,
 * for constructing a set of templates for
 * a given utterance (word, phoneme, etc.).
 * Aldebaro Klautau
 * @version 2.0 - September 07, 2000.
 * @see Pattern
*/
public class SetOfPatterns {

  public static final String m_FILE_EXTENSION = "FEA";
  public static final String m_RANGES_FILENAME = "ranges.txt";

  private PatternGenerator m_patternGenerator;

  private String m_labels;

  private int m_nminimumNumberOfFrames;

  private int m_nmaximumNumberOfFrames;

  private float m_fminimumParameterValue;

  private float m_fmaximumParameterValue;

  private float m_daverageNumberOfFrames;

  private String m_dataLocatorFileName;

  /** Vector storing this set of patterns.*/
  private Vector m_patterns;

  /**Creates a new empty SetOfPatterns with the parametric
   * representation given by nparametricRepresentationType.
   */
  public SetOfPatterns(PatternGenerator patternGenerator){
    m_patterns = new Vector();
    m_patternGenerator = patternGenerator;
    //initialize
    m_nminimumNumberOfFrames = Integer.MAX_VALUE;
    m_nmaximumNumberOfFrames = -Integer.MAX_VALUE;
    m_fminimumParameterValue = Float.MAX_VALUE;
    m_fmaximumParameterValue = -Float.MAX_VALUE;
    m_daverageNumberOfFrames = 0.0F;
  }

  /**Constructor that reads an object SetofPatterns from a binary file.
   */
  public SetOfPatterns(String inputFilename) {
    m_patterns = new Vector();
    readFromFile(inputFilename);
  }

  /**
   * Without much complications, we just load the external format pattern and
   * not worry about any properties.
   *
   * @param inputFileName the name of the file containing the necessary information
   * @param numFeatures Number of features to be loaded from the file, out of the
   *        totalFeatures that include derivatives of the original features.
   * @param totalFeatures Total number of features in the observation vector.
   *        Specifies the size of the vector to be allocated when loading static
   *        features
   * @param deltaWin The window size for derivation.
   * @param format External format identification.
   */
  public SetOfPatterns( String inputFileName, int numFeatures, int totalFeatures,
                        int deltaWin, ExternalFormatType format ) {
    if( format.equals( ExternalFormatType.ISIP_BINARY ) ) {
      m_patterns = new Vector();

      File file = new File(inputFileName);
      //check integrity of the file
      if( !file.exists() || !file.canRead() ) {
        Print.error("File does not exist or is not readable.");
        End.exit(0);
      }

      int lengthInBytes = (int) file.length();
      int nframes = lengthInBytes/8/numFeatures;
      /* arbitrary initialization options for MFCCPatternGenerator. However needs
       * correct information if we want to save sop file for the dimension space,
       * obtained from info about #mfcc, energy, derivatives, etc.
       */
      MFCCPatternGenerator patGen = new MFCCPatternGenerator( 200, 80, 8000.0,
              256, 12, 24, 0.97, true, 22, -1.0e+10, deltaWin, 50, true, true,
              true, false, true, true, true, true );

      m_nmaximumNumberOfFrames = nframes;
      m_nminimumNumberOfFrames = nframes;
      m_daverageNumberOfFrames = (float) nframes;
      m_patternGenerator = patGen;
      //this.m
//      Print.dialog( "Filename: " + inputFileName );
//      Print.dialog( "lengthInBytes: " + lengthInBytes +
//                    " numberOfFrames: " + nframes );
      //check if it were divisible in the first place...
      if( nframes*8*numFeatures != lengthInBytes ) {
        Print.error("File size: "+lengthInBytes+
                    " was not divisible by 8 and numfeatures: " +numFeatures);
        Print.error( "Filename was: " + inputFileName );
        End.exit(0);
      }
      DataInputStream din = IO.openFileDataInputStream( inputFileName );
      double[] tempFrame;
      float[][] fdata = new float[nframes][];
      for( int i=0; i<nframes; i++ ) {
        tempFrame = IO.readLittleEndianDoubleVector( din, numFeatures );
        fdata[i] = new float[totalFeatures];
        for( int j=0; j<numFeatures; j++ ) {
          fdata[i][j] = (float) tempFrame[j];
        }
      }
      patGen.estimateDerivativeUsingRegression( fdata, 0, 13, deltaWin );
      patGen.estimateDerivativeUsingRegression( fdata, 13, 13, deltaWin );
      //simply add the read pattern to the SOP.
      m_patterns.add(new Pattern(fdata));
    } else if( format.equals(ExternalFormatType.ISIP_ASCII) ) {
      Print.dialog("ISIP_ASCII not yet implemented at this time.");
      End.exit(0);
    }
  }

  private void readFromFileWithSymbolicLinks(String inputFilename,
                                             HeaderProperties headerProperties) {
    String temp = headerProperties.getPropertyAndExitIfKeyNotFound(
        "SetOfPatterns.nnumberOfFilesToInclude");
    int nnumberOfFilesToInclude = Integer.parseInt(temp);
    m_patterns = new Vector();
    m_patternGenerator = null;
    //initialize
    m_nminimumNumberOfFrames = Integer.MAX_VALUE;
    m_nmaximumNumberOfFrames = -Integer.MAX_VALUE;
    m_fminimumParameterValue = Float.MAX_VALUE;
    m_fmaximumParameterValue = -Float.MAX_VALUE;
    m_daverageNumberOfFrames = 0.0F;
    for (int i = 0; i < nnumberOfFilesToInclude; i++) {
      temp = headerProperties.getPropertyAndExitIfKeyNotFound("SetOfPatterns.File_" + i);
      SetOfPatterns setOfPatterns = new SetOfPatterns(temp);
      if (i == 0) {
        //first file, so initialize m_patternGenerator
        m_patternGenerator = setOfPatterns.getPatternGenerator();
      } else {
        if (!m_patternGenerator.equals(setOfPatterns.getPatternGenerator())) {
          End.throwError("Different PatternGenerator's in file " + inputFilename);
        }
      }
      int nnumberOfPatterns = setOfPatterns.getNumberOfPatterns();
      for (int j = 0; j < nnumberOfPatterns; j++) {
        Pattern pattern = setOfPatterns.getPattern(j);
        //add to this SetOfPatterns
        addPattern(pattern);
      }
    }
  }

  /**Reads an object SetofPatterns to a binary file with a header.
   * The header is written as 2048 bytes of text (ASCII) and then it comes
   * the binary data where the first two entries are float numbers
   * numOfPatterns and spaceDimension (number of features),
   * then for each pattern, from i = 1 to numOfPatterns there is one float
   * numOfFrames and then the matrix of floats of dimension numOfFrames by
   * spaceDimension representing the i-th pattern
   * All numbers are written in Java big-endian format (MSB byte comes before LSB).
   * Notice that this implies that all Pattern objects in a given
   * SetOfPatterns share the same space dimension.
   */
  private void readFromFile(String inputFilename) {
    try {
      FileWithHeaderReader fileWithHeaderReader = new FileWithHeaderReader(inputFilename);
      if (!fileWithHeaderReader.wasFileOpenedSuccessfully() ||
          !fileWithHeaderReader.wasEndOfHeaderIdentifierFound()) {
        Print.error(
            "SetOfPatterns: FileWithHeaderReader.wasItOpenedSuccessfully failed.");
        End.exit();
      }

      HeaderProperties headerProperties = fileWithHeaderReader.getHeaderProperties();
      //System.out.println(headerProperties.toString());

      //check if this file only has a list of files to include (called symbolic links here)
      String temp = headerProperties.getProperty("SetOfPatterns.nnumberOfFilesToInclude");
      if (temp != null) {
        //in this case read using a different method
        readFromFileWithSymbolicLinks(inputFilename, headerProperties);
        return;
      }

      temp = headerProperties.getProperty("SetOfPatterns.nmaximumNumberOfFrames");
      if (temp == null) {
        End.throwError(
            "Couldn't find property SetOfPatterns.nmaximumNumberOfFrames in header of file " +
            inputFilename);
      } else {
        m_nmaximumNumberOfFrames = Integer.parseInt(temp);
      }
      temp = headerProperties.getProperty("SetOfPatterns.nminimumNumberOfFrames");
      if (temp == null) {
        End.throwError(
            "Couldn't find property SetOfPatterns.nminimumNumberOfFrames in header of file " +
            inputFilename);
      } else {
        m_nminimumNumberOfFrames = Integer.parseInt(temp);
      }

      temp = headerProperties.getPropertyAndExitIfKeyNotFound(
          "SetOfPatterns.nmaximumNumberOfFrames");
      m_fmaximumParameterValue = Float.parseFloat(temp);
      temp = headerProperties.getPropertyAndExitIfKeyNotFound(
          "SetOfPatterns.nminimumNumberOfFrames");
      m_fminimumParameterValue = Float.parseFloat(temp);

      temp = headerProperties.getProperty(
          "SetOfPatterns.daverageNumberOfFrames");
      if (temp == null) {
        //AK
        //Print.error("SetOfPatterns: couldn't find SetOfPatterns.daverageNumberOfFrames in header.");
        //End.exit(1);
        m_daverageNumberOfFrames = m_nminimumNumberOfFrames;
      } else {
        m_daverageNumberOfFrames = Float.parseFloat(temp);
      }

      if (m_nminimumNumberOfFrames > m_nmaximumNumberOfFrames) {
        Print.error(inputFilename +
                    " SetOfPatterns: m_nminimumNumberOfFrames = " +
                    m_nminimumNumberOfFrames +
                    " is greater than m_nmaximumNumberOfFrames = " +
                    m_nmaximumNumberOfFrames + ".");
        End.exit(1);
      }

      if (m_daverageNumberOfFrames < m_nminimumNumberOfFrames ||
          m_daverageNumberOfFrames > m_nmaximumNumberOfFrames) {
        Print.error(inputFilename +
                    " SetOfPatterns: m_daverageNumberOfFrames = " +
                    m_daverageNumberOfFrames +
            " is outside the range [m_nminimumNumberOfFrames,m_nmaximumNumberOfFrames] = [" +
                    m_nminimumNumberOfFrames + ", " + m_nmaximumNumberOfFrames +
                    "].");
        End.exit(1);
      }

      m_patternGenerator = PatternGenerator.getPatternGenerator(
          headerProperties);

      m_dataLocatorFileName = headerProperties.getProperty(
          "DataLocator.FileName");
      if (m_dataLocatorFileName == null) {
        //XXX compatibility with old version
        m_dataLocatorFileName = headerProperties.getProperty(
            "Segments.FileName");
        m_dataLocatorFileName = FileNamesAndDirectories.substituteExtension(
            m_dataLocatorFileName, DataLocator.m_FILE_EXTENSION);
      }

      m_labels = headerProperties.getProperty("SetOfPatterns.Labels");

      temp = headerProperties.getProperty("SetOfPatterns.nnumberOfPatterns");
      int nnumOfPatterns = -1;
      if (temp == null) {
        Print.error(inputFilename +
            " SetOfPatterns: couldn't find SetOfPatterns.nnumberOfPatterns in header.");
        End.exit(1);
      } else {
        //parseInt() doesn't accept blank spaces, e.g. "40     "
        nnumOfPatterns = Integer.parseInt(temp.trim());
      }

      temp = headerProperties.getProperty("SetOfPatterns.nspaceDimension");
      int nspaceDimension = -1;
      if (temp == null) {
        Print.error(inputFilename +
            " SetOfPatterns: couldn't find SetOfPatterns.nspaceDimension in header.");
        End.exit(1);
      } else {
        //parseInt() doesn't accept blank spaces, e.g. "40     "
        nspaceDimension = Integer.parseInt(temp.trim());
      }

      DataInputStream inputFile = fileWithHeaderReader.getDataWithoutHeader();

      int ntemp = (int) inputFile.readFloat();
      if (ntemp != nnumOfPatterns) {
        End.throwError("nnumOfPatterns = " + nnumOfPatterns + " != ntemp = " +
                       ntemp);
      }

      ntemp = (int) inputFile.readFloat();
      if (ntemp != nspaceDimension) {
        End.throwError("nspaceDimension = " + nspaceDimension +
                       " != ntemp = " + ntemp);
      }

      for (int i = 0; i < nnumOfPatterns; i++) {
        int nnumOfFrames = (int) inputFile.readFloat();

        float[][] fparameters = new float[nnumOfFrames][nspaceDimension];
        for (int j = 0; j < nnumOfFrames; j++) {
          for (int k = 0; k < nspaceDimension; k++) {
            fparameters[j][k] = inputFile.readFloat();
          }
        }
        m_patterns.addElement(new Pattern(fparameters));
      }

      inputFile.close();
    } catch (IOException e) {
      e.printStackTrace();
      End.exit();
    }
  }

  /**Get current number of Patterns of this SetOfPatterns.
   */
  public int getNumberOfPatterns() {
    return m_patterns.size();
  }

  /**Get maximum number of frames in a Pattern of this SetOfPatterns.
   */
  public int getMaximumNumberOfFrames() {
    return m_nmaximumNumberOfFrames;
  }

  /**Get minimum number of frames in a Pattern of this SetOfPatterns.
   */
  public int getMinimumNumberOfFrames() {
    return m_nminimumNumberOfFrames;
  }

  /**Get average number of frames in a Pattern of this SetOfPatterns.
   */
  public float getAverageNumberOfFrames() {
    return m_daverageNumberOfFrames;
  }

  /**Get the PatternGenerator that generated the
   * parametric representation of this SetOfPatterns.
   */
  public PatternGenerator getPatternGenerator() {
    return m_patternGenerator;
  }

  /**Get the space dimension (number of elements of each vector
   * that compose a Pattern).
   */
  public int getSpaceDimension() {
    return m_patternGenerator.getNumberOfParameters();
  }

  /**Add the given Pattern to this SetOfPatterns.
   */
  public void addPattern(Pattern pattern) {
    //check if Pattern is valid
    if ( (pattern == null) || (pattern.getParameters() == null)) {
      Print.warning("A null Pattern was the input argument " +
                    "for method SetOfPatterns.addPattern()." +
                    " This Pattern was not added.");
    } else {
      m_patterns.addElement(pattern);
      updateFramesStatistic(pattern);
    }
  }

  public float getMaximumParameterValue() {
    return m_fmaximumParameterValue;
  }

  public float getMinimumParameterValue() {
    return m_fminimumParameterValue;
  }

  private void updateFramesStatistic(Pattern pattern) {
    int nnumberOfFrames = pattern.getNumOfFrames();
    if (nnumberOfFrames > this.m_nmaximumNumberOfFrames) {
      m_nmaximumNumberOfFrames = nnumberOfFrames;
    }
    if (nnumberOfFrames < this.m_nminimumNumberOfFrames) {
      m_nminimumNumberOfFrames = nnumberOfFrames;
    }
    float fmin = pattern.getMinimumParameterValue();
    float fmax = pattern.getMaximumParameterValue();
    if (fmin < m_fminimumParameterValue) {
      m_fminimumParameterValue = fmin;
    }
    if (fmax > m_fmaximumParameterValue) {
      m_fmaximumParameterValue = fmax;
    }
    //running average calculation
    m_daverageNumberOfFrames = (m_daverageNumberOfFrames *
                                (getNumberOfPatterns() - 1) + nnumberOfFrames) /
                               getNumberOfPatterns();
  }

  /**Get the vector of Patterns represented by this SetOfPatterns.
   */
  public Vector getSetOfPatterns() {
    return m_patterns;
  }

  /**Get a specific Pattern of this SetOfPatterns, which index
   * is npatternIndex.
   */
  public Pattern getPattern(int npatternIndex) {
    if (npatternIndex < 0 | npatternIndex > m_patterns.size() - 1) {
      End.throwError("Error in SetOfPatterns.getPattern(): index = " +
                     npatternIndex + " is outside valid range [0, " +
                     (m_patterns.size() - 1) + "]." +
                     " DataLocator for this file is " + m_dataLocatorFileName);
      return null; //make compiler happy
    } else {
      return (Pattern) m_patterns.elementAt(npatternIndex);
    }
  }

  /**Set the Pattern in the position specified by npatternIndex
   * equal to pattern, with the previous Pattern at that position
   * being discarded.
   * Notice that after using this method, the statistic of minimum
   * and maximum number of frames can be wrong, in case the user
   * overwrites the Pattern responsible for the maximum or minimum
   * number of frames in this SetOfPatterns.
   */
  public void setPattern(int npatternIndex, Pattern pattern) {
    if (npatternIndex < 0 | npatternIndex > m_patterns.size() - 1) {
      System.out.println("Error in SetOfPatterns.getPattern(): index = " +
                         npatternIndex + " is outside valid range [0, " +
                         (m_patterns.size() - 1) + "]");
    } else {
      //notice that after this method, the maximum and minimum
      //values are possibly not correct anymore.
      //Take care to keep correct the number of frames statistics after substituting a Pattern
      Pattern oldPattern = (Pattern) m_patterns.elementAt(npatternIndex);
      int noldNumFrames = oldPattern.getNumOfFrames();
      int nnewNumFrames = pattern.getNumOfFrames();
      if (nnewNumFrames > m_nmaximumNumberOfFrames) {
        m_nmaximumNumberOfFrames = nnewNumFrames;
      }
      if (nnewNumFrames < m_nminimumNumberOfFrames) {
        m_nminimumNumberOfFrames = nnewNumFrames;
      }
      if (noldNumFrames == m_nminimumNumberOfFrames) {
        Print.warning("Pattern with " + noldNumFrames +
                      " frames was substituted by another Pattern using " +
                      " SetOfPatterns.setPattern(), where " + noldNumFrames +
            " is the minimum number of frames in this SetOfPatterns. " +
            " The system will keep this minimum number of frames " +
            " but this statistic can be eventually wrong now.");
      }
      if (noldNumFrames == m_nmaximumNumberOfFrames) {
        Print.warning("Pattern with " + noldNumFrames +
                      " frames was substituted by another Pattern using " +
                      " SetOfPatterns.setPattern(), where " + noldNumFrames +
            " is the maximum number of frames in this SetOfPatterns. " +
            " The system will keep this maximum number of frames " +
            " but this statistic can be eventually wrong now.");
      }
      int ntotalNumFrames = (int) (this.m_daverageNumberOfFrames *
                                   this.getNumberOfPatterns());
      ntotalNumFrames -= noldNumFrames;
      ntotalNumFrames += nnewNumFrames;
      m_daverageNumberOfFrames = ntotalNumFrames / getNumberOfPatterns();

      //now, substitute the old by the new Pattern
      m_patterns.setElementAt(pattern, npatternIndex);
    }
  }

  public void writeToFile(String outputFilename) {
    String segmentsFileName = "NOT_SPECIFIED";
    String labels = "NOT_SPECIFIED";
    writeToFile(outputFilename, segmentsFileName, labels);
  }

  /**Writes an object SetofPatterns to a binary file with a header.
   * The header is written as 2048 bytes of text (ASCII) and then it comes
   * the binary data where the first two entries are float numbers
   * numOfPatterns and spaceDimension (number of features),
   * then for each pattern, from i = 1 to numOfPatterns there is one float
   * numOfFrames and then the matrix of floats of dimension numOfFrames by
   * spaceDimension representing the i-th pattern
   * All numbers are written in Java big-endian format (MSB byte comes before LSB).
   */
  public void writeToFile(String outputFilename, String dataLocatorFileName,
                          String labels) { //, PatternGenerator patternGenerator) {

    //check if it has the mandatory extension
    FileNamesAndDirectories.checkExtensionWithCaseIgnoredAndExitOnError(
        outputFilename,
        m_FILE_EXTENSION);

    //if set has 0 Patterns (none was added) avoid writing numbers used for initializing the search for max & min
    m_nmaximumNumberOfFrames = (m_nmaximumNumberOfFrames == -Integer.MAX_VALUE) ?
        0 : m_nmaximumNumberOfFrames;
    m_nminimumNumberOfFrames = (m_nminimumNumberOfFrames == Integer.MAX_VALUE) ?
        0 : m_nminimumNumberOfFrames;

    m_dataLocatorFileName = dataLocatorFileName;
    m_labels = labels;

    try {
      //create header
      String originalHeader = this.toString();

      //check if the total size is ok and padd with zeros if necessary
      //to complete specified number of bytes in header
      //TODO: deal with possibility of a big header
      String finalHeader = FileWithHeaderWriter.formatHeader(originalHeader,
          2048);

      //open an output file
      FileOutputStream fileOutputStream = new FileOutputStream(outputFilename);
      DataOutputStream dataOutputStream = new DataOutputStream(fileOutputStream);

      //writes the header
      dataOutputStream.writeBytes(finalHeader);

      int nnumOfPatterns = this.getNumberOfPatterns();
      int nspaceDimension = this.getSpaceDimension();

      //writes the data in binary form

      dataOutputStream.writeFloat(nnumOfPatterns);

//			//if set has 0 patterns, write just necessary header information
//			if (nnumOfPatterns == 0) {
//				outputFile.writeFloat(1.0F); //some float number
//				outputFile.close();
//				return;
//			}

      dataOutputStream.writeFloat(nspaceDimension);

      for (int i = 0; i < nnumOfPatterns; i++) {
        int nnumOfFrames = ( (Pattern) m_patterns.elementAt(i)).getNumOfFrames();
        dataOutputStream.writeFloat(nnumOfFrames);
        float[][] fparameters = ( (Pattern) m_patterns.elementAt(i)).
            getParameters();

        //IO.DisplayMatrix(fparameters);

        for (int j = 0; j < nnumOfFrames; j++) {
          for (int k = 0; k < nspaceDimension; k++) {
            dataOutputStream.writeFloat( (float) fparameters[j][k]);
          }
        }

        //System.out.println("nnumOfFrames = " + nnumOfFrames);
      }
      fileOutputStream.close();
      dataOutputStream.close();

    } catch (IOException e) {
      e.printStackTrace();
      End.exit();
    }

  }

  /**
   * Writes an object SetofPatterns to a text file.
   * First line is number of patterns, second is # of features,
   * then for each pattern:
   *   number of frames
   *   for each frame one vector
   */
  public void writeToTextFile(String outputFilename) {

    BufferedWriter bufferedWriter = IO.openBufferedWriter(outputFilename);
    int nnumOfPatterns = this.getNumberOfPatterns();
    int nspaceDimension = this.getSpaceDimension();

    IO.writeLineToWriter(bufferedWriter, "" + nnumOfPatterns);
    IO.writeLineToWriter(bufferedWriter, "" + nspaceDimension);

    for (int i = 0; i < nnumOfPatterns; i++) {
      int nnumOfFrames = ( (Pattern) m_patterns.elementAt(i)).getNumOfFrames();
      IO.writeLineToWriter(bufferedWriter, "" + nnumOfFrames);

      float[][] fparameters = ( (Pattern) m_patterns.elementAt(i)).
          getParameters();

      //IO.DisplayMatrix(fparameters);

      try {
        for (int j = 0; j < nnumOfFrames; j++) {
          bufferedWriter.write("" + fparameters[j][0]);
          for (int k = 1; k < nspaceDimension; k++) {
            bufferedWriter.write(" " + fparameters[j][k]);
          }
          bufferedWriter.newLine();
        }
      }
      catch (Exception e) {
        End.throwError("Problem writing file " + outputFilename);
      }

      //System.out.println("nnumOfFrames = " + nnumOfFrames);
    }
    IO.closeBufferedWriter(bufferedWriter);
  }

  public void dumpBinaryData() {

	    int nnumOfPatterns = this.getNumberOfPatterns();
	    int nspaceDimension = this.getSpaceDimension();

	    
	    System.out.println("" + nnumOfPatterns);
	    System.out.println("" + nspaceDimension);

	    for (int i = 0; i < nnumOfPatterns; i++) {
	      int nnumOfFrames = ( (Pattern) m_patterns.elementAt(i)).getNumOfFrames();
	      System.out.println("" + nnumOfFrames);

	      float[][] fparameters = ( (Pattern) m_patterns.elementAt(i)).
	          getParameters();

	      IO.DisplayMatrix(fparameters);

	    }
	  }
  
  
  public String getDataLocatorFileName() {
    return m_dataLocatorFileName;
  }

  public float getMaximumValueOfGivenParameter(int nparameterNumber) {
    int ndimension = getSpaceDimension();
    if (nparameterNumber < 0 || nparameterNumber > ndimension - 1) {
      End.throwError(nparameterNumber + " is outside valid range [0, " +
                     ndimension + "]");
    }
    float fmax = -Float.MAX_VALUE;
    int nnumberOfPatterns = m_patterns.size();
    for (int i = 0; i < nnumberOfPatterns; i++) {
      Pattern pattern = (Pattern) m_patterns.elementAt(i);
      float thisMax = pattern.getMaximumValueOfGivenParameter(nparameterNumber);
      if (thisMax > fmax) {
        fmax = thisMax;
      }
    }
    return fmax;
  }

  public float getMinimumValueOfGivenParameter(int nparameterNumber) {
    int ndimension = getSpaceDimension();
    if (nparameterNumber < 0 || nparameterNumber > ndimension - 1) {
      End.throwError(nparameterNumber + " is outside valid range [0, " +
                     ndimension + "]");
    }
    float fmin = Float.MAX_VALUE;
    int nnumberOfPatterns = m_patterns.size();
    for (int i = 0; i < nnumberOfPatterns; i++) {
      Pattern pattern = (Pattern) m_patterns.elementAt(i);
      float thisMin = pattern.getMinimumValueOfGivenParameter(nparameterNumber);
      if (thisMin < fmin) {
        fmin = thisMin;
      }
    }
    return fmin;
  }

  public float[][] getDynamicRange() {
    int ndimension = getSpaceDimension();
    float[][] franges = new float[ndimension][2];
    for (int i = 0; i < franges.length; i++) {
      franges[i][0] = getMinimumValueOfGivenParameter(i);
      franges[i][1] = getMaximumValueOfGivenParameter(i);
    }
    return franges;
  }

  public static float[][] getDynamicRanges(String rootDirectory) {
    String fileName = FileNamesAndDirectories.concatenateTwoPaths(rootDirectory,
        m_RANGES_FILENAME);
    Vector vector = IO.readVectorOfStringsFromFile(fileName);
    int nsize = vector.size();
    if (nsize < 1) {
      End.throwError(fileName + " seems to be empty");
    }
    float[][] franges = new float[nsize][2];
    for (int i = 0; i < nsize; i++) {
      String line = (String) vector.elementAt(i);
      StringTokenizer stringTokenizer = new StringTokenizer(line);
      int nindex = Integer.parseInt(stringTokenizer.nextToken());
      if (nindex != i) {
        End.throwError("Should read " + i + " but obtained " + nindex);
      }
      franges[i][0] = Float.parseFloat(stringTokenizer.nextToken());
      franges[i][1] = Float.parseFloat(stringTokenizer.nextToken());
    }
    return franges;
  }

  public static void createFileWithDynamicRanges(String rootDirectory) {
    DirectoryTree directoryTree = new DirectoryTree(rootDirectory,
        m_FILE_EXTENSION);
    Vector files = directoryTree.getFiles();
    int nnumberOfFiles = files.size();
    if (nnumberOfFiles < 1) {
      End.throwError("Could not find any file with extension " +
                     m_FILE_EXTENSION +
                     " under root directory " + rootDirectory);
    }
    //get information about set of parameters used from first file
    String fileName = (String) files.elementAt(0);
    SetOfPatterns setOfPatterns = new SetOfPatterns(fileName);
    int nspaceDimension = setOfPatterns.getSpaceDimension();
    //initialize values
    float[][] fdynamicRanges = setOfPatterns.getDynamicRange();
    for (int i = 1; i < nnumberOfFiles; i++) {
      fileName = (String) files.elementAt(i);
      setOfPatterns = new SetOfPatterns(fileName);
      float[][] fthisDynamicRanges = setOfPatterns.getDynamicRange();
      //update
      for (int j = 0; j < nspaceDimension; j++) {
        if (fthisDynamicRanges[j][0] < fdynamicRanges[j][0]) {
          fdynamicRanges[j][0] = fthisDynamicRanges[j][0];
        }
        if (fthisDynamicRanges[j][1] > fdynamicRanges[j][1]) {
          fdynamicRanges[j][1] = fthisDynamicRanges[j][1];
        }
      }
    }
    //write file
    StringBuffer stringBuffer = new StringBuffer();
    for (int j = 0; j < nspaceDimension; j++) {
      stringBuffer.append(j + " " + fdynamicRanges[j][0] + " " +
                          fdynamicRanges[j][1] + IO.m_NEW_LINE);
    }
    fileName = FileNamesAndDirectories.concatenateTwoPaths(rootDirectory,
        m_RANGES_FILENAME);
    IO.writeStringToFile(fileName, stringBuffer.toString());
  }

  public String getLabels() {
    return m_labels;
  }

  public void addSet(SetOfPatterns newSet) {
    if (!m_patternGenerator.equals(newSet.getPatternGenerator())) {
      End.throwError("Different PatternGenerator's !");
    }
    int n = newSet.getNumberOfPatterns();
    for (int i = 0; i < n; i++) {
      addPattern(newSet.getPattern(i));
    }
  }

  public void printContents() {
    for (int i = 0; i < getNumberOfPatterns(); i++) {
      getPattern(i).printContents();
    }
  }

  public String toString() {
    return "SetOfPatterns.nmaximumParameterValue=" + m_fmaximumParameterValue +
        IO.m_NEW_LINE + "SetOfPatterns.nminimumParameterValue=" +
        m_fminimumParameterValue +
        IO.m_NEW_LINE + "SetOfPatterns.nmaximumNumberOfFrames=" +
        m_nmaximumNumberOfFrames +
        IO.m_NEW_LINE + "SetOfPatterns.nminimumNumberOfFrames=" +
        m_nminimumNumberOfFrames +
        IO.m_NEW_LINE + "SetOfPatterns.daverageNumberOfFrames=" +
        m_daverageNumberOfFrames +
        IO.m_NEW_LINE + "SetOfPatterns.nnumberOfPatterns=" +
        this.getNumberOfPatterns() +
        IO.m_NEW_LINE + "SetOfPatterns.nspaceDimension=" +
        this.getSpaceDimension() +
        IO.m_NEW_LINE + "DataLocator.FileName = " + m_dataLocatorFileName +
        IO.m_NEW_LINE + "SetOfPatterns.Labels = " + m_labels +
        IO.m_NEW_LINE + m_patternGenerator.toString();
  }

  static public void main(String[] args) {
    if (args.length != 1) {
      Print.dialog("Usage: FEA file to be printed to stdout");
      System.exit(1);
    }
    SetOfPatterns setOfPatterns = new SetOfPatterns(args[0]);
    Print.dialog(setOfPatterns.toString());
    setOfPatterns.printContents();
  }

  /**
   * This class is supposed to help with identifying external data format and
   * simplify the acquisition.
   *
   * <p>Title: Spock</p>
   * <p>Description: Speech recognition</p>
   * <p>Copyright: Copyright (c) 2001</p>
   * <p>Company: UCSD</p>
   * @author Nikola Jevtic
   * @version 4.0
   */
  public static class ExternalFormatType extends SuperType {

    public static final ExternalFormatType ISIP_BINARY = new ExternalFormatType("ISIP_BINARY");
    public static final ExternalFormatType ISIP_ASCII = new ExternalFormatType("ISIP_ASCII");
    protected static ExternalFormatType[] m_types;

    //In case of adding a new Type (above), don't forget to add it below.
    static {
      m_types = new ExternalFormatType[2];
      m_types[0] = ISIP_BINARY;
      m_types[1] = ISIP_ASCII;
    }

    //notice the constructor is 'protected', not public.
    protected ExternalFormatType(String strName) {
      //m_strName is defined in superclass
      m_strName = strName;
    }

    /**Return true if the input String is equal
     * (case sensitive) to the String that represents
     * one of the defined ExternalFormatTypes.
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

    /**Return the index of m_types element that matches
     * the input String or -1 if there was no match.
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

    /**Return the ExternalFormatType correspondent to the given indentifier
     * String or null in case identifier is not valid.
     */
    public static final ExternalFormatType getType(String typeIdentifierString) {
      if (isValid(typeIdentifierString)) {
        int nindex = getTypeIndex(typeIdentifierString);
        return (ExternalFormatType) m_types[nindex];
      }
      else {
        return null;
      }
    }

    /**Return the ExternalFormatType correspondent to the given indentifier
     * String or exit in case identifier is not valid.
     */
    public static final ExternalFormatType getTypeAndExitOnError(
            String typeIdentifierString ) {
      if (isValid(typeIdentifierString)) {
        int nindex = getTypeIndex(typeIdentifierString);
        return (ExternalFormatType) m_types[nindex];
      } else {
        End.throwError(typeIdentifierString +
                       " is not a valid PatternGenerator type.");
        //make compiler happy:
        return null;
      }
    }

  }

}
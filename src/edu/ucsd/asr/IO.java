package edu.ucsd.asr;

import java.io.*;
import java.util.Vector;
import java.util.Enumeration;
//import java.util.Properties;
import java.text.NumberFormat;
import java.text.DecimalFormat;
import edu.ucsd.asr.TableOfLabels;
import java.util.StringTokenizer;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 *  General utilities for I/O.
 *
 *@author     Aldebaro Klautau
 *@created    October 16, 2000
 *@version    2 - August 31, 2000
 */
public final class IO {

  //private final static int m_nsleepTime = 200;
  /**
   *  Description of the Field
   */
  public final static String m_prefixForTemporaryFiles = "ucsdASR";
  public final static String m_suffixForTemporaryFiles = "tmp";
  public final static String m_NEW_LINE;
  public final static String m_SLASH;
  private static NumberFormat m_numberFormat;
  private static float m_fminimumNumber;
  private static float m_fmaximumNumber;
  private static DecimalFormat m_scientificNumberFormat;

  private static byte[] m_binputBuffer;

  static {
    Locale.setDefault(Locale.US);
    m_NEW_LINE = System.getProperties().getProperty("line.separator");
    m_SLASH = System.getProperties().getProperty("file.separator");
    //Print.dialog(System.getProperties().toString());
    //m_NEW_LINE = "\n";
    m_numberFormat = NumberFormat.getInstance(Locale.US);
    m_scientificNumberFormat = (DecimalFormat) NumberFormat.getInstance();
    setMaximumNumberWhichIsShownWithoutScientificNotation(999.9F);
    //below sets the minimum number
    setMaximumFractionDigits(3);
  }

  /**
   *  Sets the MaximumFractionDigits attribute of the IO class
   *
   *@param  nnewValue  The new MaximumFractionDigits value
   */
  public static void setMaximumFractionDigits(int nnewValue) {
    m_numberFormat.setMaximumFractionDigits(nnewValue);
    m_numberFormat.setMinimumFractionDigits(nnewValue);
    m_fminimumNumber = 1.0F / (float) Math.pow(10.0, nnewValue);
    //it doesn't need to be here:
    m_scientificNumberFormat.applyPattern("0.##E0");
    m_binputBuffer = new byte[16];
  }

  public static void setMaximumNumberWhichIsShownWithoutScientificNotation(float
          fvalue) {
    m_fmaximumNumber = fvalue;
  }

  /**
   *  Gets the PartOfVectorAsString attribute of the IO class
   *
   *@param  a                  Description of Parameter
   *@param  nmaximumDimension  Description of Parameter
   *@return                    The PartOfVectorAsString value
   */
  public static String getPartOfVectorAsString(float[] a, int nmaximumDimension) {
    String outputString = "";
    int m = a.length;
    if (m > nmaximumDimension) {
      m = nmaximumDimension;
    }
    NumberFormat numberFormat = NumberFormat.getInstance();
    DecimalFormat decimalFormat = (DecimalFormat) numberFormat;
    decimalFormat.applyPattern("0.000000E000");
    numberFormat.setMinimumIntegerDigits(1);
    numberFormat.setMaximumFractionDigits(6);
    numberFormat.setMinimumFractionDigits(6);

    for (int i = 0; i < m; i++) {
      String s = numberFormat.format(a[i]);
      //String s = Float.toString(a[i]);
      outputString = outputString.concat(" " + s);
    }
    //outputString = outputString.concat(m_NEW_LINE);
    return outputString;
  }

  /**
   *  Gets the PartOfMatrixAsString attribute of the IO class
   *
   *@param  a                  Description of Parameter
   *@param  nmaximumDimension  Description of Parameter
   *@return                    The PartOfMatrixAsString value
   */
  public static String getPartOfMatrixAsString(float[][] a,
                                               int nmaximumDimension) {
    String outputString = "";
    int m = a.length;
    int n = a[0].length;
    if (m > nmaximumDimension) {
      m = nmaximumDimension;
    }
    if (n > nmaximumDimension) {
      n = nmaximumDimension;
    }
    NumberFormat numberFormat = NumberFormat.getInstance();
    DecimalFormat decimalFormat = (DecimalFormat) numberFormat;
    decimalFormat.applyPattern("0.000000E000");
    numberFormat.setMinimumIntegerDigits(1);
    numberFormat.setMaximumFractionDigits(6);
    numberFormat.setMinimumFractionDigits(6);

    for (int i = 0; i < m; i++) {
      for (int j = 0; j < n; j++) {
        String s = numberFormat.format(a[i][j]);
        //String s = Float.toString(a[i][j]);
        //String s = decimalFormat.format(a[i][j]);
        //System.out.print("["+i+"]["+j+"]="+s+" ");
        outputString = outputString.concat(" " + s);
      }
      if (i != m - 1) {
        outputString = outputString.concat(m_NEW_LINE);
      }
    }
    //outputString = outputString.concat(m_NEW_LINE);
    return outputString;
  }

  /**
   *  Description of the Method
   *
   *@param  fileName  Description of Parameter
   *@return           Description of the Returned Value
   */
  public static BufferedReader openBufferedReader(String fileName) {
    BufferedReader bufferedReader = null;
    try {
      bufferedReader = new BufferedReader(new FileReader(fileName));
    }
    catch (Exception ex) {
      ex.printStackTrace();
      Print.error("Problem opening file " + fileName);
      End.exit();
    }
    return bufferedReader;
  }

  public static String readLine(BufferedReader bufferedReader) {
    try {
      String line = bufferedReader.readLine();
      return line;
    }
    catch (IOException e) {
      e.printStackTrace();
      Print.error("error reading from buffered reader");
    }
    return null;
  }

  public static void closeBufferedReader(BufferedReader bufferedReader) {
    try {
      bufferedReader.close();
    }
    catch (IOException e) {
      e.printStackTrace();
      Print.error("Erro closing the buffered reader.");
      End.exit(0);
    }
  }

  public static BufferedWriter openBufferedWriter(String fileName) {
    BufferedWriter bufferedWriter = null;
    try {
      bufferedWriter = new BufferedWriter(new FileWriter(fileName));
    }
    catch (Exception ex) {
      ex.printStackTrace();
      Print.error("Problem opening file " + fileName);
      End.exit();
    }
    return bufferedWriter;
  }

  public static void closeBufferedWriter(BufferedWriter bufferedWriter) {
    try {
      bufferedWriter.close();
    }
    catch (Exception ex) {
      ex.printStackTrace();
      Print.error("Problem closing file");
      End.exit();
    }
  }

  public static DataOutputStream openFileDataOutputStream(String filename) {
    DataOutputStream dataOutputStream = null;
    try {
      dataOutputStream = new DataOutputStream(new FileOutputStream(new File(
              filename)));
    }
    catch (IOException e) {
      End.throwError("Could not open file " + filename);
    }
    return dataOutputStream;
  }

  public static void closeDataOutputStream(DataOutputStream dataOutputStream) {
    try {
      dataOutputStream.close();
    }
    catch (IOException e) {
      End.throwError("Could not close DataOutputStream !");
    }
  }

  public static DataInputStream openFileDataInputStream(String filename) {
    DataInputStream din = null;
    try {
      File file = new File(filename);

      if (!file.exists() || !file.isFile()) {
        System.out.println("No such file " + file);
        System.exit(0);
      }

      if (!file.canRead()) {
        System.out.println("Can't read " + file);
        System.exit(0);
      }

      din = new DataInputStream(
              new BufferedInputStream(new FileInputStream(file)));
    }
    catch (IOException e) {
      e.printStackTrace();
      End.throwError("Could not open file " + filename);
    }
    return din;
  }

  public static boolean isEmpty(DataInputStream din) {
    int n = 0;
    try {
      n = din.available();
      //Print.dialog("available() returned " + n);
    }
    catch (IOException e) {
      End.throwError("Error accessing available() method in DataInputStream");
    }
    if (n == 0) {
      return true;
    }
    else {
      return false;
    }
  }

  public static void closeDataInputStream(DataInputStream din) {
    try {
      din.close();
    }
    catch (IOException e) {
      End.throwError("Could not close DataInputStream !");
    }
  }

  public static int readLittleEndianInteger(DataInputStream din) {
    int number = 0;
    try {
      number = din.readInt();
      number = IO.swapBytes(number);
    }
    catch (IOException e) {
      e.printStackTrace();
      End.throwError("Could not read int from DataInputStream");
    }
    return number;
  }

  public static float readLittleEndianFloat(DataInputStream din) {
    float number = 0.0f;
    try {
      number = din.readFloat();
      number = IO.swapBytes(number);
    }
    catch (IOException e) {
      e.printStackTrace();
      End.throwError("Could not read int from DataInputStream");
    }
    return number;
  }

  public static float[] readLittleEndianFloatVector(DataInputStream din, int size) {
    float[] vector = new float[size];
    try {
      for (int i = 0; i < size; i++) {
        vector[i] = din.readFloat();
      }
      vector = IO.swapBytes(vector);
    }
    catch (IOException e) {
      e.printStackTrace();
      End.throwError("Could not read int from DataInputStream");
    }
    return vector;
  }

  public static double[] readLittleEndianDoubleVector(DataInputStream din, int size) {
    double[] vector = new double[size];
    try {
      for (int i = 0; i < size; i++) {
        vector[i] = din.readDouble();
      }
      vector = IO.swapBytes( vector );
    }
    catch (IOException e) {
      e.printStackTrace();
      End.throwError("Could not read int from DataInputStream");
    }
    return vector;
  }

  public static void writeLineToWriter(BufferedWriter bufferedWriter,
                                       String line) {
    try {
      bufferedWriter.write(line);
      bufferedWriter.newLine();
    }
    catch (Exception ex) {
      ex.printStackTrace();
      Print.error("Problem writing to file");
      End.exit();
    }
  }

  /**
   *  Description of the Method
   *
   *@param  x  Description of Parameter
   *@return    Description of the Returned Value
   */
  public static String format(double x) {
    if (Math.abs(x) > m_fmaximumNumber ||
        (Math.abs(x) < m_fminimumNumber && x != 0.0)) {
      return m_scientificNumberFormat.format(x);
    }
    else {
      return m_numberFormat.format(x);
    }
  }

  public static String format(float x) {
    return format( (double) x);
  }

  public static String getBeginOfString(String longString,
                                        int nmaximumNumberOfCharacters) {
    if (longString.length() <= nmaximumNumberOfCharacters) {
      return longString;
    }
    else {
      return longString.substring(0, nmaximumNumberOfCharacters) + "...";
    }
  }

  public static String getEndOfString(String longString,
                                      int nmaximumNumberOfCharacters) {
    if (longString.length() <= nmaximumNumberOfCharacters) {
      return longString;
    }
    else {
      return "..." + longString.substring(longString.length() -
                                          nmaximumNumberOfCharacters);
    }
  }

  /**
   *  Return -1 if file does not exist.
   *
   *@param  fileName  Description of Parameter
   *@return           Description of the Returned Value
   */
  public static int rewriteTextFileUsingSystemLineSeparator(String fileName) {
    if (! (new File(fileName)).exists()) {
      return -1;
    }
    int nnumberOfLinesRead = 0;
    try {
      BufferedReader bufferedReader = new BufferedReader(
              new FileReader(fileName));
      String s = null;
      Vector outputStrings = new Vector();
      while ( (s = bufferedReader.readLine()) != null) {
        outputStrings.addElement(s);
        nnumberOfLinesRead++;
      }
      bufferedReader.close();
      BufferedWriter bufferedWriter = new BufferedWriter(
              new FileWriter(fileName));
      for (int i = 0; i < outputStrings.size(); i++) {
        bufferedWriter.write( (String) outputStrings.elementAt(i));
        bufferedWriter.newLine();
      }
      bufferedWriter.close();
    }
    catch (IOException e) {
      e.printStackTrace();
      End.exit();
    }
    return nnumberOfLinesRead;
  }

  /**
   *  Description of the Method
   *
   *@param  nvalue  Description of Parameter
   */
  public static void showCounter(int nvalue) {
    int nnumberOfDigits = 6;
    //default
    showCounter(nvalue, nnumberOfDigits);
  }

  /**
   *  Description of the Method
   *
   *@param  nvalue           Description of Parameter
   *@param  nnumberOfDigits  Description of Parameter
   */
  public static void showCounter(int nvalue, int nnumberOfDigits) {
    DecimalFormat numberFormat = new DecimalFormat();
    numberFormat.setDecimalSeparatorAlwaysShown(false);
    numberFormat.setGroupingSize(nnumberOfDigits + 1);
    //numberFormat.setParseIntegerOnly(true);
    numberFormat.setMinimumIntegerDigits(nnumberOfDigits);
    numberFormat.setMaximumIntegerDigits(nnumberOfDigits);
    for (int i = 0; i < nnumberOfDigits; i++) {
      Print.putStringOnlyToConsole("\b");
    }
    Print.putStringOnlyToConsole(numberFormat.format(nvalue));
  }

  /**
   *  Description of the Method
   *
   *@param  filename1  Description of Parameter
   *@param  filename2  Description of Parameter
   *@return            Description of the Returned Value
   */
  public static boolean areFilesTheSame(String filename1, String filename2) {
    try {
      File file1 = new File(filename1);
      File file2 = new File(filename2);

      if (file1.length() != file2.length()) {
        return false;
      }
      int nlen = 512;
      byte[] buffer1 = new byte[nlen];
      byte[] buffer2 = new byte[nlen];
      FileInputStream fileinputstream1 = new FileInputStream(file1);
      DataInputStream datainputstream1 = new DataInputStream(fileinputstream1);

      FileInputStream fileinputstream2 = new FileInputStream(file2);
      DataInputStream datainputstream2 = new DataInputStream(fileinputstream2);

      int nb1 = 0;
      int nb2 = 0;
      do {
        nb1 = fileinputstream1.read(buffer1);
        nb2 = fileinputstream2.read(buffer2);
        //assume nb1 == nb2 because files have same size
        for (int i = 0; i < nb1; i++) {
          //System.out.println((buffer1[i] - buffer2[i]) + " " + buffer1[i] + " " + buffer2[i]);
          if (buffer1[i] != buffer2[i]) {
            return false;
          }
        }
      }
      while (nb1 == nlen && nb2 == nlen);

      fileinputstream1.close();
      fileinputstream2.close();

    }
    catch (IOException e) {
      e.printStackTrace();
    }
    return true;
  }

  /**
   *  Example: IO.runDOSCommand("command.com /C dir");
   *
   *@param  command to be passed to shell
   */
  public static void runDOSCommand(String command) {
    new RunExternalProgram(command);
  }

  /**
           *  Description of the Method Example: IO.runDOSCommand("command.com /C dir");
   *
   *@param  command         Description of Parameter
   *@param  outputFileName  Description of Parameter
   */
  public static void runDOSCommandAndSaveOutputToFile(String command,
          String outputFileName) {
    new RunExternalProgram(command, true, false, true, outputFileName,
                           false, false, true);
  }

  public static void runDOSCommandShowOutputToConsoleAndSaveToFile(
          String command, String outputFileName) {
    new RunExternalProgram(command, true, true, true, outputFileName,
                           false, false, true);
  }

  /**
   *  Description of the Method
   *
   *@param  line  Description of Parameter
   *@return       Description of the Returned Value
   */
  public static float[] parseLineOfFloats(String line) {
    StringTokenizer stringTokenizer = new StringTokenizer(line);
    float[] outputVector = new float[stringTokenizer.countTokens()];
    int j = 0;
    while (stringTokenizer.hasMoreTokens()) {
      String number = stringTokenizer.nextToken();
      outputVector[j] = Float.parseFloat(number);
      j++;
    }
    return outputVector;
  }

  /**
   *  Copies file in to a new file (overwrite) called out.
   *
   *@param  in   input file
   *@param  out  output file name
   */
  public static void copyFile(String in, String out) {
    try {
      FileInputStream filein = new FileInputStream(in);
      FileOutputStream fileout = new FileOutputStream(out);
      copyStreams(filein, fileout);
      filein.close();
      fileout.close();
    }
    catch (IOException e) {
      e.printStackTrace();
      Print.error("Problem copying file " + in + " to file " + out);
      End.exit();
    }
  }

  /**
   *  I/O book, pp 43.
   *
   *@param  in               Description of Parameter
   *@param  out              Description of Parameter
   *@exception  IOException  Description of Exception
   */
  public static void copyStreams(InputStream in, OutputStream out) throws
          IOException {
    //do not allow other threads to read from the input or write to
    //the output while copying
    synchronized (in) {
      synchronized (out) {
        byte[] buffer = new byte[256];
        while (true) {
          int nbytesRead = in.read(buffer);
          if (nbytesRead == -1) {
            break;
          }
          out.write(buffer, 0, nbytesRead);
        }
      }
    }
  }

  //obs: we have the field vector of a array, and it should have some way
  //to get the dimension of a matrix. Later...
  /**
   *  Description of the Method
   *
   *@param  filename  Description of Parameter
   *@param  dmatrix   Description of Parameter
   */
  public static void writeMatrixtoBinFile(String filename,
                                          double dmatrix[][]) {
    int nnumLines = dmatrix.length;
    int nnumColum = dmatrix[0].length;
    try {
      File file = new File(filename);
      //save matrix
      FileOutputStream fileOutputStream = new FileOutputStream(file);
      DataOutputStream dataOutputStream = new DataOutputStream(
              fileOutputStream);

      dataOutputStream.writeDouble( (double) nnumLines);
      dataOutputStream.writeDouble( (double) nnumColum);

      for (int i = 0; i < nnumLines; i++) {
        for (int j = 0; j < nnumColum; j++) {
          dataOutputStream.writeDouble(dmatrix[i][j]);
        }
      }
      dataOutputStream.close();
      fileOutputStream.close();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Assume rectangular matrices (all columns of same dimension, etc.)
   * @param filename
   * @param dmatrix
   */
  public static void write3DMatrixtoBinFile(String filename,
			double dmatrix[][][]) {
		int nnumLines = dmatrix.length;
		int nnumColum = dmatrix[0].length;
		int z = dmatrix[0][0].length;

		try {
			File file = new File(filename);
			// save matrix
			FileOutputStream fileOutputStream = new FileOutputStream(file);
			DataOutputStream dataOutputStream = new DataOutputStream(
					fileOutputStream);

			dataOutputStream.writeDouble((double) nnumLines);
			dataOutputStream.writeDouble((double) nnumColum);
			dataOutputStream.writeDouble((double) z);

			for (int i = 0; i < nnumLines; i++) {				
				for (int j = 0; j < nnumColum; j++) {
					for (int k = 0; k < z; k++) {
						dataOutputStream.writeDouble(dmatrix[i][j][k]);
					}
				}
			}
			dataOutputStream.close();
			fileOutputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
  
  /**
   *  Description of the Method
   *
   *@param  vector  Description of Parameter
   *@return         Description of the Returned Value
   */
  public static short[] swapBytes(short[] vector) {
    //swap bytes
    short[] swapedVector = new short[vector.length];
    int MSB;
    int LSB;
    for (int i = 0; i < vector.length; i++) {
      LSB = vector[i] & 255;
      MSB = (vector[i] >> 8) & 255;
      swapedVector[i] = (short) ( (LSB << 8) | MSB);
    }
    return swapedVector;
  }

  /**
   *  Description of the Method
   *
   *@param  vector  Description of Parameter
   *@return         Description of the Returned Value
   */
  public static float[] swapBytes(float[] vector) {
    //swap bytes
    float[] swapedVector = new float[vector.length];
    int nbyte1;
    int nbyte2;
    int nbyte3;
    int nbyte4;

    for (int i = 0; i < vector.length; i++) {
      int noriginalBits = Float.floatToRawIntBits(vector[i]);
      //loriginalBits = 1;
      nbyte4 = noriginalBits & 255;
      //LSB
      nbyte3 = (noriginalBits >> 8) & 255;
      nbyte2 = (noriginalBits >> 16) & 255;
      nbyte1 = (noriginalBits >> 24) & 255;
      //MSB
      int nswapedBits = ( (nbyte4 << 24) | (nbyte3 << 16) | (nbyte2 << 8) |
                         nbyte1);

      //System.out.print(noriginalBits + " " + nswapedBits + " ");
      //System.out.print(nbyte1 + " " + nbyte2 + " " + nbyte3 + " " + nbyte4 + " ");

      swapedVector[i] = Float.intBitsToFloat(nswapedBits);
    }
    return swapedVector;
  }

  /**
   * This method swaps bytes in the float variable; to be used for data read
   * in the little endian format.
   * @param fnumber
   * @return
   */
  public static float swapBytes(float fnumber) {
    int nbyte1;
    int nbyte2;
    int nbyte3;
    int nbyte4;

    int noriginalBits = Float.floatToRawIntBits(fnumber);
    nbyte4 = noriginalBits & 255;
    nbyte3 = (noriginalBits >> 8) & 255;
    nbyte2 = (noriginalBits >> 16) & 255;
    nbyte1 = (noriginalBits >> 24) & 255;
    int nswappedBits = ( (nbyte4 << 24) | (nbyte3 << 16) | (nbyte2 << 8) |
                        nbyte1);

    return Float.intBitsToFloat(nswappedBits);
  }

  /**
   * This method swaps bytes in the int variable; to be used for data read
   * in the little endian format.
   * @param fnumber
   * @return
   */
  public static int swapBytes(int nnumber) {
    int nbyte1;
    int nbyte2;
    int nbyte3;
    int nbyte4;

    nbyte4 = nnumber & 255;
    nbyte3 = (nnumber >> 8) & 255;
    nbyte2 = (nnumber >> 16) & 255;
    nbyte1 = (nnumber >> 24) & 255;
    int nswappedBits = ( (nbyte4 << 24) | (nbyte3 << 16) | (nbyte2 << 8) |
                        nbyte1);

    return nswappedBits;
  }

  public static float[][] swapBytes(float[][] matrix) {
    float[][] out = new float[matrix.length][];
    for (int i = 0; i < out.length; i++) {
      out[i] = swapBytes(matrix[i]);
    }
    return out;
  }

  /**
   *  Description of the Method
   *
   *@param  vector  Description of Parameter
   *@return         Description of the Returned Value
   */
  public static double[] swapBytes(double[] vector) {
    //could use a loop as below, but maybe it is faster dealing with all 8 bytes
    //double readDoubleLittleEndian( )
    //{
    //long accum = 0;
    //for ( int shiftBy = 0; shiftBy < 64; shiftBy+ =8 )
    //{
    // must cast to long or shift done modulo 32
    //accum |= ( (long)(readByte() & 0xff)) < < shiftBy;
    //}
    //return Double.longBitsToDouble (accum);
    //}
    //swap bytes
    double[] swapedVector = new double[vector.length];
    long lbyte1;
    long lbyte2;
    long lbyte3;
    long lbyte4;
    long lbyte5;
    long lbyte6;
    long lbyte7;
    long lbyte8;

    for (int i = 0; i < vector.length; i++) {
      long loriginalBits = Double.doubleToRawLongBits(vector[i]);
      lbyte8 = loriginalBits & 255L;
      //LSB
      lbyte7 = (loriginalBits >> 8) & 255L;
      lbyte6 = (loriginalBits >> 16) & 255L;
      lbyte5 = (loriginalBits >> 24) & 255L;
      lbyte4 = (loriginalBits >> 32) & 255L;
      lbyte3 = (loriginalBits >> 40) & 255L;
      lbyte2 = (loriginalBits >> 48) & 255L;
      lbyte1 = (loriginalBits >> 56) & 255L;
      //MSB
      long lswapedBits = ( (lbyte8 << 56) | (lbyte7 << 48) | (lbyte6 << 40) |
                          (lbyte5 << 32) |
                          (lbyte4 << 24) | (lbyte3 << 16) | (lbyte2 << 8) |
                          lbyte1);

      //System.out.print(loriginalBits + " " + lswapedBits + " ");
      //System.out.print(lbyte1 + " " + lbyte5 + " " + lbyte7 + " " + lbyte8 + " ");
      swapedVector[i] = Double.longBitsToDouble(lswapedBits);
    }
    return swapedVector;
  }

  /**
   *  Description of the Method
   *
   *@param  fileName  Description of Parameter
   *@param  vector    Description of Parameter
   */
  public static void writeVectorToLittleEndianBinaryFile(String fileName,
          short[] vector) {
    writeVectorToBinaryFile(fileName, swapBytes(vector));
  }

  /**
   *  Description of the Method
   *
   *@param  fileName  Description of Parameter
   *@param  vector    Description of Parameter
   */
  public static void writeVectorToLittleEndianBinaryFile(String fileName,
          float[] vector) {
    writeVectorToBinaryFile(fileName, swapBytes(vector));
  }

  /**
   *  Description of the Method
   *
   *@param  fileName  Description of Parameter
   *@param  vector    Description of Parameter
   */
  public static void writeVectorToLittleEndianBinaryFile(String fileName,
          double[] vector) {
    writeVectorToBinaryFile(fileName, swapBytes(vector));
  }

  /**
   *  Description of the Method
   *
   *@param  fileName  Description of Parameter
   *@param  vector    Description of Parameter
   */
  public static void writeVectorToBinaryFile(String fileName,
                                             short[] vector) {
    try {
      File file = new File(fileName);
      FileOutputStream fileOutputStream = new FileOutputStream(file);
      DataOutputStream dataOutputStream = new DataOutputStream(fileOutputStream);
      for (int i = 0; i < vector.length; i++) {
        dataOutputStream.writeShort(vector[i]);
      }
      dataOutputStream.close();
      fileOutputStream.close();
    }
    catch (IOException e) {
      e.printStackTrace();
      Print.error("Problem writing file " + fileName);
      End.exit();
    }
  }

  public static void writeVectorDataOutputStream(
          DataOutputStream dataOutputStream, float[] vector) {
    try {
      for (int i = 0; i < vector.length; i++) {
        dataOutputStream.writeFloat(vector[i]);
      }
    }
    catch (IOException e) {
      e.printStackTrace();
      Print.error("Problem writing to DataOutputStream!");
      End.exit();
    }
  }

  /**
   *  Description of the Method
   *
   *@param  fileName  Description of Parameter
   *@param  vector    Description of Parameter
   */
  public static void writeVectorToBinaryFile(String fileName, float[] vector) {
    try {
      File file = new File(fileName);
      FileOutputStream fileOutputStream = new FileOutputStream(file);
      DataOutputStream dataOutputStream = new DataOutputStream(fileOutputStream);
      for (int i = 0; i < vector.length; i++) {
        dataOutputStream.writeFloat(vector[i]);
      }
      dataOutputStream.close();
      fileOutputStream.close();
    }
    catch (IOException e) {
      e.printStackTrace();
      Print.error("Problem writing file " + fileName);
      End.exit();
    }
  }

  /**
   *  Description of the Method
   *
   *@param  fileName  Description of Parameter
   *@param  vector    Description of Parameter
   */
  public static void writeVectorToBinaryFile(String fileName, double[] vector) {
    try {
      File file = new File(fileName);
      FileOutputStream fileOutputStream = new FileOutputStream(file);
      DataOutputStream dataOutputStream = new DataOutputStream(fileOutputStream);
      //IO.DisplayVector(vector);
      for (int i = 0; i < vector.length; i++) {
        dataOutputStream.writeDouble(vector[i]);
      }
      dataOutputStream.close();
      fileOutputStream.close();
    }
    catch (IOException e) {
      e.printStackTrace();
      Print.error("Problem writing file " + fileName);
      End.exit();
    }
  }

  /**
   *  Write a (possibly non-rectangular) matrix to an ASCII text file.
   *
   *@param  fileName  Description of Parameter
   *@param  matrix    Description of Parameter
   */
  public static void writeMatrixtoASCIIFile(String fileName, double[][] matrix) {
    if (matrix != null) {
      int numlin = matrix.length;
      try {
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(
                fileName));
        for (int i = 0; i < numlin; i++) {
          String line = "";
          int numcol = matrix[i].length;
          for (int j = 0; j < numcol - 1; j++) {
            line = line.concat(matrix[i][j] + " ");
          }
          //don't use space after last column because
          //Matlab (stupid) can't read such file
          line = line.concat(Double.toString(matrix[i][numcol - 1]));
          bufferedWriter.write(line);
          bufferedWriter.newLine();
        }
        bufferedWriter.close();
      }
      catch (IOException e) {
        e.printStackTrace();
      }
    }
    else {
      Print.warning(
              "IO.writeStringToFile(): There's no information to save to file " +
              fileName);
    }
  }

  /**
   *  Write a (possibly non-rectangular) matrix to an ASCII text file.
   *
   *@param  fileName  Description of Parameter
   *@param  matrix    Description of Parameter
   */
  public static void writeMatrixtoASCIIFile(String fileName, int[][] matrix) {
    if (matrix != null) {
      int numlin = matrix.length;
      try {
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(
                fileName));
        for (int i = 0; i < numlin; i++) {
          String line = "";
          int numcol = matrix[i].length;
          for (int j = 0; j < numcol - 1; j++) {
            line = line.concat(matrix[i][j] + " ");
          }
          //don't use space after last column because
          //Matlab (stupid) can't read such file
          line = line.concat(Integer.toString(matrix[i][numcol - 1]));
          bufferedWriter.write(line);
          bufferedWriter.newLine();
        }
        bufferedWriter.close();
      }
      catch (IOException e) {
        e.printStackTrace();
      }
    }
    else {
      Print.warning(
              "IO.writeStringToFile(): There's no information to save to file " +
              fileName);
    }
  }

  /**
   *  Write a (possibly non-rectangular) matrix to an ASCII text file.
   *
   *@param  fileName  Description of Parameter
   *@param  matrix    Description of Parameter
   */
  public static void writeMatrixtoASCIIFile(String fileName, float[][] matrix) {
    if (matrix != null) {
      int numlin = matrix.length;
      try {
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(
                fileName));
        for (int i = 0; i < numlin; i++) {
          String line = "";
          int numcol = matrix[i].length;
          for (int j = 0; j < numcol - 1; j++) {
            line = line.concat(matrix[i][j] + " ");
          }
          //don't use space after last column because
          //Matlab (stupid) can't read such file
          line = line.concat(Float.toString(matrix[i][numcol - 1]));
          bufferedWriter.write(line);
          bufferedWriter.newLine();
        }
        bufferedWriter.close();
      }
      catch (IOException e) {
        e.printStackTrace();
      }
    }
    else {
      Print.warning(
              "IO.writeStringToFile(): There's no information to save to file " +
              fileName);
    }
  }

  /**
   *  Description of the Method
   *
   *@param  filename  Description of Parameter
   *@param  nvector   Description of Parameter
   */
  public static void writeVectortoASCIIFile(String filename, double nvector[]) {
    try {
      File file = new File(filename);
      //save matrix
      FileOutputStream fileoutputstream = new FileOutputStream(file);
      DataOutputStream dataoutputstream = new DataOutputStream(fileoutputstream);

      for (int i = 0; i < nvector.length; i++) {
        dataoutputstream.writeBytes(Double.toString(nvector[i]) + "\r\n");
      }

      fileoutputstream.close();
    }

    catch (IOException e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  public static void writeVectortoASCIIFile(String filename, float nvector[]) {
    try {
      File file = new File(filename);
      //save matrix
      FileOutputStream fileoutputstream = new FileOutputStream(file);
      DataOutputStream dataoutputstream = new DataOutputStream(fileoutputstream);

      for (int i = 0; i < nvector.length; i++) {
        dataoutputstream.writeBytes(Float.toString(nvector[i]) + "\r\n");
      }

      fileoutputstream.close();
    }
    catch (IOException e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  /**
   *  Description of the Method
   *
   *@param  filename  Description of Parameter
   *@param  nvector   Description of Parameter
   */
  public static void writeVectortoASCIIFile(String filename, int nvector[]) {
    try {
      File file = new File(filename);
      //save matrix
      FileOutputStream fileoutputstream = new FileOutputStream(file);
      DataOutputStream dataoutputstream = new DataOutputStream(fileoutputstream);

      for (int i = 0; i < nvector.length; i++) {
        dataoutputstream.writeBytes(Integer.toString(nvector[i]) + "\r\n");
      }

      fileoutputstream.close();
    }
    catch (IOException e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  /**
   *  Description of the Method
   *
   *@param  filename  Description of Parameter
   *@param  vector    Description of Parameter
   */
  public static void writeVectortoASCIIFile(String filename, String[] vector) {
    try {
      File file = new File(filename);
      FileOutputStream fileoutputstream = new FileOutputStream(file);
      DataOutputStream dataoutputstream = new DataOutputStream(fileoutputstream);

      for (int i = 0; i < vector.length; i++) {
        dataoutputstream.writeBytes(vector[i] + m_NEW_LINE);
      }
      fileoutputstream.close();
    }
    catch (IOException e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  /**
   *  Description of the Method
   *
   *@param  fileName   Description of Parameter
   *@param  nvalues    Description of Parameter
   *@param  vector     Description of Parameter
   *@param  separator  Description of Parameter
   */
  public static void writeVectorstoASCIIFile(String fileName, int[] nvalues,
                                             String[] vector, String separator) {
    if (nvalues.length != vector.length) {
      End.throwError("Input vectors of different length");
    }
    try {
      BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(
              fileName));
      for (int i = 0; i < vector.length; i++) {
        bufferedWriter.write(nvalues[i] + separator + vector[i] + m_NEW_LINE);
      }
      bufferedWriter.close();
    }
    catch (IOException e) {
      End.throwError("Problem writing file " + fileName);
    }
  }

  /**
   *  Description of the Method
   *
   *@param  fileName   Description of Parameter
   *@param  nvalues    Description of Parameter
   *@param  vector     Description of Parameter
   *@param  separator  Description of Parameter
   */
  public static void writeVectorstoASCIIFile(String fileName, long[] nvalues,
                                             String[] vector, String separator) {
    if (nvalues.length != vector.length) {
      End.throwError("Input vectors of different length");
    }
    try {
      BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(
              fileName));
      for (int i = 0; i < vector.length; i++) {
        bufferedWriter.write(nvalues[i] + separator + vector[i] + m_NEW_LINE);
      }
      bufferedWriter.close();
    }
    catch (IOException e) {
      End.throwError("Problem writing file " + fileName);
    }
  }

  /**
   *  Description of the Method
   *
   *@param  OutputFile     Description of Parameter
   *@param  dOutputVector  Description of Parameter
   *@return                Description of the Returned Value
   */
  public static int writeVectortoFile(RandomAccessFile OutputFile,
                                      double dOutputVector[]) {
    try {
      for (int i = 0; i < dOutputVector.length; i++) {
        OutputFile.writeDouble(dOutputVector[i]);
      }
    }
    catch (EOFException e) {
      return -1;
      //end
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    return 0;
  }

  /**
   *  Write a String a text file.
   *
   *@param  filename                 Description of Parameter
   *@param  stringToBeWrittenInFile  Description of Parameter
   */
  public static void writeStringToFile(String filename,
                                       String stringToBeWrittenInFile) {
    if (stringToBeWrittenInFile != null) {
      try {
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(
                filename));
        bufferedWriter.write(stringToBeWrittenInFile);
        bufferedWriter.close();
      }
      catch (IOException e) {
        e.printStackTrace();
      }
    }
    else {
      System.out.println("WARNING: There's no information to save to file " +
                         filename);
    }
  }

  /**
   *  Write a vector containing Strings to a file.
   *
   *@param  filename         Description of Parameter
   *@param  vectorOfStrings  Description of Parameter
   */
  public static void writeVectorOfStringsToFile(String filename,
                                                Vector vectorOfStrings) {
    if (filename == null) {
      Print.error("IO.writeVectorOfStringsToFile(): filename = " +
                  filename + " is null");
      return;
    }
    if (vectorOfStrings != null || !vectorOfStrings.isEmpty()) {
      try {
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(
                filename));
        Enumeration enumeration = vectorOfStrings.elements();
        while (enumeration.hasMoreElements()) {
          String s = (String) enumeration.nextElement();
          if (s == null) {
            Print.warning("writeVectorOfStringsToFile skipped null string");
          }
          else if (s.trim().equals("")) {
            Print.warning("writeVectorOfStringsToFile skipped blank string");
          }
          else {
            bufferedWriter.write(s);
            bufferedWriter.newLine();
          }
        }
        bufferedWriter.close();
      }
      catch (IOException e) {
        e.printStackTrace();
      }
    }
    else {
      Print.warning("There's no information to save to file " + filename);
    }
  }

  /**
   *  Description of the Method
   *
   *@param  inputStream  Description of Parameter
   *@return              Description of the Returned Value
   */
  public static Vector readVectorOfStringsFromInputStream(InputStream
          inputStream) {
    Vector lines = new Vector();
    try {
      BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
              inputStream));
      String s = null;
      while ( (s = bufferedReader.readLine()) != null) {
        s = s.trim();
        if (!s.equals("")) {
          lines.addElement(s);
        }
      }
      bufferedReader.close();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    return lines;
  }

  /**
   *  Description of the Method
   *
   *@param  filename  Description of Parameter
   *@return           Description of the Returned Value
   */
  public static Vector readVectorOfStringsFromFile(String filename) {

    if (filename == null) {
      End.throwError("IO.readVectorOfStringsFromFile(): filename = " +
                     filename + " is null");
    }

    Vector vectorOfStrings = new Vector();
    try {
      BufferedReader bufferedReader = new BufferedReader(new FileReader(
              filename));
      String s = null;
      while ( (s = bufferedReader.readLine()) != null) {
        if (s.trim().equals("")) {
          Print.warning("IO.readVectorOfStringsFromFile skipped blank string");
        }
        else {
          vectorOfStrings.addElement(s);
        }
      }
      bufferedReader.close();
    }
    catch (IOException e) {
      e.printStackTrace();
      End.throwError("Problem reading file " + filename);
    }
    return vectorOfStrings;
  }

  public static Vector readVectorOfStringsFromFileIncludingBlank(String
          filename) throws Exception {

    if (filename == null) {
      throw new Exception("IO.readVectorOfStringsFromFile(): filename = " +
                          filename + " is null");
    }

    Vector vectorOfStrings = new Vector();
    try {
      BufferedReader bufferedReader = new BufferedReader(new FileReader(
              filename));
      String s = null;
      while ( (s = bufferedReader.readLine()) != null) {
        vectorOfStrings.addElement(s);
      }
      bufferedReader.close();
    }
    catch (IOException e) {
      throw new IOException("Problem reading file " + filename);
    }
    return vectorOfStrings;
  }

  public static String[] readArrayOfStringsFromFile(String fileName) {
    Vector v = readVectorOfStringsFromFile(fileName);
    if (v.size() < 1) {
      return null;
    }
    String[] out = new String[v.size()];
    for (int i = 0; i < out.length; i++) {
      out[i] = (String) v.elementAt(i);
    }
    return out;
  }

  public static String[] readArrayOfStringsFromFileIncludingBlank(String
          fileName) throws Exception {
    Vector v = readVectorOfStringsFromFileIncludingBlank(fileName);
    if (v.size() < 1) {
      return null;
    }
    String[] out = new String[v.size()];
    for (int i = 0; i < out.length; i++) {
      out[i] = (String) v.elementAt(i);
    }
    return out;
  }

  /**
           *  Assumes the file has a header, two floats, that gives the matrix dimension.
   *
   *@param  filename  Description of Parameter
   *@return           Description of the Returned Value
   */
  public static float[][] readFloatMatrixFromBinFile(String filename) {
    int nnumLines;
    int nnumColum;
    float[][] dmatrix = null;
    try {
      File file = new File(filename);

      FileInputStream fileInputStream = new FileInputStream(file);
      DataInputStream dataInputStream = new DataInputStream(fileInputStream);
      nnumLines = (int) dataInputStream.readFloat();
      nnumColum = (int) dataInputStream.readFloat();
      dmatrix = new float[nnumLines][nnumColum];
      for (int i = 0; i < nnumLines; i++) {
        for (int j = 0; j < nnumColum; j++) {
          dmatrix[i][j] = dataInputStream.readFloat();
        }
      }
      fileInputStream.close();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    return dmatrix;
  }

  /**
   *  Assumes the file has a header, 3 doubles, that gives the 3-d matrix
   *  dimension.
   *
   *@param  filename  Description of Parameter
   *@return           Description of the Returned Value
   */
  public static double[][][] read3DMatrixFromBinFile(String filename) {
    int nnumLines;
    int nnumColum;
    int z;
    double[][][] dmatrix = null;
    try {
      File file = new File(filename);

      FileInputStream fileInputStream = new FileInputStream(file);
      DataInputStream dataInputStream = new DataInputStream(fileInputStream);
      nnumLines = (int) dataInputStream.readDouble();
      nnumColum = (int) dataInputStream.readDouble();
      z = (int) dataInputStream.readDouble();
      dmatrix = new double[nnumLines][nnumColum][z];
      for (int i = 0; i < nnumLines; i++) {
        for (int j = 0; j < nnumColum; j++) {
        	for (int k = 0; k < z; k++) {
        		dmatrix[i][j][k] = dataInputStream.readDouble();
        	}
        }
      }
      fileInputStream.close();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    return dmatrix;
  }
  
  /**
   *  Assumes the file has a header, two doubles, that gives the matrix
   *  dimension.
   *
   *@param  filename  Description of Parameter
   *@return           Description of the Returned Value
   */
  public static double[][] readMatrixFromBinFile(String filename) {
    int nnumLines;
    int nnumColum;
    double[][] dmatrix = null;
    try {
      File file = new File(filename);

      FileInputStream fileInputStream = new FileInputStream(file);
      DataInputStream dataInputStream = new DataInputStream(fileInputStream);
      nnumLines = (int) dataInputStream.readDouble();
      nnumColum = (int) dataInputStream.readDouble();
      dmatrix = new double[nnumLines][nnumColum];
      for (int i = 0; i < nnumLines; i++) {
        for (int j = 0; j < nnumColum; j++) {
          dmatrix[i][j] = dataInputStream.readDouble();
        }
      }
      fileInputStream.close();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    return dmatrix;
  }

  /**
           *  Reads a matrix stored as a binary file giving that the number of columns is
   *  known. The columns can represent the space dimension for example, and the
   *  lines the number of frames of a given Pattern.
   *
   *@param  fileName          Description of Parameter
   *@param  nnumberOfColumns  Description of Parameter
   *@return                   Description of the Returned Value
   */
  public static double[][] readMatrixFromBinFile(String fileName,
                                                 int nnumberOfColumns) {
    double[] dvector = ReadFiletoDoubleVector(fileName);
    int nnumberOfLines = dvector.length / nnumberOfColumns;
    double dnumberOfLines = dvector.length / nnumberOfColumns;

    if (nnumberOfLines != dnumberOfLines) {
      System.out.println("ERROR: File " + fileName + " has size=" +
                         (dvector.length * 8) +
                         " that is not a multiple of " + nnumberOfColumns +
                         " !");
      System.exit(1);
    }

    double[][] dmatrix = new double[nnumberOfLines][nnumberOfColumns];
    for (int i = 0; i < dmatrix.length; i++) {
      for (int j = 0; j < dmatrix[0].length; j++) {
        dmatrix[i][j] = dvector[i * nnumberOfColumns + j];
      }
    }

    return dmatrix;
  }

  public static float[][] readFloatMatrixFromBinFile(String fileName,
          int nnumberOfColumns) {
    float[] dvector = readFiletoFloatVector(fileName);
    int nnumberOfLines = dvector.length / nnumberOfColumns;
    double dnumberOfLines = dvector.length / nnumberOfColumns;

    if (nnumberOfLines != dnumberOfLines) {
      End.throwError("File " + fileName + " has size=" + (dvector.length * 4) +
                     " that is not a multiple of " + nnumberOfColumns + " !");
      return null;
    }
    float[][] dmatrix = new float[nnumberOfLines][nnumberOfColumns];
    for (int i = 0; i < dmatrix.length; i++) {
      for (int j = 0; j < dmatrix[0].length; j++) {
        dmatrix[i][j] = dvector[i * nnumberOfColumns + j];
      }
    }
    return dmatrix;
  }

  /**
   *  Assumes the user knows the matrix dimension.
   *
   *@param  filename   Description of Parameter
   *@param  nnumLines  Description of Parameter
   *@param  nnumColum  Description of Parameter
   *@return            Description of the Returned Value
   */
  public static double[][] readMatrixFromBinFile(String filename, int nnumLines,
                                                 int nnumColum) {
    double[][] dmatrix = null;
    try {
      File file = new File(filename);

      FileInputStream fileInputStream = new FileInputStream(file);
      DataInputStream dataInputStream = new DataInputStream(fileInputStream);
      //nnumLines = (int) dataInputStream.readDouble();
      //nnumColum = (int) dataInputStream.readDouble();
      dmatrix = new double[nnumLines][nnumColum];
      for (int i = 0; i < nnumLines; i++) {
        for (int j = 0; j < nnumColum; j++) {
          dmatrix[i][j] = dataInputStream.readDouble();
        }
      }
      fileInputStream.close();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    return dmatrix;
  }

  /**
   *  Description of the Method
   *
   *@param  filename  Description of Parameter
   *@param  dMatrix   Description of Parameter
   *@param  numlin    Description of Parameter
   *@param  numcol    Description of Parameter
   */
  public static void ReadFiletoMatrix(String filename, double dMatrix[][],
                                      int numlin, int numcol) {
    try {
      File file = new File(filename);

      if ( (file.length() / 8) != numlin * numcol) {
        //double is 8 bytes
        System.out.println("Warning: file " + filename + " has size=" +
                           file.length() / 8 +
                           " different of matrix dimension=" + numlin * numcol +
                           ".");
      }

      if ( (file.length() / 8) < numlin * numcol) {
        //double is 8 bytes
        System.out.println("Error: file " + filename + " has size = " +
                           file.length() / 8 +
                           " smaller than matrix dimension = " +
                           numlin * numcol + ".");
        System.exit(0);
      }

      FileInputStream fileinputstream = new FileInputStream(file);
      DataInputStream datainputstream = new DataInputStream(fileinputstream);

      for (int i = 0; i < numlin; i++) {
        for (int j = 0; j < numcol; j++) {
          dMatrix[i][j] = datainputstream.readDouble();
        }
      }

      fileinputstream.close();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   *  Reads ASCII file. Each line is a number, like 13.2 -31.33 20 ...
   *
   *@param  fileName  Description of Parameter
   *@return           Description of the Returned Value
   */
  public static String readTextFiletoString(String fileName) {
    StringBuffer stringBuffer = new StringBuffer();
    try {
      BufferedReader bufferedReader = new BufferedReader(new FileReader(
              fileName));
      String s = null;
      while ( (s = bufferedReader.readLine()) != null) {
        stringBuffer.append(s + m_NEW_LINE);
      }
      bufferedReader.close();
    }
    catch (IOException e) {
      e.printStackTrace();
      End.exit("Problem opening file " + fileName);
    }
    return stringBuffer.toString();
  }

  /**
   *  Description of the Method
   *
   *@param  fileName  Description of Parameter
   *@return           Description of the Returned Value
   */
  public static double[] ReadFiletoDoubleVector(String fileName) {
    File file = new File(fileName);
    long nnumberOfDoubles = file.length() / 8;
    //double is 8 bytes
    double dnumberOfDoubles = file.length() / 8.0;

    if (nnumberOfDoubles != dnumberOfDoubles) {
      System.out.println("WARNING: File " + fileName + " has size=" +
                         file.length() +
                         " that is not a multiple of a double (8 bytes) !");
      System.out.println("Software will read only " + nnumberOfDoubles +
                         " double numbers.");
    }

    double[] dinputVector = new double[ (int) nnumberOfDoubles];

    FileInputStream fileinputstream = null;
    try {
      fileinputstream = new FileInputStream(file);
    }
    catch (FileNotFoundException e) {
      e.printStackTrace();
    }

    DataInputStream datainputstream = new DataInputStream(fileinputstream);

    try {
      for (int i = 0; i < dinputVector.length; i++) {
        dinputVector[i] = datainputstream.readDouble();
      }
    }
    catch (IOException e) {
      e.printStackTrace();
    }

    try {
      fileinputstream.close();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    return dinputVector;
  }

  /**
   *  Reads ASCII file. Each line is a number, like 13.2 -31.33 20 ...
   *
   *@param  fileName  Description of Parameter
   *@return           Description of the Returned Value
   */
  public static float[][] readTextFiletoFloatMatrix(String fileName) {
    float[][] outVector = null;
    try {
      BufferedReader bufferedReader = new BufferedReader(new FileReader(
              fileName));
      int nnumberOfLines = 0;
      String s = null;
      while ( (s = bufferedReader.readLine()) != null) {
        if (!s.trim().equals("")) {
          nnumberOfLines++;
        }
      }
      bufferedReader.close();
      if (nnumberOfLines == 0) {
        return null;
      }
      bufferedReader = new BufferedReader(new FileReader(fileName));
      outVector = new float[nnumberOfLines][];
      int i = 0;
      while ( (s = bufferedReader.readLine()) != null) {
        if (!s.trim().equals("")) {
          outVector[i] = parseLineOfFloats(s);
          i++;
        }
      }
      bufferedReader.close();
    }
    catch (IOException e) {
      e.printStackTrace();
      End.exit("Problem opening file " + fileName);
    }
    return outVector;
  }

  /**
   *  Reads ASCII file. Each line is a number, like 13.2 -31.33 20 ...
   *
   *@param  fileName  Description of Parameter
   *@return           Description of the Returned Value
   */
  public static float[] readTextFiletoFloatVector(String fileName) {
    float[] outVector = null;
    try {
      BufferedReader bufferedReader = new BufferedReader(new FileReader(
              fileName));
      int nnumberOfLines = 0;
      while (bufferedReader.readLine() != null) {
        nnumberOfLines++;
      }
      bufferedReader.close();
      if (nnumberOfLines == 0) {
        return null;
      }
      bufferedReader = new BufferedReader(new FileReader(fileName));
      outVector = new float[nnumberOfLines];
      for (int i = 0; i < outVector.length; i++) {
        outVector[i] = Float.parseFloat(bufferedReader.readLine());
      }
      bufferedReader.close();
    }
    catch (IOException e) {
      e.printStackTrace();
      End.exit("Problem opening file " + fileName);
    }
    return outVector;
  }

  /**
   *  Description of the Method
   *
   *@param  fileName  Description of Parameter
   *@return           Description of the Returned Value
   */
  public static short[] readTextFiletoShortVector(String fileName) {
    short[] outVector = null;
    try {
      BufferedReader bufferedReader = new BufferedReader(new FileReader(
              fileName));
      int nnumberOfLines = 0;
      while (bufferedReader.readLine() != null) {
        nnumberOfLines++;
      }
      bufferedReader.close();
      if (nnumberOfLines == 0) {
        return null;
      }
      bufferedReader = new BufferedReader(new FileReader(fileName));
      outVector = new short[nnumberOfLines];
      for (int i = 0; i < outVector.length; i++) {
        outVector[i] = Short.parseShort(bufferedReader.readLine());
      }
      bufferedReader.close();
    }
    catch (IOException e) {
      e.printStackTrace();
      End.exit("Problem opening file " + fileName);
    }
    return outVector;
  }

  /**
   *  Description of the Method
   *
   *@param  fileName  Description of Parameter
   *@return           Description of the Returned Value
   */
  public static float[] readFiletoFloatVector(String fileName) {
    File file = null;
    try {
      file = new File(fileName);
    }
    catch (Exception e) {
      End.throwError("Could not open file " + fileName);
      return null;
    }

    long nnumberOfFloats = file.length() / 4;
    //float is 4 bytes
    double dnumberOfFloats = file.length() / 4.0;

    if (nnumberOfFloats != dnumberOfFloats) {
      End.throwError("File " + fileName + " has size=" + file.length() +
                     " that is not a multiple of a float (4 bytes) !");
      //System.out.println("Software will read only " + nnumberOfFloats + " float numbers.");
    }

    float[] dinputVector = new float[ (int) nnumberOfFloats];

    FileInputStream fileinputstream = null;
    try {
      fileinputstream = new FileInputStream(file);
    }
    catch (FileNotFoundException e) {
      e.printStackTrace();
    }

    DataInputStream datainputstream = new DataInputStream(fileinputstream);

    try {
      for (int i = 0; i < dinputVector.length; i++) {
        dinputVector[i] = datainputstream.readFloat();
      }
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    try {
      fileinputstream.close();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    return dinputVector;
  }

  public static int[] readFiletoIntegerVector(String fileName) {
    File file = null;
    try {
      file = new File(fileName);
    }
    catch (Exception e) {
      End.throwError("Could not open file " + fileName);
      return null;
    }

    long nnumberOfFloats = file.length() / 4;
    //float is 4 bytes
    double dnumberOfFloats = file.length() / 4.0;

    if (nnumberOfFloats != dnumberOfFloats) {
      End.throwError("File " + fileName + " has size=" + file.length() +
                     " that is not a multiple of an integer (4 bytes) !");
      //System.out.println("Software will read only " + nnumberOfFloats + " float numbers.");
    }

    int[] dinputVector = new int[ (int) nnumberOfFloats];

    FileInputStream fileinputstream = null;
    try {
      fileinputstream = new FileInputStream(file);
    }
    catch (FileNotFoundException e) {
      e.printStackTrace();
    }

    DataInputStream datainputstream = new DataInputStream(fileinputstream);

    try {
      for (int i = 0; i < dinputVector.length; i++) {
        dinputVector[i] = datainputstream.readInt();
      }
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    try {
      fileinputstream.close();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    return dinputVector;
  }

  /**
   *  Description of the Method
   *
   *@param  filename  Description of Parameter
   *@param  dVector   Description of Parameter
   */
  public static void ReadFiletoVector(String filename, double dVector[]) {
    int nN = dVector.length;
    try {
      File file = new File(filename);

      if ( (file.length() / 8) != nN) {
        //double is 8 bytes
        System.out.println("Warning: file " + filename + " has size=" +
                           file.length() / 8 +
                           " different of vector dimension=" + nN + ".");
      }

      if ( (file.length() / 8) < nN) {
        //double is 8 bytes
        System.out.println("Error: file " + filename + " has size = " +
                           file.length() / 8 +
                           " smaller than vector dimension = " + nN + ".");
        System.exit(0);
      }

      FileInputStream fileinputstream = new FileInputStream(file);
      DataInputStream datainputstream = new DataInputStream(fileinputstream);

      for (int i = 0; i < nN; i++) {
        dVector[i] = datainputstream.readDouble();
      }

      fileinputstream.close();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   *  Description of the Method
   *
   *@param  filename  Description of Parameter
   *@param  fvector   Description of Parameter
   */
  public static void ReadFiletoVector(String filename, float fvector[]) {
    int nN = fvector.length;
    try {
      File file = new File(filename);

      if ( (file.length() / 4) != nN) {
        //float is 4 bytes
        System.out.println("Warning: file " + filename + " has size=" +
                           file.length() / 8 +
                           " different of vector dimension=" + nN + ".");
      }

      if ( (file.length() / 4) < nN) {
        //float is 4 bytes
        System.out.println("Error: file " + filename + " has size = " +
                           file.length() / 8 +
                           " smaller than vector dimension = " + nN + ".");
        System.exit(0);
      }

      FileInputStream fileinputstream = new FileInputStream(file);
      DataInputStream datainputstream = new DataInputStream(fileinputstream);

      for (int i = 0; i < nN; i++) {
        fvector[i] = datainputstream.readFloat();
      }

      fileinputstream.close();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   *  Description of the Method
   *
   *@param  filename  Description of Parameter
   *@param  nvector   Description of Parameter
   */
  public static void ReadFiletoVector(String filename, int nvector[]) {
    int nN = nvector.length;
    try {
      File file = new File(filename);

      if ( (file.length() / 4) != nN) {
        //int is 4 bytes
        System.out.println("Warning: file " + filename + " has size=" +
                           file.length() / 8 +
                           " different of vector dimension=" + nN + ".");
      }

      if ( (file.length() / 4) < nN) {
        //int is 4 bytes
        System.out.println("Error: file " + filename + " has size = " +
                           file.length() / 8 +
                           " smaller than vector dimension = " + nN + ".");
        System.exit(0);
      }

      FileInputStream fileinputstream = new FileInputStream(file);
      DataInputStream datainputstream = new DataInputStream(fileinputstream);

      for (int i = 0; i < nN; i++) {
        nvector[i] = datainputstream.readInt();
      }

      fileinputstream.close();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   *  Description of the Method
   *
   *@param  filename  Description of Parameter
   *@param  dVector   Description of Parameter
   */
  public static void ReadFiletoVector(String filename, short dVector[]) {
    int nN = dVector.length;
    try {
      File file = new File(filename);

      if ( (file.length() / 2) != nN) {
        //short is 2 bytes
        System.out.println("Warning: file " + filename + " has size=" +
                           file.length() / 8 +
                           " different of vector dimension=" + nN + ".");
      }

      if ( (file.length() / 2) < nN) {
        //short is 2 bytes
        System.out.println("Error: file " + filename + " has size = " +
                           file.length() / 8 +
                           " smaller than vector dimension = " + nN + ".");
        System.exit(0);
      }

      FileInputStream fileinputstream = new FileInputStream(file);
      DataInputStream datainputstream = new DataInputStream(fileinputstream);

      for (int i = 0; i < nN; i++) {
        dVector[i] = datainputstream.readShort();
      }

      fileinputstream.close();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   *  Description of the Method
   *
   *@param  TrainingSequenceFile  Description of Parameter
   *@param  dInputVector          Description of Parameter
   *@return                       Description of the Returned Value
   */
  public static int ReadVectorfromFile(RandomAccessFile TrainingSequenceFile,
                                       double dInputVector[]) {
    try {
      for (int i = 0; i < dInputVector.length; i++) {
        dInputVector[i] = TrainingSequenceFile.readDouble();
      }
    }
    catch (EOFException e) {
      return -1;
      //end
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    return 0;
  }

  /**
   *  Description of the Method
   *
   *@param  TrainingSequenceFile  Description of Parameter
   *@param  dInputVector          Description of Parameter
   *@return                       Description of the Returned Value
   */
  public static int ReadVectorfromFile(RandomAccessFile TrainingSequenceFile,
                                       short dInputVector[]) {
    try {
      for (int i = 0; i < dInputVector.length; i++) {
        dInputVector[i] = TrainingSequenceFile.readShort();
      }
    }
    catch (EOFException e) {
      return -1;
      //end
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    return 0;
  }

  /**
   *  Description of the Method
   *
   *@param  InputFile     Description of Parameter
   *@param  sInputVector  Description of Parameter
   *@return               Description of the Returned Value
   */
  public static int ReadVectorfromSwappedFile(RandomAccessFile InputFile,
                                              short sInputVector[]) {
    try {
      for (int i = 0; i < sInputVector.length; i++) {
        //short LSB = (short) InputFile.readByte();
        //short MSB = (short) InputFile.readByte();
        short LSB = (short) InputFile.readUnsignedByte();
        short MSB = (short) InputFile.readUnsignedByte();
        int z = 256 * MSB + LSB;
        sInputVector[i] = (short) z;
      }
    }
    catch (EOFException e) {
      return -1;
      //end
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    return 0;
  }

  /**
   *  Description of the Method
   *
   *@param  datainputstream  Description of Parameter
   *@param  dInputVector     Description of Parameter
   *@param  numelements      Description of Parameter
   *@return                  Description of the Returned Value
   */
  public static int ReadVectorfromFile(DataInputStream datainputstream,
                                       double dInputVector[], int numelements) {
    try {
      for (int i = 0; i < numelements; i++) {
        dInputVector[i] = datainputstream.readDouble();
      }
    }

    catch (EOFException e) {
      return -1;
    }

    catch (IOException e) {
      e.printStackTrace();
    }

    return 0;
  }

  /**
   *  Description of the Method
   *
   *@param  a  Description of Parameter
   */
  public static void DisplayMatrix(int[][] a) {
    int m = a.length;
    int n = a[0].length;

    for (int i = 0; i < m; i++) {
      System.out.print("\n");
      for (int j = 0; j < n; j++) {
        System.out.print("[" + i + "][" + j + "]=" + a[i][j] + " ");
      }
    }
    System.out.print("\n");
  }

  /**
   *  Description of the Method
   *
   *@param  a  Description of Parameter
   */
  public static void DisplayMatrix(short[][] a) {
    int m = a.length;
    int n = a[0].length;

    for (int i = 0; i < m; i++) {
      System.out.print("\n");
      for (int j = 0; j < n; j++) {
        System.out.print("[" + i + "][" + j + "]=" + a[i][j] + " ");
      }
    }
    System.out.print("\n");
  }

  /**
   *  Description of the Method
   *
   *@param  a  Description of Parameter
   */
  public static void DisplayMatrix(float[][] a) {
    int m = a.length;
    int n = a[0].length;

    for (int i = 0; i < m; i++) {
      System.out.print("\n");
      for (int j = 0; j < n; j++) {
        System.out.print("[" + i + "][" + j + "]=" + a[i][j] + " ");
      }
    }
    System.out.print("\n");
  }

  /**
   *  Description of the Method
   *
   *@param  a  Description of Parameter
   */
  public static void DisplayMatrix(double[][] a) {
    //NumberFormat numberFormat = NumberFormat.getInstance();
    //numberFormat.setMinimumIntegerDigits(3);
    //numberFormat.setMaximumFractionDigits(2);
    //numberFormat.setMinimumFractionDigits(2);
    int m = a.length;
    for (int i = 0; i < m; i++) {
      System.out.print("\n");
      int n = a[i].length;
      for (int j = 0; j < n; j++) {
        //String s = numberFormat.format(a[i][j]);
        //System.out.print("["+i+"]["+j+"]="+s+" ");
        System.out.print("[" + i + "][" + j + "]=" + format(a[i][j]) + " ");
      }
    }
    System.out.print("\n");
  }

  /**
   *  Description of the Method
   *
   *@param  a  Description of Parameter
   */
  public static void DisplayMatrix(double[][][] a) {
    int m = a.length;
    int n = a[0].length;
    int z = a[0][0].length;
    for (int i = 0; i < m; i++) {
      for (int j = 0; j < n; j++) {
        System.out.print("\n");
        for (int k = 0; k < z; k++) {
          System.out.print("[" + i + "][" + j + "][" + k + "]=" + a[i][j][k] +
                           " ");
        }
      }
    }
    System.out.print("\n");
  }

  /**
   *  Description of the Method
   *
   *@param  a  Description of Parameter
   */
  public static void DisplayMatrix(float[][][] a) {
    int m = a.length;
    int n = a[0].length;
    int z = a[0][0].length;
    for (int i = 0; i < m; i++) {
      for (int j = 0; j < n; j++) {
        System.out.print("\n");
        for (int k = 0; k < z; k++) {
          System.out.print("[" + i + "][" + j + "][" + k + "]=" + a[i][j][k] +
                           " ");
        }
      }
    }
    System.out.print("\n");
  }

  /**
   *  Description of the Method
   *
   *@param  a                  Description of Parameter
   *@param  nmaximumDimension  Description of Parameter
   */
  public static void displayPartOfMatrix(float[][] a, int nmaximumDimension) {
    int m = a.length;
    int n = a[0].length;
    if (m > nmaximumDimension) {
      m = nmaximumDimension;
    }
    if (n > nmaximumDimension) {
      n = nmaximumDimension;
    }
    NumberFormat numberFormat = NumberFormat.getInstance();
    numberFormat.setMinimumIntegerDigits(3);
    numberFormat.setMaximumFractionDigits(2);
    numberFormat.setMinimumFractionDigits(2);

    for (int i = 0; i < m; i++) {
      Print.putStringOnlyToConsole("\n");
      for (int j = 0; j < n; j++) {
        String s = numberFormat.format(a[i][j]);
        //System.out.print("["+i+"]["+j+"]="+s+" ");
        Print.putStringOnlyToConsole(s + "\t");
      }
    }
    System.out.print("\n");
  }

  /**
   *  Description of the Method
   *
   *@param  a                  Description of Parameter
   *@param  nmaximumDimension  Description of Parameter
   */
  public static void displayPartOfMatrix(double[][] a, int nmaximumDimension) {
    int m = a.length;
    int n = a[0].length;
    if (m > nmaximumDimension) {
      m = nmaximumDimension;
    }
    if (n > nmaximumDimension) {
      n = nmaximumDimension;
    }
    NumberFormat numberFormat = NumberFormat.getInstance();
    numberFormat.setMinimumIntegerDigits(3);
    numberFormat.setMaximumFractionDigits(4);
    numberFormat.setMinimumFractionDigits(4);

    for (int i = 0; i < m; i++) {
      Print.putStringOnlyToConsole("\n");
      for (int j = 0; j < n; j++) {
        String s = numberFormat.format(a[i][j]);
        //System.out.print("["+i+"]["+j+"]="+s+" ");
        Print.putStringOnlyToConsole(s + "\t");
      }
    }
    System.out.print("\n");
  }

  /**
   *  Description of the Method
   *
   *@param  a                  Description of Parameter
   *@param  nmaximumDimension  Description of Parameter
   */
  public static void displayPartOfMatrix(float[][][] a, int nmaximumDimension) {
    int m = a.length;
    int n = a[0].length;
    int z = a[0][0].length;
    if (m > nmaximumDimension) {
      m = nmaximumDimension;
    }
    if (n > nmaximumDimension) {
      n = nmaximumDimension;
    }
    if (z > nmaximumDimension) {
      z = nmaximumDimension;
    }
    NumberFormat numberFormat = NumberFormat.getInstance();
    numberFormat.setMinimumIntegerDigits(3);
    numberFormat.setMaximumFractionDigits(2);
    numberFormat.setMinimumFractionDigits(2);

    for (int i = 0; i < m; i++) {
      Print.putStringOnlyToConsole("\n");
      for (int j = 0; j < n; j++) {
        for (int k = 0; k < z; k++) {
          String s = numberFormat.format(a[i][j][k]);
          Print.putStringOnlyToConsole("[" + i + "][" + j + "][" + k + "]=" + s +
                                       " ");
        }
      }
    }
    Print.putStringOnlyToConsole("\n");
  }

  /**
   *  Description of the Method
   *
   *@param  a                  Description of Parameter
   *@param  nmaximumDimension  Description of Parameter
   */
  public static void displayPartOfMatrix(double[][][] a, int nmaximumDimension) {
    int m = a.length;
    int n = a[0].length;
    int z = a[0][0].length;
    if (m > nmaximumDimension) {
      m = nmaximumDimension;
    }
    if (n > nmaximumDimension) {
      n = nmaximumDimension;
    }
    if (z > nmaximumDimension) {
      z = nmaximumDimension;
    }

    NumberFormat numberFormat = NumberFormat.getInstance();
    numberFormat.setMinimumIntegerDigits(3);
    numberFormat.setMaximumFractionDigits(4);
    numberFormat.setMinimumFractionDigits(4);

    for (int i = 0; i < m; i++) {
      Print.putStringOnlyToConsole("\n");
      for (int j = 0; j < n; j++) {
        for (int k = 0; k < z; k++) {
          String s = numberFormat.format(a[i][j][k]);
          Print.putStringOnlyToConsole("[" + i + "][" + j + "][" + k + "]=" + s +
                                       " ");
        }
      }
    }
    Print.putStringOnlyToConsole("\n");
  }

  /**
   *  Description of the Method
   *
   *@param  a  Description of Parameter
   */
  public static void DisplayMatrixasInteger(double[][] a) {
    int m = a.length;
    for (int i = 0; i < m; i++) {
      System.out.print("\n");
      int n = a[i].length;
      for (int j = 0; j < n; j++) {
        System.out.print("[" + i + "][" + j + "]=" + ( (int) a[i][j]) + " ");
      }
    }
    System.out.print("\n");
  }

  /**
   *  Description of the Method
   *
   *@param  a  Description of Parameter
   */
  public static void DisplayEntriesofMatrixasInteger(double[][] a) {
    int m = a.length;
    for (int i = 0; i < m; i++) {
      System.out.print("\n");
      int n = a[i].length;
      for (int j = 0; j < n; j++) {
        System.out.print( ( (int) a[i][j]) + " ");
      }
    }
    System.out.print("\n");
  }

  /**
   *  Description of the Method
   *
   *@param  a  Description of Parameter
   */
  public static void DisplayVector(double[] a) {
    //NumberFormat numberFormat = NumberFormat.getInstance();
    //numberFormat.setMinimumIntegerDigits(3);
    //numberFormat.setMaximumFractionDigits(2);
    //numberFormat.setMinimumFractionDigits(2);
    int n = a.length;
    for (int i = 0; i < n; i++) {
      //String s = numberFormat.format(a[i]);
      //System.out.print("["+i+"]="+s+" ");
      System.out.print("[" + i + "]=" + a[i] + " ");
    }
    System.out.print("\n");
  }

  /**
   *  Description of the Method
   *
   *@param  a                Description of Parameter
   *@param  nfractionDigits  Description of Parameter
   */
  public static void DisplayVector(double[] a, int nfractionDigits) {
    //NumberFormat numberFormat = NumberFormat.getInstance();
    //numberFormat.setMinimumIntegerDigits(3);
    //numberFormat.setMaximumFractionDigits(nfractionDigits);
    //numberFormat.setMinimumFractionDigits(nfractionDigits);
    int n = a.length;
    for (int i = 0; i < n; i++) {
      //String s = numberFormat.format(a[i]);
      //System.out.print("["+i+"]="+s+" ");
      System.out.print("[" + i + "]=" + a[i] + " ");
    }
    System.out.print("\n");
  }

  /**
   *  Description of the Method
   *
   *@param  a  Description of Parameter
   */
  public static void displayVectorWithoutFormatting(double[] a) {
    for (int i = 0; i < a.length; i++) {
      System.out.print("[" + i + "]=" + a[i] + " ");
    }
    System.out.print("\n");
  }

  /**
   *  Description of the Method
   *
   *@param  a  Description of Parameter
   */
  public static void DisplayVector(short[] a) {
    //NumberFormat numberFormat = NumberFormat.getInstance();
    //numberFormat.setMinimumIntegerDigits(3);
    //numberFormat.setMaximumFractionDigits(2);
    //numberFormat.setMinimumFractionDigits(2);
    int n = a.length;
    for (int i = 0; i < n; i++) {
      //String s = numberFormat.format(a[i]);
      //System.out.print("["+i+"]="+s+" ");
      System.out.print("[" + i + "]=" + a[i] + " ");
    }
    System.out.print("\n");
  }

  /**
   *  Description of the Method
   *
   *@param  a  Description of Parameter
   */
  public static void DisplayVector(int[] a) {
    //NumberFormat numberFormat = NumberFormat.getInstance();
    //numberFormat.setMinimumIntegerDigits(3);
    //numberFormat.setMaximumFractionDigits(2);
    //numberFormat.setMinimumFractionDigits(2);
    int n = a.length;
    for (int i = 0; i < n; i++) {
      //String s = numberFormat.format(a[i]);
      //System.out.print("["+i+"]="+s+" ");
      System.out.print("[" + i + "]=" + a[i] + " ");
    }
    System.out.print("\n");
  }

  public static void DisplayVector(byte[] a) {
    //NumberFormat numberFormat = NumberFormat.getInstance();
    //numberFormat.setMinimumIntegerDigits(3);
    //numberFormat.setMaximumFractionDigits(2);
    //numberFormat.setMinimumFractionDigits(2);
    int n = a.length;
    for (int i = 0; i < n; i++) {
      //String s = numberFormat.format(a[i]);
      //System.out.print("["+i+"]="+s+" ");
      System.out.print("[" + i + "]=" + a[i] + " ");
    }
    System.out.print("\n");
  }

  public static void DisplayVector(boolean[] a) {
    //NumberFormat numberFormat = NumberFormat.getInstance();
    //numberFormat.setMinimumIntegerDigits(3);
    //numberFormat.setMaximumFractionDigits(2);
    //numberFormat.setMinimumFractionDigits(2);
    int n = a.length;
    for (int i = 0; i < n; i++) {
      //String s = numberFormat.format(a[i]);
      //System.out.print("["+i+"]="+s+" ");
      System.out.print("[" + i + "]=" + a[i] + " ");
    }
    System.out.print("\n");
  }

  /**
   *  Description of the Method
   *
   *@param  a  Description of Parameter
   */
  public static void DisplayVector(float[] a) {
    //NumberFormat numberFormat = NumberFormat.getInstance();
    //numberFormat.setMinimumIntegerDigits(3);
    //numberFormat.setMaximumFractionDigits(2);
    //numberFormat.setMinimumFractionDigits(2);
    int n = a.length;
    for (int i = 0; i < n; i++) {
      //String s = numberFormat.format(a[i]);
      //System.out.print("["+i+"]="+s+" ");
      System.out.print("[" + i + "]=" + a[i] + " ");
    }
    System.out.print("\n");
  }

  /**
   *  Description of the Method
   *
   *@param  a  Description of Parameter
   */
  public static void DisplayVector(long[] a) {
    //NumberFormat numberFormat = NumberFormat.getInstance();
    //numberFormat.setMinimumIntegerDigits(3);
    //numberFormat.setMaximumFractionDigits(2);
    //numberFormat.setMinimumFractionDigits(2);
    int n = a.length;
    for (int i = 0; i < n; i++) {
      //String s = numberFormat.format(a[i]);
      //System.out.print("["+i+"]="+s+" ");
      System.out.print("[" + i + "]=" + a[i] + " ");
    }
    System.out.print("\n");
  }

  /**
   *  Description of the Method
   *
   *@param  a                  Description of Parameter
   *@param  nmaximumDimension  Description of Parameter
   */
  public static void displayPartOfVector(float[] a, int nmaximumDimension) {
    int m = a.length;
    if (m > nmaximumDimension) {
      m = nmaximumDimension;
    }
    //		NumberFormat numberFormat = NumberFormat.getInstance();
    //		numberFormat.setMinimumIntegerDigits(3);
    //		numberFormat.setMaximumFractionDigits(2);
    //		numberFormat.setMinimumFractionDigits(2);
    StringBuffer stringBuffer = new StringBuffer(format(a[0]));
    for (int i = 1; i < m; i++) {
      stringBuffer.append("\t");
      stringBuffer.append(format(a[i]));
      //String s = numberFormat.format(a[i]);
      //String s = format(a[i]);
      //Print.putString(s + "\t");
    }
    Print.dialogOnlyToConsole(stringBuffer.toString());
  }

  /**
   *  Description of the Method
   *
   *@param  a                  Description of Parameter
   *@param  nmaximumDimension  Description of Parameter
   */
  public static void displayPartOfVector(double[] a, int nmaximumDimension) {
    int m = a.length;
    if (m > nmaximumDimension) {
      m = nmaximumDimension;
    }
    //		NumberFormat numberFormat = NumberFormat.getInstance();
    //		numberFormat.setMinimumIntegerDigits(3);
    //		numberFormat.setMaximumFractionDigits(3);
    //		numberFormat.setMinimumFractionDigits(3);
    for (int i = 0; i < m; i++) {
      //String s = numberFormat.format(a[i]);
      String s = format(a[i]);
      Print.putStringOnlyToConsole(s + "\t");
    }
    System.out.print("\n");
  }

  /**
   *  Description of the Method
   *
   *@param  biggermatrix     Description of Parameter
   *@param  selectedentries  Description of Parameter
   *@return                  Description of the Returned Value
   */
  public static double[][] CreateNewMatrixSelectingEntriesfromBiggerMatrix(double[][]
          biggermatrix, int[] selectedentries) {
    int m = selectedentries.length;
    double[][] newmatrix = new double[m][m];

    for (int i = 0; i < m; i++) {
      for (int j = 0; j < m; j++) {
        newmatrix[i][j] = biggermatrix[selectedentries[i]][selectedentries[j]];
      }
    }
    return newmatrix;
  }

  /**
   *  Description of the Method
   *
   *@param  a  Description of Parameter
   */
  public static void DisplayEntriesofMatrix(int[][] a) {
    int m = a.length;
    int n = a[0].length;

    for (int i = 0; i < m; i++) {
      System.out.print("\n");
      for (int j = 0; j < n; j++) {
        System.out.print(a[i][j] + " ");
      }
    }
    System.out.print("\n");
  }

  /**
   *  Description of the Method
   *
   *@param  a  Description of Parameter
   */
  public static void DisplayEntriesofMatrix(double[][] a) {
    int m = a.length;
    int n = a[0].length;

    for (int i = 0; i < m; i++) {
      System.out.print("\n");
      for (int j = 0; j < n; j++) {
        System.out.print(a[i][j] + " ");
      }
    }
    System.out.print("\n");
  }

  /**
   *  Reads a file, swaps its bytes and write to a new file. Java always assume
   *  big-endian, and one can not exchange files written by an application in a
   *  PC environment (C, etc.) to Java, without swapping the bytes.
   *  BytesperSample can be 2, 4 or 8 (short, int & float or long & double)
   *
   *@param  inputFilename    Description of Parameter
   *@param  outputFilename   Description of Parameter
   *@param  nBytesperSample  Description of Parameter
   *@author                  Aldebaro. 03/25/99.
   */
  public static void writeFilesSwappingBytes(String inputFilename,
                                             String outputFilename,
                                             int nBytesperSample) {
    File filein;
    File fileout;
    FileInputStream fin;
    FileOutputStream fout;

    filein = new File(inputFilename);
    if (!filein.exists() || !filein.isFile()) {
      System.out.println("No such file " + filein);
      System.exit(0);
    }

    if (!filein.canRead()) {
      System.out.println("Can't read " + filein);
      System.exit(0);
    }

    fileout = new File(outputFilename);

    if (fileout.exists()) {
      System.out.println(fileout + " already exists ! I can't overwrite it.");
      System.exit(0);
    }

    try {
      fin = new FileInputStream(filein);
      fout = new FileOutputStream(fileout);

      DataInputStream din = new DataInputStream(fin);
      DataOutputStream dout = new DataOutputStream(fout);

      long lLength = filein.length();
      //System.out.println("File size is " + lLength + " bytes (" + nBytesperSample+"bytes/sample)");
      byte[] data = new byte[nBytesperSample];

      for (long i = 0; i < lLength; i += nBytesperSample) {
        //read bytes
        for (int j = 0; j < nBytesperSample; j++) {
          data[j] = din.readByte();
        }
        //write bytes
        for (int j = nBytesperSample - 1; j >= 0; j--) {
          dout.writeByte(data[j]);
        }

      }

      fin.close();
      fout.close();

      //System.out.println("Happy end !");
    }

    catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   *  Writes to an ASCII file using LaTEX format.
   *
   *@param  Table                 Description of Parameter
   *@param  nnumberOfOccurrences  Description of Parameter
   *@param  dpercentageError      Description of Parameter
   *@param  outputFileName        Description of Parameter
   *@param  tableOfLabels         Description of Parameter
   *@author                       Nikola Jevtic, Aldebaro Klautau
   */
  public static void writeToTeXFile(int[][] Table, int[] nnumberOfOccurrences,
                                    double[] dpercentageError,
                                    String outputFileName,
                                    TableOfLabels tableOfLabels) {

    int m_nPhonemNumber = Table.length;

    try {
      PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(
              outputFileName)));
      out.println("\\documentclass[10pt]{article}");
      out.println("%In case you are having problems to print the table");
      out.println(
              "%try 'landscape', disabling the above line and enabling the lines below:");
      out.println("%\\documentclass[10pt,landscape]{article}");
      out.println("%\\special{landscape}");
      out.println("%\\topmargin 0in");
      out.println("%\\headheight 0in");
      out.println("%\\headsep 0in");
      out.println("%\\textheight 9in");
      out.println("%\\textwidth 7.5in");
      out.println("%\\oddsidemargin -.5in");
      out.println("%\\evensidemargin -.5in");

      out.println("\\begin{document}");
      out.println("\\tiny");
      out.print("\\begin{tabular}{|@{}c@{}||");
      //adds 2 columns: the total number of phonemes and the percentage error for that phoneme
      for (int ph = 0; ph < m_nPhonemNumber + 2; ph++) {
        out.print("@{}c@{}|");
      }

      out.println("}");
      out.println("\\hline");

      out.println("\\multicolumn{" + Integer.toString(m_nPhonemNumber + 1 + 2) + "}{c}{Table: Classification of phonemes. Line indicates the correct phoneme and column indicates the recognized phoneme.} \\\\ \\hline \\hline");

      out.print("Phnm");
      for (int ph = 0; ph < m_nPhonemNumber; ph++) {
        out.print(" & " + tableOfLabels.getFirstLabel(ph));
      }
      out.print(" & occur. & error");
      out.println(" \\\\ \\hline");
      NumberFormat numberFormat = NumberFormat.getInstance();
      //numberFormat.setMinimumIntegerDigits(3);
      numberFormat.setMaximumFractionDigits(2);
      numberFormat.setMinimumFractionDigits(2);

      for (int ph = 0; ph < m_nPhonemNumber; ph++) {

        out.print(tableOfLabels.getFirstLabel(ph));
        for (int i = 0; i < m_nPhonemNumber; i++) {
          out.print(" & " + Integer.toString(Table[ph][i]));
          ///TODO nNumberofmodels[ph] ));
          //out.print(" & " + Double.toString( (double) 100*Table[ph][i])); ///TODO nNumberofmodels[ph] ));
        }
        out.print(" & " + Integer.toString(nnumberOfOccurrences[ph]));
        out.print(" & " + numberFormat.format(dpercentageError[ph]));
        out.println(" \\\\ \\hline");
      }
      out.println("\\end{tabular}");
      out.println();
      out.println("\\end{document}");
      out.close();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   *  Description of the Method
   *
   *@param  fileName   Description of Parameter
   *@param  textToAdd  Description of Parameter
   */
  public static void appendStringWithTimeDateToEndOfTextFile(String fileName,
          String textToAdd) {
    //to write log file
    try {
      //open a file and append (not overwrite) information
      FileWriter fileWriter = new FileWriter(fileName, true);
      SimpleDateFormat formatter = new SimpleDateFormat(
              "dd/MMMMM/yyyyy 'at' hh:mm:ss aaa");
      //SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
      fileWriter.write(m_NEW_LINE + "Date = " + formatter.format(new Date()) +
                       "." + m_NEW_LINE);
      fileWriter.write(textToAdd);
      fileWriter.close();
    }
    catch (IOException e) {
      e.printStackTrace();
      Print.error("Problem writing file " + fileName);
      End.exit();
    }
  }

  /**
   *  Description of the Method
   *
   *@param  fileName   Description of Parameter
   *@param  textToAdd  Description of Parameter
   */
  public static void appendStringToEndOfTextFile(String fileName,
                                                 String textToAdd) {
    //to write log file
    try {
      //open a file and append (not overwrite) information
      FileWriter fileWriter = new FileWriter(fileName, true);
      fileWriter.write(textToAdd);
      fileWriter.close();
    }
    catch (IOException e) {
      e.printStackTrace();
      Print.error("Problem writing file " + fileName);
      End.exit();
    }
  }

  /*
   * public static Properties readPropertyFile(String fileName) {
   * Properties properties = new Properties();
   * set up new properties object
   * try {
   * FileInputStream propFile = new FileInputStream(fileName);
   * properties.load(propFile);
   * } catch (Exception e) {
   * e.printStackTrace();
   * }
   * return properties;
   * }
   */

  /**
   * Read <ENTER> from keyboard (pause)
   */
  public static void pause() {
    Print.dialogWithoutNewLineOnlyToConsole("Press <ENTER>");
    pauseWithoutMessage();
  }

  /**
   * Read <ENTER> from keyboard (pause)
   */
  public static void pauseWithoutMessage() {
    try {
      //Print.dialog("" +System.in.read(m_binputBuffer));

      System.in.read(m_binputBuffer);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    //Print.dialogWithoutNewLineOnlyToConsole("\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b");
  }

  public static File createTemporaryFileAndDeleteOnExit() {
    File file = createTemporaryFile();
    file.deleteOnExit();
    return file;
  }

  public static File createTemporaryFile() {
    try {
      return File.createTempFile(m_prefixForTemporaryFiles,
                                 m_suffixForTemporaryFiles);
    }
    catch (Exception e) {
      e.printStackTrace();
      End.exit();
      return null;
    }
  }

  public static String getDateAndTime() {
    SimpleDateFormat formatter = new SimpleDateFormat(
            "MMMMM/dd/yyyyy'at'hh:mm:ssaaa");

    //SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
    //The At in At_Date is to try making this the first line in sorted output file
    return formatter.format(new Date());
  }

  /**In JAVA, a byte is always considered as signed when converted to another type. We must mask the sign bit to JAVA, cast to an integer and process the masked bit if needed. The following method implements this idea : public class UnsignedByte {
           public static void main (String args[]) {
          byte b1 = 127  // int 127;
          byte b2 = -128 // int 128;
          byte b3 = -1   // int 255;
   */
  public static int unsignedByteToInt(byte b) {
    return (int) b & 0xFF;
  }

}
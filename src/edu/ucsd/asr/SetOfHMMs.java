package edu.ucsd.asr;

import java.io.Serializable;

/**Organizes a set of HMMs, usually represented by a JAR file.
 * @author Aldebaro Klautau
 * @version 2 - May 28, 2000
 */
public abstract class SetOfHMMs implements Serializable {

  protected double m_dbestScore; //best score in last calculation
  protected int m_nbestModel; //best model in last calculation

  public Type m_type;
  protected PatternGenerator m_patternGenerator;
  protected TableOfLabels m_tableOfLabels;

  /**Name of the file that is inside each jar file representing
   * a SetOfHMMs and contains the properties that describe the set.
   */
  public static final String HEADER_PROPERTIES_FILE_NAME = "SetOfHMMsProperties.txt";

  public abstract void findBestModelAndItsScore(Pattern pattern);

  public abstract boolean areModelsOk();

  public PatternGenerator getPatternGenerator() {
    return m_patternGenerator;
  }

  public boolean hasAnyTriphone() {
    return m_tableOfLabels.hasAnyTriphone();
  }

  public TableOfLabels getTableOfLabels() {
    return m_tableOfLabels;
  }

  public double getBestScore() {
    return m_dbestScore;
  }

  public int getBestModel() {
    return m_nbestModel;
  }

  public String getLabelOfBestModel() {
	return m_tableOfLabels.getFirstLabel(m_nbestModel);
  }
  
  /**Inner class that represents the type of SetOfHMMs's.
   * It follows the same format of some Sun's classes, as for example
   * AudioFormat, that has an inner class Encoding that can be instantiated using:
   * AudioFormat.Encoding encoding = AudioFormat.Encoding.PCM_SIGNED;
   *
   * @author Aldebaro Klautau
   * * @version 2.0 - 03/06/00.
   */
  public static class Type implements Serializable {

    public static final Type DISCRETE = new Type("DISCRETE");
    public static final Type PLAIN_CONTINUOUS = new Type("PLAIN_CONTINUOUS");
    public static final Type SHARED_CONTINUOUS = new Type("SHARED_CONTINUOUS");
    public static final Type SEMICONTINUOUS = new Type("SEMICONTINUOUS"); //tied in HTK
    public static final Type BINARYCLASSIFIERS = new Type("BINARYCLASSIFIERS");

    private String m_strName;

    //notice the constructor is 'protected', not public.
    protected Type(String strName) {
      m_strName = strName;
    }

    public final boolean equals(Object obj) {
      return super.equals(obj);
    }

    public final int hashCode() {
      return super.hashCode();
    }

    public final String toString() {
      return m_strName;
    }
  }
} // end of class

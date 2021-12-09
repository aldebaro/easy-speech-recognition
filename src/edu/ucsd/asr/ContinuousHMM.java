package edu.ucsd.asr;

import java.util.StringTokenizer;
import java.io.Serializable;

/**
 *  Continuous hidden Markov models (HMM) using mixture of Gaussians. Notice
 *  that this software assumes that continuous HMMs are always composed by
 *  mixture of Gaussians. So, there is no support for another PDF (Laplacian,
 *  etc.).
 *
 *@author     Aldebaro Klautau
 *@created    October 16, 2000
 *@version    2.0 - October 02, 2000.
 */

public class ContinuousHMM extends HMM implements Cloneable, Serializable  {

  static final long serialVersionUID = 8725429213557081418L;

  /**
   * Used for separating runs in run length encoding of state sequences.
   */
  private static final String m_separator = "|";

  /**
   *  Based on training sequence data, it indicates the minimum number of frames
   *  of data used to train given HMM.
   */
  protected int m_nminimumNumberOfFrames = -1;

  /**
   *  Based on training sequence data, it indicates the maximum number of frames
   *  of data used to train given HMM.
   */
  protected int m_nmaximumNumberOfFrames = -1;

  /**
   *  Mixture of Gaussians PDF's.
   */
  protected MixtureOfGaussianPDFs[] m_mixturesOfGaussianPDFs;

  /**
   *  Stores the best state sequence calculated in most recent call to Viterbi
   *  algorithm method.
   */
  int[] m_nstateSequenceOfLastViterbi;


  /**
   *  Construct HMM from input arguments imposing the specified topology.
   *
   *@param  ftransitionMatrix       Description of Parameter
   *@param  mixturesOfGaussianPDFs  Description of Parameter
   *@param  hMMTopology             Description of Parameter
   */
  public ContinuousHMM(float[][] ftransitionMatrix,
      MixtureOfGaussianPDFs[] mixturesOfGaussianPDFs,
      HMM.Topology hMMTopology) {
    //call other constructor
    this(ftransitionMatrix, mixturesOfGaussianPDFs);
    //but impose given topology
    m_topology = hMMTopology;
  }

  /**
   * Construct a prototype given global mean and variance
   * with all states having the same PDF. Use 1 Gaussian
   * per state.
   */
   public ContinuousHMM(float[][] ftransitionMatrix,
   float[] fmean, float[] fvar) {
    TransitionMatrix.verifyIfSquareMatrixAndExitIfNot(ftransitionMatrix);

    int nnumberOfStates = ftransitionMatrix.length;
    //discount 2 non-emitting states
    m_mixturesOfGaussianPDFs = new MixtureOfGaussianPDFs[nnumberOfStates - 2];
    GaussianPDF gaussianPDF = new DiagonalCovarianceGaussianPDF(fmean,fvar);
    float[] fweights = {1.0F};
    GaussianPDF[] gaussianPDFs = new GaussianPDF[1];
    for (int i=0; i<nnumberOfStates-2; i++) {
      gaussianPDFs[0] = (GaussianPDF) gaussianPDF.clone();
      m_mixturesOfGaussianPDFs[i] = new MixtureOfGaussianPDFs(gaussianPDFs,fweights,true);
    }
    m_ftransitionMatrix = LogDomainCalculator.calculateLog(ftransitionMatrix);
    m_type = HMM.Type.CONTINUOUS;

    if (isTransitionMatrixDescribingALeftToRightNoSkipsTopology()) {
      m_topology = HMM.Topology.LEFTRIGHT_NO_SKIPS;
    }
    else {
      m_topology = HMM.Topology.DESCRIBED_BY_TRANSITION_MATRIX;
    }
   }


  /**
   *  Construct HMM from input arguments and find the topology from properties of
   *  transition matrix.
   *
   *@param  ftransitionMatrix       Description of Parameter
   *@param  mixturesOfGaussianPDFs  Description of Parameter
   */
  public ContinuousHMM(float[][] ftransitionMatrix,
      MixtureOfGaussianPDFs[] mixturesOfGaussianPDFs) {

    TransitionMatrix.verifyIfSquareMatrixAndExitIfNot(ftransitionMatrix);
    int nnumberOfStatesAccordingToLine = ftransitionMatrix.length;
    int nnumberOfEmittingStates = mixturesOfGaussianPDFs.length;
    //HMMs are assumed to have 2 non-emitting states (entry and exit)
    if ((nnumberOfStatesAccordingToLine - 2) != nnumberOfEmittingStates) {
      Print.error("ContinuousHMM constructor: nnumberOfStatesAccordingToLine of " +
          " transition matrix = " +
          nnumberOfStatesAccordingToLine + ", while nnumberOfEmittingStates = " +
          nnumberOfEmittingStates + ".");
      End.throwError();
    }

    m_ftransitionMatrix = LogDomainCalculator.calculateLog(ftransitionMatrix);

    //IO.DisplayMatrix(m_ftransitionMatrix);
    m_mixturesOfGaussianPDFs = mixturesOfGaussianPDFs;

    m_type = HMM.Type.CONTINUOUS;

    if (isTransitionMatrixDescribingALeftToRightNoSkipsTopology()) {
      m_topology = HMM.Topology.LEFTRIGHT_NO_SKIPS;
    }
    else {
      m_topology = HMM.Topology.DESCRIBED_BY_TRANSITION_MATRIX;
    }
  }


  /**
   *  Constructor that reads HMM from a file. The file name must have the
   *  mandatory extension.
   *
   *@param  fileName  Description of Parameter
   */
  public ContinuousHMM(String fileName) {
    FileNamesAndDirectories.checkExtensionWithCaseIgnoredAndExitOnError(fileName,
        m_FILE_EXTENSION);

    //register that this HMM is Continuous
    m_type = HMM.Type.CONTINUOUS;

    //read HMM file
    //TODO: why the below is not working now ?
    //HMMFile.read(fileName,this);
    ContinuousHMM continuousHMM = (ContinuousHMM) HMMFile.read(fileName);

    m_ftransitionMatrix = continuousHMM.m_ftransitionMatrix;
    m_mixturesOfGaussianPDFs = continuousHMM.m_mixturesOfGaussianPDFs;
    m_topology = continuousHMM.m_topology;
    m_type = continuousHMM.m_type;
    m_nmaximumNumberOfFrames = continuousHMM.m_nmaximumNumberOfFrames;
    m_nminimumNumberOfFrames = continuousHMM.m_nminimumNumberOfFrames;
  }


  /**
   *  Constructor that constructs a left-right no-skips diagonal covariance
   *  matrix HMM with random numbers.
   *
   *@param  nnumberOfStates               Description of Parameter
   *@param  nnumberOfGaussiansPerMixture  Description of Parameter
   *@param  nspaceDimension               Description of Parameter
   */
  public ContinuousHMM(int nnumberOfStates,
      int nnumberOfGaussiansPerMixture,
      int nspaceDimension) {
    m_type = HMM.Type.CONTINUOUS;
    m_topology = HMM.Topology.LEFTRIGHT_NO_SKIPS;

    initializeModelRandomly(nnumberOfStates,
        nnumberOfGaussiansPerMixture,
        nspaceDimension);
  }


  /**
   *  Subclasses can use this empty constructor.
   */
  protected ContinuousHMM() {
  }


  /**
   *  Set transition matrix based on an input matrix that is NOT in log domain.
   *  The user can construct a HMM with random numbers and left-right topology
   *  and then use this method to set the desired topology.
   *
   *@param  ftransitionMatrix  The new TransitionMatrix value
   */
  public void setTransitionMatrix(float[][] ftransitionMatrix) {
    m_ftransitionMatrix = LogDomainCalculator.calculateLog(ftransitionMatrix);
    if (!isTransitionMatrixOk()) {
      End.throwError("Lines of transition matrix does not sum up to 1.");
    }
    if (isTransitionMatrixDescribingALeftToRightNoSkipsTopology()) {
      m_topology = HMM.Topology.LEFTRIGHT_NO_SKIPS;
    }
    else {
      m_topology = HMM.Topology.DESCRIBED_BY_TRANSITION_MATRIX;
    }
  }


  /**
   *  Sets the MinimumNumberOfFrames attribute of the ContinuousHMM object
   *
   *@param  nminimumNumberOfFrames  The new MinimumNumberOfFrames value
   */
  public void setMinimumNumberOfFrames(int nminimumNumberOfFrames) {
    m_nminimumNumberOfFrames = nminimumNumberOfFrames;
  }


  /**
   *  Sets the MaximumNumberOfFrames attribute of the ContinuousHMM object
   *
   *@param  nmaximumNumberOfFrames  The new MaximumNumberOfFrames value
   */
  public void setMaximumNumberOfFrames(int nmaximumNumberOfFrames) {
    m_nmaximumNumberOfFrames = nmaximumNumberOfFrames;
  }


  /**
   *  Gets the SpaceDimension attribute of the ContinuousHMM object
   *
   *@return    The SpaceDimension value
   */
  public int getSpaceDimension() {
    if (m_mixturesOfGaussianPDFs == null) {
      return -1;
    }
    else {
      return m_mixturesOfGaussianPDFs[0].getSpaceDimension();
    }
  }


  /**
   *  Gets the MaximumNumberOfGaussiansPerMixture attribute of the ContinuousHMM
   *  object
   *
   *@return    The MaximumNumberOfGaussiansPerMixture value
   */
  public int getMaximumNumberOfGaussiansPerMixture() {
    if (m_mixturesOfGaussianPDFs == null) {
      return -1;
    }
    else {
      int nmax = -1;
      for (int i = 0; i < m_mixturesOfGaussianPDFs.length; i++) {
        int ncandidate = m_mixturesOfGaussianPDFs[i].getNumberOfGaussians();
        if (ncandidate > nmax) {
          nmax = ncandidate;
        }
      }
      return nmax;
    }
  }


  /**
   *  Convert log prob into prob and return transition matrix.
   *
   *@return    The TransitionMatrix value
   */
  public float[][] getTransitionMatrix() {
    if (m_ftransitionMatrix == null) {
      return null;
    }
    else {
      return LogDomainCalculator.calculateExp(m_ftransitionMatrix);
    }
  }

  public float[][] getTransitionMatrixInLogDomainReference() {
    return m_ftransitionMatrix;
  }

  public MixtureOfGaussianPDFs getGivenMixtureOfGaussianPDFsReference(int nmixtureNumber) {
    return m_mixturesOfGaussianPDFs[nmixtureNumber];
  }

  /**
   *  Get mixtures of Gaussian PDF's, one mixture per HMM state.
   *
   *@return    The MixturesOfGaussianPDFs value
   */
  public MixtureOfGaussianPDFs[] getMixturesOfGaussianPDFs() {
    if (m_mixturesOfGaussianPDFs == null) {
      return null;
    }
    int numberOfMixtures = m_mixturesOfGaussianPDFs.length;
    MixtureOfGaussianPDFs[]
        mixturesOfGaussianPDFs = new MixtureOfGaussianPDFs[numberOfMixtures];
    for (int i = 0; i < numberOfMixtures; i++) {
      mixturesOfGaussianPDFs[i] = (MixtureOfGaussianPDFs) m_mixturesOfGaussianPDFs[i].clone();
    }
    for (int i = 0; i < mixturesOfGaussianPDFs.length; i++) {
      try {
        mixturesOfGaussianPDFs[i].discardGaussianWithVerySmallWeights();
      } catch (ASRError e) {
        //do nothing
        //if (m_nverbose > 1) {
        //	Print.dialog(e.getMessage());
        //}
      }
    }
    return mixturesOfGaussianPDFs;
  }


  /**
   *  Get mixture of given state, where first state (called 1) and the last state
   *  are non-emitting, so valid nstateNumber goes from 2, 3, last-1.
   *
   *@param  nstateNumber  Description of Parameter
   *@return               The SpecificMixture value
   */
  public MixtureOfGaussianPDFs getSpecificMixture(int nstateNumber) {
    if (m_mixturesOfGaussianPDFs == null) {
      Print.error("ContinuousHMM.getSpecificMixture(): m_mixturesOfGaussianPDFs is null");
      return null;
    }
    //does not return mixture for 2 non-emitting states
    int nnumberOfStates = m_mixturesOfGaussianPDFs.length + 2;
    if (nstateNumber < 2 || nstateNumber > nnumberOfStates - 1) {
      End.throwError("ContinuousHMM.getSpecificMixture(): state " +
          nstateNumber + " is out of valid range = [2, " +
          (nnumberOfStates - 1) + "].");
      return null;
    }
    //Print.dialog(nstateNumber + "LL");
    return (MixtureOfGaussianPDFs) m_mixturesOfGaussianPDFs[nstateNumber - 2].clone();
  }


  /**
   *  Check if HMM model obeys sthocastic constraints (probabilities sum up to 1)
   *  and does not have invalid (NaN, Inf) numbers.
   *
   *@return    The ModelOk value
   */
  public boolean isModelOk() {

    //check transition matrix
    if (!isTransitionMatrixOk()) {
      Print.warning("Transition matrix is not ok.");
      return false;
    }

    //check mixtures
    for (int i = 0; i < m_mixturesOfGaussianPDFs.length; i++) {
      if (!m_mixturesOfGaussianPDFs[i].isMixtureOk()) {
        Print.error("Mixture # " + i + " has some problem.");
        return false;
      }
    }

    //last check
    if (m_topology == HMM.Topology.LEFTRIGHT_NO_SKIPS) {
      if (isTransitionMatrixDescribingALeftToRightNoSkipsTopology()) {
        return true;
      }
      else {
        Print.warning("HMM.Topology = LEFTRIGHT_NO_SKIPS, but HMM does not have such topology.");
        return false;
      }
    }
    else {
      //in this case there is no special property
      return true;
    }
  }

  /**
   * Last value is the total likelihood. It takes in account
   * the transition probabilities.
   */
  public float[] getScoreEvolution(Pattern pattern, int[] nstateAlignment) {
    float[][] fparameters = pattern.getParameters();
    int nnumberOfStates = getNumberOfStates();
    int nnumberOfFrames = fparameters.length;
    float[] fscores = new float[nnumberOfFrames];
    //first frame
    fscores[0] = m_ftransitionMatrix[0][nstateAlignment[0]];
    //subtract 1 from nstateAlignment because it counts first state as 1
    fscores[0] += m_mixturesOfGaussianPDFs[nstateAlignment[0] - 1].calculateLogProbability(fparameters[0],0);
    for (int i = 1; i < fscores.length; i++) {
      fscores[i] = m_ftransitionMatrix[nstateAlignment[i-1]][nstateAlignment[i]];
      //subtract 1 from nstateAlignment because it counts first state as 1
      fscores[i] += m_mixturesOfGaussianPDFs[nstateAlignment[i] - 1].calculateLogProbability(fparameters[i],i);
      //add
      fscores[i] += fscores[i-1];
    }
    //exit transition
    int nlastFrame = fscores.length - 1;
    fscores[nlastFrame] += m_ftransitionMatrix[nstateAlignment[nlastFrame]][m_ftransitionMatrix.length-1];
    return fscores;
  }

  public float[][] getScoreEvolutionPerDimension(Pattern pattern,
  int[] nstateAlignment) {
    float[][] fparameters = pattern.getParameters();
    int nnumberOfStates = getNumberOfStates();
    int nnumberOfFrames = fparameters.length;
    float[][] fscores = new float[nnumberOfFrames][fparameters[0].length];
    //first frame
    //subtract 1 from nstateAlignment because it counts first state as 1
    for (int i = 0; i < fscores.length; i++) {
      //subtract 1 from nstateAlignment because it counts first state as 1
      m_mixturesOfGaussianPDFs[nstateAlignment[i] - 1].calculateContributionPerDimension(fparameters[i],fscores[i]);
    }
    //IO.displayPartOfMatrix(fscores,10);
    return fscores;
  }

  /**
   *  Get score of this HMM model given a Pattern using the Viterbi algorithm.
   *  Implementation according to HTK.
   *
   *@param  pattern  Description of Parameter
   *@return          The ScoreUsingViterbi value
   */
  public float getScoreUsingViterbi(Pattern pattern) {
    float[][] nO = pattern.getParameters();
    int nnumberOfStates = getNumberOfStates();

    float[] flastProbabilities = new float[nnumberOfStates];
    float[] fcurrentProbabilities = new float[nnumberOfStates];

    int nbestPreviousState;
    float fbestLogProbability;
    float fpreviousLogProbability;
    float fcurrentLogProbability;
    float ftransitionLogProbability;

    int nT = nO.length;

    //matrix of previous state for backtracking
    short[][] straceBack = new short[nT][nnumberOfStates - 2];

    //Initialization, t = 0
    for (int i = 1; i < nnumberOfStates - 1; i++) {
      float ftransition = m_ftransitionMatrix[0][i];
      if (ftransition > LogDomainCalculator.m_fSMALL_NUMBER) {
        //i-1 because first state is non-emitting
        float flogProb = m_mixturesOfGaussianPDFs[i - 1].calculateLogProbability(nO[0],0);
        flastProbabilities[i] = ftransition + flogProb;
      }
      else {
        flastProbabilities[i] = LogDomainCalculator.m_fLOG_DOMAIN_ZERO;
      }
      //subtract 1: non-emitting state is not considered
      straceBack[0][i - 1] = 0;
    }
    //Print.dialog("flastProbabilities, t = 0");
    //IO.DisplayVector(flastProbabilities);

    //Recursion: t=1,...,nT-1 is the time counter
    for (int t = 1; t < nT; t++) {
      for (int ncurrentState = 1; ncurrentState < nnumberOfStates - 1; ncurrentState++) {
        //don't take in account exit (non-emitting state)
        //initialize with first emitting state # 1
        nbestPreviousState = 1;
        ftransitionLogProbability = m_ftransitionMatrix[1][ncurrentState];
        fpreviousLogProbability = flastProbabilities[1];
        if (ftransitionLogProbability > LogDomainCalculator.m_fSMALL_NUMBER) {
          fbestLogProbability = ftransitionLogProbability + fpreviousLogProbability;
        }
        else {
          fbestLogProbability = LogDomainCalculator.m_fLOG_DOMAIN_ZERO;
        }

        //now do the recursion for the other states (starting with # 2)
        for (int npreviousState = 2; npreviousState < nnumberOfStates - 1; npreviousState++) {
          ftransitionLogProbability = m_ftransitionMatrix[npreviousState][ncurrentState];
          fpreviousLogProbability = flastProbabilities[npreviousState];
          if (ftransitionLogProbability > LogDomainCalculator.m_fSMALL_NUMBER) {
            fcurrentLogProbability = ftransitionLogProbability + fpreviousLogProbability;
          }
          else {
            fcurrentLogProbability = LogDomainCalculator.m_fLOG_DOMAIN_ZERO;
          }
          if (fcurrentLogProbability > fbestLogProbability) {
            fbestLogProbability = fcurrentLogProbability;
            nbestPreviousState = npreviousState;
          }
        }

        //now, the best previous state was found, so add contribution
        //of output probability
        if (fbestLogProbability > LogDomainCalculator.m_fSMALL_NUMBER) {
          //don't calculate output probability if not necessary
          fcurrentLogProbability = m_mixturesOfGaussianPDFs[ncurrentState - 1].calculateLogProbability(nO[t],t);
          fcurrentProbabilities[ncurrentState] = fbestLogProbability + fcurrentLogProbability;
        }
        else {
          fcurrentLogProbability = LogDomainCalculator.m_fLOG_DOMAIN_ZERO;
          fcurrentProbabilities[ncurrentState] = LogDomainCalculator.m_fLOG_DOMAIN_ZERO;
        }
        //update matrix for back-tracking
        //subtract 1: non-emitting state is not considered
        straceBack[t][ncurrentState - 1] = (short) nbestPreviousState;
      }
      //update the vector of previous probabilities
      for (int i = 0; i < fcurrentProbabilities.length; i++) {
        flastProbabilities[i] = fcurrentProbabilities[i];
      }
      //Print.dialog("flastProbabilities, t = " + t);
      //IO.DisplayVector(flastProbabilities);
    }

    //Termination and backtracking - find the best path
    //take in account transition from time nT-1 (last) to last state (non-emitting)
    //initialize with first emitting state # 1
    nbestPreviousState = 1;
    ftransitionLogProbability = m_ftransitionMatrix[1][nnumberOfStates - 1];
    fpreviousLogProbability = flastProbabilities[1];
    if (ftransitionLogProbability > LogDomainCalculator.m_fSMALL_NUMBER) {
      fbestLogProbability = ftransitionLogProbability + fpreviousLogProbability;
    }
    else {
      fbestLogProbability = LogDomainCalculator.m_fLOG_DOMAIN_ZERO;
    }
    //now take in account other states, starting from #2
    for (int npreviousState = 2; npreviousState < nnumberOfStates - 1; npreviousState++) {
      ftransitionLogProbability = m_ftransitionMatrix[npreviousState][nnumberOfStates - 1];
      fpreviousLogProbability = flastProbabilities[npreviousState];
      if (ftransitionLogProbability > LogDomainCalculator.m_fSMALL_NUMBER) {
        fcurrentLogProbability = ftransitionLogProbability + fpreviousLogProbability;
      }
      else {
        fcurrentLogProbability = LogDomainCalculator.m_fLOG_DOMAIN_ZERO;
      }
      if (fcurrentLogProbability > fbestLogProbability) {
        fbestLogProbability = fcurrentLogProbability;
        nbestPreviousState = npreviousState;
      }
    }

    //TODO: this thing makes sense only on Viterbi + K-means.
    //Should put it there
    //if (fbestLogProbability < LogDomainCalculator.m_fSMALL_NUMBER) {
    //	Print.error("Viterbi: no path found. Score too small: " +
    //				fbestLogProbability);
    //	End.exit();
    //}
    //invite garbage collection but don't think it's necessary
    //IO.DisplayMatrix(straceBack);
    //Print.dialog("nbestPreviousState = " + nbestPreviousState);
    doTraceBack(straceBack,nbestPreviousState);
    straceBack = null;
    //Print.dialog("fbestLogProbability = " + fbestLogProbability);
    return fbestLogProbability / nT;
  }

  private void doTraceBack(short[][] straceBack, int thisState)	{
    int segLen = straceBack.length;
    m_nstateSequenceOfLastViterbi = new int[segLen];
    for (int segIdx=segLen-1; segIdx>=0; segIdx--) {
      m_nstateSequenceOfLastViterbi[segIdx] = thisState;
      //subtract because first state is non-emitting
      thisState--;
      thisState=straceBack[segIdx][thisState];
     }
     //IO.DisplayMatrix(straceBack);
     //IO.DisplayVector(m_nstateSequenceOfLastViterbi);
  }

  /**
   *  States numbered from 1...N, where 1 is the
   *  first emitting state.
   *
   *@return    The StateSequenceOfLastViterbi value
   */
  public int[] getStateSequenceOfLastViterbi() {
    return m_nstateSequenceOfLastViterbi;
  }

  /**
   *  States numbered from 1...N, where 1 is the
   *  first emitting state.
   *  Represented as state_i*runlength_i|state_j*runlength_j|...
   *  Example, sequence 1 2 3 3 would be
   *  1*1|2*1|3*2
   *
   *@return    The StateSequenceOfLastViterbi value
   */
  public static String getRunLengthRepresentation(int[] nstateSequence) {
    if (nstateSequence == null) {
      return null;
    }
    //can assume there are at least 2 states in m_nstateSequenceOfLastViterbi
    StringBuffer stringBuffer = new StringBuffer();
    int ncurrentState = nstateSequence[0];
    int nrunLength = 1;
    for (int i = 1; i < nstateSequence.length; i++) {
      if (nstateSequence[i] == ncurrentState) {
        nrunLength++;
      } else {
        //add it
        stringBuffer.append(ncurrentState + "*" + nrunLength + m_separator);
        ncurrentState = nstateSequence[i];
        nrunLength = 1;
      }
    }
    //take care of last one
    stringBuffer.append(ncurrentState + "*" + nrunLength);
    return stringBuffer.toString();
  }

  /**
   * Number of frames associated to each state.
   */
  public static int[] getDurationInEachState(String runLengthSequence) {
    if (runLengthSequence == null) {
      return null;
    }
    StringTokenizer stringTokenizer = new StringTokenizer(runLengthSequence, m_separator);
    int nnumberOfTokens = stringTokenizer.countTokens();
    String[] tokens = new String[nnumberOfTokens];
    int[] nstateIndices = new int[nnumberOfTokens];
    int[] nrunLengths = new int[nnumberOfTokens];
    for (int i = 0; i < nnumberOfTokens; i++) {
      tokens[i] = stringTokenizer.nextToken();
      //Print.dialog("=>" + tokens[i]);
      StringTokenizer otherStringTokenizer = new StringTokenizer(tokens[i], "*");
      if (otherStringTokenizer.countTokens() != 2) {
        End.throwError("Problem with token " + tokens[i] + " of sequence " + runLengthSequence);
      }
      nstateIndices[i] = Integer.parseInt(otherStringTokenizer.nextToken());
      nrunLengths[i] = Integer.parseInt(otherStringTokenizer.nextToken());
    }
    return nrunLengths;
  }

  public String getStateSequenceUsingRunLengthEncoding() {
    return getRunLengthRepresentation(m_nstateSequenceOfLastViterbi);
  }

  public static int[] interpretRunLengthEncodedStateSequence(String sequence) {
    StringTokenizer stringTokenizer = new StringTokenizer(sequence,m_separator);
    int nnumberOfTokens = stringTokenizer.countTokens();
    String[] tokens = new String[nnumberOfTokens];
    int[] nstateIndices = new int[nnumberOfTokens];
    int[] nrunLengths = new int[nnumberOfTokens];
    for (int i = 0; i < nnumberOfTokens; i++) {
      tokens[i] = stringTokenizer.nextToken();
      //Print.dialog("=>" + tokens[i]);
      StringTokenizer otherStringTokenizer = new StringTokenizer(tokens[i],"*");
      if (otherStringTokenizer.countTokens() != 2) {
        End.throwError("Problem with token " + tokens[i] + " of sequence " + sequence);
      }
      nstateIndices[i] = Integer.parseInt(otherStringTokenizer.nextToken());
      nrunLengths[i] = Integer.parseInt(otherStringTokenizer.nextToken());
    }
    int nnumberOfStates = 0;
    for (int i = 0; i < nnumberOfTokens; i++) {
      nnumberOfStates += nrunLengths[i];
    }
    //IO.DisplayVector(nrunLengths);
    //IO.DisplayVector(nstateIndices);
    //Print.dialog("=>" + tokens[i]);
    int[] nsequence = new int[nnumberOfStates];
    int k = 0;
    for (int i = 0; i < nnumberOfTokens; i++) {
      for (int j = 0; j < nrunLengths[i]; j++) {
        nsequence[k] = nstateIndices[i];
        k++;
      }
    }
    return nsequence;
  }

  /**
   *  Gets the TransitionMatrixDescribingALeftToRightNoSkipsTopology attribute of
   *  the ContinuousHMM object
   *
   *@return    The TransitionMatrixDescribingALeftToRightNoSkipsTopology value
   */
  public boolean isTransitionMatrixDescribingALeftToRightNoSkipsTopology() {

    //it helps checking the topology if it can be assumed that lines sum up to one
    if (!isTransitionMatrixOk()) {
      return false;
    }

    float[][] ftransitionMatrix = LogDomainCalculator.calculateExp(m_ftransitionMatrix);

    //check non-emitting states
    //check if initial state #0 can be followed only be state #1
    if (ftransitionMatrix[0][1] != 1.0F) {
      return false;
    }
    //check loop of final state (HTK ignores such value and writes 0 to all last line)
    if (ftransitionMatrix[m_ftransitionMatrix.length - 1][m_ftransitionMatrix.length - 1] != 1.0F) {
      return false;
    }
    //check emitting states
    for (int i = 1; i < m_ftransitionMatrix.length - 1; i++) {
      for (int j = 1; j < m_ftransitionMatrix[0].length; j++) {
        if ((j != i) &&
            (j != (i + 1))) {
          if (ftransitionMatrix[i][j] != 0.0F) {
            //Print.warning("ftransitionMatrix[" + i + "][" + j + "]=" + ftransitionMatrix[i][j] + " should be zero.");
            return false;
          }
        }

      }
    }
    return true;
  }

  /**
   *  Gets the MinimumNumberOfFrames attribute of the ContinuousHMM object
   *
   *@return    The MinimumNumberOfFrames value
   */
  public int getMinimumNumberOfFrames() {
    return m_nminimumNumberOfFrames;
  }

  /**
   *  Gets the MaximumNumberOfFrames attribute of the ContinuousHMM object
   *
   *@return    The MaximumNumberOfFrames value
   */
  public int getMaximumNumberOfFrames() {
    return m_nmaximumNumberOfFrames;
  }

  /**To interface with Nikola's software.
   */
  public void calculateSequenceProbability(float[][] nO,
                       float[] fPr,
                       int start) {
    int nT = fPr.length;
    if ( (nT+start) > nO.length ) {
      End.throwError("not good dimension of dPr");
    }

    int nnumberOfStates = getNumberOfStates();

    float[] flastProbabilities = new float[nnumberOfStates];
    float[] fcurrentProbabilities = new float[nnumberOfStates];

    int nbestPreviousState;
    float fbestLogProbability;
    float fpreviousLogProbability;
    float fcurrentLogProbability;
    float ftransitionLogProbability;

    //Initialization, t = 0
    for (int i=1; i<nnumberOfStates-1; i++) {
      float ftransition = m_ftransitionMatrix[0][i];
      if ( ftransition > LogDomainCalculator.m_fSMALL_NUMBER) {
        //i-1 because first state is non-emitting
        float flogProb = m_mixturesOfGaussianPDFs[i-1].calculateLogProbability(nO[start+0],0);
        flastProbabilities[i] = ftransition + flogProb;
      } else {
        flastProbabilities[i] = LogDomainCalculator.m_fLOG_DOMAIN_ZERO;
      }
    }

    //find greatest value
    fPr[0] = -Float.MAX_VALUE;
    for (int i=1; i<nnumberOfStates-1; i++) {
      if (flastProbabilities[i]+m_ftransitionMatrix[i][nnumberOfStates-1] > fPr[0]) {
        fPr[0] = flastProbabilities[i]+m_ftransitionMatrix[i][nnumberOfStates-1];
      }
    }

    //Recursion: t=1,...,nT-1 is the time counter
    for (int t=1; t<nT; t++) {
      for (int ncurrentState=1; ncurrentState<nnumberOfStates-1; ncurrentState++) {//don't take in account exit (non-emitting state)

        //initialize with first emitting state # 1
        nbestPreviousState = 1;
        ftransitionLogProbability = m_ftransitionMatrix[1][ncurrentState];
        fpreviousLogProbability = flastProbabilities[1];
        if (ftransitionLogProbability > LogDomainCalculator.m_fSMALL_NUMBER) {
          fbestLogProbability = ftransitionLogProbability + fpreviousLogProbability;
        } else {
          fbestLogProbability = LogDomainCalculator.m_fLOG_DOMAIN_ZERO;
        }

        //now do the recursion for the other states (starting with # 2)
        for (int npreviousState=2; npreviousState<nnumberOfStates-1; npreviousState++) {
          ftransitionLogProbability = m_ftransitionMatrix[npreviousState][ncurrentState];
          fpreviousLogProbability = flastProbabilities[npreviousState];
          if (ftransitionLogProbability > LogDomainCalculator.m_fSMALL_NUMBER) {
            fcurrentLogProbability = ftransitionLogProbability + fpreviousLogProbability;
          } else {
            fcurrentLogProbability = LogDomainCalculator.m_fLOG_DOMAIN_ZERO;
          }
          if (fcurrentLogProbability > fbestLogProbability) {
            fbestLogProbability = fcurrentLogProbability;
            nbestPreviousState = npreviousState;
          }
        }

        //now, the best previous state was found, so add contribution
        //of output probability
        if (fbestLogProbability > LogDomainCalculator.m_fSMALL_NUMBER) {
          //don't calculate output probability if not necessary
          fcurrentLogProbability = m_mixturesOfGaussianPDFs[ncurrentState-1].calculateLogProbability(nO[t],t);
          fcurrentProbabilities[ncurrentState] = fbestLogProbability + fcurrentLogProbability;
        } else {
          fcurrentLogProbability = LogDomainCalculator.m_fLOG_DOMAIN_ZERO;
          fcurrentProbabilities[ncurrentState] = LogDomainCalculator.m_fLOG_DOMAIN_ZERO;
        }
      }
      //update the vector of previous probabilities
      for (int i=0; i<fcurrentProbabilities.length; i++) {
        flastProbabilities[i] = fcurrentProbabilities[i];
      }
      //find greatest value
      fPr[t] = -Float.MAX_VALUE;
      for (int i=1; i<nnumberOfStates-1; i++) {
        if (fcurrentProbabilities[i]+m_ftransitionMatrix[i][nnumberOfStates-1] > fPr[t]) {
          fPr[t] = fcurrentProbabilities[i]+m_ftransitionMatrix[i][nnumberOfStates-1];
        }
      }
    }
  }

  /**
   *  Description of the Method
   *
   *@return    Description of the Returned Value
   */
  public String toString() {

    String outputString = "";
    outputString = outputString.concat("Transition matrix:" + IO.m_NEW_LINE);
    outputString = outputString.concat(IO.getPartOfMatrixAsString(m_ftransitionMatrix, 10));
    for (int i = 0; i < m_mixturesOfGaussianPDFs.length; i++) {
      int nstateNumber = i + 2;
      outputString = outputString.concat(IO.m_NEW_LINE + "State # = " + nstateNumber + IO.m_NEW_LINE);
      outputString = outputString.concat(m_mixturesOfGaussianPDFs[i].toString());
    }
    return outputString;
  }


  /**
   *  Description of the Method
   *
   *@param  hmmName               Description of Parameter
   *@param  patternGeneratorType  Description of Parameter
   *@return                       Description of the Returned Value
   */
  public String toStringAsInHTK(String hmmName, String patternGeneratorType) {
    int nnumberOfStates = m_mixturesOfGaussianPDFs.length + 2;
    //supports only 1 stream
    String outputString = "~o" + IO.m_NEW_LINE + "<STREAMINFO> 1 " + getSpaceDimension() + IO.m_NEW_LINE;
    outputString = outputString.concat("<VECSIZE> " + getSpaceDimension() + " <NULLD> <" +
        patternGeneratorType + ">" + IO.m_NEW_LINE);
    outputString = outputString.concat("~h \"" + hmmName + "\"" + IO.m_NEW_LINE);
    outputString = outputString.concat("<BEGINHMM>" + IO.m_NEW_LINE);
    outputString = outputString.concat("<NUMSTATES> " + nnumberOfStates + IO.m_NEW_LINE);
    for (int nstateNumber = 2; nstateNumber < nnumberOfStates; nstateNumber++) {
      outputString = outputString.concat("<STATE> " + nstateNumber + IO.m_NEW_LINE);
      outputString = outputString.concat(m_mixturesOfGaussianPDFs[nstateNumber - 2].toStringAsInHTK() + IO.m_NEW_LINE);
    }
    outputString = outputString.concat("<TRANSP> " + getTransitionMatrix().length + IO.m_NEW_LINE);
    outputString = outputString.concat(IO.getPartOfMatrixAsString(LogDomainCalculator.calculateExp(m_ftransitionMatrix), m_ftransitionMatrix.length));
    outputString = outputString.concat(IO.m_NEW_LINE + "<ENDHMM>");
    return outputString;
  }


  /**
   *  Description of the Method
   *
   *@return    Description of the Returned Value
   */
  public Object clone() {
    return new ContinuousHMM(m_ftransitionMatrix,
        m_mixturesOfGaussianPDFs,
        m_topology);
  }


  /**
   *  Gets the TransitionMatrixOk attribute of the ContinuousHMM object
   *
   *@return    The TransitionMatrixOk value
   */
  private boolean isTransitionMatrixOk() {
    //check transition matrix
    //get a copy not in log domain
    float[][] ftransitionNotInLog = LogDomainCalculator.calculateExp(m_ftransitionMatrix);
    return TransitionMatrix.isTransitionMatrixOk(ftransitionNotInLog);
  }


  public void resetModelToRandomValues() {
    //keep the topology, but make values uniform
    m_ftransitionMatrix = LogDomainCalculator.calculateExp(m_ftransitionMatrix);
    for (int i = 0; i < m_ftransitionMatrix.length; i++) {
      float fcounter = 0;
      for (int j = 0; j < m_ftransitionMatrix[i].length; j++) {
        if (m_ftransitionMatrix[i][j] != 0) {
          m_ftransitionMatrix[i][j] = 1F;
          fcounter++;
        }
      }
      //normalize
      for (int j = 0; j < m_ftransitionMatrix[i].length; j++) {
        if (m_ftransitionMatrix[i][j] != 0) {
          m_ftransitionMatrix[i][j] /= fcounter;
        }
      }
    }
    m_ftransitionMatrix = LogDomainCalculator.calculateLog(m_ftransitionMatrix);


    int nspaceDimension = this.getSpaceDimension();
    //initialize mixtures
    for (int i = 0; i < m_mixturesOfGaussianPDFs.length; i++) {
      int nnumberOfGaussiansPerMixture = m_mixturesOfGaussianPDFs[i].getNumberOfGaussians();
      m_mixturesOfGaussianPDFs[i] = new MixtureOfGaussianPDFs(
          nnumberOfGaussiansPerMixture, nspaceDimension,
          CovarianceMatrix.Type.DIAGONAL);
    }
    this.isModelOk();
  }



  /**
   *  Initialize this object as a left-right no-skips, diagonal covariance matrix
   *  HMM with random numbers.
   *
   *@param  nnumberOfStates               Description of Parameter
   *@param  nnumberOfGaussiansPerMixture  Description of Parameter
   *@param  nspaceDimension               Description of Parameter
   */
  private void initializeModelRandomly(int nnumberOfStates,
      int nnumberOfGaussiansPerMixture,
      int nspaceDimension) {

    m_ftransitionMatrix = new float[nnumberOfStates][nnumberOfStates];

    m_ftransitionMatrix[0][1] = 1.0F;

    //discount 2 non-emitting states
    m_mixturesOfGaussianPDFs = new MixtureOfGaussianPDFs[nnumberOfStates - 2];

    //transition matrix: a[i][j] = P[S(t+1)=j / S(t)=i]
    //line: present state; column: next state
    //the initialization considers that Java already initializes the matrix with zero
    for (int i = 1; i < nnumberOfStates - 1; i++) {
      m_ftransitionMatrix[i][i] = 0.5F;
      m_ftransitionMatrix[i][i + 1] = 0.5F;
    }

    m_ftransitionMatrix[nnumberOfStates - 1][nnumberOfStates - 1] = 1.0F;
    //last state
    LogDomainCalculator.convertToLog(m_ftransitionMatrix);

    //initialize mixtures
    for (int i = 0; i < m_mixturesOfGaussianPDFs.length; i++) {
      m_mixturesOfGaussianPDFs[i] = new MixtureOfGaussianPDFs(nnumberOfGaussiansPerMixture, nspaceDimension, CovarianceMatrix.Type.DIAGONAL);
    }
    this.isModelOk();
  }

  public void splitGaussianWithLargestWeightForAllMixtures() {
    for (int i = 0; i < m_mixturesOfGaussianPDFs.length; i++) {
      m_mixturesOfGaussianPDFs[i].splitGaussianWithLargestWeight();
    }
  }

  //obtaining the vector 'result' from the next function
  public float[] getProperSizeVectorForOneFrameScore() {
    return new float[this.getNumberOfStates()-2];
  }

  //to avoid multiple calculations of gaussian mixtures
  public void getScoresForOneFrame( float[] fO, float[] result ) {
    int n = result.length;//believing that we know what we are doing...
    for( int i=0; i<n; i++ ) {
      result[i] = m_mixturesOfGaussianPDFs[i].calculateLogProbability(fO);
    }
  }

  public HMMContext getDefaultContext() {
    float[] prob = new float[this.getNumberOfStates()-2];
    Object[] hist = new Object[prob.length];
    for( int i=0; i<prob.length; i++ ) {
      prob[i] = LogDomainCalculator.m_fLOG_DOMAIN_ZERO;
    }
    return new HMMContext( prob, hist );
  }

  public Object updateContext( Object context, float inputValue, Object inHistory,
                                      float[] scores, float[] outputValues ) {
    HMMContext cnt = (HMMContext) context;
    int nstates = this.getNumberOfStates()-1;
    float[] newValues = new float[nstates-1];
    Object[] newHistories = new Object[nstates-1];
    float transition, best, estim;
    Object bestHistory;
    //update context
    for( int i=1; i<nstates; i++ ) {
      best = m_ftransitionMatrix[0][i]+inputValue;
      bestHistory = inHistory;
      for( int j=1; j<nstates; j++ ) {
        transition = m_ftransitionMatrix[j][i] + cnt.state[j-1];
        if( transition > best ) {
          best = transition;
          bestHistory = cnt.history[j-1];
        }
      }
      newValues[i-1] = best + scores[i-1];
      newHistories[i-1] = bestHistory;
    }
    cnt.state = newValues;
    cnt.history = newHistories;
    //getting the output
    best = newValues[0]+m_ftransitionMatrix[1][nstates];
    bestHistory = newHistories[0];
    estim = newValues[0];
    for( int i=2; i<nstates; i++ ) {
      transition = newValues[i-1]+m_ftransitionMatrix[i][nstates];
      if( transition > best ) {
        best = transition;
        bestHistory = newHistories[i-1];
      }
      if( newValues[i-1] > estim )
        estim = newValues[i-1];
    }
    outputValues[0] = estim;
    outputValues[1] = best;
    return bestHistory;
  }

  public Object recoverContext( Object context, float[] outputValues ) {
    HMMContext cnt = (HMMContext) context;
    int nstates = this.getNumberOfStates()-1;
    float best,estim,transition;
    Object bestHistory;
    best = cnt.state[0]+m_ftransitionMatrix[1][nstates];
    bestHistory = cnt.history[0];
    estim = cnt.state[0];
    for( int i=2; i<nstates; i++ ) {
      transition = cnt.state[i-1]+m_ftransitionMatrix[i][nstates];
      if( transition > best ) {
        best = transition;
        bestHistory = cnt.history[i-1];
      }
      if( cnt.state[i-1] > estim )
        estim = cnt.state[i-1];
    }
    outputValues[0] = estim;
    outputValues[1] = best;
    return bestHistory;
  }
}

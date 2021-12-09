package edu.ucsd.asr;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Vector;

/** Given a SetOfPatterns, initializes an HMM using Viterbi alignment.
 * It assumes left-to-right topology with no skips and that the
 * number of Gaussians per mixture is the same for all mixtures.
 *
 * @author Aldebaro Klautau
 * @version 2.0 - August 31, 2000
 */

//Implementation based on HTK manual and book by Rabiner

public final class HMMInitializer {

	//should read from properties with keyword: ContinuousHMMReestimator.fcovarianceFloor
	private static final float m_fcovarianceFloor = 1e-4F;

	private SetOfPatterns m_setOfPatterns;

	/**Counts with 2 non-emitting states
	 */
	private int m_nnumberOfStates;

	private int m_nnumberOfGaussiansPerMixture;

	private double m_dthreshold;

	/**Stores an int[] with the states assigned for each
	 * frame of m_setOfPatterns.
	 */
	private Vector m_nstateAssignment;

	/**Stores an int[] with the mixture component assigned for each
	 * frame of m_setOfPatterns.
	 */
	private Vector m_ngaussianAssignment;

	/**transition matrix: A[i][j] = P[S(t+1)=j / S(t)=i]
	line: present state; column: next state*/
	//2 states (entry and exit) are non-emitting
	private float[][] m_ftransitionMatrix;

	/**output probability matrix B[i][j] = P[O(t)=j / S(t)=i]
	line: state; column: output*/
	private DiagonalCovarianceGaussianPDF[][] m_dB;

	/** C[i][j] = weigth of j-th mixture in state i.*/
	private float[][] m_dC;

	//private HMM.Topology m_hMMTopology;

	int m_ntotalNumberOfValidTokens;
	int m_ntotalNumberOfValidFrames;

	//variables used in LBG algorithm
	int m_nmaximumNumberofIterations;
	double m_dsplittingConstant;
	double m_dstoppingCriterion;
	int m_nverbose;
	//HeaderProperties m_headerProperties;

	boolean m_oshouldWriteReportFile;
	String m_reportFileName;

	/** Assumes default values for LBG algorithm, modify through method
	setLBGParameters() of this object if necessary.*/
	public HMMInitializer(SetOfPatterns setOfPatterns,
							int nnumberOfStates,
							int nnumberOfGaussiansPerMixture,
							double dthresholdToStopIterations,
							String reestimationLogFileName,
							int nminimumNumberOfPatternsPerModel) {
							//HeaderProperties headerProperties) {


		//m_headerProperties = headerProperties;

		//String property = m_headerProperties.getProperty("ContinuousHMMReestimator.nminimumNumberOfPatternsPerModel","1");
		if (setOfPatterns.getNumberOfPatterns() <
		nminimumNumberOfPatternsPerModel) {
			 //Integer.parseInt(property)) {
			End.throwError("There are only " + setOfPatterns.getNumberOfPatterns() +
			" patterns, while the minimum is " + nminimumNumberOfPatternsPerModel);
		}

		if (reestimationLogFileName == null) {
			m_oshouldWriteReportFile = false;
		} else {
			m_oshouldWriteReportFile = true;
			m_reportFileName = reestimationLogFileName;
			FileNamesAndDirectories.createDirectoriesIfNecessaryGivenFileName(m_reportFileName);
		}

		//m_hMMTopology = hMMTopology;
		m_setOfPatterns = setOfPatterns;
		m_nnumberOfStates = nnumberOfStates;
		m_nnumberOfGaussiansPerMixture = nnumberOfGaussiansPerMixture;
		m_dthreshold = dthresholdToStopIterations;

		m_nstateAssignment = new Vector();
		m_ngaussianAssignment = new Vector();
		m_ftransitionMatrix = new float[nnumberOfStates][nnumberOfStates];
		//subtract 2 because there are 2 non-emitting states
		if (nnumberOfStates-2 < 1) {
			End.throwError(nnumberOfStates + " is not a valid number of states!" +
			" Do not to forget taking into account the 2 non-emitting states. 3 states is the minimum number.");
		}
		m_dB = new DiagonalCovarianceGaussianPDF[nnumberOfStates-2][m_nnumberOfGaussiansPerMixture];
		m_dC = new float[nnumberOfStates-2][m_nnumberOfGaussiansPerMixture];

		//default values for LBG, modify through method
		//setLBGParameters if necessary
		m_nmaximumNumberofIterations = 10;
		m_dsplittingConstant = 0.01;
		m_dstoppingCriterion = 0.1;

		//String property = m_headerProperties.getProperty("TrainingManager.nverbose","0");
		//m_nverbose = (Integer.valueOf(property)).intValue();
	}

	public void setVerbosity(int nverboseLevel) {
		m_nverbose = nverboseLevel;
	}

	/**Starts with an uniform segmentation and iterates using Viterbi-based
	 * segmentation until convergence, but returns an HMM with the specified
	 * transition matrix, instead of the one calculated (this guarantees
	 * a desired topology).
	 */
	public ContinuousHMM getHMMUsingViterbiAlignmentAndKMeans(float[][] ftransitionMatrix) {
		//calculate HMM
		ContinuousHMM continuousHMM = getHMMUsingViterbiAlignmentAndKMeans();

		//impose given transition matrix
		return new ContinuousHMM(ftransitionMatrix,
								 continuousHMM.getMixturesOfGaussianPDFs());
	}


	/**Starts with an uniform segmentation and iterates using Viterbi-based
	 * segmentation until convergence.
	 */
	public ContinuousHMM getHMMUsingViterbiAlignmentAndKMeans() {
		//m_nverbose = 3;
		StringBuffer informationForLogFile = new StringBuffer();
		if (m_nverbose > 1) {
			Print.dialog("Segmental K-means");
		}
		if (m_oshouldWriteReportFile) {
			SimpleDateFormat formatter = new SimpleDateFormat("dd/MMMMM/yyyyy 'at' hh:mm:ss aaa");
			//SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
			informationForLogFile.append("Date = " + formatter.format(new Date()) + "." + IO.m_NEW_LINE);
			informationForLogFile.append("Processing file with " + m_setOfPatterns.getNumberOfPatterns() + " patterns." + IO.m_NEW_LINE);
			informationForLogFile.append("Segmental K-means" + IO.m_NEW_LINE);
		}

		ContinuousHMM continuousHMM = null;
		MixtureOfGaussianPDFs[] mixturesOfGaussianPDFs = null;

		//in case the user calls this method for the second time without reconstructing the object
		m_nstateAssignment = new Vector();
		m_ngaussianAssignment = new Vector();

		//uniform segmentation
		findUniformSegmentation();

		//estimate transition matrix based on previous segmentation
		estimateTransitionMatrix();

		//well... the loooong name says everything
		//System.out.println("Here 1");
		assignFramesToMixturesUsingKMeansAndCalculateStatistics();

		//System.out.println("Here 2");
		//construct the first HMM
		mixturesOfGaussianPDFs = MixtureOfGaussianPDFs.assembleMixtures(m_dB, m_dC);
		continuousHMM = new ContinuousHMM(m_ftransitionMatrix, mixturesOfGaussianPDFs);

		//iteratively, run Viterbi alignment and update HMM model
		double dpreviousLogProbability = -Double.MAX_VALUE;
		double dimprovement = 0;
		int niterations = 0;
		do {
			//IO.DisplayEntriesofMatrix(this.m_ftransitionMatrix);
			findSegmentationUsingViterbi(continuousHMM);
			//IO.DisplayEntriesofMatrix(this.m_ftransitionMatrix);
			estimateTransitionMatrix();
			//IO.DisplayEntriesofMatrix(this.m_ftransitionMatrix);
			assignFramesToMixturesUsingKMeansAndCalculateStatistics();
			//now one has new matrices, so create a new model:
			mixturesOfGaussianPDFs = MixtureOfGaussianPDFs.assembleMixtures(m_dB, m_dC);
			continuousHMM = new ContinuousHMM(m_ftransitionMatrix, mixturesOfGaussianPDFs);

			//use a criterion consistent with Baum-Welch
			double dlogProbOfAllTokens = calculateLogProbability(continuousHMM);
			double dlogProbability =  dlogProbOfAllTokens / m_ntotalNumberOfValidTokens;
			//System.out.println("dlogProbability = " + dlogProbability);
			//dimprovement = (dlogProbability - dpreviousLogProbability)/Math.abs(dpreviousLogProbability);
			dimprovement = dlogProbability - dpreviousLogProbability;
			//System.out.println("dimprovement = " + dimprovement);
			if (m_oshouldWriteReportFile || (m_nverbose > 1)) {
				double dlogProbPerFrame = dlogProbOfAllTokens / m_ntotalNumberOfValidFrames;
				String information = null;
				if (niterations > 0) {
					if (niterations > 9) {
						//just to have a fancy console output :)
						 information = niterations + "| average log prob./frame=" + IO.format(dlogProbPerFrame) +
							 ", /token=" + IO.format(dlogProbability) +
							 ", convergence=" + IO.format(dimprovement);
					} else {
						 information = niterations + " | average log prob./frame=" + IO.format(dlogProbPerFrame) +
							 ", /token=" + IO.format(dlogProbability) +
							 ", convergence=" + IO.format(dimprovement);
					}
				} else {
						 information = niterations + " | average log prob./frame=" + IO.format(dlogProbPerFrame) +
							 ", /token=" + IO.format(dlogProbability);
				}
				if (m_oshouldWriteReportFile) {
					informationForLogFile.append(information + IO.m_NEW_LINE);
				}
				if (m_nverbose > 1) {
					Print.dialog(information);
				}
			}

			//update to next iteration
			dpreviousLogProbability = dlogProbability;
			niterations++;
		} while (Math.abs(dimprovement) > m_dthreshold);

		if (m_oshouldWriteReportFile) {
			informationForLogFile.append("Iterations = " + niterations + "." + IO.m_NEW_LINE);
			//call method to write file
			IO.appendStringToEndOfTextFile(m_reportFileName,informationForLogFile.toString());
		}

		return continuousHMM;
	}

	/**Sets the input parameters to the LBG algorithm. The constructor
	 * adopts the following default values:
	 * int m_nmaximumNumberofIterations = 10;
	 * double m_dsplittingConstant = 0.01;
	 * double m_dstoppingCriterion = 0.1;
	 * int m_nverbose = 0;
	 */
	public void setLBGParameters(int nmaximumNumberofIterations,
								 double dsplittingConstant,
								 double dstoppingCriterion,
								 int nverboseLevel) {
		m_nmaximumNumberofIterations = nmaximumNumberofIterations;
		m_dsplittingConstant = dsplittingConstant;
		m_dstoppingCriterion = dstoppingCriterion;
		m_nverbose = nverboseLevel;
	}

	/** Uses uniform segmentation to associate frames to states
	 * for the whole SetOfPatterns.
	 */
	private void findUniformSegmentation() {
		for (int i=0; i<m_setOfPatterns.getNumberOfPatterns(); i++) {
			Pattern pattern = m_setOfPatterns.getPattern(i);

			//System.out.println(pattern.getNumOfFrames());

			//subtract 2 because there are 2 non-emitting states
			if (pattern.getNumOfFrames() >= (m_nnumberOfStates - 2)) {
				int[] nstates = findUniformSegmentationOfAPattern(pattern,m_nnumberOfStates-2);
				m_nstateAssignment.addElement(nstates);
			} else {
				//couldn't find path, segment too short
				m_nstateAssignment.addElement(null);
				if (m_nverbose > 1) {
					 Print.warning("findUniformSegmentation() skipped pattern # = " + i + ", only " + pattern.getNumOfFrames() + " frames");
				}
			}
		}

		if (m_nstateAssignment.isEmpty()) {
			End.throwError("findUniformSegmentation() skipped all patterns. Patterns are too short");
		}

	}

	/** Uses uniform segmentation to associate frames to states.
	 * for one Pattern.
	 */
	private int[] findUniformSegmentationOfAPattern(Pattern pattern, int nnumberOfStates) {
		double dnumberOfFramesPerState = (double) pattern.getNumOfFrames() / nnumberOfStates;
		int[] nstates = new int[pattern.getNumOfFrames()];
		//add 1 because entry state is non-emitting
		for (int i=0; i<nstates.length; i++) {
			nstates[i] = 1 + (int) (i / dnumberOfFramesPerState);
		}
		//IO.DisplayVector(nstates);
		return nstates;
	}

	/**Associate frames to states using a Viterbi alignment procedure.
	 */
	private void findSegmentationUsingViterbi(ContinuousHMM continuousHMM) {

		//IO.DisplayEntriesofMatrix(this.m_ftransitionMatrix);
		int nnumberOfPatterns = m_setOfPatterns.getNumberOfPatterns();
		for (int i=0; i<nnumberOfPatterns; i++) {
			Pattern pattern = m_setOfPatterns.getPattern(i);
			float fscore = continuousHMM.getScoreUsingViterbi(pattern);
			if (fscore > LogDomainCalculator.m_fLOG_DOMAIN_ZERO) {
				int[] nstates = continuousHMM.getStateSequenceOfLastViterbi();
				//IO.DisplayVector(nstates);
				m_nstateAssignment.addElement(nstates);
			} else {
				//couldn't find path, segment too short
				m_nstateAssignment.addElement(null);
			}
		}
		//IO.DisplayEntriesofMatrix(this.m_ftransitionMatrix);
	}

	/**Calculate the average log probability per token and per frame
	 * of the SetOfPatterns using the best path probability, i.e.,
	 * Viterbi algorithm (instead of 'forward' probability).
	 */
	private double calculateLogProbability(ContinuousHMM continuousHMM) {

		int nnumberOfPatterns = m_setOfPatterns.getNumberOfPatterns();
		double dtotalLogProbability = 0.0;
		m_ntotalNumberOfValidTokens = 0;
		m_ntotalNumberOfValidFrames = 0;
		for (int i=0; i<nnumberOfPatterns; i++) {
			Pattern pattern = m_setOfPatterns.getPattern(i);
			float fscore = continuousHMM.getScoreUsingViterbi(pattern);
			if (fscore != LogDomainCalculator.m_fLOG_DOMAIN_ZERO) {
				 dtotalLogProbability += fscore;
				 m_ntotalNumberOfValidTokens++;
				 m_ntotalNumberOfValidFrames += pattern.getNumOfFrames();
			}
		}
		return dtotalLogProbability;
	}

	/**Estimates the transition matrix given an association of frames to
	 * states.
	 */
	private void estimateTransitionMatrix() {
		int[] ntransitionsFromThisState = new int[m_nnumberOfStates];
		int[][] m_nACounter = new int[m_nnumberOfStates][m_nnumberOfStates];
		for (int i=0; i<m_nstateAssignment.size(); i++) {
			int[] nstates = (int[]) m_nstateAssignment.elementAt(i);

			if (nstates == null) {
				//segment too short and couldn't find path
				continue;
			}

			//take care of first state
			m_nACounter[0][nstates[0]]++;
			ntransitionsFromThisState[0]++;

			//take care of last state
			m_nACounter[nstates[nstates.length-1]][m_nnumberOfStates-1]++;
			ntransitionsFromThisState[nstates[nstates.length-1]]++;

			for (int j=1; j<nstates.length; j++) {
				m_nACounter[nstates[j-1]][nstates[j]]++;
				ntransitionsFromThisState[nstates[j-1]]++;
			}
		}
		for (int i=0; i<m_nnumberOfStates-1; i++) {
			for (int j=1; j<m_nnumberOfStates; j++) {
				m_ftransitionMatrix[i][j] = ((float) m_nACounter[i][j]) / ntransitionsFromThisState[i];
			}
		}

		//last state remains in loop
		m_ftransitionMatrix[m_nnumberOfStates-1][m_nnumberOfStates-1] = 1.0F;

		//IO.DisplayMatrix(m_ftransitionMatrix);
	}

	/**The name says..
	 */
	private void assignFramesToMixturesUsingKMeansAndCalculateStatistics() {

		//instantiate a VectorQuantizerDesigner to do LBG (K-means)
		int nfinalCodebookSize = m_nnumberOfGaussiansPerMixture;
		int nspaceDimension = m_setOfPatterns.getPattern(0).getNumOfParametersPerFrame();
		VectorQuantizerDesigner vectorQuantizerDesigner = new
															VectorQuantizerDesigner(
																		nfinalCodebookSize,
																		nspaceDimension,
																		m_nmaximumNumberofIterations,
																		m_dsplittingConstant,
																		m_dstoppingCriterion,
																		m_nverbose);
		//System.out.println("Here 4");
		//
		for (int i=1; i<m_nnumberOfStates-1; i++) {
			Pattern patternTrainingSequence = findFramesAssociatedToGivenState(m_setOfPatterns, i);

			//use LBG to find the codebook
			VectorQuantizer vectorQuantizer = vectorQuantizerDesigner.designUsingLBG(patternTrainingSequence,null);

			//System.out.println("Here 5");
			//encode training sequence
			int[] nindices = vectorQuantizer.findBestCodewords(patternTrainingSequence);
			//for each component Gaussian, estimate mean and variance
			for (int ngaussian=0; ngaussian<m_nnumberOfGaussiansPerMixture; ngaussian++) {
				float[] dmean = estimateMean(patternTrainingSequence, nindices, ngaussian);
				float[] dvariances = estimateVariances(patternTrainingSequence, nindices, ngaussian,dmean);
				//double dminimumVariance = 1e-4;
				//subtract 1
				m_dB[i-1][ngaussian] = new DiagonalCovarianceGaussianPDF(dmean,dvariances);
			}
			//for each component Gaussian, estimate weight
			//a) first, calculate histogram
			int[] nweightCounter = new int[m_nnumberOfGaussiansPerMixture];
			for (int j=0; j<nindices.length; j++) {
				nweightCounter[nindices[j]]++;
			}
			//IO.DisplayVector(nweightCounter);
			//b) then normalize
			for (int j=0; j<m_nnumberOfGaussiansPerMixture; j++) {
				//-1 because entry state is non-emitting
				m_dC[i-1][j] = ((float) nweightCounter[j]) /nindices.length;
			}
		}
	}

	/**Given a SetOfPatterns and a state nstateOfInterest, compose one
	 * Pattern with all frames in the SetOfPatterns that were associated
	 * to nstateOfInterest.
	 */
	private Pattern findFramesAssociatedToGivenState(SetOfPatterns setOfPatterns, int nstateOfInterest) {

		Vector outputPattern = new Vector();

		//Print.dialog("nstateOfInterest = " + nstateOfInterest);

		for (int i=0; i<setOfPatterns.getNumberOfPatterns(); i++) {
			//Print.dialog("i = " + i);
			Pattern pattern = setOfPatterns.getPattern(i);
			int[] nstates = (int[]) m_nstateAssignment.elementAt(i);
			if (nstates == null) {
				//path was not found for this segment, possibly too short
				continue;
			}
			int nnumberOfFrames = pattern.getNumOfFrames();
			if (nnumberOfFrames != nstates.length) {
				Print.error("Pattern # " + i + ", nnumberOfFrames = " + nnumberOfFrames + ", nstates.length = " + nstates.length);
			}
			for (int j=0; j<nnumberOfFrames; j++) {
				//Print.dialog("j = " + j);
				if (nstates[j] == nstateOfInterest) {
					float[] parameters = pattern.getParametersOfGivenFrame(j);
					outputPattern.addElement(parameters);
				}
			}
		}
		if (outputPattern.size() > 1) {
			 return new Pattern(outputPattern);
		} else {
			End.throwError("No valid training example. That can " +
			"happen when the HMM has a left-right topology and the examples " +
			"are too short so that no one has a number of frames enough " +
			"to transverse the HMM");
			//make compiler happy
			return null;
		}
	}

	/** Calculates the mean vector given a Pattern and its state assignment.
	 */
	private float[] estimateMean(Pattern patternTrainingSequence, int[] nindices, int ngaussian) {
		int nnumberOfParametersPerFrame = patternTrainingSequence.getNumOfParametersPerFrame();
		float[][] dparameters = patternTrainingSequence.getParameters();
		float[] dmean = new float[nnumberOfParametersPerFrame];
		int ncounter = 0;
		if (dparameters != null) {
			for (int i=0; i<nindices.length; i++) {
				if (nindices[i] == ngaussian) {
					for (int j=0; j<nnumberOfParametersPerFrame; j++) {
						dmean[j] += dparameters[i][j];
					}
					ncounter++;
				}
			}
			for (int j=0; j<nnumberOfParametersPerFrame; j++) {
				dmean[j] /= ncounter;
			}
			return dmean;
		} else {
			return null;
		}
	}

	/** Calculates the variances vector given a Pattern and its state assignment.
	 */
	private float[] estimateVariances(Pattern patternTrainingSequence, int[] nindices, int ngaussian, float[] dmean) {
		int nnumberOfParametersPerFrame = patternTrainingSequence.getNumOfParametersPerFrame();
		float[][] dparameters = patternTrainingSequence.getParameters();
		float[] dvariances = new float[nnumberOfParametersPerFrame];
		int ncounter = 0;
		if (dparameters != null) {
			for (int i=0; i<nindices.length; i++) {
				if (nindices[i] == ngaussian) {
					for (int j=0; j<nnumberOfParametersPerFrame; j++) {
						dvariances[j] += (dparameters[i][j]-dmean[j])*(dparameters[i][j]-dmean[j]);
					}
					ncounter++;
				}
			}
			for (int j=0; j<nnumberOfParametersPerFrame; j++) {
				dvariances[j] /= ncounter;
				if (dvariances[j] < m_fcovarianceFloor) {
					dvariances[j] = m_fcovarianceFloor;
				}
			}
			//IO.displayPartOfVector(dvariances,5);
			return dvariances;
		} else {
			return null;
		}
	}

	public static float[][] getTransitionMatrixFromHMMPrototypeFile(String hMMConfigurationFileName) {
		//get initial HMM
		HMM hmm = HMMConfiguration.getInitialHMMFromHMMConfiguration(hMMConfigurationFileName);
		ContinuousHMM continuousHMM = (ContinuousHMM) hmm;
		return continuousHMM.getTransitionMatrix();
	}

	public static ContinuousHMM getHMMPrototypeBasedOnGivenTopologyAndGlobalMeanAndVariance(HMM hmm,
	String setOfPatternsFileName) {
		SetOfPatterns setOfPatterns = new SetOfPatterns(setOfPatternsFileName);
		return getHMMPrototypeBasedOnGivenTopologyAndGlobalMeanAndVariance(hmm, setOfPatterns);
	}

	public static ContinuousHMM getHMMPrototypeBasedOnGivenTopologyAndGlobalMeanAndVariance(HMM hmm,
	SetOfPatterns setOfPatterns) {

		float[][] fmeanAndVar = calculateGlobalMeanAndVariance(setOfPatterns);
		float[] fmean = fmeanAndVar[0];
		float[] fvar = fmeanAndVar[1];

		ContinuousHMM continuousHMM = (ContinuousHMM) hmm;

		float[][] ftransitionMatrix = continuousHMM.getTransitionMatrix();
		//use global mean and var
		continuousHMM = new ContinuousHMM(ftransitionMatrix, fmean, fvar);
		return continuousHMM;
	}

	public static ContinuousHMM getHMMPrototypeBasedOnGivenTopologyAndGlobalMeanAndVariance(String hMMConfigurationFileName,
	String setOfPatternsFileName) {
		SetOfPatterns setOfPatterns = new SetOfPatterns(setOfPatternsFileName);
		//get initial HMM
		HMM hmm = HMMConfiguration.getInitialHMMFromHMMConfiguration(hMMConfigurationFileName);
		return getHMMPrototypeBasedOnGivenTopologyAndGlobalMeanAndVariance(hmm, setOfPatterns);
	}

	/**
	 * Return: matrix where first line is the mean and
	 * second line is the variance.
	 */
	public static float[][] calculateGlobalMeanAndVariance(SetOfPatterns setOfPatterns) {
		PatternGenerator patternGenerator = setOfPatterns.getPatternGenerator();
		int nnumberOfParameters = patternGenerator.getNumberOfParameters();

		//Print.dialog("nnumberOfParameters = " + nnumberOfParameters);

		double[] dmean = new double[nnumberOfParameters];
		double[] dvar = new double[nnumberOfParameters];
		int nnumberOfPatterns = setOfPatterns.getNumberOfPatterns();
		long ltotalNumberOfFrames = 0;
		for (int i=0; i<nnumberOfPatterns; i++) {
			Pattern pattern = setOfPatterns.getPattern(i);
			float[][] fparameters = pattern.getParameters();
			int nnumberOfFrames = fparameters.length;
			for (int j=0; j<nnumberOfFrames; j++) {
				//to save double-indexing computing
				float[] fcurrentParameters = fparameters[j];
				for (int k=0; k<nnumberOfParameters; k++) {
					float fvalue = fcurrentParameters[k];
					dmean[k] += fvalue;
					//keep E[x^2] to use var = E[x^2] - E[x]^2 later on
					dvar[k] += fvalue * fvalue;
				}
			}
			ltotalNumberOfFrames += nnumberOfFrames;
		}
		float[] fmean = new float[dmean.length];
		float[] fvar = new float[dmean.length];

		for (int k=0; k<nnumberOfParameters; k++) {
			fmean[k] = (float) (dmean[k] / ltotalNumberOfFrames);
			fvar[k] = (float) (dvar[k] / ltotalNumberOfFrames);
		}
		for (int k=0; k<nnumberOfParameters; k++) {
			fvar[k] -= fmean[k] * fmean[k];
		}
		float[][] out = new float[2][];
		out[0] = fmean;
		out[1] = fvar;
		return out;
	}

	/**
	 * Return: matrix where first line is the mean and
	 * second line is the variance.
	 */
	public static float[][] calculateGlobalMeanAndVariance(String listOfParameterFiles) 
	throws Exception {
		String[] fileNames = IO.readArrayOfStringsFromFile(listOfParameterFiles);
		if (fileNames.length < 1) {
			throw new Exception("Error reading " + listOfParameterFiles);
		}
		Pattern pattern = HTKInterfacer.getPatternFromFile(fileNames[0]);
		int nnumberOfParameters = pattern.getNumOfParametersPerFrame();

		//Print.dialog("nnumberOfParameters = " + nnumberOfParameters);

		double[] dmean = new double[nnumberOfParameters];
		double[] dvar = new double[nnumberOfParameters];
		int nnumberOfPatterns = fileNames.length;
		long ltotalNumberOfFrames = 0;
		//AKA
		if (nnumberOfPatterns > 200) {
			System.err.println("Warning: using only 200 files from a total of " + nnumberOfPatterns);
			nnumberOfPatterns = 200;
		}
		for (int i=0; i<nnumberOfPatterns; i++) {
			//ak
			IO.showCounter(i);
			pattern = HTKInterfacer.getPatternFromFile(fileNames[i]);
			float[][] fparameters = pattern.getParameters();
			int nnumberOfFrames = fparameters.length;
			for (int j=0; j<nnumberOfFrames; j++) {
				//to save double-indexing computing
				float[] fcurrentParameters = fparameters[j];
				for (int k=0; k<nnumberOfParameters; k++) {
					float fvalue = fcurrentParameters[k];
					dmean[k] += fvalue;
					//keep E[x^2] to use var = E[x^2] - E[x]^2 later on
					dvar[k] += fvalue * fvalue;
				}
			}
			ltotalNumberOfFrames += nnumberOfFrames;
		}
		float[] fmean = new float[dmean.length];
		float[] fvar = new float[dmean.length];

		for (int k=0; k<nnumberOfParameters; k++) {
			fmean[k] = (float) (dmean[k] / ltotalNumberOfFrames);
			fvar[k] = (float) (dvar[k] / ltotalNumberOfFrames);
		}
		for (int k=0; k<nnumberOfParameters; k++) {
			fvar[k] -= fmean[k] * fmean[k];
		}
		float[][] out = new float[2][];
		out[0] = fmean;
		out[1] = fvar;
		return out;
	}
	
	
	public static ContinuousHMM createHMMWith1State(int nspaceDimension) {

		float[] fmean = new float[nspaceDimension];
		float[] fvar = new float[nspaceDimension];
		for (int i = 0; i < fvar.length; i++) {
			fvar[i] = 1;
		}
		DiagonalCovarianceGaussianPDF gaussianPDF = new DiagonalCovarianceGaussianPDF(fmean, fvar);

		DiagonalCovarianceGaussianPDF[] gaussians = {gaussianPDF};

		float[] fweights = {1F};

		MixtureOfGaussianPDFs mixtureOfGaussianPDFs = new MixtureOfGaussianPDFs(gaussians,fweights,true);
		MixtureOfGaussianPDFs[] mixtures = {mixtureOfGaussianPDFs};
		float[][] ftransition = { {0,1,0},
									{0,0.5F,0.5F},
									{0,0,1} };
		return new ContinuousHMM(ftransition,
								 mixtures,
								 HMM.Topology.LEFTRIGHT_NO_SKIPS);
	}

}
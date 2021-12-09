package edu.ucsd.asr;

import java.io.IOException;
import java.io.BufferedWriter;
import java.io.FileWriter;

/**
 * Re-estimates a set of (possibly) shared continuous hidden-Markov models (HMM)
 * using an embedded Baum-Welch algorithm. It follows HTK implementation. It is
 * sophisticated in the sense that uses "pruning" to save computations,
 * 
 * Also, it uses a 3-d array for the beta variables and two 2-d matrices for the
 * instantaneous alpha variables. Say that the given utterance corresponds to
 * the concatenation of 10 HMMs, each with 3 emitting states. Hence, the total
 * number of emitting states would be 30, and one could try to deal with a beta
 * matrix of dimension T x 30, where T is the number of frames. However, it is
 * more convenient to use a 3-d array of dimension T x 10 x 3. That is what the
 * code does: it avoids "unfolding" the states, but it is still capable of
 * knowing the sequence of states in the embedded HMM because the first state of
 * a HMM follows the last state of the previous HMM in the 3-d array.
 * 
 * The pruning is done by keeping a "beam" of active HMMs. Say that there are 10
 * HMMs for a given utterance. In theory, for a specific time t, one should
 * consider the probabilities of all 10 x 3 = 30 states. Instead, the code
 * thinks that at t = t0, only the 5th to the 8-th HMMs are active. That is, a
 * beam is organized with a lower qlo and higher qhi limits.
 * 
 * At one given time t, the code just deals with the "active" HMMs, the ones
 * that were not pruned. Look at the definition of m_dbeta.
 * 
 * @author Aldebaro Klautau
 * @created November 15, 2000
 * @version 2.0
 */
public class SetOfSharedContinuousHMMsReestimator {

	protected int m_nnumberOfSentencesWithBetaPruningError;

	protected float m_flogPruningThreshold;

	protected boolean m_ouseAbsolutePath;

	protected SimulationFilesAndDirectories m_simulationFilesAndDirectories;

	/**
	 * Level of text output detail (user feedback).
	 */
	protected int m_nverbose;

	protected HeaderProperties m_headerProperties;

	/**
	 * Set of HMMs that will be reestimated.
	 */
	protected SetOfSharedContinuousHMMsBeingReestimated m_setOfSharedContinuousHMMsBeingReestimated;

	/**
	 * Keeps a reference to the physical HMMs in above set. It is basically for
	 * saving typing because: m_hmms =
	 * m_setOfSharedContinuousHMMsBeingReestimated.m_physicalHMMs; In other
	 * words, m_hmms is redundant because one could directly use
	 * m_setOfSharedContinuousHMMsBeingReestimated.m_physicalHMMs instead.
	 */
	protected SetOfSharedContinuousHMMs.PhysicalHMM[] m_hmms;

	/**
	 * Table with names (labels) of HMMs (both physical and logical). The first
	 * label of i-th entry has the label (monophone, triphone, etc) associated
	 * with the i-th physical HMM. Other labels would correspond to logical HMMs
	 * associated to that physical HMM (label # 0).
	 */
	protected TableOfLabels m_tableOfHMMs;

	/**
	 * It has variable dimension Q, where Q is the number of HMMs in the
	 * transcription of current utterance, and each entry indicates the
	 * corresponding physical HMM.
	 */
	private int[] m_nindicesOfPhysicalHMMs;

	/**
	 * Stores for each physical HMM the minimum number of frames necessary to
	 * traverse the model from the entry to exit state. For example, it is 0 for
	 * "tee" models. The dimension is the number of physical HMMs.
	 */
	private short[] m_sminimumDurations;

	/**
	 * Output probabilities in log domain. The dimension is [T][Q(t)][N(q)-2],
	 * where T is the number of frames in current utterance, Q(t) = Qmax-Qmin+1
	 * is the range Qmin:Qmax of active models for frame t and N(q) is the
	 * number of emitting states of HMM specified by [t][q][].
	 */
	private float[][][] m_flogOutputProbabilities;

	/**
	 * Information about pruning.
	 */
	protected PruningInformation m_pruningInformation;

	/**
	 * Probability of current utterance in log domain (as calculated by backward
	 * recursion, which must coincide with the one obtained by forward
	 * recursion).
	 */
	protected double m_dlogProbabilityOfCurrentUtterance;

	/**
	 * Backward values in log domain. The dimension is [T][Q'(t)][N(q)], where T
	 * is the number of frames in current utterance, Q'(t) = Q'max-Q'min+1 is
	 * the range Q'min:Q'max of active models for frame t and N(q) is the number
	 * of emitting states of HMM specified by [t][q][].
	 * 
	 * AK: Why not using the notation Q(t) = Qmax-Qmin+1 as for the
	 * m_flogOutputProbabilities?
	 * It is because they are different numbers, I think. See there are
		private int[] m_nfirstHMMInOutputProbabilityMatrix;
	and
		private int[] m_nfirstHMMInBetaMatrix;
	at the same time.
	 */
	private double[][][] m_dbeta;

	/**
	 * Instead of being 3-D as m_dbeta, the algorithm stores forward values only
	 * for time t and t+1, using two 2-D matrices m_dalphat and m_dalphat1. Both
	 * matrices have size [Q][Nq], where Q is the number of HMMs for current
	 * utterance and Nq is the number of states for each physical HMM associated
	 * with q=0:Q-1. Notice that it allocates spaces for Q HMMs, instead of
	 * saving memory by allocating only space for the range of active HMMs as in
	 * m_dbeta. This allows a faster computation.
	 */
	private double[][] m_dalphat;

	private double[][] m_dalphat1;

	/**
	 * Expected number of times the signal is at a given state. It is used for a
	 * specific time, for a specific HMM. The dimension is the maximum number of
	 * states in the set of physical HMMs.
	 */
	protected float[] m_foccupationCountForCurrentTime;

	/**
	 * Because the matrix with output probabilities is allocated based on a
	 * range of active HMMs, the first HMM index must be stored in order to
	 * allow accessing the matrix.
	 */
	private int[] m_nfirstHMMInOutputProbabilityMatrix;

	/**
	 * Because the matrix beta with backward values is allocated based on a
	 * range of active HMMs, the first HMM index must be stored in order to
	 * allow accessing the matrix.
	 */
	private int[] m_nfirstHMMInBetaMatrix;

	/**
	 * Temporary storage for zero mean observation vector. It is a trick for
	 * speeding up the calculation of variance estimates.
	 */
	protected float[] m_fauxiliaryZeroMean;

	/**
	 * Minimum value allowed for a mixture weight.
	 */
	private float m_fmixtureWeightFloor;

	private float m_fcovarianceFloor = 5e-2F;

	/**
	 * Minimum number of occurrence of a label in order to have the model
	 * updated after reestimation. If the transcriptions contain a total number
	 * of occurrences below this threshold, the initial model is copied
	 * unchanged into the output file.
	 */
	private int m_nminimumNumberOfLabelOccurrencesForUpdatingModel;

	/**
	 * If false, there is no reestimation for the corresponding object.
	 */
	private boolean m_oshouldUpdateTransitionMatrix;

	private boolean m_oshouldUpdateMean;

	private boolean m_oshouldUpdateCovariance;

	private boolean m_oshouldUpdateWeights;

	/**
	 * Provides the transcriptions.
	 */
	protected String m_dataLocatorFileName;

	/**
	 * If true, the gamma values are stored or showed.
	 */
	protected boolean m_oshouldOutputGammaMatrix;
	
	/**
	 * Stores the gamma values if m_oshouldOutputGammaMatrix is true.
	 * Note that I am not going to save storage space and, differently
	 * than the m_dbeta matrix, m_dgamma will have dimension T x Nh x Nq
	 * where Nh is the total number of physical HMMs for this SetOfSharedHMMs.
	 * Let us say there are T=100 frames, Nh=40 and each HMM has Nq=3
	 * states. The storage space will be approximately 96 KBytes.   
	 */
	protected double[][][] m_dgamma;
	
	/**
	 * File for writing the gamma values.
	 */
	protected static String m_gammaOutputFileName;
	
	/**
	 * Empty constructor. To be used by the AbsurdHMMsReestimator class.
	 */
	public SetOfSharedContinuousHMMsReestimator() {
		
	}

	/**
	 * Constructor for the SetOfSharedContinuousHMMsReestimator object
	 * 
	 * @param setOfSharedContinuousHMMsBeingReestimated
	 *            Set of HMMs that will be reestimated
	 */
	public SetOfSharedContinuousHMMsReestimator(
			SetOfSharedContinuousHMMsBeingReestimated setOfSharedContinuousHMMsBeingReestimated,
			PatternGenerator patternGenerator,
			HeaderProperties headerProperties,
			// String setOfHMMOutputFileName,
			String dataLocatorFileName, String setOfPatternsInputDirectory,
			String outputOccupationStatisticsFileName) {
		// m_setOfHMMOutputFileName = setOfHMMOutputFileName;
		m_dataLocatorFileName = dataLocatorFileName;

		m_headerProperties = headerProperties;

		m_simulationFilesAndDirectories = new SimulationFilesAndDirectories(
				m_headerProperties);

		// AK - BUG should use cloning
		m_setOfSharedContinuousHMMsBeingReestimated = setOfSharedContinuousHMMsBeingReestimated;
		m_tableOfHMMs = setOfSharedContinuousHMMsBeingReestimated.m_tableOfLabels;
		m_hmms = setOfSharedContinuousHMMsBeingReestimated.m_physicalHMMs;

		// allocate space
		m_fauxiliaryZeroMean = new float[setOfSharedContinuousHMMsBeingReestimated.m_mixturesOfGaussianPDFsBeingReestimated[0]
				.getSpaceDimension()];
		m_foccupationCountForCurrentTime = new float[m_setOfSharedContinuousHMMsBeingReestimated
				.getMaximumNumberOfStatesInThisSet()];

		m_nnumberOfSentencesWithBetaPruningError = 0;
		
		//AK look at this strange solution!
/*		if (this instanceof AbsurdHMMsReestimator) {
			((AbsurdHMMsReestimator) this).interpretHeaderAndRunHMMReestimation(patternGenerator, setOfPatternsInputDirectory, outputOccupationStatisticsFileName);
		} else {
			interpretHeaderAndRunHMMReestimation(patternGenerator,
					setOfPatternsInputDirectory, outputOccupationStatisticsFileName);
		}
*/
		interpretHeader(patternGenerator,
				setOfPatternsInputDirectory, outputOccupationStatisticsFileName);
		
		// call embedded training
		reestimateSetOfHMMsUsingEmbeddedBaumWelch(patternGenerator,
				m_dataLocatorFileName,
				setOfPatternsInputDirectory, outputOccupationStatisticsFileName);		
	}

	protected void interpretHeader(
			PatternGenerator patternGenerator,
			String setOfPatternsInputDirectory,
			String outputOccupationStatisticsFileName) {

		// String dataLocatorFileName =
		// m_headerProperties.getPropertyAndExitIfKeyNotFound("SetOfSharedContinuousHMMReestimator.DataLocatorFileName");
		// m_headerProperties.setProperty("SetOfSharedContinuousHMMReestimator.DataLocatorFileName",dataLocatorFileName);

		// String setOfHMMOutputFileName =
		// m_headerProperties.getPropertyAndExitIfKeyNotFound("SetOfSharedContinuousHMMReestimator.OutputSetOfHMMsFileName");
		// m_headerProperties.setProperty("SetOfSharedContinuousHMMReestimator.OutputSetOfHMMsFileName",setOfHMMOutputFileName);

		String property = m_headerProperties.getProperty(
				"TrainingManager.nverbose", "0");
		m_nverbose = (Integer.valueOf(property)).intValue();
		CheckValues.exitOnError(m_nverbose, 0, 10, "TrainingManager.nverbose");
		m_headerProperties.setProperty("TrainingManager.nverbose", property);

		property = m_headerProperties
				.getProperty(
						"SetOfSharedContinuousHMMReestimator.oshouldUpdateTransitionMatrix",
						"true");
		m_oshouldUpdateTransitionMatrix = (Boolean.valueOf(property))
				.booleanValue();
		m_headerProperties
				.setProperty(
						"SetOfSharedContinuousHMMReestimator.oshouldUpdateTransitionMatrix",
						property);

		property = m_headerProperties
				.getProperty(
						"SetOfSharedContinuousHMMReestimator.oshouldUpdateMean",
						"true");
		m_oshouldUpdateMean = (Boolean.valueOf(property)).booleanValue();
		m_headerProperties.setProperty(
				"SetOfSharedContinuousHMMReestimator.oshouldUpdateMean",
				property);

		property = m_headerProperties.getProperty(
				"SetOfSharedContinuousHMMReestimator.oshouldUpdateCovariance",
				"true");
		m_oshouldUpdateCovariance = (Boolean.valueOf(property)).booleanValue();
		m_headerProperties.setProperty(
				"SetOfSharedContinuousHMMReestimator.oshouldUpdateCovariance",
				property);

		property = m_headerProperties.getProperty(
				"SetOfSharedContinuousHMMReestimator.oshouldUpdateWeights",
				"true");
		m_oshouldUpdateWeights = (Boolean.valueOf(property)).booleanValue();
		m_headerProperties.setProperty(
				"SetOfSharedContinuousHMMReestimator.oshouldUpdateWeights",
				property);

		property = m_headerProperties.getProperty(
				"ContinuousHMMReestimator.nmaximumIterations", "20");
		int nmaximumIterations = (Integer.valueOf(property)).intValue();
		CheckValues.exitOnError(nmaximumIterations, 1, 1000,
				"ContinuousHMMReestimator.nmaximumIterations");
		m_headerProperties.setProperty(
				"ContinuousHMMReestimator.nmaximumIterations", property);

		property = m_headerProperties.getProperty(
				"SetOfSharedContinuousHMMReestimator.fmixtureWeightFloor",
				"0.0");
		m_fmixtureWeightFloor = (Float.valueOf(property)).floatValue();
		CheckValues.exitOnError(m_fmixtureWeightFloor, 0.0F, 1.0F,
				"SetOfSharedContinuousHMMReestimator.fmixtureWeightFloor");
		m_headerProperties.setProperty(
				"SetOfSharedContinuousHMMReestimator.fmixtureWeightFloor",
				property);

		property = m_headerProperties.getProperty(
				"ContinuousHMMReestimator.fcovarianceFloor", "1e-4");
		m_fcovarianceFloor = (Float.valueOf(property)).floatValue();
		CheckValues.exitOnError(m_fcovarianceFloor, 0.0F, 1000.0F,
				"ContinuousHMMReestimator.fcovarianceFloor");
		m_headerProperties.setProperty(
				"ContinuousHMMReestimator.fcovarianceFloor", property);

		property = m_headerProperties
				.getProperty(
						"SetOfSharedContinuousHMMReestimator.nminimumNumberOfLabelOccurrencesForUpdatingModel",
						"3");
		m_nminimumNumberOfLabelOccurrencesForUpdatingModel = (Integer
				.valueOf(property)).intValue();
		CheckValues
				.exitOnError(
						m_nminimumNumberOfLabelOccurrencesForUpdatingModel,
						0,
						1000,
						"SetOfSharedContinuousHMMReestimator.nminimumNumberOfLabelOccurrencesForUpdatingModel");
		m_headerProperties
				.setProperty(
						"SetOfSharedContinuousHMMReestimator.nminimumNumberOfLabelOccurrencesForUpdatingModel",
						property);

		property = m_headerProperties.getProperty(
				"SetOfSharedContinuousHMMReestimator.flogPruningThreshold",
				"2000");
		m_flogPruningThreshold = (Float.valueOf(property)).floatValue();
		CheckValues.exitOnError(m_flogPruningThreshold, 0.0F, 2e30F,
				"SetOfSharedContinuousHMMReestimator.flogPruningThreshold");
		m_headerProperties.setProperty(
				"SetOfSharedContinuousHMMReestimator.flogPruningThreshold",
				property);

		if (m_nverbose > 3) {
			Print.dialog("Pruning in beta (first) pass (log) = "
					+ m_flogPruningThreshold);
		}

		property = m_headerProperties.getProperty(
				"TrainingManager.ouseAbsolutePath", "false");
		m_ouseAbsolutePath = (Boolean.valueOf(property)).booleanValue();
		m_headerProperties.setProperty("TrainingManager.ouseAbsolutePath",
				property);

		property = m_headerProperties.getProperty(
				"ContinuousHMMReestimator.oshouldOutputGammaMatrix", "false");
		m_oshouldOutputGammaMatrix = (Boolean.valueOf(property)).booleanValue();
		m_headerProperties.setProperty(
				"ContinuousHMMReestimator.oshouldOutputGammaMatrix", property);
		
		m_gammaOutputFileName = m_headerProperties.getProperty(
				"SetOfSharedContinuousHMMs.gammaOutputFileName", "gammaOutputFile.bin");		
		m_headerProperties.setProperty(
				"SetOfSharedContinuousHMMs.gammaOutputFileName", m_gammaOutputFileName);
	}

	/**
	 * Get the set of HMMs reestimated using embedded training.
	 * 
	 * @return The SetOfSharedContinuousHMMs reestimated using embedded training
	 */
	// XXX AK BUG not cloning...
	public SetOfSharedContinuousHMMs getSetOfReestimatedHMMs() {
		SetOfSharedContinuousHMMs setOfSharedContinuousHMMs = (SetOfSharedContinuousHMMs) m_setOfSharedContinuousHMMsBeingReestimated;
		int numberOfMixtures = m_setOfSharedContinuousHMMsBeingReestimated.m_mixturesOfGaussianPDFsBeingReestimated.length;
		setOfSharedContinuousHMMs.m_mixturesOfGaussianPDFs = new MixtureOfGaussianPDFs[numberOfMixtures];
		// Because I'm using "beingreestimated" I must convert it
		for (int i = 0; i < numberOfMixtures; i++) {
			try {
				setOfSharedContinuousHMMs.m_mixturesOfGaussianPDFs[i] = m_setOfSharedContinuousHMMsBeingReestimated.m_mixturesOfGaussianPDFsBeingReestimated[i]
						.getMixtureOfGaussianPDFs();
			} catch (Error e) {
				Print.error("Problem with mixture number " + i);
				// Print.error("Mixture:");
				// Print.error(m_setOfSharedContinuousHMMsBeingReestimated.m_mixturesOfGaussianPDFsBeingReestimated[i].getMixtureOfGaussianPDFs().toStringAsInHTK());
				for (int k = 0; k < m_hmms.length; k++) {
					int[] nmixtureIndices = m_hmms[k].getMixtureIndices();
					for (int j = 0; j < nmixtureIndices.length; j++) {
						if (nmixtureIndices[j] == i) {
							Print
									.error("This mixture is associated to HMM "
											+ m_setOfSharedContinuousHMMsBeingReestimated.m_tableOfLabels
													.getLabelsAsString(k));
						}
					}
				}
				End.exit(1);
			}

			// eliminate components with small weight
			try {
				setOfSharedContinuousHMMs.m_mixturesOfGaussianPDFs[i]
						.discardGaussianWithVerySmallWeights();
			} catch (ASRError e) {
				if (m_nverbose > 1) {
					Print.dialog(e.getMessage());
				}
			}
		}
		// Because there's no "beingreestimated" in transition matrix I can:
		setOfSharedContinuousHMMs.m_transitionMatrices = (TransitionMatrix[]) m_setOfSharedContinuousHMMsBeingReestimated.m_transitionMatrixBeingReestimated;
		return setOfSharedContinuousHMMs;
	}

	/**
	 * calculateOccupationCountForCurrentTimeAndHMM: set the global occupation
	 * count for given hmm
	 * 
	 * @param hmm
	 *            current HMM
	 * @param aqt
	 *            forward variables for time t
	 * @param bqt
	 *            backward variables for time t
	 * @param bq1t
	 *            backward variables for time t + 1
	 */
	private void calculateOccupationCountForCurrentTimeAndHMM(
			SetOfSharedContinuousHMMs.PhysicalHMM hmm, double[] aqt,
			double[] bqt, double[] bq1t, int t) {

		int N = hmm.getNumberOfStates();
		float[][] transP = hmm.getTransitionMatrix().getMatrixInLogDomain();

		for (int i = 0; i < N; i++) {
			double x = aqt[i] + bqt[i];
			if (i == 0 && bq1t != null
					&& transP[0][N - 1] > LogDomainCalculator.m_fSMALL_NUMBER) {
				x = LogDomainCalculator.add(x, aqt[0] + bq1t[0]
						+ transP[0][N - 1]);
			}

			x -= m_dlogProbabilityOfCurrentUtterance;
			m_foccupationCountForCurrentTime[i] = (float) ((x > LogDomainCalculator.m_fMINIMUM_EXP_ARGUMENT) ? LogDomainCalculator
					.calculateExp(x)
					: 0.0);

		}
		// AK I will try to retrieve the "gamma" values from below
		//if (m_oshouldOutputGammaMatrix) {
		//	System.err.println("Current time = " + t);
		//	for (int i = 0; i < N; i++) {
		//		System.err.print(m_foccupationCountForCurrentTime[i] + " ");
		//	}
		//	System.err.println("");
		//}
	}

	/**
	 * Obtain the indices of physical HMMs corresponding to each label in
	 * current transcription.
	 * 
	 * @param dataLocator
	 *            transcription of current utterance
	 */
	protected void getSequenceOfHMMsFromLabels(DataLocator dataLocator) {
		// Print.dialog(m_tableOfHMMs.toString());
		int nnumberOfSegments = dataLocator.getNumberOfSegments();
		if (nnumberOfSegments < 1) {
			End.throwError("Number of segments smaller than 1 in "
					+ dataLocator.toString());
		}
		m_nindicesOfPhysicalHMMs = new int[nnumberOfSegments];
		for (int i = 0; i < nnumberOfSegments; i++) {
			String label = dataLocator.getLabelFromGivenSegment(i);
			// Print.dialog("label " + label);
			// initially assume it corresponds to a physical HMM
			m_nindicesOfPhysicalHMMs[i] = m_tableOfHMMs.getEntry(label);
			if (m_nindicesOfPhysicalHMMs[i] == -1) {
				End.throwError("Couldn't find label " + label
						+ " associated to any physical or logical HMM.");
			}
		}
	}

	/**
	 * Get the minimum duration (number of frames to traverse HMM) for all HMMs
	 * in this set and return the sum.
	 * 
	 * @return The sum of all minimum durations for the HMMs of current
	 *         transcription
	 */
	protected int getMinimumDurationForAllHMMsAndSumThemUp() {
		m_sminimumDurations = new short[m_nindicesOfPhysicalHMMs.length];
		int ntotalDuration = 0;
		TransitionMatrixBeingReestimated[] transitionMatricesBeingReestimated = m_setOfSharedContinuousHMMsBeingReestimated.m_transitionMatrixBeingReestimated;

		// IO.DisplayVector(m_nindicesOfPhysicalHMMs);
		for (int i = 0; i < m_nindicesOfPhysicalHMMs.length; i++) {
			int ntransitionMatrixIndex = m_setOfSharedContinuousHMMsBeingReestimated.m_physicalHMMs[m_nindicesOfPhysicalHMMs[i]]
					.getTransitionMatrixIndex();
			// Print.dialog("ntransitionMatrixIndex = " +
			// ntransitionMatrixIndex);
			m_sminimumDurations[i] = transitionMatricesBeingReestimated[ntransitionMatrixIndex].m_sminimumDuration;
			// Print.dialog("m_sminimumDurations[i] " + m_sminimumDurations[i]);
			ntotalDuration += m_sminimumDurations[i];
			if (i > 1 && m_sminimumDurations[i] == 0
					&& m_sminimumDurations[i - 1] == 0) {
				End.throwError("Cannot have successive Tee models.");
			}
		}
		if ((m_sminimumDurations[0] == 0)
				|| (m_sminimumDurations[m_sminimumDurations.length - 1] == 0)) {
			End
					.throwError("CreateInsts: Cannot have Tee models at start or end of transcription.");
		}
		return ntotalDuration;
	}

	/**
	 * Gets the number of HMMs in current transcription.
	 * 
	 * @return The number of HMMs in current transcription.
	 */
	private int getNumberOfHMMsInCurrentUtterance() {
		return m_nindicesOfPhysicalHMMs.length;
	}

	/**
	 * Allocate space for two matrices with forward values.
	 * 
	 * @param nnumberOfHMMsInCurrentUtterance
	 *            number of HMMs
	 */
	private void allocateSpaceForAlphaMatrices(
			int nnumberOfHMMsInCurrentUtterance) {
		m_dalphat = new double[nnumberOfHMMsInCurrentUtterance][];
		m_dalphat1 = new double[nnumberOfHMMsInCurrentUtterance][];
		for (int q = 0; q < nnumberOfHMMsInCurrentUtterance; q++) {
			int nnumberOfStates = m_hmms[m_nindicesOfPhysicalHMMs[q]]
					.getNumberOfStates();
			m_dalphat[q] = new double[nnumberOfStates];
			m_dalphat1[q] = new double[nnumberOfStates];
		}
	}

	/**
	 * Allocate space for backward values
	 * 
	 * @param qLo
	 *            lower limit of active HMM beam
	 * @param qHi
	 *            upper limit of active HMM beam
	 * @param Q
	 *            total number of HMMs
	 * @return allocated matrix
	 */
	private double[][] createBetaQ(int qLo, int qHi, int Q) {
		// int q;
		double[][] v;

		qLo--;
		qLo--;
		if (qLo < 0) {
			qLo = 0;
		}
		qHi++;
		if (qHi > (Q - 1)) {
			qHi = Q - 1;
		}
		v = new double[qHi - qLo + 1][];
		return v;
	}

	/**
	 * Calculate forward values for first frame (t = 0).
	 * 
	 * @return startAndEnd range of active HMMs at t = 0
	 * @param Q
	 *            number of HMMs in current utterance
	 */
	// startAndEnd is array because values will be returned.
	private void calculateForwardValuesForTimeEqualToZero(int[] startAndEnd,
			int Q) {

		// int start = startAndEnd[0];
		// int end = startAndEnd[1];

		PruningInformation p = m_pruningInformation;

		double a1N = 0.0;
		int eq = p.m_sqHi[0];
		for (int q = 0; q <= eq; q++) {
			SetOfSharedContinuousHMMs.PhysicalHMM hmm = m_hmms[m_nindicesOfPhysicalHMMs[q]];
			float[][] transP = hmm.getTransitionMatrix().getMatrixInLogDomain();
			int Nq = hmm.getNumberOfStates();

			double[] aq = m_dalphat[q];
			aq[0] = (q == 0) ? 0.0 : m_dalphat[q - 1][0] + a1N;
			float[] outprob = m_flogOutputProbabilities[0][q
					- m_nfirstHMMInOutputProbabilityMatrix[0]];
			if (outprob == null) {
				End.throwError("Outprob NULL in model " + q
						+ " in calculateForwardValuesForTimeEqualToZero");
			}
			for (int j = 1; j < Nq - 1; j++) {
				double a = transP[0][j];
				// -1 in outprob because first state is non-emitting
				aq[j] = (a > LogDomainCalculator.m_fSMALL_NUMBER) ? aq[0] + a
						+ outprob[j - 1]
						: LogDomainCalculator.m_fLOG_DOMAIN_ZERO;
			}
			double x = LogDomainCalculator.m_fLOG_DOMAIN_ZERO;
			for (int i = 1; i < Nq - 1; i++) {
				double a = transP[i][Nq - 1];
				if (a > LogDomainCalculator.m_fSMALL_NUMBER) {
					x = LogDomainCalculator.add(x, aq[i] + a);
				}
			}
			aq[Nq - 1] = x;
			a1N = transP[0][Nq - 1];
		}
		zeroAlpha(eq + 1, Q - 1);
		startAndEnd[0] = 0;
		startAndEnd[1] = eq;
	}

	/**
	 * Zero forward values (m_dalphat) for HMMs in given active range.
	 * 
	 * @param qlo
	 *            first HMM in active range
	 * @param qhi
	 *            last HMM in active range
	 */
	private void zeroAlpha(int qlo, int qhi) {
		for (int q = qlo; q <= qhi; q++) {
			int Nq = m_hmms[m_nindicesOfPhysicalHMMs[q]].getNumberOfStates();
			double[] aq = m_dalphat[q];
			for (int j = 0; j < Nq; j++) {
				aq[j] = LogDomainCalculator.m_fLOG_DOMAIN_ZERO;
			}
		}
	}

	/**
	 * Calculate maximum probability of being in model q at time t, return
	 * "zero" in log domain if cannot do so.
	 * 
	 * @param q
	 *            current HMM
	 * @param t
	 *            current t
	 * @param minq
	 *            minimum index of active HMM
	 * @return maximum probability
	 */
	private double maxModelProb(int q, int t, int minq) {

		double[] bq;
		double x;
		SetOfSharedContinuousHMMs.PhysicalHMM hmm;
		double maxP;

		if (q == 0) {
			maxP = LogDomainCalculator.m_fLOG_DOMAIN_ZERO;
		} else {
			double[] bq1;
			if (q - 1 - m_nfirstHMMInBetaMatrix[t] >= 0) {
				bq1 = m_dbeta[t][q - 1 - m_nfirstHMMInBetaMatrix[t]];
			} else {
				bq1 = null;
			}
			hmm = m_hmms[m_nindicesOfPhysicalHMMs[q - 1]];
			float[][] transP = hmm.getTransitionMatrix().getMatrixInLogDomain();
			int Nq1 = hmm.getNumberOfStates();
			maxP = (bq1 == null) ? LogDomainCalculator.m_fLOG_DOMAIN_ZERO
					: m_dalphat[q - 1][Nq1 - 1] + bq1[Nq1 - 1];
			for (int qx = q - 1; qx > minq
					&& transP[0][Nq1 - 1] > LogDomainCalculator.m_fSMALL_NUMBER; qx--) {
				int qx1 = qx - 1;
				bq1 = m_dbeta[t][qx1 - m_nfirstHMMInBetaMatrix[t]];
				Nq1 = m_hmms[m_nindicesOfPhysicalHMMs[qx1]].getNumberOfStates();
				x = (bq1 == null) ? LogDomainCalculator.m_fLOG_DOMAIN_ZERO
						: m_dalphat[qx1][Nq1 - 1] + bq1[Nq1 - 1];
				if (x > maxP) {
					maxP = x;
				}
			}
		}
		hmm = m_hmms[m_nindicesOfPhysicalHMMs[q]];
		int Nq = hmm.getNumberOfStates();
		if (q - m_nfirstHMMInBetaMatrix[t] >= 0) {
			bq = m_dbeta[t][q - m_nfirstHMMInBetaMatrix[t]];
		} else {
			bq = null;
		}
		if (bq != null) {
			double[] aq = m_dalphat[q];
			for (int i = 0; i < Nq - 1; i++) {
				if ((x = aq[i] + bq[i]) > maxP) {
					maxP = x;
				}
			}
		}
		return maxP;
	}

	/**
	 * Calculate forward matrix for time t and return forward beam limits in
	 * startAndEnd
	 * 
	 * @param t
	 *            current time
	 * @param startAndEnd
	 *            beam limits
	 * @param Q
	 *            total number of HMMs in current utterance
	 * @param T
	 *            total number of frames in current utterance
	 */
	private void stepAlpha(int t, int[] startAndEnd, int Q, int T) {

		double y;
		double a;

		double x = 0.0;
		double a1N = 0.0;

		// First prune beta beam further to get alpha beam
		PruningInformation p = m_pruningInformation;
		// start start-point at bottom of beta beam at t-1
		int sq = p.m_sqLo[t - 1];
		double pr = m_dlogProbabilityOfCurrentUtterance;

		while (pr - maxModelProb(sq, t - 1, sq) > p.m_fminFrwdP) {
			// raise start point
			++sq;
			if (sq > p.m_sqHi[t]) {
				End.throwError("StepAlpha: Alpha prune failed sq(" + sq
						+ ") > qHi(" + p.m_sqHi[t] + ").");
			}
		}
		// start-point below beta beam so pull it back
		if (sq < p.m_sqLo[t]) {
			sq = p.m_sqLo[t];
		}

		int eq = (p.m_sqHi[t - 1] < (Q - 1)) ? p.m_sqHi[t - 1] + 1
				: p.m_sqHi[t - 1];
		// start end-point at top of beta beam at t-1
		// JJO : + 1 to allow for state q-1[N] -> q[1]
		// + 1 for each tee model following eq.
		while (pr - maxModelProb(eq, t - 1, sq) > p.m_fminFrwdP) {
			// lower end-point
			--eq;
			if (eq < sq) {
				End.throwError("StepAlpha: Alpha prune failed eq(" + eq
						+ ") < sq(" + sq + ").");
			}
		}
		while (eq < Q - 1 && m_sminimumDurations[eq] == 0) {
			eq++;
		}
		// end point above beta beam so pull it back
		if (eq > p.m_sqHi[t]) {
			eq = p.m_sqHi[t];
		}

		// Now compute current alpha column
		double[][] tmp = m_dalphat1;
		m_dalphat1 = m_dalphat;
		m_dalphat = tmp;

		if (sq > 0) {
			zeroAlpha(0, sq - 1);
		}

		int Nq = (sq == 0) ? 0 : m_hmms[m_nindicesOfPhysicalHMMs[sq - 1]]
				.getNumberOfStates();

		for (int q = sq; q <= eq; q++) {
			int lNq = Nq;
			SetOfSharedContinuousHMMs.PhysicalHMM hmm = m_hmms[m_nindicesOfPhysicalHMMs[q]];
			Nq = hmm.getNumberOfStates();
			float[][] transP = hmm.getTransitionMatrix().getMatrixInLogDomain();

			double[] aq = m_dalphat[q];
			double[] laq = m_dalphat1[q];
			if (laq == null) {
				End.throwError("StepAlpha: laq gone wrong!");
			}
			float[] outprob = m_flogOutputProbabilities[t][q
					- m_nfirstHMMInOutputProbabilityMatrix[t]];
			if (outprob == null) {
				End.throwError("StepAlpha: Outprob NULL at time " + t
						+ " model " + q + " in StepAlpha");
			}
			if (q == 0) {
				aq[0] = LogDomainCalculator.m_fLOG_DOMAIN_ZERO;
			} else {
				aq[0] = m_dalphat1[q - 1][lNq - 1];
				if (q > sq && a1N > LogDomainCalculator.m_fSMALL_NUMBER) {
					// tee Model
					aq[0] = LogDomainCalculator.add(aq[0], m_dalphat[q - 1][0]
							+ a1N);
				}
			}
			for (int j = 1; j < Nq - 1; j++) {
				a = transP[0][j];
				x = (a > LogDomainCalculator.m_fSMALL_NUMBER) ? a + aq[0]
						: LogDomainCalculator.m_fLOG_DOMAIN_ZERO;
				for (int i = 1; i < Nq - 1; i++) {
					a = transP[i][j];
					y = laq[i];
					if (a > LogDomainCalculator.m_fSMALL_NUMBER
							&& y > LogDomainCalculator.m_fSMALL_NUMBER) {
						x = LogDomainCalculator.add(x, y + a);
					}
				}
				// -1 because first state is non-emitting
				aq[j] = x + outprob[j - 1];
			}
			x = LogDomainCalculator.m_fLOG_DOMAIN_ZERO;
			for (int i = 1; i < Nq - 1; i++) {
				a = transP[i][Nq - 1];
				y = aq[i];
				if (a > LogDomainCalculator.m_fSMALL_NUMBER
						&& y > LogDomainCalculator.m_fSMALL_NUMBER) {
					x = LogDomainCalculator.add(x, y + a);
				}
			}
			aq[Nq - 1] = x;
			a1N = transP[0][Nq - 1];
		}
		if (eq < Q - 1) {
			zeroAlpha(eq + 1, Q - 1);
		}

		if (t == T - 1) {
			if (Math.abs((x - pr) / T) > 0.001) {
				End.throwError("StepAlpha: Forward = " + x + " /Backward = "
						+ pr + " disagree.");
			} else {
				if (m_nverbose > 1) {
					Print.dialog("StepAlpha: Forward = " + x + " /Backward = "
							+ pr + " agree.");
				}
			}
		}

		startAndEnd[0] = sq;
		startAndEnd[1] = eq;
	}

	protected void resetValuesOfOutputProbabilities() {
		for (int i = 0; i < m_setOfSharedContinuousHMMsBeingReestimated.m_mixturesOfGaussianPDFsBeingReestimated.length; i++) {
			m_setOfSharedContinuousHMMsBeingReestimated.m_mixturesOfGaussianPDFsBeingReestimated[i]
					.resetTimeForWhichProbabilityIsValid();
		}
	}

	/**
	 * Calculate forward values and accumulate statistic
	 * 
	 * @param pattern
	 *            Description of Parameter
	 */
	protected void calculateForwardValuesAndAccumulateStatistic(Pattern pattern) {
		// int negs;
		double[] aqt;
		double[] aqt1;
		double[] bqt;
		double[] bqt1;
		double[] bq1t;
		SetOfSharedContinuousHMMs.PhysicalHMM hmm;

		int Q = m_nindicesOfPhysicalHMMs.length;

		allocateSpaceForAlphaMatrices(Q);

		int[] startAndEnd = new int[2];
		// initialize with two arbitrary numbers
		startAndEnd[0] = -1;
		startAndEnd[1] = -1;
		calculateForwardValuesForTimeEqualToZero(startAndEnd, Q);
		// retrieve the values stored in array startAndEnd
		int start = startAndEnd[0];
		int end = startAndEnd[1];

		// inc access counters
		for (int q = 0; q < Q; q++) {
			int nhMMIndex = m_nindicesOfPhysicalHMMs[q];
			// Print.dialog("nhMMIndex = " + nhMMIndex);
			m_setOfSharedContinuousHMMsBeingReestimated
					.incrementNumberOfTrainingExamplesOfGivenHMM(nhMMIndex);
		}

		int T = pattern.getNumOfFrames();
		for (int t = 0; t < T; t++) {

			float ot[] = pattern.getParametersOfGivenFrame(t);

			if (t > 0) {
				startAndEnd[0] = start;
				startAndEnd[1] = end;
				stepAlpha(t, startAndEnd, Q, T);
				start = startAndEnd[0];
				end = startAndEnd[1];
			}

			for (int q = start; q <= end; q++) {
				// increment accs for each active model

				// if (m_nindicesOfPhysicalHMMs[q] == 44) {
				// Print.dialog("HERE");
				// }
				hmm = m_hmms[m_nindicesOfPhysicalHMMs[q]];
				//if (m_oshouldOutputGammaMatrix) {
				//	System.err.println("HMM number = "
				//			+ m_nindicesOfPhysicalHMMs[q]);
				//}

				// float[][] transP =
				// hmm.getTransitionMatrix().getMatrixInLogDomain();
				// Nq = hmm.getNumberOfStates();
				aqt = m_dalphat[q];
				bqt = m_dbeta[t][q - m_nfirstHMMInBetaMatrix[t]];

				if (t != T - 1 && (q - m_nfirstHMMInBetaMatrix[t + 1]) >= 0) {
					bqt1 = m_dbeta[t + 1][q - m_nfirstHMMInBetaMatrix[t + 1]];
					// Print.dialog("t = " + t + " q = " + q + "
					// m_nfirstHMMInBetaMatrix[t+1] = " +
					// m_nfirstHMMInBetaMatrix[t+1]);
				} else {
					bqt1 = null;
				}

				// bqt1 = (t == T - 1) ? null : m_dbeta[t +
				// 1][q-m_nfirstHMMInBetaMatrix[t+1]];
				aqt1 = (t == 0) ? null : m_dalphat1[q];

				if (q != Q - 1 && (q + 1 - m_nfirstHMMInBetaMatrix[t]) >= 0) {
					bq1t = m_dbeta[t][q + 1 - m_nfirstHMMInBetaMatrix[t]];
				} else {
					bq1t = null;
				}
				// bq1t = (q == Q - 1) ? null : m_dbeta[t][q +
				// 1-m_nfirstHMMInBetaMatrix[t]];
				calculateOccupationCountForCurrentTimeAndHMM(hmm, aqt, bqt,
						bq1t, t);
				
				if (m_oshouldOutputGammaMatrix) {
					//AK
					int Nq = hmm.getNumberOfStates();
					//q is the q-th HMM in this utterance, need to find the 
					//corresponding index of the HMM
					int nhmmIndex = m_nindicesOfPhysicalHMMs[q];
					for (int i = 0; i < Nq; i++) {
						m_dgamma[t][nhmmIndex][i] = m_foccupationCountForCurrentTime[i]; 
					}
				}

				// accumulate the statistics
				if (m_oshouldUpdateCovariance || m_oshouldUpdateMean
						|| m_oshouldUpdateWeights) {
					upMixParms(q, hmm, ot, t, aqt, aqt1, bqt,
							m_dlogProbabilityOfCurrentUtterance);
				}

				if (m_oshouldUpdateTransitionMatrix) {
					upTranParms(hmm, t, q, aqt, bqt, bqt1, bq1t,
							m_dlogProbabilityOfCurrentUtterance);
				}
			}
		}
	}

	/**
	 * Allocate and calculate beta and otprob matrices
	 * 
	 * @param pattern
	 *            input parameters
	 * @return log probability of current utterance
	 */
	private double allocateAndCalculateBackwardAndOutputProbabilities(
			Pattern pattern) {

		int Nq;
		int lNq = 0;
		int startq;
		double bqt[] = null;
		double bqt1[];
		double bq1t1[];
		float[] outprob;
		double x;
		double y;
		double lMax;
		double a;
		double a1N = 0.0;

		PruningInformation p = m_pruningInformation;
		SetOfSharedContinuousHMMs.PhysicalHMM hmm;

		// # of streams (utt->S)
		// S = 1;
		int Q = getNumberOfHMMsInCurrentUtterance();
		int T = pattern.getNumOfFrames();

		// allocate space
		m_flogOutputProbabilities = new float[T][][];
		m_nfirstHMMInOutputProbabilityMatrix = new int[T];

		m_dbeta = new double[T][][];
		m_nfirstHMMInBetaMatrix = new int[T];
		
		if (m_oshouldOutputGammaMatrix) {
			//allocate space for m_dgamma
			//use always the total number of HMMs
			int nnumberOfHMMs = m_hmms.length;
			m_dgamma = new double[T][nnumberOfHMMs][];
			for (int q = 0; q < nnumberOfHMMs; q++) {
				//hmm = m_hmms[m_nindicesOfPhysicalHMMs[q]];
				Nq = m_hmms[q].getNumberOfStates();
				for (int t = 0; t < T; t++) {
					m_dgamma[t][q] = new double[Nq];
				}
			}
		}

		// for calculating beam width
		double[] maxP = new double[Q];

		// Last Column t = T-1
		p.m_sqHi[T - 1] = (short) (Q - 1);
		int endq = p.m_sqLo[T - 1];

		calculateOutputProbabilities(pattern.getParametersOfGivenFrame(T - 1),
				T - 1, Q - 1, endq);
		m_dbeta[T - 1] = createBetaQ(endq, Q - 1, Q);
		m_nfirstHMMInBetaMatrix[T - 1] = endq;

		double gMax = LogDomainCalculator.m_fLOG_DOMAIN_ZERO;
		// max value of beta at time T-1
		// int q_at_gMax = 0;
		for (int q = Q - 1; q >= endq; q--) {
			hmm = m_hmms[m_nindicesOfPhysicalHMMs[q]];
			float[][] transP = hmm.getTransitionMatrix().getMatrixInLogDomain();
			Nq = hmm.getNumberOfStates();

			// Print.dialog("q = " + q + " Nq = " + Nq + " " + beta.length + " "
			// + beta[T-1].length);
			bqt = m_dbeta[T - 1][q - endq] = new double[Nq];
			bqt[Nq - 1] = (q == Q - 1) ? 0.0
					: m_dbeta[T - 1][q + 1 - endq][lNq - 1] + a1N;
			for (int i = 1; i < Nq - 1; i++) {
				bqt[i] = transP[i][Nq - 1] + bqt[Nq - 1];
			}
			outprob = m_flogOutputProbabilities[T - 1][q
					- m_nfirstHMMInOutputProbabilityMatrix[T - 1]];
			x = LogDomainCalculator.m_fLOG_DOMAIN_ZERO;
			for (int j = 1; j < Nq - 1; j++) {
				a = transP[0][j];
				y = bqt[j];
				if (a > LogDomainCalculator.m_fSMALL_NUMBER
						&& y > LogDomainCalculator.m_fSMALL_NUMBER) {
					// j-1 because first state is non-emitting
					x = LogDomainCalculator.add(x, a + outprob[j - 1] + y);
				}
			}
			bqt[0] = x;
			lNq = Nq;
			a1N = transP[0][Nq - 1];
			if (x > gMax) {
				gMax = x;
				// q_at_gMax = q;
			}
		}
		// Columns T-2 -> 0
		for (int t = T - 2; t >= 0; t--) {

			gMax = LogDomainCalculator.m_fLOG_DOMAIN_ZERO;
			// max value of beta at time t
			// q_at_gMax = 0;
			startq = p.m_sqHi[t + 1];
			endq = (p.m_sqLo[t + 1] == 0) ? 0
					: ((p.m_sqLo[t] >= p.m_sqLo[t + 1]) ? p.m_sqLo[t]
							: p.m_sqLo[t + 1] - 1);
			while (endq > 0 && m_sminimumDurations[endq - 1] == 0) {
				endq--;
			}
			// start end-point at top of beta beam at t+1
			// unless this is outside the beam taper.
			// + 1 to allow for state q+1[1] -> q[N]
			// + 1 for each tee model preceding endq.
			calculateOutputProbabilities(pattern.getParametersOfGivenFrame(t),
					t, startq, endq);
			m_dbeta[t] = createBetaQ(endq, startq, Q);
			m_nfirstHMMInBetaMatrix[t] = endq;

			// Print.dialog("endq_t_plus_1 = " + endq_t_plus_1 + ", endq = " +
			// endq + ", startq = " + startq);
			// Print.dialog("#### " + t + ", beta[t].length = " +
			// beta[t].length);
			for (int q = startq; q >= endq; q--) {
				// max value of beta in model q
				lMax = LogDomainCalculator.m_fLOG_DOMAIN_ZERO;
				hmm = m_hmms[m_nindicesOfPhysicalHMMs[q]];
				float[][] transP = hmm.getTransitionMatrix()
						.getMatrixInLogDomain();
				Nq = hmm.getNumberOfStates();

				bqt = m_dbeta[t][q - endq] = new double[Nq];

				// Print.dialog("beta[t+1].length = " + beta[t+1].length);
				// Print.dialog("q = " + q + ", endq_t_plus_1 = " +
				// endq_t_plus_1);
				if (q - m_nfirstHMMInBetaMatrix[t + 1] >= 0) {
					bqt1 = m_dbeta[t + 1][q - m_nfirstHMMInBetaMatrix[t + 1]];
				} else {
					bqt1 = null;
				}
				if ((q + 1 - m_nfirstHMMInBetaMatrix[t + 1]) >= 0) {
					bq1t1 = (q == Q - 1) ? null : m_dbeta[t + 1][q + 1
							- m_nfirstHMMInBetaMatrix[t + 1]];
				} else {
					bq1t1 = null;
				}

				// outprob = ab->otprob[t+1][q];
				if (q - m_nfirstHMMInOutputProbabilityMatrix[t + 1] >= 0) {
					outprob = m_flogOutputProbabilities[t + 1][q
							- m_nfirstHMMInOutputProbabilityMatrix[t + 1]];
				} else {
					outprob = null;
				}

				// bqt[Nq] = (bq1t1==NULL)?LZERO:bq1t1[1];
				bqt[Nq - 1] = (bq1t1 == null) ? LogDomainCalculator.m_fLOG_DOMAIN_ZERO
						: bq1t1[0];
				if (q < startq && a1N > LogDomainCalculator.m_fSMALL_NUMBER) {
					// 1Nq or 1Nq - 1
					bqt[Nq - 1] = LogDomainCalculator.add(bqt[Nq - 1],
							m_dbeta[t][q + 1 - endq][lNq - 1] + a1N);
				}
				for (int i = Nq - 2; i > 0; i--) {
					x = transP[i][Nq - 1] + bqt[Nq - 1];
					if (q >= p.m_sqLo[t + 1] && q <= p.m_sqHi[t + 1]) {
						// Print.dialog(q + " " + p.m_sqLo[t + 1] + " " +
						// p.m_sqHi[t + 1]);
						for (int j = 1; j < Nq - 1; j++) {
							a = transP[i][j];
							y = bqt1[j];
							if (a > LogDomainCalculator.m_fSMALL_NUMBER
									&& y > LogDomainCalculator.m_fSMALL_NUMBER) {
								// j-1 because first state is non-emitting
								x = LogDomainCalculator.add(x, a
										+ outprob[j - 1] + y);
							}
						}
					}
					bqt[i] = x;
					if (x > lMax) {
						lMax = x;
					}
					if (x > gMax) {
						gMax = x;
						// q_at_gMax = q;
					}
				}
				outprob = m_flogOutputProbabilities[t][q
						- m_nfirstHMMInOutputProbabilityMatrix[t]];
				x = LogDomainCalculator.m_fLOG_DOMAIN_ZERO;
				for (int j = 1; j < Nq - 1; j++) {
					a = transP[0][j];
					y = bqt[j];
					if (a > LogDomainCalculator.m_fSMALL_NUMBER
							&& y > LogDomainCalculator.m_fSMALL_NUMBER) {
						// j-1 because first state is non-emitting
						x = LogDomainCalculator.add(x, a + outprob[j - 1] + y);
					}
				}

				bqt[0] = x;
				maxP[q] = lMax;
				lNq = Nq;
				a1N = transP[0][Nq - 1];
			}

			while (gMax - maxP[startq] > p.m_flogPruningThreshold) {
				m_dbeta[t][startq - endq] = null;
				--startq;
				// lower startq till thresh reached
				if (startq < 0) {
					End.throwError("SetBeta: Beta prune failed sq < 0");
				}
			}
			while (p.m_sqHi[t] < startq) {
				// On taper
				m_dbeta[t][startq - endq] = null;
				--startq;
				// lower startq till thresh reached
				if (startq < 0) {
					End
							.throwError("SetBeta: Beta prune failed on taper sq < 0");
				}
			}
			p.m_sqHi[t] = (short) startq;
			while (gMax - maxP[endq] > p.m_flogPruningThreshold) {
				m_dbeta[t][endq - endq] = null;
				++endq;
				// raise endq till thresh reached
				if (endq > startq) {
					return (LogDomainCalculator.m_fLOG_DOMAIN_ZERO);
				}
			}
			p.m_sqLo[t] = (short) endq;
		}

		// Finally, set total prob
		double utt_pr = bqt[0];

		if (utt_pr <= LogDomainCalculator.m_fSMALL_NUMBER) {
			return LogDomainCalculator.m_fLOG_DOMAIN_ZERO;
		}

		if (m_nverbose > 1) {
			Print.dialog(" Utterance prob per frame = " + utt_pr / T);
		}

		if (m_nverbose > 3) {
			System.out.println("Beta matrix of embedded Baum-Welch");
			IO.DisplayMatrix(m_dbeta);
		}

		return utt_pr;
	}

	/**
	 * Calculate log output probability exploiting sharing.
	 * 
	 * @param inputParameters
	 *            parameters
	 * @param hmm
	 *            HMM model
	 * @param state
	 *            state of given HMM
	 * @param t
	 *            time
	 * @return log probability
	 */
	private float calculateLogOutputProbabilityOfGivenStateAtGivenTime(
			float[] inputParameters, SetOfSharedContinuousHMMs.PhysicalHMM hmm,
			int state, int t) {

		float x;
		int nmixtureIndex = hmm.getMixtureIndexOfGivenState(state);
		MixtureOfGaussianPDFsBeingReestimated mixtureOfGaussianPDFsBeingReestimated = m_setOfSharedContinuousHMMsBeingReestimated.m_mixturesOfGaussianPDFsBeingReestimated[nmixtureIndex];
		if (mixtureOfGaussianPDFsBeingReestimated.m_ntimeForWhichProbabilityIsValid == t) {
			// seen this state before
			x = mixtureOfGaussianPDFsBeingReestimated
					.getLastCalculatedLogProbability();
		} else {
			x = mixtureOfGaussianPDFsBeingReestimated.calculateLogProbability(
					inputParameters, t);
		}
		return x;
	}

	/**
	 * Calculate output probabilites at time t.
	 * 
	 * @param ot
	 *            input parameters
	 * @param t
	 *            time
	 * @param qHi
	 *            maximum HMM index in active range
	 * @param qLo
	 *            minimum HMM index in active range
	 */
	private void calculateOutputProbabilities(float[] ot, int t, int qHi,
			int qLo) {
		int q;
		int j;
		int Nq;
		// int s;
		float[] outprobj;
		float[][][] otprob;
		SetOfSharedContinuousHMMs.PhysicalHMM hmm;
		float x;
		// float sum;

		// PruningInformation p = m_pruningInformation;

		otprob = m_flogOutputProbabilities;
		if (qLo > 0) {
			--qLo;
		}
		otprob[t] = new float[qHi - qLo + 1][];

		// now, indicate the HMM # that will correspond to index 0
		m_nfirstHMMInOutputProbabilityMatrix[t] = qLo;

		if (m_nverbose > 4) {
			// if (m_oencaralhado) {
			Print.dialog("Output Probs at time " + (t + 1));
		}
		for (q = qHi; q >= qLo; q--) {
			hmm = m_hmms[m_nindicesOfPhysicalHMMs[q]];
			Nq = hmm.getNumberOfStates();
			if (otprob[t][q - qLo] == null) {
				otprob[t][q - qLo] = new float[Nq - 2];
				outprobj = otprob[t][q - qLo];
				for (j = 0; j < Nq - 2; j++) {
					x = calculateLogOutputProbabilityOfGivenStateAtGivenTime(
							ot, hmm, j, t);
					outprobj[j] = x;
				}
				if (m_nverbose > 4) {
					Print.putString("Q" + (q + 1) + ": ");
					for (int i = 0; i < m_flogOutputProbabilities[t][q - qLo].length; i++) {
						Print.putString(" " + (i + 2) + "\t"
								+ m_flogOutputProbabilities[t][q - qLo][i]);
					}
					Print.putString("\n");
				}
			} else {
				// XXX: never runs ?
				Print.dialog("AAAAAAAAAAAAAAAQUI");
			}
		}
	}

	/**
	 * Calculate backward values.
	 * 
	 * @param pattern
	 *            input parameters
	 * @return true if successful
	 */
	protected boolean calculateBackwardMatrix(Pattern pattern) {
		if (m_nverbose > 3) {		
			System.out.println("parameter with " + pattern.getNumOfFrames() + " frames");
		}
		//IO.DisplayMatrix(pattern.getParameters());

		/*
		 * int i; int j; int t; int q; int Nq; int lNq = 0; int q_at_gMax; int
		 * startq; double x; double y; double gMax; double lMax; double a;
		 * double a1N = 0.0;
		 */
		int T = pattern.getNumOfFrames();
		int Q = m_sminimumDurations.length;

		double lbeta = -1;
		PruningInformation pruneSetting = m_pruningInformation;
		double pruneThresh = pruneSetting.m_flogPruningThreshold;
		do {
			pruneSetting.setBeamTaper(m_sminimumDurations, Q, T);

			// for calculating beam width
			// double[] dmaxP = new double[Q];

			// Last Column t = T-1
			pruneSetting.m_sqHi[T - 1] = (short) Q;
			// int endq = pruneSetting.m_sqLo[T - 1];

			// calculateOutputProbabilities(pattern.getParametersOfGivenFrame()
			lbeta = allocateAndCalculateBackwardAndOutputProbabilities(pattern);

			// Print.dialog("lbeta = " + lbeta);
			if (lbeta > LogDomainCalculator.m_fSMALL_NUMBER) {
				break;
			} else if (pruneSetting.pruneInc == 0) {
				break;
			}
			pruneThresh += pruneSetting.pruneInc;
			// ak XXX logic is not right
			if (pruneThresh > pruneSetting.pruneLim
					|| pruneSetting.pruneInc == 0.0) {
				Print
						.warning("Path not found in beta pass. Bad data or over pruning.");
				Print.warning("Retrying Beta pass at " + pruneThresh);
			}
		} while (pruneThresh <= pruneSetting.pruneLim);

		if (lbeta < LogDomainCalculator.m_fSMALL_NUMBER) {
			System.err.println("WARNING: Beta prune error: "
					+ (++m_nnumberOfSentencesWithBetaPruningError) +
					" sentences with pruning error. This pattern has " + T + " frames.");
			return false;
		}

		m_dlogProbabilityOfCurrentUtterance = lbeta;
		return true;
	}

	/**
	 * Update the transition counters of given hmm
	 * 
	 * @param hmm
	 *            model
	 * @param t
	 *            time
	 * @param q
	 *            model index
	 * @param aqt
	 *            forward values for model q at time t
	 * @param bqt
	 *            backward values for model q at time t
	 * @param bqt1
	 *            backward values for model q at time t + 1
	 * @param bq1t
	 *            backward values for model q + 1 at time t
	 * @param pr
	 *            log probability of current utterance
	 */
	private void upTranParms(SetOfSharedContinuousHMMs.PhysicalHMM hmm, int t,
			int q, double[] aqt, double[] bqt, double[] bqt1, double[] bq1t,
			double pr) {
		int i;
		int j;
		int N;
		float[] ti;
		float[] ai;
		float[] outprob;
		float[] outprob1;
		// double sum;
		double x;

		N = hmm.getNumberOfStates();
		float[][] transP = hmm.getTransitionMatrix().getMatrixInLogDomain();

		// int nindexOfPhysicalHMM = m_nindicesOfPhysicalHMMs[q];
		int nindexOfTransitionMatrix = hmm.getTransitionMatrixIndex();
		TransitionMatrixAccumulator ta = m_setOfSharedContinuousHMMsBeingReestimated.m_transitionMatrixBeingReestimated[nindexOfTransitionMatrix].m_transitionMatrixAccumulator;

		// XXX in this case it doesn't seem necessary to check negative indices
		outprob = m_flogOutputProbabilities[t][q
				- m_nfirstHMMInOutputProbabilityMatrix[t]];
		if (bqt1 != null) {
			outprob1 = m_flogOutputProbabilities[t + 1][q
					- m_nfirstHMMInOutputProbabilityMatrix[t + 1]];
		} else {
			outprob1 = null;
		}
		for (i = 0; i < N - 1; i++) {
			ta.m_ftotalOccupationProbability[i] += m_foccupationCountForCurrentTime[i];
		}
		for (i = 0; i < N - 1; i++) {
			ti = ta.m_foccupationProbability[i];
			ai = transP[i];
			for (j = 1; j < N; j++) {
				if (i == 0 && j < N - 1) {
					// entry transition
					// subtract -1 because of non-emitting first state
					x = aqt[0] + ai[j] + outprob[j - 1] + bqt[j] - pr;
					if (x > LogDomainCalculator.m_fMINIMUM_EXP_ARGUMENT) {
						ti[j] += LogDomainCalculator.calculateExp(x);
					}
				} else if (i > 0 && j < N - 1 && bqt1 != null) {
					// internal transition
					// subtract -1 because of non-emitting first state
					x = aqt[i] + ai[j] + outprob1[j - 1] + bqt1[j] - pr;
					if (x > LogDomainCalculator.m_fMINIMUM_EXP_ARGUMENT) {
						ti[j] += LogDomainCalculator.calculateExp(x);
					}
				} else if (i > 0 && j == N - 1) {
					// exit transition
					x = aqt[i] + ai[N - 1] + bqt[N - 1] - pr;
					if (x > LogDomainCalculator.m_fMINIMUM_EXP_ARGUMENT) {
						ti[N - 1] += LogDomainCalculator.calculateExp(x);
					}
				}
				if (i == 0 && j == (N - 1)
						&& ai[N - 1] > LogDomainCalculator.m_fSMALL_NUMBER
						&& bq1t != null) {
					// tee transition
					x = aqt[0] + ai[N - 1] + bq1t[0] - pr;
					if (x > LogDomainCalculator.m_fMINIMUM_EXP_ARGUMENT) {
						ti[N - 1] += LogDomainCalculator.calculateExp(x);
					}
				}
			}
		}
	}

	/**
	 * Update mixtures accumulators of given hmm
	 * 
	 * @param q
	 *            model index
	 * @param hmm
	 *            model
	 * @param ot
	 *            parameters
	 * @param t
	 *            time
	 * @param aqt
	 *            forward values for model q at time t
	 * @param aqt1
	 *            forward values for model q at time t + 1
	 * @param bqt
	 *            backward values for model q at time t
	 * @param pr
	 *            log probability of current utterance
	 */
	private void upMixParms(int q, SetOfSharedContinuousHMMs.PhysicalHMM hmm,
			float[] ot, int t, double[] aqt, double[] aqt1, double[] bqt,
			double pr) {
		int M;
		int N;
		float a;
		double x;
		double initx = LogDomainCalculator.m_fLOG_DOMAIN_ZERO;
		float Lr;
		// TRUE if multiple mixture
		boolean mmix = false;
		float wght;

		N = hmm.getNumberOfStates();
		// int nhMMIndex = m_nindicesOfPhysicalHMMs[q];
		float[][] transP = hmm.getTransitionMatrix().getMatrixInLogDomain();
		int[] nmixtureIndices = hmm.getMixtureIndices();
		// dont' care number of frames here... ?
		PruningInformation pruningInformation = new PruningInformation(1,
				m_flogPruningThreshold);

		// int nhmmInQuestion = 10;
		// if (nhMMIndex == nhmmInQuestion) {
		// Print.dialog("HERE Mixture Weights at time " + (t+1) + ", model Q" +
		// (q+1));
		// if ( (t+1) == 207) {
		// t = t;
		// }
		// }

		x = -1;
		int maxM = m_setOfSharedContinuousHMMsBeingReestimated
				.getMaximumNumberOfGaussiansPerMixture();
		for (int j = 1; j < N - 1; j++) {
			if (maxM > 1) {
				// multiple Gaussians
				initx = transP[0][j] + aqt[0];
				if (t > 0) {
					for (int i = 1; i < N - 1; i++) {
						a = transP[i][j];
						if (a > LogDomainCalculator.m_fSMALL_NUMBER) {
							initx = LogDomainCalculator.add(initx, aqt1[i] + a);
						}
					}
				}
				initx += bqt[j] - pr;
			}

			// -1 because first state is non-emitting
			// float outprob = m_flogOutputProbabilities[t][q -
			// m_nfirstHMMInOutputProbabilityMatrix[t]][j - 1];

			MixtureOfGaussianPDFsBeingReestimated mixtureOfGaussianPDFsBeingReestimated = m_setOfSharedContinuousHMMsBeingReestimated.m_mixturesOfGaussianPDFsBeingReestimated[nmixtureIndices[j - 1]];
			M = mixtureOfGaussianPDFsBeingReestimated.getNumberOfGaussians();
			if (M > 1) {
				mmix = true;
			}
			float[] flogWeights = mixtureOfGaussianPDFsBeingReestimated.m_flogWeights;

			// process mixtures
			for (int m = 0; m < M; m++) {
				PDFBeingReestimated pDFBeingReestimated = mixtureOfGaussianPDFsBeingReestimated.m_pDFsBeingReestimated[m];
				wght = flogWeights[m];
				if (wght > MixtureOfGaussianPDFsBeingReestimated.m_fMINIMUM_LOG_WEIGHT) {
					// compute mixture likelihood if necessary
					if (!mmix) {
						// single mix
						x = aqt[j] + bqt[j] - pr;
					} else {
						x = initx + wght;
						float prob;
						if (pDFBeingReestimated
								.getTimeForWhichProbabilityIsValid() == t) {
							prob = pDFBeingReestimated
									.getStoredValueOfLogProbability();
						} else {
							prob = pDFBeingReestimated
									.calculateAndStoreLogProbability(ot, t);
						}
						x += prob;
					}
					if (-x < pruningInformation.m_fminFrwdP) {
						Lr = (float) LogDomainCalculator.calculateExp(x);

						// if (nhMMIndex == nhmmInQuestion) {
						// Print.dialog("pruningInformation.m_fminFrwdP " +
						// pruningInformation.m_fminFrwdP + " Lr = " + Lr);
						// Print.dialog("Lr = " + Lr);
						// }

						pDFBeingReestimated.subtractMean(ot,
								m_fauxiliaryZeroMean);

						// update weight counter
						if (m_oshouldUpdateWeights) {
							mixtureOfGaussianPDFsBeingReestimated
									.updateWeightStatistics(m, Lr);
						}
						// update mean counter
						if (m_oshouldUpdateMean) {
							mixtureOfGaussianPDFsBeingReestimated
									.updateMeanStatistics(m, Lr,
											m_fauxiliaryZeroMean);
						}

						// update covariance counter
						if (m_oshouldUpdateCovariance) {
							mixtureOfGaussianPDFsBeingReestimated
									.updateCovarianceStatistics(m, Lr,
											m_fauxiliaryZeroMean);
						}
					}
				}
			}
			// if (nhMMIndex == nhmmInQuestion) {
			// Print.dialog("total occ = " +
			// mixtureOfGaussianPDFsBeingReestimated.m_mixtureWeightsAccumulator.getTotalOccupationProbability());
			// }

		}
	}

	/**
	 * Reestimate transition matrix
	 * 
	 * @param hmm
	 *            Description of Parameter
	 * @param nindexOfPhysicalHMM
	 *            Description of Parameter
	 */
	private void reestimateTransitionMatrix(
			SetOfSharedContinuousHMMs.PhysicalHMM hmm, int nindexOfPhysicalHMM) {

		int nindexOfTransitionMatrix = hmm.getTransitionMatrixIndex();
		TransitionMatrixAccumulator transitionMatrixAccumulator = m_setOfSharedContinuousHMMsBeingReestimated.m_transitionMatrixBeingReestimated[nindexOfTransitionMatrix].m_transitionMatrixAccumulator;
		if (transitionMatrixAccumulator == null) {
			// already done...
			return;
		}
		if (m_nverbose > 3) {
			System.out.println("Transition matrix # " + nindexOfPhysicalHMM
					+ " before reestimation");
			IO.DisplayMatrix(hmm.getTransitionMatrix().getMatrix());
		}

		int nStates = hmm.getNumberOfStates();
		float[][] ftransitionMatrix = Cloner.clone(hmm.getTransitionMatrix()
				.getMatrixInLogDomain());

		for (int i = 0; i < nStates - 1; i++) {
			ftransitionMatrix[i][0] = LogDomainCalculator.m_fLOG_DOMAIN_ZERO;
			float occi = transitionMatrixAccumulator.m_ftotalOccupationProbability[i];
			if (occi == 0.0F) {
				String hMMName = m_setOfSharedContinuousHMMsBeingReestimated.m_tableOfLabels
						.getFirstLabel(nindexOfPhysicalHMM);
				// End.throwError("ContinuousHMMReestimator.reestimateTransitionMatrix()
				// HMM label: " +
				// hMMName + " zero state " + i + " occupation count.");
				Print
						.warning("ContinuousHMMReestimator.reestimateTransitionMatrix() HMM label: "
								+ hMMName
								+ " zero state "
								+ i
								+ " occupation count.");
			}
			float sum = 0.0F;
			for (int j = 1; j < nStates; j++) {
				float x = transitionMatrixAccumulator.m_foccupationProbability[i][j]
						/ occi;
				ftransitionMatrix[i][j] = x;
				sum += x;
			}
			for (int j = 1; j < nStates; j++) {
				float x = ftransitionMatrix[i][j] / sum;

				ftransitionMatrix[i][j] = LogDomainCalculator.calculateLog(x);

				// if (x < LogDomainCalculator.m_dMINIMUM_LOG_ARGUMENT) {
				// m_hmm.m_ftransitionMatrix[i][j] = (float)
				// LogDomainCalculator.m_fLOG_DOMAIN_ZERO;
				// } else {
				// m_hmm.m_ftransitionMatrix[i][j] = (float) Math.log(x);
				// }
			}

			// String label =
			// m_setOfSharedContinuousHMMsBeingReestimated.m_tableOfTransitionMatrices.getFirstLabel(nindexOfTransitionMatrix);
			// Print.dialog("transition " + label);
			// IO.DisplayMatrix(LogDomainCalculator.calculateExp(ftransitionMatrix));
		}

		// update
		try {
			m_setOfSharedContinuousHMMsBeingReestimated.m_transitionMatrixBeingReestimated[nindexOfTransitionMatrix] = new TransitionMatrixBeingReestimated(
					new TransitionMatrix(LogDomainCalculator
							.calculateExp(ftransitionMatrix)));
		} catch (Error e) {
			IO.DisplayMatrix(LogDomainCalculator
					.calculateExp(ftransitionMatrix));
			String hMMName = m_setOfSharedContinuousHMMsBeingReestimated.m_tableOfLabels
					.getFirstLabel(nindexOfPhysicalHMM);
			End
					.throwError("ContinuousHMMReestimator.reestimateTransitionMatrix() HMM label: "
							+ hMMName
							+ ", index= "
							+ nindexOfPhysicalHMM
							+ " has problem with matrix above");
		}

		// avoid re-calculating
		// transitionMatrixAccumulator = null;
		m_setOfSharedContinuousHMMsBeingReestimated.m_transitionMatrixBeingReestimated[nindexOfTransitionMatrix].m_transitionMatrixAccumulator = null;

		// arbitrarily set last row of transition matrix to
		// have a loop transition in last state with probability = 1
		// for (int j=0; j<nStates-1; j++) {
		// m_hmm.m_ftransitionMatrix[nStates-1][j] =
		// LogDomainCalculator.m_fLOG_DOMAIN_ZERO;
		// }
		// m_hmm.m_ftransitionMatrix[nStates-1][nStates-1] = 0.0F; //prob = 1

		if (m_nverbose > 3) {
			System.out.println("Transition matrix # " + nindexOfPhysicalHMM
					+ " after reestimation");
			IO
					.DisplayMatrix(m_setOfSharedContinuousHMMsBeingReestimated.m_transitionMatrixBeingReestimated[nindexOfTransitionMatrix]
							.getMatrix());
		}

	}

	/**
	 * Update all models.
	 */
	protected void reestimateModels() {
		SetOfSharedContinuousHMMs.PhysicalHMM hmm;
		int n;
		int maxM;
		// String str;

		// XXX should read from user
		int minEgs = m_nminimumNumberOfLabelOccurrencesForUpdatingModel;
		int nFloorVarMix = 0;
		int nFloorVar = 0;

		// can change ?
		maxM = m_setOfSharedContinuousHMMsBeingReestimated
				.getMaximumNumberOfGaussiansPerMixture();

		for (int px = 0; px < m_hmms.length; px++) {

			// if (px == 44) {
			// Print.dialog("HMM " + px + " = " +
			// m_setOfSharedContinuousHMMsBeingReestimated.m_tableOfLabels.getFirstLabel(px));
			// }

			hmm = m_hmms[px];
			n = m_setOfSharedContinuousHMMsBeingReestimated
					.getNumberOfTrainingExamplesOfGivenHMM(px);
			if (n < minEgs) {
				if (m_nverbose > 1) {
					Print
							.warning("UpdateModels: "
									+ m_setOfSharedContinuousHMMsBeingReestimated.m_tableOfLabels
											.getFirstLabel(px)
									+ "["
									+ px
									+ "] simply copied previous modell because only "
									+ n + " tokens were found.");
				}
			}

			if (n >= minEgs && n > 0) {
				if (m_oshouldUpdateTransitionMatrix) {
					reestimateTransitionMatrix(hmm, px);
				}
				if ((maxM > 1 && m_oshouldUpdateWeights)
						|| m_oshouldUpdateCovariance || m_oshouldUpdateMean) {
					reestimateMixtures(hmm, px);
				}
			}
		}

		// AK BUG XXX - the 2 variables below will never be different than zero!
		if (nFloorVar > 0) {
			Print.dialog(nFloorVar + " floored variance elements in "
					+ nFloorVarMix + " different mixes");
		}
	}

	/**
	 * Reestimate mixtures *@param hmm model
	 * 
	 * @param nindexOfPhysicalHMM
	 *            model index
	 */
	private void reestimateMixtures(SetOfSharedContinuousHMMs.PhysicalHMM hmm,
			int nindexOfPhysicalHMM) {

		float fcovarianceFloor = m_fcovarianceFloor;

		int[] nmixtureIndices = hmm.getMixtureIndices();
		for (int i = 0; i < nmixtureIndices.length; i++) {
			MixtureOfGaussianPDFsBeingReestimated mixtureOfGaussianPDFsBeingReestimated = m_setOfSharedContinuousHMMsBeingReestimated.m_mixturesOfGaussianPDFsBeingReestimated[nmixtureIndices[i]];

			if (mixtureOfGaussianPDFsBeingReestimated.wasReestimated()) {
				// already done this mixture...
				continue;
			}

			try {
				// i+2 below because it's just to print out results and
				// I want to count from state 1, 2, 3...
				mixtureOfGaussianPDFsBeingReestimated.reestimateMixture(i + 2,
						m_oshouldUpdateMean, m_oshouldUpdateCovariance,
						m_oshouldUpdateWeights, fcovarianceFloor,
						m_fmixtureWeightFloor);
			} catch (Error e) {
				Print.dialog(mixtureOfGaussianPDFsBeingReestimated
						.toStringAsInHTK());
				Print.error("Problem reestimating mixture number " + i
						+ " from HMM number " + nindexOfPhysicalHMM
						+ "  with label "
						+ m_tableOfHMMs.getLabels(nindexOfPhysicalHMM));
				End.exit();
			}

			mixtureOfGaussianPDFsBeingReestimated
					.setFlagThatIndicatesIfAlreadyReestimated(true);

			// if (nindexOfPhysicalHMM == 44) {
			// Print.dialog(mixtureOfGaussianPDFsBeingReestimated.getMixtureOfGaussianPDFs().toStringAsInHTK());
			// }
		}
	}

	protected void generateOccupationStatistics(String outputFileName) {
		FileNamesAndDirectories
				.createDirectoriesIfNecessaryGivenFileName(outputFileName);
		try {
			BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(
					outputFileName));
			SetOfSharedContinuousHMMs.PhysicalHMM hmm = null;
			for (int i = 0; i < m_hmms.length; i++) {
				String label = m_tableOfHMMs.getFirstLabel(i);
				int ntotalOccupation = m_setOfSharedContinuousHMMsBeingReestimated
						.getNumberOfTrainingExamplesOfGivenHMM(i);
				StringBuffer stringBuffer = new StringBuffer((i + 1) + " \""
						+ label + "\" " + ntotalOccupation);

				hmm = m_hmms[i];
				int[] nmixtureIndices = hmm.getMixtureIndices();
				int nnumberOfStates = hmm.getNumberOfStates();

				for (int j = 1; j < nnumberOfStates - 1; j++) {
					MixtureOfGaussianPDFsBeingReestimated mixtureOfGaussianPDFsBeingReestimated = m_setOfSharedContinuousHMMsBeingReestimated.m_mixturesOfGaussianPDFsBeingReestimated[nmixtureIndices[j - 1]];
					float focc = mixtureOfGaussianPDFsBeingReestimated.m_mixtureWeightsAccumulator
							.getTotalOccupationProbability();
					stringBuffer.append(" " + focc);
				}
				String line = stringBuffer.toString();
				bufferedWriter.write(line);
				bufferedWriter.newLine();
			}
			bufferedWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
			Print.error("Writing " + outputFileName);
			End.exit();
		}
	}

	/**
	 * Embedded training
	 */
	private void reestimateSetOfHMMsUsingEmbeddedBaumWelch(
			PatternGenerator patternGenerator, String dataLocatorFileName,
			// String setOfHMMOutputFileName,
			String setOfPatternsInputDirectory,
			String outputOccupationStatisticsFileName) {
		// go through all training sequence using a DatabaseManager +
		// DataLocatorFile
		// with one SOP per sentence. For each DataLocator
		DatabaseManager databaseManager = new DatabaseManager(
				dataLocatorFileName);

		// Print.dialog(m_tableOfHMMs.toString());

		if (m_nverbose > 1) {
			Print.dialog("Pruning threshold = " + m_flogPruningThreshold);
			Print.dialog("# of physical HMMs = " + m_hmms.length);
		}

		int ntotalNumberOfFrames = 0;
		double dtotalLogProbability = 0.0;

		int nnumberOfUtterances = 0;
		while (databaseManager.isThereDataToRead()) {
			// while (nnumberOfUtterances < 9) {
			DataLocator dataLocator = databaseManager.getNextDataLocator();

			if (m_nverbose > 1) {
				Print.dialog("# " + nnumberOfUtterances + " "
						+ dataLocator.getFileName());
			}

			if (m_nverbose > 2) {
				Print.dialog(dataLocator.getAllLabelsAsOneString());
			}

			// convert WAV to SOP in order to read file
			String parametersFileName = dataLocator.getFileName();
			parametersFileName = FileNamesAndDirectories.substituteExtension(
					parametersFileName, SetOfPatterns.m_FILE_EXTENSION);

			if (m_ouseAbsolutePath) {
				// get the SOP file name for this wav file
				boolean oisTraining = true;
				parametersFileName = m_simulationFilesAndDirectories
						.convertWAVToSOPFileName(
								dataLocator.getFileName(),
								m_simulationFilesAndDirectories
										.getSentenceTrainParametersDataRootDirectory(),
								oisTraining);
			} else {
				parametersFileName = FileNamesAndDirectories
						.getFileNameFromPath(parametersFileName);
				parametersFileName = FileNamesAndDirectories
						.concatenateTwoPaths(setOfPatternsInputDirectory,
								parametersFileName);
			}

			// if using HTK files:
			// Pattern pattern =
			// HTKInterfacer.getPatternFromFile(parametersFileName,
			// patternGenerator);
			// now, using SOP file
			SetOfPatterns setOfPatterns = new SetOfPatterns(parametersFileName);
			// in this case it has only 1 Pattern per SetOfPatterns
			Pattern pattern = setOfPatterns.getPattern(0);

			int nnumberOfFrames = pattern.getNumOfFrames();

			// get sequence of HMMs
			getSequenceOfHMMsFromLabels(dataLocator);
			int nminimumTotalDuration = getMinimumDurationForAllHMMsAndSumThemUp();
			if (nminimumTotalDuration > nnumberOfFrames) {
				End
						.throwError(" Unable to traverse "
								+ nminimumTotalDuration
								+ " states in "
								+ nnumberOfFrames
								+ " frames.\nMaybe the cause is bad data or over pruning.");
			}

			// XXX can eventually initialize with values obtained from user
			m_pruningInformation = new PruningInformation(nnumberOfFrames,
					m_flogPruningThreshold);

			// reset pre-computations
			resetValuesOfOutputProbabilities();

			// do forward-backward
			if (calculateBackwardMatrix(pattern)) {
				calculateForwardValuesAndAccumulateStatistic(pattern);
			} else {
				//error
				System.err.println("Error calculating beta matrix for file " + parametersFileName);
			}

			// update totals
			dtotalLogProbability += m_dlogProbabilityOfCurrentUtterance;
			ntotalNumberOfFrames += nnumberOfFrames;
			nnumberOfUtterances++;
			
			if (m_oshouldOutputGammaMatrix) {
				IO.write3DMatrixtoBinFile(m_gammaOutputFileName, m_dgamma);
				//System.out.println("gamma values for utterance # " + nnumberOfUtterances);
				//m_dgamma = IO.read3DMatrixFromBinFile(m_gammaOutputFileName);
				//IO.DisplayMatrix(m_dgamma);
			}
		}
		databaseManager.finalizeDataReading();
		if (m_nnumberOfSentencesWithBetaPruningError > 0) {
			// line feed
			Print.dialog("");
		}

		// Print.dialog("Reestimation complete - average log prob per frame = "
		// + dtotalLogProbability / ntotalNumberOfFrames);
		// Print.dialog("nnumberOfUtterances = " + nnumberOfUtterances);

		generateOccupationStatistics(outputOccupationStatisticsFileName);

		// update models
		reestimateModels();

		// do not save models here
		// String outFileName = "besta.txt";
		// HTKInterfacer.writeSetOfSharedHMMs(getSetOfSharedContinuousHMMs(),
		// setOfHMMOutputFileName);
	}

	/**
	 * Information for pruning.
	 * 
	 * @author Aldebaro Klautau
	 * @created November 15, 2000
	 */
	public class PruningInformation {
		int m_nmaxBeamWidth = 0;

		float m_fmaxAlphaBeta = LogDomainCalculator.m_fLOG_DOMAIN_ZERO;

		// same as fminFrwdP ?
		float m_minAlphaBeta = 1.0F;

		float m_flogPruningThreshold;

		// initial value
		short[] m_sqLo;

		short[] m_sqHi;

		float pruneInc = 0F; // 100.0F;

		float pruneLim = 2000.0F;

		// not in log... ?
		float m_fminFrwdP = 10.0F;

		/**
		 * Constructor for the PruningInformation object
		 * 
		 * @param nnumberOfFrames
		 *            number of frames in current utterance
		 */
		public PruningInformation(int nnumberOfFrames,
				float flogPruningThreshold) {
			m_sqLo = new short[nnumberOfFrames];
			m_sqHi = new short[nnumberOfFrames];
			m_flogPruningThreshold = flogPruningThreshold;
		}

		/**
		 * Set beam start and end points according to the minimum duration of
		 * the models in the current sequence.
		 * 
		 * @param qDms
		 *            minimum durations
		 * @param Q
		 *            number of models
		 * @param T
		 *            number of frames
		 */
		void setBeamTaper(short[] qDms, int Q, int T) {

			// Print.dialog("Q = " + Q + ", T = " + T);
			// IO.DisplayVector(qDms);
			short q;
			int dq;
			int i;
			int t;
			// Set leading taper
			q = 0;
			dq = qDms[q];
			i = 0;
			for (t = 0; t < T; t++) {
				while (i == dq) {
					i = 0;
					if (q < Q - 1) {
						q++;
						dq = qDms[q];
					} else {
						dq = -1;
					}
				}
				m_sqHi[t] = q;
				i++;
			}
			q = (short) (Q - 1);
			dq = (short) qDms[q];
			i = 0;
			for (t = T - 1; t >= 0; t--) {
				while (i == dq) {
					i = 0;
					if (q > 0) {
						q--;
						dq = qDms[q];
					} else {
						dq = -1;
					}
				}
				m_sqLo[t] = q;
				i++;
			}
		}
	}

}
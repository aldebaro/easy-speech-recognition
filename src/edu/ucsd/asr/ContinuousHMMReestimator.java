package edu.ucsd.asr;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Re-estimates a continuous hidden-Markov models (HMM) using Baum-Welch
 * algorithm. It follows HTK implementation.
 * 
 * @author Aldebaro Klautau
 * @created November 5, 2001
 * @version 2.0 - October 25, 2000.
 */
public class ContinuousHMMReestimator {

	/**
	 * Level of text output (user feedback).
	 */
	private int m_nverbose;

	private String m_reportFileName;

	private ContinuousHMMBeingReestimated m_hmm;

	private HeaderProperties m_headerProperties;

	private boolean m_oshouldWriteReportFile;

	private boolean m_oshouldUpdateTransitionMatrix;

	private boolean m_oshouldUpdateMean;

	private boolean m_oshouldUpdateCovariance;

	private boolean m_oshouldUpdateWeights;
	
	private boolean m_oshouldOutputGammaMatrix;

	// 1) output probability: m_hmm.getNumberOfStates()-2 because there are 2
	// non-emitting states.
	private float[][] m_flogOutputProbabilities;

	// static Matrix **mixoutp; /* array[2..nStates-1][1..maxT][1..nStreams]
	// [1..maxMixes] of mixprob */
	private float[][][] m_flogProbabilityForEachGaussian;

	// 2) forward and backward matrices
	private double[][] m_dalpha;

	private double[][] m_dbeta;

	// occupation statistic for pattern (called segment in HTK) 'r'
	// static Vector occr; /* array[1..nStates-1] of occ count for cur time */
	private float[] m_foccupationProbabilityOfCurrentPattern;

	private float m_fmixtureWeightFloor;

	// static Vector zot; /* temp storage for zero mean obs vector */
	private float[] m_fauxiliaryZeroMean;

	private Pattern m_currentPattern;

	/**
	 * headerProperties can specify parameters for training
	 * 
	 * @param continuousHMM
	 *            Description of Parameter
	 * @param setOfPatterns
	 *            Description of Parameter
	 * @param setOfPatternsFileName
	 *            Description of Parameter
	 * @param headerProperties
	 *            Description of Parameter
	 * @param reestimationLogFileName
	 *            Description of Parameter
	 */
	public ContinuousHMMReestimator(ContinuousHMM continuousHMM,
			SetOfPatterns setOfPatterns, String setOfPatternsFileName,
			HeaderProperties headerProperties, String reestimationLogFileName) {

		if (reestimationLogFileName == null) {
			m_oshouldWriteReportFile = false;
		} else {
			m_oshouldWriteReportFile = true;
			m_reportFileName = reestimationLogFileName;
			FileNamesAndDirectories
					.createDirectoriesIfNecessaryGivenFileName(m_reportFileName);
		}

		m_hmm = new ContinuousHMMBeingReestimated(continuousHMM);
		m_headerProperties = headerProperties;
		interpretHeaderAndRunHMMReestimation(setOfPatterns,
				setOfPatternsFileName);
	}

	// end of method
	/**
	 * Retun a copy (cloning) of current (possibly after reestimation) HMM.
	 * 
	 * @return The ReestimatedHMM value
	 */
	public ContinuousHMM getReestimatedHMM() {

		// eliminate Gaussians that have small weight
		MixtureOfGaussianPDFs[] mixtureOfGaussianPDFs = m_hmm
				.getMixturesOfGaussianPDFs();
		int ntotalNumberOfDiscardedGaussians = 0;
		for (int i = 0; i < mixtureOfGaussianPDFs.length; i++) {
			try {
				mixtureOfGaussianPDFs[i].discardGaussianWithVerySmallWeights();
			} catch (ASRError e) {
				if (m_nverbose > 3) {
					Print.dialog(e.getMessage());
				}
				ntotalNumberOfDiscardedGaussians += e.getAuxiliaryValue();
			}
		}

		if ((ntotalNumberOfDiscardedGaussians > 0) && (m_nverbose > 1)) {
			Print.warning(ntotalNumberOfDiscardedGaussians
					+ " Gaussian(s) discarded because "
					+ "of weight smaller than "
					+ MixtureOfGaussianPDFs.m_fMINIMUM_WEIGHT);
		}

		if ((ntotalNumberOfDiscardedGaussians > 0) && m_oshouldWriteReportFile) {
			IO
					.appendStringToEndOfTextFile(
							m_reportFileName,
							ntotalNumberOfDiscardedGaussians
									+ " Gaussian(s) discarded because "
									+ "of weight smaller than "
									+ IO
											.format(MixtureOfGaussianPDFs.m_fMINIMUM_WEIGHT)
									+ IO.m_NEW_LINE);
		}

		return new ContinuousHMM(m_hmm.getTransitionMatrix(),
				mixtureOfGaussianPDFs, m_hmm.m_topology);
	}

	/**
	 * Description of the Method
	 * 
	 * @param setOfPatterns
	 *            Description of Parameter
	 * @param setOfPatternsFileName
	 *            Description of Parameter
	 */
	private void interpretHeaderAndRunHMMReestimation(
			SetOfPatterns setOfPatterns, String setOfPatternsFileName) {

		String property = m_headerProperties.getProperty(
				"TrainingManager.nverbose", "0");
		m_nverbose = (Integer.valueOf(property)).intValue();
		CheckValues.exitOnError(m_nverbose, 0, 10, "TrainingManager.nverbose");
		m_headerProperties.setProperty("TrainingManager.nverbose", property);

		// property =
		// m_headerProperties.getProperty("ContinuousHMMReestimator.oshouldWriteReportFile","true");
		// m_oshouldWriteReportFile =
		// (Boolean.valueOf(property)).booleanValue();
		// m_headerProperties.setProperty("ContinuousHMMReestimator.oshouldWriteReportFile",property);
		//
		// if (m_oshouldWriteReportFile) {
		// //find directory
		// String databaseRootDirectory =
		// m_headerProperties.getProperty("TrainingManager.DatabaseRootDirectory","./");
		// databaseRootDirectory =
		// FileNamesAndDirectories.forceEndingWithSlash(databaseRootDirectory);
		//
		// String directoryForHMMs =
		// m_headerProperties.getProperty("TrainingManager.DirectoryForHMMs",databaseRootDirectory);
		// directoryForHMMs =
		// FileNamesAndDirectories.forceEndingWithSlash(directoryForHMMs);
		//
		// property =
		// m_headerProperties.getProperty("ContinuousHMMReestimator.ReportFile","HMMReestimation.log");
		//
		// m_reportFileName = directoryForHMMs + property;
		// m_headerProperties.setProperty("ContinuousHMMReestimator.ReportFile",m_reportFileName);
		// }
		property = m_headerProperties.getProperty(
				"ContinuousHMMReestimator.oshouldUpdateTransitionMatrix",
				"true");
		m_oshouldUpdateTransitionMatrix = (Boolean.valueOf(property))
				.booleanValue();
		m_headerProperties.setProperty(
				"ContinuousHMMReestimator.oshouldUpdateTransitionMatrix",
				property);

		property = m_headerProperties.getProperty(
				"ContinuousHMMReestimator.oshouldUpdateMean", "true");
		m_oshouldUpdateMean = (Boolean.valueOf(property)).booleanValue();
		m_headerProperties.setProperty(
				"ContinuousHMMReestimator.oshouldUpdateMean", property);

		property = m_headerProperties.getProperty(
				"ContinuousHMMReestimator.oshouldUpdateCovariance", "true");
		m_oshouldUpdateCovariance = (Boolean.valueOf(property)).booleanValue();
		m_headerProperties.setProperty(
				"ContinuousHMMReestimator.oshouldUpdateCovariance", property);

		property = m_headerProperties.getProperty(
				"ContinuousHMMReestimator.oshouldUpdateWeights", "true");
		m_oshouldUpdateWeights = (Boolean.valueOf(property)).booleanValue();
		m_headerProperties.setProperty(
				"ContinuousHMMReestimator.oshouldUpdateWeights", property);

		property = m_headerProperties.getProperty(
				"ContinuousHMMReestimator.oshouldOutputGammaMatrix", "false");
		m_oshouldOutputGammaMatrix = (Boolean.valueOf(property)).booleanValue();
		m_headerProperties.setProperty(
				"ContinuousHMMReestimator.oshouldOutputGammaMatrix", property);
		
		property = m_headerProperties.getProperty(
				"ContinuousHMMReestimator.nmaximumIterations", "20");
		int nmaximumIterations = (Integer.valueOf(property)).intValue();
		CheckValues.exitOnError(nmaximumIterations, 1, 1000,
				"ContinuousHMMReestimator.nmaximumIterations");
		m_headerProperties.setProperty(
				"ContinuousHMMReestimator.nmaximumIterations", property);

		property = m_headerProperties.getProperty(
				"ContinuousHMMReestimator.fmixtureWeightFloor", "0.0");
		m_fmixtureWeightFloor = (Float.valueOf(property)).floatValue();
		CheckValues.exitOnError(m_fmixtureWeightFloor, 0.0F, 1.0F,
				"ContinuousHMMReestimator.fmixtureWeightFloor");
		m_headerProperties.setProperty(
				"ContinuousHMMReestimator.fmixtureWeightFloor", property);

		// default is based on number of emitting states:
		// String minimumNumberOfFramesForValidPattern =
		// String.valueOf(m_hmm.getNumberOfStates()-2);
		String minimumNumberOfFramesForValidPattern = "1";
		property = m_headerProperties
				.getProperty(
						"ContinuousHMMReestimator.nminimumNumberOfFramesForValidPattern",
						minimumNumberOfFramesForValidPattern);
		int nminimumNumberOfFramesForValidPattern = (Integer.valueOf(property))
				.intValue();
		CheckValues
				.exitOnError(nmaximumIterations, 1, 1000,
						"ContinuousHMMReestimator.nminimumNumberOfFramesForValidPattern");
		m_headerProperties
				.setProperty(
						"ContinuousHMMReestimator.nminimumNumberOfFramesForValidPattern",
						property);

		property = m_headerProperties.getProperty(
				"ContinuousHMMReestimator.fconvergenceThreshold", "1e-4");
		float fconvergenceThreshold = (Float.valueOf(property)).floatValue();
		CheckValues.exitOnError(fconvergenceThreshold, 0.0F, 1.0F,
				"ContinuousHMMReestimator.fconvergenceThreshold");
		m_headerProperties.setProperty(
				"ContinuousHMMReestimator.fconvergenceThreshold", property);

		property = m_headerProperties.getProperty(
				"ContinuousHMMReestimator.fcovarianceFloor", "1e-4");
		float fcovarianceFloor = (Float.valueOf(property)).floatValue();
		CheckValues.exitOnError(fcovarianceFloor, 0.0F, 1000.0F,
				"ContinuousHMMReestimator.fcovarianceFloor");
		m_headerProperties.setProperty(
				"ContinuousHMMReestimator.fcovarianceFloor", property);

		property = m_headerProperties.getProperty(
				"ContinuousHMMReestimator.nminimumNumberOfPatternsPerModel",
				"3");
		int nminimumNumberOfPatternsPerModel = (Integer.valueOf(property))
				.intValue();
		CheckValues.exitOnError(nminimumNumberOfPatternsPerModel, 0, 1000,
				"ContinuousHMMReestimator.nminimumNumberOfPatternsPerModel");
		m_headerProperties.setProperty(
				"ContinuousHMMReestimator.nminimumNumberOfPatternsPerModel",
				property);

		reestimateHMMUsingBaumWelch(setOfPatterns, setOfPatternsFileName,
				nmaximumIterations, nminimumNumberOfFramesForValidPattern,
				nminimumNumberOfPatternsPerModel, fconvergenceThreshold,
				fcovarianceFloor);
	}

	/**
	 * Forward probability ("all paths"): n0 is the observation sequence
	 * 
	 * @return Description of the Returned Value
	 */
	private double calculateForwardMatrix() {
		float[][] fobservation = m_currentPattern.getParameters();

		int nT = fobservation.length;
		int nnumberOfStates = m_hmm.getNumberOfStates();

		double x;
		double a;

		// According to page 150 of HTK manual, but subtracting
		// 1 from the indices because HTK manual equations and
		// HTK software assume vectors start with index 1
		m_dalpha[0][0] = 0.0F;
		for (int j = 1; j < nnumberOfStates - 1; j++) {
			// col 1 from entry state
			a = m_hmm.m_ftransitionMatrix[0][j];
			// hmm->transP[1][j];
			if (a < LogDomainCalculator.m_fSMALL_NUMBER) {
				m_dalpha[j][0] = LogDomainCalculator.m_fLOG_DOMAIN_ZERO;
			} else {
				// j-1 because first state is non-emitting
				m_dalpha[j][0] = a + m_flogOutputProbabilities[j - 1][0];
			}
		}

		// can't jump from the first to the second non-emmiting state
		m_dalpha[nnumberOfStates - 1][0] = LogDomainCalculator.m_fLOG_DOMAIN_ZERO;

		for (int t = 1; t < nT; t++) {
			// cols 2 to T
			for (int j = 1; j < nnumberOfStates - 1; j++) {
				x = LogDomainCalculator.m_fLOG_DOMAIN_ZERO;
				for (int i = 1; i < nnumberOfStates - 1; i++) {
					a = m_hmm.m_ftransitionMatrix[i][j];
					if (a > LogDomainCalculator.m_fSMALL_NUMBER) {
						// recursively accumulate summation in x
						x = LogDomainCalculator.add(x, m_dalpha[i][t - 1] + a);
					}
				}
				// j-1 because first state is non-emitting
				m_dalpha[j][t] = x + m_flogOutputProbabilities[j - 1][t];
			}
			m_dalpha[0][t] = LogDomainCalculator.m_fLOG_DOMAIN_ZERO;
			m_dalpha[nnumberOfStates - 1][t] = LogDomainCalculator.m_fLOG_DOMAIN_ZERO;
		}

		x = LogDomainCalculator.m_fLOG_DOMAIN_ZERO;
		// finally calc seg prob
		for (int i = 1; i < nnumberOfStates - 1; i++) {
			a = m_hmm.m_ftransitionMatrix[i][nnumberOfStates - 1];
			// a=hmm->transP[i][nStates];
			if (a > LogDomainCalculator.m_fSMALL_NUMBER) {
				// recursively accumulate summation in x
				x = LogDomainCalculator.add(x, m_dalpha[i][nT - 1] + a);
			}
		}
		m_dalpha[nnumberOfStates - 1][nT - 1] = x;

		return x;
	}

	/**
	 * Backward probability ("all paths"): d0 is the observation sequence
	 * 
	 * @return Description of the Returned Value
	 */
	private double calculateBackwardMatrix() {
		float[][] fobservation = m_currentPattern.getParameters();

		double x;
		double a;

		int nT = m_currentPattern.getNumOfFrames();
		int nnumberOfStates = m_hmm.getNumberOfStates();

		m_dbeta[nnumberOfStates - 1][nT - 1] = 0.0F;

		for (int i = 1; i < nnumberOfStates - 1; i++) {
			// Col T from exit state
			m_dbeta[i][nT - 1] = m_hmm.m_ftransitionMatrix[i][nnumberOfStates - 1];
		}
		m_dbeta[0][nT - 1] = LogDomainCalculator.m_fLOG_DOMAIN_ZERO;

		for (int t = nT - 2; t >= 0; t--) {
			// Col t from col t+1
			for (int i = 0; i < nnumberOfStates; i++) {
				m_dbeta[i][t] = LogDomainCalculator.m_fLOG_DOMAIN_ZERO;
			}
			for (int j = 1; j < nnumberOfStates - 1; j++) {
				// j-1 because first state is non-emitting
				x = m_flogOutputProbabilities[j - 1][t + 1] + m_dbeta[j][t + 1];
				for (int i = 1; i < nnumberOfStates - 1; i++) {
					a = m_hmm.m_ftransitionMatrix[i][j];
					if (a > LogDomainCalculator.m_fSMALL_NUMBER) {
						m_dbeta[i][t] = LogDomainCalculator.add(m_dbeta[i][t],
								x + a);
					}
				}
			}
		}

		x = LogDomainCalculator.m_fLOG_DOMAIN_ZERO;
		for (int j = 1; j < nnumberOfStates - 1; j++) {
			a = m_hmm.m_ftransitionMatrix[0][j];
			if (a > LogDomainCalculator.m_fSMALL_NUMBER) {
				// j-1 because first state is non-emitting
				x = LogDomainCalculator.add(x, m_dbeta[j][0] + a
						+ m_flogOutputProbabilities[j - 1][0]);
			}
		}
		m_dbeta[0][0] = x;
		return x;
	}

	// SetOccr: set the global occupation counters occr for current seg
	/**
	 * Description of the Method
	 * 
	 * @param dlogProbabilityOfCurrentPattern
	 *            Description of Parameter
	 */
	private void calculateStateOccupationProbabilityOfCurrentPattern(
			double dlogProbabilityOfCurrentPattern) {

		m_foccupationProbabilityOfCurrentPattern[0] = 1.0F;
		// not in log domain
		int nStates = m_hmm.getNumberOfStates();
		int nT = m_currentPattern.getNumOfFrames();

		// don't consider last state (non-emitting)
		for (int i = 1; i < nStates - 1; i++) {
			double[] alpha_i = m_dalpha[i];
			double[] beta_i = m_dbeta[i];
			//float[] a_i = m_hmm.m_ftransitionMatrix[i];

			// reset
			double dlogProb = LogDomainCalculator.m_fLOG_DOMAIN_ZERO;

			// add contribution of all frames to state i occupation prob
			for (int t = 0; t < nT; t++) {
				dlogProb = LogDomainCalculator.add(dlogProb, alpha_i[t]
						+ beta_i[t]);
			}

			// normalize (subtraction in log domain) by total probability of
			// current pattern
			dlogProb -= dlogProbabilityOfCurrentPattern;

			// convert from log to probability
			if (dlogProb > LogDomainCalculator.m_fMINIMUM_EXP_ARGUMENT) {
				m_foccupationProbabilityOfCurrentPattern[i] = (float) LogDomainCalculator
						.calculateExp(dlogProb);
			} else {
				m_foccupationProbabilityOfCurrentPattern[i] = 0.0F;
			}
		}

		if (m_nverbose > 2) {
			Print
					.dialogOnlyToConsole("OCC (m_foccupationProbabilityOfCurrentPattern)");
			IO
					.displayPartOfVector(
							m_foccupationProbabilityOfCurrentPattern, 10);
		}
	}

	/**
	 * Write gamma = alpha * beta matrix (see Rabiner 89 paper)
	 * @param dlogProbabilityOfCurrentPattern
	 */
	private void outputGammaMatrix(double dlogProbabilityOfCurrentPattern) {

		// not in log domain
		int nStates = m_hmm.getNumberOfStates();
		int nT = m_currentPattern.getNumOfFrames();

		System.err.println("############################");
		System.err.println(nT + " frames");
		// don't consider last state (non-emitting)
		for (int i = 1; i < nStates - 1; i++) {
			double[] alpha_i = m_dalpha[i];
			double[] beta_i = m_dbeta[i];

			for (int t = 0; t < nT; t++) {
				// normalize (subtraction in log domain) by total probability of
				// current pattern
				double dgamma = alpha_i[t] + beta_i[t] - dlogProbabilityOfCurrentPattern;
				// convert from log to probability
				if (dgamma > LogDomainCalculator.m_fMINIMUM_EXP_ARGUMENT) {
					dgamma = LogDomainCalculator.calculateExp(dgamma);
				} else {
					dgamma = 0;
				}
				System.err.print(dgamma + " ");
			}
			System.err.println("");
		}
		System.err.println("############################");
	}
	
	
	/**
	 * UpTranCounts: update the transition counters in ta
	 * 
	 * @param dlogProbabilityOfCurrentPattern
	 *            Description of Parameter
	 */
	// dlogProbability is the total log probability of this sequence (pattern)
	private void updateTransitionMatrixAccumulators(
			double dlogProbabilityOfCurrentPattern) {

		int nStates = m_hmm.getNumberOfStates();
		int nT = m_currentPattern.getNumOfFrames();

		// Print.dialog("AQUI: BEFORE TRAN");
		// IO.displayPartOfMatrix(m_hmm.m_transitionMatrixAccumulator.m_foccupationProbability,10);
		// update global state occupation estimation for emitting states
		for (int i = 1; i < nStates - 1; i++) {
			m_hmm.m_transitionMatrixAccumulator.m_ftotalOccupationProbability[i] += m_foccupationProbabilityOfCurrentPattern[i];
		}

		// transitions from first state:
		// 0->j , where 0<j<nStates-1
		// loop transition (0,0) and skipping all emitting states
		// (0,nStates-1) are not allowed and will be set "0" later on
		for (int j = 1; j < nStates - 1; j++) {
			double dlogA_ij = m_hmm.m_ftransitionMatrix[0][j];

			if (dlogA_ij > LogDomainCalculator.m_fSMALL_NUMBER) {

				// subtract -1 in m_flogOutputProbabilities index because of
				// non-emitting first state
				double dlogProb = dlogA_ij
						+ m_flogOutputProbabilities[j - 1][0] + m_dbeta[j][0]
						- dlogProbabilityOfCurrentPattern;

				// Print.dialog(dlogA_ij + " " +
				// m_flogOutputProbabilities[j-1][0] + " " + " " + m_dbeta[j][0]
				// + " " + dlogProbabilityOfCurrentPattern);
				if (dlogProb > LogDomainCalculator.m_fMINIMUM_EXP_ARGUMENT) {
					double y = LogDomainCalculator.calculateExp(dlogProb);
					m_hmm.m_transitionMatrixAccumulator.m_foccupationProbability[0][j] += y;

					// Print.dialog(dlogProb + " " + y);
					// notice m_hmm.m_fstateOccupation[0] wasn't updated with
					// m_foccupationProbabilityOfCurrentPattern above
					m_hmm.m_transitionMatrixAccumulator.m_ftotalOccupationProbability[0] += y;
				}

			}

			// Print.dialog("AQUI: state j = " + j);
			// IO.displayPartOfMatrix(m_hmm.m_transitionMatrixAccumulator.m_foccupationProbability,10);
		}

		// middle emitting states
		// transitions i->j 0<i,j<nStates-1
		for (int i = 1; i < nStates - 1; i++) {
			float[] a_i = m_hmm.m_ftransitionMatrix[i];
			double[] alpha_i = m_dalpha[i];
			float[] tran_i = m_hmm.m_transitionMatrixAccumulator.m_foccupationProbability[i];
			for (int j = 1; j < nStates - 1; j++) {
				double a_ij = a_i[j];
				if (a_ij > LogDomainCalculator.m_fSMALL_NUMBER) {
					double x = LogDomainCalculator.m_fLOG_DOMAIN_ZERO;
					double[] beta_j = m_dbeta[j];

					// subtract -1 in m_flogOutputProbabilities index because of
					// non-emitting first state
					float[] outprob_j = m_flogOutputProbabilities[j - 1];

					for (int t = 0; t < nT - 1; t++) {
						x = LogDomainCalculator.add(x, alpha_i[t] + a_ij
								+ outprob_j[t + 1] + beta_j[t + 1]);
					}
					x -= dlogProbabilityOfCurrentPattern;
					if (x > LogDomainCalculator.m_fMINIMUM_EXP_ARGUMENT) {
						tran_i[j] += LogDomainCalculator.calculateExp(x);
					}
				}
			}
		}

		// last state: non-emitting
		// transitions i->nStates-1, where 0<i<nStates-1
		// loop transition (0,0) and skipping all emitting states
		// (0,nStates-1) are not allowed and will be set "0" later on
		for (int i = 1; i < nStates - 1; i++) {
			float a_ij = m_hmm.m_ftransitionMatrix[i][nStates - 1];
			if (a_ij > LogDomainCalculator.m_fSMALL_NUMBER) {
				double x = a_ij + m_dalpha[i][nT - 1]
						- dlogProbabilityOfCurrentPattern;
				if (x > LogDomainCalculator.m_fMINIMUM_EXP_ARGUMENT) {
					m_hmm.m_transitionMatrixAccumulator.m_foccupationProbability[i][nStates - 1] += LogDomainCalculator
							.calculateExp(x);
				}
			}
		}
	}

	/*
	 * UpStreamCounts: update mean, cov & mixweight counts for given stream Ak:
	 * j is the state number and s is the stream number.
	 */
	// private void UpStreamCounts(int j, int s, StreamElem *se, int vSize,
	// Logfloat pr, int seg,DVector alphj, DVector betaj) {
	// here I don't have streams, so I will call it, updatePDFCounts and merge
	// with UpPDFCounts
	/*
	 * UpPDFCounts: update output PDF counts for each stream of each state
	 */
	// TODO: should move it to objects
	/**
	 * Description of the Method
	 * 
	 * @param dlogProbabilityOfCurrentPattern
	 *            Description of Parameter
	 */
	private void updatePDFAccumulators(double dlogProbabilityOfCurrentPattern) {

		// float[][] fobservation = m_currentPattern.getParameters();
		int nStates = m_hmm.getNumberOfStates();
		int nT = m_currentPattern.getNumOfFrames();

		// do it just for emitting states
		for (int j = 1; j < nStates - 1; j++) {
			double[] alphj = m_dalpha[j];
			double[] betaj = m_dbeta[j];

			// subtract 1 because first state is non-emitting
			float[][] mixp_j = m_flogProbabilityForEachGaussian[j - 1];

			// subtract 1 because first state is non-emitting
			MixtureOfGaussianPDFsBeingReestimated mixtureOfGaussianPDFsBeingReestimated = m_hmm.m_mixturesOfGaussianPDFsBeingReestimated[j - 1];

			float[] flogWeights = mixtureOfGaussianPDFsBeingReestimated.m_flogWeights;

			for (int m = 0; m < mixtureOfGaussianPDFsBeingReestimated
					.getNumberOfGaussians(); m++) {
				PDFBeingReestimated pDFBeingReestimated = mixtureOfGaussianPDFsBeingReestimated.m_pDFsBeingReestimated[m];
				if (flogWeights[m] > MixtureOfGaussianPDFsBeingReestimated.m_fMINIMUM_LOG_WEIGHT) {
					for (int t = 0; t < nT; t++) {
						float[] fobservation = m_currentPattern
								.getParametersOfGivenFrame(t);
						// m_fauxiliaryZeroMean =
						// pDFBeingReestimated.subtractMean(fobservation);
						pDFBeingReestimated.subtractMean(fobservation,
								m_fauxiliaryZeroMean);
						double Lr;
						if (mixtureOfGaussianPDFsBeingReestimated
								.getNumberOfGaussians() == 1) {
							Lr = alphj[t] + betaj[t]
									- dlogProbabilityOfCurrentPattern;
						} else {
							if (t == 0) {
								Lr = m_hmm.getTransitionMatrixElement(0, j);
							} else {
								Lr = LogDomainCalculator.m_fLOG_DOMAIN_ZERO;
								for (int i = 1; i < nStates - 1; i++) {
									float a_ij = m_hmm
											.getTransitionMatrixElement(i, j);
									if (a_ij > LogDomainCalculator.m_fSMALL_NUMBER) {
										Lr = LogDomainCalculator.add(Lr,
												m_dalpha[i][t - 1] + a_ij);
									}
								}
							}
							if (Lr > LogDomainCalculator.m_fSMALL_NUMBER) {
								Lr += mixp_j[t][m] + flogWeights[m] + betaj[t]
										- dlogProbabilityOfCurrentPattern;
							}
							// Print.dialog(j +" "+ m+" "+t+" "+Lr +" "+
							// mixp_j[t][m] +" "+ flogWeights[m] +" "+ betaj[t]
							// +" "+ dlogProbabilityOfCurrentPattern);
						}

						if (Lr > LogDomainCalculator.m_fMINIMUM_EXP_ARGUMENT) {
							float y = (float) LogDomainCalculator
									.calculateExp(Lr);

							// update weight counter
							if (m_oshouldUpdateWeights) {
								m_hmm.updateWeightStatistics(j, m, y);
							}
							// update mean counter
							if (m_oshouldUpdateMean) {
								m_hmm.updateMeanStatistics(j, m, y,
										m_fauxiliaryZeroMean);
							}

							// update covariance counter
							if (m_oshouldUpdateCovariance) {
								m_hmm.updateCovarianceStatistics(j, m, y,
										m_fauxiliaryZeroMean);
							}

						}
					}
				}
			}
		}
	}

	/**
	 * Update the various accumulators.
	 * 
	 * @param dlogProbabilityOfCurrentPattern
	 *            Description of Parameter
	 */
	private void UpdateAccumulators(double dlogProbabilityOfCurrentPattern) {

		calculateStateOccupationProbabilityOfCurrentPattern(dlogProbabilityOfCurrentPattern);

		if (m_oshouldUpdateTransitionMatrix) {
			updateTransitionMatrixAccumulators(dlogProbabilityOfCurrentPattern);
		}
		if (m_oshouldUpdateMean || m_oshouldUpdateCovariance
				|| m_oshouldUpdateWeights) {
			updatePDFAccumulators(dlogProbabilityOfCurrentPattern);
		}
	}

	/**
	 * Description of the Method
	 */
	private void showAccumulators() {

		if (m_nverbose > 3) {

			Print.dialog("OCC");
			IO
					.displayPartOfVector(
							m_foccupationProbabilityOfCurrentPattern, 10);

			Print.dialog("TRAN");
			IO
					.displayPartOfMatrix(
							m_hmm.m_transitionMatrixAccumulator.m_foccupationProbability,
							10);

			Print.dialog("TOCC");
			IO
					.displayPartOfVector(
							m_hmm.m_transitionMatrixAccumulator.m_ftotalOccupationProbability,
							10);

			int nStates = m_hmm.getNumberOfStates();

			// do it just for emitting states
			for (int j = 1; j < nStates - 1; j++) {
				// I count 0,1,... and HTK counts 1,2,..., so j+1
				m_hmm.m_mixturesOfGaussianPDFsBeingReestimated[j - 1]
						.showAccumulators(j + 1);
			}
		}
	}

	// XXX move it to SOP class
	/**
	 * Description of the Method
	 * 
	 * @param fileName
	 *            Description of Parameter
	 * @param setOfPatterns
	 *            Description of Parameter
	 * @param nminimumNumberOfPatternsPerModel
	 *            Description of Parameter
	 * @param nminimumNumberOfFramesForValidPattern
	 *            Description of Parameter
	 * @return Description of the Returned Value
	 */
	private boolean checkSetOfPatterns(String fileName,
			SetOfPatterns setOfPatterns, int nminimumNumberOfPatternsPerModel,
			int nminimumNumberOfFramesForValidPattern) {
		if (setOfPatterns.getNumberOfPatterns() < nminimumNumberOfPatternsPerModel) {
			Print.error("SetOfPatterns has just "
					+ setOfPatterns.getNumberOfPatterns()
					+ " patterns (minimum = "
					+ nminimumNumberOfPatternsPerModel + ").");
			return false;
			// End.exit();
		}

		if (m_nverbose > 1) {
			int nnumberOfValidPatterns = 0;
			int nnumberOfTooShortPatterns = 0;
			int numberOfPatterns = setOfPatterns.getNumberOfPatterns();
			for (int i = 0; i < numberOfPatterns; i++) {
				int nobservationLength = setOfPatterns.getPattern(i)
						.getNumOfFrames();
				if (nobservationLength < nminimumNumberOfFramesForValidPattern) {
					nnumberOfTooShortPatterns++;
				} else {
					nnumberOfValidPatterns++;
				}
			}
			double dskipped = 10000.0 * ((double) nnumberOfTooShortPatterns)
					/ numberOfPatterns;
			// get 2 decimals
			int nskipeed = (int) dskipped;
			dskipped = nskipeed / 100.0;
			if (nskipeed > 0) {
				Print.warning(fileName + " has " + numberOfPatterns
						+ " patterns," + " valids = " + nnumberOfValidPatterns
						+ " and invalids (less than "
						+ nminimumNumberOfFramesForValidPattern + " frames) = "
						+ nnumberOfTooShortPatterns + "\n" + dskipped
						+ " % of frames were skipped.");
			}
		}
		return true;
	}

	// in order to save computations, should have different methods
	// depending on the type of HMM (plain, tied, etc.)
	/**
	 * Description of the Method
	 */
	private void calculateOutputProbabilities() {

		int nnumberOfEmittingStates = m_hmm.getNumberOfStates() - 2;
		int nnumberOfFrames = m_currentPattern.getNumOfFrames();
		// skip entry and exit (non-emitting) states
		for (int j = 0; j < nnumberOfEmittingStates; j++) {
			for (int t = 0; t < nnumberOfFrames; t++) {
				float[] findividualAndTotalProbabilities = m_hmm.m_mixturesOfGaussianPDFsBeingReestimated[j]
						.calculateIndividualProbabilities(m_currentPattern
								.getParametersOfGivenFrame(t), t);

				// total mixture probability is last element of
				// findividualAndTotalProbabilities
				m_flogOutputProbabilities[j][t] = findividualAndTotalProbabilities[findividualAndTotalProbabilities.length - 1];

				// store individual (each Gaussian) prob
				for (int m = 0; m < findividualAndTotalProbabilities.length - 1; m++) {
					m_flogProbabilityForEachGaussian[j][t][m] = findividualAndTotalProbabilities[m];
				}
			}
		}
	}

	/**
	 * Description of the Method
	 */
	private void reestimateTransitionMatrix() {

		if (m_nverbose > 2) {
			Print
					.dialogOnlyToConsole("m_hmm.m_transitionMatrixAccumulator.m_foccupationProbability");
			IO
					.displayPartOfMatrix(
							m_hmm.m_transitionMatrixAccumulator.m_foccupationProbability,
							10);
			Print
					.dialogOnlyToConsole("m_hmm.m_transitionMatrixAccumulator.m_ftotalOccupationProbability[i]");
			IO
					.displayPartOfVector(
							m_hmm.m_transitionMatrixAccumulator.m_ftotalOccupationProbability,
							10);
		}

		int nStates = m_hmm.getNumberOfStates();
		for (int i = 0; i < nStates - 1; i++) {
			m_hmm.m_ftransitionMatrix[i][0] = LogDomainCalculator.m_fLOG_DOMAIN_ZERO;
			float occi = m_hmm.m_transitionMatrixAccumulator.m_ftotalOccupationProbability[i];
			if (occi == 0.0F) {
				Print.dialog(m_hmm.toString());
				Print
						.error("Error in m_hmm.m_transitionMatrixAccumulator.m_ftotalOccupationProbability:");
				IO
						.DisplayVector(m_hmm.m_transitionMatrixAccumulator.m_ftotalOccupationProbability);
				End
						.throwError("ContinuousHMMReestimator.reestimateTransitionMatrix() "
								+ " zero state " + i + " occupation count.");
			}
			float sum = 0.0F;
			for (int j = 1; j < nStates; j++) {
				float x = m_hmm.m_transitionMatrixAccumulator.m_foccupationProbability[i][j]
						/ occi;
				m_hmm.m_ftransitionMatrix[i][j] = x;
				sum += x;
			}
			for (int j = 1; j < nStates; j++) {
				float x = m_hmm.m_ftransitionMatrix[i][j] / sum;

				m_hmm.m_ftransitionMatrix[i][j] = LogDomainCalculator
						.calculateLog(x);

				/*
				 * if (x < LogDomainCalculator.m_dMINIMUM_LOG_ARGUMENT) {
				 * m_hmm.m_ftransitionMatrix[i][j] = (float)
				 * LogDomainCalculator.m_fLOG_DOMAIN_ZERO; } else {
				 * m_hmm.m_ftransitionMatrix[i][j] = (float) Math.log(x); }
				 */
			}
		}

		// arbitrarily set last row of transition matrix to
		// have a loop transition in last state with probability = 1
		for (int j = 0; j < nStates - 1; j++) {
			m_hmm.m_ftransitionMatrix[nStates - 1][j] = LogDomainCalculator.m_fLOG_DOMAIN_ZERO;
		}
		m_hmm.m_ftransitionMatrix[nStates - 1][nStates - 1] = 0.0F;
		// prob = 1
	}

	/**
	 * Update HMM model based on accumulated counters.
	 * 
	 * @param fcovarianceFloor
	 *            Description of Parameter
	 */
	private void updateModel(float fcovarianceFloor) {
		if (m_oshouldUpdateTransitionMatrix) {
			reestimateTransitionMatrix();
		}

		if (m_oshouldUpdateMean || m_oshouldUpdateCovariance
				|| m_oshouldUpdateWeights) {
			int nnumberOfStates = m_hmm.getNumberOfStates();
			for (int i = 0; i < nnumberOfStates - 2; i++) {
				// i+2 below because it's just to print out results and
				// I want to count from state 1, 2, 3...
				m_hmm.m_mixturesOfGaussianPDFsBeingReestimated[i]
						.reestimateMixture(i + 2, m_oshouldUpdateMean,
								m_oshouldUpdateCovariance,
								m_oshouldUpdateWeights, fcovarianceFloor,
								m_fmixtureWeightFloor);
			}
		}

		if (m_nverbose > 3) {
			Print.dialog(m_hmm.toString());
		}

	}

	/**
	 * Description of the Method
	 * 
	 * @param nmaximumNumberOfFrames
	 *            Description of Parameter
	 */
	private void allocateSpace(int nmaximumNumberOfFrames) {
		int nnumberOfStates = m_hmm.getNumberOfStates();
		// 1) forward and backward matrices
		m_dalpha = new double[nnumberOfStates][nmaximumNumberOfFrames];
		m_dbeta = new double[nnumberOfStates][nmaximumNumberOfFrames];
		// 2) output probability: m_hmm.getNumberOfStates()-2 because there are
		// 2 non-emitting states.
		// make sure the elements of m_flogOutputProbabilities are 0.0F.
		m_flogOutputProbabilities = new float[nnumberOfStates - 2][nmaximumNumberOfFrames];

		// I am considering here that mixtures can have a different number of
		// Gaussians and then I am allocating space based on the maximum # of
		// Gaussians
		m_flogProbabilityForEachGaussian = new float[nnumberOfStates - 2][nmaximumNumberOfFrames][m_hmm
				.getMaximumNumberOfGaussiansPerMixture()];

		// make elements equal to LogDomainCalculator.m_fLOG_DOMAIN_ZERO ??
		for (int j = 0; j < m_flogProbabilityForEachGaussian.length; j++) {
			for (int t = 0; t < m_flogProbabilityForEachGaussian[0].length; t++) {
				for (int m = 0; m < m_flogProbabilityForEachGaussian[0][0].length; m++) {
					m_flogProbabilityForEachGaussian[j][t][m] = LogDomainCalculator.m_fLOG_DOMAIN_ZERO;
				}
			}
		}

		m_foccupationProbabilityOfCurrentPattern = new float[nnumberOfStates - 1];

		m_fauxiliaryZeroMean = new float[m_hmm.getSpaceDimension()];
	}

	/**
	 * Reestimate HMM model (passed as an input argument in the constructor).
	 * Returns -1 in error. Max number of iterations in parameter estimation
	 * (Baum-Welch) private final int m_nmaximumIterations; private final int
	 * m_nminimumNumberOfFramesForValidPattern; minimum variance in Gaussians.
	 * private final float m_fminimumVariance; convergence criterion private
	 * final float m_fconvergenceThreshold; min segments to train a model
	 * private final int m_nminimumNumberOfPatternsPerModel;
	 * 
	 * @param setOfPatterns
	 *            Description of Parameter
	 * @param setOfPatternsFileName
	 *            Description of Parameter
	 * @param nmaximumIterations
	 *            Description of Parameter
	 * @param nminimumNumberOfFramesForValidPattern
	 *            Description of Parameter
	 * @param nminimumNumberOfPatternsPerModel
	 *            Description of Parameter
	 * @param fconvergenceThreshold
	 *            Description of Parameter
	 * @param fcovarianceFloor
	 *            Description of Parameter
	 */
	private void reestimateHMMUsingBaumWelch(SetOfPatterns setOfPatterns,
			String setOfPatternsFileName, int nmaximumIterations,
			int nminimumNumberOfFramesForValidPattern,
			int nminimumNumberOfPatternsPerModel, float fconvergenceThreshold,
			float fcovarianceFloor) {

		// Print.dialog(nmaximumIterations + " " +fconvergenceThreshold + " "
		// +fcovarianceFloor);
		// check if SetOfPatterns has the minimum number of patterns
		if (!checkSetOfPatterns(setOfPatternsFileName, setOfPatterns,
				nminimumNumberOfPatternsPerModel,
				nminimumNumberOfFramesForValidPattern)) {
			Print.error(setOfPatternsFileName + " does not have "
					+ nminimumNumberOfPatternsPerModel
					+ " nminimumNumberOfPatternsPerModel.");
			return;
		}

		// to avoid allocating space for each Pattern, pre-allocate
		// space considering the maximum number of frames in this SetOfPatterns
		allocateSpace(setOfPatterns.getMaximumNumberOfFrames());

		int ntotalNumberOfPatterns = setOfPatterns.getNumberOfPatterns();
		if (m_nverbose > 2) {
			Print.dialog("Training sequence from file " + setOfPatternsFileName
					+ " is composed by " + ntotalNumberOfPatterns
					+ " patterns.");
		}

		StringBuffer informationForLogFile = new StringBuffer();
		if (m_nverbose > 1) {
			Print.dialog("Baum-Welch");
		}
		if (m_oshouldWriteReportFile) {
			SimpleDateFormat formatter = new SimpleDateFormat(
					"dd/MMMMM/yyyyy 'at' hh:mm:ss aaa");
			// SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
			informationForLogFile.append("Date = "
					+ formatter.format(new Date()) + "." + IO.m_NEW_LINE);
			informationForLogFile.append("Processing file "
					+ setOfPatternsFileName + " with " + ntotalNumberOfPatterns
					+ " patterns." + IO.m_NEW_LINE);
			informationForLogFile.append("Baum-Welch" + IO.m_NEW_LINE);
		}

		// initialize some variables
		boolean oconverged = false;
		// true when algorithm converged
		float flogOldProbability = LogDomainCalculator.m_fLOG_DOMAIN_ZERO;
		int niterations = 0;

		// main reestimation loop
		do {
			// zero accumulators used for reestimating model
			m_hmm.zeroAccumulators();

			int nnumberOfValidPatterns = 0;
			long ltotalNumberOfFramesInValidPatterns = 0;
			float flogProbabilityOfAllPatterns = 0.0F;
			// 1 in log domain
			float flogNewProbability = 0.0F;

			if (m_nverbose > 6) {
				Print.dialog("Initial HMM");
				m_hmm.toString();
			}

			boolean owasAnyTokenSkipped = false;
			int nnumberOfPatternsSkipped = 0;

			for (int npatternNumber = 0; npatternNumber < ntotalNumberOfPatterns; npatternNumber++) {

				// update current pattern
				m_currentPattern = setOfPatterns.getPattern(npatternNumber);

				int nobservationLength = m_currentPattern.getNumOfFrames();

				if (m_nverbose > 3) {
					Print.dialog("Iteration = " + (niterations + 1)
							+ ", nPattern # = " + npatternNumber + " has "
							+ nobservationLength + " frames.");
				}

				if (nobservationLength < nminimumNumberOfFramesForValidPattern) {
					if (m_nverbose > 4 && niterations == 0) {
						// print just for first iteration
						Print.warning("Skipped observation # = "
								+ npatternNumber + " because # of frames = "
								+ nobservationLength);
					}
				} else {
					// calculate all necessary output probabilities
					calculateOutputProbabilities();

					if (m_nverbose > 2) {
						Print
								.dialogOnlyToConsole("Log output probabilities (m_flogOutputProbabilities)");
						IO.displayPartOfMatrix(this.m_flogOutputProbabilities,
								10);
					}

					if (m_nverbose > 7) {
						Print
								.dialogOnlyToConsole("m_flogProbabilityForEachGaussian");
						IO.displayPartOfMatrix(
								this.m_flogProbabilityForEachGaussian, 10);
					}

					double dlogProbGivenByForward = calculateForwardMatrix();

					if (m_nverbose > 3) {
						Print.dialog("Alpha");
						IO.displayPartOfMatrix(this.m_dalpha, 10);
						Print
								.dialog("Log probability of current pattern given by forward matrix = "
										+ dlogProbGivenByForward);
					}

					if (dlogProbGivenByForward > LogDomainCalculator.m_fSMALL_NUMBER) {
						double dlogProbGivenByBackward = calculateBackwardMatrix();

						if (m_nverbose > 3) {
							Print.dialog("Beta");
					    	for (int i = 0; i < m_dbeta[0].length; i++) {
					    		for (int j = 0; j < m_dbeta.length; j++) {
									System.out.print(IO.format(m_dbeta[j][i]) + " ");
								}
					    		System.out.println("");
							}							
							//IO.displayPartOfMatrix(m_dbeta, 10);
							Print
									.dialog("Log probability of current pattern given by backward matrix = "
											+ dlogProbGivenByBackward);
							// Print.dialog("Current total log prob = " +
							// flogProbabilityOfAllPatterns);
						}

						// as in HTK, to reduce numerical errors:
						float flogProbabilityOfThisPattern = (float) (0.5 * (dlogProbGivenByForward + dlogProbGivenByBackward));

						// update counters
						UpdateAccumulators(flogProbabilityOfThisPattern);
						
						if (m_oshouldOutputGammaMatrix) {
							outputGammaMatrix(flogProbabilityOfThisPattern);
						}

						flogProbabilityOfAllPatterns += flogProbabilityOfThisPattern;
						nnumberOfValidPatterns++;
						ltotalNumberOfFramesInValidPatterns += nobservationLength;

					} else {
						if (m_nverbose > 1) {
							Print
									.warning("From file: "
											+ setOfPatternsFileName
											+ ". Pattern # "
											+ npatternNumber
											+ " skipped because too small forward log probability = "
											+ dlogProbGivenByForward
											+ ". This pattern has "
											+ nobservationLength + " frames.");
							// Print.dialog(getReestimatedHMM().toString());
							// if (m_nverbose == 20) {
							// System.exit(1);
							// }
							// m_nverbose = 20;
						}
						nnumberOfPatternsSkipped++;
						owasAnyTokenSkipped = true;
					}
				}

				showAccumulators();
			}

			if (owasAnyTokenSkipped) {
				if (nnumberOfPatternsSkipped > 0 && m_nverbose > 1) {
					Print
							.warning(nnumberOfPatternsSkipped
									+ " tokens were skipped (not used) because had small probability");

					Print
							.warning("Tokens were skipped (not used) because "
									+ "they had too small probability."
									+ " If a left-right HMM is being used, possibly these tokens had fewer frames"
									+ " than the necessary to transverse the HMM. You may want to change the configuration"
									+ " to ignore tokens with few frames.");
				}
			}

			if (nnumberOfValidPatterns == 0) {
				End.throwError("No usable training examples for Baum-Welch.");
				// return;
				// End.exit();
			}

			// update model
			updateModel(fcovarianceFloor);

			// normalize
			float flogProbPerPattern = flogProbabilityOfAllPatterns
					/ nnumberOfValidPatterns;
			float flogImprovement = flogProbPerPattern - flogOldProbability;

			if (m_oshouldWriteReportFile || (m_nverbose > 1)) {
				double dlogProbPerFrame = (double) flogProbabilityOfAllPatterns
						/ ltotalNumberOfFramesInValidPatterns;
				String information = null;
				if (niterations > 0) {
					if (niterations > 9) {
						// just to have a fancy console output :)
						information = niterations
								+ "| average log prob./frame="
								+ IO.format(dlogProbPerFrame) + ", /token="
								+ IO.format(flogProbPerPattern)
								+ ", convergence=" + IO.format(flogImprovement);
					} else {
						information = niterations
								+ " | average log prob./frame="
								+ IO.format(dlogProbPerFrame) + ", /token="
								+ IO.format(flogProbPerPattern)
								+ ", convergence=" + IO.format(flogImprovement);
					}
				} else {
					information = niterations + " | average log prob./frame="
							+ IO.format(dlogProbPerFrame) + ", /token="
							+ IO.format(flogProbPerPattern);
				}
				if (m_oshouldWriteReportFile) {
					informationForLogFile.append(information + IO.m_NEW_LINE);
				}
				if (m_nverbose > 1) {
					Print.dialog(information);
				}
			}

			oconverged = (Math.abs(flogImprovement) < fconvergenceThreshold);

			// update for next iteration
			flogOldProbability = flogProbPerPattern;

			niterations++;

		} while ((niterations < nmaximumIterations) && !oconverged);

		if (m_oshouldWriteReportFile) {
			informationForLogFile.append("Iterations = " + niterations + "."
					+ IO.m_NEW_LINE);
			// call method to write file
			IO.appendStringToEndOfTextFile(m_reportFileName,
					informationForLogFile.toString());
		}

		if (m_nverbose > 2) {
			if (oconverged) {
				Print
						.dialog("Reestimation converged. Total number of iterations = "
								+ niterations);
			} else {
				Print
						.dialog("Reestimation aborted. Total number of iterations = "
								+ niterations);
			}
		}
		// return niterations;
	}
	
	public void setVerbosity(int nverbosityLevel) {
		m_nverbose = nverbosityLevel;
	}

}

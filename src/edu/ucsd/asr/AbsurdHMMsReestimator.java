package edu.ucsd.asr;

import jmat.data.Matrix;

/**
 * Implements HMM reestimation according to Tales' paper, for the Absurd
 * project.
 * 
 * @author Aldebaro
 * @created April, 2007
 */
public class AbsurdHMMsReestimator extends SetOfSharedContinuousHMMsReestimator {

	// private static String m_synthesizerParametersInputFilesList;

	// [hmm][state] [meanarray]
	private float[][][] m_fsynthMean;

	// [hmm][state] [][] - covmatrix ( sigma_x)
	private float[][][][] m_fsynthFullCovarianceMatrix;

	// [hmm][state] [][] - factorLoadingMatrix A
	private float[][][][] m_ffactorLoadingMatrix;

	// [hmm][state] [][] - sigma_w
	private float[][][][] m_flinearApproximationErrorFullCovarianceMatrix;

	// [hmm][state] [biasArray]
	private float[][][] m_flinearApproximationBias;

	// VIDE ULTIMO PARAMETRO ABAIXO
	// XXX Tales : Amarrando a dimensao dos parametros do sintetizador = 13
	// private static final int m_nsynthDimension = 39;

	// [hmm][state]
	private MeanAccumulator[][] m_synthMeanAccumulators;

	// [hmm][state]
	private FullCovarianceAccumulator[][] m_synthFullCovarianceAccumulators;

	// private MatrixAccumulator[][] m_synthMatrixAccumulators;

	// [hmm][state]
	private MatrixAccumulator[][] m_parameterMatrixAccumulators;

	// [hmm][state]
	private BiasAccumulator[][] m_biasAccumulators;

	// [hmm][state]
	private LinearErrorFullCovarianceMatrixAccumulator[][] m_errorFullCovarianceAccumulators;

	// number of parameters for the synth - it can be different than the MFCC,
	// for example
	// XXX Tales : Amarrando a dimensao dos parametros do sintetizador = 13
	private int m_nsynthesizerSpaceDimension;

	public AbsurdHMMsReestimator(
			SetOfSharedContinuousHMMsBeingReestimated setOfSharedContinuousHMMsBeingReestimated,
			PatternGenerator patternGenerator,
			HeaderProperties headerProperties,
			// String setOfHMMOutputFileName,
			String dataLocatorFileName, String setOfPatternsInputDirectory,
			String outputOccupationStatisticsFileName) {

		super();
		/*
		 * not using the superclass' constructor anymore
		 * super(setOfSharedContinuousHMMsBeingReestimated, patternGenerator,
		 * headerProperties, // String setOfHMMOutputFileName,
		 * dataLocatorFileName, setOfPatternsInputDirectory,
		 * outputOccupationStatisticsFileName);
		 */
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

		super.interpretHeader(patternGenerator, setOfPatternsInputDirectory,
				outputOccupationStatisticsFileName);
		interpretHeader();

		// TODO
		initializeAccumulators();

		// call embedded training
		reestimateSetOfHMMsAndLinearTransformationsUsingEmbeddedBaumWelch(
				patternGenerator, m_dataLocatorFileName,
				setOfPatternsInputDirectory, outputOccupationStatisticsFileName);
	}

	public void interpretHeader() {
		// in case there are options specific to this class
		/*
		 * m_synthesizerParametersInputFilesList =
		 * m_headerProperties.getProperty(
		 * "AbsurdHMMsReestimator.synthesizerParametersInputFilesList",
		 * "synthInputFile.txt"); m_headerProperties.setProperty(
		 * "AbsurdHMMsReestimator.synthesizerParametersInputFilesList",
		 * m_synthesizerParametersInputFilesList);
		 */
		String property = m_headerProperties
				.getPropertyAndExitIfKeyNotFound("AbsurdHMMsReestimator.synthesizerSpaceDimension");
		m_nsynthesizerSpaceDimension = Integer.parseInt(property);
	}

	private void initializeAccumulators() {
		// verifica o "tamanho" do set of hmms e cria os acumuladores
		// o que tem de parametro a ser estimado para cada estado ?
		// para cada parametro eh necessario um "acumulador"
		// vou assumir que a cada estado ficam associados "apenas" um vetor e
		// uma matriz e depois voces complementam

		int nnumberOfPhysicalHMMs = m_hmms.length;

		// initialize accumulators
		m_synthMeanAccumulators = new MeanAccumulator[nnumberOfPhysicalHMMs][];
		m_synthFullCovarianceAccumulators = new FullCovarianceAccumulator[nnumberOfPhysicalHMMs][];
		m_parameterMatrixAccumulators = new MatrixAccumulator[nnumberOfPhysicalHMMs][];
		m_biasAccumulators = new BiasAccumulator[nnumberOfPhysicalHMMs][];
		m_errorFullCovarianceAccumulators = new LinearErrorFullCovarianceMatrixAccumulator[nnumberOfPhysicalHMMs][];

		// initialize paramters
		m_fsynthMean = new float[nnumberOfPhysicalHMMs][][];
		m_fsynthFullCovarianceMatrix = new float[nnumberOfPhysicalHMMs][][][];
		m_ffactorLoadingMatrix = new float[nnumberOfPhysicalHMMs][][][];
		m_flinearApproximationErrorFullCovarianceMatrix = new float[nnumberOfPhysicalHMMs][][][];
		m_flinearApproximationBias = new float[nnumberOfPhysicalHMMs][][];

		int nspaceDimension = m_setOfSharedContinuousHMMsBeingReestimated.m_mixturesOfGaussianPDFsBeingReestimated[0]
				.getSpaceDimension();

		for (int i = 0; i < nnumberOfPhysicalHMMs; i++) {
			int nnumberOfStatesOfThisHMM = m_hmms[i].getNumberOfStates();
			m_synthMeanAccumulators[i] = new MeanAccumulator[nnumberOfStatesOfThisHMM];
			// each state needs two matrix accumulators, later one of these
			// matrices is inverted and multiplied by the other
			m_synthFullCovarianceAccumulators[i] = new FullCovarianceAccumulator[nnumberOfStatesOfThisHMM];
			m_parameterMatrixAccumulators[i] = new MatrixAccumulator[nnumberOfStatesOfThisHMM];
			m_biasAccumulators[i] = new BiasAccumulator[nnumberOfStatesOfThisHMM];
			m_errorFullCovarianceAccumulators[i] = new LinearErrorFullCovarianceMatrixAccumulator[nnumberOfStatesOfThisHMM];

			// variables to be estimated
			m_fsynthMean[i] = new float[nnumberOfStatesOfThisHMM][m_nsynthesizerSpaceDimension];
			m_fsynthFullCovarianceMatrix[i] = new float[nnumberOfStatesOfThisHMM][m_nsynthesizerSpaceDimension][m_nsynthesizerSpaceDimension];
			m_ffactorLoadingMatrix[i] = new float[nnumberOfStatesOfThisHMM][nspaceDimension][m_nsynthesizerSpaceDimension];
			m_flinearApproximationErrorFullCovarianceMatrix[i] = new float[nnumberOfStatesOfThisHMM][nspaceDimension][nspaceDimension];
			m_flinearApproximationBias[i] = new float[nnumberOfStatesOfThisHMM][nspaceDimension];

		}

		// Tales : Initializing Accumulators
		for (int i = 0; i < nnumberOfPhysicalHMMs; i++) {
			for (int j = 0; j < m_hmms[i].getNumberOfStates(); j++) {
				m_synthMeanAccumulators[i][j] = new MeanAccumulator(
						m_nsynthesizerSpaceDimension);
				m_synthFullCovarianceAccumulators[i][j] = new FullCovarianceAccumulator(
						m_nsynthesizerSpaceDimension);
				m_parameterMatrixAccumulators[i][j] = new MatrixAccumulator(
						nspaceDimension, m_nsynthesizerSpaceDimension);
				m_biasAccumulators[i][j] = new BiasAccumulator(nspaceDimension,
						m_nsynthesizerSpaceDimension);
				m_errorFullCovarianceAccumulators[i][j] = new LinearErrorFullCovarianceMatrixAccumulator(
						nspaceDimension);
				// m_hmms[i].getReferenceToMixtures()[0].getSpaceDimension());
			}
		}

	}

	/**
	 * Embedded training
	 */
	public void reestimateSetOfHMMsAndLinearTransformationsUsingEmbeddedBaumWelch(
			PatternGenerator patternGenerator, String dataLocatorFileName,
			// String setOfHMMOutputFileName,
			String setOfPatternsInputDirectory,
			String outputOccupationStatisticsFileName) {

		firstStageAccumulation(patternGenerator, dataLocatorFileName,
				setOfPatternsInputDirectory, outputOccupationStatisticsFileName);

		estimateFirstStageSynthesizerParameters();
		// The second phase intend to calculate the full covariance matrix of
		// the linear approximation error (error = y - (Ax +b) );

		secondPhaseAccumulation(patternGenerator, dataLocatorFileName,
				setOfPatternsInputDirectory, outputOccupationStatisticsFileName);

		estimateSecondStageSynthesizerParameters();

	}

	private void firstStageAccumulation(PatternGenerator patternGenerator,
			String dataLocatorFileName,
			// String setOfHMMOutputFileName,
			String setOfPatternsInputDirectory,
			String outputOccupationStatisticsFileName) {

		// go through all training sequence using a DatabaseManager +
		// DataLocatorFile
		// with one SOP per sentence. For each DataLocator
		DatabaseManager databaseManager = new DatabaseManager(
				dataLocatorFileName);

		// read text file with list of files
		/*
		 * String[] synthFileNames = IO
		 * .readArrayOfStringsFromFile(m_synthesizerParametersInputFilesList);
		 * if (synthFileNames == null || synthFileNames.length < 1) {
		 * System.err.println("Error reading " +
		 * m_synthesizerParametersInputFilesList); System.exit(1); }
		 */

		// Print.dialog(m_tableOfHMMs.toString());
		if (m_nverbose > 1) {
			// System.out.println("Read file "
			// + m_synthesizerParametersInputFilesList + " with "
			// + synthFileNames.length + " files.");
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

			// find the parameter file name in order to read file
			String parametersFileName = dataLocator.getFileName();
			// parametersFileName = FileNamesAndDirectories.substituteExtension(
			// parametersFileName, SetOfPatterns.m_FILE_EXTENSION);
			parametersFileName = FileNamesAndDirectories.substituteExtension(
					parametersFileName, "mfc");

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

			// assume they are in the same directory
			String synthFileName = FileNamesAndDirectories.substituteExtension(
					parametersFileName, "hlsyn");

			// using HTK files:
			Pattern pattern = HTKInterfacer.getPatternFromFile(
					parametersFileName, patternGenerator);
			// not using SOP file
			// SetOfPatterns setOfPatterns = new
			// SetOfPatterns(parametersFileName);
			// in this case it has only 1 Pattern per SetOfPatterns
			// Pattern pattern = setOfPatterns.getPattern(0);

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
				System.err.println("Error calculating beta (1st stage). File " + parametersFileName 
						+ ", which is # " + (nnumberOfUtterances+1));				
			}

			// update totals
			dtotalLogProbability += m_dlogProbabilityOfCurrentUtterance;
			ntotalNumberOfFrames += nnumberOfFrames;
			nnumberOfUtterances++;

			if (m_oshouldOutputGammaMatrix) {
				IO.write3DMatrixtoBinFile(m_gammaOutputFileName, m_dgamma);
				//System.out.println("gamma values for utterance # "
				//		+ nnumberOfUtterances);
				// m_dgamma = IO.read3DMatrixFromBinFile(m_gammaOutputFileName);
				//IO.DisplayMatrix(m_dgamma);
			}

			Pattern synthPattern = HTKInterfacer
					.getPatternFromFile(synthFileName);
			//System.out
			//		.println("Tales - podemos processar agora os dados pois temos a matriz gamma e synthPattern");
			//System.out.println("AAAAAAAA " + synthFileName);
			//IO.DisplayMatrix(synthPattern.getParameters());
			
			updateSynthesizerAccumulators(synthPattern, pattern);
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

		// do not update HMM models
		if (false) {
			reestimateModels();
		}

		// estimateFirstStageSynthesizerParameters();
		//		
		// secondPhaseAccumulation (patternGenerator, dataLocatorFileName,
		// setOfPatternsInputDirectory,
		// outputOccupationStatisticsFileName);
		//		
		// estimateSecondStageSynthesizerParameters();

		// TODO

		// do not save models here
		// String outFileName = "besta.txt";
		// HTKInterfacer.writeSetOfSharedHMMs(getSetOfSharedContinuousHMMs(),
		// setOfHMMOutputFileName);
	}

	private void secondPhaseAccumulation(PatternGenerator patternGenerator,
			String dataLocatorFileName,
			// String setOfHMMOutputFileName,
			String setOfPatternsInputDirectory,
			String outputOccupationStatisticsFileName) {
		// go through all training sequence using a DatabaseManager +
		// DataLocatorFile
		// with one SOP per sentence. For each DataLocator
		DatabaseManager databaseManager = new DatabaseManager(
				dataLocatorFileName);

		// read text file with list of files
		/*
		 * String[] synthFileNames = IO
		 * .readArrayOfStringsFromFile(m_synthesizerParametersInputFilesList);
		 * if (synthFileNames == null || synthFileNames.length < 1) {
		 * System.err.println("Error reading " +
		 * m_synthesizerParametersInputFilesList); System.exit(1); }
		 */

		// Print.dialog(m_tableOfHMMs.toString());
		if (m_nverbose > 1) {
			// System.out.println("Read file "
			// + m_synthesizerParametersInputFilesList + " with "
			// + synthFileNames.length + " files.");
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

			// find the parameter file name in order to read file
			String parametersFileName = dataLocator.getFileName();
			// parametersFileName = FileNamesAndDirectories.substituteExtension(
			// parametersFileName, SetOfPatterns.m_FILE_EXTENSION);
			parametersFileName = FileNamesAndDirectories.substituteExtension(
					parametersFileName, "mfc");

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

			// assume they are in the same directory
			String synthFileName = FileNamesAndDirectories.substituteExtension(
					parametersFileName, "hlsyn");

			// using HTK files:
			Pattern pattern = HTKInterfacer.getPatternFromFile(
					parametersFileName, patternGenerator);
			// not using SOP file
			// SetOfPatterns setOfPatterns = new
			// SetOfPatterns(parametersFileName);
			// in this case it has only 1 Pattern per SetOfPatterns
			// Pattern pattern = setOfPatterns.getPattern(0);

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
				System.err.println("Error calculating beta (2nd stage). File " + parametersFileName 
						+ ", which is # " + (nnumberOfUtterances+1));				
			}


			// update totals
			dtotalLogProbability += m_dlogProbabilityOfCurrentUtterance;
			ntotalNumberOfFrames += nnumberOfFrames;
			nnumberOfUtterances++;

			if (m_oshouldOutputGammaMatrix) {
				IO.write3DMatrixtoBinFile(m_gammaOutputFileName, m_dgamma);
				//System.out.println("gamma values for utterance # "
				//		+ nnumberOfUtterances);
				// m_dgamma = IO.read3DMatrixFromBinFile(m_gammaOutputFileName);
				//IO.DisplayMatrix(m_dgamma);
			}

			Pattern synthPattern = HTKInterfacer
					.getPatternFromFile(synthFileName);
			//System.out
			//		.println("Tales - podemos processar agora os dados pois temos a matriz gamma e synthPattern");
			//System.out.println("AAAAAAAA " + synthFileName);
			//IO.DisplayMatrix(synthPattern.getParameters());

			updateSynthesizerAccumulatorsInSecondPhase(synthPattern, pattern);
		}
		databaseManager.finalizeDataReading();
		if (m_nnumberOfSentencesWithBetaPruningError > 0) {
			// line feed
			Print.dialog("");
		}

		// Print.dialog("Reestimation complete - average log prob per frame = "
		// + dtotalLogProbability / ntotalNumberOfFrames);
		// Print.dialog("nnumberOfUtterances = " + nnumberOfUtterances);

		if (false) {
			generateOccupationStatistics(outputOccupationStatisticsFileName);
		}

		// do not update HMM models
		if (false) {
			reestimateModels();
			// estimateSynthesizerParameters();
			estimateFirstStageSynthesizerParameters();
		}

		// do not save models here
		// String outFileName = "besta.txt";
		// HTKInterfacer.writeSetOfSharedHMMs(getSetOfSharedContinuousHMMs(),
		// setOfHMMOutputFileName);
	}

	private void updateSynthesizerAccumulators(Pattern synthPattern,
			Pattern mfccPattern) {
		int T = mfccPattern.getNumOfFrames();
		// or int T = m_dgamma.length;
		if (T != synthPattern.getNumOfFrames()) {
			System.err.println("Descreve erro aqui");
			// System.exit(1);
		}
		int Q = m_dgamma[0].length;
		for (int t = 0; t < T; t++) {
			float[] y = mfccPattern.getParametersOfGivenFrame(t);
			float[] x = synthPattern.getParametersOfGivenFrame(t);
			for (int q = 0; q < Q; q++) {
				int nnumberOfStates = m_hmms[q].getNumberOfStates();
				for (int s = 0; s < nnumberOfStates; s++) {
					float gamma = (float) m_dgamma[t][q][s];
					// below is never true, so I am making a mistake when
					// calculating gamma
					/*
					 * if (q==1&& gamma>0) { System.err.println("ERRRRO");
					 * System.exit(1); }
					 */
					m_synthMeanAccumulators[q][s].updateStatistics(gamma, x);
					m_synthFullCovarianceAccumulators[q][s].updateStatistics(
							gamma, x);
					m_parameterMatrixAccumulators[q][s].updateStatistics(gamma,
							y, x);
					m_biasAccumulators[q][s].updateStatistics(gamma, y, x);
				}
			}
		}

	}

	private void updateSynthesizerAccumulatorsInSecondPhase(
			Pattern synthPattern, Pattern mfccPattern) {

		int T = mfccPattern.getNumOfFrames();
		// or int T = m_dgamma.length;
		if (T != synthPattern.getNumOfFrames()) {
			System.err.println("Descreve erro aqui");
			// System.exit(1);
		}
		int Q = m_dgamma[0].length;
		for (int t = 0; t < T; t++) {
			float[] y = mfccPattern.getParametersOfGivenFrame(t);
			float[] x = synthPattern.getParametersOfGivenFrame(t);
			for (int q = 0; q < Q; q++) {
				int nnumberOfStates = m_hmms[q].getNumberOfStates();
				for (int s = 0; s < nnumberOfStates; s++) {
					float gamma = (float) m_dgamma[t][q][s];
					// below is never true, so I am making a mistake when
					// calculating gamma
					/*
					 * if (q==1&& gamma>0) { System.err.println("ERRRRO");
					 * System.exit(1); }
					 */

					// float [] temp = y - Ax - b
					float[] temp = linearApproximationError(q, s, y, x);

					m_errorFullCovarianceAccumulators[q][s].updateStatistics(
							gamma, temp);
				}
			}
		}

	}

	// implements y -Ax - b
	private float[] linearApproximationError(int ncurrentPhysicalHmm,
			int nstateNumber, float[] y, float[] x) {
		if (m_ffactorLoadingMatrix[0][0][0].length != x.length) {
			System.err.println("Display Error here!!");
			System.exit(1);
		}

		float[] ftempArray = new float[y.length];

		for (int i = 0; i < y.length; i++) {
			float temp = 0;
			for (int j = 0; j < x.length; j++) {
				temp += m_ffactorLoadingMatrix[ncurrentPhysicalHmm][nstateNumber][i][j]
						* x[j];
			}
			ftempArray[i] = temp
					+ m_flinearApproximationBias[ncurrentPhysicalHmm][nstateNumber][i];
		}

		float[] out = new float[y.length];
		// y - (Ax+b)
		for (int i = 0; i < out.length; i++) {
			out[i] = y[i] - ftempArray[i];
		}

		return out;
	}

	private void estimateFirstStageSynthesizerParameters() {
		// TODO: divide acumuladores do numerador e denominador
		// vou dar aqui um exemplo de divisao de matriz usando o pacote jmat
		// Tales
		// XXX : This commented part is a test to prove that the class
		// FullCovarianceAccumulator
		// is correctly computing the Sigma_x full covariance matrix.
		//
		// The sigma_x obtained by this method is y =[
		// 2.6666669846 4.0000000000
		// 4.0000000000 6.2222213745]
		// using the float matrix x below to update the statistics.
		//		

		// float[][] x = { {5,7},
		// {1,1},
		// {3,3}};
		//		
		// FullCovarianceAccumulator testAcc = new FullCovarianceAccumulator(2);
		//		
		// testAcc.updateStatistics(1,x[0]);
		// testAcc.updateStatistics(1,x[1]);
		// testAcc.updateStatistics(1,x[2]);
		//	
		// float[][] ffullCovarianceMatrix = new float[2][2];
		// testAcc.reestimateCovariance(0,-500, -500F, ffullCovarianceMatrix);
		// double[][] ttdx = Cloner.cloneAsDouble(ffullCovarianceMatrix);
		// Matrix ttA = new Matrix(ttdx);
		// System.out.println("AAAAA");
		// System.out.println(ttA.toString());
		// Matrix Ainv = ttA.inverse();
		// Matrix I = ttA.times(Ainv);
		// System.out.println(Ainv.toString());
		// System.out.println(I.toString());
		//
		// float[][] y = { {2,8},
		// {1,1},
		// {5,5}};
		//		
		// MatrixAccumulator testAcc2 = new MatrixAccumulator(2,2);
		// testAcc2.updateStatistics(1, y[0], x[0]);
		// testAcc2.updateStatistics(1, y[1], x[1]);
		// testAcc2.updateStatistics(1, y[2], x[2]);
		//		
		// float[][] ftestMatrix = new float[2][2];
		// testAcc2.reestimate(0, ffullCovarianceMatrix, ftestMatrix);
		// double[][] tdx = Cloner.cloneAsDouble(ftestMatrix);
		// Matrix testA = new Matrix(tdx);
		// System.out.println("AAAAA");
		// System.out.println(testA.toString());
		//		
		// BiasAccumulator testBiasAccumulator = new BiasAccumulator(2,2);
		// testBiasAccumulator.updateStatistics(1,y[0],x[0]);
		// testBiasAccumulator.updateStatistics(1,y[1],x[1]);
		// testBiasAccumulator.updateStatistics(1,y[2],x[2]);
		//		
		// float[] testBias = new float[2];
		// testBiasAccumulator.reestimateMean(0,ftestMatrix,testBias);
		// for (int i = 0; i < testBias.length; i++) {
		// System.out.println(testBias[i]);
		// }
		//		
		// System.exit(1);
		//

		int nnumberOfHMMs = m_hmms.length;

		// calculate xmean
		// float[] fmean = new float[m_nsynthesizerSpaceDimension];
		// m_synthMeanAccumulators[0][0].reestimateMean(fmean, 0, 0);

		// Calculate sigma_x

		// float[][][][] sigmax = new
		// float[nnumberOfHMMs][nnumberOfStatesForCurrentHMM][m_nsynthDimension][m_nsynthDimension];
		for (int h = 0; h < nnumberOfHMMs; h++) {
			int nnumberOfStatesForCurrentHMM = m_hmms[h].getNumberOfStates();
			// [hmm][state] fullcovMatrix[][]
			// float[][] sigmax = new
			// float[m_nsynthesizerSpaceDimension][m_nsynthesizerSpaceDimension];
			// float[] fmean = new float[m_nsynthesizerSpaceDimension];

			for (int s = 1; s < nnumberOfStatesForCurrentHMM - 1; s++) {

				if (!m_synthMeanAccumulators[h][s].reestimateMean(
						m_fsynthMean[h][s], s, 0)) {
					System.err.println("happened in HMM " + h + " => "
							+ m_tableOfHMMs.getFirstLabel(h));
				}

				// print mean
				if (m_nverbose > 5) {
					System.out.println("\nHMM " + h + " State " + s);
					System.out.println("xm =");
					for (int i = 0; i < m_fsynthMean[h][s].length; i++) {
						System.out.print(m_fsynthMean[h][s][i] + " ");
					}
				}

				m_synthFullCovarianceAccumulators[h][s].reestimateCovariance(s,
						-500, -500F, m_fsynthFullCovarianceMatrix[h][s]);
				double[][] dx = Cloner
						.cloneAsDouble(m_fsynthFullCovarianceMatrix[h][s]);
				Matrix A = new Matrix(dx);
				// print Sigma_x
				if (m_nverbose > 5) {
					System.out.println("\nsigmax =\n" + A.toString());
				}

				// float[][] fAmatrix = new
				// float[m_setOfSharedContinuousHMMsBeingReestimated.getSpaceDimension()][m_nsynthesizerSpaceDimension];
				m_parameterMatrixAccumulators[h][s].reestimate(s,
						m_fsynthFullCovarianceMatrix[h][s],
						m_ffactorLoadingMatrix[h][s]);
				// print A matrix
				Matrix temp = new Matrix(m_ffactorLoadingMatrix[h][s]);
				if (m_nverbose > 5) {
					System.out.print("Factor Loading Matrix =\n"
							+ temp.toString());

					System.out.println("Bias =");
				}
				// float[] bias = new
				// float[m_setOfSharedContinuousHMMsBeingReestimated.getSpaceDimension()];
				m_biasAccumulators[h][s].reestimateMean(s,
						m_ffactorLoadingMatrix[h][s],
						m_flinearApproximationBias[h][s]);
				if (m_nverbose > 5) {
					for (int i = 0; i < m_flinearApproximationBias[h][s].length; i++) {
						System.out.print(m_flinearApproximationBias[h][s][i]
								+ " ");
					}
					System.out.println("\n");
				}
			}
		}

	}

	private void estimateSecondStageSynthesizerParameters() {

		// // testeing linearArox method
		// for the test below the output obtained via matlab is
		// 13.7778 12.5556 9.6667 12.3333
		// 12.5556 13.6667 10.1111 9.5556
		// 9.6667 10.1111 9.2222 9.1111
		// 12.3333 9.5556 9.1111 13.5556
		// and it matches the output given by the test.
		//
		// LinearErrorFullCovarianceMatrixAccumulator ll = new
		// LinearErrorFullCovarianceMatrixAccumulator(4);
		// float[] gamma = {0.2F ,1 ,0.6F} ;
		// float[][] y = { {2,4,6,3},
		// {3,4,2,1},
		// {5,3,3,6}};
		// for (int i = 0; i < y.length; i++) {
		// ll.updateStatistics(gamma[i], y[i]);
		// }
		// float[][] errorFullCovarianceMatrix = new float[4][4];
		// ll.reestimateMatrix(0, errorFullCovarianceMatrix);
		//		
		// Matrix A = new Matrix(errorFullCovarianceMatrix);
		// System.out.println(A.toString());

		int nnumberOfHMMs = m_hmms.length;
		if (m_nverbose > 5) {
			System.out.println("ErrorFullCovarianceMatrix");
		}
		for (int h = 0; h < nnumberOfHMMs; h++) {
			int nnumberOfStatesForCurrentHMM = m_hmms[h].getNumberOfStates();
			for (int s = 1; s < nnumberOfStatesForCurrentHMM - 1; s++) {
				if (m_nverbose > 5) {
					System.out.println("\nHMM " + h + " State " + s);
				}
				m_errorFullCovarianceAccumulators[h][s].reestimateMatrix(s,
						m_flinearApproximationErrorFullCovarianceMatrix[h][s]);
				Matrix sMatix = new Matrix(
						m_flinearApproximationErrorFullCovarianceMatrix[h][s]);
				if (m_nverbose > 5) {
					System.out.println(sMatix.toString());
				}
				sMatix.inverse();
			}
		}

	}

	// should clone (um dia, agora pro paper nao)
	public AbsurdHMMs getSetOfReestimatedAbsurdHMMs() {
		// Tales: aqui tu devias fazer as contas para criar as novas medias e
		// variancias
		// Daih crias uma nova SetOfSharedContinuousHMMs
		// setOfSharedContinuousHMMs
		// e nao usas o metodo abaixo, que basicamente pega as medias e
		// variancias do
		// "this" (esse objeto), mas sim farias algo como:
		// SetOfSharedContinuousHMMs setOfSharedContinuousHMMs = new
		// SetOfSharedContinuousHMMs (...);

		int nnumberOfHMMs = m_hmms.length;
		int nspaceDimension = m_setOfSharedContinuousHMMsBeingReestimated.m_mixturesOfGaussianPDFsBeingReestimated[0]
				.getSpaceDimension();

		// [hmm][state][][] CovarianceMatrix
		float[][][][] newHMMsFullCovarianceMatrix = new float[nnumberOfHMMs][][][];
		// [hmm][state][mean]
		float[][][] newHMMsMeanVectors = new float[nnumberOfHMMs][][];
		for (int h = 0; h < nnumberOfHMMs; h++) {
			int nnumberOfStatesForCurrentHMM = m_hmms[h].getNumberOfStates();
			newHMMsFullCovarianceMatrix[h] = new float[nnumberOfStatesForCurrentHMM][nspaceDimension][nspaceDimension];
			newHMMsMeanVectors[h] = new float[nnumberOfStatesForCurrentHMM][nspaceDimension];
		}

		for (int h = 0; h < nnumberOfHMMs; h++) {
			int nnumberOfStatesForCurrentHMM = m_hmms[h].getNumberOfStates();
			for (int s = 0; s < nnumberOfStatesForCurrentHMM; s++) {

				// calculating mean
				for (int i = 0; i < m_ffactorLoadingMatrix[h][s].length; i++) {
					float tempMean = 0;
					for (int j = 0; j < m_ffactorLoadingMatrix[h][s][i].length; j++) {
						tempMean += m_ffactorLoadingMatrix[h][s][i][j]
								* m_fsynthMean[h][s][j];
					}
					newHMMsMeanVectors[h][s][i] = tempMean
							+ m_flinearApproximationBias[h][s][i];
				}

				// calculating CovMatrix
				Matrix factorLoadingMatrix = new Matrix(
						m_ffactorLoadingMatrix[h][s]);
				Matrix sigmaX = new Matrix(m_fsynthFullCovarianceMatrix[h][s]);
				Matrix sigmaW = new Matrix(
						m_flinearApproximationErrorFullCovarianceMatrix[h][s]);
				// newHMMsMeanVectors[h][s]
				double[][] temp = (((factorLoadingMatrix.times(sigmaX))
						.times(factorLoadingMatrix.transpose())).plus(sigmaW))
						.getArrayCopy();

				for (int i = 0; i < temp.length; i++) {
					for (int j = 0; j < temp[i].length; j++) {
						System.out.println(temp[i][j]);
						newHMMsFullCovarianceMatrix[h][s][i][j] = (float) temp[i][j];
					}
				}

			}
		}

		// AK fiz essa parte
		ContinuousHMM[] newContinuousHMMs = new ContinuousHMM[nnumberOfHMMs];
		for (int h = 0; h < nnumberOfHMMs; h++) {
			int nnumberOfStatesForCurrentHMM = m_hmms[h].getNumberOfStates();
			MixtureOfGaussianPDFs[] mixtures = new MixtureOfGaussianPDFs[nnumberOfStatesForCurrentHMM - 2];
			// subtract 2 because of the 2 non-emitting states
			for (int s = 0; s < nnumberOfStatesForCurrentHMM - 2; s++) {
				// the newHMMs... objects include the 2 non-emitting states, so
				// add 1
				float[][] covariance = newHMMsFullCovarianceMatrix[h][s + 1];
				float[] mean = newHMMsMeanVectors[h][s + 1];
				GaussianPDF gaussianPDF = new GaussianPDF(mean, covariance);
				// all mixtures have only 1 Gaussian
				mixtures[s] = new MixtureOfGaussianPDFs(gaussianPDF);
			}
			newContinuousHMMs[h] = new ContinuousHMM(m_hmms[h]
					.getTransitionMatrix().getMatrix(), mixtures);
		}

		String[] hmmFileNames = new String[nnumberOfHMMs];
		for (int i = 0; i < nnumberOfHMMs; i++) {
			hmmFileNames[i] = m_tableOfHMMs.getFirstLabel(i);
		}

		EmptyPatternGenerator patternGenerator = new EmptyPatternGenerator(
				nspaceDimension);

		SetOfPlainContinuousHMMs setOfPlainContinuousHMMs = new SetOfPlainContinuousHMMs(
				newContinuousHMMs, hmmFileNames, m_tableOfHMMs,
				patternGenerator);

		SetOfSharedContinuousHMMs setOfSharedContinuousHMMs = setOfPlainContinuousHMMs
				.convertToSharedHMMs();
		AbsurdHMMs absurdHMMs = new AbsurdHMMs(setOfSharedContinuousHMMs,
				m_fsynthMean, m_fsynthFullCovarianceMatrix,
				m_ffactorLoadingMatrix,
				m_flinearApproximationErrorFullCovarianceMatrix,
				m_flinearApproximationBias);
		return absurdHMMs;
	}
}// class end

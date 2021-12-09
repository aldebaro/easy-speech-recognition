package edu.ucsd.asr;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import jmat.data.Matrix;

import weka.classifiers.Classifier;
import edu.ucsd.asr.SetOfSharedContinuousHMMs.PhysicalHMM;

public class AbsurdHMMs extends SetOfSharedContinuousHMMs {

	// AK vou fazer uma gambiarra: como o Viterbi do set plain tem recursos para
	// descobrir o melhor caminho, vou usar plain para o viterbi
	private SetOfPlainContinuousHMMs m_setOfPlainContinuousHMMs;

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

	public AbsurdHMMs(SetOfSharedContinuousHMMs setOfSharedContinuousHMMs,
			float[][][] fsynthMean, float[][][][] fsynthFullCovarianceMatrix,
			float[][][][] ffactorLoadingMatrix,
			float[][][][] flinearApproximationErrorFullCovarianceMatrix,
			float[][][] flinearApproximationBias) {

		// constructor (does not clone)
		super(setOfSharedContinuousHMMs.m_mixturesOfGaussianPDFs,
				setOfSharedContinuousHMMs.m_transitionMatrices,
				setOfSharedContinuousHMMs.m_physicalHMMs,
				setOfSharedContinuousHMMs.m_tableOfMixtures,
				setOfSharedContinuousHMMs.m_tableOfTransitionMatrices,
				setOfSharedContinuousHMMs.m_patternGenerator,
				setOfSharedContinuousHMMs.m_tableOfLabels);

		// should clone
		m_setOfPlainContinuousHMMs = setOfSharedContinuousHMMs
				.convertToPlainHMMs();
		m_fsynthMean = fsynthMean;
		m_fsynthFullCovarianceMatrix = fsynthFullCovarianceMatrix;
		m_ffactorLoadingMatrix = ffactorLoadingMatrix;
		m_flinearApproximationErrorFullCovarianceMatrix = flinearApproximationErrorFullCovarianceMatrix;
		m_flinearApproximationBias = flinearApproximationBias;

	}

	public AbsurdHMMs(MixtureOfGaussianPDFs[] mixturesOfGaussianPDFs,
			TransitionMatrix[] transitionMatrices, PhysicalHMM[] physicalHMMs,
			TableOfLabels tableOfMixtures,
			TableOfLabels tableOfTransitionMatrices,
			PatternGenerator patternGenerator, TableOfLabels tableOfHMMs,
			float[][][] fsynthMean, float[][][][] fsynthFullCovarianceMatrix,
			float[][][][] ffactorLoadingMatrix,
			float[][][][] flinearApproximationErrorFullCovarianceMatrix,
			float[][][] flinearApproximationBias) {

		// constructor (does not clone)
		super(mixturesOfGaussianPDFs, transitionMatrices, physicalHMMs,
				tableOfMixtures, tableOfTransitionMatrices, patternGenerator,
				tableOfHMMs);

		// should clone
		m_setOfPlainContinuousHMMs = this.convertToPlainHMMs();
		m_fsynthMean = fsynthMean;
		m_fsynthFullCovarianceMatrix = fsynthFullCovarianceMatrix;
		m_ffactorLoadingMatrix = ffactorLoadingMatrix;
		m_flinearApproximationErrorFullCovarianceMatrix = flinearApproximationErrorFullCovarianceMatrix;
		m_flinearApproximationBias = flinearApproximationBias;
	}

	public int getNumberOfHLsynParameters() {
		return m_fsynthMean[0][0].length;
	}

	/**
	 * Gzips if extension is gz.
	 */
	public void writeAsSerializedObject(String fileName) {
		try {
			OutputStream os = new FileOutputStream(fileName);
			if (fileName.endsWith(".gz")) {
				os = new GZIPOutputStream(os);
			}
			ObjectOutputStream objectOutputStream = new ObjectOutputStream(os);
			objectOutputStream.writeObject(this);
			objectOutputStream.flush();
			objectOutputStream.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Error writing file " + fileName);
		}
	}

	/**
	 * Gunzips if extension is gz.
	 */
	public static AbsurdHMMs readSerializedObject(String fileName) {
		AbsurdHMMs absurdHMMs = null;
		try {
			InputStream is = new FileInputStream(fileName);
			if (fileName.endsWith(".gz")) {
				is = new GZIPInputStream(is);
			}
			ObjectInputStream objectInputStream = new ObjectInputStream(is);
			// Load classifier from file
			absurdHMMs = (AbsurdHMMs) objectInputStream.readObject();
			objectInputStream.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Error reading file " + fileName);
		}
		return absurdHMMs;
	}

	// soh vai funcionar para palavras isoladas (na verdade ele soh busca qual a
	// melhor physicalHMM no Set para o pattern em questao
	public Pattern findMAPEstimationOfHLSYNParameters(Pattern mfccPattern,
			String hmmLabel) {
		int nhmmIndex = m_setOfPlainContinuousHMMs.m_tableOfLabels
				.getEntry(hmmLabel);
		// use reference to simplify typing
		ContinuousHMM hmm = m_setOfPlainContinuousHMMs.m_continuousHMMs[nhmmIndex];

		double dscore = hmm.getScoreUsingViterbi(mfccPattern);
		int[] nstateSequence = hmm.getStateSequenceOfLastViterbi();
		// now we have the states q, and can calculate the x given y and q
		System.out.println("State sequence:");
		IO.DisplayVector(nstateSequence);
		System.out.println("Model " + nhmmIndex + " => " + hmmLabel
				+ " has score from Viterbi = " + dscore);

		// things of interest:
		int T = nstateSequence.length;
		int Q = getNumberOfHLsynParameters();
		float[][] x = new float[T][Q];

		for (int t = 0; t < T; t++) {
			// find the current state (subtract 1 because of the non-emitting
			// state)
			int nstate = nstateSequence[t] - 1;
			float[] y = mfccPattern.getParametersOfGivenFrame(t);
			// multiply, etc the bloddy things to get the MAP estimate of x
			// I will simply copy the b vector
			//as variaveis de instancia m_f* foram inicializadas contando com os estados non-emiting
			x[t] = calculateSynthParametersForCurrentState(nstateSequence[t], nhmmIndex, y);
			//x[t] = m_flinearApproximationBias[nhmmIndex][nstate];
		}
		return new Pattern(x);
	}

	private float[] calculateSynthParametersForCurrentState(int ncurrentState, int nhmmIndex, float[] acousticObservation) {
		// as variaveis de instancia m_f* foram inicializadas contando com os estados non-emiting
		Matrix invertedSigmaX = new Matrix(m_fsynthFullCovarianceMatrix[nhmmIndex][ncurrentState]).inverse();
		Matrix invertedSigmaW = new Matrix(m_flinearApproximationErrorFullCovarianceMatrix[nhmmIndex][ncurrentState]).inverse();
		Matrix factorLoading = new Matrix(m_ffactorLoadingMatrix[nhmmIndex][ncurrentState]);
		Matrix y = new Matrix(acousticObservation);
		Matrix b = new Matrix(m_flinearApproximationBias[nhmmIndex][ncurrentState]);
		Matrix xMean = new Matrix(m_fsynthMean[nhmmIndex][ncurrentState]);
		
		// x = (Sx^-1 + A^t * Sw^-1 * A)^-1 * ( Sx^-1 * xm + A^t * Sw^-1 * (y - b) )
		double[][] temp = 
		((invertedSigmaX.plus( ((factorLoading.transpose()).times(invertedSigmaW)).times(factorLoading) ) ).inverse())
		.times( (invertedSigmaX.times(xMean)).plus( ((factorLoading.transpose()).times(invertedSigmaW)).times( (y.minus(b)) ) )
		).getArrayCopy();
		
		float[] out = new float[temp.length];
		for (int i = 0; i < temp.length; i++) {
			out[i] = (float) temp[i][0];
		}
		
		return out;
	}
}

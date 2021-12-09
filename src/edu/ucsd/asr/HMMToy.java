package edu.ucsd.asr;

import java.util.Vector;

/**Generates observation sequence from a continuous HMM with
 * diagonal covariance matrices.
 * @author Aldebaro Klautau
 * @version 2.0 - September 07, 2000.
 */
public class HMMToy {
	
	private ContinuousHMM m_continuousHMM;
	private DiagonalGaussianSampleGenerator[][] m_pDFGenerators;
	private PMFSampleGenerator[] m_weightGenerators;
	private PMFSampleGenerator[] m_transitionGenerators;
	private int m_nnumberOfStates;
	private int m_nspaceDimension;
	
	public HMMToy(ContinuousHMM continuousHMM) {
		//get a continuous HMM
		m_continuousHMM = continuousHMM;
		
		m_nspaceDimension = continuousHMM.getSpaceDimension();
		
		//for each line of transition matrix, get a generator
		float[][] ftransitionMatrix = continuousHMM.getTransitionMatrix();
		m_nnumberOfStates = ftransitionMatrix.length;
		
		m_transitionGenerators = new PMFSampleGenerator[m_nnumberOfStates];
		for (int i=0; i<m_nnumberOfStates; i++) {
			m_transitionGenerators[i] = new PMFSampleGenerator(ftransitionMatrix[i]);
		}
		
		//for each mixture
		MixtureOfGaussianPDFs[] mixturesOfGaussianPDFs = continuousHMM.getMixturesOfGaussianPDFs();
		int nnumberOfMixtures = mixturesOfGaussianPDFs.length;
		
		m_weightGenerators = new PMFSampleGenerator[nnumberOfMixtures];
		m_pDFGenerators = new DiagonalGaussianSampleGenerator[nnumberOfMixtures][];
		for (int i=0; i<nnumberOfMixtures; i++) {
			//for each weights get a generator
			float[] fweights = mixturesOfGaussianPDFs[i].getComponentsWeights();
			m_weightGenerators[i] = new PMFSampleGenerator(fweights);
			
			//for each PDF get a generator
			GaussianPDF[] gaussians = mixturesOfGaussianPDFs[i].getGaussians();
			int nnumberOfGaussians = gaussians.length;
			m_pDFGenerators[i] = new DiagonalGaussianSampleGenerator[nnumberOfGaussians];
			for (int j=0; j<nnumberOfGaussians; j++) {
				DiagonalCovarianceGaussianPDF diagonalGaussian = (DiagonalCovarianceGaussianPDF) gaussians[j];
				m_pDFGenerators[i][j] = new DiagonalGaussianSampleGenerator(diagonalGaussian.m_fmean,
																			diagonalGaussian.getVariances());
			}
		}
	}
	
	public float[][] getSequence(int nmaximumNumberOfSamples) {
		//put the toy to work
		int nnumberOfSamples = 0;
		Vector samplesVector = new Vector();
		//first state is non-emitting, so skip it
		int ncurrentState = m_transitionGenerators[0].getSampleIndex();
		do {
			//System.out.println("ncurrentState = " + ncurrentState);
			
			//choose a Gaussian (subtract 1 because first state is non-emitting)
			int ncurrentGaussian = m_weightGenerators[ncurrentState-1].getSampleIndex();
			//generate a sample (subtract 1 because first state is non-emitting)
			float[] fsample = m_pDFGenerators[ncurrentState-1][ncurrentGaussian].getFloatSample();
			//add it
			samplesVector.addElement(fsample);
			//make a transition
			ncurrentState = m_transitionGenerators[ncurrentState].getSampleIndex();
			nnumberOfSamples++;
		} while(nnumberOfSamples < nmaximumNumberOfSamples &&
				(ncurrentState != (m_nnumberOfStates-1)));
		//convert Vector to matrix
		if (samplesVector == null || samplesVector.isEmpty()) {
			return null;
		} else {
			//confirm it
			nnumberOfSamples = samplesVector.size();
			float[][] fsamples = new float[nnumberOfSamples][];
			for (int i=0; i<nnumberOfSamples; i++) {
				fsamples[i] = (float[]) samplesVector.elementAt(i);
			}
			return fsamples;
		}
	}
	
	public Pattern getPattern(int nmaximumNumberOfSamples) {
		return new Pattern(getSequence(nmaximumNumberOfSamples));
	}
	
	public SetOfPatterns getSetOfPatterns(PatternGenerator patternGenerator,
										  int nnumberOfPatterns,
										  int nmaximumNumberOfSamples) {
		SetOfPatterns setOfPatterns = new SetOfPatterns(patternGenerator);
		for (int i=0; i<nnumberOfPatterns; i++) {
			setOfPatterns.addPattern(getPattern(nmaximumNumberOfSamples));
		}
		return setOfPatterns;
	}

	/**Use a EmptyPatternGenerator.
	 */
	public SetOfPatterns getSetOfPatterns(int nnumberOfPatterns,
										  int nmaximumNumberOfSamples) {
		PatternGenerator patternGenerator = new EmptyPatternGenerator(m_nspaceDimension);
		SetOfPatterns setOfPatterns = new SetOfPatterns(patternGenerator);
		for (int i=0; i<nnumberOfPatterns; i++) {
			setOfPatterns.addPattern(getPattern(nmaximumNumberOfSamples));
		}
		return setOfPatterns;
	}
	
}


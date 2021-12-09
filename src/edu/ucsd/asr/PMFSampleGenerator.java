package edu.ucsd.asr;

import java.util.Random;

/**
 * Generates samples draw from a discrete pdf.
 * @see HMMToy
 * @author Aldebaro Klautau
 * @version 2.0 - August 25, 2000
 */
public class PMFSampleGenerator extends SampleGenerator {

	private double[] m_dprobabilities;
	private double[] m_dvalues;
	private double[] m_dupLimits;

	//I'm using: System.currentTimeMillis()
	//private static long m_lrandomSeed = -1;

	public PMFSampleGenerator(double[] dprobabilities,
									  double[] dvalues) {

		checkInputArguments(dprobabilities,dvalues);
		m_dprobabilities = dprobabilities;
		m_dvalues = dvalues;
		m_random = new Random(System.currentTimeMillis());
		calculateUpLimits();
	}

	public PMFSampleGenerator(double[] dprobabilities) {
		//create vector with values 0, 1, ...
		double[] dvalues = new double[dprobabilities.length];
		for (int i=0; i<dvalues.length; i++) {
			dvalues[i] = i;
		}
		checkInputArguments(dprobabilities,dvalues);
		m_dprobabilities = dprobabilities;
		m_dvalues = dvalues;
		m_random = new Random(System.currentTimeMillis());
		calculateUpLimits();
	}

	public PMFSampleGenerator(float[] fprobabilities,
									  float[] fvalues) {

		checkInputArguments(fprobabilities,fvalues);
		m_dprobabilities = convertFloatVectorToDouble(fprobabilities);
		m_dvalues = convertFloatVectorToDouble(fvalues);
		m_random = new Random(System.currentTimeMillis());
		calculateUpLimits();
	}

	public PMFSampleGenerator(float[] fprobabilities) {
		//create vector with values 0, 1, ...
		float[] fvalues = new float[fprobabilities.length];
		for (int i=0; i<fvalues.length; i++) {
			fvalues[i] = (float) i;
		}
		checkInputArguments(fprobabilities,fvalues);
		m_dprobabilities = convertFloatVectorToDouble(fprobabilities);
		m_dvalues = convertFloatVectorToDouble(fvalues);
		m_random = new Random(System.currentTimeMillis());
		calculateUpLimits();
	}

	private void calculateUpLimits() {
		m_dupLimits = new double[m_dprobabilities.length];
		double sum = 0.0;
		for (int i=0; i<m_dupLimits.length; i++) {
			sum += m_dprobabilities[i];
			m_dupLimits[i] = sum;
		}
	}

	public int getSampleIndex() {
		//from [0, 1] ?
		//loop considering the possibility of x = 1
		//i could assume it is part of last interval too...
		//i could do binary search too...
		int nchosen = -1;
		do {
			double x = m_random.nextDouble();
			for (int i=0; i<m_dprobabilities.length; i++) {
				if (x < m_dupLimits[i]) {
					nchosen = i;
					break;
				}
			}
		} while (nchosen == -1);
		return nchosen;
	}

	public double getDoubleSample() {
		return m_dvalues[getSampleIndex()];
	}

	public float getFloatSample() {
		return (float) getDoubleSample();
	}

	public int[] getSequenceOfSampleIndices(int nnumberOfSamples) {
		if (nnumberOfSamples < 1) {
			return null;
		}
		int[] y = new int[nnumberOfSamples];
		for (int i=0; i<nnumberOfSamples; i++) {
			y[i] = getSampleIndex();
		}
		return y;
	}

	public double[] getSequenceOfDoubleSamples(int nnumberOfSamples) {
		if (nnumberOfSamples < 1) {
			return null;
		}
		double[] y = new double[nnumberOfSamples];
		for (int i=0; i<nnumberOfSamples; i++) {
			y[i] = getDoubleSample();
		}
		return y;
	}

	public float[] getSequenceOfFloatSamples(int nnumberOfSamples) {
		if (nnumberOfSamples < 1) {
			return null;
		}
		float[] y = new float[nnumberOfSamples];
		for (int i=0; i<nnumberOfSamples; i++) {
			y[i] = getFloatSample();
		}
		return y;
	}
}


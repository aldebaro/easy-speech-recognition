package edu.ucsd.asr;

import java.io.Serializable;
/**
 *
 * @author Aldebaro Klautau
 * @version 2.0 - August 14, 2000
 */
public final class MeanAccumulator implements Serializable {

	protected float[] m_foccupationProbability;
	protected float m_ftotalOccupationProbability;

	public MeanAccumulator(int nspaceDimension) {
		m_foccupationProbability = new float[nspaceDimension];
		m_ftotalOccupationProbability = 0.0F;
	}

	public void zeroAccumulators() {
		for (int i=0; i<m_foccupationProbability.length; i++) {
			m_foccupationProbability[i] = 0.0F;
		}
		m_ftotalOccupationProbability = 0.0F;
	}

	public void showAccumulators() {
		Print.dialog("MEAN OCC: " + m_ftotalOccupationProbability);
		Print.dialog("MEAN ACC:");
		IO.displayPartOfVector(m_foccupationProbability,10);
	}

	public void updateStatistics(float y, float[] fzeroMeanTemporaryVector) {
		// Update Mean Counter
		m_ftotalOccupationProbability += y;
		//ma->occ += y;
		//for (int k=1; k<=vSize; k++)
		//	ma->mu[k] += zot[k]*y; /* sum zero mean */
		for (int k=0; k<m_foccupationProbability.length; k++) {
			m_foccupationProbability[k] += fzeroMeanTemporaryVector[k] * y; //sum zero mean
		}
	}

	public boolean reestimateMean(float[] fcurrentMean, int nstateNumber, int ngaussianNumber) {

		//IO.DisplayVector(m_foccupationProbability);
		//System.out.println("AAAAA = " + m_ftotalOccupationProbability);

		if (m_ftotalOccupationProbability == 0.0F) {
			System.err.println("Mean reestimation: zero occupation count in state # = " +
								nstateNumber + " and Gaussian # = " + ngaussianNumber);
			return false;
			//End.throwError("Mean reestimation: zero occupation count in state # = " +
			//					nstateNumber + " and Gaussian # = " + ngaussianNumber);
		}
		int nspaceDimension = fcurrentMean.length;

		//note that the counts were based on input vectors after the current mean
		//was subtracted, so this function adds the new estimate to the current mean
		for (int k=0; k<nspaceDimension; k++) {
			fcurrentMean[k] += m_foccupationProbability[k] / m_ftotalOccupationProbability;
		}
		return true;
	}

} // end of class
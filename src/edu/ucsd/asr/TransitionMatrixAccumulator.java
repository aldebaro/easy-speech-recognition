package edu.ucsd.asr;

import java.io.Serializable;

/**
 *
 * @author Aldebaro Klautau
 * @version 2.0 - August 14, 2000
 */
public final class TransitionMatrixAccumulator implements Serializable {

	protected float[][] m_foccupationProbability;

	//per state
	protected float[] m_ftotalOccupationProbability;

	public TransitionMatrixAccumulator(int nnumberOfStates) {

		//it's not necessary to store the total occupation probability
		//for the first state because the reestimation formula uses the
		//whole probability of the pattern for this case (page 152 of HTK manual).
		//???? Ok, but UpTranCounts in fact uses the first position to update
		//for the non-emitting entry state and doesn't store the total
		//occup prob for the last state... ????
		m_ftotalOccupationProbability = new float[nnumberOfStates - 1];

		m_foccupationProbability = new float[nnumberOfStates][nnumberOfStates];
	}

	public void zeroAccumulators() {
		for (int i=0; i<m_foccupationProbability.length; i++) {
			for (int j=0; j<m_foccupationProbability[0].length; j++) {
				m_foccupationProbability[i][j] = 0.0F;
			}
		}
		for (int i=0; i<m_ftotalOccupationProbability.length; i++) {
			m_ftotalOccupationProbability[i] = 0.0F;
		}
	}

} // end of class
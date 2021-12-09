package edu.ucsd.asr;

import java.io.Serializable;

/**
 * Used by embedded Baum-Welch to store statistics.
 * @author Tales Imbiriba
 * @version 1.0 - May 04, 2007
 */
public class FullCovarianceAccumulator implements Serializable {



	protected float[][] m_foccupationProbabilityForSecondOrderExpectedValue;
	protected float[] m_foccupationProbabilityForFirstOrderExpectedValue;
	
	protected float m_ftotalOccupationProbability;

	public FullCovarianceAccumulator(int nspaceDimension) {
		m_foccupationProbabilityForSecondOrderExpectedValue = new float[nspaceDimension][nspaceDimension];
		m_foccupationProbabilityForFirstOrderExpectedValue = new float[nspaceDimension];
		m_ftotalOccupationProbability = 0.0F;
	}

	public void zeroAccumulators() {
		for (int i=0; i<m_foccupationProbabilityForSecondOrderExpectedValue.length; i++) {
			for (int j = 0; j < m_foccupationProbabilityForSecondOrderExpectedValue[i].length; j++) {
				m_foccupationProbabilityForSecondOrderExpectedValue[i][j] = 0.0F;
			}
			m_foccupationProbabilityForFirstOrderExpectedValue[i] = 0.0F;
		}
		m_ftotalOccupationProbability = 0.0F;
	}

	public void showAccumulators() {
		Print.dialog("VAR OCC: " + m_ftotalOccupationProbability);
		Print.dialog("VAR ACC:");
//		IO.displayPartOfVector(m_foccupationProbabilityForSecondOrderExpectedValue,10);
//		IO.displayPartOfVector(m_foccupationProbabilityForFirstOrderExpectedValue,10);
	}

	public void updateStatistics(float y, float[] fzeroMeanTemporaryVector) {
		m_ftotalOccupationProbability += y;
		//XXX System.out.println(y);
		for (int i=0; i<m_foccupationProbabilityForSecondOrderExpectedValue.length; i++) {
			for (int j = 0; j < m_foccupationProbabilityForSecondOrderExpectedValue[i].length; j++) {
				m_foccupationProbabilityForSecondOrderExpectedValue[i][j] += fzeroMeanTemporaryVector[i] * fzeroMeanTemporaryVector[j] * y;
			}
			m_foccupationProbabilityForFirstOrderExpectedValue[i] += fzeroMeanTemporaryVector[i] * y;
		}
	}

//	public double[] getWeightedSquaredValues(float[] fmean, float[] foldMean) {
//		return null;
//	}

	
	
	public boolean reestimateCovariance(int nstateNumber,
										//int ngaussianNumber,
										float ffloor,
										float fminimumValueForCovarianceElement,
										//float[] fmean,
										//float[] foldMean,
										//float[] fvariancesInverted
										float[][] ffullCovarianceMatrix) {
		if (m_ftotalOccupationProbability == 0.0F) {
			System.err.println("Covariance reestimation: zero occupation count in state # = " +
					nstateNumber );//+ " and Gaussian # = " + ngaussianNumber);
			return false;
			//End.throwError("Covariance reestimation: zero occupation count in state # = " +
			//					nstateNumber + " and Gaussian # = " + ngaussianNumber);
		}
		
		int ndimension = m_foccupationProbabilityForSecondOrderExpectedValue.length;
		//ffullCovarianceMatrix = new float[ndimension][ndimension];
		
		for (int i = 0; i < ndimension; i++) {
			for (int j = 0; j < ndimension; j++) {
				
				//System.out.println(m_foccupationProbabilityForSecondOrderExpectedValue[i][j] +" "+ m_ftotalOccupationProbability +" "+ m_foccupationProbabilityForFirstOrderExpectedValue[i] + " "+ m_foccupationProbabilityForFirstOrderExpectedValue[j]);
				
				float x = (m_foccupationProbabilityForSecondOrderExpectedValue[i][j] / m_ftotalOccupationProbability) -
				((m_foccupationProbabilityForFirstOrderExpectedValue[i]*m_foccupationProbabilityForFirstOrderExpectedValue[j]) / (m_ftotalOccupationProbability*m_ftotalOccupationProbability));
				//System.out.println(x);
				//System.out.println(m_foccupationProbabilityForSecondOrderExpectedValue[i][j] / m_ftotalOccupationProbability +" "+ (m_foccupationProbabilityForFirstOrderExpectedValue[i]*m_foccupationProbabilityForFirstOrderExpectedValue[j]) / (m_ftotalOccupationProbability*m_ftotalOccupationProbability));
				
				if (x < ffloor) {
					x = ffloor;
				}
				ffullCovarianceMatrix[i][j] = x;
				if (x < fminimumValueForCovarianceElement) {
					Print.warning("Covariance reestimation: variance = " + x +
							" is smaller than minimum = " + fminimumValueForCovarianceElement +
							" ! This covariance matrix will not be updated. You should " +
							"consider the possibility of increasing the floor value for " +
							"covariances, which is currently = " + ffloor);
					return false;
				}
			}
			
		}

		return true;
	}
	
	
	
	public float[][] getNumeratorReestimationForCrossVariableEstimation() {
		int ndimension = m_foccupationProbabilityForSecondOrderExpectedValue.length;
		//ffullCovarianceMatrix = new float[ndimension][ndimension];
		float[][] x = new float[ndimension][ndimension];
		for (int i = 0; i < ndimension; i++) {
			for (int j = 0; j < ndimension; j++) {
				
				//System.out.println(m_foccupationProbabilityForSecondOrderExpectedValue[i][j] +" "+ m_ftotalOccupationProbability +" "+ m_foccupationProbabilityForFirstOrderExpectedValue[i] + " "+ m_foccupationProbabilityForFirstOrderExpectedValue[j]);
				
				x[i][j] = (m_foccupationProbabilityForSecondOrderExpectedValue[i][j]) -
				((m_foccupationProbabilityForFirstOrderExpectedValue[i]*m_foccupationProbabilityForFirstOrderExpectedValue[j]) / (m_ftotalOccupationProbability));
				//System.out.println(x);
				//System.out.println(m_foccupationProbabilityForSecondOrderExpectedValue[i][j] / m_ftotalOccupationProbability +" "+ (m_foccupationProbabilityForFirstOrderExpectedValue[i]*m_foccupationProbabilityForFirstOrderExpectedValue[j]) / (m_ftotalOccupationProbability*m_ftotalOccupationProbability));
				
				
			}
			
		}
		return x;
	}
	
	
	
	
		
//	public boolean reestimateCovariance(int nstateNumber,
//										int ngaussianNumber,
//										float ffloor,
//										float fminimumValueForCovarianceElement,
//										float[] fmean,
//										float[] foldMean,
//										float[] fvariancesInverted) {
//		if (m_ftotalOccupationProbability == 0.0F) {
//			System.err.println("Covariance reestimation: zero occupation count in state # = " +
//								nstateNumber + " and Gaussian # = " + ngaussianNumber);
//			return false;
//			//End.throwError("Covariance reestimation: zero occupation count in state # = " +
//			//					nstateNumber + " and Gaussian # = " + ngaussianNumber);
//		}
//
//		for (int k=0; k<fmean.length; k++) {
//			float fmeanDifference = fmean[k] - foldMean[k];
//			float x = m_foccupationProbabilityForSecondOrderExpectedValue[k] / m_ftotalOccupationProbability - (fmeanDifference * fmeanDifference);
//			if (x < ffloor) {
//				x = ffloor;
//			}
//			if (x < fminimumValueForCovarianceElement) {
//				Print.warning("Covariance reestimation: variance = " + x +
//				" is smaller than minimum = " + fminimumValueForCovarianceElement +
//				" ! This covariance matrix will not be updated. You should " +
//				"consider the possibility of increasing the floor value for " +
//				"covariances, which is currently = " + ffloor);
//				return false;
//			}
//			//update new value
//			fvariancesInverted[k] = 1.0F / x;
//		}
//		return true;
//	}


	
}// end of class

package edu.ucsd.asr;

import java.io.Serializable;

import jmat.data.Matrix;

public class MatrixAccumulator implements Serializable {

	protected float[][] m_foccupationProbabilityForSecondOrderExpectedValue;
	protected float[] m_foccupationProbabilityForFirstOrderExpectedValueX;
	protected float[] m_foccupationProbabilityForFirstOrderExpectedValueY;
	
//	protected float[][] m_faccumulatedValues;
	protected float m_ftotalOccupationProbability;

	public MatrixAccumulator(int nySpaceDimension, int nxSpaceDimension) {
		m_foccupationProbabilityForSecondOrderExpectedValue = new float[nySpaceDimension][nxSpaceDimension];
		m_foccupationProbabilityForFirstOrderExpectedValueY = new float[nySpaceDimension];
		m_foccupationProbabilityForFirstOrderExpectedValueX = new float[nxSpaceDimension];
		m_ftotalOccupationProbability = 0.0F;
	}

	public void zeroAccumulators() {
		//TODO
		for (int i=0; i<m_foccupationProbabilityForSecondOrderExpectedValue.length; i++) {
			for (int j = 0; j < m_foccupationProbabilityForSecondOrderExpectedValue[i].length; j++) {
				m_foccupationProbabilityForSecondOrderExpectedValue[i][j] = 0.0F;
			}
			m_foccupationProbabilityForFirstOrderExpectedValueY[i] = 0.0F;
		}
		for (int i = 0; i < m_foccupationProbabilityForFirstOrderExpectedValueX.length; i++) {
			m_foccupationProbabilityForFirstOrderExpectedValueX[i] = 0.0F;
		}
		m_ftotalOccupationProbability = 0.0F;
	}

	public void showAccumulators() {
		//Print.dialog("VAR OCC: " + m_ftotalOccupationProbability);
		//int.dialog("VAR ACC:");
		IO.DisplayMatrix(m_foccupationProbabilityForSecondOrderExpectedValue);
		IO.DisplayVector(m_foccupationProbabilityForFirstOrderExpectedValueY);
		IO.DisplayVector(m_foccupationProbabilityForFirstOrderExpectedValueX);
	}

	/**
	 * Uses the outerproduct of two vectors.
	 * @param gamma
	 * @param x
	 * @param y
	 */
	public void updateStatistics(float gamma, float[] y, float[] x) {
		m_ftotalOccupationProbability += gamma;
		for (int i=0; i<y.length; i++) {
			for (int j = 0; j < x.length; j++) {
				m_foccupationProbabilityForSecondOrderExpectedValue[i][j] += gamma*y[i]*x[j];
				//TODO - implement outerproduct properly - pay attention on matrix dimension
				//m_faccumulatedValues[i][j] += x[i] * y[i] * gamma;
			}			
			m_foccupationProbabilityForFirstOrderExpectedValueY[i] += gamma*y[i];
		}
		for (int i = 0; i < x.length; i++) {
			m_foccupationProbabilityForFirstOrderExpectedValueX[i] += gamma*x[i];
		}
		
	}

	//TODO
	public boolean reestimate(int nstateNumber, float[][] fxReestimatedValues, float[][] fAmatrix) {
				
		if (m_ftotalOccupationProbability == 0.0F) {
			System.err.println("A matrix reestimation: zero occupation count in state # = " +
					nstateNumber );//+ " and Gaussian # = " + ngaussianNumber);
			return false;
			//End.throwError("Covariance reestimation: zero occupation count in state # = " +
			//					nstateNumber + " and Gaussian # = " + ngaussianNumber);
		}
		
		int nySpaceDimension = m_foccupationProbabilityForFirstOrderExpectedValueY.length;
		int nxSpaceDimension = m_foccupationProbabilityForFirstOrderExpectedValueX.length;
		
		float[][] fcrossOccupationMatrix = new float[nySpaceDimension][nxSpaceDimension];
		
		for (int i = 0; i < nySpaceDimension; i++) {
			for (int j = 0; j < nxSpaceDimension; j++) {
				fcrossOccupationMatrix[i][j] = m_foccupationProbabilityForSecondOrderExpectedValue[i][j] -
				(m_foccupationProbabilityForFirstOrderExpectedValueY[i] * m_foccupationProbabilityForFirstOrderExpectedValueX[j])/(m_ftotalOccupationProbability);				
			}
		}

//		for (int i = 0; i < nySpaceDimension; i++) {
//			for (int j = 0; j < nxSpaceDimension; j++) {
//				fAmatrix[i][j] = fcrossOccupationMatrix[i][j];
//				
//			}
//		}
		
		
		
		
		Matrix A;
		try {
			Matrix sigmax = new Matrix(fxReestimatedValues);
			Matrix crossVariablesMatrix = new Matrix(fcrossOccupationMatrix);
			A = crossVariablesMatrix.divide(sigmax);
			
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		
		
		for (int i = 0; i < nySpaceDimension; i++) {
			for (int j = 0; j < nxSpaceDimension; j++) {
				fAmatrix[i][j] = (float) A.get(i, j);
			}
		}
		
		
		return true;
	}


}

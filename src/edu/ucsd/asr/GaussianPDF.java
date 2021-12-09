package edu.ucsd.asr;

import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.Serializable;
import java.util.Random;

import jmat.data.Matrix;

/**
 * Gaussian PDF which outputs probabilities in natural-log domain.
 * 
 * This class represents a general full covariance Gaussian PDF. Subclasses
 * implement specialized PDFs, as diagonal covariance matrix, for example.
 * 
 * @author Aldebaro Klautau
 * @version 2.0 - August 11, 2000
 */
// A general equation for a Gaussian PDF is:
// f(x) = (1/Denominator) * exp(Exponent)
// So, the output probability in natural log (base e, ln)
// domain is given by
// f(x)_ln_domain = Exponent - ln(Denominator)
// ln(Denominator is pre-calculated when constructing the object.
public class GaussianPDF implements Cloneable, Serializable {

	/**
	 * Mean (average) vector.
	 */
	protected float[] m_fmean;

	/**
	 * Type of covariance matrix.
	 */
	protected CovarianceMatrix.Type m_covarianceMatrixType;

	private float[][] m_fcovarianceMatrix;

	// private float[] m_fdiagonalCovarianceMatrix;

	protected float[][] m_finvertedCovarianceMatrix;

	/**
	 * Denominator factor (in log domain).
	 */
	protected float m_fdenominatorFactor;

	private static final float m_flogTwoPi;

	private static final float m_ftwoPiSquareRoot;
	static {
		m_flogTwoPi = (float) Math.log(2.0 * Math.PI);
		m_ftwoPiSquareRoot = (float) Math.sqrt(2.0 * Math.PI);
		// m_random = new Random(System.currentTimeMillis());
	}

	protected GaussianPDF() {
		m_covarianceMatrixType = CovarianceMatrix.Type.FULL;
	}

	public GaussianPDF(float[] fmeans, float[][] fvariances) {
		m_fmean = fmeans;
		// variance ?
		// m_fcovarianceMatrix = fvariances;

		// tales
		m_fcovarianceMatrix = fvariances;
		invertAndClipCovarianceMatrix();
//		clipVariancesInverted();

		// calculate m_fdenominatorFactor
		calculateDenominatorFactor();

		m_covarianceMatrixType = CovarianceMatrix.Type.FULL;
	}

	
	public GaussianPDF(float[] fmeans, float[][] finvertedCovarianceMatrix, boolean oinverted) {
		m_fmean = fmeans;
		// variance ?
		// m_fcovarianceMatrix = fvariances;

		// tales
		m_finvertedCovarianceMatrix = finvertedCovarianceMatrix;
		//invertAndClipCovarianceMatrix();
		clipVariancesInverted();

		// calculate m_fdenominatorFactor
		calculateDenominatorFactor();

		m_covarianceMatrixType = CovarianceMatrix.Type.FULL;
	}

	
	
	
	/**
	 * Calculate denominator factor. It must be called by constructor.
	 */
	protected void calculateDenominatorFactor() {
		float flogDeterminant = getLogDeterminant();

		m_fdenominatorFactor = 0.5F * (m_fmean.length * m_flogTwoPi + flogDeterminant);

		if (Float.isInfinite(m_fdenominatorFactor)
				|| Float.isNaN(m_fdenominatorFactor)
				|| m_fdenominatorFactor < LogDomainCalculator.m_fLOG_DOMAIN_ZERO) {
			Print.error("calculateDenominatorFactor m_fdenominatorFactor = "
					+ m_fdenominatorFactor + " flogDeterminant = "
					+ flogDeterminant);
			// IO.DisplayVector(m_fvariancesInverted);
			End.exit();
		}
	}

	// tales
	private void invertAndClipCovarianceMatrix() {
		Matrix m = new Matrix(m_fcovarianceMatrix);
		double temp[][] = m.inverse().getArrayCopy();
		m_finvertedCovarianceMatrix = new float[temp.length][temp[0].length];
		for (int i = 0; i < temp.length; i++) {
			for (int j = 0; j < temp.length; j++) {
				m_finvertedCovarianceMatrix[i][j] = (float) temp[i][j];
			}
		}
		//cliping
		 for (int i = 0; i < m_finvertedCovarianceMatrix.length; i++) {
			 for (int j = 0; j < m_finvertedCovarianceMatrix[i].length; j++) {
				 if ( Float.isInfinite(m_finvertedCovarianceMatrix[i][j]) ||
				            Float.isNaN(m_finvertedCovarianceMatrix[i][j]) ) {
					 m_finvertedCovarianceMatrix[i][j] = Float.MAX_VALUE;
				 }
			}
		       
		 }
		 //does it help?
		System.gc();
	}

	//tales
	private void clipVariancesInverted(){
		for (int i = 0; i < m_finvertedCovarianceMatrix.length; i++) {
			for (int j = 0; j < m_finvertedCovarianceMatrix[i].length; j++) {
				if ( Float.isInfinite(m_finvertedCovarianceMatrix[i][j]) ||
						Float.isNaN(m_finvertedCovarianceMatrix[i][j]) ) {
					m_finvertedCovarianceMatrix[i][j] = Float.MAX_VALUE;
				}
			}	       
		}
	}
	
	
	/**
	 * Modify all elements of covariance matrix such that the minimum value is
	 * fminimumValue.
	 */
	public void setFloorVariance(float fminimumValue) {
		 //notice that the value below will be the maximum value allowed
	     float fminimumValueInverted = 1.0F / fminimumValue;

	     if (m_finvertedCovarianceMatrix != null) {
	       for (int i=0; i<m_finvertedCovarianceMatrix.length; i++) {
	    	   for (int j = 0; j < m_finvertedCovarianceMatrix[i].length; j++) {
	    		   if (fminimumValueInverted < m_finvertedCovarianceMatrix[i][j]) {
	    	           //means that m_fvariances < fminimumValue
	    			   m_finvertedCovarianceMatrix[i][j] = fminimumValueInverted;
	    	         }
	    	   }
	       }
	       clipVariancesInverted();
	     }
		//End.throwError("Not yet implemented");
	}

	public void getProbabilityPerElement(float[] x,
			float[] fprobabilityPerElement) {
		End.throwError("Not implemented yet");
	}

	/**
	 * Calculate the exponent of Gaussian PDF.
	 */
	public float calculateExponent(float[] finputVector) {
		// tales
		float fexponent = 0.0F;
		float[] ftemp = new float[finputVector.length];
		float[] ftemp2 = new float[finputVector.length];
		
		// x-m
		for (int i = 0; i < finputVector.length; i++) {
			ftemp[i] = finputVector[i] - m_fmean[i];			
		}
		// S * (x -m)
		for (int i = 0; i < ftemp.length; i++) {
			for (int j = 0; j < ftemp.length; j++) {
				ftemp2[i] += m_finvertedCovarianceMatrix[i][j]*ftemp[j];
			}
		}
		//(x-m)^t * S * (x-m)
		for (int i = 0; i < ftemp2.length; i++) {
			fexponent += ftemp[i] * ftemp2[i];
		}
		
		return -0.5F * fexponent;
//		End.throwError("Not implemented yet");
//		System.exit(1);
//
//		return 1F;
	}

	public double calculateExponentAsDouble(float[] finputVector) {
//		 tales
		double dexponent = 0.0;
		double[] ftemp = new double[finputVector.length];
		double[] ftemp2 = new double[finputVector.length];
		
		// x-m
		for (int i = 0; i < finputVector.length; i++) {
			ftemp[i] = finputVector[i] - m_fmean[i];			
		}
		// S * (x -m)
		for (int i = 0; i < ftemp.length; i++) {
			for (int j = 0; j < ftemp.length; j++) {
				ftemp2[i] += m_finvertedCovarianceMatrix[i][j]*ftemp[j];
			}
		}
		//(x-m)^t * S * (x-m)
		for (int i = 0; i < ftemp2.length; i++) {
			dexponent += ftemp[i] * ftemp2[i];
		}
		
		if (Double.isNaN(dexponent)) {
		       System.err.println("calculateExponentAsDouble() = NaN");
		       return Double.NaN;
		}
		
		return -0.5 * dexponent;
//		End.throwError("Not implemented yet");
//		System.exit(1);
//
//		return 1F;
	}

	/**
	 * Calculate the determinant.
	 */
	public float getLogDeterminant() {
		// using jmat.
		// tales
		Matrix m = new Matrix(m_fcovarianceMatrix);

		//this is the most stupid way of doing that!! 
		float z = LogDomainCalculator.calculateLog(m.determinant());
		
		if (Float.isInfinite(z) || Float.isNaN(z) ||
		         z < LogDomainCalculator.m_fLOG_DOMAIN_ZERO) {
			z = LogDomainCalculator.m_fLOG_DOMAIN_ZERO;
			Print.warning("Determinant underflow. Assuming value = " + z);
		}
		return z; 
		// End.throwError("Not implemented yet");
		// System.exit(1);
		// return 1F;
	}

	public String toStringAsInHTK() {
	     StringBuffer outputString = new StringBuffer("");
	     int nspaceDimension = m_fmean.length;
	     outputString = outputString.append("<MEAN> " + nspaceDimension + IO.m_NEW_LINE);
	     outputString = outputString.append(IO.getPartOfVectorAsString(m_fmean,m_fmean.length) + IO.m_NEW_LINE);
	     outputString = outputString.append("<InvCovar> " + nspaceDimension + IO.m_NEW_LINE);
	     
	     for (int i = 0; i < m_finvertedCovarianceMatrix.length; i++) {
			for (int j = i; j < m_finvertedCovarianceMatrix.length; j++) {
				outputString.append(IO.format(m_finvertedCovarianceMatrix[i][j]) + " ");
			}
			outputString.append(IO.m_NEW_LINE);
		}
	     return outputString.toString();		
	}

	// is it necessary or "implements" does the job ?
	public Object clone() {
		//tales
		float[] fmean = Cloner.clone(m_fmean);
	    float[][] fivar = Cloner.clone(m_finvertedCovarianceMatrix);
	    return new GaussianPDF(fmean, fivar, true);
	    
//		End.throwError("Not implemented yet");
//		System.exit(1);
//		return null;
	}

	/**
	 * Check if mean vector is composed by valid numbers (not NaN or infinite).
	 */
	public boolean isMeanOk() {
		for (int i=0; i<m_fmean.length; i++) {
			if (Float.isNaN(m_fmean[i]) | Float.isInfinite(m_fmean[i])) {
				return false;
			}
		}
		return true;
//		End.throwError("Not implemented yet");
//		System.exit(1);
//		return false;
	}

	/**
	 * return main diagonal elements of covariance matrix
	 */
	public float[] getVariances() {
		End.throwError("Not implemented yet");
		System.exit(1);
		return null;
	}

	public float[] getStandardDeviations() {
		End.throwError("Not implemented yet");
		System.exit(1);
		return null;
	}

	/**
	 * Check if covariance matrix is composed by valid numbers (not NaN or
	 * infinite).
	 */
	public boolean isCovarianceOk() {
		for (int i=0; i<m_finvertedCovarianceMatrix.length; i++) {
			for (int j = 0; j < m_finvertedCovarianceMatrix[i].length; j++) {
				if ( Float.isNaN(m_finvertedCovarianceMatrix[i][j]) |
						Float.isInfinite(m_finvertedCovarianceMatrix[i][j]) ) {
							return false;
				}
			}
				
		}
		return true;
		
//		End.throwError("Not implemented yet");
//		System.exit(1);
//		return false;
	}

	/**
	 * Write float numbers organized as follows: (a) means and then covariance
	 * matrix (first line, second line, etc).
	 */
	public void writeToStream(DataOutputStream dataOutputStream) {
		End.throwError("Not implemented yet");
		System.exit(1);
	}

	/**
	 * Read float numbers organized as follows: (a) means and then covariance
	 * matrix (first line, second line, etc).
	 */
	public GaussianPDF readFromStream(DataInputStream dataInputStream,
			int nspaceDimension) {
		End.throwError("Not implemented yet");
		System.exit(1);
		return null;
	}

	public DiagonalCovarianceGaussianPDF[] split() {
		End.throwError("Not implemented yet");
		System.exit(1);
		return null;
	}

	public void resetTimeForWhichProbabilityIsValid() {
		// there is nothing to do in this method.
		// for example: DiagonalCovarianceGaussianPDFBeingReestimated
		// has a variable that does not exist here
	}

	/**
	 * Calculate output probability in log (base e, ln or natural log) domain.
	 */
	public float calculateLogProbability(float[] x) {
		float z = calculateExponent(x) - m_fdenominatorFactor;

		if (Float.isInfinite(z) || Float.isNaN(z)) {
			Print.warning("calculateLogProbability()=" + z
					+ ", for G with denominator=" + m_fdenominatorFactor
					+ " and exponent=" + calculateExponent(x));
			if (Float.isNaN(z)) {
				z = LogDomainCalculator.m_fLOG_DOMAIN_ZERO;
			} else {
				if (z == Float.POSITIVE_INFINITY) {
					z = Float.MAX_VALUE;
				} else {
					z = LogDomainCalculator.m_fLOG_DOMAIN_ZERO;
				}
			}
		}
		return z;
	}

	public double calculateLogProbabilityAsDouble(float[] x) {
		double z = calculateExponentAsDouble(x) - m_fdenominatorFactor;

		if (Double.isInfinite(z) || Double.isNaN(z)) {
			Print.warning("calculateLogProbabilityAsDouble()=" + z
					+ ", for G with denominator=" + m_fdenominatorFactor
					+ " and exponent=" + calculateExponentAsDouble(x));
			if (Double.isNaN(z)) {
				z = LogDomainCalculator.m_fLOG_DOMAIN_ZERO;
			} else {
				if (z == Double.POSITIVE_INFINITY) {
					z = Double.MAX_VALUE;
				} else {
					z = LogDomainCalculator.m_fLOG_DOMAIN_ZERO;
				}
			}
		}
		return z;
	}

	/**
	 * Return the covariance matrix type of this object.
	 */
	public CovarianceMatrix.Type getCovarianceMatrixType() {
		return m_covarianceMatrixType;
	}

	/**
	 * Return the space dimension (number of variables) in this object.
	 */
	public int getSpaceDimension() {
		if (m_fmean == null) {
			return 0;
		} else {
			return m_fmean.length;
		}
	}

	public float[] getMean() {
		return Cloner.clone(m_fmean);
	}

	/**
	 * Subtract the mean of this Gaussian from input vector finput. The output
	 * array is passed as an input argument to avoid repeatedly allocating space
	 * in this method.
	 */
	public void subtractMean(float[] finput, float[] foutput) {
		for (int i = 0; i < m_fmean.length; i++) {
			foutput[i] = finput[i] - m_fmean[i];
		}
	}

	public float[] getMeanReference() {
		return m_fmean;
	}

	public String toString() {
		String outputString = "";
	    outputString = outputString.concat("\nMeans (MEAN)" + IO.m_NEW_LINE);
	    outputString = outputString.concat(IO.getPartOfVectorAsString(m_fmean,m_fmean.length));
	    outputString = outputString.concat("\nVariances (sigma^2) (VARS)" + IO.m_NEW_LINE);
	    
	    outputString = outputString.concat(IO.getPartOfMatrixAsString(m_fcovarianceMatrix, m_fcovarianceMatrix.length));
	    return outputString;
	}	
	
	public static void main(String[] args){
		
		
		float[] x = {2,3};
		float[] fmean = {1,4};
		float[][] fcovMatrix = {{1,0.0F},{0.0F,1.4F}};
		
		
		GaussianPDF gaussianPDF = new GaussianPDF(fmean,fcovMatrix);
		System.out.println(gaussianPDF.calculateLogProbability(x));
		
		float[] fvariances = {1,1.4F};
		
		DiagonalCovarianceGaussianPDF diagonalCovarianceGaussianPDF = new DiagonalCovarianceGaussianPDF(fmean,fvariances);
		
		System.out.println(diagonalCovarianceGaussianPDF.calculateLogProbability(x));
		
		System.out.println(gaussianPDF.toString());
		System.out.println(diagonalCovarianceGaussianPDF.toString());
	}
}
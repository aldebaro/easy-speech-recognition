package edu.ucsd.asr;

/**General matrix. Maybe should get a math package from Internet.
 * And has vectors also... Hum... Not good...
 *
 * @author Aldebaro Klautau
 * @version 1.4 - August 07, 2000
 */

public class Matrix {

	public static int[][] transpose(int[][] x) {
		int[][] y = new int[x[0].length][x.length];
		for (int i = 0; i < x.length; i++) {
			for (int j = 0; j < x[0].length; j++) {
				y[j][i] = x[i][j];
			}
		}
		return y;
	}

	public static float[][] transpose(float[][] x) {
		float[][] y = new float[x[0].length][x.length];
		for (int i = 0; i < x.length; i++) {
			for (int j = 0; j < x[0].length; j++) {
				y[j][i] = x[i][j];
			}
		}
		return y;
	}

        public static double[][] transpose(double[][] x) {
                double[][] y = new double[x[0].length][x.length];
                for (int i = 0; i < x.length; i++) {
                        for (int j = 0; j < x[0].length; j++) {
                                y[j][i] = x[i][j];
                        }
                }
                return y;
        }

	public static float[][] resize(float[] x, int nnumberOfColumns) {
		if (x.length % nnumberOfColumns != 0) {
			End.throwError("x.length = " + x.length + " is not multiple of " + nnumberOfColumns);
		}
		float[][] out = new float[x.length / nnumberOfColumns][nnumberOfColumns];
		for (int i = 0; i < out.length; i++) {
			for (int j = 0; j < nnumberOfColumns; j++) {
				out[i][j] = x[i*nnumberOfColumns + j];
			}
		}
		return out;
	}

	//maybe a class called Quantizer?
	private int quantize(float x, float fminimum, float fmaximum, float fstepSize) {
		if (x < fminimum) {
			return 0;
		}
		if (x > fmaximum) {
			x = fmaximum;
		}
		return (int) (( (x-fminimum) / fstepSize) + 0.5F);
	}

	private int[] quantize(float[] x, float[] fminimum, float[] fmaximum, float[] fstepSize) {
		int[] nvalues = new int[x.length];
		for (int i = 0; i < nvalues.length; i++) {
			nvalues[i] = quantize(x[i], fminimum[i], fmaximum[i], fstepSize[i]);
		}
		return nvalues;
	}

	private int[][] quantize(float[][] fparameters, float[] fminimum, float[] fmaximum, float[] fstepSize) {
		int[][] nquantizedValues = new int[fparameters.length][fparameters[0].length];
		for (int i = 0; i < nquantizedValues.length; i++) {
			nquantizedValues[i] = quantize(fparameters[i],fminimum, fmaximum, fstepSize);
		}
		return nquantizedValues;
	}

	/**
	 * Return indices of first entry found which has
	 * value equal to minimum. Assume rectangular matrices.
	 */
	public static int[] getIndicesOfMinimumValue(double[][] matrix) {
		double min = Double.MAX_VALUE;
		int[] nindices = new int[2];
		for (int i = 0; i < matrix.length; i++) {
			for (int j = 0; j < matrix[0].length; j++) {
				if (matrix[i][j] < min) {
				   min = matrix[i][j];
				   nindices[0] = i;
				   nindices[1] = j;
				}
			}
		}
		return nindices;
	}

	public static float getMaximumValue(float[][] fmatrix) {
		float max = -Float.MAX_VALUE;
		for (int i = 0; i < fmatrix.length; i++) {
			float lineMax = getMaximumValueOfGivenLine(fmatrix,i);
			if (lineMax > max) {
				max = lineMax;
			}
		}
		return max;
	}

	public static float getMinimumValue(float[][] fmatrix) {
		float min = Float.MAX_VALUE;
		for (int i = 0; i < fmatrix.length; i++) {
			float lineMin = getMinimumValueOfGivenLine(fmatrix,i);
			if (lineMin < min) {
				min = lineMin;
			}
		}
		return min;
	}

	public static double getMinimumValue(double[][] fmatrix) {
		double min = Float.MAX_VALUE;
		for (int i = 0; i < fmatrix.length; i++) {
			double lineMin = getMinimumValueOfGivenLine(fmatrix,i);
			if (lineMin < min) {
				min = lineMin;
			}
		}
		return min;
	}

	public static float getMaximumValueOfGivenLine(float[][] fmatrix, int nlineNumber) {
		if (nlineNumber < 0 || nlineNumber > (fmatrix.length-1) ) {
			End.throwError("Matrix has " + fmatrix.length + " lines, so " +
			nlineNumber + " is not a valid line number.");
		}
		return getMaximumValueOfVector(fmatrix[nlineNumber]);
	}

	public static float getMinimumValueOfGivenLine(float[][] fmatrix, int nlineNumber) {
		if (nlineNumber < 0 || nlineNumber > (fmatrix.length-1) ) {
			End.throwError("Matrix has " + fmatrix.length + " lines, so " +
			nlineNumber + " is not a valid line number.");
		}
		return getMinimumValueOfVector(fmatrix[nlineNumber]);
	}

	public static double getMinimumValueOfGivenLine(double[][] fmatrix, int nlineNumber) {
		if (nlineNumber < 0 || nlineNumber > (fmatrix.length-1) ) {
			End.throwError("Matrix has " + fmatrix.length + " lines, so " +
			nlineNumber + " is not a valid line number.");
		}
		return getMinimumValueOfVector(fmatrix[nlineNumber]);
	}

	public static float getMaximumValueOfVector(float[] vector) {
		float max = -Float.MAX_VALUE;
		for (int i = 0; i < vector.length; i++) {
			if (vector[i] > max) {
				max = vector[i];
			}
		}
		return max;
	}

	public static int getMaximumValueOfVector(int[] vector) {
		int max = -Integer.MAX_VALUE;
		for (int i = 0; i < vector.length; i++) {
			if (vector[i] > max) {
				max = vector[i];
			}
		}
		return max;
	}

	public static int getMinimumValueOfVector(int[] vector) {
		int min = Integer.MAX_VALUE;
		for (int i = 0; i < vector.length; i++) {
			if (vector[i] < min) {
				min = vector[i];
			}
		}
		return min;
	}

	public static double getMinimumValueOfVector(double[] vector) {
		double min = Double.MAX_VALUE;
		for (int i = 0; i < vector.length; i++) {
			if (vector[i] < min) {
				min = vector[i];
			}
		}
		return min;
	}

	public static float getMinimumValueOfVector(float[] vector) {
		float min = Float.MAX_VALUE;
		for (int i = 0; i < vector.length; i++) {
			if (vector[i] < min) {
				min = vector[i];
			}
		}
		return min;
	}

	public static float getProduct(float[] vector) {
		float value = 1;
		for (int i = 0; i < vector.length; i++) {
			value *= vector[i];
		}
		return value;
	}

	public static float getSum(float[] vector) {
		float value = 0;
		for (int i = 0; i < vector.length; i++) {
			value += vector[i];
		}
		return value;
	}

	public static float[][] subtract(float[][] matrix1, float[][] matrix2) {
		float[][] output = new float[matrix1.length][matrix1[0].length];
		for (int i = 0; i < output.length; i++) {
			for (int j = 0; j < output[0].length; j++) {
				output[i][j] = matrix1[i][j] - matrix2[i][j];
			}
		}
		return output;
	}

	public static int[][] sum(int[][] matrix1, int constant) {
		int[][] output = new int[matrix1.length][matrix1[0].length];
		for (int i = 0; i < output.length; i++) {
			for (int j = 0; j < output[0].length; j++) {
				output[i][j] = matrix1[i][j] + constant;
			}
		}
		return output;
	}

	public static int[][] abs(int[][] matrix1) {
		int[][] output = new int[matrix1.length][matrix1[0].length];
		for (int i = 0; i < output.length; i++) {
			for (int j = 0; j < output[0].length; j++) {
				output[i][j] = Math.abs(matrix1[i][j]);
			}
		}
		return output;
	}

	public static float[] normalize(float[] x) {
		float sum = 0;
		float[] y = new float[x.length];
		for (int i = 0; i < x.length; i++) {
			sum += x[i];
		}
		for (int i = 0; i < x.length; i++) {
			y[i] = x[i] / sum;
		}
		return y;
	}

	public static void normalizeInPlace(float[] x) {
		float sum = 0;
		for (int i = 0; i < x.length; i++) {
			sum += x[i];
		}
		for (int i = 0; i < x.length; i++) {
			x[i] /= sum;
		}
	}

}

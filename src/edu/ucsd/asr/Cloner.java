package edu.ucsd.asr;

/** Methods to clone intrinsic data type vectors.
 * @author Aldebaro Klautau
 * @version 2.0 - September 27, 2000.
 */
 //Whenever possible, clone with System.arraycopy
public final class Cloner {


	public static double[] clone(double[] input) {
		if (input == null) {
			return null;
		}
		double[] out = new double[input.length];
		System.arraycopy(input,0,out,0,input.length);
		return out;
	}

	public static boolean[] clone(boolean[] input) {
		if (input == null) {
			return null;
		}
		boolean[] out = new boolean[input.length];
		System.arraycopy(input,0,out,0,input.length);
		return out;
	}

	public static int[] clone(int[] input) {
		if (input == null) {
			return null;
		}
		int[] out = new int[input.length];
		System.arraycopy(input,0,out,0,input.length);
		return out;
	}

	public static float[] clone(float[] input) {
		if (input == null) {
			return null;
		}
		float[] out = new float[input.length];
		System.arraycopy(input,0,out,0,input.length);
		return out;
	}

	public static byte[] clone(byte[] input) {
		if (input == null) {
			return null;
		}
		byte[] out = new byte[input.length];
		System.arraycopy(input,0,out,0,input.length);
		return out;
	}

	/**
	 * Supports non-rectangular matrices. It would
	 * be faster if assumes rectangular matrices.
	 */
	public static int[][] clone(int[][] input) {
		if (input == null) {
			return null;
		}
		int[][] out = new int[input.length][];
		for (int i=0; i<input.length; i++) {
			out[i] = new int[input[i].length];
			System.arraycopy(input[i],0,out[i],0,input[i].length);
		}
		return out;
	}

	/**
	 * Supports non-rectangular matrices. It would
	 * be faster if assumes rectangular matrices.
	 */
	public static float[][] clone(float[][] input) {
		if (input == null) {
			return null;
		}
		float[][] out = new float[input.length][];
		for (int i=0; i<input.length; i++) {
			out[i] = new float[input[i].length];
			System.arraycopy(input[i],0,out[i],0,input[i].length);
		}
		return out;
	}

	/**
	 * Supports non-rectangular matrices. It would
	 * be faster if assumes rectangular matrices.
	 */
	public static double[][] clone(double[][] input) {
		if (input == null) {
			return null;
		}
		double[][] out = new double[input.length][];
		for (int i=0; i<input.length; i++) {
			out[i] = new double[input[i].length];
			System.arraycopy(input[i],0,out[i],0,input[i].length);
		}
		return out;
	}

	/**Support non-rectangular matrices. It would
	 * be faster if assumes rectangular matrices.
	 */
	//TODO: test it
	public static String[][] clone(String[][] input) {
		if (input == null) {
			return null;
		}
		String[][] out = new String[input.length][];
		for (int i=0; i<input.length; i++) {
			out[i] = new String[input[i].length];
			System.arraycopy(input[i],0,out[i],0,input[i].length);
		}
		return out;
	}

	public static double[] cloneRegionAsDouble(int[] input, int nbegin, int nend,
	int nneighborSamplesToInclude) {
		if (input == null) {
			return null;
		}
		int nrealBegin = nbegin - nneighborSamplesToInclude;
		if (nrealBegin < 0) {
			nrealBegin = 0;
		}
		int nrealEnd = nend + nneighborSamplesToInclude;
		if (nrealEnd > input.length-1) {
			nrealEnd = input.length-1;
		}
		double[] out = new double[nrealEnd - nrealBegin + 1];
		for (int i = 0; i < out.length; i++) {
			out[i] = input[nrealBegin + i];
		}
		return out;
	}

	public static double[] cloneAsDouble(int[] input) {
		if (input == null) {
			return null;
		}
		double[] out = new double[input.length];
		for (int i = 0; i < input.length; i++) {
			out[i] = input[i];
		}
		return out;
	}

	public static float[] cloneAsFloat(double[] input) {
		if (input == null) {
			return null;
		}
		float[] out = new float[input.length];
		for (int i = 0; i < out.length; i++) {
			out[i] = (float) input[i];
		}
		return out;
	}
	
	/**
	 * Supports non-rectangular matrices. It would
	 * be faster if assumes rectangular matrices.
	 */
	public static float[][] cloneAsFloat(double[][] input) {
		if (input == null) {
			return null;
		}
		float[][] out = new float[input.length][];
		for (int i=0; i<input.length; i++) {
			out[i] = new float[input[i].length];
			for (int j = 0; j < out.length; j++) {
				out[i][j]= (float) input[i][j];
			}
		}
		return out;
	}
	
	public static double[] cloneAsDouble(float[] input) {
		if (input == null) {
			return null;
		}
		double[] out = new double[input.length];
		for (int i = 0; i < input.length; i++) {
			out[i] = input[i];
		}
		return out;
	}

	public static int[] cloneAsInt(double[] input) {
		if (input == null) {
			return null;
		}
		int[] out = new int[input.length];
		for (int i = 0; i < input.length; i++) {
			out[i] = (int) input[i];
		}
		return out;
	}

	/**Support non-rectangular matrices. It would
	 * be faster if assumes rectangular matrices.
	 */
	public static double[][] cloneAsDouble(float[][] input) {
		if (input == null) {
			return null;
		}
		double[][] out = new double[input.length][];
		for (int i=0; i<input.length; i++) {
			out[i] = cloneAsDouble(input[i]);
		}
		return out;
	}

	/**
	 * Assume rectangular matrix
	 */
	 //to do use arrayCopy
	public static int[][] cloneAsInt(double[][] input) {
		if (input == null) {
			return null;
		}
		int[][] out = new int[input.length][input[0].length];
		for (int i=0; i<input.length; i++) {
			for (int j=0; j<input[0].length; j++) {
				out[i][j] = (int) input[i][j];
			}
		}
		return out;
	}

	/**
	 * Assume rectangular matrix
	 */
	 //to do use arrayCopy
	public static int[][] cloneAsInt(float[][] input) {
		if (input == null) {
			return null;
		}
		int[][] out = new int[input.length][input[0].length];
		for (int i=0; i<input.length; i++) {
			for (int j=0; j<input[0].length; j++) {
				out[i][j] = (int) input[i][j];
			}
		}
		return out;
	}

	/**Support non-rectangular matrices. It would
	 * be faster if assumes rectangular matrices.
	 */
	public static double[][] cloneAsDouble(int[][] input) {
		if (input == null) {
			return null;
		}
		double[][] out = new double[input.length][];
		for (int i=0; i<input.length; i++) {
			out[i] = cloneAsDouble(input[i]);
		}
		return out;
	}

} //end of class

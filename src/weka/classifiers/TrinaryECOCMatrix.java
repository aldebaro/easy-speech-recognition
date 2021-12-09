/*
 *    TrinaryECOCMatrix.java
 *    Copyright (C) 2001, Aldebaro Klautau
 */

package  weka.classifiers;

import  java.io.Serializable;
import  weka.core.SelectedTag;
import  weka.core.Tag;
import  weka.core.Instance;
import  weka.core.Utils;
import  edu.ucsd.asr.IO;
import edu.ucsd.asr.End;
import edu.ucsd.asr.Cloner;
import edu.ucsd.asr.Matrix;
import java.util.Random;

public class TrinaryECOCMatrix implements Serializable {

	static final long serialVersionUID = 715413943655495934L;

	public int[][] m_nmapFromPairOfClassesToBinaryClassifier;

	/** Coding matrix. First dimension is # of binary classifiers
	 *  and second dimension is the number of classes. */
	private int[][] m_ncodingMatrix;

	/** The error-correcting output code method to use */
	private int m_ErrorMode = ERROR_ALLPAIRS;

	/** File containing error coding matrix */
	private String m_errorCodingFileName = "not_used";

	/**
	 * For each class, the indices of their correspondent
	 * binary classifiers are pre-calculated and stored.
	 */
	 //first dimension is number of classes
	private int[][] m_npositiveBinaryClassifiersOfEachClass;
	private int[][] m_nnegativeBinaryClassifiersOfEachClass;
	/**
	 * Union of positive and negative binary classifiers.
	 */
	private int[][] m_nbinaryClassifiersOfEachClass;
	/**
	 * Pre-compute number of zero entries per class.
	 */
	private int[] m_nnumberOfZeroEntriesPerClass;

	 //first dimension is number of binary classifiers
	private int[][] m_npositiveClassesOfEachBinaryClassifier;
	private int[][] m_nnegativeClassesOfEachBinaryClassifier;

	/** The error correction modes */
	public final static int ERROR_ONEVSREST = 0;
	public final static int ERROR_ALLPAIRS = 1;
	public final static int ERROR_FROMFILE = 2;
	public final static int ERROR_GENERIC = 3;
	public final static int ERROR_ALPHA_VS_BETA = 4;
	public final static int ERROR_COMPLETE = 5;
	public final static int ERROR_SPARSE = 6;
	public final static int ERROR_DENSE = 7;
	public static final Tag[] TAGS_ERROR =  {
		new Tag(ERROR_ONEVSREST, "One versus rest"),
		new Tag(ERROR_ALLPAIRS, "All possible pairs"),
		new Tag(ERROR_FROMFILE, "Defined by file"),
		//new Tag(ERROR_GENERIC, "Uses constructor"),
		new Tag(ERROR_ALPHA_VS_BETA, "Alpha vs. beta"),
		new Tag(ERROR_COMPLETE, "Complete: 2^(n-1)-1 classifiers"),
		new Tag(ERROR_SPARSE, "Allwein's sparse"),
		new Tag(ERROR_DENSE, "Allwein's dense")
	};

	/**
	 * For alpha vs. beta coding matrices.
	 */
	private int m_nalpha;
	private int m_nbeta;

	public TrinaryECOCMatrix(int[][] codingMatrix) {
		m_ncodingMatrix = Cloner.clone(codingMatrix);
		//ak: good() is not properly implemented
		//the following matrix was invalid:
//1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 0 1 1 1 1 1 -1 0 0 0 0 -1 -1 0 0 -1 -1 -1 -1 -1 -1 -1
//-1 0 1 1 1 1 1 1 0 1 1 1 1 1 1 1 1 1 1 1 1 1 1 0 1 1 1 1 1 -1 1 1 1 1 -1 0 -1 -1 -1 -1 -1 -1 -1 -1 -1
//-1 -1 0 0 1 1 0 0 0 -1 0 0 1 1 1 1 0 1 1 1 1 1 1 1 1 1 1 1 1 0 1 1 1 1 -1 0 -1 -1 -1 -1 -1 -1 -1 -1 -1
//-1 -1 -1 -1 0 0 0 0 -1 -1 -1 0 1 1 0 1 -1 -1 0 1 1 0 1 0 1 1 1 1 1 1 1 1 1 1 0 0 0 0 -1 -1 0 -1 0 -1 -1
//-1 -1 -1 -1 -1 -1 -1 -1 -1 0 0 -1 -1 0 -1 0 -1 -1 -1 -1 0 -1 0 -1 -1 0 0 0 0 0 1 1 1 1 1 0 0 0 0 0 0 0 0 0 0
//-1 -1 -1 -1 -1 -1 -1 -1 -1 0 0 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 0 0 0 0 -1 0 1 1 1 1 1 1 1 0 1 0 0 -1 0
//-1 -1 0 -1 0 -1 -1 -1 -1 1 1 -1 0 -1 -1 -1 -1 -1 -1 0 -1 -1 -1 -1 -1 0 -1 -1 -1 -1 0 -1 0 0 0 0 0 0 0 1 1 1 0 0 1
//-1 -1 0 -1 0 -1 -1 -1 -1 1 0 -1 0 0 -1 -1 -1 -1 -1 0 -1 -1 -1 -1 -1 0 -1 -1 -1 -1 1 0 -1 0 -1 -1 -1 -1 -1 -1 -1 0 0 0 0
//-1 -1 1 0 0 0 -1 -1 -1 1 1 0 0 0 -1 -1 -1 -1 -1 0 -1 -1 -1 -1 -1 0 -1 -1 -1 -1 1 0 -1 -1 -1 -1 -1 -1 -1 -1 -1 0 0 0 1
//-1 -1 0 -1 0 0 0 0 -1 0 0 0 1 0 0 0 -1 0 0 0 0 0 0 0 0 0 0 0 0 -1 1 1 0 0 -1 0 -1 -1 -1 -1 -1 -1 0 0 -1
//		if (!good()) {
//			End.throwError("Invalid matrix !\n" + this);
//		}
		m_ErrorMode = ERROR_GENERIC;
		initialize ();
	}

	/**
	 * codingMatrixFileName is null if not needed
	 */
	public TrinaryECOCMatrix(int ncodingMatrixMode, int nnumberOfClasses,
	String codingMatrixFileName) {
		m_ErrorMode = ncodingMatrixMode;
			switch (m_ErrorMode) {
				case ERROR_FROMFILE:
				readMatrixFromFile(codingMatrixFileName);
				break;
				case ERROR_ALLPAIRS:
					createAllPairsMatrix(nnumberOfClasses);
					break;
				case ERROR_ONEVSREST:
					createOneVersusAllMatrix(nnumberOfClasses);
					break;
				case ERROR_COMPLETE:
					createCompleteMatrix(nnumberOfClasses);
					break;
				case ERROR_SPARSE:
					createSparseMatrix(nnumberOfClasses);
					break;
				case ERROR_DENSE:
					createDenseMatrix(nnumberOfClasses);
					break;
				default:
					End.throwError("Unrecognized correction code type");
			}
		initialize ();
		//IO.DisplayMatrix(m_ncodingMatrix);
		//System.exit(1);
	}

	public TrinaryECOCMatrix(int nnumberOfClasses, int alpha, int beta) {
		m_ErrorMode = ERROR_ALPHA_VS_BETA;
		m_nalpha = alpha;
		m_nbeta = beta;
		createAlphaVersusBetaMatrix(nnumberOfClasses, alpha, beta);
		initialize ();
	}

	public TrinaryECOCMatrix(String codingMatrixFileName) {
		m_ErrorMode = ERROR_FROMFILE;
		readMatrixFromFile(codingMatrixFileName);
		initialize ();
	}

	//needs to be called by every constructor
	private void initialize() {
		detectBinaryClassifiersOfEachClass ();
		detectClassesOfEachBinaryClassifier ();
	}


		/**
		 * Subclasses must allocate and fill these.
		 * First dimension is number of codes.
		 * Second dimension is number of classes.
		 */
		//protected int[][] m_ncodingMatrix;

		public int getNumberOfClasses () {
			return  m_ncodingMatrix[0].length;
		}

		public int getNumberOfBinaryClassifiers () {
			return  m_ncodingMatrix.length;
		}

		public int[][] getECOCCodingMatrixReference () {
			return  m_ncodingMatrix;
		}

		/**
		 * Returns the indices of the values set to true for this code,
		 * using 1-based indexing (for input to Weka's Range).
		 */
		public String getIndicesOfNegativeEntries (int which) {
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < m_ncodingMatrix[which].length; i++) {
				if (m_ncodingMatrix[which][i] == -1) {
					if (sb.length() != 0) {
						sb.append(',');
					}
					sb.append(i + 1);
				}
			}
			return  sb.toString();
		}

		/**
		 * Returns the indices of the values set to true for this code,
		 * using 1-based indexing (for input to Weka's Range).
		 */
		public String getIndicesOfPositiveEntries (int which) {
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < m_ncodingMatrix[which].length; i++) {
				if (m_ncodingMatrix[which][i] == 1) {
					if (sb.length() != 0) {
						sb.append(',');
					}
					sb.append(i + 1);
				}
			}
			return  sb.toString();
		}

		/**
		 * Returns the indices of the values set to 0 (don't care) for this code,
		 * using 1-based indexing (for input to Weka's Range).
		 */
		public String getIndicesOfZeroEntries (int which) {
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < m_ncodingMatrix[which].length; i++) {
				if (m_ncodingMatrix[which][i] == 0) {
					if (sb.length() != 0) {
						sb.append(',');
					}
					sb.append(i + 1);
				}
			}
			return  sb.toString();
		}

		/** Returns a human-readable representation of the codes. */
		//Notice it outputs the transpose of the code matrix.
		public String toString () {
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < m_ncodingMatrix[0].length; i++) {
				for (int j = 0; j < m_ncodingMatrix.length; j++) {
					//don't add space for first column
					if (j != 0) {
						sb.append(" ");
					}
					switch (m_ncodingMatrix[j][i]) {
						case 1:
							sb.append(" 1");
							break;
						case -1:
							sb.append("-1");
							break;
						case 0:
							sb.append(" 0");
							break;
					}
				}
				sb.append(IO.m_NEW_LINE);
			}
			return  sb.toString();
		}



	/** Constructs one-against-all code */
	private void createOneVersusAllMatrix(int numClasses) {
		if (numClasses == 2) {
			m_ncodingMatrix = new int[1][2];
			m_ncodingMatrix[0][0] = -1;
			m_ncodingMatrix[0][1] = 1;
			return;
		}
			m_ncodingMatrix = new int[numClasses][numClasses];
			for (int i = 0; i < m_ncodingMatrix.length; i++) {
				for (int j = 0; j < m_ncodingMatrix[0].length; j++) {
					m_ncodingMatrix[i][j] = (i == j) ? 1 : -1;
				}
			}
			//System.err.println("Code is one-against-rest with # classes = " + numClasses);
	}

	private void createCompleteMatrix(int numClasses) {
			long longWidth = (long)Math.pow(2, numClasses - 1) - 1;
			int width = -1;
			if (longWidth > Integer.MAX_VALUE) {
				System.err.println("Warning: number of classifiers for complete matrix = " +
				width + " is larger than maximum integer. Will use = " + Integer.MAX_VALUE);
				width = Integer.MAX_VALUE;
			} else {
				width = (int) longWidth;
			}
			m_ncodingMatrix = new int[width][numClasses];
			for (int j = 0; j < width; j++) {
				m_ncodingMatrix[j][0] = 1;
			}
			for (int i = 1; i < numClasses; i++) {
				int skip = (int) Math.pow(2, numClasses - (i + 1));
				for(int j = 0; j < width; j++) {
					if ((j / skip) % 2 != 0) {
						m_ncodingMatrix[j][i] = 1;
					} else {
						m_ncodingMatrix[j][i] = -1;
					}
				}
			}
			//System.err.println("Code:\n" + this);
	}


	/** Constructs all-pairs code */
	private void createAllPairsMatrix(int numClasses) {
			int nnumberOfPairs = (int) Utils.binomial(numClasses, 2);
			m_ncodingMatrix = new int[nnumberOfPairs][numClasses];

			m_nmapFromPairOfClassesToBinaryClassifier = new int[numClasses][numClasses];

			int ncurrentLine = 0;

			for (int i = 0; i < numClasses - 1; i++) {
				for (int j = i + 1; j < numClasses; j++) {
					//for a binary problem (only 1 classifier), make sure the
					//index 0 corresponds to the 'negative' class
					m_ncodingMatrix[ncurrentLine][i] = -1;
					m_ncodingMatrix[ncurrentLine][j] = 1;
					m_nmapFromPairOfClassesToBinaryClassifier[i][j] = ncurrentLine;
					m_nmapFromPairOfClassesToBinaryClassifier[j][i] = ncurrentLine;
					ncurrentLine++;
				}
			}

			for (int i = 0; i < numClasses; i++) {
				m_nmapFromPairOfClassesToBinaryClassifier[i][i] = -1;
			}
			//System.err.println("Code is all-pairs with # classes = " + numClasses +
			//" and # binary classifiers = " + m_ncodingMatrix.length);
	}

	public static int getNumberOfBinaryClassifiersInAlphaVersusBetaMatrix(int numClasses, int alpha, int beta) {

			int B =  (int) (Utils.binomial(numClasses,alpha+beta) * Utils.binomial(alpha+beta,alpha));
			//int[][] ncombinations = new int[B][numClasses];
			if (alpha == beta) {
				B /= 2;
			}
			return B;
	}


	/** Constructs alpha versus beta code, e.g., all-pairs is alpha = 1
	 *  and beta = 1. */
	private void createAlphaVersusBetaMatrix(int numClasses, int alpha, int beta) {
			//int nnumberOfPairs = (int) Utils.binomial(numClasses, 2);
			//m_ncodingMatrix = new int[nnumberOfPairs][numClasses];
			int[] v = new int[numClasses];
			for (int i = 0; i < v.length; i++) {
				v[i] = i;
			}
			int B =  (int) (Utils.binomial(numClasses,alpha+beta) * Utils.binomial(alpha+beta,alpha));
			//int[][] ncombinations = new int[B][numClasses];
			if (alpha != beta) {
				m_ncodingMatrix = new int[B][numClasses];
			} else {
				m_ncodingMatrix = new int[B/2][numClasses];
			}

			int[][] group1 = getNChooseKCombinations(v, alpha+beta);
			int ncurrentLine = 0;
			for (int i = 0; i < group1.length; i++) {
				int[][] group2 = getNChooseKCombinations(group1[i], alpha);
				int ndistinct = group2.length;
				if (alpha == beta) {
					ndistinct = group2.length / 2;
				}
				for (int j = 0; j < ndistinct; j++) {
					//ncombinations[ncurrentLine] = group2[j];
					//first assign all negatives
					for (int k = 0; k < group1[i].length; k++) {
						m_ncodingMatrix[ncurrentLine][group1[i][k]] = -1;
					}
					for (int k = 0; k < group2[j].length; k++) {
						m_ncodingMatrix[ncurrentLine][group2[j][k]] = 1;
					}
					ncurrentLine++;
				}
			}
			//System.err.println("Code is alpha versus beta with # classes = " + numClasses +
			//" and # binary classifiers = " + m_ncodingMatrix.length);
	}

	public static int[][] getNChooseKCombinations(int[] v, int m) {
		int n = v.length;
		if (m > n) {
			End.throwError("Can't do " + n + " choose " + m +
			" when " + m + " is bigger than " + n);
		}
		int[][] out = null;
		if (n == m) {
			out = new int[1][];
			out[0] = Cloner.clone(v);
			return out;
		} else if (m == 1) {
			out = new int[n][1];
			for (int i = 0; i < n; i++) {
				out[i][0] = v[i];
			}
			return out;
		}
		try {
			int B = (int) Utils.binomial(n,m);
			 out = new int[B][m];
			 if ( (m < n) && (m > 1)) {
				int currentLine = 0;
				for (int k = 0; k<=n-m; k++) {
					int[] vtail = new int[n-k-1];
					for (int i = 0; i < vtail.length; i++) {
						vtail[i] = v[k+1+i];
					}
					int[][] temp = getNChooseKCombinations(vtail,m-1);
					int r = temp.length;
					int c = temp[0].length;
					//pre-append to Q
					for (int i=0; i<r; i++) {
						out[currentLine][0]=v[k];
						for (int j=0; j<c; j++) {
							out[currentLine][j+1] = temp[i][j];
						}
						currentLine++;
					}
				}
			 }
		} catch (Exception e) {
			e.printStackTrace();
			IO.DisplayVector(v);
			End.exit("m = " + m + " and vector is ");
		}
		return out;
	}

	/** Constructs a code based on a file provided by user */
	private void readMatrixFromFile(String fileName) {
			float[][] fmatrix = IO.readTextFiletoFloatMatrix(fileName);
			//internally it adopts the transpose of the coding matrix
			m_ncodingMatrix = new int[fmatrix[0].length][fmatrix.length];
			for (int i = 0; i < fmatrix.length; i++) {
				for (int j = 0; j < fmatrix[0].length; j++) {
					if (fmatrix[i][j] == 1.0F) {
						m_ncodingMatrix[j][i] = 1;
					}
					else if (fmatrix[i][j] == 0.0F) {
						m_ncodingMatrix[j][i] = 0;
					}
					else if (fmatrix[i][j] == -1.0F) {
						m_ncodingMatrix[j][i] = -1;
					}
					else {
						End.throwError(fileName + " contains matrix with non-valid entry "
								+ fmatrix[i][j]);
					}
				}
			}
			if (!good()) {
				End.throwError(fileName + " contains invalid matrix\n" + this);
			}
			//System.err.println("Code read from file:\n" + this);
		}

		/**
		 * All-pairs doesn't pass test below. I should write my own code to
		 * check if there are identical columns or rows.
		 */
		private boolean good () {
			for (int i = 0; i < m_ncodingMatrix.length; i++) {
				for (int j = 0; j < m_ncodingMatrix[i].length; j++) {
					if ((m_ncodingMatrix[i][j] != 1) &&
					(m_ncodingMatrix[i][j] != -1) &&
					(m_ncodingMatrix[i][j] != 0)) {
						End.throwError("Matrix has entry different than 1,-1,0. The invalid entry is = " +
						m_ncodingMatrix[i][j]);
					}
				}
			}
//			boolean[] ninClass = new boolean[m_ncodingMatrix[0].length];
//			boolean[] ainClass = new boolean[m_ncodingMatrix[0].length];
//			for (int i = 0; i < ainClass.length; i++) {
//				ainClass[i] = true;
//			}
//			for (int i = 0; i < m_ncodingMatrix.length; i++) {
//				boolean ninCode = false;
//				boolean ainCode = true;
//				for (int j = 0; j < m_ncodingMatrix[i].length; j++) {
//					boolean current = false;
//					if (m_ncodingMatrix[i][j] == 1) {
//						current = true;
//					}
//					ninCode = ninCode || current;
//					ainCode = ainCode && current;
//					ninClass[j] = ninClass[j] || current;
//					ainClass[j] = ainClass[j] && current;
//				}
//				if (!ninCode || ainCode) {
//					System.out.println("HERE1");
//					return  false;
//				}
//			}
//			for (int j = 0; j < ninClass.length; j++) {
//				if (!ninClass[j] || ainClass[j]) {
//					//all-pairs is getting this message
//					System.out.println("HERE2");
//					return  false;
//				}
//			}
			return  true;
		}

	/** Write current coding matrix to file. */
	public void writeToFile (String fileName) throws Exception {
		IO.writeStringToFile(fileName, this.toString());
	}

	/** Write current coding matrix to file. */
	public void printTranspose () {
		//int[][] t = Matrix.transpose();
		IO.DisplayEntriesofMatrix(m_ncodingMatrix);
	}

	public boolean isAllPairs () {
		if (m_ErrorMode == ERROR_ALLPAIRS) {
			return true;
		} else {
			return false;
		}
	}

	public boolean doesItHaveEntriesEqualToZero() {
		switch (m_ErrorMode) {
			case ERROR_ALLPAIRS:
				if (getNumberOfClasses() <= 2) {
					return false;
				} else {
					return true;
				}
			case ERROR_ONEVSREST:
				return false;
			case ERROR_COMPLETE:
				return false;
			case ERROR_DENSE:
				return false;
			default:
				//check
				for (int i = 0; i < m_ncodingMatrix.length; i++) {
					for (int j = 0; j < m_ncodingMatrix[0].length; j++) {
						if (m_ncodingMatrix[i][j] == 0) {
							return true;
						}
					}
				}
				return false;
		}
	}

	/**
	 * Gets the error correction mode used. Will be one of
	 * ERROR_NONE, ERROR_RANDOM, or ERROR_EXHAUSTIVE.
	 *
	 * @return the current error correction mode.
	 */
	public SelectedTag getErrorCorrectionMode () {
		return  new SelectedTag(m_ErrorMode, TAGS_ERROR);
	}

	public String getErrorCodingFileName () {
		return  m_errorCodingFileName;
	}

	/**
	 * Sets the error correction mode used. Will be one of
	 * ERROR_NONE, ERROR_RANDOM, or ERROR_EXHAUSTIVE.
	 *
	 * @param newMethod the new error correction mode.
	 */
	public void setErrorCorrectionMode (SelectedTag newMethod) {
		if (newMethod.getTags() == TAGS_ERROR) {
			m_ErrorMode = newMethod.getSelectedTag().getID();
		}
	}

	public void setErrorCodingFileName (String fileName) {
		m_errorCodingFileName = fileName;
	}

	public double[] getHammingDistances (double[] dbinaryProbabilities) {
//		double[] d1 = {0.9, 0.3, 0.1};
//		int[][] n = {{0,1,0},{1,-1,0},{1,-1,-1}};
//		dbinaryProbabilities = d1;
//		m_ncodingMatrix = n;
		//ak: vou habilitar aqui pro Adalbery:
		System.out.println("Saidas:");
		IO.DisplayVector(dbinaryProbabilities);
		
		double[] dquantized = new double[dbinaryProbabilities.length];
		for (int i = 0; i < dquantized.length; i++) {
			dquantized[i] = (dbinaryProbabilities[i] <= 0.5) ? -1 : 1;
		}
		//ak
		IO.DisplayVector(dquantized);
		double[] dprob = new double[m_ncodingMatrix[0].length];
		for (int i = 0; i < dprob.length; i++) {
			for (int j = 0; j < dbinaryProbabilities.length; j++) {
					dprob[i] +=  1 - m_ncodingMatrix[j][i] * dquantized[j];
			}
			//only a scaling factor
			dprob[i] /= 2;
		}
		IO.DisplayVector(dprob);
		return dprob;
	}

	//get scores as in book by David:
	//number of times class won
	public double[] getNumberOfVictoriesPerClassGivenProbabilities (double[] dbinaryProbabilities) {
		double[] dquantized = new double[dbinaryProbabilities.length];
		for (int i = 0; i < dquantized.length; i++) {
			dquantized[i] = (dbinaryProbabilities[i] <= 0.5) ? -1 : 1;
		}
		double[] dvctories = new double[m_ncodingMatrix[0].length];
		for (int i = 0; i < dvctories.length; i++) {
			for (int j = 0; j < dbinaryProbabilities.length; j++) {
				if (m_ncodingMatrix[j][i] == dquantized[j]) {
					dvctories[i] ++;
				}
			}
		}
		//IO.DisplayVector(dprob);
		return dvctories;
	}

	//number of times class won
	public int[] getNumberOfVictoriesPerClassGivenRawScores (double[] dscores) {
		double[] dquantized = new double[dscores.length];
		for (int i = 0; i < dquantized.length; i++) {
			dquantized[i] = (dscores[i] <= 0) ? -1 : 1;
		}
		int[] nvctories = new int[m_ncodingMatrix[0].length];
		for (int i = 0; i < nvctories.length; i++) {
			for (int j = 0; j < dscores.length; j++) {
				if (m_ncodingMatrix[j][i] == dquantized[j]) {
					nvctories[i] ++;
				}
			}
		}
		//IO.DisplayVector(dprob);
		return nvctories;
	}

	/**
	 * Performance of binary classifiers:
	 * 1 - if correct,
	 * -1 - wrong
	 * 0 - class is unseen
	 */
	public int[] getBinaryResultsGivenRawScores (double[] dscores,
	int ncorrectLabel) {
		double[] dquantized = new double[dscores.length];
		for (int i = 0; i < dquantized.length; i++) {
			dquantized[i] = (dscores[i] <= 0) ? -1 : 1;
		}
		int[] nresults = new int[dscores.length];
		for (int j = 0; j < dscores.length; j++) {
			if (m_ncodingMatrix[j][ncorrectLabel] == 0) {
				nresults[j] = 0;
			} else if (m_ncodingMatrix[j][ncorrectLabel] == dquantized[j]) {
				nresults[j] = 1;
			} else {
				nresults[j] = -1;
			}
		}
		//IO.DisplayVector(dprob);
		return nresults;
	}


	/**
	 * Allocates space. Doesn't normalize.
	 */
	public double[] getSumPerClass (double[] dbinaryProbabilities) {
		double[] dprob = new double[m_ncodingMatrix[0].length];
		getSumPerClass (dbinaryProbabilities, dprob);
		return dprob;
	}

	/**
	 * In-place. Doesn't normalize.
	 */
	public void getSumPerClass (double[] dinputBinaryProbabilities,
	double[] doutputClassProbabilities) {
		double[] dprob = doutputClassProbabilities;
		for (int i = 0; i < dprob.length; i++) {
			for (int j = 0; j < m_npositiveBinaryClassifiersOfEachClass[i].length; j++) {
					dprob[i] += dinputBinaryProbabilities[m_npositiveBinaryClassifiersOfEachClass[i][j]];
			}
			for (int j = 0; j < m_nnegativeBinaryClassifiersOfEachClass[i].length; j++) {
					dprob[i] += 1 - dinputBinaryProbabilities[m_nnegativeBinaryClassifiersOfEachClass[i][j]];
			}
			dprob[i] += 0.5 * m_nnumberOfZeroEntriesPerClass[i];
		}
	}

	/**
	 * Uses only active binary classifiers. In-place. Doesn't normalize.
	 */
	public void getSumPerClass (double[] dinputBinaryProbabilities,
	double[] doutputClassProbabilities,
	boolean[] oisActive) {

//		System.out.println("HERE");
//		IO.DisplayVector(dinputBinaryProbabilities);
//		IO.DisplayVector(doutputClassProbabilities);
//		IO.DisplayVector(oisActive);

		double[] dprob = doutputClassProbabilities;
		for (int i = 0; i < dprob.length; i++) {
			//make sure it's initialized with 0's
			dprob[i] = 0.0;
			for (int j = 0; j < m_npositiveBinaryClassifiersOfEachClass[i].length; j++) {
				//System.out.print(" " + j + " " + i + " " + m_npositiveBinaryClassifiersOfEachClass[i][j]);
				if (oisActive[m_npositiveBinaryClassifiersOfEachClass[i][j]]) {
					dprob[i] += dinputBinaryProbabilities[m_npositiveBinaryClassifiersOfEachClass[i][j]];
				}
			}
			for (int j = 0; j < m_nnegativeBinaryClassifiersOfEachClass[i].length; j++) {
				if (oisActive[m_nnegativeBinaryClassifiersOfEachClass[i][j]]) {
					dprob[i] += 1 - dinputBinaryProbabilities[m_nnegativeBinaryClassifiersOfEachClass[i][j]];
				}
			}
			//ak not sure, what if the binary classifier correspondent
			//to entry 0 is not active?
			dprob[i] += 0.5 * m_nnumberOfZeroEntriesPerClass[i];
		}
	}

	//ak need to make it more efficient
	public double[] getPairwiseProbabilities (double[] dclassProbabilities) {
		double[] dbinaryProbabilities = new double[m_ncodingMatrix.length];
		for (int j = 0; j < dbinaryProbabilities.length; j++) {
			float sum_pos = 0;
			float sum_neg = 0;
			for (int i = 0; i < m_ncodingMatrix[0].length; i++) {
				if (m_ncodingMatrix[j][i] == 1) {
					sum_pos += dclassProbabilities[i];
				}
				else if (m_ncodingMatrix[j][i] == -1) {
					sum_neg += dclassProbabilities[i];
				}
			}
			dbinaryProbabilities[j] = sum_pos/(sum_pos + sum_neg);
		}
		return  dbinaryProbabilities;
	}

	public double[] getPairwiseSumOfProbabilities (double[] dclassProbabilities) {
		double[] dbinaryProbabilities = new double[m_ncodingMatrix.length];
		for (int j = 0; j < dbinaryProbabilities.length; j++) {
			float sum_pos = 0;
			float sum_neg = 0;
			for (int i = 0; i < m_ncodingMatrix[0].length; i++) {
				if (m_ncodingMatrix[j][i] == 1) {
					sum_pos += dclassProbabilities[i];
				}
				else if (m_ncodingMatrix[j][i] == -1) {
					sum_neg += dclassProbabilities[i];
				}
			}
			dbinaryProbabilities[j] = sum_pos + sum_neg;
		}
		return  dbinaryProbabilities;
	}

	//keep the classes correspondent to each binary classifiers
	private void detectClassesOfEachBinaryClassifier () {
		//first dimension is number of binary classifiers
		m_npositiveClassesOfEachBinaryClassifier = new int[m_ncodingMatrix.length][];
		m_nnegativeClassesOfEachBinaryClassifier = new int[m_ncodingMatrix.length][];

		for (int i = 0; i < m_ncodingMatrix.length; i++) {
			int npositive = 0;
			int nnegative = 0;
			for (int j = 0; j < m_ncodingMatrix[0].length; j++) {
				if (m_ncodingMatrix[i][j] != 0) {
					//find how many +1 and -1 exist for each classifier
					if (m_ncodingMatrix[i][j] == 1) {
						npositive++;
					} else if (m_ncodingMatrix[i][j] == -1) {
						nnegative++;
					}
				}
			}
			m_npositiveClassesOfEachBinaryClassifier[i] = new int[npositive];
			m_nnegativeClassesOfEachBinaryClassifier[i] = new int[nnegative];
			//now they work as indices
			npositive = 0;
			nnegative = 0;
			for (int j = 0; j < m_ncodingMatrix[0].length; j++) {
				if (m_ncodingMatrix[i][j] != 0) {
					if (m_ncodingMatrix[i][j] == 1) {
						m_npositiveClassesOfEachBinaryClassifier[i][npositive] = j;
						npositive++;
					} else if (m_ncodingMatrix[i][j] == -1) {
						m_nnegativeClassesOfEachBinaryClassifier[i][nnegative] = j;
						nnegative++;
					}
				}
			}
		}
	}


	//keep the binary classifiers used for each class
	//from MEHMM class: public void organizeEntriesPerState() {
	private void detectBinaryClassifiersOfEachClass () {
		//first dimension is number of states
		m_nbinaryClassifiersOfEachClass = new int[m_ncodingMatrix[0].length][];
		m_npositiveBinaryClassifiersOfEachClass = new int[m_ncodingMatrix[0].length][];
		m_nnegativeBinaryClassifiersOfEachClass = new int[m_ncodingMatrix[0].length][];
		m_nnumberOfZeroEntriesPerClass = new int[m_ncodingMatrix[0].length];

		for (int i = 0; i < m_ncodingMatrix[0].length; i++) {
			int nnonZero = 0;
			int npositive = 0;
			int nnegative = 0;
			for (int j = 0; j < m_ncodingMatrix.length; j++) {
				if (m_ncodingMatrix[j][i] != 0) {
					//find how many +1 and -1 exist for each class
					nnonZero++;
					if (m_ncodingMatrix[j][i] == 1) {
						npositive++;
					} else if (m_ncodingMatrix[j][i] == -1) {
						nnegative++;
					}
				} else {
					m_nnumberOfZeroEntriesPerClass[i]++;
				}
			}
			m_nbinaryClassifiersOfEachClass[i] = new int[nnonZero];
			m_npositiveBinaryClassifiersOfEachClass[i] = new int[npositive];
			m_nnegativeBinaryClassifiersOfEachClass[i] = new int[nnegative];
			//now nnonZero works as index
			nnonZero = 0;
			npositive = 0;
			nnegative = 0;
			for (int j = 0; j < m_ncodingMatrix.length; j++) {
				if (m_ncodingMatrix[j][i] == 1 || m_ncodingMatrix[j][i] == -1) {
					m_nbinaryClassifiersOfEachClass[i][nnonZero] = j;
					nnonZero++;
					if (m_ncodingMatrix[j][i] == 1) {
						m_npositiveBinaryClassifiersOfEachClass[i][npositive] = j;
						npositive++;
					} else if (m_ncodingMatrix[j][i] == -1) {
						m_nnegativeBinaryClassifiersOfEachClass[i][nnegative] = j;
						nnegative++;
					}
				}
			}
		}
		//consider case where there are no positive or negative entries
		for (int i = 0; i < m_ncodingMatrix[0].length; i++) {
			if (m_npositiveBinaryClassifiersOfEachClass[i] == null) {
				m_npositiveBinaryClassifiersOfEachClass[i] = new int[0];
			}
			if (m_nnegativeBinaryClassifiersOfEachClass[i] == null) {
				m_nnegativeBinaryClassifiersOfEachClass[i] = new int[0];
			}
		}
	}

	public double getSumForSpecificClass (double[] r, int nclassNumber) {
		double sum = 0;
		for (int i = 0; i < m_npositiveBinaryClassifiersOfEachClass[nclassNumber].length; i++) {
			int nbinaryClassifierNumber = m_npositiveBinaryClassifiersOfEachClass[nclassNumber][i];
			sum += r[nbinaryClassifierNumber];
		}
		for (int i = 0; i < m_nnegativeBinaryClassifiersOfEachClass[nclassNumber].length; i++) {
			int nbinaryClassifierNumber = m_nnegativeBinaryClassifiersOfEachClass[nclassNumber][i];
			sum += 1 - r[nbinaryClassifierNumber];
		}
		//ak sum 0 entries contribution ?
		sum += 0.5 * m_nnumberOfZeroEntriesPerClass[nclassNumber];
		return  sum;
	}

	//includes all pairs where at least m_nminimumNumberInActiveList elements are in nbestList
	public void setActiveClassifiers (int[] nbestList, boolean[] oactiveClassifiers, int nminimumNumberInActiveList) {
		for (int i = 0; i < m_ncodingMatrix.length; i++) {
			oactiveClassifiers[i] = false;
		}
		//for all binary classifiers
		for (int i = 0; i < m_ncodingMatrix.length; i++) {
			int nnumberOfHits = 0;
			for (int j = 0; j < nbestList.length; j++) {
				if (m_ncodingMatrix[i][nbestList[j]] != 0) {
					nnumberOfHits++;
				}
			}
			if (nnumberOfHits >= nminimumNumberInActiveList) {
				oactiveClassifiers[i] = true;
			}
		}
		//IO.DisplayVector(oactiveClassifiers);
	}

	//ak need to make it faster
	public double[] getMaximumOfEachPair (double[] dclassProbabilities) {
		double[] dbinaryProbabilities = new double[m_ncodingMatrix.length];
		for (int j = 0; j < dbinaryProbabilities.length; j++) {
			float sum_pos = 0;
			float sum_neg = 0;
			for (int i = 0; i < m_ncodingMatrix[0].length; i++) {
				if (m_ncodingMatrix[j][i] == 1) {
					sum_pos += dclassProbabilities[i];
				}
				else if (m_ncodingMatrix[j][i] == -1) {
					sum_neg += dclassProbabilities[i];
				}
			}
			//take maximum
			dbinaryProbabilities[j] = (sum_pos > sum_neg) ? sum_pos : sum_neg;
		}
		return  dbinaryProbabilities;
	}

	/**
	 * Assumes dweightsPerBinaryClassifier contains [0, 1] probabilities,
	 * where 0.5 is the threshold.
	 */
	public double[] getWeightedSumPerClassGivenProbabilities (double[] dbinaryProbabilities,
	double[] dweightsPerBinaryClassifier) {
		int nnumberOfClasses = m_ncodingMatrix[0].length;
		double[] dposterioriPerState = new double[nnumberOfClasses];
		for (int i = 0; i < dweightsPerBinaryClassifier.length; i++) {
			for (int j = 0; j < dposterioriPerState.length; j++) {
				if (m_ncodingMatrix[i][j] == 1) {
					dposterioriPerState[j] += dweightsPerBinaryClassifier[i] * dbinaryProbabilities[i];
				}
				else if (m_ncodingMatrix[i][j] == -1) {
					dposterioriPerState[j] += dweightsPerBinaryClassifier[i] * (1 - dbinaryProbabilities[i]);
				}
//				else {
//					dprob[j] += 0.5;
//				}
//				dposterioriPerState[i] += m_dweights[j] * r[j];
			}
		}
		return dposterioriPerState;
	}

	public double[] getWeightedSumPerClassGivenRawScores (double[] dbinaryScores,
	double[] dweightsPerBinaryClassifier) {
		int nnumberOfClasses = m_ncodingMatrix[0].length;
		double[] dtotalScorePerClass = new double[nnumberOfClasses];
		for (int i = 0; i < dweightsPerBinaryClassifier.length; i++) {
			for (int j = 0; j < dtotalScorePerClass.length; j++) {
				if (m_ncodingMatrix[i][j] == 1) {
					dtotalScorePerClass[j] += dweightsPerBinaryClassifier[i] * dbinaryScores[i];
				}
				else if (m_ncodingMatrix[i][j] == -1) {
					dtotalScorePerClass[j] += -1.0 * dweightsPerBinaryClassifier[i] * dbinaryScores[i];
				}
			}
		}
		return dtotalScorePerClass;
	}

	/**
	 * Assumes threshold = 0 and > 0 indicates + examples, and < 0 negative
	 */
	public double[] getSumPerClassGivenRawScores (double[] dbinaryScores) {
		int nnumberOfClasses = m_ncodingMatrix[0].length;
		double[] dtotalScorePerClass = new double[nnumberOfClasses];
		for (int i = 0; i < dbinaryScores.length; i++) {
			for (int j = 0; j < dtotalScorePerClass.length; j++) {
				if (m_ncodingMatrix[i][j] == 1) {
					dtotalScorePerClass[j] += dbinaryScores[i];
				}
				else if (m_ncodingMatrix[i][j] == -1) {
					dtotalScorePerClass[j] += -1.0 * dbinaryScores[i];
				}
			}
		}
		return dtotalScorePerClass;
	}

	/**
	 * General case that includes AdaBoost and SVM losses.
	 */
	public double[] getLossBasedDistancePerClass (double[] drawScores,
	LossDistortion L) {
		double[] doutputDistances = new double[m_ncodingMatrix[0].length];
		for (int i = 0; i < doutputDistances.length; i++) {
			for (int j = 0; j < m_npositiveBinaryClassifiersOfEachClass[i].length; j++) {
				doutputDistances[i] += L.loss(drawScores[m_npositiveBinaryClassifiersOfEachClass[i][j]]);
					//doutputDistances[i] += Math.exp(-1 * drawScores[m_npositiveBinaryClassifiersOfEachClass[i][j]]);
			}
			for (int j = 0; j < m_nnegativeBinaryClassifiersOfEachClass[i].length; j++) {
				doutputDistances[i] += L.loss(-1.0 * drawScores[m_nnegativeBinaryClassifiersOfEachClass[i][j]]);
					//doutputDistances[i] += Math.exp(drawScores[m_nnegativeBinaryClassifiersOfEachClass[i][j]]);
			}
			doutputDistances[i] += L.loss(0.0) * m_nnumberOfZeroEntriesPerClass[i];
		}
		return doutputDistances;
	}

	/**
	 * z=f(x)y and L(z) = exp(-z) for AdaBoost. L(0) = 1
	 */
	public double[] getAdaBoostDistancePerClass (double[] drawScores) {
		double[] doutputDistances = new double[m_ncodingMatrix[0].length];
		for (int i = 0; i < doutputDistances.length; i++) {
			for (int j = 0; j < m_npositiveBinaryClassifiersOfEachClass[i].length; j++) {
					doutputDistances[i] += Math.exp(-1 * drawScores[m_npositiveBinaryClassifiersOfEachClass[i][j]]);
			}
			for (int j = 0; j < m_nnegativeBinaryClassifiersOfEachClass[i].length; j++) {
					doutputDistances[i] += Math.exp(drawScores[m_nnegativeBinaryClassifiersOfEachClass[i][j]]);
			}
			doutputDistances[i] += m_nnumberOfZeroEntriesPerClass[i];
		}
		return doutputDistances;
	}

	/**
	 * z=f(x)y and L(z) = max{1-z, 0} for SVM. L(0) = 1
	 */
	public double[] getSVMDistancePerClass (double[] drawScores) {
		double[] doutputDistances = new double[m_ncodingMatrix[0].length];
		for (int i = 0; i < doutputDistances.length; i++) {
			for (int j = 0; j < m_npositiveBinaryClassifiersOfEachClass[i].length; j++) {
				double dloss = 1 - drawScores[m_npositiveBinaryClassifiersOfEachClass[i][j]];
				if (dloss < 0) {
					dloss = 0;
				}
				doutputDistances[i] += dloss;
			}
			for (int j = 0; j < m_nnegativeBinaryClassifiersOfEachClass[i].length; j++) {
				//loss = 1 - (-1*f(x))
				double dloss = 1 + drawScores[m_nnegativeBinaryClassifiersOfEachClass[i][j]];
				if (dloss < 0) {
					dloss = 0;
				}
				doutputDistances[i] += dloss;
			}
			doutputDistances[i] += m_nnumberOfZeroEntriesPerClass[i];
		}
		return doutputDistances;
	}


	/**
	 * z=f(x)y and L(z) = -z.
	 */
	public double[] getLzEqualTo1Minusz (double[] drawScores) {
		double[] doutputDistances = new double[m_ncodingMatrix[0].length];
		for (int i = 0; i < doutputDistances.length; i++) {
			for (int j = 0; j < m_npositiveBinaryClassifiersOfEachClass[i].length; j++) {
				doutputDistances[i] += -drawScores[m_npositiveBinaryClassifiersOfEachClass[i][j]];
			}
			for (int j = 0; j < m_nnegativeBinaryClassifiersOfEachClass[i].length; j++) {
				doutputDistances[i] += drawScores[m_nnegativeBinaryClassifiersOfEachClass[i][j]];
			}
			//not here L(0) = 0
			//doutputDistances[i] += m_nnumberOfZeroEntriesPerClass[i];
		}
		return doutputDistances;
	}

	/**
	 * Uses only active binary classifiers. In-place. Doesn't normalize.
	 * Assumes threshold = 0 and > 0 indicates + examples, and < 0 negative
	 */
	public void getSVMDistancePerClassForActiveClassifiers (double[] dinputBinaryScores,
	double[] doutputClassScores,
	boolean[] oisActive) {

		double[] dprob = doutputClassScores;
		for (int i = 0; i < dprob.length; i++) {
			//make sure it's initialized with 0's
			dprob[i] = 0.0;
			for (int j = 0; j < m_npositiveBinaryClassifiersOfEachClass[i].length; j++) {
				//System.out.print(" " + j + " " + i + " " + m_npositiveBinaryClassifiersOfEachClass[i][j]);
				if (oisActive[m_npositiveBinaryClassifiersOfEachClass[i][j]]) {
					double dloss = 1 - dinputBinaryScores[m_npositiveBinaryClassifiersOfEachClass[i][j]];
					if (dloss < 0) {
						dloss = 0;
					}
					dprob[i] += dloss;
				}
			}
			for (int j = 0; j < m_nnegativeBinaryClassifiersOfEachClass[i].length; j++) {
				if (oisActive[m_nnegativeBinaryClassifiersOfEachClass[i][j]]) {
					//loss = 1 - (-1*f(x))
					double dloss = 1 + dinputBinaryScores[m_nnegativeBinaryClassifiersOfEachClass[i][j]];
					if (dloss < 0) {
						dloss = 0;
					}
					dprob[i] += dloss;
				}
			}
			//ak not sure, what if the binary classifier correspondent
			//to entry 0 is not active?
			//dprob[i] += 0.5 * m_nnumberOfZeroEntriesPerClass[i];
		}

//		System.out.println("HERE");
//		IO.DisplayVector(dinputBinaryScores);
//		IO.DisplayVector(dprob);
//		IO.DisplayVector(oisActive);
	}

	public double[] getHammingDistancesForActiveClassifiers (double[] dbinaryProbabilities,
	boolean[] oisActive) {
//		double[] d1 = {0.9, 0.3, 0.1};
//		int[][] n = {{0,1,0},{1,-1,0},{1,-1,-1}};
//		dbinaryProbabilities = d1;
//		m_ncodingMatrix = n;
		//IO.DisplayVector(dbinaryProbabilities);
		double[] dquantized = new double[dbinaryProbabilities.length];
		for (int i = 0; i < dquantized.length; i++) {
			dquantized[i] = (dbinaryProbabilities[i] <= 0.5) ? -1 : 1;
		}
		//IO.DisplayVector(dquantized);
		double[] dprob = new double[m_ncodingMatrix[0].length];
		for (int i = 0; i < dprob.length; i++) {
			for (int j = 0; j < dbinaryProbabilities.length; j++) {
				if (oisActive[j]) {
					dprob[i] +=  1 - m_ncodingMatrix[j][i] * dquantized[j];
				}
			}
			//only a scaling factor
			dprob[i] /= 2;
		}
		//IO.DisplayVector(dprob);
		//System.exit(1);
		return dprob;
	}

	public double[] getWeightedHammingDistances(double[] dbinaryProbabilities,
	double[] dweights) {
//		double[] d1 = {0.9, 0.3, 0.1};
//		int[][] n = {{0,1,0},{1,-1,0},{1,-1,-1}};
//		dbinaryProbabilities = d1;
//		m_ncodingMatrix = n;
			//System.out.println("HERE");
		Utils.normalize(dweights);
		IO.DisplayVector(dweights);
		double[] dquantized = new double[dbinaryProbabilities.length];
		for (int i = 0; i < dquantized.length; i++) {
			dquantized[i] = (dbinaryProbabilities[i] <= 0.5) ? -1 : 1;
		}
		//IO.DisplayVector(dquantized);
		double[] dprob = new double[m_ncodingMatrix[0].length];
		for (int i = 0; i < dprob.length; i++) {
			for (int j = 0; j < dbinaryProbabilities.length; j++) {
					dprob[i] +=  dweights[j] * (1 - m_ncodingMatrix[j][i] * dquantized[j]);
			}
			//only a scaling factor
			dprob[i] /= 2;
		}
		IO.DisplayVector(dprob);
		//System.exit(1);
		return dprob;
	}

	public int getAlpha() {
		return m_nalpha;
	}

	public int getBeta() {
		return m_nbeta;
	}

	/**
	 * As in paper by Platt.
	 */
	 //old method, new is implemented in ScoreMultiClassClassifiers
	public int oldgetDDAGDecision (double[] dbinaryProbabilities) {
		//makes sense only for all-pairs
		if (m_ErrorMode != ERROR_ALLPAIRS) {
			return -1;
		}
//		double[] d1 = {0.9, 0.3, 0.1};
//		int[][] n = {{0,1,0},{1,-1,0},{1,-1,-1}};
//		dbinaryProbabilities = d1;
//		m_ncodingMatrix = n;
		//IO.DisplayVector(dbinaryProbabilities);
		//double[] dquantized = new double[dbinaryProbabilities.length];
		//for (int i = 0; i < dquantized.length; i++) {
		//	dquantized[i] = (dbinaryProbabilities[i] <= 0.5) ? -1 : 1;
		//}
		int nnumberOfClasses = m_ncodingMatrix[0].length;
		int nnumberOfBinaryClassifiers = m_ncodingMatrix.length;
		boolean[] owasEliminated = new boolean[nnumberOfClasses];
		//IO.DisplayVector(dquantized);
		for (int i = 0; i < nnumberOfBinaryClassifiers; i++) {
			//find pair of classes for given binary classifier
			int npositiveClassIndex = -1;
			int nnegativeClassIndex = -1;
			for (int j = 0; j < nnumberOfClasses; j++) {
				if (m_ncodingMatrix[i][j] == 1) {
					npositiveClassIndex = j;
				}
				if (m_ncodingMatrix[i][j] == -1) {
					nnegativeClassIndex = j;
				}
				if (nnegativeClassIndex != -1 && npositiveClassIndex != -1) {
					break;
				}
			}
			if (owasEliminated[npositiveClassIndex] || owasEliminated[nnegativeClassIndex]) {
				continue;
			}
			//both classes were not eliminated yet, so eliminate one
			if (dbinaryProbabilities[i] > 0.5) {
				//positive class wins
				owasEliminated[nnegativeClassIndex] = true;
			} else {
				owasEliminated[npositiveClassIndex] = true;
			}
		}

		//return only class that survived
		int nsurvivor = -1;
		for (int i = 0; i < owasEliminated.length; i++) {
			if (!owasEliminated[i]) {
				nsurvivor = i;
				break;
			}
		}

		return nsurvivor;
	}

	/**
	 * Used by SetOfMatrixEncodedHMM's
	 */
	public float[] calculateNormalizationFactors(float[][] foutputScores) {
		float sum_pos = 0;
		float sum_neg = 0;
		float[] fnormalizationFactors = new float[foutputScores.length];
		//for all frames
		for (int i = 0; i < fnormalizationFactors.length; i++) {
			float[] fbinaryProbabilities = foutputScores[i];
			int nnumberOfBinaryClassifiers = fbinaryProbabilities.length;
			for (int j = 0; j < nnumberOfBinaryClassifiers; j++) {
				for (int k = 0; k < m_npositiveClassesOfEachBinaryClassifier[j].length; k++) {
					int nstateNumber = m_npositiveClassesOfEachBinaryClassifier[j][k];
					sum_pos += fbinaryProbabilities[j];
				}
				for (int k = 0; k < m_nnegativeClassesOfEachBinaryClassifier[j].length; k++) {
					int nstateNumber = m_nnegativeClassesOfEachBinaryClassifier[j][k];
					sum_neg += 1 - fbinaryProbabilities[j];
				}
			}
			fnormalizationFactors[i] = sum_pos + sum_neg;
		}
		return fnormalizationFactors;
	}

	public float getScoreOfGivenClass(int i, float[][] foutputScores, int nclassNumber) {
		float floss = 0;
		int[] ntemp = m_npositiveBinaryClassifiersOfEachClass[nclassNumber];
		for (int z = 0; z < ntemp.length; z++) {
			floss += foutputScores[i][ntemp[z]];
		}
		ntemp = m_nnegativeBinaryClassifiersOfEachClass[nclassNumber];
		for (int z = 0; z < ntemp.length; z++) {
			floss += (1 - foutputScores[i][ntemp[z]]);
		}
		return floss;
	}

	public String getIdentifierOfGivenClass(int nclassNumber) {
		StringBuffer stringBuffer = new StringBuffer("p");
		for (int i = 0; i < m_npositiveBinaryClassifiersOfEachClass[nclassNumber].length; i++) {
			stringBuffer.append("_" + (1+m_npositiveBinaryClassifiersOfEachClass[nclassNumber][i]));
		}
		stringBuffer.append("_n");
		for (int i = 0; i < m_nnegativeBinaryClassifiersOfEachClass[nclassNumber].length; i++) {
			stringBuffer.append("_" + (1+m_nnegativeBinaryClassifiersOfEachClass[nclassNumber][i]));
		}
		return stringBuffer.toString();
	}

	public String getIdentifierOfGivenBinaryClassifier(int nbinaryClassifierNumber) {
		StringBuffer stringBuffer = new StringBuffer("p");
		for (int i = 0; i < m_npositiveClassesOfEachBinaryClassifier[nbinaryClassifierNumber].length; i++) {
			stringBuffer.append("_" + (1+m_npositiveClassesOfEachBinaryClassifier[nbinaryClassifierNumber][i]));
		}
		stringBuffer.append("_n");
		if (m_ErrorMode == TrinaryECOCMatrix.ERROR_ONEVSREST) {
			//avoid listing all classes but 1
			stringBuffer.append("_rest");
		} else {
			for (int i = 0; i < m_nnegativeClassesOfEachBinaryClassifier[nbinaryClassifierNumber].length; i++) {
				stringBuffer.append("_" + (1+m_nnegativeClassesOfEachBinaryClassifier[nbinaryClassifierNumber][i]));
			}
		}
		return stringBuffer.toString();
	}

	private void createDenseMatrix(int numCl) {
		Random random = new Random(0);
//  int numCl, numMach;
//  int i, j, k, l, r;
//  int **bestMatrix, **curMatrix;
//  int bestMinRho = 0, idCols, possBad, curRho, curBestRho;
//  int foundPos, foundNeg, foundNz;

		//srandom(0);

	//numCl = atoi(argv[1]);
		int numMach = (int) Math.ceil(10.0*(Math.log(numCl)/Math.log(2.0)));

	//printf("%d %d\n", numMach, numCl);
	int[][] curMatrix = new int[numCl][numMach];
	int[][] bestMatrix = new int[numCl][numMach];
	int curBestRho;
	int bestMinRho = 0;

//  curMatrix = (int **)malloc(numCl*sizeof(int *));
//  for (i = 0; i < numCl; i++) {
//    curMatrix[i] = (int *)malloc(numMach*sizeof(int));
//  }
//
//  bestMatrix = (int **)malloc(numCl*sizeof(int *));
//  for (i = 0; i < numCl; i++) {
//    bestMatrix[i] = (int *)malloc(numMach*sizeof(int));
//  }

	for (int i = 0; i < 10000; i++) {
		for (int j = 0; j < numCl; j++) {
			for (int k = 0; k < numMach; k++) {
				/* Generate the random matrix */
				long r = random.nextLong();
				curMatrix[j][k] = (r <= 0) ? -1 : 1;
			}
		}

		boolean idCols = true;
		/* Check for identical columns */
		for (int j = 0; j < numMach-1; j++) {
			for (int k = j+1; k < numMach; k++) {
				//for each pair of columns
				idCols = true;
				for (int l = 0; l < numCl; l++) {
					if (curMatrix[l][j] != curMatrix[l][k]) {
						idCols = false;
						break;
					}
				}
				if (idCols) {
					idCols = true;
					//stop loops
					j = numMach-1;
					k = numMach;
				}
			}
		}

		/* If no identical columns, compute rho */
		if (!idCols) {
			curBestRho = numMach;
			for (int j = 0; j < numCl-1; j++) {
				for (int k = j+1; k < numCl; k++) {
					int curRho = 0;
					for (int l = 0; l < numMach; l++) {
						if (curMatrix[j][l] != curMatrix[k][l] ) {
							curRho ++;
						}
					}
					if (curRho < curBestRho) {
						curBestRho = curRho;
					}
				}
			}

			/* Copy Matrix, If Necessary */
			if (curBestRho > bestMinRho) {
				bestMinRho = curBestRho;
				for (int j = 0; j < numCl; j++) {
					for (int k = 0; k < numMach; k++) {
						bestMatrix[j][k] = curMatrix[j][k];
					}
				}
			}
		}
	}
	if (bestMinRho == 0) {
		End.throwError("Could not find valid dense matrix! Maybe the number of classes = " +
		numCl + " is too small!");
	} else {
		m_ncodingMatrix = Matrix.transpose(bestMatrix);
	}


	}

/* This is a C program to make dense codes a la Allwein, Schapire, and Singer
	 No error checking.  You give it the number of classes. */

	 private void createSparseMatrix(int numCl) {
		if (numCl <= 2) {
			Exception e = new Exception("Cannot construct sparse matrix for only " +
			numCl + " classes!");
			e.printStackTrace();
			return;
		}
		Random random = new Random(0);
//  int numCl, numMach;
//  int i, j, k, l, r;
//  int **bestMatrix, **curMatrix;
//  int bestMinRho = 0, idCols, possBad, curRho, curBestRho;
//  int foundPos, foundNeg, foundNz;

		//srandom(0);

	//numCl = atoi(argv[1]);
		int numMach = (int) Math.ceil(15.0*(Math.log(numCl)/Math.log(2.0)));

	//printf("%d %d\n", numMach, numCl);
	int[][] curMatrix = new int[numCl][numMach];
	int[][] bestMatrix = new int[numCl][numMach];
	int curBestRho;
	int bestMinRho = 0;

//  curMatrix = (int **)malloc(numCl*sizeof(int *));
//  for (i = 0; i < numCl; i++) {
//    curMatrix[i] = (int *)malloc(numMach*sizeof(int));
//  }
//
//  bestMatrix = (int **)malloc(numCl*sizeof(int *));
//  for (i = 0; i < numCl; i++) {
//    bestMatrix[i] = (int *)malloc(numMach*sizeof(int));
//  }

	boolean possBad = true;
	for (int i = 0; i < 10000; i++) {
		/* We ignore repeat columns */
		for (int j = 0; j < numMach; j++) {
			possBad = true;
			//IO.showCounter(j);
			while (possBad) {
				boolean foundPos = false;
				boolean foundNeg = false;
				for (int k = 0; k < numCl; k++) {
					long r = random.nextLong();
					if (r <= 0) {
						curMatrix[k][j] = 0;
					} else if (r >= Long.MAX_VALUE/2) {
						curMatrix[k][j] = 1;
						foundPos = true;
					} else {
						curMatrix[k][j] = -1;
						foundNeg = true;
					}
				}
				if (foundPos && foundNeg) {
					possBad = false;
				}
			}
		}

		//ak
		//IO.DisplayMatrix(curMatrix);

		/* Check for all zero rows */
		if (!possBad) {
			for (int j = 0; j < numCl; j++) {
				boolean foundNz = false;
				for (int k = 0; k < numCl; k++) {
					if (curMatrix[j][k] != 0) {
						foundNz = true;
						k = numCl;
					}
				}
				if (!foundNz) {
					possBad = true;
					j = numMach;
				}
			}
		}

		/* If matrix is OK, compute rho */
		if (!possBad) {
			curBestRho = 2*numMach;
			for (int j = 0; j < numCl-1; j++) {
				for (int k = j+1; k < numCl; k++) {
					int curRho = 0;
					for (int l = 0; l < numMach; l++) {
						if (curMatrix[j][l] == 0 || curMatrix[k][l] == 0) {
							curRho += 1;
						} else if (curMatrix[j][l] != curMatrix[k][l]) {
							curRho += 2;
						}
					}
					if (curRho < curBestRho) {
						curBestRho = curRho;
					}
				}
			}

			/* Copy Matrix, If Necessary */
			if (curBestRho > bestMinRho) {
				bestMinRho = curBestRho;
				for (int j = 0; j < numCl; j++) {
					for (int k = 0; k < numMach; k++) {
						bestMatrix[j][k] = curMatrix[j][k];
					}
				}
			}
		}
	}
	m_ncodingMatrix = Matrix.transpose(bestMatrix);
}


	public static void main(String[] args) {

//		Random random = new Random();
//		int n = 0;
//		int t = 10000000;
//		for (int i = 0; i < t; i++) {
//			long lx = random.nextLong();
//			if (lx > 0) { //Long.MAX_VALUE / 2
//				n++;
//			}
//		}
//		System.out.println("n = " + ( (double) n / t));
//		System.exit(1);


		if (args.length != 1) {
			System.out.println("Usage: <number of classes>");
			System.out.println("Prints all matrices to stdout");
			System.exit(1);
		}
		int nclasses = Integer.parseInt(args[0]);
		TrinaryECOCMatrix m = null;

		//print whole matrix or only number of binary classifiers
		if (true) {
			//number of binary classifiers
			m = new TrinaryECOCMatrix(TrinaryECOCMatrix.ERROR_ONEVSREST, nclasses, null);
			System.out.println("one-vs-rest\n" + m.getNumberOfBinaryClassifiers());

			m = new TrinaryECOCMatrix(TrinaryECOCMatrix.ERROR_ALLPAIRS, nclasses, null);
			System.out.println("all-pairs\n" + m.getNumberOfBinaryClassifiers());
			//print all-pairs matrix
			System.out.println("all-pairs\n" + m.toString());

			System.out.println("complete\n" + ((long) (Math.pow(2, nclasses - 1) - 1.0) ));

			double x = nclasses / 2.0;
			int nalpha = (int) x;
			int nbeta = (int) x;
			if ( nalpha * 2.0 != nclasses) {
				nalpha++;
			}

			System.out.println("alpha=" + nalpha + " vs. beta=" + nbeta +" \n" + getNumberOfBinaryClassifiersInAlphaVersusBetaMatrix(nclasses, nalpha, nbeta));

			System.out.println("alpha=2 vs. beta=1\n" + getNumberOfBinaryClassifiersInAlphaVersusBetaMatrix(nclasses, 2, 1));

			m = new TrinaryECOCMatrix(TrinaryECOCMatrix.ERROR_SPARSE, nclasses, null);
			System.out.println("sparse\n" + m.getNumberOfBinaryClassifiers());

			m = new TrinaryECOCMatrix(TrinaryECOCMatrix.ERROR_DENSE, nclasses, null);
			System.out.println("dense\n" + m.getNumberOfBinaryClassifiers());
		} else {
			//whole matrix
			m = new TrinaryECOCMatrix(TrinaryECOCMatrix.ERROR_ONEVSREST, nclasses, null);
			System.out.println("one-vs-rest\n" + m.toString());

			m = new TrinaryECOCMatrix(TrinaryECOCMatrix.ERROR_ALLPAIRS, nclasses, null);
			System.out.println("all-pairs\n" + m.toString());

			m = new TrinaryECOCMatrix(TrinaryECOCMatrix.ERROR_COMPLETE, nclasses, null);
			System.out.println("complete\n" + m.toString());

			double x = nclasses / 2.0;
			int nalpha = (int) x;
			int nbeta = (int) x;
			if ( nalpha * 2.0 != nclasses) {
				nalpha++;
			}

			m = new TrinaryECOCMatrix(nclasses, nalpha, nbeta);
			System.out.println("alpha=" + nalpha + " vs. beta=" + nbeta +" \n" + m.toString());

			m = new TrinaryECOCMatrix(nclasses, 2, 1);
			System.out.println("alpha=2 vs. beta=1\n" + m.toString());

			m = new TrinaryECOCMatrix(TrinaryECOCMatrix.ERROR_SPARSE, nclasses, null);
			System.out.println("sparse\n" + m.toString());

			m = new TrinaryECOCMatrix(TrinaryECOCMatrix.ERROR_DENSE, nclasses, null);
			System.out.println("dense\n" + m.toString());
		}
	}

}
//
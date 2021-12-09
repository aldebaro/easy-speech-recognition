package weka.classifiers;

import java.io.*;
import weka.core.*;
import weka.filters.*;
import edu.ucsd.asr.*;
import java.util.*;
import jmat.data.Matrix;
import jmat.data.matrixDecompositions.CholeskyDecomposition;

/**
 * Title:        Spock
 * Description:  Speech recognition
 * Copyright:    Copyright (c) 2001
 * Company:      UCSD
 * @author       Aldebaro Klautau
 * @version 4.0
 *
 * Reference: Rifkin's Ph.D. thesis, MIT.
 *
 * See also RLSC, which supports multi-class classification.
 */

 //I use the kernel calculation of SMO (I should have a class Kernel...)
 //therefore I have a SMO member. For lazyness I get the configuration from
 //this SMO to specify the kernel (gamma for RBF). RLSC has a parameter lambda
 //when we solve:
 // (K + lambda N I) c = y
 // where K is the kernel matrix, I the identity, N # of train examples, y
 // are the +/-1 labels and c are the weights.
 //Here I will ask the user to specify lambda * N = C, and get the C from the
 //SMO configuration. Using C = 1 corresponds to lambda = 1/N.
public class BinaryRLSC extends Classifier implements OptionHandler, RawScorer {

	protected boolean m_oareThereMissingAttributes = true;

	protected Instances m_instances;

	//fields below are important to keep compatibility with SMO
	/** The filter used to make attributes numeric. */
	protected NominalToBinaryFilter m_NominalToBinary;

	/** The filter used to normalize all values. */
	private Filter m_Normalization;
	//private NormalizationFilter m_Normalization;

	/** The filter used to get rid of missing values. */
	protected ReplaceMissingValuesFilter m_Missing;

	/** True if we want to normalize */
	protected boolean m_Normalize = true;

	/** Normalization type: original used [0, 1] and new G(0,1) */
	protected boolean m_ouseOriginalNormalization = true;

	/** Only numeric attributes in the dataset? */
	protected boolean m_onlyNumeric;

	private SMO m_smo = new SMO();

	private double[] m_dweights;

	/**
	 * Builds the classifiers.
	 *
	 * @param insts the training data.
	 * @exception Exception if a classifier can't be built
	 */
	public void buildClassifier (Instances inputData) throws Exception {

		if (inputData.checkForStringAttributes()) {
			throw new Exception("Can't handle string attributes!");
		}
		if (inputData.classAttribute().isNumeric()) {
			throw new Exception("SMO can't handle a numeric class!");
		}
		if (inputData.classIndex() != inputData.numAttributes()-1) {
			throw new Exception("Class must be last attribute!");
		}
		if (inputData.numClasses() != 2) {
			throw new Exception("BinaryRLSC supports only binary problems. The number of classes = " +
			inputData.numClasses() + " is not valid!");
		}

		//get configuration
		m_Normalize = m_smo.getNormalizeData();
		m_ouseOriginalNormalization = m_smo.getUseOriginalNormalization();

		Instances instances = new Instances(inputData);

		m_onlyNumeric = true;
		for (int i = 0; i < instances.numAttributes(); i++) {
			if (i != instances.classIndex()) {
				if (!instances.attribute(i).isNumeric()) {
					m_onlyNumeric = false;
					break;
				}
			}
		}

		if (m_oareThereMissingAttributes) {
			m_Missing = new ReplaceMissingValuesFilter();
			m_Missing.setInputFormat(instances);
			instances = Filter.useFilter(instances, m_Missing);
		} else {
			m_Missing = null;
		}

		if (m_Normalize) {
			if (m_ouseOriginalNormalization) {
				m_Normalization = new NormalizationFilter();
			} else {
				//use 0 mean and 1 variance
					if (m_oareThereMissingAttributes) {
						m_Normalization = new StandardizeFilter();
					} else {
					  m_Normalization = new FastStandardizeFilter();
					}
			}
			m_Normalization.setInputFormat(instances);
			instances = Filter.useFilter(instances, m_Normalization);
		} else {
			m_Normalization = null;
		}

		if (!m_onlyNumeric) {
			m_NominalToBinary = new NominalToBinaryFilter();
			m_NominalToBinary.setInputFormat(instances);
			instances = Filter.useFilter(instances, m_NominalToBinary);
		} else {
			m_NominalToBinary = null;
		}

		//calculate kernel matrix
		Matrix kernelMatrix = new Matrix(calculateKernelMatrix(instances));
		int N = kernelMatrix.getRowDimension();
		//organize output labels as vector
		Matrix labelsVector = new Matrix(composeVectorWithLabels(instances));

		//invert matrix using QR decomposition
		//long t1 = System.currentTimeMillis();
		//Matrix solution2 = kernelMatrix.plus(Matrix.identity(N,N)).solve(labelsVector);
		//long t2 = System.currentTimeMillis();
		//System.out.println("Time = " + ( (t2-t1)/1000.0 ) + " seconds.");

		//t1 = System.currentTimeMillis();
		double lambdaTimesN = m_smo.getC();

		//N = 5;
		//IO.DisplayMatrix(Matrix.identity(N,N).times(lambdaTimesN).getArrayCopy());
		//System.exit(1);

		CholeskyDecomposition choleskyDecomposition = new CholeskyDecomposition(kernelMatrix.plus(Matrix.identity(N,N).times(lambdaTimesN)));
		//previous code with fixed lambda = N
		//CholeskyDecomposition choleskyDecomposition = new CholeskyDecomposition(kernelMatrix.plus(Matrix.identity(N,N)));
		Matrix solution = choleskyDecomposition.solve(labelsVector);
		//t2 = System.currentTimeMillis();
		//System.out.println("Time = " + ( (t2-t1)/1000.0 ) + " seconds.");
		if (!choleskyDecomposition.isSPD()) {
			//this should never happen...
			System.err.println("Kernel matrix + identity matrix is not symmetric and positive definite." +
			" Cannot use Cholesky decomposition! I am using QR decomposition");
			solution = kernelMatrix.plus(Matrix.identity(N,N)).solve(labelsVector);
		}
		//to get max abs error:
		//IO.DisplayMatrix(solution2.minus(solution).abs().max().max().getArrayCopy());

		//keep the instances and their weights
		m_instances = instances;
		m_dweights = solution.getColumnArrayCopy(0);

//		double[] dtemp = solution2.getColumnArrayCopy(0);
//		double dmax = -1e30;
//		for (int i = 0; i < dtemp.length; i++) {
//			double de = dtemp[i]-m_dweights[i];
//			if (de > dmax) {
//				dmax = de;
//			}
//			System.out.print(de + " ");
//		}
//		System.out.print("\n Max error = " + dmax);

		//IO.DisplayVector(m_dweights);
	}

	public static float[] composeVectorWithLabels(Instances instances) {
		int N = instances.numInstances();
		float[] flabels = new float[N];
		//class with index label = 1 is positive, and 0 is negative
		for (int i = 0; i < N; i++) {
			double dclass = instances.instance(i).classValue();
			if (dclass == 0) {
				flabels[i] = -1;
			} else if (dclass == 1) {
				flabels[i] = 1;
			} else {
				End.throwError("Found instance with class index = " + dclass +
				" while expecting only 0 or 1");
			}
		}
		return flabels;
	}

	private float[][] calculateKernelMatrix(Instances instances) {
		int N = instances.numInstances();
		float[][] fkernelMatrix = new float[N][N];
		for (int i = 0; i < N; i++) {
			Instance x = instances.instance(i);
			for (int j = 0; j <= i; j++) {
				Instance y = instances.instance(j);
				//System.out.println("X="+x);
				//System.out.println("Y="+y);
				fkernelMatrix[i][j] = (float) m_smo.calculateKernel(x, y);
				fkernelMatrix[j][i] = fkernelMatrix[i][j];
			}
		}
		return fkernelMatrix;
	}

	public double classifyInstance (Instance inst) throws Exception {
		double dscore = getRawScore (inst);
		//index 0 correspond to a negative label
		return (dscore < 0) ? 0 : 1;
	}

	public double getRawScore (Instance inst) {
		try {
			// Filter instance
			if (m_oareThereMissingAttributes) {
				m_Missing.input(inst);
				m_Missing.batchFinished();
				inst = m_Missing.output();
			}

			if (m_Normalize) {
				m_Normalization.input(inst);
				m_Normalization.batchFinished();
				inst = m_Normalization.output();
			}

			if (!m_onlyNumeric) {
				m_NominalToBinary.input(inst);
				m_NominalToBinary.batchFinished();
				inst = m_NominalToBinary.output();
			}
		} catch (Exception e) {
			System.err.println("Problem filtering instance in BinaryRLSC!");
			e.printStackTrace();
		}

		int N = m_instances.numInstances();
		double dscore = 0;
		for (int i = 0; i < N; i++) {
			Instance y = m_instances.instance(i);
			//System.out.println("X="+x);
			//System.out.println("Y="+y);
			double dkernel = m_smo.calculateKernel(inst, y);
			dscore += m_dweights[i] * dkernel;
		}
		return dscore;
	}

	public boolean getareThereMissingAttributes() {
		return m_oareThereMissingAttributes;
	}

	public void setareThereMissingAttributes(boolean oareThereMissingAttributes) {
		m_oareThereMissingAttributes = oareThereMissingAttributes;
	}

	public Enumeration listOptions () {
		Vector vec = new Vector(10);
		vec.addElement(new Option("\tTurn on global normalization (allows using a shared cache " +
		"to speed up computations, but performance may improve if each binary classifier has its own normalization factor",
				"G", 0, "-G"));
		vec.addElement(new Option("\tThere are no missing attributes.\n"
			+"\t(If this flag is set, training and test will go faster, but if there are missing attributes, training will fail).",
			"M", 0,"-M"));
		vec.addElement(new Option("\tUse SVMTorch instead of Weka's SMO.\n",
			"H", 0,"-H"));
		vec.addElement(new Option("\tUse cross-validation to find the best SVM parameters.\n",
			"K", 0,"-K"));
//		Enumeration enume = super.listOptions();
//		while (enume.hasMoreElements()) {
//			vec.addElement(enume.nextElement());
//		}
		return vec.elements();
	}

	public void setOptions (String[] options) throws Exception {
		//I use -M for the cache size in SMO, but the cache is not used
		setareThereMissingAttributes(! Utils.getFlag('M', options));
		((OptionHandler) m_smo).setOptions(options);
		m_smo.setCacheSize(3);
	}

	public String[] getOptions () {
		String[] superOptions = ((OptionHandler) m_smo).getOptions();
		String[] options = new String[superOptions.length + 3];
		int ncurrent = 0;
		if (!getareThereMissingAttributes()) {
			options[ncurrent++] = "-M";
		}
		if (ncurrent > 0) {
			System.arraycopy(superOptions, 0, options, ncurrent, superOptions.length);
			return options;
		} else {
			//there are only options from super class
			return superOptions;
		}
	}

	/**
	 * Prints out the classifier.
	 *
	 * @return a description of the classifier as a string
	 */
	public String toString() {

		StringBuffer text = new StringBuffer();
		int printed = 0;

		if (m_dweights == null) {
			return "BinaryRLSC: No model built yet.";
		}
		try {
			text.append("BinaryRLSC\n\n");
			text.append("SMO options: " + m_smo.getOptionsAsString() + "\n");
			text.append("Weights for instance # 0 to " + m_instances.numInstances() + "\n");
			int nnumberOfPositiveWeights = 0;
			int nnumberOfNegativeWeights = 0;
			for (int i = 0; i < m_dweights.length; i++) {
				text.append("[" + i + "] = " + m_dweights[i] + "\n");
				if (m_dweights[i] > 0) {
					nnumberOfPositiveWeights++;
				} else if (m_dweights[i] < 0) {
					nnumberOfNegativeWeights++;
				}
			}
			text.append("\n# positive weights = " + nnumberOfPositiveWeights + "\n");
			text.append("# negative weights = " + nnumberOfNegativeWeights + "\n");
			text.append("# weights equal to zero = " +
			(m_dweights.length - (nnumberOfPositiveWeights + nnumberOfNegativeWeights)) + "\n");
		} catch (Exception e) {
			e.printStackTrace();
			return "Can't print BinaryRLSC classifier.";
		}
		return text.toString();
	}

	public static void main (String[] args) throws Exception {
		//test (args);
		Classifier scheme;
		try {
			scheme = new BinaryRLSC();
			System.out.println(Evaluation.evaluateModel(scheme, args));
		} catch (Exception e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
	}


}
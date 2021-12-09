/*
 *    AllPairsClassifier.java
 *    Copyright (C) 2001, Aldebaro Klautau
 */

//Example:
//java weka.classifiers.InformaDiscriminativeClassifier -o -t tr.arff -T te.arff -F false -N 1 -I weka.classifiers.GaussiansMixture -D weka.classifiers.AllPairsClassifier -- -I 1 -P false -M 0.0 -C 1.0E-4 -R false -- -A 2 -D 1 -W weka.classifiers.j48.J48
//Note that -N 1 passes only 1 candidate to discriminative. Then -A 2 imposes that at least 2 classes correspond to non-zero entries if a binary classifier
//is to be considered. No classifiers is considered active and they return the same score. Then InformaDiscriminativeClassifier eliminates all classes but 1,
//the one with highest probability according to informative. That's the one chosen.

package  weka.classifiers;

import  java.io.Serializable;
import  java.util.Enumeration;
import  java.util.Random;
import  java.util.Vector;
import  weka.core.Attribute;
import  weka.core.AttributeStats;
import  weka.core.Instance;
import  weka.core.Instances;
import  weka.core.Option;
import  weka.core.OptionHandler;
import  weka.core.SelectedTag;
import  weka.core.Tag;
import  weka.core.Utils;
import  weka.filters.Filter;
import  weka.filters.MakeIndicatorFilter;
import  weka.filters.InstanceFilter;
import  edu.ucsd.asr.IO;


//import  JMatLink;
/**
 *
 *    To be used exclusively as second stage of an InformaDiscriminative
 *    Classifier. Otherwise, use ScoreMultiClassClassifier.
 *
 * Class for handling multi-class datasets with 2-class distribution
 * classifiers.<p>
 *
 * Valid options are:<p>
 *
 * -E num <br>
 * Sets the error-correction mode. Valid values are 0 (no correction),
 * 1 (random codes), and 2 (exhaustive code). (default 0) <p>
 *
 * -W classname <br>
 * Specify the full class name of a classifier as the basis for
 * the multi-class classifier (required).<p>
 *
 * -F filename <br>
 * File with error coding matrix.<p>
 *
 */
public class AllPairsClassifier extends ScoreMultiClassClassifier
		implements OptionHandler {

	/** Used only with InformaDiscriminativeClassifier: a binary classifier
	 *  is not taken in account if the classes it was trained do not include
	 *  at least m_nminimumNumberInActiveList elements of the active list
	 *  set by method setActiveClassifiers(). For example, if
	 *  m_nminimumNumberInActiveList = 2 and the encoding matrix is all-pairs,
	 *  then only classifiers correspondent to pairs that belong to the active
	 *  will be considered when composing the final multi-class decision. */
	private int m_nminimumNumberInActiveList = 2;
	private double[][] m_dbinaryProbabilities;
	private int[] m_nclassesCount;
	private int[][] m_nclassifiersOfEachClass;

	//smooth probabilities
	private final double m_depsilon = 0;
	private double[] m_dinformativeProbabilities;
	//weights per binary classifier, calculated as
	//m_dweights = m_trinaryECOCMatrix.getPairwiseSumOfProbabilities(m_dinformativeProbabilities);
	private double[] m_dweights;

	private boolean[] m_oactiveClassifiers;
	private int[] m_nbestList;

	//do I need it?
	public AllPairsClassifier() {
		super();
	}

	public double classifyInstance (Instance instance) throws Exception {
		switch (m_decodingMethod) {
			case 1:
				return  classifyInstancev1 (instance);
			case 2:
				return  classifyInstancev2 (instance);
//			case 3:
//				return  distributionForInstancev3(instance);
//			case 4:
//				return  distributionForInstancev4(instance);
//			case 5:
//				return  distributionForInstancev5(instance);
//			case 6:
//				return  distributionForInstancev6(instance);
			default:
				throw  new Exception("Classification method should be between 1 and 2");
		}
	}

	/**
	 * Prints the classifiers.
	 */
	public String toString () {
		if (m_Classifiers == null) {
			return  "ScoreMultiClassClassifier: No model built yet.";
		}
		StringBuffer text = new StringBuffer();
		text.append("ScoreMultiClassClassifier\n\n");
		for (int i = 0; i < m_Classifiers.length; i++) {
			text.append("Classifier ").append(i + 1);
			if (m_Classifiers[i] != null) {
				if ((m_ClassFilters != null) && (m_ClassFilters[i] != null)) {
					text.append(", using indicator values: ");
					text.append(m_ClassFilters[i].getValueRange());
				}
				text.append('\n');
				text.append(m_Classifiers[i].toString() + "\n");
			}
			else {
				text.append(" Skipped (no training examples)\n");
			}
		}
		return  text.toString();
	}

	/**
	 * Returns an enumeration describing the available options
	 *
	 * @return an enumeration of all the available options
	 */
	public Enumeration listOptions () {
		Vector vec = new Vector(4);
		vec.addElement(new Option("\tMinimum number of classes when using with Informa....",
				"A", 1, "-A <number>"));
		Enumeration enume = super.listOptions();
		while (enume.hasMoreElements()) {
			vec.addElement(enume.nextElement());
		}
		return vec.elements();
	}

//		vec.addElement(new Option("\tMethod for combining binary decisions (1 to 5)",
//				"D", 1, "-D <number>"));
//		vec.addElement(new Option("\tSets the error-correction mode. Valid values are 0 (one versus all),\n"
//				+ "1 (all pairs) and 2 (from file). (default 0)\n", "E", 1, "-E <num>"));
//		vec.addElement(new Option("\tSets the base classifier.", "W", 1, "-W <base classifier>"));
//		vec.addElement(new Option("\tSets name of file with error coding matrix.",
//				"W", 1, "-F <file name>"));
//		if (m_Classifier != null) {
//			try {
//				vec.addElement(new Option("", "", 0, "\nOptions specific to classifier "
//						+ m_Classifier.getClass().getName() + ":"));
//				Enumeration enume = ((OptionHandler)m_Classifier).listOptions();
//				while (enume.hasMoreElements()) {
//					vec.addElement(enume.nextElement());
//				}
//			} catch (Exception e) {}
//		}
//		return  vec.elements();
//	}

	/**
	 * Parses a given list of options. Valid options are:<p>
	 *
	 * -E num <br>
	 * Sets the error-correction mode. Valid values are 0 (no correction),
	 * 1 (random codes), and 2 (exhaustive code). (default 0) <p>
	 *
	 * -F fileName <br>
	 * Sets the file with error coding matrix.<p>
	 *
	 * -W classname <br>
	 * Specify the full class name of a learner as the basis for
	 * the multiclassclassifier (required).<p>
	 *
	 * @param options the list of options as an array of strings
	 * @exception Exception if an option is not supported
	 */
	public void setOptions (String[] options) throws Exception {
		String minN = Utils.getOption('A', options);
		if (minN.length() != 0) {
			setMinimumNumberInActiveList(Integer.parseInt(minN));
		}
		super.setOptions(options);
//		String temp = Utils.getOption('D', options);
//		if (temp.length() != 0) {
//			setClassificationMethod(Integer.parseInt(temp));
//		}
//		String errorString = Utils.getOption('E', options);
//		if (errorString.length() != 0) {
//			setErrorCorrectionMode(new SelectedTag(Integer.parseInt(errorString), TAGS_ERROR));
//		}
//		else {
//			setErrorCorrectionMode(new SelectedTag(ERROR_ONEVSALL, TAGS_ERROR));
//		}
//		String fileName = Utils.getOption('F', options);
//		if (m_ErrorMode == ERROR_FROMFILE) {
//			if (fileName.length() == 0) {
//				throw  new Exception("A file name must be specified with" + " the -F option.");
//			}
//			setErrorCodingFileName(fileName);
//		} else if (fileName.length() != 0 && !fileName.equals("not_used")) {
//			throw  new Exception("-F option requires error mode -E equal to 2, " + "but it was set to "
//					+ m_ErrorMode);
//		}
//		String classifierName = Utils.getOption('W', options);
//		if (classifierName.length() == 0) {
//			throw  new Exception("A classifier must be specified with" + " the -W option.");
//		}
//		setDistributionClassifier((DistributionClassifier)Classifier.forName(classifierName,
//				Utils.partitionOptions(options)));
	}

	/**
	 * Gets the current settings of the Classifier.
	 *
	 * @return an array of strings suitable for passing to setOptions
	 */
	public String[] getOptions () {
		String[] classifierOptions = super.getOptions();
//		String[] classifierOptions = new String[0];
//		if ((m_Classifier != null) && (m_Classifier instanceof OptionHandler)) {
//			classifierOptions = ((OptionHandler)m_Classifier).getOptions();
//		}
		String[] options = new String[classifierOptions.length + 20];
		int current = 0;
		options[current++] = "-A";
		options[current++] = "" + getMinimumNumberInActiveList();
//		options[current++] = "-E";
//		options[current++] = "" + m_ErrorMode;
//		options[current++] = "-F";
//		options[current++] = "" + m_errorCodingFileName;
//		options[current++] = "-D";
//		options[current++] = "" + getClassificationMethod();
//		if (getDistributionClassifier() != null) {
//			options[current++] = "-W";
//			options[current++] = getDistributionClassifier().getClass().getName();
//		}
//		options[current++] = "--";
		System.arraycopy(classifierOptions, 0, options, current, classifierOptions.length);
		current += classifierOptions.length;
		while (current < options.length) {
			options[current++] = "";
		}
		return  options;
	}

	/**
	 * @return a description of the classifier suitable for
	 * displaying in the explorer/experimenter gui
	 */
	public String globalInfo () {
		return  "A metaclassifier for handling multi-class datasets with 2-class "
				+ "distribution classifiers. This classifier is also capable of " + "applying error correcting output codes for increased accuracy.";
	}

	/**
	 * @return tip text for this property suitable for
	 * displaying in the explorer/experimenter gui
	 */
	public String errorCorrectionModeTipText () {
		return  "Sets whether error correction will be used. The default method " +
				"is no error correction: one classifier will be built per class value. "
				+ "Increased accuracy can be obtained by using error correcting output "
				+ "codes.";
	}

	/**
	 * @return tip text for this property suitable for
	 * displaying in the explorer/experimenter gui
	 */
	public String distributionClassifierTipText () {
		return  "Sets the DistributionClassifier used as the basis for " + "the multi-class classifier.";
	}

	public void setMinimumNumberInActiveList (int n) {
		m_nminimumNumberInActiveList = n;
	}

	public int getMinimumNumberInActiveList () {
		return  m_nminimumNumberInActiveList;
	}

	//includes all pairs where at least m_nminimumNumberInActiveList elements are in nbestList
	public void setActiveClassifiers (int[] nbestList) {
		if (m_oactiveClassifiers == null) {
			m_oactiveClassifiers = new boolean[m_trinaryECOCMatrix.getNumberOfBinaryClassifiers()];
		}
		m_nbestList = nbestList;
		m_trinaryECOCMatrix.setActiveClassifiers(m_nbestList,
		m_oactiveClassifiers, m_nminimumNumberInActiveList);
	}

	private double findMinimumAmongActive(double[] doutputDistances) {
		//find minimum distance among classes that got good score with informative classifier
		double dbest = Double.MAX_VALUE;
		double dwinner = -1;
		for (int i = 0; i < m_nbestList.length; i++) {
			if (doutputDistances[m_nbestList[i]] < dbest) {
				dbest = doutputDistances[m_nbestList[i]];
				dwinner = m_nbestList[i];
			}
		}
		return dwinner;
	}


		//use sum per line but only for active classifiers (set by
		//InformativeDiscriminativeClassifier)
		//here use L(z) = max{1-z, 0} (SVM loss-based) on raw scores
	private double classifyInstancev1 (Instance instance) throws Exception {
//		if (m_Classifiers.length == 1) {
//			return  ((DistributionClassifier)m_Classifiers[0]).distributionForInstance(instance);
//		}
		double[] drawScores = getRawScoresFromBinaryClassifiers(instance);
		double[] doutputDistances = new double[instance.numClasses()];
		m_trinaryECOCMatrix.getSVMDistancePerClassForActiveClassifiers(drawScores,doutputDistances,m_oactiveClassifiers);
		return findMinimumAmongActive(doutputDistances);
	}

//		//double[] probs = simpleConversionOfDistancesIntoProbabilities(doutputDistances);
//		double[] outprobs = new double[probs.length];
//		//eliminate classes that did not get good score with informative classifier
//		for (int i = 0; i < m_nbestList.length; i++) {
//			outprobs[m_nbestList[i]] = probs[m_nbestList[i]];
//		}
//		if (Utils.gr(Utils.sum(outprobs), 0)) {
//			Utils.normalize(outprobs);
//			return outprobs;
//		}
//		else {
//			return  m_ZeroR.distributionForInstance(instance);
//		}
//
		//double[] ddistances = new double[drawScores.length];
		//limit score to be at most 1
//		for (int i = 0; i < drawScores.length; i++) {
//			if (drawScores[i] > 1.0) {
//				drawScores[i] = 1.0;
//			}
//		}
//		//the bigger the scores, the better
//		//double[] dtotalRawScorePerClass = m_trinaryECOCMatrix.getSumPerClassGivenRawScores(drawScores);
//		//double[] dbinaryProbabilities = simpleConversionOfDistancesIntoProbabilities(ddistances);
//
//		double[] dtotalRawScorePerClass = new double[instance.numClasses()];
//		//System.out.println("HERE");
//		IO.DisplayVector(m_oactiveClassifiers);
//		//IO.DisplayVector(drawScores);
//		//IO.DisplayVector(dbinaryProbabilities);
//		//IO.DisplayVector(ddistances);
//		m_trinaryECOCMatrix.getSumPerClassGivenRawScores(drawScores,dtotalRawScorePerClass,m_oactiveClassifiers);
//		double[] probs = simpleConversionOfScoresIntoProbabilities(dtotalRawScorePerClass);
//		//note that probs was not normalized yet
//		double[] outprobs = new double[probs.length];
//		//eliminate classes that did not get good score with informative classifier
//		for (int i = 0; i < m_nbestList.length; i++) {
//			outprobs[m_nbestList[i]] = probs[m_nbestList[i]];
//		}
//		//IO.DisplayVector(outprobs);
//		if (Utils.gr(Utils.sum(outprobs), 0)) {
//			Utils.normalize(outprobs);
//			return outprobs;
//		}
//		else {
//			return  m_ZeroR.distributionForInstance(instance);
//		}
//	}

		//use sum per line but only for active classifiers (set by
		//InformativeDiscriminativeClassifier)
		//here use Hamming distance
	private double classifyInstancev2 (Instance instance) throws Exception {
//		if (m_Classifiers.length == 1) {
//			return  ((DistributionClassifier)m_Classifiers[0]).distributionForInstance(instance);
//		}

		double[] dhammingDistances = m_trinaryECOCMatrix.getHammingDistancesForActiveClassifiers(getBinaryHardDecisions(instance),
		m_oactiveClassifiers);
		return findMinimumAmongActive(dhammingDistances);
	}
//		double[] probs = simpleConversionOfDistancesIntoProbabilities(dhammingDistances);
//		double[] outprobs = new double[probs.length];
//		//eliminate classes that did not get good score with informative classifier
//		for (int i = 0; i < m_nbestList.length; i++) {
//			outprobs[m_nbestList[i]] = probs[m_nbestList[i]];
//		}
//		if (Utils.gr(Utils.sum(outprobs), 0)) {
//			Utils.normalize(outprobs);
//			return outprobs;
//		}
//		else {
//			return  m_ZeroR.distributionForInstance(instance);
//		}
//
//		double[] probs = new double[instance.numClasses()];
//		double[] dbinaryProbabilities = getBinaryProbabilities(instance);
//		//IO.DisplayVector(m_oactiveClassifiers);
//		//IO.DisplayVector(dbinaryProbabilities);
//		m_trinaryECOCMatrix.getSumPerClass(dbinaryProbabilities,probs,m_oactiveClassifiers);
//		//note that probs was not normalized yet
//		double[] outprobs = new double[probs.length];
//		//eliminate classes that did not get good score with informative classifier
//		for (int i = 0; i < m_nbestList.length; i++) {
//			outprobs[m_nbestList[i]] = probs[m_nbestList[i]];
//		}
//		//IO.DisplayVector(outprobs);
//		if (Utils.gr(Utils.sum(outprobs), 0)) {
//			Utils.normalize(outprobs);
//			//IO.DisplayVector(outprobs);
//			return outprobs;
//		}
//		else {
//			return  m_ZeroR.distributionForInstance(instance);
//		}
//	}

	//public double[] distributionForInstanceWithOnlyActiveClassifiers (Instance instance) throws Exception {
		//use sum per line but only for active classifiers (set by
		//InformativeDiscriminativeClassifier)
		//here use raw scores
	private double[] distributionForInstancev3NOTWORKING (Instance instance) throws Exception {
//		if (m_Classifiers.length == 1) {
//			return  ((DistributionClassifier)m_Classifiers[0]).distributionForInstance(instance);
//		}
		double[] drawScores = getRawScoresFromBinaryClassifiers(instance);
		//double[] ddistances = new double[drawScores.length];
		//the bigger the scores, the better
		//double[] dtotalRawScorePerClass = m_trinaryECOCMatrix.getSumPerClassGivenRawScores(drawScores);
		//double[] dbinaryProbabilities = simpleConversionOfDistancesIntoProbabilities(ddistances);

		double[] dtotalRawScorePerClass = new double[instance.numClasses()];
		System.out.println("HERE");
		IO.DisplayVector(m_oactiveClassifiers);
		IO.DisplayVector(drawScores);
		//ak
		//m_trinaryECOCMatrix.getSumPerClassGivenRawScores(drawScores,dtotalRawScorePerClass,m_oactiveClassifiers);
		IO.DisplayVector(dtotalRawScorePerClass);
		double[] probs = null; //simpleConversionOfScoresIntoProbabilities(dtotalRawScorePerClass);
		IO.DisplayVector(probs);
		//note that probs was not normalized yet
		double[] outprobs = new double[probs.length];
		//eliminate classes that did not get good score with informative classifier
		for (int i = 0; i < m_nbestList.length; i++) {
			outprobs[m_nbestList[i]] = probs[m_nbestList[i]];
		}
		//IO.DisplayVector(outprobs);
		if (Utils.gr(Utils.sum(outprobs), 0)) {
			Utils.normalize(outprobs);
			return outprobs;
		}
		else {
			return  null; //m_ZeroR.distributionForInstance(instance);
		}
	}

	/**
	 * Simply use sum per lines but weighted by informative probs.
	 * The -N option of InformaDiscriminativeClassifier doesn't not
	 * have influence with -D = 2 for AllPairsClassifier.
	 * Also, option -A doesn't affect this method.
	 */
	 //should look more closely to ways of calculating the weights
	 //maybe something based on the binary classifier's accuracy
	 //and informative classifier accuracy for each class.
	 //use raw scores here
	private double[] distributionForInstancev4NOTWORKING (Instance instance) throws Exception {
//		if (m_Classifiers.length == 1) {
//			return  ((DistributionClassifier)m_Classifiers[0]).distributionForInstance(instance);
//		}
		double[] drawScores = getRawScoresFromBinaryClassifiers(instance);
		double[] dweightedScores = m_trinaryECOCMatrix.getWeightedSumPerClassGivenRawScores(drawScores,
		m_dweights);
		//the larger the dweightedScores, the better
		double[] dprobs = null; //simpleConversionOfScoresIntoProbabilities(dweightedScores);
//		System.out.println("HERE");
//		IO.DisplayVector(drawScores);
//		IO.DisplayVector(m_dweights);
//		IO.DisplayVector(dweightedScores);
//		IO.DisplayVector(dprobs);
		return dprobs;
	}

	/**
	 * Simply use sum per lines but weighted by informative probs.
	 * The -N option of InformaDiscriminativeClassifier doesn't not
	 * have influence with -D = 2 for AllPairsClassifier.
	 * Also, option -A doesn't affect this method.
	 */
	 //should look more closely to ways of calculating the weights
	 //maybe something based on the binary classifier's accuracy
	 //and informative classifier accuracy for each class.
	private double[] distributionForInstancev5NOTWORKING (Instance instance) throws Exception {
//		if (m_Classifiers.length == 1) {
//			return  ((DistributionClassifier)m_Classifiers[0]).distributionForInstance(instance);
//		}
		double[] binaryProbs =  null; //getBinaryProbabilities(instance);
//		double[] probs = m_trinaryECOCMatrix.getWeightedSumPerClassGivenProbabilities(binaryProbs,
//		m_dweights);
		double[] probs = m_trinaryECOCMatrix.getWeightedHammingDistances(binaryProbs,
		m_dweights);
		Utils.normalize(probs);
		return  probs;
	}


	private double[] distributionForInstancev6NOTWORKING (Instance instance) throws Exception {
//		if (m_Classifiers.length == 1) {
//			return  ((DistributionClassifier)m_Classifiers[0]).distributionForInstance(instance);
//		}
		double[] drawScores = getRawScoresFromBinaryClassifiers(instance);
		for (int i = 0; i < drawScores.length; i++) {
			if (drawScores[i] > 1.0) {
				drawScores[i] = 1.0;
			}
		}

		double[] dweightedScores = m_trinaryECOCMatrix.getWeightedSumPerClassGivenRawScores(drawScores,
		m_dweights);
		//the larger the dweightedScores, the better
		double[] dprobs = null; //simpleConversionOfScoresIntoProbabilities(dweightedScores);
//		System.out.println("HERE");
//		IO.DisplayVector(drawScores);
//		IO.DisplayVector(m_dweights);
//		IO.DisplayVector(dweightedScores);
//		IO.DisplayVector(dprobs);
		return dprobs;
	}

	private double[] getMaximumOfEachPair (double[] dclassProbabilities) {
		return m_trinaryECOCMatrix.getMaximumOfEachPair (dclassProbabilities);
	}

	/**
	 * use equation suggested by Alon
	 */
//	public double[] distributionForInstancev5 (Instance instance) throws Exception {
//		if (m_Classifiers.length == 1) {
//			return  ((DistributionClassifier)m_Classifiers[0]).distributionForInstance(instance);
//		}
//		//keep notation as in Bianca's paper
//		//r has the dimension equal to the number of binary classifiers
//		//initialization
//		double[] r = getBinaryProbabilites(instance);
//		//smooth
//		double depsilon = 1e-1;
//		for (int i = 0; i < r.length; i++) {
//			if (r[i] < depsilon) {
//				r[i] = depsilon;
//			}
//			if (r[i] > (1-depsilon)) {
//				r[i] = (1-depsilon);
//			}
//		}
//
//		//double[] r = {0.56, 0.51, 0.60, 0.96, 0.44, 0.59};
//		//double[] r = {0.51,0.53,0.51,0.54,0.55,0.59};
//		//m_ncodingMatrix = new AllPairsCode(4).m_Codebits;
//		//detectBinaryClassifiersOfEachClass();
//		int nnumberOfClasses = m_ncodingMatrix[0].length;
//		double[] dposterioriPerState = new double[nnumberOfClasses];
//		double[] dsumOfInverses = new double[nnumberOfClasses];
//		for (int j = 0; j < dposterioriPerState.length; j++) {
//			for (int i = 0; i < m_dweights.length; i++) {
//				if (m_ncodingMatrix[i][j] == 1) {
//					dsumOfInverses[j] += 1.0 / r[i];
//				}
//				else if (m_ncodingMatrix[i][j] == -1) {
//					dsumOfInverses[j] += 1.0 / (1 - r[i]);
//				}
//				if (Double.isInfinite(dsumOfInverses[j])) {
//					break;
//				}
//	//				else {
//	//					dprob[j] += 0.5;
//	//				}
//	//				dposterioriPerState[i] += m_dweights[j] * r[j];
//			}
//		}
//
//		for (int j = 0; j < dposterioriPerState.length; j++) {
//			if (Double.isInfinite(dsumOfInverses[j])) {
//				dposterioriPerState[j] = 0;
//			} else {
//				dposterioriPerState[j] = 1.0 / (dsumOfInverses[j] - (nnumberOfClasses-2));
//			}
//		}
//
//		Utils.normalize(dposterioriPerState);
//		//IO.DisplayVector(dsumOfInverses);
//		//IO.DisplayVector(dposterioriPerState);
//		return  dposterioriPerState;
//	}

	public void setProbabilitiesGivenByInformative (double[] dprobs) {
//		for (int i = 0; i < dprobs.length; i++) {
//			dprobs[i] += 2000;
//		}

//		int n = dprobs.length;
//		int[] nascendingOrder = Utils.sort(dprobs);
//		//avoid numerical problems
//		if (dprobs[nascendingOrder[n-1]] + dprobs[nascendingOrder[n-2]] > 0.8) {
//		//      System.out.println("BEFORE");
//		//      IO.DisplayVector(dprobs);
//			if (dprobs[nascendingOrder[n-1]] > 0.7) {
//	dprobs[nascendingOrder[n-1]] -= 0.3;
//	dprobs[nascendingOrder[n-2]] -= 0.05;
//	if (dprobs[nascendingOrder[n-2]] < dprobs[nascendingOrder[n-3]]) {
//		dprobs[nascendingOrder[n-2]] = dprobs[nascendingOrder[n-3]];
//	}
//			} else {
//	dprobs[nascendingOrder[n-1]] -= 0.1;
//	dprobs[nascendingOrder[n-2]] -= 0.1;
//			}
//			Utils.normalize(dprobs);
//			//      System.out.println("AFTER");
//			//      IO.DisplayVector(dprobs);
//		}
			Utils.normalize(dprobs);
		m_dinformativeProbabilities = dprobs;
		m_dweights = m_trinaryECOCMatrix.getPairwiseSumOfProbabilities(m_dinformativeProbabilities);
		//m_dweights = m_decodingECOCMatrix.getMaximumOfEachPair(m_dinformativeProbabilities);
	}

	private double calculateSVMLoss(double z) {
			double t = 1-z;
			return (t<0) ? 0 : t;
	}



	/**
	 * Main method for testing this class.
	 *
	 * @param argv the options
	 */
	public static void main (String[] argv) {
		Classifier scheme;
		try {
			scheme = new AllPairsClassifier();
			System.out.println(Evaluation.evaluateModel(scheme, argv));
		} catch (Exception e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
	}
}




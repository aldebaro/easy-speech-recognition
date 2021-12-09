/*
 *    TwoStagesMultiClassClassifier.java
 *    Copyright (C) 2001, Aldebaro Klautau
 *
 */

 package weka.classifiers;

import java.io.*;
import java.util.*;
import weka.core.*;
import edu.ucsd.asr.*;

/**
 * Implements stacking of binary classifiers
 * in ECOC scheme. For more information, see<p>
 *
 * David H. Wolpert (1992). <i>Stacked
 * generalization</i>. Neural Networks, 5:241-259, Pergamon Press. <p>
 *
 * Valid options are:<p>
 *
 * -X num_folds <br>
 * The number of folds for the cross-validation (default 10).<p>
 *
 * -S seed <br>
 * Random number seed (default 1).<p>
 *
 * -B classifierstring <br>
 * Classifierstring should contain the full class name of a base scheme
 * followed by options to the classifier.
 * (required, option should be used once for each classifier).<p>
 *
 * -M classifierstring <br>
 * Classifierstring for the meta classifier. Same format as for base
 * classifiers. (required) <p>
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 2 $
 */
public class TwoStagesMultiClassClassifier extends DistributionClassifier implements OptionHandler {

	/** The meta classifier. */
	protected Classifier m_MetaClassifier = new weka.classifiers.DecisionStump();

	/** The base classifier. */
	protected ScoreMultiClassClassifier m_BaseClassifier = new weka.classifiers.ScoreMultiClassClassifier();

	/** Format for meta data */
	protected Instances m_MetaFormat = null;

	/** Format for base data */
	protected Instances m_BaseFormat = null;

	/** Set the number of folds for the cross-validation */
	protected int m_NumFolds = 10;

	/** Random number seed */
	protected int m_Seed = 1;

	private boolean m_otrainMetaClassifier = true;

	private boolean m_ouseCrossValidation = true;

	private String m_outputTrainMetaFile;
	private String m_outputTestMetaFile;
	private String m_inputTestMetaFile;

	/** Debugging mode, gives extra output if true */
	protected boolean m_Debug;

	/**
	 * Returns an enumeration describing the available options
	 *
	 * @return an enumeration of all the available options
	 */
	public Enumeration listOptions() {

		Vector newVector = new Vector(4);
		newVector.addElement(new Option(
				"\tFull class name of base classifiers to include, followed "
				+ "by scheme options\n"
				+ "\t(may be specified multiple times).\n"
				+ "\teg: \"weka.classifiers.NaiveBayes -K\"",
				"B", 1, "-B <scheme specification>"));
		newVector.addElement(new Option(
				"\tFull name of meta classifier, followed by options.",
				"M", 0, "-M <scheme specification>"));
		newVector.addElement(new Option(
				"\tSets the number of cross-validation folds.",
				"X", 1, "-X <number of folds>"));
		newVector.addElement(new Option(
				"\tSets the random number seed.",
				"S", 1, "-S <random number seed>"));
		newVector.addElement(new Option(
				"\tDo NOT use cross-validation.",
				"N", 0, "-N"));
		newVector.addElement(new Option(
				"\tWrite training data for meta classifier to given file.",
				"C", 1, "-C <file name>"));
		newVector.addElement(new Option(
				"\tWrite output test data for meta classifier corresponding to file specified with option -E.",
				"D", 1, "-D <file name>"));
		newVector.addElement(new Option(
				"\tInput test data to be converted to file specified by option -D (it's usually the same file provided with option -T to the Evaluator).",
				"E", 1, "-E <file name>"));
		newVector.addElement(new Option(
				"\tDo NOT train meta classifier (maybe user wants only to write meta data to files).",
				"F", 0, "-F"));
		newVector.addElement(new Option(
					"\tTurn on debugging output.",
					"A", 0, "-A"));
		return newVector.elements();
	}

	/**
	 * Parses a given list of options. Valid options are:<p>
	 *
	 * -X num_folds <br>
	 * The number of folds for the cross-validation (default 10).<p>
	 *
	 * -S seed <br>
	 * Random number seed (default 1).<p>
	 *
	 * -B classifierstring <br>
	 * Classifierstring should contain the full class name of a base scheme
	 * followed by options to the classifier.
	 * (required, option should be used once for each classifier).<p>
	 *
	 * -M classifierstring <br>
	 * Classifierstring for the meta classifier. Same format as for base
	 * classifiers. (required) <p>
	 *
	 * @param options the list of options as an array of strings
	 * @exception Exception if an option is not supported
	 */
	public void setOptions(String[] options) throws Exception {
		setUseCrossValidation(!Utils.getFlag('N', options));
		setTrainMetaClassifier(!Utils.getFlag('F', options));
		setDebug(Utils.getFlag('A', options));

		String numFoldsString = Utils.getOption('X', options);
		if (numFoldsString.length() != 0) {
			if (!m_ouseCrossValidation) {
				throw new Exception("Cannot use -N with -X");
			}
			setNumFolds(Integer.parseInt(numFoldsString));
		} else {
			setNumFolds(10);
		}
		String randomString = Utils.getOption('S', options);
		if (randomString.length() != 0) {
			setSeed(Integer.parseInt(randomString));
		} else {
			setSeed(1);
		}

		String temp = Utils.getOption('C', options);
		if (temp.length() != 0) {
			setOutputTrainMetaFile(temp);
		} else {
			setOutputTrainMetaFile(null);
		}

		temp = Utils.getOption('D', options);
		String temp2 = Utils.getOption('E', options);
		if (temp.length() != 0 && temp2.length() != 0) {
			setOutputTestMetaFile(temp);
			setInputTestMetaFile(temp2);
		} else if (temp.length() == 0 && temp2.length() == 0) {
			setOutputTestMetaFile(null);
			setInputTestMetaFile(null);
		}	else {
			throw new Exception ("Options -D and -E should be set (or not) together!");
		}

		// Iterate through the schemes
		FastVector classifiers = new FastVector();
		while (true) {
			String classifierString = Utils.getOption('B', options);
			if (classifierString.length() == 0) {
	break;
			}
			String [] classifierSpec = Utils.splitOptions(classifierString);
			if (classifierSpec.length == 0) {
	throw new Exception("Invalid classifier specification string");
			}
			String classifierName = classifierSpec[0];
			classifierSpec[0] = "";
			classifiers.addElement(Classifier.forName(classifierName,
						classifierSpec));
		}
		if (classifiers.size() == 0) {
			throw new Exception("At least one base classifier must be specified"
				+ " with the -B option.");
		} else if (classifiers.size() > 1) {
			throw new Exception("Only one base classifier must be specified"
				+ " with the -B option.");
		} else {
			Classifier [] classifiersArray = new Classifier [classifiers.size()];
			for (int i = 0; i < classifiersArray.length; i++) {
	classifiersArray[i] = (Classifier) classifiers.elementAt(i);
			}
			if ( ! (classifiersArray[0] instanceof ScoreMultiClassClassifier) ) {
				throw new Exception("Base classifier must be ScoreMultiClassClassifier");
			}
			setBaseClassifier((ScoreMultiClassClassifier) classifiersArray[0]);
		}

		String classifierString = Utils.getOption('M', options);
		String [] classifierSpec = Utils.splitOptions(classifierString);
		if (classifierSpec.length == 0) {
			throw new Exception("Meta classifier has to be provided.");
		}
		String classifierName = classifierSpec[0];
		classifierSpec[0] = "";
		setMetaClassifier(Classifier.forName(classifierName, classifierSpec));
	}

	/**
	 * Gets the current settings of the Classifier.
	 *
	 * @return an array of strings suitable for passing to setOptions
	 */
	public String [] getOptions() {

		String [] options = new String[40];
		int current = 0;

		if (getDebug()) {
			options[current++] = "-A";
		}

		if (m_ouseCrossValidation) {
			options[current++] = "-X"; options[current++] = "" + getNumFolds();
		} else {
			options[current++] = "-N";
		}

		if (!m_otrainMetaClassifier) {
			options[current++] = "-F";
		}

		if (getOutputTrainMetaFile() != null) {
			options[current++] = "-C";
			options[current++] = getOutputTrainMetaFile();
		}

		if (getOutputTestMetaFile() != null) {
			options[current++] = "-D";
			options[current++] = getOutputTestMetaFile();
		}

		if (getInputTestMetaFile() != null) {
			options[current++] = "-E";
			options[current++] = getInputTestMetaFile();
		}

		options[current++] = "-S"; options[current++] = "" + getSeed();

		options[current++] = "-B";
		options[current++] = "" + getBaseClassifierSpec(0);

		if (getMetaClassifier() != null) {
			options[current++] = "-M";
			options[current++] = getClassifierSpec(getMetaClassifier());
		}

		while (current < options.length) {
			options[current++] = "";
		}
		return options;
	}

	/**
	 * Sets the seed for random number generation.
	 *
	 * @param seed the random number seed
	 */
	public void setSeed(int seed) {

		m_Seed = seed;;
	}

	/**
	 * Gets the random number seed.
	 *
	 * @return the random number seed
	 */
	public int getSeed() {

		return m_Seed;
	}

	/**
	 * Gets the number of folds for the cross-validation.
	 *
	 * @return the number of folds for the cross-validation
	 */
	public int getNumFolds() {

		return m_NumFolds;
	}

	/**
	 * Sets the number of folds for the cross-validation.
	 *
	 * @param numFolds the number of folds for the cross-validation
	 * @exception Exception if parameter illegal
	 */
	public void setNumFolds(int numFolds) throws Exception {

		if (numFolds < 0) {
			throw new Exception("Stacking: Number of cross-validation " +
				"folds must be positive.");
		}
		m_NumFolds = numFolds;
	}

	/**
	 * Sets the list of possible classifers to choose from.
	 *
	 * @param classifiers an array of classifiers with all options set.
	 */
	public void setBaseClassifier(ScoreMultiClassClassifier classifier) {

		m_BaseClassifier = classifier;
	}

	/**
	 * Gets the list of possible classifers to choose from.
	 *
	 * @return the array of Classifiers
	 */
	public Classifier getBaseClassifier() {

		return m_BaseClassifier;
	}

	/**
	 * Gets the specific classifier from the set of base classifiers.
	 *
	 * @param index the index of the classifier to retrieve
	 * @return the classifier
	 */
	public Classifier getBaseClassifier(int index) {

		return m_BaseClassifier;
	}


	/**
	 * Adds meta classifier
	 *
	 * @param classifier the classifier with all options set.
	 */
	public void setMetaClassifier(Classifier classifier) {
		//try {
				m_MetaClassifier = classifier;
//		} catch (Exception e) {
//			End.throwError("Must be a DistributionClassifier");
//			System.exit(1);
//		}
	}

	/**
	 * Gets the meta classifier.
	 *
	 * @return the meta classifier
	 */
	public Classifier getMetaClassifier() {

		return m_MetaClassifier;
	}

	private void checkConditions(Instances data) throws Exception {
		if (m_BaseClassifier == null) {
			throw new Exception("No base classifier has been set");
		}
		if (m_MetaClassifier == null) {
			throw new Exception("No meta classifier has been set");
		}
		if (!(data.classAttribute().isNominal() ||
		data.classAttribute().isNumeric())) {
			throw new Exception("Class attribute has to be nominal or numeric!");
		}
		if (data.classIndex() != data.numAttributes()-1) {
			throw new Exception("Class must be last attribute!");
		}
		m_BaseFormat = new Instances(data, 0);
	}

//		Instances newData = new Instances(data);
//		newData.deleteWithMissingClass();
//		if (newData.numInstances() == 0) {
//			throw new Exception("No training instances without missing class!");
//		}
//		return newData;
//	}


	/**
	 *
	 * @param data the training data to be used for generating the classifier.
	 *
	 * @exception Exception if the classifier could not be built successfully
	 */
	public void buildClassifier(Instances data) throws Exception {
		checkConditions(data);

		// First, build the base classifiers on the full training data
		m_BaseClassifier.buildClassifier(data);

		//data used for training meta classifier
		Instances trainMetaData = null;
		if (m_ouseCrossValidation) {
			trainMetaData = convertToMetaDataUsingCrossValidation(data);
		} else {
			trainMetaData = convertToMetaDataUsingFinalBaseClassifier(data);
		}
		//keep a copy of the structure
		m_MetaFormat = new Instances(trainMetaData, 0);

		//write file(s) if requested by user
		if (m_outputTrainMetaFile != null) {
			trainMetaData.writeToARFF(m_outputTrainMetaFile);
		}
		if (m_outputTestMetaFile != null) {
			//read input test instance and convert it
			//assume class is last attribute
			Instances inputTest = new Instances(m_inputTestMetaFile);
			inputTest.setLastAttributeAsClass();
			m_BaseClassifier.writeScoresOfBinaryClassifiersToFile(inputTest, m_outputTestMetaFile);
		}

		//build meta classifier only if requested
		if (m_otrainMetaClassifier) {
			m_MetaClassifier.buildClassifier(trainMetaData);
		}

	}

	public void setUseCrossValidation(boolean useCrossValidation) {
		m_ouseCrossValidation = useCrossValidation;
	}

	public boolean getUseCrossValidation() {
		return m_ouseCrossValidation;
	}

	public boolean getTrainMetaClassifier() {
		return m_otrainMetaClassifier;
	}

	public void setTrainMetaClassifier(boolean trainMetaClassifier) {
		m_otrainMetaClassifier = trainMetaClassifier;
	}

	/**
	 * Classifies a given instance using the stacked classifier.
	 *
	 * @param instance the instance to be classified
	 * @exception Exception if instance could not be classified
	 * successfully
	 */
//	public double classifyInstance(Instance instance) throws Exception {
//
//		return m_MetaClassifier.classifyInstance(metaInstance(instance));
//	}
	public double[] distributionForInstance (Instance instance) throws Exception {
		//below doesn't work for SVM, so use raw scores
		//double[] firstStageDecision = ((ScoreMultiClassClassifier)m_BaseClassifier).getBinaryProbabilities(instance);
		double[] firstStageDecision = m_BaseClassifier.getRawScoresFromBinaryClassifiers(instance);

		double[] dprobPlusClass = new double[firstStageDecision.length + 1];
		System.arraycopy(firstStageDecision,0,dprobPlusClass,0,firstStageDecision.length);
		//leave the class (last entry) non-specified
		Instance newInstance = new Instance(1, dprobPlusClass);
		newInstance.setDataset(m_MetaFormat);
		double dclass = m_MetaClassifier.classifyInstance(newInstance);
		double[] dout = new double[newInstance.numClasses()];
		dout[(int) dclass] = 1;
		return dout;
	}

	/**
	 * Output a representation of this classifier
	 */
	public String toString() {

		if (m_BaseClassifier == null) {
			return "Stacking: No base scheme selected.";
		}
		if (m_MetaClassifier == null) {
			return "Stacking: No meta scheme selected.";
		}
		if (m_MetaFormat == null) {
			return "Stacking: No model built yet.";
		}
		String result = "Stacking\n\nBase classifiers\n\n";
		//for (int i = 0; i < m_BaseClassifiers.length; i++) {
			result += getBaseClassifier(0).toString() +"\n\n";
		//}

		result += "\n\nMeta classifier\n\n";
		result += m_MetaClassifier.toString();

		return result;
	}

	/**
	 * Makes the format for the level-1 data.
	 *
	 * @param instances the level-0 format
	 * @return the format for the meta data
	 */
//	protected Instances metaFormat(Instances instances) throws Exception {
//
//		FastVector attributes = new FastVector();
//		Instances metaFormat;
//		Attribute attribute;
//		int i = 0;
//
//		for (int k = 0; k < m_BaseClassifiers.length; k++) {
//			Classifier classifier = (Classifier) getBaseClassifier(k);
//			String name = classifier.getClass().getName();
//			if (m_BaseFormat.classAttribute().isNumeric()) {
//	attributes.addElement(new Attribute(name));
//			} else {
//	if (classifier instanceof DistributionClassifier) {
//		for (int j = 0; j < m_BaseFormat.classAttribute().numValues(); j++) {
//			attributes.addElement(new Attribute(name + ":" +
//						m_BaseFormat
//						.classAttribute().value(j)));
//		}
//	} else {
//		FastVector values = new FastVector();
//		for (int j = 0; j < m_BaseFormat.classAttribute().numValues(); j++) {
//			values.addElement(m_BaseFormat.classAttribute().value(j));
//		}
//		attributes.addElement(new Attribute(name, values));
//	}
//			}
//		}
//		attributes.addElement(m_BaseFormat.classAttribute());
//		metaFormat = new Instances("Meta format", attributes, 0);
//		metaFormat.setClassIndex(metaFormat.numAttributes() - 1);
//		return metaFormat;
//	}

	/**
	 * Gets the classifier specification string, which contains the class name of
	 * the classifier and any options to the classifier
	 *
	 * @param index the index of the classifier string to retrieve, starting from
	 * 0.
	 * @return the classifier string, or the empty string if no classifier
	 * has been assigned (or the index given is out of range).
	 */
	protected String getBaseClassifierSpec(int index) {

//		if (m_BaseClassifiers.length < index) {
//			return "";
//		}
		return getClassifierSpec(getBaseClassifier(index));
	}
	/**
	 * Gets the classifier specification string, which contains the class name of
	 * the classifier and any options to the classifier
	 *
	 * @param c the classifier
	 * @return the classifier specification string.
	 */
	protected String getClassifierSpec(Classifier c) {

		if (c instanceof OptionHandler) {
			return c.getClass().getName() + " "
	+ Utils.joinOptions(((OptionHandler)c).getOptions());
		}
		return c.getClass().getName();
	}

	/**
	 * Makes a level-1 instance from the given instance.
	 *
	 * @param instance the instance to be transformed
	 * @return the level-1 instance
	 */
//	protected Instance metaInstance(Instance instance) throws Exception {
//
//		double[] values = new double[m_MetaFormat.numAttributes()];
//		Instance metaInstance;
//		int i = 0;
//		for (int k = 0; k < m_BaseClassifiers.length; k++) {
//			Classifier classifier = getBaseClassifier(k);
//			if (m_BaseFormat.classAttribute().isNumeric()) {
//	values[i++] = classifier.classifyInstance(instance);
//			} else {
//	if (classifier instanceof DistributionClassifier) {
//		double[] dist = ((DistributionClassifier)classifier).
//			distributionForInstance(instance);
//		for (int j = 0; j < dist.length; j++) {
//			values[i++] = dist[j];
//		}
//	} else {
//		values[i++] = classifier.classifyInstance(instance);
//	}
//			}
//		}
//		values[i] = instance.classValue();
//		metaInstance = new Instance(1, values);
//		metaInstance.setDataset(m_MetaFormat);
//		return metaInstance;
//	}

	private void addInstances(SetOfPatterns setOfPatterns, Instances instances,
	ScoreMultiClassClassifier scoreMultiClassClassifier) throws Exception {
		Enumeration enume = instances.enumerateInstances();
		while (enume.hasMoreElements()) {
			Instance inst = (Instance) enume.nextElement();
			//below doesn't work for SVM, so use raw scores
			//double[] dprob = ((ScoreMultiClassClassifier)m_BaseClassifier).getBinaryProbabilities(inst);
			double[] dprob = scoreMultiClassClassifier.getRawScoresFromBinaryClassifiers(inst);
			double[] dprobPlusClass = new double[dprob.length + 1];
			System.arraycopy(dprob,0,dprobPlusClass,0,dprob.length);
			dprobPlusClass[dprobPlusClass.length-1] = inst.classValue();
			//need a matrix
			double[][] dmatrix = new double[1][];
			dmatrix[0] = dprobPlusClass;
			setOfPatterns.addPattern(new Pattern(dmatrix));
		}
	}

	private Instances convert(SetOfPatterns setOfPatterns) {
		Instances instances = m_BaseFormat;
		//need all class labels
		String[] labels = new String[instances.numClasses()];
		//get class attribute
		Attribute a = instances.classAttribute();
		if (!a.isNominal()) {
			End.throwError("Class needs to be nominal!");
		}
		if (a.numValues() != labels.length) {
			End.throwError("Error in logic!");
		}
		for (int i = 0; i < labels.length; i++) {
			labels[i] = a.value(i);
		}
		Instances newInstances = WekaInterfacer.getInstances (labels, setOfPatterns);

		//keep a copy of the structure (NOW it's in buildClassifier()
		//int nnumberOfBinaryClassifiers = m_BaseClassifier.getNumberOfBinaryClassifiers();
		//m_MetaFormat = WekaInterfacer.createInstances(labels, nnumberOfBinaryClassifiers);
		return newInstances;
	}

	/**
	 * Should be used to generate test data, because binary classifiers
	 * were trained using all training data (not cross-validation).
	 */
	public Instances convertToMetaDataUsingFinalBaseClassifier(Instances instances) {
		int nnumberOfBinaryClassifiers = m_BaseClassifier.getNumberOfBinaryClassifiers();
		//add 1 for storing the class value
		SetOfPatterns setOfPatterns = new SetOfPatterns(new EmptyPatternGenerator(nnumberOfBinaryClassifiers + 1));
		try {
			addInstances(setOfPatterns, instances, m_BaseClassifier);
		}
		catch (Exception ex) {
			ex.printStackTrace();
			System.exit(1);
		}
		Instances metaData = convert(setOfPatterns);
		return metaData;
	}

	public Instances convertToMetaDataUsingCrossValidation(Instances data) throws Exception {
		checkConditions(data);

		//don't change input Instances, work with a copy
		Instances newData = new Instances(data);
		newData.deleteWithMissingClass();
		if (newData.numInstances() == 0) {
			throw new Exception("No training instances without missing class!");
		}

		newData.randomize(new Random(m_Seed));
		if (newData.classAttribute().isNominal()) {
			newData.stratify(m_NumFolds);
		}

		//pay attention to avoid changing m_BaseClassifier, so use a copy
		ScoreMultiClassClassifier scoreMultiClassClassifier = (ScoreMultiClassClassifier) Classifier.makeCopy(m_BaseClassifier);

		SetOfPatterns setOfPatterns = null;

		// Create meta data. This data is used for training the meta classifiers,
		// e.g., a neural network. Note that this meta data is generated by
		// using cross-validation, to avoid overfitting the training data
		for (int j = 0; j < m_NumFolds; j++) {
				Instances train = newData.trainCV(m_NumFolds, j);

				// Build base classifier without using fold j
				scoreMultiClassClassifier.buildClassifier(train);

				//Create "header". Do it only for first iteration
				if (setOfPatterns == null) {
					 int nnumberOfBinaryClassifiers = scoreMultiClassClassifier.getNumberOfBinaryClassifiers();
					 //add 1 for storing the class value
					 setOfPatterns = new SetOfPatterns(new EmptyPatternGenerator(nnumberOfBinaryClassifiers + 1));
				}

				// This step generates the binary decisions
				Instances test = newData.testCV(m_NumFolds, j);

				if (m_Debug) {
					Evaluation evaluation = new Evaluation(train);
					evaluation.evaluateModel(scoreMultiClassClassifier, test);
					System.err.println("Error rate (%) for fold " + j + " = " + IO.format(100 * evaluation.errorRate()) +
					" # errors = " + (int) evaluation.incorrect() + " out of " + (int) evaluation.numInstances());
				}

				//incorporate this fold j to meta data
				addInstances(setOfPatterns, test, scoreMultiClassClassifier);
		}
		return convert(setOfPatterns);
	}

	/**
	 * Sets debugging mode
	 *
	 * @param debug true if debug output should be printed
	 */
	public void setDebug(boolean debug) {

		m_Debug = debug;
	}

	/**
	 * Gets whether debugging is turned on
	 *
	 * @return true if debugging output is on
	 */
	public boolean getDebug() {

		return m_Debug;
	}

	public void setOutputTrainMetaFile(String outputTrainMetaFile) {
		m_outputTrainMetaFile = outputTrainMetaFile;
	}

	public String getOutputTrainMetaFile() {
		return m_outputTrainMetaFile;
	}

	public void setOutputTestMetaFile(String outputTestMetaFile) {
		m_outputTestMetaFile = outputTestMetaFile;
	}

	public String getOutputTestMetaFile() {
		return m_outputTestMetaFile;
	}

	public void setInputTestMetaFile(String inputTestMetaFile) {
		m_inputTestMetaFile = inputTestMetaFile;
	}

	public String getInputTestMetaFile() {
		return m_inputTestMetaFile;
	}

	/**
	 * Main method for testing this class.
	 *
	 * @param argv should contain the following arguments:
	 * -t training file [-T test file] [-c class index]
	 */
	public static void main(String [] argv) {
		try {
			System.out.println(Evaluation.evaluateModel(new TwoStagesMultiClassClassifier(), argv));
		} catch (Exception e) {
			e.printStackTrace(System.err);
			System.err.println("Maybe you used option -F for not training meta classifier?" +
			" It seems this classifier is null...");
		}
	}

}

package  weka.classifiers;

import  java.io.*;
import  java.util.*;
import  java.text.SimpleDateFormat;
import  weka.core.*;
import  edu.ucsd.asr.*;

/*
 *    InformaDiscriminativeClassifier.java
 * @author Aldebaro Klautau
 * @version $Revision: 2 $
 */
public class InformaDiscriminativeClassifier extends Classifier
		implements OptionHandler {

	static final long serialVersionUID = 71548383855535325L;

	//GaussiansMixture
	private DistributionClassifier m_informativeClassifier = new weka.classifiers.GaussiansMixture();
	//ScoreMultiClassClassifier
	private Classifier m_discriminativeClassifier = new weka.classifiers.AllPairsClassifier();
	//one should always change its value through setNumOfCandidates()
	//such that m_nlistWithNBest can be resized
	private int m_nNbestToBePassedToDiscriminative = 2;
	private int[] m_nlistWithNBest = new int[2];
	private boolean m_oforceCorrectInNBestList = false;
	//if true passes only numbers specified by N-best + configuration,
	//if false uses a modified Hastie method
	//private boolean m_ouseHardDecision = false;

	/**
	 * Use EM to estimate one mixture per class.
	 *
	 * @param data the training data
	 * @exception Exception if classifier can't be built successfully
	 */
	public void buildClassifier (Instances data) throws Exception {
		//m_gaussiansMixture = new GaussiansMixture();
		m_informativeClassifier.buildClassifier(data);
		//m_scoreMultiClassClassifier = new ScoreMultiClassClassifier();
		m_discriminativeClassifier.buildClassifier(data);
	}

	private void forceCorrectClassToBeInNBestList (Instance instance) {
		int correctClass = (int)instance.classValue();
		boolean ofoundCorrect = false;
		for (int i = 0; i < m_nlistWithNBest.length; i++) {
			if (m_nlistWithNBest[i] == correctClass) {
				ofoundCorrect = true;
				break;
			}
		}
		if (!ofoundCorrect) {
			//substitute element with smallest rank by correct class
			m_nlistWithNBest[m_nlistWithNBest.length - 1] = correctClass;
		}
	}

	/**
	 * Computes class distribution for instance using mixtures.
	 *
	 * @param instance the instance for which distribution is to be computed
	 * @return the class distribution for the given instance
	 */
	public double classifyInstance (Instance instance) throws Exception {
		//		Print.dialog("Info + disc");
		//		IO.DisplayVector(m_informativeClassifier.distributionForInstance(instance));
		//		IO.DisplayVector(m_discriminativeClassifier.distributionForInstance(instance));
		double[] doriginalDistribution = m_informativeClassifier.distributionForInstance(instance);
		int[] sortedByProbability = Utils.sort(doriginalDistribution);
		//IO.DisplayVector(doriginalDistribution);
		//IO.DisplayVector(sortedByProbability);
		try {
			for (int i = 0; i < m_nNbestToBePassedToDiscriminative; i++) {
				//Utils.sort() returns in ascending order, so invert it:
				m_nlistWithNBest[i] = sortedByProbability[sortedByProbability.length - 1
						- i];
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			e.printStackTrace();
			Print.error("You set the number of candidates to be" +
			" passed to discriminative classifiers N = " + getNumOfCandidates() +
			" and there are " + doriginalDistribution.length + " classes" +
			" The number of classes cannot be bigger than N.");
			System.exit(1);
		}
		//hack to pass always correct class in N-best list
		if (m_oforceCorrectInNBestList) {
			forceCorrectClassToBeInNBestList(instance);
		}
		try {
			((AllPairsClassifier)m_discriminativeClassifier).setActiveClassifiers(m_nlistWithNBest);
			((AllPairsClassifier)m_discriminativeClassifier).setProbabilitiesGivenByInformative(doriginalDistribution);
		} catch (ClassCastException e) {
			Print.error(m_discriminativeClassifier.getClass().toString() + " is not the AllPairsClassifier !");
			e.printStackTrace();
			System.exit(1);
		}
		return  ((AllPairsClassifier)m_discriminativeClassifier).classifyInstance(instance);
		// Below what I was using for the original informative
		//return  ((AllPairsClassifier)m_discriminativeClassifier).distributionForInstanceWithOnlyActiveClassifiers(instance);
		// Below for experiments with a modified Hastie's method,
		//with weights provided by informative classifier
		//return  m_discriminativeClassifier.distributionForInstance(instance);
		//create array of double with normalized values
		//		double[] dprob = new double[prob.length];
		//		for (int i = 0; i < dprob.length; i++) {
		//			//dprob[i] = prob[i] / dsum;
		//		}
		//		return  dprob;
	}

	public void setNumOfCandidates (int nNbestToBePassedToDiscriminative) {
		m_nNbestToBePassedToDiscriminative = nNbestToBePassedToDiscriminative;
		m_nlistWithNBest = new int[m_nNbestToBePassedToDiscriminative];
	}

//	public void setUseHardDecision(boolean ouse) {
//		m_ouseHardDecision = ouse;
//		if (!m_ouseHardDecision) {
//			 m_oforceCorrectInNBestList = false;
//		}
//	}
//
//	public boolean getUseHardDecision() {
//		return m_ouseHardDecision;
//	}

	public int getNumOfCandidates () {
		return  m_nNbestToBePassedToDiscriminative;
	}

	public void setForceCorrectClassToBeInNBestList (boolean oshouldForce) {
		m_oforceCorrectInNBestList = oshouldForce;
	}

	public boolean getForceCorrectClassToBeInNBestList () {
		return  m_oforceCorrectInNBestList;
	}

	public void setInformativeClassifier (DistributionClassifier newClassifier) {
		m_informativeClassifier = newClassifier;
	}

	public void setDiscriminativeClassifier (DistributionClassifier newClassifier) {
		m_discriminativeClassifier = newClassifier;
	}

	public DistributionClassifier getInformativeClassifier () {
		return  m_informativeClassifier;
	}

	public Classifier getDiscriminativeClassifier () {
		return  m_discriminativeClassifier;
	}

	/**
	 * Prints the decision tree using the private toString method from below.
	 *
	 * @return a textual description of the classifier
	 */
	public String toString () {
		StringBuffer stringBuffer = new StringBuffer("InformaDiscriminativeClassifier:\n");
		//TODO: should print config here
		stringBuffer.append("Informative:\n" + m_informativeClassifier.toString());
		stringBuffer.append("Discriminative:\n" + m_discriminativeClassifier.toString());
		return  stringBuffer.toString();
	}

	public void setOptions (String[] options) throws Exception {
		String forceCorrect = Utils.getOption('F', options);
		if (forceCorrect.length() != 0) {
			m_oforceCorrectInNBestList = Boolean.valueOf(forceCorrect).booleanValue();
		}
//		String hardDecision = Utils.getOption('H', options);
//		if (hardDecision.length() != 0) {
//			m_ouseHardDecision = Boolean.valueOf(hardDecision).booleanValue();
//		}
		String nbestToBePassedToDiscriminative = Utils.getOption('N', options);
		if (nbestToBePassedToDiscriminative.length() != 0) {
			setNumOfCandidates(Integer.parseInt(nbestToBePassedToDiscriminative));
		}
		else {
			setNumOfCandidates(2);
		}
		String informativeClassifierName = Utils.getOption('I', options);
		if (informativeClassifierName.length() == 0) {
			throw  new Exception("A classifier must be specified with" + " the -I option.");
		}
		String discriminativeClassifierName = Utils.getOption('D', options);
		if (discriminativeClassifierName.length() == 0) {
			throw  new Exception("A classifier must be specified with" + " the -D option.");
		}
		//partitionOptions() returns everything after first --
		//Assumes the informative classifier doesn't have multiple -- segments
		String[] classifiersOptions = Utils.getOptionsBetweenDashes(options);
		setInformativeClassifier((DistributionClassifier)Classifier.forName(informativeClassifierName,
				classifiersOptions));
		setDiscriminativeClassifier((DistributionClassifier)Classifier.forName(discriminativeClassifierName,
				Utils.partitionOptions(options)));
	}

	public Enumeration listOptions () {
		Vector newVector = new Vector();
		newVector.addElement(new Option("\tForce the correct class to be in N-best list (default false).",
				"F", 1, "-F <true | false>"));
//		newVector.addElement(new Option("\tUse hard-decision (default false).", "H",
//				1, "-H <true | false>"));
		newVector.addElement(new Option("\tNumber N of N-best candidates to be passed"
				+ " to discriminative classifiers (default 2).", "N", 1, "-N <N-best>"));
		newVector.addElement(new Option("\tSets the base classifier.", "I", 1, "-I <informative classifier>"));
		newVector.addElement(new Option("\tSets the base classifier.", "D", 1, "-D <discriminative classifier>"));
		//		if (m_gaussiansMixture != null) {
		newVector.addElement(new Option("", "", 0, "\nOptions specific to informative classifier "
				+ m_informativeClassifier.getClass().getName() + ":"));
		Enumeration enume = ((OptionHandler)m_informativeClassifier).listOptions();
		while (enume.hasMoreElements()) {
			newVector.addElement(enume.nextElement());
		}
		//		}
		//		if (m_scoreMultiClassClassifier != null) {
		newVector.addElement(new Option("", "", 0, "\nOptions specific to discriminative classifier "
				+ m_discriminativeClassifier.getClass().getName() + ":"));
		//Enumeration
		enume = ((OptionHandler)m_discriminativeClassifier).listOptions();
		while (enume.hasMoreElements()) {
			newVector.addElement(enume.nextElement());
		}
		//		}
		return  newVector.elements();
	}

	/**
	 * Gets the current settings of the Classifier.
	 *
	 * @return an array of strings suitable for passing to setOptions
	 */
	//options for informative classifier should come before
	public String[] getOptions () {
		String[] informativeClassifierOptions = new String[0];
		if (m_informativeClassifier != null && (m_informativeClassifier instanceof OptionHandler)) {
			informativeClassifierOptions = ((OptionHandler)m_informativeClassifier).getOptions();
		}
		String[] discriminativeClassifierOptions = new String[0];
		if (m_discriminativeClassifier != null && (m_discriminativeClassifier instanceof OptionHandler)) {
			discriminativeClassifierOptions = ((OptionHandler)m_discriminativeClassifier).getOptions();
		}
		//give some large number of options, e.g. 12
		String[] options = new String[discriminativeClassifierOptions.length + informativeClassifierOptions.length
				+ 12];
		int current = 0;
		options[current++] = "-F";
		options[current++] = "" + getForceCorrectClassToBeInNBestList();
		options[current++] = "-N";
		options[current++] = "" + getNumOfCandidates();
		if (getInformativeClassifier() != null) {
			options[current++] = "-I";
			options[current++] = getInformativeClassifier().getClass().getName();
		}
		if (getDiscriminativeClassifier() != null) {
			options[current++] = "-D";
			options[current++] = getDiscriminativeClassifier().getClass().getName();
		}
		options[current++] = "--";
		System.arraycopy(informativeClassifierOptions, 0, options, current, informativeClassifierOptions.length);
		current += informativeClassifierOptions.length;
		options[current++] = "--";
		System.arraycopy(discriminativeClassifierOptions, 0, options, current, discriminativeClassifierOptions.length);
		current += discriminativeClassifierOptions.length;
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
		return  "Combines an informative and a discriminative classifiers." + " Both are trained with all training data."
				+ " For testing, only the N-best candidates returned by the informative"
				+ " classifier are passed to the discriminative. The main goal is to" +
				" make all-pair discriminative classifiers more robust.";
	}

	/**
	 * Main method.
	 *
	 * @param args the options for the classifier
	 */
	public static void main (String[] args) {
		try {
			System.out.println(Evaluation.evaluateModel(new InformaDiscriminativeClassifier(),
					args));
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println(e.getMessage());
		}
	}
}




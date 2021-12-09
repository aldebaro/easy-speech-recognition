package  weka.classifiers;

import  java.io.*;
import  java.util.*;
import  java.text.SimpleDateFormat;
import  weka.core.*;
import  edu.ucsd.asr.*;

/*
 *    OldGaussiansMixture.java
 * @author Aldebaro Klautau
 * @version $Revision: 2 $
 */

 //I will try to do the whole training using m_mixturesOfGaussianPDFsBeingReestimated
 //and after training, I nullify the Instances data and then I create
 //m_mixturesOfGaussianPDFs for using in classification.

 //testing with the following command line
 //java weka.classifiers.GaussiansMixture -t vowel_train.arff -T vowel_test.arff -I 2 > t
 //java weka.classifiers.GaussiansMixture -t vowel_train.arff -T vowel_test.arff -I 2 -D -Q 4000 -N 100 -V -o 2> t
 //java weka.classifiers.GaussiansMixture -t tr.arff -T te.arff -I 20 -D -Q 999999 -N 100 -V -o
public class OldGaussiansMixture extends DistributionClassifier
		implements WeightedInstancesHandler, RawScorer, OptionHandler {

	public static final long serialVersionUID = 715933919145242235L;
	public static final int m_maximumNumberOfRepeatedSmallImprovementsForMMIE = 3;
	public static final int m_nmaximumNumberOfIterationsWithoutSignificantImprovementForMMIE = 10;

	private MixtureOfGaussianPDFs[] m_mixturesOfGaussianPDFs;
	private MixtureOfGaussianPDFsBeingReestimated[] m_mixturesOfGaussianPDFsBeingReestimated;
	private float[] m_fpriors;
	private String[] m_classNames;
	private int m_nnumberOfGaussiansDiscarded;
	private float m_flogOutputProbability;
	private float[] m_fauxiliaryZeroMean;

	//options
	private boolean m_oshouldUpdateWeights = true;
	private boolean m_oshouldUpdateMean = true;
	private boolean m_oshouldUpdateCovariance = true;
	private boolean m_odebug = false;
	//private int m_nverbose = 1;
	private boolean m_oshouldWriteReportFile = false;
	private String m_reportFileName = "em.log";
	private float m_fmixtureWeightFloor = 0.0F;
	private int m_nmaximumIterations = 100;
	private float m_fconvergenceThreshold = 1e-4F;
	private float m_fcovarianceFloor = 1e-4F;
	private int m_numberOfGaussians = 1;
	private boolean m_oshouldUsePriors = false;

	private boolean m_ouseMixUpSplittingInsteadOfKMeans = false;
	private boolean m_ouseMMIE = false;
	private MixtureOfGaussianPDFs m_generatorModelForMMIE;
	private MixtureOfGaussianPDFsBeingReestimated m_generatorModelForMMIEBeingReestimated;
	private int[][] m_indexInGeneratorModel;
	private float m_fMMIEConstantD = 200;

	/** Class attribute of dataset. */
	//private Attribute m_ClassAttribute;
	//keep for compatibility with SetOfMatrixEncodedHMMs
	public void setMixtures (MixtureOfGaussianPDFs[] mixtures) {
		m_mixturesOfGaussianPDFs = mixtures;
		//there is not information about priors
		m_oshouldUsePriors = false;
	}

	/**
	 * Use EM to estimate one mixture per class.
	 *
	 * @param data the training data
	 * @exception Exception if classifier can't be built successfully
	 */
	public void buildClassifier (Instances data) throws Exception {
		Enumeration enumAtt = data.enumerateAttributes();
		while (enumAtt.hasMoreElements()) {
			Attribute attr = (Attribute)enumAtt.nextElement();
			//      if (!attr.isNominal()) {
			//        throw new Exception("GaussiansMixture: all attributes are numeric?? The last one should be nominal, please.");
			//      }
			Enumeration enume = data.enumerateInstances();
			while (enume.hasMoreElements()) {
				if (((Instance)enume.nextElement()).isMissing(attr)) {
					throw new Exception("GaussiansMixture: no missing values, please.");
				}
			}
		}
		//data = new Instances(data);
		//data.deleteWithMissingClass();
		if (data.checkForStringAttributes()) {
			throw new Exception("Can't handle string attributes!");
		}
//    if (data.numClasses() > 2) {
//      throw new Exception("Can only handle two-class datasets!");
//    }
		if (data.classAttribute().isNumeric()) {
			throw new Exception("GaussiansMixture can't handle a numeric class!");
		}
		//assume last attribute is the class
		if (data.classIndex() != (data.numAttributes() - 1)) {
			throw new Exception("AK: Class must be last attribute for GaussiansMixture!");
		}
		// Check if no instances are available
		if (data.numInstances() == 0) {
			m_mixturesOfGaussianPDFs = null;
			m_mixturesOfGaussianPDFsBeingReestimated = null;
			m_fpriors = null;
			return;
		}
		if (data.sumOfWeights() == 0.0) {
			throw new Exception("All weights are 0 !");
			//data.setAllWeights(1.0);
		}
		//allocate space
		int nnumberOfClasses = data.classAttribute().numValues();

		//m_mixturesOfGaussianPDFs = new MixtureOfGaussianPDFs[nnumberOfClasses];
		m_mixturesOfGaussianPDFsBeingReestimated = new MixtureOfGaussianPDFsBeingReestimated[nnumberOfClasses];
		m_fpriors = new float[nnumberOfClasses];
		m_classNames = new String[nnumberOfClasses];
		//auxiliary variable
		m_fauxiliaryZeroMean = new float[data.numAttributes() - 1];

		for (int i = 0; i < nnumberOfClasses; i++) {
			m_classNames[i] = data.classAttribute().value(i);
		}

		Enumeration enume = data.enumerateInstances();
		long lsum = 0;
		while (enume.hasMoreElements()) {
			Instance inst = (Instance) enume.nextElement();
			m_fpriors[((int) inst.classValue())]++;
			lsum++;
		}

		//Calculate priors. Avoid giving 0 probability to any class
		for (int i = 0; i < m_fpriors.length; i++) {
			if (m_fpriors[i] == 0) {
				m_fpriors[i] = 1;
				lsum++;
			}
		}
		for (int i = 0; i < m_fpriors.length; i++) {
			m_fpriors[i] /= lsum;
		}

		//initialize m_mixturesOfGaussianPDFsBeingReestimated with final number
		//of Gaussians specified by user
		if (m_ouseMixUpSplittingInsteadOfKMeans) {
			createMixturesInitializedByUpMix(data);
		} else {
			createMixturesInitializedByKMeans(data);
		}

		if (m_ouseMMIE) {
			//use the MLE as starting point for MMIE
			m_ouseMMIE = false;
			if (m_odebug) {
				System.err.println("First estimating with MLE:");
			}
			reestimateAllMixtures(data);
			m_ouseMMIE = true;
			if (m_odebug) {
				System.err.println("Now, estimating with MMIE:");
			}
			reestimateAllMixtures(data);
			if (m_mixturesOfGaussianPDFs == null) {
				System.err.println("WARNING: MMIE did not go through...");
				reestimateAllMixtures(data);
			}
		} else {
			//MLE
			reestimateAllMixtures(data);
			//in the case of MMIE, this method is called by reestimateAllMixtures
			keepEstimatedMixtures();
		}
		//don't need it, save RAM
		data = null;

		discardGaussiansWithSmallWeight();
		//don't need it, save RAM
		m_mixturesOfGaussianPDFsBeingReestimated = null;
		//invite garbage collection
		System.gc();
	}

	private void keepEstimatedMixtures() {
		//here I create m_mixturesOfGaussianPDFs that will be used for testing
		int nnumberOfClasses = m_mixturesOfGaussianPDFsBeingReestimated.length;
		m_mixturesOfGaussianPDFs = new MixtureOfGaussianPDFs[nnumberOfClasses];
		for (int i = 0; i < m_mixturesOfGaussianPDFs.length; i++) {
			m_mixturesOfGaussianPDFs[i] = m_mixturesOfGaussianPDFsBeingReestimated[i].getMixtureOfGaussianPDFs();
		}
	}

	private void discardGaussiansWithSmallWeight() {
		int nfinalNumberOfGaussians = 0;
		int nnumberOfClasses = m_mixturesOfGaussianPDFsBeingReestimated.length;
		for (int i = 0; i < m_mixturesOfGaussianPDFs.length; i++) {
			try {
				m_mixturesOfGaussianPDFs[i].discardGaussianWithVerySmallWeights();
			} catch (ASRError e) {
				//do nothing. I using a more general method for counting
				//that takes in account that the initialization may had
				//returned fewer Gaussians than specified
				//m_nnumberOfGaussiansDiscarded += e.getAuxiliaryValue();
			}
			nfinalNumberOfGaussians += m_mixturesOfGaussianPDFs[i].getNumberOfGaussians();
		}
		m_nnumberOfGaussiansDiscarded = (m_numberOfGaussians * nnumberOfClasses) -
																	nfinalNumberOfGaussians;
	}

	/**
	 * Classifies a given test instance.
	 *
	 * @param instance the instance to be classified
	 * @return the classification
	 */
	public double classifyInstance (Instance instance) {
		double[] probs = distributionForInstance(instance);
		double dmax = Double.NEGATIVE_INFINITY;
		int nmax = -1;
		for (int i = 0; i < probs.length; i++) {
			if (probs[i] > dmax) {
				dmax = probs[i];
				nmax = i;
			}
		}
		return (double) nmax;
	}

	public double getRawScore(Instance instance) {
		float[] x = getLogProbabilities(instance);
		double y = Math.exp(x[1] - x[0]);
		double p0 = 1.0 / (1.0 + y);
		double p1 = 1 - p0;
		return (p1 - p0);
	}

	public float[] getLogProbabilities(Instance instance) {
		float[] x = instance.getAttributesDiscardingLastOne();
		float[] logprob = new float[m_mixturesOfGaussianPDFs.length];
		for (int i = 0; i < logprob.length; i++) {
			if (m_mixturesOfGaussianPDFs[i] != null) {
				logprob[i] = m_mixturesOfGaussianPDFs[i].calculateLogProbability(x);
			} else {
				logprob[i] = LogDomainCalculator.m_fLOG_DOMAIN_ZERO;
			}
		}
		return  logprob;
	}

	/**
	 * Computes class distribution for instance using mixtures.
	 *
	 * @param instance the instance for which distribution is to be computed
	 * @return the class distribution for the given instance
	 */
	public double[] distributionForInstance (Instance instance) {

		//log numbers can be as low as -2000 and floor for exp is -700
		float[] logprob = getLogProbabilities(instance);
		float[] prob = LogDomainCalculator.calculateExp(logprob);

		//IO.DisplayVector(prob);
		//IO.DisplayVector(logprob);

		if (m_oshouldUsePriors) {
			for (int i = 0; i < prob.length; i++) {
				prob[i] *= m_fpriors[i];
			}
		}

		double dsum = 0;
		for (int i = 0; i < prob.length; i++) {
			dsum += prob[i];
		}
		double[] dprob = new double[prob.length];
		if (dsum == 0) {
			//convention adopted by Weka: return 0 for all classes if
			//there are problems
			//return new double[prob.length];
			//here I will try another approach: use log probabilities
			//to provide ranking, even if they are not "calibrated" probabilities
			//I could use exp if restrict the range in the log domain
			//but I will simply pay attention for ranking
				//Double.MIN_VALUE = 4.9E-324 (log = -744)
				//so, use -700 as floor value
			double dmin = Double.MAX_VALUE;
			for (int i = 0; i < logprob.length; i++) {
				if (logprob[i] < dmin) {
					dmin = logprob[i];
				}
			}
			if (dmin < 0) {
				dmin = Math.abs(dmin);
			}
			for (int i = 0; i < logprob.length; i++) {
				dprob[i] = logprob[i] + dmin + 1e-30;
			}
			//for (int i = 0; i < prob.length; i++) {
			//	dprob[i] = Math.exp(dprob[i]);
			//}
			//IO.DisplayVector(dprob);
			dsum = 0;
			for (int i = 0; i < dprob.length; i++) {
				dsum += dprob[i];
			}
			for (int i = 0; i < dprob.length; i++) {
				dprob[i] = dprob[i] / dsum;
			}
		} else {
			//sum is not 0, so use probabilities
			//use normalized values
			for (int i = 0; i < dprob.length; i++) {
				dprob[i] = prob[i] / dsum;
			}
		}

		//IO.DisplayVector(dprob);
		return  dprob;
	}

	/**
	 * Prints the mixture of Gaussians using the private toString method from below.
	 *
	 * @return a textual description of the classifier
	 */
	public String toString () {
		if (m_mixturesOfGaussianPDFs == null) {
			return "GaussiansMixture: No model built yet";
		}
		StringBuffer stringBuffer = new StringBuffer("GaussiansMixture:\n" +
		"Number of classes = " + m_mixturesOfGaussianPDFs.length + "\n");
		for (int i = 0; i < m_mixturesOfGaussianPDFs.length; i++) {
			stringBuffer.append("\n\nClass " + i + " | label " + m_classNames[i]);
			stringBuffer.append(" | Prior = " + m_fpriors[i] + "\n");
			stringBuffer.append(m_mixturesOfGaussianPDFs[i].toString());
		}
		stringBuffer.append("\n\nThe number of Gaussians per mixture specified by the user was "
				+ m_numberOfGaussians + " per class.\n");
		stringBuffer.append("A total of " + m_nnumberOfGaussiansDiscarded + " Gaussians were discarded during training due to their small weights.\n");
		return  stringBuffer.toString();
	}

	private float updatePDFAccumulators (Instance inst, int nclassValue) throws Exception {
		MixtureOfGaussianPDFsBeingReestimated mixtureOfGaussianPDFsBeingReestimated = m_mixturesOfGaussianPDFsBeingReestimated[nclassValue];
		float[] fobservation = inst.getAttributesDiscardingLastOne();
		double dinstanceLogWeight = LogDomainCalculator.calculateLog(inst.weight());
		return newUpdatePDFAccumulators(fobservation, dinstanceLogWeight, mixtureOfGaussianPDFsBeingReestimated);
	}

	private float newUpdatePDFAccumulators(float[] fobservation, double dinstanceLogWeight,
	MixtureOfGaussianPDFsBeingReestimated mixtureOfGaussianPDFsBeingReestimated) {
		float[] findividualAndTotalProbabilities = mixtureOfGaussianPDFsBeingReestimated.calculateIndividualProbabilities(fobservation);

		//System.out.println("###########");
		//IO.DisplayVector(fobservation);
		//IO.DisplayVector(findividualAndTotalProbabilities);

		//total mixture probability is last element of findividualAndTotalProbabilities
		float flogOutputProbability = findividualAndTotalProbabilities[findividualAndTotalProbabilities.length-1];
		//store individual (each Gaussian) prob
		float[] flogWeights = mixtureOfGaussianPDFsBeingReestimated.getComponentsWeightsInLogDomain();
		//Print.dialog(inst.weight() + " log => " + dinstanceLogWeight);
		PDFBeingReestimated[] pDFsBeingReestimated = mixtureOfGaussianPDFsBeingReestimated.getReferenceToPDFsBeingReestimated();
		for (int m = 0; m < mixtureOfGaussianPDFsBeingReestimated.getNumberOfGaussians(); m++) {
			PDFBeingReestimated pDFBeingReestimated = pDFsBeingReestimated[m];
			double Lr = LogDomainCalculator.m_fLOG_DOMAIN_ZERO;
			if (flogWeights[m] > MixtureOfGaussianPDFsBeingReestimated.m_fMINIMUM_LOG_WEIGHT) {
				pDFBeingReestimated.subtractMean(fobservation, m_fauxiliaryZeroMean);
				if (mixtureOfGaussianPDFsBeingReestimated.getNumberOfGaussians() == 1) {
					Lr = dinstanceLogWeight;
				}
				else {
					Lr = dinstanceLogWeight + findividualAndTotalProbabilities[m] + flogWeights[m] - flogOutputProbability;
				}
				//Print.dialog(j +" "+ m+" "+t+" "+Lr +" "+ mixp_j[t][m] +" "+  flogWeights[m] +" "+ betaj[t] +" "+ dlogProbabilityOfCurrentPattern);
			}
			if (Lr > LogDomainCalculator.m_fMINIMUM_EXP_ARGUMENT) {
				float y = (float)LogDomainCalculator.calculateExp(Lr);
				//update weight counter
				if (m_oshouldUpdateWeights) {
					mixtureOfGaussianPDFsBeingReestimated.updateWeightStatistics(m, y);
				}
				//update mean counter
				if (m_oshouldUpdateMean) {
					mixtureOfGaussianPDFsBeingReestimated.updateMeanStatistics(m, y, m_fauxiliaryZeroMean);
				}
				//update covariance counter
				if (m_oshouldUpdateCovariance) {
					mixtureOfGaussianPDFsBeingReestimated.updateCovarianceStatistics(m, y, m_fauxiliaryZeroMean);
				}
			}
//			else {
//				//ak
//				System.out.println("Skipped " + m);
//			}
		}
		return  flogOutputProbability;
	}

	/**
	 * Initialize all mixtures using segmental K-means.
	 */
	 //the mixtures may end up having a number of Gaussians
	 //smaller than the specified. That happens when there
	 //are less examples than the specified # of Gaussians
	 //per mixture, for example.
	private void createMixturesInitializedByKMeans (Instances insts) throws Exception {
		int nnumberOfClasses = insts.classAttribute().numValues();
		int nnumberOfStatesForHMMWith1EmittingState = 3;
		for (int i = 0; i < m_mixturesOfGaussianPDFsBeingReestimated.length; i++) {
			double dclassValue = i;
			SetOfPatterns setOfPatterns = WekaInterfacer.instancesToSetOfPatterns(insts,
					dclassValue);
			//get initial prototype
			ContinuousHMM continuousHMM = HMMInitializer.createHMMWith1State(nnumberOfClasses);
			//Use segmental K-means to obtain given number of mixtures
			HMMInitializer hMMInitializer = new HMMInitializer(setOfPatterns,
			nnumberOfStatesForHMMWith1EmittingState, m_numberOfGaussians,
					0.005, "kmeans.log", 1);
			continuousHMM = hMMInitializer.getHMMUsingViterbiAlignmentAndKMeans();
			MixtureOfGaussianPDFs[] mixs = continuousHMM.getMixturesOfGaussianPDFs();
			if (mixs.length != 1) {
				End.throwError("mixs.length = " + mixs.length + " should be 1 !");
			}
			m_mixturesOfGaussianPDFsBeingReestimated[i] = new MixtureOfGaussianPDFsBeingReestimated (mixs[0]);
		}

//		for (int i = 0; i < m_mixturesOfGaussianPDFsBeingReestimated.length; i++) {
//			reestimateMixtures(insts, m_nmaximumIterations, m_fconvergenceThreshold, m_fcovarianceFloor, i);
//		}
	}

	//K-means doesn't seem to be working, so use upmix
	private void createMixturesInitializedByUpMix (Instances insts) throws Exception {
		int nnumberOfClasses = insts.classAttribute().numValues();
		int nnumberOfStatesForHMMWith1EmittingState = 3;
		for (int i = 0; i < m_mixturesOfGaussianPDFsBeingReestimated.length; i++) {
			double dclassValue = i;
			SetOfPatterns setOfPatterns = WekaInterfacer.instancesToSetOfPatterns(insts,
					dclassValue);
			//get initial prototype
			ContinuousHMM continuousHMM = HMMInitializer.createHMMWith1State(nnumberOfClasses);
			//Use segmental K-means to obtain given number of mixtures
			HMMInitializer hMMInitializer = new HMMInitializer(setOfPatterns,
			nnumberOfStatesForHMMWith1EmittingState, 1,
					0.01, "kmeans.log", 1);
			continuousHMM = hMMInitializer.getHMMUsingViterbiAlignmentAndKMeans();
			MixtureOfGaussianPDFs[] mixs = continuousHMM.getMixturesOfGaussianPDFs();
			if (mixs.length != 1) {
				End.throwError("mixs.length = " + mixs.length + " should be 1 !");
			}
			m_mixturesOfGaussianPDFsBeingReestimated[i] = new MixtureOfGaussianPDFsBeingReestimated (mixs[0]);
		}

		for (int i = 0; i < m_mixturesOfGaussianPDFsBeingReestimated.length; i++) {
			reestimateMixtures(insts, m_nmaximumIterations, m_fconvergenceThreshold, m_fcovarianceFloor, i);
		}
		if (m_numberOfGaussians > 1) {

			//do up-mix
			for (int j = 2; j <= m_numberOfGaussians; j++) {
				for (int i = 0; i < m_mixturesOfGaussianPDFsBeingReestimated.length; i++) {
					//AK: below is wrong, not tested, etc.
					m_mixturesOfGaussianPDFsBeingReestimated[i].reestimateMixture(1, m_oshouldUpdateMean,
								m_oshouldUpdateCovariance, m_oshouldUpdateWeights, m_fcovarianceFloor,
								m_fmixtureWeightFloor);
					m_mixturesOfGaussianPDFsBeingReestimated[i].splitGaussianWithLargestWeight();
					reestimateMixtures(insts, m_nmaximumIterations, m_fconvergenceThreshold, m_fcovarianceFloor, i);
				}
			}
		} else {
			//simply reestimate Gaussians
			for (int i = 0; i < m_mixturesOfGaussianPDFsBeingReestimated.length; i++) {
				reestimateMixtures(insts, m_nmaximumIterations, m_fconvergenceThreshold, m_fcovarianceFloor, i);
			}
		}
	}

	//this is the original, which reestimate the mixtures of 1 class each time,
	//while I am writing a method that does only 1 pass over the training set
	private void reestimateMixtures (Instances insts, int nmaximumIterations, float fconvergenceThreshold,
			float fcovarianceFloor, int nclass) throws Exception {

		int ntotalNumberOfPatterns = insts.numInstances();
		StringBuffer informationForLogFile = new StringBuffer();
		if (m_odebug) {
			Print.dialog("EM for class " + nclass);
		}
		if (m_oshouldWriteReportFile) {
			SimpleDateFormat formatter = new SimpleDateFormat("dd/MMMMM/yyyyy 'at' hh:mm:ss aaa");
			//SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
			informationForLogFile.append("Date = " + formatter.format(new Date()) +
					"." + IO.m_NEW_LINE);
			informationForLogFile.append("Processing " + insts.relationName() + " with "
					+ ntotalNumberOfPatterns + " total patterns (but here only the subset of instances of a given class will be used." + IO.m_NEW_LINE);
			informationForLogFile.append("EM for class " + nclass + IO.m_NEW_LINE);
		}
		//initialize some variables
		boolean oconverged = false;                 //true when algorithm converged
		float flogOldProbability = LogDomainCalculator.m_fLOG_DOMAIN_ZERO;
		int niterations = 0;
		//for (int i = 0; i < m_mixturesOfGaussianPDFs.length; i++) {
		//m_mixturesOfGaussianPDFsBeingReestimated[nclass] = new MixtureOfGaussianPDFsBeingReestimated(m_mixturesOfGaussianPDFs[nclass]);
		//m_mixturesOfGaussianPDFsBeingReestimated[nclass] = m_mixturesOfGaussianPDFsBeingReestimated[nclass];
		//}
		//main reestimation loop
		do {
			//zero accumulators used for reestimating model
			//for (int i = 0; i < m_mixturesOfGaussianPDFsBeingReestimated.length; i++) {
				m_mixturesOfGaussianPDFsBeingReestimated[nclass].zeroAccumulators();
			//}
			int nnumberOfValidPatterns = 0;
			float flogProbabilityOfAllPatterns = 0.0F;                //1 in log domain
			//float flogNewProbability = 0.0F;
			for (int npatternNumber = 0; npatternNumber < ntotalNumberOfPatterns; npatternNumber++) {
				int nnumberOfPatternsSkipped = 0;
				//update current pattern
				Instance inst = insts.instance(npatternNumber);
				int nclassValue = (int) inst.classValue();
				if (nclassValue == nclass) {
					//update counters
					float flogProbabilityOfThisPattern = updatePDFAccumulators(inst, nclass);
					flogProbabilityOfAllPatterns += flogProbabilityOfThisPattern;
					nnumberOfValidPatterns++;
				}
				//showAccumulators();
			}
			//update model
			//updateModel(fcovarianceFloor);
			//for (int i = 0; i < m_mixturesOfGaussianPDFs.length; i++) {
			m_mixturesOfGaussianPDFsBeingReestimated[nclass].reestimateMixture(1, m_oshouldUpdateMean,
						m_oshouldUpdateCovariance, m_oshouldUpdateWeights, fcovarianceFloor,
						m_fmixtureWeightFloor);
			//}
			//normalize
			float flogProbPerPattern = flogProbabilityOfAllPatterns/nnumberOfValidPatterns;
			float flogImprovement = flogProbPerPattern - flogOldProbability;
			if (m_oshouldWriteReportFile || m_odebug) {
				int ltotalNumberOfFramesInValidPatterns = nnumberOfValidPatterns;
				double dlogProbPerFrame = (double)flogProbabilityOfAllPatterns/ltotalNumberOfFramesInValidPatterns;
				String information = null;
				if (niterations > 0) {
					if (niterations > 9) {
						//just to have a fancy console output :)
						information = niterations + "| average log prob./frame=" + IO.format(dlogProbPerFrame)
								+ ", /token=" + IO.format(flogProbPerPattern) + ", convergence="
								+ IO.format(flogImprovement);
					}
					else {
						information = niterations + " | average log prob./frame=" + IO.format(dlogProbPerFrame)
								+ ", /token=" + IO.format(flogProbPerPattern) + ", convergence="
								+ IO.format(flogImprovement);
					}
				}
				else {
					information = niterations + " | average log prob./frame=" + IO.format(dlogProbPerFrame)
							+ ", /token=" + IO.format(flogProbPerPattern);
				}
				if (m_oshouldWriteReportFile) {
					informationForLogFile.append(information + IO.m_NEW_LINE);
				}
				if (m_odebug) {
					Print.dialog(information);
				}
			}
			oconverged = (Math.abs(flogImprovement) < fconvergenceThreshold);
			//update for next iteration
			flogOldProbability = flogProbPerPattern;
			niterations++;
		} while ((niterations < nmaximumIterations) && !oconverged);
		if (m_oshouldWriteReportFile) {
			informationForLogFile.append("Iterations = " + niterations + "." + IO.m_NEW_LINE);
			//call method to write file
			IO.appendStringToEndOfTextFile(m_reportFileName, informationForLogFile.toString());
		}
		if (m_odebug) {
			if (oconverged) {
				Print.dialog("Reestimation converged. Total number of iterations = " +
						niterations);
			}
			else {
				Print.dialog("Reestimation aborted. Total number of iterations = " + niterations);
			}
		}
	}             //end of method


	private void reestimateAllMixtures (Instances insts) throws Exception {
		if (m_ouseMMIE) {
			reestimateAllMixturesWithMMIE(insts);
		} else {
			int nnumberOfClasses = insts.numClasses();
			for (int i = 0; i < nnumberOfClasses; i++) {
				reestimateMixtures(insts, m_nmaximumIterations, m_fconvergenceThreshold,
				m_fcovarianceFloor, i);
			}
		}
	}

	//this method is now used only by MMIE, but I will let it continue to
	//"support" MLE
	private void reestimateAllMixturesWithMMIE (Instances insts) throws Exception {

		//initialize some variables

		int nnumberOfClasses = insts.numClasses();
		//true when algorithm converged
		int nnumberOfRepeatedSmallImprovements = 0;

		int niterations = 1;

		double[] dlogProbabilityOfEachModel = new double[nnumberOfClasses];
		double[] dtotalProbabilities = new double[2];
		int ntotalNumberOfInstances = insts.numInstances();
		double dtotalLikelihood;
		double dlogObjectiveFunction;
		double dtotalDenominator;
		double dlogProbPerPattern;
		double dlogOldProbability = LogDomainCalculator.m_fLOG_DOMAIN_ZERO;
		double dbestProbPerPattern = Double.NEGATIVE_INFINITY;
		int niterationOfBestProbPerPattern = -1;

		//main reestimation loop: reestimates simultaneously all mixtures
		do {
			//reset counts
			for (int i = 0; i < nnumberOfClasses; i++) {
				m_mixturesOfGaussianPDFsBeingReestimated[i].zeroAccumulators();
			}
			//before starting, create generator model with current models
			if (m_ouseMMIE) {
				createGeneratorModelForMMIE();
				m_generatorModelForMMIEBeingReestimated.zeroAccumulators();
	//			m_generatorModelForMMIE = new MixtureOfGaussianPDFs(m_generatorModelForMMIEBeingReestimated.getGaussians(),
	//			m_generatorModelForMMIEBeingReestimated.getComponentsWeightsInLogDomain());
	//
	//			if (!m_generatorModelForMMIE.isMixtureOk()) {
	//				System.err.println(m_generatorModelForMMIE.toString());
	//				throw new Exception("Mixture is not OK!");
	//			}
			}

			computeStatistics(insts, dlogProbabilityOfEachModel, dtotalProbabilities);

			dtotalLikelihood = dtotalProbabilities[0];
			if (m_ouseMMIE) {
				dtotalDenominator = dtotalProbabilities[1];
				dlogObjectiveFunction = dtotalLikelihood - dtotalDenominator;
			} else {
				//MLE
				dlogObjectiveFunction = dtotalLikelihood;
			}
			//normalize
			dlogProbPerPattern = dlogObjectiveFunction / ntotalNumberOfInstances;
			double dlogImprovement = dlogProbPerPattern - dlogOldProbability;
			if (m_odebug) {
				if (m_ouseMMIE) {
					System.out.println("MMIE) It. " + niterations + " | posterior = " + IO.format(dlogProbPerPattern) +
					" | likelihood = " + IO.format(dtotalLikelihood / ntotalNumberOfInstances) +
					". Convergence = " + IO.format(dlogImprovement));
				} else {
					System.out.println("MLE)  It. " + niterations + " | total likelihood log p(x|y) / N = " + IO.format(dlogProbPerPattern) +
					". Convergence = " + IO.format(dlogImprovement));
				}
			}

			if (m_ouseMMIE && dlogProbPerPattern > dbestProbPerPattern) {
				niterationOfBestProbPerPattern = niterations;
				dbestProbPerPattern = dlogProbPerPattern;
				//should keep the best mixtures
				keepEstimatedMixtures();
			}

			if (Math.abs(dlogImprovement) < m_fconvergenceThreshold) {
				nnumberOfRepeatedSmallImprovements++;
				if (m_odebug && m_ouseMMIE) {
					System.out.println("Repeated small improvements = " + nnumberOfRepeatedSmallImprovements +
					" / " + m_maximumNumberOfRepeatedSmallImprovementsForMMIE);
				}
			} else {
				nnumberOfRepeatedSmallImprovements = 0;
			}

			//stopping criteria
			if (niterations >= m_nmaximumIterations) {
				//stop procedure
				if (m_odebug) {
					Print.dialog("Aborted. Reached maximum number of iterations = " + niterations);
				}
				break;
			}
			if (m_ouseMMIE &&
			((niterations-niterationOfBestProbPerPattern) >= m_nmaximumNumberOfIterationsWithoutSignificantImprovementForMMIE )) {
				if (m_odebug) {
					Print.dialog("Aborted. Reached maximum # of iterations without improving best result = " + m_nmaximumNumberOfIterationsWithoutSignificantImprovementForMMIE);
				}
				break;
			}
			if ( (m_ouseMMIE && (nnumberOfRepeatedSmallImprovements == m_maximumNumberOfRepeatedSmallImprovementsForMMIE))
			|| ( !m_ouseMMIE && (nnumberOfRepeatedSmallImprovements == 1)) ) {
				if (m_odebug) {
					Print.dialog("Reestimation converged. Total number of iterations = " + niterations);
				}
				break;
			}

			//update for next iteration
			dlogOldProbability = dlogProbPerPattern;
			niterations++;
			reestimateNewGaussians();
		} while (true);
		if (m_ouseMMIE && m_odebug) {
			System.out.println("Best posterior probability  = " +
			IO.format(dbestProbPerPattern) + " in iteration = " + niterationOfBestProbPerPattern);
		}
	} //end of method

	private void computeStatistics(Instances insts, double[] dlogProbabilityOfEachModel, double[] dtotalProbabilities) {
		int ntotalNumberOfInstances = insts.numInstances();
		//p(y|x) = p(x|y)p(y) / p(x)  is here p(y|x) = likelihood p(y) / denominator
		double dtotalLikelihood = 0;
		double dtotalDenominator = 0;
		for (int i = 0; i < dlogProbabilityOfEachModel.length; i++) {
			dlogProbabilityOfEachModel[i] = 0;
		}

		//go over all training set
		//the problem here is that I keep reestimating even if one model requires
		//more iterations than the other... That's not the best for MLE
		//should use an array of booleans here and individually stop reestimating models...
		for (int npatternNumber = 0; npatternNumber < ntotalNumberOfInstances; npatternNumber++) {
				Instance inst = insts.instance(npatternNumber);
				float[] fobservation = inst.getAttributesDiscardingLastOne();
				double dinstanceLogWeight = LogDomainCalculator.calculateLog(inst.weight());
				int nclassValue = (int) inst.classValue();

				//System.out.print(nclassValue + " ");
				MixtureOfGaussianPDFsBeingReestimated mixtureOfGaussianPDFsBeingReestimated = m_mixturesOfGaussianPDFsBeingReestimated[nclassValue];

				//update counts related to this class
				float flogProbabilityOfThisPattern = newUpdatePDFAccumulators(fobservation,
				dinstanceLogWeight, mixtureOfGaussianPDFsBeingReestimated);
				dlogProbabilityOfEachModel[nclassValue] += flogProbabilityOfThisPattern;
				dtotalLikelihood += flogProbabilityOfThisPattern;

				if (m_ouseMMIE) {
					//update counters
					float ftemp = newUpdatePDFAccumulators(fobservation, dinstanceLogWeight, m_generatorModelForMMIEBeingReestimated);
					dtotalDenominator += ftemp;
				}
				//showAccumulators();
			}
			dtotalProbabilities[0] = dtotalLikelihood;
			if (m_ouseMMIE) {
				dtotalProbabilities[1] = dtotalDenominator;
		}
	}

	private void reestimateNewGaussians() {
		int nnumberOfClasses = m_mixturesOfGaussianPDFsBeingReestimated.length;
			if (m_ouseMMIE) {
				PDFBeingReestimated[] generatorModelGaussians = m_generatorModelForMMIEBeingReestimated.getReferenceToPDFsBeingReestimated();
				float[] generatorWeightsOccupationCounts = m_generatorModelForMMIEBeingReestimated.getMixtureWeightsOccupationCounts();
				//update models. Don't need to update MMIE generator model
				for (int i = 0; i < nnumberOfClasses; i++) {
					//find correspondent Gaussians in generator model
					int nnumberOfGaussians = m_indexInGeneratorModel[i].length;
					PDFBeingReestimated[] generatorModelGaussiansForThisClass = new PDFBeingReestimated[nnumberOfGaussians];
					float[] generatorWeightsOccupationCountsForThisClass = new float[nnumberOfGaussians];
					//System.err.print("Class = " + i + ", gaussian indices =");
					for (int j = 0; j < nnumberOfGaussians; j++) {
						int nindice = m_indexInGeneratorModel[i][j];
						//System.err.print(" " + nindice);
						generatorModelGaussiansForThisClass[j] = generatorModelGaussians[nindice];
						generatorWeightsOccupationCountsForThisClass[j] = generatorWeightsOccupationCounts[nindice];
					}
					//System.err.print("\n");
					m_mixturesOfGaussianPDFsBeingReestimated[i].reestimateMixtureUsingMMIE(1, m_oshouldUpdateMean,
							m_oshouldUpdateCovariance, m_oshouldUpdateWeights, m_fcovarianceFloor,
							m_fmixtureWeightFloor, generatorModelGaussiansForThisClass, m_fMMIEConstantD,
							generatorWeightsOccupationCountsForThisClass);
				}
			} else {
				//use MLE formulae
				for (int i = 0; i < nnumberOfClasses; i++) {
					m_mixturesOfGaussianPDFsBeingReestimated[i].reestimateMixture(1, m_oshouldUpdateMean,
							m_oshouldUpdateCovariance, m_oshouldUpdateWeights, m_fcovarianceFloor, m_fmixtureWeightFloor);
				}
			}
	}

	public void setConvergenceThreshold (float t) {
		m_fconvergenceThreshold = t;
	}

	public float getConvergenceThreshold () {
		return m_fconvergenceThreshold;
	}

	public void setMaximumIterations (int nmaximumIterations) {
		m_nmaximumIterations = nmaximumIterations;
	}

	public int getMaximumIterations () {
		return m_nmaximumIterations;
	}

	public void setMinimumConstantDForMMIE (float d) {
		m_fMMIEConstantD = d;
	}

	public float getMinimumConstantDForMMIE () {
		return m_fMMIEConstantD;
	}

	public void setNumberOfGaussians (int ngaussians) {
		m_numberOfGaussians = ngaussians;
	}

	public int getNumberOfGaussians () {
		return  m_numberOfGaussians;
	}

	public void setUsePriors(boolean ousePriors) {
		m_oshouldUsePriors = ousePriors;
	}

	public boolean getUsePriors() {
		return m_oshouldUsePriors;
	}

	public void setMixtureWeightFloor(float fmixtureWeightFloor) {
		m_fmixtureWeightFloor = fmixtureWeightFloor;
	}

	public float getMixtureWeightFloor() {
		return m_fmixtureWeightFloor;
	}

	public void setCovarianceFloor(float fcovarianceFloor) {
		m_fcovarianceFloor = fcovarianceFloor;
	}

	public float getCovarianceFloor() {
		return m_fcovarianceFloor;
	}

	public void setWriteEMReport(boolean oshouldWriteReport) {
		m_oshouldWriteReportFile = oshouldWriteReport;
	}

	public boolean getWriteEMReport() {
		return m_oshouldWriteReportFile;
	}

	public void setOptions (String[] options) throws Exception {
		setDebug(Utils.getFlag('V', options));
		String ngaussians = Utils.getOption('I', options);
		if (ngaussians.length() != 0) {
			setNumberOfGaussians(Integer.parseInt(ngaussians));
		}	else {
			setNumberOfGaussians(1);
		}
		String nmaxit = Utils.getOption('N', options);
		if (nmaxit.length() != 0) {
			setMaximumIterations(Integer.parseInt(nmaxit));
		}	else {
			setMaximumIterations(20);
		}
		String convergence = Utils.getOption('S', options);
		if (convergence.length() != 0) {
			setConvergenceThreshold(Float.parseFloat(convergence));
		}	else {
			setConvergenceThreshold(1e-4F);
		}
		String usePriors = Utils.getOption('P', options);
		if (usePriors.equals("true")) {
			m_oshouldUsePriors = true;
		} else {
			m_oshouldUsePriors = false;
		}
		String report = Utils.getOption('R', options);
		if (report.length() != 0) {
			 m_oshouldWriteReportFile = Boolean.valueOf(report).booleanValue();
		}
		String mixFloor = Utils.getOption('M', options);
		if (mixFloor.length() != 0) {
			setMixtureWeightFloor(Float.parseFloat(mixFloor));
		}
		String covFloor = Utils.getOption('C', options);
		if (covFloor.length() != 0) {
			 setCovarianceFloor(Float.parseFloat(covFloor));
		}
		m_ouseMixUpSplittingInsteadOfKMeans = Utils.getFlag('K', options);
		m_ouseMMIE = Utils.getFlag('D', options);
		String d = Utils.getOption('Q', options);
		if (d.length() != 0) {
			if (!m_ouseMMIE) {
				throw new Exception("Cannot use option -Q without selecting MMIE");
			}
			setMinimumConstantDForMMIE(Float.parseFloat(d));
		}	else {
			setMinimumConstantDForMMIE(200);
		}
	}

	public Enumeration listOptions () {
		Vector newVector = new Vector(9);
		newVector.addElement(new Option("\tNumber of Gaussians (default 1).", "I",
				1, "-I <num of Gaussians>"));
		newVector.addElement(new Option("\tMaximum number of iterations (default 20).", "N",
				1, "-N <max num of iterations>"));
		newVector.addElement(new Option("\tUse priors (default false).", "P",
				1, "-P <true | false>"));
		newVector.addElement(new Option("\tMixture weight floor (eliminate components" +
		" with weight below this value, default 0).", "M",
				1, "-M <component weight floor>"));
		newVector.addElement(new Option("\tFloor value for elements of covariance matrix" +
		"(default 1e-4).", "C",
				1, "-C <covariance floor>"));
		newVector.addElement(new Option("\tWrite report of EM convergence (default false).", "R",
				1, "-R <true | false>"));
		newVector.addElement(new Option("\tUse Gaussian splitting to get final number (option -I) of Gaussians "+
		" instead of using K-means to find the initial set of Gaussians (default false).", "K",
				0, "-K"));
		newVector.addElement(new Option("\tUse discriminative MMIE training instead of EM (default false).", "D",
				0, "-D"));
		newVector.addElement(new Option("\tMinimum value for constant D in MMIE (default 200).", "Q",
				1, "-Q <minimum D>"));
		newVector.addElement(new Option(
				"\tTurn on debugging output.",
				"V", 0, "-V"));
		newVector.addElement(new Option("\tConvergence threshold (default 1e-4F).", "S",
				1, "-S <float>"));
		return  newVector.elements();
	}

	public String[] getOptions () {
		String[] options = new String[30];
		int current = 0;
		options[current++] = "-I";
		options[current++] = "" + getNumberOfGaussians();
		options[current++] = "-N";
		options[current++] = "" + getMaximumIterations();
		options[current++] = "-P";
		options[current++] = "" + getUsePriors();
		options[current++] = "-M";
		options[current++] = "" + getMixtureWeightFloor();
		options[current++] = "-C";
		options[current++] = "" + getCovarianceFloor();
		options[current++] = "-R";
		options[current++] = "" + getWriteEMReport();
		options[current++] = "-S";
		options[current++] = "" + getConvergenceThreshold();
		if (getUseMixUpSplittingInsteadOfKMeans()) {
			options[current++] = "-K";
		}
		if (getUseDiscriminativeMMIETraining()) {
			options[current++] = "-D";
			options[current++] = "-Q";
			options[current++] = "" + getMinimumConstantDForMMIE();
		}
		if (getDebug()) {
			options[current++] = "-V";
		}
		while (current < options.length) {
			options[current++] = "";
		}
		return  options;
	}

	public void setUseDiscriminativeMMIETraining(boolean ouseMMIE) {
		m_ouseMMIE = ouseMMIE;
	}

	public boolean getUseDiscriminativeMMIETraining() {
		return m_ouseMMIE;
	}

	public void setUseMixUpSplittingInsteadOfKMeans(boolean ouseMixUpSplittingInsteadOfKMeans) {
		m_ouseMixUpSplittingInsteadOfKMeans = ouseMixUpSplittingInsteadOfKMeans;
	}

	public boolean getUseMixUpSplittingInsteadOfKMeans() {
		return m_ouseMixUpSplittingInsteadOfKMeans;
	}

	private void createGeneratorModelForMMIE() {
		int M = m_mixturesOfGaussianPDFsBeingReestimated.length;
		m_indexInGeneratorModel = new int[M][];
		//find total number of Gaussians in all mixtures and maximum number per mixture
		int ntotalGaussians = 0;
		for (int i = 0; i < M; i++) {
			int g = m_mixturesOfGaussianPDFsBeingReestimated[i].getNumberOfGaussians();
			ntotalGaussians += g;
			//allocate space to store the index in generator model for the Gaussians in this mixture
			m_indexInGeneratorModel[i] = new int[g];
		}
		GaussianPDF[] generatorGaussians = new GaussianPDF[ntotalGaussians];
		float[] fgeneratorWeights = new float[ntotalGaussians];
		int ncurrentGaussianInGeneratorModel = 0;
		for (int i = 0; i < M; i++) {
			float fprior = m_fpriors[i];
			GaussianPDF[] gaussians = m_mixturesOfGaussianPDFsBeingReestimated[i].getGaussians();
			float[] fweights = m_mixturesOfGaussianPDFsBeingReestimated[i].getComponentsWeights();
			int g = m_mixturesOfGaussianPDFsBeingReestimated[i].getNumberOfGaussians();
			for (int j = 0; j < g; j++) {
				fgeneratorWeights[ncurrentGaussianInGeneratorModel] = fprior * fweights[j];
				generatorGaussians[ncurrentGaussianInGeneratorModel] = gaussians[j];
				m_indexInGeneratorModel[i][j] = ncurrentGaussianInGeneratorModel;
				ncurrentGaussianInGeneratorModel++;
			}
		}
//		m_generatorModelForMMIE = new MixtureOfGaussianPDFs(generatorGaussians,
//		fgeneratorWeights, true);
//		m_generatorModelForMMIEBeingReestimated = new MixtureOfGaussianPDFsBeingReestimated(m_generatorModelForMMIE);
			m_generatorModelForMMIEBeingReestimated = new MixtureOfGaussianPDFsBeingReestimated(new MixtureOfGaussianPDFs(generatorGaussians,
			fgeneratorWeights, true));
	}

	/**
	 * Sets debugging mode
	 *
	 * @param debug true if debug output should be printed
	 */
	public void setDebug(boolean debug) {

		m_odebug = debug;
	}

	/**
	 * Gets whether debugging is turned on
	 *
	 * @return true if debugging output is on
	 */
	public boolean getDebug() {

		return m_odebug;
	}

	/**
	 * Main method.
	 *
	 * @param args the options for the classifier
	 */
	public static void main (String[] args) {
		try {
			System.out.println(Evaluation.evaluateModel(new OldGaussiansMixture(), args));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

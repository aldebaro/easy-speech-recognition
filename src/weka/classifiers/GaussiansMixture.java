package  weka.classifiers;

import  java.io.*;
import  java.util.*;
import  java.text.SimpleDateFormat;
import  weka.core.*;
import  edu.ucsd.asr.*;

/*
 *    GaussiansMixture.java
 * @author Aldebaro Klautau
 * @version $Revision: 2 $
 */

 //to save memory:
 //I will try to do the whole training using m_mixturesOfGaussianPDFsBeingReestimated
 //and after training, I nullify the Instances data and then I create
 //m_mixturesOfGaussianPDFs for using in classification.

 //testing with the following command line
 //java weka.classifiers.GaussiansMixture -t vowel_train.arff -T vowel_test.arff -I 2 > t
 //java weka.classifiers.GaussiansMixture -t vowel_train.arff -T vowel_test.arff -I 2 -D -Q 4000 -N 100 -V -o 2> t
 //java weka.classifiers.GaussiansMixture -t tr.arff -T te.arff -I 20 -D -Q 999999 -N 100 -V -o

 //I am not supporting the interface WeightedInstancesHandler,
 //because in some methods I take the weight in account and in others I don't...

//interesting result: 19 Gaussians placed in ER and 24.3% of error for m_dpercentageForMMIESplitting = 0.2
//G:\newallpairs>java weka.classifiers.GaussiansMixture -t g:\newallpairs\tr.arff
//-T g:\newallpairs\te.arff -K -D -Q 500 -N 10000 -I 0 -V -L 0

//allow for different covariance matrices
public class GaussiansMixture extends DistributionClassifier
		implements RawScorer, OptionHandler {

	public static final long serialVersionUID = 715933919145242235L;
	public static final int m_maximumNumberOfRepeatedSmallImprovementsForMMIE = 3;
	public static final int m_nmaximumNumberOfIterationsWithoutSignificantImprovementForMMIE = 10;
	public static final int m_nmaximumNumberOfIterationsWhenUsingMMIEWithAutomaticSplitting = 4;
	public static final int m_nmaximumAverageNumberOfGaussiansPerMixtureForMMIE_BIC = 20;
	public static final double m_dminimumMultiplicationFactorForD = 1.2;
	public static final int INDIVIDUAL_DIAGONAL = 1;
	public static final int SHAREDBYALLCLASSES_FULL = 1;

	private MixtureOfGaussianPDFs[] m_mixturesOfGaussianPDFs;
	private MixtureOfGaussianPDFsBeingReestimated[] m_mixturesOfGaussianPDFsBeingReestimated;
	private float[] m_fpriors;
	private String[] m_classNames;
	//private int m_nnumberOfGaussiansDiscarded;
	private float m_flogOutputProbability;
	private float[] m_fauxiliaryZeroMean;

	//constant that should be added to numerator to make the posterior sum up to one
	private double m_dlogPriorContribution;

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

	private boolean m_ouseMixUpSplittingInsteadOfKMeans = true;
	private boolean m_ouseMMIE = false;
	private MixtureOfGaussianPDFs m_generatorModelForMMIE;
	private MixtureOfGaussianPDFsBeingReestimated m_generatorModelForMMIEBeingReestimated;
	private int[][] m_indexInGeneratorModel;
	private double m_dlambdaForBICAndMMIE = 1;
	private float m_fMMIEConstantD = -1;
	public static double m_dpercentageForMMIESplitting = 0.2;

	private MixtureOfGaussianPDFsBeingReestimated[] m_bestMixturesOfGaussianPDFsBeingReestimated;
	private MixtureOfGaussianPDFsBeingReestimated m_bestGeneratorModelForMMIEBeingReestimated;

	private MixtureOfGaussianPDFsBeingReestimated[] m_2bestMixturesOfGaussianPDFsBeingReestimated;
	private MixtureOfGaussianPDFsBeingReestimated m_2bestGeneratorModelForMMIEBeingReestimated;

	private boolean m_oautomaticallyTuneMultiplicationFactorForD = false;
	private String m_validationFileNameForMMIE = null;

	private FilterForClassifier m_filterForClassifier = new FilterForClassifier();
	private boolean m_ouseSomeFilter = true;

	static {
		//MatlabInterfacer.sendCommand("x=[];");
	}

	/** Class attribute of dataset. */
	//private Attribute m_ClassAttribute;
	//keep for compatibility with SetOfMatrixEncodedHMMs
	public void setMixtures (MixtureOfGaussianPDFs[] mixtures) {
		m_mixturesOfGaussianPDFs = mixtures;
		//there is no information about priors in this case
		m_oshouldUsePriors = false;
	}

	/**
	 * Use MLE (EM) or MMIE to estimate one mixture per class.
	 *
	 * @param data the training data
	 * @exception Exception if classifier can't be built successfully
	 */
	public void buildClassifier (Instances inputData) throws Exception {
		//data = new Instances(data);
		//data.deleteWithMissingClass();
		if (inputData.checkForStringAttributes()) {
			throw new Exception("Can't handle string attributes!");
		}
//    if (data.numClasses() > 2) {
//      throw new Exception("Can only handle two-class datasets!");
//    }
		if (inputData.classAttribute().isNumeric()) {
			throw new Exception("GaussiansMixture can't handle a numeric class!");
		}
		//assume last attribute is the class
		if (inputData.classIndex() != (inputData.numAttributes() - 1)) {
			throw new Exception("Class must be last attribute for GaussiansMixture!");
		}
		// Check if no instances are available
		if (inputData.numInstances() == 0) {
			m_mixturesOfGaussianPDFs = null;
			m_mixturesOfGaussianPDFsBeingReestimated = null;
			m_fpriors = null;
			return;
		}
		if (inputData.sumOfWeights() == 0.0) {
			throw new Exception("All weights are 0 !");
			//data.setAllWeights(1.0);
		}

		//setup filters and filter if needed.
		//If it does filter, it takes a copy to avoid messing up with this Instances
		//ak: need to setOptions for filter:
		m_filterForClassifier.m_Normalize = false;
		Instances data = m_filterForClassifier.setFiltering(inputData);
		m_ouseSomeFilter = m_filterForClassifier.getUseSomeFilter();

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

		//don't let the number of Gaussians be greater than the minimum number of examples per class
		int nminimumNumberOfExamplePerClass = (int) m_fpriors[Utils.minIndex(m_fpriors)];
		if (m_numberOfGaussians > nminimumNumberOfExamplePerClass) {
			m_numberOfGaussians = nminimumNumberOfExamplePerClass;
			if (m_odebug) {
				System.err.println("Warning: using only " + m_numberOfGaussians + " Gaussians due to few small datasets.");
			}
		}

		//Calculate priors. Avoid giving 0 probability to any class
		for (int i = 0; i < m_fpriors.length; i++) {
			if (m_fpriors[i] == 0) {
				m_fpriors[i] = 1;
				lsum++;
			}
		}

		//calculate the prior contribution
		m_dlogPriorContribution = 0;
		for (int i = 0; i < m_fpriors.length; i++) {
			m_dlogPriorContribution += m_fpriors[i] * Math.log(m_fpriors[i]);
		}
		m_dlogPriorContribution -= lsum * Math.log(lsum);

		for (int i = 0; i < m_fpriors.length; i++) {
			m_fpriors[i] /= lsum;
		}


		//initialize m_mixturesOfGaussianPDFsBeingReestimated with final number
		//of Gaussians specified by user
		if (m_ouseMMIE && m_numberOfGaussians == 0) {
			//implement Normandin's method
			createMixturesUsingMMIECounts(data);
		} else {
			//initialize with one of the 2 MLE methods
			if (m_ouseMixUpSplittingInsteadOfKMeans) {
				createMixturesInitializedByUpMix(data);
			} else {
				createMixturesInitializedByKMeans(data);
			}
		}

		if (m_ouseMMIE) {
			//if (m_numberOfGaussians != 0) {
				//if used automatic method, don't need to reestimate
				//if (m_numberOfGaussians != 0) {
					//don't use the MLE as starting point for MMIE anymore
					//if (m_odebug) {
					//	System.err.println("First estimating with MLE:");
					//}
					//reestimateAllMixturesWithMLE(data);
				//}
				if (m_odebug) {
					System.err.println("Now, estimating with MMIE:");
				}
				reestimateAllMixturesWithMMIE(data);
				if (m_mixturesOfGaussianPDFs == null) {
					System.err.println("WARNING: MMIE did not go through...");
					reestimateAllMixturesWithMLE(data);
				}
			//}
		} else {
			//MLE
			//only reestimate if initialized with up-mix
			if (m_ouseMixUpSplittingInsteadOfKMeans) {
			   reestimateAllMixturesWithMLE(data);
			}
			//in the case of MMIE, this method is called by reestimateAllMixtures
			copyBeingEstimatedToFinalGaussians();
		}
		//don't need it, save RAM
		data = null;

		discardGaussiansWithSmallWeight();

		//don't need it, save RAM
		m_mixturesOfGaussianPDFsBeingReestimated = null;
		//invite garbage collection
		System.gc();

		for (int i = 0; i < m_mixturesOfGaussianPDFs.length; i++) {
			if (!m_mixturesOfGaussianPDFs[i].isMixtureOk()) {
				//use 1 G/mix
				System.err.println("Problem in final Gaussians: I'm backing-off to use 1 Gaussian / mixture.");
				initializeWith1GaussianPerMixture(data);
				copyBeingEstimatedToFinalGaussians();
				break;
			}
		}

	}

	private void copyBeingEstimatedToFinalGaussians() {
		//here I create m_mixturesOfGaussianPDFs that will be used for testing
		int nnumberOfClasses = m_mixturesOfGaussianPDFsBeingReestimated.length;
		m_mixturesOfGaussianPDFs = new MixtureOfGaussianPDFs[nnumberOfClasses];
		for (int i = 0; i < m_mixturesOfGaussianPDFs.length; i++) {
			m_mixturesOfGaussianPDFs[i] = m_mixturesOfGaussianPDFsBeingReestimated[i].getMixtureOfGaussianPDFs();
		}
	}

	private void keepBestBeingEstimatedGaussians() {
		//here I create m_mixturesOfGaussianPDFs that will be used for testing
		int nnumberOfClasses = m_mixturesOfGaussianPDFsBeingReestimated.length;
		m_bestMixturesOfGaussianPDFsBeingReestimated = new MixtureOfGaussianPDFsBeingReestimated[nnumberOfClasses];
		for (int i = 0; i < nnumberOfClasses; i++) {
			//need to clone?
			m_bestMixturesOfGaussianPDFsBeingReestimated[i] = (MixtureOfGaussianPDFsBeingReestimated) m_mixturesOfGaussianPDFsBeingReestimated[i].clone();
		}
		m_bestGeneratorModelForMMIEBeingReestimated = (MixtureOfGaussianPDFsBeingReestimated) m_generatorModelForMMIEBeingReestimated.clone();
	}

	//need this to keep best during MMIE splitting
	private void keep2BestBeingEstimatedGaussians() {
		//here I create m_mixturesOfGaussianPDFs that will be used for testing
		int nnumberOfClasses = m_mixturesOfGaussianPDFsBeingReestimated.length;
		m_2bestMixturesOfGaussianPDFsBeingReestimated = new MixtureOfGaussianPDFsBeingReestimated[nnumberOfClasses];
		for (int i = 0; i < nnumberOfClasses; i++) {
			//need to clone?
			m_2bestMixturesOfGaussianPDFsBeingReestimated[i] = (MixtureOfGaussianPDFsBeingReestimated) m_mixturesOfGaussianPDFsBeingReestimated[i].clone();
		}
		m_2bestGeneratorModelForMMIEBeingReestimated = (MixtureOfGaussianPDFsBeingReestimated) m_generatorModelForMMIEBeingReestimated.clone();
	}

	private int getFinalNumberOfGaussians() {
		int nfinalNumberOfGaussians = 0;
		int nnumberOfClasses = m_mixturesOfGaussianPDFs.length;
		for (int i = 0; i < nnumberOfClasses; i++) {
			nfinalNumberOfGaussians += m_mixturesOfGaussianPDFs[i].getNumberOfGaussians();
		}
		return nfinalNumberOfGaussians;
	}

	private int getFinalNumberOfGaussiansBeingReestimated() {
		int nfinalNumberOfGaussians = 0;
		int nnumberOfClasses = m_mixturesOfGaussianPDFsBeingReestimated.length;
		for (int i = 0; i < nnumberOfClasses; i++) {
			nfinalNumberOfGaussians += m_mixturesOfGaussianPDFsBeingReestimated[i].getNumberOfGaussians();
		}
		return nfinalNumberOfGaussians;
	}

	private void discardGaussiansWithSmallWeight() {
		int nnumberOfClasses = m_mixturesOfGaussianPDFsBeingReestimated.length;
		for (int i = 0; i < nnumberOfClasses; i++) {
			try {
				m_mixturesOfGaussianPDFs[i].discardGaussianWithVerySmallWeights(LogDomainCalculator.calculateLog(this.getMixtureWeightFloor()));
			} catch (ASRError e) {
				//System.out.println("3333### " + e.getAuxiliaryValue());
				//do nothing. I using a more general method for counting
				//that takes in account that the initialization may had
				//returned fewer Gaussians than specified
				//m_nnumberOfGaussiansDiscarded += e.getAuxiliaryValue();
			}
		}
	}

	//assume binary problem
	public double getRawScore(Instance instance) {
		//double[] x = distributionForInstance(instance);
                double[] x = getLogProbabilities(instance);
		return x[0] - x[1];
	}

	public double[] getLogProbabilities(Instance instance) {
		try {
			instance = m_filterForClassifier.filterIfNeeded (instance);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		//should get a reference from instance and change methods
		//calculateLogProbabilityAsDouble to go over dimension - 1.
		//As this method is used only during test, I'm going to leave it as it is.
		float[] x = instance.getAttributesDiscardingLastOne();
		double[] logprob = new double[m_mixturesOfGaussianPDFs.length];
		for (int i = 0; i < logprob.length; i++) {
			if (m_mixturesOfGaussianPDFs[i] != null) {
				logprob[i] = m_mixturesOfGaussianPDFs[i].calculateLogProbabilityAsDouble(x);
			} else {
				logprob[i] = LogDomainCalculator.m_fLOG_DOMAIN_ZERO;
			}
		}
		return logprob;
	}

	/**
	 * Computes class distribution for instance using mixtures.
	 *
	 * @param instance the instance for which distribution is to be computed
	 * @return the class distribution for the given instance
	 */
	public double[] distributionForInstance (Instance instance) {
		double[] dprobability = getLogProbabilities(instance);

		//IO.DisplayVector(dprobability);

		//I have to avoid underflow and overflow when calculating Math.exp for
		//dprobability elements, which can be as low as -15000 (e.g., for isolet).
		//Note that Math.exp(-708)=0, and, for integer x > 709, Math.exp(x) = Infinity.
		//I'm going to normalize this posterior distribution p(y|x), so I can
		//add any number in the log domain to log(x|y) because it will cancel.
		//I should add a number to make -708 to be the minimum value of dprobability,
		//but I've to guarantee that the maximum scaled value will not exceed 709,
		//so I have a dynamic range of ~ 1400 in log domain.
		//I will assume that it's more important to keep the
		//ratios among the classes that have high probability, and
		//discard the classes with low probability if necessary. In other words,
		//I am going to shift the log probabilities such that the maximum value
		//is around 700.

		double dmaxLog = dprobability[Utils.maxIndex(dprobability)];
		//double dminLog = dprobability[Utils.minIndex(dprobability)];

		double dmaximumShiftThatAvoidsSaturation = 700 - dmaxLog;
		for (int i = 0; i < dprobability.length; i++) {
			dprobability[i] += dmaximumShiftThatAvoidsSaturation;
			if (dprobability[i] > LogDomainCalculator.m_fMINIMUM_EXP_ARGUMENT) {
				dprobability[i] = Math.exp(dprobability[i]);
			} else {
				dprobability[i] = 0;
			}
		}

		double dsum = 0;
		if (m_oshouldUsePriors) {
			for (int i = 0; i < dprobability.length; i++) {
				dprobability[i] *= m_fpriors[i];
				dsum += dprobability[i];
			}
		} else {
			for (int i = 0; i < dprobability.length; i++) {
				dsum += dprobability[i];
			}
		}
		if (dsum <= 0) {
			End.throwError("Sum is = "+ dsum + ". This should never happen given" +
			" that I am adding a constant in log domain...");
		}
		for (int i = 0; i < dprobability.length; i++) {
			dprobability[i] /= dsum;
		}

		//IO.DisplayVector(dprobability);
		//System.out.println(" s = " + dmaximumShiftThatAvoidsSaturation + "\n");

		return  dprobability;
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
			stringBuffer.append(m_mixturesOfGaussianPDFs[i].toString(LogDomainCalculator.calculateLog(this.getMixtureWeightFloor())));
		}
		stringBuffer.append("\n\nNumber of Gaussians per class:");
		for (int i = 0; i < m_mixturesOfGaussianPDFs.length; i++) {
			stringBuffer.append(" " + m_mixturesOfGaussianPDFs[i].getNumberOfGaussians());
		}
		stringBuffer.append("\nFinal number of Gaussians = " + getFinalNumberOfGaussians() + "\n");
		return  stringBuffer.toString();
	}

	private double updatePDFAccumulators (Instance inst, int nclassValue) throws Exception {
		MixtureOfGaussianPDFsBeingReestimated mixtureOfGaussianPDFsBeingReestimated = m_mixturesOfGaussianPDFsBeingReestimated[nclassValue];
		float[] fobservation = inst.getAttributesDiscardingLastOne();
		//I'm not going to use the Instances weights for now
		//double dinstanceLogWeight = LogDomainCalculator.calculateLog(inst.weight());
		//return newUpdatePDFAccumulators(fobservation, dinstanceLogWeight, mixtureOfGaussianPDFsBeingReestimated);
		return newUpdatePDFAccumulators(fobservation, mixtureOfGaussianPDFsBeingReestimated);
	}

	//private double newUpdatePDFAccumulators(float[] fobservation, double dinstanceLogWeight,
	private double newUpdatePDFAccumulators(float[] fobservation,
	MixtureOfGaussianPDFsBeingReestimated mixtureOfGaussianPDFsBeingReestimated) {
		//not optimum: the probabilities for the correct class were already calculated
		//and the calculation is repeated when doing it for the generator model.
		//That's not a big problem if the number of classes >> 2
		double[] dindividualAndTotalProbabilities = mixtureOfGaussianPDFsBeingReestimated.calculateIndividualProbabilitiesAsDouble(fobservation);

		//System.out.println("###########");
		//IO.DisplayVector(fobservation);
		//IO.DisplayVector(findividualAndTotalProbabilities);

		//total mixture probability is last element of findividualAndTotalProbabilities
		double dlogOutputProbability = dindividualAndTotalProbabilities[dindividualAndTotalProbabilities.length-1];
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
					//Lr = dinstanceLogWeight;
					Lr = 0;
				}
				else {
					Lr = dindividualAndTotalProbabilities[m] + flogWeights[m] - dlogOutputProbability;
					//Lr = dinstanceLogWeight + dindividualAndTotalProbabilities[m] + flogWeights[m] - dlogOutputProbability;
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
//				System.out.println("Skipped " + m);
//			}
		}
		return dlogOutputProbability;
	}

	private void copyBestGaussiansToGaussiansBeingReestimated() {
		m_mixturesOfGaussianPDFsBeingReestimated = new MixtureOfGaussianPDFsBeingReestimated[m_bestMixturesOfGaussianPDFsBeingReestimated.length];
		for (int i = 0; i < m_bestMixturesOfGaussianPDFsBeingReestimated.length; i++) {
			m_mixturesOfGaussianPDFsBeingReestimated[i] = (MixtureOfGaussianPDFsBeingReestimated) m_bestMixturesOfGaussianPDFsBeingReestimated[i].clone();
		}
		m_generatorModelForMMIEBeingReestimated = (MixtureOfGaussianPDFsBeingReestimated) m_bestGeneratorModelForMMIEBeingReestimated.clone();
	}

	private void createMixturesUsingMMIECounts(Instances insts) throws Exception {

		Instances testInstances = null;
		if (m_validationFileNameForMMIE != null) {
			testInstances = new Instances(m_validationFileNameForMMIE);
		}

		//limit the number of iterations when splitting Gaussians
		int noriginalMaxNumberOfIterations = getMaximumIterations();
		setMaximumIterations(m_nmaximumNumberOfIterationsWhenUsingMMIEWithAutomaticSplitting);

		//for starting, I will run only several epochs and split 1 G, the one with maximum weight
		//run one (or several) epoch(s) of MMIE and accumulate counts
		int N = insts.numInstances();
		int nnumberOfClasses = insts.numClasses();
		initializeWith1GaussianPerMixture(insts);
		reestimateAllMixturesWithMMIE(insts);
		if (m_validationFileNameForMMIE != null) {
			Evaluation evaluation = new Evaluation(testInstances);
			evaluation.evaluateModel(this, testInstances);
			System.out.println("Error rate = " + IO.format(100 * evaluation.errorRate()));
		}

		copyBestGaussiansToGaussiansBeingReestimated();
		//after that, I have the counts
		double[] dlogProbPerModel = new double[m_mixturesOfGaussianPDFsBeingReestimated.length];
		double[] dtotalLogProb = new double[2];
		computeStatistics(insts, dlogProbPerModel, dtotalLogProb);
		double dpreviousLogObjectiveFunction = dtotalLogProb[0] - dtotalLogProb[1];
		double dlogN = Math.log(N);
		dpreviousLogObjectiveFunction -= 0.5 * m_dlambdaForBICAndMMIE * countNumberOfParametersInMixture(m_generatorModelForMMIEBeingReestimated) * dlogN;
			if (m_odebug) {
				System.out.println("L = " + (dtotalLogProb[0]/N) + " logP(y|x) = " + (dpreviousLogObjectiveFunction/N));
				System.out.println("#### MMIE | Number of Gaussians: " + getFinalNumberOfGaussiansBeingReestimated());
			}
		keep2BestBeingEstimatedGaussians();
		do {
			float[][] fweightsDifference = evaluateMMIECountsForSplitting();
			//find maximum
			double dmax = Double.NEGATIVE_INFINITY;
			for (int i = 0; i < fweightsDifference.length; i++) {
				for (int j = 0; j < fweightsDifference[i].length; j++) {
					if (fweightsDifference[i][j] > dmax) {
						dmax = fweightsDifference[i][j];
					}
				}
			}
			double dthreshold = m_dpercentageForMMIESplitting * dmax;
			for (int i = 0; i < fweightsDifference.length; i++) {
				for (int j = 0; j < fweightsDifference[i].length; j++) {
					if (fweightsDifference[i][j] > dthreshold) {
						//split this Gaussian
						m_mixturesOfGaussianPDFsBeingReestimated[i].splitGivenGaussian(j);
					}
				}
			}
			if (getFinalNumberOfGaussiansBeingReestimated() > (nnumberOfClasses * m_nmaximumAverageNumberOfGaussiansPerMixtureForMMIE_BIC)) {
				System.out.println("Aborting because new total number of Gaussians = " +
				getFinalNumberOfGaussiansBeingReestimated() + " exceeds " + nnumberOfClasses +
				" * " + m_nmaximumAverageNumberOfGaussiansPerMixtureForMMIE_BIC);
				break;
			}

			//optimization is "easy" in the beginning, so set a small value
			setMultiplicationFactorForD((float) m_dminimumMultiplicationFactorForD);
			reestimateAllMixturesWithMMIE(insts);

			copyBestGaussiansToGaussiansBeingReestimated();
			computeStatistics(insts, dlogProbPerModel, dtotalLogProb);
			double dlogObjectiveFunction = dtotalLogProb[0] - dtotalLogProb[1];

			dlogObjectiveFunction -= 0.5 * m_dlambdaForBICAndMMIE * countNumberOfParametersInMixture(m_generatorModelForMMIEBeingReestimated) * dlogN;

			double dimprovement = dlogObjectiveFunction - dpreviousLogObjectiveFunction;

			if (m_odebug) {
				//System.out.println("L = " + (dtotalLogProb[0]/N) + " P(y|x) = " + (dlogObjectiveFunction/N));
				System.out.println("#### Improvement = " + dimprovement + " | number of Gaussians: " + getFinalNumberOfGaussiansBeingReestimated());
			}
			if (dimprovement < m_fconvergenceThreshold) {
				break;
			}
			//for next iteration
			keep2BestBeingEstimatedGaussians();
			dpreviousLogObjectiveFunction = dlogObjectiveFunction;

			if (m_validationFileNameForMMIE != null) {
				Evaluation evaluation = new Evaluation(testInstances);
				evaluation.evaluateModel(this, testInstances);
				System.out.println("Error rate = " + IO.format(100 * evaluation.errorRate()));
			}

		} while (true);

		//recover the best
		for (int i = 0; i < m_mixturesOfGaussianPDFsBeingReestimated.length; i++) {
			m_mixturesOfGaussianPDFsBeingReestimated[i] = m_2bestMixturesOfGaussianPDFsBeingReestimated[i];
		}
		m_generatorModelForMMIEBeingReestimated = m_2bestGeneratorModelForMMIEBeingReestimated;

		//don't need it anymore
		m_2bestGeneratorModelForMMIEBeingReestimated = null;
		m_2bestMixturesOfGaussianPDFsBeingReestimated = null;

		//restore original number
		setMaximumIterations(noriginalMaxNumberOfIterations);
	}

	//for testing:
	private void createMixturesUsingMMIECounts2(Instances insts) throws Exception {

		//for starting, I will run only several epochs and split 1 G, the one with maximum weight
		//run one (or several) epoch(s) of MMIE and accumulate counts
		int N = insts.numInstances();

		initializeWith1GaussianPerMixture(insts);
		reestimateAllMixturesWithMMIE(insts);
		copyBestGaussiansToGaussiansBeingReestimated();
		//after that, I have the counts
		double[] dlogProbPerModel = new double[m_mixturesOfGaussianPDFsBeingReestimated.length];
		double[] dtotalLogProb = new double[2];
		computeStatistics(insts, dlogProbPerModel, dtotalLogProb);
		double dlogObjectiveFunction = dtotalLogProb[0] - dtotalLogProb[1];
		System.out.println("L = " + (dtotalLogProb[0]/N) + " P(y|x) = " + (dlogObjectiveFunction/N));

		evaluateMMIECountsForSplitting();

		System.out.println("\n");

		//for (int i = 0; i < m_mixturesOfGaussianPDFsBeingReestimated.length; i++) {
		//	doubleNumberOfGaussians(i);
		//}

		//doubleNumberOfGaussians(1);
		doubleNumberOfGaussians(2);

		reestimateAllMixturesWithMMIE(insts);
		copyBestGaussiansToGaussiansBeingReestimated();
		computeStatistics(insts, dlogProbPerModel, dtotalLogProb);
		double dimprovement = dlogObjectiveFunction;
		dlogObjectiveFunction = dtotalLogProb[0] - dtotalLogProb[1];
		dimprovement = dlogObjectiveFunction - dimprovement;
		System.out.println("L = " + (dtotalLogProb[0]/N) + " P(y|x) = " + (dlogObjectiveFunction/N));
		System.out.println("Improvement = " + dimprovement);

		//after that, I have the counts
		evaluateMMIECountsForSplitting();

		doubleNumberOfGaussians(4);
		//doubleNumberOfGaussians(8);
		reestimateAllMixturesWithMMIE(insts);
		copyBestGaussiansToGaussiansBeingReestimated();
		computeStatistics(insts, dlogProbPerModel, dtotalLogProb);
		dimprovement = dlogObjectiveFunction;
		dlogObjectiveFunction = dtotalLogProb[0] - dtotalLogProb[1];
		dimprovement = dlogObjectiveFunction - dimprovement;
		System.out.println("L = " + (dtotalLogProb[0]/N) + " P(y|x) = " + (dlogObjectiveFunction/N));
		System.out.println("Improvement = " + dimprovement);

		//after that, I have the counts
		evaluateMMIECountsForSplitting();

		System.exit(1);

		//search the highest counts and split some of them
		//here, I shouldn't reestimate the one that was split
		//repeat loop
	}

	/**
	 * Initialize all mixtures using segmental K-means.
	 */
	 //the mixtures may end up having a number of Gaussians
	 //smaller than the specified. That happens when there
	 //are less examples than the specified # of Gaussians
	 //per mixture, for example.
	private void createMixturesInitializedByKMeans (Instances insts) throws Exception {
		for (int i = 0; i < m_mixturesOfGaussianPDFsBeingReestimated.length; i++) {
			m_mixturesOfGaussianPDFsBeingReestimated[i] = createMixtureInitializedByKMeans(insts, i, m_numberOfGaussians);
		}
	}

	private MixtureOfGaussianPDFsBeingReestimated createMixtureInitializedByKMeans (Instances insts,
	int nclass, int numberOfGaussians) throws Exception {
			int nnumberOfClasses = insts.classAttribute().numValues();
			int nnumberOfStatesForHMMWith1EmittingState = 3;
			double dclassValue = nclass;
			SetOfPatterns setOfPatterns = WekaInterfacer.instancesToSetOfPatterns(insts,
					dclassValue);
			//get initial prototype
			ContinuousHMM continuousHMM = HMMInitializer.createHMMWith1State(nnumberOfClasses);
			//Use segmental K-means to obtain given number of mixtures
			HMMInitializer hMMInitializer = new HMMInitializer(setOfPatterns,
			nnumberOfStatesForHMMWith1EmittingState, numberOfGaussians,
					0.005, "kmeans.log", 1);
			continuousHMM = hMMInitializer.getHMMUsingViterbiAlignmentAndKMeans();
			MixtureOfGaussianPDFs[] mixs = continuousHMM.getMixturesOfGaussianPDFs();
			if (mixs.length != 1) {
				End.throwError("mixs.length = " + mixs.length + " should be 1 !");
			}
			return new MixtureOfGaussianPDFsBeingReestimated (mixs[0]);
	}

	//returns number of instances in each class
	private long[] initializeWith1GaussianPerMixture(Instances insts) throws Exception {
		int nnumberOfClasses = insts.classAttribute().numValues();
		//we assume last attribute is the class as checked by buildClassifier()
		int nspaceDimension = insts.numAttributes() - 1;
		float[][] fmeans = new float[nnumberOfClasses][nspaceDimension];
		float[][] fvariances = new float[nnumberOfClasses][nspaceDimension];
		int nnumberOfInstances = insts.numInstances();
		long[] loccurences = new long[nnumberOfClasses];
		for (int i = 0; i < nnumberOfInstances; i++) {
			Instance instance = insts.instance(i);
			float[] x = instance.getAttributesReference();
			int nclass = (int) instance.classValue();
			float[] thisClassMean = fmeans[nclass];
			float[] thisClassVar = fvariances[nclass];
			loccurences[nclass] ++;
			for (int j = 0; j < nspaceDimension; j++) {
				thisClassMean[j] += x[j];
				thisClassVar[j] += x[j] * x[j];
			}
		}

		//find variance of whole training set
		//to do


		for (int i = 0; i < nnumberOfClasses; i++) {
			float fnormalizationFactor = 1.0F / loccurences[i];
			for (int j = 0; j < nspaceDimension; j++) {
				fmeans[i][j] *= fnormalizationFactor;
				fvariances[i][j] *= fnormalizationFactor;
				//VAR = E[X^2] - E[X]^2
				fvariances[i][j] -= fmeans[i][j]*fmeans[i][j];
				if (fvariances[i][j] < m_fcovarianceFloor) {
					fvariances[i][j] = m_fcovarianceFloor;
				}
			}
			DiagonalCovarianceGaussianPDF pdf = new DiagonalCovarianceGaussianPDF(fmeans[i], fvariances[i]);
			DiagonalCovarianceGaussianPDF[] diagonalCovarianceGaussianPDFs = new DiagonalCovarianceGaussianPDF[1];
			diagonalCovarianceGaussianPDFs[0] = pdf;
			float[] fweights = new float[1];
			//0 because a weight = 1 in log domain
			fweights[0] = 0;
			MixtureOfGaussianPDFs mixtureOfGaussianPDFs = new MixtureOfGaussianPDFs(diagonalCovarianceGaussianPDFs, fweights);
			m_mixturesOfGaussianPDFsBeingReestimated[i] = new MixtureOfGaussianPDFsBeingReestimated (mixtureOfGaussianPDFs);
		}
		return loccurences;
	}

	//start with 1 Gaussian per mixture and increment until desired final number
	private void createMixturesInitializedByUpMix (Instances insts) throws Exception {
		if (m_numberOfGaussians == 0) {
			 createMixturesInitializedByBIC (insts);
			 return;
		}
		initializeWith1GaussianPerMixture(insts);
		if (m_numberOfGaussians == 1) {
			return;
			//simply return because the caller method will reestimate Gaussians
		}
		//do up-mix
		int nnumberOfClasses = m_mixturesOfGaussianPDFsBeingReestimated.length;
		//m_numberOfGaussians is the desired number of Gaussians specified by the user
		for (int j = 2; j <= m_numberOfGaussians; j++) {
			for (int i = 0; i < nnumberOfClasses; i++) {
				m_mixturesOfGaussianPDFsBeingReestimated[i].splitGaussianWithLargestWeight();
				reestimateMixtures(insts, m_nmaximumIterations, m_fconvergenceThreshold, m_fcovarianceFloor, i);
			}
		}
	}

	//use the Bayesian information criterion (BIC) to choose number of Gaussians
	private void createMixturesInitializedByBIC (Instances insts) throws Exception {
	//private void createMixturesInitializedByUpMix (Instances insts) throws Exception {
		long[] loccurences = initializeWith1GaussianPerMixture(insts);
		int nnumberOfClasses = m_mixturesOfGaussianPDFsBeingReestimated.length;
		//get likelihoods when using only 1 Gaussian
		double[] dinitialLogProbabilityOfEachModel = new double[nnumberOfClasses];
		double[] dtemp = new double[2];
		//computeStatisticsAndUpdateAccumulator(insts, dinitialLogProbabilityOfEachModel, dtemp);
		computeStatistics(insts, dinitialLogProbabilityOfEachModel, dtemp);
		//don't need it
		dtemp = null;
		//I'm not sure I should initialize
		//for (int i = 0; i < nnumberOfClasses; i++) {
			//normalize it
			//dinitialLogProbabilityOfEachModel[i] /= loccurences[i];
		//}
		boolean oshouldTurnDebugOn = false;
		if (m_odebug) {
			oshouldTurnDebugOn = true;
			System.out.println("Initializing by incrementing Gaussians until BIC decreases");
			//turn of debug, otherwise it's too much printed information
			m_odebug = false;
		}
		for (int i = 0; i < nnumberOfClasses; i++) {
			if (oshouldTurnDebugOn) {
				System.out.println("Class = " + i);
			}
			double dlogN = Math.log(loccurences[i]);
			//go multiplying # of Gaussians by 2, here I may need a method that splits all Gaussians
			//in a mixture, or I can reuse the splitGaussianWithLargestWeight
			//reestimate
			//get the likelihood and compute BIC criterion
			//when decreased, use bisection method
			double dpreviousLogLikelihood = dinitialLogProbabilityOfEachModel[i];
			double dpreviousBIC = dpreviousLogLikelihood - 0.5 * m_dlambdaForBICAndMMIE * countNumberOfParametersInMixture(m_mixturesOfGaussianPDFsBeingReestimated[i]) * dlogN;
			//System.out.println("dtotalLogLikelihood = " + dpreviousLogLikelihood + " " + (0.5 * m_dlambdaForBICAndMMIE * countNumberOfParametersInMixture(i) * dlogN));
			if (oshouldTurnDebugOn) {
				System.out.println("likelihood = " + dpreviousLogLikelihood + " | BIC = " + dpreviousBIC);
			}
			MixtureOfGaussianPDFsBeingReestimated bestMixturesOfGaussianPDFsBeingReestimated = null;
			do {
				//the best is in previous iteration
				bestMixturesOfGaussianPDFsBeingReestimated = new MixtureOfGaussianPDFsBeingReestimated(m_mixturesOfGaussianPDFsBeingReestimated[i].getMixtureOfGaussianPDFs());

				//doubleNumberOfGaussians(i);
				int ninitialNumberOfGaussians = m_mixturesOfGaussianPDFsBeingReestimated[i].getNumberOfGaussians();

				//ak testL I'm getting bad results with BIC for e-set: 33% while K-means with 5 G gets 22%
				//so I will try to use K-means inside BIC
				if (true) {
					//up-mix
					increaseNumberOfGaussians(i, ninitialNumberOfGaussians + 1);
				} else {
					//use K-means. It gave better results for e-set
					m_mixturesOfGaussianPDFsBeingReestimated[i] = createMixtureInitializedByKMeans(insts, i, ninitialNumberOfGaussians + 1);
				}

				double dtotalLogLikelihood = reestimateMixtures(insts, m_nmaximumIterations, m_fconvergenceThreshold, m_fcovarianceFloor, i);
				//I'm not sure I should initialize
				//dtotalLogLikelihood /= loccurences[i];

				//Eq. (1) in paper by Chen & Gopinath, IBM.
				double dbic = dtotalLogLikelihood - 0.5 * m_dlambdaForBICAndMMIE * countNumberOfParametersInMixture(m_mixturesOfGaussianPDFsBeingReestimated[i]) * dlogN;
				//System.out.println("dtotalLogLikelihood = " + dtotalLogLikelihood + " " + (0.5 * m_dlambdaForBICAndMMIE * countNumberOfParametersInMixture(i) * dlogN));
				//int G = m_mixturesOfGaussianPDFsBeingReestimated[i].getNumberOfGaussians();
				//System.out.println(i + " " + G + " " + (dbic-dpreviousBIC) + " " +
				//(dtotalLogLikelihood - dpreviousLogLikelihood));
				//update for next iteration
				if (oshouldTurnDebugOn) {
					System.out.println("likelihood = " + dtotalLogLikelihood + " | BIC = " + dbic);
				}
				if (dbic - dpreviousBIC <= 0) {
					break;
				}
				dpreviousBIC = dbic;
				dpreviousLogLikelihood = dtotalLogLikelihood;
			//} while (m_mixturesOfGaussianPDFsBeingReestimated[i].getNumberOfGaussians() < 256);
			} while (true);
			//keep best model
			m_mixturesOfGaussianPDFsBeingReestimated[i] = bestMixturesOfGaussianPDFsBeingReestimated;
			//System.out.println(m_mixturesOfGaussianPDFsBeingReestimated[i].getNumberOfGaussians() + "\n\n");
		}
		//restore previous status of debug flag
		if (oshouldTurnDebugOn) {
			m_odebug = true;
		}
	}

	//assume a diagonal covariance matrix
	private int countNumberOfParametersInMixture(MixtureOfGaussianPDFsBeingReestimated mixtureOfGaussianPDFsBeingReestimated) {
		int ninitialNumberOfGaussians = mixtureOfGaussianPDFsBeingReestimated.getNumberOfGaussians();
		int nspaceDimension = mixtureOfGaussianPDFsBeingReestimated.getSpaceDimension();
		return ninitialNumberOfGaussians * (2 * nspaceDimension + 1);
	}

	private void doubleNumberOfGaussians(int nclass) {
		int ninitialNumberOfGaussians = m_mixturesOfGaussianPDFsBeingReestimated[nclass].getNumberOfGaussians();
		int nfinalNumberOfGaussians = 2 * ninitialNumberOfGaussians;
		increaseNumberOfGaussians(nclass, nfinalNumberOfGaussians);
	}

	private void increaseNumberOfGaussians(int nclass, int nfinalNumberOfGaussians) {
		int ninitialNumberOfGaussians = m_mixturesOfGaussianPDFsBeingReestimated[nclass].getNumberOfGaussians();
		if (nfinalNumberOfGaussians <= ninitialNumberOfGaussians) {
			End.throwError("nfinalNumberOfGaussians <= ninitialNumberOfGaussians" +
			nfinalNumberOfGaussians + " <= " + ninitialNumberOfGaussians);
		}
		for (int i = ninitialNumberOfGaussians; i < nfinalNumberOfGaussians; i++) {
			m_mixturesOfGaussianPDFsBeingReestimated[nclass].splitGaussianWithLargestWeight();
		}
	}

	//this is the original, which reestimate the mixtures of 1 class each time,
	//while I am writing a method that does only 1 pass over the training set
	private double reestimateMixtures (Instances insts, int nmaximumIterations, float fconvergenceThreshold,
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
		double dlogProbabilityOfAllPatterns = Double.NaN;
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
			dlogProbabilityOfAllPatterns = 0;                //1 in log domain
			//float flogNewProbability = 0.0F;
			for (int npatternNumber = 0; npatternNumber < ntotalNumberOfPatterns; npatternNumber++) {
				int nnumberOfPatternsSkipped = 0;
				//update current pattern
				Instance inst = insts.instance(npatternNumber);
				int nclassValue = (int) inst.classValue();
				if (nclassValue == nclass) {
					//update counters
					double dlogProbabilityOfThisPattern = updatePDFAccumulators(inst, nclass);
					dlogProbabilityOfAllPatterns += dlogProbabilityOfThisPattern;
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
			float flogProbPerPattern = (float) dlogProbabilityOfAllPatterns/nnumberOfValidPatterns;
			float flogImprovement = flogProbPerPattern - flogOldProbability;
			if (m_oshouldWriteReportFile || m_odebug) {
				int ltotalNumberOfFramesInValidPatterns = nnumberOfValidPatterns;
				double dlogProbPerFrame = dlogProbabilityOfAllPatterns/ltotalNumberOfFramesInValidPatterns;
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
		return dlogProbabilityOfAllPatterns;
	}             //end of method


	private void reestimateAllMixturesWithMLE (Instances insts) throws Exception {
			int nnumberOfClasses = insts.numClasses();
			for (int i = 0; i < nnumberOfClasses; i++) {
				reestimateMixtures(insts, m_nmaximumIterations, m_fconvergenceThreshold,
				m_fcovarianceFloor, i);
			}
	}

	//this method is now used only by MMIE, but I will let it continue to
	//"support" MLE
	//the best reestimated Gaussians are in m_mixturesOfGaussianPDFs
	private void reestimateAllMixturesWithMMIE (Instances insts) throws Exception {

		//MatlabInterfacer.sendCommand("x=[]");

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

			computeStatisticsAndUpdateAccumulator(insts, dlogProbabilityOfEachModel, dtotalProbabilities);

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

				//System.out.println(niterations + " | logprob " + (ntotalNumberOfInstances*dbestProbPerPattern));

				keepBestBeingEstimatedGaussians();
				copyBeingEstimatedToFinalGaussians();
			}

			if (Math.abs(dlogImprovement) < m_fconvergenceThreshold) {
				nnumberOfRepeatedSmallImprovements++;
				if (m_odebug && m_ouseMMIE) {
					System.out.println("Repeated small improvements = " + nnumberOfRepeatedSmallImprovements +
					" / " + m_maximumNumberOfRepeatedSmallImprovementsForMMIE);
				}
				if (dlogImprovement < 0) {
					changeMultiplicationFactorForD(1.2F);
				}
			} else {
				nnumberOfRepeatedSmallImprovements = 0;
				if (m_oautomaticallyTuneMultiplicationFactorForD) {
					if (dlogImprovement > 0) {
						changeMultiplicationFactorForD(0.9F);
						if (getMultiplicationFactorForD() < m_dminimumMultiplicationFactorForD) {
							//minimum allowed value is m_dminimumMultiplicationFactorForD
							setMultiplicationFactorForD((float) m_dminimumMultiplicationFactorForD);
						}
					} else {
						//duplicate its value
						changeMultiplicationFactorForD(2F);
					}
				}
			}
			if (m_odebug) {
				System.out.println("Factor for D = " + getMultiplicationFactorForD());
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

			//reestimate using accumulated counts
			//if (dlogImprovement > 0) {
				reestimateNewGaussians();
			//}


			//MatlabInterfacer.sendCommand("x=[x " + dlogProbPerPattern + "];");

		} while (true);
		if (m_ouseMMIE && m_odebug) {
			System.out.println("Best posterior probability  = " +
			IO.format(dbestProbPerPattern) + " in iteration = " + niterationOfBestProbPerPattern);
		}


		//IO.pause();

	} //end of method

	//dtotalProbabilities returns both likelihood and posterior for all data
 //dtotalProbabilities[0] = dtotalLikelihood and dtotalProbabilities[1] = dtotalDenominator

	private void computeStatisticsAndUpdateAccumulator(Instances insts, double[] dlogProbabilityOfEachModel, double[] dtotalProbabilities) {
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
				//double dinstanceLogWeight = LogDomainCalculator.calculateLog(inst.weight());
				int nclassValue = (int) inst.classValue();

				//System.out.print(nclassValue + " ");
				MixtureOfGaussianPDFsBeingReestimated mixtureOfGaussianPDFsBeingReestimated = m_mixturesOfGaussianPDFsBeingReestimated[nclassValue];

				//update counts related to this class
				double dlogProbabilityOfThisPattern = newUpdatePDFAccumulators(fobservation,
				mixtureOfGaussianPDFsBeingReestimated);
				//dinstanceLogWeight, mixtureOfGaussianPDFsBeingReestimated);
				dlogProbabilityOfEachModel[nclassValue] += dlogProbabilityOfThisPattern;
				dtotalLikelihood += dlogProbabilityOfThisPattern;

				if (m_ouseMMIE) {
					//update counters
					//double dtemp = newUpdatePDFAccumulators(fobservation, dinstanceLogWeight, m_generatorModelForMMIEBeingReestimated);
					double dtemp = newUpdatePDFAccumulators(fobservation, m_generatorModelForMMIEBeingReestimated);
					dtotalDenominator += dtemp;
				}
				//showAccumulators();
			}
			dtotalProbabilities[0] = dtotalLikelihood + m_dlogPriorContribution;
			if (m_ouseMMIE) {
				dtotalProbabilities[1] = dtotalDenominator;
		}
	}

	//dtotalProbabilities returns both likelihood and posterior for all data
	//this method doesn't update statistics
	private void computeStatistics(Instances insts, double[] dlogProbabilityOfEachModel, double[] dtotalProbabilities) {
		int ntotalNumberOfInstances = insts.numInstances();
		//p(y|x) = p(x|y)p(y) / p(x)  is here p(y|x) = likelihood p(y) / denominator
		double dtotalLikelihood = 0;
		double dtotalDenominator = 0;
		for (int i = 0; i < dlogProbabilityOfEachModel.length; i++) {
			dlogProbabilityOfEachModel[i] = 0;
		}

		for (int npatternNumber = 0; npatternNumber < ntotalNumberOfInstances; npatternNumber++) {
				Instance inst = insts.instance(npatternNumber);
				float[] fobservation = inst.getAttributesDiscardingLastOne();
				int nclassValue = (int) inst.classValue();

				//System.out.print(nclassValue + " ");
				MixtureOfGaussianPDFsBeingReestimated mixtureOfGaussianPDFsBeingReestimated = m_mixturesOfGaussianPDFsBeingReestimated[nclassValue];

				double dlogProbabilityOfThisPattern = mixtureOfGaussianPDFsBeingReestimated.calculateLogProbabilityAsDouble(fobservation);
				dlogProbabilityOfEachModel[nclassValue] += dlogProbabilityOfThisPattern;
				dtotalLikelihood += dlogProbabilityOfThisPattern;

				if (m_ouseMMIE) {
					double dtemp = m_generatorModelForMMIEBeingReestimated.calculateLogProbabilityAsDouble(fobservation);
					dtotalDenominator += dtemp;
				}
				//showAccumulators();
			}
			dtotalProbabilities[0] = dtotalLikelihood + m_dlogPriorContribution;
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

	//returns the weight counts
	private float[][] evaluateMMIECountsForSplitting() {
		int nnumberOfClasses = m_mixturesOfGaussianPDFsBeingReestimated.length;
				PDFBeingReestimated[] generatorModelGaussians = m_generatorModelForMMIEBeingReestimated.getReferenceToPDFsBeingReestimated();
				float[] generatorWeightsOccupationCounts = m_generatorModelForMMIEBeingReestimated.getMixtureWeightsOccupationCounts();

				float[][] fweightsDifference = new float[nnumberOfClasses][];

				//update models. Don't need to update MMIE generator model
				for (int i = 0; i < nnumberOfClasses; i++) {
					//find correspondent Gaussians in generator model
					int nnumberOfGaussians = m_indexInGeneratorModel[i].length;
					PDFBeingReestimated[] generatorModelGaussiansForThisClass = new PDFBeingReestimated[nnumberOfGaussians];
					float[] generatorWeightsOccupationCountsForThisClass = new float[nnumberOfGaussians];
					//System.err.print("Class = " + i + ", gaussian indices =");

					float[] foccupationCounts = m_mixturesOfGaussianPDFsBeingReestimated[i].getMixtureWeightsOccupationCounts();

					fweightsDifference[i] = new float[nnumberOfGaussians];
					for (int j = 0; j < nnumberOfGaussians; j++) {
						int nindice = m_indexInGeneratorModel[i][j];
						//System.err.print(" " + nindice);
						generatorModelGaussiansForThisClass[j] = generatorModelGaussians[nindice];
						generatorWeightsOccupationCountsForThisClass[j] = generatorWeightsOccupationCounts[nindice];

						fweightsDifference[i][j] = foccupationCounts[j] - generatorWeightsOccupationCountsForThisClass[j];

						//System.out.println(foccupationCounts[j] + " " + generatorWeightsOccupationCountsForThisClass[j] +
						//" " + IO.format(foccupationCounts[j] - generatorWeightsOccupationCountsForThisClass[j]));
					}
					//System.err.print("\n");
					//m_mixturesOfGaussianPDFsBeingReestimated[i].reestimateMixtureUsingMMIE(1, m_oshouldUpdateMean,
					//		m_oshouldUpdateCovariance, m_oshouldUpdateWeights, m_fcovarianceFloor,
					//		m_fmixtureWeightFloor, generatorModelGaussiansForThisClass, m_fMMIEConstantD,
					//		generatorWeightsOccupationCountsForThisClass);
				}
		return fweightsDifference;
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

	public void setPercentageForMMIESplitting(double d) {
		m_dpercentageForMMIESplitting = d;
	}

	public double getPercentageForMMIESplitting() {
		return m_dpercentageForMMIESplitting;
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
		//setUseSameVariance(Utils.getFlag('B', options));
		String ngaussians = Utils.getOption('I', options);
		if (ngaussians.length() != 0) {
			setNumberOfGaussians(Integer.parseInt(ngaussians));
		}	else {
			setNumberOfGaussians(1);
		}
		String lambda = Utils.getOption('L', options);
		if (lambda.length() != 0) {
			if (getNumberOfGaussians() == 0) {
				//user selected BIC or MMIE splitting
				setLambdaForBICAndMMIE(Double.parseDouble(lambda));
			} else {
				throw new Exception("Option -L can be used only with -I 0");
			}
		} else {
			if (getNumberOfGaussians() == 0) {
				//user selected BIC or MMIE splitting
				setLambdaForBICAndMMIE(1);
			}
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
		m_ouseMixUpSplittingInsteadOfKMeans = !Utils.getFlag('K', options);
//		if (!m_ouseMixUpSplittingInsteadOfKMeans && getUseSameVariance()) {
//			throw new Exception("Cannot use options -K and -U");
//		}

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
		String perc = Utils.getOption('U', options);
		if (perc.length() != 0) {
			if (!m_ouseMMIE || getNumberOfGaussians()!=0) {
				throw new Exception("Cannot use option -U without selecting MMIE (-D) with automatic splitting (-I 0)");
			}
			setPercentageForMMIESplitting(Double.parseDouble(perc));
		}

		setAutomaticallyTuneMultiplicationFactorForD(Utils.getFlag('A', options));
		if (getAutomaticallyTuneMultiplicationFactorForD() && !m_ouseMMIE) {
			throw new Exception("Cannot use option -A without selecting MMIE (-D)");
		}

		String factor = Utils.getOption('Z', options);
		if (factor.length() != 0) {
			if (!m_ouseMMIE) {
				throw new Exception("Cannot use option -Z without selecting MMIE (-D)");
			}
			if (getAutomaticallyTuneMultiplicationFactorForD()) {
				throw new Exception("Cannot use both options -A and -Z");
			}
			setMultiplicationFactorForD(Float.parseFloat(factor));
		}
		String validationFile = Utils.getOption('F', options);
		if (validationFile.length() != 0) {
			if (!m_ouseMMIE) {
				throw new Exception("Cannot use option -F without selecting MMIE (-D)");
			}
			m_validationFileNameForMMIE = validationFile.trim();
		}
	}

	public Enumeration listOptions () {
		Vector newVector = new Vector(9);
		newVector.addElement(new Option("\tNumber of Gaussians (default 1). If 0, " +
		"each class will have the number of Gaussians selected by the BIC or MMIE criteria (depending is MLE or MMIE)", "I",
				1, "-I <num of Gaussians>"));
		newVector.addElement(new Option("\tLambda for BIC and MMIE splitting (default 1).", "L",
				1, "-L <real number>"));
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
		newVector.addElement(new Option("\tUse K-means to find a initial set with the final number (option -I) of Gaussians "+
		" instead of splitting and reestimating (default false).", "K",
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
		newVector.addElement(new Option("\tPercentage for MMIE splitting (default 0.2).", "U",
				1, "-U <float>"));
		newVector.addElement(new Option("\tFactor that multiplies the count to provide a lower bound for MMIE constant D (default 2).", "Z",
				1, "-Z <float>"));
		newVector.addElement(new Option("\tAutomatically tune the factor that multiplies the count to provide a lower bound for MMIE constant D (default false).", "A",
				0, "-A"));
		newVector.addElement(new Option("\tIf true, don't update the variance (maybe use same variance for all classes later on) (default false).", "B",
				0, "-B"));
		newVector.addElement(new Option("\tValidation file name for MMIE with automatic splitting.", "F",
				1, "-F"));
		return  newVector.elements();
	}

	public String[] getOptions () {
		String[] options = new String[30];
		int current = 0;
		options[current++] = "-I";
		options[current++] = "" + getNumberOfGaussians();
		if (getNumberOfGaussians() == 0) {
			options[current++] = "-L";
			options[current++] = "" + getLambdaForBICAndMMIE();
			if (m_ouseMMIE) {
				options[current++] = "-U";
				options[current++] = "" + getPercentageForMMIESplitting();
			}
		}
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
//		if (getUseSameVariance()) {
//			options[current++] = "-B";
//		}
		if (!getUseMixUpSplittingInsteadOfKMeans()) {
			options[current++] = "-K";
		}
		if (getUseDiscriminativeMMIETraining()) {
			options[current++] = "-D";
			options[current++] = "-Q";
			options[current++] = "" + getMinimumConstantDForMMIE();
			if (getAutomaticallyTuneMultiplicationFactorForD()) {
				options[current++] = "-A";
			} else {
				options[current++] = "-Z";
				options[current++] = "" + getMultiplicationFactorForD();
			}
			if (m_validationFileNameForMMIE != null) {
				options[current++] = "-F";
				options[current++] = m_validationFileNameForMMIE;
			}
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

	public void setCovarianceType(int covarianceType) {
		if (covarianceType == 1 || covarianceType == 2) {
			//ak change this to use only one boolean
			m_oshouldUpdateCovariance = false;
		}
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

	public void setLambdaForBICAndMMIE(double dlambda) {
		m_dlambdaForBICAndMMIE = dlambda;
	}

	public double getLambdaForBICAndMMIE() {
		return m_dlambdaForBICAndMMIE;
	}

	public void setMultiplicationFactorForD(float fmultiplicationFactorForD) {
		DiagonalCovarianceGaussianPDFBeingReestimated.setMultiplicationFactorForD(fmultiplicationFactorForD);
	}

	public float getMultiplicationFactorForD() {
		return DiagonalCovarianceGaussianPDFBeingReestimated.getMultiplicationFactorForD();
	}

	public void changeMultiplicationFactorForD(float percentage) {
		float currentValue = getMultiplicationFactorForD();
		setMultiplicationFactorForD(percentage * currentValue);
	}

	public void setAutomaticallyTuneMultiplicationFactorForD(boolean oautomaticallyTuneMultiplicationFactorForD) {
		m_oautomaticallyTuneMultiplicationFactorForD = oautomaticallyTuneMultiplicationFactorForD;
	}

	public boolean getAutomaticallyTuneMultiplicationFactorForD() {
		return m_oautomaticallyTuneMultiplicationFactorForD;
	}

	public MixtureOfGaussianPDFs[] getMixturesReference() {
		return m_mixturesOfGaussianPDFs;
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
			System.out.println(Evaluation.evaluateModel(new GaussiansMixture(), args));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

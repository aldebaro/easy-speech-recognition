package weka.classifiers;

import java.io.*;
import weka.core.*;
import weka.filters.*;
import edu.ucsd.asr.*;
import java.util.*;

/**
 * Title:        Spock
 * Description:  Speech recognition
 * Copyright:    Copyright (c) 2001
 * Company:      UCSD
 * @author       Aldebaro Klautau
 * @version 4.0
 */


//it doesn't give exactly the same result as ScoreMultiClassClassifier + SMO when
//I use the cache due to differences in details of calculation

//it also doesn't bring the same result for soybean. I guess it's because the
//filter for missing values is global here, but different for each binary
//classifier in ScoreMultiClassClassifier

//If I normalize using whole set, performance decreases (only for vowel?)
//If I try to normalize each binary classifier independently, I cannot use
//common cache.

//java weka.classifiers.KernelClassifier -t vowel_train.arff -T vowel_test.arff -W weka.classifiers.IVM -G -V -K -- -K 2 -G 0.125 -I 50
public class KernelClassifier extends ScoreMultiClassClassifier {

  //default value for option -X
	private static int m_nnumberOfFoldsForCrossValidationWhenFindingSVMParameters = 10;

	//if I specify CV (-K) with 1 fold (-X 1), then 1 fold of size below is used
	//private static final int m_nnumberOfFoldsToSubstitute1 = 6;
	private static final int m_nmaximumNumOfExamplesForCVTrainingWithOneFold = 5000;
	private static final int m_nmaximumNumOfExamplesForCVTestWithOneFold = 5000;

	protected boolean m_odoGlobalNormalization = false;
	protected boolean m_ouseSharedCache = false;
	protected boolean m_oisLinearMachine = false;
	protected boolean m_oareThereMissingAttributes = true;
	//protected boolean m_ouseSVMTorch = false;
	protected boolean m_osharedCacheWasFilled = false;
	protected boolean m_ofindSVMParametersThroughCrossValidation = false;

	//all distinct support vectors
	protected float[][] m_funiqueSupportVectors;
	protected double[] m_dkernelValuesCache;
	protected Filter m_globalNormalizationFilter;
	protected Instances m_instancesHeader;
	//protected String m_outputModelFileName;

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

	/** Indicates if we need to do any sort of filtering (if not, we can save computations) */
	protected boolean m_ouseSomeFilter = true;

	/** Only numeric attributes in the dataset? */
	protected boolean m_onlyNumeric;

	protected boolean m_ouseAttributeSelection;
	private String m_evaluationClassOptions;
	private String m_searchClassOptions;
	private int[] m_nselectedAttributes;
	private int[] m_natributeOccurrences;

	//keep going in a direction until you get no decrease for three steps in a row
	private static final int m_nnumberOfAttemptsToGetADecreaseInError = 3;

	/**
	 * Builds the classifiers.
	 *
	 * @param insts the training data.
	 * @exception Exception if a classifier can't be built
	 */
	public void buildClassifier (Instances inputData) throws Exception {
		if (m_Classifier == null) {
			throw new Exception("No base classifier has been set!");
		}

		if (! (m_Classifier instanceof BinaryKernelClassifier) ) {
			throw new Exception("Base classifier must be a subclass of BinaryKernelClassifier! It was set to " + m_Classifier.getClass());
		}

		if (inputData.classIndex() != inputData.numAttributes()-1) {
			throw new Exception("Class must be last attribute!");
		}

		//get configuration information from SMO
		BinaryKernelClassifier smoWithConfiguration = (BinaryKernelClassifier) m_Classifier;
		m_Normalize = smoWithConfiguration.getNormalizeData();
		m_ouseOriginalNormalization = smoWithConfiguration.getUseOriginalNormalization();

		//assume machine is never linear (for a linear machine, use SVM)
		m_oisLinearMachine = false;
		//if (smoWithConfiguration.getExponent() == 1.0 && smoWithConfiguration.getKernelFunctionAsInteger() != SMO.RBF_KERNEL) {
		//	m_oisLinearMachine = true;
		//}

		if (m_odoGlobalNormalization && m_Normalize && m_odebug) {
			//how can I get the option for mean 0, var 1 or [0, 1] with error below
			//throw new Exception("Option -G (global normalization) must be used with SMO's option -N (don't normalize)!");
			System.err.println("Option -G (global normalization) is overriding SMO's option to normalize!" +
			" Maybe you are simply specifying to normalize to 0-mean, 1-var using a global filter, which is legal.");
		}

		m_onlyNumeric = true;
		//assume class is last attribute
		for (int i = 0; i < inputData.numAttributes()-1; i++) {
			if (!inputData.attribute(i).isNumeric()) {
				m_onlyNumeric = false;
				break;
			}
		}

		//check if we need to do any sort of filtering that would change
		//the original input instances
		m_ouseSomeFilter = !m_onlyNumeric || m_oareThereMissingAttributes ||
										 m_odoGlobalNormalization || m_Normalize || m_ouseAttributeSelection;

		Instances instances = null;
		if (m_ouseSomeFilter) {
			//need to take a copy because inputData will be modified
			instances = new Instances(inputData);
			if (m_oareThereMissingAttributes) {
				m_Missing = new ReplaceMissingValuesFilter();
				m_Missing.setInputFormat(instances);
				instances = Filter.useFilter(instances, m_Missing);
			} else {
				m_Missing = null;
			}

			if (!m_onlyNumeric) {
				m_NominalToBinary = new NominalToBinaryFilter();
				m_NominalToBinary.setInputFormat(instances);
				instances = Filter.useFilter(instances, m_NominalToBinary);
			} else {
				m_NominalToBinary = null;
			}

			// m_globalNormalizationFilter is a global filter. There is also an
			// option of using individual normalization filters, one per SharedBinaryKernelClassifier
			if (m_odoGlobalNormalization) {
				if (m_ouseOriginalNormalization) {
					m_globalNormalizationFilter = new NormalizationFilter();
				} else {
					//use 0 mean and 1 variance
					if (m_oareThereMissingAttributes) {
						m_globalNormalizationFilter = new StandardizeFilter();
					} else {
					  m_globalNormalizationFilter = new FastStandardizeFilter();
					}
				}
				m_globalNormalizationFilter.setInputFormat(instances);
				instances = Filter.useFilter(instances, m_globalNormalizationFilter);
			} else {
				//don't filter
				m_globalNormalizationFilter = null;
			}
		} else {
			//save RAM, because we will not modify the input data
			instances = inputData;
			m_Missing = null;
			m_globalNormalizationFilter = null;
			m_NominalToBinary = null;
		}

		m_instancesHeader = new Instances(instances, 0, 0);

		if (m_odebug) {
			if (m_ofindSVMParametersThroughCrossValidation) {
				System.err.println("Will find kernel parameters using cross-validation...");
			} else {
				System.err.println("Will use the initial (provided by user) kernel parameters...");
			}
		}

		if (m_ofindSVMParametersThroughCrossValidation) {
			//update m_Classifier such that final SMO has the chose configuration
			findSVMParametersAndUpdateConfiguration (instances);
		}

		if (m_odebug && m_ofindSVMParametersThroughCrossValidation) {
			System.err.println("Final options chosen through cross-validation: " + Utils.joinOptions( ((BinaryKernelClassifier) m_Classifier).getOptions()));
		}

		//use current configuration in m_Classifier and build final classifier with whole data
		buildClassifierBasedOnCurrentConfiguration (instances);
	}

	//update m_Classifier such that final SMO has the chosen configuration
	private void findSVMParametersAndUpdateConfiguration (Instances instances) throws Exception {
		SMO bestSMOConfiguration = null;
		double dC = ((SMO) m_Classifier).getC();
		boolean oisGaussianKernel = (((BinaryKernelClassifier) m_Classifier).m_kernel.isAGaussianKernel());

		double dinitialGamma = 1;
		if (oisGaussianKernel) {
			//start from gamma provided by user
			dinitialGamma = (((BinaryKernelClassifier) m_Classifier).m_kernel.getGamma());
		}
		double dgamma = dinitialGamma;

		double derrorForInitialConfiguration = getErrorUsingCrossValidation(instances, dC, dgamma, oisGaussianKernel);
		if (m_odebug) {
			if (oisGaussianKernel) {
				System.err.println("C = " + dC + ", gamma = " + dinitialGamma + " => average error = " + IO.format(derrorForInitialConfiguration));
			} else {
				System.err.println("C = " + dC + " => average error = " + IO.format(derrorForInitialConfiguration));
			}
		}

		if (oisGaussianKernel) {
			if (m_odebug) {
				System.err.println("Started finding best gamma for Gaussian kernel...");
			}

			//find best gamma with fixed C = 1
			double dsmallestErrorForLargerValue = derrorForInitialConfiguration;
			double dbestForLargerValue = dgamma;
			double dsmallestErrorForSmallerValue = derrorForInitialConfiguration;
			double dbestForSmallerValue = dgamma;
			int nattempts = 0;
			//increase gamma
			while (true) {
				dgamma *= 2;
				double derror = getErrorUsingCrossValidation(instances, dC, dgamma, oisGaussianKernel);
				if (m_odebug) {
					System.err.println("Gamma = " + dgamma + " => average error = " + IO.format(derror));
				}
				if (derror < dsmallestErrorForLargerValue) {
					dsmallestErrorForLargerValue = derror;
					dbestForLargerValue = dgamma;
					//reset counter because we got a decrease in error:
					nattempts = 0;
				} else {
					nattempts++;
					if (m_odebug) {
						System.err.println(" ERROR INCREASED attempt = " + nattempts + " / " +
						m_nnumberOfAttemptsToGetADecreaseInError);
					}
					if (nattempts == m_nnumberOfAttemptsToGetADecreaseInError) {
						break;
					}
				}
			}
			//decrease gamma
			dgamma = dinitialGamma;
			nattempts = 0;
			while (true) {
				dgamma /= 2;
				double derror = getErrorUsingCrossValidation(instances, dC, dgamma, oisGaussianKernel);
				if (m_odebug) {
					System.err.println("Gamma = " + dgamma + " => average error = " + IO.format(derror));
				}
				if (derror < dsmallestErrorForSmallerValue) {
					dsmallestErrorForSmallerValue = derror;
					dbestForSmallerValue = dgamma;
					//reset counter because we got a decrease in error:
					nattempts = 0;
				} else {
					nattempts++;
					if (m_odebug) {
						System.err.println(" ERROR INCREASED attempt = " + nattempts + " / " +
						m_nnumberOfAttemptsToGetADecreaseInError);
					}
					if (nattempts == m_nnumberOfAttemptsToGetADecreaseInError) {
						break;
					}
				}
			}
			//update to be used when finding C
			dgamma = (dsmallestErrorForSmallerValue < dsmallestErrorForLargerValue) ? dbestForSmallerValue : dbestForLargerValue;
			((BinaryKernelClassifier) m_Classifier).setKernelOptions(dgamma, Kernel.GAUSSIAN_KERNEL, -1);
			//((BinaryKernelClassifier) m_Classifier).m_kernel = new Kernel(dgamma, Kernel.GAUSSIAN_KERNEL, -1);
			if (m_odebug) {
				System.err.println("@@@ Best gamma = " + dgamma + " => smallest error = " +
				IO.format((dsmallestErrorForSmallerValue < dsmallestErrorForLargerValue) ? dsmallestErrorForSmallerValue :dsmallestErrorForLargerValue));
			}
		}

		//never try best C (maybe later make it optional)
		//note that I'm not updating C from user option, etc. If want, take a look at class SVM
		if (false) {
			if (m_odebug) {
				System.err.println("Started finding best value for C...");
			}
			//find best gamma with fixed C = 1
			double dsmallestErrorForLargerValue = derrorForInitialConfiguration;
			double dbestForLargerValue = 1;
			double dsmallestErrorForSmallerValue = derrorForInitialConfiguration;
			double dbestForSmallerValue = 1;
			int nattempts = 0;
			//increase C
			while (true) {
				dC *= 2;
				double derror = getErrorUsingCrossValidation(instances, dC, dgamma, oisGaussianKernel);
				if (m_odebug) {
					System.err.println("C = " + dC + " => average error = " + IO.format(derror));
				}
				if (derror < dsmallestErrorForLargerValue) {
					dsmallestErrorForLargerValue = derror;
					dbestForLargerValue = dC;
					//reset counter because we got a decrease in error:
					nattempts = 0;
				} else {
					nattempts++;
					if (m_odebug) {
						System.err.println(" ERROR INCREASED attempt = " + nattempts + " / " +
						m_nnumberOfAttemptsToGetADecreaseInError);
					}
					if (nattempts == m_nnumberOfAttemptsToGetADecreaseInError) {
						break;
					}
				}
			}
			//decrease C
			dC = 1;
			nattempts = 0;
			while (true) {
				dC /= 2;
				double derror = getErrorUsingCrossValidation(instances, dC, dgamma, oisGaussianKernel);
				if (m_odebug) {
					System.err.println("C = " + dC + " => average error = " + IO.format(derror));
				}
				if (derror < dsmallestErrorForSmallerValue) {
					dsmallestErrorForSmallerValue = derror;
					dbestForSmallerValue = dC;
					//reset counter because we got a decrease in error:
					nattempts = 0;
				} else {
					nattempts++;
					if (m_odebug) {
						System.err.println(" ERROR INCREASED attempt = " + nattempts + " / " +
						m_nnumberOfAttemptsToGetADecreaseInError);
					}
					if (nattempts == m_nnumberOfAttemptsToGetADecreaseInError) {
						break;
					}
				}
			}
			dC = (dsmallestErrorForSmallerValue < dsmallestErrorForLargerValue) ? dbestForSmallerValue : dbestForLargerValue;
			((SMO) m_Classifier).setC(dC);
			if (m_odebug) {
				System.err.println("@@@ Best C = " + dC + " => smallest error = " +
				((dsmallestErrorForSmallerValue < dsmallestErrorForLargerValue) ? dsmallestErrorForSmallerValue :dsmallestErrorForLargerValue));
			}
		}
	}

	private double getErrorUsingCrossValidation(Instances data, double dC, double dgamma, boolean oisGaussianKernel) throws Exception {
		double[] derrors = crossValidateModel(data, dC, dgamma, oisGaussianKernel, m_nnumberOfFoldsForCrossValidationWhenFindingSVMParameters);
		if (m_odebug) {
			System.err.print("Errors of each fold = [");
			for (int i = 0; i < derrors.length; i++) {
				System.err.print(" " + IO.format(derrors[i]));
			}
			System.err.print(" ] ");
		}
		int ncounter = 0;
		double dmean = 0;
		for (int i = 0; i < derrors.length; i++) {
			if (!Double.isNaN(derrors[i])) {
				dmean += derrors[i];
				ncounter++;
			}
		}
		return dmean / ncounter;
	}

	private double[] crossValidateModel(Instances data, double dC, double dgamma, boolean oisGaussianKernel, int numFolds) throws Exception {
		// Make a copy of the data so that we can reorder
		data = new Instances(data);

		if (data.classAttribute().isNominal()) {
			if (numFolds == 1) {
				if (data.numInstances() <= (m_nmaximumNumOfExamplesForCVTrainingWithOneFold + m_nmaximumNumOfExamplesForCVTestWithOneFold)) {
					//don't have enough data
					numFolds = 2;
					data.stratify(2);
				}
			} else {
				data.stratify(numFolds);
			}
		}
		double[] derrors = new double[numFolds];
		// Do the folds
		for (int i = 0; i < numFolds; i++) {
			Instances train = null;
			Instances test = null;
			if (numFolds == 1) {
				int nmaxTrain = m_nmaximumNumOfExamplesForCVTrainingWithOneFold;
				int nmaxTest = m_nmaximumNumOfExamplesForCVTestWithOneFold;
				Instances[] cvInstances = Instances.splitDataset(data, nmaxTrain, nmaxTest, m_odebug);
				train = cvInstances[0]; //data.trainCV(m_nnumberOfFoldsToSubstitute1, i);
				test = cvInstances[1]; //data.testCV(m_nnumberOfFoldsToSubstitute1, i);
			} else {
				train = data.trainCV(numFolds, i);
				test = data.testCV(numFolds, i);
			}
			//ak: if one wants to use a training set with small number of
			//instances (invert the role of # of folds), can uncomment
			//the lines below
			//classifier.buildClassifier(test);
			//evaluateModel(classifier, train);
			KernelClassifier svm = (KernelClassifier) Classifier.makeCopy(this);
			//((SMO) svm.m_Classifier).setC(dC);
			if (oisGaussianKernel) {
				((BinaryKernelClassifier) m_Classifier).m_kernel = new Kernel(dgamma, Kernel.GAUSSIAN_KERNEL, -1);
				//((SMO) svm.m_Classifier).setRBFKernelGamma(dgamma);
			}

			//hack: Evaluation will call the method classifyInstance, which assumes
			//the test Instance was not filtered, but here they were. So, I will
			//disable all filters.
			//keep original values:
//			boolean oareThereMissingAttributes = m_oareThereMissingAttributes;
//			boolean onlyNumeric = m_onlyNumeric;
//			boolean odoGlobalNormalization = m_odoGlobalNormalization;
//			svm.m_oareThereMissingAttributes = false;
//			svm.m_onlyNumeric = true;
			svm.setGlobalNormalization(false);
			svm.setFindSVMParametersThroughCrossValidation(false);
			svm.setDebug(false);
			if (this.getGlobalNormalization()) {
				//if global normalization is used, it overrides the individual normalization
				((BinaryKernelClassifier) svm.m_Classifier).setNormalizeData(false);
			}

//			try {
				svm.buildClassifier(train);
				Evaluation evaluation = new Evaluation(train);
				//Evaluation evaluation = new Evaluation(m_instancesHeader);

				//now, evaluate the model
				evaluation.evaluateModel(svm, test);
				derrors[i] = evaluation.errorRate();
	//			//restore original values
	//			m_oareThereMissingAttributes = oareThereMissingAttributes;
	//			m_onlyNumeric = onlyNumeric;
	//			m_odoGlobalNormalization = odoGlobalNormalization;
//			}
//			catch (ClassifierException ex) {
				//indicate that couldn't train this classifier
				//derrors[i] = Double.NaN;
//			}
		}
		return derrors;
	}

	//uses configuration in m_Classifier to design SharedBinaryKernelClassifiers
	protected void buildClassifierBasedOnCurrentConfiguration (Instances instances) throws Exception {

		//get configuration information from SMO
		BinaryKernelClassifier smoWithConfiguration = (BinaryKernelClassifier ) m_Classifier;
		m_Normalize = smoWithConfiguration.getNormalizeData();
		m_ouseOriginalNormalization = smoWithConfiguration.getUseOriginalNormalization();

		//assume machine is never linear (for a linear machine, use SVM)
		m_oisLinearMachine = false;
		//if (smoWithConfiguration.getExponent() == 1.0 && smoWithConfiguration.getKernelFunctionAsInteger() != SMO.RBF_KERNEL) {
		//	m_oisLinearMachine = true;
		//}

		m_ouseSharedCache = canUseSharedCache() && (! m_oisLinearMachine);

		super.initializeFiltersAndVariables(instances);

		int[][] nsupportVectorsMaps = new int[m_nnumberOfBinaryClassifiers][];

		if (instances.numClasses() <= 2) {
			if (m_ouseAttributeSelection) {
				End.throwError("Attribute selection is not supported for binary problems!");
			}
			m_Classifiers = new Classifier[1];
			int[] map = new int[instances.numInstances()];
			for (int i = 0; i < map.length; i++) {
				map[i] = i;
			}
			m_Classifiers[0] = new SharedBinaryKernelClassifier();
			((SharedBinaryKernelClassifier) m_Classifiers[0]).setSupportVectorsMap(map);
			//m_outputModelFileName = "temp_model_0.svmt";
			if (m_odebug) {
				System.err.println("Started training binary classifier...");
			}
			m_Classifiers[0].buildClassifier(instances);
		} else {
			//m_Classifiers = Classifier.makeCopies(m_Classifier, m_nnumberOfBinaryClassifiers);
			m_Classifiers = new Classifier[m_nnumberOfBinaryClassifiers];
			for (int i = 0; i < m_nnumberOfBinaryClassifiers; i++) {
				//both positive and negative labels
				int[] nclassesUsedToTrainClassifier = super.makeIndicesStartWithZero(m_trinaryECOCMatrix.getIndicesOfPositiveEntries(i) +
								"," + m_trinaryECOCMatrix.getIndicesOfNegativeEntries(i));
				//IO.DisplayVector(nclassesUsedToTrainClassifier);
				//use super class method, the method below of this class normalizes the instances
				Instances newInsts = super.filterInstancesForGivenBinaryClassifier(instances, i);

				int nnumberOfInstancesInTrainingSetForClassifier = newInsts.numInstances();
				//need to keep the index in training set that corresponds to index in training set for this binary classifier
				nsupportVectorsMaps[i] = super.getIndicesInTrainingSet(instances, nnumberOfInstancesInTrainingSetForClassifier, nclassesUsedToTrainClassifier);

				//System.out.println("Classifier " + i);
				//IO.DisplayVector(nsupportVectorsMaps[i]);
				//build binary classifier with filtered instances
				m_Classifiers[i] = new SharedBinaryKernelClassifier();
				((SharedBinaryKernelClassifier) m_Classifiers[i]).setSupportVectorsMap(nsupportVectorsMaps[i]);

				//set name to be written
				//m_outputModelFileName = "temp_model.svmt";

				//below is not good, because ZeroR would always be biased towards
				//the class in "blank" instance. ZeroR can deal with empty training
				//data, it always chooses class index 0 and outputs score = 0.
				//As we are interested in the score, it's ok. So I will take this out:
				//don't let a classifier be trained if there are no instances
				//if (newInsts.numInstances() == 0) {
				//	newInsts.addBlankInstance();
				//}
				if (newInsts.numInstances() == 0) {
						m_Classifiers[i] = new ZeroR();
						m_Classifiers[i].buildClassifier(newInsts);
				} else {
					try {
						m_Classifiers[i].buildClassifier(newInsts);
					} catch (ClassifierException ex) {
						if (m_odebug) {
							System.err.println("Could not train binary classifier " + (i+1) + ". Will use a ZeroR classifier.");
						}
						m_Classifiers[i] = new ZeroR();
						m_Classifiers[i].buildClassifier(newInsts);
					}
				}

				//ak
//				if (((SharedBinaryKernelClassifier) m_Classifiers[i]).m_nsupportVectorIndices == null) {
//					System.out.println("HERE");
//					System.exit(1);
//				}

				//if using cross-validation, don't print following message (too many)
				if (m_odebug && !m_ofindSVMParametersThroughCrossValidation) {
					if (m_trinaryECOCMatrix.isAllPairs()) {
						System.err.println("Finished training binary classifier " + (i+1) + " " + m_trinaryECOCMatrix.getIdentifierOfGivenBinaryClassifier(i) +
						" / " + m_nnumberOfBinaryClassifiers);

					} else {
						System.err.println("Finished training binary classifier " + (i+1) + " / " + m_nnumberOfBinaryClassifiers);
					}
				}
			}
		}

		organizeUniqueSupportVectors(instances);

		if (m_ouseAttributeSelection) {
			organizeSelectedAttributes();
		}

		if (m_ouseSharedCache) {
			m_dkernelValuesCache = new double[m_funiqueSupportVectors.length];
		}
	}

	protected  void organizeSelectedAttributes() {
		//System.out.println("m_instancesHeader.numAttributes() = " + m_instancesHeader.numAttributes());
		boolean[] owasAttributeSelected = new boolean [m_instancesHeader.numAttributes()];
		m_natributeOccurrences = new int[m_instancesHeader.numAttributes()];
		//I am not going to take the class in account
		//last attribute is class and is always selected
		//owasAttributeSelected[owasAttributeSelected.length - 1] = true;
		for (int i = 0; i < m_Classifiers.length; i++) {
			//System.out.print("Classifier " + i + " selected: ");
			int[] nindicesOfAttributes = ((SharedBinaryKernelClassifier) m_Classifiers[i]).getIndicesOfSelectedAttributes();
			for (int j = 0; j < nindicesOfAttributes.length; j++) {
				//System.out.print(nindicesOfAttributes[j] + " ");
				owasAttributeSelected[nindicesOfAttributes[j]] = true;
				m_natributeOccurrences[nindicesOfAttributes[j]]++;
			}
			//System.out.print("\n");
		}
		//IO.DisplayVector(owasAttributeSelected);
		//count how many
		int n = 0;
		for (int i = 0; i < owasAttributeSelected.length; i++) {
			if (owasAttributeSelected[i]) {
				n++;
			}
		}
		m_nselectedAttributes = new int[n];
		n = 0;
		for (int i = 0; i < owasAttributeSelected.length; i++) {
			if (owasAttributeSelected[i]) {
				m_nselectedAttributes[n++] = i;
			}
		}
	}

	protected void organizeUniqueSupportVectors(Instances instances) {
		int nnumInstanceInMultiClass = instances.numInstances();
		//go over all SVM's in m_Classifiers and count number of unique support vectors
		boolean[] oisSupportVector = new boolean [nnumInstanceInMultiClass];
		for (int i = 0; i < m_Classifiers.length; i++) {
			if (! (m_Classifiers[i] instanceof SharedBinaryKernelClassifier) ) {
				//m_Classifiers[i] wasn't trained properly, and it's probably a ZeroR classifier
				continue;
			}
			int[] nsupportVectorIndices = ((SharedBinaryKernelClassifier) m_Classifiers[i]).m_nsupportVectorOriginalIndices;
			for (int j = 0; j < nsupportVectorIndices.length; j++) {
				oisSupportVector[nsupportVectorIndices[j]] = true;
			}
		}
		//count # true's in oisSupportVector, allocate space and return cache
		int nuniqueSupportVectors = 0;
		for (int i = 0; i < oisSupportVector.length; i++) {
			if (oisSupportVector[i]) {
				nuniqueSupportVectors++;
			}
		}

		//do NOT subtract class attribute
		int nspaceDimension = instances.numAttributes();
		m_funiqueSupportVectors = new float[nuniqueSupportVectors][nspaceDimension];
		int[] nsupportVectors = new int[nnumInstanceInMultiClass];

		//only indices of SV's will be different than -1
		for (int i = 0; i < nnumInstanceInMultiClass; i++) {
			nsupportVectors[i] = -1;
		}

		int ncounter = 0;
		for (int i = 0; i < nnumInstanceInMultiClass; i++) {
			if (oisSupportVector[i]) {
				nsupportVectors[i] = ncounter;
				float[] fin = instances.instance(i).getAttributesReference();
				System.arraycopy(fin, 0, m_funiqueSupportVectors[ncounter], 0, nspaceDimension);
				ncounter++;
			}
		}

		//Print.dialog("nuniqueSupportVectors = " + nuniqueSupportVectors);
		//IO.DisplayVector(oisSupportVector);
		//IO.DisplayVector(nsupportVectors);

		//now go over all SVM's and convert the instance index to the index
		//corresponding to the array of unique SV's
		for (int i = 0; i < m_Classifiers.length; i++) {
			if (! (m_Classifiers[i] instanceof SharedBinaryKernelClassifier) ) {
				//m_Classifiers[i] wasn't trained properly, and it's probably a ZeroR classifier
				continue;
			}
			((SharedBinaryKernelClassifier) m_Classifiers[i]).convertIndicesToUniqueSVsCache(nsupportVectors);
		}

	}

	public double classifyInstance (Instance inst) throws Exception {
		m_osharedCacheWasFilled = false;

		if (m_ouseSomeFilter) {
			// Filter instance
			if (m_oareThereMissingAttributes) {
				m_Missing.input(inst);
				m_Missing.batchFinished();
				inst = m_Missing.output();
			}

			if (!m_onlyNumeric) {
				m_NominalToBinary.input(inst);
				m_NominalToBinary.batchFinished();
				inst = m_NominalToBinary.output();
			}

			if (m_odoGlobalNormalization) {
				m_globalNormalizationFilter.input(inst);
				m_globalNormalizationFilter.batchFinished();
				inst = m_globalNormalizationFilter.output();
			}
		}

		if (m_ouseSharedCache) {
			//fill up cache
			//SMO smo = (SMO) m_Classifier;
			Kernel kernel = ((BinaryKernelClassifier)  m_Classifier).m_kernel;
			for (int i = 0; i < m_funiqueSupportVectors.length; i++) {
				Instance sv = new Instance(1, m_funiqueSupportVectors[i]);
				//m_dkernelValuesCache[i] = smo.calculateKernel(inst, sv);
				m_dkernelValuesCache[i] = kernel.calculateKernel(inst, sv);
			}
			m_osharedCacheWasFilled = true;
		}

		//call super class's classifyInstance, it's going to get the raw scores
		//from SVM's and do everything else. Note SVM's don't need instance
		//to get scores if we are using the cache
		return super.classifyInstance(inst);
	}

	public double[] getBinaryHardDecisions (Instance inst) throws Exception {
		m_osharedCacheWasFilled = false;
		if (m_ouseSomeFilter) {
			// Filter instance
			if (m_oareThereMissingAttributes) {
				m_Missing.input(inst);
				m_Missing.batchFinished();
				inst = m_Missing.output();
			}

			if (!m_onlyNumeric) {
				m_NominalToBinary.input(inst);
				m_NominalToBinary.batchFinished();
				inst = m_NominalToBinary.output();
			}

			if (m_odoGlobalNormalization) {
				m_globalNormalizationFilter.input(inst);
				m_globalNormalizationFilter.batchFinished();
				inst = m_globalNormalizationFilter.output();
			}
		}

		if (m_ouseSharedCache) {
			//fill up cache
			Kernel kernel = ((BinaryKernelClassifier)  m_Classifier).m_kernel;
			for (int i = 0; i < m_funiqueSupportVectors.length; i++) {
				Instance sv = new Instance(1, m_funiqueSupportVectors[i]);
				//m_dkernelValuesCache[i] = smo.calculateKernel(inst, sv);
				m_dkernelValuesCache[i] = kernel.calculateKernel(inst, sv);
			}
			m_osharedCacheWasFilled = true;
		}

		//call super class's getBinaryHardDecisions, it's going to get the raw scores
		//from SVM's and do everything else. Note SVM's don't need instance
		//to get scores if we are using the cache
		return super.getBinaryHardDecisions(inst);
	}


	//commented out because getRawScore already normalizes...
	//when ProbabilityMultiClassClassifier calls this method to get scores,
	//the instances are not filtered in ScoreMultiClassClassifier
//	protected Instances filterInstancesForGivenBinaryClassifier(Instances insts,
//	int nbinaryClassifier) throws Exception {
//		Instances newInsts = super.filterInstancesForGivenBinaryClassifier(insts, nbinaryClassifier);
//		if (m_odoGlobalNormalization) {
//			newInsts = Filter.useFilter(newInsts, m_globalNormalizationFilter);
//		}
//		return newInsts;
//	}

	public static void writeInSVMTorchFormat(Instances insts, String outputFile,
	boolean oisBinary) throws IOException {

		//Print.dialog("Writing " + outputFile);

		BufferedWriter bufferedWriter = IO.openBufferedWriter(outputFile);

		int n = insts.numAttributes();
		//now convert data
		int ni = insts.numInstances();
		bufferedWriter.write(ni + IO.m_NEW_LINE + n);
		bufferedWriter.newLine();
		for (int i = 0; i < ni; i++) {
			Instance inst = insts.instance(i);
			//double[] data = inst.toDoubleArray();
			float[] data = inst.toFloatArray();
			//IO.DisplayVector(data);
			for (int j = 0; j < data.length-1; j++) {
				bufferedWriter.write(data[j] + " ");
			}
			int nclass = (int) data[data.length-1];
			if (oisBinary && nclass == 0) {
				nclass = -1;
			}
			bufferedWriter.write("" + nclass);
			bufferedWriter.newLine();
		}

		IO.closeBufferedWriter(bufferedWriter);
	}

	public boolean getareThereMissingAttributes() {
		return m_oareThereMissingAttributes;
	}

	public void setareThereMissingAttributes(boolean oareThereMissingAttributes) {
		m_oareThereMissingAttributes = oareThereMissingAttributes;
	}

	public int getNumberOfFoldsForCrossValidationWhenFindingSVMParameters() {
		return m_nnumberOfFoldsForCrossValidationWhenFindingSVMParameters;
	}

	public void setNumberOfFoldsForCrossValidationWhenFindingSVMParameters(int n) {
		m_nnumberOfFoldsForCrossValidationWhenFindingSVMParameters = n;
	}

	public int getNumberOfDistinctSupportVectors () {
		if (m_funiqueSupportVectors == null) {
			return -1;
		} else {
			return m_funiqueSupportVectors.length;
		}
	}

	public String numberOfSupportVectorsToString () {
		StringBuffer stringBuffer = new StringBuffer();
		for (int i = 0; i < m_nnumberOfBinaryClassifiers; i++) {
			if (m_Classifiers[i] != null && m_Classifiers[i] instanceof SharedBinaryKernelClassifier) {
				SharedBinaryKernelClassifier svm = (SharedBinaryKernelClassifier) m_Classifiers[i];
				stringBuffer.append(svm.getNumberOfSupportVectors() + " ");
			}
		}
		return stringBuffer.toString();
	}

	public Enumeration listOptions () {
		Vector vec = new Vector(10);
		vec.addElement(new Option("\tTurn on global normalization (allows using a shared cache " +
		"to speed up computations, but performance may improve if each binary classifier has its own normalization factor",
				"G", 0, "-G"));
		vec.addElement(new Option("\tThere are no missing attributes.\n"
			+"\t(If this flag is set, training and test will go faster, but if there are missing attributes, training will fail).",
			"M", 0,"-M"));
		vec.addElement(new Option("\tUse cross-validation to find the best SVM parameters.\n",
			"K", 0,"-K"));
		vec.addElement(new Option("\tSets number of folds for cross-validation (default: 10).",
			"X", 1,"-X"));
		Enumeration enume = super.listOptions();
		while (enume.hasMoreElements()) {
			vec.addElement(enume.nextElement());
		}
		return vec.elements();
	}

	public void setOptions (String[] options) throws Exception {
		setGlobalNormalization(Utils.getFlag('G', options));
		setareThereMissingAttributes(! Utils.getFlag('M', options));
		setFindSVMParametersThroughCrossValidation(Utils.getFlag('K', options));
		//equivalente of option E for filters (but here, E is used for describing the ECOC matrix)
		m_evaluationClassOptions = Utils.getOption('U', options);
		m_searchClassOptions = Utils.getOption('S', options);
		if (m_searchClassOptions.equals("") && m_evaluationClassOptions.equals("")) {
			m_ouseAttributeSelection = false;
		} else {
			m_ouseAttributeSelection = true;
		}
		String temp = Utils.getOption('X', options);
		if (temp.equals("")) {
			setNumberOfFoldsForCrossValidationWhenFindingSVMParameters(10);
		} else {
			setNumberOfFoldsForCrossValidationWhenFindingSVMParameters(Integer.parseInt(temp));
		}
		super.setOptions(options);
	}

	public String[] getOptions () {
		String[] superOptions = super.getOptions();
		String[] options = new String[superOptions.length + 6];
		int ncurrent = 0;
		if (getGlobalNormalization()) {
			options[ncurrent++] = "-G";
		}
		if (!getareThereMissingAttributes()) {
			options[ncurrent++] = "-M";
		}
		if (getFindSVMParametersThroughCrossValidation()) {
			options[ncurrent++] = "-K";
		}
		options[ncurrent++] = "-X"; options[ncurrent++] = ""+getNumberOfFoldsForCrossValidationWhenFindingSVMParameters();;
		if (ncurrent > 0) {
			System.arraycopy(superOptions, 0, options, ncurrent, superOptions.length);
			return options;
		} else {
			//there are only options from super class
			return superOptions;
		}
	}

	public void setGlobalNormalization(boolean odoGlobalNormalization) {
		m_odoGlobalNormalization = odoGlobalNormalization;
	}

	public boolean getGlobalNormalization() {
		return m_odoGlobalNormalization;
	}

	public void setFindSVMParametersThroughCrossValidation (boolean ofindSVMParametersThroughCrossValidation) {
		m_ofindSVMParametersThroughCrossValidation = ofindSVMParametersThroughCrossValidation;
	}

	public boolean getFindSVMParametersThroughCrossValidation() {
		return m_ofindSVMParametersThroughCrossValidation;
	}

	protected  boolean canUseSharedCache() {
		if (m_ouseAttributeSelection) {
			//each binary classifier can have different set of attributes,
			//so cannot use shared cache as it is implemented now
			return false;
		}
		if (m_odoGlobalNormalization) {
			//in this case, we don't allow individual normalization of SVM's
			return true;
		} else {
			//can use cache only if SVM's are not normalized
			if (m_Normalize) {
				return false;
			} else {
				return true;
			}
		}
	}

	public String toString() {
		if (m_ouseAttributeSelection) {
			StringBuffer stringBuffer = new StringBuffer(super.toString());
			if (m_nselectedAttributes.length == m_instancesHeader.numAttributes()) {
				stringBuffer.append("\nAll attributes were selected.");
			} else {
				stringBuffer.append("\nTotal of " + m_nselectedAttributes.length + " distinct attributes selected (counting with class). Their indices: ");
				stringBuffer.append(convertIndicesToString(m_nselectedAttributes));
			}
			stringBuffer.append("\nHistogram of selected attributes: ");
			stringBuffer.append(convertIndicesToString(m_natributeOccurrences));
			return stringBuffer.toString();
		} else {
			return super.toString();
		}
	}

	private static String convertIndicesToString(int[] nindices) {
		if (nindices == null || nindices.length == 0) {
			return null;
		}
		//puts, again I had the mistake of using new StringBuffer(an integer) without converting to String!
		StringBuffer stringBuffer = new StringBuffer("" + nindices[0]);
		for (int i = 1; i < nindices.length; i++) {
			stringBuffer.append(" " + nindices[i]);
		}
		return stringBuffer.toString();
	}


	protected class SharedBinaryKernelClassifier extends Classifier implements Cloneable, RawScorer {

		//static final long serialVersionUID = -6020644217154427429L;

		private double m_db;
		private double[] m_dalphas;
		//private double[] m_dweights;
		private Filter m_normalizationFilter;
		private Filter m_attributeSelectionFilter;

		//after training, indices are with respect to multiclass training set
		//useful for printing purposes
		private int[] m_nsupportVectorOriginalIndices;
		//after training, indices are with respect to unique support vectors
		//useful for calculating SVM output efficiently
		private int[] m_nsupportVectorIndices;

		//don't forget to make it null later:
		private int[] m_nsupportVectorsMap;

		protected void setSupportVectorsMap(int[] nsupportVectorsMap) {
			m_nsupportVectorsMap = nsupportVectorsMap;
		}

		public int getNumberOfSupportVectors() {
			if (m_nsupportVectorIndices == null) {
				return -1;
			} else {
				return m_nsupportVectorIndices.length;
			}
		}

		/**
		 * Builds the classifiers.
		 *
		 * @param insts the training data.
		 * @exception Exception if a classifier can't be built
		 */
		public void buildClassifier (Instances instances) throws Exception {
			if (m_nsupportVectorsMap == null) {
				throw new Exception("Support vectors map not set!");
			}

			//use m_Classifier to get options
			BinaryKernelClassifier smo = (BinaryKernelClassifier) Classifier.makeCopy(m_Classifier);
			//I am doing normalization here, so turn off inside SMO
			smo.setNormalizeData(false);

			if (m_odoGlobalNormalization)  {
				//we don't normalize here, as it was done globally before
				m_normalizationFilter = null;
			} else {
				instances = setNormalizationFilterForGivenClassifierAndFilter(instances);
			}

			if (m_ouseAttributeSelection) {
				m_attributeSelectionFilter = new AttributeSelectionFilter();
				String[] options = new String[4];
				options[0] = "-S";
				options[1] = m_searchClassOptions;
				options[2] = "-E";
				options[3] = m_evaluationClassOptions;

				((OptionHandler) m_attributeSelectionFilter).setOptions(options);
				m_attributeSelectionFilter.setInputFormat(instances);
				instances = Filter.useFilter(instances, m_attributeSelectionFilter);
				//System.out.println( Utils.joinOptions( ((OptionHandler) m_attributeSelectionFilter).getOptions() ));
				//System.out.println( ((AttributeSelectionFilter) m_attributeSelectionFilter).getOutputFormat().toSummaryString());
			}

				//use Weka's SMO
				//smo.setSupportVectorsMap(m_nsupportVectorsMap);
				smo.buildClassifier(instances);

				m_nsupportVectorOriginalIndices = smo.getSupportVectorIndicesInBinaryInstances();
				if (m_nsupportVectorOriginalIndices == null) {
					//"smo.getSupportVectorIndices() returned null ! This happened once when there were no examples of a class to train the SVM."
					m_nsupportVectorOriginalIndices = new int[0];
					throw new ClassifierException();
				}
			//use m_nsupportVectorsMap to map m_nsupportVectorIndices to indices in multiclass Instances
			//m_nsupportVectorOriginalIndices = getSupportVectorIndicesInBinaryInstances();
			for (int j = 0; j < m_nsupportVectorOriginalIndices.length; j++) {
				m_nsupportVectorOriginalIndices[j] = m_nsupportVectorsMap[m_nsupportVectorOriginalIndices[j]];
			}
			//don't need it anymore, save storage space
			m_nsupportVectorsMap = null;
			m_dalphas = smo.getWeightsReference();
			m_db = smo.getBias();
				//if (m_oisLinearMachine) {
				//	 m_dweights = Cloner.clone(smo.getLinearMachineWeightsReference());
				//}

			//allocate space, vector will be filled after unique SV's are determined (all binary classifiers are trained)
			m_nsupportVectorIndices = new int[m_nsupportVectorOriginalIndices.length];

		}

		//In fact, it doesn't use instance, but the cache that was previously filled up
		public double classifyInstance (Instance instance) throws Exception {
			if (true) {
				throw new Exception("Thought I would never call this method...");
			}
			return -1;
		}

		//called by super class ScoreMultiClassClassifier
		public double getRawScore (Instance instance) {
			try {

				double result = 0;

				//I need to use m_osharedCacheWasFilled because this method can be
				//called without being through SVM's method classifyInstance, and
				//then the cache would not be filled.
				if (m_ouseSharedCache && m_osharedCacheWasFilled) {
					//can use general cache
					//it doesn't use instance, but the cache that was previously filled up
					for (int i = 0; i < m_dalphas.length; i++) {
						//System.out.println("" + m_nsupportVectorIndices[i] + " " + m_dkernelValuesCache.length);
						result += m_dalphas[i] * m_dkernelValuesCache[m_nsupportVectorIndices[i]];
					}
					return result - m_db;
				}

				//cannot use general cache because each binary classifier has its own normalization
				if (m_normalizationFilter != null) {
					m_normalizationFilter.input(instance);
					m_normalizationFilter.batchFinished();
					instance = m_normalizationFilter.output();
				} else if (m_odoGlobalNormalization) {
					//take in account the case where this method was not called through
					//SVM's method classifyInstance, and normalize if needed
					m_globalNormalizationFilter.input(instance);
					m_globalNormalizationFilter.batchFinished();
					instance = m_globalNormalizationFilter.output();
				}

				if (m_ouseAttributeSelection) {
					//System.out.println(instance.toString());
					m_attributeSelectionFilter.input(instance);
					m_attributeSelectionFilter.batchFinished();
					instance = m_attributeSelectionFilter.output();
					//System.out.println(instance.toString());
				}

				float[] x = instance.getAttributesReference();

				//if (m_oisLinearMachine) {
				//	for (int i = 0; i < x.length-1; i++) {
				//		result += m_dweights[i] * x[i];
				//	}
				//} else {
					//SMO smo = (SMO) m_Classifier;
					Kernel kernel = ((BinaryKernelClassifier)  m_Classifier).m_kernel;
					//BinaryKernelClassifier smo = (BinaryKernelClassifier) m_Classifier;
					//here I have 4 cases: normalization (true or false) and attribute_selection (true or false)
					//to save computations I should have a switch case or if's outside the loop. Right now
					//m_ouseAttributeSelection is inside the loop
					if (m_normalizationFilter != null) {
						for (int i = 0; i < m_dalphas.length; i++) {
							//need to normalize support vector
							Instance sv = new Instance(1, m_funiqueSupportVectors[m_nsupportVectorIndices[i]]);
							sv.setDataset(m_instancesHeader);
							m_normalizationFilter.input(sv);
							m_normalizationFilter.batchFinished();
							sv = m_normalizationFilter.output();
							if (m_ouseAttributeSelection) {
								m_attributeSelectionFilter.input(sv);
								m_attributeSelectionFilter.batchFinished();
								sv = m_attributeSelectionFilter.output();
							}
							result += m_dalphas[i] * kernel.calculateKernel(instance, sv);
						}
					} else {
						//don't need to filter
						for (int i = 0; i < m_dalphas.length; i++) {
							Instance sv = new Instance(1, m_funiqueSupportVectors[m_nsupportVectorIndices[i]]);
							if (m_ouseAttributeSelection) {
								m_attributeSelectionFilter.input(sv);
								m_attributeSelectionFilter.batchFinished();
								sv = m_attributeSelectionFilter.output();
							}
							result += m_dalphas[i] * kernel.calculateKernel(instance, sv);
						}
					}

				return result - m_db;
			} catch (Exception ex) {
				ex.printStackTrace();
				return -1;
			}
			//System.out.println("instance " + x[0] + " " + result);
		}

		/**
		 * nindicesInArrayOfUniqueSupportVectors has the size of the multiclass
		 * training set and has zero entries unless it's a support vector. Ex:
		 * -1 -1 0 -1 -1 1 -1 2 -1 -1 3 4 ...
		 * before calling this method, m_nsupportVectorIndices has the indices of SV's
		 * 2 7 9 14
		 * now I convert it to the index in the unique vector's array:
		 * 0 2
		 * (this SVM uses the first and third distinct support vectors)
		 */
		private void convertIndicesToUniqueSVsCache(int[] nindicesInArrayOfUniqueSupportVectors) {
			for (int i = 0; i < m_nsupportVectorIndices.length; i++) {
				m_nsupportVectorIndices[i] = nindicesInArrayOfUniqueSupportVectors[m_nsupportVectorOriginalIndices[i]];
			}
		}

		private Instances setNormalizationFilterForGivenClassifierAndFilter(Instances instances) throws Exception {
			if (m_Normalize) {
				if (m_ouseOriginalNormalization) {
					m_normalizationFilter = new NormalizationFilter();
				} else {
					//use 0 mean and 1 variance
					if (m_oareThereMissingAttributes) {
						m_globalNormalizationFilter = new StandardizeFilter();
					} else {
					  m_globalNormalizationFilter = new FastStandardizeFilter();
					}
				}
				m_normalizationFilter.setInputFormat(instances);
				return Filter.useFilter(instances, m_normalizationFilter);
			} else {
				m_normalizationFilter = null;
				//don't filter
				return instances;
			}
		}

//		private SMO convertToSMO() {
//			SMO smo = null;
//			try {
//				smo = (SMO) Classifier.makeCopy(m_Classifier);
//			} catch (Exception e) {
//				e.printStackTrace();
//				return null;
//			}
//			smo.setSMO(m_db,
//				m_nsupportVectorOriginalIndices,
//				m_dalphas,
//				m_dweights,
//				m_oisLinearMachine,
//				m_instancesHeader);
//			return smo;
//		}

//		public String toString() {
//			if (m_dalphas != null || m_dweights != null) {
//				SMO smo = convertToSMO();
//				return smo.toString();
//			} else {
//				return "Model not build yet!";
//			}
//		}

	public String toString() {

		StringBuffer text = new StringBuffer();

		if (m_dalphas == null) {
			return "SharedBinaryKernelClassifier: No model built yet.";
		}
		try {
			text.append("SharedBinaryKernelClassifier\n\n");
			Instances headerInstances = null;
			if (m_ouseAttributeSelection) {
				headerInstances = m_attributeSelectionFilter.getOutputFormat();
			}  else {
				headerInstances = m_instancesHeader;
			}

			// If machine linear, print weight vector
//			if (m_oisLinearMachine) {
//				//print support vectors if machine is linear and their number is < 20
//				if (getNumberOfSupportVectors()  < 20) {
//					text.append("Support vectors (training set indices): ");
//					for (int i = 0; i < m_nsupportVectorOriginalIndices.length; i++) {
//						text.append(" " + m_nsupportVectorOriginalIndices[i]);
//					}
//					text.append("\n");
//				}
//				text.append("Machine linear: showing attribute weights, ");
//				text.append("not support vectors.\n\n");
//				text.append("   ");
//				text.append(m_dweights[0] + " * " + headerInstances.attribute(0).name()+"\n");
//				//subtract 1 due to class' attribute
//				for (int i = 1; i < m_dweights.length-1; i++) {
//					text.append(" + ");
//					text.append(m_dweights[i] + " * " + headerInstances.attribute(i).name()+"\n");
//				}
//			} else {
				//machine is not linear
			int nnumberOfPositiveWeights = 0;
			int nnumberOfNegativeWeights = 0;
			for (int i = 0; i < m_dalphas.length; i++) {
				if (m_dalphas[i] > 0) {
					text.append(" " + m_dalphas[i] + " * K[X(" + m_nsupportVectorOriginalIndices[i] + ") * X]\n");
					//text.append("[" + i + "] =  " + IO.format(m_dweights[i]) + "\n");
					nnumberOfPositiveWeights++;
				} else if (m_dalphas[i] < 0) {
					text.append(m_dalphas[i] + " * K[X(" + m_nsupportVectorOriginalIndices[i] + ") * X]\n");
					//text.append("[" + i + "] = " + IO.format(m_dweights[i]) + "\n");
					nnumberOfNegativeWeights++;
				}
			}
			text.append(" - " + m_db + "\n");
			text.append("\n# positive weights = " + nnumberOfPositiveWeights + "\n");
			text.append("# negative weights = " + nnumberOfNegativeWeights + "\n");
			text.append("# weights equal to zero = " +
			(m_dalphas.length - (nnumberOfPositiveWeights + nnumberOfNegativeWeights)) + "\n");


//				text.append("   " + m_dalphas[0] + " * K[X(" + m_nsupportVectorOriginalIndices[0] + ") * X]\n");
//				for (int i = 1; i < m_dalphas.length; i++) {
//					text.append(" + ");
//					text.append(m_dalphas[i] + " * K[X(" + m_nsupportVectorOriginalIndices[i] + ") * X]\n");
//				}
			//}

			text.append("\nNumber of support vectors: " + getNumberOfSupportVectors() + "\n");
			if (m_ouseAttributeSelection) {
				text.append("Attributes: " + convertIndicesToString(this.getIndicesOfSelectedAttributes()) + "\n");
			}
			String[] options = ((BinaryKernelClassifier) m_Classifier).getOptions();
			for (int i = 0; i < options.length; i++) {
				text.append(options[i] + " ");
			}
			text.append("\n");
		} catch (Exception e) {
			e.printStackTrace();
			return "Can't print SharedBinaryKernelClassifier classifier.";
		}

		return text.toString();
	}

		public int[] getIndicesOfSelectedAttributes() {
			if (m_ouseAttributeSelection) {
				return ((AttributeSelectionFilter) m_attributeSelectionFilter).getIndicesOfSelectedAttributes();
			} else {
				return null;
			}
		}

	} // end of inner class

	protected static class ClassifierException extends Exception {
	}

	public static void main (String[] args) throws Exception {
		//test (args);
		Classifier scheme;
		try {
			scheme = new KernelClassifier();
			System.out.println(Evaluation.evaluateModel(scheme, args));

			KernelClassifier kernelClassifier = (KernelClassifier) scheme;
			//if (kernelClassifier.getDebug()) {
			//ak: want to get this output
			if (true) {
				//creates problem when using -p 0 for McNemar's test
				System.out.println("Number of support vectors per binary classifier: " +
				kernelClassifier.numberOfSupportVectorsToString());
				System.out.println("Number of distinct support vectors: " +
				kernelClassifier.getNumberOfDistinctSupportVectors());
			}
		} catch (Exception e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
		//because it invokes Matlab, need to exit explicitly
		System.exit(1);
	}

}
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

//to use Torch, need AKSVMTorch in path.
//AK: IMPORTANT: it didn't work when using global normalization -G with only
//one binary problem.
public class SVM
    extends ScoreMultiClassClassifier
    implements RawScorer {

  //default value for option -X
  private static int m_nnumberOfFoldsForCrossValidationWhenFindingSVMParameters =
      10;

  //if I specify CV (-K) with 1 fold (-X 1), then 1 fold of size below is used
  //private static final int m_nnumberOfFoldsToSubstitute1 = 6;
  private static final int m_nmaximumNumOfExamplesForCVTrainingWithOneFold =
      10000;
  private static final int m_nmaximumNumOfExamplesForCVTestWithOneFold = 10000;

  protected boolean m_odoGlobalNormalization = false;
  protected boolean m_ouseSharedCache = false;
  protected boolean m_oisLinearMachine = false;
  protected boolean m_oareThereMissingAttributes = true;
  protected boolean m_ouseSVMTorch = false;
  protected boolean m_osharedCacheWasFilled = false;
  protected boolean m_ofindSVMParametersThroughCrossValidation = false;

  //all distinct support vectors
  protected float[][] m_funiqueSupportVectors;
  protected double[] m_dkernelValuesCache;
  protected Filter m_globalNormalizationFilter;
  protected Instances m_instancesHeader;
  protected String m_outputModelFileName;

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

  /** Indicates if we need to do any sort of filtering */
  protected boolean m_ouseSomeFilter = true;

  private boolean m_ofilterExamplesForSVMUsingGMM = false;
  private String m_optionsForGMMThatFiltersExamplesForSVM;
  private int m_nfinalNumberOfExamplesPerClassForGMMFilter = 1000;

  /** Only numeric attributes in the dataset? */
  protected boolean m_onlyNumeric;

  protected boolean m_ouseAttributeSelection;
  private String m_evaluationClassOptions;
  private String m_searchClassOptions;
  private int[] m_nselectedAttributes;
  private int[] m_natributeOccurrences;

  private String m_testDatasetForModelSelectionFileName;
  //this can not be set by "regular" Weka, but "externally"
  private Instances m_testInstanceForModelSelection;

  //keep going in a direction until you get no decrease for three steps in a row
  private static final int m_nnumberOfAttemptsToGetADecreaseInError = 3;

  /**
   * Builds the classifiers.
   *
   * @param insts the training data.
   * @exception Exception if a classifier can't be built
   */
  public void buildClassifier(Instances inputData) throws Exception {
    if (m_Classifier == null) {
      throw new Exception("No base classifier has been set!");
    }

    if (! (m_Classifier instanceof SMO)) {
      throw new Exception("Base classifier must be SMO! It was set to " +
                          m_Classifier.getClass());
    }

    if (inputData.classIndex() != inputData.numAttributes() - 1) {
      throw new Exception("Class must be last attribute!");
    }

    //I'm filtering here, in the beginning, to save processing
    //Because I'm using GMMs, I don't need to normalize
    //However, if I use a method different than the random one,
    //i.e., if I need to train GMMs, I should replace the missing attributes
    if (m_ofilterExamplesForSVMUsingGMM) {
      FilterExamplesForSVM filterExamplesForSVM = new FilterExamplesForSVM();
      if (!m_optionsForGMMThatFiltersExamplesForSVM.equals("")) {
        filterExamplesForSVM.m_optionsForGMM =
            m_optionsForGMMThatFiltersExamplesForSVM;
      }
      //method 1 is simply to randomly pick examples
      filterExamplesForSVM.m_nfilteringMethod = 1;
      filterExamplesForSVM.m_nfinalNumberOfExamplesPerClass =
          m_nfinalNumberOfExamplesPerClassForGMMFilter;
      inputData = filterExamplesForSVM.filterExamples(inputData);
      filterExamplesForSVM = null;
      if (m_ofindSVMParametersThroughCrossValidation) {
        System.out.println("# of train examples = " + inputData.numInstances());
      }
    }

    //get configuration information from SMO
    SMO smoWithConfiguration = (SMO) m_Classifier;
    m_Normalize = smoWithConfiguration.getNormalizeData();
    m_ouseOriginalNormalization = smoWithConfiguration.
        getUseOriginalNormalization();
    m_oisLinearMachine = false;
    if (smoWithConfiguration.getExponent() == 1.0 &&
        smoWithConfiguration.getKernelFunctionAsInteger() != SMO.RBF_KERNEL) {
      m_oisLinearMachine = true;
    }

    if (m_odoGlobalNormalization && m_Normalize && m_odebug) {
      //how can I get the option for mean 0, var 1 or [0, 1] with error below
      //throw new Exception("Option -G (global normalization) must be used with SMO's option -N (don't normalize)!");
      System.err.println(
          "Option -G (global normalization) is overriding SMO's option to normalize!" +
          " Maybe you are simply specifying to normalize to 0-mean, 1-var using a global filter, which is legal.");
    }

    m_onlyNumeric = true;
    for (int i = 0; i < inputData.numAttributes(); i++) {
      if (i != inputData.classIndex()) {
        if (!inputData.attribute(i).isNumeric()) {
          m_onlyNumeric = false;
          break;
        }
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
      }
      else {
        m_Missing = null;
      }

      if (!m_onlyNumeric) {
        m_NominalToBinary = new NominalToBinaryFilter();
        m_NominalToBinary.setInputFormat(instances);
        instances = Filter.useFilter(instances, m_NominalToBinary);
      }
      else {
        m_NominalToBinary = null;
      }

      // m_globalNormalizationFilter is a global filter. There is also an
      // option of using individual normalization filters, one per SharedSVM
      if (m_odoGlobalNormalization) {
        if (m_ouseOriginalNormalization) {
          m_globalNormalizationFilter = new NormalizationFilter();
        }
        else {
          //use 0 mean and 1 variance
          if (m_oareThereMissingAttributes) {
            m_globalNormalizationFilter = new StandardizeFilter();
          }
          else {
            m_globalNormalizationFilter = new FastStandardizeFilter();
          }
        }
        m_globalNormalizationFilter.setInputFormat(instances);
        instances = Filter.useFilter(instances, m_globalNormalizationFilter);
      }
      else {
        //don't filter
        m_globalNormalizationFilter = null;
      }
    }
    else {
      //save RAM, because we will not modify the input data
      instances = inputData;
      m_Missing = null;
      m_globalNormalizationFilter = null;
      m_NominalToBinary = null;
    }

    m_instancesHeader = new Instances(instances, 0, 0);

    if (m_odebug) {
      if (m_ofindSVMParametersThroughCrossValidation) {
        System.err.println("Will find SVM parameters using cross-validation...");
      }
      else {
        System.err.println(
            "Will use the initial (provided by user) SVM parameters...");
      }
    }

    if (m_ofindSVMParametersThroughCrossValidation) {
      //update m_Classifier such that final SMO has the chose configuration
      findSVMParametersAndUpdateConfiguration(instances);
    }

    if (m_odebug && m_ofindSVMParametersThroughCrossValidation) {
      System.err.println("Final options chosen through cross-validation: " +
                         ( (SMO) m_Classifier).getOptionsAsString());
    }

    //use current configuration in m_Classifier and build final classifier with whole data
    buildClassifierBasedOnCurrentConfiguration(instances);

    if (m_ofilterExamplesForSVMUsingGMM &&
        m_ofindSVMParametersThroughCrossValidation) {
      System.out.println("SV's = " + getNumberOfDistinctSupportVectors());
    }
  }

  //update m_Classifier such that final SMO has the chosen configuration
  private void findSVMParametersAndUpdateConfiguration(Instances instances) throws
      Exception {
    SMO bestSMOConfiguration = null;
    double dC = ( (SMO) m_Classifier).getC();

    boolean oisGaussianKernel = ( ( (SMO) m_Classifier).
                                 getKernelFunctionAsInteger() == SMO.RBF_KERNEL) ? true : false;

    double dinitialGamma = ( (SMO) m_Classifier).getRBFKernelGamma();
    double dgamma = dinitialGamma;

    double derrorForInitialConfiguration = getErrorUsingCrossValidation(
        instances, dC, dgamma, oisGaussianKernel);
    if (m_odebug) {
      if (oisGaussianKernel) {
        System.err.println("C = " + dC + ", gamma = " + dinitialGamma +
                           " => average error = " +
                           IO.format(derrorForInitialConfiguration));
      }
      else {
        System.err.println("C = " + dC + " => average error = " +
                           IO.format(derrorForInitialConfiguration));
      }
    }

    if (oisGaussianKernel) {
      if (m_odebug) {
        System.err.println("Started finding best gamma for Gaussian kernel...");
      }

      //find best gamma with fixed initial value of C
      double dsmallestErrorForLargerValue = derrorForInitialConfiguration;
      double dbestForLargerValue = dinitialGamma;
      double dsmallestErrorForSmallerValue = derrorForInitialConfiguration;
      double dbestForSmallerValue = dinitialGamma;
      int nattempts = 0;
      //increase gamma
      while (true) {
        dgamma *= 2;
        double derror = getErrorUsingCrossValidation(instances, dC, dgamma,
            oisGaussianKernel);
        if (m_odebug) {
          System.err.println("Gamma = " + dgamma + " => average error = " +
                             IO.format(derror));
        }
        if (derror < dsmallestErrorForLargerValue) {
          dsmallestErrorForLargerValue = derror;
          dbestForLargerValue = dgamma;
          //reset counter because we got a decrease in error:
          nattempts = 0;
        }
        else {
          nattempts++;
          if (m_odebug) {
            System.err.println(" ERROR INCREASED attempt = " + nattempts +
                               " / " +
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
        double derror = getErrorUsingCrossValidation(instances, dC, dgamma,
            oisGaussianKernel);
        if (m_odebug) {
          System.err.println("Gamma = " + dgamma + " => average error = " +
                             IO.format(derror));
        }
        if (derror < dsmallestErrorForSmallerValue) {
          dsmallestErrorForSmallerValue = derror;
          dbestForSmallerValue = dgamma;
          //reset counter because we got a decrease in error:
          nattempts = 0;
        }
        else {
          nattempts++;
          if (m_odebug) {
            System.err.println(" ERROR INCREASED attempt = " + nattempts +
                               " / " +
                               m_nnumberOfAttemptsToGetADecreaseInError);
          }
          if (nattempts == m_nnumberOfAttemptsToGetADecreaseInError) {
            break;
          }
        }
      }
      //update to be used when finding C
      dgamma = (dsmallestErrorForSmallerValue < dsmallestErrorForLargerValue) ?
          dbestForSmallerValue : dbestForLargerValue;
      ( (SMO) m_Classifier).setRBFKernelGamma(dgamma);
      if (m_odebug) {
        System.err.println("@@@ Best gamma = " + dgamma +
                           " => smallest error = " +
                           IO.format( (dsmallestErrorForSmallerValue <
                                       dsmallestErrorForLargerValue) ?
                                     dsmallestErrorForSmallerValue :
                                     dsmallestErrorForLargerValue));
      }
    }

    //always try best C (maybe later make it optional)
    if (true) {
      if (m_odebug) {
        System.err.println("Started finding best value for C...");
      }
      //find best gamma with initial C
      double dinitialC = dC;
      double dsmallestErrorForLargerValue = derrorForInitialConfiguration;
      double dbestForLargerValue = dC;
      double dsmallestErrorForSmallerValue = derrorForInitialConfiguration;
      double dbestForSmallerValue = dC;
      int nattempts = 0;
      //increase gamma
      while (true) {
        dC *= 2;
        double derror = getErrorUsingCrossValidation(instances, dC, dgamma,
            oisGaussianKernel);
        if (m_odebug) {
          System.err.println("C = " + dC + " => average error = " +
                             IO.format(derror));
        }
        if (derror < dsmallestErrorForLargerValue) {
          dsmallestErrorForLargerValue = derror;
          dbestForLargerValue = dC;
          //reset counter because we got a decrease in error:
          nattempts = 0;
        }
        else {
          nattempts++;
          if (m_odebug) {
            System.err.println(" ERROR INCREASED attempt = " + nattempts +
                               " / " +
                               m_nnumberOfAttemptsToGetADecreaseInError);
          }
          if (nattempts == m_nnumberOfAttemptsToGetADecreaseInError) {
            break;
          }
        }
      }
      //decrease gamma
      dC = dinitialC;
      nattempts = 0;
      while (true) {
        dC /= 2;
        double derror = getErrorUsingCrossValidation(instances, dC, dgamma,
            oisGaussianKernel);
        if (m_odebug) {
          System.err.println("C = " + dC + " => average error = " +
                             IO.format(derror));
        }
        if (derror < dsmallestErrorForSmallerValue) {
          dsmallestErrorForSmallerValue = derror;
          dbestForSmallerValue = dC;
          //reset counter because we got a decrease in error:
          nattempts = 0;
        }
        else {
          nattempts++;
          if (m_odebug) {
            System.err.println(" ERROR INCREASED attempt = " + nattempts +
                               " / " +
                               m_nnumberOfAttemptsToGetADecreaseInError);
          }
          if (nattempts == m_nnumberOfAttemptsToGetADecreaseInError) {
            break;
          }
        }
      }
      dC = (dsmallestErrorForSmallerValue < dsmallestErrorForLargerValue) ?
          dbestForSmallerValue : dbestForLargerValue;
      ( (SMO) m_Classifier).setC(dC);
      if (m_odebug) {
        System.err.println("@@@ Best C = " + dC + " => smallest error = " +
                           IO.format( (dsmallestErrorForSmallerValue <
                                       dsmallestErrorForLargerValue) ?
                                     dsmallestErrorForSmallerValue :
                                     dsmallestErrorForLargerValue));
      }
    }
  }

  private double getErrorUsingCrossValidation(Instances data, double dC,
                                              double dgamma,
                                              boolean oisGaussianKernel) throws
      Exception {
    if (m_testInstanceForModelSelection != null) {
      return getErrorUsingTestSet(data, m_testInstanceForModelSelection, dC,
                                  dgamma, oisGaussianKernel,
          m_nnumberOfFoldsForCrossValidationWhenFindingSVMParameters);
    }
    if (getTestDatasetForModelSelectionFileName() != null) {
      Instances testInstances = new Instances(
          getTestDatasetForModelSelectionFileName());
      return getErrorUsingTestSet(data, testInstances, dC, dgamma,
                                  oisGaussianKernel,
          m_nnumberOfFoldsForCrossValidationWhenFindingSVMParameters);
    }

    double[] derrors = crossValidateModel(data, dC, dgamma, oisGaussianKernel,
        m_nnumberOfFoldsForCrossValidationWhenFindingSVMParameters);
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

  private double[] crossValidateModel(Instances instances, double dC,
                                      double dgamma, boolean oisGaussianKernel,
                                      int numFolds) throws Exception {
    // Make a copy of the data we can reorder
    Instances data = new Instances(instances);

    if (data.classAttribute().isNominal()) {
      if (numFolds == 1) {
        if (data.numInstances() <=
            (m_nmaximumNumOfExamplesForCVTrainingWithOneFold +
             m_nmaximumNumOfExamplesForCVTestWithOneFold)) {
          //don't have enough data
          numFolds = 2;
          data.stratify(2);
        }
      }
      else {
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
        Instances[] cvInstances = Instances.splitDataset(data, nmaxTrain,
            nmaxTest, m_odebug);
        train = cvInstances[0]; //data.trainCV(m_nnumberOfFoldsToSubstitute1, i);
        test = cvInstances[1]; //data.testCV(m_nnumberOfFoldsToSubstitute1, i);
      }
      else {
        train = data.trainCV(numFolds, i);
        test = data.testCV(numFolds, i);
      }
      //ak: if one wants to use a training set with small number of
      //instances (invert the role of # of folds), can uncomment
      //the lines below
      //classifier.buildClassifier(test);
      //evaluateModel(classifier, train);
      SVM svm = (SVM) Classifier.makeCopy(this);
      ( (SMO) svm.m_Classifier).setC(dC);
      if (oisGaussianKernel) {
        ( (SMO) svm.m_Classifier).setRBFKernelGamma(dgamma);
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
        ( (SMO) svm.m_Classifier).setNormalizeData(false);
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

  private double getErrorUsingTestSet(Instances train, Instances test,
                                      double dC,
                                      double dgamma, boolean oisGaussianKernel,
                                      int numFolds) throws Exception {

    SVM svm = (SVM) Classifier.makeCopy(this);
    ( (SMO) svm.m_Classifier).setC(dC);
    if (oisGaussianKernel) {
      ( (SMO) svm.m_Classifier).setRBFKernelGamma(dgamma);
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
      ( (SMO) svm.m_Classifier).setNormalizeData(false);
    }

//			try {
    svm.buildClassifier(train);
    Evaluation evaluation = new Evaluation(train);
    //Evaluation evaluation = new Evaluation(m_instancesHeader);

    //now, evaluate the model
    evaluation.evaluateModel(svm, test);
    return evaluation.errorRate();
  }

  //uses configuration in m_Classifier to design SharedSVMs
  protected void buildClassifierBasedOnCurrentConfiguration(Instances instances) throws
      Exception {

    //get configuration information from SMO
    SMO smoWithConfiguration = (SMO) m_Classifier;
    m_Normalize = smoWithConfiguration.getNormalizeData();
    m_ouseOriginalNormalization = smoWithConfiguration.
        getUseOriginalNormalization();
    m_oisLinearMachine = false;
    if (smoWithConfiguration.getExponent() == 1.0 &&
        smoWithConfiguration.getKernelFunctionAsInteger() != SMO.RBF_KERNEL) {
      m_oisLinearMachine = true;
    }

    m_ouseSharedCache = canUseSharedCache() && (!m_oisLinearMachine);

    super.initializeFiltersAndVariables(instances);

    int[][] nsupportVectorsMaps = new int[m_nnumberOfBinaryClassifiers][];

    if (instances.numClasses() <= 2) {
      if (m_ouseAttributeSelection) {
        End.throwError(
            "Attribute selection is not supported for binary problems!");
      }
      m_Classifiers = new Classifier[1];
      int[] map = new int[instances.numInstances()];
      for (int i = 0; i < map.length; i++) {
        map[i] = i;
      }
      m_Classifiers[0] = new SharedSVM();
      ( (SharedSVM) m_Classifiers[0]).setSupportVectorsMap(map);
      //m_outputModelFileName = "temp_model_0.svmt";
      m_outputModelFileName = File.createTempFile("tmp-torch0", "svmt").
          getAbsolutePath();
      if (m_odebug) {
        System.err.println("Started training binary classifier...");
      }
      m_Classifiers[0].buildClassifier(instances);
    }
    else {
      //m_Classifiers = Classifier.makeCopies(m_Classifier, m_nnumberOfBinaryClassifiers);
      m_Classifiers = new Classifier[m_nnumberOfBinaryClassifiers];
      for (int i = 0; i < m_nnumberOfBinaryClassifiers; i++) {
        //both positive and negative labels
        int[] nclassesUsedToTrainClassifier = super.makeIndicesStartWithZero(
            m_trinaryECOCMatrix.getIndicesOfPositiveEntries(i) +
            "," + m_trinaryECOCMatrix.getIndicesOfNegativeEntries(i));
        //IO.DisplayVector(nclassesUsedToTrainClassifier);
        //use super class method, the method below of this class normalizes the instances
        Instances newInsts = super.filterInstancesForGivenBinaryClassifier(
            instances, i);

        int nnumberOfInstancesInTrainingSetForClassifier = newInsts.
            numInstances();
        //need to keep the index in training set that corresponds to index in training set for this binary classifier
        nsupportVectorsMaps[i] = super.getIndicesInTrainingSet(instances,
            nnumberOfInstancesInTrainingSetForClassifier,
            nclassesUsedToTrainClassifier);

        //System.out.println("Classifier " + i);
        //IO.DisplayVector(nsupportVectorsMaps[i]);
        //build binary classifier with filtered instances
        m_Classifiers[i] = new SharedSVM();
        ( (SharedSVM) m_Classifiers[i]).setSupportVectorsMap(
            nsupportVectorsMaps[i]);

        //set name to be written
        //m_outputModelFileName = "temp_model.svmt";
        m_outputModelFileName = File.createTempFile("tmp-torch", ".svmt").
            getAbsolutePath();

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
        }
        else {
          try {
            m_Classifiers[i].buildClassifier(newInsts);
          }
          catch (ClassifierException ex) {
            if (m_odebug) {
              System.err.println("Could not train SVM " + (i + 1) +
                                 ". Will use a ZeroR classifier.");
            }
            m_Classifiers[i] = new ZeroR();
            m_Classifiers[i].buildClassifier(newInsts);
          }
        }

        //if using cross-validation, don't print following message (too many)
        if (m_odebug && !m_ofindSVMParametersThroughCrossValidation) {
          if (m_trinaryECOCMatrix.isAllPairs()) {
            System.err.println("Finished training binary classifier " + (i + 1) +
                               " " +
                               m_trinaryECOCMatrix.
                               getIdentifierOfGivenBinaryClassifier(i) +
                               " / " + m_nnumberOfBinaryClassifiers);

          }
          else {
            System.err.println("Finished training binary classifier " + (i + 1) +
                               " / " + m_nnumberOfBinaryClassifiers);
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

  protected void organizeSelectedAttributes() {
    //System.out.println("m_instancesHeader.numAttributes() = " + m_instancesHeader.numAttributes());
    boolean[] owasAttributeSelected = new boolean[m_instancesHeader.
        numAttributes()];
    m_natributeOccurrences = new int[m_instancesHeader.numAttributes()];
    //I am not going to take the class in account
    //last attribute is class and is always selected
    //owasAttributeSelected[owasAttributeSelected.length - 1] = true;
    for (int i = 0; i < m_Classifiers.length; i++) {
      //System.out.print("Classifier " + i + " selected: ");
      int[] nindicesOfAttributes = ( (SharedSVM) m_Classifiers[i]).
          getIndicesOfSelectedAttributes();
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
    boolean[] oisSupportVector = new boolean[nnumInstanceInMultiClass];
    for (int i = 0; i < m_Classifiers.length; i++) {
      if (! (m_Classifiers[i] instanceof SharedSVM)) {
        //m_Classifiers[i] wasn't trained properly, and it's probably a ZeroR classifier
        continue;
      }
      int[] nsupportVectorIndices = ( (SharedSVM) m_Classifiers[i]).
          m_nsupportVectorOriginalIndices;
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
        System.arraycopy(fin, 0, m_funiqueSupportVectors[ncounter], 0,
                         nspaceDimension);
        ncounter++;
      }
    }

    //Print.dialog("nuniqueSupportVectors = " + nuniqueSupportVectors);
    //IO.DisplayVector(oisSupportVector);
    //IO.DisplayVector(nsupportVectors);

    //now go over all SVM's and convert the instance index to the index
    //corresponding to the array of unique SV's
    for (int i = 0; i < m_Classifiers.length; i++) {
      if (! (m_Classifiers[i] instanceof SharedSVM)) {
        //m_Classifiers[i] wasn't trained properly, and it's probably a ZeroR classifier
        continue;
      }
      ( (SharedSVM) m_Classifiers[i]).convertIndicesToUniqueSVsCache(
          nsupportVectors);
    }

  }

  public double classifyInstance(Instance inst) throws Exception {
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
      SMO smo = (SMO) m_Classifier;
      for (int i = 0; i < m_funiqueSupportVectors.length; i++) {
        Instance sv = new Instance(1, m_funiqueSupportVectors[i]);
        m_dkernelValuesCache[i] = smo.calculateKernel(inst, sv);
      }
      m_osharedCacheWasFilled = true;
    }

    //call super class's classifyInstance, it's going to get the raw scores
    //from SVM's and do everything else. Note SVM's don't need instance
    //to get scores if we are using the cache
    return super.classifyInstance(inst);
  }

  public double[] getBinaryHardDecisions(Instance inst) throws Exception {
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
      SMO smo = (SMO) m_Classifier;
      for (int i = 0; i < m_funiqueSupportVectors.length; i++) {
        Instance sv = new Instance(1, m_funiqueSupportVectors[i]);
        m_dkernelValuesCache[i] = smo.calculateKernel(inst, sv);
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
                                           boolean oisBinary) throws
      IOException {

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
      for (int j = 0; j < data.length - 1; j++) {
        bufferedWriter.write(data[j] + " ");
      }
      int nclass = (int) data[data.length - 1];
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

  public int getNumberOfDistinctSupportVectors() {
    if (m_funiqueSupportVectors == null) {
      return -1;
    }
    else {
      return m_funiqueSupportVectors.length;
    }
  }

  public String numberOfSupportVectorsToString() {
    StringBuffer stringBuffer = new StringBuffer();
    for (int i = 0; i < m_nnumberOfBinaryClassifiers; i++) {
      if (m_Classifiers[i] != null && m_Classifiers[i] instanceof SharedSVM) {
        SharedSVM svm = (SharedSVM) m_Classifiers[i];
        stringBuffer.append(svm.getNumberOfSupportVectors() + " ");
      }
    }
    return stringBuffer.toString();
  }

  public Enumeration listOptions() {
    Vector vec = new Vector(10);
    vec.addElement(new Option(
        "\tTurn on global normalization (allows using a shared cache " +
        "to speed up computations, but performance may improve if each binary classifier has its own normalization factor",
        "G", 0, "-G"));
    vec.addElement(new Option("\tThere are no missing attributes.\n"
                              + "\t(If this flag is set, training and test will go faster, but if there are missing attributes, training will fail).",
                              "M", 0, "-M"));
    vec.addElement(new Option("\tUse SVMTorch instead of Weka's SMO.\n",
                              "H", 0, "-H"));
    vec.addElement(new Option(
        "\tUse cross-validation to find the best SVM parameters.\n",
        "K", 0, "-K"));
    vec.addElement(new Option("\tSets number of folds for cross-validation when doing model selection with option -K (default: 10).",
                              "X", 1, "-X"));
    vec.addElement(new Option(
        "\tSets number of examples per class for a filter.\n",
        "I", 1, "-I"));
    vec.addElement(new Option(
        "\tOptions for the filter, between quotation marks.\n",
        "J", 1, "-J"));
    vec.addElement(new Option("\tFile to use for model selection (CV).\n",
                              "Q", 1, "-Q"));
    Enumeration enume = super.listOptions();
    while (enume.hasMoreElements()) {
      vec.addElement(enume.nextElement());
    }
    return vec.elements();
  }

  public void setOptions(String[] options) throws Exception {
    //System.out.println(Utils.joinOptions(options));

    setGlobalNormalization(Utils.getFlag('G', options));
    setareThereMissingAttributes(!Utils.getFlag('M', options));
    setUseSVMTorch(Utils.getFlag('H', options));

    setFindSVMParametersThroughCrossValidation(Utils.getFlag('K', options));
    String testSet = Utils.getOption('Q', options);
    if (!testSet.equals("")) {
      //I'll use the -Q to get a file for ProbabilityMulti..., so disable below
      //if (!getFindSVMParametersThroughCrossValidation()) {
      //	throw new Exception("Cannot use option -Q without -K");
      //}
      setTestDatasetForModelSelectionFileName(testSet);
    }
    else {
      setTestDatasetForModelSelectionFileName(null);
    }

    //equivalente of option E for filters (but here, E is used for describing the ECOC matrix)
    m_evaluationClassOptions = Utils.getOption('U', options);
    m_searchClassOptions = Utils.getOption('S', options);
    if (m_searchClassOptions.equals("") && m_evaluationClassOptions.equals("")) {
      m_ouseAttributeSelection = false;
    }
    else {
      m_ouseAttributeSelection = true;
    }
    String temp = Utils.getOption('X', options);
    if (temp.equals("")) {
      setNumberOfFoldsForCrossValidationWhenFindingSVMParameters(10);
    }
    else {

      setNumberOfFoldsForCrossValidationWhenFindingSVMParameters(Integer.
          parseInt(temp));
    }
    temp = Utils.getOption('I', options);
    if (!temp.equals("")) {
      m_nfinalNumberOfExamplesPerClassForGMMFilter = Integer.parseInt(temp);
    }
    m_optionsForGMMThatFiltersExamplesForSVM = Utils.getOption('J', options);

    if (temp.equals("") && m_optionsForGMMThatFiltersExamplesForSVM.equals("")) {
      m_ofilterExamplesForSVMUsingGMM = false;
    }
    else {
      m_ofilterExamplesForSVMUsingGMM = true;
    }

    super.setOptions(options);
  }

  public String[] getOptions() {
    String[] superOptions = super.getOptions();
    String[] options = new String[superOptions.length + 10];
    int ncurrent = 0;
    if (getGlobalNormalization()) {
      options[ncurrent++] = "-G";
    }
    if (!getareThereMissingAttributes()) {
      options[ncurrent++] = "-M";
    }
    if (!getUseSVMTorch()) {
      options[ncurrent++] = "-H";
    }
    if (getFindSVMParametersThroughCrossValidation()) {
      options[ncurrent++] = "-K";
      options[ncurrent++] = "-X";
      options[ncurrent++] = "" +
          getNumberOfFoldsForCrossValidationWhenFindingSVMParameters();

    }
    if (m_ofilterExamplesForSVMUsingGMM) {
      options[ncurrent++] = "-I";
      options[ncurrent++] = "" + m_nfinalNumberOfExamplesPerClassForGMMFilter;
      options[ncurrent++] = "-J";
      options[ncurrent++] = m_optionsForGMMThatFiltersExamplesForSVM;
    }
    if (ncurrent > 0) {
      System.arraycopy(superOptions, 0, options, ncurrent, superOptions.length);
      for (int i = (ncurrent + superOptions.length); i < options.length; i++) {
        if (options[i] == null) {
          options[i] = "";
        }
      }
      return options;
    }
    else {
      //there are only options from super class
      return superOptions;
    }
  }

  public void setUseSVMTorch(boolean ouseSVMTorch) {
    m_ouseSVMTorch = ouseSVMTorch;
  }

  public boolean getUseSVMTorch() {
    return m_ouseSVMTorch;
  }

  public void setGlobalNormalization(boolean odoGlobalNormalization) {
    m_odoGlobalNormalization = odoGlobalNormalization;
  }

  public boolean getGlobalNormalization() {
    return m_odoGlobalNormalization;
  }

  public void setFindSVMParametersThroughCrossValidation(boolean
      ofindSVMParametersThroughCrossValidation) {
    m_ofindSVMParametersThroughCrossValidation =
        ofindSVMParametersThroughCrossValidation;
  }

  public boolean getFindSVMParametersThroughCrossValidation() {
    return m_ofindSVMParametersThroughCrossValidation;
  }

  protected boolean canUseSharedCache() {
    if (m_ouseAttributeSelection) {
      //each binary classifier can have different set of attributes,
      //so cannot use shared cache as it is implemented now
      return false;
    }
    if (m_odoGlobalNormalization) {
      //in this case, we don't allow individual normalization of SVM's
      return true;
    }
    else {
      //can use cache only if SVM's are not normalized
      if (m_Normalize) {
        return false;
      }
      else {
        return true;
      }
    }
  }

  public String toString() {
    if (m_ouseAttributeSelection) {
      StringBuffer stringBuffer = new StringBuffer(super.toString());
      if (m_nselectedAttributes.length == m_instancesHeader.numAttributes()) {
        stringBuffer.append("\nAll attributes were selected.");
      }
      else {
        stringBuffer.append("\nTotal of " + m_nselectedAttributes.length +
            " distinct attributes selected (counting with class). Their indices: ");
        stringBuffer.append(convertIndicesToString(m_nselectedAttributes));
      }
      stringBuffer.append("\nHistogram of selected attributes: ");
      stringBuffer.append(convertIndicesToString(m_natributeOccurrences));
      return stringBuffer.toString();
    }
    else {
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

  protected class SharedSVM
      extends Classifier
      implements Cloneable, RawScorer {

    //static final long serialVersionUID = -6020644217154427429L;

    private double m_db;
    private double[] m_dalphas;
    private double[] m_dweights;
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
      }
      else {
        return m_nsupportVectorIndices.length;
      }
    }

    private void buildClassifierUsingTorch(Instances instances, SMO smo) throws
        Exception {
      //write to file
      boolean oisBinaryProblem = true;
      //String torchInputFileName = "temp_temp.torch";
      String torchInputFileName = File.createTempFile("tmp-torch", ".data").
          getAbsolutePath();

      String torchOutputModel = m_outputModelFileName;
      //if (m_odebug) {
      //	Print.dialog("Writing " + torchInputFileName);
      //}
      writeInSVMTorchFormat(instances, torchInputFileName, oisBinaryProblem);

      SVMOptions sVMOptions = SVMOptions.getSVMOptions(smo);
      String command = "AKSVMTorch " + sVMOptions.toTorch() + " " +
          torchInputFileName +
          " " + torchOutputModel;

      //faster processing option
      //if m_ofindSVMParametersThroughCrossValidation is true, don't print Torch's output
      //because there are too many SVM to train
      new RunExternalProgram(command,
                             m_odebug &&
                             !m_ofindSVMParametersThroughCrossValidation);

      //read ASCII file
      BufferedReader bufferedReader = null;
      bufferedReader = IO.openBufferedReader(torchOutputModel);
      String t = null;
      try {
        for (int i = 0; i < 4; i++) {
          t = bufferedReader.readLine();
          if (t == null) {
            throw new Exception("Couldn't read file " + torchOutputModel);
          }
          if (!t.startsWith("#")) {
            throw new Exception("Error parsing header:\n" + t);
          }
        }
        try {
          m_db = -1.0 * Double.parseDouble(bufferedReader.readLine());
        }
        catch (NumberFormatException ex) {
          System.err.println("Warning: Error parsing the value of threshold B "+
                             "(probably nan). Assuming value zero.");
          m_db = 0;
        }

        int nnumberOfSupportVectors = Integer.parseInt(bufferedReader.readLine());
        m_nsupportVectorOriginalIndices = new int[nnumberOfSupportVectors];
        m_dalphas = new double[nnumberOfSupportVectors];
        int ncounter = 0;
        while ( (t = bufferedReader.readLine()) != null) {
          StringTokenizer stringTokenizer = new StringTokenizer(t);
          if (stringTokenizer.countTokens() != 2) {
            throw new Exception("Error parsing line:\n" + t);
          }
          m_dalphas[ncounter] = Double.parseDouble(stringTokenizer.nextToken());
          m_nsupportVectorOriginalIndices[ncounter] = Integer.parseInt(
              stringTokenizer.nextToken());
          ncounter++;
        }
        if (ncounter != nnumberOfSupportVectors) {
          throw new Exception("File indicated there were " +
                              nnumberOfSupportVectors +
                              " support vectors, but only " + ncounter +
                              " were found !");
        }
        bufferedReader.close();
      }
      catch (IOException ex) {
        System.err.println(ex.getMessage());
        throw new Exception("Problem reading line\n" + t + "\nof file " +
                            torchOutputModel + " generated by " +
                            "command line:\n" + command);
      }

      //compose weights for linear SVM
      int nspaceDimension = instances.numAttributes() - 1;
      //SMO doesn't subtract class
      m_dweights = new double[nspaceDimension + 1];
      if (m_oisLinearMachine) {
        int nnumberOfSupportVectors = m_nsupportVectorOriginalIndices.length;
        for (int j = 0; j < nnumberOfSupportVectors; j++) {
          float[] x = instances.instance(m_nsupportVectorOriginalIndices[j]).
              getAttributesReference();
          for (int i = 0; i < nspaceDimension; i++) {
            m_dweights[i] += m_dalphas[j] * x[i];
          }
        }
      }

      //here I should delete the files
      new File(torchInputFileName).delete();
      new File(m_outputModelFileName).delete();
    }

    /**
     * Builds the classifiers.
     *
     * @param insts the training data.
     * @exception Exception if a classifier can't be built
     */
    public void buildClassifier(Instances instances) throws Exception {
      if (m_nsupportVectorsMap == null) {
        throw new Exception("Support vectors map not set!");
      }

      //use m_Classifier to get options
      SMO smo = (SMO) Classifier.makeCopy(m_Classifier);
      //I am doing normalization here, so turn off inside SMO
      smo.setNormalizeData(false);

      if (m_odoGlobalNormalization) {
        //we don't normalize here, as it was done globally before
        m_normalizationFilter = null;
      }
      else {
        instances = setNormalizationFilterForGivenClassifierAndFilter(instances);
      }

      if (m_ouseAttributeSelection) {
        m_attributeSelectionFilter = new AttributeSelectionFilter();
        String[] options = new String[4];
        options[0] = "-S";
        options[1] = m_searchClassOptions;
        options[2] = "-E";
        options[3] = m_evaluationClassOptions;

        ( (OptionHandler) m_attributeSelectionFilter).setOptions(options);
        m_attributeSelectionFilter.setInputFormat(instances);
        instances = Filter.useFilter(instances, m_attributeSelectionFilter);
        //System.out.println( Utils.joinOptions( ((OptionHandler) m_attributeSelectionFilter).getOptions() ));
        //System.out.println( ((AttributeSelectionFilter) m_attributeSelectionFilter).getOutputFormat().toSummaryString());
      }

      if (m_ouseSVMTorch) {
        //System.out.println("TORCH");
        //call Torch
        buildClassifierUsingTorch(instances, smo);
        //use m_nsupportVectorsMap to map m_nsupportVectorOriginalIndices to indices in multiclass Instances
        for (int j = 0; j < m_nsupportVectorOriginalIndices.length; j++) {
          m_nsupportVectorOriginalIndices[j] = m_nsupportVectorsMap[
              m_nsupportVectorOriginalIndices[j]];
        }
        if (m_nsupportVectorOriginalIndices == null ||
            m_nsupportVectorOriginalIndices.length == 0) {
          //in fact, Torch returns m_nsupportVectorOriginalIndices.length==0 but I will make sure below:
          m_nsupportVectorOriginalIndices = new int[0];
          throw new ClassifierException();
        }
      }
      else {
        //System.out.println("WEKA");
        //use Weka's SMO
        smo.setSupportVectorsMap(m_nsupportVectorsMap);
        smo.buildClassifier(instances);

        m_nsupportVectorOriginalIndices = smo.getSupportVectorIndices();
        if (m_nsupportVectorOriginalIndices == null) {
          //"smo.getSupportVectorIndices() returned null ! This happened once when there were no examples of a class to train the SVM."
          m_nsupportVectorOriginalIndices = new int[0];
          throw new ClassifierException();
        }
        m_dalphas = smo.getAlphas();
        m_db = smo.getBias();
        if (m_oisLinearMachine) {
          m_dweights = Cloner.clone(smo.getLinearMachineWeightsReference());
        }
      }

      m_nsupportVectorIndices = new int[m_nsupportVectorOriginalIndices.length];

      //don't need it, save storage space
      m_nsupportVectorsMap = null;
    }

    //In fact, it doesn't use instance, but the cache that was previously filled up
    public double classifyInstance(Instance instance) throws Exception {
      if (true) {
        throw new Exception("Thought I would never call this method...");
      }
      return -1;
    }

    //called by super class ScoreMultiClassClassifier
    public double getRawScore(Instance instance) {
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
            result += m_dalphas[i] *
                m_dkernelValuesCache[m_nsupportVectorIndices[i]];
          }
          return result - m_db;
        }

        //cannot use general cache because each binary classifier has its own normalization
        if (m_normalizationFilter != null) {
          m_normalizationFilter.input(instance);
          m_normalizationFilter.batchFinished();
          instance = m_normalizationFilter.output();
        }
        else if (m_odoGlobalNormalization) {
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

        if (m_oisLinearMachine) {
          for (int i = 0; i < x.length - 1; i++) {
            result += m_dweights[i] * x[i];
          }
        }
        else {
          SMO smo = (SMO) m_Classifier;
          //here I have 4 cases: normalization (true or false) and attribute_selection (true or false)
          //to save computations I should have a switch case or if's outside the loop. Right now
          //m_ouseAttributeSelection is inside the loop
          if (m_normalizationFilter != null) {
            for (int i = 0; i < m_dalphas.length; i++) {
              //need to normalize support vector
              Instance sv = new Instance(1,
                                         m_funiqueSupportVectors[
                                         m_nsupportVectorIndices[
                                         i]]);
              sv.setDataset(m_instancesHeader);
              m_normalizationFilter.input(sv);
              m_normalizationFilter.batchFinished();
              sv = m_normalizationFilter.output();
              if (m_ouseAttributeSelection) {
                m_attributeSelectionFilter.input(sv);
                m_attributeSelectionFilter.batchFinished();
                sv = m_attributeSelectionFilter.output();
              }
              result += m_dalphas[i] * smo.calculateKernel(instance, sv);
            }
          }
          else {
            //don't need to filter
            for (int i = 0; i < m_dalphas.length; i++) {
              Instance sv = new Instance(1,
                                         m_funiqueSupportVectors[
                                         m_nsupportVectorIndices[
                                         i]]);
              if (m_ouseAttributeSelection) {
                m_attributeSelectionFilter.input(sv);
                m_attributeSelectionFilter.batchFinished();
                sv = m_attributeSelectionFilter.output();
              }
              result += m_dalphas[i] * smo.calculateKernel(instance, sv);
            }
          }
        }
        return result - m_db;
      }
      catch (Exception ex) {
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
    private void convertIndicesToUniqueSVsCache(int[]
        nindicesInArrayOfUniqueSupportVectors) {
      for (int i = 0; i < m_nsupportVectorIndices.length; i++) {
        m_nsupportVectorIndices[i] = nindicesInArrayOfUniqueSupportVectors[
            m_nsupportVectorOriginalIndices[i]];
      }
    }

    private Instances setNormalizationFilterForGivenClassifierAndFilter(
        Instances instances) throws Exception {
      if (m_Normalize) {
        if (m_ouseOriginalNormalization) {
          m_normalizationFilter = new NormalizationFilter();
        }
        else {
          //use 0 mean and 1 variance
          m_normalizationFilter = new StandardizeFilter();
        }
        m_normalizationFilter.setInputFormat(instances);
        return Filter.useFilter(instances, m_normalizationFilter);
      }
      else {
        m_normalizationFilter = null;
        //don't filter
        return instances;
      }
    }

    private SMO convertToSMO() {
      SMO smo = null;
      try {
        smo = (SMO) Classifier.makeCopy(m_Classifier);
      }
      catch (Exception e) {
        e.printStackTrace();
        return null;
      }
      smo.setSMO(m_db,
                 m_nsupportVectorOriginalIndices,
                 m_dalphas,
                 m_dweights,
                 m_oisLinearMachine,
                 m_instancesHeader);
      return smo;
    }

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
        return "SharedSVM: No model built yet.";
      }
      try {
        text.append("SharedSVM\n\n");
        Instances headerInstances = null;
        if (m_ouseAttributeSelection) {
          headerInstances = m_attributeSelectionFilter.getOutputFormat();
        }
        else {
          headerInstances = m_instancesHeader;
        }

        // If machine linear, print weight vector
        if (m_oisLinearMachine) {
          //print support vectors if machine is linear and their number is < 20
          if (getNumberOfSupportVectors() < 20) {
            text.append("Support vectors (training set indices): ");
            for (int i = 0; i < m_nsupportVectorOriginalIndices.length; i++) {
              text.append(" " + m_nsupportVectorOriginalIndices[i]);
            }
            text.append("\n");
          }
          text.append("Machine linear: showing attribute weights, ");
          text.append("not support vectors.\n\n");
          text.append("   ");
          text.append(m_dweights[0] + " * " + headerInstances.attribute(0).name() +
                      "\n");
          //subtract 1 due to class' attribute
          for (int i = 1; i < m_dweights.length - 1; i++) {
            text.append(" + ");
            text.append(m_dweights[i] + " * " +
                        headerInstances.attribute(i).name() + "\n");
          }
        }
        else {
          //machine is not linear
          text.append("   " + m_dalphas[0] + " * K[X(" +
                      m_nsupportVectorOriginalIndices[0] + ") * X]\n");
          for (int i = 1; i < m_dalphas.length; i++) {
            text.append(" + ");
            text.append(m_dalphas[i] + " * K[X(" +
                        m_nsupportVectorOriginalIndices[i] + ") * X]\n");
          }
        }
        text.append(" - " + m_db + "\n");
        text.append("\nNumber of support vectors: " + getNumberOfSupportVectors() +
                    "\n");
        if (m_ouseAttributeSelection) {
          text.append("Attributes: " +
                      convertIndicesToString(this.
                                             getIndicesOfSelectedAttributes()) +
                      "\n");
        }
        String[] options = ( (SMO) m_Classifier).getOptions();
        for (int i = 0; i < options.length; i++) {
          text.append(options[i] + " ");
        }
        text.append("\n");
      }
      catch (Exception e) {
        e.printStackTrace();
        return "Can't print SharedSVM classifier.";
      }

      return text.toString();
    }

    public int[] getIndicesOfSelectedAttributes() {
      if (m_ouseAttributeSelection) {
        return ( (AttributeSelectionFilter) m_attributeSelectionFilter).
            getIndicesOfSelectedAttributes();
      }
      else {
        return null;
      }
    }

  } // end of inner class

  private static class ClassifierException
      extends Exception {
  }

  public double getRawScore(Instance inst) {
    if (m_Classifiers.length != 1) {
      new Exception(
          "Cannot use SVM.getRawScore() with more than 1 binary classifier").
          printStackTrace();
    }
    m_osharedCacheWasFilled = false;

    //bogus: the SharedSVM does the global filtering (which is inefficient,
    //given that it's done for all binary classifier, while "global")
    //but I didn't see other filters...
    try {

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

//				if (m_odoGlobalNormalization) {
//					m_globalNormalizationFilter.input(inst);
//					m_globalNormalizationFilter.batchFinished();
//					inst = m_globalNormalizationFilter.output();
//				}
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    return ( (RawScorer) m_Classifiers[0]).getRawScore(inst);
  }

  public String getTestDatasetForModelSelectionFileName() {
    return m_testDatasetForModelSelectionFileName;
  }

  public void setTestDatasetForModelSelectionFileName(String
      testDatasetForModelSelectionFileName) {
    m_testDatasetForModelSelectionFileName =
        testDatasetForModelSelectionFileName;
  }

  /**
   * A class should call this method before calling buildClassifier() in
   * case it wants to use the testInstances to find the best parameters.
   */
  public void setTestInstanceForModelSelectionFileName(Instances testInstaces) {
    m_testInstanceForModelSelection = testInstaces;
  }

  public static void main(String[] args) throws Exception {
    //test (args);
    Classifier scheme;
    try {
      scheme = new SVM();
      System.out.println(Evaluation.evaluateModel(scheme, args));

      SVM svm = (SVM) scheme;
      if (svm.getDebug()) {
        //creates problem when using -p 0 for McNemar's test
        System.out.println("Number of support vectors per binary classifier: " +
                           svm.numberOfSupportVectorsToString());
        System.out.println("Number of distinct support vectors: " +
                           svm.getNumberOfDistinctSupportVectors());
      }
    }
    catch (Exception e) {
      System.err.println(e.getMessage());
      e.printStackTrace();
    }
  }

}
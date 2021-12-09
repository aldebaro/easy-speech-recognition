/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 *    NaiveBayes.java
 *    Copyright (C) 1999 Eibe Frank,Len Trigg
 *
 */

package weka.classifiers;

import java.io.*;
import java.util.*;
import weka.core.*;
import weka.estimators.*;

/**
 * Class for a Naive Bayes classifier using estimator classes. Numeric 
 * estimator precision values are chosen based on analysis of the 
 * training data. For this reason, the classifier is not an 
 * UpdateableClassifier (which in typical usage are initialized with zero 
 * training instances) -- if you need the UpdateableClassifier functionality,
 * Create an empty class such as the following: <p>
 *
 * <pre><code>
 * public class NaiveBayesUpdateable extends NaiveBayes 
 *     implements UpdateableClassifier {
 *
 * }
 * </code></pre>
 * This classifier will use a default precision of 0.1 for numeric attributes
 * when buildClassifier is called with zero training instances.
 * <p>
 * For more information on Naive Bayes classifiers, see<p>
 *
 * George H. John and Pat Langley (1995). <i>Estimating
 * Continuous Distributions in Bayesian Classifiers</i>. Proceedings
 * of the Eleventh Conference on Uncertainty in Artificial
 * Intelligence. pp. 338-345. Morgan Kaufmann, San Mateo.<p>
 *
 * Valid options are:<p>
 *
 * -K <br>
 * Use kernel estimation for modelling numeric attributes rather than
 * a single normal distribution.<p>
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 2 $
 */
public class NaiveBayes extends DistributionClassifier 
  implements OptionHandler, WeightedInstancesHandler {

  /** The attribute estimators. */
  protected Estimator [][] m_Distributions;
  
  /** The class estimator. */
  protected Estimator m_ClassDistribution;

  /**
   * Whether to use kernel density estimator rather than normal distribution
   * for numeric attributes
   */
  protected boolean m_UseKernelEstimator;

  /** The number of classes (or 1 for numeric class) */
  protected int m_NumClasses;

  /**
   * The dataset header for the purposes of printing out a semi-intelligible 
   * model 
   */
  protected Instances m_Instances;

  /*** The precision parameter used for numeric attributes */
  protected static final double DEFAULT_NUM_PRECISION = 0.01;

  /**
   * Generates the classifier.
   *
   * @param instances set of instances serving as training data 
   * @exception Exception if the classifier has not been generated 
   * successfully
   */
  public void buildClassifier(Instances instances) throws Exception {

    if (instances.checkForStringAttributes()) {
      throw new Exception("Can't handle string attributes!");
    }
    if (instances.classAttribute().isNumeric()) {
      throw new Exception("Naive Bayes: Class is numeric!");
    }
    m_NumClasses = instances.numClasses();
    if (m_NumClasses < 0) {
      throw new Exception ("Dataset has no class attribute");
    }

    // Copy the instances
    m_Instances = new Instances(instances);

    // Reserve space for the distributions
    m_Distributions = new Estimator[m_Instances.numAttributes() - 1]
    [m_Instances.numClasses()];
    m_ClassDistribution = new DiscreteEstimator(m_Instances.numClasses(), 
						true);
    int attIndex = 0;
    Enumeration enume = m_Instances.enumerateAttributes();
    while (enume.hasMoreElements()) {
      Attribute attribute = (Attribute) enume.nextElement();

      // If the attribute is numeric, determine the estimator 
      // numeric precision from differences between adjacent values
      double numPrecision = DEFAULT_NUM_PRECISION;
      if (attribute.type() == Attribute.NUMERIC) {
	m_Instances.sort(attribute);
	if ((m_Instances.numInstances() > 0)
	    && !m_Instances.instance(0).isMissing(attribute)) {
	  double lastVal = m_Instances.instance(0).value(attribute);
	  double currentVal, deltaSum = 0;
	  int distinct = 0;
	  for (int i = 1; i < m_Instances.numInstances(); i++) {
	    Instance currentInst = m_Instances.instance(i);
	    if (currentInst.isMissing(attribute)) {
	      break;
	    }
	    currentVal = currentInst.value(attribute);
	    if (currentVal != lastVal) {
	      deltaSum += currentVal - lastVal;
	      lastVal = currentVal;
	      distinct++;
	    }
	  }
	  if (distinct > 0) {
	    numPrecision = deltaSum / distinct;
	  }
	}
      }


      for (int j = 0; j < m_Instances.numClasses(); j++) {
	switch (attribute.type()) {
	case Attribute.NUMERIC: 
	  if (m_UseKernelEstimator) {
	    m_Distributions[attIndex][j] = 
	    new KernelEstimator(numPrecision);
	  } else {
	    m_Distributions[attIndex][j] = 
	    new NormalEstimator(numPrecision);
	  }
	  break;
	case Attribute.NOMINAL:
	  m_Distributions[attIndex][j] = 
	  new DiscreteEstimator(attribute.numValues(), true);
	  break;
	default:
	  throw new Exception("Attribute type unknown to NaiveBayes");
	}
      }
      attIndex++;
    }

    // Compute counts
    Enumeration enumInsts = m_Instances.enumerateInstances();
    while (enumInsts.hasMoreElements()) {
      Instance instance = 
	(Instance) enumInsts.nextElement();
      updateClassifier(instance);
    }

    // Save space
    m_Instances = new Instances(m_Instances, 0);
  }


  /**
   * Updates the classifier with the given instance.
   *
   * @param instance the new training instance to include in the model 
   * @exception Exception if the instance could not be incorporated in
   * the model.
   */
  public void updateClassifier(Instance instance) throws Exception {

    if (!instance.classIsMissing()) {
      Enumeration enumAtts = m_Instances.enumerateAttributes();
      int attIndex = 0;
      while (enumAtts.hasMoreElements()) {
	Attribute attribute = (Attribute) enumAtts.nextElement();
	if (!instance.isMissing(attribute)) {
	  m_Distributions[attIndex][(int)instance.classValue()].
	    addValue(instance.value(attribute), instance.weight());
	}
	attIndex++;
      }
      m_ClassDistribution.addValue(instance.classValue(),
				   instance.weight());
    }
  }


  /**
   * Calculates the class membership probabilities for the given test 
   * instance.
   *
   * @param instance the instance to be classified
   * @return predicted class probability distribution
   * @exception Exception if there is a problem generating the prediction
   */
  public double [] distributionForInstance(Instance instance) 
  throws Exception { 
    
    double [] probs = new double[m_NumClasses];
    for (int j = 0; j < m_NumClasses; j++) {
      probs[j] = m_ClassDistribution.getProbability(j);
    }
    Enumeration enumAtts = instance.enumerateAttributes();
    int attIndex = 0;
    while (enumAtts.hasMoreElements()) {
      Attribute attribute = (Attribute) enumAtts.nextElement();
      if (!instance.isMissing(attribute)) {
	double temp, max = 0;
	for (int j = 0; j < m_NumClasses; j++) {
	  temp = Math.max(1e-75, m_Distributions[attIndex][j].
	  getProbability(instance.value(attribute)));
	  probs[j] *= temp;
	  if (probs[j] > max) {
	    max = probs[j];
	  }
	  if (Double.isNaN(probs[j])) {
	    throw new Exception("NaN returned from estimator for attribute "
				+ attribute.name() + ":\n"
				+ m_Distributions[attIndex][j].toString());
	  }
	}
	if ((max > 0) && (max < 1e-75)) { // Danger of probability underflow
	  for (int j = 0; j < m_NumClasses; j++) {
	    probs[j] *= 1e75;
	  }
	}
      }
      attIndex++;
    }

    // Display probabilities
    Utils.normalize(probs);
    return probs;
  }

  /**
   * Returns an enumeration describing the available options
   *
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(1);

    newVector.addElement(
    new Option("\tUse kernel density estimator rather than normal\n"
	       +"\tdistribution for numeric attributes",
	       "K", 0,"-K"));
    return newVector.elements();
  }

  /**
   * Parses a given list of options. Valid options are:<p>
   *
   * -K <br>
   * Use kernel estimation for modelling numeric attributes rather than
   * a single normal distribution.<p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    
    m_UseKernelEstimator = Utils.getFlag('K', options);

    Utils.checkForRemainingOptions(options);
  }

  /**
   * Gets the current settings of the classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] options = new String [1];
    int current = 0;

    if (m_UseKernelEstimator) {
      options[current++] = "-K";
    }

    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Returns a description of the classifier.
   *
   * @return a description of the classifier as a string.
   */
  public String toString() {
    
    StringBuffer text = new StringBuffer();

    text.append("Naive Bayes Classifier");
    if (m_Instances == null) {
      text.append(": No model built yet.");
    } else {
      try {
	for (int i = 0; i < m_Distributions[0].length; i++) {
	  text.append("\n\nClass " + m_Instances.classAttribute().value(i) +
		      ": Prior probability = " + Utils.
		      doubleToString(m_ClassDistribution.getProbability(i),
				     4, 2) + "\n\n");
	  Enumeration enumAtts = m_Instances.enumerateAttributes();
	  int attIndex = 0;
	  while (enumAtts.hasMoreElements()) {
	    Attribute attribute = (Attribute) enumAtts.nextElement();
	    text.append(attribute.name() + ":  " 
			+ m_Distributions[attIndex][i]);
	    attIndex++;
	  }
	}
      } catch (Exception ex) {
	text.append(ex.getMessage());
      }
    }

    return text.toString();
  }
  
  /**
   * Gets if kernel estimator is being used.
   *
   * @return Value of m_UseKernelEstimatory.
   */
  public boolean getUseKernelEstimator() {
    
    return m_UseKernelEstimator;
  }
  
  /**
   * Sets if kernel estimator is to be used.
   *
   * @param v  Value to assign to m_UseKernelEstimatory.
   */
  public void setUseKernelEstimator(boolean v) {
    
    m_UseKernelEstimator = v;
  }

  /**
   * Main method for testing this class.
   *
   * @param argv the options
   */
  public static void main(String [] argv) {

    try {
      System.out.println(Evaluation.evaluateModel(new NaiveBayes(), argv));
    } catch (Exception e) {
      e.printStackTrace();
      System.err.println(e.getMessage());
    }
  }
}













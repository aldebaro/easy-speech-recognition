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
 *    ZeroR.java
 *    Copyright (C) 1999 Eibe Frank
 *
 */

package weka.classifiers;

import java.io.*;
import java.util.*;
import weka.core.*;

/**
 * Class for building and using a 0-R classifier. Predicts the mean
 * (for a numeric class) or the mode (for a nominal class).
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 2 $
 */
public class ZeroR extends DistributionClassifier
	implements WeightedInstancesHandler, RawScorer {

	/** The class value 0R predicts. */
	private double m_ClassValue;

	/** The number of instances in each class (null if class numeric). */
	private double [] m_Counts;

	/** The class attribute. */
	private Attribute m_Class;

	private boolean m_obinaryWithoutNegativeExamples;
	private boolean m_obinaryWithoutPositiveExamples;
	private static final double m_dscoreWhenThereWereNoTrainingExamples = 10000;

	/**
	 * Generates the classifier.
	 *
	 * @param instances set of instances serving as training data
	 * @exception Exception if the classifier has not been generated successfully
	 */
	public void buildClassifier(Instances instances) throws Exception {

		m_Class = instances.classAttribute();
		m_ClassValue = 0;
		switch (instances.classAttribute().type()) {
		case Attribute.NUMERIC:
			m_Counts = null;
			break;
		case Attribute.NOMINAL:
			m_Counts = new double [instances.numClasses()];
			for (int i = 0; i < m_Counts.length; i++) {
	m_Counts[i] = 1;
			}
			break;
		default:
			throw new Exception("ZeroR can only handle nominal and numeric class"
				+ " attributes.");
		}

		Enumeration enume = instances.enumerateInstances();
		while (enume.hasMoreElements()) {
			Instance instance = (Instance) enume.nextElement();
			if (!instance.classIsMissing()) {
	if (instances.classAttribute().isNominal()) {
		m_Counts[(int)instance.classValue()] += instance.weight();
	} else {
		m_ClassValue += instance.weight() * instance.classValue();
	}
			}
		}

		//check before normalizing
		m_obinaryWithoutNegativeExamples = false;
		m_obinaryWithoutPositiveExamples = false;
		if (instances.numClasses() == 2) {
			//binary problem
			if (m_Counts[0] == 1) {
				m_obinaryWithoutNegativeExamples = true;
			}
			if (m_Counts[1] == 1) {
				m_obinaryWithoutPositiveExamples = true;
			}
		}

		if (instances.classAttribute().isNumeric()) {
			if (Utils.gr(instances.sumOfWeights(), 0)) {
	m_ClassValue /= instances.sumOfWeights();
			}
		} else {
			m_ClassValue = Utils.maxIndex(m_Counts);
			Utils.normalize(m_Counts);
		}

//		System.out.println(instances.toString());
//		System.out.println(instances.toSummaryString());
//		System.out.println("m_ClassValue = " + m_ClassValue);
//		System.out.println("m_Counts[1]  m_Counts[0]" + m_Counts[1] + " " + m_Counts[0]);
//		System.exit(1);
	}

	/**
	 * Classifies a given instance.
	 *
	 * @param instance the instance to be classified
	 * @return index of the predicted class
	 */
	public double classifyInstance(Instance instance) {

		return m_ClassValue;
	}

	//convention is positive class is index 1, and negative index 0
	//update only if positive
	//should be called only if problem is binary
	public double getRawScore(Instance instance) {
		if (m_Counts == null) {
			new Exception().printStackTrace();
			System.err.println("method ZeroR.getRawScore() was called with m_Counts == null!");
			System.exit(1);
			//ak: not sure:
			//System.out.println("1) m_ClassValue = " + m_ClassValue);
			return (m_ClassValue == 0) ? -1 : 1;
		} else {
			if (m_obinaryWithoutNegativeExamples && !m_obinaryWithoutPositiveExamples) {
				//there were no - but there were + examples, so return a large positive number
				return m_dscoreWhenThereWereNoTrainingExamples;
			}
			if (!m_obinaryWithoutNegativeExamples && m_obinaryWithoutPositiveExamples) {
				//return a negative number with large magniture
				return -m_dscoreWhenThereWereNoTrainingExamples;
			}
			//if both m_obinaryWithoutNegativeExamples && m_obinaryWithoutPositiveExamples
			//are true, return 0 as below
			//m_Counts is a distribution. Make score [-1, 1]
			//System.out.println("2) m_Counts[1] - m_Counts[0] = " + (m_Counts[1] - m_Counts[0]));
			return m_Counts[1] - m_Counts[0];
		}
	}

	/**
	 * Calculates the class membership probabilities for the given test instance.
	 *
	 * @param instance the instance to be classified
	 * @return predicted class probability distribution
	 * @exception Exception if class is numeric
	 */
	public double [] distributionForInstance(Instance instance)
			 throws Exception {

		if (m_Counts == null) {
			double[] result = new double[1];
			result[0] = m_ClassValue;
			return result;
		} else {
			return (double []) m_Counts.clone();
		}
	}

	/**
	 * Returns a description of the classifier.
	 *
	 * @return a description of the classifier as a string.
	 */
	public String toString() {

		if (m_Class ==  null) {
			return "ZeroR: No model built yet.";
		}
		if (m_Counts == null) {
			return "ZeroR predicts class value: " + m_ClassValue;
		} else {
			return "ZeroR predicts class value: " + m_Class.value((int) m_ClassValue);
		}
	}

	/**
	 * Main method for testing this class.
	 *
	 * @param argv the options
	 */
	public static void main(String [] argv) {

		try {
			System.out.println(Evaluation.evaluateModel(new ZeroR(), argv));
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}
}



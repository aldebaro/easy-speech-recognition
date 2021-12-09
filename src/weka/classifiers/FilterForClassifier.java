package weka.classifiers;

import java.util.*;
import weka.core.*;
import weka.filters.*;
import java.io.Serializable;

/**
 * Title:        Spock
 * Description:  Speech recognition
 * Copyright:    Copyright (c) 2001
 * Company:      UCSD
 * @author       Aldebaro Klautau
 * @version 4.0
 */

//assume class is last attribute
public class FilterForClassifier implements Serializable {

	/** The filter used to make attributes numeric. */
	protected NominalToBinaryFilter m_NominalToBinary;

	/** The filter used to normalize all values. */
	protected Filter m_Normalization;
	//private NormalizationFilter m_Normalization;

	/** The filter used to get rid of missing values. */
	protected ReplaceMissingValuesFilter m_Missing;

	protected boolean m_oareThereMissingAttributes = true;

	/** True if we want to normalize */
	protected boolean m_Normalize = true;

	/** Normalization type: original used [0, 1] and new G(0,1) */
	protected boolean m_ouseOriginalNormalization = true;

	/** Only numeric attributes in the dataset? */
	protected boolean m_onlyNumeric;

	/** Indicates if we need to do any sort of filtering (if not, we can save computations) */
	protected boolean m_ouseSomeFilter = true;

	protected Instances setFiltering (Instances inputData) throws Exception {
		m_onlyNumeric = true;
		//assume class is last attribute
		for (int i = 0; i < inputData.numAttributes()-1; i++) {
			if (!inputData.attribute(i).isNumeric()) {
				m_onlyNumeric = false;
				break;
			}
		}

		if (m_oareThereMissingAttributes) {
			//m_oareThereMissingAttributes is specified by the user. If the user
			//sets it to false, we don't check for missing attributes, but if it's true,
			//we check, to avoid the filter if in fact there are no missing attributes
			m_oareThereMissingAttributes = areThereMissingAttributes(inputData);
		}

		//check if we need to do any sort of filtering that would change
		//the original input instances
		m_ouseSomeFilter = !m_onlyNumeric || m_oareThereMissingAttributes || m_Normalize;

		Instances instances = null;
		if (m_ouseSomeFilter) {
			//need to take a copy because inputData will be modified
			instances = new Instances(inputData);
			if (m_oareThereMissingAttributes) {
				m_Missing = new ReplaceMissingValuesFilter();
				m_Missing.setInputFormat(instances);
				instances = Filter.useFilter(instances, m_Missing);
			} else {
				m_oareThereMissingAttributes = false;
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
		} else {
			m_NominalToBinary = null;
			m_Normalization = null;
			m_Missing = null;
			instances = inputData;
		}
		return instances;
 }

 public static boolean areThereMissingAttributes(Instances data) {
		Enumeration enumAtt = data.enumerateAttributes();
		while (enumAtt.hasMoreElements()) {
			Attribute attr = (Attribute)enumAtt.nextElement();
			//      if (!attr.isNominal()) {
			//        throw new Exception("GaussiansMixture: all attributes are numeric?? The last one should be nominal, please.");
			//      }
			Enumeration enume = data.enumerateInstances();
			while (enume.hasMoreElements()) {
				if (((Instance)enume.nextElement()).isMissing(attr)) {
					return true;
				}
			}
		}
		return false;
 }

	protected Instance filterIfNeeded (Instance inst) throws Exception {
		if (!m_ouseSomeFilter) {
			return inst;
		}
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
			System.err.println("Problem filtering instance!");
			e.printStackTrace();
		}
		return inst;
	}

	public boolean getareThereMissingAttributes() {
		return m_oareThereMissingAttributes;
	}

	public void setareThereMissingAttributes(boolean oareThereMissingAttributes) {
		m_oareThereMissingAttributes = oareThereMissingAttributes;
	}

	public boolean getUseSomeFilter() {
		return m_ouseSomeFilter;
	}

}
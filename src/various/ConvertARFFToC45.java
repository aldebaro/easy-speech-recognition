package various;

import java.util.*;
import java.io.*;
import weka.core.*;
import edu.ucsd.asr.*;

/**
 * Title:        Spock
 * Description:  Speech recognition
 * Copyright:    Copyright (c) 2001
 * Company:      UCSD
 * @author       Aldebaro Klautau
 * @version 4.0
 *
 * I'm not sure if C4.5 has a dot after all examples.
 *
 * Ripper (Cohen, 95) doesn't parse correctly attributes that
 * start with a number!
 *
 * So, this class in fact writes files in Ripper's format.
 * Because I think that C4.5 is more robust in terms of parsing.
 */

public class ConvertARFFToC45 {

	private final static boolean m_oaddDot = true;

	public static void main(String[] args) throws Exception {

		if (args.length != 1) {
			System.out.println("Usage: a) <arff> or b) <prefix> , like soybean for soybean_train and soybean_test");
			System.out.println("Output: a) <header> <data>");
			System.out.println("Output: b) <header> <data> <test>");
			System.out.println("Obs: replace with sed, so can be done only on Unix!");
			System.out.println("Example: a) adult.arff is converted to adult.names and adult.data");
			System.out.println("Example: b) soybean is converted to soybean.names, soybean.data and soybean.test");
			System.exit(1);
		}

		boolean oonlyTrain = false;
		if (args[0].trim().toLowerCase().endsWith("arff")) {
			oonlyTrain = true;
		}

		//boolean oreplaceMinuses = Boolean.valueOf(args[1]).booleanValue();

		String data = null;
		if (oonlyTrain) {
			data = args[0];
		} else {
			data = args[0] + "_train.arff";
		}
		Instances instances = new Instances(data);
		renameAttributes(instances);

		String[] header = getHeader(instances);
		int natt = header.length;
		Vector headerVector = new Vector(natt);
		headerVector.add(header[0]);
		for (int i = 1; i < natt; i++) {
			headerVector.add(header[i]);
		}
		String headerFile = null;
		if (oonlyTrain) {
			headerFile = FileNamesAndDirectories.substituteExtension(data,"names");;
		} else {
			headerFile = args[0] + ".names";
		}
		IO.writeVectorOfStringsToFile(headerFile, headerVector);

		String dataFile = null;
		if (oonlyTrain) {
			dataFile = FileNamesAndDirectories.substituteExtension(data,"data");;
		} else {
			dataFile = args[0] + ".data";
		}
		write(instances, dataFile);

		String testFile = null;
		if (!oonlyTrain) {
			testFile = args[0] + ".test";
			instances = new Instances(args[0] + "_test.arff");
			renameAttributes(instances);
			write(instances, testFile);
		}

//		if (oreplaceMinuses) {
//			System.out.println("Replacing - by _");
//			replaceMinuses(dataFile);
//			replaceMinuses(headerFile);
//			if (!oonlyTrain) {
//				replaceMinuses(testFile);
//			}
//		}

		if (oonlyTrain) {
			 System.out.println("Successfully wrote " + dataFile + " and " + headerFile);
		} else {
			System.out.println("Successfully wrote " + dataFile + ", " + headerFile +
			" and " + testFile);
		}
	}

	public static void renameAttributes(Instances instances) {
		int natt = instances.numAttributes();
		for (int i = 0; i < natt; i++) {
			Attribute attribute = instances.attribute(i);
			String name = attribute.name();
			if (isThereAnInvalidChar(name)) {
				name = name.replace('-','R');
				name = name.replace('/','R');
				name = name.replace('&','R');
				name = name.replace(' ','R');
				name = name.replace('\'','R');
				instances.renameAttribute(i, name);
			}
			if (attribute.isNumeric()) {
				continue;
			}
			try {
				Integer.parseInt(name);
				instances.renameAttribute(i, "N" + name);
			} catch (NumberFormatException ex) {
				//don't need to do anything
			}
			int N = attribute.numValues();
			for (int j = 0; j < N; j++) {
				//see if it starts with a number
				String value = attribute.value(j);
				String firstChar = value.substring(0,1);
				try {
					Integer.parseInt(firstChar);
					instances.renameAttributeValue(i, j, "V" + value);
					value = "V" + value;
				} catch (NumberFormatException ex) {
					//don't need to do anything
				}
				if (isThereAnInvalidChar(value)) {
					//System.out.println(value);
					value = value.replace('-','R');
					value = value.replace('/','R');
					value = value.replace('&','R');
					value = value.replace(' ','R');
					value = value.replace('\'','R');
					//System.out.println(value);
					instances.renameAttributeValue(i, j, value);
				}
			}
		}
	}

	private static boolean isThereAnInvalidChar(String line) {
		int i = line.indexOf("-");
		if (i != -1) {
			return true;
		}
		i = line.indexOf("/");
		if (i != -1) {
			return true;
		}
		i = line.indexOf("&");
		if (i != -1) {
			return true;
		}
		i = line.indexOf(" ");
		if (i != -1) {
			return true;
		}
		i = line.indexOf("'");
		if (i != -1) {
			return true;
		}
		return false;
	}


	private static void replaceMinuses(String fileName) {
		String command = "sed s/-/_/g " + fileName + " > tt3424";
		System.out.println(command);
		new RunExternalProgram(command, false, false);
		command = "mv tt3424 " + fileName;
		System.out.println(command);
		new RunExternalProgram(command, false, false);
	}

	public static void write(Instances instances, String dataFile) throws Exception {
		BufferedWriter bufferedWriter = IO.openBufferedWriter(dataFile);
		int N = instances.numInstances();
		String suffix = null;
		if (m_oaddDot) {
			suffix = "." + IO.m_NEW_LINE;
		} else {
			suffix = IO.m_NEW_LINE;
		}
		for (int i = 0; i < N; i++) {
			Instance instance = instances.instance(i);
			bufferedWriter.write(instance.toString() + suffix);
		}
		IO.closeBufferedWriter(bufferedWriter);
	}

	private static String[] getHeader(Instances instances) {
		//assume class is last attribute, which is printed first in C4.5
		int natt = instances.numAttributes();
		String header[] = new String[natt];
		for (int i = 0; i < natt-1; i++) {
			Attribute attribute = instances.attribute(i);
			header[i+1] = attribute.toStringAsInC45();
			//System.out.println(header[i+1]);
		}
		header[0] = getClassAttribute(instances, natt);
		return header;
		//System.out.println(header[0]);
	}

	private static String getClassAttribute(Instances instances, int natt) {
		Attribute classAttribute = instances.attribute(natt-1);
		StringBuffer text = new StringBuffer();
		Enumeration enume = classAttribute.enumerateValues();
		while (enume.hasMoreElements()) {
			text.append(Utils.quote((String) enume.nextElement()));
			if (enume.hasMoreElements()) {
				text.append(',');
			}
		}
		text.append('.');
		return text.toString();
	}

}


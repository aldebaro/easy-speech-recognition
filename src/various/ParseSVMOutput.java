package various;

import edu.ucsd.asr.*;
import weka.core.*;
import weka.classifiers.*;
import java.io.*;
import java.util.*;

/**
 * Title:        Spock
 * Description:  Speech recognition
 * Copyright:    Copyright (c) 2001
 * Company:      UCSD
 * @author
 * @version 4.0
 */

//used to parse the output of SVM with option -V
public class ParseSVMOutput {

	private static final double m_dbootstrapPrecision = 0.90;
	private static final double m_dbootstrapIntervalForNormalizedGaussian;
	private static final boolean m_osvmNotRLSC = false; //true if want RLSC

	private static String[] m_datasets = {"soybean", "letter", "satimage",
		"abalone", "optdigits", "glass", "car", "spectrometer", "yeast", "page_blocks"};
	private static String[] m_datasetOptions = {"","-M","-M","-M","-M","-M","-M","-M","-M","-M"};

	//SVM:
	//private static final String[] m_datasetOptionsForSMO = {"","-M 200","-M 100","","-M 100","","-M 60","","",""};

	//RLSC:
	private static final String[] m_datasetOptionsForSMO = {"-M 1","-M 1","-M 1","-M 1","-M 1","-M 1","-M 1","-M 1","-M 1","-M 1"};

	private static String[] m_schemes = {"onevsrest", "allpairs", "dense",
		"sparse", "complete"};
	private static String[] m_schemesNumber = {"0", "1", "7", "6", "5"};
	private static final String m_optionForAll;

	private static String m_matlabDirectory = "cd g:/rif";

	static {
		double doneTail = (1 - m_dbootstrapPrecision) / 2;
		m_dbootstrapIntervalForNormalizedGaussian = Statistics.normalInverse(1 - doneTail);
		if (m_osvmNotRLSC) {
			m_optionForAll = "-H -D 1 -G -E";
		} else {
			m_optionForAll = "-D 1 -G -E";
		}
	}

	public static void main(String[] args) throws Exception {
		long linitialTime = System.currentTimeMillis();
		//getContingencyTableWithRespectToOneVsRest(1, 0);
		//System.exit(1);

		//1
//		if (m_osvmNotRLSC) {
//			generatePredictions();
//		} else {
//			generatePredictionsForRLSC();
//		}

		//2
//		if (m_osvmNotRLSC) {
//			getTableOnlyWithAgreement();
//			//getTableOnlyWithAgreementAndPrintToLatex();
//		} else {
//			getPercentageOfAgreementForRLSC();
//		}

		//3
//		if (m_osvmNotRLSC) {
//			getContingencyTablesWithRespectToOneVsRest();
//		} else {
//			getContingencyTablesWithRespectToOneVsRestForRLSC();
//		}


    //Two results comparing Furnkranz and one-vs-rest
    //go over all datasets
		for (int i = 0; i < 10; i++) {
			System.out.println("Dataset: " + m_datasets[i]);
			//compareFurnkranzToOneversusrest(i);
			System.out.println("Agree (same prediction for SVM one-vs-rest and R^3):\n" + IO.format(getPercentageOfAgreementForR3(i)));
			System.out.println("\n\n");
		}


		//4
		//getBinaryDecisions();
		//java various.ParseSVMOutput > t.tex
		//del \akprojects\rif\*.eps
		//ren page_blocks* page-blocks*
		//move *.eps \akprojects\rif

		//go to directory with .out files, it will read all of them
		//getMeansAndVariancesForKernelParameters();

		long lfinalTime = System.currentTimeMillis();
		System.out.println("Total time = " + ( (lfinalTime - linitialTime) / 1000.0) + " seconds.");
		System.exit(1);
	}

	private static void getMeansAndVariancesForKernelParameters() {
		IO.setMaximumFractionDigits(1);
		for (int i = 0; i < 10; i++) {
			for (int j = 0; j < 5; j++) {
				String fileName = null;
				if (m_osvmNotRLSC) {
					fileName = "./" + m_schemes[j] + "/" + m_datasets[i] + "_" + m_schemes[j] + ".out";
				} else {
					fileName = "./" + m_schemes[j] + "/" + m_datasets[i] + "_" + m_schemes[j] + "_rlsc.out";
				}
				System.out.println(IO.m_NEW_LINE + fileName);
				if (new File(fileName).exists()) {
				   getMeansAndVariancesForKernelParameters(fileName);
				} else {
					System.out.println("File does not exist");
				}
			}
		}
	}

	private static void getBinaryDecisions() throws Exception {
		IO.setMaximumFractionDigits(3);
		for (int i = 0; i < 10; i++) {
			for (int j = 1; j < 5; j++) {
				System.out.println(m_datasets[i] + " " + m_schemes[j] + " " +
					m_schemes[0]);
				getBinaryDecisionsForThisScheme(j, i);
				printUsingMatlab(j, i);
				//IO.pause();
			}
		}
		System.out.print(IO.m_NEW_LINE + IO.m_NEW_LINE);
		for (int i = 0; i < 10; i++) {
			System.out.print(IO.m_NEW_LINE + latexFigures(i));
		}
	}

	private static void getBinaryDecisionsForThisScheme(int nscheme2, int ndataset) throws Exception {
		//allways onevsrest
		int nscheme1 = 0;
		String file1 = m_datasets[ndataset] + "_" + m_schemes[nscheme1] + ".pred";
		String file2 = m_datasets[ndataset] + "_" + m_schemes[nscheme2] + ".pred";

		if (!new File(file2).exists()) {
			return;
		}

		String[] lines1 = IO.readArrayOfStringsFromFileIncludingBlank(file1);
		String[] lines2 = IO.readArrayOfStringsFromFileIncludingBlank(file2);
		int n = lines1.length;
		if (n != lines2.length) {
			End.throwError("Error: " + n + " != " + lines2.length);
		}
		String fileName = m_datasets[ndataset] + "_train.arff";
		if (!new File(fileName).exists()) {
			//CV
			fileName = m_datasets[ndataset] + ".arff";
		}
		int numClasses = new Instances(fileName).numClasses();
		int[][] nmatrix = getTrinaryECOCMatrix(nscheme2, numClasses).getECOCCodingMatrixReference();

		//IO.DisplayMatrix(nmatrix);
		//skip first line with header
		int nnumberOfBinaryClassifiers = nmatrix.length;
		BufferedWriter bufferedWriter = IO.openBufferedWriter(m_datasets[ndataset] + "_" + m_schemes[nscheme2] + ".errors");
		for (int i = 1; i < n; i++) {
			int[] p1 = getPredictions(lines1[i]);
			if (p1[0] != p1[1]) {
				//one-vs-rest led to an error, check how the binary classifiers
				//of other schemes behave
				int[] p2 = getPredictions(lines2[i]);
				int ntotalClassifiers = 0;
				int nerrors = 0;
				for (int j = 0; j < nnumberOfBinaryClassifiers; j++) {
					if (doesClassifierAssignDifferentClasses(p1[0], p1[1], j, nmatrix)) {
						//System.out.println(p1[0] + " " + p1[1]);
						//get output in this case, add 2 because 2 first entries are class + predicted
						int noutput = p2[2 + j];
						int ndesiredOutput = nmatrix[j][p2[1]];
						if (noutput != ndesiredOutput) {
							nerrors++;
						}
						ntotalClassifiers++;
					}
				}
				double percentageOfBinaryErrors = ( (double) nerrors ) / ntotalClassifiers;
				bufferedWriter.write(percentageOfBinaryErrors + IO.m_NEW_LINE);
			}
		}
		IO.closeBufferedWriter(bufferedWriter);
	}

	private static String latexSubfigure(String fileName, String caption) {
		return "\\subfigure[" + caption + "]" + IO.m_NEW_LINE +
		"{"  + IO.m_NEW_LINE +
		"		%\\label{fig:thislabel}"  + IO.m_NEW_LINE +
		"		\\includegraphics[width=7cm]{" + fileName + "}"  + IO.m_NEW_LINE +
		"}"  + IO.m_NEW_LINE;
	}

	private static String latexFigures(int ndataset) {
		String t = "\\begin{figure}" + IO.m_NEW_LINE + "\\centering" + IO.m_NEW_LINE;
		for (int i = 1; i < 5; i++) {
			String fileName = m_datasets[ndataset] + m_schemes[i] + ".eps";
			t += latexSubfigure(fileName, fileName);
			if (i == 2) {
				t += "\\\\";
			}
		}
		t += "\\caption{\\small{" + m_datasets[ndataset] + ".}}" + IO.m_NEW_LINE +
		"\\label{fig:thislabel}" + IO.m_NEW_LINE + "\\end{figure}";
		return t;
	}

	private static void printUsingMatlab(int nscheme2, int ndataset) {
		String fileName = m_datasets[ndataset] + "_" + m_schemes[nscheme2] + ".errors";
		MatlabInterfacer.sendCommand(m_matlabDirectory);
		if (new File(fileName).exists()) {
			MatlabInterfacer.sendCommand("load '" + fileName +"'");
			MatlabInterfacer.sendCommand("x = " + m_datasets[ndataset] + "_" + m_schemes[nscheme2]);
		} else {
			//this scheme was not calculated, plot something
			MatlabInterfacer.sendCommand("x = zeros(1,10);");
		}
		//MatlabInterfacer.sendCommandAndPrint("plot(linspace(0,1,length(x)),fliplr(sort(x)))");
		MatlabInterfacer.sendCommand("plot(linspace(0,1,length(x)),flipud(sort(x)))");
		MatlabInterfacer.sendCommand("title('Fraction of Binary Classification Errors on OVA Errors for the " +
		m_schemes[nscheme2].toUpperCase() + " Code (" + m_datasets[ndataset] + ")')");
		MatlabInterfacer.sendCommand("xlabel('Fraction of points')");
		MatlabInterfacer.sendCommand("ylabel('Percentage of Binary Errors')");
		MatlabInterfacer.sendCommand("axis([0 1 -0.05 1.05])");
		String output = m_datasets[ndataset] + m_schemes[nscheme2] + ".eps";
		MatlabInterfacer.sendCommand("print -depsc " + output);
	}

	private static boolean doesClassifierAssignDifferentClasses(int nclass1,
	int nclass2, int nbinaryClassifier, int[][] nmatrix) {
		return (nmatrix[nbinaryClassifier][nclass1] != 0 &&
		nmatrix[nbinaryClassifier][nclass2] != 0 &&
		nmatrix[nbinaryClassifier][nclass1] != nmatrix[nbinaryClassifier][nclass2]);
	}

	private static TrinaryECOCMatrix getTrinaryECOCMatrix(int nscheme, int numClasses) {
		switch (nscheme) {
			case 0:
				return new TrinaryECOCMatrix(TrinaryECOCMatrix.ERROR_ONEVSREST, numClasses, null);
			case 1:
				return new TrinaryECOCMatrix(TrinaryECOCMatrix.ERROR_ALLPAIRS, numClasses, null);
			case 2:
				return new TrinaryECOCMatrix(TrinaryECOCMatrix.ERROR_DENSE, numClasses, null);
			case 3:
				return new TrinaryECOCMatrix(TrinaryECOCMatrix.ERROR_SPARSE, numClasses, null);
			case 4:
				return new TrinaryECOCMatrix(TrinaryECOCMatrix.ERROR_COMPLETE, numClasses, null);
			default:
				return null;
		}
	}

	private static void getPercentageOfAgreementForRLSC() throws Exception {
		IO.setMaximumFractionDigits(3);
		for (int i = 0; i < 10; i++) {
			System.out.println(m_datasets[i]);
			for (int j = 0; j < 5; j++) {
					System.out.println(m_schemes[j] + " = " +
					IO.format(getPercentageOfAgreementForRLSC(j, i)));
			}
			System.out.print(IO.m_NEW_LINE + IO.m_NEW_LINE);
		}
	}


	//this option gives less information than contingency table
	private static void getTableOnlyWithAgreement() throws Exception {
		IO.setMaximumFractionDigits(3);
		for (int i = 0; i < 10; i++) {
			System.out.println(m_datasets[i]);
			for (int j = 0; j < 5; j++) {
				for (int k = 0; k < j; k++) {
					System.out.println(m_schemes[j] + " " +
					m_schemes[k] + " = " + IO.format(getPercentageOfAgreement(k, j, i)));
				}
			}
			System.out.print(IO.m_NEW_LINE + IO.m_NEW_LINE);
		}
	}

	private static void getTableOnlyWithAgreementAndPrintToLatex() throws Exception {
		IO.setMaximumFractionDigits(3);
		for (int i = 0; i < 10; i++) {
			System.out.println(m_datasets[i] + "\\\\");
			for (int j = 0; j < 5; j++) {
				for (int k = 0; k < j; k++) {
					System.out.println(m_schemes[j] + " " +
					m_schemes[k] + " = " + IO.format(getPercentageOfAgreement(k, j, i))  + "\\\\");
				}
			}
			System.out.print(IO.m_NEW_LINE + IO.m_NEW_LINE);
		}
	}

	//as in Rifkin's thesis
	//note that we count all the times that the binary classifiers don't agree,
	//such that this number cannot be obtained from the contingency table.
	private static double getPercentageOfAgreement(int nscheme1, int nscheme2, int ndataset) throws Exception {
		String file1 = m_datasets[ndataset] + "_" + m_schemes[nscheme1] + ".pred";
		String file2 = m_datasets[ndataset] + "_" + m_schemes[nscheme2] + ".pred";
		String[] lines1 = null;
		String[] lines2 = null;
		try {
			lines1 = IO.readArrayOfStringsFromFileIncludingBlank(file1);
			lines2 = IO.readArrayOfStringsFromFileIncludingBlank(file2);
		} catch (Exception ex) {
			return Double.NaN;
		}

		int n = lines1.length;
		//System.out.println("N = " + n);
		if (n != lines2.length) {
			End.throwError("Error: " + n + " != " + lines2.length);
		}
		//skip first line with header
		int nerrors = 0;
		for (int i = 1; i < n; i++) {
			int p1 = getPrediction(lines1[i]);
			int p2 = getPrediction(lines2[i]);
			if (p1 != p2) {
				//System.out.println(i + " " + p1 + " " + p2);
				nerrors ++;
			}
		}
		//System.out.println("nerrors = " + nerrors);
		return 1.0 - ( ( (double) nerrors) / (n - 1) );
	}

	//as in Rifkin's thesis
	//note that we count all the times that the binary classifiers don't agree,
	//such that this number cannot be obtained from the contingency table.
	private static double getPercentageOfAgreementForRLSC(int nscheme1, int ndataset) throws Exception {
		String file1 = "./Predictions/" + m_datasets[ndataset] + "_" + m_schemes[nscheme1] + ".pred";
		String file2 = "./rlsc_predictions/" + m_datasets[ndataset] + "_" + m_schemes[nscheme1] + ".pred";
		String[] lines1 = null;
		String[] lines2 = null;
		//System.out.println(file1 + " and " + file2);
		try {
			lines1 = IO.readArrayOfStringsFromFileIncludingBlank(file1);
			lines2 = IO.readArrayOfStringsFromFileIncludingBlank(file2);
		} catch (Exception ex) {
			return Double.NaN;
		}

		int n = lines1.length;
		//System.out.println("N = " + n);
		if (n != lines2.length) {
			End.throwError("Error: " + n + " != " + lines2.length);
		}
		//skip first line with header
		int nerrors = 0;
		for (int i = 1; i < n; i++) {
			int p1 = getPrediction(lines1[i]);
			int p2 = getPrediction(lines2[i]);
			if (p1 != p2) {
				//System.out.println(i + " " + p1 + " " + p2);
				nerrors ++;
			}
		}
		//System.out.println("nerrors = " + nerrors);
		return 1.0 - ( ( (double) nerrors) / (n - 1) );
	}

	//as in Rifkin's thesis
	//note that we count all the times that the binary classifiers don't agree,
	//such that this number cannot be obtained from the contingency table.
	private static double getPercentageOfAgreementForR3(int ndataset) throws Exception {
		int nscheme1 = 0;
		String file1 = "./Predictions/" + m_datasets[ndataset] + "_" + m_schemes[nscheme1] + ".pred";
		String file2 = "./r3_predictions/" + m_datasets[ndataset] + ".r3labs.ripper";
		String[] lines1 = null;
		String[] lines2 = null;
		//System.out.println(file1 + " and " + file2);
		try {
			lines1 = IO.readArrayOfStringsFromFileIncludingBlank(file1);
			lines2 = IO.readArrayOfStringsFromFileIncludingBlank(file2);
		} catch (Exception ex) {
			return Double.NaN;
		}

		int n = lines1.length;
		if (n != lines2.length+1) {
			End.throwError("Error: " + n + " != 1 + " + lines2.length);
		}

		//get names of classes
		TableOfLabels tableOfLabels  = getTableOfLabelsForParsingR3Output(ndataset);

		//skip first line with header, but no for Furnkranz
		int nerrors = 0;
		for (int i = 1; i < n; i++) {
			int p1 = getPrediction(lines1[i]);
			//int p2 = getPrediction(lines2[i-1]);
			String p2string = getRipperPredictions(lines2[i-1]);
			int p2 = tableOfLabels.getEntry(p2string);
			if (p1 != p2) {
				//System.out.println(i + " " + p1 + " " + p2);
				nerrors ++;
			}
		}
		//System.out.println("nerrors = " + nerrors);
		return 1.0 - ( ( (double) nerrors) / (n - 1) );
	}

	private static void getContingencyTablesWithRespectToOneVsRest() throws Exception {
		IO.setMaximumFractionDigits(3);
		for (int i = 0; i < 10; i++) {
			for (int j = 1; j < 5; j++) {
				System.out.println("--------------------------");
				System.out.println(m_datasets[i] + " one-vs-rest / " + m_schemes[j]);
				getContingencyTableWithRespectToOneVsRest(j, i);
			}
			System.out.print(IO.m_NEW_LINE + IO.m_NEW_LINE + IO.m_NEW_LINE);
		}
	}

	private static void getContingencyTablesWithRespectToOneVsRestForRLSC() throws Exception {
		IO.setMaximumFractionDigits(3);
		for (int i = 0; i < 10; i++) {
			//don't try complete
			for (int j = 0; j < 4; j++) {
				System.out.println("--------------------------");
				System.out.println(m_datasets[i] + " " + m_schemes[j] + " SVM vs RLSC");
				getContingencyTableWithRespectToOneVsRest(j, i);
			}
			System.out.print(IO.m_NEW_LINE + IO.m_NEW_LINE + IO.m_NEW_LINE);
		}
	}

	private static TableOfLabels getTableOfLabelsForParsingR3Output(int ndataset) throws Exception {
		//get names of classes
		String fileName = m_datasets[ndataset] + ".arff";
		Instances instances = null;
		if (new File(fileName).exists()) {
			instances = new Instances(fileName);
		} else {
			instances = new Instances(m_datasets[ndataset] + "_test.arff");
		}
		ConvertARFFToC45.renameAttributes(instances);

		String[] classValues = new String[instances.classAttribute().numValues()];
		Enumeration enumeration = instances.classAttribute().enumerateValues();
		int ncounter = 0;
		while (enumeration.hasMoreElements()) {
			String value = (String) enumeration.nextElement();
			classValues[ncounter++] = value;
		}
		return new TableOfLabels(classValues);
	}

	private static void compareFurnkranzToOneversusrest(int ndataset) throws Exception {

		String file1 = null;
		String file2 = null;
		String[] lines1 = null;
		String[] lines2 = null;

		//allways onevsrest
		int nscheme1 = 0;
		file1 = m_datasets[ndataset] + "_" + m_schemes[nscheme1] + ".pred";
	  file2 = m_datasets[ndataset] + ".r3labs.ripper";

		//get names of classes
		TableOfLabels tableOfLabels  = getTableOfLabelsForParsingR3Output(ndataset);

		try {
			lines1 = IO.readArrayOfStringsFromFileIncludingBlank(file1);
			lines2 = IO.readArrayOfStringsFromFileIncludingBlank(file2);
		} catch (Exception ex) {
			System.out.println("Skipping " + file1 + " and " + file2);
			return;
		}

		int n = lines1.length;
		if (n != lines2.length+1) {
			End.throwError("Error: " + n + " != 1 + " + lines2.length);
		}
		//skip first line with header for SVM, but not Furnkranz
		int[][] ncontingencyTable = new int[2][2];
		for (int i = 1; i < n; i++) {
			int[] p1 = getPredictions(lines1[i]);
			String p2string = getRipperPredictions(lines2[i-1]);
			int[] p2 = new int[2];
			p2[0] = tableOfLabels.getEntry(p2string);
			p2[1] = p1[1];
			if (p1[0] == p1[1]) {
				//one-vs-rest got it right
				if (p2[0] == p2[1]) {
					ncontingencyTable[1][1]++;
				} else {
					//System.out.println(i + " " + p1[0] + " " + p2[0]);
					ncontingencyTable[0][1]++;
				}
			} else {
				//one-vs-rest got it wrong
				if (p2[0] == p2[1]) {
					ncontingencyTable[1][0]++;
					//System.out.println(i + " " + p1[0] + " " + p2[0]);
				} else {
					ncontingencyTable[0][0]++;
				}
			}
		}
		System.out.println("Entry [0][1]: one-vs-rest got it right and " +
		"R^3 got wrong ");

		double x = Math.abs(ncontingencyTable[0][1] - ncontingencyTable[1][0]) - 1;
		x = (x * x) / (ncontingencyTable[0][1] + ncontingencyTable[1][0]);
		double t = 3.841459;
		boolean oareEquivalentAccordingToMcNemar = (x <= t);
		if (oareEquivalentAccordingToMcNemar) {
			//don't println because IO.DisplayMatrix adds \n in beginning
			System.out.print("McNemar - Equivalent (at 0.05). " + IO.format(x) + " <= " + IO.format(t));
		} else {
			System.out.print("McNemar - NOT equivalent (at 0.05). " + IO.format(x) + " > " + IO.format(t));
		}
		IO.DisplayMatrix(ncontingencyTable);

		boolean oareEquivalentAccordingToBootstrap = bootstrap(ncontingencyTable);
		if (oareEquivalentAccordingToBootstrap ^ oareEquivalentAccordingToMcNemar) {
			System.out.println("Bootstrap and McNemar disagree.");
		}
	}

	private static int getPrediction(String line) {
		int[] x = getPredictions(line);
		return x[0];
	}

	private static int[] getPredictions(String line) {
		StringTokenizer stringTokenizer = new StringTokenizer(line);
		int n = stringTokenizer.countTokens();
		int[] nout = new int[n];
		for (int i = 0; i < n; i++) {
			nout[i] = Integer.parseInt(stringTokenizer.nextToken());
		}
		return nout;
	}

	private static String getRipperPredictions(String line) {
		StringTokenizer stringTokenizer = new StringTokenizer(line);
		return (String) stringTokenizer.nextToken();
	}


	private static void getContingencyTableWithRespectToOneVsRest(int nscheme2, int ndataset) throws Exception {

		String file1 = null;
		String file2 = null;
		String[] lines1 = null;
		String[] lines2 = null;

		if (m_osvmNotRLSC) {
			//allways onevsrest
			int nscheme1 = 0;
			file1 = m_datasets[ndataset] + "_" + m_schemes[nscheme1] + ".pred";
		  file2 = m_datasets[ndataset] + "_" + m_schemes[nscheme2] + ".pred";
		} else {
		  file1 = "./Predictions/" + m_datasets[ndataset] + "_" + m_schemes[nscheme2] + ".pred";
		  file2 = "./rlsc_predictions/" + m_datasets[ndataset] + "_" + m_schemes[nscheme2] + ".pred";
		}

		try {
			lines1 = IO.readArrayOfStringsFromFileIncludingBlank(file1);
			lines2 = IO.readArrayOfStringsFromFileIncludingBlank(file2);
		} catch (Exception ex) {
			System.out.println(m_schemes[nscheme2] + " not used for " + m_datasets[ndataset]);
			return;
		}

		int n = lines1.length;
		if (n != lines2.length) {
			End.throwError("Error: " + n + " != " + lines2.length);
		}
		//skip first line with header
		int[][] ncontingencyTable = new int[2][2];
		for (int i = 1; i < n; i++) {
			int[] p1 = getPredictions(lines1[i]);
			int[] p2 = getPredictions(lines2[i]);
			if (p1[0] == p1[1]) {
				//one-vs-rest got it right
				if (p2[0] == p2[1]) {
					ncontingencyTable[1][1]++;
				} else {
					//System.out.println(i + " " + p1[0] + " " + p2[0]);
					ncontingencyTable[0][1]++;
				}
			} else {
				//one-vs-rest got it wrong
				if (p2[0] == p2[1]) {
					ncontingencyTable[1][0]++;
					//System.out.println(i + " " + p1[0] + " " + p2[0]);
				} else {
					ncontingencyTable[0][0]++;
				}
			}
		}
		System.out.println("Entry [0][1]: one-vs-rest got it right and " +
		m_schemes[nscheme2] + " got wrong ");

		double x = Math.abs(ncontingencyTable[0][1] - ncontingencyTable[1][0]) - 1;
		x = (x * x) / (ncontingencyTable[0][1] + ncontingencyTable[1][0]);
		double t = 3.841459;
		boolean oareEquivalentAccordingToMcNemar = (x <= t);
		if (oareEquivalentAccordingToMcNemar) {
			//don't println because IO.DisplayMatrix adds \n in beginning
			System.out.print("McNemar - Equivalent (at 0.05). " + IO.format(x) + " <= " + IO.format(t));
		} else {
			System.out.print("McNemar - NOT equivalent (at 0.05). " + IO.format(x) + " > " + IO.format(t));
		}
		IO.DisplayMatrix(ncontingencyTable);

		boolean oareEquivalentAccordingToBootstrap = bootstrap(ncontingencyTable);
		if (oareEquivalentAccordingToBootstrap ^ oareEquivalentAccordingToMcNemar) {
			System.out.println("Bootstrap and McNemar disagree.");
		}
	}

	//prints npredicted + " " + ncorrect
	//for SVM
	private static void generatePredictions() throws Exception {
		IO.setMaximumFractionDigits(2);
		//i is the ECOC matrix and j the dataset
		for (int i = 0; i < 5; i++) {
			//train / test
			for (int j = 0; j < 5; j++) {
				if (i==4 && (j==0 || j==1 || j==3) ) {
					//don't do complete for soybean, letter, abalone
					continue;
				}
				generatePredictionsNonCV(i, j);
			}
			//CV
			for (int j = 5; j < 10; j++) {
				if (i==2 && (j==6 || j==9)) {
					//don't do dense for car & page-blocks
					continue;
				}
				if (i==4 && j==7) {
					//don't do complete for spectrometer
					continue;
				}
				generatePredictionsCVForSVM(i, j);
			}
		}
	}


	//for RLSC
	private static void generatePredictionsForRLSC() throws Exception {
		IO.setMaximumFractionDigits(2);
		//i is the ECOC matrix and j the dataset
		for (int i = 0; i < 4; i++) {
			//train / test
			for (int j = 0; j < 5; j++) {
				if (j == 1) {
					//don't do letter
					continue;
				}
				generatePredictionsNonCV(i, j);
			}
			//CV
			for (int j = 5; j < 10; j++) {
				if (i==2 && (j==6 || j==9)) {
					//don't do dense for car & page-blocks
					continue;
				}
				generatePredictionsCVForRLSC(i, j);
			}
		}
	}

	private static void generatePredictionsNonCV(int nscheme, int ndataset) throws Exception {
			String file = "./outputs/" + m_schemes[nscheme] + "/" + m_datasets[ndataset] + "_";
			if (m_osvmNotRLSC) {
				file += m_schemes[nscheme] + ".out";
			} else {
				file += m_schemes[nscheme] + "_rlsc.out";
			}

			String outputFile = m_datasets[ndataset] + "_" + m_schemes[nscheme] + ".pred";
			BufferedWriter bufferedWriter = IO.openBufferedWriter(outputFile);
			//printOptions(file);
			String option = null;
			String newOption = null;
			String options = null;
			String t = null;
			Classifier classifier = null;
			option = getOptionsTrainTestSplit(file);
			if (m_osvmNotRLSC) {
				newOption = FileNamesAndDirectories.replacePartOfString(option, "-L 0.0010", "-R 0.001");
				if (newOption != null) {
					option = newOption;
				}
				option = correctCacheSize(option);
				options = m_datasetOptions[ndataset] + " " + m_optionForAll + " " +
				m_schemesNumber[nscheme] + " -W weka.classifiers.SMO -- " + option + " " +
				m_datasetOptionsForSMO[ndataset];
				t = "java weka.classifiers.SVM -t " + m_datasets[ndataset] +
				"_train.arff" + " -T " + m_datasets[ndataset]+"_test.arff " + options;
				bufferedWriter.write(t + IO.m_NEW_LINE);
				System.out.println(t);
				classifier = Classifier.constructClassifier("weka.classifiers.SVM",options);
				classifier.buildClassifier(new Instances(m_datasets[ndataset]+"_train.arff"));
			} else {
				//remove some options
				//-K 2 -C 2.0 -A 65521 -L 0.0010 -P 1.0E-12 -Q false -G 0.5
				String[] tempOptions = Utils.splitOptions(option);
				Utils.getOption('A', tempOptions);
				Utils.getOption('L', tempOptions);
				Utils.getOption('P', tempOptions);
				option = Utils.joinOptions(tempOptions);

				options = m_datasetOptions[ndataset] + " " + m_optionForAll + " " +
				m_schemesNumber[nscheme] + " -W weka.classifiers.SMO -- " + option + " " +
				m_datasetOptionsForSMO[ndataset];
				t = "java weka.classifiers.RLSC -t " + m_datasets[ndataset] +
				"_train.arff" + " -T " + m_datasets[ndataset]+"_test.arff " + options;
				bufferedWriter.write(t + IO.m_NEW_LINE);
				System.out.println(t);
				classifier = Classifier.constructClassifier("weka.classifiers.RLSC",options);
				classifier.buildClassifier(new Instances(m_datasets[ndataset]+"_train.arff"));
			}

			Instances instances = new Instances(m_datasets[ndataset]+"_test.arff");
			instances.setLastAttributeAsClass();
			int n = instances.numInstances();
			int nerrors = 0;
			for (int j = 0; j < n; j++) {
				Instance instance = instances.instance(j);
				//Instance instance2 = (Instance) instance.copy();
				int ncorrect = (int) instance.classValue();
				double[] dbinaryDecisions = null;
				if (m_osvmNotRLSC) {
				   dbinaryDecisions = ((SVM) classifier).getBinaryHardDecisions(instance);
				} else {
					//dbinaryDecisions = ((RLSC) classifier).getBinaryHardDecisions(instance);
				}
				int npredicted = (int) classifier.classifyInstance(instance);
				if (ncorrect != npredicted) {
					nerrors++;
				}
				bufferedWriter.write(npredicted + " " + ncorrect);
				if (m_osvmNotRLSC) {
				   for (int k = 0; k < dbinaryDecisions.length; k++) {
					     bufferedWriter.write(" " + (int) dbinaryDecisions[k]);
				   }
				}
				bufferedWriter.write(IO.m_NEW_LINE);
			}
			bufferedWriter.close();

			if (m_osvmNotRLSC) {
				System.out.println(getCandLambda(option) +
				"   SV's = " + ((SVM) classifier).getNumberOfDistinctSupportVectors() +
				"   Error = " + IO.format(100 * ((double) nerrors) / n ));
			} else {
				System.out.println(getCandLambda(option) +
				"   Error = " + IO.format(100 * ((double) nerrors) / n ));
			}
	}

	//here I shouldn't print # SV's because each SVM has a different one
	public static void generatePredictionsCVForSVM(int nscheme, int ndataset) throws Exception {
			String file = "./outputs/" + m_schemes[nscheme] + "/" + m_datasets[ndataset] + "_";
			if (m_osvmNotRLSC) {
				file += m_schemes[nscheme] + ".out";
			} else {
				file += m_schemes[nscheme] + "_rlsc.out";
			}

			String outputFile = m_datasets[ndataset] + "_" + m_schemes[nscheme] + ".pred";
			BufferedWriter bufferedWriter = IO.openBufferedWriter(outputFile);
			//printOptions(file);
			String[] optionsForCV = getOptionsCV(file);
			Instances instances = new Instances(m_datasets[ndataset] + ".arff");

			int nfolds = 10;
			Random random = new Random(1L);
			random.setSeed(1L);
			instances.randomize(random);
			instances.stratify(nfolds);

			SVM svm = null;
			int n = 0; // instances.numInstances();
			int nerrors = 0;

			for (int i = 0; i < nfolds; i++) {

				Instances train = instances.trainCV(nfolds, i);
				train.setLastAttributeAsClass();

				Instances test = instances.testCV(nfolds, i);
				test.setLastAttributeAsClass();

				//original
				String option = optionsForCV[i];
				//check if used wrong option -L
				String newOption = FileNamesAndDirectories.replacePartOfString(optionsForCV[i], "-L 0.0010", "-R 0.001");
				if (newOption != null) {
					option = newOption;
				}
				option = correctCacheSize(option);

				if (i == 0) {
					//only for first fold, don't print C and G
					String[] split = Utils.splitOptions(option);
					//get rid of C and G
					Utils.getOption('C', split);
					Utils.getOption('G', split);
					String t = "java weka.classifiers.SVM -t " + m_datasets[ndataset] +
								 ".arff" + " " + m_datasetOptions[ndataset] + " " + m_optionForAll + " " +
				m_schemesNumber[nscheme] + " -W weka.classifiers.SMO -- " + Utils.joinOptions(split) + " " +
				m_datasetOptionsForSMO[ndataset];
					bufferedWriter.write(t + IO.m_NEW_LINE);
					System.out.println(t);
				}

				//this option has C and gamma
				String options = m_datasetOptions[ndataset] + " " + m_optionForAll + " " +
				m_schemesNumber[nscheme] + " -W weka.classifiers.SMO -- " + option + " " +
				m_datasetOptionsForSMO[ndataset];
				svm = (SVM) Classifier.constructClassifier("weka.classifiers.SVM",options);
				svm.buildClassifier(train);

				int ntest = test.numInstances();

				for (int j = 0; j < ntest; j++) {
					Instance instance = test.instance(j);
					int ncorrect = (int) instance.classValue();
					double[] dbinaryDecisions = svm.getBinaryHardDecisions(instance);
					int npredicted = (int) svm.classifyInstance(instance);
					if (ncorrect != npredicted) {
						nerrors++;
					}
					bufferedWriter.write(npredicted + " " + ncorrect);
					for (int k = 0; k < dbinaryDecisions.length; k++) {
						bufferedWriter.write(" " + (int) dbinaryDecisions[k]);
					}
					bufferedWriter.write(IO.m_NEW_LINE);
				}
				n += ntest;

			}

			bufferedWriter.close();

			System.out.println("CV (various C and gamma): Error = " + IO.format(100 * ((double) nerrors) / n ));

	}

	public static void generatePredictionsCVForRLSC(int nscheme, int ndataset) throws Exception {
			String file = "./outputs/" + m_schemes[nscheme] + "/" + m_datasets[ndataset] + "_";
			if (m_osvmNotRLSC) {
				file += m_schemes[nscheme] + ".out";
			} else {
				file += m_schemes[nscheme] + "_rlsc.out";
			}

			String outputFile = m_datasets[ndataset] + "_" + m_schemes[nscheme] + ".pred";
			BufferedWriter bufferedWriter = IO.openBufferedWriter(outputFile);
			//printOptions(file);
			String[] optionsForCV = getOptionsCV(file);
			Instances instances = new Instances(m_datasets[ndataset] + ".arff");

			int nfolds = 10;
			Random random = new Random(1L);
			random.setSeed(1L);
			instances.randomize(random);
			instances.stratify(nfolds);

			RLSC rlsc = null;
			int n = 0; // instances.numInstances();
			int nerrors = 0;

			for (int i = 0; i < nfolds; i++) {

				Instances train = instances.trainCV(nfolds, i);
				train.setLastAttributeAsClass();

				Instances test = instances.testCV(nfolds, i);
				test.setLastAttributeAsClass();

				//original
				String option = optionsForCV[i];
				//remove some options
				//-K 2 -C 2.0 -A 65521 -L 0.0010 -P 1.0E-12 -Q false -G 0.5
				String[] tempOptions = Utils.splitOptions(option);
				Utils.getOption('A', tempOptions);
				Utils.getOption('L', tempOptions);
				Utils.getOption('P', tempOptions);
				option = Utils.joinOptions(tempOptions);

				if (i == 0) {
					//only for first fold, don't print C and G
					String[] split = Utils.splitOptions(option);
					//get rid of C and G
					Utils.getOption('C', split);
					Utils.getOption('G', split);

					String t = "java weka.classifiers.RLSC -t " + m_datasets[ndataset] +
								 ".arff" + " " + m_datasetOptions[ndataset] + " " + m_optionForAll + " " +
				m_schemesNumber[nscheme] + " -W weka.classifiers.SMO -- " + Utils.joinOptions(split) + " " +
				m_datasetOptionsForSMO[ndataset];
					bufferedWriter.write(t + IO.m_NEW_LINE);
					System.out.println(t);
				}

				//this option has C and gamma
				String options = m_datasetOptions[ndataset] + " " + m_optionForAll + " " +
				m_schemesNumber[nscheme] + " -W weka.classifiers.SMO -- " + option + " " +
				m_datasetOptionsForSMO[ndataset];
				rlsc = (RLSC) Classifier.constructClassifier("weka.classifiers.RLSC",options);
				rlsc.buildClassifier(train);

				int ntest = test.numInstances();

				for (int j = 0; j < ntest; j++) {
					Instance instance = test.instance(j);
					int ncorrect = (int) instance.classValue();
					//double[] dbinaryDecisions = rlsc.getBinaryHardDecisions(instance);
					int npredicted = (int) rlsc.classifyInstance(instance);
					if (ncorrect != npredicted) {
						nerrors++;
					}
					bufferedWriter.write(npredicted + " " + ncorrect);
					//ak
//Exception in thread "main" java.lang.ArrayIndexOutOfBoundsException: 21
//        at weka.core.Instance.isMissing(Instance.java:366)
//        at weka.filters.MakeIndicatorFilter.input(MakeIndicatorFilter.java:121)
//        at weka.classifiers.ScoreMultiClassClassifier.getBinaryHardDecisions(ScoreMultiClassClassifier.java:1101)
//        at various.ParseSVMOutput.generatePredictionsCVForRLSC(ParseSVMOutput.java:677)
//        at various.ParseSVMOutput.generatePredictionsForRLSC(ParseSVMOutput.java:423)
//        at various.ParseSVMOutput.main(ParseSVMOutput.java:61)
//					for (int k = 0; k < dbinaryDecisions.length; k++) {
//						bufferedWriter.write(" " + (int) dbinaryDecisions[k]);
//					}
					bufferedWriter.write(IO.m_NEW_LINE);
				}
				n += ntest;

			}

			bufferedWriter.close();

			System.out.println("CV (various C and gamma): Error = " + IO.format(100 * ((double) nerrors) / n ));

	}

	public static void checkNonCVFiles(String[] args) throws Exception {
		for (int i = 0; i < 5; i++) {
			String file = "./outputs/onevsrest/" + m_datasets[i] + "_onevsrest.out";
			//printOptions(file);
			String option = getOptionsTrainTestSplit(file);
			String newOption = FileNamesAndDirectories.replacePartOfString(option, "-L 0.0010", "-R 0.001");
			if (newOption != null) {
				option = newOption;
			}
			option = correctCacheSize(option);
			System.out.println(file + " " + option);
			String options = m_optionForAll + " -W weka.classifiers.SMO -- " + option;

			SVM svm = (SVM) Classifier.constructClassifier("weka.classifiers.SVM",options);
			svm.buildClassifier(new Instances(m_datasets[i]+"_train.arff"));

			Instances instances = new Instances(m_datasets[i]+"_test.arff");
			Evaluation evaluation = new Evaluation(instances);
			evaluation.evaluateModel(svm, instances);
			System.out.println(getCandLambda(option) +
			"   SV's = " + svm.getNumberOfDistinctSupportVectors() +
			"   Error = " + IO.format(100*evaluation.errorRate()));
		}
	}

	private static String correctCacheSize(String options) throws Exception {
		String[] split = Utils.splitOptions(options);
		Utils.getOption('M', split);
		Utils.getOption('A', split);
		return Utils.joinOptions(split);
	}

	private static String getCandLambda(String options) throws Exception {
		String[] split = Utils.splitOptions(options);
		String C = Utils.getOption('C', split);
		String L = Utils.getOption('G', split);
		return C + " / " + Utils.log2(Double.parseDouble(L));
	}

	private static double[] getCandLambdaAsDouble(String options) throws Exception {
		String[] split = Utils.splitOptions(options);
		String C = Utils.getOption('C', split);
		String L = Utils.getOption('G', split);
		if (C.equals("")) {
			C = "1";
		}
		if (L.equals("")) {
			L = "1";
		}
		double[] out = new double[2];
		out[0] = Double.parseDouble(C);
		//out[1] = Utils.log2(Double.parseDouble(L));
		out[1] = Double.parseDouble(L);
		return out;
	}


	private static void printOptions(String fileName) throws Exception {
		String[] lines = IO.readArrayOfStringsFromFileIncludingBlank(fileName);
		for (int i = 0; i < lines.length; i++) {
			if (lines[i].startsWith("Final options chosen through cross-validation")) {
				System.out.println(lines[i+2]);
				i += 2;
			}
		}
	}

	private static String getOptionsTrainTestSplit(String fileName) throws Exception {
		String[] lines = IO.readArrayOfStringsFromFileIncludingBlank(fileName);
		String option = null;
		for (int i = 0; i < lines.length; i++) {
			if (lines[i].startsWith("Final options chosen through cross-validation")) {
				if (option != null) {
					End.throwError("Error in logic");
				}
				option = lines[i+2];
				i += 2;
			}
		}
		return option;
	}

	private static String[] getOptionsCV(String fileName) throws Exception {
		String[] lines = IO.readArrayOfStringsFromFileIncludingBlank(fileName);
		//the individual options of each fold
		String[] options = new String[10];
		int n = -1;
		for (int i = 0; i < lines.length; i++) {
			if (lines[i].startsWith("Final options chosen through cross-validation")) {
				if (n == -1) {
					//don't do anything because the first one is with the whole training data
					n++;
					continue;
				}
				options[n] = lines[i+2];
				//System.out.println(options[n]);
				n++;
				i += 2;
			}
		}
		return options;
	}

	//table[1][1]: both got it right - table[0][0]: both got it wrong
	//table[0][1]: classifier 1 got it right and 2 wrong
	private static double[] convertToDistribution(int[][] ncontingencyTable) {
		double[] dpmf = new double[4];
		long ltotal = ncontingencyTable[0][0] + ncontingencyTable[0][1] +
									ncontingencyTable[1][0] + ncontingencyTable[1][1];
		dpmf[0] = ncontingencyTable[0][0] / (double) ltotal;
		dpmf[1] = ncontingencyTable[0][1] / (double) ltotal;
		dpmf[2] = ncontingencyTable[1][0] / (double) ltotal;
		dpmf[3] = ncontingencyTable[1][1] / (double) ltotal;
		return dpmf;
	}

	//returns true if classifiers are equivalent or false otherwise
	private static boolean OLDbootstrap(int[][] ncontingencyTable) {
		double[] dpmf = convertToDistribution(ncontingencyTable);
		PMFSampleGenerator discretePDFSampleGenerator = new PMFSampleGenerator(dpmf);

		int N = 10000;
		int[] nsample = discretePDFSampleGenerator.getSequenceOfSampleIndices(N);
		int[] dsamplePMF = new int[4];
		for (int i = 0; i < N; i++) {
			dsamplePMF[nsample[i]]++;
		}

//		MatlabInterfacer.sendArray(dpmf,"d");
//		MatlabInterfacer.sendArray(dsamplePMF,"s");
//		MatlabInterfacer.sendCommand("s = s/sum(s);");
//		MatlabInterfacer.sendCommand("plot(s-d)");
//		IO.pause();

		double daccuracyOfClassifier1 = (dpmf[1] + dpmf[3]) / (double) N;
		double daccuracyOfClassifier2 = (dpmf[2] + dpmf[3]) / (double) N;
		double ddiffClassifier = daccuracyOfClassifier1 - daccuracyOfClassifier2;

		double daccuracyOfDistribution1 = (dsamplePMF[1] + dsamplePMF[3]) / (double) N;
		double daccuracyOfDistribution2 = (dsamplePMF[2] + dsamplePMF[3]) / (double) N;

		double ddiffDistributions = daccuracyOfDistribution1 - daccuracyOfDistribution2;
		double dvariance = (daccuracyOfDistribution1*(1-daccuracyOfDistribution1) +
		daccuracyOfDistribution2*(1-daccuracyOfDistribution2)) / N;

		double dinterval = Math.sqrt(dvariance) * m_dbootstrapIntervalForNormalizedGaussian;
		double absDiff = Math.abs(ddiffDistributions - ddiffClassifier);
		boolean oareEquivalent = (absDiff <= dinterval);
		if (oareEquivalent) {
			System.out.println("Bootstrap - Equivalent (at " + IO.format(1-m_dbootstrapPrecision) + "). " + IO.format(absDiff) + " <= " + IO.format(dinterval));
		} else {
			System.out.println("Bootstrap - NOT Equivalent (at " + IO.format(1-m_dbootstrapPrecision) + "). " + IO.format(absDiff) + " > " + IO.format(dinterval));
		}
//		System.out.println("dvariance = " + dvariance);
//		System.out.println("dinterval = " + dinterval);
//		System.out.println("diff = " + (ddiffDistributions - ddiffClassifier));
		return oareEquivalent;
	}

	private static double getDifferenceOnAccuracyUsingFakeDataset(PMFSampleGenerator discretePDFSampleGenerator,
	int N) {
		int[] nsample = discretePDFSampleGenerator.getSequenceOfSampleIndices(N);
		int[] dsamplePMF = new int[4];
		for (int i = 0; i < N; i++) {
			dsamplePMF[nsample[i]]++;
		}

//		MatlabInterfacer.sendArray(dpmf,"d");
//		MatlabInterfacer.sendArray(dsamplePMF,"s");
//		MatlabInterfacer.sendCommand("s = s/sum(s);");
//		MatlabInterfacer.sendCommand("plot(s-d)");
//		IO.pause();

		double daccuracyOfDistribution1 = (dsamplePMF[1] + dsamplePMF[3]) / (double) N;
		double daccuracyOfDistribution2 = (dsamplePMF[2] + dsamplePMF[3]) / (double) N;
		return daccuracyOfDistribution1 - daccuracyOfDistribution2;
	}

	//returns true if classifiers are equivalent or false otherwise
	private static boolean bootstrap(int[][] ncontingencyTable) {
//		ncontingencyTable[0][0] = 999990;
//		ncontingencyTable[0][1] = 0;
//		ncontingencyTable[1][0] = 10;
//		ncontingencyTable[1][1] = 1000000;

		//initialize
		double[] dpmf = convertToDistribution(ncontingencyTable);
		//use the multinomial pmf corresponding to the contingency table
		PMFSampleGenerator discretePDFSampleGenerator = new PMFSampleGenerator(dpmf);
		long lseed = 1;
		discretePDFSampleGenerator.setSeedOfRandomNumberGenerator(lseed);
		//N is the total number of test examples
		int N = ncontingencyTable[0][0] + ncontingencyTable[0][1] +
					  ncontingencyTable[1][0] + ncontingencyTable[1][1];

		//get the accuracy of actual pair of classifiers
		double daccuracyOfClassifier1 = dpmf[1] + dpmf[3];
		double daccuracyOfClassifier2 = dpmf[2] + dpmf[3];
		double ddiffClassifier = daccuracyOfClassifier1 - daccuracyOfClassifier2;

		//generate several draws
		int nnumDraws  = 10000;
		double[] ddifferencesInAccuracy = new double[nnumDraws];
		for (int i = 0; i < nnumDraws; i++) {
			ddifferencesInAccuracy[i] = getDifferenceOnAccuracyUsingFakeDataset(discretePDFSampleGenerator, N);
		}

//		MatlabInterfacer.sendArray(dpmf,"d");
//		MatlabInterfacer.sendArray(dsamplePMF,"s");
//		MatlabInterfacer.sendCommand("s = s/sum(s);");
//		MatlabInterfacer.sendCommand("plot(s-d)");
//		IO.pause();

		int[] nascendingOrder = Utils.sort(ddifferencesInAccuracy);
		//get percentiles for ddifferencesInAccuracy
		double doneTail = (1 - m_dbootstrapPrecision) / 2;
		int nindiceForLowerLimit = (int) (doneTail * nnumDraws);
		int nindiceForUpperLimit = (int) ( (1-doneTail) * nnumDraws);

//		for (int i = 0; i < nascendingOrder.length; i++) {
//			System.out.print(ddifferencesInAccuracy[nascendingOrder[i]] + " ");
//		}

		//System.out.println("\n" + nindiceForLowerLimit + " " + nindiceForUpperLimit);

		double dlowerLimit = 0.5 * (ddifferencesInAccuracy[nascendingOrder[nindiceForLowerLimit]]+
		ddifferencesInAccuracy[nascendingOrder[nindiceForLowerLimit+1]]);
		double dupperLimit = 0.5 * (ddifferencesInAccuracy[nascendingOrder[nindiceForUpperLimit]]+
		ddifferencesInAccuracy[nascendingOrder[nindiceForUpperLimit+1]]);

		String limits = "[" + IO.format(dlowerLimit) + ", " + IO.format(dupperLimit) + "]";

		IO.setMaximumFractionDigits(2);
		String precision = IO.format(1-m_dbootstrapPrecision);
		IO.setMaximumFractionDigits(3);
		//boolean oareEquivalent = (ddiffClassifier >= dlowerLimit) && (ddiffClassifier <= dupperLimit);
		//check if the interval includes 0
		boolean oareEquivalent = (dlowerLimit * dupperLimit <= 0);
		if (oareEquivalent) {
			System.out.println("Bootstrap - Equivalent (at " + precision + "): actual diff = " + IO.format(ddiffClassifier) + ", interval = " + limits);
		} else {
			System.out.println("Bootstrap - NOT Equivalent (at " + precision + "): actual diff = " + IO.format(ddiffClassifier) + ", interval = " + limits);
		}
		IO.setMaximumFractionDigits(3);
//		System.out.println("dvariance = " + dvariance);
//		System.out.println("dinterval = " + dinterval);
//		System.out.println("diff = " + (ddiffDistributions - ddiffClassifier));
		return oareEquivalent;
	}

	private static void getMeansAndVariancesForKernelParameters(String fileName) {
//		DirectoryTree directoryTree = new DirectoryTree(".", "out");
//		String[] files = directoryTree.getFilesAsStrings();
//		for (int i = 0; i < files.length; i++) {
//			String fileName = files[i];
			String[] options = null;
			try {
				options = getOptionsCV(fileName);
			}
			catch (Exception ex) {
				ex.printStackTrace();
			}
			if (options[0] == null) {
				//didn't use CV
				double[] dcAndLambda = null;
				try {
					String option = getOptionsTrainTestSplit(fileName);
					dcAndLambda = getCandLambdaAsDouble(option);
				}
				catch (Exception ex) {
					ex.printStackTrace();
				}
				double C = dcAndLambda[0];
				double Gamma = dcAndLambda[1];
				if (m_osvmNotRLSC) {
				   System.out.println("C = " + IO.format(C) + ", log2(gamma) = " + IO.format(Utils.log2(Gamma)));
				} else {
					System.out.println("lambda * N = " + IO.format(C) + ", log2(gamma) = " + IO.format(Utils.log2(Gamma)));
				}
				//System.out.println("Skipped " + fileName + " without 10 CV options." + IO.m_NEW_LINE);
				return;
			}
			//first one is with the whole dataset, so skip it
			double varC = 0;
			double varGamma = 0;
			double meanC = 0;
			double meanGamma = 0;
			for (int j = 0; j < options.length; j++) {
				double[] dcAndLambda = null;
				try {
					dcAndLambda = getCandLambdaAsDouble(options[j]);
				}
				catch (Exception ex) {
					ex.printStackTrace();
				}
				meanC += dcAndLambda[0];
				varC += dcAndLambda[0]*dcAndLambda[0];
				meanGamma += dcAndLambda[1];
				varGamma += dcAndLambda[1]*dcAndLambda[1];
			}
			meanC /= 10;
			meanGamma /= 10;
			varC = varC/10 - meanC * meanC;
			varGamma = varGamma/10 - meanGamma * meanGamma;
			//System.out.println(IO.m_NEW_LINE + fileName); lambda * N
			if (m_osvmNotRLSC) {
				System.out.println("C: mean = " + IO.format(meanC) + ", var = " + IO.format(varC));
				System.out.println("gamma: log2(mean) = " + IO.format(Utils.log2(meanGamma)) + ", var = " + IO.format(varGamma));
			} else {
				System.out.println("lambda * N: mean = " + IO.format(meanC) + ", var = " + IO.format(varC));
				System.out.println("gamma: log2(mean) = " + IO.format(Utils.log2(meanGamma)) + ", var = " + IO.format(varGamma));
			}
	}

}
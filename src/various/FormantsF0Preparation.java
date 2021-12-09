package various;

import edu.ucsd.asr.*;
import weka.core.*;

/**
 * Title:        Spock
 * Description:  Speech recognition
 * Copyright:    Copyright (c) 2001
 * Company:      UCSD
 * @author       Aldebaro Klautau
 * @version 4.0
 */
public class FormantsF0Preparation {

	private static int m_nwindowLength = 320;
	private static int m_nwindowShift = 160;
	private static int m_nfs = 16000; //sampling frequency

	public static void main(String[] args) {
		//generate list
		//generateList("f:/databases_2dirs/timit/train");
		//System.exit(1);

		//convert files from Snack into one FEA per sentence
		//used this for the ICASSP and SBRT papers
		//convertToFEAFile(args);

		//write several HTK files
		convertToHTKFiles(args);
	}

	//read f0 and frm files, and write each as an FEA file
	//need to confirm if method to get # of frames return the correct
	private static void convertToHTKFiles(String[] args) {
		if (args.length != 3) {
			Print.dialog(Utils.joinOptions(args));
			Print.dialog("Usage: <F0 sentences dir> <waveform dir> <output directory>");
			System.exit(1);
		}
		String wavDir = FileNamesAndDirectories.replaceAndForceEndingWithSlash(args[1]);
		String outDir = FileNamesAndDirectories.replaceAndForceEndingWithSlash(args[2]);
		FileNamesAndDirectories.createDirectoriesIfNecessary(outDir);
		DirectoryTree dt = new DirectoryTree(args[0], "f0");
		String[] files = dt.getFilesAsStrings();
		for (int i = 0; i < files.length; i++) {
			String onlyFile = FileNamesAndDirectories.getFileNameFromPath(files[i]);
			IO.showCounter(i);
			String wavFile = wavDir + FileNamesAndDirectories.substituteExtension(onlyFile, "wav");
			String outFile = outDir + FileNamesAndDirectories.substituteExtension(onlyFile, "FEA");

			float[][] ff0 = getF0AndVoicingProbability(files[i]);
			float[][] fformants = getFormantFrequencies(FileNamesAndDirectories.substituteExtension(files[i], "frm"), 4);
			int nsen = SeneffPreparation.getNumberOfFramesFrowWavFile(wavFile, m_nwindowLength, m_nwindowShift);
			int nf0 = ff0.length;
			int nfor = fformants.length;
			//if (nf0 > nsen + 2 || nfor > nsen + 1 || nf0 > nsen || nfor < nsen) {
			//  Print.dialog(files[i] + " " + nsen + " " + nf0 + " " + nfor);
			//}
			Pattern patternF0 = new Pattern(ff0);
			Pattern patternFormants = new Pattern(fformants);

			int nnumberToDeleteFromF0 = nf0 - nsen;
			int nnumberToDeleteFromFormants = nfor - nsen;
			if (nnumberToDeleteFromF0 > 0) {
				patternF0.deleteGivenNumberOfFramesFromTheEndOfThisPattern(nnumberToDeleteFromF0);
				Print.dialog("DELETE " + nnumberToDeleteFromF0 + " " + files[i] + " " + nsen + " " + nf0 + " " + nfor);
			} else if (nnumberToDeleteFromF0 < 0) {
				for (int j = 0; j < -nnumberToDeleteFromF0; j++) {
					patternF0.replicateLastFrame();
				}
				Print.dialog("ADD " + (-nnumberToDeleteFromF0) + " " + files[i] + " " + nsen + " " + nf0 + " " + nfor);
			}
			if (nnumberToDeleteFromFormants > 0) {
				patternFormants.deleteGivenNumberOfFramesFromTheEndOfThisPattern(nnumberToDeleteFromFormants);
			} else if (nnumberToDeleteFromFormants < 0) {
				for (int j = 0; j < -nnumberToDeleteFromFormants; j++) {
					patternFormants.replicateLastFrame();
				}
			}
			//now they have same # frames, put formants after F0
			patternF0.concatenateNewFeatures(patternFormants);
			
			String outputFileName = outDir + FileNamesAndDirectories.substituteExtension(onlyFile, "f0frm.htk");
			short parmKind = (short) 9;
			int sampPeriod = (int) (1.0e7 / m_nfs);
			HTKInterfacer.writePattern(patternF0.getParameters(), outputFileName, sampPeriod, parmKind);
			
			//PatternGenerator patternGenerator = new EmptyPatternGenerator(6, "f0_voicing_4formants");
			//SetOfPatterns setOfPatterns = new SetOfPatterns(patternGenerator);
			//setOfPatterns.addPattern(patternF0);
			//setOfPatterns.writeToFile(outFile, FileNamesAndDirectories.substituteExtension(onlyFile, "wav"), "SENTENCE");
		}
	}


	//read f0 and frm files, and write each as an FEA file
	//need to confirm if method to get # of frames return the correct
	private static void convertToFEAFile(String[] args) {
		if (args.length != 3) {
			Print.dialog(Utils.joinOptions(args));
			Print.dialog("Usage: <F0 sentences dir> <waveform dir> <output FEA directory>");
			System.exit(1);
		}
		String wavDir = FileNamesAndDirectories.replaceAndForceEndingWithSlash(args[1]);
		String outDir = FileNamesAndDirectories.replaceAndForceEndingWithSlash(args[2]);
		FileNamesAndDirectories.createDirectoriesIfNecessary(outDir);
		DirectoryTree dt = new DirectoryTree(args[0], "f0");
		String[] files = dt.getFilesAsStrings();
		for (int i = 0; i < files.length; i++) {
			String onlyFile = FileNamesAndDirectories.getFileNameFromPath(files[i]);
			IO.showCounter(i);
			String wavFile = wavDir + FileNamesAndDirectories.substituteExtension(onlyFile, "wav");
			String outFile = outDir + FileNamesAndDirectories.substituteExtension(onlyFile, "FEA");

			float[][] ff0 = getF0AndVoicingProbability(files[i]);
			float[][] fformants = getFormantFrequencies(FileNamesAndDirectories.substituteExtension(files[i], "frm"), 4);
			int nsen = SeneffPreparation.getNumberOfFramesFrowWavFile(wavFile, m_nwindowLength, m_nwindowShift);
			int nf0 = ff0.length;
			int nfor = fformants.length;
			//if (nf0 > nsen + 2 || nfor > nsen + 1 || nf0 > nsen || nfor < nsen) {
			//  Print.dialog(files[i] + " " + nsen + " " + nf0 + " " + nfor);
			//}
			Pattern patternF0 = new Pattern(ff0);
			Pattern patternFormants = new Pattern(fformants);

			int nnumberToDeleteFromF0 = nf0 - nsen;
			int nnumberToDeleteFromFormants = nfor - nsen;
			if (nnumberToDeleteFromF0 > 0) {
				patternF0.deleteGivenNumberOfFramesFromTheEndOfThisPattern(nnumberToDeleteFromF0);
				Print.dialog("DELETE " + nnumberToDeleteFromF0 + " " + files[i] + " " + nsen + " " + nf0 + " " + nfor);
			} else if (nnumberToDeleteFromF0 < 0) {
				for (int j = 0; j < -nnumberToDeleteFromF0; j++) {
					patternF0.replicateLastFrame();
				}
				Print.dialog("ADD " + (-nnumberToDeleteFromF0) + " " + files[i] + " " + nsen + " " + nf0 + " " + nfor);
			}
			if (nnumberToDeleteFromFormants > 0) {
				patternFormants.deleteGivenNumberOfFramesFromTheEndOfThisPattern(nnumberToDeleteFromFormants);
			} else if (nnumberToDeleteFromFormants < 0) {
				for (int j = 0; j < -nnumberToDeleteFromFormants; j++) {
					patternFormants.replicateLastFrame();
				}
			}
			//now they have same # frames, put formants after F0
			patternF0.concatenateNewFeatures(patternFormants);
			PatternGenerator patternGenerator = new EmptyPatternGenerator(6, "f0_voicing_4formants");
			SetOfPatterns setOfPatterns = new SetOfPatterns(patternGenerator);
			setOfPatterns.addPattern(patternF0);
			setOfPatterns.writeToFile(outFile, FileNamesAndDirectories.substituteExtension(onlyFile, "wav"), "SENTENCE");
		}
	}

	private static void generateList(String dir) {
		DirectoryTree directoryTree = new DirectoryTree(dir, "wav");
		String[] files = directoryTree.getFilesAsStrings();
		for (int i = 0; i < files.length; i++) {
			Print.dialog(files[i]);
		}
	}

	private static float[][] getFormantFrequencies(String formantFile, int nnumberOfFormants) {
		String[] formants = IO.readArrayOfStringsFromFile(formantFile);
		float[][] fparameters = new float[formants.length][nnumberOfFormants];
		for (int j = 0; j < formants.length; j++) {
			String[] formantResults = Utils.splitOptions(formants[j]);
			if (formantResults.length < 2 * nnumberOfFormants) {
				End.throwError("formantResults.length < 2 * nnumberOfFormants " +
						formantResults.length + " < " + (2 * nnumberOfFormants));
			}
			for (int i = 0; i < nnumberOfFormants; i++) {
				fparameters[j][i] = Float.parseFloat(formantResults[i]);
			}
		}
		return fparameters;
	}

	/**
	 * Pitch is first, then the probability of voicing is second.
	 */
	private static float[][] getF0AndVoicingProbability(String f0Name) {
		String[] f0 = IO.readArrayOfStringsFromFile(f0Name);
		float[][] fparameters = new float[f0.length][2];
		for (int j = 0; j < f0.length; j++) {
			String[] f0Results = Utils.splitOptions(f0[j]);
			fparameters[j][0] = Float.parseFloat(f0Results[0]);
			fparameters[j][1] = Float.parseFloat(f0Results[1]);
		}
		return fparameters;
	}

}

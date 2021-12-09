package debug;

import edu.ucsd.asr.*;

import java.util.Random;

public class SetOfSharedContinuousHMMsTest {

	public static void main(String[] args) {
		// isip(args);
		absurd(args);
	}
	public static void absurd(String[] args) {
		String initialDir = "C:/Temp/besta/absurd/";
		String initialSet = initialDir + "inputhmms.txt";
		// start with plain and convert to shared
		SetOfPlainContinuousHMMs setOfPlainContinuousHMMs = HTKInterfacer.getPlainHMMsFromFile(
					initialSet, new EmptyPatternGenerator(39));
		SetOfSharedContinuousHMMs setOfSharedContinuousHMMs = setOfPlainContinuousHMMs
					.convertToSharedHMMs();
		//String hTKFileName = initialDir + "parameters/1_one.mfc";
		String hTKFileName = initialDir + "parameters/0_zero.mfc";
		Pattern pattern = HTKInterfacer.getPatternFromFile(hTKFileName);
		setOfSharedContinuousHMMs.findBestModelAndItsScore(pattern);
		int n = setOfSharedContinuousHMMs.getBestModel();
		String l = setOfSharedContinuousHMMs.getLabelOfBestModel();
		System.out.println(n + " " + l);
	}

	public static void isip(String[] args) {
		String folder = "/software/isip/srstw02_session_11/models_srstw02_session11";
		folder += "/train/baum_welch/xwrd_tri/";
		SetOfSharedContinuousHMMs setOfHMMs1 = new SetOfSharedContinuousHMMs(
				folder, SetOfSharedContinuousHMMs.ExternalFormatType.ISIP);
		End.exit(0);

		String flnm = "/home/aklautau/mehmm/hmms_shared.zip";
		System.out.println("Opening " + flnm);
		SetOfSharedContinuousHMMs setOfHMMs = (SetOfSharedContinuousHMMs) SetOfHMMsFile
				.read(flnm);
		Print.dialog("Models loaded.");
		Print.dialog("Number of mixtures: " + setOfHMMs.getNumberOfMixtures());
		Print.dialog("Number of components in mixtures: "
				+ setOfHMMs.getNumberOfGaussianComponentsInMixture(0));
		int nspaceDim = setOfHMMs.getSpaceDimension();
		Print.dialog("Space dimension is: " + nspaceDim);

		// testing the two methods for computing sentences:
		Random r = new Random();
		float[][] fO, ftableM, ftableT, ftableTT;
		long lstart;
		long lmixture = 0;
		long ltime = 0;
		long ltimeT = 0;
		for (int nsentence = 0; nsentence < 100; nsentence++) {
			int size = 200 + r.nextInt(300);
			// int size = 5;
			fO = new float[size][nspaceDim];
			for (int i = 0; i < size; i++) {
				for (int j = 0; j < nspaceDim; j++) {
					fO[i][j] = r.nextFloat();
				}
			}
			lstart = System.currentTimeMillis();
			// for mixture; for component; for time
			setOfHMMs.setSentencePrecomputeMode(1);
			setOfHMMs.initializeSentence(fO);
			lmixture += System.currentTimeMillis() - lstart;
			ftableM = setOfHMMs.getSentenceMixtureScore();
			lstart = System.currentTimeMillis();
			// for mixture; for time; for component
			setOfHMMs.setSentencePrecomputeMode(2);
			setOfHMMs.initializeSentence(fO);
			ltime += System.currentTimeMillis() - lstart;
			ftableT = setOfHMMs.getSentenceMixtureScore();
			lstart = System.currentTimeMillis();
			// for time; for mixture; for component
			setOfHMMs.setSentencePrecomputeMode(3);
			setOfHMMs.initializeSentence(fO);
			ltimeT += System.currentTimeMillis() - lstart;
			ftableTT = setOfHMMs.getSentenceMixtureScore();
			// compare if we get the same scores
			double mse12 = 0.0;
			double mse13 = 0.0;
			double mse23 = 0.0;
			int nmix = setOfHMMs.getNumberOfMixtures();
			for (int t = 0; t < size; t++) {
				for (int m = 0; m < nmix; m++) {
					mse12 += Math.pow(ftableM[t][m] - ftableT[t][m], 2.0);
					mse13 += Math.pow(ftableM[t][m] - ftableTT[t][m], 2.0);
					mse23 += Math.pow(ftableT[t][m] - ftableTT[t][m], 2.0);
				}
			}
			mse12 /= (size * nmix);
			mse13 /= (size * nmix);
			mse23 /= (size * nmix);
			Print.dialog("MSE errors:\t" + mse12 + "\t" + mse13 + "\t" + mse23);
		}
		Print.dialog("Mixture ordering took:\t" + lmixture);
		Print.dialog("Time ordering took:\t" + ltime);
		Print.dialog("TimeT ordering took:\t" + ltimeT);
		End.exit(0);
		// convert from plain to shared and write to file
		String f = "hmms.zip";
		String o = "hmms_shared.zip";
		SetOfPlainContinuousHMMs hmmSet = new SetOfPlainContinuousHMMs(f);
		SetOfSharedContinuousHMMs setOfSharedContinuousHMMs3 = hmmSet
				.convertToSharedHMMs();
		setOfSharedContinuousHMMs3.writeHTKAndJARFiles(o);

		// read set of shared hmms
		// first I need to create the mapping from logical to physical HMMs
		String[] labels = hmmSet.getTableOfLabels().getAllFirstLabels();
		String map = "";
		for (int i = 0; i < labels.length; i++) {
			map += labels[i] + " " + labels[i] + IO.m_NEW_LINE;
		}
		String htkFile = "hmms_shared.txt";
		String logicalFileName2 = "map.txt";
		IO.writeStringToFile(logicalFileName2, map);
		SetOfSharedContinuousHMMs setOfSharedContinuousHMMs4 = HTKInterfacer
				.getSharedHMMsFromFiles(htkFile, logicalFileName2);
		if (setOfSharedContinuousHMMs4.equals(setOfSharedContinuousHMMs3)) {
			System.out.println("OK: HMMs are the same");
		} else {
			System.out.println("ERROR: HMMs are not the same");
		}
		System.exit(1);

		long ls = System.currentTimeMillis();

		String fileName = "C:/htk/simulationsMfcc_e_d_a/triphones/hmms_embedded_9/SetOfHMMs.txt";
		String logicalFileName = "C:/htk/simulationsMfcc_e_d_a/triphones/FinalListOfTriphonesMappingUnseenIntoSeen.txt";

		SetOfSharedContinuousHMMs setOfSharedContinuousHMMs = HTKInterfacer
				.getSharedHMMsFromFiles(fileName, logicalFileName);
		setOfSharedContinuousHMMs
				.writeHistogramOfNumberOfLogicalPerPhysicalHMM("hist.txt");

		String jarFileName = "test.jar";
		setOfSharedContinuousHMMs.writeToJARFile(fileName, logicalFileName,
				new EmptyPatternGenerator(39), jarFileName);

		SetOfSharedContinuousHMMs setOfSharedContinuousHMMs2 = new SetOfSharedContinuousHMMs(
				jarFileName);
		if (setOfSharedContinuousHMMs2.equals(setOfSharedContinuousHMMs)) {
			Print.dialog("OK");
		} else {
			Print.dialog("NOT OK");
		}

		long lf = System.currentTimeMillis();

		System.out.println((lf - ls) + " ms.");
	}
}

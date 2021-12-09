package debug;

import edu.ucsd.asr.*;

public class SetOfPlainContinuousHMMsTest {

	public static void main(String[] args) {
		//main1(args);
		main2(args);
	}
	
	private static void main1(String[] args) {

		//String sopFile = "C:/simulations/timit/39models/plp18w512s160/features/train/14_g.sop";
		//String sopFile = "C:/simulations/timit/39models/plp18w512s160/features/train/0_aa.sop";
		//String hmmFile = "C:/simulations/timit/39models/plp18w512s160/lrforwardskips5/monophones/isolated/kmeansviterbi/10/hmms.jar";
		String hmmFile = "G:/newsimuls/timit/16models/plpeda39w512s160/leftright/monophones/isolated/baumwelch/26/hmms.zip";
		String sopFile = "G:/newsimuls/timit/16models/plpeda39w512s160/features/test/1_ih.FEA";
		//String sopFile = "G:/newsimuls/timit/48models/plpeda39w512s160/features/test/1_ih.FEA";
		//String hmmFile = "G:/newsimuls/timit/48models/plpeda39w512s160/leftright/monophones/isolated/baumwelch/20/hmms.zip";
		//String hmmFile = "C:/simulations/timit/39models/plp18w512s160/lrforwardskips5/monophones/isolated/prototypes/hmms.jar";
		SetOfPlainContinuousHMMs hmmSet = new SetOfPlainContinuousHMMs(hmmFile);

		String mehmmFileName = "c:/mehmm.bin";
		SetOfMatrixEncodedHMMs hmmMatrixSet = SetOfMatrixEncodedHMMs.getSetOfMatrixEncodedHMMsFromSerializedFile(mehmmFileName);
		//SetOfMatrixEncodedHMMs hmmMatrixSet = new SetOfMatrixEncodedHMMs(hmmFile, false);

		SetOfPatterns setOfPatterns = new SetOfPatterns(sopFile);

		//check if shared and plain give same results (they did)
		//SetOfSharedContinuousHMMs hmmSharedSet = hmmSet.convertToSharedHMMs();
		for (int i=0; i<setOfPatterns.getNumberOfPatterns(); i++) {
			Pattern pattern = setOfPatterns.getPattern(i);
			hmmSet.findBestModelAndItsScore(pattern);
			//hmmSharedSet.findBestModelAndItsScore(pattern);
			hmmMatrixSet.findBestModelAndItsScore(pattern);

			int ndx1 = hmmSet.getBestModel();
			//int ndx2 = hmmSharedSet.getBestModel();
			int ndx2 = hmmMatrixSet.getBestModel();
			Print.dialog(ndx1 + " " + ndx2 + " " + hmmSet.getBestScore() + " " +
			hmmMatrixSet.getBestScore());
			if (ndx1 != ndx2) {
				Print.error("Sheet");
				//System.exit(1);
			}
		}
		System.exit(1);

		boolean okeepPathsOfNBestList = true;
		hmmSet.enableNBestListGeneration(39,okeepPathsOfNBestList);


		for (int i=0; i<10; i++) {
			Pattern pattern = setOfPatterns.getPattern(i);
			hmmSet.findBestModelAndItsScore(pattern);
			Print.dialog(hmmSet.getHMMLabelsInNBestListAsString());
			IO.displayPartOfVector(hmmSet.getScoresInNBestList(),100);
			int[][] npaths = hmmSet.getPathsOfNBestList();
			//IO.DisplayMatrix(npaths);
			Print.dialog("Scores:");
			IO.displayPartOfVector(hmmSet.getScoreEvolution(pattern,npaths[0],hmmSet.getBestModel()),100);
			Print.dialog("Paths");
			Print.dialog(hmmSet.getPathsOfNBestListUsingRunLengthEncoding());
		}


		SetOfPlainContinuousHMMs setOfPlainContinuousHMMs2 = new SetOfPlainContinuousHMMs("c:/htk/myhmms/my20G/output.jar");
		String output = setOfPlainContinuousHMMs2.toStringAsInHTK();
		IO.writeStringToFile("c:/htk/myhmms/my20G/htk.txt",output);
		//Print.dialog(output);
		//SetOfPlainContinuousHMMs setOfPlainContinuousHMMs = new SetOfPlainContinuousHMMs("test.jar");
		System.exit(1);
	}
	
	private static void main2(String[] args) {

		ContinuousHMM[] continuousHMM = new ContinuousHMM[2];
		continuousHMM[0] = new ContinuousHMM(3,3,39);
		continuousHMM[1] = new ContinuousHMM(3,3,39);
		String[] names = new String[2];
		names[0] = "test_0.HMM";
		names[1] = "test_1.HMM";
		String[][] table = new String[2][1];
		table[0][0] = "zero";
		table[1][0] = "one";
		TableOfLabels tableOfLabels = new TableOfLabels(table);
		String pattern = "c:/Users/Aldebaro/Spock14/debug/testPATTERN.PTR.txt";
		PatternGenerator patternGenerator = PatternGenerator.getPatternGenerator(pattern);

		SetOfPlainContinuousHMMs setOfPlainContinuousHMMs = new SetOfPlainContinuousHMMs(continuousHMM,
																						 names,
																						 tableOfLabels,
																						 patternGenerator);
		setOfPlainContinuousHMMs.writeToJARFile("comeon.jar",
												null,
												null);


	}
}

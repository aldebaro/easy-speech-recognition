package various;

import edu.ucsd.asr.*;

//generate the features with CutSegmentsInThirds
//writes to stdout. Need to be run from the directory with FEA files
public class TIMITSegmentalFeatures {

  //Example:
	//private static TableOfLabels m_tableOfLabels60 = new TableOfLabels(TableOfLabels.Type.TIMIT60);
	//private static TableOfLabels m_tableOfLabels39 = new TableOfLabels(TableOfLabels.Type.TIMIT39);
	private static TableOfLabels m_tableOfLabels60 = new TableOfLabels(TableOfLabels.Type.DECTALK);
	private static TableOfLabels m_tableOfLabels39 = new TableOfLabels(TableOfLabels.Type.DECTALK);



	public static void main(String[] args) {
		String[] labels = m_tableOfLabels60.getAllFirstLabels();
		String[] labels2 = m_tableOfLabels39.getAllFirstLabels();

		int nspaceDimension = -1;
		for (int i = 0; i < labels.length; i++) {
			String name = i + "_" + labels[i] + ".FEA";
			String label = m_tableOfLabels39.getFirstLabel(labels[i]);
			SetOfPatterns setOfPatterns = new SetOfPatterns(name);
			if (i == 0) {
				nspaceDimension = setOfPatterns.getSpaceDimension();
				printHeader(nspaceDimension);
			} else if (nspaceDimension != setOfPatterns.getSpaceDimension()) {
				//check
				End.throwError("nspaceDimension != setOfPatterns.getSpaceDimension() " +
				nspaceDimension + " != " + setOfPatterns.getSpaceDimension());
			}
			//Print.dialog(name + " => " + label + " => " + setOfPatterns.getNumberOfPatterns());
			int n = setOfPatterns.getNumberOfPatterns();
			for (int j = 0; j < n; j++) {
			Pattern pattern = setOfPatterns.getPattern(j);
			if (pattern.getNumOfFrames() != 1) {
			  End.throwError("Ops... I assume there is a fixed-length vector but this pattern has " + pattern.getNumOfFrames() + " frames!");
			}
			float[] parms = pattern.getParametersOfGivenFrame(0);
			System.out.print(IO.format(parms[0]));
			for (int k = 1; k < parms.length; k++) {
				System.out.print("," + IO.format(parms[k]));
			}
			System.out.print("," + label + "\n");
			}
		}
	}

	private static void printHeader(int nspaceDimension) {
		String[] labels = m_tableOfLabels60.getAllFirstLabels();
		String[] labels2 = m_tableOfLabels39.getAllFirstLabels();

		Print.dialog("@relation phonetics\n\n");
		for (int i = 0; i < nspaceDimension; i++) {
			Print.dialog("@attribute par" + (i+1) + " real");
		}
		System.out.print("@attribute label {" + labels2[0]);
		for (int i = 1; i < labels2.length; i++) {
			System.out.print("," + labels2[i]);
		}
		System.out.print("}\n\n@data\n");
	}


}

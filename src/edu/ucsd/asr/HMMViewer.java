package edu.ucsd.asr;

import java.util.Vector;
import java.util.StringTokenizer;

/**Visualize an HMM
 *
 * @author Aldebaro Klautau
 * @version 2.0 - August 29, 2000
 *
 */
public class HMMViewer {

	//per dimension
	private final static int m_nnumberOfResolutionPoints = 100;

	//private ContinuousHMM m_continuousHMM;
	//private SetOfPatterns m_setOfPatterns;
	private float[][] m_fdynamicRanges;
	private float[] m_fstepSizes;
	private int m_nspaceDimension;
	private int m_nnumberOfStates;
	private int[] m_nstateDuration;

	//per state, per element, per point
	//float[m_nnumberOfStates][m_nspaceDimension][m_nnumberOfResolutionPoints];
	//it has score values for the grid of points of each state
	//private float[][][] m_fimageValues;
	//organize m_fimageValues as a matrix
	//float[m_nnumberOfStates][m_nspaceDimension*m_nnumberOfResolutionPoints];
	//private float[][] m_fstateRepresentation;

	public HMMViewer(ContinuousHMM continuousHMM,
	SetOfPatterns setOfPatterns) {
		//m_continuousHMM = continuousHMM;
		m_nnumberOfStates = continuousHMM.getNumberOfStates() - 2;
		//m_setOfPatterns = setOfPatterns;
		m_nspaceDimension = continuousHMM.getSpaceDimension();
		//m_fimageValues = new float[continuousHMM.getNumberOfStates()-2][m_nspaceDimension][m_nnumberOfResolutionPoints];
		estimateDynamicRanges(setOfPatterns);
		calculateStepSizes();
		calculateDurationOfEachState(continuousHMM);
	}

//	public HMMViewer(ContinuousHMM continuousHMM,
//	String fileName, int nhMMIndex) {
//		m_continuousHMM = continuousHMM;
//		Vector lines = IO.readVectorOfStringsFromFile(fileName);
//		int nnumberOfLines = lines.size();
//		for (int i = 0; i < nnumberOfLines; i++) {
//			String line = (String) lines.elementAt(i);
//			StringTokenizer stringTokenizer = new StringTokenizer(line);
//			int nindex = Integer.parseInt(stringTokenizer.nextToken());
//			if (nindex == nhMMIndex) {
//				interpretLine(line);
//			}
//		}
//	}

//	private void interpretLine(String line) {
//		StringTokenizer stringTokenizer = new StringTokenizer(line);
//		int nindex = Integer.parseInt(stringTokenizer.nextToken());
//		String hMMLabel = stringTokenizer.nextToken();
//		m_nnumberOfStates = Integer.parseInt(stringTokenizer.nextToken());
//		m_nstateDuration = new int[m_nnumberOfStates];
//		for (int i = 0; i < m_nstateDuration.length; i++) {
//			m_nstateDuration[i] = Integer.parseInt(stringTokenizer.nextToken());
//		}
//		m_nspaceDimension = Integer.parseInt(stringTokenizer.nextToken());
//		m_fdynamicRanges = new float[m_nspaceDimension][2];
//		for (int i = 0; i < m_nspaceDimension; i++) {
//			m_fdynamicRanges[i][0] = Float.parseFloat(stringTokenizer.nextToken());
//			m_fdynamicRanges[i][1] = Float.parseFloat(stringTokenizer.nextToken());
//		}
//
//		m_fimageValues = new float[m_nnumberOfStates][m_nspaceDimension][m_nnumberOfResolutionPoints];
//		//estimateDynamicRanges(setOfPatterns);
//		calculateStepSizes();
//	}

	public void getHMMUniqueRepresentation(ContinuousHMM continuousHMM) {
		for (int i = 0; i < m_nnumberOfStates; i++) {
			//Print.dialog(i + "");
			analyzeState(continuousHMM,i);
		}
		//saveFile();

		//optional
		//convertToProbabilities();

		//optional
		//comment out if don't want rescaling
		rescaleStateRepresentation();
		//m_fstateRepresentation = concatenateDimensionsAsColumns();
	}

//	private void convertToProbabilities() {
//		for (int i = 0; i < m_nnumberOfStates; i++) {
//			for (int j = 0; j < m_nspaceDimension; j++) {
//				for (int k = 0; k < m_nnumberOfResolutionPoints; k++) {
//					//m_fimageValues[i][j][k] = (float) Math.exp(m_fimageValues[i][j][k]);
//				}
//			}
//		}
//	}

	private void rescaleStateRepresentation() {
	//float[m_nnumberOfStates][m_nspaceDimension][m_nnumberOfResolutionPoints];
	//it has score values for the grid of points of each state
		for (int i = 0; i < m_nnumberOfStates; i++) {
			for (int j = 0; j < m_nspaceDimension; j++) {
				//m_fimageValues[i][j] = rescale(m_fimageValues[i][j],0,10);
			}
		}
	}

	private float[] rescale(float[] x, float fmin, float fmax) {
		float fxmax = Matrix.getMaximumValueOfVector(x);
		float fxmin = Matrix.getMinimumValueOfVector(x);
		float[] scaledX = new float[x.length];
		float ffactor = (fmax - fmin) / (fxmax - fxmin);
		for (int i = 0; i < x.length; i++) {
			scaledX[i] = ((x[i] - fxmin) * ffactor) + fmin;
		}
		return scaledX;
	}

	//should be dependent on transition matrix
	private void calculateDurationOfEachState(ContinuousHMM continuousHMM) {
		float[][] ftransitionMatrix = continuousHMM.getTransitionMatrix();
		m_nstateDuration = new int[m_nnumberOfStates];
		for (int i = 0; i < m_nstateDuration.length; i++) {
			//arbitrary
			//m_nstateDuration[i] = m_nnumberOfResolutionPoints * m_nnumberOfStates;
			m_nstateDuration[i] = (int) (0.5 + (1.0 / (1.0 - ftransitionMatrix[i+1][i+1])));
		}
	}

	private void estimateDynamicRanges(SetOfPatterns setOfPatterns) {
		m_fdynamicRanges = new float[m_nspaceDimension][2];
		for (int i = 0; i < m_nspaceDimension; i++) {
			m_fdynamicRanges[i][0] = setOfPatterns.getMinimumValueOfGivenParameter(i);
			m_fdynamicRanges[i][1] = setOfPatterns.getMaximumValueOfGivenParameter(i);
		}
	}

	private void calculateStepSizes() {
		//System.out.println(m_nspaceDimension);
		m_fstepSizes = new float[m_nspaceDimension];
		for (int i = 0; i < m_nspaceDimension; i++) {
			//System.out.println("\n" + m_fdynamicRanges[i][1] + " " + m_fdynamicRanges[i][0]);
			m_fstepSizes[i] = (m_fdynamicRanges[i][1] - m_fdynamicRanges[i][0]) / m_nnumberOfResolutionPoints;
		}
	}

	//fill vector with mixture "mean"
	private void analyzeState(ContinuousHMM continuousHMM, int nstateNumber) {
		MixtureOfGaussianPDFs mixtureOfGaussianPDFs = continuousHMM.getSpecificMixture(nstateNumber+2);
		float[] fmean = mixtureOfGaussianPDFs.getWeightedAverageOfGaussianMeans();
		for (int i = 0; i < m_nspaceDimension; i++) {
			//will need to change one element for each i, so make a copy
			float[] fmeanCopy = Cloner.clone(fmean);
			for (int j=0; j < m_nnumberOfResolutionPoints; j++) {
				float fvalue = m_fdynamicRanges[i][0] + j*m_fstepSizes[i];
				fmeanCopy[i] = fvalue;
				//m_fimageValues[nstateNumber][i][j] = mixtureOfGaussianPDFs.calculateLogProbability(fmeanCopy);
			}
		}
	}

	private int getTotalDuration() {
		int nduration = 0;
		for (int i = 0; i < m_nstateDuration.length; i++) {
			nduration += m_nstateDuration[i];
		}
		return nduration;
	}

	private float[][] concatenateDimensionsAsColumns() {
		float[][] fstateRepresentation = new float[m_nnumberOfStates][m_nspaceDimension*m_nnumberOfResolutionPoints];
		for (int i = 0; i < m_nnumberOfStates; i++) {
			//concatenate as a vector
			for (int j=0; j<m_nspaceDimension; j++) {
				for (int k=0; k<m_nnumberOfResolutionPoints; k++) {
					//fstateRepresentation[i][j*m_nnumberOfResolutionPoints+k]=m_fimageValues[i][j][k];
				}
			}
		}
		return fstateRepresentation;
	}

	public void composeImageAndSaveFiles(Pattern pattern,
	int[] nstateAlignment,
	//float[][][] fhHMMRepresentation,
	boolean oright) {

//	int nnumberOfStates = fhHMMRepresentation.length;
//	int nspaceDimension = fhHMMRepresentation[0].length;
//	int nnumberOfPoints = fhHMMRepresentation[0][0].length;

	//float[getNumberOfStates()-2][m_nspaceDimension][m_nnumberOfResolutionPoints];
		float[][] ffinalImage = new float[m_nspaceDimension*m_nnumberOfResolutionPoints][nstateAlignment.length];
		//int ncurrentColumn = 0;
		for (int j=0; j<nstateAlignment.length; j++) {
			//for (int k=0; k<m_fstateRepresentation[0].length; k++) {
				//subtract 1 because in nstateAlignment first state is 1
				//ffinalImage[k][j] = m_fstateRepresentation[nstateAlignment[j]-1][k];
			//}
			//ncurrentColumn++;
		}
		//IO.DisplayMatrix(m_fimageValues[0]);
		//IO.DisplayMatrix(ffinalImage);
		if (oright) {
		//IO.writeMatrixtoASCIIFile("c:/temp/pattern.txt",pattern.getParameters());
		IO.writeMatrixtoASCIIFile("c:/temp/hmmview.txt",ffinalImage);
		IO.writeVectortoASCIIFile("c:/temp/states.txt",nstateAlignment);
		} else {
		//IO.writeMatrixtoASCIIFile("c:/temp/pattern2.txt",pattern.getParameters());
		IO.writeMatrixtoASCIIFile("c:/temp/hmmview2.txt",ffinalImage);
		IO.writeVectortoASCIIFile("c:/temp/states2.txt",nstateAlignment);
		}
		generateTrajectory(pattern,nstateAlignment,oright);
	}

	private int quantize(float x, int ndimensionIndex) {
		int nbaseValue = m_nnumberOfResolutionPoints * ndimensionIndex;
		if (x < m_fdynamicRanges[ndimensionIndex][0]) {
			return nbaseValue;
		} else if (x > m_fdynamicRanges[ndimensionIndex][1]) {
			return nbaseValue + m_nnumberOfResolutionPoints;
		} else {
			return nbaseValue + (int) (( (x-m_fdynamicRanges[ndimensionIndex][0]) / m_fstepSizes[ndimensionIndex]) + 0.5);
		}
	}

	private void generateGrid(int nnumberOfFrames) {
		int[][] ngrid = new int[nnumberOfFrames][m_nspaceDimension + 1];
		for (int i = 0; i < nnumberOfFrames; i++) {
			for (int j = 0; j < m_nspaceDimension + 1; j++) {
				ngrid[i][j] = j * m_nnumberOfResolutionPoints;
			}
		}
		IO.writeMatrixtoASCIIFile("c:/temp/griddy.txt",ngrid);
	}

	private void generateTrajectory(Pattern pattern,
	int[] nstateAlignment,
	boolean oright) {

//		int[][] nquantizedValues = Matrix.quantize(pattern.getParameters());
//
//		if (oright) {
//			IO.writeMatrixtoASCIIFile("c:/temp/pattern.txt",nquantizedValues);
//		} else {
//			IO.writeMatrixtoASCIIFile("c:/temp/pattern2.txt",nquantizedValues);
//		}

		generateGrid(nstateAlignment.length);
	}

	//to be eliminated
//	public void createFiles() {
//		float[][] ffinalImage = new float[m_nspaceDimension*m_nnumberOfResolutionPoints][getTotalDuration()];
//		int ncurrentColumn = 0;
//		for (int i = 0; i < m_nnumberOfStates; i++) {
//			//concatenate as a vector
//			float[] fstateRepresentation = new float[m_nspaceDimension*m_nnumberOfResolutionPoints];
//			for (int j=0; j<m_nspaceDimension; j++) {
//				for (int k=0; k<m_nnumberOfResolutionPoints; k++) {
//					fstateRepresentation[j*m_nnumberOfResolutionPoints+k]=m_fimageValues[i][j][k];
//				}
//			}
//			for (int j=0; j<m_nstateDuration[i]; j++) {
//				for (int k=0; k<fstateRepresentation.length; k++) {
//					ffinalImage[k][ncurrentColumn] = fstateRepresentation[k];
//				}
//				ncurrentColumn++;
//			}
//		}
//		IO.DisplayMatrix(m_fimageValues[0]);
//		IO.DisplayMatrix(ffinalImage);
//		IO.writeMatrixtoASCIIFile("c:/temp/a.txt",m_fimageValues[0]);
//		IO.writeMatrixtoASCIIFile("c:/temp/b.txt",m_fimageValues[1]);
//		IO.writeMatrixtoASCIIFile("c:/temp/c.txt",m_fimageValues[2]);
//		IO.writeMatrixtoASCIIFile("c:/temp/final.txt",ffinalImage);
//	}

	public String toString() {
		//StringBuffer stringBuffer = new StringBuffer("State durations:");
		StringBuffer stringBuffer = new StringBuffer("" + m_nstateDuration.length);
		for (int i = 0; i < m_nstateDuration.length; i++) {
			stringBuffer.append(" " + m_nstateDuration[i]);
		}
		//stringBuffer.append(" Dynamic ranges:");
		stringBuffer.append(" " + m_fdynamicRanges.length);
		for (int i = 0; i < m_fdynamicRanges.length; i++) {
			stringBuffer.append(" " + i + " " + IO.format(m_fdynamicRanges[i][0]) +
			" " + IO.format(m_fdynamicRanges[i][1]));
		}
		return stringBuffer.toString();
	}

//	public static void createViewerInformationForHMMSet(String sopDir,
//	 SetOfPlainContinuousHMMs setOfPlainContinuousHMMs,
//	 String outputFileName) {
//		TableOfLabels table = setOfPlainContinuousHMMs.getTableOfLabels();
//		int nnumberOfHMMs = table.getNumberOfEntries();
//		HMMViewer[] hMMViewer = new HMMViewer[nnumberOfHMMs];
//		ContinuousHMM[] continuousHMM = setOfPlainContinuousHMMs.getHMMs();
//		Vector outputVector = new Vector();
//		for (int i = 0; i < nnumberOfHMMs; i++) {
//			String sopFile = sopDir + i + "_" + table.getFirstLabel(i) + ".sop";
//			hMMViewer[i] = new HMMViewer(continuousHMM[i],new SetOfPatterns(sopFile));
//			outputVector.addElement(i + " " + table.getFirstLabel(i) + " " + hMMViewer[i].toString());
//		}
//		IO.writeVectorOfStringsToFile(outputFileName, outputVector);
//	}

} // end of class
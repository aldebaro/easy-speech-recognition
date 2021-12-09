package debug;

import edu.ucsd.asr.*;
//import JMatLink;
import java.util.Vector;
import java.util.StringTokenizer;

public class HMMViewerTest {

	//private static JMatLink m_engine;
	
	private static float m_sampleRate = 11025F;

	public static void main(String[] args) {

		//String sopFile = "C:/simulations/timit/39models/plp18w512s160/features/train/14_g.sop";
		//String sopFile = "C:/simulations/timit/39models/plp18w512s160/features/train/0_aa.sop";
		//String sopDir = "C:/simulations/timit/39models/plp18w512s160/features/test/";
		String sopDir = "/temp/output-hlsyntimit/56models/dct_klatt14s160/features/test/";
		//String dtlDir = "C:/simulations/timit/39models/transcriptions/monophones/isolated/test/";
		String dtlDir = "/temp/output-hlsyntimit/56models/transcriptions/monophones/isolated/test/";
		//String hmmFile = "C:/simulations/timit/39models/plp18w512s160/lrforwardskips5/monophones/isolated/kmeansviterbi/10/hmms.jar";
		String hmmFile = "C:/temp/output-hlsyntimit/56models/dct_klatt14s160/lr55/monophones/isolated/kmeansviterbi/10/hmms.zip";
		//String hmmFile = "C:/simulations/timit/39models/plp18w512s160/lrforwardskips5/monophones/isolated/prototypes/hmms.jar";
		SetOfPlainContinuousHMMs hmmSet = new SetOfPlainContinuousHMMs(hmmFile);

		String outputFileName = "c:/temp/bestao.txt";
		//HMMViewer.createViewerInformationForHMMSet(sopDir,
		//	hmmSet,outputFileName);
		//System.exit(1);


		int nnumberOfHMMs = hmmSet.getNumberOfHMMs();
		HMMViewer[] hMMViewer = new HMMViewer[nnumberOfHMMs];
		TableOfLabels table = hmmSet.getTableOfLabels();
		ContinuousHMM[] continuousHMMs = hmmSet.getHMMs();
		Print.dialog("Going to list all HMMs");
		for (int i = 0; i < nnumberOfHMMs; i++) {
			IO.showCounter(i);
			String sopFile = sopDir + i + "_" + table.getFirstLabel(i) + "." + SetOfPatterns.m_FILE_EXTENSION;
			ContinuousHMM hmm = continuousHMMs[i];
			hMMViewer[i] = new HMMViewer(hmm,new SetOfPatterns(sopFile));
			//hMMViewer[i] = new HMMViewer(outputFileName,i);
			hMMViewer[i].getHMMUniqueRepresentation(continuousHMMs[i]);
			Print.dialog("\n" + hMMViewer[i].toString());
			//fhMMRepresentations[i] =  hMMViewer.getHMMRepresentation();
		}
		Print.dialog("");

		//System.exit(1);
		//hMMViewer.createFiles();

		//now test it

		//TableOfLabels table = hmmSet.getTableOfLabels();
		int nhMMIndex = 26;
		String sopFile = sopDir + nhMMIndex + "_" + table.getFirstLabel(nhMMIndex) + "." + SetOfPatterns.m_FILE_EXTENSION;
		String dtlFile = dtlDir + nhMMIndex + "_" + table.getFirstLabel(nhMMIndex) + ".DTL";
		SetOfPatterns setOfPatterns = new SetOfPatterns(sopFile);
		DatabaseManager databaseManager = new DatabaseManager(dtlFile);

		//files will be written in a temporary file
		MatlabInterfacer.sendCommand("cd c:\\temp");

		boolean okeepPathsOfNBestList = true;
		//hmmSet.enableNBestListGeneration(5,okeepPathsOfNBestList); //keep only 5 candidates in N-best list
		hmmSet.enableNBestListGeneration(nnumberOfHMMs,okeepPathsOfNBestList); //keep all candidates in N-best list
		int i=0;
		while (databaseManager.isThereDataToRead()) {
		//for (int i=0; i<10; i++) {
			DataLocator dataLocator = databaseManager.getNextDataLocator();
			LabeledSpeech labeledSpeech = new LabeledSpeech(dataLocator);

			//XXX I am gonna use only segment # 0
			int[] naudioSamples = labeledSpeech.getAudioDataAsIntegersFromGivenSegment(0);
			double[] daudioSamples = Cloner.cloneAsDouble(naudioSamples);
			naudioSamples = null;
			MatlabInterfacer.sendArray(daudioSamples, "daudioSamples");

			//int[] nallAudioSamples = new Audio(AudioFileReader.getAudioInputStream(dataLocator.getFileName())).getAudioDataAsIntegers();
			int[] nendPoints = dataLocator.getEndpointsFromGivenSegment(0);

			int[] nallAudioSamples = labeledSpeech.getAudioOfWholeSentence().getAudioDataAsIntegers();

			int nnumberOfSamplesToInclude = 2048;
			int[] nrelativeEndPoints = findRelativeEndpoints(nendPoints,nnumberOfSamplesToInclude,nallAudioSamples.length);
			System.out.println("nrelativeEndPoints");
			IO.DisplayVector(nrelativeEndPoints);

			//double[] dallAudioSamples = Cloner.cloneAsDouble(nallAudioSamples);

			AudioPlayer.playRegionScaled(labeledSpeech.getAudioOfWholeSentence(),nendPoints[0]-nnumberOfSamplesToInclude,nendPoints[1]+nnumberOfSamplesToInclude);
			double[] dallAudioSamples = Cloner.cloneRegionAsDouble(nallAudioSamples,nendPoints[0],nendPoints[1],nnumberOfSamplesToInclude);
			nallAudioSamples = null;
			MatlabInterfacer.sendArray(dallAudioSamples, "dallAudioSamples");

			float[] frelativeEndPoints = convertToTime(nrelativeEndPoints,m_sampleRate);
			//MatlabInterfacer.sendCommand("figure(2);clf;subplot(311);waveplot(daudioSamples,16000)");
			MatlabInterfacer.sendCommand("figure(2);clf;");

			//spectrogram			
			//akspecgram(x,filterBWInHz,samplingFrequency,windowShiftInms,thresholdIndB)
			MatlabInterfacer.sendCommand("subplot(211);ak_specgram(dallAudioSamples,200," + m_sampleRate + ",1,60);");
			MatlabInterfacer.sendCommand("set(gca,'xtick',["+frelativeEndPoints[0]+" "+frelativeEndPoints[1]+"]);grid");
			
			//waveform
			MatlabInterfacer.sendCommand("subplot(212);plot([0:length(dallAudioSamples)-1]/11025,dallAudioSamples)");
			MatlabInterfacer.sendCommand("set(gca,'xtick',["+frelativeEndPoints[0]+" "+frelativeEndPoints[1]+"]);grid");			

			plotTIMITLabels(dataLocator, dallAudioSamples.length, nnumberOfSamplesToInclude);

			Pattern pattern = setOfPatterns.getPattern(i);
			hmmSet.findBestModelAndItsScore(pattern);
			int[][] nstates = hmmSet.getPathsOfNBestList();
			double[] dscores = hmmSet.getScoresInNBestList();
			int[] nindicesOfNBestList = hmmSet.getHMMIndicesInNBestList();
			
			Print.dialog(hmmSet.getHMMLabelsInNBestListAsString());
			IO.displayPartOfVector(dscores,100);

			int npositionOfBest = findPositionInList(nindicesOfNBestList, nhMMIndex);
			System.out.println("N-best list:");
			IO.DisplayVector(nindicesOfNBestList);

			if (npositionOfBest == -1) {
				End.throwError("Not found in N-best list");
			}

			int npositionOfSecond = -1;
			if (npositionOfBest != 0) {
				//error
				npositionOfSecond = 0;
			} else {
				//match
				npositionOfSecond = 1;
			}

			hMMViewer[nindicesOfNBestList[npositionOfBest]].composeImageAndSaveFiles(pattern,nstates[npositionOfBest],true);
			hMMViewer[nindicesOfNBestList[npositionOfSecond]].composeImageAndSaveFiles(pattern,nstates[npositionOfSecond],false);

			float[] fscoresFirst = hmmSet.getScoreEvolution(pattern,nstates[npositionOfBest],nhMMIndex);
			float[] fscoresSecond = hmmSet.getScoreEvolution(pattern,nstates[npositionOfSecond],nindicesOfNBestList[npositionOfSecond]);

			float[][] fscoresOfAllList = hmmSet.getScoreEvolutionForHMMList(pattern,nstates,nindicesOfNBestList);


			IO.writeVectortoASCIIFile("c:/temp/scores.txt",fscoresFirst);
			IO.writeMatrixtoASCIIFile("c:/temp/allscores.txt",fscoresOfAllList);

			//play only segment
			AudioPlayer.playScaled(labeledSpeech.getAudioFromGivenSegment(0));

			MatlabInterfacer.sendCommand("viewhmm");
			IO.pause();
			i++;
		}

                MatlabInterfacer.close();
	}

	private static void sendCommandToMatlab(String command) {
          MatlabInterfacer.sendCommandAndPrint(command);
	}

	//XXX I am gonna use only segment # 0
	private static void plotTIMITLabels(DataLocator dataLocator,
	int ntotalSamples, int nnumberOfSamplesToInclude) {
		//assume DTL has sentence file whose labels are in same directory
		String fileName = dataLocator.getFileName();

		//ak
		Print.dialog(dataLocator.toString());

		fileName = FileNamesAndDirectories.substituteExtension(fileName,"phn");
		Vector labels = IO.readVectorOfStringsFromFile(fileName);
		//get endpoints, well in the case I am working there is just one segment...
		int[][] nendPoints = dataLocator.getAllEndpoints();
		int nbegin = nendPoints[0][0] - nnumberOfSamplesToInclude;
		int nend =  nendPoints[0][1] + nnumberOfSamplesToInclude;
		int nfirstIndex = -1;
		int nlastIndex = -1;
		int nnumberOfLabels = labels.size();


		int[][] nsentenceEndpoints = new int[nnumberOfLabels][2];
		String[] sentenceLabels = new String[nnumberOfLabels];
		for (int i = 0; i < nnumberOfLabels; i++) {
			//read all line: label + endpoints
			sentenceLabels[i] = (String) labels.elementAt(i);

			//Print.dialog(sentenceLabels[i]);

			StringTokenizer stringTokenizer = new StringTokenizer(sentenceLabels[i]);
			nsentenceEndpoints[i][0] = Integer.parseInt(stringTokenizer.nextToken());
			nsentenceEndpoints[i][1] = Integer.parseInt(stringTokenizer.nextToken());
			//keep only label
			sentenceLabels[i] = stringTokenizer.nextToken();
			if (nfirstIndex == -1) {
				//still searching begin
				if (nsentenceEndpoints[i][0] >= nbegin) {
					nfirstIndex = i;
				}
			}
			if (nsentenceEndpoints[i][1] <= nend) {
				nlastIndex = i;
			}
		}
		int nnumberOfSegmentsOfInterest = nlastIndex - nfirstIndex + 1;
		int[][] nfinalEndpoints = new int[nnumberOfSegmentsOfInterest][2];
		String[] finalLabels = new String[nnumberOfSegmentsOfInterest];
		int nbaseSampleIndex = nsentenceEndpoints[nfirstIndex][0];
		for (int i = 0; i < nnumberOfSegmentsOfInterest; i++) {
			nfinalEndpoints[i][0] = nsentenceEndpoints[i + nfirstIndex][0]-nbaseSampleIndex;
			nfinalEndpoints[i][1] = nsentenceEndpoints[i + nfirstIndex][1]-nbaseSampleIndex;
			finalLabels[i] = sentenceLabels[i + nfirstIndex];
		}

		//ak
		Print.dialog(finalLabels[0]);
		IO.DisplayMatrix(nfinalEndpoints);

		plotLabelsInMatlab(nfinalEndpoints,finalLabels,ntotalSamples);
	}

	private static void plotLabelsInMatlab(int[][] nendpoints, String[] labels,
	int ntotalSamples) {

	double Srate=16000;
	//int Be=0;
	//int En=nendpoints[nendpoints.length-1][1];
	//xlm=[Be/Srate, En/Srate];

	sendCommandToMatlab("xlm = [0, " + (ntotalSamples / Srate) + "];");
	sendCommandToMatlab("set(gca,'Units','Pixels')");
	sendCommandToMatlab("AxesXY =get(gca,'Position');");

	sendCommandToMatlab("fac=AxesXY(3)/xlm(2);");
	sendCommandToMatlab("Xoffset = AxesXY(1);");
	//		  % X pixel offset

	for (int i = 0; i < labels.length; i++) {
		sendCommandToMatlab("x(1) = " + (nendpoints[i][0]/Srate));
		sendCommandToMatlab("x(2) = " + (nendpoints[i][1]/Srate));
		//x(1) = 0.128;
		//x(2) = 0.1549;

		sendCommandToMatlab("xpos = round(Xoffset+x(1)*fac);");
		//% coordinate in pixels
		sendCommandToMatlab("xwi= round((x(2)-x(1))*fac);");
		sendCommandToMatlab("if xwi<=0, xwi=2; end;");

		String color = null;
		if (i % 2 == 0) {
			color = "'b'";
		} else {
			color = "[0.5 0.5 0.5]";
		}
		sendCommandToMatlab("lbUp(" + (i+1) + ")=uicontrol('Style','text','Position',[xpos 5 xwi 20 ],'BackGroundColor'," + color + "," +
		"'ForeGroundColor','y','HorizontalAlignment','center','String','" + labels[i] + "');");
		}
	}

	private static int[] findRelativeEndpoints(int[] nendPoints, int nneighborSamplesToInclude, int ntotalSamples) {
		int nrealBegin = nendPoints[0] - nneighborSamplesToInclude;
		int nsamplesIncludedInBegin;
		if (nrealBegin < 0) {
			//included all available samples
			nsamplesIncludedInBegin = nendPoints[0];
		} else {
			nsamplesIncludedInBegin = nneighborSamplesToInclude;
		}
//		int nrealEnd = nendPoints[1] + nneighborSamplesToInclude;
//		int nsamplesIncludedInEnd;
//		if (nrealEnd > ntotalSamples-1) {
//			nsamplesIncludedInEnd = ntotalSamples-1;
//		} else {
//			nrealEnd = nrealBegin + nendPoints[1] - nendPoints[0];
//		}
		int[] out = new int[2];
		out[0] = nsamplesIncludedInBegin;
		out[1] = nsamplesIncludedInBegin + nendPoints[1] - nendPoints[0];
		return out;
	}

	private static int findPositionInList(int[] nindicesList, int ndesiredIndex) {
		for (int i = 0; i < nindicesList.length; i++) {
			if (nindicesList[i] == ndesiredIndex) {
				return i;
			}
		}
		return -1;
	}

	private static float[] convertToTime(int[] x, float sf) {
		sf = 1.0F/sf;
		float[] out = new float[x.length];
		for (int i = 0; i < x.length; i++) {
			out[i] = sf * x[i];
		}
		return out;
	}

}


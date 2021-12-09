package debug;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.StringTokenizer;

import javax.sound.sampled.AudioFormat;

import edu.ucsd.asr.Audio;
import edu.ucsd.asr.AudioFileReader;
import edu.ucsd.asr.HTKInterfacer;
import edu.ucsd.asr.MFCCPatternGenerator;
import edu.ucsd.asr.Pattern;
import edu.ucsd.asr.PatternGenerator;

public class HLSynToPattern {

	//Defines the number of parameters for the vector X (from synthesizer).
	private static final int nParam = 11;
	
	public static void main(String[] args) throws Exception {
		String fileName = "/home/jborges/workspace/ufpaspeech/classes/test.in";
		String dir = "test.hlsyn";
		String mfcFileName = "/home/jborges/workspace/ufpaspeech/classes/";
		String wavFileName = "0_zero.mfc";
		String saveInFile = "/home/jborges/workspace/ufpaspeech/classes/test.wav";
		float samplingFrequency = 11025F;
		
		if(args.length != 6) {
			System.err.println("Usage: java -cp . <HLSyn Input File> <Directory to save> <MFCC File Name> <HLSyn Pattern File Name> <Wav file name> <Sample Rate of Wav File>");
			System.exit(1);
		} else {
			fileName = args[0];
			dir = args[1];
			mfcFileName = args[2];
			saveInFile = args[3];
			wavFileName = args[4];
			samplingFrequency = Float.parseFloat(args[5]);
		}		
				
		Audio speech = getAudioFromWavFile(wavFileName , samplingFrequency);		
		PatternGenerator mfcPatternGenerator = generateHLsynBasedMfcPatternGenerator();
		Pattern mfcPattern = mfcPatternGenerator.getPattern(speech);
		HTKInterfacer.savePattern(mfcPattern, dir + mfcFileName, mfcPatternGenerator);

		BufferedReader hlsyn = new BufferedReader(new FileReader(fileName));
		hlsyn.readLine(); //Jumps first line of hlsyn file (header).
		int nLines = countNumberOfLines(fileName);
		
		float[][] frame = new float[nLines-1][nParam];
		initializeFrame(frame);
		updateFrameFromHlsynFile(hlsyn, frame);
		Pattern pattern = new Pattern(frame);
		short parmKind = (short) 9;
		int sampPeriod = (int) (1.0e7 / 11025); //period in nano seconds
		HTKInterfacer.writePattern(pattern.getParameters(), dir + saveInFile, sampPeriod, parmKind);
				
		hlsyn.close();
		//System.out.println("Done!");
	}
	
	private static int countNumberOfLines(String filename) throws Exception {
		
		BufferedReader br = new BufferedReader(new FileReader(filename));
		int counter = 0;
		
		while (br.readLine()!= null){
			counter++;
		}
		
		br.close();
		return counter;
	}
	
	//Returns an wave file information in an Audio format.
	private static Audio getAudioFromWavFile(String wavFileName , float sampRate) {	
		Audio speech = new Audio(AudioFileReader.getAudioInputStream(wavFileName));
		AudioFormat audioFormat = Audio.getDefaultAudioFormat();
		speech.setAudioFormat(audioFormat);
		speech.setSampleRate(11025F);
		//System.out.println("Playback.");
		//AudioPlayer.play(speech);
		return speech;	
	}

	//Just initializes a frame.
	private static void initializeFrame(float[][] frame) {
		for (int j = 0; j < frame.length; j++) {
			for (int i = 0; i < frame[0].length; i++) {
				frame[j][i] = 0;
			}
		}
	}
	
	//Update a frame from an Hlsyn file.
	private static void updateFrameFromHlsynFile(BufferedReader hlsyn, float[][] frame) throws Exception {
		int counter = 0;
		String line = "";
		while ((line = hlsyn.readLine())!= null){
			StringTokenizer st = new StringTokenizer(line);
			int nTokens = st.countTokens();
			
			if (nTokens != nParam) {
				System.err.println("nTokens != " + nParam + ". We must have problems.");
			}
			
			for (int i = 0; i < nTokens; i++) {
				frame[counter][i] = Integer.parseInt(st.nextToken());
			}
			counter ++;
		}
	}

	//Constructs a MFCCPatternGenerator with the following parameters.
	private static PatternGenerator generateHLsynBasedMfcPatternGenerator() {
		
		int nwindowLength = 80; //must be a power of 2
		int nwindowShift = 80;
		double dsamplingFrequency = 11025.0;
		int nFFTLength = 128;
		int nnumberOfMFCCParameters = 12;
		int nnumberofFilters = 16; //Default = 24.
		double dpreEmphasisCoefficient = 0.97;
        boolean oisLifteringEnabled = true;
        int nlifteringCoefficient = 22;
        double dminimumLogEnergyPerFrame = -1.0e+10;
        int nwindowSizeFactorForRegression = 2;
        double dsilenceThreshold = 50;
        boolean oisDCLevelExtractedForEachFrame = true;
        boolean oisAverageCepstrumSubtracted = false;
        boolean oisEnergyNormalizedByMaximumEnergy = false;
        boolean oisZeroThCepstralCoefficientCalculated = false;
        boolean oisMFCCIncludedInOutputParameters = true;
        boolean oisEnergyIncludedInOutputParameters = true;
        boolean oisFirstDerivativeIncludedInOutputParameters = true;
        boolean oisSecondDerivativeIncludedInOutputParameters = true;
		
        PatternGenerator mfcPatternGenerator = new 
		 		MFCCPatternGenerator(	nwindowLength,
		 								nwindowShift,
		 								dsamplingFrequency,
		 								nFFTLength,
		 								nnumberOfMFCCParameters,
		 								nnumberofFilters,
		 								dpreEmphasisCoefficient,
		 								oisLifteringEnabled,
		 								nlifteringCoefficient,
		 								dminimumLogEnergyPerFrame,
		 								nwindowSizeFactorForRegression,
		 								dsilenceThreshold,
		 								oisDCLevelExtractedForEachFrame,
		 								oisAverageCepstrumSubtracted,
		 								oisEnergyNormalizedByMaximumEnergy,
		 								oisZeroThCepstralCoefficientCalculated,
		 								oisMFCCIncludedInOutputParameters,
		 								oisEnergyIncludedInOutputParameters,
		 								oisFirstDerivativeIncludedInOutputParameters,
		 								oisSecondDerivativeIncludedInOutputParameters);
		
		return mfcPatternGenerator;
	}
	
}


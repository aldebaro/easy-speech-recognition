package debug;

import edu.ucsd.asr.*;

/**
 * Simulates the HMM as a generative model, that is, given an HMM, the code
 * generates observations.
 * 
 * @author Aldebaro
 * 
 */
public class HMMToyTest {

	public static void main(String[] args) {
		 //createSOPFromHMMOutput(args);
		createSOPFromMixtureOutput(args);
	}

	public static void createSOPFromMixtureOutput(String[] args) {
		if (args.length != 3 && args.length != 0) {
			System.out
					.println("HMMToyTest <output FEA file Name> <number of patterns> <maximum # of frames>");
			System.exit(1);
		}

		// String setOfPatternsFileName = "hmmtoy.SOP";
		// ContinuousHMM continuousHMM = createHMM4Gaussians();
		ContinuousHMM continuousHMM = createHMM2Gaussians();
		// ContinuousHMM continuousHMM =
		// createHMM2GaussiansGeneralTransitionMatrix();
		//ContinuousHMM continuousHMM = createHMM1Gaussian();
		//ContinuousHMM continuousHMM = createHMMWith1State();

		continuousHMM.isModelOk();
		int nspaceDimension = continuousHMM.getSpaceDimension(); 

		
		// default parameters
		String setOfPatternsFileName = "ff.FEA";
		int nnumberOfPatterns = 1;
		int nmaximumNumberOfFrames = 200;

		if (args.length == 0) {
			System.out.println("Assuming default parameters");
		} else {
			setOfPatternsFileName = args[0];
			nnumberOfPatterns = Integer.parseInt(args[1]);
			nmaximumNumberOfFrames = Integer.parseInt(args[2]);
		}

		PatternGenerator patternGenerator = new EmptyPatternGenerator(nspaceDimension);
		SetOfPatterns setOfPatterns = new SetOfPatterns(patternGenerator);
		
		
		//pro htk
		int sampPeriod = 100000; //in 100 ns
		short parmKind = 6; //858; 

		HMMToy hmmToy = new HMMToy(continuousHMM);

		for (int i = 0; i < nnumberOfPatterns; i++) {
			float[][] fsequence = hmmToy.getSequence(nmaximumNumberOfFrames);

			// write temporary text file for visualization:
			//String fileName = setOfPatternsFileName + i + ".txt";
			IO.writeMatrixtoASCIIFile("seq"+i+".txt", fsequence);
			//IO.writeMatrixtoASCIIFile(fileName, fsequence);
			IO.DisplayMatrix(fsequence);
			
//			IO.pause();

			setOfPatterns.addPattern(new Pattern(fsequence));
			
			//gerar compativel com HTK
			String fileName = setOfPatternsFileName + "." + i + ".htk";
			//String fileName = setOfPatternsFileName + "." + i + ".txt";
			//String fileName = setOfPatternsFileName + "." + i + ".wav";
			HTKInterfacer.writePattern(fsequence, fileName, sampPeriod, parmKind);
			
		}
		
		//setOfPatterns.printContents();
		//setOfPatterns.writeToFile(setOfPatternsFileName);
		System.exit(1);
	}

	public static void createSOPFromHMMOutput(String[] args) {

		if (args.length != 4) {
			System.out
					.println("HMMToyTest <output SOP file Name> <PTR or SOP file name to get PatternGenerator from> <number of patterns> <maximum # of frames>");
			System.exit(1);
		}

		// String setOfPatternsFileName =
		// "C:\\Users\\Aldebaro\\Databases\\Simple\\0_zero.SOP";
        //String setOfPatternsFileName = "hmmtoy.SOP";
		
		
		String setOfPatternsFileName = args[0];
		int nspaceDimension = Integer.parseInt(args[1]);
		int nnumberOfPatterns = Integer.parseInt(args[2]);
		int nmaximumNumberOfFrames = Integer.parseInt(args[3]);

		PatternGenerator patternGenerator = new EmptyPatternGenerator(nspaceDimension);
		
		//String setOfPatternsFileName = "hmmtoy.SOP";
		// ContinuousHMM continuousHMM = createHMM4Gaussians();
		// ContinuousHMM continuousHMM = createHMM2Gaussians();
		// ContinuousHMM continuousHMM =
		// createHMM2GaussiansGeneralTransitionMatrix();
		ContinuousHMM continuousHMM = createHMM1Gaussian();

		continuousHMM.isModelOk();
		
		SetOfPatterns setOfPatterns = new SetOfPatterns(patternGenerator);

		HMMToy hmmToy = new HMMToy(continuousHMM);

		
		
		//pro htk
		int sampPeriod = 100000; //in 100 ns
		short parmKind = 6; //858; 
				
		
		for (int i = 0; i < nnumberOfPatterns; i++) {
			float[][] fsequence = hmmToy.getSequence(nmaximumNumberOfFrames);
			// IO.WriteMatrixtoASCIIFile("seq.txt",fsequence);
			IO.writeMatrixtoASCIIFile("seq"+i+".txt", fsequence);
			setOfPatterns.addPattern(new Pattern(fsequence));
			//gerar compativel com HTK
			String fileName = setOfPatternsFileName + "." + i + ".htk";
			//String fileName = setOfPatternsFileName + "." + i + ".txt";
			//String fileName = setOfPatternsFileName + "." + i + ".wav";
			HTKInterfacer.writePattern(fsequence, fileName, sampPeriod, parmKind);
		}
		//System.out.println(setOfPatternsFileName);
		//setOfPatterns.writeToFile(setOfPatternsFileName);
		
		
	}

	private static ContinuousHMM createHMM4Gaussians() {
		float[] ones = new float[39];
		for (int i = 0; i < ones.length; i++) {
			ones[i] = 1.0F;
		}
		float[] twos = new float[39];
		for (int i = 0; i < ones.length; i++) {
			twos[i] = 2.0F;
		}
		float[] threes = new float[39];
		for (int i = 0; i < ones.length; i++) {
			threes[i] = 3.0F;
		}
		float[] fours = new float[39];
		for (int i = 0; i < ones.length; i++) {
			fours[i] = 4.0F;
		}

		DiagonalCovarianceGaussianPDF gaussianPDF1 = new DiagonalCovarianceGaussianPDF(
				ones, ones);
		DiagonalCovarianceGaussianPDF gaussianPDF2 = new DiagonalCovarianceGaussianPDF(
				twos, ones);
		DiagonalCovarianceGaussianPDF gaussianPDF3 = new DiagonalCovarianceGaussianPDF(
				threes, ones);
		DiagonalCovarianceGaussianPDF gaussianPDF4 = new DiagonalCovarianceGaussianPDF(
				fours, ones);

		// because mixture is not cloning...
		DiagonalCovarianceGaussianPDF[] gaussians1 = { gaussianPDF1,
				gaussianPDF2, gaussianPDF3, gaussianPDF4 };
		DiagonalCovarianceGaussianPDF[] gaussians2 = {
				(DiagonalCovarianceGaussianPDF) gaussianPDF1.clone(),
				(DiagonalCovarianceGaussianPDF) gaussianPDF2.clone(),
				(DiagonalCovarianceGaussianPDF) (DiagonalCovarianceGaussianPDF) gaussianPDF3
						.clone(),
				(DiagonalCovarianceGaussianPDF) gaussianPDF4.clone() };
		DiagonalCovarianceGaussianPDF[] gaussians3 = {
				(DiagonalCovarianceGaussianPDF) gaussianPDF1.clone(),
				(DiagonalCovarianceGaussianPDF) gaussianPDF2.clone(),
				(DiagonalCovarianceGaussianPDF) (DiagonalCovarianceGaussianPDF) gaussianPDF3
						.clone(),
				(DiagonalCovarianceGaussianPDF) gaussianPDF4.clone() };

		float[] fweights = { 0.25F, 0.25F, 0.25F, 0.25F };

		MixtureOfGaussianPDFs mixtureOfGaussianPDFs1 = new MixtureOfGaussianPDFs(
				gaussians1, fweights, true);
		MixtureOfGaussianPDFs mixtureOfGaussianPDFs2 = new MixtureOfGaussianPDFs(
				gaussians2, fweights, true);
		MixtureOfGaussianPDFs mixtureOfGaussianPDFs3 = new MixtureOfGaussianPDFs(
				gaussians3, fweights, true);

		MixtureOfGaussianPDFs[] mixtures = { mixtureOfGaussianPDFs1,
				mixtureOfGaussianPDFs2, mixtureOfGaussianPDFs3 };

		float[][] ftransition = { { 0, 1, 0, 0, 0 }, { 0, 0.5F, 0.5F, 0, 0 },
				{ 0, 0, 0.5F, 0.5F, 0 }, { 0, 0, 0, 0.5F, 0.5F },
				{ 0, 0, 0, 0, 1 } };

		// IO.DisplayMatrix(ftransition);

		return new ContinuousHMM(ftransition, mixtures,
				HMM.Topology.LEFTRIGHT_NO_SKIPS);
	}

	private static ContinuousHMM createHMM2Gaussians() {

		float[] mean1 = new float[1];//[39];
		for (int i = 0; i < mean1.length; i++) {
			mean1[i] = 1.0F;
		}
		//mean1[0] = 0.5F;
		//mean1[2] = 0.2F;
		//mean1[31] = 0.4F;
		//mean1[38] = -0.5F;

		float[] mean2 = new float[1];//[39];
		for (int i = 0; i < mean2.length; i++) {
			mean2[i] = 10.0F;
		}
		//mean2[0] = -0.5F;
		//mean2[1] = 0.3F;
		//mean2[7] = 8;
		//mean2[38] = 0.5F;
		
		float[] mean3 = new float[1];//[39];
		for (int i = 0; i < mean3.length; i++) {
			mean3[i] = 50.0F;
		}
		//mean2[0] = -0.5F;
		//mean2[1] = 0.3F;
		//mean2[7] = 8;
		//mean2[38] = 0.5F;
		
		float[] mean4 = new float[1];//[39];
		for (int i = 0; i < mean4.length; i++) {
			mean4[i] = 100.0F;
		}
		//mean2[0] = -0.5F;
		//mean2[1] = 0.3F;
		//mean2[7] = 8;
		//mean2[38] = 0.5F;
		
		float[] mean5 = new float[1];//[39];
		for (int i = 0; i < mean5.length; i++) {
			mean5[i] = 200.0F;
		}
		//mean2[0] = -0.5F;
		//mean2[1] = 0.3F;
		//mean2[7] = 8;
		//mean2[38] = 0.5F;
		
		float[] mean6 = new float[1];//[39];
		for (int i = 0; i < mean6.length; i++) {
			mean6[i] = 300.0F;
		}
		//mean2[0] = -0.5F;
		//mean2[1] = 0.3F;
		//mean2[7] = 8;
		//mean2[38] = 0.5F;
		
		
		float[] var1 = new float[1];//[39];
		for (int i = 0; i < mean1.length; i++) {
			var1[i] = 1.0F;
		}
		//var1[0] = 2;
		//var1[5] = 5;
		//var1[38] = 4;
		
		float[] var2 = new float[1];//[39];
		for (int i = 0; i < mean2.length; i++) {
			var2[i] = 5.0F;
		}
		//var2[0] = 1;
		//var2[4] = 8;
		//var2[38] = 1;

		float[] var3 = new float[1];//[39];
		for (int i = 0; i < mean3.length; i++) {
			var3[i] = 10.0F;
		}
		//var2[0] = 1;
		//var2[4] = 8;
		//var2[38] = 1;

				
		DiagonalCovarianceGaussianPDF gaussianPDF1 = new DiagonalCovarianceGaussianPDF(
				mean1, var1);
		DiagonalCovarianceGaussianPDF gaussianPDF2 = new DiagonalCovarianceGaussianPDF(
				mean2, var1);
		DiagonalCovarianceGaussianPDF gaussianPDF3 = new DiagonalCovarianceGaussianPDF(
				mean3, var2);
		DiagonalCovarianceGaussianPDF gaussianPDF4 = new DiagonalCovarianceGaussianPDF(
				mean4, var2);
		DiagonalCovarianceGaussianPDF gaussianPDF5 = new DiagonalCovarianceGaussianPDF(
				mean5, var3);
		DiagonalCovarianceGaussianPDF gaussianPDF6 = new DiagonalCovarianceGaussianPDF(
				mean6, var3);

		
		// because mixture is not cloning...
		DiagonalCovarianceGaussianPDF[] gaussians1 = { gaussianPDF1,
				gaussianPDF2 };
		DiagonalCovarianceGaussianPDF[] gaussians2 = {
				(DiagonalCovarianceGaussianPDF) gaussianPDF3.clone(),
				(DiagonalCovarianceGaussianPDF) gaussianPDF4.clone() };
		DiagonalCovarianceGaussianPDF[] gaussians3 = {
				(DiagonalCovarianceGaussianPDF) gaussianPDF5.clone(),
				(DiagonalCovarianceGaussianPDF) gaussianPDF6.clone() };

		float[] fweights1 = { 0.6F, 0.4F };
		float[] fweights2 = { 0.8F, 0.2F };
		float[] fweights3 = { 0.3F, 0.7F };

		MixtureOfGaussianPDFs mixtureOfGaussianPDFs1 = new MixtureOfGaussianPDFs(
				gaussians1, fweights1, true);
		MixtureOfGaussianPDFs mixtureOfGaussianPDFs2 = new MixtureOfGaussianPDFs(
				gaussians2, fweights2, true);
		MixtureOfGaussianPDFs mixtureOfGaussianPDFs3 = new MixtureOfGaussianPDFs(
				gaussians3, fweights3, true);

		MixtureOfGaussianPDFs[] mixtures = { mixtureOfGaussianPDFs1,
				mixtureOfGaussianPDFs2, mixtureOfGaussianPDFs3 };

		float[][] ftransition = { { 0, 1, 0, 0, 0 }, { 0, 0.999F, 0.001F, 0, 0 },
				{ 0, 0, 0.999F, 0.001F, 0 }, { 0, 0, 0, 0.999F, 0.001F },
				{ 0, 0, 0, 0, 1 } };

		// IO.DisplayMatrix(ftransition);

		return new ContinuousHMM(ftransition, mixtures,
				HMM.Topology.LEFTRIGHT_NO_SKIPS);
	}

	private static ContinuousHMM createHMM2GaussiansGeneralTransitionMatrix() {

		float[] mean1 = new float[39];
		for (int i = 0; i < mean1.length; i++) {
			mean1[i] = 1.0F;
		}
		mean1[0] = 0.5F;
		mean1[2] = 0.2F;
		mean1[31] = 0.4F;
		mean1[38] = -0.5F;

		float[] mean2 = new float[39];
		for (int i = 0; i < mean2.length; i++) {
			mean2[i] = 1.0F;
		}
		mean2[0] = -0.5F;
		mean2[1] = 0.3F;
		mean2[7] = 8;
		mean2[38] = 0.5F;

		// same as mean 1... :(
		float[] mean3 = new float[39];
		for (int i = 0; i < mean3.length; i++) {
			mean3[i] = 1.0F;
		}
		mean3[0] = 0.5F;
		mean3[2] = 0.2F;
		mean3[31] = 0.4F;
		mean3[38] = -0.5F;

		float[] var1 = new float[39];
		for (int i = 0; i < mean1.length; i++) {
			var1[i] = 1.0F;
		}
		var1[0] = 2;
		var1[5] = 5;
		var1[38] = 4;

		float[] var2 = new float[39];
		for (int i = 0; i < mean2.length; i++) {
			var2[i] = 2.0F;
		}
		var2[0] = 1;
		var2[4] = 8;
		var2[38] = 1;

		float[] var3 = new float[39];
		for (int i = 0; i < mean1.length; i++) {
			var3[i] = 1.0F;
		}
		var3[0] = 2;
		var3[1] = 5;
		var3[3] = 40;
		var3[4] = 50;
		var3[5] = 5;
		var3[10] = 3;
		var3[13] = 4;
		var3[35] = 10;
		var3[36] = 10;
		var3[37] = 10;
		var3[38] = 40;

		DiagonalCovarianceGaussianPDF gaussianPDF1 = new DiagonalCovarianceGaussianPDF(
				mean1, var1);
		DiagonalCovarianceGaussianPDF gaussianPDF2 = new DiagonalCovarianceGaussianPDF(
				mean2, var2);
		DiagonalCovarianceGaussianPDF gaussianPDF3 = new DiagonalCovarianceGaussianPDF(
				mean3, var3);

		// because mixture is not cloning...
		DiagonalCovarianceGaussianPDF[] gaussians1 = { gaussianPDF1,
				gaussianPDF2 };
		DiagonalCovarianceGaussianPDF[] gaussians2 = {
				(DiagonalCovarianceGaussianPDF) gaussianPDF1.clone(),
				(DiagonalCovarianceGaussianPDF) gaussianPDF2.clone() };
		DiagonalCovarianceGaussianPDF[] gaussians3 = { gaussianPDF3,
				(DiagonalCovarianceGaussianPDF) gaussianPDF2.clone() };

		float[] fweights = { 0.4F, 0.6F };

		MixtureOfGaussianPDFs mixtureOfGaussianPDFs1 = new MixtureOfGaussianPDFs(
				gaussians1, fweights, true);
		MixtureOfGaussianPDFs mixtureOfGaussianPDFs2 = new MixtureOfGaussianPDFs(
				gaussians2, fweights, true);
		MixtureOfGaussianPDFs mixtureOfGaussianPDFs3 = new MixtureOfGaussianPDFs(
				gaussians3, fweights, true);

		MixtureOfGaussianPDFs[] mixtures = { mixtureOfGaussianPDFs1,
				mixtureOfGaussianPDFs2, mixtureOfGaussianPDFs3 };

		float[][] ftransition = { { 0, 0.6F, 0.2F, 0.2F, 0 },
				{ 0, 0.6F, 0.2F, 0.2F, 0 }, { 0, 0, 0.6F, 0.4F, 0 },
				{ 0, 0, 0, 0.8F, 0.2F }, { 0, 0, 0, 0, 1 } };

		// IO.DisplayMatrix(ftransition);

		return new ContinuousHMM(ftransition, mixtures,
				HMM.Topology.DESCRIBED_BY_TRANSITION_MATRIX);
	}

	private static ContinuousHMM createHMM1Gaussian() {
		float[] half = new float[1];//[39];
		for (int i = 0; i < half.length; i++) {
			half[i] = 0.5F;
		}
		float[] twos = new float[1];//[39];
		for (int i = 0; i < half.length; i++) {
			twos[i] = 2.0F;
		}
		float[] fives = new float[1];//[39];
		for (int i = 0; i < half.length; i++) {
			fives[i] = 20.0F;
		}
		float[] eights = new float[1];//[39];
		for (int i = 0; i < half.length; i++) {
			eights[i] = 200.0F;
		}

		DiagonalCovarianceGaussianPDF gaussianPDF1 = new DiagonalCovarianceGaussianPDF(
				twos, half);
		DiagonalCovarianceGaussianPDF gaussianPDF2 = new DiagonalCovarianceGaussianPDF(
				fives, half);
		DiagonalCovarianceGaussianPDF gaussianPDF3 = new DiagonalCovarianceGaussianPDF(
				eights, half);

		DiagonalCovarianceGaussianPDF[] gaussians1 = { gaussianPDF1 };
		DiagonalCovarianceGaussianPDF[] gaussians2 = { gaussianPDF2 };
		DiagonalCovarianceGaussianPDF[] gaussians3 = { gaussianPDF3 };

		float[] fweights = { 1 };

		MixtureOfGaussianPDFs mixtureOfGaussianPDFs1 = new MixtureOfGaussianPDFs(
				gaussians1, fweights, true);
		MixtureOfGaussianPDFs mixtureOfGaussianPDFs2 = new MixtureOfGaussianPDFs(
				gaussians2, fweights, true);
		MixtureOfGaussianPDFs mixtureOfGaussianPDFs3 = new MixtureOfGaussianPDFs(
				gaussians3, fweights, true);

		MixtureOfGaussianPDFs[] mixtures = { mixtureOfGaussianPDFs1,
				mixtureOfGaussianPDFs2, mixtureOfGaussianPDFs3 };

		float[][] ftransition = { { 0, 1, 0, 0, 0 }, { 0, 0.999F, 0.001F, 0, 0 },
				{ 0, 0, 0.999F, 0.001F, 0 }, { 0, 0, 0, 0.999F, 0.001f },
				{ 0, 0, 0, 0, 1 } };

		// IO.DisplayMatrix(ftransition);

		return new ContinuousHMM(ftransition, mixtures,
				HMM.Topology.LEFTRIGHT_NO_SKIPS);
	}

	private static ContinuousHMM createHMMWith1State() {

		float[] fmean = { 0 };
		float[] fvar1 = { 1 };
		float[] fvar2 = { 20 };

		DiagonalCovarianceGaussianPDF gaussianPDF1 = new DiagonalCovarianceGaussianPDF(
				fmean, fvar1);
		DiagonalCovarianceGaussianPDF gaussianPDF2 = new DiagonalCovarianceGaussianPDF(
				fmean, fvar2);

		DiagonalCovarianceGaussianPDF[] gaussians1 = { gaussianPDF1,
				gaussianPDF2 };
		// DiagonalCovarianceGaussianPDF[] gaussians2 = {gaussianPDF2};
		// DiagonalCovarianceGaussianPDF[] gaussians3 = {gaussianPDF3};

		float[] fweights = { 0.5F, 0.5F };

		MixtureOfGaussianPDFs mixtureOfGaussianPDFs1 = new MixtureOfGaussianPDFs(
				gaussians1, fweights, true);
		// MixtureOfGaussianPDFs mixtureOfGaussianPDFs2 = new
		// MixtureOfGaussianPDFs(gaussians2,fweights,true);
		// MixtureOfGaussianPDFs mixtureOfGaussianPDFs3 = new
		// MixtureOfGaussianPDFs(gaussians3,fweights,true);

		MixtureOfGaussianPDFs[] mixtures = { mixtureOfGaussianPDFs1 };
		// mixtureOfGaussianPDFs2,
		// mixtureOfGaussianPDFs3};

		float[][] ftransition = { { 0, 1, 0 }, { 0, 0.8F, 0.2F }, { 0, 0, 1 } };

		// IO.DisplayMatrix(ftransition);

		return new ContinuousHMM(ftransition, mixtures,
				HMM.Topology.LEFTRIGHT_NO_SKIPS);
	}

}

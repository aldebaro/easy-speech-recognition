package debug;

import edu.ucsd.asr.*;

public class SampleGeneratorTest {

	public static void main(String[] args) {

		System.out.println("START");

		double[] mean = {4.0, -2.0};
		double[] variance = {10.0, 1.0};
		DiagonalGaussianSampleGenerator
			diagonalGaussianSampleGenerator =
											 new DiagonalGaussianSampleGenerator(mean,variance);
		double[][] dsequence = diagonalGaussianSampleGenerator.getDoubleSequence(10000);
		//IO.DisplayMatrix(dsequence);
		IO.writeMatrixtoASCIIFile("gm4v5.txt",dsequence);

		double[] probabilities = {0.3, 0.45, 0.25};
		double[] values = {3, 4, 5};
		PMFSampleGenerator
			discretePDFSampleGenerator = new PMFSampleGenerator(probabilities,
																		values);
		double[] dseq2 = discretePDFSampleGenerator.getSequenceOfDoubleSamples(10000);
		IO.writeVectortoASCIIFile("discrete.txt",dseq2);

		System.out.println("END");

	}
}


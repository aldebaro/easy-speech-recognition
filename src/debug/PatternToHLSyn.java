package debug;

import edu.ucsd.asr.HTKInterfacer;
import edu.ucsd.asr.Pattern;

public class PatternToHLSyn {
	public static void main(String[] args)  throws Exception {
		
		String hTKFileName = "/home/jborges/esthlsyn/hldata10.esthlsyn";//= args[0];
		
		Pattern pattern = HTKInterfacer.getPatternFromFile(hTKFileName);
		
		int N = pattern.getNumOfFrames();
		
		System.out.println("AG  AL  AB  AN  UE  FO  F1  F2  F3  F4  PS  DC  AP");
		
		for (int i = 0; i < N; i++) {
			float[] x = pattern.getParametersOfGivenFrame(i);
			
			for (int j = 0; j < x.length; j++) {
				
				System.out.print((int) x[j] + " ");
				
				if (j == 2) {
					System.out.print( 0 + " ");	
				}
				
			}
			
			System.out.print("20");
			System.out.println();
		}
		
		
		
	}
}

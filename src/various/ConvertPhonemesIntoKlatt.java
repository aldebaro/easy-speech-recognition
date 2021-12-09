package various;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.Hashtable;
import java.util.StringTokenizer;

import edu.ucsd.asr.FileNamesAndDirectories;
import edu.ucsd.asr.IO;

public class ConvertPhonemesIntoKlatt {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			System.out.println("Usage: <phoneme file>");
			System.exit(-1);
		}		
		String fileName = args[0];
		BufferedReader bufferedReader = IO.openBufferedReader(fileName);		

		//initialize table with Klatt parameters saved in files
		int nnumberOfPhonemes = KlattFromDectalkAnalysis.m_phonemes.length;
		Hashtable hashtable = new Hashtable();
		for (int i = 0; i < nnumberOfPhonemes; i++) {
			String phoneme = KlattFromDectalkAnalysis.m_phonemes[i];
			String parametersFileName = "kl_par_" + phoneme + ".txt";
			String[] parameters = IO.readArrayOfStringsFromFile(parametersFileName);
			//create string concatenating all parameters
			StringBuffer stringBuffer = new StringBuffer();
			for (int j = 0; j < parameters.length; j++) {
				stringBuffer.append(parameters[j] + IO.m_NEW_LINE);
			}
			String allParameters = stringBuffer.toString();
			hashtable.put(phoneme, allParameters);
		}

		//now do the mapping from phones to Klatt parameters
		String line = null;
		String outputFileName = FileNamesAndDirectories.substituteExtension(fileName, "auto");
		BufferedWriter bufferedWriter = IO.openBufferedWriter(outputFileName);
		while ( (line = bufferedReader.readLine()) != null) {
			StringTokenizer stringTokenizer = new StringTokenizer(line);
			int nbegin = Integer.parseInt(stringTokenizer.nextToken());
			int nend = Integer.parseInt(stringTokenizer.nextToken());
			String phoneme = stringTokenizer.nextToken();
			//get from table:
			String allParameters = (String) hashtable.get(phoneme);
			bufferedWriter.append(allParameters);			
		}
		IO.closeBufferedReader(bufferedReader);	
		IO.closeBufferedWriter(bufferedWriter);
	}

}

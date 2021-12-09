package various;

import edu.ucsd.asr.*;

public class StripKlattZeroSamples {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String dir = args[0];
		String extension = args[1];
		
		DirectoryTree directoryTree = new DirectoryTree(dir, extension);
		String[] files = directoryTree.getFilesAsStrings();
		if (files==null || files.length < 1) {
			System.err.println("Could not find ." + extension +
					" files under directory " + dir);
			System.exit(1);
		}
		for (int i = 0; i < files.length; i++) {
			System.out.println("Processing: " + files[i]);
			Audio audio = new Audio(files[i]);
			int[] nsamples = audio.getAudioDataAsIntegers();
			//find begin
			int begin = 0;
			for (int j = 0; j < nsamples.length; j++) {
				if (nsamples[j] > 0) {
					begin = j;
					break;
				}
			}
			//find end
			int end = nsamples.length-1;
			for (int j = nsamples.length-1; j > begin+1; j--) {
				if (nsamples[j] > 0) {
					end = j;
					break;
				}
			}
			int N = end - begin + 1;
			if (begin+N > nsamples.length) {
				N = nsamples.length - begin;
			}
			int[] output = new int[N];
			for (int j = 0; j < output.length; j++) {
				output[j] = nsamples[j + begin];
			}
			Audio newAudio = new Audio(output, audio.getAudioFormat());
			String outputFileName = FileNamesAndDirectories.getPathFromFileName(files[i]) +
			"nosil" + FileNamesAndDirectories.getFileNameFromPath(files[i]);
			AudioFileWriter.writeAudioFile(newAudio,
					//files[i]); //overwrite
					outputFileName); //new files
		}
		
	}

}

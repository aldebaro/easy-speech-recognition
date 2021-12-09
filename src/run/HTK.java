package run;

import edu.ucsd.asr.*;
import java.util.Locale;

/**
 * Title: Spock Description: Speech recognition Copyright: Copyright (c) 2001
 * Company: UCSD
 * 
 * @author Aldebaro Klautau
 * @version 4.0
 */

public class HTK {
	public static void main(String[] args) {
		if (args.length != 1) {
			System.out.println("Usage: java run.HTK <.TRN file>");
			System.exit(1);
		}

		// make sure the numbers are written with decimal points (not commas as
		// in Portuguese)
		Locale.setDefault(Locale.US);

		String tRNFile = args[0];

		HeaderProperties headerProperties = FileWithHeaderReader
				.getHeaderProperties(tRNFile);
		String globalFlags = headerProperties
				.getPropertyAndExitIfKeyNotFound("HTK.GlobalFlags");
		String inputDirectoryWithConfigurationFiles = headerProperties
				.getPropertyAndExitIfKeyNotFound("HTK.InputDirectoryWithConfigurationFiles");
		boolean oshouldUseBigrams = headerProperties
				.getBooleanPropertyAndExitIfKeyNotFound("HTK.oshouldUseBigrams");

		HTKToolsCaller hTKToolsCaller = new HTKToolsCaller(
				globalFlags, inputDirectoryWithConfigurationFiles,
				oshouldUseBigrams, tRNFile);

		// hTKToolsCaller.convertAllHMMFilesToJAR();

		hTKToolsCaller.runSimulation();
	}
}
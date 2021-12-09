package debug;

//import various.*;
import java.io.*;

import edu.ucsd.asr.*;

public class HTKToolsCallerTest {

	public static void main(String[] args) {

		if (args.length != 1) {
			System.out.println("Usage: HTKToolsCallerTest <.TRN file>");
			System.exit(1);
		}
		String tRNFile = args[0];

		String globalFlags = "-A -T 0";
		String inputDirectoryWithConfigurationFiles = ".";
		//"-A -D -T 0";
		HTKToolsCaller hTKToolsCaller = new HTKToolsCaller(//trainingDatabaseRootDirectory,
						 globalFlags,
						 inputDirectoryWithConfigurationFiles,
						 true,
						 tRNFile);
		//hTKToolsCaller.convertAllHMMFilesToJAR();
		hTKToolsCaller.runSimulation();
	}
}


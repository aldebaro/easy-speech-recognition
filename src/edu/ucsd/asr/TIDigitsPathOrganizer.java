package edu.ucsd.asr;

import java.util.StringTokenizer;

/**Helps to interpret a TIDigits path.
 * @author Aldebaro Klautau
 * @version 1.4 - 02/17/00
 */
public class TIDigitsPathOrganizer extends PathOrganizer {

	private String m_corpus;
	private String m_usage;
	private String m_speakerType;
	private String m_speakerID;
	private String[] m_digits;
	private String m_production;
	private String m_fileType;
        //this needs to be in sync with TableOfLabels
	private static final String[][] m_table = {{"z"},{"1"},{"2"},{"3"},
		{"4"},{"5"},{"6"},{"7"},{"8"},{"9"},{"o"}};
	private static final TableOfLabels m_tableOfLabels = new TableOfLabels(m_table);
        private static final TableOfLabels m_tableOfLabelsForTIDIGITS = new TableOfLabels(TableOfLabels.Type.TIDIGITS);

	public TIDigitsPathOrganizer(String tidigitsPathOrUniqueFile) {
		if (!parseUniqueFileWithCompleteTIDIGITSPath(tidigitsPathOrUniqueFile)) {
			parsePath(tidigitsPathOrUniqueFile);
		}
	}

	//should modify but now it's aborting if path is not ok,
	//so I can return true always
	public boolean isPathOk() {
		return true;
	}

	private void parsePath(String segmentInfo) {
		segmentInfo = segmentInfo.toLowerCase();

		segmentInfo = FileNamesAndDirectories.replaceBackSlashByForward(segmentInfo);

		//System.out.println(segmentInfo);
		int nfirstBlankSpaceIndex = segmentInfo.indexOf(" ");
		String temporaryPath;
		if (nfirstBlankSpaceIndex != -1) {
			temporaryPath = segmentInfo.substring(0,nfirstBlankSpaceIndex);
		} else {
			temporaryPath = segmentInfo;
		}
		temporaryPath = temporaryPath.toLowerCase();
		//System.out.println(temporaryPath);
		//i'm not assuming the user is reading from the TIDIGIT CD.
		//the user can have created a tree like c:/tidigits/tidigits/tidigits/test/...
		//so, find the last occurence of "tidigits" and strip off its prefix:
		int nlastTimitIndex = temporaryPath.lastIndexOf("tidigits");
		if (nlastTimitIndex == -1) {
			System.out.println("Error: couldn't find directory TIDIGIT in the path " + temporaryPath);
			System.exit(1);
		}
		String path = temporaryPath.substring(nlastTimitIndex);

		//StringTokenizer stringTokenizer = new StringTokenizer(path,System.getProperties().getProperty("file.separator"));
		StringTokenizer stringTokenizer = new StringTokenizer(path,"/");

		m_corpus = stringTokenizer.nextToken();
		if (!m_corpus.equals("tidigits")) {
			generateErrorMessage(m_corpus);
		}
		m_usage = stringTokenizer.nextToken();
		if (! (m_usage.equals("train") | m_usage.equals("test") ) ) {
			generateErrorMessage(m_usage);
		}

		m_speakerType = stringTokenizer.nextToken();
		if (! (m_speakerType.equals("man") |
				 m_speakerType.equals("woman") |
				 m_speakerType.equals("boy") |
				 m_speakerType.equals("girl") ) ) {
			generateErrorMessage(m_speakerType);
		}

		m_speakerID = stringTokenizer.nextToken();
		if (m_speakerID.length() != 2) {
			generateErrorMessage(m_speakerID);
		}

		String sentenceTemporary = stringTokenizer.nextToken();
		m_fileType = sentenceTemporary.substring(sentenceTemporary.length() - 3);
		if (! m_fileType.equals("wav") ) {
			generateErrorMessage(m_fileType);
		}
				
		//take out the file extension
		String sentence = sentenceTemporary.substring(0, sentenceTemporary.length() - 4);
		
		//ak - gambiarra visto que nao tenho o TIDIGITS!!!! XXX
		m_digits = new String[1];
		m_digits[0] = sentence.substring(sentence.length()-1, sentence.length());
		
		if (true) {
			return;
		}
		//fim da gambiarra

		m_production = sentence.substring(sentence.length() - 1);
		if (! ( m_production.equals("a") |
				 m_production.equals("b") ) ) {
			generateErrorMessage(m_production);
		}

		String digitString = sentence.substring(0, sentence.length() - 1);
		int nnumberOfDigits = digitString.length();
		if (( nnumberOfDigits < 1 ) | (nnumberOfDigits > 7) ) {
			System.out.println("Invalid (outside [1, 7]) number of digits in " +
								 segmentInfo + ".");
			System.exit(1);
		}

		m_digits = new String[nnumberOfDigits];
		for (int i=0; i<nnumberOfDigits; i++) {
			m_digits[i] = (new Character(digitString.charAt(i))).toString();
			if (! (m_digits[i].equals("o") |
					 m_digits[i].equals("z") |
					 m_digits[i].equals("1") |
					 m_digits[i].equals("2") |
					 m_digits[i].equals("3") |
					 m_digits[i].equals("4") |
					 m_digits[i].equals("5") |
					 m_digits[i].equals("6") |
					 m_digits[i].equals("7") |
					 m_digits[i].equals("8") |
					 m_digits[i].equals("9") ) ) {
				generateErrorMessage(m_digits[i]);
			}
		}

		m_production = sentence.substring(sentence.length() - 1);
		if (! ( m_production.equals("a") |
				 m_production.equals("b") ) ) {
			generateErrorMessage(m_production);
		}

	}

	public String toString() {
		//not using m_corpus
		String result =  m_usage + "/" + m_speakerType + "/" +
						 m_speakerID + "/";
		for (int i=0; i<m_digits.length; i++) {
			result += m_digits[i];
		}
		result += m_production + "." + m_fileType;
		return result;
	}

	public String getUniqueName() {
		String result = m_usage + m_speakerType + m_speakerID;
		for (int i=0; i<m_digits.length; i++) {
			result += m_digits[i];
		}
		result += m_production + "." + m_fileType;
		return result;
	}

	void generateErrorMessage(String wrongToken) {
		System.out.println("Invalid token when parsing TIDigits path:\n Original string: " + wrongToken);
		System.out.print(".\n It was parsed as:\n");
		System.out.print(m_corpus + " " + m_usage + " " + m_speakerType + " " +
						 m_speakerID + " ");
		if (m_digits != null) {
			for (int i=0; i<m_digits.length; i++) {
				System.out.print(m_digits[i] + " ");
			}
		}
		System.out.println(m_production + " " + m_fileType);
		End.throwError("Error parsing " + wrongToken);
	}

	public String getCorpus() {
		return m_corpus;
	}
	public String getUsage() {
		return m_usage;
	}
	public String getSpeakerType() {
		return m_speakerType;
	}
	public String getSpeakerID() {
		return m_speakerID;
	}
	public int getNumberOfDigits() {
		return m_digits.length;
	}
	public String[] getDigits() {
		return m_digits;
	}

        /**
         * Changes z to "zero", 1 to "one" and so on.
         */
	public String[] getDigitsAsInTableOfLabels() {
          String[] out = new String[m_digits.length];
          for (int i = 0; i < out.length; i++) {
            int n = m_tableOfLabels.getEntry(m_digits[i]);
            out[i] = m_tableOfLabelsForTIDIGITS.getFirstLabel(n);
          }
          return out;
	}

	public String getFirstDigit() {
		if (m_digits != null) {
			return m_digits[0];
		} else {
			return null;
		}
	}
	public String getProduction() {
		return m_production;
	}
	public String getFileType() {
		return m_fileType;
	}

		private boolean parseUniqueFileWithCompleteTIDIGITSPath(String originalFileName) {
			m_fileType = FileNamesAndDirectories.getExtension(originalFileName);
                        //want to decode FEA files, so disable below
			//if (!m_fileType.equalsIgnoreCase("wav")) {
			//	return false;
			//}

			originalFileName = FileNamesAndDirectories.deleteExtension(originalFileName);

				//do not want to alter letter's case so keep original name
				originalFileName = FileNamesAndDirectories.getFileNameFromPath(originalFileName);
				//but convert to lower case a temporary variable to make comparisons easier
				String fileName = originalFileName.toLowerCase();

				if (fileName.startsWith("test")) {
						//m_usage = "test";
						m_usage = originalFileName.substring(0,4);
						fileName = fileName.substring(4);
						originalFileName = originalFileName.substring(4);
				} else if (fileName.startsWith("train")) {
						//m_usage = "train";
						m_usage = originalFileName.substring(0,5);
						fileName = fileName.substring(5);
						originalFileName = originalFileName.substring(5);
				} else {
						return false;
				}

		if (fileName.startsWith("man") || fileName.startsWith("boy")) {
			m_speakerType = originalFileName.substring(0,3);
			fileName = fileName.substring(3);
			originalFileName = originalFileName.substring(3);
		} else if (fileName.startsWith("woman")) {
			m_speakerType = originalFileName.substring(0,5);
			fileName = fileName.substring(5);
			originalFileName = originalFileName.substring(5);
		} else if (fileName.startsWith("girl")) {
			m_speakerType = originalFileName.substring(0,4);
			fileName = fileName.substring(4);
			originalFileName = originalFileName.substring(4);
		} else {
			return false;
		}

		m_speakerID = originalFileName.substring(0,2);
		fileName = fileName.substring(2);
		originalFileName = originalFileName.substring(2);

		//take out last character
		m_production = originalFileName.substring(originalFileName.length()-1,originalFileName.length());
		fileName = fileName.substring(0, fileName.length()-1);
		originalFileName = originalFileName.substring(0, originalFileName.length()-1);

		if (fileName.length() < 1) {
			return false;
		}

		m_digits = new String[fileName.length()];
		for (int i = 0; i < m_digits.length; i++) {
			m_digits[i] = fileName.substring(i,i+1);
			if (!m_tableOfLabels.isLabelInTable(m_digits[i])) {
				return false;
			}
		}
		m_corpus = "tidigits";
		return true;
	}
}
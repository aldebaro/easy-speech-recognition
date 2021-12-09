package run;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;

//import JMatLink;
import edu.ucsd.asr.*;

/**
 * Based on a DTL (either the one for errors or for matches)
 * with lattices sorted according to the associated HMM models,
 * allow inspection of spectrogram, etc.
 *
 * 3 threads:
 *  a) main commands the GUI update
 *  b) ListenTIMIT is Runnable to run the calculations
 *  c) MyThread counts seconds in order to run the automatic
 *     (without user intervention) update.
 *
 *@author     Aldebaro
 *@created    February 28, 2001 - upgraded in 2009
 */
public class VisualizeTIMITClassification extends JFrame implements ActionListener, Runnable {

	private SetOfPlainContinuousHMMs m_hmmSet;
	private String m_directoryWithSentenceSOPFiles;
	DatabaseManager m_databaseManager;
	TableOfLabels m_tableOfLabels;
	PatternGenerator m_patternGenerator;
	private HMMViewer[] m_hMMViewer;
	private DataLocator m_currentDataLocator;
	private boolean m_owasEndOfFileReached = false;
	private boolean m_oshowWaveformAndSpectrogram = true;
	private boolean m_oshowScores = true;
	private boolean m_oisAudioMute = false;

	private Pattern m_pattern;
	private String m_currentSetOfPatternsFileName;

	private int m_npositionOfBestHMMInNBestList;
	private int m_npositionOfSecondBestHMMInNBestList;

	private final static int m_nnumberOfNeighborSegmentsToInclude = 2;
	private final static String m_initalSleepTimeTextField = "3";
	private final static int m_nnumberNInNBestList = 10;
	private final static boolean m_oshouldShowStatesInScoreEvolution = true;

	private int m_ncurrentNumberNInNBestList;

	private int m_nspectrogramBWInHz = 200;
	private int m_nwindowShift = 1;

	//JMatLink engine;
	private Audio m_audioOfCurrentSentence;
	private Audio m_audioOfCurrentSegmentsOfInterest;

	private Thread m_runThread;
	private boolean m_oisRunning;
	private boolean m_oshouldShowNewFile;
	private boolean m_okeepIncrementingFileIndex;
	private MyThread m_myThreadForRunningCountinuously;
	//static JMatLink m_engine;
	//private boolean m_oisBusyCalculating;
	private static Vector m_dataLocators = new Vector();
	private static int m_ncurrentFileIndex;
	//private static int m_ntotalNumberOfFiles;

	// the menubar
	JMenuBar menubar;

	// menu panes
	JMenu file, help;
	JButton m_nextFileButton;
	JButton m_previousFileButton = new JButton();
	JPanel m_commandPanel = new JPanel();
	FlowLayout flowLayout1 = new FlowLayout();
	JPanel m_optionsPanel = new JPanel();
	JPanel m_promptPanel = new JPanel();
	JButton m_gotoFileButton = new JButton();
	JTextField m_gotoFileTextField = new JTextField();
	JCheckBox m_keepDoingCheckBox = new JCheckBox();
	JTextField m_sleepTimeTextField = new JTextField();
	JLabel m_sleepingCounterLabel = new JLabel();
	JPanel m_showPanel = new JPanel();
	JPanel m_hmmOptionsPanel = new JPanel();
	JPanel m_spectrogramOptions = new JPanel();
	JCheckBox m_showScoresCheckBox = new JCheckBox();
	JCheckBox m_showHMMsCheckBox = new JCheckBox();
	JCheckBox m_showWavSpecCheckBox = new JCheckBox();
	JLabel m_showLabel = new JLabel();
	GridLayout gridLayout2 = new GridLayout();
	JTextField m_nbestCandidatePositionTextField = new JTextField();
	JLabel jLabel3 = new JLabel();
	JLabel jLabel4 = new JLabel();
	JLabel jLabel5 = new JLabel();
	JTextField m_windowShiftTextField = new JTextField();
	JTextField m_specBWTextField = new JTextField();
	JLabel m_bestCandidateLabelLabel = new JLabel();
	JLabel jLabel8 = new JLabel();
	JTextField m_sentenceText = new JTextField(60);
	JTextField m_segmentTextField = new JTextField(30);
	JTextField m_fileNameTextField = new JTextField(30);
	JPanel jPanel1 = new JPanel();
	JPanel jPanel2 = new JPanel();
	JPanel jPanel3 = new JPanel();
	GridLayout gridLayout1 = new GridLayout();
	JPanel jPanel4 = new JPanel();
	JPanel jPanel5 = new JPanel();
	JPanel jPanel6 = new JPanel();
	GridLayout gridLayout3 = new GridLayout();
	JPanel jPanel7 = new JPanel();
	JLabel jLabel7 = new JLabel();
	JLabel m_correctHMMLabelLabel = new JLabel();
	JTextField m_correctHMMPositionTextField = new JTextField();
	JLabel jLabel2 = new JLabel();
	JCheckBox jCheckBox4 = new JCheckBox();
	BorderLayout borderLayout2 = new BorderLayout();
	JPanel m_playPanel = new JPanel();
	JButton m_PlayButton = new JButton();
	JTextField m_messageTextField;
	BorderLayout borderLayout1 = new BorderLayout();
	JPanel m_messagePanel = new JPanel();
	JButton m_playSentence = new JButton();
	JButton m_exitButton = new JButton();
	GridBagLayout gridBagLayout1 = new GridBagLayout();
	GridBagLayout gridBagLayout2 = new GridBagLayout();
	JButton m_playOnlyCurrentButton = new JButton();
	JCheckBox m_muteAudioCheckBox = new JCheckBox();

	/**
	 *  Constructor for the VisualizeTIMITClassification object
	 */
	public VisualizeTIMITClassification(String hmmFileName,
			DatabaseManager databaseManager,
			TableOfLabels tableOfLabels,
			PatternGenerator patternGenerator,
			String directoryWithSentenceSOPFiles) {
		m_directoryWithSentenceSOPFiles = directoryWithSentenceSOPFiles;
		m_patternGenerator = patternGenerator;
		m_tableOfLabels = tableOfLabels;
		m_databaseManager = databaseManager;
		m_hmmSet = new SetOfPlainContinuousHMMs(hmmFileName);
		enableEvents(AWTEvent.WINDOW_EVENT_MASK);
		try {
			jbInit();
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public void actionPerformed(ActionEvent evt) {
		Object src = evt.getSource();
		if (src == m_exitButton) {
			exitAll();
		}
		if (src == m_playSentence) {
			playWholeSentence();
		}
		if (src == m_sleepTimeTextField) {
			int ntime = readSleepingTimeField();
			if (m_myThreadForRunningCountinuously != null) {
				m_myThreadForRunningCountinuously.setSleepingTime(ntime);
			}
		}
		if (src == m_nextFileButton) {
			m_nextFileButton.setEnabled(false);
			m_ncurrentFileIndex++;
			getDataLocator();
			showTextualInformation();
			m_oshouldShowNewFile = true;
			m_nextFileButton.setEnabled(true);
		}
		if (src == m_previousFileButton) {
			m_previousFileButton.setEnabled(false);
			//m_previousFileButton.setVisible(false);
			m_ncurrentFileIndex--;
			if (m_ncurrentFileIndex < 0) {
				m_messageTextField.setText("Already pointing to first file");
				//reset to avoid user decrease counter indefinetely
				m_ncurrentFileIndex = 0;
			}
			else {
				getDataLocator();
				showTextualInformation();
				m_oshouldShowNewFile = true;
			}
			m_previousFileButton.setEnabled(true);
			//m_previousFileButton.setVisible(true);
		}
		if (src == m_gotoFileButton) {
			String text = m_gotoFileTextField.getText();
			try {
				m_ncurrentFileIndex = Integer.parseInt(text);
				//Java counts 0, 1... and want first file be seen as 1 by user
				//so decrement:
				m_ncurrentFileIndex--;
				getDataLocator();
				//if (m_ncurrentFileIndex < 0 /*|| m_ncurrentFileIndex > m_ntotalNumberOfFiles - 1*/) {
				//	m_messageTextField.setText("Invalid entry");
				//reset to avoid user decrease counter indefinetely
				//m_ncurrentFileIndex = 0;
				//}
				//else {
				showTextualInformation();
				m_oshouldShowNewFile = true;
				//}
				//System.out.println(text);
			}
			catch (NumberFormatException e) {
				m_messageTextField.setText("Invalid entry");
				m_gotoFileTextField.setText("1");
			}
		}
		repaint();
	}

	/**
	 *  Description of the Method
	 */
	public synchronized void stopSimulation() {
		m_oisRunning = false;
		//waits until thread stops (pp. 34 book by Scott Oaks)
		if (m_runThread != null) {
			//System.out.println("m_runThread != null, try to join it...");
			try {
				m_runThread.join();
				//waits for this thread to die
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
			m_runThread = null;
			//System.out.println("m_runThread = null now");
		}
	}

	private void getDataLocator() {
//		if (m_ncurrentFileIndex > m_ntotalNumberOfFiles - 1) {
//		m_messageTextField.setText("Already pointing to end of files");
//		//reset to avoid user increase counter indefinetely
//		m_ncurrentFileIndex = m_ntotalNumberOfFiles - 1;
//		}
//		else {
		if (m_ncurrentFileIndex < 0) {
			m_messageTextField.setText("Already pointing to first file");
			//reset to avoid user decrease counter indefinetely
			m_ncurrentFileIndex = 0;
			if (m_dataLocators.elementAt(0) != null) {
				m_currentDataLocator = (DataLocator) m_dataLocators.elementAt(m_ncurrentFileIndex);
			} else {
				if (m_databaseManager.isThereDataToRead()) {
					DataLocator dataLocator = m_databaseManager.getNextDataLocator();
					m_dataLocators.addElement(dataLocator);
				} else {
					End.throwError("Should never happen... m_ncurrentFileIndex = " + m_ncurrentFileIndex +
					" and there is no data to read from DatabaseManager");
				}
			}
		} else if (m_ncurrentFileIndex < m_dataLocators.size()) {
			m_currentDataLocator = (DataLocator) m_dataLocators.elementAt(m_ncurrentFileIndex);
		} else {
			if (!m_owasEndOfFileReached && m_databaseManager.isThereDataToRead()) {
				DataLocator dataLocator = m_databaseManager.getNextDataLocator();
				m_dataLocators.addElement(dataLocator);
				//recursively read from m_databaseManager
				getDataLocator();
			} else {
				m_owasEndOfFileReached = true;
				m_messageTextField.setText("Already pointing to end of files");
				m_ncurrentFileIndex = m_dataLocators.size() - 1;
				m_currentDataLocator = (DataLocator) m_dataLocators.elementAt(m_ncurrentFileIndex);
			}
		}
	}

	/**
	 *  Main processing method for the VisualizeTIMITClassification object
	 */
	public void run() {
		while (m_oisRunning) {
			if (m_oshouldShowNewFile) {
				m_oshouldShowNewFile = false;

				//doWhatIsNedeed();

				long lstart = System.currentTimeMillis();
				if (m_ncurrentFileIndex < 0 /*|| m_ncurrentFileIndex > m_ntotalNumberOfFiles - 1*/) {
					m_ncurrentFileIndex = 0;
				}
				//m_currentDataLocator = getNextDataLocator();
				//String wavFileNameFullPath = (String) m_dataLocators.elementAt(m_ncurrentFileIndex);

				m_messageTextField.setText("Matlab is calculating...");

				processInformationOfDataLocator();

				//play
				//m_audioOfCurrentSentence = new Audio(AudioFileReader.getAudioInputStream(wavFileNameFullPath));
				//playAudio();

				//String dir = FileNamesAndDirectories.getPathFromFileName(wavFileNameFullPath);
				//String wavFileName = FileNamesAndDirectories.getFileNameFromPath(wavFileNameFullPath);
				//String phnFileName = FileNamesAndDirectories.substituteExtension(wavFileName, "phn");
				//call Matlab
				//engine.engEvalString("loadfile('replace','" + wavFileName + "','" + phnFileName + "');");
				//Print.dialog(prompt);
				//Print.dialog("loadfile('replace','" + wavFileName + "','"+phnFileName+"');");
				long ltime = (System.currentTimeMillis() - lstart) / 1000;
				m_messageTextField.setText("Current sentence # = " + (m_ncurrentFileIndex + 1) +
						". Processing time = " + ltime + " seconds.");
			}
			try {
				Thread.sleep(100);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 *  Gets the Integer attribute of the VisualizeTIMITClassification object
	 *
	 *@param  jTextField  Description of Parameter
	 *@return             The Integer value
	 */
	private int getInteger(JTextField jTextField) {
		int nvalue = -1;
		String text = jTextField.getText();
		try {
			nvalue = Integer.parseInt(text);
		}
		catch (NumberFormatException e) {
			m_messageTextField.setText("Invalid entry");
		}
		return nvalue;
	}


	private void jbInit() throws Exception {

		m_messageTextField = new JTextField("Press button 'Next file'");
		this.setTitle("VisualizeTIMITClassification");
		JPanel contentPane = (JPanel) this.getContentPane();
		contentPane.setLayout(gridBagLayout1);

		help = new JMenu("Help");
		m_sleepTimeTextField.setMinimumSize(new Dimension(30, 21));
		m_sleepTimeTextField.setPreferredSize(new Dimension(30, 21));
		m_gotoFileTextField.setMinimumSize(new Dimension(40, 21));
		m_promptPanel.setLayout(borderLayout2);
		m_commandPanel.setMinimumSize(new Dimension(529, 30));
		m_commandPanel.setPreferredSize(new Dimension(529, 30));
		m_promptPanel.setMinimumSize(new Dimension(4, 60));
		m_promptPanel.setPreferredSize(new Dimension(660, 60));
		m_optionsPanel.setMinimumSize(new Dimension(125, 30));
		m_optionsPanel.setPreferredSize(new Dimension(125, 30));
		m_optionsPanel.setLayout(gridBagLayout2);
		contentPane.setMinimumSize(new Dimension(670, 280));
		contentPane.setPreferredSize(new Dimension(670, 280));
		m_sleepingCounterLabel.setFont(new java.awt.Font("Monospaced", 2, 12));
		m_sleepingCounterLabel.setToolTipText("counter for automatic increment");
		m_sleepingCounterLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		m_sleepingCounterLabel.setHorizontalTextPosition(SwingConstants.RIGHT);
		m_showPanel.setBorder(BorderFactory.createEtchedBorder());
		m_showPanel.setLayout(gridLayout2);
		m_hmmOptionsPanel.setBorder(BorderFactory.createEtchedBorder());
		m_hmmOptionsPanel.setLayout(gridLayout1);
		m_spectrogramOptions.setBorder(BorderFactory.createEtchedBorder());
		m_spectrogramOptions.setLayout(gridLayout3);
		m_showScoresCheckBox.setSelected(m_oshowScores);
		m_muteAudioCheckBox.setSelected(m_oisAudioMute);
		m_showScoresCheckBox.setText("Scores evolution");
		m_showScoresCheckBox.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showScoresCheckBox_actionPerformed(e);
			}
		});
		m_showHMMsCheckBox.setText("HMMs");
		m_showWavSpecCheckBox.setText("Waveform & Spec.");
		m_showWavSpecCheckBox.setToolTipText("");
		m_showWavSpecCheckBox.setSelected(m_oshowWaveformAndSpectrogram);
		m_showWavSpecCheckBox.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showWavSpecCheckBox_actionPerformed(e);
			}
		});
		m_showLabel.setFont(new java.awt.Font("SansSerif", 1, 14));
		m_showLabel.setForeground(Color.blue);
		m_showLabel.setHorizontalAlignment(SwingConstants.CENTER);
		m_showLabel.setText("Show");
		gridLayout2.setRows(4);
		gridLayout2.setColumns(1);
		m_nbestCandidatePositionTextField.setMinimumSize(new Dimension(58, 21));
		m_nbestCandidatePositionTextField.setText("   ");
		m_nbestCandidatePositionTextField.setColumns(3);
		m_nbestCandidatePositionTextField.setHorizontalAlignment(SwingConstants.LEFT);
		jLabel3.setHorizontalAlignment(SwingConstants.LEFT);
		jLabel3.setText("Best candidate position");
		jLabel4.setFont(new java.awt.Font("SansSerif", 1, 14));
		jLabel4.setForeground(Color.blue);
		jLabel4.setHorizontalAlignment(SwingConstants.CENTER);
		jLabel4.setText("Spectrogram");
		jLabel5.setHorizontalAlignment(SwingConstants.CENTER);
		jLabel5.setText("Bandwidth (Hz)     ");
		m_windowShiftTextField.setText(Integer.toString(m_nwindowShift));
		m_windowShiftTextField.setColumns(4);
		m_windowShiftTextField.setHorizontalAlignment(SwingConstants.RIGHT);
		m_windowShiftTextField.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(ActionEvent e) {
				windowShiftTextField_actionPerformed(e);
			}
		});
		m_specBWTextField.setText(Integer.toString(m_nspectrogramBWInHz));
		m_specBWTextField.setColumns(4);
		m_specBWTextField.setHorizontalAlignment(SwingConstants.RIGHT);
		m_specBWTextField.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(ActionEvent e) {
				specBWTextField_actionPerformed(e);
			}
		});
		m_bestCandidateLabelLabel.setToolTipText("");
		m_bestCandidateLabelLabel.setHorizontalAlignment(SwingConstants.LEFT);
		m_bestCandidateLabelLabel.setText("     ");
		jLabel8.setHorizontalAlignment(SwingConstants.CENTER);
		jLabel8.setText("Window shift (ms)");
		m_sentenceText.setBorder(BorderFactory.createEtchedBorder());
		m_sentenceText.setToolTipText("sentence text");
		m_sentenceText.setEditable(false);
		m_sentenceText.setFont(new java.awt.Font("SansSerif", 0, 14));
		m_segmentTextField.setFont(new java.awt.Font("SansSerif", 0, 14));
		m_segmentTextField.setBorder(BorderFactory.createEtchedBorder());
		m_segmentTextField.setPreferredSize(new Dimension(330, 24));
		m_segmentTextField.setEditable(false);
		m_segmentTextField.setColumns(30);
		m_fileNameTextField.setFont(new java.awt.Font("SansSerif", 0, 14));
		m_fileNameTextField.setBorder(BorderFactory.createEtchedBorder());
		m_fileNameTextField.setPreferredSize(new Dimension(330, 24));
		m_fileNameTextField.setEditable(false);
		m_fileNameTextField.setColumns(30);
		gridLayout1.setRows(4);
		gridLayout1.setColumns(1);
		gridLayout3.setRows(4);
		gridLayout3.setColumns(1);
		jLabel7.setFont(new java.awt.Font("SansSerif", 1, 14));
		jLabel7.setForeground(Color.blue);
		jLabel7.setHorizontalAlignment(SwingConstants.CENTER);
		jLabel7.setText("Options");
		m_correctHMMLabelLabel.setHorizontalAlignment(SwingConstants.LEFT);
		m_correctHMMLabelLabel.setText("       ");
		m_correctHMMPositionTextField.setMinimumSize(new Dimension(58, 21));
		m_correctHMMPositionTextField.setText("   ");
		m_correctHMMPositionTextField.setColumns(3);
		m_correctHMMPositionTextField.setHorizontalAlignment(SwingConstants.LEFT);
		jLabel2.setHorizontalAlignment(SwingConstants.LEFT);
		jLabel2.setText("Correct HMM position     ");
		jCheckBox4.setText("Pick second HMM according to score");
		jPanel7.setForeground(Color.blue);
		m_playPanel.setBorder(BorderFactory.createEtchedBorder());
		m_PlayButton.setToolTipText("Play segments range of interest");
		m_PlayButton.setActionCommand("Play");
		m_PlayButton.setText("Play neighbors");
		m_PlayButton.addActionListener(new java.awt.event.ActionListener() {
			/**
			 *  Description of the Method
			 *
			 *@param  e  Description of Parameter
			 */
			public void actionPerformed(ActionEvent e) {
				playAudio();
			}
		});
		m_messageTextField.setBackground(Color.pink);
		m_messageTextField.setPreferredSize(new Dimension(620, 21));
		m_messageTextField.setEditable(false);
		m_messageTextField.setText("Suggestion: click \'Next\' button");
		m_messageTextField.setColumns(60);
		m_messagePanel.setMinimumSize(new Dimension(14, 30));
		m_messagePanel.setPreferredSize(new Dimension(670, 30));
		m_messagePanel.setLayout(borderLayout1);
		m_playSentence.setText("Play sentence");
		m_exitButton.setText("Exit");
		m_playOnlyCurrentButton.setToolTipText("Play only current segment");
		m_playOnlyCurrentButton.setText("Play segment");
		m_playOnlyCurrentButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(ActionEvent e) {
				playOnlyCurrentButton_actionPerformed(e);
			}
		});
		m_muteAudioCheckBox.setText("Mute");
		m_muteAudioCheckBox.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(ActionEvent e) {
				muteAudioCheckBox_actionPerformed(e);
			}
		});
		help.add(new JMenuItem("About"));

		m_nextFileButton = new JButton("Next File");

		// Create the menubar.  Tell the frame about it.
		menubar = new JMenuBar();
		this.setJMenuBar(menubar);
		// Create the file menu.  Add two items to it.  Add to menubar.
		file = new JMenu("File");
		m_previousFileButton.setActionCommand("Previous File");
		m_previousFileButton.setText("Previous");

		m_commandPanel.setLayout(flowLayout1);
		m_gotoFileButton.setText("Go to");
		m_gotoFileTextField.setBackground(Color.orange);
		m_gotoFileTextField.setPreferredSize(new Dimension(40, 21));
		m_gotoFileTextField.setToolTipText("Type number of file");
		m_gotoFileTextField.setText("file #");
		m_keepDoingCheckBox.setText("Automatic increment");
		m_keepDoingCheckBox.addActionListener(
				new java.awt.event.ActionListener() {
					/**
					 *  Description of the Method
					 *
					 *@param  e  Description of Parameter
					 */
					public void actionPerformed(ActionEvent e) {
						keepDoing(e);
					}
				});
		m_nextFileButton.setText("Next");
		m_sleepTimeTextField.setText(m_initalSleepTimeTextField);
		m_sleepingCounterLabel.setText("       (seconds)");
		file.add(new JMenuItem("Open"));
		file.add(new JMenuItem("Quit"));
		menubar.add(file);
		// Create Help menu; add an item; add to menubar
		menubar.add(help);

		m_nextFileButton.addActionListener(this);
		m_previousFileButton.addActionListener(this);
		m_gotoFileButton.addActionListener(this);
		m_sleepTimeTextField.addActionListener(this);
		m_exitButton.addActionListener(this);
		m_playSentence.addActionListener(this);

		contentPane.add(m_commandPanel, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0
				,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 5, 0), 103, 15));
		m_commandPanel.add(m_nextFileButton, null);
		m_commandPanel.add(m_previousFileButton, null);
		m_commandPanel.add(m_gotoFileButton, null);
		m_commandPanel.add(m_gotoFileTextField, null);
		m_commandPanel.add(m_keepDoingCheckBox, null);
		m_commandPanel.add(m_sleepTimeTextField, null);
		m_commandPanel.add(m_sleepingCounterLabel, null);
		contentPane.add(m_messagePanel, new GridBagConstraints(0, 4, 1, 1, 1.0, 1.0
				,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 0, 1, 0), -38, 5));
		m_messagePanel.add(m_messageTextField, BorderLayout.CENTER);
		m_messagePanel.add(m_exitButton, BorderLayout.EAST);
		contentPane.add(m_optionsPanel, new GridBagConstraints(0, 2, 1, 1, 1.0, 1.0
				,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 507, 104));
		m_optionsPanel.add(m_showPanel, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0
				,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 1, 0, 0), 14, 26));
		m_showPanel.add(m_showLabel, null);
		m_showPanel.add(m_showWavSpecCheckBox, null);
		m_showPanel.add(m_showScoresCheckBox, null);
		m_showPanel.add(m_showHMMsCheckBox, null);
		m_optionsPanel.add(m_hmmOptionsPanel, new GridBagConstraints(1, 0, 1, 1, 1.0, 1.0
				,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 30, -14));
		m_hmmOptionsPanel.add(jPanel7, null);
		jPanel7.add(jLabel7, null);
		m_hmmOptionsPanel.add(jPanel1, null);
		jPanel1.add(jLabel2, null);
		jPanel1.add(m_correctHMMPositionTextField, null);
		jPanel1.add(m_correctHMMLabelLabel, null);
		m_hmmOptionsPanel.add(jPanel3, null);
		jPanel3.add(jLabel3, null);
		jPanel3.add(m_nbestCandidatePositionTextField, null);
		jPanel3.add(m_bestCandidateLabelLabel, null);
		m_hmmOptionsPanel.add(jPanel2, null);
		jPanel2.add(jCheckBox4, null);
		m_optionsPanel.add(m_spectrogramOptions, new GridBagConstraints(2, 0, 1, 1, 1.0, 1.0
				,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 2), 32, 2));
		m_spectrogramOptions.add(jPanel4, null);
		jPanel4.add(jLabel4, null);
		m_spectrogramOptions.add(jPanel5, null);
		jPanel5.add(jLabel5, null);
		jPanel5.add(m_specBWTextField, null);
		m_spectrogramOptions.add(jPanel6, null);
		jPanel6.add(jLabel8, null);
		jPanel6.add(m_windowShiftTextField, null);
		contentPane.add(m_playPanel, new GridBagConstraints(0, 3, 1, 1, 1.0, 1.0
				,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 5, 0), 445, 11));
		m_playPanel.add(m_muteAudioCheckBox, null);
		m_playPanel.add(m_playSentence, null);
		m_playPanel.add(m_PlayButton, null);
		m_playPanel.add(m_playOnlyCurrentButton, null);
		contentPane.add(m_promptPanel, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0
				,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(3, 0, 0, 0), -28, 0));
		m_promptPanel.add(m_fileNameTextField, BorderLayout.WEST);
		m_promptPanel.add(m_segmentTextField, BorderLayout.CENTER);
		m_promptPanel.add(m_sentenceText, BorderLayout.SOUTH);

		addWindowListener(
				new WindowAdapter() {
					public void
					windowClosing(WindowEvent e) {
						exitAll();
						//System.exit(0);
					}
				});

		// We should call f.pack() here.  But its buggy.
		//this.setSize(300, 250);
		this.pack();

		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension frameSize = this.getSize();
		if (frameSize.height > screenSize.height) {
			frameSize.height = screenSize.height;
		}
		if (frameSize.width > screenSize.width) {
			frameSize.width = screenSize.width;
		}
		this.setLocation((screenSize.width - frameSize.width) / 2, (int) ((screenSize.height - frameSize.height) / 1.3));
		this.setVisible(true);
		//this.show();

		initialize();
	}

	private void exitAll() {
		MatlabInterfacer.close();
		m_databaseManager.finalizeDataReading();
		System.exit(0);
	}

	private void initialize() {
		//m_engine.engOpen();
//		String wavFileName = (String) m_dataLocators.elementAt(m_ncurrentFileIndex);
//		String dir = FileNamesAndDirectories.getPathFromFileName(wavFileName);
//		engine.engEvalString("cd " + dir);
//		wavFileName = FileNamesAndDirectories.getFileNameFromPath(wavFileName);
//		//engine.engEvalString("cd C:\\Aksource\\Matlab\\AkColea");
//		String phnFileName = FileNamesAndDirectories.substituteExtension(wavFileName, "phn");
//		engine.engEvalString("colea('" + wavFileName + "',16000,'" + phnFileName + "')");
//		m_ncurrentFileIndex = -1;
		createThread();

		//String sopFile = "C:/simulations/timit/39models/plp18w512s160/features/train/14_g.sop";
		//String sopFile = "C:/simulations/timit/39models/plp18w512s160/features/train/0_aa.sop";
		//String sopDir = "C:/simulations/timit/39models/plp18w512s160/features/test/";
		//String dtlDir = "C:/simulations/timit/39models/transcriptions/monophones/isolated/test/";
		//String hmmFile = "C:/simulations/timit/39models/plp18w512s160/lrforwardskips5/monophones/isolated/kmeansviterbi/10/hmms.jar";
		//String hmmFile = "C:/simulations/timit/39models/plp18w512s160/lrforwardskips5/monophones/isolated/prototypes/hmms.jar";

		//String outputFileName = "c:/temp/bestao.txt";
		//HMMViewer.createViewerInformationForHMMSet(sopDir,
		//	hmmSet,outputFileName);
		//System.exit(1);

		int nnumberOfHMMs = m_hmmSet.getNumberOfHMMs();
		m_hMMViewer = new HMMViewer[nnumberOfHMMs];
		TableOfLabels table = m_hmmSet.getTableOfLabels();
		//ContinuousHMM[] continuousHMMs = m_hmmSet.getHMMs();
//		Print.dialog("");
//		for (int i = 0; i < nnumberOfHMMs; i++) {
//		IO.showCounter(i);
//		String sopFile = sopDir + i + "_" + table.getFirstLabel(i) + ".sop";
//		ContinuousHMM hmm = continuousHMMs[i];
//		m_hMMViewer[i] = new HMMViewer(hmm, new SetOfPatterns(sopFile));
//		//hMMViewer[i] = new HMMViewer(outputFileName,i);
//		m_hMMViewer[i].getHMMUniqueRepresentation(continuousHMMs[i]);
//		Print.dialog("\n" + m_hMMViewer[i].toString());
//		//fhMMRepresentations[i] =  hMMViewer.getHMMRepresentation();
//		}
//		Print.dialog("");

		//System.exit(1);
		//hMMViewer.createFiles();
		//now test it
		//TableOfLabels table = m_hmmSet.getTableOfLabels();
		//int nhMMIndex = 26;
		//String sopFile = sopDir + nhMMIndex + "_" + table.getFirstLabel(nhMMIndex) + ".sop";
		//String dtlFile = dtlDir + nhMMIndex + "_" + table.getFirstLabel(nhMMIndex) + ".DTL";
		//SetOfPatterns setOfPatterns = new SetOfPatterns(sopFile);
		//DatabaseManager databaseManager = new DatabaseManager(dtlFile);

		//m_engine = new JMatLink();
		//m_engine.engOpen();
		MatlabInterfacer.sendCommand("cd c:\\temp");

		//necessary?
		boolean okeepPathsOfNBestList = true;
		m_hmmSet.enableNBestListGeneration(m_nnumberNInNBestList, okeepPathsOfNBestList);

		//m_currentDataLocator = getNextDataLocator();
		m_ncurrentFileIndex = -1;
		//getDataLocator();
		//m_oshouldShowNewFile = true;
		//int i = 0;
	}

	private void createThread() {
		//now, create Thread to run training
		if (m_runThread == null) {
			m_runThread = new Thread(this, "ListenTIMIT");
			//use max priority as below, but other softwares get slower
			//m_runThread.setPriority(Thread.MAX_PRIORITY);
			//so, I am assuming a default of normal priority
			//			String property = m_headerProperties.getProperty("TrainingManager.ThreadPriority");
			//			if (property != null && property.equals("MAXIMUM")) {
			//				m_runThread.setPriority(Thread.MAX_PRIORITY);
			//			}
			//			else {
			//				m_runThread.setPriority(Thread.NORM_PRIORITY);
			//			}
			m_oisRunning = true;
			m_runThread.start();
			//start() calls method run()
		}
		else {
			//Print.warning("Can't init. thread already running.");
			m_runThread.start();
		}
	}


	private void showTextualInformation() {
		//String wavFileNameFullPath = (String) m_dataLocators.elementAt(m_ncurrentFileIndex);
		String wavFileNameFullPath = m_currentDataLocator.getFileName();
		String wavFileName = FileNamesAndDirectories.getFileNameFromPath(wavFileNameFullPath);
		String promptFileName = FileNamesAndDirectories.substituteExtension(wavFileNameFullPath, "txt");
		String prompt = readPrompt(promptFileName);
		//TIMITPathOrganizer tIMITPathOrganizer = new TIMITPathOrganizer(wavFileNameFullPath);
		//m_fileNameTextField.setText((m_ncurrentFileIndex + 1) + ". " + tIMITPathOrganizer.toString())	
		m_fileNameTextField.setText((m_ncurrentFileIndex + 1) + ". " + wavFileName);

		m_sentenceText.setText((m_ncurrentFileIndex + 1) + ". " + prompt);
		showHMMInformation();
	}

	private void playAudio() {
		if (m_oisAudioMute) {
			return;
		}
		if (m_audioOfCurrentSegmentsOfInterest != null) {
			AudioPlayer.playScaled(m_audioOfCurrentSegmentsOfInterest);
		}
		else {
			m_messageTextField.setText("There is no file to play from");
		}
	}

	private void playAudioOfCurrentSegmentOnly() {
		if (m_oisAudioMute) {
			return;
		}
		if (m_currentDataLocator != null) {
			LabeledSpeech labeledSpeech = new LabeledSpeech(m_currentDataLocator);
			AudioPlayer.playScaled(labeledSpeech.getAudioFromGivenSegment(0));
		}
		else {
			m_messageTextField.setText("There is no file to play from");
		}
	}

	private void playWholeSentence() {
		if (m_oisAudioMute) {
			return;
		}
		if (m_audioOfCurrentSentence != null) {
			AudioPlayer.playScaled(m_audioOfCurrentSentence);
		}
		else {
			m_messageTextField.setText("There is no file to play from");
		}
	}

	private String readPrompt(String promptFileName) {
		Vector temp = IO.readVectorOfStringsFromFile(promptFileName);
		String line = (String) temp.elementAt(0);
		//take out endpoints
		StringTokenizer stringTokenizer = new StringTokenizer(line);
		stringTokenizer.nextToken();
		stringTokenizer.nextToken();
		StringBuffer stringBuffer = new StringBuffer(stringTokenizer.nextToken());
		while (stringTokenizer.hasMoreTokens()) {
			stringBuffer.append(" " + stringTokenizer.nextToken());
		}
		return stringBuffer.toString();
	}


	//runs in the main Thread
	private void showsCountinuously() {
		m_ncurrentFileIndex++;
		if (m_ncurrentFileIndex < 0 /*|| m_ncurrentFileIndex > m_ntotalNumberOfFiles - 1*/) {
			m_ncurrentFileIndex = 0;
		}
		getDataLocator();
		showTextualInformation();
		//Print.dialog(""+ m_ncurrentFileIndex);
		//repaint();
		m_oshouldShowNewFile = true;
		//Print.dialog(""+ m_ncurrentFileIndex);
	}


	private void updateSleepingTimeCounter(int nvalue) {
		String text = null;
		if (nvalue > 9) {
			text = Integer.toString(nvalue) + " seconds";
		}
		else {
			text = " " + Integer.toString(nvalue) + " seconds";
		}
		m_sleepingCounterLabel.setText(text);
	}

	private int readSleepingTimeField() {
		int nsleepTimeInSeconds = getInteger(m_sleepTimeTextField);
		if (nsleepTimeInSeconds < 1) {
			nsleepTimeInSeconds = 1;
		}
		else if (nsleepTimeInSeconds > 60) {
			nsleepTimeInSeconds = 60;
		}
		m_sleepTimeTextField.setText(Integer.toString(nsleepTimeInSeconds));
		return nsleepTimeInSeconds;
	}


	private void keepDoing(ActionEvent e) {
		m_okeepIncrementingFileIndex = m_keepDoingCheckBox.isSelected();
		if (m_okeepIncrementingFileIndex) {
			if (m_myThreadForRunningCountinuously == null) {
				m_myThreadForRunningCountinuously = new MyThread(readSleepingTimeField(), this);
			}
		}
		else {
			if (m_myThreadForRunningCountinuously != null) {
				m_myThreadForRunningCountinuously.stopIt();
				try {
					m_myThreadForRunningCountinuously.join();
					//waits for this thread to die
				}
				catch (InterruptedException e2) {
					e2.printStackTrace();
				}
				m_myThreadForRunningCountinuously = null;
			}
		}
		//showsCountinuously();
	}

	/**
	 *  Description of the Method
	 *
	 *@param  command  Description of Parameter
	 */
	private static void sendCommandToMatlab(String command) {
		MatlabInterfacer.sendCommandAndPrint(command);
	}


//	//XXX I am gonna use only segment # 0
//	private static void plotTIMITLabels(DataLocator dataLocator, JMatLink m_engine,
//	int ntotalSamples, int nnumberOfSamplesToInclude) {
//	//assume DTL has sentence file whose labels are in same directory
//	String fileName = dataLocator.getFileName();


//	Print.dialog(dataLocator.toString());

//	fileName = FileNamesAndDirectories.substituteExtension(fileName, "phn");
//	Vector labels = IO.readVectorOfStringsFromFile(fileName);
//	//get endpoints, well in the case I am working there is just one segment...
//	int[][] nendPoints = dataLocator.getAllEndpoints();
//	int nbegin = nendPoints[0][0] - nnumberOfSamplesToInclude;
//	int nend = nendPoints[0][1] + nnumberOfSamplesToInclude;
//	int nfirstIndex = -1;
//	int nlastIndex = -1;
//	int nnumberOfLabels = labels.size();

//	int[][] nsentenceEndpoints = new int[nnumberOfLabels][2];
//	String[] sentenceLabels = new String[nnumberOfLabels];
//	for (int i = 0; i < nnumberOfLabels; i++) {
//	//read all line: label + endpoints
//	sentenceLabels[i] = (String) labels.elementAt(i);

//	//Print.dialog(sentenceLabels[i]);
//	StringTokenizer stringTokenizer = new StringTokenizer(sentenceLabels[i]);
//	nsentenceEndpoints[i][0] = Integer.parseInt(stringTokenizer.nextToken());
//	nsentenceEndpoints[i][1] = Integer.parseInt(stringTokenizer.nextToken());
//	//keep only label
//	sentenceLabels[i] = stringTokenizer.nextToken();
//	if (nfirstIndex == -1) {
//	//still searching begin
//	if (nsentenceEndpoints[i][0] >= nbegin) {
//	nfirstIndex = i;
//	}
//	}
//	if (nsentenceEndpoints[i][1] <= nend) {
//	nlastIndex = i;
//	}
//	}
//	int nnumberOfSegmentsOfInterest = nlastIndex - nfirstIndex + 1;
//	int[][] nfinalEndpoints = new int[nnumberOfSegmentsOfInterest][2];
//	String[] finalLabels = new String[nnumberOfSegmentsOfInterest];
//	int nbaseSampleIndex = nsentenceEndpoints[nfirstIndex][0];
//	for (int i = 0; i < nnumberOfSegmentsOfInterest; i++) {
//	nfinalEndpoints[i][0] = nsentenceEndpoints[i + nfirstIndex][0] - nbaseSampleIndex;
//	nfinalEndpoints[i][1] = nsentenceEndpoints[i + nfirstIndex][1] - nbaseSampleIndex;
//	finalLabels[i] = sentenceLabels[i + nfirstIndex];
//	}


//	Print.dialog(finalLabels[0]);
//	IO.DisplayMatrix(nfinalEndpoints);

//	plotLabelsInMatlab(nfinalEndpoints, finalLabels, m_engine, ntotalSamples);
//	}


	/**
	 * Assume DTL has sentence file whose labels are in same directory, read
	 * PHN file and return it as a DataLocator.
	 */
	private static DataLocator getAllTIMITLabels(DataLocator dataLocator) {

		String fileName = dataLocator.getFileName();

		String phnFileName = FileNamesAndDirectories.substituteExtension(fileName, "phn");
		Vector labels = IO.readVectorOfStringsFromFile(phnFileName);
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

			sentenceLabels[i] = stringTokenizer.nextToken();
		}
		return new DataLocator(fileName,sentenceLabels,nsentenceEndpoints);
	}

	private static int getSegmentIndex(DataLocator dataLocator, int[] nendpoints, String label) {
		int[][] nallEndpoints = dataLocator.getAllEndpoints();
		for (int i = 0; i < nallEndpoints.length; i++) {
			if (nallEndpoints[i][0] == nendpoints[0] &&
					nallEndpoints[i][1] == nendpoints[1]) {
				//check label
				if (label.equals(dataLocator.getLabelFromGivenSegment(i))) {
					return i;
				}
			}
		}
		//if reached this point, it means did not find it...
		End.throwError("Did not find " + nendpoints[0] + " " + nendpoints[1] +
				" in " + dataLocator.toString());
		return -1;
	}

	/**
	 *  Description of the Method
	 *
	 *@param  nendpoints     Description of Parameter
	 *@param  labels         Description of Parameter
	 *@param  m_engine       Description of Parameter
	 *@param  ntotalSamples  Description of Parameter
	 */
	private void plotLabelsInMatlab(DataLocator dataLocator,
			int nfirstSegment, int nlastSegment) {

		int nnumberOfSegmentsOfInterest = nlastSegment - nfirstSegment + 1;

		int[][] nendpoints = new int[nnumberOfSegmentsOfInterest][2];
		String[] labels = new String[nnumberOfSegmentsOfInterest];
		labels[0] = dataLocator.getLabelFromGivenSegment(nfirstSegment);
		nendpoints[0] = dataLocator.getEndpointsFromGivenSegment(nfirstSegment);
		for (int i = 1; i < labels.length; i++) {
			labels[i] = dataLocator.getLabelFromGivenSegment(i + nfirstSegment);
			nendpoints[i] = dataLocator.getEndpointsFromGivenSegment(i + nfirstSegment);
			nendpoints[i][0] -= nendpoints[0][0];
			nendpoints[i][1] -= nendpoints[0][0];
		}
		nendpoints[0][1] -= nendpoints[0][0];
		nendpoints[0][0] = 0;

		int ntotalSamples = nendpoints[nendpoints.length-1][1];

		double Srate = m_patternGenerator.getSpeechSamplingRate();
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
			sendCommandToMatlab("x(1) = " + (nendpoints[i][0] / Srate));
			sendCommandToMatlab("x(2) = " + (nendpoints[i][1] / Srate));
			//x(1) = 0.128;
			//x(2) = 0.1549;
			sendCommandToMatlab("xpos = round(Xoffset+x(1)*fac);");
			//% coordinate in pixels
			sendCommandToMatlab("xwi= round((x(2)-x(1))*fac);");
			sendCommandToMatlab("if xwi<=0, xwi=2; end;");

			String color = null;
			if (i % 2 == 0) {
				color = "'b'";
			}
			else {
				color = "[0.5 0.5 0.5]";
			}
			sendCommandToMatlab("lbUp(" + (i + 1) + ")=uicontrol('Style','text','Position',[xpos 5 xwi 20 ],'BackGroundColor'," + color + "," +
					"'ForeGroundColor','y','HorizontalAlignment','center','String','" + labels[i] + "');");
		}
	}


	/**
	 *  Description of the Method
	 *
	 *@param  nendPoints                 Description of Parameter
	 *@param  nneighborSamplesToInclude  Description of Parameter
	 *@param  ntotalSamples              Description of Parameter
	 *@return                            Description of the Returned Value
	 */
	private static int[] findRelativeEndpoints(int[] nendPoints, int nneighborSamplesToInclude, int ntotalSamples) {
		int nrealBegin = nendPoints[0] - nneighborSamplesToInclude;
		int nsamplesIncludedInBegin;
		if (nrealBegin < 0) {
			//included all available samples
			nsamplesIncludedInBegin = nendPoints[0];
		}
		else {
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


	/**
	 *  Description of the Method
	 *
	 *@param  nindicesList   Description of Parameter
	 *@param  ndesiredIndex  Description of Parameter
	 *@return                Description of the Returned Value
	 */
	private static int findPositionInList(int[] nindicesList, int ndesiredIndex) {
		for (int i = 0; i < nindicesList.length; i++) {
			if (nindicesList[i] == ndesiredIndex) {
				return i;
			}
		}
		return -1;
	}


	/**
	 *  Description of the Method
	 *
	 *@param  x   Description of Parameter
	 *@param  sf  Description of Parameter
	 *@return     Description of the Returned Value
	 */
	private static float[] convertToTime(int[] x, float sf) {
		sf = 1.0F / sf;
		float[] out = new float[x.length];
		for (int i = 0; i < x.length; i++) {
			out[i] = sf * x[i];
		}
		return out;
	}


	/**
	 *  Description of the Class
	 *
	 *@author     Aldebaro
	 *@created    March 4, 2001
	 */
	private class MyThread extends Thread {
		int m_nsleepTimeInSeconds;
		VisualizeTIMITClassification m_listenTIMIT;
		boolean m_okeepDoing;


		/**
		 *  Constructor for the MyThread object
		 *
		 *@param  nsleepTimeInSeconds  Description of Parameter
		 *@param  listenTIMIT          Description of Parameter
		 */
		public MyThread(int nsleepTimeInSeconds,
				VisualizeTIMITClassification listenTIMIT) {
			super("Scheduler");
			m_nsleepTimeInSeconds = nsleepTimeInSeconds;
			m_okeepDoing = true;
			m_listenTIMIT = listenTIMIT;
			start();
		}


		/**
		 *  Sets the SleepingTime attribute of the MyThread object
		 *
		 *@param  nsleepTimeInSeconds  The new SleepingTime value
		 */
		public void setSleepingTime(int nsleepTimeInSeconds) {
			m_nsleepTimeInSeconds = nsleepTimeInSeconds;
		}


		/**
		 *  Main processing method for the MyThread object
		 */
		public void run() {
			while (m_okeepDoing) {
				try {
					m_listenTIMIT.showsCountinuously();
					for (int i = 0; i < m_nsleepTimeInSeconds; i++) {
						//Print.dialog("" + m_nsleepTimeInSeconds);
						this.sleep(1000);
						m_listenTIMIT.updateSleepingTimeCounter(i + 1);
					}
				}
				catch (Exception e) {
					e.printStackTrace();
					End.exit();
				}
			}
		}

		/**
		 *  Description of the Method
		 */
		public void stopIt() {
			m_okeepDoing = false;
		}
	}

	private static String getDirectoryWithSentenceSOPFiles(String dtlFileName) {

		//Example:
		//\temp\output-hlsyntimit\56models\dct_klatt14s160\lr55\monophones\isolated\kmeansviterbi\10\test\errors56.DTL
		File file = new File(dtlFileName);

		//\temp\output-hlsyntimit\56models\dct_klatt14s160\lr55\monophones\isolated\kmeansviterbi\10\test\
		file = new File(file.getParent());

		//test
		String trainOrTest = file.getName();

		for (int i = 0; i < 6; i++) {
			file = new File(file.getParent());
		}

		//\temp\output-hlsyntimit\56models\dct_klatt14s160\
		String parametersIdentification = file.getName(); //dct_klatt14s160

		file = new File(file.getParent());

		//\temp\output-hlsyntimit\
		file = new File(file.getParent());
		String temp = FileNamesAndDirectories.concatenateTwoPaths(file.toString(),"features/"+parametersIdentification+"/"+trainOrTest);

		return FileNamesAndDirectories.forceEndingWithSlash(temp);
	}

	public static void main(String[] args) {

		if (args.length != 1) {
			Print.dialog("Usage: java VisualizeTIMITClassification <DTL file with classification results>");
			Print.dialog("The system will look for the correspondent HMM at 2 directories above");
			System.exit(1);
		}

		//directory has DTL's and classification results
		//ex:
		//F:\debugsim\timit\39models\lsf39w512s160\lrforwardskips5\monophones\isolated\kmeansviterbi\5\classification\test

		DatabaseManager databaseManager = new DatabaseManager(args[0]);

		//use convention adopted for directories to find out where SOP files are
		String directoryWithSentenceSOPFiles = getDirectoryWithSentenceSOPFiles(args[0]);

		File file = new File(args[0]);
		for (int i = 0; i < 2; i++) {
			file = new File(file.getParent());
		}
		String hmmFileName = FileNamesAndDirectories.concatenateTwoPaths(file.toString(),SetOfHMMsFile.m_name);
		file = new File(file.getParent());
		for (int i = 0; i < 5; i++) {
			file = new File(file.getParent());
			//Print.dialog(i + " " + file.toString());
		}
		String reportDirectory = file.toString();

		//Print.dialog(reportDirectory);
		DirectoryTree directoryTree = new DirectoryTree(reportDirectory,"TRN");
		//get first TRN file
		String propertiesFileName = directoryTree.nextFile();
		if (propertiesFileName == null) {
			End.throwError("No file with extension TRN found under directory " + reportDirectory);
		}

		Print.dialog("Using file " + propertiesFileName);
		HeaderProperties headerProperties = HeaderProperties.getPropertiesFromFile(propertiesFileName);

		TableOfLabels tableOfLabels = new TableOfLabels(headerProperties);

		PatternGenerator patternGenerator = PatternGenerator.getPatternGenerator(headerProperties);

		//file.getParent();

		//go 2 dir above to get HMM.
		//go 6 above to get timit_report.TRN with SOP's

		VisualizeTIMITClassification visualizeTIMITClassification = new VisualizeTIMITClassification(hmmFileName,
				databaseManager,tableOfLabels,patternGenerator,directoryWithSentenceSOPFiles);
	}

	private void showHMMInformation() {
		getLatticesAndShowInformation();
	}

	void showWavSpecCheckBox_actionPerformed(ActionEvent e) {
		m_oshowWaveformAndSpectrogram = m_showWavSpecCheckBox.isSelected();
		if (!m_oshowWaveformAndSpectrogram) {
			sendCommandToMatlab("close(1)");
		}
	}

	private void processInformationOfDataLocator() {
		//assume DataLocator has only 1 segment (an error or match)
		String label = m_currentDataLocator.getLabelFromGivenSegment(0);
		int[] ncurrentSegmentEndpoints = m_currentDataLocator.getEndpointsFromGivenSegment(0);

		//complete information, getting all labels associated with file pointed by dataLocator
		DataLocator dataLocatorAllSegments = getAllTIMITLabels(m_currentDataLocator);

		//identify where the segments of interest are located
		int ncurrentSegmentIndex = getSegmentIndex(dataLocatorAllSegments,ncurrentSegmentEndpoints,label);
		int nnumberOfSegments = dataLocatorAllSegments.getNumberOfSegments();
		int nfirstSegment = ncurrentSegmentIndex - m_nnumberOfNeighborSegmentsToInclude;
		int nlastSegment = ncurrentSegmentIndex + m_nnumberOfNeighborSegmentsToInclude;
		if (nfirstSegment < 0) {
			nfirstSegment = 0;
		}
		if (nlastSegment > nnumberOfSegments - 1) {
			nlastSegment = nnumberOfSegments - 1;
		}

		//create a DataLocator with only segments range of interest
		DataLocator dataLocatorWithSegmentsOfInterest = dataLocatorAllSegments.getDataLocatorWithGivenRangeOfSegments(nfirstSegment,nlastSegment);

		//create object with all sentence audio data and labels
		LabeledSpeech labeledSpeech = new LabeledSpeech(dataLocatorAllSegments);

		//get Audio of segments range of interest
		m_audioOfCurrentSegmentsOfInterest = labeledSpeech.getAudioFromGivenRangeOfSegments(nfirstSegment,nlastSegment);
		m_audioOfCurrentSentence =  labeledSpeech.getAudioOfWholeSentence();

		//int[] naudioSamples = labeledSpeech.getAudioDataAsIntegersFromGivenSegment(0);

		playAudio();

		if (m_oshowWaveformAndSpectrogram) {
			plotWaveformSpectrogramAndLabelsInMatlab(dataLocatorAllSegments,
					dataLocatorWithSegmentsOfInterest, nfirstSegment, nlastSegment);
		}

		if (m_oshowScores) {
			calculateAndShowScores(label);
		}
	}

	private void getLatticesAndShowInformation() {

		//Print.dialog(m_currentDataLocator.getComments());

		String correctHMMLabel = m_currentDataLocator.getLabelFromGivenSegment(0);
		int nhMMIndex = m_tableOfLabels.getEntry(correctHMMLabel);

		m_ncurrentNumberNInNBestList = m_nnumberNInNBestList;
		String pathsOfNBestListUsingRunLengthEncoding = m_currentDataLocator.getComments();
		//use setPathsOfNBestList to parse String
		//m_hmmSet.setPathsOfNBestList(m_currentDataLocator.getComments(),m_ncurrentNumberNInNBestList);
		//int[][] nstates = m_hmmSet.getPathsOfNBestList();
		int[] nindicesOfNBestList = SetOfPlainContinuousHMMs.getNBestListHMMIndices(pathsOfNBestListUsingRunLengthEncoding,m_ncurrentNumberNInNBestList);

		//int nhMMIndex = m_tableOfLabels.getEntry(m_currentDataLocator.getLabelFromGivenSegment(0));
		int npositionOfBest = findPositionInList(nindicesOfNBestList, nhMMIndex);

		if (npositionOfBest == -1) {
			int nmax = m_hmmSet.getNumberOfHMMs();
			while (npositionOfBest == -1) {
				m_ncurrentNumberNInNBestList++;
				if (m_ncurrentNumberNInNBestList > nmax) {
					End.throwError("Not found in N-best list");
				}
				//m_hmmSet.setPathsOfNBestList(m_currentDataLocator.getComments(),m_ncurrentNumberNInNBestList);
				//int[][] nstates = m_hmmSet.getPathsOfNBestList();
				nindicesOfNBestList = SetOfPlainContinuousHMMs.getNBestListHMMIndices(pathsOfNBestListUsingRunLengthEncoding,m_ncurrentNumberNInNBestList);
				npositionOfBest = findPositionInList(nindicesOfNBestList, nhMMIndex);
			}
		}

		int npositionOfSecond = -1;
		if (npositionOfBest != 0) {
			//error
			npositionOfSecond = 0;
		}
		else {
			//match
			npositionOfSecond = 1;
		}
		m_npositionOfBestHMMInNBestList = npositionOfBest;
		m_npositionOfSecondBestHMMInNBestList = npositionOfSecond;

		m_correctHMMPositionTextField.setText(Integer.toString(m_npositionOfBestHMMInNBestList+1));
		m_correctHMMLabelLabel.setText(correctHMMLabel);
		m_nbestCandidatePositionTextField.setText(Integer.toString(m_npositionOfSecondBestHMMInNBestList+1));
		m_bestCandidateLabelLabel.setText(m_tableOfLabels.getFirstLabel(nindicesOfNBestList[m_npositionOfSecondBestHMMInNBestList]));

		StringBuffer stringBuffer = new StringBuffer("Order:");
		for (int i = 0; i < m_ncurrentNumberNInNBestList; i++) {
			stringBuffer.append(" " + m_tableOfLabels.getFirstLabel(nindicesOfNBestList[i]));
		}
		m_segmentTextField.setText(stringBuffer.toString());
	}

	private void calculateAndShowScores(String bestHMMlabel) {

		int[] nendpoints = m_currentDataLocator.getEndpointsFromGivenSegment(0);
		int nhMMIndex = m_tableOfLabels.getEntry(m_currentDataLocator.getLabelFromGivenSegment(0));

		//get SOP of sentence correspondent to DataLocator
		String wavFileName = m_currentDataLocator.getFileName();
		wavFileName = FileNamesAndDirectories.getFileNameFromPath(wavFileName);
		String sOPFileName = FileNamesAndDirectories.substituteExtension(wavFileName,SetOfPatterns.m_FILE_EXTENSION);
		sOPFileName = m_directoryWithSentenceSOPFiles + sOPFileName;

		//open such SetOfPatterns if necessary
		if (!sOPFileName.equals(m_currentSetOfPatternsFileName)) {
			//if not opened yet
			m_currentSetOfPatternsFileName = sOPFileName;
			SetOfPatterns setOfPatterns = new SetOfPatterns(sOPFileName);
			if (setOfPatterns.getNumberOfPatterns() != 1) {
				End.throwError(sOPFileName + " should have only 1 pattern!");
			}
			if (!m_patternGenerator.equals(setOfPatterns.getPatternGenerator())) {
				End.throwError("Different pattern generator's: " +
						m_patternGenerator.toString() + "\nand\n" +
						setOfPatterns.getPatternGenerator().toString());
			}
			m_pattern = setOfPatterns.getPattern(0);
		}

		int[] nframeIndices = m_patternGenerator.fromSamplesToFrames(nendpoints);
		//create a Pattern with that frames
		if (nframeIndices[0] > m_pattern.getNumOfFrames() - 1) {
			nframeIndices[0] = m_pattern.getNumOfFrames() - 1;
		}
		if (nframeIndices[1] > m_pattern.getNumOfFrames() - 1) {
			nframeIndices[1] = m_pattern.getNumOfFrames() - 1;
		}
		Pattern pattern = m_pattern.getPartOfPattern(nframeIndices[0],
				nframeIndices[1]);

		//based on how many scores user wants, get information
		//from lattice and and calculate scores per frame

		//m_hmmSet.findBestModelAndItsScore(pattern);
		//double[] dscores = m_hmmSet.getScoresInNBestList();
		//Print.dialog(m_hmmSet.getHMMLabelsInNBestListAsString());
		//IO.displayPartOfVector(dscores, 100);


//		hMMViewer[nindicesOfNBestList[npositionOfBest]].composeImageAndSaveFiles(pattern, nstates[npositionOfBest], true);
//		hMMViewer[nindicesOfNBestList[npositionOfSecond]].composeImageAndSaveFiles(pattern, nstates[npositionOfSecond], false);

		//load lattice
		m_hmmSet.setPathsOfNBestList(m_currentDataLocator.getComments(),m_ncurrentNumberNInNBestList);
		int[][] nstates = m_hmmSet.getPathsOfNBestList();
		int[] nindicesOfNBestList = m_hmmSet.getHMMIndicesInNBestList();

		//Print.dialog("m_npositionOfBestHMMInNBestList = " + m_npositionOfBestHMMInNBestList);
		//Print.dialog("m_npositionOfSecondBestHMMInNBestList = " + m_npositionOfSecondBestHMMInNBestList);
		//if (pattern.getNumOfFrames() == nstates[0].length) {
		float[] fscoresFirst = m_hmmSet.getScoreEvolution(pattern, nstates[m_npositionOfBestHMMInNBestList], nindicesOfNBestList[m_npositionOfBestHMMInNBestList]);
		float[] fscoresSecond = m_hmmSet.getScoreEvolution(pattern, nstates[m_npositionOfSecondBestHMMInNBestList], nindicesOfNBestList[m_npositionOfSecondBestHMMInNBestList]);
		float[][] fscoresOfAllList = m_hmmSet.getScoreEvolutionForHMMList(pattern, nstates, nindicesOfNBestList);
		plotScoresInMatlab(fscoresFirst,fscoresSecond,fscoresOfAllList,nstates);
		//} else {
		//	Print.warning("Race condition: did not finish calculation");
		//}

		//ak
		ContinuousHMM[] continuousHMMs = m_hmmSet.getHMMs();
		float[][] fscoresPerDimension = continuousHMMs[nindicesOfNBestList[m_npositionOfBestHMMInNBestList]].getScoreEvolutionPerDimension(pattern,nstates[m_npositionOfBestHMMInNBestList]);
		//double[][] dscoresPerDimension = Cloner.cloneAsDouble(fscoresPerDimension);
		sendArrayToMatlab(fscoresPerDimension,"scoresPerDim1");
		fscoresPerDimension = continuousHMMs[nindicesOfNBestList[m_npositionOfSecondBestHMMInNBestList]].getScoreEvolutionPerDimension(pattern,nstates[m_npositionOfSecondBestHMMInNBestList]);
		sendArrayToMatlab(fscoresPerDimension,"scoresPerDim2");
		//take the transpose and plot
		sendCommandToMatlab("figure(2);subplot(212);imagesc((scoresPerDim1-scoresPerDim2)')");


		//IO.writeVectortoASCIIFile("c:/temp/scores.txt", fscoresFirst);
		//IO.writeMatrixtoASCIIFile("c:/temp/allscores.txt", fscoresOfAllList);

		//play only segment
		//AudioPlayer.playScaled(labeledSpeech.getAudioFromGivenSegment(0));

		//m_engine.engEvalString("viewhmm");
//		IO.pause();
//		i++;
	}

	private void sendArrayToMatlab(float[] array, String arrayName) {
		MatlabInterfacer.sendArray(array, arrayName);
	}

	private void sendArrayToMatlab(float[][] array, String arrayName) {
		MatlabInterfacer.sendArray(array, arrayName);
	}

	private void sendArrayToMatlab(int[][] array, String arrayName) {
		MatlabInterfacer.sendArray(array, arrayName);
	}

	private void plotScoresInMatlab(float[] fscoresFirst,
			float[] fscoresSecond,float[][] fscoresOfAllList, int[][] nstates) {
		sendArrayToMatlab(fscoresFirst,"fscoresFirst");
		sendArrayToMatlab(fscoresSecond,"fscoresSecond");
		sendArrayToMatlab(fscoresOfAllList,"fscoresOfAllList");
		sendArrayToMatlab(nstates,"nstates");
		//take the transpose and plot
		sendCommandToMatlab("figure(2);clf;subplot(211);plot(fscoresOfAllList','g'); hold on");
		sendCommandToMatlab("plot(fscoresFirst,'r');");
		sendCommandToMatlab("plot(fscoresSecond,'b');");
		sendCommandToMatlab("title('Red: correct, Blue: second, Green: other candidates');");

		//IO.DisplayMatrix(nstates);
		if (m_oshouldShowStatesInScoreEvolution) {
			sendCommandToMatlab("markStates(nstates,fscoresOfAllList);");

//			sendCommandToMatlab("[nlin ncol] = size(nstates);");
//			sendCommandToMatlab("for i=1:nlin");
//			sendCommandToMatlab(" for j=1:ncol");
//			sendCommandToMatlab("  if nstates(i,j) == 1");
//			sendCommandToMatlab("   plot(j,fscoresOfAllList(i,j),'r+')");
//			sendCommandToMatlab("  end");
//			sendCommandToMatlab("  if nstates(i,j) == 2");
//			sendCommandToMatlab("   plot(j,fscoresOfAllList(i,j),'k*')");
//			sendCommandToMatlab("  end");
//			sendCommandToMatlab("  if nstates(i,j) == 3");
//			sendCommandToMatlab("   plot(j,fscoresOfAllList(i,j),'go')");
//			sendCommandToMatlab("  end");
//			sendCommandToMatlab(" end");
//			sendCommandToMatlab("end");

//			for (int i = 0; i < nstates.length; i++) {
//			for (int j = 0; j < nstates[i].length; j++) {
//			if (nstates[i][j] == 1) {
//			float fyAxisValue = fscoresOfAllList[i][j];
//			sendCommandToMatlab("plot("+(j+1)+","+fyAxisValue+",'ro')");
//			} else if (nstates[i][j] == 2) {
//			float fyAxisValue = fscoresOfAllList[i][j];
//			sendCommandToMatlab("plot("+(j+1)+","+fyAxisValue+",'ko')");
//			} else if (nstates[i][j] == 3) {
//			float fyAxisValue = fscoresOfAllList[i][j];
//			sendCommandToMatlab("plot("+(j+1)+","+fyAxisValue+",'go')");
//			}
//			}
//			}
		}

		sendCommandToMatlab("grid");
	}

	private void plotWaveformSpectrogramAndLabelsInMatlab(DataLocator dataLocatorAllSegments,
			DataLocator dataLocatorWithSegmentsOfInterest,int nfirstSegment, int nlastSegment) {

		//keep endpoints of segments range of interest
		int[] nendPoints = new int[2];
		nendPoints[0] = dataLocatorAllSegments.getBeginningEndpointFromGivenSegment(nfirstSegment);
		nendPoints[1] = dataLocatorAllSegments.getEndingEndpointFromGivenSegment(nlastSegment);

		double[] daudioSamples = Cloner.cloneAsDouble(m_audioOfCurrentSegmentsOfInterest.getAudioDataAsIntegers());
		MatlabInterfacer.sendArray(daudioSamples, "daudioSamples");

		//get an array to be used by Matlab for plotting
		int[] nrelativeEndPoints = dataLocatorWithSegmentsOfInterest.getAllBegginingAndLastEndingEndpointsAsOneArray();
		for (int i = 0; i < nrelativeEndPoints.length; i++) {
			nrelativeEndPoints[i] -= nendPoints[0];
		}
		//IO.DisplayVector(nrelativeEndPoints);
		float[] frelativeEndPoints = convertToTime(nrelativeEndPoints, 11025F);
		//IO.DisplayVector(frelativeEndPoints);

		sendArrayToMatlab(frelativeEndPoints,"drelativeEndPoints");
		//double[] drelativeEndPoints = Cloner.cloneAsDouble(frelativeEndPoints);
		//m_engine.engPutArray("drelativeEndPoints", drelativeEndPoints);

		//float[] frelativeEndPoints = convertToTime(nrelativeEndPoints, 16000F);
		//m_engine.engEvalString("figure(2);clf;subplot(311);waveplot(daudioSamples,16000)");
		sendCommandToMatlab("figure(1);clf;");
		sendCommandToMatlab("subplot(211);ak_specgram(daudioSamples," +
				m_nspectrogramBWInHz + ",11025," + m_nwindowShift + ",60);");
		sendCommandToMatlab("axis([0 " + (daudioSamples.length-1.0)/11025.0 + " 0 8000]);");
		//m_engine.engEvalString("set(gca,'xtick',[" + frelativeEndPoints[0] + " " + frelativeEndPoints[1] + "]);grid");
		sendCommandToMatlab("set(gca,'xtick',drelativeEndPoints);grid");
		//akspecgram(x,filterBWInHz,samplingFrequency,windowShiftInms,thresholdIndB)
		//m_engine.engEvalString("subplot(312);akspecgram(daudioSamples,200,16000,1,60)");
		sendCommandToMatlab("subplot(212);plot([0:length(daudioSamples)-1]/11025,daudioSamples)");
		//m_engine.engEvalString("set(gca,'xtick',[" + frelativeEndPoints[0] + " " + frelativeEndPoints[1] + "]);grid");
		sendCommandToMatlab("set(gca,'xtick',drelativeEndPoints);grid");
		//Print.dialog("set(gca,'xtick',["+frelativeEndPoints[0]+" "+frelativeEndPoints[1]+"])");
		//plotTIMITLabels(dataLocator, m_engine, dallAudioSamples.length, 0);
		plotLabelsInMatlab(dataLocatorAllSegments,nfirstSegment,nlastSegment);
	}

	void specBWTextField_actionPerformed(ActionEvent e) {
		m_nspectrogramBWInHz = getInteger(m_specBWTextField);
		m_specBWTextField.setText(Integer.toString(m_nspectrogramBWInHz));
	}

	void windowShiftTextField_actionPerformed(ActionEvent e) {
		m_nwindowShift = getInteger(m_windowShiftTextField);
		m_windowShiftTextField.setText(Integer.toString(m_nwindowShift));
	}

	void playOnlyCurrentButton_actionPerformed(ActionEvent e) {
		playAudioOfCurrentSegmentOnly();
	}

	void showScoresCheckBox_actionPerformed(ActionEvent e) {
		m_oshowScores = m_showScoresCheckBox.isSelected();
		if(!m_oshowScores) {
			sendCommandToMatlab("close(2)");
		}
	}

	void muteAudioCheckBox_actionPerformed(ActionEvent e) {
		m_oisAudioMute = m_muteAudioCheckBox.isSelected();
	}

}

//int[] nallAudioSamples = labeledSpeech.getAudioOfWholeSentence().getAudioDataAsIntegers();

//int nnumberOfSamplesToInclude = 2048;
//int[] nrelativeEndPoints = findRelativeEndpoints(nendPoints, nnumberOfSamplesToInclude, nallAudioSamples.length);
//IO.DisplayVector(nrelativeEndPoints);

////double[] dallAudioSamples = Cloner.cloneAsDouble(nallAudioSamples);
//AudioPlayer.playRegionScaled(labeledSpeech.getAudioOfWholeSentence(), nendPoints[0] - nnumberOfSamplesToInclude, nendPoints[1] + nnumberOfSamplesToInclude);
//double[] dallAudioSamples = Cloner.cloneRegionAsDouble(nallAudioSamples, nendPoints[0], nendPoints[1], nnumberOfSamplesToInclude);
//nallAudioSamples = null;
//m_engine.engPutArray("dallAudioSamples", dallAudioSamples);

//float[] frelativeEndPoints = convertToTime(nrelativeEndPoints, 16000F);
////m_engine.engEvalString("figure(2);clf;subplot(311);waveplot(daudioSamples,16000)");
//m_engine.engEvalString("figure(2);clf;");
//m_engine.engEvalString("subplot(211);akspecgram(dallAudioSamples,200,16000,1,60)");
//m_engine.engEvalString("set(gca,'xtick',[" + frelativeEndPoints[0] + " " + frelativeEndPoints[1] + "]);grid");
////akspecgram(x,filterBWInHz,samplingFrequency,windowShiftInms,thresholdIndB)
////m_engine.engEvalString("subplot(312);akspecgram(daudioSamples,200,16000,1,60)");
//m_engine.engEvalString("subplot(212);waveplot(dallAudioSamples,16000)");
//m_engine.engEvalString("set(gca,'xtick',[" + frelativeEndPoints[0] + " " + frelativeEndPoints[1] + "]);grid");
////Print.dialog("set(gca,'xtick',["+frelativeEndPoints[0]+" "+frelativeEndPoints[1]+"])");
//plotTIMITLabels(dataLocator, m_engine, dallAudioSamples.length, nnumberOfSamplesToInclude);

//Pattern pattern = setOfPatterns.getPattern(i);
//m_hmmSet.findBestModelAndItsScore(pattern);
//int[][] nstates = m_hmmSet.getPathsOfNBestList();
//double[] dscores = m_hmmSet.getScoresInNBestList();
//int[] nindicesOfNBestList = m_hmmSet.getHMMIndicesInNBestList();
//Print.dialog(m_hmmSet.getHMMLabelsInNBestListAsString());
//IO.displayPartOfVector(dscores, 100);

//int npositionOfBest = findPositionInList(nindicesOfNBestList, nhMMIndex);

//if (npositionOfBest == -1) {
//End.throwError("Not found in N-best list");
//}

//int npositionOfSecond = -1;
//if (npositionOfBest != 0) {
////error
//npositionOfSecond = 0;
//}
//else {
////match
//npositionOfSecond = 1;
//}

//hMMViewer[nindicesOfNBestList[npositionOfBest]].composeImageAndSaveFiles(pattern, nstates[npositionOfBest], true);
//hMMViewer[nindicesOfNBestList[npositionOfSecond]].composeImageAndSaveFiles(pattern, nstates[npositionOfSecond], false);

//float[] fscoresFirst = m_hmmSet.getScoreEvolution(pattern, nstates[npositionOfBest], nhMMIndex);
//float[] fscoresSecond = m_hmmSet.getScoreEvolution(pattern, nstates[npositionOfSecond], nindicesOfNBestList[npositionOfSecond]);

//float[][] fscoresOfAllList = hmmSet.getScoreEvolutionForHMMList(pattern, nstates, nindicesOfNBestList);

//IO.writeVectortoASCIIFile("c:/temp/scores.txt", fscoresFirst);
//IO.writeMatrixtoASCIIFile("c:/temp/allscores.txt", fscoresOfAllList);

////play only segment
//AudioPlayer.playScaled(labeledSpeech.getAudioFromGivenSegment(0));

//m_engine.engEvalString("viewhmm");
//IO.pause();
//i++;
//}

//m_engine.engClose();
//m_engine.kill();

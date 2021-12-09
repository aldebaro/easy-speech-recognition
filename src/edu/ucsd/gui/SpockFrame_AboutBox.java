/**Some description of UCSD ASR software...
 * Title:        Spock<p>
 * @author Aldebaro Klautau
 * @version 2.0 - September 26, 2000.
 */
package edu.ucsd.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
//import com.borland.jbcl.layout.*;

public class SpockFrame_AboutBox extends JDialog implements ActionListener {

	JPanel panel1 = new JPanel();
	JPanel panel2 = new JPanel();
	JPanel insetsPanel1 = new JPanel();
	JPanel insetsPanel2 = new JPanel();
	JPanel insetsPanel3 = new JPanel();
	JButton button1 = new JButton();
	ImageIcon imageIcon;
	BorderLayout borderLayout1 = new BorderLayout();
	BorderLayout borderLayout2 = new BorderLayout();
	FlowLayout flowLayout1 = new FlowLayout();
	FlowLayout flowLayout2 = new FlowLayout();
//	String product = "Spock";
//	String version = "1.0";
//	String copyright = "Copyright (c) Aldebaro Klautau";
//	String comments = "Simple word recognition in Java";
	JButton jButton1 = new JButton();
	BorderLayout borderLayout3 = new BorderLayout();
	JScrollPane jScrollPane1 = new JScrollPane();
	JTextArea jTextArea1 = new JTextArea();
	public SpockFrame_AboutBox(Frame parent) {
		super(parent);
		enableEvents(AWTEvent.WINDOW_EVENT_MASK);
		try {
			jbInit();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		//imageControl1.setIcon(imageIcon);
		pack();
	}

	private void jbInit() throws Exception  {
		//imageIcon = new ImageIcon(getClass().getResource("[Your Image]"));
		this.setTitle("About Spock");
		setResizable(false);
		panel1.setLayout(borderLayout1);
		panel2.setLayout(borderLayout2);
		insetsPanel1.setLayout(flowLayout1);
		insetsPanel2.setLayout(flowLayout1);
		insetsPanel2.setBackground(Color.white);
		insetsPanel2.setBorder(new EmptyBorder(10, 10, 10, 10));
		insetsPanel3.setLayout(borderLayout3);
		//insetsPanel3.setBorder(new EmptyBorder(10, 60, 10, 10));
		button1.setText("Ok");
		button1.addActionListener(this);
		jButton1.setBackground(Color.white);
		jButton1.setBorder(null);
		jButton1.setMinimumSize(new Dimension(410, 340));
		jButton1.setPreferredSize(new Dimension(410, 340));
		//jButton1.setIcon(new ImageIcon(SpockFrame_AboutBox.class.getResource("asrjoke.gif")));
		jButton1.setIcon(new ImageIcon(SpockFrame_AboutBox.class.getResource("spock.jpg")));
		panel1.setBackground(Color.white);
		panel2.setBackground(Color.white);
		insetsPanel3.setBackground(Color.white);
		insetsPanel3.setPreferredSize(new Dimension(495, 380));
		insetsPanel1.setBackground(Color.white);
		//jTextPane1.setText("AQUI");
		jTextArea1.setText(""+
		"                        Spock version 1.0\n\n" +
		"The package consists of a subset of classes written in 2002 for the UCSD\n" +
		"ASR project coordinated by Prof. Alon Orlitsky. The project aims\n"+
		"to implement a complete speech recognition system in Java, besides\n" +
		"other things.\n" + 
		"This GUI only supports \"isolated\" speech recognition, also called\n" +
		"\"classification\" when one is dealing with phones or phonemes.\n" +
		"Students:\n" +
		"Nikola Jevtic, Aldebaro Klautau, David Krambs,\n" +  
		"Narayana Prasad Santhanam, Roy Tenny, Krishnamurthy Narayana and Junan Zhang.\n" +
		"Spock stands for Speech PrOCessing and reKognition.\n\n" +
		"The code is now maintained by LaPS,\n" +
		"at the Federal University of Para, Brazil.\n" +
		"You can get support at http://www.laps.ufpa.br/falabrasil/");
		insetsPanel2.add(jButton1, null);
		panel2.add(insetsPanel2, BorderLayout.WEST);
		this.getContentPane().add(panel1, null);
		panel2.add(insetsPanel3, BorderLayout.CENTER);
		insetsPanel3.add(jScrollPane1, BorderLayout.CENTER);
		jScrollPane1.getViewport().add(jTextArea1, null);
		insetsPanel1.add(button1, null);
		panel1.add(insetsPanel1, BorderLayout.SOUTH);
		panel1.add(panel2, BorderLayout.NORTH);
		jTextArea1.setCaretPosition(0);
	}

	protected void processWindowEvent(WindowEvent e) {
		if (e.getID() == WindowEvent.WINDOW_CLOSING) {
			cancel();
		}
		super.processWindowEvent(e);
	}

	void cancel() {
		dispose();
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == button1) {
			cancel();
		}
	}
}
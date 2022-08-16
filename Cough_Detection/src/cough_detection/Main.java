package cough_detection;

import javax.swing.JOptionPane;

/**
 *
 * @author evgeni
 */
public class Main {

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		Recorder.init();
		
		JOptionPane.showMessageDialog(null, "Hit ok to start recording");
		Recorder.start();
		
		JOptionPane.showMessageDialog(null, "Hit ok to stop recording");
		Recorder.stop();
	}
}

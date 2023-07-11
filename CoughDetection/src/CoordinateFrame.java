import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

public class CoordinateFrame extends JFrame {
	
	private static final long serialVersionUID = 1L;
	private final CoordinatePanel panel;
	
	/*
	 * Erstellt neues Objekt des JFrames mit Parameter-Eingaben
	 */
	public CoordinateFrame(String title, double[] amplitudes, double sigma, int[] portions, int[] cough_portions) {
		this.setLayout(new BorderLayout());
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setTitle("");
		panel = new CoordinatePanel(amplitudes, sigma, portions, cough_portions);
		
		JPanel container = new JPanel(new BorderLayout());
		container.add(panel, BorderLayout.CENTER);
		container.setBorder(new TitledBorder(title));
		this.add(container, BorderLayout.CENTER);
		this.pack();
		this.setSize(800, 300);
		this.setVisible(true);
		panel.drawSoundwave();
	}
}

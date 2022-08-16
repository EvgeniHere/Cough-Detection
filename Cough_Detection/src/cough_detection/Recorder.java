package cough_detection;

import java.awt.HeadlessException;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import javax.swing.JOptionPane;

/**
 *
 * @author evgeni
 */
public abstract class Recorder {
	
	static AudioFormat aF;
	static DataLine.Info dF;
	static TargetDataLine tL;
	static Thread audioRecorderThread;
	
	public static void init() {
		aF = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 48000, 16, 2, 4, 48000, false);
            
		dF = new DataLine.Info(TargetDataLine.class, aF);

		if (!AudioSystem.isLineSupported(dF)) {
			System.err.println("Not Supported audio format");
		}
	}
	
	public static void start() {
		if (aF == null || dF == null) {
			System.out.println("Recorder not initialized!");
			return;
		}
		
		try {
			tL = (TargetDataLine)AudioSystem.getLine(dF);
			tL.open();
			tL.start();
			System.out.println("Started Recording");

			audioRecorderThread = new Thread() {
				@Override public void run() {
					AudioInputStream audioIS = new AudioInputStream(tL);

					File outputFile = new File("record.wav");

					try {
						AudioSystem.write(audioIS, AudioFileFormat.Type.WAVE, outputFile);
					} catch(IOException e) {
						System.err.println(e);
					}
				}
			};

			audioRecorderThread.start();
		} catch (LineUnavailableException ex) {
			Logger.getLogger(Recorder.class.getName()).log(Level.SEVERE, null, ex);
        } catch(HeadlessException e) {
            System.err.println(e);
        }
	}
	
	public static void stop() {
		if (!tL.isActive())
			return;
		
		tL.stop();
		System.out.println("Stopped Recording");
	}
}

import javax.sound.sampled.*;

import java.io.*;

public class Recorder {
    private static File wavFile;
    private static AudioFileFormat.Type fileType = AudioFileFormat.Type.WAVE;
    private static TargetDataLine line;
    private float SAMPLE_RATE;
 
    /**
     * Defines an audio format
     */
    AudioFormat getAudioFormat() {
        int sampleSizeInBits = 16;
        int channels = 1;
        boolean signed = true;
        boolean bigEndian = false;
        return new AudioFormat(SAMPLE_RATE, sampleSizeInBits, channels, signed, bigEndian);
    }
 
    /**
     * Captures the sound and record into a WAV file
     */
    void start(String FILE_NAME, long RECORD_TIME, float SAMPLE_RATE) {
    	wavFile = new File(FILE_NAME);
    	this.SAMPLE_RATE = SAMPLE_RATE;
    	Thread stopper = new Thread(new Runnable() {
            public void run() {
                try {
                	Thread.sleep(RECORD_TIME);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                finish();
            }
        });
        stopper.start();
        
        try {
            AudioFormat format = getAudioFormat();
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
 
            if (!AudioSystem.isLineSupported(info)) {
                System.out.println("Line not supported");
                System.exit(0);
            }
            
            line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();
 
            System.out.println("Start capturing...");
 
            AudioInputStream ais = new AudioInputStream(line);
 
            System.out.println("Start recording for " + (RECORD_TIME / 1000) + " seconds...");
 
            AudioSystem.write(ais, fileType, wavFile);
        } catch (LineUnavailableException ex) {
            ex.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
    
    /**
     * Closes the target data line to finish capturing and recording
     */
    void finish() {
        line.stop();
        line.close();
        System.out.println("Stopped recording.");
        Main.detectCoughs("input.wav");
    }
}

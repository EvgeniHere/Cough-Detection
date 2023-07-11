import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.ServiceLoader;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.spi.AudioFileWriter;

public class Resampler {

	/*
	 * Resampled Audiodatei auf eingegebene sample_rate und speichert diese mit gleichem Dateinamen im Resampled-Ordner
	 */
	public static void resampleMedia(String file_path, String file_name, int sample_rate) throws UnsupportedAudioFileException, IOException {
    	File file = new File(file_path);
        AudioInputStream audioIn = AudioSystem.getAudioInputStream(file);
        AudioFormat srcFormat = audioIn.getFormat();

        AudioFormat dstFormat = new AudioFormat(srcFormat.getEncoding(),
        		sample_rate,
                srcFormat.getSampleSizeInBits(),
                srcFormat.getChannels(),
                srcFormat.getFrameSize(),
                sample_rate,
                srcFormat.isBigEndian());

        AudioInputStream convertedIn = AudioSystem.getAudioInputStream(dstFormat, audioIn);
        
        File resampled_wav_file = new File("resampled/coughs_testing/" + file_name + ".wav");
        
        ServiceLoader<AudioFileWriter> writers = ServiceLoader.load(AudioFileWriter.class);
        Iterator<AudioFileWriter> it = writers.iterator();
        while (it.hasNext()) {
        	AudioFileWriter writer = it.next();
        	if (!writer.isFileTypeSupported(AudioFileFormat.Type.WAVE))
        		continue;
        	writer.write(convertedIn, AudioFileFormat.Type.WAVE, resampled_wav_file);
        	break;
        }
    }
    
	/*
	 * Iteriert über alle Audiodateien im Ordner und führt Resampling aus
	 */
    public static void resampleDir(int sample_rate) {
    	File dir = new File("coughs_testing");
		File[] directoryListing = dir.listFiles();
		int index = 0;
		if (directoryListing == null)
			return;
		
		for (File child : directoryListing) {
			System.out.println("File " + index + " \"" + child.getName() + "\": ");
			String file_path = "coughs_testing/" + child.getName();
			
			try {
				resampleMedia(file_path, String.valueOf(index), sample_rate);
				index++;
			} catch (UnsupportedAudioFileException | IOException e) {
				e.printStackTrace();
			}
		}
    }
}

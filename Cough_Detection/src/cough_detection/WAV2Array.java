package cough_detection;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 *
 * @author evgeni
 */
public class WAV2Array {

    private byte[] entireFileData;

    public double getSampleRate(){
        ByteBuffer wrapped = ByteBuffer.wrap(Arrays.copyOfRange(entireFileData, 24, 28)); // big-endian by default
        double sampleRate = wrapped.order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();
        return sampleRate;
    }

    public WAV2Array(String filepath, boolean print_info) throws IOException{
        Path path = Paths.get(filepath);
        this.entireFileData = Files.readAllBytes(path);

        if (print_info) {
			//extract format
			String format = new String(Arrays.copyOfRange(entireFileData, 8, 12), "UTF-8");

			//extract number of channels
			int noOfChannels = entireFileData[22];
			String noOfChannels_str;
			switch (noOfChannels) {
				case 1:
					noOfChannels_str = "1 (mono)";
					break;
				case 2:
					noOfChannels_str = "2 (stereo)";
					break;
				default:
					noOfChannels_str = noOfChannels + "(more than 2 channels)";
					break;
			}

			//extract sampling rate (SR)
			int SR = (int) this.getSampleRate();

			//extract Bit Per Second (BPS/Bit depth)
			int BPS = entireFileData[34];

			System.out.println("---------------------------------------------------");
			System.out.println("File path:          " + filepath);
			System.out.println("File format:        " + format);
			System.out.println("Number of channels: " + noOfChannels_str);
			System.out.println("Sampling rate:      " + SR);
			System.out.println("Bit depth:          " + BPS);
			System.out.println("---------------------------------------------------");
        }
    }

    public double[] getByteArray (){
        byte[] data_raw = Arrays.copyOfRange(entireFileData, 44, entireFileData.length);
        int totalLength = data_raw.length;

        //declare double array for mono
        int new_length = totalLength/4;
        double[] data_mono = new double[new_length];

        double left, right;
        for (int i = 0; i < new_length; i++) {
            left = ((data_raw[i] & 0xff) << 8) | (data_raw[i+1] & 0xff);
            right = ((data_raw[i+2] & 0xff) << 8) | (data_raw[i+3] & 0xff);
            data_mono[i] = (left + right) / 2.0;
        }
		
        return data_mono;
    }
}

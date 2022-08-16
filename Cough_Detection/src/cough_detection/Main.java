package cough_detection;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import org.jtransforms.fft.DoubleFFT_1D;

/**
 *
 * @author evgeni
 */
public class Main {
	
	public static Color getColor(double power) {
        double H = power * 0.4; // Hue (note 0.4 = Green, see huge chart below)
        double S = 1.0; // Saturation
        double B = 1.0; // Brightness

        return Color.getHSBColor((float)H, (float)S, (float)B);
    }

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		Recorder.init();
		
		JOptionPane.showMessageDialog(null, "Hit ok to start recording");
		Recorder.start();
		
		JOptionPane.showMessageDialog(null, "Hit ok to stop recording");
		Recorder.stop();
		
		// TODO Auto-generated method stub
        String filepath = "record.wav";
        try {
            //get raw double array containing .WAV data
            WAV2Array audioTest = new WAV2Array(filepath, true);
            double[] rawData = audioTest.getByteArray();
            int length = rawData.length;

            //initialize parameters for FFT
            int WS = 1024; //WS = window size
            int OF = 8;    //OF = overlap factor
            int windowStep = WS/OF;

            //calculate FFT parameters
            double SR = audioTest.getSampleRate();
            double time_resolution = WS/SR;
            double frequency_resolution = SR/WS;
            double highest_detectable_frequency = SR/2.0;
            double lowest_detectable_frequency = 5.0*SR/WS;

            System.out.println("time_resolution:              " + time_resolution*1000 + " ms");
            System.out.println("frequency_resolution:         " + frequency_resolution + " Hz");
            System.out.println("highest_detectable_frequency: " + highest_detectable_frequency + " Hz");
            System.out.println("lowest_detectable_frequency:  " + lowest_detectable_frequency + " Hz");

            //initialize plotData array
            int nX = (length-WS)/windowStep;
            int nY = WS/2;
            double[][] plotData = new double[nX][nY]; 

            //apply FFT and find MAX and MIN amplitudes

            double maxAmp = Double.MIN_VALUE;
            double minAmp = Double.MAX_VALUE;

            double amp_square;

            double[] inputImag = new double[length];

            for (int i = 0; i < nX; i++){
                Arrays.fill(inputImag, 0.0);
				DoubleFFT_1D fft = new DoubleFFT_1D(WS);
                double[] WS_array = Arrays.copyOfRange(rawData, i*windowStep, i*windowStep+WS);
				fft.realForward(WS_array);
                for (int j = 0; j < nY; j++){
                    amp_square = (WS_array[2*j]*WS_array[2*j]) + (WS_array[2*j+1]*WS_array[2*j+1]);
                    if (amp_square == 0.0){
                        plotData[i][j] = amp_square;
                    }
                    else{
                        plotData[i][j] = 10 * Math.log10(amp_square);
                    }

                    //find MAX and MIN amplitude
                    if (plotData[i][j] > maxAmp)
                        maxAmp = plotData[i][j];
                    else if (plotData[i][j] < minAmp)
                        minAmp = plotData[i][j];
                }
            }

            System.out.println("---------------------------------------------------");
            System.out.println("Maximum amplitude: " + maxAmp);
            System.out.println("Minimum amplitude: " + minAmp);
            System.out.println("---------------------------------------------------");

            //Normalization
            double diff = maxAmp - minAmp;
            for (int i = 0; i < nX; i++){
                for (int j = 0; j < nY; j++){
                    plotData[i][j] = (plotData[i][j]-minAmp)/diff;
                }
            }

            //plot image
            BufferedImage theImage = new BufferedImage(nX, nY, BufferedImage.TYPE_INT_RGB);
            double ratio;
            for(int x = 0; x<nX; x++){
                for(int y = 0; y<nY; y++){
                    ratio = plotData[x][y];

                    //theImage.setRGB(x, y, new Color(red, green, 0).getRGB());
                    Color newColor = getColor(1.0-ratio);
                    theImage.setRGB(x, y, newColor.getRGB());
                }
            }
            File outputfile = new File("saved.png");
            ImageIO.write(theImage, "png", outputfile);
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
	
	public static void print(double[] arr) {
		for (int i = 0; i < arr.length; i++)
			System.out.print(arr[i] + ", ");
		System.out.println();
	}
}

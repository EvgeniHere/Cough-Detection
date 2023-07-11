import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;

import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.regression.SimpleRegression;

import comirva.audio.util.MFCC;

public class Utility {

	/*
	 * Berechnet Mittelwert aus einem Array mit Start- (inklusiv) und Endindex (exklusiv)
	 */
    public static double calculateMean(double[] array, int start, int end) {
    	double sum = 0;
    	for (int i = start; i < end; i++) {
    		sum += Math.abs(array[i]);
    	}
    	return sum/(end - start);
    }
    
    /*
     * Berechnet die Varianz aus einem Array mit Start- (inklusiv) und Endindex (exklusiv)
     */
    public static double calculateVariance(double[] array, int start, int end) {
    	double sum = 0;
    	double mean = calculateMean(array, start, end);
    	for (int i = start; i < end; i++) {
    		sum += (array[i] - mean) * (array[i] - mean);
    	}
    	return sum/(end - start - 1);
    }
    
    /*
     * Berechnet Standardabweichung aus einem Array mit Start- (inklusiv) und Endindex (exklusiv)
     */
    public static double calculateStandardDeviation(double[] array, int start, int end) {
    	return Math.sqrt(calculateVariance(array, start, end));
    }
    
    /*
     * Berechnet Mittelwert eines Arrays
     */
    public static double calculateMean(double[] array) {
    	return calculateMean(array, 0, array.length);
    }

    /*
     * Berechnet Standardabweichung eines Arrays
     */
    public static double calculateStandardDeviation(double[] array) {
    	return Math.sqrt(calculateVariance(array, 0, array.length));
    }

    /*
     * Berechnet Varianz eines Arrays
     */
    public static double calculateVariance(double[] array) {
    	return calculateVariance(array, 0, array.length);
    }
    
    /*
     * Bererchnet Ableitung eines Arrays
     */
    public static double[] derivative(double[] array) {
    	double[] delta_array = new double[array.length];
    	delta_array[0] = array[0];
		for (int i = 1; i < delta_array.length; i++) {
			delta_array[i] = array[i - 1];
		}
		return delta_array;
    }
    
    /*
     * Erzeugt 2D MFCC-Array aus einem Amplituden-Array
     * 
     * Das 2D MFCC-Array ist zeilenförmig zu lesen. Die erste Zeile sind die MFCCs für das erste Window-Zeitfenster, usw.
     */
    public static double[][] generateMFCC(double[] amplitudes, int sample_rate, int window_size, int num_coef) {
    	double[][] mfccs = new double[1][1];
    	MFCC mfcc = new MFCC(sample_rate, window_size, num_coef, true);
    	
    	try {
    		mfccs = mfcc.process(amplitudes);
		} catch (IllegalArgumentException | IOException e) {
			e.printStackTrace();
		}
    	
    	return mfccs;
    }
    
    /*
     * Ausgabe eines Arrays in der Konsole
     */
    public static void printArray(double[] array) {
    	if (array == null)
    		return;
    	DecimalFormat df = new DecimalFormat("#.###");
    	for (int i = 0; i < array.length; i++) {
    		System.out.print(df.format(array[i]) + "; ");
    	}
    	System.out.println();
    }
    
    public static double[] calculateColumnMeans(double[][] array) {
    	double[] means = new double[array[0].length];
    	for (int i = 0; i < array[0].length; i++) {
    		for (int j = 0; j < array.length; j++) {
    			means[i] += array[j][i];
    		}
    		means[i] /= array.length;
    	}
    	return means;
    }
	
    /*
     * Finde die größten n Werte aus einem Array und gib deren Indizes zurück
     */
	public static int[] findTopNValues(double[] array, int n) {
		int[] topNValIdxs = new int[n];
		int cur_max_idx = -1;
		double cur_max_value = 0;
		for (int i = 0; i < array.length; i++) {
			if (Math.abs(array[i]) > cur_max_value) {
				cur_max_value = Math.abs(array[i]);
				cur_max_idx = i;
			}
		}
		topNValIdxs[0] = cur_max_idx;
		for (int i = 1; i < n; i++) {
			double next_max_value = 0;
			for (int j = 0; j < array.length; j++) {
				double cur_value = Math.abs(array[j]);
				if (cur_value < cur_max_value && cur_value > next_max_value) {
					next_max_value = cur_value;
					cur_max_idx = j;
				}
			}
			cur_max_value = next_max_value;
			topNValIdxs[i] = cur_max_idx;
		}
		return topNValIdxs;
	}
	
	/*
	 * Berechnet die Zero-crossing-rate eines Arrays
	 */
	public static double calculateZeroCrossingRate(int sample_rate, double[] audioData) {
        int numSamples = audioData.length;
        int numCrossing = 0;
        for (int p = 0; p < numSamples-1; p++) {
            if ((audioData[p] > 0 && audioData[p + 1] <= 0) || 
                (audioData[p] < 0 && audioData[p + 1] >= 0)) {
                numCrossing++;
            }
        }

        double numSecondsRecorded = (double)numSamples/(double)sample_rate;
        double numCycles = numCrossing/2.0;
        double frequency = numCycles/numSecondsRecorded;

        return frequency;
    }
	
	public static double[] maxOutAmps(double[] array) {
		ArrayList<Double> max_points = new ArrayList<>();
		for (int i = 1; i < array.length - 1; i++) {
			double val_before = Math.abs(array[i - 1]);
			double val = Math.abs(array[i]);
			double val_after = Math.abs(array[i + 1]);
			if (val > val_before && val > val_after)
				max_points.add(val);
		}
		double[] result = new double[max_points.size()];
		for (int i = 0; i < result.length; i++) {
			result[i] = max_points.get(i);
		}
		return result;
	}
	
	/*
	 * Erstellt aus den Parametern ein Features-Array für die ML-Modelle
	 * 
	 * Features-Array besteht aus folgender Reihenfolge:
	 * 1. bis 12. MFCC Werte
	 * 13. Standardabweichung
	 * 14. Kurtosis
	 * 15. Zero-crossing-rate
	 */
	public static double[] createFeatureVector(double mean_max_ratio, double kurtosis, double zcr, double[] mfccs) {
		double[] features = new double[3 + mfccs.length];
		for (int i = 0; i < mfccs.length; i++) {
			features[i] = mfccs[i];
		}
		features[mfccs.length] = mean_max_ratio;
		features[mfccs.length + 1] = kurtosis;
		features[mfccs.length + 2] = zcr;
		return features;
	}
	
	/*
	 * Findet laute Momente und gibt diese als ArrayList<Integer> zurück.
	 * 
	 * Die Elemente der Return-Liste sind die Timestamps mit jeweils Beginn und Ende eines lauten Moments
	 */
	public static ArrayList<Integer> getHighAmplitudeSplits(double[] amplitudes) {
		int frame_size = 128;
		double[] amp_means = new double[amplitudes.length / frame_size];
    	
    	for (int i = 0; i < amp_means.length; i++) {
    		amp_means[i] = calculateMean(amplitudes, i * frame_size, (i + 1) * frame_size);
    	}
    	
		double threshold = calculateStandardDeviation(amp_means);
    	ArrayList<Integer> timesplits = new ArrayList<>();
    	int first_exceeded = -1;
    	int last_exceeded = -1;
    	int exceed_counter = 0;
    	int subceed_counter = 0;
    	int end_counter = 0;
    	int in_sens = 10; // Mindestens 5 Stück der 256-großen Teile sind über dem Threshold
    	int out_sens = 100; // Wenn unter Threshold, Prüfe 40 weitere Stück der 256-großen Teile ob sie auch unter Threshold sind, sonst weiter wie über Threshold
    	int end_sens = 40;
    	
    	for (int i = 0; i < amp_means.length; i++) {
    		if (amp_means[i] > threshold) {
    			subceed_counter = 0;
    			
    			if (exceed_counter < in_sens) {
        			exceed_counter++;
    				continue;
    			}
    			
    			last_exceeded = i;
    			
    			if (first_exceeded == -1)
    				first_exceeded = i;
    		} else {
    			if (first_exceeded == -1)
    				continue;
    			
    			if (amp_means[i] < threshold / 10)
					end_counter++;
    			else
    				end_counter = 0;
					
    			if (end_counter < end_sens) {
    				subceed_counter++;
        			
        			if (subceed_counter < out_sens)
        				continue;
				}
    			
				int von = first_exceeded - in_sens - 1;
				
				if (von < 0)
					von = 0;
				
				int bis = last_exceeded;
				
    			timesplits.add(von);
				timesplits.add(bis);
    			
    			first_exceeded = -1;
    			last_exceeded = -1;
    			exceed_counter = 0;
				subceed_counter = 0;
				end_counter = 0;
    		}
    	}
    	
    	for (int i = 0; i < timesplits.size(); i++) {
    		timesplits.set(i, timesplits.get(i) * frame_size);
    	}
    	
    	return timesplits;
    }
}

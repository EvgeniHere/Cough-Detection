import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.apache.commons.math3.stat.descriptive.moment.Kurtosis;
import libsvm.svm_model;

public class Main {
	private static final int RECORD_TIME = 10000;
	private static final int SAMPLE_RATE = 44100;

	private static PCA pca = null;
	private static SVM svm = null;

	// MFCC Parameter
	private static final int WINDOW_SIZE = 512;
	private static final int num_mfcc_coef = 12;
	
	// Auf wieviele Dimensionen sollen die Features reduziert werden (bei der PCA)
	private static final int num_transform_dim = 5;

	private static int testing_loud_moments = 0;
	private static int detected_cough_moments = 0;

	/*
     * Darstellung der Amplituden mit Timesplits in einem JFrame
     */
	public static void showSoundwave(String title, double[] amplitudes, double sigma, ArrayList<Integer> frames_timesplits, ArrayList<Integer> frames_cough_timesplits) {
		Thread frameThread = new Thread(new Runnable() {
			public void run() {
				int[] arrPortions = new int[frames_timesplits.size()];
				for (int i = 0; i < arrPortions.length; i++) {
					arrPortions[i] = frames_timesplits.get(i);
				}

				int[] coughPortions = new int[frames_cough_timesplits.size()];
				for (int i = 0; i < coughPortions.length; i++) {
					coughPortions[i] = frames_cough_timesplits.get(i);
				}
				CoordinateFrame frame = new CoordinateFrame(title, amplitudes, sigma, arrPortions, coughPortions);
			}
		});
		frameThread.start();
	}

	public static double[] getAmplitudes(String file_path) {
		return  WavFile.getWavFile(file_path).getWavAmplitude();
	}

	/*
     * Gibt Amplituden-Arrays von lauten Momenten zurück
     */
	public static ArrayList<double[]> getHighAmplitudeFrames(double[] amplitudes) {
		ArrayList<Integer> loud_timesplits = Utility.getHighAmplitudeSplits(amplitudes);
		ArrayList<double[]> loud_frames_amps = new ArrayList<>();
		
		if (loud_timesplits == null || loud_timesplits.isEmpty())
			return new ArrayList<>();
		
		for (int split_num = 0; split_num < loud_timesplits.size() / 2; split_num++) {
			int von = loud_timesplits.get(split_num * 2);
			int bis = loud_timesplits.get(split_num * 2 + 1);
			double[] frame_amps = new double[bis - von];
			for (int i = 0; i < (bis - von); i++) {
				frame_amps[i] = amplitudes[i + von];
			}
			loud_frames_amps.add(frame_amps);
		}
		
		return loud_frames_amps;
	}

	/*
     * Gibt alle Features eines Frames zurück
     */
	public static ArrayList<double[]> getFrameFeatures(double[] frame_amps) {
		if (Utility.calculateMean(frame_amps) < 0.02)
			return new ArrayList<>();
		ArrayList<double[]> frame_windows_features = new ArrayList<>();
		for (int i = 0; i < frame_amps.length - WINDOW_SIZE; i += WINDOW_SIZE / 2) {
			double max_amp = 0;
			double[] range = new double[WINDOW_SIZE];
			for (int j = 0; j < range.length; j++) {
				range[j] = frame_amps[i + j];
				if (Math.abs(range[j]) > max_amp)
					max_amp = Math.abs(range[j]);
			}
			double kurtosis = new Kurtosis().evaluate(range);
			double zcr = Utility.calculateZeroCrossingRate(SAMPLE_RATE, range);
			double[] maxed_curve = Utility.maxOutAmps(range);
			double mean = Utility.calculateMean(maxed_curve);
			double[][] mfccs = Utility.generateMFCC(range, SAMPLE_RATE, WINDOW_SIZE, num_mfcc_coef);
			
			double[] features = Utility.createFeatureVector(mean / max_amp, kurtosis, zcr, mfccs[0]);
			frame_windows_features.add(features);
		}
		return frame_windows_features;
	}

	/*
     * Gibt alle Features aus allen lauten Momenten einer Audiodatei nicht-normalisiert zurück
     */
	public static ArrayList<double[]> getTrainData(String file_path) {
		double[] amplitudes = getAmplitudes(file_path);
		ArrayList<double[]> frames = getHighAmplitudeFrames(amplitudes);
		ArrayList<double[]> train_frames_data = new ArrayList<>();
		for (int i = 0; i < frames.size(); i++) {
			ArrayList<double[]> window_features = getFrameFeatures(frames.get(i));
			train_frames_data.addAll(window_features);
		}
		return train_frames_data;
	}

	/*
     * Gibt die normalisierten Features aus allen Windows, aus allen Frames einer Audiodatei, geordnet in sub-lists, zurück
     */
	public static ArrayList<ArrayList<double[]>> getFileCoughFeatures(String file_path) {
		double[] amplitudes = getAmplitudes(file_path);
		ArrayList<Integer> loud_timesplits = Utility.getHighAmplitudeSplits(amplitudes);
		ArrayList<Integer> cough_timesplits = new ArrayList<>();
		ArrayList<double[]> frames = getHighAmplitudeFrames(amplitudes);
		ArrayList<ArrayList<double[]>> cough_frames_windows_features = new ArrayList<>();
		for (int i = 0; i < frames.size(); i++) {
			ArrayList<double[]> frame_windows_features = getFrameFeatures(frames.get(i));
			boolean frame_has_cough = false;
			for (int j = 0; j < frame_windows_features.size(); j++) {
				Normalizer.minMaxNormalizeFeatures(frame_windows_features.get(j));
				Normalizer.meanNormalizeFeatures(frame_windows_features.get(j));
			}
			for (int j = 0; j < frame_windows_features.size(); j++) {
				double[][] svm_input = pca.transform(new double[][] {frame_windows_features.get(j)});
				double pred_res = SVM.predict(svm, svm_input[0]);
				if (pred_res > 0) {
					frame_has_cough = true;
					break;
				}
			}
			if (!frame_has_cough)
				continue;
			
			cough_timesplits.add(loud_timesplits.get(i * 2));
			cough_timesplits.add(loud_timesplits.get(i * 2 + 1));
			cough_frames_windows_features.add(frame_windows_features);
		}
		testing_loud_moments += frames.size();
		detected_cough_moments += cough_frames_windows_features.size();
		//if-Statement wenn viele Dateien getestet werden und man nur diejenigen mit Unterschieden zwischen lauten Momenten und Hustern sehen möchte
		//if (frames.size() != cough_frames_windows_features.size()) {
		//Visuals zur Datei (Empfehlung/Warnung: Auskommentieren bei Benutzung der Test-Funktion)
		//showSoundwave("Loud moments and detected coughs of file " + file_path, amplitudes, Utility.calculateStandardDeviation(amplitudes), loud_timesplits, cough_timesplits);
		//}
		return cough_frames_windows_features;
	}

	/*
     * Trainiert neue PCA und SVM Modelle anhand von Audiodateien eines Ordners und speichert die Modelle als Dateien ab
     */
	public static void trainModels() {
		File dir = new File("resampled/coughs_training/");
		File[] directoryListing = dir.listFiles();
		int index = 0;
		if (directoryListing == null)
			return;
		ArrayList<double[]> train_data_list = new ArrayList<>();
		
		for (File child : directoryListing) {
			//System.out.println("File " + index + " \"" + child.getName() + "\": ");
			ArrayList<double[]> train_file_data = getTrainData("resampled/coughs_training/" + child.getName());
			if (train_file_data != null)
				train_data_list.addAll(train_file_data);
			index++;
		}
		
		if (train_data_list.isEmpty()) {
			System.out.println("No training data gathered. Try again. :(");
			return;
		}
		
		double[][] train_data = new double[train_data_list.size()][train_data_list.get(0).length];
		for (int i = 0; i < train_data.length; i++) {
			train_data[i] = train_data_list.get(i);
		}
		
		System.out.println("Normalizing Training-Data...");
		Normalizer.minMaxNormalizeColumns(train_data);
		Normalizer.meanNormalizeColumns(train_data);
		
		System.out.println("Training PCA-Model...");
		pca = new PCA(train_data, num_transform_dim);
		double[][] pca_transform = pca.getPCATransformedDataAsDoubleArray();

		System.out.println("Training SVM-Model...");
		svm = SVM.buildModel(pca_transform);

		System.out.println("Storing Models as Files...");
		System.out.println(new File("models").mkdir());
		PCA.saveModel(pca);
		SVM.saveModel(svm);

		System.out.println("Training finished.");
	}

	/*
     * Testet die Genauigkeit der Modelle mit Audiodateien eines Ordners
     */
	public static void testModels() {
		if (pca == null || svm == null)
			loadModels();
		ArrayList<double[]> features = new ArrayList<>();
		File dir = new File("resampled/coughs_testing");
		File[] directoryListing = dir.listFiles();
		int index = -1;
		if (directoryListing == null)
			return;
		
		for (File child : directoryListing) {
			index++;
			/*if (index > 3)
				continue;*/
			System.out.println("File " + index + " \"" + child.getName() + "\": ");
			ArrayList<ArrayList<double[]>> file_frame_cough_features = getFileCoughFeatures("resampled/coughs_testing/" + child.getName());
			if (file_frame_cough_features == null || file_frame_cough_features.isEmpty()) {
				System.out.println("\tNo coughs found.");
				continue;
			}
		}
		
		System.out.println("Accuracy: " + detected_cough_moments + " / " + testing_loud_moments + " = " + detected_cough_moments / (double) testing_loud_moments);
	}

	/*
     * Lädt die Modelle aus den Modell-Dateien
     */
	public static void loadModels() {
		try {
			pca = PCA.readModel();
			svm = SVM.readModel();
		} catch (IOException e) {
			//e.printStackTrace();
		}
	}

	/*
	 * Ausgabe von Features eines auswählbaren Husters für eine bestimmte Audiodatei
	*/
	public static void detectCoughs(String file_name) {
		if (pca == null) {
			System.out.println("Reading PCA-Model...");
			try {
				pca = PCA.readModel();
			} catch (IOException e) {}
			if (pca == null) {
				System.out.println("No PCA-Model found.");
				System.out.println("Training new PCA- and SVM-Models...");
				trainModels();
				System.out.println("PCA- and SVM-Models created and parsed.");
			} else {
				System.out.println("PCA-Model parsed.");
			}
		}
		
		if (svm == null) {
			System.out.println("Reading SVM-Model...");
			try {
				svm = SVM.readModel();
			} catch (IOException e) {}
			System.out.println("SVM-Model parsed.");
		}
		
		System.out.println("Processing " + file_name + " ...");
		ArrayList<ArrayList<double[]>> cough_file_frames_windows_features = getFileCoughFeatures(file_name);
		
		if (cough_file_frames_windows_features == null || cough_file_frames_windows_features.isEmpty()) {
			System.out.println("No coughs detected. Try again. :)");
			return;
		}
		
		if (cough_file_frames_windows_features.size() == 1) {
			System.out.println("One cough detected: ");
			for (int i = 0; i < cough_file_frames_windows_features.get(0).size(); i++) {
				Utility.printArray(cough_file_frames_windows_features.get(0).get(i));
			}
			return;
		}
		
		System.out.println("Multiple coughs detected.");
		System.out.print("Print Features from cough number (0 - " + (cough_file_frames_windows_features.size() - 1) + "): ");
		Scanner sc = new Scanner(System.in);
		int split_num = sc.nextInt();
		for (int i = 0; i < cough_file_frames_windows_features.get(split_num).size(); i++) {
			Utility.printArray(cough_file_frames_windows_features.get(split_num).get(i));
		}
	}

	/*
	 * Main Methode kann Aufnahme starten, Audiodateien Resamplen auf 44.100 Hz, Modelle trainieren und testen, Huster aus bestimmten Dateien erkennen und Features dazu ausgeben
	*/
	public static void main(String[] args) {
		System.out.println("Starting CoughDetection-App...");
		
		final Recorder recorder = new Recorder();
		recorder.start("input.wav", RECORD_TIME, SAMPLE_RATE);
		
		
		// Code ab hier nicht zur normalen Ausführung sondern nur zum (gezielten) Testen gedacht
		
		// Resampled alle Dateien auf die SAMPLE_RATE in einem besimmten Ordner und verschiebt sie zu dem Ordner "resampled"
		//Resampler.resampleDir(SAMPLE_RATE);
		
		// Trainiert die Modelle anhand der Dateien im Trainings-Ordner
		//trainModels();
		
		// Testet die Modelle anhand der Dateien im Test-Ordner
		//testModels();
		// Die Accuracy der folgenden Zeile hat wenig Aussagekraft, da viele Audiodateien neben Huster auch Störgeräusche haben, die den Schnitt herunterziehen

		// Gezieltes Testen von Dateien
		//detectCoughs("input.wav");
		//detectCoughs("max.wav");
	}
}

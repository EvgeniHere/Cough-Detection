
public class Normalizer {
	public static double[] scale_mins;
	public static double[] scale_maxs;
	public static double[] scale_means;
	
	/*
	 * Normalisiert inplace, indem von jedem Wert des Arrays der Mittelwert abgezogen wird
	 */
	public static void meanNormalizeFeatures(double[] array) {
		for (int i = 0; i < array.length; i++) {
			array[i] -= scale_means[i];
		}
    }
    
	/*
	 * Normalisiert alle Werte des Arrays dem Wertebereich 0 bis 1 inplace, indem die Max- und Minwerte des gelernten Trainingssets genutzt werden
	 */
    public static void minMaxNormalizeFeatures(double[] array) {
    	for (int i = 0; i < array.length; i++) {
			array[i] = (array[i] - scale_mins[i]) / (scale_maxs[i] - scale_mins[i]);
		}
    }
    
    /*
     * Findet den Mittelwert jeder Spalte und zieht den Wert von jedem Spaltenelement ab
     */
    public static void meanNormalizeColumns(double[][] array) {
    	scale_means = new double[array[0].length];
		for (int j = 0; j < array[0].length; j++) {
			for (int i = 0; i < array.length; i++) {
				scale_means[j] += array[i][j];
			}
			scale_means[j] /= array.length;
		}
		for (int j = 0; j < array[0].length; j++) {
			for (int i = 0; i < array.length; i++) {
				array[i][j] -= scale_means[j];
			}
		}
    }

    /*
     * Berechnet Min- und Maxwerte jeder Spalte und bringt jedes Spaltenelement auf den Wertebereich 0 bis 1
     */
	public static void minMaxNormalizeColumns(double[][] array) {
		scale_mins = new double[array[0].length];
		scale_maxs = new double[array[0].length];
		for (int i = 0; i < array[0].length; i++) {
			scale_mins[i] = array[0][i];
			scale_maxs[i] = array[0][i];
		}
		for (int i = 0; i < array.length; i++) {
			for (int j = 0; j < array[i].length; j++) {
				if (scale_mins[j] > array[i][j]) {
					scale_mins[j] = array[i][j];
				}
				if (scale_maxs[j] < array[i][j]) {
					scale_maxs[j] = array[i][j];
				}
			}
		}
		for (int i = 0; i < array.length; i++) {
			for (int j = 0; j < array[i].length; j++) {
				array[i][j] = (array[i][j] - scale_mins[j]) / (scale_maxs[j] - scale_mins[j]);
			}
		}
	}
}

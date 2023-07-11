import java.io.IOException;

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_problem;

public class SVM extends svm_model {
	
	private svm_model model;
	
	public SVM(svm_model model) {
		this.model = model;
	}
	
	public svm_model getModel() {
		return model;
	}
	
	/*
	 * Liest das SVM-Modell aus der SVM-Modell-Datei
	 */
	public static SVM readModel() throws IOException {
		return new SVM(svm.svm_load_model("models/svm_model"));
	}
	
	/*
	 * Speichert das Modell als Datei
	 */
	public static void saveModel(SVM new_model) {
		try {
			svm.svm_save_model("models/svm_model", new_model.getModel());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * Klassifiziert eingegebene Features
	 * 
	 * Return ist > 0 wenn die Features dem Gelernten entspricht und < 0 wenn nicht
	 */
	public static double predict(SVM trained_model, double[] row_values) {
		svm_node[] nodes = buildPoint(row_values);
		return svm.svm_predict(trained_model.getModel(), nodes);
	}
	
	/*
	 * Erstellt einen n-Dimensionalen Punkt für das Modell
	 */
	public static svm_node[] buildPoint(double[] array) {
	    svm_node[] point = new svm_node[array.length];
	    
	    for (int i = 0; i < point.length; i++) {
	    	point[i] = new svm_node();
	    	point[i].index = i + 1;
	    	point[i].value = array[i];
	    }

	    return point;
	}
	
	/*
	 * Gibt ein SVM-Modell zurück, dass mit bestimmten Einstellungsparametern und den Trainingsdaten erstellt wird
	 */
	public static SVM buildModel(double[][] input) {
		svm_node[][] nodes = new svm_node[input.length][input[0].length];
		for (int i = 0; i < nodes.length; i++) {
			nodes[i] = buildPoint(input[i]);
		}
		
	    // Build Parameters
	    svm_parameter param = new svm_parameter();
	    param.svm_type    = svm_parameter.ONE_CLASS;
	    param.kernel_type = svm_parameter.RBF;
	    param.gamma       = 1 / (double)input[0].length;
	    param.nu          = 0.9; // Höhere Werte ergeben striktere Anforderungen an die Ähnlichkeit der Featurewerte der Huster (minimal 0, maximal 1)
	    param.cache_size  = 2000; // Arbeitsspeicher in Megabyte

	    // Build Problem
	    svm_problem problem = new svm_problem();
	    problem.x = nodes;
	    problem.l = nodes.length;
	    problem.y = prepareY(nodes.length);

	    // Build Model
	    return new SVM(svm.svm_train(problem, param));
	}

	/*
	 * Bereitet für das Modell Y vor. Der Wert des Y-Arrays zeigt keinen Zusammenhang zum Ergebnis auf.
	 */
	private static double[] prepareY(int size) {
	    double[] y = new double[size];

	    for (int i = 0; i < size; i++)
	        y[i] = 1;

	    return y;
	}
}

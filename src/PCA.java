import comirva.data.DataMatrix;

import java.io.*;
import java.util.ArrayList;
import java.util.StringTokenizer;

import Jama.*;


/**
 * This class implements the projection of a
 * data set with a Principal Components Analysis.
 * 
 * @author Markus Schedl, Peter Knees
 */
public class PCA {

	private double[] means;
	private double[] eigValues;
	private Matrix eigVecs;
	private Matrix pcaCompressed;
	private Matrix firstDimEVecs;
	
	/*
	 * Erzeugt neues PCA-Modell-Objekt anhand Parametern
	 */
	public PCA(double[] means, double[] mins, double[] maxs, double[][] transformArray) {
		Normalizer.scale_means = means;
		Normalizer.scale_mins = mins;
		Normalizer.scale_maxs = maxs;
		this.firstDimEVecs = new Matrix(transformArray);
	}
	
	/**
	 * Creates a new Principal Components Analysis (PCA) and calculates it using the
	 * Jama Matrix <code>m</code> as input. Furthermore, the projection of the input data to
	 * a space of dimensionality <code>dim</code> is performed.
	 * 
	 * @param m		a Matrix representing the input data
	 * @param dim		the number of dimensions onto which the input data is to be projected
	 */
	public PCA(Matrix m, int dim) {
		Matrix meanNormalizedData = m.transpose();
			
		// calc covariance matrix
		Matrix covMatrix = meanNormalizedData.times(meanNormalizedData.transpose());
		double[][] dimMinus1 = new double[covMatrix.getColumnDimension()][covMatrix.getColumnDimension()];
		for (int i=0; i<dimMinus1.length; i++)
			for (int j=0; j<dimMinus1.length; j++)
				dimMinus1[i][j] = covMatrix.getColumnDimension() - 1;
		covMatrix = covMatrix.arrayRightDivideEquals(new Matrix(dimMinus1));
		
		// Eigenvalue Decomposition
		EigenvalueDecomposition eigd = covMatrix.eig();
		eigVecs = eigd.getV();
		eigValues = eigd.getRealEigenvalues();

		// check requested dimensionality
		if (dim > eigVecs.getColumnDimension())
			dim = eigVecs.getColumnDimension();
		
		// extract those <code>dim</code> eigenvectors with highest eigenvalues
		firstDimEVecs = eigVecs.getMatrix(0, eigVecs.getRowDimension()-1, eigVecs.getColumnDimension()-dim, eigVecs.getColumnDimension()-1);
		
		// project data
		pcaCompressed = meanNormalizedData.transpose().times(firstDimEVecs);
	}
	
	/**
	 * Creates a new Principal Components Analysis (PCA) and calculates it using the
	 * data matrix <code>data</code> as input. Furthermore, the projection of the input data to
	 * a space of dimensionality <code>dim</code> is performed.
	 * 
	 * @param data		a double[][] representing the input data
	 * @param dim		the number of dimensions onto which the input data is to be projected
	 */
	public PCA(double[][] data, int dim) {
		this(new Matrix(data), dim);
	}
	
	/**
	 * Creates a new Principal Components Analysis (PCA) and calculates it using the
	 * data matrix <code>data</code> as input. Furthermore, the projection of the input data to
	 * a space of dimensionality <code>dim</code> is performed.
	 * 
	 * @param data		a DataMatrix representing the input data
	 * @param dim		the number of dimensions onto which the input data is to be projected
	 */
	public PCA(DataMatrix data, int dim) {
		this(new Matrix(data.toDoubleArray()), dim);
	}
	
	/*
	 * Transformiert Features zu reduzierter Dimensionalität ahnand des gelernten Modells
	 */
	public double[][] transform(double[][] array) {
		return new Matrix(array).times(firstDimEVecs).getArray();
	}
	
	public double[][] getPCATransformedDataAsDoubleArray() {
		return pcaCompressed.getArray();
	}
	public Matrix getPCATransformedDataAsMatrix() {
		return pcaCompressed;
	}
	public DataMatrix getPCATransformedDataAsDataMatrix() {
		DataMatrix dm = DataMatrix.jamaMatrixToDataMatrix(pcaCompressed);
		dm.setName("PCA-projection");
		return dm;
	}
	
	public double[][] getEigenvectorsAsDoubleArray() {
		return this.eigVecs.getArray();
	}
	public Matrix getEigenvectorsAsMatrix() {
		return this.eigVecs;
	}
	public DataMatrix getEigenvectorsAsDataMatrix() {
		DataMatrix dm = DataMatrix.jamaMatrixToDataMatrix(this.eigVecs);
		dm.setName("Eigenvectors for PCA");
		return dm;
	}
	
	public double[] getEigenvalues() {
		return this.eigValues;
	}
	public DataMatrix getEigenvaluesAsDataMatrix() {
		double[] temp = this.eigValues;
		DataMatrix dm = new DataMatrix("Eigenvalues for PCA");
		for (int i=0; i<temp.length; i++) {
			dm.addValue(new Double(temp[i]));
			dm.startNewRow();
		}
		dm.removeLastAddedElement();
		return dm;
	}
	
	public double[] getMeans() {
		return this.means;
	}
	public DataMatrix getMeansAsDataMatrix() {
		double[] temp = this.means;
		DataMatrix dm = new DataMatrix("Means for PCA");
		for (int i=0; i<temp.length; i++) {
			dm.addValue(new Double(temp[i]));
			dm.startNewRow();
		}
		dm.removeLastAddedElement();
		return dm;
	}
	
	/*
	 * Speichert das PCA-Modell als Datei ab
	 */
	public static void saveModel(PCA pca) {
	    String str = "";
	    for (int i = 0; i < Normalizer.scale_means.length; i++) {
	    	str += Normalizer.scale_means[i] + " ";
	    }
	    str += "\n";
	    for (int i = 0; i < Normalizer.scale_mins.length; i++) {
	    	str += Normalizer.scale_mins[i] + " ";
	    }
	    str += "\n";
	    for (int i = 0; i < Normalizer.scale_maxs.length; i++) {
	    	str += Normalizer.scale_maxs[i] + " ";
	    }
	    double[][] vecsArray = pca.firstDimEVecs.getArray();
	    for (int i = 0; i < vecsArray.length; i++) {
	    	str += "\n";
	    	for (int j = 0; j < vecsArray[i].length; j++) {
	    		str += vecsArray[i][j] + " ";
	    	}
	    }
	    
	    BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter("models/pca_model"));
		    writer.write(str);
	    } catch (IOException e) {
			e.printStackTrace();
		} finally {
		    try {
		    	if (writer != null)
		    		writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/*
	 * Liest das PCA-Modell aus der PCA-Modell-Datei (+ die Normalisierungsparameter! Sollte ausgelagert werden zum Normalizer)
	 */
	public static PCA readModel() throws IOException {
		double[] new_means = null;
		double[] new_mins = null;
		double[] new_maxs = null;
		double[][] new_transformArray = null;
		
		BufferedReader br = new BufferedReader(new FileReader("models/pca_model"));
	    StringBuilder sb = new StringBuilder();
	    
		String line = br.readLine();
	    ArrayList<Double> line_doubles = new ArrayList<>();
	    String[] str_values = line.trim().split(" ");
        for (int i = 0; i < str_values.length; i++) {
           line_doubles.add(Double.parseDouble(str_values[i]));
        }
        new_means = new double[line_doubles.size()];
        for (int i = 0; i < new_means.length; i++) {
        	new_means[i] = line_doubles.get(i);
        }
        
        line = br.readLine();
        line_doubles = new ArrayList<>();
	    str_values = line.trim().split(" ");
        for (int i = 0; i < str_values.length; i++) {
           line_doubles.add(Double.parseDouble(str_values[i]));
        }
        new_mins = new double[line_doubles.size()];
        for (int i = 0; i < new_means.length; i++) {
        	new_mins[i] = line_doubles.get(i);
        }
        
        line = br.readLine();
        line_doubles = new ArrayList<>();
	    str_values = line.trim().split(" ");
        for (int i = 0; i < str_values.length; i++) {
           line_doubles.add(Double.parseDouble(str_values[i]));
        }
        new_maxs = new double[line_doubles.size()];
        for (int i = 0; i < new_maxs.length; i++) {
        	new_maxs[i] = line_doubles.get(i);
        }
	    
        line = br.readLine();
		ArrayList<ArrayList<Double>> matrix = new ArrayList<>();
		while (line != null) {
		    line_doubles = new ArrayList<>();
		    str_values = line.trim().split(" ");
            for (int i = 0; i < str_values.length; i++) {
               line_doubles.add(Double.parseDouble(str_values[i]));
            }
            matrix.add(new ArrayList<>(line_doubles));
            line = br.readLine();
		}
		new_transformArray = new double[matrix.size()][line_doubles.size()];
		for (int i = 0; i < new_transformArray.length; i++) {
			for (int j = 0; j < new_transformArray[i].length; j++) {
				new_transformArray[i][j] = matrix.get(i).get(j);
			}
		}
		
		return new PCA(new_means, new_mins, new_maxs, new_transformArray);
	}
}

import java.util.Random;
public class Neuron{
	protected static Random random = new Random();
	
	protected double lastResult = 0.0;
	
	public double lastResult() {
		return lastResult;
	}
	
	protected static double sigmoid(double value){
	    if (value > 10) return 1.0;
	    if (value < -10) return 0.0;
		return 1 / (1 + Math.exp(-value));
	}
}
import java.util.Random;

import javax.swing.text.Utilities;

public class StationTypeNeuron extends Neuron {
	private double agentInputWeight = 0.05;
	private double prevTypeWeight = 0.6;
	
	private double wayCostWeight = -0.6;
	private double spaceWeight = 0.20;
	private double timeWeight = 0.05;
	
	private double randomWeight = 0.4;
	
	private StationType type;
	
	public StationTypeNeuron(StationType type) {
		super();
		this.type = type;
	}
	
	
	public double evaluate(double agentInput, double cost, double space, double time) {
		return evaluate(agentInput, 0.0, cost, space, time);
	}
	
	public double evaluate(double agentInput, double prevType, double cost, double space, double time) {
		if (space == -1) space = 5;
		if (time == -1) time = 0;
		double result = agentInput * agentInputWeight + prevType * prevTypeWeight + cost * wayCostWeight + space * spaceWeight + time * timeWeight + random.nextDouble() * randomWeight;
		lastResult = sigmoid(result);
		return lastResult;
	}
	
	public StationType getStationType() {
		return this.type;
	}
}
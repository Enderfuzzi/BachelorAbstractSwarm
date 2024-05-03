public class StationNeuron extends Neuron{
	private double typeInputWeight = 0.3;
	private double prevTypeInputWeight = 0.2;
	
	private double freqWeight = 0.1;
	private double necWeight = 0.1;
	private double spaceWeight = 0.1;
	
	private double stationWeight = -0.2;
	
	private double randomWeight = 0.50;
	
	private Station station;
	
	
	public StationNeuron(Station station) {
		super();
		this.station = station;
	}
	
	public double evaluate(double typeInput, double freq, double nec, double space, double station) {
		return evaluate(typeInput, 0.0, freq, nec, space, station);
	}
	
	public double evaluate(double typeInput, double prevTypeInput, double freq, double nec, double space, double station) {
		if (freq == -1) freq = 0;
		if (nec == -1) nec = 0;
		if (space == -1) space = 40;
		double result = typeInput * typeInputWeight + 
				prevTypeInput * prevTypeInputWeight + 
				freq * freqWeight + 
				nec * necWeight + 
				space * spaceWeight + 
				station * stationWeight;
		lastResult = sigmoid(result);
		return lastResult;	
	}
	
	public Station getStation() {
		return this.station;
	}
}
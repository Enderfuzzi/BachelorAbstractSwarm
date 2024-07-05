import java.util.Locale;
import java.util.Random;

public class Cell {
	private static Random generator = new Random();
	
	private double initialMutateFaktor = 0.3;
	private double mutateFaktor = initialMutateFaktor;
	
	
	private double weight;
	private double initialWeight;
	
	private String key;
	private boolean used;
	
	public enum Status {
		ENABLED,
		DISABLED;
	}
	
	private Status status;
	private Status bestRunStatus;
	
	//value between 0 and 1
	private double chanceForActivation;
	
	
	public Cell(String key, double weight, double chanceForActivation) {
		this.key = key;
		this.weight = weight;
		this.initialWeight = weight;
		this.chanceForActivation = chanceForActivation;
		this.used = false;
		this.status = Cell.Status.ENABLED;
		this.bestRunStatus = Cell.Status.ENABLED;
	}
	
	public double evaluate(double input) {
		if (status == Cell.Status.DISABLED) return 0.0;
		
		used = true;
		return weight * input;
	}
	

	public void mutate() {
		weight += weight * (generator.nextDouble(Math.abs(initialWeight) * mutateFaktor * 2) - Math.abs(initialWeight) * mutateFaktor);
	}

	public void reset() {
		this.weight = initialWeight;
	}
	
	public void save() {
		this.initialWeight = weight;
	}
	
	public String fileRepresentation() {
		return String.format(Locale.US,"%s:%f",key, weight);
	}

	public String getKey() {
		return key;
	}
	
	public boolean wasUsed() {	
		return used;
	}
	
	public void resetUseage() {
		used = false;
	}

	public double getBestWeight() {
		return initialWeight;
	}
	
	public double getCurrentWeight() {
		return weight;
	}
	
	public double getCurrentMutateFaktor() {
		return mutateFaktor;
	}
	
	public double getInitialMutateFaktor() {
		return initialMutateFaktor;
	}
	
	public void manipulateMutateFaktor(double faktor) {
		this.mutateFaktor += this.initialMutateFaktor * faktor;
	}
	
	public void resetMutateFaktor() {
		this.mutateFaktor = this.initialMutateFaktor;
	}
	

	public Status getStatus() {
		return this.status;
	}
	
	public void enableCell() {
		this.status = Cell.Status.ENABLED;
	}
	
	public void disableCell() {
		this.status = Cell.Status.DISABLED;
	}
	
	public boolean isEnabled() {
		return this.status == Cell.Status.ENABLED;
	}
	
	public void saveStatus() {
		this.bestRunStatus = this.status;
	}
	
	public boolean isBestStatusEnabled() {
		return this.bestRunStatus == Cell.Status.ENABLED;
	}
	
	public void computeCellActivity() {
		if (this.chanceForActivation != 0.0 && (this.chanceForActivation == 1.0 || generator.nextDouble() < this.chanceForActivation)) {
			this.enableCell();
		} else {
			this.disableCell();
		}
	}
	
	public double getChanceForActivation() {
		return this.chanceForActivation;
	}
	
	public void setChanceForActivation(double value) {
		if (value > 1.0) {
			this.chanceForActivation = 1.0;
		} else if(value < 0.0) {
			this.chanceForActivation = 0.0;
		} else {
			this.chanceForActivation = value;
		}
	}
	
	
	@Override
	public String toString() {
		return String.format(Locale.US,"[%s: %s Current weight: %f Best weight: %f | Mutate Faktor: %f | Status: %s | Chance for activation: %f]", 
				this.getClass().getSimpleName(), key, weight, initialWeight, mutateFaktor, status.name(), chanceForActivation);
	}
}
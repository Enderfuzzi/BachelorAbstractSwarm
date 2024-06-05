public record PlanEntry(Agent agent, Station station, Long predictionTime, 
		Long arrivalTime, Long finishTime, Double stationValue) implements Comparable<PlanEntry>{
	
	@Override
	public String toString() {
		return String.format("[PlanEntry] Agent: %s Station %s Prediction Time: %d Arrival Time: %d finish Time: %d Station value: %f",
				agent.name, station.name, predictionTime, arrivalTime, finishTime, stationValue);
	}

	@Override
	public int compareTo(PlanEntry other) {
		// TODO Auto-generated method stub
		int predictionTime = this.predictionTime.compareTo(other.predictionTime());
		if (predictionTime != 0) return predictionTime;
		int arrivalTime = this.arrivalTime.compareTo(other.arrivalTime());
		if (arrivalTime != 0) return arrivalTime;
		int finishTime =  this.finishTime.compareTo(other.finishTime());
		if (finishTime != 0) return finishTime;
		return this.agent.name.compareTo(other.agent().name);
	}
};
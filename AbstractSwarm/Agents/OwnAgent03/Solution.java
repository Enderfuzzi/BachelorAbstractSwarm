import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Solution {

	private HashMap<Agent, Prediction> prediction = new HashMap<>();
	
	private List<Agent> agentsAtStations = new ArrayList<>();
	
	private List<PlanEntry> plan = new ArrayList<>();
	
	
	public void addPrediction(Long time, Agent agent, Station station, Long arrivalTime, double stationValue) {
		Prediction newPrediction = new Prediction(station, time, arrivalTime);
		if (!prediction.containsKey(agent)) {
			prediction.put(agent, newPrediction);
		} else {
			if (!prediction.get(agent).equals(newPrediction)) {
				prediction.put(agent, newPrediction);
			}
		}
		
		if (prediction.get(agent).predictionTime() == prediction.get(agent).arrivalTime()) {
			agentsAtStations.add(agent);
		}
	}
	
	
	public void addReward(Long time, Agent agent) {
		if (agentsAtStations.contains(agent)) {
			Prediction currentPrediction = prediction.remove(agent);
			if (currentPrediction == null) return;
			plan.add(new PlanEntry(agent, currentPrediction.station(), 
					currentPrediction.predictionTime(), currentPrediction.arrivalTime(), time));
			agentsAtStations.remove(agent);
		}
	}
	
	public void clear() {
		prediction.clear();
		agentsAtStations.clear();
		plan.clear();
	}
	
	
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		for (PlanEntry entry : plan) {
			result.append(entry);
			result.append("\n");
		}
		return result.toString();
	}
}

record Prediction(Station station, Long predictionTime, Long arrivalTime) {
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Prediction prediction)) return false;
		if (this.station != prediction.station) return false;
		if (this.arrivalTime != prediction.arrivalTime) return false;
		return true;
	}
};

record PlanEntry(Agent agent, Station station, Long predictionTime, 
		Long arrivalTime, Long finishTime ) {
	
	@Override
	public String toString() {
		return String.format("[PlanEntry] Agent: %s Station %s Prediction Time: %d Arrival Time: %d finish Time: %d",
				agent.name, station.name, predictionTime, arrivalTime, finishTime);
	}
};


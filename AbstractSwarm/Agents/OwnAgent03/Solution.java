import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Solution {

	// list when a station gets targeted
	private HashMap<Long, List<Tuple>> target = new HashMap<>();
	
	private List<Tuple> usedTuples = new ArrayList<>();
	
	//List of agents which are finished with their task per time
	private HashMap<Agent, List<Long>> finish = new HashMap<>();
	
	
	
	
	
	public void addTarget(Long time, Agent agent, Station station, Long arrivalTime, double stationValue) {
		if (!target.containsKey(time)) {
			target.put(time, new ArrayList<>());
		}
		Tuple newTuple = new Tuple(agent, station, arrivalTime, stationValue);
		if (time - 1L > 0L) {
			if (usedTuples.contains(newTuple)) return;
		}
		usedTuples.add(newTuple);
		target.get(time).add(newTuple);
	}
	
	
	public void addFinish(Long time, Agent agent) {
		if (!finish.containsKey(agent)) {
			finish.put(agent, new ArrayList<>());
		}
		finish.get(agent).add(time);
	}
	
	public void clear() {
		target.clear();
		finish.clear();
	}
	
	
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		HashMap<Agent, Integer> index = new HashMap<>();
		
		for (Map.Entry<Long, List<Tuple>> entry : target.entrySet()) {
			if (entry.getValue().size() > 0) {
				for (Tuple tuple : entry.getValue()) {
					
					
					if (!finish.containsKey(tuple.agent())) continue;
					
					if (!index.containsKey(tuple.agent())) index.put(tuple.agent(),0);
					
					if (index.get(tuple.agent()) >= finish.get(tuple.agent()).size()) continue;
					
					
					result.append(
							String.format("[Plan] Time: %d Agent %s Station: %s Arrival Time %d Finish Time: %d Station Value: %f\n", 
									entry.getKey(), tuple.agent().name, tuple.station().name, tuple.arrivalTime(),
									finish.get(tuple.agent()).get(index.get(tuple.agent())), tuple.stationValue())
							);
					index.merge(tuple.agent(), 1, Integer::sum);
				}
				index.clear();
				result.append("-----------------------------------\n");
			}
		}
		return result.toString();
	}
	
	
}

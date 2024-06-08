import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FinishedSolution {

	private List<PlanEntry> solution;
	
	private List<PlanEntry> copy;
	
	private HashMap<Agent, Agent> mapping; 
	
	private HashMap<Long, List<Agent>> used;
	
	public FinishedSolution(List<PlanEntry> solution) {
		this.solution = new ArrayList<>(solution);
		this.copy = new ArrayList<>(solution);
		this.mapping = new HashMap<>();
		this.used = new HashMap<>();
		Collections.sort(this.solution);
		Collections.sort(this.copy);
	}
	
	public List<PlanEntry> getSolution() {
		return this.solution;
	}
	
	
	public double createStationValue(Agent me, long time, Station station) {
		/*
		for (Map.Entry<Agent, Agent> entry : mapping.entrySet() ) {
			System.out.println(entry.getKey().name + " -> " + entry.getValue().name);
		}
		*/
		for (int i = 0; i < solution.size(); i++) {
			PlanEntry entry = solution.get(i);
			if (time < entry.predictionTime()) continue;
			if (me.type != entry.agent().type) continue;
			if (!station.name.equals(entry.station().name)) continue;	
			if (!mapping.containsKey(entry.agent())) {
				mapping.put(entry.agent(), me);
			} else {
				if (mapping.get(entry.agent()) != me) continue;
			}
			
			if (!used.containsKey(time)) {
				used.put(time, new ArrayList<>());
			}
			
			if (used.get(time).contains(me)) continue;
			List<Agent> tmp = used.get(time);
			tmp.add(me);
			used.put(time, tmp);
			//System.out.println(String.format("Time: %d Agent %s Station %s Index i: %d",time, me.name, station.name, 
			//		i));
			solution.remove(i);
			return solution.size() + 1;
		}
		return 0.0;
	}
	
	public void reset() {
		this.solution = new ArrayList<>(copy);
		mapping.clear();
		used.clear();
	}
	
	public Long calculateTWT() {
		HashMap<Agent, Long> lastFinishPerAgent = new HashMap<>();
		Long result = 0L;
		Long maxTime = 0L;
		for (PlanEntry entry : copy) {
			if (maxTime < entry.finishTime()) maxTime = entry.finishTime();
			if (lastFinishPerAgent.containsKey(entry.agent())) {
				result += entry.arrivalTime() - (lastFinishPerAgent.get(entry.agent()) + 1);
				lastFinishPerAgent.put(entry.agent(), entry.finishTime());
			} else {
				lastFinishPerAgent.put(entry.agent(), entry.finishTime());
				// if arrival time = 1 waiting time = 0
				result += entry.arrivalTime() - 1L;
			}
		}
		
		for (Long value : lastFinishPerAgent.values()) {
			result += maxTime - value;
		}
		
		return result;
	}
	
	
	public String predictedTimePlanning() {
		HashMap<Agent, String> result = new HashMap<>();
		for (PlanEntry entry : copy) {
			if (!result.containsKey(entry.agent())) {
				result.put(entry.agent(), entry.agent().name + ": ");
			}
			result.put(entry.agent(), result.get(entry.agent()) 
					+ String.format("[%d,%d]",entry.arrivalTime(), entry.finishTime()));
		}
		StringBuilder builder = new StringBuilder();
		for (Map.Entry<Agent, String> entry: result.entrySet()) {
			builder.append(entry.getValue());
			builder.append("\n");
		}
		builder.append("Total Estimated waiting Time: ");
		builder.append(calculateTWT());
		builder.append("\n");
		return builder.toString();
	}
	
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		
		for (PlanEntry entry : copy) {
			result.append(entry);
			result.append("\n");
		}
		return result.toString();
	}
	
}

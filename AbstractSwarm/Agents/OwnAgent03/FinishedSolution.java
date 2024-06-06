import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FinishedSolution {

	private List<PlanEntry> solution;
	
	
	public FinishedSolution(List<PlanEntry> solution) {
		this.solution = new ArrayList<>(solution);
		Collections.sort(solution);
	}
	
	public List<PlanEntry> getSolution() {
		return this.solution;
	}
	
	
	public double createStationValue(Agent me, long time, Station station) {
		for (int i = 0; i < solution.size(); i++) {
			PlanEntry entry = solution.get(i);
			//System.out.println(entry);
			if (time != entry.predictionTime()) continue;
			if (!me.name.equals(entry.agent().name)) continue;
			if (!station.name.equals(entry.station().name)) continue;
			System.out.println(String.format("Time: %d Agent %s Station %s Index i: %d",time, me.name, station.name, solution.size() - i));
			return solution.size() - i; 
		}
		//System.out.println(String.format("Time: %d Agent %s Station %s Index i: %d",time, me.name, station.name, 0));
		return 0;
	}
	
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		
		for (PlanEntry entry : solution) {
			result.append(entry);
			result.append("\n");
		}
		return result.toString();
	}
	
	
	
	public String predictedTimePlanning() {
		HashMap<Agent, String> result = new HashMap<>();
		for (PlanEntry entry : solution) {
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
		return builder.toString();
	}
}

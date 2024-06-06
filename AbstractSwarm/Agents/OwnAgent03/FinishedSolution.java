import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FinishedSolution {

	private List<PlanEntry> solution;
	
	
	public FinishedSolution(List<PlanEntry> solution) {
		this.solution = new ArrayList<>(solution);
		Collections.sort(this.solution);
		System.out.println("Finished Solution");
		System.out.println(this);
	}
	
	public List<PlanEntry> getSolution() {
		return this.solution;
	}
	
	
	public double createStationValue(Agent me, long time, Station station) {
		for (int i = 0; i< solution.size(); i++) {
			PlanEntry entry = solution.get(i);
			//System.out.println(entry);
			//if (time < entry.predictionTime()) break;
			if (time != entry.predictionTime()) continue;
			if (!me.name.equals(entry.agent().name)) continue;
			if (!station.name.equals(entry.station().name)) continue;
			//System.out.println(String.format("Time: %d Agent %s Station %s Index i: %d",time, me.name, station.name, i + 1));
			System.out.println(String.format("Time: %d Agent %s Station %s Index i: %d",time, me.name, station.name, 
					i));
			return solution.size() - i + 1 / (double) entry.arrivalTime();
		}
		//System.out.println(String.format("Time: %d Agent %s Station %s Index i: %d",time, me.name, station.name, 0));
		return 0.0;
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
	
}

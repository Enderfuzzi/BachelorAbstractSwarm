import java.util.ArrayList;
import java.util.List;

public class FinishedSolution {

	private List<PlanEntry> solution;
	
	
	public FinishedSolution(List<PlanEntry> solution) {
		this.solution = new ArrayList<>(solution);
	}
	
	public List<PlanEntry> getSolution() {
		return this.solution;
	}
	
	
	public double createStationValue(Agent me, long time, Station station) {
		for (int i = 0; i< solution.size(); i++) {
			PlanEntry entry = solution.get(i);
			//System.out.println(entry);
			if (time < entry.predictionTime()) break;
			if (time != entry.predictionTime()) continue;
			if (me != entry.agent()) continue;
			if (station != entry.station()) continue;
			//System.out.println(String.format("Time: %d Agent %s Station %s Index i: %d",time, me.name, station.name, i + 1));
			return i + 1;
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

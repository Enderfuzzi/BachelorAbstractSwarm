import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;

public class GreedyCalculations {

	/** The random generator used for random decisions. */
	private static Random random = new Random();
	
	private static double prob = 0.8;
	
	
	private static HashMap<Station, Double> stationVisit = new HashMap<>();
	
	public static double evaluate(Agent me, HashMap<Agent, Object> others, List<Station> stations, long time, Station station,
			TimeStatistics timeStatistic) {

		double result = 0.0;
		
		if (!otherStationsReachable(me, station)) {
			return -100;
		}
			
		if (random.nextDouble() > 0.7) {
			if (me.previousTarget.type == station.type) {
				result += 0.5;
			}
		}
			result -= pathCost(me.previousTarget.type, station.type);
			
			
			stationVisit.clear();
			valueOfStation(me, others, station);
			if (stationVisit.containsKey(station)) {
				result += stationVisit.get(station);
			}
			result += spaceEfficiency(me, station.type);
			
			result += estimatedWorkTimeLeft(me) * time / timeStatistic.lowestTimeUnit;
	
		return result;
		
	}
	
	public static void communication(Agent me, HashMap<Agent, Object> others, List<Station> stations, 
			long time, Object[] defaultData, TimeStatistics timeStatistic){
	}
	
	
	public static void reward(Agent me, HashMap<Agent, Object> others, List<Station> stations, long time, double value,
			TimeStatistics timeStatistic
			) {
		
		
	}
	
	
	private static boolean otherStationsReachable(Agent me, Station station) {
		for (Map.Entry<Station, Integer> entry : me.necessities.entrySet()) {
			if (entry.getValue() <= 0) continue;
			if (pathCost(station.type, entry.getKey().type) == -1) return false;
		}
		return true;
	}
	
	
	private static List<Station> getUndirectedTimeConnectedStations(Station station) {
		List<Station> result = new ArrayList<>();
		for (TimeEdge edge : station.type.timeEdges) {
			//System.out.println(String.format("Time Edge: Station: %s ConnectedType: %s Incoming: %b Outgoing: %b", station.name, edge.connectedType.name, edge.incoming, edge.outgoing));
			if (edge.incoming || edge.outgoing) continue;
			if (edge.connectedType instanceof StationType stationType) {
					result.addAll(stationType.components);
			}
		}
		return result;
	}
	
	record Pair(StationType station, Integer cost) implements Comparable<Pair> {
		@Override
		public int compareTo(Pair other) {
			return Integer.compare(this.cost, other.cost);
		}	
	};
	
	private static int pathCost(StationType start, StationType target) {
		PriorityQueue<Pair> queue = new PriorityQueue<>();
		List<StationType> used = new ArrayList<>();
		queue.add(new Pair(start, 0));
		while (!queue.isEmpty()) {
			Pair current = queue.poll();
			if (used.contains(current.station())) continue;
			used.add(current.station());
			if (current.station() == target) {
				//System.out.println(String.format("Path calculation: %s to %s with cost: %d", start.name, target.name, current.cost()));
				return current.cost();
			}
			
			for (PlaceEdge edge : current.station().placeEdges) {
				if (edge.incoming) continue;
				queue.add(new Pair((StationType) edge.connectedType, current.cost() + edge.weight));
			}
			
		}
		//System.out.println(String.format("Path calculation: %s to %s with cost: %d", start.name, target.name, -1));
		// -> station is not reachable
		return -1;
	}
	
	private static int timeAtStation(Agent me, StationType stationType) {
		if (me.time == -1 && stationType.time == -1) return 0;
		if (me.time == -1) return stationType.time;
		if (stationType.time == -1) return me.time;
		return Math.min(me.time, stationType.time);
	}
	
	private static int necessaryVisits(Agent me, Station station) {
		if (me.frequency == -1) return me.frequency;
		if (station.frequency != -1) return station.frequency;
		if (me.necessities.containsKey(station)) {
			return me.necessities.get(station);
		}
		if (station.necessities.containsKey(me)) {
			return station.necessities.get(me);
		}
		return 0;
	}
	
	private static double spaceEfficiency(Agent me, StationType stationType) {
		if (stationType.space == -1) return 0;
		if (me.type.size == -1) return stationType.space;
		return 5 / ((stationType.space % me.type.size) + 1) ;
	}
	
	private static int estimatedWorkTimeLeft(Agent me) {
		int result = 0;
		for (Map.Entry<Station, Integer> entry : me.necessities.entrySet()) {
			if (entry.getValue() == 0) continue;
			result += timeAtStation(me, entry.getKey().type);
		}
		return result;
	}
	
	
	private static void valueOfStation(Agent me, HashMap<Agent, Object> others, Station station) {
		for (Map.Entry<Agent, Object> entry : others.entrySet()) {
			//case agent visits Station
			if (entry.getValue() == null) continue;
			Object[] communication = (Object[]) entry.getValue();
			
			
			
			if (entry.getKey().visiting) {				
				if (entry.getKey().time != -1 && entry.getKey().time <= 1) {
					stationVisit.merge(entry.getKey().target, 2.0, Double::sum);
				} else {
					stationVisit.merge(entry.getKey().target, 0.5, Double::sum);
				}
			} else {
				if (getUndirectedTimeConnectedStations(station).contains((Station) communication[0])) {
					stationVisit.merge(station, 3.5, Double::sum);
				} else {
					stationVisit.merge((Station) communication[0], -0.5, Double::sum);
				}
				
			}
		}
	}
	
	
}

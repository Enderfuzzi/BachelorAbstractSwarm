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
			
		//if (random.nextDouble() > 0.7) {
			if (me.previousTarget.type == station.type) {
				// if a station has a frequency then dont distribute the agents
				if (station.frequency != -1) {
					boolean otherTarget = false;
					for (Object object : others.values()) {
						if (object == null) continue;
						Object[] communication = (Object[]) object;
						if ((Station) communication[0] == station) {
							otherTarget = true;
							break;
						}
					}
					if (otherTarget) {
						result -= station.frequency;
					} else {
						result += station.frequency * 10 * 1 / timeAtStation(me, station.type) ;
					}
					
					
				} else {
					result += 0.5;
				}
			}
		//}
			// if an agent has a frequency the as much distribution as possible
			if (me.frequency != -1) {
				double counter = 0;
				for (Object object : others.values()) {
					if (object == null) continue;
					Object[] communication = (Object[]) object;
					if ((Station) communication[0] == station) {
						counter++;
					}
				}
				//TODO fix distribution if agents doesn't fit in station
				//Fill station as much as possible and then distribute
				if (me.type.size == -1 && station.type.space > counter) {
					result += 1.0;
				} else if (station.type.space > counter * me.type.size){
					result += 1.0;
				} else {
					result += 1 / timeAtStation(me, station.type) + 1 / counter;
				}
				/*
				System.out.println("Counter: " + counter);
				if (station.space > 0) {
					counter *= spaceEfficiency(me, station.type);
					//result += station.space * 1 / (double) timeAtStation(me, station.type);
				}
				System.out.println("Manipulated Counter: " + counter);
				if (counter > 0) {
					result -= counter;
				}
				*/
				
			}
			
			List<ResultPair> outgoingTimeConnectedStations = getOutgoingTimeConnectedStations(station);
			for (ResultPair pair : outgoingTimeConnectedStations) {
				result += timeAtStation(pair.station.type);
				result -= 1 / (double) pair.cost; 
			}
			
			List<ResultPair> incomingTimeConnectedStations = getIncomingTimeConnectedStations(station);
			for (ResultPair pair : incomingTimeConnectedStations) {
				result += 1 / (double) pair.cost + timeAtStation(pair.station.type); 

			}
			
			for (Object object : others.values()) {
				if (object == null) continue;
				Object[] communication = (Object[]) object;
				if ((Station) communication[0] == station) {
					result += 1.0;
					break;
				}
			}
			
			result -= pathCost(me.previousTarget.type, station.type);
			
			
			stationVisit.clear();
			valueOfStation(me, others, station);
			if (stationVisit.containsKey(station)) {
				result += stationVisit.get(station);
			}
			result += spaceEfficiency(me, station.type);
			
			result += timeAtStation(me, station.type) * 4 * time / (double) estimatedWorkTimeLeft(me);
			
			result += time / (double) estimatedWorkTimeLeft(me);
	
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
	
	record ResultPair(Station station, int cost) {};
	
	private static List<ResultPair> getOutgoingTimeConnectedStations(Station station) {
		List<ResultPair> result = new ArrayList<>();
		for (TimeEdge edge : station.type.timeEdges) {
			if (!edge.outgoing || edge.incoming) continue;
			if (edge.connectedType instanceof StationType stationType) {
				for (Station s : stationType.components) {
					result.add(new ResultPair(s, edge.weight));
				}
			}
		}
		return result;
	}
	
	private static List<ResultPair> getIncomingTimeConnectedStations(Station station) {
		List<ResultPair> result = new ArrayList<>();
		for (TimeEdge edge : station.type.timeEdges) {
			if (edge.outgoing || !edge.incoming) continue;
			if (edge.connectedType instanceof StationType stationType) {
				for (Station s : stationType.components) {
					result.add(new ResultPair(s, edge.weight));
				}
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
		if (me.time == -1 && stationType.time == -1) return 1;
		if (me.time == -1) return stationType.time;
		if (stationType.time == -1) return me.time;
		return Math.min(me.time, stationType.time);
	}
	
	private static int timeAtStation(StationType stationType) {
		int result = 0;
		for (VisitEdge edge : stationType.visitEdges) {
			int time = timeAtStation(((AgentType) edge.connectedType).components.get(0), stationType);
			if (result == 0 || result > time) {
				result = time;
			}
		}
		return result == 0 ? 1 : result;
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
		return 10 / ((stationType.space % me.type.size) + 1) ;
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
	
	
	//TODO Directed Time Edges
	
}

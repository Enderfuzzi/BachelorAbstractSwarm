import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;

public class ParameterCalculations {
	private static Random random = new Random();
	
	
	private static boolean firstCall = true;
	
	private static HashMap<Parameter, Cell> cells = new HashMap<>();
	
	public static double evaluate(Agent me, HashMap<Agent, Object> others, List<Station> stations, long time, Station station,
			TimeStatistics timeStatistic
			) {
		
		if (firstCall) {
			firstCall = false;
			cells = InputOutput.loadCells();
			
			if (!graphHasTimeEdges(stations)) {
				cells.get(Parameter.STATION_IS_TIME_CONNECTED).disableCell();
				cells.get(Parameter.STATION_IS_TIME_CONNECTED).setChanceForActivation(0.0);
			}
			
			if (!graphHasBoldVisitEdge(stations)) {
				cells.get(Parameter.BOLD_VISIT_EDGE).disableCell();
				cells.get(Parameter.BOLD_VISIT_EDGE).setChanceForActivation(0.0);
			}
		}
		
		if (timeStatistic.newRun) {
			if (timeStatistic.newBestRun) {
				for (Cell cell : cells.values()) {
					if (cell.wasUsed()) {
						cell.save();	
					}
					cell.saveStatus();
					cell.resetMutateFaktor();
						
					if (cell.isEnabled() && cell.wasUsed()) {
						System.out.println("Increase: " + cell.toString());
						if (cell.getChanceForActivation() <= 0.3) {
							cell.setChanceForActivation(cell.getChanceForActivation() * 2);
						} else {
							cell.setChanceForActivation(cell.getChanceForActivation() + cell.getChanceForActivation() * 0.3);
						}	
					} else {
						System.out.println("Decrease: " + cell.toString());
						if (cell.getChanceForActivation() >= 0.7) {
							cell.setChanceForActivation(cell.getChanceForActivation() / 2);
						} else {
							cell.setChanceForActivation(cell.getChanceForActivation() - cell.getChanceForActivation() * 0.3);
						}
					}
					
					InputOutput.saveCells(cells.values(), timeStatistic);
					
				}
				
				
				
				if (timeStatistic.runsSinceCurrentBest != 0L && timeStatistic.runsSinceCurrentBest % 5 == 0) {
					for (Cell cell : cells.values()) {
						cell.manipulateMutateFaktor(0.1);
					}
				}
			}
				
				InputOutput.printStatus(cells, timeStatistic);
				
				for (Cell cell : cells.values()) {
					cell.computeCellActivity();
					cell.reset();
					if (!cell.wasUsed()) continue;
					if (!cell.isEnabled()) continue;
					cell.mutate();
					cell.resetUseage();
				}	
		}
		
		
		double result = 0.0;
		
		result += cells.get(Parameter.RANDOM).evaluate(random.nextDouble());
		
		//Agent
		
		if (me.frequency != -1 && cells.get(Parameter.AGENT_FREQUENCY).isEnabled()) {
			result += cells.get(Parameter.AGENT_FREQUENCY).evaluate(me.frequency);
		}
		
		if (cells.get(Parameter.AGENT_NECESSITY).isEnabled()) {
			result += cells.get(Parameter.AGENT_NECESSITY).evaluate(me.necessities.getOrDefault(station, 0));
		}
		
		if (me.time != -1 && cells.get(Parameter.AGENT_TIME).isEnabled()) {
			result += cells.get(Parameter.AGENT_TIME).evaluate(me.time);
		}
		
		
		// Agent type
		
		if (station.space > 0 && cells.get(Parameter.AGENT_TYPE_SIZE_WITH_STATION).isEnabled()) {
			result += cells.get(Parameter.AGENT_TYPE_SIZE_WITH_STATION).evaluate(me.type.size % station.space);
		}
		
		if (cells.get(Parameter.AGENT_TYPE_SIZE).isEnabled()) {
			result += cells.get(Parameter.AGENT_TYPE_SIZE).evaluate(me.type.size);	
		}
		
		// other agent
		
		if (cells.get(Parameter.OTHER_AGENT_NECESSITY).isEnabled()) {
			int sum = 0;
			for (Integer value : me.necessities.values()) sum += value;
			result += cells.get(Parameter.OTHER_AGENT_NECESSITY).evaluate(sum - me.necessities.getOrDefault(station, 0));
		}
		
		// Station
		
		if (station.space != -1 && cells.get(Parameter.STATION_SPACE).isEnabled()) {
			result += cells.get(Parameter.STATION_SPACE).evaluate(station.space);
		}
		
		if (station.frequency != -1 && cells.get(Parameter.STATION_FREQUENCY).isEnabled()) {
			result += cells.get(Parameter.STATION_FREQUENCY).evaluate(station.frequency);
		}
		
		if (cells.get(Parameter.STATION_NECESSITY).isEnabled()) {
			result += cells.get(Parameter.STATION_NECESSITY).evaluate(station.necessities.getOrDefault(me, 0));
		}
		
		if (time != 1L && cells.get(Parameter.STATION_IS_SAME).isEnabled()) {
			result += cells.get(Parameter.STATION_IS_SAME).evaluate(4);
		}
		// Station type
		
		if (station.type.time != -1 && cells.get(Parameter.STATION_TYPE_TIME).isEnabled()) {
			result += cells.get(Parameter.STATION_TYPE_TIME).evaluate(station.type.time);
		}
		
		// Other station
		
		if (cells.get(Parameter.OTHER_STATION_NECESSITY).isEnabled()) {
			int sum = 0;
			for (Integer value : station.necessities.values()) sum += value;
			result += cells.get(Parameter.OTHER_STATION_NECESSITY).evaluate(sum - station.necessities.getOrDefault(me, 0));
		}
		

		if (cells.get(Parameter.AGENT_DISTANCE_TO_STATION).isEnabled()) {
			int pathCost = Integer.MAX_VALUE;
			pathCost = pathCost(me.previousTarget.type, station.type);
			if (pathCost != -1) {
				result += cells.get(Parameter.AGENT_DISTANCE_TO_STATION).evaluate(pathCost);
			}
		}
		
		
		if (timeStatistic.numberOfCompletedRuns >= 30) {
			List<Object[]> filteredCommunications = getCommunicationOfStation(others, station);
			if (filteredCommunications.size() > 0) {
				Object[] highestStationCommunication = filteredCommunications.get(0);
				for (Object[] communication : filteredCommunications) {
					if (((Double) communication[2]) > ((Double) highestStationCommunication[2])) {
						highestStationCommunication = communication;
					}
				}
				
				result += cells.get(Parameter.OTHER_AGENT_SELECTED_STATION).evaluate(filteredCommunications.size());

				if (me.previousTarget != null && (long) highestStationCommunication[1] > (long) (time + pathCost(me.previousTarget.type, station.type))) {
					result += cells.get(Parameter.OTHER_AGENT_VALUE_OF_STATION).evaluate((Double) highestStationCommunication[2]);
				}
				
				
				
				
			}
		}
		
		
		if (cells.get(Parameter.STATION_IS_NEAREST).isEnabled() && isNearestStation(me, station)) {
			result += cells.get(Parameter.STATION_IS_NEAREST).evaluate(0.5);
		}
		
		if (cells.get(Parameter.STATION_IS_TIME_CONNECTED).isEnabled()) {
			
			double max_value = 0.0;
			
			for (Station timeConnectedStations : getUndirectedTimeConnectedStations(station)) {
				List<Object[]> filteredCommunications = getCommunicationOfStation(others, timeConnectedStations);
				if (filteredCommunications.size() > 0) {
					Object[] highestStationCommunication = filteredCommunications.get(0);
					for (Object[] communication : filteredCommunications) {
						if (max_value == 0.0 || max_value < Math.abs((double) communication[2])) {
							max_value = (double) communication[2];
						}
					}
				}
			}
			//System.out.println(String.format("Time: %d Agent: %s Station: %s Max Value: %f", time, me.name, station.name, max_value));
			result += cells.get(Parameter.STATION_IS_TIME_CONNECTED).evaluate(max_value);
			

		}
		
		if (cells.get(Parameter.BOLD_VISIT_EDGE).isEnabled()) {
			if (isBoldEdge(me, station)) {
				result += cells.get(Parameter.BOLD_VISIT_EDGE).evaluate(2);
			}
		}
		

		if (cells.get(Parameter.STATION_IS_NEIGHBOUR).isEnabled() && isNeighbourStation(me, station)) {
			result += cells.get(Parameter.STATION_IS_NEIGHBOUR).evaluate(0.5);
		}
		

		
		if (cells.get(Parameter.AGENT_WORK_TIME_LEFT).isEnabled()) {
			result += cells.get(Parameter.AGENT_WORK_TIME_LEFT).evaluate(estimatedWorkTimeLeft(me));
		}
	
		if (cells.get(Parameter.AGENT_DISTRIBUTION).isEnabled()) {
			result += cells.get(Parameter.AGENT_DISTRIBUTION).evaluate(agentDistribution(me, others, time, station));
		}
		
		
		if (cells.get(Parameter.STATION_TYPE_IS_SAME).isEnabled()) {
			if (time == 1L && me.previousTarget.type == station.type) {
				result += cells.get(Parameter.STATION_TYPE_IS_SAME).evaluate(1);
			}
		}
		
		if (cells.get(Parameter.OTHER_STATIONS_REACHABLE).isEnabled() && otherStationsReachable(me, station)) {
			result += cells.get(Parameter.OTHER_STATIONS_REACHABLE).evaluate(1);
		}
		
		
		if (cells.get(Parameter.STATION_DIRECTED_TIME_TARGET_TIME).isEnabled() || cells.get(Parameter.STATION_DIRECTED_TIME_TARGET).isEnabled()) {
			double max_value = 0.0;
			for (ResultPair pair : getOutgoingTimeConnectedStations(station)) {
				if (cells.get(Parameter.STATION_DIRECTED_TIME_WEIGHT).isEnabled())  {
					result += cells.get(Parameter.STATION_DIRECTED_TIME_WEIGHT).evaluate(pair.cost);
				}
				
				if (cells.get(Parameter.STATION_DIRECTED_TIME_TARGET_TIME).isEnabled()) {
					result += cells.get(Parameter.STATION_DIRECTED_TIME_TARGET_TIME).evaluate(estimatedStationTime(pair.station));
				}
				
				if (cells.get(Parameter.STATION_DIRECTED_TIME_TARGET).isEnabled()) {
					List<Object[]> filteredCommunications = getCommunicationOfStation(others, pair.station);
					for (Object[] communication : filteredCommunications) {
						if (max_value == 0.0 || max_value < Math.abs((double) communication[2])) {
							max_value = Math.abs((double) communication[2]);
						}
					}
				}
			}
			if (cells.get(Parameter.STATION_DIRECTED_TIME_TARGET).isEnabled()) {
				result += cells.get(Parameter.STATION_DIRECTED_TIME_TARGET).evaluate(max_value);
			}
		}
		
		if (cells.get(Parameter.STATION_INCOMING_TIME_WEIGHT).isEnabled() || cells.get(Parameter.STATION_DIRECTED_TIME_TARGET).isEnabled()) {
			double max_value = 0.0;
			for (ResultPair pair : getIncomingTimeConnectedStations(station)) {
			
				if (cells.get(Parameter.STATION_INCOMING_TIME_WEIGHT).isEnabled()) {
					cells.get(Parameter.STATION_INCOMING_TIME_WEIGHT).evaluate(pair.cost);
				}
				
				
				if (cells.get(Parameter.STATION_DIRECTED_TIME_TARGET).isEnabled()) {
					List<Object[]> filteredCommunications = getCommunicationOfStation(others, pair.station);
					for (Object[] communication : filteredCommunications) {
						if (max_value == 0.0 || max_value < Math.abs((double) communication[2])) {
							max_value = Math.abs((double) communication[2]);
						}
					}
				}
				
			}
			
			if (cells.get(Parameter.STATION_DIRECTED_TIME_TARGET).isEnabled()) {
				result += cells.get(Parameter.STATION_DIRECTED_TIME_TARGET).evaluate(max_value);
			}
		}
		return result;
	}
	
	
	

	
	private static List<Object[]> getCommunicationOfStation(HashMap<Agent, Object> others, Station station) {
		List<Object[]> result = new ArrayList<>();
		for (Map.Entry<Agent, Object> entry : others.entrySet()) {
			if (entry.getValue() == null) continue; 
			Object[] communication = (Object[]) entry.getValue();
			if (station.name.equals(((Station)communication[0]).name)) result.add(communication);
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
	
	private static int remainingVisitsOfAStationType(Agent me, StationType type) {
		int result = 0;
		for (Map.Entry<Station, Integer> entry : me.necessities.entrySet()) {
			if (entry.getKey().type == type && entry.getValue() > 0) {
				result += entry.getValue();
			}
		}
		return result;
	}
	
	private static boolean isNearestStation(Agent me, Station station) {
		if (me.previousTarget == null) return true;
		int result = pathCost(me.previousTarget.type, station.type);
		
		ArrayList<StationType> used = new ArrayList<>();
		for (Map.Entry<Station, Integer> entry : me.necessities.entrySet()) {
			if (used.contains(entry.getKey().type)) continue;
			if (entry.getValue() < 1) continue;
			used.add(entry.getKey().type);
			if (result > pathCost(me.previousTarget.type, entry.getKey().type)) {
				return false;
			}
		}
		return true;
	}
	
	private static boolean isBoldEdge(Agent me, Station station) {
		for (VisitEdge edge : station.type.visitEdges) {
			if (edge.bold && (AgentType) edge.connectedType == me.type) return true;
		}
		return false;
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
	
	/*
	private static double timeEdgeHandling(Agent me, HashMap<Agent, Object> others,long time, Station station) {
		List<Station> outgoingConnectedStations = getOutgoingTimeConnectedStations(station);
		System.out.println(String.format("Time: %d Agent: %s Station: %s",time, me.name, station.name));
		for (Agent agent : others.keySet()) {
			//System.out.println(String.format("Agent visiting: %b Agent time: %d List Contains Station: %b", agent.visiting, agent.time,outgoingConnectedStations.contains(agent.target)));
			if (!agent.visiting) continue;
			if (agent.time == -1) continue;
			if (!outgoingConnectedStations.contains(agent.target)) continue;
			//System.out.println(String.format("Path cost: %d Station time: %d Agent time %d", pathCost(me.previousTarget.type, station.type), station.type.time, agent.time));
			if (pathCost(me.previousTarget.type, station.type) + station.type.time <= agent.time) return 10.0;
		}
		return 0.0;
	}
	*/
	
	private static boolean isNeighbourStation(Agent me, Station station) {
		if (me.previousTarget == null) return false;
		for (PlaceEdge edge : me.previousTarget.type.placeEdges) {
			if (edge.connectedType == station.type) return true;
		}
		return false;
	}
	
	private static double estimatedWorkTimeLeft(Agent me) {
		double result = 0.0;
		for (Map.Entry<Station, Integer> entry : me.necessities.entrySet()) {
			if (entry.getValue() == 0) continue;
			
			if (entry.getKey().type.time == -1 && me.type.time == -1) {
				result += entry.getValue();
			} else if (me.type.time == -1) {
				result += entry.getValue() * entry.getKey().type.time;
			} else {
				result += entry.getValue() * me.type.time;
			}
		}
		return result;
	}
	
	private static boolean graphHasTimeEdges(List<Station> stations) {
		for (Station station : stations) {
			if (station.type.timeEdges.size() > 0) return true;
		}
		return false;
	}
	
	private static boolean graphHasBoldVisitEdge(List<Station> stations) {
		for (Station station : stations) {
			for (VisitEdge edge : station.type.visitEdges) {
				if (edge.bold) return true;
			}
		}
		return false;
	}
	
	private record CustomEdge(StationType source, StationType destination, int weight) implements Comparable<CustomEdge> {
		@Override
		public int compareTo(CustomEdge other) {
			return Integer.compare(this.weight, other.weight);
		}
	}
	
	private static List<StationType> edgeStations(List<CustomEdge> customEdge) {
		HashMap<StationType, Integer> map = new HashMap<>();
		for (CustomEdge edge : customEdge) {
			if (map.containsKey(edge.source)) map.put(edge.source, map.get(edge.source) + 1);
			else map.put(edge.source, 1);
			if (map.containsKey(edge.destination)) map.put(edge.destination, map.get(edge.destination) + 1);
			else map.put(edge.destination, 1);
		}
		List<StationType> result = new ArrayList<>();
		
		int minValue = Collections.min(map.values());
		for (Map.Entry<StationType, Integer> entry : map.entrySet()) {
			if (entry.getValue() == minValue) result.add(entry.getKey());
		}
		return result;
	}
	
	
	private static List<StationType> minimumSpanningTree(Agent me) {
		List<CustomEdge> edges = new ArrayList<>();
		List<StationType> used = new ArrayList<>();
		for (Map.Entry<Station, Integer> entry : me.necessities.entrySet()) {
			if (entry.getValue() < 1) continue;
			if (used.contains(entry.getKey().type)) continue;
			for (PlaceEdge placeEdge : entry.getKey().type.placeEdges) {
				if (used.contains(placeEdge.connectedType)) continue;
				edges.add(new CustomEdge(entry.getKey().type, (StationType) placeEdge.connectedType, placeEdge.weight));
			}
			used.add(entry.getKey().type);
		}
		List<CustomEdge> mstEdges = new ArrayList<>();
		Collections.sort(edges);
		used.clear();
		for (CustomEdge customEdge : edges) {
			if (used.contains(customEdge.source) && used.contains(customEdge.destination)) continue;
			mstEdges.add(customEdge);
			if (!used.contains(customEdge.source)) used.add(customEdge.source);
			if (!used.contains(customEdge.destination)) used.add(customEdge.destination);
		}
		return edgeStations(mstEdges);
	}
	
	
	private static double agentDistribution(Agent me, HashMap<Agent, Object> others, long time, Station station) {
		HashMap<Station, Long> distribution = new HashMap<>();
		for (Station tmpStation : me.necessities.keySet()) {
			distribution.put(tmpStation, 0L);
		}
		long sum = 0L;
		for (Map.Entry<Agent, Object> entry : others.entrySet()) {
			if (entry.getKey().type != me.type) continue;
			if (entry.getValue() == null) continue;
			Object[] communication = (Object[]) entry.getValue();
			if (communication[0] == null) continue;
			if (!distribution.containsKey((Station) communication[0])) continue;
			distribution.merge((Station) communication[0], 2L, Long::sum);
			sum++;
		}
		if (sum == 0L) sum = 1L;
		double value = ((double) distribution.get(station)) / (sum * time);
		
		
		//System.out.println(String.format("Time: %d Agent: %s, Station %s, Distribution Value: %f", 
		//		time, me.name, station.name, value));
		return value;
	}
	
	private static boolean otherStationsReachable(Agent me, Station station) {
		for (Map.Entry<Station, Integer> entry : me.necessities.entrySet()) {
			if (entry.getValue() <= 0) continue;
			//System.out.println(String.format("From %s to %s", station.type.name , entry.getKey().type.name));
			if (pathCost(station.type, entry.getKey().type) == -1) return false;
		}
		//System.out.println(String.format("Agent: %s Station: %s All Stations are reachable", me.name, station.name));
		return true;
	}
	
	private static int estimatedStationTime(Station station) {
		int result = Integer.MAX_VALUE;
		if (station.type.time != -1) result = station.type.time;
		for (VisitEdge edge : station.type.visitEdges) {
			int time = ((AgentType) edge.connectedType).time;
			if (time != -1 && time < result) result = time;
		}
		System.out.println(String.format("Station: %s Estimated Time: %d", station.name, result));
		if (result == Integer.MAX_VALUE) return 0;
		return result;
	}
	
}
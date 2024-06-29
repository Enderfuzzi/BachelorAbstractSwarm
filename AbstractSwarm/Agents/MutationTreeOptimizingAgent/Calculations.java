import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;


public class Calculations {
	
	private static HashMap<Attribute, Node> attributeNodes = new HashMap<>();
	
	private static HashMap<AgentType, Integer> totalTime = new HashMap<>();
	
	private static boolean firstRun = true;
	
	private static boolean directedTimeEdges = false;
	private static boolean undirectedTimeEdges = false;
	private static boolean stationFrequency = false;
	private static boolean agentFrequency = false;
	
	private static Random random = new Random();

	public static ProbabilityStatistic baseProbability = new ProbabilityStatistic("path", "space", "distribution");
	
	private static MutationStatistic mutationStatistic = new MutationStatistic();

	

	
	private static final boolean TEXT_OUTPUT = false;
	
	
	private static List<Tree> currentTrees = new ArrayList<>();
	private static int currentTreeIndex = 0;
	private static List<Tree> bestTrees = new ArrayList<>();
	
	private static boolean generateTree = true;
	
	
	private static ProbabilityStatistic mutationProbability = new ProbabilityStatistic();
	
	private static ProbabilityStatistic basicMutationProbability = new ProbabilityStatistic();

	public static double evaluate(Agent me, HashMap<Agent, Object> others, List<Station> stations, long time, Station station,
			TimeStatistics timeStatistic) {

		
		if (firstRun) {

			directedTimeEdges = graphHasDirectedTimeEdges(stations);
			undirectedTimeEdges = graphHasUndirectedTimeEdges(stations);
			stationFrequency = graphHasStationFrequency(stations);
			agentFrequency = graphHasAgentFrequency(me, others);
			
			attributeNodes.put(Attribute.STATION_FREQUENCY, new ConsumerNode((OwnConsumer) Calculations::stationFrequency, Attribute.STATION_FREQUENCY.name()));
			
			
			attributeNodes.put(Attribute.AGENT_FREQUENCY, new ConsumerNode((OwnConsumer) Calculations::computeAgentFrequency, Attribute.AGENT_FREQUENCY.name()));
			
			attributeNodes.put(Attribute.MAX_DISTRIBUTION, new ConsumerNode((OwnConsumer) Calculations::maxDistribution, Attribute.MAX_DISTRIBUTION.name()));
			
			if (directedTimeEdges) {
				attributeNodes.put(Attribute.INCOMING_TIME_CONNECTION, new ConsumerNode((OwnConsumer) Calculations::computeIncomingConnectedStations, Attribute.INCOMING_TIME_CONNECTION.name()));
				attributeNodes.put(Attribute.OUTGOING_TIME_CONNECTION, new ConsumerNode((OwnConsumer) Calculations::computeOutgoingConnectedStations, Attribute.OUTGOING_TIME_CONNECTION.name()));
			}
			if (undirectedTimeEdges) {
				attributeNodes.put(Attribute.UNDIRECTED_TIME_CONNECTION, new ConsumerNode((OwnConsumer) Calculations::computeUndirectedTimeConnectedStations, Attribute.UNDIRECTED_TIME_CONNECTION.name()));
			}
			attributeNodes.put(Attribute.PATH_COST, new OperatorNode(Operator.MULTIPLICATION, new ValueNode(-1.0), new ConsumerNode((OwnConsumer) Calculations::pathCost, Attribute.PATH_COST.name())));
			
			attributeNodes.put(Attribute.STATION_SPACE, new OperatorNode(Operator.DIVISION, new ConsumerNode((OwnConsumer) Calculations::stationSize, "Station Size"), 
					new ConsumerNode((OwnConsumer) Calculations::agentSize, "Agent Size")));
			
			attributeNodes.put(Attribute.AGENT_TIME, new OperatorNode(Operator.DIVISION, new ConsumerNode((OwnConsumer) Calculations::totalAgentTime, "Total Agent Time"), 
					new ConsumerNode((OwnConsumer) Calculations::estimatedWorkTimeLeft, "Work Time Left")));
			
		
			
			if (directedTimeEdges) {
				baseStatistic.add("directedTime");
			}
			
			if (undirectedTimeEdges) {
				baseStatistic.add("undirectedTime");
			}
			
			mutationProbability.add("mutation", 0.8);
			basicMutationProbability.add("value");
			basicMutationProbability.add("consumer");
			basicMutationProbability.add("operator");
			
		}
		
		
		if (!totalTime.containsKey(me.type)) {
			totalTime.put(me.type, estimatedWorkTimeLeft(me, others, station));
		}
		
		// Regardless of plans if the station would result in a deadlock don't choose it
		if (!otherStationsReachable(me, station)) {
			return -100;
		}
		// agent to large
		if (agentSize(me, others, station) > stationSize(me, others, station)) {
			return -100;
		}
		//station is not reachable
		if (pathCost(me.previousTarget.type, station.type) == -1) {
			return -100;
		}
		
		
		if (timeStatistic.newBestRun) {
			bestTrees = new ArrayList<>(currentTrees);
		}
		
		
		if (timeStatistic.newRun) {
			//System.out.println(currentTrees);
			currentTrees.clear();
			currentTreeIndex = 0;
		}
		
		if (timeStatistic.lastValue != timeStatistic.time) {
			currentTreeIndex++;	
		}
		
		
		
		Tree evaluation = new Tree();
		
		if (generateTree || currentTreeIndex >= bestTrees.size()) {
		
			if (timeStatistic.newRun) {
				if (TEXT_OUTPUT) System.out.println(baseStatistic);
				
				baseStatistic.newRandom();
				baseStatistic.reset();
				
				if (TEXT_OUTPUT) System.out.println(baseStatistic.getAverage());
			}
			
			
			
			
			if (baseStatistic.compare("path")) {
				evaluation.addNode(attributeNodes.get(Attribute.PATH_COST));
			}
			
			if (baseStatistic.compare("space")) {
				evaluation.addNode(attributeNodes.get(Attribute.STATION_SPACE));
			}
			
			if (baseStatistic.compare("distribution")) {
	
				if (stationFrequency) {
					evaluation.addNode(attributeNodes.get(Attribute.STATION_FREQUENCY));
					
				}
				if (agentFrequency) {
					evaluation.addNode(attributeNodes.get(Attribute.AGENT_FREQUENCY));
					
				}
			}
			
			if (baseStatistic.compare("directedTime")) {
				evaluation.addNode(attributeNodes.get(Attribute.INCOMING_TIME_CONNECTION));
				evaluation.addNode(attributeNodes.get(Attribute.OUTGOING_TIME_CONNECTION));
			}
			
			if (baseStatistic.compare("undirectedTime")) {
				evaluation.addNode(attributeNodes.get(Attribute.UNDIRECTED_TIME_CONNECTION));
			}
			
			
			if ((!stationFrequency && !agentFrequency || !baseStatistic.compare("distribution")) && !baseStatistic.compare("space") && !baseStatistic.compare("path") && 
					!baseStatistic.compare("directedTime") && !baseStatistic.compare("undirectedTime")) {
				evaluation.addNode(attributeNodes.get(Attribute.MAX_DISTRIBUTION));
			}
		} else {
			evaluation = bestTrees.get(currentTreeIndex).copy();

			//System.out.println(statistic);
			if (timeStatistic.newRun) {
				mutationProbability.newRandom();
				basicMutationProbability.newRandom();
			}
			
			if (timeStatistic.lastValue != timeStatistic.time || currentTrees.size() < 1) {
				if (mutationProbability.compare("mutation")) {
					
					if (basicMutationProbability.compare("value")) {
						evaluation = TreeMutation.valueMutation(evaluation);
					}
					
					if (basicMutationProbability.compare("operator")) {
						evaluation = TreeMutation.mutateOperator(mutationStatistic, evaluation);
					}
					
					if (basicMutationProbability.compare("consumer")) {
						evaluation = TreeMutation.consumerWeightMutation(evaluation);
					}
					
				}
			} else {
				evaluation = currentTrees.get(currentTrees.size() - 1);
			}
			
			
			
		}
		
		
		
		if (timeStatistic.lastValue != timeStatistic.time) {
			currentTrees.add(evaluation);
		}
		
		if (timeStatistic.numberOfRuns >= 50) {
			generateTree = false;
		}

	
		
		
		
		
		firstRun = false;
		
		/*
		System.out.println("---------------------------------");
		System.out.println("Default: " + evaluation);
		
		System.out.println("Weight Mutation: " + TreeMutation.consumerWeightMutation(evaluation));
		System.out.println("Operator Mutation: " + TreeMutation.mutateOperator(mutationStatistic, evaluation));
		System.out.println("Value Mutation: " + TreeMutation.valueMutation(evaluation));
		
		System.out.println("Default: " + evaluation);
		System.out.println("----------------------------------");
		*/
		return evaluation.evaluate(me, others, station);
	}
	
	public static void communication(Agent me, HashMap<Agent, Object> others, List<Station> stations, 
			long time, Object[] defaultData, TimeStatistics timeStatistic){
	}
	
	public static long lastValue = 0;
	
	public static void reward(Agent me, HashMap<Agent, Object> others, List<Station> stations, long time, double value,
			TimeStatistics timeStatistic
			) {

		if (generateTree || currentTreeIndex >= bestTrees.size()) {
			baseStatistic.triggerCompare();
			
			baseStatistic.normalize();
			
			if (lastValue != time) {
				lastValue = time;
				
				baseStatistic.newRandom();
			}
		} else {
			mutationProbability.newRandom();
			
			basicMutationProbability.newRandom();
	}
	
	
	private static double computeAgentFrequency(Agent me,  HashMap<Agent, Object> others, Station station) {
		
		double result = 0.0;
		if (me.type.size * me.type.components.size() <= stationSize(me, others, station)) {
			result += 1.0;
		}
		
		List<StationType> used = new ArrayList<>();
		for (VisitEdge edge : me.type.visitEdges) {
			StationType stationType = (StationType) edge.connectedType;
			if (used.contains(stationType)) continue;
			used.add(stationType);
			if (me.type.size * me.type.components.size() > stationSize(stationType)) {
				result -= 0.5;
			}
			
		}
		
		List<AgentType> usedAgent = new ArrayList<>();
		usedAgent.add(me.type);
		for (Agent agent : others.keySet()) {
			if (usedAgent.contains(agent.type)) continue;
			usedAgent.add(agent.type);
			if (agent.type.size * agent.type.components.size() > stationSize(me, others, station)) {
				result += 0.5;
			}
		}
		
		if (station.space != -1) {
			if (station.space > me.type.size) {
				result += 0.5;
			}
		}
		
		return result;
	}
	
	private static double stationFrequency(Agent me, HashMap<Agent, Object> others, Station station) {
		if (station.frequency != -1) {
			double result = 0.0;
			if (me.previousTarget == station) result += 2.0;
			for (Object object : others.values()) {
				if (object == null) continue;
				Object[] communication = (Object[]) object;
				if (((Station) communication[0]) == station) {
					result -= 2.0;
				}
			}
			return result;
		} else {
			return 0;
		}
	}
	
	private static double maxDistribution(Agent me, HashMap<Agent, Object> others, Station station) {		
		if (me.necessities.getOrDefault(station, -1) > 0) {
			return 2.0;
		}
		
		double result = 0.0;
		for (Object object : others.values()) {
			if (object == null) continue;
			Object[] communication = (Object[]) object;
			if (((Station) communication[0]) == station) {
				result -= 2.0;
			}
		}
		return result + 2.0 * stationSize(me, others, station);
	}
	
	private static int stationSize(Agent me, HashMap<Agent, Object> others, Station station) {
		if (station.type.space == -1) return 1;
		return station.type.space;
	}
	
	private static int stationSize(StationType station) {
		if (station.space == -1) return 1;
		return station.space;
	}
	
	private static int agentSize(Agent me,  HashMap<Agent, Object> others, Station station) {
		if (me.type.size == -1) return 1;
		return me.type.size;
	}
	
	private static int stationTargeted(Agent me, HashMap<Agent, Object> others, Station station) {
		int counter = 0;
		for (Object object : others.values()) {
			if (object == null) continue;
			Object[] communication = (Object[]) object;
			if (((Station) communication[0]) == station) {
				counter += 1;
			}
		}
		if (TEXT_OUTPUT) System.out.println("Station target: " + station.name + " Number: " + counter);
		return counter;
	}
	
	private static boolean otherStationsReachable(Agent me, Station station) {
		for (Map.Entry<Station, Integer> entry : me.necessities.entrySet()) {
			if (entry.getValue() <= 0) continue;
			if (pathCost(station.type, entry.getKey().type) == -1) return false;
		}
		return true;
	}
	
	private static double computeOutgoingConnectedStations(Agent me, HashMap<Agent, Object> others, Station station) {
		double result = 0.0;
		List<ResultPair> outgoingTimeConnectedStations = getOutgoingTimeConnectedStations(station);
		for (ResultPair pair : outgoingTimeConnectedStations) {
			result += timeAtStation(me, others, pair.station);
			result += pair.cost;
			result += stationTargeted(me, others, pair.station);
		}
		return result;
	}
	
	private static double computeIncomingConnectedStations(Agent me, HashMap<Agent, Object> others, Station station) {
		double result = 0.0;
		List<ResultPair> incomingTimeConnectedStations = getIncomingTimeConnectedStations(station);
		for (ResultPair pair : incomingTimeConnectedStations) {
			result += timeAtStation(me, others, pair.station);
			result += pair.cost;
			result += stationTargeted(me, others, pair.station);
		}
		return result;
	}
	
	private static double computeUndirectedTimeConnectedStations(Agent me, HashMap<Agent, Object> others, Station station) {
		double result = 0.0;
		List<Station> undirectedStations = getUndirectedTimeConnectedStations(station); 
		for (Station s: undirectedStations) {
			result += timeAtStation(me, others, s);
			result += stationTargeted(me, others, s);
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
	
	private static List<Station> getUndirectedTimeConnectedStations(Station station) {
		List<Station> result = new ArrayList<>();
		for (TimeEdge edge : station.type.timeEdges) {
			if (TEXT_OUTPUT) System.out.println(String.format("Time Edge: Station: %s ConnectedType: %s Incoming: %b Outgoing: %b", station.name, edge.connectedType.name, edge.incoming, edge.outgoing));
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
				if (TEXT_OUTPUT) System.out.println(String.format("Path calculation: %s to %s with cost: %d", start.name, target.name, current.cost()));
				return current.cost();
			}
			
			for (PlaceEdge edge : current.station().placeEdges) {
				if (edge.incoming) continue;
				queue.add(new Pair((StationType) edge.connectedType, current.cost() + edge.weight));
			}
			
		}
		return -1;
	}
	
	private static int pathCost(Agent me, HashMap<Agent, Object> others, Station station) {
		// speed is 1 for each scenario
		// if speed should be considered then we have to divide the path cost with the agent speed
		return pathCost(me.previousTarget.type, station.type);
	}
	
	
	private static int timeAtStation(Agent me, HashMap<Agent, Object> others, Station station) {
		if (me.type.time == -1 && station.type.time == -1) return 1;
		if (me.type.time == -1) return station.type.time;
		if (station.type.time == -1) return me.type.time;
		return Math.min(me.type.time, station.type.time);
	}
	
	private static double totalAgentTime(Agent me, HashMap<Agent, Object> others, Station station) {
		return totalTime.get(me.type);
	}
	
	
	private static int estimatedWorkTimeLeft(Agent me, HashMap<Agent, Object> others, Station station) {
		int result = 0;		
		for (Map.Entry<Station, Integer> entry : me.necessities.entrySet()) {
			if (entry.getValue() == 0) continue;
			result += timeAtStation(me, others ,entry.getKey());
		}
		return result;
	}
	
	
	
	private static boolean graphHasDirectedTimeEdges(List<Station> stations) {
		for (Station station : stations) {
			if (station.type.timeEdges.size() > 0) {
				for (TimeEdge edge : station.type.timeEdges) {
					if (edge.incoming || edge.outgoing) return true;
				}
			}
		}
		return false;
	}
	
	private static boolean graphHasUndirectedTimeEdges(List<Station> stations) {
		for (Station station : stations) {
			if (station.type.timeEdges.size() > 0) {
				for (TimeEdge edge : station.type.timeEdges) {
					if (!edge.incoming && !edge.outgoing) return true;
				}
			}
		}
		return false;
	}
	
	private static boolean graphHasStationFrequency(List<Station> stations) {
		for (Station station : stations) {
			if (station.frequency != -1) return true;
		}
		return false;
	}
	
	private static boolean graphHasAgentFrequency(Agent me, HashMap<Agent, Object> others) {
		if (me.frequency != -1) return true;
		for (Agent agent : others.keySet()) {
			if (agent.frequency != -1) return true;
		}
		return false;
	}
}

record NodePair(Double value, Node node) {}

interface OwnConsumer {
	double compute(Agent me, HashMap<Agent, Object> others, Station station);
}


enum Attribute {
	STATION_FREQUENCY,
	AGENT_FREQUENCY,
	OUTGOING_TIME_CONNECTION,
	INCOMING_TIME_CONNECTION,
	UNDIRECTED_TIME_CONNECTION,
	PATH_COST,
	STATION_SPACE,
	AGENT_TIME,
	MAX_DISTRIBUTION,
	
	;
}


class ProbabilityStatistic {
	
	HashMap<String , Pair> map = new HashMap<>();
	
	HashMap<String, List<Double>> pastValues = new HashMap<>();
	
	public ProbabilityStatistic(String...strings) {
		for (String name : strings) {
			this.add(name);
		}
	}
	
	public void add(String name, double threshold) {
		map.put(name, new Pair(threshold));
	}
	
	public void add(String name) {
		add(name, 0.35);
	}
	
	public boolean compare(String name) {
		if (map.containsKey(name)) {
			return map.get(name).compare();
		}
		return false;
	}
	
	public void reset() {
		addToPast();
		Pair highest = null;
		for (Pair pair : map.values()) {
			if (highest == null || highest.getThreshold() < pair.getThreshold()) {
				highest = pair;
			}
		}
		for (Map.Entry<String, Pair> entry : map.entrySet()) {
			if (entry.getValue() != highest) {
				entry.getValue().setThreshold(computeAverage(entry.getKey()));
			} else {
				entry.getValue().setThreshold(computeAverage(entry.getKey() + 0.05));
			}
		}
	}
	
	public void newRandom() {
		for (Pair pair : map.values()) {
			pair.newRandom();
		}
	}
	
	public void increaseThreshold(String name, double value) {
		if (map.containsKey(name)) {
			map.get(name).setThreshold(map.get(name).getThreshold() + value);
		}
	}
	
	public void triggerCompare() {
		for (Pair pair : map.values()) {
			if (pair.compare()) {
				pair.increaseThreshold(0.15);
			}
		}
	}
	
	public void normalize() {
		double sum = 0.0;
		for (Pair pair : map.values()) {
			sum += pair.getThreshold();
		}
		if (sum != 0.0) {
			for (Pair pair : map.values()) {
				pair.setThreshold(pair.getThreshold() / sum);
			}
		}
	}
	
	private void addToPast() {
		for (Map.Entry<String, Pair> entry : map.entrySet()) {
			if (!pastValues.containsKey(entry.getKey())) {
				pastValues.put(entry.getKey(), new ArrayList<>());
			}
			List<Double> tmpList = pastValues.get(entry.getKey());
			tmpList.add(entry.getValue().getThreshold());
			pastValues.put(entry.getKey(), tmpList);
		}
	}
	
	public double computeAverage(String name) {
		if (pastValues.containsKey(name)) {
			double sum = 0.0;
			for (Double d : pastValues.get(name)) {
				sum += d;
			}
			return sum / (double) pastValues.get(name).size();
			
		}
		return 0.0;
	}
	
	public String getAverage() {
		StringBuilder sb = new StringBuilder();
		for (String name : map.keySet()) {
			sb.append(String.format("[TimeStatistic Average]: %s -> %f\n", name, computeAverage(name)));
		}
		return sb.toString();
	}
	
	private class Pair {
		private static Random random = new Random();
		
		private double randomValue;
		private double threshold;
		
		public Pair(double threshold) {
			this.threshold = threshold;
			newRandom();
		}
		
		private void checkThreshold() {
			this.threshold = Math.max(this.threshold, 0.05);
		}
		
		public void setThreshold(double value) {
			this.threshold = value;
			checkThreshold();
		}
		
		public void increaseThreshold(double value) {
			this.threshold += value;
			checkThreshold();
		}
		
		public double getThreshold() {
			return this.threshold;
		}
		
		public void newRandom() {
			this.randomValue = random.nextDouble();
		}
		
		public boolean compare() {
			return randomValue <= threshold;
		}
		@Override
		public String toString() {
			return String.format("[Random: %f Threshold %f]", randomValue, threshold);
		}
		
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, Pair> entry : map.entrySet()) {
			sb.append(String.format("[RandomStatistic]: %s -> %s\n", entry.getKey(), entry.getValue().toString()));
		}
		return sb.toString();
	}
	
}



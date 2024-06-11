import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;


public class GreedyCalculations {

	/** The random generator used for random decisions. */
	private static Random random = new Random();
	
	
	private static HashMap<Station, Double> stationVisit = new HashMap<>();
	
	
	private static HashMap<Attribute, Node> attributeNodes = new HashMap<>();
	
	private static HashMap<AgentType, Integer> totalTime = new HashMap<>();
	
	private static boolean firstRun = true;
	
	private static Node evaluationTree;
	
	private static boolean directedTimeEdges = false;
	private static boolean undirectedTimeEdges = false;
	private static boolean stationFrequency = false;
	private static boolean agentFrequency = false;
	
	
	private static List<Attribute> singleStrategies = new ArrayList<>();
	private static HashMap<Attribute, Double> singleStrategyValue = new HashMap<>();
	private static Attribute currentAttribute;
	
	private static Node activeNode;
	private static double activeValue;
	
	private static WeightedList weightedList = new WeightedList();
	
	private static double addParamThreshold = 0.5;
	private static double mutationThreshold = 0.2;
	
	private static int numberOfManipulations = 0;
	
	private static PriorityQueue<NodePair> maxHeap = new PriorityQueue<>(new Comparator<NodePair>() {
		@Override
		public int compare(NodePair first, NodePair second) {
			if (first == null && second == null) return 0;
			if (first == null) return -1;
			if (second == null) return 1;
			return -1 * Double.compare(first.value(), second.value());
		}
	});
	
	public static double evaluate(Agent me, HashMap<Agent, Object> others, List<Station> stations, long time, Station station,
			TimeStatistics timeStatistic) {
		
		
		if (firstRun) {
			/*
			for (Attribute attribute : Attribute.values()) {
				attributeNodes.put(attribute, new Node(0));
			}
			*/
			directedTimeEdges = graphHasDirectedTimeEdges(stations);
			undirectedTimeEdges = graphHasUndirectedTimeEdges(stations);
			stationFrequency = graphHasStationFrequency(stations);
			agentFrequency = graphHasAgentFrequency(me, others);
			
			attributeNodes.put(Attribute.STATION_FREQUENCY, new Node((OwnConsumer) GreedyCalculations::stationFrequency));
			
			
			attributeNodes.put(Attribute.AGENT_FREQUENCY, new Node((OwnConsumer) GreedyCalculations::computeAgentFrequency));
			
			attributeNodes.put(Attribute.MAX_DISTRIBUTION, new Node((OwnConsumer) GreedyCalculations::maxDistribution));
			
			if (directedTimeEdges) {
				attributeNodes.put(Attribute.INCOMING_TIME_CONNECTION, new Node((OwnConsumer) GreedyCalculations::computeIncomingConnectedStations));
				attributeNodes.put(Attribute.OUTGOING_TIME_CONNECTION, new Node((OwnConsumer) GreedyCalculations::computeOutgoingConnectedStations));
			}
			if (undirectedTimeEdges) {
				attributeNodes.put(Attribute.UNDIRECTED_TIME_CONNECTION, new Node((OwnConsumer) GreedyCalculations::computeUndirectedTimeConnectedStations));
			}
			attributeNodes.put(Attribute.PATH_COST, new Node(Operator.MULTIPLICATION, -1.0, new Node((OwnConsumer) GreedyCalculations::pathCost)));
			attributeNodes.put(Attribute.STATION_SPACE, new Node(Operator.DIVISION, new Node((OwnConsumer) GreedyCalculations::stationSize), new Node((OwnConsumer) GreedyCalculations::agentSize)));
			attributeNodes.put(Attribute.AGENT_TIME, new Node(Operator.DIVISION, new Node((OwnConsumer) GreedyCalculations::totalAgentTime), new Node((OwnConsumer) GreedyCalculations::estimatedWorkTimeLeft)));
			
			
			evaluationTree = new Node(Operator.ADDITION, 
					new Node(Operator.ADDITION, attributeNodes.get(Attribute.STATION_FREQUENCY), attributeNodes.get(Attribute.AGENT_FREQUENCY)), 
					new Node(Operator.ADDITION, attributeNodes.get(Attribute.PATH_COST),
							new Node(Operator.ADDITION, attributeNodes.get(Attribute.STATION_SPACE), attributeNodes.get(Attribute.AGENT_TIME))
					)
			);
			
			if (directedTimeEdges) {
				evaluationTree = new Node(Operator.ADDITION, evaluationTree, new Node(Operator.ADDITION, attributeNodes.get(Attribute.INCOMING_TIME_CONNECTION), attributeNodes.get(Attribute.OUTGOING_TIME_CONNECTION)));
			}
			
			if (undirectedTimeEdges) {
				evaluationTree = new Node(Operator.ADDITION, evaluationTree, attributeNodes.get(Attribute.UNDIRECTED_TIME_CONNECTION));
			}
			
			
			
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
		
		
		
		
		if (firstRun && timeStatistic.newRun) {
			for (Attribute attribute : attributeNodes.keySet()) {
				singleStrategies.add(attribute);
				singleStrategies.add(attribute);
			}
			Collections.shuffle(singleStrategies);
		}
		
		if (timeStatistic.newRun) {
			//perfect solution has been found
			if (maxHeap.peek() != null && maxHeap.peek().value() == 1.0) {
				activeNode = maxHeap.peek().node();
			} else {
				
				if (activeNode != null) {
					maxHeap.add(new NodePair(activeValue, activeNode));
				}
				// first use the single strategies
				if (singleStrategies.size() > 0) {
					currentAttribute = singleStrategies.remove(0);
					activeNode = attributeNodes.get(currentAttribute);
					//System.out.println(currentAttribute);
				} else {
					if (currentAttribute != null) {
						for (Map.Entry<Attribute, Double> entry : singleStrategyValue.entrySet()) {
							weightedList.add(entry.getKey(), entry.getValue());
						}
						
						activeNode = evaluationTree;
					} else {
						// combine two base methods 
						/*
						Attribute first = weightedList.getRandomAttribute();
						Attribute second = weightedList.getRandomAttribute();
						while (first == second) second = weightedList.getRandomAttribute();
						
						activeNode = new Node(Operator.DIVISION, attributeNodes.get(first), attributeNodes.get(second));
						 */
						double randomValue = random.nextDouble();
						/*
						System.out.println("First Value: " + randomValue * 2 / maxHeap.peek().node().depth() + "Second: " + addParamThreshold);
						
						System.out.println("third Value: " + randomValue * (maxHeap.peek().node().depth() / 3.0) + "fourth: " + mutationThreshold);
						System.out.println("Bevor: " + maxHeap.peek().node());
						System.out.println("Number of Consumer: " + maxHeap.peek().node().numberOfConsumer());
						System.out.println("Numnber of manipulations: " + numberOfManipulations); 
						*/
						if (maxHeap.peek() != null) { 
							if (randomValue * 2 / maxHeap.peek().node().depth() >= addParamThreshold) {
								activeNode = new Node(Operator.ADDITION, maxHeap.peek().node(), attributeNodes.get(weightedList.getRandomAttribute()));
								numberOfManipulations++;
							} else if (randomValue * (maxHeap.peek().node().depth() / 3.0) >= mutationThreshold) {
								Node tree = new Node(maxHeap.peek().node());
								if (randomValue >= 0.5) {
									//System.out.println("Number of Operators: " + tree.numberOfOperators());
									Node operator = tree.getOperatorNode(random.nextInt(tree.numberOfOperators()));
									operator.setOperator(Operator.values()[random.nextInt(Operator.values().length)]);
									activeNode = tree;
									numberOfManipulations++;
								} else {
									//System.out.println("Number of Consumer: " + tree.numberOfConsumer());
									Node consumer = tree.getConsumerNode(random.nextInt(tree.numberOfConsumer()));
									consumer.setLeft(new Node(consumer));
									consumer.setOperator(Operator.MULTIPLICATION);
									consumer.setRight(new Node(random.nextDouble(2.0)));
									activeNode = tree;
									numberOfManipulations++;
								}
								
							}
						} else {
							activeNode = evaluationTree;
						}
						
						//System.out.println(activeNode);
						
					}
					
				
					
					currentAttribute = null;
				}
			}
			
			activeValue = 1.0;
		} 
		
		
		firstRun = false;
		
		if (activeNode != null) {
			return activeNode.evaluate(me, others, station);
		}
		
		return evaluationTree.evaluate(me, others, station);
		
	}
	
	public static void communication(Agent me, HashMap<Agent, Object> others, List<Station> stations, 
			long time, Object[] defaultData, TimeStatistics timeStatistic){
	}
	
	public static void reward(Agent me, HashMap<Agent, Object> others, List<Station> stations, long time, double value,
			TimeStatistics timeStatistic
			) {
		if (activeNode != null) {
			if (activeValue > value) activeValue = value;
		}
		
		
		
		if (currentAttribute != null) {
			if (!singleStrategyValue.containsKey(currentAttribute)) {
				singleStrategyValue.put(currentAttribute, value);
			} else {
				if (singleStrategyValue.get(currentAttribute) > value) {
					singleStrategyValue.put(currentAttribute, value);
				}
			}
		}		
		//System.out.println(singleStrategyValue);
	}
	
	
	private static double computeAgentFrequency(Agent me,  HashMap<Agent, Object> others, Station station) {
		if ((stationSize(me, others,station) * station.type.components.size()) - (stationTargeted(me, others, station) * agentSize(me, others, station)) > 0) {
			return ((agentSize(me, others, station) * me.type.components.size()) / ((stationSize(me, others, station) * station.type.components.size())) - stationTargeted(me, others, station));
		} else {
			return station.space;
		}
		/*
		 * if (station.type 
		 * 
		 * 
		 */
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
		//System.out.println("Station target: " + station.name + " Number: " + counter);
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
		if (outgoingTimeConnectedStations.size() > 0) {
			result /= 2.0 * outgoingTimeConnectedStations.size();
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
		if (incomingTimeConnectedStations.size() > 0) {
			result /= 2.0 * incomingTimeConnectedStations.size();
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
		if (undirectedStations.size() > 0) {
			result /= 2.0 * undirectedStations.size();
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
	
	private static int pathCost(Agent me, HashMap<Agent, Object> others, Station station) {
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
	/*
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
	*/
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
		// return station.type.space - (station.type.space % me.type.size)
	}
	
	private static double spaceEfficiency(Agent me, Station station) {
		if (station.type.space == -1) return 0;
		if (me.type.size == -1) return station.type.space;
		return station.type.space - (station.type.space % me.type.size);
	}
	
	
	private static int estimatedWorkTimeLeft(Agent me, HashMap<Agent, Object> others, Station station) {
		int result = 0;		
		for (Map.Entry<Station, Integer> entry : me.necessities.entrySet()) {
			if (entry.getValue() == 0) continue;
			result += timeAtStation(me, others ,entry.getKey());
			//System.out.println("Station: " +  entry.getKey().name + " Time: " + timeAtStation(me, entry.getKey().type));
		}
		//System.out.println("Agent: " + me.name + " Time left: " + result);
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


class WeightedList {
	private static Random random = new Random();
	
	private double sumWeight = 0.0;
	private List<Pair> list = new ArrayList<>();
	
	private record Pair(Attribute attribute,Double lowerBound, Double upperbound) {};
	
	public void add(Attribute attribute, double weight) {
		list.add(new Pair(attribute, sumWeight, sumWeight + weight));
		sumWeight += weight;
	}
	
	public Attribute getRandomAttribute() {
		double randomValue = random.nextDouble(sumWeight);
		for (Pair pair : list) {
			if (randomValue >= pair.lowerBound && randomValue < pair.upperbound) return pair.attribute;
		}
		return null;
	}
	
	
	
}

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


enum Operator {
	ADDITION("+"),
	SUBTRACTION("-"),
	MULTIPLICATION("*"),
	DIVISION("/"),
	MODULO("%"),
	
	;
	
	private final String representation;
	
	private Operator(String representation) {
		this.representation = representation;
	}
	
	public double evaluate(double first, double second) {
		if (this == ADDITION) return first + second;
		if (this == SUBTRACTION) return first - second;
		if (this == MULTIPLICATION) return first * second;
		if (this == DIVISION) {
			if (second == 0.0) return first / 1.0;
			return first / second;
		}
		if (this == MODULO) {
			if (second == 0.0) return first % 1;
			return first % second;
		}
		return 0;
	}
	
	@Override
	public String toString() {
		return this.representation;
	}
}


class Node {
	private Operator operator;
	private double value;
	private OwnConsumer consumer;
	
	private Node left;
	private Node right;
	
	public Node(Operator operator, Node left, Node right) {
		this.operator = operator;
		this.left = left;
		this.right = right;
	}
	
	public Node(Operator operator, double left, double right) {
		this(operator, new Node(left), new Node(right));
	}
	
	public Node(Operator operator, double left, Node right) {
		this(operator, new Node(left), right);
	}
	
	public Node(Operator operator) {
		this(operator, null, null);
	}
	
	public Node(double value) {
		this(null, null, null);
		this.value = value;
	}
	
	public Node(OwnConsumer consumer) {
		this(null, null, null);
		this.consumer = consumer;
	}

	public Node(Node node) {
		if (node.isLeaf()) {
			this.value = node.value;
		}
		this.operator = node.operator;
		this.left = node.left;
		this.right = node.right;
	}
	
	private void changeValue(double value) {
		if (isLeaf()) this.value = value;
	}
	
	public boolean isLeaf() {
		return operator == null;
	}
	
	public boolean hasConsumer() {
		return consumer != null;
	}
	
	public void setOperator(Operator operator) {
		this.operator = operator;
	}
	
	public void setLeft(Node left) {
		this.left = left;
	}
	
	public void setRight(Node right) {
		this.right = right;
	}
	
	public double evaluate(Agent me, HashMap<Agent, Object> others, Station station) {
		if (isLeaf() && !hasConsumer()) return value;
		if (hasConsumer()) return consumer.compute(me, others, station);
		if (this.left != null && this.right != null) return operator.evaluate(this.left.evaluate(me, others, station), this.right.evaluate(me, others, station));
		return 0;
	}
	
	
	public int depth() {
		if (isLeaf()) return 1;
		if (this.left != null) return this.left.depth() + 1;
		if (this.right != null) return this.right.depth() + 1;
		return Math.max(this.left.depth(), this.right.depth()) + 1;
	}
	
	public int numberOfOperators() {
		if (isLeaf()) return 0;
		if (this.left != null && this.right == null) return this.left.numberOfOperators() + 1;
		if (this.right != null && this.left == null) return this.right.numberOfOperators() + 1;
		return this.left.numberOfOperators() + this.right.numberOfOperators() + 1;
	}
	
	public Node getOperatorNode(int index) {
		if (index == 0) return this;
		if (this.left != null && index <= this.left.numberOfOperators()) return this.left.getOperatorNode(index - 1);
		if (this.left != null && this.right != null) this.right.getOperatorNode(index - this.left.numberOfOperators() - 1);
		return this;
	}
	
	public int numberOfConsumer() {
		int result = this.hasConsumer() ? 1 : 0;
		if (this.left != null && this.right == null) return this.left.numberOfConsumer() + result;
		if (this.right != null && this.left == null) return this.right.numberOfConsumer() + result;
		if (this.left == null && this.right == null) return result;
		return this.left.numberOfConsumer() + this.right.numberOfConsumer() + result;
	}
	
	public Node getConsumerNode(int index) {
		if (index == 0) return this;
		int value = hasConsumer() ? 1 : 0;
		if (this.left != null && index <= this.left.numberOfConsumer()) return this.left.getConsumerNode(index - value);
		if (this.left != null && this.right != null) this.right.getConsumerNode(index - this.left.numberOfConsumer() - value);
		return this;
	}
	
	public String toString() {
		String result = "(";
		if (isLeaf() && !hasConsumer()) return "" + value;
		if (hasConsumer()) return "function";
		if (this.left != null) result += this.left.toString();
		result += " " + operator.toString() + " ";
		if (this.right != null) result += this.right.toString();
		result += ")";
		return result;
	}
	
}





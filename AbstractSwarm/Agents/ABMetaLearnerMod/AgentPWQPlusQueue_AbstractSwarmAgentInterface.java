
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Map.Entry;

public class AgentPWQPlusQueue_AbstractSwarmAgentInterface {
	private static HashMap<Agent, AgentPWQPlusQueue_QAgent> agentBrain = new HashMap<Agent, AgentPWQPlusQueue_QAgent>();
	private static HashMap<Agent, HashSet<Station>> startOfIterationHelper = new HashMap<Agent, HashSet<Station>>();
	private static HashMap<Agent, Station> lastStation = new HashMap<Agent, Station>();
	private static HashMap<Agent, Station> decision = new HashMap<Agent, Station>();
	private static HashMap<Agent, Long> decisionTimestamp = new HashMap<Agent, Long>();
	private static ArrayList<Integer> randomTurn = new ArrayList<Integer>();
	private static ArrayList<Agent> sortedAgents;
	private static ArrayList<Station> sortedStations;
	private static AgentPWQPlusQueue_VisitStatistics globalVisitStatistics;
	private static int iteration = 1;

	//HYPERPARAMETERS
	private static final float LEARNING_RATE = 0.25f;
	private static final float DISCOUNT_FACTOR = 0.5f;
	private static final float IQ_EPSILON = 0.98f;
	private static final float IQ_XI = 0.2f;
	private static final int EPSILON_TARGET_ITERATIONS = 95; //the (linear function) watermark for random exploration, from this point on no randomness is enforced
	private static final int REPLAY_MEMORY_SAMPLE_SIZE = 0; //12 -> produces problems with X-Ray 2
	private static final int REPLAY_MEMORY_SAMPLING_INTERVAL = 6;
	private static final int VISIT_STATS_ITERATION_WINDOW = 0; //0 -> keep track of every iteration, otherwise how many past iterations to keep track of
	private static final boolean ALLOW_TRANSITIVE_SELECT = true;
	private static final boolean EPSILON_GREEDY_EXPLORATION = true; //seems to work better than IQ Learning
	private static final boolean EXPLORATION_BIAS = false; //whether to add a bias to favour exploring unvisited stations
	private static final boolean PROBABILISTIC_SELECTION = false;
	private static final boolean SIMULATION_PREDICTION = false;

	private static final boolean DEBUG_OUT = false;
	private static final boolean EXPORT_AT_100TH_RUN = false;

	private static boolean isRandomAction(int iteration) {
		if (EPSILON_GREEDY_EXPLORATION)
			return Math.random() < 1 - (double)iteration / EPSILON_TARGET_ITERATIONS;
		else
			return iteration < EPSILON_TARGET_ITERATIONS && Math.random() < Math.pow(IQ_EPSILON, iteration);
	}
	private static boolean isTrueRandom(int iteration) {
		return Math.random() < IQ_XI;
	}
	private static int randomAction() {
		while (randomTurn.size() <= iteration) {
			if (EPSILON_GREEDY_EXPLORATION)
				randomTurn.add(isRandomAction(randomTurn.size()) ? 1 : 0);
			else {
				if (!isRandomAction(randomTurn.size()))
					randomTurn.add(0);
				else if (isTrueRandom(randomTurn.size()))
					randomTurn.add(1);
				else
					randomTurn.add(2);
			}
		}

		return randomTurn.get((int)iteration);
	}

	private static void initializeAgent(Agent me) {
		if (!agentBrain.containsKey(me)) {
			if (EPSILON_GREEDY_EXPLORATION)
				agentBrain.put(me, new AgentPWQPlusQueue_QAgent(sortedStations, globalVisitStatistics, REPLAY_MEMORY_SAMPLE_SIZE, REPLAY_MEMORY_SAMPLING_INTERVAL, LEARNING_RATE, DISCOUNT_FACTOR, !SIMULATION_PREDICTION && EXPLORATION_BIAS));
			else
				agentBrain.put(me, new AgentPWQPlusQueue_QAgent(me, sortedStations, globalVisitStatistics, REPLAY_MEMORY_SAMPLE_SIZE, REPLAY_MEMORY_SAMPLING_INTERVAL, LEARNING_RATE, DISCOUNT_FACTOR));
		}
	}
	private static double getAgentEvaluation(Agent me, HashMap<Agent, Object> others, Station station, long time) {
		AgentPWQPlusQueue_QAgent.EvaluationMode mode;
		if (randomAction() == 0)
			mode = AgentPWQPlusQueue_QAgent.EvaluationMode.EXPLOIT;
		else if (randomAction() == 1)
			mode = AgentPWQPlusQueue_QAgent.EvaluationMode.EXPLORE_TRUE;
		else
			mode = AgentPWQPlusQueue_QAgent.EvaluationMode.EXPLORE_AE;
		if (PROBABILISTIC_SELECTION) {
			if (!decisionTimestamp.containsKey(me) || decisionTimestamp.get(me).longValue() != time) {
				decisionTimestamp.put(me, time);

				ArrayList<Station> candidates = new ArrayList<Station>();
				ArrayList<Float> scores = new ArrayList<Float>();
				double totalScore = 0;
				for (Station s : sortedStations) {
					if (ALLOW_TRANSITIVE_SELECT) {
						if (AgentPWQPlusQueue_Pathing.canReachTransitiveWithoutSkip(me, lastStation.get(me).type, s.type)) {
							candidates.add(s);
							scores.add(agentBrain.get(me).evaluate(me, lastStation, others, agentBrain, station, time, mode));
							totalScore += scores.get(scores.size() - 1);
						}
					}
					else if (AgentPWQPlusQueue_Pathing.canReach(me, s)) {
						candidates.add(s);
						scores.add(agentBrain.get(me).evaluate(me, lastStation, others, agentBrain, station, time, mode));
						totalScore += scores.get(scores.size() - 1);
					}
				}

				double[] probabilities = new double[candidates.size()];
				for (int index = 0; index < probabilities.length; index++)
					probabilities[index] = scores.get(index) / totalScore;
				
				double selection = Math.random();
				double s = 0;
				int index = 0;
				while (s < selection)
					s += probabilities[index++];
				decision.put(me, candidates.get(index - 1));
			}
			return decision.get(me).name.equals(station.name) ? 1 : 0;
		}
		else
			return agentBrain.get(me).evaluate(me, lastStation, others, agentBrain, station, time, mode);
	}
	private static Object getAgentCommunication(Agent me, Object[] defaultData) {
		return agentBrain.get(me).communicate(me, defaultData);
	}
	private static void adaptAgent(Agent me, HashMap<Agent, Object> others, Station selectedTarget, double reward, long time) {
		if (globalVisitStatistics != null)
			globalVisitStatistics.update(me, lastStation.get(me), selectedTarget);
		agentBrain.get(me).reward(me, lastStation.get(me), selectedTarget, (float)reward, time);
	}

	/**
	 * This method allows an agent to perceive its current state and to perform
	 * actions by returning an evaluation value for potential next target
	 * stations. The method is called for every station that can be visted by
	 * the agent.
	 * 
	 * @param me       the agent itself
	 * @param others   all other agents in the scenario with their currently
	 *                 communicated information
	 * @param stations all stations in the scenario
	 * @param time     the current time unit
	 * @param station  the station to be evaluated as potential next target
	 * 
	 * @return the evaluation value of the station
	 */
	public static double evaluation(Agent me, HashMap<Agent, Object> others, List<Station> stations, long time, Station station) {
		//setting "lastStation" here might be deceiving as any "final" (absorbing) station is not stored here
		//however, there is no adequate point to set this otherwise as rewards do not seem to get calculated after a final action
		lastStation.put(me, me.previousTarget);

		if (time == 1) {
			//setup at start of simulation
			if (DEBUG_OUT)
				System.out.println(time + ": Agent " + me.name + " initializing.");
			
			if (sortedStations == null) {
				sortedStations = new ArrayList<Station>(stations);
				sortedStations.sort((i, j) -> i.name.compareTo(j.name));
			}
			if (sortedAgents == null) {
				sortedAgents = new ArrayList<Agent>(others.keySet());
				sortedAgents.add(me);
				sortedAgents.sort((i, j) -> i.name.compareTo(j.name));
			}
			if (!SIMULATION_PREDICTION) {
				if (globalVisitStatistics == null)
					globalVisitStatistics = new AgentPWQPlusQueue_VisitStatistics(sortedStations, VISIT_STATS_ITERATION_WINDOW);
				globalVisitStatistics.updateIteration();
			}
			for (Agent a : others.keySet())
				lastStation.put(a, a.previousTarget);
			
			randomTurn.clear();
			initializeAgent(me);

			if (!startOfIterationHelper.containsKey(me))
				startOfIterationHelper.put(me, new HashSet<Station>());
			//this station was already evaluated at timestamp 1, so we are doing a new iteration
			if (startOfIterationHelper.get(me).contains(station)) {
				for (Agent a : startOfIterationHelper.keySet()) {
					startOfIterationHelper.get(a).clear();
					agentBrain.get(a).updateIteration(a);
				}
				iteration++;
				if (DEBUG_OUT) {
					System.out.println();
					System.out.println("--ITERATION " + iteration + "--");
					System.out.println();
				}
				if (EXPORT_AT_100TH_RUN) {
					if (iteration == 100) {
						int index = 0;
						for (AgentPWQPlusQueue_QAgent agent : agentBrain.values())
							agent.export(index++);
					}
				}
			}
			startOfIterationHelper.get(me).add(station);
		}
		
		if (DEBUG_OUT) {
			//print remaining necessities
			for (Entry<Station, Integer> e : me.necessities.entrySet())
				System.out.println(time + ": Agent nec [" + e.getKey().name + "]: " + e.getValue());
		}
		//determine necessities at current station type and currently evaluated station
		boolean anyCurrentNecessity = false;
		for (int index = 0; index < sortedStations.size() && !anyCurrentNecessity; index++) {
			Station s = sortedStations.get(index);
			if (lastStation.get(me).type.name.equals(s.type.name)) { //any station of the lastly visited type
				if (s.necessities.containsKey(me) && s.necessities.get(me).intValue() > 0) //that has necessity for the agent
					anyCurrentNecessity = true;
				if (me.necessities.containsKey(s) && me.necessities.get(s).intValue() > 0) //or that the agent has necessity for
					anyCurrentNecessity = true;
			}
		}
		boolean targetNecessity = (me.necessities.containsKey(station) && me.necessities.get(station).intValue() > 0) ||
			(station.necessities.containsKey(me) && station.necessities.get(me).intValue() > 0);
		boolean anyAgentNecessity = false;
		for (Entry<Station, Integer> entry : me.necessities.entrySet()) {
			if (entry.getValue().intValue() > 0)
				anyAgentNecessity = true;
		}
		//boolean thisCurrentNecessity = anyCurrentNecessity && lastStation.get(me).name.equals(station.name) && targetNecessity;
		
		//do the agent's evaluation, even if the static policy defines some other behaviour (such as random)
		//this allows for training the agent on actions it did not choose itself
		double policyEvaluation = getAgentEvaluation(me, others, station, time);

		//there is no necessity for the currently evaluated target, so do not visit it explicitly (-1 score)
		if (anyAgentNecessity && !targetNecessity) {
			if (DEBUG_OUT)
				System.out.println(time + ": Agent " + me.name + " has no necessity to visit the station " + station.name + " and evaluated it to be -1");
			return -1;
		}
		//elsewise (the target has necessity), if there is a necessity left at the current station type (which you can NOT return to)
		if (anyCurrentNecessity && !AgentPWQPlusQueue_Pathing.canReachTransitive(me, null, lastStation.get(me).type, null)) {
			//then give a policy-determined score to any station of the current type
			if (lastStation.get(me).type.name.equals(station.type.name)) {
				if (DEBUG_OUT)
					System.out.println(time + ": Agent " + me.name + " can not return and evaluated station of current type to be " + policyEvaluation);
				return policyEvaluation;
			}
			//and a -1 score to every other station (since you can not return)
			else {
				if (DEBUG_OUT)
					System.out.println(time + ": Agent " + me.name + " can not return and evaluated station of distant type to be -1");
				return -1;
			}
		}
		//if there is no necessity left at the current type OR you can return later, select something according to the policy

		//this is down here to allow selection of the current station (which might otherwise be "unreachable" transitively)
		//if you can not reach the target station, give it a -1 value
		if (ALLOW_TRANSITIVE_SELECT) {
			if (!AgentPWQPlusQueue_Pathing.canReachTransitiveWithoutSkip(me, null, station.type)) {
				if (DEBUG_OUT)
					System.out.println(time + ": Agent " + me.name + " can not reach station " + station.name + " transitively and evaluated it to be -1");
				return -1;
			}
		}
		else if (!AgentPWQPlusQueue_Pathing.canReach(me, station)) {
			if (DEBUG_OUT)
				System.out.println(time + ": Agent " + me.name + " can not reach station " + station.name + " and evaluated it to be -1");
			return -1;
		}
		//else you can reach it and can give it a score

		//if random exploration is enforced (e.g. to let policy learn from random behaviour)
		if (EPSILON_GREEDY_EXPLORATION) {
			boolean rnd = randomAction() == 1;
			if (DEBUG_OUT) {
				if (rnd)
					System.out.println(time + ": Agent " + me.name + " chose random action for this time step");
				else
					System.out.println(time + ": Agent " + me.name + " chose Q value for this time step: " + station.name + "(" + policyEvaluation + ")");
			}
			return rnd ? Math.random() : policyEvaluation;
		}
		//otherwise, just follow the given agent policy
		return policyEvaluation;
	}

	/**
	 * This method allows an agent to communicate with other agents by
	 * returning a communication data object.
	 * 
	 * @param me          the agent itself
	 * @param others      all other agents in the scenario with their
	 *                    currently communicated information
	 * @param stations    all stations in the scenario
	 * @param time        the current time unit
	 * @param defaultData a triple (selected station, time unit when the
	 *                    station is reached, evaluation value of the station)
	 *                    that can be used for default communication
	 * 
	 * @return the communication data object
	 */
	public static Object communication(Agent me, HashMap<Agent, Object> others, List<Station> stations, long time, Object[] defaultData) {
		if (DEBUG_OUT) {
			System.out.println("Agent " + me.name + " communicates: Last located at " + me.previousTarget.name + " selected Station " + ((Station)defaultData[0]).name + " with score " + (Double)defaultData[2]);
			System.out.println(time + ": Agent " + me.name + " is visiting " + me.visiting + " and has target " + (me.target != null ? me.target.name : "null"));
		}
		return getAgentCommunication(me, defaultData);
	}

	/**
	 * This method allows an agent to perceive the local reward for its most
	 * recent action.
	 * 
	 * @param me       the agent itself
	 * @param others   all other agents in the scenario with their
	 *                 currently communicated information
	 * @param stations all stations in the scenario
	 * @param time     the current time unit
	 * @param value    the local reward in [0, 1] for the agent's most
	 *                 recent action
	 */
	public static void reward(Agent me, HashMap<Agent, Object> others, List<Station> stations, long time, double value) {
		if (DEBUG_OUT)
			System.out.println(time + ": Agent " + me.name + " is adapting a Q value for source " + lastStation.get(me).name + " and target " + (me.target != null ? me.target : me.previousTarget) + " with reward " + value);
		adaptAgent(me, others, me.target != null ? me.target : me.previousTarget, value, time);
	}
}

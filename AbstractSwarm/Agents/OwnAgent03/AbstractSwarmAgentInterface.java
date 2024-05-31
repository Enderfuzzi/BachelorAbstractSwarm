/*
AbstractSwarm agent that makes random decisions
Copyright (C) 2020  Daan Apeldoorn (daan.apeldoorn@uni-mainz.de)

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/


import java.io.BufferedWriter;
import java.io.BufferedReader;

import java.io.FileWriter;
import java.io.FileReader;

import java.io.IOException;

import java.lang.NumberFormatException;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Date;

import java.text.SimpleDateFormat;


/**
 * This class provides the three methods of the AbstractSwarm agent interface 
 * for state perception and performing actions, communication with other agents
 * and the perception of rewards.<br />
 * <br />
 * 
 * The properties of the interface's objects are:<br />
 * <br />
 * 
 * AGENT:<br />
 * .name           the agent's name<br />
 * .type           the agent's AGENT_TYPE (see below)<br />
 * .frequency      the number of remaining visits; -1 if the agent's type has
 *                 no frequency attribute<br />
 * .necessities    the number of remaining visits for each connected station,
 *                 -1 if the agent's type has no necessity attribute<br />          
 * .cycles         the number of remaining cycles for each incoming visit edge;
 *                 -1 if agent's type has no cycle attribute<br />
 * .time           the remaining time on the target station, -1 if agent is
 *                 currently not visiting a station or if agent's type has no
 *                 time attribute<br />
 * .target         the agent's current target<br />
 * .visiting       whether the agent is currently visiting a station<br />
 * <br />
 * 
 * STATION:<br />
 * .name           the station's name<br />
 * .type           the station's STATION_TYPE (see below)<br />
 * .frequency      the number of remaining visits; -1 if the station's type
 *                 has no frequency attribute<br />
 * .necessities    the number of remaining visits for each connected agent,
 *                 -1 if the station's type has no necessity attribute<br />          
 * .cycles         the number of remaining cycles for each incoming visit edge;
 *                 -1 if station's type has no cycle attribute<br />
 * .space          the remaining space, -1 if the station's type has no space
 *                 attribute<br />
 * <br />
 * 
 * AGENT_TYPE:<br />
 * .name           the agent type's name as string<br />
 * .type           the type as string ("AGENT_TYPE")<br />
 * .components     the agent type's AGENTs (see above)<br />
 * .frequency      the agent type's frequency attribute; -1 if the agent type 
 *                 has no frequency attribute<br />
 * .necessity      the agent type's necessity attribute; -1 if the agent type 
 *                 has no necessity attribute<br />          
 * .cycle          the agent type's cycle attribute; -1 if agent type has no
 *                 cycle attribute<br />
 * .time           the agent type's time attribute, -1 if agent type has no
 *                 time attribute<br />
 * .size           the agent type's size attribute, -1 if agent type has no
 *                 size attribute<br />
 * .priority       the agent type's priority attribute, -1 if agent type has
 *                 no priority attribute<br />
 * .visitEdges     the agent type's VISIT_EDGEs as list (see below)<br />
 * .timeEdges      the agent type's TIME_EDGEs as list (see below)<br />
 * .placeEdges     the agent type's PLACE_EDGEs as list (see below)
 *                 (note that, by definition, agent types cannot have place
 *                 edges and thus this list will always be empty; this is only
 *                 for backwards compatibility/unification reasons)<br />
 * <br />
 *
 * STATION_TYPE:<br />
 * .name           the station type's name as string<br />
 * .type           the type as string ("STATION_TYPE")<br />
 * .components     the station type's stations<br />
 * .frequency      the station type's frequency attribute; -1 if the station
 *                 type has no frequency attribute<br />
 * .necessity      the station type's necessity attribute; -1 if the station
 *                 type has no necessity attribute<br />          
 * .cycle          the station type's cycle attribute; -1 if station type has
 *                 no cycle attribute<br />
 * .time           the station type's time attribute, -1 if station type has no
 *                 time attribute<br />
 * .space          the station type's space attribute, -1 if station type has
 *                 no space attribute<br />
 * .visitEdges     the station type's VISIT_EDGEs as list (see below)<br />
 * .timeEdges      the station type's TIME_EDGEs as list (see below)<br />
 * .placeEdges     the station type's PLACE_EDGEs as list (see below)<br />
 * <br />
 *
 * VISIT EDGE:<br />
 * .type           the edge's type as string ("VISIT_EDGE")<br />
 * .connectedType  the opposite component type connected by the edge<br />
 * .bold           whether the edge is bold<br />
 * <br />
 * 
 * TIME EDGE:<br />
 * .type           the edge's type as string ("TIME_EDGE")<br />
 * .connectedType  the opposite component type connected by the edge<br />
 * .incoming       whether the edge is incoming<br />
 * .outgoing       whether the edge is outgoing<br />
 * .andConnected   whether the edge is and-connected to the opposite type<br />
 * .andOrigin      whether the edge is and-connected at its origin type<br />
 * <br />
 * 
 * PLACE EDGE:<br />
 * .type         the edge's type as string ("PLACE_EDGE")<br />
 * .connectedType  the opposite component type connected by the edge<br />
 * .incoming       whether the edge is incoming<br />
 * .outgoing       whether the edge is outgoing<br />
 */
public class AbstractSwarmAgentInterface 
{
	/** The random generator used for random decisions. */
	private static Random random = new Random();

	private final static String INITIAL_STATS = "initial_stats.txt";
	private final static String BEST_STATS = "best_stats.txt";
	private final static String OPTIMIZED_PARAMETERS = "optimized_parameters.txt";
	private final static String SAVED_PARAMETERS = "saved_parameters.txt";
	private final static String LOG_DIRECTORY = String.format("Log%s", System.getProperty("file.separator"));
	private final static String LOG_STATS = String.format("%s%s.txt", LOG_DIRECTORY, generateTimeStamp());

	private static boolean firstCall = true;
	

	private static long last_number = 0;
	
	private static long round_time_unit = 0;
	
	private static HashMap<Parameter, Cell> currentCells = new HashMap<>();
	
	private static long lowestTimeUnit = Long.MAX_VALUE;
	//private static HashMap<Parameter, Cell> bestCells = new HashMap<>();
	
	private static List<Long> listOfTimeValues = new ArrayList<>();
	
	private static boolean lastRunCompleted = false;
	private static int numberOfRuns = 0;
	private static int numberOfCompletedRuns = 0;
	private static long runsSinceCurrentBest = 0L;
	
	private static List<VisitEdge> usedBoldEdges = new ArrayList<>();
	
	
	/**
	 * This method allows an agent to perceive its current state and to perform
	 * actions by returning an evaluation value for potential next target
	 * stations. The method is called for every station that can be visted by
	 * the agent. 
	 * 
	 * @param me        the agent itself
	 * @param others    all other agents in the scenario with their currently
	 *                  communicated information
	 * @param stations  all stations in the scenario
	 * @param time      the current time unit
	 * @param station   the station to be evaluated as potential next target
	 * 
	 * @return          the evaluation value of the station
	 */
	public static double evaluation(Agent me, HashMap<Agent, Object> others, List<Station> stations, long time, Station station )
	{
		
		if (firstCall) {
			firstCall = false;
			loadCells();
			
			if (!graphHasTimeEdges(stations)) {
				currentCells.get(Parameter.STATION_IS_TIME_CONNECTED).disableCell();
				currentCells.get(Parameter.STATION_IS_TIME_CONNECTED).setChanceForActivation(0.0);
			}
			
			if (!graphHasBoldVisitEdge(stations)) {
				currentCells.get(Parameter.BOLD_VISIT_EDGE).disableCell();
				currentCells.get(Parameter.BOLD_VISIT_EDGE).setChanceForActivation(0.0);
			}
			
			
		}
		
		if (time == 1 && last_number != 1) {
			++numberOfRuns;
			++runsSinceCurrentBest;
			if (lastRunCompleted) ++numberOfCompletedRuns;
		
			
			if (numberOfRuns == 100) {
				saveBestParameters();
			}
			
			
			if (lastRunCompleted && round_time_unit < lowestTimeUnit) {
				for (Cell cell : currentCells.values()) {
					cell.saveStatus();
					if (cell.wasUsed()) {
						cell.save();
						if (cell.isEnabled()) {
							if (cell.getChanceForActivation() <= 0.3) {
								cell.setChanceForActivation(cell.getChanceForActivation() * 2);
							} else {
								cell.setChanceForActivation(cell.getChanceForActivation() + cell.getChanceForActivation() * 0.2);
							}
						} else {
							if (cell.getChanceForActivation() >= 0.7) {
								cell.setChanceForActivation(cell.getChanceForActivation() / 2);
							} else {
								cell.setChanceForActivation(cell.getChanceForActivation() - cell.getChanceForActivation() * 0.2);
							}
						}
					}
					cell.resetMutateFaktor();
				}
				
				if (round_time_unit > 0) lowestTimeUnit = round_time_unit;
				saveCells();
				runsSinceCurrentBest = 0L;
			}
			
			if (runsSinceCurrentBest != 0L && runsSinceCurrentBest % 5 == 0) {
				for (Cell cell : currentCells.values()) {
					cell.manipulateMutateFaktor(0.3);
				}
			}
			
			
			printStatus();
			
			for (Cell cell : currentCells.values()) {
				cell.computeCellActivity();
				cell.reset();
				if (!cell.wasUsed()) continue;
				if (!cell.isEnabled()) continue;
				cell.mutate();
				cell.resetUseage();
			}
			
			round_time_unit = 0;
		}
		last_number = time;
		lastRunCompleted = possibleLastRun(me, new ArrayList<Agent>(others.keySet()), stations);
		if (round_time_unit < time) round_time_unit = time;
		/*
		if (random.nextDouble() > 0.3) currentCells.get(Parameter.STATION_IS_NEIGHBOUR).disableCell();
		else currentCells.get(Parameter.STATION_IS_NEIGHBOUR).enableCell();
		
		
		if (random.nextDouble() > 0.5) currentCells.get(Parameter.AGENT_WORK_TIME_LEFT).disableCell();
		else currentCells.get(Parameter.AGENT_WORK_TIME_LEFT).enableCell();
		
		if (random.nextDouble() > 0.5) currentCells.get(Parameter.AGENT_DISTRIBUTION).disableCell();
		else currentCells.get(Parameter.AGENT_DISTRIBUTION).enableCell();
		//TODO Distribution factor
		*/
		
		double result = 0.0;
		
		result += currentCells.get(Parameter.RANDOM).evaluate(random.nextDouble());
		
		result += currentCells.get(Parameter.AGENT_FREQUENCY).evaluate(me.frequency);

		if (currentCells.get(Parameter.AGENT_DISTANCE_TO_STATION).isEnabled()) {
			int pathCost = Integer.MAX_VALUE;
			//System.out.println(String.format("Time: %d Agent: %s with previous Target: %s",time, me.name, me.previousTarget.name));
			
			//if (time != 1L) {
				pathCost = pathCost(me.previousTarget.type, station.type);
				
			//} else {
				if (time == 1L) {
					for (VisitEdge edge : me.type.visitEdges) {
						if (!edge.bold) continue;
						int tmpValue = pathCost((StationType) edge.connectedType, station.type);
						if (tmpValue < pathCost) pathCost = tmpValue;
					}
				}
				
			
				/*
				if (currentCells.get(Parameter.STATION_IS_START).isEnabled()) {
					if (minimumSpanningTree(me).contains(station.type)) {
						result += currentCells.get(Parameter.STATION_IS_START).evaluate(0.25);
					}
				}
				*/
			//}
			//System.out.println(String.format("Agent %s to station %s has cost: %d", me.name, station.name, pathCost));
			if (pathCost != Integer.MAX_VALUE) {
				result += currentCells.get(Parameter.AGENT_DISTANCE_TO_STATION).evaluate(pathCost);
			}
		}
		
		
		
		
		result += currentCells.get(Parameter.STATION_SPACE).evaluate(station.space);
		result += currentCells.get(Parameter.STATION_FREQUENCY).evaluate(station.frequency);
		
		if (numberOfCompletedRuns >= 30) {
			List<Object[]> filteredCommunications = getCommunicationOfStation(others, station);
			if (filteredCommunications.size() > 0) {
				Object[] highestStationCommunication = filteredCommunications.get(0);
				for (Object[] communication : filteredCommunications) {
					if (((Double) communication[2]) > ((Double) highestStationCommunication[2])) {
						highestStationCommunication = communication;
					}
				}
				
				result += currentCells.get(Parameter.OTHER_AGENT_SELECTED_STATION).evaluate(filteredCommunications.size());

				if (me.previousTarget != null && (long) highestStationCommunication[1] > (long) (time + pathCost(me.previousTarget.type, station.type))) {
					result += currentCells.get(Parameter.OTHER_AGENT_VALUE_OF_STATION).evaluate((Double) highestStationCommunication[2]);
				}
				
				
				
				
			}
		}
		

		
		result += currentCells.get(Parameter.STATION_TYPE_TIME).evaluate(station.type.time);
		
		if (numberOfCompletedRuns >= 500) {
			result += currentCells.get(Parameter.STATION_TYPE_COMPONENTS).evaluate(station.type.components.size());
			//result += currentCells.get(Parameter.STATION_TYPE_TIME).evaluate(station.type.time);
			//result += currentCells.get(Parameter.STATION_TYPE_SPACE).evaluate(station.type.space);
		}
		
		if (isNearestStation(me, station)) {
			result += currentCells.get(Parameter.STATION_IS_NEAREST).evaluate(0.5);
		}
		
		if (currentCells.get(Parameter.STATION_IS_TIME_CONNECTED).isEnabled()) {
			List<Station> s = getUndirectedTimeConnectedStations(station);
			for (Object ob : others.values()) {
				if (ob == null) continue;
				Object[] o = (Object[]) ob;
				if (s.contains((Station) o[0])) {
					result += currentCells.get(Parameter.STATION_IS_TIME_CONNECTED).evaluate((Double) o[2]);
				}
			}
		}
		
		if (currentCells.get(Parameter.BOLD_VISIT_EDGE).isEnabled()) {
			if (isBoldEdge(me, station)) {
				result += currentCells.get(Parameter.BOLD_VISIT_EDGE).evaluate(2);
			}
		}
		

		if (isNeighbourStation(me, station)) {
			result += currentCells.get(Parameter.STATION_IS_NEIGHBOUR).evaluate(0.5);
		}
		

		
		if (currentCells.get(Parameter.AGENT_WORK_TIME_LEFT).isEnabled()) {
			result += currentCells.get(Parameter.AGENT_WORK_TIME_LEFT).evaluate(estimatedWorkTimeLeft(me));
		}
	
		if (currentCells.get(Parameter.AGENT_DISTRIBUTION).isEnabled()) {
			result += currentCells.get(Parameter.AGENT_DISTRIBUTION).evaluate(agentDistribution(me, others, time, station));
		}
		
		
		// if station has undirected time edge and is target = pref this station
		
		//int remainingVisits = remainingVisitsOfAStationType(me, me.previousTarget.type);
		//if (remainingVisits > 0 && station.type == me.previousTarget.type) return 0.0;
		
		
		//System.out.println(String.format("Time: %d Current Station: %s Agent: %s Value %f",time, station.name, me.name, result));
		
		return result;
	}
	
	
	/**
	 * This method allows an agent to communicate with other agents by
	 * returning a communication data object.
	 * 
	 * @param me           the agent itself
	 * @param others       all other agents in the scenario with their
	 *                     currently communicated information
	 * @param stations     all stations in the scenario
	 * @param time         the current time unit
	 * @param defaultData  a triple (selected station, time unit when the 
	 *                     station is reached, evaluation value of the station)
	 *                     that can be used for default communication
	 * 
	 * @return             the communication data object
	 */
	public static Object communication( Agent me, HashMap<Agent, Object> others, List<Station> stations, long time, Object[] defaultData )
	{
		if (round_time_unit < time) round_time_unit = time;
		return defaultData;
	}

	
	/**
	 * This method allows an agent to perceive the local reward for its most
	 * recent action.
	 * 
	 * @param me           the agent itself
	 * @param others       all other agents in the scenario with their
	 *                     currently communicated information
	 * @param stations     all stations in the scenario
	 * @param time         the current time unit
	 * @param value        the local reward in [0, 1] for the agent's most
	 *                     recent action 
	 */
	public static void reward( Agent me, HashMap<Agent, Object> others, List<Station> stations, long time, double value )
	{
		if (round_time_unit < time) round_time_unit = time;
		//System.out.println(String.format("[Reward] Agent: %s Time: %d Value: %f", me.name, time, value));
	}

	

	private static void loadCells() {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(INITIAL_STATS));
			String line;
			while ((line = reader.readLine()) != null) {
				String[] splited = line.split(":");
				if (splited.length == 2) {
					Parameter parameter = Parameter.getParameter(splited[0]);
					if (parameter.isDefault()) continue;
					double cellWeight = 0.0;
					try {
						cellWeight = Double.parseDouble(splited[1]);
					} catch (NumberFormatException e) {
						//TODO add format exception handling
					}
					currentCells.put(parameter, new Cell(splited[0], cellWeight, parameter.getActivityScore()));
				}
			}
			reader.close();
		} catch (IOException e) {
			
		}
		
		for (Parameter parameter : Parameter.values()) {
			if (!currentCells.containsKey(parameter)) {
				currentCells.put(parameter, new Cell(parameter.getRepresentation(), parameter.getDefaultValue(), parameter.getActivityScore()));
			}
		}
		
	}

	private static void saveCells() {
		saveLog();
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(BEST_STATS));
			for (Cell cell : currentCells.values()) {
				writer.write(cell.fileRepresentation());
				writer.newLine();
			}
			writer.flush();
			writer.close();
		} catch (IOException e) {
			
		}
	}
	
	
	private static void saveLog() {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_STATS, true));
			
			
			writer.write(String.format("Round Time: %d | Round Number: %d | Completed Rounds: %d", lowestTimeUnit, numberOfRuns, numberOfCompletedRuns));
			writer.newLine();
			writer.write("Cell statistics:");
			writer.newLine();
			for (Cell cell : currentCells.values()) {
				writer.write(cell.fileRepresentation());
				writer.newLine();
			}
			writer.write("-----------------------------------------");
			writer.newLine();
			writer.flush();
			writer.close();
		} catch (IOException e) {
			System.out.println("Error in writing log file");
			e.printStackTrace();
		}
	}
	
	
	private static void saveBestParameters() {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(SAVED_PARAMETERS));
			
			List<String> result = new ArrayList<String>();
			String line;
			

			List<String> parameterResult = new ArrayList<String>();
			

			
			while ((line = reader.readLine()) != null) {
				double avgValue = 0.0;
				
				StringBuilder savedParameters = new StringBuilder();
				StringBuilder builder = new StringBuilder();
				
				String[] splited = line.split(";");
				Parameter parameter = Parameter.getParameter(splited[0]);
				
				if (parameter.isDefault()) continue;
				
				/*
				builder.append("|Cell: ");
				builder.append(parameter.getRepresentation());
				*/
				
				double value = 0.0;
				for (int i = 0;i < splited.length; i++) {
					savedParameters.append(splited[i]);
					savedParameters.append(";");
					
					try {
						if (i != 0) avgValue += Double.parseDouble(splited[i]);
					} catch (NumberFormatException e) {
						//TODO add format exception handling
					}
				}
				
				int numberOfParameters = 0;
				
				if (currentCells.get(parameter).isEnabled()) {
					savedParameters.append(currentCells.get(parameter).getBestWeight());
					
					avgValue += currentCells.get(parameter).getBestWeight();
					numberOfParameters = splited.length;	
				} else {
					numberOfParameters = splited.length - 1;
				}
				
				avgValue /= numberOfParameters;
				
				builder.append(String.format(Locale.US,"|Cell: %s\n|Used Parameters: %d\n|Avg Value: %f\n", parameter.getRepresentation(), numberOfParameters, avgValue));
				/*
				builder.append("\n|Used Paarameters: ");
				builder.append(numberOfParameters);
				builder.append("\n|Avg Value: ");
				builder.append(avgValue);
				builder.append("\n");
				*/
				result.add(builder.toString());
				parameterResult.add(savedParameters.toString());
			}
			
			reader.close();
			
			BufferedWriter writer = new BufferedWriter(new FileWriter(SAVED_PARAMETERS));
			
			for (String writeLine : parameterResult) {
				writer.write(writeLine);
				writer.newLine();
				writer.newLine();
			}
			
			writer.flush();
			writer.close();
			
			writer = new BufferedWriter(new FileWriter(OPTIMIZED_PARAMETERS));
			
			for (String writeLine : result) {
				writer.write(writeLine);
				writer.newLine();
			}
			
			writer.flush();
			writer.close();
			
			
			
			
				/*
				
				StringBuilder builder = new StringBuilder();
				builder.append(parameter.getRepresentation());
				builder.append(";");
				
				if (!currentCells.get(parameter).isBestStatusEnabled()) {
					for (int i = 1; i < splited.length; i++) {
						builder.append(splited[i]);
						if (i != splited.length - 1) builder.append(";");
					}
					result.add(builder.toString());
					continue;
				}
				
				
				int used = 0;
				try {
					used = Integer.parseInt(splited[1]);
				} catch (NumberFormatException e) {
					//TODO add format exception handling
				}
				double currentValue = 0.0;
				int numberOfValues = used;
				for (int i = 3;i < splited.length; i++) {
					try {
						currentValue += Double.parseDouble(splited[i]);
					} catch (NumberFormatException e) {
						//TODO add format exception handling
					}
				}
				
				currentValue += currentCells.get(parameter).getBestWeight();
				System.out.println("Current Value: " + currentValue);
				currentValue /= ++numberOfValues;
				builder.append(numberOfValues);
				builder.append(";");
				builder.append(currentValue);
				builder.append(";");
				for (int i = 3; i < splited.length; i++) {
					builder.append(splited[i]);
					builder.append(";");
				}
				builder.append(currentCells.get(parameter).getBestWeight());
				result.add(builder.toString());
				
			}
			reader.close();
			
			BufferedWriter writer = new BufferedWriter(new FileWriter(OPTIMIZED_PARAMETERS));
			
			for (String writeLine : result) {
				writer.write(writeLine);
				writer.newLine();
			}
			
			writer.flush();
			writer.close();
			*/
		} catch (IOException e) {
			System.out.print("Error on writing optimized parameters");
			
			
			
			
		}
	}
	
	
	
	private static boolean possibleLastRun(Agent me, List<Agent> others, List<Station> stations) {
		boolean tmp = false;
		for (Station station : stations) {
			if (station.frequency >= 2) return false;
			if (station.frequency == 1) {
				if (tmp) return false;
				tmp = true;
			}
		}
		
		others.add(me);
		for (Agent agent : others) {
			if (agent.frequency > 1) return false;
			for (Map.Entry<Station, Integer> entry : agent.necessities.entrySet()) {
				if (entry.getValue() > 1) return false;
			}
			
			
		}
		
		
		return true;
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
		return 0;
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
	
	/*
	private static VisitEdge getBoldEdge(Agent me, Station station) {
		for (VisitEdge edge : station.type.visitEdges) {
			if (edge.bold && (AgentType) edge.connectedType == me.type) return edge;
		}
		return null;
	}
	*/
	
	
	private static List<Station> getUndirectedTimeConnectedStations(Station station) {
		List<Station> result = new ArrayList<>();
		for (TimeEdge edge : station.type.timeEdges) {
			if (edge.weight == 0) {
				if (edge.connectedType instanceof StationType stationType) {
						result.addAll(stationType.components);
				}
			}
		}
		return result;
	}
	
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
			if (entry.getKey().type.time == -1) {
				result += entry.getValue();
			} else {
				result += entry.getValue() * entry.getKey().type.time;
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
	
	
	
	private static String generateTimeStamp() {
		return new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
	}
	
	private static void printStatus() {
		System.out.println("#################################################################################################################################");
		System.out.println(String.format("Last run time unit: %d | Last run completed: %b | Current best time unit: %d", round_time_unit, lastRunCompleted, lowestTimeUnit));
		System.out.println(String.format("Number of runs: %d | Number of completed runs: %d | Runs since best: %d",numberOfRuns, numberOfCompletedRuns, runsSinceCurrentBest));
		System.out.println("Cells for this round: ");
		for (Cell cell : currentCells.values()) {
			System.out.println(cell);
		}
	}
}


class Cell {
	private static Random generator = new Random();
	
	private double initialMutateFaktor = 0.2;
	private double mutateFaktor = initialMutateFaktor;
	
	
	private double weight;
	private double initialWeight;
	private String key;
	private boolean used;
	
	private long numberOfRuns = 0L;
	
	public enum Status {
		ENABLED,
		DISABLED;
	}
	
	private Status status;
	private Status bestRunStatus;
	
	//value between 0 and 1
	private double chanceForActivation;
	
	
	public Cell(String key, double weight, double chanceForActivation) {
		this.key = key;
		this.weight = weight;
		this.initialWeight = weight;
		this.chanceForActivation = chanceForActivation;
		this.used = false;
		this.status = Cell.Status.ENABLED;
		this.bestRunStatus = Cell.Status.ENABLED;
	}

	public double evaluate(double input) {
		if (status == Cell.Status.DISABLED) return 0.0;
		
		used = true;
		if (input < 0) input = 1.0;
		return weight * input;
	}

	public void mutate() {
		weight += weight * (generator.nextDouble(mutateFaktor * 2) - mutateFaktor);
	}

	public void reset() {
		this.weight = initialWeight;
		increaseNumberOfRuns();
	}
	
	public void save() {
		this.initialWeight = weight;
	}
	
	public String fileRepresentation() {
		return String.format(Locale.US,"%s:%f",key, weight);
	}

	public String getKey() {
		return key;
	}
	
	public boolean wasUsed() {	
		return used;
	}
	
	public void resetUseage() {
		used = false;
	}

	public double getBestWeight() {
		return initialWeight;
	}
	
	public double getCurrentWeight() {
		return weight;
	}
	
	public double getCurrentMutateFaktor() {
		return mutateFaktor;
	}
	
	public double getInitialMutateFaktor() {
		return initialMutateFaktor;
	}
	
	public void manipulateMutateFaktor(double faktor) {
		this.mutateFaktor += this.initialMutateFaktor * faktor;
	}
	
	public void resetMutateFaktor() {
		this.mutateFaktor = this.initialMutateFaktor;
	}
	
	private void increaseNumberOfRuns() {
		//numberOfRuns++;
		//if (numberOfRuns % 10 == 0) mutateFaktor *= 0.8;
	}
	
	public Status getStatus() {
		return this.status;
	}
	
	public void enableCell() {
		this.status = Cell.Status.ENABLED;
	}
	
	public void disableCell() {
		this.status = Cell.Status.DISABLED;
	}
	
	public boolean isEnabled() {
		return this.status == Cell.Status.ENABLED;
	}
	
	public void saveStatus() {
		this.bestRunStatus = this.status;
	}
	
	public boolean isBestStatusEnabled() {
		return this.bestRunStatus == Cell.Status.ENABLED;
	}
	
	public void computeCellActivity() {
		if (this.chanceForActivation != 0.0 && (this.chanceForActivation == 1.0 || generator.nextDouble() < this.chanceForActivation)) {
			this.enableCell();
		} else {
			this.disableCell();
		}
	}
	
	public double getChanceForActivation() {
		return this.chanceForActivation;
	}
	
	public void setChanceForActivation(double value) {
		if (value > 1.0) {
			this.chanceForActivation = 1.0;
		} else if(value < 0.0) {
			this.chanceForActivation = 0.0;
		} else {
			this.chanceForActivation = value;
		}
	}
		
	
	
	@Override
	public String toString() {
		return String.format(Locale.US,"[%s: %s Current weight: %f Best weight: %f | Mutate Faktor: %f | Status: %s | Chance for activation: %f]", 
				this.getClass().getSimpleName(), key, weight, initialWeight, mutateFaktor, status.name(), chanceForActivation);
	}
}

enum Parameter {

	NONE("") {
		@Override
		public boolean isDefault() {
			return true;
		}
	},
	
	BOLD_VISIT_EDGE("bold_visit_edge", 1.5),
	
	AGENT_FREQUENCY("agent_frequency"),
	AGENT_TIME("agent_time"),
	AGENT_TARGET("agent_target"),
	AGENT_VISITING("agent_visiting"),
	AGENT_WORK_TIME_LEFT("agent_work_time_left", 0.5, 0.5),
	AGENT_PRIORITY("agent_priority"),
	AGENT_DISTRIBUTION("agent_distribution",-4.5, 0.5),
	
	AGENT_DISTANCE_TO_STATION("agent_distance_to_station", -0.25),
	
	OTHER_AGENT_SELECTED_STATION("other_agent_selected_station"),
	OTHER_AGENT_TIME_TO_ARRIVAL("other_agent_time_to_arrival"),
	OTHER_AGENT_VALUE_OF_STATION("other_agent_value_of_station"),
	
	RANDOM("random"),
	
	STATION_IS_START("station_is_start", 0.5),
	
	STATION_IS_NEIGHBOUR("station_is_neighbour", 0.5, 0.7),
	STATION_IS_NEAREST("station_is_nearest"),
	STATION_IS_TIME_CONNECTED("station_is_time_connected"),
	
	STATION_CAPACITY("station_capacity"),
	STATION_SPACE("station_space"),
	STATION_FREQUENCY("station_frequency"),

	STATION_TYPE_COMPONENTS("station_type_components"),
	STATION_TYPE_FREQUENCY("station_type_frequency"),
	STATION_TYPE_NECESSITY("station_type_necessity"),
	STATION_TYPE_TIME("station_type_time", -0.05),
	STATION_TYPE_SPACE("station_type_space"),

	;

	private final static Parameter[] parameters = Parameter.values();

	private final String representation;

	private final double defaultValue;
	
	private final double activityScore;
	
	private Parameter(String representation) {
		this(representation, 0.5);
	}
	
	private Parameter(String representation, double defaultValue) {
		this(representation, defaultValue, 1.0);
	}
	
	private Parameter(String representation, double defaultValue, double activityScore) {
		this.representation = representation;
		this.defaultValue = defaultValue;
		this.activityScore = activityScore;
	}
	

	public String getRepresentation() {
		return representation;
	}

	public static Parameter getParameter(String value) {
		for (Parameter p : parameters) {
			if (p.getRepresentation().equals(value)) {
				return p;
			}
		}
		return NONE;
	}

	public boolean isDefault() {
		return false;
	}
	
	public double getDefaultValue() {
		return defaultValue;
	}

	public double getActivityScore() {
		return activityScore;
	}
	
}


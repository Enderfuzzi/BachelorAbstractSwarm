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
			//bestCells = new HashMap<>(currentCells);		
		}
		
		if (time == 1 && last_number != 1) {
			++numberOfRuns;
			System.out.println(String.format("Last Run time Unit: %d", round_time_unit));
			System.out.println(String.format("Last run completed: %b", lastRunCompleted));
			if (lastRunCompleted) ++numberOfCompletedRuns;
			
			System.out.println(String.format("Number of runs: %d", numberOfRuns));
			System.out.println(String.format("Number of completed runs: %d", numberOfCompletedRuns));
			
			
			if (lastRunCompleted && round_time_unit < lowestTimeUnit) {
					//bestCells = new HashMap<Parameter, Cell>(currentCells);
				for (Cell cell : currentCells.values()) {
					if (cell.wasUsed()) cell.save();
				}
				
				if (round_time_unit > 0) lowestTimeUnit = round_time_unit;
					//listOfTimeValues.add(round_time_unit);
				saveCells();
			}
			
			if (lastRunCompleted && round_time_unit * 1.5 > lowestTimeUnit) {
				for (Cell cell : currentCells.values()) {
					cell.manipulateMutationFaktor(cell.getBestWeight() - cell.getCurrentWeight());
				}
			}
			
			
			System.out.println(String.format("Current minimum time unit: %d", lowestTimeUnit));
			
			//currentCells = new HashMap<Parameter, Cell>(bestCells);
			System.out.println("Cells for this round: ");
			for (Cell cell : currentCells.values()) {
				System.out.println(cell);
				if (!cell.wasUsed()) continue;
				cell.reset();
				cell.mutate();
				cell.resetUseage();
			}
			
			round_time_unit = 0;
		}
		last_number = time;
		lastRunCompleted = possibleLastRun(stations);
		if (round_time_unit < time) round_time_unit = time;
		
		
		double result = 0.0;
		
		result += currentCells.get(Parameter.RANDOM).evaluate(random.nextDouble());
		
		result += currentCells.get(Parameter.AGENT_FREQUENCY).evaluate(me.frequency);

		if (me.previousTarget != null) {
			int pathCost = pathCost(me.previousTarget.type, station.type);
			result += currentCells.get(Parameter.AGENT_DISTANCE_TO_STATION).evaluate(pathCost);
			
		}
		
		
		result += currentCells.get(Parameter.STATION_SPACE).evaluate(station.space);
		result += currentCells.get(Parameter.STATION_FREQUENCY).evaluate(station.frequency);
		
		if (numberOfCompletedRuns >= 50) {
			List<Object[]> filteredCommunications = getCommunicationOfStation(others, station);
			if (filteredCommunications.size() > 0) {
				Object[] highestStationCommunication = filteredCommunications.get(0);
				for (Object[] communication : filteredCommunications) {
					if (((Double) communication[2]) > ((Double) highestStationCommunication[2])) {
						highestStationCommunication = communication;
					}
				}
				
				result += currentCells.get(Parameter.OTHER_AGENT_SELECTED_STATION).evaluate(filteredCommunications.size());
				//result += currentCells.get(Parameter.OTHER_AGENT_TIME_TO_ARRIVAL).evaluate(((Long)highestStationCommunication[1] == 0L ? 1.0 : 1 / highestStationCommunication[1]);
				if (me.previousTarget != null && (long) highestStationCommunication[1] > (long) (time + pathCost(me.previousTarget.type, station.type))) {
					result += currentCells.get(Parameter.OTHER_AGENT_VALUE_OF_STATION).evaluate((Double) highestStationCommunication[2]);
				}
				
				
				
				
			}
		}
		

		
		
		
		if (numberOfCompletedRuns >= 500) {
			result += currentCells.get(Parameter.STATION_TYPE_COMPONENTS).evaluate(station.type.components.size());
			result += currentCells.get(Parameter.STATION_TYPE_TIME).evaluate(station.type.time);
			result += currentCells.get(Parameter.STATION_TYPE_SPACE).evaluate(station.type.space);
		}
		
		if (isNearestStation(me, station)) {
			result += currentCells.get(Parameter.STATION_IS_NEAREST).evaluate(0.5);
		}
		
		//int remainingVisits = remainingVisitsOfAStationType(me, me.previousTarget.type);
		//if (remainingVisits > 0 && station.type == me.previousTarget.type) return 0.0;
		
		
		
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
					currentCells.put(parameter, new Cell(splited[0], cellWeight));
				}
			}
			reader.close();
		} catch (IOException e) {
			
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
		System.out.println("Save Log");
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
	
	
	
	private static boolean possibleLastRun(List<Station> stations) {
		boolean tmp = false;
		for (Station station : stations) {
			if (station.frequency >= 2) return false;
			if (station.frequency == 1) {
				if (tmp) return false;
				tmp = true;
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
	
	private static String generateTimeStamp() {
		return new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
	}
	
}


class Cell {
	private static Random generator = new Random();
	private double overallMutateFaktor = 0.4;
	private double lowMutateFaktor = overallMutateFaktor;
	
	private double weight;
	private double initialWeight;
	private String key;
	private boolean used;
	
	private long numberOfRuns = 0L;
	
	public Cell(String key, double weight) {
		this.key = key;
		this.weight = weight;
		this.initialWeight = weight;
		this.used = false;
	}

	public double evaluate(double input) {
		used = true;
		if (input < 0) input = 1.0;
		return weight * input;
	}

	public void mutate() {
		weight += weight * (generator.nextDouble(overallMutateFaktor * 2) - lowMutateFaktor);
	}

	public void reset() {
		this.weight = initialWeight;
		increaseNumberOfRuns();
	}
	
	public void save() {
		this.initialWeight = weight;
		this.lowMutateFaktor = overallMutateFaktor;
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
	
	
	public double getOverallMutateFaktor() {
		return overallMutateFaktor;
	}
	
	public double getLowMutateFaktor() {
		return lowMutateFaktor;
	}
	
	public void manipulateMutationFaktor(double value) {
		lowMutateFaktor *= (1 - value);
	}
	
	private void increaseNumberOfRuns() {
		//numberOfRuns++;
		//if (numberOfRuns % 10 == 0) mutateFaktor *= 0.8;
	}
	
	@Override
	public String toString() {
		return String.format(Locale.US,"[%s: %s with current weight: %f and initial weight: %f| Mutate Faktor: %f Low Mutate Faktor: %f]", 
				this.getClass().getSimpleName(), key, weight, initialWeight, overallMutateFaktor, lowMutateFaktor);
	}
}

enum Parameter {

	NONE("") {
		@Override
		public boolean isDefault() {
			return true;
		}
	},
	
	AGENT_FREQUENCY("agent_frequency"),
	AGENT_TIME("agent_time"),
	AGENT_TARGET("agent_target"),
	AGENT_VISITING("agent_visiting"),
	
	AGENT_DISTANCE_TO_STATION("agent_distance_to_station"),
	
	OTHER_AGENT_SELECTED_STATION("other_agent_selected_station"),
	OTHER_AGENT_TIME_TO_ARRIVAL("other_agent_time_to_arrival"),
	OTHER_AGENT_VALUE_OF_STATION("other_agent_value_of_station"),
	
	RANDOM("random"),
	
	STATION_IS_NEAREST("station_is_nearest"),
	
	STATION_CAPACITY("station_capacity"),
	STATION_SPACE("station_space"),
	STATION_FREQUENCY("station_frequency"),

	STATION_TYPE_COMPONENTS("station_type_components"),
	STATION_TYPE_FREQUENCY("station_type_frequency"),
	STATION_TYPE_NECESSITY("station_type_necessity"),
	STATION_TYPE_TIME("station_type_time"),
	STATION_TYPE_SPACE("station_type_space"),

	;

	private final static Parameter[] parameters = Parameter.values();

	private final String representation;

	private Parameter(String representation) {
		this.representation = representation;
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

}


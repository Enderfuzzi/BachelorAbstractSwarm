/*
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
import java.io.FileWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.naming.InitialContext;
import javax.print.DocFlavor.BYTE_ARRAY;

import org.w3c.dom.stylesheets.LinkStyle;


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
public class AbstractSwarmAgentInterface {
	private static boolean init = false;
	
	/** The random generator used for random decisions. */
	private static Random random = new Random();
	
	private static HashMap<Agent, HashMap<StationTypeNeuron, List<StationNeuron>>> firstLevelNetwork = new HashMap<>();
	
	//private static HashMap<StationTypeNeuron, List<StationNeuron>> secondLevelNetwork = new HashMap();
	
	private static BufferedWriter writer;
	
	static {
		try {
			writer = new BufferedWriter(new FileWriter("output.txt", true));
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	
	
	private static long timeStep = 0L;
	
	private static HashMap<Long , HashMap<Agent, StationNeuron>> bestTypeValue = new HashMap<>();
	
	private static HashMap<Station, Double> highestCurrentStationValue = new HashMap<>();
	
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
		if (!init) {
			init = true;
			
			fillNetwork(me);
			for(Agent agent : others.keySet()) {
				fillNetwork(agent);
			}
		}
		try {
			writer.write("-------------------------------------------------------\n");
			writer.write("Iteration: " + time + "\n");
			writer.write("Agent: " + me.name + " Station: " + station.name + "\n");
		} catch(Exception e) {
			
		}
		
		/*
		for (Object o : others.values()) {
			if (o == null) continue;
			if (station.equals((Station) o)) return 0.3 * random.nextDouble();
		}
		*/
		if (timeStep != time) {
			timeStep = time;
			//bestTypeValue.put(time, new HashMap<Agent, StationNeuron>());
			highestCurrentStationValue.clear();
		}
		
		if (!bestTypeValue.containsKey(time)) bestTypeValue.put(time, new HashMap<>());
		
		
		for (Map.Entry<StationTypeNeuron, List<StationNeuron>> entry : firstLevelNetwork.get(me).entrySet()) {
			for (StationNeuron stationNeuron : entry.getValue()) {
				if (entry.getKey().getStationType().equals(station.type)) {
					if (!highestCurrentStationValue.containsKey(station)) highestCurrentStationValue.put(station, 0.0);
					
					double cost = 0;
					if (time > 1L) {
						if (bestTypeValue.containsKey(Long.valueOf(time - 1))) {
							if (bestTypeValue.get(Long.valueOf(time - 1)).containsKey(me)) {
								cost = pathCost(bestTypeValue.get(Long.valueOf(time - 1)).get(me).getStation().type, station.type);
							}
						} else {
							try {
								writer.write("Map contains not the right key\n");
							} catch(Exception e) {
								
							}
						}
					}
					
					
					double typeNeuron = entry.getKey().evaluate(
							1 / leftStationsOfAType(me, entry.getKey().getStationType()).size(),
							stationNeuron.lastResult(),
							cost, 
							entry.getKey().getStationType().space, 
							entry.getKey().getStationType().time
						);
					double value = 0.0;
					if (others.values().contains(station)) value = 0.2;
					
					double result = stationNeuron.evaluate(
							typeNeuron, 
							station.frequency, 
							station.type.components.size(), 
							station.space,
							//highestCurrentStationValue.get(station)
							value
						);
					
					
					if (highestCurrentStationValue.get(station) < result) highestCurrentStationValue.put(station, result);
					if (!bestTypeValue.get(time).containsKey(me)) bestTypeValue.get(time).put(me, stationNeuron);
					else if (bestTypeValue.get(time).get(me).lastResult < result) bestTypeValue.get(time).put(me, stationNeuron);
					try {
						writer.write("Type Neuron value: " + typeNeuron + " Station Neuron value: " + result + "\n");
						writer.write("Highest Station Value:  " + highestCurrentStationValue.get(station) + "of Station: " + station.name + "\n");
						writer.write("Cost Faktor: " + cost + "\n");
					} catch (Exception e) {
						
					}
					
					return result;
				}
			}	
		}
		
		/**
		for (StationTypeNeuron neuron : firstLevelNetwork.get(me)) {
			for (StationNeuron stationNeuron : secondLevelNetwork.get(neuron)) {
				if (station.equals(stationNeuron.getStation())) {
					double typeNeuron = neuron.evaluate(
							1 / leftStationsOfAType(me, neuron.getStationType()).size(),
							stationNeuron.lastResult(),
							0.0, 
							neuron.getStationType().space, 
							neuron.getStationType().time
						);
					
					double result = stationNeuron.evaluate(
							typeNeuron, 
							station.frequency, 
							station.type.components.size(), 
							station.space
						);
					try {
						writer.write("Type Neuron value: " + typeNeuron + " Station Neuron value: " + result + "\n");
					} catch (Exception e) {
						
					}
					
					return result;
				}
			}
		}	
		*/	
		return 0.2 * random.nextDouble();
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
		return defaultData[0];
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
	public static void reward(Agent me, HashMap<Agent, Object> others, List<Station> stations, long time, double value )
	{
		
	}
	
	
	
	private static List<StationType> getAllConnectedStationTypes(Agent me) {
		List<StationType> resultTypes = new ArrayList();
		for (Station station : me.necessities.keySet()) {
			if (resultTypes.contains(station.type)) continue;
			resultTypes.add(station.type);
		}
		return resultTypes;
	}
	
	private static void fillNetwork(Agent me) {
		HashMap<StationTypeNeuron, List<StationNeuron>> resultType = new HashMap<>();
		for (StationType type : getAllConnectedStationTypes(me)) {
			StationTypeNeuron neuron = new StationTypeNeuron(type);
			
			List<StationNeuron> resultStation = new ArrayList();
			for (Station station : type.components) {
				resultStation.add(new StationNeuron(station));
			}
			resultType.put(neuron, resultStation);
		}
		firstLevelNetwork.put(me, resultType);
	}
	
	private static List<Station> leftStationsOfAType(Agent me, StationType type) {
		List<Station> result = new ArrayList<>();
		for (Station station : me.necessities.keySet()) {
			if (station.type.equals(type)) result.add(station);
		}
		return result;
	}
	
	private record Pair(double weight, StationType previous) {};
	
	private static double pathCost(StationType start, StationType target) {
		double result = 0.0;
		
		if (start.equals(target)) return 0.0;
		
		ArrayDeque<StationType> queue = new ArrayDeque<>();
		queue.add(start);
		
		List<PlaceEdge> used = new ArrayList<>(); 
		
		while (queue.peek() != null) {
			StationType current = queue.poll();
			if (current.equals(target)) return result;
			for (PlaceEdge edge : current.placeEdges) {
				if (used.contains(edge)) continue;
				StationType type = (StationType) edge.connectedType;
				queue.add(type);
				result += edge.weight;
			}
			
		}
		return result;
	}
	
	
	
	
	
	
}
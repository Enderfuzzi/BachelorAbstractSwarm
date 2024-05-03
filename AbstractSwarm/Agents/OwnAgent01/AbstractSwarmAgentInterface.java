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
import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;

import javax.management.loading.PrivateClassLoader;
import javax.xml.stream.events.StartDocument;

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
public class AbstractSwarmAgentInterface 
{
	/** The random generator used for random decisions. */
	private static Random random = new Random();

	
	private static boolean initalized = false;
	
	private static BufferedWriter writer;
	
	static {
		try {
			writer = new BufferedWriter(new FileWriter("output.txt", true));
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	
	private static HashMap<Agent, List<StationType>> pathMap = new HashMap<>();
	
	private static long step = 1;
	
	private static List<Station> used = new ArrayList<>();
	
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
	public static double evaluation( Agent me, HashMap<Agent, Object> others, List<Station> stations, long time, Station station )
	{
		if (!initalized) {
			initalized = true;
			
			
			
			pathMap.put(me, path(me));
			
			for (Agent tmp : others.keySet()) {
				pathMap.put(tmp, path(tmp));
			}
			
			me.target = station;
			return 1;
			
		}
		try {
			writer.write("-----------------------------------------------------------\n");
			writer.write("Station name: " + station.type.name + "\n");
			
			writer.write("Frequency: " + station.frequency + "\n");
			writer.write("Necessity: " + station.necessities + "\n");
			
			writer.write("Agent Type size: " + me.type.size + "\n");
			writer.write("Agent Necessity: " + me.necessities + "\n");
			writer.write("Agent Frequency: " + me.frequency + "\n");
			
			writer.write("Station Type: " + station.type.getClass() + "\n");
			writer.write("Station Type: " + station.type.type + "\n");
			writer.write("Station Type Time: " + station.type.time + "\n");
			writer.write("Station Type Space: " + station.type.space + "\n");
			writer.write("Station Type Components: " + station.type.components + "\n");
			
			writer.write("Station Edge: " + station.type.placeEdges + "\n");
			
			
			for (PlaceEdge edge : station.type.placeEdges) {
				writer.write("Station place edge: " + edge + "\n");
				writer.write("Station edge connected type: " + edge.connectedType.name + "\n");
				writer.write("Station edge connected type: " + edge.connectedType.getClass().getName() + "\n");
				writer.write("Station edge weight " + edge.weight + "\n");
			}
			
			writer.write("Current Target: " + me.target + "\n");
			writer.write("Currently visting: " + me.visiting + "\n");
			writer.write("Agent time: " + me.time + "\n");
			
			writer.write("Time: " + time + "\n");
			
			writer.write("-----------------------------------------------------------\n");			
			for (Field field : PlaceEdge.class.getFields()) {
				writer.write("Field: " + field + "\n");
			}
			
			
			
			writer.write("PathMap: " + pathMap + "\n");
			
			for (Map.Entry<Agent, Object> entry : others.entrySet()) {
				writer.write("Other Target name: " + entry.getKey().name + "|" + ((Station) entry.getValue()).name + "\n");
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
		
		if (step != time) {
			step = time;
			used.clear();
		}
		
		boolean skip = false;
		
		if (pathMap.get(me) != null) {
			for (StationType current : pathMap.get(me)) {
				if (current.equals(station.type)) {
					if (!used.contains(station)) {
						used.add(station);
						return 1.0;
					}
					break;
				}
				for (Map.Entry<Station, Integer> entry : me.necessities.entrySet()) {
					if (entry.getValue() <= 0) continue;
					if (entry.getKey().type.equals(current)) {
						skip = true;
						break;
					}
				}
				if (skip) break;
				
				/*
				for (Map.Entry<Station, Integer> entry : me.necessities.entrySet()) {
					if (!entry.getKey().type.equals(current)) continue;
					if (entry.getValue() <= 0) continue;
					if (entry.getKey().equals(station)) {
						if (used.contains(station)) return random.nextDouble(0.5,0.85);
						used.add(station);
						return random.nextDouble(0.75, 1.0);
					}
				}
				*/
			}
		}
		
		return random.nextDouble() * 0.55;
		
		
		
		//return random.nextDouble();
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
	public static void reward( Agent me, HashMap<Agent, Object> others, List<Station> stations, long time, double value )
	{
		try {
			//writer = new BufferedWriter(new FileWriter("output.txt", true));
			writer.write("Reward Trigger");
			//writer.close();
		} catch (Exception e) {
			// TODO: handle exception
		}
		
	}
	
	
	private record Pair(List<StationType> stations, Integer cost) {};
	
	private static HashMap<StationType, Integer> startingPoints = new HashMap<>();
	
	
	private static List<StationType> path(Agent me) {
		
		Pair best = null;
		for (Station station : me.necessities.keySet()) {
			Pair current = findFromStart(station.type);
			if (best == null) {
				best = current;
				continue;
			}
			if (!startingPoints.containsKey(current.stations.get(0)) 
					|| startingPoints.get(current.stations.get(0)) <= current.stations.get(0).space * 3) {
				if (current.cost < best.cost) {
					best = current;
				}
			}
		}
		startingPoints.merge(best.stations.get(0), 1, Integer::sum);
		return best.stations;
		
		/*
		Station start = new ArrayList<>(me.necessities.keySet())
				.get(random.nextInt(me.necessities.keySet().size()));
		
		return findFromStart(start.type);
		*/
	}
	
	private static Pair findFromStart(StationType start) {
		List<StationType> resultList = new ArrayList<>();
		int edgeWeight = 0;
		
		ArrayDeque<StationType> queue = new ArrayDeque<>();
		queue.add(start);
		
		List<String> used = new ArrayList<>(); 
		
		while (queue.peek() != null) {
			StationType current = queue.poll();
			if (resultList.contains(current)) continue;
			resultList.add(current);
			for (PlaceEdge edge : current.placeEdges) {
				if (resultList.contains((StationType) edge.connectedType)) continue;
				StationType type = (StationType) edge.connectedType;
				queue.add(type);
				edgeWeight += edge.weight + type.components.size();
			}
			
		}
		return new Pair(resultList, Integer.valueOf(edgeWeight));
	}
}

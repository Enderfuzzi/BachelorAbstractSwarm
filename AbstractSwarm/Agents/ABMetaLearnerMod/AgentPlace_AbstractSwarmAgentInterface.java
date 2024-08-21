


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Map;


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
 * .previousTarget the agent's previous target<br />
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
public class AgentPlace_AbstractSwarmAgentInterface 
{
	/** The agent's random generator. */
	static Random random = new Random();
	static boolean first = true;
	//placeholder
	static double speed = 1; 
	static double p = Double.NEGATIVE_INFINITY;
	static Map<String, Integer> visitList = new HashMap<String, Integer>();
	static String lastCheckedAgent = null;
	static List<String> lastStations = new ArrayList<String>();
	static List<String> checkedAgents = new ArrayList<String>();
	static double p1Prob = 0;
	static double p2Prob = 0;
	static boolean finalRun = false;
	static Map<StationType, Long> timeLimit = new HashMap<StationType, Long>();
	static Map<AgentType, List<List<StationType>>> plan = new HashMap<AgentType, List<List<StationType>>>();
	
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
	public static double evaluation( Agent me, HashMap<Agent, Object> others, List<Station> stations, long time, Station station ){			
		double value = 0;
	
		
		if(plan.isEmpty()) {
			//Solution for visit, dir. and undir. placeedges (+ space/size,) speed
			// Check whether there are any incoming place edges at the connected types
			boolean hasIncomingDirPlaceEdge = false;
			for( VisitEdge visitEdge : me.type.visitEdges ){
				for( PlaceEdge placeEdge : ((StationType) visitEdge.connectedType).placeEdges ){
					if( placeEdge.incoming ){
						hasIncomingDirPlaceEdge = true;
						break;
					}
				}
			}
			
			boolean otherHasIncomingDirPlaceEdge = false;
			for(Agent other : others.keySet()) {
				for( VisitEdge visitEdge : other.type.visitEdges ){
					for( PlaceEdge placeEdge : ((StationType) visitEdge.connectedType).placeEdges ){
						if( placeEdge.incoming ){
							otherHasIncomingDirPlaceEdge = true;
							break;
						}
					}
				}
			}
		
			if( hasIncomingDirPlaceEdge ){
				createMyPlan(me);
			}
			
			if( otherHasIncomingDirPlaceEdge ){
				createBigPlan(others);
			}
	
		} 
		if(plan.get(me.type) != null && !plan.get(me.type).isEmpty()) {
			for(List<StationType> plans : plan.get(me.type)) {
				StationType first = plans.get(0);
				boolean next = false;
				
				for(StationType stationType : plans) {
					if(next && stationType == station.type) {
						//return Double.POSITIVE_INFINITY;
						value += 5.0;
						next = false;
						break;
					} else {
						next = false;
					}
					if(stationType == me.previousTarget.type ) {
						next = true;
						
						if(stationType == station.type && first == station.type){	
							return Double.POSITIVE_INFINITY;
							//value += 4.0;
						}
					}
				
				}
			}
		} else {		
			//include attributes in value
			double distance = (double) distance( me.previousTarget, station );
			// If distance is zero, current station is the best choice according to distance; check for timelimit,
			if( distance == 0){	
				value += 1.4;
			}
			//else if no path could be found, current station is the worst choice according to distance
			else if( distance == -1 ){
				value += Double.NEGATIVE_INFINITY;
			} else {
				value += 1.0/distance;
			}
			
			//consider given attributes
			if(speed != -1) {
				value += value/speed;
			}
			
			//considers nec. and freq. for stations
			if(hasAttribute(me, station, "frequency")) {
				double freq = evaluationMaxVisitsLeft(me, others, stations, time, station);
				if(freq == Double.NEGATIVE_INFINITY) {
					//System.out.println("freq: "+me.name+station.name+"  "+freq+"   "+(1.0/distance));
					return freq;
				}
				value += freq;		
			}
			
			//considers space and size for agents and stations
			if(hasAttribute(me, station, "space")) {
				double space = evaluationMaxFreeSpace(me, others, stations, time, station);
				if(space == Double.NEGATIVE_INFINITY) {
					//System.out.println("space: "+me.name+station.name+"  "+space+"   "+(1.0/distance));
					return space;
				}
				value += space;
			}
		}
		
		
		//System.out.println(me.name+station.name+value);
		return value;		
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
	public static Object communication( Agent me, HashMap<Agent, Object> others, List<Station> stations, long time, Object[] defaultData ){			
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
	public static void reward( Agent me, HashMap<Agent, Object> others, List<Station> stations, long time, double value ){
	
	}
	
	/**
	 * checks if given station needs to be visited
	*/
	public static boolean isTarget( Agent me, Station station ){
		for(VisitEdge visit : me.type.visitEdges) {
			if(visit.connectedType == station.type) {
				return true; 
			}
		}
		return false; 
	}
	
	/**
	 * checks if problem has time dependecies (timeEdges)
	*/
	public static boolean hasTimeEdges( Agent me, HashMap<Agent, Object> others, List<Station> stations){
		if(!me.type.timeEdges.isEmpty()) return false;
		for(Agent agents : others.keySet()) {
			if(!agents.type.timeEdges.isEmpty()) return true;
		}
		for(Station s : stations) {
			if(!s.type.timeEdges.isEmpty()) return true;
		}
		return false;
	}
	
	/**
	 * checks if problem has given attribute
	*/
	public static boolean hasAttribute( Agent me, Station station, String attribute){	
		if(attribute.equals("time")) if(me.type.time != -1 || station.type.time != -1) return true;
		if(attribute.equals("space") || attribute.equals("size")) if(me.type.size != -1 || station.type.space != -1) return true;
		if(attribute.equals("frequency") || attribute.equals("necessity")) if(me.type.frequency != -1 || station.type.frequency != -1 || me.type.necessity != -1 || station.type.necessity != -1) return true;
		return false;
	}
	
	/**
	 * Checks timelimits on stations and agents
	 */
	public static double evaluationTime(Agent me, Station station, Agent other) {
		if(other.time == -1) return -1;
		return other.time;
	}
	
	/**
	 * Checks timelimits on stations and agents
	 */
	public static double evaluationStay(Agent me, Station station, Agent other) {
		if(other.time == -1 && station.type.time == -1) return Double.NEGATIVE_INFINITY;
		if(station.type.time == -1) return other.time;
		if(other.time == -1) return station.type.time;
		if(other.time < station.type.time) return other.time;
		return station.type.time;
	}
	
	/**
	 * Checks timelimits on stations and agents
	 */
	public static double evaluationFutureStay(Agent me, Station station, Agent other) {
		if(other.type.time == -1 && station.type.time == -1) return Double.NEGATIVE_INFINITY;
		if(station.type.time == -1) return other.type.time;
		if(other.type.time == -1) return station.type.time;
		if(other.time < station.type.time) return other.time;
		return station.type.time;
	}
	
	/**
	 * Calculates how many visits are left for a station
	 */
	public static double evaluationMaxVisitsLeft( Agent me, HashMap<Agent, Object> others, List<Station> stations, long time, Station station ){
		double freeVisits = (double) station.frequency;
		double necessities = (double) station.necessities.get(me);
		double myDistance = distance(me.previousTarget, station);
		long arrival = time+Double.valueOf(myDistance).longValue();
		//System.out.println(me.name+station.name+"distance: "+myDistance);
		if(freeVisits == -1 && necessities == -1) return 0.0;
					
		for(Agent other : others.keySet()){
			// If other agent has communicated the current station as target; is currently at the given station
			if( (others.get( other ) != null) && ((Object[]) others.get( other ))[ 0 ] == station ){
				double stay = evaluationStay(me, station, other);
				double futureStay = evaluationFutureStay(me, station, other);
				long otherArrival = (long)((Object[]) others.get( other ))[1];
				if(futureStay == Double.NEGATIVE_INFINITY && other.visiting) return Double.NEGATIVE_INFINITY;
				//other already reched target and stays longer than distance
				if ((other.visiting && stay >= myDistance) || (otherArrival <= arrival && otherArrival+futureStay >= arrival)) {
					if(station.frequency != -1 && otherArrival < arrival || station.frequency == -1) {
						freeVisits--;
						necessities--;
					}
				}
			}
		}
		
		
		if(freeVisits == 0 || necessities == 0) return Double.NEGATIVE_INFINITY;
		if(myDistance > 0) return (freeVisits+necessities)/myDistance;
		return freeVisits+necessities;
	}
	
	/**
	 * Calculates how much space is left for in a station
	 */
	public static double evaluationMaxFreeSpace( Agent me, HashMap<Agent, Object> others, List<Station> stations, long time, Station station ){
		double freeSpace = station.space;
		double myDistance = distance(me.previousTarget, station);
		long arrival = time+Double.valueOf(myDistance).longValue();
		//System.out.println(me.name+station.name+"distance2: "+myDistance);
		if(	station.space == -1 ) return 0.0;
		
		for( Agent other : others.keySet() ){
			// If other agent has communicated the current station as target
			if( (others.get( other ) != null) && ((Object[]) others.get( other ))[ 0 ] == station ){
				double stay = evaluationStay(me, station, other);
				double futureStay = evaluationFutureStay(me, station, other);
				long otherArrival = (long)((Object[]) others.get( other ))[1];
				//System.out.println(me.name+station.name+"stay: "+stay+"futureSty: "+futureStay+"arrival: "+arrival+"otherArrival: "+otherArrival);
				if(futureStay == Double.NEGATIVE_INFINITY && other.visiting) return Double.NEGATIVE_INFINITY;
				//other already reched target and stays longer than distance
				if((other.visiting && stay >= myDistance) || (otherArrival <= arrival && otherArrival+futureStay >= arrival)) {
					if(station.frequency <= 1 && otherArrival < arrival || station.frequency == -1 || station.frequency > 1) {
						//System.out.println(me.name+station.name+"stay: "+stay+"futureSty: "+futureStay+"arrival: "+arrival+"otherArrival: "+otherArrival);
						if( other.type.size != -1 ){
							// If other agent has a limited size, else remaining free space will be completely covered
							freeSpace -= other.type.size;
						} else{
							freeSpace = 0;
						}
					}
				}
			}
		}
		
		//check if agent.size fits remaining space
		//System.out.println(me.name+station.name+freeSpace);
		if(me.type.size > freeSpace || freeSpace == 0) return Double.NEGATIVE_INFINITY;
		if(myDistance > 0) return freeSpace/myDistance;
		return freeSpace;
	}
	
	
	
	/**
	 * Returns the distance from previous target to target station.
	 *  
	 * @param previousTarget  the previous target station
	 * @param target          the current target station
	 * @return                the distance from previous target to target station, -1 if no path could be found or if previous target or target is null
	 */
	private static int distance( Station previousTarget, Station target ){
		// If previousTarget and target exist
	    if( (previousTarget != null) && (target != null) ){
	      
	        Map<StationType, Integer> openList = new HashMap<StationType, Integer>();
	        List<StationType> closedList = new ArrayList<StationType>();
	        Map<StationType, Integer> distances = new HashMap<StationType, Integer>();

	        openList.put( previousTarget.type, 0 ); 
	        
	        do{
	            // Get and remove node with minimal value
	            int minValue = 0;
	            StationType minStationType = null;
	            for( StationType stationType : openList.keySet() ){
	                if( (minStationType == null) || openList.get( stationType ) < minValue ){
	                    minValue = openList.get( stationType );
	                    minStationType = stationType;
	                }
	            }
	            openList.remove( minStationType );

	            // If goal found, set current distance and stop search
	            if( minStationType == target.type ){
	            	int distanceToTarget = 0;
	                if( distances.containsKey( minStationType ) ){
	                    distanceToTarget = distances.get( minStationType );
	                }
	                else{
	                    distanceToTarget = 0;
	                }
	                return( distanceToTarget );
	            }

	            // If goal not found yet, put to closed list (will no longer be considered)
	            closedList.add( minStationType );

	            // Consider all neighbor nodes
	            for( PlaceEdge placeEdge : minStationType.placeEdges ){
                    StationType neighbor = (StationType) placeEdge.connectedType;

                    // If the current considered node with minimal value is not in the closed list
                    if( !closedList.contains( neighbor ) ){
                        // Calculate actual distance to the neighbor
                        int distance = 0;
                        if( distances.containsKey( minStationType ) ){ 
                            distance = distances.get( minStationType );
                        }
                        distance += placeEdge.weight;
                        if(minStationType.time != -1 && minStationType != target.type && minStationType != previousTarget.type) distance += minStationType.time;
                        // If node is not in open list or the new distance will be better than the old one
                        if( !openList.containsKey( neighbor ) || (distance < distances.get( neighbor )) ) {
                            openList.put( neighbor, distance );
                            distances.put( neighbor, distance );
                        }
                    }
	            }
	        }
	        while( !openList.isEmpty() );
	    }

	    // If reached to here no path could be found (or no target or previous target)
	    int distanceToTarget = -1;
	    return( distanceToTarget );
	}
	
	
	/**
	 * 
	 */
	private static List<List<StationType>> findRoute(StationType station, List<StationType> start, List<List<StationType>> part){
		//stops rekusion if cycle found or no more place edges
		
		if(start.contains(station)) {
			part.add(start);
			return part;
		}
		
		start.add(station);
		for(PlaceEdge placeEdge : station.placeEdges) {
			if(placeEdge.outgoing) {
				findRoute((StationType)placeEdge.connectedType, start, part);
			}
		}
		
		part.add(start);
		return part;
		
	}
	
	/**
	 * create my plan
	 */
	private static void createMyPlan(Agent me) {
		//System.out.println("MyPlan");
		List<List<StationType>> result = new ArrayList<List<StationType>>();
		List<StationType> visitStations = new ArrayList<StationType>(); 
		for(VisitEdge visitEdge : me.type.visitEdges) visitStations.add((StationType)visitEdge.connectedType);
	
		for (StationType station : visitStations){
			for( PlaceEdge placeEdge : station.placeEdges ){
				List<List<StationType>> part = new ArrayList<List<StationType>>();
				List<StationType> start = new ArrayList<StationType>();
				if(placeEdge.outgoing){
					start.add(station);
					part = findRoute((StationType)placeEdge.connectedType, start, part);
					part = contains(part);
					for(List<StationType> l : part) {
						if(!result.contains(l))result.add(l);
					}
				}
			}
			
		}
		
		plan.put(me.type, contains(result));
	}
	
	/**
	 * 
	 */
	private static List<List<StationType>> contains(List<List<StationType>> bigList) {
		List<List<StationType>> toRemove = new ArrayList<List<StationType>>(); 
		
		for(List<StationType> list : bigList) {
			for(List<StationType> l : bigList) {
				if(list.size() < l.size() && l.containsAll(list)) {
					toRemove.add(list);
					break;
				} else if (list.size() > l.size() && list.containsAll(l)) {
					toRemove.add(l);
					break;
				}
			}
		}
		for(List<StationType> list : toRemove) {
			bigList.remove(list);
		}
		//System.out.println("bigList: "+bigList);
		return bigList;
	}
	
	/**
	 * create plans
	 */
	private static void createBigPlan(HashMap<Agent, Object> others) {
		for(Agent other : others.keySet()) {
			createMyPlan(other);
		}
		
	}
	
}
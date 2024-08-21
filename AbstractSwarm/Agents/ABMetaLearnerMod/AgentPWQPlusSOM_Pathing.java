import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;

public class AgentPWQPlusSOM_Pathing {
    private static final boolean DEBUG_OUT = false;

    public static boolean canReach(Agent me, Station station, boolean allowSelfSelect) {
		//can reach station type that's been targeted last (current location in evaluation process)
		boolean result = allowSelfSelect && me.previousTarget.type.name.equals(station.type.name);
		for (PlaceEdge edge : me.previousTarget.type.placeEdges) {
			//or can reach if (Edge is outgoing OR undirected) AND connected to specified station type
			result |= (edge.outgoing || !edge.incoming) && edge.connectedType.name.equals(station.type.name);
		}
		return result;
	}
    public static boolean canReach(Agent me, Station station) {
        return canReach(me, station, true);
    }
	public static boolean canReachTransitive(Agent me, StationType location, StationType target, HashSet<StationType> closedStationTypeList) {
		//never self select as this is used to detect returnability
		boolean result = false;

		if (location == null)
			location = me.previousTarget.type;
		if (closedStationTypeList == null)
			closedStationTypeList = new HashSet<StationType>();
		closedStationTypeList.add(location);

		//can reach directly
		for (PlaceEdge edge : location.placeEdges)
			result |= (edge.outgoing || !edge.incoming) && edge.connectedType.name.equals(target.name);
		//or via connected stations
		if (!result) {
			for (PlaceEdge edge : location.placeEdges) {
				if (!result && !closedStationTypeList.contains(edge.connectedType))
					result |= canReachTransitive(me, (StationType)edge.connectedType, target, closedStationTypeList);
			}
		}
		return result;
	}
	public static boolean canReachTransitiveWithoutSkip(Agent me, StationType location, StationType target) {
		HashSet<StationType> closedStationTypeList = new HashSet<StationType>();
		//if you can reach the desired target
		boolean result = canReachTransitive(me, location, target, closedStationTypeList);

		if (location == null)
			location = me.previousTarget.type;
		if (DEBUG_OUT) {
			System.out.println(me.name + " can reach " + target.name + " transitive: " + result);
			System.out.println(me.name + " can return to " + location.name + " transitive: " + canReachTransitive(me, target, location, null));
		}
		//and you can not return to current location
		if (result && !canReachTransitive(me, target, location, null)) {
			//make sure every station in between can be reached later
			for (StationType skipCandidate : closedStationTypeList) {
				result &= skipCandidate == location || canReachTransitive(me, skipCandidate, location, null);
				if (DEBUG_OUT)
					System.out.println(me.name + " considering skip candidate " + skipCandidate.name + ": " + canReachTransitive(me, skipCandidate, location, null));
			}
			if (DEBUG_OUT)
				System.out.println(me.name + " has conflicting skip candidates: " + !result);
		}

		//NOTE: This is not a perfect implementation, since canReachTransitive is doing breadth first search
		//(and therefore includes some stations in the closed list that might not be visited).
		//However, this insures that every station *encountered* on the way is reachable later on. So this might even be desirable.

		return result;
	}

    public static int getDistance(Station a, Station b, List<Station> sorted) {
		//Which algorithm does abstract swarm use to find the shortest route? Does it try to find the sortest route?
		//Uses breadth first search
		ArrayList<StationType> types = new ArrayList<StationType>();
		sorted.forEach(i -> {
			if (!types.contains(i.type))
				types.add(i.type);
		});
		int[] values = new int[types.size()];
		PriorityQueue<StationType> openList = new PriorityQueue<StationType>((i, j) -> Integer.valueOf(values[types.indexOf(i)]).compareTo(values[types.indexOf(j)]));
		HashSet<StationType> closedList = new HashSet<StationType>();
		openList.add(a.type);
		while (openList.size() > 0) {
			StationType s = openList.poll();
			if (s.name.equals(b.type.name))
				return values[types.indexOf(s)];
			
			closedList.add(s);
			for (PlaceEdge edge : s.placeEdges) {
				if (edge.outgoing || !edge.incoming) {
					if (!closedList.contains((StationType)edge.connectedType)) {
						values[types.indexOf((StationType)edge.connectedType)] = values[types.indexOf(s)] + edge.weight;
						openList.add((StationType)edge.connectedType);
					}
				}
			}
		}
		return -1;
	}
}
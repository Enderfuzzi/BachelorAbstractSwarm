import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

public class AgentPWQPlusQueue_VisitStatistics {
    private class VisitStatisticsStationData {
        public HashMap<Station, Integer> data = new HashMap<Station, Integer>();
    }
    private class VisitStatisticsAgentData {
        public VisitStatisticsAgentData(Agent agent, Station sourceStation) {
            this.agent = agent;
            this.sourceStation = sourceStation;
        }

        //

        public Agent agent;
        public Station sourceStation;

        //

        @Override
        public boolean equals(Object obj) {
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            VisitStatisticsAgentData o = (VisitStatisticsAgentData)obj;
            return agent.equals(o.agent) && sourceStation.equals(o.sourceStation);
        }
        @Override
        public int hashCode() {
            return agent.hashCode() ^ sourceStation.hashCode();
        }
    }

    public AgentPWQPlusQueue_VisitStatistics(List<Station> sortedStations, int relevantIterationWindow) {
        stats = new LinkedList<HashMap<VisitStatisticsAgentData, VisitStatisticsStationData>>();
        this.sortedStations = sortedStations;
        this.relevantIterationWindow = relevantIterationWindow;
    }

    //

    private LinkedList<HashMap<VisitStatisticsAgentData, VisitStatisticsStationData>> stats;
    private List<Station> sortedStations;
    private int relevantIterationWindow;

    private HashMap<VisitStatisticsAgentData, VisitStatisticsStationData> aggregated;
    private boolean aggregatedDirty = true;

    //

    public void update(Agent agent, Station sourceStation, Station targetStation) {
        VisitStatisticsAgentData key = new VisitStatisticsAgentData(agent, sourceStation);
        if (!stats.get(0).containsKey(key))
            stats.get(0).put(key, new VisitStatisticsStationData());
            
        VisitStatisticsStationData targets = stats.get(0).get(key);
        if (!targets.data.containsKey(targetStation))
            targets.data.put(targetStation, 1);
        else
            targets.data.put(targetStation, targets.data.get(targetStation) + 1);

        aggregatedDirty = true;
    }
    public void updateIteration() {
        if (relevantIterationWindow > 0) {
            stats.add(new HashMap<VisitStatisticsAgentData, VisitStatisticsStationData>());
            while (stats.size() > relevantIterationWindow)
                stats.removeFirst();
        }
        else if (stats.size() == 0)
            stats.add(new HashMap<VisitStatisticsAgentData, VisitStatisticsStationData>());
    }
    private HashMap<VisitStatisticsAgentData, VisitStatisticsStationData> getAggregated() {
        if (aggregatedDirty) {
            aggregatedDirty = false;
            if (stats.size() == 1)
                aggregated = stats.get(0);
            else {
                aggregated = new HashMap<VisitStatisticsAgentData, VisitStatisticsStationData>();
                for (HashMap<VisitStatisticsAgentData, VisitStatisticsStationData> iteration : stats) {
                    for (Entry<VisitStatisticsAgentData, VisitStatisticsStationData> entry : iteration.entrySet()) {
                        if (aggregated.containsKey(entry.getKey())) {
                            VisitStatisticsStationData stat = new VisitStatisticsStationData();
                            stat.data = new HashMap<Station, Integer>();
                            for (Entry<Station, Integer> visitCounter : entry.getValue().data.entrySet()) {
                                if (stat.data.containsKey(visitCounter.getKey()))
                                    stat.data.put(visitCounter.getKey(), stat.data.get(visitCounter.getKey()) + visitCounter.getValue());
                                else
                                    stat.data.put(visitCounter.getKey(), visitCounter.getValue());
                            }
                            aggregated.put(entry.getKey(), stat);
                        }
                        else
                            aggregated.put(entry.getKey(), entry.getValue());
                    }
                }
            }
        }
        return aggregated;
    }

    public Station predictAgentNextTarget(Agent agent, Station sourceStation) {
        VisitStatisticsAgentData key = new VisitStatisticsAgentData(agent, sourceStation);
        HashMap<VisitStatisticsAgentData, VisitStatisticsStationData> stats = getAggregated();
        if (stats.containsKey(key)) {
            VisitStatisticsStationData targets = stats.get(key);
            Station maxStation = null;
            int totalVisits = 0;
            for (Integer i : targets.data.values())
                totalVisits += i;
            float sVal = -1;
            for (Entry<Station, Integer> stat : targets.data.entrySet()) {
                //approximate behaviour by previously observed behaviour
                if ((float)stat.getValue() / totalVisits > sVal) {
                    maxStation = stat.getKey();
                    sVal = (float)stat.getValue() / totalVisits;
                }
            }
            return maxStation;
        }
        else {
            ArrayList<Station> candidates = new ArrayList<Station>();
            for (Station c : sortedStations) {
                if (AgentPWQPlusQueue_Pathing.canReachTransitive(agent, sourceStation.type, c.type, null))
                    candidates.add(c);
            }
            return candidates.get((int)(Math.random() * candidates.size()));
        }
    }
    
    public Station predictAgentStationAt(Agent agent, Station currentStation, Station currentTarget, long currentTimeOfArrival, long predictionTime) {
        Station station = currentStation;
        Station target = currentTarget;
        long eta = currentTimeOfArrival;
        while (eta < predictionTime) {
            //eta is still before desired time in the future
            //then set the agent position and find next target for agent
            station = target;
            target = predictAgentNextTarget(agent, station);
            //and update the eta to that station
            eta += getEta(agent, station, target, sortedStations);
        }
        return station;
    }
    public Station predictAgentTargetAt(Agent agent, Station currentTarget, long currentTimeOfArrival, long predictionTime) {
        Station target = currentTarget;
        long eta = currentTimeOfArrival;
        while (eta < predictionTime) {
            //eta is still before desired time in the future
            //then find next target for agent
            Station nextTarget = predictAgentNextTarget(agent, target);
            //and update the eta to that station
            eta += getEta(agent, target, nextTarget, sortedStations);
            target = nextTarget;
        }
        return target;
    }

    public float[] predictAgentQueueScales(
        Agent agent,
        HashMap<Agent, Station> lastStation,
        HashMap<Agent, Object> communication,
        HashMap<Agent, AgentPWQPlusQueue_QAgent> brains,
        long predictionTime) {
        //predict where agents want to be at in the future
        HashMap<Agent, Station> currentStations = new HashMap<Agent, Station>();
        for (Agent a : brains.keySet()) {
            Object[] data = (Object[])communication.get(a);
            Station currentTarget = null;
            long eta = 0;
            if (data != null) {
                currentTarget = (Station)data[0];
                eta = (long)data[1];
                currentStations.put(a, predictAgentStationAt(a, lastStation.get(a), currentTarget, eta, predictionTime));
            }
            else //nothing is communicated, the agent has either not chosen or will stay
                currentStations.put(a, lastStation.get(a));
        }

        //determine queue sizes
        float[] result = new float[sortedStations.size()];
        for (int index = 0; index < result.length; index++) {
            int space = sortedStations.get(index).type.space;
            //TODO fix unlimited station space, what happens when agent size -1 visits station with space -1?
            //also, divide by 
            int queueLength = 0;
            for (Entry<Agent, Station> s : currentStations.entrySet()) {
                if (!s.getKey().name.equals(agent.name) && s.getValue().name.equals(sortedStations.get(index).name)) {
                    if (space < s.getKey().type.size) {
                        //since multiple agents could visit a station at once, scale the queue-time with the space being occupied
                        int fillFactor = s.getKey().type.size == -1 ? s.getValue().type.space : s.getKey().type.size;
                        queueLength += getVisitTime(s.getKey(), s.getValue()) * fillFactor;
                    }
                    if (s.getKey().type.size == -1) //default value to fully fill any station
                        space = 0;
                    else
                        space -= s.getKey().type.size;
                }
            }
            result[index] = queueLength;
        }

        //normalize result
        float maxLen = 0;
        for (int index = 0; index < result.length; index++) {
            if (result[index] > maxLen)
                maxLen = result[index];
        }
        if (maxLen > 0) {
            for (int index = 0; index < result.length; index++)
                result[index] /= maxLen;
        }

        return result;
    }

    public float[] getAgentVisitScale(Agent agent, Station sourceStation) {
        VisitStatisticsAgentData key = new VisitStatisticsAgentData(agent, sourceStation);
        HashMap<VisitStatisticsAgentData, VisitStatisticsStationData> stats = getAggregated();
        float[] result = new float[sortedStations.size()];
        if (stats.containsKey(key)) {
            VisitStatisticsStationData targets = stats.get(key);
            int totalVisits = 0;
            for (Integer i : targets.data.values())
                totalVisits += i;
            for (int index = 0; index < result.length; index++) {
                Integer i = targets.data.get(sortedStations.get(index));
                result[index] = 1 - (i == null ? 0 : i.intValue()) / totalVisits;
            }
        }
        else {
            for (int index = 0; index < result.length; index++)
                result[index] = 1;
        }
        return result;
    }

    //

    public static int getEta(Agent agent, Station source, Station target, List<Station> sortedStations) {
        //if an agent has "speed", they travel one distance unit every "speed" time units
        //if an agent has "time", it visits a station for exactly "time" time units
        //if a station has "time", they are visited for exactly "time" time units
        //if both have "time", the visit lasts for the shorter "time"
        int distance = AgentPWQPlusQueue_Pathing.getDistance(source, target, sortedStations);
        if (distance < 0) //can not reach
            return -1;
        int travelTime = distance; //* agent.type.speed; //speed is not yet correctly given to Java Side
        int visitTime = getVisitTime(agent, target);
        return travelTime + visitTime;
    }
    public static int getVisitTime(Agent agent, Station station) {
        int result;
        if (agent.type.time != -1 && station.type.time != -1)
            result = Math.min(agent.type.time, station.type.time);
        else if (agent.type.time != -1)
            result = agent.type.time;
        else if (station.type.time != -1)
            result = station.type.time;
        else
            result = Integer.MAX_VALUE;
        return result;
    }
}

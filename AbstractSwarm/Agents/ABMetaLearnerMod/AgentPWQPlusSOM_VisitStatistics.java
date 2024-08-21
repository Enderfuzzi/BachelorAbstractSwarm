import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

public class AgentPWQPlusSOM_VisitStatistics {
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

    public AgentPWQPlusSOM_VisitStatistics(List<Station> sortedStations) {
        stats = new HashMap<VisitStatisticsAgentData, VisitStatisticsStationData>();
        this.sortedStations = sortedStations;
    }

    //

    private HashMap<VisitStatisticsAgentData, VisitStatisticsStationData> stats;
    private List<Station> sortedStations;

    //

    public void update(Agent agent, Station sourceStation, Station targetStation) {
        VisitStatisticsAgentData key = new VisitStatisticsAgentData(agent, sourceStation);
        if (!stats.containsKey(key))
            stats.put(key, new VisitStatisticsStationData());
            
        VisitStatisticsStationData targets = stats.get(key);
        if (!targets.data.containsKey(targetStation))
            targets.data.put(targetStation, 1);
        else
            targets.data.put(targetStation, targets.data.get(targetStation) + 1);
    }

    public Station predictAgentNextTarget(Agent agent, AgentPWQPlusSOM_QAgent qAgent, Station sourceStation) {
        VisitStatisticsAgentData key = new VisitStatisticsAgentData(agent, sourceStation);
        if (stats.containsKey(key)) {
            VisitStatisticsStationData targets = stats.get(key);
            Station maxStation = null;
            //int totalVisits = 0;
            //for (Integer i : targets.data.values())
            //    totalVisits += i;
            float qVal = -1;
            for (Entry<Station, Integer> stat : targets.data.entrySet()) {
                //approximate state Q value (coop Q Table) plus evaluate station (use ego Q Table)
                //if (((float)stat.getValue() / totalVisits + qAgent.evaluateStationOnly(agent, sourceStation, stat.getKey())) > qVal) {
                //    maxStation = stat.getKey();
                //    qVal = (float)stat.getValue() / totalVisits + qAgent.evaluateStationOnly(agent, sourceStation, stat.getKey());
                //}
                //if ((float)stat.getValue() / totalVisits > qVal) {
                //    maxStation = stat.getKey();
                //    qVal = (float)stat.getValue() / totalVisits;
                //}
                if (qAgent.evaluateStationOnly(agent, sourceStation, stat.getKey()) > qVal) {
                    maxStation = stat.getKey();
                    qVal = qAgent.evaluateStationOnly(agent, sourceStation, stat.getKey());
                }
            }
            return maxStation;
        }
        else {
            ArrayList<Station> candidates = new ArrayList<Station>();
            for (Station c : sortedStations) {
                if (AgentPWQPlusSOM_Pathing.canReachTransitive(agent, sourceStation.type, c.type, null))
                    candidates.add(c);
            }
            return candidates.get((int)(Math.random() * candidates.size()));
        }
    }
    public int getEta(Agent agent, Station source, Station target) {
        //if an agent has "speed", they travel one distance unit every "speed" time units
        //if an agent has "time", it visits a station for exactly "time" time units
        //if a station has "time", they are visited for exactly "time" time units
        //if both have "time", the visit lasts for the shorter "time"
        int distance = AgentPWQPlusSOM_Pathing.getDistance(source, target, sortedStations);
        int travelTime = distance; //* agent.type.speed; //speed is not yet correctly given to Java Side
        int visitTime;
        if (agent.type.time != -1 && target.type.time != -1) 
            visitTime = Math.min(agent.type.time, target.type.time);
        else if (agent.type.time != -1)
            visitTime = agent.type.time;
        else if (target.type.time != -1)
            visitTime = target.type.time;
        else
            visitTime = Integer.MAX_VALUE;
        return travelTime + visitTime;
    }

    public Station predictAgentStationAt(Agent agent, AgentPWQPlusSOM_QAgent qAgent, Station currentStation, Station currentTarget, long currentTimeOfArrival, long predictionTime) {
        Station station = currentStation;
        Station target = currentTarget;
        long eta = currentTimeOfArrival;
        while (eta < predictionTime) {
            //eta is still before desired time in the future
            //then set the agent position and find next target for agent
            station = target;
            target = predictAgentNextTarget(agent, qAgent, station);
            //and update the eta to that station
            eta += getEta(agent, station, target);
        }
        return station;
    }
    public Station predictAgentTargetAt(Agent agent, AgentPWQPlusSOM_QAgent qAgent, Station currentTarget, long currentTimeOfArrival, long predictionTime) {
        Station target = currentTarget;
        long eta = currentTimeOfArrival;
        while (eta < predictionTime) {
            //eta is still before desired time in the future
            //then find next target for agent
            Station nextTarget = predictAgentNextTarget(agent, qAgent, target);
            //and update the eta to that station
            eta += getEta(agent, target, nextTarget);
            target = nextTarget;
        }
        return target;
    }
}

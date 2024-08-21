import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AgentPWQPlusSOM_StateMapper {
    public AgentPWQPlusSOM_StateMapper(ArrayList<Station> sortedStations, AgentPWQPlusSOM_VisitStatistics visitStatistics, int agentCount, boolean aggregateStationTypes) {
        //state consists of position of agents (on station types) and each agent's current target type
        if (aggregateStationTypes) {
            stationTypes = new HashMap<String, Integer>();
            int index = 0;
            for (Station s : sortedStations) {
                if (!stationTypes.containsKey(s.type.name))
                    stationTypes.put(s.type.name, index++);
            }
        }
        else
            this.sortedStations = sortedStations;
        this.visitStatistics = visitStatistics;
        this.agentCount = agentCount;
    }

    //

    private HashMap<String, Integer> stationTypes;
    private ArrayList<Station> sortedStations;
    private AgentPWQPlusSOM_VisitStatistics visitStatistics;
    private int agentCount;

    private PrintWriter writer;
    public boolean logMappings;

    public static final int VALUE_COUNT_PER_AGENT = 1;

    //

    public int size() {
        return agentCount * VALUE_COUNT_PER_AGENT;
    }
    private int maxIndex() {
        return stationTypes != null ? stationTypes.size() : sortedStations.size();
    }

    private float mapStation(Station station, int maxIndex) {
        //a station type (either as position or target) is described as a unique (equidistant) number from (0; 1]
        return (float)((stationTypes != null ? stationTypes.get(station.type.name) : sortedStations.indexOf(station)) + 1) / maxIndex;
    }
    private Station getTargetFromCommunication(Object comData) {
        return comData == null ? null : (Station)(((Object[])comData)[0]);
    }
    public float[] mapState(List<Agent> sortedAgents, HashMap<Agent, Station> lastStation, HashMap<Agent, Object> communication) {
        if (logMappings && writer == null) {
            try {
                writer = new PrintWriter("StateMapper.log");
            }
            catch (IOException e) {
            }
        }
        float[] result = new float[size()];
        int maxIndex = maxIndex();
        int agentIndex = 0;
        for (Agent agent : sortedAgents) {
            if (logMappings)
                writer.print(agent.name + "@" + lastStation.get(agent).name + "->" + (communication.get(agent) != null ? getTargetFromCommunication(communication.get(agent)).name : "null") + " ");
            //result[agentIndex * VALUE_COUNT_PER_AGENT] = mapStation(lastStation.get(agent), maxIndex);

            Station agentTarget = getTargetFromCommunication(communication.get(agent));
            if (agentTarget != null)
                result[agentIndex * VALUE_COUNT_PER_AGENT] = mapStation(agentTarget, maxIndex);
            else {
                result[agentIndex * VALUE_COUNT_PER_AGENT] = mapStation(visitStatistics.predictAgentNextTarget(
                    agent,
                    AgentPWQPlusSOM_AbstractSwarmAgentInterface.getAgentBrain(agent),
                    lastStation.get(agent)),
                    maxIndex);
            }
            
            agentIndex++;
        }

        if (logMappings) {
            writer.println();
            for (int index = 0; index < result.length; index++)
                writer.print(result[index] + " ");
            writer.println();
            writer.flush();
        }

        return result;
    }
}

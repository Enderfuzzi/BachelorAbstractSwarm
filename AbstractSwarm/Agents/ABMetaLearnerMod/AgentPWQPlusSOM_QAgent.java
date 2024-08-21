import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

public class AgentPWQPlusSOM_QAgent {
    public AgentPWQPlusSOM_QAgent(
        ArrayList<Agent> sortedAgents,
        ArrayList<Station> sortedStations,
        AgentPWQPlusSOM_StateMapper stateMapper,
        AgentPWQPlusSOM_VisitStatistics visitStatistics,
        int envQTableSize,
        int replaySampleSize,
        int replaySampleInterval,
        float learningRate,
        float discountFactor,
        boolean mapTargetsToStates) {
        this.sortedAgents = sortedAgents;
        this.sortedStations = sortedStations;
        this.stateMapper = stateMapper;
        this.visitStatistics = visitStatistics;
        this.replaySampleSize = replaySampleSize;
        this.replaySampleInterval = replaySampleInterval;
        this.mapTargetsToStates = mapTargetsToStates;

        som = AgentPWQPlusSOM_SelfOrganizingMap.fromTargetSize(stateMapper.size(), 1, envQTableSize); //StateMapper.VALUE_COUNT_PER_AGENT
        som.setParameters(7500, 0.1f, 0.025f, (int)Math.ceil(Math.sqrt(envQTableSize)), 1); // envQTableSize / (StateMapper.VALUE_COUNT_PER_AGENT * 2)
        
        envQTable = new float[som.size()][];
        if (mapTargetsToStates) {
            for (int index = 0; index < envQTable.length; index++)
                envQTable[index] = new float[som.size()];
        }
        else {
            for (int index = 0; index < envQTable.length; index++)
                envQTable[index] = new float[sortedStations.size()];
        }

        stationQTable = new float[sortedStations.size()][];
        for (int index = 0; index < stationQTable.length; index++)
            stationQTable[index] = new float[sortedStations.size()];

        this.learningRate = learningRate;
        this.discountFactor = discountFactor;

        timestamp = new HashMap<Agent, Long>();
        sourceIndex = new HashMap<Agent, Integer>();
        if (mapTargetsToStates)
            targetIndices = new HashMap<Agent, int[]>();
        scores = new HashMap<Agent, float[]>();
        replayMemory = new AgentPWQPlusSOM_ReplayMemory();
    }

    //

    private float[][] envQTable, stationQTable;
    private ArrayList<Agent> sortedAgents;
    private ArrayList<Station> sortedStations;
    private AgentPWQPlusSOM_StateMapper stateMapper;
    private AgentPWQPlusSOM_VisitStatistics visitStatistics;
    private AgentPWQPlusSOM_SelfOrganizingMap som;
    private float learningRate;
    private float discountFactor;
    private int replaySampleSize;
    private int replaySampleInterval;
    private boolean mapTargetsToStates;

    private HashMap<Agent, Long> timestamp;
    private HashMap<Agent, Integer> sourceIndex;
    private HashMap<Agent, int[]> targetIndices;
    private HashMap<Agent, float[]> scores;

    private AgentPWQPlusSOM_ReplayMemory replayMemory;

    //

    public float evaluateStationOnly(Agent agent, Station lastStation, Station target) {
        int sourceIndex = sortedStations.indexOf(lastStation);
        int targetIndex = sortedStations.indexOf(target);
        return stationQTable[sourceIndex][targetIndex];
    }
    public float evaluateMappedTarget(Agent agent, HashMap<Agent, Station> lastStation, HashMap<Agent, Object> communication, HashMap<Agent, AgentPWQPlusSOM_QAgent> brains, Station target, long time) {
        //identify source state index
        float[] state = stateMapper.mapState(sortedAgents, lastStation, communication);
        int sourceIndex = som.map(state);

        if (!timestamp.containsKey(agent) || timestamp.get(agent).longValue() != time) {
            timestamp.put(agent, time);

            //calculate every target state (every reachable station mapped to the corresponding state)
            int[] ti = new int[sortedStations.size()];
            float[] scores = new float[sortedStations.size()];
            float[][] theoreticalStates = new float[sortedStations.size()][];
            int stationIndex = 0;
            for (Station s : sortedStations) {
                //you can reach this station
                if (AgentPWQPlusSOM_Pathing.canReachTransitive(agent, lastStation.get(agent).type, s.type, null)) {
                    int distance = AgentPWQPlusSOM_Pathing.getDistance(lastStation.get(agent), s, sortedStations);
                    long arrivalTime = time + distance;
                    //determine state when target station is reached as precisely as possible
                    HashMap<Agent, Station> futureLastStation = new HashMap<Agent, Station>(lastStation);
                    futureLastStation.put(agent, s);
                    HashMap<Agent, Object> futureCommunication = new HashMap<Agent, Object>(communication);
                    for (Agent a : communication.keySet()) {
                        if (communication.get(a) != null) {
                            Object[] data = (Object[])communication.get(a);
                            if ((long)data[1] <= arrivalTime) {
                                //agent will have reached the station
                                futureLastStation.put(a, visitStatistics.predictAgentStationAt(a, brains.get(a), lastStation.get(a), (Station)data[0], (long)data[1], arrivalTime));
                                //however, you can not determine how they will have decided from there on
                                Object c = (Object)(new Object[] { visitStatistics.predictAgentTargetAt(a, brains.get(a), (Station)data[0], (long)data[1], arrivalTime) });
                                futureCommunication.put(a, c);
                            }
                        }
                    }
                    theoreticalStates[stationIndex] = stateMapper.mapState(sortedAgents, futureLastStation, futureCommunication);
                    int targetIndex = som.map(theoreticalStates[stationIndex]);

                    //and store the index mapping (ti: station -> state) and Q value for later fitting
                    ti[stationIndex] = targetIndex;
                    scores[stationIndex] = envQTable[sourceIndex][targetIndex];
                }
                stationIndex++;
            }
            this.scores.put(agent, scores);
            this.sourceIndex.put(agent, sourceIndex);
            targetIndices.put(agent, ti);

            //adapt self organizing map to every state used, do this after index calculation to make the above more stable
            som.adapt(state);
            for (int index = 0; index < theoreticalStates.length; index++) {
                if (theoreticalStates[index] != null)
                    som.adapt(theoreticalStates[index]);
            }
        }

        return scores.get(agent)[sortedStations.indexOf(target)];
    }
    public float evaluateTarget(Agent agent, HashMap<Agent, Station> lastStation, HashMap<Agent, Object> communication, Station target, long time) {
        //identify source state index
        float[] state = stateMapper.mapState(sortedAgents, lastStation, communication);
        int sourceIndex = som.map(state);
        this.sourceIndex.put(agent, sourceIndex);

        som.adapt(state);
        //replay memory: keep track of latest available state across all agents
        if (!timestamp.containsKey(agent) || timestamp.get(agent).longValue() != time)
            timestamp.put(agent, time);
        replayMemory.updateState(time, state);

        int targetIndex = sortedStations.indexOf(target);
        return envQTable[sourceIndex][targetIndex];
    }

    public float evaluate(Agent agent, HashMap<Agent, Station> lastStation, HashMap<Agent, Object> communication, HashMap<Agent, AgentPWQPlusSOM_QAgent> brains, Station target, long time) {
        //determine environment-only (cooperative) Q value
        float envQValue = mapTargetsToStates ?
            evaluateMappedTarget(agent, lastStation, communication, brains, target, time) :
            evaluateTarget(agent, lastStation, communication, target, time);

        //determine egoistic Q value
        float stationQValue = evaluateStationOnly(agent, lastStation.get(agent), target);

        //return the value associated with the given station PLUS the egoistic value
        return envQValue + stationQValue;
    }

    public Object communicate(Agent agent, Object[] defaultData) {
        return defaultData;
    }

    private void rewardImmediate(Agent agent, Station lastStation, Station target, float reward) {
        //retrieve scores, index of the state from which the last prediction was made
        int sourceIndex = this.sourceIndex.get(agent);
        float[] scores = mapTargetsToStates ? this.scores.get(agent) : envQTable[sourceIndex];
        //and the index of the state that is associated with the actually selected target (or the target station index)
        int targetIndex = mapTargetsToStates ?
            targetIndices.get(agent)[sortedStations.indexOf(target)] :
            sortedStations.indexOf(target);
        float max = 0;
        for (int index = 0; index < scores.length; index++) {
            if (scores[index] > max)
                max = scores[index];
        }
        //then do a Q-Table update on those states
        envQTable[sourceIndex][targetIndex] = (1 - learningRate) * envQTable[sourceIndex][targetIndex] + learningRate * (reward + discountFactor * max);

        //lastly, update Q table for agent-egoistic station selection
        sourceIndex = sortedStations.indexOf(lastStation);
        if (mapTargetsToStates) //target index value is shared if this is false, so no need to set it again
            targetIndex = sortedStations.indexOf(target);
        max = 0;
        for (int index = 0; index < stationQTable[targetIndex].length; index++) {
            if (max < stationQTable[targetIndex][index]);
                max = stationQTable[targetIndex][index];
        }
        stationQTable[sourceIndex][targetIndex] = (1 - learningRate) * stationQTable[sourceIndex][targetIndex] + learningRate * (reward + discountFactor * max);
    }
    private void rewardReplay(Agent agent, float[] state, Station lastStation, Station target, float reward) {
        if (mapTargetsToStates)
            throw new RuntimeException("Replay reward with mapped targets not supported yet.");
        //retrieve scores, index of the state from which the last prediction was made
        int sourceIndex = som.map(state);
        float[] scores = envQTable[sourceIndex];
        //and the index of the state that is associated with the actually selected target (or the target station index)
        int targetIndex = sortedStations.indexOf(target);
        float max = 0;
        for (int index = 0; index < scores.length; index++) {
            if (scores[index] > max)
                max = scores[index];
        }
        //then do a Q-Table update on those states
        envQTable[sourceIndex][targetIndex] = (1 - learningRate) * envQTable[sourceIndex][targetIndex] + learningRate * (reward + discountFactor * max);

        //lastly, update Q table for agent-egoistic station selection
        sourceIndex = sortedStations.indexOf(lastStation);
        if (mapTargetsToStates) //target index value is shared if this is false, so no need to set it again
            targetIndex = sortedStations.indexOf(target);
        max = 0;
        for (int index = 0; index < stationQTable[targetIndex].length; index++) {
            if (max < stationQTable[targetIndex][index]);
                max = stationQTable[targetIndex][index];
        }
        stationQTable[sourceIndex][targetIndex] = (1 - learningRate) * stationQTable[sourceIndex][targetIndex] + learningRate * (reward + discountFactor * max);
    }
    public void reward(Agent agent, Station lastStation, Station target, float reward) {
        if (replaySampleSize == 0)
            rewardImmediate(agent, lastStation, target, reward);
        else {
            replayMemory.addReward(agent, lastStation, target, reward, timestamp.get(agent));
            int count = replayMemory.countSamples(agent);
            if (count >= replaySampleSize && count % replaySampleInterval == 0) {
                ArrayList<float[]> states = new ArrayList<float[]>();
                ArrayList<Station> sourceStations = new ArrayList<Station>();
                ArrayList<Station> targetStations = new ArrayList<Station>();
                ArrayList<Float> rewards = new ArrayList<Float>();
                replayMemory.sample(agent, replaySampleSize, states, sourceStations, targetStations, rewards);
                for (int index = 0; index < states.size(); index++)
                    rewardReplay(agent, states.get(index), sourceStations.get(index), targetStations.get(index), rewards.get(index));
            }
        }
    }

    public void updateIteration(Agent agent) {
        ArrayList<float[]> states = new ArrayList<float[]>();
        ArrayList<Station> sourceStations = new ArrayList<Station>();
        ArrayList<Station> targetStations = new ArrayList<Station>();
        ArrayList<Float> rewards = new ArrayList<Float>();
        replayMemory.sample(agent, replaySampleSize, states, sourceStations, targetStations, rewards);
        for (int index = 0; index < states.size(); index++)
            rewardReplay(agent, states.get(index), sourceStations.get(index), targetStations.get(index), rewards.get(index));
        replayMemory.clear();
    }

    public void export(int index) {
        try {
            PrintWriter writer = new PrintWriter("Q-Table-E" + index + ".txt");
            for (int s = 0; s < envQTable.length; s++) {
                for (int t = 0; t < envQTable[s].length; t++)
                    writer.print(envQTable[s][t] + " ");
                writer.println();
            }
            writer.close();
            writer = new PrintWriter("Q-Table-S" + index + ".txt");
            for (int s = 0; s < stationQTable.length; s++) {
                for (int t = 0; t < stationQTable[s].length; t++)
                    writer.print(stationQTable[s][t] + " ");
                writer.println();
            }
            writer.close();
            som.export(index);
        }
        catch (IOException e) {
        }
    }
}

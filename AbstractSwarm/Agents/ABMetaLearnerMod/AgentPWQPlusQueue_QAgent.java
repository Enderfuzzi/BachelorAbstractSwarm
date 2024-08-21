import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

public class AgentPWQPlusQueue_QAgent {
    public enum EvaluationMode {
        EXPLORE_TRUE, EXPLORE_AE, EXPLOIT
    }

    public AgentPWQPlusQueue_QAgent(ArrayList<Station> sortedStations, int replaySampleSize, int replaySampleInterval, float learningRate, float discountFactor) {
        this(sortedStations, null, replaySampleSize, replaySampleInterval, learningRate, discountFactor, false);
    }
    public AgentPWQPlusQueue_QAgent(
        ArrayList<Station> sortedStations,
        AgentPWQPlusQueue_VisitStatistics visitStatistics,
        int replaySampleSize,
        int replaySampleInterval,
        float learningRate,
        float discountFactor,
        boolean explorationBias) {
        this.sortedStations = sortedStations;
        this.visitStatistics = visitStatistics;
        this.replaySampleSize = replaySampleSize;
        this.replaySampleInterval = replaySampleInterval;
        intervalQLearning = false;
        this.explorationBias = explorationBias;

        freeQTable = new float[sortedStations.size()][];
        queueQTable = new float[sortedStations.size()][];
        for (int index = 0; index < freeQTable.length; index++) {
            freeQTable[index] = new float[sortedStations.size()];
            queueQTable[index] = new float[sortedStations.size()];
        }

        this.targetEta = new long[sortedStations.size()];
        this.learningRate = learningRate;
        this.discountFactor = discountFactor;
        this.replayMemory = new AgentPWQPlusQueue_ReplayMemory();
    }
    public AgentPWQPlusQueue_QAgent(
        Agent agent,
        ArrayList<Station> sortedStations,
        AgentPWQPlusQueue_VisitStatistics visitStatistics,
        int replaySampleSize,
        int replaySampleInterval,
        float learningRate,
        float discountFactor) {
        this.sortedStations = sortedStations;
        this.visitStatistics = visitStatistics;
        this.replaySampleSize = replaySampleSize;
        this.replaySampleInterval = replaySampleInterval;
        intervalQLearning = true;

        freeQTable = new float[sortedStations.size()][];
        queueQTable = new float[sortedStations.size()][];
        for (int index = 0; index < freeQTable.length; index++) {
            freeQTable[index] = new float[sortedStations.size()];
            queueQTable[index] = new float[sortedStations.size()];
        }

        upperFreeQTable = new float[sortedStations.size()][];
        upperQueueQTable = new float[sortedStations.size()][];
        for (int index = 0; index < upperFreeQTable.length; index++) {
            upperFreeQTable[index] = new float[sortedStations.size()];
            upperQueueQTable[index] = new float[sortedStations.size()];
            for (int target = 0; target < upperFreeQTable[index].length; target++) {
                //bound by 1 + 1 * discount + 1 * discount * discount + ... with discount < 0 => geometric row
                boolean visitSourceStation = false;
                boolean visitTargetStation = false;
                for (VisitEdge ve : agent.type.visitEdges) {
                    visitSourceStation |= ((StationType)ve.connectedType).name.equals(sortedStations.get(index).type.name);
                    visitTargetStation |= ((StationType)ve.connectedType).name.equals(sortedStations.get(target).type.name);
                }
                if (visitSourceStation && visitTargetStation &&
                    AgentPWQPlusQueue_Pathing.canReachTransitive(null, sortedStations.get(index).type, sortedStations.get(target).type, null)) {
                    upperFreeQTable[index][target] = 1 / (1 - discountFactor);
                    upperQueueQTable[index][target] = 1 / (1 - discountFactor);
                }
            }
        }   

        this.targetEta = new long[sortedStations.size()];
        this.learningRate = learningRate;
        this.discountFactor = discountFactor;
        this.replayMemory = new AgentPWQPlusQueue_ReplayMemory();
    }

    //

    private float[][] freeQTable, queueQTable;
    private float[][] upperFreeQTable, upperQueueQTable;
    private ArrayList<Station> sortedStations;
    private AgentPWQPlusQueue_VisitStatistics visitStatistics;
    private AgentPWQPlusQueue_ReplayMemory replayMemory;
    private float learningRate;
    private float discountFactor;
    private int replaySampleSize;
    private int replaySampleInterval;
    private boolean intervalQLearning;
    private boolean explorationBias;

    private long[] targetEta;
    private long timestamp;
    private float[] queueScales;
    private float[] scores;

    //

    public float evaluateFreeOnly(Station lastStation, Station target) {
        int sourceIndex = sortedStations.indexOf(lastStation);
        int targetIndex = sortedStations.indexOf(target);
        if (intervalQLearning)
            return (freeQTable[sourceIndex][targetIndex] + upperFreeQTable[sourceIndex][targetIndex]) / 2;
        else
            return freeQTable[sourceIndex][targetIndex];
    }
    public float evaluateQueueOnly(Station lastStation, Station target) {
        int sourceIndex = sortedStations.indexOf(lastStation);
        int targetIndex = sortedStations.indexOf(target);
        if (intervalQLearning)
            return (queueQTable[sourceIndex][targetIndex] + upperQueueQTable[sourceIndex][targetIndex]) / 2;
        else
            return queueQTable[sourceIndex][targetIndex];
    }
    public float evaluateWithoutPrediction(Station lastStation, Station target, float queueScale) {
        return (1 - queueScale) * evaluateFreeOnly(lastStation, target) + queueScale * evaluateQueueOnly(lastStation, target);
    }

    private void evaluateIntervalQ(
        int sourceIndex,
        EvaluationMode evalMode) {
        //exploration
        if (evalMode == EvaluationMode.EXPLORE_TRUE) {
            //true exploration
            for (int index = 0; index < scores.length; index++) {
                //scores[index] = (float)Math.random();
                scores[index] = (upperFreeQTable[sourceIndex][index] - freeQTable[sourceIndex][index] +
                    upperQueueQTable[sourceIndex][index] - queueQTable[sourceIndex][index]) / 2;
            }
        }
        else if (evalMode == EvaluationMode.EXPLORE_AE) {
            //action elimination exploration
            //all actions are candidates, where the lower of all actions is smaller than the upper of this action
            for (int index = 0; index < scores.length; index++) {
                boolean candidate = true;
                float qs = queueScales[index];
                float myUpper = (1 - qs) * upperFreeQTable[sourceIndex][index] + qs * upperQueueQTable[sourceIndex][index];
                for (int i = 0; i < scores.length && candidate; i++) {
                    float lower = (1 - qs) * freeQTable[sourceIndex][index] + qs * queueQTable[sourceIndex][index];
                    candidate = lower <= myUpper;
                }
                //if this action is a candidate, give it an exploration relevant value
                //otherwise, give it the minimal value (-1) used to auto-exclude
                //scores[index] = candidate ? (float)Math.random() : -1;
                scores[index] = candidate ? (upperFreeQTable[sourceIndex][index] - freeQTable[sourceIndex][index] +
                    upperQueueQTable[sourceIndex][index] - queueQTable[sourceIndex][index]) / 2 : -1;
            }
        }
        else {
            //exploitation
            for (int index = 0; index < scores.length; index++) {
                //scores[index] = (1 - queueScales[index]) * upperFreeQTable[sourceIndex][index] + queueScales[index] * upperQueueQTable[sourceIndex][index];
                scores[index] = ((1 - queueScales[index]) * freeQTable[sourceIndex][index] + queueScales[index] * queueQTable[sourceIndex][index] +
                    (1 - queueScales[index]) * upperFreeQTable[sourceIndex][index] + queueScales[index] * upperQueueQTable[sourceIndex][index]) / 2;
            }
        }
    }
    public float evaluate(
        Agent agent,
        HashMap<Agent, Station> lastStation,
        HashMap<Agent, Object> communication,
        HashMap<Agent, AgentPWQPlusQueue_QAgent> brains,
        Station target,
        long time,
        EvaluationMode evalMode) {
        //identify source state index
        int sourceIndex = sortedStations.indexOf(lastStation.get(agent));
        int targetIndex = sortedStations.indexOf(target);

        if (timestamp != time) {
            timestamp = time;

            for (int index = 0; index < targetEta.length; index++)
                targetEta[index] = AgentPWQPlusQueue_VisitStatistics.getEta(agent, lastStation.get(agent), sortedStations.get(index), sortedStations);
            scores = new float[freeQTable[sourceIndex].length];
            queueScales = new float[freeQTable[sourceIndex].length];

            if (intervalQLearning)
                evaluateIntervalQ(sourceIndex, evalMode);
            else {
                //no interval-Q, the decision to force random (epsilon-greedy) exploration is done outside of this agent
                for (int index = 0; index < scores.length; index++) {
                    if (visitStatistics != null) //use stochastic method from past observations
                        queueScales[index] = visitStatistics.predictAgentQueueScales(agent, lastStation, communication, brains, targetEta[index])[index];
                    else
                        queueScales[index] = AgentPWQPlusQueue_VisitSimulation.predictQueueScales(sortedStations, lastStation, communication, brains, time, targetEta[index])[index];
                    scores[index] = (1 - queueScales[index]) * freeQTable[sourceIndex][index] + queueScales[index] * queueQTable[sourceIndex][index];
                }
            }
            
            if (explorationBias) {
                float[] visitScale = visitStatistics.getAgentVisitScale(agent, lastStation.get(agent));
                for (int index = 0; index < scores.length; index++)
                    scores[index] += visitScale[index];
            }
        }

        return scores[targetIndex];
    }

    public Object communicate(Agent agent, Object[] defaultData) {
        return defaultData;
    }

    private void rewardImmediate(Station lastStation, Station target, float reward, long time) {
        //retrieve index of the state from which the prediction was made
        int sourceIndex = sortedStations.indexOf(lastStation);
        int targetIndex = sortedStations.indexOf(target);
        //find max of free and queue Q tables (both of same size and with same indexing)
        float freeMax = 0;
        float queueMax = 0;
        float upperFreeMax = 0;
        float upperQueueMax = 0;
        for (int index = 0; index < freeQTable[targetIndex].length; index++) {
            if (freeQTable[targetIndex][index] > freeMax)
                freeMax = freeQTable[targetIndex][index];
            if (queueQTable[targetIndex][index] > queueMax)
                queueMax = queueQTable[targetIndex][index];
            if (intervalQLearning) {
                if (upperFreeQTable[targetIndex][index] > upperFreeMax)
                    upperFreeMax = upperFreeQTable[targetIndex][index];
                if (upperQueueQTable[targetIndex][index] > upperQueueMax)
                    upperQueueMax = upperQueueQTable[targetIndex][index];
            }
        }
        boolean queued = time > targetEta[targetIndex];
        //then do a Q-Table update on those states
        if (queued) {
            queueQTable[sourceIndex][targetIndex] = (1 - learningRate) * queueQTable[sourceIndex][targetIndex] + learningRate * (reward + discountFactor * queueMax);
            if (intervalQLearning) {
                upperQueueQTable[sourceIndex][targetIndex] = (1 - learningRate) * upperQueueQTable[sourceIndex][targetIndex] +
                    learningRate * (reward + discountFactor * upperQueueMax);
            }
        }
        else {
            freeQTable[sourceIndex][targetIndex] = (1 - learningRate) * freeQTable[sourceIndex][targetIndex] + learningRate * (reward + discountFactor * freeMax);
            if (intervalQLearning) {
                upperFreeQTable[sourceIndex][targetIndex] = (1 - learningRate) * upperFreeQTable[sourceIndex][targetIndex] +
                    learningRate * (reward + discountFactor * upperFreeMax);
            }
        }
    }
    private void rewardReplay(Station lastStation, Station target, float reward, boolean queued) {
        //retrieve index of the state from which the prediction was made
        int sourceIndex = sortedStations.indexOf(lastStation);
        int targetIndex = sortedStations.indexOf(target);
        //find max of free and queue Q tables (both of same size and with same indexing)
        float freeMax = 0;
        float queueMax = 0;
        float upperFreeMax = 0;
        float upperQueueMax = 0;
        for (int index = 0; index < freeQTable[targetIndex].length; index++) {
            if (freeQTable[targetIndex][index] > freeMax)
                freeMax = freeQTable[targetIndex][index];
            if (queueQTable[targetIndex][index] > queueMax)
                queueMax = queueQTable[targetIndex][index];
            if (intervalQLearning) {
                if (upperFreeQTable[targetIndex][index] > upperFreeMax)
                    upperFreeMax = upperFreeQTable[targetIndex][index];
                if (upperQueueQTable[targetIndex][index] > upperQueueMax)
                    upperQueueMax = upperQueueQTable[targetIndex][index];
            }
        }
        //then do a Q-Table update on those states
        if (queued) {
            queueQTable[sourceIndex][targetIndex] = (1 - learningRate) * queueQTable[sourceIndex][targetIndex] + learningRate * (reward + discountFactor * queueMax);
            if (intervalQLearning) {
                upperQueueQTable[sourceIndex][targetIndex] = (1 - learningRate) * upperQueueQTable[sourceIndex][targetIndex] +
                    learningRate * (reward + discountFactor * upperQueueMax);
            }
        }
        else {
            freeQTable[sourceIndex][targetIndex] = (1 - learningRate) * freeQTable[sourceIndex][targetIndex] + learningRate * (reward + discountFactor * freeMax);
            if (intervalQLearning) {
                upperFreeQTable[sourceIndex][targetIndex] = (1 - learningRate) * upperFreeQTable[sourceIndex][targetIndex] +
                    learningRate * (reward + discountFactor * upperFreeMax);
            }
        }
    }
    public void reward(Agent agent, Station lastStation, Station target, float reward, long time) {
        if (replaySampleSize == 0)
            rewardImmediate(lastStation, target, reward, time);
        else {
            replayMemory.addReward(agent, lastStation, target, time > targetEta[sortedStations.indexOf(target)], reward, timestamp);
            int count = replayMemory.countSamples(agent);
            if (count >= replaySampleSize && count % replaySampleInterval == 0) {
                ArrayList<Station> sourceStations = new ArrayList<Station>();
                ArrayList<Station> targetStations = new ArrayList<Station>();
                ArrayList<Boolean> queued = new ArrayList<Boolean>();
                ArrayList<Float> rewards = new ArrayList<Float>();
                replayMemory.sample(agent, count, sourceStations, targetStations, queued, rewards);
                for (int index = 0; index < sourceStations.size(); index++)
                    rewardReplay(sourceStations.get(index), targetStations.get(index), rewards.get(index), queued.get(index));
            }
        }
    }

    public void updateIteration(Agent agent) {
        ArrayList<Station> sourceStations = new ArrayList<Station>();
        ArrayList<Station> targetStations = new ArrayList<Station>();
        ArrayList<Boolean> queued = new ArrayList<Boolean>();
        ArrayList<Float> rewards = new ArrayList<Float>();
        replayMemory.sample(agent, replaySampleSize, sourceStations, targetStations, queued, rewards);
        for (int index = 0; index < sourceStations.size(); index++)
            rewardReplay(sourceStations.get(index), targetStations.get(index), rewards.get(index), queued.get(index));
        replayMemory.clear();
    }

    public void export(int index) {
        try {
            PrintWriter writer = new PrintWriter("Q-Table-F" + index + ".txt");
            for (int s = 0; s < freeQTable.length; s++) {
                for (int t = 0; t < freeQTable[s].length; t++)
                    writer.print(freeQTable[s][t] + " ");
                writer.println();
            }
            writer.close();
            writer = new PrintWriter("Q-Table-Q" + index + ".txt");
            for (int s = 0; s < queueQTable.length; s++) {
                for (int t = 0; t < queueQTable[s].length; t++)
                    writer.print(queueQTable[s][t] + " ");
                writer.println();
            }
            writer.close();
            if (intervalQLearning) {
                writer = new PrintWriter("Q-Table-FU" + index + ".txt");
                for (int s = 0; s < upperFreeQTable.length; s++) {
                    for (int t = 0; t < upperFreeQTable[s].length; t++)
                        writer.print(upperFreeQTable[s][t] + " ");
                    writer.println();
                }
                writer.close();
                writer = new PrintWriter("Q-Table-QU" + index + ".txt");
                for (int s = 0; s < upperQueueQTable.length; s++) {
                    for (int t = 0; t < upperQueueQTable[s].length; t++)
                        writer.print(upperQueueQTable[s][t] + " ");
                    writer.println();
                }
                writer.close();
            }
        }
        catch (IOException e) {
        }
    }
}

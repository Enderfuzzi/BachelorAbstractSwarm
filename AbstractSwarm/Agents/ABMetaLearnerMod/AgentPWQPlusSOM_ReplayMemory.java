import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

public class AgentPWQPlusSOM_ReplayMemory {
    private class Quartet<T, U, V, W> {
        public Quartet(T item1, U item2, V item3, W item4) {
            this.item1 = item1;
            this.item2 = item2;
            this.item3 = item3;
            this.item4 = item4;
        }

        //

        public T item1;
        public U item2;
        public V item3;
        public W item4;

        //
        
        @Override
        @SuppressWarnings("unchecked")
        public boolean equals(Object obj) {
            if (obj == null || obj.getClass() != getClass())
                return false;
            Quartet<T, U, V, W> o = (Quartet<T, U, V, W>)obj;
            return item1.equals(o.item1) && item2.equals(o.item2) && item3.equals(o.item3) && item4.equals(o.item4);
        }
        @Override
        public int hashCode() {
            return item1.hashCode() ^ (item2.hashCode() << 2) ^ (item3.hashCode() << 5) ^ (item4.hashCode() >> 2);
        }
    }

    public AgentPWQPlusSOM_ReplayMemory() {
        states = new HashMap<Long, float[]>();
        rewards = new HashMap<Quartet<Agent, Long, Station, Station>, Float>();
    }

    //

    private HashMap<Long, float[]> states;
    private HashMap<Quartet<Agent, Long, Station, Station>, Float> rewards;

    //

    public void updateState(long time, float[] state) {
        states.put(time, state);
    }
    public void addReward(Agent agent, Station source, Station target, float reward, long decisionTime) {
        rewards.put(new Quartet<Agent, Long, Station, Station>(agent, decisionTime, source, target), reward);
    }

    public void clear() {
        states.clear();
        rewards.clear();
    }

    public int countSamples(Agent agent) {
        int result = 0;
        for (Entry<Quartet<Agent, Long, Station, Station>, Float> e : rewards.entrySet()) {
            if (e.getKey().item1.name.equals(agent.name))
                result++;
        }
        return result;
    }

    public void sample(Agent agent, int count, ArrayList<float[]> states, ArrayList<Station> sourceStations, ArrayList<Station> targetStations, ArrayList<Float> rewards) {
        ArrayList<Quartet<Long, Station, Station, Float>> candidates = new ArrayList<>();
        for (Entry<Quartet<Agent, Long, Station, Station>, Float> e : this.rewards.entrySet()) {
            if (e.getKey().item1.name.equals(agent.name))
                candidates.add(new Quartet<Long, Station, Station, Float>(e.getKey().item2, e.getKey().item3, e.getKey().item4, e.getValue()));
        }
        if (candidates.size() > 0) {
            for (int index = 0; index < count; index++) {
                int selection = (int)(Math.random() * candidates.size());
                Quartet<Long, Station, Station, Float> quartet = candidates.get(selection);
                states.add(this.states.get(quartet.item1));
                sourceStations.add(quartet.item2);
                targetStations.add(quartet.item3);
                rewards.add(quartet.item4);
            }
        }
    }
}
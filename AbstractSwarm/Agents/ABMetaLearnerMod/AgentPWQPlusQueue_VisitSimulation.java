import java.util.ArrayList;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Map.Entry;

public class AgentPWQPlusQueue_VisitSimulation {
    private class SimEvent implements Comparable<SimEvent> {
        public SimEvent(Agent agent, Station station, int type, long time) {
            this.agent = agent;
            this.station = station;
            this.type = type;
            this.time = time;
        }

        //

        public Agent agent;
        public Station station;
        public int type;
        public long time;

        public static final int TYPE_DEPARTURE = 1, TYPE_ARRIVAL = 2;

        //

        @Override
        public int compareTo(SimEvent o) {
            if (o == null)
                return 0;
            int i = Long.compare(time, o.time);
            return i == 0 ? Integer.compare(type, o.type) : i;
        }
    }

    public AgentPWQPlusQueue_VisitSimulation(ArrayList<Station> sortedStations, HashMap<Agent, Station> lastStation, HashMap<Agent, Object> communication, HashMap<Agent, AgentPWQPlusQueue_QAgent> brains, long time) {
        //at this point, "lastStation" does not represent the actual location
        //since an agent might be waiting in queue at some other station or is in transit
        visiting = new HashMap<Agent, Station>();
        waiting = new HashMap<Agent, Station>();
        this.brains = brains;
        this.sortedStations = sortedStations;

        eventQueue = new PriorityQueue<SimEvent>();
        for (Entry<Agent, Object> c : communication.entrySet()) {
            Object[] data = (Object[])c.getValue();
            if (data != null) {
                Station target = (Station)data[0];
                long arrival = (long)data[1];
                eventQueue.add(new SimEvent(c.getKey(), target, SimEvent.TYPE_ARRIVAL, arrival));
            }
        }

        //if nothing is communicated, then this agent will leave his last (and current) station this time step
        for (Entry<Agent, Station> e : lastStation.entrySet()) {
            if (communication.get(e.getKey()) == null)
                eventQueue.add(new SimEvent(e.getKey(), e.getValue(), SimEvent.TYPE_DEPARTURE, time));
        }

        //next, catch up to reality, i.e. set location to where agents are actually located at
        //null -> in transit; Station@... -> eta<time && (waiting in queue (visiting=false) || visiting (visiting=true))#
        ArrayList<SimEvent> departures = new ArrayList<SimEvent>();
        while (eventQueue.peek().time < time) {
            SimEvent event = eventQueue.poll();
            switch (event.type) {
                case SimEvent.TYPE_ARRIVAL:
                    if (event.agent.visiting) {
                        //currently visiting target station
                        visiting.put(event.agent, event.station);
                        //this is inaccurate, since visit time might be delayed by previous queue - which we can not find the length of
                        eventQueue.add(new SimEvent(event.agent, event.station, SimEvent.TYPE_DEPARTURE,
                            Math.max(event.time + AgentPWQPlusQueue_VisitStatistics.getVisitTime(event.agent, event.station), time)));
                    }
                    else {
                        //waiting in queue for next departure from this station
                        waiting.put(event.agent, event.station);
                        SimEvent departure = findNextDepartureAt(event.station);
                        //if none can be determined (ordering of eventQueue), reevaluate the visit at the next time stamp
                        long reevalTime = departure != null ? departure.time : time + 1;
                        eventQueue.add(new SimEvent(event.agent, event.station, SimEvent.TYPE_ARRIVAL, reevalTime));
                    }
                    break;
                case SimEvent.TYPE_DEPARTURE:
                    visiting.remove(event.agent); //in transit
                    departures.add(event);
                    break;
            }
        }
        float[] queueScales = getQueueScales();
        for (SimEvent event : departures)
            handleDeparture(event, queueScales);
    }

    //

    private HashMap<Agent, AgentPWQPlusQueue_QAgent> brains;
    private HashMap<Agent, Station> visiting;
    private HashMap<Agent, Station> waiting;
    private ArrayList<Station> sortedStations;

    private PriorityQueue<SimEvent> eventQueue;

    //

    private SimEvent findNextDepartureAt(Station station) {
        SimEvent result = null;
        for (SimEvent event : eventQueue) {
            if (event.station.name.equals(station.name) && event.type == SimEvent.TYPE_DEPARTURE && (result == null || result.time > event.time))
                result = event;
        }
        return result;
    }

    private boolean canVisit(Agent agent, Station station) {
        int space = station.type.space;
        for (Entry<Agent, Station> e : visiting.entrySet()) {
            if (e.getValue().name.equals(station.name)) {
                if (e.getKey().type.size == -1) //default value to fully fill any station
                    space = 0;
                else
                    space -= e.getKey().type.size;
            }
        }
        return space > 0 && space >= agent.type.size;
    }
    public int[] getQueueLengths() {
        int[] result = new int[sortedStations.size()];
        for (Entry<Agent, Station> e : waiting.entrySet()) {
            //an agent is located here aka. he is visiting it or in queue
            if (e.getValue() != null) {
                int index = sortedStations.indexOf(e.getValue());
                int fillFactor = e.getKey().type.size == -1 ? e.getValue().type.space : e.getKey().type.size;
                result[index] += AgentPWQPlusQueue_VisitStatistics.getVisitTime(e.getKey(), e.getValue()) * fillFactor;
            }
        }
        return result;
    }
    public float[] getQueueScales() {
        int[] queueLengths = getQueueLengths();
        float max = 0;
        for (int index = 0; index < queueLengths.length; index++) {
            if (queueLengths[index] > max)
                max = queueLengths[index];
        }
        float[] result = new float[queueLengths.length];
        for (int index = 0; index < result.length; index++)
            result[index] = max > 0 ? queueLengths[index] / max : 0;
        return result;
    }

    private void handleDeparture(SimEvent departureEvent, float[] queueScales) {
        visiting.remove(departureEvent.agent);
        int maxIndex = -1;
        float maxValue = -1;
        AgentPWQPlusQueue_QAgent qa = brains.get(departureEvent.agent);
        for (int index = 0; index < sortedStations.size(); index++) {
            //qa can be null at the very beginning of the first time step in the first iteration
            //at this point nothing is learned anyway and random behaviour is expected
            if (AgentPWQPlusQueue_Pathing.canReachTransitive(null, departureEvent.station.type, sortedStations.get(index).type, null)) {
                float eval = qa == null ? (float)Math.random() : qa.evaluateWithoutPrediction(departureEvent.station, sortedStations.get(index), queueScales[index]);
                if (eval > maxValue) {
                    eval = maxValue;
                    maxIndex = index;
                }
            }
        }
        if (maxIndex != -1) {
            eventQueue.add(new SimEvent(departureEvent.agent, sortedStations.get(maxIndex), SimEvent.TYPE_ARRIVAL,
                departureEvent.time + AgentPWQPlusQueue_VisitStatistics.getEta(departureEvent.agent, departureEvent.station, sortedStations.get(maxIndex), sortedStations)));
        }
    }

    public void simulateUntil(long time) {
        while (eventQueue.peek().time < time) {
            SimEvent event = eventQueue.poll();
            switch (event.type) {
                case SimEvent.TYPE_DEPARTURE:
                    handleDeparture(event, getQueueScales());
                    break;
                case SimEvent.TYPE_ARRIVAL:
                    if (canVisit(event.agent, event.station)) {
                        waiting.remove(event.agent);
                        visiting.put(event.agent, event.station);
                        eventQueue.add(new SimEvent(event.agent, event.station, SimEvent.TYPE_DEPARTURE, event.time + AgentPWQPlusQueue_VisitStatistics.getVisitTime(event.agent, event.station)));
                    }
                    else {
                        waiting.put(event.agent, event.station);
                        SimEvent departure = findNextDepartureAt(event.station);
                        //departure == null fallback should not ever become relevant
                        eventQueue.add(new SimEvent(event.agent, event.station, SimEvent.TYPE_ARRIVAL, departure != null ? departure.time : event.time + 1));
                    }
                    break;
            }
        }
    }

    //

    public static float[] predictQueueScales(ArrayList<Station> sortedStations,
        HashMap<Agent, Station> lastStation,
        HashMap<Agent, Object> communication,
        HashMap<Agent, AgentPWQPlusQueue_QAgent> brains,
        long currentTime,
        long predictionTime) {
        AgentPWQPlusQueue_VisitSimulation sim = new AgentPWQPlusQueue_VisitSimulation(sortedStations, lastStation, communication, brains, currentTime);
        sim.simulateUntil(predictionTime);
        return sim.getQueueScales();
    }
}
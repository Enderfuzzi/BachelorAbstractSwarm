
public record Tuple(Agent agent, Station station, Long arrivalTime, Double stationValue) {
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Tuple tuple)) return false;
		if (!this.agent.name.equals(tuple.agent.name)) return false;
		if (!this.station.name.equals(tuple.station.name)) return false;
		if (this.arrivalTime != tuple.arrivalTime) return false;
		if (Math.abs(this.stationValue - tuple.stationValue) > 0.000001) return false;
		return true;
	}
}

public enum Parameter {

	NONE("") {
		@Override
		public boolean isDefault() {
			return true;
		}
	},
	
	BOLD_VISIT_EDGE("bold_visit_edge", 1.5),
	
	AGENT_FREQUENCY("agent_frequency"),
	AGENT_TIME("agent_time"),
	AGENT_TARGET("agent_target"),
	AGENT_VISITING("agent_visiting"),
	AGENT_WORK_TIME_LEFT("agent_work_time_left", 0.5, 0.5),
	AGENT_PRIORITY("agent_priority"),
	AGENT_DISTRIBUTION("agent_distribution",-4.5, 0.5),
	AGENT_NECESSITY("agent_necessitiy", 0.5),
	
	AGENT_TYPE_SIZE("agent_type_size", -0.75),
	AGENT_TYPE_SIZE_WITH_STATION("agent_type_size_with_station", -1.0),
	
	AGENT_DISTANCE_TO_STATION("agent_distance_to_station", -0.25),
	
	OTHER_AGENT_NECESSITY("other_agent_necessity", 0.25),
	OTHER_AGENT_SELECTED_STATION("other_agent_selected_station", 0.5, 0.2),
	OTHER_AGENT_TIME_TO_ARRIVAL("other_agent_time_to_arrival"),
	OTHER_AGENT_VALUE_OF_STATION("other_agent_value_of_station", 0.5, 0.2),
	
	RANDOM("random"),
	
	OTHER_STATIONS_REACHABLE("other_stations_reachable", 5, 1),
	
	STATION_IS_START("station_is_start", 0.5),
	
	STATION_IS_NEIGHBOUR("station_is_neighbour", 0.5, 0.7),
	STATION_IS_NEAREST("station_is_nearest"),
	STATION_IS_TIME_CONNECTED("station_is_time_connected"),
	STATION_IS_SAME("station_is_same", 1.0, 0.3),
	
	STATION_CAPACITY("station_capacity"),
	STATION_SPACE("station_space"),
	STATION_FREQUENCY("station_frequency"),
	STATION_NECESSITY("station_necessity", 0.5),
	
	STATION_DIRECTED_TIME_WEIGHT("station_directed_time_weight", 0.25),
	STATION_DIRECTED_TIME_TARGET_TIME("station_directed_time_target_time", 0.70),
	STATION_DIRECTED_TIME_TARGET("station_directed_time", 0.5),
	STATION_INCOMING_TIME_WEIGHT("station_incoming_time_weight", -0.25),
	
	STATION_TYPE_COMPONENTS("station_type_components"),
	STATION_TYPE_FREQUENCY("station_type_frequency"),
	STATION_TYPE_NECESSITY("station_type_necessity"),
	STATION_TYPE_TIME("station_type_time", -0.35, 0.6),
	STATION_TYPE_SPACE("station_type_space"),
	STATION_TYPE_IS_SAME("station_type_is_same", 1, 0.7),
	
	OTHER_STATION_NECESSITY("other_station_necessity", 0.25),
	
	;

	private final static Parameter[] parameters = Parameter.values();

	private final String representation;

	private final double defaultValue;
	
	private final double activityScore;
	
	private Parameter(String representation) {
		this(representation, 0.5);
	}
	
	private Parameter(String representation, double defaultValue) {
		this(representation, defaultValue, 1.0);
	}
	
	private Parameter(String representation, double defaultValue, double activityScore) {
		this.representation = representation;
		this.defaultValue = defaultValue;
		this.activityScore = activityScore;
	}
	

	public String getRepresentation() {
		return representation;
	}

	public static Parameter getParameter(String value) {
		for (Parameter p : parameters) {
			if (p.getRepresentation().equals(value)) {
				return p;
			}
		}
		return NONE;
	}

	public boolean isDefault() {
		return false;
	}
	
	public double getDefaultValue() {
		return defaultValue;
	}

	public double getActivityScore() {
		return activityScore;
	}
	
}

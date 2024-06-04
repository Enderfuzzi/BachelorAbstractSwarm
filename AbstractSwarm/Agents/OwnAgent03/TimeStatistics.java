
public class TimeStatistics {

	public boolean newRun = true;
	public boolean newBestRun = false;
	public boolean lastRunCompleted = false;
	
	public int numberOfRuns = 0;
	public int numberOfCompletedRuns = 0;
	public int runsSinceCurrentBest = 0;
	
	public long roundTimeUnit = 0L;
	public long lowestTimeUnit = Long.MAX_VALUE;
	
	public long lastValue = 0L;
}

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class InputOutput {
	
	private final static String INITIAL_STATS = "initial_stats.txt";
	private final static String BEST_STATS = "best_stats.txt";
	private final static String OPTIMIZED_PARAMETERS = "optimized_parameters.txt";
	private final static String SAVED_PARAMETERS = "saved_parameters.txt";
	private final static String LOG_DIRECTORY = String.format("Log%s", System.getProperty("file.separator"));
	private final static String LOG_STATS = String.format("%s%s.txt", LOG_DIRECTORY, generateTimeStamp());

	
	
	public static HashMap<Parameter, Cell> loadCells() {
		HashMap<Parameter, Cell> result = new HashMap<>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(INITIAL_STATS));
			String line;
			while ((line = reader.readLine()) != null) {
				String[] splited = line.split(":");
				if (splited.length == 2) {
					Parameter parameter = Parameter.getParameter(splited[0]);
					if (parameter.isDefault()) continue;
					double cellWeight = 0.0;
					try {
						cellWeight = Double.parseDouble(splited[1]);
					} catch (NumberFormatException e) {
						System.out.println("[Error] in loading Cells");
						//TODO add format exception handling
					}
					result.put(parameter, new Cell(splited[0], cellWeight, parameter.getActivityScore()));
				}
			}
			reader.close();
		} catch (IOException e) {
			
		}
		
		for (Parameter parameter : Parameter.values()) {
			if (!result.containsKey(parameter)) {
				result.put(parameter, new Cell(parameter.getRepresentation(), parameter.getDefaultValue(), parameter.getActivityScore()));
			}
		}
		return result;
	}

	public static void saveCells(Collection<Cell> cells, TimeStatistics timeStatistic) {
		saveLog(cells, timeStatistic);
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(BEST_STATS));
			for (Cell cell : cells) {
				writer.write(cell.fileRepresentation());
				writer.newLine();
			}
			writer.flush();
			writer.close();
		} catch (IOException e) {
			System.out.println("[Error] in saving Cells");
		}
	}
	
	
	public static void saveLog(Collection<Cell> cells, TimeStatistics timeStatistic) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_STATS, true));
			
			
			writer.write(String.format("Round Time: %d | Round Number: %d | Completed Rounds: %d", 
					timeStatistic.lowestTimeUnit, timeStatistic.numberOfRuns, timeStatistic.numberOfCompletedRuns));
			writer.newLine();
			writer.write("Cell statistics:");
			writer.newLine();
			for (Cell cell : cells) {
				writer.write(cell.fileRepresentation());
				writer.newLine();
			}
			writer.write("-----------------------------------------");
			writer.newLine();
			writer.flush();
			writer.close();
		} catch (IOException e) {
			System.out.println("Error in writing log file");
			e.printStackTrace();
		}
	}
	
	
	public static void saveBestParameters(HashMap<Parameter, Cell> cells) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(SAVED_PARAMETERS));
			
			List<String> result = new ArrayList<String>();
			String line;
			

			List<String> parameterResult = new ArrayList<String>();
			
			List<Parameter> foundParameter = new ArrayList<>();
			
			while ((line = reader.readLine()) != null) {
				double avgValue = 0.0;
				
				StringBuilder savedParameters = new StringBuilder();
				StringBuilder builder = new StringBuilder();
				
				String[] splited = line.split(";");
				Parameter parameter = Parameter.getParameter(splited[0]);
				
				if (parameter.isDefault()) continue;
				foundParameter.add(parameter);
				
				for (int i = 0;i < splited.length; i++) {
					savedParameters.append(splited[i]);
					savedParameters.append(";");
					
					try {
						if (i != 0) avgValue += Double.parseDouble(splited[i]);
					} catch (NumberFormatException e) {
						//TODO add format exception handling
					}
				}
				
				int numberOfParameters = 0;
				
				if (cells.get(parameter).isBestStatusEnabled()) {
					savedParameters.append(cells.get(parameter).getBestWeight());
					
					avgValue += cells.get(parameter).getBestWeight();
					numberOfParameters = splited.length;	
				} else {
					numberOfParameters = splited.length - 1;
				}
				
				avgValue /= numberOfParameters;
				
				builder.append(String.format(Locale.US,"|Cell: %s\n|Used Parameters: %d\n|Avg Value: %f\n", parameter.getRepresentation(), numberOfParameters, avgValue));

				result.add(builder.toString());
				parameterResult.add(savedParameters.toString());
			}
			reader.close();
			// Add new Parameters which are not saved yet
			for (Parameter parameter : Parameter.values()) {
				if (parameter.isDefault()) continue;
				if (foundParameter.contains(parameter)) continue;
				if (!cells.get(parameter).isBestStatusEnabled()) continue;
				
				StringBuilder builder = new StringBuilder();
				builder.append(String.format(Locale.US,"|Cell: %s\n|Used Parameters: %d\n|Avg Value: %f\n", 
						parameter.getRepresentation(), 1, cells.get(parameter).getBestWeight()));
				result.add(builder.toString());
				
				StringBuilder savedParameters = new StringBuilder();
				savedParameters.append(parameter.getRepresentation());
				savedParameters.append(";");
				savedParameters.append(cells.get(parameter).getBestWeight());
				parameterResult.add(savedParameters.toString());	
			}
			
			
			
			
			BufferedWriter writer = new BufferedWriter(new FileWriter(SAVED_PARAMETERS));
			
			for (String writeLine : parameterResult) {
				writer.write(writeLine);
				writer.newLine();
				writer.newLine();
			}
			
			writer.flush();
			writer.close();
			
			writer = new BufferedWriter(new FileWriter(OPTIMIZED_PARAMETERS));
			
			for (String writeLine : result) {
				writer.write(writeLine);
				writer.newLine();
			}
			
			writer.flush();
			writer.close();
			
		} catch (IOException e) {
			System.out.print("Error on writing optimized parameters");	
		}
	}
	
	

	private static String generateTimeStamp() {
		return new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
	}
	
	public static void printStatus(HashMap<Parameter, Cell> cells, TimeStatistics timeStatistic) {
		System.out.println("#################################################################################################################################");
		System.out.println(String.format("Last run time unit: %d | Last run completed: %b | Current best time unit: %d", 
				timeStatistic.roundTimeUnit, timeStatistic.lastRunCompleted, timeStatistic.lowestTimeUnit));
		System.out.println(String.format("Number of runs: %d | Number of completed runs: %d | Runs since best: %d", 
				timeStatistic.numberOfRuns, timeStatistic.numberOfCompletedRuns, timeStatistic.runsSinceCurrentBest));
		System.out.println("Cells for this round: ");
		for (Map.Entry<Parameter, Cell>  entry : cells.entrySet()) {
			if (entry.getKey().isDefault()) continue;
			System.out.println(entry.getValue());
		}
	}
	
}
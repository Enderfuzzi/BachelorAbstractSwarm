import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Mutation {

	private static Random random = new Random();
	
	private static HashMap<Operator, Double> operatorWeight = new HashMap<>();
	
	static {
		operatorWeight.put(Operator.ADDITION, 0.2);
		operatorWeight.put(Operator.MULTIPLICATION, 0.2);
		operatorWeight.put(Operator.MODULO, 0.2);
		operatorWeight.put(Operator.SUBTRACTION, 0.2);
		operatorWeight.put(Operator.DIVISION, 0.2);
	}
	
	public static Tree mutateValue(Tree tree) {
		Tree copy = new Tree(tree);
		List<TmpNode> possibleNodes = copy.getValueNodes();
		if (possibleNodes.size() < 1) return copy; 
		TmpNode selected = possibleNodes.get(random.nextInt(possibleNodes.size()));
		selected.setValue(selected.getValue() * random.nextDouble(2.0));
		return copy;
	}
	
	public static Tree mutateOperator(Tree tree) {
		Tree copy = new Tree(tree);
		List<TmpNode> possibleNodes = copy.getOperatorNodes();
		if (possibleNodes.size() < 1) return copy; 
		TmpNode selected = possibleNodes.get(random.nextInt(possibleNodes.size()));
		
		double randomValue = random.nextDouble();
		for (Map.Entry<Operator, Double> entry : operatorWeight.entrySet()) {
			if (entry.getValue() > randomValue) {
				selected.setOperator(entry.getKey());
				break;
			}
			randomValue -= entry.getValue();
		}
		
		return copy;
	}
	
	public static Tree addConsumerWeight(Tree tree) {
		Tree copy = new Tree(tree);
		List<TmpNode> possibleNodes = copy.getConsumerNodes();
		if (possibleNodes.size() < 1) return copy; 
		TmpNode selected = possibleNodes.get(random.nextInt(possibleNodes.size()));
		selected.setLeft(new TmpNode(selected));
		selected.setOperator(Operator.MULTIPLICATION);
		selected.setRight(new TmpNode(random.nextDouble(2.0)));
		selected.setConsumer(null);
		return copy;
	}
}

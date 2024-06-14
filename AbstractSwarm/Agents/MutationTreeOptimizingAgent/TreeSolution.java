import java.util.ArrayList;
import java.util.List;

public class TreeSolution {

	private long bestTwT;
	private List<Tree> best;
	
	private List<List<Tree>> solutionPool;
	
	
	public TreeSolution() {
		best = new ArrayList<>();
		solutionPool = new ArrayList<>();
	}
	
	
	public void add(List<Tree> trees, long twt) {
		if (twt < bestTwT) {
			best = new ArrayList<>(trees);
			bestTwT = twt;
		} else {
			// when do I add a node to the solution pool
			solutionPool.add(new ArrayList<>(trees));
			
			
		}
		
		
	}
	
	
	public TmpNode mutate(TmpNode node) {
		
		
		return null;
	}
	
	public TmpNode crossover(TmpNode node) {
		
		return null;
	}
	
	
}

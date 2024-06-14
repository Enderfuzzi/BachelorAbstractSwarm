import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class Tree {
	TmpNode root;
	
	public Tree() {
		root = null;
	}
	
	public Tree(TmpNode root) {
		this.root = root;
	}
	
	public Tree(Tree tree) {
		this.root = tree.root;
	}
	
	public boolean isEmpty() {
		return root == null;
	}
	
	public double evaluate(Agent me, HashMap<Agent, Object> others, Station station) {
		if (!this.isEmpty()) return root.evaluate(me, others, station);
		return 0.0;
	}
	
	public void addNode(TmpNode node) {
		if (this.isEmpty()) {
			root = node;
			return;
		}
		root.addNode(node);
	}
	
	
	public List<TmpNode> getValueNodes() {
		if (this.isEmpty()) return new ArrayList<>();
		List<TmpNode> result = new ArrayList<>();
		Queue<TmpNode> queue = new LinkedList<>();
		queue.add(root);
		while (!queue.isEmpty()) {
			TmpNode tmp = queue.poll();
			if (tmp.isLeaf() && !tmp.hasConsumer()) {
				result.add(tmp);
			} else if (!tmp.isLeaf()) {
				queue.add(tmp.getLeft());
				queue.add(tmp.getRight());
			}
		}
		return result;
	}
	
	public List<TmpNode> getOperatorNodes() {
		if (this.isEmpty()) return new ArrayList<>();
		List<TmpNode> result = new ArrayList<>();
		Queue<TmpNode> queue = new LinkedList<>();
		queue.add(root);
		while (!queue.isEmpty()) {
			TmpNode tmp = queue.poll();
			if (!tmp.isLeaf()) {
				result.add(tmp);
				queue.add(tmp.getLeft());
				queue.add(tmp.getRight());
			}
		}
		return result;
	}
	
	public List<TmpNode> getConsumerNodes() {
		if (this.isEmpty()) return new ArrayList<>();
		List<TmpNode> result = new ArrayList<>();
		Queue<TmpNode> queue = new LinkedList<>();
		queue.add(root);
		while (!queue.isEmpty()) {
			TmpNode tmp = queue.poll();
			if (tmp.isLeaf() && tmp.hasConsumer()) {
				result.add(tmp);
			} else if (!tmp.isLeaf()){
				queue.add(tmp.getLeft());
				queue.add(tmp.getRight());
			}
		}
		return result;
	}
	
	@Override
	public String toString() {
		if (this.isEmpty()) return "()";
		return root.toString();
	}
}

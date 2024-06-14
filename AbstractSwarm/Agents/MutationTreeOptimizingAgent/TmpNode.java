
import java.util.HashMap;

public class TmpNode {
	
	private Operator operator = null;
	private double value;
	private OwnConsumer consumer = null;
	
	private TmpNode left = null;
	private TmpNode right = null;
	
	public TmpNode(Operator operator, TmpNode left, TmpNode right) {
		this.operator = operator;
		this.left = left;
		this.right = right;
	}
	
	public TmpNode(Operator operator, double left, double right) {
		this(operator, new TmpNode(left), new TmpNode(right));
	}
	
	public TmpNode(Operator operator, double left, TmpNode right) {
		this(operator, new TmpNode(left), right);
	}
	
	public TmpNode(Operator operator) {
		this(operator, null, null);
	}
	
	public TmpNode(double value) {
		this(null, null, null);
		this.value = value;
	}
	
	public TmpNode(OwnConsumer consumer) {
		this(null, null, null);
		this.consumer = consumer;
	}

	public TmpNode(TmpNode node) {
		if (node.isLeaf() && !node.hasConsumer()) {
			this.value = node.value;
		} else if (node.isLeaf()) {
			this.consumer = node.consumer;
		} else {
			this.operator = node.operator;
			this.left = new TmpNode(node.left);
			this.right = new TmpNode(node.right);
		}
	}
	
	public TmpNode() {
		value = 0.0;
	}
	
	public void setValue(double value) {
		if (isLeaf() && !hasConsumer()) this.value = value;
	}
	
	public boolean isLeaf() {
		return operator == null;
	}
	
	public boolean hasConsumer() {
		return consumer != null;
	}
	
	public void setOperator(Operator operator) {
		this.operator = operator;
	}
	
	public void setLeft(TmpNode left) {
		this.left = left;
	}
	
	public TmpNode getLeft() {
		return left;
	}
	
	public void setRight(TmpNode right) {
		this.right = right;
	}
	
	public TmpNode getRight() {
		return right;
	}
	
	public void setConsumer(OwnConsumer consumer) {
		this.consumer = consumer;
	}
	
	public double getValue() {
		if (isLeaf() && !hasConsumer()) {
			return value;
		}
		return 0.0;
	}
	
	public void addNode(TmpNode newNode) {
		if (isLeaf()) {
			operator = Operator.ADDITION;
			if (hasConsumer()) {
				this.left = new TmpNode(consumer);
				consumer = null;
			} else {
				this.left = new TmpNode(value);
				value = 0;
			}
			this.right = newNode;
			return;
		}
		
		if (left.depth() <= right.depth()) {
			left.addNode(newNode);
		} else {
			right.addNode(newNode);
		}
		
	}
	
	public double evaluate(Agent me, HashMap<Agent, Object> others, Station station) {
		if (isLeaf() && !hasConsumer()) return value;
		if (isLeaf() && hasConsumer()) return consumer.compute(me, others, station);
		if (this.left != null && this.right != null) return operator.evaluate(this.left.evaluate(me, others, station), this.right.evaluate(me, others, station));
		return 0;
	}
	
	
	public int depth() {
		if (isLeaf()) return 1;
		if (this.left != null) return this.left.depth() + 1;
		if (this.right != null) return this.right.depth() + 1;
		return Math.max(this.left.depth(), this.right.depth()) + 1;
	}
	
	public String toString() {
		String result = "(";
		if (isLeaf() && !hasConsumer()) return "" + value;
		if (hasConsumer()) return "function";
		if (this.left != null) result += this.left.toString();
		result += " " + operator.toString() + " ";
		if (this.right != null) result += this.right.toString();
		result += ")";
		return result;
	}

	
}


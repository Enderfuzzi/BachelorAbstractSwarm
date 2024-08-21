import java.util.HashSet;
import java.util.LinkedList;

public class AgentPWQPlusSOM_SOM_Node {
    public AgentPWQPlusSOM_SOM_Node(int[] positionVector, int weightDimensionality) {
        weight = new float[weightDimensionality];
        //initialize node with a random weight vector
        for (int index = 0; index < weightDimensionality; index++)
            weight[index] = (float)Math.random();
        this.positionVector = new int[positionVector.length];
        for (int i = 0; i < positionVector.length; i++)
            this.positionVector[i] = positionVector[i];
    }

    //

    private AgentPWQPlusSOM_SOM_Node[] neighbours;
    private float[] weight;
    private int[] positionVector;

    //

    public void setNeighbours(AgentPWQPlusSOM_SOM_Node[] value) {
        neighbours = value;
    }

    private float getEuclideanDistance(float[] input) {
        float result = 0;
        for (int index = 0; index < input.length; index++)
            result += (weight[index] - input[index]) * (weight[index] - input[index]);
        return (float)Math.sqrt(result);
    }
    public float getDistance(float[] input) {
        return getEuclideanDistance(input);
    }

    private int getManhattanDistance(AgentPWQPlusSOM_SOM_Node node) {
        int result = 0;
        for (int index = 0; index < positionVector.length; index++)
            result += Math.abs(positionVector[index] - node.positionVector[index]);
        return result;
    }
    public int getDistance(AgentPWQPlusSOM_SOM_Node node) {
        return getManhattanDistance(node);
    }

    private float getDistanceFalloff(float distance, float radius) {
        float nom = -(distance * distance);
        float denom = 2 * radius * radius;
        return (float)Math.exp(nom / denom);
    }

    public float[] getWeight() {
        return weight;
    }
    public int[] getPosition() {
        return positionVector;
    }

    public void adaptWeight(float[] input, int radius, float learningRate) {
        //gets called on best matching unit
        //collects neighbours in breadth first search until radius is reached -> e.g. manhattan distance metric
        HashSet<AgentPWQPlusSOM_SOM_Node> n = new HashSet<AgentPWQPlusSOM_SOM_Node>();
        LinkedList<AgentPWQPlusSOM_SOM_Node> queue = new LinkedList<AgentPWQPlusSOM_SOM_Node>();
        queue.addLast(this);
        n.add(this);
        while (queue.size() > 0) {
            AgentPWQPlusSOM_SOM_Node current = queue.removeFirst();
            float dist = getDistance(current);
            if (dist <= radius) {
                for (AgentPWQPlusSOM_SOM_Node neighbour : current.neighbours) {
                    if (!n.contains(neighbour)) {
                        queue.addLast(neighbour);
                        n.add(neighbour);
                    }
                }
                
                //and modifies weight of found node
                for (int index = 0; index < current.weight.length; index++)
                    current.weight[index] += learningRate * getDistanceFalloff(dist, radius) * (input[index] - current.weight[index]);
            }
        }
    }
}

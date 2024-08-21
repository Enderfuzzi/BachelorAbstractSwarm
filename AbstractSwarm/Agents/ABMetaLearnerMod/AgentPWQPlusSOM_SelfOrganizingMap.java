import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

public class AgentPWQPlusSOM_SelfOrganizingMap {
    public AgentPWQPlusSOM_SelfOrganizingMap(int inputDimensionality, int mapDimensionality, int mapSizePerDim) {
        //map size (output size) is defined by the number of dimensions of the map and the count of neurons per dim
        nodes = new AgentPWQPlusSOM_SOM_Node[(int)Math.pow(mapSizePerDim, mapDimensionality)];
        this.mapSizePerDim = mapSizePerDim;
        bmuCount = new int[nodes.length];
        bmuDist = new double[nodes.length];

        //create the nodes with their respective position
        int[] index = new int[mapDimensionality];
        while (index[index.length - 1] < mapSizePerDim) {
            nodes[linearize(index, mapSizePerDim)] = new AgentPWQPlusSOM_SOM_Node(index, inputDimensionality);
            iterateMultidimIndex(index, mapSizePerDim);
        }

        //reset the multidimensional index and create a copy
        for (int i = 0; i < index.length; i++)
            index[i] = 0;
        int[] nIndex = new int[index.length];

        //set the neighbours of each node
        ArrayList<AgentPWQPlusSOM_SOM_Node> neighbours = new ArrayList<AgentPWQPlusSOM_SOM_Node>();
        while (index[index.length - 1] < mapSizePerDim) {
            //set the copy index to match the iteration index
            for (int j = 0; j < index.length; j++)
                nIndex[j] = index[j];
            
            //collect the neighbours per dimension
            for (int i = 0; i < index.length; i++) {
                //this dimension has neighbor with smaller index
                if (index[i] > 0) {
                    nIndex[i] = index[i] - 1;
                    neighbours.add(nodes[linearize(nIndex, mapSizePerDim)]);
                }
                //this dimension has neighbour with bigger index
                if (index[i] < mapSizePerDim - 1) {
                    nIndex[i] = index[i] + 1;
                    neighbours.add(nodes[linearize(nIndex, mapSizePerDim)]);
                }
                //reset this dimension in the copy for future iterations
                nIndex[i] = index[i];
            }
            //set the neighbours of the current node
            nodes[linearize(index, mapSizePerDim)].setNeighbours(neighbours.toArray(new AgentPWQPlusSOM_SOM_Node[neighbours.size()]));
            neighbours.clear();
            //iterate further
            iterateMultidimIndex(index, mapSizePerDim);
        }
    }
    public static AgentPWQPlusSOM_SelfOrganizingMap fromTargetSize(int inputDimensionality, int mapDimensionality, int targetNeuronCount) {
        return new AgentPWQPlusSOM_SelfOrganizingMap(inputDimensionality, mapDimensionality, (int)Math.round(Math.pow(targetNeuronCount, 1d / mapDimensionality)));
    }

    //

    private AgentPWQPlusSOM_SOM_Node[] nodes;
    private int mapSizePerDim;

    private float startLearningRate;
    private float endLearningRate;

    private int iteration;
    private int targetIterations;

    private int startRadius;
    private int endRadius;

    private int[] bmuCount;
    private double[] bmuDist;

    //

    public void setParameters(int targetIterations, float startLearningRate, float endLearningRate, int startRadius, int endRadius) {
        this.targetIterations = targetIterations;
        this.startLearningRate = startLearningRate;
        this.endLearningRate = endLearningRate;
        this.startRadius = startRadius;
        this.endRadius = endRadius;
    }
    public int size() {
        return nodes.length;
    }

    public AgentPWQPlusSOM_SOM_Node getNode(int... indices) {
        return nodes[linearize(indices, mapSizePerDim)];
    }

    public int getIteration() {
        return iteration;
    }
    public void setIteration(int value) {
        iteration = value;
    }

    public float getLearningRate() {
        return (float)(startLearningRate * Math.pow(endLearningRate / startLearningRate, (double)iteration / targetIterations));
    }
    public int getRadius() {
        return iteration >= targetIterations ? endRadius : (int)(startRadius * Math.pow((double)endRadius / startRadius, (double)iteration / targetIterations));
    }

    private int getBestMatchingUnit(float[] input) {
        //find best matching unit; i.e. node with weight vector closest to given input
        int result = 0;
        float bestMatchingDistance = nodes[result].getDistance(input);
        for (int index = 1; index < nodes.length; index++) {
            float distance = nodes[index].getDistance(input);
            if (distance < bestMatchingDistance) {
                bestMatchingDistance = distance;
                result = index;
            }
        }
        bmuCount[result]++;
        bmuDist[result] += bestMatchingDistance;
        return result;
    }

    public int map(float[] input) {
        //return which node (index) was matching best
        return getBestMatchingUnit(input);
    }
    public void adapt(float[] input) {
        //on the best matching unit, perform an update and increase the iteration
        nodes[getBestMatchingUnit(input)].adaptWeight(input, getRadius(), getLearningRate());
        iteration++;
    }

    public void export(int index) {
        try {
            PrintWriter writer = new PrintWriter("SOM-" + index + ".txt");
            for (AgentPWQPlusSOM_SOM_Node node : nodes) {
                int[] pos = node.getPosition();
                for (int i = 0; i < pos.length - 1; i++)
                    writer.print(pos[i] + ", ");
                writer.print(pos[pos.length - 1] + ": ");
                float[] weight = node.getWeight();
                for (int i = 0; i < weight.length - 1; i++)
                    writer.print(weight[i] + ", ");
                writer.println(weight[weight.length - 1]);
            }

            for (int bmu : bmuCount)
                writer.print(bmu + " ");
            writer.println();
            for (double bmu : bmuDist)
                writer.print((bmu / nodes.length) + " ");
            writer.println();
            writer.close();
        }
        catch (IOException e) {
        }
    }

    //

    public static void iterateMultidimIndex(int[] index, int sizePerDim) {
        //increase first index
        index[0]++;
        //propagate increase where it gets out of range
        for (int i = 0; i < index.length - 1; i++) {
            if (index[i] >= sizePerDim) {
                index[i] = 0;
                index[i + 1]++;
            }
        }
    }
    public static int linearize(int[] pos, int dim) {
        int result = 0;
        int offset = 1;
        for (int index = 0; index < pos.length; index++) {
            result += offset * pos[index];
            offset *= dim;
        }
        return result;
    }
}

package ustc.nodb.core;

public class Edge implements Comparable<Edge>{
    private final int srcVId;
    private final int destVId;
    private int weight;

    public Edge(int srcVId, int destVId, int weight) {
        this.srcVId = srcVId;
        this.destVId = destVId;
        this.weight = weight;
    }

    public int getSrcVId() {
        return srcVId;
    }

    public int getDestVId() {
        return destVId;
    }

    public int getWeight() {
        return weight;
    }

    public void addWeight(){
        weight++;
    }

    @Override
    public int compareTo(Edge edge) {
        return Integer.compare(this.weight, edge.getWeight());
    }
}

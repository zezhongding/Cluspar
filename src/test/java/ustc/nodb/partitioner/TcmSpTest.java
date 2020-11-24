package ustc.nodb.partitioner;

import org.junit.Test;
import ustc.nodb.cluster.StreamCluster;
import ustc.nodb.core.Graph;
import ustc.nodb.game.ClusterPackGame;
import ustc.nodb.properties.GlobalConfig;
import ustc.nodb.sketch.GraphSketch;
import ustc.nodb.thread.ClusterTask;
import ustc.nodb.thread.GameTask;
import ustc.nodb.thread.SketchTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.*;

import static org.junit.Assert.*;

public class TcmSpTest {

    Graph graph;
    ArrayList<GraphSketch> graphSketches = new ArrayList<>();
    ArrayList<StreamCluster> streamClusters = new ArrayList<>();
    ArrayList<ClusterPackGame> clusterPackGames = new ArrayList<>();
    int selectId = 0;

    public TcmSpTest() {
        graph = new Graph();
        graph.readGraphFromFile();
    }

    public void testSketchTask() throws InterruptedException, ExecutionException {
        ExecutorService taskPool = Executors.newCachedThreadPool();
        CompletionService<GraphSketch> completionService = new ExecutorCompletionService<>(taskPool);

        for (int i = 0; i < GlobalConfig.getHashNum(); i++) {
            completionService.submit(new SketchTask(graph, i));
        }

        for (int i = 0; i < GlobalConfig.getHashNum(); i++) {
            try {
                Future<GraphSketch> result = completionService.take();
                graphSketches.add(result.get());

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

//        for(GraphSketch sketch : graphSketches){
//            System.out.println(sketch);
//        }
    }

    public void testClusterTask() throws ExecutionException, InterruptedException {

        testSketchTask();

        ExecutorService taskPool = Executors.newCachedThreadPool();
        CompletionService<StreamCluster> completionService = new ExecutorCompletionService<>(taskPool);

        for(int i = 0; i < GlobalConfig.getHashNum(); i++){
            completionService.submit(new ClusterTask(graphSketches.get(i), i));
        }

        for (int i = 0; i < GlobalConfig.getHashNum(); i++) {
            try {
                Future<StreamCluster> result = completionService.take();
                streamClusters.add(result.get());

            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

//        for(StreamCluster cluster : streamClusters){
//            System.out.println(cluster);
//        }
//
//        for(StreamCluster cluster : streamClusters){
//            HashMap<Integer, HashMap<Integer, Integer>> map = cluster.getInnerAndCutEdge();
//
//            map.forEach((k1, v1)->{
//                v1.forEach((k2, v2)->{
//                    System.out.println(k1.toString() + " : " + k2.toString() + " : " + v2.toString());
//                });
//            });
//        }
    }

    public void testPackGameTask() throws ExecutionException, InterruptedException {
        testClusterTask();

        ExecutorService taskPool = Executors.newCachedThreadPool();
        CompletionService<ClusterPackGame> completionService = new ExecutorCompletionService<>(taskPool);

        for(int i = 0; i < GlobalConfig.getHashNum(); i++){
            completionService.submit(new GameTask(streamClusters.get(i), i));
        }

        for (int i = 0; i < GlobalConfig.getHashNum(); i++) {
            try {
                Future<ClusterPackGame> result = completionService.take();
                clusterPackGames.add(result.get());

            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        int cutEdge = clusterPackGames.get(0).getCutEdge();
        selectId = 0;
        int i = 0;
        for(ClusterPackGame game : clusterPackGames) {
            if(game.getCutEdge() < cutEdge){
                cutEdge = game.getCutEdge();
                selectId = i;
            }
            i++;
        }

//        for(ClusterPackGame game : clusterPackGames){
//            ArrayList<HashSet<Integer>> partition = game.getInvertedPartitionIndex();
//            partition.forEach(v->{
//                v.forEach(cluster->{
//                    System.out.print(cluster.toString() + " ");
//                });
//                System.out.println();
//            });
//            System.out.println("cut edge: " + game.getCutEdge());
//        }
    }

    @Test
    public void testTcmSp() throws ExecutionException, InterruptedException {
        testPackGameTask();

        TcmSp tcmSp = new TcmSp(graph, graphSketches.get(selectId), streamClusters.get(selectId), clusterPackGames.get(selectId));

        tcmSp.performStep();

        System.out.println("replicate factor: " + tcmSp.getReplicateFactor());
        System.out.println("relative edge load balance: " + tcmSp.getLoadBalance());
    }

}
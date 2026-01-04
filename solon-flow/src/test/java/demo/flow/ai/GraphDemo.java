package demo.flow.ai;

import org.noear.solon.flow.Graph;

/**
 *
 * @author noear 2026/1/4 created
 *
 */
public class GraphDemo {
    public static void main(String[] args) {
        Graph devGraph = Graph.create("1", spec -> {

        });

        Graph managerGraph = Graph.create("1", spec -> {
            spec.addStart("s").linkAdd(devGraph.getId());
            spec.addActivity(devGraph.asTask()).linkAdd("e");
            spec.addEnd("e");
        });
    }
}
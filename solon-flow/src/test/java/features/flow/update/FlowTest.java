package features.flow.update;

import org.junit.jupiter.api.Test;
import org.noear.solon.flow.Graph;
import org.noear.solon.flow.GraphSpec;
import org.noear.solon.flow.NodeSpec;

/**
 * @author noear 2025/7/20 created
 */
public class FlowTest {
    @Test
    public void json() {
        //加鉴
        Graph graph = new GraphSpec("c1").create(spec -> {
            spec.addNode(NodeSpec.startOf("s1"));
            spec.addNode(NodeSpec.parallelOf("p1").linkAdd("n1").linkAdd("n2"));
            spec.addNode(NodeSpec.activityOf("n1").linkAdd("e1"));
            spec.addNode(NodeSpec.activityOf("n2").linkAdd("e1"));
            spec.addNode(NodeSpec.endOf("e1"));
        });

        String oldJson = graph.toJson();
        System.out.println(oldJson);

        assert oldJson.equals("{\"id\":\"c1\",\"title\":\"c1\",\"layout\":[{\"id\":\"s1\",\"type\":\"start\"},{\"id\":\"p1\",\"type\":\"parallel\",\"link\":[{\"nextId\":\"n1\"},{\"nextId\":\"n2\"}]},{\"id\":\"n1\",\"type\":\"activity\",\"link\":[{\"nextId\":\"e1\"}]},{\"id\":\"n2\",\"type\":\"activity\",\"link\":[{\"nextId\":\"e1\"}]},{\"id\":\"e1\",\"type\":\"end\"}]}");

        System.out.println("---------------------");

        //------------------
        Graph graph2 = GraphSpec.fromText(oldJson).create(spec -> {
            spec.addNode(NodeSpec.parallelOf("p1").linkAdd("n1").linkAdd("n2").linkAdd("n3"));
            spec.addNode(NodeSpec.activityOf("n3").linkAdd("e1"));
        });

        String newJson = graph2.toJson();
        System.out.println(newJson);

        assert newJson.equals("{\"id\":\"c1\",\"title\":\"c1\",\"layout\":[{\"id\":\"s1\",\"type\":\"start\",\"link\":[{\"nextId\":\"p1\"}]},{\"id\":\"p1\",\"type\":\"parallel\",\"link\":[{\"nextId\":\"n1\"},{\"nextId\":\"n2\"},{\"nextId\":\"n3\"}]},{\"id\":\"n1\",\"type\":\"activity\",\"link\":[{\"nextId\":\"e1\"}]},{\"id\":\"n2\",\"type\":\"activity\",\"link\":[{\"nextId\":\"e1\"}]},{\"id\":\"e1\",\"type\":\"end\"},{\"id\":\"n3\",\"type\":\"activity\",\"link\":[{\"nextId\":\"e1\"}]}]}");
    }

    @Test
    public void yaml() {
        //加鉴
        Graph graph = new GraphSpec("c1").create(spec -> {
            spec.addNode(NodeSpec.startOf("s1"));
            spec.addNode(NodeSpec.parallelOf("p1").linkAdd("n1").linkAdd("n2"));
            spec.addNode(NodeSpec.activityOf("n1").linkAdd("e1"));
            spec.addNode(NodeSpec.activityOf("n2").linkAdd("e1"));
            spec.addNode(NodeSpec.endOf("e1"));
        });

        String oldJson = graph.toYaml();
        System.out.println(oldJson);

        System.out.println("---------------------");

        //------------------
        Graph graph2 = GraphSpec.fromText(oldJson).create(spec -> {
            spec.addNode(NodeSpec.parallelOf("p1").linkAdd("n1").linkAdd("n2").linkAdd("n3"));
            spec.addNode(NodeSpec.activityOf("n3").linkAdd("e1"));
        });


        String newJson = graph2.toYaml();
        System.out.println(newJson);
    }
}
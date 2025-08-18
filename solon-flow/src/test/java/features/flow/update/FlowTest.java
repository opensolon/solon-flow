package features.flow.update;

import org.junit.jupiter.api.Test;
import org.noear.solon.flow.Chain;
import org.noear.solon.flow.ChainDecl;
import org.noear.solon.flow.NodeDecl;

/**
 * @author noear 2025/7/20 created
 */
public class FlowTest {
    @Test
    public void json() {
        //加鉴
        Chain chain = new ChainDecl("c1").create(decl -> {
            decl.addNode(NodeDecl.startOf("s1"));
            decl.addNode(NodeDecl.parallelOf("p1").linkAdd("n1").linkAdd("n2"));
            decl.addNode(NodeDecl.activityOf("n1").linkAdd("e1"));
            decl.addNode(NodeDecl.activityOf("n2").linkAdd("e1"));
            decl.addNode(NodeDecl.endOf("e1"));
        });

        String oldJson = chain.toJson();
        System.out.println(oldJson);

        assert oldJson.equals("{\"id\":\"c1\",\"title\":\"c1\",\"layout\":[{\"id\":\"s1\",\"type\":\"start\"},{\"id\":\"p1\",\"type\":\"parallel\",\"link\":[{\"nextId\":\"n1\"},{\"nextId\":\"n2\"}]},{\"id\":\"n1\",\"type\":\"activity\",\"link\":[{\"nextId\":\"e1\"}]},{\"id\":\"n2\",\"type\":\"activity\",\"link\":[{\"nextId\":\"e1\"}]},{\"id\":\"e1\",\"type\":\"end\"}]}");

        System.out.println("---------------------");

        //------------------
        Chain chain2 = ChainDecl.parseByText(oldJson).create(decl -> {
            decl.addNode(NodeDecl.parallelOf("p1").linkAdd("n1").linkAdd("n2").linkAdd("n3"));
            decl.addNode(NodeDecl.activityOf("n3").linkAdd("e1"));
        });

        String newJson = chain2.toJson();
        System.out.println(newJson);

        assert newJson.equals("{\"id\":\"c1\",\"title\":\"c1\",\"layout\":[{\"id\":\"s1\",\"type\":\"start\",\"link\":[{\"nextId\":\"p1\"}]},{\"id\":\"p1\",\"type\":\"parallel\",\"link\":[{\"nextId\":\"n1\"},{\"nextId\":\"n2\"},{\"nextId\":\"n3\"}]},{\"id\":\"n1\",\"type\":\"activity\",\"link\":[{\"nextId\":\"e1\"}]},{\"id\":\"n2\",\"type\":\"activity\",\"link\":[{\"nextId\":\"e1\"}]},{\"id\":\"e1\",\"type\":\"end\"},{\"id\":\"n3\",\"type\":\"activity\",\"link\":[{\"nextId\":\"e1\"}]}]}");
    }

    @Test
    public void yaml() {
        //加鉴
        Chain chain = new ChainDecl("c1").create(decl -> {
            decl.addNode(NodeDecl.startOf("s1"));
            decl.addNode(NodeDecl.parallelOf("p1").linkAdd("n1").linkAdd("n2"));
            decl.addNode(NodeDecl.activityOf("n1").linkAdd("e1"));
            decl.addNode(NodeDecl.activityOf("n2").linkAdd("e1"));
            decl.addNode(NodeDecl.endOf("e1"));
        });

        String oldJson = chain.toYaml();
        System.out.println(oldJson);

        System.out.println("---------------------");

        //------------------
        Chain chain2 = ChainDecl.parseByText(oldJson).create(decl -> {
            decl.addNode(NodeDecl.parallelOf("p1").linkAdd("n1").linkAdd("n2").linkAdd("n3"));
            decl.addNode(NodeDecl.activityOf("n3").linkAdd("e1"));
        });


        String newJson = chain2.toYaml();
        System.out.println(newJson);
    }
}
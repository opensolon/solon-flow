package features.flow.update;

import org.junit.jupiter.api.Test;
import org.noear.solon.flow.Chain;
import org.noear.solon.flow.NodeDecl;

/**
 * @author noear 2025/7/20 created
 */
public class FlowTest {
    @Test
    public void json() {
        //加鉴
        Chain chain = new Chain("c1");

        chain.addNode(NodeDecl.startOf("s1"));
        chain.addNode(NodeDecl.parallelOf("p1").linkAdd("n1").linkAdd("n2"));
        chain.addNode(NodeDecl.activityOf("n1").linkAdd("e1"));
        chain.addNode(NodeDecl.activityOf("n2").linkAdd("e1"));
        chain.addNode(NodeDecl.endOf("e1"));
        chain.check();

        String oldJson = chain.toJson();
        System.out.println(oldJson);

        System.out.println("---------------------");

        //------------------
        Chain chain2 = Chain.parseByText(oldJson);
        chain2.addNode(NodeDecl.parallelOf("p1").linkAdd("n1").linkAdd("n2").linkAdd("n3"));
        chain2.addNode(NodeDecl.activityOf("n3").linkAdd("e1"));
        chain2.check();

        String newJson = chain2.toJson();
        System.out.println(newJson);
    }

    @Test
    public void yaml() {
        //加鉴
        Chain chain = new Chain("c1");

        chain.addNode(NodeDecl.startOf("s1"));
        chain.addNode(NodeDecl.parallelOf("p1").linkAdd("n1").linkAdd("n2"));
        chain.addNode(NodeDecl.activityOf("n1").linkAdd("e1"));
        chain.addNode(NodeDecl.activityOf("n2").linkAdd("e1"));
        chain.addNode(NodeDecl.endOf("e1"));
        chain.check();

        String oldJson = chain.toYaml();
        System.out.println(oldJson);

        System.out.println("---------------------");

        //------------------
        Chain chain2 = Chain.parseByText(oldJson);
        chain2.addNode(NodeDecl.parallelOf("p1").linkAdd("n1").linkAdd("n2").linkAdd("n3"));
        chain2.addNode(NodeDecl.activityOf("n3").linkAdd("e1"));
        chain2.check();

        String newJson = chain2.toYaml();
        System.out.println(newJson);
    }
}
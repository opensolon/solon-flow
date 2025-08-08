package features.flow.cfg_iterator;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.noear.solon.annotation.Inject;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.FlowEngine;
import org.noear.solon.test.SolonTest;

import java.util.TreeMap;

@Slf4j
@SolonTest
public class IteratorTest {
    @Inject
    FlowEngine flowEngine;

    @Test
    public void case1(){
        FlowContext context = new FlowContext();
        flowEngine.eval("fetch", context);

        log.warn(new TreeMap<>(context.model()).toString());
    }

    @Test
    public void case2(){
        FlowContext context = new FlowContext();
        flowEngine.eval("fetch2", context);

        log.warn(new TreeMap<>(context.model()).toString());
    }
}

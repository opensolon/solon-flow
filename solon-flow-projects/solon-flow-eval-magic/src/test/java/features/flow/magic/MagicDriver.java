package features.flow.magic;

import org.noear.solon.flow.Evaluation;
import org.noear.solon.flow.driver.AbstractChainDriver;
import org.noear.solon.flow.evaluation.MagicEvaluation;

/**
 * @author noear 2025/3/26 created
 */
public class MagicDriver extends AbstractChainDriver {
    @Override
    public Evaluation evaluation() {
        return MagicEvaluation.INSTANCE;
    }

    @Override
    public Object getComponent(String componentName) {
        return null;
    }
}

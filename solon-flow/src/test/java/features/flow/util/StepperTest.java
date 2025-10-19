package features.flow.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.noear.solon.flow.FlowException;
import org.noear.solon.flow.util.Stepper;

/**
 *
 * @author noear 2025/10/19 created
 *
 */
public class StepperTest {
    @Test
    public void case1() {
        Stepper stepper = Stepper.from("1:3:1");


        System.out.println(stepper);
        assert stepper.toString().equals("Stepper{start=1, end=3, step=1, current=1}");

        stepper.next();
        assert stepper.toString().equals("Stepper{start=1, end=3, step=1, current=1}");

        stepper.next();
        assert stepper.toString().equals("Stepper{start=1, end=3, step=1, current=2}");


        Assertions.assertThrows(Throwable.class, () -> stepper.next());
    }

    @Test
    public void case2() {
        Stepper stepper = Stepper.from("1...3");


        System.out.println(stepper);
        assert stepper.toString().equals("Stepper{start=1, end=3, step=1, current=1}");

        stepper.next();
        assert stepper.toString().equals("Stepper{start=1, end=3, step=1, current=1}");

        stepper.next();
        assert stepper.toString().equals("Stepper{start=1, end=3, step=1, current=2}");

        Assertions.assertThrows(Throwable.class, () -> stepper.next());
    }
}

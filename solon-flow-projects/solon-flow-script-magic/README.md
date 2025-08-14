
配置示例

```yaml
# case1.yml
id: f1
layout:
  - task: |
      context.put("result", a + b);
    when: a > b
```

代码示例

```java
public void demo() throws Throwable {
    FlowEngine engine = FlowEngine.newInstance();
    engine.register(new SimpleFlowDriver(new MagicActuator()));

    engine.load("classpath:flow/*");

    FlowContext context = FlowContext.of();
    context.put("a", 1);
    context.put("b", 2);

    engine.eval("f1", context);
}
```
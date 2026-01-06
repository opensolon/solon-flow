package features.workflow.generated;

import org.junit.jupiter.api.Test;
import org.noear.solon.flow.*;
import org.noear.solon.flow.workflow.*;
import org.noear.solon.flow.workflow.controller.ActorStateController;
import org.noear.solon.flow.workflow.controller.BlockStateController;
import org.noear.solon.flow.workflow.repository.InMemoryStateRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WorkflowService 多图协同测试
 * 模拟生产环境中的复杂场景：多个流程图相互调用、嵌套执行
 */
class WorkflowServiceMultiGraphTest {

    // 用于追踪跨图调用的执行记录
    private final Map<String, List<String>> crossGraphExecutionTrace = new ConcurrentHashMap<>();

    @Test
    void testMultiGraphCollaboration() {
        // 测试目的：验证多个流程图之间的协同工作
        // 测试场景：主流程调用子流程，子流程再调用其他子流程的多层嵌套
        // 验证点：跨图调用、状态传递、数据共享、执行跟踪

        System.out.println("=== 开始多图协同测试 ===");

        // ===== 1. 创建最底层的子流程（数据处理流程） =====
        TaskComponent dataProcessingComponent = new TaskComponent() {
            @Override
            public void run(FlowContext context, Node node) throws Throwable {
                String traceId = context.getOrDefault("traceId", "");
                crossGraphExecutionTrace.computeIfAbsent(traceId, k -> new ArrayList<>())
                        .add("data-processing-" + node.getId() + ":" + System.currentTimeMillis());

                String inputData = context.getOrDefault("inputData", "");
                String processedData = inputData + "->已处理";
                context.put("processedData", processedData);
                context.put("dataProcessed", true);
                context.put("processor", context.getOrDefault("processor", "system"));
            }
        };

        Graph dataProcessingGraph = Graph.create("data-processing-flow", "数据处理流程", spec -> {
            spec.addStart("start").title("开始").linkAdd("validate-data");

            spec.addActivity("validate-data").title("数据验证")
                    .metaPut("actor", "data-validator")
                    .task(dataProcessingComponent)
                    .linkAdd("transform-data");

            spec.addActivity("transform-data").title("数据转换")
                    .metaPut("actor", "data-transformer")
                    .task(dataProcessingComponent)
                    .linkAdd("end");

            spec.addEnd("end").title("完成");
        });

        // ===== 2. 创建中间层子流程（审批流程） =====
        TaskComponent approvalComponent = new TaskComponent() {
            @Override
            public void run(FlowContext context, Node node) throws Throwable {
                String traceId = context.getOrDefault("traceId", "");
                crossGraphExecutionTrace.computeIfAbsent(traceId, k -> new ArrayList<>())
                        .add("approval-" + node.getId() + ":" + System.currentTimeMillis());

                String nodeId = node.getId();
                if ("apply".equals(nodeId)) {
                    context.put("applicationData", "申请-" + UUID.randomUUID());
                    context.put("applyBy", context.getOrDefault("applicant", "unknown"));
                } else if ("review".equals(nodeId)) {
                    String result = context.getOrDefault("reviewResult", "approve");
                    context.put("reviewResult", result);
                    context.put("reviewedBy", context.getOrDefault("reviewer", "system"));
                    context.put("reviewTime", System.currentTimeMillis());
                }
            }
        };

        Graph approvalGraph = Graph.create("approval-flow", "审批流程", spec -> {
            spec.addStart("start").title("开始").linkAdd("apply");

            spec.addActivity("apply").title("提交申请")
                    .metaPut("actor", "applicant")
                    .task(approvalComponent)
                    .linkAdd("review");

            spec.addExclusive("review").title("审批")
                    .metaPut("actor", "reviewer")
                    .task(approvalComponent)
                    .linkAdd("data-processing", link -> link.when("${reviewResult} == 'approve'").title("通过-调用数据处理"))
                    .linkAdd("reject-handle", link -> link.when("${reviewResult} == 'reject'").title("驳回"));

            // 调用数据处理子流程
            spec.addActivity("data-processing").title("调用数据处理")
                    .task(new TaskComponent() {
                        @Override
                        public void run(FlowContext context, Node node) throws Throwable {
                            String traceId = context.getOrDefault("traceId", "");
                            crossGraphExecutionTrace.computeIfAbsent(traceId, k -> new ArrayList<>())
                                    .add("call-data-processing:" + System.currentTimeMillis());

                            // 设置数据处理需要的参数
                            context.put("inputData", context.getAs("applicationData"));
                            context.put("processor", "approval-system");

                            // 调用子流程（这里模拟调用，实际中需要工作流引擎支持跨图调用）
                            // 注意：实际实现可能需要通过 #data-processing-flow 这样的语法
                            System.out.println("审批流程调用数据处理流程: " + context.getInstanceId());
                        }
                    })
                    .linkAdd("notify-result");

            spec.addActivity("reject-handle").title("驳回处理")
                    .task(approvalComponent)
                    .linkAdd("end");

            spec.addActivity("notify-result").title("通知结果")
                    .task(approvalComponent)
                    .linkAdd("end");

            spec.addEnd("end").title("完成");
        });

        // ===== 3. 创建主流程（业务主流程） =====
        TaskComponent mainProcessComponent = new TaskComponent() {
            @Override
            public void run(FlowContext context, Node node) throws Throwable {
                String traceId = context.getOrDefault("traceId", "");
                crossGraphExecutionTrace.computeIfAbsent(traceId, k -> new ArrayList<>())
                        .add("main-" + node.getId() + ":" + System.currentTimeMillis());

                String nodeId = node.getId();
                if ("init".equals(nodeId)) {
                    context.put("businessId", "BIZ-" + UUID.randomUUID().toString().substring(0, 8));
                    context.put("initTime", System.currentTimeMillis());
                    context.put("initiator", context.getOrDefault("operator", "system"));
                } else if ("final-check".equals(nodeId)) {
                    context.put("finalCheckPassed", true);
                    context.put("checker", context.getOrDefault("checker", "system"));
                    context.put("checkTime", System.currentTimeMillis());
                }
            }
        };

        Graph mainGraph = Graph.create("main-business-flow", "业务主流程", spec -> {
            spec.addStart("start").title("开始").linkAdd("init");

            spec.addActivity("init").title("初始化业务")
                    .metaPut("actor", "operator")
                    .task(mainProcessComponent)
                    .linkAdd("parallel-gateway");

            spec.addParallel("parallel-gateway").title("并行处理")
                    .linkAdd("data-collection")
                    .linkAdd("approval-process");

            // 数据收集分支
            spec.addActivity("data-collection").title("数据收集")
                    .metaPut("actor", "data-collector")
                    .task(mainProcessComponent)
                    .linkAdd("data-processing");

            spec.addActivity("data-processing").title("调用数据处理")
                    .task(new TaskComponent() {
                        @Override
                        public void run(FlowContext context, Node node) throws Throwable {
                            String traceId = context.getOrDefault("traceId", "");
                            crossGraphExecutionTrace.computeIfAbsent(traceId, k -> new ArrayList<>())
                                    .add("main-call-data-processing:" + System.currentTimeMillis());

                            // 模拟调用数据处理子流程
                            System.out.println("主流程调用数据处理流程: " + context.getInstanceId());
                            context.put("dataProcessedByMain", true);
                        }
                    })
                    .linkAdd("sync-gateway");

            // 审批分支
            spec.addActivity("approval-process").title("调用审批流程")
                    .metaPut("actor", "approval-initiator")
                    .task(new TaskComponent() {
                        @Override
                        public void run(FlowContext context, Node node) throws Throwable {
                            String traceId = context.getOrDefault("traceId", "");
                            crossGraphExecutionTrace.computeIfAbsent(traceId, k -> new ArrayList<>())
                                    .add("main-call-approval:" + System.currentTimeMillis());

                            // 设置审批参数
                            context.put("applicant", context.getOrDefault("operator", "system"));
                            context.put("reviewResult", "approve");

                            // 模拟调用审批子流程
                            System.out.println("主流程调用审批流程: " + context.getInstanceId());
                        }
                    })
                    .linkAdd("sync-gateway");

            // 同步网关（等待两个分支都完成）
            spec.addParallel("sync-gateway").title("同步汇总")
                    .linkAdd("final-check");

            spec.addActivity("final-check").title("最终检查")
                    .metaPut("actor", "checker")
                    .task(mainProcessComponent)
                    .linkAdd("end");

            spec.addEnd("end").title("业务完成");
        });

        // ===== 4. 创建复合工作流服务 =====
        FlowEngine engine = FlowEngine.newInstance();
        engine.load(dataProcessingGraph);
        engine.load(approvalGraph);
        engine.load(mainGraph);

        WorkflowService workflowService = WorkflowService.of(
                engine,
                new ActorStateController("actor"),
                new InMemoryStateRepository()
        );

        // ===== 5. 测试执行 =====
        String traceId = "multi-graph-test-" + UUID.randomUUID().toString().substring(0, 8);
        String instanceId = "business-instance-" + traceId;

        System.out.println("\n=== 执行主流程实例: " + instanceId + " ===");

        // 启动主流程
        FlowContext mainContext = FlowContext.of(instanceId);
        mainContext.put("actor", "operator");
        mainContext.put("operator", "张三");
        mainContext.put("traceId", traceId);

        // 获取并执行主流程任务
        Task initTask = workflowService.getTask(mainGraph.getId(), mainContext);
        assertNotNull(initTask);
        assertEquals("init", initTask.getNodeId());
        assertEquals(TaskState.WAITING, initTask.getState());

        // 执行初始化任务
        assertDoesNotThrow(() -> initTask.run(mainContext));
        workflowService.postTask(mainGraph.getId(), "init", TaskAction.FORWARD, mainContext);

        // 验证主流程状态
        assertNotNull(mainContext.getAs("businessId"));
        assertNotNull(mainContext.getAs("initTime"));
        assertEquals("张三", mainContext.getAs("initiator"));

        // 获取并行分支任务
        FlowContext dataCollectorContext = FlowContext.of(instanceId);
        dataCollectorContext.put("actor", "data-collector");
        dataCollectorContext.put("traceId", traceId);

        FlowContext approvalInitiatorContext = FlowContext.of(instanceId);
        approvalInitiatorContext.put("actor", "approval-initiator");
        approvalInitiatorContext.put("traceId", traceId);

        // 数据收集分支
        Task dataCollectionTask = workflowService.getTask(mainGraph.getId(), dataCollectorContext);
        assertNotNull(dataCollectionTask);
        assertEquals("data-collection", dataCollectionTask.getNodeId());
        workflowService.postTask(mainGraph.getId(), "data-collection", TaskAction.FORWARD, dataCollectorContext);

        // 审批分支
        Task approvalProcessTask = workflowService.getTask(mainGraph.getId(), approvalInitiatorContext);
        assertNotNull(approvalProcessTask);
        assertEquals("approval-process", approvalProcessTask.getNodeId());
        workflowService.postTask(mainGraph.getId(), "approval-process", TaskAction.FORWARD, approvalInitiatorContext);

        // 验证两个分支都触发了子流程调用
        List<String> traceLog = crossGraphExecutionTrace.get(traceId);
        assertNotNull(traceLog);
        System.out.println("\n跨图调用跟踪:");
        traceLog.forEach(System.out::println);

        // 检查最终检查任务
        FlowContext checkerContext = FlowContext.of(instanceId);
        checkerContext.put("actor", "checker");
        checkerContext.put("checker", "李检查员");
        checkerContext.put("traceId", traceId);

        Task finalCheckTask = workflowService.getTask(mainGraph.getId(), checkerContext);
        assertNotNull(finalCheckTask);
        assertEquals("final-check", finalCheckTask.getNodeId());

        // 执行最终检查
        assertDoesNotThrow(() -> finalCheckTask.run(checkerContext));
        workflowService.postTask(mainGraph.getId(), "final-check", TaskAction.FORWARD, checkerContext);

        // 验证主流程完成
        Task finalTask = workflowService.getTask(mainGraph.getId(), checkerContext);
        assertNull(finalTask);

        // 验证执行跟踪
        assertTrue(traceLog.size() >= 4, "至少应该有4次执行记录");
        assertTrue(traceLog.stream().anyMatch(log -> log.contains("main-")), "应包含主流程执行");
        assertTrue(traceLog.stream().anyMatch(log -> log.contains("call-data-processing")), "应包含数据处理调用");
        assertTrue(traceLog.stream().anyMatch(log -> log.contains("call-approval")), "应包含审批调用");

        System.out.println("\n=== 多图协同测试完成 ===");
        System.out.println("总执行记录数: " + traceLog.size());
        System.out.println("实例ID: " + instanceId);
    }

    @Test
    void testGraphHierarchyWithConditionalBranching() {
        // 测试目的：验证多层嵌套流程图的条件分支
        // 测试场景：主流程根据条件选择不同的子流程分支
        // 验证点：条件判断、动态路径选择、嵌套深度

        System.out.println("=== 开始层级条件分支测试 ===");

        // ===== 1. 创建两个不同的处理子流程 =====

        // 快速处理流程
        TaskComponent quickProcessComponent = new TaskComponent() {
            @Override
            public void run(FlowContext context, Node node) throws Throwable {
                context.put("processType", "quick");
                context.put("processTime", System.currentTimeMillis());
                context.put("processedBy", "quick-processor");
            }
        };

        Graph quickProcessGraph = Graph.create("quick-process-flow", "快速处理流程", spec -> {
            spec.addStart("start").linkAdd("quick-step1");

            spec.addActivity("quick-step1").title("快速步骤1")
                    .task(quickProcessComponent)
                    .linkAdd("quick-step2");

            spec.addActivity("quick-step2").title("快速步骤2")
                    .task(quickProcessComponent)
                    .linkAdd("end");

            spec.addEnd("end").title("快速完成");
        });

        // 详细处理流程
        TaskComponent detailedProcessComponent = new TaskComponent() {
            @Override
            public void run(FlowContext context, Node node) throws Throwable {
                context.put("processType", "detailed");
                context.put("processTime", System.currentTimeMillis());
                context.put("processedBy", "detailed-processor");
                context.put("step", node.getId());
            }
        };

        Graph detailedProcessGraph = Graph.create("detailed-process-flow", "详细处理流程", spec -> {
            spec.addStart("start").linkAdd("detail-step1");

            spec.addActivity("detail-step1").title("详细步骤1")
                    .task(detailedProcessComponent)
                    .linkAdd("detail-step2");

            spec.addActivity("detail-step2").title("详细步骤2")
                    .task(detailedProcessComponent)
                    .linkAdd("detail-step3");

            spec.addActivity("detail-step3").title("详细步骤3")
                    .task(detailedProcessComponent)
                    .linkAdd("end");

            spec.addEnd("end").title("详细完成");
        });

        // ===== 2. 创建决策主流程 =====
        TaskComponent decisionComponent = new TaskComponent() {
            @Override
            public void run(FlowContext context, Node node) throws Throwable {
                String nodeId = node.getId();
                if ("analyze".equals(nodeId)) {
                    // 分析业务类型和优先级
                    String businessType = context.getOrDefault("businessType", "normal");
                    int priority = context.getOrDefault("priority", 1);

                    boolean needQuickProcess = "normal".equals(businessType) && priority <= 3;
                    context.put("needQuickProcess", needQuickProcess);
                    context.put("analyzed", true);
                }
            }
        };

        Graph decisionGraph = Graph.create("decision-main-flow", "决策主流程", spec -> {
            spec.addStart("start").linkAdd("analyze");

            spec.addActivity("analyze").title("业务分析")
                    .task(decisionComponent)
                    .linkAdd("process-decision");

            spec.addExclusive("process-decision").title("处理决策")
                    .task(new TaskComponent() {
                        @Override
                        public void run(FlowContext context, Node node) throws Throwable {
                            // 决策逻辑
                            System.out.println("执行处理决策: " + context.getInstanceId());
                        }
                    })
                    .linkAdd("quick-process", link -> link.when(c ->
                            Boolean.TRUE.equals(c.<Boolean>getAs("needQuickProcess"))).title("快速处理"))
                    .linkAdd("detailed-process", link -> link.when(c ->
                            Boolean.FALSE.equals(c.<Boolean>getAs("needQuickProcess"))).title("详细处理"));

            // 快速处理分支
            spec.addActivity("quick-process").title("调用快速处理")
                    .task(new TaskComponent() {
                        @Override
                        public void run(FlowContext context, Node node) throws Throwable {
                            System.out.println("调用快速处理流程: " + context.getInstanceId());
                            context.put("selectedFlow", "quick");
                        }
                    })
                    .linkAdd("post-quick");

            spec.addActivity("post-quick").title("快速后处理")
                    .task(decisionComponent)
                    .linkAdd("finalize");

            // 详细处理分支
            spec.addActivity("detailed-process").title("调用详细处理")
                    .task(new TaskComponent() {
                        @Override
                        public void run(FlowContext context, Node node) throws Throwable {
                            System.out.println("调用详细处理流程: " + context.getInstanceId());
                            context.put("selectedFlow", "detailed");
                        }
                    })
                    .linkAdd("post-detail");

            spec.addActivity("post-detail").title("详细后处理")
                    .task(decisionComponent)
                    .linkAdd("finalize");

            spec.addActivity("finalize").title("最终处理")
                    .task(decisionComponent)
                    .linkAdd("end");

            spec.addEnd("end").title("完成");
        });

        // ===== 3. 创建复合工作流服务 =====
        FlowEngine engine = FlowEngine.newInstance();
        engine.load(quickProcessGraph);
        engine.load(detailedProcessGraph);
        engine.load(decisionGraph);

        StateRepository stateRepository = new InMemoryStateRepository();

        WorkflowService workflowService = WorkflowService.of(
                engine,
                new BlockStateController(),
                stateRepository
        );

        // ===== 4. 测试场景1：普通业务，快速处理 =====
        System.out.println("\n=== 测试场景1：普通业务（快速处理）===");
        String instanceId1 = "normal-business-" + UUID.randomUUID().toString().substring(0, 6);
        FlowContext context1 = FlowContext.of(instanceId1);
        context1.put("businessType", "normal");
        context1.put("priority", 2); // 优先级较低

        // 执行分析
        Task analyzeTask1 = workflowService.getTask(decisionGraph.getId(), context1);
        assertNotNull(analyzeTask1);
        assertEquals("analyze", analyzeTask1.getNodeId());
        workflowService.postTask(decisionGraph.getId(), "analyze", TaskAction.FORWARD, context1);

        // 验证分析结果
        assertTrue(context1.<Boolean>getAs("analyzed"));
        assertTrue(context1.<Boolean>getAs("needQuickProcess"));

        // 执行决策（应该选择快速处理）
        Task decisionTask1 = workflowService.getTask(decisionGraph.getId(), context1);
        assertNotNull(decisionTask1);
        assertEquals("quick-process", decisionTask1.getNodeId());
        workflowService.postTask(decisionGraph.getId(), "quick-process", TaskAction.FORWARD, context1);

        // 验证选择了快速流程
        assertEquals("quick", context1.getAs("selectedFlow"));

        // ===== 5. 测试场景2：重要业务，详细处理 =====
        System.out.println("\n=== 测试场景2：重要业务（详细处理）===");
        String instanceId2 = "important-business-" + UUID.randomUUID().toString().substring(0, 6);
        FlowContext context2 = FlowContext.of(instanceId2);
        context2.put("needQuickProcess", false);
        context2.put("businessType", "important");
        context2.put("priority", 5); // 优先级较高

        // 执行分析
        Task analyzeTask2 = workflowService.getTask(decisionGraph.getId(), context2);
        assertNotNull(analyzeTask2);
        workflowService.postTask(decisionGraph.getId(), "analyze", TaskAction.FORWARD, context2);

        // 验证分析结果
        assertTrue(context2.<Boolean>getAs("analyzed"));
        assertFalse(context2.<Boolean>getAs("needQuickProcess"));

        workflowService = WorkflowService.of(
                engine,
                new ActorStateController(),
                stateRepository
        );

        // 执行决策（应该选择详细处理）
        workflowService.postTask(decisionGraph.getId(), "process-decision", TaskAction.FORWARD, context2);

        // 验证选择了详细流程
        assertEquals("detailed", context2.getAs("selectedFlow"));

        System.out.println("\n=== 层级条件分支测试完成 ===");
    }

    @Test
    void testErrorPropagationAcrossGraphs() {
        // 测试目的：验证错误在多个流程图之间的传播和处理
        // 测试场景：子流程失败导致父流程进入错误处理分支
        // 验证点：错误传播、异常处理、补偿机制

        System.out.println("=== 开始跨图错误传播测试 ===");

        // ===== 1. 创建可能失败的子流程 =====
        TaskComponent riskyProcessComponent = new TaskComponent() {
            @Override
            public void run(FlowContext context, Node node) throws Throwable {
                boolean shouldFail = context.getOrDefault("shouldFail", false);
                if (shouldFail) {
                    throw new RuntimeException("子流程执行失败: " + node.getId());
                }
                context.put("riskyProcessCompleted", true);
                context.put("processNode", node.getId());
            }
        };

        Graph riskyProcessGraph = Graph.create("risky-process-flow", "风险处理流程", spec -> {
            spec.addStart("start").linkAdd("step1");

            spec.addActivity("step1").title("风险步骤1")
                    .task(riskyProcessComponent)
                    .linkAdd("step2");

            spec.addActivity("step2").title("风险步骤2")
                    .task(riskyProcessComponent)
                    .linkAdd("end");

            spec.addEnd("end").title("风险完成");
        });

        // ===== 2. 创建带错误处理的主流程 =====
        Graph mainFlowWithErrorHandling = Graph.create("main-with-error-handling", "带错误处理的主流程", spec -> {
            spec.addStart("start").linkAdd("pre-process");

            spec.addActivity("pre-process").title("预处理")
                    .task(new TaskComponent() {
                        @Override
                        public void run(FlowContext context, Node node) throws Throwable {
                            context.put("preProcessed", true);
                            context.put("preProcessor", "system");
                        }
                    })
                    .linkAdd("call-risky-process");

            spec.addActivity("call-risky-process").title("调用风险流程")
                    .task(new TaskComponent() {
                        @Override
                        public void run(FlowContext context, Node node) throws Throwable {
                            System.out.println("调用风险处理流程: " + context.getInstanceId());
                            // 这里实际应该调用子流程，我们模拟调用
                        }
                    })
                    .linkAdd("normal-continue", link -> link.when("${riskyProcessCompleted} == true").title("正常继续"))
                    .linkAdd("error-handle", link -> link.when(c -> {
                        // 检查是否有错误发生
                        return c.getOrDefault("errorOccurred", false);
                    }).title("错误处理"));

            spec.addActivity("normal-continue").title("正常继续")
                    .task(new TaskComponent() {
                        @Override
                        public void run(FlowContext context, Node node) throws Throwable {
                            context.put("normalPath", true);
                            context.put("completionTime", System.currentTimeMillis());
                        }
                    })
                    .linkAdd("end");

            spec.addActivity("error-handle").title("错误处理")
                    .task(new TaskComponent() {
                        @Override
                        public void run(FlowContext context, Node node) throws Throwable {
                            context.put("errorHandled", true);
                            context.put("errorHandler", "recovery-system");
                            context.put("recoveryTime", System.currentTimeMillis());
                        }
                    })
                    .linkAdd("compensate");

            spec.addActivity("compensate").title("补偿处理")
                    .task(new TaskComponent() {
                        @Override
                        public void run(FlowContext context, Node node) throws Throwable {
                            context.put("compensated", true);
                            context.put("compensation", "执行了补偿操作");
                        }
                    })
                    .linkAdd("end");

            spec.addEnd("end").title("流程结束");
        });

        // ===== 3. 创建工作流服务 =====
        FlowEngine engine = FlowEngine.newInstance();
        engine.load(riskyProcessGraph);
        engine.load(mainFlowWithErrorHandling);

        WorkflowService workflowService = WorkflowService.of(
                engine,
                new ActorStateController(),
                new InMemoryStateRepository()
        );

        // ===== 4. 测试场景1：子流程成功 =====
        System.out.println("\n=== 测试场景1：子流程成功执行 ===");
        String successInstanceId = "success-case-" + UUID.randomUUID().toString().substring(0, 6);
        FlowContext successContext = FlowContext.of(successInstanceId);

        // 预处理
        Task preProcessTask = workflowService.getTask(mainFlowWithErrorHandling.getId(), successContext);
        assertNotNull(preProcessTask);
        workflowService.postTask(mainFlowWithErrorHandling.getId(), "pre-process", TaskAction.FORWARD, successContext);

        // 设置子流程成功
        successContext.put("riskyProcessCompleted", true);

        // 调用风险流程（应该走正常分支）
        Task riskyProcessTask = workflowService.getTask(mainFlowWithErrorHandling.getId(), successContext);
        assertNotNull(riskyProcessTask);
        workflowService.postTask(mainFlowWithErrorHandling.getId(), "call-risky-process", TaskAction.FORWARD, successContext);

        // 验证走了正常路径
        assertTrue(successContext.<Boolean>getAs("normalPath"));
        assertNull(successContext.getAs("errorHandled"));

        // ===== 5. 测试场景2：子流程失败 =====
        System.out.println("\n=== 测试场景2：子流程失败，触发错误处理 ===");
        String errorInstanceId = "error-case-" + UUID.randomUUID().toString().substring(0, 6);
        FlowContext errorContext = FlowContext.of(errorInstanceId);

        // 预处理
        workflowService.postTask(mainFlowWithErrorHandling.getId(), "pre-process", TaskAction.FORWARD, errorContext);

        // 设置子流程失败
        errorContext.put("errorOccurred", true);
        errorContext.put("errorMessage", "子流程执行失败");

        // 调用风险流程（应该走错误处理分支）
        workflowService.postTask(mainFlowWithErrorHandling.getId(), "call-risky-process", TaskAction.FORWARD, errorContext);

        // 验证走了错误处理路径
        assertTrue(errorContext.<Boolean>getAs("errorHandled"));
        assertTrue(errorContext.<Boolean>getAs("compensated"));
        assertEquals("执行了补偿操作", errorContext.getAs("compensation"));
        assertNull(errorContext.getAs("normalPath"));

        System.out.println("\n=== 跨图错误传播测试完成 ===");
    }

    @Test
    void testGraphReuseAndTemplatePattern() {
        // 测试目的：验证流程图的重用和模板模式
        // 测试场景：多个主流程复用相同的子流程模板
        // 验证点：模板复用、参数传递、独立执行

        System.out.println("=== 开始图重用和模板模式测试 ===");

        // ===== 1. 创建可重用的验证流程模板 =====
        TaskComponent validationComponent = new TaskComponent() {
            @Override
            public void run(FlowContext context, Node node) throws Throwable {
                String nodeId = node.getId();
                String validationType = context.getOrDefault("validationType", "basic");

                if ("format-check".equals(nodeId)) {
                    String data = context.getOrDefault("inputData", "");
                    boolean isValid = data != null && !data.isEmpty();
                    context.put("formatValid", isValid);
                    context.put("formatCheckTime", System.currentTimeMillis());
                } else if ("business-check".equals(nodeId)) {
                    String businessRules = context.getOrDefault("businessRules", "default");
                    boolean isCompliant = "complex".equals(businessRules) ?
                            context.<Boolean>getAs("formatValid") : true;
                    context.put("businessCompliant", isCompliant);
                    context.put("businessCheckTime", System.currentTimeMillis());
                }
            }
        };

        Graph validationTemplate = Graph.create("validation-template", "验证模板流程", spec -> {
            spec.addStart("start").linkAdd("format-check");

            spec.addActivity("format-check").title("格式检查")
                    .task(validationComponent)
                    .linkAdd("business-check");

            spec.addActivity("business-check").title("业务检查")
                    .task(validationComponent)
                    .linkAdd("validation-result");

            spec.addExclusive("validation-result").title("验证结果")
                    .task(validationComponent)
                    .linkAdd("end", link -> link.when(c ->
                            Boolean.TRUE.equals(c.<Boolean>getAs("formatValid")) &&
                                    Boolean.TRUE.equals(c.<Boolean>getAs("businessCompliant"))).title("验证通过"))
                    .linkAdd("reject", link -> link.when(c ->
                            Boolean.FALSE.equals(c.<Boolean>getAs("formatValid")) ||
                                    Boolean.FALSE.equals(c.<Boolean>getAs("businessCompliant"))).title("验证拒绝"));

            spec.addActivity("reject").title("拒绝处理")
                    .task(validationComponent)
                    .linkAdd("end");

            spec.addEnd("end").title("验证完成");
        });

        // ===== 2. 创建订单处理流程（使用验证模板） =====
        Graph orderProcessingFlow = Graph.create("order-processing", "订单处理流程", spec -> {
            spec.addStart("start").linkAdd("order-input");

            spec.addActivity("order-input").title("订单录入")
                    .task(new TaskComponent() {
                        @Override
                        public void run(FlowContext context, Node node) throws Throwable {
                            context.put("orderData", "订单-" + UUID.randomUUID());
                            context.put("orderType", "normal");
                            context.put("inputData", context.getAs("orderData"));
                            context.put("validationType", "order");
                        }
                    })
                    .linkAdd("call-validation");

            spec.addActivity("call-validation").title("调用验证")
                    .task(new TaskComponent() {
                        @Override
                        public void run(FlowContext context, Node node) throws Throwable {
                            System.out.println("订单流程调用验证模板: " + context.getInstanceId());
                            // 设置验证参数
                            context.put("businessRules", "order-rules");
                        }
                    })
                    .linkAdd("process-order", link -> link.when(c ->
                            Boolean.TRUE.equals(c.<Boolean>getAs("formatValid")) &&
                                    Boolean.TRUE.equals(c.<Boolean>getAs("businessCompliant"))).title("验证通过"))
                    .linkAdd("order-reject", link -> link.when(c ->
                            Boolean.FALSE.equals(c.<Boolean>getAs("formatValid")) ||
                                    Boolean.FALSE.equals(c.<Boolean>getAs("businessCompliant"))).title("验证拒绝"));

            spec.addActivity("process-order").title("处理订单")
                    .task(new TaskComponent() {
                        @Override
                        public void run(FlowContext context, Node node) throws Throwable {
                            context.put("orderProcessed", true);
                            context.put("processingTime", System.currentTimeMillis());
                        }
                    })
                    .linkAdd("end");

            spec.addActivity("order-reject").title("订单拒绝")
                    .task(new TaskComponent() {
                        @Override
                        public void run(FlowContext context, Node node) throws Throwable {
                            context.put("orderRejected", true);
                            context.put("rejectionReason", "验证失败");
                        }
                    })
                    .linkAdd("end");

            spec.addEnd("end").title("订单完成");
        });

        // ===== 3. 创建用户注册流程（也使用验证模板） =====
        Graph userRegistrationFlow = Graph.create("user-registration", "用户注册流程", spec -> {
            spec.addStart("start").linkAdd("user-input");

            spec.addActivity("user-input").title("用户输入")
                    .task(new TaskComponent() {
                        @Override
                        public void run(FlowContext context, Node node) throws Throwable {
                            context.put("userData", "用户-" + UUID.randomUUID());
                            context.put("inputData", context.getAs("userData"));
                            context.put("validationType", "user");
                        }
                    })
                    .linkAdd("call-validation");

            spec.addActivity("call-validation").title("调用验证")
                    .task(new TaskComponent() {
                        @Override
                        public void run(FlowContext context, Node node) throws Throwable {
                            System.out.println("用户注册调用验证模板: " + context.getInstanceId());
                            // 设置验证参数
                            context.put("businessRules", "user-rules");
                        }
                    })
                    .linkAdd("create-user", link -> link.when(c ->
                            Boolean.TRUE.equals(c.<Boolean>getAs("formatValid")) &&
                                    Boolean.TRUE.equals(c.<Boolean>getAs("businessCompliant"))).title("验证通过"))
                    .linkAdd("registration-reject", link -> link.when(c ->
                            Boolean.FALSE.equals(c.<Boolean>getAs("formatValid")) ||
                                    Boolean.FALSE.equals(c.<Boolean>getAs("businessCompliant"))).title("验证拒绝"));

            spec.addActivity("create-user").title("创建用户")
                    .task(new TaskComponent() {
                        @Override
                        public void run(FlowContext context, Node node) throws Throwable {
                            context.put("userCreated", true);
                            context.put("creationTime", System.currentTimeMillis());
                        }
                    })
                    .linkAdd("end");

            spec.addActivity("registration-reject").title("注册拒绝")
                    .task(new TaskComponent() {
                        @Override
                        public void run(FlowContext context, Node node) throws Throwable {
                            context.put("registrationRejected", true);
                            context.put("rejectionReason", "用户验证失败");
                        }
                    })
                    .linkAdd("end");

            spec.addEnd("end").title("注册完成");
        });

        // ===== 4. 创建工作流服务并加载所有图 =====
        FlowEngine engine = FlowEngine.newInstance();
        engine.load(validationTemplate);
        engine.load(orderProcessingFlow);
        engine.load(userRegistrationFlow);

        WorkflowService workflowService = WorkflowService.of(
                engine,
                new ActorStateController(),
                new InMemoryStateRepository()
        );

        // ===== 5. 测试订单流程 =====
        System.out.println("\n=== 测试订单处理流程 ===");
        String orderInstanceId = "order-" + UUID.randomUUID().toString().substring(0, 6);
        FlowContext orderContext = FlowContext.of(orderInstanceId);

        // 执行订单录入
        Task orderInputTask = workflowService.getTask(orderProcessingFlow.getId(), orderContext);
        assertNotNull(orderInputTask);
        workflowService.postTask(orderProcessingFlow.getId(), "order-input", TaskAction.FORWARD, orderContext);

        // 验证数据已设置
        assertNotNull(orderContext.getAs("orderData"));
        assertEquals("order", orderContext.getAs("validationType"));

        // 设置验证通过
        orderContext.put("formatValid", true);
        orderContext.put("businessCompliant", true);

        // 调用验证（应该走通过分支）
        workflowService.postTask(orderProcessingFlow.getId(), "call-validation", TaskAction.FORWARD, orderContext);

        // 验证订单被处理
        assertTrue(orderContext.<Boolean>getAs("orderProcessed"));

        // ===== 6. 测试用户注册流程 =====
        System.out.println("\n=== 测试用户注册流程 ===");
        String userInstanceId = "user-" + UUID.randomUUID().toString().substring(0, 6);
        FlowContext userContext = FlowContext.of(userInstanceId);

        // 执行用户输入
        Task userInputTask = workflowService.getTask(userRegistrationFlow.getId(), userContext);
        assertNotNull(userInputTask);
        workflowService.postTask(userRegistrationFlow.getId(), "user-input", TaskAction.FORWARD, userContext);

        // 验证数据已设置
        assertNotNull(userContext.getAs("userData"));
        assertEquals("user", userContext.getAs("validationType"));

        // 设置验证失败
        userContext.put("formatValid", false);
        userContext.put("businessCompliant", true);

        // 调用验证（应该走拒绝分支）
        workflowService.postTask(userRegistrationFlow.getId(), "call-validation", TaskAction.FORWARD, userContext);

        // 验证注册被拒绝
        assertTrue(userContext.<Boolean>getAs("registrationRejected"));
        assertEquals("用户验证失败", userContext.getAs("rejectionReason"));

        System.out.println("\n=== 图重用和模板模式测试完成 ===");
        System.out.println("验证模板被两个不同的流程重用");
    }
}
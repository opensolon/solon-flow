package features.workflow.generated;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.noear.solon.flow.*;
import org.noear.solon.flow.workflow.Task;
import org.noear.solon.flow.workflow.TaskAction;
import org.noear.solon.flow.workflow.TaskState;
import org.noear.solon.flow.workflow.WorkflowService;
import org.noear.solon.flow.workflow.controller.ActorStateController;
import org.noear.solon.flow.workflow.repository.InMemoryStateRepository;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 模拟生产测试 - 复杂业务场景、状态持久化、异常处理（不使用Mock）
 */
class WorkflowServiceProductionTest {

    private FlowEngine flowEngine;
    private WorkflowService workflowService;
    private Graph approvalGraph;
    private Graph complexParallelGraph;

    // 记录任务执行历史
    private Map<String, List<String>> taskExecutionHistory = new ConcurrentHashMap<>();

    // 自定义任务组件，记录执行历史
    private TaskComponent recordingTaskComponent = new TaskComponent() {
        @Override
        public void run(FlowContext context, Node node) throws Throwable {
            String instanceId = context.getInstanceId();
            String nodeId = node.getId();

            taskExecutionHistory.computeIfAbsent(instanceId, k -> new ArrayList<>())
                    .add(nodeId + ":" + System.currentTimeMillis());

            // 模拟业务逻辑
            context.put(nodeId + "_executed", true);
            context.put(nodeId + "_executionTime", System.currentTimeMillis());

            // 模拟特定节点的业务逻辑
            if ("apply".equals(nodeId)) {
                context.put("applicationData", "申请数据-" + UUID.randomUUID());
            } else if ("review".equals(nodeId) || "manager-review".equals(nodeId)) {
                String result = context.getOrDefault("reviewResult", "approve").toString();
                context.put("reviewResult", result);
                context.put("reviewer", context.getOrDefault("reviewer", "system"));
            }
        }
    };

    // 模拟可能抛出异常的任务组件
    private TaskComponent errorProneTaskComponent = new TaskComponent() {
        @Override
        public void run(FlowContext context, Node node) throws Throwable {
            String instanceId = context.getInstanceId();
            boolean shouldFail = context.getOrDefault("shouldFail", false);

            if (shouldFail && "errorTask".equals(node.getId())) {
                throw new RuntimeException("模拟任务执行失败: " + instanceId);
            }

            context.put(node.getId() + "_completed", true);
        }
    };

    @BeforeEach
    void setUp() {
        // 清空执行历史
        taskExecutionHistory.clear();

        // 创建审批流程图：开始 -> 申请 -> 审批 -> (通过->结束, 驳回->修改申请)
        approvalGraph = Graph.create("approval-process", "审批流程", spec -> {
            spec.addStart("start").title("开始").linkAdd("apply");

            // 申请节点
            spec.addActivity("apply").title("提交申请")
                    .metaPut("actor", "applicant")
                    .task(recordingTaskComponent)
                    .linkAdd("review");

            // 审批节点（排他网关）
            spec.addExclusive("review").title("审批节点")
                    .metaPut("actor", "reviewer")
                    .task(recordingTaskComponent)
                    .linkAdd("end", link -> link.when(c->"approve".equals(c.getAs("reviewResult"))).title("通过")) //"reviewResult.equals(\"approve\")"
                    .linkAdd("apply", link -> link.when(c->"reject".equals(c.getAs("reviewResult"))).title("驳回")); //"reviewResult.equals(\"reject\")"

            spec.addEnd("end").title("审批完成");
        });

        // 创建复杂并行流程图
        complexParallelGraph = Graph.create("parallel-process", "并行流程", spec -> {
            spec.addStart("start").title("开始").linkAdd("parallel-gateway");

            // 并行网关分出3个任务
            spec.addParallel("parallel-gateway").title("并行网关")
                    .linkAdd("task1")
                    .linkAdd("task2")
                    .linkAdd("task3");

            // 三个并行任务
            spec.addActivity("task1").title("任务1")
                    .metaPut("actor", "user1")
                    .metaPut("department", "sales")
                    .task(recordingTaskComponent)
                    .linkAdd("inclusive-gateway");

            spec.addActivity("task2").title("任务2")
                    .metaPut("actor", "user2")
                    .metaPut("department", "finance")
                    .task(recordingTaskComponent)
                    .linkAdd("inclusive-gateway");

            spec.addActivity("task3").title("任务3")
                    .metaPut("actor", "user3")
                    .metaPut("department", "hr")
                    .task(recordingTaskComponent)
                    .linkAdd("inclusive-gateway");

            // 包容网关
            spec.addInclusive("inclusive-gateway").title("包容网关")
                    .linkAdd("consolidate", link -> link.when("true").title("汇总"))
                    .linkAdd("approval", link -> link.when("${needApproval} == true").title("需要审批"));

            // 汇总任务
            spec.addActivity("consolidate").title("数据汇总")
                    .metaPut("actor", "admin")
                    .task(recordingTaskComponent)
                    .linkAdd("end");

            // 审批任务
            spec.addActivity("approval").title("主管审批")
                    .metaPut("actor", "manager")
                    .task(recordingTaskComponent)
                    .linkAdd("consolidate");

            spec.addEnd("end").title("流程结束");
        });

        // 创建错误处理流程图
        Graph errorHandlingGraph = Graph.create("error-process", "错误处理流程", spec -> {
            spec.addStart("start").linkAdd("normalTask");

            spec.addActivity("normalTask").title("正常任务")
                    .task(recordingTaskComponent)
                    .linkAdd("errorTask");

            spec.addActivity("errorTask").title("可能失败的任务")
                    .task(errorProneTaskComponent)
                    .linkAdd("end");

            spec.addEnd("end").title("结束");
        });

        // 创建流引擎并加载所有图
        flowEngine = FlowEngine.newInstance();
        flowEngine.load(approvalGraph);
        flowEngine.load(complexParallelGraph);
        flowEngine.load(errorHandlingGraph);

        // 使用 InMemoryStateRepository 模拟生产环境的状态存储
        workflowService = WorkflowService.of(
                flowEngine,
                new ActorStateController("actor"),
                new InMemoryStateRepository()
        );
    }

    @Test
    void testCompleteApprovalProcessWithRealComponents() {
        // 模拟申请人
        String instanceId = "request-" + UUID.randomUUID().toString().substring(0, 8);
        FlowContext applicantContext = FlowContext.of(instanceId);
        applicantContext.put("actor", "applicant");
        applicantContext.put("applicantName", "张三");
        applicantContext.put("requestAmount", 5000);

        // 模拟审批人
        FlowContext reviewerContext = FlowContext.of(instanceId);
        reviewerContext.put("actor", "reviewer");
        reviewerContext.put("reviewerName", "李审批员");
        reviewerContext.put("reviewResult", "approve"); // 审批通过

        // 1. 申请人提交申请
        Task applicantTask = workflowService.getTask("approval-process", applicantContext);
        assertNotNull(applicantTask);
        assertEquals("apply", applicantTask.getNodeId());
        assertEquals(TaskState.WAITING, applicantTask.getState());

        // 运行申请任务
        assertDoesNotThrow(() -> applicantTask.run(applicantContext));

        // 验证任务执行效果
        assertTrue(applicantContext.getAs("apply_executed"));
        assertNotNull(applicantContext.getAs("applicationData"));

        // 提交申请
        workflowService.postTask("approval-process", "apply", TaskAction.FORWARD, applicantContext);

        // 2. 审批人审批
        Task reviewerTask = workflowService.getTask("approval-process", reviewerContext);
        assertNotNull(reviewerTask);
        assertEquals("review", reviewerTask.getNodeId());

        // 运行审批任务
        assertDoesNotThrow(() -> reviewerTask.run(reviewerContext));

        // 验证审批任务执行效果
        assertTrue(reviewerContext.getAs("review_executed"));
        assertEquals("approve", reviewerContext.getAs("reviewResult"));

        // 提交审批通过
        workflowService.postTask("approval-process", "review", TaskAction.FORWARD, reviewerContext);

        // 3. 验证流程完成
        Task finalTask = workflowService.getTask("approval-process", reviewerContext);
        assertNull(finalTask);

        // 验证执行历史记录
        List<String> history = taskExecutionHistory.get(instanceId);
        assertNotNull(history);
        assertTrue(history.size() >= 2); // 至少执行了apply和review

        // 验证状态持久化
        Node applyNode = approvalGraph.getNode("apply");
        Node reviewNode = approvalGraph.getNode("review");

        assertEquals(TaskState.COMPLETED, workflowService.getState(applyNode, applicantContext));
        assertEquals(TaskState.COMPLETED, workflowService.getState(reviewNode, reviewerContext));

        System.out.println("审批流程执行历史: " + history);
    }

    @Test
    void testRejectionAndResubmissionWorkflow() {
        String instanceId = "reject-test-" + UUID.randomUUID().toString().substring(0, 6);

        FlowContext applicantContext = FlowContext.of(instanceId);
        applicantContext.put("actor", "applicant");
        applicantContext.put("requestDetails", "初始申请内容");

        FlowContext reviewerContext = FlowContext.of(instanceId);
        reviewerContext.put("actor", "reviewer");

        // 第一轮：提交申请
        workflowService.getTask("approval-process", applicantContext);
        workflowService.postTask("approval-process", "apply", TaskAction.FORWARD, applicantContext);

        // 第一轮：审批驳回
        reviewerContext.put("reviewResult", "reject");
        reviewerContext.put("rejectionReason", "信息不完整");
        workflowService.getTask("approval-process", reviewerContext);
        workflowService.postTask("approval-process", "review", TaskAction.FORWARD, reviewerContext);

        // 验证流程回到申请节点
        Task taskAfterRejection = workflowService.getTask("approval-process", applicantContext);
        assertNotNull(taskAfterRejection);
        assertEquals("apply", taskAfterRejection.getNodeId());
        assertEquals(TaskState.WAITING, taskAfterRejection.getState());

        // 第二轮：修改后重新提交
        applicantContext.put("requestDetails", "修改后的申请内容");
        applicantContext.put("isResubmitted", true);
        workflowService.postTask("approval-process", "apply", TaskAction.FORWARD, applicantContext);

        // 第二轮：审批通过
        reviewerContext.put("reviewResult", "approve");
        reviewerContext.put("approvalComment", "信息已完善，批准");
        workflowService.postTask("approval-process", "review", TaskAction.FORWARD, reviewerContext);

        // 验证流程完成
        Task finalTask = workflowService.getTask("approval-process", reviewerContext);
        assertNull(finalTask);

        // 验证执行历史
        List<String> history = taskExecutionHistory.get(instanceId);
        assertNotNull(history);
        // 应该执行了：apply(第一次), review(驳回), apply(第二次), review(通过)
        long applyCount = history.stream().filter(h -> h.startsWith("apply")).count();
        long reviewCount = history.stream().filter(h -> h.startsWith("review")).count();
        assertTrue(applyCount >= 2);
        assertTrue(reviewCount >= 2);

        System.out.println("驳回重提交流程执行历史: " + history);
    }

    @Test
    void testComplexParallelProcess() {
        String instanceId = "parallel-test-" + UUID.randomUUID().toString().substring(0, 6);

        // 测试不同用户访问同一流程实例
        Map<String, FlowContext> userContexts = new HashMap<>();
        String[] users = {"user1", "user2", "user3", "admin", "manager"};

        for (String user : users) {
            FlowContext context = FlowContext.of(instanceId);
            context.put("actor", user);
            context.put("department", user.equals("user1") ? "sales" :
                    user.equals("user2") ? "finance" :
                            user.equals("user3") ? "hr" : "other");
            userContexts.put(user, context);
        }

        // 1. 各个用户获取自己的任务
        Map<String, Task> userTasks = new HashMap<>();
        for (String user : users) {
            Task task = workflowService.getTask("parallel-process", userContexts.get(user));
            userTasks.put(user, task);
        }

        // user1, user2, user3 应该有任务，admin和manager暂时没有
        assertNotNull(userTasks.get("user1"));
        assertNotNull(userTasks.get("user2"));
        assertNotNull(userTasks.get("user3"));
        assertNull(userTasks.get("admin"));
        assertNull(userTasks.get("manager"));

        // 验证各个用户的任务正确性
        assertEquals("task1", userTasks.get("user1").getNodeId());
        assertEquals("task2", userTasks.get("user2").getNodeId());
        assertEquals("task3", userTasks.get("user3").getNodeId());

        // 2. 用户1完成任务1
        workflowService.postTask("parallel-process", "task1", TaskAction.FORWARD, userContexts.get("user1"));

        // 3. 用户2完成任务2
        workflowService.postTask("parallel-process", "task2", TaskAction.FORWARD, userContexts.get("user2"));

        // 4. 用户3完成任务3
        workflowService.postTask("parallel-process", "task3", TaskAction.FORWARD, userContexts.get("user3"));

        // 5. 检查管理员任务（汇总任务）
        Task adminTask = workflowService.getTask("parallel-process", userContexts.get("admin"));
        assertNotNull(adminTask);
        assertEquals("consolidate", adminTask.getNodeId());

        // 6. 管理员完成汇总任务
        userContexts.get("admin").put("consolidatedData", "汇总完成的数据");
        workflowService.postTask("parallel-process", "consolidate", TaskAction.FORWARD, userContexts.get("admin"));

        // 7. 验证流程完成
        Task finalTask = workflowService.getTask("parallel-process", userContexts.get("admin"));
        assertNull(finalTask);

        // 验证所有任务状态
        for (String user : Arrays.asList("user1", "user2", "user3", "admin")) {
            Node taskNode = complexParallelGraph.getNode(
                    user.equals("user1") ? "task1" :
                            user.equals("user2") ? "task2" :
                                    user.equals("user3") ? "task3" : "consolidate"
            );
            assertEquals(TaskState.COMPLETED,
                    workflowService.getState(taskNode, userContexts.get(user)));
        }

        System.out.println("并行流程执行完成，实例ID: " + instanceId);
    }

    @Test
    void testHighConcurrencyWorkflowAccess() throws InterruptedException {
        int threadCount = 10;
        int iterationsPerThread = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<String> completedInstances = Collections.synchronizedList(new ArrayList<>());

        // 并发任务
        Runnable concurrentTask = () -> {
            try {
                startLatch.await();

                for (int i = 0; i < iterationsPerThread; i++) {
                    try {
                        String instanceId = "concurrent-" + Thread.currentThread().getId() + "-" + i;
                        FlowContext context = FlowContext.of(instanceId);
                        context.put("actor", "applicant");
                        context.put("requestData", "测试数据-" + i);

                        // 获取并完成任务
                        Task task = workflowService.getTask("approval-process", context);
                        if (task != null) {
                            task.run(context);
                            workflowService.postTask("approval-process", task.getNodeId(),
                                    TaskAction.FORWARD, context);

                            completedInstances.add(instanceId);
                            successCount.incrementAndGet();
                        }

                        // 短暂休眠，模拟真实环境
                        Thread.sleep(ThreadLocalRandom.current().nextInt(10, 50));

                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                        System.err.println("并发任务异常: " + e.getMessage());
                    }
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                finishLatch.countDown();
            }
        };

        // 启动所有线程
        for (int i = 0; i < threadCount; i++) {
            executor.submit(concurrentTask);
        }

        // 同时开始执行
        startLatch.countDown();

        // 等待所有线程完成
        boolean completed = finishLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "并发测试应在10秒内完成");

        System.out.println("并发测试结果:");
        System.out.println("成功次数: " + successCount.get());
        System.out.println("失败次数: " + failureCount.get());
        System.out.println("完成实例数: " + completedInstances.size());

        // 验证至少有一定成功率
        assertTrue(successCount.get() > 0, "并发测试应有成功案例");
        assertTrue(failureCount.get() < threadCount * iterationsPerThread * 0.3,
                "失败率应低于30%");
    }

    @Test
    void testErrorHandlingAndRecovery() {
        String instanceId = "error-test-" + UUID.randomUUID().toString().substring(0, 6);

        FlowContext context = FlowContext.of(instanceId);

        // 测试正常流程
        System.out.println("=== 测试正常流程 ===");

        // 正常任务应该成功
        Task normalTask = workflowService.getTask("error-process", context);
        assertNotNull(normalTask);
        assertEquals("normalTask", normalTask.getNodeId());

        assertDoesNotThrow(() -> normalTask.run(context));
        workflowService.postTask("error-process", "normalTask", TaskAction.FORWARD, context);

        // 测试错误流程
        System.out.println("=== 测试错误流程 ===");

        context.put("shouldFail", true);

        Task errorTask = workflowService.getTask("error-process", context);
        assertNotNull(errorTask);
        assertEquals("errorTask", errorTask.getNodeId());

        // 任务执行应该抛出异常
        Exception exception = assertThrows(RuntimeException.class,
                () -> errorTask.run(context));
        assertTrue(exception.getMessage().contains("模拟任务执行失败"));

        // 即使任务执行失败，仍然可以提交（模拟生产环境的重试机制）
        System.out.println("=== 模拟重试机制 ===");

        context.put("shouldFail", false); // 清除失败标记
        workflowService.postTask("error-process", "errorTask", TaskAction.FORWARD, context);

        // 验证流程可以继续
        Task finalTask = workflowService.getTask("error-process", context);
        assertNull(finalTask, "流程应已完成");

        // 验证状态
        Node normalNode = flowEngine.getGraph("error-process").getNode("normalTask");
        Node errorNode = flowEngine.getGraph("error-process").getNode("errorTask");

        assertEquals(TaskState.COMPLETED, workflowService.getState(normalNode, context));
        assertEquals(TaskState.COMPLETED, workflowService.getState(errorNode, context));

        System.out.println("错误处理测试完成，实例ID: " + instanceId);
    }

    @Test
    void testMultipleInstanceIsolation() {
        // 创建多个独立的流程实例
        int instanceCount = 5;
        List<String> instanceIds = new ArrayList<>();
        List<FlowContext> contexts = new ArrayList<>();

        for (int i = 0; i < instanceCount; i++) {
            String instanceId = "multi-instance-" + i + "-" + UUID.randomUUID().toString().substring(0, 6);
            instanceIds.add(instanceId);

            FlowContext context = FlowContext.of(instanceId);
            context.put("actor", "applicant");
            context.put("instanceIndex", i);
            contexts.add(context);
        }

        // 并行处理所有实例
        for (int i = 0; i < instanceCount; i++) {
            FlowContext context = contexts.get(i);
            String instanceId = instanceIds.get(i);

            // 每个实例独立运行
            Task task = workflowService.getTask("approval-process", context);
            assertNotNull(task);
            assertEquals("apply", task.getNodeId());

            // 为不同实例设置不同的数据
            context.put("customData", "实例" + i + "的数据");

            // 运行并提交任务
            assertDoesNotThrow(() -> task.run(context));
            workflowService.postTask("approval-process", "apply", TaskAction.FORWARD, context);
        }

        // 验证实例间数据隔离
        for (int i = 0; i < instanceCount; i++) {
            FlowContext context = contexts.get(i);
            String expectedData = "实例" + i + "的数据";

            assertEquals(expectedData, context.getAs("customData"));

            // 验证执行历史隔离
            List<String> history = taskExecutionHistory.get(instanceIds.get(i));
            assertNotNull(history);
            assertTrue(history.stream().anyMatch(h -> h.startsWith("apply")));

            // 验证状态隔离
            Node applyNode = approvalGraph.getNode("apply");
            TaskState state = workflowService.getState(applyNode, context);
            assertEquals(TaskState.COMPLETED, state);
        }

        System.out.println("多实例隔离测试完成，共测试 " + instanceCount + " 个实例");
    }

    @Test
    void testWorkflowWithConditionalBranching() {
        String instanceId = "conditional-test-" + UUID.randomUUID().toString().substring(0, 6);

        // 测试不同条件的流程分支
        Map<String, Object> testCases = new LinkedHashMap<>();
        testCases.put("小额申请（<=5000）", 3000);
        testCases.put("大额申请（>5000）", 8000);
        testCases.put("边界值（=5000）", 5000);

        for (Map.Entry<String, Object> testCase : testCases.entrySet()) {
            String caseName = testCase.getKey();
            int amount = (int) testCase.getValue();

            System.out.println("\n=== 测试用例: " + caseName + "，金额: " + amount + " ===");

            FlowContext context = FlowContext.of(instanceId + "-" + caseName.hashCode());
            context.put("actor", "applicant");
            context.put("amount", amount);

            // 创建条件流程图
            Graph conditionalGraph = Graph.create("conditional-" + caseName.hashCode(),
                    "条件测试-" + caseName, spec -> {
                        spec.addStart("start").linkAdd("apply");

                        spec.addActivity("apply").title("申请")
                                .metaPut("actor", "applicant")
                                .task(recordingTaskComponent)
                                .linkAdd("review");

                        spec.addExclusive("review").title("审批")
                                .metaPut("actor", "reviewer")
                                .task(recordingTaskComponent)
                                .linkAdd("auto-approve", link -> link.when(c->c.<Integer>getAs("amount") <= 5000).title("自动审批")) //"${amount} <= 5000"
                                .linkAdd("manager-approve", link -> link.when(c->c.<Integer>getAs("amount") > 5000).title("经理审批")); //"${amount} > 5000"

                        spec.addActivity("auto-approve").title("自动审批")
                                .task(recordingTaskComponent)
                                .linkAdd("end");

                        spec.addActivity("manager-approve").title("经理审批")
                                .metaPut("actor", "manager")
                                .task(recordingTaskComponent)
                                .linkAdd("end");

                        spec.addEnd("end").title("完成");
                    });

            flowEngine.load(conditionalGraph);

            // 运行流程
            WorkflowService caseWorkflow = WorkflowService.of(
                    flowEngine,
                    new ActorStateController("actor"),
                    new InMemoryStateRepository()
            );

            // 申请人提交
            Task applicantTask = caseWorkflow.getTask(conditionalGraph.getId(), context);
            assertNotNull(applicantTask);
            caseWorkflow.postTask(conditionalGraph.getId(), "apply", TaskAction.FORWARD, context);

            // 根据金额判断应该走哪个分支
            if (amount <= 5000) {
                // 应该走自动审批分支
                FlowContext reviewerContext = FlowContext.of(context.getInstanceId());
                reviewerContext.put("actor", "reviewer");
                reviewerContext.put("amount", amount);

                Task reviewTask = caseWorkflow.getTask(conditionalGraph.getId(), reviewerContext);
                assertNotNull(reviewTask);
                caseWorkflow.postTask(conditionalGraph.getId(), "review", TaskAction.FORWARD, reviewerContext);

                // 验证自动审批节点应该会被执行
                List<String> history = taskExecutionHistory.get(context.getInstanceId());
                assertTrue(history != null && history.stream().anyMatch(h -> h.startsWith("auto-approve")),
                        caseName + " 应执行自动审批");

                System.out.println(caseName + " 走自动审批分支");
            } else {
                // 应该走经理审批分支
                FlowContext managerContext = FlowContext.of(context.getInstanceId());
                managerContext.put("actor", "manager");
                managerContext.put("amount", amount);

                Task reviewTask = caseWorkflow.getTask(conditionalGraph.getId(), managerContext);
                assertNotNull(reviewTask);
                caseWorkflow.postTask(conditionalGraph.getId(), "review", TaskAction.FORWARD, managerContext);

                // 验证经理审批节点应该会被执行
                List<String> history = taskExecutionHistory.get(context.getInstanceId());
                assertTrue(history != null && history.stream().anyMatch(h -> h.startsWith("manager-approve")),
                        caseName + " 应执行经理审批");

                System.out.println(caseName + " 走经理审批分支");
            }
        }
    }

    @Test
    void testPerformanceAndStress() {
        // 性能测试：批量处理多个流程实例
        int batchSize = 100;
        long startTime = System.currentTimeMillis();

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < batchSize; i++) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    String instanceId = "perf-test-" + UUID.randomUUID().toString().substring(0, 8);
                    FlowContext context = FlowContext.of(instanceId);
                    context.put("actor", "applicant");

                    // 执行完整的审批流程
                    Task task = workflowService.getTask("approval-process", context);
                    if (task != null) {
                        task.run(context);
                        workflowService.postTask("approval-process", task.getNodeId(),
                                TaskAction.FORWARD, context);
                    }

                } catch (Exception e) {
                    // 在性能测试中，记录但不抛出异常
                    System.err.println("性能测试异常: " + e.getMessage());
                }
            });

            futures.add(future);
        }

        // 等待所有任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("\n=== 性能测试结果 ===");
        System.out.println("批量大小: " + batchSize);
        System.out.println("总耗时: " + duration + "ms");
        System.out.println("平均每个实例: " + (duration / (double) batchSize) + "ms");
        System.out.println("QPS: " + (batchSize / (duration / 1000.0)));

        // 验证性能指标（根据实际情况调整）
        assertTrue(duration < 10000, "批量处理应在10秒内完成");
        assertTrue(duration / (double) batchSize < 100, "每个实例平均处理时间应小于100ms");
    }
}
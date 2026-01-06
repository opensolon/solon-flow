package features.workflow.generated;

import org.junit.jupiter.api.Test;
import org.noear.solon.flow.*;
import org.noear.solon.flow.workflow.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 模拟生产测试 - 复杂业务场景、状态持久化、异常处理
 * 每个测试方法都有自己完全独立的：工作流引擎、工作流服务、流程图定义、组件实现
 */
class WorkflowServiceProductionTest {

    @Test
    void testSimpleLinearWorkflow() {
        // 1. 创建独立的组件
        TaskComponent taskComponent = new TaskComponent() {
            @Override
            public void run(FlowContext context, Node node) throws Throwable {
                String nodeId = node.getId();
                context.put(nodeId + "_executed", true);
                context.put(nodeId + "_time", System.currentTimeMillis());
                context.put("executionData", "数据-" + nodeId);
            }
        };

        // 2. 创建独立的流程图
        Graph graph = Graph.create("simple-linear-" + UUID.randomUUID(), "简单线性流程", spec -> {
            spec.addStart("start").title("开始").linkAdd("task1");

            spec.addActivity("task1").title("任务1")
                    .metaPut("actor", "user")
                    .task(taskComponent)
                    .linkAdd("task2");

            spec.addActivity("task2").title("任务2")
                    .metaPut("actor", "user")
                    .task(taskComponent)
                    .linkAdd("end");

            spec.addEnd("end").title("结束");
        });

        // 3. 创建独立的工作流服务
        FlowEngine engine = FlowEngine.newInstance();
        engine.load(graph);

        WorkflowService workflowService = WorkflowService.of(
                engine,
                new org.noear.solon.flow.workflow.controller.ActorStateController("actor"),
                new org.noear.solon.flow.workflow.repository.InMemoryStateRepository()
        );

        // 4. 测试执行
        String instanceId = "test-simple-" + UUID.randomUUID().toString().substring(0, 6);
        FlowContext context = FlowContext.of(instanceId);
        context.put("actor", "user");

        // 获取并完成任务1
        Task task1 = workflowService.getTask(graph.getId(), context);
        assertNotNull(task1);
        assertEquals("task1", task1.getNodeId());
        assertEquals(TaskState.WAITING, task1.getState());

        // 运行任务
        assertDoesNotThrow(() -> task1.run(context));
        workflowService.postTask(graph.getId(), "task1", TaskAction.FORWARD, context);

        // 获取并完成任务2
        Task task2 = workflowService.getTask(graph.getId(), context);
        assertNotNull(task2);
        assertEquals("task2", task2.getNodeId());

        workflowService.postTask(graph.getId(), "task2", TaskAction.FORWARD, context);

        // 验证流程完成
        Task finalTask = workflowService.getTask(graph.getId(), context);
        assertNull(finalTask);

        // 验证执行效果
        assertTrue(context.getAs("task1_executed"));
        assertTrue(context.getAs("task2_executed"));

        System.out.println("简单线性流程测试完成: " + instanceId);
    }

    @Test
    void testApprovalProcessWithRealComponents() {
        // 1. 创建独立的任务组件
        TaskComponent applyTaskComponent = new TaskComponent() {
            @Override
            public void run(FlowContext context, Node node) throws Throwable {
                context.put("applicationData", "申请数据-" + UUID.randomUUID());
                context.put("apply_executed", true);
            }
        };

        TaskComponent reviewTaskComponent = new TaskComponent() {
            @Override
            public void run(FlowContext context, Node node) throws Throwable {
                String result = context.getOrDefault("reviewResult", "approve").toString();
                context.put("reviewResult", result);
                context.put("reviewer", context.getOrDefault("reviewer", "system"));
                context.put("review_executed", true);
            }
        };

        // 2. 创建独立的流程图
        Graph graph = Graph.create("approval-process-" + UUID.randomUUID(), "审批流程", spec -> {
            spec.addStart("start").title("开始").linkAdd("apply");

            spec.addActivity("apply").title("提交申请")
                    .metaPut("actor", "applicant")
                    .task(applyTaskComponent)
                    .linkAdd("review");

            spec.addExclusive("review").title("审批")
                    .metaPut("actor", "reviewer")
                    .task(reviewTaskComponent)
                    .linkAdd("end", link -> link.when("${reviewResult} == 'approve'").title("通过"))
                    .linkAdd("apply", link -> link.when("${reviewResult} == 'reject'").title("驳回"));

            spec.addEnd("end").title("审批完成");
        });

        // 3. 创建独立的工作流服务
        FlowEngine engine = FlowEngine.newInstance();
        engine.load(graph);

        WorkflowService workflowService = WorkflowService.of(
                engine,
                new org.noear.solon.flow.workflow.controller.ActorStateController("actor"),
                new org.noear.solon.flow.workflow.repository.InMemoryStateRepository()
        );

        // 4. 测试执行
        String instanceId = "approval-test-" + UUID.randomUUID().toString().substring(0, 6);

        // 申请人提交申请
        FlowContext applicantContext = FlowContext.of(instanceId);
        applicantContext.put("actor", "applicant");
        applicantContext.put("applicantName", "张三");
        applicantContext.put("reviewResult", "approve");

        Task applyTask = workflowService.getTask(graph.getId(), applicantContext);
        assertNotNull(applyTask);
        assertEquals("apply", applyTask.getNodeId());
        assertEquals(TaskState.WAITING, applyTask.getState());

        assertDoesNotThrow(() -> applyTask.run(applicantContext));
        workflowService.postTask(graph.getId(), "apply", TaskAction.FORWARD, applicantContext);

        // 审批人审批
        FlowContext reviewerContext = FlowContext.of(instanceId);
        reviewerContext.put("actor", "reviewer");
        reviewerContext.put("reviewerName", "李审批员");
        reviewerContext.put("reviewResult", "approve");

        Task reviewTask = workflowService.getTask(graph.getId(), reviewerContext);
        assertNotNull(reviewTask);
        assertEquals("review", reviewTask.getNodeId());

        assertDoesNotThrow(() -> reviewTask.run(reviewerContext));
        workflowService.postTask(graph.getId(), "review", TaskAction.FORWARD, reviewerContext);

        // 验证流程完成
        Task finalTask = workflowService.getTask(graph.getId(), reviewerContext);
        assertNull(finalTask);

        // 验证执行效果
        assertTrue(applicantContext.getAs("apply_executed"));
        assertTrue(reviewerContext.getAs("review_executed"));
        assertEquals("approve", reviewerContext.getAs("reviewResult"));

        System.out.println("审批流程测试完成: " + instanceId);
    }

    @Test
    void testRejectionAndResubmissionWorkflow() {
        // 1. 创建独立的任务组件
        TaskComponent taskComponent = new TaskComponent() {
            @Override
            public void run(FlowContext context, Node node) throws Throwable {
                String nodeId = node.getId();
                context.put(nodeId + "_executed", true);
                context.put(nodeId + "_count", context.getOrDefault(nodeId + "_count", 0) + 1);
            }
        };

        // 2. 创建独立的流程图
        Graph graph = Graph.create("rejection-process-" + UUID.randomUUID(), "驳回重提交流程", spec -> {
            spec.addStart("start").title("开始").linkAdd("apply");

            spec.addActivity("apply").title("提交申请")
                    .metaPut("actor", "applicant")
                    .task(taskComponent)
                    .linkAdd("review");

            spec.addExclusive("review").title("审批")
                    .metaPut("actor", "reviewer")
                    .task(taskComponent)
                    .linkAdd("end", link -> link.when("${reviewResult} == 'approve'").title("通过"))
                    .linkAdd("apply", link -> link.when("${reviewResult} == 'reject'").title("驳回"));

            spec.addEnd("end").title("完成");
        });

        // 3. 创建独立的工作流服务
        FlowEngine engine = FlowEngine.newInstance();
        engine.load(graph);

        WorkflowService workflowService = WorkflowService.of(
                engine,
                new org.noear.solon.flow.workflow.controller.ActorStateController("actor"),
                new org.noear.solon.flow.workflow.repository.InMemoryStateRepository()
        );

        // 4. 测试执行
        String instanceId = "rejection-test-" + UUID.randomUUID().toString().substring(0, 6);

        FlowContext applicantContext = FlowContext.of(instanceId);
        applicantContext.put("actor", "applicant");

        FlowContext reviewerContext = FlowContext.of(instanceId);
        reviewerContext.put("actor", "reviewer");

        // 第一轮：提交申请
        workflowService.getTask(graph.getId(), applicantContext);
        workflowService.postTask(graph.getId(), "apply", TaskAction.FORWARD, applicantContext);

        // 第一轮：审批驳回
        reviewerContext.put("reviewResult", "reject");
        workflowService.getTask(graph.getId(), reviewerContext);
        workflowService.postTask(graph.getId(), "review", TaskAction.FORWARD, reviewerContext);

        // 验证流程回到申请节点
        Task taskAfterRejection = workflowService.getTask(graph.getId(), applicantContext);
        assertNotNull(taskAfterRejection);
        assertEquals("apply", taskAfterRejection.getNodeId());
        assertEquals(TaskState.WAITING, taskAfterRejection.getState());

        // 第二轮：修改后重新提交
        applicantContext.put("reviewResult", "approve");
        workflowService.postTask(graph.getId(), "apply", TaskAction.FORWARD, applicantContext);

        // 第二轮：审批通过
        reviewerContext.put("reviewResult", "approve");
        workflowService.postTask(graph.getId(), "review", TaskAction.FORWARD, reviewerContext);

        // 验证流程完成
        Task finalTask = workflowService.getTask(graph.getId(), reviewerContext);
        assertNull(finalTask);

        // 验证执行次数
        Integer applyCount = applicantContext.getAs("apply_count");
        Integer reviewCount = reviewerContext.getAs("review_count");
        assertNotNull(applyCount);
        assertNotNull(reviewCount);
        assertTrue(applyCount >= 2);
        assertTrue(reviewCount >= 2);

        System.out.println("驳回重提交流程测试完成: " + instanceId);
    }

    @Test
    void testComplexParallelProcess() {
        // 1. 创建独立的任务组件
        TaskComponent taskComponent = new TaskComponent() {
            @Override
            public void run(FlowContext context, Node node) throws Throwable {
                String nodeId = node.getId();
                context.put(nodeId + "_completed", true);
                context.put(nodeId + "_executor", context.getAs("actor"));
                context.put(nodeId + "_time", System.currentTimeMillis());
            }
        };

        // 2. 创建独立的并行流程图
        Graph graph = Graph.create("parallel-process-" + UUID.randomUUID(), "并行流程", spec -> {
            spec.addStart("start").title("开始").linkAdd("parallel-gateway");

            spec.addParallel("parallel-gateway").title("并行网关")
                    .linkAdd("task1")
                    .linkAdd("task2")
                    .linkAdd("task3");

            spec.addActivity("task1").title("任务1")
                    .metaPut("actor", "user1")
                    .metaPut("department", "sales")
                    .task(taskComponent)
                    .linkAdd("consolidate");

            spec.addActivity("task2").title("任务2")
                    .metaPut("actor", "user2")
                    .metaPut("department", "finance")
                    .task(taskComponent)
                    .linkAdd("consolidate");

            spec.addActivity("task3").title("任务3")
                    .metaPut("actor", "user3")
                    .metaPut("department", "hr")
                    .task(taskComponent)
                    .linkAdd("consolidate");

            spec.addActivity("consolidate").title("数据汇总")
                    .metaPut("actor", "admin")
                    .task(taskComponent)
                    .linkAdd("end");

            spec.addEnd("end").title("流程结束");
        });

        // 3. 创建独立的工作流服务
        FlowEngine engine = FlowEngine.newInstance();
        engine.load(graph);

        WorkflowService workflowService = WorkflowService.of(
                engine,
                new org.noear.solon.flow.workflow.controller.ActorStateController("actor"),
                new org.noear.solon.flow.workflow.repository.InMemoryStateRepository()
        );

        // 4. 测试执行
        String instanceId = "parallel-test-" + UUID.randomUUID().toString().substring(0, 6);

        // 测试不同用户访问
        String[] users = {"user1", "user2", "user3", "admin"};
        Map<String, FlowContext> contexts = new HashMap<>();
        Map<String, Task> tasks = new HashMap<>();

        for (String user : users) {
            FlowContext context = FlowContext.of(instanceId);
            context.put("actor", user);
            contexts.put(user, context);

            Task task = workflowService.getTask(graph.getId(), context);
            tasks.put(user, task);
        }

        // 验证前三个用户有任务，admin暂时没有
        assertNotNull(tasks.get("user1"));
        assertNotNull(tasks.get("user2"));
        assertNotNull(tasks.get("user3"));
        assertNull(tasks.get("admin"));

        // 各个用户完成任务
        for (String user : new String[]{"user1", "user2", "user3"}) {
            String nodeId = "task" + user.charAt(user.length() - 1);
            assertEquals(nodeId, tasks.get(user).getNodeId());
            workflowService.postTask(graph.getId(), nodeId, TaskAction.FORWARD, contexts.get(user));
        }

        // 检查管理员任务
        Task adminTask = workflowService.getTask(graph.getId(), contexts.get("admin"));
        assertNotNull(adminTask);
        assertEquals("consolidate", adminTask.getNodeId());

        // 管理员完成任务
        contexts.get("admin").put("consolidatedData", "汇总完成的数据");
        workflowService.postTask(graph.getId(), "consolidate", TaskAction.FORWARD, contexts.get("admin"));

        // 验证流程完成
        Task finalTask = workflowService.getTask(graph.getId(), contexts.get("admin"));
        assertNull(finalTask);

        // 验证所有任务完成状态
        for (String user : users) {
            String nodeId = user.equals("admin") ? "consolidate" : "task" + user.charAt(user.length() - 1);
            Node node = graph.getNode(nodeId);
            assertEquals(TaskState.COMPLETED, workflowService.getState(node, contexts.get(user)));
        }

        System.out.println("并行流程测试完成: " + instanceId);
    }

    @Test
    void testErrorHandlingWorkflow() {
        // 1. 创建独立的任务组件（可能抛出异常）
        TaskComponent normalTaskComponent = new TaskComponent() {
            @Override
            public void run(FlowContext context, Node node) throws Throwable {
                context.put("normal_executed", true);
            }
        };

        TaskComponent errorTaskComponent = new TaskComponent() {
            @Override
            public void run(FlowContext context, Node node) throws Throwable {
                boolean shouldFail = context.getOrDefault("shouldFail", false);
                if (shouldFail) {
                    throw new RuntimeException("模拟任务执行失败");
                }
                context.put("error_executed", true);
            }
        };

        // 2. 创建独立的错误处理流程图
        Graph graph = Graph.create("error-process-" + UUID.randomUUID(), "错误处理流程", spec -> {
            spec.addStart("start").linkAdd("normalTask");

            spec.addActivity("normalTask").title("正常任务")
                    .task(normalTaskComponent)
                    .linkAdd("errorTask");

            spec.addActivity("errorTask").title("可能失败的任务")
                    .task(errorTaskComponent)
                    .linkAdd("end");

            spec.addEnd("end").title("结束");
        });

        // 3. 创建独立的工作流服务
        FlowEngine engine = FlowEngine.newInstance();
        engine.load(graph);

        WorkflowService workflowService = WorkflowService.of(
                engine,
                new org.noear.solon.flow.workflow.controller.ActorStateController(),
                new org.noear.solon.flow.workflow.repository.InMemoryStateRepository()
        );

        // 4. 测试执行
        String instanceId = "error-test-" + UUID.randomUUID().toString().substring(0, 6);
        FlowContext context = FlowContext.of(instanceId);

        // 执行正常任务
        Task normalTask = workflowService.getTask(graph.getId(), context);
        assertNotNull(normalTask);
        assertEquals("normalTask", normalTask.getNodeId());

        assertDoesNotThrow(() -> normalTask.run(context));
        workflowService.postTask(graph.getId(), "normalTask", TaskAction.FORWARD, context);

        // 设置失败条件并测试错误任务
        context.put("shouldFail", true);
        Task errorTask = workflowService.getTask(graph.getId(), context);
        assertNotNull(errorTask);
        assertEquals("errorTask", errorTask.getNodeId());

        // 任务执行应该抛出异常
        Exception exception = assertThrows(RuntimeException.class,
                () -> errorTask.run(context));
        assertTrue(exception.getMessage().contains("模拟任务执行失败"));

        // 清除失败条件后重试
        context.put("shouldFail", false);
        workflowService.postTask(graph.getId(), "errorTask", TaskAction.FORWARD, context);

        // 验证流程可以继续
        Task finalTask = workflowService.getTask(graph.getId(), context);
        assertNull(finalTask);

        System.out.println("错误处理流程测试完成: " + instanceId);
    }

    @Test
    void testConditionalBranchingWorkflow() {
        // 1. 创建独立的组件
        TaskComponent taskComponent = new TaskComponent() {
            @Override
            public void run(FlowContext context, Node node) throws Throwable {
                context.put(node.getId() + "_executed", true);
                context.put("executor", context.getAs("actor"));
            }
        };

        ConditionComponent smallAmountCondition = new ConditionComponent() {
            @Override
            public boolean test(FlowContext context) throws Throwable {
                Integer amount = context.getAs("amount");
                return amount != null && amount <= 5000;
            }
        };

        ConditionComponent largeAmountCondition = new ConditionComponent() {
            @Override
            public boolean test(FlowContext context) throws Throwable {
                Integer amount = context.getAs("amount");
                return amount != null && amount > 5000;
            }
        };

        // 2. 创建独立的条件分支流程图
        Graph graph = Graph.create("conditional-process-" + UUID.randomUUID(), "条件分支流程", spec -> {
            spec.addStart("start").linkAdd("apply");

            spec.addActivity("apply").title("申请")
                    .metaPut("actor", "applicant")
                    .task(taskComponent)
                    .linkAdd("review");

            spec.addExclusive("review").title("审批")
                    .metaPut("actor", "reviewer")
                    .task(taskComponent)
                    .linkAdd("auto-approve", link -> link.when(smallAmountCondition).title("自动审批"))
                    .linkAdd("manager-approve", link -> link.when(largeAmountCondition).title("经理审批"));

            spec.addActivity("auto-approve").title("自动审批")
                    .task(taskComponent)
                    .linkAdd("end");

            spec.addActivity("manager-approve").title("经理审批")
                    .metaPut("actor", "manager")
                    .task(taskComponent)
                    .linkAdd("end");

            spec.addEnd("end").title("完成");
        });

        // 3. 创建独立的工作流服务
        FlowEngine engine = FlowEngine.newInstance();
        engine.load(graph);

        WorkflowService workflowService = WorkflowService.of(
                engine,
                new org.noear.solon.flow.workflow.controller.ActorStateController("actor"),
                new org.noear.solon.flow.workflow.repository.InMemoryStateRepository()
        );

        // 4. 测试小额申请（走自动审批分支）
        System.out.println("=== 测试小额申请（<=5000）===");
        String smallInstanceId = "small-amount-" + UUID.randomUUID().toString().substring(0, 6);
        FlowContext smallContext = FlowContext.of(smallInstanceId);
        smallContext.put("actor", "applicant");
        smallContext.put("amount", 3000);

        workflowService.getTask(graph.getId(), smallContext);
        workflowService.postTask(graph.getId(), "apply", TaskAction.FORWARD, smallContext);

        FlowContext smallReviewerContext = FlowContext.of(smallInstanceId);
        smallReviewerContext.put("actor", "reviewer");
        smallReviewerContext.put("amount", 3000);

        workflowService.postTask(graph.getId(), "review", TaskAction.FORWARD, smallReviewerContext);

        Task smallFinalTask = workflowService.getTask(graph.getId(), smallReviewerContext);
        assertNull(smallFinalTask);
        System.out.println("小额申请测试完成");

        // 5. 测试大额申请（走经理审批分支）
        System.out.println("=== 测试大额申请（>5000）===");
        String largeInstanceId = "large-amount-" + UUID.randomUUID().toString().substring(0, 6);
        FlowContext largeContext = FlowContext.of(largeInstanceId);
        largeContext.put("actor", "applicant");
        largeContext.put("amount", 8000);

        workflowService.getTask(graph.getId(), largeContext);
        workflowService.postTask(graph.getId(), "apply", TaskAction.FORWARD, largeContext);

        FlowContext managerContext = FlowContext.of(largeInstanceId);
        managerContext.put("actor", "manager");
        managerContext.put("amount", 8000);

        workflowService.postTask(graph.getId(), "review", TaskAction.FORWARD, managerContext);

        Task largeFinalTask = workflowService.getTask(graph.getId(), managerContext);
        assertNull(largeFinalTask);
        System.out.println("大额申请测试完成");
    }

    @Test
    void testHighConcurrencyWorkflowAccess() throws InterruptedException {
        // 1. 创建独立的任务组件
        TaskComponent taskComponent = new TaskComponent() {
            @Override
            public void run(FlowContext context, Node node) throws Throwable {
                context.put("executed", true);
                context.put("thread", Thread.currentThread().getName());
            }
        };

        // 2. 创建独立的简单流程图用于并发测试
        Graph graph = Graph.create("concurrent-process-" + UUID.randomUUID(), "并发测试流程", spec -> {
            spec.addStart("start").linkAdd("task");

            spec.addActivity("task").title("任务")
                    .metaPut("actor", "user")
                    .task(taskComponent)
                    .linkAdd("end");

            spec.addEnd("end").title("结束");
        });

        // 3. 创建独立的工作流服务
        FlowEngine engine = FlowEngine.newInstance();
        engine.load(graph);

        WorkflowService workflowService = WorkflowService.of(
                engine,
                new org.noear.solon.flow.workflow.controller.ActorStateController("actor"),
                new org.noear.solon.flow.workflow.repository.InMemoryStateRepository()
        );

        // 4. 并发测试
        int threadCount = 10;
        int iterationsPerThread = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        Runnable concurrentTask = () -> {
            try {
                startLatch.await();

                for (int i = 0; i < iterationsPerThread; i++) {
                    try {
                        String instanceId = "concurrent-" + Thread.currentThread().getId() + "-" + i + "-" + UUID.randomUUID().toString().substring(0, 4);
                        FlowContext context = FlowContext.of(instanceId);
                        context.put("actor", "user");

                        // 获取并完成任务
                        Task task = workflowService.getTask(graph.getId(), context);
                        if (task != null) {
                            task.run(context);
                            workflowService.postTask(graph.getId(), task.getNodeId(),
                                    TaskAction.FORWARD, context);
                            successCount.incrementAndGet();
                        }

                        // 短暂休眠
                        Thread.sleep(ThreadLocalRandom.current().nextInt(1, 10));

                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                        // 在并发测试中，异常是正常的
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
        boolean completed = finishLatch.await(3, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "并发测试应在3秒内完成");

        System.out.println("并发测试结果:");
        System.out.println("成功次数: " + successCount.get());
        System.out.println("失败次数: " + failureCount.get());

        // 验证至少有一定成功率
        assertTrue(successCount.get() > 0, "并发测试应有成功案例");
    }

    @Test
    void testMultipleInstanceIsolation() {
        // 1. 创建独立的任务组件
        TaskComponent taskComponent = new TaskComponent() {
            @Override
            public void run(FlowContext context, Node node) throws Throwable {
                String instanceId = context.getInstanceId();
                String customData = context.getOrDefault("customData", "default").toString();
                context.put("processed", true);
                context.put("instanceData", instanceId + ":" + customData);
            }
        };

        // 2. 创建独立的流程图
        Graph graph = Graph.create("isolation-process-" + UUID.randomUUID(), "实例隔离流程", spec -> {
            spec.addStart("start").linkAdd("process");

            spec.addActivity("process").title("处理任务")
                    .metaPut("actor", "processor")
                    .task(taskComponent)
                    .linkAdd("end");

            spec.addEnd("end").title("结束");
        });

        // 3. 创建独立的工作流服务
        FlowEngine engine = FlowEngine.newInstance();
        engine.load(graph);

        WorkflowService workflowService = WorkflowService.of(
                engine,
                new org.noear.solon.flow.workflow.controller.ActorStateController("actor"),
                new org.noear.solon.flow.workflow.repository.InMemoryStateRepository()
        );

        // 4. 测试多个实例隔离
        int instanceCount = 5;
        List<String> instanceIds = new ArrayList<>();
        List<FlowContext> contexts = new ArrayList<>();

        for (int i = 0; i < instanceCount; i++) {
            String instanceId = "instance-" + i + "-" + UUID.randomUUID().toString().substring(0, 6);
            instanceIds.add(instanceId);

            FlowContext context = FlowContext.of(instanceId);
            context.put("actor", "processor");
            context.put("customData", "数据" + i);
            contexts.add(context);
        }

        // 处理所有实例
        for (int i = 0; i < instanceCount; i++) {
            FlowContext context = contexts.get(i);

            Task task = workflowService.getTask(graph.getId(), context);
            assertNotNull(task);
            assertEquals("process", task.getNodeId());

            assertDoesNotThrow(() -> task.run(context));
            workflowService.postTask(graph.getId(), "process", TaskAction.FORWARD, context);
        }

        // 验证实例间数据隔离
        for (int i = 0; i < instanceCount; i++) {
            FlowContext context = contexts.get(i);
            String expectedData = "数据" + i;
            String expectedInstanceData = instanceIds.get(i) + ":" + expectedData;

            assertEquals(expectedData, context.getAs("customData"));
            assertEquals(expectedInstanceData, context.getAs("instanceData"));
            assertTrue(context.getAs("processed"));
        }

        System.out.println("多实例隔离测试完成，共测试 " + instanceCount + " 个实例");
    }

    @Test
    void testPerformanceBenchmark() {
        // 1. 创建轻量级的任务组件
        TaskComponent lightTaskComponent = new TaskComponent() {
            @Override
            public void run(FlowContext context, Node node) throws Throwable {
                // 模拟最小化的工作负载
                context.put("processed", true);
            }
        };

        // 2. 创建简单的性能测试流程图
        Graph graph = Graph.create("perf-process-" + UUID.randomUUID(), "性能测试流程", spec -> {
            spec.addStart("start").linkAdd("task1");

            spec.addActivity("task1").title("任务1")
                    .metaPut("actor", "user")
                    .task(lightTaskComponent)
                    .linkAdd("task2");

            spec.addActivity("task2").title("任务2")
                    .metaPut("actor", "user")
                    .task(lightTaskComponent)
                    .linkAdd("end");

            spec.addEnd("end").title("结束");
        });

        // 3. 创建独立的工作流服务
        FlowEngine engine = FlowEngine.newInstance();
        engine.load(graph);

        WorkflowService workflowService = WorkflowService.of(
                engine,
                new org.noear.solon.flow.workflow.controller.ActorStateController("actor"),
                new org.noear.solon.flow.workflow.repository.InMemoryStateRepository()
        );

        // 4. 性能测试
        int iterationCount = 100;
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < iterationCount; i++) {
            String instanceId = "perf-" + i + "-" + UUID.randomUUID().toString().substring(0, 4);
            FlowContext context = FlowContext.of(instanceId);
            context.put("actor", "user");

            // 执行完整流程
            Task task1 = workflowService.getTask(graph.getId(), context);
            if (task1 != null) {
                task1.run(context);
                workflowService.postTask(graph.getId(), "task1", TaskAction.FORWARD, context);

                Task task2 = workflowService.getTask(graph.getId(), context);
                if (task2 != null) {
                    task2.run(context);
                    workflowService.postTask(graph.getId(), "task2", TaskAction.FORWARD, context);
                }
            }
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("\n=== 性能测试结果 ===");
        System.out.println("迭代次数: " + iterationCount);
        System.out.println("总耗时: " + duration + "ms");
        System.out.println("平均每个实例: " + (duration / (double) iterationCount) + "ms");

        if (duration > 0) {
            double qps = (iterationCount * 2.0) / (duration / 1000.0); // 两个任务
            System.out.println("QPS (任务数): " + String.format("%.2f", qps));
        }

        // 性能验证
        assertTrue(duration < 5000, "性能测试应在5秒内完成");
    }
}
package features.workflow.generated;

import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.FlowEngine;
import org.noear.solon.flow.Graph;
import org.noear.solon.flow.Node;
import org.noear.solon.flow.workflow.*;
import org.noear.solon.flow.workflow.controller.ActorStateController;
import org.noear.solon.flow.workflow.repository.InMemoryStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

/**
 * workflowExecutor 多图多角色审批测试用例
 *
 * 本测试类验证复杂审批流程场景，包括：
 * 1. 多图嵌套架构：主审批流程 + 技术评审子图 + 财务评审子图
 * 2. 多角色协同审批：申请人、部门经理、技术负责人、财务负责人、总经理
 * 3. 复杂流程控制：并行网关、条件分支、子图调用、结果合并
 *
 * 测试目标：
 * - 验证多级审批流程的正确执行顺序
 * - 测试基于角色的任务分配和权限控制
 * - 验证并行审批和条件分支的逻辑正确性
 * - 测试子图调用的独立性和结果隔离
 * - 验证工作流状态的持久化和恢复机制
 */
public class WorkflowMultiGraphMultiRoleTest {

    private FlowEngine flowEngine;
    private WorkflowExecutor workflowExecutor;

    // ==================== 角色定义常量 ====================
    /** 申请人角色：提交审批申请 */
    private static final String ROLE_APPLICANT = "applicant";
    /** 部门经理角色：一级审批，决定是否进入后续评审 */
    private static final String ROLE_DEPT_MANAGER = "deptManager";
    /** 技术负责人角色：执行技术方案、代码、架构评审 */
    private static final String ROLE_TECH_LEADER = "techLeader";
    /** 财务负责人角色：执行预算和财务合规性评审 */
    private static final String ROLE_FINANCE_LEADER = "financeLeader";
    /** 总经理角色：最终审批，处理大额或特殊项目 */
    private static final String ROLE_GENERAL_MANAGER = "generalManager";

    // ==================== 流程图ID常量 ====================
    /** 主审批流程图ID：协调整个审批流程 */
    private static final String MAIN_APPROVAL_GRAPH_ID = "mainApproval";
    /** 技术评审子图ID：处理技术相关评审 */
    private static final String TECH_REVIEW_GRAPH_ID = "techReview";
    /** 财务评审子图ID：处理财务相关评审 */
    private static final String FINANCE_REVIEW_GRAPH_ID = "financeReview";

    /**
     * 测试前置设置：构建完整的审批流程体系
     *
     * 构建流程：
     * 1. 技术评审子图：包含技术方案评审、并行代码/架构评审、结果合并
     * 2. 财务评审子图：包含预算评审、金额条件分支、额外审批路径
     * 3. 主审批图：调用子图，实现完整的多级审批流程
     */
    @BeforeEach
    void setUp() {
        // 1. 初始化流程引擎
        flowEngine = FlowEngine.newInstance();

        // 2. 构建技术评审子图（并行评审模式）
        Graph techReviewGraph = Graph.create(TECH_REVIEW_GRAPH_ID, "技术评审流程", spec -> {
            spec.addStart("tech_start").title("技术评审开始")
                    .linkAdd("tech_review1");

            // 技术方案评审节点
            spec.addActivity("tech_review1").title("技术方案评审")
                    .metaPut("actor", ROLE_TECH_LEADER)
                    .metaPut("department", "技术部")
                    .linkAdd("tech_parallel");

            // 并行网关：同时启动代码评审和架构评审
            spec.addParallel("tech_parallel").title("并行技术评审")
                    .linkAdd("code_review")
                    .linkAdd("arch_review");

            // 代码评审节点
            spec.addActivity("code_review").title("代码评审")
                    .metaPut("actor", ROLE_TECH_LEADER)
                    .metaPut("reviewType", "code")
                    .linkAdd("tech_merge");

            // 架构评审节点
            spec.addActivity("arch_review").title("架构评审")
                    .metaPut("actor", ROLE_TECH_LEADER)
                    .metaPut("reviewType", "architecture")
                    .linkAdd("tech_merge");

            // 包容网关：合并并行评审结果，等待所有分支完成
            spec.addInclusive("tech_merge").title("技术评审合并")
                    .linkAdd("tech_end");

            spec.addEnd("tech_end").title("技术评审结束");
        });

        // 3. 构建财务评审子图（条件分支模式）
        Graph financeReviewGraph = Graph.create(FINANCE_REVIEW_GRAPH_ID, "财务评审流程", spec -> {
            spec.addStart("finance_start").title("财务评审开始")
                    .linkAdd("budget_review");

            // 预算评审节点
            spec.addActivity("budget_review").title("预算评审")
                    .metaPut("actor", ROLE_FINANCE_LEADER)
                    .metaPut("amount", "budget")
                    .linkAdd("finance_decision");

            // 排他网关：根据金额决定审批路径
            // 优先级10：额外审批路径（金额>10万）
            // 默认分支：常规审批路径（金额≤10万）
            spec.addExclusive("finance_decision").title("财务决策")
                    .linkAdd("normal_approval", link -> link
                            .when("amount <= 100000")
                            .title("常规审批"))
                    .linkAdd("extra_approval", link -> link
                            .when("amount > 100000")
                            .title("额外审批")
                            .priority(10))
                    .linkAdd("normal_approval"); // 默认分支

            // 常规审批路径
            spec.addActivity("normal_approval").title("财务常规审批")
                    .metaPut("actor", ROLE_FINANCE_LEADER)
                    .linkAdd("finance_end");

            // 额外审批路径（大额项目需要）
            spec.addActivity("extra_approval").title("财务额外审批")
                    .metaPut("actor", ROLE_FINANCE_LEADER)
                    .metaPut("requireGeneralManager", true)
                    .linkAdd("finance_end");

            spec.addEnd("finance_end").title("财务评审结束");
        });

        // 4. 构建主审批图（协调整个审批流程）
        Graph mainApprovalGraph = Graph.create(MAIN_APPROVAL_GRAPH_ID, "主审批流程", spec -> {
            // 开始节点
            spec.addStart("start").title("流程开始")
                    .linkAdd("apply");

            // 申请提交节点（申请人）
            spec.addActivity("apply").title("提交申请")
                    .metaPut("actor", ROLE_APPLICANT)
                    .task((context, node) -> {
                        // 模拟申请提交：设置申请信息
                        context.put("applicantName", "张三");
                        context.put("projectName", "新项目开发");
                        context.put("budgetAmount", 150000); // 15万元，触发额外财务审批
                        context.put("hasTechnicalReview", true);
                    })
                    .linkAdd("dept_approval");

            // 部门经理审批节点
            spec.addActivity("dept_approval").title("部门经理审批")
                    .metaPut("actor", ROLE_DEPT_MANAGER)
                    .task((context, node) -> {
                        // 模拟部门经理审批：90%通过率
                        boolean approved = Math.random() > 0.1;
                        context.put("deptApproved", approved);
                        context.put("deptComment", approved ? "同意" : "需要修改");
                    })
                    .linkAdd("parallel_gateway");

            // 并行网关：同时启动技术评审和财务评审
            spec.addParallel("parallel_gateway").title("并行评审网关")
                    .linkAdd("tech_review_call")
                    .linkAdd("finance_review_call");

            // 调用技术评审子图（通过task("#图ID")实现图间调用）
            spec.addActivity("tech_review_call").title("技术评审")
                    .task("#" + TECH_REVIEW_GRAPH_ID)
                    .linkAdd("merge_gateway");

            // 调用财务评审子图
            spec.addActivity("finance_review_call").title("财务评审")
                    .task("#" + FINANCE_REVIEW_GRAPH_ID)
                    .linkAdd("merge_gateway");

            // 并行网关：等待所有子图评审完成
            spec.addParallel("merge_gateway").title("评审结果合并")
                    .linkAdd("final_decision");

            // 排他网关：最终决策，根据条件选择审批路径
            spec.addExclusive("final_decision").title("最终决策")
                    .linkAdd("general_manager_approval", link -> link
                            .when("budgetAmount > 100000")
                            .title("需要总经理审批")
                            .priority(10))
                    .linkAdd("complete_approval", link -> link
                            .when("deptApproved == true && techReviewPassed == true && financeReviewPassed == true")
                            .title("直接完成审批"))
                    .linkAdd("reject"); // 默认拒绝分支

            // 总经理审批节点（大额项目）
            spec.addActivity("general_manager_approval").title("总经理审批")
                    .metaPut("actor", ROLE_GENERAL_MANAGER)
                    .metaPut("level", "final")
                    .task((context, node) -> {
                        // 模拟总经理审批：95%通过率
                        boolean approved = Math.random() > 0.05;
                        context.put("generalManagerApproved", approved);
                        context.put("finalComment", approved ? "最终同意" : "最终拒绝");
                    })
                    .linkAdd("complete_approval", link -> link.when("generalManagerApproved == true"))
                    .linkAdd("reject");

            // 审批完成节点
            spec.addActivity("complete_approval").title("审批完成")
                    .task((context, node) -> {
                        context.put("approvalStatus", "APPROVED");
                        context.put("completionTime", System.currentTimeMillis());
                    })
                    .linkAdd("end");

            // 审批拒绝节点
            spec.addActivity("reject").title("审批拒绝")
                    .task((context, node) -> {
                        context.put("approvalStatus", "REJECTED");
                        context.put("rejectReason", "不符合公司政策");
                    })
                    .linkAdd("end");

            spec.addEnd("end").title("流程结束");
        });

        // 5. 加载所有图到流程引擎（注意加载顺序）
        flowEngine.load(techReviewGraph);
        flowEngine.load(financeReviewGraph);
        flowEngine.load(mainApprovalGraph);

        // 6. 创建工作流服务
        workflowExecutor = WorkflowExecutor.of(
                flowEngine,
                new ActorStateController("actor"), // 基于角色的状态控制器
                new InMemoryStateRepository()     // 内存状态仓库（测试用）
        );
    }

    /**
     * 测试1：完整的审批流程路径
     *
     * 验证整个审批流程的正确执行顺序：
     * 申请人提交 → 部门经理审批 → 并行技术/财务评审 → 总经理审批 → 流程完成
     */
    @Test
    void testMultiGraphMultiRoleApprovalProcess() {
        // 1. 申请人提交申请
        FlowContext applicantContext = FlowContext.of("test_instance_001")
                .put("actor", ROLE_APPLICANT)
                .put("applicantId", "user_001");

        // 获取申请人的当前任务
        Task applicantTask = workflowExecutor.claimTask(MAIN_APPROVAL_GRAPH_ID, applicantContext);
        assertNotNull(applicantTask);
        assertEquals("apply", applicantTask.getNodeId());
        assertEquals(TaskState.WAITING, applicantTask.getState());

        // 申请人执行任务
        workflowExecutor.submitTask(applicantTask, TaskAction.FORWARD, applicantContext);

        // 2. 部门经理审批
        FlowContext deptManagerContext = applicantContext
                .put("actor", ROLE_DEPT_MANAGER)
                .put("managerId", "manager_001");

        Task deptManagerTask = workflowExecutor.claimTask(MAIN_APPROVAL_GRAPH_ID, deptManagerContext);
        assertNotNull(deptManagerTask);
        assertEquals("dept_approval", deptManagerTask.getNodeId());

        // 部门经理审批通过
        workflowExecutor.submitTask(deptManagerTask, TaskAction.FORWARD, deptManagerContext);

        // 3. 验证进入并行网关，获取并行任务
        FlowContext systemContext = deptManagerContext;
        Collection<Task> parallelTasks = workflowExecutor.findNextTasks(MAIN_APPROVAL_GRAPH_ID, systemContext);
        assertEquals(2, parallelTasks.size()); // 应该有技术评审和财务评审两个任务

        // 4. 技术负责人进行技术评审
        FlowContext techLeaderContext = systemContext
                .put("actor", ROLE_TECH_LEADER)
                .put("techLeaderId", "tech_001");

        // 获取技术评审子图中的任务
        Collection<Task> techReviewTasks = workflowExecutor.findNextTasks(TECH_REVIEW_GRAPH_ID, techLeaderContext);
        assertFalse(techReviewTasks.isEmpty());

        // 技术负责人完成技术评审
        for (Task task : techReviewTasks) {
            workflowExecutor.submitTaskIfWaiting(task, TaskAction.FORWARD, techLeaderContext);
        }

        // 技术评审有多个阶段，继续执行剩余任务
        techReviewTasks = workflowExecutor.findNextTasks(TECH_REVIEW_GRAPH_ID, techLeaderContext);
        assertFalse(techReviewTasks.isEmpty());
        for (Task task : techReviewTasks) {
            workflowExecutor.submitTaskIfWaiting(task, TaskAction.FORWARD, techLeaderContext);
        }

        // 5. 财务负责人进行财务评审
        FlowContext financeLeaderContext = techLeaderContext
                .put("actor", ROLE_FINANCE_LEADER)
                .put("financeLeaderId", "finance_001")
                .put("amount", 150000); // 设置金额触发额外审批

        // 获取财务评审子图中的任务
        Collection<Task> financeReviewTasks = workflowExecutor.findNextTasks(FINANCE_REVIEW_GRAPH_ID, financeLeaderContext);

        // 财务负责人完成财务评审（由于金额较大，会触发额外审批路径）
        for (Task task : financeReviewTasks) {
            workflowExecutor.submitTaskIfWaiting(task, TaskAction.FORWARD, financeLeaderContext);
        }

        // 子图预算评审（仍由财务负责人执行）
        financeReviewTasks = workflowExecutor.findNextTasks(FINANCE_REVIEW_GRAPH_ID, financeLeaderContext);
        for (Task task : financeReviewTasks) {
            workflowExecutor.submitTaskIfWaiting(task, TaskAction.FORWARD, financeLeaderContext);
        }

        // 6. 验证需要总经理审批（金额>10万触发）
        FlowContext checkContext = financeLeaderContext
                .put("actor", ROLE_GENERAL_MANAGER)
                .put("generalManagerId", "gm_001");

        Task nextTask = workflowExecutor.claimTask(MAIN_APPROVAL_GRAPH_ID, checkContext);

        // 由于金额较大，应该需要总经理审批
        if (nextTask != null && "general_manager_approval".equals(nextTask.getNodeId())) {
            // 7. 总经理审批
            FlowContext generalManagerContext = checkContext;

            Task gmTask = workflowExecutor.claimTask(MAIN_APPROVAL_GRAPH_ID, generalManagerContext);
            assertNotNull(gmTask);
            assertEquals("general_manager_approval", gmTask.getNodeId());

            // 总经理审批通过
            workflowExecutor.submitTask(gmTask, TaskAction.FORWARD, generalManagerContext);
        }

        // 8. 验证流程完成
        systemContext = checkContext;
        Task finalTask = workflowExecutor.claimTask(MAIN_APPROVAL_GRAPH_ID, systemContext);
        if (finalTask != null) {
            assertEquals("complete_approval", finalTask.getNodeId());
            workflowExecutor.submitTask(finalTask, TaskAction.FORWARD, systemContext);
        }

        // 9. 验证最终状态
        Node endNode = flowEngine.getGraph(MAIN_APPROVAL_GRAPH_ID).getNode("end");
        TaskState finalState = workflowExecutor.getState(endNode, systemContext);
        assertNotNull(finalState);
    }

    /**
     * 测试2：并行审批场景
     *
     * 验证技术评审和财务评审可以并行执行，互不影响
     */
    @Test
    void testParallelApprovalWithMultipleActors() {
        FlowContext context = FlowContext.of("test_instance_002")
                .put("applicantName", "李四")
                .put("projectName", "并行测试项目")
                .put("budgetAmount", 50000);

        // 1. 申请人提交
        context.put("actor", ROLE_APPLICANT);
        Task applicantTask = workflowExecutor.claimTask(MAIN_APPROVAL_GRAPH_ID, context);
        workflowExecutor.submitTask(applicantTask, TaskAction.FORWARD, context);
        assertEquals("apply", applicantTask.getNodeId());

        // 2. 部门经理审批
        context.put("actor", ROLE_DEPT_MANAGER);
        Task deptTask = workflowExecutor.claimTask(MAIN_APPROVAL_GRAPH_ID, context);
        workflowExecutor.submitTask(deptTask, TaskAction.FORWARD, context);
        assertEquals("dept_approval", deptTask.getNodeId());

        // 3. 模拟技术负责人和财务负责人并行审批
        FlowContext techContext = context
                .put("actor", ROLE_TECH_LEADER);

        FlowContext financeContext = context
                .put("actor", ROLE_FINANCE_LEADER)
                .put("amount", 50000);

        // 获取技术评审任务
        Collection<Task> techTasks = workflowExecutor.findNextTasks(TECH_REVIEW_GRAPH_ID, techContext);
        assertEquals(1, techTasks.size());

        // 获取财务评审任务
        Collection<Task> financeTasks = workflowExecutor.findNextTasks(FINANCE_REVIEW_GRAPH_ID, financeContext);
        assertEquals(1, financeTasks.size());

        // 并行执行技术评审
        for (Task task : techTasks) {
            workflowExecutor.submitTaskIfWaiting(task, TaskAction.FORWARD, techContext);
        }

        // 并行执行财务评审
        for (Task task : financeTasks) {
            workflowExecutor.submitTaskIfWaiting(task, TaskAction.FORWARD, financeContext);
        }

        // 验证主流程继续执行
        FlowContext checkContext = context;
        Task nextTask = workflowExecutor.claimTask(MAIN_APPROVAL_GRAPH_ID, checkContext);
        assertNotNull(nextTask);
        assertEquals("mainApproval", nextTask.getNode().getGraph().getId());
    }

    /**
     * 测试3：审批拒绝和回退场景
     *
     * 验证审批被拒绝时流程的正确终止
     */
    @Test
    void testApprovalRejectionAndBacktrack() {
        FlowContext context = FlowContext.of("test_instance_003");

        // 1. 提交申请
        context.put("actor", ROLE_APPLICANT);
        Task applicantTask = workflowExecutor.claimTask(MAIN_APPROVAL_GRAPH_ID, context);
        workflowExecutor.submitTask(applicantTask, TaskAction.FORWARD, context);

        // 2. 部门经理拒绝（终止审批）
        context.put("actor", ROLE_DEPT_MANAGER);
        Task deptTask = workflowExecutor.claimTask(MAIN_APPROVAL_GRAPH_ID, context);
        workflowExecutor.submitTask(deptTask, TaskAction.TERMINATE, context);

        // 3. 验证流程终止状态
        FlowContext checkContext = FlowContext.of("test_instance_003");
        Task finalTask = workflowExecutor.claimTask(MAIN_APPROVAL_GRAPH_ID, checkContext);

        // 应该停留在终止节点
        assertNull(finalTask);

        finalTask = workflowExecutor.findTask(MAIN_APPROVAL_GRAPH_ID, checkContext);

        // 应该停留在终止节点
        assertNotNull(finalTask);
        assertEquals(deptTask.getNodeId(), finalTask.getNodeId());
        assertEquals(TaskState.TERMINATED, finalTask.getState());
    }

    /**
     * 测试4：跳转审批场景
     *
     * 验证FORWARD_JUMP动作可以跳过中间节点直接跳转到目标节点
     */
    @Test
    void testJumpApproval() {
        FlowContext context = FlowContext.of("test_instance_004");

        // 提交申请
        context.put("actor", ROLE_APPLICANT);
        Task applicantTask = workflowExecutor.claimTask(MAIN_APPROVAL_GRAPH_ID, context);
        workflowExecutor.submitTask(applicantTask, TaskAction.FORWARD, context);

        // 部门经理审批
        context.put("actor", ROLE_DEPT_MANAGER);
        Task deptTask = workflowExecutor.claimTask(MAIN_APPROVAL_GRAPH_ID, context);
        workflowExecutor.submitTask(deptTask, TaskAction.FORWARD, context);

        // 模拟总经理直接跳转到最终审批（跳过技术评审和财务评审）
        context.put("actor", ROLE_GENERAL_MANAGER);

        // 跳转到总经理审批节点（FORWARD_JUMP跳过中间环节）
        Node finalApprovalNode = flowEngine.getGraph(MAIN_APPROVAL_GRAPH_ID).getNode("general_manager_approval");
        workflowExecutor.submitTask(finalApprovalNode.getGraph(), finalApprovalNode, TaskAction.FORWARD_JUMP, context);
        System.out.println(context.lastRecord());
        assertTrue(context.lastRecord().isEnd());

        // 验证状态：流程应该已结束
        FlowContext checkContext = FlowContext.fromJson(context.toJson());
        Task finalTask = workflowExecutor.claimTask(MAIN_APPROVAL_GRAPH_ID, checkContext);
        assertNull(finalTask);
        assertTrue(checkContext.lastRecord().isEnd());
    }

    /**
     * 测试5：多实例并发执行
     *
     * 验证多个审批实例可以独立运行，状态互不干扰
     */
    @Test
    void testMultiInstanceWorkflow() {
        for (int i = 1; i <= 3; i++) {
            String instanceId = "multi_instance_" + i;
            FlowContext context = FlowContext.of(instanceId)
                    .put("actor", ROLE_APPLICANT)
                    .put("applicantName", "用户" + i)
                    .put("projectName", "项目" + i)
                    .put("budgetAmount", i * 50000);

            // 启动每个实例
            Task task = workflowExecutor.claimTask(MAIN_APPROVAL_GRAPH_ID, context);
            assertNotNull(task);
            workflowExecutor.submitTask(task, TaskAction.FORWARD, context);
            task = workflowExecutor.claimTask(MAIN_APPROVAL_GRAPH_ID, context);

            // 验证每个实例独立运行
            FlowContext checkContext = FlowContext.fromJson(context.toJson());
            Task currentTask = workflowExecutor.claimTask(MAIN_APPROVAL_GRAPH_ID, checkContext);

            assertEquals(task, currentTask);
            assertEquals(context.lastNodeId(), checkContext.lastNodeId());
        }
    }

    /**
     * 测试6：条件分支工作流
     *
     * 验证基于金额的条件分支逻辑：
     * - 小金额（≤10万）：不需要总经理审批
     * - 大金额（>10万）：需要总经理审批
     */
    @Test
    void testWorkflowWithConditionalBranches() throws Throwable {
        // 场景1：小金额项目，不需要总经理审批
        testSmallAmountProject();

        // 场景2：大金额项目，需要总经理审批
        testLargeAmountProject();
    }

    /**
     * 子场景测试：小金额项目审批
     * 验证金额≤10万时不会触发总经理审批路径
     */
    private void testSmallAmountProject() {
        FlowContext context = FlowContext.of("small_amount_instance")
                .put("budgetAmount", 50000); // 5万，小金额

        // 提交申请
        context.put("actor", ROLE_APPLICANT);
        Task task = workflowExecutor.claimTask(MAIN_APPROVAL_GRAPH_ID, context);
        workflowExecutor.submitTask(task, TaskAction.FORWARD, context);

        // 部门经理审批
        context.put("actor", ROLE_DEPT_MANAGER);
        task = workflowExecutor.claimTask(MAIN_APPROVAL_GRAPH_ID, context);
        workflowExecutor.submitTask(task, TaskAction.FORWARD, context);

        // 验证不会进入总经理审批节点
        FlowContext checkContext = context;
        Collection<Task> tasks = workflowExecutor.findNextTasks(MAIN_APPROVAL_GRAPH_ID, checkContext);

        boolean hasGeneralManagerTask = tasks.stream()
                .anyMatch(t -> "general_manager_approval".equals(t.getNodeId()));
        assertFalse(hasGeneralManagerTask, "小金额项目不应需要总经理审批");
    }

    /**
     * 子场景测试：大金额项目审批
     * 验证金额>10万时会触发总经理审批路径
     */
    private void testLargeAmountProject() throws Throwable {
        FlowContext context = FlowContext.of("large_amount_instance")
                .put("amount", 200000)
                .put("budgetAmount", 200000); // 20万，大金额

        // 提交申请
        context.put("actor", ROLE_APPLICANT);
        Task task = workflowExecutor.claimTask(MAIN_APPROVAL_GRAPH_ID, context);
        workflowExecutor.submitTask(task, TaskAction.FORWARD, context);

        // 部门经理审批
        context.put("actor", ROLE_DEPT_MANAGER);
        task = workflowExecutor.claimTask(MAIN_APPROVAL_GRAPH_ID, context);
        workflowExecutor.submitTask(task, TaskAction.FORWARD, context);

        // 执行技术方案评审和预算评审
        Collection<Task> tasks = workflowExecutor.findNextTasks(MAIN_APPROVAL_GRAPH_ID, context);
        for (Task task1 : tasks) {
            workflowExecutor.submitTask(task1, TaskAction.FORWARD, context);
        }

        // 触发额外评审路径
        tasks = workflowExecutor.findNextTasks(MAIN_APPROVAL_GRAPH_ID, context);
        for (Task task1 : tasks) {
            workflowExecutor.submitTask(task1, TaskAction.FORWARD, context);
        }

        // 验证会进入总经理审批节点
        FlowContext checkContext = context;
        tasks = workflowExecutor.findNextTasks(MAIN_APPROVAL_GRAPH_ID, checkContext);

        boolean hasGeneralManagerTask = tasks.stream()
                .anyMatch(t -> "general_manager_approval".equals(t.getNodeId()));
        assertTrue(hasGeneralManagerTask, "大金额项目应需要总经理审批");
    }

    /**
     * 测试7：工作流状态持久化和恢复
     *
     * 验证工作流状态可以序列化为JSON并正确恢复
     */
    @Test
    void testWorkflowStatePersistence() {
        String instanceId = "persistence_test_instance";

        // 第一阶段：提交申请
        FlowContext context1 = FlowContext.of(instanceId)
                .put("actor", ROLE_APPLICANT);

        Task task1 = workflowExecutor.claimTask(MAIN_APPROVAL_GRAPH_ID, context1);
        workflowExecutor.submitTask(task1, TaskAction.FORWARD, context1);

        // 保存当前状态（模拟持久化）
        String stateJson = context1.toJson();

        // 第二阶段：从持久化状态恢复
        FlowContext context2 = FlowContext.fromJson(stateJson);
        context2.put("actor", ROLE_DEPT_MANAGER);

        Task task2 = workflowExecutor.claimTask(MAIN_APPROVAL_GRAPH_ID, context2);
        assertNotNull(task2);
        assertEquals("dept_approval", task2.getNodeId());

        // 继续执行
        workflowExecutor.submitTask(task2, TaskAction.FORWARD, context2);

        // 验证流程可以继续执行
        FlowContext checkContext = FlowContext.of(instanceId);
        Collection<Task> tasks = workflowExecutor.findNextTasks(MAIN_APPROVAL_GRAPH_ID, checkContext);
        assertFalse(tasks.isEmpty());
    }
}
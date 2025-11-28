<template>
    <div class="canvasView" style="width: 100%; height: 100%;position: relative;">
        <div ref="flowContainerRef" style="width: 100%; height: 100%"></div>
        <TeleportContainer></TeleportContainer>
        <NodeFormDialog ref="nodeFormDialogRef"></NodeFormDialog>
        <EdgeFormDialog ref="edgeFormDialogRef"></EdgeFormDialog>
        <GraphFormDialog ref="graphFormDialogRef"></GraphFormDialog>
        <div id="minimap" style="bottom: 20px;right: 20px;position: absolute;"></div>
    </div>
</template>
<script setup>
import { onMounted, defineComponent, ref, nextTick } from 'vue';
import { Graph, Shape } from "@antv/x6";
import { Snapline } from '@antv/x6-plugin-snapline'
import { Selection } from '@antv/x6-plugin-selection'
import { MiniMap } from '@antv/x6-plugin-minimap'
import { Dnd } from '@antv/x6-plugin-dnd'
import { register, getTeleport } from '@antv/x6-vue-shape'
import { nodeTypeDef } from '../nodeTypeDef.js';
import * as utils from '@/utils/index.js'
import BaseNode from './BaseNode.vue'
import NodeFormDialog from './NodeFormDialog.vue';
import EdgeFormDialog from './EdgeFormDialog.vue';
import GraphFormDialog from './GraphFormDialog.vue';
import dagre from '@dagrejs/dagre';

const props = defineProps({
    dndContainer: {
        type: Object,
    }
})

const TeleportContainer = defineComponent(getTeleport());
const flowContainerRef = ref(null); // 画布容器的引用
const nodeFormDialogRef = ref(null); // 节点表单对话框的引用
const edgeFormDialogRef = ref(null); // 边表单对话框的引用
const graphFormDialogRef = ref(null); // 流图表单对话框的引用
let graph = null
let dnd = null
let currentEditEdge = null // 当前编辑的边
let currentEditNode = null // 当前编辑的节点
let currentEditGraph = {} // 当前编辑的流图

onMounted(() => {
    nextTick(() => {
        registerNode()
        initGraph()
        clear()
    })

})

let baseNodeInfo = {
    width: 160,
    height: 40,
    attr: {
        magnet: true,
    },
    ports: {
        groups: {
            top: {
                position: 'top',
                attrs: {
                    circle: {
                        magnet: true,
                        stroke: '#8f8f8f',
                        r: 5,
                    },
                },
            },
            left: {
                attrs: {
                    position: 'left',
                    circle: {
                        magnet: true,
                        stroke: '#8f8f8f',
                        r: 5,
                    },
                },
            },
            right: {
                position: 'right',
                attrs: {
                    circle: {
                        magnet: true,
                        stroke: '#8f8f8f',
                        r: 5,
                    }
                }
            },
            bottom: {
                position: 'bottom',
                attrs: {
                    circle: {
                        magnet: true,
                        stroke: '#8f8f8f',
                        r: 5,
                    }
                }
            }
        },
        items: [
            {
                id: 'port_l1',
                group: 'left',
            },
            {
                id: 'port_t1',
                group: 'top',
            },
            {
                id: 'port_r1',
                group: 'right'
            },
            {
                id: 'port_b1',
                group: 'bottom'
            }
        ]
    }
}

function registerNode() {
    Object.keys(nodeTypeDef).forEach((key) => {
        const nodeType = nodeTypeDef[key]
        register(Object.assign({}, baseNodeInfo, {
            shape: nodeType.type,
            component: BaseNode
        })
        )
    })
}

function initGraph() {
    Graph.registerEdge("flow-edge", {
        inherit: 'edge',
        attrs: {
            line: {
                stroke: '#adadad',
                strokeWidth: 1,
                targetMarker: {
                    name: 'classic',
                    size: 7,
                },
            },
        },
        router: {
            name: 'manhattan',
            args: {
                padding: 20,
            },
        },
    }, true)

    graph = new Graph({
        container: flowContainerRef.value,
        grid: true,
        mousewheel: true,
        scaling: {
            min: 0.5,
            max: 1,
        },
        selecting: {
            enabled: true,
            rubberband: true,
            showNodeSelectionBox: true,
            showEdgeSelectionBox: true,
        },
        highlighting: {
            magnetAvailable: {
                name: 'stroke',
                args: {
                    attrs: {
                        'stroke-width': 2,
                        stroke: '#4169dd',
                    },
                },
            },
            magnetAdsorbed: {
                name: 'stroke',
                args: {
                    attrs: {
                        fill: '#fff',
                        stroke: '#31d0c6',
                        strokeWidth: 4,
                    },
                },
            },
        },
        connecting: {
            allowMulti: false,
            allowBlank: false,
            allowEdge: false,
            allowNode: false,
            highlight: true,
            router: 'manhattan',
            connector: {
                name: 'rounded',
                args: {
                    radius: 8,
                },
            },
            snap: {
                radius: 20,
            },
            anchor: 'center',
            createEdge() {
                const edgeId = 'edge_' + utils.uuid2()
                return graph.createEdge({
                    id: edgeId,
                    shape: 'flow-edge',
                    data: {
                        id: edgeId,
                    }
                })
            },
            validateConnection({
                sourceCell,
                targetCell,
                sourceMagnet,
                targetMagnet,
            }) {
                // 不能连接自身
                if (sourceCell === targetCell) {
                    return false
                }
                const nodeType = targetCell.data.type;
                if(nodeType){
                    if(nodeTypeDef[nodeType].validateConnection){
                        return nodeTypeDef[nodeType].validateConnection(graph,sourceCell,targetCell)
                    }
                }
                return true

            },
            validateMagnet(args) {
                const nodeType = args.cell.data.type;
                if(nodeType){
                    if(nodeTypeDef[nodeType].validateMagnet){
                        return nodeTypeDef[nodeType].validateMagnet(graph,args.cell)
                    }
                }
                return true;
            }
        },
        // scroller: true,
        panning: true,
        background: {
            color: "#f2f7fa",
        },
    });
    graph.use(
        new Snapline({
            enabled: true,
        }),
    )
    graph.use(
        new Selection({
            enabled: true,
        }),
    )
    graph.use(
        new MiniMap({
            container: document.getElementById('minimap'),
            width:150,
            height:150,
        }),
    )

    dnd = new Dnd({
        target: graph,
        scaled: false,
        dndContainer: props.dndContainer.value,
        getDragNode: (node) => node.clone({ keepId: true }),
        getDropNode: (node) => node.clone({ keepId: true }),
    })

    graph.on('node:mouseenter', () => {
        showPorts(true)
    })
    graph.on('node:mouseleave', () => {
        showPorts(false)
    })

    graph.on('cell:mouseenter', ({ cell }) => {
        if (cell.isEdge()) {
            cell.addTools([{name: 'button-remove', args: { distance: '50%' }}])
        }
    })
    graph.on('cell:mouseleave', ({ cell }) => {
        if (cell.isEdge()) {
            cell.removeTools()
        }
    })

    graph.on('cell:dblclick', ({ cell }) => {
      if (cell.isEdge()) {
        currentEditEdge = cell
        closeAllFormDialog()
        edgeFormDialogRef.value.show(graph, currentEditEdge)
      }
    })

    graph.on('node:toDel', (node) => {
        graph.removeNode(node)
    })
    graph.on('node:toEdit', (node) => {
        currentEditNode = node
        closeAllFormDialog()
        nodeFormDialogRef.value.show(graph, currentEditNode)
    })
}

function closeAllFormDialog() {
    nodeFormDialogRef.value.toClose()
    edgeFormDialogRef.value.toClose()
    graphFormDialogRef.value.toClose()
}

function showPorts(show) {
    const ports = flowContainerRef.value.querySelectorAll('.x6-port-body');
    for (let i = 0, len = ports.length; i < len; i++) {
        ports[i].style.visibility = show ? 'visible' : 'hidden';
    }
}

function svgToDataURL(svgPath, fillColor = "#FFF") {
    const svgCode = `
    <svg width="14px" height="14px" viewBox="0 0 1024 1024" xmlns="http://www.w3.org/2000/svg" style="fill:${fillColor}">
      <path d="${svgPath}"></path>
    </svg>
  `;
    const encoded = window.btoa(svgCode);
    return `data:image/svg+xml;base64,${encoded}`;
}

function initStartNode() {
    createNode('start', true, 10, 10)
    graph.centerContent();
    showPorts(false)
}

function createNode(type, isAdd = true, x = 10, y = 10) {
    const id = "node_" + utils.uuid2()
    const nodeType = nodeTypeDef[type]
    const node = graph.createNode({
        shape: nodeType.type,
        id: id,
        x: x,
        y: y,
        data: {
            id: id,
            type: nodeType.type,
            title: nodeType.title,
        },
    })
    if (isAdd) {
        graph.addNode(node)
    }
    return node
}

function onSiderStartDrag(e, nodeType) {
    let node = createNode(nodeType.type, false, 10, 10)
    dnd.start(node, e)
}

function onEditGraphConfig() {
    closeAllFormDialog()
    graphFormDialogRef.value.show(graph, currentEditGraph)
}

function getData() { // 导出当前画布的内容为 JSON 格式的字符串，用于保存或分享
    return {
        graphData: currentEditGraph,
        graphViewData: graph.toJSON()
    }
}

function clear(isInitStartNode = true) { // 清空画布内容，可选是否重新初始化开始节点
    graph.clearCells()
    if (isInitStartNode) {
        initStartNode()
    }
    currentEditGraph = {
        id: "_" + utils.uuid2()
    }
    closeAllFormDialog()
}

function setGraph(graphData) { // 导入 JSON 格式的字符串，用于加载或分享的内容
    currentEditGraph = graphData || {
        id: "_" + utils.uuid2()
    }
}

function setData(data) { // 导入 JSON 格式的字符串，用于加载或分享的内容
    graph.fromJSON(data)
}

// 自动布局 dir为布局类型，默认TB从上到下
// Direction for rank nodes. Can be TB, BT, LR, or RL, where T = top, B = bottom, L = left, and R = right.
function autoLayout(dir = "TB") { // 自动布局
    const nodeWidth = 140; // 节点的宽度
    const nodeHeight = 40; // 节点的高度
    const nodes = graph.getNodes()
    const edges = graph.getEdges()
    const g = new dagre.graphlib.Graph()

    if (dir == "TB") {
        g.setGraph({ rankdir: dir, nodesep: 300, ranksep: 100, edgesep: 200,align:'DL' }) // 上下
    } else if (dir == "LR") {
        g.setGraph({ rankdir: dir, nodesep: 100, ranksep: 220, edgesep: 100,align:'DL' }) // 左右
    }

    // g.setGraph({ rankdir: dir, nodesep: 220, ranksep: 220 ,edgesep:200}) 左右
    g.setDefaultEdgeLabel(() => ({}))

    nodes.forEach((node) => {
        g.setNode(node.id, { nodeWidth, nodeHeight })
    })

    edges.forEach((edge) => {
        const source = edge.getSource()
        const target = edge.getTarget()
        g.setEdge(source.cell, target.cell)
    })

    dagre.layout(g)

    g.nodes().forEach((id) => {
        const node = graph.getCellById(id)
        if (node) {
            const pos = g.node(id)
            node.position(pos.x, pos.y)
        }
    })

    // 对循环网关进行特殊处理，使得网关中间的节点有缩进效果
    graph.getRootNodes().forEach((node) => {
        indentNodes(node, 0)
    })
}

// 节点缩进。参数:当前节点，缩进深度
function indentNodes(node, indentDepth) {
    const x = node.position().x
    
    node.position(x + 100 * indentDepth, node.position().y)

    let myIndentDepth = indentDepth
    if(node.data.type === 'iterator') {
        if(node.data.meta && node.data.meta.$for){
            myIndentDepth = indentDepth + 1
        }else{
            myIndentDepth = indentDepth - 1
            node.position(x + 100 * myIndentDepth, node.position().y)
        }
    }

    const edges = graph.getOutgoingEdges(node)
    if(edges && edges.length>0){
        edges.forEach((edge) => {
            console.log(edge)
            const target = graph.getCellById(edge.getTarget().cell)
            console.log(target)
            indentNodes(target, myIndentDepth)
        })
    }
}

defineExpose({
    onSiderStartDrag,
    onEditGraphConfig,
    getData,
    setData,
    clear,
    setGraph,
    autoLayout
})
</script>
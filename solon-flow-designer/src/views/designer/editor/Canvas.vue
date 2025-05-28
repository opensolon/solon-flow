<template>
    <div class="canvasView" style="width: 100%; height: 100%;position: relative;">
        <div ref="flowContainerRef" style="width: 100%; height: 100%"></div>
        <TeleportContainer></TeleportContainer>
        <NodeFormDialog ref="nodeFormDialogRef"></NodeFormDialog>
        <EdgeFormDialog ref="edgeFormDialogRef"></EdgeFormDialog>
        <ChainFormDialog ref="chainFormDialogRef"></ChainFormDialog>
    </div>
</template>
<script setup>
import { onMounted, defineComponent, ref, nextTick } from 'vue';
import { Graph, Shape } from "@antv/x6";
import { Snapline } from '@antv/x6-plugin-snapline'
import { Selection } from '@antv/x6-plugin-selection'
import { Dnd } from '@antv/x6-plugin-dnd'
import { register, getTeleport } from '@antv/x6-vue-shape'
import {nodeTypeDef} from '../nodeTypeDef.js';
import * as utils from '@/utils/index.js'
import BaseNode from './BaseNode.vue'
import NodeFormDialog from './NodeFormDialog.vue';
import EdgeFormDialog from './EdgeFormDialog.vue';
import ChainFormDialog from './ChainFormDialog.vue';
import dagre from '@dagrejs/dagre';

const props = defineProps({
    dndContainer:{
        type: Object,
    }
})

const TeleportContainer = defineComponent(getTeleport());
const flowContainerRef = ref(null); // 画布容器的引用
const nodeFormDialogRef = ref(null); // 节点表单对话框的引用
const edgeFormDialogRef = ref(null); // 边表单对话框的引用
const chainFormDialogRef = ref(null); // chain表单对话框的引用
let graph = null
let dnd = null
let currentEditEdge = null // 当前编辑的边
let currentEditNode = null // 当前编辑的节点
let currentEditChain = {} // 当前编辑的chain

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
        }
    }, true)

    graph = new Graph({
        container: flowContainerRef.value,
        grid: true,
        mousewheel: true,
        scaling:{
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
                    data:{
                        id:edgeId,
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
                return true

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

    dnd = new Dnd({
      target: graph,
      scaled: false,
      dndContainer: props.dndContainer.value,
      getDragNode: (node) => node.clone({ keepId: true }),
      getDropNode: (node) => node.clone({ keepId: true }),
    })

    graph.on('node:mouseenter', () => {
        showPorts( true)
    })
    graph.on('node:mouseleave', () => {
        showPorts( false)
    })

    graph.on('cell:mouseenter', ({ cell }) => {
        if (cell.isEdge()) {
            cell.addTools(['button-remove', {
                name: 'button',
                args: {
                    markup: [
                        {
                            tagName: 'circle',
                            selector: 'button',
                            attrs: {
                                r: 7,
                                fill: '#0078d4',
                                cursor: 'pointer',
                            },
                        },

                        {
                            tagName: 'image',
                            attrs: {
                                'xlink:href': svgToDataURL('M195.245646 780.395106c19.73827-56.509738 39.257605-113.092455 59.239136-169.506105 10.795963-30.534233 16.302196-41.097881 40.208759-63.175114l235.814432-233.548448L707.910359 489.874722l-7.773434 7.993586c-72.276758 74.408948-144.480537 148.854386-216.734185 223.264551-21.323121 24.186314-32.36113 31.227529-62.506144 42.303244-0.218936 0.098521-0.414762 0.19461-0.633697 0.267588-0.583829 0.232315-1.17009 0.46463-1.753919 0.672619-2.047049 0.778438-4.14275 1.558092-6.214125 2.338963-7.431651 2.763455-14.865734 5.530559-22.297385 8.284284-22.662278 8.371858-45.30023 16.729121-67.939398 25.111925-26.707723 9.881298-53.439773 19.73827-80.074518 29.777689-13.452383 5.057415-29.266839 3.65501-40.13578-6.189799-12.621643-11.477096-12.816253-28.523673-6.602128-43.304266zM733.061206 465.896398L555.486104 290.016831l77.249032-76.530192c14.40232-14.279473 33.82435-22.23657 54.197533-22.23657 20.396294 0 39.792781 7.957097 54.218212 22.23657l69.13503 68.500117c14.42543 14.303799 22.466452 33.49473 22.466452 53.683035 0 20.213847-8.039806 39.416941-22.466452 53.696414l-77.224705 76.530193z'),
                                x: -7,
                                y: -7,
                                cursor: 'pointer',
                            }
                        },
                    ],
                    distance: 20,
                    onClick: (e) => {
                        // e.edge.remove({ ui: true })
                        currentEditEdge = e.cell
                        closeAllFormDialog()
                        edgeFormDialogRef.value.show(graph, currentEditEdge)
                    }
                }
            }])
        }
    })
    graph.on('cell:mouseleave', ({ cell }) => {
        if (cell.isEdge()) {
            cell.removeTools()
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
    chainFormDialogRef.value.toClose()
}

function showPorts( show) {
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
    createNode('start',true,10,10)
    graph.centerContent();
    showPorts( false)
}

function createNode(type,isAdd=true,x=10,y=10) {
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
    if(isAdd) {
        graph.addNode(node)
    }
    return node
}

function onSiderStartDrag(e, nodeType) {
    let node = createNode(nodeType.type,false, 10, 10)
    dnd.start(node,e)
}

function onEditChainConfig() {
    closeAllFormDialog()
    chainFormDialogRef.value.show(graph,currentEditChain)
}

function getData() { // 导出当前画布的内容为 JSON 格式的字符串，用于保存或分享
    return {
        chain: currentEditChain,
        graphData: graph.toJSON()
    }
}

function clear(isInitStartNode = true) { // 清空画布内容，可选是否重新初始化开始节点
    graph.clearCells()
    if(isInitStartNode){
        initStartNode()
    }
    currentEditChain = {
        id : "chain_"+utils.uuid2()
    }
    closeAllFormDialog()
}

function setChain(chainData) { // 导入 JSON 格式的字符串，用于加载或分享的内容
    currentEditChain = chainData || {
        id : "chain_"+utils.uuid2()
    }
}

function setData(data) { // 导入 JSON 格式的字符串，用于加载或分享的内容
    graph.fromJSON(data)
}

// 自动布局 dir为布局类型，默认TB从上到下
// Direction for rank nodes. Can be TB, BT, LR, or RL, where T = top, B = bottom, L = left, and R = right.
function autoLayout(dir="TB") { // 自动布局
    const nodeWidth = 140; // 节点的宽度
    const nodeHeight = 40; // 节点的高度
    const nodes = graph.getNodes()
    const edges = graph.getEdges()
    const g = new dagre.graphlib.Graph()
    if(dir=="TB"){
        g.setGraph({ rankdir: dir, nodesep: 220, ranksep: 100 ,edgesep:200}) // 上下
    }else if(dir == "LR"){
        g.setGraph({ rankdir: dir, nodesep: 100, ranksep: 220,edgesep:200}) // 左右
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
}

defineExpose({
    onSiderStartDrag,
    onEditChainConfig,
    getData,
    setData,
    clear,
    setChain,
    autoLayout
})
</script>
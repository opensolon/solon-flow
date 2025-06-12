<template>
    <div class="node" @dblclick="editNode">
        <div class="node-icon" :style="{'background-color':nodeInfo.color}">
            <font-awesome-icon :icon="nodeInfo.icon" />
        </div>
        <div class="node-title">{{ nodeInfo.title }}</div>
        <div class="node-tools">
            <a-space >
                <div class="wf-node-tool wf-node-tool-deleteNode" @click="deleteNode">
                    <font-awesome-icon icon="fa-solid fa-xmark" />
                </div>
            </a-space>
        </div>
    </div>
</template>
<script setup>
import { onMounted, reactive,watch } from 'vue';
import {nodeTypeDef,groupMap} from '../nodeTypeDef.js';
const props = defineProps({
    node: {
        type: Object
    },
    graph: {
        type: Object
    }
});
var nodeInfo = reactive({
    id: null,
    "type": null,
    "title": null,
    "color": "#0820e2",
    "icon": "fa-solid fa-play",
})

onMounted(() => {
    const nodeData = props.node.getData()
    const nodeType = nodeTypeDef[nodeData.type];
    if(!nodeData.title && nodeData.id) {
      if (!nodeData.id.startsWith('node_')) {
        nodeData.title = nodeData.id;
      }
    }

    if(!nodeData.title) {
      // 如果没有title，就使用 task 或者 type.title
      if (nodeData.task) {
        if (nodeData.task.startsWith('#') || nodeData.task.startsWith('@') || nodeData.task.startsWith('$')) {
          nodeData.title = nodeData.task;
        } else {
          nodeData.title = '脚本代码';
        }
      } else {
        nodeData.title = nodeType.title
      }

      props.node.setData({
        title: nodeData.title
      })
    }

    nodeInfo = Object.assign(nodeInfo,{}, nodeData,{
        "color": nodeType.color,
        "icon": nodeType.icon,
        title: nodeData.title
    })
    props.node.on("node:data:changed", () => {
        const nodeData = props.node.getData()
        nodeInfo.title = nodeData.title
    })
})

function deleteNode() {
    // 确认是否删除
    if (!confirm("确认删除该节点吗？")) { return }
    props.graph.emit("node:toDel",props.node)
}
function editNode() {
    props.graph.emit("node:toEdit",props.node)
}
</script>
<style lang="less" >
.node-tools{
    position: absolute;
    right: 10px;
    cursor: pointer;
    display: none;
}
.node:hover .node-tools{
    display: block;
}
.wf-node-tool {
    display: flex;
    width: 18px;
    height: 18px;
    color: white;
    border-radius: 50%;
    background-color: #1890ff;

    font-size: 12px;
    align-items: center;
    justify-content: center;
    -moz-box-shadow: 1px 1px 7px 1px rgba(181, 183, 199, 0.73);
    box-shadow: 1px 1px 7px 1px rgba(181, 183, 199, 0.73);
    
}

.wf-node-tool-addNode {
    right: 40px;
    background-color: #1890ff;
}

.wf-node-tool-deleteNode {
    right: 10px;
    background-color: #fa0d35;
}

.wf-node-tool-editNode {
    right: 70px;
    background-color: #05940d;
}

</style>
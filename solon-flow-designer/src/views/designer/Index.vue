<template>
  <div class="editor">
    <div class="editor-header">
      <Header @editChainConfig="onEditChainConfig" @toExport="toExport"></Header>
    </div>
    <div class="editor-content">
      <div class="editor-sider">
        <Sider ref="siderRef" @startDrag="onStartDrag"></Sider>
      </div>
      <div class="editor-canvas">
        <FlowCanvas ref="flowCanvasRef" :dndContainer="siderRef"></FlowCanvas>
        <a-modal v-model:visible="state.isExportDialogOpen" title="导出" :footer="null" @cancel="state.isExportDialogOpen = false">
          <a-textarea :rows="10" v-model:value="state.exportData" />
        </a-modal>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref,reactive } from 'vue';
import Header from './editor/Header.vue';
import Sider from './editor/Sider.vue';
import FlowCanvas from './editor/Canvas.vue';

const flowCanvasRef = ref(null); // 画布容器的引用
const siderRef = ref(null); // 侧边栏容器的引用

const state = reactive({
  isExportDialogOpen: false, // 导出对话框的状态
  exportData: '', // 导出的数据
})

const onStartDrag = ({e,nodeType}) => {
  flowCanvasRef.value.onSiderStartDrag(e,nodeType); // 调用画布容器的方法
}
const onEditChainConfig = () => {
  flowCanvasRef.value.onEditChainConfig();
}

const toExport = () => {
  const data = flowCanvasRef.value.getData();
  console.log('data',data)
  const nodeLinkMap = {}; // 用于存储节点和边的关联关系
  const nodes = [];
  data.graphData.cells.forEach(cell => {
    if(cell.shape == 'flow-edge'){
      const edgeData = cell.data || {}
      const edge ={
        source:cell.source.cell,
        sourcePort:cell.source.port,
        target:cell.target.cell,
        targetPort:cell.target.port,
        id:cell.id,
        title:edgeData.title,
      }
      nodeLinkMap[cell.source.cell] = nodeLinkMap[cell.source.cell] || []; // 初始化节点的边数组
      nodeLinkMap[cell.source.cell].push(edge); // 将边添加到节点的边数组中
    }else{
      const nodeData = cell.data || {};
      nodes.push({
        id:cell.id,
        type:nodeData.type,
        title:nodeData.title,
        task:nodeData.task,
        when:nodeData.when,
        meta:nodeData.meta,
        x: cell.position.x, // 节点的 x 坐标
        y: cell.position.y, // 节点的 y 坐标
      })
    }
  })

  nodes.forEach(node => {
    node.link = nodeLinkMap[node.id] || []; // 将边数组添加到节点对象中
  });

  const chainData = data.chain; // 构建最终的 JSON 数据
  chainData.layout = nodes

  console.log('chainData',chainData)
  state.exportData = JSON.stringify(chainData); // 格式化输出 JSON 数据
  state.isExportDialogOpen = true; // 打开导出对话框
}
</script>

<style lang="less">
.editor {
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: #f0f0f0;

  .editor-header {
    height: 50px;
    background-color: #fff;
    border-bottom: 1px solid #ccc;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 0 20px
  }

  .editor-content {
    flex: 1;
    display: flex;
    overflow: hidden;
  }

 .editor-sider {
    width: 200px;
    background-color: #fff;
    border-right: 1px solid #ccc;
  }
  .editor-canvas {
    flex: 1;
    background-color: #fff;
  }
}

.node {
        width:160px;
        height: 40px;
        padding: 10px;
        border: 1px solid #ccc;
        cursor: move;
        border-radius: 5px;
        display: flex;
        align-items: center;
        background-color: #fff;

        &:hover {
                background-color: #deeff5;
                border-color: #0099ff;
            }

        .node-icon {
            margin-right: 10px;
            font-size: 14px;
            color: #FFF;
            border-radius: 5px;
            width: 20px;
            height: 20px;
            display: flex;
            align-items: center;
            justify-content: center;

            
        }

       .node-title {
            font-size: 14px;
        }
    }
</style>
<template>
  <div class="editor">
    <div class="editor-header">
      <Header @editChainConfig="onEditChainConfig" @toExport="toExport" @toImport="toImport" @toClear="toClear"></Header>
    </div>
    <div class="editor-content">
      <div class="editor-sider">
        <Sider ref="siderRef" @startDrag="onStartDrag"></Sider>
      </div>
      <div class="editor-canvas">
        <FlowCanvas ref="flowCanvasRef" :dndContainer="siderRef"></FlowCanvas>
        <a-modal v-model:open="state.isExportDialogOpen" title="导出" :footer="null" @cancel="state.isExportDialogOpen = false">
          <a-textarea :rows="10" v-model:value="state.exportData" />
        </a-modal>
        <a-modal v-model:open="state.isImportDialogOpen" title="导入" @ok="handleImport" @cancel="state.isImportDialogOpen = false">
          <a-textarea :rows="10" v-model:value="state.importData" />
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
import * as utils from '@/utils/index.js'
import yamlUtils from 'js-yaml'

const flowCanvasRef = ref(null); // 画布容器的引用
const siderRef = ref(null); // 侧边栏容器的引用

const state = reactive({
  isExportDialogOpen: false, // 导出对话框的状态
  exportData: '', // 导出的数据
  importType: 'json'
})

const onStartDrag = ({e,nodeType}) => {
  flowCanvasRef.value.onSiderStartDrag(e,nodeType); // 调用画布容器的方法
}
const onEditChainConfig = () => {
  flowCanvasRef.value.onEditChainConfig();
}

const toExport = () => {
  // 组只数据，将antv-x6格式转换为solon-flow格式
  const data = flowCanvasRef.value.getData();
  const nodeLinkMap = {}; // 用于存储节点和边的关联关系
  const nodes = [];
  data.graphData.cells.forEach(cell => {
    if(cell.shape == 'flow-edge'){
      const edgeData = cell.data || {}
      const edge ={
        v_source:cell.source.cell,
        v_sourcePort:cell.source.port,
        v_target:cell.target.cell,
        v_targetPort:cell.target.port,

        nextId: cell.target.cell, // 边的目标节点
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
        v_x: cell.position.x, // 节点的 x 坐标
        v_y: cell.position.y, // 节点的 y 坐标
      })
    }
  })

  nodes.forEach(node => {
    node.link = nodeLinkMap[node.id] || []; // 将边数组添加到节点对象中
  });

  const chainData = data.chain; // 构建最终的 JSON 数据
  chainData.layout = nodes

  console.log('chainData',chainData)
  state.exportData = JSON.stringify(chainData,null,4); // 格式化输出 JSON 数据
  state.isExportDialogOpen = true; // 打开导出对话框
}

function toImport(type) {
  state.importType = type
  state.isImportDialogOpen = true; // 打开导入对话框
}

function handleImport() {
  let data = null
  if(state.importType == 'json'){
    data = JSON.parse(state.importData); // 解析导入的数据
  }else{
    data = yamlUtils.load(state.importData); // 解析导入的数据
  }
  
  flowCanvasRef.value.clear(false); // 清空画布容器中的内容
  flowCanvasRef.value.setChain(data); // 
  // 组织数据，将solon-flow格式转换为antv-x6格式
  if(data.layout){
    const graphData = {
      cells: [], // 存储节点和边的数组
    };
    // 简化模式，只有节点信息，没有边信息
    let isSimpleMode = true; // 标记为非简单模式

    let temp_x = 10;
    let temp_y = 10;
    data.layout.forEach(node => {
      node.type = node.type || 'activity'
      node.id = node.id || 'node_'+utils.uuid2()
      const nodeData = {
        id: node.id, // 节点的唯一标识符
        shape: node.type, // 节点的形状
        data: { // 节点的自定义数据
          id: node.id, // 节点的唯一标识符
          type: node.type, // 节点的类型
          title: node.title, // 节点的标题
          task: node.task, // 节点的任务
          when: node.when, // 节点的条件
          meta: node.meta, // 节点的元数据
        },
        position: { // 节点的位置
          x: node.v_x, // 节点的 x 坐标
          y: node.v_y, // 节点的 y 坐标
        },
      }

      if(!node.v_x || !node.v_y){
        nodeData.position = {
          x: temp_x, // 节点的 x 坐标
          y: temp_y, // 节点的 y 坐标
        }
        temp_x += 100;
        temp_y += 100;
      }

      graphData.cells.push(nodeData); // 将节点数据添加到数组中

      if(node.link){
        isSimpleMode = false;
        if(Array.isArray(node.link)){
          node.link.forEach(link => {
            if(typeof link == 'object'){
              if(!link.id){
                link.id = 'edge_'+utils.uuid2()
              }
              if(!link.v_source){
                link.v_source = node.id,
                link.v_sourcePort = 'port_b1'
              }
              if(!link.v_target){
                link.v_target = link.nextId,
                link.v_targetPort = 'port_t1'
              }
              const edgeData = {
                id: link.id, // 边的唯一标识符
                shape: 'flow-edge', // 边的形状
                source: { cell: link.v_source, port: link.v_sourcePort }, // 边的源节点和端口
                target: { cell: link.v_target, port: link.v_targetPort }, // 边的目标节点和端口
                data: { // 边的自定义数据
                  id: link.id, // 边的唯一标识符
                  nextId: link.target, // 边的目标节点
                  title: link.title, // 边的标题
                },
              }
              if(link.title){
                edgeData.labels=[link.title]
              }
              graphData.cells.push(edgeData); // 将边数据添加到数组中
            }else{// string的情况
              const edgeData = buildEdgeForStringType(node.id,link) // 构建边数据的函数
              graphData.cells.push(edgeData); // 将边数据添加到数组中
            }
            

          })
        }else{
          // string的情况
          const edgeData = buildEdgeForStringType(node.id,node.link) // 构建边数据的函数
          graphData.cells.push(edgeData); // 将边数据添加到数组中
        }
      }
    })

    if(isSimpleMode){
      let edges = []
      for(let i = 0;i<graphData.cells.length-1;i++){
        const cell = graphData.cells[i]
        const nextCell = graphData.cells[i+1]
        
        const edgeData = buildEdgeForStringType(cell.id,nextCell.id) // 构建边数据的函数
        edges.push(edgeData); // 将边数据添加到数组中
      }
      graphData.cells = graphData.cells.concat(edges)
    }


    console.log('graphData',graphData)
    flowCanvasRef.value.setData(graphData); // 将节点和边数据设置到画布容器中
  }
  
  state.isImportDialogOpen = false; // 关闭导入对话框
}

function toClear() {
  flowCanvasRef.value.clear(); // 清空画布容器中的内容
}

function buildEdgeForStringType(source,target){
  const edgeId = 'edge_'+utils.uuid2()
  const edgeData = {
    id: edgeId, // 边的唯一标识符
    shape: 'flow-edge', // 边的形状
    source: { cell: source, port: 'port_b1' }, // 边的源节点和端口
    target: { cell: target, port: 'port_t1' }, // 边的目标节点和端口
    data: { // 边的自定义数据
      id: edgeId, // 边的唯一标识符
      nextId: target, // 边的目标节点
      title: null, // 边的标题
    },
  }
  return edgeData
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
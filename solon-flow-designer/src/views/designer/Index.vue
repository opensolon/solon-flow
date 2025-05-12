<template>
  <div class="editor">
    <div class="editor-header">
      <Header ></Header>
    </div>
    <div class="editor-content">
      <div class="editor-sider">
        <Sider ref="siderRef" @startDrag="onStartDrag"></Sider>
      </div>
      <div class="editor-canvas">
        <FlowCanvas ref="flowCanvasRef" :dndContainer="siderRef"></FlowCanvas>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import Header from './editor/Header.vue';
import Sider from './editor/Sider.vue';
import FlowCanvas from './editor/Canvas.vue';

const flowCanvasRef = ref(null); // 画布容器的引用
const siderRef = ref(null); // 侧边栏容器的引用

const onStartDrag = ({e,nodeType}) => {
  flowCanvasRef.value.onSiderStartDrag(e,nodeType); // 调用画布容器的方法
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
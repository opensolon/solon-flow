<template>
  <div class="editor">
    <div class="editor-header">
      <Header ></Header>
    </div>
    <div class="editor-content">
      <div class="editor-sider">
        <Sider></Sider>
      </div>
      <div class="editor-canvas">3</div>
    </div>
  </div>
</template>

<script setup>
import Header from './editor/Header.vue';
import Sider from './editor/Sider.vue';
</script>

<style scoped lang="less">
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
}
</style>
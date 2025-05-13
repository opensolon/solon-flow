<template>
    <a-space>
        <a-button type="primary">放大</a-button>
        <a-button type="primary">缩小</a-button>
        <a-button type="primary">恢复</a-button>
        <a-divider type="vertical" style="background-color: #afafaf"/>
        <a-button type="primary">前进</a-button>
        <a-button type="primary">后退</a-button>

        <a-divider type="vertical" style="background-color: #afafaf"/>
        <a-button type="primary" >导入</a-button>
        <a-button type="primary" @Click="toExport">导出</a-button>

        <a-button type="primary">清空</a-button>
        <a-divider type="vertical" style="background-color: #afafaf"/>
        <a-button type="primary" @click="editChainConfig">Chain 配置</a-button>
    </a-space>
</template>
<script setup>
import { ref } from 'vue'

const emit = defineEmits(['editChainConfig'])

function editChainConfig() { 
    emit('editChainConfig')
} // 打开编辑 Chain 配置的对话框

function toExport() { // 导出当前画布的内容为 JSON 格式的字符串，用于保存或分享
    emit('toExport')
}
</script>
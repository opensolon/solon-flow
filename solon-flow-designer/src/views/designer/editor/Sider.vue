<template>
    <a-collapse :bordered="false" defaultActiveKey="switch" :activeKey="state.activeKeys" @change="onChange">
        <a-collapse-panel v-for="group in groupMap" :key="group.value" :header="group.title">
            <a-space direction="vertical" style="width: 100%;">
                <div class="node" v-for="nodeType in group.nodes" :key="nodeType"
                    @mousedown="e => toStartDrag(e, nodeTypeDef[nodeType])">
                    <div class="node-icon" :style="{ 'background-color': nodeTypeDef[nodeType].color }">
                        <font-awesome-icon :icon="nodeTypeDef[nodeType].icon" />
                    </div>
                    <div class="node-title">{{ nodeTypeDef[nodeType].title }}</div>
                </div>
            </a-space>
        </a-collapse-panel>
    </a-collapse>

</template>
<script setup>
import { onMounted,reactive } from 'vue';
import { nodeTypeDef, groupMap } from '../nodeTypeDef.js';
const emit = defineEmits(['startDrag']); // 定义emi

const state = reactive({
    activeKeys: ['switch','gateway','activity'], // 初始展开的面板
});
onMounted(() => {
    // console.log(nodeTypeDef);
    // console.log(groupMap);
})

function toStartDrag(e, nodeType) {
    emit('startDrag', { e, nodeType });
}

function onChange(activeKeys) {
    // console.log(activeKeys); // 打印 activeKey
    state.activeKeys = activeKeys; // 更新 activeKeys
}
</script>
<style scoped lang="less"></style>
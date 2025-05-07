<template>
    <a-collapse defaultActiveKey="switch">
        <a-collapse-panel v-for="group in groupMap" :key="group.value" :header="group.name">
            <div class="node" v-for="nodeType in group.nodes" :key="nodeType">
                <div class="node-icon" :style="{'background-color':nodeTypeDef[nodeType].color}">
                    <font-awesome-icon :icon="nodeTypeDef[nodeType].icon" />
                </div>
                <div class="node-title">{{ nodeTypeDef[nodeType].name }}</div>
            </div>
        </a-collapse-panel>
    </a-collapse>
    
</template>
<script setup>
import { onMounted } from 'vue';
import {nodeTypeDef,groupMap} from '../nodeTypeDef.js';
onMounted(() => {
    console.log(nodeTypeDef);
    console.log(groupMap);
})
</script>
<style scoped lang="less">
    .node {
        padding: 10px;
        border: 1px solid #ccc;
        margin: 5px;
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
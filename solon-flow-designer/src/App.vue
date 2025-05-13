<template>
  <a-config-provider :locale="zhCN">
  <a-layout>
    <a-layout-header>
      <div class="logo">Solon Flow Designer</div>
      <a-menu theme="dark" mode="horizontal" :selectedKeys="state.selectedKeys" @click="goTo">
        <a-menu-item key="Home">HOME</a-menu-item>
        <a-menu-item key="Design">设计工具</a-menu-item>
        <a-menu-item key="AIFLOW">AI编排（未完成）</a-menu-item>
      </a-menu>
    </a-layout-header>
    <a-layout-content>
      <router-view v-slot="{ Component }">
        <keep-alive>
          <component :is="Component" />
        </keep-alive>
      </router-view>
    </a-layout-content>
  </a-layout>
</a-config-provider>
</template>
<script setup>
import zhCN from 'ant-design-vue/es/locale/zh_CN';
import { watch, reactive } from 'vue';
import { useRouter } from 'vue-router';
const router = useRouter();

watch(
  () => router.currentRoute.value.name,
  (v) => {
    state.selectedKeys = [v]; // 监听路由变化并更新 selectedKeys 状态
  }
)

const state = reactive({
  selectedKeys: [],
});

function goTo(item) {
  router.push({ name: item.key });
}
</script>
<style scoped>
.logo {
  width: 200px;
  color: #fff;
  line-height: 32px;
  margin: 16px 24px 16px 0;
  font-size: 18px;
  font-weight: bold;
  float: left;
}

.ant-layout {
  height: 100%;
}
</style>

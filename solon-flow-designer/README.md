# 说明
Solon flow designer 是 vue3 + vite + antd + x6 开发的流程设计器。

目的是为了提供一个简单易用的流程设计器，支持流程的设计、导入、导出等功能。

# 依赖组件
|组件|版本|说明|网站|
|-|-|-|-|
|vue|3.5|前端基础框架|https://cn.vuejs.org/|
|vue-router|4.5|前端路由|https://router.vuejs.org/|
|fortawesome||图标库|https://fontawesome.com/|
|codemirror|6.8|代码脚本编辑工具|https://codemirror.net/|
|ant x6|2.18|流程图框架|http://x6.antv.antgroup.com|
|vite|6.3|前端工具链|https://cn.vitejs.dev/|

# 安装和运行

1、安装node，访问Node.js 中文网 (nodejs.com.cn)

安装完成后，可以通过命令行输入以下命令查看版本号
```bash
node -v
npm -v
```

2、执行npm install
根据package.json中的依赖版本下载依赖包

```bash
npm install
```

3、运行

```bash
npm run dev
```

# 功能及计划
- [x] 基本功能
- [x] 导入导出功能
- [x] 节点编辑功能
- [x] 节点连线功能
- [x] 节点删除功能
- [x] 节点缩放功能
- [x] 节点移动功能
- [x] 节点拖拽功能
- [x] 连线删除功能
- [x] 连线编辑功能
- [x] 支持流程的设计、导入、导出等功能
- [ ] 支持单步调试

# 代码说明

## 1、节点定义

如果有新的节点类型，需要在 `src/views/designer/nodeTypeDef.js` 中定义。

nodeTypeDef 属性定义节点的基本信息、样式、图标

groupMap 属性表示分组

## 2、节点对应的编辑表单

不同的节点类型，有不同的编辑表单
一个表单就是一个vue组件

首先在 `src/views/designer/nodeForm` 下面定义好表单文件vue

然后在 `src/views/designer/editor/NodeFormDialog.vue` 中引入组件，并在 nodeTypeFormMap 中声明，key 是节点定义的类型，value 是组件




# 注意

1、配置上下文根

在 `vite.config.js` 中，配置 base 属性，指定上下文根。

```js
base: '/solon-flow-designer/'
```

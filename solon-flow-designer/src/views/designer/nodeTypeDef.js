const nodeTypeDef = {
    "start": {
        "type": "start",
        "title": "开始",
        "color": "#0820e2",
        "icon": "fa-solid fa-play",
    },
    "end": {
        "type": "end",
        "title": "结束",
        "color": "#5d1dd0",
        "icon": "fa-solid fa-stop",
        validateMagnet(graph, cell) {
            return false
        }
    },
    "inclusive": {
        "type": "inclusive",
        "title": "包含网关",
        "color": "#f1711f",
        "icon": "fa-solid fa-genderless",
    },
    "exclusive": {
        "type": "exclusive",
        "title": "排他网关",
        "color": "#ae0e6a",
        "icon": "fa-solid fa-xmark",
    },
    "parallel": {
        "type": "parallel",
        "title": "并行网关",
        "color": "#7e48ee",
        "icon": "fa-solid fa-plus",
    },
    "activity": {
        "type": "activity",
        "title": "活动节点",
        "color": "#047cc2",
        "icon": "fa-solid fa-user",
    },
    "iterator": {
        "type": "iterator",
        "title": "循环网关",
        "color": "#00b401",
        "icon": "fa-solid fa-repeat",
        validateMagnet(graph, cell) {
            const edges = graph.getOutgoingEdges(cell)
            return !edges || edges.length < 1
        },
        validateConnection(graph, srccell, targetcell) {
            const edges = graph.getIncomingEdges(targetcell)
            return !edges || edges.length < 1
        }
    }
    /*
    ,
    "script": {
        "type": "script",
        "title": "脚本节点",
        "color": "#2cdcae",
        "icon": "fa-solid fa-file",
    },
    "subFlow": {
        "type": "subFlow",
        "title": "子流程",
        "color": "#f4a63a",
        "icon": "fa-solid fa-diagram-next",
    },
    */
}

const groupMap = [
    { "title": "开关节点", "value": "switch", "nodes": ["start", "end"] },
    { "title": "网关节点", "value": "gateway", "nodes": ["inclusive", "exclusive", "parallel", "iterator"] },
    { "title": "活动节点", "value": "activity", "nodes": ["activity"] }
]

export { nodeTypeDef, groupMap }
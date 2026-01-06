package features.flow.manual.app;

import java.util.HashMap;
import java.util.Map;

/*
 * 节点之间数据传输对象
 */
public class NodeTransData {
    private Map<String,Map<String,Object>> dataMap = new HashMap<>();

    public Map<String, Map<String, Object>> getDataMap() {
        return dataMap;
    }

    public Map<String, Object> getNodeData(String nodeId) {
        return dataMap.get(nodeId);
    }

    public <T> T getNodeData(String nodeId,String key){
        Map<String,Object> nodeData = dataMap.get(nodeId);
        if(nodeData == null){
            return null;
        }
        return (T)nodeData.get(key);
    }

    public <T> T getRefNodeData(String ref){
        String[] refs = ref.split("/");
        return this.getNodeData(refs[1], refs[3]);
    }

    public void putNodeData(String nodeId,String key,Object value){
        Map<String,Object> nodeData = dataMap.get(nodeId);
        if(nodeData == null){
            nodeData = new HashMap<>();
            dataMap.put(nodeId, nodeData);
        }
        nodeData.put(key, value);
    }
}

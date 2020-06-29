package com.prism.pom.vo;

import com.prism.pom.service.treeTextParser.core.Node;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

public class DepTree
{
    /**
     * 依赖树
     */
    private Map<String, Node> dependenciesTreeMap = new TreeMap<>();
    /**
     * 模块
     */
    private Map<String, Node> modules = new LinkedHashMap<>();
    /**
     * 属性值表
     */
    private Map<String, PackageProp> propMap = new HashMap<>();
    /**
     * 节点表
     */
    private Map<String, Node> dependencies = new HashMap<>();

    public void addDependenciesTree(String key, Node node) {
        dependenciesTreeMap.put(key, node);
    }

    public Node getModule(String groupId, String artifactId)
    {
        return modules.get(groupId + artifactId);
    }

    public void addModule(Node module)
    {
        modules.put(module.getGroupId() + module.getArtifactId(), module);
    }

    public Map<String, Node> getPlatformDepsTree() {
        return dependenciesTreeMap;
    }

    public Node getTree(String key) {
        return dependenciesTreeMap.get(key);
    }

    public Map<String, PackageProp> getPropMap()
    {
        return propMap;
    }

    public void addProp(String key, PackageProp packageProp)
    {
        this.propMap.put(key, packageProp);
    }

    public PackageProp getProp(String key)
    {
        return propMap.get(key);
    }

    public Node getDependency(String path , String groupId, String artifactId)
    {
        return dependencies.get(path + groupId + artifactId);
    }

    public void addDependencies(String path, Node node)
    {
        this.dependencies.put(path + node.getGroupId() + node.getArtifactId(), node);
    }
}

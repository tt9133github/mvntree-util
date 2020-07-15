package com.prism.pom.api;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.prism.pom.service.FileUtil;
import com.prism.pom.service.MvnTreeCommandParser;
import com.prism.pom.service.PomTxtParser;
import com.prism.pom.service.treeTextParser.core.Node;
import com.prism.pom.vo.DepTree;

import java.io.*;
import java.util.List;

public class DependencyTreeUtil
{
    public String pomparse(String jsonStr, String mvnHome) throws Exception
    {
        MvnTreeCommandParser mvnTreeCommandParser = new MvnTreeCommandParser();
        PomTxtParser pomTxtParser = new PomTxtParser();
        JSONObject json = parseToJson(jsonStr);
        JSONArray depManagerFiles = (JSONArray) json.get("files");
        JSONObject result = new JSONObject();
        List<Node> trees = mvnTreeCommandParser.mvntreeParse(depManagerFiles, mvnHome);
        DepTree depTree = pomTxtParser.parseRepoFiles(depManagerFiles);
        for (Node node : trees)
        {
            resetNodePath(depTree, node);
        }

        if (!trees.isEmpty())
        {
            result.put("status", "200");
            result.put("msg", "success");
            result.put("content", trees.toString());
        }
        else
        {
            result.put("status", "400");
            result.put("msg", "failure");
            //mvnTreeCommandParser.delFile(new File(System.getProperty("java.io.tmpdir") + tmpFileName));
        }
        return result.toJSONString();
    }

    private void resetNodePath(DepTree depTree, Node node)
    {
        Node moduleNode = null;
        if (node.getScope() == null)
        {
            moduleNode = depTree.getModule(node.getGroupId(), node.getArtifactId());
            node.setPath(moduleNode.getPath());
           for(Node dep : node.getChildNodes())
           {
               resetNodePath(depTree, dep);
           }
        }
        else
        {
            Node topNode = getThisNodeModule(node);
            moduleNode = depTree.getModule(topNode.getGroupId(), topNode.getArtifactId());
            Node txtParsedNode = findNodeInThisModule(moduleNode, node);
            if (txtParsedNode == null)
            {
                System.out.println("error");
            }
            else
            {
                setAllNodesPath(node, txtParsedNode.getPath());
            }
        }
    }

    private Node findNodeInThisModule(Node moduleNode, Node node)
    {
        if (moduleNode.getGroupId().equals(node.getGroupId()) && moduleNode.getArtifactId().equals(node.getArtifactId()))
        {
            return moduleNode;
        }
        for (Node child : moduleNode.getChildNodes())
        {
            if (child.getGroupId().equals(node.getGroupId()) && child.getArtifactId().equals(node.getArtifactId()))
            {
                return child;
            }
        }
        return null;
    }

    private void setAllNodesPath(Node node, String path)
    {
        node.setPath(path);
        for (Node child : node.getChildNodes())
        {
            setAllNodesPath(child, path);
        }
    }

    private Node getThisNodeModule(Node node)
    {
        if (node.getScope() != null)
        {
            return getThisNodeModule(node.getParent());
        }
        return node;
    }

    private JSONObject parseToJson(String jsonStr) throws UnsupportedEncodingException
    {
        jsonStr = java.net.URLDecoder.decode(jsonStr, "utf-8");
        if (jsonStr.endsWith("="))
        {
            jsonStr = jsonStr.substring(0, jsonStr.length() - 1);
        }
        return JSONObject.parseObject(jsonStr);
    }


}

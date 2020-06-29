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
        for(Node node : trees)
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
        Node moduleNode = depTree.getModule(node.getGroupId(), node.getArtifactId());
        if(moduleNode != null)
        {
            node.setPath(moduleNode.getPath());
            for(Node child : node.getChildNodes())
            {
                resetNodePath(depTree, child);
            }
        }
        else
        {
            Node topNode = getThisNodeModule(node);
            moduleNode = depTree.getModule(topNode.getGroupId(), topNode.getArtifactId());
            Node txtParsedNode = depTree.getDependency(moduleNode.getPath(), node.getGroupId(), node.getArtifactId());
            if (txtParsedNode != null)
            {
                setAllNodesPath(node, txtParsedNode.getPath());
            }
            else
            {
                setAllNodesPath(node, topNode.getPath());
            }
        }
    }

    private void setAllNodesPath(Node node, String path)
    {
        node.setPath(path);
        for(Node child : node.getChildNodes())
        {
            setAllNodesPath(child, path);
        }
    }

    private Node getThisNodeModule(Node node)
    {
        if(node.getScope() != null)
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

    public static void main(String[] args) throws Exception
    {
        String mavenHome = "C:\\Program Files\\JetBrains\\IntelliJ IDEA 2019.2.3\\plugins\\maven\\lib\\maven3";
        String projectPath = "C:\\code\\test\\guns";
        JSONArray filesArray = new JSONArray();
        addPomToArray(projectPath, new File(projectPath), filesArray);

        JSONObject input = new JSONObject();
        input.put("files", filesArray);

        DependencyTreeUtil d = new DependencyTreeUtil();
        String resultJson = d.pomparse(input.toJSONString(), mavenHome);
        JSONObject result = (JSONObject) JSONObject.parse(resultJson);

        System.out.println(result);
    }

    private static void addPomToArray(String originalPath, File file, JSONArray files)
    {
        for(File f : file.listFiles())
        {
            if(f.isDirectory())
            {
                addPomToArray(originalPath, f, files);
            }
            if(f.getPath().contains("pom.xml"))
            {
                JSONObject json = new JSONObject();
                json.put("path", f.getAbsolutePath().replace(originalPath, ""));
                json.put("content", FileUtil.getFileContent(f));
                files.add(json);
            }
        }
    }

}

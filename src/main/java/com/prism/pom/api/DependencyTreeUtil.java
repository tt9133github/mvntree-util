package com.prism.pom.api;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.prism.pom.service.DependenciesTreeService;

import com.prism.pom.service.treeTextParser.core.Node;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

import java.io.*;
import java.rmi.ConnectIOException;
import java.util.*;

public class DependencyTreeUtil
{
    public String pomparse(String jsonStr, String mvnHome) throws Exception
    {
        DependenciesTreeService dependenciesTreeService = new DependenciesTreeService();
        JSONObject json = parseToJson(jsonStr);
        JSONArray depManagerFiles = (JSONArray) json.get("files");
        String tmpFileName = RandomStringUtils.randomAlphanumeric(10);
        String outputPath = System.getProperty("java.io.tmpdir") + tmpFileName + File.separator + "pom.txt";
        formDirectory(dependenciesTreeService, depManagerFiles, tmpFileName);

        List<Node> trees = new LinkedList<>();
        Map<String, String> modulePathMap = new HashMap<>();
        Map<String, String> moduleContentMap = new HashMap<>();
        depManagerFiles = orderByPath(depManagerFiles);

        for (int i = 0; i < depManagerFiles.size(); i++)
        {
            JSONObject elem = depManagerFiles.getJSONObject(i);
            String path = elem.getString("path");
            String content = elem.getString("content");
            Model model = null;
            try
            {
                Reader fr = new StringReader(content.trim());
                MavenXpp3Reader xpp3Reader = new MavenXpp3Reader();
                model = xpp3Reader.read(fr);
                modulePathMap.put(model.getGroupId() + model.getArtifactId(), path);
                moduleContentMap.put(model.getGroupId() + model.getArtifactId(), content);
                boolean isMulti = model.getModules() != null;
                if(isParsed(trees, model.getArtifactId()))
                {
                    continue;
                }
                String inputPath = System.getProperty("java.io.tmpdir") + tmpFileName;
                Node node = dependenciesTreeService.pomTreeParse(isMulti, inputPath, outputPath, mvnHome);
                if(node == null)
                {
                    continue;
                }
                trees.add(node);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                continue;
            }
        }
        for(Node tree : trees)
        {
            dependenciesTreeService.setNodeFile(tree, modulePathMap, moduleContentMap);
        }

        JSONObject result = new JSONObject();

        if (!trees.isEmpty())
        {
            result.put("status", "200");
            result.put("msg", "success");
            result.put("content", trees.toString());
            dependenciesTreeService.delFile(new File(System.getProperty("java.io.tmpdir") + tmpFileName));
        }
        else
        {
            result.put("status", "400");
            result.put("msg", "failure");
            //dependenciesTreeService.delFile(new File(System.getProperty("java.io.tmpdir") + tmpFileName));
        }

        return result.toJSONString();
    }

    private JSONArray orderByPath(JSONArray depManagerFiles)
    {
        Map<String, JSONObject> pathMap = new HashMap<>();
        List<String> pathList = new LinkedList<>();
        for (Object m : depManagerFiles)
        {
            String path = (String) ((JSONObject)m).get("path");
            pathMap.put(path, (JSONObject)m);
            pathList.add(path);
        }

        Collections.sort(pathList, Comparator.comparing(item -> item.length()));
        JSONArray result = new JSONArray();
        Iterator<String> iterator = pathList.iterator();
        while (iterator.hasNext())
        {
            String path = iterator.next();
            result.add(pathMap.get(path));
        }

        return result;
    }

    private boolean isParsed(List<Node> parsedNodes, String artifactId)
    {
        for (Node parsedNode : parsedNodes)
        {
            if (artifactId.equalsIgnoreCase(parsedNode.getArtifactId()))
            {
                return true;
            }
            if (parsedNode.getChildNodes() != null && !parsedNode.getChildNodes().isEmpty())
            {
                boolean b = isParsed(parsedNode.getChildNodes(), artifactId);
                if (b)
                {
                    return true;
                }
            }
        }
        return false;
    }

    private void formDirectory(DependenciesTreeService dependenciesTreeService, JSONArray depManagerFiles, String tmpFileName) throws IOException
    {
        boolean isMulti = (depManagerFiles.size() > 1);

        if (isMulti)
        {
            for (int i = 0; i < depManagerFiles.size(); i++)
            {
                JSONObject elem = depManagerFiles.getJSONObject(i);
                String path = elem.getString("path");
                String content = elem.getString("content");
                dependenciesTreeService.writefile(tmpFileName, path, content);
            }
        }
        else
        {
            JSONObject elem = depManagerFiles.getJSONObject(0);
            String path = elem.getString("path");
            String content = elem.getString("content");
            dependenciesTreeService.writefile(tmpFileName, path, content);
        }
    }

    public String gradleTree(String jsonStr) throws Exception
    {
        DependenciesTreeService dependenciesTreeService = new DependenciesTreeService();

        JSONObject json = parseToJson(jsonStr);
        String executePath = (String) json.get("path");
        JSONArray depManagerFiles = (JSONArray) json.get("files");
        JSONObject result = new JSONObject();
        String projectName = "";//MatchUtil.match((String) depManagerFiles.getJSONObject(0).get("url"), "\\w+$");
        boolean isMulti = (depManagerFiles.size() > 1);

        formDirectory(dependenciesTreeService, depManagerFiles, projectName);

        String treeTxt = dependenciesTreeService.gradleTreeParse(null);
        if (!treeTxt.isEmpty())
        {
            result.put("status", "200");
            result.put("msg", "success");
            result.put("projectname", projectName);
            result.put("content", treeTxt);
            dependenciesTreeService.delFile(new File(System.getProperty("java.io.tmpdir") + projectName));
        }
        else
        {
            result.put("status", "400");
            result.put("msg", "failure");
            result.put("projectname", projectName);
            dependenciesTreeService.delFile(new File(System.getProperty("java.io.tmpdir") + projectName));
        }

        return result.toJSONString();
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

    public String goTree(JSONObject json) throws Exception
    {
        DependenciesTreeService dependenciesTreeService = new DependenciesTreeService();

        //JSONObject json = parseToJson(jsonStr);
        List depManagerFiles = (List) json.get("files");
        String projectName = UUID.randomUUID().toString();

        //构建go目录
        for (int i = 0; i < depManagerFiles.size(); i++)
        {
            LinkedHashMap gradle = (LinkedHashMap) depManagerFiles.get(i);
            String path = (String) gradle.get("path");
            String gradleContent = (String) gradle.get("content");
            dependenciesTreeService.writefile(projectName, path, gradleContent);
        }
        String tmppath = System.getProperty("java.io.tmpdir") + projectName + File.separator + "go.mod";

        String treeTxt = dependenciesTreeService.goTreeParse(tmppath);
        if(StringUtils.isEmpty(treeTxt))
        {
            //logger.info("failed to generate go tree, path: {}", System.getProperty("java.io.tmpdir") + projectName);
        }
        else
        {
            dependenciesTreeService.delFile(new File(System.getProperty("java.io.tmpdir") + projectName));
        }

        return treeTxt;
    }

}

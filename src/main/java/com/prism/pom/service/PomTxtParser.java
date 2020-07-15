package com.prism.pom.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.prism.pom.service.treeTextParser.core.Node;
import com.prism.pom.vo.DepTree;
import com.prism.pom.vo.PackageProp;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

import java.io.BufferedReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.*;

public class PomTxtParser extends FileUtil
{
    public DepTree parseRepoFiles(JSONArray depManagerFiles)
    {
        DepTree depTree = new DepTree();
        depManagerFiles = orderByPath(depManagerFiles);
        Map<String, Model> models = new LinkedHashMap<>();
        for (int i = 0; i < depManagerFiles.size(); i++)
        {
            JSONObject elem = depManagerFiles.getJSONObject(i);
            String content = (String) elem.get("content");
            String path = (String) elem.get("path");

            Model model = null;
            try
            {
                Reader fr = new StringReader(content);
                MavenXpp3Reader xpp3Reader = new MavenXpp3Reader();
                model = xpp3Reader.read(fr);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                continue;
            }
            //parse properties
            model.setName(path);
            model.setDescription(content);
            if (model.getGroupId() == null && model.getParent() != null)
            {
                model.setGroupId(model.getParent().getGroupId());
            }
            models.put(model.getGroupId() + model.getArtifactId(), model);
        }

        //parse module
        for (Model model : models.values())
        {
            parsePropMap(model, depTree);
            parseDependencyManagement(model, depTree);

            String path = model.getName();
            Node moduleNode = new Node(model.getGroupId(), model.getArtifactId(), model.getPackaging(), null, model.getModelVersion(), null, null, false);
            moduleNode.setPath(path);
            Parent parent = model.getParent();
            if (parent != null)
            {
                Node parentNode = depTree.getModule(parent.getGroupId(), parent.getArtifactId());
                if (parentNode == null)
                {
                    /**
                     * <parent>
                     *     <groupId>org.springframework.boot</groupId>
                     *     <artifactId>spring-boot-starter-parent</artifactId>
                     *     <version>2.1.7.RELEASE</version>
                     * </parent>
                     */
                    //spring boot parent 管理的依赖须请求远端数据库
                    PackageProp packageProp = new PackageProp();
                    packageProp.setFile(model.getName());
                    packageProp.setLines(getLineNumberByArtifactIdAndVersion(model.getDescription(), parent.getArtifactId(), parent.getVersion()));
                    packageProp.setValue(parent.getVersion());
                    depTree.addProp("parent", packageProp);
                    depTree.addDependenciesTree(path, moduleNode);
                }
                else
                {
                    if (moduleNode.getGroupId() == null)
                    {
                        moduleNode.setGroupId(parent.getGroupId());
                    }
                    parentNode.addChildNode(moduleNode);
                }
            }
            else
            {
                depTree.addDependenciesTree(moduleNode.getPath(), moduleNode);
            }
            depTree.addModule(moduleNode);
        }

        //parse dependencies
        for (Model model : models.values())
        {
            parsePropMap(model, depTree);
            Node moduleNode = depTree.getModule(model.getGroupId(), model.getArtifactId());
            parseModuleDependencies(moduleNode, model, depTree);
        }

        return depTree;
    }


    private Integer getLineNumberByArtifactIdAndVersion(String source, String artifactId, String version)
    {
        int lineNum = 0;
        boolean matchArtifactId = false;
        int linesFormArtifactId = 1;
        try (BufferedReader br = new BufferedReader(new StringReader(source)))
        {
            String line = null;
            while ((line = br.readLine()) != null)
            {
                lineNum++;
                if (isCommentLine(line) || isExclusionLine(line))
                {
                    linesFormArtifactId = 1;
                    matchArtifactId = false;
                    continue;
                }
                if (!matchArtifactId)
                {
                    if (line.indexOf("<artifactId>" + artifactId + "</artifactId>") > 0)
                    {
                        matchArtifactId = true;
                        continue;
                    }
                }
                else
                {
                    if (line.trim().isEmpty())
                    {
                        continue;
                    }
                    else if (line.indexOf(version) > 0)
                    {
                        return lineNum;
                    }
                    else
                    {
                        if (linesFormArtifactId < 1)
                        {
                            return 0;
                        }
                        linesFormArtifactId--;
                    }
                }
            }
        }
        catch (Exception e)
        {
        }
        return 0;
    }

    private Node parseModuleDependencies(Node module, Model model, DepTree depTree)
    {
        String content = model.getDescription();
        String path = model.getName();
        Map<String, PackageProp> propMap = depTree.getPropMap();

        Node moduleNode = new Node(model.getGroupId(), model.getArtifactId(), null, null, model.getVersion(), null, null, false);
        depTree.addDependencies(path, moduleNode);

        for (org.apache.maven.model.Dependency d : model.getDependencies())
        {
            String version = d.getVersion();
            String groupId = d.getGroupId();
            String artifactId = d.getArtifactId();
            Integer line = null;
            String depSourceFile = null;
            //find in properties or parent pom
            if (groupId != null && groupId.contains("$"))
            {
                PackageProp packageProp = getProp(propMap, groupId);
                if (packageProp == null)
                {
                    continue;
                }
                groupId = packageProp.getValue();
            }
            if (artifactId != null && artifactId.contains("$"))
            {
                PackageProp packageProp = getProp(propMap, artifactId);
                if (packageProp == null)
                {
                    continue;
                }
                artifactId = packageProp.getValue();
            }
            PackageProp packageProp = null;
            if (version != null && version.contains("$"))
            {
                packageProp = getProp(propMap, version);
            }
            else if (version == null)
            {
                //look up management
                packageProp = propMap.get(groupId + artifactId);
                if(packageProp == null)
                {
                    //then look up parent
                    packageProp = propMap.get("parent");
                }
            }
            if (packageProp != null)
            {
                version = packageProp.getValue();
                line = packageProp.getLines();
                depSourceFile = packageProp.getFile();
            }
            if (depSourceFile == null)
            {
                depSourceFile = path;
                line = getLineNumberByArtifactIdAndVersion(content, d.getArtifactId(), d.getVersion());
            }
            Node depNode = new Node(groupId, artifactId, null, null, version, d.getScope() == null ? "" : d.getScope(), null, false);
            depNode.setPath(depSourceFile + ":" + line);
            /*if (isModule(depTree, d))
            {
                depNode = depTree.getModule(d.getGroupId(), d.getArtifactId());
            }*/
            module.addChildNode(depNode);
            //depTree.addDependencies(path, depNode);
        }
        return module;
    }

    private PackageProp getProp(Map<String, PackageProp> propMap, String statement)
    {
        PackageProp packageProp = propMap.get(MatchUtil.matchColumn(statement, "\\$\\{(\\S+)\\}"));
        return packageProp;
    }

    private boolean isModule(DepTree depTree, org.apache.maven.model.Dependency d)
    {
        Node node = depTree.getModule(d.getGroupId(), d.getArtifactId());
        return node != null;
    }

    private void parsePropMap(Model model, DepTree depTree)
    {
        String content = model.getDescription();
        String path = model.getName();

        //<properties>
        Properties properties = model.getProperties();
        Iterator iterator = properties.entrySet().iterator();
        while (iterator.hasNext())
        {
            Map.Entry<String, String> entry = (Map.Entry<String, String>) iterator.next();
            String key = entry.getKey();
            String value = entry.getValue();
            int pos = getLineNumberByKeyWord(content, key);
            PackageProp packageProp = new PackageProp();
            packageProp.setFile(path);
            packageProp.setLines(pos == 0 ? null : pos);
            packageProp.setValue(value);
            depTree.addProp(key, packageProp);
        }
    }

    private Integer getLineNumberByKeyWord(String source, String key)
    {
        int lineNum = 0;
        try (BufferedReader br = new BufferedReader(new StringReader(source)))
        {
            String line = null;
            while ((line = br.readLine()) != null)
            {
                lineNum++;
                if (line.indexOf(key.trim()) > 0)
                {
                    return lineNum;
                }
            }
        }
        catch (Exception e)
        {

        }
        return 0;
    }

    private void parseDependencyManagement(Model model, DepTree depTree)
    {
        DependencyManagement dependencyManagement = model.getDependencyManagement();
        if (dependencyManagement == null || dependencyManagement.getDependencies() == null)
        {
            return;
        }
        String content = model.getDescription();
        String path = model.getName();

        //<dependencyManagement>
        for (org.apache.maven.model.Dependency d : dependencyManagement.getDependencies())
        {
            if (d.getVersion().contains("$"))
            {
                String key = MatchUtil.matchColumn(d.getVersion(), "\\$\\{(\\S+)\\}");
                PackageProp packageProp = depTree.getProp(key);
                if (packageProp == null)
                {
                    //error
                    continue;
                }
                d.setVersion(packageProp.getValue());
                depTree.addProp(d.getGroupId() + d.getArtifactId(), depTree.getProp(key));
                depTree.addProp(d.getGroupId(), depTree.getProp(key));  //may be override
            }
            else
            {
                String version = d.getVersion();
                String key = d.getGroupId() + d.getArtifactId();
                PackageProp packageProp = new PackageProp();
                packageProp.setFile(path);
                packageProp.setValue(version);
                packageProp.setLines(getLineNumberByArtifactIdAndVersion(content, d.getArtifactId(), d.getVersion()));//wrong
                depTree.addProp(key, packageProp);
            }
        }
    }

    private static boolean isCommentLine(String line)
    {
        return MatchUtil.isMatch(line, "<!--(?:(?!-->).)+-->");
    }

    private static boolean isExclusionLine(String line)
    {
        return MatchUtil.isMatch(line, "exclusion");
    }
}

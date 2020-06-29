package com.prism.pom.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.prism.pom.service.treeTextParser.core.Node;
import com.prism.pom.service.treeTextParser.core.TextParser;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.shared.invoker.*;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.*;
import java.util.*;

public class MvnTreeCommandParser extends FileUtil
{
    String env = "dev";

    public List<Node> mvntreeParse(JSONArray depManagerFiles, String mvnHome) throws Exception
    {
        List<Node> trees = new LinkedList<>();
        String tmpFileName = RandomStringUtils.randomAlphanumeric(10);

        formDirectory(depManagerFiles, tmpFileName);
        depManagerFiles = orderByPath(depManagerFiles);

        for (int i = 0; i < depManagerFiles.size(); i++)
        {
            JSONObject elem = depManagerFiles.getJSONObject(i);
            String path = elem.getString("path");
            String content = elem.getString("content");
            Reader fr = new StringReader(content.trim());
            MavenXpp3Reader xpp3Reader = new MavenXpp3Reader();
            Model model = xpp3Reader.read(fr);
            boolean isMulti = model.getModules() != null;
            if (isParsed(trees, model.getArtifactId()))
            {
                continue;
            }
            String inputPath = System.getProperty("java.io.tmpdir") + tmpFileName + File.separator + path;
            String outputPath = System.getProperty("java.io.tmpdir") + tmpFileName + File.separator + path.replace(".xml", ".txt");

            Integer state = doCommand(isMulti, inputPath, outputPath, mvnHome);

            if (state == 0 || state == 1)
            {
                Node node = generateNodeByTreeTxt(outputPath, isMulti);
                if (node == null)
                {
                    continue;
                }
                trees.add(node);
            }
        }

        if (!trees.isEmpty())
        {
            delFile(new File(System.getProperty("java.io.tmpdir") + tmpFileName));
        }
        else
        {
            //mvnTreeCommandParser.delFile(new File(System.getProperty("java.io.tmpdir") + tmpFileName));
        }
        return trees;
    }

    private void setTreeNodesPath(Node node, String path)
    {
        node.setPath(path);
        for (Node child : node.getChildNodes())
        {
            setTreeNodesPath(child, path);
        }
    }

    private Node generateNodeByTreeTxt(String outputPath, boolean isMulti) throws IOException
    {
        Reader r = null;
        Node node = null;
        try
        {
            r = new BufferedReader(new InputStreamReader(new FileInputStream(new File(outputPath))));
            TextParser parser = new TextParser();
            node = parser.parse(r, isMulti);
        }
        catch (Exception e)
        {
            System.out.println(e);
        }
        finally
        {
            r.close();
        }
        return node;
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

    public String mvnBuild(String path)
    {
        String result = "";
        try
        {
            File dir = new File(new File(path).getParent());//此处是指定路径
            Process process;
            if ("dev".equalsIgnoreCase(env))
            {
                String[] cmd = new String[]{"cmd", "/c", "mvn install -DskipTests"};
                process = Runtime.getRuntime().exec(cmd, null, dir);
            }
            else
            {
                String cmd = "mvn install -DskipTests";
                process = Runtime.getRuntime().exec(cmd, null, dir);
            }

            // 记录dos命令的返回信息
            StringBuffer resStr = new StringBuffer();
            // 获取返回信息的流
            InputStream in = process.getInputStream();
            Reader reader = new InputStreamReader(in);
            BufferedReader bReader = new BufferedReader(reader);
            for (String res = ""; (res = bReader.readLine()) != null; )
            {
                resStr.append(res + "\n");
            }
            result = resStr.toString();
            bReader.close();
            reader.close();
            in.close();
            process.getOutputStream().close();  // 不要忘记了一定要关
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return result;
    }

    private Integer doCommand(boolean isMulti, String path, String outputFile, String mavenHome) throws Exception
    {
        File txt = new File(outputFile);
        txt.createNewFile();
        Integer state = -1;
        InvocationRequest invocationRequest = new DefaultInvocationRequest();
        invocationRequest.setPomFile(new File(path));
        if (!isMulti)
        {
            invocationRequest.setGoals(Collections.singletonList("dependency:tree --fail-never" + " -DoutputFile=" + outputFile + " -DoutputType=text"));
        }
        else
        {
            invocationRequest.setGoals(Collections.singletonList("dependency:tree --fail-never" + " -DoutputFile=" + outputFile + " -DoutputType=text -DappendOutput=true"));
        }

        Invoker invoker = new DefaultInvoker();
        invoker.setMavenHome(new File(mavenHome));
        invoker.setLogger(new PrintStreamLogger(System.err, InvokerLogger.ERROR)
        {

        });
        invoker.setOutputHandler(new InvocationOutputHandler()
        {
            public void consumeLine(String s) throws IOException
            {

            }
        });
        InvocationResult result = invoker.execute(invocationRequest);
        state = result.getExitCode();


        return state;
    }
}

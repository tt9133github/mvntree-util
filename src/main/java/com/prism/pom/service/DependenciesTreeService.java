package com.prism.pom.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.prism.pom.api.DependencyTreeUtil;
import com.prism.pom.service.treeTextParser.core.Node;
import com.prism.pom.service.treeTextParser.core.TextParser;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.shared.invoker.*;

import javax.swing.text.AbstractDocument;
import java.io.*;
import java.util.*;

public class DependenciesTreeService
{
    String env = "dev";

    public static void main(String[] args) throws Exception
    {
        String mavenHome = "C:\\Program Files\\JetBrains\\IntelliJ IDEA 2019.3.4\\plugins\\maven\\lib\\maven3";
        String path1 = "C:\\code\\test\\ForTest-master\\ForTest-master\\java\\maven\\project-aggregation\\pom.xml";
        String path2 = "C:\\code\\test\\ForTest-master\\ForTest-master\\java\\maven\\project-aggregation\\submodule-1\\pom.xml";
        String path3 = "C:\\code\\test\\ForTest-master\\ForTest-master\\java\\maven\\project-aggregation\\submodule-2\\pom.xml";

        DependencyTreeUtil d = new DependencyTreeUtil();
        JSONArray files = new JSONArray();
        JSONObject json1 = new JSONObject();
        json1.put("path", "pom.xml");
        json1.put("content", getFileContent(new File(path1)));
        files.add(json1);

        JSONObject json2 = new JSONObject();
        json2.put("path", "submodule-1\\pom.xml");
        json2.put("content", getFileContent(new File(path2)));
        files.add(json2);

        JSONObject json3 = new JSONObject();
        json3.put("path", "submodule-2\\pom.xml");
        json3.put("content", getFileContent(new File(path3)));
        files.add(json3);

        JSONObject input = new JSONObject();
        input.put("files", files);
        String node = d.pomparse(input.toJSONString(), mavenHome);
        JSONObject result = (JSONObject) JSONObject.parse(node);
        System.out.println(node);
    }

    private static Node getModuleNode(Node node)
    {
        if (node.getScope() == null)
        {
            return node;
        }
        return getModuleNode(node.getParent());
    }

    public void setNodeFile(Node node, Map<String, String> modulePathMap, Map<String, String> moduleContentMap)
    {
        Node moduleNode = getModuleNode(node);
        String modulePath = modulePathMap.get(moduleNode.getGroupId() + moduleNode.getArtifactId());
        String moduleContent = moduleContentMap.get(moduleNode.getGroupId() + moduleNode.getArtifactId());
        int lineNumber = getLineNumberByArtifactIdAndVersion(moduleContent, node.getArtifactId(), node.getVersion());
        if(lineNumber == 0)
        {
            node.setPath(node.getParent().getPath());
        }
        else
        {
            node.setPath(modulePath + ":" + lineNumber);
        }
        for(Node child : node.getChildNodes())
        {
            setNodeFile(child, modulePathMap, moduleContentMap);
        }
    }

    /**
     * 删除文件夹
     *
     * @param file
     * @return
     */
    public boolean delFile(File file)
    {
        if (file.isDirectory())
        {
            String[] children = file.list();
            //递归删除目录中的子目录下
            for (int i = 0; i < children.length; i++)
            {
                boolean success = delFile(new File(file, children[i]));
                if (!success)
                {
                    return false;
                }
            }
        }
        // 目录此时为空，可以删除
        return file.delete();
    }

    protected String readtxt(String path) throws IOException
    {
        File file = new File(path);
        FileInputStream fis = null;
        InputStreamReader isr = null;
        BufferedReader br = null;
        StringBuffer sb = new StringBuffer();
        String temp = "";
        try
        {
            fis = new FileInputStream(file);
            isr = new InputStreamReader(fis);
            br = new BufferedReader(isr);
            while ((temp = br.readLine()) != null)
            {
                sb.append(temp);
                sb.append(System.getProperty("line.separator"));
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        } finally
        {
            if (fis != null)
            {
                fis.close();
            }
            if (isr != null)
            {
                isr.close();
            }
            if (br != null)
            {
                br.close();
            }
        }
        return sb.toString();
    }

    private static Integer getLineNumberByArtifactIdAndVersion(String source, String artifactId, String version)
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

    private static boolean isCommentLine(String line)
    {
        return MatchUtil.isMatch(line, "<!--(?:(?!-->).)+-->");
    }

    private static boolean isExclusionLine(String line)
    {
        return MatchUtil.isMatch(line, "exclusion");
    }

    public static String getFileContent(File file)
    {
        StringBuilder result = new StringBuilder();
        try
        {
            BufferedReader br = new BufferedReader(new FileReader(file));
            //构造一个BufferedReader类来读取文件
            String s = null;
            while ((s = br.readLine()) != null)
            {//使用readLine方法，一次读一行
                result.append(s + System.lineSeparator());
            }
            br.close();
        }
        catch (Exception e)
        {
        }
        return result.toString();
    }

    public void writefile(String projectname, String path, String pomcontent) throws IOException
    {
        String tmpdir = System.getProperty("java.io.tmpdir");
        //logger.info(tmpdir);

        FileOutputStream fos = null;
        PrintWriter pw = null;
        try
        {

            File file = new File(tmpdir + projectname + File.separator + path);
            if (!file.getParentFile().exists())
            {
                file.getParentFile().mkdirs();
            }
            if (!file.exists())
            {
                file.createNewFile();
            }

            StringBuffer buffer = new StringBuffer();

            fos = new FileOutputStream(file);
            pw = new PrintWriter(fos);
            buffer.append(pomcontent);
            pw.write(buffer.toString().toCharArray());
            pw.flush();

        }
        catch (Exception e)
        {
            e.printStackTrace();
        } finally
        {
            if (pw != null)
            {
                pw.close();
            }
            if (fos != null)
            {
                fos.close();
            }
        }
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
                String[] cmd = new String[]{"cmd", "/c",
                        "mvn install -DskipTests"
                };
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

    public Node pomTreeParse(boolean isMulti, String path, String outputFile, String mavenHome) throws IOException
    {
        File txt = new File(outputFile);
        try
        {
            txt.createNewFile();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        int state = -1;
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
        try
        {
            InvocationResult result = invoker.execute(invocationRequest);
            state = result.getExitCode();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        if (state == 0 || state == 1)
        {
            Reader r = null;
            Node node = null;
            try
            {
                r = new BufferedReader(new InputStreamReader(new FileInputStream(new File(outputFile))));
                TextParser parser = new TextParser();
                node = parser.parse(r, isMulti);
                delFile(new File(outputFile));
            }
            catch (Exception e)
            {
                System.out.println(e);
            } finally
            {
                r.close();
            }
            return node;
        }
        return null;
    }


    public String gradleTreeParse(String path)
    {
        String result = "";
        try
        {
            File dir = new File(new File(path).getParent());//此处是指定路径
            Process process;
            if ("dev".equalsIgnoreCase(env))
            {
                String[] cmd = new String[]{"cmd", "/c",
                        "gradle dependencies --configuration runtime"
                };
                process = Runtime.getRuntime().exec(cmd, null, dir);
            }
            else
            {
                String cmd = "gradle dependencies --configuration runtime";
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
            System.out.println(result);
            bReader.close();
            reader.close();
            process.getOutputStream().close();  // 不要忘记了一定要关

        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return result;
    }

    public String goTreeParse(String path)
    {
        String result = "";
        try
        {
            File dir = new File(new File(path).getParent());//此处是指定路径
            Process process;
            //Runtime.getRuntime().exec("set GOPROXY=https://goproxy.cn", null, dir);
            if ("dev".equalsIgnoreCase(env))
            {
                String[] cmd = new String[]{"cmd", "/c",
                        "go mod graph"
                };
                process = Runtime.getRuntime().exec(cmd, null, dir);
            }
            else
            {
                String cmd = "go mod graph";
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
            process.getOutputStream().close();  // 不要忘记了一定要关
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return result;
    }

}

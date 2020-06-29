package com.prism.pom.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.*;
import java.util.*;

public class FileUtil
{
    public JSONArray orderByPath(JSONArray depManagerFiles)
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

    public void formDirectory(JSONArray depManagerFiles, String tmpFileName)
    {
        boolean isMulti = (depManagerFiles.size() > 1);

        if (isMulti)
        {
            for (int i = 0; i < depManagerFiles.size(); i++)
            {
                JSONObject elem = depManagerFiles.getJSONObject(i);
                String path = elem.getString("path");
                String content = elem.getString("content");
                writefile(tmpFileName, path, content);
            }
        }
        else
        {
            JSONObject elem = depManagerFiles.getJSONObject(0);
            String path = elem.getString("path");
            String content = elem.getString("content");
            writefile(tmpFileName, path, content);
        }
    }

    public void writefile(String projectname, String path, String pomcontent)
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
                try
                {
                    fos.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
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
}

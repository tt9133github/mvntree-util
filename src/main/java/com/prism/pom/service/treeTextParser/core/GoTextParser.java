package com.prism.pom.service.treeTextParser.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public class GoTextParser
{
    public Node parse(String content) throws IOException
    {
        final BufferedReader br = new BufferedReader(new StringReader(content));
        Map<String, Node> nodeMap = new HashMap<>();
        Node mainNode = null;
        String line;
        while ((line = br.readLine()) != null)
        {
            Node node = parseLine(nodeMap, line);
            if(mainNode == null)
            {
                mainNode = node;
            }
        }

        Iterator<Map.Entry<String, Node>> iterator = nodeMap.entrySet().iterator();
        while (iterator.hasNext())
        {
            Map.Entry<String, Node> next = iterator.next();
            Node fatherNode = next.getValue();
            Iterator<Node> children = fatherNode.getChildNodes().iterator();
            while (children.hasNext())
            {
                Node child = children.next();
                Node find = nodeMap.get(child.getPackaging() + child.getVersion());
                if (find != null)
                {
                    for(Node secondChild : find.getChildNodes())
                    {
                        child.addChildNode(secondChild);
                    }
                }
            }
        }
        return mainNode;
    }

    private Node parseLine(Map<String, Node> nodeMap, String line)
    {
        String part[] = line.split(" ");
        if (part == null || part.length != 2)
        {
            //log here error line
            return null;
        }
        String sourcePart = part[0];
        String dependentPart = part[1];
        Node node = nodeMap.get(sourcePart);
        if (node == null)
        {
            node = extractNode(sourcePart);
            String key = getNodeKey(node);
            nodeMap.put(key, node);
        }
        Node child = extractNode(dependentPart);
        node.addChildNode(child);
        return node;
    }

    private String getNodeKey(Node node)
    {
        return node.getVersion() == null ? node.getPackaging() : node.getPackaging() + node.getVersion();
    }

    public static Node extractNode(String str)
    {
        String tmp[] = str.split("@");
        String nodeName = tmp[0];
        String version = tmp.length == 2 ? tmp[1] : null;
        return new Node(null, null, nodeName, null, version, null, null, false);
    }
}

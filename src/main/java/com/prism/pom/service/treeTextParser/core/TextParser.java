/**
 * Copyright 2011 Alexandre Dutra
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.prism.pom.service.treeTextParser.core;


import java.io.*;

public class  TextParser extends AbstractLineBasedParser
{

    public Node parse(Reader reader) throws ParseException
    {
        try
        {
            this.lines = splitLines(reader);
        }
        catch (IOException e)
        {
            throw new ParseException(e);
        }

        if (lines.isEmpty())
        {
            return null;
        }

        return parseInternal(0);
    }

    public Node parse(Reader reader, boolean isMulti) throws ParseException
    {
        this.isMulti = isMulti;
        try
        {
            this.lines = splitLines(reader);
            if(lastLineIsTopModuleLine())
            {
                swapLastLineToTop();
            }
        }
        catch (IOException e)
        {
            throw new ParseException(e);
        }

        if (lines.isEmpty())
        {
            return null;
        }

        return parseInternal(0);
    }

    private void swapLastLineToTop()
    {
        this.lines.add(0, this.lines.remove(this.lines.size() - 1));    //将最后一行移到第一行
    }

    private boolean lastLineIsTopModuleLine()
    {
        return this.lines.get(this.lines.size() - 1).matches("^\\w\\S+$");
    }

    private Node parseInternal(final int depth)
    {
        //current node
        final Node node = this.parseLine();

        this.lineIndex++;

        //children
        while (this.lineIndex < this.lines.size() && this.computeDepth(this.lines.get(this.lineIndex)) > depth)
        {
            final Node child = this.parseInternal(depth + 1);
            if (node != null)
            {
                node.addChildNode(child);
            }
        }
        return node;
    }

    private int computeDepth(final String line)
    {
        return isMulti ? getArtifactIndex(line) / 3 + 1 : getArtifactIndex(line) / 3;
    }

    /**
     * sample lineIndex structure:
     * <pre>|  |  \- org.apache.activemq:activeio-core:test-jar:tests:3.1.0:compile</pre>
     * @return
     */
    private Node parseLine()
    {
        String line = this.lines.get(this.lineIndex);
        String artifact;
        if (line.contains("active project artifact:"))
        {
            artifact = extractActiveProjectArtifact();
        }
        else
        {
            artifact = extractArtifact(line);
        }
        Node node = parseArtifactString(artifact);
        return node;
    }

    private String extractArtifact(String line)
    {
        return line.substring(getArtifactIndex(line));
    }

    private int getArtifactIndex(final String line)
    {
        for (int i = 0; i < line.length(); i++)
        {
            final char c = line.charAt(i);
            switch (c)
            {
                case ' '://whitespace, standard and extended
                case '|'://standard
                case '+'://standard
                case '\\'://standard
                case '-'://standard
                case '³'://extended
                case 'Ã'://extended
                case 'Ä'://extended
                case 'À'://extended
                    continue;
                default:
                    return i;
            }
        }
        return -1;
    }

    public static void main(String[] args)
    {
        try
        {
            Reader r = new BufferedReader(new InputStreamReader(new FileInputStream("C:\\Users\\40352\\AppData\\Local\\Temp\\lqVei1Lvp8\\pom.txt"), "UTF-8"));
            TextParser parser = new TextParser();
            Node node = parser.parse(r, true);
            StandardTextVisitor visitor = new StandardTextVisitor();
            visitor.visit(node);
            System.out.println(visitor);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}

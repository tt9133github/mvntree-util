/**
 * Copyright 2011 Alexandre Dutra
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.prism.pom.service.treeTextParser.core;

import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

public class TgfParser extends AbstractLineBasedParser {

    private static enum ParsePhase {
        NODE, EDGE
    }

    private Map<String, Node> nodes = new HashMap<String, Node>();

    private Node root;

    private ParsePhase phase = ParsePhase.NODE;

    public Node parse(Reader reader) throws ParseException {

        try {
            this.lines = splitLines(reader);
        } catch (IOException e) {
            throw new ParseException(e);
        }


        if(lines.isEmpty()) {
            return null;
        }

        for(; lineIndex < this.lines.size(); lineIndex++) {
            this.parseLine();
        }

        return root;

    }

    /**
     * sample line structure:
     * <pre>
     * -1437430659 com.ibm:mqjms:jar:6.0.0:runtime
     * #
     * 1770590530 96632433 compile
     * </pre>
     */
    private void parseLine() {
        String line = this.lines.get(this.lineIndex);
        if("#".equals(line)) {
            this.phase = ParsePhase.EDGE;
        } else if(this.phase == ParsePhase.NODE) {
            String id = StringUtils.substringBefore(line, " ");
            String artifact;
            if(line.contains("active project artifact:")) {
                artifact = extractActiveProjectArtifact();
            } else {
                artifact = StringUtils.substringAfter(line, " ");
            }
            Node node = parseArtifactString(artifact);
            if(root == null) {
                this.root = node;
            }
            nodes.put(id, node);
        } else {
            String parentId = StringUtils.substringBefore(line, " ");
            String childId = StringUtils.substringBetween(line, " ");
            Node parent = nodes.get(parentId);
            Node child = nodes.get(childId);
            parent.addChildNode(child);
        }
    }

}

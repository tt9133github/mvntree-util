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
package com.prism.pom.service.treeTextParser.extras;

import com.prism.pom.service.treeTextParser.core.Node;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.velocity.VelocityContext;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


public class JQueryJSTreeRenderer extends VelocityRenderer {

    private Map<String, String> imagesMap = new HashMap<String, String>();

    {
        imagesMap.put("jar", "img/common/jar.gif");
        imagesMap.put("war", "img/common/war.gif");
        imagesMap.put("ear", "img/common/ear.gif");
        imagesMap.put("pom", "img/common/pom.gif");
        imagesMap.put("plugin", "img/common/plugin.gif");
        imagesMap.put("bundle", "img/common/bundle.gif");
        imagesMap.put("default", "img/common/other.gif");
    }

    @Override
    protected String getTemplatePath() {
        return "/vm/jstree/index.vm";
    }

    @Override
    protected String getVelocityPropertiesPath() {
        return "/vm/jstree/velocity.properties";
    }

    @Override
    protected VelocityContext createVelocityContext(Node tree) {
        VelocityContext context = super.createVelocityContext(tree);
        context.put("imagesMap", imagesMap);
        return context;
    }

    @Override
    protected Collection<File> getFilesToCopy() {
        return FileUtils.listFiles(staticDir, FileFilterUtils.or(new PathContainsFilter("jstree"), new PathContainsFilter("common")), TrueFileFilter.TRUE);
    }

}

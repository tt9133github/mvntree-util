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




/**
 * Generates an output that is identical to the output generated by the following Maven command:
 * <pre>mvn dependency:tree -DoutputType=text -Dtokens=whitespace</pre>
 * @author Alexandre Dutra
 *
 */
public class WhitespaceTextVisitor extends AbstractTextVisitor {


    @Override
    public String getTreeSymbols(Node node) {
        return "   ";
    }

    @Override
    public String getParentTreeSymbols(Node node) {
        return "   ";
    }

}

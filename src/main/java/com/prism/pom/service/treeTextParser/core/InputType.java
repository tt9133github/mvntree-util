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
/**
 *
 */
package com.prism.pom.service.treeTextParser.core;



/**
 * @author Alexandre Dutra
 *
 */
public enum InputType {

    TEXT {

        @Override
        public Parser newParser() {
            return new TextParser();
        }
    },

    DOT {

        @Override
        public Parser newParser() {
            return new DotParser();
        }
    },

    GRAPHML {

        @Override
        public Parser newParser() {
            return new GraphmlParser();
        }
    },

    TGF {

        @Override
        public Parser newParser() {
            return new TgfParser();
        }
    };

    public abstract Parser newParser();

}

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.jcr.contentloader.it;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.tinybundles.core.TinyBundle;
import org.osgi.framework.Bundle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Basic test of a bundle that provides initial content */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class I18nInitialContentIT extends ContentloaderTestSupport {

    protected TinyBundle setupTestBundle(TinyBundle b) throws IOException {
        b.set(SLING_INITIAL_CONTENT_HEADER, DEFAULT_PATH_IN_BUNDLE + ";ignoreImportProviders:=json;path:=" + contentRootPath);
        addContent(b, DEFAULT_PATH_IN_BUNDLE, "i18n/en.json");
        addContent(b, DEFAULT_PATH_IN_BUNDLE, "i18n/en.json.xml");
        return b;
    }
    
    @Test
    public void bundleStarted() {
        final Bundle b = findBundle(bundleSymbolicName);
        assertNotNull("Expecting bundle to be found:" + bundleSymbolicName, b);
        assertEquals("Expecting bundle to be active:" + bundleSymbolicName, Bundle.ACTIVE, b.getState());
    }
    
    @Test
    public void i18nJsonFile() throws RepositoryException {
        final String filePath = contentRootPath + "/i18n/en.json"; 
        assertTrue("file node " + filePath + " exists", session.itemExists(filePath)); 
        Node node = session.getNode(filePath);
        assertEquals("file has node type 'nt:file'", "nt:file", node.getPrimaryNodeType().getName());
        
        boolean mixLanguageFound = false;
        for (NodeType mixin : node.getMixinNodeTypes()) {
            if ("mix:language".equals(mixin.getName())) {
                mixLanguageFound = true;
            }
        }
        assertTrue("file has mixin 'mix:language'", mixLanguageFound); 
        assertEquals("file has property 'en'", "en", node.getProperty("jcr:language").getString());

        final String descriptorPath = contentRootPath + "/i18n/en.json.xml"; 
        assertFalse("descriptor " + descriptorPath + " does not exists", session.itemExists(descriptorPath)); 
    }

}

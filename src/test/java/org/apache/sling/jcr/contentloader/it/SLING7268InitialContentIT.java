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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlList;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.tinybundles.core.TinyBundle;
import org.osgi.framework.Bundle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * test of a bundle that provides initial content that creates a user/group and
 * defines an ace for those principals within the same transaction
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class SLING7268InitialContentIT extends ContentloaderTestSupport {

    protected TinyBundle setupTestBundle(TinyBundle b) throws IOException {
        b.set(SLING_INITIAL_CONTENT_HEADER, DEFAULT_PATH_IN_BUNDLE + ";path:=" + contentRootPath);
        addContent(b, DEFAULT_PATH_IN_BUNDLE, "SLING-7268.json");
        return b;
    }

    @Test
    public void bundleStarted() {
        final Bundle b = findBundle(bundleSymbolicName);
        assertNotNull("Expecting bundle to be found:" + bundleSymbolicName, b);
        assertEquals("Expecting bundle to be active:" + bundleSymbolicName, Bundle.ACTIVE, b.getState());
    }

    @Test
    public void initialContentInstalled() throws RepositoryException {
        final String folderPath = contentRootPath + "/SLING-7268";
        assertTrue("Expecting initial content to be installed", session.itemExists(folderPath));
        assertEquals("folder has node type 'sling:Folder'", "sling:Folder",
                session.getNode(folderPath).getPrimaryNodeType().getName());
    }

    @Test
    public void userCreated() throws RepositoryException {
        UserManager userManager = AccessControlUtil.getUserManager(session);
        Authorizable authorizable = userManager.getAuthorizable("sling7268_user");
        assertNotNull("Expecting test user to exist", authorizable);
    }

    @Test
    public void groupCreated() throws RepositoryException {
        UserManager userManager = AccessControlUtil.getUserManager(session);
        Authorizable authorizable = userManager.getAuthorizable("sling7268_group");
        assertNotNull("Expecting test group to exist", authorizable);
        assertTrue(authorizable instanceof Group);
        Iterator<Authorizable> members = ((Group) authorizable).getMembers();
        assertTrue(members.hasNext());
        Authorizable firstMember = members.next();
        assertEquals("sling7268_user", firstMember.getID());
    }


    @Test
    public void aceCreated() throws RepositoryException {
        final String folderPath = contentRootPath + "/SLING-7268";
        assertTrue("Expecting test folder to exist", session.itemExists(folderPath));
        AccessControlManager accessControlManager = AccessControlUtil.getAccessControlManager(session);
        AccessControlPolicy[] policies = accessControlManager.getPolicies(folderPath);
        List<AccessControlEntry> allEntries = new ArrayList<AccessControlEntry>();
        for (AccessControlPolicy accessControlPolicy : policies) {
            if (accessControlPolicy instanceof AccessControlList) {
                AccessControlEntry[] accessControlEntries = ((AccessControlList) accessControlPolicy)
                        .getAccessControlEntries();
                for (AccessControlEntry accessControlEntry : accessControlEntries) {
                    allEntries.add(accessControlEntry);
                }
            }
        }
        assertEquals(3, allEntries.size());
        Map<String, AccessControlEntry> aceMap = new HashMap<>();
        for (AccessControlEntry accessControlEntry : allEntries) {
            aceMap.put(accessControlEntry.getPrincipal().getName(), accessControlEntry);
        }

        // check ACE for sling7268_user
        AccessControlEntry testUserAce = aceMap.get("sling7268_user");
        assertNotNull("Expected ACE for test user", testUserAce);
        assertEquals("sling7268_user", testUserAce.getPrincipal().getName());
        Privilege[] privileges = testUserAce.getPrivileges();
        assertNotNull(privileges);
        assertEquals(2, privileges.length);
        Set<String> privilegeNames = new HashSet<>();
        for (Privilege privilege : privileges) {
            privilegeNames.add(privilege.getName());
        }
        assertTrue("Expecting granted read privilege", privilegeNames.contains("jcr:read"));
        assertTrue("Expecting granted write privilege", privilegeNames.contains("jcr:write"));

        // check ACE for sling7268_group
        AccessControlEntry testGroupAce = aceMap.get("sling7268_group");
        assertNotNull("Expected ACE for test user", testGroupAce);
        assertEquals("sling7268_group", testGroupAce.getPrincipal().getName());
        privileges = testGroupAce.getPrivileges();
        assertNotNull(privileges);
        assertEquals(1, privileges.length);
        privilegeNames = new HashSet<>();
        for (Privilege privilege : privileges) {
            privilegeNames.add(privilege.getName());
        }
        assertTrue("Expecting granted modifyAccessControl privilege",
                privilegeNames.contains("jcr:modifyAccessControl"));

        // check ACE for everyone group
        AccessControlEntry everyoneAce = aceMap.get("everyone");
        assertNotNull("Expected ACE for everyone", everyoneAce);
        assertEquals("everyone", everyoneAce.getPrincipal().getName());
        privileges = everyoneAce.getPrivileges();
        assertNotNull(privileges);
        assertEquals(1, privileges.length);

        assertEquals("Expecting granted read privilege", "jcr:read", privileges[0].getName());
    }
}

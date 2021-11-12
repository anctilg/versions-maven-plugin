package org.codehaus.mojo.versions;

import java.util.Map;
import java.util.regex.*;

import javax.xml.stream.XMLStreamException;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
import org.apache.maven.plugin.*;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.mojo.versions.api.*;
import org.codehaus.mojo.versions.rewriting.ModifiedPomXMLEventReader;

/**
 * Sets properties to the latest versions of specific artifacts.
 *
 * @author Stephen Connolly
 * @author Guillaume Anctil
 * @since 2.9
 */
@Mojo(name = "use-releases-properties", requiresProject = true, requiresDirectInvocation = true, threadSafe = true)
public class UseReleasesPropertiesMojo extends AbstractVersionsDependencyUpdaterMojo
{
    // ------------------------------ FIELDS ------------------------------

    /**
     * Pattern to match a snapshot version.
     */
    public final Pattern matchSnapshotRegex = Pattern.compile("^(.+)-((SNAPSHOT)|(\\d{8}\\.\\d{6}-\\d+))$");

    /**
     * Any restrictions that apply to specific properties.
     *
     * @since 2.9
     */
    @Parameter
    private Property[] properties;

    /**
     * A comma separated list of properties to update.
     *
     * @since 2.9
     */
    @Parameter(property = "includeProperties")
    private String includeProperties = null;

    /**
     * A comma separated list of properties to not update.
     *
     * @since 2.9
     */
    @Parameter(property = "excludeProperties")
    private String excludeProperties = null;

    /**
     * Whether properties linking versions should be auto-detected or not.
     *
     * @since 2.9
     */
    @Parameter(property = "autoLinkItems", defaultValue = "true")
    private boolean autoLinkItems;

    // -------------------------- STATIC METHODS --------------------------

    // -------------------------- OTHER METHODS --------------------------

    /**
     * @param pom the pom to update.
     * @throws MojoExecutionException when things go wrong
     * @throws MojoFailureException when things go wrong in a very bad way
     * @throws XMLStreamException when things go wrong with XML streaming
     * @see AbstractVersionsUpdaterMojo#update(ModifiedPomXMLEventReader)
     * @since 2.9
     */
    @Override
    protected void update(ModifiedPomXMLEventReader pom) throws MojoExecutionException, MojoFailureException, XMLStreamException
    {
        Map<Property, PropertyVersions> propertyVersions = this.getHelper().getVersionPropertiesMap(getProject(), properties, includeProperties, excludeProperties, autoLinkItems);
        for (Map.Entry<Property, PropertyVersions> entry : propertyVersions.entrySet())
        {
            Property property = entry.getKey();
            PropertyVersions version = entry.getValue();

            final String currentVersion = getProject().getProperties().getProperty(property.getName());
            if (currentVersion == null)
            {
                continue;
            }
            boolean canUpdateProperty = true;
            for (ArtifactAssociation association : version.getAssociations())
            {
                if (!(isIncluded(association.getArtifact())))
                {
                    getLog().info("Not updating the property ${" + property.getName() + "} because it is used by artifact " + association.getArtifact().toString()
                            + " and that artifact is not included in the list of " + " allowed artifacts to be updated.");
                    canUpdateProperty = false;
                    break;
                }
            }

            if (canUpdateProperty)
            {
                Matcher versionMatcher = matchSnapshotRegex.matcher(currentVersion);
                if (versionMatcher.matches())
                {
                    updatePropertyToNewestVersion(pom, property, version, currentVersion, false, 3);
                }
            }
        }
    }
}

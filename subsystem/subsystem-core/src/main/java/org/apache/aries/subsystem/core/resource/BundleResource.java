/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.aries.subsystem.core.resource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.apache.aries.subsystem.core.archive.ImportPackageHeader;
import org.apache.aries.subsystem.core.internal.OsgiContentCapability;
import org.apache.aries.subsystem.core.internal.OsgiIdentityCapability;
import org.osgi.framework.Constants;
import org.osgi.framework.wiring.Capability;
import org.osgi.framework.wiring.Requirement;
import org.osgi.framework.wiring.Resource;

public class BundleResource implements Resource {
	public static BundleResource newInstance(URL content) throws IOException {
		BundleResource result = new BundleResource(content);
		result.capabilities.add(new OsgiIdentityCapability(result, result.manifest));
		result.capabilities.add(new OsgiContentCapability(result, content));
		return result;
	}
	
	private final List<Capability> capabilities = new ArrayList<Capability>();
	private final Manifest manifest;
	
	private BundleResource(InputStream content) throws IOException {
		JarInputStream jis = new JarInputStream(content);
		try {
			Manifest manifest = jis.getManifest();
			if (manifest == null)
				throw new IllegalArgumentException("The jar file contained no manifest");
			this.manifest = manifest;
		}
		finally {
			try {
				jis.close();
			}
			catch (IOException e) {}
		}
	}
	
	private BundleResource(URL content) throws IOException {
		this(content.openStream());
	}
	
	private BundleResource(String content) throws IOException {
		/*
		 * TODO
		 * Capabilities
		 * 		Export-Package
		 * 		Provide-Capability
		 * 		BSN + Version (host)
		 * 		osgi.identity
		 * Requirements
		 * 		Import-Package
		 * 		Require-Bundle
		 * 		Require-Capability
		 * 		Fragment-Host
		 */
		this(new URL(content));
	}

	public List<Capability> getCapabilities(String namespace) {
		if (namespace == null) {
			return Collections.unmodifiableList(capabilities);
		}
		ArrayList<Capability> result = new ArrayList<Capability>(capabilities.size());
		for (Capability capability : capabilities) {
			if (namespace.equals(capability.getNamespace())) {
				result.add(capability);
			}
		}
		result.trimToSize();
		return result;
	}

	public List<Requirement> getRequirements(String namespace) {
		/* Requirements
		 * 		Import-Package
		 * 		Require-Bundle
		 * 		Require-Capability
		 * 		Fragment-Host
		 */
		ArrayList<Requirement> result = new ArrayList<Requirement>();
		String importPackageHeaderStr = manifest.getMainAttributes().getValue(Constants.IMPORT_PACKAGE);
		if (importPackageHeaderStr != null) {
			ImportPackageHeader header = new ImportPackageHeader(importPackageHeaderStr);
			result.addAll(header.getRequirements(this));
		}
		return result;
	}
}
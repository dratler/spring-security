/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.security.ldap.server;

import com.unboundid.ldap.sdk.LDAPException;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import org.mockito.internal.matchers.Null;
import org.springframework.context.support.GenericApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Eddú Meléndez
 * @author Shay Dratler
 */
public class UnboundIdContainerTests {

	private final String validLdifClassPath = "classpath:test-server.ldif";
	private final String validRootDn = "dc=springframework,dc=org";

	@Test
	public void startLdapServer() throws Exception {
		UnboundIdContainer server = new UnboundIdContainer(
				validRootDn, validLdifClassPath);
		server.setApplicationContext(new GenericApplicationContext());
		List<Integer> ports = getDefaultPorts(1);
		server.setPort(ports.get(0));

		try {
			server.afterPropertiesSet();
			assertThat(server.getPort()).isEqualTo(ports.get(0));
		} finally {
			server.destroy();
		}
	}


	@Test
	public void afterPropertiesSetWhenPortIsZeroThenRandomPortIsSelected() throws Exception {
		UnboundIdContainer server = new UnboundIdContainer(validRootDn, validLdifClassPath);
		server.setApplicationContext(new GenericApplicationContext());
		server.setPort(0);
		try {
			server.afterPropertiesSet();
			assertThat(server.getPort()).isNotEqualTo(0);
		} finally {
			server.destroy();
		}
	}

	private List<Integer> getDefaultPorts(int count) throws IOException {
		List<ServerSocket> connections = new ArrayList<>();
		List<Integer> availablePorts = new ArrayList<>(count);
		try {
			for (int i = 0; i < count; i++) {
				ServerSocket socket = new ServerSocket(0);
				connections.add(socket);
				availablePorts.add(socket.getLocalPort());
			}
			return availablePorts;
		} finally {
			for (ServerSocket conn : connections) {
				conn.close();
			}
		}
	}

	@Test
	public void missingContextInInit() throws Exception {
		UnboundIdContainer server = new UnboundIdContainer(validRootDn, validLdifClassPath);
		server.setPort(0);
		try {
			server.afterPropertiesSet();
			assertThat(server.getPort()).isNotEqualTo(0);
		} catch (Exception e) {
			assertThat(e.getCause()).isInstanceOf(NullPointerException.class);
			assertThat(e.getMessage()).contains("Unable to load LDIF test-server-malformed.txt");
		} finally {
			server.destroy();
		}
	}

	@Test
	public void testInvalidLdapFile() {
		UnboundIdContainer server = new UnboundIdContainer(validRootDn, "missing-file.ldif");
		server.setApplicationContext(new GenericApplicationContext());
		server.setPort(0);
		try {
			server.afterPropertiesSet();
		} catch (Exception e) {
			assertThat(e.getCause()).isInstanceOf(IllegalStateException.class);
			assertThat(e.getMessage()).contains("Unable to load LDIF test-server-malformed.txt");
		} finally {
			server.destroy();
		}
	}

	@Test
	public void testInvalidNullLdapFile() {
		UnboundIdContainer server = new UnboundIdContainer(validRootDn, null);
		BuildInvalidServer(server);
	}

	@Test
	public void testInvalidEmptyPathLdapFile() {
		UnboundIdContainer server = new UnboundIdContainer(validRootDn, "");
		BuildInvalidServer(server);
	}

	@Test
	public void testInvalidDirectoryAsLdapFile() {
		UnboundIdContainer server = new UnboundIdContainer(validRootDn, "classpath:/");
		server.setApplicationContext(new GenericApplicationContext());
		server.setPort(0);
		try {
			server.afterPropertiesSet();
		} catch (Exception e) {
			assertThat(e.getCause()).isInstanceOf(IllegalArgumentException.class);
			assertThat(e.getMessage()).contains("Unable to load LDIF /");
		} finally {
			server.destroy();
		}
	}

	@Test
	public void testInvalidFileTextFileAsLdapFile() {
		UnboundIdContainer server = new UnboundIdContainer(validRootDn, "classpath:test-server-malformed.txt");
		BuildInvalidServer(server);
	}

	@Test
	public void testInvalidRootDnLdapFile() {
		UnboundIdContainer server = new UnboundIdContainer("dc=fake,dc=org", validLdifClassPath);
		server.setApplicationContext(new GenericApplicationContext());
		server.setPort(0);
		try {
			server.afterPropertiesSet();

		} catch (Exception e) {
			assertThat(e.getCause()).isInstanceOf(LDAPException.class);
			assertThat(e.getMessage()).contains("Unable to add entry 'ou=groups,dc=springframework,dc=org' because it is not within any of the base DNs configured in the server.");
		} finally {
			server.destroy();
		}
	}

	private void BuildInvalidServer(UnboundIdContainer server) {
		server.setApplicationContext(new GenericApplicationContext());
		server.setPort(0);
		try {
			server.afterPropertiesSet();

		} catch (Exception e) {
			assertThat(e.getCause()).isInstanceOf(IllegalArgumentException.class);
			assertThat(e.getMessage()).contains("Unable to load LDIF test-server-malformed.txt");
		} finally {
			server.destroy();
		}
	}

}

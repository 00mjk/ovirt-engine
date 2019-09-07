/*
Copyright (c) 2017 Red Hat, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package org.ovirt.engine.core.common.utils.ansible;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.ovirt.engine.core.common.utils.Pair;
import org.ovirt.engine.core.utils.MockEngineLocalConfigExtension;

@ExtendWith(MockEngineLocalConfigExtension.class)
public class AnsibleCommandBuilderTest {

    private static final String OVIRT_HOME = "/var/lib/ovirt-engine/";
    private static final String PRIVATE_KEY = "--private-key=/etc/pki/ovirt-engine/keys/engine_id_rsa";
    private static final String ANSIBLE_PLAYBOOK = "myplaybook.yml";
    private static final String ANSIBLE_PLAYBOOK_FULL_PATH = "/usr/share/ovirt-engine/playbooks/myplaybook.yml";
    private static final String IGNORE_SSH_CONFIG = "--ssh-common-args=-F " + OVIRT_HOME + ".ssh/config";
    private static final String ANSIBLE_LOG_LEVEL = "-v";

    public static Stream<Pair<String, String>> mockEngineLocalConfiguration() {
        return Stream.of(
                new Pair<>("ENGINE_PKI", "/etc/pki/ovirt-engine/"),
                new Pair<>("ENGINE_USR", "/usr/share/ovirt-engine/"),
                new Pair<>("ENGINE_VAR", OVIRT_HOME),
                new Pair<>("ENGINE_LOG", "/var/log/ovirt-engine/")
        );
    }

    @Test
    public void testAllEmpty() {
        String command = createCommand(new AnsibleCommandBuilder().playbook(ANSIBLE_PLAYBOOK));
        assertEquals(
            join(
                AnsibleCommandBuilder.ANSIBLE_COMMAND,
                IGNORE_SSH_CONFIG,
                ANSIBLE_LOG_LEVEL,
                PRIVATE_KEY,
                ANSIBLE_PLAYBOOK_FULL_PATH
            ),
            command
        );
    }

    @Test
    public void testDisableVerboseMode() {
        String command = createCommand(
            new AnsibleCommandBuilder()
                .verboseLevel(AnsibleVerbosity.LEVEL0)
                .playbook(ANSIBLE_PLAYBOOK)
        );

        assertEquals(
            join(AnsibleCommandBuilder.ANSIBLE_COMMAND, IGNORE_SSH_CONFIG, PRIVATE_KEY, ANSIBLE_PLAYBOOK_FULL_PATH),
            command
        );
    }

    @Test
    public void testInventoryFile() {
        Path inventoryFile = Paths.get("myfile");
        String command = createCommand(
            new AnsibleCommandBuilder()
                .inventoryFile(inventoryFile)
                .playbook(ANSIBLE_PLAYBOOK)
        );
        assertEquals(
            join(
                AnsibleCommandBuilder.ANSIBLE_COMMAND,
                IGNORE_SSH_CONFIG,
                ANSIBLE_LOG_LEVEL,
                PRIVATE_KEY,
                "--inventory=" + inventoryFile,
                ANSIBLE_PLAYBOOK_FULL_PATH
            ),
            command
        );
    }

    @Test
    public void testDifferentVerbosity() {
        String command = createCommand(
            new AnsibleCommandBuilder()
                .verboseLevel(AnsibleVerbosity.LEVEL2)
                .playbook(ANSIBLE_PLAYBOOK)
        );
        assertEquals(
            join(
                AnsibleCommandBuilder.ANSIBLE_COMMAND,
                IGNORE_SSH_CONFIG,
                "-vv",
                PRIVATE_KEY,
                ANSIBLE_PLAYBOOK_FULL_PATH
            ),
            command
        );
    }

    @Test
    public void testVerbosityLevelZero() {
        String command = createCommand(
            new AnsibleCommandBuilder()
                .verboseLevel(AnsibleVerbosity.LEVEL0)
                .playbook(ANSIBLE_PLAYBOOK)
        );
        assertEquals(
            join(AnsibleCommandBuilder.ANSIBLE_COMMAND, IGNORE_SSH_CONFIG, PRIVATE_KEY, ANSIBLE_PLAYBOOK_FULL_PATH),
            command
        );
    }

    @Test
    public void testExtraVariables() {
        String command = createCommand(
                new AnsibleCommandBuilder()
                        .variable("a", "1")
                        .variable("b", "2")
                        .variable("c", "3")
                        .playbook(ANSIBLE_PLAYBOOK));
        assertEquals(
            join(
                AnsibleCommandBuilder.ANSIBLE_COMMAND,
                IGNORE_SSH_CONFIG,
                ANSIBLE_LOG_LEVEL,
                PRIVATE_KEY,
                "--extra-vars=a=\"1\"",
                "--extra-vars=b=\"2\"",
                "--extra-vars=c=\"3\"",
                ANSIBLE_PLAYBOOK_FULL_PATH
            ),
            command
        );
    }

    @Test
    public void testComplexCommand() {
        String command = createCommand(
            new AnsibleCommandBuilder()
                .privateKey(Paths.get("/mykey"))
                .inventoryFile(Paths.get("/myinventory"))
                .limit("mylimit")
                .verboseLevel(AnsibleVerbosity.LEVEL3)
                        .variable("a", "1")
                        .variable("b", "2")
                .playbook(ANSIBLE_PLAYBOOK)
        );
        assertEquals(
            join(
                AnsibleCommandBuilder.ANSIBLE_COMMAND,
                IGNORE_SSH_CONFIG,
                "-vvv",
                "--private-key=/mykey",
                "--inventory=/myinventory",
                "--limit=mylimit",
                "--extra-vars=a=\"1\"",
                "--extra-vars=b=\"2\"",
                ANSIBLE_PLAYBOOK_FULL_PATH
            ),
            command
        );
    }

    @ParameterizedTest
    @CsvSource({
            "1.2.3.4, 22, 1.2.3.4:22",
            "1::33:44, 22, [1::33:44]:22",
            "www.ovirt.org, 22, www.ovirt.org:22"
    })
    public void testFormatHostPort(String host, int port, String result) {
        AnsibleCommandBuilder builder = new AnsibleCommandBuilder();
        assertEquals(result, builder.formatHostPort(host, port));
    }

    private String createCommand(AnsibleCommandBuilder command) {
        return StringUtils.join(command.build().getCommand(), " ").trim();
    }

    private String join(String ... params) {
        return StringUtils.join(params, " ");
    }
}

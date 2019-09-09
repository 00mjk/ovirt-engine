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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VdsStatic;
import org.ovirt.engine.core.common.utils.ValidationUtils;
import org.ovirt.engine.core.utils.EngineLocalConfig;

/**
 * AnsibleCommandBuilder creates a ansible-playbook command.
 *
 * By default:
 * 1) We don't use any cluster.
 * 2) We use verbose mode level 1 (-v).
 * 3) Playbook directory is $PREFIX/usr/share/ovirt-ansible-roles/playbooks
 * 4) Private key used is $PREFIX/etc/pki/ovirt-engine/keys/engine_id_rsa
 * 5) Log file is $PREFIX/var/log/ovirt-engine/ansible/{prefix}-{timestamp}-{playbook-name}[-{suffix}].log
 * 6) Default inventory file is used.
 */
public class AnsibleCommandBuilder {

    public static final String ANSIBLE_COMMAND = "/usr/bin/ansible-playbook";

    private AnsibleVerbosity verboseLevel;
    private Path privateKey;
    private String cluster;
    private List<String> hostnames;
    private Map<String, Object> variables;
    private String variableFilePath;
    private String limit;
    private Path inventoryFile;
    private String playbook;
    private boolean checkMode;

    // Logging:
    private String logFileDirectory;
    private String logFilePrefix;
    private String logFileSuffix;
    private String logFileName;
    /*
     * By default Ansible logs to syslog of the host where the playbook is being executed. If this parameter is set to
     * true the logging will be done to file which you can specify by log* methods. If this parameters is set to false,
     * the logging wil be done to syslog on hosts.
     */
    private boolean enableLogging;

    private EngineLocalConfig config;
    private Path playbookDir;

    // ENV variables
    private Map<String, String> envVars;

    private List<String> ansibleCommand;

    public AnsibleCommandBuilder() {
        cluster = "unspecified";
        enableLogging = true;
        envVars = new HashMap<>();
        config = EngineLocalConfig.getInstance();
        playbookDir = Paths.get(config.getUsrDir().getPath(), "playbooks");
        privateKey = Paths.get(config.getPKIDir().getPath(), "keys", "engine_id_rsa");
        variables = new HashMap<>();

        try {
            verboseLevel = AnsibleVerbosity.valueOf(
                    "LEVEL" + EngineLocalConfig.getInstance().getProperty("ANSIBLE_PLAYBOOK_VERBOSITY_LEVEL"));
        } catch (IllegalArgumentException | NullPointerException e) {
            verboseLevel = AnsibleVerbosity.LEVEL1;
        }
    }

    public AnsibleCommandBuilder checkMode(boolean checkMode) {
        this.checkMode = checkMode;
        return this;
    }

    public AnsibleCommandBuilder verboseLevel(AnsibleVerbosity verboseLevel) {
        this.verboseLevel = verboseLevel;
        return this;
    }

    public AnsibleCommandBuilder privateKey(Path privateKey) {
        this.privateKey = privateKey;
        return this;
    }

    public AnsibleCommandBuilder inventoryFile(Path inventoryFile) {
        this.inventoryFile = inventoryFile;
        return this;
    }

    public AnsibleCommandBuilder cluster(String cluster) {
        this.cluster = cluster;
        return this;
    }

    public AnsibleCommandBuilder hosts(VdsStatic... hosts) {
        this.hostnames = Arrays.stream(hosts)
                .map(h -> formatHostPort(h.getHostName(), h.getSshPort()))
                .collect(Collectors.toList());
        return this;
    }

    public AnsibleCommandBuilder hosts(VDS... hosts) {
        this.hostnames = Arrays.stream(hosts)
                .map(h -> formatHostPort(h.getHostName(), h.getSshPort()))
                .collect(Collectors.toList());
        return this;
    }

    protected String formatHostPort(String host, int port) {
        return ValidationUtils.isValidIpv6(host)
                ? String.format("[%1$s]:%2$s", host, port)
                : String.format("%1$s:%2$s", host, port);
    }

    public AnsibleCommandBuilder variable(String name, Object value) {
        this.variables.put(name, value);
        return this;
    }

    public AnsibleCommandBuilder limit(String limit) {
        this.limit = limit;
        return this;
    }

    public AnsibleCommandBuilder logFileDirectory(String logFileDirectory) {
        this.logFileDirectory = logFileDirectory;
        return this;
    }

    public AnsibleCommandBuilder logFileName(String logFileName) {
        this.logFileName = logFileName;
        return this;
    }

    public AnsibleCommandBuilder logFilePrefix(String logFilePrefix) {
        this.logFilePrefix = logFilePrefix;
        return this;
    }

    public AnsibleCommandBuilder logFileSuffix(String logFileSuffix) {
        this.logFileSuffix = logFileSuffix;
        return this;
    }

    public AnsibleCommandBuilder playbook(String playbook) {
        this.playbook = Paths.get(playbookDir.toString(), playbook).toString();
        return this;
    }

    public AnsibleCommandBuilder variableFilePath(String variableFilePath) {
        this.variableFilePath = variableFilePath;
        return this;
    }

    public AnsibleCommandBuilder stdoutCallback(String stdoutCallback) {
        this.envVars.put(AnsibleEnvironmentConstants.ANSIBLE_STDOUT_CALLBACK, stdoutCallback);
        return this;
    }

    public String playbook() {
        return playbook;
    }

    public AnsibleCommandBuilder enableLogging(boolean enableLogging) {
        this.enableLogging = enableLogging;
        return this;
    }

    public Path inventoryFile() {
        return inventoryFile;
    }

    public Path playbookDir() {
        return playbookDir;
    }

    public List<String> hostnames() {
        return hostnames;
    }

    public String cluster() {
        return cluster;
    }

    public Path privateKey() {
        return privateKey;
    }

    public String logFileDirectory() {
        return logFileDirectory;
    }

    public String logFileName() {
        return logFileName;
    }

    public String logFilePrefix() {
        return logFilePrefix;
    }

    public String logFileSuffix() {
        return logFileSuffix;
    }

    public String stdoutCallback() {
        return envVars.get(AnsibleEnvironmentConstants.ANSIBLE_STDOUT_CALLBACK);
    }

    public boolean enableLogging() {
        return enableLogging;
    }

    /**
     * The generated command will look like:
     *
     * /usr/bin/ansible-playbook -${verboseLevel} --private-key=${privateKey} --limit=${limit} \
     * --extra-vars=${variables} ${playbook}
     *
     * The logFile is set up to:
     *
     * /var/log/ovirt-engine/${logDirectory:ansible}/
     * ${logFilePrefix:ansible}-${timestamp}-${logFileName:playbook}[-${logFileSuffix}].log
     */
    public List<String> build() {
        ansibleCommand = new ArrayList<>();
        ansibleCommand.add(ANSIBLE_COMMAND);

        // Always ignore system wide SSH configuration:
        ansibleCommand.add(String.format("--ssh-common-args=-F %1$s/.ssh/config", config.getVarDir()));

        if (verboseLevel.ordinal() > 0) {
            ansibleCommand.add(
                    "-" + IntStream.range(0, verboseLevel.ordinal()).mapToObj(i -> "v").collect(Collectors.joining()));
        }

        if (checkMode) {
            ansibleCommand.add("--check");
        }

        if (privateKey != null) {
            ansibleCommand.add(String.format("--private-key=%1$s", privateKey));
        }

        if (inventoryFile != null) {
            ansibleCommand.add(String.format("--inventory=%1$s", inventoryFile));
        }

        if (limit != null) {
            ansibleCommand.add(String.format("--limit=%1$s", limit));
        }

        variables.entrySet()
                .stream()
                .map(e -> String.format("--extra-vars=%1$s=\"%2$s\"", e.getKey(), e.getValue()))
                .forEach(ansibleCommand::add);

        if (variableFilePath != null) {
            ansibleCommand.add(String.format("--extra-vars=@%s", variableFilePath));
        }

        ansibleCommand.add(playbook);

        return ansibleCommand;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        // Env vars:
        sb.append(
                envVars.entrySet()
                        .stream()
                        .map(entry -> entry.getKey() + "=" + entry.getValue())
                        .collect(Collectors.joining(" ")));
        // Command:
        if (ansibleCommand != null) {
            sb.append(" ");
            sb.append(StringUtils.join(ansibleCommand, " "));
            sb.append(" ");
        }

        return sb.toString();
    }
}

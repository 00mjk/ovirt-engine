# ====================================================================
#
#  Licensed to the Apache Software Foundation (ASF) under one or more
#  contributor license agreements.  See the NOTICE file distributed with
#  this work for additional information regarding copyright ownership.
#  The ASF licenses this file to You under the Apache License, Version 2.0
#  (the "License"); you may not use this file except in compliance with
#  the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
# ====================================================================
#
# This software consists of voluntary contributions made by many
# individuals on behalf of the Apache Software Foundation.  For more
# information on the Apache Software Foundation, please see
# <http://www.apache.org/>.

MVN=mvn
EXTRA_BUILD_FLAGS=
BUILD_FLAGS=-P gwt-admin,gwt-user
PACKAGE_NAME=ovirt-engine
ENGINE_NAME=$(PACKAGE_NAME)
PREFIX=/usr/local
LOCALSTATE_DIR=$(PREFIX)/var
ENGINE_STATE=$(LOCALSTATE_DIR)/lib/$(ENGINE_NAME)
BIN_DIR=$(PREFIX)/bin
SYSCONF_DIR=$(PREFIX)/etc
DATAROOT_DIR=$(PREFIX)/share
DATA_DIR=$(DATAROOT_DIR)/$(ENGINE_NAME)
MAVENPOM_DIR=$(DATAROOT_DIR)/maven-poms
JAVA_DIR=$(DATAROOT_DIR)/java
PKG_JAVA_DIR=$(JAVA_DIR)/$(ENGINE_NAME)
PKG_SYSCONF_DIR=$(SYSCONF_DIR)/$(ENGINE_NAME)
PKG_PKI_DIR=$(SYSCONF_DIR)/pki/$(ENGINE_NAME)
PKG_EAR_DIR=$(DATA_DIR)/engine.ear
PKG_JBOSS_MODULES=$(DATA_DIR)/modules
RPMBUILD=rpmbuild
PYTHON=python
PYTHON_DIR:=$(shell $(PYTHON) -c "from distutils.sysconfig import get_python_lib as f;print f()")

# RPM version
APP_VERSION:=$(shell cat pom.xml | grep '<engine.version>' | awk -F\> '{print $$2}' | awk -F\< '{print $$1}')
RPM_VERSION:=$(shell echo $(APP_VERSION) | sed "s/-/_/")

# Release Version; used to create y in <x.x.x-y> numbering.
# Should be used to create releases.
RPM_RELEASE_VERSION=3

SPEC_FILE_IN=packaging/fedora/spec/ovirt-engine.spec.in
SPEC_FILE=$(PACKAGE_NAME).spec
OUTPUT_RPMBUILD=$(shell pwd -P)/tmp.rpmbuild
OUTPUT_DIR=output
TARBALL=$(PACKAGE_NAME)-$(RPM_VERSION).tar.gz
SRPM=$(OUTPUT_DIR)/$(PACKAGE_NAME)-$(RPM_VERSION)*.src.rpm
ARCH=noarch
BUILD_FILE=tmp.built
MAVEN_OUTPUT_DIR_DEFAULT=$(shell pwd -P)/tmp.repos
MAVEN_OUTPUT_DIR=$(MAVEN_OUTPUT_DIR_DEFAULT)

ARTIFACTS = \
	backend bll \
	common \
	compat \
	dal \
	engine-config \
	engine-notifier \
	engine-notifier-resources \
	engine-notifier-service \
	engine-tools-common \
	engineencryptutils \
	genericapi \
	interface-common-jaxrs \
	manager \
	manager-modules \
	manager-tools \
	restapi-definition \
	restapi-jaxrs \
	restapi-parent \
	restapi-types \
	root \
	scheduler \
	searchbackend \
	utils \
	vdsbroker

# avoid duplicate jars, remove 1st component
# link to 2nd component
# no convention yet...
OWN_JAR_FIXUPS = \
	$(PKG_EAR_DIR)/engine-bll,bll \
	$(PKG_EAR_DIR)/engine-genericapi,genericapi \
	$(PKG_EAR_DIR)/engine-scheduler,scheduler \
	$(PKG_EAR_DIR)/lib/engine-common,common \
	$(PKG_EAR_DIR)/lib/engine-compat,compat \
	$(PKG_EAR_DIR)/lib/engine-dal,dal \
	$(PKG_EAR_DIR)/lib/engine-encryptutils,engineencryptutils \
	$(PKG_EAR_DIR)/lib/engine-tools-common,engine-tools-common \
	$(PKG_EAR_DIR)/lib/engine-utils,utils \
	$(PKG_EAR_DIR)/lib/engine-vdsbroker,vdsbroker \
	$(PKG_EAR_DIR)/lib/searchbackend,searchbackend

all: $(BUILD_FILE)

$(BUILD_FILE):
	export MAVEN_OPTS="${MAVEN_OPTS} -XX:MaxPermSize=512m"
	$(MVN) \
		$(BUILD_FLAGS) \
		$(EXTRA_BUILD_FLAGS) \
		dependency:resolve-plugins
	$(MVN) \
		$(BUILD_FLAGS) \
		$(EXTRA_BUILD_FLAGS) \
		-D skipTests \
		-D altDeploymentRepository=install::default::file://$(MAVEN_OUTPUT_DIR) \
		deploy
	touch $(BUILD_FILE)

clean:
	$(MVN) clean $(EXTRA_BUILD_FLAGS)
	rm -rf $(OUTPUT_RPMBUILD) $(SPEC_FILE) $(OUTPUT_DIR) $(BUILD_FILE)
	[ "$(MAVEN_OUTPUT_DIR_DEFAULT)" = "$(MAVEN_OUTPUT_DIR)" ] && rm -fr "$(MAVEN_OUTPUT_DIR)"

test:
	$(MVN) install $(BUILD_FLAGS) $(EXTRA_BUILD_FLAGS)

install: \
	all \
	install_without_maven

install_without_maven: \
	create_dirs \
	install_artifacts \
	install_config \
	install_sysprep \
	install_notification_service \
	install_db_scripts \
	install_setup \
	install_misc \
	install_sec \
	install_aio_plugin \
	install_jboss_modules \
	install_service

tarball:
	sed -e 's/@PACKAGE_VERSION@/$(RPM_VERSION)/g' \
            -e 's/@PACKAGE_RELEASE@/$(RPM_RELEASE_VERSION)/g' $(SPEC_FILE_IN) > $(SPEC_FILE)
	git ls-files | tar --files-from /proc/self/fd/0 -czf $(TARBALL) $(SPEC_FILE)
	rm -f $(SPEC_FILE)
	@echo
	@echo You can use $(RPMBUILD) -tb $(TARBALL) to produce rpms
	@echo

srpm:	tarball
	rm -rf $(OUTPUT_RPMBUILD)
	mkdir -p $(OUTPUT_RPMBUILD)/{SPECS,RPMS,SRPMS,SOURCES,BUILD,BUILDROOT}
	mkdir -p $(OUTPUT_DIR)
	$(RPMBUILD) -ts --define="_topdir $(OUTPUT_RPMBUILD)" $(TARBALL)
	mv $(OUTPUT_RPMBUILD)/SRPMS/*.rpm $(OUTPUT_DIR)
	rm -rf $(OUTPUT_RPMBUILD)
	@echo
	@echo srpm is ready at $(OUTPUT_DIR)
	@echo

rpm:	srpm
	rm -rf $(OUTPUT_RPMBUILD)
	mkdir -p $(OUTPUT_RPMBUILD)/{SPECS,RPMS,SRPMS,SOURCES,BUILD,BUILDROOT}
	mkdir -p $(OUTPUT_DIR)
	$(RPMBUILD) --define="_topdir $(OUTPUT_RPMBUILD)" $(RPMBUILD_EXTRA_ARGS) --rebuild $(SRPM)
	mv $(OUTPUT_RPMBUILD)/RPMS/$(ARCH)/*.rpm $(OUTPUT_DIR)
	rm -rf $(OUTPUT_RPMBUILD)
	@echo
	@echo rpms are ready at $(OUTPUT_DIR)
	@echo

# This is intended to quickly build a set of RPMs that don't
# contain working copies of the GWT applications, mostly useful
# for testing the RPM build process itself or for testing only the
# backend and the RESTAPI
rpm-quick:
	$(MAKE) \
		rpm \
		RPMBUILD_EXTRA_ARGS='--define="__jar_repack 0" \
			--define="BUILD_FLAGS -D dummy" \
			--define="EXTRA_BUILD_FLAGS -D maven.test.skip=true -D gwt.userAgent=gecko1_8"'
	@echo
	@echo WARNING:
	@echo rpms produces from quick are partial!
	@echo *DO NOT* use them for any other use but debug.
	@echo

create_dirs:
	@echo "*** Creating Directories"
	@install -dm 755 $(DESTDIR)$(BIN_DIR)
	@install -dm 755 $(DESTDIR)$(DATA_DIR)/bin
	@install -dm 755 $(DESTDIR)$(DATA_DIR)/conf
	@install -dm 755 $(DESTDIR)$(DATA_DIR)/db-backups
	@install -dm 755 $(DESTDIR)$(DATA_DIR)/ovirt-isos
	@install -dm 755 $(DESTDIR)$(DATA_DIR)/scripts/plugins
	@install -dm 755 $(DESTDIR)$(PYTHON_DIR)/sos/plugins
	@install -dm 755 $(DESTDIR)$(PKG_SYSCONF_DIR)/engine-config
	@install -dm 755 $(DESTDIR)$(PKG_SYSCONF_DIR)/engine-manage-domains
	@install -dm 755 $(DESTDIR)$(SYSCONF_DIR)/sysconfig
	@install -dm 755 $(DESTDIR)$(SYSCONF_DIR)/cron.daily
	@install -dm 755 $(DESTDIR)$(SYSCONF_DIR)/security/limits.d
	@install -dm 755 $(DESTDIR)$(SYSCONF_DIR)/rc.d/init.d

	@install -dm 755 $(DESTDIR)/usr/lib/systemd/system
	@install -dm 755 $(DESTDIR)$(SYSCONF_DIR)/tmpfiles.d

install_artifacts:
	@echo "*** Deploying EAR to $(DESTDIR)"
	install -dm 755 $(DESTDIR)$(PKG_EAR_DIR)
	install -dm 755 $(DESTDIR)$(PKG_JAVA_DIR)
	install -dm 755 $(DESTDIR)$(MAVENPOM_DIR)

	X=`find "$(MAVEN_OUTPUT_DIR)" -name engine-server-ear-$(APP_VERSION).ear` && unzip "$$X" -d "$(DESTDIR)$(PKG_EAR_DIR)"

	for artifact_id in  $(ARTIFACTS); do \
		POM=`find "$(MAVEN_OUTPUT_DIR)" -name "$${artifact_id}-$(APP_VERSION).pom"`; \
		if ! [ -f "$${POM}" ]; then \
			echo "ERROR: Cannot find artifact $${artifact_id}"; \
			exit 1; \
		fi; \
		JAR=`echo "$${POM}" | sed 's/\.pom/.jar/'`; \
		install -p -m 644 "$${POM}" "$(DESTDIR)$(MAVENPOM_DIR)/$(PACKAGE_NAME)-$${artifact_id}.pom"; \
		[ -f "$${JAR}" ] && install -p -m 644 "$${JAR}" "$(DESTDIR)$(PKG_JAVA_DIR)/$${artifact_id}.jar"; \
	done

	# Replace jar files in the ear with links to their actuals
	# locations
	for jar_line in $(OWN_JAR_FIXUPS); do \
		path=`echo $${jar_line} | sed 's/,.*//'`; \
		jar=`echo $${jar_line} | sed 's/.*,//'`; \
		rm -rf "$(DESTDIR)$${path}"*.jar; \
		ln -s "$(PKG_JAVA_DIR)/$${jar}.jar" "$(DESTDIR)$${path}.jar"; \
	done

install_setup:
	@echo "*** Deploying setup executables"

	# Configuration files:
	install -m 644 packaging/fedora/setup/engine-config-install.properties $(DESTDIR)$(DATA_DIR)/conf
	install -m 644 packaging/fedora/setup/iptables.default $(DESTDIR)$(DATA_DIR)/conf
	install -m 644 packaging/fedora/setup/nfs.sysconfig $(DESTDIR)$(DATA_DIR)/conf
	install -m 644 packaging/fedora/setup/ovirt-engine-proxy.conf.in $(DESTDIR)$(DATA_DIR)/conf

	# Shared python modules:
	install -m 644 packaging/fedora/setup/nfsutils.py $(DESTDIR)$(DATA_DIR)/scripts
	install -m 644 packaging/fedora/setup/basedefs.py $(DESTDIR)$(DATA_DIR)/scripts
	install -m 644 packaging/fedora/setup/engine_validators.py $(DESTDIR)$(DATA_DIR)/scripts
	install -m 644 packaging/fedora/setup/setup_params.py $(DESTDIR)$(DATA_DIR)/scripts
	install -m 644 packaging/fedora/setup/setup_sequences.py $(DESTDIR)$(DATA_DIR)/scripts
	install -m 644 packaging/fedora/setup/setup_controller.py $(DESTDIR)$(DATA_DIR)/scripts
	install -m 644 packaging/fedora/setup/common_utils.py $(DESTDIR)$(DATA_DIR)/scripts
	install -m 644 packaging/fedora/setup/miniyum.py $(DESTDIR)$(DATA_DIR)/scripts
	install -m 644 packaging/fedora/setup/output_messages.py $(DESTDIR)$(DATA_DIR)/scripts
	install -m 644 packaging/fedora/setup/post_upgrade.py $(DESTDIR)$(DATA_DIR)/scripts

	# Example Plugin:
	install -m 644 packaging/fedora/setup/plugins/example_plugin_000.py $(DESTDIR)$(DATA_DIR)/scripts/plugins

	# Main programs and links:
	install -m 755 packaging/fedora/setup/engine-setup.py $(DESTDIR)$(DATA_DIR)/scripts
	ln -s $(DATA_DIR)/scripts/engine-setup.py $(DESTDIR)$(BIN_DIR)/engine-setup
	install -m 755 packaging/fedora/setup/engine-cleanup.py $(DESTDIR)$(DATA_DIR)/scripts
	ln -s $(DATA_DIR)/scripts/engine-cleanup.py $(DESTDIR)$(BIN_DIR)/engine-cleanup
	install -m 755 packaging/fedora/setup/engine-upgrade.py $(DESTDIR)$(DATA_DIR)/scripts
	ln -s $(DATA_DIR)/scripts/engine-upgrade.py $(DESTDIR)$(BIN_DIR)/engine-upgrade
	install -m 755 packaging/fedora/setup/engine-check-update $(DESTDIR)$(BIN_DIR)/

	# Backups folder
	install -dm 755 $(DESTDIR)$(ENGINE_STATE)/backups

	sed -i "s/MYVERSION/$(RPM_VERSION)-$(RPM_RELEASE_VERSION)/" $(DESTDIR)$(PKG_EAR_DIR)/root.war/engineVersion.js

install_aio_plugin:
	install -m 755 packaging/fedora/setup/plugins/all_in_one_100.py $(DESTDIR)$(DATA_DIR)/scripts/plugins

install_sec:
	install -dm 755 $(DESTDIR)$(PKG_PKI_DIR)/certs
	install -dm 755 $(DESTDIR)$(PKG_PKI_DIR)/keys
	install -dm 755 $(DESTDIR)$(PKG_PKI_DIR)/private
	install -dm 755 $(DESTDIR)$(PKG_PKI_DIR)/requests

	# Configuration files:
	install -m 644 backend/manager/conf/ca/openssl.conf $(DESTDIR)$(PKG_PKI_DIR)
	install -m 644 backend/manager/conf/ca/cacert.template $(DESTDIR)$(PKG_PKI_DIR)
	install -m 644 backend/manager/conf/ca/cert.template $(DESTDIR)$(PKG_PKI_DIR)

	# Certificate database:
	install -m 644 backend/manager/conf/ca/database.txt $(DESTDIR)$(PKG_PKI_DIR)
	install -m 644 backend/manager/conf/ca/serial.txt $(DESTDIR)$(PKG_PKI_DIR)

	# Scripts:
	install -m 755 backend/manager/conf/ca/*.sh $(DESTDIR)$(PKG_PKI_DIR)

install_config:
	@echo "*** Deploying engine-config & engine-manage-domains"

	# Configuration files for the configuration tool:
	install -m 644 backend/manager/tools/engine-config/src/main/resources/engine-config.conf $(DESTDIR)$(PKG_SYSCONF_DIR)/engine-config/
	install -m 644 backend/manager/tools/engine-config/src/main/resources/engine-config.*properties $(DESTDIR)$(PKG_SYSCONF_DIR)/engine-config/
	install -m 644 backend/manager/tools/engine-config/src/main/resources/log4j.xml $(DESTDIR)$(PKG_SYSCONF_DIR)/engine-config/

	# Main program for the configuration tool:
	install -m 750 backend/manager/tools/engine-config/src/main/resources/engine-config $(DESTDIR)$(DATA_DIR)/bin/engine-config.sh
	ln -s $(DATA_DIR)/bin/engine-config.sh $(DESTDIR)$(BIN_DIR)/engine-config

	# Configuration files for the domain management tool:
	install -m 644 backend/manager/modules/utils/src/main/resources/engine-manage-domains.conf $(DESTDIR)$(PKG_SYSCONF_DIR)/engine-manage-domains/
	install -m 644 backend/manager/modules/utils/src/main/resources/engine-manage-domains/log4j.xml $(DESTDIR)$(PKG_SYSCONF_DIR)/engine-manage-domains/

	# Main program for the domain management tool:
	install -m 750 backend/manager/conf/kerberos/engine-manage-domains $(DESTDIR)$(DATA_DIR)/bin/engine-manage-domains.sh
	ln -s $(DATA_DIR)/bin/engine-manage-domains.sh $(DESTDIR)$(BIN_DIR)/engine-manage-domains

install_sysprep:
	@echo "*** Deploying sysperp"
	@install -dm 755 $(DESTDIR)$(PKG_SYSCONF_DIR)/sysprep
	install -m 644 backend/manager/conf/sysprep/* $(DESTDIR)$(PKG_SYSCONF_DIR)/sysprep

install_notification_service:
	@echo "*** Deploying notification service"

	install -dm 755 $(DESTDIR)$(PKG_SYSCONF_DIR)/notifier

	# Configuration files:
	install -m 644 backend/manager/tools/engine-notifier/engine-notifier-resources/src/main/resources/log4j.xml $(DESTDIR)$(PKG_SYSCONF_DIR)/notifier/
	install -m 640 backend/manager/tools/engine-notifier/engine-notifier-resources/src/main/resources/notifier.conf $(DESTDIR)$(PKG_SYSCONF_DIR)/notifier/

	# Main program:
	install -m 755 backend/manager/tools/engine-notifier/engine-notifier-resources/src/main/resources/notifier.sh $(DESTDIR)$(DATA_DIR)/bin/engine-notifier.sh
	install -m 755 backend/manager/tools/engine-notifier/engine-notifier-resources/src/main/resources/engine-notifierd $(DESTDIR)$(SYSCONF_DIR)/rc.d/init.d/

install_db_scripts:
	@echo "*** Deploying Database scripts"
	install -dm 755 $(DESTDIR)$(DATA_DIR)/dbscripts
	cp -r backend/manager/dbscripts/* $(DESTDIR)$(DATA_DIR)/dbscripts
	find $(DESTDIR)$(DATA_DIR)/dbscripts -type d -exec chmod 755 {} \;
	find $(DESTDIR)$(DATA_DIR)/dbscripts -type f -name '*.sql' -exec chmod 644 {} \;
	find $(DESTDIR)$(DATA_DIR)/dbscripts -type f -name '*.sh' -exec chmod 755 {} \;

install_misc:
	@echo "*** Copying additional files"

	# Shell scripts used by several programs:
	install -m 755 backend/manager/tools/engine-tools-common/src/main/shell/engine-prolog.sh $(DESTDIR)$(DATA_DIR)/bin

	# Other misc things:
	install -m 644 backend/manager/conf/jaas.conf $(DESTDIR)$(DATA_DIR)/conf
	install -m 640 backend/manager/conf/engine.conf $(DESTDIR)$(PKG_SYSCONF_DIR)/
	install -m 644 backend/manager/conf/engine.conf.defaults $(DESTDIR)$(DATA_DIR)/conf
	install -m 755 packaging/resources/ovirtlogrot.sh ${DESTDIR}$(DATA_DIR)/scripts/
	install -m 755 packaging/resources/ovirt-cron ${DESTDIR}$(SYSCONF_DIR)/cron.daily/
	install -m 644 packaging/resources/ovirt-tmpfilesd ${DESTDIR}$(SYSCONF_DIR)/tmpfiles.d/$(ENGINE_NAME).conf

	# USB filter:
	install -m 644 frontend/usbfilter.txt $(DESTDIR)$(PKG_SYSCONF_DIR)

install_jboss_modules:
	@echo "*** Deploying JBoss modules"

	# PostgreSQL driver:
	install -dm 755 $(DESTDIR)$(PKG_JBOSS_MODULES)/org/postgresql/main
	install -m 644 deployment/modules/org/postgresql/main/module.xml $(DESTDIR)$(PKG_JBOSS_MODULES)/org/postgresql/main/.
	ln -s $(JAVA_DIR)/postgresql-jdbc.jar $(DESTDIR)$(PKG_JBOSS_MODULES)/org/postgresql/main/.

install_service:
	@echo "*** Deploying service"

	# Install the files:
	install -dm 755 $(DESTDIR)$(DATA_DIR)/service
	install -m 644 packaging/fedora/engine-service.xml.in $(DESTDIR)$(DATA_DIR)/service
	install -m 644 packaging/fedora/engine-service-logging.properties $(DESTDIR)$(DATA_DIR)/service
	install -m 755 packaging/fedora/engine-service.py $(DESTDIR)$(DATA_DIR)/service
	install -m 644 packaging/fedora/engine-service.sysconfig $(DESTDIR)$(SYSCONF_DIR)/sysconfig/ovirt-engine
	install -m 644 packaging/fedora/engine-service.limits $(DESTDIR)$(SYSCONF_DIR)/security/limits.d/10-$(ENGINE_NAME).conf
	install -m 644 packaging/fedora/engine-service.systemd $(DESTDIR)/usr/lib/systemd/system/ovirt-engine.service

	# Install the links:
	ln -s $(DATA_DIR)/service/engine-service.py $(DESTDIR)$(BIN_DIR)/engine-service


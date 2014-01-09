#
# ovirt-engine-setup -- ovirt engine setup
# Copyright (C) 2013 Red Hat, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#


"""Upgrade configuration from legacy plugin."""


import os
import stat
import glob
import gettext
_ = lambda m: gettext.dgettext(message=m, domain='ovirt-engine-setup')


from otopi import util
from otopi import plugin
from otopi import constants as otopicons
from otopi import filetransaction


from ovirt_engine import configfile


from ovirt_engine_setup import constants as osetupcons


@util.export
class Plugin(plugin.PluginBase):
    """Upgrade configuration from legacy plugin."""

    def __init__(self, context):
        super(Plugin, self).__init__(context=context)
        self._backup = None
        self._backup_stat = None

    @plugin.event(
        stage=plugin.Stages.STAGE_CUSTOMIZATION,
        condition=lambda self: self.environment[
            osetupcons.CoreEnv.UPGRADE_FROM_LEGACY
        ],
        priority=plugin.Stages.PRIORITY_FIRST,
        name=osetupcons.Stages.UPGRADE_FROM_LEGACY_CONFIG
    )
    def _customization(self):
        legacy = osetupcons.FileLocations.LEGACY_OVIRT_ENGINE_SYSCONFIG
        config = None
        for filename in (
            legacy,
            legacy + '.rpmsave',
        ):
            if os.path.exists(filename):
                self.logger.debug('Upgrading from 3.2.z')
                self.environment[
                    osetupcons.ApacheEnv.CONFIGURE_ROOT_REDIRECTION
                ] = True
                config = configfile.ConfigFile([
                    filename,
                ])
                break
        if config is None:
            self.logger.debug('Upgrading from 3.3.z legacy')
            config = configfile.ConfigFile([
                osetupcons.FileLocations.OVIRT_ENGINE_SERVICE_CONFIG
            ])

        #preserve engine http and https ports.
        if config.getboolean('ENGINE_HTTP_ENABLED'):
            self.environment[
                osetupcons.ConfigEnv.JBOSS_DIRECT_HTTP_PORT
            ] = config.get('ENGINE_HTTP_PORT')

        if config.getboolean('ENGINE_HTTPS_ENABLED'):
            self.environment[
                osetupcons.ConfigEnv.JBOSS_DIRECT_HTTPS_PORT
            ] = config.get('ENGINE_HTTPS_PORT')

        self.environment[osetupcons.ConfigEnv.FQDN] = config.get('ENGINE_FQDN')
        if not config.getboolean('ENGINE_PROXY_ENABLED'):
            self.environment[osetupcons.ApacheEnv.CONFIGURE_SSL] = True
        else:
            #if it's enabled it has been already done
            self.environment[osetupcons.ApacheEnv.CONFIGURE_SSL] = False
            for key in (
                otopicons.CoreEnv.MODIFIED_FILES,
                osetupcons.CoreEnv.UNINSTALL_UNREMOVABLE_FILES,
            ):
                self.environment[key].append(
                    self.environment[
                        osetupcons.ApacheEnv.HTTPD_CONF_SSL
                    ]
                )
        self.environment[osetupcons.DBEnv.SECURED] = config.getboolean(
            name='ENGINE_DB_SECURED',
            default='ssl=true' in config.get(
                'ENGINE_DB_URL',
                '',
            )
        )
        self.environment[
            osetupcons.DBEnv.SECURED_HOST_VALIDATION
        ] = config.getboolean(
            name='ENGINE_DB_SECURED_VALIDATION',
            default=False,
        )

    @plugin.event(
        stage=plugin.Stages.STAGE_EARLY_MISC,
        condition=lambda self: self.environment[
            osetupcons.CoreEnv.UPGRADE_FROM_LEGACY
        ],
    )
    def _early_misc(self):
        if os.path.exists(
            osetupcons.FileLocations.LEGACY_OVIRT_ENGINE_SYSCONFIG
        ):
            # keep a copy of sysconfig content before yum transaction,
            # allowing rollback.
            self._backup_stat = os.stat(
                osetupcons.FileLocations.LEGACY_OVIRT_ENGINE_SYSCONFIG
            )
            with open(
                osetupcons.FileLocations.LEGACY_OVIRT_ENGINE_SYSCONFIG,
                'r',
            ) as f:
                self._backup = f.read()

    @plugin.event(
        stage=plugin.Stages.STAGE_MISC,
        condition=lambda self: self.environment[
            osetupcons.CoreEnv.UPGRADE_FROM_LEGACY
        ],
    )
    def _misc(self):
        legacy = osetupcons.FileLocations.LEGACY_OVIRT_ENGINE_SYSCONFIG
        legacy_rpmsave = legacy + '.rpmsave'
        legacy_confd = legacy + '.d'

        # yum update renamed it. Here we rename back, so that if a failure
        # causes a rollback to previous packages, the conf file will already
        # be in place.
        if os.path.exists(legacy_rpmsave) and not os.path.exists(legacy):
            os.rename(legacy_rpmsave, legacy)

        if os.path.exists(legacy_confd):
            for n in glob.glob(os.path.join(legacy_confd, '*.conf')):
                with open(n, 'r') as f:
                    self.environment[
                        otopicons.CoreEnv.MAIN_TRANSACTION
                    ].append(
                        filetransaction.FileTransaction(
                            name=os.path.join(
                                (
                                    osetupcons.FileLocations.
                                    OVIRT_ENGINE_SERVICE_CONFIGD
                                ),
                                os.path.basename(n),
                            ),
                            content=f.read().splitlines(),
                        )
                    )

    @plugin.event(
        stage=plugin.Stages.STAGE_CLEANUP,
        condition=lambda self: self.environment[
            osetupcons.CoreEnv.UPGRADE_FROM_LEGACY
        ],
    )
    def _cleanup(self):
        if (
            self._backup is not None and
            self.environment[otopicons.BaseEnv.ERROR]
        ):
            with open(
                osetupcons.FileLocations.LEGACY_OVIRT_ENGINE_SYSCONFIG,
                'w',
            ) as f:
                f.write(
                    self._backup,
                )
            os.chmod(
                osetupcons.FileLocations.LEGACY_OVIRT_ENGINE_SYSCONFIG,
                stat.S_IMODE(self._backup_stat.st_mode)
            )
            os.chown(
                osetupcons.FileLocations.LEGACY_OVIRT_ENGINE_SYSCONFIG,
                self._backup_stat.st_uid,
                self._backup_stat.st_gid
            )

    @plugin.event(
        stage=plugin.Stages.STAGE_CLOSEUP,
        condition=lambda self: self.environment[
            osetupcons.CoreEnv.UPGRADE_FROM_LEGACY
        ],
    )
    def _closeup(self):
        legacy = osetupcons.FileLocations.LEGACY_OVIRT_ENGINE_SYSCONFIG
        legacy_rpmsave = legacy + '.rpmsave'

        # Here we don't need it anymore, and rename back
        if os.path.exists(legacy) and not os.path.exists(legacy_rpmsave):
            os.rename(legacy, legacy_rpmsave)


# vim: expandtab tabstop=4 shiftwidth=4

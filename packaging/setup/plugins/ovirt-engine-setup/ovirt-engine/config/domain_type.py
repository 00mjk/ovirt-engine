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

"""
Storage pool type configuration plugin
"""

import gettext
_ = lambda m: gettext.dgettext(message=m, domain='ovirt-engine-setup')


from otopi import util
from otopi import plugin

from ovirt_engine_setup import constants as osetupcons
from ovirt_engine_setup.engine import constants as oenginecons
from ovirt_engine_setup.engine_common \
    import constants as oengcommcons


@util.export
class Plugin(plugin.PluginBase):
    """
    Storage pool type configuration plugin
    """
    STORAGE_TYPES = {
        'nfs': 1,
        'fc': 2,
        'iscsi': 3,
        'posixfs': 6,
        'glusterfs': 7,
    }

    def __init__(self, context):
        super(Plugin, self).__init__(context=context)
        self._enabled = False

    @plugin.event(
        stage=plugin.Stages.STAGE_INIT,
    )
    def _init(self):
        self.environment.setdefault(
            osetupcons.ConfigEnv.STORAGE_TYPE,
            None
        )

    @plugin.event(
        stage=plugin.Stages.STAGE_CUSTOMIZATION,
        before=(
            oenginecons.Stages.DIALOG_TITLES_E_ENGINE,
        ),
        after=(
            osetupcons.Stages.CONFIG_APPLICATION_MODE_AVAILABLE,
            oenginecons.Stages.DIALOG_TITLES_S_ENGINE,
        ),
        condition=lambda self: (
            self.environment[oengcommcons.EngineDBEnv.NEW_DATABASE] and
            self.environment[
                osetupcons.ConfigEnv.APPLICATION_MODE
            ] != 'gluster'
        ),
    )
    def _customization(self):
        self._enabled = True

        if self.environment[
            osetupcons.ConfigEnv.STORAGE_TYPE
        ] is None:
            self.environment[
                osetupcons.ConfigEnv.STORAGE_TYPE
            ] = self.dialog.queryString(
                name='OVESETUP_CONFIG_STORAGE_TYPE',
                note=_(
                    'Default storage type: (@VALUES@) [@DEFAULT@]: '
                ),
                prompt=True,
                validValues=(
                    'NFS',
                    'FC',
                    'ISCSI',
                    'POSIXFS',
                    'GLUSTERFS',
                ),
                caseSensitive=False,
                default=oenginecons.Defaults.DEFAULT_CONFIG_STORAGE_TYPE,
            )

    @plugin.event(
        stage=plugin.Stages.STAGE_MISC,
        after=(
            oengcommcons.Stages.DB_CONNECTION_AVAILABLE,
        ),
        condition=lambda self: self._enabled,
    )
    def _misc(self):
        self.environment[oengcommcons.EngineDBEnv.STATEMENT].execute(
            statement="""
                select inst_update_default_storage_pool_type (%(type)s)
            """,
            args={
                'type': self.STORAGE_TYPES[
                    self.environment[osetupcons.ConfigEnv.STORAGE_TYPE]
                ],
            },
        )


# vim: expandtab tabstop=4 shiftwidth=4

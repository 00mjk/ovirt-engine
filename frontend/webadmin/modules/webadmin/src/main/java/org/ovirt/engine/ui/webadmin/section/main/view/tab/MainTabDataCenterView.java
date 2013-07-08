package org.ovirt.engine.ui.webadmin.section.main.view.tab;

import java.util.List;

import org.ovirt.engine.core.common.businessentities.StoragePoolStatus;
import org.ovirt.engine.core.common.businessentities.StorageType;
import org.ovirt.engine.core.common.businessentities.StoragePool;
import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.uicommon.model.MainModelProvider;
import org.ovirt.engine.ui.common.widget.action.ActionButtonDefinition;
import org.ovirt.engine.ui.common.widget.action.CommandLocation;
import org.ovirt.engine.ui.common.widget.table.column.EnumColumn;
import org.ovirt.engine.ui.common.widget.table.column.TextColumnWithTooltip;
import org.ovirt.engine.ui.uicommonweb.ReportInit;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.models.datacenters.DataCenterListModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.ApplicationResources;
import org.ovirt.engine.ui.webadmin.section.main.presenter.tab.MainTabDataCenterPresenter;
import org.ovirt.engine.ui.webadmin.section.main.view.AbstractMainTabWithDetailsTableView;
import org.ovirt.engine.ui.webadmin.uicommon.ReportActionsHelper;
import org.ovirt.engine.ui.webadmin.widget.action.WebAdminButtonDefinition;
import org.ovirt.engine.ui.webadmin.widget.action.WebAdminImageButtonDefinition;
import org.ovirt.engine.ui.webadmin.widget.action.WebAdminMenuBarButtonDefinition;
import org.ovirt.engine.ui.webadmin.widget.table.column.CommentColumn;
import org.ovirt.engine.ui.webadmin.widget.table.column.DcStatusColumn;

import com.google.gwt.core.client.GWT;
import com.google.inject.Inject;

public class MainTabDataCenterView extends AbstractMainTabWithDetailsTableView<StoragePool, DataCenterListModel> implements MainTabDataCenterPresenter.ViewDef {

    interface ViewIdHandler extends ElementIdHandler<MainTabDataCenterView> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    @Inject
    public MainTabDataCenterView(MainModelProvider<StoragePool, DataCenterListModel> modelProvider,
            ApplicationResources resources, ApplicationConstants constants) {
        super(modelProvider);
        ViewIdHandler.idHandler.generateAndSetIds(this);
        initTable(resources, constants);
        initWidget(getTable());
    }

    void initTable(ApplicationResources resources, ApplicationConstants constants) {
        getTable().enableColumnResizing();

        getTable().addColumn(new DcStatusColumn(), constants.empty(), "30px"); //$NON-NLS-1$

        TextColumnWithTooltip<StoragePool> nameColumn = new TextColumnWithTooltip<StoragePool>() {
            @Override
            public String getValue(StoragePool object) {
                return object.getName();
            }
        };
        getTable().addColumn(nameColumn, constants.nameDc(), "150px"); //$NON-NLS-1$

        TextColumnWithTooltip<StoragePool> storageTypeColumn = new EnumColumn<StoragePool, StorageType>() {
            @Override
            public StorageType getRawValue(StoragePool object) {
                return object.getstorage_pool_type();
            }
        };
        getTable().addColumn(storageTypeColumn, constants.storgeTypeDc(), "150px"); //$NON-NLS-1$

        TextColumnWithTooltip<StoragePool> statusColumn = new EnumColumn<StoragePool, StoragePoolStatus>() {
            @Override
            public StoragePoolStatus getRawValue(StoragePool object) {
                return object.getstatus();
            }
        };
        getTable().addColumn(statusColumn, constants.statusDc(), "150px"); //$NON-NLS-1$

        TextColumnWithTooltip<StoragePool> versionColumn = new TextColumnWithTooltip<StoragePool>() {
            @Override
            public String getValue(StoragePool object) {
                return object.getcompatibility_version().getValue();
            }
        };
        getTable().addColumn(versionColumn, constants.comptVersDc(), "150px"); //$NON-NLS-1$

        TextColumnWithTooltip<StoragePool> descColumn = new TextColumnWithTooltip<StoragePool>() {
            @Override
            public String getValue(StoragePool object) {
                return object.getdescription();
            }
        };
        getTable().addColumn(descColumn, constants.descriptionDc(), "300px"); //$NON-NLS-1$

        getTable().addColumn(new CommentColumn<StoragePool>(), constants.commentLabel(), "100px"); //$NON-NLS-1$

        getTable().addActionButton(new WebAdminButtonDefinition<StoragePool>(constants.newDC()) {
            @Override
            protected UICommand resolveCommand() {
                return getMainModel().getNewCommand();
            }
        });
        getTable().addActionButton(new WebAdminButtonDefinition<StoragePool>(constants.editDC()) {
            @Override
            protected UICommand resolveCommand() {
                return getMainModel().getEditCommand();
            }
        });
        getTable().addActionButton(new WebAdminButtonDefinition<StoragePool>(constants.removeDC()) {
            @Override
            protected UICommand resolveCommand() {
                return getMainModel().getRemoveCommand();
            }
        });

        getTable().addActionButton(new WebAdminButtonDefinition<StoragePool>(constants.forceRemoveDC()) {
            @Override
            protected UICommand resolveCommand() {
                return getMainModel().getForceRemoveCommand();
            }
        });

        if (ReportInit.getInstance().isReportsEnabled()) {
            List<ActionButtonDefinition<StoragePool>> resourceSubActions =
                    ReportActionsHelper.getInstance().getResourceSubActions("DataCenter", getModelProvider()); //$NON-NLS-1$
            if (resourceSubActions != null && resourceSubActions.size() > 0) {
                getTable().addActionButton(new WebAdminMenuBarButtonDefinition<StoragePool>(constants.showReportDC(),
                        resourceSubActions));
            }
        }

        getTable().addActionButton(new WebAdminImageButtonDefinition<StoragePool>(constants.guideMeDc(),
                resources.guideSmallImage(), resources.guideSmallDisabledImage(), true) {
            @Override
            protected UICommand resolveCommand() {
                return getMainModel().getGuideCommand();
            }
        });

        getTable().addActionButton(new WebAdminButtonDefinition<StoragePool>(constants.reinitializeDC(), CommandLocation.OnlyFromContext) {
            @Override
            protected UICommand resolveCommand() {
                return getMainModel().getRecoveryStorageCommand();
            }
        });
    }
}

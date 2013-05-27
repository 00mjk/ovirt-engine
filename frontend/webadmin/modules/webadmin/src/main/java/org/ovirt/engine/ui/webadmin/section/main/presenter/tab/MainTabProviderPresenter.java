package org.ovirt.engine.ui.webadmin.section.main.presenter.tab;

import java.util.List;

import org.ovirt.engine.core.common.businessentities.Provider;
import org.ovirt.engine.ui.common.uicommon.model.MainModelProvider;
import org.ovirt.engine.ui.common.widget.tab.ModelBoundTabData;
import org.ovirt.engine.ui.uicommonweb.models.providers.ProviderListModel;
import org.ovirt.engine.ui.webadmin.gin.ClientGinjector;
import org.ovirt.engine.ui.webadmin.place.ApplicationPlaces;
import org.ovirt.engine.ui.webadmin.section.main.presenter.AbstractMainTabWithDetailsPresenter;
import org.ovirt.engine.ui.webadmin.section.main.presenter.MainTabPanelPresenter;

import com.google.gwt.event.shared.EventBus;
import com.google.inject.Inject;
import com.gwtplatform.dispatch.annotation.GenEvent;
import com.gwtplatform.mvp.client.TabData;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.annotations.TabInfo;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.PlaceRequest;
import com.gwtplatform.mvp.client.proxy.TabContentProxyPlace;

public class MainTabProviderPresenter extends AbstractMainTabWithDetailsPresenter<Provider, ProviderListModel, MainTabProviderPresenter.ViewDef, MainTabProviderPresenter.ProxyDef> {

    @GenEvent
    public static class ProviderSelectionChange {

        List<Provider> selectedItems;

    }

    @ProxyCodeSplit
    @NameToken(ApplicationPlaces.providerMainTabPlace)
    public interface ProxyDef extends TabContentProxyPlace<MainTabProviderPresenter> {
    }

    public interface ViewDef extends AbstractMainTabWithDetailsPresenter.ViewDef<Provider> {
    }

    @TabInfo(container = MainTabPanelPresenter.class)
    static TabData getTabData(ClientGinjector ginjector) {
        return new ModelBoundTabData(ginjector.getApplicationConstants().providerMainTabLabel(), 11,
                ginjector.getMainTabProviderModelProvider());
    }

    @Inject
    public MainTabProviderPresenter(EventBus eventBus, ViewDef view, ProxyDef proxy,
            PlaceManager placeManager, MainModelProvider<Provider, ProviderListModel> modelProvider) {
        super(eventBus, view, proxy, placeManager, modelProvider);
    }

    @Override
    protected void fireTableSelectionChangeEvent() {
        ProviderSelectionChangeEvent.fire(this, getSelectedItems());
    }

    @Override
    protected PlaceRequest getMainTabRequest() {
        return new PlaceRequest(ApplicationPlaces.providerMainTabPlace);
    }
}


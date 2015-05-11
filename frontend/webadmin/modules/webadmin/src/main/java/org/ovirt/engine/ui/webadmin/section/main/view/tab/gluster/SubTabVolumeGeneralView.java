package org.ovirt.engine.ui.webadmin.section.main.view.tab.gluster;

import java.util.ArrayList;
import java.util.Arrays;

import javax.inject.Inject;

import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeEntity;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeType;
import org.ovirt.engine.ui.common.uicommon.model.DetailModelProvider;
import org.ovirt.engine.ui.common.view.AbstractSubTabFormView;
import org.ovirt.engine.ui.common.widget.form.FormBuilder;
import org.ovirt.engine.ui.common.widget.form.FormItem;
import org.ovirt.engine.ui.common.widget.form.GeneralFormPanel;
import org.ovirt.engine.ui.common.widget.label.TextBoxLabel;
import org.ovirt.engine.ui.common.widget.label.TextBoxLabelBase;
import org.ovirt.engine.ui.common.widget.label.VolumeTransportTypeLabel;
import org.ovirt.engine.ui.uicommonweb.models.gluster.VolumeGeneralModel;
import org.ovirt.engine.ui.uicommonweb.models.volumes.VolumeListModel;
import org.ovirt.engine.ui.uicompat.EnumTranslator;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.IEventListener;
import org.ovirt.engine.ui.uicompat.PropertyChangedEventArgs;
import org.ovirt.engine.ui.uicompat.Translator;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.section.main.presenter.tab.gluster.SubTabVolumeGeneralPresenter;
import org.ovirt.engine.ui.webadmin.widget.label.DetailsTextBoxLabel;
import org.ovirt.engine.ui.webadmin.widget.label.VolumeCapacityLabel;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.Editor;
import com.google.gwt.editor.client.SimpleBeanEditorDriver;
import com.google.gwt.uibinder.client.UiField;

public class SubTabVolumeGeneralView extends AbstractSubTabFormView<GlusterVolumeEntity, VolumeListModel, VolumeGeneralModel> implements SubTabVolumeGeneralPresenter.ViewDef, Editor<VolumeGeneralModel> {

    interface Driver extends SimpleBeanEditorDriver<VolumeGeneralModel, SubTabVolumeGeneralView> {
    }

    @UiField(provided = true)
    GeneralFormPanel formPanel;

    protected static final ApplicationConstants constants = GWT.create(ApplicationConstants.class);

    TextBoxLabel name = new TextBoxLabel();
    TextBoxLabel volumeId = new TextBoxLabel();
    TextBoxLabel volumeType = new TextBoxLabel();
    TextBoxLabel replicaCount = new TextBoxLabel();
    TextBoxLabel stripeCount = new TextBoxLabel();
    TextBoxLabel disperseCount = new TextBoxLabel();
    TextBoxLabel redundancyCount = new TextBoxLabel();
    TextBoxLabel numOfBricks = new TextBoxLabel();
    VolumeTransportTypeLabel transportTypes = new VolumeTransportTypeLabel();
    TextBoxLabel snapMaxLimit = new TextBoxLabel();

    VolumeCapacityLabel<Long> volumeTotalCapacity;
    VolumeCapacityLabel<Long> volumeUsedCapacity;
    VolumeCapacityLabel<Long> volumeFreeCapacity;

    FormBuilder formBuilder;

    FormItem replicaFormItem;
    FormItem stripeFormItem;
    FormItem disperseCountFormItem;
    FormItem redundancyCountFormItem;

    @Ignore
    DetailsTextBoxLabel<ArrayList<TextBoxLabelBase<Long>>, Long> volumeCapacityDetailsLabel = new DetailsTextBoxLabel<ArrayList<TextBoxLabelBase<Long>>, Long>(constants.total(), constants.used(), constants.free());

    private final Driver driver = GWT.create(Driver.class);

    @Inject
    public SubTabVolumeGeneralView(DetailModelProvider<VolumeListModel, VolumeGeneralModel> modelProvider, ApplicationConstants constants) {
        super(modelProvider);

        initCapacityLabel(constants);

        // Init form panel:
        formPanel = new GeneralFormPanel();

        initWidget(formPanel);
        driver.initialize(this);

        // Build a form using the FormBuilder
        formBuilder = new FormBuilder(formPanel, 1, 11);

        formBuilder.addFormItem(new FormItem(constants.NameVolume(), name, 0, 0));

        formBuilder.addFormItem(new FormItem(constants.volumeIdVolume(), volumeId, 1, 0));
        formBuilder.addFormItem(new FormItem(constants.volumeTypeVolume(), volumeType, 2, 0));

        replicaFormItem = new FormItem(constants.replicaCountVolume(), replicaCount, 3, 0);
        formBuilder.addFormItem(replicaFormItem);

        stripeFormItem = new FormItem(constants.stripeCountVolume(), stripeCount, 4, 0);
        formBuilder.addFormItem(stripeFormItem);

        formBuilder.addFormItem(new FormItem(constants.numberOfBricksVolume(), numOfBricks, 5, 0));
        formBuilder.addFormItem(new FormItem(constants.transportTypesVolume(), transportTypes, 6, 0));
        formBuilder.addFormItem(new FormItem(constants.maxNumberOfSnapshotsVolume(), snapMaxLimit, 7, 0));
        disperseCountFormItem = new FormItem(constants.disperseCount(), disperseCount, 8, 0);
        formBuilder.addFormItem(disperseCountFormItem);

        redundancyCountFormItem = new FormItem(constants.redundancyCount(), redundancyCount, 9, 0);
        formBuilder.addFormItem(redundancyCountFormItem);

        volumeCapacityDetailsLabel.setWidth("275px");//$NON-NLS-1$
        formBuilder.addFormItem(new FormItem(constants.volumeCapacityStatistics(), volumeCapacityDetailsLabel, 10, 0));

        getDetailModel().getPropertyChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                VolumeGeneralModel model = (VolumeGeneralModel) sender;
                if ("VolumeType".equals(((PropertyChangedEventArgs) args).propertyName)) { //$NON-NLS-1$
                    translateVolumeType((GlusterVolumeEntity) model.getEntity());
                }
            }
        });
    }

    private void initCapacityLabel(ApplicationConstants constants) {
        this.volumeTotalCapacity = new VolumeCapacityLabel<Long>(constants);
        this.volumeFreeCapacity = new VolumeCapacityLabel<Long>(constants);
        this.volumeUsedCapacity = new VolumeCapacityLabel<Long>(constants);
    }

    @Override
    public void setMainTabSelectedItem(GlusterVolumeEntity selectedItem) {
        driver.edit(getDetailModel());

        replicaFormItem.setIsAvailable(selectedItem.getVolumeType().isReplicatedType());
        stripeFormItem.setIsAvailable(selectedItem.getVolumeType().isStripedType());
        disperseCountFormItem.setIsAvailable(selectedItem.getVolumeType().isDispersedType());
        redundancyCountFormItem.setIsAvailable(selectedItem.getVolumeType().isDispersedType());

        ArrayList<TextBoxLabelBase<Long>> volumeCapacityDetails =
                new ArrayList<TextBoxLabelBase<Long>>(Arrays.asList(volumeTotalCapacity, volumeUsedCapacity, volumeFreeCapacity));
        volumeCapacityDetailsLabel.setValue(volumeCapacityDetails);

        formBuilder.update(getDetailModel());
    }

    private void translateVolumeType(GlusterVolumeEntity volumeEntity) {
        Translator translator = EnumTranslator.create(GlusterVolumeType.class);
        if (translator.containsKey(volumeEntity.getVolumeType())) {
            getDetailModel().setVolumeTypeSilently(translator.get(volumeEntity.getVolumeType()));
        }
    }

}

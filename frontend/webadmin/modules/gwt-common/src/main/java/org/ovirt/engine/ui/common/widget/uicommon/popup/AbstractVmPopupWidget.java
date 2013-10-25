
package org.ovirt.engine.ui.common.widget.uicommon.popup;

import static org.ovirt.engine.ui.common.widget.uicommon.popup.vm.PopupWidgetConfig.simpleField;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.ovirt.engine.core.common.businessentities.Disk;
import org.ovirt.engine.core.common.businessentities.Disk.DiskStorageType;
import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.common.businessentities.ImageStatus;
import org.ovirt.engine.core.common.businessentities.Quota;
import org.ovirt.engine.core.common.businessentities.UsbPolicy;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.VmTemplate;
import org.ovirt.engine.core.common.queries.ConfigurationValues;
import org.ovirt.engine.core.compat.StringHelper;
import org.ovirt.engine.ui.common.CommonApplicationConstants;
import org.ovirt.engine.ui.common.CommonApplicationMessages;
import org.ovirt.engine.ui.common.CommonApplicationResources;
import org.ovirt.engine.ui.common.CommonApplicationTemplates;
import org.ovirt.engine.ui.common.idhandler.WithElementId;
import org.ovirt.engine.ui.common.widget.Align;
import org.ovirt.engine.ui.common.widget.dialog.AdvancedParametersExpander;
import org.ovirt.engine.ui.common.widget.dialog.InfoIcon;
import org.ovirt.engine.ui.common.widget.dialog.tab.DialogTab;
import org.ovirt.engine.ui.common.widget.dialog.tab.DialogTabPanel;
import org.ovirt.engine.ui.common.widget.editor.EntityModelCellTable;
import org.ovirt.engine.ui.common.widget.editor.EntityModelCheckBoxEditor;
import org.ovirt.engine.ui.common.widget.editor.EntityModelRadioButtonEditor;
import org.ovirt.engine.ui.common.widget.editor.EntityModelTextBoxEditor;
import org.ovirt.engine.ui.common.widget.editor.EntityModelTextBoxOnlyEditor;
import org.ovirt.engine.ui.common.widget.editor.ListModelListBoxEditor;
import org.ovirt.engine.ui.common.widget.editor.ListModelListBoxOnlyEditor;
import org.ovirt.engine.ui.common.widget.editor.ListModelTypeAheadListBoxEditor;
import org.ovirt.engine.ui.common.widget.form.key_value.KeyValueWidget;
import org.ovirt.engine.ui.common.widget.parser.MemorySizeParser;
import org.ovirt.engine.ui.common.widget.profile.ProfilesInstanceTypeEditor;
import org.ovirt.engine.ui.common.widget.renderer.EnumRenderer;
import org.ovirt.engine.ui.common.widget.renderer.MemorySizeRenderer;
import org.ovirt.engine.ui.common.widget.renderer.NullSafeRenderer;
import org.ovirt.engine.ui.common.widget.table.column.TextColumnWithTooltip;
import org.ovirt.engine.ui.common.widget.uicommon.popup.vm.PopupWidgetConfigMap;
import org.ovirt.engine.ui.common.widget.uicommon.storage.DisksAllocationView;
import org.ovirt.engine.ui.uicommonweb.dataprovider.AsyncDataProvider;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.DataCenterWithCluster;
import org.ovirt.engine.ui.uicommonweb.models.vms.DiskModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.TimeZoneModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.UnitVmModel;
import org.ovirt.engine.ui.uicompat.EnumTranslator;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.IEventListener;
import org.ovirt.engine.ui.uicompat.PropertyChangedEventArgs;
import org.ovirt.engine.ui.uicompat.external.StringUtils;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.SimpleBeanEditorDriver;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.text.shared.AbstractRenderer;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.CellTable.Resources;
import com.google.gwt.user.client.ui.ButtonBase;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.ValueLabel;
import com.google.gwt.user.client.ui.Widget;

public abstract class AbstractVmPopupWidget extends AbstractModeSwitchingPopupWidget<UnitVmModel> {

    interface Driver extends SimpleBeanEditorDriver<UnitVmModel, AbstractVmPopupWidget> {
    }

    interface ViewUiBinder extends UiBinder<DialogTabPanel, AbstractVmPopupWidget> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    protected interface Style extends CssResource {
        String longCheckboxContent();

        String provisioningEditorContent();

        String provisioningRadioContent();

        String cdAttachedLabelWidth();

        String assignedVmsLabel();

        String labelDisabled();

        String generalTabExtendedRightWidgetWidth();

        String cdImageEditor();

        String monitorsStyles();

        String migrationSelectorInner();

        String isVirtioScsiEnabledEditor();
    }

    @UiField
    protected Style style;

    // ==General Tab==
    @UiField
    protected DialogTab generalTab;

    @UiField(provided = true)
    @Path(value = "dataCenterWithClustersList.selectedItem")
    @WithElementId("dataCenterWithCluster")
    public ListModelTypeAheadListBoxEditor<Object> dataCenterWithClusterEditor;

    @UiField(provided = true)
    @Path(value = "quota.selectedItem")
    @WithElementId("quota")
    public ListModelTypeAheadListBoxEditor<Object> quotaEditor;

    @UiField
    @Ignore
    public Label nameLabel;

    @UiField(provided = true)
    @Path(value = "name.entity")
    @WithElementId("name")
    public EntityModelTextBoxOnlyEditor nameEditor;

    @UiField(provided = true)
    @Ignore
    public InfoIcon poolNameIcon;

    @UiField(provided = true)
    @Path(value = "description.entity")
    @WithElementId("description")
    public EntityModelTextBoxEditor descriptionEditor;

    @UiField
    @Path(value = "comment.entity")
    @WithElementId("comment")
    public EntityModelTextBoxEditor commentEditor;

    @UiField(provided = true)
    @Path(value = "template.selectedItem")
    @WithElementId("template")
    public ListModelTypeAheadListBoxEditor<Object> templateEditor;

    @UiField(provided = true)
    @Path(value = "OSType.selectedItem")
    @WithElementId("osType")
    public ListModelListBoxEditor<Object> oSTypeEditor;

    @UiField(provided = true)
    @Path(value = "vmType.selectedItem")
    @WithElementId("vmType")
    public ListModelListBoxEditor<Object> vmTypeEditor;

    @UiField(provided = true)
    @Path(value = "isDeleteProtected.entity")
    @WithElementId("isDeleteProtected")
    public EntityModelCheckBoxEditor isDeleteProtectedEditor;

    @UiField
    public Panel logicalNetworksEditorPanel;

    @UiField
    @Ignore
    @WithElementId("vnicsEditor")
    public ProfilesInstanceTypeEditor profilesInstanceTypeEditor;

    @UiField
    @Ignore
    Label generalWarningMessage;

    // == System ==
    @UiField
    protected DialogTab systemTab;

    @UiField(provided = true)
    @Path(value = "memSize.entity")
    @WithElementId("memSize")
    public EntityModelTextBoxEditor memSizeEditor;

    @UiField(provided = true)
    @Path(value = "totalCPUCores.entity")
    @WithElementId("totalCPUCores")
    public EntityModelTextBoxEditor totalvCPUsEditor;

    @UiField
    @Ignore
    AdvancedParametersExpander vcpusAdvancedParameterExpander;

    @UiField
    @Ignore
    Panel vcpusAdvancedParameterExpanderContent;

    @UiField(provided = true)
    @Path(value = "numOfSockets.selectedItem")
    @WithElementId("numOfSockets")
    public ListModelListBoxEditor<Object> numOfSocketsEditor;

    @UiField(provided = true)
    @Path(value = "coresPerSocket.selectedItem")
    @WithElementId("coresPerSocket")
    public ListModelListBoxEditor<Object> corePerSocketEditor;

    @UiField(provided = true)
    @Path(value = "isSoundcardEnabled.entity")
    @WithElementId("isSoundcardEnabled")
    public EntityModelCheckBoxEditor isSoundcardEnabledEditor;

    @UiField(provided = true)
    @Path("copyPermissions.entity")
    @WithElementId("copyTemplatePermissions")
    public EntityModelCheckBoxEditor copyTemplatePermissionsEditor;

    // == Pools ==
    @UiField
    protected DialogTab poolTab;

    @UiField(provided = true)
    @Path(value = "poolType.selectedItem")
    @WithElementId("poolType")
    public ListModelListBoxEditor<Object> poolTypeEditor;

    @UiField(provided = true)
    @Ignore
    public InfoIcon newPoolPrestartedVmsIcon;

    @UiField(provided = true)
    @Ignore
    public InfoIcon editPoolPrestartedVmsIcon;

    @UiField(provided = true)
    @Ignore
    public InfoIcon newPoolMaxAssignedVmsPerUserIcon;

    @UiField(provided = true)
    @Ignore
    public InfoIcon editPoolMaxAssignedVmsPerUserIcon;

    @UiField(provided = true)
    @Path(value = "prestartedVms.entity")
    @WithElementId("prestartedVms")
    public EntityModelTextBoxOnlyEditor prestartedVmsEditor;

    @UiField(provided = true)
    @Path("maxAssignedVmsPerUser.entity")
    @WithElementId("maxAssignedVmsPerUser")
    public EntityModelTextBoxOnlyEditor maxAssignedVmsPerUserEditor;

    @UiField
    @Ignore
    public FlowPanel newPoolEditVmsPanel;

    @UiField
    @Ignore
    public FlowPanel newPoolEditMaxAssignedVmsPerUserPanel;

    @UiField
    @Ignore
    public Label prestartedLabel;

    @UiField(provided = true)
    @Path("numOfDesktops.entity")
    @WithElementId("numOfVms")
    public EntityModelTextBoxEditor numOfVmsEditor;

    @UiField
    @Ignore
    public FlowPanel editPoolEditVmsPanel;

    @UiField
    public FlowPanel editIncreaseVmsPanel;

    @UiField
    @Ignore
    public FlowPanel editPoolIncraseNumOfVmsPanel;

    @UiField
    public FlowPanel editPrestartedVmsPanel;
    @UiField
    @Ignore
    public FlowPanel editPoolEditMaxAssignedVmsPerUserPanel;

    @UiField
    @Ignore
    public Label editPrestartedVmsLabel;

    @UiField(provided = true)
    @Path("prestartedVms.entity")
    @WithElementId("editPrestartedVms")
    public EntityModelTextBoxOnlyEditor editPrestartedVmsEditor;

    @UiField(provided = true)
    @Path("numOfDesktops.entity")
    @WithElementId("incraseNumOfVms")
    public EntityModelTextBoxOnlyEditor incraseNumOfVmsEditor;

    @UiField(provided = true)
    @Path("maxAssignedVmsPerUser.entity")
    @WithElementId("editMaxAssignedVmsPerUser")
    public EntityModelTextBoxOnlyEditor editMaxAssignedVmsPerUserEditor;

    @UiField(provided = true)
    @Path("assignedVms.entity")
    public ValueLabel<Object> outOfxInPool;

    // ==Initial run Tab==
    @UiField
    protected DialogTab initialRunTab;

    @UiField(provided = true)
    @Path(value = "domain.selectedItem")
    @WithElementId("domain")
    public ListModelListBoxEditor<Object> domainEditor;

    @UiField(provided = true)
    @Path(value = "timeZone.selectedItem")
    @WithElementId("timeZone")
    public ListModelListBoxEditor<Object> timeZoneEditor;

    // ==Console Tab==
    @UiField
    protected DialogTab consoleTab;

    @UiField(provided = true)
    @Path(value = "displayProtocol.selectedItem")
    @WithElementId("displayProtocol")
    public ListModelListBoxEditor<Object> displayProtocolEditor;

    @UiField(provided = true)
    @Path(value = "vncKeyboardLayout.selectedItem")
    @WithElementId("vncKeyboardLayout")
    public ListModelListBoxEditor<Object> vncKeyboardLayoutEditor;

    @UiField(provided = true)
    @Path(value = "usbPolicy.selectedItem")
    @WithElementId("usbPolicy")
    public ListModelListBoxEditor<Object> usbSupportEditor;

    @UiField(provided = true)
    @Path(value = "numOfMonitors.selectedItem")
    @WithElementId("numOfMonitors")
    public ListModelListBoxEditor<Object> numOfMonitorsEditor;

    @UiField(provided = true)
    @Path(value = "isSingleQxlEnabled.entity")
    @WithElementId("isSingleQxlEnabled")
    public EntityModelCheckBoxEditor isSingleQxlEnabledEditor;

    @UiField(provided = true)
    @Path(value = "isStateless.entity")
    @WithElementId("isStateless")
    public EntityModelCheckBoxEditor isStatelessEditor;

    @UiField(provided = true)
    @Path(value = "isRunAndPause.entity")
    @WithElementId("isRunAndPause")
    public EntityModelCheckBoxEditor isRunAndPauseEditor;

    @UiField(provided = true)
    @Path(value = "isSmartcardEnabled.entity")
    @WithElementId("isSmartcardEnabled")
    public EntityModelCheckBoxEditor isSmartcardEnabledEditor;

    @UiField(provided = true)
    @Path(value = "allowConsoleReconnect.entity")
    @WithElementId("allowConsoleReconnect")
    public EntityModelCheckBoxEditor allowConsoleReconnectEditor;

    @UiField(provided = true)
    @Path(value = "isConsoleDeviceEnabled.entity")
    @WithElementId("isConsoleDeviceEnabled")
    public EntityModelCheckBoxEditor isConsoleDeviceEnabledEditor;

    // ==Host Tab==
    @UiField
    protected DialogTab hostTab;

    @UiField(provided = true)
    @Path(value = "hostCpu.entity")
    @WithElementId("hostCpu")
    public EntityModelCheckBoxEditor hostCpuEditor;

    @UiField(provided = true)
    @Path(value = "migrationMode.selectedItem")
    @WithElementId("migrationMode")
    public ListModelListBoxEditor<Object> migrationModeEditor;

    @UiField(provided = true)
    @Ignore
    @WithElementId("specificHost")
    public RadioButton specificHost;

    @UiField
    @Ignore
    public Label specificHostLabel;

    @UiField(provided = true)
    public InfoIcon nonEditableWhileVmNotDownInfo;

    @UiField(provided = true)
    @Path(value = "defaultHost.selectedItem")
    @WithElementId("defaultHost")
    public ListModelListBoxEditor<Object> defaultHostEditor;

    @UiField(provided = true)
    @Path(value = "isAutoAssign.entity")
    @WithElementId("isAutoAssign")
    public EntityModelRadioButtonEditor isAutoAssignEditor;

    @UiField(provided = true)
    InfoIcon cpuPinningInfo;

    @UiField(provided = true)
    @Path(value = "cpuPinning.entity")
    @WithElementId("cpuPinning")
    public EntityModelTextBoxOnlyEditor cpuPinning;

    @UiField(provided = true)
    @Path(value = "cpuSharesAmountSelection.selectedItem")
    @WithElementId("cpuSharesAmountSelection")
    public ListModelListBoxOnlyEditor<Object> cpuSharesAmountSelectionEditor;

    @UiField
    @Ignore
    public Label cpuSharesEditor;

    @UiField(provided = true)
    @Path(value = "cpuSharesAmount.entity")
    @WithElementId("cpuSharesAmount")
    public EntityModelTextBoxOnlyEditor cpuSharesAmountEditor;

    // ==High Availability Tab==
    @UiField
    protected DialogTab highAvailabilityTab;

    @UiField(provided = true)
    @Path(value = "isHighlyAvailable.entity")
    @WithElementId("isHighlyAvailable")
    public EntityModelCheckBoxEditor isHighlyAvailableEditor;

    // TODO: Priority is a ListModel which is rendered as RadioBox
    @UiField(provided = true)
    @Ignore
    @WithElementId("priority")
    public EntityModelCellTable<ListModel> priorityEditor;

    @UiField(provided = true)
    @Path(value = "watchdogModel.selectedItem")
    @WithElementId("watchdogModel")
    public ListModelListBoxEditor<Object> watchdogModelEditor;

    @UiField(provided = true)
    @Path(value = "watchdogAction.selectedItem")
    @WithElementId("watchdogAction")
    public ListModelListBoxEditor<Object> watchdogActionEditor;

    // ==Resource Allocation Tab==
    @UiField
    protected DialogTab resourceAllocationTab;

    @UiField
    protected FlowPanel storageAllocationPanel;

    @UiField
    protected HorizontalPanel provisionSelectionPanel;

    @UiField
    protected FlowPanel disksAllocationPanel;

    @UiField
    @Ignore
    @WithElementId("provisioning")
    public ListModelListBoxEditor<Object> provisioningEditor;

    @UiField(provided = true)
    @Path(value = "minAllocatedMemory.entity")
    @WithElementId("minAllocatedMemory")
    public EntityModelTextBoxEditor minAllocatedMemoryEditor;

    @UiField(provided = true)
    @Path(value = "memoryBalloonDeviceEnabled.entity")
    EntityModelCheckBoxEditor isMemoryBalloonDeviceEnabled;

    @UiField(provided = true)
    @Path(value = "provisioningThin_IsSelected.entity")
    @WithElementId("provisioningThin")
    public EntityModelRadioButtonEditor provisioningThinEditor;

    @UiField(provided = true)
    @Path(value = "provisioningClone_IsSelected.entity")
    @WithElementId("provisioningClone")
    public EntityModelRadioButtonEditor provisioningCloneEditor;

    @UiField(provided = true)
    @Path(value = "isVirtioScsiEnabled.entity")
    EntityModelCheckBoxEditor isVirtioScsiEnabled;

    @UiField(provided = true)
    @Ignore
    public InfoIcon isVirtioScsiEnabledInfoIcon;

    @UiField
    @Ignore
    Label disksAllocationLabel;

    @UiField(provided = true)
    @Ignore
    @WithElementId("disksAllocation")
    public DisksAllocationView disksAllocationView;

    // ==Boot Options Tab==
    @UiField
    protected DialogTab bootOptionsTab;

    @UiField(provided = true)
    @Path(value = "firstBootDevice.selectedItem")
    @WithElementId("firstBootDevice")
    public ListModelListBoxEditor<Object> firstBootDeviceEditor;

    @UiField(provided = true)
    @Path(value = "secondBootDevice.selectedItem")
    @WithElementId("secondBootDevice")
    public ListModelListBoxEditor<Object> secondBootDeviceEditor;

    @UiField(provided = true)
    @Path(value = "cdImage.selectedItem")
    @WithElementId("cdImage")
    public ListModelListBoxEditor<Object> cdImageEditor;

    @UiField(provided = true)
    @Path(value = "cdAttached.entity")
    @WithElementId("cdAttached")
    public EntityModelCheckBoxEditor cdAttachedEditor;

    @UiField
    protected FlowPanel linuxBootOptionsPanel;

    @UiField(provided = true)
    @Path(value = "kernel_path.entity")
    @WithElementId("kernelPath")
    public EntityModelTextBoxEditor kernel_pathEditor;

    @UiField(provided = true)
    @Path(value = "initrd_path.entity")
    @WithElementId("initrdPath")
    public EntityModelTextBoxEditor initrd_pathEditor;

    @UiField(provided = true)
    @Path(value = "kernel_parameters.entity")
    @WithElementId("kernelParameters")
    public EntityModelTextBoxEditor kernel_parametersEditor;

    @UiField
    @Ignore
    Label nativeUsbWarningMessage;

    // ==Custom Properties Tab==
    @UiField
    protected DialogTab customPropertiesTab;

    @UiField
    @Ignore
    protected KeyValueWidget customPropertiesSheetEditor;

    private final CommonApplicationMessages messages;

    @UiField
    @Ignore
    protected AdvancedParametersExpander expander;

    @UiField
    @Ignore
    Panel expanderContent;

    @UiField
    @Ignore
    ButtonBase refreshButton;

    private UnitVmModel unitVmModel;

    private final Driver driver = GWT.create(Driver.class);

    private final CommonApplicationTemplates applicationTemplates;

    private final CommonApplicationConstants constants;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public AbstractVmPopupWidget(CommonApplicationConstants constants,
            CommonApplicationResources resources,
            final CommonApplicationMessages messages,
            CommonApplicationTemplates applicationTemplates) {

        this.messages = messages;
        this.applicationTemplates = applicationTemplates;
        this.constants = constants;

        initListBoxEditors();
        // Contains a special parser/renderer
        memSizeEditor = new EntityModelTextBoxEditor(
                new MemorySizeRenderer(constants), new MemorySizeParser(), new ModeSwitchingVisibilityRenderer());
        minAllocatedMemoryEditor = new EntityModelTextBoxEditor(
                new MemorySizeRenderer(constants), new MemorySizeParser(), new ModeSwitchingVisibilityRenderer());

        // TODO: How to align right without creating the widget manually?
        hostCpuEditor = new EntityModelCheckBoxEditor(Align.RIGHT, new ModeSwitchingVisibilityRenderer());
        isHighlyAvailableEditor = new EntityModelCheckBoxEditor(Align.RIGHT, new ModeSwitchingVisibilityRenderer());
        watchdogModelEditor = new ListModelListBoxEditor<Object>(new ModeSwitchingVisibilityRenderer());
        watchdogActionEditor = new ListModelListBoxEditor<Object>(new ModeSwitchingVisibilityRenderer());
        isStatelessEditor = new EntityModelCheckBoxEditor(Align.RIGHT, new ModeSwitchingVisibilityRenderer());
        isRunAndPauseEditor = new EntityModelCheckBoxEditor(Align.RIGHT, new ModeSwitchingVisibilityRenderer());
        isDeleteProtectedEditor = new EntityModelCheckBoxEditor(Align.RIGHT, new ModeSwitchingVisibilityRenderer());
        isSmartcardEnabledEditor = new EntityModelCheckBoxEditor(Align.RIGHT, new ModeSwitchingVisibilityRenderer());
        isConsoleDeviceEnabledEditor = new EntityModelCheckBoxEditor(Align.RIGHT, new ModeSwitchingVisibilityRenderer(), true);
        cdAttachedEditor = new EntityModelCheckBoxEditor(Align.LEFT, new ModeSwitchingVisibilityRenderer());
        allowConsoleReconnectEditor = new EntityModelCheckBoxEditor(Align.RIGHT, new ModeSwitchingVisibilityRenderer());
        isSoundcardEnabledEditor = new EntityModelCheckBoxEditor(Align.RIGHT, new ModeSwitchingVisibilityRenderer());
        copyTemplatePermissionsEditor = new EntityModelCheckBoxEditor(Align.RIGHT, new ModeSwitchingVisibilityRenderer());
        isMemoryBalloonDeviceEnabled = new EntityModelCheckBoxEditor(Align.RIGHT, new ModeSwitchingVisibilityRenderer(), true);
        isVirtioScsiEnabled = new EntityModelCheckBoxEditor(Align.RIGHT, new ModeSwitchingVisibilityRenderer());
        isSingleQxlEnabledEditor = new EntityModelCheckBoxEditor(Align.RIGHT, new ModeSwitchingVisibilityRenderer());
        cpuPinningInfo =
                new InfoIcon(SafeHtmlUtils.fromTrustedString(applicationTemplates.italicFixedWidth("400px",//$NON-NLS-1$
                        constants.cpuPinningLabelExplanation())
                        .asString()
                        .replaceAll("(\r\n|\n)", "<br />")), resources);//$NON-NLS-1$ //$NON-NLS-2$
        isVirtioScsiEnabledInfoIcon =
                new InfoIcon(applicationTemplates.italicText(constants.isVirtioScsiEnabledInfo()), resources);
        nonEditableWhileVmNotDownInfo =
                new InfoIcon(applicationTemplates.italicText(constants.nonEditableMigrationFieldsWhileVmNotDownInfo()),
                        resources);
        priorityEditor = new EntityModelCellTable<ListModel>(
                (Resources) GWT.create(ButtonCellTableResources.class));
        disksAllocationView = new DisksAllocationView(constants);

        initPoolSpecificWidgets(resources, messages);
        initTextBoxEditors();

        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));

        expander.initWithContent(expanderContent.getElement());
        vcpusAdvancedParameterExpander.initWithContent(vcpusAdvancedParameterExpanderContent.getElement());
        editPrestartedVmsEditor.setKeepTitleOnSetEnabled(true);

        applyStyles();

        localize(constants);

        super.initializeModeSwitching(generalTab);

        generateIds();

        priorityEditor.addEntityModelColumn(new TextColumnWithTooltip<EntityModel>() {
            @Override
            public String getValue(EntityModel model) {
                return model.getTitle();
            }
        }, ""); //$NON-NLS-1$

        driver.initialize(this);
    }

    private void initTextBoxEditors() {
        descriptionEditor = new EntityModelTextBoxEditor(new ModeSwitchingVisibilityRenderer());
        commentEditor = new EntityModelTextBoxEditor(new ModeSwitchingVisibilityRenderer());
        totalvCPUsEditor = new EntityModelTextBoxEditor(new ModeSwitchingVisibilityRenderer());
        numOfVmsEditor = new EntityModelTextBoxEditor(new ModeSwitchingVisibilityRenderer());
        cpuPinning = new EntityModelTextBoxOnlyEditor(new ModeSwitchingVisibilityRenderer());
        cpuSharesAmountEditor = new EntityModelTextBoxOnlyEditor(new ModeSwitchingVisibilityRenderer());
        kernel_pathEditor = new EntityModelTextBoxEditor(new ModeSwitchingVisibilityRenderer());
        initrd_pathEditor = new EntityModelTextBoxEditor(new ModeSwitchingVisibilityRenderer());
        kernel_parametersEditor = new EntityModelTextBoxEditor(new ModeSwitchingVisibilityRenderer());
        nameEditor = new EntityModelTextBoxOnlyEditor(new ModeSwitchingVisibilityRenderer());
        prestartedVmsEditor = new EntityModelTextBoxOnlyEditor(new ModeSwitchingVisibilityRenderer());
        editPrestartedVmsEditor = new EntityModelTextBoxOnlyEditor(new ModeSwitchingVisibilityRenderer());
        maxAssignedVmsPerUserEditor = new EntityModelTextBoxOnlyEditor(new ModeSwitchingVisibilityRenderer());
        editMaxAssignedVmsPerUserEditor = new EntityModelTextBoxOnlyEditor(new ModeSwitchingVisibilityRenderer());
    }

    protected void initPoolSpecificWidgets(CommonApplicationResources resources,
            final CommonApplicationMessages messages) {
        createNumOfDesktopEditors();

        incraseNumOfVmsEditor.setKeepTitleOnSetEnabled(true);
        numOfVmsEditor.setKeepTitleOnSetEnabled(true);

        newPoolPrestartedVmsIcon =
                new InfoIcon(applicationTemplates.italicText(messages.prestartedHelp()), resources);

        editPoolPrestartedVmsIcon =
                new InfoIcon(applicationTemplates.italicText(messages.prestartedHelp()), resources);

        poolNameIcon =
                new InfoIcon(applicationTemplates.italicText(messages.poolNameHelp()), resources);

        newPoolMaxAssignedVmsPerUserIcon =
                new InfoIcon(applicationTemplates.italicText(messages.maxAssignedVmsPerUserHelp()), resources);

        editPoolMaxAssignedVmsPerUserIcon =
                new InfoIcon(applicationTemplates.italicText(messages.maxAssignedVmsPerUserHelp()), resources);

        outOfxInPool = new ValueLabel<Object>(new AbstractRenderer<Object>() {

            @Override
            public String render(Object object) {
                return messages.outOfXVMsInPool(object.toString());
            }

        });
    }

    /**
     * There are two editors which edits the same entity - in the correct subclass make sure that the correct one's
     * value is used to edit the model
     * <p>
     * The default implementation just creates the simple editors
     */
    protected void createNumOfDesktopEditors() {
        incraseNumOfVmsEditor = new EntityModelTextBoxOnlyEditor();
        numOfVmsEditor = new EntityModelTextBoxEditor();
    }

    protected abstract void generateIds();

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void initListBoxEditors() {
        // General tab
        dataCenterWithClusterEditor = new ListModelTypeAheadListBoxEditor<Object>(
                new ListModelTypeAheadListBoxEditor.NullSafeSuggestBoxRenderer<Object>() {

                    @Override
                    public String getReplacementStringNullSafe(Object data) {
                        return ((DataCenterWithCluster) data).getCluster().getName() + "/" //$NON-NLS-1$
                                + ((DataCenterWithCluster) data).getDataCenter().getName();
                    }

                    @Override
                    public String getDisplayStringNullSafe(Object data) {
                        String dcDescription =
                                ((DataCenterWithCluster) data).getDataCenter().getdescription();

                        return typeAheadNameDescriptionTemplateNullSafe(
                                ((DataCenterWithCluster) data).getCluster().getName(),
                                !StringHelper.isNullOrEmpty(dcDescription) ? dcDescription
                                        : ((DataCenterWithCluster) data).getDataCenter().getName()
                        );
                    }

                },
                new ModeSwitchingVisibilityRenderer());

        quotaEditor = new ListModelTypeAheadListBoxEditor<Object>(
                new ListModelTypeAheadListBoxEditor.NullSafeSuggestBoxRenderer<Object>() {

                    @Override
                    public String getReplacementStringNullSafe(Object data) {
                        return ((Quota) data).getQuotaName();
                    }

                    @Override
                    public String getDisplayStringNullSafe(Object data) {
                        return typeAheadNameDescriptionTemplateNullSafe(
                                ((Quota) data).getQuotaName(),
                                ((Quota) data).getDescription()
                        );
                    }

                },
                new ModeSwitchingVisibilityRenderer());

        templateEditor = new ListModelTypeAheadListBoxEditor<Object>(
                new ListModelTypeAheadListBoxEditor.NullSafeSuggestBoxRenderer<Object>() {

                    @Override
                    public String getReplacementStringNullSafe(Object data) {
                        return ((VmTemplate) data).getName();
                    }

                    @Override
                    public String getDisplayStringNullSafe(Object data) {
                        return typeAheadNameDescriptionTemplateNullSafe(
                                ((VmTemplate) data).getName(),
                                ((VmTemplate) data).getDescription()
                        );
                    }
                },
                new ModeSwitchingVisibilityRenderer());


        oSTypeEditor = new ListModelListBoxEditor<Object>(new AbstractRenderer<Object>() {
            @Override
            public String render(Object object) {
                return AsyncDataProvider.getOsName((Integer)object);
            }
        }, new ModeSwitchingVisibilityRenderer());
        vmTypeEditor = new ListModelListBoxEditor<Object>(new EnumRenderer(), new ModeSwitchingVisibilityRenderer());

        numOfSocketsEditor = new ListModelListBoxEditor<Object>(new ModeSwitchingVisibilityRenderer());
        corePerSocketEditor = new ListModelListBoxEditor<Object>(new ModeSwitchingVisibilityRenderer());

        // Pools
        poolTypeEditor = new ListModelListBoxEditor<Object>(new NullSafeRenderer<Object>() {
            @Override
            public String renderNullSafe(Object object) {
                return ((EntityModel) object).getTitle();
            }
        }, new ModeSwitchingVisibilityRenderer());

        // Windows Sysprep
        domainEditor = new ListModelListBoxEditor<Object>(new NullSafeRenderer<Object>() {
            @Override
            public String renderNullSafe(Object object) {
                return object.toString();
            }
        }, new ModeSwitchingVisibilityRenderer());

        timeZoneEditor = new ListModelListBoxEditor<Object>(new NullSafeRenderer<Object>() {
            @Override
            public String renderNullSafe(Object object) {
                TimeZoneModel timeZone = (TimeZoneModel) object;
                if (timeZone.isDefault()) {
                    return messages.defaultTimeZoneCaption(timeZone.getDisplayValue());
                } else {
                    return timeZone.getDisplayValue();
                }
            }
        }, new ModeSwitchingVisibilityRenderer());

        // Console tab
        displayProtocolEditor = new ListModelListBoxEditor<Object>(new NullSafeRenderer<Object>() {
            @Override
            public String renderNullSafe(Object object) {
                return ((EntityModel) object).getTitle();
            }
        }, new ModeSwitchingVisibilityRenderer());

        usbSupportEditor =
                new ListModelListBoxEditor<Object>(new EnumRenderer(), new ModeSwitchingVisibilityRenderer());
        numOfMonitorsEditor = new ListModelListBoxEditor<Object>(new NullSafeRenderer<Object>() {
            @Override
            public String renderNullSafe(Object object) {
                return object.toString();
            }
        }, new ModeSwitchingVisibilityRenderer());

        vncKeyboardLayoutEditor = new ListModelListBoxEditor<Object>(new AbstractRenderer<Object>() {

            final String globalLayout =
                    (String) AsyncDataProvider.getConfigValuePreConverted(ConfigurationValues.VncKeyboardLayout);

            @Override
            public String render(Object object) {
                if (object == null) {
                    return messages.globalVncKeyboardLayoutCaption(globalLayout);
                } else {
                    return object.toString();
                }
            };
        }, new ModeSwitchingVisibilityRenderer());

        // Host Tab
        specificHost = new RadioButton("runVmOnHostGroup"); //$NON-NLS-1$
        isAutoAssignEditor =
                new EntityModelRadioButtonEditor("runVmOnHostGroup", new ModeSwitchingVisibilityRenderer()); //$NON-NLS-1$
        defaultHostEditor = new ListModelListBoxEditor<Object>(new NullSafeRenderer<Object>() {
            @Override
            public String renderNullSafe(Object object) {
                return ((VDS) object).getName();
            }
        }, new ModeSwitchingVisibilityRenderer());

        migrationModeEditor =
                new ListModelListBoxEditor<Object>(new EnumRenderer(), new ModeSwitchingVisibilityRenderer());

        // Resource Allocation
        provisioningThinEditor =
                new EntityModelRadioButtonEditor("provisioningGroup", new ModeSwitchingVisibilityRenderer()); //$NON-NLS-1$
        provisioningCloneEditor =
                new EntityModelRadioButtonEditor("provisioningGroup", new ModeSwitchingVisibilityRenderer()); //$NON-NLS-1$

        // Boot Options Tab
        firstBootDeviceEditor = new ListModelListBoxEditor<Object>(new NullSafeRenderer<Object>() {
            @Override
            public String renderNullSafe(Object object) {
                return ((EntityModel) object).getTitle();
            }
        }, new ModeSwitchingVisibilityRenderer());

        secondBootDeviceEditor = new ListModelListBoxEditor<Object>(new NullSafeRenderer<Object>() {
            @Override
            public String renderNullSafe(Object object) {
                return ((EntityModel) object).getTitle();
            }
        }, new ModeSwitchingVisibilityRenderer());

        cdImageEditor = new ListModelListBoxEditor<Object>(new NullSafeRenderer<Object>() {
            @Override
            public String renderNullSafe(Object object) {
                return (String) object;
            }
        }, new ModeSwitchingVisibilityRenderer());

        cpuSharesAmountSelectionEditor =
                new ListModelListBoxOnlyEditor<Object>(new EnumRenderer(), new ModeSwitchingVisibilityRenderer());
    }

    private String typeAheadNameDescriptionTemplateNullSafe(String name, String description) {
        return applicationTemplates.typeAheadNameDescription(
                name != null ? name : "",
                description != null ? description : "")
                .asString();
    }

    protected void localize(CommonApplicationConstants constants) {
        // Tabs
        highAvailabilityTab.setLabel(constants.highAvailVmPopup());
        resourceAllocationTab.setLabel(constants.resourceAllocVmPopup());
        bootOptionsTab.setLabel(constants.bootOptionsVmPopup());
        customPropertiesTab.setLabel(constants.customPropsVmPopup());
        systemTab.setLabel(constants.systemVmPopup());

        // General Tab
        generalTab.setLabel(constants.GeneralVmPopup());
        dataCenterWithClusterEditor.setLabel(constants.hostClusterVmPopup());
        quotaEditor.setLabel(constants.quotaVmPopup());
        nameLabel.setText(constants.nameVmPopup());
        descriptionEditor.setLabel(constants.descriptionVmPopup());
        commentEditor.setLabel(constants.commentLabel());
        templateEditor.setLabel(constants.basedOnTemplateVmPopup());

        oSTypeEditor.setLabel(constants.osVmPopup());
        vmTypeEditor.setLabel(constants.optimizedFor());
        isStatelessEditor.setLabel(constants.statelessVmPopup());
        isRunAndPauseEditor.setLabel(constants.runAndPauseVmPopup());
        isDeleteProtectedEditor.setLabel(constants.deleteProtectionPopup());
        isConsoleDeviceEnabledEditor.setLabel(constants.consoleDeviceEnabled());
        copyTemplatePermissionsEditor.setLabel(constants.copyTemplatePermissions());
        isSmartcardEnabledEditor.setLabel(constants.smartcardVmPopup());
        isMemoryBalloonDeviceEnabled.setLabel(constants.memoryBalloonDeviceEnabled());
        isVirtioScsiEnabled.setLabel(constants.isVirtioScsiEnabled());

        // Pools Tab
        poolTab.setLabel(constants.poolVmPopup());
        poolTypeEditor.setLabel(constants.poolTypeVmPopup());
        editPrestartedVmsLabel.setText(constants.prestartedVms());

        prestartedLabel.setText(constants.prestartedPoolPopup());
        numOfVmsEditor.setLabel(constants.numOfVmsPoolPopup());
        maxAssignedVmsPerUserEditor.setLabel(constants.maxAssignedVmsPerUser());
        editMaxAssignedVmsPerUserEditor.setLabel(constants.maxAssignedVmsPerUser());

        // initial run Tab
        initialRunTab.setLabel(constants.initialRunVmPopup());
        domainEditor.setLabel(constants.domainVmPopup());
        timeZoneEditor.setLabel(constants.tzVmPopup());

        // Console Tab
        consoleTab.setLabel(constants.consoleVmPopup());
        displayProtocolEditor.setLabel(constants.protocolVmPopup());
        vncKeyboardLayoutEditor.setLabel(constants.vncKeyboardLayoutVmPopup());
        usbSupportEditor.setLabel(constants.usbPolicyVmPopup());
        numOfMonitorsEditor.setLabel(constants.monitorsVmPopup());
        allowConsoleReconnectEditor.setLabel(constants.allowConsoleReconnect());

        // Host Tab
        hostTab.setLabel(constants.hostVmPopup());
        isAutoAssignEditor.setLabel(constants.anyHostInClusterVmPopup());
        // specificHostEditor.setLabel("Specific");
        hostCpuEditor.setLabel(constants.useHostCpu());
        cpuPinning.setLabel(constants.cpuPinningLabel());

        // High Availability Tab
        isHighlyAvailableEditor.setLabel(constants.highlyAvailableVmPopup());

        // watchdog
        watchdogActionEditor.setLabel(constants.watchdogAction());
        watchdogModelEditor.setLabel(constants.watchdogModel());

        // Resource Allocation Tab
        provisioningEditor.setLabel(constants.templateProvisVmPopup());
        provisioningThinEditor.setLabel(constants.thinVmPopup());
        provisioningCloneEditor.setLabel(constants.cloneVmPopup());
        minAllocatedMemoryEditor.setLabel(constants.physMemGuarVmPopup());

        // Boot Options
        firstBootDeviceEditor.setLabel(constants.firstDeviceVmPopup());
        secondBootDeviceEditor.setLabel(constants.secondDeviceVmPopup());
        kernel_pathEditor.setLabel(constants.kernelPathVmPopup());
        initrd_pathEditor.setLabel(constants.initrdPathVmPopup());
        kernel_parametersEditor.setLabel(constants.kernelParamsVmPopup());

        // System tab
        memSizeEditor.setLabel(constants.memSizeVmPopup());
        totalvCPUsEditor.setLabel(constants.numOfVCPUs());
        corePerSocketEditor.setLabel(constants.coresPerSocket());
        numOfSocketsEditor.setLabel(constants.numOfSockets());
        isSoundcardEnabledEditor.setLabel(constants.soundcardEnabled());
        isSingleQxlEnabledEditor.setLabel(constants.singleQxlEnabled());
    }

    protected void applyStyles() {
        hostCpuEditor.addContentWidgetStyleName(style.longCheckboxContent());
        allowConsoleReconnectEditor.addContentWidgetStyleName(style.longCheckboxContent());
        provisioningEditor.addContentWidgetStyleName(style.provisioningEditorContent());
        provisioningThinEditor.addContentWidgetStyleName(style.provisioningRadioContent());
        provisioningCloneEditor.addContentWidgetStyleName(style.provisioningRadioContent());
        cdAttachedEditor.addContentWidgetStyleName(style.cdAttachedLabelWidth());
        cdImageEditor.addContentWidgetStyleName(style.cdImageEditor());
        numOfMonitorsEditor.addContentWidgetStyleName(style.monitorsStyles());
        numOfMonitorsEditor.setStyleName(style.monitorsStyles());
        numOfMonitorsEditor.hideLabel();
        migrationModeEditor.addContentWidgetStyleName(style.migrationSelectorInner());
        isVirtioScsiEnabled.addContentWidgetStyleName(style.isVirtioScsiEnabledEditor());
    }

    @Override
    public void edit(UnitVmModel model) {
        super.edit(model);
        unitVmModel = model;

        priorityEditor.setRowData(new ArrayList<EntityModel>());
        priorityEditor.asEditor().edit(model.getPriority());
        driver.edit(model);
        initTabAvailabilityListeners(model);
        initListeners(model);
        initCustomPropertySheet(model);
        hideAlwaysHiddenFields();
    }

    @UiHandler("refreshButton")
    void handleRefreshButtonClick(ClickEvent event) {
        unitVmModel.getBehavior().refreshCdImages();
    }

    private void initCustomPropertySheet(final UnitVmModel object) {
        object.getCustomPropertySheet().getKeyValueLines().getItemsChangedEvent().addListener(new IEventListener() {

            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                customPropertiesSheetEditor.edit(object.getCustomPropertySheet());
            }
        });
    }

    protected void setupHostTabAvailability(UnitVmModel model) {
        changeApplicationLevelVisibility(hostTab, model.getIsHostAvailable());
    }

    protected void setupCustomPropertiesAvailability(UnitVmModel model) {
        changeApplicationLevelVisibility(customPropertiesTab, model.getIsCustomPropertiesTabAvailable());
    }

    protected void initListeners(final UnitVmModel object) {
        // TODO should be handled by the core framework
        object.getPropertyChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                String propName = ((PropertyChangedEventArgs) args).PropertyName;
                if ("IsHostAvailable".equals(propName)) { //$NON-NLS-1$
                    setupHostTabAvailability(object);
                } else if ("IsHostTabValid".equals(propName)) { //$NON-NLS-1$
                    if (object.getIsHostTabValid()) {
                        hostTab.markAsValid();
                    } else {
                        hostTab.markAsInvalid(null);
                    }
                } else if ("IsCustomPropertiesTabAvailable".equals(propName)) { //$NON-NLS-1$
                    setupCustomPropertiesAvailability(object);
                } else if ("IsDisksAvailable".equals(propName)) { //$NON-NLS-1$
                    addDiskAllocation(object);
                }
            }
        });

        object.getIsAutoAssign().getPropertyChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                boolean isAutoAssign = (Boolean) object.getIsAutoAssign().getEntity();
                defaultHostEditor.setEnabled(!isAutoAssign);

                // only this is not bind to the model, so needs to listen to the change explicitly
                specificHost.setValue(!isAutoAssign);
            }
        });

        // only for non local storage available
        setupHostTabAvailability(object);

        object.getProvisioning().getPropertyChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                boolean isProvisioningChangable = object.getProvisioning().getIsChangable();
                provisioningThinEditor.setEnabled(isProvisioningChangable);
                provisioningCloneEditor.setEnabled(isProvisioningChangable);

                boolean isProvisioningAvailable = object.getProvisioning().getIsAvailable();
                changeApplicationLevelVisibility(provisionSelectionPanel, isProvisioningAvailable);

                boolean isDisksAvailable = object.getIsDisksAvailable();
                changeApplicationLevelVisibility(disksAllocationPanel, isDisksAvailable);

                changeApplicationLevelVisibility(storageAllocationPanel, isProvisioningAvailable || isDisksAvailable ||
                    (Boolean) object.getIsVirtioScsiEnabled().getIsAvailable());
            }
        });

        object.getIsVirtioScsiEnabled().getPropertyChangedEvent().addListener(new IEventListener() {

            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                PropertyChangedEventArgs e = (PropertyChangedEventArgs) args;

                if (e.PropertyName == "IsAvailable") { //$NON-NLS-1$
                    isVirtioScsiEnabledInfoIcon.setVisible(object.getIsVirtioScsiEnabled().getIsAvailable());
                }
            }
        });

        object.getUsbPolicy().getPropertyChangedEvent().addListener(new IEventListener() {

            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                PropertyChangedEventArgs e = (PropertyChangedEventArgs) args;

                if (e.PropertyName == "SelectedItem") { //$NON-NLS-1$
                    updateUsbNativeMessageVisibility(object);
                }
            }
        });

        updateUsbNativeMessageVisibility(object);

        object.getEditingEnabled().getEntityChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                Boolean enabled = (Boolean) object.getEditingEnabled().getEntity();
                if (Boolean.FALSE.equals(enabled)) {
                    disableAllTabs();
                    generalWarningMessage.setText(constants.notAvailableWithNoUpDC());
                }
            }
        });

        object.getCpuSharesAmountSelection().getPropertyChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                if ("IsAvailable".equals(((PropertyChangedEventArgs) args).PropertyName)) { //$NON-NLS-1$
                    changeApplicationLevelVisibility(cpuSharesEditor, object.getCpuSharesAmountSelection().getIsAvailable());
                }
            }
        });

        profilesInstanceTypeEditor.edit(object.getNicsWithLogicalNetworks(), object.getVnicProfiles().getItems());
        object.getNicsWithLogicalNetworks().getItemsChangedEvent().addListener(new IEventListener() {

            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                profilesInstanceTypeEditor.edit(object.getNicsWithLogicalNetworks(), object.getVnicProfiles()
                        .getItems());
            }
        });
    }

    /**
     * This raises a warning for USB devices that won't persist a VM migration when using Native USB with SPICE in
     * certain, configurable cluster version.
     */
    protected void updateUsbNativeMessageVisibility(final UnitVmModel object) {
        VDSGroup vdsGroup = object.getSelectedCluster();
        changeApplicationLevelVisibility(nativeUsbWarningMessage,
                object.getUsbPolicy().getSelectedItem() == UsbPolicy.ENABLED_NATIVE
                        && vdsGroup != null
                        && vdsGroup.getcompatibility_version() != null
                        && !(Boolean) AsyncDataProvider.getConfigValuePreConverted(ConfigurationValues.MigrationSupportForNativeUsb,
                                vdsGroup.getcompatibility_version().getValue()));
    }

    private void addDiskAllocation(UnitVmModel model) {
        if (!model.getIsDisksAvailable()) {
            return;
        }
        disksAllocationView.edit(model.getDisksAllocationModel());
        model.getDisksAllocationModel().setDisks(model.getDisks());
    }

    private void initTabAvailabilityListeners(final UnitVmModel vm) {
        // TODO should be handled by the core framework
        vm.getPropertyChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                String propName = ((PropertyChangedEventArgs) args).PropertyName;
                if ("IsWindowsOS".equals(propName)) { //$NON-NLS-1$
                    domainEditor.setEnabled(vm.getIsWindowsOS());
                } else if ("IsGeneralTabValid".equals(propName)) { //$NON-NLS-1$
                    if (vm.getIsGeneralTabValid()) {
                        generalTab.markAsValid();
                    } else {
                        generalTab.markAsInvalid(null);
                    }
                } else if ("IsDisplayTabValid".equals(propName)) { //$NON-NLS-1$
                    if (vm.getIsDisplayTabValid()) {
                        consoleTab.markAsValid();
                    } else {
                        consoleTab.markAsInvalid(null);
                    }
                } else if ("IsAllocationTabValid".equals(propName)) { //$NON-NLS-1$
                    if (vm.getIsAllocationTabValid()) {
                        resourceAllocationTab.markAsValid();
                    } else {
                        resourceAllocationTab.markAsInvalid(null);
                    }
                } else if ("IsHighlyAvailable".equals(propName)) { //$NON-NLS-1$
                    changeApplicationLevelVisibility(highAvailabilityTab, (Boolean) vm.getIsHighlyAvailable()
                            .getEntity());
                } else if ("IsBootSequenceTabValid".equals(propName)) { //$NON-NLS-1$
                    if ((Boolean) vm.getIsHighlyAvailable().getEntity()) {
                        bootOptionsTab.markAsValid();
                    } else {
                        bootOptionsTab.markAsInvalid(null);
                    }
                } else if ("IsCustomPropertiesTabValid".equals(propName)) { //$NON-NLS-1$
                    if (vm.getIsCustomPropertiesTabValid()) {
                        customPropertiesTab.markAsValid();
                    } else {
                        customPropertiesTab.markAsInvalid(null);
                    }
                }
                else if ("IsDisksAvailable".equals(propName)) { //$NON-NLS-1$
                    boolean isDisksAvailable = vm.getIsDisksAvailable();
                    changeApplicationLevelVisibility(disksAllocationPanel, isDisksAvailable);

                    boolean isProvisioningAvailable = vm.getProvisioning().getIsAvailable();
                    changeApplicationLevelVisibility(storageAllocationPanel, isProvisioningAvailable
                            || isDisksAvailable || (Boolean) vm.getIsVirtioScsiEnabled().getIsAvailable());

                    if (isDisksAvailable) {
                        // Update warning message by disks status
                        updateDisksWarningByImageStatus(vm.getDisks(), ImageStatus.ILLEGAL);
                        updateDisksWarningByImageStatus(vm.getDisks(), ImageStatus.LOCKED);
                    }
                    else {
                        // Clear warning message
                        generalWarningMessage.setText(""); //$NON-NLS-1$
                    }
                }
            }
        });

        // TODO: Move to a more appropriate method
        vm.getPropertyChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                String propName = ((PropertyChangedEventArgs) args).PropertyName;
                if ("IsLinuxOS".equals(propName)) { //$NON-NLS-1$
                    changeApplicationLevelVisibility(linuxBootOptionsPanel, vm.getIsLinuxOS());
                }
            }
        });

        defaultHostEditor.setEnabled(false);
        specificHost.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> event) {
                defaultHostEditor.setEnabled(specificHost.getValue());
                ValueChangeEvent.fire(isAutoAssignEditor.asRadioButton(), false);
            }
        });

        // TODO: This is a hack and should be handled cleanly via model property availability
        isAutoAssignEditor.addDomHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                defaultHostEditor.setEnabled(false);
            }
        }, ClickEvent.getType());

        vm.getIsAutoAssign().getEntityChangedEvent().addListener(new IEventListener() {

            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                if (!isAutoAssignEditor.asRadioButton().getValue())
                    specificHost.setValue(true, true);
            }
        });
    }

    private void updateDisksWarningByImageStatus(List<DiskModel> disks, ImageStatus imageStatus) {
        ArrayList<String> disksAliases =
                getDisksAliasesByImageStatus(disks, imageStatus);

        if (!disksAliases.isEmpty()) {
            generalWarningMessage.setText(messages.disksStatusWarning(
                    EnumTranslator.createAndTranslate(imageStatus),
                    (StringUtils.join(disksAliases, ", ")))); //$NON-NLS-1$
        }
    }

    private ArrayList<String> getDisksAliasesByImageStatus(List<DiskModel> disks, ImageStatus status) {
        ArrayList<String> disksAliases = new ArrayList<String>();

        if (disks == null) {
            return disksAliases;
        }

        for (DiskModel diskModel : disks) {
            Disk disk = diskModel.getDisk();
            if (disk.getDiskStorageType() == DiskStorageType.IMAGE &&
                    ((DiskImage) disk).getImageStatus() == status) {

                disksAliases.add(disk.getDiskAlias());
            }
        }

        return disksAliases;
    }

    @Override
    public UnitVmModel flush() {
        priorityEditor.flush();
        profilesInstanceTypeEditor.flush();
        return driver.flush();
    }

    @Override
    public void focusInput() {
        nameEditor.setFocus(true);
    }

    public interface ButtonCellTableResources extends CellTable.Resources {
        interface TableStyle extends CellTable.Style {
        }

        @Override
        @Source({ CellTable.Style.DEFAULT_CSS, "org/ovirt/engine/ui/common/css/ButtonCellTable.css" })
        TableStyle cellTableStyle();
    }

    @Override
    public int setTabIndexes(int nextTabIndex) {
        // ==General Tab==
        nextTabIndex = generalTab.setTabIndexes(nextTabIndex);
        quotaEditor.setTabIndex(nextTabIndex++);
        oSTypeEditor.setTabIndex(nextTabIndex++);
        templateEditor.setTabIndex(nextTabIndex++);

        nameEditor.setTabIndex(nextTabIndex++);
        descriptionEditor.setTabIndex(nextTabIndex++);
        commentEditor.setTabIndex(nextTabIndex++);
        isStatelessEditor.setTabIndex(nextTabIndex++);
        isRunAndPauseEditor.setTabIndex(nextTabIndex++);
        isDeleteProtectedEditor.setTabIndex(nextTabIndex++);
        copyTemplatePermissionsEditor.setTabIndex(nextTabIndex++);

        numOfVmsEditor.setTabIndex(nextTabIndex++);
        prestartedVmsEditor.setTabIndex(nextTabIndex++);
        editPrestartedVmsEditor.setTabIndex(nextTabIndex++);
        incraseNumOfVmsEditor.setTabIndex(nextTabIndex++);
        maxAssignedVmsPerUserEditor.setTabIndex(nextTabIndex++);
        editMaxAssignedVmsPerUserEditor.setTabIndex(nextTabIndex++);

        // ==System Tab==
        nextTabIndex = systemTab.setTabIndexes(nextTabIndex);
        memSizeEditor.setTabIndex(nextTabIndex++);
        totalvCPUsEditor.setTabIndex(nextTabIndex++);

        nextTabIndex = vcpusAdvancedParameterExpander.setTabIndexes(nextTabIndex);
        corePerSocketEditor.setTabIndex(nextTabIndex++);
        numOfSocketsEditor.setTabIndex(nextTabIndex++);

        // == Pools ==
        nextTabIndex = poolTab.setTabIndexes(nextTabIndex);
        poolTypeEditor.setTabIndex(nextTabIndex++);

        // ==Initial run Tab==
        nextTabIndex = initialRunTab.setTabIndexes(nextTabIndex);
        timeZoneEditor.setTabIndex(nextTabIndex++);
        domainEditor.setTabIndex(nextTabIndex++);

        // ==Console Tab==
        nextTabIndex = consoleTab.setTabIndexes(nextTabIndex);
        displayProtocolEditor.setTabIndex(nextTabIndex++);
        vncKeyboardLayoutEditor.setTabIndex(nextTabIndex++);
        usbSupportEditor.setTabIndex(nextTabIndex++);
        isSingleQxlEnabledEditor.setTabIndex(nextTabIndex++);
        numOfMonitorsEditor.setTabIndex(nextTabIndex++);
        isSmartcardEnabledEditor.setTabIndex(nextTabIndex++);
        nextTabIndex = expander.setTabIndexes(nextTabIndex);
        allowConsoleReconnectEditor.setTabIndex(nextTabIndex++);
        isSoundcardEnabledEditor.setTabIndex(nextTabIndex++);
        isConsoleDeviceEnabledEditor.setTabIndex(nextTabIndex++);

        // ==Host Tab==
        nextTabIndex = hostTab.setTabIndexes(nextTabIndex);
        isAutoAssignEditor.setTabIndex(nextTabIndex++);
        specificHost.setTabIndex(nextTabIndex++);
        defaultHostEditor.setTabIndex(nextTabIndex++);
        migrationModeEditor.setTabIndex(nextTabIndex++);
        hostCpuEditor.setTabIndex(nextTabIndex++);

        // ==High Availability Tab==
        nextTabIndex = highAvailabilityTab.setTabIndexes(nextTabIndex);
        isHighlyAvailableEditor.setTabIndex(nextTabIndex++);
        priorityEditor.setTabIndex(nextTabIndex++);

        watchdogModelEditor.setTabIndex(nextTabIndex++);
        watchdogActionEditor.setTabIndex(nextTabIndex++);

        // ==Resource Allocation Tab==
        nextTabIndex = resourceAllocationTab.setTabIndexes(nextTabIndex);
        minAllocatedMemoryEditor.setTabIndex(nextTabIndex++);
        provisioningEditor.setTabIndex(nextTabIndex++);
        provisioningThinEditor.setTabIndex(nextTabIndex++);
        provisioningCloneEditor.setTabIndex(nextTabIndex++);
        cpuPinning.setTabIndex(nextTabIndex++);
        cpuSharesAmountEditor.setTabIndex(nextTabIndex++);
        nextTabIndex = disksAllocationView.setTabIndexes(nextTabIndex);

        // ==Boot Options Tab==
        nextTabIndex = bootOptionsTab.setTabIndexes(nextTabIndex);
        firstBootDeviceEditor.setTabIndex(nextTabIndex++);
        secondBootDeviceEditor.setTabIndex(nextTabIndex++);
        cdAttachedEditor.setTabIndex(nextTabIndex++);
        cdImageEditor.setTabIndex(nextTabIndex++);
        kernel_pathEditor.setTabIndex(nextTabIndex++);
        initrd_pathEditor.setTabIndex(nextTabIndex++);
        kernel_parametersEditor.setTabIndex(nextTabIndex++);

        // ==Custom Properties Tab==
        nextTabIndex = customPropertiesTab.setTabIndexes(nextTabIndex);

        return nextTabIndex;
    }

    @Override
    protected PopupWidgetConfigMap createWidgetConfiguration() {
        return super.createWidgetConfiguration().
                putAll(allTabs(), simpleField().visibleInAdvancedModeOnly()).
                putAll(adancedFieldsFromGeneralTab(), simpleField().visibleInAdvancedModeOnly()).
                putAll(consoleTabWidgets(), simpleField().visibleInAdvancedModeOnly()).
                update(consoleTab, simpleField()).
                update(numOfMonitorsEditor, simpleField()).
                update(isSingleQxlEnabledEditor, simpleField()).
                putOne(isSoundcardEnabledEditor, simpleField().visibleInAdvancedModeOnly()).
                putOne(isConsoleDeviceEnabledEditor, simpleField().visibleInAdvancedModeOnly());
    }

    protected List<Widget> consoleTabWidgets() {
        return Arrays.<Widget> asList(
                displayProtocolEditor,
                usbSupportEditor,
                isSmartcardEnabledEditor,
                nativeUsbWarningMessage,
                expander,
                numOfMonitorsEditor,
                vncKeyboardLayoutEditor
        );
    }

    protected List<Widget> poolSpecificFields() {
        return Arrays.<Widget> asList(numOfVmsEditor,
                newPoolEditVmsPanel,
                editPoolEditVmsPanel,
                editPoolIncraseNumOfVmsPanel,
                poolTab,
                prestartedVmsEditor,
                poolNameIcon,
                newPoolEditMaxAssignedVmsPerUserPanel,
                editPoolEditMaxAssignedVmsPerUserPanel);
    }

    protected List<Widget> allTabs() {
        return Arrays.<Widget> asList(initialRunTab,
                consoleTab,
                hostTab,
                resourceAllocationTab,
                bootOptionsTab,
                customPropertiesTab,
                highAvailabilityTab,
                poolTab,
                systemTab);
    }

    protected List<Widget> adancedFieldsFromGeneralTab() {
        return Arrays.<Widget> asList(
                memSizeEditor,
                totalvCPUsEditor,
                vcpusAdvancedParameterExpander,
                copyTemplatePermissionsEditor
                );
    }

    protected void disableAllTabs() {
        generalTab.disableContent();
        poolTab.disableContent();
        initialRunTab.disableContent();
        consoleTab.disableContent();
        hostTab.disableContent();
        highAvailabilityTab.disableContent();
        resourceAllocationTab.disableContent();
        bootOptionsTab.disableContent();
        customPropertiesTab.disableContent();
        systemTab.disableContent();
        oSTypeEditor.setEnabled(false);
        quotaEditor.setEnabled(false);
        dataCenterWithClusterEditor.setEnabled(false);
        templateEditor.setEnabled(false);
        vmTypeEditor.setEnabled(false);
    }

}

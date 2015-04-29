package edu.ualberta.med.biobank.dialogs;

import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnap.commons.i18n.I18n;
import org.xnap.commons.i18n.I18nFactory;

import edu.ualberta.med.biobank.common.util.StringUtil;
import edu.ualberta.med.biobank.gui.common.validators.NonEmptyStringValidator;
import edu.ualberta.med.biobank.gui.common.widgets.BgcBaseText;
import edu.ualberta.med.biobank.gui.common.widgets.utils.ComboSelectionUpdate;
import edu.ualberta.med.biobank.widgets.BiobankLabelProvider;

public class ScanAssignDialog extends ScanLinkDialog {

    private static final I18n i18n = I18nFactory.getI18n(ScanLinkDialog.class);

    private static Logger log = LoggerFactory.getLogger(ScanLinkDialog.class);

    @SuppressWarnings("nls")
    private static final String SCAN_ASSIGN_DIALOG_SETTINGS =
        ScanAssignDialog.class.getSimpleName() + "_SETTINGS";

    @SuppressWarnings("nls")
    private static final String TITLE = i18n.tr("Scan assign");

    IObservableValue palletBarcode = new WritableValue(StringUtil.EMPTY_STRING, String.class);

    IObservableValue palletLabel = new WritableValue(StringUtil.EMPTY_STRING, String.class);

    private ComboViewer containerType;

    public ScanAssignDialog(Shell parentShell, org.apache.log4j.Logger activityLogger) {
        super(parentShell, activityLogger);
    }

    @Override
    protected IDialogSettings getDialogSettings() {
        IDialogSettings settings = super.getDialogSettings();
        IDialogSettings section = settings.getSection(SCAN_ASSIGN_DIALOG_SETTINGS);
        if (section == null) {
            section = settings.addNewSection(SCAN_ASSIGN_DIALOG_SETTINGS);
        }
        return section;
    }

    @Override
    protected String getTitleAreaTitle() {
        return TITLE;
    }

    @Override
    protected String getDialogShellTitle() {
        return TITLE;
    }

    @SuppressWarnings("nls")
    @Override
    protected void createControlWidgets(Composite contents) {
        super.createControlWidgets(contents);

        BgcBaseText palletBarcodeText = (BgcBaseText) widgetCreator.createBoundWidgetWithLabel(
            contents,
            BgcBaseText.class,
            SWT.NONE,
            // TR: label
            i18n.tr("Pallet product barcode"),
            new String[0],
            palletBarcode,
            // TR: validation error message
            new NonEmptyStringValidator(i18n.tr("Enter the pallet's product barcode")));

        GridData gridData = new GridData();
        gridData.grabExcessHorizontalSpace = true;
        gridData.horizontalAlignment = SWT.FILL;
        gridData.horizontalSpan = 2;
        palletBarcodeText.setLayoutData(gridData);

        palletBarcodeText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                palletBarcodeTextModified();
            }
        });

        BgcBaseText palletLabelText = (BgcBaseText) widgetCreator.createBoundWidgetWithLabel(
            contents,
            BgcBaseText.class,
            SWT.NONE,
            // TR: label
            i18n.tr("Pallet label"),
            new String[0],
            palletLabel,
            // TR: validation error message
            new NonEmptyStringValidator(i18n.tr("Enter the pallet's label")));

        gridData = new GridData();
        gridData.grabExcessHorizontalSpace = true;
        gridData.horizontalAlignment = SWT.FILL;
        gridData.horizontalSpan = 2;
        palletLabelText.setLayoutData(gridData);

        palletLabelText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                palletLabelTextModified();
            }
        });

        containerType = widgetCreator.createComboViewer(
            contents,
            // TR: validation error message
            i18n.tr("Pallet type"),
            null,
            null,
            // TR: validation error message
            i18n.tr("Please select the pallet type"),
            false,
            null,
            new ComboSelectionUpdate() {
                @Override
                public void doSelection(Object selectedObject) {
                    // sourceSelected.setValue(selectedObject != null);
                    //
                    // Specimen spc = (Specimen) selectedObject;
                    // sourceChildTypes = spc.getSpecimenType().getChildSpecimenTypes();
                    // cvAliquots.refresh();
                }
            },
            new BiobankLabelProvider());

        gridData = new GridData();
        gridData.grabExcessHorizontalSpace = true;
        gridData.horizontalAlignment = SWT.FILL;
        gridData.horizontalSpan = 2;
        containerType.getCombo().setLayoutData(gridData);
    }

    private void palletBarcodeTextModified() {
        // if (!checkingMultipleContainerPosition) {
        // palletproductBarcodeTextModified = true;
        // palletTypesViewer.setInput(null);
        // // log.debug("clearing selections in palletTypesViewer");
        // currentMultipleContainer.setContainerType(null);
        // palletPositionText.setEnabled(true);
        // palletPositionText.setText(StringUtil.EMPTY_STRING);
    }

    protected void palletLabelTextModified() {
        // TODO Auto-generated method stub

    }
}

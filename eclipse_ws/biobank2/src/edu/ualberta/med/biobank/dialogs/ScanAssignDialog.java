package edu.ualberta.med.biobank.dialogs;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnap.commons.i18n.I18n;
import org.xnap.commons.i18n.I18nFactory;

import edu.ualberta.med.biobank.SessionManager;
import edu.ualberta.med.biobank.common.action.container.ContainerGetInfoAction;
import edu.ualberta.med.biobank.common.util.StringUtil;
import edu.ualberta.med.biobank.common.wrappers.ContainerTypeWrapper;
import edu.ualberta.med.biobank.common.wrappers.ContainerWrapper;
import edu.ualberta.med.biobank.gui.common.BgcPlugin;
import edu.ualberta.med.biobank.gui.common.validators.AbstractValidator;
import edu.ualberta.med.biobank.gui.common.validators.NonEmptyStringValidator;
import edu.ualberta.med.biobank.gui.common.widgets.BgcBaseText;
import edu.ualberta.med.biobank.helpers.ScanAssignHelper;
import edu.ualberta.med.biobank.model.Capacity;
import edu.ualberta.med.biobank.model.Container;

public class ScanAssignDialog extends ScanLinkDialog
    implements ModifyListener, FocusListener {

    private static final I18n i18n = I18nFactory.getI18n(ScanLinkDialog.class);

    private static Logger log = LoggerFactory.getLogger(ScanLinkDialog.class);

    @SuppressWarnings("nls")
    private static final String SCAN_ASSIGN_DIALOG_SETTINGS =
        ScanAssignDialog.class.getSimpleName() + "_SETTINGS";

    @SuppressWarnings("nls")
    private static final String TITLE = i18n.tr("Scan assign");

    private final org.apache.log4j.Logger activityLogger;

    private BgcBaseText palletBarcodeText;

    private BgcBaseText palletLabelText;

    private final IObservableValue palletBarcode = new WritableValue(StringUtil.EMPTY_STRING, String.class);

    private final IObservableValue palletLabel = new WritableValue(StringUtil.EMPTY_STRING, String.class);

    private PalletBarcodeValidator palletBarcodeValidator;

    private NonEmptyStringValidator palletLabelValidator;

    protected boolean palletBarcodeTextModified;

    private Boolean palletBardcodeValid = null;

    private boolean checkingPalletLabel = false;

    private boolean isNewMultipleContainer;

    private ContainerWrapper palletContainer;

    private boolean palletLabelTextModified;

    private final int validPalletRows;

    private final int validPalletCols;

    /**
     * 
     * 
     * @param parentShell
     * @param rows The number of rows allowed for the container type.
     * @param cols The number of columns allowed for the container type.
     * @param activityLogger
     */
    public ScanAssignDialog(Shell parentShell, int rows, int cols,
        org.apache.log4j.Logger activityLogger) {
        super(parentShell, activityLogger);
        this.validPalletRows = rows;
        this.validPalletCols = cols;
        this.activityLogger = activityLogger;
        this.palletContainer = new ContainerWrapper(SessionManager.getAppService());
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

        palletBarcodeValidator = new PalletBarcodeValidator();
        palletBarcodeText = (BgcBaseText) widgetCreator.createBoundWidgetWithLabel(
            contents,
            BgcBaseText.class,
            SWT.NONE,
            // TR: label
            i18n.tr("Pallet product barcode"),
            new String[0],
            palletBarcode,
            palletBarcodeValidator);

        GridData gridData = new GridData();
        gridData.grabExcessHorizontalSpace = true;
        gridData.horizontalAlignment = SWT.FILL;
        gridData.horizontalSpan = 2;

        palletBarcodeText.setLayoutData(gridData);
        palletBarcodeText.addModifyListener(this);
        palletBarcodeText.addFocusListener(this);

        palletLabelValidator = new NonEmptyStringValidator(
            // TR: validation error message
            i18n.tr("Enter the pallet's label"));
        palletLabelText = (BgcBaseText) widgetCreator.createBoundWidgetWithLabel(
            contents,
            BgcBaseText.class,
            SWT.NONE,
            // TR: label
            i18n.tr("Pallet label"),
            new String[0],
            palletLabel,
            palletLabelValidator);

        gridData = new GridData();
        gridData.grabExcessHorizontalSpace = true;
        gridData.horizontalAlignment = SWT.FILL;
        gridData.horizontalSpan = 2;
        palletLabelText.setLayoutData(gridData);

        palletLabelText.addModifyListener(this);
        palletLabelText.addFocusListener(this);
    }

    @Override
    public void modifyText(ModifyEvent e) {
        if (palletBarcodeText.isEventSource(e)) {
            palletBarcodeTextModified();
        } else if (palletLabelText.isEventSource(e)) {
            palletLabelTextModified();
        }
    }

    private void palletBarcodeTextModified() {
        if (!checkingPalletLabel) {
            palletBarcodeTextModified = true;
            palletContainer.setContainerType(null);
            palletLabelText.setEnabled(true);
            palletLabelText.setText(StringUtil.EMPTY_STRING);
        }
    }

    private void palletLabelTextModified() {
        palletLabelTextModified = true;
        palletBarcodeText.setText(StringUtil.EMPTY_STRING);
        // log.debug("clearing selections in palletTypesViewer");
        if (palletContainer == null) {
            log.info("here");
        }
        palletContainer.setContainerType(null);
    }

    @Override
    public void focusGained(FocusEvent e) {
        // do nothing
    }

    @Override
    public void focusLost(FocusEvent e) {
        if (palletBarcodeText.isEventSource(e)) {
            palletBarcodeTextFocusLost();
        } else if (palletLabelText.isEventSource(e)) {
            palletLabelTextFocusLost();
        }
    }

    private void palletBarcodeTextFocusLost() {
        if (palletBarcodeTextModified) {
            String value = (String) palletBarcode.getValue();
            if (!value.isEmpty()) {
                boolean ok = checkPalletBarcode();
                if (!ok) {
                    Display.getDefault().asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            palletBarcodeText.setFocus();
                        }
                    });
                }
            }
        }
        palletBarcodeTextModified = false;
    }

    @SuppressWarnings("nls")
    protected boolean checkPalletBarcode() {
        try {
            Container qryContainer = new Container();
            qryContainer.setProductBarcode((String) palletBarcode.getValue());

            palletBardcodeValid = null;
            List<Container> containers = SessionManager.getAppService().doAction(
                new ContainerGetInfoAction(qryContainer,
                    SessionManager.getUser().getCurrentWorkingSite().getWrappedObject())
                ).getList();

            if (containers.size() > 1) {
                throw new IllegalStateException("multiple containers found with product barcode:"
                    + qryContainer.getProductBarcode());
            } else if (containers.isEmpty()) {
                isNewMultipleContainer = true;
                return true;
            }

            palletContainer = new ContainerWrapper(SessionManager.getAppService(), containers.get(0));
            isNewMultipleContainer = false;
            Capacity capacity = palletContainer.getContainerType().getCapacity();
            palletLabelText.setText(palletContainer.getLabel());

            if ((capacity.getRowCapacity() != validPalletRows)
                || (capacity.getColCapacity() != validPalletCols)) {

                // TR: dialog message
                String msg = i18n.tr("Container dimensions are invalid for the pallet that was scanned. "
                    + "Container with product barcode \"{0}\" has {1} rows and {2} columns.",
                    qryContainer.getProductBarcode(),
                    capacity.getRowCapacity(),
                    capacity.getColCapacity());

                BgcPlugin.openError(
                    // TR: dialog title
                    i18n.tr("Invalid container"),
                    msg);
                activityLogger.trace(NLS.bind("ERROR: {0}", msg));
                return false;
            }

            if (!ScanAssignHelper.isContainerValid(
                palletContainer, palletLabelText.getText())) {
                return false;
            }

            palletBardcodeValid = true;

            activityLogger.trace(MessageFormat.format(
                "Product barcode {0} already exists at position {1} of site {2} with type {3}.",
                palletContainer.getProductBarcode(),
                palletContainer.getLabel(),
                palletContainer.getSite().getNameShort(),
                palletContainer.getContainerType().getName()));

            // can't modify the position if exists already
            palletLabelText.setEnabled(false);
        } catch (Exception ex) {
            BgcPlugin.openError(
                // TR: dialog title
                i18n.tr("Values validation"), ex);
            activityLogger.trace(NLS.bind("ERROR: {0}", ex.getMessage()));
            return false;
        }
        return true;
    }

    private void palletLabelTextFocusLost() {
        final String label = (String) palletLabel.getValue();

        if (palletLabelText.isEnabled() && palletLabelTextModified
            && palletLabelValidator.validate(label).equals(Status.OK_STATUS)) {
            BusyIndicator.showWhile(Display.getDefault(), new Runnable() {
                @SuppressWarnings("nls")
                @Override
                public void run() {
                    checkingPalletLabel = true;
                    palletContainer = ScanAssignHelper.getOrCreateContainerByLabel(label, palletContainer);

                    if (palletContainer == null) {
                        activityLogger.trace(NLS.bind(
                            "ERROR: could not get container with label {0}", palletLabel));
                        return;
                    }

                    log.info("palletContainer: label: {}, containerLabel", label, palletContainer.getLabel());
                    log.info("palletContainer: barcode: {}", palletContainer.getProductBarcode());

                    boolean ok = checkAndUpdateContainer(palletContainer, label);

                    palletContainer.setLabel(label);
                    if (!ok) {
                        BgcPlugin.focusControl(palletLabelText);
                    }
                }
            });
        }
    }

    @SuppressWarnings("nls")
    private boolean checkAndUpdateContainer(ContainerWrapper container, String palletLabel) {
        try {
            List<ContainerTypeWrapper> possibleTypes = ScanAssignHelper.getContainerTypes(container, true);

            if (!checkValidContainer(container)) return false;

            String newBarcode = container.getProductBarcode();

            log.info("checkAndUpdateContainer: container: label: {}", palletLabel);
            log.info("checkAndUpdateContainer: container: barcode: {}", newBarcode);

            if (!palletContainer.isNew()) {
                palletContainer.reset();
            }

            activityLogger.trace(ScanAssignHelper.containerProductBarcodeUpdateLogMessage(
                container, newBarcode, palletLabel));

            if ((newBarcode != null) && !newBarcode.isEmpty()) {
                palletBarcodeText.setText(newBarcode);
            }

            if (possibleTypes.isEmpty()) {
                BgcPlugin.openAsyncError(
                    // TR: dialog title
                    i18n.tr("Containers Error"),
                    // TR: dialog message
                    i18n.tr("No container type that can hold specimens has been found "
                        + "(if scanner is used, the container should be of size 8*12 or 10*10)"));
                return false;
            }
        } catch (Exception ex) {
            BgcPlugin.openError(
                // TR: dialog title
                i18n.tr("Values validation"), ex);
            activityLogger.trace(NLS.bind("ERROR: {0}", ex.getMessage()));
            return false;
        }

        return true;
    }

    @SuppressWarnings("nls")
    private boolean checkValidContainer(ContainerWrapper pallet) {
        switch (ScanAssignHelper.checkExistingContainerValid(pallet)) {
        case VALID:
        case IS_NEW:
            return true;

        case DOES_NOT_HOLD_SPECIMENS:
            BgcPlugin.openError(
                // TR: dialog title
                i18n.tr("Error"),
                // TR: dialog message
                i18n.tr("Container selected can't hold specimens"));
            return false;

        default:
            throw new IllegalArgumentException("container is invalid");
        }
    }

    private class PalletBarcodeValidator extends AbstractValidator {

        @SuppressWarnings("nls")
        public PalletBarcodeValidator() {
            // TR: validation error message
            super(i18n.tr("Enter the pallet's label"));
        }

        @Override
        public IStatus validate(Object value) {
            if (value != null) {
                if (!(value instanceof String)) {
                    throw new RuntimeException();
                }

                String strValue = (String) value;
                if (strValue.length() != 0) {
                    hideDecoration();

                    if ((palletBardcodeValid != null) && palletBardcodeValid) {
                        return Status.OK_STATUS;
                    }
                }
            }
            showDecoration();
            return ValidationStatus.error(errorMessage);
        }

    }
}

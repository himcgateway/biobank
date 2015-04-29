package edu.ualberta.med.biobank.dialogs;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnap.commons.i18n.I18n;
import org.xnap.commons.i18n.I18nFactory;

public class ScanAssignDialog extends ScanLinkDialog {

    private static final I18n i18n = I18nFactory.getI18n(ScanLinkDialog.class);

    private static Logger log = LoggerFactory.getLogger(ScanLinkDialog.class);

    @SuppressWarnings("nls")
    private static final String SCAN_ASSIGN_DIALOG_SETTINGS =
        ScanAssignDialog.class.getSimpleName() + "_SETTINGS";

    @SuppressWarnings("nls")
    private static final String TITLE = i18n.tr("Scan link");

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

    @Override
    protected void createControls(Composite parent) {
        super.createControls(parent);
    }

}

package edu.ualberta.med.biobank.common.action.patient;

import edu.ualberta.med.biobank.CommonBundle;
import edu.ualberta.med.biobank.i18n.Bundle;
import edu.ualberta.med.biobank.i18n.LString;
import edu.ualberta.med.biobank.i18n.LocalizedException;

@SuppressWarnings("nls")
public class PatientMergeException extends LocalizedException {
    private static final long serialVersionUID = 1L;
    private static final Bundle bundle = new CommonBundle();

    private static LString MESSAGE =
        bundle.tr("Problem merging patients").format();

    public PatientMergeException() {
        super(MESSAGE);
    }
}

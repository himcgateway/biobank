package edu.ualberta.med.biobank.treeview.request;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
    private static final String BUNDLE_NAME = "edu.ualberta.med.biobank.treeview.request.messages"; //$NON-NLS-1$
    public static String DispatchCenterAdapter_site_label;
    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}

package edu.ualberta.med.biobank.views;

import org.eclipse.swt.widgets.Composite;

import edu.ualberta.med.biobank.SessionManager;
import edu.ualberta.med.biobank.treeview.AbstractAdapterBase;
import edu.ualberta.med.biobank.treeview.report.AbstractReportGroup;
import edu.ualberta.med.biobank.treeview.report.PrivateReportsGroup;
import edu.ualberta.med.biobank.treeview.report.SharedReportsGroup;

public class AdvancedReportsView extends AbstractAdministrationView {
    public static final String ID = "edu.ualberta.med.biobank.views.AdvancedReportsView"; //$NON-NLS-1$

    private static AdvancedReportsView currentView;

    public AdvancedReportsView() {
        currentView = this;
        SessionManager.addView(this);
    }

    @Override
    public void createPartControl(Composite parent) {
        super.createPartControl(parent);
        createNodes();
    }

    @Override
    public void reload() {
        rootNode.removeAll();
        createNodes();

        for (AbstractAdapterBase adapter : rootNode.getChildren()) {
            adapter.rebuild();
        }

        super.reload();
    }

    public static AdvancedReportsView getCurrent() {
        return currentView;
    }

    private void createNodes() {
        if (SessionManager.getInstance().isConnected()) {
            AbstractReportGroup adapter = new PrivateReportsGroup(rootNode, 0);
            adapter.setParent(rootNode);
            adapter.setModifiable(true);
            rootNode.addChild(adapter);

            adapter = new SharedReportsGroup(rootNode, 1);
            adapter.setParent(rootNode);
            rootNode.addChild(adapter);
        }
    }

    @Override
    protected void internalSearch() {
        //
    }

    @Override
    protected String getTreeTextToolTip() {
        return null;
    }

    @Override
    public String getId() {
        return ID;
    }
}

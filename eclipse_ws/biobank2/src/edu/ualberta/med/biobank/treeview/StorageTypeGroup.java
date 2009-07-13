package edu.ualberta.med.biobank.treeview;

import java.util.Collection;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Tree;

import edu.ualberta.med.biobank.SessionManager;
import edu.ualberta.med.biobank.forms.StorageTypeEntryForm;
import edu.ualberta.med.biobank.forms.input.FormInput;
import edu.ualberta.med.biobank.model.ModelUtils;
import edu.ualberta.med.biobank.model.Site;
import edu.ualberta.med.biobank.model.StorageType;

public class StorageTypeGroup extends Node {

    public StorageTypeGroup(SiteAdapter parent, int id) {
        super(parent, id, "Storage Types", true);
    }

    @Override
    public void performDoubleClick() {
        performExpand();
    }

    @Override
    public void popupMenu(TreeViewer tv, Tree tree, Menu menu) {
        MenuItem mi = new MenuItem(menu, SWT.PUSH);
        mi.setText("Add Storage Type");
        mi.addSelectionListener(new SelectionListener() {
            public void widgetSelected(SelectionEvent event) {
                StorageTypeAdapter adapter = new StorageTypeAdapter(
                    StorageTypeGroup.this, new StorageType());
                openForm(new FormInput(adapter), StorageTypeEntryForm.ID);
            }

            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });
    }

    @Override
    public void loadChildren(boolean updateNode) {
        Site currentSite = ((SiteAdapter) getParent()).getSite();
        Assert.isNotNull(currentSite, "null site");

        try {
            // read from database again
            currentSite = (Site) ModelUtils.getObjectWithId(getAppService(),
                Site.class, currentSite.getId());
            ((SiteAdapter) getParent()).setSite(currentSite);

            Collection<StorageType> storageTypes = currentSite
                .getStorageTypeCollection();
            SessionManager.getLogger().trace(
                "updateStudies: Site " + currentSite.getName() + " has "
                    + storageTypes.size() + " studies");

            for (StorageType storageType : storageTypes) {
                SessionManager.getLogger().trace(
                    "updateStudies: Storage Type " + storageType.getId() + ": "
                        + storageType.getName());

                StorageTypeAdapter node = (StorageTypeAdapter) getChild(storageType
                    .getId());

                if (node == null) {
                    node = new StorageTypeAdapter(this, storageType);
                    addChild(node);
                }
                if (updateNode) {
                    SessionManager.getInstance().getTreeViewer().update(node,
                        null);
                }
            }
        } catch (Exception e) {
            SessionManager.getLogger().error(
                "Error while loading storage type group children for site "
                    + currentSite.getName(), e);
        }
    }

    @Override
    public Node accept(NodeSearchVisitor visitor) {
        return visitor.visit(this);
    }

    @Override
    public String getTitle() {
        return null;
    }

}

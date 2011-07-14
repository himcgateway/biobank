package edu.ualberta.med.biobank.treeview.admin;

import java.util.Collection;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;

import edu.ualberta.med.biobank.common.wrappers.ContainerTypeWrapper;
import edu.ualberta.med.biobank.common.wrappers.ModelWrapper;
import edu.ualberta.med.biobank.common.wrappers.SiteWrapper;
import edu.ualberta.med.biobank.forms.ContainerTypeEntryForm;
import edu.ualberta.med.biobank.forms.ContainerTypeViewForm;
import edu.ualberta.med.biobank.treeview.AdapterBase;

public class ContainerTypeAdapter extends AdapterBase {

    public ContainerTypeAdapter(AdapterBase parent,
        ContainerTypeWrapper containerType) {
        super(parent, containerType);
    }

    public ContainerTypeWrapper getContainerType() {
        return (ContainerTypeWrapper) modelObject;
    }

    @Override
    protected String getLabelInternal() {
        ContainerTypeWrapper containerType = getContainerType();
        Assert.isNotNull(containerType, "container type is null"); //$NON-NLS-1$
        return containerType.getName();
    }

    @Override
    public String getTooltipText() {
        ContainerTypeWrapper type = getContainerType();
        if (type != null) {
            SiteWrapper site = type.getSite();
            if (site != null) {
                return site.getNameShort() + " - " //$NON-NLS-1$
                    + getTooltipText(Messages.ContainerTypeAdapter_type_label);
            }
        }
        return getTooltipText(Messages.ContainerTypeAdapter_type_label);

    }

    @Override
    public void popupMenu(TreeViewer tv, Tree tree, Menu menu) {
        addEditMenu(menu, Messages.ContainerTypeAdapter_type_label);
        addViewMenu(menu, Messages.ContainerTypeAdapter_type_label);
        addDeleteMenu(menu, Messages.ContainerTypeAdapter_type_label);
    }

    @Override
    protected String getConfirmDeleteMessage() {
        return Messages.ContainerTypeAdapter_delete_confirm_msg;
    }

    @Override
    public boolean isDeletable() {
        return internalIsDeletable();
    }

    @Override
    protected AdapterBase createChildNode() {
        return null;
    }

    @Override
    protected AdapterBase createChildNode(ModelWrapper<?> child) {
        return null;
    }

    @Override
    protected Collection<? extends ModelWrapper<?>> getWrapperChildren()
        throws Exception {
        return null;
    }

    @Override
    protected int getWrapperChildCount() throws Exception {
        return 0;
    }

    @Override
    public String getEntryFormId() {
        return ContainerTypeEntryForm.ID;
    }

    @Override
    public String getViewFormId() {
        return ContainerTypeViewForm.ID;
    }

}

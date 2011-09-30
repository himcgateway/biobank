package edu.ualberta.med.biobank.forms;

import java.util.ArrayList;
import java.util.Collection;

import org.acegisecurity.AccessDeniedException;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.Section;
import org.springframework.remoting.RemoteAccessException;
import org.springframework.remoting.RemoteConnectFailureException;

import edu.ualberta.med.biobank.BiobankPlugin;
import edu.ualberta.med.biobank.SessionManager;
import edu.ualberta.med.biobank.common.exception.BiobankException;
import edu.ualberta.med.biobank.common.formatters.NumberFormatter;
import edu.ualberta.med.biobank.common.wrappers.ModelWrapper;
import edu.ualberta.med.biobank.forms.input.FormInput;
import edu.ualberta.med.biobank.gui.common.BgcPlugin;
import edu.ualberta.med.biobank.gui.common.forms.BgcFormBase;
import edu.ualberta.med.biobank.gui.common.widgets.BgcBaseText;
import edu.ualberta.med.biobank.gui.common.widgets.InfoTableSelection;
import edu.ualberta.med.biobank.gui.common.widgets.utils.BgcWidgetCreator;
import edu.ualberta.med.biobank.server.applicationservice.exceptions.BiobankServerException;
import edu.ualberta.med.biobank.server.applicationservice.exceptions.BiobankSessionException;
import edu.ualberta.med.biobank.treeview.AbstractAdapterBase;
import edu.ualberta.med.biobank.treeview.AdapterBase;
import edu.ualberta.med.biobank.widgets.BiobankLabelProvider;
import edu.ualberta.med.biobank.widgets.utils.WidgetCreator;

/**
 * Base class for data all BioBank view and entry forms. This class is the
 * superclass for {@link BiobankEntryForm} and {@link BiobankViewForm}. Please
 * extend from these two classes instead of <code>BiobankFormBase</code>.
 * <p>
 * Form creation is called in a non-UI thread so making calls to the ORM layer
 * possible. See {@link #createFormContent()}
 */
public abstract class BiobankFormBase extends BgcFormBase {

    protected AbstractAdapterBase adapter;

    public BiobankFormBase() {
        //
    }

    @Override
    public void setFocus() {
        if ((adapter != null) && (adapter.getId() != null)) {
            SessionManager.setSelectedNode(adapter);
            // if selection fails, then the adapter needs to be matched at the
            // id level
            if (SessionManager.getSelectedNode() == null) {
                AbstractAdapterBase node = SessionManager
                    .searchFirstNode(adapter.getModelObject());
                SessionManager.setSelectedNode(node);
            }
        }
    }

    @Override
    protected BgcWidgetCreator createWidgetCreator() {
        return new WidgetCreator(widgets);
    }

    @Override
    public void doSave(IProgressMonitor monitor) {
        // default does nothing
    }

    @Override
    public void doSaveAs() {
        // default does nothing
    }

    @Override
    public void init(IEditorSite editorSite, IEditorInput input)
        throws PartInitException {
        if (!(input instanceof FormInput))
            throw new PartInitException("Invalid editor input"); //$NON-NLS-1$
        FormInput formInput = (FormInput) input;

        adapter = (AdapterBase) formInput.getAdapter(AdapterBase.class);
        if (adapter != null) {
            Assert.isNotNull(adapter, "Bad editor input (null value)"); //$NON-NLS-1$
            if (!formInput.hasPreviousForm()) {
                currentLinkedForms = new ArrayList<BgcFormBase>();
            }
            linkedForms = currentLinkedForms;
            linkedForms.add(this);
        }
        super.init(editorSite, formInput);
        getSite().setSelectionProvider(this);
    }

    protected Object getModelObject() throws Exception {
        Object o = adapter.getModelObject();
        if (o instanceof ModelWrapper) {
            ModelWrapper<?> modelObject = (ModelWrapper<?>) o;
            if (!modelObject.isNew()) {
                o = modelObject.getDatabaseClone();
            }
        }
        return o;
    }

    @Override
    protected void performDoubleClick(DoubleClickEvent event) {
        Object selection = event.getSelection();
        if (selection instanceof StructuredSelection) {
            Object element = ((StructuredSelection) selection)
                .getFirstElement();
            if (element instanceof AdapterBase) {
                ((AdapterBase) element).performDoubleClick();
            } else if (element instanceof ModelWrapper<?>) {
                SessionManager.openViewForm((ModelWrapper<?>) element);
            }
        } else if (selection instanceof InfoTableSelection) {
            InfoTableSelection tableSelection = (InfoTableSelection) selection;
            if (tableSelection.getObject() instanceof ModelWrapper<?>) {
                SessionManager.openViewForm((ModelWrapper<?>) tableSelection
                    .getObject());
            }
        }
    }

    @Override
    public boolean isDirty() {
        return false;
    }

    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }

    protected void addSectionToolbar(Section section, String tooltip,
        SelectionListener listener) {
        addSectionToolbar(section, tooltip, listener, null);
    }

    protected void addSectionToolbar(Section section, String tooltip,
        SelectionListener listener, Class<?> wrapperTypeToAdd) {
        addSectionToolbar(section, tooltip, listener, wrapperTypeToAdd, null);
    }

    protected void addSectionToolbar(Section section, String tooltip,
        SelectionListener listener, Class<?> wrapperTypeToAdd, String imageKey) {
        ((WidgetCreator) widgetCreator).addSectionToolbar(section, tooltip,
            listener, wrapperTypeToAdd, imageKey);
    }

    public AbstractAdapterBase getAdapter() {
        return adapter;
    }

    protected <T> ComboViewer createComboViewer(Composite parent,
        String fieldLabel, Collection<T> input, T selection) {
        return widgetCreator.createComboViewer(parent, fieldLabel, input,
            selection, new BiobankLabelProvider());
    }

    public static void setTextValue(BgcBaseText label, Object value) {
        if (value != null) {
            String stringValue;
            if (value instanceof Number)
                stringValue = NumberFormatter.format((Number) value);
            else
                stringValue = value.toString();
            setTextValue(label, stringValue);
        }
    }

    public static void setCheckBoxValue(Button button, Boolean value) {
        if (value != null) {
            button.setSelection(value.booleanValue());
        }
    }

    @Override
    protected Image getFormImage() {
        return BiobankPlugin.getDefault().getImage(adapter);
    }

    // implementation of ISelectionProvider

    @Override
    public ISelection getSelection() {
        if (adapter != null)
            return new StructuredSelection(adapter);
        return null;
    }

    protected void saveErrorCatch(Exception ex, IProgressMonitor monitor,
        boolean lastThrowException) {
        if (ex instanceof RemoteConnectFailureException) {
            BgcPlugin.openRemoteConnectErrorMessage(ex);
            cancelSave(monitor);
        } else if (ex instanceof RemoteAccessException) {
            BgcPlugin.openRemoteAccessErrorMessage(ex);
            cancelSave(monitor);
        } else if (ex instanceof AccessDeniedException) {
            BgcPlugin.openAccessDeniedErrorMessage(ex);
            cancelSave(monitor);
        } else if (ex instanceof BiobankException) {
            BgcPlugin.openAsyncError(Messages.BiobankFormBase_save_error_title,
                ex);
            cancelSave(monitor);
        } else if (ex instanceof BiobankServerException) {
            BgcPlugin.openAsyncError(Messages.BiobankFormBase_save_error_title,
                ex);
            cancelSave(monitor);
        } else if (ex instanceof BiobankSessionException) {
            BgcPlugin.openAsyncError(Messages.BiobankFormBase_save_error_title,
                ex);
            cancelSave(monitor);
        } else {
            cancelSave(monitor);
            if (lastThrowException)
                throw new RuntimeException(ex);
            else
                BgcPlugin.openAsyncError(
                    Messages.BiobankFormBase_save_error_title, ex);
        }
    }

    protected void cancelSave(IProgressMonitor monitor) {
        if (monitor != null)
            monitor.setCanceled(true);
    }
}

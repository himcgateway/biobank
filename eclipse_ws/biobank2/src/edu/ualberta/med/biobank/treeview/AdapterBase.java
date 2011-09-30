package edu.ualberta.med.biobank.treeview;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Semaphore;

import org.eclipse.core.runtime.Assert;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.springframework.remoting.RemoteAccessException;

import edu.ualberta.med.biobank.SessionManager;
import edu.ualberta.med.biobank.common.wrappers.ContainerWrapper;
import edu.ualberta.med.biobank.common.wrappers.ModelWrapper;
import edu.ualberta.med.biobank.forms.input.FormInput;
import edu.ualberta.med.biobank.gui.common.BgcLogger;
import edu.ualberta.med.biobank.gui.common.BgcPlugin;
import edu.ualberta.med.biobank.treeview.admin.ContainerAdapter;
import edu.ualberta.med.biobank.treeview.listeners.AdapterChangedEvent;
import edu.ualberta.med.biobank.treeview.listeners.AdapterChangedListener;
import edu.ualberta.med.biobank.treeview.util.DeltaEvent;
import edu.ualberta.med.biobank.treeview.util.IDeltaListener;
import edu.ualberta.med.biobank.treeview.util.NullDeltaListener;
import gov.nih.nci.system.applicationservice.WritableApplicationService;

/**
 * Base class for all "Session" tree view nodes. Generally, most of the nodes in
 * the tree are adapters for classes in the ORM model.
 */
public abstract class AdapterBase extends AbstractAdapterBase {

    private static BgcLogger logger = BgcLogger.getLogger(AdapterBase.class
        .getName());

    protected static final String BGR_LOADING_LABEL = Messages.AdapterBase_loading;

    protected IDeltaListener deltaListener = NullDeltaListener
        .getSoleInstance();

    private boolean loadChildrenInBackground;

    private Thread childUpdateThread;

    private Semaphore loadChildrenSemaphore;

    // FIXME can we merge this list of listeners with the DeltaListener ?
    private List<AdapterChangedListener> listeners;

    public AdapterBase(AdapterBase parent, ModelWrapper<?> object,
        boolean loadChildrenInBackground) {
        super(parent, object, null);
        this.loadChildrenInBackground = loadChildrenInBackground;
    }

    @Override
    protected void init() {
        loadChildrenSemaphore = new Semaphore(10, true);
        children = new ArrayList<AbstractAdapterBase>();
        if (getParent() != null) {
            addListener(getParent().deltaListener);
        }
        listeners = new ArrayList<AdapterChangedListener>();
    }

    public AdapterBase(AdapterBase parent, ModelWrapper<?> object) {
        this(parent, object, true);
    }

    public AdapterBase(AdapterBase parent, int id, String name,
        boolean hasChildren, boolean loadChildrenInBackground) {
        super(parent, id, name, hasChildren);
        this.loadChildrenInBackground = loadChildrenInBackground;
    }

    public ModelWrapper<?> getModelObject() {
        return (ModelWrapper<?>) super.getModelObject();
    }

    public ModelWrapper<?> getModelObjectClone() throws Exception {
        return getModelObject().getDatabaseClone();
    }

    public void setParent(AdapterBase parent) {
        this.parent = parent;
    }

    @Override
    public Integer getId() {
        if (getModelObject() != null) {
            return getModelObject().getId();
        }
        return super.getId();
    }

    /**
     * Derived classes should not override this method. Instead they should
     * implement getNameInternal().
     * 
     * @return the name for the node.
     */
    @Override
    public String getLabel() {
        if (getModelObject() != null) {
            return getLabelInternal();
        } else if (parent != null
            && ((AdapterBase) parent).loadChildrenInBackground) {
            return BGR_LOADING_LABEL;
        }
        return super.getLabel();
    }

    /**
     * Derived classses should implement this method instead of overriding
     * getName().
     * 
     * @return the name of the node. The name is the label displayed in the
     *         treeview.
     */
    protected abstract String getLabelInternal();

    @Override
    public AdapterBase getParent() {
        return (AdapterBase) parent;
    }

    @Override
    public AdapterBase getChild(Object object, boolean reloadChildren) {
        if (reloadChildren) {
            loadChildren(false);
        }
        if (children.size() == 0)
            return null;

        Class<?> wrapperClass = object.getClass();
        Integer wrapperId = ((ModelWrapper<?>) object).getId();
        for (AbstractAdapterBase child : children) {
            ModelWrapper<?> childModelObject = ((AdapterBase) child)
                .getModelObject();
            if ((childModelObject != null)
                && childModelObject.getClass().equals(wrapperClass)
                && child.getId() != null && child.getId().equals(wrapperId))
                return (AdapterBase) child;
        }
        return null;
    }

    @Override
    public AdapterBase getChild(Object object) {
        return getChild(object, false);
    }

    @Override
    public AdapterBase getChild(int id) {
        return (AdapterBase) super.getChild(id);
    }

    @Override
    public AdapterBase getChild(int id, boolean reloadChildren) {
        return (AdapterBase) super.getChild(id, reloadChildren);
    }

    @Override
    public void addChild(AbstractAdapterBase child) {
        super.addChild(child);
        ((AdapterBase) child).addListener(deltaListener);
        fireAdd(child);
    }

    @Override
    public void insertAfter(AbstractAdapterBase existingNode,
        AbstractAdapterBase newNode) {
        super.insertAfter(existingNode, newNode);
        ((AdapterBase) newNode).addListener(deltaListener);
        fireAdd(newNode);
    }

    @Override
    public void removeChild(AbstractAdapterBase item, boolean closeForm) {
        if (children.size() == 0)
            return;
        AbstractAdapterBase itemToRemove = null;
        for (AbstractAdapterBase child : children) {
            if ((child.getId() == null && item.getId() == null)
                || (child.getId().equals(item.getId()) && child.getLabel()
                    .equals(item.getLabel())))
                itemToRemove = child;
        }
        if (itemToRemove != null) {
            if (closeForm) {
                closeEditor(new FormInput(itemToRemove));
            }
            children.remove(itemToRemove);
            // override because of fireRemove
            fireRemove(itemToRemove);
        }
    }

    @Override
    public void removeAll() {
        super.removeAll();
        notifyListeners();
    }

    @Deprecated
    public WritableApplicationService getAppService() {
        if (getModelObject() != null) {
            return getModelObject().getAppService();
        }
        if (parent != null)
            return ((AdapterBase) parent).getAppService();
        return null;
    }

    public void addListener(IDeltaListener listener) {
        this.deltaListener = listener;
    }

    public void removeListener(IDeltaListener listener) {
        if (this.deltaListener.equals(listener)) {
            this.deltaListener = NullDeltaListener.getSoleInstance();
        }
    }

    protected void fireAdd(Object added) {
        deltaListener.add(new DeltaEvent(added));
    }

    protected void fireRemove(Object removed) {
        deltaListener.remove(new DeltaEvent(removed));
    }

    /**
     * Called to load it's children;
     * 
     * @param updateNode If not null, the node in the treeview to update.
     */
    @Override
    public void loadChildren(boolean updateNode) {
        try {
            loadChildrenSemaphore.acquire();
        } catch (InterruptedException e) {
            BgcPlugin.openAsyncError(Messages.AdapterBase_load_error_title, e);
        }

        if (loadChildrenInBackground) {
            loadChildrenBackground(true);
            return;
        }

        try {
            Collection<? extends ModelWrapper<?>> children = getWrapperChildren();
            if (children != null) {
                for (ModelWrapper<?> child : children) {
                    AbstractAdapterBase node = getChild(child);
                    if (node == null) {
                        node = createChildNode(child);
                        addChild(node);
                    }
                    if (updateNode) {
                        SessionManager.updateAdapterTreeNode(node);
                    }
                }
                SessionManager.refreshTreeNode(AdapterBase.this);
            }
        } catch (final RemoteAccessException exp) {
            BgcPlugin.openRemoteAccessErrorMessage(exp);
        } catch (Exception e) {
            String text = getClass().getName();
            if (getModelObject() != null) {
                text = getModelObject().toString();
            }
            logger.error("Error while loading children of node " + text, e); //$NON-NLS-1$
        } finally {
            loadChildrenSemaphore.release();
        }
    }

    @SuppressWarnings("unused")
    public void loadChildrenBackground(final boolean updateNode) {
        if ((childUpdateThread != null) && childUpdateThread.isAlive()) {
            loadChildrenSemaphore.release();
            return;
        }

        try {
            int childCount = getWrapperChildCount();
            if (childCount == 0) {
                setHasChildren(false);
            } else
                setHasChildren(true);
            final List<AbstractAdapterBase> newNodes = new ArrayList<AbstractAdapterBase>();
            for (int i = 0, n = childCount - children.size(); i < n; ++i) {
                final AbstractAdapterBase node = createChildNode(-i);
                addChild(node);
                newNodes.add(node);
            }

            childUpdateThread = new Thread() {
                @Override
                public void run() {
                    try {
                        Collection<? extends ModelWrapper<?>> childObjects = getWrapperChildren();
                        if (childObjects != null) {
                            for (ModelWrapper<?> child : childObjects) {
                                // first see if this object is among the
                                // children, if not then it is being loaded
                                // for the first time
                                AbstractAdapterBase node = getChild(child);
                                if (node == null) {
                                    Assert.isTrue(newNodes.size() > 0);
                                    node = newNodes.get(0);
                                    newNodes.remove(0);
                                }
                                Assert.isNotNull(node);
                                ((AdapterBase) node).setModelObject(child);
                                final AbstractAdapterBase nodeToUpdate = node;
                                Display.getDefault().syncExec(new Runnable() {
                                    @Override
                                    public void run() {
                                        SessionManager
                                            .refreshTreeNode(nodeToUpdate);
                                    }
                                });
                            }
                            Display.getDefault().asyncExec(new Runnable() {
                                @Override
                                public void run() {
                                    SessionManager
                                        .refreshTreeNode(AdapterBase.this);
                                }
                            });
                        }
                    } catch (final RemoteAccessException exp) {
                        BgcPlugin.openRemoteAccessErrorMessage(exp);
                    } catch (Exception e) {
                        String modelString = Messages.AdapterBase_unknow;
                        if (getModelObject() != null) {
                            modelString = getModelObject().toString();
                        }
                        logger.error("Error while loading children of node " //$NON-NLS-1$
                            + modelString + " in background", e); //$NON-NLS-1$
                    } finally {
                        loadChildrenSemaphore.release();
                    }
                }
            };
            childUpdateThread.start();
        } catch (Exception e) {
            String nodeString = "null"; //$NON-NLS-1$
            if (getModelObject() != null) {
                nodeString = getModelObject().toString();
            }
            logger.error(
                "Error while expanding children of node " + nodeString, e); //$NON-NLS-1$
            loadChildrenSemaphore.release();
        }
    }

    /**
     * get the list of this model object children that this node should have as
     * children nodes.
     * 
     * @throws Exception
     */
    protected abstract Collection<? extends ModelWrapper<?>> getWrapperChildren()
        throws Exception;

    @Override
    protected Collection<?> getChildrenObjects() throws Exception {
        return getWrapperChildren();
    }

    protected abstract int getWrapperChildCount() throws Exception;

    @Override
    protected int getChildrenCount() throws Exception {
        return getWrapperChildCount();
    }

    public static boolean closeEditor(FormInput input) {
        IWorkbenchPage page = PlatformUI.getWorkbench()
            .getActiveWorkbenchWindow().getActivePage();
        IEditorPart part = page.findEditor(input);
        if (part != null) {
            return page.closeEditor(part, true);
        }
        return false;
    }

    public static IEditorPart openForm(FormInput input, String id) {
        return openForm(input, id, true);
    }

    public static IEditorPart openForm(FormInput input, String id,
        boolean focusOnEditor) {
        closeEditor(input);
        try {
            IEditorPart part = PlatformUI.getWorkbench()
                .getActiveWorkbenchWindow().getActivePage()
                .openEditor(input, id, focusOnEditor);
            return part;
        } catch (PartInitException e) {
            logger.error("Can't open form with id " + id, e); //$NON-NLS-1$
            return null;
        }

    }

    @Override
    public void openViewForm() {
        if (getViewFormId() != null && getModelObject() != null
            && getModelObject().getWrappedObject() != null) {
            openForm(new FormInput(this), getViewFormId());
        }
    }

    @Override
    public List<AbstractAdapterBase> search(Object searchedObject) {
        if (getModelObject() != null && getModelObject().equals(searchedObject))
            return Arrays.asList(new AbstractAdapterBase[] { this });
        return new ArrayList<AbstractAdapterBase>();
    }

    public void resetObject() throws Exception {
        if (getModelObject() != null) {
            getModelObject().reset();
        }
    }

    @Override
    public void deleteWithConfirm() {
        String msg = getConfirmDeleteMessage();
        if (msg == null) {
            throw new RuntimeException("adapter has no confirm delete msg: " //$NON-NLS-1$
                + getClass().getName());
        }
        boolean doDelete = true;
        if (msg != null)
            doDelete = BgcPlugin.openConfirm(
                Messages.AdapterBase_confirm_delete_title, msg);
        if (doDelete) {
            BusyIndicator.showWhile(Display.getDefault(), new Runnable() {
                @Override
                public void run() {
                    // the order is very important
                    if (getModelObject() != null) {
                        IWorkbenchPage page = PlatformUI.getWorkbench()
                            .getActiveWorkbenchWindow().getActivePage();
                        IEditorPart part = page.findEditor(new FormInput(
                            AdapterBase.this));
                        getParent().removeChild(AdapterBase.this, false);
                        try {
                            getModelObject().delete();
                            page.closeEditor(part, true);
                        } catch (Exception e) {
                            BgcPlugin.openAsyncError(
                                Messages.AdapterBase_delete_error_title, e);
                            getParent().addChild(AdapterBase.this);
                            return;
                        }
                        getParent().rebuild();
                        getParent().notifyListeners();
                        notifyListeners();
                        additionalRefreshAfterDelete();
                    }
                }
            });
        }
    }

    @Override
    protected boolean internalIsDeletable() {
        return super.internalIsDeletable() && getModelObject() != null
            && SessionManager.canDelete(getModelObject());
    }

    @Override
    public boolean isEditable() {
        return super.isEditable() && SessionManager.canUpdate(getModelObject());
    }

    public void setLoadChildrenInBackground(boolean loadChildrenInBackground) {
        this.loadChildrenInBackground = loadChildrenInBackground;
    }

    public void addChangedListener(AdapterChangedListener listener) {
        listeners.add(listener);
    }

    public void removeChangedListener(AdapterChangedListener listener) {
        listeners.remove(listener);
    }

    public void notifyListeners(AdapterChangedEvent event) {
        for (AdapterChangedListener listener : listeners) {
            listener.changed(event);
        }
    }

    public void notifyListeners() {
        notifyListeners(new AdapterChangedEvent(this));
    }

    protected List<AbstractAdapterBase> searchChildContainers(
        Object searchedObject, ContainerAdapter container,
        final List<ContainerWrapper> parents) {
        List<AbstractAdapterBase> res = new ArrayList<AbstractAdapterBase>();
        if (parents.contains(container.getModelObject())) {
            AbstractAdapterBase child = container
                .getChild(searchedObject, true);
            if (child == null) {
                for (AbstractAdapterBase childContainer : container
                    .getChildren()) {
                    if (childContainer instanceof ContainerAdapter) {
                        res = searchChildContainers(searchedObject,
                            (ContainerAdapter) childContainer, parents);
                    } else {
                        res = childContainer.search(searchedObject);
                    }
                    if (res.size() > 0)
                        break;
                }
            } else {
                res.add(child);
            }
        }
        return res;
    }

    public RootNode getRootNode() {
        return getParentFromClass(RootNode.class);
    }

    @Override
    public void performExpand() {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                loadChildren(true);
                RootNode root = getRootNode();
                if (root != null) {
                    root.expandChild(AdapterBase.this);
                }
            }
        });
    }

}
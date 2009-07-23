package edu.ualberta.med.biobank.widgets;

import org.eclipse.jface.viewers.ILazyContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.springframework.util.Assert;

import edu.ualberta.med.biobank.model.Container;
import edu.ualberta.med.biobank.model.ContainerType;
import edu.ualberta.med.biobank.model.PvInfo;
import edu.ualberta.med.biobank.model.Sample;
import edu.ualberta.med.biobank.treeview.ClinicAdapter;
import edu.ualberta.med.biobank.treeview.ContainerTypeAdapter;
import edu.ualberta.med.biobank.treeview.PatientAdapter;
import edu.ualberta.med.biobank.treeview.PatientVisitAdapter;
import edu.ualberta.med.biobank.treeview.StudyAdapter;

public class BiobankContentProvider implements ILazyContentProvider {
    private TableViewer viewer;

    private Object[] elements;

    public BiobankContentProvider(TableViewer viewer) {
        this.viewer = viewer;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java
     * .lang.Object)
     */
    public Object[] getElements(Object inputElement) {
        if (inputElement instanceof StudyAdapter[]) {
            return (StudyAdapter[]) inputElement;
        } else if (inputElement instanceof ClinicAdapter[]) {
            return (ClinicAdapter[]) inputElement;
        } else if (inputElement instanceof PatientAdapter[]) {
            return (PatientAdapter[]) inputElement;
        } else if (inputElement instanceof PatientVisitAdapter[]) {
            return (PatientVisitAdapter[]) inputElement;
        } else if (inputElement instanceof ContainerTypeAdapter[]) {
            return (ContainerTypeAdapter[]) inputElement;
        } else if (inputElement instanceof ContainerType[]) {
            return (ContainerType[]) inputElement;
        } else if (inputElement instanceof Container[]) {
            return (Container[]) inputElement;
        } else if (inputElement instanceof PvInfo[]) {
            return (PvInfo[]) inputElement;
        } else if (inputElement instanceof Sample[]) {
            return (Sample[]) inputElement;
        }
        Assert.isTrue(false, "invalid type for inputElement: "
            + inputElement.getClass().getName());
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.viewers.IContentProvider#dispose()
     */
    public void dispose() {

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface
     * .viewers.Viewer, java.lang.Object, java.lang.Object)
     */
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        this.elements = (Object[]) newInput;
    }

    public void updateElement(int index) {
        viewer.replace(elements[index], index);
    }

}

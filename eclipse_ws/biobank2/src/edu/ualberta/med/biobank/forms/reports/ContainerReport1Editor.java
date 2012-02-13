package edu.ualberta.med.biobank.forms.reports;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Composite;

public class ContainerReport1Editor extends ReportsEditor {

    public static String ID =
        "edu.ualberta.med.biobank.editors.ContainerCapacityEditor"; //$NON-NLS-1$

    @Override
    protected void createOptionSection(Composite parent) {
        //
    }

    @Override
    protected String[] getColumnNames() {
        return new String[] { Messages.ContainerCapacityEditor_container_label,
            Messages.ContainerCapacityEditor_capacity_label,
            Messages.ContainerCapacityEditor_nberInUse_label,
            Messages.ContainerCapacityEditor_perCentInUse_label };
    }

    @Override
    protected List<String> getParamNames() {
        return new ArrayList<String>();
    }

    @Override
    protected void initReport() throws Exception {
        //
    }

    @Override
    protected List<Object> getPrintParams() throws Exception {
        return new ArrayList<Object>();
    }

    @Override
    public void setValues() throws Exception {
    }

}

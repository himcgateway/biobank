package edu.ualberta.med.biobank.forms;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.supercsv.cellprocessor.ParseDate;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.exception.SuperCSVException;
import org.supercsv.io.CsvBeanReader;
import org.supercsv.io.ICsvBeanReader;
import org.supercsv.prefs.CsvPreference;

import edu.ualberta.med.biobank.common.util.RequestSpecimenState;
import edu.ualberta.med.biobank.common.wrappers.RequestSpecimenWrapper;
import edu.ualberta.med.biobank.common.wrappers.RequestWrapper;
import edu.ualberta.med.biobank.common.wrappers.ResearchGroupWrapper;
import edu.ualberta.med.biobank.common.wrappers.SpecimenWrapper;
import edu.ualberta.med.biobank.gui.common.BgcPlugin;
import edu.ualberta.med.biobank.gui.common.widgets.BgcBaseText;
import edu.ualberta.med.biobank.treeview.admin.ResearchGroupAdapter;
import edu.ualberta.med.biobank.views.SpecimenTransitView;
import edu.ualberta.med.biobank.widgets.FileBrowser;

public class ResearchGroupViewForm extends AddressViewFormCommon {
    public static final String ID = "edu.ualberta.med.biobank.forms.ResearchGroupViewForm"; //$NON-NLS-1$

    private ResearchGroupAdapter researchGroupAdapter;

    private ResearchGroupWrapper researchGroup;

    // private ResearchGroupStudyInfoTable studiesTable;

    private BgcBaseText nameLabel;

    private BgcBaseText nameShortLabel;

    private BgcBaseText activityStatusLabel;

    private BgcBaseText commentLabel;

    private BgcBaseText studyLabel;

    private FileBrowser csvSelector;

    @Override
    protected void init() throws Exception {
        Assert.isTrue(adapter instanceof ResearchGroupAdapter,
            "Invalid editor input: object of type " //$NON-NLS-1$
                + adapter.getClass().getName());

        researchGroupAdapter = (ResearchGroupAdapter) adapter;
        researchGroup = researchGroupAdapter.getWrapper();
        researchGroup.reload();
        setPartName(NLS.bind(Messages.ResearchGroupViewForm_title,
            researchGroup.getNameShort()));
    }

    @Override
    protected void createFormContent() throws Exception {
        form.setText(NLS.bind(Messages.ResearchGroupViewForm_title,
            researchGroup.getName()));

        GridLayout layout = new GridLayout(1, false);
        page.setLayout(layout);
        page.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        createResearchGroupSection();
        createAddressSection(researchGroup);
        createUploadSection();
    }

    private void createUploadSection() {
        Composite client = createSectionWithClient("Request Upload");
        client.setLayout(new GridLayout(3, false));
        client.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        toolkit.paintBordersFor(client);
        toolkit.createLabel(client,
            "Submit a request on behalf of this research group:");
        csvSelector = new FileBrowser(client, "CSV File", SWT.NONE);
        csvSelector.adaptToToolkit(toolkit, true);
        Button b = new Button(client, SWT.PUSH);
        b.setText("Upload Request");
        b.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                try {
                    saveRequest();
                } catch (Exception e1) {
                    BgcPlugin.openAsyncError("Error Uploading", e1);
                }
            }
        });
        // b.setEnabled(false);
    }

    public void saveRequest() throws Exception {
        RequestWrapper request = new RequestWrapper(appService);

        FileReader f = new FileReader(csvSelector.getFilePath());
        int newLines = 0;
        while (f.ready() && newLines < 4) {
            char c = (char) f.read();
            if (c == '\n')
                newLines++;
        }

        ICsvBeanReader reader = new CsvBeanReader(f,
            CsvPreference.STANDARD_PREFERENCE);

        final CellProcessor[] processors = new CellProcessor[] { null, null,
            new ParseDate("yyyy-MM-dd"), null, null, null };

        List<RequestInput> requests = new ArrayList<RequestInput>();

        try {
            String[] header = new String[] { "pnumber", "inventoryID",
                "dateDrawn", "specimenTypeNameShort", "location",
                "activityStatus" };
            RequestInput srequest;
            while ((srequest = reader.read(RequestInput.class, header,
                processors)) != null) {
                requests.add(srequest);
            }
        } catch (SuperCSVException e) {
            throw new Exception("Parse error at line " + reader.getLineNumber()
                + "\n" + e.getCsvContext());
        } finally {
            reader.close();
        }

        List<RequestSpecimenWrapper> specs = new ArrayList<RequestSpecimenWrapper>();
        for (RequestInput ob : requests) {
            RequestSpecimenWrapper r = new RequestSpecimenWrapper(appService);
            r.setRequest(request);
            r.setState(RequestSpecimenState.AVAILABLE_STATE);
            SpecimenWrapper spec = SpecimenWrapper.getSpecimen(appService,
                ob.getInventoryID());
            if (spec == null)
                continue;
            r.setSpecimen(spec);
            specs.add(r);
        }
        request.addToRequestSpecimenCollection(specs);

        request.setStudy(researchGroup.getStudy());
        request.setCreated(new Date());
        request.setSubmitted(new Date());
        request.setAddress(request.getStudy().getResearchGroup().getAddress());

        request.persist();

        BgcPlugin.openMessage("Success", "Request successfully uploaded");
        SpecimenTransitView.reloadCurrent();
    }

    private void createResearchGroupSection() throws Exception {
        Composite client = toolkit.createComposite(page);
        client.setLayout(new GridLayout(2, false));
        client.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        toolkit.paintBordersFor(client);

        nameLabel = createReadOnlyLabelledField(client, SWT.NONE,
            Messages.label_name);
        nameShortLabel = createReadOnlyLabelledField(client, SWT.NONE,
            Messages.label_nameShort); //$NON-NLS-1$
        studyLabel = createReadOnlyLabelledField(client, SWT.NONE, "Study"); //$NON-NLS-1$
        activityStatusLabel = createReadOnlyLabelledField(client, SWT.NONE,
            Messages.label_activity);
        commentLabel = createReadOnlyLabelledField(client, SWT.MULTI,
            Messages.label_comments);

        setResearchGroupValues();
    }

    private void setResearchGroupValues() throws Exception {
        setTextValue(nameLabel, researchGroup.getName());
        setTextValue(nameShortLabel, researchGroup.getNameShort());
        setTextValue(studyLabel, researchGroup.getStudy());
        setTextValue(activityStatusLabel, researchGroup.getActivityStatus());
        setTextValue(commentLabel, researchGroup.getComment());
    }

    @Override
    public void reload() throws Exception {
        researchGroup.reload();
        setPartName(NLS.bind(Messages.ResearchGroupViewForm_title,
            researchGroup.getName()));
        form.setText(NLS.bind(Messages.ResearchGroupViewForm_title,
            researchGroup.getName()));
        setResearchGroupValues();
        setAddressValues(researchGroup);
    }

}
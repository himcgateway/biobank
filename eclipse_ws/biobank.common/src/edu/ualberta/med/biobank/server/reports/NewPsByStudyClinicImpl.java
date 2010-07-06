package edu.ualberta.med.biobank.server.reports;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import edu.ualberta.med.biobank.common.util.ReportOption;
import gov.nih.nci.system.applicationservice.WritableApplicationService;

public class NewPsByStudyClinicImpl extends AbstractReport {

    private static final String QUERY = "select pv.patient.study.nameShort,"
        + " pv.shipment.clinic.name, year(pv.dateProcessed), {0}(pv.dateProcessed),"
        + " count(*) from edu.ualberta.med.biobank.model.PatientVisit pv"
        + " where pv.dateProcessed=(select min(pvCollection.dateProcessed)"
        + " from edu.ualberta.med.biobank.model.Patient p join p.patientVisitCollection"
        + " as pvCollection where p=pv.patient) and pv.patient.study.site "
        + siteOperatorString + siteIdString
        + " group by pv.patient.study.nameShort, pv.shipment.clinic.name,"
        + " year(pv.dateProcessed), {0}(pv.dateProcessed)";

    private boolean groupByYear = false;

    public NewPsByStudyClinicImpl(List<Object> parameters,
        List<ReportOption> options) {
        super(QUERY, parameters, options);
        for (int i = 0; i < options.size(); i++) {
            ReportOption option = options.get(i);
            if (parameters.get(i) == null)
                parameters.set(i, option.getDefaultValue());
            if (option.getType().equals(String.class))
                parameters.set(i, "%" + parameters.get(i) + "%");
        }
        // FIXME modify column [2] on client side
        String groupBy = (String) parameters.remove(0);
        queryString = MessageFormat.format(queryString, groupBy);
        groupByYear = groupBy.equals("Year");
    }

    @Override
    public List<Object> postProcess(WritableApplicationService appService,
        List<Object> results) {
        List<Object> compressedDates = new ArrayList<Object>();
        if (groupByYear) {
            for (Object ob : results) {
                Object[] castOb = (Object[]) ob;
                compressedDates.add(new Object[] { castOb[0], castOb[1],
                    castOb[3], castOb[4] });
            }
        } else {
            // FIXME need BiobankListProxy
            for (Object ob : results) {
                Object[] castOb = (Object[]) ob;
                compressedDates.add(new Object[] { castOb[0], castOb[1],
                    castOb[3] + "-" + castOb[2], castOb[4] });
            }
        }
        return compressedDates;
    }

}
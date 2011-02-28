package edu.ualberta.med.biobank.server.reports;

import edu.ualberta.med.biobank.common.reports.BiobankReport;

/**
 * needs one parameters = study.nameShort
 */
public class SampleTypePvCountImpl extends AbstractReport {

    // private static final String QUERY =
    // "Select pv.shipmentPatient.patient.pnumber, pv.dateProcessed,"
    // + " pv.dateDrawn,  Alias.sampleType.name, count(*) from "
    // + ProcessingEvent.class.getName()
    // + " as pv join pv.aliquotCollection as Alias"
    // +
    // " left join Alias.aliquotPosition p where (p is not null and p not in (from "
    // + AliquotPosition.class.getName()
    // + " a where a.container.label like '"
    // + SENT_SAMPLES_FREEZER_NAME
    // + "')) and pv.shipmentPatient.patient.study.nameShort LIKE ? "
    // +
    // " GROUP BY pv, Alias.sampleType ORDER BY pv.shipmentPatient.patient.pnumber, pv.dateProcessed";

    public SampleTypePvCountImpl(BiobankReport report) {
        // super(QUERY, report);
        super("", report);
    }

}

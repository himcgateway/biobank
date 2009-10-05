package edu.ualberta.med.biobank.common.utils;

import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

import edu.ualberta.med.biobank.model.Site;
import edu.ualberta.med.biobank.model.Study;
import gov.nih.nci.system.applicationservice.ApplicationException;
import gov.nih.nci.system.applicationservice.WritableApplicationService;
import gov.nih.nci.system.query.hibernate.HQLCriteria;

public class SiteUtils {

    private static final Logger logger = Logger.getLogger(SiteUtils.class
        .getName());

    public static Study getStudyInSite(WritableApplicationService appService,
        String nameShort, Site site) {
        HQLCriteria criteria = new HQLCriteria("from " + Study.class.getName()
            + " where nameShort = ? and site = ?", Arrays.asList(new Object[] {
            nameShort, site }));
        List<Study> studies;
        try {
            studies = appService.query(criteria);
            if (studies.size() == 1) {
                return studies.get(0);
            }
        } catch (ApplicationException e) {
            logger.error("Problem while queriyng a study", e);
        }
        return null;
    }

}

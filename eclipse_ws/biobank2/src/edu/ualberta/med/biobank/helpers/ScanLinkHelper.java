package edu.ualberta.med.biobank.helpers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.ualberta.med.biobank.SessionManager;
import edu.ualberta.med.biobank.common.action.specimenType.SpecimenTypesGetForContainerTypesAction;
import edu.ualberta.med.biobank.common.wrappers.SiteWrapper;
import edu.ualberta.med.biobank.model.Capacity;
import edu.ualberta.med.biobank.model.SpecimenType;
import edu.ualberta.med.scannerconfig.PalletDimensions;
import gov.nih.nci.system.applicationservice.ApplicationException;

/**
 * Code to help with scan linking. Can be called by scan link forms and dialogs.
 *
 * @author nelson
 *
 */
public class ScanLinkHelper {

    /**
     * If the current center is a site, and if this site defines containers of 8*12 or 10*10 size,
     * then get the specimen types these containers can contain
     */
    public static List<SpecimenType> getSpecimenTypeForPalletScannable()
        throws ApplicationException {
        List<SpecimenType> result = new ArrayList<SpecimenType>();

        SiteWrapper site = SessionManager.getUser().getCurrentWorkingSite();
        if (site == null) {
            // scan link being used when working center is a clinic, clinics do not have
            // container types
            return result;
        }

        Set<Capacity> capacities = new HashSet<Capacity>();
        for (PalletDimensions gridDimensions : PalletDimensions.values()) {
            Capacity capacity = new Capacity();
            capacity.setRowCapacity(gridDimensions.getRows());
            capacity.setColCapacity(gridDimensions.getCols());
            capacities.add(capacity);
        }
        result = SessionManager.getAppService().doAction(
            new SpecimenTypesGetForContainerTypesAction(
                site.getWrappedObject(), capacities
            )).getList();
        return result;
    }
}

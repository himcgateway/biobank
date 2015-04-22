package edu.ualberta.med.biobank.helpers;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import edu.ualberta.med.biobank.SessionManager;
import edu.ualberta.med.biobank.common.action.specimen.SpecimenLinkSaveAction.AliquotedSpecimenResInfo;
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

    @SuppressWarnings("nls")
    public static void printSaveMultipleLogMessage(
        List<AliquotedSpecimenResInfo> linkedSpecimens, Logger activityLogger) {
        StringBuffer sb = new StringBuffer("ALIQUOTED SPECIMENS:\n");
        for (AliquotedSpecimenResInfo resInfo : linkedSpecimens) {
            sb.append(MessageFormat.format(
                "LINKED: ''{0}'' with type ''{1}'' to source: {2} ({3}) - Patient: {4} - Visit: {5} - Center: {6}\n",
                resInfo.inventoryId, resInfo.typeName, resInfo.parentTypeName,
                resInfo.parentInventoryId, resInfo.patientPNumber, resInfo.visitNumber,
                resInfo.currentCenterName));
        }
        // Want only one common 'log entry' so use a stringbuffer to print
        // everything together
        activityLogger.trace(sb.toString());

        Map<String, Set<AliquotedSpecimenResInfo>> counts =
            new HashMap<String, Set<AliquotedSpecimenResInfo>>();

        for (AliquotedSpecimenResInfo spc : linkedSpecimens) {
            Set<AliquotedSpecimenResInfo> set = counts.get(spc.patientPNumber);
            if (set == null) {
                set = new HashSet<AliquotedSpecimenResInfo>();
                counts.put(spc.patientPNumber, set);
            }
            set.add(spc);
        }

        // LINKING\: {0} specimens linked to patient {1} on center {2}
        appendLog(MessageFormat.format(
            "LINKING: {0} specimens linked to patient {1} on center {2}", linkedSpecimens.size(),
            linkFormPatientManagement.getCurrentPatient().getPnumber(), SessionManager.getUser()
                .getCurrentWorkingCenter().getNameShort()));
    }
}

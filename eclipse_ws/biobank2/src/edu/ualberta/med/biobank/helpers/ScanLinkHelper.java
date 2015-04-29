package edu.ualberta.med.biobank.helpers;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.PlatformUI;
import org.xnap.commons.i18n.I18n;
import org.xnap.commons.i18n.I18nFactory;

import edu.ualberta.med.biobank.SessionManager;
import edu.ualberta.med.biobank.common.action.container.ContainerGetContainerOrParentsByLabelAction;
import edu.ualberta.med.biobank.common.action.container.ContainerGetContainerOrParentsByLabelAction.ContainerData;
import edu.ualberta.med.biobank.common.action.specimen.SpecimenLinkSaveAction.AliquotedSpecimenResInfo;
import edu.ualberta.med.biobank.common.action.specimenType.SpecimenTypesGetForContainerTypesAction;
import edu.ualberta.med.biobank.common.wrappers.ContainerTypeWrapper;
import edu.ualberta.med.biobank.common.wrappers.ContainerWrapper;
import edu.ualberta.med.biobank.common.wrappers.SiteWrapper;
import edu.ualberta.med.biobank.dialogs.select.SelectParentContainerDialog;
import edu.ualberta.med.biobank.gui.common.BgcPlugin;
import edu.ualberta.med.biobank.model.Capacity;
import edu.ualberta.med.biobank.model.Container;
import edu.ualberta.med.biobank.model.ContainerType;
import edu.ualberta.med.biobank.model.Site;
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

    private static final I18n i18n = I18nFactory.getI18n(ScanLinkHelper.class);

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

    /**
     * Want only one common 'log entry' so use a stringbuffer to print every thing together.
     * 
     * @param linkedSpecimens
     * 
     * @return A formatted string that can be logged stating what specimens were linked.
     */
    @SuppressWarnings("nls")
    public static List<String> linkedSpecimensLogMessage(
        List<AliquotedSpecimenResInfo> linkedSpecimens) {
        List<String> result = new ArrayList<String>();

        StringBuffer sb = new StringBuffer("ALIQUOTED SPECIMENS:\n");
        for (AliquotedSpecimenResInfo resInfo : linkedSpecimens) {
            sb.append(MessageFormat.format(
                "LINKED: ''{0}'' with type ''{1}'' to source: {2} ({3}) - Patient: {4} - Visit: {5} - Center: {6}\n",
                resInfo.inventoryId, resInfo.typeName, resInfo.parentTypeName,
                resInfo.parentInventoryId, resInfo.patientPNumber, resInfo.visitNumber,
                resInfo.currentCenterName));
        }
        result.add(sb.toString());

        Map<String, Integer> counts = new HashMap<String, Integer>();

        for (AliquotedSpecimenResInfo spc : linkedSpecimens) {
            Integer count = counts.get(spc.patientPNumber);
            if (count == null) {
                counts.put(spc.patientPNumber, 1);
            } else {
                counts.put(spc.patientPNumber, count + 1);
            }
        }

        for (Entry<String, Integer> entry : counts.entrySet()) {
            String pnumber = entry.getKey();
            Integer count = entry.getValue();

            // LINKING\: {0} specimens linked to patient {1} on center {2}
            result.add(MessageFormat.format(
                "LINKING: {0} specimens linked to patient {1} on center {2}",
                count,
                pnumber,
                SessionManager.getUser().getCurrentWorkingCenter().getNameShort()));
        }
        return result;
    }

    @SuppressWarnings("nls")
    public static boolean isContainerValid(ContainerWrapper palletContainer, String positionText) {
        // a container with this barcode exists
        if (!isPalletScannable(palletContainer)) {
            BgcPlugin.openAsyncError(
                // TR: dialog title
                i18n.tr("Values validation"),
                // TR: dialog message
                i18n.tr("A container with this barcode exists but is not a 8*12 or 10*10 container."));
            return false;
        }

        if (!positionText.isEmpty()
            && !positionText.equals(palletContainer.getLabel())) {
            // a label was entered but is different from the one set to the pallet
            // retrieved
            BgcPlugin.openAsyncError(
                // TR: dialog title
                i18n.tr("Values validation"),
                // TR: dialog message
                i18n.tr("A pallet with barcode {0} is already used in position {1}.",
                    palletContainer.getProductBarcode(),
                    palletContainer.getFullInfoLabel()));
            return false;
        }
        return true;
    }

    private static boolean isPalletScannable(ContainerWrapper container) {
        return isPalletScannable(container.getContainerType());
    }

    public static boolean isPalletScannable(ContainerTypeWrapper ctype) {
        for (PalletDimensions gridDimensions : PalletDimensions.values()) {
            int rows = gridDimensions.getRows();
            int cols = gridDimensions.getCols();
            if (ctype.isPalletRowsCols(rows, cols))
                return true;
        }
        return false;
    }

    @SuppressWarnings("nls")
    public boolean checkMultipleContainerPosition(String label) {
        initContainersFromPosition(label, null);

        if ((container == null) && parentContainers.isEmpty()) {
            // no container selected
            return false;
        }

        if (container != null) {
            container.setLabel(palletPositionText.getText());
            return checkAndUpdateContainer();
        }

        ContainerWrapper parentContainer = parentContainers.get(0);
        try {
            container = parentContainer.getChildByLabel(
                palletPositionText.getText().substring(parentContainer.getLabel().length()));

            if (container == null) {
                // no container at this position right now, create one
                String childLabel = palletPositionText.getText().substring(
                    parentContainer.getLabel().length());
                parentContainer.addChild(childLabel, currentMultipleContainer);
                container = currentMultipleContainer;
                container.setParent(parentContainer, parentContainer.getPositionFromLabelingScheme(
                    childLabel));
            }

            checkAndUpdateContainer();
        } catch (Exception ex) {
            BgcPlugin.openError(
                // TR: dialog title
                i18n.tr("Values validation"), ex);
            appendLog(NLS.bind("ERROR: {0}", ex.getMessage()));
            return false;
        }
        return true;
    }

    /**
     * Search possible parents from the position text. Is used both by single and multiple assign.
     * 
     * @param positionText the position to use for initialization
     * @param type
     */
    @SuppressWarnings("nls")
    public static List<ContainerWrapper> initContainersFromPosition(String label,
        ContainerTypeWrapper type) {
        List<ContainerWrapper> result = new ArrayList<ContainerWrapper>();
        try {
            Site site = SessionManager.getUser().getCurrentWorkingSite().getWrappedObject();

            ContainerType rawType = null;
            if (type != null) {
                rawType = type.getWrappedObject();
            }

            // ContainerWrapper container = null;
            ContainerData containerData = SessionManager.getAppService().doAction(
                new ContainerGetContainerOrParentsByLabelAction(label, site, rawType));

            List<Container> possibleParents = containerData.getPossibleParentContainers();

            if (containerData.getContainer() != null) {
                result.add(new ContainerWrapper(SessionManager.getAppService(),
                    containerData.getContainer()));
            } else if (possibleParents.isEmpty()) {
                BgcPlugin.openAsyncError(
                    // TR: dialog title
                    i18n.tr("Container label error"),
                    // TR: dialog message
                    i18n.tr("Unable to find a container with label {0}", label));
            } else if (possibleParents.size() == 1) {
                Container parent = possibleParents.get(0);
                result.add(new ContainerWrapper(SessionManager.getAppService(), parent));
            } else {
                SelectParentContainerDialog dlg = new SelectParentContainerDialog(
                    PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), possibleParents);
                dlg.open();
                if (dlg.getSelectedContainer() == null) {
                    Set<String> labelData = new HashSet<String>();
                    for (Container cont : possibleParents) {
                        labelData.add(ContainerWrapper.getFullInfoLabel(cont));
                    }
                    BgcPlugin.openError(
                        // TR: dialog title
                        i18n.tr("Container problem"),
                        // TR: dialog message
                        i18n.tr("More than one container found matching {0}",
                            StringUtils.join(labelData, ", ")));
                } else {
                    result.add(new ContainerWrapper(SessionManager.getAppService(),
                        dlg.getSelectedContainer()));
                }
            }
        } catch (Exception ex) {
            BgcPlugin.openError(
                // TR: dialog title
                i18n.tr("Init container from position"), ex);
        }
        return result;
    }
}

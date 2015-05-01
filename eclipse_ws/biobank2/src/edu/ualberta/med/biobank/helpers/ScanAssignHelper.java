package edu.ualberta.med.biobank.helpers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.eclipse.ui.PlatformUI;
import org.xnap.commons.i18n.I18n;
import org.xnap.commons.i18n.I18nFactory;

import edu.ualberta.med.biobank.SessionManager;
import edu.ualberta.med.biobank.common.action.container.ContainerGetContainerOrParentsByLabelAction;
import edu.ualberta.med.biobank.common.action.container.ContainerGetContainerOrParentsByLabelAction.ContainerData;
import edu.ualberta.med.biobank.common.wrappers.ContainerTypeWrapper;
import edu.ualberta.med.biobank.common.wrappers.ContainerWrapper;
import edu.ualberta.med.biobank.dialogs.select.SelectParentContainerDialog;
import edu.ualberta.med.biobank.gui.common.BgcPlugin;
import edu.ualberta.med.biobank.model.Container;
import edu.ualberta.med.biobank.model.Site;
import edu.ualberta.med.scannerconfig.PalletDimensions;

public class ScanAssignHelper {

    private static final I18n i18n = I18nFactory.getI18n(ScanAssignHelper.class);

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

    /**
     * Returns the container matching the container label, or if no container exists, creates a
     * child container with a parent container that can have a valid child with the given label.
     * 
     * @param palletLabel the label to search for.
     * 
     * @param currentContainer if a new container is to be created under the parent container, then
     *            this is the one that is added.
     * 
     * @return The container matching the label.
     */
    @SuppressWarnings("nls")
    public static ContainerWrapper getOrCreateContainerByLabel(
        String palletLabel, ContainerWrapper currentContainer) {
        ContainerWrapper container = getContainerByLabel(palletLabel);

        if (container == null) {
            // no container selected
            return null;
        }

        if (container.getLabel().equals(palletLabel)) {
            // only get here if label matches an existing container
            return container;
        }

        // only get here if the label does not match an exising container, and there is 1 or more
        // parent containers that can hold a container with the given label
        ContainerWrapper parentContainer = container;
        try {
            container = parentContainer.getChildByLabel(
                palletLabel.substring(parentContainer.getLabel().length()));

            if (container == null) {
                // no container at this position right now, create one
                String childLabel = palletLabel.substring(parentContainer.getLabel().length());
                parentContainer.addChild(childLabel, currentContainer);
                container = currentContainer;
                container.setParent(parentContainer,
                    parentContainer.getPositionFromLabelingScheme(childLabel));
            }

            return container;
        } catch (Exception ex) {
            BgcPlugin.openError(
                // TR: dialog title
                i18n.tr("Values validation"), ex);
        }
        return null;
    }

    /**
     * Returns a container matching the given label, or a parent that can have a child with the
     * given label.
     * 
     * @param palletLabel the container label.
     * 
     * @returns returns the container with the given label, or a parent container that can have a
     *          child with the given label.
     */
    @SuppressWarnings("nls")
    public static ContainerWrapper getContainerByLabel(String palletLabel) {
        try {
            Site site = SessionManager.getUser().getCurrentWorkingSite().getWrappedObject();

            ContainerData containerData = SessionManager.getAppService().doAction(
                new ContainerGetContainerOrParentsByLabelAction(palletLabel, site, null));

            List<Container> possibleParents = containerData.getPossibleParentContainers();

            if (containerData.getContainer() != null) {
                return new ContainerWrapper(SessionManager.getAppService(), containerData.getContainer());
            } else if (possibleParents.isEmpty()) {
                BgcPlugin.openAsyncError(
                    // TR: dialog title
                    i18n.tr("Container label error"),
                    // TR: dialog message
                    i18n.tr("Unable to find a container with label {0}", palletLabel));
            } else if (possibleParents.size() == 1) {
                Container parent = possibleParents.get(0);
                return new ContainerWrapper(SessionManager.getAppService(), parent);
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
                    return new ContainerWrapper(SessionManager.getAppService(), dlg.getSelectedContainer());
                }
            }
        } catch (Exception ex) {
            BgcPlugin.openError(
                // TR: dialog title
                i18n.tr("Init container from position"), ex);
        }
        return null;
    }

    public static List<ContainerTypeWrapper> getContainerTypes(
        ContainerWrapper container, boolean usingFlatbedScanner) {
        // try {
        List<ContainerTypeWrapper> possibleTypes = new ArrayList<ContainerTypeWrapper>(0);
        ContainerWrapper parentContainer = container.getParentContainer();

        if (parentContainer != null) {
            possibleTypes.addAll(getPossibleTypes(
                parentContainer.getContainerType().getChildContainerTypeCollection(),
                usingFlatbedScanner));
        } else {
            possibleTypes.add(container.getContainerType());
        }

        return possibleTypes;
        // }
    }

    /**
     * is use scanner, want only 8*12 or 10*10 pallets. Also check the container type can hold
     * specimens
     */
    public static List<ContainerTypeWrapper> getPossibleTypes(
        List<ContainerTypeWrapper> childContainerTypeCollection,
        boolean usingFlatbedScanner) {
        List<ContainerTypeWrapper> palletTypes = new ArrayList<ContainerTypeWrapper>();
        for (ContainerTypeWrapper type : childContainerTypeCollection) {
            if (!type.getSpecimenTypeCollection().isEmpty()
                && (!usingFlatbedScanner || ScanAssignHelper.isPalletScannable(type)))
                palletTypes.add(type);
        }
        return palletTypes;
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
}

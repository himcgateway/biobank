package edu.ualberta.med.biobank.common.wrappers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import edu.ualberta.med.biobank.common.BiobankCheckException;
import edu.ualberta.med.biobank.common.formatters.DateFormatter;
import edu.ualberta.med.biobank.common.wrappers.internal.AbstractPositionWrapper;
import edu.ualberta.med.biobank.common.wrappers.internal.AliquotPositionWrapper;
import edu.ualberta.med.biobank.model.ActivityStatus;
import edu.ualberta.med.biobank.model.Aliquot;
import edu.ualberta.med.biobank.model.AliquotPosition;
import edu.ualberta.med.biobank.model.PatientVisit;
import edu.ualberta.med.biobank.model.SampleType;
import edu.ualberta.med.biobank.server.applicationservice.BiobankApplicationService;
import edu.ualberta.med.biobank.util.RowColPos;
import gov.nih.nci.system.applicationservice.ApplicationException;
import gov.nih.nci.system.applicationservice.WritableApplicationService;
import gov.nih.nci.system.query.hibernate.HQLCriteria;

public class AliquotWrapper extends
    AbstractPositionHolder<Aliquot, AliquotPosition> {

    private PatientVisitWrapper patientVisit;
    private SampleTypeWrapper sampleType;
    private ActivityStatusWrapper activityStatus;

    public AliquotWrapper(WritableApplicationService appService,
        Aliquot wrappedObject) {
        super(appService, wrappedObject);
    }

    public AliquotWrapper(WritableApplicationService appService) {
        super(appService);
    }

    @Override
    protected String[] getPropertyChangeNames() {
        return new String[] { "inventoryId", "patientVisit", "position",
            "linkDate", "sampleType", "quantity", "activityStatus", "comment" };
    }

    @Override
    public Class<Aliquot> getWrappedClass() {
        return Aliquot.class;
    }

    @Override
    public void persist() throws Exception {
        // check if position was deleted
        if (getPosition() == null) {
            // get original position
            AliquotPosition rawPos = wrappedObject.getAliquotPosition();
            if (rawPos != null) {
                AbstractPositionWrapper<AliquotPosition> pos = new AliquotPositionWrapper(
                    appService, rawPos);
                pos.delete();
            }
            wrappedObject.setAliquotPosition(null);
        }
        super.persist();
    }

    @Override
    protected void persistChecks() throws BiobankCheckException,
        ApplicationException {
        if (getActivityStatus() == null) {
            throw new BiobankCheckException(
                "the aliquot does not have an activity status");
        }
        checkPatientVisitNotNull();
        checkInventoryIdUnique();
        checkParentAcceptSampleType();
        super.persistChecks();
    }

    private void checkPatientVisitNotNull() throws BiobankCheckException {
        if (getPatientVisit() == null) {
            throw new BiobankCheckException("patient visit should be set");
        }
    }

    public String getInventoryId() {
        return wrappedObject.getInventoryId();
    }

    public void setInventoryId(String inventoryId) {
        String oldInventoryId = inventoryId;
        wrappedObject.setInventoryId(inventoryId);
        propertyChangeSupport.firePropertyChange("inventoryId", oldInventoryId,
            inventoryId);
    }

    public void checkInventoryIdUnique() throws BiobankCheckException,
        ApplicationException {
        List<AliquotWrapper> aliquots = getAliquotsInSite(appService,
            getInventoryId(), getSite());
        boolean alreadyExists = false;
        if (aliquots.size() > 0 && isNew()) {
            alreadyExists = true;
        } else {
            for (AliquotWrapper aliquot : aliquots) {
                if (!aliquot.getId().equals(getId())) {
                    alreadyExists = true;
                    break;
                }
            }
        }
        if (alreadyExists) {
            throw new BiobankCheckException("An aliquot with inventory id \""
                + getInventoryId() + "\" already exists.");
        }
    }

    private void checkParentAcceptSampleType() throws BiobankCheckException {
        ContainerWrapper parent = getParent();
        if (parent != null) {
            ContainerTypeWrapper parentType = getParent().getContainerType();
            try {
                parentType.reload();
            } catch (Exception e) {
                throw new BiobankCheckException(e);
            }
            List<SampleTypeWrapper> types = parentType
                .getSampleTypeCollection();
            if (types == null || !types.contains(getSampleType())) {
                throw new BiobankCheckException("Container "
                    + getParent().getFullInfoLabel()
                    + " does not allow inserts of sample type "
                    + getSampleType().getName() + ".");
            }
        }
    }

    @Override
    public SiteWrapper getSite() {
        if (getPatientVisit() != null) {
            return getPatientVisit().getPatient().getStudy().getSite();
        }
        return null;
    }

    protected void setPatientVisit(PatientVisit patientVisit) {
        if (patientVisit == null)
            this.patientVisit = null;
        else
            this.patientVisit = new PatientVisitWrapper(appService,
                patientVisit);
        PatientVisit oldPvRaw = wrappedObject.getPatientVisit();
        PatientVisit newPvRaw = patientVisit;
        wrappedObject.setPatientVisit(newPvRaw);
        propertyChangeSupport.firePropertyChange("patientVisit", oldPvRaw,
            newPvRaw);
    }

    public void setPatientVisit(PatientVisitWrapper patientVisit) {
        if (patientVisit == null) {
            setPatientVisit((PatientVisit) null);
        } else {
            setPatientVisit(patientVisit.getWrappedObject());
        }
    }

    public PatientVisitWrapper getPatientVisit() {
        if (patientVisit == null) {
            PatientVisit pv = wrappedObject.getPatientVisit();
            if (pv == null)
                return null;
            this.patientVisit = new PatientVisitWrapper(appService, pv);
        }
        return patientVisit;
    }

    public void setAliquotPositionFromString(String positionString,
        ContainerWrapper parentContainer) throws Exception {
        RowColPos rcp = parentContainer.getContainerType()
            .getRowColFromPositionString(positionString);
        if ((rcp.row > -1) && (rcp.col > -1)) {
            setPosition(rcp);
        } else {
            throw new Exception("Position " + positionString + " not valid");
        }
    }

    /**
     * Method used to check if the current position of this aliquot is available
     * on the container. Return true if the position is free, false otherwise
     */
    public boolean isPositionFree(ContainerWrapper parentContainer)
        throws ApplicationException {
        RowColPos position = getPosition();
        if (position != null) {
            HQLCriteria criteria = new HQLCriteria("from "
                + Aliquot.class.getName()
                + " where aliquotPosition.row=? and aliquotPosition.col=?"
                + " and aliquotPosition.container=?",
                Arrays.asList(new Object[] { position.row, position.col,
                    parentContainer.getWrappedObject() }));

            List<Aliquot> samples = appService.query(criteria);
            if (samples.size() > 0) {
                return false;
            }
        }
        return true;
    }

    protected void setSampleType(SampleType type) {
        if (type == null)
            this.sampleType = null;
        else
            this.sampleType = new SampleTypeWrapper(appService, type);
        SampleType oldTypeRaw = wrappedObject.getSampleType();
        SampleType newTypeRaw = type;
        wrappedObject.setSampleType(type);
        propertyChangeSupport.firePropertyChange("sampleType", oldTypeRaw,
            newTypeRaw);
    }

    public void setSampleType(SampleTypeWrapper type) {
        if (type == null) {
            setSampleType((SampleType) null);
        } else {
            setSampleType(type.wrappedObject);
        }
    }

    public SampleTypeWrapper getSampleType() {
        if (sampleType == null) {
            SampleType s = wrappedObject.getSampleType();
            if (s == null)
                return null;
            this.sampleType = new SampleTypeWrapper(appService, s);
        }
        return sampleType;
    }

    public void setLinkDate(Date date) {
        Date oldDate = getLinkDate();
        wrappedObject.setLinkDate(date);
        propertyChangeSupport.firePropertyChange("linkDate", oldDate, date);
    }

    public Date getLinkDate() {
        return wrappedObject.getLinkDate();
    }

    public String getFormattedLinkDate() {
        return DateFormatter.formatAsDateTime(wrappedObject.getLinkDate());
    }

    public void setQuantity(Double quantity) {
        Double oldQuantity = wrappedObject.getQuantity();
        wrappedObject.setQuantity(quantity);
        propertyChangeSupport.firePropertyChange("quantity", oldQuantity,
            quantity);
    }

    public Double getQuantity() {
        return wrappedObject.getQuantity();
    }

    public ActivityStatusWrapper getActivityStatus() {
        if (activityStatus == null) {
            ActivityStatus a = wrappedObject.getActivityStatus();
            if (a == null)
                return null;
            this.activityStatus = new ActivityStatusWrapper(appService, a);
        }
        return activityStatus;
    }

    public void setActivityStatus(ActivityStatusWrapper activityStatus) {
        this.activityStatus = activityStatus;
        ActivityStatus oldActivityStatus = wrappedObject.getActivityStatus();
        ActivityStatus rawObject = null;
        if (activityStatus != null) {
            rawObject = activityStatus.getWrappedObject();
        }
        wrappedObject.setActivityStatus(rawObject);
        propertyChangeSupport.firePropertyChange("activityStatus",
            oldActivityStatus, activityStatus);
    }

    public void setComment(String comment) {
        String oldComment = wrappedObject.getComment();
        wrappedObject.setComment(comment);
        propertyChangeSupport
            .firePropertyChange("comment", oldComment, comment);
    }

    public String getComment() {
        return wrappedObject.getComment();
    }

    public String getPositionString() {
        return getPositionString(true, true);
    }

    public String getPositionString(boolean fullString,
        boolean addTopParentShortName) {
        RowColPos position = getPosition();
        if (position == null) {
            return null;
        }

        if (!fullString) {
            return getPositionStringInParent(position, getParent());
        }
        ContainerWrapper directParent = getParent();
        ContainerWrapper topContainer = directParent;
        while (topContainer.hasParent()) {
            topContainer = topContainer.getParent();
        }
        String nameShort = topContainer.getContainerType().getNameShort();
        if (addTopParentShortName && nameShort != null)
            return directParent.getLabel()
                + getPositionStringInParent(position, directParent) + " ("
                + nameShort + ")";
        return directParent.getLabel()
            + getPositionStringInParent(position, directParent);
    }

    private String getPositionStringInParent(RowColPos position,
        ContainerWrapper parent) {
        if (parent != null) {
            return parent.getContainerType().getPositionString(position);
        }
        return null;
    }

    public void setQuantityFromType() {
        StudyWrapper study = patientVisit.getPatient().getStudy();
        Double volume = null;
        Collection<SampleStorageWrapper> sampleStorageCollection = study
            .getSampleStorageCollection();
        if (sampleStorageCollection != null) {
            for (SampleStorageWrapper ss : sampleStorageCollection) {
                if (ss.getSampleType().getId().equals(getSampleType().getId())) {
                    volume = ss.getVolume();
                }
            }
        }
        setQuantity(volume);
    }

    @Override
    public void loadAttributes() throws Exception {
        super.loadAttributes();
        getPositionString();
        wrappedObject.getSampleType().getName();
    }

    @Override
    protected void deleteChecks() throws BiobankCheckException,
        ApplicationException {
    }

    public static List<AliquotWrapper> getAliquotsInSite(
        WritableApplicationService appService, String inventoryId,
        SiteWrapper site) throws ApplicationException {
        HQLCriteria criteria = new HQLCriteria(
            "from "
                + Aliquot.class.getName()
                + " where inventoryId = ? and patientVisit.patient.study.site.id = ?",
            Arrays.asList(new Object[] { inventoryId, site.getId() }));
        List<Aliquot> aliquots = appService.query(criteria);
        List<AliquotWrapper> list = new ArrayList<AliquotWrapper>();
        for (Aliquot aliquot : aliquots) {
            if (aliquot.getInventoryId().equals(inventoryId)) {
                list.add(new AliquotWrapper(appService, aliquot));
            }
        }
        return list;
    }

    public static List<AliquotWrapper> getAliquotsNonActive(
        WritableApplicationService appService, SiteWrapper site)
        throws Exception {
        ActivityStatusWrapper activeStatus = ActivityStatusWrapper
            .getActivityStatus(appService, "Active");
        HQLCriteria criteria = new HQLCriteria(
            "from "
                + Aliquot.class.getName()
                + " where patientVisit.patient.study.site.id = ? and activityStatus != ?",
            Arrays.asList(new Object[] { site.getId(),
                activeStatus.getWrappedObject() }));
        List<Aliquot> aliquots = appService.query(criteria);
        List<AliquotWrapper> list = new ArrayList<AliquotWrapper>();

        for (Aliquot aliquot : aliquots) {
            list.add(new AliquotWrapper(appService, aliquot));
        }
        return list;
    }

    public static List<AliquotWrapper> getAliquotsInSiteWithPositionLabel(
        WritableApplicationService appService, SiteWrapper site,
        String positionString) throws ApplicationException,
        BiobankCheckException {
        // assume that the aliquot label position is 2 or 3 letters
        // will search with both possible positions
        String lastContainerLabel = positionString.substring(0,
            positionString.length() - 2);
        String aliquotPositionLabel = positionString.replace(
            lastContainerLabel, "");
        List<AliquotWrapper> aliquots = getAliquotsInSiteWithPositionLabel(
            appService, site, lastContainerLabel, aliquotPositionLabel);
        lastContainerLabel = positionString.substring(0,
            positionString.length() - 3);
        aliquotPositionLabel = positionString.replace(lastContainerLabel, "");
        aliquots.addAll(getAliquotsInSiteWithPositionLabel(appService, site,
            lastContainerLabel, aliquotPositionLabel));
        return aliquots;
    }

    private static List<AliquotWrapper> getAliquotsInSiteWithPositionLabel(
        WritableApplicationService appService, SiteWrapper site,
        String containerString, String aliquotString)
        throws ApplicationException, BiobankCheckException {
        List<ContainerWrapper> containers = ContainerWrapper
            .getContainersInSite(appService, site, containerString);
        List<AliquotWrapper> aliquots = new ArrayList<AliquotWrapper>();
        for (ContainerWrapper container : containers) {
            RowColPos rcp = null;
            try {
                rcp = container.getContainerType().getRowColFromPositionString(
                    aliquotString);
            } catch (Exception e) {
                // do nothing. The positionString doesn't fit the current
                // container.
            }
            if (rcp != null) {
                if ((rcp.row > -1) && (rcp.col > -1)) {
                    AliquotWrapper aliquot = container.getAliquot(rcp.row,
                        rcp.col);
                    if (aliquot != null) {
                        aliquots.add(aliquot);
                    }
                }
            }
        }
        return aliquots;
    }

    @Override
    public int compareTo(ModelWrapper<Aliquot> o) {
        if (o instanceof AliquotWrapper) {
            return getInventoryId().compareTo(
                ((AliquotWrapper) o).getInventoryId());
        }
        return 0;
    }

    @Override
    protected AbstractPositionWrapper<AliquotPosition> getSpecificPositionWrapper(
        boolean initIfNoPosition) {
        if (newPositionSet) {
            if (rowColPosition != null) {
                AliquotPositionWrapper posWrapper = new AliquotPositionWrapper(
                    appService);
                posWrapper.setRow(rowColPosition.row);
                posWrapper.setCol(rowColPosition.col);
                posWrapper.setAliquot(this);
                wrappedObject.setAliquotPosition(posWrapper.getWrappedObject());
                return posWrapper;
            }
        } else {
            AliquotPosition pos = wrappedObject.getAliquotPosition();
            if (pos != null) {
                return new AliquotPositionWrapper(appService, pos);
            } else if (initIfNoPosition) {
                AliquotPositionWrapper posWrapper = new AliquotPositionWrapper(
                    appService);
                posWrapper.setAliquot(this);
                wrappedObject.setAliquotPosition(posWrapper.getWrappedObject());
                return posWrapper;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return getInventoryId();
    }

    @Override
    protected void log(String action, String details) {
        ((BiobankApplicationService) appService).logActivity(action,
            getPatientVisit().getPatient().getPnumber(), getInventoryId(),
            getPositionString(true, false), "aliquot " + details, "Aliquot");
    }

    @Override
    public void reload() throws Exception {
        patientVisit = null;
        sampleType = null;
        activityStatus = null;
        super.reload();
    }
}

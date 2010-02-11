package edu.ualberta.med.biobank.common.wrappers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.ualberta.med.biobank.common.BiobankCheckException;
import edu.ualberta.med.biobank.common.formatters.DateFormatter;
import edu.ualberta.med.biobank.common.wrappers.internal.PvAttrWrapper;
import edu.ualberta.med.biobank.common.wrappers.internal.StudyPvAttrWrapper;
import edu.ualberta.med.biobank.model.Patient;
import edu.ualberta.med.biobank.model.PatientVisit;
import edu.ualberta.med.biobank.model.PvAttr;
import edu.ualberta.med.biobank.model.PvSampleSource;
import edu.ualberta.med.biobank.model.Sample;
import edu.ualberta.med.biobank.model.Shipment;
import gov.nih.nci.system.applicationservice.ApplicationException;
import gov.nih.nci.system.applicationservice.WritableApplicationService;
import gov.nih.nci.system.query.hibernate.HQLCriteria;

public class PatientVisitWrapper extends ModelWrapper<PatientVisit> {

    private Map<String, StudyPvAttrWrapper> studyPvAttrMap;

    private Map<String, PvAttrWrapper> pvAttrMap;

    private Set<PvSampleSourceWrapper> deletedPvSampleSources = new HashSet<PvSampleSourceWrapper>();

    public PatientVisitWrapper(WritableApplicationService appService,
        PatientVisit wrappedObject) {
        super(appService, wrappedObject);
        studyPvAttrMap = null;
        pvAttrMap = null;
    }

    public PatientVisitWrapper(WritableApplicationService appService) {
        super(appService);
    }

    @Override
    protected String[] getPropertyChangeNames() {
        return new String[] { "patient", "dateProcessed", "comment",
            "pvAttrCollection", "sampleCollection", "username", "shipment",
            "pvSampleSourceCollection" };
    }

    public Date getDateProcessed() {
        return wrappedObject.getDateProcessed();
    }

    public String getFormattedDateProcessed() {
        return DateFormatter.formatAsDateTime(getDateProcessed());
    }

    public String getComment() {
        return wrappedObject.getComment();
    }

    public PatientWrapper getPatient() {
        Patient patient = wrappedObject.getPatient();
        if (patient == null) {
            return null;
        }
        return new PatientWrapper(appService, patient);
    }

    public void setPatient(PatientWrapper patientWrapper) {
        setPatient(patientWrapper.getWrappedObject());
    }

    public void setPatient(Patient patient) {
        Patient oldPatient = wrappedObject.getPatient();
        wrappedObject.setPatient(patient);
        propertyChangeSupport
            .firePropertyChange("patient", oldPatient, patient);
    }

    @SuppressWarnings("unchecked")
    public List<SampleWrapper> getSampleCollection() {
        List<SampleWrapper> sampleCollection = (List<SampleWrapper>) propertiesMap
            .get("sampleCollection");
        if (sampleCollection == null) {
            Collection<Sample> children = wrappedObject.getSampleCollection();
            if (children != null) {
                sampleCollection = new ArrayList<SampleWrapper>();
                for (Sample sample : children) {
                    sampleCollection.add(new SampleWrapper(appService, sample));
                }
                propertiesMap.put("sampleCollection", sampleCollection);
            }
        }
        return sampleCollection;
    }

    @SuppressWarnings("unchecked")
    private List<PvAttrWrapper> getPvAttrCollection() {
        List<PvAttrWrapper> pvAttrCollection = (List<PvAttrWrapper>) propertiesMap
            .get("pvAttrCollection");
        if (pvAttrCollection == null) {
            Collection<PvAttr> children = wrappedObject.getPvAttrCollection();
            if (children != null) {
                pvAttrCollection = new ArrayList<PvAttrWrapper>();
                for (PvAttr pvAttr : children) {
                    pvAttrCollection.add(new PvAttrWrapper(appService, pvAttr));
                }
                propertiesMap.put("pvAttrCollection", pvAttrCollection);
            }
        }
        return pvAttrCollection;
    }

    private void setPvAttrCollection(Collection<PvAttr> pvAttrCollection,
        boolean setNull) {
        Collection<PvAttr> oldCollection = wrappedObject.getPvAttrCollection();
        wrappedObject.setPvAttrCollection(pvAttrCollection);
        propertyChangeSupport.firePropertyChange("pvAttrCollection",
            oldCollection, pvAttrCollection);
        if (setNull) {
            propertiesMap.put("pvAttrCollection", null);
        }
    }

    private void setPvAttrCollection(Collection<PvAttrWrapper> pvAttrCollection) {
        Collection<PvAttr> pvCollection = new HashSet<PvAttr>();
        for (PvAttrWrapper pv : pvAttrCollection) {
            pvCollection.add(pv.getWrappedObject());
        }
        setPvAttrCollection(pvCollection, false);
        propertiesMap.put("pvAttrCollection", pvAttrCollection);
    }

    private Map<String, StudyPvAttrWrapper> getStudyPvAttrMap() {
        if (studyPvAttrMap != null)
            return studyPvAttrMap;

        studyPvAttrMap = new HashMap<String, StudyPvAttrWrapper>();
        Collection<StudyPvAttrWrapper> studyPvAttrCollection = getPatient()
            .getStudy().getStudyPvAttrCollection();
        if (studyPvAttrCollection != null) {
            for (StudyPvAttrWrapper studyPvAttr : studyPvAttrCollection) {
                studyPvAttrMap.put(studyPvAttr.getLabel(), studyPvAttr);
            }
        }
        return studyPvAttrMap;
    }

    private Map<String, PvAttrWrapper> getPvAttrMap() {
        getStudyPvAttrMap();
        if (pvAttrMap != null)
            return pvAttrMap;

        pvAttrMap = new HashMap<String, PvAttrWrapper>();
        List<PvAttrWrapper> pvAttrCollection = getPvAttrCollection();
        if (pvAttrCollection != null) {
            for (PvAttrWrapper pvAttr : pvAttrCollection) {
                pvAttrMap.put(pvAttr.getStudyPvAttr().getLabel(), pvAttr);
            }
        }
        return pvAttrMap;
    }

    public String[] getPvAttrLabels() {
        getPvAttrMap();
        return pvAttrMap.keySet().toArray(new String[] {});
    }

    public String getPvAttrValue(String label) throws Exception {
        getPvAttrMap();
        PvAttrWrapper pvAttr = pvAttrMap.get(label);
        if (pvAttr == null) {
            StudyPvAttrWrapper studyPvAttr = studyPvAttrMap.get(label);
            // make sure "label" is a valid study pv attr
            if (studyPvAttr == null) {
                throw new Exception("StudyPvAttr with label \"" + label
                    + "\" is invalid");
            }
            // not assigned yet so return null
            return null;
        }
        return pvAttr.getValue();
    }

    public String getPvAttrTypeName(String label) throws Exception {
        getPvAttrMap();
        PvAttrWrapper pvAttr = pvAttrMap.get(label);
        StudyPvAttrWrapper studyPvAttr = null;
        if (pvAttr != null) {
            studyPvAttr = pvAttr.getStudyPvAttr();
        } else {
            studyPvAttr = studyPvAttrMap.get(label);
            // make sure "label" is a valid study pv attr
            if (studyPvAttr == null) {
                throw new Exception("StudyPvAttr withr label \"" + label
                    + "\" does not exist");
            }
        }
        return studyPvAttr.getPvAttrType().getName();
    }

    public String[] getPvAttrPermissible(String label) throws Exception {
        getPvAttrMap();
        PvAttrWrapper pvAttr = pvAttrMap.get(label);
        StudyPvAttrWrapper studyPvAttr = null;
        if (pvAttr != null) {
            studyPvAttr = pvAttr.getStudyPvAttr();
        } else {
            studyPvAttr = studyPvAttrMap.get(label);
            // make sure "label" is a valid study pv attr
            if (studyPvAttr == null) {
                throw new Exception("PvAttr for label \"" + label
                    + "\" does not exist");
            }
        }
        String permissible = studyPvAttr.getPermissible();
        if (permissible == null) {
            return null;
        }
        return permissible.split(";");
    }

    /**
     * Assigns a value to a patient visit attribute. The value is parsed for
     * correctness.
     * 
     * @param label The attribute's label.
     * @param value The value to assign.
     * @throws Exception when assigning a label of type "select_single" or
     *             "select_multiple" and the value is not one of the permissible
     *             ones.
     * @throws NumberFormatException when assigning a label of type "number" and
     *             the value is not a valid double number.
     * @throws ParseException when assigning a label of type "date_time" and the
     *             value is not a valid date and time.
     * @see edu.ualberta.med.biobank
     *      .common.formatters.DateFormatter.DATE_TIME_FORMAT
     */
    public void setPvAttrValue(String label, String value) throws Exception {
        getPvAttrMap();
        PvAttrWrapper pvAttr = pvAttrMap.get(label);
        StudyPvAttrWrapper studyPvAttr = null;

        if (pvAttr != null) {
            studyPvAttr = pvAttr.getStudyPvAttr();
        } else {
            studyPvAttr = studyPvAttrMap.get(label);
            if (studyPvAttr == null) {
                throw new Exception("no StudyPvAttr found for label \"" + label
                    + "\"");
            }
        }

        if (studyPvAttr.getLocked().equals(true)) {
            throw new Exception("attribute for label \"" + label
                + "\" is locked, changes not premitted");
        }

        if (value != null) {
            // validate the value
            value = value.trim();
            String type = studyPvAttr.getPvAttrType().getName();
            List<String> permissibleSplit = null;

            if (type.equals("select_single") || type.equals("select_multiple")) {
                String permissible = studyPvAttr.getPermissible();
                if (permissible != null) {
                    permissibleSplit = Arrays.asList(permissible.split(";"));
                }
            }

            if (type.equals("select_single")) {
                if (!permissibleSplit.contains(value)) {
                    throw new Exception("value " + value
                        + "is invalid for label \"" + label + "\"");
                }
            } else if (type.equals("select_multiple")) {
                if (value.length() > 0) {
                    for (String singleVal : value.split(";")) {
                        if (!permissibleSplit.contains(singleVal)) {
                            throw new Exception("value " + singleVal + " ("
                                + value + ") is invalid for label \"" + label
                                + "\"");
                        }
                    }
                }
            } else if (type.equals("number")) {
                Double.parseDouble(value);
            } else if (type.equals("date_time")) {
                DateFormatter.dateFormatter.parse(value);
            } else if (type.equals("text")) {
                // do nothing
            } else {
                throw new Exception("type \"" + type + "\" not tested");
            }
        }

        pvAttr = new PvAttrWrapper(appService, new PvAttr());
        pvAttr.setPatientVisit(this);
        pvAttr.setStudyPvAttr(studyPvAttr);
        pvAttr.setValue(value);
        pvAttrMap.put(label, pvAttr);
    }

    public void setDateProcessed(Date date) {
        Date oldDate = getDateProcessed();
        wrappedObject.setDateProcessed(date);
        propertyChangeSupport
            .firePropertyChange("dateProcessed", oldDate, date);
    }

    public void setComment(String comment) {
        String oldComment = getComment();
        wrappedObject.setComment(comment);
        propertyChangeSupport
            .firePropertyChange("comment", oldComment, comment);
    }

    @Override
    protected void persistChecks() throws BiobankCheckException,
        ApplicationException, WrapperException {
        checkHasShipment();
        checkPatientInShipment();
        // patient to clinic relationship tested by shipment, so no need to
        // test it again here
    }

    private void checkHasShipment() throws BiobankCheckException {
        if (getShipment() == null) {
            throw new BiobankCheckException(
                "This visit should contain a shipment");
        }
    }

    private void checkPatientInShipment() throws BiobankCheckException {
        List<PatientWrapper> shipmentPatients = getShipment()
            .getPatientCollection();
        if (shipmentPatients == null
            || !shipmentPatients.contains(getPatient())) {
            throw new BiobankCheckException(
                "The patient should be part of the shipment");
        }
    }

    @Override
    protected void persistDependencies(PatientVisit origObject)
        throws Exception {
        if (pvAttrMap != null) {
            setPvAttrCollection(pvAttrMap.values());
        }
        deletedPvSampleSources();
    }

    private void deletedPvSampleSources() throws Exception {
        for (PvSampleSourceWrapper ss : deletedPvSampleSources) {
            if (!ss.isNew()) {
                ss.delete();
            }
        }
    }

    public String getUsername() {
        return wrappedObject.getUsername();
    }

    public void setUsername(String username) {
        String oldUsername = wrappedObject.getUsername();
        wrappedObject.setUsername(username);
        propertyChangeSupport.firePropertyChange("username", oldUsername,
            username);
    }

    public ShipmentWrapper getShipment() {
        Shipment s = wrappedObject.getShipment();
        if (s == null) {
            return null;
        }
        return new ShipmentWrapper(appService, s);
    }

    public void setShipment(Shipment s) {
        ShipmentWrapper oldShipment = getShipment();
        wrappedObject.setShipment(s);
        propertyChangeSupport.firePropertyChange("shipment", oldShipment, s);
    }

    public void setShipment(ShipmentWrapper s) {
        setShipment(s.wrappedObject);
    }

    @SuppressWarnings("unchecked")
    public List<PvSampleSourceWrapper> getPvSampleSourceCollection(boolean sort) {
        List<PvSampleSourceWrapper> pvSampleSourceCollection = (List<PvSampleSourceWrapper>) propertiesMap
            .get("pvSampleSourceCollection");
        if (pvSampleSourceCollection == null) {
            Collection<PvSampleSource> children = wrappedObject
                .getPvSampleSourceCollection();
            if (children != null) {
                pvSampleSourceCollection = new ArrayList<PvSampleSourceWrapper>();
                for (PvSampleSource pvSampleSource : children) {
                    pvSampleSourceCollection.add(new PvSampleSourceWrapper(
                        appService, pvSampleSource));
                }
                propertiesMap.put("pvSampleSourceCollection",
                    pvSampleSourceCollection);
            }
        }
        if ((pvSampleSourceCollection != null) && sort)
            Collections.sort(pvSampleSourceCollection);
        return pvSampleSourceCollection;
    }

    public List<PvSampleSourceWrapper> getPvSampleSourceCollection() {
        return getPvSampleSourceCollection(false);
    }

    public void addPvSampleSources(
        Collection<PvSampleSourceWrapper> newPvSampleSources) {
        Collection<PvSampleSource> allPvObjects = new HashSet<PvSampleSource>();
        List<PvSampleSourceWrapper> allPvWrappers = new ArrayList<PvSampleSourceWrapper>();
        // already added
        List<PvSampleSourceWrapper> currentList = getPvSampleSourceCollection();
        if (currentList != null) {
            for (PvSampleSourceWrapper pvss : currentList) {
                allPvObjects.add(pvss.getWrappedObject());
                allPvWrappers.add(pvss);
            }
        }
        // new added
        for (PvSampleSourceWrapper pvss : newPvSampleSources) {
            allPvObjects.add(pvss.getWrappedObject());
            allPvWrappers.add(pvss);
        }
        setPvSamplSources(allPvObjects, allPvWrappers);
    }

    private void setPvSamplSources(Collection<PvSampleSource> allPvObjects,
        List<PvSampleSourceWrapper> allPvWrappers) {
        Collection<PvSampleSource> oldCollection = wrappedObject
            .getPvSampleSourceCollection();
        wrappedObject.setPvSampleSourceCollection(allPvObjects);
        propertyChangeSupport.firePropertyChange("pvSampleSourceCollection",
            oldCollection, allPvObjects);
        propertiesMap.put("pvSampleSourceCollection", allPvWrappers);
    }

    public void removePvSampleSources(
        Collection<PvSampleSourceWrapper> pvSampleSourcesToRemove) {
        deletedPvSampleSources.addAll(pvSampleSourcesToRemove);
        Collection<PvSampleSource> allPvObjects = new HashSet<PvSampleSource>();
        List<PvSampleSourceWrapper> allPvWrappers = new ArrayList<PvSampleSourceWrapper>();
        // already added
        List<PvSampleSourceWrapper> currentList = getPvSampleSourceCollection();
        if (currentList != null) {
            for (PvSampleSourceWrapper pvss : currentList) {
                if (!deletedPvSampleSources.contains(pvss)) {
                    allPvObjects.add(pvss.getWrappedObject());
                    allPvWrappers.add(pvss);
                }
            }
        }
        setPvSamplSources(allPvObjects, allPvWrappers);
    }

    @Override
    public Class<PatientVisit> getWrappedClass() {
        return PatientVisit.class;
    }

    @Override
    protected void deleteChecks() throws BiobankCheckException,
        ApplicationException {
        if (hasSamples()) {
            throw new BiobankCheckException("Unable to delete patient visit "
                + getDateProcessed()
                + " since it has samples stored in database.");
        }
    }

    public boolean hasSamples() throws ApplicationException,
        BiobankCheckException {
        String queryString = "select count(samples) from "
            + Patient.class.getName() + " as p"
            + " left join p.patientVisitCollection as visits"
            + " left join visits.sampleCollection as samples"
            + " where p = ? and visits = ?)";
        HQLCriteria c = new HQLCriteria(queryString, Arrays
            .asList(new Object[] { wrappedObject.getPatient(), wrappedObject }));
        List<Long> results = appService.query(c);
        if (results.size() != 1) {
            throw new BiobankCheckException("Invalid size for HQL query result");
        }
        return results.get(0) > 0;
    }

    @Override
    public int compareTo(ModelWrapper<PatientVisit> wrapper) {
        if (wrapper instanceof PatientVisitWrapper) {
            Date v1Date = wrappedObject.getDateProcessed();
            Date v2Date = wrapper.wrappedObject.getDateProcessed();
            if (v1Date != null && v2Date != null) {
                return v1Date.compareTo(v2Date);
            }
        }
        return 0;
    }

    @Override
    public void resetInternalField() {
        pvAttrMap = null;
        studyPvAttrMap = null;
        deletedPvSampleSources.clear();
    }

    @Override
    public String toString() {
        return getFormattedDateProcessed();
    }
}

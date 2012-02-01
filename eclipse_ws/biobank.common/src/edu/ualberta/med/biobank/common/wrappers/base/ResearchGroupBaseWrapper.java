/*
 * This code is automatically generated. Please do not edit.
 */

package edu.ualberta.med.biobank.common.wrappers.base;

import java.util.List;
import gov.nih.nci.system.applicationservice.WritableApplicationService;
import edu.ualberta.med.biobank.model.ResearchGroup;
import edu.ualberta.med.biobank.common.wrappers.Property;
import java.util.ArrayList;
import edu.ualberta.med.biobank.common.peer.ResearchGroupPeer;
import edu.ualberta.med.biobank.common.wrappers.CenterWrapper;
import edu.ualberta.med.biobank.common.wrappers.StudyWrapper;
import edu.ualberta.med.biobank.common.wrappers.base.StudyBaseWrapper;
import edu.ualberta.med.biobank.common.exception.BiobankCheckException;
import edu.ualberta.med.biobank.common.wrappers.RequestWrapper;
import edu.ualberta.med.biobank.common.wrappers.base.RequestBaseWrapper;

public abstract class ResearchGroupBaseWrapper extends CenterWrapper<ResearchGroup> {

    public ResearchGroupBaseWrapper(WritableApplicationService appService) {
        super(appService);
    }

    public ResearchGroupBaseWrapper(WritableApplicationService appService,
        ResearchGroup wrappedObject) {
        super(appService, wrappedObject);
    }

    @Override
    public final Class<ResearchGroup> getWrappedClass() {
        return ResearchGroup.class;
    }

    @Override
    public Property<Integer, ? super ResearchGroup> getIdProperty() {
        return ResearchGroupPeer.ID;
    }

    @Override
    protected List<Property<?, ? super ResearchGroup>> getProperties() {
        List<Property<?, ? super ResearchGroup>> superNames = super.getProperties();
        List<Property<?, ? super ResearchGroup>> all = new ArrayList<Property<?, ? super ResearchGroup>>();
        all.addAll(superNames);
        all.addAll(ResearchGroupPeer.PROPERTIES);
        return all;
    }

    public StudyWrapper getStudy() {
        boolean notCached = !isPropertyCached(ResearchGroupPeer.STUDY);
        StudyWrapper study = getWrappedProperty(ResearchGroupPeer.STUDY, StudyWrapper.class);
        if (study != null && notCached) ((StudyBaseWrapper) study).setResearchGroupInternal(this);
        return study;
    }

    public void setStudy(StudyBaseWrapper study) {
        if (isInitialized(ResearchGroupPeer.STUDY)) {
            StudyBaseWrapper oldStudy = getStudy();
            if (oldStudy != null) oldStudy.setResearchGroupInternal(null);
        }
        if (study != null) study.setResearchGroupInternal(this);
        setWrappedProperty(ResearchGroupPeer.STUDY, study);
    }

    void setStudyInternal(StudyBaseWrapper study) {
        setWrappedProperty(ResearchGroupPeer.STUDY, study);
    }

    public List<RequestWrapper> getRequestCollection(boolean sort) {
        boolean notCached = !isPropertyCached(ResearchGroupPeer.REQUEST_COLLECTION);
        List<RequestWrapper> requestCollection = getWrapperCollection(ResearchGroupPeer.REQUEST_COLLECTION, RequestWrapper.class, sort);
        if (notCached) {
            for (RequestBaseWrapper e : requestCollection) {
                e.setResearchGroupInternal(this);
            }
        }
        return requestCollection;
    }

    public void addToRequestCollection(List<? extends RequestBaseWrapper> requestCollection) {
        addToWrapperCollection(ResearchGroupPeer.REQUEST_COLLECTION, requestCollection);
        for (RequestBaseWrapper e : requestCollection) {
            e.setResearchGroupInternal(this);
        }
    }

    void addToRequestCollectionInternal(List<? extends RequestBaseWrapper> requestCollection) {
        if (isInitialized(ResearchGroupPeer.REQUEST_COLLECTION)) {
            addToWrapperCollection(ResearchGroupPeer.REQUEST_COLLECTION, requestCollection);
        } else {
            getElementQueue().add(ResearchGroupPeer.REQUEST_COLLECTION, requestCollection);
        }
    }

    public void removeFromRequestCollection(List<? extends RequestBaseWrapper> requestCollection) {
        removeFromWrapperCollection(ResearchGroupPeer.REQUEST_COLLECTION, requestCollection);
        for (RequestBaseWrapper e : requestCollection) {
            e.setResearchGroupInternal(null);
        }
    }

    void removeFromRequestCollectionInternal(List<? extends RequestBaseWrapper> requestCollection) {
        if (isPropertyCached(ResearchGroupPeer.REQUEST_COLLECTION)) {
            removeFromWrapperCollection(ResearchGroupPeer.REQUEST_COLLECTION, requestCollection);
        } else {
            getElementQueue().remove(ResearchGroupPeer.REQUEST_COLLECTION, requestCollection);
        }
    }

    public void removeFromRequestCollectionWithCheck(List<? extends RequestBaseWrapper> requestCollection) throws BiobankCheckException {
        removeFromWrapperCollectionWithCheck(ResearchGroupPeer.REQUEST_COLLECTION, requestCollection);
        for (RequestBaseWrapper e : requestCollection) {
            e.setResearchGroupInternal(null);
        }
    }

    void removeFromRequestCollectionWithCheckInternal(List<? extends RequestBaseWrapper> requestCollection) throws BiobankCheckException {
        removeFromWrapperCollectionWithCheck(ResearchGroupPeer.REQUEST_COLLECTION, requestCollection);
    }

}

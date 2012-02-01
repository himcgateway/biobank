/*
 * This code is automatically generated. Please do not edit.
 */

package edu.ualberta.med.biobank.common.wrappers.base;

import java.util.List;
import gov.nih.nci.system.applicationservice.WritableApplicationService;
import edu.ualberta.med.biobank.model.EventAttr;
import edu.ualberta.med.biobank.common.wrappers.Property;
import edu.ualberta.med.biobank.common.wrappers.ModelWrapper;
import edu.ualberta.med.biobank.common.peer.EventAttrPeer;
import edu.ualberta.med.biobank.common.wrappers.internal.StudyEventAttrWrapper;
import edu.ualberta.med.biobank.common.wrappers.base.StudyEventAttrBaseWrapper;
import edu.ualberta.med.biobank.common.wrappers.CollectionEventWrapper;
import edu.ualberta.med.biobank.common.wrappers.base.CollectionEventBaseWrapper;
import java.util.Arrays;

public class EventAttrBaseWrapper extends ModelWrapper<EventAttr> {

    public EventAttrBaseWrapper(WritableApplicationService appService) {
        super(appService);
    }

    public EventAttrBaseWrapper(WritableApplicationService appService,
        EventAttr wrappedObject) {
        super(appService, wrappedObject);
    }

    @Override
    public final Class<EventAttr> getWrappedClass() {
        return EventAttr.class;
    }

    @Override
    public Property<Integer, ? super EventAttr> getIdProperty() {
        return EventAttrPeer.ID;
    }

    @Override
    protected List<Property<?, ? super EventAttr>> getProperties() {
        return EventAttrPeer.PROPERTIES;
    }

    public String getValue() {
        return getProperty(EventAttrPeer.VALUE);
    }

    public void setValue(String value) {
        String trimmed = value == null ? null : value.trim();
        setProperty(EventAttrPeer.VALUE, trimmed);
    }

    public StudyEventAttrWrapper getStudyEventAttr() {
        StudyEventAttrWrapper studyEventAttr = getWrappedProperty(EventAttrPeer.STUDY_EVENT_ATTR, StudyEventAttrWrapper.class);
        return studyEventAttr;
    }

    public void setStudyEventAttr(StudyEventAttrBaseWrapper studyEventAttr) {
        setWrappedProperty(EventAttrPeer.STUDY_EVENT_ATTR, studyEventAttr);
    }

    void setStudyEventAttrInternal(StudyEventAttrBaseWrapper studyEventAttr) {
        setWrappedProperty(EventAttrPeer.STUDY_EVENT_ATTR, studyEventAttr);
    }

    public CollectionEventWrapper getCollectionEvent() {
        boolean notCached = !isPropertyCached(EventAttrPeer.COLLECTION_EVENT);
        CollectionEventWrapper collectionEvent = getWrappedProperty(EventAttrPeer.COLLECTION_EVENT, CollectionEventWrapper.class);
        if (collectionEvent != null && notCached) ((CollectionEventBaseWrapper) collectionEvent).addToEventAttrCollectionInternal(Arrays.asList(this));
        return collectionEvent;
    }

    public void setCollectionEvent(CollectionEventBaseWrapper collectionEvent) {
        if (isInitialized(EventAttrPeer.COLLECTION_EVENT)) {
            CollectionEventBaseWrapper oldCollectionEvent = getCollectionEvent();
            if (oldCollectionEvent != null) oldCollectionEvent.removeFromEventAttrCollectionInternal(Arrays.asList(this));
        }
        if (collectionEvent != null) collectionEvent.addToEventAttrCollectionInternal(Arrays.asList(this));
        setWrappedProperty(EventAttrPeer.COLLECTION_EVENT, collectionEvent);
    }

    void setCollectionEventInternal(CollectionEventBaseWrapper collectionEvent) {
        setWrappedProperty(EventAttrPeer.COLLECTION_EVENT, collectionEvent);
    }

}

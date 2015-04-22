package edu.ualberta.med.biobank.test.action;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;

import org.hibernate.Transaction;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import edu.ualberta.med.biobank.common.action.clinic.ClinicGetInfoAction;
import edu.ualberta.med.biobank.common.action.clinic.ClinicGetInfoAction.ClinicInfo;
import edu.ualberta.med.biobank.common.action.collectionEvent.CollectionEventGetInfoAction;
import edu.ualberta.med.biobank.common.action.collectionEvent.CollectionEventGetInfoAction.CEventInfo;
import edu.ualberta.med.biobank.common.action.collectionEvent.CollectionEventGetSourceSpecimenListInfoAction;
import edu.ualberta.med.biobank.common.action.collectionEvent.CollectionEventGetSourceSpecimensAction;
import edu.ualberta.med.biobank.common.action.exception.ModelNotFoundException;
import edu.ualberta.med.biobank.common.action.info.OriginInfoSaveInfo;
import edu.ualberta.med.biobank.common.action.info.ShipmentInfoSaveInfo;
import edu.ualberta.med.biobank.common.action.info.SiteInfo;
import edu.ualberta.med.biobank.common.action.originInfo.OriginInfoSaveAction;
import edu.ualberta.med.biobank.common.action.patient.PatientGetInfoAction;
import edu.ualberta.med.biobank.common.action.patient.PatientGetInfoAction.PatientInfo;
import edu.ualberta.med.biobank.common.action.processingEvent.ProcessingEventBriefInfo;
import edu.ualberta.med.biobank.common.action.processingEvent.ProcessingEventDeleteAction;
import edu.ualberta.med.biobank.common.action.processingEvent.ProcessingEventGetBriefInfoAction;
import edu.ualberta.med.biobank.common.action.processingEvent.ProcessingEventGetInfoAction;
import edu.ualberta.med.biobank.common.action.processingEvent.ProcessingEventGetInfoAction.PEventInfo;
import edu.ualberta.med.biobank.common.action.processingEvent.ProcessingEventSaveAction;
import edu.ualberta.med.biobank.common.action.scanprocess.GetProcessingEventsAction;
import edu.ualberta.med.biobank.common.action.search.PEventByWSSearchAction;
import edu.ualberta.med.biobank.common.action.site.SiteGetInfoAction;
import edu.ualberta.med.biobank.common.action.specimen.SpecimenInfo;
import edu.ualberta.med.biobank.common.action.specimen.SpecimenLinkSaveAction;
import edu.ualberta.med.biobank.common.action.specimen.SpecimenLinkSaveAction.AliquotedSpecimenInfo;
import edu.ualberta.med.biobank.model.ActivityStatus;
import edu.ualberta.med.biobank.model.CollectionEvent;
import edu.ualberta.med.biobank.model.Patient;
import edu.ualberta.med.biobank.model.ProcessingEvent;
import edu.ualberta.med.biobank.model.Site;
import edu.ualberta.med.biobank.model.Specimen;
import edu.ualberta.med.biobank.model.Study;
import edu.ualberta.med.biobank.test.Utils;
import edu.ualberta.med.biobank.test.action.helper.CollectionEventHelper;
import edu.ualberta.med.biobank.test.action.helper.SiteHelper.Provisioning;

public class TestProcessingEvent extends TestAction {

    @Rule
    public TestName testname = new TestName();

    private Provisioning provisioning;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        session.beginTransaction();
        provisioning = new Provisioning(session, factory);
        session.getTransaction().commit();
    }

    @Test
    public void saveWithoutSpecimens() throws Exception {
        String worksheet = Utils.getRandomString(20, 50);
        String commentText = Utils.getRandomString(20, 30);
        Date date = Utils.getRandomDate();

        Site site = (Site) session.load(Site.class, provisioning.siteId);

        Integer pEventId = exec(new ProcessingEventSaveAction(
            null, site, date, worksheet, ActivityStatus.ACTIVE, commentText,
            new HashSet<Integer>(), new HashSet<Integer>())).getId();

        // Check ProcessingEvent is in database with correct values
        ProcessingEvent pevent = (ProcessingEvent)
            session.get(ProcessingEvent.class, pEventId);

        PEventInfo peventInfo =
            exec(new ProcessingEventGetInfoAction(pevent));

        Assert.assertEquals(pevent.getWorksheet(),
            peventInfo.pevent.getWorksheet());
        Assert.assertEquals(1, peventInfo.pevent.getComments().size());
        Assert.assertEquals(pevent.getCreatedAt(),
            peventInfo.pevent.getCreatedAt());
        Assert
            .assertEquals(0, peventInfo.sourceSpecimenInfos.size());
    }

    @Test
    public void saveWithSpecimens() throws Exception {
        String worksheet = Utils.getRandomString(50);
        String commentText = Utils.getRandomString(20, 30);
        Date date = Utils.getRandomDate();

        // create a collection event from a clinic
        Integer ceventId = CollectionEventHelper
            .createCEventWithSourceSpecimens(getExecutor(),
                provisioning.patientIds.get(0), provisioning.getClinic());
        CEventInfo ceventInfo =
            exec(new CollectionEventGetInfoAction(ceventId));
        List<SpecimenInfo> sourceSpecs = ceventInfo.sourceSpecimenInfos;

        // ship the specimens to the site
        OriginInfoSaveInfo oiSaveInfo = new OriginInfoSaveInfo(
            null, provisioning.siteId, provisioning.clinicId, null,
            SpecimenInfo.getSpecimenIds(sourceSpecs), null);
        ShipmentInfoSaveInfo shipSaveInfo = new ShipmentInfoSaveInfo(
            null, Utils.getRandomString(2, 5), Utils.getRandomDate(),
            Utils.getRandomDate(), Utils.getRandomString(2, 5),
            getShippingMethods().get(0).getId());
        getExecutor()
            .exec(new OriginInfoSaveAction(oiSaveInfo, shipSaveInfo));

        // create a processing event with one of the collection event source
        // specimen
        Site site = (Site) session.load(Site.class, provisioning.siteId);
        Integer pEventId = exec(new ProcessingEventSaveAction(
            null, site, date, worksheet, ActivityStatus.ACTIVE,
            commentText, new HashSet<Integer>(
                Arrays.asList(sourceSpecs.get(0).specimen.getId())),
            new HashSet<Integer>())).getId();

        // sourceSpecs.get(0) will have its activity status set to closed now
        sourceSpecs.get(0).specimen.setActivityStatus(ActivityStatus.CLOSED);

        // create aliquoted specimens by doing a scan link
        Set<AliquotedSpecimenInfo> aliquotedSpecimenInfos =
            new HashSet<AliquotedSpecimenInfo>();
        for (int i = 0, n = getR().nextInt(5) + 1; i < n; i++) {
            AliquotedSpecimenInfo aliquotedSpecimenInfo =
                new AliquotedSpecimenInfo();
            aliquotedSpecimenInfo.inventoryId = Utils.getRandomString(10, 15);
            aliquotedSpecimenInfo.typeId = getSpecimenTypes().get(0).getId();
            aliquotedSpecimenInfo.activityStatus = ActivityStatus.ACTIVE;
            aliquotedSpecimenInfo.parentSpecimenId =
                sourceSpecs.get(0).specimen.getId();
            aliquotedSpecimenInfo.containerId = null;
            aliquotedSpecimenInfo.position = null;
            aliquotedSpecimenInfos.add(aliquotedSpecimenInfo);
        }

        exec(new SpecimenLinkSaveAction(provisioning.siteId,
            provisioning.studyId, aliquotedSpecimenInfos));

        ProcessingEvent pevent = (ProcessingEvent)
            session.get(ProcessingEvent.class, pEventId);

        // Check ProcessingEvent is in database with correct values
        PEventInfo peventInfo =
            exec(new ProcessingEventGetInfoAction(pevent));

        Assert.assertEquals(worksheet, peventInfo.pevent.getWorksheet());
        Assert.assertEquals(1, peventInfo.pevent.getComments().size());
        Assert.assertEquals(date, peventInfo.pevent.getCreatedAt());
        Assert
            .assertEquals(1, peventInfo.sourceSpecimenInfos.size());

        ClinicInfo clinicInfo =
            exec(new ClinicGetInfoAction(provisioning.clinicId));
        SiteInfo siteInfo =
            exec(new SiteGetInfoAction(provisioning.siteId));
        PatientInfo patientInfo =
            exec(new PatientGetInfoAction(provisioning.patientIds
                .get(0)));

        // check eager loaded associations
        for (SpecimenInfo specimenInfo : peventInfo.sourceSpecimenInfos) {
            Assert.assertEquals(sourceSpecs.get(0).specimen.getSpecimenType()
                .getName(), specimenInfo.specimen.getSpecimenType().getName());

            Assert.assertEquals(
                ActivityStatus.CLOSED,
                specimenInfo.specimen.getActivityStatus());

            Assert.assertEquals(clinicInfo.clinic.getName(),
                specimenInfo.specimen.getOriginInfo().getCenter().getName());

            Assert.assertEquals(siteInfo.getSite().getName(),
                specimenInfo.specimen.getCurrentCenter().getName());

            Assert.assertEquals(patientInfo.patient.getPnumber(),
                specimenInfo.specimen.getCollectionEvent().getPatient()
                    .getPnumber());

            Assert.assertEquals(patientInfo.patient.getStudy().getName(),
                specimenInfo.specimen.getCollectionEvent().getPatient()
                    .getStudy().getName());

        }
    }

    @Test
    public void delete() throws Exception {
        Site site = (Site) session.load(Site.class, provisioning.siteId);

        Integer pEventId = exec(new ProcessingEventSaveAction(
            null, site, Utils.getRandomDate(), Utils
                .getRandomString(50), ActivityStatus.ACTIVE, null,
            new HashSet<Integer>(), new HashSet<Integer>())).getId();
        ProcessingEvent pevent = (ProcessingEvent)
            session.get(ProcessingEvent.class, pEventId);

        PEventInfo peventInfo =
            exec(new ProcessingEventGetInfoAction(pevent));
        exec(new ProcessingEventDeleteAction(peventInfo.pevent));

        try {
            exec(new ProcessingEventGetInfoAction(pevent));
            Assert
                .fail("one of the source specimen of this pevent has children. "
                    + "Can't delete the processing event");
        } catch (ModelNotFoundException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void deleteWithSourcesSpecimens() throws Exception {
        // add cevent and source specimens
        Integer ceventId = CollectionEventHelper
            .createCEventWithSourceSpecimens(getExecutor(),
                provisioning.patientIds.get(0), provisioning.getSite());
        CEventInfo ceventInfo =
            exec(new CollectionEventGetInfoAction(ceventId));
        List<SpecimenInfo> sourceSpecs = ceventInfo.sourceSpecimenInfos;
        Integer spcId = sourceSpecs.get(0).specimen.getId();

        // create a processing event with one of the collection event source
        // specimen.
        Site site = (Site) session.load(Site.class, provisioning.siteId);
        Integer pEventId = exec(new ProcessingEventSaveAction(
            null, site, Utils.getRandomDate(), Utils
                .getRandomString(50), ActivityStatus.ACTIVE, null,
            new HashSet<Integer>(Arrays.asList(spcId)),
            new HashSet<Integer>())).getId();

        Specimen spc = (Specimen) session.load(Specimen.class, spcId);
        Assert.assertNotNull(spc);
        Assert.assertNotNull(spc.getProcessingEvent());
        Assert.assertEquals(pEventId, spc.getProcessingEvent().getId());

        // delete this processing event. Can do it since the specimen has no
        // children
        ProcessingEvent pevent = (ProcessingEvent)
            session.get(ProcessingEvent.class, pEventId);

        PEventInfo peventInfo =
            exec(new ProcessingEventGetInfoAction(pevent));
        exec(new ProcessingEventDeleteAction(peventInfo.pevent));

        try {
            exec(new ProcessingEventGetInfoAction(pevent));
            Assert.fail("processing event still exists");
        } catch (ModelNotFoundException e) {
            Assert.assertTrue(true);
        }

        session.clear();
        spc = (Specimen) session.load(Specimen.class, spcId);
        session.refresh(spc);
        Assert.assertNotNull(spc);
        Assert.assertNull(spc.getProcessingEvent());
    }

    @Test
    public void getCeventSouceSpecimenInfoEmpty() {
        session.beginTransaction();
        CollectionEvent cevent = factory.createCollectionEvent();
        session.getTransaction().commit();

        List<SpecimenInfo> specimenData = exec(
            new CollectionEventGetSourceSpecimenListInfoAction(cevent.getId())).getList();
        Assert.assertEquals(0, specimenData.size());
    }

    @Test
    public void getCeventSouceSpecimenInfo() {
        session.beginTransaction();
        CollectionEvent cevent = factory.createCollectionEvent();
        Set<Specimen> specimens = new HashSet<Specimen>();
        specimens.add(factory.createParentSpecimen());
        specimens.add(factory.createParentSpecimen());
        specimens.add(factory.createParentSpecimen());
        session.getTransaction().commit();

        List<SpecimenInfo> specimensData = exec(
            new CollectionEventGetSourceSpecimenListInfoAction(cevent.getId())).getList();
        Assert.assertEquals(specimens.size(), specimensData.size());

        for (SpecimenInfo spcInfo : specimensData) {
            Assert.assertTrue(specimens.contains(spcInfo.specimen));
        }
    }

    @Test
    public void getCeventSouceSpecimensEmpty() {
        session.beginTransaction();
        CollectionEvent cevent = factory.createCollectionEvent();
        ProcessingEvent pevent = factory.createProcessingEvent();
        session.getTransaction().commit();

        List<Specimen> specimens = exec(
            new CollectionEventGetSourceSpecimensAction(cevent, pevent)).getList();
        Assert.assertEquals(0, specimens.size());
    }

    @Test
    public void getCeventSouceSpecimen() {
        session.beginTransaction();
        CollectionEvent cevent = factory.createCollectionEvent();
        ProcessingEvent pevent = factory.createProcessingEvent();

        Set<Specimen> parentSpecimens = new HashSet<Specimen>();
        Set<Specimen> childSpecimens = new HashSet<Specimen>();

        for (int i = 0; i < 3; ++i) {
            parentSpecimens.add(factory.createParentSpecimen());
            childSpecimens.add(factory.createChildSpecimen());
        }
        session.getTransaction().commit();

        List<Specimen> specimens = exec(
            new CollectionEventGetSourceSpecimensAction(cevent, pevent)).getList();
        Assert.assertEquals(parentSpecimens.size(), specimens.size());
        Assert.assertTrue(specimens.containsAll(parentSpecimens));
    }

    @Test
    public void getCeventSouceSpecimenWithFlagged() {
        session.beginTransaction();
        CollectionEvent cevent = factory.createCollectionEvent();
        ProcessingEvent pevent = factory.createProcessingEvent();

        Set<Specimen> parentSpecimens = new HashSet<Specimen>();
        Set<Specimen> childSpecimens = new HashSet<Specimen>();

        // set the second parent specimen to have a FLAGGED activity status
        for (int i = 0; i < 3; ++i) {
            Specimen parentSpecimen = factory.createParentSpecimen();
            if (i == 2) {
                parentSpecimen.setActivityStatus(ActivityStatus.FLAGGED);
            }

            parentSpecimens.add(parentSpecimen);
            childSpecimens.add(factory.createChildSpecimen());
        }
        session.getTransaction().commit();

        List<Specimen> specimens = exec(
            new CollectionEventGetSourceSpecimensAction(cevent, pevent, true)).getList();
        Assert.assertEquals(parentSpecimens.size() - 1, specimens.size());

        for (Specimen parentSpecimen : parentSpecimens) {
            if (parentSpecimen.getActivityStatus() == ActivityStatus.FLAGGED) continue;
            Assert.assertTrue(specimens.contains(parentSpecimen));
        }
    }

    @Test
    public void deleteWithAliquotedSpecimens() throws Exception {
        session.beginTransaction();
        factory.createCollectionEvent();
        factory.createParentSpecimen();
        ProcessingEvent pevent = factory.createProcessingEvent();
        factory.createChildSpecimen();
        session.getTransaction().commit();

        try {
            exec(new ProcessingEventDeleteAction(pevent));
            Assert.fail("the parent specimen of this pevent has children. "
                + "Can't delete the processing event");
        } catch (Exception e) {
            // do nothing
        }

        ProcessingEvent pe = (ProcessingEvent) session.load(ProcessingEvent.class,
            pevent.getId());
        Assert.assertNotNull(pe);
    }

    @Test
    public void peventGetBriefInfoAction() {
        Transaction tx = session.beginTransaction();
        factory.createProcessingEvent();
        Specimen alqSpecimen = factory.createChildSpecimen();
        factory.getDefaultParentSpecimen().getChildSpecimens().add(alqSpecimen);
        tx.commit();

        ProcessingEventBriefInfo peventBriefInfo =
            exec(new ProcessingEventGetBriefInfoAction(factory
                .getDefaultProcessingEvent()));

        Assert.assertEquals(factory
            .getDefaultProcessingEvent(), peventBriefInfo.pevent);
        Assert.assertEquals(factory.getDefaultStudy().getNameShort(),
            peventBriefInfo.studyNameShort);
        Assert.assertEquals(new Long(1), peventBriefInfo.sourceSpcCount);
        Assert.assertEquals(new Long(1), peventBriefInfo.aliquotSpcCount);
    }

    @Test
    public void peventSearchByWorksheet() {
        session.beginTransaction();

        Study study = factory.createStudy();
        factory.createClinic();
        study.getContacts().add(factory.createContact());
        study.getPatients().add(factory.createPatient());
        factory.createCollectionEvent();
        Site site = factory.createSite();
        site.getStudies().add(study);
        Specimen parentSpecimen = factory.createParentSpecimen();
        ProcessingEvent pevent = factory.createProcessingEvent();
        parentSpecimen.setProcessingEvent(pevent);

        session.getTransaction().commit();

        String worksheet = pevent.getWorksheet();

        List<Integer> peventIds =
            exec(new PEventByWSSearchAction(worksheet, site)).getList();

        Assert.assertEquals(1, peventIds.size());
        Assert.assertEquals(pevent.getId(), peventIds.get(0));

        // test delete
        session.beginTransaction();

        pevent.getSpecimens().clear();
        parentSpecimen.setProcessingEvent(null);
        session.delete(pevent);

        session.getTransaction().commit();

        peventIds = exec(new PEventByWSSearchAction(worksheet, site)).getList();
        Assert.assertEquals(0, peventIds.size());
    }

    @Test
    public void getProcessingEvents() {
        session.beginTransaction();

        Study study = factory.createStudy();
        factory.createClinic();
        study.getContacts().add(factory.createContact());
        Patient patient = factory.createPatient();
        study.getPatients().add(patient);
        CollectionEvent cevent = factory.createCollectionEvent();
        Site site = factory.createSite();
        site.getStudies().add(study);
        session.getTransaction().commit();

        List<ProcessingEvent> pevents =
            exec(new GetProcessingEventsAction(patient.getPnumber(), site.getId())).getList();

        session.beginTransaction();

        Specimen parentSpecimen = factory.createParentSpecimen();
        ProcessingEvent pevent = factory.createProcessingEvent();
        parentSpecimen.setProcessingEvent(pevent);

        session.getTransaction().commit();

        pevents = exec(new GetProcessingEventsAction(patient.getPnumber(), site.getId())).getList();

        Assert.assertEquals(1, pevents.size());
        for (ProcessingEvent pe : pevents) {
            Assert.assertEquals(1, pe.getSpecimens().size());
            for (Specimen spc : pe.getSpecimens()) {
                Assert.assertEquals(spc.getCollectionEvent().getId(), cevent.getId());
                Assert.assertEquals(spc.getCollectionEvent().getPatient().getId(), patient.getId());
                Assert.assertEquals(spc.getCollectionEvent().getPatient().getStudy().getId(), study.getId());
            }
        }
    }

    @Test
    public void processingEventsLastSevenDays() {
        session.beginTransaction();

        Study study = factory.createStudy();
        factory.createClinic();
        study.getContacts().add(factory.createContact());
        Patient patient = factory.createPatient();
        study.getPatients().add(patient);
        CollectionEvent cevent = factory.createCollectionEvent();
        Site site = factory.createSite();
        site.getStudies().add(study);
        Specimen parentSpecimen = factory.createParentSpecimen();
        ProcessingEvent pevent = factory.createProcessingEvent();
        parentSpecimen.setProcessingEvent(pevent);

        Calendar cal = Calendar.getInstance();

        // yesterday at midnight
        cal.add(Calendar.DATE, -1);
        cal.set(Calendar.AM_PM, Calendar.AM);
        cal.set(Calendar.HOUR, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        Date date = cal.getTime();

        pevent.setCreatedAt(date);

        session.getTransaction().commit();

        List<ProcessingEvent> pevents =
            exec(new GetProcessingEventsAction(patient.getPnumber(), site.getId(), true)).getList();

        Assert.assertEquals(1, pevents.size());
        for (ProcessingEvent pe : pevents) {
            Assert.assertEquals(1, pe.getSpecimens().size());
            for (Specimen spc : pe.getSpecimens()) {
                Assert.assertEquals(spc.getCollectionEvent().getId(), cevent.getId());
                Assert.assertEquals(spc.getCollectionEvent().getPatient().getId(), patient.getId());
                Assert.assertEquals(spc.getCollectionEvent().getPatient().getStudy().getId(), study.getId());
            }
        }

        // create another pevent, but for 8 days ago, should not be returned by action
        cal.add(Calendar.DATE, -8);
        cal.set(Calendar.AM_PM, Calendar.AM);
        cal.set(Calendar.HOUR, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        date = cal.getTime();

        session.beginTransaction();
        pevent = factory.createProcessingEvent();
        pevent.setCreatedAt(date);
        session.getTransaction().commit();

        pevents = exec(new GetProcessingEventsAction(patient.getPnumber(), site.getId(), true)).getList();

        Assert.assertEquals(1, pevents.size());
    }
}

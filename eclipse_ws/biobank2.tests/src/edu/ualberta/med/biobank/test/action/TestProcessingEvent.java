package edu.ualberta.med.biobank.test.action;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.validation.ConstraintViolationException;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import edu.ualberta.med.biobank.common.action.clinic.ClinicGetInfoAction;
import edu.ualberta.med.biobank.common.action.clinic.ClinicGetInfoAction.ClinicInfo;
import edu.ualberta.med.biobank.common.action.collectionEvent.CollectionEventGetInfoAction;
import edu.ualberta.med.biobank.common.action.collectionEvent.CollectionEventGetInfoAction.CEventInfo;
import edu.ualberta.med.biobank.common.action.collectionEvent.CollectionEventGetSourceSpecimenListInfoAction;
import edu.ualberta.med.biobank.common.action.exception.ModelNotFoundException;
import edu.ualberta.med.biobank.common.action.info.OriginInfoSaveInfo;
import edu.ualberta.med.biobank.common.action.info.ShipmentInfoSaveInfo;
import edu.ualberta.med.biobank.common.action.info.SiteInfo;
import edu.ualberta.med.biobank.common.action.patient.PatientGetInfoAction;
import edu.ualberta.med.biobank.common.action.patient.PatientGetInfoAction.PatientInfo;
import edu.ualberta.med.biobank.common.action.processingEvent.ProcessingEventDeleteAction;
import edu.ualberta.med.biobank.common.action.processingEvent.ProcessingEventGetInfoAction;
import edu.ualberta.med.biobank.common.action.processingEvent.ProcessingEventGetInfoAction.PEventInfo;
import edu.ualberta.med.biobank.common.action.processingEvent.ProcessingEventSaveAction;
import edu.ualberta.med.biobank.common.action.shipment.OriginInfoSaveAction;
import edu.ualberta.med.biobank.common.action.site.SiteGetInfoAction;
import edu.ualberta.med.biobank.common.action.specimen.SpecimenInfo;
import edu.ualberta.med.biobank.common.action.specimen.SpecimenLinkSaveAction;
import edu.ualberta.med.biobank.common.action.specimen.SpecimenLinkSaveAction.AliquotedSpecimenInfo;
import edu.ualberta.med.biobank.model.ActivityStatus;
import edu.ualberta.med.biobank.model.ProcessingEvent;
import edu.ualberta.med.biobank.model.Specimen;
import edu.ualberta.med.biobank.server.applicationservice.exceptions.ModelIsUsedException;
import edu.ualberta.med.biobank.test.Utils;
import edu.ualberta.med.biobank.test.action.helper.CollectionEventHelper;
import edu.ualberta.med.biobank.test.action.helper.SiteHelper.Provisioning;

public class TestProcessingEvent extends TestAction {

    @Rule
    public TestName testname = new TestName();

    private String name;
    private Provisioning provisioning;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        name = getMethodNameR();
        provisioning = new Provisioning(EXECUTOR, name);
    }

    @Test
    public void saveWithoutSpecimens() throws Exception {
        String worksheet = Utils.getRandomString(20, 50);
        String commentText = Utils.getRandomString(20, 30);
        Date date = Utils.getRandomDate();
        Integer pEventId =
            EXECUTOR.exec(
                new ProcessingEventSaveAction(
                    null, provisioning.siteId, date, worksheet,
                    ActivityStatus.ACTIVE, commentText,
                    new HashSet<Integer>())).getId();

        // Check ProcessingEvent is in database with correct values
        PEventInfo peventInfo =
            EXECUTOR.exec(new ProcessingEventGetInfoAction(pEventId));

        Assert.assertEquals(worksheet, peventInfo.pevent.getWorksheet());
        Assert.assertEquals(1, peventInfo.pevent.getComments().size());
        Assert.assertEquals(date, peventInfo.pevent.getCreatedAt());
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
            .createCEventWithSourceSpecimens(EXECUTOR,
                provisioning.patientIds.get(0), provisioning.clinicId);
        CEventInfo ceventInfo =
            EXECUTOR.exec(new CollectionEventGetInfoAction(ceventId));
        List<SpecimenInfo> sourceSpecs = ceventInfo.sourceSpecimenInfos;

        // ship the specimens to the site
        OriginInfoSaveInfo oiSaveInfo = new OriginInfoSaveInfo(
            null, provisioning.siteId, provisioning.clinicId, null,
            SpecimenInfo.getSpecimenIds(sourceSpecs), null);
        ShipmentInfoSaveInfo shipSaveInfo = new ShipmentInfoSaveInfo(
            null, Utils.getRandomString(2, 5), Utils.getRandomDate(),
            Utils.getRandomDate(), Utils.getRandomString(2, 5),
            getShippingMethods().get(0).getId());
        EXECUTOR
            .exec(new OriginInfoSaveAction(oiSaveInfo, shipSaveInfo));

        // create a processing event with one of the collection event source
        // specimen
        Integer pEventId = EXECUTOR.exec(new ProcessingEventSaveAction(
            null, provisioning.siteId, date, worksheet,
            ActivityStatus.ACTIVE, commentText,
            new HashSet<Integer>(
                Arrays.asList(sourceSpecs.get(0).specimen.getId()))))
            .getId();

        // create aliquoted specimens by doing a scan link
        Set<AliquotedSpecimenInfo> aliquotedSpecimenInfos =
            new HashSet<AliquotedSpecimenInfo>();
        for (int i = 0, n = R.nextInt(5) + 1; i < n; i++) {
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

        EXECUTOR.exec(new SpecimenLinkSaveAction(provisioning.siteId,
            provisioning.studyId, aliquotedSpecimenInfos));

        // Check ProcessingEvent is in database with correct values
        PEventInfo peventInfo =
            EXECUTOR.exec(new ProcessingEventGetInfoAction(pEventId));

        Assert.assertEquals(worksheet, peventInfo.pevent.getWorksheet());
        Assert.assertEquals(1, peventInfo.pevent.getComments().size());
        Assert.assertEquals(date, peventInfo.pevent.getCreatedAt());
        Assert
            .assertEquals(1, peventInfo.sourceSpecimenInfos.size());

        ClinicInfo clinicInfo =
            EXECUTOR.exec(new ClinicGetInfoAction(provisioning.clinicId));
        SiteInfo siteInfo =
            EXECUTOR.exec(new SiteGetInfoAction(provisioning.siteId));
        PatientInfo patientInfo =
            EXECUTOR.exec(new PatientGetInfoAction(provisioning.patientIds
                .get(0)));

        // check eager loaded associations
        for (SpecimenInfo specimenInfo : peventInfo.sourceSpecimenInfos) {
            Assert.assertEquals(sourceSpecs.get(0).specimen.getSpecimenType()
                .getName(), specimenInfo.specimen.getSpecimenType().getName());

            Assert.assertEquals(
                sourceSpecs.get(0).specimen.getActivityStatus(),
                specimenInfo.specimen.getActivityStatus());

            Assert.assertEquals(clinicInfo.clinic.getName(),
                specimenInfo.specimen.getOriginInfo().getCenter().getName());

            Assert.assertEquals(siteInfo.site.getName(),
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
    public void saveSameWorksheet() throws Exception {
        String worksheet = Utils.getRandomString(50);
        Date date = Utils.getRandomDate();
        EXECUTOR.exec(new ProcessingEventSaveAction(
            null, provisioning.siteId, date, worksheet,
            ActivityStatus.ACTIVE, null,
            new HashSet<Integer>()));

        // try to save another pevent with the same worksheet
        try {
            EXECUTOR.exec(new ProcessingEventSaveAction(null,
                provisioning.siteId, new Date(), worksheet,
                ActivityStatus.ACTIVE, null,
                new HashSet<Integer>()));
            Assert
                .fail("should not be able to use the same worksheet to 2 different pevents");
        } catch (ConstraintViolationException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void delete() throws Exception {
        Integer pEventId = EXECUTOR.exec(new ProcessingEventSaveAction(
            null, provisioning.siteId, Utils.getRandomDate(), Utils
                .getRandomString(50), ActivityStatus.ACTIVE, null,
            new HashSet<Integer>())).getId();

        EXECUTOR.exec(new ProcessingEventDeleteAction(pEventId));

        try {
            EXECUTOR.exec(new ProcessingEventGetInfoAction(pEventId));
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
            .createCEventWithSourceSpecimens(EXECUTOR,
                provisioning.patientIds.get(0), provisioning.siteId);
        CEventInfo ceventInfo =
            EXECUTOR.exec(new CollectionEventGetInfoAction(ceventId));
        List<SpecimenInfo> sourceSpecs = ceventInfo.sourceSpecimenInfos;
        Integer spcId = sourceSpecs.get(0).specimen.getId();

        // create a processing event with one of the collection event source
        // specimen.
        Integer pEventId = EXECUTOR.exec(new ProcessingEventSaveAction(
            null, provisioning.siteId, Utils.getRandomDate(), Utils
                .getRandomString(50), ActivityStatus.ACTIVE, null,
            new HashSet<Integer>(Arrays.asList(spcId)))).getId();

        Specimen spc = (Specimen) session.load(Specimen.class, spcId);
        Assert.assertNotNull(spc);
        Assert.assertNotNull(spc.getProcessingEvent());
        Assert.assertEquals(pEventId, spc.getProcessingEvent().getId());

        // delete this processing event. Can do it since the specimen has no
        // children
        EXECUTOR.exec(new ProcessingEventDeleteAction(pEventId));

        try {
            EXECUTOR.exec(new ProcessingEventGetInfoAction(pEventId));
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

    @Ignore
    @Test
    /*
     * Need way to create aliquoted specimens
     */
    public void deleteWithAliquotedSpecimens() throws Exception {
        // add cevent and source specimens
        Integer ceventId = CollectionEventHelper
            .createCEventWithSourceSpecimens(EXECUTOR,
                provisioning.patientIds.get(0), provisioning.siteId);
        ArrayList<SpecimenInfo> sourceSpecs = EXECUTOR.exec(
            new CollectionEventGetSourceSpecimenListInfoAction(ceventId))
            .getList();
        Integer spcId = sourceSpecs.get(0).specimen.getId();

        // FIXME need to add a child to the source specimen

        // create a processing event with one of the collection event source
        // specimen.
        Integer pEventId = EXECUTOR.exec(
            new ProcessingEventSaveAction(
                null, provisioning.siteId, Utils.getRandomDate(),
                Utils.getRandomString(50), ActivityStatus.ACTIVE, null,
                new HashSet<Integer>(Arrays.asList(spcId)))).getId();

        Specimen spc = (Specimen) session.load(Specimen.class, spcId);
        Assert.assertNotNull(spc);
        Assert.assertNotNull(spc.getProcessingEvent());
        Assert.assertEquals(pEventId, spc.getProcessingEvent().getId());

        // delete this processing event. Can do it since the specimen has no
        // children
        try {
            EXECUTOR.exec(new ProcessingEventDeleteAction(pEventId));
            Assert
                .fail("one of the source specimen of this pevent has children. "
                    + "Can't delete the processing event");
        } catch (ModelIsUsedException e) {
            Assert.assertTrue(true);
        }

        ProcessingEvent pe =
            (ProcessingEvent) session.load(ProcessingEvent.class,
                pEventId);
        Assert.assertNotNull(pe);
    }
}

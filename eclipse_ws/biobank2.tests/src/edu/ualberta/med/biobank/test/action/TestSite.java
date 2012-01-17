package edu.ualberta.med.biobank.test.action;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Query;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import edu.ualberta.med.biobank.common.action.ListResult;
import edu.ualberta.med.biobank.common.action.activityStatus.ActivityStatusEnum;
import edu.ualberta.med.biobank.common.action.collectionEvent.CollectionEventGetSpecimenInfosAction;
import edu.ualberta.med.biobank.common.action.container.ContainerDeleteAction;
import edu.ualberta.med.biobank.common.action.container.ContainerSaveAction;
import edu.ualberta.med.biobank.common.action.containerType.ContainerTypeDeleteAction;
import edu.ualberta.med.biobank.common.action.containerType.ContainerTypeSaveAction;
import edu.ualberta.med.biobank.common.action.dispatch.DispatchDeleteAction;
import edu.ualberta.med.biobank.common.action.dispatch.DispatchGetSpecimenInfosAction;
import edu.ualberta.med.biobank.common.action.exception.ActionCheckException;
import edu.ualberta.med.biobank.common.action.exception.ModelNotFoundException;
import edu.ualberta.med.biobank.common.action.exception.NullPropertyException;
import edu.ualberta.med.biobank.common.action.info.SiteInfo;
import edu.ualberta.med.biobank.common.action.info.StudyCountInfo;
import edu.ualberta.med.biobank.common.action.processingEvent.ProcessingEventDeleteAction;
import edu.ualberta.med.biobank.common.action.processingEvent.ProcessingEventSaveAction;
import edu.ualberta.med.biobank.common.action.site.SiteDeleteAction;
import edu.ualberta.med.biobank.common.action.site.SiteGetInfoAction;
import edu.ualberta.med.biobank.common.action.site.SiteSaveAction;
import edu.ualberta.med.biobank.common.action.specimen.SpecimenInfo;
import edu.ualberta.med.biobank.common.util.HibernateUtil;
import edu.ualberta.med.biobank.model.Address;
import edu.ualberta.med.biobank.model.DispatchSpecimen;
import edu.ualberta.med.biobank.model.Site;
import edu.ualberta.med.biobank.model.Specimen;
import edu.ualberta.med.biobank.test.Utils;
import edu.ualberta.med.biobank.test.action.helper.CollectionEventHelper;
import edu.ualberta.med.biobank.test.action.helper.ContainerTypeHelper;
import edu.ualberta.med.biobank.test.action.helper.DispatchHelper;
import edu.ualberta.med.biobank.test.action.helper.SiteHelper;
import edu.ualberta.med.biobank.test.action.helper.SiteHelper.Provisioning;
import edu.ualberta.med.biobank.test.action.helper.SpecimenHelper;
import edu.ualberta.med.biobank.test.action.helper.StudyHelper;
import gov.nih.nci.system.applicationservice.ApplicationException;

public class TestSite extends TestAction {

    @Rule
    public TestName testname = new TestName();

    private String name;

    private SiteSaveAction siteSaveAction;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        name = testname.getMethodName() + r.nextInt();

        siteSaveAction =
            SiteHelper.getSaveAction(name, name, ActivityStatusEnum.ACTIVE);
    }

    @Test
    public void saveNew() throws Exception {
        // null name
        siteSaveAction.setName(null);
        try {
            actionExecutor.exec(siteSaveAction);
            Assert.fail(
                "should not be allowed to add site with no name");
        } catch (NullPropertyException e) {
            Assert.assertTrue(true);
        }

        // null short name
        siteSaveAction.setName(name);
        siteSaveAction.setNameShort(null);
        try {
            actionExecutor.exec(siteSaveAction);
            Assert.fail(
                "should not be allowed to add site with no short name");
        } catch (NullPropertyException e) {
            Assert.assertTrue(true);
        }

        siteSaveAction.setNameShort(name);
        siteSaveAction.setActivityStatusId(null);
        try {
            actionExecutor.exec(siteSaveAction);
            Assert.fail(
                "should not be allowed to add Site with no activity status");
        } catch (NullPropertyException e) {
            Assert.assertTrue(true);
        }

        siteSaveAction.setActivityStatusId(
            ActivityStatusEnum.ACTIVE.getId());
        siteSaveAction.setAddress(null);
        try {
            actionExecutor.exec(siteSaveAction);
            Assert.fail(
                "should not be allowed to add site with no address");
        } catch (NullPropertyException e) {
            Assert.assertTrue(true);
        }

        // test invalid act status: 5, -1

        siteSaveAction.setAddress(new Address());
        siteSaveAction.setStudyIds(null);
        try {
            actionExecutor.exec(siteSaveAction);
            Assert.fail(
                "should not be allowed to add site with null site ids");
        } catch (NullPropertyException e) {
            Assert.assertTrue(true);
        }

        // success path
        siteSaveAction.setStudyIds(new HashSet<Integer>());
        actionExecutor.exec(siteSaveAction);
    }

    @Test
    public void nameChecks() throws Exception {
        Integer siteId = actionExecutor.exec(siteSaveAction).getId();

        // ensure we can change name on existing clinic
        SiteInfo siteInfo =
            actionExecutor.exec(new SiteGetInfoAction(siteId));
        siteInfo.site.setName(name + "_2");
        siteSaveAction = SiteHelper.getSaveAction(actionExecutor, siteInfo);
        actionExecutor.exec(siteSaveAction);

        // ensure we can change short name on existing site
        siteInfo = actionExecutor.exec(new SiteGetInfoAction(siteId));
        siteInfo.site.setNameShort(name + "_2");
        siteSaveAction = SiteHelper.getSaveAction(actionExecutor, siteInfo);
        actionExecutor.exec(siteSaveAction);

        // test for duplicate name
        SiteSaveAction saveSite =
            SiteHelper.getSaveAction(name + "_2", name,
                ActivityStatusEnum.ACTIVE);
        try {
            actionExecutor.exec(saveSite);
            Assert.fail("should not be allowed to add site with same name");
        } catch (ActionCheckException e) {
            Assert.assertTrue(true);
        }

        // test for duplicate name short
        saveSite.setName(Utils.getRandomString(5, 10));
        saveSite.setNameShort(name + "_2");

        try {
            actionExecutor.exec(saveSite);
            Assert.fail(
                "should not be allowed to add site with same name short");
        } catch (ActionCheckException e) {
            Assert.assertTrue(true);
        }

    }

    @Test
    public void studyCollection() throws Exception {
        Set<Integer> studyIds;
        List<Integer> allStudyIds = new ArrayList<Integer>();
        Set<Integer> studyIdsSet1 = new HashSet<Integer>();
        Set<Integer> studyIdsSet2 = new HashSet<Integer>();

        for (int i = 0; i < 20; ++i) {
            Integer id = StudyHelper.createStudy(
                actionExecutor, name + "_study" + i, ActivityStatusEnum.ACTIVE);
            allStudyIds.add(id);
            if (i < 10) {
                studyIdsSet1.add(id);
            } else {
                studyIdsSet2.add(id);
            }
        }

        // add study set 1 one by one
        Integer siteId = actionExecutor.exec(siteSaveAction).getId();
        SiteInfo siteInfo =
            actionExecutor.exec(new SiteGetInfoAction(siteId));
        Set<Integer> expectedStudyIds = new HashSet<Integer>();

        for (Integer studyId : studyIdsSet1) {
            expectedStudyIds.add(studyId);

            SiteSaveAction siteSaveAction =
                SiteHelper.getSaveAction(actionExecutor, siteInfo);
            studyIds = getStudyIds(siteInfo.studyCountInfo);
            studyIds.add(studyId);
            siteSaveAction.setStudyIds(studyIds);
            actionExecutor.exec(siteSaveAction);
            siteInfo = actionExecutor.exec(new SiteGetInfoAction(siteId));
            Assert.assertEquals(expectedStudyIds,
                getStudyIds(siteInfo.studyCountInfo));
        }

        // create a second site, site 2, with the second set of studies
        Integer siteId2 = SiteHelper.createSite(actionExecutor, name + "_2",
            Utils.getRandomString(8, 12),
            ActivityStatusEnum.ACTIVE, studyIdsSet2);
        siteInfo = actionExecutor.exec(new SiteGetInfoAction(siteId2));
        expectedStudyIds.clear();
        expectedStudyIds.addAll(studyIdsSet2);
        Assert.assertEquals(expectedStudyIds,
            getStudyIds(siteInfo.studyCountInfo));

        // make sure site 1 still has same collection
        siteInfo = actionExecutor.exec(new SiteGetInfoAction(siteId));
        expectedStudyIds.clear();
        expectedStudyIds.addAll(studyIdsSet1);
        Assert.assertEquals(expectedStudyIds,
            getStudyIds(siteInfo.studyCountInfo));

        // delete studies one by one from Site 1
        siteInfo = actionExecutor.exec(new SiteGetInfoAction(siteId));
        for (Integer studyId : studyIdsSet1) {
            expectedStudyIds.remove(studyId);

            siteSaveAction = SiteHelper.getSaveAction(actionExecutor, siteInfo);
            studyIds = getStudyIds(siteInfo.studyCountInfo);
            studyIds.remove(studyId);
            siteSaveAction.setStudyIds(studyIds);
            actionExecutor.exec(siteSaveAction);
            siteInfo = actionExecutor.exec(new SiteGetInfoAction(siteId));
            Assert.assertEquals(expectedStudyIds,
                getStudyIds(siteInfo.studyCountInfo));
        }

        // delete studies from Site 2
        siteInfo = actionExecutor.exec(new SiteGetInfoAction(siteId2));
        studyIds = getStudyIds(siteInfo.studyCountInfo);
        studyIds.removeAll(studyIdsSet2);
        siteSaveAction = SiteHelper.getSaveAction(actionExecutor, siteInfo);
        actionExecutor.exec(siteSaveAction);
        siteInfo = actionExecutor.exec(new SiteGetInfoAction(siteId));
        Assert.assertTrue(getStudyIds(siteInfo.studyCountInfo).isEmpty());

        // attempt to add an invalid study ID
        siteInfo = actionExecutor.exec(new SiteGetInfoAction(siteId));
        SiteSaveAction siteSaveAction =
            SiteHelper.getSaveAction(actionExecutor, siteInfo);
        studyIds = getStudyIds(siteInfo.studyCountInfo);
        studyIds.add(-1);
        siteSaveAction.setStudyIds(studyIds);
        try {
            actionExecutor.exec(siteSaveAction);
            Assert.fail("should not be allowed to add an invalid study id");
        } catch (ModelNotFoundException e) {
            Assert.assertTrue(true);
        }
    }

    private Set<Integer> getStudyIds(List<StudyCountInfo> studyCountInfo) {
        Set<Integer> ids = new HashSet<Integer>();
        for (StudyCountInfo info : studyCountInfo) {
            ids.add(info.getStudy().getId());
        }
        return ids;
    }

    @Test
    public void delete() throws ApplicationException {
        Integer siteId = actionExecutor.exec(siteSaveAction).getId();
        actionExecutor.exec(new SiteDeleteAction(siteId));

        // hql query for site should return empty
        Query q =
            session.createQuery("SELECT COUNT(*) FROM "
                + Site.class.getName() + " WHERE id=?");
        q.setParameter(0, siteId);
        Long result = HibernateUtil.getCountFromQuery(q);
        Assert.assertTrue(result.equals(0L));
    }

    private Provisioning createSiteWithContainerType()
        throws ApplicationException {
        Provisioning provisioning =
            SiteHelper.provisionProcessingConfiguration(actionExecutor, name);

        ContainerTypeSaveAction ctSaveAction =
            ContainerTypeHelper.getSaveAction(name, name, provisioning.siteId,
                true, 3, 10,
                getContainerLabelingSchemes().get(0).getId());
        Integer containerTypeId = actionExecutor.exec(ctSaveAction).getId();
        provisioning.containerTypeIds.add(containerTypeId);
        return provisioning;
    }

    @Test
    public void deleteWithContainerTypes() throws ApplicationException {
        Provisioning provisioning = createSiteWithContainerType();
        try {
            actionExecutor.exec(new SiteDeleteAction(provisioning.siteId));
            Assert
                .fail(
                "should not be allowed to delete a site with container types");
        } catch (ActionCheckException e) {
            Assert.assertTrue(true);
        }

        // delete container type followed by site - should work now
        actionExecutor.exec(new ContainerTypeDeleteAction(
            provisioning.containerTypeIds.get(0)));
        actionExecutor.exec(new SiteDeleteAction(provisioning.siteId));
    }

    @Test
    public void deleteWithContainers() throws ApplicationException {
        Provisioning provisioning = createSiteWithContainerType();

        Integer containerTypeId = provisioning.containerTypeIds.get(0);

        ContainerSaveAction containerSaveAction = new ContainerSaveAction();
        containerSaveAction.setStatusId(ActivityStatusEnum.ACTIVE.getId());
        containerSaveAction.setBarcode(Utils.getRandomString(5, 10));
        containerSaveAction.setLabel("01");
        containerSaveAction.setSiteId(provisioning.siteId);
        containerSaveAction.setTypeId(containerTypeId);
        Integer containerId = actionExecutor.exec(containerSaveAction).getId();

        try {
            actionExecutor.exec(new SiteDeleteAction(provisioning.siteId));
            Assert
                .fail(
                "should not be allowed to delete a site with containers");
        } catch (ActionCheckException e) {
            Assert.assertTrue(true);
        }

        // delete container followed by site - should work now
        actionExecutor.exec(new ContainerDeleteAction(containerId));
        actionExecutor.exec(new ContainerTypeDeleteAction(containerTypeId));
        actionExecutor.exec(new SiteDeleteAction(provisioning.siteId));
    }

    @Test
    public void deleteWithProcessingEvents() throws Exception {
        Provisioning provisioning =
            SiteHelper.provisionProcessingConfiguration(actionExecutor, name);

        // create a collection event
        Integer ceventId = CollectionEventHelper
            .createCEventWithSourceSpecimens(actionExecutor,
                provisioning.patientIds.get(0), provisioning.clinicId);
        ArrayList<SpecimenInfo> sourceSpecs = actionExecutor
            .exec(new CollectionEventGetSpecimenInfosAction(ceventId,
                false)).getList();

        // create a processing event with one of the collection event source
        // specimens
        Integer pEventId = actionExecutor.exec(
            new ProcessingEventSaveAction(
                null, provisioning.siteId, Utils.getRandomDate(), Utils
                    .getRandomString(5, 8), 1, null, Arrays
                    .asList(sourceSpecs.get(0).specimen.getId()))).getId();

        try {
            actionExecutor.exec(new SiteDeleteAction(provisioning.siteId));
            Assert
                .fail(
                "should not be allowed to delete a site with processing events");
        } catch (ActionCheckException e) {
            Assert.assertTrue(true);
        }

        // delete the processing event
        actionExecutor.exec(new ProcessingEventDeleteAction(pEventId));
        actionExecutor.exec(new SiteDeleteAction(provisioning.siteId));
    }

    @Test
    public void deleteWithSrcDispatch() throws Exception {
        Provisioning provisioning =
            SiteHelper.provisionProcessingConfiguration(actionExecutor, name);

        Integer dispatchId1 =
            DispatchHelper.createDispatch(actionExecutor,
                provisioning.clinicId,
                provisioning.siteId,
                provisioning.patientIds.get(0));

        // create a second site to dispatch to
        Integer siteId2 = actionExecutor.exec(
            SiteHelper.getSaveAction(name + "_site2", name + "_site2",
                ActivityStatusEnum.ACTIVE)).getId();

        Integer dispatchId2 =
            DispatchHelper.createDispatch(actionExecutor, provisioning.siteId,
                siteId2, provisioning.patientIds.get(0));

        try {
            actionExecutor.exec(new SiteDeleteAction(provisioning.siteId));
            Assert
                .fail(
                "should not be allowed to delete a site which is a source of dispatches");
        } catch (ActionCheckException e) {
            Assert.assertTrue(true);
        }

        // delete the dispatch and then the site
        Set<Specimen> specimens = new HashSet<Specimen>();
        ListResult<DispatchSpecimen> dispatchSpecimens =
            actionExecutor
                .exec(new DispatchGetSpecimenInfosAction(dispatchId1));
        for (DispatchSpecimen dspec : dispatchSpecimens.getList()) {
            specimens.add(dspec.getSpecimen());
        }

        dispatchSpecimens =
            actionExecutor
                .exec(new DispatchGetSpecimenInfosAction(dispatchId2));
        for (DispatchSpecimen dspec : dispatchSpecimens.getList()) {
            specimens.add(dspec.getSpecimen());
        }

        actionExecutor.exec(new DispatchDeleteAction(dispatchId2));
        actionExecutor.exec(new DispatchDeleteAction(dispatchId1));

        for (Specimen specimen : specimens) {
            SpecimenHelper.deleteSpecimen(actionExecutor, specimen);
        }

        deleteOriginInfos(provisioning.siteId);
        actionExecutor.exec(new SiteDeleteAction(provisioning.siteId));
    }

    @Test
    public void deleteWithDstDispatch() throws Exception {
        Provisioning provisioning =
            SiteHelper.provisionProcessingConfiguration(actionExecutor, name);

        Integer dispatchId =
            DispatchHelper.createDispatch(actionExecutor,
                provisioning.clinicId,
                provisioning.siteId,
                provisioning.patientIds.get(0));

        try {
            actionExecutor.exec(new SiteDeleteAction(provisioning.siteId));
            Assert
                .fail(
                "should not be allowed to delete a site which is a destination for dispatches");
        } catch (ActionCheckException e) {
            Assert.assertTrue(true);
        }

        // delete the dispatch and then the site - no need to delete dispatch
        // specimens
        actionExecutor.exec(new DispatchDeleteAction(dispatchId));
        actionExecutor.exec(new SiteDeleteAction(provisioning.siteId));
    }

}
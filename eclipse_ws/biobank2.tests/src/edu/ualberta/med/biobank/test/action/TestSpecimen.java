package edu.ualberta.med.biobank.test.action;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.UUID;

import junit.framework.Assert;

import org.hibernate.criterion.Restrictions;
import org.junit.Test;

import edu.ualberta.med.biobank.common.action.exception.ActionException;
import edu.ualberta.med.biobank.common.action.search.SpecimenByInventorySearchAction;
import edu.ualberta.med.biobank.common.action.search.SpecimenByMicroplateSearchAction;
import edu.ualberta.med.biobank.common.action.specimen.SpecimenActionHelper;
import edu.ualberta.med.biobank.common.action.specimen.SpecimenBriefInfo;
import edu.ualberta.med.biobank.common.action.specimen.SpecimenGetInfoAction;
import edu.ualberta.med.biobank.common.action.specimen.SpecimenGetPossibleTypesAction;
import edu.ualberta.med.biobank.common.action.specimen.SpecimenGetPossibleTypesAction.SpecimenTypeData;
import edu.ualberta.med.biobank.common.action.specimen.SpecimenMicroplateConsistentAction;
import edu.ualberta.med.biobank.common.action.specimen.SpecimenMicroplateConsistentAction.SpecimenMicroplateInfo;
import edu.ualberta.med.biobank.common.action.specimen.SpecimenSetGetInfoAction;
import edu.ualberta.med.biobank.common.util.InventoryIdUtil;
import edu.ualberta.med.biobank.model.Capacity;
import edu.ualberta.med.biobank.model.Container;
import edu.ualberta.med.biobank.model.ContainerLabelingScheme;
import edu.ualberta.med.biobank.model.ContainerType;
import edu.ualberta.med.biobank.model.Site;
import edu.ualberta.med.biobank.model.Specimen;
import edu.ualberta.med.biobank.model.SpecimenPosition;
import edu.ualberta.med.biobank.model.SpecimenType;
import edu.ualberta.med.biobank.model.util.RowColPos;

public class TestSpecimen extends TestAction {

    private Container createContainer() {
        ContainerLabelingScheme labeling = (ContainerLabelingScheme)
            session.createCriteria(ContainerLabelingScheme.class)
                .add(Restrictions.eq("name", "SBS Standard")).uniqueResult();

        Container topContainer = factory.createTopContainer();

        Capacity capacity = new Capacity();
        capacity.setRowCapacity(3);
        capacity.setColCapacity(1);
        factory.setDefaultCapacity(capacity);

        ContainerType ctype = factory.createContainerType();
        topContainer.getContainerType().setChildLabelingScheme(labeling);
        topContainer.getContainerType().getChildContainerTypes().add(ctype);
        return factory.createContainer();
    }

    @Test
    public void checkGetAction() throws Exception {
        session.beginTransaction();
        Specimen specimen = factory.createParentSpecimen();
        session.getTransaction().commit();

        SpecimenBriefInfo specimenBriefInfo = exec(
            new SpecimenGetInfoAction(specimen.getId()));

        Assert.assertEquals(specimen, specimenBriefInfo.getSpecimen());
    }

    @Test
    public void checkGetActionWithPosition() throws Exception {
        session.beginTransaction();
        Container specimenContainer = createContainer();
        ContainerType ctype = specimenContainer.getContainerType();
        factory.createParentSpecimen();
        factory.createProcessingEvent();
        session.flush();

        SpecimenType childSpecimenType = null;
        Map<Specimen, String> specimenPosString = new HashMap<Specimen, String>();
        Set<Specimen> childSpecimens = new HashSet<Specimen>();
        for (int i = 0; i < 3; ++i) {
            Specimen childSpecimen = factory.createChildSpecimen();
            childSpecimens.add(childSpecimen);

            if (i == 0) {
                childSpecimenType = childSpecimen.getSpecimenType();
                ctype.getSpecimenTypes().add(childSpecimenType);
                session.update(ctype);
                session.flush();
            }

            SpecimenPosition specimenPosition = new SpecimenPosition();
            specimenPosition.setRow(i);
            specimenPosition.setCol(0);

            RowColPos pos = new RowColPos(i, 0);
            String positionString = ContainerLabelingScheme.getPositionString(
                pos, ctype.getChildLabelingScheme().getId(), ctype.getCapacity().getRowCapacity(),
                ctype.getCapacity().getColCapacity(), ctype.getLabelingLayout());
            specimenPosition.setPositionString(positionString);
            specimenPosString.put(childSpecimen, positionString);

            specimenPosition.setSpecimen(childSpecimen);
            childSpecimen.setSpecimenPosition(specimenPosition);

            specimenPosition.setContainer(specimenContainer);
            specimenContainer.getSpecimenPositions().add(specimenPosition);

            session.update(childSpecimen);
        }
        session.getTransaction().commit();

        for (Specimen childSpecimen : childSpecimens) {
            SpecimenBriefInfo specimenBriefInfo = exec(
                new SpecimenGetInfoAction(childSpecimen.getId()));
            Assert.assertEquals(childSpecimen, specimenBriefInfo.getSpecimen());

            Stack<Container> containerStack = specimenBriefInfo.getParents();
            Assert.assertEquals(2, containerStack.size());
            Assert.assertEquals(specimenContainer, containerStack.get(0));
            Assert.assertEquals(specimenContainer.getParentContainer(),
                containerStack.get(1));

            Assert.assertEquals(
                specimenContainer.getLabel() + specimenPosString.get(childSpecimen),
                SpecimenActionHelper.getPositionString(
                    specimenBriefInfo.getSpecimen(), true, false));
        }

    }

    // search for an invalid specimenId
    @Test
    public void searchByInventoryIdActionBadId() {
        session.beginTransaction();
        Site site = factory.createSite();
        session.getTransaction().commit();

        String badInventoryId = new UUID(128, 256).toString();

        final List<Integer> actionResult = exec(
            new SpecimenByInventorySearchAction(badInventoryId, site.getId())).getList();
        Assert.assertEquals(0, actionResult.size());
    }

    @Test
    public void searchByInventoryIdAction() {
        session.beginTransaction();
        factory.createClinic();
        Site site = factory.createSite();
        factory.createStudy();
        Specimen spc = factory.createParentSpecimen();
        session.getTransaction().commit();

        final List<Integer> actionResult = exec(new SpecimenByInventorySearchAction(
            spc.getInventoryId(), site.getId())).getList();
        Assert.assertEquals(1, actionResult.size());
        Assert.assertEquals(spc.getId(), actionResult.get(0));
    }

    @Test
    public void searchByMicroplateIdAction() {
        session.beginTransaction();
        factory.createClinic();
        factory.createStudy();
        factory.createContainer();
        Specimen spc = factory.createMicroplateSpecimen("A1");
        session.getTransaction().commit();

        final List<String> actionResult = exec(new SpecimenByMicroplateSearchAction(
            InventoryIdUtil.microplatePart(spc.getInventoryId()))).getList();
        Assert.assertEquals(1, actionResult.size());
        Assert.assertEquals(spc.getInventoryId(), actionResult.get(0));
    }

    @Test
    public void checkMicroplateConsistencyAction() {
        session.beginTransaction();
        factory.createClinic();
        factory.createStudy();
        factory.createContainer();
        Specimen spc1 = factory.createMicroplateSpecimen("A1");
        Specimen spc2 = factory.createMicroplateSpecimen("A2");
        Site site = factory.createSite();
        session.getTransaction().commit();

        List<SpecimenMicroplateInfo> smInfos = new ArrayList<SpecimenMicroplateInfo>();
        SpecimenMicroplateInfo smi1 = new SpecimenMicroplateInfo();
        smi1.inventoryId = spc1.getInventoryId();
        smi1.containerId = null;
        smi1.position = null;
        smInfos.add(smi1);
        try {
            exec(new SpecimenMicroplateConsistentAction(
                site.getId(), false, smInfos));
            Assert.fail();
        } catch (ActionException ae) {
        }
        SpecimenMicroplateInfo smi2 = new SpecimenMicroplateInfo();
        smi2.inventoryId = spc2.getInventoryId();
        smi2.containerId = null;
        smi2.position = null;
        smInfos.add(smi2);
        try {
            exec(new SpecimenMicroplateConsistentAction(
                site.getId(), false, smInfos));
        } catch (ActionException ae) {
            Assert.fail();
        }
    }

    @Test
    public void getPossibleTypesParentSpecimen() {
        Set<SpecimenType> specimenTypes = new HashSet<SpecimenType>();
        session.beginTransaction();
        factory.createStudy();
        specimenTypes.add(factory.createSpecimenType());
        factory.createSourceSpecimen();
        specimenTypes.add(factory.createSpecimenType());
        factory.createSourceSpecimen();
        factory.createCollectionEvent();
        Specimen specimen = factory.createParentSpecimen();
        session.getTransaction().commit();

        SpecimenTypeData SpecimenTypeData = exec(new SpecimenGetPossibleTypesAction(specimen));
        Assert.assertEquals(specimenTypes.size(), SpecimenTypeData.getSpecimenTypes().size());
        Assert.assertEquals(0, SpecimenTypeData.getVolumeMap().size());
    }

    @Test
    public void getPossibleTypesChildSpecimen() {
        Set<SpecimenType> specimenTypes = new HashSet<SpecimenType>();
        session.beginTransaction();
        factory.createStudy();

        factory.createSpecimenType();
        factory.createSourceSpecimen();
        factory.createSpecimenType();
        factory.createSourceSpecimen();

        specimenTypes.add(factory.createSpecimenType());
        factory.createAliquotedSpecimen();
        specimenTypes.add(factory.createSpecimenType());
        factory.createAliquotedSpecimen();

        factory.createCollectionEvent();
        factory.createParentSpecimen();
        factory.createProcessingEvent();
        Specimen specimen = factory.createChildSpecimen();
        session.getTransaction().commit();

        SpecimenTypeData SpecimenTypeData = exec(new SpecimenGetPossibleTypesAction(specimen));
        Assert.assertEquals(specimenTypes.size(), SpecimenTypeData.getSpecimenTypes().size());
        Assert.assertEquals(specimenTypes.size(), SpecimenTypeData.getVolumeMap().size());
    }

    @Test
    public void specimenSetGetInfoAction() {
        Set<String> inventoryIds = new HashSet<String>();
        session.beginTransaction();

        Specimen specimen = factory.createParentSpecimen();
        inventoryIds.add(specimen.getInventoryId());
        specimen = factory.createChildSpecimen();
        inventoryIds.add(specimen.getInventoryId());
        session.getTransaction().commit();

        ArrayList<SpecimenBriefInfo> result = exec(new SpecimenSetGetInfoAction(
            factory.getDefaultSite(),
            inventoryIds)).getList();
        Assert.assertEquals(inventoryIds.size(), result.size());
        for (SpecimenBriefInfo info : result) {
            Assert.assertTrue(inventoryIds.contains(info.getSpecimen().getInventoryId()));
        }
    }

    @Test
    public void specimenSetGetInfoActionDoesNotExist() {
        Set<String> inventoryIds = new HashSet<String>();
        inventoryIds.add(getMethodNameR());

        session.beginTransaction();
        Site site = factory.createSite();
        session.getTransaction().commit();

        ArrayList<SpecimenBriefInfo> result = exec(new SpecimenSetGetInfoAction(
            site,
            inventoryIds)).getList();
        Assert.assertEquals(0, result.size());
    }
}

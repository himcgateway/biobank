package test.ualberta.med.biobank;

import java.util.Collection;
import java.util.Iterator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import edu.ualberta.med.biobank.common.BiobankCheckException;
import edu.ualberta.med.biobank.common.wrappers.internal.ContainerLabelingSchemeWrapper;
import edu.ualberta.med.biobank.common.wrappers.ContainerTypeWrapper;
import edu.ualberta.med.biobank.common.wrappers.SiteWrapper;
import edu.ualberta.med.biobank.model.ContainerLabelingScheme;

public class TestContainerLabelingScheme extends
    TestDatabase {

   private ContainerLabelingSchemeWrapper clsw;
    
    @Before
    public void setUp() throws Exception {
        super.setUp();
    	clsw = newContainerLabelingScheme();
    }
    
    
    @Test(expected=BiobankCheckException.class)
    public void TestDeleteChecks() throws BiobankCheckException, Exception {
        SiteWrapper site = addSite("sitename");
    	ContainerTypeWrapper ctw = addContainerType(site, "dummyCT", "dct", clsw.getId(), 2, 2, false);
    	//should not be able to delete a cls that is in use
    	clsw.delete();
    }

    @Test
    public void TestGetAllLabelingSchemes() throws BiobankCheckException, Exception {
    	//check after add
        Collection<ContainerLabelingSchemeWrapper> beforeWraps = ContainerLabelingSchemeWrapper.getAllLabelingSchemes(appService);
        clsw.persist();
        Collection<ContainerLabelingSchemeWrapper> afterWraps = ContainerLabelingSchemeWrapper.getAllLabelingSchemes(appService);
        
        Iterator<ContainerLabelingSchemeWrapper> b = beforeWraps.iterator();
        Iterator<ContainerLabelingSchemeWrapper> a = afterWraps.iterator();
        while (b.hasNext()&&a.hasNext()) {
            Assert.assertTrue(b.next().equals(a.next()));
        }
        Assert.assertTrue(!b.hasNext()&&a.hasNext());
        Assert.assertTrue(clsw.equals(a.next()));
        
        //check after delete
        clsw.delete();
        afterWraps = ContainerLabelingSchemeWrapper.getAllLabelingSchemes(appService);
        b = beforeWraps.iterator();
        a = afterWraps.iterator();
        while (b.hasNext()&&a.hasNext()) {
            Assert.assertTrue(b.next().equals(a.next()));
        }
        Assert.assertTrue(!b.hasNext()&&!a.hasNext());   
    }
    
    @Test
    public void TestBasicGettersAndSetters() throws BiobankCheckException, Exception {
        testGettersAndSetters(clsw);
    }

    public static ContainerLabelingSchemeWrapper newContainerLabelingScheme() {
    	ContainerLabelingSchemeWrapper clsw=new ContainerLabelingSchemeWrapper(appService, new ContainerLabelingScheme());
    	clsw.setName("SchemeName");
    	return clsw;
    }
    
}

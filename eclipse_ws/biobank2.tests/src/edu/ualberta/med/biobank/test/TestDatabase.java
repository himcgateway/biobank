package edu.ualberta.med.biobank.test;

import edu.ualberta.med.biobank.common.BiobankCheckException;
import edu.ualberta.med.biobank.common.wrappers.ModelWrapper;
import edu.ualberta.med.biobank.test.internal.SourceVesselHelper;
import edu.ualberta.med.biobank.test.internal.SampleTypeHelper;
import edu.ualberta.med.biobank.test.internal.ShippingCompanyHelper;
import edu.ualberta.med.biobank.test.internal.SiteHelper;
import gov.nih.nci.system.applicationservice.WritableApplicationService;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;


public class TestDatabase {
    protected static WritableApplicationService appService;

    protected Random r;

    private static final List<Class<?>> IGNORE_RETURN_TYPES = new ArrayList<Class<?>>() {
        private static final long serialVersionUID = 1L;
        {
            add(java.lang.Class.class);
            add(java.lang.Object.class);
        }
    };

    private class GetterInfo {
        Method getMethod;
        Method setMethod;
    }

    @Before
    public void setUp() throws Exception {
        r = new Random();
        appService = AllTests.appService;
        if (appService == null) {
            AllTests.setUp();
            appService = AllTests.appService;
        }
    }

    @After
    public void tearDown() throws Exception {
        try {
            SampleTypeHelper.deleteCreatedSampleTypes();
            SiteHelper.deleteCreatedSites();
            SourceVesselHelper.deleteCreatedSourceVessels();
            ShippingCompanyHelper.deleteCreateShippingCompanies();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            Assert.fail();
        }
    }

    public Collection<GetterInfo> getGettersAndSetters(ModelWrapper<?> w) {
        HashMap<String, GetterInfo> map = new HashMap<String, GetterInfo>();
        Method[] methods = w.getClass().getMethods();

        for (Method method : methods) {
            if (method.getName().startsWith("get")
                && !method.getName().equals("getClass")
                && !IGNORE_RETURN_TYPES.contains(method.getReturnType())
                && !Collection.class.isAssignableFrom(method.getReturnType())
                && !Map.class.isAssignableFrom(method.getReturnType())
                && !method.getReturnType().isArray()
                && !method.getReturnType().getName().startsWith(
                    "edu.ualberta.med.biobank.common")) {
                GetterInfo getterInfo = new GetterInfo();
                getterInfo.getMethod = method;
                map.put(method.getName(), getterInfo);
            }
        }

        for (Method method : methods) {
            if (method.getName().startsWith("set")
                && !method.getName().equals("setClass")) {
                String setterName = method.getName();
                String getterName = "g"
                    + setterName.substring(1, setterName.length());
                GetterInfo getterInfo = map.get(getterName);
                if (getterInfo == null) {
                    // System.out.println("no getter found for "
                    // + w.getClass().getName() + "." + setterName + "()");
                    continue;
                }
                getterInfo.setMethod = method;
            }
        }
        return map.values();
    }

    public void testGettersAndSetters(ModelWrapper<?> w)
        throws BiobankCheckException, Exception {
        testGettersAndSetters(w, null);
    }

    public void testGettersAndSetters(ModelWrapper<?> w,
        List<String> skipMethods) throws BiobankCheckException, Exception {
        Collection<GetterInfo> gettersInfoList = getGettersAndSetters(w);

        if (skipMethods != null) {
            List<String> methodNames = new ArrayList<String>();
            for (GetterInfo getterInfo : gettersInfoList) {
                methodNames.add(getterInfo.getMethod.getName());
            }

            for (String methodName : skipMethods) {
                if (!methodNames.contains(methodName)) {
                    throw new Exception("method to skip does not exist: "
                        + methodName);
                }
            }
        }

        for (GetterInfo getterInfo : gettersInfoList) {
            if ((skipMethods != null)
                && skipMethods.contains(getterInfo.getMethod.getName())) {
                continue;
            }

            if (getterInfo.setMethod == null) {
                // System.out.println("no setter found for "
                // + w.getClass().getName() + "."
                // + getterInfo.getMethod.getName() + "()");
                continue;
            }

            Class<?> returnType = getterInfo.getMethod.getReturnType();

            for (int i = 0; i < 5; ++i) {
                Object parameter = null;

                if (returnType.equals(java.lang.Boolean.class)) {
                    parameter = new Boolean(r.nextBoolean());
                } else if (returnType.equals(java.lang.Integer.class)) {
                    parameter = new Integer(r.nextInt(Integer.MAX_VALUE));
                } else if (returnType.equals(java.lang.Double.class)) {
                    parameter = new Double(r.nextDouble());
                } else if (returnType.equals(java.lang.String.class)) {
                    parameter = Utils.getRandomString(32);
                } else if (returnType.equals(java.util.Date.class)) {
                    parameter = Utils.getRandomDate();
                } else {
                    throw new Exception("return type "
                        + getterInfo.getMethod.getReturnType().getName()
                        + " for method " + getterInfo.getMethod.getName()
                        + " for class " + w.getClass().getName()
                        + " not implemented");
                }

                // System.out.println("invoking " + w.getClass().getName() + "."
                // + getterInfo.getMethod.getName());

                getterInfo.setMethod.invoke(w, parameter);
                w.persist();
                w.reload();
                Object getResult = getterInfo.getMethod.invoke(w);

                Assert.assertEquals(w.getClass().getName() + "."
                    + getterInfo.getMethod.getName() + "()", parameter,
                    getResult);
            }
        }

    }
}

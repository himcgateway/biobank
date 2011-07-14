package edu.ualberta.med.biobank.treeview.report;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import edu.ualberta.med.biobank.SessionManager;
import edu.ualberta.med.biobank.common.wrappers.ReportWrapper;
import edu.ualberta.med.biobank.model.Report;
import edu.ualberta.med.biobank.server.query.BiobankSQLCriteria;
import edu.ualberta.med.biobank.treeview.AdapterBase;
import gov.nih.nci.system.applicationservice.ApplicationException;
import gov.nih.nci.system.query.hibernate.HQLCriteria;

public class SharedReportsGroup extends AbstractReportGroup {
    private static final String NODE_NAME = Messages.SharedReportsGroup_shared_node_label;
    private static final String USER_ID_TOKEN = "{userId}"; //$NON-NLS-1$
    private static final String USER_ID_LIST_TOKEN = "{userIds}"; //$NON-NLS-1$
    private static final String HQL_REPORT_OF_USER = "from " //$NON-NLS-1$
        + Report.class.getName() + " where isPublic <> 0 and userId in (" //$NON-NLS-1$
        + USER_ID_LIST_TOKEN + ")"; //$NON-NLS-1$
    private static final String SQL_USERS_IN_SAME_GROUP = "SELECT CONVERT(u2.user_id, CHAR) FROM csm_user u1, csm_user u2 WHERE u1.user_id = " //$NON-NLS-1$
        + USER_ID_TOKEN
        + " AND NOT EXISTS (SELECT g1.group_id FROM csm_user_group g1 WHERE g1.user_id = u1.user_id AND g1.group_id NOT IN (SELECT g2.group_id FROM csm_user_group g2 WHERE g2.user_id = u2.user_id)) AND NOT EXISTS (SELECT g1.group_id FROM csm_user_group g1 WHERE g1.user_id = u2.user_id AND g1.group_id NOT IN (SELECT g2.group_id FROM csm_user_group g2 WHERE g2.user_id = u1.user_id))"; //$NON-NLS-1$

    public SharedReportsGroup(AdapterBase parent, int id) {
        super(parent, id, NODE_NAME);
    }

    @Override
    protected Collection<ReportWrapper> getReports() {
        List<ReportWrapper> reports = new ArrayList<ReportWrapper>();

        if (SessionManager.getInstance().isConnected()) {
            String userId = SessionManager.getUser().getId().toString();

            String sqlString = SQL_USERS_IN_SAME_GROUP.replace(USER_ID_TOKEN,
                userId);
            BiobankSQLCriteria sqlCriteria = new BiobankSQLCriteria(sqlString);
            List<Object> userIds = Arrays.asList();

            try {
                userIds = SessionManager.getAppService().query(sqlCriteria,
                    Report.class.getName());
            } catch (ApplicationException e1) {
                e1.printStackTrace();
            }

            if (!userIds.isEmpty()) {
                String userIdList = StringUtils.join(userIds.toArray(), ","); //$NON-NLS-1$

                String hqlString = HQL_REPORT_OF_USER.replace(
                    USER_ID_LIST_TOKEN, userIdList);

                HQLCriteria hqlCriteria = new HQLCriteria(hqlString,
                    Arrays.asList(new Object[] {}));
                try {
                    List<Report> rawReports = SessionManager.getAppService()
                        .query(hqlCriteria);
                    for (Report rawReport : rawReports) {
                        reports.add(new ReportWrapper(SessionManager
                            .getAppService(), rawReport));
                    }
                } catch (ApplicationException e) {
                    e.printStackTrace();
                }
            }
        }

        return reports;
    }
}

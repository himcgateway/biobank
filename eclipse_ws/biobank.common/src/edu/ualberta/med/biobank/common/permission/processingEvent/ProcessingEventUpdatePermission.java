package edu.ualberta.med.biobank.common.permission.processingEvent;

import edu.ualberta.med.biobank.common.action.ActionContext;
import edu.ualberta.med.biobank.common.permission.Permission;
import edu.ualberta.med.biobank.common.permission.PermissionEnum;

public class ProcessingEventUpdatePermission implements Permission {
    private static final long serialVersionUID = 1L;
    private Integer peventId;

    public ProcessingEventUpdatePermission(Integer peventId) {
        this.peventId = peventId;
    }

    @Override
    public boolean isAllowed(ActionContext context) {
        // FIXME specific study or center ?
        return PermissionEnum.PROCESSING_EVENT_UPDATE
            .isAllowed(context.getUser());
    }
}
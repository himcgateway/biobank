package edu.ualberta.med.biobank.common.action.containerType;

import edu.ualberta.med.biobank.common.action.AbstractMutator;
import edu.ualberta.med.biobank.common.action.ActionContext;
import edu.ualberta.med.biobank.common.action.exception.NullPropertyException;
import edu.ualberta.med.biobank.common.peer.ContainerTypePeer;
import edu.ualberta.med.biobank.model.ContainerType;

public class ContainerTypeMutator extends AbstractMutator<ContainerType> {
    public ContainerTypeMutator(ActionContext context, ContainerType model) {
        super(context, ContainerType.class, model);
    }

    public void setName(String name) throws NullPropertyException {
        notNull(ContainerTypePeer.NAME, name);
        unique(propertyValue(ContainerTypePeer.NAME, name));
        model.setName(name);
    }

    public void setNameShort(String nameShort) throws NullPropertyException {
        notNull(ContainerTypePeer.NAME_SHORT, nameShort);
        unique(propertyValue(ContainerTypePeer.NAME_SHORT, nameShort));
        model.setNameShort(nameShort);
    }

    public void setTopLevel(boolean topLevel) {
        // if not new and used by containers, check not changed
        model.setTopLevel(topLevel);
        // context.getSession().getSessionFactory().op
    }

}
package edu.ualberta.med.biobank.action.dispatch;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import edu.ualberta.med.biobank.action.Action;
import edu.ualberta.med.biobank.action.ActionContext;
import edu.ualberta.med.biobank.action.IdResult;
import edu.ualberta.med.biobank.action.exception.ActionException;
import edu.ualberta.med.biobank.action.info.DispatchSaveInfo;
import edu.ualberta.med.biobank.action.info.DispatchSpecimenInfo;
import edu.ualberta.med.biobank.action.info.ShipmentInfoSaveInfo;
import edu.ualberta.med.biobank.permission.dispatch.DispatchUpdatePermission;
import edu.ualberta.med.biobank.common.wrappers.ShipmentInfoWrapper;
import edu.ualberta.med.biobank.model.Center;
import edu.ualberta.med.biobank.model.Comment;
import edu.ualberta.med.biobank.model.Dispatch;
import edu.ualberta.med.biobank.model.DispatchSpecimen;
import edu.ualberta.med.biobank.model.ShipmentInfo;
import edu.ualberta.med.biobank.model.ShippingMethod;
import edu.ualberta.med.biobank.model.Specimen;
import edu.ualberta.med.biobank.model.type.DispatchState;

public class DispatchSaveAction implements Action<IdResult> {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private DispatchSaveInfo dInfo;
    private Set<DispatchSpecimenInfo> dsInfos;
    private ShipmentInfoSaveInfo siInfo;

    public DispatchSaveAction(DispatchSaveInfo dInfo,
        Set<DispatchSpecimenInfo> dsInfos, ShipmentInfoSaveInfo siInfo) {
        this.dInfo = dInfo;
        this.dsInfos = dsInfos;
        this.siInfo = siInfo;
    }

    @Override
    public boolean isAllowed(ActionContext context) throws ActionException {
        return new DispatchUpdatePermission(dInfo.id).isAllowed(context);
    }

    @Override
    public IdResult run(ActionContext context) throws ActionException {
        Dispatch disp =
            context.get(Dispatch.class, dInfo.id, new Dispatch());

        disp.setReceiverCenter(context.get(Center.class, dInfo.receiverId));
        disp.setSenderCenter(context.get(Center.class, dInfo.senderId));

        if (dInfo.state == null)
            dInfo.state = DispatchState.CREATION;

        disp.setState(dInfo.state);

        disp.getDispatchSpecimens().clear();
        disp.getDispatchSpecimens().addAll(reassemble(context, disp,
            dsInfos));

        if (siInfo != null) {
            ShipmentInfo si =
                context
                    .get(ShipmentInfo.class, siInfo.siId, new ShipmentInfo());
            si.setBoxNumber(siInfo.boxNumber);
            si.setPackedAt(siInfo.packedAt);
            si.setReceivedAt(siInfo.receivedAt);
            si.setWaybill(siInfo.waybill);

            ShippingMethod sm = context.load(ShippingMethod.class,
                siInfo.shippingMethodId);

            si.setShippingMethod(sm);
            disp.setShipmentInfo(si);
        }

        // This stuff could be extracted to a util method. need to think about
        // how
        if (!dInfo.comment.trim().isEmpty()) {
            Set<Comment> comments = disp.getComments();
            if (comments == null) comments = new HashSet<Comment>();
            Comment newComment = new Comment();
            newComment.setTimeCreated(new Date());
            newComment.setMessage(dInfo.comment);
            newComment.setUser(context.getUser());
            context.getSession().saveOrUpdate(newComment);

            comments.add(newComment);
            disp.setComments(comments);
        }

        context.getSession().saveOrUpdate(disp);
        context.getSession().flush();

        return new IdResult(disp.getId());
    }

    private Set<DispatchSpecimen> reassemble(ActionContext context,
        Dispatch dispatch,
        Set<DispatchSpecimenInfo> dsInfos) {
        Set<DispatchSpecimen> dispSpecimens =
            new HashSet<DispatchSpecimen>();
        for (DispatchSpecimenInfo dsInfo : dsInfos) {
            DispatchSpecimen dspec =
                context.get(DispatchSpecimen.class, dsInfo.id,
                    new DispatchSpecimen());
            Specimen spec = context.load(Specimen.class,
                dsInfo.specimenId);
            spec.setCurrentCenter(context.get(Center.class, dInfo.receiverId));
            context.getSession().saveOrUpdate(spec);
            dspec.setSpecimen(spec);
            dspec.setState(dsInfo.state);
            dspec.setDispatch(dispatch);
            dispSpecimens.add(dspec);
        }
        return dispSpecimens;

    }

    public static ShipmentInfoSaveInfo prepareShipInfo(
        ShipmentInfoWrapper shipmentInfo) {
        if (shipmentInfo == null) return null;
        ShipmentInfoSaveInfo si =
            new ShipmentInfoSaveInfo(shipmentInfo.getId(),
                shipmentInfo.getBoxNumber(), shipmentInfo.getPackedAt(),
                shipmentInfo.getReceivedAt(), shipmentInfo.getWaybill(),
                shipmentInfo.getShippingMethod().getId());
        return si;
    }
}

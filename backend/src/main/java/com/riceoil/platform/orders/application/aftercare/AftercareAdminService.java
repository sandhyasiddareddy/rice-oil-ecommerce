package com.riceoil.platform.orders.application.aftercare;

import com.riceoil.platform.orders.application.OrderAftercareMailer;
import com.riceoil.platform.shared.observability.BusinessLog;
import com.riceoil.platform.shared.web.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AftercareAdminService {

    private final AftercareRequestStateService stateService;
    private final RefundProvider refundProvider;
    private final OrderAftercareMailer mailer;

    public AftercareAdminService(AftercareRequestStateService stateService, RefundProvider refundProvider,
                                 OrderAftercareMailer mailer) {
        this.stateService = stateService;
        this.refundProvider = refundProvider;
        this.mailer = mailer;
    }

    public List<AdminAftercareRequest> pendingRefundRequests() {
        return stateService.pendingRefundRequests();
    }

    public List<AdminAftercareRequest> pendingCancellationRequests() {
        return stateService.pendingCancellationRequests();
    }

    public void approveRefund(Long requestId) {
        RefundExecution execution = stateService.beginRefundApproval(requestId);
        BusinessLog.info("REFUND_APPROVED", "refundRequestId", execution.refundRequestId(),
                "orderId", execution.orderId(), "razorpayPaymentId", execution.paymentId(),
                "amount", execution.amount());
        RefundProvider.ProviderRefundResult result = callProvider(execution, "refund-" + execution.refundRequestId());
        stateService.completeRefund(execution.refundRequestId(), result.refundId(), result.status());
        BusinessLog.info("REFUND_SENT_TO_RAZORPAY", "refundRequestId", execution.refundRequestId(),
                "orderId", execution.orderId(), "razorpayPaymentId", execution.paymentId(),
                "razorpayRefundId", result.refundId(), "providerStatus", result.status());
        if ("failed".equalsIgnoreCase(result.status())) {
            throw new IllegalStateException("Razorpay reported that the refund failed");
        }
        mailer.refundInitiated(execution.orderId(), execution.amount(), "INR");
    }

    public void rejectRefund(Long requestId) {
        stateService.rejectRefund(requestId);
        BusinessLog.info("REFUND_REJECTED", "refundRequestId", requestId);
        stateService.orderIdOfRefund(requestId).ifPresent(mailer::refundRejected);
    }

    public void approveCancellation(Long requestId) {
        CancellationExecution execution = stateService.beginCancellationApproval(requestId);
        if (execution.alreadyCompleted()) {
            BusinessLog.info("CANCELLATION_ALREADY_COMPLETED", "cancellationRequestId", requestId);
            mailer.orderCancelled(execution.orderId(), false);
            return;
        }
        BusinessLog.info("CANCELLATION_APPROVED", "cancellationRequestId", requestId,
                "refundRequired", execution.refund() != null);
        if (execution.refund() != null) {
            RefundExecution refund = execution.refund();
            RefundProvider.ProviderRefundResult result = callProvider(refund, "cancel-refund-" + requestId);
            stateService.completeRefund(refund.refundRequestId(), result.refundId(), result.status());
            BusinessLog.info("REFUND_SENT_TO_RAZORPAY", "refundRequestId", refund.refundRequestId(),
                    "cancellationRequestId", requestId, "orderId", refund.orderId(),
                    "razorpayPaymentId", refund.paymentId(), "razorpayRefundId", result.refundId(),
                    "providerStatus", result.status(), "amount", refund.amount());
            if ("failed".equalsIgnoreCase(result.status())) {
                throw new IllegalStateException("Razorpay reported that the cancellation refund failed");
            }
        }
        stateService.completeCancellation(requestId);
        BusinessLog.info("ORDER_CANCELLED", "cancellationRequestId", requestId);
        mailer.orderCancelled(execution.orderId(), execution.refund() != null);
    }

    public void rejectCancellation(Long requestId) {
        stateService.rejectCancellation(requestId);
        BusinessLog.info("CANCELLATION_REJECTED", "cancellationRequestId", requestId);
        stateService.orderIdOfCancellation(requestId).ifPresent(mailer::cancellationRejected);
    }

    private RefundProvider.ProviderRefundResult callProvider(RefundExecution execution, String reference) {
        try {
            return refundProvider.refund(execution.paymentId(), execution.amountInSubunits(), reference);
        } catch (RuntimeException ex) {
            BusinessLog.error("REFUND_PROVIDER_CALL_FAILED", ex, "refundRequestId", execution.refundRequestId(),
                    "orderId", execution.orderId(), "razorpayPaymentId", execution.paymentId(),
                    "amount", execution.amount(), "reference", reference, "reason", ex.getMessage());
            throw ex;
        }
    }
}

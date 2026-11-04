package com.riceoil.platform.orders.application.aftercare;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AftercareAdminServiceTest {

    @Test
    void approveRefund_shouldSendApprovedAmountToProviderAndPersistProviderReference() {
        AftercareRequestStateService stateService = mock(AftercareRequestStateService.class);
        RefundProvider refundProvider = mock(RefundProvider.class);
        RefundExecution execution = new RefundExecution(7L, 15L, "pay_123", new BigDecimal("129.00"));
        when(stateService.beginRefundApproval(7L)).thenReturn(execution);
        when(refundProvider.refund("pay_123", 12900, "refund-7"))
                .thenReturn(new RefundProvider.ProviderRefundResult("rfnd_123", "pending"));
        AftercareAdminService service = new AftercareAdminService(stateService, refundProvider,
                mock(com.riceoil.platform.orders.application.OrderAftercareMailer.class));

        service.approveRefund(7L);

        verify(stateService).completeRefund(7L, "rfnd_123", "pending");
    }

    @Test
    void approveCancellation_shouldIssueFullRefundBeforeCompletingPaidOrderCancellation() {
        AftercareRequestStateService stateService = mock(AftercareRequestStateService.class);
        RefundProvider refundProvider = mock(RefundProvider.class);
        RefundExecution refund = new RefundExecution(8L, 15L, "pay_123", new BigDecimal("129.00"));
        when(stateService.beginCancellationApproval(4L))
                .thenReturn(new CancellationExecution(4L, 15L, refund, false));
        when(refundProvider.refund("pay_123", 12900, "cancel-refund-4"))
                .thenReturn(new RefundProvider.ProviderRefundResult("rfnd_456", "processed"));
        AftercareAdminService service = new AftercareAdminService(stateService, refundProvider,
                mock(com.riceoil.platform.orders.application.OrderAftercareMailer.class));

        service.approveCancellation(4L);

        verify(stateService).completeRefund(8L, "rfnd_456", "processed");
        verify(stateService).completeCancellation(4L);
    }
}

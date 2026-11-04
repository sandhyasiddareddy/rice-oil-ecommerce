package com.riceoil.platform.orders.api.aftercare;

import com.riceoil.platform.orders.application.aftercare.AftercareAdminService;
import com.riceoil.platform.orders.application.aftercare.AdminAftercareRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/aftercare")
public class AdminAftercareController {

    private final AftercareAdminService aftercareAdminService;

    public AdminAftercareController(AftercareAdminService aftercareAdminService) {
        this.aftercareAdminService = aftercareAdminService;
    }

    @GetMapping("/refunds")
    public List<AdminAftercareRequest> pendingRefunds() {
        return aftercareAdminService.pendingRefundRequests();
    }

    @PostMapping("/refunds/{requestId}/approve")
    public void approveRefund(@PathVariable Long requestId) {
        aftercareAdminService.approveRefund(requestId);
    }

    @PostMapping("/refunds/{requestId}/reject")
    public void rejectRefund(@PathVariable Long requestId) {
        aftercareAdminService.rejectRefund(requestId);
    }

    @GetMapping("/cancellations")
    public List<AdminAftercareRequest> pendingCancellations() {
        return aftercareAdminService.pendingCancellationRequests();
    }

    @PostMapping("/cancellations/{requestId}/approve")
    public void approveCancellation(@PathVariable Long requestId) {
        aftercareAdminService.approveCancellation(requestId);
    }

    @PostMapping("/cancellations/{requestId}/reject")
    public void rejectCancellation(@PathVariable Long requestId) {
        aftercareAdminService.rejectCancellation(requestId);
    }
}

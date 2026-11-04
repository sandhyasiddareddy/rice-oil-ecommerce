package com.riceoil.platform.shipping.application;

import com.riceoil.platform.shipping.domain.ShippingFeeTierEntity;
import com.riceoil.platform.shipping.domain.ShippingFeeTierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ShippingFeeService {

    public record Tier(BigDecimal minOrderAmount, BigDecimal fee) {
    }

    private final ShippingFeeTierRepository repository;

    public ShippingFeeService(ShippingFeeTierRepository repository) {
        this.repository = repository;
    }

    /** Fee of the tier with the highest minimum that the amount reaches; 0 when no tiers are configured. */
    @Transactional(readOnly = true)
    public BigDecimal feeFor(BigDecimal orderAmount) {
        BigDecimal amount = orderAmount == null ? BigDecimal.ZERO : orderAmount;
        BigDecimal fee = BigDecimal.ZERO;
        for (ShippingFeeTierEntity tier : repository.findAllByOrderByMinOrderAmountAsc()) {
            if (amount.compareTo(tier.getMinOrderAmount()) >= 0) {
                fee = tier.getFee();
            }
        }
        return fee.setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public List<Tier> listTiers() {
        return repository.findAllByOrderByMinOrderAmountAsc().stream()
                .map(t -> new Tier(t.getMinOrderAmount(), t.getFee()))
                .toList();
    }

    @Transactional
    public List<Tier> replaceTiers(List<Tier> tiers) {
        Set<BigDecimal> seen = new HashSet<>();
        for (Tier tier : tiers) {
            if (tier.minOrderAmount() == null || tier.fee() == null
                    || tier.minOrderAmount().signum() < 0 || tier.fee().signum() < 0) {
                throw new IllegalArgumentException("Tier amounts must be present and not negative");
            }
            if (!seen.add(tier.minOrderAmount().setScale(2, RoundingMode.HALF_UP))) {
                throw new IllegalArgumentException("Duplicate tier minimum: " + tier.minOrderAmount());
            }
        }
        repository.deleteAllInBatch();
        for (Tier tier : tiers) {
            ShippingFeeTierEntity entity = new ShippingFeeTierEntity();
            entity.setMinOrderAmount(tier.minOrderAmount().setScale(2, RoundingMode.HALF_UP));
            entity.setFee(tier.fee().setScale(2, RoundingMode.HALF_UP));
            repository.save(entity);
        }
        return listTiers();
    }
}

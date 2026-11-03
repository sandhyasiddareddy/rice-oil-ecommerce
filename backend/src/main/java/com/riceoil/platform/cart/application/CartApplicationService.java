package com.riceoil.platform.cart.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.riceoil.platform.cart.domain.CartEntity;
import com.riceoil.platform.cart.domain.CartItemEntity;
import com.riceoil.platform.cart.domain.CartItemRepository;
import com.riceoil.platform.cart.domain.CartRepository;
import com.riceoil.platform.cart.domain.CartStatus;
import com.riceoil.platform.shared.web.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class CartApplicationService {

    private static final String INR = "INR";

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CheckoutQuoteService checkoutQuoteService;
    private final ObjectMapper objectMapper;

    public CartApplicationService(CartRepository cartRepository,
                                  CartItemRepository cartItemRepository,
                                  CheckoutQuoteService checkoutQuoteService,
                                  ObjectMapper objectMapper) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.checkoutQuoteService = checkoutQuoteService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public CartSnapshot createCart(String currency) {
        return createCart(currency, null);
    }

    @Transactional
    public CartSnapshot createCart(String currency, Long customerId) {
        CartEntity cart = new CartEntity();
        cart.setStatus(CartStatus.ACTIVE);
        cart.setCurrency(normalizeCurrency(currency));
        cart.setCustomerId(customerId);
        String guestAccessToken = null;
        if (customerId == null) {
            guestAccessToken = GuestCartAccessToken.generate();
            cart.setGuestAccessTokenHash(GuestCartAccessToken.hash(guestAccessToken));
        }
        cart.setSubtotalAmount(money(BigDecimal.ZERO));
        cart.setTaxAmount(money(BigDecimal.ZERO));
        cart.setDiscountAmount(money(BigDecimal.ZERO));
        cart.setTotalAmount(money(BigDecimal.ZERO));

        CartEntity saved = cartRepository.save(cart);
        return toSnapshot(saved, List.of(), List.of(), guestAccessToken);
    }

    @Transactional(readOnly = true)
    public CartSnapshot getCart(Long cartId) {
        return getCart(cartId, null);
    }

    @Transactional
    public CartSnapshot getCart(Long cartId, Long customerId) {
        return getCart(cartId, customerId, null);
    }

    @Transactional
    public CartSnapshot getCart(Long cartId, Long customerId, String guestAccessToken) {
        CartEntity cart = findCart(cartId, customerId, guestAccessToken);
        List<CartItemEntity> items = cartItemRepository.findByCartIdOrderByIdAsc(cartId);
        return toSnapshot(cart, items, readCouponCodes(cart.getCouponCodesJson()));
    }

    @Transactional
    public CartSnapshot addItem(Long cartId, AddCartItemCommand command) {
        return addItem(cartId, command, null);
    }

    @Transactional
    public CartSnapshot addItem(Long cartId, AddCartItemCommand command, Long customerId) {
        return addItem(cartId, command, customerId, null);
    }

    @Transactional
    public CartSnapshot addItem(Long cartId, AddCartItemCommand command, Long customerId, String guestAccessToken) {
        if (command.quantity() <= 0) {
            throw new IllegalArgumentException("quantity must be greater than zero");
        }
        if (command.unitPrice() == null || command.unitPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("unitPrice must be zero or greater");
        }

        CartEntity cart = findCart(cartId, customerId, guestAccessToken);
        CartItemEntity item = new CartItemEntity();
        item.setCart(cart);
        item.setSku(command.sku().trim().toUpperCase());
        item.setProductName(command.productName().trim());
        item.setUnitPrice(money(command.unitPrice()));
        item.setQuantity(command.quantity());
        item.setLineTotal(money(command.unitPrice().multiply(BigDecimal.valueOf(command.quantity()))));

        cartItemRepository.save(item);
        List<CartItemEntity> items = cartItemRepository.findByCartIdOrderByIdAsc(cartId);
        return toSnapshot(cart, items, readCouponCodes(cart.getCouponCodesJson()));
    }

    @Transactional
    public CartSnapshot updateItem(Long cartId, Long itemId, UpdateCartItemCommand command) {
        return updateItem(cartId, itemId, command, null);
    }

    @Transactional
    public CartSnapshot updateItem(Long cartId, Long itemId, UpdateCartItemCommand command, Long customerId) {
        return updateItem(cartId, itemId, command, customerId, null);
    }

    @Transactional
    public CartSnapshot updateItem(Long cartId, Long itemId, UpdateCartItemCommand command, Long customerId,
                                   String guestAccessToken) {
        CartEntity cart = findCart(cartId, customerId, guestAccessToken);
        CartItemEntity item = cartItemRepository.findByIdAndCartId(itemId, cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found: " + itemId));

        BigDecimal unitPrice = command.unitPrice() == null ? item.getUnitPrice() : command.unitPrice();
        int quantity = command.quantity() == null ? item.getQuantity() : command.quantity();

        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be greater than zero");
        }
        if (unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("unitPrice must be zero or greater");
        }

        item.setUnitPrice(money(unitPrice));
        item.setQuantity(quantity);
        item.setLineTotal(money(unitPrice.multiply(BigDecimal.valueOf(quantity))));
        cartItemRepository.save(item);

        List<CartItemEntity> items = cartItemRepository.findByCartIdOrderByIdAsc(cartId);
        return toSnapshot(cart, items, readCouponCodes(cart.getCouponCodesJson()));
    }

    @Transactional
    public CartSnapshot removeItem(Long cartId, Long itemId) {
        return removeItem(cartId, itemId, null);
    }

    @Transactional
    public CartSnapshot removeItem(Long cartId, Long itemId, Long customerId) {
        return removeItem(cartId, itemId, customerId, null);
    }

    @Transactional
    public CartSnapshot removeItem(Long cartId, Long itemId, Long customerId, String guestAccessToken) {
        CartEntity cart = findCart(cartId, customerId, guestAccessToken);
        CartItemEntity item = cartItemRepository.findByIdAndCartId(itemId, cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found: " + itemId));

        cartItemRepository.delete(item);
        List<CartItemEntity> items = cartItemRepository.findByCartIdOrderByIdAsc(cartId);
        return toSnapshot(cart, items, readCouponCodes(cart.getCouponCodesJson()));
    }

    @Transactional
    public CartSnapshot refreshPricing(Long cartId, CartPricingRefreshCommand command) {
        return refreshPricing(cartId, command, null);
    }

    @Transactional
    public CartSnapshot refreshPricing(Long cartId, CartPricingRefreshCommand command, Long customerId) {
        return refreshPricing(cartId, command, customerId, null);
    }

    @Transactional
    public CartSnapshot refreshPricing(Long cartId, CartPricingRefreshCommand command, Long customerId,
                                       String guestAccessToken) {
        CartEntity cart = findCart(cartId, customerId, guestAccessToken);
        List<CartItemEntity> items = cartItemRepository.findByCartIdOrderByIdAsc(cartId);

        BigDecimal subtotal = items.stream()
                .map(CartItemEntity::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        CheckoutQuoteResult quote = checkoutQuoteService.quote(
            new CheckoutQuoteRequest(subtotal, command.taxCode(), command.couponCodes(), null)
        );

        cart.setTaxCode(quote.taxCode());
        cart.setCouponCodesJson(writeCouponCodes(command.couponCodes()));
        cart.setSubtotalAmount(quote.baseAmount());
        cart.setTaxAmount(quote.taxAmount());
        cart.setDiscountAmount(quote.discountAmount());
        cart.setTotalAmount(quote.finalAmount());
        CartEntity saved = cartRepository.save(cart);

        List<String> coupons = command.couponCodes() == null ? List.of() : command.couponCodes();
        return toSnapshot(saved, items, coupons);
    }

    @Transactional
    public CartSnapshot getOrCreateCustomerCart(Long customerId, Long guestCartId) {
        return getOrCreateCustomerCart(customerId, guestCartId, null);
    }

    @Transactional
    public CartSnapshot getOrCreateCustomerCart(Long customerId, Long guestCartId, String guestAccessToken) {
        CartEntity customerCart = cartRepository.findFirstByCustomerIdAndStatusOrderByUpdatedAtDesc(customerId, CartStatus.ACTIVE)
                .orElse(null);
        CartEntity guestCart = guestCartId == null ? null : cartRepository.findById(guestCartId).orElse(null);

        if (guestCartId != null && guestCart == null) {
            throw new ResourceNotFoundException("Guest cart not found: " + guestCartId);
        }
        if (guestCart != null) {
            if (guestCart.getStatus() != CartStatus.ACTIVE
                    || (guestCart.getCustomerId() != null && !guestCart.getCustomerId().equals(customerId))
                    || (guestCart.getCustomerId() == null
                        && !GuestCartAccessToken.matches(guestAccessToken, guestCart.getGuestAccessTokenHash()))) {
                throw new AccessDeniedException("Guest cart token is invalid");
            }
        }

        if (guestCart != null
                && (customerCart == null || guestCart.getCurrency().equals(customerCart.getCurrency()))) {
            if (customerCart == null || customerCart.getId().equals(guestCart.getId())) {
                guestCart.setCustomerId(customerId);
                guestCart.setGuestAccessTokenHash(null);
                customerCart = cartRepository.save(guestCart);
            } else {
                mergeGuestCart(guestCart, customerCart);
                guestCart.setStatus(CartStatus.EXPIRED);
                guestCart.setGuestAccessTokenHash(null);
                cartRepository.save(guestCart);
            }
        }

        if (customerCart == null) {
            CartEntity newCart = new CartEntity();
            newCart.setCustomerId(customerId);
            newCart.setStatus(CartStatus.ACTIVE);
            newCart.setCurrency(INR);
            newCart.setSubtotalAmount(money(BigDecimal.ZERO));
            newCart.setTaxAmount(money(BigDecimal.ZERO));
            newCart.setDiscountAmount(money(BigDecimal.ZERO));
            newCart.setTotalAmount(money(BigDecimal.ZERO));
            customerCart = cartRepository.save(newCart);
        }

        List<CartItemEntity> items = cartItemRepository.findByCartIdOrderByIdAsc(customerCart.getId());
        return toSnapshot(customerCart, items, readCouponCodes(customerCart.getCouponCodesJson()));
    }

    private void mergeGuestCart(CartEntity guestCart, CartEntity customerCart) {
        List<CartItemEntity> customerItems = cartItemRepository.findByCartIdOrderByIdAsc(customerCart.getId());
        List<CartItemEntity> guestItems = cartItemRepository.findByCartIdOrderByIdAsc(guestCart.getId());
        for (CartItemEntity guestItem : guestItems) {
            CartItemEntity matching = customerItems.stream()
                    .filter(item -> item.getSku().equalsIgnoreCase(guestItem.getSku()))
                    .findFirst()
                    .orElse(null);
            if (matching == null) {
                CartItemEntity copy = new CartItemEntity();
                copy.setCart(customerCart);
                copy.setSku(guestItem.getSku());
                copy.setProductName(guestItem.getProductName());
                copy.setUnitPrice(guestItem.getUnitPrice());
                copy.setQuantity(guestItem.getQuantity());
                copy.setLineTotal(guestItem.getLineTotal());
                customerItems.add(cartItemRepository.save(copy));
            } else {
                matching.setQuantity(matching.getQuantity() + guestItem.getQuantity());
                matching.setLineTotal(money(matching.getUnitPrice().multiply(BigDecimal.valueOf(matching.getQuantity()))));
                cartItemRepository.save(matching);
            }
        }

        BigDecimal subtotal = customerItems.stream()
                .map(CartItemEntity::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        CheckoutQuoteResult quote = checkoutQuoteService.quote(new CheckoutQuoteRequest(
                subtotal,
                customerCart.getTaxCode(),
                readCouponCodes(customerCart.getCouponCodesJson()),
                null
        ));
        customerCart.setSubtotalAmount(quote.baseAmount());
        customerCart.setTaxAmount(quote.taxAmount());
        customerCart.setDiscountAmount(quote.discountAmount());
        customerCart.setTotalAmount(quote.finalAmount());
        cartRepository.save(customerCart);
    }

    public void verifyCartAccess(CartEntity cart, Long customerId, String guestAccessToken) {
        if (cart.getCustomerId() != null) {
            if (customerId == null || !cart.getCustomerId().equals(customerId)) {
                throw new AccessDeniedException("Cart does not belong to the authenticated customer");
            }
        } else if (customerId != null || !GuestCartAccessToken.matches(
                guestAccessToken,
                cart.getGuestAccessTokenHash()
        )) {
            throw new AccessDeniedException("Guest cart token is invalid");
        }
    }

    private CartEntity findCart(Long cartId, Long customerId, String guestAccessToken) {
        CartEntity cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found: " + cartId));
        verifyCartAccess(cart, customerId, guestAccessToken);
        return cart;
    }

    private CartSnapshot toSnapshot(CartEntity cart, List<CartItemEntity> items, List<String> couponCodes) {
        return toSnapshot(cart, items, couponCodes, null);
    }

    private CartSnapshot toSnapshot(CartEntity cart, List<CartItemEntity> items, List<String> couponCodes,
                                   String guestAccessToken) {
        List<CartItemSnapshot> itemSnapshots = items.stream()
                .map(item -> new CartItemSnapshot(
                        item.getId(),
                        item.getSku(),
                        item.getProductName(),
                        item.getUnitPrice(),
                        item.getQuantity(),
                        item.getLineTotal()
                ))
                .toList();

        return new CartSnapshot(
                cart.getId(),
                cart.getStatus(),
                cart.getCurrency(),
                cart.getTaxCode(),
                cart.getSubtotalAmount(),
                cart.getTaxAmount(),
                cart.getDiscountAmount(),
                cart.getTotalAmount(),
                couponCodes,
                itemSnapshots,
                guestAccessToken
        );
    }

    private String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return INR;
        }
        String normalized = currency.trim().toUpperCase();
        if (normalized.length() != 3) {
            throw new IllegalArgumentException("currency must be a 3-letter code");
        }
        return normalized;
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String writeCouponCodes(List<String> couponCodes) {
        try {
            List<String> list = couponCodes == null ? List.of() : couponCodes;
            return objectMapper.writeValueAsString(list);
        } catch (Exception ex) {
            return "[]";
        }
    }

    private List<String> readCouponCodes(String couponCodesJson) {
        if (couponCodesJson == null || couponCodesJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(couponCodesJson, new TypeReference<ArrayList<String>>() {
            });
        } catch (Exception ex) {
            return List.of();
        }
    }
}

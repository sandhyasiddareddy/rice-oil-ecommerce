package com.riceoil.platform.orders.api.checkout;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.riceoil.platform.cart.domain.CartEntity;
import com.riceoil.platform.cart.domain.CartItemEntity;
import com.riceoil.platform.cart.domain.CartItemRepository;
import com.riceoil.platform.cart.domain.CartRepository;
import com.riceoil.platform.cart.domain.CartStatus;
import com.riceoil.platform.cart.application.GuestCartAccessToken;
import com.riceoil.platform.orders.domain.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "app.catalog.sample-data.enabled=false",
    "spring.flyway.enabled=false",
    "spring.datasource.url=jdbc:h2:mem:platform-orders-it;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class CheckoutOrderIdempotencyIntegrationTest {

    private static final String GUEST_CART_TOKEN = "guest-cart-integration-test-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
    }

    @Test
    void placeOrder_sameIdempotencyKey_shouldReplayExistingOrder() throws Exception {
        Long cartId = seedActiveCartWithItem();

        MvcResult firstResponse = postOrder("idem-replay-1", cartId);
        MvcResult secondResponse = postOrder("idem-replay-1", cartId);

        JsonNode firstJson = objectMapper.readTree(firstResponse.getResponse().getContentAsString());
        JsonNode secondJson = objectMapper.readTree(secondResponse.getResponse().getContentAsString());

        assertEquals(firstJson.get("id").asLong(), secondJson.get("id").asLong());
        assertEquals(firstJson.get("orderNumber").asText(), secondJson.get("orderNumber").asText());
        assertEquals(1L, orderRepository.count());

        CartEntity updatedCart = cartRepository.findById(cartId).orElseThrow();
        assertEquals(CartStatus.CHECKED_OUT, updatedCart.getStatus());
    }

    private MvcResult postOrder(String idempotencyKey, Long cartId) throws Exception {
        return mockMvc.perform(post("/api/checkout/orders")
                        .header("Idempotency-Key", idempotencyKey)
                        .header("X-Guest-Cart-Token", GUEST_CART_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cartId\":" + cartId + ",\"customerEmail\":\"Buyer@Example.com\","
                                + "\"shippingAddress\":{\"fullName\":\"Buyer\",\"phone\":\"9876543210\","
                                + "\"line1\":\"1 Main Street\",\"city\":\"Chennai\",\"state\":\"Tamil Nadu\","
                                + "\"postalCode\":\"600001\",\"country\":\"IN\"}}"))
                .andExpect(status().isOk())
                .andReturn();
    }

    private Long seedActiveCartWithItem() {
        CartEntity cart = new CartEntity();
        cart.setStatus(CartStatus.ACTIVE);
        cart.setGuestAccessTokenHash(GuestCartAccessToken.hash(GUEST_CART_TOKEN));
        cart.setCurrency("INR");
        cart.setTaxCode("GST5");
        cart.setSubtotalAmount(new BigDecimal("200.00"));
        cart.setTaxAmount(new BigDecimal("10.00"));
        cart.setDiscountAmount(new BigDecimal("20.00"));
        cart.setTotalAmount(new BigDecimal("190.00"));
        CartEntity savedCart = cartRepository.save(cart);

        CartItemEntity item = new CartItemEntity();
        item.setCart(savedCart);
        item.setSku("RICE-5KG");
        item.setProductName("Rice");
        item.setUnitPrice(new BigDecimal("100.00"));
        item.setQuantity(2);
        item.setLineTotal(new BigDecimal("200.00"));
        cartItemRepository.save(item);

        assertTrue(savedCart.getId() != null);
        return savedCart.getId();
    }
}

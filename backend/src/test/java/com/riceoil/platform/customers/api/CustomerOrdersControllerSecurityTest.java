package com.riceoil.platform.customers.api;

import com.riceoil.platform.customers.application.CustomerOrderQueryService;
import com.riceoil.platform.shared.web.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerOrdersController.class)
@Import(SecurityConfig.class)
class CustomerOrdersControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerOrderQueryService customerOrderQueryService;

    @Test
    void emailHeaderAlone_shouldNotExposeCustomerOrders() throws Exception {
        mockMvc.perform(get("/api/customers/me/orders")
                        .header("X-Customer-Email", "buyer@example.com"))
                .andExpect(status().isUnauthorized());
    }
}
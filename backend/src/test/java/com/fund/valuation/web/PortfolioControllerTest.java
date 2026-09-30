package com.fund.valuation.web;

import com.fund.valuation.domain.User;
import com.fund.valuation.dto.PortfolioSummaryView;
import com.fund.valuation.dto.PortfolioView;
import com.fund.valuation.service.AuthService;
import com.fund.valuation.service.PortfolioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PortfolioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private PortfolioService portfolioService;

    @Test
    void nonVipUserIsRejectedWithForbidden() throws Exception {
        User normalUser = new User();
        normalUser.setUsername("normal_user");
        normalUser.setRole(User.ROLE_USER);
        normalUser.setIsVip(false);

        when(authService.resolveUsername("token_normal")).thenReturn("normal_user");
        when(authService.getUser("normal_user")).thenReturn(normalUser);

        mockMvc.perform(get("/api/portfolio")
                        .header("Authorization", "Bearer token_normal"))
                .andExpect(status().isForbidden());
    }

    @Test
    void vipUserCanAccessPortfolio() throws Exception {
        User vipUser = new User();
        vipUser.setUsername("vip_user");
        vipUser.setRole(User.ROLE_USER);
        vipUser.setIsVip(true);

        when(authService.resolveUsername("token_vip")).thenReturn("vip_user");
        when(authService.getUser("vip_user")).thenReturn(vipUser);
        PortfolioSummaryView summary = new PortfolioSummaryView(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
        );
        when(portfolioService.getPortfolio("vip_user")).thenReturn(new PortfolioView(summary, List.of()));

        mockMvc.perform(get("/api/portfolio")
                        .header("Authorization", "Bearer token_vip"))
                .andExpect(status().isOk());
    }
}

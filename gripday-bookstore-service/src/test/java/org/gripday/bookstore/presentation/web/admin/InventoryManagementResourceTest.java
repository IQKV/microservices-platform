package org.gripday.bookstore.presentation.web.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.gripday.bookstore.domain.dto.BulkInventoryRequest;
import org.gripday.bookstore.domain.dto.InventoryDto;
import org.gripday.bookstore.domain.dto.UpdateInventoryRequest;
import org.gripday.bookstore.domain.dto.UserContext;
import org.gripday.bookstore.domain.exception.InsufficientInventoryException;
import org.gripday.bookstore.domain.exception.UnauthorizedOperationException;
import org.gripday.bookstore.domain.service.InventoryService;
import org.gripday.bookstore.presentation.web.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InventoryManagementResource.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestSecurityConfig.class)
class InventoryManagementResourceTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private InventoryService inventoryService;

  private InventoryDto createTestInventoryDto() {
    return new InventoryDto(
        1L,
        "Test Book",
        50,
        5,
        45,
        10,
        false,
        LocalDateTime.now()
    );
  }

  private UserContext createAdminUserContext() {
    return new UserContext(
        1L,
        "admin",
        "admin@example.com",
        Set.of("ADMIN"),
        Set.of("INVENTORY_WRITE"),
        "IT",
        "org1",
        Map.of()
    );
  }

  private UserContext createRegularUserContext() {
    return new UserContext(
        2L,
        "user",
        "user@example.com",
        Set.of("USER"),
        Set.of("INVENTORY_READ"),
        "Sales",
        "org1",
        Map.of()
    );
  }

  @Test
  void updateInventory_WithValidRequestAndAdminUser_ShouldUpdateInventory() throws Exception {
    var request = new UpdateInventoryRequest(75, 15);
    var updatedInventory = createTestInventoryDto();
    var userContext = createAdminUserContext();

    when(inventoryService.updateInventory(eq(1L), any(UpdateInventoryRequest.class), any(UserContext.class)))
        .thenReturn(updatedInventory);

    mockMvc.perform(put("/api/v1/bookstore/admin/inventory/1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .requestAttr("userContext", userContext))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.bookId").value(1))
        .andExpect(jsonPath("$.quantity").value(50));
  }

  @Test
  void updateInventory_WithInvalidRequest_ShouldReturn400() throws Exception {
    var invalidRequest = new UpdateInventoryRequest(-10, -5);
    var userContext = createAdminUserContext();

    mockMvc.perform(put("/api/v1/bookstore/admin/inventory/1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalidRequest))
            .requestAttr("userContext", userContext))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void updateInventory_WithUnauthorizedUser_ShouldReturn403() throws Exception {
    var request = new UpdateInventoryRequest(75, 15);
    var userContext = createRegularUserContext();

    when(inventoryService.updateInventory(eq(1L), any(UpdateInventoryRequest.class), any(UserContext.class)))
        .thenThrow(new UnauthorizedOperationException("Insufficient privileges"));

    mockMvc.perform(put("/api/v1/bookstore/admin/inventory/1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .requestAttr("userContext", userContext))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("AUTH_INSUFFICIENT_PRIVILEGES"));
  }

  @Test
  void bulkUpdateInventory_WithValidRequestAndAdminUser_ShouldUpdateInventories() throws Exception {
    var requests = List.of(
        new BulkInventoryRequest(1L, 50, 10),
        new BulkInventoryRequest(2L, 30, 5)
    );
    var updatedInventories = List.of(createTestInventoryDto());
    var userContext = createAdminUserContext();

    when(inventoryService.bulkUpdateInventory(anyList(), any(UserContext.class)))
        .thenReturn(updatedInventories);

    mockMvc.perform(post("/api/v1/bookstore/admin/inventory/bulk-update")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(requests))
            .requestAttr("userContext", userContext))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].bookId").value(1));
  }

  @Test
  void reserveQuantity_WithValidRequestAndAdminUser_ShouldReserveQuantity() throws Exception {
    var userContext = createAdminUserContext();

    mockMvc.perform(post("/api/v1/bookstore/admin/inventory/1/reserve")
            .param("quantity", "5")
            .requestAttr("userContext", userContext))
        .andExpect(status().isOk());
  }

  @Test
  void reserveQuantity_WithInsufficientInventory_ShouldReturn409() throws Exception {
    var userContext = createAdminUserContext();

    doThrow(new InsufficientInventoryException(1L, 100, 45))
        .when(inventoryService).reserveQuantity(eq(1L), eq(100), any(UserContext.class));

    mockMvc.perform(post("/api/v1/bookstore/admin/inventory/1/reserve")
            .param("quantity", "100")
            .requestAttr("userContext", userContext))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("DOMAIN_INSUFFICIENT_INVENTORY"));
  }

  @Test
  void releaseReservedQuantity_WithValidRequestAndAdminUser_ShouldReleaseQuantity() throws Exception {
    var userContext = createAdminUserContext();

    mockMvc.perform(post("/api/v1/bookstore/admin/inventory/1/release")
            .param("quantity", "3")
            .requestAttr("userContext", userContext))
        .andExpect(status().isOk());
  }

  @Test
  void adjustInventoryQuantity_WithValidRequestAndAdminUser_ShouldAdjustQuantity() throws Exception {
    var userContext = createAdminUserContext();

    mockMvc.perform(post("/api/v1/bookstore/admin/inventory/1/adjust")
            .param("adjustment", "10")
            .requestAttr("userContext", userContext))
        .andExpect(status().isOk());
  }

  @Test
  void adjustInventoryQuantity_WithNegativeAdjustment_ShouldWork() throws Exception {
    var userContext = createAdminUserContext();

    mockMvc.perform(post("/api/v1/bookstore/admin/inventory/1/adjust")
            .param("adjustment", "-5")
            .requestAttr("userContext", userContext))
        .andExpect(status().isOk());
  }
}

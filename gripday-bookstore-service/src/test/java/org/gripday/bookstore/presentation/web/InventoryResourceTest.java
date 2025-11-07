package org.gripday.bookstore.presentation.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
import org.gripday.bookstore.domain.exception.BookNotFoundException;
import org.gripday.bookstore.domain.exception.InsufficientInventoryException;
import org.gripday.bookstore.domain.exception.UnauthorizedOperationException;
import org.gripday.bookstore.domain.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InventoryResource.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestSecurityConfig.class)
class InventoryResourceTest {

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
  void getInventory_WhenInventoryExists_ShouldReturnInventory() throws Exception {
    var inventory = createTestInventoryDto();
    when(inventoryService.getInventory(1L)).thenReturn(inventory);

    mockMvc.perform(get("/api/v1/bookstore/inventory/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.bookId").value(1))
        .andExpect(jsonPath("$.bookTitle").value("Test Book"))
        .andExpect(jsonPath("$.quantity").value(50))
        .andExpect(jsonPath("$.availableQuantity").value(45));
  }

  @Test
  void getInventory_WhenBookNotFound_ShouldReturn404() throws Exception {
    when(inventoryService.getInventory(999L))
        .thenThrow(new BookNotFoundException(999L));

    mockMvc.perform(get("/api/v1/bookstore/inventory/999"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
  }

  @Test
  void checkAvailability_WhenAvailable_ShouldReturnTrue() throws Exception {
    when(inventoryService.isBookAvailable(1L, 5)).thenReturn(true);

    mockMvc.perform(get("/api/v1/bookstore/inventory/1/availability")
            .param("quantity", "5"))
        .andExpect(status().isOk())
        .andExpect(content().string("true"));
  }

  @Test
  void checkAvailability_WhenNotAvailable_ShouldReturnFalse() throws Exception {
    when(inventoryService.isBookAvailable(1L, 100)).thenReturn(false);

    mockMvc.perform(get("/api/v1/bookstore/inventory/1/availability")
            .param("quantity", "100"))
        .andExpect(status().isOk())
        .andExpect(content().string("false"));
  }

  @Test
  void checkAvailability_WithDefaultQuantity_ShouldUseOne() throws Exception {
    when(inventoryService.isBookAvailable(1L, 1)).thenReturn(true);

    mockMvc.perform(get("/api/v1/bookstore/inventory/1/availability"))
        .andExpect(status().isOk())
        .andExpect(content().string("true"));
  }

  @Test
  void updateInventory_WithValidRequestAndAdminUser_ShouldUpdateInventory() throws Exception {
    var request = new UpdateInventoryRequest(75, 15);
    var updatedInventory = createTestInventoryDto();
    var userContext = createAdminUserContext();

    when(inventoryService.updateInventory(eq(1L), any(UpdateInventoryRequest.class), any(UserContext.class)))
        .thenReturn(updatedInventory);

    mockMvc.perform(put("/api/v1/bookstore/inventory/1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .requestAttr("userContext", userContext))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.bookId").value(1))
        .andExpect(jsonPath("$.quantity").value(50));
  }

  @Test
  void updateInventory_WithInvalidRequest_ShouldReturn400() throws Exception {
    var invalidRequest = new UpdateInventoryRequest(-10, -5); // Invalid negative values
    var userContext = createAdminUserContext();

    mockMvc.perform(put("/api/v1/bookstore/inventory/1")
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

    mockMvc.perform(put("/api/v1/bookstore/inventory/1")
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

    mockMvc.perform(post("/api/v1/bookstore/inventory/bulk-update")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(requests))
            .requestAttr("userContext", userContext))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].bookId").value(1));
  }

  @Test
  void getLowStockInventory_ShouldReturnLowStockItems() throws Exception {
    var lowStockInventory = createTestInventoryDto();
    when(inventoryService.getLowStockInventory()).thenReturn(List.of(lowStockInventory));

    mockMvc.perform(get("/api/v1/bookstore/inventory/low-stock"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].bookId").value(1));
  }

  @Test
  void getOutOfStockInventory_ShouldReturnOutOfStockItems() throws Exception {
    var outOfStockInventory = createTestInventoryDto();
    when(inventoryService.getOutOfStockInventory()).thenReturn(List.of(outOfStockInventory));

    mockMvc.perform(get("/api/v1/bookstore/inventory/out-of-stock"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].bookId").value(1));
  }

  @Test
  void reserveQuantity_WithValidRequestAndAdminUser_ShouldReserveQuantity() throws Exception {
    var userContext = createAdminUserContext();

    mockMvc.perform(post("/api/v1/bookstore/inventory/1/reserve")
            .param("quantity", "5")
            .requestAttr("userContext", userContext))
        .andExpect(status().isOk());
  }

  @Test
  void reserveQuantity_WithInsufficientInventory_ShouldReturn409() throws Exception {
    var userContext = createAdminUserContext();

    doThrow(new InsufficientInventoryException(1L, 100, 45))
        .when(inventoryService).reserveQuantity(eq(1L), eq(100), any(UserContext.class));

    mockMvc.perform(post("/api/v1/bookstore/inventory/1/reserve")
            .param("quantity", "100")
            .requestAttr("userContext", userContext))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("DOMAIN_INSUFFICIENT_INVENTORY"));
  }

  @Test
  void releaseReservedQuantity_WithValidRequestAndAdminUser_ShouldReleaseQuantity() throws Exception {
    var userContext = createAdminUserContext();

    mockMvc.perform(post("/api/v1/bookstore/inventory/1/release")
            .param("quantity", "3")
            .requestAttr("userContext", userContext))
        .andExpect(status().isOk());
  }

  @Test
  void adjustInventoryQuantity_WithValidRequestAndAdminUser_ShouldAdjustQuantity() throws Exception {
    var userContext = createAdminUserContext();

    mockMvc.perform(post("/api/v1/bookstore/inventory/1/adjust")
            .param("adjustment", "10")
            .requestAttr("userContext", userContext))
        .andExpect(status().isOk());
  }

  @Test
  void adjustInventoryQuantity_WithNegativeAdjustment_ShouldWork() throws Exception {
    var userContext = createAdminUserContext();

    mockMvc.perform(post("/api/v1/bookstore/inventory/1/adjust")
            .param("adjustment", "-5")
            .requestAttr("userContext", userContext))
        .andExpect(status().isOk());
  }

  @Test
  void getTotalInventoryCount_ShouldReturnTotalCount() throws Exception {
    when(inventoryService.getTotalInventoryCount()).thenReturn(1000L);

    mockMvc.perform(get("/api/v1/bookstore/inventory/stats/total-count"))
        .andExpect(status().isOk())
        .andExpect(content().string("1000"));
  }

  @Test
  void getTotalReservedCount_ShouldReturnReservedCount() throws Exception {
    when(inventoryService.getTotalReservedCount()).thenReturn(50L);

    mockMvc.perform(get("/api/v1/bookstore/inventory/stats/reserved-count"))
        .andExpect(status().isOk())
        .andExpect(content().string("50"));
  }

  @Test
  void getLowStockCount_ShouldReturnLowStockCount() throws Exception {
    when(inventoryService.countLowStockItems()).thenReturn(15L);

    mockMvc.perform(get("/api/v1/bookstore/inventory/stats/low-stock-count"))
        .andExpect(status().isOk())
        .andExpect(content().string("15"));
  }

  @Test
  void getOutOfStockCount_ShouldReturnOutOfStockCount() throws Exception {
    when(inventoryService.countOutOfStockItems()).thenReturn(3L);

    mockMvc.perform(get("/api/v1/bookstore/inventory/stats/out-of-stock-count"))
        .andExpect(status().isOk())
        .andExpect(content().string("3"));
  }
}
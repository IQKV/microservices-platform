package org.gripday.bookstore.inventory;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.gripday.bookstore.catalog.BookNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InventoryResource.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(org.gripday.bookstore.shared.TestSecurityConfig.class)
class InventoryResourceTest {

  @Autowired
  private MockMvc mockMvc;

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

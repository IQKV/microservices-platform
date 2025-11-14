package org.gripday.userservice.presentation.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.gripday.userservice.domain.service.OrganizationManagementService;
import org.gripday.userservice.presentation.dto.CreateOrganizationRequest;
import org.gripday.userservice.presentation.dto.OrganizationDto;
import org.gripday.userservice.presentation.dto.UpdateOrganizationRequest;
import org.gripday.userservice.presentation.dto.UserContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = OrganizationManagementResource.class,
    excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
        type = org.springframework.context.annotation.FilterType.REGEX,
        pattern = "org\\.gripday\\.userservice\\.(config|infrastructure)\\..*"),
    properties = {"spring.jpa.hibernate.ddl-auto=none", "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration"})
@org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase(replace = org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE)
@Import(TestSecurityConfig.class)
class OrganizationManagementResourceTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private OrganizationManagementService organizationManagementService;

  private OrganizationDto testOrganizationDto;
  private UserContext adminUser;

  @BeforeEach
  void setUp() {
    adminUser = new UserContext(
        1L,
        "admin",
        "admin@test.com",
        Set.of("ADMIN"),
        Set.of(),
        "Admin",
        "User",
        "tenant-123",
        null
    );

    testOrganizationDto = new OrganizationDto(
        1L,
        "Test Org",
        "Description",
        "Technology",
        "https://test.com",
        "123-456",
        "123 Street",
        "City",
        "Country",
        true,
        1L,
        "testuser",
        "tenant-123",
        LocalDateTime.now(),
        LocalDateTime.now()
    );
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getAllOrganizations_Success() throws Exception {
    var page = new PageImpl<>(List.of(testOrganizationDto), PageRequest.of(0, 20), 1);
    when(organizationManagementService.getAllOrganizations(any(), any())).thenReturn(page);

    mockMvc.perform(get("/api/v1/organizations")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].name").value("Test Org"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getOrganizationById_Success() throws Exception {
    when(organizationManagementService.getOrganizationById(eq(1L), any())).thenReturn(testOrganizationDto);

    mockMvc.perform(get("/api/v1/organizations/1")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Test Org"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void createOrganization_Success() throws Exception {
    var request = new CreateOrganizationRequest(
        "New Org",
        "Description",
        "Tech",
        null,
        null,
        null,
        null,
        null,
        true,
        null
    );

    when(organizationManagementService.createOrganization(any(), any())).thenReturn(testOrganizationDto);

    mockMvc.perform(post("/api/v1/organizations")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Test Org"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void updateOrganization_Success() throws Exception {
    var request = new UpdateOrganizationRequest(
        "Updated Org",
        "Updated Description",
        null,
        null,
        null,
        null,
        null,
        null,
        true,
        null
    );

    when(organizationManagementService.updateOrganization(eq(1L), any(), any())).thenReturn(testOrganizationDto);

    mockMvc.perform(put("/api/v1/organizations/1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Test Org"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void deleteOrganization_Success() throws Exception {
    doNothing().when(organizationManagementService).deleteOrganization(eq(1L), any());

    mockMvc.perform(delete("/api/v1/organizations/1")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent());
  }
}

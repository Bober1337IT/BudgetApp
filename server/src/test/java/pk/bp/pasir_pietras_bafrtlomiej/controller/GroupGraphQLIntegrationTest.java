package pk.bp.pasir_pietras_bafrtlomiej.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import pk.bp.pasir_pietras_bafrtlomiej.dto.LoginDTO;
import pk.bp.pasir_pietras_bafrtlomiej.dto.UserDTO;
import pk.bp.pasir_pietras_bafrtlomiej.repository.DebtRepository;
import pk.bp.pasir_pietras_bafrtlomiej.repository.GroupRepository;
import pk.bp.pasir_pietras_bafrtlomiej.repository.MembershipRepository;
import pk.bp.pasir_pietras_bafrtlomiej.repository.TransactionRepository;
import pk.bp.pasir_pietras_bafrtlomiej.repository.UserRepository;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy integracyjne GraphQL dla grup (Lab 05).
 * <p>
 * Wzorowane na {@link AuthControllerIntegrationTest} — ten sam MockMvc, profil "test", H2, @Transactional.
 * Dla GraphQL requesty wymagają tokena JWT w nagłówku Authorization.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class GroupGraphQLIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private DebtRepository debtRepository;

    private static final String TEST_PASSWORD = "SecurePassword123";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        debtRepository.deleteAll();
        membershipRepository.deleteAll();
        groupRepository.deleteAll();
        transactionRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String generateUniqueEmail() {
        return "test_" + UUID.randomUUID().toString().substring(0, 8) + "@pk.pl";
    }

    private void registerUser(String email, String username) throws Exception {
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername(username);
        userDTO.setEmail(email);
        userDTO.setPassword(TEST_PASSWORD);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDTO)))
                .andDo(print())
                .andExpect(status().isOk());
    }

    private String loginAndGetToken(String email) throws Exception {
        LoginDTO loginDto = new LoginDTO();
        loginDto.setEmail(email);
        loginDto.setPassword(TEST_PASSWORD);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("token").asText();
    }

    private MvcResult graphql(String token, String query) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("query", query));

        return mockMvc.perform(post("/graphql")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    @Order(1)
    @DisplayName("Utworzenie grupy dodaje właściciela jako członka i zwraca grupę w myGroups")
    void shouldCreateGroupAndAddOwnerAsMember() throws Exception {
        String email = generateUniqueEmail();
        registerUser(email, "owner_user");
        String token = loginAndGetToken(email);

        String groupName = "Wspolne wakacje";

        String createGroupQuery = """
                mutation {
                  createGroup(groupDTO: { name: "%s" }) {
                    id
                    name
                    ownerId
                  }
                }
                """.formatted(groupName);

        MvcResult createResult = graphql(token, createGroupQuery);

        jsonPath("$.data.createGroup.name").value(groupName).match(createResult);
        jsonPath("$.data.createGroup.id").exists().match(createResult);
        jsonPath("$.data.createGroup.ownerId").exists().match(createResult);

        String groupId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("createGroup").path("id").asText();
        String ownerId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("createGroup").path("ownerId").asText();

        String myGroupsQuery = """
                query {
                  myGroups {
                    id
                    name
                    ownerId
                  }
                }
                """;

        MvcResult myGroupsResult = graphql(token, myGroupsQuery);

        jsonPath("$.data.myGroups.length()").value(1).match(myGroupsResult);
        jsonPath("$.data.myGroups[0].id").value(groupId).match(myGroupsResult);
        jsonPath("$.data.myGroups[0].name").value(groupName).match(myGroupsResult);
        jsonPath("$.data.myGroups[0].ownerId").value(ownerId).match(myGroupsResult);

        String groupMembersQuery = """
                query {
                  groupMembers(groupId: "%s") {
                    id
                    groupId
                    userId
                    userEmail
                  }
                }
                """.formatted(groupId);

        MvcResult membersResult = graphql(token, groupMembersQuery);

        jsonPath("$.data.groupMembers.length()").value(1).match(membersResult);
        jsonPath("$.data.groupMembers[0].groupId").value(groupId).match(membersResult);
        jsonPath("$.data.groupMembers[0].userId").value(ownerId).match(membersResult);
        jsonPath("$.data.groupMembers[0].userEmail").value(email).match(membersResult);
    }

    @Test
    @Order(2)
    @DisplayName("Owner nie może usunąć siebie z grupy przez removeMember")
    void shouldRejectRemovingOwnerFromGroup() throws Exception {
        String email = generateUniqueEmail();
        registerUser(email, "owner_user");
        String token = loginAndGetToken(email);

        String createGroupQuery = """
                mutation {
                  createGroup(groupDTO: { name: "Grupa test" }) {
                    id
                  }
                }
                """;

        MvcResult createResult = graphql(token, createGroupQuery);
        String groupId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("createGroup").path("id").asText();

        String groupMembersQuery = """
                query {
                  groupMembers(groupId: "%s") {
                    id
                    userEmail
                  }
                }
                """.formatted(groupId);

        MvcResult membersResult = graphql(token, groupMembersQuery);
        jsonPath("$.data.groupMembers.length()").value(1).match(membersResult);

        String membershipId = objectMapper.readTree(membersResult.getResponse().getContentAsString())
                .path("data").path("groupMembers").get(0).path("id").asText();

        String removeMemberQuery = """
                mutation {
                  removeMember(membershipId: "%s")
                }
                """.formatted(membershipId);

        MvcResult removeResult = graphql(token, removeMemberQuery);

        jsonPath("$.errors[0].message")
                .value("Nie można usunąć właściciela z jego grupy.")
                .match(removeResult);
        jsonPath("$.data.removeMember").doesNotExist().match(removeResult);

        MvcResult membersAfterRemove = graphql(token, groupMembersQuery);
        jsonPath("$.data.groupMembers.length()").value(1).match(membersAfterRemove);
        jsonPath("$.data.groupMembers[0].userEmail").value(email).match(membersAfterRemove);
    }

    @Test
    @Order(3)
    @DisplayName("Zwykły członek nie może usunąć grupy")
    void shouldRejectDeleteGroupByNonOwnerMember() throws Exception {

        String ownerEmail = generateUniqueEmail();
        String memberEmail = generateUniqueEmail();

        registerUser(ownerEmail, "owner_user");
        registerUser(memberEmail, "member_user");

        String ownerToken = loginAndGetToken(ownerEmail);
        String memberToken = loginAndGetToken(memberEmail);

        String createGroupQuery = """
                mutation {
                  createGroup(groupDTO: { name: "Grupa wspolna" }) {
                    id
                    name
                  }
                }
                """;

        MvcResult createResult = graphql(ownerToken, createGroupQuery);

        String groupId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("createGroup").path("id").asText();

        String addMemberQuery = """
                mutation {
                  addMember(membershipDTO: { groupId: "%s", userEmail: "%s" }) {
                    id
                    userEmail
                  }
                }
                """.formatted(groupId, memberEmail);

        MvcResult addMemberResult = graphql(ownerToken, addMemberQuery);

        jsonPath("$.data.addMember.userEmail").value(memberEmail).match(addMemberResult);

        String deleteGroupQuery = """
                mutation {
                  deleteGroup(id: "%s")
                }
                """.formatted(groupId);

        MvcResult deleteResult = graphql(memberToken, deleteGroupQuery);

        jsonPath("$.errors[0].message")
                .value("Tylko właściciel grupy może ją usunąć.")
                .match(deleteResult);
        jsonPath("$.data.deleteGroup").doesNotExist().match(deleteResult);

        String myGroupsQuery = """
                query {
                  myGroups {
                    id
                    name
                  }
                }
                """;

        MvcResult myGroupsResult = graphql(ownerToken, myGroupsQuery);

        jsonPath("$.data.myGroups.length()").value(1).match(myGroupsResult);
        jsonPath("$.data.myGroups[0].id").value(groupId).match(myGroupsResult);
    }

    @Test
    @Order(4)
    @DisplayName("Usunięcie grupy przez właściciela usuwa powiązane długi i grupę")
    void shouldDeleteGroupAndRelatedDebtsWhenOwnerDeletesGroup() throws Exception {
        String ownerEmail = generateUniqueEmail();
        String memberEmail = generateUniqueEmail();

        registerUser(ownerEmail, "owner_user");
        registerUser(memberEmail, "member_user");

        String ownerToken = loginAndGetToken(ownerEmail);
        String memberToken = loginAndGetToken(memberEmail);

        String createGroupQuery = """
                mutation {
                  createGroup(groupDTO: { name: "Grupa z dlugami" }) {
                    id
                    ownerId
                  }
                }
                """;

        MvcResult createResult = graphql(ownerToken, createGroupQuery);
        JsonNode createGroupNode = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("createGroup");

        String groupId = createGroupNode.path("id").asText();
        String ownerId = createGroupNode.path("ownerId").asText();
        long groupIdLong = Long.parseLong(groupId);

        String addMemberQuery = """
                mutation {
                  addMember(membershipDTO: { groupId: "%s", userEmail: "%s" }) {
                    id
                    userId
                  }
                }
                """.formatted(groupId, memberEmail);

        MvcResult addMemberResult = graphql(ownerToken, addMemberQuery);
        String memberId = objectMapper.readTree(addMemberResult.getResponse().getContentAsString())
                .path("data").path("addMember").path("userId").asText();

        String createDebtQuery = """
                mutation {
                  createDebt(debtDTO: {
                    groupId: "%s"
                    debtorId: "%s"
                    creditorId: "%s"
                    amount: 150.0
                    title: "Obiad w restauracji"
                  }) {
                    id
                    amount
                    title
                  }
                }
                """.formatted(groupId, memberId, ownerId);

        MvcResult createDebtResult = graphql(ownerToken, createDebtQuery);

        jsonPath("$.data.createDebt.amount").value(150.0).match(createDebtResult);
        jsonPath("$.data.createDebt.title").value("Obiad w restauracji").match(createDebtResult);

        String groupDebtsQuery = """
                query {
                  groupDebts(groupId: "%s") {
                    id
                    amount
                    title
                  }
                }
                """.formatted(groupId);

        MvcResult debtsBeforeDelete = graphql(ownerToken, groupDebtsQuery);
        jsonPath("$.data.groupDebts.length()").value(1).match(debtsBeforeDelete);
        assertEquals(1, debtRepository.findByGroupId(groupIdLong).size());

        String deleteGroupQuery = """
                mutation {
                  deleteGroup(id: "%s")
                }
                """.formatted(groupId);

        MvcResult deleteResult = graphql(ownerToken, deleteGroupQuery);
        jsonPath("$.data.deleteGroup").value(true).match(deleteResult);

        assertFalse(groupRepository.findById(groupIdLong).isPresent());
        assertTrue(debtRepository.findByGroupId(groupIdLong).isEmpty());
        assertTrue(membershipRepository.findByGroupId(groupIdLong).isEmpty());

        String myGroupsQuery = """
                query {
                  myGroups {
                    id
                  }
                }
                """;

        MvcResult myGroupsResult = graphql(ownerToken, myGroupsQuery);
        jsonPath("$.data.myGroups.length()").value(0).match(myGroupsResult);

        MvcResult memberGroupsResult = graphql(memberToken, myGroupsQuery);
        jsonPath("$.data.myGroups.length()").value(0).match(memberGroupsResult);
    }
}

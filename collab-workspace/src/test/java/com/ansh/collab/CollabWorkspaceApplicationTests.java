package com.ansh.collab;

import com.ansh.collab.model.Document;
import com.ansh.collab.model.DocumentVersion;
import com.ansh.collab.repository.DocumentRepository;
import com.ansh.collab.repository.DocumentVersionRepository;
import com.ansh.collab.repository.UserRepository;
import com.ansh.collab.service.UserService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * SEPM Practical – JUnit Test Cases
 * Project  : Collab Workspace (Real-Time Collaborative Document Editor)
 * Stack    : Spring Boot + WebSocket + MySQL
 * Tool     : JUnit 5 + Spring MockMvc
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CollabWorkspaceTests {

	@Autowired MockMvc mockMvc;
	@Autowired UserService userService;
	@Autowired UserRepository userRepo;
	@Autowired DocumentRepository docRepo;
	@Autowired DocumentVersionRepository versionRepo;

	// ── Cleanup before each test run ─────────────────────────────────────────
	@BeforeEach
	void cleanUp() {
		versionRepo.deleteAll();
		docRepo.deleteAll();
		userRepo.deleteAll();
	}

	// ════════════════════════════════════════════════════════════════════════
	// TC-01  Register with valid credentials
	// ════════════════════════════════════════════════════════════════════════
	@Test @Order(1)
	@DisplayName("TC-01 | Register – valid username and password")
	void tc01_registerValidUser() throws Exception {
		mockMvc.perform(post("/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"username\":\"ansh\",\"password\":\"1234\"}")
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(content().string("Registered successfully"));

		assertTrue(userRepo.existsByUsername("ansh"),
				"User 'ansh' should exist in the database after registration");
	}

	// ════════════════════════════════════════════════════════════════════════
	// TC-02  Register with duplicate username
	// ════════════════════════════════════════════════════════════════════════
	@Test @Order(2)
	@DisplayName("TC-02 | Register – duplicate username rejected")
	void tc02_registerDuplicateUser() throws Exception {
		userService.register("ansh", "1234");

		mockMvc.perform(post("/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"username\":\"ansh\",\"password\":\"abcd\"}")
						.with(csrf()))
				.andExpect(status().isBadRequest())
				.andExpect(content().string("Username already taken"));
	}

	// ════════════════════════════════════════════════════════════════════════
	// TC-03  Register with empty password
	// ════════════════════════════════════════════════════════════════════════
	@Test @Order(3)
	@DisplayName("TC-03 | Register – empty password rejected")
	void tc03_registerEmptyPassword() throws Exception {
		mockMvc.perform(post("/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"username\":\"ansh\",\"password\":\"\"}")
						.with(csrf()))
				.andExpect(status().isBadRequest())
				.andExpect(content().string("Username and password are required"));
	}

	// ════════════════════════════════════════════════════════════════════════
	// TC-04  Register with password shorter than 4 characters
	// ════════════════════════════════════════════════════════════════════════
	@Test @Order(4)
	@DisplayName("TC-04 | Register – short password rejected")
	void tc04_registerShortPassword() throws Exception {
		mockMvc.perform(post("/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"username\":\"test\",\"password\":\"ab\"}")
						.with(csrf()))
				.andExpect(status().isBadRequest())
				.andExpect(content().string("Password must be at least 4 characters"));
	}

	// ════════════════════════════════════════════════════════════════════════
	// TC-05  Unauthenticated access to index.html redirects to login
	// ════════════════════════════════════════════════════════════════════════
	@Test @Order(5)
	@DisplayName("TC-05 | Security – unauthenticated access redirects to login")
	void tc05_unauthenticatedRedirectsToLogin() throws Exception {
		mockMvc.perform(get("/index.html"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrlPattern("**/login.html"));
	}

	// ════════════════════════════════════════════════════════════════════════
	// TC-06  /auth/me returns logged-in username
	// ════════════════════════════════════════════════════════════════════════
	@Test @Order(6)
	@DisplayName("TC-06 | Auth – /auth/me returns current username")
	@WithMockUser(username = "ansh")
	void tc06_authMeReturnsUsername() throws Exception {
		mockMvc.perform(get("/auth/me"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("ansh"));
	}

	// ════════════════════════════════════════════════════════════════════════
	// TC-07  Create a document — createdBy set to logged-in user
	// ════════════════════════════════════════════════════════════════════════
	@Test @Order(7)
	@DisplayName("TC-07 | Documents – create sets createdBy to logged-in user")
	@WithMockUser(username = "ansh")
	void tc07_createDocumentSetsOwner() throws Exception {
		mockMvc.perform(post("/documents")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"Meeting Notes\",\"content\":\"\"}")
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Meeting Notes"))
				.andExpect(jsonPath("$.createdBy").value("ansh"));
	}

	// ════════════════════════════════════════════════════════════════════════
	// TC-08  Get all documents
	// ════════════════════════════════════════════════════════════════════════
	@Test @Order(8)
	@DisplayName("TC-08 | Documents – GET /documents returns all documents")
	@WithMockUser(username = "ansh")
	void tc08_getAllDocuments() throws Exception {
		Document doc = new Document();
		doc.setTitle("Test Doc");
		doc.setContent("Hello");
		doc.setCreatedBy("ansh");
		docRepo.save(doc);

		mockMvc.perform(get("/documents"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].title").value("Test Doc"));
	}

	// ════════════════════════════════════════════════════════════════════════
	// TC-09  Owner can delete their own document
	// ════════════════════════════════════════════════════════════════════════
	@Test @Order(9)
	@DisplayName("TC-09 | Documents – owner can delete their document")
	@WithMockUser(username = "ansh")
	void tc09_ownerCanDeleteDocument() throws Exception {
		Document doc = new Document();
		doc.setTitle("To Delete");
		doc.setContent("");
		doc.setCreatedBy("ansh");
		doc = docRepo.save(doc);

		mockMvc.perform(delete("/documents/" + doc.getId()).with(csrf()))
				.andExpect(status().isOk())
				.andExpect(content().string("Document deleted successfully"));

		assertFalse(docRepo.existsById(doc.getId()),
				"Document should no longer exist in DB");
	}

	// ════════════════════════════════════════════════════════════════════════
	// TC-10  Non-owner cannot delete someone else's document (403)
	// ════════════════════════════════════════════════════════════════════════
	@Test @Order(10)
	@DisplayName("TC-10 | Security – non-owner gets 403 on delete")
	@WithMockUser(username = "raj")
	void tc10_nonOwnerCannotDeleteDocument() throws Exception {
		Document doc = new Document();
		doc.setTitle("Ansh's Doc");
		doc.setContent("");
		doc.setCreatedBy("ansh");   // owned by ansh, but raj is logged in
		doc = docRepo.save(doc);

		mockMvc.perform(delete("/documents/" + doc.getId()).with(csrf()))
				.andExpect(status().isForbidden());

		assertTrue(docRepo.existsById(doc.getId()),
				"Document should still exist — non-owner delete was blocked");
	}

	// ════════════════════════════════════════════════════════════════════════
	// TC-11  Update document title and content
	// ════════════════════════════════════════════════════════════════════════
	@Test @Order(11)
	@DisplayName("TC-11 | Documents – update title and content")
	@WithMockUser(username = "ansh")
	void tc11_updateDocument() throws Exception {
		Document doc = new Document();
		doc.setTitle("Old Title");
		doc.setContent("Old content");
		doc.setCreatedBy("ansh");
		doc = docRepo.save(doc);

		mockMvc.perform(put("/documents/" + doc.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"New Title\",\"content\":\"New content\"}")
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("New Title"))
				.andExpect(jsonPath("$.content").value("New content"));
	}

	// ════════════════════════════════════════════════════════════════════════
	// TC-12  Get versions for a document
	// ════════════════════════════════════════════════════════════════════════
	@Test @Order(12)
	@DisplayName("TC-12 | Version History – versions retrieved for document")
	@WithMockUser(username = "ansh")
	void tc12_getVersions() throws Exception {
		Document doc = new Document();
		doc.setTitle("Versioned Doc");
		doc.setContent("v1 content");
		doc.setCreatedBy("ansh");
		doc = docRepo.save(doc);

		DocumentVersion v = new DocumentVersion();
		v.setDocumentId(doc.getId());
		v.setContent("v1 content");
		v.setUsername("ansh");
		v.setTimestamp(java.time.LocalDateTime.now());
		versionRepo.save(v);

		mockMvc.perform(get("/documents/" + doc.getId() + "/versions"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].username").value("ansh"))
				.andExpect(jsonPath("$[0].content").value("v1 content"));
	}

	// ════════════════════════════════════════════════════════════════════════
	// TC-13  Restore a previous version
	// ════════════════════════════════════════════════════════════════════════
	@Test @Order(13)
	@DisplayName("TC-13 | Version History – restore reverts document content")
	@WithMockUser(username = "ansh")
	void tc13_restoreVersion() throws Exception {
		Document doc = new Document();
		doc.setTitle("Doc");
		doc.setContent("current content");
		doc.setCreatedBy("ansh");
		doc = docRepo.save(doc);

		DocumentVersion v = new DocumentVersion();
		v.setDocumentId(doc.getId());
		v.setContent("old content");
		v.setUsername("ansh");
		v.setTimestamp(java.time.LocalDateTime.now());
		v = versionRepo.save(v);

		mockMvc.perform(put("/documents/" + doc.getId() + "/restore/" + v.getId())
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").value("old content"));
	}

	// ════════════════════════════════════════════════════════════════════════
	// TC-14  UserService – password is BCrypt hashed, not stored in plain text
	// ════════════════════════════════════════════════════════════════════════
	@Test @Order(14)
	@DisplayName("TC-14 | UserService – password stored as BCrypt hash")
	void tc14_passwordIsHashed() {
		userService.register("hashtest", "mypassword");
		String stored = userRepo.findByUsername("hashtest")
				.orElseThrow().getPassword();

		assertNotEquals("mypassword", stored,
				"Password must not be stored in plain text");
		assertTrue(stored.startsWith("$2a$") || stored.startsWith("$2b$"),
				"Password should be a BCrypt hash");
	}

	// ════════════════════════════════════════════════════════════════════════
	// TC-15  Document has correct fields after creation
	// ════════════════════════════════════════════════════════════════════════
	@Test @Order(15)
	@DisplayName("TC-15 | Documents – all fields persisted correctly")
	void tc15_documentFieldsPersisted() {
		Document doc = new Document();
		doc.setTitle("My Doc");
		doc.setContent("Some content here");
		doc.setCreatedBy("ansh");
		Document saved = docRepo.save(doc);

		assertNotNull(saved.getId(),       "ID should be auto-generated");
		assertEquals("My Doc",            saved.getTitle());
		assertEquals("Some content here", saved.getContent());
		assertEquals("ansh",             saved.getCreatedBy());
	}

	// ════════════════════════════════════════════════════════════════════════
	// TC-16  Version list is empty for a brand-new document
	// ════════════════════════════════════════════════════════════════════════
	@Test @Order(16)
	@DisplayName("TC-16 | Version History – new document has no versions")
	@WithMockUser(username = "ansh")
	void tc16_newDocumentHasNoVersions() throws Exception {
		Document doc = new Document();
		doc.setTitle("Fresh Doc");
		doc.setContent("");
		doc.setCreatedBy("ansh");
		doc = docRepo.save(doc);

		mockMvc.perform(get("/documents/" + doc.getId() + "/versions"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isEmpty());
	}

	// ════════════════════════════════════════════════════════════════════════
	// TC-17  /auth/me returns 401 when not authenticated
	// ════════════════════════════════════════════════════════════════════════
	@Test @Order(17)
	@DisplayName("TC-17 | Auth – /auth/me returns 401 when not logged in")
	void tc17_authMeUnauthorized() throws Exception {
		mockMvc.perform(get("/auth/me"))
				.andExpect(status().is3xxRedirection()); // redirected to login
	}

	// ════════════════════════════════════════════════════════════════════════
	// TC-18  Multiple versions saved correctly for a document
	// ════════════════════════════════════════════════════════════════════════
	@Test @Order(18)
	@DisplayName("TC-18 | Version History – multiple versions stored correctly")
	@WithMockUser(username = "ansh")
	void tc18_multipleVersions() throws Exception {
		Document doc = new Document();
		doc.setTitle("Multi-version Doc");
		doc.setContent("v3");
		doc.setCreatedBy("ansh");
		doc = docRepo.save(doc);

		for (int i = 1; i <= 3; i++) {
			DocumentVersion v = new DocumentVersion();
			v.setDocumentId(doc.getId());
			v.setContent("v" + i);
			v.setUsername("ansh");
			v.setTimestamp(java.time.LocalDateTime.now());
			versionRepo.save(v);
		}

		List<DocumentVersion> versions = versionRepo.findByDocumentId(doc.getId());
		assertEquals(3, versions.size(), "Should have exactly 3 versions");
	}

	// ════════════════════════════════════════════════════════════════════════
	// TC-19  UserService throws on duplicate registration
	// ════════════════════════════════════════════════════════════════════════
	@Test @Order(19)
	@DisplayName("TC-19 | UserService – throws RuntimeException on duplicate")
	void tc19_userServiceThrowsOnDuplicate() {
		userService.register("dupuser", "pass1234");
		assertThrows(RuntimeException.class,
				() -> userService.register("dupuser", "otherpass"),
				"Should throw RuntimeException when username is already taken");
	}

	// ════════════════════════════════════════════════════════════════════════
	// TC-20  GET /documents returns empty list when no documents exist
	// ════════════════════════════════════════════════════════════════════════
	@Test @Order(20)
	@DisplayName("TC-20 | Documents – empty list when no documents exist")
	@WithMockUser(username = "ansh")
	void tc20_emptyDocumentList() throws Exception {
		mockMvc.perform(get("/documents"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isEmpty());
	}
}
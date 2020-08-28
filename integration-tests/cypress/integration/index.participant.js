describe("Participant course view", () => {
	beforeEach(() => {
		cy.visit("/?debugChangeLogin=participant")
	})
	
	it("shows course data", () => {
		cy.get("main")
			.removeNondeterminism()
			.snapshot()
	})

	it("logs us out", () => {
		cy.contains("Log out")
			.click()
		cy.contains("Access Token")
	})

	it("submits a test request", () => {
		cy.contains("Request test")
			.click()
		cy.get("main")
			.removeNondeterminism()
			.snapshot("index.participant__after-request")
		cy.visit("/?debugChangeLogin=admin")
		cy.reload()
		cy.get("#test-requests")
			.contains("Demo PARTICIPANT on Complexity in Debug Course 2020/21")
	})

	it("shows results and grades", () => {
		cy.visit("/?debugChangeLogin=admin")
		cy.visit("/courses/59f29bcb-978a-48ed-a0ac-7d8425920cdf/grading")
		cy.get("input[name=adc6d249-bc3d-4f9d-8421-5440e0d1e991-score-e6c21a35-b654-4218-9466-482fea2157fa]")
			.clear()
			.type("3")
			.closest("form")
			.submit()
		cy.visit("/?debugChangeLogin=participant")
		cy.get("main")
			.removeNondeterminism()
			.snapshot()
	})

	it("shows the current session", () => {
		cy.get("#sessions")
			.contains("<- current")
	})

	it("revokes non-current sessions", () => {
		cy.clearCookies()
		cy.visit("/?debugChangeLogin=admin")
		cy.get("#sessions li:not(.current)")
			.first()
			.as("revokedLI")
		cy.get("@revokedLI")
			.find("strong [style]")
			.then(li => {
				const sessionIdToRemove = li.text()
				cy.contains(sessionIdToRemove)
				cy.get("@revokedLI")
					.contains("Revoke")
					.click()
				cy.get("#sessions li")
					.each(li => {
						const thisSessionId = li.find("strong [style]")
							.text()
						expect(thisSessionId).to.not.include(sessionIdToRemove)
					})
			})
	})

	it("revokes current sessions", () => {
		cy.clearCookies()
		cy.visit("/?debugChangeLogin=admin")
		cy.get("#sessions li.current")
			.first()
			.as("revokedLI")
		cy.get("@revokedLI")
			.find("strong [style]")
			.then(li => {
				const sessionIdToRemove = li.text()
				cy.contains(sessionIdToRemove)
				cy.get("@revokedLI")
					.contains("Revoke")
					.click()
				cy.url().should("eq", Cypress.config().baseUrl + "/")
				cy.get("#accessToken")
					.type("454e7007a3ed86b9639794453546977dcf8e63c8720d169d{enter}")
				cy.get("#sessions li")
					.each(li => {
						const thisSessionId = li.find("strong [style]")
							.text()
						expect(thisSessionId).to.not.include(sessionIdToRemove)
					})
			})
	})
})

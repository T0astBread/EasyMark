describe("Admin index page", () => {
	beforeEach(() => {
		cy.visit("/?debugChangeLogin=admin")
	})

	it("deletes test requests", () => {
		cy.visit("/?debugChangeLogin=participant")
		cy.contains("Request test")
			.click()
		cy.visit("/?debugChangeLogin=admin")
		cy.reload()
		cy.get("#test-requests")
			.contains("Demo PARTICIPANT on Complexity in Debug Course 2020/21")
		cy.get("#test-requests .remove-button")
			.click()
		cy.get("#test-requests")
			.snapshot()
		cy.visit("/?debugChangeLogin=participant")
		cy.get("main")
			.removeNondeterminism()
			.snapshot()
	})

	it("links to courses", () => {
		cy.get("#courses")
			.contains("Debug Course 2020/21")
			.click()
		cy.url().should("eq", Cypress.config().baseUrl + "/courses/59f29bcb-978a-48ed-a0ac-7d8425920cdf")
	})

	it("creates courses", () => {
		// Snapshot testing isn't that great here since everything is
		// full of random UUIDs (same in some other tests)
		cy.get("#courseName")
			.type("New Course #1{enter}")
		cy.get("#courses ul")
			.contains("New Course #1")
		cy.get("#courseName")
			.type("New Course #2")
		cy.contains("Add course")
			.click()
		cy.get("#courses ul")
			.contains("New Course #2")
	})

	it("reorders courses", () => {
		cy.get("#courseName")
			.type("New Course #1{enter}")
		cy.get("#courses ul")
			.contains("New Course #1")
			.next()
			.next()
			.click()
		cy.get("#courses ul")
			.contains("New Course #1")
			.parentsUntil("ul")
			.next()
			.contains("Debug Course 2020/21")
	})

	it("deletes courses", () => {
		cy.contains("Debug Course 2020/21")
			.next()
			.should("match", "a")
			.click()
		cy.get("main")
			.removeNondeterminism()
			.snapshot()
		cy.reload()
		cy.contains("Yes, delete")
			.click()
		cy.get("#courses")
			.removeNondeterminism()
			.snapshot()
	})

	it("logs us out", () => {
		cy.contains("Log out")
			.click()
		cy.contains("Access Token")
	})

	it("links to settings", () => {
		cy.contains("Settings")
			.click()
		cy.url().should("eq", Cypress.config().baseUrl + "/settings")
	})
})

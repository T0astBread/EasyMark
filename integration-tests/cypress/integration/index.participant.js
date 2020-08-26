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
})

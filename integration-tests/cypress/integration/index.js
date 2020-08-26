describe("Login page", () => {
	it("appears as expected", () => {
		cy.visit("/")
		cy.get("main")
			.removeNondeterminism()
			.snapshot()
	})

	it("logs us in as a participant", () => {
		cy.visit("/")
		cy.get("#accessToken")
			.type("454e7007a3ed86b9639794453546977dcf8e63c8720d169d{enter}")  // see Utils.java
		cy.contains("Reached 0 out of 0 points")
	})
})

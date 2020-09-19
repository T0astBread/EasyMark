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
		cy.contains("Reached 0.0 out of 0.0 points")
	})

	it("logs admin logins to the activity log", () => {
		cy.visit("/")
		cy.get("#accessToken")
			.type("977d1f28171b17a71b25f69df08a690224ecf8c637d5e2a8{enter}")  // see Utils.java
		cy.get("#activity-log")
			.contains("Logged in")
			.closest("li")
			.then(li => expect(li.text()).to.include("Logged in; IP Address: 127.0.0.1, Session: "))
	})
})

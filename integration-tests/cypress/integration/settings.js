describe("Settings page", () => {
	beforeEach(() => {
		cy.visit("/settings?debugChangeLogin=admin")
	})

	it("appears as expected", () => {
		cy.get("main")
			.removeNondeterminism()
			.snapshot()
	})

	it("exports encrypted data", () => {
		cy.request("/encrypted-data")
			.then(res => {
				const date = new Date()
				const year = (date.getYear() + 1900).toString().padStart(2, "0")
				const month = (date.getMonth() + 1).toString().padStart(2, "0")
				const day = date.getDate().toString().padStart(2, "0")
				expect(res.headers["content-disposition"]).to.equal(`attachment; filename="easymark_${year}-${month}-${day}_613198a7-eb79-468c-88a1-77fcc75b51dc.csv\"`)
				expect(res.headers["content-type"]).to.equal("text/csv")
			})
	})

	it("resets our access token, preserving our data", () => {
		cy.contains("Reset access token")
			.click()
		cy.get("main")
			.removeNondeterminism()
			.snapshot()
		cy.reload()
		cy.get("#oldAccessToken")
			.type("977d1f28171b17a71b25f69df08a690224ecf8c637d5e2a8")
		cy.contains("Reset access token")
			.click()
		cy.get("#cat").then(accessTokenElem => {
			const accessToken = accessTokenElem.text()
			expect(accessToken).to.match(/[0-9a-f]{48}/)
			
			accessTokenElem.text("f00ba5")  // replace with deterministic text
			cy.get("main")
				.snapshot()

			cy.contains("Continue")
				.click()
			cy.contains("Access Token")

			cy.get("#accessToken")
				.type(accessToken + "{enter}")
			cy.get("body")
				.removeNondeterminism()
				.snapshot()

			cy.visit("/courses/59f29bcb-978a-48ed-a0ac-7d8425920cdf/grading")
			cy.get("body")
				.removeNondeterminism()
				.snapshot()
		})
	})


	it("resets our access token, losing our data, then restores some of the data", () => {
		cy.contains("Reset access token")
			.click()
		cy.get("main")
			.removeNondeterminism()
			.snapshot()
		cy.reload()
		cy.contains("Reset access token")
			.click()
		cy.get("#cat").then(catElem => {
			const cat = catElem.text()
			expect(cat).to.match(/[0-9a-f]{48}/)
			
			catElem.text("f00ba5")  // replace with deterministic text
			cy.get("main")
				.snapshot()

			cy.contains("Continue")
				.click()
			cy.contains("Access Token")

			cy.get("#accessToken")
				.type(cat + "{enter}")
			cy.get("body")
				.removeNondeterminism()
				.snapshot()

			cy.visit("/courses/59f29bcb-978a-48ed-a0ac-7d8425920cdf/grading")
			cy.get("body")
				.removeNondeterminism()
				.snapshot()

			cy.visit("/settings")
			cy.get("textarea[name=data]")
				.type("a93fe134-1bd1-4135-acab-e501d8e160ca;Paul BRITTLES{enter}adc6d249-bc3d-4f9d-8421-5440e0d1e991;Participant RESTORED{enter}")
			cy.contains("Import")
				.click()
				
			cy.visit("/courses/59f29bcb-978a-48ed-a0ac-7d8425920cdf/grading")
			cy.get("body")
				.removeNondeterminism()
				.snapshot()
		})
	})


	it("creates a new admin", () => {
		let id, accessToken
		cy.contains("Create new admin")
			.click()
		cy.contains("New access token for")
			.children("strong")
			.then(idElem => {
				id = idElem.text()
				id = id.substr(0, id.length - 1)
				expect(id).to.match(/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/)
				return cy.get("#cat")
			})
			.then(accessTokenElem => {
				accessToken = accessTokenElem.text()
				expect(accessToken).to.match(/[0-9a-f]{48}/)

				cy.contains("Continue")
					.click()
					
				cy.contains("<- YOU")
					.closest("li")
					.next()
					.contains(id)
					.closest("li")
					.contains("owns")
					.closest("li")
					.contains("no courses")
					.closest("li")
					.contains("Reset access token")
					.closest("li")
					.contains("Delete")
					
				cy.contains("Log out")
					.click()

				cy.get("#accessToken")
					.type(accessToken)
					.type("{enter}")
					
				cy.get("#courseName")
					.type("Other Admin's Course{enter}")

				cy.visit("/settings")
				
				cy.contains("<- YOU")
					.closest("li")
					.contains(id)
					.closest("li")
					.contains("Other Admin's Course")

				cy.contains("Log out")
					.click()

				cy.get("#accessToken")
					.type("977d1f28171b17a71b25f69df08a690224ecf8c637d5e2a8{enter}")

				// to make sure we don't show courses that don't belong to an admin
				cy.get("body")
					.removeNondeterminism()
					.snapshot()
			})
	})


	it("deletes an admin", () => {
		let id, accessToken
		cy.contains("Create new admin")
			.click()
		cy.contains("New access token for")
			.children("strong")
			.then(idElem => {
				id = idElem.text()
				id = id.substr(0, id.length - 1)
				expect(id).to.match(/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/)
				return cy.get("#cat")
			})
			.then(accessTokenElem => {
				accessToken = accessTokenElem.text()
				expect(accessToken).to.match(/[0-9a-f]{48}/)

				cy.contains("Continue")
					.click()
					
				cy.contains("Log out")
					.click()
				cy.get("#accessToken")
					.type(accessToken)
					.type("{enter}")
					
				cy.get("#courseName")
					.type("Other Admin's Course{enter}")

				cy.contains("Log out")
					.click()
				cy.get("#accessToken")
					.type("977d1f28171b17a71b25f69df08a690224ecf8c637d5e2a8{enter}")
					
				cy.visit("/settings")

				cy.contains("Other Admin's Course")
					.closest("li")
					.contains("Delete")
					.click()

				cy.contains("Are you sure you want to delete")
				cy.contains("admin " + id + " with all associated courses, participants, chapters, test requests, assignments and assignment results?")
				
				cy.contains("No")
					.click()
				cy.url().should("eq", Cypress.config().baseUrl + "/settings")

				cy.contains("Other Admin's Course")
					.closest("li")
					.contains("Delete")
					.click()
					
				cy.contains("Yes, delete")
					.click()

				cy.contains("Admins")
					.next("ul")
					.removeNondeterminism()
					.snapshot()
					
				cy.contains("Log out")
					.click()
				cy.get("input[name=csrfToken]").then(csrfInput => {
					cy.request({
						method: "POST",
						url: "/login",
						failOnStatusCode: false,
						form: true,
						body: {
							accessToken,
							csrfToken: csrfInput.attr("value"),
						},
					}).then(res => {
						expect(res.status).to.eq(403)
						expect(res.body).to.include("Forbidden")
					})
				})
			})
	})

	it("logs admin creations", () => {
		cy.contains("Create new admin")
			.click()
		cy.visit("/")
		cy.get("#activity-log li")
			.then(items => {
				expect(items.length).to.equal(1)
				return cy.get("#sessions .current [style]")
			})
			.then(currentSessionElem => {
				cy.get("#activity-log li [style]")
					.then(logIdElem => {
						const sessionId = currentSessionElem.text()
						expect(logIdElem.text()).to.equal(sessionId)
					})
			})
	})

	it("logs admin AT resets", () => {
		let adminId
		cy.contains("Create new admin")
			.click()
		cy.contains("Continue")
			.click()
		cy.contains("no courses")
			.closest("li")
			.then(li => {
				adminId = li.find("> :first-child > *").text()
				cy.wrap(li)
					.contains("Reset access token")
					.click()
				cy.contains("Reset access token")
					.click()
				cy.contains("Continue")
					.click()
				return cy.get("#sessions .current [style]")
			})
			.then(currentSessionElem => {
				cy.get("#activity-log li [style]")
					.each(logIdElem => {
						const sessionId = currentSessionElem.text()
						expect(logIdElem.text()).to.equal(sessionId)
					})
				cy.get("#activity-log")
					.contains("Admin created:")
					.closest("li")
					.contains(adminId)
				cy.get("#activity-log")
					.contains("Reset access token of admin")
					.closest("li")
					.contains(adminId)
			})
	})
})

describe("Grading editor", () => {
	beforeEach(() => {
		cy.visit("/?debugChangeLogin=admin")
		cy.visit("/courses/59f29bcb-978a-48ed-a0ac-7d8425920cdf/grading")
	})

	it("appears as expected", () => {
		cy.get("body")
			.removeNondeterminism()
			.snapshot()
	})

	it("focuses inputs on Ctrl+M", () => {
		cy.get("input[name=adc6d249-bc3d-4f9d-8421-5440e0d1e991-score-e6c21a35-b654-4218-9466-482fea2157fa]")
			.type("{ctrl}m")
			.should(input => {
				const { top, left } = input[0].getBoundingClientRect()
				expect(top - 287).to.be.lessThan(15)
				expect(left - 174).to.be.lessThan(15)
			})
	})

	it("focuses inputs on Ctrl+Arrows", () => {
		cy.get("input[name=adc6d249-bc3d-4f9d-8421-5440e0d1e991-score-e6c21a35-b654-4218-9466-482fea2157fa]")
			.type("{ctrl}{rightArrow}{ctrl}{rightArrow}{ctrl}{rightArrow}")
		cy.focused()
			.should("have.attr", "name", "adc6d249-bc3d-4f9d-8421-5440e0d1e991-score-6244a9b2-97aa-4b7b-864c-fc8ccb4b8766")
			.should(input => {
				const { top, left } = input[0].getBoundingClientRect()
				expect(top - 287).to.be.lessThan(15)
				expect(left - 174).to.be.lessThan(15)
			})
			.type("{ctrl}{leftArrow}{ctrl}{leftArrow}{ctrl}{leftArrow}{ctrl}{leftArrow}")
		cy.focused()
			.should("have.attr", "name", "adc6d249-bc3d-4f9d-8421-5440e0d1e991-notes")
			.should(input => {
				const { top, left } = input[0].getBoundingClientRect()
				expect(top - 287).to.be.lessThan(15)
				expect(left - 473).to.be.lessThan(15)
			})
	})

	it("stores inputs", () => {
		cy.get("input[name=adc6d249-bc3d-4f9d-8421-5440e0d1e991-notes]")
			.clear()
			.type("Warning is out")
			.type("{ctrl}{rightArrow}")
		cy.focused()
			.should("have.attr", "name", "adc6d249-bc3d-4f9d-8421-5440e0d1e991-score-e6c21a35-b654-4218-9466-482fea2157fa")
			.clear()
			.type("3")
		cy.contains("Save")
			.click()
		cy.get('input[value="Warning is out"]')
			.should("have.attr", "name", "adc6d249-bc3d-4f9d-8421-5440e0d1e991-notes")
		cy.reload()
		cy.get('input[value="Warning is out"]')
			.should("have.attr", "name", "adc6d249-bc3d-4f9d-8421-5440e0d1e991-notes")
	})

	it("calculates grades", () => {
		cy.get("input[name=adc6d249-bc3d-4f9d-8421-5440e0d1e991-score-e6c21a35-b654-4218-9466-482fea2157fa]")
			.clear()
			.type("3")
			.closest("form")
			.submit()
		cy.get("body")
			.removeNondeterminism()
			.snapshot()
	})

	it("deletes participants", () => {
		cy.get('a[href="/participants/adc6d249-bc3d-4f9d-8421-5440e0d1e991/confirm-delete"]')
			.click()
		cy.get("body")
			.removeNondeterminism()
			.snapshot()
		cy.reload()
		cy.contains("Yes, delete")
			.click()
		cy.get("body")
			.removeNondeterminism()
			.snapshot()
		cy.reload()
		cy.contains("Log out")
			.click()
		cy.get("input[name=csrfToken]").then(csrfInput => {
			cy.request({
				method: "POST",
				url: "/login",
				failOnStatusCode: false,
				form: true,
				body: {
					accessToken: "454e7007a3ed86b9639794453546977dcf8e63c8720d169d",
					csrfToken: csrfInput.attr("value"),
				},
			}).then(res => {
				expect(res.status).to.eq(403)
				expect(res.body).to.eq("Forbidden")
			})
		})
	})
})

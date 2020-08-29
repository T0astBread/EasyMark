/*
Call this command on an element to remove nondeterministic contents.
Useful for snapshot testing.

Example:
```
cy.get("main")
	.removeNondeterminism()
	.snapshot()
```
*/
Cypress.Commands.add("removeNondeterminism", {
	prevSubject: "element",
}, subject => {
	subject.find("input[name=csrfToken]")
		.attr("value", "")
	subject.find(".session-list")
		.html("")
	subject.find("#activity-log ul")
		.html("")
	return subject
})

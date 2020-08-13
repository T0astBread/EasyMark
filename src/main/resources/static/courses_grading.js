const tableElem = document.querySelector(".table-wrapper")
const participantHeaderElem = document.getElementById("participant-header")

const scrollIntoView = elem => {
    const { offsetTop, offsetLeft } = elem.parentElement
    const left = elem.classList.contains("score-input")
        ? offsetLeft - participantHeaderElem.clientWidth
        : 0
    tableElem.scrollTo({
        top: offsetTop - tableElem.clientHeight * .5,
        left,
        behavior: 'smooth'
    })
}

const handleKeyPress = evt => {
    const elem = evt.currentTarget

    const doThing = (right, up) => {
        const x = parseInt(elem.getAttribute("data-position-x"))
        const y = parseInt(elem.getAttribute("data-position-y"))
        if (x == null || y == null)
            return

        const selector = `input[data-position-x="${x+right}"][data-position-y="${y-up}"], button[data-position-x="${x+right}"][data-position-y="${y-up}"]`
        const newFocused = document.querySelector(selector)
        if (newFocused != null) {
            newFocused.focus()
        }

        evt.preventDefault()
    }

    if (!evt.shiftKey) {
        switch (evt.key) {
            case "ArrowUp":
                doThing(0, 1)
                break
            case "ArrowDown":
                doThing(0, -1)
                break
            case "ArrowLeft":
                doThing(-1, 0)
                break
            case "ArrowRight":
                doThing(1, 0)
                break
        }
    }
}

document.querySelectorAll("table input, table button").forEach(elem => {
    elem.addEventListener("keydown", handleKeyPress)
    elem.addEventListener("focus", evt => scrollIntoView(evt.currentTarget))
})

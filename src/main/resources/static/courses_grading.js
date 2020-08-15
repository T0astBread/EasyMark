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

const moveFocus = (elem, right, up) => {
    const x = parseInt(elem.getAttribute("data-position-x"))
    const y = parseInt(elem.getAttribute("data-position-y"))
    if (x == null || y == null)
        return

    const selector = `input[data-position-x="${x+right}"][data-position-y="${y-up}"], button[data-position-x="${x+right}"][data-position-y="${y-up}"]`
    const newFocused = document.querySelector(selector)
    if (newFocused != null) {
        newFocused.focus()
        scrollIntoView(newFocused)
    }
}

const handleKeyPress = evt => {
    const elem = evt.currentTarget

    if (evt.ctrlKey && elem.tagName === "INPUT" || elem.tagName === "BUTTON") {
        switch (evt.key) {
            case "ArrowUp":
                moveFocus(elem, 0, 1)
                break
            case "ArrowDown":
                moveFocus(elem, 0, -1)
                break
            case "ArrowLeft":
                moveFocus(elem, -1, 0)
                break
            case "ArrowRight":
                moveFocus(elem, 1, 0)
                break
            case "m":
                scrollIntoView(elem)
                break
            default:
                return
        }
        evt.preventDefault()
    }
}

document.querySelectorAll("table input, table button").forEach(elem => {
    elem.addEventListener("keydown", handleKeyPress)
})

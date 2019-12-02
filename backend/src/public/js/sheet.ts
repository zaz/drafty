const activeClass = "active";
let activeTableCellElement: null | HTMLTableCellElement = null;
/* activated when associated head is clicked */
let activeTableColElement: null | HTMLTableColElement = null;
const copiedClass = "copied";
let lastCopiedTableCellElement: null | HTMLTableCellElement | HTMLTableColElement = null;

const tableElement: HTMLTableElement = document.getElementById("sheet") as HTMLTableElement;
const tableRowElements: HTMLCollection = tableElement.rows;
const tableColElements: HTMLCollection = tableElement.getElementsByTagName("col");

// platform
function isMac() {
  const platform = window.navigator.platform;
  return platform.includes("Mac");
}
/* handle mac shortcut differently */
const onMac: boolean = isMac();

/* differentiate table element */
function isTableData(element: HTMLElement): boolean {
  return element.tagName === "TD";
}
function isTableHead(element: HTMLElement): boolean {
  return element.tagName === "TH";
}
function isTableCell(element: HTMLElement): boolean {
  const tagName = element.tagName;
  return tagName === "TD" || tagName === "TH";
}

function getTableColElement(index: number): HTMLTableColElement | undefined {
  return tableColElements[index] as HTMLTableColElement;
}
function* getTableColElements(index: number) {
  for (const tableRowElement of tableRowElements) {
    const tableRow = tableRowElement as HTMLTableRowElement;
    yield tableRow.cells[index];
  }
}

/* deactivate */
function deactivateTableData() {
  activeTableCellElement.classList.remove(activeClass);
}
function deactivateTableHead() {
  activeTableCellElement.classList.remove(activeClass);
}
function deactivateTableCol() {
  if (activeTableColElement) {
    activeTableColElement.classList.remove(activeClass);
    activeTableColElement = null;
  }
}
function deactivateTableCellElement() {
  if (isTableData(activeTableCellElement)) {
    deactivateTableData();
  } else if (isTableHead(activeTableCellElement)) {
    deactivateTableHead();
    deactivateTableCol();
  }
  activeTableCellElement = null;
}

/* activate */
function activateTableData() {
  activeTableCellElement.classList.add(activeClass);
  activeTableCellElement.focus();
}
function activateTableHead() {
  activeTableCellElement.classList.add(activeClass);
  activeTableCellElement.focus();
}
function activateTableCol() {
  const index = activeTableCellElement.cellIndex;
  const tableColElement = getTableColElement(index);
  if (tableColElement) {
    activeTableColElement = tableColElement;
    activeTableColElement.classList.add(activeClass);
  }
}
function activateTableCellElement(tableCellElement: HTMLTableCellElement) {
  activeTableCellElement = tableCellElement;
  if (isTableData(tableCellElement)) {
    activateTableData();
  } else if (isTableHead(tableCellElement)) {
    activateTableHead();
  }
}
function clickOnActiveElement(tableCellElement: HTMLTableCellElement) {
  return tableCellElement === activeTableCellElement;
}

function updateActiveTableCellElement(tableCellElement: HTMLTableCellElement | null) {
  if (!tableCellElement) {
    return;
  }

  if (activeTableCellElement) {
    deactivateTableCellElement();
  }
  activateTableCellElement(tableCellElement);
}

// navigation
function getTopTableRow(tableRowElement: HTMLTableRowElement): HTMLTableRowElement | null {
  return tableRowElement.previousElementSibling as HTMLTableRowElement;
}
function getDownTableRow(tableRowElement: HTMLTableRowElement): HTMLTableRowElement | null {
  return tableRowElement.nextElementSibling as HTMLTableRowElement;
}

function getCellInTableRow(tableRowElement: HTMLTableRowElement, cellIndex: number): HTMLTableCellElement | null {
  return tableRowElement.cells[cellIndex];
}

function getLeftTableCellElement(tableCellElement: HTMLTableCellElement): HTMLTableCellElement | null {
  return tableCellElement.previousElementSibling as HTMLTableCellElement;
}
function getRightTableCellElement(tableCellElement: HTMLTableCellElement): HTMLTableCellElement | null {
  return tableCellElement.nextElementSibling as HTMLTableCellElement;
}
function getUpTableCellElement(tableCellElement: HTMLTableCellElement): HTMLTableCellElement | null {
  if (!isTableData(tableCellElement)) {
    // ignore up request on table head
    return null;
  }
  const cellIndex = tableCellElement.cellIndex;
  const topTableRow = getTopTableRow(tableCellElement.parentElement as HTMLTableRowElement);
  if (!topTableRow) {
    return null;
  }
  return getCellInTableRow(topTableRow, cellIndex);
}
function getDownTableCellElement(tableCellElement: HTMLTableCellElement): HTMLTableCellElement | null {
  if (!isTableData(tableCellElement)) {
    // ignore up request on table head
    return null;
  }
  const cellIndex = tableCellElement.cellIndex;
  const downTableRow = getDownTableRow(tableCellElement.parentElement as HTMLTableRowElement);
  if (!downTableRow) {
    return null;
  }
  return getCellInTableRow(downTableRow, cellIndex);
}

// events
/* click event */
function activeTableHeadOnRepeatedClick(event: MouseEvent) {
  if (activeTableColElement) {
    // table column is active, deactivate column and focus only on table head
    deactivateTableCol();
  } else {
    // only activate table column at repeated click (after even number of clicks)
    activateTableCol();
  }
}
function activeElementOnRepeatedClick(event: MouseEvent) {
  if (!activeTableCellElement) {
    return;
  }
  if (isTableData(activeTableCellElement)) {
    // TODO
  } else if (isTableHead(activeTableCellElement)) {
    activeTableHeadOnRepeatedClick(event);
  }
}
function tableCellElementOnClick(tableCellElement: HTMLTableCellElement, event: MouseEvent) {
  if (clickOnActiveElement(tableCellElement)) {
    // handle repeated click differently
    activeElementOnRepeatedClick(event);
  } else {
    updateActiveTableCellElement(tableCellElement);
  }
  event.preventDefault();
  event.stopPropagation();
}
tableElement.addEventListener("click", function(event: MouseEvent) {
  const target: HTMLElement = event.target as HTMLElement;
  if (isTableCell(target)) {
    tableCellElementOnClick(target as HTMLTableCellElement, event);
  }
}, true);

/* keyboard event */
/** copy **/
function initializeClipboardTextarea() {
  const textarea = document.createElement("textarea");
  textarea.id = "clipboard-textarea";
  textarea.readOnly = true;
  const bodyElement = document.body;
  bodyElement.appendChild(textarea);
  return textarea;
}
const clipboardTextarea: HTMLTextAreaElement = initializeClipboardTextarea();
function copyTextareaToClipboard() {
  clipboardTextarea.select();
  document.execCommand("copy");
}
function clearClipboardTextarea() {
  clipboardTextarea.value = "";
}
function unhighlightCopiedElement() {
  if (lastCopiedTableCellElement) {
    lastCopiedTableCellElement.classList.remove(copiedClass);
    lastCopiedTableCellElement = null;
  }
}
function highlightCopiedElement(element: HTMLTableCellElement | HTMLTableColElement) {
  lastCopiedTableCellElement = element;
  element.classList.add(copiedClass);
}
function hasCopyModifier(event: KeyboardEvent) {
  if (onMac) {
    return event.metaKey;
  } else {
    return event.ctrlKey;
  }
}
function copyElementTextToTextarea(tableCellElement: HTMLTableCellElement) {
  clipboardTextarea.value = tableCellElement.textContent;
}
function copyTableColumnToTextarea(index: number) {
  for (const tableCellElement of getTableColElements(index)) {
    clipboardTextarea.value += `${tableCellElement.textContent}\n`;
  }
  clipboardTextarea.value = clipboardTextarea.value.trimRight();
}
function tableCellElementOnCopy(tableCellElement: HTMLTableCellElement, event: KeyboardEvent) {
  if (hasCopyModifier(event)) {
    unhighlightCopiedElement();
    clearClipboardTextarea();
    let elementToHighlight;
    if (activeTableColElement) {
      // copy entire column
      copyTableColumnToTextarea(activeTableCellElement.cellIndex);
      elementToHighlight = activeTableColElement;
    } else {
      copyElementTextToTextarea(tableCellElement);
      elementToHighlight = tableCellElement;
    }
    copyTextareaToClipboard();
    highlightCopiedElement(elementToHighlight);
  }
  // ignore when only C is pressed
}

function tableCellElementOnKeyEvent(tableCellElement: HTMLTableCellElement, event: KeyboardEvent) {
  switch (event.key) {
    case "Down": // IE/Edge specific value
    case "ArrowDown":
      updateActiveTableCellElement(getDownTableCellElement(tableCellElement));
      break;
    case "Up": // IE/Edge specific value
    case "ArrowUp":
      updateActiveTableCellElement(getUpTableCellElement(tableCellElement));
      break;
    case "Left": // IE/Edge specific value
    case "ArrowLeft":
      updateActiveTableCellElement(getLeftTableCellElement(tableCellElement));
      break;
    case "Right": // IE/Edge specific value
    case "ArrowRight":
    case "Tab": // handle Tab as a pressing Right arrow
      updateActiveTableCellElement(getRightTableCellElement(tableCellElement));
      break;
    case "c": // handle potential CTRL+c or CMD+c
      tableCellElementOnCopy(tableCellElement, event);
      break;
  }
  event.preventDefault();
  event.stopPropagation();
}
tableElement.addEventListener("keydown", function(event: KeyboardEvent) {
  const target: HTMLElement = event.target as HTMLElement;
  if (isTableCell(target)) {
    tableCellElementOnKeyEvent(target as HTMLTableCellElement, event);
  }
}, true);

/* mouse events */
interface ResizableHTMLTableCellElement extends HTMLTableCellElement {
  atResize?: boolean;
  nearLeftBorder?: boolean;
  nearRightBorder?: boolean;
}
let tableCellElementUnderMouse: null | ResizableHTMLTableCellElement = null;
const nearLeftBorderClass = "near-left-border";
const nearRightBorderClass = "near-right-border";
function stopResizing(resizableHTMLTableCellElement: ResizableHTMLTableCellElement) {
  resizableHTMLTableCellElement.atResize = false;
}
function updateTableCellElementUnderMouse(tableCellElement: HTMLTableCellElement) {
  if (tableCellElementUnderMouse) {
    // stop resizing
    stopResizing(tableCellElementUnderMouse);
    tableCellElementUnderMouse.classList.remove(nearLeftBorderClass, nearRightBorderClass);
  }
  tableCellElementUnderMouse = tableCellElement;
}
function isTableCellElementAtResizing(tableCellElement: ResizableHTMLTableCellElement) {
  return tableCellElement.atResize === true;
}
function handleMouseMoveNearElementBorder(tableCellElement: ResizableHTMLTableCellElement, event: MouseEvent) {
  const {left: elementLeft, right: elementRight} = tableCellElement.getBoundingClientRect();
  const mouseX = event.clientX;
  const distanceFromLeftBorder = mouseX - elementLeft;
  const distanceFromRightBorder = elementRight - mouseX;
  if (distanceFromLeftBorder >= 10 && distanceFromRightBorder >= 10) {
    // reset indicator classes if far from both borders
    tableCellElement.classList.remove(nearLeftBorderClass, nearRightBorderClass);
  } else {
    if (distanceFromLeftBorder < 10) {
      // near left border
      tableCellElement.classList.add(nearLeftBorderClass);
    }
    if (distanceFromRightBorder < 10) {
      // near right border
    tableCellElement.classList.add(nearRightBorderClass);
    }
  }
}
function tableHeadOnMouseMove(tableCellElement: HTMLTableCellElement, event: MouseEvent) {
  if (tableCellElement === tableCellElementUnderMouse) {
    // same element under mouse move
    if (isTableCellElementAtResizing(tableCellElementUnderMouse)) {
      // resize by moved amount
      return;
    }
  } else {
    // different element under mouse move
    updateTableCellElementUnderMouse(tableCellElement);
  }
  // handle mouse move to element border
  handleMouseMoveNearElementBorder(tableCellElement, event);
}
tableElement.addEventListener("mousemove", function(event: MouseEvent) {
  const target: HTMLElement = event.target as HTMLElement;
  if (isTableHead(target)) {
    tableHeadOnMouseMove(target as HTMLTableCellElement, event);
  }
});
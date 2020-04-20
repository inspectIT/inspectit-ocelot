/**
 * A heuristic for getting the "JSON-path" at a given position within a YAML document.
 * This fuunction takes the list of lines of the document.
 * The cursor is considered to be after the last character of the last line.
 *
 * The return value is an array of path segments found in order, e.g. [inspectit,config,http].
 * If the path lies within a list, the list index is also correctly added to the path.
 *
 * This function does fail for JSON-style Maps and lists within the yaml.
 * @param {} allLines the lines of the YAML document
 */
export const getYamlPath = (allLines) => {
  let lines = allLines;
  if (findNonQuotedChar(lines[lines.length - 1], '#') != -1) {
    return null; // we are in a comment
  }

  lines = removeCommentsAndBlankLines(lines);

  let path = beginPath(lines[lines.length - 1]);

  let listIndices = [];

  const lastLineIndentation = getIndentation(lines[lines.length - 1]);
  let parentLineNumber = getNonListParent(lastLineIndentation - 1, lines, lines.length - 1, listIndices);
  path = listIndices.concat(path);

  while (parentLineNumber != -1) {
    const indentation = getIndentation(lines[parentLineNumber]);
    const statement = decodeStatment(lines[parentLineNumber].substring(indentation));
    if (statement.value) {
      return null;
    }
    path = [statement.key, ...path];
    listIndices = [];
    parentLineNumber = getNonListParent(indentation - 1, lines, parentLineNumber, listIndices);
    path = listIndices.concat(path);
  }
  return path;
};

/**
 *
 * Begins to find the configuration path based on the line which contains the cursor
 * For example, if the line is `    "enabled" : tr`,
 * the returned path is [enabled,tr].
 * Leading indentation of the line is ignored.
 *
 * @param {*} cursorLine
 */
const beginPath = (cursorLine) => {
  const indentation = getIndentation(cursorLine);
  const lastStatement = decodeStatment(cursorLine.substring(indentation));
  if (lastStatement.value != null) {
    return [lastStatement.key, lastStatement.value];
  } else {
    return [lastStatement.key];
  }
};

/**
 * Removes yaml comments (starting with '#') and all empty lines.
 * If the last line is a blank line, it is kept.
 * @param {*} lines the array of lines
 */
const removeCommentsAndBlankLines = (lines) => {
  lines = lines.map((line) => {
    const idx = findNonQuotedChar(line, '#');
    return idx != -1 ? line.substring(0, idx) : line;
  });
  // remove all empty lines (except for the last one)
  const lastLine = lines[lines.length - 1];
  lines = lines.filter((line) => line === lastLine || !line.match(/^\s*$/));
  return lines;
};

/**
 * Scans for the parent line of a given starting line in yaml.
 * A parent line is a line in the form `<key>:` which has lower indentation than the starting line.
 * If any dashes are encountered while scannign for the parent line, the resulting list indices are added to the provided
 * "indices" array.
 *
 * For example, given the followign snippet:
 *      config:
 *        - first
 *        - - something
 *          - something else
 *          - last
 * If we invoke this method scanning for the parent pointing to the last line, the result will be the line number of "config:".
 * The indices array will be [1,2].
 * The reason is, that in parsed form config[1][2] will refer to "last".
 *
 * Returns the index of the found parent line or "-1" if no parent exists.
 *
 * @param {*} pos  the index of the last character in the starting line which is part of the indentation.
 * @param {*} lines the array of all lines
 * @param {*} lineNumber the index of the starting line number
 * @param {*} indices the array to place found list-indices within.
 */
const getNonListParent = (pos, lines, lineNumber, indices) => {
  if (pos == -1) {
    return -1;
  }
  const line = lines[lineNumber];
  const firstDash = getFirstDash(line);
  const lastDash = getLastDash(line, pos);
  if (firstDash != lastDash) {
    const result = getNonListParent(lastDash - 1, lines, lineNumber, indices);
    indices.push(0);
    return result;
  }

  if (firstDash == -1) {
    // no more dashes
    let parentLine = lineNumber - 1;
    const leadingSpaces = pos + 1;
    while (parentLine >= 0 && getLeadingSpaces(lines[parentLine]) >= leadingSpaces) {
      parentLine--;
    }
    if (parentLine != -1) {
      const parentIndent = getIndentation(lines[parentLine]);
      if (parentIndent >= leadingSpaces) {
        return getNonListParent(leadingSpaces - 1, lines, parentLine, indices);
      }
    }
    return parentLine;
  } else {
    const leadingSpaces = firstDash;
    let parentLine = lineNumber - 1;
    let listIndex = 0;
    while (parentLine >= 0 && (getLeadingSpaces(lines[parentLine]) > leadingSpaces || getFirstDash(lines[parentLine]) == firstDash)) {
      if (lines[parentLine].length > firstDash && lines[parentLine][firstDash] == '-') {
        listIndex++;
      }
      parentLine--;
    }
    if (parentLine >= 0 && lines[parentLine].length > firstDash && lines[parentLine][firstDash] == '-') {
      listIndex++;
    }
    let result = parentLine;
    if (parentLine != -1) {
      const indent = firstDash + 1;
      const parentIndent = getIndentation(lines[parentLine]);
      if (parentIndent >= indent) {
        result = getNonListParent(firstDash - 1, lines, parentLine, indices);
      }
    }
    indices.push(listIndex);
    return result;
  }
};

/**
 * Extracts the key and the value from a colon-assignment in yaml.
 * Hereby quotes and brackets are removed.
 * Examples.
 *  ` hello: world  ` -> (key = hello, value = world)
 *  ` '[hello]': "world"  ` -> (key = hello, value = world)
 *  ` "hello": "wor  ` -> (key = hello, value = wor)
 *  ` "hello":   ` -> (key = hello, value = <empty string>)
 *  ` "hello"   ` -> (key = hello, value = null)
 *
 * @param {*} statement the input statement as a string
 */
const decodeStatment = (statement) => {
  const colonIndex = findNonQuotedChar(statement, ':');
  let key = unquote(colonIndex != -1 ? statement.substring(0, colonIndex).trim() : statement.trim());
  const value = colonIndex != -1 ? unquote(statement.substring(colonIndex + 1).trim()) : null;
  key = removeBrackets(key);

  return { key, value };
};

/**
 * Removes quotes from the beginnin and end of a string.
 * Leading quotes (single and double) are always removed.
 * Trailing quotes are only removed if a leading quote was removed.
 *
 * @param {*} text the string to remove the quotes from.
 */
const unquote = (text) => {
  if (text.startsWith("'") || text.startsWith('"')) {
    if (text.endsWith("'") || text.endsWith('"')) {
      return text.substring(1, text.length - 1);
    } else {
      return text.substring(1, text.length);
    }
  }
  return text;
};

/**
 * Removes Brackets "[" and "]" from the beginnin and end of a string.
 * Leading "[" are always removed.
 * Trailing "]" are only removed if a leading "[" was removed.
 *
 * @param {*} text the string to remove the brackets from.
 */
const removeBrackets = (text) => {
  if (text.startsWith('[')) {
    if (text.endsWith(']')) {
      return text.substring(1, text.length - 1);
    } else {
      return text.substring(1, text.length);
    }
  }
  return text;
};

/**
 * Finds the first occurence of a given character in a given string.
 * Hereby occurences are ignored which are within single or double quotes.
 *
 * Returns the index of the found occurece or -1 if none is found.
 *
 * @param {*} str the string to scan within
 * @param {*} char the character to scan for
 */
const findNonQuotedChar = (str, char) => {
  const NORMAL = 0;
  const SINGLE_QUOTED = 1;
  const DOUBLE_QUOTED = 2;
  let state = NORMAL;
  for (let i = 0; i < str.length; i++) {
    if (str[i] == char && state == NORMAL) {
      return i;
    } else if (str[i] == "'") {
      if (state == NORMAL) {
        state = SINGLE_QUOTED;
      } else if (state == SINGLE_QUOTED) {
        state = NORMAL;
      }
    } else if (str[i] == '"') {
      if (state == NORMAL) {
        state = DOUBLE_QUOTED;
      } else if (state == DOUBLE_QUOTED) {
        state = NORMAL;
      }
    }
  }
  return -1;
};

/**
 * Returns the number of leading spaces or dashes for the given string.
 *
 * @param {*} line the string to get the indentation of
 */
const getIndentation = (line) => {
  let pos = 0;
  while (pos < line.length && (line[pos] == ' ' || line[pos] == '-')) {
    pos++;
  }
  return pos;
};

/**
 * Returns the number of leading spaces for the given string.
 *
 * @param {*} line the string to get the number of leading spaces from
 */
const getLeadingSpaces = (line) => {
  let pos = 0;
  while (pos < line.length && line[pos] == ' ') {
    pos++;
  }
  return pos;
};

/**
 * Returns the index of the first dash after spaces for the given string.
 * Returns -1 if there is no dash found.
 *
 * @param {*} line the string to find the first dash within
 */
const getFirstDash = (line) => {
  let pos = 0;
  while (pos < line.length && line[pos] == ' ') {
    pos++;
  }
  if (pos < line.length && line[pos] == '-') {
    return pos;
  } else {
    return -1;
  }
};

/**
 * Scans backwards in a given string, starting at a specified index, for a dash.
 * If a dash is found, it's index is returned.
 * Otherwise -1 is returned.
 *
 * @param {*} line the string to find the first dash within
 * @param {*} posInclusive the index to start scanning at
 */
const getLastDash = (line, posInclusive) => {
  let pos = posInclusive;
  while (pos >= 0 && line[pos] != '-') {
    pos--;
  }
  return pos;
};

import _ from 'lodash';
import { Button } from 'primereact/button';
import { Dialog } from 'primereact/dialog';
import { InputText } from 'primereact/inputtext';
import { ListBox } from 'primereact/listbox';
import { ProgressSpinner } from 'primereact/progressspinner';
import { ToggleButton } from 'primereact/togglebutton';
import PropTypes from 'prop-types';
import React, { useEffect, useRef, useState } from 'react';
import useFetchData from '../../../../hooks/use-fetch-data';

/**
 * Template for the individual serach result items.
 */
const SearchResultTemplate = ({ filename, lineNumber, line, matches }) => {
  // tokenization of the result line into separate elements (matching part and non matching part) for highlighting
  let index = 0;
  const split = [];
  _.each(matches, (match) => {
    const { start, end } = match;
    if (start > index) {
      const previousText = line.substring(index, start);
      split.push({ text: previousText, highlight: false });
    }
    const matchText = line.substring(start, end);
    split.push({ text: matchText, highlight: true });
    index = end;
  });

  if (index < line.length) {
    const endingText = line.substring(index);
    split.push({ text: endingText, highlight: false });
  }

  split[0].text = split[0].text.replace(/^\s+/, ''); // trim leading spaces

  return (
    <>
      <style jsx>{`
        .item {
          display: flex;
        }
        .match {
          flex-grow: 1;
          font-family: monospace;
          overflow: hidden;
          white-space: nowrap;
          text-overflow: ellipsis;
        }
        .source {
          color: #9e9e9e;
          margin-left: 2rem;
          white-space: nowrap;
        }
        .source span {
          color: black;
        }
        :global(.p-highlight) .source,
        :global(.p-highlight) .source span {
          color: inherit;
        }
        .highlight {
          background-color: #ffa726;
          padding: 0 0.1rem;
          margin: 0 1px;
          border-radius: 4px;
        }
      `}</style>

      <div className="item">
        <div className="match">
          {split.map((match, index) =>
            match.highlight ? (
              <span key={index} className="highlight">
                {match.text}
              </span>
            ) : (
              match.text
            )
          )}
        </div>
        <div className="source">
          {filename} <span>{lineNumber + 1}</span>
        </div>
      </div>
    </>
  );
};

SearchResultTemplate.propTypes = {
  /** The filename containing the matches */
  filename: PropTypes.string,
  /** The line number where the matches are starting */
  lineNumber: PropTypes.number,
  /** The file's content on the current line */
  line: PropTypes.string,
  /** The matches of the search query */
  matches: PropTypes.arrayOf(PropTypes.object),
};

/**
 * The search dialog itself.
 */
const SearchDialog = ({ visible, onHide, openFile }) => {
  // constants
  const searchLimit = 100;

  // state variables
  const [searchTarget, setSearchTarget] = useState(0); // the current selected file name
  const [resultSelection, setResultSelection] = useState(null);
  const [searchResults, setSearchResults] = useState([]);
  const [query, setQuery] = useState('');

  // refs
  const queryInputRef = useRef(null);

  // fetching the search results
  const [{ data, isLoading }, refreshData] = useFetchData('/search', {
    query: query,
    'include-first-line': true,
    limit: searchLimit,
  });

  // derived variables
  const showDataLimit = data && data.length >= searchLimit;

  // executing the search request if possible
  const executeSearch = () => {
    if (query && !isLoading) {
      setResultSelection(null);
      refreshData();
    }
  };

  // open the currently selected file in the config editor
  const onOpenFile = () => {
    openFile(resultSelection.filename);
    onHide();
  };

  // sets the focus on the query input
  const focusQueryInput = () => {
    queryInputRef.current.element.focus();
  };

  // focus the query input if the loading state of the request is changing. necessary because the focus is lost during loading
  useEffect(() => {
    if (queryInputRef.current) {
      if (!isLoading) {
        focusQueryInput();
      }
    }
  }, [isLoading]);

  // updating the result list in case the data (raw data of the search request) is changing
  useEffect(() => {
    const result = _(data)
      .groupBy((element) => element.file + ':' + element.startLine)
      .map((elements, key) => {
        const matches = _.map(elements, (element) => {
          return {
            start: element.startColumn,
            end: element.endColumn,
          };
        });

        return {
          key,
          filename: elements[0].file, // always the same because we group for this
          lineNumber: elements[0].startLine, // always the same because we group for this
          line: elements[0].firstLine, // must be the same for all elements
          matches,
        };
      })
      .value();

    setSearchResults(result);
  }, [data]);

  // the dialogs footer
  const footer = (
    <div>
      <Button label="Open File" icon="pi pi-external-link" onClick={onOpenFile} disabled={!resultSelection} />
    </div>
  );

  return (
    <>
      <style jsx>{`
        .this :global(.p-dialog-content) {
          padding: 0;
          display: flex;
          flex-direction: column;
          align-items: stretch;
          height: 75vh;
        }
        .input-container {
          margin: 0.5rem;
        }
        .target {
          display: flex;
        }
        .target :global(.p-togglebutton) {
          margin-right: 0.5rem;
        }
        .target-details {
          flex-grow: 1;
        }
        .target-details :global(.target-direcotry) {
          width: 100%;
        }
        .query {
          margin-bottom: 0.5rem;
        }
        .query :global(.query-input) {
          flex-grow: 1;
        }
        .this :global(.p-listbox) {
          width: 100%;
          border: 0;
          border-radius: 0;
        }
        .content {
          flex-grow: 1;
          overflow-y: auto;
        }
        .no-data {
          color: #9e9e9e;
          display: flex;
          align-items: center;
          justify-content: center;
          height: 100%;
        }
        .limit-information {
          text-align: center;
          color: #9e9e9e;
          padding-top: 0.5rem;
          padding-bottom: 0.5rem;
        }
        .limit-information span {
          background-color: #90a4ae;
          padding: 0.25rem 1.5rem;
          border-radius: 0.5rem;
          color: white;
        }
        .loading-indicator {
          height: 100%;
          align-items: center;
          display: flex;
        }
      `}</style>

      <div className="this">
        <Dialog
          className="search-dialog"
          header="Find in Configuration Files"
          visible={visible}
          style={{ width: '50vw', minWidth: '50rem' }}
          onHide={onHide}
          blockScroll
          footer={footer}
          onShow={focusQueryInput}
          focusOnShow={false}
        >
          <div className="input-container">
            <div className="query p-inputgroup">
              <span className="p-inputgroup-addon">
                <i className="pi pi-search"></i>
              </span>
              <InputText
                ref={queryInputRef}
                className="query-input"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                onKeyPress={(e) => e.key === 'Enter' && executeSearch()}
                disabled={isLoading}
              />
              <Button icon="pi pi-search" onClick={executeSearch} disabled={!query || isLoading} />
            </div>

            <div className="target">
              <ToggleButton
                onLabel="In Workspace"
                offLabel="In Workspace"
                checked={searchTarget === 0}
                onChange={() => setSearchTarget(0)}
              />
              <ToggleButton
                onLabel="In Directory"
                offLabel="In Directory"
                checked={searchTarget === 1}
                onChange={() => setSearchTarget(1)}
                disabled={true}
              />
              <ToggleButton
                onLabel="In Agent Mapping"
                offLabel="In Agent Mapping"
                checked={searchTarget === 2}
                onChange={() => setSearchTarget(2)}
                disabled={true}
              />

              <div className="target-details">{searchTarget === 1 && <InputText className="target-direcotry" placeholder="/" />}</div>
            </div>
          </div>

          <div className="content">
            {isLoading ? (
              <div className="loading-indicator">
                <ProgressSpinner />
              </div>
            ) : searchResults && searchResults.length > 0 ? (
              <ListBox
                optionLabel="key"
                value={resultSelection}
                options={searchResults}
                onChange={(e) => setResultSelection(e.value)}
                itemTemplate={SearchResultTemplate}
              />
            ) : (
              <span className="no-data">Nothing to show</span>
            )}
          </div>

          {showDataLimit && (
            <div className="limit-information">
              <span>Please refine your search. Showing only the first {searchLimit} hits.</span>
            </div>
          )}
        </Dialog>
      </div>
    </>
  );
};

SearchDialog.propTypes = {
  /** Whether this dialog is shown */
  visible: PropTypes.bool,
  /** Is called when the dialog should disappear */
  onHide: PropTypes.func,
  /** Is called when a specific file should be opened due to a user interaction */
  openFile: PropTypes.func,
};

export default SearchDialog;

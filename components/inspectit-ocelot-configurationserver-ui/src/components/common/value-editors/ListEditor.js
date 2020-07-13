import React, { useState, useEffect } from 'react';
import classNames from 'classnames';
import PropTypes from 'prop-types';
import { isEqual, uniq } from 'lodash';
import { InputText } from 'primereact/inputtext';
import { Button } from 'primereact/button';
import { AutoComplete } from 'primereact/autocomplete';

/**
 * Editor for string lists.
 */
const ListEditor = ({ value, options, disabled, entryWidth, updateValue, entryIcon, separators, validateEntryFunc }) => {
  const [entries, setEntries] = useState([]);
  const [newEntry, setNewEntry] = useState('');
  const [suggestions, setSuggestions] = useState([]);

  useEffect(() => {
    if (!isEqual(entries, value) && value) {
      setEntries([...value]);
    }
  }, [value]);

  useEffect(() => {
    if (options && !isEqual(suggestions, options)) {
      setSuggestions([...options]);
    }
  }, [options]);

  const addEntries = () => {
    if (newEntry) {
      let newEntries = newEntry
        .split(separators)
        .map((s) => s.trim())
        .filter((s) => s !== '');

      newEntries = uniq(newEntries);

      newEntries = newEntries
        .filter((s) => !entries || !entries.some((other) => other === s))
        .filter((s) => !validateEntryFunc || validateEntryFunc(s) === true);

      newEntries = [...entries, ...newEntries];

      setEntries(newEntries);
      setNewEntry('');
      updateValue(newEntries);
    }
  };

  const entryIconClassNames = classNames('pi', entryIcon);

  const tooltipOptions = {
    showDelay: 500,
    position: 'top',
  };

  return (
    <div className="this">
      <style jsx>{`
        .this {
          display: flex;
          flex: 1 1 auto;
          flex-direction: column;
          align-items: stretch;
          justify-content: flex-start;
          padding: 0.5rem;
        }
        .this :global(.input-row) {
          display: flex;
          flex-direction: row;
        }
        .this :global(.input-row .p-inputtext) {
          flex-grow: 1;
        }
        .this :global(.autocomplete-span) {
          flex-grow: 1;
        }
        .this :global(.input-row .p-button) {
          margin-left: 0.5rem;
        }
        .this :global(.p-button.p-autocomplete-dropdown) {
          margin-left: 0;
        }
        .this :global(.entries-view) {
          margin-top: 0.5rem;
          overflow: auto;
        }
        .this :global(.entry-row) {
          display: flex;
          flex-direction: row;
          justify-content: space-between;
          align-items: center;
          background: #eaeaea;
          border-radius: 5px;
          flex-grow: 0;
          margin: 0.2rem 0;
          padding: 0.5rem;
        }
        .this :global(.entry-row:hover) {
          background: #007adb;
        }
        .this :global(.entry-row:hover .remove-button) {
          color: red;
        }
        .this :global(.entry-row:hover .entry) {
          color: white;
        }
        .this :global(.entry) {
          display: flex;
          flex-direction: row;
          justify-content: flex-start;
          color: #555;
          word-break: break-all;
        }
        .this :global(.entry .pi) {
          margin-right: 1rem;
        }
        .this :global(.remove-button) {
          cursor: pointer;
        }
      `}</style>
      <div className="input-row">
        {!options && (
          <InputText
            disabled={disabled}
            value={newEntry}
            onChange={(e) => setNewEntry(e.target.value)}
            autoFocus
            onKeyPress={(e) => e.key === 'Enter' && addEntries()}
          />
        )}

        {!!options && (
          <span className="p-fluid autocomplete-span">
            <AutoComplete
              value={newEntry}
              onChange={(e) => setNewEntry(e.target.value)}
              dropdown
              suggestions={suggestions}
              completeMethod={(event) => {
                const results = options.filter((opt) => opt.toLowerCase().startsWith(event.query.toLowerCase()));
                setSuggestions(results);
              }}
              onKeyPress={(e) => e.key === 'Enter' && addEntries()}
            />
          </span>
        )}
        <Button disabled={disabled} tooltip="Add entries" icon="pi pi-plus" tooltipOptions={tooltipOptions} onClick={() => addEntries()} />
      </div>
      {entries && entries.length > 0 && (
        <div className="p-grid p-justify-around entries-view p-nogutter">
          {entries.map((entry) => (
            <div key={entry} className="p-col-fixed entry-row" style={{ width: entryWidth }}>
              <div className="entry">
                <i className={entryIconClassNames}></i>
                {entry}
              </div>
              <div className="remove-button">
                <i
                  className="pi pi-times"
                  entryId={entry}
                  onClick={(e) => {
                    if (e && e.target && e.target.attributes && e.target.attributes.entryId) {
                      const entryId = e.target.attributes.entryId.value;
                      const newEntries = entries.filter((ent) => ent !== entryId);
                      setEntries(newEntries);
                      updateValue(newEntries);
                    }
                  }}
                ></i>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

ListEditor.propTypes = {
  /** An array of current values in the list. */
  value: PropTypes.array.isRequired,
  /** Optional list of options */
  options: PropTypes.array,
  /** The css width of the entry elements in the view */
  entryWidth: PropTypes.string,
  /** THe prime react icon name for the elements */
  entryIcon: PropTypes.string,
  /** Whether the editor is disabled or not */
  disabled: PropTypes.bool,
  /** Regular expression or string denoting the element separators */
  separators: PropTypes.object,
  /** Callback on value change */
  updateValue: PropTypes.func,
  /** Function used to validate element entries */
  validateEntryFunc: PropTypes.func,
};

ListEditor.defaultProps = {
  options: undefined,
  disabled: false,
  entryWidth: '30rem',
  entryIcon: 'pi-circle-on',
  separators: ',',
  updateValue: () => {},
  validateEntryFunc: () => {
    return true;
  },
};

export default ListEditor;

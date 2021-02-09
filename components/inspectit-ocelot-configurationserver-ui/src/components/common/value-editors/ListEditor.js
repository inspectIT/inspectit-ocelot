import React, { useState, useEffect } from 'react';
import classNames from 'classnames';
import { useDispatch } from 'react-redux';
import PropTypes from 'prop-types';
import { isEqual, uniq } from 'lodash';
import { InputText } from 'primereact/inputtext';
import { Button } from 'primereact/button';
import { AutoComplete } from 'primereact/autocomplete';
import { notificationActions } from '../../../redux/ducks/notification';

/**
 * Editor for string lists.
 */
const ListEditor = ({ compId, value, options, disabled, entryWidth, updateValue, entryIcon, separators, validateEntryFunc }) => {
  const dispatch = useDispatch();
  const [inputText, setInputText] = useState('');
  const [suggestions, setSuggestions] = useState([]);
  const [hasError, setHasError] = useState(false);

  useEffect(() => {
    setInputText('');
  }, [compId]);

  useEffect(() => {
    if (hasError) {
      setHasError(false);
    }
  }, [inputText]);

  useEffect(() => {
    if (options && !isEqual(suggestions, options)) {
      setSuggestions([...options]);
    }
  }, [options]);

  const entries = value ? [...value] : [];

  const addEntries = () => {
    if (inputText) {
      const seperatorMatch = inputText.match(separators);
      const validSeparator = seperatorMatch ? seperatorMatch[0] : ' ';

      let newEntries = inputText
        .split(separators)
        .map((s) => s.trim())
        .filter((s) => s !== '');

      newEntries = uniq(newEntries);

      newEntries = newEntries.filter((s) => !entries || !entries.some((other) => other === s));

      const malformedEntries = validateEntryFunc ? newEntries.filter((s) => !validateEntryFunc(s)) : [];

      newEntries = newEntries.filter((s) => !validateEntryFunc || validateEntryFunc(s) === true);

      newEntries = [...entries, ...newEntries];

      setInputText(malformedEntries.join(validSeparator));
      if (malformedEntries.length > 0) {
        setHasError(true);
        dispatch(notificationActions.showWarningMessage('Invalid input', 'Your input contains invalid entries.'));
      }

      updateValue(newEntries);
    }
  };

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
        .this :global(.input-error .p-inputtext) {
          background-color: #ffe6e6;
        }
      `}</style>
      <div className={classNames('input-row', { 'input-error': hasError })}>
        {!options && (
          <InputText
            disabled={disabled}
            value={inputText}
            onChange={(e) => setInputText(e.target.value)}
            autoFocus
            onKeyPress={(e) => e.key === 'Enter' && addEntries()}
          />
        )}

        {!!options && (
          <span className="p-fluid autocomplete-span">
            <AutoComplete
              value={inputText}
              onChange={(e) => setInputText(e.target.value)}
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
                <i className={classNames('pi', entryIcon)}></i>
                {entry}
              </div>
              {!disabled && (
                <div className="remove-button">
                  <i className="pi pi-times" onClick={() => updateValue(entries.filter((ent) => ent !== entry))}></i>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

ListEditor.propTypes = {
  /** The id of this component instance.
   * Needed to reset values on parent change when no other props are changing. */
  compId: PropTypes.any.isRequired,
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
  separators: PropTypes.oneOfType([PropTypes.string, PropTypes.object]),
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

import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import { Dropdown } from 'primereact/dropdown';

/**
 * Editor for String type values with predefined selection list.
 */
const SelectionEditor = ({ value, options, editable, disabled, updateValue, keyfilter }) => {
  const [val, setValue] = useState(value);
  const wrapperRef = React.createRef();
  const selectItems = !options ? [] : options.map((item) => ({ label: item, value: item }));

  // we need this, as onBlur is not working properly with the dropdown box
  // (onBlur is triggered when drop down list is opened).
  const handleClickOutside = (event) => {
    if (wrapperRef && !wrapperRef.current.contains(event.target)) {
      updateValue(val);
    }
  };

  useEffect(() => {
    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  });

  return (
    <div className="this" ref={wrapperRef}>
      <style jsx>{`
        .this {
          flex-grow: 0;
          width: fit-content;
        }
        .this :global(.p-dropdown) {
          min-width: 250px;
        }
      `}</style>
      <Dropdown
        disabled={disabled}
        value={val}
        editable={editable}
        options={selectItems}
        autoFocus
        filter
        keyfilter={keyfilter}
        onChange={(e) => setValue(e.value)}
        onKeyPress={(e) => e.key === 'Enter' && updateValue(val)}
      />
    </div>
  );
};

SelectionEditor.propTypes = {
  /** The value to edit */
  value: PropTypes.string.isRequired,
  /** List of strings as options for the dropdown */
  options: PropTypes.array,
  /** Whether the editor is editable or not */
  editable: PropTypes.bool,
  /** Whether the editor is disabled or not */
  disabled: PropTypes.bool,
  /** The keyfilter limiting the allowed keys*/
  keyfilter: PropTypes.any,
  /** Callback on value change */
  updateValue: PropTypes.func,
};

SelectionEditor.defaultProps = {
  disabled: false,
  editable: true,
  options: [],
  keyfilter: undefined,
  updateValue: () => {},
};

export default SelectionEditor;

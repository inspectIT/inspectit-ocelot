import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { InputText } from 'primereact/inputtext';

/**
 * Editor for String type values.
 */
const TextEditor = ({ value, disabled, updateValue, keyfilter }) => {
  const [val, setValue] = useState(value);

  return (
    <InputText
      style={{ width: '100%' }}
      disabled={disabled}
      value={val}
      onChange={(e) => setValue(e.target.value)}
      autoFocus
      keyfilter={keyfilter}
      onKeyPress={(e) => e.key === 'Enter' && updateValue(val)}
      onBlur={() => updateValue(val)}
    />
  );
};

TextEditor.propTypes = {
  /** The value to edit */
  value: PropTypes.string.isRequired,
  /** The keyfilter limiting the allowed keys*/
  keyfilter: PropTypes.any,
  /** Whether the editor is disabled or not */
  disabled: PropTypes.bool,
  /** Callback on value change */
  updateValue: PropTypes.func,
};

TextEditor.defaultProps = {
  disabled: false,
  keyfilter: undefined,
  updateValue: () => {},
};

export default TextEditor;

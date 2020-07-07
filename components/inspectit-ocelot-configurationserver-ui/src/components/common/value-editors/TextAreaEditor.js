import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { InputTextarea } from 'primereact/inputtextarea';

/**
 * Editor for long texts.
 */
const TextAreaEditor = ({ value, disabled, updateValue, height }) => {
  const [val, setValue] = useState(value);

  const heightObj = height ? { height: height + 'px' } : {};
  return (
    <InputTextarea
      style={{ ...{ width: '100%' }, ...heightObj }}
      disabled={disabled}
      value={val}
      onChange={(e) => setValue(e.target.value)}
      autoFocus
      onBlur={() => updateValue(val ? val : null)}
    />
  );
};

TextAreaEditor.propTypes = {
  /** The value to edit */
  value: PropTypes.string.isRequired,
  /** Whether the editor is disabled or not */
  disabled: PropTypes.bool,
  /** Callback on value change */
  updateValue: PropTypes.func,
  /** The height in pixel of this element. */
  height: PropTypes.number,
};

TextAreaEditor.defaultProps = {
  disabled: false,
  height: 30,
  updateValue: () => {},
};

export default TextAreaEditor;

import React, { useState } from 'react';
import { InputTextarea } from 'primereact/inputtextarea';

/**
 * Editor for long texts.
 */
const TextAreaEditor = ({ value, type, disabled, updateValue, height}) => {
  const [val, setValue] = useState(value);

  const heightObj = height ? {height: height + 'px'} : {};
  return (<InputTextarea 
    style={{...{width: '100%'},...heightObj}}
    disabled={disabled}
    value={val} 
    onChange={(e) => setValue(e.target.value)}
    autoFocus 
    onBlur={() => updateValue(val ? val : null)}
  />);
};

export default TextAreaEditor;

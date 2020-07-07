import React, { useState } from 'react';
import { InputText } from 'primereact/inputtext';

/**
 * Editor for String type values.
 */
const TextEditor = ({ value, disabled, updateValue, keyfilter}) => {
  const [val, setValue] = useState(value);

  return (
    <InputText
      style={{ width: '100%' }}
      disabled={disabled}
      value={val}
      onChange={(e) => setValue(e.target.value)}
      autoFocus
      keyfilter={keyfilter}
      onKeyPress={(e) => (e.key === 'Enter') && updateValue(val)}
      onBlur={() => updateValue(val)}
    />);
};

export default TextEditor;

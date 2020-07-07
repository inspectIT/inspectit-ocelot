import React, { useState } from 'react';
import { InputSwitch } from 'primereact/inputswitch';

/**
 * Editor for Bool type values.
 */
const BoolEditor = ({ value, disabled, updateValue }) => {
  const [val, setValue] = useState(value);

  return (<InputSwitch
    disabled={disabled}
    checked={val}
    onChange={(e) => {
      setValue(e.target.value);
      updateValue(e.target.value);
    }}
  />);
};

export default BoolEditor;

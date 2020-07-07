import React, { useState, useEffect } from 'react';
import { InputNumber } from 'primereact/inputnumber';

/**
 * Editor for number type values.
 */
const NumberEditor = ({ value, type, disabled, updateValue }) => {
  const [val, setValue] = useState(value);

  const ref = React.createRef();
  useEffect(() => {
    ref.current.inputEl.focus();
  });

  const minFractionDigits = type === 'int' ? 0 : 1;
  const maxFractionDigits = type === 'int' ? 0 : 10;
  return (<InputNumber
    ref={ref}
    disabled={disabled}
    style={{ width: '100%' }}
    value={val}
    onChange={(e) => setValue(e.value)}
    mode="decimal"
    autoFocus
    minFractionDigits={minFractionDigits}
    maxFractionDigits={maxFractionDigits}
    useGrouping={false}
    onKeyPress={(e) => (e.key === 'Enter') && updateValue(val)}
    onBlur={() => updateValue(val)}

  />);
};

export default NumberEditor;

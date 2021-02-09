import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { InputSwitch } from 'primereact/inputswitch';

/**
 * Editor for Bool type values.
 */
const BoolEditor = ({ value, disabled, updateValue }) => {
  const [val, setValue] = useState(value);

  return (
    <InputSwitch
      disabled={disabled}
      checked={val}
      onChange={(e) => {
        setValue(e.target.value);
        updateValue(e.target.value);
      }}
    />
  );
};

BoolEditor.propTypes = {
  /** The value to edit */
  value: PropTypes.bool.isRequired,
  /** Whether the editor is disabled or not */
  disabled: PropTypes.bool,
  /** Callback on value change */
  updateValue: PropTypes.func,
};

BoolEditor.defaultProps = {
  disabled: false,
  updateValue: () => {},
};

export default BoolEditor;

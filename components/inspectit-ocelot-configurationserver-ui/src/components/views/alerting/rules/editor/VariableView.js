import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { Inplace, InplaceDisplay, InplaceContent } from 'primereact/inplace';
import classNames from 'classnames';
import NumberEditor from '../../../../common/value-editors/NumberEditor';
import TextEditor from '../../../../common/value-editors/TextEditor';
import BoolEditor from '../../../../common/value-editors/BoolEditor';
import SelectionEditor from '../../../../common/value-editors/SelectionEditor';

/**
 * The VariableView component encapsulates displaying and editing of values of different type.
 */
const VariableView = ({ name, value, type, description, options, readOnly, onVarUpdate, onErrorStatusUpdate, hasError, isDefault }) => {
  if (onErrorStatusUpdate !== undefined) {
    onErrorStatusUpdate(name, validate(type, value));
  }

  const onDataChanged = (newValue) => {
    if (value !== newValue) {
      if (onErrorStatusUpdate !== undefined) {
        onErrorStatusUpdate(name, validate(type, newValue));
      }
      onVarUpdate(name, !newValue ? null : newValue);
    }
  };

  return (
    <>
      <style jsx>
        {`
          .this {
            margin: 1rem;
            border-bottom: 1px solid #c8c8c8;
          }
          .this :global(.varHeaderContainer) {
            display: flex;
            flex-direction: row;
            align-items: center;
          }
          .this :global(.varHeader) {
            font-weight: bold;
            font-size: larger;
          }
          .this :global(.varHeaderType) {
            margin-left: 1rem;
            color: #888;
            font-size: smaller;
          }
          .this :global(.varDescription) {
            color: #888;
            margin: 0.5rem 0 0.5rem;
          }
          .this :global(.defaultValue) {
            color: #aaa;
          }
          .this :global(.pi-exclamation-triangle) {
            color: red;
          }
          .this :global(.error-col) {
            display: flex;
            font-size: larger;
            flex-direction: row;
            justify-content: flex-end;
          }
          .this :global(.p-inplace-display .pi-pencil) {
            margin-right: 0.5rem;
          }
        `}
      </style>
      <div className="this p-grid">
        <div className="p-col-6">
          <div className="varHeaderContainer">
            <div className="varHeader">{name}</div>
            <div className="varHeaderType">{type}</div>
          </div>
          <div className="varDescription">{description}</div>
        </div>

        <div className="p-col-1 error-col">{hasError && <i className="pi pi-exclamation-triangle" />}</div>
        <div className="p-col-5">
          <ValueView value={value} type={type} options={options} readOnly={readOnly} isDefault={isDefault} onDataChanged={onDataChanged} />
        </div>
      </div>
    </>
  );
};

/**
 * View that allows switching between value display and value edit mode.
 */
const ValueView = ({ value, type, options, readOnly, isDefault, onDataChanged }) => {
  const [inplaceActive, setInplaceState] = useState(false);

  const valueEditor =
    type === 'bool' || !readOnly ? (
      <ValueEditor
        type={type}
        value={value}
        readOnly={readOnly}
        options={options}
        onDataChanged={(value) => {
          setInplaceState(false);
          onDataChanged(value);
        }}
      />
    ) : undefined;

  if (type === 'bool') {
    return valueEditor;
  } else if (readOnly) {
    return <SimpleDataView value={value} isDefault={isDefault} />;
  } else {
    return (
      <Inplace active={inplaceActive} onToggle={() => setInplaceState(true)}>
        <InplaceDisplay>
          <i className="pi pi-pencil" />
          <SimpleDataView value={value} isDefault={isDefault} />
        </InplaceDisplay>
        <InplaceContent>{valueEditor}</InplaceContent>
      </Inplace>
    );
  }
};

/**
 * Editor vor single values.
 */
const ValueEditor = ({ type, value, options, readOnly, onDataChanged }) => {
  if (type === 'bool') {
    return <BoolEditor type={type} value={value} disabled={readOnly} updateValue={onDataChanged} />;
  } else if (type === 'int' || type === 'float') {
    return <NumberEditor type={type} value={value} disabled={readOnly} updateValue={onDataChanged} />;
  } else if (type === 'string' || type === 'regex') {
    return <TextEditor type={type} value={value} disabled={readOnly} updateValue={onDataChanged} />;
  } else if (type === 'duration') {
    return <TextEditor type={type} value={value} keyfilter={/^[\dsmhdw]+$/} disabled={readOnly} updateValue={onDataChanged} />;
  } else if (type === 'selection') {
    return <SelectionEditor options={options} value={value} editable={true} disabled={readOnly} updateValue={onDataChanged} />;
  }
};

/**
 * Simple value display component.
 */
const SimpleDataView = ({ value, isDefault }) => {
  const className = classNames({ defaultValue: isDefault });
  return (
    <span className={className} style={{ width: '100%' }}>
      {'' + value}
    </span>
  );
};

/**
 * Validates the given value of the given type.
 */
const validate = (type, value) => {
  const strValue = '' + value;
  var error = false;
  if (validators[type] !== undefined) {
    error = !validators[type](strValue);
  }
  return error;
};

/**
 * Variable type dependent value validators.
 */
const validators = {
  duration: (value) => {
    const matchResult = value.match(/\d+[smhdw]/);
    return matchResult && matchResult[0] === value;
  },
  regex: (value) => {
    try {
      new RegExp(value);
    } catch (e) {
      return false;
    }
    return true;
  },
};

VariableView.propTypes = {
  /**  Name of the variable */
  name: PropTypes.string.isRequired,
  /**  Value of the variable */
  value: PropTypes.string.isRequired,
  /**  Type of the variable */
  type: PropTypes.string.isRequired,
  /**  Description of the variable */
  description: PropTypes.string,
  /**  Options in case it's a selection variable */
  options: PropTypes.array,
  /**  Whether content is read only */
  readOnly: PropTypes.bool,
  /**  Whether content has errors */
  hasError: PropTypes.bool,
  /**  Whether value is a default value*/
  isDefault: PropTypes.bool,
  /**  Callback on variable update */
  onVarUpdate: PropTypes.func,
  /**  Callback on content error status changed */
  onErrorStatusUpdate: PropTypes.func,
};

VariableView.defaultProps = {
  description: '',
  options: [],
  readOnly: false,
  hasError: false,
  isDefault: false,
  onVarUpdate: () => {},
  onErrorStatusUpdate: () => {},
};

export default VariableView;

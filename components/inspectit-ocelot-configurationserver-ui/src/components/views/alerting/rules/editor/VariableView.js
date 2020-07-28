import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import { Inplace, InplaceDisplay, InplaceContent } from 'primereact/inplace';
import { Button } from 'primereact/button';
import classnames from 'classnames';
import TextEditor from '../../../../common/value-editors/TextEditor';
import BoolEditor from '../../../../common/value-editors/BoolEditor';
import SelectionEditor from '../../../../common/value-editors/SelectionEditor';
import _ from 'lodash';

/**
 * The VariableView component encapsulates displaying and editing of values of different type.
 */
const VariableView = ({
  name,
  value,
  type,
  description,
  options,
  isNullValueValid,
  readOnly,
  onVarUpdate,
  onErrorStatusUpdate,
  error,
  isDefault,
  customErrorCheck,
}) => {
  useEffect(() => {
    checkForError(value);
  });

  const checkForError = (val) => {
    if (!_.isNil(onErrorStatusUpdate)) {
      let errorMessage = customErrorCheck ? customErrorCheck(val) : false;
      if (!errorMessage) {
        errorMessage = isValueErrornuous(type, val, isNullValueValid);
      }
      onErrorStatusUpdate(name, errorMessage);
    }
  };

  const onDataChanged = (newValue) => {
    if (value !== newValue) {
      checkForError(newValue);

      let val = newValue;
      if (type === 'int') {
        val = parseInt(newValue);
      } else if (type === 'float') {
        val = parseFloat(newValue);
      }
      onVarUpdate(name, _.isNil(newValue) ? null : val);
    }
  };

  return (
    <>
      <style jsx>
        {`
          .this {
            padding: 1rem 0.5rem 1rem 0.5rem;
            flex-grow: 1;
            display: flex;
            transition: background-color 0.25s;
          }
          .this:not(:last-child) {
            border-bottom: 1px solid #c8c8c8;
          }
          .this:hover {
            background-color: #f5f5f5;
          }
          .name-container {
            display: flex;
            align-items: baseline;
          }
          .name {
            font-weight: bold;
          }
          .type {
            margin-left: 1rem;
            color: #888;
            font-style: italic;
            font-family: monospace;
          }
          .description {
            color: #616161;
            margin-top: 0.5rem;
          }
          .this :global(.error-col .p-button) {
            background-color: unset;
            border: unset;
            color: #f44336;
            cursor: help;
          }
          .this :global(.error-col .p-button-text) {
            padding: 0;
          }
          .meta-col {
            flex-grow: 6;
            flex-basis: 0;
          }
          .error-col {
            flex-grow: 1;
            flex-basis: 0;
            text-align: right;
            padding: 0.4rem 0.5rem 0 0;
          }
          .value-col {
            flex-grow: 5;
            flex-basis: 0;
          }
        `}
      </style>

      <div className="this">
        <div className="meta-col">
          <div className="name-container">
            <div className="name">{name}</div>
            <div className="type">{type}</div>
          </div>
          <div className="description">{description}</div>
        </div>
        <div className="error-col">{error && <Button icon="pi pi-exclamation-triangle" tooltip={error} />}</div>
        <div className="value-col">
          <ValueView
            value={value}
            type={type}
            options={options}
            readOnly={readOnly}
            isDefault={isDefault}
            error={!!error}
            onDataChanged={onDataChanged}
          />
        </div>
      </div>
    </>
  );
};

/**
 * View that allows switching between value display and value edit mode.
 */
const ValueView = ({ value, type, options, readOnly, isDefault, onDataChanged, error }) => {
  const [inplaceActive, setInplaceState] = useState(false);

  const updateData = (value) => {
    setInplaceState(false);
    onDataChanged(value);
  };

  const valueEditor = <ValueEditor type={type} value={value} readOnly={readOnly} options={options} onDataChanged={updateData} />;

  const dataView = <SimpleDataView value={value} isDefault={isDefault} error={error} />;

  if (type === 'bool') {
    return valueEditor;
  } else if (readOnly) {
    return dataView;
  } else {
    return (
      <>
        <style jsx>{`
          :global(.value-view-editor .p-inplace-display) {
            display: flex;
          }
          .pi-pencil {
            margin-right: 0.5rem;
            color: #616161;
          }
        `}</style>

        <Inplace className="value-view-editor" active={inplaceActive} onToggle={() => setInplaceState(true)}>
          <InplaceDisplay>
            <i className="pi pi-pencil" />
            {dataView}
          </InplaceDisplay>
          <InplaceContent>{valueEditor}</InplaceContent>
        </Inplace>
      </>
    );
  }
};

/**
 * Editor vor single values.
 */
const ValueEditor = ({ type, value, options, readOnly, onDataChanged }) => {
  if (type === 'bool') {
    return <BoolEditor type={type} value={value} disabled={readOnly} updateValue={onDataChanged} />;
  } else if (type === 'int') {
    return <TextEditor type={type} value={value} keyfilter={/^[\d-]+$/} disabled={readOnly} updateValue={onDataChanged} />;
  } else if (type === 'float') {
    return <TextEditor type={type} value={value} keyfilter={/^[\d-.]+$/} disabled={readOnly} updateValue={onDataChanged} />;
  } else if (type === 'string' || type === 'regex') {
    return <TextEditor type={type} value={value} disabled={readOnly} updateValue={onDataChanged} />;
  } else if (type === 'duration') {
    return <TextEditor type={type} value={value} keyfilter={/^[\dsmhdn]+$/} disabled={readOnly} updateValue={onDataChanged} />;
  } else if (type === 'selection') {
    return <SelectionEditor options={options} value={value} editable={true} disabled={readOnly} updateValue={onDataChanged} />;
  }
};

/**
 * Simple value display component.
 */
const SimpleDataView = ({ value, isDefault, error }) => {
  const style = {};
  return (
    <>
      <style jsx>{`
        .value {
          font-family: monospace;
        }
        .default {
          color: #9e9e9e;
          font-style: italic;
        }
        .error {
          color: #f44336;
        }
      `}</style>
      <div className={classnames('value', { default: isDefault || !value, error: error })}>{new String(value || 'Empty Value')}</div>
    </>
  );
};

/**
 * Validates the given value of the given type.
 */
const isValueErrornuous = (type, value, isNullValueValid) => {
  if (!isNullValueValid && _.isNil(value)) {
    return 'Value must not be null!';
  }
  const strValue = '' + value;
  if (!_.isNil(validators[type])) {
    return validators[type](strValue);
  } else {
    return null;
  }
};

/**
 * Variable type dependent value validators.
 */
const validators = {
  duration: (value) => {
    const matchResult = value.match(/\d+(ms|ns|s|m|h|d)/);
    const hasError = !matchResult || matchResult[0] !== value;
    return hasError ? 'Duration variables must follow the following pattern:  1234[ns|ms|s|m|h|d]  Examples: 10s, 7d, ...' : null;
  },
  int: (value) => {
    const matchResult = value.match(/-?\d+/);
    const hasError = !matchResult || matchResult[0] !== value;
    return hasError ? 'Invalid integer value' : null;
  },
  float: (value) => {
    const matchResult = value.match(/-?\d+(\.\d*)?/);
    const hasError = !matchResult || matchResult[0] !== value;
    return hasError ? 'Invalid float value. Must be of the form: 1.2' : null;
  },
  regex: (value) => {
    try {
      new RegExp(value);
    } catch (e) {
      return 'This is not a valid regular expression!';
    }

    return null;
  },
};

VariableView.propTypes = {
  /**  Name of the variable */
  name: PropTypes.string.isRequired,
  /**  Value of the variable */
  value: PropTypes.any,
  /**  Type of the variable */
  type: PropTypes.string.isRequired,
  /**  Description of the variable */
  description: PropTypes.any,
  /** Indicates whether a null value is treated as valid or not */
  isNullValueValid: PropTypes.bool,
  /**  Options in case it's a selection variable */
  options: PropTypes.array,
  /**  Whether content is read only */
  readOnly: PropTypes.bool,
  /**  An error message if existent */
  error: PropTypes.string,
  /**  Whether value is a default value*/
  isDefault: PropTypes.bool,
  /**  Callback on variable update */
  onVarUpdate: PropTypes.func,
  /**  Callback on content error status changed */
  onErrorStatusUpdate: PropTypes.func,
  /** Custom value validator function */
  customErrorCheck: PropTypes.func,
};

VariableView.defaultProps = {
  description: '',
  isNullValueValid: true,
  options: [],
  readOnly: false,
  hasError: undefined,
  isDefault: false,
  onVarUpdate: () => {},
  onErrorStatusUpdate: () => {},
};

export default VariableView;

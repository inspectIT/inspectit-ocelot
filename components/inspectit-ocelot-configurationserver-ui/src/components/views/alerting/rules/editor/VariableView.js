import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { Inplace, InplaceDisplay, InplaceContent } from 'primereact/inplace';
import classnames from 'classnames';
import NumberEditor from '../../../../common/value-editors/NumberEditor';
import TextEditor from '../../../../common/value-editors/TextEditor';
import BoolEditor from '../../../../common/value-editors/BoolEditor';
import SelectionEditor from '../../../../common/value-editors/SelectionEditor';
import _ from 'lodash';

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
      onVarUpdate(name, _.isNil(newValue) ? null : newValue);
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
          .pi-exclamation-triangle {
            color: #f44336;
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

        <div className="error-col">{hasError && <i className="pi pi-exclamation-triangle" />}</div>
        <div className="value-col">
          <ValueView
            value={value}
            type={type}
            options={options}
            readOnly={readOnly}
            isDefault={isDefault}
            error={hasError}
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
      <div className={classnames('value', { default: isDefault, error: error })}>{new String(value)}</div>
    </>
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

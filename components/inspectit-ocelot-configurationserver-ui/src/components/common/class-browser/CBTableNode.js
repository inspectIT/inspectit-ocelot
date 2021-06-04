import React from 'react';
import PropTypes from 'prop-types';
import { RadioButton } from 'primereact/radiobutton';

const CBTableModel = ({ label, type, value, onChange, selectedMethod, parent }) => {
  if (type === 'package') {
    return (
      <>
        <style jsx>{`
          i {
            margin-right: 0.5rem;
            color: gray;
          }
          span {
            font-family: monospace;
          }
        `}</style>
        <i className="pi pi-folder"></i> <span>{label}</span>
      </>
    );
  } else {
    let typeIcon;
    let typeClass;
    let selectionButton;
    if (type === 'class') {
      typeIcon = 'c';
      typeClass = 'theme-class';
    } else if (type === 'interface') {
      typeIcon = 'i';
      typeClass = 'theme-interface';
    } else if (type === 'method') {
      typeIcon = 'm';
      typeClass = 'theme-method';

      const method = {
        value,
        label,
        parent
      };

      selectionButton = <RadioButton value={value} onChange={() => onChange(method)} checked={selectedMethod === value} style={{marginRight: "0.5rem"}} />;
    }

    return (
      <>
        <style jsx>{`
          .theme-class {
            background-color: #719cbb;
          }
          .theme-interface {
            background-color: #55ab73;
          }
          .theme-method {
            background-color: #c855f7;
          }
          .type-icon {
            border-radius: 50%;
            width: 1rem;
            height: 1rem;
            text-align: center;
            color: white;
            font-size: 0.75rem;
            margin-right: 0.5rem;
            font-family: monospace;
            line-height: 1rem;
          }
          span {
            flex-grow: 1;
            overflow: hidden;
            text-overflow: ellipsis;
          }
        `}</style>
        <div className={'type-icon ' + typeClass}>{typeIcon}</div>
        <span title={label}>{label}</span>
        {selectionButton}
      </>
    );
  }
};

CBTableModel.propTypes = {
  /** The label to show */
  label: PropTypes.string,
  /** The elements type */
  type: PropTypes.oneOf(['package', 'class', 'interface', 'method']),
  /** The value of the radio button */
  value: PropTypes.string,
  /** Callback when the radion button is selected */
  onChange: PropTypes.func,
  /** The value of the currently selected element */
  selectedMethod: PropTypes.string,
};

CBTableModel.defaultProps = {
  /** Callback when the radion button is selected */
  onChange: () => {},
};

export default CBTableModel;

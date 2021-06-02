import { Button } from 'primereact/button';
import React from 'react';
import PropTypes from 'prop-types';

/**
 * The method configuration footer bar.
 */
const MethodConfigurationEditorFooter = ({ onAdd }) => {
  return (
    <>
      <style jsx>
        {`
          .this {
            border-top: 1px solid #ddd;
            display: flex;
            padding: 0.5rem 1rem;
            flex-direction: row;
          }
        `}
      </style>

      <div className="this p-component">
        <Button label="Add" onClick={onAdd} />
      </div>
    </>
  );
};

MethodConfigurationEditorFooter.propTypes = {
  /** Callback which is executed when the add scope button is pressed. */
  onAdd: PropTypes.func,
};

export default MethodConfigurationEditorFooter;

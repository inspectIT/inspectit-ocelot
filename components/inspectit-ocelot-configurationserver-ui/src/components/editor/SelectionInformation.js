import React from 'react';
import PropTypes from 'prop-types';

/**
 * Placeholder component for the case that no content is selected.
 */
const SelectionInformation = ({ hint }) => {
  return (
    <>
      <style jsx>{`
        .selection-information {
          display: flex;
          height: 100%;
          align-items: center;
          justify-content: center;
          color: #bbb;
        }
      `}</style>
      <div className="selection-information">
        <div>{hint}</div>
      </div>
    </>
  );
};

SelectionInformation.propTypes = {
  /** The hint to show */
  hint: PropTypes.string,
};

SelectionInformation.defaultProps = {
  hint: 'Select item',
};

export default SelectionInformation;

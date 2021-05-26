import React from 'react';
import PropTypes from 'prop-types';

const HighlightText = ({ value }) => {
  return (
    <>
      <style jsx>{`
        span {
          background-color: #e6e6e6;
          font-family: monospace;
          padding: 0.25rem 0.5rem;
          border-radius: 0.2rem;
          color: #333333;
          font-weight: 700;
        }
      `}</style>
      <span>{value}</span>
    </>
  );
};

HighlightText.propTypes = {
  value: PropTypes.string,
};

HighlightText.defaultProps = {
  value: null,
};

export default HighlightText;

import React from 'react';
import PropTypes from 'prop-types';

const AVAILABLE_THEMES = ['gray', 'blue', 'yellow', 'green'];

/**
 * Component for showing a highlighted text element.
 */
const HighlightText = ({ value, theme }) => {
  const themeClass = AVAILABLE_THEMES.includes(theme) ? theme : AVAILABLE_THEMES[0];

  return (
    <>
      <style jsx>{`
        span {
          font-family: monospace;
          padding: 0.1rem 0.5rem;
          border-radius: 0.2rem;
          font-weight: 700;
        }
        .gray {
          color: #333333;
          background-color: #e6e6e6;
        }
        .blue {
          color: #466594;
          background-color: #cfe2ff;
        }
        .yellow {
          color: #805b36;
          background-color: #ffd8b2;
        }
        .green {
          color: #256029;
          background-color: #c8e6c9;
        }
      `}</style>
      <span className={themeClass}>{value}</span>
    </>
  );
};

HighlightText.propTypes = {
  /** The text to show. */
  value: PropTypes.string,
  /** The color theme to use */
  theme: PropTypes.oneOf(AVAILABLE_THEMES),
};

HighlightText.defaultProps = {
  value: null,
  theme: 'gray',
};

export default HighlightText;

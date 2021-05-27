import React from 'react';
import PropTypes from 'prop-types';

/**
 * Component for displaying an error.
 */
const ErrorInformation = ({ text, error }) => {
  console.log(error);
  return (
    <>
      <style jsx>{`
        .error-information {
          display: flex;
          height: 100%;
          align-items: center;
          justify-content: center;
        }
        .error-box {
          border: 1px solid #e2a4a4;
          max-width: 40rem;
          width: 100%;
          margin: 1rem;
          background-color: #ffcdd2;
          color: #c63737;
        }
        .headline {
          padding: 1rem 2rem 0.5rem;
          border-bottom: 1px solid #e2a4a4;
          font-weight: 700;
          letter-spacing: 0.3px;
          text-transform: uppercase;
        }
        .contact-note {
          padding: 1rem 2rem 0;
          font-size: 0.9rem;
        }
        .error-message {
          padding: 1rem 2rem;
          font-size: 0.9rem;
          font-family: monospace;
        }
      `}</style>
      <div className="error-information">
        <div className="error-box">
          <div className="headline">Error: {text}</div>
          <div className="contact-note">Please contact your system administrator with the following error:</div>
          <div className="error-message">YAML Syntax Error: {error.message}</div>
        </div>
      </div>
    </>
  );
};

ErrorInformation.propTypes = {
  /** The hint to show */
  text: PropTypes.string.isRequired,
  error: PropTypes.object.isRequired,
};

ErrorInformation.defaultProps = {};

export default ErrorInformation;

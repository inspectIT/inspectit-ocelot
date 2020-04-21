import React from 'react';

/**
 * Notificationbar used in the editor view to show notifications.
 */
const Notificationbar = ({ isError, icon, text }) => (
  <>
    <style jsx>
      {`
        .this {
          background-color: #abff87;
          display: flex;
          padding: 0.75rem;
          align-items: center;
        }
        .this.error {
          background-color: #ff8181;
        }
        .this .pi {
          margin-right: 0.5rem;
        }
        .text {
          font-size: 0.9rem;
        }
      `}
    </style>
    <div className={'this' + (isError ? ' error' : '')}>
      {icon && <i className={'pi ' + icon} />}
      <div className="text">{text}</div>
    </div>
  </>
);

export default Notificationbar;

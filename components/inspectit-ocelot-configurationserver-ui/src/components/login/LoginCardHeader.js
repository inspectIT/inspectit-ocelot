import React from 'react';
import { linkPrefix } from '../../lib/configuration';

/**
 * The used header in the login card.
 */
const LoginCardHeader = () => {
  return (
    <div className="this">
      <style jsx>
        {`
          .this {
            text-align: center;
            padding: 1rem 0rem;
          }
          .ocelot-head {
            height: 9rem;
          }
          .text-ocelot {
            font-size: 1rem;
            margin-top: 1.25rem;
            font-weight: bold;
            color: #e8a034;
          }
          .text-server {
            font-size: 1.5rem;
          }
        `}
      </style>
      <img className="ocelot-head" src={linkPrefix + '/static/images/inspectit-ocelot.svg'} />
      <div className="text-ocelot">inspectIT Ocelot</div>
      <div className="text-server">Configuration Server</div>
    </div>
  );
};

export default LoginCardHeader;

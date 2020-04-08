import React from 'react';
import { filter } from 'lodash';

const StatusFooterToolbar = ({ data }) => {
  const totalCount = data.length;
  const agentCount = filter(data, (d) => d.metaInformation).length;
  return (
    <>
      <style jsx>
        {`
          .this {
            background-color: #eeeeee;
            border-top: 1px solid #dddddd;
            padding: 0.5rem 1.5rem;
            font-size: 0.9rem;
            text-align: right;
            color: #333;
          }
          .separator {
            margin: 0 0.5rem;
          }
        `}
      </style>
      <div className="this">
        inspectIT Ocelot Agents: {agentCount} <span className="separator">|</span> Generic Clients: {totalCount - agentCount}{' '}
        <span className="separator">|</span> Total: {totalCount}
      </div>
    </>
  );
};

export default StatusFooterToolbar;

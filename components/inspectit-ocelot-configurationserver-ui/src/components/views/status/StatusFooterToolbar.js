import React from 'react';
import { filter } from 'lodash';

const StatusFooterToolbar = ({ filteredData, data }) => {
  const filteredTotalCount = filteredData.length;
  const filteredAgentCount = filter(filteredData, (f) => f.metaInformation).length;
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
        inspectIT Ocelot Agents: {filteredAgentCount} / {agentCount} <span className="separator">|</span> Generic Clients:{' '}
        {filteredTotalCount - filteredAgentCount} / {totalCount - agentCount}
        <span className="separator">|</span> Total: {filteredTotalCount} / {totalCount}
      </div>
    </>
  );
};

export default StatusFooterToolbar;

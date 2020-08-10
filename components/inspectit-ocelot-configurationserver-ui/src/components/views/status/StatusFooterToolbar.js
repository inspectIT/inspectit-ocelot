import React from 'react';
import { filter } from 'lodash';

const StatusFooterToolbar = ({ fullData, filteredData }) => {
  const shownTotalCount = filteredData.length;
  const shownAgentCount = filter(filteredData, (f) => f.metaInformation).length;
  const totalCount = fullData.length;
  const agentCount = filter(fullData, (d) => d.metaInformation).length;

  const showFilterDetails = shownTotalCount != totalCount;

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
        inspectIT Ocelot Agents: {showFilterDetails && <span>{shownAgentCount} / </span>}
        {agentCount} <span className="separator">|</span> Generic Clients:{' '}
        {showFilterDetails && <span>{shownTotalCount - shownAgentCount} / </span>}
        {totalCount - agentCount}
        <span className="separator">|</span> Total: {showFilterDetails && <span>{shownTotalCount} / </span>}
        {totalCount}
      </div>
    </>
  );
};

export default StatusFooterToolbar;

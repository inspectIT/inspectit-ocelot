import React, { useState, useEffect } from 'react';
import { TabView, TabPanel } from 'primereact/tabview';
import AlertingRulesView from './rules/AlertingRulesView';
import AlertingChannelsView from './topics/AlertingChannelsView.js';
import * as topicsAPI from './topics/TopicsAPI';

/**
 * Tab layout for the alerting view. This view provides a navigation between alerting rules and alerting channels.
 */
const AlertingView = () => {
  const [activeIndex, setActiveIndex] = useState(0);
  const [availableTopics, setAvailableTopics] = useState([]);

  useEffect(() => {
    topicsAPI.fetchTopics((topics) => setAvailableTopics(topics));
  });

  return (
    <div className="this">
      <style jsx>{`
        .this {
          height: 100%;
          display: flex;
        }

        .this :global(.p-tabview-panels) {
          display: flex;
          flex-grow: 1;
          max-height: calc(100% - 35px);
          padding: 0;
        }

        .this :global(.p-tabview-panel) {
          flex-grow: 1;
          display: flex;
        }

        .this :global(.tabView) {
          display: flex;
          flex-direction: column;
          height: 100%;
          width: 100%;
        }
      `}</style>
      <TabView className="tabView" activeIndex={activeIndex} onTabChange={(e) => setActiveIndex(e.index)}>
        <TabPanel header="Alerting Rules">
          <AlertingRulesView availableTopics={availableTopics} />
        </TabPanel>
        <TabPanel header="Alerting Topics">
          <AlertingChannelsView />
        </TabPanel>
      </TabView>
    </div>
  );
};

export default AlertingView;

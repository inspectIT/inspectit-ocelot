import React, { useState, useEffect } from 'react';
import { uniq } from 'lodash';
import { TabView, TabPanel } from 'primereact/tabview';
import AlertingRulesView from './rules/AlertingRulesView';
import * as topicsAPI from './topics/TopicsAPI';
import * as rulesAPI from './rules/RulesAPI';

/**
 * Tab layout for the alerting view. This view provides a navigation between alerting rules and alerting channels.
 */
const AlertingView = () => {
  const [updateDate, setUpdateDate] = useState(Date.now());
  const [activeIndex, setActiveIndex] = useState(0);
  const [rules, setRules] = useState(undefined);
  const [templates, setTemplates] = useState(undefined);
  const [existingTopics, setExistingTopics] = useState(undefined);
  const [referencedTopics, setReferencedTopics] = useState(undefined);

  useEffect(() => {
    if (!rules) {
      refreshRules();
    }
    if (!templates) {
      refreshTemplates();
    }
    if (!existingTopics) {
      refreshTopics();
    }
  });

  useEffect(() => {
    if (rules) {
      const topics = uniq(
        rules.map(r => r.topic)
          .filter(t => t !== undefined && (!existingTopics || !existingTopics.some(topic => topic.id === t))))
        .map(topicName => ({ id: topicName, referencedOnly: true }));
      setReferencedTopics(topics);
    }
  }, [JSON.stringify(rules)]);

  const refreshRules = () => rulesAPI.fetchAlertingRules((rules) => setRules(rules), () => setRules([]));
  const refreshTemplates = () => rulesAPI.fetchAlertingTemplates((templates) => setTemplates(templates), () => setTemplates([]));
  const refreshTopics = () => topicsAPI.fetchTopics((topics) => setExistingTopics(topics));

  const refreshAll = () => {
    refreshRules();
    refreshTemplates();
    refreshTopics();
    setUpdateDate(Date.now());
  };

  const availableTopics = [...(existingTopics || []), ...(referencedTopics || [])];

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
          flex-grow: 1;
        }
      `}</style>
      <TabView className="tabView" activeIndex={activeIndex} onTabChange={(e) => setActiveIndex(e.index)}>
        <TabPanel header="Alerting Rules">
          <AlertingRulesView updateDate={updateDate} availableTopics={availableTopics} rules={rules} templates={templates} onRefresh={refreshAll} />
        </TabPanel>
      </TabView>
    </div>
  );
};

export default AlertingView;

import React, { useState, useEffect } from 'react';
import _ from 'lodash';
import classnames from 'classnames';
import { TabMenu } from 'primereact/tabmenu';
import AlertingRulesView from './rules/AlertingRulesView';
import AlertingChannelsView from './topics/AlertingChannelsView.js';
import * as topicsAPI from './topics/TopicsAPI';
import * as rulesAPI from './rules/RulesAPI';
import useDeepEffect from '../../../hooks/use-deep-effect';
import { ruleIcon, topicIcon } from './constants';

/**
 * Existing tabs for the alerting view tab menu
 */
const items = [
  { label: 'Alerting Rules', icon: classnames('pi', 'pi-fw', ruleIcon) },
  { label: 'Notification Channels', icon: classnames('pi', 'pi-fw', topicIcon) },
];

/**
 * Tab layout for the alerting view. This view provides a navigation between alerting rules and alerting channels.
 */
const AlertingView = () => {
  // state variables
  const [updateDate, setUpdateDate] = useState(Date.now());
  const [activeTab, setActiveTab] = useState(items[0]);
  const [rules, setRules] = useState([]);
  const [templates, setTemplates] = useState([]);
  const [existingTopics, setExistingTopics] = useState([]);
  const [referencedTopics, setReferencedTopics] = useState([]);

  // refreshing the data when the view is mounted
  useEffect(() => {
    refreshAll();
  }, []);

  // collects all topics referenced by any rule
  useDeepEffect(() => {
    const topics = _(rules)
      .map((rule) => rule.topic)
      .filter()
      .filter((topicId) => !existingTopics.some((topic) => topic.id === topicId))
      .map((topicId) => ({ id: topicId, referencedOnly: true }))
      .uniq()
      .sortBy(['id'])
      .value();

    setReferencedTopics(topics);
  }, [rules]);

  const refreshRules = () =>
    rulesAPI.fetchAlertingRules(
      (rules) => setRules(rules),
      () => setRules([])
    );
  const refreshTemplates = () =>
    rulesAPI.fetchAlertingTemplates(
      (templates) => setTemplates(templates),
      () => setTemplates([])
    );
  const refreshTopics = () => topicsAPI.fetchTopics((topics) => setExistingTopics(_(topics).sortBy(['id']).value()));

  // reloads all the alerting data - rules, topics, templates...
  const refreshAll = () => {
    refreshRules();
    refreshTemplates();
    refreshTopics();
    setUpdateDate(Date.now());
  };

  const availableTopics = [...existingTopics, ...referencedTopics];

  return (
    <>
      <style jsx>{`
        .this {
          height: 100%;
          display: flex;
          flex-direction: column;
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
        .this :global(.menu) {
          padding-top: .5rem;
          border-bottom: 1px solid #c8c8c8;
        }
        .this :global(.menu .p-tabmenu-nav) {
          margin-left: 1rem;
          border-bottom: 0;
        }
        .this :global(.menu .p-tabmenu-nav .p-tabmenuitem:not(.p-highlight) .p-menuitem-link ) {
          background-color: #ccc;
          border-color: #c8c8c8;
        }

        }
        .this :global(.menu .p-tabmenu-nav .p-tabmenuitem:not(.p-highlight) .p-menuitem-link .p-menuitem-icon),
        .this :global(.menu .p-tabmenu-nav .p-tabmenuitem:not(.p-highlight) .p-menuitem-link .p-menuitem-text) {
          color: #555;
        }
      `}</style>
      <div className="this">
        <TabMenu className="menu" model={items} activeItem={activeTab} onTabChange={(e) => setActiveTab(e.value)} />

        {activeTab === items[0] && (
          <AlertingRulesView updateDate={updateDate} topics={availableTopics} rules={rules} templates={templates} onRefresh={refreshAll} />
        )}

        {activeTab === items[1] && <AlertingChannelsView updateDate={updateDate} topics={availableTopics} onRefresh={refreshAll} />}
      </div>
    </>
  );
};

export default AlertingView;

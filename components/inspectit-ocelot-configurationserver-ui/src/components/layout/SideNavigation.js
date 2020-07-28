import React, { useState, useEffect } from 'react';
import classNames from 'classnames';
import NavigationItem from './SideNavigationItem';
import { useSelector } from 'react-redux';

/** Data */
import itemData from '../../data/side-navigation-items.json';
import featureEnabled from './FeatureEnabledHelper';

const initialItemsToShow = {
  top: itemData.top.filter((item) => item.alwaysEnabled === true),
  bottom: itemData.bottom.filter((item) => item.alwaysEnabled === true),
};

/**
 * The application's side-navigation.
 * The items are defined in the JSON file which has been imported.
 */
const SideNavigation = () => {
  const [itemsToShow, setItemsToShow] = useState(initialItemsToShow);
  const [unfolded, setUnfolded] = useState(false);

  const globalState = useSelector((state) => state);

  // On component mount: identify which features are enabled for the current user.
  useEffect(() => {
    const checkEnabledFeatures = async () => {
      let enabledTopFeatures = [];
      for (let i = 0; i < itemData.top.length; i++) {
        const fEnabled = await featureEnabled(itemData.top[i].name, globalState);
        if (fEnabled) {
          enabledTopFeatures.push(itemData.top[i]);
        }
      }

      let enabledBottomFeatures = [];
      for (let i = 0; i < itemData.bottom.length; i++) {
        const fEnabled = await featureEnabled(itemData.bottom[i].name, globalState);
        if (fEnabled) {
          enabledBottomFeatures.push(itemData.bottom[i]);
        }
      }
      setItemsToShow({ top: enabledTopFeatures, bottom: enabledBottomFeatures });
    };

    checkEnabledFeatures();
  }, []);

  return (
    <div className={classNames('this', { collapsedSideNav: !unfolded })}>
      <style jsx>
        {`
          .this {
            position: fixed;
            top: 4rem;
            left: 0;
            bottom: 0;
            background-color: #eee;
            display: flex;
            flex-direction: column;
            align-items: center;
            border-right: 1px solid #ddd;
            z-index: 100;
          }
          .collapsedSideNav {
            width: 4rem;
          }
          .toggleButtonSideNavBar {
            width: 100%;
            margin: 0.5rem 0 0.5rem;
            color: #888;
            cursor: pointer;
            display: flex;
            flex-direction: row;
            justify-content: center;
          }
          .toggleButtonSideNavBar:hover {
            color: #444;
          }
        `}
      </style>
      {itemsToShow.top.map((item) => (
        <NavigationItem key={item.name} href={item.href} name={item.name} icon={item.icon} unfolded={unfolded} />
      ))}
      <div style={{ flexGrow: 1 }} />
      {itemsToShow.bottom.map((item) => (
        <NavigationItem key={item.name} href={item.href} name={item.name} icon={item.icon} unfolded={unfolded} />
      ))}
      <div className="toggleButtonSideNavBar" onClick={() => setUnfolded(!unfolded)}>
        <i className={classNames('pi', { 'pi-angle-double-right': !unfolded, 'pi-angle-double-left': unfolded })} />
      </div>
    </div>
  );
};

export default SideNavigation;

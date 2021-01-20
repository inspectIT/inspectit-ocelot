import React, { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { configurationActions } from '../../../../redux/ducks/configuration';
import VersionItem from './VersionItem';
import { VERSION_LIMIT } from '../../../../data/constants';

/**
 * The sidebar panel for showing existing versions of the configuration files.
 */
const HistoryView = () => {
  const dispatch = useDispatch();

  // global state variables
  const versions = useSelector((state) => state.configuration.versions);
  const currentVersion = useSelector((state) => state.configuration.selectedVersion);

  // derived variables
  const limitReached = versions.length >= VERSION_LIMIT;

  useEffect(() => {
    if (versions.length === 0) {
      dispatch(configurationActions.fetchVersions());
    }
  }, []);

  const selectVersion = (versionId) => {
    dispatch(configurationActions.selectVersion(versionId));
  };

  const createVersionItem = (item, index) => {
    const { id, author, date } = item;
    const isLatest = index === 0;
    const isSelected = currentVersion === item.id || (currentVersion === null && isLatest);
    return (
      <VersionItem
        key={index}
        versionName={id.substring(0, 6)}
        author={author}
        timestamp={date * 1000}
        isSelected={isSelected}
        onClick={() => selectVersion(isLatest ? null : item.id)}
        isLatest={isLatest}
      />
    );
  };

  return (
    <>
      <style jsx>
        {`
          .items {
            background-color: #fff;
            border-right: 1px solid #ddd;
            display: flex;
            flex-direction: column;
            width: 22rem;
            overflow-y: auto;
            max-height: 100%;
            box-shadow: -5px 0px 5px 0px #0000001c;
          }
          .branch {
            background-color: #e0e0e0;
            color: #111;
            padding: 1.5rem 1rem 0.5rem;
            border-bottom: 1px solid #9e9e9e;
          }
          .items .branch:not(:first-child) {
            border-top: 1px solid #9e9e9e;
          }
          .limit-note {
            padding: 1rem;
            text-align: center;
            font-size: 0.8rem;
            background-color: #90a4ae;
            color: white;
          }
        `}
      </style>

      <div className="items">
        <div className="branch">Live Configuration</div>
        <VersionItem versionName="Latest" isSelected={currentVersion === 'live'} onClick={() => selectVersion('live')} isLatest={false} />

        <div className="branch">Workspace Configuration</div>

        {versions.map(createVersionItem)}

        {limitReached && <div className="limit-note">Only the last {VERSION_LIMIT} versions are shown.</div>}
      </div>
    </>
  );
};

export default HistoryView;

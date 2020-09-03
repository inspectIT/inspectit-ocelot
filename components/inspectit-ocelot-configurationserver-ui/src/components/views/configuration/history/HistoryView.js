import React, { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { configurationActions } from '../../../../redux/ducks/configuration';
import VersionItem from './VersionItem';

/**
 * The sidebar panel for showing existing versions of the configuration files.
 */
const HistoryView = () => {
  const dispatch = useDispatch();

  // global state variables
  const versions = useSelector((state) => state.configuration.versions);
  const currentVersion = useSelector((state) => state.configuration.selectedVersion);

  useEffect(() => {
    if (versions.length === 0) {
      dispatch(configurationActions.fetchVersions());
    }
  }, []);

  const selectVersion = (versionId) => {
    dispatch(configurationActions.selectVersion(versionId)); // TODO move this to the filetree
  };

  const createVersionItem = (item, index) => {
    const isLatest = index === 0;
    const isSelected = currentVersion === item.id || (currentVersion === null && isLatest);
    return (
      <VersionItem
        key={index}
        item={item}
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
            overflow-y: scroll;
            max-height: 100%;
            box-shadow: -5px 0px 5px 0px #0000001c;
          }
          .branch {
            background-color: #e0e0e0;
            color: #111;
            padding: 1rem 1rem 0.5rem;
            border-bottom: 1px solid #9e9e9e;
          }
        `}
      </style>

      <div className="items">
        <div className="branch">Workspace</div>
        {versions.map(createVersionItem)}
      </div>
    </>
  );
};

export default HistoryView;

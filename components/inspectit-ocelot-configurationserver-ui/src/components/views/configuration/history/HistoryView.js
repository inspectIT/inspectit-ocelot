import React, { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { configurationActions } from '../../../../redux/ducks/configuration';
import VersionItem from './VersionItem';

const HistoryView = ({ selectedVersion, selectedVersionChange }) => {
  const dispatch = useDispatch();

  // global state variables
  const versions = useSelector((state) => state.configuration.versions);

  useEffect(() => {
    if (versions.length == 0) {
      dispatch(configurationActions.fetchVersions());
    }
  }, []);

  const selectVersion = (item, index) => {
    const id = item.id;
    selectedVersionChange(index, id);

    if (index == 0) {
      dispatch(configurationActions.fetchFiles()); // TODO move this to the filetree
    } else {
      dispatch(configurationActions.fetchFiles(id));
    }
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
        `}
      </style>

      <div className="items">
        {versions.map((item, index) => (
          <VersionItem
            key={index}
            item={item}
            isSelected={selectedVersion == index}
            onClick={() => selectVersion(item, index)}
            isLatest={index === 0}
          />
        ))}
      </div>
    </>
  );
};

export default HistoryView;

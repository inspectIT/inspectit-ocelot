import React, { useEffect} from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { configurationActions } from '../../../../redux/ducks/configuration'
import Navigationbar from './Navigationbar';
import contentItem from './ContentItem';

const HistoryView = ({ selectedVersion, selectedVersionChange, showHistory, showHistoryView }) => {

  const dispatch = useDispatch();

  // global state variables
  const versions = useSelector((state) => state.configuration.versions)

  useEffect(() => {
    if (showHistory) {
      if (versions.length == 0) {
        dispatch(configurationActions.fetchVersions());
      }
    }
  }, [showHistory])

  const selectVersion = (item, index) => {
    const id = item.id;
    selectedVersionChange(index, id);

    if (index == 0) {
      dispatch(configurationActions.fetchFiles(null));
    } else {
      dispatch(configurationActions.fetchFiles(id));
    }

  }

  return (
    <>
      <style jsx>
        {`
          .this {
            border: 0;
            border-radius: 0;
            background-color: #eee;
            border-bottom: 1px solid #ddd;
            display: flex;
            flex: 1;
            background-color: white;
          }
          .content {
           
            border-bottom: 1px solid #dddddd;
            border-left: 1px solid #dddddd;
            overflow-x: hidden;
            flex: 1;
            height: 85.1rem;
          }
          .version-selected {
            color: white;
            background: #007AD9;
          }
          .navigationbar {
            background-color: #eeeeee;
            border-left: 1px solid #dddddd;
          }
        `}
      </style>

      <div className="this">
        {showHistory ? (
          <div className="content">
            {versions.map((item, index) => (
              <div className={selectedVersion == index ? "version-selected" : null} key={index} onClick={() => selectVersion(item, index)} >
                {contentItem(item)}
              </div>
            ))}
          </div>
        ) : null}
        <div className="navigationbar">
          <Navigationbar showHistoryView={showHistoryView} show={showHistory} />
        </div>
      </div>
    </>
  );
};

export default HistoryView;

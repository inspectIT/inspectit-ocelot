import axios from '../../../lib/axios-api';
import { notificationActions } from '../notification';
import yaml from 'js-yaml';

import { camelCase } from 'lodash'

export const fetchConfigurationFile = (attributes, mappingName) => {
	let fileName
	if(!mappingName){
		fileName='configFile'
	} else{
		fileName = camelCase(mappingName)
	}

	return dispatch => {
		axios
		.get('/agent/configuration', {
			params: {
				...attributes
			}
		})
		.then(res => {
			let data
			try{
				data = yaml.safeDump(yaml.safeLoad(res.data))
				fileName = fileName + '.yml'
			} catch (a) {
				dispatch(notificationActions.showInfoMessage("parsing to YAML failed", "The file will be downloaded as .txt"));
				data = res.data
				fileName = fileName + '.txt'
			} finally {
				var blob = new Blob([data], {type: 'text/x-yaml'});
				const url = window.URL.createObjectURL(blob);
	
				const link = document.createElement('a');
				link.href = url;
				link.download = `${fileName}`;
	
				document.body.appendChild(link);
				link.click();
			}
		})
	}
}
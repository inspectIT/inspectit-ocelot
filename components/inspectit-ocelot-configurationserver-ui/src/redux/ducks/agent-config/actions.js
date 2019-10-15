import axios from '../../../lib/axios-api';
import { notificationActions } from '../notification';
import yaml from 'js-yaml';
import RandExp from 'randexp';

/**
 * pretends to be an agent and requests the configuration for the given attributes
 * and will start a download dialog afterwards
 * 
 * @param {obj} attributes - the attribute object for which the configuration shall be downloaded
 */
export const fetchConfigurationFile = (attributes) => {
	let fileName = 'agent-config'
	const solvedAtts = _solveRegexValues(attributes)

	return dispatch => {
		axios
		.get('/agent/configuration', {
			params: {
				...solvedAtts
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
		.catch(() => {
			dispatch(notificationActions.showInfoMessage("Downloading config file failed", "No file could have been retrieved. There might not be a configuration for the given attributes"));
		})
	}
}

const _solveRegexValues = (obj) => {
	let res = {}
	Object.keys(obj).map(key => {
		res[key] = new RandExp(obj[key]).gen()
	})
	return res
}
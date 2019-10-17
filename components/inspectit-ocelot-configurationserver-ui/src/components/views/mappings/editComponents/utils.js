import { find, isEqual, cloneDeep, escapeRegExp } from 'lodash';

/**
 * 
 * @param {array} root - array of objects which include at least key: string and children: array in case of nesting
 * @param {string} keyNode - the string/ node key which will be looked for
 */
export const findNode = (root, keyNode) => {
  if(!root || !keyNode){
    return null
  }

  let res = find(root, {key: keyNode});
  if(res){
    return res
  }

  for(let i = 0; !res && i < root.length; i++){
    let childNodes = root[i].children;
    if(childNodes){
      res = findNode(childNodes, keyNode);
    }
  }
  return res;
}

export const findParentNode = (root, keyNode) => {
	return findNode(root, getParentKey(keyNode))
}

export const getParentKey = (string) => {
	const key = string.slice(0, string.lastIndexOf('/'))
	return !isEqual(key, string) ? key : null
}

export const addNode = (root, path) => {
	let keys = path.split('/').filter(str => str !== '');
	if(keys.length <= 0) {return}
	
	let children = Array.isArray(root) ? root : root.children;
	if(!children) {return}

	const newKey = `${root.key ? root.key : ''}/${keys[0]}`;
	let added;
	children.forEach(node => {
		if(node.key === newKey){
			keys.shift();
			added = true;
			addNode(node, keys.join('/'));
		}
	})

	if(!added){
		let newNode = {
			key: newKey,
			label: keys[0],
			icon: 'pi pi-fw pi-question-circle'
		}
		if(!isFile(keys[0])){
			newNode.children = []
		}

		children.push(newNode);
		keys.shift();
		addNode(newNode, keys.join('/'));
	}
}

export const toNodeKey = (string) => {
  if(!string) {return ''}
  let res = string.startsWith('/') ? string : `/${string}`;
  return res
}

/** '/folder/folder/file.yml' becomes ['/folder', '/folder/foler', '/folder/folder/file.yml'] */
export const getTreeKeysArray = (string) => {
  if(string === '/'){
    return ['/']
  }

  let substrings = string.split('/')
  .filter(string => string !== '')
  .map(string => `/${string}`)
  .map((value, index, array) => {
    let splitArray = array.slice(0, index)
    if(splitArray.length > 0){
    return splitArray.reduce((previousValue, currentValue) => { return previousValue + currentValue }) + value
    } else { return value }
  })

  return substrings
}

export const isFile = (string) => {
  if(string.toLowerCase().endsWith('.yml') || string.toLowerCase().endsWith('.yaml')){
    return true;
  }
  return null;
}

export const isSubfile = (baseString, searchString) => {
  const fileRegExp = new RegExp(`${escapeRegExp(searchString)}(?!\\w)(\\/|(?!\\W))`);
  if(baseString.search(fileRegExp) !== -1){
    return true
  }
  return false
}

export const removeSequelPaths = (sources, icludedItems) => {
  let res = cloneDeep(sources);
  icludedItems.forEach(item => {
    sources.forEach(source => {
      if(item.path === '/'){
        res = res.filter(path => path === item.path)
      }
      else if(isSubfile(source, item.path)){
        if(item.isFolder && source > item.path){
          res = res.filter(path => path !== source);
        }
        else if (!item.isFolder && source !== item.path){
          res = res.filter(path => path !== source);
        }
      }
    })
  })
  return res;
}
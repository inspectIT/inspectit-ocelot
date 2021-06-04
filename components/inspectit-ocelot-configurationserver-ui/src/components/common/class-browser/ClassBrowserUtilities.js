import _ from 'lodash';

export const transformClassStructureToTableModel = (result, { name, type, methods }) => {
  const packages = name.split('.');
  const className = packages.pop();

  // find target package
  let targetNode = result;
  let packagePath;
  packages.forEach((packageName) => {
    let target;
    if (targetNode === result) {
      target = _.find(targetNode, { label: packageName });
    } else {
      target = _.find(targetNode.children, { label: packageName });
    }

    if (packagePath) {
      packagePath += '.' + packageName;
    } else {
      packagePath = packageName;
    }

    if (!target) {
      target = {
        key: packagePath,
        label: packageName,
        type: 'package',
        children: [],
        selectable: false,
      };

      if (targetNode === result) {
        result.push(target);
      } else {
        targetNode.children.push(target);
      }
    }

    targetNode = target;
  });

  // add type
  const typeNode = {
    key: name,
    label: className,
    type,
    children: [],
    selectable: false,
  };
  targetNode.children.push(typeNode);

  // add methods
  methods.forEach((method) => {
    typeNode.children.push({
      key: name + ': ' + method,
      label: method,
      type: 'method',
    });
  });
};

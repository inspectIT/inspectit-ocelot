export const tooltipOptions = {
  showDelay: 500,
  position: 'top',
};

export const classMatchers = [
  { label: 'Class', value: 'class' },
  { label: 'Superclass', value: 'superClass' },
  { label: 'Interface', value: 'interface' },
];

export const matcherTypes = [
  { label: 'EQUALS_FULLY', value: 'equalsFully' },
  { label: 'MATCHES', value: 'matches' },
  { label: 'STARTS_WITH', value: 'startsWith' },
  { label: 'STARTS_WITH_IGNORE_CASE', value: 'startsWithIgnoreCase' },
  { label: 'CONTAINS', value: 'contains' },
  { label: 'CONTAINS_IGNORE_CASE', value: 'containsIgnoreCase' },
  { label: 'ENDS_WITH', value: 'endsWith' },
  { label: 'ENDS_WITH_IGNORE_CASE', value: 'endsWithIgnoreCase' },
];

export const methodVisibility = [
  { name: 'public', key: 'public' },
  { name: 'protected', key: 'protected' },
  { name: 'default', key: 'default' },
  { name: 'private', key: 'private' },
];

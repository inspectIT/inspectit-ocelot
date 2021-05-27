export const classMatchers = [
  { label: 'Class', value: 'class' },
  { label: 'Superclass', value: 'superClass' },
  { label: 'Interface', value: 'interface' },
];

export const matcherTypes = [
  { label: 'equals', value: 'EQUALS_FULLY' },
  { label: 'matches', value: 'MATCHES' },
  { label: 'starts with', value: 'STARTS_WITH' },
  { label: 'starts with (case-insensitive)', value: 'STARTS_WITH_IGNORE_CASE' },
  { label: 'contains', value: 'CONTAINS' },
  { label: 'contains (case-insensitive)', value: 'CONTAINS_IGNORE_CASE' },
  { label: 'ends with', value: 'ENDS_WITH' },
  { label: 'ends with (case-insensitive)', value: 'ENDS_WITH_IGNORE_CASE' },
];

export const methodVisibility = [
  { name: 'public', key: 'public' },
  { name: 'protected', key: 'protected' },
  { name: 'default', key: 'default' },
  { name: 'private', key: 'private' },
];

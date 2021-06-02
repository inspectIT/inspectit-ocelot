export const TYPE_MATCHERS = [
  { label: 'Class', value: 'class' },
  { label: 'Superclass', value: 'superclass' },
  { label: 'Interface', value: 'interface' },
];

export const MATCHER_TYPES = [
  { label: 'equals', value: 'EQUALS_FULLY' },
  { label: 'matches', value: 'MATCHES' },
  { label: 'starts with', value: 'STARTS_WITH' },
  { label: 'starts with (case-insensitive)', value: 'STARTS_WITH_IGNORE_CASE' },
  { label: 'contains', value: 'CONTAINS' },
  { label: 'contains (case-insensitive)', value: 'CONTAINS_IGNORE_CASE' },
  { label: 'ends with', value: 'ENDS_WITH' },
  { label: 'ends with (case-insensitive)', value: 'ENDS_WITH_IGNORE_CASE' },
];

export const METHOD_VISIBILITY = ['PUBLIC', 'PROTECTED', 'DEFAULT', 'PRIVATE'];

/** Mapping of texts to the corresponding matcher modes. */
export const MATCHER_MODE_DESCRIPTION = {
  EQUALS_FULLY: 'equal',
  EQUALS_FULLY_IGNORE_CASE: 'equal (case-insensitive)',
  STARTS_WITH: 'start with',
  STARTS_WITH_IGNORE_CASE: 'start with (case-insensitive)',
  ENDS_WITH: 'end with',
  ENDS_WITH_IGNORE_CASE: 'end with (case-insensitive)',
  CONTAINS: 'contain',
  CONTAINS_IGNORE_CASE: 'contain (case-insensitive)',
  MATCHES: 'match',
};

/** Mapping of texts to the corresponding matcher modes with tailing 's'. */
export const MATCHER_MODE_DESCRIPTION_S = {
  EQUALS_FULLY: 'equals',
  EQUALS_FULLY_IGNORE_CASE: 'equals (case-insensitive)',
  STARTS_WITH: 'starts with',
  STARTS_WITH_IGNORE_CASE: 'starts with (case-insensitive)',
  ENDS_WITH: 'ends with',
  ENDS_WITH_IGNORE_CASE: 'ends with (case-insensitive)',
  CONTAINS: 'contains',
  CONTAINS_IGNORE_CASE: 'contains (case-insensitive)',
  MATCHES: 'matches',
};

/** Default visibilities for methods. */
export const DEFAULT_VISIBILITIES = ['PUBLIC', 'PROTECTED', 'PACKAGE', 'PRIVATE'];

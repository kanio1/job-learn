/**
 * Combination EP range + metamorphic inclusion.
 *
 * EP cuts the list at minAmount; MR checks the narrower result ⊆ wider.
 */

import { mrFilter } from '../metamorphic/FilterInclusion'

export const metamorphicListFilter = mrFilter

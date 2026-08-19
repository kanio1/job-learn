import { test as setup } from '@playwright/test'
import { readOnlyUserAccount } from './accounts'
import { pomAuthFiles } from '../utils/env'
import { saveKeycloakStorageState } from './keycloak.setup'

setup('prepare read-only-user storage state', async ({ page }) => {
  await saveKeycloakStorageState(page, readOnlyUserAccount(), pomAuthFiles.readOnlyUser)
})

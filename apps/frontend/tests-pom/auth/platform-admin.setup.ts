import { test as setup } from '@playwright/test'
import { platformAdminAccount } from './accounts'
import { pomAuthFiles } from '../utils/env'
import { saveKeycloakStorageState } from './keycloak.setup'

setup('prepare platform-admin storage state', async ({ page }) => {
  await saveKeycloakStorageState(page, platformAdminAccount(), pomAuthFiles.platformAdmin)
})

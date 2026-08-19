import { test as setup } from '@playwright/test'
import { platformAdminAccount } from './accounts'
import { pomAuthFiles } from '../utils/env'
import { saveKeycloakStorageState } from './keycloak.setup'

/** Isolated copy so logout / device-revoke cannot skip or poison chromium-admin. */
setup('prepare platform-admin session storage state', async ({ page }) => {
  await saveKeycloakStorageState(page, platformAdminAccount(), pomAuthFiles.platformAdminSession)
})

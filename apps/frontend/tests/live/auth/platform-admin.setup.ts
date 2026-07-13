import { test as setup } from '@playwright/test'
import { liveAuthFiles, platformAdminAccount, saveLiveKeycloakStorageState } from './live-keycloak'

setup('prepare real platform-admin storage state', async ({ page }) => {
  await saveLiveKeycloakStorageState(page, platformAdminAccount(), liveAuthFiles.platformAdmin)
})

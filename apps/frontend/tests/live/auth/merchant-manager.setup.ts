import { test as setup } from '@playwright/test'
import { liveAuthFiles, merchantManagerAccount, saveLiveKeycloakStorageState } from './live-keycloak'

setup('prepare real merchant-manager storage state', async ({ page }) => {
  await saveLiveKeycloakStorageState(page, merchantManagerAccount(), liveAuthFiles.merchantManager)
})

import { test as setup } from '@playwright/test'
import { merchantManagerAccount } from './accounts'
import { pomAuthFiles } from '../utils/env'
import { saveKeycloakStorageState } from './keycloak.setup'

setup('prepare merchant-manager storage state', async ({ page }) => {
  await saveKeycloakStorageState(page, merchantManagerAccount(), pomAuthFiles.merchantManager)
})

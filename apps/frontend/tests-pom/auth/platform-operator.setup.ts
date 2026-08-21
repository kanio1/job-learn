import { test as setup } from '@playwright/test'
import { platformOperatorAccount } from './accounts'
import { pomAuthFiles } from '../utils/env'
import { saveKeycloakStorageState } from './keycloak.setup'

setup('prepare platform-operator storage state', async ({ page }) => {
  await saveKeycloakStorageState(page, platformOperatorAccount(), pomAuthFiles.platformOperator)
})

import { test as setup } from '@playwright/test'
import { supportAgentAccount } from './accounts'
import { pomAuthFiles } from '../utils/env'
import { saveKeycloakStorageState } from './keycloak.setup'

setup('prepare support-agent storage state', async ({ page }) => {
  await saveKeycloakStorageState(page, supportAgentAccount(), pomAuthFiles.supportAgent)
})

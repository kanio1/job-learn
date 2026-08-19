import { test as setup } from '@playwright/test'
import { tenantAdminAccount } from './accounts'
import { pomAuthFiles } from '../utils/env'
import { saveKeycloakStorageState } from './keycloak.setup'

setup('prepare tenant-admin storage state', async ({ page }) => {
  await saveKeycloakStorageState(page, tenantAdminAccount(), pomAuthFiles.tenantAdmin)
})

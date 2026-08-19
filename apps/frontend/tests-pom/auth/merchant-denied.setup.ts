import { test as setup } from '@playwright/test'
import { merchantDeniedAccount } from './accounts'
import { pomAuthFiles } from '../utils/env'
import { saveDeniedStorageState } from './keycloak.setup'

setup('prepare merchant.denied storage state', async ({ page }) => {
  await saveDeniedStorageState(page, merchantDeniedAccount(), pomAuthFiles.merchantDenied)
})

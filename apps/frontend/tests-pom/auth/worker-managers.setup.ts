import { test as setup } from '@playwright/test'
import { merchantManagerAccountForWorker, POM_WORKER_COUNT } from './accounts'
import { workerManagerAuthFile } from '../utils/env'
import { saveKeycloakStorageState } from './keycloak.setup'

for (let index = 0; index < POM_WORKER_COUNT; index++) {
  setup(`prepare merchant-manager w${index} storage state`, async ({ page }) => {
    await saveKeycloakStorageState(
      page,
      merchantManagerAccountForWorker(index),
      workerManagerAuthFile(index),
    )
  })
}

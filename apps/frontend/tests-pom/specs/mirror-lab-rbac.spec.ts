import { expect, test } from '../fixtures'
import { pomAuthFiles } from '../utils/env'
import { App } from '../pages/App'
import type { Page } from '@playwright/test'

function approvalInput(page: Page) {
  return page.locator('[data-testid="approval-id"] input, input[data-testid="approval-id"]').first()
}

test('maker cannot self-approve; checker can', async ({ browser }) => {
  const managerContext = await browser.newContext({ storageState: pomAuthFiles.merchantManager })
  const adminContext = await browser.newContext({ storageState: pomAuthFiles.platformAdmin })
  const manager = new App(await managerContext.newPage())
  const admin = new App(await adminContext.newPage())
  try {
    await manager.mirrorBank.goto()
    await manager.mirrorBank.expectLoaded()
    await manager.page.getByTestId('approval-create').click()
    await expect(approvalInput(manager.page)).not.toHaveValue('')
    const approvalId = (await approvalInput(manager.page).inputValue()).trim()
    expect(approvalId.length).toBeGreaterThan(8)

    await manager.page.getByTestId('approval-approve').click()
    await manager.page.getByTestId('confirm-action-confirm').click()
    await expect(manager.page.getByTestId('approval-result')).toContainText('403')

    await admin.mirrorBank.goto()
    await admin.mirrorBank.expectLoaded()
    await approvalInput(admin.page).fill(approvalId)
    await admin.page.getByTestId('approval-approve').click()
    await admin.page.getByTestId('confirm-action-confirm').click()
    await expect(admin.page.getByTestId('approval-result')).toContainText('200')
  }
  finally {
    await managerContext.close()
    await adminContext.close()
  }
})
